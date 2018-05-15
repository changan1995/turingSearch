package edu.upenn.cis555.searchengine.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;
import edu.upenn.cis555.searchengine.servlet.extraapi.MapDrawer;
import edu.upenn.cis555.searchengine.global.*;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.google.common.hash.BloomFilter;

import com.google.common.hash.Funnels;

import edu.upenn.cis555.searchengine.jettyserver.MinimalJettyServer;

@SuppressWarnings("serial")
public class SearchServlet extends HttpServlet {

	int onePage = 10;
	public Map<String, List<Link>> alllinks = new LinkedHashMap<String, List<Link>>(10, (float) 0.75, true) {
		protected boolean removeEldestEntry(java.util.Map.Entry<String, List<Link>> eldest) {
			return size() > 5;
		}
	};
	public boolean debug = true;
	public Cache cache = new Cache();
	// private HashSet<String> titleSet =null;
	private BloomFilter<CharSequence> bl;
	private Pattern titleBlackLst = Pattern.compile(".*(redirect|moved|login|sitemap|justia|image|jpg|bandcamp|png|neatoShop|climatemps|foundation|justia|networkadvertising).*");
	private Pattern urlBlackLst = Pattern.compile(".*(climatemps|wikimediafoundation|sitemap|blogspot|justia|bandcamp|neatoshop|bandcamp|help).*");
	public long startTime = 0;
	private String[] correctLst = null;
	private boolean correct = false;
	// private HashSet<String>
	private Pattern blackLst = Pattern.compile(".*(sign|Sign|Redirect|login|Log in).*");

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String map=request.getParameter("map");
		String restaurant=request.getParameter("restaurant");
		double sumPgRank=0;
		double sumTFIDF=0;
		if (alllinks.size() > 20) {
			alllinks.clear();
		}
		List<Link> thisLinks = new ArrayList<Link>();
		startTime = System.nanoTime();
		correct=false;
		correctLst = null;
		SnowballStemmer stemmer = new englishStemmer();
		String oriQuery = request.getParameter("query");
		String[] wordList = oriQuery.split("([^a-zA-Z0-9]+)");
		String query = "";
		HashMap<String, Double> query_tfs = new HashMap<String, Double>();
		Stem.StemAndComputeTf(wordList, query_tfs);
		int i = 0;
		correctLst = new String[wordList.length];
		for (i = 0; i < wordList.length; i++) {
			query = query + wordList[i];
			String temp = SymSpell.Correct(wordList[i], "");
			if (temp != null && temp.equals(wordList[i])==false) {
				correctLst[i] = temp;
				correct = true;
			}else{
				correctLst[i]=wordList[i];
			}
		}
		
		if (alllinks.containsKey(query)) {
			response.sendRedirect("/search?query=" + query + "&page=1"+"&map="+map+"&restaurant="+restaurant);
			return;
		}

