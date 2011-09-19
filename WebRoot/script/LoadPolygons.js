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
var SizeBS = 1000000;

var globalvar = null;
var infowindow;
function loadXml(xml) {
    //alert(" sas load xml");
  //  arrayPolygons = new Array();
   // Polygon = new Array();
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
	//console.log("id[pol]: "+id[pol]);
        total.push($(this).attr('total'));
	//console.log("total.push: "+$(this).attr('total'));
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
		//console.log("circles.length: "+circles.length);
                n++;
            });	   
            pol++;
        });
    });    
    createPolygonsFortheMapWithInformationClosure(pol);       
}
function createPolygonsFortheMapWithInformationClosure(pol) {
    Polygon = new Array();
    for (var i = 0; i < pol; i++) {
        Polygon[id[i]] = new google.maps.Polygon({
            clickable: true,
            paths: arrayPolygons[i],
            strokeColor: "#FF0000",
            strokeWeight: wid[i],
            fillColor: "#0000FF",
            fillOpacity: 0.01,
	    zIndex: 20
        });
        attachMessage(Polygon[id[i]], i, id[i]);
    }
    restorePolygon();        
}


function attachMessage(Polygon, nrPolOntheScreen, idp) {
    var message = idp.toString();
    var infowindow = new google.maps.InfoWindow({
        content: message,
        size: new google.maps.Size(50, 50)
    });

    google.maps.event.addListener(Polygon, 'click', function (event) {
	unSelectAllThePolygons();
        cleanPhotos();
	$("#maxContainer").show();
        Polygon.setOptions({
            fillColor: "#0000FF",
            fillOpacity: 0.35
        });
        boolSelected[idp] = 1;
	globalvar = nrPolOntheScreen;
        globalPolygonSelected = idp;
        ids = "";
        if (boolSelected[globalPolygonSelected] == 1) {
            ids = ids + "" + globalPolygonSelected;
        }
        $('#numberOfItems').empty().html("<span> Number of pictures selected: " + sel[globalPolygonSelected] + "idpoligon= "+ globalPolygonSelected+ " <img src='images/89.gif' height='20' width='20'/> </span>");	//globalvar
	$("#chart1").html("<img title='Year Level' id = 'IntChartID_1' class='InteriorChart' src='TimeSeriesChart.png?areaid="+getId()+"&level=year&width=140&height=80&"+new Date()+"  '>");
	$("#chart2").html("<img  title='Month Level'  id = 'IntChartID_2' class='InteriorChart' src='TimeSeriesChart.png?areaid="+getId()+"&level=month&width=140&height=80&"+new Date()+"  '>");	
	$("#chart3").html("<img title='Day Level' id = 'IntChartID_3' class='InteriorChart' src='TimeSeriesChart.png?areaid="+getId()+"&level=day&width=140&height=80&"+new Date()+"  '>");
	$("#chart4").html("<br><img title='Hours Level' id = 'IntChartID_4' class='InteriorChart' src='TimeSeriesChart.png?areaid="+getId()+"&level=hour&width=140&height=80&"+new Date()+"  '>");
	$("#chart5").html("<img title='Week Day Level' id = 'IntChartID_5' class='InteriorChart' src='TimeSeriesChart.png?areaid="+getId()+"&level=weekday&width=140&height=80&"+new Date()+"  '>");
	atacheEventsOnCharts(); // charts.js
        setCarousel(ids);
    });
    google.maps.event.addListener(Polygon, 'mouseover', function (event) {      
        Polygon.setOptions({
            fillColor: "#0000FF", //blue #0000FF
            fillOpacity: 0.35
        });
	infowindow = new google.maps.InfoWindow();       
 	infowindow = new google.maps.InfoWindow({
 	    disableAutoPan:true
 	});
	var vertices = this.getPath();
	var contentString = "<b> Total pictures: "+total[nrPolOntheScreen]+"</b><br /> <b>Selected pictures: "+selc[nrPolOntheScreen]+"</b>";	
	infowindow.setContent(contentString);
	infowindow.setPosition(event.latLng);
	infowindow.open(map);
    });     
    google.maps.event.addListener(Polygon, 'mouseout', function (event) {     
	infowindow.close()
        if (boolSelected[idp] == 0) { // if it is not selected the polygon with id = idp
            Polygon.setOptions({
                fillOpacity: 0.01, //0.01
                fillColor: "#FF0000" //red
            });
        }
    });
    History.push(Polygon);
    Polygon.setMap(map);
    if (History.length == pol){
      $("#legendInfo").html("<span>number of pictures </span><br/> <span> <img src='images/circle_bl.ico' width='20px' height='20px'/> 1-99 </span> <br/><span> <img src='images/circle_gr.ico' width='20px' height='20px' /> 100-999</span><br/><span> <img src='images/circle_lgr.ico' width='20px' height='20px'/> 1000-9999</span> <br/><span> <img src='images/circle_or.ico' width='20px' height='20px'/> > 10000</span>");    
    }
}

function initboolSelected(){
    for (var i = 0; i < SizeBS; i++) {
       boolSelected[i] = 0;
       sel[i]=0;
    }
}

function unSelectAllThePolygons() {
         if ((boolSelected[globalPolygonSelected] == 1)&&(Polygon[globalPolygonSelected]!=null)) {
            boolSelected[globalPolygonSelected] = 0;
            Polygon[globalPolygonSelected].setOptions({
                fillOpacity: 0.01,
                fillColor: "#FF0000"
            });
       }
}
function restorePolygon(){
     // console.log("error sometimes: "+globalPolygonSelected+" ");
      if ((boolSelected[globalPolygonSelected] == 1)&&(Polygon[globalPolygonSelected]!=null)) {
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
        $('#numberOfItems').empty().html("<span> Number of pictures selected: " + sel[globalPolygonSelected] + "idpoligon= "+ globalPolygonSelected+ " <img src='images/89.gif' height='20' width='20'/> </span>");	//globalvar
	$("#chart1").html("<img title='Year Level' id = 'IntChartID_1' class='InteriorChart' src='TimeSeriesChart.png?areaid="+getId()+"&level=year&width=140&height=80&"+new Date()+"  '>");
	$("#chart2").html("<img  title='Month Level'  id = 'IntChartID_2' class='InteriorChart' src='TimeSeriesChart.png?areaid="+getId()+"&level=month&width=140&height=80&"+new Date()+"  '>");
	$("#chart3").html("<img title='Day Level' id = 'IntChartID_3' class='InteriorChart' src='TimeSeriesChart.png?areaid="+getId()+"&level=day&width=140&height=80&"+new Date()+"  '>");
	$("#chart4").html("<br><img title='Hours Level' id = 'IntChartID_4' class='InteriorChart' src='TimeSeriesChart.png?areaid="+getId()+"&level=hour&width=140&height=80&"+new Date()+"  '>");
	$("#chart5").html("<img title='Week Day Level' id = 'IntChartID_5' class='InteriorChart' src='TimeSeriesChart.png?areaid="+getId()+"&level=weekday&width=140&height=80&"+new Date()+"  '>");
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

