package ui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.Supervisor;
import spade.analysis.system.SystemUI;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.lib.ui.ListOrderer;
import spade.lib.util.IntArray;
import spade.vis.database.AttrDescriptor;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;

/**
* Provides a user interface for selection of attributes. If there are parameter-
* dependent attributes, lets the user select parameter values.
*/
public class AttributeChooser {
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
	* Indicates whether it is necessary to offer the user to change the default
	* column order. By default is true.
	*/
	protected boolean columnsMayBeReordered = false;
	/**
	* The frame to be used as the owner of all the dialogs used
	*/
	protected Frame mainFrame = null;
	/**
	* The panel for selecting top-level attributes
	*/
	protected RootAttrSelectPanel rootSP = null;
	/**
	* The list of selected (top-level) attributes. For an attribute, there may be
	* a group of corresponding columns.
	*/
	protected Vector selAttr = null;
	/**
	* The panel for selecting parameter values
	*/
	protected ParamValSelectPanel pvsel = null;
	/**
	* The list of low-level attributes. For each attribute, there is only one
	* corresponding column.
	*/
	protected Vector selColumns = null;
	/**
	* Descriptors of the selected attributes, which contain information about
	* the attributes, parameters, and selected parameter values.
	* The elements of the vector are instances of the class
	* spade.vis.database.AttrDescriptor.
	*/
	protected Vector attrDescr = null;
	/**
	 * If available, may be used for finding currently existing layers and tables.
	 * It is possible to use the names of selected objects from a layer or a
	 * table as parameter values, if they can be correctly transformed
	 */
	protected DataKeeper dataKeeper = null;
	/**
	 * If available, may be used for taking selected objects from a layer or a
	 * table and trying to treat their names as parameter values
	 */
	protected Supervisor supervisor = null;
	/**
	* Indicates that the user has selected the "back" button in one of the dialogs.
	*/
	protected boolean backPressed = false;

	/**
	* Informs if the user has selected the "back" button in the last dialog.
	*/
	public boolean backButtonPressed() {
		return backPressed;
	}

	/**
	 * If provided, may be used for finding currently existing layers and tables.
	 * It is possible to use the names of selected objects from a layer or a
	 * table as parameter values, if they can be correctly transformed
	 */
	public void setDataKeeper(DataKeeper dataKeeper) {
		this.dataKeeper = dataKeeper;
	}

	/**
	 * If available, may be used for taking selected objects from a layer or a
	 * table and trying to treat their names as parameter values
	 */
	public void setSupervisor(Supervisor supervisor) {
		this.supervisor = supervisor;
	}

	/**
	* Allows to choose either only one or more top-level attributes
	*/
	public void setSelectOnlyOne(boolean value) {
		selectOnlyOne = value;
	}

	/**
	* Sets whether it is necessary to offer the user to change the default
	* column order. By default is true.
	*/
	public void setColumnsMayBeReordered(boolean value) {
		columnsMayBeReordered = value;
	}

	/**
	* Allows the user to select top-level attributes. Returns a vector with the
	* chosen attributes, i.e. instances of spade.vis.database.Attribute.
	* @param table - the table to select attributes from
	* @param prompt - a text to be shown to the user
	*/
	public Vector selectTopLevelAttributes(AttributeDataPortion table, String prompt, SystemUI ui) {
		return selectTopLevelAttributes(table, null, null, false, prompt, ui);
	}

	/**
	* Allows the user to select top-level attributes. Returns a vector with the
	* chosen attributes, i.e. instances of spade.vis.database.Attribute.
	* @param table - the table to select attributes from
	* @param selectAttrIds - identifiers of the attributes that must be pre-selected
	* @param excludeAttrIds - identifiers of the attributes that must not be
	*                         proposed for selection
	* @param onlyNumeric - allows selection of only numeric attributes
	* @param prompt - a text to be shown to the user
	*/
	public Vector selectTopLevelAttributes(AttributeDataPortion table, Vector selectAttrIds, Vector excludeAttrIds, boolean onlyNumeric, String prompt, SystemUI ui) {
		backPressed = false;
		pvsel = null;
		selAttr = null;
		attrDescr = null;
		selColumns = null;
		if (table == null)
			return null;
		this.table = table;
		if (!table.hasData()) {
			table.loadData();
		}
		if (!table.hasData())
			return null;
		if (ui != null) {
			mainFrame = ui.getMainFrame();
		}
		if (mainFrame == null) {
			mainFrame = CManager.getAnyFrame();
		}
		rootSP = new RootAttrSelectPanel(table, selectAttrIds, excludeAttrIds, onlyNumeric, selectOnlyOne, prompt);
		OKDialog okd = new OKDialog(mainFrame, res.getString("Select_attributes"), true, false);
		okd.addContent(rootSP);
		okd.show();
		if (okd.wasCancelled())
			return null;
		selAttr = rootSP.getSelectedAttributes();
		buildAttrDescriptors();
		return selAttr;
	}

