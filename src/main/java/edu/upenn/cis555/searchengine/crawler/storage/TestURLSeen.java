package edu.upenn.cis555.searchengine.crawler.storage;

import java.util.HashSet;

public class TestURLSeen {

	public static void main(String[] args) {
		DBWrapper db = DBWrapper.getInstance();
		db.setUp();
		System.out.println(db.checkURLSeen("a"));
//		db.saveURLSeen("abc");
		System.out.println(db.checkURLSeen("abc"));
		
		
	}
}
