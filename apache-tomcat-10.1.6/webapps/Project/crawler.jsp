<%@ page import="project_package.URLIndex" %>
<%@ page import="project_package.WordIndex" %>
<%@ page import="project_package.*" %>
<%@ page import="IRUtilities.Porter" %>
<%@ page import="java.net.URL" %>

<html>
	<head> 
		<title> Crawl result </title>
		<style>
	
			.input-element {
				font: 12pt sans-serif;
				width: 80%;
				align-self: auto;

			}

			.submit-element {
				font: 12pt sans-serif;
			}

			.url-element{
				font: 12pt sans-serif;
				width: 70%;
				align-self: auto;
			}

			.number-element{
				font: 12pt sans-serif;
				width: 5%;
				align-self: auto;
			}

			body {
				border: 40px solid white;
			}
	  
		</style> 
	</head> 
<body>
    Boscogle:
<form method="post" action="project.jsp"> 
    <input type="text" name="txtquery" class="input-element" required> 
    <!-- <input type="checkbox" name="save" value="save" checked>Save -->
    <input type="submit" value="Search" class="submit-element"> 
</form> 

    Crawl more page into database:
<form method="post" action="crawler.jsp"> 
	URL: <input type="text" name="urltext" class="url-element" required> 
	No. page: <input type="number" name="num" class="number-elemen  t" min="1" max="500" required>
	<input type="submit" value="crawl" class="submit-element"> 
</form>
<hr>

<%
    String urltext = request.getParameter("urltext");
    String num = request.getParameter("num");

    try {
        new URL(urltext);
        int maxPage = Integer.parseInt(num);
        out.println("Crawling " + maxPage + " pages from " + urltext);
        out.println("<br>");
        out.println("Please wait...");
        out.println("<br>");
        Crawler crawler = new Crawler();
        long startTime = System.currentTimeMillis();
        crawler.runCrawler(urltext, maxPage); 
        long endTime = System.currentTimeMillis();
        long totalTime = (endTime - startTime)/1000;
        out.println("Done! Total time used: "+ totalTime + " seconds");
    } catch (Exception e) {
        out.println("Invalid URL or number of pages!");
    }
%>
</body>
</html>
