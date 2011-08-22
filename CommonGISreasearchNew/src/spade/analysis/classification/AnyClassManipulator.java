package spade.analysis.classification;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.analysis.manipulation.Manipulator;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.TextCanvas;
import spade.lib.color.ColorCanvas;
import spade.lib.color.ColorDlg;
import spade.lib.color.ColorListener;
import spade.lib.util.BubbleSort;
import spade.lib.util.Comparator;
import spade.lib.util.IntArray;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.CombinedFilter;
import spade.vis.database.ObjectContainer;
import spade.vis.database.ObjectFilter;
import spade.vis.database.ObjectFilterByCluster;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 23-Apr-2007
 * Time: 11:33:37
 */
public class AnyClassManipulator extends Panel implements Manipulator, Comparator, ActionListener, ItemListener, ColorListener, PropertyChangeListener, Destroyable {
	protected Supervisor sup = null;
	protected Classifier cl = null;
	/**
	 * The container in which the objects are classified
	 */
	protected ObjectContainer oCont = null;
	/**
	 * The filter to remove classes from view
	 */
	protected ObjectFilterByCluster clusterFlt = null;
	/**
	 * The filter of the container, to show the number of active objects
	 */
	protected ObjectFilter filter = null;

	protected ColorCanvas ccs[] = null;
	protected Checkbox cbs[] = null;
	protected Panel clPanels[] = null;
	protected ColorDlg cDlg = null;
	protected Panel mainP = null;
	protected Checkbox cbRemoveEmptyFromList = null;
	protected boolean destroyed = false;
	/**
	 * An array specifying the order of the classes according to their sizes.
	 * Consists of pairs where the first element is the index of the class and
	 * the second element is its size.
	 */
	protected int classOrder[][] = null;
	/**
	 * Number of objects in each class after filtering
	 */
	protected int classSizesFiltered[] = null;

	/**
	 * Construction of map manipulator. Returns true if successfully constructed.
	 * Arguments: 1) supervisor responsible for dynamic linking of displays;
	 * 2) visualizer or classifier to be manipulated;
	 * 3) table with source data (is ignored);
	 * The Manipulator must check if the visualizer has the appropriate type.
	 * The visualizer should be an instance of Classifier.
	 */
	@Override
	public boolean construct(Supervisor sup, Object visualizer, AttributeDataPortion dataTable) {
		return construct(sup, visualizer, dataTable, null);
	}

	/**
	 * Construction of map manipulator. Returns true if successfully constructed.
	 * Arguments: 1) supervisor responsible for dynamic linking of displays;
	 * 2) visualizer or classifier to be manipulated;
	 * 3) table with source data (is ignored);
	 * 4) ann array specifying the order of the classes, which consists of pairs
	 * where the first element is the index of the class and the second element is its size
	 * The Manipulator must check if the visualizer has the appropriate type.
	 * The visualizer should be an instance of Classifier.
	 */
	public boolean construct(Supervisor sup, Object visualizer, AttributeDataPortion dataTable, int classOrder[][]) {
		if (visualizer == null)
			return false;
		if (!(visualizer instanceof Classifier))
			return false;
		cl = (Classifier) visualizer;
		cl.addPropertyChangeListener(this);
		if (cl.getObjectContainer() != null) {
			cl.getObjectContainer().addPropertyChangeListener(this);
			filter = cl.getObjectContainer().getObjectFilter();
			if (filter != null) {
				filter.addPropertyChangeListener(this);
			}
		}
		this.sup = sup;
		this.classOrder = classOrder;
		setLayout(new ColumnLayout());
		System.out.println("Call makeInterior()...");
		long t0 = System.currentTimeMillis();
		makeInterior();
		long t = System.currentTimeMillis();
		System.out.println("makeInterior() took " + (t - t0) + " msec.");
		t0 = t;
		System.out.println("Call makeClusterFilter()...");
		makeClusterFilter();
		t = System.currentTimeMillis();
		System.out.println("makeClusterFilter() took " + (t - t0) + " msec.");
		return true;
	}

