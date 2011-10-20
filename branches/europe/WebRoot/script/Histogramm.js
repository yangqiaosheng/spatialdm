var contorArray = 0;
var globalArray = new Array(90);
var datafromXml = [];

var yearMax;
var monthMax;
var daysMax;
var hourMax;
var weekDayMax;

// because we can't devide with 0: 
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
var globId = 0;
var ApplyPolygons = false;
var YEAR = true;
var once = true;



var myArray1;
var myArray2;
var myArray3;
var myArray4;
var myArray5;

var globalYear = [];
var spanElementInfo = [];
var spanElement = [];
var globalMonth = [];
var globalDay =[];
var globalHour =[];
var globalWeek = [];




var classWidth;
var strokeColor;
var color;
var tooltip;
var borderStyle;
var borderWidth;
var verticalAlign;
var information;
var both;
var bothId;
var hystogrammDiv;
var hystogrammDivInfo;

function setArray(param) {
	if ((param == 7) && (YEAR == true)) {
		var myArray = [];
		var dataArray = [];
		var dataRepresentation = [];
		if (YEAR) {
			YEAR = false;
			for ( var i = 0; i < param; i++) {
				dataArray[i] = globalDataY[i]; 
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

		for ( var i = 0; i < param-1; i++) {			
			dataArray[i] = globalDataW[i+1]; // data extraction			
		}
		dataArray[i] = globalDataW[0];
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
	//console.log("load XML")
	var klocalYear = 0;
	var klocalMonth = 0;
	var klocalDay = 0;
	var klocalHour = 0;
	var klocalWeekDay = 0;
	$(xml).find('year').each(function() {
		globalDataY[klocalYear] = $(this).text();		
		klocalYear++;
	});
	$(xml).find('month').each(function() {
		globalDataM[klocalMonth] = $(this).text();		
		klocalMonth++;
	});
	$(xml).find('day').each(function() {
		globalDataD[klocalDay] = $(this).text();		
		klocalDay++;
	});
	$(xml).find('hour').each(function() {
		globalDataH[klocalHour] = $(this).text();		
		klocalHour++;
	});
	$(xml).find('weekday').each(function() {
		globalDataW[klocalWeekDay] = $(this).text();				
		klocalWeekDay++;
	});

	maxY = $(xml).find('years').attr('maxValue');	
	maxM = $(xml).find('months').attr('maxValue');	
	maxD = $(xml).find('days').attr('maxValue');	
	maxH = $(xml).find('hours').attr('maxValue');
	maxW = $(xml).find('weekdays').attr('maxValue');	
	init();
}

function init() {

	var opts1 = {
		color : "#FFFFAA",
		strokeColor : 'rgb(16,	78, 139)',
		tooltip : true,
		classWidth : '28.428571px',
		borderStyle : 'solid',
		borderWidth : '1px',
		verticalAlign : 'text-bottom'	
	};

	var opts2 = {
		color : "#FFFFAA",
		strokeColor : 'rgb(16,	78, 139)',
		tooltip : true,
		classWidth : '16.166666px',
		borderStyle : 'solid',
		borderWidth : '1px',
		verticalAlign : 'text-bottom'	
	};

	var opts3 = {
		color : "#FFFFAA",
		strokeColor : 'rgb(16,	78, 139)',
		tooltip : true,
		classWidth : '5.6451612px',
		borderStyle : 'solid',
		borderWidth : '1px',
		verticalAlign : 'text-bottom'
	};

	var opts4 = {
		color : "#FFFFAA",
		strokeColor : 'rgb(16,	78, 139)',
		tooltip : true,
		classWidth : '7.583333px',
		borderStyle : 'solid',
		borderWidth : '1px',
		verticalAlign : 'text-bottom'
	};

	var opts5 = {
		color : "#FFFFAA",
		strokeColor : 'rgb(16,	78, 139)',
		tooltip : true,
		classWidth : '28.428571px',
		borderStyle : 'solid',
		borderWidth : '1px',
		verticalAlign : 'text-bottom'	
	};

	myArray1 = setArray(7); // years
	myArray2 = setArray(12); // months
	myArray3 = setArray(31); // days
	myArray4 = setArray(24);// hours
	myArray5 = setArray(7);// weekdays

	var model1 = new HistogrammModel(myArray1, true, false, false, false, false);
 	var model2 = new HistogrammModel(myArray2, false, true, false, false, false);
 	var model3 = new HistogrammModel(myArray3, false, false, true, false, false);
 	var model4 = new HistogrammModel(myArray4, false, false, false, true, false);
 	var model5 = new HistogrammModel(myArray5, false, false, false, false, true);

	var view1 = new Histogramm(model1, opts1);
	$("#parent1").empty().html("");
	view1.render1(document.getElementById('parent1'), myArray1);

 	var view2 = new Histogramm(model2, opts2);
	$("#parent2").empty().html("");
 	view2.render2(document.getElementById('parent2'), myArray2);
// 
 	var view3 = new Histogramm(model3, opts3);
	$("#parent3").empty().html("");
 	view3.render3(document.getElementById('parent3'), myArray3);
// 
 	var view4 = new Histogramm(model4, opts4);
	$("#parent4").empty().html("");
 	view4.render4(document.getElementById('parent4'), myArray4);
// 
	var view5 = new Histogramm(model5, opts5);
	$("#parent5").empty().html("");
 	view5.render5(document.getElementById('parent5'), myArray5);
	//console.log("step 1");      
	if (ApplyPolygons == false) {
		ApplyPolygons = true;
		if (AGGREGATION==true){
		    headerXMLHistogram = createHeaderXML();
		}
		//console.log("headerXMLHistogram "+headerXMLHistogram);
		bodyXMLHistogram = timeController1XMLHistogram_Selected();
		  //console.log("step 2");
		//if (jQuery("#AggregationCheckBox").attr("checked") == true){
		  sendToServerCalendarData(headerXMLHistogram, bodyXMLHistogram);		
		//}else{}
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

function BubbleUpWindow(cls, subSpan){
	 $(cls).bt(""+subSpan.innerHTML+"",  {
		      padding: 10,
		      width: 130,
		      spikeLength: 40,
		      spikeGirth: 20,
		      cornerRadius: 20,
		      fill:'#ccffff',
		      strokeWidth: 3,
		      strokeStyle: '#CC0',
		      positions: 'top',
		      shrinkToFit: true,
		      cssStyles: {color: '#000000', fontWeight: 'bold'}
		    });
}

function OnMouseOver(cls, spanElementInfo, spanElement, t , subSpan, index, arrayX, numberPhotos){
	spanElementInfo.onmouseover = function() {
		if (arrayX[index].label == 0){
		    if (numberPhotos == 0){
		      spanElementInfo.style.background = '#FFFFAA';
		      spanElement.style.background = '#FFFFAA';
		      subSpan.innerHTML = "#pictures:" + arrayX[index].value2;
		      BubbleUpWindow(cls, subSpan);
		    }else{
		       spanElementInfo.style.background = '#DDDDDD';
		       spanElement.style.background = '#DDDDDD';
		       subSpan.innerHTML = "#pictures:" + arrayX[index].value2;
		       BubbleUpWindow(cls ,subSpan);
		    }
		}else{
		     if (numberPhotos == 0){
		     spanElementInfo.style.background = '#DDDDDD';
		       spanElement.style.background = '#DDDDDD';
		       subSpan.innerHTML = "#pictures:" + arrayX[index].value2;
		       BubbleUpWindow(cls ,subSpan);
		    }else{
		      spanElementInfo.style.background = '#FFFFAA';
		      spanElement.style.background = '#FFFFAA';
		      subSpan.innerHTML = "#pictures:" + arrayX[index].value2;
		      BubbleUpWindow(cls, subSpan);
		    }
		}
		
	};
}

function OnMouseOut(spanElementInfo, spanElement, t , subSpan, index, arrayX, numberPhotos){
	spanElementInfo.onmouseout = function() {
		if (arrayX[index].label == 0) {
			if (numberPhotos != 0){
			  spanElementInfo.style.background = "#FFFFAA";
			  spanElement.style.background = "#FFFFAA";
			}else{
			  spanElementInfo.style.background = "#DDDDDD";
			  spanElement.style.background = "#DDDDDD";
			}
		} else {
			if (numberPhotos != 0){
			  spanElementInfo.style.background = "#DDDDDD";
			  spanElement.style.background = "#DDDDDD";
			}else{
			  spanElementInfo.style.background = "#FFFFAA";
			  spanElement.style.background = "#FFFFAA";
			}
		}
		subSpan.innerHTML = "";
	};
}
function OnClick(spanElementInfo, spanElement, t , subSpan, index, arrayX, numberPhotos){
	 spanElementInfo.onclick = function() {
		if (arrayX[index].label == 0) {
			if (numberPhotos == 0){
			    spanElementInfo.style.background = '#FFFFAA';
			    spanElement.style.background = '#FFFFAA';
			    arrayX[index].label++;
			    globalArray[arrayX[index].indexVal]++
			    arrayX[index].label = arrayX[index].label%2; // instead of 1
			    globalArray[arrayX[index].indexVal] = globalArray[arrayX[index].indexVal]%2;			    
			    if (once == true) {
				    $("#histogramContent").append("<div id='absButton'><input type='button' onclick='javascript:askHistogramSelected();' value = 'Submit Query' class='right'></div>");
				    once = false;
			    }
			}else{
			    spanElementInfo.style.background = '#DDDDDD';
			    spanElement.style.background = '#DDDDDD';			   
			    arrayX[index].label++;
			    globalArray[arrayX[index].indexVal]++
			    arrayX[index].label = arrayX[index].label%2; // instead of 1
			    globalArray[arrayX[index].indexVal] = globalArray[arrayX[index].indexVal]%2;			  
			    if (once == true) {
				    $("#histogramContent").append("<div id='absButton'><input type='button' onclick='javascript:askHistogramSelected();' value = 'Submit Query' class='right'></div>");
				    once = false;
			    }
			}
		}else {
			if (numberPhotos == 0){
			  spanElementInfo.style.background = "#FFFFAA";
			  spanElement.style.background = "#FFFFAA";			  
			  arrayX[index].label++;
			  globalArray[arrayX[index].indexVal]++
			  arrayX[index].label = arrayX[index].label%2;
			  globalArray[arrayX[index].indexVal] = globalArray[arrayX[index].indexVal]%2;			
			}else{
			  spanElementInfo.style.background = "#DDDDDD";
			  spanElement.style.background = "#DDDDDD";			  
			  arrayX[index].label++;
			  globalArray[arrayX[index].indexVal]++
			  arrayX[index].label = arrayX[index].label%2;
			  globalArray[arrayX[index].indexVal] = globalArray[arrayX[index].indexVal]%2;			 
		      }
		}
	};
}

function activateToolTipSimple(t , subSpan, index, arrayX, numberPhotos){
	var k = index +t;	
	OnMouseOver(".spanElementInfo", spanElementInfo[k], spanElement[k], t , subSpan, index, arrayX, numberPhotos);
	OnMouseOut(spanElementInfo[k], spanElement[k], t , subSpan, index, arrayX, numberPhotos);    
	OnClick(spanElementInfo[k], spanElement[k], t , subSpan, index, arrayX, numberPhotos);

	OnMouseOver(".spanElement", spanElement[k], spanElementInfo[k], t , subSpan, index, arrayX, numberPhotos);
	OnMouseOut(spanElement[k], spanElementInfo[k], t , subSpan, index, arrayX, numberPhotos);    
	OnClick(spanElement[k], spanElementInfo[k], t , subSpan, index, arrayX, numberPhotos);
};

function askHistogramSelected() {	
	//console.log("AGGREGATION "+AGGREGATION);
	//deleteHistory();
	//removeCircles();
	addRemoveElementsFromHistogram();
	resetParameters();
	if (AGGREGATION==true){
	    deleteHistory();
	    removeCircles();
	    addRemoveElementsFromHistogram();
	    resetParameters();
	    headerXMLHistogram = createHeaderXML();	    
	}
	//console.log("headerXMLHistogram "+headerXMLHistogram);
	bodyXMLHistogram = timeController1XMLHistogram_Selected();
	sendToServerCalendarDataHistogram(headerXMLHistogram, bodyXMLHistogram);
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
		if (globalArray[i] == 0) {
			text1 = text1 + "<year>" + nameString[i + 1] + "</year>";
		}
	}
	text1 = text1 + "</years>";
	// *********************************************************
	text1 = text1 + "<months>";// second child // months
	for ( var i = 7; i <= 18; i++) { // in the beginning I have the months
		if (globalArray[i] == 0) {
			text1 = text1 + "<month>" + nameString[i + 1] + "</month>";
		}
	}
	text1 = text1 + "</months>";
	// *********************************************************
	text1 = text1 + "<days>";// third child // days
	for ( var i = 19; i <= 49; i++) { // in the beginning I have the days
		if (globalArray[i] == 0) {
			text1 = text1 + "<day>" + nameString[i + 1] + "</day>";
		}
	}
	text1 = text1 + "</days>";
	// *********************************************************
	text1 = text1 + "<hours>";
	for ( var i = 50; i <= 73; i++) {
		if (globalArray[i] == 0) {
			text1 = text1 + "<hour>" + nameString[i + 1] + "</hour>";
		}
	}
	text1 = text1 + "</hours>";
	// ********************************************************* for the day of
	// the week
	text1 = text1 + "<weekdays>";
	for ( var i = 74; i <= 79; i++) {
		if (globalArray[i] == 0) {			
			text1 = text1 + "<weekday>" + nameString[i + 2] + "</weekday>";
		}
	}
	if (globalArray[i] == 0) {
		text1 = text1 + "<weekday>" + nameString[75] + "</weekday>";
	}
	text1 = text1 + "</weekdays>";
	// *********************************************************
	text1 = text1 + "</calendar>";
	//alert("text1 HA: "+text1);
	return text1;
}
function setElementsForRender(thisL){
	classWidth = thisL.options.classWidth;
	strokeColor = thisL.options.strokeColor;
	color = thisL.options.color;	
	tooltip = thisL.options.tooltip;
	borderStyle = thisL.options.borderStyle;
	borderWidth = thisL.options.borderWidth;
	verticalAlign = thisL.options.verticalAlign;
	information = document.createElement("div");
	information.className = "histogramInfoButtons1";	
	information.style.display = "block";
	information.style.cssFloat = "left";
}

function setElementsForRenderSecondo(thisL, i, k){
		both = document.createElement('div');
		both.style.display = "block";
		both.style.cssFloat = "left";	
		both.style.bottom = "0px";
		bothId = 'both';
		both.id = bothId+""+globId;
		globId++;
		hystogrammDiv = document.createElement('div');
		hystogrammDiv.className = "histogramDiv";
		hystogrammDivInfo = document.createElement('div');	
		spanElementInfo[k] = document.createElement('div');	
		spanElement[k] = document.createElement('span');
		hystogrammDivInfo.className = "hystogrammDivInfo";		
		spanElement[k].className = "spanElement";	
		spanElement[k].id="spanElement"+k;	
		spanElement[k].style.borderColor = strokeColor;
		spanElement[k].style.borderStyle = borderStyle;	
		spanElement[k].style.borderBottomWidth = borderWidth;
		spanElement[k].style.borderTopWidth = borderWidth;						
		spanElement[k].style.borderRightWidth = borderWidth;
		spanElement[k].style.borderLeftWidth = borderWidth;
		spanElement[k].style.verticalAlign = verticalAlign;		
		if (thisL.model.data[i].value==0){
		    spanElement[k].style.padding = "0px " + "0px" + " 0px " + "0px";  
		    spanElement[k].style.borderRightWidth = '0px';
		    spanElement[k].style.borderLeftWidth = '0px';
		    spanElement[k].style.borderTopWidth = '0px';
		    spanElement[k].style.borderBottomWidth = '0px';
		    spanElement[k].style.backgroundColor = '#DDDDDD';
		}else{
		    spanElement[k].style.padding = thisL.model.data[i].value + "px " +classWidth + " 0px " +classWidth;  
		    spanElement[k].style.backgroundColor = color;
		}
}

function setElementsForRenderThird(thisL, i, k){
		var spanElementInfoClass = 'spanElementInfo';
		spanElementInfo[k].className = spanElementInfoClass;
		spanElementInfo[k].id = "spanElementInfo"+k;
		spanElementInfo[k].style.borderColor = strokeColor;
		spanElementInfo[k].style.borderStyle = borderStyle;
		spanElementInfo[k].style.display = "block";
		spanElementInfo[k].style.cssFloat = "left";
		spanElementInfo[k].style.borderBottomWidth = borderWidth;
		spanElementInfo[k].style.borderTopWidth = borderWidth;
		spanElementInfo[k].style.borderRightWidth = borderWidth;
		spanElementInfo[k].style.borderLeftWidth = borderWidth;
		if (thisL.model.data[i].value==0){
		    spanElementInfo[k].style.backgroundColor = '#DDDDDD';
		    spanElementInfo[k].style.marginTop="16px";
		}else{
		    spanElementInfo[k].style.backgroundColor = color;
		}
		spanElementInfo[k].style.verticalAlign = verticalAlign;

}
Histogramm.prototype.render1 = function(parent, arrayX) {
 	setElementsForRender(this);	
	for ( var i = 0; i < this.model.getNumberOfClasses(); i++) {
		var k=i;
		setElementsForRenderSecondo(this, i, k);
		globalYear[i] = this.model.data[i].value;
		if ((this.model.getNumberOfClasses() == 7) && (this.model.getIndexValueOfBool() == 1)) {
			setElementsForRenderThird(this, i, k);
			switch (i) {
			case 0:
				spanElementInfo[i].innerHTML = '2005';
				spanElementInfo[i].style.width = "56.8571428px";
				spanElementInfo[i].style.textAlign = "center";				
				break;
			case 1:
				spanElementInfo[i].innerHTML = '2006';
				spanElementInfo[i].style.width = "56.8571428px";
				spanElementInfo[i].style.textAlign = "center";				
				break;
			case 2:
				spanElementInfo[i].innerHTML = '2007';
				spanElementInfo[i].style.width = "56.8571428px";
				spanElementInfo[i].style.textAlign = "center";				
				break;
			case 3:
				spanElementInfo[i].innerHTML = '2008';
				spanElementInfo[i].style.width = "56.8571428px";
				spanElementInfo[i].style.textAlign = "center";				
				break;
			case 4:
				spanElementInfo[i].innerHTML = '2009';
				spanElementInfo[i].style.width = "56.8571428px";
				spanElementInfo[i].style.textAlign = "center";				
				break;
			case 5:
				spanElementInfo[i].innerHTML = '2010';
				spanElementInfo[i].style.width = "56.8571428px";
				spanElementInfo[i].style.textAlign = "center";				
				break;
			case 6:
				spanElementInfo[i].innerHTML = '2011';
				spanElementInfo[i].style.width = "56.8571428px";
				spanElementInfo[i].style.textAlign = "center";
				break; 
			}		
		    var subSpan = document.createElement('span');
		    subSpan.className ="Menu"+i;
		    //spanElement[i].appendChild(subSpan);		    
		    if (tooltip == true) {			
			activateToolTipSimple(0, subSpan, i, arrayX, this.model.data[i].value);
		    }
		    hystogrammDiv.appendChild(spanElement[i]);
		    hystogrammDivInfo.appendChild(spanElementInfo[i]);
		    both.appendChild(hystogrammDiv);
		    both.appendChild(hystogrammDivInfo);		   
		  } 
	  parent.appendChild(both);
	  if (i==this.model.getNumberOfClasses()-1){
	    information.innerHTML = "<div><input type='button' value='+' class='histogramInfoButton' onclick='javascript:selectAllYear();'></div> <div><input type='button' value='-' class='histogramInfoButton' onclick='javascript:de_selectAllYear();'></div> <div><input type='button' value='+ -' class='histogramInfoButton' onclick='javascript:mirrorAllYear();'></div>";
	    parent.appendChild(information);  
	  }
	}
};


Histogramm.prototype.render2 = function(parent, arrayX) {	
	setElementsForRender(this);	
	for ( var i = 0; i < this.model.getNumberOfClasses(); i++) {
		var k=i+7;
	        setElementsForRenderSecondo(this, i, k);
		globalMonth[i] = this.model.data[i].value;
		if ((this.model.getNumberOfClasses() == 12)&& (this.model.getIndexValueOfBool() == 2)) {
			setElementsForRenderThird(this, i, k);
			switch (i) {
			case 0:
				spanElementInfo[k].innerHTML = 'Jan';				
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "32.333332px";
				break;
			case 1:
				spanElementInfo[k].innerHTML = 'Feb';				
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "32.333332px";
				break;
			case 2:
				spanElementInfo[k].innerHTML = 'Mar';				
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "32.333332px";
				break;
			case 3:
				spanElementInfo[k].innerHTML = 'Apr';				
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "32.333332px";
				break;
			case 4:
				spanElementInfo[k].innerHTML = 'May';				
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "32.333332px";
				break;
			case 5:
				spanElementInfo[k].innerHTML = 'Jun';				
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "32.333332px";
				break;
			case 6:
				spanElementInfo[k].innerHTML = 'Jul';				
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "32.333332px";
				break;
			case 7:
				spanElementInfo[k].innerHTML = 'Aug';				
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "32.333332px";
				break;
			case 8:
				spanElementInfo[k].innerHTML = 'Sep';				
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "32.333332px";
				break;
			case 9:
				spanElementInfo[k].innerHTML = 'Oct';				
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "32.333332px";
				break;
			case 10:
				spanElementInfo[k].innerHTML = 'Nov';				
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "32.333332px";
				break;
			case 11:
				spanElementInfo[k].innerHTML = 'Dec';				
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "32.333332px";
				break;

			}			
		    var subSpan = document.createElement('span');
		    subSpan.className ="Menu";
		    //both.appendChild(subSpan);
		    if (tooltip == true) {			
			activateToolTipSimple(7, subSpan, i, arrayX, this.model.data[i].value);
		    }
		    hystogrammDiv.appendChild(spanElement[k]);
		    hystogrammDivInfo.appendChild(spanElementInfo[k]);
		    both.appendChild(hystogrammDiv);
		    both.appendChild(hystogrammDivInfo);
		  } 
	  parent.appendChild(both);
	  if (i==this.model.getNumberOfClasses()-1){
	    information.innerHTML = "<div><input type='button' value='+' class='histogramInfoButton' onclick='javascript:selectAllMonths();'></div> <div><input type='button' value='-' class='histogramInfoButton'  onclick='javascript:de_selectAllMonths()'></div> <div><input type='button' value='+ -' class='histogramInfoButton' onclick='javascript:mirrorAllMonths();'></div>";
	    parent.appendChild(information);  
	  }
	}
};

Histogramm.prototype.render3 = function(parent, arrayX) {	
	setElementsForRender(this);	
	for ( var i = 0; i < this.model.getNumberOfClasses(); i++) {
		var k=i+19;
	        setElementsForRenderSecondo(this, i, k);
		globalDay[i] = this.model.data[i].value;
		if (this.model.getNumberOfClasses() == 31) {
			setElementsForRenderThird(this, i, k);
			switch (i) {
			case 0:
				spanElementInfo[k].innerHTML = '1';				
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;
			case 1:
				spanElementInfo[k].innerHTML = '2';				
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;
			case 2:
				spanElementInfo[k].innerHTML = '3';				
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;
			case 3:
				spanElementInfo[k].innerHTML = '4';				
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;
			case 4:
				spanElementInfo[k].innerHTML = '5';				
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;
			case 5:
				spanElementInfo[k].innerHTML = '6';
				// spanElementInfo.style.padding = '0px ' + '2.5px';
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;
			case 6:
				spanElementInfo[k].innerHTML = '7';
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;
			case 7:
				spanElementInfo[k].innerHTML = '8';
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;
			case 8:
				spanElementInfo[k].innerHTML = '9';
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;
			case 9:
				spanElementInfo[k].innerHTML = '10';
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;
			case 10:
				spanElementInfo[k].innerHTML = '11';
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;
			case 11:
				spanElementInfo[k].innerHTML = '12';
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;
			case 12:
				spanElementInfo[k].innerHTML = '13';
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;
			case 13:
				spanElementInfo[k].innerHTML = '14';
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;
			case 14:
				spanElementInfo[k].innerHTML = '15';
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;
			case 15:
				spanElementInfo[k].innerHTML = '16';
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;
			case 16:
				spanElementInfo[k].innerHTML = '17';
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;
			case 17:
				spanElementInfo[k].innerHTML = '18';
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;
			case 18:
				spanElementInfo[k].innerHTML = '19';
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;
			case 19:
				spanElementInfo[k].innerHTML = '20';
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;
			case 20:
				spanElementInfo[k].innerHTML = '21';
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;
			case 21:
				spanElementInfo[k].innerHTML = '22';
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;
			case 22:
				spanElementInfo[k].innerHTML = '23';
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;
			case 23:
				spanElementInfo[k].innerHTML = '24';
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;
			case 24:
				spanElementInfo[k].innerHTML = '25';
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;
			case 25:
				spanElementInfo[k].innerHTML = '26';
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;
			case 26:
				spanElementInfo[k].innerHTML = '27';
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;
			case 27:
				spanElementInfo[k].innerHTML = '28';
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;
			case 28:
				spanElementInfo[k].innerHTML = '29';
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;
			case 29:
				spanElementInfo[k].innerHTML = '30';
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;
			case 30:
				spanElementInfo[k].innerHTML = '31';
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.width = "11.290322px";
				spanElementInfo[k].style.fontSize = "8.5px";
				break;

			}
		    var subSpan = document.createElement('span');
		    subSpan.className ="Menu";
		    //both.appendChild(subSpan);
		    if (tooltip == true) {			
			activateToolTipSimple(19, subSpan, i, arrayX, this.model.data[i].value);
		    }
		    hystogrammDiv.appendChild(spanElement[k]);
		    hystogrammDivInfo.appendChild(spanElementInfo[k]);
		    both.appendChild(hystogrammDiv);
		    both.appendChild(hystogrammDivInfo);
		  } 
	  parent.appendChild(both);
	  if (i==this.model.getNumberOfClasses()-1){
	    information.innerHTML = "<div><input type='button' value='+' class='histogramInfoButton' onclick='javascript:selectAllDays();'></div> <div><input type='button' value='-' class='histogramInfoButton' onclick='javascript:de_selectAllDays()'></div> <div><input type='button' value='+ -' class='histogramInfoButton' onclick='javascript:mirrorAllDays();'></div>";
	    parent.appendChild(information);  
	  }
	}
};


Histogramm.prototype.render4 = function(parent, arrayX) {	
	setElementsForRender(this);	
	for ( var i = 0; i < this.model.getNumberOfClasses(); i++) {
		var k=i+50;
	        setElementsForRenderSecondo(this, i, k);
		globalHour[i] = this.model.data[i].value;
		if (this.model.getNumberOfClasses() == 24) {
			setElementsForRenderThird(this, i, k);
			switch (i) {
			case 0:
				spanElementInfo[k].innerHTML = '0';
				spanElementInfo[k].style.width = "15.166666px";
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.fontSize = "10px";
				break;
			case 1:
				spanElementInfo[k].innerHTML = '1';
				spanElementInfo[k].style.width = "15.166666px";
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.fontSize = "10px";
				break;
			case 2:
				spanElementInfo[k].innerHTML = '2';
				spanElementInfo[k].style.width = "15.166666px";
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.fontSize = "10px";
				break;
			case 3:
				spanElementInfo[k].innerHTML = '3';
				spanElementInfo[k].style.width = "15.166666px";
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.fontSize = "10px";
				break;
			case 4:
				spanElementInfo[k].innerHTML = '4';
				spanElementInfo[k].style.width = "15.166666px";
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.fontSize = "10px";
				break;
			case 5:
				spanElementInfo[k].innerHTML = '5';
				spanElementInfo[k].style.width = "15.166666px";
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.fontSize = "10px";
				break;
			case 6:
				spanElementInfo[k].innerHTML = '6';
				spanElementInfo[k].style.width = "15.166666px";
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.fontSize = "10px";
				break;
			case 7:
				spanElementInfo[k].innerHTML = '7';
				spanElementInfo[k].style.width = "15.166666px";
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.fontSize = "10px";
				break;
			case 8:
				spanElementInfo[k].innerHTML = '8';
				spanElementInfo[k].style.width = "15.166666px";
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.fontSize = "10px";
				break;
			case 9:
				spanElementInfo[k].innerHTML = '9';
				spanElementInfo[k].style.width = "15.166666px";
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.fontSize = "10px";
				break;
			case 10:
				spanElementInfo[k].innerHTML = '10';
				spanElementInfo[k].style.width = "15.166666px";
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.fontSize = "10px";
				break;
			case 11:
				spanElementInfo[k].innerHTML = '11';
				spanElementInfo[k].style.width = "15.166666px";
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.fontSize = "10px";
				break;
			case 12:
				spanElementInfo[k].innerHTML = '12';
				spanElementInfo[k].style.width = "15.166666px";
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.fontSize = "10px";
				break;
			case 13:
				spanElementInfo[k].innerHTML = '13';
				spanElementInfo[k].style.width = "15.166666px";
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.fontSize = "10px";
				break;
			case 14:
				spanElementInfo[k].innerHTML = '14';
				spanElementInfo[k].style.width = "15.166666px";
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.fontSize = "10px";
				break;
			case 15:
				spanElementInfo[k].innerHTML = '15';
				spanElementInfo[k].style.width = "15.166666px";
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.fontSize = "10px";
				break;
			case 16:
				spanElementInfo[k].innerHTML = '16';
				spanElementInfo[k].style.width = "15.166666px";
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.fontSize = "10px";
				break;
			case 17:
				spanElementInfo[k].innerHTML = '17';
				spanElementInfo[k].style.width = "15.166666px";
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.fontSize = "10px";
				break;
			case 18:
				spanElementInfo[k].innerHTML = '18';
				spanElementInfo[k].style.width = "15.166666px";
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.fontSize = "10px";
				break;
			case 19:
				spanElementInfo[k].innerHTML = '19';
				spanElementInfo[k].style.width = "15.166666px";
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.fontSize = "10px";
				break;
			case 20:
				spanElementInfo[k].innerHTML = '20';
				spanElementInfo[k].style.width = "15.166666px";
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.fontSize = "10px";
				break;
			case 21:
				spanElementInfo[k].innerHTML = '21';
				spanElementInfo[k].style.width = "15.166666px";
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.fontSize = "10px";
				break;
			case 22:
				spanElementInfo[k].innerHTML = '22';
				spanElementInfo[k].style.width = "15.166666px";
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.fontSize = "10px";
				break;
			case 23:
				spanElementInfo[k].innerHTML = '23';
				spanElementInfo[k].style.width = "15.166666px";
				spanElementInfo[k].style.textAlign = "center";
				spanElementInfo[k].style.fontSize = "10px";
				break;
			}
		       var subSpan = document.createElement('span');
		    subSpan.className ="Menu";
		    //both.appendChild(subSpan);
		    if (tooltip == true) {			
			activateToolTipSimple(50, subSpan, i, arrayX, this.model.data[i].value);
		    }
		    hystogrammDiv.appendChild(spanElement[k]);
		    hystogrammDivInfo.appendChild(spanElementInfo[k]);
		    both.appendChild(hystogrammDiv);
		    both.appendChild(hystogrammDivInfo);
		  } 
	  parent.appendChild(both);
	  if (i==this.model.getNumberOfClasses()-1){
	    information.innerHTML = "<div><input type='button' value='+' class='histogramInfoButton' onclick='javascript:selectAllHours();'></div> <div><input type='button' value='-' class='histogramInfoButton' onclick='javascript:de_selectAllHours();'></div> <div><input type='button' value='+ -' class='histogramInfoButton' onclick='javascript:mirrorAllHours();'></div>";
	    parent.appendChild(information);  
	  }
	}
};

Histogramm.prototype.render5 = function(parent, arrayX) {	
	setElementsForRender(this);	
	for ( var i = 0; i < this.model.getNumberOfClasses(); i++) {
		var k=i+74;
	        setElementsForRenderSecondo(this, i, k);
		globalWeek[i] = this.model.data[i].value;
		if ((this.model.getNumberOfClasses() == 7) && (this.model.getIndexValueOfBool() == 5)) {
			setElementsForRenderThird(this, i, k);
			switch (i) {			
			case 0:
				spanElementInfo[k].innerHTML = 'Mon';
				spanElementInfo[k].style.width = "56.8571428px";
				spanElementInfo[k].style.textAlign = "center";
				break;
			case 1:
				spanElementInfo[k].innerHTML = 'Tue';
				spanElementInfo[k].style.width = "56.8571428px";
				spanElementInfo[k].style.textAlign = "center";
				break;
			case 2:
				spanElementInfo[k].innerHTML = 'Wed';
				spanElementInfo[k].style.width = "56.8571428px";
				spanElementInfo[k].style.textAlign = "center";
				break;
			case 3:
				spanElementInfo[k].innerHTML = 'Thu';
				spanElementInfo[k].style.width = "56.8571428px";
				spanElementInfo[k].style.textAlign = "center";
				break;
			case 4:
				spanElementInfo[k].innerHTML = 'Fri';
				spanElementInfo[k].style.width = "56.8571428px";
				spanElementInfo[k].style.textAlign = "center";
				break;
			case 5:
				spanElementInfo[k].innerHTML = 'Sat';
				spanElementInfo[k].style.width = "56.8571428px";
				spanElementInfo[k].style.textAlign = "center";
				break;
			case 6:
				spanElementInfo[k].innerHTML = 'Sun';
				spanElementInfo[k].style.width = "56.8571428px";// 25.714285px
				spanElementInfo[k].style.textAlign = "center";
				break;

			}
		    var subSpan = document.createElement('span');
		    subSpan.className ="Menu";
		    if (tooltip == true) {			
			activateToolTipSimple(74, subSpan, i, arrayX, this.model.data[i].value);
		    }
		    hystogrammDiv.appendChild(spanElement[k]);
		    hystogrammDivInfo.appendChild(spanElementInfo[k]);
		    both.appendChild(hystogrammDiv);
		    both.appendChild(hystogrammDivInfo);
		  } 
	  parent.appendChild(both);
	  if (i==this.model.getNumberOfClasses()-1){
	    information.innerHTML = "<div><input type='button' value='+' class='histogramInfoButton' onclick='javascript:selectAllWeekDays();'></div> <div><input type='button' value='-' class='histogramInfoButton' onclick='javascript:de_selectAllWeekDays();'></div> <div><input type='button' value='+ -' class='histogramInfoButton'  onclick='javascript:mirrorAllWeekDays();'></div>";
	    parent.appendChild(information);  
	  }
	}
};

function selectAllWeekDays(){
        for (var i = 0; i <7; i++){
		var j = i+74;
		spanElementInfo[j].style.background = "#FFFFAA";
		spanElement[j].style.background = "#FFFFAA";
		var numberPhotos = globalWeek[i];	
		if (myArray5[i].label == 0) {
			if (numberPhotos == 0){
			    myArray5[i].label++;
			    globalArray[myArray5[i].indexVal]++
			    myArray5[i].label = myArray5[i].label%2;
			    globalArray[myArray5[i].indexVal] = globalArray[myArray5[i].indexVal]%2;
			} else{	
			}
		}else {
			if (numberPhotos == 0){
			}else{
			    myArray5[i].label++;
			    globalArray[myArray5[i].indexVal]++
			    myArray5[i].label = myArray5[i].label%2; // instead of 1
			    globalArray[myArray5[i].indexVal] = globalArray[myArray5[i].indexVal]%2;	
		      }
		}
      }
      if (once == true) {
	$("#histogramContent").append("<div id='absButton'><input type='button' onclick='javascript:askHistogramSelected();' value = 'Submit Query' class='right'></div>");
	once = false;
      }     
}

function de_selectAllWeekDays(){
        for (var i = 0; i <7; i++){
		var j = i+74;
		spanElementInfo[j].style.background = "#DDDDDD";
		spanElement[j].style.background = "#DDDDDD";
		var numberPhotos = globalWeek[i];	
		if (myArray5[i].label == 0) {
			if (numberPhotos == 0){
			} else{	
			    myArray5[i].label++;
			    globalArray[myArray5[i].indexVal]++
			    myArray5[i].label = myArray5[i].label%2;
			    globalArray[myArray5[i].indexVal] = globalArray[myArray5[i].indexVal]%2;
			}
		}else {
			if (numberPhotos == 0){
			    myArray5[i].label++;
			    globalArray[myArray5[i].indexVal]++
			    myArray5[i].label = myArray5[i].label%2; // instead of 1
			    globalArray[myArray5[i].indexVal] = globalArray[myArray5[i].indexVal]%2;	
			}else{
		      }
		}
      }
      if (once == true) {
	$("#histogramContent").append("<div id='absButton'><input type='button' onclick='javascript:askHistogramSelected();' value = 'Submit Query' class='right'></div>");
	once = false;
      }     
}

function mirrorAllWeekDays(){
        for (var i = 0; i <7; i++){
		var j = i+74;		
		var numberPhotos = globalWeek[i];	
		if (myArray5[i].label == 0) {
			if (numberPhotos == 0){
			    spanElementInfo[j].style.background = "#FFFFAA";
			    spanElement[j].style.background = "#FFFFAA";
			    myArray5[i].label++;
			    globalArray[myArray5[i].indexVal]++
			    myArray5[i].label = myArray5[i].label%2;
			    globalArray[myArray5[i].indexVal] = globalArray[myArray5[i].indexVal]%2;
			} else{	
			    spanElementInfo[j].style.background = "#DDDDDD";
			    spanElement[j].style.background = "#DDDDDD";
			    myArray5[i].label++;
			    globalArray[myArray5[i].indexVal]++
			    myArray5[i].label = myArray5[i].label%2;
			    globalArray[myArray5[i].indexVal] = globalArray[myArray5[i].indexVal]%2;
			}
		}else {
			if (numberPhotos == 0){
			    spanElementInfo[j].style.background = "#DDDDDD";
			    spanElement[j].style.background = "#DDDDDD";
			    myArray5[i].label++;
			    globalArray[myArray5[i].indexVal]++
			    myArray5[i].label = myArray5[i].label%2; // instead of 1
			    globalArray[myArray5[i].indexVal] = globalArray[myArray5[i].indexVal]%2;	
			}else{
			    spanElementInfo[j].style.background = "#FFFFAA";
			    spanElement[j].style.background = "#FFFFAA";
			    myArray5[i].label++;
			    globalArray[myArray5[i].indexVal]++
			    myArray5[i].label = myArray5[i].label%2; // instead of 1
			    globalArray[myArray5[i].indexVal] = globalArray[myArray5[i].indexVal]%2;	
		      }
		}
      }
      if (once == true) {
	$("#histogramContent").append("<div id='absButton'><input type='button' onclick='javascript:askHistogramSelected();' value = 'Submit Query' class='right'></div>");
	once = false;
      }     
}



function selectAllHours(){
       for (var i = 0; i <24; i++){
		var j = i+50;
		spanElementInfo[j].style.background = "#FFFFAA";
		spanElement[j].style.background = "#FFFFAA";
		var numberPhotos = globalHour[i];	
		if (myArray4[i].label == 0) {
			if (numberPhotos == 0){
			    myArray4[i].label++;
			    globalArray[myArray4[i].indexVal]++
			    myArray4[i].label = myArray4[i].label%2;
			    globalArray[myArray4[i].indexVal] = globalArray[myArray4[i].indexVal]%2;
			} else{	
			}
		}else {
			if (numberPhotos == 0){
			}else{
			    myArray4[i].label++;
			    globalArray[myArray4[i].indexVal]++
			    myArray4[i].label = myArray4[i].label%2; // instead of 1
			    globalArray[myArray4[i].indexVal] = globalArray[myArray4[i].indexVal]%2;	
		      }
		}
      }
      if (once == true) {
	$("#histogramContent").append("<div id='absButton'><input type='button' onclick='javascript:askHistogramSelected();' value = 'Submit Query' class='right'></div>");
	once = false;
      }     
}

function de_selectAllHours(){
       for (var i = 0; i <24; i++){
		var j = i+50;
		spanElementInfo[j].style.background = "#DDDDDD";
		spanElement[j].style.background = "#DDDDDD";
		var numberPhotos = globalHour[i];	
		if (myArray4[i].label == 0) {
			if (numberPhotos == 0){			  
			} else{	
			    myArray4[i].label++;
			    globalArray[myArray4[i].indexVal]++
			    myArray4[i].label = myArray4[i].label%2;
			    globalArray[myArray4[i].indexVal] = globalArray[myArray4[i].indexVal]%2;
			}
		}else {
			if (numberPhotos == 0){
			    myArray4[i].label++;
			    globalArray[myArray4[i].indexVal]++
			    myArray4[i].label = myArray4[i].label%2; // instead of 1
			    globalArray[myArray4[i].indexVal] = globalArray[myArray4[i].indexVal]%2;	
			}else{
		      }
		}
      }
      if (once == true) {
	$("#histogramContent").append("<div id='absButton'><input type='button' onclick='javascript:askHistogramSelected();' value = 'Submit Query' class='right'></div>");
	once = false;
      }     
}

function mirrorAllHours(){
       for (var i = 0; i <24; i++){
		var j = i+50;
		var numberPhotos = globalHour[i];	
		if (myArray4[i].label == 0) {
			if (numberPhotos == 0){
			    spanElementInfo[j].style.background = "#FFFFAA";
			    spanElement[j].style.background = "#FFFFAA";
			    myArray4[i].label++;
			    globalArray[myArray4[i].indexVal]++
			    myArray4[i].label = myArray4[i].label%2;
			    globalArray[myArray4[i].indexVal] = globalArray[myArray4[i].indexVal]%2;
			} else{
			    spanElementInfo[j].style.background = "#DDDDDD";
			    spanElement[j].style.background = "#DDDDDD";
			    myArray4[i].label++;
			    globalArray[myArray4[i].indexVal]++
			    myArray4[i].label = myArray4[i].label%2;
			    globalArray[myArray4[i].indexVal] = globalArray[myArray4[i].indexVal]%2;
			}
		}else {
			if (numberPhotos == 0){
			    spanElementInfo[j].style.background = "#DDDDDD";
			    spanElement[j].style.background = "#DDDDDD";
			    myArray4[i].label++;
			    globalArray[myArray4[i].indexVal]++
			    myArray4[i].label = myArray4[i].label%2; // instead of 1
			    globalArray[myArray4[i].indexVal] = globalArray[myArray4[i].indexVal]%2;	
			}else{
			    spanElementInfo[j].style.background = "#FFFFAA";
			    spanElement[j].style.background = "#FFFFAA";
			    myArray4[i].label++;
			    globalArray[myArray4[i].indexVal]++
			    myArray4[i].label = myArray4[i].label%2; // instead of 1
			    globalArray[myArray4[i].indexVal] = globalArray[myArray4[i].indexVal]%2;	
		      }
		}
      }
      if (once == true) {
	$("#histogramContent").append("<div id='absButton'><input type='button' onclick='javascript:askHistogramSelected();' value = 'Submit Query' class='right'></div>");
	once = false;
      }     
}




function selectAllDays(){
       for (var i = 0; i <31; i++){
		var j = i+19;
		spanElementInfo[j].style.background = "#FFFFAA";
		spanElement[j].style.background = "#FFFFAA";
		var numberPhotos = globalDay[i];	
		if (myArray3[i].label == 0) {
			if (numberPhotos == 0){
			    myArray3[i].label++;
			    globalArray[myArray3[i].indexVal]++
			    myArray3[i].label = myArray3[i].label%2;
			    globalArray[myArray3[i].indexVal] = globalArray[myArray3[i].indexVal]%2;
			} else{	
			}
		}else {
			if (numberPhotos == 0){
			}else{
			    myArray3[i].label++;
			    globalArray[myArray3[i].indexVal]++
			    myArray3[i].label = myArray3[i].label%2; // instead of 1
			    globalArray[myArray3[i].indexVal] = globalArray[myArray3[i].indexVal]%2;	
		      }
		}
      }
      if (once == true) {
	$("#histogramContent").append("<div id='absButton'><input type='button' onclick='javascript:askHistogramSelected();' value = 'Submit Query' class='right'></div>");
	once = false;
      }     
}

function de_selectAllDays(){
       for (var i = 0; i <31; i++){
		var j = i+19;
		spanElementInfo[j].style.background = "#DDDDDD";
		spanElement[j].style.background = "#DDDDDD";
		var numberPhotos = globalDay[i];	
		if (myArray3[i].label == 0) {
			if (numberPhotos == 0){			  
			} else{	
			    myArray3[i].label++;
			    globalArray[myArray3[i].indexVal]++
			    myArray3[i].label = myArray3[i].label%2;
			    globalArray[myArray3[i].indexVal] = globalArray[myArray3[i].indexVal]%2;
			}
		}else {
			if (numberPhotos == 0){
			    myArray3[i].label++;
			    globalArray[myArray3[i].indexVal]++
			    myArray3[i].label = myArray3[i].label%2; // instead of 1
			    globalArray[myArray3[i].indexVal] = globalArray[myArray3[i].indexVal]%2;	
			}else{			  
		      }
		}
      }
      if (once == true) {
	$("#histogramContent").append("<div id='absButton'><input type='button' onclick='javascript:askHistogramSelected();' value = 'Submit Query' class='right'></div>");
	once = false;
      }     
}

function mirrorAllDays(){
         for (var i = 0; i <31; i++){
		var j = i+19;		
		var numberPhotos = globalDay[i];	
		if (myArray3[i].label == 0) {
			if (numberPhotos == 0){
			    spanElementInfo[j].style.background = "#FFFFAA";
			    spanElement[j].style.background = "#FFFFAA";
			    myArray3[i].label++;
			    globalArray[myArray3[i].indexVal]++
			    myArray3[i].label = myArray3[i].label%2;
			    globalArray[myArray3[i].indexVal] = globalArray[myArray3[i].indexVal]%2;
			} else{
			    spanElementInfo[j].style.background = "#DDDDDD";
			    spanElement[j].style.background = "#DDDDDD";
			    myArray3[i].label++;
			    globalArray[myArray3[i].indexVal]++
			    myArray3[i].label = myArray3[i].label%2;
			    globalArray[myArray3[i].indexVal] = globalArray[myArray3[i].indexVal]%2;
			}
		}else {
			if (numberPhotos == 0){
			    spanElementInfo[j].style.background = "#DDDDDD";
			    spanElement[j].style.background = "#DDDDDD";
			    myArray3[i].label++;
			    globalArray[myArray3[i].indexVal]++
			    myArray3[i].label = myArray3[i].label%2; 
			    globalArray[myArray3[i].indexVal] = globalArray[myArray3[i].indexVal]%2;	
			}else{
			    spanElementInfo[j].style.background = "#FFFFAA";
			    spanElement[j].style.background = "#FFFFAA";
			    myArray3[i].label++;
			    globalArray[myArray3[i].indexVal]++
			    myArray3[i].label = myArray3[i].label%2;
			    globalArray[myArray3[i].indexVal] = globalArray[myArray3[i].indexVal]%2;	
		      }
		}
      }
      if (once == true) {
	$("#histogramContent").append("<div id='absButton'><input type='button' onclick='javascript:askHistogramSelected();' value = 'Submit Query' class='right'></div>");
	once = false;
      }     
}




function selectAllMonths(){
      for (var i = 0; i < 12; i++){
		var j = i+7;
		spanElementInfo[j].style.background = "#FFFFAA";
		spanElement[j].style.background = "#FFFFAA";
		var numberPhotos = globalMonth[i];		
		if (myArray2[i].label == 0) {		
			if (numberPhotos == 0){				   	   
			    myArray2[i].label++;
			    globalArray[myArray2[i].indexVal]++
			    myArray2[i].label = myArray2[i].label%2;
			    globalArray[myArray2[i].indexVal] = globalArray[myArray2[i].indexVal]%2;
			} else{				  
			}
		}else {
			if (numberPhotos == 0){			 
			}else{			  
			    myArray2[i].label++;
			    globalArray[myArray2[i].indexVal]++
			    myArray2[i].label = myArray2[i].label%2; // instead of 1
			    globalArray[myArray2[i].indexVal] = globalArray[myArray2[i].indexVal]%2;	
		      }
		}  	
      }
      if (once == true) {
	$("#histogramContent").append("<div id='absButton'><input type='button' onclick='javascript:askHistogramSelected();' value = 'Submit Query' class='right'></div>");
	once = false;
      }     
}

function de_selectAllMonths(){
      for (var i = 0; i < 12; i++){
		var j = i+7;
		spanElementInfo[j].style.background = "#DDDDDD";
		spanElement[j].style.background = "#DDDDDD";
		var numberPhotos = globalMonth[i];		
		if (myArray2[i].label == 0) {		
			if (numberPhotos == 0){				   	   
			   
			} else{		
			    myArray2[i].label++;
			    globalArray[myArray2[i].indexVal]++
			    myArray2[i].label = myArray2[i].label%2;
			    globalArray[myArray2[i].indexVal] = globalArray[myArray2[i].indexVal]%2;		 	    			   
			}
		}else {
			if (numberPhotos == 0){	
			    myArray2[i].label++;
			    globalArray[myArray2[i].indexVal]++
			    myArray2[i].label = myArray2[i].label%2; // instead of 1
			    globalArray[myArray2[i].indexVal] = globalArray[myArray2[i].indexVal]%2;			
			}else{						   
		      }
		}  	
      }
      if (once == true) {
	$("#histogramContent").append("<div id='absButton'><input type='button' onclick='javascript:askHistogramSelected();' value = 'Submit Query' class='right'></div>");
	once = false;
      }     
}


function mirrorAllMonths(){
	  for (var i = 0; i < 12; i++){
		var j = i+7;		
		var numberPhotos = globalMonth[i];		
		if (myArray2[i].label == 0) {		
			if (numberPhotos == 0){	
			    spanElementInfo[j].style.background = "#FFFFAA";
			    spanElement[j].style.background = "#FFFFAA";	   
			    myArray2[i].label++;
			    globalArray[myArray2[i].indexVal]++
			    myArray2[i].label = myArray2[i].label%2;
			    globalArray[myArray2[i].indexVal] = globalArray[myArray2[i].indexVal]%2;
			} else{	
			    spanElementInfo[j].style.background = "#DDDDDD";
			    spanElement[j].style.background = "#DDDDDD";
			    myArray2[i].label++;
			    globalArray[myArray2[i].indexVal]++
			    myArray2[i].label = myArray2[i].label%2;
			    globalArray[myArray2[i].indexVal] = globalArray[myArray2[i].indexVal]%2;		   
			}
		}else {
			if (numberPhotos == 0){
			    spanElementInfo[j].style.background = "#DDDDDD";
			    spanElement[j].style.background = "#DDDDDD";
			    myArray2[i].label++;
			    globalArray[myArray2[i].indexVal]++
			    myArray2[i].label = myArray2[i].label%2; // instead of 1
			    globalArray[myArray2[i].indexVal] = globalArray[myArray2[i].indexVal]%2;
			}else{
			  spanElementInfo[j].style.background = "#FFFFAA";
			    spanElement[j].style.background = "#FFFFAA";
			    myArray2[i].label++;
			    globalArray[myArray2[i].indexVal]++
			    myArray2[i].label = myArray2[i].label%2; // instead of 1
			    globalArray[myArray2[i].indexVal] = globalArray[myArray2[i].indexVal]%2;	
		      }
		}  	
      }
      if (once == true) {
	$("#histogramContent").append("<div id='absButton'><input type='button' onclick='javascript:askHistogramSelected();' value = 'Submit Query' class='right'></div>");
	once = false;
      }     
}
/*this changes the color and the selection value 0 or 1*/
function selectAllYear(){       
      for (var i = 0; i < 7; i++){		
		spanElementInfo[i].style.background = "#FFFFAA";
		spanElement[i].style.background = "#FFFFAA";
		var numberPhotos = globalYear[i];		
		if (myArray1[i].label == 0) {		
			if (numberPhotos == 0){			    
			    myArray1[i].label++;
			    globalArray[myArray1[i].indexVal]++
			    myArray1[i].label = myArray1[i].label%2; // instead of 1
			    globalArray[myArray1[i].indexVal] = globalArray[myArray1[i].indexVal]%2;			    			
			} else{				  
			}
		}else {
			if (numberPhotos == 0){					 
			}else{					   
			    myArray1[i].label++;
			    globalArray[myArray1[i].indexVal]++
			    myArray1[i].label = myArray1[i].label%2; // instead of 1
			    globalArray[myArray1[i].indexVal] = globalArray[myArray1[i].indexVal]%2;	
		      }
		}  	
      }
      if (once == true) {
	$("#histogramContent").append("<div id='absButton'><input type='button' onclick='javascript:askHistogramSelected();' value = 'Submit Query' class='right'></div>");
	once = false;
      }     
}

function de_selectAllYear(){
        for (var i = 0; i < 7; i++){		
		spanElementInfo[i].style.background = "#DDDDDD";
		spanElement[i].style.background = "#DDDDDD";
		var numberPhotos = globalYear[i];
		if (myArray1[i].label == 0) {
			if (numberPhotos == 0){			   		    			
			} else{
			    myArray1[i].label++;
			    globalArray[myArray1[i].indexVal]++
			    myArray1[i].label = myArray1[i].label%2;
			    globalArray[myArray1[i].indexVal] = globalArray[myArray1[i].indexVal]%2;	
			}
		}else {
			if (numberPhotos == 0){
			    myArray1[i].label++;
			    globalArray[myArray1[i].indexVal]++
			    myArray1[i].label = myArray1[i].label%2; // instead of 1
			    globalArray[myArray1[i].indexVal] = globalArray[myArray1[i].indexVal]%2;			 
			}else{					   			   
		      }
		}  	
      }
      if (once == true) {
	$("#histogramContent").append("<div id='absButton'><input type='button' onclick='javascript:askHistogramSelected();' value = 'Submit Query' class='right'></div>");
	once = false;
      }
}

function mirrorAllYear(){
      for (var i = 0; i < 7; i++){				
		var numberPhotos = globalYear[i];
		if (myArray1[i].label == 0) {
			if (numberPhotos == 0){	
			    spanElementInfo[i].style.background = "#FFFFAA";
			    spanElement[i].style.background = "#FFFFAA";
			    myArray1[i].label++;
			    globalArray[myArray1[i].indexVal]++
			    myArray1[i].label = myArray1[i].label%2; // instead of 1
			    globalArray[myArray1[i].indexVal] = globalArray[myArray1[i].indexVal]%2;		   		    			
			} else{
			    spanElementInfo[i].style.background = "#DDDDDD";
			    spanElement[i].style.background = "#DDDDDD";
			    myArray1[i].label++;
			    globalArray[myArray1[i].indexVal]++
			    myArray1[i].label = myArray1[i].label%2;
			    globalArray[myArray1[i].indexVal] = globalArray[myArray1[i].indexVal]%2;	
			}
		}else {
			if (numberPhotos == 0){
			    spanElementInfo[i].style.background = "#DDDDDD";
			    spanElement[i].style.background = "#DDDDDD";
			    myArray1[i].label++;
			    globalArray[myArray1[i].indexVal]++
			    myArray1[i].label = myArray1[i].label%2; // instead of 1
			    globalArray[myArray1[i].indexVal] = globalArray[myArray1[i].indexVal]%2;			 
			}else{
			    spanElementInfo[i].style.background = "#FFFFAA";
			    spanElement[i].style.background = "#FFFFAA";
			    myArray1[i].label++;
			    globalArray[myArray1[i].indexVal]++
			    myArray1[i].label = myArray1[i].label%2; // instead of 1
			    globalArray[myArray1[i].indexVal] = globalArray[myArray1[i].indexVal]%2;
		      }
		}
      }
      if (once == true) {
	$("#histogramContent").append("<div id='absButton'><input type='button' onclick='javascript:askHistogramSelected();' value = 'Submit Query' class='right'></div>");
	once = false;
      }
}



function histogramReset(){
	for (var i=0;i<globalArray.length;i++){
	  globalArray[i]=0;
	}
	resetParameters();
	askHistogramSelected();
}

function askHistogram() {
      askHistogramSelected();
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
	for ( var i = 74; i <= 79; i++) {				
	    text1 = text1 + "<weekday>" + nameString[i + 2] + "</weekday>";		
	}
	text1 = text1 + "<weekday>" + nameString[75] + "</weekday>";
	text1 = text1 + "</weekdays>";	
	// *********************************************************
	text1 = text1 + "</calendar>";
	return text1;
}
// sendToServer.js

