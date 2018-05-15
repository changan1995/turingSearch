package cs3.cs2.cs.searchengine.pagerank;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "pageranktest")
public class PageRankEntry {
	
	private String urlString;
	private double rank;
	
	public PageRankEntry(String urlString, double rank) {
		this.urlString = urlString;
		this.rank = rank;
	}
	
	@DynamoDBHashKey(attributeName = "urlString")    
	public String getUrlString() {
		return urlString;
    }
    
	public void setUrlString(String urlString) {
		this.urlString = urlString;
    }
    
    @DynamoDBAttribute(attributeName = "rank")        
	public double getRank() {
    		return rank;
    }
    
	public void setRank(double rank) {
		this.rank = rank;
    }

}
