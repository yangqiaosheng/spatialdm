<%@ page language="java" pageEncoding="UTF-8"%>

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
		<script type="text/javascript">
      		setInterval("tick()",50);
     		function tick(){
       		 	document.getElementById("clock").innerHTML=new Date();
	      	}
     		getSmallPhotos(1);
	    </script>
	</head>
	<body>
		<div style="width:350px;" id="clock"></div>
		Request Parameters:	<%= request.getParameter("areaid") %>

	</body>
</html>