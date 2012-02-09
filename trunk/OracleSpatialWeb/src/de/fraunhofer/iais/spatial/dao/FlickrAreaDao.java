package de.fraunhofer.iais.spatial.dao;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.fraunhofer.iais.spatial.dto.FlickrAreaDto;
import de.fraunhofer.iais.spatial.dto.FlickrAreaDto.Level;
import de.fraunhofer.iais.spatial.entity.FlickrArea;
import de.fraunhofer.iais.spatial.entity.FlickrPhoto;
import de.fraunhofer.iais.spatial.entity.FlickrArea.Radius;

/**
 * Data Access Interface to database
 * @author <a href="mailto:haolin.zhi@iais.fraunhofer.de">Haolin Zhi</A>
 * @author <a href="mailto:iulian.peca@iais.fraunhofer.de">Iulian Peca</a>
 *
 */
public abstract class FlickrAreaDao {

	public static String dbHourPatternStr = "YYYY-MM-DD@HH24";
	public static String dbDayPatternStr = "YYYY-MM-DD";
	public static String dbMonthPatternStr = "YYYY-MM";
	public static String dbYearPatternStr = "YYYY";

	public static String hourDateFormatStr = "yyyy-MM-dd@HH";
	public static String dayDateFormatStr = "yyyy-MM-dd";
	public static String monthDateFormatStr = "yyyy-MM";
	public static String yearDateFormatStr = "yyyy";

	// eg. 2010-03-04@23:334;
	public static String hourRegExPatternStr = "(\\d{4}-\\d{2}-\\d{2}@\\d{2}):(\\d+);";
	public static String dayRegExPatternStr = "(\\d{4}-\\d{2}-\\d{2}):(\\d+);";
	public static String monthRegExPatternStr = "(\\d{4}-\\d{2})(\\d+);";
	public static String yearRegExPatternStr = "(\\d{4}):(\\d+);";

	// eg. 2010-03-04@23:test tags|32,schön|334,宫殿|12;
	public static String hourTagsRegExPatternStr = "(\\d{4}-\\d{2}-\\d{2}@\\d{2}):<(([\\p{L} \\p{Nd}]+\\|\\d+,?)+)>;";
	public static String dayTagsRegExPatternStr = "(\\d{4}-\\d{2}-\\d{2}):<(([\\p{L} \\p{Nd}]+\\|\\d+,?)+)>;";

	/**
	 * Returns the instance of FlickrArea related to that areaid
	 * @param areaid
	 * @return FlickrArea
	 */
	public abstract FlickrArea getAreaById(int areaid, Radius radius);

	/**
	 * Returns a List of FlickrArea ids which interact to this point
	 * @param x
	 * @param y
	 * @return List<Integer>
	 */
	public abstract List<Integer> getAreaIdsByPoint(double x, double y, Radius radius);

	/**
	 * Returns a List of FlickrArea instances which interact to this point
	 * @param x
	 * @param y
	 * @return List<FlickrArea>
	 */
	@Deprecated
	public abstract List<FlickrArea> getAreasByPoint(double x, double y, Radius radius);

	/**
	 * Returns the size of the List of FlickrArea instances which interact to this rectangle
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return List<FlickrArea>
	 */
	@Deprecated
	public abstract int getAreasByRectSize(double x1, double y1, double x2, double y2, Radius radius);

	/**
	 * Returns the size of the List of FlickrArea instances which interact to this rectangle
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @param crossDateLine
	 * @return List<FlickrArea>
	 */
	public abstract int getAreasByRectSize(double x1, double y1, double x2, double y2, Radius radius, boolean crossDateLine);

	/**
	 * Returns a List of FlickrArea ids which interact to this rectangle
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return List<Integer>
	 */
	@Deprecated
	public abstract List<Integer> getAreaIdsByRect(double x1, double y1, double x2, double y2, Radius radius);

	/**
	 * Returns a List of FlickrArea ids which interact to this rectangle
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @param radius
	 * @param crossDateLine
	 * @return List<Integer>
	 */
	public abstract List<Integer> getAreaIdsByRect(double x1, double y1, double x2, double y2, Radius radius, boolean crossDateLine);

	/**
	 * Returns a List of FlickrArea instances which interact to this rectangle
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return List<FlickrArea>
	 */
	@Deprecated
	public abstract List<FlickrArea> getAreasByRect(double x1, double y1, double x2, double y2, Radius radius);

