package spade.analysis.tools.db_tools;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.Point;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.analysis.system.Processor;
import spade.analysis.system.SystemUI;
import spade.analysis.tools.DataAnalyser;
import spade.analysis.tools.GridBuildPanel;
import spade.analysis.tools.SingleInstanceTool;
import spade.analysis.tools.clustering.ClusterSpecimenInfo;
import spade.analysis.tools.clustering.ClustersInfo;
import spade.analysis.tools.clustering.ObjectToClusterAssignment;
import spade.analysis.tools.clustering.ObjectsToClustersAssigner;
import spade.analysis.tools.clustering.SingleClusterInfo;
import spade.analysis.tools.db_tools.movement.SamplingPanel;
import spade.analysis.tools.db_tools.movement.TrajectorySeparator;
import spade.analysis.tools.db_tools.statistics.TableStatisticsDisplay;
import spade.analysis.tools.db_tools.statistics.TableStatisticsItem;
import spade.analysis.tools.events.IncrementalEventsSummarizer;
import spade.analysis.tools.moves.ClassifierBasedSummarizer;
import spade.analysis.tools.moves.DirectionAndSpeedVisualizer;
import spade.analysis.tools.moves.IncrementalMovesSummariser;
import spade.analysis.tools.moves.MovementToolRegister;
import spade.analysis.tools.moves.TrajectoriesTableBuilder;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.MultiSelector;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.TextCanvas;
import spade.lib.util.FloatArray;
import spade.lib.util.IdMaker;
import spade.lib.util.IntArray;
import spade.lib.util.LongArray;
import spade.lib.util.StringUtil;
import spade.lib.util.WordCounter;
import spade.time.Date;
import spade.time.TimeCount;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.time.ui.EnterDateSchemeUI;
import spade.time.ui.TimeDialogs;
import spade.vis.action.HighlightListener;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.Parameter;
import spade.vis.database.SpatialEntity;
import spade.vis.database.TableFilter;
import spade.vis.dataview.TableViewer;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DLayerManager;
import spade.vis.dmap.DMovingObject;
import spade.vis.dmap.DVectorGridLayer;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapViewer;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.spec.DataSourceSpec;
import spade.vis.spec.LinkDataDescription;
import core.ActionDescr;
import core.ResultDescr;
import data_load.LayerFromTableGenerator;
import db_work.data_descr.ColumnDescriptor;
import db_work.data_descr.ColumnDescriptorDate;
import db_work.data_descr.ColumnDescriptorNum;
import db_work.data_descr.ColumnDescriptorQual;
import db_work.data_descr.TableDescriptor;
import db_work.database.JDBCConnector;
import db_work.database.OracleConnector;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 22-Dec-2006
 * Time: 14:09:23
 * Allows the user to establish a connection to a database table with point
 * data (e.g. events or positions of moving objects at different times)
 * and perform certain types of database queries. In particular, the user can
 * aggregate the data by spatial compartments (cells of a rectangular grid)
 * and, possibly, temporal intervals, and load the aggregated data into the
 * system, where they can be visualised and analysed. The result of aggregation
 * is a vector grid layer (DVectorGridLayer) with an associated table, which
 * contains attribute values (possibly, time series) corresponding to the
 * grid cells.
 */
public class DatabaseAnalyser implements DataAnalyser, SingleInstanceTool, ActionListener, WindowListener {
	private Frame frame = null, dialogFrame = null;

	protected ESDACore core = null;
	/**
	 * Known tables: the tool has already connected to them earlier and
	 * acquired metadata from the user. The elements are instances of
	 * DBTableDescriptor.
	 */
	protected Vector tdescr = null;
	/**
	 * The "connectors" (instances of JDBCConnector) used for retrieving
	 * data from the table and performing various database queries.
	 */
	protected Vector connectors = null;
	/**
	 * Descriptor of the current table (i.e. the one under analysis)
	 */
	protected DBTableDescriptor currTblD = null;
	/**
	 * The database connector for the current table
	 */
	protected JDBCConnector currTblConnector = null;
	/**
	 * The list with the names of the tables
	 */
	protected List tblList = null;
	/**
	 * Indicates that a non-modal dialog with the user is currently in progress.
	 * When dialogInProgress is true, the tool does not react to any command
	 * or event.
	 */
	protected boolean dialogInProgress = false;

	/**
	* Returns true when the tool has everything necessary for its operation.
	* A GridBuilder does not need any additional classes and therefore always
	* returns true.
	*/
	public boolean isValid(ESDACore core) {
		return true;
	}

	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	public void run(ESDACore core) {
		if (frame != null)
			return;
		this.core = core;
		if (tdescr != null && tdescr.size() > 0) {
			makeWindow();
		} else {
			addNewTable();
		}
	}

	protected void makeWindow() {
		if (tdescr == null || tdescr.size() < 1)
			return;
		tblList = new List(10);
		for (int i = 0; i < tdescr.size(); i++) {
			DBTableDescriptor td = (DBTableDescriptor) tdescr.elementAt(i);
			tblList.add(((td.name != null) ? td.name : td.dbTableName) + "@" + td.computerName);
		}
		tblList.addActionListener(this);
		Panel p = new Panel(new BorderLayout());
		p.add(tblList, BorderLayout.CENTER);
		Panel pp = new Panel(new ColumnLayout());
		Panel bp = new Panel(new FlowLayout(FlowLayout.CENTER, 10, 2));
		Button b = new Button("Get column information");
		b.setActionCommand("column_info");
		b.addActionListener(this);
		bp.add(b);
		b = new Button("Analyse");
		b.setActionCommand("analyse");
		b.addActionListener(this);
		bp.add(b);
		pp.add(bp);
		bp = new Panel(new FlowLayout(FlowLayout.RIGHT, 10, 2));
		b = new Button("Open a new table");
		b.setActionCommand("add");
		b.addActionListener(this);
		bp.add(b);
		pp.add(bp);
		p.add(pp, BorderLayout.SOUTH);
		tblList.select(0);
		frame = new Frame("Database analyser");
		frame.setLayout(new BorderLayout());
		frame.add(p, BorderLayout.CENTER);
		frame.pack();
		Dimension ss = Toolkit.getDefaultToolkit().getScreenSize(), fs = frame.getSize();
		Frame mainFrame = core.getUI().getMainFrame();
		int x = ss.width - fs.width - 50, y = 50;
		if (mainFrame != null && mainFrame.isVisible()) {
			Point l = mainFrame.getLocation();
			Dimension mfs = mainFrame.getSize();
			if (l != null && mfs != null) {
				y = l.y;
				if (y + fs.height > ss.height) {
					y = ss.height - fs.height;
				}
				x = l.x + mfs.width;
				if (x + fs.width > ss.width) {
					x = ss.width - fs.width;
				}
			}
		}
		frame.setLocation(x, y);
		frame.addWindowListener(this);
		frame.setVisible(true);
	}

