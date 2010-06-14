package de.fraunhofer.iais.spatial.dao.ibatis.type;

import java.awt.geom.Point2D;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import oracle.spatial.geometry.JGeometry;
import oracle.sql.STRUCT;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

public class Point2DTypeHandler implements TypeHandler {

	@Override
	public Object getResult(ResultSet rs, String columnName) throws SQLException {
		STRUCT st = (STRUCT) rs.getObject(columnName);
		JGeometry j_geom = JGeometry.load(st);
		Point2D point = j_geom.getJavaPoint();
		return point;
	}

	@Override
	public Object getResult(CallableStatement cs, int columnIndex) throws SQLException {
		STRUCT st = (STRUCT) cs.getObject(columnIndex);
		JGeometry j_geom = JGeometry.load(st);
		Point2D point = j_geom.getJavaPoint();
		return point;
	}

	@Override
	public void setParameter(PreparedStatement ps, int i, Object parameter,JdbcType jdbcType) throws SQLException {
	}

}