	/**
	 * Sets the order of the classes according to their sizes. The argument must
	 * consist of pairs where the first element is the index of the class and
	 * the second element is its size.
	 */
	public void setClassOrder(int[][] classOrder) {
		this.classOrder = classOrder;
		if (ccs != null) {
			mainP.setVisible(false);
			mainP.removeAll();
			constructMainPanel();
			mainP.setVisible(true);
			mainP.invalidate();
			CManager.validateAll(mainP);
		}
	}

	protected void constructMainPanel() {
		if (classOrder == null) {
			classOrder = this.getClassOrderBySize();
		}
		int ncl = (classOrder != null) ? classOrder.length : cl.getNClasses();
		ccs = new ColorCanvas[ncl];
		cbs = new Checkbox[ncl];
		clPanels = new Panel[ncl];
		for (int i = 0; i < ncl; i++) {
			clPanels[i] = new Panel(new BorderLayout());
			ccs[i] = new ColorCanvas();
			ccs[i].setColor(cl.getClassColor(i));
			if (cl.allowChangeClassColor()) {
				ccs[i].setActionListener(this);
			}
			clPanels[i].add(ccs[i], "West");
			boolean active = clusterFlt == null || !clusterFlt.isClusterHidden(i);
			cbs[i] = new Checkbox(cl.getClassName(i), active);
			//cbs[i]=new Checkbox(cl.getClassName(i),!cl.isClassHidden(i));
			cbs[i].addItemListener(this);
			clPanels[i].add(cbs[i], "Center");
		}
		setClassTexts();
		boolean remove = cbRemoveEmptyFromList != null && cbRemoveEmptyFromList.getState();
		int nShown = 0;
		if (classOrder == null) {
			for (int i = 0; i < ncl; i++) {
				if (!remove || classSizesFiltered[i] > 0) {
					mainP.add(clPanels[i]);
					++nShown;
				}
			}
		} else {
			for (int i = 0; i < ncl; i++) {
				int idx = classOrder[i][0];
				if (!remove || classSizesFiltered[idx] > 0) {
					mainP.add(clPanels[idx]);
					++nShown;
				}
			}
		}
		mainP.add(new Label(ncl + " classes in total; " + nShown + " shown"));
	}

	protected void setClassTexts() {
		if (cbs == null || cbs.length < 1)
			return;
		if (classSizesFiltered == null || classSizesFiltered.length != cbs.length) {
			classSizesFiltered = new int[cbs.length];
		}
		IntArray counts = cl.getClassSizes(), countsFlt = cl.getFilteredClassSizes();
		for (int i = 0; i < cbs.length; i++) {
			classSizesFiltered[i] = (countsFlt == null) ? counts.elementAt(i) : countsFlt.elementAt(i);
			if (countsFlt == null || counts.elementAt(i) == countsFlt.elementAt(i)) {
				cbs[i].setLabel(cl.getClassName(i) + " (" + counts.elementAt(i) + ")");
			} else {
				cbs[i].setLabel(cl.getClassName(i) + " (" + counts.elementAt(i) + "/" + countsFlt.elementAt(i) + ")");
			}
		}
	}

	protected void makeInterior() {
		String name = cl.getName();
		if (name == null) {
			name = "Classes";
		}
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
		cbRemoveEmptyFromList = new Checkbox("remove empty classes from list", false);
		add(cbRemoveEmptyFromList);
		cbRemoveEmptyFromList.addItemListener(this);
		if (sup != null) {
			Component broadPan = null;
			try {
				Object obj = Class.forName("spade.analysis.classification.ClassBroadcastPanel").newInstance();
				if (obj != null && (obj instanceof ClassOperator) && (obj instanceof Component)) {
					ClassOperator cop = (ClassOperator) obj;
					if (cop.construct(cl, sup)) {
						broadPan = (Component) obj;
					}
				}
			} catch (Exception e) {
			}
			if (broadPan != null) {
				add(broadPan);
			}
		}
	}

