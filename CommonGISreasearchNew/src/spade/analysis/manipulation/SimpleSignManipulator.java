package spade.analysis.manipulation;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.ActionCanvas;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.TextCanvas;
import spade.lib.util.BubbleSort;
import spade.lib.util.ObjectWithCount;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTable;
import spade.vis.database.QualAttrCondition;
import spade.vis.database.TableFilter;
import spade.vis.geometry.SimpleSign;
import spade.vis.mapvis.SimpleSignPresenter;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Mar 9, 2010
 * Time: 1:03:14 PM
 * Manipulates visualization by SimpleSignPresenter, which represents values of a
 * qualitative attribute by simple icons varying only in shape.
 */
public class SimpleSignManipulator extends Panel implements Manipulator, PropertyChangeListener, ActionListener, ItemListener, Destroyable {
	protected Supervisor sup = null;
	protected SimpleSignPresenter sPres = null;
	protected DataTable dataTable = null;
	/**
	 * The filter to remove objects with certain values from view
	 */
	protected TableFilter tFilter = null;
	protected QualAttrCondition qac = null;
	//the order of the values
	protected Vector<ObjectWithCount> valOrder = null;

	protected Checkbox cbs[] = null;
	protected ActionCanvas acs[] = null;
	protected Panel clPanels[] = null;
	protected Panel mainP = null;
	protected Checkbox cbRemoveHiddenFromList = null;
	protected boolean destroyed = false;

	/**
	 * Construction of map manipulator. Returns true if successfully constructed.
	 * Arguments: 1) supervisor responsible for dynamic linking of displays;
	 * 2) visualizer or classifier to be manipulated;
	 * 3) table with source data;
	 * The Manipulator must check if the visualizer has the appropriate type.
	 * The visualizer should be an instance of SimpleSignPresenter.
	 */
	@Override
	public boolean construct(Supervisor sup, Object visualizer, AttributeDataPortion dataTable) {
		if (visualizer == null)
			return false;
		if (!(visualizer instanceof SimpleSignPresenter))
			return false;
		if (!(dataTable instanceof DataTable))
			return false;
		sPres = (SimpleSignPresenter) visualizer;
		sPres.addVisChangeListener(this);
		this.sup = sup;
		this.dataTable = (DataTable) dataTable;
		dataTable.addPropertyChangeListener(this);
		setLayout(new ColumnLayout());
		makeInterior();
		return true;
	}

	protected void constructMainPanel() {
		Vector vals = sPres.getValues();
		if (vals == null)
			return;
		int ncl = vals.size();
		if (ncl < 1)
			return;
		acs = new ActionCanvas[ncl];
		cbs = new Checkbox[ncl];
		clPanels = new Panel[ncl];
		int counts[] = sPres.getValueCounts();
		if (counts != null && counts.length > 1) {
			valOrder = new Vector<ObjectWithCount>(counts.length, 1);
		}
		for (int i = 0; i < ncl; i++) {
			clPanels[i] = new Panel(new BorderLayout());
			SimpleSign s = sPres.getSignForValue(i);
			int w = s.getWidth(), h = s.getHeight();
			acs[i] = new ActionCanvas(s);
			acs[i].setMinimumSize(new Dimension(w + 10, h + 10));
			acs[i].addActionListener(this);
			acs[i].setActionCommand(String.valueOf(i));
			clPanels[i].add(acs[i], "West");
			String label = vals.elementAt(i).toString();
			boolean active = qac == null || qac.doesSatisfy(label);
			if (counts != null) {
				label += " (" + counts[i] + ")";
			}
			cbs[i] = new Checkbox(label, active);
			cbs[i].addItemListener(this);
			clPanels[i].add(cbs[i], "Center");
			if (valOrder != null) {
				valOrder.addElement(new ObjectWithCount(new Integer(i), counts[i]));
			}
		}
		boolean remove = cbRemoveHiddenFromList != null && cbRemoveHiddenFromList.getState();
		if (valOrder == null) {
			for (int i = 0; i < ncl; i++) {
				if (!remove || cbs[i].getState()) {
					mainP.add(clPanels[i]);
				}
			}
		} else {
			BubbleSort.sort(valOrder);
			for (int i = 0; i < valOrder.size(); i++) {
				int idx = ((Integer) valOrder.elementAt(i).obj).intValue();
				if (!remove || cbs[idx].getState()) {
					mainP.add(clPanels[idx]);
				}
			}
		}
		mainP.add(new Label(ncl + " values in total"));
	}

