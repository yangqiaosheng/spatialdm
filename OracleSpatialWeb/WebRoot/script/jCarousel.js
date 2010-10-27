g_carouselPageSize = 15;
g_carouselTotalSize = 0;
g_jcarousel = null;

function carousel_addEmptyItems(carousel, start, num){
	for(var i = start; i <= start + num; i++){
		carousel.add(i, carousel_getItemHTML(i, null, null));
		var itemObj = $("#item" + i);
		itemObj.fadeTo(0, 0);
	}
	g_carouselTotalSize += num;
}

function carousel_initCallback(carousel) {
//	jQuery('#photoInit').bind('click', function() {
//		alert("change");
//        carousel.scroll(1);
//        cleanPhotoItems();
//        g_carouselTotalSize = g_carouselPageSize;
//        return false;
//    });

	g_jcarousel = carousel;
	carousel_addEmptyItems(carousel, 1, g_carouselPageSize);
}

function carousel_getItemHTML(i, item, itemdesc){
	 return "<div class='item' id='item" + i + "'></div> <div class='itemdesc' id='itemdesc" + i + "'></div>";
};

function carousel_itemLastInCallback(carousel, state, last)
{
    // Since we get all URLs in one file, we simply add all items
    // at once and set the size accordingly.
//    if (state != 'init')
//        return;
//
//    jQuery.get('dynamic_ajax.txt', function(data) {
//        mycarousel_itemAddCallback(carousel, carousel.first, carousel.last, data);
//    });

	if(carousel.last >= g_carouselTotalSize){
//		alert(g_carouselTotalSize);
//		alert(carousel.last);
		carousel_addEmptyItems(carousel, g_carouselTotalSize + 1, g_carouselPageSize);
		getSmallPhotos(g_areaid, g_carouselTotalSize - g_carouselPageSize + 1);
//		carousel.scroll(1);
	}

};

function jcarouselInit(){
	jQuery('#carousel').jcarousel({
    	 initCallback: carousel_initCallback,
    	 itemLastInCallback: carousel_itemLastInCallback
    });
    var carousel = jQuery('#carousel').data('jcarousel'); // taking the instance
}

$(document).ready(function(){
     jcarouselInit();
});
