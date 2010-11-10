function createHeaderXML(bounds, center, zoomLevel){
  var localXmlheader = "";
  localXmlheader= "<screen>";
  localXmlheader= localXmlheader+"<bounds>"+bounds+"</bounds><center>"+center+"</center><zoom>"+zoomLevel+"</zoom>";
  localXmlheader= localXmlheader+"</screen>";
  return localXmlheader;
}
function individualPolygonXml(text1){
  var localXml = "";
  localXml = "<polygon>"+text1+"</polygon>";
  return localXml;
}
function intervalXML(textPopCalendar){
  var localXml = "";
  localXml = "<interval>"+textPopCalendar+"</interval>";
  return localXml;
}
function selectedDaysXML(selectedResult){
  var localXml = "";
  localXml = "<selected_days>"+selectedResult+"</selected_days>";
  return localXml;
}
function timeController1XML(text1){
	text1=text1+"<calendar>";
	//*********************************************************
	text1=text1+"<years>"; // first child // years
	for (var i =1; i<=6; i++){	// in the beginning I have the years	
		if (number[i] % 2 != 0) { // I take the selections
			text1=text1+"<year>"+nameString[i]+"</year>";
		}
	}	
	text1=text1+"</years>";	
	//*********************************************************
	text1=text1+"<months>";// second child // months
	for (var i =7; i<=18; i++){	// in the beginning I have the months	
		if (number[i] % 2 != 0) { // I take the selections
			text1=text1+"<month>"+nameString[i]+"</month>";
		}
	}	
	text1=text1+"</months>";
	//*********************************************************
	text1=text1+"<days>";// third child // days
	for (var i =19; i<=49; i++){	// in the beginning I have the days	
		if (number[i] % 2 != 0) { // I take the selections
			text1=text1+"<day>"+nameString[i]+"</day>";
		}
	}	
	text1=text1+"</days>";		
	//*********************************************************
	text1=text1+"<hours>";
	for (var i =50; i<=73; i++){	
		if (number[i] % 2 != 0) { 
			text1=text1+"<hour>"+nameString[i]+"</hour>";
		}
	}
	text1=text1+"</hours>";
	//********************************************************* for the day of the week
	text1=text1+"<weekdays>";
	for (var i =74; i<=80; i++){	
		if (number[i] % 2 != 0) { 
			text1=text1+"<weekday>"+nameString[i]+"</weekday>";
		}
	}
	text1=text1+"</weekdays>";
	//*********************************************************
	
	text1=text1+"</calendar>";
	return text1;
	
}