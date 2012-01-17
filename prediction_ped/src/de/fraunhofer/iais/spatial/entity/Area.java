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

public class Area implements Serializable {

	private static final long serialVersionUID = -5736041303318937777L;

	private long id;
	private String name;
	private double area;
	private Point2D center;
	private List<Point2D> geom;
	private boolean isCached;
	private long totalCount;

	private SortedMap<String, Integer> datesCount = Maps.newTreeMap();

	public long getId() {
		return id;
	}

	public void setId(long areaId) {
		this.id = areaId;
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

	public SortedMap<String, Integer> getDatesCount() {
		return datesCount;
	}

}
