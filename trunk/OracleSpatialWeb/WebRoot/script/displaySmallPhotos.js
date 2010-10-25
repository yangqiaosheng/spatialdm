function getSmallPhotos(areaid) {
	$.get('SmallPhotoUrl', {
				areaid : areaid
			}, parsePhotosXml);
}

function addPhotoMaker(lat, lng, title) {
	g_photo_marker = new google.maps.Marker({
				position : new google.maps.LatLng(lat, lng),
				map : map,
				title : title
			});
}

function showPhoto(photoId, date, personId, rawTags, latitude, longitude, title, url) {
	var photoWindowObj = $("#photoWindow");
	photoWindowObj.fadeTo('fast', 1);

	var photoWindowDescObj = $("#photoWindowDesc");
	var photoWindowImgObj = $("#photoWindowImg");
	photoWindowImgObj.html("<img src='" + url + "'>");

	$("#legendInfo").html("<table border='1' bordercolor='gray'>" +
			"<tr><td>Title:</td><td>" + title + "</td></tr>" +
			"<tr><td>Person:</td><td>" + personId + "</td></tr>" +
			"<tr><td>Date:</td><td>"+date+"</td></tr>" +
			"<tr><td>RawTags:</td><td>"+rawTags+"</td></tr>" +
			"<table>");
}

function hidePhoto(){
	var photoWindowObj = $("#photoWindow");
	photoWindowObj.hide();
	$("#legendInfo").html("");
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
				var flickrWebUrl = "http://www.flickr.com/photos/" + personId +  "/" + photoId + "/";

				var itemObj = $("#item" + index);
				itemObj.html("<a href='" + flickrWebUrl + "' target='_blank'><div class='itemimg' id='itemimg" + index + "'><img align='middle' src='" + url + "'></div></a>");
// itemObj.html("<div class='itemimg' id='itemimg" + index + "' onmouseout='removePhotoMaker()' onmouseover='addPhotoMaker(" + latitude + ","
// + longitude + ")' ><img align='middle' src='" + url + "'></div>");
				itemObj.css('border', 'ridge');
				itemObj.css('border-width', 'medium');
				itemObj.css('border-color', 'gray');

				var itemimgObj = $("#itemimg" + index)
				itemimgObj.css('display', 'none');
				itemimgObj.css('cursor', 'pointer');
				itemimgObj.fadeTo(1000, 0.1).fadeTo(1000, 1);
//				itemimgObj.click(function() {
//							g_photo_selected = true;
//						});
				itemimgObj.mouseenter(function() {
//							g_photo_selected = false;
							addPhotoMaker(latitude, longitude, title);
							showPhoto(photoId, date, personId, rawTags, latitude, longitude, title, url);
						});
				itemimgObj.mouseleave(function() {
//							if(g_photo_selected != true){
								removePhotoMaker();
								hidePhoto();
//							}
						});

				var itemdescObj = $("#itemdesc" + index);
				itemdescObj.html(date);
				itemdescObj.fadeTo(1500, 1);
			})
}
