//google.load("maps", "2"); // I load the content of the map
//Map example

// google.maps is generic. after that comes the class  which is in the API 3
var map = null;
var myOptions = null;
var geocoder =  null;
var geocoder = null;
var readyToExecute = true;
var readyToExecute_A = true;
var readyToExecute_B = true;


var History = new Array();
var arrayPolygons = new Array();
var Polygon = new Array();
var tags  = new Array();
var tagsMask  = new Array();
var tagsClick  = new Array();
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
var timeOut;
var AGGREGATION = true;
var TagExistance = true;


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
	  initboolSelected();// LoadPolygons.js
	  $('#EnabledOrDisabled').html(" Enabled <br/>");
	  agregationPolygonsAdd();
	 // scaleLevelOnStart();
	 /* google.maps.event.addListener(map, "idle", function() {
	    askHistogram();
	  });*/
};
//********************************************************************************************
var listenerHandle;
function agregationPolygonsAdd() {
	AGGREGATION = true;
	$("#voronoiT").attr('value', 'Disable triangle agregation');
	listenerHandle = google.maps.event.addListener(map, 'idle',function() {
				removeCircles();
				var bounds = map.getBounds();
				var center = map.getCenter();
				var zoomLevel = map.getZoom();
				scaleLevelOnStart();
				askHistogram();
			});
}

function agregationPolygonsRemove() {
	//refreshButtons();
	//removeCircles();
	AGGREGATION = false;
	google.maps.event.removeListener(listenerHandle);
}

$(function() {
	jQuery("#AggregationCheckBox").change(function() {
		if (jQuery("#AggregationCheckBox").attr("checked") == true) {
			agregationPolygonsAdd();
			jQuery("#EnabledOrDisabled").html("Enabled");
		} else {
			agregationPolygonsRemove();
			jQuery("#EnabledOrDisabled").html("Disabled");
		}
	});

});

$(function() {
	jQuery("#TAGCheckBox").change(function() {
		if (jQuery("#TAGCheckBox").attr("checked") == true) {
		    TagExistance = true;

		} else {
		    TagExistance = false;
		    $("#tag").html()!="";
		    $("#tagClick").html()!="";
		}
	});

});




$(function() {
	$("#timeC7, #timeC1, #timeC2, #timeC3, #tab1ready, #tab2ready, #tab3ready, #tab4ready")
			.hover(function() {
				$(this).removeClass("timeCstar");
				$(this).addClass("hover");
			}, function() {
				$(this).removeClass("hover");
				$(this).addClass("timeCstar");
			});
});

$(function() {
	$("#CalendarContent").hide();
	$("#histogramContent").hide();
	$("#SearchBox").hide();
	$("#selectedYMDHCheckbox").click(function() {
		$("#CalendarContent").hide(1000);
		$("#histogramContent").hide(1000);
		$("#table7").css({ 'padding-bottom' : '0px'});
	});
	$("#CalendarcheckBox").click(function() {
		$("#CalendarContent").show(1000);
		$("#histogramContent").hide(1000);
		$("#table7").css({ 'padding-bottom' : '0px'});
	});
	$("#histogramCheckbox").click(function() {
		$("#table7").css({ 'padding-bottom' : '40px'});
		$("#histogramContent").show(1000);
		$("#CalendarContent").hide(1000);
	});
	$("#SearchCheckBox").toggle(function() {
	     $("#SearchBox").show();
	}, function() {
	      $("#SearchBox").hide();
	});

});
//********************************************************************************************
function addRemoveElementsFromHistogram(){
	    $("#parent1").remove();
	    $("#parent2").remove();
	    $("#parent3").remove();
	    $("#parent4").remove();
	    $("#parent5").remove();
	    $("#absButton").remove();
	    $("#histogramContent").append("<div id=parent1></div>");
	    $("#histogramContent").append("<div id=parent2></div>");
	    $("#histogramContent").append("<div id=parent3></div>");
	    $("#histogramContent").append("<div id=parent4></div>");
	    $("#histogramContent").append("<div id=parent5></div>");
}

