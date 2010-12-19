package de.fraunhofer.iais.spatial.dao.mybatis;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import oracle.spatial.geometry.JGeometry;

import org.mybatis.spring.SqlSessionTemplate;

import de.fraunhofer.iais.spatial.dao.FlickrDeWestAreaDao;
import de.fraunhofer.iais.spatial.dto.FlickrDeWestAreaDto;
import de.fraunhofer.iais.spatial.dto.FlickrDeWestAreaDto.Level;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestPhoto;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea.Radius;

public class FlickrDeWestAreaDaoMybatis extends FlickrDeWestAreaDao {

//	private final static String resource = "mybatis-config.xml";
	private SqlSessionTemplate sessionTemplate;

	public void setSessionTemplate(SqlSessionTemplate sessionTemplate) {
		this.sessionTemplate = sessionTemplate;
	}

//	public FlickrDeWestAreaDaoMybatis() throws IOException {
//		if (sessionTemplate == null) {
//			Reader reader = Resources.getResourceAsReader(resource);
//			SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
//			sessionTemplate = new SqlSessionTemplate(sqlSessionFactory);
//			System.out.println("initializing session factory");
//		}
//	}

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

		Map<String, Integer> hoursCount = new TreeMap<String, Integer>();
		a.setHoursCount(hoursCount);

		String count = (String) sessionTemplate.selectOne(FlickrDeWestArea.class.getName() + ".hourCount", a);
		if (count != null) {
			parseCounts(count, hoursCount, hourRegExPattern);
		}
	}

	private void loadDaysCount(FlickrDeWestArea a) {
		if (a.getDaysCount() != null)
			return; // cached

		Map<String, Integer> daysCount = new TreeMap<String, Integer>();
		a.setDaysCount(daysCount);

		String count = (String) sessionTemplate.selectOne(FlickrDeWestArea.class.getName() + ".dayCount", a);
		if (count != null) {
			parseCounts(count, daysCount, dayRegExPattern);
		}
	}

	private void loadMonthsCount(FlickrDeWestArea a) {
		if (a.getMonthsCount() != null)
			return; // cached

		Map<String, Integer> monthsCount = new TreeMap<String, Integer>();
		a.setMonthsCount(monthsCount);

		String count = (String) sessionTemplate.selectOne(FlickrDeWestArea.class.getName() + ".monthCount", a);
		if (count != null) {
			parseCounts(count, monthsCount, monthRegExPattern);
		}
	}

	private void loadYearsCount(FlickrDeWestArea a) {
		if (a.getYearsCount() != null)
			return; // cached

		Map<String, Integer> yearsCount = new TreeMap<String, Integer>();
		a.setYearsCount(yearsCount);

		String count = (String) sessionTemplate.selectOne(FlickrDeWestArea.class.getName() + ".yearCount", a);
		if (count != null) {
			parseCounts(count, yearsCount, yearRegExPattern);
		}
	}

	@Override
	public List<FlickrDeWestArea> getAllAreas(Radius radius) {
		FlickrDeWestAreaDto areaDto = new FlickrDeWestAreaDto();
		areaDto.setRadius(radius);

		List<FlickrDeWestArea> as = (List<FlickrDeWestArea>) sessionTemplate.selectList(FlickrDeWestArea.class.getName() + ".selectAll", areaDto);
		initAreas(as, radius);

		return as;
	}

	@Override
	public FlickrDeWestArea getAreaById(int areaid, Radius radius) {
		FlickrDeWestAreaDto areaDto = new FlickrDeWestAreaDto();
		areaDto.setAreaid(areaid);
		areaDto.setRadius(radius);

		FlickrDeWestArea a = (FlickrDeWestArea) sessionTemplate.selectOne(FlickrDeWestArea.class.getName() + ".selectById", areaDto);
		initArea(a, radius);

		return a;
	}

	@Override
	public List<FlickrDeWestArea> getAreasByPoint(double x, double y, Radius radius) {
		JGeometry queryGeom = new JGeometry(2001, 8307, x, y, 0, null, null);
		FlickrDeWestAreaDto areaDto = new FlickrDeWestAreaDto();
		areaDto.setRadius(radius);
		areaDto.setQueryGeom(queryGeom);

		List<FlickrDeWestArea> as = (List<FlickrDeWestArea>) sessionTemplate.selectList(FlickrDeWestArea.class.getName() + ".selectByIdGeom", areaDto);
		initAreas(as, radius);

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

		List<FlickrDeWestArea> as = (List<FlickrDeWestArea>) sessionTemplate.selectList(FlickrDeWestArea.class.getName() + ".selectByIdGeom", areaDto);
		initAreas(as, radius);

		return as;
	}

	@Override
	public List<FlickrDeWestArea> getAreasByPolygon(List<Point2D> polygon, Radius radius) {

		int[] elemInfo = { 1, 1003, 1 };
		double[] ordinates = new double[polygon.size() * 2];

		int i = 0;
		for (Point2D p : polygon) {
			ordinates[i++] = p.getX();
			ordinates[i++] = p.getY();
		}

		JGeometry queryGeom = new JGeometry(2003, 8307, elemInfo, ordinates);
		FlickrDeWestAreaDto areaDto = new FlickrDeWestAreaDto();
		areaDto.setRadius(radius);
		areaDto.setQueryGeom(queryGeom);

		List<FlickrDeWestArea> as = (List<FlickrDeWestArea>) sessionTemplate.selectList(FlickrDeWestArea.class.getName() + ".selectByIdGeom", areaDto);
		initAreas(as, radius);

		return as;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected List<FlickrDeWestPhoto> getPhotos(FlickrDeWestArea area, String queryStr, int num) {

		Level queryLevel = FlickrDeWestAreaDao.judgeQueryLevel(queryStr);
		String oracleDatePatternStr = FlickrDeWestAreaDao.judgeOracleDatePatternStr(queryLevel);
		Map parameters = new HashMap();
		parameters.put("areaid", area.getId());
		parameters.put("radius", area.getRadius());
		parameters.put("queryLevel", queryLevel);
		parameters.put("queryStr", queryStr);
		parameters.put("oracleDatePatternStr", oracleDatePatternStr);
		parameters.put("num", num);

		List<FlickrDeWestPhoto> photos = (List<FlickrDeWestPhoto>) sessionTemplate.selectList(FlickrDeWestPhoto.class.getName() + ".selectByDateQuery", parameters);
		this.initPhotos(photos, area);

		return photos;
	}

	@Override
	public int getTotalCount(int areaid, Radius radius) {
		FlickrDeWestAreaDto areaDto = new FlickrDeWestAreaDto();
		areaDto.setAreaid(areaid);
		areaDto.setRadius(radius);

		Object numObj = sessionTemplate.selectOne(FlickrDeWestArea.class.getName() + ".totalCount", areaDto);
		int num = 0;
		if(numObj != null){
			num = (Integer)numObj;
		}
		return num;
	}

}