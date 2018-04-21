package edu.upenn.cis555.searchengine.crawler;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import org.jboss.netty.util.internal.ConcurrentHashMap;

import edu.upenn.cis555.searchengine.crawler.info.RobotsTxtInfo;
import edu.upenn.cis555.searchengine.crawler.storage.DBWrapper;
import edu.upenn.cis555.searchengine.crawler.structure.URLEntry;

import org.apache.commons.io.FileUtils;

public class Crawler {
    public static PriorityBlockingQueue<URLEntry> urlToDo;
    public static ConcurrentHashMap<String, RobotsTxtInfo> robotLst;//TODO:concurrent handle
    public static int crawledNum;
    public static BloomFilter<CharSequence> bl;
    public static int maxFileSize;

    //udp settings
    public static InetAddress host = null;
    public static DatagramSocket s = null;
    public static final int THREADNUMS = 10;

    public static void main(String args[]) {
        /**arg0 url to start
         * arg1 the directory holds db environment
         * arg2 int MB of document
         * arg3 maximum number
         * arg4 hostname for monitoring //todo
         */

        //parameter setup
        URL urlCurrent = null;
        try {
            urlCurrent = new URL(args[0]);
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        }
        String dbDirectory = args[1];
        maxFileSize = Integer.parseInt(args[2]) * 1024 * 1024;
        int maxFileNumber = 100;
        String hostname = "cis455.cis.upenn.edu";
        if (args.length > 3) {
            maxFileNumber = Integer.parseInt(args[3]);
            ;
            if (args.length > 4) {
                hostname = args[4];
                if(args.length>5){
                    if(args[5].equals("1")){
                        try {//clean db
							FileUtils.cleanDirectory(new File(dbDirectory));
						} catch (IOException e) {
							e.printStackTrace();
						} 
                    }
                }
            }
        }

        //
        //initial environment
        bl =BloomFilter.create(Funnels.stringFunnel(), 10000);
        urlToDo = new PriorityBlockingQueue<>(maxFileNumber);//in the sput
        robotLst = new ConcurrentHashMap<String, RobotsTxtInfo>();//in the each filter
        urlToDo.add(new URLEntry(urlCurrent, System.currentTimeMillis()));
        DBWrapper dbWrapper = new DBWrapper(dbDirectory);
        crawledNum = maxFileNumber;

        //udp
        try {
            host = InetAddress.getByName(hostname);
            try {
                s = new DatagramSocket();
            } catch (SocketException e) {
                e.printStackTrace();
            }
        } catch (UnknownHostException e1) {
            e1.printStackTrace();
        }

        //thread setup
        ExecutorService executorService = Executors.newCachedThreadPool();
//        List<Future<Integer>> resultList = new ArrayList<Future<Integer>>();
        List<CrawlerWorker> cwLst= new ArrayList<>();

        //thread starts
        for (int i = 0; i < THREADNUMS; i++) {
//            Future<Integer> future = executorService.submit(new CrawlerWorker(i,dbWrapper,crawledNum));
            CrawlerWorker cw = new CrawlerWorker(i,dbWrapper,crawledNum);
            cwLst.add(cw);
            executorService.execute(cw);
//            resultList.add(future);
        }

//        //thread pool finish
//        for (Future<Integer> fs : resultList) {
//            try {
//                System.out.println(fs.get());
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            } catch (ExecutionException e) {
//                e.printStackTrace();
//            } finally {
//                executorService.shutdown();
//            }
//

        System.out.println("finished");
    }
}