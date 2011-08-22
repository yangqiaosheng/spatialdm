package spade.analysis.tools.clustering;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.FlexibleGridLayout;
import spade.lib.basicwin.ImageCanvas;
import spade.lib.page_util.PageCollection;
import spade.lib.page_util.PageElementImage;
import spade.lib.page_util.PageElementMultiple;
import spade.lib.page_util.PageStructure;
import spade.lib.util.BubbleSort;
import spade.lib.util.Comparator;
import spade.lib.util.IntArray;
import spade.vis.database.CombinedFilter;
import spade.vis.database.DataTable;
import spade.vis.database.ObjectFilter;
import spade.vis.database.ObjectFilterByCluster;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Mar 10, 2009
 * Time: 5:39:25 PM
 * Displays iconic representations of clusters.
 * Allows to select/deselect clusters.
 */
public class ClustersOverviewPanel extends Panel implements PropertyChangeListener, ItemListener, ActionListener, Destroyable, Comparator {
	/**
	 * Used for generating HTML pages with the iconic representations of the clusters
	 */
	public ESDACore core = null;
	/**
	 * Descriptors of the clusters or classes
	 */
	protected Vector<ClusterImage> cImages = null;
	/**
	 * The table defining the clusters or classes of the objects
	 * described in this table
	 */
	protected DataTable table = null;
	/**
	 * The title of the panel
	 */
	protected String title = null;
	/**
	 * A header of an HTML page, which may be generated from this panel
	 */
	protected String header = null;
	/**
	 * The index of the table column with the labels of the clusters or classes
	 */
	protected int colN = -1;
	/**
	 * Sorted labels of the classes (clusters)
	 */
	protected Vector labels = null;
	/**
	 * Which of the clusters are currently hidden
	 */
	protected boolean hidden[] = null;
	/**
	 * The filter to remove classes from view
	 */
	protected ObjectFilterByCluster clusterFlt = null;
	/**
	 * The main panel with a flexible grid layout, containing the images
	 */
	protected Panel mainP = null;
	/**
	 * The components of the main panel
	 */
	protected Panel clPanels[] = null;

	protected Checkbox showAllCB = null, showSelCB = null;
	protected Checkbox clShownCB[] = null;

	/**
	 * @param table - the table describing some previously clustered or classified objects
	 * @param colN - the column with the labels of the clusters or classes
	 * @param cImages - previously constructed descriptors of the clusters or classes
	 * @param title - the title of the panel
	 * @param header - the header of an HTML page, which may be generated from this panel
	 * @param core - used for generating HTML pages
	 */
	public ClustersOverviewPanel(DataTable table, int colN, Vector<ClusterImage> cImages, String title, String header, ESDACore core) {
		this.table = table;
		this.colN = colN;
		this.cImages = cImages;
		this.core = core;
		this.title = title;
		this.header = header;
		if (table == null || colN < 0 || cImages == null || cImages.size() < 1)
			return;

		IntArray c = new IntArray(1, 1);
		c.addElement(colN);
		labels = table.getAllValuesInColumns(c);
		BubbleSort.sort(labels, this);
		for (int i = 0; i < cImages.size(); i++) {
			ClusterImage cim = cImages.elementAt(i);
			cim.clusterIdx = labels.indexOf(cim.clusterLabel);
		}
		hidden = new boolean[labels.size()];
		for (int i = 0; i < hidden.length; i++) {
			hidden[i] = false;
		}

		boolean sizesUnknown = true;
		for (int i = 0; i < cImages.size() && sizesUnknown; i++) {
			sizesUnknown = cImages.elementAt(i).size == 0;
		}
		if (sizesUnknown) {
			countClusterSizes(table, colN, cImages);
		}
		BubbleSort.sort(cImages);

		mainP = new Panel(new FlexibleGridLayout(2, 2));
		clShownCB = new Checkbox[cImages.size()];
		clPanels = new Panel[cImages.size()];
		for (int i = 0; i < cImages.size(); i++) {
			ClusterImage cim = cImages.elementAt(i);
			ImageCanvas ic = new ImageCanvas(cim.image);
			clShownCB[i] = new Checkbox(cim.clusterLabel + " (" + cim.size + ")", true);
			clShownCB[i].addItemListener(this);
			clPanels[i] = new Panel(new BorderLayout());
			clPanels[i].add(clShownCB[i], BorderLayout.NORTH);
			clPanels[i].add(ic, BorderLayout.CENTER);
			mainP.add(clPanels[i]);
		}
		ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
		scp.add(mainP);
		setLayout(new BorderLayout());
		add(scp, BorderLayout.CENTER);

		CheckboxGroup cbg = new CheckboxGroup();
		showAllCB = new Checkbox("all", cbg, true);
		showAllCB.addItemListener(this);
		showSelCB = new Checkbox("selected", cbg, false);
		showSelCB.addItemListener(this);
		Panel p = new Panel(new BorderLayout());
		Panel p1 = new Panel(new FlowLayout(FlowLayout.LEFT, 1, 1));
		p.add(p1, BorderLayout.WEST);
		p1.add(new Label("Show"));
		p1.add(showAllCB);
		p1.add(showSelCB);
		Panel p2 = new Panel(new FlowLayout(FlowLayout.LEFT, 10, 1));
		p1.add(p2);
		Button b = new Button("Deselect all");
		b.setActionCommand("hide_all");
		b.addActionListener(this);
		p2.add(b);
		b = new Button("Select all");
		b.setActionCommand("show_all");
		b.addActionListener(this);
		p2.add(b);
		p1 = new Panel(new FlowLayout(FlowLayout.CENTER, 10, 1));
		b = new Button("Print");
		b.addActionListener(this);
		b.setActionCommand("print");
		p1.add(b);
		p.add(p1, BorderLayout.EAST);
		add(p, BorderLayout.NORTH);

		makeClusterFilter();
	}

