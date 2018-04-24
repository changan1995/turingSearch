package edu.upenn.cis555.searchengine.crawler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import edu.upenn.cis555.searchengine.crawler.info.RobotsTxtInfo;
import edu.upenn.cis555.searchengine.crawler.structure.URLEntry;

public class RobotsRule {
	public static Map<String, RobotsTxtInfo> robotLst;
	
	private static final String agent = "cis455crawler";
	
	public RobotsRule() {
		robotLst = Collections.synchronizedMap(new LinkedHashMap<String, RobotsTxtInfo>(200, (float) 0.75, true) {
			private static final long serialVersionUID = -247150175692784225L;

			@Override
			protected boolean removeEldestEntry(java.util.Map.Entry<String, RobotsTxtInfo> eldest) {
				return size() > 200;
			}
		});
	}
	
	public int getDelay(String host) throws Exception {
		RobotsTxtInfo robot = robotLst.get(host);
		if (robot == null) {
			robot = requestRobotTxt(host);
		}
		if (robot.containsUserAgent(agent)) {
			return robot.getCrawlDelay(agent);
		}
		return robot.getCrawlDelay("*");
		
	}
	
	private RobotsTxtInfo requestRobotTxt(String host) throws Exception {
		HttpClient hc = new HttpClient();
		RobotsTxtInfo robot;
        if (!hc.send("GET", "http://" + host + "/robots.txt")) { //turn to absolute address
            robot = new RobotsTxtInfo("User-agent: *", host);
            robotLst.put(host, robot);
            return robot;
        }
        robot = new RobotsTxtInfo(hc.getContent(), host);
        robotLst.put(host, robot);
        return robot;
	}
	
	public boolean canCrawl(String host, String filePath) throws Exception {
		RobotsTxtInfo robot = robotLst.get(host);
		if (robot == null) {
			robot = requestRobotTxt(host);
		}
		ArrayList<String> disallowed = null;
		if (robot.containsUserAgent("*")) {
			disallowed = robot.getDisallowedLinks("*");
        } 
		if (robot.containsUserAgent(agent)) {
			disallowed = robot.getDisallowedLinks(agent);
        } else {
            return true;
        }
		for (String s : disallowed) {
            // System.out.println(s);
            if (s.equals("/") || filePath.startsWith(s)) {
                return false;
            }
        }
		return true;
	}
}
