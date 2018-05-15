package cs3.cs2.cs.searchengine.docsample;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import cs3.cs2.cs.searchengine.indexer.IndexerSeperator;
import cs3.cs2.cs.searchengine.indexer.S3Client;

public class DocSampleMapper extends Mapper<LongWritable,Text,Text,Text>{
	private S3Client s3client=new S3Client();
	@Override
	public void map(LongWritable key, Text value,Context context){
		String[] s=value.toString().split(IndexerSeperator.hiveSeperator);
		if(s.length<2){
			System.out.println("Invalid Input Format");
			return;
		}		
		String url=s[0];
		String s3Key=s[1];
		String content=s3client.getContent(s3Key);
		
		if(content==null){
			System.out.println("Empty content for "+s3Key);
			return;
		}
		Document doc;
		try{
			// Filter out the documents that are not in English
			doc = Jsoup.parse(content);
			Elements html=doc.select("html[lang]");
			if(html!=null){
				String language=html.attr("lang");
				if(language!=""){
					if(!language.equals("en") && !language.equals("en-US") && !language.equals("en-GB")){
						return;
					}
				}
			}
			String title=doc.title().trim();
			if(title.length()==0){
				title="Empty_title";
			}
			String text=doc.text().trim();
			if(text.length()==0){
				text="Empty_Body";
			}
			else if(text.length()>350){
				text=text.substring(0,350);
			}
			
			context.write(
	    			new Text(url), 
	    			new Text(
	    					title
	    					+ IndexerSeperator.hiveSeperator
	    					+ text
	    					)
	    			);
		}catch(Exception e){
			e.printStackTrace();
		}

	}

}
