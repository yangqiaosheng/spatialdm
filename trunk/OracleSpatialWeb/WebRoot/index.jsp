<!-- <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"> -->
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
pageEncoding="ISO-8859-1"%>
<html>
    <head>
        <script type="text/javascript">           
	   var g_kml_layer = [];
           var g_kml_counter = 0;
        </script>
        <meta http-equiv="content-type" content="text/html; charset=utf-8" />
        <title>Spatial Data Visualization</title>
        <link rel="stylesheet" type="text/css" href="css/fonts.css" />
        <script src="http://maps.google.com/maps?file=api&amp;v=2&amp;sensor=true&amp;key=ABQIAAAAXpO0zI9yNeA_EFs7s1MwGRTTMp1AMdarjn7eaZ2eBJYjc9PhCRR3amHAAPmxas4EmiEg12cBftKILg"text/javascript">
        </script>	
        <script type="text/javascript" src="http://maps.google.com/maps/api/js?sensor=true">
        </script>
	
	<script type="text/javascript" src="script/GoogleMapsFeatures.js"></script>
        <script type="text/javascript" src="script/simileTimeLine.js"></script>
    
        <link type="text/css" rel="stylesheet" href="css/tableStyle.css" />
        <link type="text/css" rel="stylesheet" href="css/controllerTimeStyle.css" />
        <link type="text/css" rel="stylesheet" href="css/contrllerSearchStyle.css" />
        <link type="text/css" rel="stylesheet" href="css/verticalmenu.css" />
        	
	<!--Simile start -->
	<script src="http://api.simile-widgets.org/timeline/2.3.1/timeline-api.js?bundle=true" type="text/javascript"></script>
	<!--Simile end-->
	
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
	<script type="text/javascript" src="script/DragnDrop.js">    
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
	
   	  
        <link type="text/css" rel="stylesheet" href="css/general.css" />
        <link type="text/css" rel="stylesheet" href="css/carousel.css" />
        <link type="text/css" rel="stylesheet" href="css/calendar.css" />
        <link type="text/css" rel="stylesheet" href="css/legend.css" />
	<link type="text/css" rel="stylesheet" href="css/contextMenu.css" />
	
	
    </head>
    <!--if one of the functions gtom onload is not executing, then the chain is broken and the next ones will not execute as well-->
    <body onload="onLoad(); start(); loadStart();";  onunload="GUnload()"; onresize="onResize();">
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
	 </div>
	 <jsp:include page="carousel.jsp"/>  
	 <jsp:include page="legend.jsp"/>

      <script type="text/javascript" src="script/TimeController.js"> </script>              
      <script type="text/javascript" src="script/sendToServer.js"> </script>   
      <script type="text/javascript" src="script/prepareXMLforServer.js"> </script>   
      <script type="text/javascript" src="script/VerticalMenu.js"></script>	    

     </body>
</html>
