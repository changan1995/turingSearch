package edu.upenn.cis555.searchengine.crawler.logger;


import edu.upenn.cis555.searchengine.crawler.storage.DBWrapper;
import edu.upenn.cis555.searchengine.crawler.storage.DocDB;

public class BDBPrint{
    public static void main(String args[]){
        DBWrapper dbWrapper = new DBWrapper(args[0]);
        DocDB docDB =dbWrapper.getDocDB();
        System.out.println(docDB.getNum());
    }
}