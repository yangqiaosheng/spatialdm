package de.fraunhofer.iais.ta.test;

import java.awt.Color;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.math.NumberUtils;
import org.jaitools.swing.JTSFrame;
import org.junit.Test;

import au.com.bytecode.opencsv.CSVReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import de.fraunhofer.iais.ta.entity.TrajectorySegment;
import de.fraunhofer.iais.ta.geometry.ArrowGeometryCalculator;
import de.fraunhofer.iais.ta.geometry.FeatureGeometryCalculator;
import de.fraunhofer.iais.ta.service.TrajectoryRenderer;

public class TrajectorySegCalculatorTest {

	public static void main(String[] args) {
		JTSFrame jtsFrame = new JTSFrame("JTS Frame");

		GeometryFactory geometryFactory = new GeometryFactory();

		TrajectorySegment segment = new TrajectorySegment("d", 1, 30, new Coordinate(10, 20), new Coordinate(10, 30));
		TrajectoryRenderer render = new TrajectoryRenderer();
		render.renderTrajectorySegment(segment);
//		jtsFrame.addGeometry(segment.getFeature().getBoundary(), Color.BLACK);
		for(Polygon peak : segment.getFeature().getTexture()){
			jtsFrame.addGeometry(peak, Color.GREEN);
		}


		TrajectorySegment segment2 = new TrajectorySegment("d", 1, 200, new Coordinate(10, 30), new Coordinate(20, 30));
		render.renderTrajectorySegment(segment2);
//		jtsFrame.addGeometry(segment2.getFeature().getBoundary(), Color.BLACK);
		for(Polygon peak : segment2.getFeature().getTexture()){
			jtsFrame.addGeometry(peak, Color.GREEN);
		}

		TrajectorySegment segment3 = new TrajectorySegment("d", 1, 100, new Coordinate(20, 30), new Coordinate(50, 10));
		render.renderTrajectorySegment(segment3);
//		jtsFrame.addGeometry(segment3.getFeature().getBoundary(), Color.BLACK);
		for(Polygon peak : segment3.getFeature().getTexture()){
			jtsFrame.addGeometry(peak, Color.GREEN);
		}


		TrajectorySegment segment4 = new TrajectorySegment("d", 1, 150, new Coordinate(50, 10), new Coordinate(20, 50));
		render.renderTrajectorySegment(segment4);
//		jtsFrame.addGeometry(segment4.getFeature().getBoundary(), Color.BLACK);
		for(Polygon peak : segment4.getFeature().getTexture()){
			jtsFrame.addGeometry(peak, Color.GREEN);
		}
		jtsFrame.addGeometry(segment4.getFeature().getBoundary().union(segment3.getFeature().getBoundary()).union(segment2.getFeature().getBoundary()).union(segment.getFeature().getBoundary()), Color.BLACK);

		jtsFrame.setVisible(true);
		jtsFrame.setSize(400, 300);
	}
}
