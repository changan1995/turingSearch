package cs3.cs2.cs.searchengine.pagerank;

import java.io.File;
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

public class PageRankIteration extends Configured implements Tool {
	
	private final static double damping = 0.85;
	
	@Override
	public int run(String[] args) throws Exception {
		String inputDir = args[0];
		String outputDir = args[1];
		String idx = args[2];

		Job job = Job.getInstance();

		FileInputFormat.setInputPaths(job, new Path(inputDir));
		FileOutputFormat.setOutputPath(job, new Path(outputDir));
		
		job.setJobName("iteration" + idx);
		job.setMapperClass(IterationMapper.class);
		job.setReducerClass(IterationReducer.class);
		job.setJarByClass(PageRankIteration.class);
		
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		job.getConfiguration().set("mapreduce.output.basename", "iteration" + idx);
		
		return job.waitForCompletion(true) ? 0 : 1;
	}
	
	private static class IterationMapper extends Mapper<LongWritable, Text, Text, Text> {
		
		@Override
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			// url\to:value r:value outlinks
			String line = value.toString();
//			System.out.println(line);
			String[] pair = line.split("\\t", 2);
			String rankAndUrls = pair[1];
			String[] parts = rankAndUrls.split("\001", 3);
			
			context.write(new Text(pair[0]), new Text(parts[1]));
			if (parts.length > 2) {
				double rank = Double.parseDouble(parts[1].substring("r:".length()));
				context.write(new Text(pair[0]), new Text("out:" + parts[2]));
				String[] outlinks = parts[2].split("\001");
				// distribute rank to outlink pages
				double average = rank / outlinks.length;
				for (String outlink : outlinks) {
					context.write(new Text(outlink), new Text(String.valueOf(average)));
				}
			}
		}
	}
	
	private static class IterationReducer extends Reducer<Text, Text, Text, Text> {
		
		@Override
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			String outlinks = "";
			String rankString = "";
			double rank = 0;
			for (Text value : values) {
				String v = value.toString();
				if (v.startsWith("out:")) {
					// keep the outlinks structure
					outlinks = v.substring(4);
				} else if (v.startsWith("r:")) {
					rankString = v;
				} else {
					// sum all rank from other pages
					rank += Double.parseDouble(v);
				}
			}
			StringBuilder sb = new StringBuilder();
			// origin new value now is old value
			sb.append(rankString.replace('r', 'o'));
//			if (rank == 0) {
//				sb.append("\001" + rankString);
//			} else {
			// new rank value
				sb.append("\001r:" + String.valueOf(rank * damping + 1 - damping));
//			}
			if (!"".equals(outlinks)) {
				sb.append("\001" + outlinks);
			}
			context.write(key, new Text(sb.toString()));
		}
	}
}
