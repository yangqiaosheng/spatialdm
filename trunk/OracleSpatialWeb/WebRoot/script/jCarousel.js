g_carouselPageSize = 60;
g_carouselTotalSize = 0;
g_carouselEnd = true;
g_jcarousel = null;

function carousel_addEmptyItems(carousel, start, num){
	g_carouselTotalSize = (num + g_carouselTotalSize);
	carousel.size(g_carouselTotalSize);

	for(var i = start; i <= (start + num - 1); i++){
		if (carousel.has(i)) {
            continue;
        }
		carousel.add(i, carousel_getItemHTML(i, null, null));
		var itemObj = $("#item" + i);
		itemObj.fadeTo(0, 0);
	}
}

function carousel_getItemHTML(i, item, itemdesc){
	 return "<div class='item' id='item" + i + "'></div> <div class='itemdesc' id='itemdesc" + i + "'></div>";
};

function carousel_initCallback(carousel) {
	g_jcarousel = carousel;
}



function carousel_itemLastInCallback(carousel, state, last){
	if((carousel.last >= g_carouselTotalSize - 20) && (g_carouselTotalSize >= g_carouselPageSize) && (g_carouselEnd == false)){
		alert("call:" + g_carouselTotalSize);
		getSmallPhotos(g_areaid, (g_carouselTotalSize / g_carouselPageSize + 1));
	}

};

function jcarouselInit(){
	jQuery('#carousel').jcarousel({
		size: 0,
    	initCallback: carousel_initCallback,
    	itemLastInCallback: carousel_itemLastInCallback
    });
    var carousel = jQuery('#carousel').data('jcarousel'); // taking the instance
}

$(document).ready(function(){
     jcarouselInit();
});
