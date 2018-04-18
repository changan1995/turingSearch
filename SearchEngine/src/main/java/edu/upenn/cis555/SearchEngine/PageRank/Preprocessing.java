package edu.upenn.cis555.SearchEngine.PageRank;

import java.io.IOException;

import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

public class Preprocessing {
	
	public static class PreprocessMapper extends Mapper<LongWritable, Text, Text, Text> {
		
		@Override
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String line = value.toString();
			String[] pair = line.split(" ", 2);
			String rankAndUrls = "rank:1";
			if (pair.length == 2) rankAndUrls += " "+ pair[1];
			context.write(new Text(pair[0]), new Text(rankAndUrls));
		}
	}
	
	public static class PreprocessReducer extends Reducer<Text, Text, Text, Text> {
		
		@Override
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			for (Text v : values)
				context.write(key, v);
		}
	}

}
