package cs3.cs2.cs.searchengine.global;

import java.util.HashMap;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

public class Stem {
	static public void StemAndComputeTf(String[] wordList,HashMap<String, Double> tfs){
		SnowballStemmer stemmer = new englishStemmer();
		double word_max=0;
		for(String word: wordList){
			word=word.toLowerCase();		
			stemmer.setCurrent(word);
			stemmer.stem();
			String normalizedWord = stemmer.getCurrent().trim();		
			if(normalizedWord.length()==0)continue;
			if(tfs.containsKey(normalizedWord))
				tfs.put(normalizedWord,tfs.get(normalizedWord)+1);
			else
				tfs.put(normalizedWord,(double) 1);
			word_max=tfs.get(normalizedWord)>word_max?tfs.get(normalizedWord):word_max;		
		}
		for(String word :tfs.keySet()){
			tfs.put(word, tfs.get(word)/word_max);
		}
		return;
	}
}
