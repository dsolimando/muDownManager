/* 
 * Copyright (C) 2010 hrsldn@gmail.com.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 */
package mudownmanager.backend;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import mudownmanager.Callback;
import mudownmanager.DownloadContext;
import mudownmanager.Properties;
import mudownmanager.DownloadContext.Status;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Deprecated
public class Client implements MUClient {

	private static HttpClient httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());
	
	private String destDir = "/tmp";
	
	private ConcurrentHashMap<String, DownloadThread> threads = new ConcurrentHashMap<String, DownloadThread>();
	
	private ThreadPoolExecutor threadPoolExecutor;
	
	public void init () {
		Thread thread = new Thread (new Runnable() {
			
			@Override
			public void run() {
				while (true) {
					List<String> toRemove = new ArrayList<String>();
					for (String key : threads.keySet()) {
						if (threads.get(key).getThreadStatus() != Status.RUNNING
								&& threads.get(key).getThreadStatus() != Status.IDLE) {
							toRemove.add(key);
						}
					}
					for (String tr : toRemove) {
						threads.remove(tr);
					}
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}				
			}
		});
		
		thread.start();
	}
	
	public Client() {
		threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
		init();
	}

	public boolean login(String username, String password) throws LoginException {

		try {
			PostMethod postMethod = new PostMethod("http://www.megaupload.com");
			postMethod.addParameter("login", "1");
			postMethod.addParameter("username", username);
			postMethod.addParameter("password", password);
			
			httpClient.executeMethod(postMethod);

			String content = postMethod.getResponseBodyAsString();
			
			if (content.contains("Welcome")) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			throw new LoginException(e);
		}
	}

	public static void main(String[] args) throws LinkBuckReslutionException {
		Client client = new Client();
		try {
//			System.setProperty("http.proxyHost", "dl03.inf.rtbf.be");
//			System.setProperty("http.proxyPort", "7878");
			//httpClient.getHostConfiguration().setProxy("dl03.inf.rtbf.be", 7878);
			client.login("et990219", "tttttt");
			String url = client.resolveLinkbuck("http://2cab6beb.ubucks.net/");
			System.out.println(url);
			DownloadContext downloadContext = client.getDownloadInfos("http://www.megaupload.com/?d=QJQESY28");
			System.out.println(downloadContext.getFilename());
			System.out.println(downloadContext.getTotalFileSize());
			System.out.println(downloadContext.getUrl());
			client.startCopy(downloadContext, new Callback<String, Object>() {

				@Override
				public void call(Map<String, Object> parameters) {
					DownloadContext downloadContext = (DownloadContext) parameters.get("downloadContext");
					System.out.println(String.format("%d/%d. Bandwith:%s", downloadContext.getCurrentFileSize(),downloadContext.getTotalFileSize(),downloadContext.getBandwidth()));
				}
			});
		} catch (LoginException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InfoRetrievalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public DownloadContext getDownloadInfos(String url) throws InfoRetrievalException {
		
		DownloadContext downloadContext = new DownloadContext();
		downloadContext.setDestDir(destDir);
		
		try {
			GetMethod getMethod = new GetMethod(url);
			DOMParser domParser = new DOMParser();
			httpClient.executeMethod(getMethod);
			domParser.parse(new InputSource(new InputStreamReader(getMethod.getResponseBodyAsStream())));
			final Document htmlDocument = domParser.getDocument();

			final XPath downloadXPath = XPathFactory.newInstance().newXPath();
			
			final NodeList downloadInfos = (NodeList) downloadXPath.evaluate("//TABLE[@height='125']/TR/TD/FONT", htmlDocument, XPathConstants.NODESET);
			final Node downloadLinkNode = (Node) downloadXPath.evaluate("//DIV[@id='downloadlink']/A", htmlDocument, XPathConstants.NODE);

			downloadContext.setUrl(FrenchLanguageUtils.removeURLSpecialChars(downloadLinkNode.getAttributes().getNamedItem("href").getNodeValue().trim()));
			downloadContext.setCurrentFileSize(0);
			
			String size = downloadInfos.item(5).getTextContent();
			long sizel = 0;
			if (size.endsWith("MB")) {
				sizel = (long) (Double.parseDouble(size.replaceAll("MB", "").replaceAll(" ", "")) * 1024);
			} else if (size.endsWith("GB")) {
				sizel = (long) (Double.parseDouble (size.replaceAll("GB", "").replaceAll(" ", "")) * 1024*1024);
			} else if (size.endsWith("KB")) {
				sizel = (long) Double.parseDouble(size.replaceAll("KB", "").replaceAll(" ", ""));
			}
			downloadContext.setTotalFileSize(sizel);
			downloadContext.setFilename(downloadInfos.item(1).getTextContent().trim());
			downloadContext.setBandwidth(0);
			
			return downloadContext;
			
		} catch (Exception e) {
			throw new InfoRetrievalException(e);
		} 
	}
	
	public void setDestDir(String destDir) {
		this.destDir = destDir;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void startCopy(DownloadContext downloadContext, Callback callback) {
		DownloadThread downloadThread = new DownloadThread();
		downloadThread.setCallback(callback);
		downloadThread.setDownloadContext(downloadContext);
		downloadThread.setHttpClient(httpClient);
		threads.put(downloadContext.getUrl(), downloadThread);
		threadPoolExecutor.execute(downloadThread);
	}
	
	public void configure(Properties properties) {
		//threadPoolExecutor.setMaximumPoolSize(properties.getMaxNumThreads());
		threadPoolExecutor.setCorePoolSize(properties.getMaxNumThreads());
		
		this.destDir = properties.getDestDirectory();
		if (properties.getProxy()) {
			System.setProperty("java.net.useSystemProxies", "false");
			httpClient.getHostConfiguration().setProxy(properties.getProxyUrl(), Integer.parseInt(properties.getProxyPort()));
		} else {
			System.setProperty("java.net.useSystemProxies", "true");
		}
	}
	
	@Override
	public void stop(DownloadContext downloadContext) {
		DownloadThread downloadThread = threads.get(downloadContext.getUrl());
		
		if (downloadThread != null)
			downloadThread.stop();
	}
	
	@Override
	public String resolveLinkbuck(String url) throws LinkBuckReslutionException {
		
		GetMethod getMethod = new GetMethod(url);
		try {
			DOMParser domParser = new DOMParser();
			httpClient.executeMethod(getMethod);
			domParser.parse(new InputSource(new InputStreamReader(getMethod.getResponseBodyAsStream())));
			final Document htmlDocument = domParser.getDocument();

			final XPath downloadXPath = XPathFactory.newInstance().newXPath();
			
			final Node downloadInfosNode = (Node) downloadXPath.evaluate("//A[@class='lb_link right']", htmlDocument, XPathConstants.NODE);
			
			return downloadInfosNode.getAttributes().getNamedItem("href").getTextContent().trim();
		} catch (Exception e) {
			throw new LinkBuckReslutionException(e);
		} 
	}
	
//	@Override
//	public void setMaxDownloads(int maxDownloads) {
//		threadPoolExecutor.setMaximumPoolSize(maxDownloads);
//	}
}
