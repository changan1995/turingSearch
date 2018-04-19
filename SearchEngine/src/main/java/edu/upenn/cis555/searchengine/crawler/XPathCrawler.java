package edu.upenn.cis555.searchengine.crawler;

import edu.upenn.cis555.searchengine.crawler.info.RobotsTxtInfo;
import edu.upenn.cis555.searchengine.crawler.info.URLInfo;
// import edu.upenn.cis555.searchengine.crawler.hw1.BlockingQueue;
// import edu.upenn.cis555.searchengine.crawler.hw1.HttpServer;
import edu.upenn.cis555.searchengine.crawler.storage.DBWrapper;
import edu.upenn.cis555.searchengine.crawler.storage.Doc;
import edu.upenn.cis555.searchengine.crawler.storage.DocDB;
import java.io.*;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
// import org.mockito.internal.matchers.Null;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Pattern;

import com.sleepycat.je.Database;
import com.sleepycat.je.Transaction;

public class XPathCrawler {
    // public static BlockingQueue<URL> urlToDo;//add seen url
    public static PriorityQueue<URLComparable> urlToDo;
    public static BlockingQueue<URL> urlDone;
    public static HashMap<String,RobotsTxtInfo> robotLst;
    public static boolean flag = false;
    public static DBWrapper dbWrapper = null;
    public static DocDB docDB =null;
    public static boolean updateflag=false;
    public static RobotsTxtInfo robot;
    public static long lastCrawedTime=new Long(0);
    public static Transaction txn =null;
    public static long delay;
    public static int crawledNum=0;
    public static DatagramSocket s=null;
    public static String dbDirectory;
    public static InetAddress host =null;

