package spade.analysis.tools.db_tools.movement;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import spade.analysis.tools.db_tools.DBTableDescriptor;
import spade.analysis.tools.db_tools.DataSemantics;
import spade.analysis.tools.db_tools.statistics.TableStatisticsDisplay;
import spade.analysis.tools.db_tools.statistics.TableStatisticsItem;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.NotificationLine;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.SplitLayout;
import spade.lib.util.StringUtil;
import db_work.data_descr.ColumnDescriptor;
import db_work.data_descr.ColumnDescriptorDate;
import db_work.data_descr.ColumnDescriptorNum;
import db_work.data_descr.TableDescriptor;
import db_work.database.OracleConnector;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 24-Jan-2007
 * Time: 12:31:26
 * Separates movement data into trajectories, i.e. from <entity_id,time,x,y,...>
 * to <trajectory_id,entity_id,time,x,y,...>
 */
public class TrajectorySeparator implements ActionListener {
	/**
	 * The number of mandatory columns required for the separator. The meanings of
	 * these columns are listed at the beginning of the array
	 * DataSemantics.movementSemantics
	 */
	static public final int nMandMoveColumns = 6;
	/**
	 * The reader used for connection to the database and processing the data
	 */
	protected OracleConnector reader = null;
	/**
	 * Descriptor of the current table (i.e. the one under analysis)
	 */
	protected DBTableDescriptor tblDescr = null;
	/**
	 * The user-specified threshold value for the temporal gap
	 */
	protected int timeGap = 0;
	/**
	 * The units in which the time gap is specified: 'y', 'm', 'd', 'h',
	 * 't' (minutes), or 's'
	 */
	protected char timeGapUnit = 0;
	/**
	 * The user-specified threshold value for the spatial gap
	 */
	protected double spaceGap = 0.;
	/**
	 * The prefix for the new table(s) produced
	 */
	protected String tablePrefix = null;
	/**
	 * The main frame of the application
	 */
	protected Frame mainFrame = null;
	/**
	 * The notification line for messages
	 */
	protected NotificationLine lStatus = null;
	/**
	 * Indicates a failure of some operation
	 */
	protected boolean error = false;

	/**
	 * Column name with IDs of trajectories, or NULL if this is the 1st separation
	 */
	protected String trIdColumn = null;

	protected Button bExplore = null;

	/**
	 * It is suposed that the reader has been previously connected to the table
	 * with the source data and the tblDescr contains all necessary metadata
	 * about the table, in particular, the meanings of the fields.
	 * @param reader - the reader connected to the database table with movement data
	 * @param tblDescr - the description (metadata) of the table
	 */
	public TrajectorySeparator(OracleConnector reader, DBTableDescriptor tblDescr, String trIdColumn) {
		this.reader = reader;
		this.tblDescr = tblDescr;
		this.trIdColumn = trIdColumn;
	}

	/**
	 * Sets a reference to the main frame of the application
	 */
	public void setMainFrame(Frame mainFrame) {
		this.mainFrame = mainFrame;
	}

	/**
	 * Sets a reference to the notification line for messages
	 */
	public void setNotificationLine(NotificationLine lStatus) {
		this.lStatus = lStatus;
	}

