package de.fraunhofer.iais.spatial.dao;

import java.util.List;
import java.util.Set;

import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestPhoto;
import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea.Radius;

public interface FlickrDeWestAreaDao {

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
	 * Returns a FlickrDeWestPhoto instance within the area
	 * @param areaid
	 * @param radius
	 * @param hour
	 * @param idx - index number of the result
	 * @return
	 */
	public abstract FlickrDeWestPhoto getPhoto(int areaid, Radius radius, String hour, int idx);

	/** 
	 * Returns a List of FlickrDeWestPhoto instances within the area
	 * @param areaid
	 * @param radius
	 * @param hours
	 * @param num - numbers of result
	 * @return
	 */
	public abstract List<FlickrDeWestPhoto> getPhotos(int areaid, Radius radius, Set<String> hours, int num);

	/**
	 * Returns a List of FlickrDeWestPhoto instances within the area
	 * @param areaid
	 * @param radius
	 * @param hour
	 * @param num - numbers of result
	 * @return
	 */
	public abstract List<FlickrDeWestPhoto> getPhotos(int areaid, Radius radius, String hour, int num);

}