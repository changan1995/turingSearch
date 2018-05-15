package edu.upenn.cis555.searchengine.servlet;

import edu.upenn.cis555.searchengine.jettyserver.MinimalJettyServer;

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
