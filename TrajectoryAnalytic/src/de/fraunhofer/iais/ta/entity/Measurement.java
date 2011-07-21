package de.fraunhofer.iais.ta.entity;

import java.util.Calendar;
import java.util.Date;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

public class Measurement {

	private String trId;
	private int trN;
	private int pIdx;
	private Date time;
	private Coordinate coordinate;

	public Measurement(String trId, int trN, int pIdx, Date time, Coordinate coordinate) {
		super();
		this.trId = trId;
		this.trN = trN;
		this.pIdx = pIdx;
		this.time = time;
		this.coordinate = coordinate;
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

	public int getpIdx() {
		return pIdx;
	}

	public void setpIdx(int pIdx) {
		this.pIdx = pIdx;
	}


	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public Coordinate getCoordinate() {
		return coordinate;
	}

	public void setCoordinate(Coordinate coordinate) {
		this.coordinate = coordinate;
	}
}
