var contorArray = 0;
var globalArray = new Array(90);
var datafromXml = [];

var yearMax;
var monthMax;
var daysMax;
var hourMax;
var weekDayMax;

var globalDataY = [];
var maxY = 1;
var globalDataM = [];
var maxM = 1;
var globalDataD = [];
var maxD = 1;
var globalDataH = [];
var maxH = 1;
var globalDataW = [];
var maxW = 1;
var ApplyPolygons = false;
var YEAR = true;
var once = true;

function setArray(param) {
	if ((param == 7) && (YEAR == true)) {
		var myArray = [];
		var dataArray = [];
		var dataRepresentation = [];
		if (YEAR) {
			YEAR = false;
			for ( var i = 0; i < param; i++) {
				// globalDataY[i] = Math.floor(Math.random()*500000);
				dataArray[i] = globalDataY[i]; // data extraction
				// if (maxY<globalDataY[i]){maxY = globalDataY[i];}
				// console.log(globalDataY[i]+" "+i)
			}
			for ( var i = 0; i < param; i++) {
				dataRepresentation[i] = (50 * dataArray[i]) / maxY;
			}
			for ( var i = 0; i < param; i++) {
				var object_name = 'Object' + i;
				object_name = {
					label : 0,
					indexVal : contorArray,
					value2 : dataArray[i],
					value : dataRepresentation[i]
				};
				myArray[i] = object_name;
				if (globalArray[contorArray]!=1){
				  globalArray[contorArray] = 0;
				  contorArray++;
				}else{
				    contorArray++;
				}
			}

		}
		return myArray;
	}
	if (param == 12) {
		var myArray = [];
		var dataArray = [];
		var dataRepresentation = [];

		for ( var i = 0; i < param; i++) {
			// globalDataM[i] = Math.floor(Math.random()*500000);
			dataArray[i] = globalDataM[i]; // data extraction
			// if (maxM<globalDataM[i]){maxM = globalDataM[i];}
			// console.log(globalDataM[i]+" "+i)
		}
		for ( var i = 0; i < param; i++) {
			dataRepresentation[i] = (50 * dataArray[i]) / maxM;
		}
		for ( var i = 0; i < param; i++) {
			var object_name = 'Object' + i;
			object_name = {
				label : 0,
				indexVal : contorArray,
				value2 : dataArray[i],
				value : dataRepresentation[i]
			};

			myArray[i] = object_name;
				if (globalArray[contorArray]!=1){
				  globalArray[contorArray] = 0;
				  contorArray++;
				}else{
				    contorArray++;
				}
		}
		return myArray;
	}
	if (param == 31) {
		var myArray = [];
		var myArray = [];
		var dataArray = [];
		var dataRepresentation = [];

		for ( var i = 0; i < param; i++) {
			// globalDataD[i] = Math.floor(Math.random()*500000);
			dataArray[i] = globalDataD[i]; // data extraction
			// if (maxD<globalDataD[i]){maxD = globalDataD[i];}
			// console.log(globalDataD[i]+" "+i)
		}
		for ( var i = 0; i < param; i++) {
			dataRepresentation[i] = (50 * dataArray[i]) / maxD;
		}
		for ( var i = 0; i < param; i++) {
			var object_name = 'Object' + i;
			object_name = {
				label : 0,
				indexVal : contorArray,
				value2 : dataArray[i],
				value : dataRepresentation[i]
			};

			myArray[i] = object_name;
				if (globalArray[contorArray]!=1){
				  globalArray[contorArray] = 0;
				  contorArray++;
				}else{
				    contorArray++;
				}
		}
		return myArray;
	}
	if (param == 24) {
		var myArray = [];
		var myArray = [];
		var dataArray = [];
		var dataRepresentation = [];

		for ( var i = 0; i < param; i++) {
			// globalDataH[i] = Math.floor(Math.random()*500000);
			dataArray[i] = globalDataH[i]; // data extraction
			// if (maxH<globalDataH[i]){maxH = globalDataH[i];}
			// console.log(globalDataH[i]+" "+i)
		}
		for ( var i = 0; i < param; i++) {
			dataRepresentation[i] = (50 * dataArray[i]) / maxH;
		}
		for ( var i = 0; i < param; i++) {
			var object_name = 'Object' + i;
			object_name = {
				label : 0,
				indexVal : contorArray,
				value2 : dataArray[i],
				value : dataRepresentation[i]
			};

			myArray[i] = object_name;
				if (globalArray[contorArray]!=1){
				  globalArray[contorArray] = 0;
				  contorArray++;
				}else{
				    contorArray++;
				}
		}
		return myArray;
	}

	if ((param == 7) && (YEAR == false)) {
		var myArray = [];
		var myArray = [];
		var dataArray = [];
		var dataRepresentation = [];

		for ( var i = 0; i < param; i++) {
			// globalDataW[i] = Math.floor(Math.random()*500000);
			dataArray[i] = globalDataW[i]; // data extraction
			// if (maxW<globalDataW[i]){maxW = globalDataW[i];}
			// console.log(globalDataW[i]+" "+i)
		}
		for ( var i = 0; i < param; i++) {
			dataRepresentation[i] = (50 * dataArray[i]) / maxW;
		}
		for ( var i = 0; i < param; i++) {
			var object_name = 'Object' + i;
			object_name = {
				label : 0,
				indexVal : contorArray,
				value2 : dataArray[i],
				value : dataRepresentation[i]
			};
			myArray[i] = object_name;
				if (globalArray[contorArray]!=1){
				  globalArray[contorArray] = 0;
				  contorArray++;
				}else{
				    contorArray++;
				}
		}
		return myArray;
	}
}

