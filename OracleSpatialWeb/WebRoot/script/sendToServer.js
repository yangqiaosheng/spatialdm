function sendToServerCalendarData(headerXML, bodyXML) {
	var textToSend = headerXML + "" + bodyXML;
	textToSend = "<request>" + textToSend + "</request>";
	if (window.XMLHttpRequest) {
		xmlHttp = new XMLHttpRequest();
	} else if (window.ActiveXObject) {
		xmlHttp = new ActiveXObject("Microsoft.XMLHTTP");
	} else {
		document.write("browser not supported");
	}
	xmlHttp.onreadystatechange = function() {
		if (xmlHttp.readyState == 4) {
			url = xmlHttp.responseXML.getElementsByTagName("url")[0].firstChild.nodeValue;
			loadkml(url);
		}
	};
//	xmlHttp.open("POST", "http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/RequestKml");
	xmlHttp.open("POST", "http://localhost:8080/OracleSpatialWeb/RequestKml");

	xmlHttp.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
	xmlHttp.send("xml=" + encodeURIComponent(textToSend));
}
function sendToServer_ScreenCenter_ScreenBounds(headerXML, bodyXML) {
	var screenBounds_screenCenter = headerXML + "" + bodyXML;
	screenBounds_screenCenter = "<request>" + screenBounds_screenCenter + "</request>";
	if (window.XMLHttpRequest) {
		xmlHttp = new XMLHttpRequest();
	} else if (window.ActiveXObject) {
		xmlHttp = new ActiveXObject("Microsoft.XMLHTTP");
	} else {
		document.write("browser not supported");
	}
	xmlHttp.onreadystatechange = function() {
		if (xmlHttp.readyState == 4) {
			url = xmlHttp.responseXML.getElementsByTagName("url")[0].firstChild.nodeValue;
			if (map.getZoom() < 8) {
//		refreshButtons();
				loadkml(url);
				$("#item1").append("\n 0 8)");
				$("#item2").append("\n" + map.getZoom());
			}
			if ((map.getZoom() < 10) && (map.getZoom() >= 8)) {
//		refreshButtons();
				loadkml(url);
				$("#item1").append("\n [8,10)");
				$("#item2").append("\n" + map.getZoom());
			}
			if ((map.getZoom() < 11) && (map.getZoom() >= 10)) {
//		refreshButtons();
				loadkml(url);
				$("#item1").append("\n [10,11)");
				$("#item2").append("\n" + map.getZoom());
			}
			if ((map.getZoom() < 12) && (map.getZoom() >= 11)) {
//		refreshButtons();
				loadkml(url);
				$("#item1").append("\n [11,12)");
				$("#item2").append("\n" + map.getZoom());
			}
			if (map.getZoom() >= 12) {
//		refreshButtons();
				loadkml(url);
				$("#item1").append("\n [12..)");
				$("#item2").append("\n" + map.getZoom());
			}
			$("#legendInfo").html("screenBounds:" + screenBounds_screenCenter);
		}
	};
	xmlHttp.open("POST", "http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/RequestKml")
	xmlHttp.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
	xmlHttp.send("persist=true&xml=" + encodeURIComponent(screenBounds_screenCenter));
}
