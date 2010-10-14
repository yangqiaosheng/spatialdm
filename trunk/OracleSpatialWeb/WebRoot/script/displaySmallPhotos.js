

function getSmallPhotos(areaid) {
	$.get('SmallPhotoUrl', {
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

function showPhoto(url) {
    var photoObj = $("#photo");
    photoObj.html("<img align='middle' src='" + url + "'>");
}

function removePhotoMaker() {
	g_photo_marker.setMap(null);
}

function parsePhotosXml(xml) {

	var xmlObj = $(xml);

	// clean the items
	for (var i = 1; i <= 15; i++) {
		var itemObj = $("#item" + i);
		itemObj.html("");
		itemObj.css('border', 'none');

		var itemdescObj = $("#itemdesc" + i)
		itemdescObj.html("");
		itemdescObj.fadeTo('slow', 0);
	}

	// fill the items with photos
	xmlObj.find("photo").each(function(j) {
				var index = $(this).attr('index');
				var url = $(this).children("smallUrl").text();
				var date = $(this).children("date").text();
				var longitude = $(this).children("longitude").text();
				var latitude = $(this).children("latitude").text();
				var viewed = $(this).children("viewed").text();
				var title = $(this).children("title").text();
				var personId = $(this).children("personId").text();
				var photoId = $(this).children("photoId").text();
				var rawTags = $(this).children("rawTags").text();

				var itemObj = $("#item" + index);
				itemObj.html("<div class='itemimg' id='itemimg" + index + "'><img align='middle' src='" + url + "'></div>");
// itemObj.html("<div class='itemimg' id='itemimg" + index + "' onmouseout='removePhotoMaker()' onmouseover='addPhotoMaker(" + latitude + ","
// + longitude + ")' ><img align='middle' src='" + url + "'></div>");
				itemObj.css('border', 'ridge');
				itemObj.css('border-width', 'medium');
				itemObj.css('border-color', 'blue');

				var itemimgObj = $("#itemimg" + index)
				itemimgObj.css('display', 'none');
				itemimgObj.fadeTo(1000, 0.1).fadeTo(1000, 1);
				itemimgObj.click(function() {
							 showPhoto(url);
						});

				var itemdescObj = $("#itemdesc" + index);
				itemdescObj.html(date);
				itemdescObj.fadeTo(1500, 1);
			})
}
