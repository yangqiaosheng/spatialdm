package de.fraunhofer.iais.ta.test;

import java.awt.Color;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.math.NumberUtils;
import org.geotools.geometry.jts.JTS;
import org.jaitools.swing.JTSFrame;
import org.junit.Test;

import au.com.bytecode.opencsv.CSVReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.buffer.BufferBuilder;

import de.fraunhofer.iais.ta.geometry.ArrowGeometryCalculator;

public class GeometryCalculatorTest {

	@Test
	public void arrowTest() {
		Polygon arrowPolygon = new ArrowGeometryCalculator().arrow(new Coordinate(1, 3), new Coordinate(25, 10), 3f);
		Polygon arrowPolygon2 = new ArrowGeometryCalculator().arrow(new Coordinate(1, 3), new Coordinate(25, 10), 3f, 0.1f, 0.2f);
		System.out.println(arrowPolygon.toText());
		System.out.println(arrowPolygon2.toText());

		JTSFrame jtsFrame = new JTSFrame("JTS Frame");
		jtsFrame.addGeometry(arrowPolygon2, Color.BLACK);
		jtsFrame.setVisible(true);
	}

	@Test
	public void bufferTest() {
		Coordinate[] coordinates = new Coordinate[] { new Coordinate(10, 20),  new Coordinate(10, 30),  new Coordinate(20, 30)};
		GeometryFactory geometryFactory = new GeometryFactory();
		LineString path = geometryFactory.createLineString(coordinates);
		System.out.println(path.toText());

//		BufferBuilder bufferBuilder = new BufferBuilder(BufferParameters);
		JTSFrame jtsFrame = new JTSFrame("JTS Frame");
		jtsFrame.addGeometry(path, Color.BLACK);
		jtsFrame.setVisible(true);
	}

	@Test
	public void csvTest2() throws IOException {
		CSVReader reader = new CSVReader(new FileReader("data/single_trajectory.csv"));
		String[] nextLine;

		List<Polygon> polygons = new ArrayList<Polygon>();

		Coordinate preCoordinate = null;
		reader.readNext();
		while ((nextLine = reader.readNext()) != null) {
			Coordinate nowCoordinate = new Coordinate(NumberUtils.toDouble(nextLine[3]), NumberUtils.toDouble(nextLine[4])); ;
			if(preCoordinate != null) {
				Polygon arrowPolygon = new ArrowGeometryCalculator().arrow(preCoordinate, nowCoordinate, 0.001f, 0.1f, 0.1f);
				polygons.add(arrowPolygon);
				System.out.println(arrowPolygon.toText());
			}
			preCoordinate = nowCoordinate;
		}

		Geometry[] geometries = new Geometry[polygons.size()];
		polygons.toArray(geometries);
		GeometryCollection polygonsCollection = new GeometryCollection(geometries, polygons.get(0).getFactory());
		System.out.println(polygonsCollection.toText());
	}
}
