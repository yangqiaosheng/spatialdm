/**
 * 
 */
package spade.time.vis;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.ESDACore;
import spade.analysis.system.Supervisor;
import spade.analysis.tools.events.EventMaker;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.PopupManager;
import spade.lib.basicwin.RowLayout;
import spade.lib.util.IntArray;
import spade.lib.util.QSortAlgorithm;
import spade.lib.util.StringUtil;
import spade.time.TimeMoment;
import spade.vis.action.Highlighter;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.ObjectFilter;
import spade.vis.database.Parameter;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.geometry.Geometry;
import spade.vis.space.GeoObject;

/**
 * @author Admin
 *
 */
public class RTimeSeriesPanel extends Panel {

	private Map<String, String> methods = new HashMap<String, String>();
	private Map<String, String[]> parameters = new HashMap<String, String[]>();
	private Map<String, String[]> parametersText = new HashMap<String, String[]>();
	private Map<String, String[]> parametersDefault = new HashMap<String, String[]>();

	String APath = null; // the path to the current application
	String RPath = ""; // the path to the R installation
	DataTable table = null; // the data to be analyzed
	Parameter par = null; // the temporal parameter
	Vector attr = null; // the attributes to form the time series
	//TextField TfParameters = null;     // the additional parameter for the time series method
	Choice ChMethods = null; // the time series method to be applied
	Label lResults = null; // label with the statistics of the results
	Button bPeriodic = null;
	Panel pResults = null;
	protected Supervisor supervisor = null; //supervisor
	Highlighter highlighter = null; // highlighter
	TimeGraph tigr = null; // time graph canvas, used for aligning canvases

	TimeArrangerWithIDsCanvas taCanvas = null;

	// how many time a R-script was called
	public static int calls = 0;

	// additional statistics about the time series
	class Statistics {
		int events = 0; // first attribute
		double maxValue = -Double.MAX_VALUE; // last / fifth attribute
		TimeMoment maxEvent = null; // fourth attribute
		TimeMoment firstEvent = null; // second attribute
		TimeMoment lastEvent = null; // third attribute
	}

	String[] additionalStatistics = { "Number of events", "Date of first", "Date of last", "Date of max", "Value of max" };
	char[] statisticsTypes = { AttributeTypes.integer, AttributeTypes.time, AttributeTypes.time, AttributeTypes.time, AttributeTypes.real };
	// mapping from each time series id to its statistics
	Map<String, Statistics> MStatistics = null;

	/**
	 * @param supervisor - used to get the required paths, the highlighter, and the system's core
	 * @param tigr - where the time series are drawn
	 * @param attr - the time-varying attribute
	 */
	public RTimeSeriesPanel(Supervisor supervisor, TimeGraph tigr, Vector attr) {
		this();
		this.supervisor = supervisor;
		RPath = supervisor.getSystemSettings().getParameterAsString("PATH_TO_RSTATISTICS");
		ESDACore core = (ESDACore) supervisor.getSystemSettings().getParameter("core");
		if (core != null) {
			APath = core.getDataKeeper().getApplicationPath();
		}
		this.highlighter = supervisor.getHighlighter(tigr.getTable().getEntitySetIdentifier());
		this.tigr = tigr;
		// prepare application path
		if (APath != null) {
			int idx = APath.lastIndexOf("\\");
			APath = APath.substring(0, idx);
		}
		this.table = (DataTable) tigr.getTable();
		this.par = tigr.getTemporalParameter();
		this.attr = attr;
	}

	public String paraString = "";
	public Panel parent = this;
	TextField tf = null;
	Label lb = null;
	Panel pControls = null;

