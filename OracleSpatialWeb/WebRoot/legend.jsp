
<div id="pictureContainer" class='tabcontent'>
	<div id="TagGraphic" onclick='TagGraphicDisappear()'></div>
	<div id="pictureMouseOver"></div>
	<div id="TimeSeriesGraphic" style="display: none;" onclick='hideTimeSeriesGraphic()'>
		<%--		<div id="TimeSeriesGraphicLoading" style="display: none;"><img title='Loading' src='images/89.gif'></div>--%>
		<%--		<div id="TimeSeriesGraphicImg" style="display: none;"></div>--%>
	</div>
</div>
<div id="TagChart" class='tabcontent'>
	<div id="TagChartMenu">
		<div id="TagChartCloseButton" onclick="TagChartHide()">
			<img src="images/close.png" />
		</div>
	</div>

	<div id="TagChartGraphic"></div>
</div>



<div id="infoContainer" class="legendContainer timeCstar">
	<%--  <div id="legendHead">
     <img id="calImg" src="images/legend2.png" />
      <input type="button" id="showhide" value="hide"></div> --%>
	<div id="legendInfo" class="timeCstar">
		This content changes dynamically.
	</div>
	<div id="mapInfo" class="tabcontent">
		<span>Currently at scale level <span id="scaleLevel"></span> of 13.
		<%--		Map zoom: <span id="zoomMapLevel"> </span>--%> </span>
	</div>
</div>