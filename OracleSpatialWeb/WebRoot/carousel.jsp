
<!--*****************************************Carousel Code*********************************************************************************-->
<div id="maxContainer">
	<div id="container">
		<ul id="carousel" class="jcarousel-skin-tango">
			<% for(int i = 1 ; i <= 15 ; i++){ %>
			<li>
				<div class="item" id="item<%=i %>"></div>
				<div class="itemdesc" id="itemdesc<%=i %>"></div>
			</li>
			<% } %>
		</ul>
	</div>
</div>