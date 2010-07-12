<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>  
		<head>  
			<!-- define global variables -->
			<script type="text/javascript">
				g_kml_layer = new Array(10000);
				g_kml_counter = 0;
			</script>
			
			<!-- include yui core -->
			<script type="text/javascript" src="http://yui.yahooapis.com/3.1.0/build/yui/yui.js"></script>
			<!-- include all requirements for node -->
			<script type="text/javascript" src="http://yui.yahooapis.com/3.1.0/build/oop/oop.js"></script>
			<script type="text/javascript" src="http://yui.yahooapis.com/3.1.0/build/event-custom/event-custom.js"></script>
			<script type="text/javascript" src="http://yui.yahooapis.com/3.1.0/build/event/event.js"></script>
			<script type="text/javascript" src="http://yui.yahooapis.com/3.1.0/build/attribute/attribute.js"></script>
			<script type="text/javascript" src="http://yui.yahooapis.com/3.1.0/build/base/base.js"></script>
			<script type="text/javascript" src="http://yui.yahooapis.com/3.1.0/build/pluginhost/pluginhost.js"></script>
			<script type="text/javascript" src="http://yui.yahooapis.com/3.1.0/build/dom/dom.js"></script>
			<script type="text/javascript" src="http://yui.yahooapis.com/3.1.0/build/node/node.js"></script>
		  
			<!-- for example -->
			<script type="text/javascript" src="http://yui.yahooapis.com/3.1.0/build/yui/yui-min.js"></script>		  		  		
			<meta http-equiv="content-type" content="text/html; charset=utf-8">
			<title>Spatial Data Visualization</title>
						
			<link rel="stylesheet" type="text/css" href="css/fonts.css"> <!--for carousel -->
			<link type="text/css" rel="stylesheet" href="css/carousel.css">	<!-- for carousel -->
						
			<script src="http://maps.google.com/maps?file=api&amp;v=2&amp;sensor=true&amp;key=ABQIAAAAXpO0zI9yNeA_EFs7s1MwGRQH2Rk2fdHKrd3j6ZrwVbxJK3gvzxTzB5NzdXPflGWQvmnBnP55gTTSgQ"text/javascript"></script>          		    
			<!-- API 3 -->
			<script type="text/javascript" src="http://maps.google.com/maps/api/js?sensor=true"></script>
			<!-- ****************************************************************************  -->				  	
			<script type="text/javascript" src="script/timecontroller1.js"></script> 	  	
	    		<script src="script/utilities.js"></script> <!--for carosel-->
			<script src="script/carousel-min.js"></script> <!--for carousel-->
			<script type="text/javascript" src="script/carouselariaplugin.js"></script>	 <!--for carousel-->
			<!--*************************************Script**********************************-->
			<script type="text/javascript" src="script/googleMapsFeatures.js"></script> <!--for google map-->
			<script type="text/javascript" src="script/verticalmenufunctions.js"></script> <!--here I have functionality for the vertical menu  -->
			<!--*************************************Style**********************************-->
			<link type="text/css" rel="stylesheet" href="css/tableStyle.css">	<!-- for table left -->					
			<link type="text/css" rel="stylesheet" href="css/controllerTimeStyle.css">	<!-- for controller time style -->
			<link type="text/css" rel="stylesheet" href="css/contrllerSearchStyle.css">	<!-- for controller time style -->
					
			<!--*************************************Style for carousel**********************************-->			
			<link type="text/css" rel="stylesheet" href="css/carouselStyle.css">	<!-- for table carousel -->
			<link type="text/css" rel="stylesheet" href="css/verticalmenu.css"> <!-- vertical menu characteristics  -->
			
			<!--*************************************Style for button1**********************************-->	
			<script type="text/javascript" src="script/button1AJAX.js"></script> <!-- is the ajax for button1 -->					
			<script type="text/javascript" src="script/verticalmenu.js"></script>
						 	
			<!-- ************************** scripts by Yahoo api******************************************** -->
			
			<script type="text/javascript" src="script/oneFunctionCarousel.js"></script> <!-- one function for carousel, I think the  position in this document is important for this script -->
						
			<!-- ************************** stuff for time controller1 caledar******************************************** -->
			
			<!--  <link rel="stylesheet" type="text/css" href="http://yui.yahooapis.com/2.8.1/build/fonts/fonts-min.css" />  -->
			<link rel="stylesheet" type="text/css" href="http://yui.yahooapis.com/2.8.1/build/calendar/assets/skins/sam/calendar.css" />
			<link rel="stylesheet" type="text/css" href="css/localselecteddays.css" />
			
			<script type="text/javascript" src="http://yui.yahooapis.com/2.8.1/build/yahoo-dom-event/yahoo-dom-event.js"></script>
			<script type="text/javascript" src="http://yui.yahooapis.com/2.8.1/build/calendar/calendar-min.js"></script>
						
			
			<!-- try openlayers too -->
			<script type="text/javascript" src="http://www.openlayers.org/api/OpenLayers.js"></script>
			<script src="http://www.openlayers.org/api/OpenLayers.js"></script>
						 
			<!-- ******************************** -->
			<script type="text/javascript" src="script/drag&dropYahoo.js"></script> <!-- for drag and drop table -->										
  </head>           
  <body onload = "start(); loadStart();"; onunload="GUnload()";>
   <div id="map_canvas"></div>   
	<br/>
	<!--*****************************************table_tab_panel************************************************************--->
	<div id="move">
	
	 <table id="table1" border="0" cellpadding="0" cellspacing="0">
	<tr>
		<td>
			<div id="idmove">X</div>
		</td>		
		<td>
			<div id="tab1focus" class="tab tabfocus" style="display:block;"> Introduction </div>			
			<div id="tab1ready" class="tab tabhold" style="display:none;">
				<span onclick="ManageTabPanelDisplay('tab1focus','tab2ready','tab3ready', 'tab4ready', 'content1')">Introduction</span>
			</div>
		</td>
		<td width="2"> </td> <!-- the distance between the buttons -->
		<td>
			<div id="tab2focus" class="tab tabfocus" style="display:none;"> Pictures </div>
			<div id="tab2ready" class="tab tabhold" style="display:block;">
				<span onclick="ManageTabPanelDisplay('tab1ready','tab2focus','tab3ready','tab4ready','content2')">Pictures</span>
			</div>
		</td>
		<td width="2"> </td>
		<td>
			<div id="tab3focus" class="tab tabfocus" style="display:none;">Properties</div>
			<div id="tab3ready" class="tab tabhold" style="display:block;">
				<span onclick="ManageTabPanelDisplay('tab1ready','tab2ready','tab3focus', 'tab4ready','content3')">Properties</span>				
			</div>
		</td>
		<td width="2"></td>
		<td>
			<div id="tab4focus" class="tab tabfocus" style="display:none;">Others</div>
			<div id="tab4ready" class="tab tabhold" style="display:block;">
				<span onclick="ManageTabPanelDisplay('tab1ready','tab2ready','tab3ready','tab4focus', 'content4')">Others</span>				
			</div>
		</td>		
	</tr>	
	<tr>
	<!--I use this content in order to put well the second line in the table. to be no space between the first/second and second/third-->
		<td id="content" colspan="8">
		<!-- here I can put the content from controller1 -->
			<div id="content1" class="tabcontent" style="display:block;">
				<span>text</span>
			</div>

		<div id="content2" class="tabcontent" style="display: none;">										       <button name="button3" onclick="mapAddMakers()">Markers</button>		
		<!-- maybe I have to modify the name of the function to be more friendly -->
		<button name="button5" onclick="drawPolylines()">Loading Polylines xml test</button>				
		<button name="button100" onclick="addMapGPolygon()">whithout KML</button>

		<button name="button4" onclick="refresh()">Refresh page!</button>
		<!-- 	<button name="button8" onclick="colorPolygon()">Load something from DB</button>  -->
		</div>
		<div id="content3" class="tabcontent" style="display:none;">
				<p>
					<input type="checkbox" onclick="activateZoom(this.checked)" id="scrollWheelZoomActivation" CHECKED/> <!-- default is checked -->
					<strong> Check or uncheck to activate scroll wheel zoom </strong>
				</p>
				<p>
				<input name="search" type="text" id="search" size="25" autocomplete="off"/>
				<button name="button1x" onclick="SearchLocation()">Search</button>					
				</p>											
		</div>
		<div id="content4" class="new_tabcontent" style="display: none;">
		<!--	here i put the vertical menu
				-->

		<ul id="verticalmenu" class="glossymenu">
			<li><a href="javascript:AddPolynomsOnMilan()">Add Polygons
			in Milano</a></li>
			<li><a href="javascript:addxml1()">Add Polygons With Color</a></li>
			<li><a href="javascript:addxml2()">Add Polygons WO Color</a></li>
			<li><a href="#">button1</a>
			<ul>
				<li><a href="javascript:alert('a javascript function call')">details
				on polygons</a></li>
				<li><a href="javascript:alert('a javascript function call')">button1</a></li>
				<li><a href="javascript:alert('a javascript function call')">button1</a></li>
			</ul>
			</li>
			<li><a href="javascript:addxml3()">original Controller 1</a></li>
			<li><a href="javascript:alert('a javascript function call')">button1</a></li>
			<li><a href="javascript:alert('a javascript function call')">button1</a></li>
		</ul>
		</div>
		</td>
	</tr>
	
	<tr>	
	<td colspan="8">	
	<table id="table2">
	<tr> 		
			 <td><span id="controltime_header1" class="style1_header"><a class="turn-red" href="#" onclick="javascript:selectbuttonHeader('1');"><b><i>years:</i></b></a></span></td>

			<!-- <td><span id="controltime" class="style1"><a class="turn-red" href="javascript:alert('this must remain cheked')"><b><i>2005</i></b></a></span></td> -->
			
			<td><span id="controltime1" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('1');"><b><i>2005</i></b></a></span></td>						
			<td><span id="controltime2" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('2');"><b><i>2006</i></b></a></span></td>
			<td><span id="controltime3" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('3');"><b><i>2007</i></b></a></span></td>
			<td><span id="controltime4" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('4');"><b><i>2008</i></b></a></span></td>
			<td><span id="controltime5" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('5');"><b><i>2009</i></b></a></span></td>
			<td><span id="controltime6" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('6');"><b><i>2010</i></b></a></span></td>					
	</tr>
	</table>
	</td>
	</tr>
	
	<tr>
	<td colspan="8">
	<table id="table3">	
	<tr> 
			 <td><span id="controltime_header2" class="style1_header"><a class="turn-red" href="#" onclick="javascript:selectbuttonHeader('2');"><b><i>months:</i></b></a></span></td>
				
			<td><span id="controltime7" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('7');"><b><i>Jan.</i></b></a></span></td>
			<td><span id="controltime8" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('8');"><b><i>Feb.</i></b></a></span></td>
			<td><span id="controltime9" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('9');"><b><i>Mar.</i></b></a></span></td>
			<td><span id="controltime10" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('10');"><b><i>Apr.</i></b></a></span></td>
			<td><span id="controltime11" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('11');"><b><i>May.</i></b></a></span></td>
			<td><span id="controltime12" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('12');"><b><i>Jun.</i></b></a></span></td>
			<td><span id="controltime13" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('13');"><b><i>Jul.</i></b></a></span></td>
			<td><span id="controltime14" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('14');"><b><i>Aug.</i></b></a></span></td>
			<td><span id="controltime15" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('15');"><b><i>Sep.</i></b></a></span></td>
			<td><span id="controltime16" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('16');"><b><i>Oct.</i></b></a></span></td>
			<td><span id="controltime17" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('17');"><b><i>Nov.</i></b></a></span></td>
			<td><span id="controltime18" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('18');"><b><i>Dec.</i></b></a></span></td>
						
	</tr> 
	</table>
	</td>
	</tr>
	
    <tr>
	<td colspan="8">
	<table id="table4">	
	<tr> 
			    <td><span id="controltime_header3" class="style1_header"><a class="turn-red" href="#" onclick="javascript:selectbuttonHeader('3');"><b><i>days:</i></b></a></span></td>

			<td> <span id="controltime19" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('19');"><b><i>1</i></b></a></span></td>
			<td> <span id="controltime20" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('20');"><b><i>2</i></b></a></span></td>
			<td> <span id="controltime21" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('21');"><b><i>3</i></b></a></span></td>
			<td> <span id="controltime22" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('22');"><b><i>4</i></b></a></span></td>
			<td> <span id="controltime23" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('23');"><b><i>5</i></b></a></span></td>
			<td> <span id="controltime24" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('24');"><b><i>6</i></b></a></span></td>
			<td> <span id="controltime25" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('25');"><b><i>7</i></b></a></span></td>
			<td> <span id="controltime26" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('26');"><b><i>8</i></b></a></span></td>
			<td> <span id="controltime27" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('27');"><b><i>9</i></b></a></span></td>
			<td> <span id="controltime28" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('28');"><b><i>10</i></b></a></span></td>
			
			<td> <span id="controltime29" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('29');"><b><i>11</i></b></a></span></td>
			<td> <span id="controltime30" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('30');"><b><i>12</i></b></a></span></td>
			<td> <span id="controltime31" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('31');"><b><i>13</i></b></a></span></td>
			<td> <span id="controltime32" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('32');"><b><i>14</i></b></a></span></td>
			<td> <span id="controltime33" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('33');"><b><i>15</i></b></a></span></td>
			<td> <span id="controltime34" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('34');"><b><i>16</i></b></a></span></td>
			<td> <span id="controltime35" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('35');"><b><i>17</i></b></a></span></td>
			<td> <span id="controltime36" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('36');"><b><i>18</i></b></a></span></td>
			<td> <span id="controltime37" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('37');"><b><i>19</i></b></a></span></td>
			<td> <span id="controltime38" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('38');"><b><i>20</i></b></a></span></td>									
	</tr> 
	</table>
	</td>	 
	</tr>
	
	<tr>
	<td colspan="8">
	<table id="table4_1">	
	<tr> 
	
			<td> <span id="controltime39" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('39');"><b><i>21</i></b></a></span></td>
			<td> <span id="controltime40" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('40');"><b><i>22</i></b></a></span></td>
			<td> <span id="controltime41" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('41');"><b><i>23</i></b></a></span></td>
			<td> <span id="controltime42" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('42');"><b><i>24</i></b></a></span></td>
			<td> <span id="controltime43" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('43');"><b><i>25</i></b></a></span></td>
			<td> <span id="controltime44" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('44');"><b><i>26</i></b></a></span></td>
			<td> <span id="controltime45" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('45');"><b><i>27</i></b></a></span></td>
			<td> <span id="controltime46" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('46');"><b><i>28</i></b></a></span></td>
			<td> <span id="controltime47" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('47');"><b><i>29</i></b></a></span></td>
			<td> <span id="controltime48" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('48');"><b><i>30</i></b></a></span></td>
			<td> <span id="controltime49" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('49');"><b><i>31</i></b></a></span></td>
	</tr> 
	</table>
	</td>	 
	</tr>
	
	
	<tr>
	<td colspan="8">
	<table id="table5">	
	<tr> 
			 <td><span id="controltime_header4" class="style1_header"><a class="turn-red" href="#" onclick="javascript:selectbuttonHeader('4');"><b><i>hours:</i></b></a></span></td>
			<td><span id="controltime50" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('50');"><b><i>0</i></b></a></span></td>
			<td><span id="controltime51" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('51');"><b><i>1</i></b></a></span></td>
			<td><span id="controltime52" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('52');"><b><i>2</i></b></a></span></td>
			<td><span id="controltime53" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('53');"><b><i>3</i></b></a></span></td>
			<td><span id="controltime54" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('54');"><b><i>4</i></b></a></span></td>
			<td><span id="controltime55" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('55');"><b><i>5</i></b></a></span></td>
			<td><span id="controltime56" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('56');"><b><i>6</i></b></a></span></td>
			<td><span id="controltime57" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('57');"><b><i>7</i></b></a></span></td>
			<td><span id="controltime58" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('58');"><b><i>8</i></b></a></span></td>
			<td><span id="controltime59" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('59');"><b><i>9</i></b></a></span></td>
			
			<td><span id="controltime60" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('60');"><b><i>10</i></b></a></span></td>
			<td><span id="controltime61" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('61');"><b><i>11</i></b></a></span></td>
			<td><span id="controltime62" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('62');"><b><i>12</i></b></a></span></td>
			<td><span id="controltime63" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('63');"><b><i>13</i></b></a></span></td>
			<td><span id="controltime64" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('64');"><b><i>14</i></b></a></span></td>
			<td><span id="controltime65" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('65');"><b><i>15</i></b></a></span></td>
			<td><span id="controltime66" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('66');"><b><i>16</i></b></a></span></td>
			<td><span id="controltime67" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('67');"><b><i>17</i></b></a></span></td>
			<td><span id="controltime68" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('68');"><b><i>18</i></b></a></span></td>
			<td><span id="controltime69" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('69');"><b><i>19</i></b></a></span></td>
			
			<td><span id="controltime70" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('70');"><b><i>20</i></b></a></span></td>						
						
	</tr> 
	</table>
	</td>	
	</tr>
	
	
	<tr>
	<td colspan="8">
	<table id="table5_1">	
	<tr> 
			<td><span id="controltime71" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('71');"><b><i>21</i></b></a></span></td>
			<td><span id="controltime72" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('72');"><b><i>22</i></b></a></span></td>
			<td><span id="controltime73" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('73');"><b><i>23</i></b></a></span></td>
	 </tr> 
	</table>
	</td>	
	</tr>
	 
	<tr>
	<td colspan="8">
		<table id="table6">
		<tr>
			<td><span id="controltime74" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('74');"><b><i>Monday</i></b></a></span></td>
			<td><span id="controltime75" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('75');"><b><i>Tuesday</i></b></a></span></td>
			<td><span id="controltime76" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('76');"><b><i>Wednesday</i></b></a></span></td>
			<td><span id="controltime77" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('77');"><b><i>Thursday</i></b></a></span></td>
			<td><span id="controltime78" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('78');"><b><i>Friday</i></b></a></span></td>
			<td><span id="controltime79" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('79');"><b><i>Saturday</i></b></a></span></td>
			<td><span id="controltime80" class="style1"><a class="turn-red" href="#" onclick="javascript:selectbutton('80');"><b><i>Sunday</i></b></a></span></td>
		</tr>
		</table>
	</td>
	</tr>
	
	<tr>
		<td colspan="3">
		<button name="button8" onclick="javascript:refreshButtons();">Refresh buttons</button> 
		</td>
		<td colspan="2">
		<button name="button9" onclick="javascript:ask();">Ask query time!</button> 
		</td>
	</tr>
	
	<!-- here I have the second controller ********************************************************8 -->
	
	<tr>
		<td colspan=8 class="style1">
			<div class="yui-skin-sam">
				<div id="cal1Container"></div>
				<script type="text/javascript" src="script/calendar.js"></script>
				<div style="clear:both"></div>
			</div>
		</td>
	</tr>	
	
	<tr>		
		<td colspan="8" class="style1">
			<span> <button name="button10_1" onclick="javascript:refreshCalendar();">Refresh</button></span>
			<span> <button name="button10" onclick="javascript:selectedCalendar();">Ask Calendar!</button></span> 			
		</td>
	
		<td colspan=8 class="style1">		
			<div class="yui-skin-sam">
				<div id="caleventlog" class="eventlog">
					<div class="hd"></div>  
					<div id="evtentries" class="bd"></div>  
				</div>
			</div>
		</td>
	</tr>	
	<!-- here I have the third controller ********************************************************8 -->
	<tr>
		<td colspan="8" class="style1">
		<table id="table6">
			<tr>
				<td>		
				<form name="radio_form">		
				<span> 	<input TYPE=RADIO NAME="time" VALUE="year" CHECKED />year 
						<input TYPE=RADIO NAME="time" VALUE="month" />month 
						<input TYPE=RADIO NAME="time" VALUE="day" />day 
						<input TYPE=RADIO NAME="time" VALUE="hour" />hour
				</span></form>
				<button onclick="radio_buttons();">submit</button>				
				<span id="unit"> </span>
				<span>time unit</span>
				</td>
			</tr>
			<tr>
				<td colspan=1>time bar</td>
			
			</tr>						
			<!-- try to use slide bar froma another api, here I can not use proper move of the tables and move of the bar time -->		
		</table>
		</td>
	</tr>
		
