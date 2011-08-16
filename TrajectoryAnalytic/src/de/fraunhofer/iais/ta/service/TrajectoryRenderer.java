package de.fraunhofer.iais.ta.service;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

import de.fraunhofer.iais.ta.entity.TrajectorySegment;
import de.fraunhofer.iais.ta.geometry.FeatureGeometryCalculator;

public class TrajectoryRenderer {

	private FeatureGeometryCalculator featureGeometryCalculator = new FeatureGeometryCalculator();

	public List<Polygon> renderTrajectorySegment(TrajectorySegment segment){
		float width = 1;
		List<Polygon> polygons = new ArrayList<Polygon>();

		Polygon boundary = featureGeometryCalculator.boundary(segment.getFromCoordinate(), segment.getToCoordinate(), width);
		polygons.add(boundary);

		return polygons;
	}
}