	/**
	 * Compares two string values (labels of classes or clusters).
	 * First attempts to transform them to numbers.
	 */
	@Override
	public int compare(Object o1, Object o2) {
		if (o1 == null || o2 == null)
			return 0;
		if (!(o1 instanceof String) || !(o2 instanceof String))
			return 0;
		String st1 = (String) o1, st2 = (String) o2;
		float v1 = Float.NaN, v2 = Float.NaN;
		try {
			v1 = Float.parseFloat(st1);
		} catch (NumberFormatException e) {
		}
		try {
			v2 = Float.parseFloat(st2);
		} catch (NumberFormatException e) {
		}
		if (Float.isNaN(v1))
			if (Float.isNaN(v2))
				return st1.compareTo(st2);
			else
				return 1;
		if (Float.isNaN(v2))
			return -1;
		if (v1 < v2)
			return -1;
		if (v1 > v2)
			return 1;
		return 0;
	}

	/**
	 * For each of the clusters represented in cImages counts the number of
	 * trajectories in this cluster
	 * @param table - the table describing the trajectories
	 * @param colN - the column with the labels of the clusters or classes
	 * @param cImages - previously constructed descriptors of the clusters or classes
	 */
	public void countClusterSizes(DataTable table, int colN, Vector<ClusterImage> cImages) {
		if (table == null || colN < 0 || cImages == null || cImages.size() < 1)
			return;
		for (int i = 0; i < cImages.size(); i++) {
			cImages.elementAt(i).size = 0;
		}
		for (int i = 0; i < table.getDataItemCount(); i++) {
			String cLabel = table.getAttrValueAsString(colN, i);
			if (cLabel == null) {
				continue;
			}
			for (int j = 0; j < cImages.size(); j++)
				if (cLabel.equals(cImages.elementAt(j).clusterLabel)) {
					++cImages.elementAt(j).size;
					break;
				}
		}
	}

