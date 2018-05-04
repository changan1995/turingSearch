package edu.upenn.cis555.searchengine.crawler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.upenn.cis555.searchengine.crawler.storage.DBWrapper;

public class URLFrontier {
	
	static Logger log = Logger.getLogger(URLFrontier.class);
	Random random =new Random();
	int numThreads;
	// public static Lock lock = new ReentrantLock();
	// ConcurrentLinkedQueue<String> frontend;
	ConcurrentHashMap<String, Queue<String>> hostToQueue = new ConcurrentHashMap<String, Queue<String>>();
	PriorityBlockingQueue<TTR> hostQueue;
	public static Pattern whileLst = Pattern.compile("wikipedia|cnn|nytimes|espn|yahoo|reddit|apache|.org");	
	DBWrapper db;
	// LinkedHashMap<String, Long> lastRelease = new LinkedHashMap<String, Long>(6 * numThreads * 5, (float) 0.75, true) {
	// 	private static final long serialVersionUID = 2009731084826885027L;

	// 	@Override
	// 	protected boolean removeEldestEntry(java.util.Map.Entry<String, Long> eldest) {
	// 		return size() > 6 * numThreads * 5;
	// 	}
	// };
	// ConcurrentHashMap<String, Integer> delayCache;
	// private long initTime=System.currentTimeMillis();
	// int hostSize = numThreads *15;
	private int maxQueueNum = 20; 
	private int maxHostNum;
	
	// int upperLimit;
	
	@SuppressWarnings("unchecked")
	public URLFrontier(int numThreads, List<String> seedURLs) {
		this.numThreads = numThreads;
		maxHostNum= Crawler.hostNum;
		hostToQueue = new ConcurrentHashMap<String, Queue<String>>();
		hostQueue = new PriorityBlockingQueue<>(maxHostNum);
		db = DBWrapper.getInstance();
		int emptyIdx = 0;
		long currentTime = System.currentTimeMillis();
		
		db.bulidBL();
		// fill the queues with seeds.
		for (String url : seedURLs) {
			if(!this.addUrl(url)){
				log.error("add url error");
				db.addURL(System.currentTimeMillis(), url);
				Crawler.num.incrementAndGet();
				
			}
		}
		
		// fill the rest with db queues, may not be at spreaded hosts;	
		for (String url : db.getURLs(((maxHostNum-emptyIdx)/50))) {
			if(!this.addUrl(url)){
				log.error("add url error");
				db.addURL(System.currentTimeMillis(), url);
				Crawler.num.incrementAndGet();
			}

		}
		
		
		TimerTask fillEmptyTask = new TimerTask() {
			@Override
			public void run() {
				
				// log.debug("Empty Queue" + emptyQueue.size());
				log.debug("BDB size:" + db.getFrontierCount());
				// log.debug("Last release LRU: " + lastRelease.size());
				log.debug("Crawled docs: " + Crawler.num.get());
				
				log.debug("Release heap size: " + hostQueue.size());
		
			}
		};
		
		Timer timer = new Timer();
		// save urlseen every 2 seconds
		timer.scheduleAtFixedRate(fillEmptyTask, 100, 2 * 1000);
		
	}
	

	public String getURL() throws Exception {
		int hostQueueCount = hostQueue.size();
		if(hostQueueCount<maxHostNum*0.15){
			for (String url : db.getURLs((maxHostNum-hostQueueCount)/50)) {
				if(!this.addUrl(url)){
					log.error("add url error");
					db.addURL(System.currentTimeMillis(), url);				
					Crawler.num.incrementAndGet();	
				}
	
				// else {
				// 	frontend.add(url);
				// }
			}
		}


		TTR release;
		if((release = hostQueue.take())==null){//blocking queue is currently blocking
			return null;
		}			
		if(release.getCount()<0){
			hostToQueue.remove(release.getHost());
			return null;
		}else{
			release.decrement();
		}
		// log.debug("Release heap size: " + hostQueue.size());
		
		long wait = release.getReleasTime() - System.currentTimeMillis();
		if (wait > 0) {
			log.debug("wait");
			Thread.sleep(wait);
		}
//		log.error("Active thread:" + Thread.activeCount());
		String returnString = null;
		String host = release.getHost();
		// synchronized (lastRelease) {
		// 	lastRelease.put(host, System.currentTimeMillis());
		// }
		Queue<String> queue =null;
		if((queue = hostToQueue.get(host))==null){//not in the hashmap
			return null;
		}else{//in the hashmap
			if(queue.size()>1){
				returnString = queue.poll();
				release.setReleaseTime(System.currentTimeMillis()+getDelay(host));
				hostQueue.add(release);				
				return returnString;
			}else if (queue.size()==1){
				returnString = queue.poll();
				return returnString;
			}else{
				hostToQueue.remove(host);
				return null;
			}
		}
	}
	
	
	private int getDelay(String host) throws Exception {
		// if (delayCache.containsKey(host)) {
		// 	return delayCache.get(host);
		// } else {
			int delay = 0;
			try {
				delay = Crawler.rule.getDelay(host); 
			} catch(Exception e) {
				return 0;
			}
			return delay;
		// }
	}
	
	public boolean addUrl(String url) {// add url to queue, call hashmap first then call queue.
		String host =null;
		Queue<String> queue = null;
		if((host=this.getHost(url))==null){ return false;}//get host name		
		if((queue = hostToQueue.get(url))==null){//not in the hashmap
			if(hostQueue.size()<maxHostNum){
				int count = 20;
				if(whileLst.matcher(host).find()){
					count =100000;
				}
				TTR ttr =new TTR(host, System.currentTimeMillis(),count);
				queue =new LinkedList<>();
				queue.add(url);
				hostToQueue.put(host,queue);
				hostQueue.add(ttr);
				return true;
			}else{
				return false;
			}
			//put new host to quque
		}else{//in the hashmap
			if(queue.size()<maxQueueNum){
				queue.add(url);
				//TODO : add to hashmap.
				return true;
			}else{
				return false;
			}
		}
		
	}
	
	public boolean hasHost(String host) {
		// synchronized (hostToQueue) {
			return hostToQueue.containsKey(host);
		// }
	}
	

	public String getHost(String url){
		String host=null;
		try{
			host = (new URL(url)).getHost();
		}catch(MalformedURLException e){
			//pass 
		}
		return host;
	}
}
