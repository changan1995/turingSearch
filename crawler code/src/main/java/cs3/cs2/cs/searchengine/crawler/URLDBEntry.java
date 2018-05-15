package cs3.cs2.cs.searchengine.crawler;

import java.util.Set;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.datamodeling.S3Link;

@DynamoDBTable(tableName = "user-table")
public class URLDBEntry {
    private String urlString;
    private S3Link contentLink;
    private Set outLink;

    @DynamoDBHashKey
    public String getUrl() {return urlString;}
    public void setUrl(String urlString) {this.urlString = urlString;}

    @DynamoDBAttribute
    public S3Link getContentLink() {return contentLink;}
    public void setConentLink(S3Link content) {this.contentLink = content;}

    
}