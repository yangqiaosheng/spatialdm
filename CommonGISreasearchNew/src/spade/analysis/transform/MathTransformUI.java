package spade.analysis.transform;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.FoldablePanel;
import spade.lib.lang.Language;
import spade.lib.util.IntArray;
import spade.vis.database.AttributeDataPortion;

/**
* The controls for setting parameters in a MathTransformer
*/
public class MathTransformUI extends Panel implements ItemListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.transform.Res");
	/**
	* The names of the transformation modes in the same order as they are
	* defined in the class MathTransformer
	*/
	public static String modeNames[] = { res.getString("off"), res.getString("log"), res.getString("log10"), res.getString("asc_order"), res.getString("desc_order"), res.getString("Z_score") };
	/**
	* The MathTransformer to be controlled
	*/
	protected MathTransformer mtrans = null;
	/**
	* The main panel used when individual transformation of each attribute is
	* allowed. Changes its content depending on whether each attribute is
	* transformed individually or all together.
	*/
	protected Panel mainP = null;
	/**
	* Used for switching on and off the individual transformation mode
	*/
	protected Checkbox indCB = null;
	/**
	* In the mode of common transformation of all attributes, used for
	* selection of the transformation operation.
	*/
	protected Choice funcCh = null;
	/**
	* Identifiers of the attributes transformed by the transformer
	*/
	protected Vector attrs = null;
	/**
	* In the mode of individual transformation of each attribute, used for
	* selection of the transformation operations for each attribute.
	*/
	protected Choice indFuncCh[] = null;

	public MathTransformUI(MathTransformer trans) {
		mtrans = trans;
		if (mtrans == null)
			return;
		attrs = mtrans.getAttrIds();
		if (attrs != null && attrs.size() < 3 && mtrans.getAllowIndividualTransformation()) {
			AttributeDataPortion table = mtrans.getDataTable();
			if (table != null) {
				IntArray colNs = mtrans.getColumnNumbers();
				if (colNs != null && colNs.size() <= 5) {
					attrs.removeAllElements();
					for (int i = 0; i < colNs.size(); i++) {
						attrs.addElement(table.getAttributeId(colNs.elementAt(i)));
					}
				}
			}
		}
		funcCh = new Choice();
		for (int i = MathTransformer.TR_FIRST; i <= MathTransformer.TR_LAST; i++) {
			funcCh.add(modeNames[i]);
		}
		funcCh.select(mtrans.getTransMode());
		funcCh.addItemListener(this);
		if (!mtrans.getAllowIndividualTransformation() || attrs == null || attrs.size() < 2) {
			setLayout(new ColumnLayout());
			add(new Label(res.getString("math_trans") + ":"));
			Panel p = new Panel(new BorderLayout());
			p.add(funcCh, BorderLayout.WEST);
			add(p);
		} else {
			constructMainPanel();
			FoldablePanel fp = new FoldablePanel(mainP, new Label(res.getString("math_trans")));
			//fp.open(); closed at start
			setLayout(new BorderLayout());
			add(fp, BorderLayout.CENTER);
		}
	}

	/**
	* Depending on whether the transformer transforms each attribute individually
	* or all are transformed together, constructs the content of the main panel.
	*/
	protected void constructMainPanel() {
		boolean visible = isShowing();
		if (visible) {
			setVisible(false);
		}
		if (mainP == null) {
			mainP = new Panel(new ColumnLayout());
		} else {
			mainP.removeAll();
		}
		if (indCB == null) {
			indCB = new Checkbox(res.getString("trans_ind"), mtrans.getTransformIndividually());
			indCB.addItemListener(this);
		}
		if (mtrans.getTransformIndividually() && attrs != null && attrs.size() > 1) {
			if (indFuncCh == null) {
				indFuncCh = new Choice[attrs.size()];
				for (int i = 0; i < indFuncCh.length; i++) {
					indFuncCh[i] = new Choice();
					for (int j = MathTransformer.TR_FIRST; j <= MathTransformer.TR_LAST; j++) {
						indFuncCh[i].add(modeNames[j]);
					}
					indFuncCh[i].select(mtrans.getTransMode((String) attrs.elementAt(i)));
					indFuncCh[i].addItemListener(this);
				}
			}
			for (int i = 0; i < indFuncCh.length; i++) {
				Panel p = new Panel(new BorderLayout());
				p.add(indFuncCh[i], BorderLayout.EAST);
				p.add(new Label(mtrans.getAttrName((String) attrs.elementAt(i))), BorderLayout.WEST);
				mainP.add(p);
			}
		} else {
			Panel p = new Panel(new BorderLayout());
			p.add(funcCh, BorderLayout.WEST);
			mainP.add(p);
		}
		mainP.add(indCB);
		if (visible) {
			setVisible(true);
		}
		CManager.validateAll(mainP);
	}

	/**
	* Reacts to mode selection in the choice (or one of choices, if each attribute
	* is transformed individually) and to switching on/off the individual
	* transformation mode.
	*/
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(funcCh)) {
			mtrans.setTransMode(funcCh.getSelectedIndex());
			mtrans.doTransformation();
		} else if (e.getSource().equals(indCB)) {
			mtrans.setTransformIndividually(indCB.getState());
			boolean changed = false;
			for (int i = 0; i < attrs.size() && !changed; i++) {
				changed = mtrans.getTransMode((String) attrs.elementAt(i)) != mtrans.getTransMode();
			}
			if (changed) {
				mtrans.doTransformation();
			}
			constructMainPanel();
		} else if (e.getSource() instanceof Choice) {
			int idx = -1;
			for (int i = 0; i < indFuncCh.length && idx < 0; i++)
				if (e.getSource().equals(indFuncCh[i])) {
					idx = i;
				}
			if (idx >= 0) {
				mtrans.setTransMode(indFuncCh[idx].getSelectedIndex(), (String) attrs.elementAt(idx));
				mtrans.doTransformation();
			}
		}
	}
}