package ui;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.DialogContent;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.MultiSelector;
import spade.lib.basicwin.TextCanvas;
import spade.lib.lang.Language;
import spade.lib.util.IntArray;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.Parameter;

/**
* A UI for selection of top-level attributes from a table. A top-level attribute
* is either a parameter-independent attribute (corresponds to a single table
* column) or a parent of a group of attributes referring to different values
* of a parameter or value combinations of several parameters (corresponds to
* a group of table column).
*/
public class RootAttrSelectPanel extends Panel implements DialogContent, ItemListener {
	static ResourceBundle res = Language.getTextResource("ui.Res");
	/**
	* The table from which attributes are selected
	*/
	protected AttributeDataPortion table = null;
	/**
	* If true, only one top-level attribute may be selected
	*/
	protected boolean selectOnlyOne = false;
	/**
	* The top-level attributes of the table
	*/
	protected Vector attrs = null;
	/**
	* The names of the top-level attributes of the table
	*/
	protected Vector attrNames = null;
	/**
	* A UI element used for selection of multiple items
	*/
	protected MultiSelector msel = null;
	/**
	* If only one item may be selected, a simple single-selection list is used
	* instead of the MultiSelector
	*/
	protected List aList = null;
	/**
	* Switches on/off showing non-numeric attributes
	*/
	protected Checkbox nonNumCB = null;
	/**
	* Indexes of numeric attributes in the list of all attributes
	*/
	protected IntArray numAttrInds = null;
	/**
	* The list of identifiers of the last selected attributes
	*/
	protected static Vector lastSel = null;

	/**
	* Constructs the UI for the selection of top-level attributes
	*/
	public RootAttrSelectPanel(AttributeDataPortion table, boolean maySelectOnlyOne, String prompt) {
		this(table, null, null, false, maySelectOnlyOne, prompt);
	}

