package edu.upenn.cis555.searchengine.crawler.storage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

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

	public static String envDirectory = "./store";

	private Environment myEnv;
	// private static EntityStore store;
	// private HashMap<String, Database> mapping;
	private Database URLFrontier;
	private Database URLSeen;
	private Database classDB;

	private static DBWrapper db;

	private StoredClassCatalog classCatalog;
	
	private static final int maxURL = 50;
	
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
		DatabaseConfig seenConfig = new DatabaseConfig();
		seenConfig.setAllowCreate(true);
		seenConfig.setDeferredWrite(true);
//		seenConfig.setBtreeComparator(LongComparator.class);
		URLSeen = myEnv.openDatabase(null, "urlseen", seenConfig);
	}

	public static DBWrapper getInstance() {
		if (db == null) {
			db = new DBWrapper();
		}
		return db;
	}

	public synchronized void addURL(long time, String url) {
		DatabaseEntry keyEntry = new DatabaseEntry();
		DatabaseEntry dataEntry = new DatabaseEntry();
		LongBinding.longToEntry(time, keyEntry);
		StringBinding.stringToEntry(url, dataEntry);
//		Transaction txn = myEnv.beginTransaction(null, null);
		URLFrontier.put(null, keyEntry, dataEntry);
//		URLFrontier.put(null, keyEntry, dataEntry);
		URLFrontier.sync();
//		txn.commit();
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
		while (cursor.getNext(keyEntry, dataEntry, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
			String url = StringBinding.entryToString(dataEntry);
			list.add(url);
			URLFrontier.delete(null, keyEntry);
			count++;
			if (count >= limit) break;
		}
		URLFrontier.sync();
		return list;
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
	
	
	public boolean checkURLSeen(String url) {
		DatabaseEntry keyEntry = new DatabaseEntry();
		DatabaseEntry dataEntry = new DatabaseEntry();
		StringBinding.stringToEntry(url, keyEntry);
		if (URLSeen.get(null, keyEntry, dataEntry, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
			return true;
		}
		return false;
	}
	

}