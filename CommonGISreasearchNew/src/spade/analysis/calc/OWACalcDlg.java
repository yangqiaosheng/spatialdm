package spade.analysis.calc;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.generators.PCPGenerator;
import spade.analysis.plot.FNReorder;
import spade.analysis.plot.PCPlot;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.PopupManager;
import spade.lib.basicwin.Slider;
import spade.lib.basicwin.SplitLayout;
import spade.lib.basicwin.TabbedPanel;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeTypes;
import spade.vis.database.TableStat;

/**
 * OWACalcDlg - Dialog for Ordered Weighted Averaging, a multi-criteria aggregation method
 * Much of the code has been copied and/or modified from IdealPointCalc.java provided by G.& N.Andrienko - thanks!
 * @author Claus Rinner, Institute for Geoinformatics, University of M\uFFFCnster, Germany
 */
public class OWACalcDlg extends CalcDlg implements ActionListener, ItemListener {

	static public ResourceBundle res = Language.getTextResource("spade.analysis.calc.Res");

	/* following text: "Aggregates values of multiple criteria into a single evaluation " +
	 *   "score according to a user-defined decision strategy" */
	private static final String strExplanation = res.getString("Aggregates_values_of") + res.getString("score_according_to_a");

	/* following text: "Select at least two numeric attributes " +
	 *   "representing criteria for evaluation"*/
	private static final String strPrompt = res.getString("Select_at_least_two") + res.getString("representing_criteria");

	protected TableStat tStat = null;
	protected float score[] = null;
	protected boolean orderAfterScore = true;

	protected TabbedPanel tabP = null;
	protected SplitLayout splL = null;

	protected WAPanel cp = null;
	protected PCPlot pcp = null;
	protected FNReorder pcpl = null;

	protected String AttrIDscore = null;
	protected String AttrIDorder = null;

	protected Checkbox cbClassify = null;

	private Vector vLabels = null; // "best value", ..., "worst value"
	private Vector vSliders = null; // sliders for order weights
	private Vector vWeights = null; // display of order weights

	private TextField tfOrness;
	private TextField tfTradeoff;
	private TextField tfDispersion;

	private Canvas cnvImage = null; // for triangle graph
	private Panel bottomP = null, bottomSubP = null;
	private Button btnOK;
	private Button btnCancel;

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

	/**
	 * Default constructor
	 */
	public OWACalcDlg() {
		super();
	}

	/**
	* Constructs the dialog appearance
	*/
	@Override
	protected void makeInterface() {
		// following text: "Ordered Weighted Averaging"
		this.setTitle(res.getString("ordwa"));
		this.setSize(300, 200);
		this.setLayout(new BorderLayout());

		Panel mainP = new Panel();
		splL = new SplitLayout(mainP, SplitLayout.VERT);
		splL.addComponent(makeWeightPanel(), 0.5f);
		splL.addComponent(makeTrianglePanel(), 0.5f);
		mainP.setLayout(splL);

		tabP = new TabbedPanel();
		// following text: "Order weights"
		tabP.addComponent(res.getString("order_weights"), mainP);
		tabP.makeLayout();

		add(tabP, "Center");

		bottomP = new Panel();
		bottomP.setLayout(new ColumnLayout());
		bottomP.add(new Line(false));

		bottomSubP = new Panel();
		bottomSubP.setLayout(new FlowLayout());
		bottomP.add(bottomSubP);

		boolean classify = (supervisor != null) && (supervisor.getSystemSettings() != null) && supervisor.getSystemSettings().checkParameterValue("DECISION_TYPE", "classification");
		//following text: "Classify results"
		cbClassify = new Checkbox(res.getString("classres"), classify);
		cbClassify.addItemListener(this);
		bottomSubP.add(cbClassify);

		/*
		btnOK = new Button("OK");
		btnOK.addActionListener(this);
		btnOK.setActionCommand("ok");
		bottomP.add(btnOK);
		*/
		// following text: "Close"
		btnCancel = new Button(res.getString("Close"));
		btnCancel.addActionListener(this);
		btnCancel.setActionCommand("cancel");
		bottomSubP.add(btnCancel);

		add(bottomP, "South");

		this.doLayout();
		this.pack();
	}

