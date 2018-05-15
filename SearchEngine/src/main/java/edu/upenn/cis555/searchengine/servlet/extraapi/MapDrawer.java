package edu.upenn.cis555.searchengine.servlet.extraapi;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

public class MapDrawer {
    private String urlString = null;
    private URL url = null;
    private double lat = 0;
    private double lng = 0;
    private String formattedAddress = null;
    private HttpsURLConnection conn;

    public MapDrawer(String address) throws Throwable {
        urlString = "https://maps.googleapis.com/maps/api/geocode/json?key=AIzaSyCCYax-OSS7hnQ-oiBbjHbeZ9O2DP9ArpI&language=en";
        urlString += "&address=" + address;
        url = new URL(urlString);
        conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
    }

    public boolean getValue() throws Throwable {
        if (conn.getResponseCode() != 200) {
            return false;
        }
        Reader reader = new InputStreamReader(conn.getInputStream());
        JsonReader jsonReader = new JsonReader(reader);
        MapJson json = new Gson().fromJson(jsonReader, MapJson.class);
        this.lat = Double.parseDouble(json.getLat());
        this.lng = Double.parseDouble(json.getLng());
        this.formattedAddress = json.getFormattedAddress();
        return true;
    }

    public void printScript(PrintWriter out){
        out.write(
            "<div id=\"map\"></div>"+
            "<script>"+
            " function initMap() {"+
            "   var uluru = {lng:"+this.getLng()+", lat: "+this.getLat()+"};"+
             "   var map = new google.maps.Map(document.getElementById(\"map\"), {"+
             "    zoom: 12,"+
             "    center: uluru"+
             "  });"+
             "  var marker = new google.maps.Marker({"+
             "    position: uluru,"+
             "    map: map"+
             "  });"+
             "}"+
             "</script>"+
             "<script async defer src=\"https://maps.googleapis.com/maps/api/js?key=AIzaSyCCYax-OSS7hnQ-oiBbjHbeZ9O2DP9ArpI&callback=initMap&language=en\">"+
             "</script>"
        );

    }

    public String getFormattedAddress() {
        return this.formattedAddress;
    }

    public Double getLat() {
        return this.lat;
    }

    public Double getLng() {
        return this.lng;
    }

    public static void main(String args[]) {
        MapDrawer map;
        try {
            map = new MapDrawer("apple");
            if (!map.getValue()) {
                System.err.println("no address");
            }
            System.out.println(map.getFormattedAddress());
            System.out.println(map.getLat() + "\t" + map.getLng());
        } catch (Throwable e) {
            System.err.println("no address");
        }

    }
}