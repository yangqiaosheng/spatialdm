package de.fraunhofer.iais.spatial.dao.mybatis;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.spatial.geometry.JGeometry;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import de.fraunhofer.iais.spatial.dao.FlickrDeWestAreaDao;
import de.fraunhofer.iais.spatial.dto.FlickrDeWestAreaDto;
import de.fraunhofer.iais.spatial.dto.FlickrDeWestAreaDto.QueryLevel;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestPhoto;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea.Radius;

public class FlickrDeWestAreaDaoMybatis extends FlickrDeWestAreaDao {

	private final static String resource = "ibatis-config.xml";
	private static SqlSessionFactory sqlSessionFactory = null;

	public FlickrDeWestAreaDaoMybatis() {
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


	private void initArea(FlickrDeWestArea a, Radius radius) {
		if (a != null) {
			a.setRadius(radius);
			a.setSelectCount(0);
			a.setTotalCount(getTotalCount(a.getId(), a.getRadius()));
			loadYearsCount(a);
			loadMonthsCount(a);
			loadDaysCount(a);
			loadHoursCount(a);
		}
	}

	private void initAreas(List<FlickrDeWestArea> as, Radius radius) {
		for (FlickrDeWestArea a : as) {
			initArea(a, radius);
		}
	}

	private void initPhotos(List<FlickrDeWestPhoto> photos, FlickrDeWestArea area) {
		for (FlickrDeWestPhoto photo : photos) {
			photo.setArea(area);
		}
	}

	private void loadHoursCount(FlickrDeWestArea a) {
		if (a.getHoursCount() != null)
			return; // cached

		Map<String, Integer> hoursCount = new LinkedHashMap<String, Integer>();
		a.setHoursCount(hoursCount);

		SqlSession session = sqlSessionFactory.openSession();
		try {
			String count = (String) session.selectOne(FlickrDeWestArea.class.getName() + ".hourCount", a);
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

	private void loadDaysCount(FlickrDeWestArea a) {
		if (a.getDaysCount() != null)
			return; // cached

		Map<String, Integer> daysCount = new LinkedHashMap<String, Integer>();
		a.setDaysCount(daysCount);

		SqlSession session = sqlSessionFactory.openSession();
		try {
			String count = (String) session.selectOne(FlickrDeWestArea.class.getName() + ".dayCount", a);
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


	private void loadMonthsCount(FlickrDeWestArea a) {
		if (a.getMonthsCount() != null)
			return; // cached

		Map<String, Integer> monthsCount = new LinkedHashMap<String, Integer>();
		a.setMonthsCount(monthsCount);

		SqlSession session = sqlSessionFactory.openSession();
		try {
			String count = (String) session.selectOne(FlickrDeWestArea.class.getName() + ".monthCount", a);
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

	private void loadYearsCount(FlickrDeWestArea a) {
		if (a.getYearsCount() != null)
			return; // cached

		Map<String, Integer> yearsCount = new LinkedHashMap<String, Integer>();
		a.setYearsCount(yearsCount);

		SqlSession session = sqlSessionFactory.openSession();
		try {
			String count = (String) session.selectOne(FlickrDeWestArea.class.getName() + ".yearCount", a);
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

	@Override
	public List<FlickrDeWestArea> getAllAreas(Radius radius) {
		FlickrDeWestAreaDto areaDto = new FlickrDeWestAreaDto();
		areaDto.setRadius(radius);

		SqlSession session = sqlSessionFactory.openSession();
		List<FlickrDeWestArea> as = null;
		try {
			as = session.selectList(FlickrDeWestArea.class.getName() + ".selectAll", areaDto);
			initAreas(as, radius);
		} finally {
			session.close();
		}
		return as;
	}

	@Override
	public FlickrDeWestArea getAreaById(int areaid, Radius radius) {
		FlickrDeWestAreaDto areaDto = new FlickrDeWestAreaDto();
		areaDto.setAreaid(areaid);
		areaDto.setRadius(radius);

		SqlSession session = sqlSessionFactory.openSession();
		FlickrDeWestArea a = null;
		try {
			a = (FlickrDeWestArea) session.selectOne(FlickrDeWestArea.class.getName() + ".selectById", areaDto);
			initArea(a, radius);
		} finally {
			session.close();
		}
		return a;
	}

	@Override
	public List<FlickrDeWestArea> getAreasByPoint(double x, double y, Radius radius) {
		JGeometry queryGeom = new JGeometry(2001, 8307, x, y, 0, null, null);
		FlickrDeWestAreaDto areaDto = new FlickrDeWestAreaDto();
		areaDto.setRadius(radius);
		areaDto.setQueryGeom(queryGeom);

		List<FlickrDeWestArea> as = null;
		SqlSession session = sqlSessionFactory.openSession();
		try {
			as = session.selectList(FlickrDeWestArea.class.getName() + ".selectByIdGeom", areaDto);
			initAreas(as, radius);
		} finally {
			session.close();
			System.out.println("close");
		}
		return as;
	}

	@Override
	public List<FlickrDeWestArea> getAreasByRect(double x1, double y1, double x2, double y2, Radius radius) {

		int[] elemInfo = { 1, 1003, 3 };
		double[] ordinates = { x1, y1, x2, y2 };
		JGeometry queryGeom = new JGeometry(2003, 8307, elemInfo, ordinates);
		FlickrDeWestAreaDto areaDto = new FlickrDeWestAreaDto();
		areaDto.setRadius(radius);
		areaDto.setQueryGeom(queryGeom);

		SqlSession session = sqlSessionFactory.openSession();
		List<FlickrDeWestArea> as = null;
		try {
			as = session.selectList(FlickrDeWestArea.class.getName() + ".selectByIdGeom", areaDto);
			initAreas(as, radius);
		} finally {
			session.close();
		}
		return as;
	}


	@Override
	public List<FlickrDeWestArea> getAreasByPolygon(List<Point2D> polygon, Radius radius) {

		int[] elemInfo = { 1, 1003, 1 };
		double[] ordinates = new double[polygon.size() * 2];

		int i = 0;
		for(Point2D p : polygon){
			ordinates[i++] = p.getX();
			ordinates[i++] = p.getY();
		}

		JGeometry queryGeom = new JGeometry(2003, 8307, elemInfo, ordinates);
		FlickrDeWestAreaDto areaDto = new FlickrDeWestAreaDto();
		areaDto.setRadius(radius);
		areaDto.setQueryGeom(queryGeom);

		SqlSession session = sqlSessionFactory.openSession();
		List<FlickrDeWestArea> as = null;
		try {
			as = session.selectList(FlickrDeWestArea.class.getName() + ".selectByIdGeom", areaDto);
			initAreas(as, radius);
		} finally {
			session.close();
		}
		return as;
	}

	@Override
	public List<FlickrDeWestPhoto> getPhotos(int areaid, Radius radius, SortedSet<String> queryStrs, int num) {
		List<FlickrDeWestPhoto> photos = new ArrayList<FlickrDeWestPhoto>();
		FlickrDeWestArea area = this.getAreaById(areaid, radius);
		QueryLevel queryLevel = FlickrDeWestAreaDao.judgeQueryLevel(queryStrs.first());
		Map<String, Integer> count = null;

		switch (queryLevel) {
		case YEAR:
			count = area.getYearsCount();
			break;
		case MONTH:
			count = area.getMonthsCount();
			break;
		case DAY:
			count = area.getDaysCount();
			break;
		case HOUR:
			count = area.getHoursCount();
			break;
		}

		List<String> tempQueryStrs = new ArrayList<String>(queryStrs);
		for (int i = tempQueryStrs.size() - 1; photos.size() < num && i >= 0; i--) {
			if (count.get(tempQueryStrs.get(i)) != null && count.get(tempQueryStrs.get(i)) > 0) {
				photos.addAll(this.getPhotos(area, tempQueryStrs.get(i), num - photos.size()));

			}
		}
		return photos;
	}


	protected List<FlickrDeWestPhoto> getPhotos(FlickrDeWestArea area, String queryStr, int num) {

		QueryLevel queryLevel = FlickrDeWestAreaDao.judgeQueryLevel(queryStr);
		String oracleDatePatternStr = FlickrDeWestAreaDao.judgeOracleDatePatternStr(queryLevel);
		String oracleToDateStr = "to_date('" + queryStr + "', '" + oracleDatePatternStr + "')";
		Map parameters = new HashMap();
		parameters.put("areaid", area.getId());
		parameters.put("radius", area.getRadius());
		parameters.put("queryLevel", queryLevel);
		parameters.put("queryStr", queryStr);
		parameters.put("oracleDatePatternStr", oracleDatePatternStr);
		parameters.put("num", num);

		List<FlickrDeWestPhoto> photos = null;

		SqlSession session = sqlSessionFactory.openSession();
		try {
			photos = session.selectList(FlickrDeWestPhoto.class.getName() + ".selectByDateQuery", parameters);
			this.initPhotos(photos, area);
		} finally {
			session.close();
		}
		return photos;
	}

	@Override
	public int getTotalCount(int areaid, Radius radius) {
		FlickrDeWestAreaDto areaDto = new FlickrDeWestAreaDto();
		areaDto.setAreaid(areaid);
		areaDto.setRadius(radius);

		int num = 0;

		SqlSession session = sqlSessionFactory.openSession();
		try {
			num = (Integer) session.selectOne(FlickrDeWestArea.class.getName() + ".totalCount", areaDto);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			session.close();
		}
		return num;
	}

}
