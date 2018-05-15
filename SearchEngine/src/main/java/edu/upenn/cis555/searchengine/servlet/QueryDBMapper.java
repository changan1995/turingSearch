package edu.upenn.cis555.searchengine.servlet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

public class QueryDBMapper {
	static BasicAWSCredentials credential = new BasicAWSCredentials(
			"AKIAIGP2KXIDSJESG32A", "5vDxSePUmjLd15uZbVc4wgR2wmJqjVFjB8JCBf7E");
	/*
	 * static BasicAWSCredentials credential = new BasicAWSCredentials(
	 * "AKIAI5IAAU67IX6Y45QA", "BDQWLjoCrCsLzJxzZFrdGyN1YQA/5TA3xAvhNyiu");
	 */
	public static AmazonDynamoDB client = AmazonDynamoDBClientBuilder
			.standard().withRegion("us-east-1")
			.withCredentials(new AWSStaticCredentialsProvider(credential))
			.build(); 

	/*
	 * static AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
	 * .build();
	 */

	
	/* public static void main(String[] args) throws Exception { 
		 try {
			 DynamoDBMapper mapper = new DynamoDBMapper(client);
			 List<Indexer_tf_new> ls=FindScore(mapper, "java");
			 for (Indexer_tf_new i: ls){
				 System.out.println(i.urlString);
				 System.out.println(i.rank);
				 System.out.println(i.tf);
			 }
			 //System.out.println(FindIdf(mapper, "out")); 
			 //FindScore(mapper,"http://ahsaweb.net/"); 
			 //FindScore(mapper, "https://fa.southfront.org");
		 } catch (Throwable t) {
			 System.err.println("Error running the QueryDBMapper: " + t);
			 t.printStackTrace(); 
		}
	}*/
	
	/*static Map<String, Double> FindScore(DynamoDBMapper mapper, Set<String> set){
		Map<String,Double> pageranks=new HashMap<String,Double>();
		for (String s:set){
			Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
			eav.put(":val1", new AttributeValue().withS(s));
			DynamoDBQueryExpression<PageRank> queryExpression = new DynamoDBQueryExpression<PageRank>()
					.withIndexName("urlString-index").withConsistentRead(false)
					.withKeyConditionExpression("urlString = :val1")
					.withExpressionAttributeValues(eav);
			List<PageRank> queryResult = mapper.query(PageRank.class,
					queryExpression);
			double score = queryResult.get(0).getRank();
			pageranks.put(s, score);
		}
		return pageranks;
	}*/
	
	/*static Map<String, Double> FindScore(DynamoDBMapper mapper, Set<String> set){
		Map<String, Double> pageranks=new HashMap<String, Double>();
		ArrayList<PageRank> itemsToGet= new ArrayList<PageRank>();
		for (String s:set){
			PageRank pagerank=new PageRank();
			pagerank.setUrlString(s);
			itemsToGet.add(pagerank);
		}
		Map<String, List<Object>> items=mapper.batchLoad(itemsToGet);
		List<Object> ls=items.get("PageRank");
		for (Object o:ls){
			PageRank pr=(PageRank) o;
			pageranks.put(pr.urlString, pr.rank);
		}
		return pageranks;
	}*/

	
	static double FindIdf(DynamoDBMapper mapper, String word) {
		Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
		eav.put(":val1", new AttributeValue().withS(word));
		DynamoDBQueryExpression<Indexer_idf> queryExpression = new DynamoDBQueryExpression<Indexer_idf>()
				.withKeyConditionExpression("word = :val1")
				.withExpressionAttributeValues(eav);
		List<Indexer_idf> queryResult = mapper.query(Indexer_idf.class,
				queryExpression);
		double idf;
		if (queryResult.isEmpty()) {
			idf = 0;
		} else {
			idf = queryResult.get(0).getIdf();
		}
		return idf;
	}

	
	/*
	 * for Indexer_tf_new i: url=i.getUrlString, pagerank=i.getRank, tf=i.getTf
	 */
	static List<Indexer_tf_new> FindScore(DynamoDBMapper mapper, String word) {
		Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
		eav.put(":val1", new AttributeValue().withS(word));
		DynamoDBQueryExpression<Indexer_tf_new> queryExpression = new DynamoDBQueryExpression<Indexer_tf_new>()
				.withIndexName("word-index").withConsistentRead(false)
				.withKeyConditionExpression("word = :val1")
				.withExpressionAttributeValues(eav);
		List<Indexer_tf_new> scores = mapper.query(Indexer_tf_new.class,
				queryExpression);
		return scores;
	}
	
	/*static Map<String, Double> FindTf(DynamoDBMapper mapper, String word) {
		Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
		eav.put(":val1", new AttributeValue().withS(word));
		DynamoDBQueryExpression<Indexer_tf> queryExpression = new DynamoDBQueryExpression<Indexer_tf>()
				.withIndexName("word-index").withConsistentRead(false)
				.withKeyConditionExpression("word = :val1")
				.withExpressionAttributeValues(eav);
		List<Indexer_tf> queryResult = mapper.query(Indexer_tf.class,
				queryExpression);
		Map<String, Double> tfs = new HashMap<String, Double>();
		for (Indexer_tf indexer : queryResult) {
			String url = indexer.getUrlString();
			double tf = indexer.getTf();
			tfs.put(url, tf);
		}
		return tfs;
	}*/

	/*@DynamoDBTable(tableName = "PageRank")
	public static class PageRank {
		private String urlString;
		private double rank;

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
	}*/

	@DynamoDBTable(tableName = "Indexer_idf")
	public static class Indexer_idf {
		private String word;
		private double idf;

		@DynamoDBHashKey(attributeName = "word")
		public String getWord() {
			return word;
		}

		public void setWord(String word) {
			this.word = word;
		}

		@DynamoDBAttribute(attributeName = "idf")
		public double getIdf() {
			return idf;
		}

		public void setIdf(double idf) {
			this.idf = idf;
		}
	}

	@DynamoDBTable(tableName = "Indexer_tf_new")
	public static class Indexer_tf_new {
		private String id;
		private int len;
		private double rank;
		private double tf;
		private String urlString;
		private String word;

		@DynamoDBHashKey(attributeName = "id")
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@DynamoDBAttribute(attributeName = "len")
		public int getLen() {
			return len;
		}

		public void setLen(int len) {
			this.len = len;
		}

		@DynamoDBAttribute(attributeName = "rank")
		public double getRank() {
			return rank;
		}

		public void setRank(double rank) {
			this.rank = rank;
		}
		
		@DynamoDBAttribute(attributeName = "tf")
		public double getTf() {
			return tf;
		}

		public void setTf(double tf) {
			this.tf = tf;
		}

		@DynamoDBAttribute(attributeName = "urlString")
		public String getUrlString() {
			return urlString;
		}

		public void setUrlString(String urlString) {
			this.urlString = urlString;
		}
		
		@DynamoDBIndexHashKey(globalSecondaryIndexName = "word-index", attributeName = "word")
		public String getWord() {
			return word;
		}

		public void setWord(String word) {
			this.word = word;
		}
	}
}
