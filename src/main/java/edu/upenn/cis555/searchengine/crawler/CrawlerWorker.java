package edu.upenn.cis555.searchengine.crawler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import com.sleepycat.je.Transaction;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
// import org.mockito.internal.matchers.Null;

import edu.upenn.cis555.searchengine.crawler.info.RobotsTxtInfo;
import edu.upenn.cis555.searchengine.crawler.info.URLInfo;
// import edu.upenn.cis555.searchengine.crawler.hw1.BlockingQueue;
// import edu.upenn.cis555.searchengine.crawler.hw1.HttpServer;
import edu.upenn.cis555.searchengine.crawler.storage.DBWrapper;
import edu.upenn.cis555.searchengine.crawler.storage.Doc;
import edu.upenn.cis555.searchengine.crawler.storage.DocDB;
import edu.upenn.cis555.searchengine.crawler.structure.URLEntry;

public class CrawlerWorker implements Runnable {
    // public static BlockingQueue<URL> urlToDo;//add seen url
    // public static BlockingQueue<URL> urlDone;
    private boolean flag = false;
    private DBWrapper dbWrapper = null;
    private DocDB docDB = null;
    private boolean updateflag = false;
    private RobotsTxtInfo robot;
    // private long lastCrawedTime = new Long(0);
    // private Transaction txn = null;
    private long toCrawlDate;
    private int id;
    private int crawledNum;
    // public static String dbDirectory;

    public CrawlerWorker(int id,DBWrapper dbWrapper,int crawledNum){
        this.id =id;
        this.dbWrapper=dbWrapper;
        this.docDB =dbWrapper.getDocDB();
        this.crawledNum=crawledNum;
        System.out.println(id + "worker setup");
        // private PriorityQueue<URLEntry> urlToDo = Crawler.urlToDo;
    }

