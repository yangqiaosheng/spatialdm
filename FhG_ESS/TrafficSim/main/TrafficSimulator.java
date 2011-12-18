package main;

import spade.lib.util.Parameters;
import spade.lib.util.CopyFile;
import spade.time.Date;
import spade.analysis.tools.moves.simulation.SimulationEngine;
import spade.analysis.tools.moves.simulation.PlaceMovesAggregate;
import configstart.SysConfigReader;
import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.io.File;
import java.io.FileWriter;

/**
 * Created by IntelliJ IDEA.
 * User: natascha
 * Date: Nov 30, 2011
 * Time: 3:33:32 PM
 * An autonomous program that loads and runs a traffic simulation model.
 * The model simulates additional traffic, e.g., many cars leaving the
 * area of a stadium after a football game.
 */
public class TrafficSimulator {
	/**
	* Argument: the path to the file with the simulation parameters
	*/
	public static void main(String args[]) {
		if (args == null || args.length < 1) {
			System.out.println("The path to the file with the simulation parameters " + "must be specified as an argument!");
			return;
		}
		//read and set values of system parameters
		Parameters parm = new Parameters();
		SysConfigReader scr = new SysConfigReader(parm, null);
		if (!scr.readConfiguration(args[0])) {
			System.out.println("Could not read parameters from " + args[0] + "!");
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
			System.out.println("No simulation start time!");
			return;
		}
		String timeStr = parm.getParameterAsString("START_TIME");
		Date d = new Date();
		d.scheme = "dd/mm/yyyy;hh:tt";
		if (!d.setMoment(timeStr) || !d.isValid()) {
			System.out.println("Invalid simulation start time: " + timeStr + "; expected dd/mm/yyyy;hh:tt");
			return;
		}
		Document doc = LoadXML.loadXML(parm.getParameterAsString("MODEL"));
		if (doc == null)
			return;
		SimulationEngine sim = new SimulationEngine();
		if (!sim.restoreFromXML(doc)) {
			System.out.println("Cannot read the model: " + sim.err);
			return;
		}
		if (!sim.retrievePlaces()) {
			System.out.println("Failed to retrieve places: " + sim.err);
			return;
		}
		sim.predT0 = d;
		if (!sim.prepareModels()) {
			System.out.println("Model error: " + sim.err);
			return;
		}
		int nObj = 0;
		if (parm.getParameter("N_MOVERS") != null)
			try {
				int k = Integer.parseInt(parm.getParameterAsString("N_MOVERS"));
				if (k > 0)
					nObj = k;
			} catch (Exception ex) {
			}
		if (nObj == sim.nMoversTotal)
			nObj = 0;
		int nRoutesTotal = sim.attachRoutesToOrigins(nObj); //the N of trajectories as defined by the route frequencies
		if (nRoutesTotal < 1) {
			System.out.println("Failed to attach the possible routes to the origin places: " + sim.err);
			return;
		}
		if (!sim.simulateMovement()) {
			System.out.println(sim.err);
			return;
		}
		int aggILen = 10;
		if (parm.getParameter("AGGREGATION_INTERVAL_LENGTH") != null)
			try {
				int k = Integer.parseInt(parm.getParameterAsString("AGGREGATION_INTERVAL_LENGTH"));
				if (k > 0)
					aggILen = k;
			} catch (Exception ex) {
			}
		PlaceMovesAggregate pAgg[] = sim.aggregateByPlaces(aggILen);
		if (pAgg == null || pAgg.length < 1) {
			System.out.println("Failed to aggregate: " + sim.err);
			return;
		}
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
		d = (Date) sim.predT0.getCopy();
		d.setPrecision('t');
		try {
			writer.write("place_id,Load at t=" + d.toString());
			for (int i = 1; i < pAgg[0].baseLoads.length; i++) {
				d.add(aggILen);
				writer.write(",Load at t=" + d.toString());
			}
			writer.write("\r\n");
			for (int i = 0; i < pAgg.length; i++) {
				writer.write(pAgg[i].id);
				for (int j = 0; j < pAgg[i].baseLoads.length; j++)
					writer.write("," + (pAgg[i].baseLoads[j] + pAgg[i].addCounts[j]));
				writer.write("\r\n");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		try {
			writer.close();
		} catch (Exception ex) {
		}
	}

}
