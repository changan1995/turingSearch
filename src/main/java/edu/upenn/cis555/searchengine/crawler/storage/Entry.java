package edu.upenn.cis555.searchengine.crawler.storage;

import java.util.HashSet;
import java.util.Set;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.S3Link;

@DynamoDBTable(tableName = "Crawed")
public class Entry{
    private String urlString;
    private S3Link contentLink;
    private Set<String> outLinks;

    public Entry(String urlString,S3Link contentLink,Set<String> outLinks){
        this.urlString=urlString;
        this.contentLink=contentLink;
        this.outLinks=outLinks;
    }
    
    public Entry(String urlString,S3Link contentLink){
        this.urlString=urlString;
        this.contentLink=contentLink;
        this.outLinks=new HashSet<>();
    }

    public Entry(String urlString){
        this.urlString=urlString;
        this.contentLink=null;
        this.outLinks=new HashSet<>();
    }

    @DynamoDBHashKey(attributeName = "urlString")    
	public String getUrlString() {
		return urlString;
    }
    
	public void setUrlString(String urlString) {
		this.urlString = urlString;
    }
    
    @DynamoDBAttribute(attributeName = "contentLink")        
	public S3Link getContentLink() {
		return contentLink;
    }
    
	public void setContentLink(S3Link contentLink) {
		this.contentLink = contentLink;
    }
    
    @DynamoDBAttribute(attributeName = "outLinks")            
	public Set<String> getOutLinks() {
		return outLinks;
    }
    
	public void setOutLinks(Set<String> outLinks) {
		this.outLinks = outLinks;
    }
    
    // public void addOutLinks(String outLink){
    //     this.outLinks.add(outLink);
    // }
    
}