		Map<String, Link> urls = new HashMap<String, Link>();
		for (String word : query_tfs.keySet()) {
			System.out.println(word);
			double query_tf = query_tfs.get(word);
			IdfThread idfThread = new IdfThread(word);
			TfThread tfThread = new TfThread(word);
			idfThread.start();
			tfThread.start();
			try {
				idfThread.join();
				tfThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			long idfTime = System.nanoTime();
			double time1 = (idfTime - startTime) / 1000000000.0;
			System.out.println("idf:" + idfThread.idf + " " + time1 + "s");

			double idf = idfThread.idf;
			// if (idfThread.idf != 0) {
			// Map<String, Double> tfs = QueryDBMapper.FindTf(
			// MinimalJettyServer.mapper, normalizedWord);//
			// key:url;value:tf(word-url)
			for (QueryDBMapper.Indexer_tf_new indexer_tf : tfThread.scores) {
				String url = indexer_tf.getUrlString();
				// blacklist website
				if (urlBlackLst.matcher(url).matches()) continue;
//				if (url.contains("neatoshop")) continue;
//				if (url.contains("climatemps")) continue;
//				if (url.contains("blogspot")) continue;
//				if (url.contains("archive.org")) continue;
//				if (url.contains("soccerway")) continue;
				double tf = indexer_tf.getTf();
				int len = indexer_tf.getLen();
				double tfidfScore = MagicFunction.getMagicTf(tf, query_tf, idf,
						len);
				if (urls.containsKey(url)) {
					Link l = urls.get(url);
					if (url.contains(word)) tfidfScore *= 1.5;
					tfidfScore *= 10;
					l.hasKeyword++;
					l.addTfidf(tfidfScore);
					sumTFIDF+=tfidfScore;
					urls.put(url, l);
				} else {
					if (url.contains(word)) tfidfScore *= 1.5;
					tfidfScore *= 10;
//					if (url.contains("en.wikipedia"))
					Link l = new Link(url, tfidfScore, indexer_tf.getRank(), query_tfs.size());
					sumTFIDF+=tfidfScore;
					sumPgRank+=l.getPageRank();
					urls.put(url, l);
					thisLinks.add(l);
				}
			}
			long combineTime = System.nanoTime();
			double time3 = (combineTime - startTime) / 1000000000.0;
			System.out.println("All score finished " + time3 + "s");
			// }

		}
		for (Link l : thisLinks) {
			l.computeTotalScore(sumTFIDF,sumPgRank);
			
		}
		thisLinks.sort(new Compare());
		long sortTime = System.nanoTime();
		double time5 = (sortTime - startTime) / 1000000000.0;
		System.out.println("sortLink is done " + time5 + "s");
		alllinks.put(query, thisLinks);
		response.sendRedirect("/search?query=" + query + "&page=1"+"&map="+map+"&restaurant="+restaurant);
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		// titleSet = new HashSet<String>();
		bl = BloomFilter.create(Funnels.stringFunnel(), 2000000);		
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		if (request.getParameter("correct")!=null){
			try {
				doPost(request, response);
				return;
			} catch (ServletException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (request.getParameter("query") != null) {
			String query = request.getParameter("query");
			List<Link> thisLinks = alllinks.get(query);
			int pages = thisLinks.size() / onePage;
			if (thisLinks.size() % onePage != 0) {
				pages = pages + 1;
			}
			out.println("<html><head>");
			out.println("<style>#map{position:relative;top:70px;left:183px;height:40%;width:40%}</style>");
			out.println("<link rel='stylesheet' type='text/css' href='/conf/stylesheet1.css'/>");
			out.println("<link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css'>");
			out.println("</head>");
			out.println("<body>");
			out.println("<div>");
			out.println("<img class='logo' src='/conf/title1.jpg'>");
			out.println("<form class='oneline1' action='search' method='post'>"
					+ "<input id='borderimg1' type='text' name='query'>"
					+"<input id='showmap' type='checkbox' name='map'><h id='maptext'>Show Map</h><br>"
					+"<input id='restaurant' type='checkbox' name='restaurant'><h id='foodtext'>Find Restaurants</h><br>"
					+ "<input id='search' type='image' src='/conf/search2.jpg'></form>");
			out.println("</div>");
			if (correct==true){
				out.println("<h id='correct'>Do you want to search <a href='/search?correct=true&query=");
				for (String correctWord: correctLst){
					out.println(correctWord+"*");
				}
				out.println("'>");
				for (String correctWord: correctLst){
					out.println(correctWord+" ");
				}
				out.println("</a>?</h>");
			}
			if (thisLinks.size() == 0) {
				out.println("<h id='noresults1'>Your search did not match any documents.</h>");
				out.println("<h id='noresults2'>Please check the spell or try different keywords.</h>");
			} else {
				long endTime = System.nanoTime();
				double totalTime = (endTime - startTime) / 1000000000.0;
				if(Integer.parseInt(request.getParameter("page"))==1){
					out.println("<h id='totalresults'>About " + thisLinks.size()
							+ " results ("+totalTime+" seconds)</h>");
					String rest = request.getParameter("restaurant");
					if (rest != null && rest.equals("on")){
						Yelp yelp=new Yelp(query, request.getRemoteAddr());
						out.println("<div id='yelp'>");
						for (Map<String,String> food: yelp.getFoods()){
							out.println("<img src="+food.get("image_url")+" alt='a' width='300' height='200'>");
							out.println("<p><a href='"+food.get("url")+"'>"+food.get("name")+"</a></p>");
							out.println("<p>Rating: "+food.get("rating")+" Price: "+food.get("price")+"</p>");
							out.println("<h>"+food.get("address")+"</h>");
						}
						out.println("</div>");
					}
					String mapString = request.getParameter("map");
					if (mapString != null && mapString.equals("on")){
						//map draw
						MapDrawer map=null;
						try {
							map = new MapDrawer(query);
							if(map.getValue()){
								map.printScript(out);
							}
						} catch (Throwable e1) {
							e1.printStackTrace();
						}
						//map end
					}
				}else{
					out.println("<h id='totalresults'>About " + thisLinks.size()
							+ " results </h>");
				}

				Document doc;
				int i = 1;// order number of a link in one page
				int page = Integer.parseInt(request.getParameter("page"));// the
																			// page
																			// number
				int firstlink = (page - 1) * onePage;
				if (page == pages) {
					out.println("<div id='whole'>");
					while (firstlink < thisLinks.size()) {
						Link current_link = null;
						if(firstlink<thisLinks.size()){
							current_link = thisLinks.get(firstlink);								
						}else{
							break;
						}
						// if(current_link!=null){
						// 	break;
						// }
						String html = current_link.getUrl();
						try {
							String encode = URLEncoder.encode(html, "UTF-8");
							doc = Jsoup.parse(cache.getContent(encode));
							if(bl.mightContain(doc.title())|| doc.title().equals("")){
								thisLinks.remove(firstlink);								
								continue;
							}else{
								bl.put(doc.title());
							}
							if(blackLst.matcher(doc.title().toLowerCase()).matches()){
								thisLinks.remove(firstlink);								
								continue;
							}
							if (titleBlackLst.matcher(doc.title().toLowerCase()).matches()) {
								thisLinks.remove(firstlink);								
								continue;
							}
							String s = doc.body().text();
							if (s.contains("?????") | doc.title().contains("??")) {
								thisLinks.remove(firstlink);								
								continue;
							}
							System.out.println(html + ":" + current_link.hasKeyword);
							out.println("<h id='title'><a href=" + html + ">"
									+ doc.title() + "</a></h><br>");
							out.println("<h id='link'>"
									+ html
									+ " "
									+ "<div class='dropdown'>"
									+ "<button class='btn'><i class='fa fa-caret-down'></i></button>"
									+ "<div class='dropdown-content'><a href='/search?cached="
									+ encode + "'>Cached</a></div></div></h>");
							out.println("<p id='text'>");
							if (s.length() > 350) {
								s = s.substring(0, 350) + "...";
							}
							while (s.startsWith(" ")) {
								s = s.substring(1);
							}
							out.println(s);
							System.out.println(current_link.getUrl()+current_link.hasKeyword);							
							if (debug == true) {
								out.println("tfidf:"
										+ thisLinks.get(firstlink).getTfidf());
								out.println("pagerank:"
										+ thisLinks.get(firstlink).getPageRank()
										+ " ");
								out.println("totalscore:"
										+ thisLinks.get(firstlink).totalScore
										+ " ");
							}
							out.println("</p>");
							out.println("<br>");
							firstlink = firstlink + 1;
						} catch (Exception e) {
							firstlink = firstlink + 1;
						}

					}
					String previous = Integer.toString(Integer.parseInt(request
							.getParameter("page")) - 1);
					if (pages != 1) {
						out.println("<a href=/search?query="
								+ request.getParameter("query") + "&page="
								+ previous + ">Previous</a>");
					}
					int n = (page / 10) * 10 + 1;
					while (n < page) {
						out.println("<a href=/search?query="
								+ request.getParameter("query") + "&page=" + n
								+ ">" + n + "</a>");
						n = n + 1;
					}
					out.println(n);
					out.println("</div>");
				} else {
					out.println("<div id='whole'>");
					while (i <= onePage) {
						Link current_link =null;
						// try{
							if(firstlink<thisLinks.size()){
								current_link = thisLinks.get(firstlink);								
							}else{
								break;
							}
						// 	if(current_link!=null){
						// 		break;
						// 	}
						// }catch(Exception e){
						// 	out.println("<h id='noresults1'>Your search did not match any documents.</h>");
						// 	out.println("<h id='noresults2'>Please check the spell or try different keywords.</h>");
						// 	return;
						// }

						String html = current_link.getUrl();
						try {
							String encode = URLEncoder.encode(html, "UTF-8");
							doc = Jsoup.parse(cache.getContent(encode));
							if(bl.mightContain(doc.title()) || doc.title().equals("")){
								thisLinks.remove(firstlink);								
								continue;
							}else{
								bl.put(doc.title());
							}
							if(blackLst.matcher(doc.title().toLowerCase()).matches()){
								thisLinks.remove(firstlink);								
								continue;
							}
							if (titleBlackLst.matcher(doc.title().toLowerCase()).matches()) {
								thisLinks.remove(firstlink);								
								continue;
							}
							String s = doc.body().text();
							if (s.contains("?????") | doc.title().contains("??")) {
								thisLinks.remove(firstlink);								
								continue;
							}
							System.out.println(html + ":" + current_link.hasKeyword);
							out.println("<h id='title'><a href=" + html + ">"
									+ doc.title() + "</a></h><br>");
							out.println("<h id='link'>"
									+ html
									+ " "
									+ "<div class='dropdown'>"
									+ "<button class='btn'><i class='fa fa-caret-down'></i></button>"
									+ "<div class='dropdown-content'><a href='/search?cached="
									+ encode + "'>Cached</a></div></div></h>");
							out.println("<p id='text'>");
							
							if (s.length() > 350) {
								s = s.substring(0, 350) + "...";
							}
							while (s.startsWith(" ")) {
								s = s.substring(1);
							}
							out.println(s);
							if (debug == true) {
								out.println("tfidf:"
										+ thisLinks.get(firstlink).getTfidf() + " ");
								out.println("pagerank:"
										+ thisLinks.get(firstlink).getPageRank()
										+ " ");
								out.println("totalscore:"
										+ thisLinks.get(firstlink).totalScore
										+ " ");
							}
							out.println("</p>");
							out.println("<br>");
							i = i + 1;
							firstlink = firstlink + 1;
						} catch (Exception e) {
							i = i + 1;
							firstlink = firstlink + 1;
						}
					}
					if (page == 1) {
						out.println("1");
						int n = 2;
						int lastpage = 10;
						if (pages < 10) {
							lastpage = pages;
						}
						while (n <= lastpage) {
							out.println("<a href=/search?query="
									+ request.getParameter("query") + "&page="
									+ n + ">" + n + "</a>");
							n = n + 1;
						}
						out.println("<a href=/search?query="
								+ request.getParameter("query") + "&page=" + 2
								+ ">Next</a>");
						out.println("</div>");
					} else {
						String previous = Integer.toString(Integer
								.parseInt(request.getParameter("page")) - 1);
						String next = Integer.toString(Integer.parseInt(request
								.getParameter("page")) + 1);
						out.println("<a href=/search?query="
								+ request.getParameter("query") + "&page="
								+ previous + ">Previous</a>");
						int n = (page / 10) * 10 + 1;
						int lastpage = (page / 10 + 1) * 10;
						if (pages < lastpage) {
							lastpage = pages;
						}
						while (n <= lastpage) {
							if (n == page) {
								out.println(n);
							} else {
								out.println("<a href=/search?query="
										+ request.getParameter("query")
										+ "&page=" + n + ">" + n + "</a>");
							}
							n = n + 1;
						}
						out.println("<a href=/search?query="
								+ request.getParameter("query") + "&page="
								+ next + ">Next</a>");
					}
				}
				out.println("</body></html>");
			}
		} else {
			if (request.getParameter("cached") != null) {
				String url = request.getParameter("cached");
				String encode = URLEncoder.encode(url, "UTF-8");
				out.println(cache.getContent(encode));
			} else {
				out.println("<html><head>");
				out.println("<link rel='stylesheet' type='text/css' href='/conf/stylesheet.css'/>");
				out.println("</head>");
				out.println("<body>");
				out.println("<img src='/conf/title1.jpg'>");
				out.println("<form class='center2' action='search' method='post'>"
						+ "<input id='borderimg1' type='text' name='query'><br>"
						+ "<input id='search' type='image' src='/conf/search2.jpg'></form>");
				out.println("</body></html>");

			}
		}

	}
}
