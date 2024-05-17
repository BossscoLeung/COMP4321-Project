package project_package;

import java.util.Vector;
import java.util.stream.Collectors;

import org.htmlparser.beans.StringBean;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TitleTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;


import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;

import org.htmlparser.beans.LinkBean;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;

/**
 * Tools for crawling the web
 */
class Tools {
    public static String getPageTitle(String URL) throws Exception {
		// Create a new Parser object and parse the HTML document
        Parser parser = new Parser();
        parser.setResource(URL);
        
        // Use a TagNameFilter to get only the TitleTag nodes
        NodeList titleNodes = parser.extractAllNodesThatMatch(new TagNameFilter("title"));
        
        // Get the text content of the first TitleTag node
        TitleTag titleTag = (TitleTag) titleNodes.elementAt(0);
        String title = titleTag.getTitle();
        
		return title;
    }

	public static Vector<String> phaseTitle(String title) {
		Vector<String> v_word = new Vector<String>();
		StringTokenizer st = new StringTokenizer(title);
		while(st.hasMoreTokens()) {
			v_word.add(st.nextToken());
		}

		return v_word;
	}

	public static Date getPageLastModified(String URL) throws Exception {
		try {
			URL url = new URL(URL);
			URLConnection connection = url.openConnection();
			long lastModified = connection.getLastModified();
			Date date = new Date(lastModified);
			
			if (!date.equals(new Date(0)))
				return date;
		

			Parser parser = new Parser(connection);
			TagNameFilter filter = new TagNameFilter("head");
			NodeList list = parser.extractAllNodesThatMatch(filter);
			Node head = list.elementAt(0);

			NodeList children = head.getChildren();
			for (int i = 0; i < children.size(); i++) {
				Node child = children.elementAt(i);
				if (child.getText().contains("meta") && child.getText().contains("last-modified")) {
					String content = child.getText();
					int index = content.indexOf("last-modified");
					String lastModifiedStr = content.substring(index + 14);
					date = java.text.DateFormat.getDateInstance().parse(lastModifiedStr);
					if (!date.equals(new Date(0)))
						return date;
				}
			}

			HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
			httpConnection.setRequestMethod("HEAD");
			lastModified = httpConnection.getLastModified();
			date = new Date(lastModified);
			if (!date.equals(new Date(0)))
				return date;
			
			
			// no last modified date found, use the date of the page
			lastModified = connection.getDate();
			date = new Date(lastModified);
			return date;
		} catch (Exception e) {
			return new Date(0);
		}
	}

	public static int getPageContentLength(String URL) throws IOException{
		URL url = new URL(URL);
		URLConnection connection = url.openConnection();
		int contentLength = connection.getContentLength();
		return contentLength;
	}

	// convert a URL array to string vector
	public static Vector<String> convertUrlArrayToStringVector(URL[] urlArray) {
        List<URL> urlList = Arrays.asList(urlArray);
        Vector<String> stringVector = new Vector<String>(
            urlList.stream()
                .map(URL::toString)
                .collect(Collectors.toList())
        );
        return stringVector;
	}

}


public class Crawler
{
	
	private Vector<String> extractWords(String url) throws ParserException{
		// extract words in url and return them
		StringBean sb;
		Vector<String> v_word = new Vector<String>();

        sb = new StringBean ();
        sb.setLinks(false);
		// sb.setLinks(true);
        sb.setURL (url);	
		StringTokenizer st = new StringTokenizer(sb.getStrings ());
		while(st.hasMoreTokens()) {
			v_word.addElement(st.nextToken());
		}
		return v_word;
	}

	private Vector<String> extractLinks(String url) throws ParserException{
		// extract links in url and return them
		LinkBean lb = new LinkBean();
	    lb.setURL(url);
	    URL[] URL_array = lb.getLinks();
		Vector<String> v_link = Tools.convertUrlArrayToStringVector(URL_array);
		return v_link;
	}