	/**
	 * Determines the order of the classes according to their sizes (from the
	 * biggest to the smallest). Returns an array of pairs where the first element
	 * is the index of the class and the second element is its size.
	 */
	public int[][] getClassOrderBySize() {
		if (cl == null)
			return null;
		int ncl = cl.getNClasses();
		if (ncl < 1)
			return null;
		long t0 = System.currentTimeMillis();
		int objClassNumbers[] = cl.getObjectClassNumbers();
		if (objClassNumbers == null)
			return null;
		int c_idxl_size[][] = new int[ncl][2];
		for (int i = 0; i < ncl; i++) {
			c_idxl_size[i][0] = i;
			c_idxl_size[i][1] = 0;
		}
		for (int i = 0; i < objClassNumbers.length; i++)
			if (objClassNumbers[i] >= 0) {
				++c_idxl_size[objClassNumbers[i]][1];
			}
		Vector v = new Vector(ncl + 5, 1);
		for (int i = 0; i < ncl; i++) {
			v.addElement(c_idxl_size[i]);
		}
		BubbleSort.sort(v, this);
		int order[][] = new int[ncl][2];
		for (int i = 0; i < v.size(); i++) {
			int pair[] = (int[]) v.elementAt(i);
			order[i][0] = pair[0];
			order[i][1] = pair[1];
		}
		long t = System.currentTimeMillis();
		System.out.println("getClassOrderBySize() took " + (t - t0) + " msec.");
		return order;
	}

	/**
	 * Compares two pairs consisting of cluster numbers and sizes.
	 * Returns 0 if the objects are equal, <0 if the first object is less than the
	 * second one, >0 otherwise
	 */
	@Override
	public int compare(Object obj1, Object obj2) {
		if (obj1 == null || obj2 == null)
			return 0;
		if (!(obj1 instanceof int[]) || !(obj2 instanceof int[]))
			return 0;
		int p1[] = (int[]) obj1, p2[] = (int[]) obj2;
		if (cl.getClassName(p1[0]).equals("noise"))
			return 1;
		if (cl.getClassName(p2[0]).equals("noise"))
			return -1;
		if (p1[1] < p2[1])
			return 1;
		if (p1[1] > p2[1])
			return -1;
		return 0;
	}

	private ObjectFilterByCluster findClusterFilter(ObjectContainer oCont, int colN) {
		ObjectFilter oFilter = oCont.getObjectFilter();
		if (oFilter == null)
			return null;
		if (oFilter instanceof ObjectFilterByCluster) {
			ObjectFilterByCluster cf = (ObjectFilterByCluster) oFilter;
			if (cf.getTableColN() == colN)
				return cf;
			return null;
		}
		if (oFilter instanceof CombinedFilter) {
			CombinedFilter cFilter = (CombinedFilter) oFilter;
			for (int i = 0; i < cFilter.getFilterCount(); i++) {
				oFilter = cFilter.getFilter(i);
				if (oFilter instanceof ObjectFilterByCluster) {
					ObjectFilterByCluster cf = (ObjectFilterByCluster) oFilter;
					if (cf.getTableColN() == colN)
						return cf;
				}
			}
		}
		return null;
	}

	private void makeClusterFilter() {
		if (clusterFlt != null)
			return;
		ObjectContainer oCont = cl.getObjectContainer();
		if (oCont == null)
			return;
		int colN = -1;
		if ((cl instanceof TableClassifier) && (oCont instanceof AttributeDataPortion)) {
			TableClassifier tcl = (TableClassifier) cl;
			if (tcl.attr != null && tcl.attr.size() == 1 && tcl.subAttr == null) {
				AttributeDataPortion table = (AttributeDataPortion) oCont;
				colN = table.getAttrIndex((String) tcl.attr.elementAt(0));
			}
		}
		if (colN >= 0) {
			clusterFlt = findClusterFilter(oCont, colN);
		}
		if (clusterFlt != null) {
			clusterFlt.addController(this);
			reflectFilterState();
			return;
		}
		String objIds[] = cl.getObjectIds();
		if (objIds == null)
			return;
		int objClassNumbers[] = cl.getObjectClassNumbers();
		if (objClassNumbers == null)
			return;
		clusterFlt = new ObjectFilterByCluster();
		clusterFlt.setObjectContainer(oCont);
		if (colN >= 0) {
			clusterFlt.setTableColN(colN);
		}
		clusterFlt.setEntitySetIdentifier(oCont.getEntitySetIdentifier());
		clusterFlt.setClustering(objIds, objClassNumbers);
		oCont.setObjectFilter(clusterFlt);
		if (oCont instanceof PropertyChangeListener) {
			clusterFlt.addPropertyChangeListener((PropertyChangeListener) oCont);
		}
		clusterFlt.addController(this);
	}

