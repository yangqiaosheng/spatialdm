function atacheEventsOnCharts() {
	var obj = $("#TimeSeriesGraphic");
	$("#chart1").click(function() {
		obj.show("slow");
		obj.html("<img title='Year Level' src='TimeSeriesChart.png?areaid=" + getId() + "&level=year&width=600&height=300&smooth=yes&timestamp=" + new Date().getTime() + "  '>");
	});

	$("#chart2").click(function() {
		obj.show("slow");
		obj.html("<img title='Month Level' src='TimeSeriesChart.png?areaid=" + getId() + "&level=month&width=600&height=300&timestamp=" + new Date().getTime() + "  '>");
	});

	$("#chart3").click(function() {
		obj.show("slow");
		obj.html("<img title='Day Level' src='TimeSeriesChart.png?areaid=" + getId() + "&level=day&width=600&height=300&smooth=yes&timestamp=" + new Date().getTime() + "  '>");
	});

	$("#chart4").click(function() {
		obj.show("slow");
		obj.html("<img title='Hours Level' src='TimeSeriesChart.png?areaid=" + getId() + "&level=hour&width=600&height=300&smooth=yes&timestamp=" + new Date().getTime() + "  '>");
	});

	$("#chart5").click(function() {
		obj.show("slow");
		obj.html("<img title='Week Day Level' src='TimeSeriesChart.png?areaid=" + getId() + "&level=weekday&width=600&height=300&timestamp=" + new Date().getTime() + "  '>");
	});
}

function hideTimeSeriesGraphic() {
	$("#TimeSeriesGraphic").hide("slow");
	$("#TimeSeriesGraphic").html("");
}