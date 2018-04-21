package edu.upenn.cis555.searchengine.crawler.info;

import java.util.ArrayList;
import java.util.HashMap;

public class RobotsTxtInfo {
	
	private HashMap<String,ArrayList<String>> disallowedLinks;
	private HashMap<String,ArrayList<String>> allowedLinks;
	public long delay=0;// i will be upset if you download this much XD
	private HashMap<String,Integer> crawlDelays;
    private ArrayList<String> sitemapLinks;
    private String absoluteHost= null;
	private ArrayList<String> userAgents;
	private long nextCrawlDate = 0;
	
	// public RobotsTxtInfo(){
	// 	disallowedLinks = new HashMap<String,ArrayList<String>>();
	// 	allowedLinks = new HashMap<String,ArrayList<String>>();
	// 	crawlDelays = new HashMap<String,Integer>();
	// 	sitemapLinks = new ArrayList<String>();
	// 	userAgents = new ArrayList<String>();
    // }
    public RobotsTxtInfo(String txt,String host){
		nextCrawlDate = System.currentTimeMillis();
        absoluteHost=host;
		disallowedLinks = new HashMap<String,ArrayList<String>>();
		allowedLinks = new HashMap<String,ArrayList<String>>();
		crawlDelays = new HashMap<String,Integer>();
		sitemapLinks = new ArrayList<String>();
        userAgents = new ArrayList<String>();
        String agent =null;
        String lines[] = txt.split("\\r?\\n");
        for(String line:lines){
            if (line.indexOf("User-agent") != -1) {
				// User agent ,store for this section
				agent = line.substring(line.indexOf(':') + 1).trim();
				this.addUserAgent(agent);
			} else if (line.indexOf("Disallow") != -1) {
				// disallow
                String path = line.substring(line.indexOf(':') + 1).trim();
                if(path.endsWith("*")){
                    path=path.substring(0,path.lastIndexOf('*'));
                }else if(path.endsWith("/")){
                    this.addDisallowedLink(agent, absoluteHost+path.substring(0,path.lastIndexOf('/')));//private and private/ are both forbiddened              
                }
				this.addDisallowedLink(agent, absoluteHost+path);
			} else if (line.indexOf("Allow") != -1) {
				// allow
                String path = line.substring(line.indexOf(':') + 1).trim();
                if(path.endsWith("*")){
                    path=path.substring(0,path.lastIndexOf('*'));
                }
				this.addAllowedLink(agent, absoluteHost+path);
			} else if (line.indexOf("Crawl-delay") != -1) {
				// delay
                try{
                    int delay = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
                    this.addCrawlDelay(agent, delay);
                    
                }catch(NumberFormatException e ){
                    continue;
                }
			}
        }
	}
	
	public void addDisallowedLink(String key, String value){
		if(!disallowedLinks.containsKey(key)){
			ArrayList<String> temp = new ArrayList<String>();
			temp.add(value);
			disallowedLinks.put(key, temp);
		}
		else{
			ArrayList<String> temp = disallowedLinks.get(key);
			if(temp == null)
				temp = new ArrayList<String>();
			temp.add(value);
			disallowedLinks.put(key, temp);
		}
	}
	
	public void addAllowedLink(String key, String value){
		if(!allowedLinks.containsKey(key)){
			ArrayList<String> temp = new ArrayList<String>();
			temp.add(value);
			allowedLinks.put(key, temp);
		}
		else{
			ArrayList<String> temp = allowedLinks.get(key);
			if(temp == null)
				temp = new ArrayList<String>();
			temp.add(value);
			allowedLinks.put(key, temp);
		}
	}
	
	public void addCrawlDelay(String key, Integer value){
		crawlDelays.put(key, value);
	}
	
	public void addSitemapLink(String val){
		sitemapLinks.add(val);
	}
	
	public void addUserAgent(String key){
		userAgents.add(key);
	}
	
	public boolean containsUserAgent(String key){
		return userAgents.contains(key);
	}
	
	public ArrayList<String> getDisallowedLinks(String key){
		return disallowedLinks.get(key);
	}
	
	public ArrayList<String> getAllowedLinks(String key){
		return allowedLinks.get(key);
	}
	
	public int getCrawlDelay(String key){
        if(crawlDelays.get(key)==null){
            return 1;//default delay as 2 second
		}
		return crawlDelays.get(key);
	}
	
	public void print(){
		for(String userAgent:userAgents){
			System.out.println("User-Agent: "+userAgent);
			ArrayList<String> dlinks = disallowedLinks.get(userAgent);
			if(dlinks != null)
				for(String dl:dlinks)
					System.out.println("Disallow: "+dl);
			ArrayList<String> alinks = allowedLinks.get(userAgent);
			if(alinks != null)
					for(String al:alinks)
						System.out.println("Allow: "+al);
			if(crawlDelays.containsKey(userAgent))
				System.out.println("Crawl-Delay: "+crawlDelays.get(userAgent));
			System.out.println();
		}
		if(sitemapLinks.size() > 0){
			System.out.println("# SiteMap Links");
			for(String sitemap:sitemapLinks)
				System.out.println(sitemap);
		}
	}
	
	public boolean crawlContainAgent(String key){
		return crawlDelays.containsKey(key);
	}

	/**
	 * @return the nextCrawlDate
	 */
	public  long  getNextCrawlDate(String key) {
        synchronized(this){
		long current = System.currentTimeMillis();
        if(nextCrawlDate<current){//if nextcrawled has passed , return current;
			nextCrawlDate=current + getCrawlDelay(key)*1000;
			return current;
		}else{//if nextcrawled time not arrived, return next crawed time ,and add crawl time.
            long temp = nextCrawlDate;
            // System.out.println("crawl delay is "+getCrawlDelay(key));
            nextCrawlDate+= getCrawlDelay(key)*1000;
            // System.out.println((nextCrawlDate-System.currentTimeMillis())/1000);
			return temp;
        }
        }
	}

	/**
	 * @param nextCrawlDate the nextCrawlDate to set
	 */
	public void setNextCrawlDate(long nextCrawlDate) {
		this.nextCrawlDate = nextCrawlDate;
	}
}
