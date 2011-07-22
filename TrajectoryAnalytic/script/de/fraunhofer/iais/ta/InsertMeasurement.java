package de.fraunhofer.iais.ta;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.math.NumberUtils;

import au.com.bytecode.opencsv.CSVReader;

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.io.WKTWriter;

import de.fraunhofer.iais.ta.GeoConfigContext;
import de.fraunhofer.iais.ta.entity.Measurement;
import de.fraunhofer.iais.ta.entity.TrajectorySegment;
import de.fraunhofer.iais.ta.util.DBUtil;


public class InsertMeasurement {

	static DBUtil db = new DBUtil("/jdbc_pg.properties", 3, 10);
	static SimpleDateFormat timeFormat = new SimpleDateFormat("d/M/yyyy h:mm");

	public static void main(String[] args) throws ParseException, IOException {
		CSVReader reader = new CSVReader(new FileReader("tr_0404_05_11_min_3km.csv"));
		String[] nextLine;
		System.out.println("Title: " + ToStringBuilder.reflectionToString(reader.readNext()));
		List<Measurement> measurements = Lists.newArrayList();
		while ((nextLine = reader.readNext()) != null) {
			Measurement measurement = intiMeasurement(nextLine);
			measurements.add(measurement);
			System.out.println(ToStringBuilder.reflectionToString(measurement));
		}

		Connection conn = db.getConn();

		PreparedStatement insertPstmt = db.getPstmt(conn, "insert into measurement (tr_id, tr_n, p_idx, x, y, point, time) values (?, ?, ?, ?, ?, ST_GeographyFromText(?), ?)");
		List<TrajectorySegment> trajectories = Lists.newArrayList();
		Measurement preMeasurement = null;
		try {
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

			for (Measurement measurement : measurements) {
				String trId = measurement.getTrId();
				int trN = measurement.getTrN();
				int pIdx = measurement.getpIdx();
				Coordinate coordinate = measurement.getCoordinate();
				Date time = measurement.getTime();

				int i = 1;
				insertPstmt.setString(i++, trId);
				insertPstmt.setInt(i++, trN);
				insertPstmt.setInt(i++, pIdx);
				insertPstmt.setDouble(i++, coordinate.x);
				insertPstmt.setDouble(i++, coordinate.y);
				String pointDbStr = "SRID=" + GeoConfigContext.SRID + ";" + WKTWriter.toPoint(coordinate);
				insertPstmt.setString(i++, pointDbStr);
				insertPstmt.setTimestamp(i++, new Timestamp(time.getTime()));
				insertPstmt.executeUpdate();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.close(insertPstmt);
			db.close(conn);

		}
	}

	private static Measurement intiMeasurement(String[] nextLine) throws ParseException {
		String trId = nextLine[0];
		int trN = NumberUtils.toInt(nextLine[1]);
		int pIdx = NumberUtils.toInt(nextLine[2]);
		Coordinate coordinate = new Coordinate(NumberUtils.toDouble(nextLine[3]), NumberUtils.toDouble(nextLine[4]));
		Date time = timeFormat.parse(nextLine[5]);
		Measurement measurement = new Measurement(trId, trN, pIdx, time, coordinate);
		return measurement;
	}


}
