package edu.upenn.cis555.searchengine.crawler.storage;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.S3Link;
import com.amazonaws.services.s3.model.S3ObjectId;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

import org.omg.IOP.TAG_ORB_TYPE;



public class DB{
    private List<Entry> buffer; 
    private DynamoDBMapper mapper;
    private String bucketName ="cis555test5nodes";
    private String envDirectory = "crawled/";
    

    public DB(){
        this.buffer = new ArrayList<>();
        BasicAWSCredentials awsCreds = new BasicAWSCredentials("AKIAJJCSNAGYGNT4OWWA", "+oEGkYblnrQ2I08UoOKmLiiUHbBZp6nzi+KvB0Ff");
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
        if(this.size()>=20){
            flush();
        }
        try{
            this.buffer.add(entry);
        }catch(Throwable e){
            System.err.println("err in add to buffer ,not send");
        }
        return true;
    }

    public synchronized boolean flush(){

        System.out.println("flushing buffer");
        try{

        mapper.batchSave(buffer);
        }catch(Throwable t){
            t.printStackTrace();
            System.err.println("err in sending buffer");
            return false;

        }
        buffer.clear();
        return true;
    }
    
    
//    public List<Entry> getAllDocEntry() {
//    		
//    }


}