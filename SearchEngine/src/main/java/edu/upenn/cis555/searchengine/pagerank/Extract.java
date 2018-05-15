package cs3.cs2.cs.searchengine.pagerank;

import java.io.IOException;
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

public class Extract extends Configured implements Tool {
	
	private static class ExtractMapper extends Mapper<LongWritable, Text, Text, Text> {
		
		// s3 bucket
		private static String bucketName = "turingSearch-crawler-changanw-2";
		private String envDirectory = "test3/";
		private AmazonS3 s3;
		
		@Override
		protected void setup(Mapper<LongWritable, Text, Text, Text>.Context context)
				throws IOException, InterruptedException {
			s3 = AmazonS3ClientBuilder.defaultClient();
		}
		
		@Override
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String line = value.toString();
			// url \001 s3key
			String[] pair = line.split("\001", 2);
			String content = null;
			try {
				//get doc content
				content = s3.getObjectAsString(bucketName, pair[1]);
			} catch (Exception e) {
				return;
			}
			// parse all outlinks using jsoup
			Document doc = Jsoup.parse(content, pair[0]);
			Elements links = doc.select("a[href]");
			HashSet<String> list = new HashSet<>();
			for (Element l : links) {
				list.add(l.attr("abs:href"));
			}
			// the seperator between all links is \002
			String outlinks = StringUtils.join(list, "\002");
			context.write(new Text(pair[0]), new Text(outlinks));
		}
		
//		private String getContent(String path) throws IOException {
//			// try {
//			S3Object o = s3.getObject("bucketName", path);
//			S3ObjectInputStream s3is = o.getObjectContent();
//			BufferedReader reader = new BufferedReader(new InputStreamReader(s3is, "UTF-8"));
//			StringBuilder sb = new StringBuilder();
//			String line = null;
//			while ((line = reader.readLine()) != null) {
//				sb.append(line + "\n");
////				System.out.println(line + "\n");
//			}
//			return sb.toString();
//		}
	}
	
	private static class ExtractReducer extends Reducer<Text, Text, Text, Text> {
		
		@Override
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			for (Text v : values) {
				context.write(key, v);
			}
		}
	}

	@Override
	public int run(String[] args) throws Exception {
		String inputDir = args[0];
		String outputDir = args[1];
		
//		Configuration conf = getConf();
//		conf.set("mapred.textoutputformat.separator", "\001");
//		Job job = Job.getInstance(conf);
		Job job = Job.getInstance();
		
		FileInputFormat.setInputPaths(job, new Path(inputDir));
		FileOutputFormat.setOutputPath(job, new Path(outputDir));
		
		job.setJobName("extract");
		job.setMapperClass(ExtractMapper.class);
		job.setReducerClass(ExtractReducer.class);
		job.setJarByClass(Extract.class);
		
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		job.getConfiguration().set("mapreduce.output.basename", "extract");
		
		return job.waitForCompletion(true) ? 0 : 1;
	}

}