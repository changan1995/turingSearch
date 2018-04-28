package edu.upenn.cis555.searchengine.crawler;

import java.io.*;
import java.net.DatagramPacket;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;

import edu.upenn.cis555.searchengine.crawler.info.URLInfo;
import edu.upenn.cis555.searchengine.crawler.structure.URLEntry;;

public class HttpClient {
	// private final String URL = "www.baidu.com";
	// private final String request = "GET http://www.baidu.com/ HTTP/1.1\r\nHost:
	// www.baidu.com\r\n\r\n" ;
	// private final int PORT = 80 ;
	// public int contentLength=-1;
	// public String contentType=null;
	// public long lastModified = -1;
	// public String request = null; //request is the raw string to send
	// public String buffer;//for split the string
	// public HttpsURLConnection httpsconn=null;
	// public String responseRaw=null;
	// public int timeout = 9999999;//lol
	// public InputStream txtin=null;//read txt from https
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

	// both send from here
	public boolean send(String method, String urlString) {
		// url string testing

		// String urlString = urlEntry.getUrl().toString();
		//
		// //black list
		//
		//
		// long toCrawlTime = urlEntry.getToCrawlDate();
		// long currentTime = System.currentTimeMillis();
		// long wait =(toCrawlTime-currentTime)/10;//TODO: some how 10 times??
		// if(wait>0){
		// try {
		// // System.out.println(Crawler.urlToDo.size());//debug size
		// Thread.sleep(wait);
		// } catch (InterruptedException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }
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
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(5 * 1000);
			conn.setRequestMethod(method);
			// set headers
			conn.setRequestProperty("accept", "*/*");
			conn.setRequestProperty("connection", "Keep-Alive");
			conn.setRequestProperty("user-agent", "cis455crawler");
			conn.setRequestProperty("Accept-Language", "en");

			// connect
			if ((responseSucces = conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
				String lang = conn.getHeaderField("content-language");
				if (lang != null)
					if (!lang.contains("en")) return false;
				contentLength = conn.getContentLength();
				contentType = conn.getContentType();
				lastModified = conn.getLastModified();

				// get inputstream reader
				in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
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

	// //text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8
	// this.request=method+" "+urlString+" HTTP/1.1\r\nHost:
	// "+urlInfo.getHostName()+"\r\nUser-Agent: cis455crawler\r\n\r\n";
	// StringBuilder sb =new StringBuilder();
	// Socket socket =null;
	// OutputStream out =null;
	// BufferedReader in =null;



	// try {
	// //handle https
	// String line;
	// if(!https){
	// socket = new Socket(urlInfo.getHostName(),urlInfo.getPortNo());
	// socket.setSoTimeout(timeout); //set socket
	// out = socket.getOutputStream();
	// in = new BufferedReader(new
	// InputStreamReader(socket.getInputStream(),"UTF-8"));
	// out.write(request.getBytes()); //send
	// out.flush();
	// line=in.readLine();
	// if(!line.startsWith("HTTP/1.1 200")){//todo redirect
	// socket.close();
	// return false;
	// }else{
	// sb.append(line);
	// }

	// out.write(request.getBytes()); //send
	// out.flush();
	// }else{
	// //https has own way of set headers, and content doesnt include headers.

	// httpsconn = (HttpsURLConnection) (new URL(urlString)).openConnection();
	// httpsconn.setRequestMethod("GET");
	// httpsconn.setRequestProperty("Host", (new URL(urlString)).getHost());
	// httpsconn.setRequestProperty("User-Agent", "cis455crawler");

	// try{
	// if(httpsconn.getResponseCode()!=200){
	// httpsconn.disconnect();
	// return false;
	// }
	// in= new BufferedReader(new
	// InputStreamReader(httpsconn.getInputStream(),"UTF-8"));

	// this.contentLength=httpsconn.getContentLength();
	// this.contentType=httpsconn.getContentType();
	// this.lastModified=httpsconn.getLastModified();

	// }catch(Exception e ){
	// return false;
	// }

	// }

	// boolean hasContent = false;
	// int contentLength = 0;
	// StringBuilder body =new StringBuilder();
	// if(!https){
	// while (!(line = in.readLine()).equals(""))
	// {//http://www.facilities.upenn.edu/robots.txt wont finish???
	// sb.append("\r\n" + line);
	// final String contentHeader = "Content-Length: ";
	// if (line.startsWith(contentHeader)) {
	// hasContent=true;
	// contentLength = Integer.parseInt(line.substring(contentHeader.length()));
	// }
	// }
	// if(method.equals("GET")){
	// if (hasContent) {
	// int c = 0;
	// for (int i = 0; i < contentLength; i++) {
	// c = in.read();
	// body.append((char) c);
	// }
	// }
	// }
	// }else{
	// while (true) {//http://www.facilities.upenn.edu/robots.txt wont finish???
	// line= in.readLine();
	// if(line ==null){break;}
	// if(line == ("")){break;}
	// sb.append("\r\n" + line);
	// final String contentHeader = "Content-Length: ";
	// if (line.startsWith(contentHeader)) {
	// hasContent=true;
	// contentLength = Integer.parseInt(line.substring(contentHeader.length()));
	// }
	// }
	// if(method.equals("GET")){
	// if (hasContent) {
	// int c = 0;
	// for (int i = 0; i < contentLength; i++) {
	// c = in.read();
	// body.append((char) c);
	// }
	// }
	// }
	// }

	// sb.append("\r\n\r\n");//sb = headers+ \r\n\r\n + body
	// sb.append(body.toString());

	// if(!https&&socket!=null){
	// socket.close();
	// }else if(https&&httpsconn!=null){
	// httpsconn.disconnect();
	// }
	// } catch (SocketTimeoutException e) {
	// System.out.println(urlString+"\t time out");
	// if(urlString.startsWith("http://")){//if http not available try https
	// return send(method,urlString+"https://"+urlString.substring(7));
	// }else{
	// return false;
	// }
	// // e.printStackTrace();
	// }catch (UnknownHostException e) {
	// e.printStackTrace();
	// } catch (IOException e) {
	// // System.out.println(urlString+"\t not found or IO error");
	// e.printStackTrace();
	// return false;

	// }

	// if(!https){// http request has some headers in the first, split by \r\n\r\n
	// this.buffer = sb.toString();
	// HttpResponse hr = new HttpResponse(buffer);
	// // if(hr.getstatusCode()!="200"){
	// // return false;
	// // }
	// // if(hr.getHeaders("Content-Length")==null){
	// // return false;
	// // }
	// if(hr.getHeaders("Content-Length")==null){
	// return false;
	// }
	// this.contentLength=Integer.parseInt(hr.getHeaders("Content-Length"));
	// this.contentType=hr.getHeaders("Content-Type");
	// String lastmodified =hr.getHeaders("Last-Modified");
	// this.lastModified=(lastmodified==null)?new
	// Long(0):Utilities.convertDate(lastmodified);
	// this.responseRaw= buffer.split("\r\n\r\n",2)[1];
	// return true;
	// }else{
	// this.responseRaw= sb.toString();
	// return true;
	// }

	// }
	// public String toString(){
	// return this.responseRaw;
	// }

}