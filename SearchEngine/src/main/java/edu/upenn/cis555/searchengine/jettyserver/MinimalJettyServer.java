package edu.upenn.cis555.searchengine.jettyserver;

/**
 * Created by CIS555 Team on 3/10/17.
 */
import java.io.File;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import edu.upenn.cis555.searchengine.servlet.QueryDBMapper;
import edu.upenn.cis555.searchengine.servlet.SymSpell;

public class MinimalJettyServer  {
    
	public static DynamoDBMapper mapper = new DynamoDBMapper(QueryDBMapper.client,DynamoDBMapperConfig.DEFAULT);
	public static BasicAWSCredentials awsCreds = new BasicAWSCredentials(
			"AKIAIYNM4CTBYO3IOVPA", "zwVsbZmrENmqXg011IoJmkCbiKHlKCZzlb3+Lo/o");
	public static AmazonS3 s3 = AmazonS3ClientBuilder.standard()
			.withRegion("us-east-1")
			.withCredentials(new AWSStaticCredentialsProvider(awsCreds))
            .build();   
            
    
    public static void main(String[] args) throws Exception {
        SymSpell.CreateDictionary(args[0], "");
        Server server = new Server(8080);

        WebAppContext context = new WebAppContext();

        context.setDescriptor("conf/web.xml");
        context.setContextPath("/");
        context.setResourceBase(".");
        context.setParentLoaderPriority(false);
        server.setHandler(context);
        
        server.start();
        server.join();
    }
}
