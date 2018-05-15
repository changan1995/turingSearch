package cs3.cs2.cs.searchengine.servlet;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import cs3.cs2.cs.searchengine.jettyserver.MinimalJettyServer;

public class Cache {

	/*BasicAWSCredentials awsCreds = new BasicAWSCredentials(
			"AKIAIYNM4CTBYO3IOVPA", "zwVsbZmrENmqXg011IoJmkCbiKHlKCZzlb3+Lo/o");
	AmazonS3 s3 = AmazonS3ClientBuilder.standard()
			.withRegion("us-east-1")
			.withCredentials(new AWSStaticCredentialsProvider(awsCreds))
			.build();*/

	// public AmazonS3 s3=AmazonS3ClientBuilder.defaultClient();
	public static String bucketName = "turingSearch-crawler-changanw-2";
	public String envDirectory = "test3/";

	public String getContent(String url) {
		String content = MinimalJettyServer.s3.getObjectAsString(bucketName, envDirectory + url);
		return content;
	}

	public static void main(String[] args) {
		try {
			String url = URLEncoder.encode("http://www.legis.state.la.us/",
					"UTF-8");
			Cache cache = new Cache();
			System.out.println(cache.getContent(url));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
