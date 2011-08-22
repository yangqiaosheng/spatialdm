package ui;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.TabbedPanel;
import spade.lib.lang.Language;
import spade.lib.ui.ParamValSelector;
import spade.lib.util.IntArray;
import spade.vis.database.Attribute;
import spade.vis.database.Parameter;

/**
* A UI for selection of parameter values for parameter-dependent attributes.
*/
class ParamValSelectPanel extends Panel implements ItemListener {
	static ResourceBundle res = Language.getTextResource("ui.Res");
	/**
	* The initial list of attributes. May contain both parameter-dependent and
	* parameter-independent attributes.
	*/
	protected Vector attrs = null;
	/**
	* The list of parameter-dependent attributes
	*/
	protected Vector depAttrs = null;
	/**
	* The numbers of the parameter-dependent attributes in the initial list of
	* all attributes
	*/
	protected IntArray idxs = null;
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
	* Selectors of parameter values for each parameter-dependent attribute
	*/
	protected ParamValSelector psel[] = null;
	protected TabbedPanel tpa = null;
	protected Checkbox separateSelectCB = null;
	protected Panel header = null;

	/**
	* From the given list of attributes, selects only attributes which depend on
	* one or more of the specified parameters. Returns an aray with the indexes
	* of the parameter-dependent attributes in the initial list of attributes or
	* null if there are no such attributes.
	* @param attrs - vector of instances of spade.vis.database.Attribute.
	* @param params - vector of instances of spade.vis.database.Parameter.
	*/
	public static IntArray getParamDependentAttrs(Vector attrs, Vector params) {
		if (attrs == null || attrs.size() < 1 || params == null || params.size() < 1)
			return null;
		IntArray idxs = new IntArray(attrs.size(), 1);
		for (int i = 0; i < attrs.size(); i++) {
			Attribute at = (Attribute) attrs.elementAt(i);
			if (at.getChildrenCount() < 2) {
				continue;
			}
			boolean depends = false;
			for (int j = 0; j < params.size() && !depends; j++) {
				depends = at.dependsOnParameter((Parameter) params.elementAt(j));
			}
			if (depends) {
				idxs.addElement(i);
			}
		}
		if (idxs.size() < 1)
			return null;
		return idxs;
	}

