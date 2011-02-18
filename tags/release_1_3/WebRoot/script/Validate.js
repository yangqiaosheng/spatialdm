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
                //	validMinPicker: {dpMinDate: []},
                //	validMaxPicker: {dpMaxDate: []},
                //	validMinMaxPicker: {dpMinMaxDate: []}
            },
            messages: {
                //	validRangePicker: 'Please enter a valid date range',
                popupDatepicker1: 'Invalid Date!'
            }
        });
    });
});