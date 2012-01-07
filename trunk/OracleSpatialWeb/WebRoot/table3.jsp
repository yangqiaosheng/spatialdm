<fieldset id="table3" class="tabcontent">
	<legend id="calendarLabel" class="tabLabel">
		<span>Select time from calendar</span>
	</legend>
	<div id="CalendarContent">
		<span id="intervalSelection" class="timeCstar"><b>Interval Selection</b>
		</span>
		<form id="validateForm" action="#" onsubmit="return false;">
			<input type="text" name="popupDatepicker1" id="popupDatepicker1" class="dpDate" />
			<input type="submit" class="submit" id="queryPopupDatepicker" value="Submit"></input>
		</form>
		<hr />
		<span id="individualDaysSelection" class="timeCstar"><b>Days Selection</b>
		</span>
		<br />
		<div id="inlineDatepicker"></div>
		<span> <span>
				<button name="button10_1" onclick="javascript:refreshCalendar();">
					Reset
				</button> </span> <span>
				<button name="calendardays" onclick="javascript:selectedCalendarDays();">
					Submit
				</button> </span> </span>
	</div>
</fieldset>