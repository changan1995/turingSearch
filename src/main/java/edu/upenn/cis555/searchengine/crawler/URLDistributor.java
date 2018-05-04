package edu.upenn.cis555.searchengine.crawler;

import static org.asynchttpclient.Dsl.post;

import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashMap;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.log4j.Logger;
import org.asynchttpclient.Request;

import edu.upenn.cis555.searchengine.crawler.storage.DBWrapper;
import edu.upenn.cis555.searchengine.crawler.structure.URLList;

public class URLDistributor{
	// private static final long serialVersionUID = -1715283408490447605L;
	
//	AsyncHttpClient c = asyncHttpClient(config().setProxyServer(proxyServer("127.0.0.1", 38080)));
	// AsyncHttpClient c = asyncHttpClient();
	
	static Logger log = Logger.getLogger(URLDistributor.class);
	
	// flush buffer when exceed this limit
	private static final int maxURLNum = 100;
	public static long urlSeenCount = 0;
	public static long urlFrontierCount=0;
	DBWrapper db;
	HashMap<String, URLList> buffers;
	String[] workerList;
	int index;
	final ObjectMapper om = new ObjectMapper();
	URLFrontier frontier;
	
	
	public boolean urlFrontierFull(){
		return db.getFrontierCount()>2000;
	}

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
	}
	
	
	private void addURLToQueue(String url) throws MalformedURLException {
		// check duplicate url
		if(this.urlFrontierFull()){
			return;
		}
		if (!db.checkURLSeen(url)) {
			// add url to queue
			if(!this.urlFrontierFull()){
				db.saveURLSeen(url);
			}
			String host = new URL(url).getHost();
			if (!frontier.addUrl(url)) {//return false on failure
					db.addURL(System.currentTimeMillis(), url);
					Crawler.num.incrementAndGet();
			}
		}
	}
    
    public int hashToIndex(int hash){
        for(int i =0;i<Crawler.hashList.length;i++){
            if(hash<Crawler.hashList[i]){
                return i;
            }
        }
        return 0;
    }


	public void distributeURL(Set<String> urls) {
		for (String url: urls ){
			try {
				URL u = new URL(url);
				String host = u.getHost();
                // int idx = Math.abs(host.hashCode()) % workerList.length;
                int idx = hashToIndex(Math.abs(host.hashCode()%100));
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
		return ;
		
	}
	
	private void addToBuffer(String address, String url) {
		URLList buf = buffers.get(address);
		// synchronized (buf) {
			buf.list.add(url);
//			log.debug(address + " buf size:" + buf.list.size());
			if (buf.list.size() >= maxURLNum) {
				sendToWorker(address, buf);
				buf.list.clear();
			}
			
		// }
	}
	
	private void sendToWorker(String address, URLList content) {
		try {
			String jsonForList = om.writerWithDefaultPrettyPrinter().writeValueAsString(content);
			byte[] toSend = jsonForList.getBytes();
			Request request = post("http://" + address + "/push").setBody(toSend).build();
			log.debug("Try to send");
			// evictQueue.add(c.executeRequest(request));
			new Thread(() -> {
						HttpClient hc = new HttpClient();
						try {
							hc.distributeUrl("http://" + address + "/push", toSend);
						} catch (SocketTimeoutException e) {
							log.error("timeout for this");
						}
			 		// }
			}).start();
//			log.debug(conn.getResponseCode());
			log.debug("Sent urls to " + address);
//			conn.disconnect();
		} catch (Exception e) {
			log.error("Sent urls to " + address + ": " + e.getMessage());
		}
		
	}
	
	
}
