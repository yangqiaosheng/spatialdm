package spade.analysis.calc;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.plot.DotPlot;
import spade.analysis.plot.DotPlotNonLinear;
import spade.analysis.plot.DynamicQueryStat;
import spade.analysis.plot.ScatterPlotWithCrossLines;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.FocusListener;
import spade.lib.basicwin.Focuser;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.PlotCanvas;
import spade.lib.basicwin.SplitLayout;
import spade.lib.basicwin.SplitPanel;
import spade.lib.color.ColorCanvas;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;
import spade.vis.action.ObjectEvent;
import spade.vis.action.ObjectEventHandler;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataTable;

class RuleValidatorSP extends Frame implements ItemListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.calc.Res");

	protected Supervisor supervisor = null;
	protected DataTable dTable = null;
	protected int fn[] = null;
	protected Choice chv = null, chh = null;
	protected ScatterPlotWithCrossLines sp = null;
	protected PlotCanvas pc = null;

	public RuleValidatorSP(Supervisor supervisor, ObjectEventHandler oeh, DataTable dTable, int fn[]) {
		super(res.getString("Rule_Validator")); //"Rule Validator Scatter Plot"
		this.supervisor = supervisor;
		this.dTable = dTable;
		this.fn = fn;
		setLayout(new BorderLayout());
		chv = new Choice();
		chv.addItemListener(this);
		for (int i = 0; i < fn.length - 1; i++) {
			chv.addItem(dTable.getAttributeName(fn[i]));
		}
		add(chv, "North");
		chh = new Choice();
		chh.addItemListener(this);
		for (int i = 0; i < fn.length - 1; i++) {
			chh.addItem(dTable.getAttributeName(fn[i]));
		}
		chh.select(1);
		add(chh, "South");
		pc = new PlotCanvas();
		add(pc, "Center");
		sp = new ScatterPlotWithCrossLines(true, true, supervisor, oeh);
		sp.setDataSource(dTable);
		sp.setFieldNumbers(fn[1], fn[0]);
		sp.setIsZoomable(false);
		sp.setup();
		sp.checkWhatSelected();
		sp.setCanvas(pc);
		pc.setContent(sp);
		setSize(100, 200);
		//pc.setSize(100,200);
		show();
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		sp.setFieldNumbers(fn[chh.getSelectedIndex()], fn[chv.getSelectedIndex()]);
		sp.setup();
	}

	public void breaksChanged(int n, float min, float max) {
		if (n == chv.getSelectedIndex()) {
			sp.setVMax(max);
			sp.setVMin(min);
			//pc.repaint();
			return;
		}
		if (n == chh.getSelectedIndex()) {
			sp.setHMax(max);
			sp.setHMin(min);
			//pc.repaint();
			return;
		}
	}
}

class RuleValidatorDPCanvas extends PlotCanvas {
	static ResourceBundle res = ResourceBundle.getBundle("spade.analysis.calc.Res");
	protected DotPlot dpf = null;
	protected DotPlotNonLinear dp = null;
	protected String name = null;
	protected DataTable dTable = null;
	protected int fn = -1;
	protected int classes[] = null;
	boolean isFirstCondition = false, isTarget = false;

	public RuleValidatorDPCanvas(DotPlotNonLinear dp, DotPlot dpf, boolean isFirstCondition, boolean isTarget, DataTable dTable, int fn) {
		super();
		this.dp = dp;
		this.dpf = dpf;
		this.isFirstCondition = isFirstCondition;
		this.isTarget = isTarget;
		this.dTable = dTable;
		this.fn = fn;
		name = dTable.getAttributeName(fn);
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(40 * Metrics.mm(), 10 * Metrics.mm() + Metrics.fh);
	}

	public void setClasses(int classes[]) {
		this.classes = classes;
	}

	protected static int ds = 5; // dash size

