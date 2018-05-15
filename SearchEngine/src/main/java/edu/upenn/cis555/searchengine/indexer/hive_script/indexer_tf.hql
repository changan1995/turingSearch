set hive.vectorized.execution.enabled = true;
set hive.vectorized.execution.reduce.enabled = true;

DROP TABLE IF EXISTS s3_indexer;
DROP TABLE IF EXISTS ddb_tf;
DROP TABLE IF EXISTS ddb_pr;

CREATE EXTERNAL TABLE s3_indexer
    (word STRING, urlString STRING, len BIGINT, tf DOUBLE, idf DOUBLE)
ROW FORMAT SERDE'org.apache.hadoop.hive.serde2.RegexSerDe'
WITH SERDEPROPERTIES ("input.regex" = "^(.*)\t(.*)\001(.*)\001(.*)\001(.*)$")
LOCATION 's3://cis555indexer/output_final';

SET dynamodb.throughput.read.percent=1.0;
SET dynamodb.throughput.write.percent=1.0;

CREATE EXTERNAL TABLE ddb_tf 
    (id STRING, word STRING, urlString STRING, len BIGINT, tf DOUBLE, rank DOUBLE)
STORED BY 'org.apache.hadoop.hive.dynamodb.DynamoDBStorageHandler' 
TBLPROPERTIES (
    "dynamodb.table.name" = "Indexer_tf_new", 
    "dynamodb.column.mapping" = "id:id,word:word,urlString:urlString,len:len,tf:tf,rank:rank"
);

CREATE EXTERNAL TABLE ddb_pr 
    (urlString STRING, rank DOUBLE)
STORED BY 'org.apache.hadoop.hive.dynamodb.DynamoDBStorageHandler' 
TBLPROPERTIES (
    "dynamodb.table.name" = "PageRank", 
    "dynamodb.column.mapping" = "urlString:urlString,rank:rank"
);

INSERT OVERWRITE TABLE ddb_tf 
SELECT uuid() as id, word, ddb_pr.urlString, len, tf, ddb_pr.rank FROM s3_indexer JOIN ddb_pr ON (s3_indexer.urlString = ddb_pr.urlString);