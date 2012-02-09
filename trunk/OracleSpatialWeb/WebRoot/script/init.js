var _gaq = _gaq || [];
_gaq.push( [ '_setAccount', 'UA-22122163-1' ]);
_gaq.push( [ '_trackPageview' ]);

(function() {
	var ga = document.createElement('script');
	ga.type = 'text/javascript';
	ga.async = true;
	ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
	var s = document.getElementsByTagName('script')[0];
	s.parentNode.insertBefore(ga, s);
})();

$(function() {
	var mozilla = false;
	var version = 0;
	jQuery.each(jQuery.browser, function(i, val) {
		if (i == "mozilla") {
			mozilla = val;
		} else if (i == "version") {
			version = val;
		}
	});

	if (mozilla == false || version < 4.0 || screen.height < 1024 || screen.width < 1280) {
		$("#dialog-confirm").dialog( {
			resizable : false,
			width : 420,
			height : 280,
			modal : true,
			buttons : {
				"Continue" : function() {
					$(this).dialog("close");
				}
			}
		});
	}

});