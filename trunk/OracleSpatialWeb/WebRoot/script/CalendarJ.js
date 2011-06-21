var selectedDays;

$(document).ready(function() {
	$('#popupDatepicker1').datepick( {
		renderer : $.datepick.themeRollerRenderer,
		showTrigger : '#calImg',
		//showOnFocus: false,                   
		rangeSelect : true,
		monthsToShow : 2,
		dateFormat : 'dd/mm/yyyy',
		yearRange : '2005:2011'
	});
	//********************************************************************************************************
	$('#inlineDatepicker').datepick( {
		onSelect : function(date) {
			selectedDays = date;
			//alert('You picked ' + selectedDays); 
		},
		renderer : $.datepick.themeRollerRenderer,
		monthsToShow : 2,
		yearRange : '2005:2011',
		dateFormat : 'dd/mm/yyyy',
		multiSelect : 300
	}); /*the maximum number that one can select by hand*/

});

function selectedCalendarDays() {

	var bounds = map.getBounds();
	var center = map.getCenter();
	var zoomLevel = map.getZoom();
	headerXML = createHeaderXML(bounds, center, zoomLevel);

	var selectedCalendarDays = "";
	var selectedResult = "";
	for ( var i = 0; i < selectedDays.length; i++) {
		selectedCalendarDays = selectedCalendarDays + "" + selectedDays[i];
	}
	var patt1 = /(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[ ](0[1-9]|1[0-9]|2[0-9]|3[0-1])[ ](20[0-1][0-9])/g;
	selectedResult = selectedCalendarDays.match(patt1);
	bodyXML = selectedDaysXML(selectedResult);
	sendToServerCalendarData(headerXML, bodyXML);
}
function refreshCalendar() {
	deleteHistory();
	$('#inlineDatepicker').datepick('clear'); // Close a pop up datepicker and clear its field 
	$('#inlineDatepicker').datepick('destroy'); // Remove datepicker functionality 
	$('#inlineDatepicker').datepick( {
		onSelect : function(date) {
			selectedDays = date;
			//alert('You picked ' + selectedDays); 
		},
		renderer : $.datepick.themeRollerRenderer,
		monthsToShow : 2,
		yearRange : '2005:2011',
		dateFormat : 'dd/mm/yyyy',
		multiSelect : 300
	}); /*the maximum number that one can select by hand*/
}
