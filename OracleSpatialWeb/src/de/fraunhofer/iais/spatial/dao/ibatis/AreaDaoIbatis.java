package de.fraunhofer.iais.spatial.dao.ibatis;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.spatial.geometry.JGeometry;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import de.fraunhofer.iais.spatial.dao.AreaDao;
import de.fraunhofer.iais.spatial.entity.Area;

public class AreaDaoIbatis implements AreaDao {

	private final static String resource = "ibatis-config.xml";
	private static SqlSessionFactory sqlSessionFactory = null;

	public AreaDaoIbatis() {
		if (sqlSessionFactory == null)
			try {
				Reader reader = Resources.getResourceAsReader(resource);
				sqlSessionFactory = new SqlSessionFactoryBuilder()
						.build(reader);
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Area> getAllAreas() {
		SqlSession session = sqlSessionFactory.openSession();
		List<Area> as = null;
		try {
			as = (List<Area>) session.selectList(Area.class.getName()
					+ ".selectAll");
		} finally {
			session.close();
		}
		return as;
	}

	@Override
	public Area getAreaById(String id) {
		SqlSession session = sqlSessionFactory.openSession();
		Area a = null;
		try {
			a = (Area) session.selectOne(Area.class.getName() + ".selectById",
					id);
		} finally {
			session.close();
		}
		return a;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Area> getAreasByPoint(double x, double y) {
		SqlSession session = sqlSessionFactory.openSession();
		JGeometry j_geom = new JGeometry(2001, 8307, x, y, 0, null, null);
		List<Area> as = null;
		try {
			as = (List<Area>) session.selectList(Area.class.getName()
					+ ".select", j_geom);
		} finally {
			session.close();
			System.out.println("close");
		}
		return as;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Area> getAreasByRect(double x1, double y1, double x2, double y2) {
		SqlSession session = sqlSessionFactory.openSession();
		int[] elemInfo = { 1, 1003, 3 };
		double[] ordinates = { x1, y1, x2, y2 };
		JGeometry j_geom = new JGeometry(2003, 8307, elemInfo, ordinates);
		List<Area> as = null;
		try {
			as = (List<Area>) session.selectList(Area.class.getName()
					+ ".select", j_geom);
		} finally {
			session.close();
		}
		return as;
	}

	@Override
	public int getPersonCount(String areaid, String person) {
		int num = 0;

		SqlSession session = sqlSessionFactory.openSession();
		try {
			String count = (String) session.selectOne(Area.class.getName()
					+ ".personCount", areaid);
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

	@Override
	public int getHourCount(String areaid, String hour) {
		int num = 0;

		SqlSession session = sqlSessionFactory.openSession();
		try {
			String count = (String) session.selectOne(Area.class.getName()
					+ ".hourCount", areaid);
			if (count != null) {
				Pattern p = Pattern.compile(hour + ":(\\d{1,});");
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

	@Override
	public int getHourCount(String areaid, Set<String> hours) {
		int num = 0;
		SqlSession session = sqlSessionFactory.openSession();
		try {
			String count = (String) session.selectOne(Area.class.getName()
					+ ".hourCount", areaid);
			if (count != null) {
				Pattern p = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}@\\d{2}):(\\d{1,});");
				Matcher m = p.matcher(count);
				while (m.find()) {
					if (hours.contains(m.group(1))) {
						num += Integer.parseInt(m.group(2));
					}
				}
			}
		} finally {
			session.close();
		}
		return num;
	}

	@Override
	public int getDayCount(String areaid, String day) {
		int num = 0;

		SqlSession session = sqlSessionFactory.openSession();
		try {
			String count = (String) session.selectOne(Area.class.getName()
					+ ".dayCount", areaid);
			if (count != null) {
				Pattern p = Pattern.compile(day + ":(\\d{1,});");
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

	@Override
	public int getDayCount(String areaid, Set<String> days) {
		int num = 0;
		SqlSession session = sqlSessionFactory.openSession();
		try {
			String count = (String) session.selectOne(Area.class.getName()
					+ ".dayCount", areaid);
			if (count != null) {
				Pattern p = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}):(\\d{1,});");
				Matcher m = p.matcher(count);
				while (m.find()) {
					if (days.contains(m.group(1))) {
						num += Integer.parseInt(m.group(2));
					}
				}
			}
		} finally {
			session.close();
		}
		return num;
	}

	@Override
	public int getMonthCount(String areaid, String month) {
		int num = 0;
		SqlSession session = sqlSessionFactory.openSession();
		try {
			String count = (String) session.selectOne(Area.class.getName()
					+ ".monthCount", areaid);
			if (count != null) {
				Pattern p = Pattern.compile(month + ":(\\d{1,});");
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

	@Override
	public int getMonthCount(String areaid, Set<String> months) {
		int num = 0;
		SqlSession session = sqlSessionFactory.openSession();
		try {
			String count = (String) session.selectOne(Area.class.getName()
					+ ".monthCount", areaid);
			if (count != null) {
				Pattern p = Pattern.compile("(\\d{4}-\\d{2}):(\\d{1,});");
				Matcher m = p.matcher(count);
				while (m.find()) {
					if (months.contains(m.group(1))) {
						num += Integer.parseInt(m.group(2));
					}
				}
			}
		} finally {
			session.close();
		}
		return num;
	}

	@Override
	public int getYearCount(String areaid, String year) {
		int num = 0;

		SqlSession session = sqlSessionFactory.openSession();
		try {
			String count = (String) session.selectOne(Area.class.getName()
					+ ".yearCount", areaid);
			if (count != null) {
				Pattern p = Pattern.compile(year + ":(\\d{1,});");
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

	@Override
	public int getYearCount(String areaid, Set<String> years) {
		int num = 0;
		SqlSession session = sqlSessionFactory.openSession();
		try {
			String count = (String) session.selectOne(Area.class.getName()
					+ ".yearCount", areaid);
			if (count != null) {
				Pattern p = Pattern.compile("(\\d{4}):(\\d{1,});");
				Matcher m = p.matcher(count);
				while (m.find()) {
					if (years.contains(m.group(1))) {
						num += Integer.parseInt(m.group(2));
					}
				}
			}
		} finally {
			session.close();
		}
		return num;
	}

	@Override
	public int getTotalCount(String areaid) {
		int num = 0;

		SqlSession session = sqlSessionFactory.openSession();
		try {
			num = (Integer) session.selectOne(Area.class.getName()
					+ ".totalCount", areaid);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			session.close();
		}
		return num;
	}

}