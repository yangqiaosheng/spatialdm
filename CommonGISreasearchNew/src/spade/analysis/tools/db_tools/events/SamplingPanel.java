package spade.analysis.tools.db_tools.events;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import spade.analysis.tools.db_tools.DBTableDescriptor;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.FocusListener;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.StringUtil;
import spade.time.Date;
import spade.time.FocusInterval;
import spade.time.TimeCount;
import spade.time.TimeMoment;
import spade.time.ui.TimeDialogs;
import spade.time.ui.TimeSlider;
import spade.time.ui.TimeSliderPanel;
import spade.vis.geometry.RealRectangle;
import db_work.database.OracleConnector;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Jun 3, 2009
 * Time: 4:42:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class SamplingPanel extends Panel implements ActionListener, ItemListener, FocusListener, PropertyChangeListener {

	protected Frame frParent = null;
	DBTableDescriptor dbtd = null;
	protected RealRectangle brr = null; // bounding rectangle for filtering

	protected OracleConnector currTblConnector = null;
	protected String tableName = null;
	protected Checkbox cbSampling = null, cbQuery = null, cbDynamicQuery = null;

	protected List lExtraAttrs = null;

	protected TextField tfSample = null, tfSeed = null;
	protected Button bEstimateSample = null, bEstimateQuery = null;
	protected Label lEstimatesSample = null, lEstimatesQuery = null;
	protected TextField tfExtraSQL = null;
	protected TextArea taQuery = null;
	protected Button bSaveView = null;

	//protected Focuser f[]=null;
	protected TextField tfMin[] = null, tfMax[] = null;
	protected FocusInterval focusInterval = null;
	protected Button bFullExtentS = null;
	protected FocusInterval fiTime[] = null;
	protected Button bFullExtent[] = null;
	protected Date d000000 = null, d235959 = null;

	protected Checkbox cbMonths[][] = null, cbDOWs[][] = null;
	protected Button bMonthsAll = null, bMonthsNone = null, bDOWsAll = null, bDOWsNone = null;
	protected Checkbox cbInside = null;

	protected TimeMoment mintm = null, maxtm = null;
	public boolean physicalTime = false;
	public char datePrecision = 's';
	public String dateScheme = "";

	public boolean getUseSampling() {
		return cbSampling != null && cbSampling.getState();
	}

	public float getSampleSize() {
		float f = Float.NaN;
		if (getUseSampling()) {
			try {
				f = Float.valueOf(tfSample.getText()).floatValue();
			} catch (NumberFormatException nfe) {
			}
		}
		return (f >= 0.000001 && f < 100f) ? f : Float.NaN;
	}

	public float getSeed() {
		float f = Float.NaN;
		if (getUseSampling()) {
			try {
				f = Float.valueOf(tfSeed.getText()).floatValue();
			} catch (NumberFormatException nfe) {
			}
		}
		return (f >= 0f) ? f : Float.NaN;
	}

	public boolean getUseQuery() {
		return cbQuery != null && cbQuery.getState();
	}

	public String getQueryString() {
		return (taQuery == null) ? "" : taQuery.getText();
	}

	public SamplingPanel(OracleConnector currTblConnector, DBTableDescriptor dbtd, Frame frame, RealRectangle brr, List lExtraAttrs) {
		super();
		this.frParent = frame;
		this.currTblConnector = currTblConnector;
		this.dbtd = dbtd;
		this.brr = brr;
		this.lExtraAttrs = lExtraAttrs;
		long nrec = -1;
		String sql = "";
		tableName = currTblConnector.getTableDescriptor(0).tableName;
		// counts events
		long timeForEstimate = System.currentTimeMillis();
		try {
			Statement statement = currTblConnector.getConnection().createStatement();
			sql = "select count(*),min(" + dbtd.timeColName + "),max(" + dbtd.timeColName + ") from " + tableName;
			ResultSet result = statement.executeQuery(sql);
			result.next();
			nrec = result.getLong(1);
			statement.close();
		} catch (SQLException se) {
			System.out.println("SQL error:\n" + sql + "\n" + se.toString());
		}
		timeForEstimate = System.currentTimeMillis() - timeForEstimate;
		// create UI
		setLayout(new BorderLayout());
		Panel mp = new Panel(new ColumnLayout());
		add(mp, BorderLayout.WEST);
		mp.add(new Label(tableName + ": " + nrec + " events"));
		mp.add(new Line(false));
		// query
		Panel p = new Panel(new BorderLayout());
		mp.add(p);
		p.add(cbQuery = new Checkbox("Use query", timeForEstimate < 1000), BorderLayout.WEST);
		p.add(cbDynamicQuery = new Checkbox("Dynamic query (may be slow)", timeForEstimate < 1000), BorderLayout.EAST);
		cbQuery.addItemListener(this);
		p = new Panel(new BorderLayout());
		mp.add(p);
		p.add(new Label("Extra SQL code:"), BorderLayout.WEST);
		p.add(tfExtraSQL = new TextField(30), BorderLayout.CENTER);
		tfExtraSQL.addActionListener(this);
		mp.add(taQuery = new TextArea("", 5, 30, TextArea.SCROLLBARS_VERTICAL_ONLY));
		taQuery.setEditable(false);
		bEstimateQuery = new Button("Estimate N of points");
		bEstimateQuery.addActionListener(this);
		bEstimateQuery.setEnabled(getUseQuery());
		mp.add(bEstimateQuery);
		lEstimatesQuery = new Label("...no estimation yet");
		mp.add(lEstimatesQuery);
		mp.add(new Line(false));
		// sampling
		mp.add(cbSampling = new Checkbox("Use sampling", false));
		cbSampling.addItemListener(this);
		p = new Panel(new FlowLayout());
		mp.add(p);
		p.add(new Label("Sample size (0.000001 .. 99.999): "));
		p.add(tfSample = new TextField("5.0", 10));
		tfSample.setEnabled(getUseSampling());
		p = new Panel(new FlowLayout());
		mp.add(p);
		p.add(new Label("Sample seed (>=0): "));
		p.add(tfSeed = new TextField("0", 10));
		tfSeed.setEnabled(getUseSampling());
		bEstimateSample = new Button("Estimate N of points");
		bEstimateSample.addActionListener(this);
		bEstimateSample.setEnabled(getUseSampling());
		mp.add(bEstimateSample);
		lEstimatesSample = new Label("...no estimation yet");
		mp.add(lEstimatesSample);
		mp.add(new Line(false));
		bSaveView = new Button("Save Query/Sample as View");
		bSaveView.addActionListener(this);
		mp.add(bSaveView);
		mp.add(new Line(false));
		add(new Line(true), BorderLayout.CENTER);
		add(createQueryPanel(), BorderLayout.EAST);
	}

	protected Panel createQueryPanel() {
		Panel pQuery = new Panel(new ColumnLayout());
		long duration = 0;
		try {
			Statement statement = currTblConnector.getConnection().createStatement();
			String sql = "select min(" + dbtd.timeColName + "),max(" + dbtd.timeColName + ")\n" + "from " + tableName;
			System.out.println("* <" + sql + ">");
			ResultSet result = statement.executeQuery(sql);
			ResultSetMetaData md = result.getMetaData();
			int timeColumnType = md.getColumnType(1);
			physicalTime = timeColumnType == java.sql.Types.DATE || timeColumnType == java.sql.Types.TIME || timeColumnType == java.sql.Types.TIMESTAMP;
			result.next();
			if (physicalTime) {
				String minmax[] = new String[2];
				minmax[0] = "Minimal date/time = " + result.getDate(1) + " " + result.getTime(1);
				minmax[1] = "Maximal date/time = " + result.getDate(2) + " " + result.getTime(2);
				if (physicalTime) {
					datePrecision = TimeDialogs.askDesiredDatePrecision(minmax);
				}
				dateScheme = TimeDialogs.getSuitableDateScheme(datePrecision);
				spade.time.Date dt = new spade.time.Date();
				dt.setPrecision(datePrecision);
				dt.setDateScheme(dateScheme);
				java.util.Date date = result.getDate(1);
				java.sql.Time time = result.getTime(1);
				dt.setDate(date, time);
				mintm = dt;
				dt = new spade.time.Date();
				dt.setPrecision(datePrecision);
				dt.setDateScheme(dateScheme);
				date = result.getDate(2);
				time = result.getTime(2);
				dt.setDate(date, time);
				maxtm = dt;
			} else {
				mintm = new TimeCount(result.getLong(1));
				maxtm = new TimeCount(result.getLong(2));
			}
			statement.close();
		} catch (SQLException se) {
			System.out.println("SQL error:" + se.toString());
		}
		// create UI
		focusInterval = new FocusInterval();
		focusInterval.setDataInterval(mintm, maxtm);
		focusInterval.addPropertyChangeListener(this);
		TimeSlider timeSlider = new TimeSlider(focusInterval);
		TimeSliderPanel tspan = new TimeSliderPanel(timeSlider, this, true);
		pQuery.add(tspan);
		bFullExtentS = new Button("Full extent");
		bFullExtentS.addActionListener(this);
		bFullExtentS.setEnabled(false);
		pQuery.add(bFullExtentS);

		if (physicalTime) {
			cbMonths = new Checkbox[2][];
			cbDOWs = new Checkbox[2][]; //cbYears=new Checkbox[2][];
			pQuery.add(new Line(false));
			if (/*duration>30*/true) { // months
				Panel p = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
				p.add(new Label("months:"));
				p.add(bMonthsAll = new Button("all"));
				bMonthsAll.addActionListener(this);
				p.add(bMonthsNone = new Button("none"));
				bMonthsNone.addActionListener(this);
				cbMonths[0] = new Checkbox[12];
				for (int i = 0; i < cbMonths[0].length; i++) {
					cbMonths[0][i] = new Checkbox(String.valueOf(1 + i), true);
					cbMonths[0][i].addItemListener(this);
					p.add(cbMonths[0][i]);
				}
				pQuery.add(p);
				pQuery.add(new Line(false));
			}
			if (/*duration>1*/true) { // DOWs
				Panel p = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
				p.add(new Label("days:"));
				p.add(bDOWsAll = new Button("all"));
				bDOWsAll.addActionListener(this);
				p.add(bDOWsNone = new Button("none"));
				bDOWsNone.addActionListener(this);
				String dows[] = { "mon", "tue", "wed", "thu", "fri", "sat", "sun" };
				cbDOWs[0] = new Checkbox[dows.length];
				for (int i = 0; i < cbDOWs[0].length; i++) {
					cbDOWs[0][i] = new Checkbox("" + dows[i], true);
					cbDOWs[0][i].addItemListener(this);
					p.add(cbDOWs[0][i]);
				}
				pQuery.add(p);
				pQuery.add(new Line(false));
			}
			if (((Date) mintm).useElement('h') || ((Date) mintm).useElement('t') || ((Date) mintm).useElement('s')) {
				d000000 = new spade.time.Date();
				d000000.setPrecision('s');
				d000000.setDateScheme("hhttss");
				d000000.setHour(0);
				d000000.setMinute(0);
				d000000.setSecond(0);
				d235959 = new spade.time.Date();
				d235959.setPrecision('s');
				d235959.setDateScheme("hhttss");
				d235959.setHour(23);
				d235959.setMinute(59);
				d235959.setSecond(59);
				fiTime = new FocusInterval[4];
				bFullExtent = new Button[4];
				for (int n = 0; n <= 1; n++) {
					fiTime[n] = new FocusInterval();
					fiTime[n].setDataInterval(d000000, d235959);
					if (n == 1) {
						fiTime[n].setWhatIsFixed(FocusInterval.END);
					}
					fiTime[n].addPropertyChangeListener(this);
					timeSlider = new TimeSlider(fiTime[n]);
					tspan = new TimeSliderPanel(timeSlider, this, true);
					pQuery.add(tspan);
					bFullExtent[n] = new Button("Full extent");
					bFullExtent[n].addActionListener(this);
					bFullExtent[n].setEnabled(false);
					pQuery.add(bFullExtent[n]);
					pQuery.add(new Line(false));
				}
			}
		}
		if (brr != null) {
			cbInside = new Checkbox("inside [" + brr.rx1 + "," + brr.ry1 + "," + brr.rx2 + "," + brr.ry2 + "]", false);
			cbInside.addItemListener(this);
			pQuery.add(cbInside);
			pQuery.add(new Line(false));
		}
		return pQuery;
	}

	/*
	private Panel createFocuserPanel (int idx, String lText, float fMin, float fMax) {
	  Panel pf=new Panel(new BorderLayout());
	  f[idx]=new Focuser();
	  f[idx].setIsVertical(false);
	  f[idx].setAbsMinMax(fMin,fMax);
	  f[idx].setIsUsedForQuery(true);
	  f[idx].addFocusListener(this);
	  tfMin[idx]=(Float.isNaN(fMin))?null:new TextField(""+fMin,6);
	  tfMax[idx]=(Float.isNaN(fMax))?null:new TextField(""+fMax,6);
	  f[idx].setTextFields(tfMin[idx],tfMax[idx]);
	  pf.add(new Label(lText),BorderLayout.NORTH);
	  if (tfMin[idx]!=null)
	    pf.add(tfMin[idx],BorderLayout.WEST);
	  pf.add(new FocuserCanvas(f[idx],false),BorderLayout.CENTER);
	  if (tfMax[idx]!=null)
	    pf.add(tfMax[idx],BorderLayout.EAST);
	  return pf;
	}
	*/

	public void itemStateChanged(ItemEvent ie) {
		if (ie.getSource().equals(cbQuery)) {
			bEstimateQuery.setEnabled(getUseQuery());
		}
		if (ie.getSource().equals(cbSampling)) {
			tfSample.setEnabled(getUseSampling());
			tfSeed.setEnabled(getUseSampling());
			bEstimateSample.setEnabled(getUseSampling());
		}
		if (cbMonths != null) {
			for (Checkbox[] cbMonth : cbMonths)
				if (cbMonth != null) {
					for (int j = 0; j < cbMonth.length; j++)
						if (ie.getSource().equals(cbMonth[j])) {
							generateQuerySQL();
						}
				}
		}
		if (cbDOWs != null) {
			for (Checkbox[] cbDOW : cbDOWs)
				if (cbDOW != null) {
					for (int j = 0; j < cbDOW.length; j++)
						if (ie.getSource().equals(cbDOW[j])) {
							generateQuerySQL();
						}
				}
		}
		if (ie.getSource().equals(cbInside)) {
			generateQuerySQL();
		}
	}

	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource().equals(tfExtraSQL)) {
			generateQuerySQL();
		}
		if (ae.getSource().equals(bEstimateQuery)) {
			estimateSample(false, true);
		}
		if (ae.getSource().equals(bEstimateSample)) {
			estimateSample(true, true);
		}
		if (ae.getSource().equals(bMonthsAll) && cbMonths[0] != null) {
			for (int i = 0; i < cbMonths[0].length; i++) {
				cbMonths[0][i].setState(true);
			}
			generateQuerySQL();
		}
		if (ae.getSource().equals(bMonthsNone) && cbMonths[0] != null) {
			for (int i = 0; i < cbMonths[0].length; i++) {
				cbMonths[0][i].setState(false);
			}
			generateQuerySQL();
		}
		if (ae.getSource().equals(bDOWsAll) && cbDOWs[0] != null) {
			for (int i = 0; i < cbDOWs[0].length; i++) {
				cbDOWs[0][i].setState(true);
			}
			generateQuerySQL();
		}
		if (ae.getSource().equals(bDOWsNone) && cbDOWs[0] != null) {
			for (int i = 0; i < cbDOWs[0].length; i++) {
				cbDOWs[0][i].setState(false);
			}
			generateQuerySQL();
		}
		if (ae.getSource().equals(bSaveView)) {
			createView();
		}
		if (ae.getSource().equals(bFullExtentS)) {
			focusInterval.showWholeInterval();
		}
		if (fiTime != null) {
			for (int n = 0; n < fiTime.length; n++)
				if (ae.getSource().equals(bFullExtent[n])) {
					fiTime[n].showWholeInterval();
				}
		}
	}

	protected void createView() {
		if (taQuery.getText().length() == 0) {
			System.out.println("* no query defined yet, nothing to store yet");
			return;
		}
		String sql = "", viewName = "";
		Panel p = new Panel(new BorderLayout());
		p.add(new Label("Name for the view:"), BorderLayout.NORTH);
		p.add(new Label(tableName + "_"), BorderLayout.WEST);
		TextField tf = new TextField("", 10);
		p.add(tf, BorderLayout.CENTER);
		OKDialog dlg = new OKDialog(frParent, "Set name for the view to be created", true);
		dlg.addContent(p);
		dlg.show();
		if (dlg.wasCancelled() || tf.getText().trim().length() == 0)
			return;
		viewName = "_" + tf.getText().trim();
		try {
			Statement statement = currTblConnector.getConnection().createStatement();
			float fSample = getSampleSize(), fSeed = getSeed();
			String extraAttrs = "";
			for (int i = 0; i < lExtraAttrs.getItemCount(); i++)
				if (lExtraAttrs.isIndexSelected(i)) {
					if (extraAttrs.length() > 0) {
						extraAttrs += ",";
					}
					extraAttrs += lExtraAttrs.getItem(i);
				}
			sql = "create or replace view " + tableName + viewName + " as\n" + "SELECT \n" + ((dbtd.idColIdx >= 0) ? dbtd.idColName + "," : "") + dbtd.xColName + "," + dbtd.yColName + "," + dbtd.timeColName
					+ ((extraAttrs.length() > 0) ? "," + extraAttrs : "") + "\n" + "FROM " + tableName + "\n" + ((getUseSampling()) ? "SAMPLE (" + fSample + ")" + ((Float.isNaN(fSeed) ? "" : " SEED (" + fSeed + ")")) + "\n" : "")
					+ ((getUseQuery() && getQueryString().length() > 0) ? "WHERE " + getQueryString() + "\n" : "");
			System.out.println("* <" + sql + ">");
			statement.executeUpdate(sql);
			statement.close();
		} catch (SQLException se) {
			System.out.println("SQL error:\n" + sql + "\n" + se.toString());
		}
	}

	protected void estimateSample(boolean bSample, boolean loggingInConsole) {
		float fSample = getSampleSize(), fSeed = getSeed();
		String sql = "";
		try {
			if (bSample) {
				lEstimatesSample.setEnabled(false);
				bEstimateSample.setEnabled(false);
			} else {
				lEstimatesQuery.setEnabled(false);
				bEstimateQuery.setEnabled(false);
			}
			Statement statement = currTblConnector.getConnection().createStatement();
			sql = "SELECT COUNT(*)\n" + "FROM " + tableName + "\n" + ((bSample) ? "SAMPLE (" + fSample + ")" + ((Float.isNaN(fSeed) ? "" : " SEED (" + fSeed + ")")) + "\n" : "")
					+ ((getUseQuery() && getQueryString().length() > 0) ? "WHERE " + getQueryString() + "\n" : "");
			if (loggingInConsole) {
				System.out.println("* <" + sql + ">");
			}
			ResultSet result = statement.executeQuery(sql);
			result.next();
			int nrec = result.getInt(1);
			statement.close();
			if (bSample) {
				lEstimatesSample.setText("Estimation: " + nrec + " points");
				lEstimatesSample.setEnabled(true);
				bEstimateSample.setEnabled(true);
			} else {
				lEstimatesQuery.setText("Estimation: " + nrec + " points");
				lEstimatesQuery.setEnabled(true);
				bEstimateQuery.setEnabled(true);
			}
		} catch (SQLException se) {
			System.out.println("SQL error:\n" + sql + "\n" + se.toString());
		}
	}

	public void focusChanged(Object source, double lowerLimit, double upperLimit) {
		generateQuerySQL();
	}

	public void limitIsMoving(Object source, int n, double currValue) {
		generateQuerySQL();
	}

	protected void generateQuerySQL() {
		String sql = "";
		sql += generateQuerySQL_fitem(sql, dbtd.timeColName);
		if (physicalTime) {
			String str = generateSQLlist(cbMonths[0]);
			if (str != null) {
				String extra = "(TO_CHAR(" + dbtd.timeColName + ",'MM') in (" + str + "))";
				if (sql.length() > 0) {
					extra = " and " + extra;
				}
				sql += extra;
			}
			str = generateSQLlist(cbDOWs[0]);
			if (str != null) {
				String extra = "(TO_CHAR(" + dbtd.timeColName + ",'dy') in (" + str + "))";
				if (sql.length() > 0) {
					extra = " and " + extra;
				}
				sql += extra;
			}
			if (fiTime != null) {
				String extra = "";
				for (int k = 0; k < fiTime.length; k++) {
					if (fiTime[k] != null && bFullExtent[k].isEnabled()) {
						if (k == 1 && bFullExtent[0].isEnabled()) {
							extra += " or ";
						}
						if (!fiTime[k].getCurrIntervalStart().toString().equals("000000")) {
							extra += "to_char(" + dbtd.timeColName + ",'sssss')>=" + "to_char(to_date('01012009" + fiTime[k].getCurrIntervalStart().toString() + "','ddmmyyyyhh24miss'),'sssss')";
						}
						if (!fiTime[k].getCurrIntervalEnd().toString().equals("235959") && !fiTime[k].getCurrIntervalEnd().toString().equals("240000")) {
							if (!fiTime[k].getCurrIntervalStart().toString().equals("000000")) {
								extra += " and ";
							}
							extra += "to_char(" + dbtd.timeColName + ",'sssss')<=" + "to_char(to_date('01012009" + fiTime[k].getCurrIntervalEnd().toString() + "','ddmmyyyyhh24miss'),'sssss')";
						}
					}
				}
				if (extra.length() > 0) {
					extra = "(" + extra + ")";
					if (sql.length() > 0) {
						extra = " and " + extra;
					}
					sql += extra;
				}
			}
		}
		if (cbInside != null && cbInside.getState()) {
			String extra = "(" + dbtd.xColName + ">=" + brr.rx1 + " and " + dbtd.yColName + ">=" + brr.ry1 + " and " + dbtd.xColName + "<=" + brr.rx2 + " and " + dbtd.yColName + "<=" + brr.ry2 + ")";
			if (sql.length() > 0) {
				extra = " and " + extra;
			}
			sql += extra;
		}
		String extra = tfExtraSQL.getText().trim();
		if (extra.length() > 0) {
			if (sql.length() > 0) {
				extra = "and (" + extra + ")";
			}
			sql += extra;
		}
		taQuery.setText(sql);
		if (cbDynamicQuery.getState()) {
			estimateSample(false, false);
		}
	}

	private String generateSQLlist(Checkbox cb[]) {
		String str = null;
		boolean all = true, none = true;
		if (cb != null) {
			for (Checkbox element : cb)
				if (element.getState()) {
					if (str == null) {
						str = "'" + StringUtil.padString(element.getLabel(), '0', 2, true) + "'";
					} else {
						str += ",'" + StringUtil.padString(element.getLabel(), '0', 2, true) + "'";
					}
					none = false;
				} else {
					all = false;
				}
		}
		if (all || none) {
			str = null;
		}
		return str;
	}

	private String generateQuerySQL_fitem(String sql, String attr) {
		String extra = "";
		FocusInterval fi = focusInterval;
		if (fi == null || fi.getCurrIntervalStart() == null || fi.getCurrIntervalEnd() == null)
			return extra;
		if (fi.getCurrIntervalStart().toNumber() > fi.getDataIntervalStart().toNumber()) {
			if (sql.length() > 0) {
				extra += " and ";
			}
			extra += " (" + attr + " >= " + generateOracleDT(fi.getCurrIntervalStart()) + ")";
		}
		if (fi.getCurrIntervalEnd().toNumber() < fi.getDataIntervalEnd().toNumber()) {
			if (sql.length() > 0 || extra.length() > 0) {
				extra += " and ";
			}
			extra += " (" + attr + " <= " + generateOracleDT(fi.getCurrIntervalEnd()) + ")";
		}
		return extra;
	}

	private String generateOracleDT(TimeMoment tm) {
		if (physicalTime) {
			Date dt = (Date) tm;
			String str = "TO_DATE('" + StringUtil.padString("" + dt.day, '0', 2, true) + StringUtil.padString("" + dt.month, '0', 2, true) + StringUtil.padString("" + dt.year, '0', 4, true) + " "
					+ StringUtil.padString("" + ((dt.hour == -1) ? "" : dt.hour), '0', 2, true) + StringUtil.padString("" + ((dt.min == -1) ? "" : dt.min), '0', 2, true) + StringUtil.padString("" + ((dt.sec == -1) ? "" : dt.sec), '0', 2, true)
					+ "','ddmmyyyy hh24miss')";
			return str;
		} else
			return "" + tm.toNumber();
	}

	/**
	 * Listens to changes of the time focuser
	 */
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource() == focusInterval) {
			if (e.getPropertyName().equalsIgnoreCase("current_interval")) {
				TimeMoment t1 = (TimeMoment) e.getOldValue(), t2 = (TimeMoment) e.getNewValue();
				if (t2 != null && t2.subtract(t1) < 1) {
					t1 = t1.getCopy();
					t2 = t1.getCopy();
					t2.add(1);
					focusInterval.setCurrInterval(t1, t2);
				}
				if (bFullExtentS != null) {
					bFullExtentS.setEnabled(mintm.compareTo(t1) < 0 || maxtm.compareTo(t2) > 0);
				}
			}
			generateQuerySQL();
		}
		if (fiTime != null) {
			for (int n = 0; n < fiTime.length; n++)
				if (e.getSource() == fiTime[n]) {
					if (e.getPropertyName().equalsIgnoreCase("current_interval")) {
						TimeMoment t1 = (TimeMoment) e.getOldValue(), t2 = (TimeMoment) e.getNewValue();
						if (t2 != null && t2.subtract(t1) < 1) {
							t1 = t1.getCopy();
							t2 = t1.getCopy();
							t2.add(1);
							fiTime[n].setCurrInterval(t1, t2);
						}
						if (bFullExtent[n] != null) {
							bFullExtent[n].setEnabled(d000000.compareTo(t1) < 0 || d235959.compareTo(t2) > 0);
						}
					}
					generateQuerySQL();
				}
		}
	}

}
