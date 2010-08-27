YAHOO.namespace("example.calendar");

var ContorSelected=0;
var ContorDeselected=0;


var textSelected = new Array(3000);
var textDESelected = new Array(3000);
var textFinal=new Array(3000);
	
	
// I should have also something with the time
var time1 = new Array(3000);
var time2 = new Array(3000);

//the string xml that I want to send to server
var text = "";
var contor;
var k; 


/*here I refresh everything*/
/*I think 3000 is big enough for the selection of the calendar. One must select by hand more then 3000 days in order to break this*/
function InitializeParameters()
{
	ContorSelected=0;
	ContorDeselected=0;
	
}
	
YAHOO.example.calendar.init = function() {
//var eLog = YAHOO.util.Dom.get("evtentries");
var eCount = 1;
function logEvent(msg) {
//	eLog.innerHTML = '<pre class="entry"><strong>' + eCount + ').</strong> ' + msg + '</pre>' + eLog.innerHTML;
	/*here I put also the time when the day is selected*/
	if (msg[0] == "S") {
		textSelected[ContorSelected]= msg;							
		var valueTime= new Date();
		time1[ContorSelected]=valueTime.getTime();
		delete valueTime;				
		ContorSelected++;
	}
	else
	/*here I put also the time when the day is deselected*/
	if (msg[0] == "D") {
		textDESelected[ContorDeselected] = msg;
		var valueTime= new Date();
		time2[ContorDeselected]=valueTime.getTime();				
		delete valueTime;				
		ContorDeselected++;
	}
	eCount++;
}
function dateToLocaleString(dt, cal) {
	var wStr = cal.cfg.getProperty("WEEKDAYS_LONG")[dt.getDay()];
	var dStr = dt.getDate();
	var mStr = cal.cfg.getProperty("MONTHS_LONG")[dt.getMonth()];
	var yStr = dt.getFullYear();
	return (wStr + ", " + dStr + " " + mStr + " " + yStr);
}
function mySelectHandler(type, args, obj) {
	var selected = args[0];
	var selDate = this.toDate(selected[0]);
		logEvent("SELECTED: " + dateToLocaleString(selDate, this));
}
;					
function myDeselectHandler(type, args, obj) {
	var deselected = args[0];
	var deselDate = this.toDate(deselected[0]);
		logEvent("DESELECTED: " + dateToLocaleString(deselDate, this));
}
;
YAHOO.example.calendar.cal1 = new YAHOO.widget.CalendarGroup("cal1",
		"cal1Container", {
			MULTI_SELECT : true
		}, {
			PAGES : 2
		});
YAHOO.example.calendar.cal1.selectEvent.subscribe(mySelectHandler,
		YAHOO.example.calendar.cal1, true);
YAHOO.example.calendar.cal1.deselectEvent.subscribe(myDeselectHandler,
	YAHOO.example.calendar.cal1, true);
	YAHOO.example.calendar.cal1.render();
}
YAHOO.util.Event.onDOMReady(YAHOO.example.calendar.init);
/*the time of selecting and deselecting should help me to visualize the last buttons which remains selected*/ 
	
/*check if two strings are equal*/
function StringEqual(stringD, stringS){
	lungd=stringD.length;
	lungS=stringS.length;		
	/*I check in stringD if stringS is there*/
	if ((stringD.indexOf(stringS))!=-1){
		return true;
	}
	else
		return false;
}

	/*eliminating duplicates*/	
function EliminateDuplicates(Selected, Cont){	
	if (Cont>=2){
		for (var i=Cont-1; i>=0; i--){
			for (var k=i-1; k>=0; k--){
				if ((Selected[i]==Selected[k])&&(Selected[i]!=0)){
					Selected[k]=0;						
				}
			}				
		}
	}
}	

