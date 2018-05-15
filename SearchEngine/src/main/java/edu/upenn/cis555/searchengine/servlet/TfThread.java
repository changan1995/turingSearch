package edu.upenn.cis555.searchengine.servlet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.upenn.cis555.searchengine.jettyserver.MinimalJettyServer;

public class TfThread extends Thread{
	
	public String word;
	public List<QueryDBMapper.Indexer_tf_new> scores=new ArrayList<QueryDBMapper.Indexer_tf_new>();

	public TfThread(String word) {
		this.word = word;
	}

	public void run() {
		scores = QueryDBMapper.FindScore(MinimalJettyServer.mapper, word);
	}

}
