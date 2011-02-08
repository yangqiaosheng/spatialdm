<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%
String path = request.getContextPath();
String basePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+path+"/";
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html><head>
<meta http-equiv="content-type" content="text/html; charset=UTF-8">


		<title>Request Kml Demo</title>
		<script type="text/javascript" src="script/jquery-1.4.2.min.js"></script>
		<script type="text/javascript"><!--
			var urlprefix = "<%=basePath%>RequestKml?xml=";
			//var xmlstr = "<request>\n\t<screen>\n\t\t<bounds>((49.89920084169041, 3.604375000000002), (51.68376322740835, 10.635625000000001))</bounds>\n\t\t<center>(50.8, 7.12)</center>\n\t\t<zoom>8</zoom>\n\t</screen>\n\t<calendar>\n\t\t<years>\n\t\t\t<year>2005</year>\n\t\t\t<year>2006</year>\n\t\t\t<year>2007</year>\n\t\t\t<year>2008</year>\n\t\t\t<year>2009</year>\n\t\t\t<year>2010</year>\n\t\t\t<year>2011</year>\n\t\t</years>\n\t\t<months>\n\t\t\t<month>January</month>\n\t\t\t<month>February</month>\n\t\t\t<month>March</month>\n\t\t\t<month>April</month>\n\t\t\t<month>May</month>\n\t\t\t<month>June</month>\n\t\t\t<month>July</month>\n\t\t\t<month>August</month>\n\t\t\t<month>September</month>\n\t\t\t<month>October</month>\n\t\t\t<month>November</month>\n\t\t\t<month>December</month>\n\t\t</months>\n\t\t<days>\n\t\t\t<day>1</day>\n\t\t\t<day>2</day>\n\t\t\t<day>3</day>\n\t\t\t<day>4</day>\n\t\t\t<day>5</day>\n\t\t\t<day>6</day>\n\t\t\t<day>7</day>\n\t\t\t<day>8</day>\n\t\t\t<day>9</day>\n\t\t\t<day>10</day>\n\t\t\t<day>11</day>\n\t\t\t<day>12</day>\n\t\t\t<day>13</day>\n\t\t\t<day>14</day>\n\t\t\t<day>15</day>\n\t\t\t<day>16</day>\n\t\t\t<day>17</day>\n\t\t\t<day>18</day>\n\t\t\t<day>19</day>\n\t\t\t<day>20</day>\n\t\t\t<day>21</day>\n\t\t\t<day>22</day>\n\t\t\t<day>23</day>\n\t\t\t<day>24</day>\n\t\t\t<day>25</day>\n\t\t\t<day>26</day>\n\t\t\t<day>27</day>\n\t\t\t<day>28</day>\n\t\t\t<day>29</day>\n\t\t\t<day>30</day>\n\t\t\t<day>31</day>\n\t\t</days>\n\t\t<hours>\n\t\t\t<hour>0</hour>\n\t\t\t<hour>1</hour>\n\t\t\t<hour>2</hour>\n\t\t\t<hour>3</hour>\n\t\t\t<hour>4</hour>\n\t\t\t<hour>5</hour>\n\t\t\t<hour>6</hour>\n\t\t\t<hour>7</hour>\n\t\t\t<hour>8</hour>\n\t\t\t<hour>9</hour>\n\t\t\t<hour>10</hour>\n\t\t\t<hour>11</hour>\n\t\t\t<hour>12</hour>\n\t\t\t<hour>13</hour>\n\t\t\t<hour>14</hour>\n\t\t\t<hour>15</hour>\n\t\t\t<hour>16</hour>\n\t\t\t<hour>17</hour>\n\t\t\t<hour>18</hour>\n\t\t\t<hour>19</hour>\n\t\t\t<hour>20</hour>\n\t\t\t<hour>21</hour>\n\t\t\t<hour>22</hour>\n\t\t\t<hour>23</hour>\n\t\t</hours>\n\t\t<weekdays>\n\t\t\t<weekday>Sunday</weekday>\n\t\t\t<weekday>Monday</weekday>\n\t\t\t<weekday>Tuesday</weekday>\n\t\t\t<weekday>Wednesday</weekday>\n\t\t\t<weekday>Thursday</weekday>\n\t\t\t<weekday>Friday</weekday>\n\t\t\t<weekday>Saturday</weekday>\n\t\t</weekdays>\n\t</calendar>\n</request>"

			$(function(){
				//$('#xmlstr').text(xmlstr);
				$('#urlprefix').text(urlprefix);
			});

			function genUrl(xmlstr){
				window.open(urlprefix+xmlstr);
			}
 		--></script>
	</head><body>
		<h1>Request Kml Demo</h1>
		<p style="color: rgb(170, 0, 0);">Recommended web browsers: Firefox</p>

		<table style="width: 1200px;" border="1">
			<tbody><tr>
				<td>
					Parameters:
				</td>
				<td>
					<p>
						Europe: minLatitude=34.26329 minLongitude=-13.119622 maxLatitude=72.09216 maxLongitude=35.287624 	<br>
					</p>

					<p>
						&lt;bounds&gt; Bounding Box: ((minLatitude, minLongitude), (maxLatitude, maxLongitude)) <br>
						&lt;center&gt; Screen Center: (latitude, longitude)	<br>
						&lt;zoom&gt; Google Maps Zoom Level: integer 5 - 11	<br>
						Polygon Radius: 5 -> R_320000,  6 -> R_160000, 7 -> R_80000, 8 -> R_40000, 9 -> R_20000, 10 -> R_10000, 11 -> R_5000	<br>
					</p>

					<p>
						&lt;year&gt;: 		2005 - 2011	<br>
						&lt;month&gt;: 		January - December	<br>
						&lt;day&gt;: 		1 - 31	<br>
						&lt;hour&gt;:		0 - 23 <br>
						&lt;weekdays&gt;:	Sunday - Saturday	<br>
					</p>

				</td>
			</tr>

		</tbody></table>

		<br>
		<form>
			<table style="width: 1200px;" border="1">
				<tr>
					<td>
						<div id="urlprefix"></div>
					</td>
				</tr>
				<tr>
					<td>
						<textarea name="xmlstr" id="xmlstr" style="width: 100%; height: 400px;">&lt;request&gt;
	&lt;screen&gt;
		&lt;bounds&gt;((49.89920084169041, 3.604375000000002), (51.68376322740835, 10.635625000000001))&lt;/bounds&gt;
		&lt;center&gt;(50.8, 7.12)&lt;/center&gt;
		&lt;zoom&gt;8&lt;/zoom&gt;
	&lt;/screen&gt;
	&lt;calendar&gt;
		&lt;years&gt;
			&lt;year&gt;2005&lt;/year&gt;
			&lt;year&gt;2006&lt;/year&gt;
			&lt;year&gt;2007&lt;/year&gt;
			&lt;year&gt;2008&lt;/year&gt;
			&lt;year&gt;2009&lt;/year&gt;
			&lt;year&gt;2010&lt;/year&gt;
			&lt;year&gt;2011&lt;/year&gt;
		&lt;/years&gt;
		&lt;months&gt;
			&lt;month&gt;January&lt;/month&gt;
			&lt;month&gt;February&lt;/month&gt;
			&lt;month&gt;March&lt;/month&gt;
			&lt;month&gt;April&lt;/month&gt;
			&lt;month&gt;May&lt;/month&gt;
			&lt;month&gt;June&lt;/month&gt;
			&lt;month&gt;July&lt;/month&gt;
			&lt;month&gt;August&lt;/month&gt;
			&lt;month&gt;September&lt;/month&gt;
			&lt;month&gt;October&lt;/month&gt;
			&lt;month&gt;November&lt;/month&gt;
			&lt;month&gt;December&lt;/month&gt;
		&lt;/months&gt;
		&lt;days&gt;
			&lt;day&gt;1&lt;/day&gt;
			&lt;day&gt;2&lt;/day&gt;
			&lt;day&gt;3&lt;/day&gt;
			&lt;day&gt;4&lt;/day&gt;
			&lt;day&gt;5&lt;/day&gt;
			&lt;day&gt;6&lt;/day&gt;
			&lt;day&gt;7&lt;/day&gt;
			&lt;day&gt;8&lt;/day&gt;
			&lt;day&gt;9&lt;/day&gt;
			&lt;day&gt;10&lt;/day&gt;
			&lt;day&gt;11&lt;/day&gt;
			&lt;day&gt;12&lt;/day&gt;
			&lt;day&gt;13&lt;/day&gt;
			&lt;day&gt;14&lt;/day&gt;
			&lt;day&gt;15&lt;/day&gt;
			&lt;day&gt;16&lt;/day&gt;
			&lt;day&gt;17&lt;/day&gt;
			&lt;day&gt;18&lt;/day&gt;
			&lt;day&gt;19&lt;/day&gt;
			&lt;day&gt;20&lt;/day&gt;
			&lt;day&gt;21&lt;/day&gt;
			&lt;day&gt;22&lt;/day&gt;
			&lt;day&gt;23&lt;/day&gt;
			&lt;day&gt;24&lt;/day&gt;
			&lt;day&gt;25&lt;/day&gt;
			&lt;day&gt;26&lt;/day&gt;
			&lt;day&gt;27&lt;/day&gt;
			&lt;day&gt;28&lt;/day&gt;
			&lt;day&gt;29&lt;/day&gt;
			&lt;day&gt;30&lt;/day&gt;
			&lt;day&gt;31&lt;/day&gt;
		&lt;/days&gt;
		&lt;hours&gt;
			&lt;hour&gt;0&lt;/hour&gt;
			&lt;hour&gt;1&lt;/hour&gt;
			&lt;hour&gt;2&lt;/hour&gt;
			&lt;hour&gt;3&lt;/hour&gt;
			&lt;hour&gt;4&lt;/hour&gt;
			&lt;hour&gt;5&lt;/hour&gt;
			&lt;hour&gt;6&lt;/hour&gt;
			&lt;hour&gt;7&lt;/hour&gt;
			&lt;hour&gt;8&lt;/hour&gt;
			&lt;hour&gt;9&lt;/hour&gt;
			&lt;hour&gt;10&lt;/hour&gt;
			&lt;hour&gt;11&lt;/hour&gt;
			&lt;hour&gt;12&lt;/hour&gt;
			&lt;hour&gt;13&lt;/hour&gt;
			&lt;hour&gt;14&lt;/hour&gt;
			&lt;hour&gt;15&lt;/hour&gt;
			&lt;hour&gt;16&lt;/hour&gt;
			&lt;hour&gt;17&lt;/hour&gt;
			&lt;hour&gt;18&lt;/hour&gt;
			&lt;hour&gt;19&lt;/hour&gt;
			&lt;hour&gt;20&lt;/hour&gt;
			&lt;hour&gt;21&lt;/hour&gt;
			&lt;hour&gt;22&lt;/hour&gt;
			&lt;hour&gt;23&lt;/hour&gt;
		&lt;/hours&gt;
		&lt;weekdays&gt;
			&lt;weekday&gt;Sunday&lt;/weekday&gt;
			&lt;weekday&gt;Monday&lt;/weekday&gt;
			&lt;weekday&gt;Tuesday&lt;/weekday&gt;
			&lt;weekday&gt;Wednesday&lt;/weekday&gt;
			&lt;weekday&gt;Thursday&lt;/weekday&gt;
			&lt;weekday&gt;Friday&lt;/weekday&gt;
			&lt;weekday&gt;Saturday&lt;/weekday&gt;
		&lt;/weekdays&gt;
	&lt;/calendar&gt;
&lt;/request&gt;</textarea>
					</td>
				</tr>
				<tr>
					<td>
						<input value="Generate Request Url" onclick="genUrl(xmlstr.value)" type="button">
					</td>
				</tr>
			</table>
		</form>

	</body></html>