	/**
	 * run the crawler for maxPage pages
	 * @param maxPage the maximum number of pages to be crawled starting from url
	 * @throws IOException
	 */
	public void runCrawler(String url, int maxPage) throws IOException{
		StopStem stopStem = new StopStem();
		Vector<String> proceedURL = new Vector<String>();
		Vector<String> waitingURL = new Vector<String>();
		String currentURL = url;
		URLIndex urlIndex = new URLIndex("URL");
		WordIndex wordIndex = new WordIndex("WordDB");
		
		try {
			while(maxPage>0){
				System.out.println("--------------------------------------------------------------------------");
				System.out.println("Checking " + currentURL);

				// get the last modified date of the current page
				Date lastModified = Tools.getPageLastModified(currentURL);
				if(lastModified.getTime() == 0){ // connection failed 
					// add the current page to the proceedURL
					proceedURL.add(currentURL);

					// get the next page
					if (waitingURL.size() > 0) {
						currentURL = waitingURL.get(0);
						waitingURL.remove(0);
					} 
					else {
						System.out.println("--------------------------------------------------------------------------");
						System.out.println("No more page to crawl");
						break;
					}
					continue;
				}
				System.out.println("Last modified: " + lastModified);

				// this page need update or not
				int pageDetermine = urlIndex.addPage(currentURL, lastModified);
				System.out.println(pageDetermine==0?"No need update":"Need update");

				// get the UUID of this page
				UUID thispageUuid = urlIndex.getPageId(currentURL);

				Vector<String> thisPageOutLinks = extractLinks(currentURL);
				// add all page
				for (String link : thisPageOutLinks) {
					urlIndex.addPage(link, null);
				}

				// need update
				if(pageDetermine == 1 || pageDetermine == 2){
					System.out.println("Crawling " + currentURL);
					if (pageDetermine == 2){ // need update
						// delete all the old index of this page
						urlIndex.updatePageCleaning(thispageUuid);
						wordIndex.delPage(thispageUuid);
						urlIndex.addPage(currentURL, lastModified);
					}

					// get the body of the current page
					Vector<String> thisPageWords = extractWords(currentURL);
					int pageSize = Tools.getPageContentLength(currentURL);
					if (pageSize == -1)
						pageSize = thisPageWords.size();

					// stop stem all body word in this page
					Vector<String> stopStem_body = stopStem.runStopStem(thisPageWords);

					// index all body word in this page
					for(String word : stopStem_body) {
						wordIndex.addWord(word);
					}

					// get the title of the current page
					String title = Tools.getPageTitle(currentURL);
					urlIndex.addPageTitle(thispageUuid, title);

					Vector<String> stopStem_title = stopStem.runStopStem(Tools.phaseTitle(title));
					
					// index all title word in this page
					for(String word : stopStem_title) {
						wordIndex.addWord(word);
					}

					// convert the stopStem_body into a map with word as key and the vector of the position of the word as value
					Map<String, Vector<Integer>> wordPositionMap = new HashMap<String, Vector<Integer>>();
					for (int i = 0; i < stopStem_body.size(); i++) {
						String word = stopStem_body.get(i);
						if (wordPositionMap.containsKey(word)) {
							wordPositionMap.get(word).add(i+1);
						} else {
							Vector<Integer> positionVector = new Vector<Integer>();
							positionVector.add(i+1);
							wordPositionMap.put(word, positionVector);
						}
					}

					// add forward and inverted index of this page
					for (Map.Entry<String, Vector<Integer>> entry : wordPositionMap.entrySet()) {
						UUID wordUuid = wordIndex.getWordId(entry.getKey());
						wordIndex.addForward(thispageUuid, wordUuid, entry.getValue());
						wordIndex.addInverted(wordUuid, thispageUuid, entry.getValue());
					}

					// convert the stopStem_title into a map with word as key and the vector of the position of the word as value
					Map<String, Vector<Integer>> wordPositionMap_title = new HashMap<String, Vector<Integer>>();
					for (int i = 0; i < stopStem_title.size(); i++) {
						String word = stopStem_title.get(i);
						if (wordPositionMap_title.containsKey(word)) {
							wordPositionMap_title.get(word).add(i+1);
						} else {
							Vector<Integer> positionVector = new Vector<Integer>();
							positionVector.add(i+1);
							wordPositionMap_title.put(word, positionVector);
						}
					}
					// add forward and inverted index of this page
					for (Map.Entry<String, Vector<Integer>> entry : wordPositionMap_title.entrySet()) {
						UUID wordUuid = wordIndex.getWordId(entry.getKey());
						wordIndex.addTitleInverted(wordUuid, thispageUuid, entry.getValue());
					}

					urlIndex.addLastModified(thispageUuid, lastModified);
					urlIndex.addPageSize(thispageUuid, pageSize);

					// add relationship
					for (String link : thisPageOutLinks) {
						UUID chilUuid = urlIndex.getPageId(link);
						urlIndex.addParentToChilden(thispageUuid, chilUuid);
						urlIndex.addChildToParents(chilUuid, thispageUuid);
					}
					
				}

				// add the current page to the proceedURL
				proceedURL.add(currentURL);

				// add the child page to the waitingURL
				for (String link : thisPageOutLinks) {
					if (!proceedURL.contains(link) && !waitingURL.contains(link)) {
						waitingURL.add(link);
					}
				}

				// get the next page
				if (waitingURL.size() > 0) {
					currentURL = waitingURL.get(0);
					waitingURL.remove(0);
				} 
				else {
					System.out.println("--------------------------------------------------------------------------");
					System.out.println("No more page to crawl");
					break;
				}

				urlIndex.commit();
				wordIndex.commit();

				// update the maxPage
				maxPage--;
			}

			urlIndex.finalize();
			wordIndex.finalize();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}

	
