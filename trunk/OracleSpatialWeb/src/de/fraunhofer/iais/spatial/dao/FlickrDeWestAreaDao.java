package de.fraunhofer.iais.spatial.dao;

import java.util.List;

import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea.Radius;

public interface FlickrDeWestAreaDao {
	
	/**
	 * Returns a List of all the Area instances
	 * @return List<FlickrDeWestArea>
	 */
	public abstract List<FlickrDeWestArea> getAllAreas(Radius radius);

	/**
	 * Returns the instance of Area related to that areaid 
	 * @param areaid
	 * @return FlickrDeWestArea
	 */
	public abstract FlickrDeWestArea getAreaById(int areaid, Radius radius);

	/**
	 * Returns a List of Area instances which interact to this point
	 * @param x
	 * @param y
	 * @return List<FlickrDeWestArea>
	 */
	public abstract List<FlickrDeWestArea> getAreasByPoint(double x, double y, Radius radius);

	/**
	 * Returns a List of Area instances which interact to this rectangle
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

}