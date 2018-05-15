package edu.upenn.cis555.searchengine.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Yelp {

	public String location;
	public String lat;
	public String lon;

	public Yelp(String location, String addre) {
		this.location = location;
		try {
//			URL ipapi = new URL("https://ipinfo.io/"+"68.81.73.39"+"/json");
			URL ipapi = new URL("https://ipinfo.io/"+addre+"/json");
			HttpsURLConnection conn = (HttpsURLConnection)ipapi.openConnection();
	        conn.setRequestProperty("User-Agent", "java-ipinfo-client");
	        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	        String pos;
	        while ((pos = reader.readLine()) != null) {
	        	if (pos.contains("loc")) {
	        		String[] pair = pos.trim().replace("\"", "").split("\"|,|:");
	        		lat = pair[1].trim();
	        		lon = pair[2].trim();
	        	}
	         System.out.println(pos);
	        }
	        reader.close();
			} catch (Exception e) {
			}
	}

	public List<Map<String, String>> getFoods() {
		List<Map<String, String>> foods = new ArrayList<Map<String, String>>();
		try {
			String urlString = "https://api.yelp.com/v3/businesses/search?";
			if (lat != null) {
				urlString += "latitude=" + lat + "&";
				urlString += "longitude=" + lon + "&";
				urlString += "term=" + this.location;
			}
			else {
				urlString += "location=" + this.location;
			}
			System.out.println(urlString);
			URL yelpUrl = new URL(urlString);
			HttpsURLConnection connection = (HttpsURLConnection) yelpUrl
					.openConnection();
			connection.setRequestMethod("GET");
			connection
					.addRequestProperty(
							"Authorization",
							"Bearer nczIC6sGH0kpdGnvh1gnIjknrPzWoe4r1hT0MGbkJGizzz9ULKrL_BKFf-zQ8FmvzjMqfexIWLXKgcQSGF3f-a_Phr3J6LhEupZLjF1XJ4WQsfJvjEcHPFExvj7rWnYx");
			System.out.println(connection.getResponseCode());
			if (connection.getResponseCode() == 200) {
				BufferedReader br = new BufferedReader(new InputStreamReader(
						connection.getInputStream()));
				JSONParser parser = new JSONParser();
				try {
					JSONObject all = (JSONObject) parser.parse(br);
					/*int total = (int)(long)all.get("total");
					if (total > 50) {
						total = 50;
					}*/
					JSONArray rests = (JSONArray) all.get("businesses");
					int total=rests.size();
					if (total>50){
						total=50;
					}
					int n = 0;
					while (n < total) {
						Map<String, String> one = new HashMap<String, String>();
						JSONObject restaus = (JSONObject) rests.toArray()[n];
						String name = (String) restaus.get("name");
						String rating = Double.toString((double) restaus
								.get("rating"));
						String price = (String) restaus.get("price");
						String url = (String) restaus.get("url");
						String image_url = (String) restaus.get("image_url");
						JSONObject addr = (JSONObject) restaus.get("location");
						JSONArray addre = (JSONArray) addr
								.get("display_address");
						int in = 0;
						String address = "";
						while (in < addre.size()) {
							address = address + " " + addre.toArray()[in];
							in = in + 1;
						}
						one.put("name", name);
						one.put("rating", rating);
						one.put("price", price);
						one.put("url", url);
						one.put("image_url", image_url);
						one.put("address", address);
						foods.add(one);
						n = n + 1;
					}
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else {
				System.out
						.println("We can't connect to Yelp! Please check the network!");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return foods;
	}
	
	/*public static void main(String[] args){
		Yelp yelp=new Yelp("newyork");
		System.out.println(yelp.getFoods().size());
	}*/
}