	private ObjectFilterByCluster findClusterFilter() {
		ObjectFilter oFilter = table.getObjectFilter();
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
		clusterFlt = findClusterFilter();
		if (clusterFlt != null) {
			clusterFlt.addController(this);
			reflectFilterState();
			return;
		}
		String objIds[] = new String[table.getDataItemCount()];
		int objClusters[] = new int[table.getDataItemCount()];
		for (int i = 0; i < table.getDataItemCount(); i++) {
			objIds[i] = table.getDataItemId(i);
			String cLabel = table.getAttrValueAsString(colN, i);
			if (cLabel == null) {
				objClusters[i] = -1;
			} else {
				objClusters[i] = labels.indexOf(cLabel);
			}
		}
		clusterFlt = new ObjectFilterByCluster();
		clusterFlt.setObjectContainer(table);
		clusterFlt.setTableColN(colN);
		clusterFlt.setEntitySetIdentifier(table.getEntitySetIdentifier());
		clusterFlt.setClustering(objIds, objClusters);
		table.setObjectFilter(clusterFlt);
		if (table instanceof PropertyChangeListener) {
			clusterFlt.addPropertyChangeListener((PropertyChangeListener) table);
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
		boolean changed = false;
		for (int i = 0; i < hidden.length; i++)
			if (hidden[i] != clusterFlt.isClusterHidden(i)) {
				hidden[i] = !hidden[i];
				for (int j = 0; j < cImages.size(); j++)
					if (cImages.elementAt(j).clusterIdx == i) {
						clShownCB[j].setState(!hidden[i]);
						changed = true;
						break;
					}
			}
		if (changed && showSelCB.getState()) {
			buildMainPanel();
		}
	}

	/**
	 * Builds the main panel depending on whether all or only selected
	 * clusters must be visible
	 */
	protected void buildMainPanel() {
		mainP.setVisible(false);
		mainP.removeAll();
		if (showAllCB.getState()) {
			for (Panel clPanel : clPanels) {
				mainP.add(clPanel);
			}
		} else {
			for (int i = 0; i < clPanels.length; i++)
				if (clShownCB[i].getState()) {
					mainP.add(clPanels[i]);
				}
		}
		mainP.setVisible(true);
		mainP.invalidate();
		CManager.validateAll(mainP);
	}

	/**
	 * Reacts to changes of the cluster filter, possibly, made by another controller
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(clusterFlt)) {
			if (e.getPropertyName().equals("destroyed")) {
				destroy();
				return;
			}
			if (e.getPropertyName().equals("Filter")) {
				reflectFilterState();
			}
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(showAllCB) || e.getSource().equals(showSelCB)) {
			boolean someHidden = false;
			for (int i = 0; i < clShownCB.length && !someHidden; i++) {
				someHidden = !clShownCB[i].getState();
			}
			if (someHidden) {
				buildMainPanel();
			}
		} else {
			//select/deselect a single cluster
			for (int j = 0; j < clShownCB.length; j++)
				if (e.getSource().equals(clShownCB[j])) {
					int idx = cImages.elementAt(j).clusterIdx;
					hidden[idx] = !clShownCB[j].getState();
					if (showSelCB.getState()) {
						buildMainPanel();
					}
					clusterFlt.setClusterIsHidden(hidden[idx], idx);
					break;
				}
		}
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals("hide_all")) {
			boolean changed = false;
			for (int i = 0; i < hidden.length; i++)
				if (!hidden[i]) {
					hidden[i] = true;
					for (int j = 0; j < cImages.size(); j++)
						if (cImages.elementAt(j).clusterIdx == i) {
							clShownCB[j].setState(false);
							changed = true;
							break;
						}
				}
			if (changed && showSelCB.getState()) {
				buildMainPanel();
			}
			if (clusterFlt != null) {
				IntArray iar = new IntArray(hidden.length, 1);
				for (int i = 0; i < hidden.length; i++) {
					iar.addElement(i);
				}
				clusterFlt.setClustersAreHidden(iar);
			}
			return;
		}
		if (ae.getActionCommand().equals("show_all")) {
			boolean changed = false;
			for (int i = 0; i < hidden.length; i++)
				if (hidden[i]) {
					hidden[i] = false;
					for (int j = 0; j < cImages.size(); j++)
						if (cImages.elementAt(j).clusterIdx == i) {
							clShownCB[j].setState(true);
							changed = true;
							break;
						}
				}
			if (changed && showSelCB.getState()) {
				buildMainPanel();
			}
			if (clusterFlt != null) {
				clusterFlt.clearFilter();
			}
			return;
		}
		if (ae.getActionCommand().equals("print")) {
			PageElementMultiple mul = new PageElementMultiple();
			int totalW = mainP.getSize().width;
			int imW = cImages.elementAt(0).image.getWidth(null);
			mul.nColumns = totalW / imW;
			if (mul.nColumns < 1) {
				mul.nColumns = 1;
			}
			for (int j = 0; j < cImages.size(); j++)
				if (!hidden[cImages.elementAt(j).clusterIdx]) {
					PageElementImage im = new PageElementImage();
					im.image = cImages.elementAt(j).image;
					im.header = clShownCB[j].getLabel();
					im.fname = "_" + cImages.elementAt(j).clusterLabel;
					mul.addItem(im);
				}
			if (mul.getItemCount() < 1)
				return;
			if (core.mayAskForComments()) {
				String txt = Dialogs.askForComment(core.getUI().getMainFrame(), "Comment?", null, "Comment", true);
				if (txt != null) {
					mul.header = txt;
				}
			}
			PageStructure ps = new PageStructure();
			ps.layout = PageStructure.LAYOUT_COLUMN;
			ps.addElement(mul);
			PageCollection pc = new PageCollection();
			pc.addPage(ps);
			ps.fname = "_sum_clusters";
			ps.title = title;
			ps.header = header + " (" + mul.getItemCount() + " classes)";
			String str[] = core.updateDTinPageMaker();
			String sLFdt_fmt = str[0]; // human-readable
			String sLFdt = str[1]; // file name
			String sLFlastCommand = sLFdt_fmt + " <A HREF=\"./" + sLFdt + ps.fname + ".html\">Printed " + mul.getItemCount() + " images of " + title + "</A></P>";
			core.logSimpleAction(sLFlastCommand);
			core.makePages(pc);
		}
	}

	protected boolean destroyed = false;

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	@Override
	public void destroy() {
		if (clusterFlt != null) {
			clusterFlt.removeController(this);
		}
		clusterFlt = null;
		destroyed = true;
	}
}
