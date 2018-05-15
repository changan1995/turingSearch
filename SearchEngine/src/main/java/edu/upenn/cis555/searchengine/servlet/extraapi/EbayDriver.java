package edu.upenn.cis555.searchengine.servlet.extraapi;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.upenn.cis555.searchengine.servlet.extraapi.URLReader;

/**
 *
 * @author rajeev jha (xxx@yyy.com)
 *
 */
public class EbayDriver {

    public final static String EBAY_APP_ID = "Quankang-CIS455Se-SBX-ae043dc2e-6edc6dd4";
    public final static String EBAY_FINDING_SERVICE_URI = "http://svcs.ebay.com/services/search/FindingService/v1?OPERATION-NAME="
            + "{operation}&SERVICE-VERSION={version}&SECURITY-APPNAME="
            + "{applicationId}&GLOBAL-ID={globalId}&keywords={keywords}"
            + "&paginationInput.entriesPerPage={maxresults}";
    public static final String SERVICE_VERSION = "1.0.0";
    public static final String OPERATION_NAME = "findItemsByKeywords";
    public static final String GLOBAL_ID = "EBAY-US";
    public final static int REQUEST_DELAY = 3000;
    public final static int MAX_RESULTS = 10;
    private int maxResults;

    public EbayDriver() {
        this.maxResults = MAX_RESULTS;

    }

    public EbayDriver(int maxResults) {
        this.maxResults = maxResults;
    }

    // public String getName() {
    //     return IDriver.EBAY_DRIVER;
    // }

    public void run(String tag) throws Exception {

        String address = createAddress(tag);
        print("sending request to :: ", address);
        String response = URLReader.read(address);
        print("response :: ", response);
        //process xml dump returned from EBAY
        processResponse(response);
        //Honor rate limits - wait between results
        Thread.sleep(REQUEST_DELAY);


    }

    private String createAddress(String tag) {

        //substitute token
        String address = EbayDriver.EBAY_FINDING_SERVICE_URI;
        address = address.replace("{version}", EbayDriver.SERVICE_VERSION);
        address = address.replace("{operation}", EbayDriver.OPERATION_NAME);
        address = address.replace("{globalId}", EbayDriver.GLOBAL_ID);
        address = address.replace("{applicationId}", EbayDriver.EBAY_APP_ID);
        address = address.replace("{keywords}", tag);
        address = address.replace("{maxresults}", "" + this.maxResults);

        return address;

    }

    private void processResponse(String response) throws Exception {


        XPath xpath = XPathFactory.newInstance().newXPath();
        InputStream is = new ByteArrayInputStream(response.getBytes("UTF-8"));
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = domFactory.newDocumentBuilder();


        Document doc = builder.parse(is);
        XPathExpression ackExpression = xpath.compile("//findItemsByKeywordsResponse/ack");
        XPathExpression itemExpression = xpath.compile("//findItemsByKeywordsResponse/searchResult/item");

        String ackToken = (String) ackExpression.evaluate(doc, XPathConstants.STRING);
        print("ACK from ebay API :: ", ackToken);
        if (!ackToken.equals("Success")) {
            throw new Exception(" service returned an error");
        }

        NodeList nodes = (NodeList) itemExpression.evaluate(doc, XPathConstants.NODESET);

        for (int i = 0; i < nodes.getLength(); i++) {

            Node node = nodes.item(i);

            String itemId = (String) xpath.evaluate("itemId", node, XPathConstants.STRING);
            String title = (String) xpath.evaluate("title", node, XPathConstants.STRING);
            String itemUrl = (String) xpath.evaluate("viewItemURL", node, XPathConstants.STRING);
            String galleryUrl = (String) xpath.evaluate("galleryURL", node, XPathConstants.STRING);

            String currentPrice = (String) xpath.evaluate("sellingStatus/currentPrice", node, XPathConstants.STRING);

            print("currentPrice", currentPrice);
            print("itemId", itemId);
            print("title", title);
            print("galleryUrl", galleryUrl);

        }

        is.close();

    }

    private void print(String name, String value) {
        System.out.println(name + "::" + value);
    }

    public static void main(String[] args) throws Exception {
        EbayDriver driver = new EbayDriver();
        String tag = "Velo binding machine";
        driver.run(java.net.URLEncoder.encode(tag, "UTF-8"));

    }
}