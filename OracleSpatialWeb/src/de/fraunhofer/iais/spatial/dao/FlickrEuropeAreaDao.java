package de.fraunhofer.iais.spatial.dao;

import java.awt.geom.Point2D;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.fraunhofer.iais.spatial.dto.FlickrEuropeAreaDto;
import de.fraunhofer.iais.spatial.dto.FlickrEuropeAreaDto.Level;
import de.fraunhofer.iais.spatial.entity.FlickrArea;
import de.fraunhofer.iais.spatial.entity.FlickrArea.Radius;
import de.fraunhofer.iais.spatial.entity.FlickrPhoto;

public abstract class FlickrEuropeAreaDao {

	public static String dbHourPatternStr = "YYYY-MM-DD@HH24";
	public static String dbDayPatternStr = "YYYY-MM-DD";
	public static String dbMonthPatternStr = "YYYY-MM";
	public static String dbYearPatternStr = "YYYY";

	public static SimpleDateFormat hourDateFormat = new SimpleDateFormat("yyyy-MM-dd@HH");
	public static SimpleDateFormat dayDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	public static SimpleDateFormat monthDateFormat = new SimpleDateFormat("yyyy-MM");
	public static SimpleDateFormat yearDateFormat = new SimpleDateFormat("yyyy");

	// eg. 2010-03-04@23:334;
	public static Pattern hourRegExPattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}@\\d{2}):(\\d+);");
	public static Pattern dayRegExPattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}):(\\d+);");
	public static Pattern monthRegExPattern = Pattern.compile("(\\d{4}-\\d{2})(\\d+);");
	public static Pattern yearRegExPattern = Pattern.compile("(\\d{4}):(\\d+);");

	// eg. 2010-03-04@23:test tags|32,schön|334,宫殿|12;
	public static Pattern hourTagsRegExPattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}@\\d{2}):<(([\\p{L} \\p{Nd}]+\\|\\d+,?)+)>;");


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
	 * @param radius
	 * @return long - number of photos
	 */
	public abstract long getTotalCountWithinArea(long areaid, Radius radius);

	/**
	 * get the total amount of photos stored in the database
	 * @return long - number of photos
	 */
	public abstract long getTotalPhotoNum();

	/**
	 * get the total amount of world photos stored in the database
	 * @return long - number of world photos
	 */
	public abstract long getTotalWorldPhotoNum();

	/**
	 * get the total amount of people stored in the database
	 * @return long - number of people
	 */
	public abstract long getTotalPeopleNum();

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

		List<String> tempQueryStrs = new ArrayList<String>(new TreeSet<String>(areaDto.getQueryStrs()));
//		List<String> tempQueryStrs = new ArrayList<String>(areaDto.getQueryStrs());
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

		//sort photos list based on comparator
		Collections.sort(photos, new Comparator<FlickrPhoto>() {
			@Override
			public int compare(FlickrPhoto p1, FlickrPhoto p2) {
				return p2.getDate().compareTo(p1.getDate());
			}
		});

		return photos;
	}

	public final static String judgeDbDateCountPatternStr(Level queryLevel){
		String dbDatePatternStr = null;

		switch (queryLevel) {
		case YEAR:
			dbDatePatternStr = FlickrEuropeAreaDao.dbYearPatternStr;
			break;
		case MONTH:
			dbDatePatternStr = FlickrEuropeAreaDao.dbMonthPatternStr;
			break;
		case DAY:
			dbDatePatternStr = FlickrEuropeAreaDao.dbDayPatternStr;
			break;
		case HOUR:
			dbDatePatternStr = FlickrEuropeAreaDao.dbHourPatternStr;
			break;
		}
		return dbDatePatternStr;
	}

	public final static Pattern judgeDbDateCountRegExPattern(Level queryLevel){
		Pattern dataCountRegExPatter = null;

		switch (queryLevel) {
		case YEAR:
			dataCountRegExPatter = FlickrEuropeAreaDao.yearRegExPattern;
			break;
		case MONTH:
			dataCountRegExPatter = FlickrEuropeAreaDao.monthRegExPattern;
			break;
		case DAY:
			dataCountRegExPatter = FlickrEuropeAreaDao.dayRegExPattern;
			break;
		case HOUR:
			dataCountRegExPatter = FlickrEuropeAreaDao.hourRegExPattern;
			break;
		}
		return dataCountRegExPatter;
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

	public final static void parseCountDbString(String count, SortedMap<String, Integer> datesCount, Pattern dateRegExPattern) {
		Matcher m = dateRegExPattern.matcher(count);
		while (m.find()) {
			datesCount.put(m.group(1), Integer.parseInt(m.group(2)));
		}
	}

	public final static void parseHoursTagsCountDbString(String count, SortedMap<String, Map<String, Integer>> hoursTagsCount) {
		Matcher m = hourTagsRegExPattern.matcher(count);
		while (m.find()) {
			Map<String, Integer> tagsCount = Maps.newLinkedHashMap();
			for(String term : StringUtils.split(m.group(2), ",")){
				tagsCount.put(StringUtils.substringBefore(term, "|"), NumberUtils.toInt(StringUtils.substringAfter(term, "|")));
			}
			hoursTagsCount.put(m.group(1), tagsCount);
		}
	}

	public final static String createDatesCountDbString(SortedMap<String, Integer> datesCount){
		StringBuffer strBuffer = new StringBuffer();

		for (Map.Entry<String, Integer> e : datesCount.entrySet()) {
			strBuffer.append(e.getKey())
					 .append(":")
					 .append(e.getValue())
					 .append(";");
		}
		return strBuffer.toString();
	}

	public final static String createDatesTagsCountDbString(SortedMap<String, Map<String, Integer>> datesTagsCount){
		StringBuffer strBuffer = new StringBuffer();

		for (Map.Entry<String, Map<String, Integer>> e : datesTagsCount.entrySet()) {
			strBuffer.append(e.getKey())
					 .append(":")
					 .append("<");

			for (Map.Entry<String, Integer> term : sortTagsCountByValuesDesc(e.getValue()).entrySet()) {
				strBuffer.append(term.getKey())
						 .append("|")
						 .append(term.getValue())
						 .append(",");
			}
			strBuffer.append(">")
					 .append(";");
		}
		return strBuffer.toString();
	}

	private static Map<String, Integer> sortTagsCountByValuesDesc(Map<String, Integer> unsortTagsCount) {

		List<Map.Entry<String, Integer>> entries = Lists.newLinkedList(unsortTagsCount.entrySet());

		//sort list based on comparator
		Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
			@Override
			public int compare(Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2) {
				return e2.getValue() - e1.getValue();
			}
		});

		//put sorted list into map again
		Map<String, Integer> sortedMap = Maps.newLinkedHashMap();

		for(Map.Entry<String, Integer> entry : entries){
			sortedMap.put(entry.getKey(), entry.getValue());
		}

	return sortedMap;
   }
}