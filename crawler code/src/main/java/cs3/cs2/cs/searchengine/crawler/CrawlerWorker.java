package cs3.cs2.cs.searchengine.crawler;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
// import org.mockito.internal.matchers.Null;

import cs3.cs2.cs.searchengine.crawler.info.RobotsTxtInfo;
import cs3.cs2.cs.searchengine.crawler.storage.DB;
// import cs3.cs2.cs.searchengine.crawler.hw1.BlockingQueue;
// import cs3.cs2.cs.searchengine.crawler.hw1.HttpServer;
import cs3.cs2.cs.searchengine.crawler.storage.DBWrapper;
import cs3.cs2.cs.searchengine.crawler.storage.Entry;

public class CrawlerWorker implements Runnable {
	
	static Logger log = Logger.getLogger("debugLogger");
    private boolean flag = false;
    private RobotsTxtInfo robot;
    private long toCrawlDate;
    private int id;
    private DB db;
    public static final int bodyLength = 350;
    private int crawledNum;
    private Entry entry =null;
    private URLFrontier frontier;
    private DBWrapper dbWrapper;
	private URLDistributor distributor;
	public static Pattern pattern = Pattern.compile("^http[s]?://.*(facebook|google|twitter|amazon|linkedin|pornhub|weibo|instagram|blogspot|tumblr)\\.com.*");
	public static Pattern blackLst = Pattern.compile("cn|.39.|163|nuomi|as.com|image|.ro|glassdoor|fr.|kwnews|academia|getithalfoff|cn|search|gril|porn|sex|create|subscribe|jp|kr|korea|hao123|qq|jd|shanghai|beijing|china|360|climatemps|hao315|leju|kankan|pussy|blog|fangjia|fangzi|cheshi|fuck|fbi");
	public static Pattern blackLst2 = Pattern.compile("wmflabs|log|walla|.il|neatoshop|.fr|fr.|.pl|.it|it.|.es|.tw|.ch|.ia|youtube|xtools|wikimedia|search");
    
    public CrawlerWorker(int id, int crawledNum, URLFrontier frontier){
        this.id =id;
        this.db = new DB();
        this.crawledNum=crawledNum;
        this.frontier = frontier;
        System.out.println(id + "worker setup");
        this.distributor = new URLDistributor(Crawler.index, Crawler.workerList, frontier);
        dbWrapper=DBWrapper.getInstance();
    }

    public void download(String url) throws Exception {
        HttpClient hc = new HttpClient();
        
        if (!hc.send("GET", url)) {
            return;
        }
        //put the file in to db. prepare for multiple value
        try {
            entry= new Entry(url);
            String contentString =hc.getContent();

//        log.debug("id"+id+"\tUpDynamoing:\t" + url);   
            MessageDigest digest=null;
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            byte[] encoded = digest.digest(contentString.getBytes(StandardCharsets.UTF_8));     
            if(!Crawler.bl_content.put(encoded)){
                return;
            }
            dbWrapper.saveContentSeen(encoded);
            db.setContentLink(entry, contentString);
            anaylize(url,contentString);
           if(db.add(entry)){
            log.debug("id"+id+"\tDownloaded:\t" + url);                    
           }else{
            log.debug("id"+id+"\tFailededed:\t" + url);                                
           }
            Crawler.num.incrementAndGet();
//        log.debug("id"+id+"\tUpDynamoDBed:\t" + url);        
            
        } catch (Exception e) {
            e.printStackTrace();
        }

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
            if(text.length()>120)
                continue;
            // System.out.println(doc.getUrl()+"\t\t"+text);
			try {
                URL url = new URL(text);
                String host = url.getHost();
                if(blackLst.matcher(host).find()||blackLst2.matcher(host).find()){
                    continue;
                }
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
                
        // if(!distributor.urlFrontierFull()){
            distributor.distributeURL(outLinksBuff);
        
        // }
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
