package edu.upenn.cis555.searchengine.servlet;

import java.util.HashSet;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

public class MagicFunction {
	public static double getMagicTf(double tf, double query_tf,double idf, int len){
		
//		if(len>1000){
//			tf=tf*Math.pow(10, Double.valueOf(len)/1000.0);
//		}
		tf=tf*Math.pow(len,0.2);
		query_tf=magic_a+(1-magic_a)*query_tf;
		
		return tf*query_tf*idf;
	}
	public static double getTitleScore(HashSet<String> queries, String title){
		String[] wordList = title.split("([^a-zA-Z0-9]+)");
		SnowballStemmer stemmer = new englishStemmer();
		for(String word : wordList){
			word = word.toLowerCase();
			stemmer.setCurrent(word);
			stemmer.stem();
			String normalizedWord = stemmer.getCurrent().trim();
		}
		
		return 0.0;
	}
	public static double magic_a=0.4;
}
