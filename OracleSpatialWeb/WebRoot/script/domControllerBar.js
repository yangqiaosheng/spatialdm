
YAHOO.util.Event.onDOMReady( function () {
	var Dom = YAHOO.util.Dom,
		Event = YAHOO.util.Event;

	var pb = new YAHOO.widget.ProgressBar({value:0,minValue:-150,maxValue:-150}).render('basic');	
	var rangeSlider = YAHOO.widget.Slider.getHorizDualSlider("sliderRange",
        "sliderRangeMinThumb", "sliderRangeMaxThumb",
        300, 0, [0,300]
	);
	rangeSlider.animate = false;
	rangeSlider.subscribe('change', function() {
		var value = Math.round(this.minVal  - 150);
		Dom.get('minValue').innerHTML = value;
		value = Math.round(this.maxVal  - 150);
		Dom.get('maxValue').innerHTML = value;
	});
	rangeSlider.subscribe('slideEnd', function() {
		var value = Math.round(this.minVal - 150);
		pb.set('minValue',value);
		value = Math.round(this.maxVal - 150);
		pb.set('maxValue',value);
	});


});
