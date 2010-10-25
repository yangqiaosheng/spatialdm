<div id="table1" class="tabcontent">

    <span id="table_header">
      <span id="headline">
		<span id="idmove"><img id="calImg" width="20" height="20" src="images/hand_cursor.png" /></span>		
		<span class="head" class="headerButtonbefore" id="Introduction"><b>Introduction</b></span>	  
		<span class="head" class="headerButtonbefore" id="Pictures"><b>Pictures </b></span>
		<span class="head" class="headerButtonbefore" id="Properties"><b>Properties</b></span>
		<span class="head" class="headerButtonbefore" id="Others"><b>Others</b></span>	 
      </span>     
	  
	  
	  
	  
	<span id="content" >
	  <span id="content1" class="tabcontent" style="display: block;">
	    <span id="content1_1">text1</span>	      
		<span>
		  <span><i>Draw a polygon </i></span>		 
		  <br/>
		    <span>
		      <input type="button" id="reset" value="Paint" />
		      <input type="button" id="askServer" value="Submit" />
		    </span>		 
		</span>
		<span>
			<span><i>Aggregation depends on scale</i></span>
			<br/>
		   	<form name="AggregationForm">		
				<span>
					<input type="radio" name="group1" value="Enable"> Enable </input> 
					<input type="radio" name="group1" value="Disable" checked> Disable </input>
				</span>
			</form>
		</span>
	  </span>
	  <span id="content2">
	    <button name="testKML" onclick="loadTestKml()">Load KML</button>  
	    <button name="button4" onclick="refresh()"> Refresh page! </button>
	  </span>
	  <span id="content3" style="display: none;">
	    <p>
	      <input type="checkbox" onclick="activateZoom(this.checked)" id="scrollWheelZoomActivation" CHECKED>
	      </input><strong>Check or uncheck to activate scroll wheel zoom </strong>
	    </p>
	    <p>
	      <input name="search" type="text" id="search" size="25" autocomplete="off"></input>
	      <button name="button1x" onclick="SearchLocation()"> Search</button>
	    </p>
	  </span>
	  <span id="content4" class="new_tabcontent" style="display: none;">
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
	  </span>
	</span>
    
    </span>

</div>
