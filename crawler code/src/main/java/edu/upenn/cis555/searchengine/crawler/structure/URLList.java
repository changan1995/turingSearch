package edu.upenn.cis555.searchengine.crawler.structure;

import java.io.Serializable;
import java.util.LinkedList;

public class URLList implements Serializable{
    private static final long serialVersionUID = 3484819397569692815L;
    public LinkedList<String> list;
    public URLList() {
        list = new LinkedList<>();
    }
    
} 