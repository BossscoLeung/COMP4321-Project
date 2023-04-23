<%@ page import="java.util.*" %>


<HTML>
<!-- I am HTML Comment -->
<%-- I am JSP Comment --%>
<BODY>

<% java.util.Date date = new java.util.Date(); %>

Hello!  The time is now <%= date %> <br>


<%! 
    String print_hour(Date date)
    {
        return "It is now "+date.getHours()%12+" o\'clock<BR>" ;
    }
%>

<%=print_hour(date)%>

<%
    out.println( "<BR>Your machine's address is " );
    out.println( request.getRemoteHost());
%>




</BODY>
</HTML>