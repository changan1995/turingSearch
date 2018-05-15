package edu.upenn.turingSearch.searchengine.crawler.info;

public class URLInfo {
	private String hostName;
	private int portNo;
    private String filePath;
    private String absoluteRoot;
    private String protocals;
    private boolean portFlag= false;
    private String url = null;
	
	/**
	 * Constructor called with raw URL as input - parses URL to obtain host name and file path
	 */
	public URLInfo(String docURL){
        this.url = docURL;
		if(docURL == null || docURL.equals(""))
			return;
		docURL = docURL.trim();
		//change it in to accept www.  and https @changanw
		if(docURL.length() < 8)
			return;
		// Stripping off 'http://'
		if(docURL.startsWith("http://")){
            protocals="http://";
			docURL = docURL.substring(7);
		// If starting with 'www.' , stripping that off too
		}else if(docURL.startsWith("www.")){
            protocals="http://www.";
			docURL = docURL.substring(4);
		}else if(docURL.startsWith("https://")){
            protocals="https://";
			docURL = docURL.substring(8);
		}else{
			return;
		}
		int i = 0;
		while(i < docURL.length()){
			char c = docURL.charAt(i);
			if(c == '/')
				break;
			i++;
		}
		String address = docURL.substring(0,i);
		if(i == docURL.length())
			filePath = "/";
		else
			filePath = docURL.substring(i); //starts with '/'
		if(address.equals("/") || address.equals(""))
			return;
		if(address.indexOf(':') != -1){
            portFlag = true;
			String[] comp = address.split(":",2);
			hostName = comp[0].trim();
			try{
				portNo = Integer.parseInt(comp[1].trim());
			}catch(NumberFormatException nfe){
				portNo = 80;
			}
		}else{
			hostName = address;
			portNo = 80;
		}
	}
	
	public URLInfo(String hostName, String filePath){
		this.hostName = hostName;
		this.filePath = filePath;
		this.portNo = 80;
	}
	
	public URLInfo(String hostName,int portNo,String filePath){
		this.hostName = hostName;
		this.portNo = portNo;
		this.filePath = filePath;
	}
    
    public String getUrlNoPort(){
        return protocals+hostName+filePath;
    }

	public String getHostName(){
		return hostName;
	}
	
	public void setHostName(String s){
		hostName = s;
	}

	public int getPortNo(){
		return portNo;
	}
	
	public void setPortNo(int p){
		portNo = p;
	}
	
	public String getFilePath(){
		return filePath;
	}
	
	public void setFilePath(String fp){
		filePath = fp;
	}
	
	
	public static void main(String[] args) {
		URLInfo ui = new URLInfo("http://crawltest.cis.upenn.edu/misc");
        System.out.println(ui.getHostName());//baidu.com
		System.out.println(ui.getUrlNoPort());//80
		System.out.println(ui.getFilePath());// /cat.jpeg
		System.out.println(ui.getAbsoluteRoot());//https://www.baidu.com

	}

	/**
	 * @return the absoluteRoot
	 */
	public String getAbsoluteRoot() {
		return protocals+hostName;
	}

	/**
	 * @param absoluteRoot the absoluteRoot to set
	 */
	public void setAbsoluteRoot(String absoluteRoot) {
		this.absoluteRoot = absoluteRoot;
	}
}
