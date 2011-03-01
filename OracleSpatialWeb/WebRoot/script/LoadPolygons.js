var History = new Array();
var arrayPolygons = new Array();
var Polygon = new Array();
var pol = 0;
var total = new Array(); // mouseover polygon gives the total number of pictures
var sel = new Array(); // mouse over polygon gives the selected number of pictures
var id = new Array();
var center = new Array();
var wid = new Array();
var boolSelected = new Array();
var SizeBS = 45000;
var globalvar = null;

function loadXml(xml) {
    //alert(" sas load xml");
    arrayPolygons = new Array();
    Polygon = new Array();
    pol = 0;
    total = new Array();
    sel = new Array();
    selc = new Array();
    id = new Array();
    center = new Array();
    wid = new Array();
   // boolSelected = new Array();
    deleteHistory();
    removeCircles();
    $(xml).find('polygon').each(function () {
        id[pol] = $(this).attr('id');
        total.push($(this).attr('total'));
	selc.push($(this).attr('select'));
	sel[id[pol]]=$(this).attr('select');
        $(this).find('line').each(function () {
            wid[pol] = $(this).attr('width');
            //$('#legendInfo').append(" wid: " + wid[pol]);
            var n = 0;
            var lng = new Array();
            var lat = new Array();
            var pts = new Array();
            $(this).find('point').each(function () {
                lng[n] = $(this).attr('lng');
                lat[n] = $(this).attr('lat');
                pts[n] = new google.maps.LatLng(parseFloat(lat[n]), parseFloat(lng[n]));
                n++;
            });
            arrayPolygons[pol] =(pts);
        });
        $(this).find('center').each(function () {
            var n = 0;
            var lng = new Array();
            var lat = new Array();
            $(this).find('point').each(function () {
                lng[n] = $(this).attr('lng');
                lat[n] = $(this).attr('lat');		
                center[pol] = new google.maps.LatLng(parseFloat(lat[n]), parseFloat(lng[n]));		
		var distanceWidget = new DistanceWidget(center[pol], sel[id[pol]]);				
                n++;
            });          
            pol++;
        });
    });
    createPolygonsFortheMapWithInformationClosure(pol);
}
function createPolygonsFortheMapWithInformationClosure(pol) {
    for (var i = 0; i < pol; i++) {
        Polygon[id[i]] = new google.maps.Polygon({
            clickable: true,
            paths: arrayPolygons[i],
            strokeColor: "#FF0000",            
            strokeWeight: wid[i],
            fillColor: "#0000FF",
            fillOpacity: 0.01
        });
        attachMessage(Polygon[id[i]], i, id[i]);
    }
    restorePolygon();
    //submitAndChangeTheCarousel();
}


