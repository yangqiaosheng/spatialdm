function atacheEventsOnCharts(){
  var yr = $("#IntChartID_1");
  yr.toggle(function(){
	    $(this).css("height", "30%");
	    $(this).css("width", "140%");
      }, function(){
	    $(this).css("height", "60px");
	    $(this).css("width", "30%");
      });  
var mon = $("#IntChartID_2");
  mon.toggle(function(){
	    $(this).css("height", "30%");
	    $(this).css("width", "140%");
      }, function(){
	    $(this).css("height", "60px");
	    $(this).css("width", "30%");
      });  
var h = $("#IntChartID_3");
  h.toggle(function(){
	    $(this).css("height", "30%");
	    $(this).css("width", "140%");
      }, function(){
	    $(this).css("height", "60px");
	    $(this).css("width", "30%");
      });  
}