function loadHistogram(xml) {
	var klocalYear = 0;
	var klocalMonth = 0;
	var klocalDay = 0;
	var klocalHour = 0;
	var klocalWeekDay = 0;
	$(xml).find('year').each(function() {
		globalDataY[klocalYear] = $(this).text();
		// console.log("years: "+globalDataY[klocalYear]+" "+klocalYear);
		klocalYear++;
	});
	$(xml).find('month').each(function() {
		globalDataM[klocalMonth] = $(this).text();
		// console.log("months: "+globalDataM[klocalMonth]+" "+klocalMonth);
		klocalMonth++;
	});
	$(xml).find('day').each(function() {
		globalDataD[klocalDay] = $(this).text();
		// console.log("days: "+globalDataD[klocalDay]+" "+klocalDay);
		klocalDay++;
	});
	$(xml).find('hour').each(function() {
		globalDataH[klocalHour] = $(this).text();
		// console.log("hours: "+globalDataH[klocalHour]+" "+klocalHour);
		klocalHour++;
	});
	$(xml).find('weekday').each(function() {
		globalDataW[klocalWeekDay] = $(this).text();
		// console.log("weekdays: "+globalDataW[klocalWeekDay]+"
		// "+klocalWeekDay);
		klocalWeekDay++;
	});

	maxY = $(xml).find('years').attr('maxValue');
	// console.log(maxY);
	maxM = $(xml).find('months').attr('maxValue');
	// console.log(maxM);
	maxD = $(xml).find('days').attr('maxValue');
	// console.log(maxD);
	maxH = $(xml).find('hours').attr('maxValue');
	// console.log(maxH);
	maxW = $(xml).find('weekdays').attr('maxValue');
	// console.log(maxW);
	init();
}

function init() {

	var opts1 = {
		color : "#DDDDDD",
		strokeColor : 'rgb(16,	78, 139)',
		tooltip : true,
		classWidth : '28.428571px',
		borderStyle : 'solid',
		borderWidth : '1px',
		verticalAlign : 'text-bottom'
	// fontSize : '12px'
	};

	var opts2 = {
		color : "#DDDDDD",
		strokeColor : 'rgb(16,	78, 139)',
		tooltip : true,
		classWidth : '16.166666px',
		borderStyle : 'solid',
		borderWidth : '1px',
		verticalAlign : 'text-bottom'
	// fontSize : '12px'
	};

	var opts3 = {
		color : "#DDDDDD",
		strokeColor : 'rgb(16,	78, 139)',
		tooltip : true,
		classWidth : '5.6451612px',
		borderStyle : 'solid',
		borderWidth : '1px',
		verticalAlign : 'text-bottom'
	// fontSize : '12px'
	};

	var opts4 = {
		color : "#DDDDDD",
		strokeColor : 'rgb(16,	78, 139)',
		tooltip : true,
		classWidth : '7.583333px',
		borderStyle : 'solid',
		borderWidth : '1px',
		verticalAlign : 'text-bottom'
	// fontSize : '12px'
	};

	var opts5 = {
		color : "#DDDDDD",
		strokeColor : 'rgb(16,	78, 139)',
		tooltip : true,
		classWidth : '28.428571px',
		borderStyle : 'solid',
		borderWidth : '1px',
		verticalAlign : 'text-bottom'
	// fontSize : '12px'
	};

	var myArray1 = setArray(7); // years
	var myArray2 = setArray(12); // months
	var myArray3 = setArray(31); // days
	var myArray4 = setArray(24);// hours
	var myArray5 = setArray(7);// weekdays

	var model1 = new HistogrammModel(myArray1, true, false, false, false, false);
	var model2 = new HistogrammModel(myArray2, false, true, false, false, false);
	var model3 = new HistogrammModel(myArray3, false, false, true, false, false);
	var model4 = new HistogrammModel(myArray4, false, false, false, true, false);
	var model5 = new HistogrammModel(myArray5, false, false, false, false, true);

	var view1 = new Histogramm(model1, opts1);
	view1.render(document.getElementById('parent1'), myArray1);

	var view2 = new Histogramm(model2, opts2);
	view2.render(document.getElementById('parent2'), myArray2);

	var view3 = new Histogramm(model3, opts3);
	view3.render(document.getElementById('parent3'), myArray3);

	var view4 = new Histogramm(model4, opts4);
	view4.render(document.getElementById('parent4'), myArray4);

	var view5 = new Histogramm(model5, opts5);
	view5.render(document.getElementById('parent5'), myArray5);
	// the good one
	if (ApplyPolygons == false) {
		ApplyPolygons = true;
		headerXMLHistogram = createHeaderXML();
		bodyXMLHistogram = timeController1XMLHistogram_Selected();
		//alert("sent for polygons "+bodyXMLHistogram);
		if (jQuery("#AggregationCheckBox").attr("checked") == true){
		  sendToServerCalendarData(headerXMLHistogram, bodyXMLHistogram);		
		}else{}
	}
}

