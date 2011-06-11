package de.fraunhofer.iais.spatial.dao.mybatis;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oracle.spatial.geometry.JGeometry;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.jdom.IllegalDataException;
import org.mybatis.spring.SqlSessionTemplate;

import de.fraunhofer.iais.spatial.dao.FlickrEuropeAreaDao;
import de.fraunhofer.iais.spatial.dto.FlickrEuropeAreaDto;
import de.fraunhofer.iais.spatial.dto.FlickrEuropeAreaDto.Level;
import de.fraunhofer.iais.spatial.entity.FlickrArea;
import de.fraunhofer.iais.spatial.entity.FlickrPhoto;
import de.fraunhofer.iais.spatial.entity.FlickrArea.Radius;

public class FlickrEuropeAreaDaoPgMybatis extends FlickrEuropeAreaDao {

private static final String DB_NAME = "Pg";
//	private final static String resource = "mybatis-config.xml";
	private SqlSessionTemplate sessionTemplate;

	public void setSessionTemplate(SqlSessionTemplate sessionTemplate) {
		this.sessionTemplate = sessionTemplate;
	}

	private void initArea(FlickrArea a, Radius radius) {
		if (a != null) {
			a.setRadius(radius);
			a.setSelectedCount(0);
			a.setTotalCount(getTotalCountWithinArea(a.getId(), a.getRadius()));
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

		String count = (String) sessionTemplate.selectOne(FlickrArea.class.getName() + DB_NAME + ".hourCount", area);
		if (count != null) {
			parseCountDbString(count, area.getHoursCount(), hourRegExPattern);
		}
	}

	private void loadDaysCount(FlickrArea area) {
		if (MapUtils.isNotEmpty(area.getDaysCount()))
			return; // cached

		String count = (String) sessionTemplate.selectOne(FlickrArea.class.getName() + DB_NAME + ".dayCount", area);
		if (count != null) {
			parseCountDbString(count, area.getDaysCount(), dayRegExPattern);
		}
	}

	private void loadMonthsCount(FlickrArea area) {
		if (MapUtils.isNotEmpty(area.getMonthsCount()))
			return; // cached

		String count = (String) sessionTemplate.selectOne(FlickrArea.class.getName() + DB_NAME + ".monthCount", area);
		if (count != null) {
			parseCountDbString(count, area.getMonthsCount(), monthRegExPattern);
		}
	}

	private void loadYearsCount(FlickrArea area) {
		if (MapUtils.isNotEmpty(area.getYearsCount()))
			return; // cached

		String count = (String) sessionTemplate.selectOne(FlickrArea.class.getName() + DB_NAME + ".yearCount", area);
		if (count != null) {
			parseCountDbString(count, area.getYearsCount(), yearRegExPattern);
		}
	}

	@Override
	public List<FlickrArea> getAllAreas(Radius radius) {
		FlickrEuropeAreaDto areaDto = new FlickrEuropeAreaDto();
		areaDto.setRadius(radius);

		List<FlickrArea> as = (List<FlickrArea>) sessionTemplate.selectList(FlickrArea.class.getName() + DB_NAME + ".selectAll", areaDto);
		initAreas(as, radius);

		return as;
	}

	@Override
	public FlickrArea getAreaById(int areaid, Radius radius) {
		FlickrEuropeAreaDto areaDto = new FlickrEuropeAreaDto();
		areaDto.setAreaid(areaid);
		areaDto.setRadius(radius);

		FlickrArea a = (FlickrArea) sessionTemplate.selectOne(FlickrArea.class.getName() + DB_NAME + ".selectById", areaDto);
		initArea(a, radius);

		return a;
	}

	@Override
	public List<FlickrArea> getAreasByPoint(double x, double y, Radius radius) {
		FlickrEuropeAreaDto areaDto = new FlickrEuropeAreaDto();
		String pgQueryGeom = "SRID=4326;point(" + x + " " + y + ")";
		areaDto.setPgQueryGeom(pgQueryGeom);
		areaDto.setRadius(radius);

		List<FlickrArea> as = (List<FlickrArea>) sessionTemplate.selectList(FlickrArea.class.getName() + DB_NAME + ".selectByIdGeom", areaDto);
		initAreas(as, radius);

		return as;
	}

	@Override
	public int getAreasByRectSize(double x1, double y1, double x2, double y2, Radius radius) {

		FlickrEuropeAreaDto areaDto = new FlickrEuropeAreaDto();
		String pgQueryGeom = "SRID=4326;polygon((" + x1 + " " + y1 + ", "
												   + x2 + " " + y1 + ", "
												   + x2 + " " + y2 + ", "
												   + x1 + " " + y2 + ", "
												   + x1 + " " + y1 + ""
												   + "))";
		areaDto.setPgQueryGeom(pgQueryGeom);
		areaDto.setRadius(radius);

		Object numObj = sessionTemplate.selectOne(FlickrArea.class.getName() + DB_NAME + ".selectByIdGeomSize", areaDto);
		int num = 0;
		if(numObj != null){
			num = (Integer)numObj;
		}
		return num;
	}

	@Override
	public List<FlickrArea> getAreasByRect(double x1, double y1, double x2, double y2, Radius radius) {

		FlickrEuropeAreaDto areaDto = new FlickrEuropeAreaDto();
		String pgQueryGeom = "SRID=4326;polygon((" + x1 + " " + y1 + ", "
												   + x2 + " " + y1 + ", "
												   + x2 + " " + y2 + ", "
												   + x1 + " " + y2 + ", "
												   + x1 + " " + y1 + ""
												   + "))";
		areaDto.setPgQueryGeom(pgQueryGeom);
		areaDto.setRadius(radius);

		List<FlickrArea> as = (List<FlickrArea>) sessionTemplate.selectList(FlickrArea.class.getName() + DB_NAME + ".selectByIdGeom", areaDto);
		initAreas(as, radius);

		return as;
	}

	@Override
	public List<FlickrArea> getAreasByPolygon(List<Point2D> polygon, Radius radius) {

		FlickrEuropeAreaDto areaDto = new FlickrEuropeAreaDto();
		String pgQueryGeom = "SRID=4326;polygon((";
		for (Point2D p : polygon) {
			pgQueryGeom += + p.getX() + " " + p.getY() + ", ";
		}

		pgQueryGeom = StringUtils.removeEnd(pgQueryGeom, ", ");
		pgQueryGeom +=  "))";

		areaDto.setPgQueryGeom(pgQueryGeom);
		areaDto.setRadius(radius);


		List<FlickrArea> as = (List<FlickrArea>) sessionTemplate.selectList(FlickrArea.class.getName() + DB_NAME + ".selectByIdGeom", areaDto);
		initAreas(as, radius);

		return as;
	}

	@Override
	public long getTotalCountWithinArea(long areaid, Radius radius) {
		FlickrEuropeAreaDto areaDto = new FlickrEuropeAreaDto();
		areaDto.setAreaid(areaid);
		areaDto.setRadius(radius);

		Object numObj = sessionTemplate.selectOne(FlickrArea.class.getName() + DB_NAME  + ".totalCount", areaDto);
		long num = 0;
		if(numObj != null){
			num = (Long)numObj;
		}
		return num;
	}

	@Override
	public long getTotalPhotoNum() {
		Object numObj = sessionTemplate.selectOne(FlickrPhoto.class.getName() + DB_NAME  + ".totalNum");
		long num = 0;
		if(numObj != null){
			num = (Long)numObj;
		}
		return num;
	}

	@Override
	public long getTotalWorldPhotoNum() {
		Object numObj = sessionTemplate.selectOne(FlickrPhoto.class.getName() + DB_NAME  + ".totalWorldPhotoNum");
		long num = 0;
		if(numObj != null){
			num = (Long)numObj;
		}
		return num;
	}

	@Override
	public long getTotalPeopleNum() {
		Object numObj = sessionTemplate.selectOne(FlickrPhoto.class.getName() + DB_NAME  + ".totalPeopleNum");
		long num = 0;
		if(numObj != null){
			num = (Long)numObj;
		}
		return num;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected List<FlickrPhoto> getPhotos(FlickrArea area, String queryStr, int num) {

		Level queryLevel = FlickrEuropeAreaDao.judgeQueryLevel(queryStr);
		String oracleDatePatternStr = FlickrEuropeAreaDao.judgeDbDateCountPatternStr(queryLevel);
		Map parameters = new HashMap();

		if(queryLevel == Level.YEAR){
			throw new IllegalDataException("TO_DATE('" + queryStr + "', 'YYYY') in Oracle will returns a wrong result!");
		}

		parameters.put("areaid", area.getId());
		parameters.put("radius", area.getRadius());
		parameters.put("queryLevel", queryLevel);
		parameters.put("queryStr", queryStr);
		parameters.put("oracleDatePatternStr", oracleDatePatternStr);
		parameters.put("num", num);

		List<FlickrPhoto> photos = (List<FlickrPhoto>) sessionTemplate.selectList(FlickrPhoto.class.getName() + DB_NAME  + ".selectByDateQuery", parameters);
		this.initPhotos(photos, area);

		return photos;
	}

}
