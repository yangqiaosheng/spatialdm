// var listenerHandle;
// function agregationPolygonsAdd() {
// 	console.log("add Aggregation");
// 	$("#voronoiT").attr('value', 'Disable triangle agregation');
// 	listenerHandle = google.maps.event.addListener(map, 'idle',function() {
// 				removeCircles();
// 				var bounds = map.getBounds();
// 				var center = map.getCenter();
// 				var zoomLevel = map.getZoom();
// 				scaleLevelOnStart();
// 				askHistogram();
// 			});
// }
// 
// function agregationPolygonsRemove() {
// 	//refreshButtons();
// 	//removeCircles();
// 	google.maps.event.removeListener(listenerHandle);	
// }
// $(function() {
// 	jQuery("#AggregationCheckBox").change(function() {
// 		if (jQuery("#AggregationCheckBox").attr("checked") == true) {
// 			agregationPolygonsAdd();
// 			jQuery("#EnabledOrDisabled").html("Enabled");
// 		} else {
// 			agregationPolygonsRemove();
// 			jQuery("#EnabledOrDisabled").html("Disabled");
// 		}
// 	});
// 
// });
// $(function() {
// 	$("#timeC7, #timeC1, #timeC2, #timeC3, #tab1ready, #tab2ready, #tab3ready, #tab4ready")
// 			.hover(function() {
// 				$(this).removeClass("timeCstar");
// 				$(this).addClass("hover");
// 			}, function() {
// 				$(this).removeClass("hover");
// 				$(this).addClass("timeCstar");
// 			});
// });
// 
// $(function() {	
// 	$("#CalendarContent").hide();
// 	$("#histogramContent").hide();
// 	$("#SearchBox").hide();	
// 	$("#selectedYMDHCheckbox").click(function() {
// 		$("#CalendarContent").hide(1000);
// 		$("#histogramContent").hide(1000);
// 		$("#table7").css({ 'padding-bottom' : '0px'});
// 	}); 
// 	$("#CalendarcheckBox").click(function() {
// 		$("#CalendarContent").show(1000);
// 		$("#histogramContent").hide(1000);
// 		$("#table7").css({ 'padding-bottom' : '0px'});
// 	});
// 	$("#histogramCheckbox").click(function() {		
// 		$("#table7").css({ 'padding-bottom' : '40px'});
// 		$("#histogramContent").show(1000);
// 		$("#CalendarContent").hide(1000);
// 	});	
// 	$("#SearchCheckBox").toggle(function() {
// 	     $("#SearchBox").show();
// 	}, function() {
// 	      $("#SearchBox").hide();
// 	});
// 
// });