	@Override
	public void paint(Graphics g) {
		int fh = Metrics.fh;
		int width = getSize().width, height = getSize().height;
		g.setColor(Color.black);
		String str = "";
		if (isFirstCondition) {
			g.drawString(res.getString("IF_"), 1, fh);
		}
		if (isTarget) {
			g.drawString(res.getString("THEN_"), 1, fh);
		}
		g.drawString(name, 10, 2 * fh);
		int y = 2 * fh + 3 * Metrics.mm() + 3 * ds, dy = height - 2 * fh - 3 * Metrics.mm() - 6 * ds;
		dp.setBounds(new Rectangle(10, y/*2*fh+3*Metrics.mm()+3*ds*/, width - 20, dy/*height-2*fh-3*Metrics.mm()-6*ds*/));
		dp.draw(g);

		if (classes != null) {
			dy = 8 * Metrics.mm();
			for (int i = 0; i < dTable.getDataItemCount(); i++)
				if (classes[i] >= 1 && classes[i] <= 3) {
					int n = (classes[i] == 1) ? 0 : ((classes[i] == 2) ? 2 : 1);
					int x = dp.mapX(dTable.getNumericAttrValue(fn, i));
					g.setColor(RuleValidator.valueColor[n].darker());
					g.drawLine(x, y + dy + ds * (n - 1), x, y + dy + ds * n);
				}
		}

		y = 2 * fh;
		dy = 3 * Metrics.mm();
		dpf.setBounds(new Rectangle(10, y/*2*fh*/, width - 20, dy/*3*Metrics.mm()*/));
		if (dpf.getIsHidden())
			return;
		dpf.draw(g);
		if (classes == null)
			return;
		y += Metrics.mm();
		for (int i = 0; i < dTable.getDataItemCount(); i++)
			if (classes[i] >= 1 && classes[i] <= 3) {
				int n = (classes[i] == 1) ? 0 : ((classes[i] == 2) ? 2 : 1);
				int x = dpf.mapX(dTable.getNumericAttrValue(fn, i));
				g.setColor(RuleValidator.valueColor[n].darker());
				g.drawLine(x, y + dy + ds * (n - 1), x, y + dy + ds * n);
			}
	}
}

