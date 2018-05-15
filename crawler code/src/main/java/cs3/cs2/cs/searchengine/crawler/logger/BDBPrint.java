//package cs3.cs2.cs.searchengine.crawler.logger;
//
//
//import java.util.List;
//
//import cs3.cs2.cs.searchengine.crawler.storage.DBWrapper;
//import cs3.cs2.cs.searchengine.crawler.storage.Doc;
//import cs3.cs2.cs.searchengine.crawler.storage.DocDB;
//
//public class BDBPrint{
//    public static void main(String args[]){
//        DBWrapper dbWrapper = new DBWrapper(args[0]);
//        DocDB docDB =dbWrapper.getDocDB();
//        System.out.println(docDB.getNum());
//        List<Doc> docLst =  docDB.getallDocs();
//        for(Doc doc : docLst){
//            doc.getUrl();       //url
//            doc.getContent();   //content string
//        }
//    }
//}