package edu.upenn.cis555.searchengine.crawler;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import edu.upenn.cis555.searchengine.crawler.storage.DBWrapper;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

public class URLDistributor {
	
	static Logger log = Logger.getLogger(URLDistributor.class);
	
	private static final int maxURLNum = 20;
	
	DBWrapper db;
	HashSet<String> urlSeen;
	HashMap<String, MyStringBuilder> buffers;
	String[] workerList;
	int index;
	
	private class MyStringBuilder {
		StringBuilder sb = new StringBuilder();
		int count = 0;
	}

	public URLDistributor(int index, String[] workerList) {
		
		// worker list
		this.workerList = workerList;
		this.index = index;
		for (int i = 0; i < workerList.length; i++) {
			if (i != index) {
				buffers.put(workerList[i], new MyStringBuilder());
			}
		}
		db = DBWrapper.getInstance();
		HashSet<String> set = null;
		try {
			set = db.getURLSeen();
		} catch(Exception e) {
			log.debug("Error get URLSeen.");
		}
		if (set != null) {
			urlSeen = set;
		} else {
			urlSeen = new HashSet<>();
		}
		log.debug("URLSeen size:" + set.size());
		
		TimerTask seenTask = new TimerTask() {
			@Override
			public void run() {
				db.saveURLSeen(urlSeen);
				log.debug("Saved all seen url to BDBs");
			}
		};
		
		Timer timer = new Timer();
		// save urlseen every 30 minutes
		timer.scheduleAtFixedRate(seenTask, 5 * 1000, 10 * 1000);
		
		Spark.post("/push", new Route() {

			@Override
			public Object handle(Request arg0, Response arg1) {
				String body = arg0.body();
				for (String url : body.split("\n")) {
					try {
						addURLToQueue(new URL(url));
					} catch (MalformedURLException e) {
					}
				}
				return "recieved";
			}

		});
	}
	
	
	private void addURLToQueue(URL url) {
		// check duplicate url
		String u = url.toString();
		if (!urlSeen.contains(u)) {
			// add url to queue
			log.debug("Recieved " + u);
			urlSeen.add(u);
			db.addURL(System.currentTimeMillis(), u);
		} 
	}
	
	
	public void distributeURL(String url) {
		try {
			URL u = new URL(url);
			int idx = Math.abs(u.getAuthority().hashCode()) % workerList.length;
			if (index == idx) {
				// still in the local node
				if (!urlSeen.contains(url)) {
					// add url to queue
					urlSeen.add(url);
					db.addURL(System.currentTimeMillis(), url);
				}
			} else {
				// should be sent to other node
				addToBuffer(workerList[idx], url);
			} 
		} catch (Exception e) {
			System.out.println("distribute url: " + url);
			e.printStackTrace();
			return;
		}
	}
	
	private void addToBuffer(String address, String url) {
		MyStringBuilder buf = buffers.get(address);
		synchronized (buf) {
			buf.sb.append(url + "\n");
			buf.count++;
			// if exceed the size, send to other node
			if (buf.count >= maxURLNum) {
				sendToWorker(address, buf.sb.toString());
				buf.sb.setLength(0);
				buf.count = 0;
			}
		}
	}
	
	private void sendToWorker(String address, String content) {
		try {
			URL url = new URL("http://" + address + "/push");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			
			// send this to /push as a POST!
			OutputStream os = conn.getOutputStream();
			byte[] toSend = content.getBytes();
			os.write(toSend);
			os.flush();
			conn.getResponseCode();
			log.debug("Sent urls to " + address);
			conn.disconnect();
		} catch (Exception e) {
			log.error("Sent urls to" + address + ": " + e.getMessage());
		}
		
	}
	
	
}
