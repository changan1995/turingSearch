package cs3.cs2.cs.searchengine.indexer;
/**
 * author: Yi Guo
 * Input: <word,url\001len\001tf>
 * Output: <word,url\001len\001tf\001idf>
 */
import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
public class InvertIndexerReducer extends Reducer<Text,Text,Text,Text> {
	static final int TOTAL_CRAWLED_FILE=1504289;//FOR local test
	@Override
	public void reduce(Text key, Iterable<Text> values,Context context){
		int numDoc=0;
		ArrayList<String> Doc_TF_List=new ArrayList<String>();
		for(Text tf : values){
			numDoc++;
			Doc_TF_List.add(tf.toString());
		}
		String idf=Double.toString(Math.log10((double)TOTAL_CRAWLED_FILE/(double)numDoc));
		//Note: the Doc_TF includes (url--tf)
		try {
			for(String Doc_TF: Doc_TF_List){
				context.write(key,
						new Text(Doc_TF+
								IndexerSeperator.hiveSeperator+
								idf)
				);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}