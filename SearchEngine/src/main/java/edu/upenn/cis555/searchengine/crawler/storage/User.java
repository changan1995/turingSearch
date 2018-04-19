package edu.upenn.cis455.storage;


import org.w3c.dom.Document;

import edu.upenn.cis455.crawler.info.URLInfo;
import edu.upenn.cis455.hw1.HttpServer;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

@Entity
public class User {
	
	@PrimaryKey
	private String userName;
		 
	private String password;
	
    public User() {}
    
	public User(String userName, String password) {
        
        this.userName = userName;
		this.password = password;
	}
	
	public String getuserName() { return this.userName; }
	public String getPassword() { return this.password; }
	
	public void setuserName(String url) {
		this.userName = userName;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
    
}
