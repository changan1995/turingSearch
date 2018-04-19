package edu.upenn.cis455.crawler;

import edu.upenn.cis455.crawler.info.RobotsTxtInfo;
import edu.upenn.cis455.crawler.info.URLInfo;
import edu.upenn.cis455.hw1.BlockingQueue;
import edu.upenn.cis455.hw1.HttpServer;
import edu.upenn.cis455.storage.DBWrapper;
import edu.upenn.cis455.storage.Doc;
import edu.upenn.cis455.storage.DocDB;
import test.edu.upenn.cis.stormlite.PrintBolt;

import java.io.*;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.mockito.internal.matchers.Null;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.upenn.cis.stormlite.Config;
import edu.upenn.cis.stormlite.LocalCluster;
import edu.upenn.cis.stormlite.Topology;
import edu.upenn.cis.stormlite.TopologyBuilder;
import edu.upenn.cis.stormlite.tuple.Fields;
import java.util.Queue;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sleepycat.je.Database;
import com.sleepycat.je.Transaction;

public class XPathCrawler2 {



    public static BlockingQueue<String> urlToDo;//add seen url
    // public static BlockingQueue<URL> urlDone;
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


	static Logger log = Logger.getLogger(XPathCrawler2.class);

	private static final String SPOUT = "SPOUT";
    private static final String PARSER = "PARSER";
    private static final String CRAWLER = "CRAWLER";
    private static final String PRINT_BOLT = "PRINT_BOLT";
    private static final String FILTER = "FILTER";

    // //if we can access return true;
    // public static boolean checkRobot(URL url) {
    //     //put all valid links in urlToDo;
	// 	ArrayList<String> allowed;
    //     ArrayList<String> disallowed;
    //     URLInfo urlInfo = new URLInfo(url.toString());
    //     String absoluteRoot = urlInfo.getAbsoluteRoot();
    //     String hostName=urlInfo.getHostName();
    //     String agent=null;
    //     if(hostName.contains("google")){
    //         return false;
    //     }
    //     robot = robotLst.get(hostName);
    //     if(robot==null){
    //         //new host found, download the robot
    //         lastCrawedTime = new Long(0);
    //         HttpClient hc = new HttpClient();
    //         if(!hc.send("GET", "https://"+hostName+"/robots.txt")){ //turn to absolute address
    //             robot = new RobotsTxtInfo("User-agent: *",absoluteRoot);
    //             robotLst.put(hostName, robot);
    //             return true;
    //         }
    //         robot = new RobotsTxtInfo(hc.toString(),absoluteRoot);
    //         robotLst.put(hostName, robot);
    //     }
    //     //find allowed/disallowed
    //     if(robot.containsUserAgent("cis455crawler")){
    //         agent="cis455crawler";
    //     }else if(robot.containsUserAgent("*")){
    //         agent="*";
    //     }else{
    //         return true;
    //     }

    //     allowed=robot.getAllowedLinks(agent);
    //     disallowed=robot.getDisallowedLinks(agent);
    //     if(disallowed==null){
    //         return true;
    //     }
    //     if(disallowed.size()==0){
    //         return true;
    //     }
    //     for(String s:disallowed){
    //         // System.out.println(s);
    //         if(urlInfo.getUrlNoPort().startsWith(s)){
    //             return false;
    //         }
    //     }
    //     delay=robot.getCrawlDelay(agent);

    //     return true;
    // }

    public static void main(String args[]) {
        /**arg0 url to start
         * arg1 the directory holds db environment
         * arg2 int MB of document
         * arg3 maximum number
         * arg4 hostname for monitoring //todo
         */

        
        urlToDo = new BlockingQueue<>();//in the sput
        String urlCurrent = null;
        urlCurrent = args[0];
        try {
			urlToDo.put(urlCurrent);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

        Config config = new Config();
        config.put("dbDirectory", args[1]);
        config.put("maxFileSize", Integer.parseInt(args[2])*1024*1024+"");
        int maxFileNumber = 100;
        String hostname = "cis455.cis.upenn.edu";
        
        if (args.length > 3) {
            maxFileNumber = Integer.parseInt(args[3]);
            ;
            if (args.length > 4) {
                hostname = args[4];
            }
        }
        
        config.put("maxFileNumber", maxFileNumber+"");
        config.put("hostname", hostname);

        //udp
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
        
        PrintBolt printer = new PrintBolt();        
        CrawlerQueueSpout spout = new CrawlerQueueSpout();
        DocumentParserBolt parser = new DocumentParserBolt();
        UrlFilterBolt filter = new UrlFilterBolt();
        CrawlerBolt crawler = new CrawlerBolt();


        // CrawlerQueueSpout ==> DocumentParserBolt ==> UrlFilterBolt
        TopologyBuilder builder = new TopologyBuilder();

        // Only one source ("spout") for the words
        builder.setSpout(SPOUT, spout, 1);
        
        // Four parallel word counters, each of which gets specific words
        builder.setBolt(CRAWLER, crawler, 4).shuffleGrouping(SPOUT);

        // Four parallel word counters, each of which gets specific words
        builder.setBolt(PARSER, parser, 4).shuffleGrouping(CRAWLER);
        
        // A single printer bolt (and officially we round-robin)
        builder.setBolt(FILTER, filter, 4).shuffleGrouping(PARSER);    

        // //testing output
        // String end =SPOUT;    
        // builder.setBolt(PRINT_BOLT, printer, 4).shuffleGrouping(end);


        LocalCluster cluster = new LocalCluster();
        Topology topo = builder.createTopology();

        ObjectMapper mapper = new ObjectMapper();
		try {
			String str = mapper.writeValueAsString(topo);
			
			System.out.println("The StormLite topology is:\n" + str);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        
        cluster.submitTopology("test", config,
        		builder.createTopology());
        try {
			Thread.sleep(1000000);
		} catch (InterruptedException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
        cluster.killTopology("test");
        cluster.shutdown();
        System.exit(0);
    }
}