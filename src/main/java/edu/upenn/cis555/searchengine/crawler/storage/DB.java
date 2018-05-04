package edu.upenn.cis555.searchengine.crawler.storage;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper.FailedBatch;

import org.apache.log4j.Logger;



public class DB{

    static Logger log = Logger.getLogger(DB.class);
    
    private List<Entry> buffer; 
    private DynamoDBMapper mapper;
    private String bucketName ="cis455-crawler-changanw-2";
    private String envDirectory = "test3/";
    

    public DB(){
        this.buffer = new ArrayList<>();
        BasicAWSCredentials awsCreds = new BasicAWSCredentials("AKIAJJVOKDS4O2JIIO5Q", "PRB3moNWX5FZn5AdqNWwOvw/X6WfpI0gklm/MKQW");
        // AWSCredentialsProvider s3Credential = new AWSCredentialsProviderChain(credentialsProviders)
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                                .withRegion("us-east-1")
                                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                                .build();
        
        try {
            AWSCredentialsProvider s3Creds = new AWSStaticCredentialsProvider(awsCreds);
            mapper = new DynamoDBMapper(client,s3Creds); 
            System.out.println("DB established!");

        } catch (Throwable t) {
            System.err.println("Error running the DynamoDBMapperBatchWriteExample: " + t);
            t.printStackTrace();
        }
    }

    public void setContentLink(Entry entry,String content){
        try{
            
            entry.setContentLink(mapper.createS3Link(bucketName, envDirectory+URLEncoder.encode(entry.getUrlString(),"UTF-8")));
            // S#Object stringObject = new S3Object()
            entry.getContentLink().uploadFrom(content.getBytes());
        }catch(Throwable t){
            System.err.println("err in upload & create S3Link");
            return;
        }
        return;
    }

    private static class DBHolder{
        private static final DB INSTANCE = new DB();
    }

    public static final DB gettInstance(){
        return DBHolder.INSTANCE;
    }

    public int size(){
        return buffer.size();
    }

    public synchronized boolean add(Entry entry){
        // if you want to upload, use this.
        // if(this.size()>=23){
        //     flush();
        // }
        // try{
        //     this.buffer.add(entry);
        // }catch(Throwable e){
        //     System.err.println("err in add to buffer ,not send");
        //     return false;
        // }
        return true;
    }

    public synchronized boolean flush(){

        System.out.println("flushing buffer");
        try{

        List<FailedBatch> failed =  mapper.batchSave(buffer);
        
        // failed.
        System.err.println(failed.size());
            // failed.get(0).
        // mapper.re
        }catch (AmazonServiceException ase) {
            log.debug("send error"+ase.getMessage());
            System.err.println("Could not complete operation");
            System.err.println("Error Message:  " + ase.getMessage());
            System.err.println("HTTP Status:    " + ase.getStatusCode());
            System.err.println("AWS Error Code: " + ase.getErrorCode());
            System.err.println("Error Type:     " + ase.getErrorType());
            System.err.println("Request ID:     " + ase.getRequestId());
        
        } catch (AmazonClientException ace) {
            log.debug("Error Message:  " + ace.getMessage());
            System.err.println("Internal error occured communicating with DynamoDB");
            System.out.println("Error Message:  " + ace.getMessage());
        }catch(Throwable t){
            t.printStackTrace();
            System.err.println("err in sending buffer");
            return false;

        }
        buffer.clear();
        return true;
    }
    
}