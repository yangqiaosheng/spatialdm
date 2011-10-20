function atacheEventsOnCharts(){
  var yr = $("#chart1");
  yr.toggle(function(){	 
	  $("#pictureMouseOver").html("<img title='Year Level' id = 'IntChartID_1' class='InteriorChart' src='TimeSeriesChart.png?areaid="+getId()+"&level=year&width=600&height=300&smooth=yes&timestamp="+new Date().getTime()+"  '>");
      }, function(){
	  $("#pictureMouseOver").html("");	  	  
	 
      });  

var mon = $("#chart2");  
  mon.toggle(function(){	 
	  $("#pictureMouseOver").html("<img title='Month Level' id = 'IntChartID_2' class='InteriorChart' src='TimeSeriesChart.png?areaid="+getId()+"&level=month&width=600&height=300&smooth=yes&timestamp="+new Date().getTime()+"  '>");
      }, function(){
	  $("#pictureMouseOver").html("");	 	
      });  

var h = $("#chart3");
  h.toggle(function(){
	 $("#pictureMouseOver").html("<img title='Day Level' id = 'IntChartID_3' class='InteriorChart' src='TimeSeriesChart.png?areaid="+getId()+"&level=day&width=600&height=300&smooth=yes&timestamp="+new Date().getTime()+"  '>");
      }, function(){
	 $("#pictureMouseOver").html("");	 
      }); 
var h = $("#chart4");
  h.toggle(function(){
	  $("#pictureMouseOver").html("<img title='Hours Level' id = 'IntChartID_4' class='InteriorChart' src='TimeSeriesChart.png?areaid="+getId()+"&level=hour&width=600&height=300&smooth=yes&timestamp="+new Date().getTime()+"  '>");
      }, function(){
	  $("#pictureMouseOver").html("");	 
      }); 


var h = $("#chart5");
  h.toggle(function(){
	  $("#pictureMouseOver").html("<img title='Week Day Level' id = 'IntChartID_5' class='InteriorChart' src='TimeSeriesChart.png?areaid="+getId()+"&level=weekday&width=600&height=300&smooth=yes&timestamp="+new Date().getTime()+"  '>");
      }, function(){
	  $("#pictureMouseOver").html("");	
      }); 
}