function Histogramm(model, options) {
	this.model = model;
	this.options = options;
	this.color = this.options.color;
	this.strokeColor = this.options.strokeColor;
	this.tooltip = this.options.tooltip;
	this.classWidth = this.options.classWidth;
	this.borderStyle = this.options.borderStyle;
	this.borderWidth = this.options.borderWidth;
	this.verticalAlign = this.options.verticalAlign;
}

function activateToolTip(spanElement, subSpan, index, arrayX, classNumber) {
	spanElement.onmouseover = function() {	
		spanElement.style.background = '#FFFF00';
		subSpan.innerHTML = "#pictures:" + arrayX[index].value2;
		// spanElement.innerHTML = arrayX[index].value2;
	};
	spanElement.onmouseout = function() {
		if (arrayX[index].label == 0) {
			spanElement.style.background = "#DDDDDD";
		} else {
			spanElement.style.background = '#FFFF00';
		}		
		subSpan.innerHTML = "";
	};
	spanElement.onclick = function() {
		if (arrayX[index].label == 0) {
			spanElement.style.background = '#FFFF00';
			arrayX[index].label = 1;			
			globalArray[arrayX[index].indexVal] = 1;
			console.log(globalArray[arrayX[index].indexVal]+" "+arrayX[index].indexVal);
			if (once == true) {
				$("#histogramContent").append("<button id='button20' onclick='javascript:askHistogramSelected();'> Submit </button>");
				once = false;
			}
		} else {
			spanElement.style.background = "#DDDDDD";
			arrayX[index].label = 0;
			globalArray[arrayX[index].indexVal] = 0;
		}
	};

};
function askHistogramSelected() {
	addRemoveElementsFromHistogram();
	resetParameters();
	headerXMLHistogram = createHeaderXML();
	bodyXMLHistogram = timeController1XMLHistogram_Selected();
	sendToServerCalendarDataHistogram(headerXMLHistogram, bodyXMLHistogram);
	//sendToServerCalendarData(headerXMLHistogram, bodyXMLHistogram);
	for (var i=0; i<globalArray.length; i++){
	    console.log(globalArray[i]+" "+i);
	}
};

function resetParameters(){
	ApplyPolygons = false;
	YEAR = true;
	contorArray = 0;
	once = true;
};

