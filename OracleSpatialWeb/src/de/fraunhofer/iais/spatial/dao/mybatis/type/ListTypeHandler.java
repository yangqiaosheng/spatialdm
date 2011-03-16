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

import org.apache.commons.collections.CollectionUtils;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

public class ListTypeHandler implements TypeHandler {

	@Override
	public Object getResult(ResultSet rs, String columnName) throws SQLException {
		STRUCT st = (STRUCT) rs.getObject(columnName);
		JGeometry j_geom = JGeometry.load(st);
		List<Point2D> geom = new LinkedList<Point2D>();
		for (int i = 0; i < j_geom.getOrdinatesArray().length;) {
			geom.add(new Point2D.Double(j_geom.getOrdinatesArray()[i++], j_geom.getOrdinatesArray()[i++]));
		}

		return geom;



	}

	@Override
	public Object getResult(CallableStatement cs, int columnIndex) throws SQLException {
		STRUCT st = (STRUCT) cs.getObject(columnIndex);
		JGeometry j_geom = JGeometry.load(st);
		List<Point2D> geom = new LinkedList<Point2D>();
		for (int i = 0; i < j_geom.getOrdinatesArray().length;) {
			geom.add(new Point2D.Double(j_geom.getOrdinatesArray()[i++], j_geom.getOrdinatesArray()[i++]));
		}

		return geom;
	}

	@Override
	public void setParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType) throws SQLException {

	}

}
