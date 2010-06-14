package de.fraunhofer.iais.spatial.entity;

import java.awt.geom.Point2D;

import oracle.spatial.geometry.JGeometry;

/**
 * Entity mapped to the table AREAS20KMRADIUS and AREAS20KMRADIUS_COUNT
 * @author zhi
 */
public class Area {

	private int count;
	private int totalCount;
	private String id;
	private String name;
	private JGeometry geom;
	private float area;
	private Point2D center;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public Point2D getCenter() {
		return center;
	}

	public void setCenter(Point2D center) {
		this.center = center;
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

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}

}
