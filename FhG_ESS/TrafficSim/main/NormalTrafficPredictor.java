package main;

import spade.lib.util.Parameters;
import spade.lib.util.StringUtil;
import spade.lib.util.CopyFile;
import spade.time.Date;
import spade.analysis.tools.time_series.TSModelSet;
import spade.analysis.tools.time_series.ModelledObjectInfo;
import configstart.SysConfigReader;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.w3c.dom.Document;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.fraunhofer.iais.spatial.service.ModelManager;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: natascha
 * Date: Dec 2, 2011
 * Time: 3:25:07 PM
 * An autonomous program that loads and runs a temporal model of normal city traffic
 * predicting the number of cars in each place by time intervals.
 */
public class NormalTrafficPredictor {

	/**
	* Argument: the path to the file with the modelling parameters
	*/

	public static void main(String args[]) {
		/*params3.txt*/
		Map<Long, Map<String, Integer>> areaEvents3 = generateEvents("models/model_place_presence_15min_intervals.xml", "01/12/2011;19:30", "02/12/2011;02:00", true);
	}

	public static Map<Long, Map<String, Integer>> generateEvents(String model, String startTime, String endTime, boolean addNoise) {
		Map<Long, Map<String, Integer>> areaEvents = Maps.newTreeMap();
		String basePath = StringUtils.defaultString(System.getProperty("ess.root"));

		//set values of system parameters
		Parameters parm = new Parameters();
		parm.setParameter("MODEL", basePath + model);
		parm.setParameter("START_TIME", startTime);
		parm.setParameter("END_TIME", endTime);
		parm.setParameter("OUTPUT_FILE", "data/normal_place_loads.csv");

		if (parm.getParameter("MODEL") == null) {
			throw new RuntimeException("No path to the model!");
		}
		if (parm.getParameter("OUTPUT_FILE") == null) {
			throw new RuntimeException("No path for the output file!");
		}
		if (parm.getParameter("START_TIME") == null) {
			System.out.println("No modelling start time!");
		}
		if (parm.getParameter("END_TIME") == null) {
			throw new RuntimeException("No modelling end time!");
		}
		String timeStr = parm.getParameterAsString("START_TIME");
		Date dStart = new Date();
		dStart.scheme = "dd/mm/yyyy;hh:tt";
		if (!dStart.setMoment(timeStr) || !dStart.isValid()) {
			throw new RuntimeException("Invalid modelling start time: " + timeStr + "; expected dd/mm/yyyy;hh:tt");
		}
		timeStr = parm.getParameterAsString("END_TIME");
		Date dEnd = new Date();
		dEnd.scheme = "dd/mm/yyyy;hh:tt";
		if (!dEnd.setMoment(timeStr) || !dEnd.isValid()) {
			throw new RuntimeException("Invalid modelling end time: " + timeStr + "; expected dd/mm/yyyy;hh:tt");
		}
		if (dStart.compareTo(dEnd) >= 0) {
			throw new RuntimeException("The modelling start time is not earlier than the end time!");
		}

		Document doc = LoadXML.loadXML(parm.getParameterAsString("MODEL"));
		if (doc == null)
			throw new RuntimeException("cannot load the model xml file!");

		TSModelSet modSet = new TSModelSet();
		if (!modSet.restoreFromXML(doc)) {
			throw new RuntimeException("Model error: " + modSet.getErrorMessage());
		}
		dStart.setPrecision(modSet.startTime.getPrecision());
		dEnd.setPrecision(modSet.startTime.getPrecision());
		int nSteps = modSet.getNTimeSteps(dStart, dEnd);
		if (nSteps < 1) {
			throw new RuntimeException(modSet.getErrorMessage());
		}
		double values[][] = modSet.predict(dStart, dEnd, addNoise);
		if (values == null || values.length < 1) {
			throw new RuntimeException(modSet.getErrorMessage());
		}
		long modTLen = modSet.endTime.subtract(modSet.startTime) + 1;
		int stepLen = (int) Math.round(1.0 * modTLen / modSet.nTimeSteps);

		String path = parm.getParameterAsString("OUTPUT_FILE");
		String dir = CopyFile.getDir(path);
		if (dir != null && dir.length() > 0) {
			File file = new File(dir);
			if (!file.exists())
				file.mkdir();
			if (!file.exists()) {
				System.out.println("Cannot create directory [" + dir + "]!");
				return null;
			}
		}
		FileWriter writer = null;
		try {
			writer = new FileWriter(path);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Cannot create file [" + path + "]!");
		}
		ModelledObjectInfo oInfo[] = modSet.getObjInfos();
		Date d = (Date) dStart.getCopy();
		try {
			SimpleDateFormat inputDateFormat = new SimpleDateFormat("dd/MM/yyyy;HH:mm");
			SimpleDateFormat outputDateFormat = new SimpleDateFormat(ModelManager.minuteDateFormatStr);
			List<String> dates = Lists.newArrayList();
			writer.write("place_id,Load at t=" + d.toString());
			dates.add(outputDateFormat.format(inputDateFormat.parse(d.toString())));
			for (int i = 1; i < values[0].length; i++) {
				d.add(stepLen);
				writer.write(",Load at t=" + d.toString());
				dates.add(outputDateFormat.format(inputDateFormat.parse(d.toString())));
			}
			writer.write("\r\n");
//			System.out.println(dates);

			for (int i = 0; i < values.length; i++)
				if (values[i] != null && values[i].length > 0) {
					writer.write(oInfo[i].id);
					long areaId = NumberUtils.toLong(oInfo[i].id);
					Map<String, Integer> events = Maps.newTreeMap();
					for (int j = 0; j < values[i].length; j++){
						writer.write("," + Math.round(values[i][j]));
						events.put(dates.get(j), (int)Math.round(values[i][j]));
					}
					System.out.println(areaId + " " + events);
					writer.write("\r\n");
					areaEvents.put(areaId, events);
				}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		try {
			writer.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return areaEvents;
	}
}
