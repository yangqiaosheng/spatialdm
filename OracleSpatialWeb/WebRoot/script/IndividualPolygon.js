$(document).ready(function(){
    var creator = null;
    jQuery('#reset').click(function(){
        if (creator != null) {
            creator.destroy();
            creator = null;
            $("#reset").attr('value', 'Paint');
            $("#legendInfo").html("");
        }
        else {
        
            creator = new PolygonCreator(map);
            $("#reset").attr('value', 'Reset');	    
            $("#legendInfo").html("<p>The last point of the new created polygon must be the same with the first point. (Close the polygon)</p>");
        }
    });
    jQuery('#askServer').click(function(){
        $("#showhide").attr('value', 'hide');
        if (null == creator.showData()) {
            alert("In order to see results you have to create a polygon");
        }
        else {
            alert("message sent to server!");
            $("#legendInfo").html("<p>You will be informed when the result will be computed. It may take few minutes </p>");
	    
	    var bounds = map.getBounds();
	    var center = map.getCenter();
	    var zoomLevel = map.getZoom();
	    headerXML = createHeaderXML(bounds, center, zoomLevel);    
	    var text1="";
	    text1=text1+creator.showData();	   
	    bodyXML = individualPolygonXml(text1);    	    
	    sendToServerCalendarData(headerXML, bodyXML);
        }
    });
});
