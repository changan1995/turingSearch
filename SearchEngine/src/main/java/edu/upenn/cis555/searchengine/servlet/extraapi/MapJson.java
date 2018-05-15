package cs3.cs2.cs.searchengine.servlet.extraapi;

import java.util.List;
import java.util.Map;

public class MapJson{
    Result[] results;
    String status;

    public String getLat(){
        if(!this.status.equals("OK"))
        return null;
        return this.results[0].geometry.location.lat;
    }


    public String getLng(){
        if(!this.status.equals("OK"))
        return null;
        return this.results[0].geometry.location.lng;
    }

    public String getFormattedAddress(){
        if(!this.status.equals("OK"))
            return null;
        return this.results[0].formatted_address;
    }

    public class Result{
        // Address_components[] address_components;
        String formatted_address;
        Geometry geometry;
    }

    // public class Address_components{
    //     String[] types;
    //     String long_name;
    //     String short_name;        
    // }

    public class Geometry{
        Location location;
    }

    public class Location{
        String lat;
        String lng;
    }
}





// {
//     "results" : [
//        {
//           "address_components" : [
//              {
//                 "long_name" : "1600",
//                 "short_name" : "1600",
//                 "types" : [ "street_number" ]
//              },
//              {
//                 "long_name" : "Amphitheatre Pkwy",
//                 "short_name" : "Amphitheatre Pkwy",
//                 "types" : [ "route" ]
//              },
//              {
//                 "long_name" : "Mountain View",
//                 "short_name" : "Mountain View",
//                 "types" : [ "locality", "political" ]
//              },
//              {
//                 "long_name" : "Santa Clara County",
//                 "short_name" : "Santa Clara County",
//                 "types" : [ "administrative_area_level_2", "political" ]
//              },
//              {
//                 "long_name" : "California",
//                 "short_name" : "CA",
//                 "types" : [ "administrative_area_level_1", "political" ]
//              },
//              {
//                 "long_name" : "United States",
//                 "short_name" : "US",
//                 "types" : [ "country", "political" ]
//              },
//              {
//                 "long_name" : "94043",
//                 "short_name" : "94043",
//                 "types" : [ "postal_code" ]
//              }
//           ],
//           "formatted_address" : "1600 Amphitheatre Parkway, Mountain View, CA 94043, USA",
//           "geometry" : {
//              "location" : {
//                 "lat" : 37.4224764,
//                 "lng" : -122.0842499
//              },
//              "location_type" : "ROOFTOP",
//              "viewport" : {
//                 "northeast" : {
//                    "lat" : 37.4238253802915,
//                    "lng" : -122.0829009197085
//                 },
//                 "southwest" : {
//                    "lat" : 37.4211274197085,
//                    "lng" : -122.0855988802915
//                 }
//              }
//           },
//           "place_id" : "ChIJ2eUgeAK6j4ARbn5u_wAGqWA",
//           "types" : [ "street_address" ]
//        }
//     ],
//     "status" : "OK"
//  }