    public static void download(String url) {
        // long sleeptime=0;
        long sleeptime= 1000*delay-System.currentTimeMillis()+lastCrawedTime>0?1000*delay-System.currentTimeMillis()+lastCrawedTime:0;
        try {
            // System.out.println(sleeptime);
			Thread.sleep(sleeptime);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
        System.out.println("downloading "+url);
        HttpClient hc = new HttpClient();
        if(!hc.send("GET", url)){
            return;
        }

        
        //put the file in to db. prepare for multiple value
        txn = dbWrapper.getTransaction();
        try {
            String lastModified = Utilities.convertTime(hc.getLastModified());
            Doc doc = new Doc(url,hc.toString(),hc.getContentType(),new Long(hc.getContentLength()),lastModified);
            if(updateflag){
                docDB.updateDoc(doc, txn);
            }else{
                docDB.insertDoc(doc, txn);
            }
            anaylize(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }

        txn.commit();
        lastCrawedTime = System.currentTimeMillis();
    }

    //decided whehter it is the form we required
    public static boolean typeValid(String contentType) {
        try{
            contentType=contentType.toLowerCase().trim();
            return (contentType.equals("text/html")||contentType.equals("application/xml")||contentType.endsWith("+xml")||contentType.endsWith("xml")||contentType.endsWith("html"));

        }catch(NullPointerException e){
            // System.out.println("")
            return false;
        }
    }

    //put links generated from JSOUP document to urlToDo, and filter some obviously we dont want
    public static void anaylize(Doc doc) {
        if(!doc.getDocType().toLowerCase().endsWith("html")){
            return;
        }
        String absHost=doc.getAbsHost();
        Document document = Jsoup.parse(doc.getContent(),absHost);
        Elements links = document.select("a[href]");
        for(Element link:links){
            String text= link.attr("abs:href");
            String pattern = "^.*\\.(png|jpg|pdf|docx|pptx)$";
            if(Pattern.matches(pattern, text)||text.contains("@")){
                continue;
            }
            try {
                urlToDo.add(new URLComparable(text, toCrawlDate));
			} catch (MalformedURLException e) {
                try {
					urlToDo.put(new URL(doc.getUrl()+text));
				} catch (MalformedURLException | InterruptedException e1) {
					e1.printStackTrace();
				}
                // System.err.println("missedparsing:"+text);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        }
    }

    //if we can access return true;
    public static boolean checkRobot(URL url) {
        //put all valid links in urlToDo;
		ArrayList<String> allowed;
        ArrayList<String> disallowed;
        URLInfo urlInfo = new URLInfo(url.toString());
        String absoluteRoot = urlInfo.getAbsoluteRoot();
        String hostName=urlInfo.getHostName();
        String agent=null;
        if(hostName.contains("google")){
            return false;
        }
        robot = robotLst.get(hostName);
        if(robot==null){
            //new host found, download the robot
            lastCrawedTime = new Long(0);
            HttpClient hc = new HttpClient();
            if(!hc.send("GET", "https://"+hostName+"/robots.txt")){ //turn to absolute address
                robot = new RobotsTxtInfo("User-agent: *",absoluteRoot);
                robotLst.put(hostName, robot);
                return true;
            }
            robot = new RobotsTxtInfo(hc.toString(),absoluteRoot);
            robotLst.put(hostName, robot);
        }
        //find allowed/disallowed
        if(robot.containsUserAgent("cis455crawler")){
            agent="cis455crawler";
        }else if(robot.containsUserAgent("*")){
            agent="*";
        }else{
            return true;
        }

        allowed=robot.getAllowedLinks(agent);
        disallowed=robot.getDisallowedLinks(agent);
        if(disallowed==null){
            return true;
        }
        if(disallowed.size()==0){
            return true;
        }
        for(String s:disallowed){
            // System.out.println(s);
            if(urlInfo.getUrlNoPort().startsWith(s)){
                return false;
            }
        }
        delay=robot.getCrawlDelay(agent);

        return true;
    }

    public static void main(String args[]) {
        /**arg0 url to start
         * arg1 the directory holds db environment
         * arg2 int MB of document
         * arg3 maximum number
         * arg4 hostname for monitoring //todo
         */


        //initial environment
        URL urlCurrent = null;
        try {
            urlCurrent = new URL(args[0]);
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        }
        dbDirectory = args[1];
        int maxFileSize = Integer.parseInt(args[2])*1024*1024;
        int maxFileNumber = 100;
        String hostname = "cis455.cis.upenn.edu";
        if (args.length > 3) {
            maxFileNumber = Integer.parseInt(args[3]);
            ;
            if (args.length > 4) {
                hostname = args[4];
            }
        }
        urlToDo = new BlockingQueue<URL>(maxFileNumber);//in the sput
        robotLst = new HashMap<String,RobotsTxtInfo>();//in the each filter
        dbWrapper = DBWrapper.getInstance(dbDirectory);
        crawledNum=maxFileNumber;
        try {
            host = InetAddress.getByName(hostname);
            try {
				s= new DatagramSocket();
			} catch (SocketException e) {
				e.printStackTrace();
			}
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
        
        


        //set up, put in initial one, setup database.
        try {
            urlToDo.put(urlCurrent);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        docDB = dbWrapper.getDocDB();
        //set up end

        while (!flag) {//main loop

            updateflag=false;
            //take out one url
            if(urlToDo.isEmpty()){
                flag=true;
                continue;
            }else{
                try {
                    urlCurrent = urlToDo.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            //Robot check
            if(!checkRobot(urlCurrent)){
                continue;
            }

            //send head to check if we need to do download();
            String urlString = urlCurrent.toString();   
            try {
                Doc doc = docDB.get(urlString);
                if (doc != null) {
                    //Doc is in the DB
                    updateflag=true;
                    Long crawledDate = Utilities.convertDate(doc.getcrawledDate());
                    //send head to check 
                    HttpClient hc = new HttpClient();
                    if(!hc.send("HEAD", urlString)){
                        crawledNum--;
                        continue;
                    }
                    if (hc.getLastModified() <= crawledDate) {
                        System.out.println("not modified "+urlString);
                        continue;
                    }else if(hc.getContentLength() > maxFileSize || !typeValid(hc.getContentType())){
                        continue;
                    } else {
                        download(urlString);
                    }
                } else {
                    //Doc is NOT in the DB                    
                    updateflag=false;
                    HttpClient hc = new HttpClient();
                    if(!hc.send("HEAD", urlString)){
                        crawledNum--;                        
                        continue;
                    }
                    if((hc.getContentLength()<maxFileSize)&&typeValid(hc.getContentType())){
                        download(urlString);
                    }else{
                        continue;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }

            //after one loop check status 
            if (crawledNum<=0) {
                flag = true;
            }
        }

        //finished
        dbWrapper.close();

        System.out.println("finished");
    }

    public class URLComparable implements Comparable<URLComparable>{
        private String url;
        private long toCrawlDate;

        public URLComparable(String url,long toCrawlDate){
            this.url=url;
            this.toCrawlDate = toCrawlDate;
        }

		@Override
		public int compareTo(URLComparable o) {
            URLComparable u = (URLComparable)o;
            
            return (int)(u.getToCrawlDate() - this.toCrawlDate);
        }

		/**
		 * @return the url
		 */
		public String getUrl() {
			return url;
		}

		/**
		 * @param url the url to set
		 */
		public void setUrl(String url) {
			this.url = url;
		}

		/**
		 * @return the toCrawlDate
		 */
		public long getToCrawlDate() {
			return toCrawlDate;
		}

		/**
		 * @param toCrawlDate the toCrawlDate to set
		 */
		public void setToCrawlDate(long toCrawlDate) {
			this.toCrawlDate = toCrawlDate;
		}

    }

}
