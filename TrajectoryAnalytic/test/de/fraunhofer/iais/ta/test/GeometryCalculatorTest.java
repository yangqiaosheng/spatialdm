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

import de.fraunhofer.iais.ta.geometry.ArrowGeometryCalculator;
import de.fraunhofer.iais.ta.geometry.FeatureGeometryCalculator;

public class GeometryCalculatorTest {

	public static void main(String[] args) {
		Coordinate[] coordinates = new Coordinate[] { new Coordinate(10, 20),  new Coordinate(10, 30),  new Coordinate(20, 30)};
		GeometryFactory geometryFactory = new GeometryFactory();
		LineString path = geometryFactory.createLineString(coordinates);
		System.out.println(path.toText());

//		BufferBuilder bufferBuilder = new BufferBuilder(BufferParameters);
		JTSFrame jtsFrame = new JTSFrame("JTS Frame");
		Geometry buffer = path.buffer(1.5d);
		for (Coordinate coordinate : buffer.getCoordinates()){
			jtsFrame.addGeometry(geometryFactory.createPoint(coordinate).buffer(0.1d), Color.BLACK);
		}
		jtsFrame.addGeometry(geometryFactory.createPoint( buffer.getCoordinates()[5]).buffer(0.2d), Color.BLACK);

		jtsFrame.addGeometry(buffer, Color.BLACK);
		jtsFrame.setVisible(true);
		jtsFrame.setSize(400, 300);

		jtsFrame.addGeometry(path, Color.RED);

		Polygon trianglePolygon = new FeatureGeometryCalculator().triangle(new Coordinate(10, 20),  new Coordinate(10, 30), 1f);
		List<Polygon> triangles = new FeatureGeometryCalculator().triangles(new Coordinate(10, 20),  new Coordinate(10, 30), 1f , 2, 1);
		for(Polygon triangle : triangles){
//			jtsFrame.addGeometry(triangle, Color.RED);
		}

		Polygon peakPolygon = new FeatureGeometryCalculator().peak(new Coordinate(10, 20),  new Coordinate(10, 30), 1f, 0.2f);
		Polygon peakPolygon2 = new FeatureGeometryCalculator().peak(new Coordinate(10, 20),  new Coordinate(10, 50), 1f, 3f, 7f);
//		jtsFrame.addGeometry(peakPolygon, Color.BLACK);
//		jtsFrame.addGeometry(peakPolygon2, Color.RED);

		List<Polygon> peaks = new FeatureGeometryCalculator().peaks(new Coordinate(10, 20),  new Coordinate(10, 30), 1f , 0.2f, 2.2f, 0.3f);
		for(Polygon peak : peaks){
			jtsFrame.addGeometry(peak, Color.GREEN);
		}


	}

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
