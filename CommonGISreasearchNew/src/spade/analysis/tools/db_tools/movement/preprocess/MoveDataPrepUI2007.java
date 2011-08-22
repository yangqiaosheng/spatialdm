package spade.analysis.tools.db_tools.movement.preprocess;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Vector;

import spade.lib.basicwin.ColumnLayout;
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
 * Creator: N.Andrienko
 * Date: 01-Dec-2006
 * Time: 15:39:18
 * This is a wizard to run a series of dialogs for preprocessing of movement data
 */
public class MoveDataPrepUI2007 implements WindowListener {
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
	protected String extraAttrCNames = null;
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
		p.add(new Label("Geospatial Visual Analytics Toolkit", Label.CENTER));
		p.add(new Label("A wizard for processing movement data", Label.CENTER));
		p.add(new Label("version 22 October 2009", Label.CENTER));
		p.add(new Line(false));
		p.add(new Label("Records of movement are connected into sequences."));
		p.add(new Label("Derived attributes (distance, speed etc.) are computed"));
		mainFrame.add(p, BorderLayout.NORTH);
		taskTC = new TextCanvas();
		taskTC.addTextLine("Step 1/7: connect to the database and choose the table with movement data");
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
		showTask("Step 2/7: connect to the table containing movement data");
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
		showTask("Step 3/7: specify the relevant columns and their meanings");
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
		showTask("Step 4/7: checking nature of time and space");
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
		showTask("Step 5/7: checking uniqueness of id+date");
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
			showTask("* There are duplicates...");
		}
		showTask("Step 6/7: define the resulting table");
		TableDefinitionUI tableDefinitionUI = new TableDefinitionUI(reader.getTableDescriptor(0).tableName, bCoordinatesAreGeo, xCName, yCName, dateCName, idCName, extraCNames);
		ok = new OKDialog(mainFrame, "Table Definition", true);
		ok.addContent(tableDefinitionUI);
		ok.show();
		if (ok.wasCancelled())
			return false;
		destTableName = tableDefinitionUI.getTableName();
		showTask("* output=" + destTableName);
		dummyEntityID = tableDefinitionUI.getDummyID();
		extraAttrCNames = "";
		String st[] = tableDefinitionUI.getExtraCNames();
		if (st != null && st.length > 0) {
			for (String element : st) {
				extraAttrCNames += "," + element;
			}
		}
		showMessage("Performing calculations in ORACLE...", false);
		long t0 = System.currentTimeMillis();
		if (calculateDerivedAttributes(xCName, yCName, dateCName, idCName, v != null)) {
			long t1 = System.currentTimeMillis() - t0;
			showTask("Ready. Elapsed time " + StringUtil.floatToStr(t1 / 1000f, 3) + " (s)");
			showMessage("Table " + destTableName + " successfully created!", false);
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
		showTask("Step 7/7: calculate derived attributes");
		return reader.deriveMovementCharacteristics(destTableName, reader.getTableDescriptor(0).tableName, idCName, dummyEntityID, dateCName, xCName, yCName, extraAttrCNames, bCoordinatesAreGeo, bTimeIsPhysical, bCleanDuplicates);
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
		pp.add(new Label("(duplicating records will be skipped)"));
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

	@Override
	public void windowClosing(WindowEvent e) {
		if (e.getSource().equals(mainFrame)) {
			finish((error) ? 1 : 0);
		}
	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowOpened(WindowEvent e) {
	};

	@Override
	public void windowIconified(WindowEvent e) {
	};

	@Override
	public void windowDeiconified(WindowEvent e) {
	};

	public void windowActivated(WindowEvent e) {
	};

	public void windowDeactivated(WindowEvent e) {
	};
}
