package edu.upenn.cis555.searchengine.indexer;
/**
 * author: Yi Guo
 * Description: Wrapped class of AmazonS3 client
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
public class S3Client {
	public static final String BUCKET_NAME="cis455-crawler-changanw-2";
	private AmazonS3 s3Client =null;
	public S3Client(){
		//For EC2
//		s3Client = AmazonS3ClientBuilder.defaultClient();
		//For local test
		BasicAWSCredentials awsCreds = new BasicAWSCredentials("AKIAIYNM4CTBYO3IOVPA", "zwVsbZmrENmqXg011IoJmkCbiKHlKCZzlb3+Lo/o");
		s3Client = AmazonS3ClientBuilder.standard()
								.withRegion("us-east-1")
		                        .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
		                        .build();
	}
	public String getContent(String s3key){
		S3Object obj= s3Client.getObject(BUCKET_NAME, s3key);
		InputStream in = obj.getObjectContent();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		StringBuilder sb = new StringBuilder();
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		}catch (AmazonServiceException ASE) {
            System.out.println("AmazonServiceException caught!");
            System.out.println("Error: " + ASE.getMessage());
            System.out.println("HTTP Status Code: " + ASE.getStatusCode());
            System.out.println("AWS Error Code: " + ASE.getErrorCode());
            System.out.println("Error Type: " + ASE.getErrorType());
            System.out.println("Request ID: " + ASE.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("AmazonClientException caught!");
            System.out.println("Error Message: " + ace.getMessage());
		}catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("IOException caught!");
			e.printStackTrace();
		}
		return sb.toString();
	}
}
