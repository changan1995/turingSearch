package edu.upenn.cis555.searchengine.crawler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
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
import edu.upenn.cis555.searchengine.crawler.structure.URLList;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

public class URLReciver{
	
	
	// private static final long serialVersionUID = -1715283408490447605L;
	
	static Logger log = Logger.getLogger(URLReciver.class);
	
	// flush buffer when exceed this limit
	private static final int maxURLNum = 500;
	
	DBWrapper db;
	HashMap<String, URLList> buffers;
	Random random = new Random();
	String[] workerList;
	int index;
	final ObjectMapper om = new ObjectMapper();
	URLFrontier frontier;
	
	public URLReciver(int index, String[] workerList, URLFrontier frontier) {
		
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
					log.debug("get request");
					URLList list = om.readValue(arg0.body(), URLList.class);

					 log.debug("Recieved " + list.list.size());					
					for (String url : list.list){
						try {
							addURLToQueue(url);
							// log.debug("Recieved " + url);
						} catch(Exception e) {
							continue;
						}
					}
				} catch (Exception e) {
					log.debug(e.getMessage());
					e.printStackTrace();
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
				db.addURL(System.currentTimeMillis(), url);
			}
		} 
	}
	
	
	
}
