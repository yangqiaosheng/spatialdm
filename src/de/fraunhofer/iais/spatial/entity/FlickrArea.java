package de.fraunhofer.iais.spatial.entity;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import com.google.common.collect.Maps;

/**
 * Entity mapped to the table FLICKR_DE_WEST_TABLE_RADIUS and FLICKR_DE_WEST_TABLE_COUNT
 * @author zhi
 */

public class FlickrArea implements Serializable {

	private static final long serialVersionUID = -5736041303318937777L;

	private long id;
	private Radius radius;
	private String name;
	private double area;
	private Point2D center;
	private List<Point2D> geom;
	private boolean isCached;

	private long totalCount;
	private SortedMap<String, Integer> yearsCount = Maps.newTreeMap();
	private SortedMap<String, Integer> monthsCount = Maps.newTreeMap();
	private SortedMap<String, Integer> daysCount = Maps.newTreeMap();
	private SortedMap<String, Integer> hoursCount = Maps.newTreeMap();
	private SortedMap<String, Map<String, Integer>> hoursTagsCount = Maps.newTreeMap();
	private SortedMap<String, Map<String, Integer>> daysTagsCount = Maps.newTreeMap();

	public enum Radius {
		R375, R750, R1250, R2500, R5000, R10000, R20000, R40000, R80000, R160000, R320000;

		@Override
		public String toString() {
			return this.name().substring(1);
		}
	}

	public long getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Point2D> getGeom() {
		return geom;
	}

	public void setGeom(List<Point2D> geom) {
		this.geom = geom;
	}

	public double getArea() {
		return area;
	}

	public void setArea(double area) {
		this.area = area;
	}

	public Point2D getCenter() {
		return center;
	}

	public void setCenter(Point2D center) {
		this.center = center;
	}

	public long getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(long totalCount) {
		this.totalCount = totalCount;
	}

	public boolean isCached() {
		return isCached;
	}

	public void setCached(boolean isCached) {
		this.isCached = isCached;
	}

	public SortedMap<String, Integer> getYearsCount() {
		return yearsCount;
	}

	public SortedMap<String, Integer> getMonthsCount() {
		return monthsCount;
	}

	public SortedMap<String, Integer> getDaysCount() {
		return daysCount;
	}

	public SortedMap<String, Integer> getHoursCount() {
		return hoursCount;
	}

	public Radius getRadius() {
		return radius;
	}

	public void setRadius(Radius radius) {
		this.radius = radius;
	}

	public SortedMap<String, Map<String, Integer>> getHoursTagsCount() {
		return hoursTagsCount;
	}

	public SortedMap<String, Map<String, Integer>> getDaysTagsCount() {
		return daysTagsCount;
	}

}
