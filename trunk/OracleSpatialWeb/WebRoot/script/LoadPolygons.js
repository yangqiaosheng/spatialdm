/*var History = new Array();
var arrayPolygons = new Array();
var Polygon = new Array();
var tags  = new Array();
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
*/

//var mouseoverTimeoutId = null;

/*function pausecomp(millis) 
{
  var date = new Date();
  var curDate = null;

  do { curDate = new Date(); } 
  while(curDate-date < millis);
} 
*/
/*
function loadTags(idp, xml, center, total, selc, mouseString){    
	infowindow.close();
	tags = new Array();
	var contorK=0;		
	$(xml).find('tag').each(function () { 
	      var tag = new classTag();
	      var nameTag = $(this).attr('name').replace(/^\s+|\s+$/g, '');
	      var number = $(this).attr('num').replace(/^\s+|\s+$/g, '');
	      tag.nameTag = nameTag;	      
	      tag.size = number;
	      tags[contorK] = tag;	      
	      contorK++;
	}); 
      //http://blogs.dekoh.com/dev/2007/10/29/choosing-a-good-font-size-variation-algorithm-for-your-tag-cloud/
	var minFontSize = 2;
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
      if ((eval(mouseString)==1)){
	$("#tag").html("");
	for (var i=0;i<contorK;i++){
	      $("#tag").append("<span onclick='wordClick()'><font size="+tags[eval(shuffledVector[i])].getFontSize()+" face='arial' color='blue'>"+tags[eval(shuffledVector[i])].getNameTag()+"<!--,"+tags[eval(shuffledVector[i])].getSize()+","+tags[eval(shuffledVector[i])].getFontSize()+": --> </font></span>");
	}
	var contentString1 = "<div id='infoWind'><div style='text-align: center;'>Picures and Tags: <br/> Total: "+total+" Selected: "+selc+"<br/></div>";
	if (($("#tag").html()!="")){
	  var contentString2 = "<div class ='taginfo'>"+$("#tag").html()+"</div><br/></div>";
	  var contentString = contentString1+contentString2;
	  infowindow.setContent(contentString);
	  infowindow.setPosition(center);
	  infowindow.open(map);
	}
      }
      if (eval(mouseString)==2){
	readyToExecute = false;
	//$("#tag").html("");
	$("#tagClick").html("");
	if ($("#tag").html()==""){
	  for (var i=0;i<contorK;i++){
		$("#tagClick").append("<span onclick='wordClick()'><font size="+tags[eval(shuffledVector[i])].getFontSize()+" face='arial' color='blue'>"+tags[eval(shuffledVector[i])].getNameTag()+"<!--,"+tags[eval(shuffledVector[i])].getSize()+","+tags[eval(shuffledVector[i])].getFontSize()+": --> </font></span>");
	  }
	  var contentString1 = "<div id='infoWind' OnMouseOver='mouseOverTag()' OnMouseOut='mouseOutTag()'><div style='text-align: center;'>Picures and Tags: <br/> Total: "+total+" Selected: "+selc+"<br/></div>";
	    if ($("#tagClick").html()!=""){	     
	      var contentString2 = "<div class ='taginfo'>"+$("#tagClick").html()+"</div><br/></div>";
	      var contentString = contentString1+contentString2;	      
	      infowindowClick.setContent(contentString);
	      infowindowClick.setPosition(center);
	      infowindowClick.open(map);	      
	  }
	}else{	  
	  $("#tagClick").append($("#tag").html());
	  var contentString1 = "<div id='infoWind' OnMouseOver='mouseOverTag()' OnMouseOut='mouseOutTag()'><div style='text-align: center;'>Picures and Tags: <br/> Total: "+total+" Selected: "+selc+"<br/></div>";
	    if ($("#tagClick").html()!=""){
	      var contentString2 = "<div class ='taginfo'>"+$("#tagClick").html()+"</div><br/></div>";
	      var contentString = contentString1+contentString2;
	      infowindowClick.setContent(contentString);
	      infowindowClick.setPosition(center);
	      infowindowClick.open(map);
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

function wordClick(){
     $(this).css("background-color","yellow");
}
function mouseOverTag(){
    readyToExecute=false;  
}
function mouseOutTag(){
    readyToExecute=true;
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
} */
