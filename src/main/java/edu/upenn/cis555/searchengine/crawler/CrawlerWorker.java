package edu.upenn.cis555.searchengine.crawler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.Funnels;
import com.jayway.jsonpath.spi.cache.LRUCache;
import com.sleepycat.je.Transaction;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
// import org.mockito.internal.matchers.Null;

import edu.upenn.cis555.searchengine.crawler.info.RobotsTxtInfo;
import edu.upenn.cis555.searchengine.crawler.info.URLInfo;
import edu.upenn.cis555.searchengine.crawler.storage.DB;
// import edu.upenn.cis555.searchengine.crawler.hw1.BlockingQueue;
// import edu.upenn.cis555.searchengine.crawler.hw1.HttpServer;
import edu.upenn.cis555.searchengine.crawler.storage.DBWrapper;
import edu.upenn.cis555.searchengine.crawler.storage.Doc;
import edu.upenn.cis555.searchengine.crawler.storage.DocDB; 
import edu.upenn.cis555.searchengine.crawler.storage.Entry;
import edu.upenn.cis555.searchengine.crawler.structure.URLEntry;

public class CrawlerWorker implements Runnable {
	
	static Logger log = Logger.getLogger("debugLogger");
    // public static BlockingQueue<URL> urlToDo;//add seen url
    // public static BlockingQueue<URL> urlDone;
    private boolean flag = false;
    // private DBWrapper dbWrapper = null;
    // private DocDB docDB = null;
    private RobotsTxtInfo robot;
    // private long lastCrawedTime = new Long(0);
    // private Transaction txn = null;
    private long toCrawlDate;
    private int id;
    private DB db;
    public static final int bodyLength = 350;
    // private LRUCache 
    // private BloomFilter<CharSequence> bl;
    private int crawledNum;
    private Entry entry =null;
    // public static String dbDirectory;
    private URLFrontier frontier;
	private URLDistributor distributor;
	public static Pattern pattern = Pattern.compile("^http[s]?://.*(facebook|google|twitter|amazon|linkedin|pornhub|weibo|instagram|blogspot|tumblr)\\.com.*");

    public CrawlerWorker(int id, int crawledNum, URLFrontier frontier){
        this.id =id;
        this.db = new DB();
        this.crawledNum=crawledNum;
        this.frontier = frontier;
        System.out.println(id + "worker setup");
        this.distributor = new URLDistributor(Crawler.index, Crawler.workerList, frontier);
        
        // bl= BloomFilter.create(Funnels.stringFunnel(), 10000);
        // private PriorityQueue<URLEntry> urlToDo = Crawler.urlToDo;
    }

