package edu.upenn.cis555.searchengine.crawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;

import edu.upenn.cis555.searchengine.crawler.info.URLInfo;;

public class HttpClient {

	private boolean responseSucces = false; // response whether success
	private URL url = null; // url
	private URLInfo urlInfo = null; // url info parser
	private boolean https; // whether https or not
	private String body; // string body
	private Map<String, List<String>> headers;// headers mapper

	// content variable
	private int contentLength = 0;
	private String contentType = "null";
	private long lastModified = 0;
	private boolean english = false; // whether english or not

	static Logger log = Logger.getLogger(HttpClient.class);

	public HttpClient() {
		// TODO: redirect tohandle

	}

	public boolean distributeUrl(String urlString , byte[] content) throws SocketTimeoutException{
		URL url=null;
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			return false;
		}
		HttpURLConnection conn=null;
		try {
			conn = (HttpURLConnection) url.openConnection();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return false;
			
		}
		HttpURLConnection.setFollowRedirects(false);
		conn.setConnectTimeout(2 * 1000);
		try {
			conn.setRequestMethod("POST");
		} catch (ProtocolException e) {
			// TODO Auto-generated catch block
			return false;
			
		}
		OutputStream out =null;
		try {
			conn.setDoOutput(true);
			out = conn.getOutputStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return false;
			
		}
		try {
			out.write(content);
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return false;
			
		}
		try {
			if ((responseSucces = conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
				return true;
			// get response headers
			// Map<String, List<String>> headers = conn.getHeaderFields();
			// // headders test
			// for (String key : headers.keySet()) {
			// System.out.println(key + "--->" + headers.get(key));
			// }
		}else{
			return false;
		}
		} catch (IOException e) {
			e.printStackTrace();
			// TODO Auto-generated catch block
			return false;
			
		}finally{
			return false;
		}
	}

	// both send from here
	public boolean send(String method, String urlString) {
		https = urlString.toLowerCase().startsWith("https://");
		urlInfo = new URLInfo(urlString);
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

		try {
			if (https) {
				return httpsSend(method, url);
			} else {
				return httpSend(method, url);
			}
		} catch (Exception e) {
			return false;
		}
		// return responseSucces;

	}

	public boolean httpSend(String method, URL url) throws SocketTimeoutException {
		body = "";
		BufferedReader in = null;
		try {
			// get connection
			HttpConnection conn = new HttpConnection(method, url.toString());
			HttpURLConnection.setFollowRedirects(false);
			conn.setConnectTimeout(5 * 1000); 
			// conn.setRequestMethod(method);
			// // set headers
			// conn.setRequestProperty("accept", "*/*");
			// conn.setRequestProperty("connection", "Keep-Alive");
			// conn.setRequestProperty("content-language", "en");
			// conn.setRequestProperty("user-agent", "cis455crawler");
			// conn.setRequestProperty("Accept-Language", "en");

			// connect
			if ((responseSucces = conn.getResponseCode() == 200)) {
				String lang = conn.getHeaderField("content-language");
				if (lang != null)
					if (!lang.contains("en")) return false;
				contentLength = Integer.parseInt(conn.getHeaderField("Content-Length"));
				contentType = conn.getHeaderField("Content-Type");

				// get inputstream reader
				// in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				in = conn.getReader();
				String line;
				while ((line = in.readLine()) != null) {
					body += "\n" + line;
				}
				// get response headers
				// Map<String, List<String>> headers = conn.getHeaderFields();
				// // headders test
				// for (String key : headers.keySet()) {
				// System.out.println(key + "--->" + headers.get(key));
				// }
			}
		} catch (Exception e) {
			log.debug("error in send " + method + ": " + url.toString() + " " + e.getMessage());
			return false;
		}
		// close in put stream
		finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
				return false;
			}
		}

		// afte get the response handle the response headers & content

		
		// sendUDP(url.toString());
		return true;
	}

	public boolean httpsSend(String method, URL url) throws SocketTimeoutException {
		body = "";
		BufferedReader in = null;
		HttpsURLConnection httpsconn = null;
		try {
			httpsconn = (HttpsURLConnection) url.openConnection();
			HttpsURLConnection.setFollowRedirects(false);
			httpsconn.setConnectTimeout(2 * 1000);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		try {
			httpsconn.setRequestMethod(method);
		} catch (ProtocolException e) {
			e.printStackTrace();
		}
		httpsconn.setRequestProperty("Host", url.getHost());
		httpsconn.setRequestProperty("User-Agent", "cis455crawler");
		httpsconn.setRequestProperty("Accept-Language", "en");
		httpsconn.setRequestProperty("content-language", "en");
		

		try {
			// check response code
			if (httpsconn.getResponseCode() != 200) {
				httpsconn.disconnect();
				return false;
			}
			String lang = httpsconn.getHeaderField("content-language");
			if (lang != null)
				if (!lang.contains("en")) return false;
				
			in = new BufferedReader(new InputStreamReader(httpsconn.getInputStream(), "UTF-8"));
			
			contentLength = httpsconn.getContentLength();
			contentType = httpsconn.getContentType();
			lastModified = httpsconn.getLastModified();

			String line;
			while ((line = in.readLine()) != null) {
				body += "\n" + line;
			}

		} catch (Exception e) {
			return false;
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
				return false;
			}
		}

		// sendUDP(url.toString());		
		return true;

	}

	public void sendUDP(String urlString){
			// UDP send
	byte[] data = ("changanw;"+urlString).getBytes();
	DatagramPacket packet = new DatagramPacket(data, data.length,
	Crawler.host, 10455);
	try {
		Crawler.s.send(packet);
	} catch (IOException e1) {
	System.err.println("UDP failed of urlString:"+urlString);
	}
	}

	/**
	 * @return the contentLength
	 */
	public int getContentLength() {
		return contentLength;
	}

	/**
	 * @param contentLength
	 *            the contentLength to set
	 */
	public void setContentLength(int contentLength) {
		this.contentLength = contentLength;
	}

	/**
	 * @return the contentType
	 */
	public String getContentType() {
		return contentType;
	}

	/**
	 * @param contentType
	 *            the contentType to set
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	/**
	 * @return the lastModified
	 */
	public long getLastModified() {
		return lastModified;
	}

	/**
	 * @param lastModified
	 *            the lastModified to set
	 */
	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	/**
	 * @return the english
	 */
	public boolean isEnglish() {
		return english;
	}

	/**
	 * @param english
	 *            the english to set
	 */
	public void setEnglish(boolean english) {
		this.english = english;
	}

	public String getContent() {
		return this.body;
	}


	public static void main(String[] args){
		HttpClient hc = new HttpClient();
		System.out.println(hc.send("GET", "https://en.wikipedia.org/wiki/Turbine")==true);
	}

}