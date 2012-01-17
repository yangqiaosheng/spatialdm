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
		<p style="color: rgb(170, 0, 0);">Recommended web browsers: IE8, Firefox, Chorme(with <a href='https://chrome.google.com/extensions/detail/gbammbheopgpmaagmckhpjbfgdfkpadb'>XMLTree</a> extension)</p>

		<table style="width: 1200px;" border="1">
			<tbody><tr>
				<td style="width:10%;">
					Parameters:
				</td>
				<td>
					<p>
						Ped: minLatitude=43.810000000 minLongitude=4.3513360925 maxLatitude=43.8200000000 maxLongitude=4.3633397349) 	<br>
					</p>
					<p>
						&lt;bounds&gt; Bounding Box: ((minLatitude, minLongitude), (maxLatitude, maxLongitude)) <br>
						Polygon Radius: 1000 <br>
					</p>
					<p>
						Transform:	<br>
						&lt;from&gt; point: (latitude, longitude) <br>
						&lt;to&gt; point: (latitude, longitude)	<br>
						&lt;scale&gt; 1.0	<br>
					</p>
					<p>
						&lt;year&gt;: 		2011	<br>
						&lt;month&gt;: 		August	<br>
						&lt;day&gt;: 		05	<br>
						&lt;hour&gt;:		16 - 22 <br>
						&lt;weekdays&gt;:	Sunday - Saturday	<br>
					</p>

				</td>
			</tr>

		</tbody></table>

		<br>
		<form>
			<table style="width: 1200px;" border="1">
				<tr>
					<td rowspan="3" style="width:10%">
						URL:
					</td>
					<td>
						<div id="urlprefix"></div>
					</td>
				</tr>
				<tr>
					<td>
						<textarea name="xmlstr" id="xmlstr" style="width: 100%; height: 400px;">&lt;request&gt;
	&lt;screen&gt;
		&lt;bounds&gt;((43.810000000, 4.3513360925), (43.8200000000, 4.3633397349))&lt;/bounds&gt;
	&lt;/screen&gt;
	&lt;transform&gt;
	    &lt;move&gt;
			&lt;from&gt;(0.0, 0.0)&lt;/from&gt;
			&lt;to&gt;(0.0, 0.0)&lt;/to&gt;
		&lt;/move&gt;
		&lt;scale&gt;1.0&lt;/scale&gt;
	&lt;/transform&gt;
	&lt;calendar&gt;
		&lt;years&gt;
			&lt;year&gt;2011&lt;/year&gt;
		&lt;/years&gt;
		&lt;months&gt;
			&lt;month&gt;August&lt;/month&gt;
		&lt;/months&gt;
		&lt;days&gt;0
			&lt;day&gt;05&lt;/day&gt;
		&lt;/days&gt;
		&lt;hours&gt;
			&lt;hour&gt;16&lt;/hour&gt;
			&lt;hour&gt;17&lt;/hour&gt;
			&lt;hour&gt;18&lt;/hour&gt;
			&lt;hour&gt;19&lt;/hour&gt;
			&lt;hour&gt;20&lt;/hour&gt;
			&lt;hour&gt;21&lt;/hour&gt;
			&lt;hour&gt;22&lt;/hour&gt;
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
						<p style="color:#880000">
							XML Schema: <a href='<%=basePath%>KmlRequest.xsd'><%=basePath%>KmlRequest.xsd</a>
						</p>
					</td>
				</tr>
				<tr>
					<td>
						<input value="Submit Request" onclick="genUrl(xmlstr.value)" type="button">
					</td>
				</tr>
			</table>
		</form>

	</body></html>