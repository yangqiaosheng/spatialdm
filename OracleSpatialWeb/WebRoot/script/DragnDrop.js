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
	$("#photoWindow").draggable( {
		opacity : 1
	});
	$("#pictureMouseOver").draggable( {
		opacity : 1
	});

	setMovableStyle($("#controlPanel"));
	setMovableStyle($("#infoContainer"));
	setMovableStyle($("#maxContainer"));
	setMovableStyle($("#photoWindow"));
	setMovableStyle($("#pictureMouseOver"));
});

function setMovableStyle(obj) {
	obj.css("cursor", "-moz-grab");
	obj.mouseup(function() {
		$(this).css("cursor", "-moz-grab");
	}).mousedown(function() {
		$(this).css('cursor', '-moz-grabbing');
	});
}
