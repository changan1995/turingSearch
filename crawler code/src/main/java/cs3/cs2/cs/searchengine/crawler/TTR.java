package cs3.cs2.cs.searchengine.crawler;

import java.util.LinkedList;
import java.util.Queue;

public class TTR implements Comparable<TTR>{
	// Time to release
	private String host;
	private long releaseTime;
	private int count;
	// private Queue<String> urlQueue;
	
	public TTR(String host, long releaseTime) {
		this.host = host;
		this.releaseTime = releaseTime;
		// this.urlQueue=new LinkedList<>();
	}
		
	public TTR(String host, long releaseTime ,int count) {
		this.host = host;
		this.releaseTime = releaseTime;
		this.count=count;
		// this.urlQueue=new LinkedList<>();
	}
	
	@Override
	public int compareTo(TTR o) {
		return Long.compare(this.releaseTime, o.releaseTime);
	}

	public String getHost(){
		return this.host;
	}

	public void setHost(String host){
		this.host=host;
		return;
	}

	public long getReleasTime(){
		return this.releaseTime;
	}

	public void setReleaseTime(long releaseTime){
		this.releaseTime=releaseTime;
		return;
	}

	/**
	 * @return the count
	 */
	public int getCount() {
		return count;
	}

	/**
	 * @param count the count to set
	 */
	public void decrement() {
		this.count--;
	}

}