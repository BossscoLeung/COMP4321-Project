<%-- --%>
<%@ page import="StopStem" %>
<%@ page import="SearchEngine" %>
<%@ page import="PageMeta" %>
<%@ page import="URLIndex" %>
<%@ page import="WordIndex" %>
<%@ page import="IRUtilities.Porter" %>


<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>


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
	out.println("Query is:");
	out.println("<br>");
	String query = request.getParameter("txtquery");
	out.println(query);
	out.println("<hr>");
	

	/*
	SearchEngine se = new SearchEngine(query);
	Map<UUID, Double> resultlist = se.search();

	if(resultlist == null){
		out.println("No match of the phase Or your all your query words are not indexed.");
	}
	else{
		URLIndex urlIndex = new URLIndex("URL");
		WordIndex wordIndex = new WordIndex("WordDB");

		out.println("The results are:");
		out.println("<br>");
		int numPage = 1;
		for (Map.Entry<UUID, Double> entry : resultlist.entrySet()) {
			if (numPage > 51) {
				break;
			}

			UUID pageID = entry.getKey();
			Double score = entry.getValue();
			out.println("Ranking: " + numPage + " Score: " + score);
			out.println("<br>");
			out.println(urlIndex.getPageTitle(pageID));
			out.println("<br>");
			out.println(urlIndex.getPageURL(pageID));
			out.println("<br>");
			out.println(((PageMeta)urlIndex.getPageMeta(pageID)).getLastModified() + ", " + ((PageMeta)urlIndex.getPageMeta(pageID)).getPageSize());
			out.println("<br>");

			Map<UUID, Integer> wordList = wordIndex.getHighestFrequencyWords(pageID, 5);

            Map<String, Integer> wordList2 = new HashMap<String,Integer>();
            for (UUID wordID : wordList.keySet()){
                wordList2.put(wordIndex.getWord(wordID), wordList.get(wordID));
            }
			out.println("Top 5 frequency words: " + wordList2);
			out.println("<br>");

			Vector<UUID>parentsList = urlIndex.getChildToParents(pageID);
			int i = 1;
			for (UUID parent : parentsList){
                if (i >= 6) break;
                out.println("Parent link" + i + ": " + urlIndex.getPageURL(parent));
				out.println("<br>");
                i++;
            }

            Vector<UUID>childenList = urlIndex.getParentToChilden(pageID);
            i = 1;
            for (UUID child : childenList){
                if (i >= 6) break;
                out.println("Child link" + i + ": " + urlIndex.getPageURL(child));
				out.println("<br>");
                i++;
            }
			out.println("<hr>");
			numpage += 1;
		}
	} */

}
else{
	out.println("You input nothing");
	out.println("<hr>");
	Cookie ck[] = request.getCookies();
	int count = 0;
	if(ck != null){
		out.println("Search Histroy:");
		out.println("<br>");
		for(int i = ck.length-1;i >= 0; i--){
			if(ck[i].getName().equals("query"+String.valueOf(i))){
				out.println(ck[i].getValue());
				out.println("<br>");
			}
			count ++;
		}
	}
	out.println("<hr>");
}

%>
</body>
</html>
