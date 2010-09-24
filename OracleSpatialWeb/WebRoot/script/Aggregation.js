(function($) {
$(document).ready(function(){     
   function agregationPolygonsAdd(){
        $("#voronoiT").attr('value', 'Disable triangle agregation');        
	$("#legendInfo").html("<span>number of pictures </span><br/> <span> <img src='images/circle_bl.ico' width='20px' height='20px'/> 1-99 </span> <br/><span> <img src='images/circle_gr.ico' width='20px' height='20px' /> 100-999</span><br/><span> <img src='images/circle_lgr.ico' width='20px' height='20px'/> 1000-9999</span> <br/><span> <img src='images/circle_or.ico' width='20px' height='20px'/> &ge 10000</span>");

        google.maps.event.addListener(map, 'zoom_changed', function(){
            // this I have to send to the server
            //alert("map.getBounds(): " + map.getBounds() + "\n map.getCenter():" + map.getCenter() + "\n map.getZoom(): " + map.getZoom());
	    var bounds = map.getBounds();
	    var center = map.getCenter();
	    var zoomLevel = map.getZoom();
	    headerXML = createHeaderXML(bounds, center, zoomLevel);
	    bodyXML = "";
	    sendToServer_ScreenCenter_ScreenBounds(headerXML, bodyXML);	              
        });
    }
      
    function agregationPolygonsRemove(){
        jQuery("#voronoiT").attr('value', 'Enable triangle agregation');
        $("#legendInfo").html("");
        refreshButtons();
        google.maps.event.clearListeners(map, 'zoom_changed');        
    }
    
    $(function(){
        $("#voronoiT").toggle(function(){
            agregationPolygonsAdd();
        }, function(){
            agregationPolygonsRemove();
        });        	            
      });
      $(function(){
	$("#timeC1, #timeC2, #timeC3, #tab1ready, #tab2ready, #tab3ready, #tab4ready").hover(function(){
            $(this).removeClass("timeCstar");
            $(this).addClass("hover");
            
        }, function(){
            $(this).removeClass("hover");
            $(this).addClass("timeCstar");
        }); 
      });    
      
      $(function(){
	$("#SimileTimeController").hide('1');
        $('#SimileCheckbox').toggle( function()
	  { $("#SimileTimeController").slideDown('normal');},
	  function()
	  { $("#SimileTimeController").slideUp('normal');}
	); 	    
      });
      
      $(function(){
	$("#individualPolygonContent").hide('1');
        $('#individualPolygonCheckbox').toggle( function()
	  {$("#individualPolygonContent").slideDown('normal');},
	  function()
	  {$("#individualPolygonContent").slideUp('normal');});       
      });
	
      $(function(){
	$("#voronoiDiagramContent").hide('1');
        $('#voronoiDiagramCheckbox').toggle( function()
	  {$("#voronoiDiagramContent").slideDown('normal');},
	  function()
	  {$("#voronoiDiagramContent").slideUp('normal');});       
      });
      
      $(function(){
	$("#selectedYMDHContent").hide('1');
        $('#selectedYMDHCheckbox').toggle( function()
	  {$("#selectedYMDHContent").slideDown('normal');},
	  function()
	  {$("#selectedYMDHContent").slideUp('normal');});       
      });

      $(function(){
	$("#CalendarContent").hide('1');
        $('#CalendarcheckBox').toggle( function()
	  {$("#CalendarContent").slideDown('normal');},
	  function()
	  {$("#CalendarContent").slideUp('normal');});       
      });
 
    });
})(jQuery);
