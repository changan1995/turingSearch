package edu.upenn.cis555.searchengine.crawler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.upenn.cis555.searchengine.crawler.storage.DBWrapper;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

public class URLDistributor {
	
	class URLList {
		LinkedList<String> list;
		public URLList() {
			list = new LinkedList<>();
		}
		
	} 
	
	static Logger log = Logger.getLogger(URLDistributor.class);
	
	// flush buffer when exceed this limit
	private static final int maxURLNum = 50;
	
	DBWrapper db;
	HashMap<String, URLList> buffers;
	Random random = new Random();
	String[] workerList;
	int index;
	final ObjectMapper om = new ObjectMapper();
	URLFrontier frontier;
	
	public URLDistributor(int index, String[] workerList, URLFrontier frontier) {
		
		// worker list
		this.workerList = workerList;
		this.index = index;
		this.frontier = frontier;
		om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
		buffers = new HashMap<>();
		for (int i = 0; i < workerList.length; i++) {
			if (i != index) {
				buffers.put(workerList[i], new URLList());
			}
		}
		db = DBWrapper.getInstance();
//		try {
//			set = db.getURLSeen();
//		} catch(Exception e) {
//			log.debug("Error get URLSeen.");
//		}
//		if (set != null) {
//			urlSeen = set;
//		} else {
//			urlSeen = new HashSet<>();
//		}
//		log.debug("URLSeen size:" + set.size());
		
//		TimerTask seenTask = new TimerTask() {
//			@Override
//			public void run() {
//				db.saveURLSeen(urlSeen);
//				log.debug("Saved all seen url to BDBs");
//			}
//		};
//		
//		Timer timer = new Timer();
//		// save urlseen every 30 minutes
//		timer.scheduleAtFixedRate(seenTask, 5 * 1000, 10 * 1000);
		
		Spark.post("/push", new Route() {

			@Override
			public Object handle(Request arg0, Response arg1) {
				try {
					URLList list = om.readValue(arg0.body(), URLList.class);
					for (String url : list.list){
						try {
							addURLToQueue(url);
							// log.debug("Recieved " + url);
						} catch(Exception e) {
							continue;
						}
					}
				} catch (JsonParseException e) {
				} catch (JsonMappingException e) {
				} catch (IOException e) {
				}
				return "recieved";
			}

		});
	}
	
	
	private void addURLToQueue(String url) throws MalformedURLException {
		// check duplicate url
		if (!db.checkURLSeen(url)) {
			// add url to queue
			db.saveURLSeen(url);
			String host = new URL(url).getHost();
			if (!frontier.hasHost(host) && !frontier.hitUpperBound()) {
				// add to in memory queue
				frontier.addURLToHead(url);
			} else {
				// add to BDB
				db.addURL(random.nextLong(), url);
			}
		} 
	}
	
	
	public void distributeURL(String url) {
		try {
			URL u = new URL(url);
			int idx = Math.abs(u.getHost().hashCode()) % workerList.length;
			if (index == idx) {
				// still in the local node
				addURLToQueue(url);
			} else {
				// should be sent to other node
				addToBuffer(workerList[idx], url);
			} 
		} catch (Exception e) {
			log.error("Distribute url: " + url + " " + e.getMessage());
			return;
		}
	}
	
	private void addToBuffer(String address, String url) {
		URLList buf = buffers.get(address);
		synchronized (buf) {
			buf.list.add(url);
//			log.debug(address + " buf size:" + buf.list.size());
			// if exceed the size, send to other node
			if (buf.list.size() >= maxURLNum) {
				sendToWorker(address, buf);
				buffers.put(address, new URLList());
			}
		}
	}
	
	private void sendToWorker(String address, URLList content) {
		try {
			URL url = new URL("http://" + address + "/push");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			
			// send this to /push as a POST!
			OutputStream os = conn.getOutputStream();
			String jsonForList = om.writerWithDefaultPrettyPrinter().writeValueAsString(content);
			byte[] toSend = jsonForList.getBytes();
			os.write(toSend);
			os.flush();
			conn.getResponseCode();
			// log.debug("Sent urls to " + address);
			conn.disconnect();
		} catch (Exception e) {
			log.error("Sent urls to " + address + ": " + e.getMessage());
		}
		
	}
	
	
}
