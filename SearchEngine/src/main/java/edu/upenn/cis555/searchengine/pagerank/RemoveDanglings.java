package edu.upenn.cis555.searchengine.pagerank;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;

public class RemoveDanglings extends Configured implements Tool {
	
	private static class DanglingsMapper extends Mapper<LongWritable, Text, Text, Text> {
		
		@Override
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String line = value.toString();
			// bad line
			if (!line.startsWith("http")) {
//				System.err.println("Not start with http: " + line);
				return;
			}
			// field separators \t
			String[] pair = line.split("\\t", 2);
			// TODO comment out
//			String[] pair = line.split(" ", 2);
			if (pair.length < 2) {
//				System.err.println("Should be three part: " + line);
				return;
			}
			// c represent the page is crawled
			context.write(new Text(pair[0]), new Text("c"));
			// map to key "t" to calculate the total number pages
			context.write(new Text("t"), new Text(""));
			
//			if (pair.length == 2) {
			if (pair.length == 2) {
				// links separators between string set
				String[] links = pair[1].split("\002");
				for (String link : links) {
				// TODO comment out
//				for (String link : pair[1].split(" ")) {
//					link.replaceAll(" ", "%20");
					if ("".equals(link)) continue;
					if (link.contains("\t")) {
//						System.out.println("Contains tab: " + link);
					}
					context.write(new Text(link), new Text(pair[0]));
				}
				
			}
		}
	}
	
	private static class DanglingsReducer extends Reducer<Text, Text, Text, Text> {
		
		@Override
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			if (key.toString().equals("t")) {
				// total number of pages
				int max = 0;
				for (Text v : values) {
					max++;
				}
				// write the total to hdfs
				FileSystem fs = FileSystem.get(new Configuration());
//				Path p = new Path(FileOutputFormat.getOutputPath(context).getParent().getName() + File.separator + "total");
				Path p = new Path("/total");
				FSDataOutputStream output = fs.create(p);
				output.writeInt(max);
				output.flush();
				output.close();
//				System.out.println("output path:" + FileOutputFormat.getOutputPath(context).getParent());
//				FSDataInputStream input = fs.open(p);
//				System.out.println("total:" + input.readInt());
				return;
			}
			// check if page is crawled.
			boolean crawled = false;
			LinkedList<String> list = new LinkedList<>();
			for (Text v : values) {
				String value = v.toString();
				if (value.equals("c")) {
					crawled = true;
				} else {
					list.add(value);
				}
			}
			// if crawled, reverse key and value, prepare for the initialization step
			if (crawled) {
				context.write(key, new Text("r:1"));
				for (String link : list) {
					context.write(new Text(link), key);
				}
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
		
		job.setJobName("remove danglings");
		job.setMapperClass(DanglingsMapper.class);
		job.setReducerClass(DanglingsReducer.class);
		job.setJarByClass(RemoveDanglings.class);
		
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		job.getConfiguration().set("mapreduce.output.basename", "remove");
		
		return job.waitForCompletion(true) ? 0 : 1;
	}

}
