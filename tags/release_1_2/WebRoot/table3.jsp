<div id="table3" class="tabcontent">
  <span>
    <input type="checkbox" id="CalendarcheckBox"><b id="timeC2" class="timeCstar">Select time from calendar</b></input>
  </span>
  <div id="CalendarContent">
    <br/>
    <span id="intervalSelection" class="timeCstar">Select an interval</span>    
    <form id="validateForm" action="#" onsubmit="return false;">
      <p>
	<input type="text" name="popupDatepicker1" id="popupDatepicker1" class="dpDate" />
	<br/>
	<input type="submit" class="submit" id="queryPopupDatepicker" value="Submit"></input>
      </p>
    </form>
    <br/>
    <span id="individualDaysSelection" class="timeCstar">Select specific days</span>
    <br/>
    <div id="inlineDatepicker"></div>
    <span>
      <span>
	<button name="button10_1" onclick="javascript:refreshCalendar();"> Reset </button> 
      </span>
      <span>
	<button name="calendardays" onclick="javascript:selectedCalendarDays();"> Submit </button>
      </span>
    </span>
  </div>
</div>