var boolean = false;
var number= new Array(200);
var field= new Array(200);
var nameString = new Array(200); // in this string I map months, years, days hours.
var text="";
// this function is executed in the begining onload on body
function loadStart()
{
	for (var i=1;i<100;i++)// here are actually 73 elements in sjp, but dosn't matter if I put more
	{
		field[i] = document.getElementById('controltime' + i);
		number[i]=0;
		switch (i) {
		case 1:
			nameString[i] = "2005";
			break;
		case 2:
			nameString[i] = "2006";
			break;
		case 3:
			nameString[i] = "2007";
			break;
		case 4:
			nameString[i] = "2008";
			break;
		case 5:
			nameString[i] = "2009";
			break;
		case 6:
			nameString[i] = "2010";
			break;
		// ***********************************************************************
		case 7:
			nameString[i] = "January";
			break;
		case 8:
			nameString[i] = "February";
			break;
		case 9:
			nameString[i] = "March";
			break;
		case 10:
			nameString[i] = "April";
			break;
		case 11:
			nameString[i] = "May";
			break;
		case 12:
			nameString[i] = "June";
			break;
		case 13:
			nameString[i] = "July";
			break;
		case 14:
			nameString[i] = "August";
			break;
		case 15:
			nameString[i] = "September";
			break;
		case 16:
			nameString[i] = "October";
			break;
		case 17:
			nameString[i] = "November";
			break;
		case 18:
			nameString[i] = "December";
			break;
		// ***********************************************************************

		case 19:
			nameString[i] = "1";
			break;
		case 20:
			nameString[i] = "2";
			break;
		case 21:
			nameString[i] = "3";
			break;
		case 22:
			nameString[i] = "4";
			break;
		case 23:
			nameString[i] = "5";
			break;
		case 24:
			nameString[i] = "6";
			break;
		case 25:
			nameString[i] = "7";
			break;
		case 26:
			nameString[i] = "8";
			break;
		case 27:
			nameString[i] = "9";
			break;
		case 28:
			nameString[i] = "10";
			break;
		case 29:
			nameString[i] = "11";
			break;
		case 30:
			nameString[i] = "12";
			break;
		case 31:
			nameString[i] = "13";
			break;
		case 32:
			nameString[i] = "14";
			break;
		case 33:
			nameString[i] = "15";
			break;
		case 34:
			nameString[i] = "16";
			break;
		case 35:
			nameString[i] = "17";
			break;
		case 36:
			nameString[i] = "18";
			break;
		case 37:
			nameString[i] = "19";
			break;
		case 38:
			nameString[i] = "20";
			break;
		case 39:
			nameString[i] = "21";
			break;
		case 40:
			nameString[i] = "22";
			break;
		case 41:
			nameString[i] = "23";
			break;
		case 42:
			nameString[i] = "24";
			break;
		case 43:
			nameString[i] = "25";
			break;
		case 44:
			nameString[i] = "26";
			break;
		case 45:
			nameString[i] = "27";
			break;
		case 46:
			nameString[i] = "28";
			break;
		case 47:
			nameString[i] = "29";
			break;
		case 48:
			nameString[i] = "30";
			break;
		case 49:
			nameString[i] = "31";
			break;

		// ***********************************************************************
		case 50:
			nameString[i] = "0";
			break;
		case 51:
			nameString[i] = "1";
			break;
		case 52:
			nameString[i] = "2";
			break;
		case 53:
			nameString[i] = "3";
			break;
		case 54:
			nameString[i] = "4";
			break;
		case 55:
			nameString[i] = "5";
			break;
		case 56:
			nameString[i] = "6";
			break;
		case 57:
			nameString[i] = "7";
			break;
		case 58:
			nameString[i] = "8";
			break;
		case 59:
			nameString[i] = "9";
			break;
		case 60:
			nameString[i] = "10";
			break;
		case 61:
			nameString[i] = "11";
			break;
		case 62:
			nameString[i] = "12";
			break;
		case 63:
			nameString[i] = "13";
			break;
		case 64:
			nameString[i] = "14";
			break;
		case 65:
			nameString[i] = "15";
			break;
		case 66:
			nameString[i] = "16";
			break;
		case 67:
			nameString[i] = "17";
			break;
		case 68:
			nameString[i] = "18";
			break;
		case 69:
			nameString[i] = "19";
			break;
		case 70:
			nameString[i] = "20";
			break;
		case 71:
			nameString[i] = "21";
			break;
		case 72:
			nameString[i] = "22";
			break;
		case 73:
			nameString[i] = "23";
			break;
			// ***********************************************************************		
		case 74:
			nameString[i]="Monday";
			break;
		case 75:
			nameString[i]="Tuesday";
			break;
		case 76:
			nameString[i]="Wednesday";
			break;
		case 77:
			nameString[i]="Thursday";
			break;
		case 78:
			nameString[i]="Friday";
			break;
		case 79:
			nameString[i]="Saturday";
			break;
		case 80:
			nameString[i]="Sunday";
			break;
		}
	}
	
}
//this fucntion receive a parameter as number of the argument- even if the parameter is not visible
function selectbutton()
{			
	var argv = selectbutton.arguments;
	number[argv[0]]++;	// even, odd for selection deselection
	if (number[argv[0]]%2!=0)
	{			
		field[argv[0]].style.background='#ffffff';		
	}
	else
	{	
		field[argv[0]].style.background='#009933';
	}	
}

