package edu.upenn.cis555.searchengine.pagerank;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;


public class FinalStep extends Configured implements Tool {
	
	private static class FinalMapper extends Mapper<LongWritable, Text, Text, DoubleWritable> {
		
		@Override
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			// just extract url and its new rank value (r:(value))
			String line = value.toString();
//			System.out.println(line);
			String[] pair = line.split("\\t", 2);
			String[] valuepair = pair[1].split("\001", 3);
			// valuepair[1] = r:rank
			double finalResult = Double.parseDouble(valuepair[1].substring(2));
//			System.out.println(finalResult);
			context.write(new Text(pair[0]), new DoubleWritable(finalResult));
		}
	}
	
	private static class FinalReducer extends Reducer<Text, DoubleWritable, Text, DoubleWritable> {
		
		@Override
		public void reduce(Text key, Iterable<DoubleWritable> values, Context context) throws IOException, InterruptedException {
			for (DoubleWritable v : values) {
				context.write(key, v);
			}
		}
	}

	@Override
	public int run(String[] args) throws Exception {
		String inputDir = args[0];
		String outputDir = args[1];
		
		Job job = Job.getInstance();
		
		FileInputFormat.setInputPaths(job, new Path(inputDir));
		FileOutputFormat.setOutputPath(job, new Path(outputDir));
		
		job.setJobName("final");
		job.setMapperClass(FinalMapper.class);
		job.setReducerClass(FinalReducer.class);
		job.setJarByClass(FinalStep.class);
		
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(DoubleWritable.class);
		
		job.getConfiguration().set("mapreduce.output.basename", "final");
		
		return job.waitForCompletion(true) ? 0 : 1;
	}

}