	/**
	* Constructs the UI for the selection of top-level attributes
	* @param table - the table to select attributes from
	* @param selectAttrIds - identifiers of the attributes that must be pre-selected
	* @param excludeAttrIds - identifiers of the attributes that must not be
	*                         proposed for selection
	* @param onlyNumeric - allows selection of only numeric attributes
	* @param prompt - a text to be shown to the user
	*/
	public RootAttrSelectPanel(AttributeDataPortion table, Vector selectAttrIds, Vector excludeAttrIds, boolean onlyNumeric, boolean maySelectOnlyOne, String prompt) {
		if (table == null) {
			makeErrorUI(res.getString("no_attributes"));
			return;
		}
		this.table = table;
		selectOnlyOne = maySelectOnlyOne;
		//collect a list of attributes of the table
		attrs = table.getTopLevelAttributes();
		if (attrs == null || attrs.size() < 1) {
			makeErrorUI(res.getString("no_attributes"));
			return;
		}
		if (excludeAttrIds != null && excludeAttrIds.size() > 0) {
			for (int i = attrs.size() - 1; i >= 0; i--) {
				Attribute at = (Attribute) attrs.elementAt(i);
				if (excludeAttrIds.contains(at.getIdentifier())) {
					attrs.removeElementAt(i);
				}
			}
			if (attrs.size() < 1) {
				makeErrorUI(res.getString("no_attributes"));
				return;
			}
		}
		if (onlyNumeric) {
			for (int i = attrs.size() - 1; i >= 0; i--) {
				Attribute at = (Attribute) attrs.elementAt(i);
				if (AttributeTypes.isNominalType(at.getType())) {
					attrs.removeElementAt(i);
				}
			}
			if (attrs.size() < 1) {
				makeErrorUI(res.getString("no_numeric_attributes"));
				return;
			}
		}
		boolean hasNonNumeric = false, hasTemporalPar = false, hasOtherPar = false;
		attrNames = new Vector(attrs.size(), 1);
		for (int i = 0; i < attrs.size(); i++) {
			Attribute at = (Attribute) attrs.elementAt(i);
			hasNonNumeric = hasNonNumeric || AttributeTypes.isNominalType(at.getType());
			String name = at.getName();
			if (at.hasChildren()) {
				int nTemporalPar = 0, nOtherPar = 0;
				for (int j = 0; j < table.getParamCount(); j++) {
					Parameter par = table.getParameter(j);
					if (at.dependsOnParameter(par))
						if (par.isTemporal()) {
							++nTemporalPar;
						} else {
							++nOtherPar;
						}
				}
				if (nTemporalPar > 0 || nOtherPar > 0) {
					String prefix = "(";
					for (int j = 0; j < nTemporalPar; j++) {
						prefix += "T";
					}
					for (int j = 0; j < nOtherPar; j++) {
						prefix += "P";
					}
					name = prefix + ") " + name;
					hasTemporalPar = hasTemporalPar || nTemporalPar > 0;
					hasOtherPar = hasOtherPar || nOtherPar > 0;
				}
			}
			attrNames.addElement(name);
		}
		setLayout(new BorderLayout());
		if (prompt != null) {
			TextCanvas tc = new TextCanvas();
			tc.setText(prompt);
			add(tc, BorderLayout.NORTH);
		}
		if (selectAttrIds != null && selectAttrIds.size() > 0) {
			lastSel = (Vector) selectAttrIds.clone();
		}
		if (lastSel != null) {
			for (int i = lastSel.size() - 1; i >= 0; i--) {
				Attribute at = table.getAttribute((String) lastSel.elementAt(i));
				if (at == null) {
					lastSel.removeElementAt(i);
				} else if (at.getParent() != null) {
					at = at.getParent();
					lastSel.removeElementAt(i);
					if (!lastSel.contains(at.getIdentifier())) {
						lastSel.insertElementAt(at.getIdentifier(), i);
					}
				}
			}
		}
		if (selectOnlyOne) {
			aList = new List(Math.min(10, attrNames.size() + 1));
			add(aList, BorderLayout.CENTER);
			for (int i = 0; i < attrNames.size(); i++) {
				aList.add((String) attrNames.elementAt(i));
			}
		} else {
			msel = new MultiSelector(attrNames, true);
			add(msel, BorderLayout.CENTER);
		}
		if (lastSel != null && lastSel.size() > 0) {
			int idxs[] = null;
			if (!selectOnlyOne) {
				idxs = new int[lastSel.size()];
			}
			int k = 0;
			for (int n = 0; n < lastSel.size(); n++) {
				String id = (String) lastSel.elementAt(n);
				int aidx = -1;
				for (int i = 0; i < attrs.size() && aidx < 0; i++) {
					Attribute at = (Attribute) attrs.elementAt(i);
					if (id.equals(at.getIdentifier())) {
						aidx = i;
					}
				}
				if (aidx >= 0)
					if (selectOnlyOne) {
						aList.select(aidx);
						break;
					} else {
						idxs[k++] = aidx;
					}
			}
			if (!selectOnlyOne) {
				if (k > 0 && k < idxs.length) {
					int i1[] = new int[k];
					for (int i = 0; i < k; i++) {
						i1[i] = idxs[i];
					}
					idxs = i1;
				}
				if (k > 0) {
					msel.selectItems(idxs);
				}
			}
		}
		if (hasNonNumeric || hasTemporalPar || hasOtherPar) {
			Panel p = new Panel(new ColumnLayout());
			if (hasNonNumeric) {
				nonNumCB = new Checkbox(res.getString("include_non_numeric"), true);
				nonNumCB.addItemListener(this);
				p.add(nonNumCB);
				p.add(new Line(false));
			}
			if (hasTemporalPar) {
				p.add(new Label("(T) - " + res.getString("time_dep")));
			}
			if (hasOtherPar) {
				p.add(new Label("(P) - " + res.getString("par_dep")));
			}
			add(p, BorderLayout.SOUTH);
		}
	}

	/**
	* If there are no attributes to select from, creates an "empty" UI with the
	* specified error message displayed
	*/
	protected void makeErrorUI(String errorMsg) {
		setLayout(new BorderLayout());
		Label l = new Label(errorMsg, Label.CENTER);
		l.setForeground(Color.yellow);
		l.setBackground(Color.red.darker());
		add(l, "Center");
	}

