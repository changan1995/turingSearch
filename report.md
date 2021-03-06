# TuringSearch
TuringSearch is based on traditional searching & crawler structure. The distributed crawler established on AWS ec2s,
with high efficiency and scalability. The Pagerank and Indexer is implemented to support a query style search from the user-interface.

## author

Quankang Wang: changanw@seas.upenn.edu	
Mojiang Jia: mojjia@seas.upenn.edu
Yitong Long: yitongl@seas.upenn.edu		
Yi Guo: guoyi1@seas.upenn.edu

# Introduction

##  1.1 Approach
Our Turing Search Engine consists of 4 main components, Crawler, Indexer, PageRank and User Interface. The approaches we used are shown below.

##### Crawler: 
We implemented distributed crawlers communicating with each other by RESTFUL API. We maintained a cached LRU(100), containing both URL and content SHA256 and used Consumer (take url and download), producer( parse and put url) to avoid trap and filter duplicated URL..

##### Indexer: 
MapReduce was used to calculate the value of tf and idf. We used EMR for map reduce process and stored the tables in DynamoDB for query. For keyword stemming, we chose to use snowball,a lightweight pure-algo open source stemmer.

##### PageRank Engine: 
Given the crawled information, we used Hadoop MapReduce to implement a iterative PageRank algorithm, designed a data encoding to serve the output of previous iteration as input to the next. We used Random Surfer Model by adding a decay factor prevent “sinks” and “hogs”. Web graphs in Stanford Large Network Dataset Collection (https://snap.stanford.edu/data/) was used  to test correctness.

##### User interface: 
The user interface is implemented by java servlet. The search interface was written by JavaScript, HTML and CSS. As to the result interface, we ranked the results by combining TF/IDF values and PageRank scores. The search results from Google map and Yelp were integrated by using the APIs.

## 1.2 Division of Labor

* Quankang Wang:Crawler, Google map Search, spell Checking ranking mathematic support.
* Yi Guo: Index and reversed index, ranking mathematic support.
* Mojiang Jia: PageRank, crawler， ranking mathematic support.
* Yitong Long: User Interface, Yelp search support, ranking system.
<!-- 
##1.3 Timeline
Milestone 0: Made a project plan (April 6th)
Milestone 1: Set up Amazon EC2, S3, DynamoDB (April 14th)
Milestone 2: Deployed crawler on EC2 (April 23rd)
Milestone 3: Finished Indexer and Pagerank and started running them on EMR(April 27th)
Milestone 4: Started integration (April 30th)
Milestone 5: Improved ranking performance (May 3rd)
Milestone 6: Finished final report (May 4th) -->

# Architecture
Crawler, Indexer, PageRank and User Interface are four main components of our search engine.  The crawler crawled a huge amount of web pages and store them in the database. The indexer took words and other information from the crawler to create an index. Also, the PageRank did link analysis of ths web pages crawled. Finally, by combining the results of index and PageRank and other factors, the search engine ranked the search results and showed them in the user interface. 

# Implementation
## 3.1 Crawler
The crawler is derived from the [paper](https://doi.org/10.1023/A:1019213109274) of Allan Heydon and Marc Najork.We modified the structure according to our goal. 
![image](http://github.com/changan1995/turingSearch/raw/master/figure/figure1.png)


URL Frontier:  We have a priority queue of next crawl date along with LRU robots to maintain information from hosts, and each entry in the priority queue mapping with another queue of URLs belong to the host. 
Crawler:  Everytime we crawl a file, we first check its content with BloomFilter, then parse it with Jsoup and get urls from it if it’s new one, and before we save new URLs into db or Frontier, we check with BloomFilter of URLSeens.
Distributor:  We define the state of Crawler as Consume or not, which will be triggered by database’s count exceeds the limit, and when the priority queue in Frontier has less than 0.15 of its maximum, it turns back to non-Consume state. Under normal state, the URLs got from URLs will be directly sent to Frontier.When under Consume state it will not add URLs to Frontier but load URLs in local database, and draw URLs from database, to maintain a relative small size of db.  Besides, the Distributor will send URLs if it belongs to other nodes. In our case, it will mode the hashcode of hosts to number of Nodes, and send them accordingly.
Robustness: Since we store the about 2000 next-to-crawl URLs in database, even if the Crawler is stopped, or crashed (We tested it with SIGKILL), it can build BloomFilterand Frontier from database on node.
Scalability: We have async distributors to send URLs in new threads, and Receiver to handle the requests from other nodes, we can simply update the IP information of new Nodes to add nodes into System.The distributing rule is similar to chord, if we want to add new node, we only need to stop and update the node within the target range, therefore, the other nodes will still transferring URLs to that node, which will serve as a master and distributes those URLs to newly added ones.

## 3.2 Indexer
The overall architecture of indexer can be described as the following figures.
![image](http://github.com/changan1995/turingSearch/raw/master/figure/figure2.png)
Map Reduce Framwork:  The goal of mapreduce framework is to compute the tf and idf value for each word in each document. The mapreduce takes in the html content from s3 by using the corresponding s3 Key. The content is parsed into indices by Jsoup. Meaningless words like are filter out by a stopword list and all the indices are stemmed. The overall process can be written as follows:
Map:
content=getContentFromS3(s3 key)
Wordlist = Split content by regex("([^a-zA-Z0-9]+)")
Filter out stop words and stem the rest words
For each word emit(word, url/001/len/tf) (<key,value>)
Reduce:
For each key(word), compute idf
emit(key, value+”/001”+idf.toString())
The tf and idf are computed as:.
	tf(i,j)=a+(1-a)*freq(i,j)/freq(l,j), 	idf(i)=log(N/n(i))
The freq(i,j) is the number of times that word i appears in document j. l refers to the word that appears most time in the document j. N refers to the total number of crawled files and n(i) refers to the number of crawled files that contains word i.
Stemmer:  We use snowball stemmer to stem words. The reason we choose this is that it is relatively light weight compared to StanfordCoreNLP and OpenNLP and offers desired stemming results for most words.
DynamoDB:  DynamoDB can be very efficient for query. But since it has limitation on the size of a single stored item, we can only store the crawled files on S3. For tf and idf table whose items are relative small, we choose to store them in DynamoDB to improve the efficiency of query. We first store them in S3 and then use hive script to transfer them into DynamoDB tables. The structure of tf and idf table are described as below：
```
Tf table <uuid, word, url, content_length,tf, pagerank>(word is the global secondary key)
Idf table <word,idf>
```
After trying several queries from the servlet, we find out that the pagerank query is the bottleneck of overall query efficiency, so we decide to join the pagerank table with tf table. Even though this takes a little bit more memory, the servlet only need to access the DynamoDB one time to get both tf and page rank value. This in the end makes the overall query process 10 times faster than before.

## 3.3 PageRank
We use Hadoop to implement PageRank, and our implementation includes following 5 steps:
    1. Extract all links from all crawled documents.
    2. Remove danglings (urls that are not crawled).
    3. Initialize the format that can be used for iterations.
    4. Calculate PageRank iteration by iteration, the stop criteria is either finishing the maximum number of iterations (40 iterations) or the average error (10^-7) is less than a specified threshold.
    5. Extract url and corresponding rank. And write the result from S3 to DynamoDB.

![image](http://github.com/changan1995/turingSearch/raw/master/figure/figure3.png)

Step1. Extract all links:  We stored all crawled documents in S3. To extract all links, we first used S3 API to list all files and their S3key to generate a file in which each line has the format of (url, S3key). 
Then we write a Hadoop MapReduce job to extract all links by parsing content and output (url, link1 0x02 link2 0x02 link3 0x02…).
Step2. Remove danglings: In the mapper, we write 3 different things to reducer, 1) each of its outlink as key and url as value, 2) url and a character ‘c’(to indicate the url was crawled), 3) a text ‘t’ and ‘’(to calculate total docs). In the reducer, if key is ‘t’, increment total and write the total to HDFS. If the values contains ‘c’, for each value write (value, key).
Step3. Initialize:  The input format is (url, outlink), so the mapper just write (url, outlink). In the reducer, we write (url, o:1 0x01 r:1 0x01 link1 0x01 link2 …) where o represent old value(initial 1) and r (initial 1) represent new value and use this encoding as the input and output for each iteration.
Step4. Iteration:  In the mapper, we first split string by \001, parse the double value of current rank(r: rank), write (url, r:rank) and (url, out:link1 0x01 link2 ...) to preserve the encoding. Then we calculate the number of outlinks(num), for each outlink(out) write (out , rank/num). In the reducer, we have a StringBuilder to construct the desired encoding. If the value starts with “r:”, append “o:rank” to StringBuilder. If the value can be parsed as Double, sum these values up as the new rank, append “r:new_rank” to StringBuilder. And if the value starts with “out:”, append this value to the end of StringBuilder. So the final output of reducer is (url, o:rank 0x01 r:new_rank 0x01 link1 0x01 link2 ...). To reduce hogs and sinks, we introduced a damping factor, which is 0.85.
After every 5 iterations, there is a coverage MapReduce job, which takes the output of last iteration, extract old rank and new rank, calculate the difference between ranks and sum all errors up to get to total error, write this error in HDFS. Since we already have the total number of docs in HDFS, we can calculate the average error, if the average error is smaller than the specified threshold (In our case,10^-7), exit iteration loop, otherwise keep running next iteration.
Step5. Extract the PageRank and write to DynamoDB:  Just get the rank(r:rank) and write (url, rank). Finally create external tables and transfer data from S3 to DynamoDB using hive.
We also tried Spark, the major steps are the same, except we didn’t check convergence, just ran 40 iterations. And it’s much easier to use Spark Dataframe join to remove danglings than Hadoop.
## 3.4 Search Engine & User Interface
Overall: We handled the user’s request by java servlet. When the servlet receives a request, according to the query, it will get the relevant web pages and scores from DynamoDB. By combining the TF/IDF, pagerank score and some other factors, it ranks all the results of the query and shows them on the results page. Also, google map and Yelp results are integrated by using the APIs. A simple google-style spell check is implemented by a spell check method.

Ranking:  Firstly, we used the snowball stemmer to normalize the query string, and get a list of words. For each word, we calculated the tf value for the query. Next, the normalized words were used as keys to find the corresponding idfs, urls of websites, tfs and pagerank scores. Basically, we used the function learnt in class to calculate the total score of each relevant websites. For example, if there is a query like “word1, word2”, the total score for a website ws will be (TF(word1)*IDF(word1)+TF(word2)*IDF(word2))*PageRank. By simply doing this, we found that the pagerank score played a leading role in the result, so we reduced the weight of pagerank score. Also, other factors were taken into consideration. For example, if one web page contains all the key words we want, we will give it extra scores. And if the key words we need are in the title of the web page, we’ll add extra scores to it. In addition, by searching a big amount of queries and analyzing the results, we made a blacklist which contained the low-quality websites we wanted to filter and a whitelist which contained the high-quality websites we wanted to show to the user, such as some official websites. All these factors listed above contributed to the final rankings of our results. 
User Interface:  The requests were handled by Java servlet, the webpages were written by JavaScript, HTML and CSS. Also, there is a button called cached, which can lead the user to the raw content we crawled.
Google Map(Extra):  We used Google GeoCoding APIs and frontend js to perform search result of address. Basically we used Gson for Json parsing and used our handwrite HTTPClient to send and receive the requests.
Yelp(Extra): First we sent a request to https://ipinfo.io/{clientIP}/json to get the location in longitude and latitude, then we used the API to get the results from Yelp. The results were returned as JSON. JSON parser was used to get the specific information we needed. The requests were sent by HttpsURLConnection.
Spell Check(Extra): We used SymSpell to build the dictionary of frequently used words. During spell checking we go through dics to find similar words, and measure them by different_distance / word_length, and return the minimum modification of the query words, with the redirection of it.


# 4 Evaluation
## 4.1 Crawler 
Scale of System:  The experiment is based on same seedPage, and also same thread number and other parameters.Obviously the efficiency grows proportionally to the nodes number, which is a good evident of our scalability. Since the transfer of the URLs won’t trash the crawling process.



Efficiency of threads number:  We tried out this on c5.2xlarge ec2 model, and we can see drop on the efficiency growth on 50 threads, since the ec2 has ECU for computation, we won’t try out more threads, but it’s obvious that more threads may leads to trashing. We essentially chose around 30 threads, which balanced efficiency against trashing.

## 4.2 Indexer
Performance:  The mapreduce job of indexer finishes in 6 hours with a 10 node EMR cluster given 1.5 million crawled page. There are in total 400 million extracted words. The hive script is executed on a 10 nodes EMR cluster and it takes 15 hours to transfer the data into DynamoDB table with writing capacity of 3000. The bottleneck is the writing capacity. We tried to improve the writing capacity to over 10 thousands and overall time cost drops dramatically.
## 4.3 PageRank
Analysis
The total number of documents that we crawled is 1504289. To calculate the PageRank, we used 8 EC2 nodes (m3x2large) for MapReduce, and it took 1 hour and 15 minutes to finish the job. We chose 40 as the maximum number of iterations and the threshold of average error is 0.0000001. The following figure shows how total error changes with more iterations. As we can see the total error decrease rapidly, but to make sure PageRank converge (every page changes very little), we use the average error to test convergence.
![image](http://github.com/changan1995/turingSearch/raw/master/figure/figure4.png)
For Spark, we used 3 EC2 nodes (m3xlarge) and finished the job in 2.5 hours. It’s hard to say which is faster since the number of nodes and the type of nodes are different. However, Hadoop used two times better nodes and about 2.7 times more nodes, and used half of time that Spark took, so roughly speaking, Spark is more efficient that Hadoop.
## 4.4 Search Engine & User Interface
Search time
After we received the request from the user, we processed the query, found the relevant values, calculated the total scores and ranked all the websites. By combining the tf values, urls of websites and pagerank scores to one table, the search time was shortened largely. For one word, the search time of ten thousands results is about 0.6s. 

# 5 Lessons Learned
As a whole, the project is a success. We implemented the basic functions of a search engine and added some extra features to it. But there are still some points that we can improve. For the crawler part, if we have enough time, we can crawl more web pages so that the search results can be more comprehensive. For the indexer, instead of using snowball, we can use some more functional stemmers so that not only a single word, but a phrase can be considered. Also, we will integrate the url title and sample body into the tf table so that we don’t need to parse the html in the front end and has more info for ranking. For the PageRank, in our project, we used urls to calculate the score, in fact, we can use domains to do this. It will be faster and more accurate. For the user interface, if given more time, we can integrate the search results of more web services, such as Amazon and Ebay,  to make our search engine more functional. 

# 6 Extra Credits
Add support for digests to detect when the same document has been retrieved more than once
Try Apache Spark as a basis of PageRank implementation, still use Hadoop results for final ranking.
Integrate search results from web services(Google API, Yelp)
Google-style spell check
The details have been described above.
