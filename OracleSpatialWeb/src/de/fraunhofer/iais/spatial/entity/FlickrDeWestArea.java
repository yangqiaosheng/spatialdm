package de.fraunhofer.iais.spatial.entity;

import java.awt.geom.Point2D;

import oracle.spatial.geometry.JGeometry;

public class FlickrDeWestArea {
	private String id;
	private String name;
	private JGeometry geom;
	private float area;
	private Point2D center;

	private int totalCount;
	
	public enum Radius {
	    _5000, _10000, _20000, _40000, _80000
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
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

}
