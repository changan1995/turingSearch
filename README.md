# ![Turing Search](https://github.com/changan1995/turingSearch/raw/master/SearchEngine/conf/title1.jpg?raw=true)

TuringSearch is based on traditional searching & crawler structure. The distributed crawler established on AWS ec2s,with high efficiency and scalability. The Pagerank and Indexer is implemented to support a query style search from the user-interface. 
Essentially, 1.5 million websites are crawled in one day, concerning a wide spread of topics, which has already provided both accuracy and efficiency in searching.
The whole system is capable of, as it is designed for, processing thousands times larger scale in releatively short time.

## author

[Quankang Wang](mailto:changanw@seas.upenn.edu) Crawler, Ranking algorithm, Map Searching.

[Mojiang Jia](mailtomojjia@seas.upenn.edu) PageRank, Ranking.

[Yi Guo](mailto:guoyi1@seas.upenn.edu) Indexer.

[Yitong Long](mailto:yitongl@seas.upenn.edu) User Interface, Yelp support.

# Introduction

##  Architecture

Our Turing Search Engine consists of 4 main components, Crawler, Indexer, PageRank and User Interface. The approaches we used are shown below.

### Crawler: 

The Crawler used Chord like distributing system, with high efficiency design and great scalability. Select the URLs by domain hashvalue, and implement high performance and polite crawlering derived from the [paper of Allan Heydon and Marc Najork](https://doi.org/10.1023/A:1019213109274) 

![crawler structure](https://github.com/changan1995/turingSearch/raw/master/figure/figure1.png?raw=true)

The crawling efficiency grows exponentially with the number of nodes of the distributed system,meanwhile linearly with the computing power of single nodes.

![crawler efficiency](https://github.com/changan1995/turingSearch/raw/master/figure/figure3.png?raw=true)

There are multiple techniques implemented in preventing hogs and sinks, as well as malicious sites, including content duplicate detection, malicious host detection, trash preventing techniques, etc.


### Indexer: 

MapReduce was used to calculate the value of tf and idf. We used EMR for map reduce process and stored the tables in DynamoDB for query. For keyword stemming, we chose to use snowball,a lightweight pure-algo open source stemmer.
![indexer data structure](https://github.com/changan1995/turingSearch/raw/master/figure/figure2.png?raw=true)


### PageRank Engine: 

Given the crawled information, we used Hadoop MapReduce to implement a iterative PageRank algorithm, designed a data encoding to serve the output of previous iteration as input to the next. We used Random Surfer Model by adding a decay factor prevent “sinks” and “hogs”. Web graphs in [Stanford Large Network Dataset Collection](https://snap.stanford.edu/data/) was used  to test correctness.

TODO: implementing perlocating pagerank.

### User interface: 

The user interface is implemented by java servlet. The search interface was written by JavaScript, HTML and CSS. As to the result interface, we ranked the results by combining TF/IDF values and PageRank scores. The search results from Google map and Yelp were integrated by using the APIs.