	/**
	* Returns the vector of the selected attributes.
	* The elements of the vector are instances of the class
	* spade.vis.database.Attribute.
	*/
	public Vector getSelectedAttributes() {
		return selAttr;
	}

	/**
	* For the user-selected top-level attributes creates a vector of corresponding
	* attribute descriptors, instances of the class spade.vis.database.AttrDescriptor,
	* and stores it in the internal variable attrDescr.
	*/
	protected void buildAttrDescriptors() {
		if (selAttr != null && selAttr.size() > 0) {
			attrDescr = new Vector(selAttr.size(), 1);
			for (int i = 0; i < selAttr.size(); i++) {
				AttrDescriptor ad = new AttrDescriptor();
				ad.attr = (Attribute) selAttr.elementAt(i);
				attrDescr.addElement(ad);
			}
		}
	}

	/**
	* Excludes the attributes with the specified indexes from the further
	* consideration.
	*/
	public void excludeAttributes(IntArray attrIdxs) {
		if (selAttr == null || selAttr.size() < 1 || attrIdxs == null || attrIdxs.size() < 1)
			return;
		Vector attr = new Vector(selAttr.size(), 1), adescr = null;
		if (attrDescr != null && attrDescr.size() == selAttr.size()) {
			adescr = new Vector(selAttr.size(), 1);
		}
		for (int i = 0; i < selAttr.size(); i++)
			if (attrIdxs.indexOf(i) < 0) { //should not be excluded
				attr.addElement(selAttr.elementAt(i));
				if (adescr != null) {
					adescr.addElement(attrDescr.elementAt(i));
				}
			}
		if (attr.size() > 0 && attr.size() < selAttr.size()) {
			selAttr = attr;
			attrDescr = adescr;
			selColumns = null;
			pvsel = null;
		}
	}

	/**
	* Returns the descriptors of the selected attributes: the attributes and the
	* relevant values of all parameters this attribute depends on (no selection
	* of parameter values is done).
	* The elements of the resulting vector are instances of the class
	* spade.vis.database.AttrDescriptor.
	*/
	public Vector getAttrDescriptors() {
		if (selAttr == null || selAttr.size() < 1)
			return null;
		if (attrDescr == null || attrDescr.size() != selAttr.size()) {
			buildAttrDescriptors();
		}
		if (table.getParamCount() > 0) {
			for (int i = 0; i < attrDescr.size(); i++) {
				AttrDescriptor ad = (AttrDescriptor) attrDescr.elementAt(i);
				if ((ad.parVals == null || ad.children == null) && ad.attr.hasChildren()) {
					completeAttrDescriptor(ad, table.getParameters());
				}
			}
		}
		return attrDescr;
	}

