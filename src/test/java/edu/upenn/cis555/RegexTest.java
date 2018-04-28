package edu.upenn.cis555;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class RegexTest{
    public static Pattern pattern = Pattern.compile("cn|.39.|pussy|fangjia|glassdoor|fangzi|cheshi|.jp");
    

    public static void main(String args[]){
        String array[]={"http://www.baidu.com.cn","http://www.cnn.com/report","http://www.123.com","https://www.glassdoor.ca/Reviews/Expedia-good-benefits-Reviews-EI_IE9876.0,7_KH8,21.htm","http://mailtosadasd","http://www.39saa.com","http://www.39.com"};
        for(String text:array){
            URL url=null;
			try {
				url = new URL(text);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            String host =url.getHost();
            System.out.println(pattern.matcher(text).find()+"\t"+host);
        }
        
        try {
			System.out.println((new URL("http://baidu.com.cn/fasda/123")).getHost());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

}