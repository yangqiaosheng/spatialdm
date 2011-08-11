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
	  };
	  map = new google.maps.Map(document.getElementById("map_canvas"), myOptions);			   
	  //activateZoom(true);  	
	  
	  google.maps.event.addListener(map, "rightclick", function(event){showContextMenu(event.latLng);});
	  google.maps.event.addListener(map, "click", function(event){hideContextMenu();});	 	
	  initboolSelected();// LoadPolygons.js 
	  agregationPolygonsAdd();
	  $('#EnabledOrDisabled').html(" Enabled <br/>");
	  scaleLevelOnStart();
	  google.maps.event.addListener(map, "tilesloaded", function() {	    	    
	    askHistogram();// in Histogram
	  });	    

};
function addRemoveElementsFromHistogram(){
	    $("#parent1").remove();
	    $("#parent2").remove();
	    $("#parent3").remove();
	    $("#parent4").remove();
	    $("#parent5").remove();
	    $("#button20").remove();

	    $("#histogramContent").append("<div id=parent1></div>");
	    $("#histogramContent").append("<div id=parent2></div>");
	    $("#histogramContent").append("<div id=parent3></div>");
	    $("#histogramContent").append("<div id=parent4></div>");
	    $("#histogramContent").append("<div id=parent5></div>");
	  
}
             
function start() {	
	$("#maxContainer").hide();	
	initialize1();	
}

function scaleLevelOnStart(){
	if (map.getZoom() <= 5 ) {
		$("#scaleLevel").html("1");
	}
	if (map.getZoom() == 6) {
		$("#scaleLevel").html("2");
	}
	if (map.getZoom() == 7) {
		$("#scaleLevel").html("3");
	}
	if (map.getZoom() == 8) {
		$("#scaleLevel").html("4");
	}
	if (map.getZoom() == 9) {
		$("#scaleLevel").html("5");
	}
	if (map.getZoom() == 10) {
		$("#scaleLevel").html("6");
	}
	if (map.getZoom() >= 11) {
		$("#scaleLevel").html("7");
	}	
}