	/**
	* Reacts to the checkbox switching on/off showing non-numeric attributes
	*/
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (nonNumCB != null && e.getSource().equals(nonNumCB)) {
			if (!nonNumCB.getState() && numAttrInds == null) {
				numAttrInds = new IntArray(attrs.size(), 1);
				for (int i = 0; i < attrs.size(); i++) {
					Attribute at = (Attribute) attrs.elementAt(i);
					if (AttributeTypes.isNumericType(at.getType()) || AttributeTypes.isTemporal(at.getType())) {
						numAttrInds.addElement(i);
					}
				}
			}
			int inds[] = null;
			if (selectOnlyOne) {
				int k = aList.getSelectedIndex();
				if (k >= 0) {
					inds = new int[1];
					inds[0] = k;
				}
			} else {
				msel.getSelectedIndexes();
			}
			Vector names = attrNames;
			if (!nonNumCB.getState()) {
				names = new Vector(numAttrInds.size(), 1);
				for (int i = 0; i < numAttrInds.size(); i++) {
					names.addElement(attrNames.elementAt(numAttrInds.elementAt(i)));
				}
				int nsel = 0;
				if (inds != null) {
					for (int ind : inds)
						if (numAttrInds.indexOf(ind) >= 0) {
							++nsel;
						}
				}
				if (nsel < 1) {
					inds = null;
				} else if (nsel < inds.length) {
					int inds1[] = new int[nsel];
					int k = 0;
					for (int ind : inds) {
						int n = numAttrInds.indexOf(ind);
						if (n >= 0) {
							inds1[k++] = n;
						}
					}
					inds = inds1;
				} else {
					for (int i = 0; i < inds.length; i++) {
						inds[i] = numAttrInds.indexOf(inds[i]);
					}
				}
			} else if (inds != null && numAttrInds != null) {
				for (int i = 0; i < inds.length; i++) {
					inds[i] = numAttrInds.elementAt(inds[i]);
				}
			}
			if (selectOnlyOne) {
				aList.setVisible(false);
				aList.removeAll();
				for (int i = 0; i < names.size(); i++) {
					aList.add((String) names.elementAt(i));
				}
				if (inds != null) {
					aList.select(inds[0]);
				} else {
					aList.select(-1);
				}
				aList.setVisible(true);
			} else {
				msel.replaceItemList(names);
				if (inds != null) {
					msel.selectItems(inds);
				}
			}
		}
	}

	/**
	* The error message
	*/
	protected String err = null;

	/**
	* Checks if the user has selected any attribute
	*/
	@Override
	public boolean canClose() {
		if (table == null || attrs == null)
			return true;
		if (selectOnlyOne)
			if ((selectOnlyOne && aList.getSelectedIndex() < 0) || (!selectOnlyOne && msel.getSelectedIndexes() == null)) {
				err = res.getString("No_attributes");
				return false;
			}
		return true;
	}

	@Override
	public String getErrorMessage() {
		return err;
	}

	/**
	* Returns the user-selected attributes (instances of spade.vis.database.Attribute)
	*/
	public Vector getSelectedAttributes() {
		if ((msel == null && aList == null) || attrs == null || attrs.size() < 1)
			return null;
		int inds[] = null;
		if (msel != null) {
			inds = msel.getSelectedIndexes();
		} else {
			int k = aList.getSelectedIndex();
			if (k < 0)
				return null;
			inds = new int[1];
			inds[0] = k;
		}
		if (inds == null)
			return null;
		Vector selected = new Vector(inds.length, 1);
		if (lastSel == null) {
			lastSel = new Vector(10, 10);
		} else {
			lastSel.removeAllElements();
		}
		boolean onlyNum = nonNumCB != null && !nonNumCB.getState();
		for (int ind : inds) {
			Attribute at = (Attribute) attrs.elementAt((onlyNum) ? numAttrInds.elementAt(ind) : ind);
			selected.addElement(at);
			lastSel.addElement(at.getIdentifier());
		}
		return selected;
	}
}