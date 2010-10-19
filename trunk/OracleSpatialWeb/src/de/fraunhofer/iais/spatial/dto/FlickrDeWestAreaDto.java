package de.fraunhofer.iais.spatial.dto;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import oracle.spatial.geometry.JGeometry;

import de.fraunhofer.iais.spatial.entity.FlickrDeWestArea.Radius;

public class FlickrDeWestAreaDto {

	private SortedSet<String> years = new TreeSet<String>();
	private SortedSet<String> months = new TreeSet<String>();
	private SortedSet<String> days = new TreeSet<String>();
	private SortedSet<String> hours = new TreeSet<String>();
	private SortedSet<String> weekdays = new TreeSet<String>();
	private SortedSet<String> queryStrs;
	private QueryLevel queryLevel;
	private int areaid;
	private Radius radius;
	private int zoom;
	private Point2D center;
	private Rectangle2D boundaryRect;
	private List<Point2D> polygon;
	private Date beginDate;
	private Date endDate;
	private SortedSet<Date> selectedDays;
	private SortedSet<String> years4Chart;
	JGeometry queryGeom;

	public enum QueryLevel {
		HOUR, DAY, MONTH, YEAR;
	}

	public SortedSet<String> getYears() {
		return years;
	}

	public void setYears(SortedSet<String> years) {
		this.years = years;
	}

	public SortedSet<String> getMonths() {
		return months;
	}

	public void setMonths(SortedSet<String> months) {
		this.months = months;
	}

	public SortedSet<String> getDays() {
		return days;
	}

	public void setDays(SortedSet<String> days) {
		this.days = days;
	}

	public SortedSet<String> getHours() {
		return hours;
	}

	public void setHours(SortedSet<String> hours) {
		this.hours = hours;
	}

	public SortedSet<String> getWeekdays() {
		return weekdays;
	}

	public void setWeekdays(SortedSet<String> weekdays) {
		this.weekdays = weekdays;
	}

	public int getAreaid() {
		return areaid;
	}

	public void setAreaid(int areaid) {
		this.areaid = areaid;
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

	public SortedSet<String> getQueryStrs() {
		return queryStrs;
	}

	public void setQueryStrs(SortedSet<String> queryStrs) {
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

	public SortedSet<Date> getSelectedDays() {
		return selectedDays;
	}

	public void setSelectedDays(SortedSet<Date> selectedDays) {
		this.selectedDays = selectedDays;
	}

	public SortedSet<String> getYears4Chart() {
		return years4Chart;
	}

	public void setYears4Chart(SortedSet<String> years4Chart) {
		this.years4Chart = years4Chart;
	}

	public JGeometry getQueryGeom() {
		return queryGeom;
	}

	public void setQueryGeom(JGeometry queryGeom) {
		this.queryGeom = queryGeom;
	}

}
