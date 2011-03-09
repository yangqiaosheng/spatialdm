package de.fraunhofer.iais.spatial.dao.mybatis;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oracle.spatial.geometry.JGeometry;

import org.apache.commons.collections.MapUtils;
import org.mybatis.spring.SqlSessionTemplate;

import de.fraunhofer.iais.spatial.dao.FlickrEuropeAreaDao;
import de.fraunhofer.iais.spatial.dto.FlickrEuropeAreaDto;
import de.fraunhofer.iais.spatial.dto.FlickrEuropeAreaDto.Level;
import de.fraunhofer.iais.spatial.entity.FlickrArea;
import de.fraunhofer.iais.spatial.entity.FlickrPhoto;
import de.fraunhofer.iais.spatial.entity.FlickrArea.Radius;

public class FlickrEuropeAreaDaoOracleMybatis extends FlickrEuropeAreaDao {

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

	private void initArea(FlickrArea a, Radius radius) {
		if (a != null) {
			a.setRadius(radius);
			a.setSelectedCount(0);
			a.setTotalCount(getTotalCount(a.getId(), a.getRadius()));
			loadYearsCount(a);
			loadMonthsCount(a);
			loadDaysCount(a);
			loadHoursCount(a);
		}
	}

	private void initAreas(List<FlickrArea> as, Radius radius) {
		for (FlickrArea a : as) {
			initArea(a, radius);
		}
	}

	private void initPhotos(List<FlickrPhoto> photos, FlickrArea area) {
		for (FlickrPhoto photo : photos) {
			photo.setArea(area);
		}
	}

	private void loadHoursCount(FlickrArea area) {
		if (MapUtils.isNotEmpty(area.getHoursCount()))
			return; // cached

		String count = (String) sessionTemplate.selectOne(FlickrArea.class.getName() + ".hourCount", area);
		if (count != null) {
			parseCounts(count, area.getHoursCount(), hourRegExPattern);
		}
	}

	private void loadDaysCount(FlickrArea area) {
		if (MapUtils.isNotEmpty(area.getDaysCount()))
			return; // cached

		String count = (String) sessionTemplate.selectOne(FlickrArea.class.getName() + ".dayCount", area);
		if (count != null) {
			parseCounts(count, area.getDaysCount(), dayRegExPattern);
		}
	}

	private void loadMonthsCount(FlickrArea area) {
		if (MapUtils.isNotEmpty(area.getMonthsCount()))
			return; // cached

		String count = (String) sessionTemplate.selectOne(FlickrArea.class.getName() + ".monthCount", area);
		if (count != null) {
			parseCounts(count, area.getMonthsCount(), monthRegExPattern);
		}
	}

	private void loadYearsCount(FlickrArea area) {
		if (MapUtils.isNotEmpty(area.getYearsCount()))
			return; // cached

		String count = (String) sessionTemplate.selectOne(FlickrArea.class.getName() + ".yearCount", area);
		if (count != null) {
			parseCounts(count, area.getYearsCount(), yearRegExPattern);
		}
	}

	@Override
	public List<FlickrArea> getAllAreas(Radius radius) {
		FlickrEuropeAreaDto areaDto = new FlickrEuropeAreaDto();
		areaDto.setRadius(radius);

		List<FlickrArea> as = (List<FlickrArea>) sessionTemplate.selectList(FlickrArea.class.getName() + ".selectAll", areaDto);
		initAreas(as, radius);

		return as;
	}

	@Override
	public FlickrArea getAreaById(int areaid, Radius radius) {
		FlickrEuropeAreaDto areaDto = new FlickrEuropeAreaDto();
		areaDto.setAreaid(areaid);
		areaDto.setRadius(radius);

		FlickrArea a = (FlickrArea) sessionTemplate.selectOne(FlickrArea.class.getName() + ".selectById", areaDto);
		initArea(a, radius);

		return a;
	}

