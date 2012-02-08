package de.fraunhofer.iais.spatial.dao.mybatis.type;

import java.awt.geom.Point2D;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import oracle.spatial.geometry.JGeometry;
import oracle.sql.STRUCT;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.postgresql.geometric.PGpoint;

public class Point2DTypeHandler implements TypeHandler {

	@Override
	public Object getResult(ResultSet rs, String columnName) throws SQLException {
		Object resultObj = rs.getObject(columnName);
		if (resultObj instanceof STRUCT) {
			STRUCT st = (STRUCT) resultObj;
			JGeometry j_geom = JGeometry.load(st);
			Point2D point = j_geom.getJavaPoint();
			return point;
		} else if (resultObj instanceof PGpoint) {
			PGpoint point = (PGpoint) resultObj;
			return new Point2D.Double(point.x, point.y);
		} else
			throw new ClassFormatError("Error in converting the column:" + columnName + " to a Point2D Object");

	}

	@Override
	public Object getResult(CallableStatement cs, int columnIndex) throws SQLException {
		Object resultObj = cs.getObject(columnIndex);
		if (resultObj instanceof STRUCT) {
			STRUCT st = (STRUCT) resultObj;
			JGeometry j_geom = JGeometry.load(st);
			Point2D point = j_geom.getJavaPoint();
			return point;
		} else if (resultObj instanceof PGpoint) {
			PGpoint point = (PGpoint) resultObj;
			return new Point2D.Double(point.x, point.y);
		} else
			throw new ClassFormatError("Error in converting the column with Index:" + columnIndex + " to a Point2D Object");
	}

	@Override
	public void setParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType) throws SQLException {
	}

}
