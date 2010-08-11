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
			google.maps.event.addListener(map, 'zoom_changed', function() {
	//		    alert("map.getBounds(): "+map.getBounds()+"\n map.getCenter():"+map.getCenter()+"\n map.getZoom(): "+map.getZoom());
				//I deleted the cercles because they have to have different size for the polygons that are generated
				if (map.getZoom()<8){
					// this function is in anothe .js file but I used here as if it is in this .js file
					 loadkml_1("http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/kml/FlickrDeWestArea_Total_80000.kml");

				}
				if ((map.getZoom()<10)&&(map.getZoom()>=9)){
					loadkml_1("http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/kml/FlickrDeWestArea_Total_40000.kml");					
				}
				if ((map.getZoom()<11)&&(map.getZoom()>=10)){
                                        loadkml_1("http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/kml/FlickrDeWestArea_Total_20000.kml");
                                }
				 if ((map.getZoom()<12)&&(map.getZoom()>=11)){
                                        loadkml_1("http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/kml/FlickrDeWestArea_Total_10000.kml");
                                }
				 if ((map.getZoom()>=12)){
                                        loadkml_1("http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/kml/FlickrDeWestArea_Total_5000.kml");
                                }				
  			});	
					    
		activateZoom(true);		
	}
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

function ManageTabPanelDisplay() {
	var idlist = new Array('tab1focus', 'tab2focus', 'tab3focus', 'tab4focus',
			'tab1ready', 'tab2ready', 'tab3ready', 'tab4ready', 'content1',
			'content2', 'content3', 'content4');
	if (arguments.length < 1) {
		return;
	}
	for ( var i = 0; i < idlist.length; i++) {
		var block = false;
		for ( var j = 0; j < arguments.length; j++) {
			if (idlist[i] == arguments[j]) {
				block = true;
				break;
			}
		}
		if (block) {
			document.getElementById(idlist[i]).style.display = "block";
		} else {
			document.getElementById(idlist[i]).style.display = "none";
		}
	}
}
function start() {
	initialize1();
}
function refresh() {
	window.location.reload();
}
// add maybe some modified markers from linux.
function mapAddMakers() {	 
	var marker = new google.maps.Marker({
		position: new google.maps.LatLng(50.73, 6.12),
		map: map,
		title: "Marker 1"
	});
	var marker = new google.maps.Marker({
		position: new google.maps.LatLng(51.73, 7.12),
		map: map,
		title: "Marker 2"
	});
	var marker = new google.maps.Marker({
		position: new google.maps.LatLng(50.73, 7.12),
		map: map,
		title: "Marker 3"
	});
}
	

function drawPolylines() {                                         	 	                                                                         var flightPlanCoordinates= [new google.maps.LatLng(50.73, 6.12),
	                            new google.maps.LatLng(51.73, 7.12),
	                            new google.maps.LatLng(50.73, 7.12),
	                            new google.maps.LatLng(50.73, 6.12)];

	 var flightPath = new google.maps.Polyline({
	      path: flightPlanCoordinates,
	      strokeColor: "#FF0000",
	      strokeOpacity: 1.0,
	      strokeWeight: 2 // the thickness of a line
	    });
	 flightPath.setMap(map);

}
function addMapGPolygon(){
	 var StartTriangle;
	 
	 var triangleCoords = [
	                       new google.maps.LatLng(50.73, 6.12),
	                       new google.maps.LatLng(51.73, 7.12),
	                       new google.maps.LatLng(50.73, 7.12)
	                   ];
	 StartTriangle = new google.maps.Polygon({
	      paths: triangleCoords,
	      strokeColor: "#FF0000",
	      strokeOpacity: 0.8,
	      strokeWeight: 3,
	      fillColor: "#FF0000",
	      fillOpacity: 0.35
	    });
	 StartTriangle.setMap(map);
	 google.maps.event.addListener(StartTriangle, 'click', showArrays);	    
	 infowindow = new google.maps.InfoWindow();
	  

	  function showArrays(event) {

	    // Since this Polygon only has one path, we can call getPath()
	    // to return the MVCArray of LatLngs
	    var vertices = this.getPath();

	    var contentString = "<b>Triangle Polygon</b><br />";
	    contentString += "Clicked Location: <br />" + event.latLng.lat() + "," + event.latLng.lng() + "<br />";

	    // Iterate over the vertices.
	    for (var i =0; i < vertices.length; i++) {
	      var xy = vertices.getAt(i);
	      contentString += "<br />" + "Coordinate: " + i + "<br />" + xy.lat() +"," + xy.lng();
	    }

	    // Replace our Info Window's content and position
	    infowindow.setContent(contentString);
	    infowindow.setPosition(event.latLng);

	    infowindow.open(map);
	  }  
}

function SearchLocation()
{
	addressInput = document.getElementById('search').value;
	geocoder = new google.maps.Geocoder();
	if (addressInput!="")		
	{
		alert ("Search: "+addressInput);
		if (geocoder){
			geocoder.geocode( { 'address': addressInput}, function(results, status) {
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
	else
	{
		alert ("No search location");
	}
}