// load xml string parser
//However, before an XML document can be accessed and manipulated, it must be loaded into an XML DOM object.
//An XML parser reads XML, and converts it into an XML DOM object that can be accessed with JavaScript. 
function loadXMLString(txt) {
	if (window.DOMParser) {
		parser = new DOMParser();
		xmlDoc = parser.parseFromString(txt, "text/xml");
	} else // Internet Explorer
	{
		xmlDoc = new ActiveXObject("Microsoft.XMLDOM");
		xmlDoc.async = "false";
		xmlDoc.loadXML(txt);
	}	
	return xmlDoc;
}
var t=1;
var kml1_1=null;
// areas1.kml is from the timecontroller1


function ask() {
	text=""; // text is the data that I send. This must also to be refreshed.
	document.getElementById("content1").innerHTML = text;
	
	text=text+"<calendar>";
	//*********************************************************
	text=text+"<years>"; // first child // years
	for (var i =1; i<=6; i++){	// in the beginning I have the years	
		if (number[i] % 2 != 0) { // I take the selections
			text=text+"<year>"+nameString[i]+"</year>";
		}
	}	
	text=text+"</years>";	
	//*********************************************************
	text=text+"<months>";// second child // months
	for (var i =7; i<=18; i++){	// in the beginning I have the months	
		if (number[i] % 2 != 0) { // I take the selections
			text=text+"<month>"+nameString[i]+"</month>";
		}
	}	
	text=text+"</months>";
	//*********************************************************
	text=text+"<days>";// third child // days
	for (var i =19; i<=49; i++){	// in the beginning I have the days	
		if (number[i] % 2 != 0) { // I take the selections
			text=text+"<day>"+nameString[i]+"</day>";
		}
	}	
	text=text+"</days>";		
	//*********************************************************
	text=text+"<hours>";
	for (var i =50; i<=73; i++){	
		if (number[i] % 2 != 0) { 
			text=text+"<hour>"+nameString[i]+"</hour>";
		}
	}
	text=text+"</hours>";
	//********************************************************* for the day of the week
	text=text+"<weekdays>";
	for (var i =74; i<=80; i++){	
		if (number[i] % 2 != 0) { 
			text=text+"<weekday>"+nameString[i]+"</weekday>";
		}
	}
	text=text+"</weekdays>";
	//*********************************************************
	
	text=text+"</calendar>";
	//ext field
	xmlDoc=loadXMLString(text);
	 
	year=xmlDoc.getElementsByTagName("year"); // 1-6
	 
	month = xmlDoc.getElementsByTagName("month");// 7-18
	 
	day = xmlDoc.getElementsByTagName("day");// 19 - 49
	 
	hour=xmlDoc.getElementsByTagName("hour");//50 - 73
	
	weekday=xmlDoc.getElementsByTagName("weekday"); //74-80
	
	if (window.XMLHttpRequest){
		xmlhttp=new XMLHttpRequest();

	}
	xmlhttp.onreadystatechange = function() {			
		if(xmlhttp.readyState==4){
			url = xmlhttp.responseXML.getElementsByTagName("url")[0].firstChild.nodeValue;
			document.getElementById("content1").innerHTML = "kml:" + url; 
			loadkml_1(url);
		}			
	};
	 xmlhttp.open("POST","http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/RequestKml"); // no cache
	 //xmlhttp.open("POST","http://localhost:8080/OracleSpatialWeb/RequestKml"); // no cache
	 xmlhttp.setRequestHeader('Content-Type','application/x-www-form-urlencoded; charset=UTF-8');
	 xmlhttp.send("xml="+encodeURIComponent(text));
    //alert("data have been sent to the server"+text);	 	
	//alert("loading...");
	//setTimeout("loadkml_1()", 5000);
//	loadkml_1();
}

function loadkml_1(url)
{
	alert(url);
	kml1_1 = new google.maps.KmlLayer(url, {
                    suppressInfoWindows: false,
                    preserveViewport: true,
                    map: map
                  });

               //alert("put");
                if (GBrowserIsCompatible()) {
                        // alert("I am where I should be");                     
//                        kml.setMap(map);
                } else {
                        alert("Sorry, the Google Maps API is not compatible with this browser in kml overlay section");
                	}
}

