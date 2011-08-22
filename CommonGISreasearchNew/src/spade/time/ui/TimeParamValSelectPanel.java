package spade.time.ui;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.TabbedPanel;
import spade.lib.lang.Language;
import spade.lib.ui.ParamValSelector;
import spade.lib.util.IntArray;
import spade.time.vis.VisAttrDescriptor;
import spade.vis.database.AttrDescriptor;
import spade.vis.database.Attribute;
import spade.vis.database.Parameter;

/**
* A UI for selection of temporal parameter values for time-dependent attributes.
* Allows the user to select one or more relative time references, such as t,
* t+1, t+2, ..., where t is the current time moment (e.g. in animation).
*/
public class TimeParamValSelectPanel extends Panel implements ItemListener {
	static ResourceBundle res = Language.getTextResource("spade.time.ui.Res");
	/**
	* The initial list of attribute descriptors, instances of the class
	* spade.vis.database.AttrDescriptor. May contain both time-dependent and
	* time-independent attributes.
	*/
	protected Vector attrDescr = null;
	/**
	* The temporal parameter
	*/
	protected Parameter tPar = null;
	/**
	* The list of descriptors of time-dependent attributes, instances of the class
	* spade.vis.database.AttrDescriptor.
	*/
	protected Vector depAttrs = null;
	/**
	* The numbers of the time-dependent attributes in the initial list of
	* all attributes
	*/
	protected IntArray idxs = null;
	/**
	* Selectors of parameter values for each parameter-dependent attribute
	*/
	protected ParamValSelector psel[] = null;
	protected TabbedPanel tpa = null;
	protected Checkbox separateSelectCB = null;
	protected Panel header = null;

	/**
	* Constructs the UI for the selection of values of the temporal parameter. The
	* vector attrDescr must consist of instances of spade.vis.database.AttrDescriptor.
	* The temporal parameter is specified through the argument tPar.
	* The vector currSelection consists of instances of the class
	* spade.time.vis.VisAttrDescriptor and is used for pre-selecting some
	* parameter values. May be null.
	*/
	public TimeParamValSelectPanel(Vector attrDescr, Parameter tPar, Vector currSelection) {
		if (attrDescr == null || attrDescr.size() < 1) {
			makeErrorUI(res.getString("no_attributes"));
			return;
		}
		this.attrDescr = attrDescr;
		this.tPar = tPar;
		depAttrs = new Vector(attrDescr.size(), 1);
		idxs = new IntArray(attrDescr.size(), 1);
		for (int i = 0; i < attrDescr.size(); i++) {
			AttrDescriptor ad = (AttrDescriptor) attrDescr.elementAt(i);
			if (ad.children != null && ad.children.size() > 1 && ad.attr.dependsOnParameter(tPar)) {
				depAttrs.addElement(ad);
				idxs.addElement(i);
			}
		}
		if (depAttrs.size() < 1) {
			depAttrs = null;
			idxs = null;
			makeErrorUI(res.getString("no_temp_attr_selected"));
			return;
		}
		psel = new ParamValSelector[1];
		header = new Panel(new ColumnLayout());
		if (depAttrs.size() == 1) {
			header.add(new Label(res.getString("attribute")));
			AttrDescriptor ad = (AttrDescriptor) depAttrs.elementAt(0);
			header.add(new Label(ad.getName(), Label.CENTER));
			header.add(new Label(res.getString("depends_on_temp_param"), Label.CENTER));
		} else {
			header.add(new Label(res.getString("attributes")));
			for (int i = 0; i < depAttrs.size(); i++) {
				AttrDescriptor ad = (AttrDescriptor) depAttrs.elementAt(i);
				header.add(new Label(ad.getName(), Label.CENTER));
			}
			header.add(new Label(res.getString("depend_on_temp_param"), Label.CENTER));
		}
		header.add(new Line(false));
		header.add(new Label(res.getString("Select_val_temp_par") + ":", Label.CENTER));
		Vector paramVals[] = new Vector[1];
		paramVals[0] = new Vector(tPar.getValueCount() * 2 + 1, 1);
		paramVals[0].addElement(tPar.getName());
		paramVals[0].addElement("t");
		for (int i = 1; i < tPar.getValueCount(); i++) {
			paramVals[0].addElement("t+" + i);
		}
		for (int i = 0; i < tPar.getValueCount(); i++) {
			paramVals[0].addElement(tPar.getValue(i));
		}
		psel[0] = new ParamValSelector((depAttrs.size() == 1) ? header : null, paramVals);
		IntArray selTIdxs = new IntArray(paramVals[0].size(), 1);
		if (currSelection != null && currSelection.size() > 0) {
			//find what values of the temporal parameters are currently selected
			for (int i = 0; i < currSelection.size(); i++) {
				VisAttrDescriptor vad = (VisAttrDescriptor) currSelection.elementAt(i);
				if (vad.isTimeDependent)
					if (selTIdxs.indexOf(vad.offset) < 0) {
						selTIdxs.addElement(vad.offset);
					} else {
						;
					}
				else if (vad.fixedParams != null) {
					for (int j = 0; j < vad.fixedParams.size(); j++)
						if (vad.fixedParams.elementAt(j).toString().equals(tPar.getName())) {
							int idx = tPar.getValueIndex(vad.fixedParamVals.elementAt(j));
							if (idx >= 0) {
								idx += tPar.getValueCount();
								if (selTIdxs.indexOf(idx) < 0) {
									selTIdxs.addElement(idx);
								}
							}
							break;
						}
				}
			}
		}
		if (selTIdxs.size() < 1) {
			selTIdxs.addElement(0);
		}
		psel[0].selectParamValues(0, selTIdxs.getTrimmedArray());
		setLayout(new BorderLayout());
		if (depAttrs.size() > 1) {
			add(header, BorderLayout.NORTH);
			add(psel[0], BorderLayout.CENTER);
			separateSelectCB = new Checkbox(res.getString("individually"), false);
			separateSelectCB.addItemListener(this);
			add(separateSelectCB, BorderLayout.SOUTH);
		} else {
			add(psel[0], BorderLayout.CENTER);
		}
	}

