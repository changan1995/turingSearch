package cs3.cs2.cs.searchengine.indexer;
/**
 * author: Yi Guo
 * Input: <url,s3key>
 * Output: <word,url\001len\001tf>
 */
import java.util.HashMap;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Mapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document; 
import org.jsoup.select.Elements;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;


public class InvertIndexerMapper extends Mapper<LongWritable,Text,Text,Text>{
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
		HashMap<String,Integer>wordCount=new HashMap<String,Integer>();
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
			String text=doc.text();
			//For indexer, we only extract words and number here 
			String[] wordList=text.split("([^a-zA-Z0-9]+)");
			int len=wordList.length;
			int maxCount=0;
			SnowballStemmer stemmer = new englishStemmer();
			for(String word : wordList){
				word=word.toLowerCase();
				// Filter out the stopwords. It is optional. If we have enough time, we may create a database
				// with these stop words indices
				if(StopWordList.stopwords.contains(word)){
					continue;
				}
				stemmer.setCurrent(word);
				stemmer.stem();
				String index=stemmer.getCurrent();
				//Don't know why. But we need this check
				index=index.trim();
				if(index.length()==0 || index.length()>200){
					len--;
					continue;
				}
				if(!wordCount.containsKey(index)){
					wordCount.put(index, 1);
				}else{
					wordCount.put(index, wordCount.get(index)+1);
				}	
				maxCount=wordCount.get(index)>maxCount?wordCount.get(index):maxCount;
			}
			for(String word: wordCount.keySet()){
				//choose one
//				double tf=(double)wordCount.get(word)/(double)maxCount;
				double tf=(double)wordCount.get(word)/(double)len;
				context.write(new Text(word),
						new Text(
								url+
								IndexerSeperator.hiveSeperator+
								Integer.toString(len)+
								IndexerSeperator.hiveSeperator+
								Double.toString(tf)
								)
				);
			}
		}catch(IllegalArgumentException e){
			System.out.println("Invalid formate of html file");
			e.printStackTrace();
		}catch(Exception e){
			e.printStackTrace();
			return;
		}
		
		
		
	}
	
}


