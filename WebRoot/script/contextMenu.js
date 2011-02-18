
function hideContextMenu1() {
    $('.contextmenu1').remove();
}

function showContextMenu1(caurrentLatLng, total, sel) {
    var projection;
    var contextmenuDir;
    projection = map.getProjection();
    $('.contextmenu1').remove();
    contextmenuDir = document.createElement("div");
    contextmenuDir.className = 'contextmenu1';
    contextmenuDir.innerHTML = "<div class = 'context1' ><a id='total'><div class=context>total: " + total + " <\/div><\/a><a id='selected'><div class=context1>selected: " + sel + " <\/div><\/a></div>";
    $(map.getDiv()).append(contextmenuDir);
    setMenuXY1(caurrentLatLng);
    contextmenuDir.style.visibility = "visible";
    $('#total, #selected').hover(function () {
        var cssObj = {
            'color': 'blue'
        }
        $(this).css(cssObj);
    },
    function () {
        var cssObj = {
            'color': 'black'
        }
        $(this).css(cssObj);
    });
}

function setMenuXY1(caurrentLatLng) {
    var mapWidth = $('#map_canvas').width();
    var mapHeight = $('#map_canvas').height();
    var menuWidth = $('.contextmenu1').width();
    var menuHeight = $('.contextmenu1').height();
    var clickedPosition = getCanvasXY1(caurrentLatLng);
    var x = clickedPosition.x;
    var y = clickedPosition.y;
    if ((mapWidth - x) < menuWidth)
    x = x - menuWidth;
    if ((mapHeight - y) < menuHeight)
    y = y - menuHeight;
    $('.contextmenu1').css('left', x);
    $('.contextmenu1').css('top', y);
}

function getCanvasXY1(caurrentLatLng) {
    var scale = Math.pow(2, map.getZoom());
    var nw = new google.maps.LatLng(
    map.getBounds().getNorthEast().lat(),
    map.getBounds().getSouthWest().lng());
    var worldCoordinateNW = map.getProjection().fromLatLngToPoint(nw);
    var worldCoordinate = map.getProjection().fromLatLngToPoint(caurrentLatLng);
    var caurrentLatLngOffset = new google.maps.Point(
    Math.floor((worldCoordinate.x - worldCoordinateNW.x) * scale),
    Math.floor((worldCoordinate.y - worldCoordinateNW.y) * scale));
    return caurrentLatLngOffset;
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