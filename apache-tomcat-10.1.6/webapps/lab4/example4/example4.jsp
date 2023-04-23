<html>
<head> <title>  Use of sessions</title> </head>
<body>
<%
if(session.isNew()){
session.setMaxInactiveInterval(1800);
%>

Hi, what is your name?
<form method="post">
<input type="text" name="txtname">
<input type="submit" value="Submit">
</form>
<%
}else{
	
	if(request.getParameter("txtname")!=null)
	{
		session.setAttribute("name",request.getParameter("txtname"));
	}

	if(session.getAttribute("name")==null)
	{
		session.invalidate();
		out.println("You have not entered your name! Please refresh");
	}
	else
	{
		
		out.println("Hi! "+session.getAttribute("name"));		
	}
	
}
%>
<body>
</html>
