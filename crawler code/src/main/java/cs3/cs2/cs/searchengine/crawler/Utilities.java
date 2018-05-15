package cs3.cs2.cs.searchengine.crawler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Date;
// import s3


public class Utilities{
    public static String convertTime(long time){
        Locale locale = Locale.US;
        Date date = new Date(time);
        SimpleDateFormat f = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",locale);
        f.setTimeZone(TimeZone.getTimeZone("GMT"));
        return f.format(date);
    }

    public static long convertDate(String date){

        
        Locale locale = Locale.US;
        long milliseconds=-1;
        SimpleDateFormat f = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",locale);
        f.setTimeZone(TimeZone.getTimeZone("GMT"));
        try {
            Date d = f.parse(date);
            milliseconds = d.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return milliseconds;
    }
}