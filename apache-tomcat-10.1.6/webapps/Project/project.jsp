<%-- --%>
<%@ page import="project_package.URLIndex" %>
<%@ page import="project_package.WordIndex" %>
<%@ page import="project_package.SearchEngine" %>
<%@ page import="IRUtilities.Porter" %>


<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.UUID" %>
<%@ page import="java.util.Vector" %>


<html>
	<head> 
		<title> Search result </title>
		<style>
	
			.input-element {
				font: 12pt sans-serif;
				width: 80%;
				align-self: auto;
	
			}
	
			.submit-element {
				font: 12pt sans-serif;
			}

			body {
			border: 40px solid white;
			}
	  
		</style> 
	</head> 
<body>
Boscogle:
<form method="post" action="project.jsp"> 
<input type="text" name="txtquery" class="input-element"> 
<input type="checkbox" name="save" value="save" checked>Save
<!-- <input type="checkbox" name="clear" value="clear">clear histroy -->
<input type="submit" value="Search" class="submit-element"> 
</form> 
<%

if(request.getParameter("txtquery")!=""){	
	out.println("Your search query:");
	String query = request.getParameter("txtquery");
	out.println(query);
	out.println("<hr>");
	

	
	SearchEngine se = new SearchEngine(query);
	Map<UUID, Double> resultlist = se.search();

	if(resultlist == null){
		out.println("No match of the phase Or all of your query words are not indexed.");
	}
	else{
		URLIndex urlIndex = new URLIndex("URL");
		WordIndex wordIndex = new WordIndex("WordDB");

		int numPage = 1;
		for (Map.Entry<UUID, Double> entry : resultlist.entrySet()) {
			if (numPage > 50) {
				break;
			}

			UUID pageID = entry.getKey();
			Double score = entry.getValue();
			out.println("Ranking: " + numPage + ", Score: " + score);
			out.println("<br>");
			out.println("Title: " + urlIndex.getPageTitle(pageID));
			out.println("<br>");
			out.println("URL: <a href=\"" + urlIndex.getPageURL(pageID) + "\" style=\"text-decoration:none;\" >" + urlIndex.getPageURL(pageID) + "</a>");
			out.println("<br>");
			out.println(urlIndex.getPageMetaString(pageID));

			out.println("<br>");

			Map<UUID, Integer> wordList = wordIndex.getHighestFrequencyWords(pageID, 5);

            Map<String, Integer> wordList2 = new HashMap<String,Integer>();
            for (UUID wordID : wordList.keySet()){
                wordList2.put(wordIndex.getWord(wordID), wordList.get(wordID));
            }
			out.println("Top 5 frequency words: " + wordList2);
			out.println("<br>");
			out.println("<br>");

			Vector<UUID>parentsList = urlIndex.getChildToParents(pageID);
			int i = 1;
			for (UUID parent : parentsList){
                if (i >= 6) break;
				
                out.println("Parent link " + i + ": " + "<a href=\"" + urlIndex.getPageURL(parent) + "\" style=\"text-decoration:none;\">" + urlIndex.getPageURL(parent) + "</a>");
				out.println("<br>");
                i++;
            }
			out.println("<br>");

            Vector<UUID>childenList = urlIndex.getParentToChilden(pageID);
            i = 1;
            for (UUID child : childenList){
                if (i >= 6) break;
                out.println("Child link " + i + ": " + "<a href=\"" + urlIndex.getPageURL(child) + "\" style=\"text-decoration:none;\">" + urlIndex.getPageURL(child) + "</a>");
				out.println("<br>");
                i++;
            }
			out.println("<hr>");
			numPage += 1;
		}
		urlIndex.close();
		wordIndex.close();
	} 

}
else{
	out.println("You input nothing");
	out.println("<hr>");
}


%>
</body>
</html>