	/**
	 * Gets statistics about values in the column with the given number and
	 * fills it in the given TableStatisticsItem
	 */
	private void getColumnStat(TableDescriptor td, ColumnDescriptor cd, TableStatisticsItem stat) {
		if (td == null || cd == null || stat == null)
			return;
		boolean isCharOrIdent = cd.type.equalsIgnoreCase("CHAR");
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
			//Math.round(Math.random()*1000); //to be replaced
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
	 * Finds the descriptor of the column with the given name
	 */
	private ColumnDescriptor findColumnDescriptor(TableDescriptor td, String colName) {
		if (colName == null)
			return null;
		for (int i = 0; i < td.getNColumns(); i++) {
			ColumnDescriptor cd = td.getColumnDescriptor(i);
			if (colName.equals(cd.name))
				return cd;
		}
		return null;
	}

	/**
	 * 1) Gets value statistics from the table columns listed in the table metadata
	 * 2) Displays the statistics and asks the user about the separation criteria
	 * 3) Performs the separation
	 * If successfull, returns a descriptor of the new database table containing
	 * the resulting separated trajectories.
	 */
	public DBTableDescriptor doSeparation() {
		String tblNameSuffix = "";
		if (reader == null || reader.getTableDescriptor(0) == null)
			return null;
		if (tblDescr == null || tblDescr.relevantColumns == null)
			return null;
		reader.reOpenConnection();
		int tblIdx = reader.getNumOfTableDescriptors() - 1;
		TableDescriptor td = reader.getTableDescriptor(tblIdx);
		//Getting value statistics for the specified columns from the database
		for (int i = 0; i < nMandMoveColumns; i++)
			if (tblDescr.relevantColumns[i] == null || findColumnDescriptor(td, tblDescr.relevantColumns[i]) == null) {
				showMessage("Could not find a column with " + DataSemantics.movementSemantics[i], true);
				//reader.closeConnection();
				return null;
			}
		TableStatisticsItem stat[] = new TableStatisticsItem[10];
		stat[0] = new TableStatisticsItem();
		stat[0].name = "N records";
		//...get the number of records...
		long num = reader.getNrows(tblIdx);
		stat[0].value = String.valueOf(num);
		stat[1] = new TableStatisticsItem();
		stat[1].name = "ident";
		getColumnStat(td, findColumnDescriptor(td, "ID_"), stat[1]);
		stat[1].name = "N entities";
		stat[2] = new TableStatisticsItem();
		stat[2].name = "tr_ident";
		getColumnStat(td, findColumnDescriptor(td, "TID_"), stat[2]);
		stat[2].name = (stat[2].value == null) ? null : "N trajectories";

		ColumnDescriptor cds[] = new ColumnDescriptor[5];
		cds[0] = findColumnDescriptor(td, "X_");
		cds[1] = findColumnDescriptor(td, "Y_");
		cds[2] = findColumnDescriptor(td, "DISTANCE_");
		cds[3] = findColumnDescriptor(td, "DT_");
		cds[4] = findColumnDescriptor(td, "DIFFTIME_");
		reader.getStatsForColumnDescriptors(td, cds);

		for (int i = 0; i < cds.length; i++) {
			int i3 = i + 3;
			stat[i3] = new TableStatisticsItem();
			switch (i) {
			case 0:
				stat[i3].name = "X (longitude)";
				break;
			case 1:
				stat[i3].name = "Y (latitude)";
				break;
			case 2:
				stat[i3].name = "Distance to next";
				break;
			case 3:
				stat[i3].name = "Date/Time";
				break;
			case 4:
				stat[i3].name = "Time to next";
				break;
			}
			getColumnStat(td, cds[i], stat[i3]);
		}
		stat[8] = new TableStatisticsItem();
		stat[9] = new TableStatisticsItem();
		float minDeltaT = (stat[7].value == null) ? Float.NaN : Float.valueOf(stat[7].value).floatValue(), maxDeltaT = (stat[7].maxValue == null) ? Float.NaN : Float.valueOf(stat[7].maxValue).floatValue();
		if (minDeltaT < 1) {
			stat[8].name = "Time to next (hours)";
			stat[8].value = String.valueOf(24 * minDeltaT);
			stat[8].maxValue = String.valueOf(24 * maxDeltaT);
		}
		if (minDeltaT < 1f / 24) {
			stat[9].name = "Time to next (mins)";
			stat[9].value = String.valueOf(24 * 60 * minDeltaT);
			stat[9].maxValue = String.valueOf(24 * 60 * maxDeltaT);
		}
		showMessage("The value statistics successfully obtained!", false);
		//Getting the separation criterion
		TableStatisticsDisplay statD = new TableStatisticsDisplay(stat);
		String dateCName = tblDescr.timeColName, idCName = tblDescr.idColName, diffTimeCName = null, distanceCName = null, nextDateCName = null;
		for (int i = 0; i < tblDescr.relevantColumns.length; i++)
			if (tblDescr.relevantColumns[i] != null) {
				if (DataSemantics.movementSemantics[i].equalsIgnoreCase("time interval to next measurement")) {
					diffTimeCName = tblDescr.relevantColumns[i];
				} else if (DataSemantics.movementSemantics[i].equalsIgnoreCase("distance to next measurement")) {
					distanceCName = tblDescr.relevantColumns[i];
				} else if (DataSemantics.movementSemantics[i].equalsIgnoreCase("time of next measurement")) {
					nextDateCName = tblDescr.relevantColumns[i];
				}
			}
		SeparationCriteriaUI critUI = new SeparationCriteriaUI(reader, dateCName, idCName, diffTimeCName, nextDateCName, distanceCName, trIdColumn);
		Panel p = new Panel();
		SplitLayout spl = new SplitLayout(p, SplitLayout.VERT);
		p.setLayout(spl);
		Panel pp = new Panel(new BorderLayout());
		pp.add(statD, BorderLayout.NORTH);
		bExplore = new Button("Explore Distance/Time to Next");
		bExplore.addActionListener(this);
		pp.add(bExplore, BorderLayout.SOUTH);
		spl.addComponent(pp, 0.6f);
		spl.addComponent(critUI, 0.4f);
		OKDialog dia = new OKDialog(mainFrame, "Separation criteria", true);
		dia.addContent(p);
		critUI.setStatusLine(dia.getStatusLine());
		dia.show();
		if (dia.wasCancelled()) {
			//reader.closeConnection();
			showMessage("The separation criterion has not been chosen!", true);
			return null;
		}
		if (critUI.timeGapChosen()) {
			timeGap = critUI.getTimeGap();
			timeGapUnit = critUI.getTimeGapUnit();
			tblNameSuffix = "" + timeGapUnit + timeGap;
		} else if (critUI.spaceGapChosen()) {
			spaceGap = critUI.getSpaceGap();
			tblNameSuffix = "geo" + spaceGap;
		} else if (critUI.timeIntervalsChosen()) {
			tblNameSuffix = "intervals";
		} else {
			//reader.closeConnection();
			showMessage("The separation criterion has not been chosen!", true);
			return null;
		}
		showMessage("Performing database operations...", false);
		long t0 = System.currentTimeMillis();
		if (tablePrefix == null) {
			tablePrefix = td.tableName;
		}
		reader.dropTmpTable(tablePrefix + "_t1");
		boolean ok = reader.getTrajectories_Step01(tablePrefix + "_t1", td.tableName, idCName, dateCName, nextDateCName, trIdColumn, critUI.getCondition(), critUI.getConditionAttr()) > 0;
		long t1 = System.currentTimeMillis() - t0;
		System.out.println("* Elapsed time: " + StringUtil.floatToStr(t1 / 1000f, 3) + " (s)");
		showMessage("Trajectory separation: step 1 " + ((ok) ? "fulfilled" : "failed") + "!", !ok);
		String extraAttrCNames = "", extraAttrCNames1 = "";
		Vector v = reader.getColumnsOfTable(null, td.tableName), v1 = (v == null) ? null : (Vector) v.clone();
		if (v != null) { // remove "standard" (predefined) column names
			v.remove("ID_");
			v.remove("TID_");
			v.remove("TNUM_");
			v.remove("DT_");
			v.remove("NEXTTIME_");
			v.remove("DIFFTIME_");
			v.remove("X_");
			v.remove("NEXTX_");
			v.remove("Y_");
			v.remove("NEXTY_");
			v.remove("DX_");
			v.remove("DY_");
			v.remove("DISTANCE_");
			v.remove("COURSE_C");
			v.remove("SPEED_C");
			v.remove("ACCELERATION_C");
			v.remove("TURN_C");
			v.remove("DISTANCE_NEXT_");
			v.remove("DIFFTIME_NEXT_");
			v1.remove("TID_");
			v1.remove("TNUM_");
			v1.remove("DISTANCE_NEXT_");
			v1.remove("DIFFTIME_NEXT_");
		}
		if (v != null) {
			for (int i = 0; i < v.size(); i++) {
				extraAttrCNames += ", " + ((String) v.elementAt(i));
			}
		}
		if (v1 != null && trIdColumn != null) {
			for (int i = 0; i < v1.size(); i++) {
				extraAttrCNames1 += ((i == 0) ? "" : ", ") + ((String) v1.elementAt(i));
			}
		} else {
			extraAttrCNames1 = null;
		}
		if (ok) {
			t1 = System.currentTimeMillis();
			reader.dropTmpTable(tablePrefix + "_t2");
			showMessage("Performing step 2 of trajectory separation...", false);
			ok = reader.getTrajectories_Step02(tablePrefix, tablePrefix + "_t1", tablePrefix + "_t2", extraAttrCNames, extraAttrCNames1) > 0;
			showMessage("Trajectory separation: step 2 " + ((ok) ? "fulfilled" : "failed") + "!", !ok);
			t1 = System.currentTimeMillis() - t1;
			System.out.println("* Elapsed time: " + StringUtil.floatToStr(t1 / 1000f, 3) + " (s)");
		}
		String dbTableName = tablePrefix + tblNameSuffix, tblNiceName = null;
		if (ok) {
			t1 = System.currentTimeMillis();
			p = new Panel(new GridLayout(2, 1));
			pp = new Panel(new BorderLayout());
			pp.add(new Label("Name of the resulting table in the database:"), BorderLayout.WEST);
			TextField dbTableNameTF = new TextField(dbTableName, 30);
			pp.add(dbTableNameTF, BorderLayout.CENTER);
			p.add(pp);
			pp = new Panel(new BorderLayout());
			pp.add(new Label("Name of the resulting table in the UI:"), BorderLayout.WEST);
			TextField tblNiceNameTF = new TextField("Trajectories from " + ((tblDescr.name == null) ? tblDescr.dbTableName : tblDescr.name) + tblNameSuffix, 50);
			pp.add(tblNiceNameTF, BorderLayout.CENTER);
			p.add(pp);
			dia = new OKDialog(mainFrame, "Resulting table name", false);
			dia.addContent(p);
			boolean goodName = false;
			while (!goodName) {
				dia.show();
				String str = dbTableNameTF.getText();
				if (str != null) {
					str = str.trim();
					if (str.length() > 0) {
						dbTableName = StringUtil.SQLid(str, 30);
						goodName = dbTableName.equals(str);
					}
				}
				if (!goodName) {
					dbTableNameTF.setText(dbTableName);
				}
			}
			showMessage("Performing step 3 of trajectory separation...", false);
			reader.dropTmpTable(dbTableName);
			ok = reader.getTrajectories_Step03(tablePrefix + "_t2", dbTableName, extraAttrCNames) > 0;
			showMessage("Trajectory separation: step 3 " + ((ok) ? "fulfilled" : "failed") + "!", !ok);
			if (ok) {
				reader.dropTmpTable(tablePrefix + "_t1");
				reader.dropTmpTable(tablePrefix + "_t2");
				tblNiceName = tblNiceNameTF.getText();
				if (tblNiceName != null) {
					tblNiceName = tblNiceName.trim();
					if (tblNiceName.length() < 1) {
						tblNiceName = null;
					}
				}
			}
			t1 = System.currentTimeMillis() - t1;
			System.out.println("* Elapsed time: " + StringUtil.floatToStr(t1 / 1000f, 3) + " (s)");
		}
		//reader.closeConnection();
		t1 = System.currentTimeMillis() - t0;
		if (ok) {
			System.out.println("Trajectories successfully separated! Execute time: " + StringUtil.floatToStr(t1 / 1000f, 3) + " (s)");
		} else {
			System.out.println("Trajectory separation failed! Execute time: " + StringUtil.floatToStr(t1 / 1000f, 3) + " (s)");
		}
		clearStatusLine();
		if (!ok)
			return null;
		DBTableDescriptor resTDescr = new DBTableDescriptor();
		tblDescr.copyTo(resTDescr);
		resTDescr.dbTableName = dbTableName;
		if (tblNiceName != null) {
			resTDescr.name = tblNiceName;
		} else {
			resTDescr.name = dbTableName;
		}
		resTDescr.relevantColumns[DataSemantics.idxOfTrajId] = DataSemantics.canonicFieldNamesMovement[DataSemantics.idxOfTrajId];
		return resTDescr;
	}

	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource().equals(bExplore)) {
			int tblIdx = reader.getNumOfTableDescriptors() - 1;
			TableDescriptor td = reader.getTableDescriptor(tblIdx);
			float maxD = 0f, minT = 0f, maxT = 0f;
			String colname = "DISTANCE_";
			ColumnDescriptor cd = findColumnDescriptor(td, colname);
			if (cd != null) {
				maxD = ((ColumnDescriptorNum) cd).max;
			}
			colname = "DIFFTIME_";
			cd = findColumnDescriptor(td, colname);
			if (cd != null) {
				minT = ((ColumnDescriptorNum) cd).min;
				maxT = ((ColumnDescriptorNum) cd).max;
			}
			String texts[] = { "Distance (km)", "Time (days)", "Time (hours)", "Time (minutes)" };
			CheckboxGroup cbg = new CheckboxGroup();
			Checkbox cb[] = new Checkbox[2 + ((minT < 1) ? 2 : 0)];
			cb[0] = new Checkbox(texts[0], true, cbg);
			cb[1] = new Checkbox(texts[1], false, cbg);
			if (minT < 1) {
				cb[2] = new Checkbox(texts[2], false, cbg);
				cb[3] = new Checkbox(texts[3], false, cbg);
			}
			Panel p = new Panel(new ColumnLayout());
			p.add(new Label("Find N of records"));
			p.add(new Label("where"));
			for (Checkbox element : cb) {
				p.add(element);
			}
			Panel pp = new Panel(new BorderLayout());
			pp.add(new Label(">="), BorderLayout.WEST);
			TextField tf = new TextField("0", 10);
			pp.add(tf, BorderLayout.CENTER);
			p.add(pp);
			p.add(new Line(false));
			OKDialog okd = new OKDialog(mainFrame, "Explore freshold values", true);
			okd.addContent(p);
			while (true) {
				okd.show();
				if (okd.wasCancelled())
					return;
				String sval = tf.getText();
				float fval = Float.valueOf(sval).floatValue();
				if (Float.isNaN(fval) || fval <= 0) {
					tf.setText("0");
				} else {
					try {
						int iopt = -1;
						for (int i = 0; iopt == -1 && i < cb.length; i++)
							if (cb[i].getState()) {
								iopt = i;
							}
						long tStart = System.currentTimeMillis();
						Statement statement = reader.getConnection().createStatement();
						String sqlString = "select count(*)\nfrom " + reader.getTableDescriptor(0).tableName + "\nwhere ";
						switch (iopt) {
						case 0:
							sqlString += "DISTANCE_ >= ";
							break;
						case 1:
							sqlString += "DIFFTIME_ >= ";
							break;
						case 2:
							sqlString += "DIFFTIME_*24 >= ";
							break;
						case 3:
							sqlString += "DIFFTIME_*24*60 >= ";
							break;
						}
						sqlString += fval;
						System.out.println("** <" + sqlString + ">");
						ResultSet result = statement.executeQuery(sqlString);
						result.next();
						long n = result.getLong(1);
						result.close();
						statement.close();
						long tEnd = System.currentTimeMillis() - tStart;
						System.out.println("* Ready. Elapsed time " + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s)");
						Panel pr = new Panel(new ColumnLayout());
						pr.add(new Label("N of records"));
						pr.add(new Label("where"));
						pr.add(new Label(cb[iopt].getLabel() + " >= " + tf.getText()));
						pr.add(new Label("is " + n));
						OKDialog okdr = new OKDialog(mainFrame, "Exploration result", true, true);
						okdr.addContent(pr);
						okdr.show();
						if (!okdr.wasBackPressed())
							return;
					} catch (SQLException se) {
						System.out.println("Error: " + se.toString());
					}
				}
			}
		}
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
}
