var smallUrl = new Array();
var photoId = new Array();
var polygonId = new Array();
var polygonRadius = new Array();
var date = new Array();
var personId = new Array();
var titlePicture = new Array();
var mycarousel_itemList = new Array();

var dateTotal = new Array();
var weekdayTotal = new Array();
var titlePictureTotal = new Array();
var latitudeTotal = new Array();
var longitudeTotal = new Array();
var smallUrlTotal = new Array();
var contorTotal = 0;
var poz = 0;

var ids = "";
var last = 0;
//var Ibool = false;

var g_jcarousel = null;
var page_size = 30;
var lastCarousel = 0;
var booleanF = false;
var booleanF2 = false;
var g_carouselTotalSize = 0;
var timeofUpload = 0;
var xmldoc;
var g_photo_marker = null;
var one_stepCarousel = 5;
var beforeLoad = 7;
var globalPolygonSelected = -1;
var isTagCarousel = false;
var selectedTag = null;
var selectedQueryDateStr = null;
var tagNum = 0;

function scrollEvtHandler(obj) {
	//console.log("SmallPhotos 1");
	//console.log("SmallPhotos 1 g_carouselTotalSize: "+g_carouselTotalSize);
	lastCarousel = obj.last;
	if ((obj.last > g_carouselTotalSize - beforeLoad) && (g_carouselTotalSize - beforeLoad) > 0) {
		timeofUpload++;
		page = page + 1;
		if (isTagCarousel == false) {
			sendToServerFromCarousel(ids, page_size, page);
		} else {
			sendToServerFromTagCarousel(ids, page_size, page, selectedTag, selectedQueryDateStr);
		}
	}
}

function loadTheCarousel(k, g_jcarousel, smallUrl) {
	for ( var i = 1; i <= smallUrl.length; i++) {
		g_jcarousel.addItem(mycarousel_getItemHTML(i, k, smallUrl[i - 1]));
		g_jcarousel.render();
		g_jcarousel.show();
		assignEventsForTheCarouselItems(i, k, i + k);
	}
	var tagInfo = "";
	var selectedNum = sel[globalPolygonSelected];
	if(selectedTag != null){
		tagInfo = "with tag '" + selectedTag + "' on " + selectedQueryDateStr;
		selectedNum = tagNum;
	}
	$('#numberOfItems').empty().html(
			"<span> Number of pictures selected: " + selectedNum + "</span> " + tagInfo + " <span style='color:#999999'> polygon_id= " + globalPolygonSelected + " </span>");//globalvar
}

function assignEventsForTheCarouselItems(i, k, t) {
	var itemSubObj = $("#subitem" + t);
	var itemObj = $("#item" + t);
	itemObj.bind('mouseover', (function() {
		itemSubObj.css("background-color", "#A7BDF7");
		$("#pictureMouseOver").html(
				"<img src='" + smallUrlTotal[t - 1] + "'/><div class='tabcontent'>" + weekdayTotal[t - 1] + " " + dateTotal[t - 1] + "</div><div class='tabcontent'>"
						+ titlePictureTotal[t - 1].substr(0, 30) + "</div>");
		$("#pictureMouseOver").show();
		addPhotoMaker(latitudeTotal[t - 1], longitudeTotal[t - 1], titlePictureTotal[t - 1]);
	}));

	itemObj.bind('mouseout', (function() {
		$("#pictureMouseOver").hide();
		$("#pictureMouseOver").html("");
		itemSubObj.css("background-color", "#CCFFFF");
		removePhotoMaker();
	}));
}

function addPhotoMaker(lat, lng, titler) {
	if (g_photo_marker != null) {
		g_photo_marker.setMap(null);
	}
	g_photo_marker = new google.maps.Marker( {
		position : new google.maps.LatLng(lat, lng),
		map : map,
		title : titler
	});
	g_photo_marker.setMap(map);
}

function removePhotoMaker() {
	g_photo_marker.setMap(null);
}

function mycarousel_getItemHTML(i, k, item) {
	return "<li><div class = 'item' id = 'item" + (i + k) + "'><a href='http://www.flickr.com/photos/" + personId[i - 1] + "/" + photoId[i - 1] + "' target='_blank'><img src='"
			+ smallUrl[i - 1] + "' height=100% /></a></div></li>";
}

