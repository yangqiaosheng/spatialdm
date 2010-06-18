(function() {
	var carousel;

	YAHOO.util.Event.onDOMReady(function(ev) {
		var carousel = new YAHOO.widget.Carousel("container", {
			animation : {
				speed : 2
			},
			describedby : "my-carousel-label"
		});

		carousel.render(); // get ready for rendering the widget
			carousel.show(); // display the widget
		});
})();
