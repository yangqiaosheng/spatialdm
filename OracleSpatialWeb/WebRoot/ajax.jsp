<%@ page language="java" import="java.util.*, de.fraunhofer.iais.spatial.dto.*" pageEncoding="UTF-8"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
	<head>
		<title>test AJAX</title>

		<link rel="stylesheet" type="text/css" href="script/jCarousel/skins/tango/skin.css" />
		<link type="text/css" rel="stylesheet" href="css/general.css" />
		<link type="text/css" rel="stylesheet" href="css/carousel.css" />
		<link type="text/css" rel="stylesheet" href="css/calendar.css" />
		<link type="text/css" rel="stylesheet" href="css/legend.css" />
		<link type="text/css" rel="stylesheet" href="css/contextMenu.css" />

		<script type="text/javascript" src="http://code.jquery.com/jquery-1.4.2.min.js"></script>
		<script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8/jquery-ui.min.js"></script>
		<script type="text/javascript" src="script/jCarousel/lib/jquery.jcarousel.min.js"></script>
		<script type="text/javascript" src="script/jCarousel.js"></script>
		<script type="text/javascript" src="script/displaySmallPhotos.js"></script>

	</head>
	<body>
		Request Parameters:
		<% FlickrDeWestAreaDto areaDto = (FlickrDeWestAreaDto)session.getAttribute("areaDto"); %>
		<% if(areaDto != null) { %>
		<table border="1">
			<tr>
				<td>Screen Boundary</td>
				<td><%=areaDto.getBoundaryRect() %></td>
			</tr>
			<tr>
				<td>Screen Center</td>
				<td><%=areaDto.getCenter() %></td>
			</tr>
			<tr>
				<td>Polygon Radius</td>
				<td><%=areaDto.getRadius() %></td>
			</tr>
			<tr>
				<td>Date Limit</td>
				<td><%=areaDto.getQueryStrs() %></td>
			</tr>
		</table>

		<hr>
		<br>

		Input an areaid:
		<input type="text" name="areaid" id="areaid" />
		<input type="button" value="Request Photos" onclick="getSmallPhotos(areaid.value)" />
		<div id="result"></div>
		<% } else { %>
			Please do a query here: <a href="index.jsp">index.jsp</a>
		<% }  %>

		<jsp:include page="carousel.jsp" />
	</body>
</html>