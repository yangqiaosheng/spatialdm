
		function radio_buttons() {
		  text_R_Buttons = "";
		  for ( var i = 0; i < document.radio_form.time.length; i++) {
		  if (document.radio_form.time[i].checked) {
			if (i == 0) {
				//alert("year selection");
				text_R_Buttons = text_R_Buttons
						+ "<select name='numbers' id='numbers'><option value='1'>1</option><option value='2'>2</option><option value='3'>3</option><option value='4'>4</option><option value='5'>5</option><option value='6'>6</option></select>";
				document.getElementById("unit").innerHTML = text_R_Buttons;
				text_R_Buttons = "";
				break;
			}
			if (i == 1) {
				//alert("month selection");
				text_R_Buttons = text_R_Buttons
						+ "<select name='numbers' id='numbers'><option value='1'>1</option>"
						+ "<option value='2'>2</option>"
						+ "<option value='3'>3</option>"
						+ "<option value='4'>4</option>"
						+ "<option value='5'>5</option>"
						+ "<option value='6'>6</option>"
						+ "<option value='7'>7</option>"
						+ "<option value='8'>8</option>"
						+ "<option value='9'>9</option>"
						+ "<option value='10'>10</option>"
						+ "<option value='11'>11</option>"
						+ "<option value='12'>12</option>"
						+ "</select>";
				document.getElementById("unit").innerHTML = text_R_Buttons;
				text_R_Buttons = "";
				break;
			}
			if (i == 2) {
				text_R_Buttons = "";
				//alert("day selection");
				text_R_Buttons = text_R_Buttons
						+ "<select name='numbers' id='numbers'><option value='1'>1</option>"
						+ "<option value='2'>2</option>"
						+ "<option value='3'>3</option>"
						+ "<option value='4'>4</option>"
						+ "<option value='5'>5</option>"
						+ "<option value='6'>6</option>"
						+ "<option value='7'>7</option>"
						+ "<option value='8'>8</option>"
						+ "<option value='9'>9</option>"
						+ "<option value='10'>10</option>"
						+ "<option value='11'>11</option>"
						+ "<option value='12'>12</option>"
						+ "<option value='13'>13</option>"
						+ "<option value='14'>14</option>"
						+ "<option value='15'>15</option>"
						+ "<option value='16'>16</option>"
						+ "<option value='17'>17</option>"
						+ "<option value='18'>18</option>"
						+ "<option value='19'>19</option>"
						+ "<option value='20'>20</option>"
						+ "<option value='21'>21</option>"
						+ "<option value='22'>22</option>"
						+ "<option value='23'>23</option>"
						+ "<option value='24'>24</option>"
						+ "<option value='25'>25</option>"
						+ "<option value='26'>26</option>"
						+ "<option value='27'>27</option>"
						+ "<option value='28'>28</option>"
						+ "<option value='29'>29</option>"
						+ "<option value='30'>30</option>"
						+ "<option value='31'>31</option>"
						+ "</select>";
				document.getElementById("unit").innerHTML = text_R_Buttons;
				text_R_Buttons = "";
				break;
			}
			if (i == 3)
				text_R_Buttons = "";
			//alert("hour selection");
			text_R_Buttons = text_R_Buttons
					+ "<select name='numbers' id='numbers'><option value='1'>1</option>"
					+ "<option value='2'>2</option>"
					+ "<option value='3'>3</option>"
					+ "<option value='4'>4<send/option>"
					+ "<option value='5'>5</option>"
					+ "<option value='6'>6</option>"
					+ "<option value='7'>7</option>"
					+ "<option value='8'>8</option>"
					+ "<option value='9'>9</option>"
					+ "<option value='10'>10</option>"
					+ "<option value='11'>11</option>"
					+ "<option value='12'>12</option>"
					+ "<option value='13'>13</option>"
					+ "<option value='14'>14</option>"
					+ "<option value='15'>15</option>"
					+ "<option value='16'>16</option>"
					+ "<option value='17'>17</option>"
					+ "<option value='18'>18</option>"
					+ "<option value='19'>19</option>"
					+ "<option value='20'>20</option>"
					+ "<option value='21'>21</option>"
					+ "<option value='22'>22</option>"
					+ "<option value='23'>23</option>"
					+ "<option value='24'>24</option>"
					+ "</select>";
			document.getElementById("unit").innerHTML = text_R_Buttons;
			text_R_Buttons = "";
			break;

			}
		     }
  
		}

		$(document).ready(function(){				
			var select = $("#minbeds");
			var slider = $('<div id="slider"></div>').insertAfter('#minbeds').slider({
				min: 0,
				max: 6,															     range:true, 
				value:[1,4], 			
				slide:function(event, ui) {						
					$("#amount").val(ui.values[1]-ui.values[0]);
					select[0].selectedIndex = ui.value;
							
			    }
			});
//			$("#minbeds").click(function() {
//				slider.slider("value", this.selectedIndex);
//			});
							
		});
				
		 
