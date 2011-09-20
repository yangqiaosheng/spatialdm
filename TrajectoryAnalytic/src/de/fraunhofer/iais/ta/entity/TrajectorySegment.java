package de.fraunhofer.iais.ta.entity;

import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class TrajectorySegment {

	private String trId;
	private int trN;
	private int speed;
	private Coordinate fromCoordinate;
	private Coordinate toCoordinate;
	private RenderFeature feature = new RenderFeature();

	public class RenderFeature {
		private float width;
		private float textureWidthRatio;
		private Polygon boundary;
		private Polygon textureBoundary;
		private List<Polygon> texture;

		public float getWidth() {
			return width;
		}

		public void setWidth(float width) {
			this.width = width;
		}

		public float getTextureWidthRatio() {
			return textureWidthRatio;
		}

		public void setTextureWidthRatio(float textureWidthRatio) {
			this.textureWidthRatio = textureWidthRatio;
		}

		public Polygon getBoundary() {
			return boundary;
		}

		public void setBoundary(Polygon boundary) {
			this.boundary = boundary;
		}

		public Polygon getTextureBoundary() {
			return textureBoundary;
		}

		public void setTextureBoundary(Polygon textureBoundary) {
			this.textureBoundary = textureBoundary;
		}

		public List<Polygon> getTexture() {
			return texture;
		}

		public void setTexture(List<Polygon> texture) {
			this.texture = texture;
		}
	}

	public TrajectorySegment(String trId, int trN, int speed, Coordinate fromCoordinate, Coordinate toCoordinate) {
		super();
		this.trId = trId;
		this.trN = trN;
		this.speed = speed;
		this.fromCoordinate = fromCoordinate;
		this.toCoordinate = toCoordinate;
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

	public RenderFeature getFeature() {
		return feature;
	}

	public void setFeature(RenderFeature feature) {
		this.feature = feature;
	}

	public double getLength(){
		return toCoordinate.distance(fromCoordinate);

	}
}
