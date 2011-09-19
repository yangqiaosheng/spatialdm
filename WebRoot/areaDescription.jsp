<%@ page language="java" import="de.fraunhofer.iais.spatial.dto.*" pageEncoding="UTF-8"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
	<head>
		<title>Region TimeSeriesChart</title>
	</head>
	<body>

		<br>
		<div style='width:600px; height:300px'>
			<img src='TimeSeriesChart.png?areaid=<%=request.getParameter("areaid") %>&level=<%=request.getParameter("level") %>' >

			<table width="100%">
				<tr>
					<th><a href='areaDescription.jsp?areaid=<%=request.getParameter("areaid") %>&level=hour'>hour</a></th>
					<th><a href='areaDescription.jsp?areaid=<%=request.getParameter("areaid") %>&level=day'>day</a></th>
					<th><a href='areaDescription.jsp?areaid=<%=request.getParameter("areaid") %>&level=month'>month</a></th>
					<th><a href='areaDescription.jsp?areaid=<%=request.getParameter("areaid") %>&level=year'>year</a></th>
				</tr>
			</table>
		</div>
	</body>
</html>