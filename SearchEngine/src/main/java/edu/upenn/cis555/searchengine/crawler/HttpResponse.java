// package edu.upenn.cis555.searchengine.crawler;


// // import edu.upenn.ci5455.shw1.HttpServer;
// import java.net.Socket;
// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.Iterator;
// import java.util.Map;
// import java.util.Map.Entry;
// import javax.servlet.http.Cookie;

// public class HttpResponse{
//     //modified from hw1, for parse the input

//     public Map<String,String> headers;
//     private String initLine;
//     // private String optionMessage;
//     private String method;
//     private String statusCode;
//     private int port;
//     public String httpVersion;
//     public String status;
//     public String originResrouce;
//     // public boolean servlet_flag=false;
//     // public String servletString;
//     // public String servletStringbuff[];
//     // public String matchedServlet=null;
//     public Socket socket;
//     public String raw;
//     public String content=null;
//     public ArrayList<Cookie> cookie =new ArrayList<>();
    
//     public HttpResponse(String request){

//         this.raw=request;
//         // this.cookie =new ArrayList<>();
//         // this.socket=socket;
//         String originRequest[] = request.split("\r\n\r\n");
//         String[] buffer = originRequest[0].split("[\r\n]+");
//         // for(int i=0;i<buffer.length;i++){
//         //     System.out.println(buffer[i]);
//         // }
//         this.initLine=buffer[0];
//         // this.port =port;
//         String[] buffer2= this.initLine.split(" "); //first line:
//         this.httpVersion=buffer2[0].trim().toUpperCase();
//         this.statusCode=buffer2[1].trim().toLowerCase();
//         String statusCode = this.statusCode;
//         this.status=buffer2[2].trim().toLowerCase();
//         this.initalMap();
//         for(int index=1;index<buffer.length;index++) {
//             String[] temp=buffer[index].split(":",2);
// //            System.out.println(buffer[index]);
//             this.setHeaders(temp[0].trim().toLowerCase(), temp[1].trim().toLowerCase());
//             // if(temp[0].trim().toLowerCase().equals("cookie")){
//             //     this.cookie = new ArrayList<>();
//             //     this.addcookie(temp[1].trim());
//             // }
//         }
//         //handle body
//         if(originRequest.length==2){
//             this.content = originRequest[1];
// //            System.out.print(this.Content);
//         }
        
//     }

//     public void addcookie(String rawCookie){
//         for(String rawCookieEntry:rawCookie.split(";")){
//             String[] temp = rawCookieEntry.split("=",2);
//             Cookie c = new Cookie(temp[0].trim(), temp[1].trim());
//             this.cookie.add(c);
//         }
//     }

//     public String getMethod(){
//         return this.method;
//     }

//     public String getstatusCode(){
//         return this.statusCode;
//     }
    
//     public void initalMap(){
//         this.headers = new HashMap<String,String>();
//     }

//     public String toString(){
//         return this.raw;
//     }

//     public Map<String,String> setHeaders(String key, String value){
//         try{
//             this.headers.put(key,value);
//         }catch(Exception e){
//             System.err.println("error in setHeaders");
//         }
//         return this.headers;
//     }

//     public String getHeaders(String key){
//         return this.headers.get(key.toLowerCase());
//     }

//     public String getHttpVersion(){//HTTP/x.x
//         return this.httpVersion;
//     }

//     public String getStatus(){//HTTP/x.x
//         return this.status;
//     }

//     public String getContent(){//HTTP/x.x
//         return this.content;
//     }

//     public static void main(String args[]){
//         // HttpResponse hr = new HttpResponse("HTTP/1.1 200 OK\r\nhost:localhost:8080\r\nContent-Length:76\r\nContent-Type:text/html\r\n\r\n<p> Helloworld </p><p> Helloworld </p><p> Helloworld </p><p> Helloworld </p>\r\n");
//         String rawresponse="";
//         HttpClient hc = new HttpClient();
//         hc.send("HEAD","http://localhost:8080/Helloworld.html");
//         HttpResponse hr = new HttpResponse(hc.responseRaw);
//         System.out.println(hr.getHttpVersion());
//         System.out.println(hr.getstatusCode());
//         System.out.println(hr.getStatus());
//         System.out.println(hr.getHeaders("content-length"));
//         System.out.println(hr.toString());
//     }
// }