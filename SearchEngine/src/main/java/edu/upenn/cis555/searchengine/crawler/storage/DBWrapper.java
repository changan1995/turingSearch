package edu.upenn.cis555.searchengine.crawler.storage;

import java.io.File;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.Transaction;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.impl.Store;

// import edu.upenn.cis455.crawler.CrawlerWorker;
// import XPAThcra

public class DBWrapper {
	
	private static String envDirectory = null;
	
	private static Environment myEnv;
	private static EntityStore store;

	private static DocDB docDB;
//	private static UserDB userDB;
	
	public DBWrapper(String envDir){
		envDirectory=envDir;
		try{
            setup();
            this.docDB = new DocDB(this.store);
//            this.userDB = new UserDB(this.store);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

	// singleton
    // private static class DBWrapperHolder {
    //     private static final DBWrapper INSTANCE = new DBWrapper();
    // }

    // public static final DBWrapper getInstance(String envDir) {
    //         envDirectory=envDir;
    //         return DBWrapperHolder.INSTANCE;
    // }
	
	public Environment getEnvironment() { return this.myEnv; }
	public EntityStore getStore() { return this.store; }
	public DocDB getDocDB() { return this.docDB; }
//	public UserDB getUserDB() { return this.userDB; }
	
	public void setup() {
		
		EnvironmentConfig envConfig = new EnvironmentConfig();
		envConfig.setTransactional(true);
		envConfig.setAllowCreate(true);
		File f = new File(this.envDirectory);
		if (!f.exists()) {
			f.mkdir();
		}
		this.myEnv = new Environment(new File(this.envDirectory), envConfig);
		
		StoreConfig storeConfig = new StoreConfig();
		storeConfig.setAllowCreate(true);
		storeConfig.setTransactional(true);
		this.store = new EntityStore(this.myEnv, "store", storeConfig);
	}
	
	public Transaction getTransaction() {
		return this.myEnv.beginTransaction(null, null);
	}
	
	public void close() {
		// first close store
		try {
			store.close();
		} catch(DatabaseException e) {
			System.err.println("Error closing store: " + e.toString());
			System.exit(-1);
		}
		 
		// Close environment
		try {
			myEnv.close();
		} catch(DatabaseException e) {
			System.err.println("Error closing MyDbEnv: " + e.toString());
			System.exit(-1);
		}
	}
	
}
