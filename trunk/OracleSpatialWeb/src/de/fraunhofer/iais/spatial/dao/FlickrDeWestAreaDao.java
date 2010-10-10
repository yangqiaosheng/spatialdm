package de.fraunhofer.iais.spatial.dao;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.regex.Pattern;

import de.fraunhofer.iais.spatial.dto.FlickrDeWestAreaDto.QueryLevel;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestPhoto;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea.Radius;

public abstract class FlickrDeWestAreaDao {

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
	public abstract List<FlickrDeWestArea> getAllAreas(Radius radius);

	/**
	 * Returns the instance of FlickrDeWestArea related to that areaid
	 * @param areaid
	 * @return FlickrDeWestArea
	 */
	public abstract FlickrDeWestArea getAreaById(int areaid, Radius radius);

	/**
	 * Returns a List of FlickrDeWestArea instances which interact to this point
	 * @param x
	 * @param y
	 * @return List<FlickrDeWestArea>
	 */
	public abstract List<FlickrDeWestArea> getAreasByPoint(double x, double y, Radius radius);

	/**
	 * Returns a List of FlickrDeWestArea instances which interact to this rectangle
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return List<FlickrDeWestArea>
	 */
	public abstract List<FlickrDeWestArea> getAreasByRect(double x1, double y1, double x2, double y2, Radius radius);

	/**
	 * get the total amount of photos uploaded within this area
	 * @param areaid
	 * @return int - number of photos
	 */
	public abstract int getTotalCount(int areaid, Radius radius);


	/**
	 * Returns a List of FlickrDeWestPhoto instances within the area
	 * @param areaid
	 * @param radius
	 * @param queryStrs
	 * @param num - numbers of result
	 * @return
	 */
	public abstract List<FlickrDeWestPhoto> getPhotos(int areaid, Radius radius, SortedSet<String> queryStrs, int num);



	protected static String judgeOracleDatePatternStr(QueryLevel queryLevel){
		String oracleDatePatternStr = null;

		switch (queryLevel) {
		case YEAR:
			oracleDatePatternStr = FlickrDeWestAreaDao.oracleYearPatternStr;
			break;
		case MONTH:
			oracleDatePatternStr = FlickrDeWestAreaDao.oracleMonthPatternStr;
			break;
		case DAY:
			oracleDatePatternStr = FlickrDeWestAreaDao.oracleDayPatternStr;
			break;
		case HOUR:
			oracleDatePatternStr = FlickrDeWestAreaDao.oracleHourPatternStr;
			break;
		}
		return oracleDatePatternStr;
	}

	protected static QueryLevel judgeQueryLevel(String QueryStr){
		if(QueryStr.matches(FlickrDeWestAreaDao.hourRegExPattern.pattern().split(":")[0])){
			return QueryLevel.HOUR;
		}else if(QueryStr.matches(FlickrDeWestAreaDao.dayRegExPattern.pattern().split(":")[0])){
			return QueryLevel.DAY;
		}else if(QueryStr.matches(FlickrDeWestAreaDao.monthRegExPattern.pattern().split(":")[0])){
			return QueryLevel.MONTH;
		}else if(QueryStr.matches(FlickrDeWestAreaDao.yearRegExPattern.pattern().split(":")[0])){
			return QueryLevel.YEAR;
		}else{
			return null;
		}

	}
}