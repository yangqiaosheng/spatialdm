//google.load("maps", "2"); // I load the content of the map
//Map example

// google.maps is generic. after that comes the class  which is in the API 3
var map = null;
var myOptions = null;
var geocoder =  null;
var geocoder = null;


 
function initialize1()
{
	if (GBrowserIsCompatible()) {
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
	  activateZoom(true);  	
	  google.maps.event.addListener(map, "rightclick", function(event){showContextMenu(event.latLng);});
	  google.maps.event.addListener(map, "click", function(event){hideContextMenu();});
	}   
}
function hideContextMenu(){
   $('.contextmenu').remove();
}
function showContextMenu(caurrentLatLng) {
        var projection;
        var contextmenuDir;
        projection = map.getProjection() ;
        $('.contextmenu').remove();
        contextmenuDir = document.createElement("div");
        contextmenuDir.className  = 'contextmenu';
        contextmenuDir.innerHTML = "<div class = 'context' ><a id='menu1'><div class=context>zoom in<\/div><\/a><a id='menu2'><div class=context>zoom out<\/div><\/a><a id='menu3'><div class=context>Set Center Here<\/div><\/a></div>";
        $(map.getDiv()).append(contextmenuDir);
        setMenuXY(caurrentLatLng);
        contextmenuDir.style.visibility = "visible";
	$('#menu1, #menu2, #menu3').hover(function () {
	   var cssObj = {
	    'color' : 'blue' 
	  }
	  $(this).css(cssObj);	  
	}, 
	function () {    
	  var cssObj = {
	    'color' : 'black'
	  }
	  $(this).css(cssObj);
	});
	$('#menu1').click(function(){
	  map.setCenter(caurrentLatLng);
	  map.setZoom(map.getZoom()+1);
	  hideContextMenu();
	});  
	$('#menu2').click(function(){
	  map.setCenter(caurrentLatLng);
	  map.setZoom(map.getZoom()-1);
	  hideContextMenu();
	});
	$('#menu3').click(function(){
	 map.setCenter(caurrentLatLng);
	 hideContextMenu();
	});
}

function setMenuXY(caurrentLatLng){
    var mapWidth = $('#map_canvas').width();
    var mapHeight = $('#map_canvas').height();
    var menuWidth = $('.contextmenu').width();
    var menuHeight = $('.contextmenu').height();
    var clickedPosition = getCanvasXY(caurrentLatLng);
    var x = clickedPosition.x ;
    var y = clickedPosition.y ;
    if((mapWidth - x ) < menuWidth)
         x = x - menuWidth;
    if((mapHeight - y ) < menuHeight)
        y = y - menuHeight;
    $('.contextmenu').css('left',x);
    $('.contextmenu').css('top',y);
}

function getCanvasXY(caurrentLatLng){
     var scale = Math.pow(2, map.getZoom());
     var nw = new google.maps.LatLng(
         map.getBounds().getNorthEast().lat(),
         map.getBounds().getSouthWest().lng()
     );
     var worldCoordinateNW = map.getProjection().fromLatLngToPoint(nw);
     var worldCoordinate = map.getProjection().fromLatLngToPoint(caurrentLatLng);
     var caurrentLatLngOffset = new google.maps.Point(
         Math.floor((worldCoordinate.x - worldCoordinateNW.x) * scale),
         Math.floor((worldCoordinate.y - worldCoordinateNW.y) * scale)
     );
     return caurrentLatLngOffset;
}
    
function activateZoom(isChecked) {
	if (isChecked) {
	  map.setOptions( myOptions = {
	  scrollwheel:true
	});
	} else {
	  map.setOptions( myOptions = {
	  scrollwheel:false
	  });
	}
}
function codeAddress() {
    var address = document.getElementById("address").value;
    if (geocoder) {
      geocoder.geocode( { 'address': address}, function(results, status) {
        if (status == google.maps.GeocoderStatus.OK) {
          map.setCenter(results[0].geometry.location);
          var marker = new google.maps.Marker({
              map: map, 
              position: results[0].geometry.location
          });
        } else {
          alert("Geocode was not successful for the following reason: " + status);
        }
      });
    }
  }

function start() {
	initialize1();
}