	/*
	 * create panel
	 */
	public RTimeSeriesPanel() {
		super(new BorderLayout());

		pControls = new Panel(new RowLayout());
		add(pControls, BorderLayout.NORTH);

		// choises for method

		methods.put("Peak detection", "peaksV2.R");
		// parameters.put("Peak detection", new String[] {"deltaI", "dTI", "altI"});
		// parametersText.put("Peak detection", new String[] {"deltaI", "dTI", "altI"});
		// parametersDefault.put("Peak detection", new String[] {"100", "3", "0"});
		parameters.put("Peak detection", new String[] { "deltaTRelI", "deltaN", "dTR", "altI" });
		parametersText.put("Peak detection", new String[] { "deltaTRelI", "deltaN", "dTR", "altI" });
		parametersDefault.put("Peak detection", new String[] { "0", "100", "3:1:0", "0" });
		methods.put("Drift detection", "phEvents.R");
		parameters.put("Drift detection", new String[] { "deltaN", "altI" });
		parametersText.put("Drift detection", new String[] { "deltaN", "altI" });
		parametersDefault.put("Drift detection", new String[] { "100", "0" });
		methods.put("Correlation", "correlation.R");
		parameters.put("Correlation", new String[] { "periodI", "varNormI" });
		parametersText.put("Correlation", new String[] { "periodI", "varNormI" });
		parametersDefault.put("Correlation", new String[] { "12", "0" });
		methods.put("Periodicity", "periods2.R");
		parameters.put("Periodicity", new String[] { "pminI", "pmaxI", "minSuppN", "minConfN" });
		parametersText.put("Periodicity", new String[] { "pminI", "pmaxI", "minSuppN", "minConfN" });
		parametersDefault.put("Periodicity", new String[] { "12", "12", "0.5", "0.5" });

		tf = new TextField("Select a method in choice field", 45);
		ChMethods = new Choice();
		final PopupManager pm = new PopupManager(tf, "help", true);
		ChMethods.addItem("");
		for (String name : methods.keySet()) {
			ChMethods.addItem(name);
		}
		ChMethods.select(0);
		ChMethods.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				String[] paras = parameters.get(ChMethods.getSelectedItem());
				paraString = "";
				String tmpString = "";
				for (int i = 0; i < paras.length; i++) {
					paraString += " " + paras[i] + "=%" + (i + 5);
					tmpString += paras[i] + "=" + parametersDefault.get(ChMethods.getSelectedItem())[i] + " ";
				}
				tf.setText(tmpString);
				String hstr = ChMethods.getSelectedItem();
				switch (ChMethods.getSelectedIndex()) {
				case 1: // Periodicity: pminI=12 pmaxI=12 minSuppN=0.5 minConfN=0.5
					hstr = "Periodicity:\n" + "pminI..pmaxI: expected period\n" + "minSuppN, minConfN: support and confidence thresholds";
					break;
				case 2: // Correlation: periodI=12 varNormI=0
					hstr = "Temporal correlation:\n" + "periodI: expected period\n" + "varNormI: use normalization (0/1)";
					break;
				case 3: // Peak detection: deltaTRelI=0 deltaN=100 dTR=3:1:0 altI=0
					hstr = "Peak detection:\n" + "deltaTRelI: normalization\n - absolute values (0); by mean (1); by mean and stdd(2)\n" + "deltaN: desired peak amplitude\n" + "dTR: time window, start:step:end values\n"
							+ "altI: search for peaks (0) or pits (1)";
					break;
				case 4: // Drift detection: deltaN=100 altI=0
					hstr = "Drift detection:\n" + "deltaN: desired drift value\n" + "altI: search for increase (0) or decrease (1)";
					break;
				}
				pm.setText(hstr);
			}
		});
		pControls.add(ChMethods);
		lb = new Label("Parameters");
		pControls.add(lb);
		pControls.add(tf);

		// button to start R script

		Button BStart = new Button("Run R");
		BStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (paraString.length() <= 0)
					return;
				String paraVals = "";
				String tmpString = tf.getText();
				String[] pars = tmpString.split(" ");
				for (String par2 : pars) {
					paraVals += " " + par2.split("=")[1] + " ";
				}
				runRscript(paraString, paraVals, ChMethods.getSelectedItem());
			}
		});
		pControls.add(BStart);

		pResults = new Panel(new BorderLayout());
		add(pResults, BorderLayout.SOUTH);

		lResults = new Label("");
		pResults.add(lResults, BorderLayout.CENTER);
		bPeriodic = new Button("periodicity");
		bPeriodic.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				create2dTimeCanvas();
			}
		});
		pResults.add(bPeriodic, BorderLayout.EAST);
		bPeriodic.setEnabled(false);

	}

	/**
	 * Visualizes the specified time-dependent attribute(s) from the given file
	 * on a time graph or multiple time graphs.
	 */
	public void runRscript(String methodParamStr, String methodParamValues, String method) {
		if (table == null || par == null || attr == null || attr.size() < 1)
			return;

		if (MStatistics == null) {
			MStatistics = new HashMap<String, Statistics>();
		} else {
			MStatistics.clear();
		}

		// creating data structures for TimeArrangerCanvas
		vvIds = new Vector<Vector<String>>(100, 100);
		vvEvtNums = new Vector<Vector<Integer>>(100, 100);

		// the batch file calling the RScript
		String batchFile = "start.bat";
		System.out.println("Time series analyses " + method);
		String RFile = methods.get(method);
		File f = null;
		String Path = "";

		java.util.Date date = new java.util.Date();
		String d = "_" + StringUtil.padString("" + (1900 + date.getYear()), '0', 4, true) + StringUtil.padString("" + (1 + date.getMonth()), '0', 2, true) + StringUtil.padString("" + date.getDate(), '0', 2, true) + "_"
				+ StringUtil.padString("" + date.getHours(), '0', 2, true) + StringUtil.padString("" + date.getMinutes(), '0', 2, true) + StringUtil.padString("" + date.getSeconds(), '0', 2, true);
		try {
			// get path for all temporal files
			if (APath == null) {
				Path = ((f == null) ? "c:" : f.getAbsolutePath()) + "\\tmp";
			} else {
				Path = APath + "\\tmp";
			}

			f = new File(Path);
			if (!f.exists()) {
				f.mkdir();
			}

			f = new File(Path + "\\" + batchFile);
			if (f.exists()) {
				f.delete();
			}
			BufferedWriter out = new BufferedWriter(new FileWriter(f));
			out.write("echo OFF\n");
			out.write("\"" + RPath + "\\R-2.10.1\\bin" + "\\RScript.exe\" %1 dataInS=%2 dataOutS=%3 debugFileS=%4 " + methodParamStr + " > out.txt\n");
			out.write("exit\n");
			out.flush();
			out.close();

			// copy R script
			String pathToRscripts = RPath + "\\Scripts\\" + RFile;
			File RScriptSource = new File(pathToRscripts);
			if (!RScriptSource.exists()) {
				System.out.println("Error: R scripts are not found at \"" + pathToRscripts + "\"");
				Dialogs.showMessage(CManager.getAnyFrame(this), "R scripts are not found at\n" + pathToRscripts, "Error: no R scripts found");
				return;
			}
			File RScriptDest = new File(Path + "\\" + RFile);
			FileInputStream from = new FileInputStream(RScriptSource);
			FileOutputStream to = new FileOutputStream(RScriptDest);
			int bytesRead;
			byte[] buffer = new byte[4096];
			while ((bytesRead = from.read(buffer)) != -1) {
				to.write(buffer, 0, bytesRead); // write
			}
			from.close();
			to.close();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		String FileToR = "data" + d + ".txt";
		String FileFromR = "results" + d + ".csv";
		// id
		String id = "";
		// write table to file
		try {
			// in case data and result file already exists, delete them
			File data = new File(Path + "\\" + FileToR);
			if (data.exists()) {
				data.delete();
			}
			File res = new File(Path + "\\" + FileFromR);
			if (res.exists()) {
				res.delete();
			}
			res = null;
			Vector<Attribute> attrs = ((Attribute) attr.get(0)).getChildren();
			Vector<String> attrsNames = new Vector<String>();
			for (int j = 0; j < attrs.size() - 1; j++) {
				attrsNames.add((String) attrs.get(j).getIdentifier());
			}
			IntArray attrNumbers = new IntArray(attrsNames.size(), 1);
			for (int i = 0; i < attrsNames.size(); i++) {
				int idx = table.getAttrIndex((String) attrsNames.elementAt(i));
				if (idx >= 0) {
					attrNumbers.addElement(idx);
				}
			}
			FileOutputStream fos = new FileOutputStream(data);
			DataOutputStream bw = new DataOutputStream(fos);
			bw.writeBytes("id,name");
			for (int i = 0; i < attrsNames.size(); i++) {
				bw.writeBytes("," + attrsNames.get(i));
			}
			bw.writeBytes("\n");
			ObjectFilter oFilter = table.getObjectFilter();
			for (int i = 0; i < table.getDataItemCount(); i++)
				if (oFilter == null || oFilter.isActive(i)) {
					String val = "";
					id = "" + i; // instead of ID, we use just record number in the table
									// because R scriptrs require integers
					bw.writeBytes(id + "");

					for (int j = 0; j < attrNumbers.size() - 1; j++) {
						String currVal = table.getAttrValueAsString(attrNumbers.elementAt(j), i);
						if (currVal == null) {
							currVal = "0";
						}
						val += "," + currVal;
					}
					if (i == table.getDataItemCount() - 1) {
						bw.flush();
					}
					bw.writeBytes(val + "\n"); // rec.getAttrValueAsString(rec.getAttrCount()
				}
			bw.close();
			fos.close();

			// call R via batch file and command line by system call
			int exit_value = -1;
			f = new File("");
			// compond parameter values
			String pValues = methodParamValues;

			String args = " \"" + Path + File.separator + FileToR + "\" \"" + Path + File.separator + FileFromR + "\" \"" + Path + File.separator + "debug.out\"" + pValues;
			System.out.println(args);
			args = args.replaceAll("\\\\", "\\\\\\\\");
			System.out.println(args);
			String command = "cmd.exe" + " /C " + "\"\"" + Path + "\\start.bat\" \"" + Path + File.separator + RFile + "\"" + " " + args + "\"";

			System.out.println("R system call starting: " + command);
			Process p = Runtime.getRuntime().exec(command);
			calls++;
			exit_value = p.waitFor();
			System.out.println("R system call ending with " + exit_value);
			// read the results file create by the R method
			BufferedReader br = new BufferedReader(new FileReader(Path + "\\" + FileFromR));

			// add additional column to table
			int k = 0;

			// string for method
			String m = "";
			if (method.equals("Peak detection")) {
				m = "peak";
			}
			if (method.equals("Drift detection")) {
				m = "drift";
			}
			if (method.equals("Correlation")) {
				m = "correlation";
			}

			for (int i = 0; i < tigr.getTemporalParameter().getValueCount(); i++) {
				vvIds.addElement(null);
				vvEvtNums.addElement(null);
			}

			// old attribute count
			int oldAttr = table.getAttrCount();
			// finding unique attribute number and name
			int add_attr = calls;
			String aName = "Call " + add_attr + " " + additionalStatistics[0] + " " + m;
			while (table.findAttrByName(aName) >= 0) {
				add_attr++;
				aName = "Call " + add_attr + " " + additionalStatistics[0] + " " + m;
				//aName = "Number of events nr. " + add_attr;
			}

			aName = "R results nr. " + add_attr;
			// Number of events
			int nEvents = 0;
			int pmin = 0, pmax = 0;
			// range for dTI for peak detection
			int dTmin = 0, dTmax = 0, dTstep = 0;
			// add additional statistics as attributes
			if (method.equals("Periodicity")) {
				// search for min and max periodicity
				String[] paras = methodParamValues.trim().split("[ ]+");
				pmin = Integer.parseInt(paras[0].trim());
				pmax = Integer.parseInt(paras[1].trim());
				for (int v = pmin; v <= pmax; v++) {
					Attribute att = new Attribute("Call " + add_attr + " period " + v + " max support", AttributeTypes.real);
					table.addAttribute(att);
					att = new Attribute("Call " + add_attr + " period " + v + " median support", AttributeTypes.real);
					table.addAttribute(att);
					att = new Attribute("Call " + add_attr + " period " + v + " max confidence", AttributeTypes.real);
					table.addAttribute(att);
					att = new Attribute("Call " + add_attr + " period " + v + " median confidence", AttributeTypes.real);
					table.addAttribute(att);
				}
			} else {
				// peak detection
				for (int i = (method.equals("Correlation")) ? additionalStatistics.length - 1 : 0; i < additionalStatistics.length; i++) {
					Attribute statAttr = new Attribute("Call " + add_attr + " " + additionalStatistics[i] + " " + m, statisticsTypes[i]);
					table.addAttribute(statAttr);
				}
			}

			table.makeUniqueAttrIdentifiers();

			// set 0 counts of events in all records
			for (int recN = 0; recN < table.getDataItemCount(); recN++) {
				table.setNumericAttributeValue(0, table.getAttrCount() - 5, recN);
			}

			long intervalLength = ((TimeMoment) par.getValue(1)).subtract(((TimeMoment) par.getValue(0)));

			IntArray iaTWins = new IntArray(10, 10);
			String line = "";
			String[] vals = null;

			int timeID = -1;
			while ((line = br.readLine()) != null) {
				// load all output for each dTI as table
				vals = line.split("[,]");
				// get id of resulting series
				if (vals != null) {
					try {
						int recN = -1;
						try {
							recN = Integer.valueOf(vals[0].trim()).intValue();
						} catch (NumberFormatException nfo) {
							recN = -1;
						}
						if (recN == -1) { // Panic!
							System.out.println("* unexpected table record number: " + vals[0]);
							continue;
						}
						id = table.getDataItemId(recN);
						// additional statistics
						if (!MStatistics.containsKey(id)) {
							MStatistics.put(id, new Statistics());
						}
						Statistics currStat = MStatistics.get(id);
						// another event
						currStat.events++;
						DataRecord rec = (DataRecord) table.getDataItem(recN);

						double value = 0d; // value of the event
						int timeWindow = 0; // used time window

						// temporal index of the event
						if (method.equals("Correlation")) {
							timeID = 0;
							TimeMoment currTime = (TimeMoment) par.getValue(timeID);
							currStat.firstEvent = currTime;
							currStat.lastEvent = currTime;
							value = Double.parseDouble(vals[2].trim());
							currStat.maxValue = value;
							rec.setAttrValue(currStat.maxValue, table.getAttrCount() - 1);
						} else {
							if (method.equals("Periodicity")) {
								int period = Integer.parseInt(vals[2].trim());
								period = pmax - period;
								rec.setAttrValue(Double.parseDouble(vals[6].trim()), table.getAttrCount() - 1 - 4 * period);
								rec.setAttrValue(Double.parseDouble(vals[5].trim()), table.getAttrCount() - 2 - 4 * period);
								rec.setAttrValue(Double.parseDouble(vals[4].trim()), table.getAttrCount() - 3 - 4 * period);
								rec.setAttrValue(Double.parseDouble(vals[3].trim()), table.getAttrCount() - 4 - 4 * period);
							} else {
								if (method.equals("Peak detection")) {
									timeWindow = Integer.parseInt(vals[4].trim());
									if (iaTWins.indexOf(timeWindow) < 0) {
										iaTWins.addElement(timeWindow);
									}
								}
								timeID = Integer.parseInt(vals[2].trim());
								value = Double.parseDouble(vals[3].trim());
							}
						}
						if (method.equals("Correlation") || method.equals("Periodicity")) { // for correlation or periodicy we record only max value

						} else {
							// time of current event
							TimeMoment currTime = (TimeMoment) par.getValue(timeID);
							// is current value bigger than the last on this
							// time series?
							if (value > currStat.maxValue) {
								currStat.maxValue = value;
								currStat.maxEvent = currTime;
							}
							// searching for first event
							if (currStat.firstEvent == null) {
								currStat.firstEvent = currTime;
							}
							if (currTime.compareTo(currStat.firstEvent) < 0) {
								currStat.firstEvent = currTime;
							}
							// searching for last event
							if (currStat.lastEvent == null) {
								currStat.lastEvent = currTime;
							}
							if (currTime.compareTo(currStat.lastEvent) >= 0) {
								currStat.lastEvent = currTime;
							}
							// resulting indexes begin with 0
							// rec.setAttrValue('y', oldAttr + timeID);
							nEvents++;
							// preparing data for the TimeArranger canvas
							Vector<String> vIds = vvIds.elementAt(timeID);
							if (vIds == null) {
								vIds = new Vector<String>(10, 10);
								vvIds.setElementAt(vIds, timeID);
							}
							vIds.addElement(rec.getId());
							Vector<Integer> vEvtNums = vvEvtNums.elementAt(timeID);
							if (vEvtNums == null) {
								vEvtNums = new Vector<Integer>(10, 10);
								vvEvtNums.setElementAt(vEvtNums, timeID);
							}
							vEvtNums.addElement(new Integer(nEvents - 1));

							// adding Event <id,currTime,value> to the Event Layer
							if (!makingEventsFailed) {
								if (evMaker == null) {
									startMakingEvents(method + " (" + methodParamValues.trim() + ")", method + " " + methodParamValues);
								}
								if (evMaker != null) {
									updateEventLayer(recN, rec.getId(), rec.getName(), currTime, intervalLength, value, timeWindow);
								}
							}

							// set attribute values for the additional
							// statistics
							rec.setAttrValue(currStat.events, table.getAttrCount() - 5);
							rec.setAttrValue(currStat.firstEvent, table.getAttrCount() - 4);
							rec.setAttrValue(currStat.lastEvent, table.getAttrCount() - 3);
							rec.setAttrValue(currStat.maxEvent, table.getAttrCount() - 2);
							rec.setAttrValue(currStat.maxValue, table.getAttrCount() - 1);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			br.close();

			if (evMaker != null) {
				if (method.equals("Peak detection") && iaTWins.getTrimmedArray() != null && iaTWins.getTrimmedArray().length > 1) {
					addTimeWindowStatToTable(iaTWins);
				}
				finishMakingEvents();
			}

			if (nEvents > 0) {
				createTimeCanvas();
			} else {
				removeTimeCanvas();
			}
			bPeriodic.setEnabled(nEvents > 0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void addTimeWindowStatToTable(IntArray iaTWins) {
		int twins[] = iaTWins.getTrimmedArray();
		QSortAlgorithm.sort(twins);

		Parameter param = new Parameter();
		for (int twin : twins) {
			param.addValue(twin);
		}
		param.setName("Time window, call " + calls);
		table.addParameter(param);
		Attribute aParentCount = new Attribute("Call " + calls + " Peaks Count", 'N'), aParentMax = new Attribute("Call " + calls + " Max Peak", 'N');
		int attrCount = table.getAttrCount();
		for (int twIdx = 0; twIdx < twins.length; twIdx++) {
			Attribute statAttr = new Attribute("Call " + calls + " t.win=" + twins[twIdx] + " Peaks Count", 'N');
			statAttr.addParamValPair(param.getName(), param.getValue(twIdx));
			aParentCount.addChild(statAttr);
			table.addAttribute(statAttr);
			statAttr = new Attribute("Call " + calls + " t.win=" + twins[twIdx] + " Max Peak", 'N');
			statAttr.addParamValPair(param.getName(), param.getValue(twIdx));
			aParentMax.addChild(statAttr);
			table.addAttribute(statAttr);
		}

		for (int recN = 0; recN < table.getDataItemCount(); recN++) {
			for (int twIdx = 0; twIdx < twins.length; twIdx++) {
				table.setNumericAttributeValue(0, attrCount + 2 * twIdx, recN);
			}
		}

		DataTable evTbl = evMaker.getEventTable();
		for (int twIdx = 0; twIdx < twins.length; twIdx++) {
			for (int i = 0; i < evTbl.getDataItemCount(); i++)
				if (evTbl.getNumericAttrValue(tsTimeWindowN, i) == twins[twIdx]) {
					int recN = (int) evTbl.getNumericAttrValue(tsNColN, i);
					int count = (int) table.getNumericAttrValue(attrCount + 2 * twIdx, recN);
					table.setNumericAttributeValue(count + 1, attrCount + 2 * twIdx, recN);
					double maxv = evTbl.getNumericAttrValue(tsValueColN, i);
					if (count > 0) {
						maxv = Math.max(maxv, table.getNumericAttrValue(attrCount + 2 * twIdx + 1, recN));
					}
					table.setNumericAttributeValue(maxv, attrCount + 2 * twIdx + 1, recN);
				}
		}
	}

	/**
	 * Used to create a map layer with the extracted events
	 */
	protected EventMaker evMaker = null;
	/**
	 * The type of the currently produced events, e.g. "peak" (possibly, with parameters)
	 */
	protected String evType = null;
	/**
	 * Signals that an attempt to make a layer with events has failed
	 */
	protected boolean makingEventsFailed = false;
	/**
	 * The geographical layer with the objects described by the time series
	 */
	protected DGeoLayer souLayer = null;
	/**
	 * Reserved identifiers for the table columns with the attributes of the events
	 */
	public static final String tsNColId = "__time_series_N__", tsNameColId = "__time_series_name__", tsValueColId = "__event_value__", tsTimeWindowId = "__time_window__";
	/**
	 * The indexes of the table columns with the attributes of the events
	 */
	protected int tsNColN = -1, tsNameColN = -1, tsValueColN = -1, tsTimeWindowN = -1;

	/**
	 * Initiates the generation of a map layer with the extracted events
	 */
	protected void startMakingEvents(String evType, String desiredLayerName) {
		if (evMaker != null)
			return;
		makingEventsFailed = true;
		ESDACore core = (ESDACore) supervisor.getSystemSettings().getParameter("core");
		if (core == null)
			return;
		if (souLayer == null) {
			AttributeDataPortion table = tigr.getTable();
			if (table == null)
				return;
			DataKeeper dataKeeper = core.getDataKeeper();
			souLayer = (DGeoLayer) dataKeeper.getTableLayer(table);
			if (souLayer == null)
				return;
		}
		evMaker = new EventMaker();
		evMaker.setSystemCore(core);
		DGeoLayer newLayer = evMaker.chooseOrMakeGeoEventLayer(Geometry.point, souLayer.isGeographic(), false, desiredLayerName);
		if (newLayer == null)
			return;
		if (par != null && par.isTemporal()) {
			evMaker.accountForEventTimeRange((TimeMoment) par.getFirstValue(), (TimeMoment) par.getLastValue());
		}
		newLayer.setSourceLayer(souLayer);
		this.evType = evType;
		DataTable evTbl = evMaker.getEventTable();
		tsNColN = evTbl.getAttrIndex(tsNColId);
		tsNameColN = evTbl.getAttrIndex(tsNameColId);
		tsValueColN = evTbl.getAttrIndex(tsValueColId);
		tsTimeWindowN = evTbl.getAttrIndex(tsTimeWindowId);
		Vector v = new Vector(3, 1);
		boolean attrAdded = false;
		if (tsNColN < 0) {
			evTbl.addAttribute("Time series N", tsNColId, AttributeTypes.integer);
			tsNColN = evTbl.getAttrCount() - 1;
			v.addElement(tsNColId);
			attrAdded = true;
		}
		if (tsNameColN < 0) {
			evTbl.addAttribute("Time series Id", tsNameColId, AttributeTypes.character);
			tsNameColN = evTbl.getAttrCount() - 1;
			v.addElement(tsNameColId);
			attrAdded = true;
		}
		if (tsValueColN < 0) {
			evTbl.addAttribute("Event value", tsValueColId, AttributeTypes.real);
			tsValueColN = evTbl.getAttrCount() - 1;
			v.addElement(tsValueColId);
			attrAdded = true;
		}
		if (tsTimeWindowN < 0) {
			evTbl.addAttribute("Event time window", tsTimeWindowId, AttributeTypes.integer);
			tsTimeWindowN = evTbl.getAttrCount() - 1;
			v.addElement(tsTimeWindowId);
			attrAdded = true;
		}
		if (attrAdded) {
			evTbl.notifyPropertyChange("new_attributes", null, v);
		}
		makingEventsFailed = false;
	}

	/**
	 * Adds a new event to the map layer with events
	 * @param recN - the index (record number) of the time series in the table
	 * @param recId - the identifier of the time series
	 * @param recName - the name of the time series
	 * @param currTime - the time moment at which the event occurred
	 * @param value - the value attained at this moment
	 */
	protected void updateEventLayer(int recN, String recId, String recName, TimeMoment currTime, long intervalLength, double value, int timeWindow) {
		if (evMaker == null || souLayer == null)
			return;
		GeoObject souObj = souLayer.findObjectById(recId);
		if (souObj == null)
			return;
		Geometry geom = souObj.getGeometry();
		if (geom == null)
			return;
		TimeMoment endTime = currTime.getCopy();
		endTime.add(intervalLength);
		DGeoObject eo = evMaker.addEvent(recN + "_" + currTime.toString() + "-" + timeWindow, evType, SpatialEntity.getCentre(geom), currTime, endTime);
		if (eo != null) {
			DataRecord rec = (DataRecord) eo.getData();
			rec.setNumericAttrValue(recN, String.valueOf(recN), tsNColN);
			rec.setAttrValue(recName, tsNameColN);
			rec.setNumericAttrValue(value, String.valueOf(value), tsValueColN);
			rec.setNumericAttrValue(timeWindow, String.valueOf(timeWindow), tsTimeWindowN);
		}
	}

	/**
	 * Finalizes the generation of the map layer with the events
	 */
	protected void finishMakingEvents() {
		if (evMaker == null)
			return;
		evMaker.finishLayerBuilding();
		tigr.setEventLayer(evMaker.getEventTable());
		evMaker = null;
	}

	/**
	 * vvIds: Ids of objects (time series) having events in each time moment
	 * vvEvtNums: Integers representing record numbbers of events in the event table
	 */
	protected Vector<Vector<String>> vvIds = null;
	protected Vector<Vector<Integer>> vvEvtNums = null;

	protected void createTimeCanvas() {
		TimeMoment times[] = new TimeMoment[par.getValueCount()];
		for (int i = 0; i < par.getValueCount(); i++) {
			times[i] = (TimeMoment) par.getValue(i);
		}
		taCanvas = new TimeArrangerWithIDsCanvas(table, tigr, tigr.getEventTable(), lResults, highlighter, times, vvIds, vvEvtNums);
		this.removeAll();
		add(pControls, BorderLayout.NORTH);
		add(taCanvas, BorderLayout.CENTER);
		add(pResults, BorderLayout.SOUTH);
		invalidate();
		validate();
	}

	protected void create2dTimeCanvas() {
		new PeriodicEventBarDialog(CManager.getAnyFrame(this), par, highlighter, tigr, vvIds, vvEvtNums);
	}

	protected void removeTimeCanvas() {
		this.removeAll();
		add(pControls, BorderLayout.NORTH);
		invalidate();
		validate();
	}

	protected void redrawTimeCanvas() {
		if (taCanvas != null) {
			taCanvas.componentResized(null);
			taCanvas.redraw();
		}
	}

}
