package de.fraunhofer.iais.spatial.dto;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea.Radius;

public class FlickrDeWestAreaDto {
	private List<String> years = new ArrayList<String>();
	private List<String> months = new ArrayList<String>();
	private List<String> days = new ArrayList<String>();
	private List<String> hours = new ArrayList<String>();
	private Set<String> weekdays = new HashSet<String>();
	private Set<String> queryStrs = new HashSet<String>();
	private QueryLevel queryLevel;
	private Radius radius;
	private int zoom;
	private Point2D center;
	private Rectangle2D boundaryRect;
	
	public enum QueryLevel {
	    HOUR, DAY, MONTH, YEAR;
	}
	
	public List<String> getYears() {
		return years;
	}
	public void setYears(List<String> years) {
		this.years = years;
	}
	public List<String> getMonths() {
		return months;
	}
	public void setMonths(List<String> months) {
		this.months = months;
	}
	public List<String> getDays() {
		return days;
	}
	public void setDays(List<String> days) {
		this.days = days;
	}
	public List<String> getHours() {
		return hours;
	}
	public void setHours(List<String> hours) {
		this.hours = hours;
	}
	public Set<String> getWeekdays() {
		return weekdays;
	}
	public void setWeekdays(Set<String> weekdays) {
		this.weekdays = weekdays;
	}
	public Radius getRadius() {
		return radius;
	}
	public void setRadius(Radius radius) {
		this.radius = radius;
	}
	public int getZoom() {
		return zoom;
	}
	public void setZoom(int zoom) {
		this.zoom = zoom;
	}
	public Point2D getCenter() {
		return center;
	}
	public void setCenter(Point2D center) {
		this.center = center;
	}
	public Rectangle2D getBoundaryRect() {
		return boundaryRect;
	}
	public void setBoundaryRect(Rectangle2D boundaryRect) {
		this.boundaryRect = boundaryRect;
	}
	public Set<String> getQueryStrs() {
		return queryStrs;
	}
	public void setQueryStrs(Set<String> queryStrs) {
		this.queryStrs = queryStrs;
	}
	public QueryLevel getQueryLevel() {
		return queryLevel;
	}
	public void setQueryLevel(QueryLevel queryLevel) {
		this.queryLevel = queryLevel;
	}
}
