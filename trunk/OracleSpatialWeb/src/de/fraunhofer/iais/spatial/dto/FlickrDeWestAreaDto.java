package de.fraunhofer.iais.spatial.dto;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea.Radius;

public class FlickrDeWestAreaDto {
	private Set<String> years = new TreeSet<String>();
	private Set<String> months = new TreeSet<String>();
	private Set<String> days = new TreeSet<String>();
	private Set<String> hours = new TreeSet<String>();
	private Set<String> weekdays = new TreeSet<String>();
	private Set<String> queryStrs;
	private QueryLevel queryLevel;
	private Radius radius;
	private int zoom;
	private Point2D center;
	private Rectangle2D boundaryRect;
	private List<Point2D> polygon;
	private Date beginDate;
	private Date endDate;
	private Set<Date> selectedDays;
	private Set<String> years4Chart;

	public enum QueryLevel {
		HOUR, DAY, MONTH, YEAR;
	}

	public Set<String> getYears() {
		return years;
	}

	public void setYears(Set<String> years) {
		this.years = years;
	}

	public Set<String> getMonths() {
		return months;
	}

	public void setMonths(Set<String> months) {
		this.months = months;
	}

	public Set<String> getDays() {
		return days;
	}

	public void setDays(Set<String> days) {
		this.days = days;
	}

	public Set<String> getHours() {
		return hours;
	}

	public void setHours(Set<String> hours) {
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

	public List<Point2D> getPolygon() {
		return polygon;
	}

	public void setPolygon(List<Point2D> polygon) {
		this.polygon = polygon;
	}

	public Date getBeginDate() {
		return beginDate;
	}

	public void setBeginDate(Date beginDate) {
		this.beginDate = beginDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public Set<Date> getSelectedDays() {
		return selectedDays;
	}

	public void setSelectedDays(Set<Date> selectedDays) {
		this.selectedDays = selectedDays;
	}

	public Set<String> getYears4Chart() {
		return years4Chart;
	}

	public void setYears4Chart(Set<String> years4Chart) {
		this.years4Chart = years4Chart;
	}
}
