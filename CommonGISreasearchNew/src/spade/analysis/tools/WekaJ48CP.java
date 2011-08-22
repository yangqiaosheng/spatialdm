package spade.analysis.tools;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;

import spade.analysis.plot.SegmentedBarPlot;
import spade.analysis.system.ESDACore;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.PlotCanvas;
import spade.lib.basicwin.PopupManager;
import spade.lib.basicwin.SplitPanel;
import spade.lib.basicwin.TreeViewWithStatusBars;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeDataPortion;
import weka.classifiers.Evaluation;
import weka.classifiers.j48.C45Split;
import weka.classifiers.j48.ClassifierTree;
import weka.classifiers.j48.J48;
import weka.core.Attribute;
import weka.core.AttributeStats;
import weka.core.Instances;

public class WekaJ48CP extends Frame implements ActionListener, ItemListener /*, MouseListener, MouseMotionListener*/{

	protected AttributeDataPortion tbl = null;
	protected WekaAttrSelector was = null;
	protected Instances wasInstances = null;
	protected Checkbox[] wasCb = null;
	protected int[] nAttrOccur = null;
	protected float fAttrMin[] = null, fAttrMax[] = null;

	protected Panel pTargetVals = null;
	protected Checkbox cbPruned = null;
	protected TreeViewWithStatusBars tv = null;

	Vector vsbp = null;

	// target attribute: CommonGIS references
	protected String tAttrId = null;
	protected int tAttrN = 0;
	protected String tAttrValueList[] = null;
	protected Color tAttrValueColors[] = null;
	// number of values for each instance
	protected int tAttrVals[] = null;

	protected Vector treeRuleDef = null, // definitions of rules for tree items
			treeItemsData = null; // and data items corresponding to each tree item
	protected Panel rulePanel = null;

	// information popups
	protected PopupManager popM = null;