/*function start() {
	$("#maxContainer").hide();
	initialize1();
}
*/

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
	if (map.getZoom() == 11) {
		$("#scaleLevel").html("7");
	}
	if (map.getZoom() == 12) {
		$("#scaleLevel").html("8");
	}
	if (map.getZoom() == 13) {
		$("#scaleLevel").html("9");
	}
	if (map.getZoom() == 14) {
		$("#scaleLevel").html("10");
	}
	if (map.getZoom() >= 15) {
		$("#scaleLevel").html("11");
	}
}
var parLocal = 0;
function hidemoveCP() {
    $("#move1").toggle("slow", function(){
	if (parLocal%2==0){
	  $("#control").attr('value','show');
	  parLocal++
	}
	else{
	  $("#control").attr('value','hide');
	  parLocal++
	}
    });
}
var carouselLocal = 0;
function hidemoveCarousel(){
   $("#maxContainer").toggle("slow", function(){
	if (carouselLocal%2==0){
	  $("#carouselControl").attr('value','show');
	  carouselLocal++
	}
	else{
	  $("#carouselControl").attr('value','hide');
	  carouselLocal++
	}
    });
}

function initboolSelected(){
    for (var i = 0; i < SizeBS; i++) {
       boolSelected[i] = 0;
       sel[i]=0;
    }
}

function deleteHistory() {
    for (var i = 0; i < History.length; i++) {
        History[i].setMap();
    }
    History = new Array();
}