public class RuleValidator extends CalcDlg implements ActionListener, ItemListener, ObjectEventHandler, FocusListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.calc.Res");

	public static final String expl = res.getString("vitr");//"Validates IF-THEN rules"
	public static final String prompt = res.getString("Scaatt");
	//"Select condition attribute(s) and the target attribute of the rule";
	protected static String valueList[] = { res.getString("support"), res.getString("contradict"), res.getString("do_not_support"), res.getString("not_relevant") };
	public static Color valueColor[] = { Color.green, Color.red, Color.yellow, Color.gray };
	protected static int ruleNumber = 0;

	protected Vector vdp = null, vdpf = null, // couples of dot plots for all attributes
			vsp = null, // scatter-plots for all attributes used in conditions
			vtfmin = null, vtfmax = null; // min/max text fields for all attributes
	protected RuleValidatorSP rvsp = null;
	protected float amin[] = null, amax[] = null;
	protected Vector vdpc = null; // Vector of canvasas with dot plots and focusers
	protected DynamicQueryStat dqs = null; // statistics Panel
	protected Panel pleft = null, pright = null;
	protected Checkbox dynUpdate = null, showText = null, focals[] = null;
	protected Label lqc = null, lqt = null; // quality in relation to conditions/target

	/**
	* Returns the minimum number of attributes needed for this computation
	*/
	@Override
	public int getMinAttrNumber() {
		return 2;
	}

	/**
	* Returns the maximum number of attributes needed for this computation.
	* If the maximum number is unlimited, returns -1.
	*/
	@Override
	public int getMaxAttrNumber() {
		return -1;
	}

	@Override
	protected void makeInterface() {
		ruleNumber++;
		//setTitle("IF-THEN Rule N "+ruleNumber+" Validation");
		setTitle(res.getString("itrn") + ruleNumber + res.getString(" Validation"));
		constructDotPlots();
		focals = new Checkbox[4 * fn.length];
		Panel mainP = new Panel();
		mainP.setLayout(new GridLayout(fn.length, 1));
		int N = -1;
		for (int i = 0; i < fn.length; i++) {
			Panel p = new Panel();
			p.setLayout(new BorderLayout());
			DotPlotNonLinear dp = (DotPlotNonLinear) vdp.elementAt(i);
			DotPlot dpf = (DotPlot) vdpf.elementAt(i);
			RuleValidatorDPCanvas dpc = new RuleValidatorDPCanvas(dp, dpf, i == 0, i == fn.length - 1, dTable, fn[i]);
			vdpc.addElement(dpc);
			dp.setCanvas(dpc);
			dpf.setCanvas(dpc);
			Panel pbottom = new Panel();
			pbottom.setLayout(new BorderLayout());
			pbottom.add((TextField) vtfmin.elementAt(i), "West");
			pbottom.add((TextField) vtfmax.elementAt(i), "East");
			pbottom.add(new Line(false), "South");
			Panel pbottomSub = new Panel();
			pbottomSub.setLayout(new FlowLayout());
			pbottom.add(pbottomSub, "South");
			CheckboxGroup cbg = new CheckboxGroup();
			pbottomSub.add(new Label(res.getString("Focal points: "))); //
			N++;
			pbottomSub.add(focals[N] = new Checkbox(res.getString("No"), true, cbg));
			focals[N].addItemListener(this);
			N++;
			pbottomSub.add(focals[N] = new Checkbox(res.getString("Left"), false, cbg));
			focals[N].addItemListener(this);
			N++;
			pbottomSub.add(focals[N] = new Checkbox(res.getString("Right"), false, cbg));
			focals[N].addItemListener(this);
			N++;
			pbottomSub.add(focals[N] = new Checkbox(res.getString("Both"), false, cbg));
			focals[N].addItemListener(this);
			p.add(dpc, "Center");
			p.add(pbottom, "South");
			mainP.add(p);
		}

		dqs = new DynamicQueryStat(dTable.getDataItemCount(), fn.length - 1);
		initialiseMinMax();
		for (int i = 0; i < fn.length - 1; i++) {
			updateAttributeStatistics(i);
		}

		Panel pSp = new Panel();
		pSp.setLayout(new GridLayout(fn.length, 1));
		vsp = new Vector(fn.length - 1, 10);
		for (int i = 0; i < fn.length - 1; i++) {
			PlotCanvas pc = new PlotCanvas();
			pSp.add(pc);
			ScatterPlotWithCrossLines sp = new ScatterPlotWithCrossLines(true, true, supervisor, this);
			vsp.addElement(sp);
			sp.setDataSource(dTable);
			sp.setFieldNumbers(fn[i], fn[fn.length - 1]);
			sp.setIsZoomable(false);
			sp.setup();
			sp.checkWhatSelected();
			sp.setCanvas(pc);
			pc.setContent(sp);
		}
		pSp.add(new Label(""));

		SplitPanel splP = new SplitPanel(true);
		SplitLayout spl = new SplitLayout(splP, SplitLayout.VERT);
		splP.setLayout(spl);
		spl.addComponent(mainP, 1f);
		spl.addComponent(dqs, 0.5f);
		spl.addComponent(pSp, 0.5f);

		Panel bottomp = new Panel();
		bottomp.setLayout(new ColumnLayout());
		Panel p = new Panel();
		p.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		ColorCanvas cc = new ColorCanvas();
		cc.setColor(valueColor[0]);
		p.add(cc);
		p.add(new Label(res.getString("_in_")));
		cc = new ColorCanvas();
		cc.setColor(valueColor[0]);
		p.add(cc);
		p.add(new Label(","));
		cc = new ColorCanvas();
		cc.setColor(valueColor[1]);
		p.add(cc);
		p.add(new Label(" "));
		p.add(lqc = new Label(""));
		bottomp.add(p);
		p = new Panel();
		p.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		cc = new ColorCanvas();
		cc.setColor(valueColor[0]);
		p.add(cc);
		p.add(new Label(res.getString("_in_")));
		cc = new ColorCanvas();
		cc.setColor(valueColor[0]);
		p.add(cc);
		p.add(new Label(","));
		cc = new ColorCanvas();
		cc.setColor(valueColor[2]);
		p.add(cc);
		p.add(new Label(" "));
		p.add(lqt = new Label(""));
		bottomp.add(p);
		bottomp.add(new Line(false));
		p = new Panel();
		p.setLayout(new FlowLayout());
		dynUpdate = new Checkbox(res.getString("Dynamic update"), true);
		p.add(dynUpdate);
		showText = new Checkbox(res.getString("Rule text"), false);
		showText.addItemListener(this);
		p.add(showText);
		Button b = new Button(res.getString("Cspw"));//"Conditions scatter-plot window"
		b.addActionListener(this);
		p.add(b);
		bottomp.add(p);

		setLayout(new BorderLayout());
		add(splP, "Center");
		add(bottomp, "South");
		pack();
	}

	protected void constructDotPlots() {
		if (vdp == null) {
			vdp = new Vector(fn.length, 10);
			vdpf = new Vector(fn.length, 10);
			vtfmin = new Vector(fn.length, 10);
			vtfmax = new Vector(fn.length, 10);
			vdpc = new Vector(fn.length, 10);
			for (int i = 0; i < fn.length; i++) {
				DotPlotNonLinear dp = constructDotPlot(i);
				vdp.addElement(dp);
				DotPlot dpf = constructFixedDotPlot(i);
				vdpf.addElement(dpf);
				TextField tfmin = new TextField(String.valueOf(dp.getFocuser().getCurrMin()), 10), tfmax = new TextField(String.valueOf(dp.getFocuser().getCurrMax()), 10);
				vtfmin.addElement(tfmin);
				vtfmax.addElement(tfmax);
				dp.getFocuser().setTextFields(tfmin, tfmax);
			}
		}
	}

	protected DotPlotNonLinear constructDotPlot(int i) {
		DotPlotNonLinear dp = new DotPlotNonLinear(true, false,/*selectionEnabled*/true, supervisor, this);
		dp.setDataSource(dTable);
		dp.setFieldNumber(fn[i]);
		dp.setIsZoomable(true);
		dp.setup();
		dp.getFocuser().addFocusListener(this);
		dp.getFocuser().setIsUsedForQuery(false); // to allow adjusting values
		dp.checkWhatSelected();
		dp.setTextDrawing(false);
		dp.getFocuser().setTextDrawing(false);
		dp.getFocuser().setToDrawCurrMinMax(false);
		return dp;
	}

	protected DotPlot constructFixedDotPlot(int i) {
		DotPlot dpf = new DotPlot(true, false,/*selectionEnabled*/true, supervisor, this);
		dpf.setDataSource(dTable);
		dpf.setFieldNumber(fn[i]);
		dpf.setIsHidden(true);
		dpf.setIsZoomable(false);
		//dpf.setFocuserOnLeft(true);
		dpf.setup();
		dpf.checkWhatSelected();
		dpf.setTextDrawing(false);
		dpf.setFocuserDrawsTexts(false);
		return dpf;
	}

	@Override
	protected void start() {
		createResultAttribute();
		updateResultAttribute();
	}

	@Override
	public String getExplanation() {
		return expl;
	}

	@Override
	public String getAttributeSelectionPrompt() {
		return prompt;
	}

	protected int classes[] = null;
	protected String savedAttrId = null;

	protected void createResultAttribute() {
		classes = new int[dTable.getDataItemCount()];
		for (int j = 0; j < classes.length; j++) {
			classes[j] = 0;
		}
		// adding the column to the table
		Vector sourceAttr = new Vector(fn.length, 10);
		for (int element : fn) {
			sourceAttr.addElement(dTable.getAttributeId(element));
		}
		int idx = dTable.addDerivedAttribute(res.getString("Rule_N_") + ruleNumber, AttributeTypes.character, AttributeTypes.classify, sourceAttr);
		dTable.getAttribute(idx).setValueListAndColors(valueList, valueColor);
		// edit attr.name
		savedAttrId = dTable.getAttributeId(idx);
		Vector resultAttr = new Vector(1, 1);
		resultAttr.addElement(new String(savedAttrId));
		AttrNameEditor.attrAddedToTable(dTable, resultAttr);
		// inform all displays about change of values
		dTable.notifyPropertyChange("values", null, resultAttr);
		// show results of calculations on the map
		tryShowOnMap(resultAttr, "qualitative_colour", true);
	}

	protected void initialiseMinMax() {
		if (amin == null || amin.length != fn.length) {
			amin = new float[fn.length];
			amax = new float[fn.length];
		}
		for (int i = 0; i < fn.length; i++) {
			Focuser fo = ((DotPlot) vdp.elementAt(i)).getFocuser();
			amin[i] = (float) fo.getCurrMin();
			amax[i] = (float) fo.getCurrMax();
		}
	}

	protected void updateResultAttribute() {
		initialiseMinMax();
		int n1 = 0, n2 = 0, n3 = 0;
		for (int j = 0; j < dTable.getDataItemCount(); j++) {
			boolean conditionsOK = true;
			for (int i = 0; i < fn.length; i++) {
				float val = (float) dTable.getNumericAttrValue(fn[i], j);
				if (Float.isNaN(val)) {
					classes[j] = 0;
					break;
				}
				if (i == fn.length - 1) {
					boolean targetOK = val >= amin[i] && val <= amax[i];
					if (targetOK)
						if (conditionsOK) { // support
							classes[j] = 1;
							n1++;
						} else { // if=false, then=true
							classes[j] = 2;
							n2++;
						}
					else if (conditionsOK) { // contradict
						classes[j] = 3;
						n3++;
					} else {
						classes[j] = 4; // not relevant
					}
				} else if (conditionsOK) {
					conditionsOK = val >= amin[i] && val <= amax[i];
				}
			}
		}
		if (n1 + n3 > 0) {
			int n = n1 + n3;
			float f = n1 * 100 / n;
			lqc.setText(" " + n1 + res.getString("_of_") + n + " (" + StringUtil.floatToStr(f, 2) + "%) : " + res.getString("sup"));//"support (quality in respect to the conditions)"
		} else {
			lqc.setText("");
		}
		if (n1 + n2 > 0) {
			int n = n1 + n2;
			float f = n1 * 100 / n;
			lqt.setText(" " + n1 + res.getString("_of_") + n + " (" + StringUtil.floatToStr(f, 2) + "%) : " + res.getString("cov"));//"coverage (quality in respect to the target)"
		} else {
			lqt.setText("");
		}
		int idx = dTable.getAttrIndex(savedAttrId);
		for (int j = 0; j < dTable.getDataItemCount(); j++) {
			String str = "";
			switch (classes[j]) {
			case 1:
				str = valueList[0];
				break; //"support"
			case 2:
				str = valueList[2];
				break; //"do not support"
			case 3:
				str = valueList[1];
				break; //"contradict"
			case 4:
				str = valueList[3];
				break; //"not relevant"
			}
			dTable.getDataRecord(j).setAttrValue(str, idx);
		}
		for (int i = 0; i < fn.length; i++) {
			RuleValidatorDPCanvas dpc = (RuleValidatorDPCanvas) vdpc.elementAt(i);
			dpc.setClasses(classes);
		}
		Vector resultAttr = new Vector(1, 1);
		resultAttr.addElement(new String(savedAttrId));
		dTable.notifyPropertyChange("values", null, resultAttr);
	}

	protected void updateAttributeStatistics(int n) {
		int nplus = 0, nnan = 0;
		for (int j = 0; j < dTable.getDataItemCount(); j++) {
			float v = (float) dTable.getNumericAttrValue(fn[n], j);
			if (Float.isNaN(v)) {
				nnan++;
			} else if (v >= amin[n] && v <= amax[n]) {
				nplus++;
			}
		}
		dqs.setNumbers(n, nplus, nnan);
	}

	@Override
	public void focusChanged(Object source, double lowerLimit, double upperLimit) {
		if (!(source instanceof Focuser))
			return;
		Focuser fo = (Focuser) source;
		for (int i = 0; i < vdp.size(); i++)
			if (fo == ((DotPlot) vdp.elementAt(i)).getFocuser()) {
				if (rvsp != null) {
					rvsp.breaksChanged(i, (float) lowerLimit, (float) upperLimit);
				}
				((DotPlot) vdpf.elementAt(i)).setIsHidden(!fo.isRestricted());
				if (i < vdp.size() - 1) {
					ScatterPlotWithCrossLines sp = (ScatterPlotWithCrossLines) vsp.elementAt(i);
					sp.setHMin(lowerLimit);
					sp.setHMax(upperLimit);
				} else {
					for (int j = 0; j < vdp.size() - 1; j++) {
						ScatterPlotWithCrossLines sp = (ScatterPlotWithCrossLines) vsp.elementAt(j);
						sp.setVMin(lowerLimit);
						sp.setVMax(upperLimit);
					}
				}
				updateResultAttribute();
				updateAttributeStatistics(i);
				return;
			}
	}

	@Override
	public void limitIsMoving(Object source, int n, double currValue) {
		if (!dynUpdate.getState())
			return;
		if (!(source instanceof Focuser))
			return;
		Focuser fo = (Focuser) source;
		for (int i = 0; i < vdp.size(); i++)
			if (fo == ((DotPlot) vdp.elementAt(i)).getFocuser()) {
				((DotPlot) vdpf.elementAt(i)).setIsHidden(false);
				break;
			}
		if (n == 0) {
			focusChanged(source, currValue, fo.getCurrMax());
		} else {
			focusChanged(source, fo.getCurrMin(), currValue);
		}
	}

	/**
	* A method from the ObjectEventHandler interface.
	* The RuleValidator receives object events from its dot plots and tranferres
	* them to the supervisor.
	*/
	@Override
	public void processObjectEvent(ObjectEvent oevt) {
		if (supervisor != null) {
			supervisor.processObjectEvent(new ObjectEvent(this, oevt.getType(), oevt.getSourceMouseEvent(), oevt.getSetIdentifier(), oevt.getAffectedObjects()));
		}
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (focals == null)
			return;
		int N = -1;
		for (int i = 0; i < fn.length; i++) {
			if (focals[N + 1].equals(ie.getSource())) {
				DotPlotNonLinear dp = (DotPlotNonLinear) vdp.elementAt(i);
				dp.setFp(null, null);
				//dp.setIsZoomable(true);
				//dp.getFocuser().setIsUsedForQuery(false); // to allow adjusting values
				dp.setFocuserOnLeft(true);
				return;
			}
			if (focals[N + 2].equals(ie.getSource())) {
				DotPlotNonLinear dp = (DotPlotNonLinear) vdp.elementAt(i);
				double abs[] = new double[1], scr[] = new double[1];
				abs[0] = dp.getFocuser().getCurrMin();
				scr[0] = 0.5f;
				dp.setFp(abs, scr);
				//dp.setIsZoomable(false);
				//dp.getFocuser().setIsUsedForQuery(true);
				dp.setFocuserOnLeft(false);
				return;
			}
			if (focals[N + 3].equals(ie.getSource())) {
				DotPlotNonLinear dp = (DotPlotNonLinear) vdp.elementAt(i);
				double abs[] = new double[1], scr[] = new double[1];
				abs[0] = dp.getFocuser().getCurrMax();
				scr[0] = 0.5f;
				dp.setFp(abs, scr);
				//dp.setIsZoomable(false);
				//dp.getFocuser().setIsUsedForQuery(true);
				dp.setFocuserOnLeft(false);
				return;
			}
			if (focals[N + 3].equals(ie.getSource())) {
				DotPlotNonLinear dp = (DotPlotNonLinear) vdp.elementAt(i);
				//dp.setIsZoomable(false);
				return;
			}
			N += 4;
		}
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		rvsp = new RuleValidatorSP(supervisor, this, dTable, fn);
	}
}
