
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
//get Year Month Days
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
	 //text is the string xml that I send to the server
	if (window.XMLHttpRequest) {

         xmlHttp = new XMLHttpRequest();
	} else if (window.ActiveXObject) {
         xmlHttp = new ActiveXObject("Microsoft.XMLHTTP");
	} else {	
         document.write("browser not supported");
	}	
	xmlHttp.onreadystatechange = function() {	
		  // 0—Uninitialized
		  // 1—Loading
		  // 2—Loaded
		  // 3—Interactive
		  // 4—Complete
		if(xmlHttp.readyState==4){
			url = xmlHttp.responseXML.getElementsByTagName("url")[0].firstChild.nodeValue;
			document.getElementById("content1").innerHTML = "kml:" + url; 
			loadkml(url);
		}
	};
	xmlHttp.open("POST","http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/RequestKml"); // no cache
//	xmlHttp.open("POST","http://localhost:8080/OracleSpatialWeb/RequestKml"); // no cache
	xmlHttp.setRequestHeader('Content-Type','application/x-www-form-urlencoded; charset=UTF-8');
	xmlHttp.send("xml2="+encodeURIComponent(text));	
}


function refreshCalendar(){
	text="refresh"; // text is the data that I send. This must also to be refreshed.(for select and deselect from user)
        document.getElementById("content1_1").innerHTML = text;
        kml_2[g_kml_counter-1].setMap(); // this is deleting from the map the polygons that are on the map just one step before.

}


