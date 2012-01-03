package de.fraunhofer.iais.spatial.dao.mybatis;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jdom.IllegalDataException;
import org.mybatis.spring.SqlSessionTemplate;

import de.fraunhofer.iais.spatial.dao.FlickrAreaDao;
import de.fraunhofer.iais.spatial.dto.FlickrAreaDto;
import de.fraunhofer.iais.spatial.dto.FlickrAreaDto.Level;
import de.fraunhofer.iais.spatial.entity.FlickrArea;
import de.fraunhofer.iais.spatial.entity.FlickrPhoto;
import de.fraunhofer.iais.spatial.entity.FlickrArea.Radius;

public class FlickrAreaDaoPgMybatis extends FlickrAreaDao {

	private static final String DB_NAME = "Pg";

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

		if (area.isCached() == false) {
			//initialize the not cached object
			area.setCached(true);
			area.setTotalCount(getTotalCountWithinArea(area.getId()));
			/*
			loadYearsCount(area);
			loadMonthsCount(area);
			*/
			loadDaysCount(area);
			loadHoursCount(area);
		}
	}

	private void initAreas(List<FlickrArea> areas) {
		for (FlickrArea a : areas) {
			initArea(a);
		}
	}

	private void initPhotos(List<FlickrPhoto> photos, FlickrArea area) {
		for (FlickrPhoto photo : photos) {
			photo.setArea(area);
		}
	}

	@Override
	public void loadHoursTagsCount(FlickrArea area) {

		String count = (String) sessionTemplate.selectOne(FlickrArea.class.getName() + DB_NAME + ".hoursTagsCount", area);
		if (count != null) {
			parseHoursTagsCountDbString(count, area.getHoursTagsCount());
		}
	}

	@Override
	public void loadDaysTagsCount(FlickrArea area) {

		String count = (String) sessionTemplate.selectOne(FlickrArea.class.getName() + DB_NAME + ".daysTagsCount", area);
		if (count != null) {
			parseDaysTagsCountDbString(count, area.getDaysTagsCount());
		}
	}

	private void loadHoursCount(FlickrArea area) {

		String count = (String) sessionTemplate.selectOne(FlickrArea.class.getName() + DB_NAME + ".hoursCount", area);
		if (count != null) {
			parseCountDbString(count, area.getHoursCount(), hourRegExPattern);
		}
	}

	@Deprecated
	private void loadDaysCount(FlickrArea area) {

		String count = (String) sessionTemplate.selectOne(FlickrArea.class.getName() + DB_NAME + ".daysCount", area);
		if (count != null) {
			parseCountDbString(count, area.getDaysCount(), dayRegExPattern);
		}
	}

	@Deprecated
	private void loadMonthsCount(FlickrArea area) {

		String count = (String) sessionTemplate.selectOne(FlickrArea.class.getName() + DB_NAME + ".monthsCount", area);
		if (count != null) {
			parseCountDbString(count, area.getMonthsCount(), monthRegExPattern);
		}
	}

	@Deprecated
	private void loadYearsCount(FlickrArea area) {

		String count = (String) sessionTemplate.selectOne(FlickrArea.class.getName() + DB_NAME + ".yearsCount", area);
		if (count != null) {
			parseCountDbString(count, area.getYearsCount(), yearRegExPattern);
		}
	}

	@Override
	public List<Integer> getAllAreaIds(Radius radius) {

		FlickrAreaDto areaDto = new FlickrAreaDto();
		areaDto.setRadius(radius);

		return (List<Integer>) sessionTemplate.selectList(FlickrArea.class.getName() + DB_NAME + ".selectAllIds", areaDto);
	}

	@Override
	@Deprecated
	public List<FlickrArea> getAllAreas(Radius radius) {

		/*
		 * couldn't make use of the mybatis object cache
		FlickrEuropeAreaDto areaDto = new FlickrEuropeAreaDto();
		areaDto.setRadius(radius);
		List<FlickrArea> areas = (List<FlickrArea>) sessionTemplate.selectList(FlickrArea.class.getName() + DB_NAME + ".selectAll", areaDto);
		initAreas(areas, radius);
		*/

		List<FlickrArea> areas = new ArrayList<FlickrArea>();
		for (int areaid : getAllAreaIds(radius)) {
			areas.add(getAreaById(areaid, radius));
		}

		return areas;
	}

	@Override
	public FlickrArea getAreaById(int areaid, Radius radius) {
		FlickrAreaDto areaDto = new FlickrAreaDto();
		areaDto.setAreaid(areaid);
		areaDto.setRadius(radius);

		FlickrArea area = (FlickrArea) sessionTemplate.selectOne(FlickrArea.class.getName() + DB_NAME + ".selectById", areaDto);
		initArea(area);

		return area;
	}

	@Override
	public List<Integer> getAreaIdsByPoint(double x, double y, Radius radius) {
		FlickrAreaDto areaDto = new FlickrAreaDto();
		String pgQueryGeom = "SRID=4326;point(" + x + " " + y + ")";
		areaDto.setPgQueryGeom(pgQueryGeom);
		areaDto.setRadius(radius);

		return (List<Integer>) sessionTemplate.selectList(FlickrArea.class.getName() + DB_NAME + ".selectIdsByGeom", areaDto);
	}

	@Override
	@Deprecated
	public List<FlickrArea> getAreasByPoint(double x, double y, Radius radius) {
		List<FlickrArea> areas = new ArrayList<FlickrArea>();
		for (int areaid : getAreaIdsByPoint(x, y, radius)) {
			areas.add(getAreaById(areaid, radius));
		}
		return areas;
	}

	@Override
	public int getAreasByRectSize(double x1, double y1, double x2, double y2, Radius radius) {

		FlickrAreaDto areaDto = new FlickrAreaDto();
		String pgQueryGeom = "SRID=4326;polygon((" + x1 + " " + y1 + ", " + x2 + " " + y1 + ", " + x2 + " " + y2 + ", " + x1 + " " + y2 + ", " + x1 + " " + y1 + "" + "))";
		areaDto.setPgQueryGeom(pgQueryGeom);
		areaDto.setRadius(radius);

		Object numObj = sessionTemplate.selectOne(FlickrArea.class.getName() + DB_NAME + ".selectByGeomSize", areaDto);
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
	public List<Integer> getAreaIdsByRect(double x1, double y1, double x2, double y2, Radius radius) {

		FlickrAreaDto areaDto = new FlickrAreaDto();
		String pgQueryGeom = "SRID=4326;polygon((" + x1 + " " + y1 + ", " + x2 + " " + y1 + ", " + x2 + " " + y2 + ", " + x1 + " " + y2 + ", " + x1 + " " + y1 + "" + "))";
		areaDto.setPgQueryGeom(pgQueryGeom);
		areaDto.setRadius(radius);

		return (List<Integer>) sessionTemplate.selectList(FlickrArea.class.getName() + DB_NAME + ".selectIdsByGeom", areaDto);
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
	@Deprecated
	public List<FlickrArea> getAreasByRect(double x1, double y1, double x2, double y2, Radius radius) {

		List<FlickrArea> areas = new ArrayList<FlickrArea>();
		for (int areaid : getAreaIdsByRect(x1, y1, x2, y2, radius)) {
			areas.add(getAreaById(areaid, radius));
		}

		return areas;
	}

	@Override
	public List<Integer> getAreaIdsByPolygon(List<Point2D> polygon, Radius radius) {

		FlickrAreaDto areaDto = new FlickrAreaDto();
		String pgQueryGeom = "SRID=4326;polygon((";
		for (Point2D p : polygon) {
			pgQueryGeom += +p.getX() + " " + p.getY() + ", ";
		}

		pgQueryGeom = StringUtils.removeEnd(pgQueryGeom, ", ");
		pgQueryGeom += "))";

		areaDto.setPgQueryGeom(pgQueryGeom);
		areaDto.setRadius(radius);

		return (List<Integer>) sessionTemplate.selectList(FlickrArea.class.getName() + DB_NAME + ".selectIdsByGeom", areaDto);
	}

	@Override
	@Deprecated
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

		Object numObj = sessionTemplate.selectOne(FlickrArea.class.getName() + DB_NAME + ".totalCount", areaDto);
		long num = 0;
		if (numObj != null) {
			num = (Long) numObj;
		}
		return num;
	}

	@Override
	public long getTotalEuropePhotoNum() {
		Object numObj = sessionTemplate.selectOne(FlickrPhoto.class.getName() + DB_NAME + ".totalNum");
		long num = 0;
		if (numObj != null) {
			num = (Long) numObj;
		}
		return num;
	}

	@Override
	public long getTotalWorldPhotoNum() {
		Object numObj = sessionTemplate.selectOne(FlickrPhoto.class.getName() + DB_NAME + ".totalWorldPhotoNum");
		long num = 0;
		if (numObj != null) {
			num = (Long) numObj;
		}
		return num;
	}

	@Override
	public long getTotalPeopleNum() {
		Object numObj = sessionTemplate.selectOne(FlickrPhoto.class.getName() + DB_NAME + ".totalPeopleNum");
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
		String dbDatePatternStr = FlickrAreaDao.judgeDbDateCountPatternStr(queryLevel);
		Map parameters = new HashMap();

		if (queryLevel == Level.YEAR) {
			throw new IllegalDataException("TO_DATE('" + queryStr + "', 'YYYY') in Oracle will returns a wrong result!");
		}

		parameters.put("areaid", area.getId());
		parameters.put("radius", area.getRadius());
		parameters.put("queryLevel", queryLevel);
		parameters.put("queryStr", queryStr);
		parameters.put("dbDatePatternStr", dbDatePatternStr);
		parameters.put("num", num);

		List<FlickrPhoto> photos = (List<FlickrPhoto>) sessionTemplate.selectList(FlickrPhoto.class.getName() + DB_NAME + ".selectByAreaDate", parameters);
		this.initPhotos(photos, area);

		return photos;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<FlickrPhoto> getPhotos(FlickrArea area, String tag, String queryStr, int num, int offset) {

		Level queryLevel = FlickrAreaDao.judgeQueryLevel(queryStr);
		String dbDatePatternStr = FlickrAreaDao.judgeDbDateCountPatternStr(queryLevel);
		Map parameters = new HashMap();

		if (queryLevel == Level.YEAR) {
			throw new IllegalDataException("TO_DATE('" + queryStr + "', 'YYYY') in Oracle will returns a wrong result!");
		}

		parameters.put("areaid", area.getId());
		parameters.put("radius", area.getRadius());
		parameters.put("tag", tag);
		parameters.put("queryLevel", queryLevel);
		parameters.put("queryStr", queryStr);
		parameters.put("dbDatePatternStr", dbDatePatternStr);
		parameters.put("num", num);
		parameters.put("offset", offset);

		List<FlickrPhoto> photos = (List<FlickrPhoto>) sessionTemplate.selectList(FlickrPhoto.class.getName() + DB_NAME + ".selectByAreaTagDate", parameters);
		this.initPhotos(photos, area);

		return photos;
	}

}
