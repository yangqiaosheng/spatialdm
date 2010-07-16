var menuids=new Array("verticalmenu") 
var submenuoffset=-2

function createcssmenu(){
for (var i=0; i<menuids.length; i++){
  var ultags=document.getElementById(menuids[i]).getElementsByTagName("ul")
for (var t=0; t<ultags.length; t++){
    var spanref=document.createElement("span")
		spanref.className="arrowdiv"
		spanref.innerHTML="&nbsp;&nbsp;"
		ultags[t].parentNode.getElementsByTagName("a")[0].appendChild(spanref)
    ultags[t].parentNode.onmouseover=function(){
    this.getElementsByTagName("ul")[0].style.left=this.parentNode.offsetWidth+submenuoffset+"px"
    this.getElementsByTagName("ul")[0].style.display="block"
    }
    ultags[t].parentNode.onmouseout=function(){
    this.getElementsByTagName("ul")[0].style.display="none"
    }
    }
  }
}


if (window.addEventListener)
window.addEventListener("load", createcssmenu, false)
else if (window.attachEvent)
window.attachEvent("onload", createcssmenu)


var t=1;
var kml=null;
kml = new google.maps.KmlLayer('http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/kml/areas1.kml?' + new Date().getTime(), {
		    suppressInfoWindows: false,
		    preserveViewport: true,
		    map: map
		  } );
function addxml1() {
	//good	
	if (t % 2 != 0) {
		t++;		
		//alert("put");
		if (GBrowserIsCompatible()) {
			// alert("I am where I should be");			
			kml.setMap(map);
		} else {
			alert("Sorry, the Google Maps API is not compatible with this browser in kml overlay section");
		}
		return;		
	}
	if (t % 2 == 0) {				
		t++;
		//alert("delete");			
		kml.setMap();
		//alert("finish");
	}
} 

// it is working
var t2=1;
var kml2=null;
kml2 = new google.maps.KmlLayer(
		'http://sophisnerd.org/peca/project0.3/WebContent/kml/areasWOC.kml?' + new Date().getTime(), {
		    suppressInfoWindows: false,
		    preserveViewport: true,
		    map: map
		  } );
function addxml2() {
	
	//good	
	if (t2 % 2 != 0) {
		t2++;		
		//alert("put");
		if (GBrowserIsCompatible()) {
			// alert("I am where I should be");			
			kml2.setMap(map);
		} else {
			alert("Sorry, the Google Maps API is not compatible with this browser in kml overlay section");
		}
		return;		
	}
	if (t2 % 2 == 0) {				
		t2++;
		//alert("delete");			
		kml2.setMap();
		//alert("finish");
	}
}

function addxml3() {
//	if (GBrowserIsCompatible()) {		
//		var kml = new GGeoXml("http://sophisnerd.org/peca/project0.3/WebContent/kml/aresoriginal.kml?"+ new Date().getTime()); // fooling the browser
//		map.addOverlay(kml);
//	} else {
//		alert("Sorry, the Google Maps API is not compatible with this browser in kml overlay section");
//	}
}

function AddPolynomsOnMilan()
{
//	if (GBrowserIsCompatible()) 
//	{ 											
//		var kml = new GGeoXml("http://sophisnerd.org/peca/project0.3/WebContent/kml/milanpoly.kml?" + new Date().getTime()); // fooling the browser 
//		map.addOverlay(kml); 
//	}					    			
//	else 
//	{
//		alert("Sorry, the Google Maps API is not compatible with this browser in kml overlay section");
//	}
}
