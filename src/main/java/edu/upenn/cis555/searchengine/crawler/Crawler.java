package edu.upenn.cis555.searchengine.crawler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.amazonaws.services.waf.model.CreateWebACLRequest;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import edu.upenn.cis555.searchengine.crawler.info.RobotsTxtInfo;
import edu.upenn.cis555.searchengine.crawler.storage.DBWrapper;
import edu.upenn.cis555.searchengine.crawler.structure.URLEntry;
import edu.upenn.cis555.searchengine.crawler.structure.URLList;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

public class Crawler {
	static Logger log = Logger.getLogger(Crawler.class);
	// public static PriorityBlockingQueue<URLEntry> urlToDo;
	public static Map<String, RobotsTxtInfo> robotLst;// TODO:concurrent handle
	public static int crawledNum = 250000;
	// public static BloomFilter<CharSequence> bl;
	public static int maxFileSize = 100 * 1024;

	public static AtomicInteger num = new AtomicInteger(0);

	// udp settings
	public static InetAddress host = null;
	public static DatagramSocket s = null;
	public static int threadNum;
	public static int port;
	public static int hostNum;

	public URLFrontier frontier;
	public URLDistributor distributor;
	public URLReciver receiver;
	public static RobotsRule rule = new RobotsRule();
	private ExecutorService executorService = Executors.newCachedThreadPool();
	public static int index = -1;
	public static String[] workerList;

	public Crawler(int index, String[] workerList, ArrayList<String> seedURL, int threadNum) {
		Spark.port(port);
		
		Spark.threadPool(20, 8, 30000);
		
		Spark.get("/status", new Route() {
			@Override
			public Object handle(Request arg0, Response arg1) {
				return workerList[index] + " crawled:" + num.get();
			}
		});

		Spark.get("/shutdown", new Route() {

			@Override
			public Object handle(Request arg0, Response arg1) {
				Runnable task = new Runnable() {
					
					@Override
					public void run() {
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
						}
						log.debug("Shutdown");
						System.exit(0);
					}
				};
				Thread thread = new Thread(task);
				thread.start();
				return "shutdown";
			}
		});

		Crawler.index = index;
		Crawler.workerList = workerList;
		frontier = new URLFrontier(threadNum, seedURL);
		// distributor = new URLDistributor(index, workerList, frontier);
		receiver = new URLReciver(index, workerList, frontier);
	}

	public static ArrayList<String> parseConfig(String path) throws IOException {
		File config = new File(path);
		BufferedReader reader = new BufferedReader(new FileReader(config));
		String line;
		ArrayList<String> list = new ArrayList<>();
		while ((line = reader.readLine()) != null) {
			list.add(line);
		}
		reader.close();
		return list;
	}

	public static ArrayList<String> parseSeed(String path) throws IOException {
		File config = new File(path);
		BufferedReader reader = new BufferedReader(new FileReader(config));
		String line;
		ArrayList<String> list = new ArrayList<>();
		while ((line = reader.readLine()) != null) {
			list.add(line);
		}
		reader.close();
		return list;
	}

	public void start() {
		// thread starts
		for (int i = 0; i < threadNum; i++) {
			// Future<Integer> future = executorService.submit(new
			// CrawlerWorker(i,dbWrapper,crawledNum));
			CrawlerWorker cw = new CrawlerWorker(i, crawledNum, frontier);
			executorService.execute(cw);
			// executorService.exec
			// resultList.add(future);
		}
	}

	public static void main(String args[]) throws InterruptedException {
		/**
		 * arg0 url to start arg1 the directory holds db environment arg2 int MB of
		 * document arg3 maximum number arg4 hostname for monitoring //todo
		 */
		String hostname = "cis455.cis.upenn.edu";
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
		System.out.print(args.length);
		if (args.length < 2) {
			System.out.println("java -jar configfile index seedURLFile");
			return;
		}
		if (args.length >= 3) {
			threadNum = Integer.parseInt(args[2]);
			log.debug(threadNum  + "\t Threads are setuped");			
		}
		String configPath = args[0];
		int index = Integer.parseInt(args[1]);

		ArrayList<String> workers;
		try {
			workers = parseConfig(configPath);
		} catch (IOException e) {
			System.out.println("Parse config file error.");
			return;
		}

		String[] workerList = workers.toArray(new String[workers.size()]);

		port = Integer.parseInt(workerList[index].split(":")[1]);

		ArrayList<String> seedURL = new ArrayList<>();
		if (args.length >= 4) {
			hostNum = Integer.parseInt(args[3]);
			log.debug(hostNum  + "\t Threads are setuped");			
		}
		if (args.length >= 5) {
			String seedFile = args[4];
			try {
				seedURL = parseSeed(seedFile);
			} catch (IOException e) {
				System.out.println("Parse config seed file error.");
				return;
			}
		}

		DBWrapper.envDirectory += "" + index;
		DBWrapper db = DBWrapper.getInstance();
		db.setUp();

		// parameter setup
		// URL urlCurrent = null;
		// try {
		// urlCurrent = new URL(args[0]);
		// } catch (MalformedURLException e1) {
		// e1.printStackTrace();
		// }
		// String dbDirectory = args[1];
		// maxFileSize = Integer.parseInt(args[2]) * 1024 * 1024;
		// int maxFileNumber = 100;
		// String hostname = "cis455.cis.upenn.edu";
		// if (args.length > 3) {
		// maxFileNumber = Integer.parseInt(args[3]);
		// ;
		// if (args.length > 4) {
		// hostname = args[4];
		// if (args.length > 5) {
		// if (args[5].equals("1")) {
		// try {// clean db
		// FileUtils.cleanDirectory(new File(dbDirectory));
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// }
		// }
		// }
		// }

		//
		// initial environment
		// bl = BloomFilter.create(Funnels.stringFunnel(), 200000);
		// urlToDo = new PriorityBlockingQueue<>(maxFileNumber);// in the sput
		robotLst = Collections.synchronizedMap(new LinkedHashMap<String, RobotsTxtInfo>() {
			@Override
			protected boolean removeEldestEntry(java.util.Map.Entry<String, RobotsTxtInfo> eldest) {
				return size() > 200;
			}
		});
		// urlToDo.add(new URLEntry(urlCurrent, System.currentTimeMillis()));
		// DBWrapper dbWrapper = new DBWrapper(dbDirectory);
		// crawledNum = maxFileNumber;

		// // udp
		// try {
		// host = InetAddress.getByName(hostname);
		// try {
		// s = new DatagramSocket();
		// } catch (SocketException e) {
		// e.printStackTrace();
		// }
		// } catch (UnknownHostException e1) {
		// e1.printStackTrace();
		// }

		Crawler crawler = new Crawler(index, workerList, seedURL,threadNum);

		crawler.start();

		while (true) {
			Thread.sleep(1000000);
		}
	}

}