package de.fraunhofer.iais.spatial.entity;

import java.awt.geom.Point2D;
import java.util.Map;
import java.util.TreeMap;

import oracle.spatial.geometry.JGeometry;

/**
 * Entity mapped to the table FLICKR_DE_WEST_TABLE_RADIUS and FLICKR_DE_WEST_TABLE_COUNT
 * @author zhi
 */

public class FlickrArea {
	private int id;
	private Radius radius;
	private String name;
	private JGeometry geom;
	private float area;
	private Point2D center;

	private Map<String, Integer> yearsCount = new TreeMap<String, Integer>();
	private Map<String, Integer> monthsCount = new TreeMap<String, Integer>();
	private Map<String, Integer> daysCount = new TreeMap<String, Integer>();
	private Map<String, Integer> hoursCount = new TreeMap<String, Integer>();
	private int totalCount;
	private int selectedCount;

	public enum Radius {
		R5000, R10000, R20000, R40000, R80000, R160000, R320000;

		@Override
		public String toString() {
			return this.name().substring(1);
		}

	}

	public int getId() {
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

	public JGeometry getGeom() {
		return geom;
	}

	public void setGeom(JGeometry geom) {
		this.geom = geom;
	}

	public float getArea() {
		return area;
	}

	public void setArea(float area) {
		this.area = area;
	}

	public Point2D getCenter() {
		return center;
	}

	public void setCenter(Point2D center) {
		this.center = center;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}

	public Map<String, Integer> getYearsCount() {
		return yearsCount;
	}

	public Map<String, Integer> getMonthsCount() {
		return monthsCount;
	}

	public Map<String, Integer> getDaysCount() {
		return daysCount;
	}

	public Map<String, Integer> getHoursCount() {
		return hoursCount;
	}

	public int getSelectCount() {
		return selectedCount;
	}

	public void setSelectedCount(int selectCount) {
		this.selectedCount = selectCount;
	}

	public Radius getRadius() {
		return radius;
	}

	public void setRadius(Radius radius) {
		this.radius = radius;
	}

}