	protected void makeInterior() {
		String name = sPres.getAttrName(0);
		TextCanvas tc = new TextCanvas();
		tc.setText(name);
		add(tc);
		mainP = new Panel(new ColumnLayout());
		constructMainPanel();
		add(mainP);
		Panel p = new Panel(new FlowLayout(FlowLayout.CENTER, 20, 2));
		Button b = new Button("Hide all");
		b.setActionCommand("hide_all");
		b.addActionListener(this);
		p.add(b);
		b = new Button("Show all");
		b.setActionCommand("show_all");
		b.addActionListener(this);
		p.add(b);
		add(p);
		cbRemoveHiddenFromList = new Checkbox("remove hidden values from list", false);
		add(cbRemoveHiddenFromList);
		cbRemoveHiddenFromList.addItemListener(this);
	}

	/**
	 * Removes or adds the controls for the hidden classes
	 */
	protected void removeOrAddHiddenClasses() {
		boolean remove = cbRemoveHiddenFromList != null && cbRemoveHiddenFromList.getState();
		mainP.removeAll();
		if (valOrder == null) {
			for (int i = 0; i < clPanels.length; i++) {
				if (!remove || cbs[i].getState()) {
					mainP.add(clPanels[i]);
				}
			}
		} else {
			for (int i = 0; i < valOrder.size(); i++) {
				int idx = ((Integer) valOrder.elementAt(i).obj).intValue();
				if (!remove || cbs[idx].getState()) {
					mainP.add(clPanels[idx]);
				}
			}
		}
		mainP.add(new Label(cbs.length + " classes in total"));
		CManager.validateAll(mainP);
		boolean showInLegend[] = new boolean[cbs.length];
		for (int i = 0; i < cbs.length; i++) {
			showInLegend[i] = !remove || cbs[i].getState();
		}
		sPres.setShowInLegend(showInLegend);
		sPres.notifyVisChange();
	}

	private void makeFilter() {
		if (tFilter != null)
			return;
		if (dataTable == null)
			return;
		int colN = sPres.getColumnN(0);
		if (colN < 0)
			return;
		qac = new QualAttrCondition();
		qac.setTable(dataTable);
		qac.setAttributeIndex(colN);
		qac.setMissingValuesOK(false);
		tFilter = new TableFilter();
		tFilter.setObjectContainer(dataTable);
		tFilter.setEntitySetIdentifier(dataTable.getEntitySetIdentifier());
		tFilter.addAttrCondition(qac);
		dataTable.setObjectFilter(tFilter);
	}

	/**
	 * When the filter is changed by another controller, reflects the
	 * changes in the UI
	 */
	protected void reflectFilterState() {
		if (qac == null)
			return;
		if (!qac.hasLimit()) {
			for (Checkbox cb : cbs) {
				cb.setState(true);
			}
			return;
		}
		Vector vals = sPres.getValues();
		if (vals == null)
			return;
		for (int i = 0; i < cbs.length; i++) {
			boolean hidden = qac.doesSatisfy(vals.elementAt(i).toString());
			if (hidden != !cbs[i].getState()) {
				cbs[i].setState(!hidden);
			}
		}
	}