</table>
</div>

<!--*****************************************Carousel Code*********************************************************************************--->	
	 <div id="container">
			<ol id="carousel">
				<li class="intro">
					<p>text1 text2 text3 text4 text5 text6 text7 text8 text9 text10 text11 text12 text13 text14 text15 text16 text17 
					text18 text19 text20 text21 text22 text23 text24 text25 text26 text27 text28 text29 text30 text31 text32 text33</p>		
				</li>
				<li class="item">
					<p>text1 text2 text3 text4 text5 text6 text7 text8 text9 text10 text11 text12 text13 text14 text15 text16 text17 
					text18 text19 text20 text21 text22 text23 text24 text25 text26 text27 text28 text29 text30 text31 text32 text33</p>		

				</li>
				<li class="item">
					<p>text1 text2 text3 text4 text5 text6 text7 text8 text9 text10 text11 text12 text13 text14 text15 text16 text17 
					text18 text19 text20 text21 text22 text23 text24 text25 text26 text27 text28 text29 text30 text31 text32 text33</p>		

				</li>
				<li class="item">
					<p>text1 text2 text3 text4 text5 text6 text7 text8 text9 text10 text11 text12 text13 text14 text15 text16 text17 
					text18 text19 text20 text21 text22 text23 text24 text25 text26 text27 text28 text29 text30 text31 text32 text33</p>		
				</li>
				<li class="item">
					<p>text1 text2 text3 text4 text5 text6 text7 text8 text9 text10 text11 text12 text13 text14 text15 text16 text17 
					text18 text19 text20 text21 text22 text23 text24 text25 text26 text27 text28 text29 text30 text31 text32 text33</p>		
	
				</li>
				<li class="item">
					<p>text1 text2 text3 text4 text5 text6 text7 text8 text9 text10 text11 text12 text13 text14 text15 text16 text17 
					text18 text19 text20 text21 text22 text23 text24 text25 text26 text27 text28 text29 text30 text31 text32 text33</p>		
										
				</li>
				<li class="item">
					<p>text1 text2 text3 text4 text5 text6 text7 text8 text9 text10 text11 text12 text13 text14 text15 text16 text17 
					text18 text19 text20 text21 text22 text23 text24 text25 text26 text27 text28 text29 text30 text31 text32 text33</p>		
			
				</li>
				<li class="item">
					<p>text1 text2 text3 text4 text5 text6 text7 text8 text9 text10 text11 text12 text13 text14 text15 text16 text17 
					text18 text19 text20 text21 text22 text23 text24 text25 text26 text27 text28 text29 text30 text31 text32 text33</p>		
				
				</li>
				<li class="item">
					<p>text1 text2 text3 text4 text5 text6 text7 text8 text9 text10 text11 text12 text13 text14 text15 text16 text17 
					text18 text19 text20 text21 text22 text23 text24 text25 text26 text27 text28 text29 text30 text31 text32 text33</p>		
			
				</li>
				<li class="item">
					<p>text1 text2 text3 text4 text5 text6 text7 text8 text9 text10 text11 text12 text13 text14 text15 text16 text17 
					text18 text19 text20 text21 text22 text23 text24 text25 text26 text27 text28 text29 text30 text31 text32 text33</p>		
			
				</li>
				<li class="item">
					<p>text1 text2 text3 text4 text5 text6 text7 text8 text9 text10 text11 text12 text13 text14 text15 text16 text17 
					text18 text19 text20 text21 text22 text23 text24 text25 text26 text27 text28 text29 text30 text31 text32 text33</p>		
		
				</li>
			</ol>
		</div>		
  </body> 
</html>