	/**
	 * When the filter is changed by another controller, reflects the
	 * changes in the UI
	 */
	protected void reflectFilterState() {
		if (clusterFlt == null)
			return;
		if (!clusterFlt.areObjectsFiltered()) {
/*
      if (cl.getHiddenClassCount()>0) {
        cl.exposeAllClasses();
        cl.notifyColorsChange();
      }
*/
			for (Checkbox cb : cbs) {
				cb.setState(true);
			}
			removeOrAddHiddenClasses();
			return;
		}
		boolean changed = false;
		for (int i = 0; i < cbs.length; i++) {
			boolean hidden = clusterFlt.isClusterHidden(i);
			if (hidden != !cbs[i].getState()) {
				cbs[i].setState(!hidden);
				//cl.setClassIsHidden(hidden,i);
				changed = true;
			}
		}
		if (changed) {
			removeOrAddHiddenClasses();
/*
		if (changed)
		  cl.notifyColorsChange();
*/
		}
	}

	/**
	 * Removes or adds the controls for the hidden classes
	 */
	protected void removeOrAddHiddenClasses() {
		if (classSizesFiltered == null) {
			setClassTexts();
		}
		boolean remove = cbRemoveEmptyFromList != null && cbRemoveEmptyFromList.getState();
		mainP.removeAll();
		int nShown = 0;
		if (classOrder == null) {
			for (int i = 0; i < cbs.length; i++) {
				if (!remove || classSizesFiltered[i] > 0) {
					mainP.add(clPanels[i]);
					++nShown;
				}
			}
		} else {
			for (int i = 0; i < cbs.length; i++) {
				int idx = classOrder[i][0];
				if (!remove || classSizesFiltered[idx] > 0) {
					mainP.add(clPanels[idx]);
					++nShown;
				}
			}
		}
		mainP.add(new Label(cbs.length + " classes in total; " + nShown + " shown"));
		CManager.validateAll(mainP);
		boolean showInLegend[] = new boolean[cbs.length];
		for (int i = 0; i < cbs.length; i++) {
			showInLegend[i] = !remove || classSizesFiltered[i] > 0;
		}
		cl.setShowInLegend(showInLegend);
		cl.notifyColorsChange();
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (ie.getSource().equals(cbRemoveEmptyFromList)) {
			removeOrAddHiddenClasses();
		} else if (ie.getSource() instanceof Checkbox) {
			for (int i = 0; i < cbs.length; i++)
				if (cbs[i] == ie.getSource()) {
					boolean hidden = !cbs[i].getState();
					//cl.setClassIsHidden(hidden,i);
					if (clusterFlt != null) {
						clusterFlt.setClusterIsHidden(hidden, i);
					}
/*
          if (hidden && cbRemoveEmptyFromList !=null && cbRemoveEmptyFromList.getState())
            removeOrAddHiddenClasses();
*/
					break;
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
					name = cl.getClassName(i);
				}
			// getting new color for it
			if (cDlg == null) {
				cDlg = new ColorDlg(CManager.getAnyFrame(this), "");
			}
			cDlg.setTitle("Color for: " + name);
			cDlg.selectColor(this, cc, cc.getColor());
			return;
		}
		if (ae.getActionCommand().equals("hide_all")) {
			for (Checkbox cb : cbs) {
				cb.setState(false);
			}
			//cl.hideAllClasses();
			if (clusterFlt != null) {
				int ncl = cl.getNClasses();
				IntArray iar = new IntArray(ncl, 1);
				for (int i = 0; i < ncl; i++) {
					iar.addElement(i);
				}
				clusterFlt.setClustersAreHidden(iar);
			}
/*
      boolean remove=cbRemoveEmptyFromList !=null && cbRemoveEmptyFromList.getState();
      if (remove)
        removeOrAddHiddenClasses();
*/
			return;
		}
		if (ae.getActionCommand().equals("show_all")) {
			for (Checkbox cb : cbs) {
				cb.setState(true);
			}
			//cl.exposeAllClasses();
			if (clusterFlt != null) {
				clusterFlt.clearFilter();
			}
/*
      boolean remove=cbRemoveEmptyFromList !=null && cbRemoveEmptyFromList.getState();
      if (remove)
        removeOrAddHiddenClasses();
*/
			return;
		}
	}

