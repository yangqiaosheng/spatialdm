package de.fraunhofer.iais.spatial.dto;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import oracle.spatial.geometry.JGeometry;

import com.google.common.collect.Sets;

import de.fraunhofer.iais.spatial.entity.FlickrArea.Radius;

public class FlickrEuropeAreaDto {

	private SortedSet<String> years = Sets.newTreeSet();
	private SortedSet<String> months = Sets.newTreeSet();
	private SortedSet<String> days = Sets.newTreeSet();
	private SortedSet<String> hours = Sets.newTreeSet();
	private SortedSet<String> weekdays = Sets.newTreeSet();
	private SortedSet<String> queryStrs = Sets.newTreeSet();
	private Level queryLevel;
	private long areaid;
	private Radius radius;
	private int zoom;
	private Point2D center;
	private Rectangle2D boundaryRect;
	private List<Point2D> polygon;
	private Date beginDate;
	private Date endDate;
	JGeometry oracleQueryGeom;
	String pgQueryGeom;

	public enum Level {
		HOUR, DAY, MONTH, YEAR, WEEKDAY;
	}

	public SortedSet<String> getYears() {
		return years;
	}

	public SortedSet<String> getMonths() {
		return months;
	}

	public SortedSet<String> getDays() {
		return days;
	}

	public SortedSet<String> getHours() {
		return hours;
	}

	public SortedSet<String> getWeekdays() {
		return weekdays;
	}

	public long getAreaid() {
		return areaid;
	}

	public void setAreaid(long areaid) {
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

	public Level getQueryLevel() {
		return queryLevel;
	}

	public void setQueryLevel(Level queryLevel) {
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

	public JGeometry getOracleQueryGeom() {
		return oracleQueryGeom;
	}

	public void setOracleQueryGeom(JGeometry queryGeom) {
		this.oracleQueryGeom = queryGeom;
	}

	public String getPgQueryGeom() {
		return pgQueryGeom;
	}

	public void setPgQueryGeom(String pgQueryGeom) {
		this.pgQueryGeom = pgQueryGeom;
	}

}
