package spade.time;


public class Res extends java.util.ListResourceBundle {
	static final Object[][] contents = new String[][] { { "sec_", "sec." }, { "min_", "min." }, { "second", "second" }, { "minute", "minute" }, { "hour", "hour" }, { "day", "day" }, { "month", "month" }, { "year", "year" }, { "seconds", "seconds" },
			{ "minutes", "minutes" }, { "hours", "hours" }, { "days", "days" }, { "months", "months" }, { "years", "years" }, { "no_time_symbols", "There are no time symbols" }, { "in_template", "in the template" }, { "Symbols", "Symbols" },
			{ "must_come_in_sequence", "must come in uninterrupted sequence" }, { "and", "and" }, { "but_no", "but no" }, { "found", "found" }, { "null_template", "Null template!" }, { "empty_template", "Empty template!" } };

	@Override
	public Object[][] getContents() {
		return contents;
	}
}