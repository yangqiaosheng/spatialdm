<div id="table3" class="tabcontent">
  <span>
    <input type="checkbox" id="CalendarcheckBox"><b id="timeC2" class="timeCstar">Time Controller 2</b></input>
  </span>
  <div id="CalendarContent">
    <span id="intervalSelection" class="timeCstar">Select an interval</span>
    <br/>
    <form id="validateForm" action="#" onsubmit="return false;">
      <p>
	<input type="text" name="popupDatepicker1" id="popupDatepicker1" class="dpDate" />
	<br/>
	<input type="submit" class="submit" id="queryPopupDatepicker" value="Query"></input>
      </p>
    </form>
    <br/>
    <span id="individualDaysSelection" class="timeCstar">Select specific days</span>
    <br/>
    <div id="inlineDatepicker"></div>
    <span>
      <span>
	<button name="button10_1" onclick="javascript:refreshCalendar();"> Refresh </button> 
      </span>
      <span>
	<button name="calendardays" onclick="javascript:selectedCalendarDays();"> Ask Calendar! </button>
      </span>
    </span>
  </div>
</div>