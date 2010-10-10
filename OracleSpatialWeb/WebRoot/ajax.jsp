<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>

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

		<script type="text/javascript" src="script/jquery.js"></script>
		<script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8/jquery-ui.min.js"></script>
		<script type="text/javascript" src="script/jCarousel/lib/jquery.jcarousel.min.js"></script>
		<script type="text/javascript" src="script/jCarousel.js"></script>
		<script type="text/javascript" src="script/displaySmallPhotos.js"></script>

	</head>
	<body>
		Test The AJAX:
		<input type="text" id="username" />
		<input type="button" value="submit" onclick="getSmallPhotos()" />
		<div id="result"></div>

		<jsp:include page="carousel.jsp" />
	</body>
</html>