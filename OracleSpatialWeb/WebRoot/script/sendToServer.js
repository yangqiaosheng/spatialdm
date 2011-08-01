function sendToServerCalendarData(headerXML, bodyXML) {
	//alert("send to Server");
	var textToSend = headerXML + "" + bodyXML;
	textToSend = "<request>" + textToSend + "</request>";
	if (window.XMLHttpRequest) {
		xmlHttp = new XMLHttpRequest();
	} else if (window.ActiveXObject) {
		xmlHttp = new ActiveXObject("Microsoft.XMLHTTP");
	} else {
		document.write("browser not supported");
	}
	// var xmlHttp = createXMLHttpRequest();

	xmlHttp.onreadystatechange = function() {
		if (xmlHttp.readyState == 4) {
			var xmlDoc = xmlHttp.responseText;
			//alert("calendar answer: "+xmlDoc);
			loadXml(xmlDoc);
		}
	};

//	xmlHttp.open("POST", "http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/SpatialXml");
	xmlHttp.open("POST", "SpatialXml");

	xmlHttp.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
	xmlHttp.send("xml=" + encodeURIComponent(textToSend)+ "&timestamp=" + new Date().getTime());
	//alert("calendar"+textToSend);
}

function sendToServer_ScreenCenter_ScreenBounds(headerXML, bodyXML) {

	var screenBounds_screenCenter = headerXML + "" + bodyXML;
	screenBounds_screenCenter = "<request>" + screenBounds_screenCenter
			+ "</request>";
	if (window.XMLHttpRequest) {
		xmlHttp = new XMLHttpRequest();
	} else if (window.ActiveXObject) {
		xmlHttp = new ActiveXObject("Microsoft.XMLHTTP");
	} else {
		document.write("browser not supported");
	}
	xmlHttp.onreadystatechange = function() {
		if (xmlHttp.readyState == 4) {
			var xmlDoc = xmlHttp.responseText;
			if (map.getZoom() < 8) {
				$("#scaleLevel").html("1");
				loadXml(xmlDoc);
			}
			if ((map.getZoom() < 10) && (map.getZoom() >= 8)) {
				$("#scaleLevel").html("2");
				loadXml(xmlDoc);
			}
			if ((map.getZoom() < 11) && (map.getZoom() >= 10)) {
				$("#scaleLevel").html("3");
				loadXml(xmlDoc);
			}
			if ((map.getZoom() < 12) && (map.getZoom() >= 11)) {
				$("#scaleLevel").html("4");
				loadXml(xmlDoc);
			}
			if (map.getZoom() >= 12) {
				$("#scaleLevel").html("5");
				loadXml(xmlDoc);
			}
		}
	};
//	xmlHttp.open("POST", "http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/SpatialXml");
	xmlHttp.open("POST", "SpatialXml");
	xmlHttp.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
	xmlHttp.send("persist=true&xml="+ encodeURIComponent(screenBounds_screenCenter) + "&timestamp=" + new Date().getTime());
}

function sendToServerFromCarousel(ids, page_size, page) {
	// alert("areaid="+ids+" page_size="+page_size+" page="+page);
	if (window.XMLHttpRequest) {
		xmlHttp = new XMLHttpRequest();
	} else if (window.ActiveXObject) {
		xmlHttp = new ActiveXObject("Microsoft.XMLHTTP");
	} else {
		document.write("browser not supported");
	}
//	xmlHttp.open("POST","http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/SmallPhotoUrl");
	xmlHttp.open("POST","SmallPhotoUrl");
	xmlHttp.setRequestHeader('Content-Type',
			'application/x-www-form-urlencoded; charset=UTF-8');
	xmlHttp.send("areaid=" + ids + "&page_size=" + page_size + "&page=" + page);
	xmlHttp.onreadystatechange = function() {
		if (xmlHttp.readyState == 4) {
			var xmlDoc = xmlHttp.responseText;
			carouselLoadPictures(xmlDoc);
		}
	};
}
