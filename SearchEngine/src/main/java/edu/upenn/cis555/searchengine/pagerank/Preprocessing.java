package edu.upenn.cis555.searchengine.pagerank;

import java.io.IOException;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;

public class Preprocessing extends Configured implements Tool {
	
	private static class PreprocessMapper extends Mapper<LongWritable, Text, Text, Text> {
		
		@Override
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String line = value.toString();
			System.out.println(line);
			String[] pair = line.split(" ", 2);
			String rankAndUrls = "rank:1";
			if (pair.length == 2) rankAndUrls += " "+ pair[1];
			context.write(new Text(pair[0]), new Text(rankAndUrls));
		}
	}
	
	private static class PreprocessReducer extends Reducer<Text, Text, Text, Text> {
		
		@Override
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			for (Text v : values)
				context.write(key, v);
		}
	}

	@Override
	public int run(String[] args) throws Exception {
		String inputDir = args[0];
		String outputDir = args[1];

		Job job = Job.getInstance();
		
		FileInputFormat.setInputPaths(job, new Path(inputDir));
		FileOutputFormat.setOutputPath(job, new Path(outputDir));
		
		job.setJobName("preprocess");
		job.setMapperClass(PreprocessMapper.class);
		job.setReducerClass(PreprocessReducer.class);
		job.setJarByClass(Preprocessing.class);
		
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		job.getConfiguration().set("mapreduce.output.basename", "preprocess");
		
		return job.waitForCompletion(true) ? 0 : 1;
	}

}
