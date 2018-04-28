package edu.upenn.cis555.searchengine.crawler;

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

	// public Queue getQueue(){
	// 	return this.urlQueue;
	// }

	// public void setQueue(Queue urlQueue){
	// 	this.urlQueue=urlQueue;
	// 	return;
	// }

	// public void add(String urlString){
	// 	this.urlQueue.add(urlString);
	// }

	// public String poll(){
	// 	return this.urlQueue.poll();
	// }

	// public int size(){
	// 	return this.urlQueue.size();
	// }

	// public void clear(){
	// 	this.host=null;
	// 	this.releaseTime=-1;
	// 	this.urlQueue=null;
	// 	return ;
	// }
}