function refreshButtons() { // ok number[i] reverse to 0
        for ( var i = 1; i < 100; i++) {
                if (number[i] != 0) {
                        field[i].style.background = '#009933';
                        number[i] = 0;
                }
        }

        text=""; // text is the data that I send. This must also to be refreshed.(for select and deselect from user)
        document.getElementById("content1").innerHTML = text;	
	kml1_1.setMap();
	kml1_1=null;
}

function radio_buttons() {
	text_R_Buttons = "";
	for ( var i = 0; i < document.radio_form.time.length; i++) {
		if (document.radio_form.time[i].checked) {
			if (i == 0) {
				//alert("year selection");
				text_R_Buttons = text_R_Buttons
						+ "<select name='numbers_years'><option value='1'>1</option><option value='2'>2</option><option value='3'>3</option><option value='4'>4</option><option value='5'>5</option><option value='6'>6</option></select>";
				document.getElementById("unit").innerHTML = text_R_Buttons;
				text_R_Buttons = "";
				break;
			}
			if (i == 1) {
				//alert("month selection");
				text_R_Buttons = text_R_Buttons
						+ "<select name='numbers_months'><option value='1'>1</option>"
						+ "<option value='2'>2</option>"
						+ "<option value='3'>3</option>"
						+ "<option value='4'>4</option>"
						+ "<option value='5'>5</option>"
						+ "<option value='6'>6</option>"
						+ "<option value='7'>7</option>"
						+ "<option value='8'>8</option>"
						+ "<option value='9'>9</option>"
						+ "<option value='10'>10</option>"
						+ "<option value='11'>11</option>"
						+ "<option value='12'>12</option>";
				document.getElementById("unit").innerHTML = text_R_Buttons;
				text_R_Buttons = "";
				break;
			}
			if (i == 2) {
				text_R_Buttons = "";
				//alert("day selection");
				text_R_Buttons = text_R_Buttons
						+ "<select name='numbers_days'><option value='1'>1</option>"
						+ "<option value='2'>2</option>"
						+ "<option value='3'>3</option>"
						+ "<option value='4'>4</option>"
						+ "<option value='5'>5</option>"
						+ "<option value='6'>6</option>"
						+ "<option value='7'>7</option>"
						+ "<option value='8'>8</option>"
						+ "<option value='9'>9</option>"
						+ "<option value='10'>10</option>"
						+ "<option value='11'>11</option>"
						+ "<option value='12'>12</option>"
						+ "<option value='13'>13</option>"
						+ "<option value='14'>14</option>"
						+ "<option value='15'>15</option>"
						+ "<option value='16'>16</option>"
						+ "<option value='17'>17</option>"
						+ "<option value='18'>18</option>"
						+ "<option value='19'>19</option>"
						+ "<option value='20'>20</option>"
						+ "<option value='21'>21</option>"
						+ "<option value='22'>22</option>"
						+ "<option value='23'>23</option>"
						+ "<option value='24'>24</option>"
						+ "<option value='25'>25</option>"
						+ "<option value='26'>26</option>"
						+ "<option value='27'>27</option>"
						+ "<option value='28'>28</option>"
						+ "<option value='29'>29</option>"
						+ "<option value='30'>30</option>"
						+ "<option value='31'>31</option>";
				document.getElementById("unit").innerHTML = text_R_Buttons;
				text_R_Buttons = "";
				break;
			}
			if (i == 3)
				text_R_Buttons = "";
			//alert("hour selection");
			text_R_Buttons = text_R_Buttons
					+ "<select name='numbers_days'><option value='1'>1</option>"
					+ "<option value='2'>2</option>"
					+ "<option value='3'>3</option>"
					+ "<option value='4'>4<send/option>"
					+ "<option value='5'>5</option>"
					+ "<option value='6'>6</option>"
					+ "<option value='7'>7</option>"
					+ "<option value='8'>8</option>"
					+ "<option value='9'>9</option>"
					+ "<option value='10'>10</option>"
					+ "<option value='11'>11</option>"
					+ "<option value='12'>12</option>"
					+ "<option value='13'>13</option>"
					+ "<option value='14'>14</option>"
					+ "<option value='15'>15</option>"
					+ "<option value='16'>16</option>"
					+ "<option value='17'>17</option>"
					+ "<option value='18'>18</option>"
					+ "<option value='19'>19</option>"
					+ "<option value='20'>20</option>"
					+ "<option value='21'>21</option>"
					+ "<option value='22'>22</option>"
					+ "<option value='23'>23</option>"
					+ "<option value='24'>24</option>";
			document.getElementById("unit").innerHTML = text_R_Buttons;
			text_R_Buttons = "";
			break;

		}
	}

}
