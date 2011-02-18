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
	//var xmlHttp = createXMLHttpRequest();

	xmlHttp.onreadystatechange = function() {
		if (xmlHttp.readyState == 4) {
			var xmlDoc = xmlHttp.responseText;
			//alert("message: "+xmlDoc);
			loadXml(xmlDoc);
		}
	};

	xmlHttp.open("POST", "SpatialXml");
//	xmlHttp.open("POST", "http://localhost:8080/OracleSpatialWeb/RequestKml");

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
			//alert("blaaaaaaaaaaaaa");
			var xmlDoc = xmlHttp.responseText;
			//alert("message: "+xmlDoc);
			//alert("message: "+screenBounds_screenCenter)
			if (map.getZoom() < 8) {
				//alert(map.getZoom()+" booleanSelected "+boolSelected[number]+ " globalPolygonSelected: "+globalPolygonSelected+" polygon Id "+id[globalPolygonSelected]);
				loadXml(xmlDoc);
			}
			if ((map.getZoom() < 10) && (map.getZoom() >= 8)) {
				//alert(map.getZoom()+" booleanSelected "+boolSelected[number]+ " globalPolygonSelected: "+globalPolygonSelected+" polygon Id "+id[globalPolygonSelected]);
				loadXml(xmlDoc);
			}
			if ((map.getZoom() < 11) && (map.getZoom() >= 10)) {
				//alert(map.getZoom()+" booleanSelected "+boolSelected[number]+ " globalPolygonSelected: "+globalPolygonSelected+" polygon Id "+id[globalPolygonSelected]);
				loadXml(xmlDoc);
			}
			if ((map.getZoom() < 12) && (map.getZoom() >= 11)) {
				//alert(map.getZoom()+" booleanSelected "+boolSelected[number]+ " globalPolygonSelected: "+globalPolygonSelected+" polygon Id "+id[globalPolygonSelected]);
				loadXml(xmlDoc);
			}
			if (map.getZoom() >= 12) {
				//alert(map.getZoom()+" booleanSelected "+boolSelected[number]+ " globalPolygonSelected: "+globalPolygonSelected+" polygon Id "+id[globalPolygonSelected]);
				loadXml(xmlDoc);
			}
			$("#legendInfo").html("screenBounds:" + screenBounds_screenCenter);
		}
	};
	xmlHttp.open("POST", "SpatialXml");
	xmlHttp.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
	xmlHttp.send("persist=true&xml=" + encodeURIComponent(screenBounds_screenCenter));
}

function sendToServerFromCarousel(ids, page_size, page){
   // alert("areaid="+ids+" page_size="+page_size+" page="+page);
    if (window.XMLHttpRequest) {
		xmlHttp = new XMLHttpRequest();
	} else if (window.ActiveXObject) {
		xmlHttp = new ActiveXObject("Microsoft.XMLHTTP");
	} else {
		document.write("browser not supported");
	}
    xmlHttp.open("POST", "SmallPhotoUrl");
    xmlHttp.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
    xmlHttp.send("areaid="+ids+"&page_size="+page_size+"&page="+page);
    xmlHttp.onreadystatechange = function() {
		if (xmlHttp.readyState == 4) {
			var xmlDoc = xmlHttp.responseText;
			carouselLoadPictures(xmlDoc);
		}
	};
}