function GetYMD(txtSelected, stringtest){
	//alert("I am  in my new function: "+stringtest);	
	if (stringtest=="year")
	{
		contor=txtSelected.length-1;
		var yearr = new Array();
		var year = new Array();
		var yeartext="";
		
		k=0;
		while (txtSelected.charAt(contor)!=' '){				
			yearr[k]=txtSelected.charAt(contor);
			contor--;
			k++;
		}
		for (var i = 1; i<=k; i++)
		{
			year[i]=yearr[k-i];
			yeartext=yeartext+year[i];
		}
		//alert("!! the year is : "+yeartext);
		return yeartext;
	}
	else 
	if (stringtest=="month")
	{		
		contor=contor-1;// to escape from space
		var monthr = new Array();
		var month = new Array();
		var monthtext="";
		
		k=0;
		while (txtSelected.charAt(contor)!=' '){				
			monthr[k]=txtSelected.charAt(contor);
			contor--;
			k++;
		}
		for (var i = 1; i<=k; i++)
		{
			month[i]=monthr[k-i];
			monthtext=monthtext+month[i];
		}
	//	alert("!! the month is : "+monthtext);
		return monthtext;
	}
	else
	if (stringtest=="day")
	{
		contor=contor-1;// to escape from space
		var dayr = new Array();
		var day = new Array();
		var daytext="";
		
		k=0;
		while (txtSelected.charAt(contor)!=' '){				
			dayr[k]=txtSelected.charAt(contor);
			contor--;
			k++;
		}
		for (var i = 1; i<=k; i++)
		{
			day[i]=dayr[k-i];
			daytext=daytext+day[i];
		}
	//	alert("!! the day is : "+daytext);
		return daytext;
	}
}
//var kml_2 = new Array(10000);
//var countkml = 0;

function selectedCalendar(){	
	/*I should eliminate duplicates from selected and also from deselected*/		
	EliminateDuplicates(textSelected, ContorSelected);
	EliminateDuplicates(textDESelected, ContorDeselected);
	
	/*I don't have duplicates here anymore */		
	/*deslected part is all the time less or equal then selected part*/
	for (var i=ContorDeselected-1;i>=0;i--){ 		
		for (var j=ContorSelected-1;j>=0;j--){ 
			if ((time1[j]<time2[i])&&(textDESelected[i]!=0)&&(textSelected[j]!=0)&&(StringEqual(textDESelected[i], textSelected[j])==true)){
				textSelected[j]=0;
			}				
		}
	}
	text="<calendar>";
	for (var i=0;i<ContorSelected;i++) {	
		if (textSelected[i]!=0){
			var stringMess=textSelected[i]; // now work with it
			text=text+"<year>";
				text=text+GetYMD(textSelected[i], "year");
			text=text+"</year>";
			text=text+"<month>";	
				text=text+GetYMD(textSelected[i], "month");
			text=text+"</month>";
			text=text+"<day>";
				text=text+GetYMD(textSelected[i], "day");
			text=text+"</day>";
		}
	}
	text =text+"</calendar>";
	if (window.XMLHttpRequest) {

         xmlHttp = new XMLHttpRequest();
    } else if (window.ActiveXObject) {

         xmlHttp = new ActiveXObject("Microsoft.XMLHTTP");
    } else {
	
         document.write("browser not supported");
    }	
	xmlHttp.onreadystatechange = function() {			
		if(xmlHttp.readyState==4){
			url = xmlHttp.responseXML.getElementsByTagName("url")[0].firstChild.nodeValue;
//			document.getElementById("content1").innerHTML = "kml:" + url; 
			loadkml_1(url);
		}			
	};
//	alert("step1");
	xmlHttp.open("POST","http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/RequestKml"); // no cache
//	xmlHttp.open("POST","http://localhost:8080/OracleSpatialWeb/RequestKml"); // no cache
//	alert("step2");
	xmlHttp.setRequestHeader('Content-Type','application/x-www-form-urlencoded; charset=UTF-8');
//	alert("step3");
	xmlHttp.send("xml2="+encodeURIComponent(text));
//	alert("step4");
	//everything (is)seems to be good till here
	
	//alert("data have been sent to the server! Loading...");
	//setTimeout("loadkml_2()", 5000);
		
}
var l=0;
function loadkml_2(url) {               
	for (var i=g_kml_counter-l; i<g_kml_counter;i++){
		g_kml_layer[i].setMap(); // this is deleting from the map the polygons 
	}
                //this put on the map the polygons!
        g_kml_layer[g_kml_counter] = new google.maps.KmlLayer(url, {
                            suppressInfoWindows: false,
                            preserveViewport: true,
                            map: map
                          });
	if (GBrowserIsCompatible()) {
                                // alert("I am where I should be");                     
                                // kml.setMap(map);
        } else {
		alert("Sorry, the Google Maps API is not compatible with this browser in kml overlay section");
        }
        g_kml_counter++;
        l=1;
}
// it must refresh the calendar and the map. 
// for now it just refresh the map
function refreshCalendar(){
	text="refresh"; // text is the data that I send. This must also to be refreshed.(for select and deselect from user)
        document.getElementById("content1_1").innerHTML = text;
        kml_2[g_kml_counter-1].setMap(); // this is deleting from the map the polygons that are on the map just one step before.

}

