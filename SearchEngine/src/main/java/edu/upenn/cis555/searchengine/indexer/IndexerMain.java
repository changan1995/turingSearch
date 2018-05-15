package edu.upenn.cis555.searchengine.indexer; 
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ToolRunner;


public class IndexerMain {
	
	public static void main(String args[])throws Exception{	
		int mapreduce=ToolRunner.run(new Configuration(), new InvertIndexerDriven(),args);
		System.out.println("Indexer finished");
		return;
	}

}
