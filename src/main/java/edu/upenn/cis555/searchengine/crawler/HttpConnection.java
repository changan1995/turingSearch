// package edu.upenn.cis555.searchengine.crawler;

// import java.net.Socket;

// public class HttpConnection{
//     public HttpConnection(String urlString){
//         Socket s = new Socket(urlString,);
// 		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
// 		BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
// 		// custom request headers
// 		out.write("HEAD " + path + " HTTP/1.1\r\n");
// 		out.write("Host: " + host + "\r\n");
// 		out.write("User-Agent: cis455crawler\r\n");
// 		out.write("\r\n");
// 		out.flush();
//     }
// }