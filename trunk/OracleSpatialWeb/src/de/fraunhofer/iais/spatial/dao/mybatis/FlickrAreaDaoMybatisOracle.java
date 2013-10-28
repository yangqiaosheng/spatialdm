package de.fraunhofer.iais.spatial.dao.mybatis;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import oracle.spatial.geometry.JGeometry;

import org.jdom.Element;
import org.jdom.IllegalDataException;
import org.mybatis.spring.SqlSessionTemplate;

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

import de.fraunhofer.iais.spatial.dao.FlickrAreaDao;
import de.fraunhofer.iais.spatial.dto.FlickrAreaDto;
import de.fraunhofer.iais.spatial.dto.FlickrAreaDto.Level;
import de.fraunhofer.iais.spatial.entity.FlickrArea;
import de.fraunhofer.iais.spatial.entity.FlickrPhoto;
import de.fraunhofer.iais.spatial.entity.FlickrArea.Radius;

public class FlickrAreaDaoMybatisOracle extends FlickrAreaDao {

	Pattern hourRegExPattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}@\\d{2}):(\\d+);");
	Pattern dayRegExPattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}):(\\d+);");
	Pattern monthRegExPattern = Pattern.compile("(\\d{4}-\\d{2})(\\d+);");
	Pattern yearRegExPattern = Pattern.compile("(\\d{4}):(\\d+);");

