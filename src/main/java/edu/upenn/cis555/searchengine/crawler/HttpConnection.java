package edu.upenn.cis555.searchengine.crawler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.util.HashMap;

public class HttpConnection{
    private Socket s;
    private BufferedWriter out;
    private BufferedReader reader;
    private String host;
    private HashMap<String,String> headers;

    public HttpConnection(String method,String urlString){
        s = null;
        host = null;
        try {
            URL url = new URL(urlString);
            host = url.getHost();
		} catch (MalformedURLException e1) {
            return;
        }
		try {
            s = new Socket(host,80);
            out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
		} catch (IOException e) {
            // e.printStackTrace();
            System.err.println("create connection error\t"+urlString);            
		}
		// custom request headers
		try {
            out.write(method+" "+ urlString + " HTTP/1.1\r\n");
            out.write("Host :"+host+"\r\n");
            out.write("User-Agent:cis455crawler\r\n");
            out.write("Accept-Language:en\r\n");
            out.write("content-language:en\r\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
            // e.printStackTrace();            
            System.err.println("print out the header");
		}
		// out.write("Host: " + host + "\r\n");
		// out.write("User-Agent: cis455crawler\r\n");
		// out.write("\r\n");
		// out.flush();
    }

    public void setConnectTimeout(int timeout){
        if(s!=null){
            try {
				s.setSoTimeout(timeout);
			} catch (SocketException e) {
				e.printStackTrace();
			}
        }
    }

    public int getResponseCode(){//send
        try {
			out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        String line;
		int count = 0;
		String preHeader = null;
		try {
			while ((line = reader.readLine()) != null) {
			count++;
			if (line.equals("")) {
				break;
			}
			if (count == 1) {
				int idx = line.indexOf(" ");
				String status = line.substring(idx + 1).trim();
				headers.put("status", status);
			} else {
				if (line.startsWith(" ") || line.startsWith("\t")) {
					headers.put(preHeader, headers.get(preHeader)
							+ line.trim());
				} else {
					int idx = line.indexOf(':');
					if (idx == -1) {
						continue;
					}
					String header = line.substring(0, idx).trim().toLowerCase();
					preHeader = header;
					String value = line.substring(idx + 1).trim();
					if (headers.containsKey(header)) {
						headers.put(header, headers.get(header) + "," + value);
					}
					else {
						headers.put(header, value);
					}
				}
				
			}
        }
		} catch (IOException e) {
			return 500;
		}
        return Integer.parseInt(headers.get("status"));
    }

    public BufferedReader getReader(){
        return reader;
    }

    public void disconnect(){
        try {
			s.close();
		} catch (IOException e) {
			// error on closing.
		}
    }

    public String getHeaderField(String header){
        return headers.get(header);
    }
}