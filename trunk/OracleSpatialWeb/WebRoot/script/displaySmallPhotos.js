

function getSmallPhotos(areaid) {
	$.get('http://kd-photomap.iais.fraunhofer.de/OracleSpatialWeb/SmallPhotoUrl', {
				areaid : areaid
			}, parsePhotosXml);
}

function addPhotoMaker(lat, lng) {
	g_photo_marker = new google.maps.Marker({
				position : new google.maps.LatLng(lat, lng),
				map : map,
				title : lat + "," + lng
			});
}

function removePhotoMaker() {
	g_photo_marker.setMap(null);
}

function parsePhotosXml(xml) {

	var xmlObj = $(xml);

	//clean the items
	for (var i = 1; i <= 15; i++) {
		var itemObj = $("#item" + i);
		itemObj.html("");
		itemObj.css('border', 'none');

		var itemdescObj = $("#itemdesc" + i)
		itemdescObj.html("");
		itemdescObj.fadeTo('slow', 0);
	}

	//fill the items with photos
	xmlObj.find("photo").each(function(j) {
		var itemObj = $("#item" + $(this).attr('index'));
		itemObj.html("<div class='itemimg' onmouseout='removePhotoMaker()' onmouseover='addPhotoMaker(" + $(this).children("latitude").text() + ","
				+ $(this).children("longitude").text() + ")' ><img align='middle' src='" + $(this).children("smallUrl").text() + "'></div>");
		itemObj.css('border', 'ridge');
		itemObj.css('border-width', 'medium');
		itemObj.css('border-color', 'blue');

		var itemdescObj = $("#itemdesc" + $(this).attr('index'));
		itemdescObj.html($(this).children("date").text());
		itemdescObj.fadeTo('slow', 1);
	})
}