	@Override
	public List<FlickrArea> getAreasByPoint(double x, double y, Radius radius) {
		JGeometry queryGeom = new JGeometry(2001, 8307, x, y, 0, null, null);
		FlickrEuropeAreaDto areaDto = new FlickrEuropeAreaDto();
		areaDto.setRadius(radius);
		areaDto.setQueryGeom(queryGeom);

		List<FlickrArea> as = (List<FlickrArea>) sessionTemplate.selectList(FlickrArea.class.getName() + ".selectByIdGeom", areaDto);
		initAreas(as, radius);

		return as;
	}

	@Override
	public int getAreasByRectSize(double x1, double y1, double x2, double y2, Radius radius) {
		int[] elemInfo = { 1, 1003, 3 };
		double[] ordinates = { x1, y1, x2, y2 };
		JGeometry queryGeom = new JGeometry(2003, 8307, elemInfo, ordinates);
		FlickrEuropeAreaDto areaDto = new FlickrEuropeAreaDto();
		areaDto.setRadius(radius);
		areaDto.setQueryGeom(queryGeom);

		Object numObj = sessionTemplate.selectOne(FlickrArea.class.getName() + ".selectByIdGeomSize", areaDto);
		int num = 0;
		if(numObj != null){
			num = (Integer)numObj;
		}
		return num;
	}

	@Override
	public List<FlickrArea> getAreasByRect(double x1, double y1, double x2, double y2, Radius radius) {

		int[] elemInfo = { 1, 1003, 3 };
		double[] ordinates = { x1, y1, x2, y2 };
		JGeometry queryGeom = new JGeometry(2003, 8307, elemInfo, ordinates);
		FlickrEuropeAreaDto areaDto = new FlickrEuropeAreaDto();
		areaDto.setRadius(radius);
		areaDto.setQueryGeom(queryGeom);

		List<FlickrArea> as = (List<FlickrArea>) sessionTemplate.selectList(FlickrArea.class.getName() + ".selectByIdGeom", areaDto);
		initAreas(as, radius);

		return as;
	}

	@Override
	public List<FlickrArea> getAreasByPolygon(List<Point2D> polygon, Radius radius) {

		int[] elemInfo = { 1, 1003, 1 };
		double[] ordinates = new double[polygon.size() * 2];

		int i = 0;
		for (Point2D p : polygon) {
			ordinates[i++] = p.getX();
			ordinates[i++] = p.getY();
		}

		JGeometry queryGeom = new JGeometry(2003, 8307, elemInfo, ordinates);
		FlickrEuropeAreaDto areaDto = new FlickrEuropeAreaDto();
		areaDto.setRadius(radius);
		areaDto.setQueryGeom(queryGeom);

		List<FlickrArea> as = (List<FlickrArea>) sessionTemplate.selectList(FlickrArea.class.getName() + ".selectByIdGeom", areaDto);
		initAreas(as, radius);

		return as;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected List<FlickrPhoto> getPhotos(FlickrArea area, String queryStr, int num) {

		Level queryLevel = FlickrEuropeAreaDao.judgeQueryLevel(queryStr);
		String oracleDatePatternStr = FlickrEuropeAreaDao.judgeOracleDatePatternStr(queryLevel);
		Map parameters = new HashMap();
		parameters.put("areaid", area.getId());
		parameters.put("radius", area.getRadius());
		parameters.put("queryLevel", queryLevel);
		parameters.put("queryStr", queryStr);
		parameters.put("oracleDatePatternStr", oracleDatePatternStr);
		parameters.put("num", num);

		List<FlickrPhoto> photos = (List<FlickrPhoto>) sessionTemplate.selectList(FlickrPhoto.class.getName() + ".selectByDateQuery", parameters);
		this.initPhotos(photos, area);

		return photos;
	}

	@Override
	public int getTotalCount(int areaid, Radius radius) {
		FlickrEuropeAreaDto areaDto = new FlickrEuropeAreaDto();
		areaDto.setAreaid(areaid);
		areaDto.setRadius(radius);

		Object numObj = sessionTemplate.selectOne(FlickrArea.class.getName() + ".totalCount", areaDto);
		int num = 0;
		if(numObj != null){
			num = (Integer)numObj;
		}
		return num;
	}

}
