//google.load("maps", "2"); // I load the content of the map
//Map example

// google.maps is generic. after that comes the class  which is in the API 3
var map = null;
var myOptions = null;
var geocoder =  null;
var geocoder = null;
 
function initialize1()
{	
	  var myLatlng = new google.maps.LatLng(50.80, 7.12);
	  myOptions = {
	    zoom: 8, 
	    center: myLatlng,
	    mapTypeControl: true,			    
	    navigationControl: true,
	    navigationControlOptions: {position: google.maps.ControlPosition.RIGHT},
	    mapTypeId: google.maps.MapTypeId.ROADMAP
	  }
	  map = new google.maps.Map(document.getElementById("map_canvas"), myOptions);			   
	  //activateZoom(true);  	
	  
	  google.maps.event.addListener(map, "rightclick", function(event){showContextMenu(event.latLng);});
	  google.maps.event.addListener(map, "click", function(event){hideContextMenu();});	 	
	  initboolSelected();// LoadPolygons.js 
	  agregationPolygonsAdd();
	  $('#EnabledOrDisabled').html(" Enabled <br/>");
	  scaleLevelOnStart()
}
             
function start() {	
	initialize1();
}

function scaleLevelOnStart(){
	if (map.getZoom() < 8) {
		$("#scaleLevel").html("1");	
	}
	if ((map.getZoom() < 10) && (map.getZoom() >= 8)) {				
		$("#scaleLevel").html("2");			
	}
	if ((map.getZoom() < 11) && (map.getZoom() >= 10)) {
		$("#scaleLevel").html("3");				
	}
	if ((map.getZoom() < 12) && (map.getZoom() >= 11)) {				
		$("#scaleLevel").html("4");
	}
	if (map.getZoom() >= 12) {
		$("#scaleLevel").html("5");
	}	
}


