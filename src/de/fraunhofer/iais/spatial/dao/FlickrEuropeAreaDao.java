package de.fraunhofer.iais.spatial.dao;

import java.awt.geom.Point2D;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hamcrest.core.IsInstanceOf;

import de.fraunhofer.iais.spatial.dto.FlickrEuropeAreaDto;
import de.fraunhofer.iais.spatial.dto.FlickrEuropeAreaDto.Level;
import de.fraunhofer.iais.spatial.entity.FlickrArea;
import de.fraunhofer.iais.spatial.entity.FlickrPhoto;
import de.fraunhofer.iais.spatial.entity.FlickrArea.Radius;

public abstract class FlickrEuropeAreaDao {

	public static String oracleHourPatternStr = "YYYY-MM-DD@HH24";
	public static String oracleDayPatternStr = "YYYY-MM-DD";
	public static String oracleMonthPatternStr = "YYYY-MM";
	public static String oracleYearPatternStr = "YYYY";

	public static SimpleDateFormat hourDateFormat = new SimpleDateFormat("yyyy-MM-dd@HH");
	public static SimpleDateFormat dayDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	public static SimpleDateFormat monthDateFormat = new SimpleDateFormat("yyyy-MM");
	public static SimpleDateFormat yearDateFormat = new SimpleDateFormat("yyyy");

