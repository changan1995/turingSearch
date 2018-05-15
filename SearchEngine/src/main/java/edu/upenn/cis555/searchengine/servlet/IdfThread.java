package cs3.cs2.cs.searchengine.servlet;

import cs3.cs2.cs.searchengine.jettyserver.MinimalJettyServer;

public class IdfThread extends Thread {

	public String word;
	public double idf;

	public IdfThread(String word) {
		this.word = word;
	}

	public void run() {
		idf = QueryDBMapper.FindIdf(MinimalJettyServer.mapper, word);
	}
}
