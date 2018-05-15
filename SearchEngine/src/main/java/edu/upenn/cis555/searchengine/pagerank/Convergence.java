package cs3.cs2.cs.searchengine.pagerank;

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


public class Convergence extends Configured implements Tool {
	
	private static class ConMapper extends Mapper<LongWritable, Text, Text, DoubleWritable> {
		
		@Override
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String line = value.toString();
//			System.out.println(line);
			// url \t o:old_vale \001 r:new_value \001 outlinks
			String[] pair = line.split("\\t", 2);
			String[] valuepair = pair[1].split("\001", 3);
			double error = Math.abs(Double.parseDouble(valuepair[0].substring(2)) - Double.parseDouble(valuepair[1].substring(2)));
//			System.out.println(error);
			// map to the same key "error"
			context.write(new Text("error"), new DoubleWritable(error));
		}
	}
	
	private static class ConReducer extends Reducer<Text, DoubleWritable, DoubleWritable, Text> {
		
		@Override
		public void reduce(Text key, Iterable<DoubleWritable> values, Context context) throws IOException, InterruptedException {
			double total = 0;
			// sum up all error
			for (DoubleWritable d : values) {
				total += d.get();
			}
			context.write(new DoubleWritable(total), new Text(""));
		}
	}

	@Override
	public int run(String[] args) throws Exception {
		String inputDir = args[0];
		String outputDir = args[1];
		String idx = args[2];
		
		Configuration conf = new Configuration();
		conf.set("idx", idx);

		Job job = Job.getInstance();
		
		FileInputFormat.setInputPaths(job, new Path(inputDir));
		FileOutputFormat.setOutputPath(job, new Path("/converge" + idx));
		
		job.setJobName("converge" + idx);
		job.setMapperClass(ConMapper.class);
		job.setReducerClass(ConReducer.class);
		job.setJarByClass(Convergence.class);
		
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(DoubleWritable.class);
		job.setOutputKeyClass(DoubleWritable.class);
		job.setOutputValueClass(Text.class);
		job.setNumReduceTasks(1);
		
		job.getConfiguration().set("mapreduce.output.basename", "converge" + idx);
		
		return job.waitForCompletion(true) ? 0 : 1;
	}

}