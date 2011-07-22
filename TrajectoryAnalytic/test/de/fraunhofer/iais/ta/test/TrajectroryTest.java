package de.fraunhofer.iais.ta.test;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import oracle.spatial.util.WKT;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.math.NumberUtils;
import org.junit.Test;

import au.com.bytecode.opencsv.CSVReader;

import com.google.common.collect.Lists;
import com.mchange.v2.c3p0.impl.NewPooledConnection;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.WKTWriter;
import com.vividsolutions.jts.io.oracle.OraWriter;

import de.fraunhofer.iais.ta.GeoConfigContext;
import de.fraunhofer.iais.ta.entity.Measurement;
import de.fraunhofer.iais.ta.entity.TrajectorySegment;
import de.fraunhofer.iais.ta.util.DBUtil;


public class TrajectroryTest {


	static DBUtil db = new DBUtil("/jdbc_pg.properties", 3, 10);

	@Test
	public void toTextTest() throws IOException {
		Point p = GeoConfigContext.GEOMETRY_FACTORY.createPoint(new Coordinate(10, 10));
		System.out.println(p.toText());
		System.out.println(p.toString());
		System.out.println(new WKTWriter().writeFormatted(p));
	}

	@Test
	public void csvReaderTest() throws IOException {
		CSVReader reader = new CSVReader(new FileReader("data/single_trajectory.csv"));
		String[] nextLine;
		while ((nextLine = reader.readNext()) != null) {
			// nextLine[] is an array of values from the line
			System.out.println(nextLine[3] + nextLine[4] + "etc...");
		}
	}

	@Test
	public void csv2SingleTrajectoryTest() throws IOException, ParseException {
		CSVReader reader = new CSVReader(new FileReader("data/single_trajectory.csv"));
		String[] nextLine;
		System.out.println("Title: " + ToStringBuilder.reflectionToString(reader.readNext()));
		List<Measurement> measurements = Lists.newArrayList();

		List<TrajectorySegment> trajectories = Lists.newArrayList();
		Measurement preMeasurement = null;
		for (Measurement measurement : measurements) {
			if (preMeasurement != null) {
				String trId = measurement.getTrId();
				int trN = measurement.getTrN();
				Coordinate fromCoordinate = preMeasurement.getCoordinate();
				Coordinate toCoordinate = measurement.getCoordinate();
				double length = toCoordinate.distance(fromCoordinate);
				TrajectorySegment trajectory = new TrajectorySegment(trId, trN, 0, fromCoordinate, toCoordinate, length);
				trajectories.add(trajectory);
			}
			preMeasurement = measurement;
		}

	}



	@Test
	public void selectMovementTest() throws IOException, ParseException {

		Connection conn = db.getConn();

		PreparedStatement selectPstmt = db.getPstmt(conn, "select tr_id, tr_n, p_idx, x, y, time, speed from measurement");
		List<Measurement> measurements = Lists.newArrayList();
		ResultSet selectRs = db.getRs(selectPstmt);
		try {
			while (selectRs.next()) {
				Measurement measurement = initMeasurement(selectRs);
				measurements.add(measurement);
				System.out.println(ToStringBuilder.reflectionToString(measurement));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.close(selectPstmt);
			db.close(conn);

		}

	}

	private Measurement initMeasurement(ResultSet rs) throws SQLException {
		String trId = rs.getString("tr_id");
		int trN = rs.getInt("tr_n");
		int pIdx = rs.getInt("p_idx");
		Coordinate coordinate = new Coordinate(rs.getDouble("x"), rs.getDouble("y"));
		Date time = rs.getTimestamp("time");
		Measurement measurement = new Measurement(trId, trN, pIdx, time, coordinate);
		return measurement;
	}


}
