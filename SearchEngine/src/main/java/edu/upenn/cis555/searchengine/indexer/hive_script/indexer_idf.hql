set hive.vectorized.execution.enabled = true;
set hive.vectorized.execution.reduce.enabled = true;

DROP TABLE IF EXISTS s3_indexer;
DROP TABLE IF EXISTS ddb_idf;

CREATE EXTERNAL TABLE s3_indexer
    (word STRING, urlString STRING, len BIGINT, tf DOUBLE, idf DOUBLE)
ROW FORMAT SERDE'org.apache.hadoop.hive.serde2.RegexSerDe'
WITH SERDEPROPERTIES ("input.regex" = "^(.*)\t(.*)\001(.*)\001(.*)\001(.*)$")
LOCATION 's3://cis555indexer/test';

SET dynamodb.throughput.read.percent=1.0;
SET dynamodb.throughput.write.percent=1.0;

CREATE EXTERNAL TABLE ddb_idf 
    (word STRING, idf DOUBLE)
STORED BY 'org.apache.hadoop.hive.dynamodb.DynamoDBStorageHandler' 
TBLPROPERTIES (
    "dynamodb.table.name" = "Indexer_idf", 
    "dynamodb.column.mapping" = "word:word,idf:idf"
);

INSERT OVERWRITE TABLE ddb_idf 
SELECT DISTINCT word, idf FROM s3_indexer;