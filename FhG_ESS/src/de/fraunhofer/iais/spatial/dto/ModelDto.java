package de.fraunhofer.iais.spatial.dto;

import java.awt.geom.Point2D;

public class ModelDto {

	String modelNamel;
	String modelType;
	String title;
	String call;
	String valid;
	private Point2D position;
	private Point2D transfromVector;

	enum ModelType {
		AGGREGATION, PREDITION
	}

	public String getModelNamel() {
		return modelNamel;
	}

	public void setModelNamel(String modelNamel) {
		this.modelNamel = modelNamel;
	}

	public String getModelType() {
		return modelType;
	}

	public void setModelType(String modelType) {
		this.modelType = modelType;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getCall() {
		return call;
	}

	public void setCall(String call) {
		this.call = call;
	}

	public String getValid() {
		return valid;
	}

	public void setValid(String valid) {
		this.valid = valid;
	}

	public Point2D getPosition() {
		return position;
	}

	public void setPosition(Point2D position) {
		this.position = position;
	}

	public Point2D getTransfromVector() {
		return transfromVector;
	}

	public void setTransfromVector(Point2D transfromVector) {
		this.transfromVector = transfromVector;
	}

}
