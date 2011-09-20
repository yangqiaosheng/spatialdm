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

import com.google.common.collect.Lists;
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

		TrajectoryRenderer render = new TrajectoryRenderer();
		TrajectorySegment segment1 = new TrajectorySegment("d", 1, 30, new Coordinate(10, 20), new Coordinate(10, 30));
		render.renderTrajectorySegment(segment1);
		TrajectorySegment segment2 = new TrajectorySegment("d", 1, 200, new Coordinate(10, 30), new Coordinate(20, 30));
		render.renderTrajectorySegment(segment2);
		TrajectorySegment segment3 = new TrajectorySegment("d", 1, 100, new Coordinate(20, 30), new Coordinate(50, 10));
		render.renderTrajectorySegment(segment3);
		TrajectorySegment segment4 = new TrajectorySegment("d", 1, 150, new Coordinate(50, 10), new Coordinate(20, 50));
		render.renderTrajectorySegment(segment4);

		List<TrajectorySegment> segments = Lists.newArrayList();
		segments.add(segment1);
		segments.add(segment2);
		segments.add(segment3);
		segments.add(segment4);

		render.joinTrajectorySegments(segments);

//		jtsFrame.addGeometry(segment.getFeature().getBoundary(), Color.BLACK);
		for(Polygon peak : segment1.getFeature().getTexture()){
			jtsFrame.addGeometry(peak, Color.GREEN);
		}


//		jtsFrame.addGeometry(segment2.getFeature().getBoundary(), Color.BLACK);
		for(Polygon peak : segment2.getFeature().getTexture()){
			jtsFrame.addGeometry(peak, Color.GREEN);
		}

//		jtsFrame.addGeometry(segment3.getFeature().getBoundary(), Color.BLACK);
		for(Polygon peak : segment3.getFeature().getTexture()){
			jtsFrame.addGeometry(peak, Color.GREEN);
		}


//		jtsFrame.addGeometry(segment4.getFeature().getBoundary(), Color.BLACK);
		for(Polygon peak : segment4.getFeature().getTexture()){
			jtsFrame.addGeometry(peak, Color.GREEN);
		}
		jtsFrame.addGeometry(segment4.getFeature().getBoundary().union(segment3.getFeature().getBoundary()).union(segment2.getFeature().getBoundary()).union(segment1.getFeature().getBoundary()), Color.BLACK);

		jtsFrame.setVisible(true);
		jtsFrame.setSize(400, 300);
	}
}
