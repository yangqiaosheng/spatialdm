package spade.analysis.classification;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Component;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;

import spade.analysis.manipulation.Manipulator;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.FoldablePanel;
import spade.lib.basicwin.Line;
import spade.lib.color.ColorCanvas;
import spade.lib.color.ColorDlg;
import spade.lib.color.ColorListener;
import spade.lib.lang.Language;
import spade.vis.database.AttributeDataPortion;

public class QualitativeClassManipulator extends Panel implements Manipulator, ActionListener, ItemListener, ColorListener, PropertyChangeListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.classification.Res");
	protected Supervisor sup = null;
	protected QualitativeClassifier qc = null;
	protected ColorCanvas ccs[] = null;
	protected Checkbox cbs[] = null;
	protected ColorDlg cDlg = null;
	protected Panel mainP = null;

	/**
	* Construction of map manipulator. Returns true if successfully constructed.
	* Arguments: 1) supervisor responsible for dynamic linking of displays;
	* 2) visualizer or classifier to be manipulated; 3) table with source data;
	* 4) table filter
	* The Manipulator must check if the visualizer has the appropriate type.
	* For QualitativeClassManipulator visualizer should be an instance of
	* QualitativeClassifier.
	*/
	@Override
	public boolean construct(Supervisor sup, Object visualizer, AttributeDataPortion dataTable) {
		if (visualizer == null)
			return false;
		if (!(visualizer instanceof QualitativeClassifier))
			return false;
		qc = (QualitativeClassifier) visualizer;
		qc.addPropertyChangeListener(this);
		this.sup = sup;
		setLayout(new ColumnLayout());
		makeInterior();
		return true;
	}

	protected void constructMainPanel() {
		mainP = new Panel(new ColumnLayout());
		ccs = new ColorCanvas[qc.getNClasses()];
		cbs = new Checkbox[ccs.length];
		for (int i = 0; i < ccs.length; i++) {
			Panel p = new Panel();
			p.setLayout(new BorderLayout());
			ccs[i] = new ColorCanvas();
			ccs[i].setColor(qc.getClassColor(i));
			ccs[i].setActionListener(this);
			p.add(ccs[i], "West");
//ID
			cbs[i] = new Checkbox(qc.getClassName(i), !qc.isClassHidden(i)/*true*/);
//~ID
			cbs[i].addItemListener(this);
			p.add(cbs[i], "Center");
			mainP.add(p);
		}
	}

	protected void makeInterior() {
		add(new Label(qc.getAttributeName(), Label.CENTER));
		constructMainPanel();
		add(mainP);
		add(new Line(false));
		ClassificationStatisticsCanvas csc = new ClassificationStatisticsCanvas(qc);
		FoldablePanel fp = new FoldablePanel(csc, new Label(res.getString("Classification")));
		fp.setBackground(new Color(223, 223, 223));
		add(fp);
		add(new Line(false));
		RangedDistPanel rdp = new RangedDistPanel(sup, qc);
		fp = new FoldablePanel(rdp, new Label(res.getString("Ranged_dist")));
		add(fp);
		add(new Line(false));
		Component broadPan = null;
		try {
			Object obj = Class.forName("spade.analysis.classification.ClassBroadcastPanel").newInstance();
			if (obj != null && (obj instanceof ClassOperator) && (obj instanceof Component)) {
				ClassOperator cop = (ClassOperator) obj;
				if (cop.construct(qc, sup)) {
					broadPan = (Component) obj;
				}
			}
		} catch (Exception e) {
		}
		if (broadPan != null) {
			add(broadPan);
		}
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (ie.getSource() instanceof Checkbox) {
			for (int i = 0; i < cbs.length; i++)
				if (cbs[i] == ie.getSource()) {
					qc.setClassIsHidden(!cbs[i].getState(), i);
					qc.notifyChange("colors", null);
					return;
				}
		}
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource() instanceof ColorCanvas) {
			// finding the ColorCanvas clicked
			ColorCanvas cc = null;
			String name = null;
			for (int i = 0; i < ccs.length && cc == null; i++)
				if (ccs[i] == ae.getSource()) {
					cc = ccs[i];
					name = qc.getClassName(i);
				}
			// getting new color for it
			if (cDlg == null) {
				cDlg = new ColorDlg(CManager.getAnyFrame(this), "");
			}
			//cDlg.setVisible(true);
			// following text: "Color for: "
			cDlg.setTitle(res.getString("Color_for_") + name);
			cDlg.selectColor(this, cc, cc.getColor());
			return;
		}
	}

	/*
	* color change through the dialog
	*/
	@Override
	public void colorChanged(Color c, Object sel) {
		// find a ColorCanvas produced the message
		ColorCanvas cc = null;
		String ID = null;
		for (int i = 0; i < ccs.length && cc == null; i++)
			if (ccs[i] == sel) {
				cc = ccs[i];
				// save a color
				cc.setColor(c);
				qc.setColorForClass(i, c);
				// hide a dialog
				cDlg.setVisible(false);
				return;
			}
	}

	/**
	* Reacts to changes in the classifier: if the classes changed, resets itself
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(qc)) {
			if (cbs == null && qc.getNClasses() == 0)
				return;
			if (cbs == null || qc.getNClasses() != cbs.length) {
				//the number of classes changed; reset everything
				boolean visible = isShowing();
				if (visible) {
					setVisible(false);
				}
				if (mainP != null) {
					remove(mainP);
				}
				constructMainPanel();
				add(mainP, 1);
				if (visible) {
					setVisible(true);
					CManager.validateAll(mainP);
				}
				return;
			}
			if (e.getPropertyName().equals("colors")) {
				for (int i = 0; i < qc.getNClasses(); i++) {
					ccs[i].setColor(qc.getClassColor(i));
				}
				return;
			}
			//check class colors
			for (int i = 0; i < qc.getNClasses(); i++)
				if (!qc.getClassColor(i).equals(ccs[i].getColor())) {
					ccs[i].setColor(qc.getClassColor(i));
				}
			//check class names
			for (int i = 0; i < qc.getNClasses(); i++)
				if (!qc.getClassName(i).equals(cbs[i].getLabel())) {
					cbs[i].setLabel(qc.getClassName(i));
				}
		}
	}
}
