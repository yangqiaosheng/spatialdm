
	var circles = new Array();
	function DistanceWidget(variable1, selectedPictures) {
	  var radiusWidget =  RadiusWidget(map, variable1, selectedPictures); 
	}
      // DistanceWidget.prototype = new google.maps.MVCObject();
  
 
       function RadiusWidget(map, variable1, selectedPictures) {
	var color;
	var raza;
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
	  $("#zoomMapLevel").html(" "+map.getZoom());
	 // $("#legendInfo").append(" here1 ");	  
	  raza = 96000;
	}
	if (map.getZoom() == 2) {
	  $("#zoomMapLevel").html(" "+map.getZoom());  
	  raza = 96000;
	}
	if (map.getZoom() == 3) {
	  $("#zoomMapLevel").html(" "+map.getZoom());	  
	  raza = 96000;
	}
	if (map.getZoom() == 4) {
	  $("#zoomMapLevel").html(" "+map.getZoom());	  
	  raza = 48000;
	}
	if (map.getZoom() == 5) {//
	  $("#zoomMapLevel").html(" "+map.getZoom());
	  raza = 26000;
	}
	if (map.getZoom() == 6) {//ok
	  $("#zoomMapLevel").html(" "+map.getZoom());
	  raza = 14000;
	}	
	if (map.getZoom() == 7) {//ok  
	 $("#zoomMapLevel").html(" "+map.getZoom());
	  raza = 8000;
	}	
	if (map.getZoom() == 8) {//ok	
	 $("#zoomMapLevel").html(" "+map.getZoom());
	  raza = 5000;
	}
	if (map.getZoom() == 9) {//ok
	  $("#zoomMapLevel").html(" "+map.getZoom());
	  raza = 2500;
	}
 	if (map.getZoom() == 10) {//ok
	 $("#zoomMapLevel").html(" "+map.getZoom());
 	  raza = 1250;
 	}
	if (map.getZoom() == 11) {//ok
	  $("#zoomMapLevel").html(" "+map.getZoom()); 
	  raza = 625;
	}
	if (map.getZoom() == 12) {//ok
	  $("#zoomMapLevel").html(" "+map.getZoom());	 
	  raza = 312.5;
	}
	if (map.getZoom() == 13) {//ok
	  $("#zoomMapLevel").html(" "+map.getZoom());
	    raza = 156.25;
	}
	if (map.getZoom() == 14) {//ok
	  $("#zoomMapLevel").html(" "+map.getZoom());
	    raza = 78.125;
	}
	if (map.getZoom() == 15) {//ok
	   $("#zoomMapLevel").html(" "+map.getZoom());
	   raza = 39.0625;
	}
	if (map.getZoom() == 16) {//ok
	  $("#zoomMapLevel").html(" "+map.getZoom());
	   raza = 19.53125;
	}
	if (map.getZoom() == 17) {
	  $("#zoomMapLevel").html(" "+map.getZoom());
	   raza = 19.53125;
	}
	if (map.getZoom() == 18) {
	  $("#zoomMapLevel").html(" "+map.getZoom());
	   raza = 19.53125;
	}
	if (map.getZoom() == 19) {
	  $("#zoomMapLevel").html(" "+map.getZoom());
	  raza = 19.53125;
	}
	if (map.getZoom() == 20) {
	  $("#zoomMapLevel").html(" "+map.getZoom());
	 raza = 19.53125;
	}
	if (map.getZoom() == 21) {
	  $("#zoomMapLevel").html(" "+map.getZoom());
	  raza = 19.53125;
	}
	// max 21 zoom levels
	  
	       
	    var circle = new google.maps.Circle({
	      map: map,
	      strokeWeight: 2,
	      fillOpacity: 0.6,
	      center: variable1,	      
	      radius: raza,
	      fillColor: color,
	      zIndex: 1
	    });
	   // console.log("raza:"+raza);	    
	    
	    circles.push(circle);
	
	 circles[circles.length-1].setMap(map);     
       }
       RadiusWidget.prototype = new google.maps.MVCObject();  

function removeCircles(){
  //alert("removeCircles()");
  if (circles.length != 0){
    for (var i=0; i<circles.length; i++){
      circles[i].setMap(null);  
    }
  }
  circles = new Array();
}




