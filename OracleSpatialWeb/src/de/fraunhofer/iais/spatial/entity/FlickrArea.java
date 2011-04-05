package de.fraunhofer.iais.spatial.entity;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import oracle.spatial.geometry.JGeometry;

/**
 * Entity mapped to the table FLICKR_DE_WEST_TABLE_RADIUS and FLICKR_DE_WEST_TABLE_COUNT
 * @author zhi
 */

public class FlickrArea {
	private long id;
	private Radius radius;
	private String name;
	private float area;
	private Point2D center;
	private List<Point2D> geom;

	private Map<String, Integer> yearsCount = new HashMap<String, Integer>();
	private Map<String, Integer> monthsCount = new HashMap<String, Integer>();
	private Map<String, Integer> daysCount = new HashMap<String, Integer>();
	private Map<String, Integer> hoursCount = new HashMap<String, Integer>();
	private transient long totalCount;
	private transient long selectedCount;
	private transient ChartData chartsData = new ChartData();

	public enum Radius {
		R5000, R10000, R20000, R40000, R80000, R160000, R320000;

		@Override
		public String toString() {
			return this.name().substring(1);
		}
	}

	public class ChartData{
		private Map<Integer, Integer> years = new TreeMap<Integer, Integer>();
		private Map<Integer, Integer> months = new TreeMap<Integer, Integer>();
		private Map<Integer, Integer> days = new TreeMap<Integer, Integer>();
		private Map<Integer, Integer> hours = new TreeMap<Integer, Integer>();
		private Map<Integer, Integer> weekdays = new TreeMap<Integer, Integer>();

		public Map<Integer, Integer> getYears() {
			return years;
		}
		public Map<Integer, Integer> getMonths() {
			return months;
		}
		public Map<Integer, Integer> getDays() {
			return days;
		}
		public Map<Integer, Integer> getHours() {
			return hours;
		}
		public Map<Integer, Integer> getWeekdays() {
			return weekdays;
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

	public long getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(long totalCount) {
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

	public long getSelectCount() {
		return selectedCount;
	}

	public void setSelectedCount(long selectCount) {
		this.selectedCount = selectCount;
	}

	public Radius getRadius() {
		return radius;
	}

	public void setRadius(Radius radius) {
		this.radius = radius;
	}

	public ChartData getChartsData() {
		return chartsData;
	}

}
