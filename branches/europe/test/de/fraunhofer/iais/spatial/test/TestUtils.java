package de.fraunhofer.iais.spatial.test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import org.junit.Test;

public class TestUtils {

	@Test
	public void testCalendar() throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("EEEEE", Locale.ENGLISH);
		Calendar calendar = Calendar.getInstance();

		calendar.set(Calendar.YEAR, 2009);
		calendar.set(Calendar.MONTH, 1);
		calendar.set(Calendar.DAY_OF_MONTH, 32);
		calendar.set(Calendar.HOUR_OF_DAY, 12);
		// filter out the selected weekdays
		calendar.setLenient(false);
		System.out.println(calendar.isLenient());
		if (true) {
//							queryStrs.add(y + "-" + m + "-" + d + "@" + h);
			System.out.println(calendar.getTime());
		}

	}
}
