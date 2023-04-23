<html>
<body>
	The words you entered are:<br><br>
<%
if(request.getParameter("txtquery")!=null)
{	
	out.println("The words you entered are:");
	out.println("<hr>");
	String str = request.getParameter("txtquery");
	String[] words = str.split(" ");
	for (String word : words) {
		out.println(word);
		out.println("<br>");
	}
}
else
{
	out.println("You input nothing");
}

%>
</body>
</html>