	public WekaJ48CP(Instances instances, ESDACore core, AttributeDataPortion tbl) {
		super("Weka J48 Classifier Control Panel");
		this.tbl = tbl;
		selectTargetAttribute(instances);
		int wekaAttrIdx = instances.classIndex();
		Attribute wekaAttr = instances.attribute(wekaAttrIdx);
		tAttrId = wekaAttr.name();
		tAttrN = tbl.getAttrIndex(tAttrId);
		tAttrValueList = tbl.getAttribute(tAttrN).getValueList();
		tAttrValueColors = tbl.getAttribute(tAttrN).getValueColors();
		tAttrVals = new int[instances.numInstances()];
		for (int i = 0; i < tAttrVals.length; i++) {
			tAttrVals[i] = -1;
			for (int j = 0; j < wekaAttr.numValues() && tAttrVals[i] == -1; j++)
				if (wekaAttr.value(j).equals(instances.instance(i).stringValue(wekaAttrIdx))) {
					tAttrVals[i] = j;
				}
		}
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				dispose();
			}
		});
		setLayout(new BorderLayout());
		SplitPanel sp = new SplitPanel(true);
		add(sp, BorderLayout.CENTER);
		Panel lp = new Panel(), rp = new Panel();
		sp.addSplitComponent(lp, 0.5f);
		sp.addSplitComponent(rp, 0.5f);
		lp.setLayout(new BorderLayout());
		lp.add(was = new WekaAttrSelector(instances, tbl, true), BorderLayout.CENTER);
		Panel p = new Panel();
		lp.add(p, BorderLayout.NORTH);
		p.setLayout(new ColumnLayout());
		p.add(new Label("Target : " + tbl.getAttributeName(wekaAttr.name())));
		AttributeStats as = instances.attributeStats(wekaAttrIdx);
		int n[] = as.nominalCounts;
		for (int j = 0; j < tAttrValueList.length; j++) {
			Panel pp = new Panel(new BorderLayout());
			p.add(pp);
			Label l = new Label(String.valueOf(n[j]), Label.RIGHT);
			l.setBackground(tAttrValueColors[j]);
			l.setForeground((tAttrValueColors[j].getGreen() > 127) ? Color.black : Color.white);
			pp.add(l, BorderLayout.WEST);
			l = new Label(tAttrValueList[j]);
			pp.add(l, BorderLayout.CENTER);
		}
		p.add(new Line(false));
		p = new Panel();
		lp.add(p, BorderLayout.SOUTH);
		p.setLayout(new ColumnLayout());
		p.add(new Line(false));
		p.add(cbPruned = new Checkbox("Pruned tree", true));
		p.add(new Line(false));
		Button b = new Button("Run J48 classifier");
		b.addActionListener(this);
		p.add(b);
		rp.setLayout(new BorderLayout());
		tv = new TreeViewWithStatusBars();
		tv.addItemListener(this);
		tv.addItem("0", "Tree is not built yet", null);
		tv.setup();
		rp.add(tv, BorderLayout.CENTER);
		// create rulePanel ...
		setActiveRule(-1);
		Panel tvcp = new Panel(new ColumnLayout());
		rulePanel = new Panel(new ColumnLayout());
		tvcp.add(rulePanel);
		tvcp.add(new Line(false));
		tvcp.add(new Label("Tree manipulation controls ..."));
		rp.add(tvcp, BorderLayout.SOUTH);
		setSize(500, 300);
		pack();
		show();
		// activate PopupManager
		if (popM == null) {
			popM = new PopupManager(this, "", true);
			popM.setOnlyForActiveWindow(false);
		}
		//The window must be properly registered in order to be closed in a case
		//when the aplication is closed or changed.
		core.getWindowManager().registerWindow(this);
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		runWekaJ48();
	}

	protected void runWekaJ48(/*, ESDACore core, AttributeDataPortion tbl*/) {
		try {
			wasInstances = was.getSubset();
			wasCb = was.getCheckbox();
			if (nAttrOccur == null) {
				nAttrOccur = new int[wasCb.length];
				fAttrMin = new float[wasCb.length];
				fAttrMax = new float[wasCb.length];
				for (int i = 0; i < wasCb.length; i++) {
					fAttrMin[i] = Float.NaN;
					fAttrMax[i] = Float.NaN;
				}
			}
			for (int i = 0; i < nAttrOccur.length; i++) {
				nAttrOccur[i] = 0;
			}
			System.out.println("* J48 Classification Tree: Start " + (new java.util.Date()));
			Evaluation evaluation = new Evaluation(wasInstances);
			J48 j48 = new J48();
			j48.setUnpruned(!cbPruned.getState());
			j48.buildClassifier(wasInstances);
			System.out.println("* J48 Classification Tree: Finish " + (new java.util.Date()));
			evaluation.evaluateModel(j48, wasInstances);
			//System.out.println(j48.prefix());
			System.out.println(j48.toString());
			System.out.println(evaluation.toMatrixString());
			boolean bDataItems[] = new boolean[wasInstances.numInstances()]; //tbl.getDataItemCount()];
			for (int i = 0; i < bDataItems.length; i++) {
				bDataItems[i] = true;
				for (int j = 0; j < wasInstances.numAttributes() && bDataItems[i]; j++) {
					bDataItems[i] = !wasInstances.instance(i).isMissing(j);
				}
			}
			treeRuleDef = new Vector(150, 150);
			treeItemsData = new Vector(150, 150);
			vsbp = new Vector(150, 150);
			tv.clear();
			tv.setVisible(false);
			int n[] = getCounts(bDataItems);
			SegmentedBarPlot sbp = new SegmentedBarPlot(tAttrValueColors, n, n);
			vsbp.addElement(sbp);
			String str = "(" + n[0] + "=";
			for (int j = 1; j < n.length; j++) {
				str += ((j == 1) ? "" : "+") + n[j];
			}
			str += ")";
			String itemID = "0";
			tv.addItem(itemID, str, null);
			Vector v = new Vector(150, 150);
			treeRuleDef.addElement(v);
			treeItemsData.addElement(bDataItems);
			//System.out.println("* A: "+(new java.util.Date()));
			CTtoTV(itemID, j48.getClassifierTree(), bDataItems, v, vsbp);
			//System.out.println("* B: "+(new java.util.Date()));
			tv.setVSBP(vsbp);
			tv.setVisible(true);
			tv.setup();
			tv.collapseAll();
			tv.expand(0);
			tv.setSelectedIndex(0);
			setActiveRule(0);
			Color greyColor = new Color(208, 208, 208);
			for (int i = 0; i < wasCb.length; i++) {
				wasCb[i].setBackground((nAttrOccur[i] == 0) ? Color.white : greyColor);
			}
			was.setNumbers(nAttrOccur);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void CTtoTV(String parentID, ClassifierTree ct, boolean bDataItems[], Vector vrd, Vector vsbp) {
		C45Split c45Split = ct.getC45Split();
		if (c45Split == null || ct.getSons() == null)
			return;
		for (int i = 0; i < ct.getSons().length; i++) {
			int wekaAttrIdx = c45Split.getSplitAttrIndex();
			Attribute wekaAttr = wasInstances.attribute(wekaAttrIdx);
			String tblAttrId = wekaAttr.name();
			Vector nextvrd = (Vector) vrd.clone();
			nextvrd.addElement(tblAttrId);
			if (i == 0) {
				nAttrOccur[was.getIdxOfNthSelected(wekaAttrIdx)]++;
			}
			nextvrd.addElement(new Integer(treeRuleDef.size()));
			String item = tbl.getAttributeName(tblAttrId) + " ";
			boolean subDataItems[] = bDataItems.clone();
			if (wekaAttr.isNumeric()) {
				double val = (float) c45Split.getSplitPoint();
				nextvrd.addElement(new Integer((i == 0) ? 0 : 1));
				float min = fAttrMin[wekaAttrIdx], max = fAttrMax[wekaAttrIdx];
				if (Float.isNaN(fAttrMin[wekaAttrIdx])) {
					fAttrMin[wekaAttrIdx] = min = (float) wasInstances.attributeStats(wekaAttrIdx).numericStats.min;
					fAttrMax[wekaAttrIdx] = max = (float) wasInstances.attributeStats(wekaAttrIdx).numericStats.max;
				}
				String str = StringUtil.floatToStr((float) val, min, max);
				nextvrd.addElement(str);
				item += ((i == 0) ? "<= " : "> ") + str;
				for (int j = 0; j < subDataItems.length; j++)
					if (subDataItems[j]) {
						double v = wasInstances.instance(j).value(wekaAttrIdx);
						if (i == 0) {
							subDataItems[j] = v <= val;
						} else {
							subDataItems[j] = v > val;
						}
					}
			} else {
				item += "= " + wekaAttr.value(i);
				nextvrd.addElement(new Integer(2));
				nextvrd.addElement(wekaAttr.value(i));
				for (int j = 0; j < subDataItems.length; j++)
					if (subDataItems[j]) {
						String v = wasInstances.instance(j).stringValue(wekaAttrIdx);
						subDataItems[j] = v.equals(wekaAttr.value(i));
					}
			}
			int n[] = getCounts(subDataItems);
			SegmentedBarPlot sbp0 = (SegmentedBarPlot) vsbp.elementAt(0), sbp = new SegmentedBarPlot(tAttrValueColors, sbp0.getTotalClassCounts(), n);
			vsbp.addElement(sbp);
			String str = "(" + n[0] + "=";
			for (int j = 1; j < n.length; j++) {
				str += ((j == 1) ? "" : "+") + n[j];
			}
			item = str + ") " + item;
			String itemID = String.valueOf(tv.getItemCount());
			tv.addItem(itemID, item, parentID);
			treeRuleDef.addElement(nextvrd);
			treeItemsData.addElement(subDataItems);
			CTtoTV(itemID, ct.getSons()[i], subDataItems, nextvrd, vsbp);
		}
	}

	protected int[] getCounts(boolean bDataItems[]) {
		int wekaAttrIdx = wasInstances.classIndex();
		Attribute wekaAttr = wasInstances.attribute(wekaAttrIdx);
		int n = 1 + wekaAttr.numValues(), counts[] = new int[n];
		for (int j = 0; j < n; j++) {
			counts[j] = 0;
		}
		for (int i = 0; i < bDataItems.length; i++)
			if (bDataItems[i]) {
				counts[0]++;
				counts[1 + tAttrVals[i]]++;
			}
		return counts;
	}

	protected void selectTargetAttribute(Instances instances) {
		//if (!AnyNominalAttribute) return;
		IntArray ia = new IntArray(instances.numAttributes(), 10);
		for (int i = 0; i < instances.numAttributes(); i++)
			if (instances.attribute(i).isNominal()) {
				ia.addElement(i);
			}
		int selIdx;
		if (ia.size() == 1) {
			selIdx = ia.elementAt(0);
		} else {
			List l = new List(5);
			for (int i = 0; i < ia.size(); i++) {
				String tblAttrId = instances.attribute(ia.elementAt(i)).name();
				l.add(tbl.getAttributeName(tblAttrId));
			}
			l.select(0);
			OKDialog dlg = new OKDialog(CManager.getAnyFrame(), "Select target attribute", false);
			dlg.addContent(l);
			dlg.show();
			selIdx = ia.elementAt(l.getSelectedIndex());
		}
		instances.setClassIndex(selIdx);
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (ie.getSource().equals(tv)) {
			String id = (String) ie.getItem();
			int n = -1;
			try {
				n = Integer.valueOf(id).intValue();
			} catch (NumberFormatException nfe) {
			}
			setActiveRule(n);
		}
	}

	protected int activeRuleNumber = -1;

	protected void setActiveRule(int n) {
		if (n == -1)
			return;
		if (n >= treeRuleDef.size())
			// something is wrong...
			return;
		//System.out.println("* active rule "+n);
		if (n == activeRuleNumber)
			return;
		activeRuleNumber = n;
		Vector vrd = (Vector) treeRuleDef.elementAt(n);
		boolean bDataItems[] = (boolean[]) treeItemsData.elementAt(n);
		rulePanel.removeAll();
		rulePanel.add(new Line(false));
		PlotCanvas pc = new PlotCanvas();
		/*
		pc.addMouseListener(this);
		pc.addMouseMotionListener(this);
		*/
		SegmentedBarPlot sbp = (SegmentedBarPlot) vsbp.elementAt(0);
		pc.setContent(sbp);
		Panel ruleLinePanel = new Panel(new BorderLayout());
		ruleLinePanel.add(pc, BorderLayout.WEST);
		ruleLinePanel.add(new Label("IF"), BorderLayout.CENTER);
		rulePanel.add(ruleLinePanel);
		if (vrd != null && vrd.size() > 0) {
			for (int i = 0; i < vrd.size(); i += 4) {
				String tblAttrId = (String) vrd.elementAt(i), tblAttrName = "\"" + tbl.getAttributeName(tblAttrId) + "\"";
				int idx = ((Integer) vrd.elementAt(i + 1)).intValue();
				int ioper = ((Integer) vrd.elementAt(i + 2)).intValue();
				String soper = (ioper == 0) ? "<=" : ((ioper == 1) ? ">" : "=");
				String cmp = (String) vrd.elementAt(i + 3);
				ruleLinePanel = new Panel(new BorderLayout());
				pc = new PlotCanvas();
				/*
				pc.addMouseListener(this);
				pc.addMouseMotionListener(this);
				*/
				sbp = (SegmentedBarPlot) vsbp.elementAt(idx);
				pc.setContent(sbp);
				ruleLinePanel.add(pc, BorderLayout.WEST);
				ruleLinePanel.add(new Label(((i == 0) ? "  " : "& ") + tblAttrName + " " + soper + " " + cmp), BorderLayout.CENTER);
				rulePanel.add(ruleLinePanel);
			}
		}
		int nn[] = getCounts(bDataItems);
		Panel p = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		rulePanel.add(p);
		Label l = new Label("THEN \"" + tbl.getAttributeName(tAttrN) + "\" =");
		p.add(l);
		for (int j = 1; j < nn.length; j++)
			if (nn[j] > 0) {
				l = new Label(tAttrValueList[j - 1], Label.RIGHT);
				l.setForeground(Color.black);
				p.add(l);
				l = new Label(String.valueOf(nn[j]), Label.CENTER);
				l.setBackground(tAttrValueColors[j - 1]);
				l.setForeground((tAttrValueColors[j - 1].getGreen() > 127) ? Color.black : Color.white);
				p.add(l);
			}
		CManager.validateAll(rulePanel);
	}

	// MouseListener interface
	/*
	public void mouseReleased (MouseEvent e) { // selection!
	  System.out.println("* "+e);
	}
	public void mouseMoved (MouseEvent e) { // information popups!
	  System.out.println("* "+e);
	  if (popM==null) return;
	  popM.hideWindow();
	  boolean found=true;
	  if (!found)
	    popM.setKeepHidden(true);
	  else {
	    popM.setText("ku-ku");
	    popM.setKeepHidden(false);
	    Canvas c=(Canvas)e.getSource();
	    popM.startShow((int)c.getLocationOnScreen().getX()+e.getX(),(int)c.getLocationOnScreen().getY()+e.getY());
	  }
	}
	public void mousePressed (MouseEvent e) {}
	public void mouseClicked (MouseEvent e) {}
	public void mouseEntered (MouseEvent e) {}
	public void mouseExited (MouseEvent e) {}
	public void mouseDragged (MouseEvent e) {}
	*/

}