package de.fraunhofer.iais.spatial.dao.mybatis;

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.spatial.geometry.JGeometry;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import de.fraunhofer.iais.spatial.dao.AreaDao;
import de.fraunhofer.iais.spatial.entity.Area;

public class AreaDaoMybatis implements AreaDao {

	private final static String resource = "ibatis-config.xml";
	private static SqlSessionFactory sqlSessionFactory = null;

	public AreaDaoMybatis() {
		if (sqlSessionFactory == null) {
			try {
				Reader reader = Resources.getResourceAsReader(resource);
				sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
				System.out.println("initializing session factory");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/* (non-Javadoc)
	 * @see de.fraunhofer.iais.spatial.dao.jdbc.AreaDao#getAllAreas()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<Area> getAllAreas() {
		SqlSession session = sqlSessionFactory.openSession();
		List<Area> as = null;
		try {
			as = session.selectList(Area.class.getName() + ".selectAll");
			initAreas(as);
		} finally {
			session.close();
		}
		return as;
	}

	/* (non-Javadoc)
	 * @see de.fraunhofer.iais.spatial.dao.jdbc.AreaDao#getAreaById(String)
	 */
	@Override
	public Area getAreaById(int areaid) {
		SqlSession session = sqlSessionFactory.openSession();
		Area a = null;
		try {
			a = (Area) session.selectOne(Area.class.getName() + ".selectById", areaid);
			initArea(a);
		} finally {
			session.close();
		}
		return a;
	}

	/* (non-Javadoc)
	 * @see de.fraunhofer.iais.spatial.dao.jdbc.AreaDao#getAreasByPoint(double, double)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<Area> getAreasByPoint(double x, double y) {
		SqlSession session = sqlSessionFactory.openSession();
		JGeometry j_geom = new JGeometry(2001, 8307, x, y, 0, null, null);
		List<Area> as = null;
		try {
			as = session.selectList(Area.class.getName() + ".select", j_geom);
			initAreas(as);
		} finally {
			session.close();
			System.out.println("close");
		}
		return as;
	}

	/* (non-Javadoc)
	 * @see de.fraunhofer.iais.spatial.dao.jdbc.AreaDao#getAreasByRect(double, double， double, double)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<Area> getAreasByRect(double x1, double y1, double x2, double y2) {
		SqlSession session = sqlSessionFactory.openSession();
		int[] elemInfo = { 1, 1003, 3 };
		double[] ordinates = { x1, y1, x2, y2 };
		JGeometry j_geom = new JGeometry(2003, 8307, elemInfo, ordinates);
		List<Area> as = null;
		try {
			as = session.selectList(Area.class.getName() + ".select", j_geom);
			initAreas(as);
		} finally {
			session.close();
		}
		return as;
	}

	/* (non-Javadoc)
	 * @see de.fraunhofer.iais.spatial.dao.jdbc.AreaDao#getPersonCount(String, String)
	 */
	@Override
	public int getPersonCount(int areaid, String person) {
		int num = 0;

		SqlSession session = sqlSessionFactory.openSession();
		try {
			String count = (String) session.selectOne(Area.class.getName() + ".personCount", areaid);
			if (count != null) {
				Pattern p = Pattern.compile(person + ":(\\d{1,});");
				Matcher m = p.matcher(count);
				if (m.find()) {
					num += Integer.parseInt(m.group(1));
				}
			}
		} finally {
			session.close();
		}
		return num;
	}

	/* (non-Javadoc)
	 * @see de.fraunhofer.iais.spatial.dao.jdbc.AreaDao#getTotalCount(String)
	 */
	@Override
	public int getTotalCount(int areaid) {
		int num = 0;

		SqlSession session = sqlSessionFactory.openSession();
		try {
			num = (Integer) session.selectOne(Area.class.getName() + ".totalCount", areaid);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			session.close();
		}
		return num;
	}

	private void initArea(Area a) {
		if (a != null) {
			a.setSelectCount(0);
			a.setTotalCount(getTotalCount(a.getId()));
			loadYearsCount(a);
			loadMonthsCount(a);
			loadDaysCount(a);
			loadHoursCount(a);
		}
	}

	private void initAreas(List<Area> as) {
		for (Area a : as) {
			initArea(a);
		}
	}

	private void loadDaysCount(Area a) {
		if (a.getDaysCount() != null)
			return; // cached

		Map<String, Integer> daysCount = new LinkedHashMap<String, Integer>();
		a.setDaysCount(daysCount);

		SqlSession session = sqlSessionFactory.openSession();
		try {
			String count = (String) session.selectOne(Area.class.getName() + ".dayCount", a.getId());
			if (count != null) {
				Pattern p = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}):(\\d{1,});");
				Matcher m = p.matcher(count);
				while (m.find()) {
					daysCount.put(m.group(1), Integer.parseInt(m.group(2)));
				}
			}
		} finally {
			session.close();
		}
	}

	private void loadHoursCount(Area a) {
		if (a.getHoursCount() != null)
			return; // cached

		Map<String, Integer> hoursCount = new LinkedHashMap<String, Integer>();
		a.setHoursCount(hoursCount);

		SqlSession session = sqlSessionFactory.openSession();
		try {
			String count = (String) session.selectOne(Area.class.getName() + ".hourCount", a.getId());
			if (count != null) {
				Pattern p = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}@\\d{2}):(\\d{1,});");
				Matcher m = p.matcher(count);
				while (m.find()) {
					hoursCount.put(m.group(1), Integer.parseInt(m.group(2)));
				}
			}
		} finally {
			session.close();
		}
	}

	private void loadMonthsCount(Area a) {
		if (a.getMonthsCount() != null)
			return; // cached

		Map<String, Integer> monthsCount = new LinkedHashMap<String, Integer>();
		a.setMonthsCount(monthsCount);

		SqlSession session = sqlSessionFactory.openSession();
		try {
			String count = (String) session.selectOne(Area.class.getName() + ".monthCount", a.getId());
			if (count != null) {
				Pattern p = Pattern.compile("(\\d{4}-\\d{2}):(\\d{1,});");
				Matcher m = p.matcher(count);
				while (m.find()) {
					monthsCount.put(m.group(1), Integer.parseInt(m.group(2)));
				}
			}
		} finally {
			session.close();
		}
	}

	private void loadYearsCount(Area a) {
		if (a.getYearsCount() != null)
			return; // cached

		Map<String, Integer> yearsCount = new LinkedHashMap<String, Integer>();
		a.setYearsCount(yearsCount);

		SqlSession session = sqlSessionFactory.openSession();
		try {
			String count = (String) session.selectOne(Area.class.getName() + ".yearCount", a.getId());
			if (count != null) {
				Pattern p = Pattern.compile("(\\d{4}):(\\d{1,});");
				Matcher m = p.matcher(count);
				while (m.find()) {
					yearsCount.put(m.group(1), Integer.parseInt(m.group(2)));
				}
			}
		} finally {
			session.close();
		}
	}

}
