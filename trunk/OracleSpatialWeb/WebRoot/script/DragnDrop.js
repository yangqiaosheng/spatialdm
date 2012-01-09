$(document).ready(function() {
	$("#infoContainer").draggable( {
		opacity : 1
	});
	$("#controlPanel").draggable( {
		handle : '#idmove'
	});
	$("#maxContainer").draggable( {
		opacity : 1
	});
	$("#TagChart").draggable( {
		opacity : 1
	});
//	$("#photoWindow").draggable( {
//		opacity : 1
//	});

	setMovableStyle($("#controlPanel"));
	setMovableStyle($("#infoContainer"));
	setMovableStyle($("#maxContainer"));
	setMovableStyle($("#TagChartMenu"));

//	setMovableStyle($("#photoWindow"));
});

function setMovableStyle(obj) {
	obj.css("cursor", "-moz-grab");
	obj.mouseup(function() {
		$(this).css("cursor", "-moz-grab");
	}).mousedown(function() {
		$(this).css('cursor', '-moz-grabbing');
	});
}