//	private final static String resource = "mybatis-config.xml";
	private SqlSessionTemplate sessionTemplate;

	public void setSessionTemplate(SqlSessionTemplate sessionTemplate) {
		this.sessionTemplate = sessionTemplate;
	}

	private void initArea(FlickrArea area) {

		if (area != null && area.isCached() == false) {
			//initialize the not cached object
			area.setCached(true);
			area.setTotalCount(getTotalCountWithinArea(area.getId()));
			setCenter(area);
			/*
			loadYearsCount(area);
			loadMonthsCount(area);
			loadDaysCount(area);
			*/
			loadHoursCount(area);
		}
	}

	private void setCenter(FlickrArea area) {
		GeometryFactory geometryFactory = new GeometryFactory();
		List<Coordinate> coordinates = Lists.newArrayList();

		for (Point2D point : area.getGeom()) {
			coordinates.add(new Coordinate(point.getX(), point.getY()));
		}

		LinearRing shell = geometryFactory.createLinearRing(coordinates.toArray(new Coordinate[0]));
		Polygon polygon = geometryFactory.createPolygon(shell, null);
		area.setCenter(new Point2D.Double(polygon.getCentroid().getX(), polygon.getCentroid().getY()));

	}

	private void initPhotos(List<FlickrPhoto> photos, FlickrArea area) {
		for (FlickrPhoto photo : photos) {
			photo.setArea(area);
		}
	}

	@Override
	public void loadHoursTagsCount(FlickrArea area, boolean withoutStopWords) {
		area.getHoursTagsCount().clear();
		String count = null;
		if (withoutStopWords) {
			count = (String) sessionTemplate.selectOne(this.getClass().getName() + ".Area.hoursTagsCount_WithoutStopWords", area);
		} else {
			count = (String) sessionTemplate.selectOne(this.getClass().getName() + ".Area.hoursTagsCount", area);
		}
		if (count != null) {
			parseHoursTagsCountDbString(count, area.getHoursTagsCount());
		}
	}

	private void loadHoursCount(FlickrArea area) {

		String count = (String) sessionTemplate.selectOne(this.getClass().getName() + ".Area.hoursCount", area);
		if (count != null) {
			parseCountDbString(count, area.getHoursCount(), hourRegExPattern);
		}
	}

	@Deprecated
	private void loadDaysCount(FlickrArea area) {

		String count = (String) sessionTemplate.selectOne(this.getClass().getName() + ".Area.daysCount", area);
		if (count != null) {
			parseCountDbString(count, area.getDaysCount(), dayRegExPattern);
		}
	}

	@Deprecated
	private void loadMonthsCount(FlickrArea area) {

		String count = (String) sessionTemplate.selectOne(this.getClass().getName() + ".Area.monthsCount", area);
		if (count != null) {
			parseCountDbString(count, area.getMonthsCount(), monthRegExPattern);
		}
	}

	@Deprecated
	private void loadYearsCount(FlickrArea area) {

		String count = (String) sessionTemplate.selectOne(this.getClass().getName() + ".Area.yearsCount", area);
		if (count != null) {
			parseCountDbString(count, area.getYearsCount(), yearRegExPattern);
		}
	}

	@Override
	public FlickrArea getAreaById(int areaid, Radius radius) {
		FlickrAreaDto areaDto = new FlickrAreaDto();
		areaDto.setAreaid(areaid);
		areaDto.setRadius(radius);

		FlickrArea area = (FlickrArea) sessionTemplate.selectOne(this.getClass().getName() + ".Area.selectById", areaDto);
		initArea(area);

		return area;
	}

	@Override
	public List<Integer> getAreaIdsByPoint(double x, double y, Radius radius) {
		JGeometry queryGeom = new JGeometry(2001, 8307, x, y, 0, null, null);
		FlickrAreaDto areaDto = new FlickrAreaDto();
		areaDto.setRadius(radius);
		areaDto.setOracleQueryGeom(queryGeom);

		return (List<Integer>) sessionTemplate.selectList(this.getClass().getName() + ".Area.selectIdsByGeom", areaDto);
	}

	@Override
	public List<FlickrArea> getAreasByPoint(double x, double y, Radius radius) {
		List<FlickrArea> areas = new ArrayList<FlickrArea>();
		for (int areaid : getAreaIdsByPoint(x, y, radius)) {
			areas.add(getAreaById(areaid, radius));
		}
		return areas;
	}

	@Override
	@Deprecated
	public int getAreasByRectSize(double x1, double y1, double x2, double y2, Radius radius) {
		int[] elemInfo = { 1, 1003, 3 };
		double[] ordinates = { x1, y1, x2, y2 };
		JGeometry queryGeom = new JGeometry(2003, 8307, elemInfo, ordinates);
		FlickrAreaDto areaDto = new FlickrAreaDto();
		areaDto.setRadius(radius);
		areaDto.setOracleQueryGeom(queryGeom);

		Object numObj = sessionTemplate.selectOne(this.getClass().getName() + ".Area.selectByGeomSize", areaDto);
		int num = 0;
		if (numObj != null) {
			num = (Integer) numObj;
		}
		return num;
	}

	@Override
	public int getAreasByRectSize(double x1, double y1, double x2, double y2, Radius radius, boolean crossDateLine) {
		int num = 0;
		if (crossDateLine) {
			num = getAreasByRectSize(x1, y1, 180, y2, radius) + getAreasByRectSize(-180, y1, x2, y2, radius);
		} else {
			num = getAreasByRectSize(x1, y1, x2, y2, radius);
		}
		return num;
	}

	@Override
	public List<Integer> getAllAreaIds(Radius radius) {
		FlickrAreaDto areaDto = new FlickrAreaDto();
		areaDto.setRadius(radius);
		return (List<Integer>) sessionTemplate.selectList(this.getClass().getName() + ".Area.selectAllIds", areaDto);
	}

	@Override
	@Deprecated
	public List<Integer> getAreaIdsByRect(double x1, double y1, double x2, double y2, Radius radius) {
		int[] elemInfo = { 1, 1003, 3 };
		double[] ordinates = { x1, y1, x2, y2 };
		JGeometry queryGeom = new JGeometry(2003, 8307, elemInfo, ordinates);
		FlickrAreaDto areaDto = new FlickrAreaDto();
		areaDto.setRadius(radius);
		areaDto.setOracleQueryGeom(queryGeom);

		return (List<Integer>) sessionTemplate.selectList(this.getClass().getName() + ".Area.selectIdsByGeom", areaDto);
	}

	@Override
	public List<Integer> getAreaIdsByRect(double x1, double y1, double x2, double y2, Radius radius, boolean crossDateLine) {
		List<Integer> areaIds;
		if (crossDateLine) {
			areaIds = getAreaIdsByRect(x1, y1, 180, y2, radius);
			areaIds.addAll(getAreaIdsByRect(-180, y1, x2, y2, radius));
		} else {
			areaIds = getAreaIdsByRect(x1, y1, x2, y2, radius);
		}

		return areaIds;
	}

	@Override
	public List<FlickrArea> getAreasByRect(double x1, double y1, double x2, double y2, Radius radius) {

		List<FlickrArea> areas = new ArrayList<FlickrArea>();
		for (int areaid : getAreaIdsByRect(x1, y1, x2, y2, radius)) {
			areas.add(getAreaById(areaid, radius));
		}

		return areas;
	}

	@Override
	public List<Integer> getAreaIdsByPolygon(List<Point2D> polygon, Radius radius) {
		int[] elemInfo = { 1, 1003, 1 };
		double[] ordinates = new double[polygon.size() * 2];

		int i = 0;
		for (Point2D p : polygon) {
			ordinates[i++] = p.getX();
			ordinates[i++] = p.getY();
		}

		JGeometry queryGeom = new JGeometry(2003, 8307, elemInfo, ordinates);
		FlickrAreaDto areaDto = new FlickrAreaDto();
		areaDto.setRadius(radius);
		areaDto.setOracleQueryGeom(queryGeom);

		return (List<Integer>) sessionTemplate.selectList(this.getClass().getName() + ".Area.selectIdsByGeom", areaDto);
	}

	@Override
	public List<FlickrArea> getAreasByPolygon(List<Point2D> polygon, Radius radius) {

		List<FlickrArea> areas = new ArrayList<FlickrArea>();
		for (int areaid : getAreaIdsByPolygon(polygon, radius)) {
			areas.add(getAreaById(areaid, radius));
		}

		return areas;
	}

	@Override
	public long getTotalCountWithinArea(long areaid) {
		FlickrAreaDto areaDto = new FlickrAreaDto();
		areaDto.setAreaid(areaid);

		Object numObj = sessionTemplate.selectOne(this.getClass().getName() + ".Area.totalCount", areaDto);
		long num = 0;
		if (numObj != null) {
			num = (Long) numObj;
		}
		return num;
	}

	@Override
	public long getTotalWorldPhotoNum() {
		Object numObj = sessionTemplate.selectOne(this.getClass().getName() + ".Area.totalWorldPhotoNum");
		long num = 0;
		if (numObj != null) {
			num = (Long) numObj;
		}
		return num;
	}

	@Override
	public long getTotalPeopleNum() {
		Object numObj = sessionTemplate.selectOne(this.getClass().getName() + ".Area.totalPeopleNum");
		long num = 0;
		if (numObj != null) {
			num = (Long) numObj;
		}
		return num;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected List<FlickrPhoto> getPhotos(FlickrArea area, String queryStr, int num) {

		Level queryLevel = FlickrAreaDao.judgeQueryLevel(queryStr);
		String oracleDatePatternStr = FlickrAreaDao.judgeDbDateCountPatternStr(queryLevel);
		Map parameters = new HashMap();
		parameters.put("areaid", area.getId());
		parameters.put("radius", area.getRadius());
		parameters.put("queryLevel", queryLevel);
		parameters.put("queryStr", queryStr);
		parameters.put("oracleDatePatternStr", oracleDatePatternStr);
		parameters.put("num", num);

		List<FlickrPhoto> photos = (List<FlickrPhoto>) sessionTemplate.selectList(this.getClass().getName() + ".Photo.selectByDateQuery", parameters);
		this.initPhotos(photos, area);

		return photos;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<FlickrPhoto> getPhotos(FlickrArea area, String tag, String queryStr, int page, int pageSize) {
		int offset = (page - 1) * pageSize;

		Level queryLevel = FlickrAreaDao.judgeQueryLevel(queryStr);
		String dbDatePatternStr = FlickrAreaDao.judgeDbDateCountPatternStr(queryLevel);
		Map parameters = new HashMap();

		if (queryLevel == Level.YEAR)
			throw new IllegalDataException("TO_DATE('" + queryStr + "', 'YYYY') in Oracle will returns a wrong result!");

		parameters.put("areaid", area.getId());
		parameters.put("radius", area.getRadius());
		parameters.put("tag", tag);
		parameters.put("queryLevel", queryLevel);
		parameters.put("queryStr", queryStr);
		parameters.put("dbDatePatternStr", dbDatePatternStr);
		parameters.put("num", pageSize);
		parameters.put("offset", offset);

		List<FlickrPhoto> photos = (List<FlickrPhoto>) sessionTemplate.selectList(this.getClass().getName() + ".Photo.selectByAreaTagDate", parameters);
		this.initPhotos(photos, area);

		return photos;
	}

}
