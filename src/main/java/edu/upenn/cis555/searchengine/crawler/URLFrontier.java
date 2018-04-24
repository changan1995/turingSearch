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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import edu.upenn.cis555.searchengine.crawler.storage.DBWrapper;

public class URLFrontier {
	
	static Logger log = Logger.getLogger(URLFrontier.class);
	Random random =new Random();
	int numThreads;
	ConcurrentLinkedQueue<String> frontend;
	Queue<String>[] backends;
	ConcurrentHashMap<String, Integer> hostToQueue;
	PriorityBlockingQueue<TTR> releaseHeap;
	DBWrapper db;
	HashSet<Integer> emptyQueue = new HashSet<>();
	LinkedHashMap<String, Long> lastRelease = new LinkedHashMap<String, Long>(6 * numThreads * 5, (float) 0.75, true) {
		private static final long serialVersionUID = 2009731084826885027L;

		@Override
		protected boolean removeEldestEntry(java.util.Map.Entry<String, Long> eldest) {
			return size() > 6 * numThreads * 5;
		}
	};
	ConcurrentHashMap<String, Integer> delayCache;
	private long initTime=System.currentTimeMillis();
	
	int upperLimit;
	
	@SuppressWarnings("unchecked")
	public URLFrontier(int numThreads, List<String> seedURLs) {
		this.numThreads = numThreads;
		int maxHostNum = numThreads*4;
		frontend = new ConcurrentLinkedQueue<>();
		hostToQueue = new ConcurrentHashMap<String, Integer>();
		releaseHeap = new PriorityBlockingQueue<>(maxHostNum);
		backends = new Queue[maxHostNum];
		upperLimit = numThreads * 100;
		lastRelease =  new LinkedHashMap<String, Long>(maxHostNum * 5, (float) 0.75, true) {
			private static final long serialVersionUID = 2009731084826885027L;

			@Override
			protected boolean removeEldestEntry(java.util.Map.Entry<String, Long> eldest) {
				return size() > maxHostNum * 5;
			}
		};
		delayCache =  new ConcurrentHashMap<>();
		db = DBWrapper.getInstance();
		int emptyIdx = 0;
		for (int i = 0; i < maxHostNum; i++) {
			backends[i] = new LinkedList<String>();
		}
		
		for (String url : seedURLs) {
			if (emptyIdx < maxHostNum) {
				if (addToBackEnd(url, emptyIdx)) emptyIdx++;
			}
			else {
				frontend.add(url);
			}
		}
		
		for (String url : db.getURLs(-1)) {
			if (emptyIdx < maxHostNum) {
				if (addToBackEnd(url, emptyIdx)) emptyIdx++;
			}
			else {
				frontend.add(url);
			}
		}
		
		while (emptyIdx < maxHostNum) {
			emptyQueue.add(emptyIdx);
			emptyIdx++;
		}
		
		TimerTask fillEmptyTask = new TimerTask() {
			@Override
			public void run() {
				HashSet<Integer> emptyQueue = URLFrontier.this.emptyQueue;
				ConcurrentHashMap<String, Integer> hostToQueue = URLFrontier.this.hostToQueue;
				ConcurrentLinkedQueue<String> frontend = URLFrontier.this.frontend;
				
				log.debug("Empty Queue" + emptyQueue.size());
				log.debug("FrontQueue size:" + frontend.size());
				log.debug("Last release LRU: " + lastRelease.size());
				log.debug("Crawled docs: " + Crawler.num.get());
				log.error("Active thread:" + Thread.activeCount());
				log.debug("Release heap size: " + releaseHeap.size());
		

//				if (frontend.size() > 0.8 * upperLimit) {
//					return;
//				}
				if (releaseHeap.size() >= 0.75 * maxHostNum) {
					return;
				}
				
				ArrayList<String> list = db.getURLs(50);
				
				String uF;
				int limit = 20;
				if (frontend.size() > 0.8 * upperLimit) {
					limit = 50;
				}
				int count = 0;
				while((uF = frontend.poll()) != null) {
					list.add(uF);
					count++;
					if (count >= limit) {
						break;
					}
				}
				HashMap<String, LinkedList<String>> map = new HashMap<>();
				for (String url : list) {
					URL u;
					try {
						u = new URL(url);
						String host = u.getHost();
						LinkedList<String> hostList = map.getOrDefault(host, new LinkedList<>());
						hostList.add(url);
						map.put(host, hostList);
					} catch (MalformedURLException e) {
					}
				}
				synchronized (emptyQueue) {
					Iterator<Integer> iter = emptyQueue.iterator();
					while(iter.hasNext()) {
						int idx = iter.next();
						String toRemove = null;
						for (String host : map.keySet()) {
							if (hostToQueue.containsKey(host)) {
								int hostIdx = hostToQueue.get(host);
								if (backends[hostIdx].size() > 200) {
									continue;
								}
								backends[hostIdx].addAll(map.get(host));
								continue;
							} else {
								iter.remove();
								backends[idx].addAll(map.get(host));
								toRemove = host;
								hostToQueue.put(host, idx);
								Long releaseTime = lastRelease.get(host);
								if (releaseTime == null)
									releaseHeap.put(new TTR(host, System.currentTimeMillis()));
								else {
//									try {
										long time = releaseTime.longValue() + getDelay(host) * 1000;
										releaseHeap.put(new TTR(host, time));
//									} catch(Exception e) {
//										continue;
//									}
								}
								break;
							}
						}
						if (toRemove != null) {
							map.remove(toRemove);
						}
					}
					
//					for (String url : list) {
//						try {
//							URL u = new URL(url);
//							String host = u.getHost();
////							int delay = Crawler.rule.getDelay(host);
////							log.debug("Delay("+ host + "): " + delay);
////							synchronized (hostToQueue) {
//								if (hostToQueue.containsKey(host)) {
////									log.debug("Has item put" + host + " to " + hostToQueue.get(host));
////									log.debug("Queue of " + host + ":" + backends[hostToQueue.get(host)].size());
//									if (backends[hostToQueue.get(host)].size() > 200) {
//										continue;
//									}
////									else
//									backends[hostToQueue.get(host)].add(url);
//									continue;
//								} else if (iter.hasNext()) {
//									int idx = iter.next();
//									iter.remove();
////									log.debug("Empty put" + host + " to " + idx);
//									backends[idx].add(url);
//									hostToQueue.put(host, idx);
//									Long releaseTime = lastRelease.get(host);
//									if (releaseTime == null)
//										releaseHeap.put(new TTR(host, System.currentTimeMillis()));
//									else {
//										long time = releaseTime.longValue() + getDelay(host) * 1000;
//										releaseHeap.put(new TTR(host, time));
//									}
//									continue;
//								}
////							}
//							frontend.add(url);
//						} catch (Exception e) {
//							continue;
//						}
//					}
					
				}
			}
		};
		
		Timer timer = new Timer();
		// save urlseen every 2 seconds
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
	// private AtomicInteger i;	

	public String getURL() throws InterruptedException {
		
		TTR release;
		synchronized (releaseHeap) {
			release = releaseHeap.take();
			// log.debug("Release heap size: " + releaseHeap.size());
		}
		long wait = release.releaseTime - System.currentTimeMillis();
		if (wait > 0) {
			log.debug("wait");
			Thread.sleep(wait);
		}
		String returnString = retrieveBackQuene(release);
		
		return returnString;
	}
	
	public String retrieveBackQuene(TTR release) {
		String host = release.host;
		synchronized (lastRelease) {
			lastRelease.put(host, System.currentTimeMillis());
		}
		int idx = hostToQueue.get(host);
		String url = backends[idx].poll();
		if (backends[idx].isEmpty()) {
//			synchronized (hostToQueue) {
				hostToQueue.remove(host);
//			}
			// new Thread(() -> {
			// 	if (!frontToBack(idx)) {
			 		synchronized (emptyQueue) {
						emptyQueue.add(idx);
			 		}
			// 	}
			// }).start();
		} else {
			// TODO change release time
			release.releaseTime = System.currentTimeMillis() + getDelay(host) * 1000;
			releaseHeap.put(release);
		}
		// log.debug("Get " + url);
		return url;
	}
	
// 	public synchronized boolean frontToBack(int idx) {
// 		// get url from front queue
// 		String s;
// 		synchronized (frontend) {
// 			if (frontend.isEmpty()) {
// 				frontend.addAll(db.getURLs(-1));
// 			}
// 			s = frontend.poll();
// 		}
// 		if (s == null) {
// 			return false;
// 		}
// 		try {
// 			URL url = new URL(s);
// 			String host = url.getHost();
// 			if (hostToQueue.containsKey(host)) {
// 				backends[hostToQueue.get(host)].add(s);
// 				return false;
// 			} else {
// 				hostToQueue.put(host, idx);
// 				backends[idx].add(s);
// //				synchronized (lastRelease) {
// 					Long releaseTime = lastRelease.get(host);
// 					if (releaseTime == null)
// 						releaseHeap.put(new TTR(host, System.currentTimeMillis()));
// 					else {
// 						long time = releaseTime.longValue() + getDelay(host) * 1000;
// 						releaseHeap.put(new TTR(host, time));
// 					}
// //				}
// 				return true;
// 			}
// 		} catch (MalformedURLException e) {
// 			return false;
// 		}
	// }
	
	private int getDelay(String host) {
		if (delayCache.containsKey(host)) {
			return delayCache.get(host);
		} else {
			int delay = 2;
			try {
				delay = Crawler.rule.getDelay(host);
			} catch(Exception e) {
			}
			return delay;
		}
	}
	
	public void addURLToHead(String url) {
			frontend.add(url);
	}
	
	public boolean hasHost(String host) {
		// synchronized (hostToQueue) {
			return hostToQueue.containsKey(host);
		// }
	}
	
	public int getFrontend() {
		return frontend.size();
	}
	
	public boolean hitUpperBound() {
		// synchronized (frontend) {
			return frontend.size() >= upperLimit*0.3;
		// }
	}

}
