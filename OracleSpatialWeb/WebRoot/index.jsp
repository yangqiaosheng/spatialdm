<%@ page language="java" pageEncoding="UTF-8" contentType="text/html; charset=UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">

<html>
	<head>
		<meta http-equiv="pragma" content="no-cache">
		<meta http-equiv="cache-control" content="no-cache">
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		<meta http-equiv="expires" content="0">
		<meta http-equiv="keywords" content="kd-photomap,spatial,data,mining,visualization,fraunhofer,iais">
		<meta http-equiv="description" content="Spatial Data Visualization">
		<link rel="shortcut icon" href="images/favicon.ico" />

		<title>Spatial Data Visualization</title>
		<link rel="stylesheet" type="text/css" href="css/fonts.css" />

		<!-- Google Maps API lib -->
		<script type="text/javascript" src="http://www.google.com/jsapi?autoload={'modules':[{name:'maps',version:3,other_params:'sensor=false'}]}"></script>

		<!-- jQuery lib -->
<%--		<script type="text/javascript" src="script/jquery/jquery-1.7.1.js"></script>--%>
		<script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/jquery/1.5.2/jquery.min.js"></script>

		<!-- jQuery-UI plugin -->
		<script type="text/javascript" src="script/jquery/plugins/js/jquery-ui-1.8.16.custom.min.js"></script>
		<link type="text/css" rel="stylesheet" href="script/jquery/plugins/css/redmond/jquery-ui-1.8.16.custom.css" />
		<script type="text/javascript" src="script/init.js"></script>

		<!-- jQuery Date Picker plugin -->
		<script type="text/javascript" src="script/jquery/plugins/js/jquery.datepick.js"></script>
		<script type="text/javascript" src="script/jquery/plugins/js/jquery.validate.js"></script>
		<script type="text/javascript" src="script/jquery/plugins/js/jquery.datepick.validation.js"></script>
		<link rel="stylesheet" type="text/css" href="script/jquery/plugins/css/jquery.datepick.css" />
		<link rel="stylesheet" type="text/css" href="script/jquery/plugins/css/redmond.datepick.css" />

		<!-- jQuery Metadata plugin -->
		<script type="text/javascript" src="script/jquery/plugins/js/jquery.metadata.js"></script>

		<!-- highstock library -->
		<script type="text/javascript" src="script/highcharts/highcharts.js"></script>
<%--	<script type="text/javascript" src="script/highstock/highstock.js"></script>--%>

		<!-- jQuery Tagcloud plugin -->
		<script type="text/javascript" charset="utf-8" src="script/jquery/plugins/js/jquery.tagcloud.js"></script>

		<!--jQuery BeautyTips plugin -->
		<link type="text/css" rel="stylesheet" href="script/jquery/plugins/css/jquery.bt.css" />
		<script type="text/javascript" src="script/jquery/plugins/js/jquery.bt.js"></script>
<%--		<script type="text/javascript" src="script/jquery/plugins/js/jquery.bt.min.js"></script>--%>

		<!-- jQuery corner plugin -->
		<script type="text/javascript" src="script/jquery/plugins/js/jquery.corner.js"></script>

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

		<script type="text/javascript" src="script/GoogleMapsFeatures.js"></script>

		<link type="text/css" rel="stylesheet" href="css/tableStyle.css" />
		<link type="text/css" rel="stylesheet" href="css/controllerTimeStyle.css" />
		<link type="text/css" rel="stylesheet" href="css/general.css" />
		<link type="text/css" rel="stylesheet" href="css/carousel.css" />
		<link type="text/css" rel="stylesheet" href="css/calendar.css" />
		<link type="text/css" rel="stylesheet" href="css/legend.css" />
		<link type="text/css" rel="stylesheet" href="css/contextMenu.css" />
		<link type="text/css" rel="stylesheet" href="css/histogram.css" />


	</head>
	<body class="yui-skin-sam" onload="loadStart();">
	<div id="fb-root"></div>
		<script>
			(function(d, s, id) {
			  var js, fjs = d.getElementsByTagName(s)[0];
			  if (d.getElementById(id)) return;
			  js = d.createElement(s); js.id = id;
			  js.src = "//connect.facebook.net/de_DE/all.js#xfbml=1";
			  fjs.parentNode.insertBefore(js, fjs);
			}(document, 'script', 'facebook-jssdk'));
		</script>
		<div style="display: none;">
			<img id="calImg" src="images/calendar.gif" class="trigger" />
		</div>
		<div id="map_canvas"></div>
		<br />
		<fieldset id="controlPanel" class="timeCstar movable">
			<legend id="controlPanelLabel" class="controlPanel">
				<span>Control Panel</span>
			</legend>
			<div id="controlPanelContent" class="timeCstar">
				<div id="tableQuarry">
					<jsp:include page="table1.jsp" />
					<jsp:include page="table7.jsp" />
					<jsp:include page="table3.jsp" />
					<jsp:include page="table4.jsp" />
					<jsp:include page="table5.jsp" />
					<jsp:include page="aboutUs.jsp" />
				</div>
			</div>

		</fieldset>
		<jsp:include page="carousel.jsp" />
		<jsp:include page="legend.jsp" />

		<div id="tag"></div>
		<div id="tagClick"></div>
		<div class="photoWindow timeCstar" id="photoWindow">
			<div class="photoWindowDesc" id="photoWindowDesc"></div>
			<div class="photoWindowImg" id="photoWindowImg"></div>
		</div>
		<div id="dialog-confirm" title="System Requirements">
			<span class="ui-icon ui-icon-alert" style="float: left; margin: 0 7px 20px 0;"></span>
			<div>
				<div >
					To use all the functionalities on kd-photomap, you will need a PC with the system requirements as follows:
				</div>
				<ul>
					<li>
						Display Resolution: 1280 x 1024, or higher <p></p>
					</li>
					<li>
						Web Browser: Mozilla Firefox 3.5, or above
					</li>
				</ul>
			</div>
		</div>

		<script type="text/javascript" src="script/TimeController.js"></script>
		<script type="text/javascript" src="script/prepareXMLforServer.js"></script>
		<script type="text/javascript" src="script/LoadResultsFromServer.js"></script>
		<script type="text/javascript" src="script/Search.js"></script>
		<script type="text/javascript" src="script/charts.js"></script>
		<script type="text/javascript" src="script/circles.js"></script>
		<script type="text/javascript" src="script/contextMenu.js"></script>
		<script type="text/javascript" src="script/DragnDrop.js"></script>
		<script type="text/javascript" src="script/corner.js"></script>
		<script type="text/javascript" src="script/Validate.js"></script>
		<script type="text/javascript" src="script/HistogrammModel.js"></script>
		<script type="text/javascript" src="script/Histogramm.js"></script>
		<script type="text/javascript" src="script/MouseDragAndDrop.js"></script>
		<script type="text/javascript" src="script/CalendarJ.js"></script>
		<script type="text/javascript" src="script/sendToServer.js"></script>
	</body>
</html>