	/**
	 * Reacts to the checkboxes
	 */
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(cbRemoveHiddenFromList)) {
			removeOrAddHiddenClasses();
		} else if (e.getSource() instanceof Checkbox) {
			Vector vals = sPres.getValues();
			if (vals == null)
				return;
			makeFilter();
			Vector selVals = new Vector(vals.size(), 1);
			boolean changed = false;
			for (int i = 0; i < cbs.length; i++) {
				boolean active = cbs[i].getState();
				changed = changed || (active != qac.doesSatisfy(vals.elementAt(i).toString()));
				if (active) {
					selVals.addElement(vals.elementAt(i));
				}
			}
			if (changed) {
				qac.setRightValues(selVals);
				if (selVals.size() < 1) {
					qac.setAllValuesOff(true);
				}
				tFilter.notifyFilterChange();
				removeOrAddHiddenClasses();
			}
		}
	}

	/**
	 * Reacts to a click on an ActionCanvas
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof ActionCanvas) {
			int idx = -1;
			for (int i = 0; i < acs.length && idx < 0; i++)
				if (e.getSource().equals(acs[i])) {
					idx = i;
				}
			if (idx < 0)
				return;
			Vector vals = sPres.getValues();
			Panel p = new Panel(new BorderLayout());
			p.add(new Label(vals.elementAt(idx).toString(), Label.CENTER), BorderLayout.NORTH);
			int nCols = 5, nRows = (int) Math.round(Math.ceil((SimpleSign.SHAPE_LAST + 1.0) / nCols));
			Panel p1 = new Panel(new GridLayout(nRows, nCols, 3, 3));
			Checkbox cb[] = new Checkbox[SimpleSign.SHAPE_LAST + 1];
			CheckboxGroup cbg = new CheckboxGroup();
			int shape = sPres.getShapeForValue(idx);
			for (int i = 0; i <= SimpleSign.SHAPE_LAST; i++) {
				Panel p2 = new Panel(new GridLayout(1, 3, 0, 0));
				ActionCanvas ac = new ActionCanvas(sPres.getSignWithShape(i));
				p2.add(ac);
				cb[i] = new Checkbox("", i == shape, cbg);
				p2.add(cb[i]);
				p2.add(new Label(""));
				p1.add(p2);
			}
			p.add(p1, BorderLayout.SOUTH);
			OKDialog dia = new OKDialog(sup.getUI().getMainFrame(), "Select a symbol", true);
			dia.addContent(p);
			dia.show();
			if (dia.wasCancelled())
				return;
			for (int i = 0; i < cb.length; i++)
				if (cb[i].getState()) {
					sPres.setShapeForValue(i, idx);
					break;
				}
		} else if (e.getActionCommand().equals("hide_all")) {
			for (Checkbox cb : cbs) {
				cb.setState(false);
			}
			makeFilter();
			qac.setRightValues(null);
			qac.setAllValuesOff(true);
			tFilter.notifyFilterChange();
			boolean remove = cbRemoveHiddenFromList != null && cbRemoveHiddenFromList.getState();
			if (remove) {
				removeOrAddHiddenClasses();
			}
		} else if (e.getActionCommand().equals("show_all")) {
			for (Checkbox cb : cbs) {
				cb.setState(true);
			}
			if (qac != null) {
				qac.clearLimits();
			}
			tFilter.notifyFilterChange();
			boolean remove = cbRemoveHiddenFromList != null && cbRemoveHiddenFromList.getState();
			if (remove) {
				removeOrAddHiddenClasses();
			}
		}
	}

	/**
	* Reacts to changes in the visualizer and filter
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(sPres)) {
			if (e.getPropertyName().equals("destroyed")) {
				destroy();
				return;
			}
			Vector vals = sPres.getValues();
			if (vals == null)
				return;
			int ncl = vals.size();
			if (ncl != acs.length)
				return;
			//if the color of the icons changed, change the color in the action canvases
			for (int i = 0; i < acs.length; i++) {
				SimpleSign s = sPres.getSignForValue(i);
				acs[i].setImageDrawer(s);
				int w = s.getWidth(), h = s.getHeight();
				acs[i].setMinimumSize(new Dimension(w + 10, h + 10));
				acs[i].invalidate();
				acs[i].getParent().invalidate();
				acs[i].repaint();
			}
			CManager.validateAll(mainP);
		} else if (e.getSource().equals(dataTable)) {
			if (e.getPropertyName().equals("values") || e.getPropertyName().equals("data_added") || e.getPropertyName().equals("data_removed") || e.getPropertyName().equals("data_updated")) {
				boolean visible = isShowing();
				if (visible) {
					setVisible(false);
				}
				if (mainP != null) {
					remove(mainP);
					mainP.removeAll();
				}
				constructMainPanel();
				add(mainP, 1);
				if (visible) {
					setVisible(true);
					CManager.validateAll(mainP);
				}
			}
		}
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	@Override
	public void destroy() {
		sPres.removeVisChangeListener(this);
		dataTable.removePropertyChangeListener(this);
		if (tFilter != null) {
			dataTable.removeObjectFilter(tFilter);
		}
		destroyed = true;
	}
}
