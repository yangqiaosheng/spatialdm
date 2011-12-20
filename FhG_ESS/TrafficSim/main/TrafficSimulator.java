package main;

import spade.lib.util.Parameters;
import spade.lib.util.CopyFile;
import spade.time.Date;
import spade.analysis.tools.moves.simulation.SimulationEngine;
import spade.analysis.tools.moves.simulation.PlaceMovesAggregate;
import configstart.SysConfigReader;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.fraunhofer.iais.spatial.entity.Area;
import de.fraunhofer.iais.spatial.service.CsvImporter;
import de.fraunhofer.iais.spatial.service.ModelManager;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
		/*params1.txt*/
		Map<Long, Map<String, Integer>> areaEvents1 = generateEvents("models/sim_model.xml", "01/12/2011;19:30", "10000", "10");

		/*params2.txt*/
		Map<Long, Map<String, Integer>> areaEvents2 = generateEvents("models/sim_model_reduced_north.xml", "01/12/2011;19:30", "10000", "10");
	}

	public static Map<Long, Map<String, Integer>> generateEvents(String model, String startTime, String nMovers, String aggrIntervalLength) {
		Map<Long, Map<String, Integer>> areaEvents = Maps.newTreeMap();

		//set values of system parameters
		Parameters parm = new Parameters();
		parm.setParameter("MODEL", model);
		parm.setParameter("START_TIME", startTime);
		parm.setParameter("N_MOVERS", nMovers);
		parm.setParameter("AGGREGATION_INTERVAL_LENGTH", aggrIntervalLength);
		parm.setParameter("OUTPUT_FILE", "data/normal_place_loads.csv");

		if (parm.getParameter("MODEL") == null) {
			throw new RuntimeException("No path to the model!");
		}
		if (parm.getParameter("OUTPUT_FILE") == null) {
			throw new RuntimeException("No path for the output file!");
		}
		if (parm.getParameter("START_TIME") == null) {
			throw new RuntimeException("No simulation start time!");
		}
		String timeStr = parm.getParameterAsString("START_TIME");
		Date d = new Date();
		d.scheme = "dd/mm/yyyy;hh:tt";
		if (!d.setMoment(timeStr) || !d.isValid()) {
			System.out.println("Invalid simulation start time: " + timeStr + "; expected dd/mm/yyyy;hh:tt");
			return null;
		}
		Document doc = LoadXML.loadXML(parm.getParameterAsString("MODEL"));
		if (doc == null)
			throw new RuntimeException("cannot load the model xml file!");
		SimulationEngine sim = new SimulationEngine();
		if (!sim.restoreFromXML(doc)) {
			throw new RuntimeException("Cannot read the model: " + sim.err);
		}
		if (!sim.retrievePlaces()) {
			throw new RuntimeException("Failed to retrieve places: " + sim.err);
		}
		sim.predT0 = d;
		if (!sim.prepareModels()) {
			throw new RuntimeException("Model error: " + sim.err);
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
			throw new RuntimeException("Failed to attach the possible routes to the origin places: " + sim.err);
		}
		if (!sim.simulateMovement()) {
			throw new RuntimeException(sim.err);
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
			throw new RuntimeException("Failed to aggregate: " + sim.err);
		}
		String path = parm.getParameterAsString("OUTPUT_FILE");
		String dir = CopyFile.getDir(path);
		if (dir != null && dir.length() > 0) {
			File file = new File(dir);
			if (!file.exists())
				file.mkdir();
			if (!file.exists()) {
				throw new RuntimeException("Cannot create directory [" + dir + "]!");
			}
		}
		FileWriter writer = null;
		try {
			writer = new FileWriter(path);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Cannot create file [" + path + "]!");
		}
		d = (Date) sim.predT0.getCopy();
		d.setPrecision('t');
		try {
			SimpleDateFormat inputDateFormat = new SimpleDateFormat("dd/MM/yyyy;HH:mm");
			SimpleDateFormat outputDateFormat = new SimpleDateFormat(ModelManager.minuteDateFormatStr);
			List<String> dates = Lists.newArrayList();
			writer.write("place_id,Load at t=" + d.toString());
			dates.add(outputDateFormat.format(inputDateFormat.parse(d.toString())));
			for (int i = 1; i < pAgg[0].baseLoads.length; i++) {
				d.add(aggILen);
				writer.write(",Load at t=" + d.toString());
				dates.add(outputDateFormat.format(inputDateFormat.parse(d.toString())));
			}
			writer.write("\r\n");
//			System.out.println(dates);

			for (int i = 0; i < pAgg.length; i++) {
				writer.write(pAgg[i].id);
				long areaId = NumberUtils.toLong(pAgg[i].id);
				Map<String, Integer> events = Maps.newTreeMap();
				for (int j = 0; j < pAgg[i].baseLoads.length; j++) {
					writer.write("," + (pAgg[i].baseLoads[j] + pAgg[i].addCounts[j]));
					events.put(dates.get(j), (pAgg[i].baseLoads[j] + pAgg[i].addCounts[j]));
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
