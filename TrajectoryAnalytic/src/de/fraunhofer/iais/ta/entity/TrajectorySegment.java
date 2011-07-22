package de.fraunhofer.iais.ta.entity;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.Point;

public class TrajectorySegment {

	private String trId;
	private int trN;
	private int speed;
	private Coordinate fromCoordinate;
	private Coordinate toCoordinate;
	private GeometryCollection geom;
	private double length;

	public TrajectorySegment(String trId, int trN, int speed, Coordinate fromCoordinate, Coordinate toCoordinate, double lenght) {
		super();
		this.trId = trId;
		this.trN = trN;
		this.speed = speed;
		this.fromCoordinate = fromCoordinate;
		this.toCoordinate = toCoordinate;
		this.length = lenght;
	}
	public String getTrId() {
		return trId;
	}
	public void setTrId(String trId) {
		this.trId = trId;
	}
	public int getTrN() {
		return trN;
	}
	public void setTrN(int trN) {
		this.trN = trN;
	}
	public int getSpeed() {
		return speed;
	}
	public void setSpeed(int speed) {
		this.speed = speed;
	}
	public Coordinate getFromCoordinate() {
		return fromCoordinate;
	}
	public void setFromCoordinate(Coordinate fromCoordinate) {
		this.fromCoordinate = fromCoordinate;
	}
	public Coordinate getToCoordinate() {
		return toCoordinate;
	}
	public void setToCoordinate(Coordinate toCoordinate) {
		this.toCoordinate = toCoordinate;
	}
	public GeometryCollection getGeom() {
		return geom;
	}
	public void setGeom(GeometryCollection geom) {
		this.geom = geom;
	}
	public double getLength() {
		return length;
	}
	public void setLength(double lenght) {
		this.length = lenght;
	}

}
