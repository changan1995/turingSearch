//package cs3.cs2.cs.searchengine.PageRank;
//
//// import java.io.IOException;
//
//import org.apache.hadoop.fs.Path;
//import org.apache.hadoop.io.Text;
//import org.apache.hadoop.mapreduce.Job;
//import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
//import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
//
//public class PageRankMain {
//
//	public static void main(String[] args) throws Exception {
//		if (args.length != 2) {
//			System.out.printf("Usage: Driver <input dir> <output dir>\n");
//			System.exit(-1);
//		}
//
//		String inputDir = args[0];
//		String outputDir = args[1];
//
//		Job job = Job.getInstance();
//
//		FileInputFormat.setInputPaths(job, new Path(inputDir));
//		FileOutputFormat.setOutputPath(job, new Path(outputDir));
//		
//		job.setJobName("test");
//		job.setMapperClass(Preprocessing.PreprocessMapper.class);
//		job.setReducerClass(Preprocessing.PreprocessReducer.class);
//		job.setJarByClass(PageRankMain.class);
//		
//		job.setOutputKeyClass(Text.class);
//		job.setOutputValueClass(Text.class);
//		
//		job.waitForCompletion(true);
//	}
//}
