package de.fraunhofer.iais.spatial.dto;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import oracle.spatial.geometry.JGeometry;

import com.google.common.collect.Sets;

import de.fraunhofer.iais.spatial.entity.FlickrArea.Radius;

/**
 * Data Transfer Object (Value Object) which stores all the parameters from the servelts' request
 * @author <a href="mailto:haolin.zhi@iais.fraunhofer.de">Haolin Zhi</A>
 * @author <a href="mailto:iulian.peca@iais.fraunhofer.de">Iulian Peca</a>
 *
 */
public class FlickrAreaDto implements Serializable {

	private SortedSet<String> years = Sets.newTreeSet();
	private SortedSet<String> months = Sets.newTreeSet();
	private SortedSet<String> days = Sets.newTreeSet();
	private SortedSet<String> hours = Sets.newTreeSet();
	private SortedSet<String> weekdays = Sets.newTreeSet();
	private Set<String> queryStrs = Sets.newTreeSet();
	private Level queryLevel;
	private long areaid;
	private Radius radius;
	private int zoom;
	private Point2D center;
	private Rectangle2D boundaryRect;
	private boolean crossDateLine;
	private List<Point2D> polygon;
	private Date beginDate;
	private Date endDate;
	JGeometry oracleQueryGeom;
	String pgQueryGeom;
	private Point2D transfromVector;
	boolean withoutStopWords = true;

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

	public boolean isCrossDateLine() {
		return crossDateLine;
	}

	public void setCrossDateLine(boolean crossDateLine) {
		this.crossDateLine = crossDateLine;
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

	public int getQueryStrsLength() {
		int queryStrsLength = 0;
		switch (queryLevel) {
		case HOUR:
			queryStrsLength = "2010-01-02@14".length();
			break;
		case DAY:
			queryStrsLength = "2010-01-02".length();
			break;
		case MONTH:
			queryStrsLength = "2010-01".length();
			break;
		case YEAR:
			queryStrsLength = "2010".length();
			break;
		}
		return queryStrsLength;
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

	public Point2D getTransfromVector() {
		return transfromVector;
	}

	public void setTransfromVector(Point2D transfromVector) {
		this.transfromVector = transfromVector;
	}

	public boolean isWithoutStopWords() {
		return withoutStopWords;
	}

	public void setWithoutStopWords(boolean withStopWords) {
		this.withoutStopWords = withStopWords;
	}

}