	public void setAllClassesVisible() {
		for (Checkbox cb : cbs) {
			cb.setState(true);
		}
	}

	public void setAllClassesHidden() {
		for (Checkbox cb : cbs) {
			cb.setState(false);
		}
	}

	/*
	* color change through the dialog
	*/
	@Override
	public void colorChanged(Color c, Object sel) {
		// find a ColorCanvas produced the message
		ColorCanvas cc = null;
		for (int i = 0; i < ccs.length && cc == null; i++)
			if (ccs[i] == sel) {
				cc = ccs[i];
				// save a color
				cc.setColor(c);
				cl.setColorForClass(i, c);
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
		if (e.getSource().equals(cl)) {
			if (e.getPropertyName().equals("destroyed")) {
				destroy();
				return;
			}
			if (cbs == null && cl.getNClasses() == 0)
				return;
			if (e.getPropertyName().equals("classes")) {
				//cl.exposeAllClasses();
				if (clusterFlt != null) {
					clusterFlt.setClustering(cl.getObjectIds(), cl.getObjectClassNumbers());
				}
				classOrder = this.getClassOrderBySize();
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
			} else if (e.getPropertyName().equals("colors")) {
				for (int i = 0; i < cl.getNClasses(); i++) {
					ccs[i].setColor(cl.getClassColor(i));
				}
				return;
			}
/*
      //check what is hidden
      for (int i=0; i<cl.getNClasses(); i++)
        cbs[i].setState(!cl.isClassHidden(i));
*/
			//check class colors
			for (int i = 0; i < cl.getNClasses(); i++)
				if (!cl.getClassColor(i).equals(ccs[i].getColor())) {
					ccs[i].setColor(cl.getClassColor(i));
				}
			//check class names
			setClassTexts();
		} else if (e.getSource().equals(clusterFlt)) {
			if (e.getPropertyName().equals("destroyed")) {
				destroy();
				return;
			}
			if (e.getPropertyName().equals("Filter")) {
				reflectFilterState();
			}
		} else if (e.getSource() instanceof ObjectContainer) {
			if (e.getPropertyName().equalsIgnoreCase("filter")) {
				if (filter != null) {
					filter.removePropertyChangeListener(this);
				}
				filter = null;
				filter = cl.getObjectContainer().getObjectFilter();
				if (filter != null) {
					filter.addPropertyChangeListener(this);
				}
			}
		} else if (e.getSource().equals(filter)) {
			setClassTexts();
			boolean remove = cbRemoveEmptyFromList != null && cbRemoveEmptyFromList.getState();
			if (remove) {
				removeOrAddHiddenClasses();
			}
		}
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	@Override
	public void destroy() {
		cl.removePropertyChangeListener(this);
		if (cl.getObjectContainer() != null) {
			cl.getObjectContainer().removePropertyChangeListener(this);
		}
		if (filter != null) {
			filter.removePropertyChangeListener(this);
		}
		if (clusterFlt != null) {
			if (oCont != null) {
				oCont.removeObjectFilter(clusterFlt);
			}
			clusterFlt.removeController(this);
		}
		destroyed = true;
	}
}
