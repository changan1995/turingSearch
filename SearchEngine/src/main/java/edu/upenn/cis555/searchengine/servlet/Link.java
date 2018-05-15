package edu.upenn.cis555.searchengine.servlet;

public class Link {
	
	private String url="";
	private double tfidf=0.0;
	private double pagerank=0.0;
	public double titleScore=0.0;
	public double totalScore=0.0;
	public String title="";
	public String SampleContent="";
	public int keywordNum;
	public int hasKeyword = 0;
	private double squareRt =0;
	
	public Link(String url, double tfidf, double pagerank, int keywordNum){
		this.url=url;
		this.tfidf=tfidf;//*(1 + hasKeyword/keywordNum);
//		if(pagerank>20)pagerank=20;
		this.pagerank=pagerank>2?1+Math.log(pagerank)/Math.log(2):pagerank;
		this.keywordNum=keywordNum;
		this.hasKeyword=1;
	}
	public double computeTotalScore(double sumTFIDF,double sumPgRank){
		// pagerank=pagerank>2?1+Math.log(pagerank)/Math.log(2):pagerank;
//		double p=pagerank>2?Math.log(pagerank)/Math.log(2):pagerank;
//		if (url.contains("en.wikipedia")) p += 10;
		// this.tfidf =tfidf*(1 + hasKeyword/keywordNum);
		// this.pagerank= pagerank;
		totalScore=1.5*pagerank/sumPgRank+tfidf/sumTFIDF*(1+this.hasKeyword/this.keywordNum);
		totalScore/=Math.log(url.length());
		return totalScore;
	}

	public void setTfidf(double tfidf){
		this.tfidf=tfidf;
	}

	public void addTfidf(double addition){
		this.tfidf+=addition;
	}
	
	public Link(String url, double tfidf, double pagerank, double titleScore,String title, String SampleContent, double totalScore){
		this.url=url;
		this.tfidf=tfidf;
		this.pagerank=pagerank;
		this.titleScore=titleScore;
		this.totalScore=totalScore;
		this.title=title;
		this.SampleContent=SampleContent;
	}
	
	public double getTfidf(){
		return this.tfidf;
	}
	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}
	/**
	 * @param url the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	public double getPageRank(){
		return this.pagerank;
	}

}
