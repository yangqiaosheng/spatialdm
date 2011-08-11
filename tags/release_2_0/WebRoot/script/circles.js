var raza;
var circles = new Array();
    function DistanceWidget(variable1, selectedPictures) {
        var radiusWidget = new RadiusWidget(map, variable1, selectedPictures); 
      }
      DistanceWidget.prototype = new google.maps.MVCObject();
 
 
       function RadiusWidget(map, variable1, selectedPictures) {
	var color;
	if (selectedPictures >= 10000){
	  color = '#CC0000'; // red
	}
	if ((selectedPictures >= 1000)&&(selectedPictures <= 9999)){
	  color = '#CCFF00'; // yellow
	}
	if ((selectedPictures >= 100)&&(selectedPictures <=999)){
	  color = '#00FF00'; // green
	}
	if ((selectedPictures >= 1)&&(selectedPictures <=99)){
	  color = '#0000CC'; // blue
	}
	if (selectedPictures == 0){
	  color = '#FFFFFF'; // white
	}	
	if (map.getZoom() == 1) {
	 // $("#legendInfo").append(" here1 ");	  
	  raza = 384000;
	}
	if (map.getZoom() == 2) {
	 // $("#legendInfo").append(" here2 ");		  
	  raza = 192000;
	}
	if (map.getZoom() == 3) {
	//  $("#legendInfo").append(" here3 ");		  
	  raza = 96000;
	}
	if (map.getZoom() == 4) {
	 // $("#legendInfo").append(" here4 ");		  
	  raza = 48000;
	}
	if (map.getZoom() == 5) {
	//  $("#legendInfo").append(" here4 ");		  
	  raza = 24000;
	}
	if (map.getZoom() == 6) {	  
	  raza = 12000;
	}	
	if (map.getZoom() == 7) {	  
	  raza = 6000;
	}	
	if (map.getZoom() == 8) {	
	  raza = 3000;
	}
	if (map.getZoom() == 9) {	 
	  raza = 1500;
	}
 	if (map.getZoom() == 10) {
 	  raza = 750;
 	}
	if (map.getZoom() == 11) {	 
	  raza = 325;
	}
	if (map.getZoom() >= 12) {	 
	  raza = 162.5;
	}
	// max 21 zoom levels
	  
	
         circles.push(new google.maps.Circle({
	   map: map,
           strokeWeight: 2,
	   fillOpacity: 0.6,
	   center: variable1,
	   radius: raza,
	   fillColor: color
         })); 
	 //$("#legendInfo").append("  "+raza);
	 circles[circles.length-1].setMap(map);     
       }
       RadiusWidget.prototype = new google.maps.MVCObject();  

function removeCircles(){
  if (circles.length != 0){
    for (var i=0; i<circles.length; i++){
      circles[i].setMap(null);  
    }
  }
  circles = new Array();
}




