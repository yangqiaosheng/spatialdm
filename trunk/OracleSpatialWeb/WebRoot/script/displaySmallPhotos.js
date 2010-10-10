function getSmallPhotos() {
	//    alert($("#userName"));
	var JQueryObj = $("#username");
	var username = JQueryObj.val();
	alert(username);
	$.get('SmallPhotoUrl', { areaid: username, radius: '20000'}, parseXml(), "xml");

}

function parseXml(xml) {


	var ResultObj = $("#result");


	var xmlObj = $(xml);
    xmlObj.find("photo").each(function(i){
                            var oId = $(this).attr("id");
                            var IdValue = oId.text();
                            var oUrl = $(this).chlidren("url");
                            ResultObj.html(IdValue + " " +  oUrl.value() + "<br>");
						})
alert($(xml).find("id").text());
}