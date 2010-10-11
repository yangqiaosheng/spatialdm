

function getSmallPhotos(areaid) {
	alert(areaid);
	$.get('SmallPhotoUrl', {
				areaid : areaid
			}, parsePhotosXml);
}

function parsePhotosXml(xml) {
	var ResultObj = $("#result");
//  alert($(xml).text());

	var xmlObj = $(xml);
	xmlObj.find("photo").each(function(i) {
//  alert($(this).children("smallUrl").text());
//  alert($(this).attr('index'));
        var itemObj = $("#item" + $(this).attr('index'));
        itemObj.html("<img src='" + $(this).children("smallUrl").text() + "'>");
//		ResultObj.html(ResultObj.html() + $(this).attr('index') + " " + $(this).children("date").text() + " <img src='"
//				+ $(this).children("smallUrl").text() + "'><br>");

		var itemdescObj = $("#itemdesc" + $(this).attr('index'));
        itemdescObj.html("<p>" + $(this).children("date").text() + "</p>");
	})

}