	/**
	 * Creates the order weight panel.
	 * This is used for the initial definition of a decision strategy and, later,
	 * as a tab folder besides the importance weight panel.
	 */
	protected Panel makeWeightPanel() {
		Slider sl;
		Label l;
		Button b;
		int n = this.fn.length; // number of criteria (= fields)

		// create the order weight panel to be returned by this method
		Panel pnlWeights = new Panel();
		pnlWeights.setLayout(new BorderLayout());

		// initialize vectors
		vLabels = new Vector();
		vSliders = new Vector();
		vWeights = new Vector();

		// fill vectors with default, equal weights
		for (int i = 0; i < n; i++) {
			// explanation labels above each slider
			// following text: "Weight for highest values"
			if (i == 0) {
				vLabels.addElement(new Label(res.getString("Wfhv")));
			} else if (i == (n - 1)) {
				vLabels.addElement(new Label(res.getString("Wflv")));
			} else {
				vLabels.addElement(new Label()); // no labels for intermediate sliders
			}

			// weight value labels to the right of sliders, with tooltip
			l = new Label(StringUtil.floatToStr(1.0f / n, 2));
			// following text:  "Use slider to change this order weight"
			new PopupManager(l, res.getString("Uscow"), true);
			vWeights.addElement(l);

			// sliders, with tooltip
			sl = new Slider(this, 0.0f, 1.0f, 1.0f / n);
			sl.setNAD(true);
			//following text: "Set order weight for highest weighted standardized criterion value of each alternative"
			if (i == 0) {
				new PopupManager(sl, res.getString("Sowfh"), true);
			} else if (i == (n - 1)) {
				new PopupManager(sl, res.getString("Sowfl"), true);
			} else {
				new PopupManager(sl, res.getString("Sowfi"), true);
			}
			vSliders.addElement(sl);
		}

		// create upper panel for sliders
		Panel upP = new Panel();

		pnlWeights.add(upP, "Center");

		// variant with scroll panel: may require checking size of upper panel (see WAPanel.java)
/*
    scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
    scp.add(upP);
    pnlWeights.add(scp, "Center");
*/

		// fill upper panel
		upP.setLayout(new GridLayout(n, 1));
		for (int i = 0; i < n; i++) {
			Panel pp = new Panel();
			pp.setLayout(new ColumnLayout());
			Panel p = new Panel();
			p.setLayout(new BorderLayout());
			p.add((Label) vLabels.elementAt(i), "Center");
			pp.add(p);
			p = new Panel();
			p.setLayout(new BorderLayout());
			if (n > 1) {
				p.add((Slider) vSliders.elementAt(i), "Center");
				p.add((Label) vWeights.elementAt(i), "East");
			}
			p.add(new Line(false), "South");
			pp.add(p);
			upP.add(pp);
		}

		// create lower panel for buttons
		Panel loP = new Panel();
		loP.setLayout(new ColumnLayout());

		// fill lower panel
		Panel pnlButtons = new Panel();
		pnlButtons.setLayout(new GridLayout(2, 3));
		pnlButtons.add(new Label()); // dummy
		// following text: "Set to WLC"
		b = new Button(res.getString("stwlc"));
		b.addActionListener(this);
		b.setActionCommand("wlc");
		// following text: "Set equal weights for full tradeoff between good and bad values"
		new PopupManager(b, res.getString("Sewff"), true);
		pnlButtons.add(b);
		pnlButtons.add(new Label()); // dummy
		// following text: "Set to AND"
		b = new Button(res.getString("Sta"));
		b.addActionListener(this);
		b.setActionCommand("and");
		// following text: "Give full weight to worst value of each alternative (pessimistic strategy)"
		new PopupManager(b, res.getString("Gfww"), true);
		pnlButtons.add(b);
		// following text: "Random"
		b = new Button(res.getString("Random"));
		b.addActionListener(this);
		b.setActionCommand("random");
		// following text: "Set random order weights"
		new PopupManager(b, res.getString("Srow"), true);
		pnlButtons.add(b);
		// following text:  "Set to OR"
		b = new Button(res.getString("Sto"));
		b.addActionListener(this);
		b.setActionCommand("or");
		// following text: "Give full weight to best value of each alternative (optimistic strategy)"
		new PopupManager(b, res.getString("Gfwb"), true);
		pnlButtons.add(b);

		loP.add(pnlButtons);
		pnlWeights.add(loP, "South");

		return pnlWeights;
	}

