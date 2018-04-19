package edu.upenn.cis455.storage;

import java.util.ArrayList;


import com.sleepycat.je.Transaction;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
//todo synchronized??
public class DocDB {
	
	private PrimaryIndex<String,Doc> docByUrl;

	public DocDB(EntityStore store) {
		docByUrl = store.getPrimaryIndex(String.class, Doc.class);
	}
	
	public Doc get(String url) {
		return docByUrl.get(url);
	}
	
	public ArrayList<Doc> getallDocs() {
		ArrayList<Doc> docs;
		EntityCursor<Doc> iterDocs = docByUrl.entities();
		try {
			docs = new ArrayList<Doc>();
			for (Doc d : iterDocs) {
				docs.add(d);
			}
		} finally {
			iterDocs.close();
		}
		return docs;
	}
	
	public boolean insertDoc(Doc doc, Transaction txn) {
		return docByUrl.putNoOverwrite(txn, doc);
    }
    
    public long getNum(){
        return docByUrl.count();
    }
	
	public void updateDoc(Doc doc, Transaction txn) {
		doc.setcrawledDate();
		docByUrl.put(txn, doc);
    }
    
    public boolean put(Doc doc,Transaction txn){//overall put
        try{
            if(this.get(doc.getUrl())!=null){//update flag
                doc.setcrawledDate();
                docByUrl.put(txn,doc);
                return true;
            }else{
                docByUrl.putNoOverwrite(txn, doc);
                return true;
            }
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }
}
