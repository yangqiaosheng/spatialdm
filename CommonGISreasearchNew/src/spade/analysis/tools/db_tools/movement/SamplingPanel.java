package spade.analysis.tools.db_tools.movement;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Label;
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

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.FocusListener;
import spade.lib.basicwin.Focuser;
import spade.lib.basicwin.FocuserCanvas;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.TabbedPanel;
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
 * Date: Jan 23, 2008
 * Time: 3:12:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class SamplingPanel extends Panel implements ActionListener, ItemListener, FocusListener, PropertyChangeListener {

	protected Frame frParent = null;
	protected RealRectangle brr = null; // bounding rectangle for filtering

	protected OracleConnector currTblConnector = null;
	protected String tableName = null;
	protected Checkbox cbSampling = null, cbQuery = null, cbDynamicQuery = null;
	protected TextField tfSample = null, tfSeed = null;
	protected Button bEstimateSample = null, bEstimateQuery = null;
	protected Label lEstimatesSample = null, lEstimatesQuery = null;
	protected TextField tfExtraSQL = null;
	protected TextArea taQuery = null;
	protected Checkbox cbSamplingType[] = null;
	protected Button bSaveView = null;

	protected Focuser f[] = null;
	protected TextField tfMin[] = null, tfMax[] = null;
	protected FocusInterval focusIntervalS = null, focusIntervalE = null;
	protected Button bFullExtentS = null, bFullExtentE = null;
	protected FocusInterval fiTime[] = null;
	protected Button bFullExtent[] = null;
	protected Date d000000 = null, d235959 = null;

	protected Checkbox cbMonths[][] = null, cbDOWs[][] = null;
	protected Button bsMonthsAll = null, bsMonthsNone = null, beMonthsAll = null, beMonthsNone = null, bsDOWsAll = null, bsDOWsNone = null, beDOWsAll = null, beDOWsNone = null;
	protected Checkbox cbSbrr = null, cbEbrr = null, cbTrInside = null;

	public int coordsAreGeographic = -1; // -1=not set; 0=no; 1=yes
	public boolean physicalTime = false;
	public char datePrecision = 's';
	public String dateScheme = "";

	long minnpoints = 0, maxnpoints = 0;
	float maxduration = 0, mindistance = 0, maxdistance = 0, mindisplacement = 0, maxdisplacement = 0, minbr_diagonal = 0, maxbr_diagonal = 0;
	TimeMoment mintm = null, maxtm = null, mintms = null, maxtms = null, mintme = null, maxtme = null;

	public boolean getUseSampling() {
		return cbSampling != null && cbSampling.getState();
	}

	public boolean isSamplingByObjects() {
		return cbSamplingType != null && cbSamplingType[0].getState();
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

	public SamplingPanel(OracleConnector currTblConnector, Frame frame, int coordsAreGeographic, RealRectangle brr) {
		super();
		this.frParent = frame;
		this.currTblConnector = currTblConnector;
		this.coordsAreGeographic = coordsAreGeographic;
		this.brr = brr;
		long nrec = -1, ntr = -1, nid = -1;
		String sql = "";
		tableName = currTblConnector.getTableDescriptor(0).tableName;
		if (coordsAreGeographic == -1)
			if (tableName.indexOf("$$") >= 0) {
				coordsAreGeographic = 1;
			} else {
				// collect basic statistics
				boolean mayBeGeogr = false;
				try {
					Statement statement = currTblConnector.getConnection().createStatement();
					sql = ((currTblConnector.checkIfTableOrViewExists(tableName + "_se")) ? "select min(br_x1),min(br_y1),max(br_x2),max(br_y2) from " + tableName + "_se" : "select min(x_),min(y_),max(x_),max(y_) from " + tableName);
					System.out.println("* <" + sql + ">");
					ResultSet result = statement.executeQuery(sql);
					result.next();
					float x1 = result.getFloat(1), y1 = result.getFloat(2), x2 = result.getFloat(3), y2 = result.getFloat(4);
					mayBeGeogr = x1 >= -180 && y1 >= -90 && x2 <= 180 && y2 <= 90;
					statement.close();
				} catch (SQLException se) {
					System.out.println("SQL error:\n" + sql + "\n" + se.toString());
				}
				if (mayBeGeogr) {
					coordsAreGeographic = (Dialogs.askYesOrNo(frParent, "Are the coordinates in the data geographic (latitudes and longitudes)?", "Geographic coordinates?")) ? 1 : 0;
				} else {
					coordsAreGeographic = 0;
				}
			}
		// create index table, if needed
		currTblConnector.createSEtable(tableName, coordsAreGeographic);
		// counts entities, trajectories, points
		long timeForEstimate = System.currentTimeMillis();
		try {
			Statement statement = currTblConnector.getConnection().createStatement();
			sql = "select count(distinct tid_), count(distinct id_), sum(npoints) from " + tableName + "_se";
			ResultSet result = statement.executeQuery(sql);
			result.next();
			ntr = result.getLong(1);
			nid = result.getLong(2);
			nrec = result.getLong(3);
			statement.close();
		} catch (SQLException se) {
			System.out.println("SQL error:\n" + sql + "\n" + se.toString());
		}
		timeForEstimate = System.currentTimeMillis() - timeForEstimate;
		// create UI
		setLayout(new BorderLayout());
		Panel mp = new Panel(new ColumnLayout());
		add(mp, BorderLayout.WEST);
		mp.add(new Label(tableName + ": " + nid + " entities, " + ntr + " trajectories, " + nrec + " points"));
		mp.add(new Line(false));
		// general
		Panel p = new Panel(new FlowLayout());
		mp.add(p);
		p.add(new Label("Selection/sampling by:"));
		CheckboxGroup cbg = new CheckboxGroup();
		cbSamplingType = new Checkbox[2];
		p.add(cbSamplingType[0] = new Checkbox("by objects", cbg, false));
		p.add(cbSamplingType[1] = new Checkbox("by trajectories", cbg, true));
		mp.add(new Line(false));
		// query
		p = new Panel(new BorderLayout());
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
		TabbedPanel pQuery = null;
		long duration = 0;
		try {
			Statement statement = currTblConnector.getConnection().createStatement();
			String sql = "select min(npoints),max(npoints),max(duration),\n" + "min(distance),max(distance),max(displacement),max(br_diagonal),min(sdt),max(sdt),min(edt),max(edt),max(edt)-min(sdt)\n" + "from " + tableName + "_se";
			System.out.println("* <" + sql + ">");
			ResultSet result = statement.executeQuery(sql);
			ResultSetMetaData md = result.getMetaData();
			int timeColumnType = md.getColumnType(8);
			physicalTime = timeColumnType == java.sql.Types.DATE || timeColumnType == java.sql.Types.TIME || timeColumnType == java.sql.Types.TIMESTAMP;
			result.next();
			minnpoints = result.getLong(1);
			maxnpoints = result.getLong(2);
			maxduration = result.getFloat(3);
			mindistance = result.getFloat(4);
			maxdistance = result.getFloat(5);
			mindisplacement = 0;
			maxdisplacement = result.getFloat(6);
			minbr_diagonal = 0;
			maxbr_diagonal = result.getFloat(7);

			if (physicalTime) {
				String minmax[] = new String[2];
				minmax[0] = "Minimal date/time = " + result.getDate(8) + " " + result.getTime(8);
				minmax[1] = "Maximal date/time = " + result.getDate(11) + " " + result.getTime(11);
				if (physicalTime) {
					datePrecision = TimeDialogs.askDesiredDatePrecision(minmax);
				}
				dateScheme = TimeDialogs.getSuitableDateScheme(datePrecision);
				spade.time.Date dt = new spade.time.Date();
				dt.setPrecision(datePrecision);
				dt.setDateScheme(dateScheme);
				java.util.Date date = result.getDate(8);
				java.sql.Time time = result.getTime(8);
				dt.setDate(date, time);
				mintms = dt;
				dt = new spade.time.Date();
				dt.setPrecision(datePrecision);
				dt.setDateScheme(dateScheme);
				date = result.getDate(9);
				time = result.getTime(9);
				dt.setDate(date, time);
				maxtms = dt;
				dt = new spade.time.Date();
				dt.setPrecision(datePrecision);
				dt.setDateScheme(dateScheme);
				date = result.getDate(10);
				time = result.getTime(10);
				dt.setDate(date, time);
				mintme = dt;
				dt = new spade.time.Date();
				dt.setPrecision(datePrecision);
				dt.setDateScheme(dateScheme);
				date = result.getDate(11);
				time = result.getTime(11);
				dt.setDate(date, time);
				maxtme = dt;
				duration = result.getLong(12);
				//System.out.println("* duration="+duration);
			} else {
				mintms = new TimeCount(result.getLong(8));
				maxtms = new TimeCount(result.getLong(9));
				mintme = new TimeCount(result.getLong(10));
				maxtme = new TimeCount(result.getLong(11));
			}
			mintm = mintms.copyTo(mintm);
			maxtm = maxtms.copyTo(maxtm);

			statement.close();
		} catch (SQLException se) {
			System.out.println("SQL error:" + se.toString());
		}
		// create UI
		TabbedPanel tp = new TabbedPanel();
		Panel p1 = new Panel(new ColumnLayout()), p2 = new Panel(new ColumnLayout()), p3 = new Panel(new ColumnLayout()), p4 = new Panel(new ColumnLayout());
		pQuery = new TabbedPanel();
		pQuery.addComponent("Trajectory", p1);
		pQuery.addComponent("Start", p2);
		pQuery.addComponent("End", p3);
		pQuery.addComponent("Points", p4);

		f = new Focuser[10];
		tfMin = new TextField[10];
		tfMax = new TextField[10];

		p1.add(createFocuserPanel(0, "N points in trajectory:", minnpoints, maxnpoints));
		p1.add(createFocuserPanel(1, "Travelled distance:", mindistance, maxdistance));
		p1.add(createFocuserPanel(2, "Displacement:", mindisplacement, maxdisplacement));
		p1.add(createFocuserPanel(3, "Bounding rectangle diagonal:", minbr_diagonal, maxbr_diagonal));
		if (physicalTime) {
			p1.add(createFocuserPanel(4, "Duration(days):", 0, maxduration));
			p1.add(createFocuserPanel(5, "Duration(hours):", 0, maxduration * 24));
			p1.add(createFocuserPanel(6, "Duration(minutes):", 0, maxduration * 24 * 60));
		} else {
			p1.add(createFocuserPanel(4, "Duration(days):", 0, maxduration));
		}

		p2.add(new Label("Conditions for START points"));

		focusIntervalS = new FocusInterval();
		focusIntervalS.setDataInterval(mintms, maxtms);
		focusIntervalS.addPropertyChangeListener(this);
		TimeSlider timeSlider = new TimeSlider(focusIntervalS);
		TimeSliderPanel tspan = new TimeSliderPanel(timeSlider, this, true);
		p2.add(tspan);
		bFullExtentS = new Button("Full extent");
		bFullExtentS.addActionListener(this);
		bFullExtentS.setEnabled(false);
		p2.add(bFullExtentS);

		if (physicalTime) {
			cbMonths = new Checkbox[2][];
			cbDOWs = new Checkbox[2][]; //cbYears=new Checkbox[2][];
			p2.add(new Line(false));
			if (duration > 30) { // months
				Panel p = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
				p.add(new Label("months:"));
				p.add(bsMonthsAll = new Button("all"));
				bsMonthsAll.addActionListener(this);
				p.add(bsMonthsNone = new Button("none"));
				bsMonthsNone.addActionListener(this);
				cbMonths[0] = new Checkbox[12];
				for (int i = 0; i < cbMonths[0].length; i++) {
					cbMonths[0][i] = new Checkbox(String.valueOf(1 + i), true);
					cbMonths[0][i].addItemListener(this);
					p.add(cbMonths[0][i]);
				}
				p2.add(p);
				p2.add(new Line(false));
			}
			if (duration > 1) { // DOWs
				Panel p = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
				p.add(new Label("days:"));
				p.add(bsDOWsAll = new Button("all"));
				bsDOWsAll.addActionListener(this);
				p.add(bsDOWsNone = new Button("none"));
				bsDOWsNone.addActionListener(this);
				String dows[] = { "mon", "tue", "wed", "thu", "fri", "sat", "sun" };
				cbDOWs[0] = new Checkbox[dows.length];
				for (int i = 0; i < cbDOWs[0].length; i++) {
					cbDOWs[0][i] = new Checkbox("" + dows[i], true);
					cbDOWs[0][i].addItemListener(this);
					p.add(cbDOWs[0][i]);
				}
				p2.add(p);
				p2.add(new Line(false));
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
					p2.add(tspan);
					bFullExtent[n] = new Button("Full extent");
					bFullExtent[n].addActionListener(this);
					bFullExtent[n].setEnabled(false);
					p2.add(bFullExtent[n]);
					p2.add(new Line(false));
				}
			}
		}
		if (brr != null) {
			cbSbrr = new Checkbox("within [" + brr.rx1 + "," + brr.ry1 + "," + brr.rx2 + "," + brr.ry2 + "]", false);
			cbSbrr.addItemListener(this);
			p2.add(cbSbrr);
			p2.add(new Line(false));
		}

		p3.add(new Label("Conditions for END points"));
		focusIntervalE = new FocusInterval();
		focusIntervalE.setDataInterval(mintme, maxtme);
		focusIntervalE.addPropertyChangeListener(this);
		timeSlider = new TimeSlider(focusIntervalE);
		tspan = new TimeSliderPanel(timeSlider, this, true);
		p3.add(tspan);
		bFullExtentE = new Button("Full extent");
		bFullExtentE.addActionListener(this);
		bFullExtentE.setEnabled(false);
		p3.add(bFullExtentE);

		if (physicalTime) {
			p3.add(new Line(false));
			if (duration > 30) { // months
				Panel p = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
				p.add(new Label("months:"));
				p.add(beMonthsAll = new Button("all"));
				beMonthsAll.addActionListener(this);
				p.add(beMonthsNone = new Button("none"));
				beMonthsNone.addActionListener(this);
				cbMonths[1] = new Checkbox[12];
				for (int i = 0; i < cbMonths[1].length; i++) {
					cbMonths[1][i] = new Checkbox(String.valueOf(1 + i), true);
					cbMonths[1][i].addItemListener(this);
					p.add(cbMonths[1][i]);
				}
				p3.add(p);
				p3.add(new Line(false));
			}
			if (duration > 1) { // DOWs
				Panel p = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
				p.add(new Label("days:"));
				p.add(beDOWsAll = new Button("all"));
				beDOWsAll.addActionListener(this);
				p.add(beDOWsNone = new Button("none"));
				beDOWsNone.addActionListener(this);
				String dows[] = { "mon", "tue", "wed", "thu", "fri", "sat", "sun" };
				cbDOWs[1] = new Checkbox[dows.length];
				for (int i = 0; i < cbDOWs[1].length; i++) {
					cbDOWs[1][i] = new Checkbox("" + dows[i], true);
					cbDOWs[1][i].addItemListener(this);
					p.add(cbDOWs[1][i]);
				}
				p3.add(p);
				p3.add(new Line(false));
			}
			if (((Date) mintm).useElement('h') || ((Date) mintm).useElement('t') || ((Date) mintm).useElement('s')) {
				for (int n = 2; n <= 3; n++) {
					fiTime[n] = new FocusInterval();
					fiTime[n].setDataInterval(d000000, d235959);
					if (n == 1) {
						fiTime[n].setWhatIsFixed(FocusInterval.END);
					}
					fiTime[n].addPropertyChangeListener(this);
					timeSlider = new TimeSlider(fiTime[n]);
					tspan = new TimeSliderPanel(timeSlider, this, true);
					p3.add(tspan);
					bFullExtent[n] = new Button("Full extent");
					bFullExtent[n].addActionListener(this);
					bFullExtent[n].setEnabled(false);
					p3.add(bFullExtent[n]);
					p3.add(new Line(false));
				}
			}
		}
		if (brr != null) {
			cbEbrr = new Checkbox("within [" + brr.rx1 + "," + brr.ry1 + "," + brr.rx2 + "," + brr.ry2 + "]", false);
			cbEbrr.addItemListener(this);
			p3.add(cbEbrr);
			p3.add(new Line(false));
		}

		p4.add(new Label("Conditions for trajectory points"));
		if (brr != null) {
			cbTrInside = new Checkbox("inside [" + brr.rx1 + "," + brr.ry1 + "," + brr.rx2 + "," + brr.ry2 + "]", false);
			cbTrInside.addItemListener(this);
			p4.add(cbTrInside);
			p4.add(new Line(false));
		}

		pQuery.makeLayout();
		return pQuery;
	}

	private Panel createFocuserPanel(int idx, String lText, float fMin, float fMax) {
		Panel pf = new Panel(new BorderLayout());
		f[idx] = new Focuser();
		f[idx].setIsVertical(false);
		//f.setSingleDelimiter("right");
		f[idx].setAbsMinMax(fMin, fMax);
		f[idx].setIsUsedForQuery(true);
		f[idx].addFocusListener(this);
		tfMin[idx] = (Float.isNaN(fMin)) ? null : new TextField("" + fMin, 6);
		tfMax[idx] = (Float.isNaN(fMax)) ? null : new TextField("" + fMax, 6);
		f[idx].setTextFields(tfMin[idx], tfMax[idx]);
		pf.add(new Label(lText), BorderLayout.NORTH);
		if (tfMin[idx] != null) {
			pf.add(tfMin[idx], BorderLayout.WEST);
		}
		pf.add(new FocuserCanvas(f[idx], false), BorderLayout.CENTER);
		if (tfMax[idx] != null) {
			pf.add(tfMax[idx], BorderLayout.EAST);
		}
		return pf;
	}

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
		if (ie.getSource().equals(cbSbrr) || ie.getSource().equals(cbEbrr) || ie.getSource().equals(cbTrInside)) {
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
		if (ae.getSource().equals(bsMonthsAll) && cbMonths[0] != null) {
			for (int i = 0; i < cbMonths[0].length; i++) {
				cbMonths[0][i].setState(true);
			}
			generateQuerySQL();
		}
		if (ae.getSource().equals(bsMonthsNone) && cbMonths[0] != null) {
			for (int i = 0; i < cbMonths[0].length; i++) {
				cbMonths[0][i].setState(false);
			}
			generateQuerySQL();
		}
		if (ae.getSource().equals(beMonthsAll) && cbMonths[1] != null) {
			for (int i = 0; i < cbMonths[1].length; i++) {
				cbMonths[1][i].setState(true);
			}
			generateQuerySQL();
		}
		if (ae.getSource().equals(beMonthsNone) && cbMonths[1] != null) {
			for (int i = 0; i < cbMonths[1].length; i++) {
				cbMonths[1][i].setState(false);
			}
			generateQuerySQL();
		}
		if (ae.getSource().equals(bsDOWsAll) && cbDOWs[0] != null) {
			for (int i = 0; i < cbDOWs[0].length; i++) {
				cbDOWs[0][i].setState(true);
			}
			generateQuerySQL();
		}
		if (ae.getSource().equals(bsDOWsNone) && cbDOWs[0] != null) {
			for (int i = 0; i < cbDOWs[0].length; i++) {
				cbDOWs[0][i].setState(false);
			}
			generateQuerySQL();
		}
		if (ae.getSource().equals(beDOWsAll) && cbDOWs[1] != null) {
			for (int i = 0; i < cbDOWs[1].length; i++) {
				cbDOWs[1][i].setState(true);
			}
			generateQuerySQL();
		}
		if (ae.getSource().equals(beDOWsNone) && cbDOWs[1] != null) {
			for (int i = 0; i < cbDOWs[1].length; i++) {
				cbDOWs[1][i].setState(false);
			}
			generateQuerySQL();
		}
		if (ae.getSource().equals(bSaveView)) {
			createView();
		}
		if (ae.getSource().equals(bFullExtentS)) {
			focusIntervalS.showWholeInterval();
		}
		if (ae.getSource().equals(bFullExtentE)) {
			focusIntervalE.showWholeInterval();
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
			sql = "create or replace view "
					+ tableName
					+ viewName
					+ "_se as\n"
					+ ((getUseSampling()) ? "select a.* from AD071013_TA_H3_se a,\n" + "(SELECT DISTINCT " + ((isSamplingByObjects()) ? "ID_" : "TID_") + "\n" + "      FROM " + tableName + "_se\n" + "      SAMPLE (" + getSampleSize() + ")"
							+ ((Float.isNaN(getSeed())) ? "" : " SEED (" + getSeed() + ")") + "\n" + "      WHERE " + taQuery.getText() + ") b\n" + "where a.tid_=b.tid_   \n" + "order by a.tid_   " : "select * from " + tableName + "_se\n" + "where "
							+ taQuery.getText());
			System.out.println("* <" + sql + ">");
			statement.executeUpdate(sql);
			sql = "create or replace view " + tableName + viewName + " as\n" + "select a.* from " + tableName + " a,\n"
					+ ((isSamplingByObjects()) ? "(select distinct ID_ from " + tableName + viewName + "_se) b\n" + "where a.ID_ = b.ID_\n" : "(select distinct TID_ from " + tableName + viewName + "_se) b\n" + "where a.TID_ = b.TID_\n")
					+ "order by a.TID_, a.TNUM_";
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
			sql = "SELECT SUM(npoints), COUNT(DISTINCT a.tid_), COUNT(DISTINCT a.id_)\n" + "FROM " + tableName + "_se a,\n" + ((isSamplingByObjects()) ? "     (SELECT DISTINCT ID_\n" : "     (SELECT DISTINCT TID_\n") + "      FROM " + tableName
					+ "_se\n" + ((bSample) ? "      SAMPLE (" + fSample + ")" + ((Float.isNaN(fSeed) ? "" : " SEED (" + fSeed + ")")) + "\n" : "") + ((getUseQuery() && getQueryString().length() > 0) ? "      WHERE " + getQueryString() + "\n" : "")
					+ ") b\n" + ((isSamplingByObjects()) ? "WHERE a.ID_ = b.ID_" : "WHERE a.TID_ = b.TID_");
			if (loggingInConsole) {
				System.out.println("* <" + sql + ">");
			}
			ResultSet result = statement.executeQuery(sql);
			result.next();
			int nrec = result.getInt(1), ntr = result.getInt(2), nid = result.getInt(3);
			statement.close();
			if (bSample) {
				lEstimatesSample.setText("Estimation: " + nid + " entities, " + ntr + " trajectories, " + nrec + " points");
				lEstimatesSample.setEnabled(true);
				bEstimateSample.setEnabled(true);
			} else {
				lEstimatesQuery.setText("Estimation: " + nid + " entities, " + ntr + " trajectories, " + nrec + " points");
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
		sql += generateQuerySQL_fitem(0, sql, "npoints", minnpoints, maxnpoints);
		sql += generateQuerySQL_fitem(1, sql, "distance", mindistance, maxdistance);
		sql += generateQuerySQL_fitem(2, sql, "displacement", mindisplacement, maxdisplacement);
		sql += generateQuerySQL_fitem(3, sql, "br_diagonal", minbr_diagonal, maxbr_diagonal);
		sql += generateQuerySQL_fitem(4, sql, "duration", 0, maxduration);
		sql += generateQuerySQL_fitem(5, sql, "duration*24", 0, maxduration * 24);
		sql += generateQuerySQL_fitem(6, sql, "duration*24*60", 0, maxduration * 24 * 60);
		sql += generateQuerySQL_fitem(1, sql, "sdt");
		sql += generateQuerySQL_fitem(2, sql, "edt");
		if (cbSbrr != null && cbSbrr.getState()) {
			String extra = "(sx>=" + brr.rx1 + " and sy>=" + brr.ry1 + " and sx<=" + brr.rx2 + " and sy<=" + brr.ry2 + ")";
			if (sql.length() > 0) {
				extra = " and " + extra;
			}
			sql += extra;
		}
		if (cbEbrr != null && cbEbrr.getState()) {
			String extra = "(ex>=" + brr.rx1 + " and ey>=" + brr.ry1 + " and ex<=" + brr.rx2 + " and ey<=" + brr.ry2 + ")";
			if (sql.length() > 0) {
				extra = " and " + extra;
			}
			sql += extra;
		}
		if (cbTrInside != null && cbTrInside.getState()) {
			String extra = "(br_x1>=" + brr.rx1 + " and br_y1>=" + brr.ry1 + " and br_x2<=" + brr.rx2 + " and br_y2<=" + brr.ry2 + ")";
			if (sql.length() > 0) {
				extra = " and " + extra;
			}
			sql += extra;
		}
		if (physicalTime && cbMonths != null) {
			String str = generateSQLlist(cbMonths[0]);
			if (str != null) {
				String extra = "(TO_CHAR(sdt,'MM') in (" + str + "))";
				if (sql.length() > 0) {
					extra = " and " + extra;
				}
				sql += extra;
			}
			str = generateSQLlist(cbDOWs[0]);
			if (str != null) {
				String extra = "(TO_CHAR(sdt,'dy') in (" + str + "))";
				if (sql.length() > 0) {
					extra = " and " + extra;
				}
				sql += extra;
			}
			str = generateSQLlist(cbMonths[1]);
			if (str != null) {
				String extra = "(TO_CHAR(edt,'MM') in (" + str + "))";
				if (sql.length() > 0) {
					extra = " and " + extra;
				}
				sql += extra;
			}
			str = generateSQLlist(cbDOWs[1]);
			if (str != null) {
				String extra = "(TO_CHAR(edt,'dy') in (" + str + "))";
				if (sql.length() > 0) {
					extra = " and " + extra;
				}
				sql += extra;
			}
			if (fiTime != null) {
				for (int k = 0; k < fiTime.length; k += 2) {
					String extra = "";
					for (int n = k; n <= k + 1; n++)
						if (fiTime[n] != null && bFullExtent[n].isEnabled()) {
							if ((n == k + 1) && bFullExtent[k].isEnabled()) {
								extra += " or ";
							}
							if (!fiTime[n].getCurrIntervalStart().toString().equals("000000")) {
								extra += "to_char(" + ((n <= 1) ? "sdt" : "edt") + ",'sssss')>=" + "to_char(to_date('01012009" + fiTime[n].getCurrIntervalStart().toString() + "','ddmmyyyyhh24miss'),'sssss')";
							}
							if (!fiTime[n].getCurrIntervalEnd().toString().equals("235959") && !fiTime[n].getCurrIntervalEnd().toString().equals("240000")) {
								if (!fiTime[n].getCurrIntervalStart().toString().equals("000000")) {
									extra += " and ";
								}
								extra += "to_char(" + ((n <= 1) ? "sdt" : "edt") + ",'sssss')<=" + "to_char(to_date('01012009" + fiTime[n].getCurrIntervalEnd().toString() + "','ddmmyyyyhh24miss'),'sssss')";
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

	private String generateQuerySQL_fitem(int idx, String sql, String attr, float fMin, float fMax) {
		if (f[idx] == null)
			return "";
		String extra = "";
		if (f[idx].getCurrMin() > fMin) {
			if (sql.length() > 0) {
				extra += " and ";
			}
			extra += "(" + attr + " >= " + f[idx].getCurrMin() + ")";
		}
		if (f[idx].getCurrMax() < fMax) {
			if (sql.length() > 0 || extra.length() > 0) {
				extra += " and ";
			}
			extra += "(" + attr + " <= " + f[idx].getCurrMax() + ")";
		}
		return extra;
	}

	private String generateQuerySQL_fitem(int idx, String sql, String attr) {
		String extra = "";
		FocusInterval fi = (idx == 1) ? focusIntervalS : focusIntervalE;
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
		if (e.getSource() == focusIntervalS) {
			if (e.getPropertyName().equalsIgnoreCase("current_interval")) {
				TimeMoment t1 = (TimeMoment) e.getOldValue(), t2 = (TimeMoment) e.getNewValue();
				if (t2 != null && t2.subtract(t1) < 1) {
					t1 = t1.getCopy();
					t2 = t1.getCopy();
					t2.add(1);
					focusIntervalS.setCurrInterval(t1, t2);
				}
				if (bFullExtentS != null) {
					bFullExtentS.setEnabled(mintm.compareTo(t1) < 0 || maxtm.compareTo(t2) > 0);
				}
			}
			generateQuerySQL();
		}
		if (e.getSource() == focusIntervalE) {
			if (e.getPropertyName().equalsIgnoreCase("current_interval")) {
				TimeMoment t1 = (TimeMoment) e.getOldValue(), t2 = (TimeMoment) e.getNewValue();
				if (t2 != null && t2.subtract(t1) < 1) {
					t1 = t1.getCopy();
					t2 = t1.getCopy();
					t2.add(1);
					focusIntervalE.setCurrInterval(t1, t2);
				}
				if (bFullExtentE != null) {
					bFullExtentE.setEnabled(mintm.compareTo(t1) < 0 || maxtm.compareTo(t2) > 0);
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
