function getSmallPhotos() {
	//    alert($("#userName"));
	var JQueryObj = $("#username");
	var username = JQueryObj.val();
	alert(username);
	$.get('SmallPhotoUrl', { areaid: username, radius: '20000'}, callback);
}

function callback(data) {
//    alert(data);
	var ResultObj = $("#result");
	ResultObj.html(data);
}