	/**
	 * Creates the graphical display of OWA behavior (triangle) and text fields for risk, tradeoff, and dispersion measures.
	 */
	protected Panel makeTrianglePanel() {
		Panel pnlTriangle = new Panel();
		pnlTriangle.setLayout(new BorderLayout());

		// add a coordiante system for tradeoff and dispersion vs. risk (orness)
		cnvImage = new Canvas() {

			@Override
			public Dimension getMinimumSize() {
				return new Dimension(150, 120);
			}

			@Override
			public Dimension getPreferredSize() {
				return new Dimension(200, 160);
			}

			@Override
			public void paint(Graphics g) {
				if ((tfOrness != null) && (tfTradeoff != null) && (tfDispersion != null)) {
					float orness = Float.valueOf(tfOrness.getText()).floatValue();
					float tradeoff = Float.valueOf(tfTradeoff.getText()).floatValue();
					float dispersion = Float.valueOf(tfDispersion.getText()).floatValue();

					int w = this.getSize().width;
					int h = this.getSize().height;

					g.setColor(Color.black);
					g.drawLine(3, h - 5, w - 5, h - 5); // risk axis
					g.drawLine(5, h - 3, 5, 5); // tradeoff axis

					// x axis label

					// following text: "Risk"
					g.drawString(res.getString("Risk"), w - 40, h - 10);
					// y axis labels
					g.setColor(Color.green);
					// following text: "Dispersion"
					g.drawString(res.getString("Dispersion"), 10, 15);
					g.setColor(Color.red);
					// following text: "Tradeoff"
					g.drawString(res.getString("Tradeoff"), 10, 30);

					g.setColor(Color.black);
					g.drawLine(3, h / 2, 7, h / 2); // x=0.5
					g.drawLine(3, 5, 7, 5);
					g.drawLine(w / 2, h - 3, w / 2, h - 7); // y=0.5
					g.drawLine(w - 5, h - 3, w - 5, h - 7);

					g.setColor(Color.gray);
					g.drawLine(5, h - 5, w / 2, 5);
					g.drawLine(w / 2, 5, w - 5, h - 5);
					g.drawLine(w - 5, h - 5, 5, h - 5);

					g.setColor(Color.red);
					g.drawLine((int) (5 + orness * (w - 10)) - 5, (int) (h - 5 - tradeoff * (h - 10)) + 0, (int) (5 + orness * (w - 10)) + 5, (int) (h - 5 - tradeoff * (h - 10)) + 0);
					g.drawLine((int) (5 + orness * (w - 10)) + 0, (int) (h - 5 - tradeoff * (h - 10)) + 5, (int) (5 + orness * (w - 10)) + 0, (int) (h - 5 - tradeoff * (h - 10)) - 5);
					g.drawOval((int) (5 + orness * (w - 10)) - 3, (int) (h - 5 - tradeoff * (h - 10)) - 3, 6, 6);

					g.setColor(Color.green);
					g.drawLine((int) (5 + orness * (w - 10)) - 5, (int) (h - 5 - dispersion * (h - 10)) + 0, (int) (5 + orness * (w - 10)) + 5, (int) (h - 5 - dispersion * (h - 10)) + 0);
					g.drawLine((int) (5 + orness * (w - 10)) + 0, (int) (h - 5 - dispersion * (h - 10)) + 5, (int) (5 + orness * (w - 10)) + 0, (int) (h - 5 - dispersion * (h - 10)) - 5);
					g.drawOval((int) (5 + orness * (w - 10)) - 3, (int) (h - 5 - dispersion * (h - 10)) - 3, 6, 6);
				}
			}
		};

		Panel pnlImage = new Panel();
		pnlImage.add(cnvImage);
		pnlTriangle.add("Center", pnlImage);

		// create lower panel for display of OWA measures
		Panel pnlMeasures = new Panel();
		pnlMeasures.setLayout(new ColumnLayout());
		Panel p;
		Button b;

		Panel pnlOrness = new Panel();
		// following text: "Risk"
		pnlOrness.add(new Label(res.getString("Risk")));
		pnlOrness.add(tfOrness = new TextField("0.0"));
		p = new Panel();
		b = new Button("+");
		b.addActionListener(this);
		b.setActionCommand("moreRisk");

		// following text: "Increase risk by increasing weight of best value"
		new PopupManager(b, res.getString("Irbiww"), true);
		p.add(b);
		b = new Button("-");
		b.addActionListener(this);
		b.setActionCommand("lessRisk");
		// following text: "Reduce risk by increasing weight of worst value"
		new PopupManager(b, res.getString("rrbiw"), true);
		p.add(b);
		pnlOrness.add(p);
		pnlMeasures.add(pnlOrness);

		Panel pnlTradeoff = new Panel();
		// following text:  "Tradeoff"
		pnlTradeoff.add(new Label(res.getString("Tradeoff")));
		pnlTradeoff.add(tfTradeoff = new TextField("0.0"));
		p = new Panel();
		b = new Button("+");
		b.addActionListener(this);
		b.setActionCommand("moreTradeoff");
		// following text:  "Increase tradeoff by reducing maximal weight"
		new PopupManager(b, res.getString("It"), true);
		p.add(b);
		b = new Button("-");
		b.addActionListener(this);
		b.setActionCommand("lessTradeoff");
		// following text: "Reduce tradeoff by increasing maximal weight"
		new PopupManager(b, res.getString("Rt"), true);
		p.add(b);
		pnlTradeoff.add(p);
		pnlMeasures.add(pnlTradeoff);

		Panel pnlDispersion = new Panel();
		// following text: "Dispersion"
		pnlDispersion.add(new Label(res.getString("Dispersion")));
		pnlDispersion.add(tfDispersion = new TextField("0.0"));
		p = new Panel();
		b = new Button("+");
		b.addActionListener(this);
		b.setActionCommand("moreTradeoff");
		// following text: "Increase dispersion by reducing maximal weight"
		new PopupManager(b, res.getString("Id"), true);
		p.add(b);
		b = new Button("-");
		b.addActionListener(this);
		b.setActionCommand("lessTradeoff");
		// following text: "Reduce dispersion by increasing maximal weight"
		new PopupManager(b, res.getString("Rd"), true);
		p.add(b);
		pnlDispersion.add(p);
		pnlMeasures.add(pnlDispersion);

		// add display for measures below triangle graph
		pnlTriangle.add("South", pnlMeasures);

		// calculate measures for current set of order weights
		this.updateMeasures();

		return pnlTriangle;
	}

