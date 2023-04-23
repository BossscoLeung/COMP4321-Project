<html>
<body>
<%
if(request.getParameter("txtname")!=null)
{
	out.println("You input "+request.getParameter("txtname"));
}
else
{
	out.println("You input nothing");
}

%>
</body>
</html>
