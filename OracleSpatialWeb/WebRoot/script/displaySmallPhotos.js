function getSmallPhotos() {
	//    alert($("#userName"));
	var JQueryObj = $("#username");
	var username = JQueryObj.val();
	alert(username);
	$.post('TestServlet', "areaid=" + username, callback);
}

function callback(data) {
//    alert(data);
	var ResultObj = $("#result");
	ResultObj.html(data);
}