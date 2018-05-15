package cs3.cs2.cs.searchengine.docsample;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ToolRunner;



public class DocSampleMain {
	public static void main(String args[])throws Exception{	
		int mapreduce=ToolRunner.run(new Configuration(), new DocSampleDriven(),args);
		System.out.println("Doc Sample finished");
		return;
	}
}