    public void download(URLEntry urlEntry) {
        // long sleeptime=0;
        // long sleeptime = 1000 * delay - System.currentTimeMillis() + lastCrawedTime > 0
        //         ? 1000 * delay - System.currentTimeMillis() + lastCrawedTime
        //         : 0;
        // if(urlEntry.getToCrawlDate()>System.currentTimeMillis()){
        //     try {
		// 		Thread.sleep(1000);
		// 	} catch (InterruptedException e) {
		// 		e.printStackTrace();
		// 	}
        // }
        String url =urlEntry.getUrl().toString();
        HttpClient hc = new HttpClient();
        if (!hc.send("GET", urlEntry)) {
            return;
        }
        System.out.println("downloading " + url);        
        Transaction txn;
        //put the file in to db. prepare for multiple value
        txn = dbWrapper.getTransaction();
        try {
            String lastModified = Utilities.convertTime(hc.getLastModified());
            Doc doc = new Doc(url, hc.getContent(), hc.getContentType(), new Long(hc.getContentLength()), lastModified);
            if (updateflag) {
                docDB.updateDoc(doc, txn);
            } else {
                docDB.insertDoc(doc, txn);
            }
            anaylize(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }

        txn.commit();
        // lastCrawedTime = System.currentTimeMillis();
    }

    //decided whehter it is the form we required
    public static boolean typeValid(String contentType) {
        try {
            contentType = contentType.toLowerCase().trim();
            return (contentType.contains("text/html") || contentType.contains("application/xml")
                    || contentType.endsWith("xml") || contentType.endsWith("xml") || contentType.endsWith("html"));

        } catch (NullPointerException e) {
            // System.out.println("")
            return false;
        }
    }

    //put links generated from JSOUP document to urlToDo, and filter some obviously we dont want
    public void anaylize(Doc doc) {
        if (!doc.getDocType().toLowerCase().contains("html")) {
            return;
        }
        URL url2=null;
		try {
			url2 = new URL(doc.getUrl());
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
        String absHost = url2.getProtocol()+"://"+url2.getHost()+url2.getPath();
        Document document = Jsoup.parse(doc.getContent(),absHost);
        Elements links = document.select("a");
        for (Element link : links) {
            String text = link.absUrl("abs:href");
            String pattern = "^.*\\.(png|jpg|pdf|docx|pptx)$";
            if (Pattern.matches(pattern, text) || text.contains("@")) {
                continue;
            }
            // System.out.println(doc.getUrl()+"\t\t"+text);
            URL url=null;
			try {
				url = new URL(text);
			} catch (MalformedURLException e) {
                // System.out.println("nullpointer"+text);
                // e.printStackTrace();
                continue;
			}
            Crawler.urlToDo.add(new URLEntry(url, toCrawlDate));
        }
    }

    //if we can access return true;
    public boolean checkRobot(URL url) {
        //put all valid links in urlToDo;
        ArrayList<String> allowed;
        ArrayList<String> disallowed;
        URLInfo urlInfo = new URLInfo(url.toString());
        String absoluteRoot = urlInfo.getAbsoluteRoot();
        String hostName = urlInfo.getHostName();
        String agent = null;
        if (hostName.contains("google")) {
            return false;
        }
        robot = Crawler.robotLst.get(hostName);
        if (robot == null) {
            //new host found, download the robot
            // lastCrawedTime = new Long(0);
            HttpClient hc = new HttpClient();
            if (!hc.send("GET", new URLEntry("http://" + url.getHost() + "/robots.txt", System.currentTimeMillis()))) { //turn to absolute address
                robot = new RobotsTxtInfo("User-agent: *", absoluteRoot);
                Crawler.robotLst.put(hostName, robot);
                return true;
            }
            robot = new RobotsTxtInfo(hc.getContent(), absoluteRoot);
            Crawler.robotLst.put(hostName, robot);
        }
        //find allowed/disallowed
        if (robot.containsUserAgent("cis455crawler")) {
            agent = "cis455crawler";
        } else if (robot.containsUserAgent("*")) {
            agent = "*";
        } else {
            return true;
        }

        allowed = robot.getAllowedLinks(agent);
        disallowed = robot.getDisallowedLinks(agent);
        if (disallowed == null) {
            return true;
        }
        if (disallowed.size() == 0) {
            return true;
        }
        for (String s : disallowed) {
            // System.out.println(s);
            if (urlInfo.getUrlNoPort().startsWith(s)) {
                return false;
            }
        }
        System.out.println(id+"gets ");
        toCrawlDate = robot.getNextCrawlDate(agent);
        System.out.println(id+"release ");

        return true;
    }

    // public CrawlerWorker(int id){
    //     this.id =id;
    // }

    @Override
    public void run() {

        while (!flag) {//main loop
            URLEntry urlEntry = null;
            updateflag = false;
            //take out one url
            if (Crawler.urlToDo.isEmpty()) {
//                flag = true;
                // try {
				// 	Thread.sleep(500);
				// } catch (InterruptedException e) {
				// 	e.printStackTrace();
				// }
                continue;
            } else {
                urlEntry = Crawler.urlToDo.poll();
                // System.out.println()
            }
            if(urlEntry==null){
                continue;
            }
            URL urlCurrent = urlEntry.getUrl();
            //Robot check
            if (!checkRobot(urlCurrent)) {
                System.out.println("not allowed: "+urlCurrent.toString());
                continue;
            }

            //send head to check if we need to do download();
            String urlString = urlCurrent.toString();
            try {
                Doc doc = docDB.get(urlString);
                if (doc != null) {
                    //Doc is in the DB
                    updateflag = true;
                    Long crawledDate = Utilities.convertDate(doc.getcrawledDate());
                    //send head to check 
                    HttpClient hc = new HttpClient();
                    if (!hc.send("HEAD",urlEntry)) {
                        crawledNum--;
                        continue;
                    }
                    if (hc.getLastModified() <= crawledDate) {
                        System.out.println("not modified " + urlString);
                        continue;
                    } else if (hc.getContentLength() > Crawler.maxFileSize || !typeValid(hc.getContentType())) {
                        continue;
                    } else {
                        download(urlEntry);
                    }
                } else {
                    //Doc is NOT in the DB                    
                    updateflag = false;
                    HttpClient hc = new HttpClient();
                    if (!hc.send("HEAD", urlEntry)) {
                        crawledNum--;
                        continue;
                    }
                    if ((hc.getContentLength() < Crawler.maxFileSize) && typeValid(hc.getContentType())) {
                        download(urlEntry);
                    } else {
                        continue;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }

            //after one loop check status 
            if (crawledNum <= 0) {
                flag = true;
            }
        }

        return ; //TODO: crawled number

    }

}
