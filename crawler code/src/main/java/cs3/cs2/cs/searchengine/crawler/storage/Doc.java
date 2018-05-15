package cs3.cs2.cs.searchengine.crawler.storage;


// import org.w3c.dom.Document;

import cs3.cs2.cs.searchengine.crawler.Utilities;
import cs3.cs2.cs.searchengine.crawler.info.URLInfo;
// import cs3.cs2.turingSearch.hw1.HttpServer;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

@Entity
public class Doc {
	
	@PrimaryKey
	private String url;
		 
	private String content;
	private String docType;
	private long docLength;
    private String crawledDate;
    private String absHost;
    private String lastModified;
	
    public Doc() {}
    
	public Doc(String url, String content, String docType, long docLength, String lastModified) {
        URLInfo temp = new URLInfo(url);
        this.absHost = temp.getAbsoluteRoot();
		this.url = url; 
        this.content = content;
		this.docType = docType;
		this.docLength = docLength;
		this.crawledDate = Utilities.convertTime(System.currentTimeMillis());
        if(lastModified==null){
            this.lastModified=Utilities.convertTime(0);
        }else{
            this.lastModified = lastModified;
        }
	}
	
	public String getUrl() { return this.url; }
	public String getDocType() { return this.docType; }
	public long getDocLength() { return this.docLength; }
    public String getcrawledDate() { return this.crawledDate; }
    public String getLastModified() {return this.lastModified;}
	
	public void setUrl(String url) {
		this.url = url;
    }
    
	public void setDocument(String content, String docType) {
		this.content = content;
		this.docType = docType;
	}
    
    public String getContent(){
        return this.content;
    }

	public void setcrawledDate() {
		this.crawledDate = Utilities.convertTime(System.currentTimeMillis());
	}

	/**
	 * @return the absHost
	 */
	public String getAbsHost() {
		return absHost;
	}

	/**
	 * @param absHost the absHost to set
	 */
	public void setAbsHost(String absHost) {
		this.absHost = absHost;
	}
	
}