function attachMessage(Polygon, valuei, number) {
    var message = number.toString();    
    var infowindow = new google.maps.InfoWindow({
        content: message,
        size: new google.maps.Size(50, 50)
    });
    
    google.maps.event.addListener(Polygon, 'click', function (event) {     
	unSelectAllThePolygons();
        cleanPhotos();     	
        this.setOptions({
            fillColor: "#0000FF",
            fillOpacity: 0.35
        });
        boolSelected[number] = 1;	
	globalvar = valuei;
        globalPolygonSelected = number;        
	//alert(" center ="+center[globalvar].lat()+" "+center[globalvar].lng());
        ids = "";        
        if (boolSelected[globalPolygonSelected] == 1) {
            ids = ids + "" + globalPolygonSelected;
        }        
        $('#numberOfItems').html("<span> Number of pictures selected: " + sel[globalPolygonSelected] + " idpoligon= "+ globalPolygonSelected+ "</span>");	
	$("#chart1").html("<img title='Year Level' id = 'IntChartID_1' class='InteriorChart' src='http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/TimeSeriesChart.png?areaid="+getId()+"&level=year&random="+new Date()+"  '>"); 
	$("#chart1").append("<img  title='Month Level'  id = 'IntChartID_2' class='InteriorChart' src='http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/TimeSeriesChart.png?areaid="+getId()+"&level=month&random="+new Date()+" '>"); 
	$("#chart1").append("<img title='Hours Level' id = 'IntChartID_3' class='InteriorChart' src='http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/TimeSeriesChart.png?areaid="+getId()+"&level=hour&random="+new Date()+" '>"); 
	$("#chart1").append("<img title='Day Level' id = 'IntChartID_4' class='InteriorChart' src='http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/TimeSeriesChart.png?areaid="+getId()+"&level=day&random="+new Date()+" '>"); 
	$("#chart1").append("<img title='Week Day Level' id = 'IntChartID_5' class='InteriorChart' src='http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/TimeSeriesChart.png?areaid="+getId()+"&level=weekday&random="+new Date()+" '>"); 
	atacheEventsOnCharts(); // charts.js
        setCarousel(ids);
    });
    google.maps.event.addListener(Polygon, 'mouseover', function (event) {
        this.setOptions({
            fillColor: "#0000FF",
            fillOpacity: 0.35
        });
        showContextMenu1(center[valuei], total[valuei], selc[valuei]);	
    });
    google.maps.event.addListener(Polygon, 'mouseout', function (event) {
        hideContextMenu1();
        hideContextMenu();
        if (boolSelected[number] == 0) {
            this.setOptions({
                fillOpacity: 0.01,
                fillColor: "#FF0000"
            });
        }
    });
    History.push(Polygon);
    Polygon.setMap(map);
}

function initboolSelected(){ 
    for (var i = 0; i < SizeBS; i++) {
       boolSelected[i] = 0;
       sel[i]=0;
    }      
}
function unSelectAllThePolygons() {        
        if (boolSelected[globalPolygonSelected] == 1) {
            boolSelected[globalPolygonSelected] = 0;
            Polygon[globalPolygonSelected].setOptions({
                fillOpacity: 0.01,
                fillColor: "#FF0000"
            });
       }    
}
function restorePolygon(){
      if (boolSelected[globalPolygonSelected] == 1) {            
            Polygon[globalPolygonSelected].setOptions({
                fillColor: "#0000FF",
		fillOpacity: 0.35
            });	
	cleanPhotos(); 
	ids = "";        
        if (boolSelected[globalPolygonSelected] == 1) {
            ids = ids + "" + globalPolygonSelected;
        }  
	//alert("globalvar "+globalvar +" sel[globalvar]"+sel[globalvar]);
        $('#numberOfItems').html("<span> Number of pictures selected: " + sel[globalPolygonSelected] + "idpoligon= "+ globalPolygonSelected+ "</span>");	//globalvar
	$("#chart1").html("<img title='Year Level' id = 'IntChartID_1' class='InteriorChart' src='http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/TimeSeriesChart.png?areaid="+getId()+"&level=year&random="+new Date()+"  '>"); 
	$("#chart1").append("<img  title='Month Level'  id = 'IntChartID_2' class='InteriorChart' src='http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/TimeSeriesChart.png?areaid="+getId()+"&level=month&random="+new Date()+" '>"); 
	$("#chart1").append("<img title='Hours Level' id = 'IntChartID_3' class='InteriorChart' src='http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/TimeSeriesChart.png?areaid="+getId()+"&level=hour&random="+new Date()+" '>"); 
	$("#chart1").append("<img title='Day Level' id = 'IntChartID_4' class='InteriorChart' src='http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/TimeSeriesChart.png?areaid="+getId()+"&level=day&random="+new Date()+" '>"); 
	$("#chart1").append("<img title='Week Day Level' id = 'IntChartID_5' class='InteriorChart' src='http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/TimeSeriesChart.png?areaid="+getId()+"&level=weekday&random="+new Date()+" '>"); 
	atacheEventsOnCharts(); // charts.js
        setCarousel(ids);
       }    
}
function deleteHistory() {
   // $("#maxContainer").removeClass('visible').addClass('invisible');
    for (var i = 0; i < History.length; i++) {
        History[i].setMap();
    }
    History = new Array();   
	
}

