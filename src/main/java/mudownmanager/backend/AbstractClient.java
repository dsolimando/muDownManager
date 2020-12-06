package mudownmanager.backend;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import mudownmanager.Callback;
import mudownmanager.DownloadContext;
import mudownmanager.DownloadContext.Status;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.cyberneko.html.parsers.DOMParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public abstract class AbstractClient implements MUClient<String,Object> {

	protected ConcurrentHashMap<String, DownloadThread> threads = new ConcurrentHashMap<String, DownloadThread>();
	
	@Autowired
	protected HttpClient httpClient;
	
	@Autowired
	protected ExecutorService threadPoolExecutor;

	protected String destDir = "/tmp";
	
	public void init() {
		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					List<String> toRemove = new ArrayList<String>();
					for (String key : threads.keySet()) {
						if (threads.get(key).getThreadStatus() != Status.RUNNING && threads.get(key).getThreadStatus() != Status.IDLE) {
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
	
	public void startCopy(DownloadContext downloadContext, Callback<String,Object> callback) {
		DownloadThread downloadThread = new DownloadThread();
		downloadThread.setCallback((Callback<String, Object>) callback);
		downloadThread.setDownloadContext(downloadContext);
		downloadThread.setHttpClient(httpClient);
		threads.put(downloadContext.getUrl(), downloadThread);
		threadPoolExecutor.execute(downloadThread);
	}
	
	public void stop(DownloadContext downloadContext) {
		DownloadThread downloadThread = threads.get(downloadContext.getUrl());
		
		if (downloadThread != null)
			downloadThread.stop();
	}

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
}
