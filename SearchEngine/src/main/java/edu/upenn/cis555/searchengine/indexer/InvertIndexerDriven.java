package edu.upenn.cis555.searchengine.indexer;
/**
 * author: Yi Guo
 */
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
public class InvertIndexerDriven extends Configured implements Tool{
	@Override
	public int run(String[] arg0) throws Exception {
		// TODO Auto-generated method stub
		Job job=Job.getInstance();
		job.setJobName("Build Invert Indexer");
		job.setJarByClass(InvertIndexerDriven.class);
		
		job.setMapperClass(InvertIndexerMapper.class);
		//For multiple nodes
//		job.setCombinerClass(IndexReduce.class);
		job.setReducerClass(InvertIndexerReducer.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		FileInputFormat.setInputPaths(job, new Path(arg0[0]));
		FileOutputFormat.setOutputPath(job, new Path(arg0[1]));
		
		return job.waitForCompletion(true) ? 0 : 1;
	}

}
