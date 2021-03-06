package cs3.cs2.cs.searchengine.crawler.storage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.asynchttpclient.request.body.generator.ByteArrayBodyGenerator;

import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

import cs3.cs2.cs.searchengine.crawler.URLFrontier;
import cs3.cs2.cs.searchengine.crawler.Crawler;

import com.sleepycat.bind.ByteArrayBinding;
import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.bind.tuple.BooleanBinding;
import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.bind.tuple.LongBinding;
import com.sleepycat.bind.tuple.StringBinding;
import com.sleepycat.je.Cursor;
//import com.sleepycat.persist.EntityStore;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DatabaseNotFoundException;

public class DBWrapper {
	
	static Logger log = Logger.getLogger(DBWrapper.class);

	public static String envDirectory = "./store";
	
	public static int endIdx;

	private Environment myEnv;
	// private static EntityStore store;
	// private HashMap<String, Database> mapping;
	private Database URLFrontier;
	private Database URLSeen;
	private Database classDB;
	private Database contentSeen;
	private ByteArrayBinding bb = new ByteArrayBinding();	
	// private 

	private static DBWrapper db;

	private StoredClassCatalog classCatalog;
	
	private static final int maxURL = Crawler.threadNum*5;
	
	private int i=0;
	private int j=0;

	public void setUp() {
		EnvironmentConfig envConfig = new EnvironmentConfig();
		envConfig.setAllowCreate(true);
		envConfig.setTransactional(true);
		File f = new File(envDirectory);
		if (!f.exists())
			f.mkdirs();
		myEnv = new Environment(f, envConfig);
		DatabaseConfig dbConfig = new DatabaseConfig();
		dbConfig.setAllowCreate(true);
		classDB = myEnv.openDatabase(null, "ClassCatalogDB", dbConfig);
		classCatalog = new StoredClassCatalog(classDB);
		DatabaseConfig frontierConfig = new DatabaseConfig();
		frontierConfig.setAllowCreate(true);
		frontierConfig.setDeferredWrite(true);
		URLFrontier = myEnv.openDatabase(null, "frontier", frontierConfig);
		// URLFrontier.
		DatabaseConfig seenConfig = new DatabaseConfig();
		seenConfig.setAllowCreate(true);
		seenConfig.setDeferredWrite(true);
//		seenConfig.setBtreeComparator(LongComparator.class);
		URLSeen = myEnv.openDatabase(null, "urlseen", seenConfig);
		contentSeen = myEnv.openDatabase(null, "contentSeen", seenConfig);
	}

	public static DBWrapper getInstance() {
		if (db == null) {
			db = new DBWrapper();
		}
		return db;
	}

	public synchronized void addURL(long time, String url) {
//		log.debug("save " + url + " at " + time);
		DatabaseEntry keyEntry = new DatabaseEntry();
		DatabaseEntry dataEntry = new DatabaseEntry();
		LongBinding.longToEntry(time, keyEntry);
		StringBinding.stringToEntry(url, dataEntry);
		URLFrontier.put(null, keyEntry, dataEntry);
		URLFrontier.sync();
	}

	public long getSeenCount(){
		return URLSeen.count();
	}

	public long getContentSeenCount(){
		return contentSeen.count();
	}

	public long getFrontierCount(){
		return URLFrontier.count();
	}

