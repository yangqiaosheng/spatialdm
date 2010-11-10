(function($) {
$(document).ready(function(){       
   var listenerHandle;
   function agregationPolygonsAdd(){
           $("#voronoiT").attr('value', 'Disable triangle agregation');        
	   $("#legendInfo").html("<span>number of pictures </span><br/> <span> <img src='images/circle_bl.ico' width='20px' height='20px'/> 1-99 </span> <br/><span> <img src='images/circle_gr.ico' width='20px' height='20px' /> 100-999</span><br/><span> <img src='images/circle_lgr.ico' width='20px' height='20px'/> 1000-9999</span> <br/><span> <img src='images/circle_or.ico' width='20px' height='20px'/> &ge 10000</span>");	   
	    listenerHandle = google.maps.event.addListener(map, 'zoom_changed', function(){   
	    var bounds = map.getBounds();
	    var center = map.getCenter();
	    var zoomLevel = map.getZoom();
	    headerXML = createHeaderXML(bounds, center, zoomLevel);
	    bodyXML = "";
       	    sendToServer_ScreenCenter_ScreenBounds(headerXML, bodyXML);   
        });
    }
      
    function agregationPolygonsRemove(){      
        $("#legendInfo").html("");
        refreshButtons();
        google.maps.event.removeListener(listenerHandle);
    }     
    $(function(){			
		jQuery('#AggregationForm').change(function(){
			var value = jQuery("input:radio[name='group1']:checked").val();
			if ( value == 'Enable'){							
				agregationPolygonsAdd();
				//alert('Enable');
			}		
			else{				
				agregationPolygonsRemove();
				//alert('Disable');
			}
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
