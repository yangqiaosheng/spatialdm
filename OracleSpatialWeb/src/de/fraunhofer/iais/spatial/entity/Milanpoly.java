package de.fraunhofer.iais.spatial.entity;

import oracle.spatial.geometry.JGeometry;

public class Milanpoly {

	private int id;
	private String name;
	private String clustering;
	private JGeometry geom;
	private float area;

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

	public String getClustering() {
		return clustering;
	}

	public void setClustering(String clustering) {
		this.clustering = clustering;
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

}
