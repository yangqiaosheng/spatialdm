<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%
	String path = request.getContextPath();
	String basePath = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + path + "/";
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
	<head>
		<title>Region TimeSeriesChart</title>
	</head>
	<body>
		<%
			String both = request.getParameter("both");
			if(both != null){
				session.setAttribute("both", both);
			}
		%>
		<br>
		<%
			String[] areaids = request.getParameterValues("areaid");
			String areaidStr = "";
			for (String areaid : areaids) {
				areaidStr += "&areaid=" + areaid;
			}
		%>
		<div style='width: 900px; height: auto'>
			<div style='width: 900px; height: 400px'>
				<img src='TimeSeriesChart.png?smooth=true&width=900&height=400&level=<%=request.getParameter("level")%><%=areaidStr%>'>
			</div>
			<%
				String bothAttr = (String)session.getAttribute("both");
				if(bothAttr != null && bothAttr.equals("yes")){
			%>
			<div style='width: 900px; height: 400px'>
				<img src='TimeSeriesChart.png?width=900&height=400&level=<%=request.getParameter("level")%><%=areaidStr%>'>
			</div>
			<%
				}
			%>
			<table width="100%">
				<tr>
					<th>
						<a href='<%=basePath %>regionChart.jsp?level=hour<%=areaidStr%>'>Hour</a>
					</th>
					<th>
						<a href='<%=basePath %>regionChart.jsp?level=day<%=areaidStr%>'>Day</a>
					</th>
					<th>
						<a href='<%=basePath %>regionChart.jsp?level=month<%=areaidStr%>'>Month</a>
					</th>
					<th>
						<a href='<%=basePath %>regionChart.jsp?level=year<%=areaidStr%>'>Year</a>
					</th>
					<th>
						<a href='<%=basePath %>regionChart.jsp?level=weekday<%=areaidStr%>'>Weekday</a>
					</th>
				</tr>
			</table>
		</div>
	</body>
</html>