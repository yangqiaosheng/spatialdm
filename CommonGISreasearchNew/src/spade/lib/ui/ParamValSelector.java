package spade.lib.ui;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.MultiSelector;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.TabbedPanel;
import spade.lib.basicwin.TimeParameterSelector;
import spade.lib.util.IntArray;
import spade.time.TimeMoment;
import spade.vis.action.Highlighter;
import spade.vis.database.AttributeDataPortion;

/**
* A UI for selection of specific parameter values for a parameter-dependent
* attribute.
*/
public class ParamValSelector extends Panel implements ActionListener {
	/**
	* The source information about parameters and their values
	*/
	protected Vector params[] = null;
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
	* The selectors for each individual parameter
	*/
	// protected MultiSelector msel[]=null;
	protected Panel msel[] = null;

	protected TabbedPanel tabPanel = null;

	/**
	* For the given array of vectors containing names (first elements) and values
	* (the rest of each vector) of the parameters, creates a UI for selection
	* of particular values.
	*/
	public ParamValSelector(Component top, Vector params[]) {
		this(top, params, null, null);
	}

	/**
	* For the given array of vectors containing names (first elements) and values
	* (the rest of each vector) of the parameters, creates a UI for selection
	* of particular values.
	*/
	public ParamValSelector(Component top, Vector params[], DataKeeper dataKeeper, Supervisor supervisor) {
		if (params == null)
			return;
		this.params = params;
		int npar = params.length;
		this.dataKeeper = dataKeeper;
		this.supervisor = supervisor;

		setLayout(new ColumnLayout());
		if (top != null) {
			add(top);
		}
		if (npar > 1) {
			tabPanel = new TabbedPanel();
			add(tabPanel);
		}
		//msel=new MultiSelector[npar];
		msel = new Panel[npar];
		for (int i = 0; i < npar; i++) {
			Vector v = params[i];
			String name = (String) v.elementAt(0);
			Vector values = (Vector) v.clone();
			values.removeElementAt(0);

/*
      boolean containsTimeParams=true;
      for (int j=0; j<values.size() && containsTimeParams; j++)
        containsTimeParams=(values.elementAt(j) instanceof spade.time.TimeMoment);

      if (containsTimeParams)
        msel[i]=new TimeParameterSelector(values,false);
      else
        msel[i]=new MultiSelector(values,false,true);
      //this is removed because TimeParameterSelector is not needed here!
*/
			msel[i] = new MultiSelector(values, false, true);

			//msel[i].setAdvancedSelectorThreshold(10);
			if (tabPanel != null) {
				tabPanel.addComponent(name, msel[i]);
			} else {
				add(new Label(name));
				add(msel[i]);
			}
		}
		if (tabPanel != null) {
			tabPanel.makeLayout();
		}
		if (dataKeeper != null && supervisor != null) {
			add(new Line(false));
			Button b = new Button("Take names of currently selected objects");
			b.setActionCommand("use_sel_objects");
			b.addActionListener(this);
			Panel p = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 3));
			p.add(b);
			add(p);
		}
	}

	/**
	* For the given parameter (specified by its index in the list of parameters),
	* selects the values with the specified indexes.
	*/
	public void selectParamValues(int paramIdx, int valIdxs[]) {
		if (params != null && paramIdx >= 0 && paramIdx < params.length) {
			if (msel[paramIdx] instanceof MultiSelector) {
				((MultiSelector) msel[paramIdx]).selectItems(valIdxs);
			}
			if (msel[paramIdx] instanceof TimeParameterSelector) {
				((TimeParameterSelector) msel[paramIdx]).selectItemsAbsolute(valIdxs);
			}
		}
	}

	/**
	 * Reacts to the button "Take names of currently selected objects"
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() != null && e.getActionCommand().equals("use_sel_objects")) {
			if (dataKeeper == null || supervisor == null)
				return;
			List tblList = new List(Math.min(dataKeeper.getTableCount() + 1, 10));
			for (int i = 0; i < dataKeeper.getTableCount(); i++) {
				tblList.add(dataKeeper.getTable(i).getName());
			}
			tblList.select(tblList.getItemCount() - 1);
			Panel p = new Panel(new BorderLayout());
			p.add(tblList, BorderLayout.CENTER);
			p.add(new Label("From which table must the selected objects be taken?"), BorderLayout.NORTH);
			OKDialog dia = new OKDialog(CManager.getAnyFrame(), "Select the table", true);
			dia.addContent(p);
			dia.show();
			if (dia.wasCancelled())
				return;
			int idx = tblList.getSelectedIndex();
			if (idx < 0)
				return;
			AttributeDataPortion tbl = dataKeeper.getTable(idx);
			if (tbl.getDataItemCount() < 1) {
				Dialogs.showMessage(CManager.getAnyFrame(), "The table does not contain any objects!", "No objects!");
				return;
			}
			Highlighter hl = supervisor.getHighlighter(tbl.getEntitySetIdentifier());
			if (hl == null) {
				Dialogs.showMessage(CManager.getAnyFrame(), "No highlighter found for the objects of the table!", "No highlighter!");
				return;
			}
			Vector selObj = hl.getSelectedObjects();
			if (selObj == null || selObj.size() < 1) {
				Dialogs.showMessage(CManager.getAnyFrame(), "None of the objects is currently selected!", "No selection!");
				return;
			}
			List objList = new List(Math.min(10, selObj.size() + 1), true);
			for (int i = 0; i < selObj.size(); i++) {
				idx = tbl.indexOf(selObj.elementAt(i).toString());
				if (idx >= 0) {
					objList.add(tbl.getDataItemName(idx));
					objList.select(objList.getItemCount() - 1);
				}
			}
			if (objList.getItemCount() < 1) {
				Dialogs.showMessage(CManager.getAnyFrame(), "None of the objects is currently selected!", "No selection!");
				return;
			}
			p = new Panel(new BorderLayout());
			p.add(objList, BorderLayout.CENTER);
			p.add(new Label("Take the names of the following objects:"), BorderLayout.NORTH);
			dia = new OKDialog(CManager.getAnyFrame(), "Select the objects", true);
			dia.addContent(p);
			dia.show();
			if (dia.wasCancelled())
				return;
			String names[] = objList.getSelectedItems();
			if (names == null || names.length < 1)
				return;
			//try to transform the names into parameter values
			int parIdx = 0;
			if (params.length > 1 && tabPanel != null) {
				parIdx = tabPanel.getActiveTabN();
			}
			Vector v = params[parIdx];
			Vector values = (Vector) v.clone();
			values.removeElementAt(0);
			Vector<TimeMoment> times = null;
			if (values.elementAt(0) instanceof TimeMoment) {
				TimeMoment t = (TimeMoment) values.elementAt(0);
				TimeMoment t1 = t.getCopy();
				times = new Vector<TimeMoment>(names.length, 10);
				for (String name2 : names)
					if (t1.setMoment(name2)) {
						times.addElement(t1);
						t1 = t.getCopy();
					}
				if (times.size() < 1) {
					Dialogs.showMessage(CManager.getAnyFrame(), "The names of the objects cannot be transformed to time moments!", "Not time moments!");
					return;
				}
			}
			IntArray valIdxs = new IntArray(names.length, 10);
			for (int i = 0; i < values.size(); i++) {
				boolean found = false;
				if (times != null && (values.elementAt(i) instanceof TimeMoment)) {
					TimeMoment t = (TimeMoment) values.elementAt(i);
					for (int j = 0; j < times.size() && !found; j++) {
						found = t.equals(times.elementAt(j));
					}
				} else {
					String val = values.elementAt(i).toString();
					for (int j = 0; j < names.length && !found; j++) {
						found = val.equalsIgnoreCase(names[j]);
					}
				}
				if (found) {
					valIdxs.addElement(i);
				}
			}
			if (valIdxs.size() < 1) {
				Dialogs.showMessage(CManager.getAnyFrame(), "No valid parameter values occur among the names of the selected objects!", "No valid parameter values!");
				return;
			}
			selectParamValues(parIdx, valIdxs.getTrimmedArray());
		}
	}

	/**
	* Returns the indexes of the selected values of the parameter with the given index
	*/
	public int[] getSelectedValueIndexes(int parIdx) {
		if (params == null || parIdx < 0 || parIdx >= params.length)
			return null;
		if (msel[parIdx] instanceof TimeParameterSelector)
			return ((TimeParameterSelector) msel[parIdx]).getSelectedIndexes();
		if (msel[parIdx] instanceof MultiSelector)
			return ((MultiSelector) msel[parIdx]).getSelectedIndexes();
		return null;
	}

	/**
	* Returns a vector of selected values of the parameter with the given index
	*/
	public Vector getSelectedParamValues(int parIdx) {
		if (params == null || parIdx < 0 || parIdx >= params.length)
			return null;
		int selIdx[] = null;
		if (msel[parIdx] instanceof TimeParameterSelector) {
			selIdx = ((TimeParameterSelector) msel[parIdx]).getSelectedIndexes();
		} else if (msel[parIdx] instanceof MultiSelector) {
			selIdx = ((MultiSelector) msel[parIdx]).getSelectedIndexes();
		}
		if (selIdx == null || selIdx.length < 1) {
			selIdx = new int[params[parIdx].size() - 1];
			for (int j = 0; j < selIdx.length; j++) {
				selIdx[j] = j;
			}
		}
		Vector paramValues = new Vector(selIdx.length, 1);
		for (int element : selIdx) {
			paramValues.addElement(params[parIdx].elementAt(1 + element));
		}
		return paramValues;
	}

	/**
	* Returns all selected combinations of values of the parameters
	*/
	public Vector getParamValCombinations() {
		if (params == null)
			return null;
		int npar = params.length;
		Vector result = new Vector(100, 100);
		for (int i = 0; i < npar; i++) {
			int selIdx[] = null;
			if (msel[i] instanceof TimeParameterSelector) {
				selIdx = ((TimeParameterSelector) msel[i]).getSelectedIndexes();
			} else if (msel[i] instanceof MultiSelector) {
				selIdx = ((MultiSelector) msel[i]).getSelectedIndexes();
			}

			if (selIdx == null) {
				selIdx = new int[params[i].size() - 1];
				for (int j = 0; j < selIdx.length; j++) {
					selIdx[j] = j;
				}
			}
			if (result.size() < 1) {
				for (int element : selIdx) {
					Object v[] = new Object[1];
					v[0] = params[i].elementAt(1 + element);
					result.addElement(v);
				}
			} else {
				Vector res = new Vector(result.size() * selIdx.length, 100);
				for (int k = 0; k < result.size(); k++) {
					Object v[] = (Object[]) result.elementAt(k);
					for (int element : selIdx) {
						Object v1[] = new Object[v.length + 1];
						for (int n = 0; n < v.length; n++) {
							v1[n] = v[n];
						}
						v1[v.length] = params[i].elementAt(1 + element);
						res.addElement(v1);
					}
				}
				result = res;
			}
		}
		return result;
	}

	/**
	* Returns the list of parameters and values it works with, i.e. an array of
	* vectors containing names (first elements) and values (the rest of each
	* vector) of the parameters
	*/
	public Vector[] getParameters() {
		return params;
	}

	/**
	* For some strange reason, without this method the preferred height of the
	* panel is about two times more than required.
	*/
	@Override
	public Dimension getPreferredSize() {
		Dimension d;
		for (int i = 0; i < getComponentCount(); i++) {
			Component c = getComponent(i);
			d = c.getPreferredSize();
			//System.out.println(c.getName()+" ("+c.getClass().getName()+"): "+d.height);
		}
		d = super.getPreferredSize();
		//System.out.println("ParamValSelector: "+d.height);
		return d;
	}
	/**/
}
