package edu.upenn.cis555.searchengine.crawler;

public class TTR implements Comparable<TTR>{
	// Time to release
	String host;
	long releaseTime;
	
	public TTR(String host, long releaseTime) {
		this.host = host;
		this.releaseTime = releaseTime;
	}
	
	@Override
	public int compareTo(TTR o) {
		return Long.compare(this.releaseTime, o.releaseTime);
	}
}