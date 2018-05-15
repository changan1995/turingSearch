package edu.upenn.cis555.searchengine.docsample;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Reducer.Context;

public class DocSampleReducer extends Reducer<Text,Text,Text,Text> {
	@Override
	public void reduce(Text key, Iterable<Text> values,Context context){
		try{
			for (Text value : values)
			{
				context.write( key, value );
			}
		}catch (Exception e){
			e.printStackTrace();
		}

	}
}