	/**
	 * Returns a List of FlickrArea ids which interact to this polygon
	 * @param polygon
	 * @param radius
	 * @return List<Integer>
	 */
	public abstract List<Integer> getAreaIdsByPolygon(List<Point2D> polygon, Radius radius);

	/**
	 * Returns a List of FlickrArea instances which interact to this polygon
	 * @param polygon
	 * @param radius
	 * @return List<FlickrArea>
	 */
	@Deprecated
	public abstract List<FlickrArea> getAreasByPolygon(List<Point2D> polygon, Radius radius);

	/**
	 *
	 * @param area
	 */
	public abstract void loadHoursTagsCount(FlickrArea area, boolean withoutStopWords);

	/**
	 * get the total amount of photos uploaded within this area
	 * @param areaid
	 * @return long - number of photos
	 */
	public abstract long getTotalCountWithinArea(long areaid);

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
	 *
	 * @param area
	 * @param areaDto
	 * @param page
	 * @param pageSize
	 * @return
	 */
	public final List<FlickrPhoto> getPhotos(FlickrArea area, FlickrAreaDto areaDto, int page, int pageSize) {
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
				if (pos + count.get(tempQueryStrs.get(i)) <= start) {
					pos += count.get(tempQueryStrs.get(i));
				} else {
					break;
				}
			}
			idx++;
		}

		for (int i = tempQueryStrs.size() - idx; photos.size() < pageSize && i >= 0; i--) {
			if (count != null && count.get(tempQueryStrs.get(i)) != null && count.get(tempQueryStrs.get(i)) > 0) {
				List<FlickrPhoto> tempPhotos = this.getPhotos(area, tempQueryStrs.get(i), pageSize - photos.size() + (start - pos));
				photos.addAll(tempPhotos.subList(start - pos, tempPhotos.size()));
				pos = start;
			}
		}

		int i = 1;
		for (FlickrPhoto photo : photos) {
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

	public abstract List<FlickrPhoto> getPhotos(FlickrArea area, String tag, String queryStr, int page, int pageSize);

	public final static String judgeDbDateCountPatternStr(Level queryLevel) {
		String dbDatePatternStr = null;

		switch (queryLevel) {
		case YEAR:
			dbDatePatternStr = FlickrAreaDao.dbYearPatternStr;
			break;
		case MONTH:
			dbDatePatternStr = FlickrAreaDao.dbMonthPatternStr;
			break;
		case DAY:
			dbDatePatternStr = FlickrAreaDao.dbDayPatternStr;
			break;
		case HOUR:
			dbDatePatternStr = FlickrAreaDao.dbHourPatternStr;
			break;
		}
		return dbDatePatternStr;
	}

	public final static Pattern judgeDbDateCountRegExPattern(Level queryLevel) {
		Pattern dataCountRegExPatter = null;

		switch (queryLevel) {
		case YEAR:
			dataCountRegExPatter = Pattern.compile(FlickrAreaDao.yearRegExPatternStr);
			break;
		case MONTH:
			dataCountRegExPatter = Pattern.compile(FlickrAreaDao.monthRegExPatternStr);
			break;
		case DAY:
			dataCountRegExPatter = Pattern.compile(FlickrAreaDao.dayRegExPatternStr);
			break;
		case HOUR:
			dataCountRegExPatter = Pattern.compile(FlickrAreaDao.hourRegExPatternStr);
			break;
		}
		return dataCountRegExPatter;
	}

	protected final static Level judgeQueryLevel(String QueryStr) {
		if (QueryStr.matches(FlickrAreaDao.hourRegExPatternStr.split(":")[0]))
			return Level.HOUR;
		else if (QueryStr.matches(FlickrAreaDao.dayRegExPatternStr.split(":")[0]))
			return Level.DAY;
		else if (QueryStr.matches(FlickrAreaDao.monthRegExPatternStr.split(":")[0]))
			return Level.MONTH;
		else if (QueryStr.matches(FlickrAreaDao.yearRegExPatternStr.split(":")[0]))
			return Level.YEAR;
		else
			return null;

	}

	public final static synchronized void parseCountDbString(String count, SortedMap<String, Integer> datesCount, Pattern dateRegExPattern) {
		Matcher m = dateRegExPattern.matcher(count);
		while (m.find()) {
			datesCount.put(m.group(1), Integer.parseInt(m.group(2)));
		}
	}

	public final static void parseHoursTagsCountDbString(String count, SortedMap<String, Map<String, Integer>> hoursTagsCount) {
		parseHoursTagsCountDbString(count, hoursTagsCount, null);
	}

	public final static void parseHoursTagsCountDbString(String count, SortedMap<String, Map<String, Integer>> hoursTagsCount, List<Set<String>> stopwordsList) {
/* using regex consumes too much memory
//		hourTagsRegExPatternStr = "(\\d{4}-\\d{2}-\\d{2}@\\d{2}):<(([\\p{L} \\p{Nd}]+\\|\\d+,?)+)>;";
//		Matcher m = Pattern.compile(hourTagsRegExPatternStr).matcher(count);
*/
		for (String terms : StringUtils.split(count, ";")) {
			String key = terms.substring(0, 13);
			if (!key.matches("\\d{4}-\\d{2}-\\d{2}@\\d{2}"))
				throw new RuntimeException("hour key:" + key);
			String content = terms.substring(15, terms.length() - 1);
			Map<String, Integer> tagsCount = Maps.newLinkedHashMap();
			for (String term : StringUtils.split(content, ",")) {
				boolean isStopword = false;
				if (CollectionUtils.isNotEmpty(stopwordsList)) {
					for (Set<String> stopwords : stopwordsList) {
						if (stopwords.contains(StringUtils.substringBefore(term, "|"))) {
							isStopword = true;
							break;
						}
					}
				}
				if (isStopword == false) {
					tagsCount.put(StringUtils.substringBefore(term, "|"), NumberUtils.toInt(StringUtils.substringAfter(term, "|")));
				}
			}
			if (tagsCount.size() > 0) {
				hoursTagsCount.put(key, tagsCount);
			}
		}
	}

	public final static void parseDaysTagsCountDbString(String count, SortedMap<String, Map<String, Integer>> daysTagsCount) {
/* using regex consumes too much memory
		Matcher m = Pattern.compile(dayTagsRegExPatternStr).matcher(count);
		while (m.find()) {
*/
		for (String terms : StringUtils.split(count, ";")) {
			String key = terms.substring(0, 10);
			if (!key.matches("\\d{4}-\\d{2}-\\d{2}"))
				throw new RuntimeException("day key:" + key);
			String content = terms.substring(12, terms.length() - 1);
			Map<String, Integer> tagsCount = Maps.newLinkedHashMap();
			for (String term : StringUtils.split(content, ",")) {
				tagsCount.put(StringUtils.substringBefore(term, "|"), NumberUtils.toInt(StringUtils.substringAfter(term, "|")));
			}
			daysTagsCount.put(key, tagsCount);
		}
	}

	public final static String createDatesCountDbString(SortedMap<String, Integer> datesCount) {
		StringBuffer strBuffer = new StringBuffer();

		for (Map.Entry<String, Integer> e : datesCount.entrySet()) {
			strBuffer.append(e.getKey()).append(":").append(e.getValue()).append(";");
		}
		return strBuffer.toString();
	}

	public final static String createDatesTagsCountDbString(SortedMap<String, Map<String, Integer>> datesTagsCount) {
		return createDatesTagsCountDbString(datesTagsCount, 0);
	}

	public final static String createDatesTagsCountDbString(SortedMap<String, Map<String, Integer>> datesTagsCount, int limitSize) {
		StringBuffer strBuffer = new StringBuffer();

		for (Map.Entry<String, Map<String, Integer>> e : datesTagsCount.entrySet()) {
			strBuffer.append(e.getKey()).append(":").append("<");

			int i = 0;
			for (Map.Entry<String, Integer> term : sortTagsCountByValuesDesc(e.getValue()).entrySet()) {
				if (limitSize > 0 && i++ > limitSize) {
					break;
				}

				strBuffer.append(term.getKey()).append("|").append(term.getValue()).append(",");
			}
			strBuffer.append(">").append(";");
		}
		return strBuffer.toString();
	}

	public static Map<String, Integer> sortTagsCountByValuesDesc(Map<String, Integer> unsortTagsCount) {

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

		for (Map.Entry<String, Integer> entry : entries) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}

		return sortedMap;
	}

}