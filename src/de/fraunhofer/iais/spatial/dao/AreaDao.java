package de.fraunhofer.iais.spatial.dao;

import java.util.List;

import de.fraunhofer.iais.spatial.entity.Area;

public interface AreaDao {

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
	public abstract Area getAreaById(int areaid);

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

	/**
	 * count the amount of photos uploaded by this person within this area
	 * @param areaid
	 * @param person
	 * @return int - number of photos
	 */
	public abstract int getPersonCount(int areaid, String person);

	/**
	 * get the total amount of photos uploaded within this area
	 * @param areaid
	 * @return int - number of photos
	 */
	public abstract int getTotalCount(int areaid);

}