	/**
	* If one or more of the user-selected attributes depends on parameters,
	* allows the user to select values of the parameters. Some parameters (e.g.
	* the temporal parameter) may be excluded from the selection process, for
	* example, to be handled individually. Returns the descriptors of the selected
	* attributes: the attributes and the corresponding parameter values.
	* The elements of the resulting vector are instances of the class
	* spade.vis.database.AttrDescriptor.
	* @param paramsToExclude - the vector of parameters that should be excluded
	*                          from the selection process. The elements of the
	*                          vector are instances of the class
	*                          spade.vis.database.Parameter.
	*/
	public Vector selectParamValues(Vector paramsToExclude) {
		backPressed = false;
		selColumns = null;
		if (selAttr == null || selAttr.size() < 1)
			return null;
		if (paramsToExclude != null && paramsToExclude.size() < 1) {
			paramsToExclude = null;
		}
		//if there are no parameter-dependent attributes among the selected
		//attributes, return this vector without changes
		Vector params = null;
		if (table.getParamCount() > 0) {
			if (paramsToExclude == null) {
				params = table.getParameters();
			} else {
				params = new Vector(table.getParamCount(), 1);
				for (int i = 0; i < table.getParamCount(); i++)
					if (!paramsToExclude.contains(table.getParameter(i))) {
						params.addElement(table.getParameter(i));
					}
			}
			if (params.size() < 1) {
				params = null;
			}
		}
		IntArray depIdxs = null;
		if (params != null) {
			depIdxs = ParamValSelectPanel.getParamDependentAttrs(selAttr, params);
			if (depIdxs != null && depIdxs.size() < 1) {
				depIdxs = null;
			}
		}
		if (attrDescr == null || attrDescr.size() != selAttr.size()) {
			buildAttrDescriptors();
		}
		if (table.getParamCount() > 0) {
			for (int i = 0; i < attrDescr.size(); i++)
				if (depIdxs == null || depIdxs.indexOf(i) < 0) {
					completeAttrDescriptor((AttrDescriptor) attrDescr.elementAt(i), table.getParameters());
				}
		}
		if (depIdxs == null)
			return attrDescr;
		Vector depAttr = new Vector(depIdxs.size(), 1);
		for (int i = 0; i < depIdxs.size(); i++) {
			depAttr.addElement(selAttr.elementAt(depIdxs.elementAt(i)));
		}
		if (pvsel == null) {
			pvsel = new ParamValSelectPanel(depAttr, params, dataKeeper, supervisor);
		}
		OKDialog okd = new OKDialog(mainFrame, res.getString("select_param_values"), true, true);
		okd.addContent(pvsel);
		okd.show();
		if (okd.wasCancelled()) {
			selAttr = null;
			attrDescr = null;
			return null;
		}
		if (okd.wasBackPressed()) {
			backPressed = true;
			pvsel = null;
			return null;
		}
		for (int i = 0; i < depIdxs.size(); i++) {
			AttrDescriptor ad = (AttrDescriptor) attrDescr.elementAt(depIdxs.elementAt(i));
			ad.parVals = pvsel.getParValuesForAttr(i);
			if (ad.parVals != null && params.size() < table.getParamCount()) {
				Vector pv[] = ad.attr.getAllParametersAndValues(table.getParameters());
				for (int j = 0; j < pv.length; j++) {
					boolean found = false;
					for (int k = 0; k < ad.parVals.length && !found; k++)
						if (pv[j].elementAt(0).equals(ad.parVals[k].elementAt(0))) {
							found = true;
							pv[j] = ad.parVals[k];
						}
				}
				ad.parVals = pv;
			}
			completeAttrDescriptor(ad, table.getParameters());
		}
		return attrDescr;
	}

	/**
	* Completes the given attribute descriptor by finding the relevant values of
	* the specified parameters and the children corresponding to the possible
	* combinations of parameter values.
	* @param params - the vector of parameters. The elements of the vector are
	*                 instances of the class spade.vis.database.Parameter.
	*/
	protected void completeAttrDescriptor(AttrDescriptor ad, Vector params) {
		if (ad == null || ad.attr == null || !ad.attr.hasChildren())
			return;
		if (params == null)
			return;
		ad.children = null;
		if (ad.parVals == null || ad.parVals.length < 1) {
			ad.parVals = ad.attr.getAllParametersAndValues(params);
		}
		if (ad.parVals == null || ad.parVals.length < 1) {
			ad.parVals = null;
			return;
		}
		//generate all possible combinations of parameter values
		Vector comb = new Vector(100, 100);
		for (Vector parVal : ad.parVals)
			if (comb.size() < 1) {
				for (int k = 1; k < parVal.size(); k++) {
					Object v[] = new Object[1];
					v[0] = parVal.elementAt(k);
					comb.addElement(v);
				}
			} else {
				Vector res = new Vector(comb.size() * parVal.size(), 100);
				for (int k = 0; k < comb.size(); k++) {
					Object v[] = (Object[]) comb.elementAt(k);
					for (int l = 1; l < parVal.size(); l++) {
						Object v1[] = new Object[v.length + 1];
						for (int n = 0; n < v.length; n++) {
							v1[n] = v[n];
						}
						v1[v.length] = parVal.elementAt(l);
						res.addElement(v1);
					}
				}
				comb = res;
			}
		String parNames[] = new String[ad.parVals.length];
		for (int j = 0; j < ad.parVals.length; j++) {
			parNames[j] = (String) ad.parVals[j].elementAt(0);
		}
		ad.children = new Vector(ad.attr.getChildrenCount(), 1);
		for (int j = 0; j < comb.size(); j++) {
			Object values[] = (Object[]) comb.elementAt(j);
			//find the child corresponding to this value combination
			boolean found = false;
			for (int n = 0; n < ad.attr.getChildrenCount() && !found; n++) {
				Attribute child = ad.attr.getChild(n);
				boolean ok = true;
				for (int nv = 0; nv < values.length && ok; nv++) {
					ok = child.hasParamValue(parNames[nv], values[nv]);
				}
				if (ok) {
					ad.children.addElement(child);
					found = true;
				}
			}
		}
	}

