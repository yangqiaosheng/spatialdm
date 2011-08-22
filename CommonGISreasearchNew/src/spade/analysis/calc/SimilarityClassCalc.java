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
import spade.lib.basicwin.Slider;
import spade.lib.basicwin.SplitLayout;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;
import spade.vis.action.HighlightListener;
import spade.vis.action.Highlighter;
import spade.vis.action.ObjectEvent;
import spade.vis.database.AttrTransform;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.TableStat;
import spade.vis.event.DMouseEvent;
import spade.vis.map.MapViewer;

public class SimilarityClassCalc extends CalcDlg implements ActionListener, ItemListener, HighlightListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.calc.Res");
	/* following text: "Determines for each object (characterised by "+
	* "one table row) to which of two selected objects or groups of objects it "+
	* "is closer (more similar). The distance is defined in terms of values of some "+
	* "selected attributes."*/
	public static final String expl = res.getString("Determines_for_each") + res.getString("one_table_row_to") + res.getString("is_closer_more") + res.getString("selected_attributes_");

	/* following text: "Select at least two numeric attributes "+
	 * "for the estimation of the similatity between objects."*/
	public static final String prompt = res.getString("Select_at_least_two") + res.getString("for_the_estimation_of2");

	public String metricNames[] = { "L1", "L2", "C", "T" };
	/*following text: "L1.\nDist(A,B)=Sum(|Ai-Bi|)",
	                 "L2.\nDist(A,B)=Sqrt(Sum((Ai-Bi)^2))",
	                 "C.\nDist(A,B)=Max(|Ai-Bi|)",
	                 "Special metric for time series data.\nIt reflects number of time moments\nwith similar change of values.\n\nDo not use with non-temporal data !"*/
	public String metricToolTips[] = { "L1.\nDist(A,B)=Sum(|Ai-Bi|)", "L2.\nDist(A,B)=Sqrt(Sum((Ai-Bi)^2))", "C.\nDist(A,B)=Max(|Ai-Bi|)", res.getString("Special_metric_for2") };

	private Checkbox cbMethod[] = null, cbMetric[] = new Checkbox[4];
	private Label lName = null;
	private ObjectList ol = null;
	private Button bContinue = null;
	private Slider slTreshold = null;
	private Label lTreshold = null;

	/**
	* reference objects used for calculations
	*/
	protected Vector referenceObjects1 = null, referenceObjects2 = null;

	public Vector getReferenceObjects(int n) {
		return (n == 1) ? referenceObjects1 : referenceObjects2;
	}

	/**
	* IDs of the computed attributes
	*/
	protected String Dist1ID = null, Dist2ID = null, AttrID = null;

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
		// following text: "Classification by similarity"
		setTitle(res.getString("Cbs"));
		if (supervisor != null && dTable != null) {
			supervisor.registerHighlightListener(this, dTable.getEntitySetIdentifier());
		}
		initFirstStage();
	}

	public void initFirstStage() { // make UI for selecting reference objects
		setLayout(new BorderLayout());

		// following text: ("North",new Label("Select 1st class sample(s)",Label.CENTER)
		add("North", new Label(res.getString("Sfcs"), Label.CENTER));
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
		// following text:  "Continue"
		bContinue = new Button(res.getString("Continue"));
		pp.add(bContinue);
		bContinue.setActionCommand("Ready1");
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
		if (referenceObjects2 != null)
			return; // we are at the third stage already !!!
		Highlighter highlighter = supervisor.getHighlighter(dTable.getEntitySetIdentifier());
		bContinue.setEnabled(highlighter.getSelectedObjects() != null && highlighter.getSelectedObjects().size() > 0);
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		//al.actionPerformed(new ActionEvent(this,0,this.compute));
		// recomputing columns
		int column1 = dTable.getAttrIndex(getDist1ID()), column2 = dTable.getAttrIndex(getDist2ID()), column3 = dTable.getAttrIndex(getAttrID());
		int classes[] = compute();
		double dist1[] = getDistances(1), dist2[] = getDistances(2);
		String attrVals[] = dTable.getAttribute(column3).getValueList();
		dTable.setNumericAttributeValues(dist1, column1);
		dTable.setNumericAttributeValues(dist2, column2);
		for (int i = 0; i < dTable.getDataItemCount(); i++) {
			dTable.getDataRecord(i).setAttrValue(attrVals[1 + classes[i]], column3);
		}
		Vector attr = new Vector(3, 1);
		attr.addElement(getDist1ID());
		attr.addElement(getDist2ID());
		attr.addElement(getAttrID());
		// inform all displays about change of values
		dTable.notifyPropertyChange("values", null, attr);
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand() != null && ae.getActionCommand().equals("close")) {
			dispose();
			return;
		}
		Highlighter highlighter = supervisor.getHighlighter(dTable.getEntitySetIdentifier());
		if (ae.getSource() instanceof Button && ae.getActionCommand().equals("Ready1"))
			if (highlighter.getSelectedObjects() != null && highlighter.getSelectedObjects().size() > 0) {
				ol.destroy();
				referenceObjects1 = new Vector(highlighter.getSelectedObjects().size(), 10);
				for (int i = 0; i < highlighter.getSelectedObjects().size(); i++) {
					referenceObjects1.addElement(highlighter.getSelectedObjects().elementAt(i));
				}
				initSecondStage();
			}
		if (ae.getSource() instanceof Button && ae.getActionCommand().equals("Ready2"))
			if (highlighter.getSelectedObjects() != null && highlighter.getSelectedObjects().size() > 0) {
				highlighter.removeHighlightListener(this);
				ol.destroy();
				referenceObjects2 = new Vector(highlighter.getSelectedObjects().size(), 10);
				for (int i = 0; i < highlighter.getSelectedObjects().size(); i++) {
					referenceObjects2.addElement(highlighter.getSelectedObjects().elementAt(i));
				}
				initThirdStage();
			}
		if (slTreshold != null && ae.getSource() == slTreshold) {
			if (slTreshold.getValue() < 0.01)
				return;
			lTreshold.setText(" " + StringUtil.doubleToStr(slTreshold.getValue(), slTreshold.getAbsMin(), slTreshold.getAbsMax()));
			// recomputing columns
			int column3 = dTable.getAttrIndex(getAttrID());
			int classes[] = compute(false);
			String attrVals[] = dTable.getAttribute(column3).getValueList();
			for (int i = 0; i < dTable.getDataItemCount(); i++) {
				dTable.getDataRecord(i).setAttrValue(attrVals[1 + classes[i]], column3);
			}
			Vector attr = new Vector(1, 1);
			attr.addElement(getAttrID());
			// inform all displays about change of values
			dTable.notifyPropertyChange("values", null, attr);
		}
	}

	public void initSecondStage() { // make UI for setting up parameters of calculations
		Highlighter highlighter = supervisor.getHighlighter(dTable.getEntitySetIdentifier());
		highlighter.clearSelection(this);

		removeAll();
		setLayout(new BorderLayout());

		Panel pp = new Panel();
		pp.setLayout(new ColumnLayout());
		// following text: "1st class samples:"
		pp.add(new Label(res.getString("fcs"), Label.CENTER));
		for (int i = 0; i < referenceObjects1.size(); i++) {
			String ID = (String) referenceObjects1.elementAt(i);
			String name = dTable.getDataItemName(dTable.getObjectIndex(ID));
			pp.add(new Label((name == null) ? ID : name, Label.LEFT));
		}
		pp.add(new Line(false));
		// following text:  "Select 2nd class sample(s)"
		pp.add(new Label(res.getString("S2cs"), Label.CENTER));

		add("North", pp);

		ol = new ObjectList();
		ol.construct(supervisor, 10, dTable);
		add(ol, "Center");

		Panel p = new Panel();
		// following text: "South"
		add("South", p);
		p.setLayout(new ColumnLayout());
		p.add(new Line(false));

		// following text:  "Attributes used in calculations:"
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
		// following text: "Continue"
		bContinue = new Button(res.getString("Continue"));
		pp.add(bContinue);
		bContinue.setActionCommand("Ready2");
		bContinue.addActionListener(this);
		// following text: "Close"
		Button b = new Button(res.getString("Close"));
		pp.add(b);
		b.setActionCommand("close");
		b.addActionListener(this);
		p.add(pp);

		bContinue.setEnabled(false);

		pack();
	}

	protected SplitLayout splL = null;

	public void initThirdStage() { // make UI for setting up parameters of calculations
		Highlighter highlighter = supervisor.getHighlighter(dTable.getEntitySetIdentifier());
		highlighter.clearSelection(this);
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

		// following text:  "Classification: "
		p.add(lName = new Label(res.getString("Classification: "), Label.CENTER));

		p.add(new Line(false));

		if (referenceObjects1.size() > 1 || referenceObjects2.size() > 1) {
			CheckboxGroup cbg = new CheckboxGroup();
			// following text:  "How to compute the distance to the group ?"
			p.add(new Label(res.getString("Htctdttg"), Label.CENTER));
			cbMethod = new Checkbox[3];
			// following text: "Distance to the mass center of the group"
			p.add(cbMethod[0] = new Checkbox(res.getString("Dttmcotg"), false, cbg));
			// following text:  "Minimal distance to group members"
			p.add(cbMethod[1] = new Checkbox(res.getString("Mdtgm"), true, cbg));
			// following text:  "Average distance to group mmbers"
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

		// following text:  "Distance threshold for classification:"
		p.add(new Label(res.getString("Dtfc"), Label.CENTER));
		pp = new Panel();
		pp.setLayout(new BorderLayout());
		pp.add(slTreshold = new Slider(this, 0f, 1f, 0.5f), "Center");
		slTreshold.setNAD(true);
		pp.add(lTreshold = new Label(" 0.500"/*StringUtil.floatToStr(0.5f,0f,1f)*/), "East");
		p.add(pp);
		p.add(new Line(false));

		// following text: "Samples"
		p.add(new Label(res.getString("Samples"), Label.CENTER));
		for (int i = 0; i < Math.max(referenceObjects1.size(), referenceObjects2.size()); i++) {
			pp = new Panel();
			pp.setLayout(new BorderLayout());
			String name = "";
			if (i < referenceObjects1.size()) {
				String ID = (String) referenceObjects1.elementAt(i);
				name = dTable.getDataItemName(dTable.getObjectIndex(ID));
			}
			pp.add(new Label(name, Label.LEFT), "West");
			name = "";
			if (i < referenceObjects2.size()) {
				String ID = (String) referenceObjects2.elementAt(i);
				name = dTable.getDataItemName(dTable.getObjectIndex(ID));
			}
			pp.add(new Label(name, Label.LEFT), "East");
			p.add(pp);
		}

		lName.setText(lName.getText() + getClassName(1) + " Vs. " + getClassName(2));

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
		// following text: "Close"
		pp.setLayout(new FlowLayout());
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
		calculateSimilarityClass();
	}

	public void continueThirdStage(java.awt.Component c) {
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

	public String getClassName(int n) {
		String str = "";
		if (n == 1) {
			for (int i = 0; i < referenceObjects1.size(); i++) {
				String ID = (String) referenceObjects1.elementAt(i), name = dTable.getDataItemName(dTable.getObjectIndex(ID));
				str += ((i == 0) ? "" : "+") + ((name == null) ? ID : name);
			}
		} else {
			for (int i = 0; i < referenceObjects2.size(); i++) {
				String ID = (String) referenceObjects2.elementAt(i), name = dTable.getDataItemName(dTable.getObjectIndex(ID));
				;
				str += ((i == 0) ? "" : "+") + ((name == null) ? ID : name);
			}
		}
		return str;
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

	protected void calculateSimilarityClass() {
		int classes[] = compute();
		// adding the columns to the table
		Vector sourceAttrs = new Vector(fn.length, 5);
		for (int element : fn) {
			sourceAttrs.addElement(dTable.getAttributeId(element));
		}
		//following text: "Distance to "
		int idx0 = dTable.addDerivedAttribute(res.getString("Distance_to ") + getClassName(1), AttributeTypes.real, AttributeTypes.distance, sourceAttrs);
		Dist1ID = dTable.getAttributeId(idx0);
		//following text: "Distance to "
		dTable.addDerivedAttribute(res.getString("Distance_to ") + getClassName(2), AttributeTypes.real, AttributeTypes.distance, sourceAttrs);
		Dist2ID = dTable.getAttributeId(idx0 + 1);
		Vector distAttr = new Vector(2, 2);
		distAttr.addElement(Dist1ID);
		distAttr.addElement(Dist2ID);
		dTable.addDerivedAttribute(getAttrName(), AttributeTypes.character, AttributeTypes.classify_similar, distAttr);
		AttrID = dTable.getAttributeId(idx0 + 2);
		// updating Attribute structure - begin
		Attribute attr = dTable.getAttribute(idx0 + 2);
		String valNames[] = new String[3];
		// following text:"Like "
		// following text:"Non classified"
		// following text:"Like "
		valNames[0] = (res.getString("Like ")) + getClassName(1);
		valNames[1] = (res.getString("Non_classified"));
		valNames[2] = (res.getString("Like ")) + getClassName(2);
		attr.setValueListAndColors(valNames, null);
		// updating Attribute structure - end
		Vector resultAttrs = new Vector(3, 5);
		resultAttrs.addElement(Dist1ID);
		resultAttrs.addElement(Dist2ID);
		resultAttrs.addElement(AttrID);
		//notify about new attribute
		attrAddedToTable(resultAttrs);
		// writing data to the table
		String attrVals[] = dTable.getAttribute(idx0 + 2).getValueList();
		dTable.setNumericAttributeValues(dist1, idx0);
		dTable.setNumericAttributeValues(dist2, idx0 + 1);
		for (int i = 0; i < dTable.getDataItemCount(); i++) {
			dTable.getDataRecord(i).setAttrValue(attrVals[1 + classes[i]], idx0 + 2);
		}
		// show results of calculations on the map
		Vector attrs = new Vector(1, 5);
		attrs.addElement(AttrID);
		tryShowOnMap(attrs, null, true);
		PCPGenerator vg = new PCPGenerator();
		Component c = vg.constructDisplaySCCalc(supervisor, dTable, getInvolvedAttrs(), getReferenceObjects(1), getReferenceObjects(2));
		supervisor.notifyGlobalPropertyChange(Supervisor.eventDisplayedAttrs);
		continueThirdStage(c);
	}

	protected double dist1[] = null, dist2[] = null;

	public double[] getDistances(int n) {
		return (n == 1) ? dist1 : dist2;
	}

	public int[] compute() {
		return compute(true);
	}

	public int[] compute(boolean toComputeDistances) {
		if (toComputeDistances) {
			computeDistances();
		}
		// find max distance to update slider parameters
		double max = 0;
		for (int i = 0; i < dist1.length; i++) {
			if (max < dist1[i]) {
				max = dist1[i];
			}
			if (max < dist2[i]) {
				max = dist2[i];
			}
		}
		double t = slTreshold.getValue();
		if (t >= max) {
			t = max;
		}
		slTreshold.setValues(0f, max, t);
		lTreshold.setText(" " + StringUtil.doubleToStr(slTreshold.getValue(), slTreshold.getAbsMin(), slTreshold.getAbsMax()));
		//
		int res[] = new int[dTable.getDataItemCount()];
		for (int i = 0; i < res.length; i++)
			if (dist1[i] < dist2[i] && dist1[i] < t) {
				res[i] = -1;
			} else if (dist2[i] < dist1[i] && dist2[i] < t) {
				res[i] = 1;
			} else {
				res[i] = 0;
			}
		return res;
	}

	public void computeDistances() {
		if (tStat == null) {
			tStat = new TableStat();
			tStat.setDataTable(dTable);
			aTransf = new AttrTransform(dTable, tStat, fn);
		}
		int len = dTable.getDataItemCount();
		dist1 = new double[len];
		dist2 = new double[len];
		if (getMethod() == 1) {
			tStat.ComputeMaxRefValRatio(referenceObjects1, fn);
			aTransf.setRefVal(tStat.getRefVal(), tStat.getMaxRefValRatio());
			for (int i = 0; i < dTable.getDataItemCount(); i++) {
				dist1[i] = distance(i, -1);
			}
			tStat.ComputeMaxRefValRatio(referenceObjects2, fn);
			aTransf.setRefVal(tStat.getRefVal(), tStat.getMaxRefValRatio());
			for (int i = 0; i < dTable.getDataItemCount(); i++) {
				dist2[i] = distance(i, -1);
			}
		} else {
			for (int i = 0; i < dist1.length; i++) {
				dist1[i] = Float.NaN;
				dist2[i] = Float.NaN;
			}
			for (int n = 0; n < referenceObjects1.size(); n++) {
				String id = (String) referenceObjects1.elementAt(n);
				int recn = dTable.indexOf(id);
				if (recn < 0) {
					System.out.println("* record expected but not found, id=<" + id + ">");
					continue;
				}
				tStat.ComputeMaxRefValRatio(recn, fn);
				aTransf.setRefVal(tStat.getRefVal(), tStat.getMaxRefValRatio());
				for (int i = 0; i < len; i++) {
					double d = distance(i, recn);
					if (Double.isNaN(d)) {
						continue;
					}
					if (getMethod() == 2) {
						if (Double.isNaN(dist1[i]) || dist1[i] > d) {
							dist1[i] = d;
						}
					} else { // method==3
						if (Double.isNaN(dist1[i])) {
							dist1[i] = d;
						} else {
							dist1[i] += d;
						}
					}
				}
			}
			if (getMethod() == 3) {
				for (int i = 0; i < len; i++)
					if (!Double.isNaN(dist1[i])) {
						dist1[i] /= referenceObjects1.size();
					}
			}
			for (int n = 0; n < referenceObjects2.size(); n++) {
				String id = (String) referenceObjects2.elementAt(n);
				int recn = dTable.indexOf(id);
				if (recn < 0) {
					System.out.println("* record expected but not found, id=<" + id + ">");
					continue;
				}
				tStat.ComputeMaxRefValRatio(recn, fn);
				aTransf.setRefVal(tStat.getRefVal(), tStat.getMaxRefValRatio());
				for (int i = 0; i < len; i++) {
					double d = distance(i, recn);
					if (Double.isNaN(d)) {
						continue;
					}
					if (getMethod() == 2) {
						if (Double.isNaN(dist2[i]) || dist2[i] > d) {
							dist2[i] = d;
						}
					} else { // method==3
						if (Double.isNaN(dist2[i])) {
							dist2[i] = d;
						} else {
							dist2[i] += d;
						}
					}
				}
			}
			if (getMethod() == 3) {
				for (int i = 0; i < len; i++)
					if (!Double.isNaN(dist2[i])) {
						dist2[i] /= referenceObjects2.size();
					}
			}
			/*
			* !!!
			*/
			tStat.ComputeMaxRefValRatio(referenceObjects1, fn);
			aTransf.setRefVal(tStat.getRefVal(), tStat.getMaxRefValRatio());
		}
	}

	public String getDist1ID() {
		return Dist1ID;
	}

	public String getDist2ID() {
		return Dist2ID;
	}

	public String getAttrID() {
		return AttrID;
	}

	public Vector getInvolvedAttrs() {
		Vector v = new Vector(fn.length + 3);
		for (int element : fn) {
			v.addElement(new String(dTable.getAttributeId(element)));
		}
		v.addElement(new String(Dist1ID));
		v.addElement(new String(AttrID));
		v.addElement(new String(Dist2ID));
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