	/**
	* Switches between separate selection of parameter values for each attribute
	* and simultaneous selection (if all attributes depend on the same parameters)
	*/
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(separateSelectCB)) {
			setVisible(false);
			if (separateSelectCB.getState()) {
				ParamValSelector ps0 = psel[0];
				int idxs[] = ps0.getSelectedValueIndexes(0);
				if (psel.length < depAttrs.size()) {
					Vector paramVals[] = ps0.getParameters();
					psel = new ParamValSelector[depAttrs.size()];
					psel[0] = ps0;
					for (int i = 1; i < depAttrs.size(); i++) {
						psel[i] = new ParamValSelector(null, paramVals);
						psel[i].selectParamValues(0, idxs);
					}
				}
				tpa = new TabbedPanel();
				remove(ps0);
				for (int i = 0; i < psel.length; i++) {
					tpa.addComponent(((AttrDescriptor) depAttrs.elementAt(i)).getName(), psel[i]);
				}
				add(tpa, BorderLayout.CENTER);
				tpa.makeLayout();
			} else {
				tpa.removeAllComponents();
				for (ParamValSelector element : psel) {
					element.setVisible(true);
				}
				remove(tpa);
				tpa = null;
				add(psel[0], BorderLayout.CENTER);
			}
			setVisible(true);
			CManager.validateFully(this);
		}
	}

	/**
	* If there are no attributes or no parameter values to select from, creates an
	* "empty" UI with the specified error message displayed
	*/
	protected void makeErrorUI(String errorMsg) {
		setLayout(new BorderLayout());
		Label l = new Label(errorMsg, Label.CENTER);
		l.setForeground(Color.yellow);
		l.setBackground(Color.red.darker());
		add(l, "Center");
	}

	/**
	* Returns the list of descriptors of the attributes corresponding to the
	* selected parameter values. The resulting vector consists of instances of
	* spade.time.vis.VisAttrDescriptor.
	*/
	public Vector getAttrDescriptors() {
		if (attrDescr == null || attrDescr.size() < 1)
			return null;
		Vector descr = new Vector(Math.max(attrDescr.size() * 3, 20), 10);
		for (int i = 0; i < attrDescr.size(); i++) {
			AttrDescriptor ad = (AttrDescriptor) attrDescr.elementAt(i);
			int k = idxs.indexOf(i);
			if (k < 0) {
				VisAttrDescriptor vd = new VisAttrDescriptor();
				vd.attr = ad.attr;
				vd.attrId = ad.attr.getIdentifier();
				if (ad.children != null) {
					vd.parent = ad.attr;
				}
				vd.isTimeDependent = false;
				if (ad.parVals != null && ad.parVals.length > 0) {
					vd.fixedParams = new Vector(ad.parVals.length, 1);
					vd.fixedParamVals = new Vector(ad.parVals.length, 1);
					for (Vector parVal : ad.parVals) {
						String name = (String) parVal.elementAt(0);
						Object value = parVal.elementAt(1);
						vd.fixedParams.addElement(name);
						vd.fixedParamVals.addElement(value);
					}
				}
				descr.addElement(vd);
			} else {
				ParamValSelector ps = psel[0];
				if (k > 0 && (separateSelectCB == null || separateSelectCB.getState())) {
					ps = psel[k];
				}
				Vector values = ps.getSelectedParamValues(0);
				if (values == null || values.size() < 1) {
					continue;
				}
				for (int j = 0; j < values.size(); j++) {
					VisAttrDescriptor vd = new VisAttrDescriptor();
					vd.attr = ad.attr;
					vd.attrId = ad.attr.getIdentifier();
					vd.parent = ad.attr;
					Object val = values.elementAt(j);
					vd.isTimeDependent = (val instanceof String); //a relative time moment
					int nFixedPar = ad.parVals.length;
					if (vd.isTimeDependent) {
						--nFixedPar;
					}
					if (nFixedPar > 0) {
						vd.fixedParams = new Vector(nFixedPar, 1);
						vd.fixedParamVals = new Vector(nFixedPar, 1);
						for (Vector parVal : ad.parVals) {
							String name = (String) parVal.elementAt(0);
							boolean temporal = name.equals(tPar.getName());
							if (temporal && vd.isTimeDependent) {
								continue;
							}
							vd.fixedParams.addElement(name);
							if (temporal) {
								vd.fixedParamVals.addElement(val);
							} else {
								vd.fixedParamVals.addElement(parVal.elementAt(1));
							}
						}
					}
					if (vd.isTimeDependent) {
						String str = (String) val;
						int idx = str.indexOf('+');
						if (idx > 0) {
							try {
								vd.offset = Integer.valueOf(str.substring(idx + 1).trim()).intValue();
							} catch (NumberFormatException nfe) {
							}
						}
					} else {
						boolean childFound = false;
						for (int n = 0; n < ad.children.size() && !childFound; n++) {
							Attribute child = (Attribute) ad.children.elementAt(n);
							if (child.hasParamValue(tPar.getName(), val)) {
								vd.attr = child;
								vd.attrId = child.getIdentifier();
								childFound = true;
							}
						}
						if (!childFound) {
							continue;
						}
					}
					descr.addElement(vd);
				}
			}
		}
		return descr;
	}
}
