<html>
<head><title> Use of cookies </title></head>
<body>
<%
Cookie ck[] = request.getCookies();
int times = 0;
if(ck == null)
{
     out.println("It is the first time you visit this page");
}
else
{
     for(int i = 0; i < ck.length; i++)
     {
	if(ck[i].getName().equals("times"))
		times = Integer.parseInt(ck[i].getValue());
     }
     out.println("It is the "+(times+1)+" times you visit this page");
}

Cookie newck = new Cookie("times",Integer.toString(times+1));
newck.setMaxAge(60*60); // 1 hour
response.addCookie(newck);

%>
</body>
</html>
