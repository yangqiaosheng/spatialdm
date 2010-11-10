package de.fraunhofer.iais.spatial.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import de.fraunhofer.iais.spatial.dao.AreaDao;
import de.fraunhofer.iais.spatial.dao.FlickrDeWestAreaDao;
import de.fraunhofer.iais.spatial.entity.Area;
import de.fraunhofer.iais.spatial.service.AreaMgr;
import de.fraunhofer.iais.spatial.util.StringUtil;
import de.fraunhofer.iais.spatial.web.servlet.RequestKmlServlet;

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
