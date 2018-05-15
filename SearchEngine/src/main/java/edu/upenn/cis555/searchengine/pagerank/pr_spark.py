
# coding: utf-8

# The setup script is from CIS545

import os

os.environ['SPARK_HOME'] = '/usr/lib/spark'
os.environ['SPARK_LOG_DIR'] = '/var/log/spark'
os.environ['HADOOP_HOME'] = '/usr/lib/hadoop'
os.environ['PYSPARK_SUBMIT_ARGS'] = '"--name" "PySparkShell" "pyspark-shell"'
os.environ['PYTHONSTARTUP'] = '/usr/lib/spark/python/pyspark.shell.py'
os.environ['HADOOP_CONF_DIR'] = '/etc/hadoop/conf'
os.environ['SPARK_ENV_LOADED'] = '1'
os.environ['AWS_PATH'] = '/opt/aws'
os.environ['AWS_AUTO_SCALING_HOME'] = '/opt/aws/apitools/as'
os.environ['SPARK_DAEMON_JAVA_OPTS'] = ' -XX:OnOutOfMemoryError=\'kill -9 %p\''
os.environ['SPARK_WORKER_DIR'] = '/var/run/spark/work'
os.environ['SPARK_SCALA_VERSION'] = '2.10'

import sys
sys.path.append('/usr/lib/spark/python/lib/py4j-0.10.4-src.zip')
sys.path.append('/usr/lib/spark/python/')

import pyspark
from pyspark.sql import SparkSession


def map_row(r):
    if len(r) == 1:
        return []
    else:
        pair = []
        # seperator \002
        for i in r[1].split('\002'):
            if i != '':
                pair.append((r[0], i))
        return pair
#         return [(r[0], i) for i in r[1].split(' ')]
    
def distribute_weight(line):
    num = len(line[1][0])
    for url in line[1][0]:
        # evenly distrubute rank to each links
        yield (url, line[1][1] / num)

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: pagerank <input> <output>")
        exit(-1)
    spark = SparkSession.builder.appName('SparkPageRank').getOrCreate()
    origin = spark.read.text(sys.argv[1]).rdd.map(lambda r: r[0].split('\t',1))
    crawled_sdf = origin.map(lambda r: [r[0]]).toDF(['crawled'])
    all_sdf = origin.flatMap(lambda r: map_row(r)).toDF(['from', 'to'])
    # remove dangling (not crawled docs)
    init_sdf = all_sdf.join(crawled_sdf, all_sdf['to'] == crawled_sdf['crawled'])['from', 'to']
    # init keeps the structure of web graph, cache the init in memory, reuse for every iteration
    init = init_sdf.rdd.groupByKey().cache()
    # initial value 1.0
    ranks = crawled_sdf.rdd.map(lambda crawled: (crawled[0], 1.0))
    
    damping = 0.85
    # 40 iterations
    for i in range(40):
        # format of each line : (url, ([outlinks], rank))
        distributions = init.join(ranks) \
                    .flatMap(lambda line: distribute_weight(line))
        ranks = distributions.reduceByKey(lambda a, b: a + b) \
                .mapValues(lambda rank: rank * damping + 1 - damping)
        print(str(i) + ": done")
    ranks.saveAsTextFile(sys.argv[2])
    spark.stop()
    

