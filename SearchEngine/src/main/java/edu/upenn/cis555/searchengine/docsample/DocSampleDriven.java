package edu.upenn.cis555.searchengine.docsample;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;



public class DocSampleDriven extends Configured implements Tool{
	@Override
	public int run(String[] arg0) throws Exception {
		// TODO Auto-generated method stub
		Job job=Job.getInstance();
		job.setJobName("Doc Sampler");
		job.setJarByClass(DocSampleDriven.class);
		
		job.setMapperClass(DocSampleMapper.class);
		//For multiple nodes
//		job.setCombinerClass(DocSampleReducer.class);
		job.setReducerClass(DocSampleReducer.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		FileInputFormat.setInputPaths(job, new Path(arg0[0]));
		FileOutputFormat.setOutputPath(job, new Path(arg0[1]));
		
		return job.waitForCompletion(true) ? 0 : 1;
	}

}
