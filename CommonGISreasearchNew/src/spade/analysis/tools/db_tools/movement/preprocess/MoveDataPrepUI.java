package spade.analysis.tools.db_tools.movement.preprocess;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.NotificationLine;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.TextCanvas;
import spade.lib.util.StringUtil;
import db_work.data_descr.ColumnDescriptor;
import db_work.data_descr.ColumnDescriptorDate;
import db_work.data_descr.ColumnDescriptorNum;
import db_work.data_descr.TableDescriptor;
import db_work.database.OracleConnector;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Jul 7, 2010
 * Time: 2:58:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class MoveDataPrepUI implements WindowListener {
	/**
	 * Mandatory meanings: the table to process MUST contain columns with these
	 * meanings.
	 */
	protected static final String MEANINGS[] = { "X (longitude)", "Y (latitude)", "Time" };
	/**
	 * Optional meanings: the table to process may also contain columns with these
	 * meanings.
	 */
	protected static final String OPTMEANINGS[] = { "entity identifier" };
	protected static final String MEANINGS1[] = { MEANINGS[0], MEANINGS[1], MEANINGS[2], OPTMEANINGS[0], "time to next measurement", "distance to next measurement", "time of next measurement" };
	protected static final String OPTMEANINGS1[] = {};

	protected String meanings[] = MEANINGS, optMeanings[] = OPTMEANINGS;
	/**
	 * Table column numbers corresponding to the mandatory meanings
	 */
	protected int[] mandColNumbers = null;
	/**
	 * Table column numbers corresponding to the optional meanings
	 */
	protected int[] optColNumbers = null;
	/**
	 * The reader used for connection to the database and processing the data
	 */
	protected OracleConnector reader = null;
	/**
	 * The descriptor of the table with the source data
	 */
	protected TableDescriptor sourceTableDescr = null;
	/**
	 * The main frame of the wizard where, in particular, messages are shown
	 */
	protected Frame mainFrame = null;
	/**
	 * The component where the current wizard step is described
	 */
	protected TextCanvas taskTC = null;
	/**
	 * The notification line for messages
	 */
	protected NotificationLine lStatus = null;
	/**
	 * Indicates a failure of some operation
	 */
	protected boolean error = false;

	protected String destTableName = null;
	protected String dummyEntityID = null;
	protected String extraAttrCNames = null, extraAttrColumns[] = null;
	protected boolean deleteTmpTables[] = null;

	protected boolean bTimeIsPhysical = false, bCoordinatesAreGeo = false;

	/**
	 * Runs the wisard for the data transformation
	 */
	public void runPreprocessor() {
		mainFrame = new Frame("Movement Data Processor");
		mainFrame.setVisible(true);
		Metrics.setFontMetrics(mainFrame.getGraphics());
		mainFrame.setVisible(false);
		mainFrame.setLayout(new BorderLayout());
		Panel p = new Panel(new ColumnLayout());
		p.add(new Label("V-Analytics - Geospatial Visual Analytics", Label.CENTER));
		p.add(new Label("A wizard for processing movement data", Label.CENTER));
		p.add(new Label("version 30 December 2010", Label.CENTER));
		p.add(new Line(false));
		p.add(new Label("Records of movement are connected into sequences"));
		p.add(new Label("Duplicated records are merged"));
		p.add(new Label("Optionally, stationary records are removed"));
		p.add(new Label("Derived attributes (distance, speed etc.) are computed"));
		mainFrame.add(p, BorderLayout.NORTH);
		taskTC = new TextCanvas();
		taskTC.addTextLine("Step 1/10: connect to the database and choose the table with movement data");
		taskTC.setBackground(new Color(255, 255, 190));
		taskTC.setForeground(Color.blue.darker());
		mainFrame.add(taskTC, BorderLayout.CENTER);
		lStatus = new NotificationLine(null);
		mainFrame.add(lStatus, BorderLayout.SOUTH);
		showMessage(" ", false);
		mainFrame.pack();
		mainFrame.setVisible(true);
		mainFrame.addWindowListener(this);

		if (!connectToDatabase()) {
			showMessage("Table processing interrupted!", true);
			return;
		}
		if (!connectToTable()) {
			showMessage("Table processing interrupted!", true);
			reader.closeConnection();
			return;
		}
		if (!getColumnSemantics()) {
			showMessage("Table processing interrupted!", true);
			reader.closeConnection();
			return;
		}
		;
		reader.dropTmpTable(destTableName + "_se");
		reader.closeConnection();
	}

	/**
	 * Tries to connect to the database. Returns true if successful.
	 */
	protected boolean connectToDatabase() {
		if (reader != null)
			return true; //already connected
		reader = new OracleConnector();
		if (!reader.openConnection(true)) {
			reader = null;
			error = true;
			showMessage("Could not connect to the database!", true);
			return false;
		}
		showMessage("Successfully connected to the database!", false);
		showTask("* " + reader.getUserName() + "@" + reader.getComputerName() + ":" + reader.getDatabaseName());
		reader.setFrame(mainFrame);
		return true;
	}

	/**
	 * Proposes the user to choose a table and tries to connect to the
	 * table from the database.
	 */
	protected boolean connectToTable() {
		if (reader == null)
			return false;
		showTask("Step 2/10: connect to the table containing movement data");
		if (!reader.loadTableDescriptor(0, null, false)) {
			error = true;
			showMessage("Failed to connect to the table!", true);
			return false;
		}
		showTask("* input=" + reader.getTableDescriptor(0).tableName);
		showMessage("The table has been successfully connected!", false);
		return true;
	}

	/**
	 * Asks the user to provide meta-information about the structure of a table
	 * containing movement data: which of the columns contain coordinates, time,
	 * entity identifier, trajectory identifier (if any), etc.
	 */
	protected boolean getColumnSemantics() {
		if (reader == null || reader.getTableDescriptor(0) == null)
			return false;
		TableDescriptor td = reader.getTableDescriptor(0);
		showTask("Step 3/10: specify the relevant columns and their meanings");
		ColumnSemanticsUI semUI = new ColumnSemanticsUI(reader.getTableDescriptor(0), meanings, optMeanings);
		OKDialog dia = new OKDialog(mainFrame, "Column Semantics", true);
		dia.addContent(semUI);
		dia.show();
		if (dia.wasCancelled()) {
			showMessage("The required column meanings have not been provided!", true);
			return false;
		}
		mandColNumbers = semUI.getMandatoryColumnNumbers();
		optColNumbers = semUI.getOptionalColumnNumbers();
		String dateCName = null, //"date_and_time"
		xCName = null, //"long_1"
		yCName = null, //"lat"
		idCName = null; //
		if (mandColNumbers[0] >= 0) {
			xCName = td.getColumnDescriptor(mandColNumbers[0]).name;
		}
		if (mandColNumbers[1] >= 0) {
			yCName = td.getColumnDescriptor(mandColNumbers[1]).name;
		}
		if (mandColNumbers[2] >= 0) {
			dateCName = td.getColumnDescriptor(mandColNumbers[2]).name;
		}
		if (optColNumbers != null && optColNumbers[0] >= 0) {
			idCName = td.getColumnDescriptor(optColNumbers[0]).name;
		}
		// extra dialog: table prefix, ids, deletion of tmp tables, extra columns from src table
		Vector extraCNames = new Vector(20, 20);
		for (int i = 0; i < td.getNColumns(); i++) {
			String cn = td.getColumnDescriptor(i).name;
			if (!cn.equals(xCName) && !cn.equals(yCName) && !cn.equals(dateCName) && !cn.equals(idCName)) {
				extraCNames.addElement(cn);
			}
		}
		showTask("Step 4/10: checking nature of time and space");
		ColumnDescriptor cds[] = new ColumnDescriptor[mandColNumbers.length];
		for (int i = 0; i < mandColNumbers.length; i++) {
			cds[i] = td.getColumnDescriptor(mandColNumbers[i]);
		}
		reader.getStatsForColumnDescriptors(td, cds);
		bTimeIsPhysical = td.getColumnDescriptor(mandColNumbers[2]).type.equals("DATE");
		String stmin = "", stmax = "";
		Panel p = new Panel(new ColumnLayout());
		p.add(new Label("Time:"));
		p.add(new Label("Database type=" + td.getColumnDescriptor(mandColNumbers[2]).type));
		if (td.getColumnDescriptor(mandColNumbers[2]) instanceof ColumnDescriptorNum) {
			ColumnDescriptorNum cdn = (ColumnDescriptorNum) td.getColumnDescriptor(mandColNumbers[2]);
			stmin = "" + cdn.min;
			stmax = "" + cdn.max;
		}
		if (td.getColumnDescriptor(mandColNumbers[2]) instanceof ColumnDescriptorDate) {
			ColumnDescriptorDate cdd = (ColumnDescriptorDate) td.getColumnDescriptor(mandColNumbers[2]);
			stmin = "" + cdd.min;
			stmax = "" + cdd.max;
		}
		p.add(new Label("Min=" + stmin + " , Max=" + stmax));
		p.add(new Label("*** " + ((bTimeIsPhysical) ? "Is physical time" : "Is abstract time")));
		p.add(new Line(false));
		boolean isGeo = true;
		p.add(new Label("X (Longitude):"));
		p.add(new Label("Database type=" + td.getColumnDescriptor(mandColNumbers[0]).type));
		ColumnDescriptorNum cdn = (ColumnDescriptorNum) td.getColumnDescriptor(mandColNumbers[0]);
		stmin = "" + cdn.min;
		stmax = "" + cdn.max;
		isGeo = cdn.min >= -180 && cdn.min <= 180;
		p.add(new Label("Min=" + stmin + " , Max=" + stmax));
		p.add(new Label("Y (Latitude):"));
		p.add(new Label("Database type=" + td.getColumnDescriptor(mandColNumbers[1]).type));
		cdn = (ColumnDescriptorNum) td.getColumnDescriptor(mandColNumbers[1]);
		stmin = "" + cdn.min;
		stmax = "" + cdn.max;
		isGeo = isGeo && cdn.min >= -90 && cdn.min <= 90;
		p.add(new Label("Min=" + stmin + " , Max=" + stmax));
		Checkbox cbIsGeo = new Checkbox("Coordinates are geographical", isGeo);
		cbIsGeo.setEnabled(isGeo);
		p.add(cbIsGeo);
		p.add(new Line(false));
		OKDialog ok = new OKDialog(mainFrame, "Nature of space/time in data", true);
		ok.addContent(p);
		ok.show();
		if (ok.wasCancelled())
			return false;
		bCoordinatesAreGeo = cbIsGeo.getState();
		showTask("* time is " + ((bTimeIsPhysical) ? "physical" : "abstract"));
		showTask("* coordinates are " + ((bCoordinatesAreGeo) ? "geographical" : "abstract"));
		showTask("Step 5/10: checking uniqueness of id+date");
		String columnNames[] = null;
		if (idCName == null) {
			columnNames = new String[1];
			columnNames[0] = dateCName;
		} else {
			columnNames = new String[2];
			columnNames[0] = idCName;
			columnNames[1] = dateCName;
		}
		Vector v = reader.checkIfValuesAreUnique(reader.getTableDescriptor(0).tableName, columnNames);
		if (v != null && !askUserToContinue(v))
			return false;
		if (v == null) {
			System.out.println("* all combinations are unique");
			showTask("* all combinations are unique");
		} else {
			System.out.println("* There are duplicates...");
			showTask("* there are duplicates...");
			showTask("* details: see " + reader.getTableDescriptor(0).tableName + "_DUPLICATES");
		}
		showTask("Step 6/10: define the resulting table");
		TableDefinitionUI tableDefinitionUI = new TableDefinitionUI(reader.getTableDescriptor(0).tableName, bCoordinatesAreGeo, xCName, yCName, dateCName, idCName, extraCNames);
		ok = new OKDialog(mainFrame, "Table Definition", true);
		ok.addContent(tableDefinitionUI);
		ok.show();
		if (ok.wasCancelled())
			return false;
		destTableName = tableDefinitionUI.getTableName();
		dummyEntityID = tableDefinitionUI.getDummyID();
		extraAttrCNames = "";
		extraAttrColumns = tableDefinitionUI.getExtraCNames();
		if (extraAttrColumns != null && extraAttrColumns.length > 0) {
			for (String extraAttrColumn : extraAttrColumns) {
				extraAttrCNames += "," + extraAttrColumn;
			}
		}
		showMessage("Performing calculations in ORACLE...", false);
		long t0 = System.currentTimeMillis();
		if (calculateDerivedAttributes(xCName, yCName, dateCName, idCName, v != null)) {
			long t1 = System.currentTimeMillis() - t0;
			showTask("Ready. Elapsed time " + StringUtil.floatToStr(t1 / 1000f, 3) + " (s)");
			showMessage("Table " + destTableName + " successfully created!", false);
			if (Dialogs.askYesOrNo(mainFrame, "Do you want to store log in a file?", "Store report?")) {
				FileDialog fd = new FileDialog(mainFrame, "Specify the file to store the distances");
				fd.setFile(reader.getTableDescriptor(0).tableName + ".txt");
				fd.setMode(FileDialog.SAVE);
				fd.show();
				if (fd.getDirectory() != null) {
					String fname = fd.getDirectory() + fd.getFile();
					FileOutputStream out = null;
					try {
						out = new FileOutputStream(fname);
					} catch (IOException ioe) {
						showMessage("Could not create file " + fname, true);
						System.out.println(ioe);
					}
					DataOutputStream dos = new DataOutputStream(out);
					try {
						dos.writeBytes("V-Analytics: Geospatial Visual Analytics System a.k.a. CommonGIS\r\n");
						dos.writeBytes("                 Trajectory preprocessor\r\n");
						dos.writeBytes("                  version 30 December 2010\r\n\r\n\r\n");
						dos.writeBytes(taskTC.getTextWithLineBreaks() + "\r\n\r\n");
					} catch (IOException ioe) {
						showMessage("Error writing file " + fname, true);
						System.out.println(ioe);
					}
					try {
						out.close();
					} catch (IOException e) {
					}
				}
			}
		} else {
			showMessage("Table creation failed!", true);
			return false;
		}
		//
		meanings = MEANINGS1;
		optMeanings = OPTMEANINGS1;
		mandColNumbers = new int[7];
		for (int i = 0; i < mandColNumbers.length; i++) {
			mandColNumbers[i] = i;
		}
		optColNumbers = null;
		return true;
	}

	protected boolean calculateDerivedAttributes(String xCName, String yCName, String dateCName, String idCName, boolean bCleanDuplicates) {
		if (reader == null || reader.getTableDescriptor(0) == null)
			return false;
		boolean ok = true;
		showTask("Step 7/10: creating NoDuplicates table " + reader.getTableDescriptor(0).tableName + "_ND");
		reader.dropTmpTable(reader.getTableDescriptor(0).tableName + "_ND");
		//reader.dropTmpTable(reader.getTableDescriptor(0).tableName+"_ND__");
		String srcTblName = reader.getTableDescriptor(0).tableName;
		ok = reader.createNDtable(bCleanDuplicates, reader.getTableDescriptor(0).tableName, idCName, dummyEntityID, dateCName, xCName, yCName, extraAttrCNames);
		if (!ok)
			return false;
		srcTblName += "_ND";
		showTask("* table w/out duplicates: " + srcTblName);
		showTask("Step 8/10: making trajectories");
		StringBuffer sb = new StringBuffer(destTableName);
		sb.insert(destTableName.indexOf('$'), "_ND");
		destTableName = sb.toString();
		reader.dropTmpTable(destTableName);
		ok = reader.createTrajectories(destTableName, srcTblName, idCName, dummyEntityID, dateCName, xCName, yCName, bCoordinatesAreGeo, bTimeIsPhysical);
		showTask("* trajectories: " + destTableName);
		if (!ok)
			return false;
		//if (bCleanDuplicates)
		//reader.dropTmpTable(reader.getTableDescriptor(0).tableName+"_ND__");
		showTask("Step 9/10: removing stationary points");
		float speed = 1f;
		EstimateNStationaryPointsUI nspUI = new EstimateNStationaryPointsUI(reader, destTableName, extraAttrColumns, speed, bCoordinatesAreGeo);
		OKDialog dlg = new OKDialog(mainFrame, "Removing stationary points", true);
		dlg.addContent(nspUI);
		dlg.show();
		if (dlg.wasCancelled()) {
			showTask("* skipped");
			return true;
		}
		reader.dropTmpTable(reader.getTableDescriptor(0).tableName + "_NDS");
		reader.dropTmpTable(reader.getTableDescriptor(0).tableName + "_NDR");
		speed = nspUI.speed;
		showTask("* speed threshold=" + speed + ((nspUI.getSpeedAttr() == null) ? " (computed)" : " (computed or measured)"));
		ok = reader.removeStationaryPoints(destTableName, reader.getTableDescriptor(0).tableName, speed, nspUI.getSpeedAttr(), extraAttrCNames);
		if (!ok) {
			showTask("* removing stationary points skipped");
			return false;
		}
		showTask("* table w/out duplicates & stops: " + reader.getTableDescriptor(0).tableName + "_NDS");
		showTask("Step 10/10: making trajectories");
		sb = new StringBuffer(destTableName);
		sb.insert(destTableName.indexOf('$'), "S");
		destTableName = sb.toString();
		reader.dropTmpTable(reader.getTableDescriptor(0).tableName + "_NDS__");
		reader.dropTmpTable(destTableName);
		ok = reader.createTrajectories(destTableName, reader.getTableDescriptor(0).tableName + "_NDS", idCName, dummyEntityID, dateCName, xCName, yCName, bCoordinatesAreGeo, bTimeIsPhysical);
		showTask("* trajectories: " + destTableName);
		reader.dropTmpTable(reader.getTableDescriptor(0).tableName + "_NDR");
		reader.dropTmpTable(reader.getTableDescriptor(0).tableName + "_NDS__");
		return ok;
	}

	public boolean askUserToContinue(Vector v) {
		Panel p = new Panel(new BorderLayout());
		Long count = ((Long) v.elementAt(0)).longValue(), total = ((Long) v.elementAt(1)).longValue();
		Panel pp = new Panel(new ColumnLayout());
		p.add(pp, BorderLayout.NORTH);
		pp.add(new Label("Following " + count + " combinations occur several times,"));
		pp.add(new Label("" + total + " times in total"));
		if (count > 100) {
			pp.add(new Label("(only 100 of them are displayed)"));
		}
		List l = new List(10);
		for (int i = 2; i < v.size(); i += 2) {
			String cols[] = (String[]) v.elementAt(i);
			long n = ((Long) v.elementAt(i + 1)).intValue();
			String st = cols[0];
			for (int c = 1; c < cols.length; c++) {
				st += "," + cols[c];
			}
			st += ": " + n;
			l.add(st);
		}
		p.add(l, BorderLayout.CENTER);
		pp = new Panel(new ColumnLayout());
		p.add(pp, BorderLayout.SOUTH);
		pp.add(new Label("Do you want to continue?"));
		pp.add(new Label("(duplicating records will be merged)"));
		pp.add(new Line(false));
		OKDialog ok = new OKDialog(mainFrame, "Problem: duplicating combinations", true);
		ok.addContent(p);
		ok.show();
		if (ok.wasCancelled()) {
			showMessage("Table creation failed!", true);
			return false;
		} else
			return true;
	}

	/**
	* Shows the given message. If "trouble" is true, then this
	* is an error message.
	*/
	public void showMessage(String msg, boolean trouble) {
		if (lStatus != null) {
			lStatus.showMessage(msg, trouble);
		}
		if (trouble) {
			System.out.println("ERROR: " + msg);
		}
	}

	/**
	* Shows the given message. Assumes that this is not an error message.
	*/
	public void showMessage(String msg) {
		showMessage(msg, false);
	}

	/**
	* Clears the status line
	*/
	public void clearStatusLine() {
		if (lStatus != null) {
			lStatus.showMessage(null, false);
		}
	}

	/**
	 * Puts the given text in the task field of the main window
	 */
	protected void showTask(String txt) {
		taskTC.addTextLine(txt);
		taskTC.invalidate();
		mainFrame.validate();
		Dimension d1 = mainFrame.getSize(), d2 = mainFrame.getPreferredSize();
		if (d2.width > d1.width || d2.height > d1.height) {
			mainFrame.setSize(d2.width, d2.height);
			mainFrame.validate();
		}
	}

	/**
	 * Finishes the work: closes database connection, closes the main window,
	 * exits from the system.
	 */
	public void finish(int code) {
		if (reader != null) {
			reader.closeConnection();
		}
		if (mainFrame != null) {
			mainFrame.dispose();
		}
		System.exit(code);
	}

	public void windowClosing(WindowEvent e) {
		if (e.getSource().equals(mainFrame)) {
			finish((error) ? 1 : 0);
		}
	}

	public void windowClosed(WindowEvent e) {
	}

	public void windowOpened(WindowEvent e) {
	};

	public void windowIconified(WindowEvent e) {
	};

	public void windowDeiconified(WindowEvent e) {
	};

	public void windowActivated(WindowEvent e) {
	};

	public void windowDeactivated(WindowEvent e) {
	};
}
