package edu.upenn.cis555.searchengine.servlet;

import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

public class Compare implements Comparator<Link>{

	/*@Override
	public int compare(Entry<String, Double> arg0, Entry<String, Double> arg1) {
		if (arg0.getValue()>arg1.getValue()){
			return -1;
		}else if (arg0.getValue()<arg1.getValue()){
			return 1;
		}else{
			return 0;
		}
	}*/
	
	@Override
	public int compare(Link arg0, Link arg1) {
		if (arg0.totalScore>arg1.totalScore){
			return -1;
		}else if(arg0.totalScore<arg1.totalScore){
			return 1;
		}else{
			return 0;
		}
	}
}