	/**
	* Constructs the UI for the selection of parameter values. The vector attrs
	* must consist of instances of spade.vis.database.Attribute. The vector params
	* contains all parameters of the table as instances of spade.vis.database.Parameter
	*/
	public ParamValSelectPanel(Vector attrs, Vector params, DataKeeper dataKeeper, Supervisor supervisor) {
		this.dataKeeper = dataKeeper;
		this.supervisor = supervisor;
		if (attrs == null || attrs.size() < 1) {
			makeErrorUI(res.getString("no_attributes"));
			return;
		}
		if (params == null || params.size() < 1) {
			makeErrorUI(res.getString("no_params"));
			return;
		}
		idxs = getParamDependentAttrs(attrs, params);
		if (idxs == null || idxs.size() < 1) {
			idxs = null;
			makeErrorUI(res.getString("no_par_dep_attr"));
			return;
		}
		this.attrs = attrs;
		depAttrs = new Vector(idxs.size(), 1);
		for (int i = 0; i < idxs.size(); i++) {
			depAttrs.addElement(attrs.elementAt(idxs.elementAt(i)));
		}
		boolean sameParameters = true;
		if (depAttrs.size() > 1) {
			//check if the attributes depend on the same parameters
			Vector paramVals0[] = ((Attribute) depAttrs.elementAt(0)).getAllParametersAndValues(params);
			for (int i = 1; i < depAttrs.size() && sameParameters; i++) {
				Vector paramVals[] = ((Attribute) depAttrs.elementAt(i)).getAllParametersAndValues(params);
				sameParameters = haveSameParamsAndValues(paramVals0, paramVals);
			}
		}
		psel = new ParamValSelector[(sameParameters) ? 1 : depAttrs.size()];
		header = new Panel(new ColumnLayout());
		if (depAttrs.size() == 1) {
			header.add(new Label(res.getString("Attribute")));
			Attribute at = (Attribute) depAttrs.elementAt(0);
			header.add(new Label(at.getName(), Label.CENTER));
			Vector paramVals[] = at.getAllParametersAndValues(params);
			int npar = paramVals.length;
			header.add(new Label((npar == 1) ? res.getString("depends_on_1") : res.getString("depends_on_2"), Label.CENTER));
			header.add(new Line(false));
			header.add(new Label((npar == 1) ? res.getString("select_values_1") + ":" : res.getString("select_values_2") + ":", Label.CENTER));
		} else if (sameParameters) {
			header.add(new Label(res.getString("Attributes")));
			Panel p = null;
			if (depAttrs.size() > 5) {
				ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
				header.add(scp);
				p = new Panel(new ColumnLayout());
				scp.add(p);
			}
			for (int i = 0; i < depAttrs.size(); i++) {
				Attribute at = (Attribute) depAttrs.elementAt(i);
				Label lab = new Label(at.getName(), Label.CENTER);
				if (p == null) {
					header.add(lab);
				} else {
					p.add(lab);
				}
			}
			Vector paramVals[] = ((Attribute) depAttrs.elementAt(0)).getAllParametersAndValues(params);
			int npar = paramVals.length;
			header.add(new Label((npar == 1) ? res.getString("depend_on_1") : res.getString("depend_on_2"), Label.CENTER));
			header.add(new Line(false));
			header.add(new Label((npar == 1) ? res.getString("select_values_1") + ":" : res.getString("select_values_2") + ":", Label.CENTER));
		} else {
			header.add(new Label(res.getString("Some_chosen_attr"), Label.CENTER));
			header.add(new Label(res.getString("depend_on_params"), Label.CENTER));
			header.add(new Label(res.getString("select_values_3") + ":", Label.CENTER));
		}
		if (depAttrs.size() > 1 && !sameParameters) {
			tpa = new TabbedPanel();
		}
		int k = 0;
		for (int i = 0; i < ((sameParameters) ? 1 : depAttrs.size()); i++) {
			Attribute at = (Attribute) depAttrs.elementAt(i);
			Vector paramVals[] = at.getAllParametersAndValues(params);
			psel[i] = new ParamValSelector((depAttrs.size() == 1) ? header : null, paramVals, dataKeeper, supervisor);
			if (tpa != null) {
				tpa.addComponent(at.getName(), psel[i]);
			}
		}
		setLayout(new BorderLayout());
		if (depAttrs.size() > 1) {
			add(header, BorderLayout.NORTH);
			if (tpa != null) {
				add(tpa, BorderLayout.CENTER);
				tpa.makeLayout();
			} else {
				add(psel[0], BorderLayout.CENTER);
			}
			if (sameParameters) {
				separateSelectCB = new Checkbox(res.getString("individually"), false);
				separateSelectCB.addItemListener(this);
				add(separateSelectCB, BorderLayout.SOUTH);
			}
		} else {
			add(psel[0], BorderLayout.CENTER);
		}
	}