	/**
	* Returns the selected low-level attributes. Each attribute corresponds to a
	* single table column. The resulting vector consists of instances of
	* spade.vis.database.Attribute.
	*/
	protected Vector getColumns() {
		selColumns = null;
		if (selAttr == null || selAttr.size() < 1)
			return null;
		if (attrDescr == null || attrDescr.size() < 1) {
			buildAttrDescriptors();
		}
		if (attrDescr == null || attrDescr.size() < 1)
			return null;
		selColumns = new Vector(attrDescr.size(), 1);
		for (int i = 0; i < attrDescr.size(); i++) {
			AttrDescriptor ad = (AttrDescriptor) attrDescr.elementAt(i);
			if (ad.children == null || ad.children.size() < 1) {
				selColumns.addElement(ad.attr);
			} else {
				for (int j = 0; j < ad.children.size(); j++) {
					selColumns.addElement(ad.children.elementAt(j));
				}
			}
		}
		return selColumns;
	}

	/**
	* Supports the complete process of column selection, i.e. from the top level
	* down to the level of columns. Returns a vector of column attributes,
	* i.e. instances of spade.vis.database.Attribute.
	* @param table - the table to select attributes from
	* @param prompt - a text to be shown to the user
	*/
	public Vector selectColumns(AttributeDataPortion table, String prompt, SystemUI ui) {
		return selectColumns(table, null, null, false, prompt, ui);
	}

	/**
	* Supports the complete process of column selection, i.e. from the top level
	* down to the level of columns. Returns a vector of column attributes,
	* @param table - the table to select attributes from
	* @param selectAttrIds - identifiers of the attributes that must be pre-selected
	* @param excludeAttrIds - identifiers of the attributes that must not be
	*                         proposed for selection
	* @param onlyNumeric - allows selection of only numeric attributes
	* @param prompt - a text to be shown to the user
	*/
	public Vector selectColumns(AttributeDataPortion table, Vector selectAttrIds, Vector excludeAttrIds, boolean onlyNumeric, String prompt, SystemUI ui) {
		if (table == null)
			return null;
		do {
			selectTopLevelAttributes(table, selectAttrIds, excludeAttrIds, onlyNumeric, prompt, ui);
			if (selAttr == null || selAttr.size() < 1)
				return null;
			if (selectParamValues(null) == null && !backPressed)
				return null;
		} while (backPressed);
		getColumns();
		if (columnsMayBeReordered) {
			getColumnOrder();
		}
		return selColumns;
	}

	/**
	* Show the user the resulting list of low-level attributes (columns)
	* and allow to edit the order of attributes.
	*/
	protected void getColumnOrder() {
		if (selColumns == null || selColumns.size() < 2)
			return;
		Panel p = new Panel();
		ListOrderer lord = null;
		p.setLayout(new BorderLayout());
		p.add(new Label(res.getString("attributes_selected_")), BorderLayout.NORTH);
		lord = new ListOrderer(selColumns);
		p.add(lord, BorderLayout.CENTER);
		OKDialog okd = new OKDialog(mainFrame, res.getString("Attributes"), true, false);
		okd.addContent(p);
		okd.show();
		if (okd.wasCancelled())
			return;
		if (lord != null) {
			selColumns = lord.getOrderedItems();
		}
	}

	/**
	* Returns the attributes corresponding to the selected columns
	*/
	public Vector getSelectedColumns() {
		if (selColumns == null || selColumns.size() < 1) {
			getColumns();
		}
		return selColumns;
	}

	/**
	* Returns the identifiers of the selected columns
	*/
	public Vector getSelectedColumnIds() {
		if (selColumns == null || selColumns.size() < 1) {
			getColumns();
		}
		if (selColumns == null || selColumns.size() < 1)
			return null;
		Vector attr = new Vector(selColumns.size(), 1);
		for (int i = 0; i < selColumns.size(); i++) {
			attr.addElement(((Attribute) selColumns.elementAt(i)).getIdentifier());
		}
		return attr;
	}
}
