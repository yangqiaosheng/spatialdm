var zoom1=0;
var zoom2=0;
var zoom3=0;
var zoom4=0;
var zoom5=0;

function sendToServerCalendarData(headerXML, bodyXML){      
      var textToSend = headerXML+""+bodyXML;
      textToSend = "<request>"+textToSend+"</request>";
      //alert("textToSend: "+textToSend);
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
		loadkml(url);
		//$("#legendInfo").html("Sent:" + response);
	    }
	  };
	  xmlHttp.open("POST","http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/RequestKml");
	  xmlHttp.setRequestHeader('Content-Type','application/x-www-form-urlencoded; charset=UTF-8');
	  xmlHttp.send("xml="+encodeURIComponent(textToSend));
    }
function  sendToServer_ScreenCenter_ScreenBounds(headerXML, bodyXML){       
	//alert(" \n bounds:"+screenBounds+ "\n center:"+screenCenter);
	var screenBounds_screenCenter = headerXML+""+bodyXML;
	screenBounds_screenCenter = "<request>"+screenBounds_screenCenter+"</request>";
	//alert("textToSend: "+screenBounds_screenCenter);
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
	      //alert("aaaaaaaaaaa");
		// here is expected just a string that is the number.
		//url = xmlHttp.responseXML.getElementsByTagName("url")[0].firstChild.nodeValue;
	      url = xmlHttp.responseXML.getElementsByTagName("url")[0].firstChild.nodeValue;
	      //alert("url:"+url);
	      if ((map.getZoom() < 8)&&(zoom1==0)) { // far from the earth 						
		//alert("url:"+url);
		loadkml(url);
		zoom1=1;
		zoom2=0;
		zoom3=0;
		zoom4=0;
		zoom5=0;		
		$("#item1").append("\n 0 8)");
		$("#item2").append("\n"+map.getZoom());
	      }	    
	      if (((map.getZoom() < 10) && (map.getZoom() >= 8))&&(zoom2==0)) {		
	      //alert("url:"+url);
                loadkml(url);
		zoom1=0;
		zoom2=1;
		zoom3=0;
		zoom4=0;
		zoom5=0;		
		$("#item1").append("\n [8,10)");
		$("#item2").append("\n"+map.getZoom());
	      }	    
	      if (((map.getZoom() < 11) && (map.getZoom() >= 10))&&(zoom3==0)) {		
		//alert("url:"+url);
                loadkml(url);
		zoom1=0;
		zoom2=0;
		zoom3=1;
		zoom4=0;
		zoom5=0;		
		$("#item1").append("\n [10,11)");
		$("#item2").append("\n"+map.getZoom());
	      }
	    
	      if (((map.getZoom() < 12) && (map.getZoom() >= 11))&&(zoom4==0)) {		
	      //alert("url:"+url);
                loadkml(url);
		zoom1=0;
		zoom2=0;
		zoom3=0;
		zoom4=1;
		zoom5=0;		
		$("#item1").append("\n [11,12)");
		$("#item2").append("\n"+map.getZoom());
	      }	    
	      if ((map.getZoom() >= 12)&&(zoom5==0)) {				
		//alert("url:"+url);
                loadkml(url);
		zoom1=0;
		zoom2=0;
		zoom3=0;
		zoom4=0;
		zoom5=1;		
		$("#item1").append("\n [12..)");
		$("#item2").append("\n"+map.getZoom());
	      }
	      $("#legendInfo").html("screenBounds:" + url);
	    }
	  };
	  xmlHttp.open("POST","http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/RequestKml")
	  xmlHttp.setRequestHeader('Content-Type','application/x-www-form-urlencoded; charset=UTF-8');
	  xmlHttp.send("xml="+encodeURIComponent(screenBounds_screenCenter));	 
    }