	/**
	* Checks if the given two arrays contain the same parameters and values. The
	* elements of the arrays are vectors, in which the first elements are the
	* names of parameters and the rest are values.
	*/
	protected boolean haveSameParamsAndValues(Vector params0[], Vector params1[]) {
		if (params0 == null || params1 == null)
			return false;
		if (params0.length != params1.length)
			return false;
		for (Vector element : params0) {
			String name = (String) element.elementAt(0);
			//find the vector in params1 with the same name at the beginning
			Vector pv1 = null;
			for (int j = 0; j < params1.length && pv1 == null; j++)
				if (name.equalsIgnoreCase((String) params1[j].elementAt(0))) {
					pv1 = params1[j];
				}
			if (pv1 == null)
				return false;
			if (pv1.size() != element.size())
				return false;
			for (int j = 1; j < pv1.size(); j++) {
				Object elem = pv1.elementAt(j);
				boolean found = false;
				for (int k = 1; k < pv1.size() && !found; k++) {
					found = elem.equals(element.elementAt(k));
				}
				if (!found)
					return false;
			}
		}
		return true;
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
	* Switches between separate selection of parameter values for each attribute
	* and simultaneous selection (if all attributes depend on the same parameters)
	*/
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (separateSelectCB != null && e.getSource().equals(separateSelectCB)) {
			setVisible(false);
			if (separateSelectCB.getState()) {
				ParamValSelector ps0 = psel[0];
				if (psel.length < depAttrs.size()) {
					Vector paramVals[] = ps0.getParameters();
					psel = new ParamValSelector[depAttrs.size()];
					psel[0] = ps0;
					for (int i = 1; i < depAttrs.size(); i++) {
						psel[i] = new ParamValSelector(null, paramVals, dataKeeper, supervisor);
					}
				}
				tpa = new TabbedPanel();
				remove(ps0);
				for (int i = 0; i < psel.length; i++) {
					tpa.addComponent(((Attribute) depAttrs.elementAt(i)).getName(), psel[i]);
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
	* Returns the selected parameter values for the attribute with the given index.
	* Each vector in the array contains the parameter name as its first element
	* and the values following the name.
	*/
	public Vector[] getParValuesForAttr(int attrIdx) {
		if (depAttrs == null || depAttrs.size() < 1 || attrIdx < 0 || attrIdx >= attrs.size())
			return null;
		Attribute at = (Attribute) attrs.elementAt(attrIdx);
		int k = idxs.indexOf(attrIdx);
		if (k < 0)
			return null;
		ParamValSelector ps = psel[0];
		if (k > 0 && (separateSelectCB == null || separateSelectCB.getState())) {
			ps = psel[k];
		}
		Vector params[] = ps.getParameters();
		if (params == null || params.length < 1)
			return null;
		Vector parVals[] = new Vector[params.length];
		for (int j = 0; j < params.length; j++) {
			parVals[j] = ps.getSelectedParamValues(j);
			parVals[j].insertElementAt(params[j].elementAt(0), 0);
		}
		return parVals;
	}

	/**
	* Returns the list of "children" attributes corresponding to the selected
	* parameter values. Each attribute corresponds to a single table column.
	*/
	public Vector getColumns() {
		if (depAttrs == null || depAttrs.size() < 1)
			return attrs;
		Vector columns = new Vector(100, 100);
		for (int i = 0; i < attrs.size(); i++) {
			Attribute at = (Attribute) attrs.elementAt(i);
			int k = idxs.indexOf(i);
			if (k < 0) {
				if (at.getChildrenCount() == 1) {
					columns.addElement(at.getChild(0));
				} else {
					columns.addElement(at);
				}
			} else {
				ParamValSelector ps = psel[0];
				if (k > 0 && (separateSelectCB == null || separateSelectCB.getState())) {
					ps = psel[k];
				}
				Vector params[] = ps.getParameters();
				String parNames[] = new String[params.length];
				for (int j = 0; j < params.length; j++) {
					parNames[j] = (String) params[j].elementAt(0);
				}
				Vector comb = ps.getParamValCombinations();
				for (int j = 0; j < comb.size(); j++) {
					Object values[] = (Object[]) comb.elementAt(j);
					//find the child corresponding to this value combination
					boolean found = false;
					for (int n = 0; n < at.getChildrenCount() && !found; n++) {
						Attribute child = at.getChild(n);
						boolean ok = true;
						for (int nv = 0; nv < values.length && ok; nv++) {
							ok = child.hasParamValue(parNames[nv], values[nv]);
						}
						if (ok) {
							columns.addElement(child);
							found = true;
						}
					}
				}
			}
		}
		return columns;
	}
	/*
	public Dimension getPreferredSize () {
	  Dimension d;
	  for (int i=0; i<getComponentCount(); i++) {
	    Component c=getComponent(i);
	    d=c.getPreferredSize();
	    System.out.println(c.getName()+" ("+c.getClass().getName()+"): "+d.height);
	  }
	  d=super.getPreferredSize();
	  System.out.println("ParamValSelectPanel: "+d.height);
	  return d;
	}
	*/
}