	/**
	* Create user interface and display decision strategy panel.
	* Returns null because calculations are combined with visualisation and do not finish immediately.
	*/
	@Override
	public Vector doCalculation() {
		makeInterface();
		show();

		// remove if calculation process is to be started only after user's initial definition of order weights
		start();

		return null;
	}

	/**
	* Starts the calculation process
	*/
	@Override
	protected void start() {

		// change dialog appearance
		//btnCancel.setLabel("Close");
		//bottomP.remove(btnOK);

		// add panel for importance weights (criterion weights) as the second tab and bring it to front
		Panel mainP = new Panel();
		splL = new SplitLayout(mainP, SplitLayout.VERT);
		mainP.setLayout(splL);
		Vector attr = null;
		if (fn != null) {
			attr = new Vector(fn.length, 1);
			for (int element : fn) {
				attr.addElement(dTable.getAttributeId(element));
			}
		}
		cp = new WAPanel(this, dTable, attr, true, false); // criterion weight panel without choices
		splL.addComponent(cp, 0.4f);
		// following text:  "Criterion weights"
		tabP.addComponent(res.getString("Criterion_weights"), mainP);
		tabP.makeLayout();
		// following text: "Criterion weights"
		tabP.showTab("Criterion weights");

		// computing new column
		float vals[] = compute();

		// adding the column to the table
		int idx = dTable.addDerivedAttribute("Evaluation score", AttributeTypes.real, AttributeTypes.evaluate_score, attr);
		/*
		for (int i=0; i<dTable.getDataItemCount(); i++) {
		  dTable.getDataRecord(i).setAttrValue("" + vals[i], idx);
		} */
		dTable.setNumericAttributeValues(vals, idx);
		AttrIDscore = dTable.getAttributeId(idx);

		// computing orders
		int order[] = TableStat.getOrderOfColumn(dTable, idx);
		idx = dTable.addDerivedAttribute("Ranking", AttributeTypes.integer, AttributeTypes.evaluate_rank, attr);

		for (int i = 0; i < dTable.getDataItemCount(); i++) {
			//dTable.getDataRecord(i).setAttrValue(""+order[i],idx);
			dTable.getDataRecord(i).setAttrValue((order[i] == -1) ? "" : String.valueOf(order[i]), idx);
		}

		//dTable.setNumericAttributeValues(order,idx);
		AttrIDorder = dTable.getAttributeId(idx);

		// set prohibited attributes for CP
		String pa[] = new String[2];
		pa[0] = AttrIDscore;
		pa[1] = AttrIDorder;
		cp.setProhibitedAttributes(pa);

		// prepare results
		Vector resultAttrs = new Vector(2, 5);
		resultAttrs.addElement(AttrIDscore);
		resultAttrs.addElement(AttrIDorder);
		Vector mapAttr = new Vector(1, 5);
		mapAttr.addElement(AttrIDorder);

		// add attribute dependency and notify about new attribute
		attrAddedToTable(resultAttrs);

		// show results of calculations on the map
		tryShowOnMap(mapAttr, cbClassify.getState() ? "class1D" : "value_paint", true);

		PCPGenerator vg = new PCPGenerator();
		float w[] = cp.getWeights();
		float weights[] = new float[w.length + 2];
		boolean isMax[] = cp.getIsMax();

		for (int i = 0; i < w.length; i++) {
			weights[i] = w[i] * ((isMax[i]) ? 1 : -1);
		}

		if (true) {
			weights[w.length] = 1;
			weights[w.length + 1] = -1;
		}
		//else { weights[w.length]=-1; weights[w.length+1]=+1; }

		Object o[] = vg.constructDisplayIPCalc(supervisor, dTable, getInvolvedAttrs(), weights);
		//setPCP((Component)o[0],(java.awt.Choice)o[1],(PCPlot)o[2]);

		splL.addComponent((Component) o[0], 2f);
		pack();

		supervisor.notifyGlobalPropertyChange(Supervisor.eventDisplayedAttrs);
		this.pcp = (PCPlot) o[2];
		this.pcpl = (FNReorder) o[3];
		pcpl.setGroupBreak((float) (fn.length - 0.5));
		pcpl.addActionListener(this);
		setWeightsInPcp();

		this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
	}

