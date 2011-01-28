<!-- <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"> -->
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
pageEncoding="ISO-8859-1"%>
<html>
    <head>        
        <meta http-equiv="content-type" content="text/html; charset=utf-8" /> 
        <title>Spatial Data Visualization</title>

        <link rel="stylesheet" type="text/css" href="css/fonts.css" />

              <!-- key for http://kd-photomap.iais.fraunhofer.de -->
	<%-- <script src="http://maps.google.com/maps?file=api&amp;v=2&amp;sensor=true&amp;key=ABQIAAAAi1SR3oqNEcC8fZw7QDGwqRQH2Rk2fdHKrd3j6ZrwVbxJK3gvzxS14-FtxSKP0KVbN4pJnaoJfQXUtg" type="text/javascript"></script>
        <script type="text/javascript" src="http://maps.google.com/maps/api/js?sensor=true"></script> --%>
	<script type="text/javascript" src="http://www.google.com/jsapi?autoload={'modules':[{name:'maps',version:3,other_params:'sensor=false'}]}"></script> 
 
	 <script type="text/javascript" src="script/GoogleMapsFeatures.js"></script>         

        <link type="text/css" rel="stylesheet" href="css/tableStyle.css" />
        <link type="text/css" rel="stylesheet" href="css/controllerTimeStyle.css" />
        <link type="text/css" rel="stylesheet" href="css/contrllerSearchStyle.css" />
        <link type="text/css" rel="stylesheet" href="css/verticalmenu.css" />
	

        <!--this is jQuery use:)-->
        <script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/jquery/1.4.2/jquery.min.js">
        </script>
        <script src="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8/jquery-ui.min.js">
        </script>
        <link href="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8/themes/base/jquery-ui.css" rel="stylesheet" type="text/css" />
        <script type="text/javascript" src="script/polygon.min.js">
	</script>
        <script type="text/javascript" src="script/jCarousel/lib/jquery.jcarousel.min.js">
        </script>
        <link rel="stylesheet" type="text/css" href="script/jCarousel/skins/tango/skin.css" />
        <link rel="stylesheet" type="text/css" href="css/redmond.datepick.css" />
	<script type="text/javascript" src="script/jquery.datepick.js">
        </script>
	<script type="text/javascript" src="script/jquery.metadata.js">
	</script>
	<script type="text/javascript" src="script/jquery.validate.js">
	</script>
	<script type="text/javascript" src="script/jquery.datepick.validation.js">
	</script>
		
    	<link type="text/css" rel="stylesheet" href="css/general.css" />
	<link type="text/css" rel="stylesheet" href="css/carousel.css" />
    	<link type="text/css" rel="stylesheet" href="css/calendar.css" />
	<link type="text/css" rel="stylesheet" href="css/legend.css" />
	<link type="text/css" rel="stylesheet" href="css/contextMenu.css" />


 <!-- Core + Skin CSS --> 
    <link rel="stylesheet" type="text/css" href="http://yui.yahooapis.com/2.8.2r1/build/carousel/assets/skins/sam/carousel.css">
 
     <!-- Dependencies --> 
    <script src="http://yui.yahooapis.com/2.8.2r1/build/yahoo-dom-event/yahoo-dom-event.js"></script>
    <script src="http://yui.yahooapis.com/2.8.2r1/build/element/element-min.js"></script>
 
    <!-- Optional: Animation library for animating the scrolling of items --> 
    <script src="http://yui.yahooapis.com/2.8.2r1/build/animation/animation-min.js"></script>
    <!-- Optional: Connection library for dynamically loading items --> 
    <script src="http://yui.yahooapis.com/2.8.2r1/build/connection/connection-min.js"></script>
 
    <!-- Source file --> 
    <script src="http://yui.yahooapis.com/2.8.2r1/build/carousel/carousel-min.js"></script>



    </head>
    <!--if one of the functions gtom onload is not executing, then the chain is broken and the next ones will not execute as well-->
    <body onload="start(); loadStart();";  onunload="GUnload()"; onresize="onResize();">
        <div style="display: none;">
            <img id="calImg" src="images/calendar.gif" class="trigger" />
        </div>
        <div id="map_canvas">
        </div>
        <br/>
        <div id="move">
			<jsp:include page="table1.jsp"/>		
			<jsp:include page="table2.jsp"/>
			<jsp:include page="table3.jsp"/>
			<jsp:include page="table4.jsp"/>
			<jsp:include page="table5.jsp"/>
			
	</div>
		<jsp:include page="carousel.jsp"/>
		<jsp:include page="legend.jsp"/>
		
		<div class="photoWindow timeCstar" id="photoWindow">
			<div class="photoWindowDesc" id="photoWindowDesc"></div>
			<div class="photoWindowImg" id="photoWindowImg"></div>
		</div>		
		<script type="text/javascript" src="script/TimeController.js"> </script>
		<script type="text/javascript" src="script/traffic.js"></script>
		<script type="text/javascript" src="script/sendToServer.js"> </script>
		<script type="text/javascript" src="script/prepareXMLforServer.js"> </script>
		<script type="text/javascript" src="script/VerticalMenu.js"></script>
		<script type="text/javascript" src="script/LoadResultsFromServer.js"></script>
		<script type="text/javascript" src="script/Search.js"></script>
		<script type="text/javascript" src="script/circles.js"></script>
		<script type="text/javascript" src="script/charts.js"></script>
		<script type="text/javascript" src="script/LoadPolygons.js"></script>  
		<script type="text/javascript" src="script/contextMenu.js"></script>
	<script type="text/javascript" src="script/DragnDrop.js">
	</script>
	<script type="text/javascript" src="script/Resizeble.js">
	</script>
        <script type="text/javascript" src="script/jCarousel.js">
        </script>
        <script type="text/javascript" src="script/Legend.js">
        </script>
  	<script type="text/javascript" src="script/Validate.js">
        </script>
        <script type="text/javascript" src="script/CalendarJ.js">
        </script>
	<script type="text/javascript" src="script/Aggregation.js">
	</script> 
	<script type="text/javascript" src="script/IndividualPolygon.js">
	</script>
	<script type="text/javascript" src="script/displaySmallPhotos.js">
	</script>
	<script type="text/javascript" src="script/XmlOverlay.js">
	</script>	      				
     </body>
</html>