function loadXml(xml) {
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
    infowindow = new google.maps.InfoWindow({
 	    disableAutoPan:true,
	    maxWidth: 300,
	    zIndex: 2
	  });
    infowindowClick = new google.maps.InfoWindow({
 	    disableAutoPan:true,
	    maxWidth: 300,
	    zIndex: 1
	  });
    var numberOfTags = 30;
    google.maps.event.addListener(Polygon, 'click', function (event) {
    if ((readyToExecute_A==true)&&(readyToExecute_B ==  true)){
	readyToExecute=true;
      }else{
	readyToExecute=false;
      }
      if (readyToExecute==true){
	  //console.log("MAin "+readyToExecute+" "+readyToExecute_A);
	  readyToExecute = false;
	  readyToExecute_A = false;
	  tagsClick = new Array();
	  $("#tagClick").html("");
	  infowindowClick.close();
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
	  askForTags(idp, numberOfTags, center[nrPolOntheScreen], total[nrPolOntheScreen], selc[nrPolOntheScreen], 2);
	}
    });
    google.maps.event.addListener(Polygon, 'mouseover', function (event) {
	infowindow.close();
	if ((readyToExecute_A==true)&&(readyToExecute_B == true)){
	  readyToExecute=true;
	}else{
	  readyToExecute=false;
	}
	if (readyToExecute==true){
	      Polygon.setOptions({
	      fillColor: "#0000FF",
	      fillOpacity: 0.35
	      });
	      var nr =1;
	      var centerP  = center[nrPolOntheScreen];
	      var totalP = total[nrPolOntheScreen];
	      var selectedPolygons = selc[nrPolOntheScreen];
	      timeOut = setTimeout(function innerFunction(){
				if (window.XMLHttpRequest) {
				  xmlHttp = new XMLHttpRequest();
				} else if (window.ActiveXObject) {
				    xmlHttp = new ActiveXObject("Microsoft.XMLHTTP");
				} else {
				  document.write("browser not supported");
				}
				xmlHttp.open("POST", "Tag");
				xmlHttp.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
				xmlHttp.send("areaid=" + idp + "&size=" + numberOfTags+ "&timestamp=" + new Date().getTime());
				xmlHttp.onreadystatechange = function() {
				  if (xmlHttp.readyState == 4) {
				      var xmlDoc = xmlHttp.responseText;
				      loadTags(idp, xmlDoc, centerP, totalP, selectedPolygons, nr);
				  }
				};}, 500);
	}
    });
    google.maps.event.addListener(Polygon, 'mouseout', function (event) {
	clearTimeout(timeOut);
	infowindow.close();
        if (boolSelected[idp] == 0) {
            Polygon.setOptions({
                fillOpacity: 0.01,
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

// this function is executed in the begining onload on body
function loadStart() {
	field = get_set_Field();
	number = get_set_number();
	nameString = loadDataForTable();
	activateTheHeaderOfTable();
	$("#maxContainer").hide();
	initialize1();
}

//, total, selectedP, infowindow

function askForTags(ids, numberOfTags, center, total, selc, number){
      if (window.XMLHttpRequest) {
		xmlHttp = new XMLHttpRequest();
	} else if (window.ActiveXObject) {
		xmlHttp = new ActiveXObject("Microsoft.XMLHTTP");
	} else {
		document.write("browser not supported");
	}
	xmlHttp.open("POST", "Tag");
	xmlHttp.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
	//console.log("areaid=" + ids + "&size=" + numberOfTags);
	xmlHttp.send("areaid=" + ids + "&size=" + numberOfTags+ "&timestamp=" + new Date().getTime());
	xmlHttp.onreadystatechange = function() {
		if (xmlHttp.readyState == 4) {
			var xmlDoc = xmlHttp.responseText;
			loadTags(ids, xmlDoc, center, total, selc, eval(number));
		}
	};
}



function loadTags(idp, xml, center, total, selc, mouseString){
      //mouseover  //////////////////------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
      if ((eval(mouseString)==1)){
	infowindow.close();
	tags = new Array();
	var contorK=0;
	$(xml).find('tag').each(function () {
	      var tag = new classTag();
	      var nameTag = $(this).attr('name');
	      var number = $(this).attr('num');
	      tag.nameTag = nameTag;
	      tag.size = number;
	      tags[contorK] = tag;
	      contorK++;
	});
      //http://blogs.dekoh.com/dev/2007/10/29/choosing-a-good-font-size-variation-algorithm-for-your-tag-cloud/
	var minFontSize = 1;
	var maxFontSize = 6;
	var maxOccurs = 0;
	var minOccurs = 0;
	var vector = new Array();
	var vectorForMAXMIN= new Array();
	for (var i=0;i<contorK;i++){
	    vector[i] = i;
	    vectorForMAXMIN[i] = tags[i].getSize();
	}
	maxOccurs = vectorForMAXMIN[0];
	minOccurs = vectorForMAXMIN[contorK-1];
	var shuffledVector = new Array();
	shuffledVector =arrayShuffle(vector);
	for (var i=0;i<contorK;i++){
	    var weight = (Math.log(eval(tags[eval(shuffledVector[i])].getSize()))-Math.log(eval(minOccurs)))/(Math.log(eval(maxOccurs))-Math.log(eval(minOccurs)));
	    tags[eval(shuffledVector[i])].fontSize = eval(minFontSize) + Math.round((eval(maxFontSize)-eval(minFontSize))*eval(weight));
	}
	$("#tag").html("");

	for (var i=0;i<contorK;i++){
	      $("#tag").append("<span  id = '"+tags[eval(shuffledVector[i])].getNameTag()+""+eval(shuffledVector[i])+"' onclick='wordClick("+eval(shuffledVector[i])+","+idp+")' OnMouseOver='mouseOverTagSpan("+eval(shuffledVector[i])+")' OnMouseOut='mouseOutWORD("+eval(shuffledVector[i])+")'><font size="+tags[eval(shuffledVector[i])].getFontSize()+" face='arial' color='blue'>"+tags[eval(shuffledVector[i])].getNameTag()+" </font></span>");
	}
	var contentString1 = "<div id='infoWind'><div style='text-align: center;'>Picures and Tags: <br/> Total: "+total+" Selected: "+selc+"<br/></div>";
	if (($("#tag").html()!="")/*&&($("#tagClick").html()=="")*/){
	  if (TagExistance==true){
	    var contentString2 = $("#tag").html();
	  }else{
	    var contentString2 = "";
	  }
	  var contentString = contentString1+contentString2;
	  createInfoWindowMouseOver(center, contentString);
	}
      }
      //click   //////////////////------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
      if (eval(mouseString)==2){
	//console.log("click "+readyToExecute+" "+readyToExecute_A);
	infowindow.close();
	infowindowClick.close();
	//tags = new Array();
	tagsClick = new Array();
	tagsMask = new Array();
	//$("#tag").html("");
	var contorK=0;
	$(xml).find('tag').each(function () {
	      var tag = new classTag();
	      var nameTag = $(this).attr('name');
	      var number = $(this).attr('num');
	      tag.nameTag = nameTag;
	      tag.size = number;
	      tagsClick[contorK] = tag;
	      tagsMask[contorK] = 0;
	      contorK++;
	});
	var minFontSize = 1;
	var maxFontSize = 6;
	var maxOccurs = 0;
	var minOccurs = 0;
	var vector = new Array();
	var vectorForMAXMIN= new Array();
	for (var i=0;i<contorK;i++){
	    vector[i] = i;
	    vectorForMAXMIN[i] = tagsClick[i].getSize();
	}
	maxOccurs = vectorForMAXMIN[0];
	minOccurs = vectorForMAXMIN[contorK-1];
	var shuffledVector = new Array();
	shuffledVector =arrayShuffle(vector);
	for (var i=0;i<contorK;i++){
	    var weight = (Math.log(eval(tagsClick[eval(shuffledVector[i])].getSize()))-Math.log(eval(minOccurs)))/(Math.log(eval(maxOccurs))-Math.log(eval(minOccurs)));
	    tagsClick[eval(shuffledVector[i])].fontSize = eval(minFontSize) + Math.round((eval(maxFontSize)-eval(minFontSize))*eval(weight));
	}
	//readyToExecute = false;
	//readyToExecute_A = false;
	$("#tagClick").html("");
	if ($("#tag").html()==""){
	  for (var i=0;i<contorK;i++){
		$("#tagClick").append("<span id = '"+tagsClick[eval(shuffledVector[i])].getNameTag()+""+eval(shuffledVector[i])+"' onclick='wordClick("+eval(shuffledVector[i])+","+idp+")' OnMouseOver='mouseOverTagSpan("+eval(shuffledVector[i])+")'  OnMouseOut='mouseOutWORD("+eval(shuffledVector[i])+")'><font size="+tagsClick[eval(shuffledVector[i])].getFontSize()+" face='arial' color='blue'>"+tagsClick[eval(shuffledVector[i])].getNameTag()+" </font></span>");
	  }
	    var contentString1 = "<div id='infoWind' OnMouseOver='mouseOverTag()' OnMouseOut='mouseOutTag()'><div style='text-align: center;'>Picures and Tags: <br/> Total: "+total+" Selected: "+selc+"<br/></div>";
	    if ($("#tagClick").html()!=""){
	      if (TagExistance==true){
		var contentString2 = $("#tagClick").html();
	      }else{
		var contentString2 = "";
	      }
	      var contentString = contentString1+contentString2;
	      createInfoWindowMouseClick(center, contentString);
	  }
	}else{
	  $("#tagClick").append($("#tag").html());
	  tagsClick = new Array();
	  for (var i=0;i<contorK;i++){
	      tagsClick[i] = tags[i];
	  }
	  var contentString1 = "<div id='infoWind' OnMouseOver='mouseOverTag()' OnMouseOut='mouseOutTag()'><div style='text-align: center;'>Picures and Tags: <br/> Total: "+total+" Selected: "+selc+"<br/></div>";
	    if ($("#tagClick").html()!=""){
	      if (TagExistance==true){
		var contentString2 = "<div class ='taginfo'>"+$("#tagClick").html()+"</div><br/></div>";
	      }else{
		var contentString2 = "";
	      }
	      var contentString = contentString1+contentString2;
	      createInfoWindowMouseClick(center, contentString);
	  }
	}
	$('#numberOfItems').empty().html("<span> Number of pictures selected: " + sel[idp] + " <img src='images/89.gif' height='20' width='20'/> </span>");	//globalvar
	$("#chart1").html("<img title='Year Level' id = 'IntChartID_1' class='InteriorChart' src='TimeSeriesChart.png?areaid="+getId()+"&level=year&width=140&height=80&timestamp="+new Date().getTime()+"  '>");
	$("#chart2").html("<img  title='Month Level'  id = 'IntChartID_2' class='InteriorChart' src='TimeSeriesChart.png?areaid="+getId()+"&level=month&width=140&height=80&timestamp="+new Date().getTime()+"  '>");
	$("#chart3").html("<img title='Day Level' id = 'IntChartID_3' class='InteriorChart' src='TimeSeriesChart.png?areaid="+getId()+"&level=day&width=140&height=80&timestamp="+new Date().getTime()+"  '>");
	$("#chart4").html("<br><img title='Hours Level' id = 'IntChartID_4' class='InteriorChart' src='TimeSeriesChart.png?areaid="+getId()+"&level=hour&width=140&height=80&timestamp="+new Date().getTime()+"  '>");
	$("#chart5").html("<img title='Week Day Level' id = 'IntChartID_5' class='InteriorChart' src='TimeSeriesChart.png?areaid="+getId()+"&level=weekday&width=140&height=80&timestamp="+new Date().getTime()+"  '>");
	atacheEventsOnCharts();
	setCarousel(idp);
      }
}
function createInfoWindowMouseOver(center, contentString){
	  infowindow = new google.maps.InfoWindow({
 	    disableAutoPan:true,
	    maxWidth: 300,
	    zIndex: 2
	  });
	  infowindow.setContent(contentString);
	  infowindow.setPosition(center);
	  infowindow.open(map);
}

function createInfoWindowMouseClick(center, contentString){
	  infowindowClick = new google.maps.InfoWindow({
 	    disableAutoPan:true,
	    maxWidth: 300,
	    zIndex: 1
	  });
	  infowindowClick.setContent(contentString);
	  infowindowClick.setPosition(center);
	  infowindowClick.open(map);
}
function mouseOverTagSpan(num){
   	jQuery("#"+tagsClick[num].getNameTag()+""+num).css("background-color","yellow");
}
function wordClick(num, idp){
      jQuery("#TagGraphic").html("");
      if (tagsMask[num]==0){
	tagsMask[num] = 1;
	jQuery("#"+tagsClick[num].getNameTag()+""+num).css("background-color","yellow");
	jQuery("#TagGraphic").append("<img title='Tag Graph' src='TagChart.png?areaid="+idp+"&tag="+encodeURIComponent(tagsClick[num].getNameTag())+"&timestamp="+new Date().getTime()+"  '>");
	$("#TagGraphic").show('slow');
      }else{
	tagsMask[num] = 0;
	jQuery("#"+tagsClick[num].getNameTag()+""+num).css("background-color","white");
	$("#TagGraphic").hide('slow');
	jQuery("#TagGraphic").html("");
      }
}
function mouseOutWORD(num){
	if (tagsMask[num]==0){
	  jQuery("#"+tagsClick[num].getNameTag()+""+num).css("background-color","white");
	}
}
function mouseOverTag(){
    readyToExecute=false;
    readyToExecute_B = false;
}
function mouseOutTag(){
    readyToExecute_B = true;
    if (readyToExecute_A==true){
      readyToExecute=true;
    }
}

function arrayShuffle(oldArray) {
	var newArray = oldArray.slice();
 	var len = newArray.length;
	var i = len;
	 while (i--) {
	 	var p = parseInt(Math.random()*len);
		var t = newArray[i];
  		newArray[i] = newArray[p];
	  	newArray[p] = t;
 	}
	return newArray;
};


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
	$("#chart1").html("<img title='Year Level' id = 'IntChartID_1' class='InteriorChart' src='TimeSeriesChart.png?areaid="+getId()+"&level=year&width=140&height=80&timestamp="+new Date().getTime()+"  '>");
	$("#chart2").html("<img  title='Month Level'  id = 'IntChartID_2' class='InteriorChart' src='TimeSeriesChart.png?areaid="+getId()+"&level=month&width=140&height=80&timestamp="+new Date().getTime()+"  '>");
	$("#chart3").html("<img title='Day Level' id = 'IntChartID_3' class='InteriorChart' src='TimeSeriesChart.png?areaid="+getId()+"&level=day&width=140&height=80&timestamp="+new Date().getTime()+"  '>");
	$("#chart4").html("<br><img title='Hours Level' id = 'IntChartID_4' class='InteriorChart' src='TimeSeriesChart.png?areaid="+getId()+"&level=hour&width=140&height=80&timestamp="+new Date().getTime()+"  '>");
	$("#chart5").html("<img title='Week Day Level' id = 'IntChartID_5' class='InteriorChart' src='TimeSeriesChart.png?areaid="+getId()+"&level=weekday&width=140&height=80&timestamp="+new Date().getTime()+"  '>");
	atacheEventsOnCharts(); // charts.js
        setCarousel(ids);
       }
}

// the class of Tag
function classTag(){
    classTag.nameTag="";
    classTag.size=0;
    classTag.fontSize=0;
    classTag.prototype.getFontSize = function(){
	return this.fontSize;
    }
    classTag.prototype.getNameTag = function(){
	return this.nameTag;
    }
    classTag.prototype.getSize = function(){
	return this.size;
    }
}

function TagGraphicDisappear(){
    $("#TagGraphic").hide('slow');
}