//this function is called from the sendToServer
function carouselLoadPictures(xml) {
	setTheParameters();
	readXml(xml);
	xmldoc = null;
	if (booleanF2 == false) {
		//console.log("carousel");
		YAHOO.widget.Carousel.prototype.STRINGS.NEXT_BUTTON_TEXT = "<img src='right-enabled.gif'/> ";
		YAHOO.widget.Carousel.prototype.STRINGS.PREVIOUS_BUTTON_TEXT = "<img src='left-enabled.gif'/> ";
		YAHOO.widget.Carousel.prototype.STRINGS.PAGER_PREFIX_TEXT = "Go to page: ";
		//YAHOO.widget.Carousel.prototype.STRINGS.FIRST_PAGE= "1";
		g_jcarousel = new YAHOO.widget.Carousel("carousel", {
			animation : {
				speed : 0.5,
				effect : null
			},
			numVisible : one_stepCarousel,
			scrollInc : one_stepCarousel
		});
		g_jcarousel.addListener("afterScroll", scrollEvtHandler);
		g_jcarousel.render();
		g_jcarousel.show();
		g_carouselTotalSize = g_carouselTotalSize + smallUrl.length;
		$(".yui-carousel-nav").css("background", "url('images/ui-bg_inset-hard_100_f5f8f9_1x30.png') repeat-x scroll 0 0 transparent");
		$(".yui-carousel-nav").css("height", "30px");
		//console.log("g_carouselTotalSize: "+g_carouselTotalSize);
		timeofUpload++;
		loadTheCarousel(0, g_jcarousel, smallUrl);
		booleanF2 = true;
	}
	if (lastCarousel > g_carouselTotalSize - beforeLoad) {
		loadTheCarousel(g_carouselTotalSize, g_jcarousel, smallUrl);
		g_carouselTotalSize = g_carouselTotalSize + smallUrl.length;
	}
	// console.log("ready to execute is true");
	// readyToExecute=true;
}

function readXml(xml) {
	setTheParameters();
	$(xml).find('photo').each(function() {
		photoId[poz] = $(this).find('photoId').text();
		titlePicture[poz] = $(this).find('title').text();
		date[poz] = $(this).find('date').text();
		personId[poz] = $(this).find('personId').text();
		smallUrl[poz] = $(this).find('smallUrl').text();
		weekdayTotal[contorTotal] = $(this).find('weekday').text();
		smallUrlTotal[contorTotal] = $(this).find('smallUrl').text();
		titlePictureTotal[contorTotal] = $(this).find('title').text();
		dateTotal[contorTotal] = $(this).find('date').text();
		latitudeTotal[contorTotal] = $(this).find('latitude').text();
		longitudeTotal[contorTotal] = $(this).find('longitude').text();
		contorTotal++;
		poz++;
	});
}

function setTheParameters() {

	///??  $(".yui-carousel-nav.form.select").val("Go to page: 1");
	// $("select option[value='Go to page: 1']").attr("selected", true);
	for ( var i = 0; i < smallUrl.length; i++) {
		smallUrl[i] = "";
		photoId[i] = "";
		polygonId[i] = "";
		polygonRadius[i] = "";
		date[i] = "";
		personId[i] = "";
		titlePicture[i] = "";
		mycarousel_itemList[i] = "";
	}
	smallUrl = new Array();
	photoId = new Array();
	polygonId = new Array();
	polygonRadius = new Array();
	date = new Array();
	personId = new Array();
	titlePicture = new Array();
	mycarousel_itemList = new Array();
	poz = 0;
}

function cleanPhotos() {
	if (g_jcarousel != null) {
		g_jcarousel.clearItems();
		g_jcarousel.scrollTo(1, false);
		g_jcarousel.set("selectedItem", 1);
	}
	dateTotal = new Array();
	weekdayTotal = new Array();
	titlePictureTotal = new Array();
	latitudeTotal = new Array();
	longitudeTotal = new Array();
	smallUrlTotal = new Array();
	contorTotal = 0;
	setTheParameters();
	//g_carouselTotalSize = 0;
	lastCarousel = 0;
	page = 1;
	booleanF = false;
}

function getId() {
	return ids;
}

function setCarousel(showCarousel, ids) {
	if(showCarousel == true){
		$("#maxContainer").show("slow");
	}
	selectedTag = null;
    selectedQueryDateStr = null;
    tagNum = 0;
	isTagCarousel = false;
	g_carouselTotalSize = 0;
	//console.log("SmallPhotos 2");
	$("#maxContainer").removeClass('invisible').addClass('visible');
	page = 1;
	cleanPhotos();
	sendToServerFromCarousel(ids, page_size, page);
}

function setTagCarousel(showCarousel, ids, tag, queryDateStr, num) {
	if(showCarousel == true){
		$("#maxContainer").show("slow");
	}
	$('#numberOfItems').empty().html(
				"<span> Number of pictures selected: " + num + " tag '" + tag + "' on " + queryDateStr + "<span style='color:#999999'> polygon_id= " + ids+ " </span>"
						+ " <img src='images/89.gif' height='20' width='20'/> </span>");
	isTagCarousel = true;
	tagNum = num;
	selectedTag = tag;
	selectedQueryDateStr = queryDateStr;
	g_carouselTotalSize = 0;
	//console.log("SmallPhotos 2");
	$("#maxContainer").removeClass('invisible').addClass('visible');
	page = 1;
	cleanPhotos();
	sendToServerFromTagCarousel(ids, page_size, page, selectedTag, selectedQueryDateStr);
}