	public static Pattern hourRegExPattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}@\\d{2}):(\\d{1,});");
	public static Pattern dayRegExPattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}):(\\d{1,});");
	public static Pattern monthRegExPattern = Pattern.compile("(\\d{4}-\\d{2}):(\\d{1,});");
	public static Pattern yearRegExPattern = Pattern.compile("(\\d{4}):(\\d{1,});");


	/**
	 * Returns a List of all the FlickrDeWestArea instances
	 * @return List<FlickrDeWestArea>
	 */
	public abstract List<FlickrArea> getAllAreas(Radius radius);

	/**
	 * Returns the instance of FlickrDeWestArea related to that areaid
	 * @param areaid
	 * @return FlickrDeWestArea
	 */
	public abstract FlickrArea getAreaById(int areaid, Radius radius);

	/**
	 * Returns a List of FlickrDeWestArea instances which interact to this point
	 * @param x
	 * @param y
	 * @return List<FlickrDeWestArea>
	 */
	public abstract List<FlickrArea> getAreasByPoint(double x, double y, Radius radius);

	/**
	 * Returns a List of FlickrDeWestArea instances which interact to this rectangle
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return List<FlickrDeWestArea>
	 */
	public abstract int getAreasByRectSize(double x1, double y1, double x2, double y2, Radius radius);

	/**
	 * Returns a List of FlickrDeWestArea instances which interact to this rectangle
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return List<FlickrDeWestArea>
	 */
	public abstract List<FlickrArea> getAreasByRect(double x1, double y1, double x2, double y2, Radius radius);

	/**
	 * Returns a List of FlickrDeWestArea instances which interact to this polygon
	 * @param polygon
	 * @param radius
	 * @return
	 */
	public abstract List<FlickrArea> getAreasByPolygon(List<Point2D> polygon, Radius radius);

	/**
	 * get the total amount of photos uploaded within this area
	 * @param areaid
	 * @return int - number of photos
	 */
	public abstract int getTotalCount(int areaid, Radius radius);


	protected abstract List<FlickrPhoto> getPhotos(FlickrArea area, String queryStr, int num);

	/**
	 * Returns a List of FlickrDeWestPhoto instances within the area
	 * @param areaid
	 * @param radius
	 * @param queryStrs
	 * @param page - page index >= 1
	 * @param pageSize
	 * @return
	 */
	public final List<FlickrPhoto> getPhotos(FlickrArea area, FlickrEuropeAreaDto areaDto, int page, int pageSize) {
		int start = (page - 1) * pageSize;
		List<FlickrPhoto> photos = new ArrayList<FlickrPhoto>();
		Map<String, Integer> count = null;

		switch (areaDto.getQueryLevel()) {
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

		int idx = 1;
		int pos = 0;

		List<String> tempQueryStrs = new ArrayList<String>(areaDto.getQueryStrs());
		for (int i = tempQueryStrs.size() - 1; i >= 0; i--) {
			if (count != null && count.get(tempQueryStrs.get(i)) != null && count.get(tempQueryStrs.get(i)) > 0) {
				if(pos + count.get(tempQueryStrs.get(i)) <= start){
					pos += count.get(tempQueryStrs.get(i));
				}else{
					break;
				}
			}
			idx ++;
		}

		for (int i = tempQueryStrs.size() - idx; photos.size() < pageSize && i >= 0; i--) {
			if (count != null && count.get(tempQueryStrs.get(i)) != null && count.get(tempQueryStrs.get(i)) > 0) {
				List<FlickrPhoto> tempPhotos = this.getPhotos(area, tempQueryStrs.get(i), pageSize - photos.size() + (start - pos));
				photos.addAll(tempPhotos.subList(start - pos, tempPhotos.size()));
				pos = start;
			}
		}

		int i = 1;
		for (FlickrPhoto photo : photos){
			photo.setIndex((i++) + (page - 1) * pageSize);
		}

		return photos;
	}

	public final static String judgeOracleDatePatternStr(Level queryLevel){
		String oracleDatePatternStr = null;

		switch (queryLevel) {
		case YEAR:
			oracleDatePatternStr = FlickrEuropeAreaDao.oracleYearPatternStr;
			break;
		case MONTH:
			oracleDatePatternStr = FlickrEuropeAreaDao.oracleMonthPatternStr;
			break;
		case DAY:
			oracleDatePatternStr = FlickrEuropeAreaDao.oracleDayPatternStr;
			break;
		case HOUR:
			oracleDatePatternStr = FlickrEuropeAreaDao.oracleHourPatternStr;
			break;
		}
		return oracleDatePatternStr;
	}

	public final static Pattern judgeOracleRegExPattern(Level queryLevel){
		Pattern oracleRegExPatter = null;

		switch (queryLevel) {
		case YEAR:
			oracleRegExPatter = FlickrEuropeAreaDao.yearRegExPattern;
			break;
		case MONTH:
			oracleRegExPatter = FlickrEuropeAreaDao.monthRegExPattern;
			break;
		case DAY:
			oracleRegExPatter = FlickrEuropeAreaDao.dayRegExPattern;
			break;
		case HOUR:
			oracleRegExPatter = FlickrEuropeAreaDao.hourRegExPattern;
			break;
		}
		return oracleRegExPatter;
	}

	protected final static Level judgeQueryLevel(String QueryStr){
		if(QueryStr.matches(FlickrEuropeAreaDao.hourRegExPattern.pattern().split(":")[0])){
			return Level.HOUR;
		}else if(QueryStr.matches(FlickrEuropeAreaDao.dayRegExPattern.pattern().split(":")[0])){
			return Level.DAY;
		}else if(QueryStr.matches(FlickrEuropeAreaDao.monthRegExPattern.pattern().split(":")[0])){
			return Level.MONTH;
		}else if(QueryStr.matches(FlickrEuropeAreaDao.yearRegExPattern.pattern().split(":")[0])){
			return Level.YEAR;
		}else{
			return null;
		}

	}

	public final static void parseCounts(String count, Map<String, Integer> counts, Pattern dateRegExPattern) {
		Matcher m = dateRegExPattern.matcher(count);
		while (m.find()) {
			counts.put(m.group(1), Integer.parseInt(m.group(2)));
		}
	}

	public final static String createCountsDbString(Map<String, Integer> counts){
		StringBuffer strBuffer = new StringBuffer();
		if(!(counts instanceof SortedMap<?, ?>)){
			counts = new TreeMap<String, Integer>(counts);
		}

		for (Map.Entry<String, Integer> e : counts.entrySet()) {
			strBuffer.append(e.getKey())
					 .append(":")
					 .append(e.getValue())
					 .append(";");
		}
		return strBuffer.toString();
	}
}