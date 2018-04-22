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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.log4j.Logger;

import edu.upenn.cis555.searchengine.crawler.storage.DBWrapper;

public class URLFrontier {
	
	static Logger log = Logger.getLogger(URLFrontier.class);
	
	int numThreads;
	Queue<String> frontend;
	Queue<String>[] backends;
	HashMap<String, Integer> hostToQueue;
	PriorityBlockingQueue<TTR> releaseHeap;
	DBWrapper db;
	HashSet<Integer> emptyQueue = new HashSet<>();
	LinkedHashMap<String, Long> lastRelease;
	
	@SuppressWarnings("unchecked")
	public URLFrontier(int numThreads, List<String> seedURLs) {
		this.numThreads = numThreads;
		frontend = new LinkedList<>();
		hostToQueue = new HashMap<String, Integer>();
		releaseHeap = new PriorityBlockingQueue<>(150);
		backends = new Queue[3 * numThreads];
		lastRelease =  new LinkedHashMap<String, Long>() {
			@Override
			protected boolean removeEldestEntry(java.util.Map.Entry<String, Long> eldest) {
				return size() > 100;
			}
		};
		db = DBWrapper.getInstance();
		int emptyIdx = 0;
		for (int i = 0; i < 3 * numThreads; i++) {
			backends[i] = new LinkedList<String>();
		}
		
		for (String url : seedURLs) {
			if (emptyIdx < 3 * numThreads) {
				if (addToBackEnd(url, emptyIdx)) emptyIdx++;
			}
			else {
				frontend.add(url);
			}
		}
		
		for (String url : db.getURLs()) {
			if (emptyIdx < 3 * numThreads) {
				if (addToBackEnd(url, emptyIdx)) emptyIdx++;
			}
			else {
				frontend.add(url);
			}
		}
		
		while (emptyIdx < 3 * numThreads) {
			emptyQueue.add(emptyIdx);
			emptyIdx++;
		}
		
		TimerTask fillEmptyTask = new TimerTask() {
			@Override
			public void run() {
				HashSet<Integer> emptyQueue = URLFrontier.this.emptyQueue;
				HashMap<String, Integer> hostToQueue = URLFrontier.this.hostToQueue;
				synchronized (emptyQueue) {
					log.debug("Timer task: " + Crawler.num.get());
					log.error("Active thread:" + Thread.activeCount());
					Iterator<Integer> iter = emptyQueue.iterator();
					ArrayList<String> list = db.getURLs();
					for (String url : list) {
						try {
							URL u = new URL(url);
							String host = u.getHost();
							synchronized (hostToQueue) {
								if (hostToQueue.containsKey(host)) {
									backends[hostToQueue.get(host)].add(url);
									continue;
								} else if (iter.hasNext()) {
									int idx = iter.next();
									iter.remove();
									backends[idx].add(url);
									hostToQueue.put(host, idx);
									Long releaseTime = lastRelease.get(host);
									if (releaseTime == null)
										releaseHeap.put(new TTR(host, System.currentTimeMillis()));
									else 
										releaseHeap.put(new TTR(host, releaseTime.longValue() + 3000));
									continue;
								}
							}
							frontend.add(url);
						} catch (MalformedURLException e) {
						}
					}
					
//					while (iter.hasNext()) {
//						int idx = iter.next();
//						if (!frontToBack(idx)) {
//							iter.remove();
//						}
//					}
				}
			}
		};
		
		Timer timer = new Timer();
		// save urlseen every 3 seconds
		timer.scheduleAtFixedRate(fillEmptyTask, 100, 2 * 1000);
		
	}
	
	private boolean addToBackEnd(String url, int emptyIdx) {
		try {
			URL u = new URL(url);
			String host = u.getHost();
			if (hostToQueue.containsKey(host)) {
				backends[hostToQueue.get(host)].add(url);
				return false;
			} else {
				backends[emptyIdx].add(url);
				hostToQueue.put(host, emptyIdx);
				releaseHeap.put(new TTR(host, System.currentTimeMillis()));
				return true;
			}
		} catch (MalformedURLException e) {
			return false;
		}
	}
	
//	public TTR getNextAvailableHost() {
//		synchronized (releaseHeap) {
//			return releaseHeap.poll();
//		}
//	}
//	
//	public void putAvailableHost(TTR available) {
//		synchronized (releaseHeap) {
//			releaseHeap.add(available);
//		}
//	}
	
	public String getURL() throws InterruptedException {
		TTR release;
		synchronized (releaseHeap) {
			release = releaseHeap.take();
			System.out.println(release.host + ": " + release.releaseTime);
			System.out.println(releaseHeap.size());
		}
		long wait = release.releaseTime - System.currentTimeMillis();
		if (wait > 0) {
			System.out.println("wait");
			Thread.sleep(wait);
		}
		return retrieveBackQuene(release);
	}
	
	public String retrieveBackQuene(TTR release) {
		String host = release.host;
		synchronized (lastRelease) {
			lastRelease.put(host, System.currentTimeMillis());
		}
		int idx = hostToQueue.get(host);
		String url = backends[idx].poll();
		if (backends[idx].isEmpty()) {
			hostToQueue.remove(host);
			new Thread(() -> {
				if (!frontToBack(idx)) {
					synchronized (emptyQueue) {
						emptyQueue.add(idx);
					}
				}
			}).start();
		} else {
			// TODO change release time
			release.releaseTime = System.currentTimeMillis() + 3000;
			releaseHeap.put(release);
		}
		return url;
	}
	
	public synchronized boolean frontToBack(int idx) {
		// get url from front queue
		String s;
//		synchronized (frontend) {
			if (frontend.isEmpty()) {
				frontend.addAll(db.getURLs());
			}
			s = frontend.poll();
//		}
		if (s == null) {
			return false;
		}
		try {
			URL url = new URL(s);
			String host = url.getHost();
			if (hostToQueue.containsKey(host)) {
				backends[hostToQueue.get(host)].add(s);
				return false;
			} else {
				hostToQueue.put(host, idx);
				backends[idx].add(s);
//				synchronized (lastRelease) {
					Long releaseTime = lastRelease.get(host);
					if (releaseTime == null)
						releaseHeap.put(new TTR(host, System.currentTimeMillis()));
					else 
						releaseHeap.put(new TTR(host, releaseTime.longValue() + 3000));
//				}
				return true;
			}
		} catch (MalformedURLException e) {
			return false;
		}
	}

}
