<div id="table1">
  <span>
    <table id="table_header">
      <tr>
	<td>
	  <span id="idmove"><img id="calImg" width="20" height="20" src="images/hand_cursor.png" /></span>
	</td> 
	<td>
	  <div id="tab1focus" class="tab tabfocus" style="display: block;">
	    <b>Introduction</b>
	  </div>
	  <div id="tab1ready" class="tab tabhold" style="display: none;">
	    <div onclick="ManageTabPanelDisplay('tab1focus','tab2ready','tab3ready', 'tab4ready', 'content1')">
	      <b>Introduction</b>
	    </div>
	  </div>
	</td>
	<td>
	  <div id="tab2focus" class="tab tabfocus" style="display: none;">
	    <b>Pictures </b>
	  </div>
	  <div id="tab2ready" class="tab tabhold" style="display: block;">
	    <div onclick="ManageTabPanelDisplay('tab1ready','tab2focus','tab3ready','tab4ready','content2')">
	      <b>Pictures</b>
	    </div>
	  </div>
	</td>
	<td>
	  <div id="tab3focus" class="tab tabfocus" style="display: none;">
	    <b>Properties</b>
	  </div>
	  <div id="tab3ready" class="tab tabhold" style="display: block;">
	    <div onclick="ManageTabPanelDisplay('tab1ready','tab2ready','tab3focus', 'tab4ready','content3')">
	      <b>Properties</b>
	    </div>
	  </div>
	</td>
	<td>
	  <div id="tab4focus" class="tab tabfocus" style="display: none;">
	    <b>Others</b>
	  </div>
	  <div id="tab4ready" class="tab tabhold" style="display: block;">
	    <div onclick="ManageTabPanelDisplay('tab1ready','tab2ready','tab3ready','tab4focus', 'content4')">
	      <b>Others</b>
	    </div>
	  </div>
	</td>
      </tr>
      <tr>      
	<td id="content" colspan="8">
	  <div id="content1" class="tabcontent" style="display: block;">
	    <span id="content1_1">text1</span>	      
		<div>
		  <span><input type="checkbox" id = "individualPolygonCheckbox"><i id="individualPolygon" class="timeCstar">Draw polygond for individual query</i> </input> </span>
		  <div id = "individualPolygonContent">
		    <span>
		      <input type="button" id="reset" value="Paint" />
		      <input type="button" id="askServer" value="Query" />
		    </span>
		  </div>
		</div>
		<div>
		  <span><input type="checkbox" id="voronoiDiagramCheckbox" /><i id="voronoiTriangles" class="timeCstar">Make use of the given database polygons</i></span>
		  <div id = "voronoiDiagramContent">
		    <input type="button" id="voronoiT" value="Enable triangle agregation" />
		  </div>
		</div>			      
	  </div>
	  <div id="content2" class="tabcontent" style="display: none;">
	    <button name="button3" onclick="mapAddMakers()"> Markers </button>
	    <button name="button5" onclick="drawPolylines()"> Polylines </button>
	    <button name="button100" onclick="addMapGPolygon()"> wo KML </button>
	    <button name="button4" onclick="refresh()"> Refresh page! </button>
	  </div>
	  <div id="content3" class="tabcontent" style="display: none;">
	    <p>
	      <input type="checkbox" onclick="activateZoom(this.checked)" id="scrollWheelZoomActivation" CHECKED>
	      </input><strong>Check or uncheck to activate scroll wheel zoom </strong>
	    </p>
	    <p>
	      <input name="search" type="text" id="search" size="25" autocomplete="off"></input>
	      <button name="button1x" onclick="SearchLocation()"> Search</button>
	    </p>
	  </div>
	  <div id="content4" class="new_tabcontent" style="display: none;">
	    <ul id="verticalmenu" class="glossymenu">
	      <li>
		<a href="javascript:AddPolynomsOnMilan()">Milano</a>
	      </li>
	      <li>
		<a href="javascript:addxml1()">Add Polygons</a>
	      </li>
	      <li>
		<a href="javascript:addxml2()">Add Polygons</a>
	      </li>
	      <li>
		<a href="#">button1</a>
		<ul>
		  <li>
		    <a href="javascript:alert('a javascript function call')">details on polygons</a>
		  </li>
		  <li>
		    <a href="javascript:alert('a javascript function call')">button1</a>
		  </li>
		  <li>
		    <a href="javascript:alert('a javascript function call')">button1</a>
		  </li>
		</ul>
	      </li>
	      <li>
		<a href="javascript:addxml3()">original Controller 1</a>
	      </li>
	      <li>
		<a href="javascript:alert('a javascript function call')">button1</a>
	      </li>
	      <li>
		  <a href="javascript:alert('a javascript function call')">button1</a>
	      </li>
	    </ul>
	  </div>
	</td>
      </tr>
    </table>
  </span>
</div>