	protected void addNewTable() {
		List list = new List();
		for (String element : DBConnectorManager.DB_FORMATS) {
			list.add(element);
		}
		list.select(0);
		OKDialog ok = new OKDialog(getFrame(), "Database format?", true);
		ok.addContent(list);
		ok.show();
		if (ok.wasCancelled())
			return;
		int idx = list.getSelectedIndex();
		if (idx < 0)
			return;
		JDBCConnector reader = DBConnectorManager.getConnector(idx);
		if (reader == null) {
			showMessage(DBConnectorManager.errMsg);
			return;
		}
		if (tdescr != null && tdescr.size() > 0) {
			DBTableDescriptor dbtd = (DBTableDescriptor) tdescr.elementAt(tdescr.size() - 1);
			if (reader.getURLPrefix().equals(dbtd.urlPrefix)) {
				reader.setComputerName(dbtd.computerName);
				reader.setDatabaseName(dbtd.databaseName);
				reader.setPort(dbtd.port);
				reader.setPSW(dbtd.psw);
				reader.setUserName(dbtd.userName);
			}
		}
		if (!reader.openConnection(true)) {
			showMessage(reader.getErrorMessage(), true);
			return;
		}
		showMessage("Successfully connected to the database!", false);
		if (!reader.loadTableDescriptor(0, null, false)) {
			showMessage("Failed to connect to the table!", true);
			return;
		}
		showMessage("The table has been successfully connected!", false);

		TableDescriptor td = reader.getTableDescriptor(0);
		list = new List(Math.min(10, td.getNColumns()));
		for (int i = 0; i < td.getNColumns(); i++) {
			ColumnDescriptor cd = td.getColumnDescriptor(i);
			list.add(cd.name + " (" + cd.type + ")");
		}
		Panel pp = new Panel(new ColumnLayout());
		pp.add(new Label("Table: " + td.tableName, Label.CENTER));
		Panel p = new Panel(new BorderLayout());
		p.add(new Label("Name:"), BorderLayout.WEST);
		TextField nameTF = new TextField(td.tableName, 30);
		p.add(nameTF, BorderLayout.CENTER);
		pp.add(p);
		p = new Panel(new BorderLayout());
		p.add(pp, BorderLayout.NORTH);
		p.add(list, BorderLayout.CENTER);
		pp = new Panel(new ColumnLayout());
		pp.add(new Label("Type of data in the table:", Label.CENTER));
		CheckboxGroup cbg = new CheckboxGroup();
		Checkbox typeCB[] = new Checkbox[DataSemantics.dataMeaningNames.length - 1];
		for (int i = 0; i < DataSemantics.dataMeaningNames.length - 1; i++) {
			typeCB[i] = new Checkbox(DataSemantics.dataMeaningNames[i + 1], false, cbg);
			pp.add(typeCB[i]);
			if (!DataSemantics.dataMeaningNames[i + 1].equalsIgnoreCase(DataSemantics.dataMeaningTexts[i + 1])) {
				pp.add(new Label("- " + DataSemantics.dataMeaningTexts[i + 1], Label.RIGHT));
			}
		}
		p.add(pp, BorderLayout.EAST);
		ok = new OKDialog(getFrame(), "Data type?", true);
		ok.addContent(p);
		ok.show();
		if (ok.wasCancelled())
			//reader.closeConnection();
			return;
		DBTableDescriptor dbtd = new DBTableDescriptor();
		dbtd.computerName = reader.getComputerName();
		dbtd.databaseName = reader.getDatabaseName();
		dbtd.driver = reader.getDriver();
		dbtd.format = reader.getFormat();
		dbtd.port = reader.getPort();
		dbtd.psw = reader.getPSW();
		dbtd.urlPrefix = reader.getURLPrefix();
		dbtd.userName = reader.getUserName();
		dbtd.dbTableName = td.tableName;
		dbtd.name = nameTF.getText();
		if (dbtd.name == null || dbtd.name.trim().length() < 1) {
			dbtd.name = td.tableName;
		} else {
			dbtd.name = dbtd.name.trim();
		}
		idx = -1;
		for (int i = 0; i < typeCB.length && idx < 0; i++)
			if (typeCB[i].getState()) {
				idx = i;
			}
		if (idx >= 0) {
			dbtd.dataMeaning = idx + 1;
		}
		//get column semantics
		String meanings[] = null;
		int nMandMeanings = 0;
		String canonicFieldNames[] = null;
		switch (dbtd.dataMeaning) {
		case DataSemantics.STATIC_POINTS:
			meanings = DataSemantics.pointSemantics;
			nMandMeanings = DataSemantics.nMandPointSem;
			canonicFieldNames = DataSemantics.canonicFieldNamesPoints;
			break;
		case DataSemantics.EVENTS:
			meanings = DataSemantics.eventSemantics;
			nMandMeanings = DataSemantics.nMandEventSem;
			canonicFieldNames = DataSemantics.canonicFieldNamesEvents;
			break;
		case DataSemantics.MOVEMENTS:
			meanings = DataSemantics.movementSemantics;
			nMandMeanings = DataSemantics.nMandMoveSem;
			canonicFieldNames = DataSemantics.canonicFieldNamesMovement;
			break;
		case DataSemantics.TIME_RELATED:
			meanings = DataSemantics.timerelatedSemantics;
			nMandMeanings = DataSemantics.nMandTimerelatedSem;
			canonicFieldNames = DataSemantics.canonicFieldNamesTimerelated;
			break;
		}
		if (meanings != null) {
			ColumnSemanticsUI semUI = new ColumnSemanticsUI(td, meanings, nMandMeanings);
			semUI.setCanonicFieldNames(canonicFieldNames);
			ok = new OKDialog(getFrame(), "Column Semantics", true);
			ok.addContent(semUI);
			ok.show();
			if (ok.wasCancelled()) {
				showMessage("The required column meanings have not been provided!", true);
				//reader.closeConnection();
				return;
			}
			int colN[] = semUI.getColumnNumbers();
			if (colN != null) {
				dbtd.relevantColumns = new String[colN.length];
				for (int i = 0; i < colN.length; i++)
					if (colN[i] >= 0) {
						dbtd.relevantColumns[i] = new String(td.getColumnDescriptor(colN[i]).name);
						if (meanings[i].equalsIgnoreCase("x-coordinate")) {
							dbtd.xColIdx = colN[i];
							dbtd.xColName = dbtd.relevantColumns[i];
						} else if (meanings[i].equalsIgnoreCase("y-coordinate")) {
							dbtd.yColIdx = colN[i];
							dbtd.yColName = dbtd.relevantColumns[i];
						} else if (meanings[i].equalsIgnoreCase("time")) {
							dbtd.timeColIdx = colN[i];
							dbtd.timeColName = dbtd.relevantColumns[i];
						} else if (meanings[i].equalsIgnoreCase("event identifier") || meanings[i].equalsIgnoreCase("object identifier") || meanings[i].equalsIgnoreCase("entity identifier") || meanings[i].equalsIgnoreCase("identifier")) {
							dbtd.idColIdx = colN[i];
							dbtd.idColName = dbtd.relevantColumns[i];
						}
					}
				/*
				for (int i=0; i<dbtd.relevantColumns.length; i++) {
				  if (i>0) System.out.print(", ");
				  System.out.print("\""+dbtd.relevantColumns[i]+"\"");
				}
				*/
			}
		}
		//reader.closeConnection();
		if (tdescr == null) {
			tdescr = new Vector(10, 10);
		}
		tdescr.addElement(dbtd);
		if (connectors == null) {
			connectors = new Vector(10, 10);
		}
		connectors.addElement(reader);
		currTblD = dbtd;
		currTblConnector = reader;
		if (tblList != null) {
			tblList.add(dbtd.name + "@" + dbtd.computerName);
			tblList.select(tblList.getItemCount() - 1);
		} else {
			makeWindow();
		}
		// now check the nature of the coordinates and create OSM layer
		if (dbtd.dataMeaning == DataSemantics.MOVEMENTS || dbtd.dataMeaning == DataSemantics.EVENTS) {
			OracleConnector oc = (OracleConnector) currTblConnector;
			String tableName = currTblConnector.getTableDescriptor(0).tableName;
			int coordsAreGeographic = -1;
			if (core.getDataLoader() != null && core.getDataLoader().getMap(0) != null) {
				coordsAreGeographic = (core.getDataLoader().getMap(0).isGeographic()) ? 1 : 0;
			}
			// now compute the bounding rectangle
			float x1 = 0, y1 = 0, x2 = 0, y2 = 0;
			String sql = "";
			// collect basic statistics
			boolean mayBeGeogr = false;
			try {
				Statement statement = oc.getConnection().createStatement();
				sql = ((oc.checkIfTableOrViewExists(tableName + "_se")) ? "select min(br_x1),min(br_y1),max(br_x2),max(br_y2) from " + tableName + "_se" : "select min(" + dbtd.xColName + "),min(" + dbtd.yColName + "),max(" + dbtd.xColName + "),max("
						+ dbtd.yColName + ") from " + tableName);
				System.out.println("* <" + sql + ">");
				ResultSet result = statement.executeQuery(sql);
				result.next();
				x1 = result.getFloat(1);
				y1 = result.getFloat(2);
				x2 = result.getFloat(3);
				y2 = result.getFloat(4);
				mayBeGeogr = x1 >= -180 && y1 >= -90 && x2 <= 180 && y2 <= 90;
				statement.close();
			} catch (SQLException se) {
				System.out.println("SQL error:\n" + sql + "\n" + se.toString());
			}
			if (coordsAreGeographic == -1)
				if (tableName.indexOf("$$") >= 0) {
					coordsAreGeographic = 1;
				} else {
					if (coordsAreGeographic == -1 && mayBeGeogr) {
						coordsAreGeographic = (Dialogs.askYesOrNo(getFrame(), "Are the coordinates in the data geographic (latitudes and longitudes)?", "Geographic coordinates?")) ? 1 : 0;
					} else {
						coordsAreGeographic = 0;
					}
				}
			// create rectangle layer and load OSM
			DGeoLayer tLayer = new DGeoLayer();
			tLayer.setType(Geometry.area);
			tLayer.setName(tableName + ": bounding rectangle");
			DGeoObject dgo = new DGeoObject();
			SpatialEntity rspe = new SpatialEntity(tableName + ": bounding rectangle");
			rspe.setGeometry(new RealRectangle(x1, y1, x2, y2));
			dgo.setup(rspe);
			tLayer.addGeoObject(dgo);
			tLayer.setHasAllObjects(true);
			DrawingParameters dp = tLayer.getDrawingParameters();
			if (dp == null) {
				dp = new DrawingParameters();
				tLayer.setDrawingParameters(dp);
			}
			dp.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.5f * (float) Math.random(), 1 - 0.5f * (float) Math.random());
			dp.lineWidth = 1;
			dp.fillContours = false;
			dp.transparency = 0;
			DataLoader dLoader = core.getDataLoader();
			dLoader.addMapLayer(tLayer, -1, coordsAreGeographic);
		}
		//if (core.getDataLoader()!=null && core.getDataLoader().getMap(0)!=null)
		//System.out.println("* after: "+core.getDataLoader().getMap(0).isGeographic());
	}

	protected JDBCConnector connectToTable(DBTableDescriptor tbld, boolean askUser) {
		if (tbld == null || tbld.format == null)
			return null;
		JDBCConnector reader = DBConnectorManager.getConnector(tbld.format);
		if (reader == null) {
			showMessage(DBConnectorManager.errMsg);
			return null;
		}
		reader.setComputerName(tbld.computerName);
		reader.setDatabaseName(tbld.databaseName);
		reader.setPort(tbld.port);
		reader.setPSW(tbld.psw);
		reader.setUserName(tbld.userName);
		reader.setDBTableName(tbld.dbTableName);
		reader.setFrame(getFrame());
		if (!reader.openConnection(askUser)) {
			showMessage(reader.getErrorMessage(), true);
			return null;
		}
		if (!reader.loadTableDescriptor(0, null, false)) {
			showMessage("Failed to connect to the table!", true);
			//reader.closeConnection();
			return null;
		}
		showMessage("The table has been successfully connected!", false);
		//
		return reader;
	}

	/**
	 * Gets statistics about values in the column with the given number and
	 * fills it in the given TableStatisticsItem
	 */
	private void getColumnStat(JDBCConnector reader, TableDescriptor td, ColumnDescriptor cd, TableStatisticsItem stat) {
		if (reader == null || td == null || cd == null || stat == null)
			return;
		boolean isCharOrIdent = cd.type.contains("CHAR");
		if (!isCharOrIdent) {
			String str = stat.name.toLowerCase();
			isCharOrIdent = str.indexOf("ident") >= 0;
		}
		//obtain statistics of values in this column
		if (isCharOrIdent) {
			if (cd.nUniqueValues < 0) {
				reader.getColumnDescriptorDetails(td, cd);
			}
			//...get the number of different values
			long num = cd.nUniqueValues;
			stat.value = String.valueOf(num);
			stat.comment = "different values";
		} else if (cd instanceof ColumnDescriptorNum) {
			ColumnDescriptorNum cdn = (ColumnDescriptorNum) cd;
			//...get the range (minimum and maximum values)
			double num1 = cdn.min, num2 = cdn.max;
			stat.value = String.valueOf(num1);
			stat.maxValue = String.valueOf(num2);
		} else if (cd instanceof ColumnDescriptorDate) {
			ColumnDescriptorDate cdd = (ColumnDescriptorDate) cd;
			//...get the range of the dates
			stat.value = cdd.min;
			stat.maxValue = cdd.max;
		}
	}

	/**
	 * Displays a window where the user can see a summary information about table
	 * columns
	 */
	protected void showColumnInfo() {
		if (currTblD == null || currTblConnector == null)
			return;
		int tblIdx = currTblConnector.getNumOfTableDescriptors() - 1;
		TableDescriptor td = currTblConnector.getTableDescriptor(tblIdx);
		if (td == null || td.columns == null || td.columns.size() < 1)
			return;
		//allow the user to select table columns
		Vector colNames = new Vector(td.columns.size(), 1);
		for (int i = 0; i < td.columns.size(); i++) {
			ColumnDescriptor cd = (ColumnDescriptor) td.columns.elementAt(i);
			String meaning = currTblD.getColumnMeaning(cd.name);
			if (meaning == null) {
				colNames.addElement(cd.name);
			} else {
				colNames.addElement(cd.name + " (" + meaning + ")");
			}
		}
		MultiSelector ms = new MultiSelector(colNames, false);
		Panel p = new Panel(new BorderLayout());
		p.add(new Label("Select table columns to see value summaries:"), BorderLayout.NORTH);
		p.add(ms, BorderLayout.CENTER);
		OKDialog dia = new OKDialog(getFrame(), "Select columns to see value summaries", true);
		dia.addContent(p);
		dia.show();
		if (dia.wasCancelled())
			return;
		int sel[] = ms.getSelectedIndexes();
		if (sel == null || sel.length < 1)
			return;
		currTblConnector.reOpenConnection();
		TableStatisticsItem stat[] = new TableStatisticsItem[sel.length + 1];
		stat[0] = new TableStatisticsItem();
		stat[0].name = "Number of records";
		//...get the number of records...
		long num = currTblConnector.getNrows(tblIdx);
		Vector v = new Vector(10, 10);
		for (int element : sel) {
			ColumnDescriptor cd = td.getColumnDescriptor(element);
			if (cd instanceof ColumnDescriptorNum && Float.isNaN(((ColumnDescriptorNum) cd).min)) {
				v.addElement(cd);
			}
			if (cd instanceof ColumnDescriptorDate && ((ColumnDescriptorDate) cd).min == null) {
				v.addElement(cd);
			}
		}
		if (num <= 0 || v.size() > 0) {
			if (currTblConnector.getConnection() == null) {
				currTblConnector.reOpenConnection();
				if (currTblConnector.getConnection() == null) {
					showMessage("Failed to get a connection!", true);
					return;
				}
			}
			if (num <= 0) {
				currTblConnector.retrieveRowCount(tblIdx);
				num = currTblConnector.getNrows(tblIdx);
			}
			if (v.size() >= 0) {
				ColumnDescriptor cds[] = new ColumnDescriptor[v.size()];
				for (int i = 0; i < cds.length; i++) {
					cds[i] = (ColumnDescriptor) v.elementAt(i);
				}
				currTblConnector.getStatsForColumnDescriptors(currTblConnector.getTableDescriptor(0), cds);
			}
		}
		stat[0].value = String.valueOf(num);
		for (int i = 0; i < sel.length; i++) {
			ColumnDescriptor cd = (ColumnDescriptor) td.columns.elementAt(sel[i]);
			stat[i + 1] = new TableStatisticsItem();
			stat[i + 1].name = (String) colNames.elementAt(sel[i]);
			getColumnStat(currTblConnector, td, cd, stat[i + 1]);
		}
		showMessage("The value statistics successfully obtained!", false);
		//currTblConnector.closeConnection();
		TableStatisticsDisplay statD = new TableStatisticsDisplay(stat);
		core.getDisplayProducer().makeWindow(statD, currTblD.name + ": column overview");
	}

	/**
	 * Checks if the first n names of the relevant fields correspond to the
	 * specified canonic names
	 */
	protected boolean columnNamesAreCanonic(DBTableDescriptor tblDescr, String canonicNames[], int n) {
		if (tblDescr == null || canonicNames == null || tblDescr.relevantColumns == null || tblDescr.relevantColumns.length < n)
			return false;
		for (int i = 0; i < n; i++)
			if (tblDescr.relevantColumns[i] == null || !tblDescr.relevantColumns[i].equals(canonicNames[i]))
				return false;
		return true;
	}

	protected void analyse() {
		if (currTblD == null || currTblConnector == null)
			return;
		if (currTblD.dataMeaning == DataSemantics.MOVEMENTS && (currTblConnector instanceof OracleConnector) && columnNamesAreCanonic(currTblD, DataSemantics.canonicFieldNamesMovement, TrajectorySeparator.nMandMoveColumns)) {
			boolean hasTrIds = currTblD.relevantColumns[DataSemantics.idxOfTrajId] != null;
			int nOp = (hasTrIds) ? 7 : 2;
			Panel p = new Panel(new GridLayout(nOp + 1, 1, 0, 2));
			p.add(new Label("Select the operation to perform:"));
			CheckboxGroup cbg = new CheckboxGroup();
			Checkbox opCB[] = new Checkbox[nOp];
			if (hasTrIds) {
				opCB[0] = new Checkbox("further separate trajectories", true, cbg);
				p.add(opCB[0]);
				opCB[1] = new Checkbox("load trajectories", true, cbg);
				p.add(opCB[1]);
				opCB[2] = new Checkbox("get starts and ends", false, cbg);
				p.add(opCB[2]);
				opCB[3] = new Checkbox("get statistics for trajectories", false, cbg);
				p.add(opCB[3]);
				opCB[4] = new Checkbox("load and process trajectories in bunches", false, cbg);
				p.add(opCB[4]);
				opCB[5] = new Checkbox("load results of bunch clustering", false, cbg);
				p.add(opCB[5]);
			} else {
				opCB[0] = new Checkbox("separate trajectories", true, cbg);
				p.add(opCB[0]);
			}
			opCB[nOp - 1] = new Checkbox("aggregate data", false, cbg);
			p.add(opCB[nOp - 1]);
			OKDialog okd = new OKDialog(getFrame(), "Operation?", true);
			okd.addContent(p);
			okd.show();
			if (okd.wasCancelled())
				return;
			if (opCB[nOp - 1].getState()) {
				specifyAggregation();
			} else if (opCB[0].getState()) {
				separateTrajectories(currTblD.relevantColumns[DataSemantics.idxOfTrajId]);
			} else if (opCB[1].getState()) {
				loadTrajectories();
			} else if (opCB[2].getState()) {
				getStartsEnds();
			} else if (opCB[3].getState()) {
				getTrajectoryStatistics();
			} else if (opCB.length > 4 && opCB[4].getState()) {
				loadAndProcessTrajectoiresInBunches();
			} else {
				;
			}
			if (opCB.length > 5 && opCB[5].getState()) {
				loadResultsOfBunchClustering();
			} else {
				;
			}
		} else if (currTblD.dataMeaning == DataSemantics.EVENTS && (currTblConnector instanceof OracleConnector) && currTblD.xColIdx >= 0 && currTblD.yColIdx >= 0) {
			int nOp = 4;
			Panel p = new Panel(new GridLayout(nOp + 1, 1, 0, 2));
			p.add(new Label("Select the operation to perform:"));
			CheckboxGroup cbg = new CheckboxGroup();
			Checkbox opCB[] = new Checkbox[nOp];
			opCB[0] = new Checkbox("load events", true, cbg);
			p.add(opCB[0]);
			opCB[1] = new Checkbox("aggregate data in DB", false, cbg);
			p.add(opCB[1]);
			opCB[2] = new Checkbox("aggregate data in memory", false, cbg);
			p.add(opCB[2]);
			opCB[3] = new Checkbox("extract event information", false, cbg);
			p.add(opCB[3]);
			OKDialog okd = new OKDialog(getFrame(), "Operation?", true);
			okd.addContent(p);
			okd.show();
			if (okd.wasCancelled())
				return;
			if (opCB[0].getState()) {
				loadEvents();
			} else if (opCB[1].getState()) {
				specifyAggregation();
			} else if (opCB[2].getState()) {
				aggregateEventsByBunches();
			} else if (opCB[3].getState()) {
				extractEventInformation();
			}
		} else if (currTblD.dataMeaning == DataSemantics.TIME_RELATED && (currTblConnector instanceof OracleConnector)) {
			int nOp = 1;
			Panel p = new Panel(new GridLayout(nOp + 1, 1, 0, 2));
			p.add(new Label("Select the operation to perform:"));
			CheckboxGroup cbg = new CheckboxGroup();
			Checkbox opCB[] = new Checkbox[nOp];
			opCB[0] = new Checkbox("aggregate data in DB", false, cbg);
			p.add(opCB[0]);
			OKDialog okd = new OKDialog(getFrame(), "Operation?", true);
			okd.addContent(p);
			okd.show();
			if (okd.wasCancelled())
				return;
			if (opCB[0].getState()) {
				currTblD.dbProc = new DBProcedureSpec();
				// columns with IDs of grouping objects
				StringDivisionSpec sds = new StringDivisionSpec();
				sds.columnIdx = currTblD.idColIdx;
				sds.columnName = currTblD.idColName;
				currTblD.dbProc.addDivisionSpec(sds);
				// time
				specifyAggregation();
				// do aggregation and load results
				aggregateTimerelated();
			}
		} else {
			showMessage("Sorry... No methods to deal with this kind of data!", true);
		}
	}

	/**
	 * Aggregate time-related data
	 */
	protected void aggregateTimerelated() {
		TableDescriptor td = currTblConnector.getTableDescriptor(0);
		Panel p = new Panel(new ColumnLayout());
		List list = new List(10, true);
		for (int i = 0; i < td.getNColumns(); i++) {
			list.add(td.getColumnDescriptor(i).name);
		}
		p.add(new Label("Select extra parameter(s) for grouping:"));
		p.add(list);
		p.add(new Line(false));
		p.add(new Label("SQL query:"));
		TextField tf = new TextField("");
		p.add(tf);
		OKDialog ok = new OKDialog(getFrame(), "Grouping and Query", true);
		ok.addContent(p);
		ok.show();
		if (ok.wasCancelled())
			return;
		// set extra groupin parameters - only UI is needed + set list of values
		for (int i = 0; i < list.getItemCount(); i++)
			if (list.isIndexSelected(i)) {
				StringDivisionSpec sds = new StringDivisionSpec();
				sds.columnIdx = i;
				sds.columnName = list.getItem(i);
				currTblD.dbProc.addDivisionSpec(sds);
				try {
					String sql = "select distinct " + sds.columnName + " from " + td.tableName + "\norder by " + sds.columnName;
					Statement statement = currTblConnector.getConnection().createStatement();
					ResultSet result = statement.executeQuery(sql);
					while (result.next()) {
						String str = result.getString(1);
						if (sds.labels == null) {
							sds.labels = new Vector<String>(10, 10);
						}
						if (str == null || str.trim().length() == 0) {
							str = "null";
						}
						sds.labels.addElement(str);
					}
					statement.close();
				} catch (SQLException e) {
				}
			}
		// set query restrictions
		String whereStr = tf.getText().trim(); //for example, "diff<>8";
		// select aggregation attributes in addition to counts
		defineAggregationOperations();
		// process time
		Parameter pars[] = new Parameter[currTblD.dbProc.getDivisionSpecCount() - 1], pars_dummy[] = new Parameter[currTblD.dbProc.getDivisionSpecCount()];
		for (int pidx = 0; pidx < currTblD.dbProc.getDivisionSpecCount() - 1; pidx++) {
			DivisionSpec div = currTblD.dbProc.getDivisionSpec(pidx + 1);
			pars[pidx] = pars_dummy[1 + pidx] = new Parameter();
			String str = "parameter " + pidx;
			if (div instanceof StringDivisionSpec) {
				str = ((StringDivisionSpec) div).columnName;
			}
			if (div instanceof TimeCycleDivisionSpec) {
				TimeCycleDivisionSpec tcDiv = (TimeCycleDivisionSpec) div;
				if (tcDiv.cycleCode.equals("MM")) {
					str = "Month";
				} else if (tcDiv.cycleCode.equals("D")) {
					str = "Day of week";
				} else if (tcDiv.cycleCode.equals("HH24")) {
					str = "Hour";
				} else if (tcDiv.cycleCode.equals("MI")) {
					str = "Minute";
				} else if (tcDiv.cycleCode.equals("SS")) {
					str = "Second";
				}
				pars[pidx].setTemporal(true);
			}
			TimeLineDivisionSpec tlDiv = null;
			if (div instanceof TimeLineDivisionSpec) {
				str = "Time interval (start)";
				pars[pidx].setTemporal(true);
				tlDiv = (TimeLineDivisionSpec) div;
			}
			pars[pidx].setName(str);
			for (int vidx = 0; vidx < div.getPartitionCount(); vidx++)
				if (!pars[pidx].isTemporal()) {
					pars[pidx].addValue(div.getPartitionLabel(vidx));
				} else {
					if (tlDiv != null) {
						pars[pidx].addValue(tlDiv.getBreak(vidx));
					} else {
						TimeCount tc = new TimeCount((long) (vidx + 1));
						tc.setShownValue(div.getPartitionLabel(vidx));
						pars[pidx].addValue(tc);
					}
				}
			if (tlDiv != null && (pars[pidx].getFirstValue() instanceof Date)) {
				String expl[] = { "Time interval: from " + pars[pidx].getFirstValue() + " to " + pars[pidx].getLastValue() };
				char datePrecision = TimeDialogs.askDesiredDatePrecision(expl, (Date) pars[pidx].getFirstValue());
				String dateScheme = ((Date) pars[pidx].getFirstValue()).scheme;
				if (datePrecision != ((Date) pars[pidx].getFirstValue()).getPrecision()) {
					dateScheme = Date.removeExtraElementsFromScheme(dateScheme, datePrecision);
				}
				EnterDateSchemeUI enterSch = new EnterDateSchemeUI("Edit, if desired, the date/time scheme (template) for the parameter values. " + " The chosen precision is " + Date.getTextForTimeSymbol(datePrecision) + ". " + expl[0],
						"Date scheme:", dateScheme);
				OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Date/time scheme", false);
				dia.addContent(enterSch);
				dia.show();
				dateScheme = enterSch.getScheme();
				for (int i = 0; i < pars[pidx].getValueCount(); i++) {
					((Date) pars[pidx].getValue(i)).setPrecision(datePrecision);
					((Date) pars[pidx].getValue(i)).setDateScheme(dateScheme);
				}
			}
		}
		// generate and run SQL code
		DataTable dt = new DataTable();
		ResultSet result = doAggregationQuery(dt, whereStr);
		/* fast query for distinct dates (produces the same results):

		select a.country,a.date_of_death-b.mind+1,a.count from
		(select country,date_of_death,count(*) as count
		from cxydsm_epi
		group by country,date_of_death
		order by country,date_of_death) a,
		(select min(date_of_death) as mind from cxydsm_epi) b

		*/
		// extract the results and construct data table
		if (result == null) {
			currTblConnector.closeConnection();
			return; // add some error message!
		}
		// create resulting table
		for (int pidx = 1; pidx < currTblD.dbProc.getDivisionSpecCount(); pidx++) {
			dt.addParameter(pars[pidx - 1]);
		}
		for (int i = 0; i < currTblD.dbProc.getAttrStatSpecCount(); i++) {
			AttrStatSpec ass = currTblD.dbProc.getAttrStatSpec(i);
			String tblColumnName = (String) ass.resultColNames.elementAt(0);
			Attribute attrRoot = new Attribute(tblColumnName, (tblColumnName.startsWith("Count_")) ? AttributeTypes.integer : AttributeTypes.real);
			attrRoot.setName(tblColumnName);
			prepareNDimensionalAttributes(dt, attrRoot, attrRoot, pars_dummy, 1, 0);
		}
		// load results into table
		try {
			long nrec = 0;
			String lastId = "";
			DataRecord dr = null;
			while (result.next()) {
				String recID = result.getString(1);
				int idx[] = new int[pars.length];
				for (int i = 0; i < pars.length; i++) {
					String str = result.getString(2 + i);
					try {
						idx[i] = Integer.valueOf(str).intValue();
					} catch (NumberFormatException nfe) {
						if (str == null || str.trim().length() == 0) {
							str = "null";
						}
						idx[i] = pars[i].getValueIndex(str);
					}
					if (idx[i] == 0) {
						idx[i] = pars[i].getValueCount();
					}
				}
				int n = idx[idx.length - 1] - 1, mult = 1;
				for (int level = idx.length - 2; level >= 0; level--) {
					mult *= pars[level + 1].getValueCount();
					n += (idx[level] - 1) * mult;
				}
				mult *= pars[0].getValueCount();
				if (lastId.equals(recID)) { // same record, other parameters
					for (int i = 0; i < currTblD.dbProc.getAttrStatSpecCount(); i++) {
						AttrStatSpec ass = currTblD.dbProc.getAttrStatSpec(i);
						if (ass.columnName.startsWith("Count_")) {
							long l = result.getLong(2 + pars.length + i);
							dr.setNumericAttrValue(l, n + i * mult);
							//System.out.println("* recID="+recID+", idx="+idx[0]+", value="+l);
						} else {
							float f = result.getFloat(2 + pars.length + i);
							dr.setNumericAttrValue(f, n + i * mult);
							//System.out.println("* recID="+recID+", idx="+idx[0]+", value="+f);
						}
					}
				} else { // new record
					if (dr != null) {
						dt.addDataRecord(dr);
					}
					dr = new DataRecord(recID);
					for (int i = 0; i < currTblD.dbProc.getAttrStatSpecCount(); i++) {
						AttrStatSpec ass = currTblD.dbProc.getAttrStatSpec(i);
						if (ass.columnName.startsWith("Count_")) {
							long l = result.getLong(2 + pars.length + i);
							dr.setNumericAttrValue(l, n + i * mult);
						} else {
							float f = result.getFloat(2 + pars.length + i);
							dr.setNumericAttrValue(f, n + i * mult);
						}
					}
					lastId = recID;
				}
				nrec++;
				//if (nrec%1000==0)
				//System.out.println("* nrec="+nrec+"...");
			}
			if (dr != null) {
				dt.addDataRecord(dr);
			}
			System.out.println("* nrec=" + nrec);
		} catch (SQLException se) {
			System.out.println("SQL error:" + se.toString());
		}
		currTblConnector.closeConnection();
		// tuning attribute names in the resulting table
		String tNames[] = new String[1];
		tNames[0] = (dt.getName() == null) ? "" : dt.getName();
		tNames = Dialogs.editStringValues(core.getUI().getMainFrame(), null, tNames, "Edit the table name", "Table name", true);
		if (tNames != null) {
			dt.setName(tNames[0]);
		}
		Vector attr = dt.getTopLevelAttributes();
		String aNames[] = new String[attr.size()];
		for (int i = 0; i < aNames.length; i++) {
			aNames[i] = ((Attribute) attr.elementAt(i)).getName();
		}
		aNames = Dialogs.editStringValues(core.getUI().getMainFrame(), null, aNames, "Edit the names of the table attributes if needed", "Attribute names", true);
		if (aNames != null) {
			for (int i = 0; i < aNames.length; i++)
				if (aNames[i] != null) {
					((Attribute) attr.elementAt(i)).setName(aNames[i]);
				}
		}
		//put 0 instead of missing values in time series or other parameter-dependent attributes
		int nNewCols = dt.getAttrCount();
		int c0 = dt.getAttrCount() - nNewCols;
		while (c0 < dt.getAttrCount() - 1) {
			Attribute at = dt.getAttribute(c0);
			if (!at.isNumeric()) {
				++c0;
				continue;
			}
			Attribute parent = at.getParent();
			if (parent == null) {
				++c0;
				continue;
			}
			int cNext = c0 + 1;
			while (cNext < dt.getAttrCount() && dt.getAttribute(cNext).getParent().equals(parent)) {
				++cNext;
			}
			for (int nr = 0; nr < dt.getDataItemCount(); nr++) {
				boolean hasSome = false;
				DataRecord rec = dt.getDataRecord(nr);
				for (int nc = c0; nc < cNext && !hasSome; nc++) {
					hasSome = !Double.isNaN(rec.getNumericAttrValue(nc));
				}
				if (!hasSome) {
					continue;
				}
				for (int nc = c0; nc < cNext; nc++)
					if (Double.isNaN(rec.getNumericAttrValue(nc))) {
						if (at.getIdentifier().startsWith("Count_")) {
							rec.setNumericAttrValue(0, nc);
						} else { // interpolate
							int idxPrev = nc - 1, idxNext = nc + 1;
							for (idxPrev = nc - 1; idxPrev > c0 && Double.isNaN(rec.getNumericAttrValue(idxPrev)); idxPrev--) {
								;
							}
							for (idxNext = nc + 1; idxNext < cNext && Double.isNaN(rec.getNumericAttrValue(idxNext)); idxNext++) {
								;
							}
							if (idxPrev >= c0 && idxNext < cNext && !Double.isNaN(rec.getNumericAttrValue(idxPrev)) && !Double.isNaN(rec.getNumericAttrValue(idxNext))) {
								double val = rec.getNumericAttrValue(idxPrev) + (rec.getNumericAttrValue(idxNext) - rec.getNumericAttrValue(idxPrev)) * (nc - idxPrev) / (idxNext - idxPrev);
								rec.setNumericAttrValue(val, nc);
							}
						}
					}
			}
			c0 = cNext;
		}
		core.getDataLoader().addTable(dt);
		core.getUI().showMessage("Aggregation finished!", false);
	}

	/**
	 * For movement data, performs separation of trajectories
	 */
	protected void separateTrajectories(String trIdColumn) {
		if (currTblD == null || currTblConnector == null)
			return;
		if (!(currTblConnector instanceof OracleConnector))
			return;
		if (currTblConnector.getConnection() == null) {
			currTblConnector.reOpenConnection();
			if (currTblConnector.getConnection() == null) {
				showMessage("Failed to get a connection!", true);
				return;
			}
		}
		TrajectorySeparator sep = new TrajectorySeparator((OracleConnector) currTblConnector, currTblD, trIdColumn);
		if (core.getUI() != null) {
			sep.setMainFrame(core.getUI().getMainFrame());
			sep.setNotificationLine(core.getUI().getStatusLine());
		}
		DBTableDescriptor resTblD = sep.doSeparation();
		if (resTblD != null) {
			//add the new table to the list of tables
			tdescr.addElement(resTblD);
			connectors.addElement(null);
			tblList.add(resTblD.name + "@" + resTblD.computerName);
		}
		currTblConnector.closeConnection();
	}

	/**
	 * Load events from the database
	 */
	protected DataTable loadEvents() {
		if (currTblD == null || currTblConnector == null)
			return null;
		if (!(currTblConnector instanceof OracleConnector))
			return null;
		Connection connection = currTblConnector.getConnection();
		if (connection == null) {
			currTblConnector.reOpenConnection();
			connection = currTblConnector.getConnection();
			if (connection == null) {
				showMessage("Failed to get a connection!", true);
				return null;
			}
		}
		RealRectangle brr = null;
		if (core.getDataLoader() != null && core.getDataLoader().getMap(0) != null) {
			brr = ((DLayerManager) core.getDataLoader().getMap(0)).getSpatialWindowExtent();
		}
		// selecting additional attributes and sampling
		TableDescriptor tblDescr = currTblConnector.getTableDescriptor(0);
		String tableName = tblDescr.tableName;
		ResultSet result = null;
		boolean sqlError = false;
		String sqlString = "select " + currTblD.xColName + "," + currTblD.yColName + "," + currTblD.timeColName;
		int extraAttrStartIdx = 4;
		if (currTblD.idColIdx >= 0) {
			sqlString += "," + currTblD.idColName;
			extraAttrStartIdx++;
		}
		//
		List lst = new List(10, true);
		for (int i = 0; i < tblDescr.getNColumns(); i++) {
			String cname = tblDescr.getColumnDescriptor(i).name;
			boolean found = false;
			for (int j = 0; j < currTblD.relevantColumns.length && !found; j++) {
				found = cname.equalsIgnoreCase(currTblD.relevantColumns[j]);
			}
			if (!found) {
				lst.add(cname);
			}
		}
		spade.analysis.tools.db_tools.events.SamplingPanel samplingPanel = new spade.analysis.tools.db_tools.events.SamplingPanel((OracleConnector) currTblConnector, currTblD, getFrame(), brr, lst);
		Panel p = new Panel(new BorderLayout()), pl = new Panel(new BorderLayout());
		p.add(pl, BorderLayout.EAST);
		p.add(samplingPanel, BorderLayout.WEST);
		p.add(new Line(true), BorderLayout.CENTER);
		TextCanvas tCan = new TextCanvas();
		tCan.addTextLine("Which additional data about the events would you like to load?");
		pl.add(tCan, BorderLayout.NORTH);
		pl.add(lst, BorderLayout.CENTER);
		OKDialog dia = new OKDialog(getFrame(), "Sampling and Additional Data", true);
		dia.addContent(p);
		dia.show();
		if (dia.wasCancelled())
			return null;

		// loading!
		ActionDescr actionDescr = new ActionDescr();
		actionDescr.aName = "Load events from " + currTblD.dbTableName;
		actionDescr.startTime = System.currentTimeMillis();
		actionDescr.addParamValue("Database URL", currTblConnector.makeDatabaseURL());
		actionDescr.addParamValue("Database name", currTblConnector.getDatabaseName());
		actionDescr.addParamValue("User", currTblConnector.getUserName());
		actionDescr.addParamValue("Table", tblDescr.tableName);
		String attrIds[] = null;
		attrIds = lst.getSelectedItems();
		if (attrIds != null && attrIds.length < 1) {
			attrIds = null;
		}
		if (attrIds != null) {
			// Attach the names of the additional attributes to the SQL string
			for (String attrId : attrIds) {
				sqlString += "," + attrId;
			}
			actionDescr.addParamValue("Additional attributes", attrIds);
		}
		//end attach additional attributes
		if (samplingPanel.getUseSampling() || (samplingPanel.getUseQuery() && samplingPanel.getQueryString().length() > 0)) {
			float fSample = samplingPanel.getSampleSize(), fSeed = samplingPanel.getSeed();
			if (samplingPanel.getUseQuery() && samplingPanel.getQueryString().length() > 0) {
				actionDescr.addParamValue("Selection", samplingPanel.getQueryString());
			}
			if (samplingPanel.getUseSampling()) {
				actionDescr.addParamValue("Sample size", new Float(fSample));
				if (!Float.isNaN(fSeed)) {
					actionDescr.addParamValue("Sample seed", new Float(fSeed));
				}
			}
			sqlString += " FROM " + tableName + "\n" + ((samplingPanel.getUseSampling()) ? "SAMPLE (" + fSample + ")" + ((Float.isNaN(fSeed) ? "" : " SEED (" + fSeed + ")")) + "\n" : "")
					+ ((samplingPanel.getUseQuery() && samplingPanel.getQueryString().length() > 0) ? "WHERE " + samplingPanel.getQueryString() : "");
		} else {
			sqlString += " FROM " + tableName + "";
		}
		long tStart = System.currentTimeMillis();
		Vector<Attribute> attrList = null;
		ResultSetMetaData md = null;
		try {
			Statement statement = connection.createStatement();
			System.out.println("* <" + sqlString + ">");
			result = statement.executeQuery(sqlString);
			md = result.getMetaData();
			attrList = new Vector(3 + ((attrIds == null) ? 0 : attrIds.length), 1);
			Attribute attr = new Attribute(currTblD.xColName, AttributeTypes.real);
			attrList.addElement(attr);
			attr = new Attribute(currTblD.yColName, AttributeTypes.real);
			attrList.addElement(attr);
			attr = new Attribute(currTblD.timeColName, AttributeTypes.time);
			attrList.addElement(attr);
			if (attrIds != null) {
				for (int i = 0; i < attrIds.length; i++) {
					int colType = md.getColumnType(i + extraAttrStartIdx);
					char aType = AttributeTypes.character;
					switch (colType) {
					case java.sql.Types.NUMERIC:
					case java.sql.Types.DECIMAL:
					case java.sql.Types.DOUBLE:
					case java.sql.Types.FLOAT:
						aType = AttributeTypes.real;
						break;
					case java.sql.Types.BIGINT:
					case java.sql.Types.INTEGER:
						aType = AttributeTypes.integer;
						break;
					case java.sql.Types.DATE:
					case java.sql.Types.TIME:
					case java.sql.Types.TIMESTAMP:
						aType = AttributeTypes.time;
						break;
					case java.sql.Types.BOOLEAN:
						aType = AttributeTypes.logical;
						break;
					}
					attr = new Attribute(attrIds[i], aType);
					attrList.addElement(attr);
				}
			}
		} catch (SQLException se) {
			showMessage("SQL error:" + se.toString(), true);
			sqlError = true;
		}
		// create DataTable
		DataTable tbl = new DataTable();
		tbl.setName(tableName);
		tbl.setMadeByAction(actionDescr);
		ResultDescr aRes = new ResultDescr();
		aRes.product = tbl;
		actionDescr.addResultDescr(aRes);
		if (attrList != null) {
			for (int i = 0; i < attrList.size(); i++) {
				tbl.addAttribute(attrList.elementAt(i));
			}
		}
		// fill the table
		int np = 0; // N events
		Vector<DGeoObject> vdgo = new Vector(1000, 1000);
		try {
			while (result.next()) {
				np++;
				String id = (currTblD.idColIdx >= 0) ? result.getString(4) : "" + np;
				RealPoint pos = new RealPoint(result.getFloat(1), result.getFloat(2));
				TimeMoment tm = null;
				if (samplingPanel.physicalTime) {
					spade.time.Date dt = new spade.time.Date();
					tm = dt;
					java.util.Date date = result.getDate(3);
					java.sql.Time time = result.getTime(3);
					dt.setPrecision(samplingPanel.datePrecision);
					dt.setDateScheme(samplingPanel.dateScheme);
					dt.setDate(date, time);
				} else {
					long t = result.getLong(3);
					TimeCount tc = new TimeCount(t);
					tm = tc;
				}

				DataRecord pRec = null;
				if (attrList != null) {
					pRec = new DataRecord(id);
					tbl.addDataRecord(pRec);
					pRec.setNumericAttrValue(result.getFloat(1), 0);
					pRec.setNumericAttrValue(result.getFloat(2), 1);
					pRec.addAttrValue(tm);
					if (attrIds != null) {
						for (int i = 0; i < attrIds.length; i++) {
							int colType = md.getColumnType(i + extraAttrStartIdx);
							switch (colType) {
							case java.sql.Types.NUMERIC:
							case java.sql.Types.DECIMAL:
							case java.sql.Types.DOUBLE:
							case java.sql.Types.FLOAT:
								double dval = result.getDouble(i + extraAttrStartIdx);
								if (result.wasNull()) {
									dval = Double.NaN;
								}
								pRec.setNumericAttrValue(dval, (Double.isNaN(dval)) ? null : result.getString(i + extraAttrStartIdx), i + 3);
								break;
							case java.sql.Types.BIGINT:
							case java.sql.Types.INTEGER:
								long ival = result.getInt(i + extraAttrStartIdx);
								pRec.setNumericAttrValue((double) ival, String.valueOf(ival), i + 3);
								break;
							case java.sql.Types.BOOLEAN:
								boolean bval = result.getBoolean(i + extraAttrStartIdx);
								pRec.addAttrValue((bval) ? "T" : "F");
								break;
							case java.sql.Types.DATE:
							case java.sql.Types.TIME:
							case java.sql.Types.TIMESTAMP:
								spade.time.Date dt = new spade.time.Date();
								dt.setPrecision(samplingPanel.datePrecision);
								dt.setDateScheme(samplingPanel.dateScheme);
								tm = dt;
								try {
									java.util.Date date = result.getDate(i + extraAttrStartIdx);
									java.sql.Time time = result.getTime(i + extraAttrStartIdx);
									dt.setDate(date, time);
									pRec.addAttrValue(dt);
								} catch (Exception e) {
									String strVal = result.getString(i + extraAttrStartIdx);
									System.out.println(">>> Could not get date from \"" + strVal + "\"");
									pRec.addAttrValue(null);
								}
								break;
							default:
								pRec.addAttrValue(result.getString(i + extraAttrStartIdx));
							}
						}
					}
				}
				if (np % 100 == 0) {
					showMessage(np + " events loaded", false);
				}
				SpatialEntity se = new SpatialEntity(id);
				se.setGeometry(pos);
				TimeReference tr = new TimeReference();
				tr.setValidFrom(tm);
				tr.setValidUntil(tm);
				se.setTimeReference(tr);
				DGeoObject dgo = new DGeoObject();
				dgo.setup(se);
				dgo.setThematicData(pRec);
				vdgo.addElement(dgo);
			}
		} catch (SQLException se) {
			showMessage("SQL error:" + se.toString(), true);
			sqlError = true;
		}
		if (vdgo.size() < 1) {
			showMessage("No result set obtained!", true);
			return null;
		}
		long tEnd = System.currentTimeMillis() - tStart;
		System.out.println("* Ready. Elapsed time " + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s)");
		showMessage("Successfully got " + vdgo.size() + " events!", false);

		String layerName = "Events from " + tableName + " (" + currTblConnector.getComputerName() + ")";
		if (samplingPanel != null && samplingPanel.getUseSampling()) {
			layerName += "Sample size=" + samplingPanel.getSampleSize() + ((Float.isNaN(samplingPanel.getSeed())) ? "" : ", seed=" + samplingPanel.getSeed());
		}

		//make a layer with the trajectories
		DGeoLayer tLayer = new DGeoLayer();
		tLayer.setType(Geometry.point);
		tLayer.setName(layerName);
		tLayer.setGeoObjects(vdgo, true);
		tLayer.setHasMovingObjects(true);
		DrawingParameters dp = tLayer.getDrawingParameters();
		if (dp == null) {
			dp = new DrawingParameters();
			tLayer.setDrawingParameters(dp);
		}
		dp.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
		dp.lineWidth = 1;
		dp.transparency = 0;
		aRes = new ResultDescr();
		aRes.product = tLayer;
		actionDescr.addResultDescr(aRes);

		int coordsAreGeographic = -1;
		if (core.getDataLoader() != null && core.getDataLoader().getMap(0) != null) {
			coordsAreGeographic = (core.getDataLoader().getMap(0).isGeographic()) ? 1 : 0;
		}

		DataLoader dLoader = core.getDataLoader();
		int trTblN = dLoader.addTable(tbl);
		tLayer.setDataTable(tbl);
		dLoader.addMapLayer(tLayer, -1, coordsAreGeographic);
		dLoader.setLink(tLayer, trTblN);
		tLayer.setLinkedToTable(true);
		tLayer.setThematicFilter(tbl.getObjectFilter());
		tLayer.setLinkedToTable(true);
		dLoader.processTimeReferencedObjectSet(tLayer);
		dLoader.processTimeReferencedObjectSet(tbl);

		actionDescr.endTime = System.currentTimeMillis();
		core.logAction(actionDescr);

		return null;
	}

	/**
	 * Load non-aggregated trajectories from the database
	 */
	protected void loadTrajectories() {
		loadTrajectories("", null);
	}

	protected DataTable loadTrajectories(String tableNameInUI, String sqlCode) {
		// if table is the result of SQL code, there are no further dialogs for
		if (currTblD == null || currTblConnector == null)
			return null;
		if (!(currTblConnector instanceof OracleConnector))
			return null;
		Connection connection = currTblConnector.getConnection();
		if (connection == null) {
			currTblConnector.reOpenConnection();
			connection = currTblConnector.getConnection();
			if (connection == null) {
				showMessage("Failed to get a connection!", true);
				return null;
			}
		}
		// selecting additional attributes and sampling
		TableDescriptor tblDescr = currTblConnector.getTableDescriptor(0);
		String tableName = (sqlCode == null) ? tblDescr.tableName : sqlCode;
		if (sqlCode == null) {
			tableNameInUI = tableName;
		}
		ResultSet result = null;
		boolean sqlError = false;
		String sqlString = "select a.tid_,a.x_,a.y_,a.dt_,a.distance_";
		int extraAttrStartIdx = 6;
		int idxEId = -1;
		for (int i = 0; i < DataSemantics.movementSemantics.length && idxEId < 0; i++)
			if (DataSemantics.movementSemantics[i].equalsIgnoreCase("entity identifier")) {
				idxEId = i;
			}
		boolean hasEId = idxEId >= 0 && currTblD.relevantColumns[idxEId] != null;
		if (hasEId) {
			sqlString += ",a." + currTblD.relevantColumns[idxEId];
			++extraAttrStartIdx;
		}
		//attach additional attributes AND set sampling parameters
		//1. Ask the user which additional attributes of the positions to load
		List lst = new List(10, true);
		int coordsAreGeographic = -1;
		if (core.getDataLoader() != null && core.getDataLoader().getMap(0) != null) {
			coordsAreGeographic = (core.getDataLoader().getMap(0).isGeographic()) ? 1 : 0;
		}
		RealRectangle brr = null;
		if (core.getDataLoader() != null && core.getDataLoader().getMap(0) != null) {
			brr = ((DLayerManager) core.getDataLoader().getMap(0)).getSpatialWindowExtent();
		}
		SamplingPanel samplingPanel = new SamplingPanel((OracleConnector) currTblConnector, getFrame(), coordsAreGeographic, brr);
		if (sqlCode == null) {
			for (int i = 0; i < currTblD.relevantColumns.length; i++)
				if (i != idxEId && currTblD.relevantColumns[i] != null && !currTblD.relevantColumns[i].equalsIgnoreCase("tid_") && !currTblD.relevantColumns[i].equalsIgnoreCase("x_") && !currTblD.relevantColumns[i].equalsIgnoreCase("y_")
						&& !currTblD.relevantColumns[i].equalsIgnoreCase("dt_")) {
					lst.add(currTblD.relevantColumns[i]);
				}
			for (int i = 0; i < tblDescr.getNColumns(); i++) {
				String cname = tblDescr.getColumnDescriptor(i).name;
				boolean found = false;
				for (int j = 0; j < currTblD.relevantColumns.length && !found; j++) {
					found = cname.equalsIgnoreCase(currTblD.relevantColumns[j]);
				}
				if (!found) {
					lst.add(cname);
				}
			}
			Panel p = new Panel(new BorderLayout()), pl = new Panel(new BorderLayout());
			p.add(pl, BorderLayout.EAST);
			p.add(samplingPanel, BorderLayout.WEST);
			p.add(new Line(true), BorderLayout.CENTER);
			TextCanvas tCan = new TextCanvas();
			tCan.addTextLine("Which additional data about the positions would you like to load?");
			pl.add(tCan, BorderLayout.NORTH);
			pl.add(lst, BorderLayout.CENTER);
			OKDialog dia = new OKDialog(getFrame(), "Sampling and Additional Data", true);
			dia.addContent(p);
			dia.show();
			if (dia.wasCancelled())
				return null;
		}
		ActionDescr actionDescr = new ActionDescr();
		actionDescr.aName = "Load trajectories from " + tableNameInUI;
		actionDescr.startTime = System.currentTimeMillis();
		actionDescr.addParamValue("Database URL", currTblConnector.makeDatabaseURL());
		actionDescr.addParamValue("Database name", currTblConnector.getDatabaseName());
		actionDescr.addParamValue("User", currTblConnector.getUserName());
		actionDescr.addParamValue("Table", tblDescr.tableName);
		String attrIds[] = null;
		attrIds = lst.getSelectedItems();
		if (attrIds != null && attrIds.length < 1) {
			attrIds = null;
		}
		if (attrIds != null) {
			//2. Attach the names of the additional attributes to the SQL string
			for (String attrId : attrIds) {
				sqlString += ",a." + attrId;
			}
			actionDescr.addParamValue("Additional attributes", attrIds);
		}
		//end attach additional attributes

		if (samplingPanel.getUseSampling() || (samplingPanel.getUseQuery() && samplingPanel.getQueryString().length() > 0)) {
			float fSample = samplingPanel.getSampleSize(), fSeed = samplingPanel.getSeed();
			actionDescr.addParamValue("Sampling/selection by", (samplingPanel.isSamplingByObjects()) ? "objects" : "trajectories");
			if (samplingPanel.getUseQuery() && samplingPanel.getQueryString().length() > 0) {
				actionDescr.addParamValue("Selection", samplingPanel.getQueryString());
			}
			if (samplingPanel.getUseSampling()) {
				actionDescr.addParamValue("Sample size", new Float(fSample));
				if (!Float.isNaN(fSeed)) {
					actionDescr.addParamValue("Sample seed", new Float(fSeed));
				}
			}
			sqlString += " FROM " + tableName + " a,\n" + ((samplingPanel.isSamplingByObjects()) ? "     (SELECT DISTINCT ID_" : "     (SELECT DISTINCT TID_\n") + "      FROM " + tableName + "_se\n"
					+ ((samplingPanel.getUseSampling()) ? "      SAMPLE (" + fSample + ")" + ((Float.isNaN(fSeed) ? "" : " SEED (" + fSeed + ")")) + "\n" : "")
					+ ((samplingPanel.getUseQuery() && samplingPanel.getQueryString().length() > 0) ? "      WHERE " + samplingPanel.getQueryString() + "\n" : "") + ") b\n"
					+ ((samplingPanel.isSamplingByObjects()) ? "WHERE a.ID_ = b.ID_" : "WHERE a.TID_ = b.TID_");
		} else {
			sqlString += " from (" + tableName + ") a";
		}
		sqlString += "\norder by a.TID_, a.TNUM_";
		long tStart = System.currentTimeMillis();
		Vector attrList = null;
		try {
			Statement statement = connection.createStatement();
			System.out.println("* <" + sqlString + ">");
			result = statement.executeQuery(sqlString);
			ResultSetMetaData md = result.getMetaData();
			if (attrIds != null) {
				attrList = new Vector(attrIds.length, 1);
				for (int i = 0; i < attrIds.length; i++) {
					int colType = md.getColumnType(i + extraAttrStartIdx);
					char aType = AttributeTypes.character;
					switch (colType) {
					case java.sql.Types.NUMERIC:
					case java.sql.Types.DECIMAL:
					case java.sql.Types.DOUBLE:
					case java.sql.Types.FLOAT:
						aType = AttributeTypes.real;
						break;
					case java.sql.Types.BIGINT:
					case java.sql.Types.INTEGER:
						aType = AttributeTypes.integer;
						break;
					case java.sql.Types.DATE:
					case java.sql.Types.TIME:
					case java.sql.Types.TIMESTAMP:
						aType = AttributeTypes.time;
						break;
					case java.sql.Types.BOOLEAN:
						aType = AttributeTypes.logical;
						break;
					}
					Attribute attr = new Attribute(attrIds[i], aType);
					attrList.addElement(attr);
				}
			}
		} catch (SQLException se) {
			showMessage("SQL error:" + se.toString(), true);
			sqlError = true;
		}
		if (result == null) {
			if (!sqlError) {
				showMessage("No result set obtained!", true);
			}
			currTblConnector.closeConnection();
			return null;
		}
		long tEnd = System.currentTimeMillis() - tStart;
		System.out.println("* Ready. Elapsed time " + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s)");
		showMessage("Creating trajectory data structures", false);
		System.out.println("* Creating trajectory data structures...");
		tStart = System.currentTimeMillis();
		Vector mObjects = new Vector(200, 100); //trajectories
		DMovingObject mObj = null;
		int np = 0;
		double segmentLength = 0;
		actionDescr.addParamValue("Physical time", new Boolean(samplingPanel.physicalTime));
		if (samplingPanel.physicalTime) {
			actionDescr.addParamValue("Date precision", new Character(samplingPanel.datePrecision));
			actionDescr.addParamValue("Date scheme", samplingPanel.dateScheme);
		}
		try {
			while (result.next()) {
				++np;
				String id = result.getString(1);
				//System.out.println("* id="+id);
				RealPoint pos = new RealPoint(result.getFloat(2), result.getFloat(3));
				segmentLength = result.getDouble(5); // distance
				String eId = null;
				if (hasEId) {
					eId = result.getString(6);
				}
				TimeMoment tm = null;
				if (samplingPanel.physicalTime) {
					spade.time.Date dt = new spade.time.Date();
					tm = dt;
					java.util.Date date = result.getDate(4);
					java.sql.Time time = result.getTime(4);
					dt.setPrecision(samplingPanel.datePrecision);
					dt.setDateScheme(samplingPanel.dateScheme);
					dt.setDate(date, time);
				} else {
					long t = result.getLong(4);
					TimeCount tc = new TimeCount(t);
					tm = tc;
				}

				DataRecord pRec = null;
				if (attrList != null) {
					pRec = new DataRecord(id + "_" + np);
					pRec.setAttrList(attrList);
					for (int i = 0; i < attrList.size(); i++) {
						switch (((Attribute) attrList.elementAt(i)).getType()) {
						case 'C':
							pRec.addAttrValue(result.getString(i + extraAttrStartIdx));
							break;
						case 'I':
							long ival = result.getInt(i + extraAttrStartIdx);
							pRec.setNumericAttrValue((double) ival, String.valueOf(ival), i);
							break;
						case 'R':
							double dval = result.getDouble(i + extraAttrStartIdx);
							if (!result.wasNull()) {
								pRec.setNumericAttrValue(dval, String.valueOf(dval), i);
							} else {
								pRec.setAttrValue(null, i);
							}
							break;
						case 'L':
							boolean bval = result.getBoolean(i + extraAttrStartIdx);
							pRec.addAttrValue((bval) ? "T" : "F");
							break;
						case 'T': {
							spade.time.Date dt = new spade.time.Date();
							dt.setPrecision(samplingPanel.datePrecision);
							dt.setDateScheme(samplingPanel.dateScheme);
							tm = dt;
							java.util.Date date = result.getDate(4);
							java.sql.Time time = result.getTime(4);
							dt.setDate(date, time);
							pRec.addAttrValue(dt);
							break;
						}
						}
					}
				}

				if (mObj == null || !id.equalsIgnoreCase(mObj.getIdentifier())) {
					np = 1;
					mObj = new DMovingObject();
					mObj.setIdentifier(id);
					if (hasEId) {
						mObj.setEntityId(eId);
					}
					mObj.setTrackLength(0);
					mObjects.addElement(mObj);
					if (mObjects.size() % 100 == 0) {
						showMessage(mObjects.size() + " trajectories loaded", false);
					}
				}
				mObj.addPosition(pos, tm, tm, pRec);
				mObj.incrementTrackLength(segmentLength);
			}
		} catch (SQLException se) {
			showMessage("SQL error:" + se.toString(), true);
			sqlError = true;
		}
		if (sqlCode == null) {
			currTblConnector.closeConnection();
		}
		if (mObjects.size() < 1) {
			if (!sqlError) {
				showMessage("No result set obtained!", true);
			}
			return null;
		}
		tEnd = System.currentTimeMillis() - tStart;
		System.out.println("* Ready. Elapsed time " + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s)");
		showMessage("Successfully got " + mObjects.size() + " trajectories!", false);

		String layerName = "Trajectories from " + tableNameInUI + " (" + currTblConnector.getComputerName() + ")";
		if (samplingPanel != null && samplingPanel.getUseSampling()) {
			layerName += " sample by " + ((samplingPanel.isSamplingByObjects()) ? "objects" : "trajectories") + ", size=" + samplingPanel.getSampleSize() + ((Float.isNaN(samplingPanel.getSeed())) ? "" : ", seed=" + samplingPanel.getSeed());
		}
		//make a table corresponding to these objects
		DataTable dtTraj = TrajectoriesTableBuilder.makeTrajectoryDataTable(mObjects);
		dtTraj.setName(layerName + ": general data");
		dtTraj.setMadeByAction(actionDescr);
		ResultDescr aRes = new ResultDescr();
		aRes.product = dtTraj;
		actionDescr.addResultDescr(aRes);

		//make a layer with the trajectories
		DGeoLayer tLayer = new DGeoLayer();
		tLayer.setType(Geometry.line);
		tLayer.setName(layerName);
		tLayer.setGeoObjects(mObjects, true);
		tLayer.setHasMovingObjects(true);
		DrawingParameters dp = tLayer.getDrawingParameters();
		if (dp == null) {
			dp = new DrawingParameters();
			tLayer.setDrawingParameters(dp);
		}
		dp.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
		dp.lineWidth = 2;
		dp.transparency = 0;
		aRes = new ResultDescr();
		aRes.product = tLayer;
		actionDescr.addResultDescr(aRes);

		DataLoader dLoader = core.getDataLoader();
		int trTblN = dLoader.addTable(dtTraj);
		tLayer.setDataTable(dtTraj);
		dLoader.addMapLayer(tLayer, -1, samplingPanel.coordsAreGeographic);
		dLoader.setLink(tLayer, trTblN);
		tLayer.setLinkedToTable(true);
		tLayer.setThematicFilter(dtTraj.getObjectFilter());
		tLayer.setLinkedToTable(true);
		dLoader.processTimeReferencedObjectSet(tLayer);
		dLoader.processTimeReferencedObjectSet(dtTraj);

		actionDescr.endTime = System.currentTimeMillis();
		core.logAction(actionDescr);

		return dtTraj;
	}

	/**
	 * Extracts starts and ends of trajectories from the DB
	 */
	protected void getStartsEnds() {
		if (currTblConnector.getConnection() == null) {
			currTblConnector.reOpenConnection();
			if (currTblConnector.getConnection() == null) {
				showMessage("Failed to get a connection!", true);
				return;
			}
		}
		String tableName = currTblConnector.getTableDescriptor(0).tableName;
		Vector v = ((OracleConnector) currTblConnector).getTrajectoriesStartsEnds(tableName);
		if (v == null) {
			showMessage("Error when performing DB operation", true);
			return;
		}
		currTblConnector.closeConnection();
		boolean bPhysicalTime = v.elementAt(2) != null;

		char datePrecision = 's';
		if (bPhysicalTime) {
			datePrecision = TimeDialogs.askDesiredDatePrecision();
		}
		String dateScheme = TimeDialogs.getSuitableDateScheme(datePrecision);

		Vector iaTID = (Vector) v.elementAt(0);
		Vector vEID = (Vector) v.elementAt(1), vStartDate = (bPhysicalTime) ? (Vector) v.elementAt(2) : null, vStartTime = (bPhysicalTime) ? (Vector) v.elementAt(3) : null, vEndDate = (bPhysicalTime) ? (Vector) v.elementAt(4) : null, vEndTime = (bPhysicalTime) ? (Vector) v
				.elementAt(5) : null;
		FloatArray faStartX = (FloatArray) v.elementAt(6), faStartY = (FloatArray) v.elementAt(7), faEndX = (FloatArray) v.elementAt(8), faEndY = (FloatArray) v.elementAt(9), faStopDuration = (FloatArray) v.elementAt(10);
		LongArray laStart = (bPhysicalTime) ? null : (LongArray) v.elementAt(3), laEnd = (bPhysicalTime) ? null : (LongArray) v.elementAt(5);

		String ptTableName = "Starts and ends of trajectories in " + tableName + " " + "(" + currTblConnector.getComputerName() + ")";
		DataTable dtPoint = new DataTable();
		dtPoint.setName(ptTableName);
		int tIDCN = -1, eIDCN = -1, xCN = -1, yCN = -1, startOrEndCN = -1, tCN = -1, tSD = -1;
		//dtPoint.setEntitySetIdentifier("points_"+tableName+"_"+currTblConnector.getComputerName()); //todo: unique names
		dtPoint.addAttribute("x", "x", AttributeTypes.real);
		xCN = dtPoint.getAttrCount() - 1;
		dtPoint.addAttribute("y", "y", AttributeTypes.real);
		yCN = dtPoint.getAttrCount() - 1;
		dtPoint.addAttribute("Trajectory ID", "TID", AttributeTypes.character);
		tIDCN = dtPoint.getAttrCount() - 1;
		dtPoint.addAttribute("Entity ID", "EID", AttributeTypes.character);
		eIDCN = dtPoint.getAttrCount() - 1;
		dtPoint.addAttribute("StartOrEnd", "start_end", AttributeTypes.character);
		startOrEndCN = dtPoint.getAttrCount() - 1;
		dtPoint.addAttribute("Time", "time", AttributeTypes.time);
		tCN = dtPoint.getAttrCount() - 1;
		dtPoint.addAttribute("Stop duration", "stopDuration", AttributeTypes.real);
		tSD = dtPoint.getAttrCount() - 1;
		if (bPhysicalTime) {
			dtPoint.addAttribute("Stop duration (hours)", "stopDuration_hours", AttributeTypes.real);
			dtPoint.addAttribute("Stop duration (minutes)", "stopDuration_minutes", AttributeTypes.real);
		}
		TimeMoment start = null, end = null;
		for (int i = 0; i < iaTID.size(); i++) {
			TimeMoment tmStart = null, tmEnd = null;
			if (bPhysicalTime) {
				// trajectory: start time
				spade.time.Date startTime = new spade.time.Date();
				startTime.setPrecision(datePrecision);
				startTime.setDateScheme(dateScheme);
				java.util.Date date = (java.util.Date) vStartDate.elementAt(i);
				java.sql.Time time = (java.sql.Time) vStartTime.elementAt(i);
				startTime.setDate(date, time);
				tmStart = startTime;
				// trajectory: end time
				spade.time.Date endTime = new spade.time.Date();
				endTime.setPrecision(datePrecision);
				endTime.setDateScheme(dateScheme);
				date = (java.util.Date) vEndDate.elementAt(i);
				time = (java.sql.Time) vEndTime.elementAt(i);
				endTime.setDate(date, time);
				tmEnd = endTime;
			} else {
				spade.time.TimeCount tcStart = new spade.time.TimeCount();
				tcStart.setMoment(laStart.elementAt(i));
				tmStart = tcStart;
				spade.time.TimeCount tcEnd = new spade.time.TimeCount();
				tcEnd.setMoment(laEnd.elementAt(i));
				tmEnd = tcEnd;
			}
			if (start == null || start.compareTo(tmStart) > 0) {
				start = tmStart;
			}
			if (end == null || end.compareTo(tmEnd) < 0) {
				end = tmEnd;
			}
			DataRecord drp = new DataRecord("" + 2 * i);
			drp.setAttrValue(String.valueOf(iaTID.elementAt(i)), tIDCN);
			drp.setAttrValue((String) vEID.elementAt(i), eIDCN);
			drp.setNumericAttrValue(faStartX.elementAt(i), xCN);
			drp.setNumericAttrValue(faStartY.elementAt(i), yCN);
			drp.setAttrValue("start", startOrEndCN);
			drp.setAttrValue(tmStart, tCN);
			dtPoint.addDataRecord(drp);
			// point: end
			drp = new DataRecord("" + (2 * i + 1));
			drp.setAttrValue(String.valueOf(iaTID.elementAt(i)), tIDCN);
			drp.setAttrValue((String) vEID.elementAt(i), eIDCN);
			drp.setNumericAttrValue(faEndX.elementAt(i), xCN);
			drp.setNumericAttrValue(faEndY.elementAt(i), yCN);
			drp.setAttrValue("end", startOrEndCN);
			drp.setAttrValue(tmEnd, tCN);
			drp.setAttrValue(faStopDuration.elementAt(i), tSD);
			if (bPhysicalTime) {
				drp.setAttrValue(24 * faStopDuration.elementAt(i), tSD + 1);
				drp.setAttrValue(60 * 24 * faStopDuration.elementAt(i), tSD + 2);
			}
			dtPoint.addDataRecord(drp);
		}
		boolean hasYears = bPhysicalTime, hasMonths = ((spade.time.Date) start).requiresElement('m'), hasDays = ((spade.time.Date) start).requiresElement('d'), hasHours = ((spade.time.Date) start).requiresElement('h');
		if (bPhysicalTime) {
			start.setPrecision('y');
			end.setPrecision('y');
			hasYears = end.subtract(start) > 0;
			if (!hasYears && hasMonths) { //check whether the months are really needed
				start.setPrecision('m');
				end.setPrecision('m');
				hasMonths = end.subtract(start) > 0;
			}
			if (!hasMonths && hasDays) { //check whether the days are really needed
				start.setPrecision('d');
				end.setPrecision('d');
				hasDays = end.subtract(start) > 0;
			}
			if (!hasDays && hasHours) { //check whether the hours are really needed
				start.setPrecision('h');
				end.setPrecision('h');
				hasHours = end.subtract(start) > 0;
			}
			start.setPrecision(datePrecision);
			end.setPrecision(datePrecision);
			char optPrecision = datePrecision;
			int yearCNp = -1, monthCNp = -1, dowCNp = -1, hourCNp = -1, dateCN = -1, timeCN = -1;
			if (hasDays && hasHours) {
				dtPoint.addAttribute("Date", "date", AttributeTypes.time);
				dateCN = dtPoint.getAttrCount() - 1;
				dtPoint.addAttribute("Time of day", "time_day", AttributeTypes.time);
				timeCN = dtPoint.getAttrCount() - 1;
			} else if (!hasHours) {
				dtPoint.getAttribute(tCN).setName("Date");
			}
			if (hasYears) {
				dtPoint.addAttribute("Year", "year", AttributeTypes.integer);
				yearCNp = dtPoint.getAttrCount() - 1;
			}
			if (hasMonths) {
				dtPoint.addAttribute("Month", "month", AttributeTypes.integer);
				monthCNp = dtPoint.getAttrCount() - 1;
			}
			if (hasDays) {
				dtPoint.addAttribute("Day of week", "dow", AttributeTypes.integer);
				dowCNp = dtPoint.getAttrCount() - 1;
			}
			if (hasHours) {
				dtPoint.addAttribute("Hour", "hour", AttributeTypes.integer);
				hourCNp = dtPoint.getAttrCount() - 1;
			}
			spade.time.Date date = null, time = null;
			if (dateCN >= 0 && timeCN >= 0) {
				date = new spade.time.Date();
				date.setDateScheme("dd/mm/yyyy");
				date.setPrecision('d');
				time = new spade.time.Date();
				time.setDateScheme("hh:tt:ss");
				time.setPrecision('s');
			}
			for (int i = 0; i < dtPoint.getDataItemCount(); i++) {
				DataRecord rec = dtPoint.getDataRecord(i);
				Date d = (Date) rec.getAttrValue(tCN);
				if (date != null && time != null) {
					date.setElementValue('y', d.getElementValue('y'));
					date.setElementValue('m', d.getElementValue('m'));
					date.setElementValue('d', d.getElementValue('d'));
					rec.setAttrValue(date.getCopy(), dateCN);
					time.setElementValue('h', d.getElementValue('h'));
					time.setElementValue('t', d.getElementValue('t'));
					time.setElementValue('s', d.getElementValue('s'));
					rec.setAttrValue(time.getCopy(), timeCN);
				}
				if (yearCNp >= 0) {
					int n = d.getElementValue('y');
					rec.setNumericAttrValue((float) n, String.valueOf(n), yearCNp);
				}
				if (monthCNp >= 0) {
					int n = d.getElementValue('m');
					rec.setNumericAttrValue((float) n, String.valueOf(n), monthCNp);
				}
				if (dowCNp >= 0) {
					int n = d.getDayOfWeek();
					rec.setNumericAttrValue((float) n, String.valueOf(n), dowCNp);
				}
				if (hourCNp >= 0) {
					int n = d.getElementValue('h');
					rec.setNumericAttrValue((float) n, String.valueOf(n), hourCNp);
				}
			}
		} else {

		}
		DataLoader dLoader = core.getDataLoader();
		int tn = dLoader.addTable(dtPoint);
		Vector points = new Vector(dtPoint.getDataItemCount(), 10);
		for (int i = 0; i < dtPoint.getDataItemCount(); i++) {
			RealPoint rp = new RealPoint((float) dtPoint.getNumericAttrValue(xCN, i), (float) dtPoint.getNumericAttrValue(yCN, i));
			SpatialEntity se = new SpatialEntity("" + i);
			se.setGeometry(rp);
			if (tCN >= 0) {
				Object val = dtPoint.getAttrValue(tCN, i);
				if (val != null && (val instanceof spade.time.Date)) {
					spade.time.Date t = (spade.time.Date) val;
					TimeReference tref = new TimeReference();
					tref.setValidFrom(t);
					tref.setValidUntil(t);
					se.setTimeReference(tref);
				}
			}
			DGeoObject dgo = new DGeoObject();
			dgo.setup(se);
			dgo.setThematicData(dtPoint.getDataRecord(i));
			points.addElement(dgo);
		}
		DGeoLayer dgl = new DGeoLayer();
		dgl.setName(dtPoint.getName());
		dgl.setType(Geometry.point);
		dgl.setGeoObjects(points, true);
		DrawingParameters dp = dgl.getDrawingParameters();
		if (dp == null) {
			dp = new DrawingParameters();
			dgl.setDrawingParameters(dp);
		}
		dp.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
		dp.fillColor = dp.lineColor;
		dLoader.addMapLayer(dgl, -1);
		dLoader.setLink(dgl, tn);
		dgl.setLinkedToTable(true);
		if (tCN >= 0) {
			dLoader.processTimeReferencedObjectSet(dgl);
			dLoader.processTimeReferencedObjectSet(dtPoint);
		}
		if (!Dialogs.askYesOrNo(getFrame(), "Generate a map layer with VECTORS (arrows) connecting " + "the starts with the corresponding ends?", "Generate vectors?"))
			return;

		int nVectors = iaTID.size();
		Vector trIds = new Vector(nVectors, 1), eIds = new Vector(nVectors, 1), starts = new Vector(nVectors, 1), ends = new Vector(nVectors, 1), startTimes = new Vector(nVectors, 1), endTimes = new Vector(nVectors, 1);
		for (int i = 0; i < dtPoint.getDataItemCount(); i += 2) {
			trIds.addElement(dtPoint.getAttrValueAsString(tIDCN, i));
			eIds.addElement(dtPoint.getAttrValueAsString(eIDCN, i));
			starts.addElement(((DGeoObject) points.elementAt(i)).getSpatialData());
			ends.addElement(((DGeoObject) points.elementAt(i + 1)).getSpatialData());
			startTimes.addElement(dtPoint.getAttrValue(tCN, i));
			endTimes.addElement(dtPoint.getAttrValue(tCN, i + 1));
		}
		DataTable dtVectors = TrajectoriesTableBuilder.makeMoveSummaryTable(trIds, eIds, null, null, starts, ends, startTimes, endTimes);
		String trTableName = "Moves of " + tableName + " (" + currTblConnector.getComputerName() + ")", vectLayerName = "Vectors of " + tableName + " (" + currTblConnector.getComputerName() + ")";
		dtVectors.setName(trTableName);
		int vectorTblN = dLoader.addTable(dtVectors);
		//dtTraj.setEntitySetIdentifier("trajectories_"+tableName+"_"+currTblConnector.getComputerName()); //todo: unique names
		DGeoLayer moveLayer = buildDiscreteMovesLayer(dtVectors, dtVectors.getAttrIndex("startID"), dtVectors.getAttrIndex("endID"), dtVectors.getAttrIndex("start_date_time"), dtVectors.getAttrIndex("end_date_time"), dgl);
		if (moveLayer != null) {
			moveLayer.setName(vectLayerName);
			moveLayer.setDataSource(moveLayer.getDataSource());
			dLoader.addMapLayer(moveLayer, dLoader.getCurrentMapN());
			dLoader.setLink(moveLayer, vectorTblN);
			moveLayer.setLinkedToTable(true);
			dLoader.processTimeReferencedObjectSet(moveLayer);
			dLoader.processTimeReferencedObjectSet(dtVectors);
		}
	}

	/**
	 * Extracts trajectory statistics and attaches it to an existing or new table.
	 */
	protected void getTrajectoryStatistics() {
		TableDescriptor td = currTblConnector.getTableDescriptor(0);
		int nc = td.getNColumns();
		Vector vNumColumnNames = new Vector(nc, 100), vQualColumnNames = new Vector(nc, 100);
		for (int i = 0; i < nc; i++) {
			String stype = td.getColumnDescriptor(i).type;
			if (stype.equals("NUMBER") || stype.equals("FLOAT") || stype.equals("DECIMAL")) {
				vNumColumnNames.addElement(td.getColumnDescriptor(i).name);
			}
			if (stype.equals("CHAR") || stype.equals("VARCHAR2")) {
				vQualColumnNames.addElement(td.getColumnDescriptor(i).name);
			}
		}
		String defStatNames[] = { "Number of positions", "Duration", "Travelled distance", "Stop duration", "Distance to next trajectory" };
		TrajectoryAggregationUI taui = new TrajectoryAggregationUI(defStatNames, true, vNumColumnNames, vQualColumnNames);
		OKDialog okd = new OKDialog(getFrame(), "Desired trajectory statistics?", true);
		okd.addContent(taui);
		okd.show();
		if (okd.wasCancelled())
			return;
		if (taui.getAttr() == null) {
			boolean someSelected = false;
			for (int i = 0; i < defStatNames.length && !someSelected; i++) {
				someSelected = taui.isDefStatSelected(i);
			}
			if (!someSelected)
				return;
		}
		String tableName = td.tableName;
		if (currTblConnector.getConnection() == null) {
			currTblConnector.reOpenConnection();
			if (currTblConnector.getConnection() == null) {
				showMessage("Failed to get a connection!", true);
				return;
			}
		}
		Vector v = ((OracleConnector) currTblConnector).getTrajectoryAggregates(tableName, taui.getAttr(), taui.getOper());
		String[] attrNames = (String[]) v.elementAt(0);
		Vector vTID = (Vector) v.elementAt(1);
		IntArray //iaTID=(IntArray)v.elementAt(1),
		iaN = (IntArray) v.elementAt(3);
		FloatArray faDuration = (FloatArray) v.elementAt(4), faDistance = (FloatArray) v.elementAt(5), faStopDuration = (FloatArray) v.elementAt(6), faDistanceNext = (FloatArray) v.elementAt(7);
		FloatArray faExtra[] = (FloatArray[]) v.elementAt(8);

		Vector vQAttrNames = taui.getQAttr(), vQAttrResults = null;
		if (vQAttrNames != null) {
			for (int i = 0; i < vQAttrNames.size(); i++) {
				Vector vTrajIds = new Vector(100, 100), vFreqVals = new Vector(100, 100);
				((OracleConnector) currTblConnector).getMostFrequentValueForGroups(tableName, (String) vQAttrNames.elementAt(i), vTrajIds, vFreqVals);
				if (vQAttrResults == null) {
					vQAttrResults = new Vector(100, 100);
				}
				vQAttrResults.addElement(vTrajIds);
				vQAttrResults.addElement(vFreqVals);
			}
		}
		currTblConnector.closeConnection();

		DataTable dtTraj = null;
		boolean newTable = false;
		DataKeeper dKeeper = core.getDataKeeper();
		if (dKeeper.getTableCount() > 0) {
			Panel p = new Panel(new BorderLayout());
			p.add(new Label("Add the data to the table:"), BorderLayout.NORTH);
			List lst = new List(Math.min(10, dKeeper.getTableCount() + 1));
			for (int i = 0; i < dKeeper.getTableCount(); i++) {
				lst.add(dKeeper.getTable(i).getName());
			}
			p.add(lst, BorderLayout.CENTER);
			Checkbox cbNewTbl = new Checkbox("make a new table", false);
			p.add(cbNewTbl, BorderLayout.SOUTH);
			while (dtTraj == null) {
				do {
					okd = new OKDialog(getFrame(), "DB processing results", false);
					okd.addContent(p);
					okd.show();
				} while (lst.getSelectedIndex() < 0 && !cbNewTbl.getState());
				if (cbNewTbl.getState()) {
					String trTableName = "Statistics for moves in " + tableName + " (" + currTblConnector.getComputerName() + ")";
					dtTraj = new DataTable();
					dtTraj.setName(trTableName);
					newTable = true;
				} else {
					DataTable table = (DataTable) dKeeper.getTable(lst.getSelectedIndex());
					int nIdsFound = 0;
					for (int i = 0; i < table.getDataItemCount(); i++) {
/*
             int nId=-1;
             try {
               nId=Integer.parseInt(table.getDataItemId(i));
             } catch (NumberFormatException nfe) { nId=-1; }
             if (nId<0) continue;
*/
						if (vTID.indexOf(table.getDataItemId(i)) >= 0) {
							++nIdsFound;
						}
					}
					if (nIdsFound < 1) {
						Dialogs.showMessage(getFrame(), "Unsuitable table: the record identifiers " + "differ from the trajectory identifiers!", "Unsuitable table!");
					} else if (nIdsFound < vTID.size())
						if (Dialogs.askYesOrNo(getFrame(), "Possibly unsuitable table: " + table.getName() + ".\nOnly " + nIdsFound + " table records have appropriate identifiers.\n" + "The table contains " + table.getDataItemCount() + " records.\n"
								+ vTID.size() + " trajectory identifiers have been got from the database.\n" + "Proceed with adding data to this table?", "Possibly unsuitable table")) {
							dtTraj = table;
						} else {
							;
						}
					else {
						dtTraj = table;
					}
				}
			}
		} else {
			String trTableName = "Statistics for moves in " + tableName + " (" + currTblConnector.getComputerName() + ")";
			dtTraj = new DataTable();
			dtTraj.setName(trTableName);
			newTable = true;
		}

		boolean physicalTime = false;
		if (currTblD.timeColIdx >= 0) {
			ColumnDescriptor cd = currTblConnector.getTableDescriptor(0).getColumnDescriptor(currTblD.timeColIdx);
			physicalTime = cd instanceof ColumnDescriptorDate;
		}
		int durationHoursCN = -1, durationMinutesCN = -1, stopDurationHoursCN = -1, stopDurationMinutesCN = -1, nCN = -1, durCN = -1, distCN = -1, distanceNextCN = -1, stopDurationCN = -1;
		if (taui.isDefStatSelected(0)) {
			dtTraj.addAttribute(defStatNames[0], IdMaker.makeId("N_pos", dtTraj), AttributeTypes.integer);
			nCN = dtTraj.getAttrCount() - 1;
		}
		if (taui.isDefStatSelected(1)) {
			dtTraj.addAttribute(defStatNames[1], IdMaker.makeId("duration", dtTraj), AttributeTypes.real);
			durCN = dtTraj.getAttrCount() - 1;
			if (physicalTime) {
				dtTraj.addAttribute(defStatNames[1] + " (hours)", IdMaker.makeId("tripDuration_hours", dtTraj), AttributeTypes.real);
				durationHoursCN = dtTraj.getAttrCount() - 1;
				dtTraj.addAttribute(defStatNames[1] + " (minutes)", IdMaker.makeId("tripDuration_minutes", dtTraj), AttributeTypes.real);
				durationMinutesCN = dtTraj.getAttrCount() - 1;
			}
		}
		if (taui.isDefStatSelected(2)) {
			dtTraj.addAttribute(defStatNames[2], IdMaker.makeId("distance", dtTraj), AttributeTypes.real);
			distCN = dtTraj.getAttrCount() - 1;
		}
		if (taui.isDefStatSelected(3)) {
			dtTraj.addAttribute(defStatNames[3], IdMaker.makeId("stopDuration", dtTraj), AttributeTypes.real);
			stopDurationCN = dtTraj.getAttrCount() - 1;
			if (physicalTime) {
				dtTraj.addAttribute(defStatNames[3] + " (hours)", IdMaker.makeId("stopDuration_hours", dtTraj), AttributeTypes.real);
				stopDurationHoursCN = dtTraj.getAttrCount() - 1;
				dtTraj.addAttribute(defStatNames[3] + " (minutes)", IdMaker.makeId("stopDuration_minutes", dtTraj), AttributeTypes.real);
				stopDurationMinutesCN = dtTraj.getAttrCount() - 1;
			}
		}
		if (taui.isDefStatSelected(4)) {
			dtTraj.addAttribute(defStatNames[4], IdMaker.makeId("distance_next_", dtTraj), AttributeTypes.real);
			distanceNextCN = dtTraj.getAttrCount() - 1;
		}
		int nTrAttr = dtTraj.getAttrCount();
		for (String attrName : attrNames) {
			dtTraj.addAttribute(attrName, IdMaker.makeId(attrName, dtTraj), AttributeTypes.real);
		}
		for (int i = 0; i < vTID.size(); i++) {
			DataRecord drt = null;
			String id = (String) vTID.elementAt(i); // String.valueOf(iaTID.elementAt(i));
			if (newTable) {
				drt = new DataRecord(id);
				dtTraj.addDataRecord(drt);
			} else {
				int idx = dtTraj.indexOf(id);
				if (idx < 0) {
					continue;
				}
				drt = dtTraj.getDataRecord(idx);
			}
			if (nCN >= 0) {
				drt.setNumericAttrValue(iaN.elementAt(i), nCN);
			}
			if (durCN >= 0) {
				drt.setNumericAttrValue(faDuration.elementAt(i), durCN);
			}
			if (durationHoursCN >= 0) {
				drt.setNumericAttrValue(faDuration.elementAt(i) * 24, durationHoursCN);
			}
			if (durationMinutesCN >= 0) {
				drt.setNumericAttrValue(faDuration.elementAt(i) * 24 * 60, durationMinutesCN);
			}
			if (distCN >= 0) {
				drt.setNumericAttrValue(faDistance.elementAt(i), distCN);
			}
			if (stopDurationCN >= 0) {
				drt.setNumericAttrValue(faStopDuration.elementAt(i), stopDurationCN);
			}
			if (stopDurationHoursCN >= 0) {
				drt.setNumericAttrValue(faStopDuration.elementAt(i) * 24, stopDurationHoursCN);
			}
			if (stopDurationMinutesCN >= 0) {
				drt.setNumericAttrValue(faStopDuration.elementAt(i) * 24 * 60, stopDurationMinutesCN);
			}
			if (distanceNextCN >= 0) {
				drt.setNumericAttrValue(faDistanceNext.elementAt(i), distanceNextCN);
			}
			// extra attributes of trajectories
			for (int attr = 0; attr < attrNames.length; attr++) {
				drt.setNumericAttrValue(faExtra[attr].elementAt(i), nTrAttr + attr);
			}
		}
		if (vQAttrNames != null) {
			for (int attr = 0; attr < vQAttrNames.size(); attr++) {
				String an = (String) vQAttrNames.elementAt(attr);
				dtTraj.addAttribute(an, IdMaker.makeId(an, dtTraj), AttributeTypes.character);
				Vector vTrajIds = (Vector) vQAttrResults.elementAt(2 * attr), vFreqVals = (Vector) vQAttrResults.elementAt(2 * attr + 1);
				for (int i = 0; i < vTrajIds.size(); i++) {
					DataRecord drt = null;
					String id = (String) vTrajIds.elementAt(i);
					int idx = dtTraj.indexOf(id);
					if (idx < 0) {
						continue;
					}
					drt = dtTraj.getDataRecord(idx);
					drt.setAttrValue((String) vFreqVals.elementAt(i), drt.getAttrCount() - 1);
				}
			}
		}
		if (newTable) {
			core.getDataLoader().addTable(dtTraj);
		}
		showMessage("The statistics has been added to the table " + dtTraj.getName(), false);
	}

	/**
	 * Builds a layer with vectors between discrete locations (areas of interest).
	 * The vectors are instances of DLinkObject.
	 * moveTbl contains the data defining the moves: start and end locations
	 * (specified through their identifiers) and start and end times plus,
	 * possibly, other attributes.
	 * startLocColN and endLocColN are the numbers of the table columns containing
	 * the identifiers of the start and end locations of the moves.
	 * startTimeColN and endTimeColN are the numbers of the table columns containing
	 * the identifiers of the start and end times of the moves.
	 * locLayer contains the locations (coordinates or outlines) referred to
	 * from the table.
	 */
	protected DGeoLayer buildDiscreteMovesLayer(DataTable moveTbl, int startLocColN, int endLocColN, int startTimeColN, int endTimeColN, DGeoLayer locLayer) {
		if (moveTbl == null || locLayer == null)
			return null;
		if (startLocColN < 0 || endLocColN < 0)
			return null;
		LayerFromTableGenerator lgen = null;
		String className = MovementToolRegister.getToolClassName("build_links");
		if (className != null) {
			try {
				lgen = (LayerFromTableGenerator) Class.forName(className).newInstance();
			} catch (Exception e) {
			}
		}
		if (lgen == null) {
			showMessage("Failed to generate an instance of " + className + "!", true);
			return null;
		}
		DataSourceSpec spec = (DataSourceSpec) moveTbl.getDataSource();
		if (spec == null) {
			spec = new DataSourceSpec();
			moveTbl.setDataSource(spec);
		}
		spec.toBuildMapLayer = true;
		if (spec.descriptors == null) {
			spec.descriptors = new Vector(5, 5);
		}
		LinkDataDescription ldd = new LinkDataDescription();
		spec.descriptors.addElement(ldd);
		ldd.souColIdx = startLocColN;
		ldd.souColName = moveTbl.getAttributeName(startLocColN);
		ldd.destColIdx = endLocColN;
		ldd.destColName = moveTbl.getAttributeName(endLocColN);
		if (startTimeColN >= 0) {
			ldd.souTimeColIdx = startTimeColN;
			ldd.souTimeColName = moveTbl.getAttributeName(startTimeColN);
			Date date = null;
			for (int i = 0; i < moveTbl.getDataItemCount() && date == null; i++) {
				Object obj = moveTbl.getAttrValue(startTimeColN, i);
				if (obj == null) {
					continue;
				}
				if (obj instanceof Date) {
					date = (Date) obj;
				} else {
					break;
				}
			}
			if (date != null) {
				ldd.souTimeScheme = date.scheme;
			}
		}
		if (endTimeColN >= 0) {
			ldd.destTimeColIdx = endTimeColN;
			ldd.destTimeColName = moveTbl.getAttributeName(endTimeColN);
			Date date = null;
			for (int i = 0; i < moveTbl.getDataItemCount() && date == null; i++) {
				Object obj = moveTbl.getAttrValue(endTimeColN, i);
				if (obj == null) {
					continue;
				}
				if (obj instanceof Date) {
					date = (Date) obj;
				} else {
					break;
				}
			}
			if (date != null) {
				ldd.destTimeScheme = date.scheme;
			}
		}
		ldd.layerRef = locLayer.getContainerIdentifier();

		DGeoLayer layer = lgen.buildLayer(moveTbl, core.getDataLoader(), core.getDataLoader().getCurrentMapN());
		if (layer == null) {
			String msg = lgen.getErrorMessage();
			if (msg != null) {
				showMessage(msg, true);
			}
			return null;
		}
		DrawingParameters dp = layer.getDrawingParameters();
		if (dp == null) {
			dp = new DrawingParameters();
			layer.setDrawingParameters(dp);
		}
		dp.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
		dp.lineWidth = 2;
		dp.transparency = 40;
		return layer;
	}

	//--------------------------------------------------------------------------------------------------------------------
	/**
	 * Selection of aggregation operations
	 */
	protected void specifyAggregation() {
		if (currTblD == null || currTblConnector == null)
			return;
		if (currTblConnector.getConnection() == null) {
			currTblConnector.reOpenConnection();
			if (currTblConnector.getConnection() == null) {
				showMessage("Failed to get a connection!", true);
				return;
			}
		}
		ColumnDescriptor cd = currTblConnector.getTableDescriptor(0).getColumnDescriptor(currTblD.timeColIdx);
		//ColumnDescriptorDate cdd=(cd instanceof ColumnDescriptorDate)?(ColumnDescriptorDate)cd:null;
		Vector vcds = new Vector(3, 1);
		if (cd != null) {
			vcds.addElement(cd);
		}
		if (currTblD.xColIdx > 0) {
			ColumnDescriptorNum cdn = (ColumnDescriptorNum) currTblConnector.getTableDescriptor(0).getColumnDescriptor(currTblD.xColIdx);
			if (Float.isNaN(cdn.min)) {
				vcds.addElement(cdn);
			}
			cdn = (ColumnDescriptorNum) currTblConnector.getTableDescriptor(0).getColumnDescriptor(currTblD.yColIdx);
			if (Float.isNaN(cdn.min)) {
				vcds.addElement(cdn);
			}
		}
		if (vcds.size() > 0) {
			ColumnDescriptor cds[] = new ColumnDescriptor[vcds.size()];
			for (int i = 0; i < cds.length; i++) {
				cds[i] = (ColumnDescriptor) vcds.elementAt(i);
			}
			currTblConnector.getStatsForColumnDescriptors(currTblConnector.getTableDescriptor(0), cds);
		}
		STAggregationUI staui = new STAggregationUI(getFrame(), currTblD, cd);
		OKDialog okd = new OKDialog(getFrame(), "Desired aggregation(s)?", true);
		okd.addContent(staui);
		okd.show();
		if (okd.wasCancelled())
			return;
		if (staui.getDoDirectionAndSpeed()) {
			DirectionAndSpeedDivisionSpec dasds = new DirectionAndSpeedDivisionSpec();
			dasds.minSpeed = staui.getDandSMinSpeed();
			dasds.nSegments = staui.getDandSNDirections();
			dasds.columnName = staui.getDandSDirectionColumnName();
			dasds.speedColumnName = staui.getDandSSpeedColumnName();
			if (currTblD.dbProc == null) {
				currTblD.dbProc = new DBProcedureSpec();
			}
			currTblD.dbProc.addDivisionSpec(dasds);
		}
		if (staui.vDivisionSpecTime != null) {
			for (int i = 0; i < staui.vDivisionSpecTime.size(); i++) {
				DivisionSpec div = (DivisionSpec) staui.vDivisionSpecTime.elementAt(i);
				currTblD.dbProc.addDivisionSpec(div);
			}
		}
		if (staui.getDoSpatialGrid()) {
			specifySpatialAggregation();
		}
	}

	/**
	 * Starts a sequence of dialogs to specify spatial data aggregation method and
	 * parameters
	 */
	protected void specifySpatialAggregation() {
		if (currTblD == null || currTblConnector == null)
			return;
		if (currTblD.xColName == null || currTblD.yColName == null)
			return;
		TextCanvas tc = new TextCanvas();
		tc.addTextLine("Do you wish to apply spatial aggregation by cells of " + "a regular rectangular grid?");
		Panel p = new Panel(new ColumnLayout());
		p.add(tc);
		CheckboxGroup cbg = new CheckboxGroup();
		Checkbox noCB = new Checkbox("no", false, cbg);
		p.add(noCB);
		Checkbox existCB = null;
		List gridList = null;
		//find existing grid layers
		Vector grids = null;
		SystemUI ui = core.getUI();
		int mapN = (ui != null) ? ui.getCurrentMapN() : 0;
		if (mapN < 0) {
			mapN = 0;
		}
		LayerManager lman = (core.getDataKeeper() != null) ? core.getDataKeeper().getMap(mapN) : null;
		if (lman != null && lman.getLayerCount() > 0) {
			for (int i = 0; i < lman.getLayerCount(); i++)
				if (lman.getGeoLayer(i) instanceof DVectorGridLayer) {
					if (grids == null) {
						grids = new Vector(lman.getLayerCount() - i, 1);
					}
					grids.addElement(lman.getGeoLayer(i));
				}
		}
		if (grids != null && grids.size() > 0) {
			gridList = new List(Math.min(10, grids.size()));
			existCB = new Checkbox("use existing grid layer:", false, cbg);
			p.add(existCB);
			for (int i = 0; i < grids.size(); i++) {
				DVectorGridLayer grl = (DVectorGridLayer) grids.elementAt(i);
				gridList.add(grl.getName());
			}
			p.add(gridList);
			gridList.select(0);
		}
		Checkbox newGridCB = new Checkbox("build a new grid", true, cbg);
		p.add(newGridCB);

		OKDialog ok = new OKDialog(getFrame(), "Spatial aggregation?", false);
		ok.addContent(p);
		ok.show();

		if (newGridCB.getState()) {
			//build a new grid
			dialogInProgress = true;
			GridBuildPanel gbp = null;
			//find the bounding rectangle for the data
			float x1 = Float.NaN, y1 = Float.NaN, x2 = Float.NaN, y2 = Float.NaN;
			boolean foundX = false, foundY = false;
			TableDescriptor td = currTblConnector.getTableDescriptor(0);
			ColumnDescriptor cd = td.getColumnDescriptor(currTblD.xColIdx);
			if (cd instanceof ColumnDescriptorNum) {
				ColumnDescriptorNum cdn = (ColumnDescriptorNum) cd;
				foundX = true;
				x1 = cdn.min;
				x2 = cdn.max;
			}
			cd = td.getColumnDescriptor(currTblD.yColIdx);
			if (cd instanceof ColumnDescriptorNum) {
				ColumnDescriptorNum cdn = (ColumnDescriptorNum) cd;
				foundY = true;
				y1 = cdn.min;
				y2 = cdn.max;
			}
			boolean geo = false;
			if (lman != null) {
				geo = lman.isGeographic();
			} else {
				geo = Dialogs.askYesOrNo(core.getUI().getMainFrame(), "Are the coordinates geographic (X is longitude and Y is latitude)?", "Geographic?");
			}
			if (foundX && foundY) {
				gbp = new GridBuildPanel(core, this, x1, x2, y1, y2, geo);
			} else {
				gbp = new GridBuildPanel(core, this, geo);
			}
			dialogFrame = new Frame("Construct a grid for aggregation");
			dialogFrame.setLayout(new BorderLayout());
			dialogFrame.add(gbp);
			dialogFrame.pack();
			Dimension ss = Toolkit.getDefaultToolkit().getScreenSize(), fs = dialogFrame.getSize();
			dialogFrame.setLocation(ss.width - fs.width - 50, 50);
			dialogFrame.setVisible(true);
		} else if (existCB != null && existCB.getState()) {
			aggregateBySpatialGrid((DVectorGridLayer) grids.elementAt(gridList.getSelectedIndex()));
		} else {
			aggregateBySpatialGrid(null);
		}
	}

	protected void defineAggregationOperations() {
		if (currTblD.relevantColumns != null) {
			TableDescriptor td = currTblConnector.getTableDescriptor(0);
			int nc = td.getNColumns();
			Vector vNumColumnNames = new Vector(nc, 100), vQualColumnNames = new Vector(nc, 100);
			for (int i = 0; i < nc; i++) {
				String stype = td.getColumnDescriptor(i).type;
				if (stype.equals("NUMBER") || stype.equals("FLOAT") || stype.equals("DECIMAL")) {
					vNumColumnNames.addElement(td.getColumnDescriptor(i).name);
				}
				if (stype.equals("CHAR") || stype.equals("VARCHAR2")) {
					vQualColumnNames.addElement(td.getColumnDescriptor(i).name);
				}
			}
			String defStatNames[] = { "Number of entities", "Number of trajectories", "Number of points" }, defStatAttrName[] = { "Count_entities", "Count_traj", "Count_points" };
			if (currTblD.dataMeaning == DataSemantics.TIME_RELATED) {
				defStatNames = new String[1];
				defStatNames[0] = "Number of events";
				defStatAttrName = new String[1];
				defStatAttrName[0] = "Count_events";
			}
			TrajectoryAggregationUI taui = new TrajectoryAggregationUI(defStatNames, currTblD.dataMeaning == DataSemantics.MOVEMENTS && currTblD.relevantColumns[DataSemantics.idxOfTrajId] != null, vNumColumnNames, vQualColumnNames);
			OKDialog ok = new OKDialog(getFrame(), "Specify aggregation operations", true);
			ok.addContent(taui);
			ok.show();
			if (ok.wasCancelled())
				return;
			for (int i = 0; i < defStatNames.length; i++)
				if (taui.isDefStatSelected(i)) {
					AttrStatSpec spec = new AttrStatSpec();
					spec.columnIdx = -1; //colIdx; ?? is it needed
					spec.columnName = defStatAttrName[i];
					spec.statNames = new Vector(1, 1);
					spec.resultColNames = new Vector(1, 1);
					spec.statNames.addElement(defStatAttrName[i]);
					spec.resultColNames.addElement(defStatAttrName[i]);
					currTblD.dbProc.addAttrStatSpec(spec);
				}
			int nspec = (taui.getAttr() == null) ? 0 : taui.getAttr().length;
			for (int i = 0; i < nspec; i++) {
				AttrStatSpec spec = new AttrStatSpec();
				spec.columnIdx = -1; //colIdx; ?? is it needed
				spec.columnName = taui.getAttr()[i];
				spec.statNames = new Vector(1, 1);
				spec.resultColNames = new Vector(1, 1);
				int ioper = taui.getOper()[i];
				String soper;
				switch (ioper) {
				//case 0: soper="max";      break;
				case 1:
					soper = "min";
					break;
				case 2:
					soper = "max-min";
					break;
				case 3:
					soper = "avg";
					break;
				case 4:
					soper = "stddev";
					break;
				case 5:
					soper = "median";
					break;
				default:
					soper = "max";
				}

				spec.statNames.addElement(soper);
				spec.resultColNames.addElement(soper + "_" + taui.getAttr()[i]);
				currTblD.dbProc.addAttrStatSpec(spec);
			}
		}
		System.out.println(">>> Formed " + currTblD.dbProc.getAttrStatSpecCount() + " attribute statistics specifications!");
	}

	/**
	 * Called when the user has finished building or selectiong of a grid for
	 * spatial aggregation
	 */
	protected void aggregateBySpatialGrid(DVectorGridLayer grid) {
		if (currTblD == null || currTblConnector == null)
			return;

		// dividing by spatial grid
		if (grid != null) {
			float colXCoords[] = grid.getColXCoords(), rowYCoords[] = grid.getRowYCoords();
			if (colXCoords != null && rowYCoords != null) {
				if (currTblD.dbProc == null) {
					currTblD.dbProc = new DBProcedureSpec();
				}
				NumAttrDivisionSpec div = new NumAttrDivisionSpec();
				div.columnIdx = currTblD.xColIdx;
				div.columnName = currTblD.xColName;
				div.breaks = new FloatArray(colXCoords.length, 10);
				for (float colXCoord : colXCoords) {
					div.breaks.addElement(colXCoord);
				}
				currTblD.dbProc.addDivisionSpec(div);
				div = new NumAttrDivisionSpec();
				div.columnIdx = currTblD.yColIdx;
				div.columnName = currTblD.yColName;
				div.breaks = new FloatArray(rowYCoords.length, 10);
				for (float rowYCoord : rowYCoords) {
					div.breaks.addElement(rowYCoord);
				}
				currTblD.dbProc.addDivisionSpec(div);
			}
		}

		// define aggregation operations
		defineAggregationOperations();

		// create resulting table
		DataTable dt = new DataTable();
		dt.addAttribute("x", "x", AttributeTypes.real);
		dt.addAttribute("y", "y", AttributeTypes.real);
		Parameter pars[] = null;
		int idxDasds = -1; // index of DirectionAndSpeed division specification
		int nSignColumns = 5;
		float minSpeed = 0f;
		if (currTblD.dbProc.getDivisionSpecCount() > 2) {
			pars = new Parameter[currTblD.dbProc.getDivisionSpecCount() - 2];
			for (int pidx = 0; pidx < currTblD.dbProc.getDivisionSpecCount() - 2; pidx++) {
				DivisionSpec div = (DivisionSpec) currTblD.dbProc.getDivisionSpec(pidx);
				if (div instanceof DirectionAndSpeedDivisionSpec) {
					idxDasds = pidx;
					nSignColumns = div.getPartitionCount();
					minSpeed = ((DirectionAndSpeedDivisionSpec) div).minSpeed;
				}
				pars[pidx] = new Parameter();
				String str = "parameter " + pidx;
				if (div instanceof TimeCycleDivisionSpec) {
					TimeCycleDivisionSpec tcDiv = (TimeCycleDivisionSpec) div;
					if (tcDiv.cycleCode.equals("MM")) {
						str = "Month";
					} else if (tcDiv.cycleCode.equals("D")) {
						str = "Day of week";
					} else if (tcDiv.cycleCode.equals("HH24")) {
						str = "Hour";
					} else if (tcDiv.cycleCode.equals("MI")) {
						str = "Minute";
					} else if (tcDiv.cycleCode.equals("SS")) {
						str = "Second";
					}
					pars[pidx].setTemporal(true);
				}
				TimeLineDivisionSpec tlDiv = null;
				if (div instanceof TimeLineDivisionSpec) {
					str = "Time interval (start)";
					pars[pidx].setTemporal(true);
					tlDiv = (TimeLineDivisionSpec) div;
				}
				if (div instanceof DirectionAndSpeedDivisionSpec) {
					str = "Direction and Speed";
				}
				pars[pidx].setName(str);
				for (int vidx = 0; vidx < div.getPartitionCount(); vidx++)
					if (!pars[pidx].isTemporal()) {
						pars[pidx].addValue(div.getPartitionLabel(vidx));
					} else {
						if (tlDiv != null) {
							pars[pidx].addValue(tlDiv.getBreak(vidx));
						} else {
							TimeCount tc = new TimeCount((long) (vidx + 1));
							tc.setShownValue(div.getPartitionLabel(vidx));
							pars[pidx].addValue(tc);
						}
					}
				if (tlDiv != null && (pars[pidx].getFirstValue() instanceof Date)) {
					String expl[] = { "Time interval: from " + pars[pidx].getFirstValue() + " to " + pars[pidx].getLastValue() };
					char datePrecision = TimeDialogs.askDesiredDatePrecision(expl, (Date) pars[pidx].getFirstValue());
					String dateScheme = ((Date) pars[pidx].getFirstValue()).scheme;
					if (datePrecision != ((Date) pars[pidx].getFirstValue()).getPrecision()) {
						dateScheme = Date.removeExtraElementsFromScheme(dateScheme, datePrecision);
					}
					EnterDateSchemeUI enterSch = new EnterDateSchemeUI("Edit, if desired, the date/time scheme (template) for the parameter values. " + " The chosen precision is " + Date.getTextForTimeSymbol(datePrecision) + ". " + expl[0],
							"Date scheme:", dateScheme);
					OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Date/time scheme", false);
					dia.addContent(enterSch);
					dia.show();
					dateScheme = enterSch.getScheme();
					for (int i = 0; i < pars[pidx].getValueCount(); i++) {
						((Date) pars[pidx].getValue(i)).setPrecision(datePrecision);
						((Date) pars[pidx].getValue(i)).setDateScheme(dateScheme);
					}
				}
				if (grid.getThematicData() != null) {
					for (int i = 1; i <= grid.getThematicData().getParamCount(); i++) {
						Parameter par0 = grid.getThematicData().getParameter(pars[pidx].getName());
						if (par0 == null) {
							break;
						}
						if (par0.isSame(pars[pidx])) {
							break;
						}
						pars[pidx].setName(str + "_" + i);
					}
				}
				dt.addParameter(pars[pidx]);
			}
			for (int i = 0; i < currTblD.dbProc.getAttrStatSpecCount(); i++) {
				AttrStatSpec ass = currTblD.dbProc.getAttrStatSpec(i);
				String tblColumnName = (String) ass.resultColNames.elementAt(0);
				Attribute attrRoot = new Attribute(tblColumnName, (tblColumnName.startsWith("Count_")) ? AttributeTypes.integer : AttributeTypes.real);
				attrRoot.setName(tblColumnName);
				prepareNDimensionalAttributes(dt, attrRoot, attrRoot, pars, 0, 0);
			}
		} else {
			for (int i = 0; i < currTblD.dbProc.getAttrStatSpecCount(); i++) {
				AttrStatSpec ass = currTblD.dbProc.getAttrStatSpec(i);
				String tblColumnName = (String) ass.resultColNames.elementAt(0);
				dt.addAttribute(tblColumnName, tblColumnName, (tblColumnName.startsWith("Count_")) ? AttributeTypes.integer : AttributeTypes.real);
			}
		}

		ResultSet result = doAggregationQuery(dt);
		if (result == null) {
			currTblConnector.closeConnection();
			return; // add some error message!
		}

		// load results into table
		try {
			long prevlx = -1, prevly = -1;
			DataRecord dr = null;
			while (result.next()) {
				if (currTblD.dbProc.getDivisionSpecCount() == 2) { // only spatial division
					long lx = result.getLong(1), ly = result.getLong(2); //, lcount=result.getLong(3);
					dr = new DataRecord("");
					dr.setNumericAttrValue(lx, 0);
					dr.setNumericAttrValue(ly, 1);
					for (int i = 0; i < currTblD.dbProc.getAttrStatSpecCount(); i++) {
						AttrStatSpec ass = currTblD.dbProc.getAttrStatSpec(i);
						dr.setNumericAttrValue((ass.columnName.startsWith("Count_")) ? result.getLong(3 + i) : result.getFloat(3 + i), 2 + i);
					}
					//dr.setNumericAttrValue(lcount,2);
					dt.addDataRecord(dr);
					dr = null;
				} else {
					int idx[] = new int[pars.length];
					//System.out.print("* idx for");
					for (int i = 0; i < pars.length; i++) {
						idx[i] = result.getInt(1 + i);
						//System.out.print("_"+idx[i]);
					}
					int n = idx[idx.length - 1] - 1, mult = 1;
					for (int level = idx.length - 2; level >= 0; level--) {
						mult *= pars[level + 1].getValueCount();
						n += (idx[level] - 1) * mult;
					}
					//System.out.println(" is "+n);
					mult *= pars[0].getValueCount();
					long lx = result.getLong(1 + pars.length), ly = result.getLong(2 + pars.length); //, lcount=result.getLong(3+pars.length);
					if (lx == prevlx && ly == prevly) {
						//dr.setNumericAttrValue(lcount,2+n);
						for (int i = 0; i < currTblD.dbProc.getAttrStatSpecCount(); i++) {
							AttrStatSpec ass = currTblD.dbProc.getAttrStatSpec(i);
							dr.setNumericAttrValue((ass.columnName.startsWith("Count_")) ? result.getLong(3 + pars.length + i) : result.getFloat(3 + pars.length + i), 2 + n + i * mult);
						}
					} else {
						dt.addDataRecord(dr);
						dr = new DataRecord("");
						dr.setNumericAttrValue(lx, 0);
						dr.setNumericAttrValue(ly, 1);
						//dr.setNumericAttrValue(lcount,2+n);
						for (int i = 0; i < currTblD.dbProc.getAttrStatSpecCount(); i++) {
							AttrStatSpec ass = currTblD.dbProc.getAttrStatSpec(i);
							dr.setNumericAttrValue((ass.columnName.startsWith("Count_")) ? result.getLong(3 + pars.length + i) : result.getFloat(3 + pars.length + i), 2 + n + i * mult);
						}
						prevlx = lx;
						prevly = ly;
					}
				}
			}
			if (dr != null) {
				dt.addDataRecord(dr);
			}
		} catch (SQLException se) {
			System.out.println("SQL error:" + se.toString());
		}
		currTblConnector.closeConnection();
		int nRecAdded = 0;
		// attaching the table to the grid layer
		if (grid.getThematicData() == null) {
			nRecAdded = grid.attachTable(dt, 0, 1);
			if (nRecAdded < 1) {
				showMessage("Failed to add thematic data to layer " + grid.getName(), true);
				return;
			}
			String name = "Aggregated data from " + currTblD.name;
/*
      name=Dialogs.askForStringValue(core.getUI().getMainFrame(),"Table name?",
        name,null,"Table name",false);
*/
			dt.setName(name);
			DataLoader dLoader = core.getDataLoader();
			dLoader.setLink(grid, dLoader.addTable(dt));
			grid.setThematicFilter(dt.getObjectFilter());
			grid.setLinkedToTable(true);
			showMessage("Table " + dt.getName() + " has been attached to layer " + grid.getName(), false);
			if (dt.hasTemporalParameter()) {
				dLoader.processTimeParameterTable(dt);
			}
		} else {
			nRecAdded = grid.addThematicData(dt, 0, 1);
			if (nRecAdded > 0) {
				showMessage("New attributes have been added to layer " + grid.getName(), false);
			} else {
				showMessage("Failed to add new attributes to layer " + grid.getName(), true);
			}
		}
		// tuning attribute names in the resulting table
		DataTable tbl = (DataTable) grid.getThematicData();
		Vector attr = tbl.getTopLevelAttributes();
		String aNames[] = new String[attr.size()];
		for (int i = 0; i < aNames.length; i++) {
			aNames[i] = ((Attribute) attr.elementAt(i)).getName();
		}
		aNames = Dialogs.editStringValues(core.getUI().getMainFrame(), null, aNames, "Edit the names of the table attributes if needed", "Attribute names", true);
		if (aNames != null) {
			for (int i = 0; i < aNames.length; i++)
				if (aNames[i] != null) {
					((Attribute) attr.elementAt(i)).setName(aNames[i]);
				}
		}
		//put 0 instead of missing values in time series or other parameter-dependent attributes
		int nNewCols = dt.getAttrCount() - 2; //the first two columns are x and y
		int c0 = tbl.getAttrCount() - nNewCols;
		while (c0 < tbl.getAttrCount() - 1) {
			Attribute at = tbl.getAttribute(c0);
			if (!at.isNumeric()) {
				++c0;
				continue;
			}
			Attribute parent = at.getParent();
			if (parent == null) {
				++c0;
				continue;
			}
			int cNext = c0 + 1;
			while (cNext < tbl.getAttrCount() && tbl.getAttribute(cNext).getParent().equals(parent)) {
				++cNext;
			}
			for (int nr = 0; nr < tbl.getDataItemCount(); nr++) {
				boolean hasSome = false;
				DataRecord rec = tbl.getDataRecord(nr);
				for (int nc = c0; nc < cNext && !hasSome; nc++) {
					hasSome = !Double.isNaN(rec.getNumericAttrValue(nc));
				}
				if (!hasSome) {
					continue;
				}
				for (int nc = c0; nc < cNext; nc++)
					if (Double.isNaN(rec.getNumericAttrValue(nc))) {
						rec.setNumericAttrValue(0, nc);
					}
			}
			c0 = cNext;
		}
		// visualize results
		if (idxDasds >= 0) { // visualization specific for direction and speed aggregation
			boolean useMainView = grid.getVisualizer() == null;
			MapViewer mapView = core.getUI().getMapViewer((useMainView) ? "main" : "_blank_");
			if (mapView == null || mapView.getLayerManager() == null) {
				core.getUI().showMessage("No map view", true);
				return;
			}
			if (!useMainView) {
				//find the copy of the geographical layer in the new map view
				int lidx = mapView.getLayerManager().getIndexOfLayer(grid.getContainerIdentifier());
				if (lidx >= 0 && (mapView.getLayerManager().getGeoLayer(lidx) instanceof DVectorGridLayer)) {
					grid = (DVectorGridLayer) mapView.getLayerManager().getGeoLayer(lidx);
				}
			}
			DirectionAndSpeedVisualizer vis = new DirectionAndSpeedVisualizer();
			vis.setDataSource(nSignColumns, minSpeed, tbl, pars, tbl.getAttrCount() - (dt.getAttrCount() - 2));
			vis.setup();
			//VisAttrDescriptor vd=new VisAttrDescriptor();
			// we visualize last dt.getAttrCount() attributes of the table <tbl>
			Vector vaid = new Vector(dt.getAttrCount(), 1);
			for (int i = 2; i < dt.getAttrCount(); i++) {
				vaid.addElement(dt.getAttributeId(i));
			}
			if (core.getDisplayProducer().displayOnMap(vis, "direction&speed", tbl, vaid, grid, true, mapView) == null) {
				core.getUI().showMessage(core.getDisplayProducer().getErrorMessage(), true);
				return;
			}
			core.getSupervisor().registerTool(vis);
		} else {
			core.getUI().showMessage("Aggregation finished!", false);
		}
	}

	private int prepareNDimensionalAttributes(DataTable dt, Attribute attrRoot, Attribute attrParent, Parameter pars[], int level, int attrCount) {
		if (level >= pars.length)
			return attrCount;
		DivisionSpec div = (DivisionSpec) currTblD.dbProc.getDivisionSpec(level);
		Parameter par = pars[level];
		for (int vidx = 0; vidx < div.getPartitionCount(); vidx++) {
			Attribute attr = new Attribute(attrParent.getIdentifier() + "_" + (vidx + 1), attrParent.getType());
			for (int i = 0; i < attrParent.getParameterCount(); i++) {
				attr.addParamValPair(attrParent.getParamValPair(i));
			}
			attr.addParamValPair(par.getName(), par.getValue(vidx));
			if (level == pars.length - 1) {
				attrRoot.addChild(attr);
				dt.addAttribute(attr);
				attrCount++;
				//System.out.println("* "+attrCount+": "+attr.getIdentifier());
			}
			attrCount = prepareNDimensionalAttributes(dt, attrRoot, attr, pars, level + 1, attrCount);
		}
		return attrCount;
	}

	/**
	 * Makes a query to the database
	 */
	protected ResultSet doAggregationQuery(DataTable dTable) {
		return doAggregationQuery(dTable, "");
	}

	protected ResultSet doAggregationQuery(DataTable dTable, String whereStr) {
		if (currTblD == null || currTblConnector == null)
			return null;
		if (currTblD.dbProc == null || currTblD.dbProc.getDivisionSpecCount() < 1)
			return null; //no division has been specified!
		currTblD.dbProc.findCount = true;
		// now let's run this query
		if (currTblD.dbProc.getAttrStatSpecCount() > 0) {
			try {
				int nn = currTblD.dbProc.getDivisionSpecCount() - 2;
				String orderByStr = "";
				if (!(currTblD.dataMeaning == DataSemantics.TIME_RELATED) && nn > 0) {
					orderByStr = "to_char(b" + nn + "_class) || ' ' || " + "to_char(b" + (nn + 1) + "_class)";
				}
				// last 2 DivisionSpec should correspond to the spatial division
				for (int j = 0; j < currTblD.dbProc.getDivisionSpecCount(); j++) { // create helper table
					DivisionSpec ds = currTblD.dbProc.getDivisionSpec(j);
					if (ds instanceof StringDivisionSpec) {
						continue;
					}
					String tblResult = currTblD.dbTableName + "_tmp_" + j;
					currTblConnector.dropTmpTable(tblResult);
					if (ds instanceof NumAttrDivisionSpec) {
						NumAttrDivisionSpec nads = (NumAttrDivisionSpec) ds;
						String sqlString = "create table " + tblResult + " (break number)";
						System.out.println("* <" + sqlString + ">");
						long tStart = System.currentTimeMillis();
						Statement statement = currTblConnector.getConnection().createStatement();
						boolean b = statement.execute(sqlString);
						statement.close();
						System.out.println("* result=" + b);
						long tEnd = System.currentTimeMillis() - tStart;
						System.out.println("* Ready. Elapsed time " + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s)");
						tStart = System.currentTimeMillis();
						for (int k = 1; k < nads.breaks.size() - 1; k++) {
							sqlString = "insert into " + tblResult + " values (" + nads.getBreak(k) + ")";
							System.out.println("* <" + sqlString + ">");
							statement = currTblConnector.getConnection().createStatement();
							b = statement.execute(sqlString);
							//System.out.println("* result="+b);
							statement.close();
						}
						sqlString = "commit";
						System.out.println("* <" + sqlString + ">");
						statement = currTblConnector.getConnection().createStatement();
						b = statement.execute(sqlString);
						statement.close();
						System.out.println("* result=" + b);
						tEnd = System.currentTimeMillis() - tStart;
						System.out.println("* Ready. Elapsed time " + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s)");
						if (whereStr.length() > 0) {
							whereStr += " and \n";
						}
						whereStr += "(" + nads.columnName + " >= " + nads.getBreak(0) + " and " + nads.columnName + " <= " + nads.getBreak(nads.breaks.size() - 1) + ")";
					} else if (ds instanceof TimeCycleDivisionSpec) { // cyclical time
						TimeCycleDivisionSpec tcds = (TimeCycleDivisionSpec) ds;
						boolean translationNeeded = false;
						if (tcds.cycleCode == "D") {
							// in some localizations of windows it is necessary to translate days of week
							// particularly, is needed if standards=US , not needed, if "UK" !!!!
							String sqls = "select TO_NUMBER (TO_CHAR (TO_DATE ('01.01.2008','DD.MM.YYYY'),'D')) from dual";
							System.out.println("* <" + sqls + ">");
							Statement statement = currTblConnector.getConnection().createStatement();
							ResultSet rrr = statement.executeQuery(sqls);
							rrr.next();
							int tuesday = rrr.getInt(1);
							statement.close();
							translationNeeded = tuesday != 2;
							System.out.println("* result: tuesday=" + tuesday + ", translation of days is " + ((translationNeeded) ? "" : "not ") + "needed");
						}
						String sqlString = "create table " + tblResult + " (class number, prevbreak number, break number)";
						System.out.println("* <" + sqlString + ">");
						long tStart = System.currentTimeMillis();
						Statement statement = currTblConnector.getConnection().createStatement();
						boolean b = statement.execute(sqlString);
						statement.close();
						System.out.println("* result=" + b);
						long tEnd = System.currentTimeMillis() - tStart;
						System.out.println("* Ready. Elapsed time " + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s)");
						tStart = System.currentTimeMillis();
						for (int k = 0; k < tcds.getPartitionCount(); k++) {
							int breaks[] = tcds.getPartition(k);
							if (translationNeeded) {
								for (int i = 0; i < breaks.length; i++)
									if (breaks[i] == 7) {
										breaks[i] = 1;
									} else {
										breaks[i] += 1;
									}
							}
							sqlString = "insert into " + tblResult + " values (" + (k + 1) + ", " + breaks[0] + ", " + breaks[breaks.length - 1] + ")";
							System.out.println("* <" + sqlString + ">");
							statement = currTblConnector.getConnection().createStatement();
							b = statement.execute(sqlString);
							statement.close();
						}
						sqlString = "commit";
						System.out.println("* <" + sqlString + ">");
						statement = currTblConnector.getConnection().createStatement();
						b = statement.execute(sqlString);
						statement.close();
						System.out.println("* result=" + b);
						tEnd = System.currentTimeMillis() - tStart;
						System.out.println("* Ready. Elapsed time " + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s)");
					} else if (ds instanceof TimeLineDivisionSpec) { // linear time
						TimeLineDivisionSpec tlds = (TimeLineDivisionSpec) ds;
						String sqlString = "create table " + tblResult + " (break " + ((tlds.getBreak(0) instanceof Date) ? "Date" : "Number") + ")";
						System.out.println("* <" + sqlString + ">");
						long tStart = System.currentTimeMillis();
						Statement statement = currTblConnector.getConnection().createStatement();
						boolean b = statement.execute(sqlString);
						statement.close();
						System.out.println("* result=" + b);
						long tEnd = System.currentTimeMillis() - tStart;
						System.out.println("* Ready. Elapsed time " + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s)");
						tStart = System.currentTimeMillis();
						for (int k = 1; k < tlds.breaks.size() - 1; k++) {
							sqlString = "insert into " + tblResult + " values ";
							if (tlds.getBreak(k) instanceof Date) {
								Date dt = (Date) tlds.getBreak(k);
								sqlString += "(to_date('" + StringUtil.padString("" + ((dt.day >= 1 && dt.day <= 31) ? dt.day : 1), '0', 2, true) + StringUtil.padString("" + ((dt.month >= 1 && dt.month <= 12) ? dt.month : 1), '0', 2, true)
										+ StringUtil.padString("" + ((dt.year >= 1900) ? dt.year : 1900), '0', 4, true) + StringUtil.padString("" + ((dt.hour >= 0 && dt.hour <= 23) ? dt.hour : 0), '0', 2, true)
										+ StringUtil.padString("" + ((dt.min >= 0 && dt.min <= 59) ? dt.min : 0), '0', 2, true) + StringUtil.padString("" + ((dt.sec >= 0 && dt.sec <= 59) ? dt.sec : 0), '0', 2, true) + "','ddmmyyyyhh24miss'))";
							} else { // TimeCount
								TimeCount tc = (TimeCount) tlds.getBreak(k);
								sqlString += "(" + tc.getMoment() + ")";
							}
							System.out.println("* <" + sqlString + ">");
							statement = currTblConnector.getConnection().createStatement();
							b = statement.execute(sqlString);
							//System.out.println("* result="+b);
							statement.close();
						}
						sqlString = "commit";
						System.out.println("* <" + sqlString + ">");
						statement = currTblConnector.getConnection().createStatement();
						b = statement.execute(sqlString);
						statement.close();
						System.out.println("* result=" + b);
						tEnd = System.currentTimeMillis() - tStart;
						System.out.println("* Ready. Elapsed time " + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s)");
						String dtStr = "";
						for (int idx = 0; idx <= tlds.breaks.size() - 1; idx += tlds.breaks.size() - 1) {
							if (tlds.getBreak(idx) instanceof Date) {
								Date dt = (Date) tlds.getBreak(idx);
								dtStr = "(to_date('" + StringUtil.padString("" + ((dt.day >= 1 && dt.day <= 31) ? dt.day : 1), '0', 2, true) + StringUtil.padString("" + ((dt.month >= 1 && dt.month <= 12) ? dt.month : 1), '0', 2, true)
										+ StringUtil.padString("" + ((dt.year >= 1900) ? dt.year : 1900), '0', 4, true) + StringUtil.padString("" + ((dt.hour >= 0 && dt.hour <= 23) ? dt.hour : 0), '0', 2, true)
										+ StringUtil.padString("" + ((dt.min >= 0 && dt.min <= 59) ? dt.min : 0), '0', 2, true) + StringUtil.padString("" + ((dt.sec >= 0 && dt.sec <= 59) ? dt.sec : 0), '0', 2, true) + "','ddmmyyyyhh24miss'))";
							} else { // TimeCount
								TimeCount tc = (TimeCount) tlds.getBreak(idx);
								dtStr = "" + tc.getMoment();
							}
							if (whereStr.length() > 0) {
								whereStr += " and ";
							}
							whereStr += "(" + tlds.columnName + ((idx == 0) ? " >= " : " <= ") + dtStr + ")";
						}
					} else { // Direction and speed division
						DirectionAndSpeedDivisionSpec dasds = (DirectionAndSpeedDivisionSpec) ds;
						String sqlString = "create table " + tblResult + " (class number, a1 number, a2 number)";
						System.out.println("* <" + sqlString + ">");
						long tStart = System.currentTimeMillis();
						Statement statement = currTblConnector.getConnection().createStatement();
						boolean b = statement.execute(sqlString);
						statement.close();
						System.out.println("* result=" + b);
						long tEnd = System.currentTimeMillis() - tStart;
						System.out.println("* Ready. Elapsed time " + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s)");
						tStart = System.currentTimeMillis();
						float vals[][] = dasds.getIntervals();
						for (int k = -1; k < vals.length; k++) {
							if (k == -1) {
								sqlString = "insert into " + tblResult + " values (1, null, null)";
							} else {
								sqlString = "insert into " + tblResult + " values (" + Math.round(1 + vals[k][0]) + ", " + vals[k][1] + ", " + vals[k][2] + ")";
							}
							System.out.println("* <" + sqlString + ">");
							statement = currTblConnector.getConnection().createStatement();
							b = statement.execute(sqlString);
							//System.out.println("* result="+b);
							statement.close();
						}
						sqlString = "commit";
						System.out.println("* <" + sqlString + ">");
						statement = currTblConnector.getConnection().createStatement();
						b = statement.execute(sqlString);
						statement.close();
						System.out.println("* result=" + b);
						tEnd = System.currentTimeMillis() - tStart;
						System.out.println("* Ready. Elapsed time " + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s)");
					}
				}
				// helper tables already created, now do the query
				String sqlString = "select ";
				for (int j = 0; j < currTblD.dbProc.getDivisionSpecCount(); j++) {
					sqlString += ((j == 0) ? "" : ",") + ((currTblD.dbProc.getDivisionSpec(j) instanceof StringDivisionSpec) ? ((StringDivisionSpec) currTblD.dbProc.getDivisionSpec(j)).columnName : " b" + j + "_class");
				}
				for (int i = 0; i < currTblD.dbProc.getAttrStatSpecCount(); i++) {
					AttrStatSpec ass = currTblD.dbProc.getAttrStatSpec(i);
					String func = (String) ass.statNames.elementAt(0);
					// "Count_entities","Count_traj","Count_points"
					if (func.equals("Count_entities")) {
						sqlString += ", count(distinct " + currTblD.idColName + ")"; // ", count(distinct id_)"
					} else if (func.equals("Count_traj")) {
						sqlString += ", count(distinct tid_)";
					} else if (func.equals("Count_points") || func.equals("Count_events")) {
						sqlString += ", count(*)";
					} else if (func.equals("max-min")) {
						sqlString += ", MAX(" + ass.columnName + ")-MIN(" + ass.columnName + ")";
					} else {
						//if (func.startsWith("standard"))
						//sqlString+=", STDD("+ass.columnName+")";
						//else
						sqlString += ", " + func + "(" + ass.columnName + ")";
					}
				}
				//sqlString+=" count(*) from \n(\n";
				sqlString += " from \n(\n";
				sqlString += "select a.*"; // + INSERT HERE names of attributes used in aggregation instead of a.*
				for (int j = 0; j < currTblD.dbProc.getDivisionSpecCount(); j++)
					if (currTblD.dbProc.getDivisionSpec(j) instanceof StringDivisionSpec) {
						; //sqlString+=", "+((StringDivisionSpec)currTblD.dbProc.getDivisionSpec(j)).columnName+" ";
					} else {
						sqlString += ", b" + j + ".class as b" + j + "_class ";
					}
				sqlString += "from\n" + currTblD.dbTableName + " a,\n";
				boolean needsComma = false;
				for (int j = 0; j < currTblD.dbProc.getDivisionSpecCount(); j++) {
					DivisionSpec ds = currTblD.dbProc.getDivisionSpec(j);
					if (needsComma && !(currTblD.dbProc.getDivisionSpec(j) instanceof StringDivisionSpec)) {
						needsComma = false;
						sqlString += ",\n";
					}
					if (ds instanceof NumAttrDivisionSpec || ds instanceof TimeLineDivisionSpec) {
						sqlString += "(select rownum as class, a.* from (\n" + "select lag(break,1) over (order by break asc) as prevbreak, break\n" + "from " + currTblD.dbTableName + "_tmp_" + j + "\n" + "union all\n"
								+ "select max(break), null from " + currTblD.dbTableName + "_tmp_" + j + ") a\n" + "order by break) b" + j;
					}
					if (ds instanceof TimeCycleDivisionSpec || ds instanceof DirectionAndSpeedDivisionSpec) {
						sqlString += currTblD.dbTableName + "_tmp_" + j + " b" + j;
					}
					if ((currTblD.dbProc.getDivisionSpec(j) instanceof StringDivisionSpec)) {
						;
					} else {
						needsComma = true;
						sqlString += "\n";
					}
				}
				sqlString += "where " + whereStr + "\n";
				for (int j = 0; j < currTblD.dbProc.getDivisionSpecCount(); j++) {
					DivisionSpec ds = currTblD.dbProc.getDivisionSpec(j);
					String cn = ds.columnName;
					if (ds instanceof NumAttrDivisionSpec || ds instanceof TimeLineDivisionSpec) {
						sqlString += "  and (b" + j + ".prevbreak is null or " + cn + ">=b" + j + ".prevbreak) and (b" + j + ".break is null or " + cn + "<b" + j + ".break)\n";
					}
					if (ds instanceof TimeCycleDivisionSpec) {
						cn = "to_char(" + cn + ",'" + ((TimeCycleDivisionSpec) ds).cycleCode + "')";
						sqlString += "  and (b" + j + ".prevbreak=b" + j + ".break and b" + j + ".break=" + cn + " or\n" + "       b" + j + ".prevbreak<b" + j + ".break and b" + j + ".prevbreak<=" + cn + " and " + cn + "<=b" + j + ".break or\n"
								+ "       b" + j + ".prevbreak>b" + j + ".break and (b" + j + ".prevbreak<=" + cn + " or " + cn + "<=b" + j + ".break))\n";
					}
					if (ds instanceof DirectionAndSpeedDivisionSpec) {
						DirectionAndSpeedDivisionSpec dasds = (DirectionAndSpeedDivisionSpec) ds;
						String cns = dasds.speedColumnName;
						sqlString += "  and (" + cns + "<=" + dasds.minSpeed + " and b" + j + ".a1 is null or\n" + "       " + cns + ">" + dasds.minSpeed + " and " + cn + ">b" + j + ".a1 and " + cn + "<=b" + j + ".a2)\n";
					}
				}
				sqlString += ")";
				String groupByStr = "";
				for (int j = 0; j < currTblD.dbProc.getDivisionSpecCount(); j++) {
					groupByStr += ((j == 0) ? "" : ",") + ((currTblD.dbProc.getDivisionSpec(j) instanceof StringDivisionSpec) ? ((StringDivisionSpec) currTblD.dbProc.getDivisionSpec(j)).columnName : "b" + j + "_class");
				}
				sqlString += "\ngroup by " + groupByStr;
				sqlString += "\norder by " + ((orderByStr.length() > 0) ? orderByStr : groupByStr);
				/*
				sqlString+=" order by ";
				for (int j=0; j<currTblD.dbProc.getDivisionSpecCount(); j++)
				  sqlString+=((j==0)?"":",")+"b"+j+"_class";
				*/
				System.out.println("* <" + sqlString + ">");
				long tStart = System.currentTimeMillis();
				Statement statement = currTblConnector.getConnection().createStatement();
				ResultSet result = statement.executeQuery(sqlString);
				long tEnd = System.currentTimeMillis() - tStart;
				System.out.println("* Ready. Elapsed time " + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s)");
				for (int j = 0; j < currTblD.dbProc.getDivisionSpecCount(); j++)
					if (!(currTblD.dbProc.getDivisionSpec(j) instanceof StringDivisionSpec)) {
						currTblConnector.dropTmpTable(currTblD.dbTableName + "_tmp_" + j);
					}
				return result;

			} catch (SQLException se) {
				System.out.println("SQL error:" + se.toString());
				return null;
			}
		}
		return null;
	}

	/**
	 * Loads trajectories from the database by bunches
	 * and performs processing (e.g. data cleaning, selection by filter etc.)
	 */
	protected void loadAndProcessTrajectoiresInBunches() {
		boolean geo = false;
		if (core.getDataKeeper().getMap(0) != null) {
			geo = core.getDataKeeper().getMap(0).isGeographic();
		}
		// find layers with trajectories
		final Vector moveLayers = new Vector(10, 10);
		if (core != null && core.getUI() != null) {
			MapViewer mapViewer = core.getUI().getLatestMapViewer();
			if (mapViewer != null && mapViewer.getLayerManager() != null) {
				//Find instances of DGeoLayer containing trajectories
				LayerManager lman = mapViewer.getLayerManager();
				for (int i = 0; i < lman.getLayerCount(); i++) {
					GeoLayer layer = lman.getGeoLayer(i);
					if ((layer instanceof DGeoLayer) && layer.getObjectCount() > 0 && (layer.getObjectAt(0) instanceof DMovingObject)) {
						moveLayers.addElement(layer);
					}
				}
			}
		}
		//
		if (currTblD == null || currTblConnector == null)
			return;
		if (!(currTblConnector instanceof OracleConnector))
			return;
		Connection connection = currTblConnector.getConnection();
		if (connection == null) {
			currTblConnector.reOpenConnection();
			connection = currTblConnector.getConnection();
			if (connection == null) {
				showMessage("Failed to get a connection!", true);
				return;
			}
		}
		final String tableName = currTblConnector.getTableDescriptor(0).tableName;
		ResultSet result = null;
		String sqlString = "";
		// get basic statistics
		// create index table, if needed
		((OracleConnector) currTblConnector).createSEtable(tableName, (geo) ? 1 : 0);

		long nrec = 0, ntr = 0, neid = 0, mintrid = 0, maxtrid = 0;
		try {
			Statement statement = currTblConnector.getConnection().createStatement();
			sqlString = "SELECT SUM(npoints), COUNT(DISTINCT id_), COUNT(DISTINCT tid_)\n" + "FROM " + tableName + "_se";
			System.out.println("* <" + sqlString + ">");
			result = statement.executeQuery(sqlString);
			result.next();
			nrec = result.getLong(1);
			neid = result.getLong(2);
			ntr = result.getLong(3);
		} catch (SQLException se) {
			System.out.println("SQL error:" + se.toString());
		}
		// set database window parameters
		Panel p = new Panel(new ColumnLayout());
		p.add(new Label("Table: " + tableName));
		p.add(new Label("" + nrec + " points, " + neid + " entities, " + ntr + " trajectories"));
		p.add(new Label("Range of trajectory ids: " + mintrid + ".." + maxtrid));
		p.add(new Line(false));
		final Checkbox cbSkip = new Checkbox("Exclude trajectories of the layer ", false);
		final Choice chLayers = new Choice();
		if (moveLayers != null && moveLayers.size() > 0) {
			Panel pp = new Panel(new FlowLayout());
			p.add(pp);
			pp.add(cbSkip);
			pp.add(chLayers);
			for (int i = 0; i < moveLayers.size(); i++) {
				chLayers.addItem(((GeoLayer) moveLayers.elementAt(i)).getName() + " (" + ((GeoLayer) moveLayers.elementAt(i)).getObjectCount() + ")");
			}
		}
		p.add(new Line(false));
		final TextField tfSize = new TextField((ntr > 1000) ? "1000" : (ntr > 1000) ? "100" : "10", 10);
		Panel pp = new Panel(new FlowLayout());
		p.add(pp);
		pp.add(new Label("Size of bunch:"));
		pp.add(tfSize);
		pp = new Panel(new FlowLayout());
		p.add(pp);
		pp = new Panel(new FlowLayout());
		p.add(pp);
		final Label lEstTime = new Label("Estimated loading time=              (s)");
		pp.add(lEstTime);
		final Button bTry = new Button("Estimate");
		pp.add(bTry);
		bTry.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) { // estimating loading time for one bunch
				long maxid = 0;
				try {
					maxid = Integer.valueOf(tfSize.getText()).longValue();
				} catch (NumberFormatException nfe) {
					tfSize.setText("1000");
					return;
				}
				bTry.setEnabled(false);
				try {
					long tStart = System.currentTimeMillis();
					Statement statement = currTblConnector.getConnection().createStatement();
					String sqlString = "SELECT tr.id_, tr.tid_, tr.x_, tr.y_, tr.dt_\n" + "FROM " + tableName + " tr, (select * from " + tableName + "_se where rownum<" + (maxid + 1) + ") se\n" + "WHERE tr.tid_=se.tid_\n" + "order by tid_, tnum_";
					System.out.println("* <" + sqlString + ">");
					ResultSet result = statement.executeQuery(sqlString);
					while (result.next()) {
						result.getString(2);
					}
					long tEnd = System.currentTimeMillis() - tStart;
					System.out.println("* Ready. Elapsed time " + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s)");
					lEstTime.setText("Estimated loading time=" + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s)");
				} catch (SQLException se) {
					System.out.println("SQL error:" + se.toString());
				}
				bTry.setEnabled(true);
			}
		});
		p.add(new Line(false));

		Choice chProcessors = null;
		CheckboxGroup cbg = null;
		Checkbox cbUseProcessor = null;
		Vector vp = core.getProcessorsForObjectType(Processor.GEO_TRAJECTORY);
		if (vp != null && vp.size() > 0) {
			cbg = new CheckboxGroup();
			cbUseProcessor = new Checkbox("Apply processor:", true, cbg);
			p.add(cbUseProcessor);
			chProcessors = new Choice();
			for (int i = 0; i < vp.size(); i++) {
				Processor pr = (Processor) vp.elementAt(i);
				chProcessors.addItem(pr.getName());
			}
			chProcessors.select(vp.size() - 1);
			p.add(chProcessors);
		}
		Checkbox cbSummarize = new Checkbox("Summarise the trajectories", cbUseProcessor == null, cbg);
		p.add(cbSummarize);
		p.add(new Line(false));

		OKDialog dia = new OKDialog(getFrame(), "Window for database processing: settings", true);
		dia.addContent(p);
		dia.show();
		if (dia.wasCancelled())
			return;
		boolean summarize = cbSummarize.getState();
		boolean applyProcessor = !summarize && cbUseProcessor != null && cbUseProcessor.getState();
		if (!summarize && !applyProcessor)
			return;

		// processing database
		long size = 0;
		try {
			size = Integer.valueOf(tfSize.getText()).longValue();
		} catch (NumberFormatException nfe) {
			return;
		}
		ActionDescr mainActionDescr = null;
		Processor processor = null;
		IncrementalMovesSummariser mSum = null;

		if (applyProcessor) {
			processor = (Processor) vp.elementAt(chProcessors.getSelectedIndex());
			processor.initialise(core);
			processor.createUI();
			mainActionDescr = new ActionDescr();
			mainActionDescr.aName = "Apply processor \"" + processor.getName() + "\" to trajectories from " + tableName;
			String prName = processor.getClass().getName();
			int ppos = prName.lastIndexOf('.');
			if (ppos > 0) {
				prName = prName.substring(ppos + 1);
			}
			mainActionDescr.addParamValue("Processor", prName);
		} else {
			mSum = new IncrementalMovesSummariser();
			if (!mSum.init(core, tableName, false))
				return;
			TableDescriptor tDescr = currTblConnector.getTableDescriptor(0);
			ColumnDescriptor cd = tDescr.getColumnDescriptor(currTblD.timeColIdx);
			ColumnDescriptorDate cdd = (cd instanceof ColumnDescriptorDate) ? (ColumnDescriptorDate) cd : null;
			if (cdd != null) {
				if (cdd.min == null || cdd.max == null) {
					ColumnDescriptor d[] = { cdd };
					currTblConnector.getStatsForColumnDescriptors(tDescr, d);
				}
				if (cdd.min != null && cdd.max != null) {
					TimeDivisionUI tdUI = new TimeDivisionUI(cdd.min, cdd.max);
					dia = new OKDialog(getFrame(), "Temporal aggregation", true);
					dia.addContent(tdUI);
					dia.show();
					if (!dia.wasCancelled() && tdUI.getTimeBreaks() != null) {
						mSum.setTemporalAggregationParameters(tdUI.getStart(), tdUI.getEnd(), tdUI.getTimeBreaks(), tdUI.useCycle(), tdUI.getCycleUnit(), tdUI.getNCycleElements(), tdUI.getCycleName());
					}
				}
			}
			mainActionDescr = mSum.aDescr;
		}
		mainActionDescr.startTime = System.currentTimeMillis();
		mainActionDescr.addParamValue("Database URL", currTblConnector.makeDatabaseURL());
		mainActionDescr.addParamValue("Database name", currTblConnector.getDatabaseName());
		mainActionDescr.addParamValue("User", currTblConnector.getUserName());
		mainActionDescr.addParamValue("Table", tableName);
		core.logAction(mainActionDescr);

		ClassifierBasedSummarizer clSummarizer = null;
		ActionDescr sumActionDescr = null;

		if (processor != null && (processor instanceof ObjectsToClustersAssigner)) {
			RealRectangle terrBounds = null;
			int tblIdx = currTblConnector.getNumOfTableDescriptors() - 1;
			TableDescriptor td = currTblConnector.getTableDescriptor(tblIdx);
			if (td != null && td.columns != null && td.columns.size() > 0) {
				ColumnDescriptor xCD = td.getColumnDescriptor(currTblD.xColIdx), yCD = td.getColumnDescriptor(currTblD.yColIdx);
				if (xCD != null && yCD != null && (xCD instanceof ColumnDescriptorNum) && (yCD instanceof ColumnDescriptorNum)) {
					ColumnDescriptorNum xCDNum = (ColumnDescriptorNum) xCD, yCDNum = (ColumnDescriptorNum) yCD;
					if (Float.isNaN(xCDNum.min) || Float.isNaN(xCDNum.max) || Float.isNaN(yCDNum.min) || Float.isNaN(yCDNum.max)) {
						ColumnDescriptor cds[] = new ColumnDescriptor[2];
						cds[0] = xCDNum;
						cds[1] = yCDNum;
						currTblConnector.getStatsForColumnDescriptors(td, cds);
					}
					if (!Float.isNaN(xCDNum.min) && !Float.isNaN(xCDNum.max) && !Float.isNaN(yCDNum.min) && !Float.isNaN(yCDNum.max)) {
						terrBounds = new RealRectangle(xCDNum.min, yCDNum.min, xCDNum.max, yCDNum.max);
					}
				}
			}
			if (terrBounds != null) {
				clSummarizer = new ClassifierBasedSummarizer((ObjectsToClustersAssigner) processor, terrBounds, core, td.tableName);
				if (!clSummarizer.isReady()) {
					clSummarizer = null;
				} else {
					sumActionDescr = new ActionDescr();
					int nClasses = clSummarizer.getNClasses();
					sumActionDescr.aName = "Summarize trajectories from " + tableName + " by " + nClasses + " classes";
					sumActionDescr.comment = "The classes are defined by the classifier \"" + processor.getName();
					sumActionDescr.addParamValue("N of classes", new Integer(nClasses));
					sumActionDescr.addParamValue("Angle", new Float(clSummarizer.angle));
					sumActionDescr.addParamValue("Stop time", new Long(clSummarizer.stopTime));
					sumActionDescr.addParamValue("min distance between extracted points", new Float(clSummarizer.minDist));
					sumActionDescr.addParamValue("max distance between extracted points", new Float(clSummarizer.maxDist));
					sumActionDescr.addParamValue("cluster radius", new Float(clSummarizer.clRadius));
					core.logAction(sumActionDescr);
				}
			}
		}

		String sOutputDBtblName = "";
		PreparedStatement psOutput = null;

		if (processor != null) {
			String resultClassName = processor.getSingleResultClassName();
			System.out.println(resultClassName);
			if (resultClassName != null && resultClassName.equals("spade.analysis.tools.clustering.ObjectToClusterAssignment")) {
				ObjectsToClustersAssigner otcap = (ObjectsToClustersAssigner) processor;
				String defaultName = "CL" + ((otcap.mustFindClosest) ? "C01" : "F01");
				// asking user if he wants to create table with results
				p = new Panel(new BorderLayout());
				pp = new Panel(new ColumnLayout());
				pp.add(new Label("Source of trajectories: " + tableName));
				pp.add(new Line(false));
				Checkbox cbStore = new Checkbox("store cluster assignments in database", true);
				pp.add(cbStore);
				p.add(pp, BorderLayout.NORTH);
				p.add(new Label("Output:"), BorderLayout.WEST);
				TextField tfTblPrefix = new TextField(tableName + "_");
				tfTblPrefix.setEnabled(false);
				p.add(tfTblPrefix, BorderLayout.CENTER);
				TextField tfTblName = new TextField(defaultName, 35);
				p.add(tfTblName, BorderLayout.EAST);
				dia = new OKDialog(getFrame(), "Window for database processing: settings", true);
				dia.addContent(p);
				dia.show();
				if (dia.wasCancelled())
					return;
				if (cbStore.getState()) { // creating table, preparing PreparedStament for fast storing
					String tn = tfTblName.getText();
					if (tn.trim().length() == 0) {
						tn = defaultName;
					}
					sOutputDBtblName = tableName + "_" + tn;
					mainActionDescr.addParamValue("Resulting DB table", sOutputDBtblName);
					currTblConnector.dropTmpTable(sOutputDBtblName);
					String sStructure = "TRAJECTORY_ID VARCHAR2(50), CLUSTER_IDX NUMBER(6), SPECIMEN_IDX NUMBER(6), DISTANCE FLOAT(16)", columns = "TRAJECTORY_ID, CLUSTER_IDX, SPECIMEN_IDX, DISTANCE", values = "?,?,?,?", createTblSql = "CREATE TABLE "
							+ sOutputDBtblName + " (" + sStructure + ")", insertSql = "INSERT INTO " + sOutputDBtblName + " (" + columns + ") VALUES (" + values + ")";
					String sErrorMessage = "";
					try {
						sErrorMessage = "Cannot create table: <" + createTblSql + ">";
						Statement stat = connection.createStatement();
						stat.executeUpdate(createTblSql);
						stat.close();
						sErrorMessage = "Cannot prepare statement <" + insertSql + ">";
						psOutput = connection.prepareStatement(insertSql);
					} catch (SQLException se) {
						showMessage(sErrorMessage + " " + se.toString(), true);
						System.out.println("ERROR: " + sErrorMessage + " " + se.toString());
					}
				} else {
					mainActionDescr.addParamValue("Resulting DB table", "none");
				}
			}
		}

		boolean isPhysicalTime = false;
		char datePrecision = 's';
		String dateScheme = null;
		if (cbSkip.getState()) {
			DGeoLayer layer = (DGeoLayer) moveLayers.elementAt(chLayers.getSelectedIndex());
			TimeReference tr = layer.getTimeSpan();
			TimeMoment tm = tr.getValidFrom();
			isPhysicalTime = tm instanceof spade.time.Date;
			if (isPhysicalTime) {
				datePrecision = tm.getPrecision();
				dateScheme = ((Date) tm).scheme;
			}
		} else {
			try {
				Statement statement = connection.createStatement();
				sqlString = "select dt_ FROM " + tableName + " where rownum<2";
				System.out.println("* <" + sqlString + ">");
				result = statement.executeQuery(sqlString);
				ResultSetMetaData md = result.getMetaData();
				int timeColumnType = md.getColumnType(1);
				isPhysicalTime = timeColumnType == java.sql.Types.DATE || timeColumnType == java.sql.Types.TIME || timeColumnType == java.sql.Types.TIMESTAMP;
				if (isPhysicalTime) {
					String comments[] = { "(in the trajectories)" };
					datePrecision = TimeDialogs.askDesiredDatePrecision(comments);
				}
				dateScheme = TimeDialogs.getSuitableDateScheme(datePrecision);
			} catch (SQLException se) {
				showMessage("SQL error:" + se.toString(), true);
			}
		}

		mainActionDescr.startTime = System.currentTimeMillis();
		if (sumActionDescr != null) {
			sumActionDescr.startTime = mainActionDescr.startTime;
		}
		int nProcessed = 0, nFailed = 0;
		long tStartLoop = System.currentTimeMillis();
		for (long idx = 1; idx <= ntr; idx += size) {
			try {
				long tStart = System.currentTimeMillis();
				Statement statement = currTblConnector.getConnection().createStatement();
				sqlString = "SELECT tr.id_, tr.tid_, tr.x_, tr.y_, tr.dt_\n" + "FROM " + tableName + " tr, (select * from (select tid_, rownum as n from " + tableName + "_se) where n>=" + idx + " and n<" + (idx + size) + ") se\n"
						+ "WHERE tr.tid_=se.tid_\n" + "order by tid_, tnum_";
				//System.out.println("* <"+sqlString+">");
				result = statement.executeQuery(sqlString);
				String prevTid = "";
				DMovingObject mObj = null;
				while (result.next()) {
					String eId = result.getString(1);
					String id = result.getString(2);
					RealPoint pos = new RealPoint(result.getFloat(3), result.getFloat(4));
					TimeMoment tm = null;
					if (isPhysicalTime) {
						spade.time.Date dt = new spade.time.Date();
						tm = dt;
						java.util.Date date = result.getDate(5);
						java.sql.Time time = result.getTime(5);
						dt.setPrecision(datePrecision);
						if (dateScheme != null) {
							dt.setDateScheme(dateScheme);
						}
						dt.setDate(date, time);
					} else {
						long t = result.getLong(5);
						TimeCount tc = new TimeCount(t);
						tm = tc;
					}
					if (!id.equals(prevTid)) {
						// check if we should skip this trajectory
						boolean skip = mObj == null || mObj.getIdentifier() == null || mObj.getIdentifier().length() == 0;
						if (cbSkip.getState()) {
							GeoLayer layer = (GeoLayer) moveLayers.elementAt(chLayers.getSelectedIndex());
							for (int i = 0; i < layer.getObjectCount() && !skip; i++) {
								skip = prevTid.equals(layer.getObjectAt(i).getIdentifier());
							}
						}
						if (mObj != null && !skip) {
							// process trajectory;
/*
              if (mObj.getIdentifier().equals("130")) {
                System.out.println("Object "+mObj.getIdentifier());
              }
*/
							//System.out.println("* processing "+mObj.getIdentifier());
							if (processor != null) {
								Object prRes = processor.processObject(mObj);
								if (prRes != null) { // store results
									if (prRes instanceof ObjectToClusterAssignment) {
										ObjectToClusterAssignment oclas = (ObjectToClusterAssignment) prRes;
										if (clSummarizer != null && oclas.clusterN >= 0) {
											clSummarizer.processTrajectory(mObj, oclas.clusterN);
										}
										if (psOutput != null) {
											psOutput.setString(1, oclas.id);
											psOutput.setInt(2, oclas.clusterN);
											if (oclas.clusterN < 0) {
												psOutput.setString(3, "");
												psOutput.setString(4, "");
											} else {
												psOutput.setInt(3, oclas.specimenIdx);
												psOutput.setDouble(4, oclas.distance);
											}
											psOutput.executeUpdate();
										}
									}
									++nProcessed;
								} else {
									++nFailed;
									showMessage("Trajectory " + prevTid + ": processing failed!", true);
								}
							} else if (mSum != null) {
								boolean ok = mSum.addTrajectory(mObj);
								if (ok) {
									++nProcessed;
								} else {
									++nFailed;
								}
							}

							mObj = null;
							if (nProcessed % 100 == 0) {
								showMessage(nProcessed + " trajectories processed; " + nFailed + " failed", false);
							}
						}
						prevTid = id;
						mObj = new DMovingObject();
						mObj.setGeographic(geo);
						mObj.setIdentifier(id);
						if (eId != null) {
							mObj.setEntityId(eId);
						}
					}
					// add one one point to trajectory
					mObj.addPosition(pos, tm, tm, null);
				}
				if (mObj != null) {
					// process trajectory;
					if (processor != null) {
						Object prRes = processor.processObject(mObj);
						if (prRes != null) {
							if (prRes instanceof ObjectToClusterAssignment) {
								ObjectToClusterAssignment oclas = (ObjectToClusterAssignment) prRes;
								if (clSummarizer != null && oclas.clusterN >= 0) {
									clSummarizer.processTrajectory(mObj, oclas.clusterN);
								}
								if (psOutput != null) {
									psOutput.setString(1, oclas.id.trim());
									psOutput.setInt(2, oclas.clusterN);
									if (oclas.clusterN < 0) {
										psOutput.setString(3, "");
										psOutput.setString(4, "");
									} else {
										psOutput.setInt(3, oclas.specimenIdx);
										psOutput.setDouble(4, oclas.distance);
									}
									psOutput.executeUpdate();
								}
							}
							++nProcessed;
						} else {
							++nFailed;
							showMessage("Trajectory " + prevTid + ": processing failed!", true);
						}
					} else if (mSum != null) {
						boolean ok = mSum.addTrajectory(mObj);
						if (ok) {
							++nProcessed;
						} else {
							++nFailed;
						}
					}
				}
				showMessage(nProcessed + " trajectories processed; " + ((nFailed == 0) ? "" : nFailed + " failed"), false);
				long tEnd = System.currentTimeMillis(), tEndTotal = tEnd;
				tEnd -= tStart;
				tEndTotal -= tStartLoop;
				System.out.println("* ids:" + idx + ".." + (idx + size - 1) + ", elapsed time " + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s), cumulative time " + StringUtil.floatToStr(tEndTotal / 1000f, 3) + " (s)");
			} catch (SQLException se) {
				System.out.println("SQL error:\n" + sqlString + "\n" + se.toString());
			}

		}

		currTblConnector.closeConnection();
		if (processor != null) {
			processor.closeUI();
		}

		mainActionDescr.endTime = System.currentTimeMillis();
		if (sumActionDescr != null) {
			sumActionDescr.endTime = mainActionDescr.endTime;
		}

		if (clSummarizer != null) {
			DGeoLayer layer = clSummarizer.makeLayer();
			if (layer != null && sumActionDescr != null) {
				ResultDescr rd = new ResultDescr();
				rd.product = layer;
				sumActionDescr.addResultDescr(rd);
				if (layer.getThematicData() != null) {
					rd = new ResultDescr();
					rd.product = layer.getThematicData();
					sumActionDescr.addResultDescr(rd);
				}
				sumActionDescr.endTime = mainActionDescr.endTime;
			}
		}
		if (mSum != null) {
			mSum.makeLayers(null);
		}

		if (processor != null) {
			Object prRes = processor.getResult();
			if (prRes == null) {
				showMessage("No results of the processing obtained!", true);
				return;
			}
			if (prRes instanceof ClustersInfo) {
				//create a table with the classification results
				String name = "Clustering of " + tableName + " (" + currTblConnector.getComputerName() + ")";
/*
        name=Dialogs.askForStringValue(core.getUI().getMainFrame(),
            "Table name?",name,
            "A table with classification results  will be "+
            "added to the tables of the application","New table",false);
*/
				DataTable clTable = new DataTable();
				clTable.setName(name);
				clTable.addAttribute("Cluster N", "cluster_n", AttributeTypes.integer);
				clTable.addAttribute("Original size", "orig_size", AttributeTypes.integer);
				clTable.addAttribute("New size", "new_size", AttributeTypes.integer);
				clTable.addAttribute("N of prototypes", "n_specimen", AttributeTypes.integer);
				clTable.addAttribute("Original mean distance to prototype", "orig_dist", AttributeTypes.real);
				clTable.addAttribute("New mean distance to prototype", "new_dist", AttributeTypes.real);
				ClustersInfo clustersInfo = (ClustersInfo) prRes;
				for (int clIdx = 0; clIdx < clustersInfo.clusterInfos.size(); clIdx++) {
					SingleClusterInfo clIn = clustersInfo.clusterInfos.elementAt(clIdx);
					DataRecord rec = new DataRecord(clIn.clusterLabel);
					clTable.addDataRecord(rec);
					rec.setNumericAttrValue(clIn.clusterN, String.valueOf(clIn.clusterN), 0);
					rec.setNumericAttrValue(clIn.origSize, String.valueOf(clIn.origSize), 1);
					if (clIn.specimens != null && clIn.specimens.size() > 0) {
						rec.setNumericAttrValue(clIn.specimens.size(), String.valueOf(clIn.specimens.size()), 3);
						int prevSize = 0, newSize = 0;
						double meanDOrig = 0, meanDNew = 0;
						for (int spIdx = 0; spIdx < clIn.specimens.size(); spIdx++) {
							ClusterSpecimenInfo spec = clIn.specimens.elementAt(spIdx);
							if (spec.nSimilarOrig > 0) {
								meanDOrig = (meanDOrig * prevSize + spec.meanDistOrig * spec.nSimilarOrig) / (prevSize + spec.nSimilarOrig);
							}
							if (spec.nSimilarNew > 0) {
								meanDNew = (meanDNew * newSize + spec.meanDistNew * spec.nSimilarNew) / (newSize + spec.nSimilarNew);
							}
							prevSize += spec.nSimilarOrig;
							newSize += spec.nSimilarNew;
						}
						rec.setNumericAttrValue(newSize, String.valueOf(newSize), 2);
						rec.setNumericAttrValue(meanDOrig, String.valueOf(meanDOrig), 4);
						rec.setNumericAttrValue(meanDNew, String.valueOf(meanDNew), 5);
					} else {
						rec.setNumericAttrValue(0, "0", 2);
						rec.setNumericAttrValue(0, "0", 3);
					}
				}
				core.getDataLoader().addTable(clTable);
				ResultDescr rd = new ResultDescr();
				rd.product = clTable;
				rd.comment = "number of trajectories from the DB assigned to each cluster";
				mainActionDescr.addResultDescr(rd);
			}
		}
	}

	//--------------------------------------------------------------------------------------------------------------------

	protected void loadResultsOfBunchClustering() {
		if (currTblD == null || currTblConnector == null)
			return;
		if (!(currTblConnector instanceof OracleConnector))
			return;
		Connection connection = currTblConnector.getConnection();
		if (connection == null) {
			currTblConnector.reOpenConnection();
			connection = currTblConnector.getConnection();
			if (connection == null) {
				showMessage("Failed to get a connection!", true);
				return;
			}
		}
		final String tblNameTraj = currTblConnector.getTableDescriptor(0).tableName;
		ResultSet result = null;
		// select table with results, if exists
		String sqlString = currTblConnector.getOnlyMyTablesQuery();
		Choice chTables = new Choice();
		try {
			Statement statement = connection.createStatement();
			System.out.println("* <" + sqlString + ">");
			result = statement.executeQuery(sqlString);
			while (result.next()) {
				String tn = result.getString(1);
				if (tn != null && tn.length() > tblNameTraj.length() && tn.toUpperCase().startsWith(tblNameTraj.toUpperCase())) {
					chTables.addItem(tn);
				}
			}
		} catch (SQLException se) {
			showMessage("SQL error:" + se.toString(), true);
			System.out.println("SQL error:" + se.toString() + ", code <" + sqlString + ">");
		}
		if (chTables.getItemCount() == 0) {
			showMessage("No clustering results found, expected name " + tblNameTraj + "_CLXxx", true);
			return;
		}
		String str = chTables.getItem(0);
		if (chTables.getItemCount() > 1) { // selecting a table with clustering results
			Panel p = new Panel(new ColumnLayout());
			p.add(new Label("Trajectories: " + tblNameTraj));
			p.add(new Line(false));
			p.add(new Label("Clusters:"));
			p.add(chTables);
			p.add(new Line(false));
			OKDialog dia = new OKDialog(getFrame(), "Select clustering results", true);
			dia.addContent(p);
			dia.show();
			if (dia.wasCancelled())
				return;
			str = chTables.getSelectedItem();
		}
		final String tblNameClust = str;
		// get statistics of results
		final DataTable freqTable = new DataTable();
		freqTable.setEntitySetIdentifier(tblNameClust);
		freqTable.addAttribute("Cluster Index", "clust_idx", AttributeTypes.integer);
		freqTable.addAttribute("N trajectories", "count", AttributeTypes.integer);
		freqTable.addAttribute("N prototypes", "specimen", AttributeTypes.integer);
		freqTable.addAttribute("Average distance", "avg_dist", AttributeTypes.real);
		sqlString = "select cluster_idx, count(*), count (distinct specimen_idx), avg(distance)\n" + "from " + tblNameClust + "\n" + "group by cluster_idx";
		try {
			Statement statement = connection.createStatement();
			System.out.println("* <" + sqlString + ">");
			result = statement.executeQuery(sqlString);
			while (result.next()) {
				Long l = result.getLong(1);
				DataRecord rec = new DataRecord("" + l);
				freqTable.addDataRecord(rec);
				rec.setNumericAttrValue(l, 0);
				rec.setNumericAttrValue(result.getLong(2), 1);
				rec.setNumericAttrValue(result.getLong(3), 2);
				rec.setNumericAttrValue(result.getLong(4), 3);
			}
		} catch (SQLException se) {
			showMessage("SQL error:" + se.toString(), true);
			System.out.println("SQL error:" + se.toString() + ", code <" + sqlString + ">");
		}
		// select clusters to be loaded
		TableViewer tableViewer = new TableViewer(freqTable, core.getSupervisor(), new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
			}
		});
		tableViewer.setTreatItemNamesAsNumbers(true);
		Vector attr = new Vector(freqTable.getAttrCount(), 1);
		for (int i = 0; i < freqTable.getAttrCount(); i++) {
			attr.addElement(freqTable.getAttributeId(i));
		}
		tableViewer.setVisibleAttributes(attr);
		tableViewer.setTableLens(true);
		Panel p = new Panel(new BorderLayout());
		p.add(new Label("Select clusters for loading from " + tblNameClust), BorderLayout.NORTH);
		p.add(tableViewer, BorderLayout.CENTER);
		Panel pp = new Panel(new ColumnLayout());
		pp.add(new Label("Selected clusters:"));
		final Label lSelected = new Label("                                             "), lTrN = new Label("0 trajectories in total");
		pp.add(lSelected);
		pp.add(lTrN);
		pp.add(new Line(false));
		Panel ppp = new Panel(new BorderLayout());
		pp.add(ppp);
		final Button bCopyClusters = new Button("Copy selected cluster(s) to new Oracle table");
		bCopyClusters.setEnabled(false);
		ppp.add(bCopyClusters, BorderLayout.EAST);
		pp.add(new Line(false));
		p.add(pp, BorderLayout.SOUTH);
		core.getSupervisor().getHighlighter(freqTable.getEntitySetIdentifier()).addHighlightListener(new HighlightListener() {
			public void highlightSetChanged(Object source, String setId, Vector highlighted) {
			}

			public void selectSetChanged(Object source, String setId, Vector highlighted) {
				String str = "";
				long nTr = 0;
				if (highlighted != null) {
					for (int i = 0; i < highlighted.size(); i++) {
						String id = ((String) highlighted.elementAt(i));
						int rowN = freqTable.getObjectIndex(id);
						if (rowN >= 0) {
							str += ((str.length() > 0) ? "," : "") + id;
							nTr += freqTable.getNumericAttrValue(1, rowN);
						}
					}
				}
				lSelected.setText(str);
				lTrN.setText(nTr + " trajectories in total");
				bCopyClusters.setEnabled(nTr > 0);
			}
		});
		bCopyClusters.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				String sqlString = currTblConnector.getOnlyMyTablesQuery();
				try {
					List listTblNames = new List(10);
					// ask for name of a new table
					Connection connection = currTblConnector.getConnection();
					Statement statement = connection.createStatement();
					System.out.println("* <" + sqlString + ">");
					ResultSet result = statement.executeQuery(sqlString);
					while (result.next()) {
						String tn = result.getString(1);
						if (tn != null && tn.length() > tblNameTraj.length() && tn.toUpperCase().startsWith(tblNameTraj.toUpperCase())) {
							listTblNames.add(tn);
						}
					}
					TextField tfTblName = new TextField("_CLUST", 20);
					Panel p = new Panel(new BorderLayout());
					Panel pp = new Panel(new ColumnLayout());
					p.add(pp, BorderLayout.EAST);
					pp.add(new Label("Existing tables:"));
					pp.add(listTblNames);
					pp = new Panel(new ColumnLayout());
					p.add(pp, BorderLayout.WEST);
					pp.add(new Label("Prefix:"));
					pp.add(new Label(tblNameTraj, Label.RIGHT));
					pp = new Panel(new ColumnLayout());
					p.add(pp, BorderLayout.CENTER);
					pp.add(new Label("Suffix:"));
					pp.add(tfTblName);
					OKDialog dia = new OKDialog(getFrame(), "Select clustering results", true);
					dia.addContent(p);
					dia.show();
					if (dia.wasCancelled())
						return;
					String outputTable = tfTblName.getText().trim();
					if (outputTable.length() == 0) {
						outputTable = "_CLUST";
					}
					// perform operation
					Vector v = core.getSupervisor().getHighlighter(freqTable.getEntitySetIdentifier()).getSelectedObjects();
					String ids = "";
					for (int i = 0; i < v.size(); i++) {
						ids += ((i == 0) ? "" : ",") + (String) v.elementAt(i);
					}
					sqlString = "create table " + tblNameTraj + outputTable + " as\n" + "select t1.*\n" + "from " + tblNameTraj + " t1, " + tblNameClust + " t2\n" + "where (t1.tid_=t2.trajectory_id and t2.cluster_idx in (" + ids + ") )";
					statement.executeUpdate(sqlString);
				} catch (SQLException se) {
					showMessage("SQL error:" + se.toString(), true);
					System.out.println("SQL error:" + se.toString() + ", code <" + sqlString + ">");
				}
			}
		});
		OKDialog dia = new OKDialog(getFrame(), "Select clustering results", true);
		dia.addContent(p);
		lSelected.setFont(lSelected.getFont().deriveFont(Font.BOLD));
		dia.show();
		if (dia.wasCancelled())
			return;
		Vector v = core.getSupervisor().getHighlighter(freqTable.getEntitySetIdentifier()).getSelectedObjects();
		if (v == null || v.size() == 0)
			return;
		// now proceed with loading
		/*
		SQL code (trajectories):
		select a.*
		from sample_testing_m30 a,  sample_testing_m30_clc01 b
		where (a.tid_=b.trajectory_id and b.cluster_idx=9)
		order by tid_, tnum_
		SQL code (cluster IDs):
		select trajectory_id, cluster_idx, specimen_idx, distance
		from sample_testing_m30_clc01
		where cluster_idx in (1,3,4)
		*/
		String ids = "";
		for (int i = 0; i < v.size(); i++) {
			ids += ((i == 0) ? "" : ",") + (String) v.elementAt(i);
		}
		DataTable dt = loadTrajectories(tblNameClust, "select t1.*\n" + "from " + tblNameTraj + " t1, " + tblNameClust + " t2\n" + "where (t1.tid_=t2.trajectory_id and t2.cluster_idx in (" + ids + ") )");
		if (dt != null) { // adding extra attributes to the table
			dt.addAttribute("Cluster N", "clust_idx", AttributeTypes.character);
			int attrNcl = dt.getAttrCount() - 1;
			dt.addAttribute("Prototype N", "specimen", AttributeTypes.character);
			int attrNsp = dt.getAttrCount() - 1;
			dt.addAttribute("Distance", "dist", AttributeTypes.real);
			int attrNdist = dt.getAttrCount() - 1;
			try {
				Statement statement = connection.createStatement();
				sqlString = "select trajectory_id, cluster_idx, specimen_idx, distance\n" + "from " + tblNameClust + "\n" + "where cluster_idx in (" + ids + ")";
				System.out.println("* <" + sqlString + ">");
				result = statement.executeQuery(sqlString);
				while (result.next()) {
					String id = result.getString(1).trim();
					int rowN = dt.getObjectIndex(id);
					if (rowN >= 0) {
						dt.setCharAttributeValue(result.getString(2).trim(), attrNcl, rowN);
						dt.setCharAttributeValue(result.getString(3).trim(), attrNsp, rowN);
						dt.setNumericAttributeValue(result.getFloat(4), attrNdist, rowN);
					}
				}
			} catch (SQLException se) {
				showMessage("SQL error:" + se.toString(), true);
				System.out.println("SQL error:" + se.toString() + ", code <" + sqlString + ">");
			}
		}

	}

	/**
	 * Loads events from the database by bunches
	 * and aggregates them in the memory
	 */
	protected void aggregateEventsByBunches() {
		if (currTblD == null || currTblConnector == null)
			return;
		if (!(currTblConnector instanceof OracleConnector))
			return;
		Connection connection = currTblConnector.getConnection();
		if (connection == null) {
			currTblConnector.reOpenConnection();
			connection = currTblConnector.getConnection();
			if (connection == null) {
				showMessage("Failed to get a connection!", true);
				return;
			}
		}
		final String tableName = currTblConnector.getTableDescriptor(0).tableName;
		boolean physicalTime = false;
		if (currTblD.timeColIdx >= 0) {
			ColumnDescriptor cd = currTblConnector.getTableDescriptor(0).getColumnDescriptor(currTblD.timeColIdx);
			physicalTime = cd instanceof ColumnDescriptorDate;
		}
		ResultSet result = null;
		String sqlString = "";
		// get basic statistics

		long nrec = 0, mintrid = 0, maxtrid = 0;
		// processing database
		long size = 1000;
		ActionDescr aDescr = null;
		IncrementalEventsSummarizer eSum = new IncrementalEventsSummarizer();
		if (!eSum.init(core, tableName))
			return;
		TableDescriptor tDescr = currTblConnector.getTableDescriptor(0);
		ColumnDescriptor cd = tDescr.getColumnDescriptor(currTblD.timeColIdx);
		ColumnDescriptorDate cdd = (cd instanceof ColumnDescriptorDate) ? (ColumnDescriptorDate) cd : null;
		if (cdd != null) {
			if (cdd.min == null || cdd.max == null) {
				ColumnDescriptor d[] = { cdd };
				currTblConnector.getStatsForColumnDescriptors(tDescr, d);
			}
			if (cdd.min != null && cdd.max != null) {
				TimeDivisionUI tdUI = new TimeDivisionUI(cdd.min, cdd.max);
				OKDialog dia = new OKDialog(getFrame(), "Temporal aggregation", true);
				dia.addContent(tdUI);
				dia.show();
				if (!dia.wasCancelled() && tdUI.getTimeBreaks() != null) {
					eSum.setTemporalAggregationParameters(tdUI.getStart(), tdUI.getEnd(), tdUI.getTimeBreaks(), tdUI.useCycle(), tdUI.getCycleUnit(), tdUI.getNCycleElements(), tdUI.getCycleName());
				}
			}
		}
		aDescr = eSum.aDescr;
		aDescr.startTime = System.currentTimeMillis();
		aDescr.addParamValue("Database URL", currTblConnector.makeDatabaseURL());
		aDescr.addParamValue("Database name", currTblConnector.getDatabaseName());
		aDescr.addParamValue("User", currTblConnector.getUserName());
		aDescr.addParamValue("Table", tableName);
		core.logAction(aDescr);

		aDescr.startTime = System.currentTimeMillis();
		long np = 0, maxNR = -1; // N events
		String tmpTablName = tableName + "_tmp";
		currTblConnector.dropTmpTable(tmpTablName);
		try {
			Statement statement = currTblConnector.getConnection().createStatement();
			sqlString = "create table " + tmpTablName + " as (select a.*, rownum as rn from " + tableName + " a)";
			System.out.println(sqlString);
			statement.execute(sqlString);
			sqlString = "select max(rn) from " + tmpTablName;
			System.out.println(sqlString);
			result = statement.executeQuery(sqlString);
			result.next();
			maxNR = result.getLong(1);
		} catch (SQLException se) {
			System.out.println("SQL error:\n" + sqlString + "\n" + se.toString());
		}
		for (long idx = 1; idx <= maxNR; idx += size) {
			try {
				Statement statement = currTblConnector.getConnection().createStatement();
				sqlString = "select " + currTblD.xColName + "," + currTblD.yColName + "," + currTblD.timeColName + "\nfrom " + tmpTablName + "" + "\nwhere rn>=" + idx + " and rn<" + (idx + size);
				result = statement.executeQuery(sqlString);
				while (result.next()) {
					np++;
					RealPoint pos = new RealPoint(result.getFloat(1), result.getFloat(2));
					TimeMoment tm = null;
					if (physicalTime) {
						spade.time.Date dt = new spade.time.Date();
						tm = dt;
						java.util.Date date = result.getDate(3);
						java.sql.Time time = result.getTime(3);
						dt.setDate(date, time);
					} else {
						long t = result.getLong(3);
						TimeCount tc = new TimeCount(t);
						tm = tc;
					}
					eSum.accumulateEvent(pos.x, pos.y, tm, tm);
				}
				statement.close();
			} catch (SQLException se) {
				System.out.println("SQL error:\n" + sqlString + "\n" + se.toString());
				break;
			}
			showMessage(np + " events processed", false);
			System.out.println(np + " events processed");
		}

		currTblConnector.dropTmpTable(tmpTablName);

		currTblConnector.closeConnection();
		aDescr.endTime = System.currentTimeMillis();
		System.out.println("* elapsed time: " + StringUtil.floatToStr((aDescr.endTime - aDescr.startTime) / 1000f, 3) + " sec");

		eSum.putCountsInTable();
	}

	protected void extractEventInformation() {
		// find a layer with events
		//Find instances of DGeoLayer containing areas
		LayerManager lman = core.getUI().getCurrentMapViewer().getLayerManager();
		Vector eventLayers = new Vector(lman.getLayerCount(), 1);
		for (int i = 0; i < lman.getLayerCount(); i++) {
			GeoLayer layer = lman.getGeoLayer(i);
			if ((layer instanceof DGeoLayer) && layer.getObjectCount() > 0 && layer.getType() == Geometry.point && ((DGeoLayer) layer).getSourceLayer() != null) {
				eventLayers.addElement(layer);
			}
		}
		if (eventLayers.size() < 1) {
			core.getUI().showMessage("No layers with events found!", true);
			return;
		}
		Panel mainP = new Panel(new ColumnLayout());
		mainP.add(new Label("Select the layer with events:"));
		List aList = new List(Math.max(eventLayers.size() + 1, 5));
		for (int i = 0; i < eventLayers.size(); i++) {
			aList.add(((DGeoLayer) eventLayers.elementAt(i)).getName());
		}
		aList.select(aList.getItemCount() - 1);
		mainP.add(aList);
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Summarise events", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return;
		int idx = aList.getSelectedIndex();
		if (idx < 0)
			return;
		DGeoLayer eventLayer = (DGeoLayer) eventLayers.elementAt(idx);
		if (eventLayer == null || eventLayer.getObjectCount() < 1) {
			core.getUI().showMessage("No areas found!", true);
			return;
		}

		DataTable eventTable = (DataTable) eventLayer.getThematicData();
		int tblEventsIDidx = eventTable.findAttrByName("Time series Id"), tblEventsTSidx = eventTable.findAttrByName("Start time"), tblEventsTEidx = eventTable.findAttrByName("End time"), tblEventTypeIdx = eventTable.findAttrByName("Event type");

		// find last event type (if several)
		String lastEventType = eventTable.getAttrValueAsString(tblEventTypeIdx, eventTable.getDataItemCount() - 1);

		// connecting to the Table
		if (currTblD == null || currTblConnector == null)
			return;
		if (!(currTblConnector instanceof OracleConnector))
			return;
		Connection connection = currTblConnector.getConnection();
		if (connection == null) {
			currTblConnector.reOpenConnection();
			connection = currTblConnector.getConnection();
			if (connection == null) {
				showMessage("Failed to get a connection!", true);
				return;
			}
		}
		final String tableName = currTblConnector.getTableDescriptor(0).tableName;
		boolean physicalTime = false;
		if (currTblD.timeColIdx >= 0) {
			ColumnDescriptor cd = currTblConnector.getTableDescriptor(0).getColumnDescriptor(currTblD.timeColIdx);
			physicalTime = cd instanceof ColumnDescriptorDate;
		}
		ResultSet result = null;
		String sqlString = "";

		// select table columns to aggregate
		Choice ch = new Choice();
		int tblIdx = currTblConnector.getNumOfTableDescriptors() - 1;
		TableDescriptor td = currTblConnector.getTableDescriptor(tblIdx);
		for (int i = 0; i < td.columns.size(); i++) {
			ColumnDescriptor cd = (ColumnDescriptor) td.columns.elementAt(i);
			if (cd instanceof ColumnDescriptorQual && currTblD.getColumnMeaning(cd.name) == null) {
				ch.addItem(cd.name);
			}
		}
		Checkbox cb = new Checkbox("count sentences", false);
		Panel p = new Panel(new ColumnLayout());
		p.add(new Label("Select attribute"));
		p.add(ch);
		p.add(cb);
		p.add(new Line(false));
		OKDialog ok = new OKDialog(getFrame(), "Event attribute", false);
		ok.addContent(p);
		ok.show();
		String sAttr = ch.getSelectedItem();
		boolean bSentences = cb.getState();

		// processing events
		TableFilter tf = (eventTable.getObjectFilter() instanceof TableFilter) ? (TableFilter) eventTable.getObjectFilter() : null;
		String attrName = ch.getSelectedItem() + ": counts", attrMostFreq = ch.getSelectedItem() + ": most frequent";
		int idxAttr = eventTable.findAttrByName(attrName), idxMostFreq = eventTable.findAttrByName(attrName);
		if (idxAttr < 0) {
			eventTable.addAttribute(new Attribute(attrName, AttributeTypes.character));
			idxAttr = eventTable.getAttrCount() - 1;
		}
		if (!bSentences && idxMostFreq < 0) {
			eventTable.addAttribute(new Attribute(attrMostFreq, AttributeTypes.character));
			idxMostFreq = eventTable.getAttrCount() - 1;
		}

		String sx = currTblD.xColName, sy = currTblD.yColName, st = currTblD.timeColName;
		int nRecsProcessed = 0;
		Statement statement = null;
		try {
			statement = currTblConnector.getConnection().createStatement();
			for (int rec = 0; rec < eventTable.getDataItemCount(); rec++)
				if (eventTable.getAttrValueAsString(tblEventTypeIdx, rec).equals(lastEventType) && (tf == null || tf.isActive(rec))) {
					DGeoLayer polygons = ((DGeoLayer) eventLayer).getSourceLayer();
					if (polygons == null) {
						continue;
					}
					int idxPoly = polygons.getObjectIndex(eventTable.getAttrValueAsString(tblEventsIDidx, rec));
					if (idxPoly < 0) {
						continue;
					}
					DGeoObject poly = polygons.getObject(idxPoly);
					RealRectangle r = poly.getBounds();

					sqlString = "select " + sx + "," + sy + "," + sAttr + " from " + tableName + "\n" + "where " + sx + ">=" + r.rx1 + " and " + sx + "<=" + r.rx2 + " and " + sy + ">=" + r.ry1 + " and " + sy + "<=" + r.ry2;
					if (physicalTime) {
						sqlString += "\nand " + st + ">=" + ((Date) eventTable.getAttrValue(tblEventsTSidx, rec)).toSQLstring() + " and " + st + "<" + ((Date) eventTable.getAttrValue(tblEventsTEidx, rec)).toSQLstring();
					} else {
						sqlString += "\nand " + st + ">=" + ((Date) eventTable.getAttrValue(tblEventsTSidx, rec)).toString() + " and " + st + "<" + ((Date) eventTable.getAttrValue(tblEventsTEidx, rec)).toString();
					}
					System.out.println(sqlString);
					result = statement.executeQuery(sqlString);
					int N1 = 0, N2 = 0, N3 = 0;
					WordCounter wc = null;
					while (result.next()) {
						float x = result.getFloat(1), y = result.getFloat(2);
						N1++;
						// check point-in-polygon
						boolean in = poly.contains(x, y, 0);
						if (in) {
							N2++;
							String sValue = result.getString(3);
							if (sValue != null) {
								sValue = sValue.trim();
								sValue = sValue.toLowerCase();
								if (sValue.startsWith("img") || sValue.startsWith("dsc")) {
									continue;
								}
								if (sValue.endsWith(".jpg") || sValue.endsWith(".png") || sValue.endsWith(".gif")) {
									continue;
								}
								try {
									Integer.valueOf(sValue);
									continue; // skip numbers
								} catch (NumberFormatException nfe) { /* ok (not a number) */
								}
							}
							//System.out.println("* x="+x+", y="+y+", <"+sValue+">");
							N3++;
							if (wc == null) {
								wc = new WordCounter();
							}
							if (bSentences) {
								wc.addString(sValue);
							} else {
								wc.countString(sValue);
							}
						}
					}
					nRecsProcessed++;
					System.out.println("* " + nRecsProcessed + " recs processed; last: id=" + eventTable.getAttrValueAsString(tblEventsIDidx, rec) + ", N points:" + N1 + ", in poly:" + N2 + ", in use:" + N3);
					String s[] = (wc == null) ? null : wc.getMostFrequentCollocations(50, 5, 5, true, "-----"), str = "";
					if (s != null) {
						for (int i = 0; i < s.length; i++) {
							str += ((i == 0) ? "" : "\n") + s[i];
						}
					}
					if (!bSentences && wc != null) {
						wc.sortWords();
					}
					s = (wc == null) ? null : wc.getMostFrequentWords(15, true);
					if (s != null) {
						if (str.length() > 0) {
							str += "\n";
						}
						for (int i = 0; i < s.length; i++) {
							str += ((i == 0) ? "" : "\n") + s[i];
						}
					}
					System.out.println(str);
					eventTable.setCharAttributeValue(str, idxAttr, rec);
					if (idxMostFreq >= 0) {
						eventTable.setCharAttributeValue(wc.getMostFrequentWord(), idxMostFreq, rec);
					}
				}
			statement.close();
		} catch (SQLException se) {
			System.out.println("SQL error:\n" + sqlString + "\n" + se.toString());
		}

	}

	//--------------------------------------------------------------------------------------------------------------------
	/**
	* Shows the given message. If "trouble" is true, then this
	* is an error message.
	*/
	public void showMessage(String msg, boolean trouble) {
		if (core.getUI() != null) {
			core.getUI().showMessage(msg, trouble);
		} else if (trouble) {
			System.out.println("ERROR: " + msg);
		}
	}

	/**
	* Shows the given message. Assumes that this is not an error message.
	*/
	public void showMessage(String msg) {
		showMessage(msg, false);
	}

	protected Frame getFrame() {
		if (frame != null)
			return frame;
		Frame f = null;
		if (core.getUI() != null) {
			f = core.getUI().getMainFrame();
		}
		if (f == null) {
			f = CManager.getAnyFrame();
		}
		return f;
	}

	public void actionPerformed(ActionEvent e) {
		String cmd = null;
		if (e.getSource().equals(tblList)) {
			cmd = "add";
		} else {
			cmd = e.getActionCommand();
		}
		if (dialogInProgress)
			if (e.getSource() instanceof GridBuildPanel) {
				DVectorGridLayer grLayer = null;
				if (cmd.equals("finish")) {
					grLayer = ((GridBuildPanel) e.getSource()).getGridLayer();
				}
				if (dialogFrame != null) {
					dialogFrame.dispose();
					dialogFrame = null;
				}
				dialogInProgress = false;
				if (grLayer != null) {
					aggregateBySpatialGrid(grLayer);
				}
			} else
				return;
		if (cmd.equals("add")) {
			addNewTable();
		} else if (cmd.equals("column_info") || cmd.equals("analyse")) {
			int idx = tblList.getSelectedIndex();
			if (idx < 0)
				return;
			currTblD = (DBTableDescriptor) tdescr.elementAt(idx);
			currTblD.dbProc = null;
			currTblConnector = (JDBCConnector) connectors.elementAt(idx);
			if (currTblConnector == null) {
				currTblConnector = connectToTable(currTblD, false);
				if (currTblConnector == null)
					return;
				//currTblConnector.closeConnection();
				connectors.setElementAt(currTblConnector, idx);
			}
			if (cmd.equals("column_info")) {
				showColumnInfo();
			} else if (cmd.equals("analyse")) {
				analyse();
			}
		}
	}

	public void windowClosing(WindowEvent e) {
		if (dialogInProgress)
			return;
		if (e.getSource().equals(frame)) {
			frame.dispose();
			frame = null;
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
