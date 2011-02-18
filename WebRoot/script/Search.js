var markerS = new Array();
var ts = -1;
function SearchLocation()
{
	ts++;
	addressInput = document.getElementById('search').value;
	geocoder = new google.maps.Geocoder();
	
	if (addressInput!="")		
	{		
		if (geocoder){
			geocoder.geocode( { 'address': addressInput}, function(results, status) {
		        if (status == google.maps.GeocoderStatus.OK) {
		          map.setCenter(results[0].geometry.location);
		          setMarkerSearch(results[0].geometry.location);			  
 		        } else {		          
		        }
		      });
		}
 	}	
	else
	{
		alert ("No search location");
	}
}

function setMarkerSearch(a){
   markerS[ts] = new google.maps.Marker({
	position: a,
	map: map
    });
    setListener(markerS[ts]);    
}

function setListener(b){    
	 google.maps.event.addListener(b, 'click', function() {        	
	 b.setMap(null);
	});
}

function keyEvent(e){    
    if (e.keyCode == 13) // 13 = enter key
    { 
      SearchLocation();
    }    
} 


