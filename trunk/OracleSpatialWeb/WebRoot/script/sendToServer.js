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
//			$("#legendInfo").html("<span>number of pictures </span><br/> <span> <img src='images/circle_bl.ico' width='20px' height='20px'/> 1-99 </span> <br/><span> <img src='images/circle_gr.ico' width='20px' height='20px' /> 100-999</span><br/><span> <img src='images/circle_lgr.ico' width='20px' height='20px'/> 1000-9999</span> <br/><span> <img src='images/circle_or.ico' width='20px' height='20px'/> > 10000</span>");
		}
	};

	xmlHttp.open("POST", "SpatialXml");
	// xmlHttp.open("POST",
	// "http://localhost:8080/OracleSpatialWeb/RequestKml");

	xmlHttp.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
	xmlHttp.send("xml=" + encodeURIComponent(textToSend) + "&timestamp=" + new Date().getTime());
	//alert("calendar"+textToSend);
}

// function sendToServer_ScreenCenter_ScreenBounds(headerXML, bodyXML) {
//
// 	var screenBounds_screenCenter = headerXML + "" + bodyXML;
// 	screenBounds_screenCenter = "<request>" + screenBounds_screenCenter+ "</request>";
// 	$("#legendInfo").html("");
// 	if (window.XMLHttpRequest) {
// 		xmlHttp = new XMLHttpRequest();
// 	} else if (window.ActiveXObject) {
// 		xmlHttp = new ActiveXObject("Microsoft.XMLHTTP");
// 	} else {
// 		document.write("browser not supported");
// 	}
// 	xmlHttp.onreadystatechange = function() {
// 		if (xmlHttp.readyState == 4) {
// 			var xmlDoc = xmlHttp.responseText;
// 			if (map.getZoom() < 8) {
// 				$("#scaleLevel").html("1");
// 				loadXml(xmlDoc);
// 			}
// 			if ((map.getZoom() < 10) && (map.getZoom() >= 8)) {
// 				$("#scaleLevel").html("2");
// 				loadXml(xmlDoc);
// 			}
// 			if ((map.getZoom() < 11) && (map.getZoom() >= 10)) {
// 				$("#scaleLevel").html("3");
// 				loadXml(xmlDoc);
// 			}
// 			if ((map.getZoom() < 12) && (map.getZoom() >= 11)) {
// 				$("#scaleLevel").html("4");
// 				loadXml(xmlDoc);
// 			}
// 			if (map.getZoom() >= 12) {
// 				$("#scaleLevel").html("5");
// 				loadXml(xmlDoc);
// 			}
// 		}
// 	};
// 	xmlHttp.open("POST", "SpatialXml");
// 	xmlHttp.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
// 	xmlHttp.send("persist=true&xml="+ encodeURIComponent(screenBounds_screenCenter)+ "&timestamp=" + new Date().getTime());
// }

function sendToServerFromCarousel(ids, page_size, page) {
	if (window.XMLHttpRequest) {
		xmlHttp = new XMLHttpRequest();
	} else if (window.ActiveXObject) {
		xmlHttp = new ActiveXObject("Microsoft.XMLHTTP");
	} else {
		document.write("browser not supported");
	}
	xmlHttp.open("POST", "SmallPhotoUrl");
	xmlHttp.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
	xmlHttp.send("areaid=" + ids + "&page_size=" + page_size + "&page=" + page);
	xmlHttp.onreadystatechange = function() {
		if (xmlHttp.readyState == 4) {
			var xmlDoc = xmlHttp.responseText;
			carouselLoadPictures(xmlDoc);
		}
	};
}

function sendToServerFromTagCarousel(ids, page_size, page, tag, queryDateStr) {
	if (window.XMLHttpRequest) {
		xmlHttp = new XMLHttpRequest();
	} else if (window.ActiveXObject) {
		xmlHttp = new ActiveXObject("Microsoft.XMLHTTP");
	} else {
		document.write("browser not supported");
	}
	xmlHttp.open("POST", "SmallPhotoUrl");
	xmlHttp.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
	xmlHttp.send("areaid=" + ids + "&tag=" + tag + "&queryDateStr=" + queryDateStr + "&page_size=" + page_size + "&page=" + page);
	xmlHttp.onreadystatechange = function() {
		if (xmlHttp.readyState == 4) {
			var xmlDoc = xmlHttp.responseText;
			carouselLoadPictures(xmlDoc);
		}
	};
}
//, total, selectedP, infowindow
/*
function askForTags(ids, numberOfTags, center, total, selc, number){
      if (window.XMLHttpRequest) {
		xmlHttp = new XMLHttpRequest();
	} else if (window.ActiveXObject) {
		xmlHttp = new ActiveXObject("Microsoft.XMLHTTP");
	} else {
		document.write("browser not supported");
	}
	xmlHttp.open("POST", "Tag");
	xmlHttp.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
	//console.log("areaid=" + ids + "&size=" + numberOfTags);
	xmlHttp.send("areaid=" + ids + "&size=" + numberOfTags+ "&timestamp=" + new Date().getTime());
	xmlHttp.onreadystatechange = function() {
		if (xmlHttp.readyState == 4) {
			var xmlDoc = xmlHttp.responseText;
			loadTags(ids, xmlDoc, center, total, selc, eval(number));
		}
	};
}
*/

function sendToServerCalendarDataHistogram(headerXMLHistogram, bodyXMLHistogram) {
	var textToSend = headerXMLHistogram + "" + bodyXMLHistogram;
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
			var xmlDoc = xmlHttp.responseText;
			loadHistogram(xmlDoc);
		}
	};
	$("#legendInfo").empty().html('<img src="images/89.gif" height="50" width="50"  />');
	$("#parent1").empty().html('<img src="images/89.gif" height="50" width="50"  />');
	$("#parent2").empty().html('<img src="images/89.gif" height="50" width="50"  />');
	$("#parent3").empty().html('<img src="images/89.gif" height="50" width="50"  />');
	$("#parent4").empty().html('<img src="images/89.gif" height="50" width="50"  />');
	$("#parent5").empty().html('<img src="images/89.gif" height="50" width="50"  />');
	xmlHttp.open("POST", "HistrogramsData");
	xmlHttp.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
	xmlHttp.send("xml=" + encodeURIComponent(textToSend) + "&timestamp=" + new Date().getTime());
}