	/**
	 * Returns list of attribute IDs for criteria plus evaluation score and ranking column IDs
	 */
	private Vector getInvolvedAttrs() {
		Vector v = new Vector(fn.length + 2);

		for (int element : fn) {
			v.addElement(new String(dTable.getAttributeId(element)));
		}
		v.addElement(new String(AttrIDscore));
		v.addElement(new String(AttrIDorder));

		return v;
	}

	/**
	 * Handles GUI events
	 * Possible events:
	 * <ul>
	 * <li>switch of "Classify" checkbox
	 * </ul>
	 */
	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (ie.getSource().equals(cbClassify)) {
			Vector mapAttr = new Vector(1, 5);
			mapAttr.addElement(AttrIDorder);
			tryShowOnMap(mapAttr, cbClassify.getState() ? "class1D" : "value_paint", true);
		}
	}

	/**
	 * Handles GUI events
	 * Possible events:
	 * <ul>
	 * <li>click on OK or CANCEL/CLOSE buttons
	 * <li>click on AND, WLC, OR, or RANDOM buttons
	 * <li>click on PLUS or MINUS buttons to increase risk or tradeoff (same operation for dispersion)
	 * <li>dragging of sliders
	 * <li>event on criterion weight panel (adding/removing criteria) not yet implemented
	 * </ul>
	 */
	@Override
	public void actionPerformed(ActionEvent ae) {
		// event was clicking a button
		if (ae.getSource() instanceof Button) {
			Button b = (Button) ae.getSource();

			// dialog's OK and CANCEL/CLOSE buttons
			if (b.getActionCommand().equalsIgnoreCase("ok")) {
				this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
				this.start();
			} else if (b.getActionCommand().equalsIgnoreCase("cancel")) {
				//this.hide();
				this.setVisible(false);
				this.dispose();
			}

			// different changes of order weights
			else {
				if (b.getActionCommand().equalsIgnoreCase("wlc")) {
					this.setWeightsToWLC();
				} else if (b.getActionCommand().equalsIgnoreCase("or")) {
					this.setWeightsToOr();
				} else if (b.getActionCommand().equalsIgnoreCase("and")) {
					this.setWeightsToAnd();
				} else if (b.getActionCommand().equalsIgnoreCase("random")) {
					this.setWeightsRandomly();
				} else if (b.getActionCommand().equalsIgnoreCase("moreRisk")) {
					float value = (float) ((Slider) vSliders.firstElement()).getValue();
					value *= 1.2; // increase weight of best criterion by 20%
					if (value > 1.0f) {
						value = 1.0f;
					}
					this.updateWeights(0, value);
				} else if (b.getActionCommand().equalsIgnoreCase("lessRisk")) {
					float value = (float) ((Slider) vSliders.lastElement()).getValue();
					value *= 1.2f; // increase weight of worst criterion by 20%
					if (value > 1.0f) {
						value = 1.0f;
					}
					this.updateWeights(vWeights.size() - 1, value);
				} else if (b.getActionCommand().equalsIgnoreCase("moreTradeoff")) {
					float value = (float) ((Slider) vSliders.firstElement()).getValue();
					int index = 0;
					for (int i = 1; i < vSliders.size(); i++) {
						// find maximal weight
						if (((Slider) vSliders.elementAt(i)).getValue() > value) {
							value = (float) ((Slider) vSliders.elementAt(i)).getValue();
							index = i;
						}
					}
					value /= 1.2f; // decrease maximum weight by 20%
					if (value < 0.0f) {
						value = 0.0f;
					}
					this.updateWeights(index, value);
				} else if (b.getActionCommand().equalsIgnoreCase("lessTradeoff")) {
					float value = (float) ((Slider) vSliders.firstElement()).getValue();
					int index = 0;
					for (int i = 1; i < vSliders.size(); i++) {
						// find maximal weight
						if (((Slider) vSliders.elementAt(i)).getValue() > value) {
							value = (float) ((Slider) vSliders.elementAt(i)).getValue();
							index = i;
						}
					}
					value *= 1.2f; // increase maximum weight by 20%
					if (value > 1.0f) {
						value = 1.0f;
					}
					this.updateWeights(index, value);
				}

				// for all order weight changes do this:
				this.updateMeasures();
				if (cp != null) {
					this.updateDataTable();
				}
			}
		}

		// event was dragging a slider
		else if (ae.getSource() instanceof Slider) {
			int n = vSliders.size();
			int index = -1;
			Slider sl;
			float value = Float.NaN;
			for (int i = 0; i < n; i++) {
				if (vSliders.elementAt(i) == ae.getSource()) {
					sl = (Slider) ae.getSource();
					index = i;
					value = (float) sl.getValue();
					break;
				}
			}

			this.updateWeights(index, value);
			this.updateMeasures();
			if (cp != null) {
				this.updateDataTable();
			}
		}

		// event from criterion weight panel
		else if (ae.getSource() == cp) {
			if (ae.getActionCommand().equals("fnChanged")) {
				// following text: "Sorry, adding/removing attributes currently not supported in OWA"
				System.out.println("Sorry");
			} else { // "weightsChanged" - just recompute !
			}
			this.updateDataTable();
		}

		// event from parallel coordinate plot in criterion weight panel
		else if (ae.getSource() == pcpl) {
			int dragged = pcpl.getDragged();
			int draggedTo = pcpl.getDraggedTo();
			float groupBreak = pcpl.getGroupBreak();

			if (draggedTo > groupBreak) {
				orderAfterScore = !orderAfterScore;
			} else {
				cp.fnReordered(dragged, draggedTo);
				Vector attr = cp.getAttributes();
				if (attr == null) {
					fn = null;
				} else {
					fn = new int[attr.size()];
					for (int i = 0; i < attr.size(); i++) {
						fn[i] = dTable.getAttrIndex((String) attr.elementAt(i));
					}
				}
			}
			setWeightsInPcp();
		}

	}

	/**
	 *
	 */
	private void updateDataTable() {
		// recomputing columns
		int column = dTable.getAttrIndex(AttrIDscore);
		float vals[] = compute();

		//for (int i=0; i<dTable.getDataItemCount(); i++)
		//  dTable.getDataRecord(i).setNumericAttrValue(vals[i],column);
		dTable.setNumericAttributeValues(vals, column);

		int order[] = TableStat.getOrderOfColumn(dTable, column);
		column = dTable.getAttrIndex(AttrIDorder);

		for (int i = 0; i < dTable.getDataItemCount(); i++) {
			//  dTable.getDataRecord(i).setNumericAttrValue(order[i],column);
			dTable.getDataRecord(i).setAttrValue((order[i] == -1) ? "" : String.valueOf(order[i]), column);
		}

		//dTable.setNumericAttributeValues(order,column);

		// set weights in parallel coordinate plot
		setWeightsInPcp();

		// inform all displays about change of values
		Vector attr = new Vector(2, 1);
		attr.addElement(AttrIDscore);
		attr.addElement(AttrIDorder);
		dTable.notifyPropertyChange("values", null, attr);
	}

	/**
	 *
	 */
	private void setWeightsInPcp() {
		float w[] = cp.getWeights(), weights[] = new float[w.length + 2];
		boolean isMax[] = cp.getIsMax();

		for (int i = 0; i < w.length; i++) {
			weights[i] = w[i] * ((isMax[i]) ? 1 : -1);
		}
		if (orderAfterScore) {
			weights[w.length] = 1;
			weights[w.length + 1] = -1;
		} else {
			weights[w.length] = -1;
			weights[w.length + 1] = 1;
		}
		pcp.setWeights(weights);
	}

	/**
	 * This is the actual implementation of the Ordered Weighted Averaging method
	 */
	private float[] compute() {
		if (tStat == null) {
			tStat = new TableStat();
			tStat.setDataTable(dTable);
		}

		boolean isMax[] = cp.getIsMax();
		float W[] = cp.getWeights(); // criterion weights from control panel
		float value = Float.NaN; // temporary storage for normalized (weighted) criterion outcome
		Vector oValue; // temporary storage for ordered weighted criterion outcomes

		if (score == null) {
			score = new float[dTable.getDataItemCount()];
		}

		java.io.BufferedWriter log = null;

// start verifier output
/*
try {
log = new java.io.BufferedWriter(new java.io.FileWriter("cgis-owa-verifier.csv"));
log.write("importance weights:\n");
for (int i=0; i<fn.length; i++) {
log.write(W[i] + ";");
}
log.write("\n\n");
log.write("order weights:\n");
for (int i=0; i<fn.length; i++) {
log.write(((Slider) vSliders.elementAt(i)).getValue() + ";");
}
log.write("\n\n");
log.write(dTable.getDataItemCount() + " alternatives, " + fn.length + " criteria (maximize?");
for (int i=0; i<fn.length; i++) log.write(" " + isMax[i]);
log.write(")\n\n");
log.write("raw values - normalized values - weighted ordered values\n\n");
} catch (Exception e) { }
*/

		// go through list of 'decision alternatives' (geo-objects)
		for (int n = 0; n < dTable.getDataItemCount(); n++) {

			oValue = new Vector();

			// go through 'decision criteria' (data fields, attributes)

// verifier output of raw values
/*
if (log != null) try {
for (int i=0; i<fn.length; i++) {
log.write(dTable.getNumericAttrValue(fn[i],n) + ";");
}
log.write("\n");
} catch (Exception e) { }
*/

			for (int i = 0; i < fn.length; i++) {

				// normalize criterion outcome
				try {
					if (isMax[i]) {
						value = (float) ((dTable.getNumericAttrValue(fn[i], n) - tStat.getMin(fn[i])) / (tStat.getMax(fn[i]) - tStat.getMin(fn[i])));
					} else {
						value = (float) ((tStat.getMax(fn[i]) - dTable.getNumericAttrValue(fn[i], n)) / (tStat.getMax(fn[i]) - tStat.getMin(fn[i])));
					}
				} catch (ArithmeticException ae) {
					System.out.println("* " + ae);
					value = 0.0f;
				}

// verifier output of normalized values
/*
if (log!=null) try {
log.write(value + ";");
} catch (Exception e) { }
*/

				// multiply by criterion weight
				value *= W[i];

				// order the weighted values
				boolean inserted = false;
				for (int j = 0; j < i; j++) {
					// if value is greater than any order value, insert before that one
					if (value > ((Float) oValue.elementAt(j)).floatValue()) {
						oValue.insertElementAt(new Float(value), j);
						inserted = true;
						break;
					}
				}
				if (!inserted) {
					oValue.addElement(new Float(value));
				}
			}

// verifier output of ordered weighted values
/*
if (log!=null) try {
log.write("\n");
for (int k=0; k<fn.length; k++) {
log.write(((Float) oValue.elementAt(k)).floatValue() + ";");
}
log.write("\n");
} catch (Exception e) { }
*/

			// multiply ordered weighted values by order weights
			score[n] = 0.0f;
			for (int k = 0; k < fn.length; k++) {
				score[n] += ((Slider) vSliders.elementAt(k)).getValue() * ((Float) oValue.elementAt(k)).floatValue();
			}

// verifier output of score
/*
if (log!=null) try {
log.write("score = " + score[n] + "\n\n");
} catch (Exception e) { }
*/

		}

// end of verifier output
/*
if (log!=null) try {
log.flush();
log.close();
} catch (Exception e) { }
*/

		return score;
	}

	/**
	 * Updates the measures after a change of order weights
	 */
	private void updateMeasures() {
		int n = vSliders.size();
		Slider sl;
		float orness = 0.0f;
		float tradeoff = 0.0f;
		float dispersion = 0.0f;
		float v;

		// start calculation of risk, orness, dispersion with temporary storage
		for (int i = 0; i < n; i++) {
			v = (float) ((Slider) vSliders.elementAt(i)).getValue();
			orness += (n - (i + 1)) * v;
			tradeoff += (v - (1.0f / n)) * (v - (1.0f / n));
			if (v > 0.0f) {
				dispersion += v * Math.log(v);
			}
		}

		// finish calculations:
		// Orness according to Yager 1988, p.187
		orness = orness / (n - 1.0f);
		// Tradeoff according to Jiang & Eastman 2000, p.179
		tradeoff = 1.0f - (float) Math.sqrt((n / (n - 1.0f)) * tradeoff);
		// Dispersion according to Yager 1988, p.188, normalized by ln(n)
		dispersion = -dispersion / (float) Math.log(n);

		// set text field to new values
		tfOrness.setText(StringUtil.floatToStr(orness, 2));
		tfTradeoff.setText(StringUtil.floatToStr(tradeoff, 2));
		tfDispersion.setText("" + dispersion);

		// repaint triangle canvas
		cnvImage.repaint();
	}

	/**
	 * Updates sliders after change by user
	 * @param index The index of the slider that has been modified by the user
	 * @param value The new value of the modified slider
	 */
	private void updateWeights(int index, float value) {
		int n = vSliders.size();
		Slider sl;
		Label l;

		// check correct index
		if ((index < 0) || (index >= n))
			return;

		// set value of changed slider and build new sum for remaining sliders
		l = (Label) vWeights.elementAt(index);
		l.setText(StringUtil.floatToStr(value, 2));
		sl = (Slider) vSliders.elementAt(index);
		sl.setValue(value);
		float newSum = 1.0f - value;

		// build current sum of remaining sliders
		float oldSum = 0.0f;
		for (int i = 0; i < n; i++) {
			if (i != index) {
				oldSum += ((Slider) vSliders.elementAt(i)).getValue();
			}
		}

		// adjust remaining sliders by multiplying with newSum/oldSum
		for (int i = 0; i < n; i++)
			if (i != index) {
				sl = (Slider) vSliders.elementAt(i);
				float val = (newSum == 0.0f) ? 0.0f : ((oldSum == 0.0f) ? (1.0f / (n - 1)) : (float) (sl.getValue() * newSum / oldSum));
				sl.setValue(val);
				l = (Label) vWeights.elementAt(i);
				l.setText(StringUtil.floatToStr(val, 2));
			}
	}

	/*
	 *  set random order weights
	 */
	private void setWeightsRandomly() {
		int n = vSliders.size();
		float[] weights = new float[n];
		float sum = 0.0f;

		Slider sl;
		Label l;

		// set weights ot random number [0..1]
		for (int i = 0; i < n; i++) {
			weights[i] = (float) Math.random();
			sum += weights[i];
		}

		// adjust to have sum = 1.0
		for (int i = 0; i < n; i++) {
			sl = (Slider) vSliders.elementAt(i);
			sl.setValue(weights[i] / sum);
			l = (Label) vWeights.elementAt(i);
			l.setText(StringUtil.floatToStr(weights[i] / sum, 2));
		}
	}

	/**
	 *  Set order weights to (1.0, 0.0, ..., 0.0)
	 */
	private void setWeightsToOr() {
		int n = vSliders.size();
		Slider sl;
		Label l;

		// set first order weight to 1.0
		sl = (Slider) vSliders.firstElement();
		sl.setValue(1.0f);
		l = (Label) vWeights.firstElement();
		l.setText(StringUtil.floatToStr(1.0f, 2));

		// set remaining order weights to 0.0
		for (int i = 1; i < n; i++) {
			sl = (Slider) vSliders.elementAt(i);
			sl.setValue(0.0f);
			l = (Label) vWeights.elementAt(i);
			l.setText(StringUtil.floatToStr(0.0f, 2));
		}
	}

	/**
	 *  Set order weights to (1/n, ..., 1/n)
	 */
	private void setWeightsToWLC() {
		int n = vSliders.size();
		Slider sl;
		Label l;

		// set all order weights to 1/n
		for (int i = 0; i < n; i++) {
			sl = (Slider) vSliders.elementAt(i);
			sl.setValue(1.0f / n);
			l = (Label) vWeights.elementAt(i);
			l.setText(StringUtil.floatToStr(1.0f / n, 2));
		}
	}

	/**
	 *  Set order weights to (0.0, ..., 0.0, 1.0)
	 */
	private void setWeightsToAnd() {
		int n = vSliders.size();
		Slider sl;
		Label l;

		// set all but last order weights to 0.0
		for (int i = 0; i < (n - 1); i++) {
			sl = (Slider) vSliders.elementAt(i);
			sl.setValue(0.0f);
			l = (Label) vWeights.elementAt(i);
			l.setText(StringUtil.floatToStr(0.0f, 2));
		}

		// set last order weights to 1.0
		sl = (Slider) vSliders.lastElement();
		sl.setValue(1.0f);
		l = (Label) vWeights.lastElement();
		l.setText(StringUtil.floatToStr(1.0f, 2));
	}

	/**
	* If there was an error in computation, returns the error message
	* However, a CalcDLG itself displays error messages, if any.
	* Therefore this method returns null.
	*/
	@Override
	public String getErrorMessage() {
		// following text:  "Error in Ordered Weighted Averaging method"

		return res.getString("ErrorOWA");
	}

	/**
	 * Return a short explanation of the OWA aggregation operator
	 */
	@Override
	public String getExplanation() {
		return strExplanation;
	}

	/**
	 * Return a prompt ...
	 */
	@Override
	public String getAttributeSelectionPrompt() {
		return strPrompt;
	}

}
