<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">

<html>
<head>
<meta http-equiv="content-type" content="text/html; charset=utf-8" />
<title>Spatial Data Visualization</title>

<link rel="stylesheet" type="text/css" href="css/fonts.css" />

<script type="text/javascript"
	src="http://www.google.com/jsapi?autoload={'modules':[{name:'maps',version:3,other_params:'sensor=false'}]}"></script>
	
	
	<!--this is jQuery use:)-->
<script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/jquery/1.4.2/jquery.min.js"> </script>
<script src="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8/jquery-ui.min.js"> </script>

<!--tags library-->
<script src="script/jquery.tagcloud.js" type="text/javascript" charset="utf-8"></script> 

      <!--using library bt-->
<%-- <script type="text/javascript" src="script/bt/jquery.bt.js"></script> --%>
<script type="text/javascript" src="script/bt/jquery.bt.min.js"></script>
<link type="text/css" rel="stylesheet" href="script/bt/jquery.bt.css" />
<script type="text/javascript" src="script/jquery.corner.js"> </script>

<script type="text/javascript" src="script/GoogleMapsFeatures.js"></script>


<link type="text/css" rel="stylesheet" href="css/tableStyle.css" />
<link type="text/css" rel="stylesheet" href="css/controllerTimeStyle.css" />



<link
	href="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8/themes/base/jquery-ui.css"
	rel="stylesheet" type="text/css" />
<link rel="stylesheet" type="text/css" href="css/redmond.datepick.css" />
<script type="text/javascript" src="script/jquery.datepick.js"></script>
<script type="text/javascript" src="script/jquery.metadata.js"></script>
<script type="text/javascript" src="script/jquery.validate.js"> </script>
<script type="text/javascript"src="script/jquery.datepick.validation.js"> </script>


<link type="text/css" rel="stylesheet" href="css/general.css" />
<link type="text/css" rel="stylesheet" href="css/carousel.css" />
<link type="text/css" rel="stylesheet" href="css/calendar.css" />
<link type="text/css" rel="stylesheet" href="css/legend.css" />
<link type="text/css" rel="stylesheet" href="css/contextMenu.css" />


<!-- Core + Skin CSS -->
<link rel="stylesheet" type="text/css"
	href="http://yui.yahooapis.com/2.8.2r1/build/carousel/assets/skins/sam/carousel.css">

<!-- Dependencies -->
<script
	src="http://yui.yahooapis.com/2.8.2r1/build/yahoo-dom-event/yahoo-dom-event.js"></script>
<script
	src="http://yui.yahooapis.com/2.8.2r1/build/element/element-min.js"></script>

<!-- Optional: Animation library for animating the scrolling of items -->
<script
	src="http://yui.yahooapis.com/2.8.2r1/build/animation/animation-min.js"></script>
<!-- Optional: Connection library for dynamically loading items -->
<script
	src="http://yui.yahooapis.com/2.8.2r1/build/connection/connection-min.js"></script>

<!-- Source file -->
<script
	src="http://yui.yahooapis.com/2.8.2r1/build/carousel/carousel-min.js"></script>

<script type="text/javascript">


    var _gaq = _gaq || [];
    _gaq.push(['_setAccount', 'UA-22122163-1']);
    _gaq.push(['_trackPageview']);

    (function() {
      var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
      ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
      var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
    })();

   </script>   
<link type="text/css" rel="stylesheet" href="css/histogram.css" />


</head>
<body class="yui-skin-sam" onload="loadStart();">
	<div style="display: none;">
		<img id="calImg" src="images/calendar.gif" class="trigger"/>
	</div>
	<div id="map_canvas"></div>
	<br />
	<div id="move" class="timeCstar">
		<div id="QuarryHead" class="tabcontent">
			<span>Control Panel</span> <input type="button" id="control" value="hide" onclick="hidemoveCP()" />
			<span>Carousel: </span> <input type="button" id="carouselControl" value="hide" onclick="hidemoveCarousel()" />
		</div>
		<div id="move1"  class="timeCstar">
			<div id="tableQuarry">
				<jsp:include page="table1.jsp" /> 
				<jsp:include page="table7.jsp" />
				<jsp:include page="table3.jsp" /> 
				<jsp:include page="table4.jsp" />
				<jsp:include page="table5.jsp" />
				<jsp:include page="table6.jsp" />
			</div>
		</div>

	</div>
	<jsp:include page="carousel.jsp" />
	<jsp:include page="legend.jsp" />
	
	<div id="tag"></div>
	<div id="tagClick"></div>
	<div class="photoWindow timeCstar" id="photoWindow">
		<div class="photoWindowDesc" id="photoWindowDesc"></div>
		<div class="photoWindowImg" id="photoWindowImg"></div>
	</div>	
	<script type="text/javascript" src="script/TimeController.js"> </script>		
	<script type="text/javascript" src="script/prepareXMLforServer.js"> </script>	
	<script type="text/javascript" src="script/LoadResultsFromServer.js"></script>
	<script type="text/javascript" src="script/Search.js"></script>	
	<script type="text/javascript" src="script/charts.js"></script>	
	<script type="text/javascript" src="script/circles.js"></script>
	<script type="text/javascript" src="script/contextMenu.js"></script>
	<script type="text/javascript" src="script/DragnDrop.js">
	</script>
	<script type="text/javascript" src="script/Resizeble.js">
	</script>	
	<script type="text/javascript" src="script/Legend.js">
        </script>
	<script type="text/javascript" src="script/LoadPolygons.js"></script>
	<script type="text/javascript" src="script/Validate.js">
        </script>	
	<script type="text/javascript" src="script/Aggregation.js">
	</script>			
	<script type="text/javascript" src="script/HistogrammModel.js">
	</script>
	<script type="text/javascript" src="script/Histogramm.js"></script>
	<script type="text/javascript" src="script/MouseDragAndDrop.js">
	</script>
	<script type="text/javascript" src="script/CalendarJ.js"></script>	
	<script type="text/javascript" src="script/sendToServer.js"> </script>
		
</body>
</html>
