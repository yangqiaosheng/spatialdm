package de.fraunhofer.iais.spatial.dao.mybatis.type;

import java.awt.geom.Point2D;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import oracle.spatial.geometry.JGeometry;
import oracle.sql.STRUCT;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.postgis.LinearRing;
import org.postgis.MultiPolygon;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;

public class ListTypeHandler implements TypeHandler {

	@Override
	public Object getResult(ResultSet rs, String columnName) throws SQLException {
		Object resultObj = rs.getObject(columnName);
		List<Point2D> geom = new LinkedList<Point2D>();

		if (resultObj instanceof STRUCT) {
			STRUCT st = (STRUCT) resultObj;
			JGeometry j_geom = JGeometry.load(st);
			for (int i = 0; i < j_geom.getOrdinatesArray().length;) {
				geom.add(new Point2D.Double(j_geom.getOrdinatesArray()[i++], j_geom.getOrdinatesArray()[i++]));
			}
			return geom;
		} else if (resultObj instanceof PGgeometry) {
			if(((PGgeometry)resultObj).getGeometry() instanceof MultiPolygon){
				MultiPolygon polygon = (MultiPolygon) ((PGgeometry)resultObj).getGeometry();
				LinearRing rng = polygon.getPolygons()[0].getRing(0);
				for(Point point : rng.getPoints()){
					geom.add(new Point2D.Double(point.getX(), point.getY()));
				}
				return geom;
			} else {
				Polygon polygon = (Polygon) ((PGgeometry)resultObj).getGeometry();
				LinearRing rng = polygon.getRing(0);
				for(Point point : rng.getPoints()){
					geom.add(new Point2D.Double(point.getX(), point.getY()));
				}
				return geom;
			}
		} else {
			throw new ClassFormatError("Error in converting the column:" + columnName + " to a List Object");
		}

	}

	@Override
	public Object getResult(CallableStatement cs, int columnIndex) throws SQLException {
		Object resultObj = cs.getObject(columnIndex);
		List<Point2D> geom = new LinkedList<Point2D>();

		if (resultObj instanceof STRUCT) {
			STRUCT st = (STRUCT) cs.getObject(columnIndex);
			JGeometry j_geom = JGeometry.load(st);
			for (int i = 0; i < j_geom.getOrdinatesArray().length;) {
				geom.add(new Point2D.Double(j_geom.getOrdinatesArray()[i++], j_geom.getOrdinatesArray()[i++]));
			}
			return geom;
		} else if (resultObj instanceof PGgeometry) {
			if(((PGgeometry)resultObj).getGeometry() instanceof MultiPolygon){
				MultiPolygon polygon = (MultiPolygon) ((PGgeometry)resultObj).getGeometry();
				LinearRing rng = polygon.getPolygons()[0].getRing(0);
				for(Point point : rng.getPoints()){
					geom.add(new Point2D.Double(point.getX(), point.getY()));
				}
				return geom;
			} else {
				Polygon polygon = (Polygon) ((PGgeometry)resultObj).getGeometry();
				LinearRing rng = polygon.getRing(0);
				for(Point point : rng.getPoints()){
					geom.add(new Point2D.Double(point.getX(), point.getY()));
				}
				return geom;
			}
		} else {
			throw new ClassFormatError("Error in converting the column with Index:" + columnIndex + " to a List Object");
		}
	}

	@Override
	public void setParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType) throws SQLException {

	}

}
