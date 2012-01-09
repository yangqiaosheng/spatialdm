function atacheEventsOnCharts() {
	var obj = $("#TimeSeriesGraphic");
	$("#chart1").hover(function() {
		obj.show("slow");
		obj.html("<img title='Year Level' src='TimeSeriesChart.png?areaid=" + getId() + "&level=year&width=600&height=300&smooth=yes&timestamp=" + new Date().getTime() + "  '>");
	}, function() {
	});

	$("#chart2").hover(function() {
		obj.show("slow");
		obj.html("<img title='Month Level' src='TimeSeriesChart.png?areaid=" + getId() + "&level=month&width=600&height=300&timestamp=" + new Date().getTime() + "  '>");
	}, function() {
	});

	$("#chart3").hover(function() {
		obj.show("slow");
		obj.html("<img title='Day Level' src='TimeSeriesChart.png?areaid=" + getId() + "&level=day&width=600&height=300&smooth=yes&timestamp=" + new Date().getTime() + "  '>");
	}, function() {
	});
	$("#chart4").hover(function() {
		obj.show("slow");
		obj.html("<img title='Hours Level' src='TimeSeriesChart.png?areaid=" + getId() + "&level=hour&width=600&height=300&smooth=yes&timestamp=" + new Date().getTime() + "  '>");
	}, function() {
	});

	$("#chart5").hover(function() {
		obj.show("slow");
		obj.html("<img title='Week Day Level' src='TimeSeriesChart.png?areaid=" + getId() + "&level=weekday&width=600&height=300&timestamp=" + new Date().getTime() + "  '>");
	}, function() {
	});
}

function hideTimeSeriesGraphic() {
	$("#TimeSeriesGraphic").hide("slow");
	$("#TimeSeriesGraphic").html("");
}