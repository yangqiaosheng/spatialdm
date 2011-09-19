$(document).ready(function () {
    $.validator.setDefaults({
        submitHandler: function () {
            var textPopCalendar = $("#popupDatepicker1").val();
            var bounds = map.getBounds();
            var center = map.getCenter();
            var zoomLevel = map.getZoom();
            headerXML = createHeaderXML(bounds, center, zoomLevel);
            bodyXML = intervalXML(textPopCalendar);
            sendToServerCalendarData(headerXML, bodyXML);
        }
    });
    $(function () {
        $('#validateForm').validate({
            errorPlacement: $.datepick.errorPlacement,
            rules: {         
            },
            messages: {                
                popupDatepicker1: 'Invalid Date!'
            }
        });
    });
});