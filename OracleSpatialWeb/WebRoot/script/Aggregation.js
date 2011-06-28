var listenerHandle;
function agregationPolygonsAdd() {
	$("#voronoiT").attr('value', 'Disable triangle agregation');
	$("#legendInfo").html("<span>number of pictures </span><br/> <span> <img src='images/circle_bl.ico' width='20px' height='20px'/> 1-99 </span> <br/><span> <img src='images/circle_gr.ico' width='20px' height='20px' /> 100-999</span><br/><span> <img src='images/circle_lgr.ico' width='20px' height='20px'/> 1000-9999</span> <br/><span> <img src='images/circle_or.ico' width='20px' height='20px'/> > 10000</span>");
	listenerHandle = google.maps.event.addListener(map, 'zoom_changed',
			function() {
				removeCircles(); // circle.js
				var bounds = map.getBounds();
				var center = map.getCenter();
				var zoomLevel = map.getZoom();
				//$("#legendInfo").html(zoomLevel+" ");
//				headerXML = createHeaderXML(bounds, center, zoomLevel);
//				bodyXML = "";
//				sendToServer_ScreenCenter_ScreenBounds(headerXML, bodyXML);
			});
}

function agregationPolygonsRemove() {
	refreshButtons();
	google.maps.event.removeListener(listenerHandle);
}
$(function() {
	jQuery("#AggregationCheckBox").change(function() {
		if (jQuery("#AggregationCheckBox").attr("checked") == true) {
			agregationPolygonsAdd();
			jQuery("#EnabledOrDisabled").html("Enabled");
		} else {
			agregationPolygonsRemove();
			jQuery("#EnabledOrDisabled").html("Disabled");
		}
	});

});
$(function() {
	$(
			"#timeC1, #timeC2, #timeC3, #tab1ready, #tab2ready, #tab3ready, #tab4ready")
			.hover(function() {
				$(this).removeClass("timeCstar");
				$(this).addClass("hover");
			}, function() {
				$(this).removeClass("hover");
				$(this).addClass("timeCstar");
			});
});

$(function() {
	$("#CalendarContent").hide();
	//$("#selectedYMDHContent").hide();
	$("#histogramContent").hide();

	$("#selectedYMDHCheckbox").click(function() {
		//$("#selectedYMDHContent").show(1000);
		$("#CalendarContent").hide(1000);
		$("#histogramContent").hide(1000);
	});
	$("#CalendarcheckBox").click(function() {
		$("#CalendarContent").show(1000);
		//$("#selectedYMDHContent").hide(1000);
		$("#histogramContent").hide(1000);
	});
	$("#histogramCheckbox").click(function() {
		$("#histogramContent").show(1000);
		$("#CalendarContent").hide(1000);
		//$("#selectedYMDHContent").hide(1000);
	});
});
