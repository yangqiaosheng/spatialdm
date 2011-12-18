package main;

import spade.lib.util.Parameters;
import spade.lib.util.StringUtil;
import spade.lib.util.CopyFile;
import spade.time.Date;
import spade.analysis.tools.time_series.TSModelSet;
import spade.analysis.tools.time_series.ModelledObjectInfo;
import configstart.SysConfigReader;
import org.w3c.dom.Document;

import java.io.File;
import java.io.FileWriter;

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
		String paraFile = "params3.txt";
		if (args.length == 1) {
			paraFile = args[0];
		}
		//read and set values of system parameters
		Parameters parm = new Parameters();
		SysConfigReader scr = new SysConfigReader(parm, null);
		if (!scr.readConfiguration(paraFile)) {
			System.out.println("Could not read parameters from " + paraFile + "!");
			return;
		}
		if (parm.getParameter("MODEL") == null) {
			System.out.println("No path to the model!");
			return;
		}
		if (parm.getParameter("OUTPUT_FILE") == null) {
			System.out.println("No path for the output file!");
			return;
		}
		if (parm.getParameter("START_TIME") == null) {
			System.out.println("No modelling start time!");
			return;
		}
		if (parm.getParameter("END_TIME") == null) {
			System.out.println("No modelling end time!");
			return;
		}
		String timeStr = parm.getParameterAsString("START_TIME");
		Date dStart = new Date();
		dStart.scheme = "dd/mm/yyyy;hh:tt";
		if (!dStart.setMoment(timeStr) || !dStart.isValid()) {
			System.out.println("Invalid modelling start time: " + timeStr + "; expected dd/mm/yyyy;hh:tt");
			return;
		}
		timeStr = parm.getParameterAsString("END_TIME");
		Date dEnd = new Date();
		dEnd.scheme = "dd/mm/yyyy;hh:tt";
		if (!dEnd.setMoment(timeStr) || !dEnd.isValid()) {
			System.out.println("Invalid modelling end time: " + timeStr + "; expected dd/mm/yyyy;hh:tt");
			return;
		}
		if (dStart.compareTo(dEnd) >= 0) {
			System.out.println("The modelling start time is not earlier than the end time!");
			return;
		}

		Document doc = LoadXML.loadXML(parm.getParameterAsString("MODEL"));
		if (doc == null)
			return;

		TSModelSet modSet = new TSModelSet();
		if (!modSet.restoreFromXML(doc)) {
			System.out.println("Model error: " + modSet.getErrorMessage());
			return;
		}
		dStart.setPrecision(modSet.startTime.getPrecision());
		dEnd.setPrecision(modSet.startTime.getPrecision());
		int nSteps = modSet.getNTimeSteps(dStart, dEnd);
		if (nSteps < 1) {
			System.out.println(modSet.getErrorMessage());
			return;
		}
		boolean addNoise = StringUtil.sameStringsIgnoreCase(parm.getParameterAsString("ADD_NOISE"), "true");
		double values[][] = modSet.predict(dStart, dEnd, addNoise);
		if (values == null || values.length < 1) {
			System.out.println(modSet.getErrorMessage());
			return;
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
				return;
			}
		}
		FileWriter writer = null;
		try {
			writer = new FileWriter(path);
		} catch (Exception ex) {
			System.out.println("Cannot create file [" + path + "]!");
			ex.printStackTrace();
			return;
		}
		ModelledObjectInfo oInfo[] = modSet.getObjInfos();
		Date d = (Date) dStart.getCopy();
		try {
			writer.write("place_id,Load at t=" + d.toString());
			for (int i = 1; i < values[0].length; i++) {
				d.add(stepLen);
				writer.write(",Load at t=" + d.toString());
			}
			writer.write("\r\n");
			for (int i = 0; i < values.length; i++)
				if (values[i] != null && values[i].length > 0) {
					writer.write(oInfo[i].id);
					for (int j = 0; j < values[i].length; j++)
						writer.write("," + Math.round(values[i][j]));
					writer.write("\r\n");
				}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		try {
			writer.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
