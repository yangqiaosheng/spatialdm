var circles = new Array();
function DistanceWidget(center, selectedPictures) {
	var radiusWidget = RadiusWidget(map, center, selectedPictures);
}
// DistanceWidget.prototype = new google.maps.MVCObject();

function RadiusWidget(map, center, selectedPictures) {
	var color;
	var raza;
	if (selectedPictures >= 10000) {
		color = '#CC0000'; // red
	}
	if ((selectedPictures >= 1000) && (selectedPictures <= 9999)) {
		color = '#CCFF00'; // yellow
	}
	if ((selectedPictures >= 100) && (selectedPictures <= 999)) {
		color = '#00FF00'; // green
	}
	if ((selectedPictures >= 1) && (selectedPictures <= 99)) {
		color = '#0000CC'; // blue
	}
	if (selectedPictures == 0) {
		color = '#FFFFFF'; // white
	}
	if (map.getZoom() == 1) {
		$("#zoomMapLevel").html(" " + map.getZoom());
		// $("#legendInfo").append(" here1 ");
		raza = 320000;
	}
	if (map.getZoom() == 2) {
		$("#zoomMapLevel").html(" " + map.getZoom());
		raza = 320000
	}
	if (map.getZoom() == 3) {
		$("#zoomMapLevel").html(" " + map.getZoom());
		raza = 160000;
	}
	if (map.getZoom() == 4) {
		$("#zoomMapLevel").html(" " + map.getZoom());
		raza = 80000;
	}
	if (map.getZoom() == 5) {//
		$("#zoomMapLevel").html(" " + map.getZoom());
		raza = 40000;
	}
	if (map.getZoom() == 6) {//ok
		$("#zoomMapLevel").html(" " + map.getZoom());
		raza = 20000;
	}
	if (map.getZoom() == 7) {//ok
		$("#zoomMapLevel").html(" " + map.getZoom());
		raza = 10000;
	}
	if (map.getZoom() == 8) {//ok
		$("#zoomMapLevel").html(" " + map.getZoom());
		raza = 5000;
	}
	if (map.getZoom() == 9) {//ok
		$("#zoomMapLevel").html(" " + map.getZoom());
		raza = 2500;
	}
	if (map.getZoom() == 10) {//ok
		$("#zoomMapLevel").html(" " + map.getZoom());
		raza = 1250;
	}
	if (map.getZoom() == 11) {//ok
		$("#zoomMapLevel").html(" " + map.getZoom());
		raza = 625;
	}
	if (map.getZoom() == 12) {//ok
		$("#zoomMapLevel").html(" " + map.getZoom());
		raza = 312.5;
	}
	if (map.getZoom() == 13) {//ok
		$("#zoomMapLevel").html(" " + map.getZoom());
		raza = 156.25;
	}
	if (map.getZoom() == 14) {//ok
		$("#zoomMapLevel").html(" " + map.getZoom());
		raza = 78.125;
	}
	if (map.getZoom() == 15) {
		$("#zoomMapLevel").html(" " + map.getZoom());
		raza = 78.125;
	}
	if (map.getZoom() == 16) {
		$("#zoomMapLevel").html(" " + map.getZoom());
		raza = 78.125;
	}
	if (map.getZoom() == 17) {
		$("#zoomMapLevel").html(" " + map.getZoom());
		raza = 78.125;
	}
	if (map.getZoom() == 18) {
		$("#zoomMapLevel").html(" " + map.getZoom());
		raza = 78.125;
	}
	if (map.getZoom() == 19) {
		$("#zoomMapLevel").html(" " + map.getZoom());
		raza = 78.125;
	}
	if (map.getZoom() == 20) {
		$("#zoomMapLevel").html(" " + map.getZoom());
		raza = 78.125;
	}
	if (map.getZoom() == 21) {
		$("#zoomMapLevel").html(" " + map.getZoom());
		raza = 78.125;
	}
	// max 21 zoom levels

	var circle = new google.maps.Circle( {
		map : map,
		strokeWeight : 2,
		fillOpacity : 0.6,
		center : center,
		//inverse projectiong scale
		radius : raza * map.getProjection().fromLatLngToPoint(new google.maps.LatLng(Math.abs(center.lat()), center.lng())).y / 100,
		fillColor : color,
		zIndex : 1
	});

	circles.push(circle);

	circles[circles.length - 1].setMap(map);
}
function lat2y(a) {
	return 180 / Math.PI * Math.log(Math.tan(Math.PI / 4 + a * (Math.PI / 180) / 2));
}
RadiusWidget.prototype = new google.maps.MVCObject();
function cosh(arg) {
	// Returns the hyperbolic cosine of the number, defined as (exp(number) + exp(-number))/2
	//
	// version: 1109.2015
	// discuss at: http://phpjs.org/functions/cosh    // +   original by: Onno Marsman
	// *     example 1: cosh(-0.18127180117607017);
	// *     returns 1: 1.0164747716114113
	return (Math.exp(arg) + Math.exp(-arg)) / 2;
}
function sinh(arg) {
	// Returns the hyperbolic sine of the number, defined as (exp(number) - exp(-number))/2
	//
	// version: 1109.2015
	// discuss at: http://phpjs.org/functions/sinh    // +   original by: Onno Marsman
	// *     example 1: sinh(-0.9834330348825909);
	// *     returns 1: -1.1497971402636502
	return (Math.exp(arg) - Math.exp(-arg)) / 2;
}

function removeCircles() {
	//alert("removeCircles()");
	if (circles.length != 0) {
		for ( var i = 0; i < circles.length; i++) {
			circles[i].setMap(null);
		}
	}
	circles = new Array();
}
