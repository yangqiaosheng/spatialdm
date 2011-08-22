package spade.analysis.calc;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.generators.PCPGenerator;
import spade.analysis.plot.ObjectList;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.PopupManager;
import spade.lib.basicwin.SplitLayout;
import spade.lib.lang.Language;
import spade.vis.action.HighlightListener;
import spade.vis.action.Highlighter;
import spade.vis.action.ObjectEvent;
import spade.vis.database.AttrTransform;
import spade.vis.database.AttributeTypes;
import spade.vis.database.TableStat;
import spade.vis.event.DMouseEvent;
import spade.vis.map.MapViewer;

public class SimilarityCalc extends CalcDlg implements ActionListener, ItemListener, HighlightListener {

	static ResourceBundle res = Language.getTextResource("spade.analysis.calc.Res");
	/*following text: "Computes the degree of similarity (distance) "+
	* "of each object (characterised by one table row) to a selected object or "+
	* "a group of objects. The distance is defined in terms of values of some "+
	* "selected attributes."*/
	public static final String expl = res.getString("Computes_the_degree") + res.getString("of_each_object") + res.getString("agroup_of_objects_The") + res.getString("selected_attributes_");

	/*following text: "Select at least two numeric attributes "+
	 * "for the estimation of the similarity between objects."*/
	public static final String prompt = res.getString("Select_at_least_two") + res.getString("for_the_estimation_of");

	public String metricNames[] = { "L1", "L2", "C", "T" };

	/* following text: "L1.\nDist(A,B)=Sum(|Ai-Bi|)",
	 *               "L2.\nDist(A,B)=Sqrt(Sum((Ai-Bi)^2))",
	 *                "C.\nDist(A,B)=Max(|Ai-Bi|)",
	 *               "Special metric for time series data.\nIt reflects number of time moments\nwith similar change of values.\n\nDo not use with non-temporal data !"*/
	public String metricToolTips[] = { "L1.\nDist(A,B)=Sum(|Ai-Bi|)", "L2.\nDist(A,B)=Sqrt(Sum((Ai-Bi)^2))", "C.\nDist(A,B)=Max(|Ai-Bi|)", res.getString("Special_metric_for") };
	private Checkbox cbMethod[] = null, cbMetric[] = new Checkbox[4];
	private Label lName = null;
	private ObjectList ol = null;
	private Button bContinue = null;

	/**
	* reference objects used for calculations
	*/
	protected Vector referenceObjects = null;

	public Vector getReferenceObjects() {
		return referenceObjects;
	}

	/**
	* ID of the computed attribute
	*/
	protected String AttrID = null;

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
	* Constructs the dialog appearance
	*/
	@Override
	public void makeInterface() {
		// following text: "Similarity in attribute space"
		setTitle(res.getString("Sias"));
		if (supervisor != null && dTable != null) {
			supervisor.registerHighlightListener(this, dTable.getEntitySetIdentifier());
		}
		initFirstStage();
	}

	public void initFirstStage() { // make UI for selecting reference objects
		setLayout(new BorderLayout());

		// following text: ("North",new Label("Select reference object(s)",Label.CENTER))
		add("North", new Label(res.getString("Srob"), Label.CENTER));
		ol = new ObjectList();
		ol.construct(supervisor, 10, dTable);
		add(ol, "Center");

		Panel p = new Panel();
		add("South", p);
		p.setLayout(new ColumnLayout());
		p.add(new Line(false));

		// following text: "Attributes used in calculations:"
		p.add(new Label(res.getString("Auic"), Label.CENTER));
		if (fn.length <= 5) {
			for (int element : fn) {
				p.add(new Label(dTable.getAttributeName(element), Label.LEFT));
			}
			p.add(new Line(false));
		} else {
			Panel pp = new Panel(new ColumnLayout());
			for (int element : fn) {
				pp.add(new Label(dTable.getAttributeName(element), Label.LEFT));
			}
			ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
			scp.add(pp);
			p.add(scp);
		}

		Panel pp = new Panel();
		pp.setLayout(new FlowLayout());
		// following text: "Continue"
		bContinue = new Button(res.getString("Continue"));
		pp.add(bContinue);
		bContinue.setActionCommand("Ready");
		bContinue.addActionListener(this);
		// following text: "Close"
		Button b = new Button(res.getString("Close"));
		pp.add(b);
		b.setActionCommand("close");
		b.addActionListener(this);
		p.add(pp);

		Highlighter highlighter = supervisor.getHighlighter(dTable.getEntitySetIdentifier());
		bContinue.setEnabled(highlighter.getSelectedObjects() != null && highlighter.getSelectedObjects().size() > 0);

		pack();
	}

