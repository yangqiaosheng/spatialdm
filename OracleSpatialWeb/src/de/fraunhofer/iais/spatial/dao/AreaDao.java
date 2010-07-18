package de.fraunhofer.iais.spatial.dao;

import java.util.List;
import java.util.Set;

import de.fraunhofer.iais.spatial.entity.Area;

public interface AreaDao {

	/**
	 * count the amount of photos uploaded by this person within this area
	 * @param areaid
	 * @param person
	 * @return int - number of photos
	 */
	public abstract int getPersonCount(String areaid, String person);

	/**
	 * count the amount of photos uploaded during this hour within this area
	 * @param areaid
	 * @param hour
	 * @return int - number of photos
	 */
	public abstract int getHourCount(String areaid, String hour);

	/**
	 * count the amount of photos uploaded during these hours within this area
	 * @param areaid
	 * @param hours
	 * @return int - number of photos
	 */
	public abstract int getHourCount(String areaid, Set<String> hours);

	/**
	 * count the amount of photos uploaded during this day within this area
	 * @param areaid
	 * @param day
	 * @return int - number of photos
	 */
	public abstract int getDayCount(String areaid, String day);

	/**
	 * count the amount of photos uploaded during these days within this area
	 * @param areaid
	 * @param days
	 * @return int - number of photos
	 */
	public abstract int getDayCount(String areaid, Set<String> days);

	/**
	 * count the amount of photos uploaded during this month within this area
	 * @param areaid
	 * @param month
	 * @return int - number of photos
	 */
	public abstract int getMonthCount(String areaid, String month);

	/**
	 * count the amount of photos uploaded during these months within this area
	 * @param areaid
	 * @param months
	 * @return int - number of photos
	 */
	public abstract int getMonthCount(String areaid, Set<String> months);

	/**
	 * get the amount of photos uploaded during this year within this area
	 * @param areaid
	 * @param year
	 * @return int - number of photos
	 */
	public abstract int getYearCount(String areaid, String year);

	/**
	 * get the amount of photos uploaded during these years within this area
	 * @param areaid
	 * @param years
	 * @return int - number of photos
	 */
	public abstract int getYearCount(String areaid, Set<String> years);

	/**
	 * get the total amount of photos uploaded within this area
	 * @param areaid
	 * @return int - number of photos
	 */
	public abstract int getTotalCount(String areaid);

	/**
	 * Returns a List of all the Area instances
	 * @return List<Area>
	 */
	public abstract List<Area> getAllAreas();

	/**
	 * Returns the instance of Area related to that areaid 
	 * @param areaid
	 * @return Area
	 */
	public abstract Area getAreaById(String areaid);

	/**
	 * Returns a List of Area instances which interact to this point
	 * @param x
	 * @param y
	 * @return List<Area>
	 */
	public abstract List<Area> getAreasByPoint(double x, double y);

	/**
	 * Returns a List of Area instances which interact to this rectangle
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return List<Area>
	 */
	public abstract List<Area> getAreasByRect(double x1, double y1, double x2, double y2);

}