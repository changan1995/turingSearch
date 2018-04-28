package edu.upenn.cis555;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class RegexTest{
    public static Pattern pattern = Pattern.compile("cn|.39.|pussy|fangjia|fangzi|cheshi|.jp");
    

    public static void main(String args[]){
        String array[]={"www.baidu.com.cn","www.cnn.com/report","www.123.com","mailto:sadasd","www.39sa:a.com","www.39.com"};
        for(String text:array){
            System.out.println(pattern.matcher(text).find()+"\t"+text);
        }
        
        try {
			System.out.println((new URL("http://baidu.com.cn/fasda/123")).getHost());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

}