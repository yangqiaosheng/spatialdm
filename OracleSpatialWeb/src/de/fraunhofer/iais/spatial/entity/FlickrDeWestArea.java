package de.fraunhofer.iais.spatial.entity;

import java.awt.geom.Point2D;
import java.util.Map;

import oracle.spatial.geometry.JGeometry;

public class FlickrDeWestArea {
	private int id;
	private Radius radius; 
	private String name;
	private JGeometry geom;
	private float area;
	private Point2D center;

	private Map<String, Integer> yearsCount;
	private Map<String, Integer> monthsCount;
	private Map<String, Integer> daysCount;
	private Map<String, Integer> hoursCount;
	private int totalCount;
	private int selectCount;
	
	public enum Radius {
	    _5000, _10000, _20000, _40000, _80000;

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

	public void setYearsCount(Map<String, Integer> yearsCount) {
		this.yearsCount = yearsCount;
	}

	public Map<String, Integer> getMonthsCount() {
		return monthsCount;
	}

	public void setMonthsCount(Map<String, Integer> monthsCount) {
		this.monthsCount = monthsCount;
	}

	public Map<String, Integer> getDaysCount() {
		return daysCount;
	}

	public void setDaysCount(Map<String, Integer> daysCount) {
		this.daysCount = daysCount;
	}

	public Map<String, Integer> getHoursCount() {
		return hoursCount;
	}

	public void setHoursCount(Map<String, Integer> hoursCount) {
		this.hoursCount = hoursCount;
	}

	public int getSelectCount() {
		return selectCount;
	}

	public void setSelectCount(int selectCount) {
		this.selectCount = selectCount;
	}

	public Radius getRadius() {
		return radius;
	}

	public void setRadius(Radius radius) {
		this.radius = radius;
	}

}
