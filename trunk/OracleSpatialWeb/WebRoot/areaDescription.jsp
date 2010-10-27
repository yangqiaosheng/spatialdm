<%@ page language="java" import="de.fraunhofer.iais.spatial.dto.*" pageEncoding="UTF-8"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
	<head>
		<title>Area Description</title>
	</head>
	<body>
		<table border="1">
			<tr>
				<td>Total:</td>
				<td><%=request.getParameter("total") %></td>
			</tr>
			<tr>
				<td>Selected:</td>
				<td><%=request.getParameter("selected") %></td>
			</tr>
		</table>

		<br>

		<div style='width:600px; height:300px'>
			<img src='TimeSeriesChart?areaid=<%=request.getParameter("areaid") %>' >
		</div>

	</body>
</html>