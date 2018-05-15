package cs3.cs2.cs.searchengine.crawler.storage;




public class DBdelete{


    public static void main(String[] args){
        int index = Integer.parseInt(args[0]);
        DBWrapper.envDirectory += "" + index;
        DBWrapper db = DBWrapper.getInstance();
        db.setUp();
        db.closeFrontier();
        db.removeDB("frontier");
        System.out.println("successfulremoved");
    }
 
}