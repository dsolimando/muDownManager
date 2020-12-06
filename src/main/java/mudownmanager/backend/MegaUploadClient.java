package mudownmanager.backend;

import java.io.InputStreamReader;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import mudownmanager.DownloadContext;

public class MegaUploadClient extends AbstractClient {

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

	@Override
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
}