function timeController1XMLHistogram_Selected() {
	var text1 = "";
	text1 = text1 + "<calendar>";
	// *********************************************************
	text1 = text1 + "<years>"; // first child // years
	for ( var i = 0; i <= 6; i++) { // in the beginning I have the years
		if (globalArray[i] == 1) {
			text1 = text1 + "<year>" + nameString[i + 1] + "</year>";
		}
	}
	text1 = text1 + "</years>";
	// *********************************************************
	text1 = text1 + "<months>";// second child // months
	for ( var i = 7; i <= 18; i++) { // in the beginning I have the months
		if (globalArray[i] == 1) {
			text1 = text1 + "<month>" + nameString[i + 1] + "</month>";
		}
	}
	text1 = text1 + "</months>";
	// *********************************************************
	text1 = text1 + "<days>";// third child // days
	for ( var i = 19; i <= 49; i++) { // in the beginning I have the days
		if (globalArray[i] == 1) {
			text1 = text1 + "<day>" + nameString[i + 1] + "</day>";
		}
	}
	text1 = text1 + "</days>";
	// *********************************************************
	text1 = text1 + "<hours>";
	for ( var i = 50; i <= 73; i++) {
		if (globalArray[i] == 1) {
			text1 = text1 + "<hour>" + nameString[i + 1] + "</hour>";
		}
	}
	text1 = text1 + "</hours>";
	// ********************************************************* for the day of
	// the week
	text1 = text1 + "<weekdays>";
	for ( var i = 74; i <= 80; i++) {
		if (globalArray[i] == 1) {
			text1 = text1 + "<weekday>" + nameString[i + 1] + "</weekday>";
		}
	}
	text1 = text1 + "</weekdays>";
	// *********************************************************
	text1 = text1 + "</calendar>";
	return text1;
}
Histogramm.prototype.render = function(parent, arrayX) {

	var classWidth = this.options.classWidth;
	var strokeColor = this.options.strokeColor;
	var color = this.options.color;
	// var title = this.options.title;
	var tooltip = this.options.tooltip;
	var borderStyle = this.options.borderStyle;
	var borderWidth = this.options.borderWidth;
	var verticalAlign = this.options.verticalAlign;

	var hystogrammDiv = document.createElement('div');
	var hystogrammDivInfo = document.createElement('div');
	// console.log(this.model.getNumberOfClasses());
	for ( var i = 0; i < this.model.getNumberOfClasses(); i++) {
		var spanElement = document.createElement('span');
		var spanElementId = 'spanElement';
		spanElement.id = spanElementId;
		spanElement.style.borderColor = strokeColor;
		spanElement.style.borderStyle = borderStyle;
		spanElement.style.borderBottomWidth = borderWidth;
		spanElement.style.borderTopWidth = borderWidth;
		// spanElement.style.display = "block";
		// spanElement.style.cssFloat = "left";

		spanElement.style.backgroundColor = color;
		if (arrayX[i].value2 != 0) {
			spanElement.style.padding = '0px ' + classWidth;
			spanElement.style.borderRightWidth = borderWidth;
			spanElement.style.borderLeftWidth = borderWidth;
		} else {
			spanElement.style.padding = '0px ' + classWidth;
			spanElement.style.borderRightWidth = borderWidth;
			spanElement.style.borderLeftWidth = borderWidth;
		}
		// spanElement.style.width = classWidth+"px";
		spanElement.style.verticalAlign = verticalAlign;
		// spanElement.innerHTML = "";
		// spanElement.style.fontSize = this.model.data[i].value + "px";
		spanElement.style.paddingTop = this.model.data[i].value + "px";

		var subSpan = document.createElement('span');
		spanElement.appendChild(subSpan);

		if (tooltip == true) {
			activateToolTip(spanElement, subSpan, i, arrayX, this.model
					.getNumberOfClasses());
		}
		hystogrammDiv.appendChild(spanElement);
	}
	parent.appendChild(hystogrammDiv);

	// console.log("this.model.getNumberOfClasses()
	// "+this.model.getNumberOfClasses());
	for ( var i = 0; i < this.model.getNumberOfClasses(); i++) {
		if ((this.model.getNumberOfClasses() == 7)
				&& (this.model.getIndexValueOfBool() == 1)) {
			// console.log('classWidth ' + classWidth);
			var spanElementInfo = document.createElement('div');
			var spanElementInfoId = 'spanElementInfo' + i;
			spanElementInfo.id = spanElementInfoId;
			spanElementInfo.style.borderColor = strokeColor;
			spanElementInfo.style.borderStyle = borderStyle;
			spanElementInfo.style.display = "block";
			spanElementInfo.style.cssFloat = "left";
			spanElementInfo.style.borderBottomWidth = borderWidth;
			spanElementInfo.style.borderTopWidth = borderWidth;
			spanElementInfo.style.borderRightWidth = "1px";
			spanElementInfo.style.borderLeftWidth = borderWidth;
			spanElementInfo.style.backgroundColor = color;
			// spanElementInfo.style.padding = '0px ' + '5%';
			// spanElementInfo.style.padding = '0px ' + classWidth;
			// spanElementInfo.style.width = classWidth+"px";
			spanElementInfo.style.verticalAlign = verticalAlign;
			switch (i) {
			case 0:
				spanElementInfo.innerHTML = '2005';
				spanElementInfo.style.width = "56.8571428px";// 25.714285px
				spanElementInfo.style.textAlign = "center";
				// spanElementInfo.style.padding = '0px ' + '5px';
				break;
			case 1:
				spanElementInfo.innerHTML = '2006';
				spanElementInfo.style.width = "56.8571428px";// 25.714285px
				spanElementInfo.style.textAlign = "center";
				// spanElementInfo.style.padding = '0px ' + '2%';
				break;
			case 2:
				spanElementInfo.innerHTML = '2007';
				spanElementInfo.style.width = "56.8571428px";// 25.714285px
				spanElementInfo.style.textAlign = "center";
				// spanElementInfo.style.padding = '0px ' + '2%';
				break;
			case 3:
				spanElementInfo.innerHTML = '2008';
				spanElementInfo.style.width = "56.8571428px";// 25.714285px
				spanElementInfo.style.textAlign = "center";
				// spanElementInfo.style.padding = '0px ' + '2%';
				break;
			case 4:
				spanElementInfo.innerHTML = '2009';
				spanElementInfo.style.width = "56.8571428px";// 25.714285px
				spanElementInfo.style.textAlign = "center";
				// spanElementInfo.style.padding = '0px ' + '2%';
				break;
			case 5:
				spanElementInfo.innerHTML = '2010';
				spanElementInfo.style.width = "56.8571428px";// 25.714285px
				spanElementInfo.style.textAlign = "center";
				// spanElementInfo.style.padding = '0px ' + '2%';
				break;
			case 6:
				spanElementInfo.innerHTML = '2011';
				spanElementInfo.style.width = "56.8571428px";// 25.714285px
				spanElementInfo.style.textAlign = "center";
				// spanElementInfo.style.padding = '0px ' +'2%';
				break;
			}
			// spanElementInfo.style.fontSize = this.model.data[i].value + "px";
			hystogrammDivInfo.appendChild(spanElementInfo);
		}
		if (this.model.getNumberOfClasses() == 12) {
			var spanElementInfo = document.createElement('div');
			var spanElementInfoId = 'spanElementInfo' + i;
			spanElementInfo.id = spanElementInfoId;
			spanElementInfo.style.borderColor = strokeColor;
			spanElementInfo.style.borderStyle = borderStyle;
			spanElementInfo.style.display = "block";
			spanElementInfo.style.cssFloat = "left";
			spanElementInfo.style.borderBottomWidth = borderWidth;
			spanElementInfo.style.borderTopWidth = borderWidth;
			spanElementInfo.style.borderRightWidth = "1px";
			spanElementInfo.style.borderLeftWidth = borderWidth;
			spanElementInfo.style.backgroundColor = color;
			spanElementInfo.style.verticalAlign = verticalAlign;
			switch (i) {
			case 0:
				spanElementInfo.innerHTML = 'Jan';
				// spanElementInfo.style.padding = '0px ' + '4.7px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "32.333332px";
				break;
			case 1:
				spanElementInfo.innerHTML = 'Feb';
				// spanElementInfo.style.padding = '0px ' + '3px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "32.333332px";
				break;
			case 2:
				spanElementInfo.innerHTML = 'Mar';
				// spanElementInfo.style.padding = '0px ' + '2.5px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "32.333332px";
				break;
			case 3:
				spanElementInfo.innerHTML = 'Apr';
				// spanElementInfo.style.padding = '0px ' + '3.5px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "32.333332px";
				break;
			case 4:
				spanElementInfo.innerHTML = 'May';
				// spanElementInfo.style.padding = '0px ' + '1px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "32.333332px";
				break;
			case 5:
				spanElementInfo.innerHTML = 'Jun';
				// spanElementInfo.style.padding = '0px ' + '4.5px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "32.333332px";
				break;
			case 6:
				spanElementInfo.innerHTML = 'Jul';
				// spanElementInfo.style.padding = '0px ' + '6.6px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "32.333332px";
				break;
			case 7:
				spanElementInfo.innerHTML = 'Aug';
				// spanElementInfo.style.padding = '0px ' + '2px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "32.333332px";
				break;
			case 8:
				spanElementInfo.innerHTML = 'Sep';
				// spanElementInfo.style.padding = '0px ' + '2.3px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "32.333332px";
				break;
			case 9:
				spanElementInfo.innerHTML = 'Oct';
				// spanElementInfo.style.padding = '0px ' + '3.5px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "32.333332px";
				break;
			case 10:
				spanElementInfo.innerHTML = 'Nov';
				// spanElementInfo.style.padding = '0px ' + '1.5px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "32.333332px";
				break;
			case 11:
				spanElementInfo.innerHTML = 'Dec';
				// spanElementInfo.style.padding = '0px ' + '2px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "32.333332px";
				break;

			}
			hystogrammDivInfo.appendChild(spanElementInfo);
		}

		if (this.model.getNumberOfClasses() == 31) {
			var spanElementInfo = document.createElement('div');
			var spanElementInfoId = 'spanElementInfo' + i;
			spanElementInfo.id = spanElementInfoId;
			spanElementInfo.style.borderColor = strokeColor;
			spanElementInfo.style.borderStyle = borderStyle;
			spanElementInfo.style.display = "block";
			spanElementInfo.style.cssFloat = "left";
			spanElementInfo.style.borderBottomWidth = borderWidth;
			spanElementInfo.style.borderTopWidth = borderWidth;
			spanElementInfo.style.borderRightWidth = "1px";
			spanElementInfo.style.borderLeftWidth = borderWidth;
			spanElementInfo.style.backgroundColor = color;
			spanElementInfo.style.verticalAlign = verticalAlign;
			switch (i) {
			case 0:
				spanElementInfo.innerHTML = '1';
				// spanElementInfo.style.padding = '0px ' + '2.5px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;
			case 1:
				spanElementInfo.innerHTML = '2';
				// spanElementInfo.style.padding = '0px ' + '2.5px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;
			case 2:
				spanElementInfo.innerHTML = '3';
				// spanElementInfo.style.padding = '0px ' + '2.5px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;
			case 3:
				spanElementInfo.innerHTML = '4';
				// spanElementInfo.style.padding = '0px ' + '2.5px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;
			case 4:
				spanElementInfo.innerHTML = '5';
				// spanElementInfo.style.padding = '0px ' + '2.5px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;
			case 5:
				spanElementInfo.innerHTML = '6';
				// spanElementInfo.style.padding = '0px ' + '2.5px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;
			case 6:
				spanElementInfo.innerHTML = '7';
				// spanElementInfo.style.padding = '0px ' + '2.5px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;
			case 7:
				spanElementInfo.innerHTML = '8';
				// spanElementInfo.style.padding = '0px ' + '2.5px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;
			case 8:
				spanElementInfo.innerHTML = '9';
				// spanElementInfo.style.padding = '0px ' + '2.5px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;
			case 9:
				spanElementInfo.innerHTML = '10';
				// spanElementInfo.style.padding = '0px ' + '0px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;
			case 10:
				spanElementInfo.innerHTML = '11';
				// spanElementInfo.style.padding = '0px ' + '0px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;
			case 11:
				spanElementInfo.innerHTML = '12';
				// spanElementInfo.style.padding = '0px ' + '0px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;
			case 12:
				spanElementInfo.innerHTML = '13';
				// spanElementInfo.style.padding = '0px ' + '0px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;
			case 13:
				spanElementInfo.innerHTML = '14';
				// spanElementInfo.style.padding = '0px ' + '0px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;
			case 14:
				spanElementInfo.innerHTML = '15';
				// spanElementInfo.style.padding = '0px ' + '0px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;
			case 15:
				spanElementInfo.innerHTML = '16';
				// spanElementInfo.style.padding = '0px ' + '0px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;
			case 16:
				spanElementInfo.innerHTML = '17';
				// spanElementInfo.style.padding = '0px ' + '0px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;
			case 17:
				spanElementInfo.innerHTML = '18';
				// spanElementInfo.style.padding = '0px ' + '0px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;
			case 18:
				spanElementInfo.innerHTML = '19';
				// spanElementInfo.style.padding = '0px ' + '0px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;
			case 19:
				spanElementInfo.innerHTML = '20';
				// spanElementInfo.style.padding = '0px ' + '0px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;
			case 20:
				spanElementInfo.innerHTML = '21';
				// spanElementInfo.style.padding = '0px ' + '0px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;
			case 21:
				spanElementInfo.innerHTML = '22';
				// spanElementInfo.style.padding = '0px ' + '0px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;
			case 22:
				spanElementInfo.innerHTML = '23';
				// spanElementInfo.style.padding = '0px ' + '0px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;
			case 23:
				spanElementInfo.innerHTML = '24';
				// spanElementInfo.style.padding = '0px ' + '0px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;
			case 24:
				spanElementInfo.innerHTML = '25';
				// spanElementInfo.style.padding = '0px ' + '0px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;
			case 25:
				spanElementInfo.innerHTML = '26';
				// spanElementInfo.style.padding = '0px ' + '0px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;
			case 26:
				spanElementInfo.innerHTML = '27';
				// spanElementInfo.style.padding = '0px ' + '0px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;
			case 27:
				spanElementInfo.innerHTML = '28';
				// spanElementInfo.style.padding = '0px ' + '0px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;
			case 28:
				spanElementInfo.innerHTML = '28';
				// spanElementInfo.style.padding = '0px ' + '0px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;
			case 29:
				spanElementInfo.innerHTML = '30';
				// spanElementInfo.style.padding = '0px ' + '0px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;
			case 30:
				spanElementInfo.innerHTML = '31';
				// spanElementInfo.style.padding = '0px ' + '0px';
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.width = "11.290322px";
				spanElementInfo.style.fontSize = "8.5px";
				break;

			}
			hystogrammDivInfo.appendChild(spanElementInfo);
		}

		if (this.model.getNumberOfClasses() == 24) {
			var spanElementInfo = document.createElement('div');
			var spanElementInfoId = 'spanElementInfo' + i;
			spanElementInfo.id = spanElementInfoId;
			spanElementInfo.style.borderColor = strokeColor;
			spanElementInfo.style.borderStyle = borderStyle;
			spanElementInfo.style.display = "block";
			spanElementInfo.style.cssFloat = "left";
			spanElementInfo.style.borderBottomWidth = borderWidth;
			spanElementInfo.style.borderTopWidth = borderWidth;
			spanElementInfo.style.borderRightWidth = "1px";
			spanElementInfo.style.borderLeftWidth = borderWidth;
			spanElementInfo.style.backgroundColor = color;
			spanElementInfo.style.verticalAlign = verticalAlign;
			switch (i) {
			case 0:
				spanElementInfo.innerHTML = '0';
				spanElementInfo.style.width = "15.166666px";
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.fontSize = "10px";
				break;
			case 1:
				spanElementInfo.innerHTML = '1';
				spanElementInfo.style.width = "15.166666px";
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.fontSize = "10px";
				break;
			case 2:
				spanElementInfo.innerHTML = '2';
				spanElementInfo.style.width = "15.166666px";
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.fontSize = "10px";
				break;
			case 3:
				spanElementInfo.innerHTML = '3';
				spanElementInfo.style.width = "15.166666px";
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.fontSize = "10px";
				break;
			case 4:
				spanElementInfo.innerHTML = '4';
				spanElementInfo.style.width = "15.166666px";
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.fontSize = "10px";
				break;
			case 5:
				spanElementInfo.innerHTML = '5';
				spanElementInfo.style.width = "15.166666px";
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.fontSize = "10px";
				break;
			case 6:
				spanElementInfo.innerHTML = '6';
				spanElementInfo.style.width = "15.166666px";
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.fontSize = "10px";
				break;
			case 7:
				spanElementInfo.innerHTML = '7';
				spanElementInfo.style.width = "15.166666px";
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.fontSize = "10px";
				break;
			case 8:
				spanElementInfo.innerHTML = '8';
				spanElementInfo.style.width = "15.166666px";
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.fontSize = "10px";
				break;
			case 9:
				spanElementInfo.innerHTML = '9';
				spanElementInfo.style.width = "15.166666px";
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.fontSize = "10px";
				break;
			case 10:
				spanElementInfo.innerHTML = '10';
				spanElementInfo.style.width = "15.166666px";
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.fontSize = "10px";
				break;
			case 11:
				spanElementInfo.innerHTML = '11';
				spanElementInfo.style.width = "15.166666px";
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.fontSize = "10px";
				break;
			case 12:
				spanElementInfo.innerHTML = '12';
				spanElementInfo.style.width = "15.166666px";
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.fontSize = "10px";
				break;
			case 13:
				spanElementInfo.innerHTML = '13';
				spanElementInfo.style.width = "15.166666px";
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.fontSize = "10px";
				break;
			case 14:
				spanElementInfo.innerHTML = '14';
				spanElementInfo.style.width = "15.166666px";
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.fontSize = "10px";
				break;
			case 15:
				spanElementInfo.innerHTML = '15';
				spanElementInfo.style.width = "15.166666px";
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.fontSize = "10px";
				break;
			case 16:
				spanElementInfo.innerHTML = '16';
				spanElementInfo.style.width = "15.166666px";
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.fontSize = "10px";
				break;
			case 17:
				spanElementInfo.innerHTML = '17';
				spanElementInfo.style.width = "15.166666px";
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.fontSize = "10px";
				break;
			case 18:
				spanElementInfo.innerHTML = '18';
				spanElementInfo.style.width = "15.166666px";
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.fontSize = "10px";
				break;
			case 19:
				spanElementInfo.innerHTML = '19';
				spanElementInfo.style.width = "15.166666px";
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.fontSize = "10px";
				break;
			case 20:
				spanElementInfo.innerHTML = '20';
				spanElementInfo.style.width = "15.166666px";
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.fontSize = "10px";
				break;
			case 21:
				spanElementInfo.innerHTML = '21';
				spanElementInfo.style.width = "15.166666px";
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.fontSize = "10px";
				break;
			case 22:
				spanElementInfo.innerHTML = '22';
				spanElementInfo.style.width = "15.166666px";
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.fontSize = "10px";
				break;
			case 23:
				spanElementInfo.innerHTML = '23';
				spanElementInfo.style.width = "15.166666px";
				spanElementInfo.style.textAlign = "center";
				spanElementInfo.style.fontSize = "10px";
				break;
			}
			hystogrammDivInfo.appendChild(spanElementInfo);

		}
		if ((this.model.getNumberOfClasses() == 7)
				&& (this.model.getIndexValueOfBool() == 5)) {
			// console.log('classWidth ' + classWidth);
			var spanElementInfo = document.createElement('div');
			var spanElementInfoId = 'spanElementInfo' + i;
			spanElementInfo.id = spanElementInfoId;
			spanElementInfo.style.borderColor = strokeColor;
			spanElementInfo.style.borderStyle = borderStyle;
			spanElementInfo.style.display = "block";
			spanElementInfo.style.cssFloat = "left";
			spanElementInfo.style.borderBottomWidth = borderWidth;
			spanElementInfo.style.borderTopWidth = borderWidth;
			spanElementInfo.style.borderRightWidth = "1px";
			spanElementInfo.style.borderLeftWidth = borderWidth;
			spanElementInfo.style.backgroundColor = color;
			spanElementInfo.style.verticalAlign = verticalAlign;
			switch (i) {
			case 0:
				spanElementInfo.innerHTML = 'Sun';
				spanElementInfo.style.width = "56.8571428px";// 25.714285px
				spanElementInfo.style.textAlign = "center";
				break;

			case 1:
				spanElementInfo.innerHTML = 'Mon';
				spanElementInfo.style.width = "56.8571428px";
				spanElementInfo.style.textAlign = "center";
				break;
			case 2:
				spanElementInfo.innerHTML = 'Tue';
				spanElementInfo.style.width = "56.8571428px";
				spanElementInfo.style.textAlign = "center";
				break;
			case 3:
				spanElementInfo.innerHTML = 'Wed';
				spanElementInfo.style.width = "56.8571428px";
				spanElementInfo.style.textAlign = "center";
				break;
			case 4:
				spanElementInfo.innerHTML = 'Thu';
				spanElementInfo.style.width = "56.8571428px";
				spanElementInfo.style.textAlign = "center";
				break;
			case 5:
				spanElementInfo.innerHTML = 'Fri';
				spanElementInfo.style.width = "56.8571428px";
				spanElementInfo.style.textAlign = "center";
				break;
			case 6:
				spanElementInfo.innerHTML = 'Sat';
				spanElementInfo.style.width = "56.8571428px";
				spanElementInfo.style.textAlign = "center";
				break;
			}
			// spanElementInfo.style.fontSize = this.model.data[i].value + "px";
			hystogrammDivInfo.appendChild(spanElementInfo);
		}
	}
	parent.appendChild(hystogrammDivInfo);
};
function askHistogram() {
	// set the global array. We forget the last query if we change the map center/position
	for (var i=0;i<globalArray.length;i++){
	  globalArray[i]=0;
	}
	resetParameters();
	headerXMLHistogram = createHeaderXML();
	bodyXMLHistogram = timeController1XMLHistogram();
	sendToServerCalendarDataHistogram(headerXMLHistogram, bodyXMLHistogram);
}
// prepeareXMLforServer
function timeController1XMLHistogram() {
	var text1 = "";
	text1 = text1 + "<calendar>";
	// *********************************************************
	text1 = text1 + "<years>"; // first child // years
	for ( var i = 1; i <= 7; i++) { // in the beginning I have the years
		text1 = text1 + "<year>" + nameString[i] + "</year>";
	}
	text1 = text1 + "</years>";
	// *********************************************************
	text1 = text1 + "<months>";// second child // months
	for ( var i = 8; i <= 19; i++) { // in the beginning I have the months
		text1 = text1 + "<month>" + nameString[i] + "</month>";
	}
	text1 = text1 + "</months>";
	// *********************************************************
	text1 = text1 + "<days>";// third child // days
	for ( var i = 20; i <= 50; i++) { // in the beginning I have the days
		text1 = text1 + "<day>" + nameString[i] + "</day>";
	}
	text1 = text1 + "</days>";
	// *********************************************************
	text1 = text1 + "<hours>";
	for ( var i = 51; i <= 74; i++) {
		text1 = text1 + "<hour>" + nameString[i] + "</hour>";
	}
	text1 = text1 + "</hours>";
	// ********************************************************* for the day of
	// the week
	text1 = text1 + "<weekdays>";
	for ( var i = 75; i <= 81; i++) {
		text1 = text1 + "<weekday>" + nameString[i] + "</weekday>";
	}
	text1 = text1 + "</weekdays>";
	// *********************************************************
	text1 = text1 + "</calendar>";
	return text1;
}
// sendToServer.js
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
	// var xmlHttp = createXMLHttpRequest();

	xmlHttp.onreadystatechange = function() {
		if (xmlHttp.readyState == 4) {
			var xmlDoc = xmlHttp.responseText;
			// alert("histogram response: "+xmlDoc);
			loadHistogram(xmlDoc);
		}
	};

	xmlHttp
			.open("POST",
					"http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/HistrogramsData");

	xmlHttp.setRequestHeader('Content-Type',
			'application/x-www-form-urlencoded; charset=UTF-8');
	xmlHttp.send("xml=" + encodeURIComponent(textToSend));
	//alert("histogram"+textToSend);
}
