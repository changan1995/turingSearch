package cs3.cs2.cs.searchengine.pagerank;

import org.apache.hadoop.util.ToolRunner;

public class DataExtractMain {
	
	public static void main(String[] args) throws Exception {
		// extract all links in all docs in s3
		if (args.length != 2) {
			System.out.println("Usage: <input dir> <output dir>");
			System.exit(-1);
		}
		String inputDir = args[0];
		String outputDir = args[1];
		
		if (ToolRunner.run(new Extract(), new String[] { inputDir, outputDir } ) == 1) {
			System.err.println("Extract data error");
			System.exit(-1);
		}
	}
}
