package edu.upenn.cis555.searchengine.crawler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Date;


public class Utilities{
    public static String convertTime(long time){
        Locale locale = Locale.US;
        Date date = new Date(time);
        SimpleDateFormat f = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",locale);
        f.setTimeZone(TimeZone.getTimeZone("GMT"));
        return f.format(date);
    }

    public static long convertDate(String date){
        // Date date = null;
        // String dateString = headLinesDict.get(arg0);
        // SimpleDateFormat df1 = new SimpleDateFormat(
        //         "EEE, dd MMM yyyy HH:mm:ss z");
        // df1.setTimeZone(TimeZone.getTimeZone("GMT"));
        // try {
        //     date = df1.parse(dateString);
        // } catch (ParseException e) {
        //     e.printStackTrace();
        // }
        // long millisecond = date.getTime();
        // System.out.println(millisecond);
        // return millisecond;
        
        
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