    public void download(String url) throws Exception {
        // }
//        String url =urlEntry.getUrl().toString();
        HttpClient hc = new HttpClient();
//        log.debug("id"+id+"\tDownloading:\t" + url);        
        
        if (!hc.send("GET", url)) {
            return;
        }
        log.debug("id"+id+"\tDownloaded:\t" + url);        
        //put the file in to db. prepare for multiple value
        // txn = dbWrapper.getTransaction();
        try {
            // String lastModified = Utilities.convertTime(hc.getLastModified());
            // Doc doc = new Doc(url, hc.getContent(), hc.getContentType(), new Long(hc.getContentLength()), lastModified);
            // if (updateflag) {
            //     docDB.updateDoc(doc, txn);
            // } else {
            //     docDB.insertDoc(doc, txn);
            // }
            entry= new Entry(url);
            // TODO uncomment the DynamoDB
            String contentString =hc.getContent();
//        log.debug("id"+id+"\tUpDynamoing:\t" + url);        
        
           db.setContentLink(entry, contentString);
            anaylize(url,contentString);
           db.add(entry);
            Crawler.num.incrementAndGet();
//        log.debug("id"+id+"\tUpDynamoDBed:\t" + url);        
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        // txn.commit();
        // lastCrawedTime = System.currentTimeMillis();
    }

    //decided whehter it is the form we required
    public static boolean typeValid(String contentType) throws Exception {
        try {
            contentType = contentType.toLowerCase().trim();
            return (contentType.contains("text/html"));

        } catch (NullPointerException e) {
            // System.out.println("")
            return false;
        }
    }

    //put links generated from JSOUP document to urlToDo, and filter some obviously we dont want
    public void anaylize(String urlString,String contentString) throws Exception{
//        URL url2=null;

        Set<String> outLinksBuff =new HashSet<>();
//		try {
//			url2 = new URL(urlString);
//		} catch (MalformedURLException e1) {
//			e1.printStackTrace();
//		}
//        String absHost = url2.getProtocol()+"://"+url2.getHost()+url2.getPath();
        Document document = Jsoup.parse(contentString, urlString);
        Elements links = document.select("a");
        // List<String> out
        for (Element link : links) {
            String text = link.absUrl("abs:href");
            if (text.startsWith("mailto")) {
            		continue;
            }
            if (pattern.matcher(text).find()) {
                continue;
            }
            if (text.contains("wikipedia") && !text.contains("en.wikipedia")) {
            		continue;
            }
            // System.out.println(doc.getUrl()+"\t\t"+text);
			try {
				URL url = new URL(text);
			} catch (MalformedURLException e) {
                // System.out.println("nullpointer"+text);
                // e.printStackTrace();
                continue;
            }
            // if(url!=null)
            outLinksBuff.add(text);
//            if(Crawler.urlToDo.size()>100){
//                Crawler.urlToDo.poll();
//            }
//            Crawler.urlToDo.add(new URLEntry(url, toCrawlDate));
            
            // distribute url
            // distributor.distributeURL(text);
            
        }
        // try{
		// 	String body = document.body().text();
		// 	if (body.length() > bodyLength) {
		// 		entry.setBodySample(body.substring(0, bodyLength));
		// 	} else {
		// 		entry.setBodySample(body);
		// 	}
		// 	entry.setTitle(document.title());
		// } catch (NullPointerException e) {
		// 	// body / title missed
		// }
        entry.setOutLinks(outLinksBuff);
                
        if(!distributor.urlFrontierFull()){
            distributor.distributeURL(outLinksBuff);
        
        }
        return;
        
    }

    //if we can access return true;
    public boolean checkRobot(URL url) throws Exception{
//        log.debug("id"+id+"\tcheckroboting\t" + url);        
        boolean returnvalue= Crawler.rule.canCrawl(url.getHost(), url.getPath());
//        log.debug("id"+id+"\tcheckroboted\t" + url);        
        return returnvalue;
        //put all valid links in urlToDo;
//        ArrayList<String> allowed;
//        ArrayList<String> disallowed;
//        URLInfo urlInfo = new URLInfo(url.toString());
//        String absoluteRoot = urlInfo.getAbsoluteRoot();
//        String hostName = urlInfo.getHostName();
//        String agent = null;
//        robot = Crawler.robotLst.get(hostName);
//        if (robot == null) {
//            //new host found, download the robot
//            // lastCrawedTime = new Long(0);
//            HttpClient hc = new HttpClient();
//            if (!hc.send("GET", new URLEntry("http://" + url.getHost() + "/robots.txt", System.currentTimeMillis()))) { //turn to absolute address
//                robot = new RobotsTxtInfo("User-agent: *", absoluteRoot);
//                Crawler.robotLst.put(hostName, robot);
//                return true;
//            }
//            robot = new RobotsTxtInfo(hc.getContent(), absoluteRoot);
//            Crawler.robotLst.put(hostName, robot);
//        }
//        //find allowed/disallowed
//        if (robot.containsUserAgent("cis455crawler")) {
//            agent = "cis455crawler";
//        } else if (robot.containsUserAgent("*")) {
//            agent = "*";
//        } else {
//            return true;
//        }
//
//        allowed = robot.getAllowedLinks(agent);
//        disallowed = robot.getDisallowedLinks(agent);
//        if (disallowed == null) {
//            return true;
//        }
//        if (disallowed.size() == 0) {
//            return true;
//        }
//        for (String s : disallowed) {
//            // System.out.println(s);
//            if (urlInfo.getUrlNoPort().startsWith(s)) {
//                return false;
//            }
//        }
//        // System.out.println(id+"gets ");
//        toCrawlDate = robot.getNextCrawlDate(agent);
//        // System.out.println(id+"release ");
//
//        return true;
    }

    // public CrawlerWorker(int id){
    //     this.id =id;
    // }

    @Override
    public void run() {
        // threadName = Thread.currentThread().getName();
    		Thread.currentThread().setName("" + id);
        while (true) {//main loop
//            URLEntry urlEntry = null;
            //take out one url
        		try {
            String urlString;
			try {
				urlString = frontier.getURL();
			} catch (InterruptedException e2) {
				continue;
			}
//			log.debug("id"+id+"\t get:\t" + urlString);
//			urlEntry = new URLEntry(url, 0);
//            if (Crawler.urlToDo.isEmpty()) {
////                flag = true;
//                // try {
//				// 	Thread.sleep(500);
//				// } catch (InterruptedException e) {
//				// 	e.printStackTrace();
//				// }
//                continue;
//            } else {
//                urlEntry = Crawler.urlToDo.poll();
//                // System.out.println()
//            }
//            if(urlEntry==null){
//                continue;
//            }
//            URL urlCurrent = urlEntry.getUrl();
            URL urlCurrent;
			try {
				urlCurrent = new URL(urlString);
			} catch (MalformedURLException e1) {
				continue;
			}
            //Robot check
            if (!checkRobot(urlCurrent)) {
                log.debug("Not allowed: "+urlCurrent.toString());
                continue;
            }

            //send head to check if we need to do download();
            try {
                // Doc doc = docDB.get(urlString);
                // filter.put(object);
//                if (!Crawler.bl.put(urlString)) {
                    //Doc has been seen
                    // updateflag = true;
                    // Long crawledDate = Utilities.convertDate(doc.getcrawledDate());
                    // //send head to check 
                    // HttpClient hc = new HttpClient();
                    // if (!hc.send("HEAD",urlEntry)) {
                    //     crawledNum--;
                    //     continue;
                    // }
                    // if (hc.getLastModified() <= crawledDate) {
                    //     System.out.println("not modified " + urlString);
                    //     continue;
                    // } else if (hc.getContentLength() > Crawler.maxFileSize || !typeValid(hc.getContentType())) {
                    //     continue;
                    // } else {
                    //     download(urlEntry);
                    // }
//                } else {
                    //Doc is NOT in the DB                    
                    HttpClient hc = new HttpClient();
                    if (!hc.send("HEAD", urlString)) {
                        // crawledNum--;
//                    		log.debug("id"+id+"\t remove after Head: " + urlString);
                        continue;
                    }
                    if ((hc.getContentLength() < Crawler.maxFileSize) && typeValid(hc.getContentType())) {
                        download(urlString);
                    } else {
                        continue;
                    }
//                }
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }

            //after one loop check status 
//            if (crawledNum <= 0) {
//                flag = true;
//            }
        		} catch (Throwable e) {
        			System.err.println("id"+id+"\t unhandled throwable:\t" + e.getMessage());
        			e.printStackTrace();
        		}
        }

        // return ; //TODO: crawled number

    }

}
