package de.fraunhofer.iais.spatial.dto;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import com.google.common.collect.Sets;

public class AreaDto implements Serializable {

	private String ModelNamel;
	private SortedSet<String> years = Sets.newTreeSet();
	private SortedSet<String> months = Sets.newTreeSet();
	private SortedSet<String> days = Sets.newTreeSet();
	private SortedSet<String> hours = Sets.newTreeSet();
	private SortedSet<String> minutes = Sets.newTreeSet();
	private SortedSet<String> weekdays = Sets.newTreeSet();
	private Set<String> queryStrs = Sets.newTreeSet();
	private long areaid;
	private String radius;
	private Point2D center;
	private Rectangle2D boundaryRect;
	private Point2D transfromVector;
	private Level queryLevel;
	private int minuteInterval;

	public enum Level {
		MINUTE, HOUR, DAY, MONTH, YEAR, WEEKDAY;
	}

	public int getQueryStrsLength() {
		int queryStrsLength = 0;
		switch (queryLevel) {
		case MINUTE:
			queryStrsLength = "2010-01-02@10:10".length();
			break;
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

	public String getModelNamel() {
		return ModelNamel;
	}

	public void setModelNamel(String modelNamel) {
		ModelNamel = modelNamel;
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

	public SortedSet<String> getMinutes() {
		return minutes;
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

	public String getRadius() {
		return radius;
	}

	public void setRadius(String radius) {
		this.radius = radius;
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

	public Point2D getTransfromVector() {
		return transfromVector;
	}

	public void setTransfromVector(Point2D transfromVector) {
		this.transfromVector = transfromVector;
	}

	public int getMinuteInterval() {
		return minuteInterval;
	}

	public void setMinuteInterval(int minuteInterval) {
		this.minuteInterval = minuteInterval;
	}

}