	public synchronized ArrayList<String> getURLs(int limit) {
		if (limit == -1) {
			limit = maxURL;
		}
		DatabaseEntry keyEntry = new DatabaseEntry();
		DatabaseEntry dataEntry = new DatabaseEntry();
		Cursor cursor = URLFrontier.openCursor(null, null);

		ArrayList<String> list = new ArrayList<>();
		int count = 0;
		// log.debug("Start get URLs");
		while (cursor.getNext(keyEntry, dataEntry, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
			String url = StringBinding.entryToString(dataEntry);
			list.add(url);
			// log.debug("get:" + url);
			URLFrontier.delete(null, keyEntry);
			count++;
			if (count >= limit) break;
		}
		URLFrontier.sync();
		// log.debug("Stop get URLs");
		return list;
	}

	
	public synchronized String getURL() {
		DatabaseEntry keyEntry = new DatabaseEntry();
		DatabaseEntry dataEntry = new DatabaseEntry();
		Cursor cursor = URLFrontier.openCursor(null, null);
		String urlString =null;
		// log.debug("Start get URLs");
		if(cursor.getNext(keyEntry, dataEntry, LockMode.DEFAULT) == OperationStatus.SUCCESS)
			urlString= StringBinding.entryToString(dataEntry);
			// log.debug("get:" + url);
			URLFrontier.delete(null, keyEntry);
	
		URLFrontier.sync();
		// log.debug("Stop get URLs");
		return urlString;
	}

	public void bulidBL(){
		DatabaseEntry keyEntry = new DatabaseEntry();
		DatabaseEntry dataEntry = new DatabaseEntry();
		Cursor cursor = URLSeen.openCursor(null, null);	
		Cursor contentCursor = contentSeen.openCursor(null, null);
		int i=0;	
		while(cursor.getNext(keyEntry, dataEntry, LockMode.DEFAULT) == OperationStatus.SUCCESS){
			i++;
			// Crawler.bl.put(StringBinding.entryToString(keyEntry));
			// Crawler.bl_content.put()
		}
		
		keyEntry = new DatabaseEntry();
		dataEntry = new DatabaseEntry();
		i=0;
		while(contentCursor.getNext(keyEntry, dataEntry, LockMode.DEFAULT) == OperationStatus.SUCCESS){
			i++;
			// Crawler.bl_content.put(bb.entryToObject(keyEntry));
			// Crawler.bl_content.put()
		}
		System.out.println("built "+i+"seen url");
	}

	
//	@SuppressWarnings({ "rawtypes", "unchecked" })
//	public void saveURLSeen(HashSet<String> set) {
//		DatabaseEntry keyEntry = new DatabaseEntry();
//		DatabaseEntry dataEntry = new DatabaseEntry();
//		IntegerBinding.intToEntry(0, keyEntry);
//		EntryBinding dataBinding = new SerialBinding(classCatalog, URLSeen.class);
//		URLSeen seen = new URLSeen();
//		seen.urlSeen = set;
//	    dataBinding.objectToEntry(seen, dataEntry);
//		URLSeen.put(null, keyEntry, dataEntry);
//		URLSeen.sync();
//	}
	
//	@SuppressWarnings({ "rawtypes", "unchecked" })
//	public HashSet<String> getURLSeen() {
//		DatabaseEntry keyEntry = new DatabaseEntry();
//		DatabaseEntry dataEntry = new DatabaseEntry();
//		Cursor cursor = URLSeen.openCursor(null, null);
//		EntryBinding dataBinding = new SerialBinding(classCatalog, URLSeen.class);
//		if (cursor.getNext(keyEntry, dataEntry, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
//			URLSeen seen = (URLSeen) dataBinding.entryToObject(dataEntry);
//			return seen.urlSeen;
//		} else {
//			return new HashSet<String>();
//		}
//	}
		
	public void saveURLSeen(String url) {
		DatabaseEntry keyEntry = new DatabaseEntry();
		DatabaseEntry dataEntry = new DatabaseEntry();
		StringBinding.stringToEntry(url, keyEntry);
		BooleanBinding.booleanToEntry(true, dataEntry);
	//		Transaction txn = myEnv.beginTransaction(null, null);
		URLSeen.put(null, keyEntry, dataEntry);
	//		URLFrontier.put(null, keyEntry, dataEntry);
		URLSeen.sync();
	}	



	public void saveContentSeen(byte[] contentString) {
		DatabaseEntry keyEntry = new DatabaseEntry();
		DatabaseEntry dataEntry = new DatabaseEntry();
		// StringBinding.stringToEntry(url, keyEntry);

		bb.objectToEntry(contentString, keyEntry);
		BooleanBinding.booleanToEntry(true, dataEntry);
	//		Transaction txn = myEnv.beginTransaction(null, null);
		contentSeen.put(null, keyEntry, dataEntry);
	//		URLFrontier.put(null, keyEntry, dataEntry);
		contentSeen.sync();

		
	}

	public void removeDB(String id) {
		try {
			// close database before remove
			myEnv.removeDatabase(null, id);
		} catch (DatabaseNotFoundException e) {
			System.out.println("Database " + id + " doesn't exist.");
		}
	}
	
	public void closeFrontier(){
		this.URLFrontier.close();
	}

	public boolean checkURLSeen(String url) {
		// DatabaseEntry keyEntry = new DatabaseEntry();
		// DatabaseEntry dataEntry = new DatabaseEntry();
		// StringBinding.stringToEntry(url, keyEntry);
		// if (URLSeen.get(null, keyEntry, dataEntry, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
		// 	return true;
		// }
		// return false;
		return !Crawler.bl.put(url);
	}
	

}