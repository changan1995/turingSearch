package edu.upenn.cis555.searchengine.crawler.storage;

import java.io.Serializable;
import java.util.HashSet;

public class URLSeen implements Serializable{
	private static final long serialVersionUID = 3549429482663438823L;
	HashSet<String> urlSeen;
}