	@Override
	public void highlightSetChanged(Object source, String setId, Vector highlighted) {
	}

	@Override
	public void selectSetChanged(Object source, String setId, Vector selected) {
		if (referenceObjects != null)
			return; // we are at the second stage already !!!
		Highlighter highlighter = supervisor.getHighlighter(dTable.getEntitySetIdentifier());
		bContinue.setEnabled(highlighter.getSelectedObjects() != null && highlighter.getSelectedObjects().size() > 0);
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		//al.actionPerformed(new ActionEvent(this,0,this.compute));
		// recomputing columns
		int column = dTable.getAttrIndex(getAttrID());
		float vals[] = compute();
		dTable.setNumericAttributeValues(vals, column);
		Vector attr = new Vector(1, 1);
		attr.addElement(getAttrID());
		// inform all displays about change of values
		dTable.notifyPropertyChange("values", null, attr);
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals("close")) {
			dispose();
			return;
		}
		if (ae.getSource() instanceof Button && ae.getActionCommand().equals("Ready")) {
			Highlighter highlighter = supervisor.getHighlighter(dTable.getEntitySetIdentifier());
			if (highlighter.getSelectedObjects() != null && highlighter.getSelectedObjects().size() > 0) {
				highlighter.removeHighlightListener(this);
				ol.destroy();
				referenceObjects = new Vector(highlighter.getSelectedObjects().size(), 10);
				for (int i = 0; i < highlighter.getSelectedObjects().size(); i++) {
					referenceObjects.addElement(highlighter.getSelectedObjects().elementAt(i));
				}
				initSecondStage();
			}
		}
	}

	protected SplitLayout splL = null;

	public void initSecondStage() { // make UI for setting up parameters of calculations
		removeAll();

		setLayout(new BorderLayout());
		Panel mainP = new Panel();
		add(mainP, "Center");

		mainP.setLayout(splL = new SplitLayout(mainP, SplitLayout.VERT));

		Panel cp = new Panel(); // Control Panel
		splL.addComponent(cp, 0.4f);

		cp.setLayout(new BorderLayout());

		Panel p = new Panel();
		cp.add("South", p);
		p.setLayout(new ColumnLayout());

		// following text: "Distance to "
		p.add(lName = new Label(res.getString("Distance_to "), Label.CENTER));

		p.add(new Line(false));

		if (referenceObjects.size() > 1) {
			CheckboxGroup cbg = new CheckboxGroup();
			// following text:  "How to compute the distance to the group ?"
			p.add(new Label(res.getString("Htctd"), Label.CENTER));
			cbMethod = new Checkbox[3];
			// following text: "Distance to the mass center of the group"
			p.add(cbMethod[0] = new Checkbox(res.getString("Dttmc"), false, cbg));
			// following text: "Minimal distance to group members"
			p.add(cbMethod[1] = new Checkbox(res.getString("Mdtgm"), true, cbg));
			// following text: "Average distance to group members"
			p.add(cbMethod[2] = new Checkbox(res.getString("Adtgm"), false, cbg));
			p.add(new Line(false));
		}

		Panel pp = new Panel();
		pp.setLayout(new FlowLayout());
		// following text: "Metric:"
		pp.add(new Label(res.getString("Metric:"), Label.CENTER));
		CheckboxGroup cbg = new CheckboxGroup();
		for (int i = 0; i < cbMetric.length; i++) { // T is not in use yet ... only fot time !
			pp.add(cbMetric[i] = new Checkbox(metricNames[i], i == 0, cbg));
			new PopupManager(cbMetric[i], metricToolTips[i], true);
		}
		p.add(pp);

		p.add(new Line(false));

		// following text: "Reference object(s)"
		p.add(new Label(res.getString("Robj"), Label.CENTER));
		for (int i = 0; i < referenceObjects.size(); i++) {
			String ID = (String) referenceObjects.elementAt(i), name = dTable.getDataItemName(dTable.getObjectIndex(ID));
			p.add(new Label(name, Label.LEFT));
			lName.setText(lName.getText() + ((i == 0) ? "" : "+") + /*ID*/name);
		}

		p.add(new Line(false));

		// following text: "Attributes used in calculations:"
		p.add(new Label(res.getString("Auic"), Label.CENTER));
		if (fn.length <= 5) {
			for (int element : fn) {
				p.add(new Label(dTable.getAttributeName(element), Label.LEFT));
			}
			p.add(new Line(false));
		} else {
			pp = new Panel(new ColumnLayout());
			for (int element : fn) {
				pp.add(new Label(dTable.getAttributeName(element), Label.LEFT));
			}
			ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
			scp.add(pp);
			p.add(scp);
		}

		pp = new Panel();
		pp.setLayout(new FlowLayout());
		// following text: "Close"
		Button b = new Button(res.getString("Close"));
		pp.add(b);
		b.setActionCommand("close");
		b.addActionListener(this);
		p.add(pp);

		if (cbMethod != null) {
			for (Checkbox element : cbMethod) {
				element.addItemListener(this);
			}
		}
		for (Checkbox element : cbMetric) {
			element.addItemListener(this);
		}

		//al.actionPerformed(new ActionEvent(this,0,this.compute,1));
		calculateSimilarity();
	}

	public void continueSecondStage(java.awt.Component c) {
		splL.addComponent(c, 0.6f);
		pack();
	}

	@Override
	public void dispose() {
		super.dispose();
		supervisor.removeHighlightListener(this, dTable.getEntitySetIdentifier());
		if (ol != null) {
			ol.destroy();
		}
	}

	public String getAttrName() {
		return lName.getText();
	}

	public int getMetric() {
		for (int i = 0; i < cbMetric.length; i++)
			if (cbMetric[i].getState())
				return (i + 1);
		return 0;
	}

	public int getMethod() {
		if (cbMethod == null)
			return 2;
		for (int i = 0; i < cbMethod.length; i++)
			if (cbMethod[i].getState())
				return (i + 1);
		return 0;
	}

	protected int sign(float d) {
		if (d > 0.001f)
			return 1;
		else if (d < -0.001f)
			return -1;
		else
			return 0;
	}

	protected int sign(double d) {
		if (d > 0.001f)
			return 1;
		else if (d < -0.001f)
			return -1;
		else
			return 0;
	}

	protected double distance(int recn, int refrecn) {
		double dist = 0f;
		if (getMetric() == 4) { // time !
			int n = 0;
			for (int j = 0; j < fn.length - 1; j++) {
				double dj = dTable.getNumericAttrValue(fn[j], recn), dj1 = dTable.getNumericAttrValue(fn[j + 1], recn), djr = dTable.getNumericAttrValue(fn[j], refrecn), djr1 = dTable.getNumericAttrValue(fn[j + 1], refrecn);
				if (!Double.isNaN(dj) && !Double.isNaN(dj1) && !Double.isNaN(djr) && !Double.isNaN(djr1)) {
					n++;
					double d = dj1 - dj, dr = djr1 - djr;
					dist += Math.abs(sign(d) - sign(dr));
				}
			}
			if (n > 0) {
				dist /= n;
			} else {
				dist = Double.NaN;
			}
			return dist;
		}
		for (int j = 0; j < fn.length; j++) {
			double d = Math.abs(aTransf.value(dTable.getNumericAttrValue(fn[j], recn), j, 3) - 0.5f);
			switch (getMetric()) {
			case 1: /* L1 */
				dist += d;
				break;
			case 2: /* L2 */
				dist += d * d;
				break;
			case 3: /* C  */
				if (dist < d) {
					dist = d;
				}
				break;
			}
		}
		switch (getMetric()) {
		case 1: // L1
		case 3: // C
			break;
		case 2: // L2
			dist = Math.sqrt(dist);
			break;
		}
		return dist;
	}

	@Override
	protected void start() {
		if (supervisor == null || supervisor.getUI() == null)
			return;
		MapViewer mapView = supervisor.getUI().getCurrentMapViewer();
		if (mapView != null) {
			mapView.getMapEventMeaningManager().setCurrentEventMeaning(DMouseEvent.mDrag, "select");
		}
		supervisor.getObjectEventMeaningManager().setCurrentEventMeaning(ObjectEvent.click, "highlight");
	}

	protected void calculateSimilarity() {
		// computing new column
		float vals[] = compute();
		// adding the column to the table
		Vector sourceAttrs = new Vector(fn.length, 5);
		for (int element : fn) {
			sourceAttrs.addElement(dTable.getAttributeId(element));
		}
		int attrIdx = dTable.addDerivedAttribute(lName.getText(), AttributeTypes.real, AttributeTypes.distance, sourceAttrs);
		dTable.setNumericAttributeValues(vals, attrIdx);
		AttrID = dTable.getAttributeId(attrIdx);
		Vector resultAttrs = new Vector(1, 5);
		resultAttrs.addElement(AttrID);
		//add attribute dependency and notify about new attribute
		attrAddedToTable(resultAttrs);
		// show results of calculations on the map
		tryShowOnMap(resultAttrs, null, true);
		PCPGenerator vg = new PCPGenerator();
		Highlighter highlighter = supervisor.getHighlighter(dTable.getEntitySetIdentifier());
		Component c = vg.constructDisplaySCalc(supervisor, dTable, getInvolvedAttrs(), (highlighter == null) ? null : highlighter.getSelectedObjects());
		supervisor.notifyGlobalPropertyChange(Supervisor.eventDisplayedAttrs);
		continueSecondStage(c);
	}

	public float[] compute() {
		if (tStat == null) {
			tStat = new TableStat();
			tStat.setDataTable(dTable);
			aTransf = new AttrTransform(dTable, tStat, fn);
		}
		float vals[] = new float[dTable.getDataItemCount()];
		if (getMethod() == 1) {
			tStat.ComputeMaxRefValRatio(referenceObjects, fn);
			aTransf.setRefVal(tStat.getRefVal(), tStat.getMaxRefValRatio());
			for (int i = 0; i < dTable.getDataItemCount(); i++) {
				vals[i] = (float) distance(i, -1);
			}
		} else {
			for (int i = 0; i < vals.length; i++) {
				vals[i] = Float.NaN;
			}
			for (int n = 0; n < referenceObjects.size(); n++) {
				String id = (String) referenceObjects.elementAt(n);
				int recn = dTable.indexOf(id);
				if (recn < 0) {
					System.out.println("* record expected but not found, id=<" + id + ">");
					continue;
				}
				tStat.ComputeMaxRefValRatio(recn, fn);
				aTransf.setRefVal(tStat.getRefVal(), tStat.getMaxRefValRatio());
				for (int i = 0; i < vals.length; i++) {
					float d = (float) distance(i, recn);
					if (Float.isNaN(d)) {
						continue;
					}
					if (getMethod() == 2) {
						if (Float.isNaN(vals[i]) || vals[i] > d) {
							vals[i] = d;
						}
					} else { // method==3
						if (Float.isNaN(vals[i])) {
							vals[i] = d;
						} else {
							vals[i] += d;
						}
					}
				}
			}
			if (getMethod() == 3) {
				for (int i = 0; i < vals.length; i++)
					if (!Float.isNaN(vals[i])) {
						vals[i] /= referenceObjects.size();
					}
			}
			tStat.ComputeMaxRefValRatio(referenceObjects, fn);
			aTransf.setRefVal(tStat.getRefVal(), tStat.getMaxRefValRatio());
		}
		return vals;
	}

	public String getAttrID() {
		return AttrID;
	}

	public Vector getInvolvedAttrs() {
		Vector v = new Vector(fn.length + 1);
		for (int element : fn) {
			v.addElement(new String(dTable.getAttributeId(element)));
		}
		v.addElement(new String(AttrID));
		return v;
	}

	/**
	* Returns an explanation about this calculation method
	*/
	@Override
	public String getExplanation() {
		return expl;
	}

	/**
	* Returns an instruction for the user about what and how many attributes to
	* select.
	*/
	@Override
	public String getAttributeSelectionPrompt() {
		return prompt;
	}
}