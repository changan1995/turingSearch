package cs3.cs2.cs.searchengine.servlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

public class Try {
	
	public static void main(String[] args){
		
		/*String html="https://java.com/en/download/";
		Document doc;
		try {
			doc = Jsoup.connect(html).get();
			String title=doc.title(); //get the title
			String text=doc.body().text(); //get the text
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		/*SnowballStemmer stemmer = new englishStemmer();
			stemmer.setCurrent("hello");
			stemmer.stem();
			System.out.println(stemmer.getCurrent());*/
		List<String> ls=new ArrayList<String>();
		for (String s:ls){
			s.split(",");
		}
	}
}
