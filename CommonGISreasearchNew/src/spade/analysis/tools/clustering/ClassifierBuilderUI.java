package spade.analysis.tools.clustering;

import it.unipi.di.sax.kmedoids.KMedoids;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.InputDoublePanel;
import spade.lib.basicwin.InputIntPanel;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.PopupManager;
import spade.lib.basicwin.SelectDialog;
import spade.lib.page_util.PageCollection;
import spade.lib.page_util.PageElementImage;
import spade.lib.page_util.PageElementMultiple;
import spade.lib.page_util.PageElementTable;
import spade.lib.page_util.PageElementText;
import spade.lib.page_util.PageStructure;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.vis.action.HighlightListener;
import spade.vis.action.Highlighter;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.ObjectFilterBySelection;
import spade.vis.dataview.TableViewer;
import spade.vis.dmap.DAggregateLinkLayer;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DLayerManager;
import spade.vis.dmap.DLinkLayer;
import spade.vis.dmap.MapCanvas;
import spade.vis.dmap.TrajectoryObject;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapContext;
import spade.vis.map.MapViewer;
import ui.MapBkgImageMaker;
import ui.SimpleMapView;
import core.ActionDescr;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jan 8, 2009
 * Time: 6:33:51 PM
 * A UI to control the process of building of a classifier based on
 * results of clustering.
 */
public class ClassifierBuilderUI extends Panel implements ActionListener, ItemListener, PropertyChangeListener, MouseListener, WindowListener, Destroyable {
	/**
	 * Used for an access to the highlighter etc.
	 */
	protected ESDACore core = null;
	/**
	 * Contains the results of the clustering, which are used for
	 * building the classifier
	 */
	protected DClusterObject clusteredObjects[] = null;
	/**
	 * Contains information about the clusters, which needs to be visualised
	 * and, perhaps, interactively modified.
	 */
	protected ClustersInfo clustersInfo = null;
	/**
	 * Contains the original, unmodified information about the clusters, which
	 * can be used to reset the classifier.
	 */
	protected ClustersInfo origClustersInfo = null;
	/**
	 * Will be used for testing the classifier
	 */
	protected ObjectsToClustersAssigner clAssigner = null;
	/**
	 * A map layer containing the specimens of the clusters.
	 */
	protected DGeoLayer spLayer = null;
	/**
	 * A table attached to the layer with the cluster specimens.
	 */
	protected DataTable spTable = null;
	/**
	 * The index of the table column containing the cluster numbers
	 * of the cluster specimens
	 */
	protected int clusterColN = -1;
	/**
	 * the user-specified maximum radius of the subccluster (may be NaN)
	 */
	protected double maxSubclusterRadius = Double.NaN;
	/**
	 * The "working" map viewer, in which the specimens of the clusters are shown
	 */
	protected MapViewer workMapView = null;

	protected TextField nClustersTF = null;
	protected Choice clusterCh = null;
	protected List specList = null;
	protected Panel specDetailsPanel = null;
	protected Checkbox showOnlySpecimenCB = null;
	protected Checkbox showSubclusterCB = null;
	protected Checkbox closestCB = null, firstCB = null;
	protected TextField clusterTestResultsTF[] = null;
	protected Checkbox clusterTestResultsCB[] = null;
	protected Checkbox showSpecimenNeighboursCB = null;
	protected CheckboxGroup whatToShowCBG = null;
	protected Button undoB = null;

	/**
	 * The index of the currently selected cluster
	 */
	protected int selClusterIdx = -1;
	/**
	 * The results of cluster testing
	 */
	protected Vector<ClusterTestResult> testResults = null;
	/**
	 * The identifiers of the objects which were not assigned to any cluster
	 * in the result of the testing
	 */
	protected IntArray unclassified = null;
	/**
	 * The layer with the original clustered object (more precisely,
	 * its copy shown in the work map window)
	 */
	protected DGeoLayer origLayer = null;
	/**
	 * Used for filtering the objects of the original layer
	 */
	protected ObjectFilterBySelection origLayerFilter = null;
	/**
	 * Used for filtering the objects of the layer with the cluster specimen
	 */
	protected ObjectFilterBySelection spFilter = null;
	/**
	 * Stores successive states in the process of building a classifier.
	 * Enables "undo" operations.
	 */
	protected Vector<ClassifierBuildState> states = null;

	/**
	 * Labels visually representing results of cluster inspection
	 */
	protected Vector<Label> clusterLabels = null;
	protected Panel pClusterLabels = null;
	protected Vector<String> clustersInspected = null, clustersSeen = null;

	protected ClassifierBuilderUIextraTable extraTbl = null;
	protected Button bExtraTable = null;

	/**
	 * log file
	 */
	protected String sLFpath = null, // folder for saving
			sLFdt = null, sLFdt_fmt = null; // date/time of last saving - file name prefix
	protected String sLFlastCommand = null; // comment included into index.html
	/**
	 * Used for documenting purposes (producing of small images for HTML pages)
	 */
	protected MapBkgImageMaker imgMaker = null;
	protected DGeoLayer bkgLayer = null;
	protected MapCanvas smallMap = null;

	/**
	 * Constructs the user interface
	 * @param clusteredLayer - The layer to which the clustering has been applied.
	 * @param clusteredObjects - Contains the results of the clustering, which are used for
	 * building the classifier
	 * @param clustersInfo - Contains information about the clusters, which needs to be visualised
	 * and, perhaps, interactively modified.
	 * @param spLayer -  A map layer containing the specimens of the clusters.
	 * @param spTable - A table attached to the layer with the cluster specimens.
	 * @param clusterColN - The index of the table column containing the cluster numbers
	 * of the cluster specimens.
	 * @param maxSubclusterRadius - the user-specified maximum radius of the subccluster (may be NaN)
	 * @param workMapView - The "working" map viewer, in which the specimens of the clusters are shown
	 * @param core - used for accessing the highlighter etc.
	 */
	public ClassifierBuilderUI(DGeoLayer clusteredLayer, DClusterObject clusteredObjects[], ClustersInfo clustersInfo, DGeoLayer spLayer, DataTable spTable, int clusterColN, double maxSubclusterRadius, MapViewer workMapView, ESDACore core) {
		if (clustersInfo == null || clusteredLayer == null || clusteredObjects == null)
			return;
		this.clusteredObjects = clusteredObjects;
		this.origClustersInfo = clustersInfo;
		this.spLayer = spLayer;
		this.spTable = spTable;
		this.clusterColN = clusterColN;
		this.maxSubclusterRadius = maxSubclusterRadius;
		this.workMapView = workMapView;
		this.core = core;
		clustersInspected = new Vector<String>(10, 10);
		clustersSeen = new Vector<String>(10, 10);
		clusterLabels = new Vector<Label>(clustersInfo.getClustersCount(), 10);
		//find the copy of the original layer in the new map view
		int lidx = workMapView.getLayerManager().getIndexOfLayer(clusteredLayer.getContainerIdentifier());
		if (lidx >= 0) {
			origLayer = (DGeoLayer) workMapView.getLayerManager().getGeoLayer(lidx);
		}

		makeImageLayer();

		setLayout(new BorderLayout());
		Panel pp = new Panel(new ColumnLayout());
		Panel ppp = new Panel(new FlowLayout(FlowLayout.LEFT));
		ppp.add(new Label("Overview of the clusters:"));
		ppp.add(bExtraTable = new Button("Show details"));
		bExtraTable.addActionListener(this);
		bExtraTable.setActionCommand("show_details_of_clusters");
		Button bl = new Button("Print all");
		bl.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				printAll();
			}
		});
		ppp.add(bl);
		Button b2 = new Button("Print current");
		b2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				printCurrent();
			}
		});
		ppp.add(b2);
		pp.add(ppp);
		pClusterLabels = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		pp.add(pClusterLabels);
		pClusterLabels.add(new Label(" "));
		pp.add(new Line(false));
		pp.add(new Label("Examine, test, and modify the classifier", Label.CENTER));
		Panel p = new Panel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		p.add(new Label("Cluster:"));
		clusterCh = new Choice();
		clusterCh.addItemListener(this);
		p.add(clusterCh);
		p.add(new Label("   "));
		Button b = new Button("Clean the cluster*");
		b.setActionCommand("clean_cluster");
		b.addActionListener(this);
		p.add(b);
		b = new Button("Refine the cluster**");
		b.setActionCommand("refine_cluster");
		b.addActionListener(this);
		p.add(b);
		pp.add(p);
		pp.add(new Label("* Remove selected members from the cluster and re-divide it into subclusters."));
		pp.add(new Label("** Divide into smaller clusters using K-Medoids algorithm."));
		pp.add(new Label("** Selected cluster members may be used as initial cluster seeds."));
		pp.add(new Line(false));
		pp.add(new Label("Cluster prototypes:", Label.LEFT));
		add(pp, BorderLayout.NORTH);
		Panel sp = new Panel(new BorderLayout());
		add(sp, BorderLayout.CENTER);
		specList = new List(10, true);
		specList.addItemListener(this);
		pp = new Panel(new BorderLayout());
		pp.add(specList, BorderLayout.CENTER);
		p = new Panel(new FlowLayout(FlowLayout.CENTER, 10, 2));
		b = new Button("- all");
		b.setActionCommand("deselect_all");
		b.addActionListener(this);
		p.add(b);
		b = new Button("+ all");
		b.setActionCommand("select_all");
		b.addActionListener(this);
		p.add(b);
		pp.add(p, BorderLayout.SOUTH);
		sp.add(pp, BorderLayout.WEST);
		specDetailsPanel = new Panel(new ColumnLayout());
		ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
		scp.add(specDetailsPanel);
		sp.add(scp, BorderLayout.CENTER);
		pp = new Panel(new ColumnLayout());
		add(pp, BorderLayout.SOUTH);

		p = new Panel(new FlowLayout(FlowLayout.CENTER, 10, 2));
		pp.add(p);
		whatToShowCBG = new CheckboxGroup();
		showOnlySpecimenCB = new Checkbox("Show only the prototype(s)", false, whatToShowCBG);
		showOnlySpecimenCB.addItemListener(this);
		p.add(showOnlySpecimenCB);
		showSubclusterCB = new Checkbox("Show the subcluster(s)", false, whatToShowCBG);
		showSubclusterCB.addItemListener(this);
		p.add(showSubclusterCB);

		p = new Panel(new FlowLayout(FlowLayout.LEFT, 10, 2));
		pp.add(p);
		p.add(new Label("   "));
		b = new Button("Refine the subcluster");
		b.setActionCommand("refine_sub");
		b.addActionListener(this);
		p.add(b);
		b = new Button("Merge the subclusters");
		b.setActionCommand("merge_sub");
		b.addActionListener(this);
		p.add(b);
		p = new Panel(new FlowLayout(FlowLayout.RIGHT, 10, 2));
		pp.add(p);
		b = new Button("Remove the prototype(s)");
		b.setActionCommand("remove_specimen");
		b.addActionListener(this);
		p.add(b);
		b = new Button("Exclude the subcluster(s)");
		b.setActionCommand("exclude_subcluster");
		b.addActionListener(this);
		p.add(b);
		p.add(new Label(" "));

		pp.add(new Line(false));
		p = new Panel(new FlowLayout(FlowLayout.LEFT, 10, 2));
		pp.add(p);
		sp = new Panel(new ColumnLayout());
		p.add(sp);
		sp.add(new Label("Test strategy:", Label.CENTER));
		CheckboxGroup cbg = new CheckboxGroup();
		closestCB = new Checkbox("find the closest* prototype", true, cbg);
		closestCB.addItemListener(this);
		sp.add(closestCB);
		firstCB = new Checkbox("pick the first close* prototype", false, cbg);
		firstCB.addItemListener(this);
		sp.add(firstCB);
		sp.add(new Label("* such that distance < threshold", Label.CENTER));
		b = new Button("Test the classifier");
		b.setActionCommand("test");
		b.addActionListener(this);
		Panel pb = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 2));
		pb.add(b);
		sp.add(pb);

		p.add(new Line(true));
		GridBagLayout gridbag = new GridBagLayout();
		sp = new Panel(gridbag);
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0f;
		c.weighty = 1.0f;
		c.fill = GridBagConstraints.HORIZONTAL;

		p.add(sp);
		Label l = new Label("Test results for the selected cluster:", Label.CENTER);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		sp.add(l);
		clusterTestResultsCB = new Checkbox[5];
		clusterTestResultsTF = new TextField[5];
		clusterTestResultsCB[0] = new Checkbox("N original members:", true, whatToShowCBG);
		clusterTestResultsCB[1] = new Checkbox("Correctly classified:", false, whatToShowCBG);
		clusterTestResultsCB[2] = new Checkbox("False negatives:", false, whatToShowCBG);
		clusterTestResultsCB[3] = new Checkbox("False positives:", false, whatToShowCBG);
		clusterTestResultsCB[4] = new Checkbox("Not in any cluster:", false, whatToShowCBG);
		for (int i = 0; i < 5; i++) {
			if (i == 4) {
				Line ln = new Line(false);
				c.gridwidth = GridBagConstraints.REMAINDER;
				c.fill = GridBagConstraints.HORIZONTAL;
				gridbag.setConstraints(ln, c);
				sp.add(ln);
			}
			clusterTestResultsCB[i].addItemListener(this);
			clusterTestResultsCB[i].setEnabled(i == 0);
			c.gridwidth = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			gridbag.setConstraints(clusterTestResultsCB[i], c);
			sp.add(clusterTestResultsCB[i]);
			clusterTestResultsTF[i] = new TextField("0", 5);
			clusterTestResultsTF[i].setEditable(false);
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.fill = GridBagConstraints.NONE;
			gridbag.setConstraints(clusterTestResultsTF[i], c);
			sp.add(clusterTestResultsTF[i]);
		}
		pp.add(new Line(false));
		p = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 2));
		pp.add(p);
		undoB = new Button("Undo");
		undoB.setActionCommand("undo");
		undoB.addActionListener(this);
		p.add(undoB);
		undoB.setEnabled(false);
		pp.add(new Line(false));

		resetPrimaryClusters(clusteredObjects);
		clustersSeen.addElement(clusterCh.getSelectedItem());

		extraTbl = new ClassifierBuilderUIextraTable(core.getSupervisor());
		extraTbl.addWindowListener(this);
		updateExtraStatTable();
		//vLFIdxBuffer=new Vector<String>(20,10); // buffer for the index file
	}

	/**
	 * Updates the UI according to the changes in the clustersInfo.
	 */
	public void resetPrimaryClusters(DClusterObject clusteredObjects[]) {
		restoreState(0, false);
		clustersInfo = (ClustersInfo) origClustersInfo.clone();
		if (states != null) {
			states.removeAllElements();
		}
		clearTestResults();
		update(clusteredObjects);
	}

	protected void checkWhatToShow() {
		if (!whatToShowCBG.getSelectedCheckbox().isEnabled()) {
			whatToShowCBG.setSelectedCheckbox(clusterTestResultsCB[0]);
			applyFilters();
		}
	}

	protected void clearTestResults() {
		if (clAssigner != null) {
			clAssigner.clearPreviousResults();
		}
		clAssigner = null;
		testResults = null;
		for (int i = 1; i < 5; i++) {
			clusterTestResultsTF[i].setText("0");
			clusterTestResultsCB[i].setEnabled(false);
		}
		checkWhatToShow();
	}

	protected void update(DClusterObject clusteredObjects[]) {
		this.clusteredObjects = clusteredObjects;
		clusterCh.removeAll();
		specList.removeAll();
		specDetailsPanel.removeAll();
		if (clustersInfo == null || clustersInfo.getClustersCount() < 1)
			return;
		//
		for (int i = 0; i < clustersInfo.getClustersCount(); i++) {
			clusterCh.addItem(clustersInfo.getSingleClusterInfo(i).clusterLabel);
		}
		if (selClusterIdx < 0 || selClusterIdx >= clusterCh.getItemCount()) {
			selClusterIdx = 0;
		}
		clusterCh.select(selClusterIdx);
		//
		pClusterLabels.removeAll();
		boolean smthAdded = false;
		for (int i = 0; i < clustersInfo.getClustersCount(); i++) {
			if (i >= clusterLabels.size()) {
				Label l = new Label("" + (1 + i), Label.CENTER);
				l.addMouseListener(this);
				clusterLabels.addElement(l);
				smthAdded = true;
			}
			pClusterLabels.add(clusterLabels.elementAt(i));
		}
		if (smthAdded) {
			pClusterLabels.invalidate();
			CManager.validateAll(pClusterLabels);
		}
		//
		setClusterLabelsStyles();
		showClusterInfo();
		if (extraTbl != null) {
			extraTbl.selectSingleRow(clusterCh.getSelectedItem());
		}
		if (spFilter != null) {
			spLayer.removeObjectFilter(spFilter);
		}
		spFilter = null;
		checkWhatToShow();
		applyFilters();
	}

	/**
	 * Shows the information about the currently selected cluster
	 */
	protected void showClusterInfo() {
		if (selClusterIdx < 0)
			return;
		SingleClusterInfo clIn = clustersInfo.getSingleClusterInfo(selClusterIdx);
		specList.removeAll();
		for (int i = 0; i < clIn.getSpecimensCount(); i++) {
			ClusterSpecimenInfo csp = clIn.getClusterSpecimenInfo(i);
			specList.add((i + 1) + ") " + csp.specimen.id);
			specList.select(i);
		}
		SingleClusterInfo scl = clustersInfo.getSingleClusterInfo(selClusterIdx);
		clusterTestResultsTF[0].setText(String.valueOf(scl.origSize));
		fillSpecDetailsPanel();
		showClusterTestResults();
	}

	protected void fillSpecDetailsPanel() {
		specDetailsPanel.removeAll();
		if (selClusterIdx < 0)
			return;
		SingleClusterInfo clIn = clustersInfo.getSingleClusterInfo(selClusterIdx);
		// replaced by a table
		/*
		int k=0;
		for (int i=0; i<clIn.getSpecimensCount(); i++)
		  if (specList.isIndexSelected(i)) {
		    ++k;
		    if (k>1) specDetailsPanel.add(new Line(false));
		    ClusterSpecimenInfo csp=clIn.getClusterSpecimenInfo(i);
		    specDetailsPanel.add(new Label(specList.getItem(i)));
		    Panel p=new Panel(new RowLayout(10,0));
		    specDetailsPanel.add(p);
		    p.add(new Label("Distance threshold:   "+String.valueOf(csp.distanceThr)+"   "));
		    Button b=new Button("Change");
		    b.setActionCommand("change_threshold_"+i);
		    b.addActionListener(this);
		    p.add(b);
		    if (!Double.isNaN(csp.origDistanceThr) && csp.origDistanceThr!=csp.distanceThr) {
		      b=new Button("Reset");
		      b.setActionCommand("reset_threshold_"+i);
		      b.addActionListener(this);
		      p.add(b);
		    }
		    specDetailsPanel.add(new Label("Original N of neighbours (subcluster size):   "+String.valueOf(csp.nSimilarOrig)));
		    specDetailsPanel.add(new Label("N of neighbours found in the test:   "+String.valueOf(csp.nSimilarNew)));
		    specDetailsPanel.add(new Label("Mean distance to the original neighbours:   "+String.valueOf(csp.meanDistOrig)));
		    specDetailsPanel.add(new Label("Mean distance to the found neighbours:   "+String.valueOf(csp.meanDistNew)));
		  }
		*/
		// table to replace panel
		final DataTable freqTable = new DataTable();
		freqTable.setEntitySetIdentifier("tmp_tbl");
		freqTable.addAttribute("Distance threshold", "dist_thr", AttributeTypes.real);
		freqTable.addAttribute("Original N of neighbours (subcluster size)", "subcl_size", AttributeTypes.integer);
		freqTable.addAttribute("N of neighbours found in the test", "n_neighb_test", AttributeTypes.integer);
		freqTable.addAttribute("Mean distance to the original neighbours", "dist_orig", AttributeTypes.real);
		freqTable.addAttribute("Mean distance to the found neighbours", "dist_test", AttributeTypes.real);
		double maxvals[] = new double[3];
		for (int i = 0; i < maxvals.length; i++) {
			maxvals[i] = 0;
		}
		for (int i = 0; i < clIn.getSpecimensCount(); i++)
			if (specList.isIndexSelected(i)) {
				ClusterSpecimenInfo csp = clIn.getClusterSpecimenInfo(i);
				if (maxvals[0] < csp.distanceThr) {
					maxvals[0] = csp.distanceThr;
				}
				if (maxvals[1] < csp.meanDistOrig) {
					maxvals[1] = csp.meanDistOrig;
				}
				if (maxvals[2] < csp.meanDistNew) {
					maxvals[2] = csp.meanDistNew;
				}
			}
		for (int i = 0; i < clIn.getSpecimensCount(); i++)
			if (specList.isIndexSelected(i)) {
				ClusterSpecimenInfo csp = clIn.getClusterSpecimenInfo(i);
				DataRecord rec = new DataRecord(specList.getItem(i));
				freqTable.addDataRecord(rec);
				rec.setNumericAttrValue(csp.distanceThr, StringUtil.doubleToStr(csp.distanceThr, 0, maxvals[0]), 0);
				rec.setNumericAttrValue(csp.nSimilarOrig, "" + csp.nSimilarOrig, 1);
				rec.setNumericAttrValue(csp.nSimilarNew, "" + csp.nSimilarNew, 2);
				rec.setNumericAttrValue(csp.meanDistOrig, StringUtil.doubleToStr(csp.meanDistOrig, 0, maxvals[1]), 3);
				rec.setNumericAttrValue(csp.meanDistNew, StringUtil.doubleToStr(csp.meanDistNew, 0, maxvals[2]), 4);
			}
		TableViewer tableViewer = new TableViewer(freqTable, core.getSupervisor(), new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
			}
		});
		tableViewer.setTreatItemNamesAsNumbers(true);
		Vector attr = new Vector(freqTable.getAttrCount(), 1);
		for (int i = 0; i < freqTable.getAttrCount(); i++) {
			attr.addElement(freqTable.getAttributeId(i));
		}
		tableViewer.setVisibleAttributes(attr);
		tableViewer.setTableLens(true);
		Panel p = new Panel(new BorderLayout());
		p.add(tableViewer, BorderLayout.CENTER);
		p.add(new Label("* click row to change distance threshold"), BorderLayout.NORTH);
		specDetailsPanel.add(p);
		core.getSupervisor().getHighlighter(freqTable.getEntitySetIdentifier()).addHighlightListener(new HighlightListener() {
			public void highlightSetChanged(Object source, String setId, Vector highlighted) {
			}

			public void selectSetChanged(Object source, String setId, Vector highlighted) {
				int rowN = -1;
				if (highlighted != null) {
					for (int i = 0; i < highlighted.size() && rowN == -1; i++) {
						String id = ((String) highlighted.elementAt(i));
						rowN = freqTable.getObjectIndex(id);
					}
				}
				if (rowN >= 0) {
					actionPerformed(new ActionEvent(this, 0, "change_threshold_" + rowN));
					core.getSupervisor().getHighlighter(freqTable.getEntitySetIdentifier()).clearSelection(this);
				}
			}
		});
		// end of table
		CManager.validateAll(specDetailsPanel);
	}

	protected int getNUnclassified() {
		if (unclassified == null)
			return 0;
		return unclassified.size();
	}

	protected void setClusterLabelsStyles() {
		for (int i = 0; i < clusterLabels.size(); i++) {
			Label l = clusterLabels.elementAt(i);
			if (l.getFont() == null)
				return;
			String str = l.getText();
			if (clustersInspected.contains(str)) {
				l.setFont(l.getFont().deriveFont(Font.BOLD));
			} else if (clustersSeen.contains(str)) {
				l.setFont(l.getFont().deriveFont(Font.PLAIN));
			} else {
				l.setFont(l.getFont().deriveFont(Font.ITALIC));
			}
		}
	}

	protected void updateExtraStatTable() {
		if (extraTbl == null)
			return;
		DataTable tbl = extraTbl.getTblStat();
		tbl.removeAllData();
		double overallMax = 0;
		for (int i = 0; i < clustersInfo.getClustersCount(); i++) {
			SingleClusterInfo scl = clustersInfo.getSingleClusterInfo(i);
			for (int j = 0; j < scl.getSpecimensCount(); j++)
				if (scl.getClusterSpecimenInfo(j).distanceThr > overallMax) {
					overallMax = scl.getClusterSpecimenInfo(j).distanceThr;
				}
		}
		for (int i = 0; i < clustersInfo.getClustersCount(); i++) {
			SingleClusterInfo scl = clustersInfo.getSingleClusterInfo(i);
			if (scl == null) {
				continue;
			}
			DataRecord rec = new DataRecord(scl.clusterLabel);
			rec.setNumericAttrValue(scl.getSpecimensCount(), "" + scl.getSpecimensCount(), 0);
			double max = 0f;
			for (int j = 0; j < scl.getSpecimensCount(); j++)
				if (scl.getClusterSpecimenInfo(j).distanceThr > max) {
					max = scl.getClusterSpecimenInfo(j).distanceThr;
				}
			if (max > 0) {
				rec.setNumericAttrValue(max, StringUtil.doubleToStr(max, 0, overallMax), 1);
			}
			rec.setNumericAttrValue(scl.origSize, "" + scl.origSize, 2);
			if (testResults != null && testResults.elementAt(i) != null) {
				ClusterTestResult ctr = testResults.elementAt(i);
				rec.setNumericAttrValue(ctr.getNCorrectlyClassified(), "" + ctr.getNCorrectlyClassified(), 3);
				rec.setNumericAttrValue(ctr.getNFalseNegatives(), "" + ctr.getNFalseNegatives(), 4);
				rec.setNumericAttrValue(ctr.getNFalsePositives(), "" + ctr.getNFalsePositives(), 5);
			}
			tbl.addDataRecord(rec);
		}
		tbl.notifyPropertyChange("data_updated", null, null);
		CManager.validateAll(extraTbl);
	}

	protected void showClusterTestResults() {
		updateExtraStatTable();
		if (testResults == null || selClusterIdx < 0 || testResults.size() <= selClusterIdx || testResults.elementAt(selClusterIdx) == null) {
			for (int i = 1; i < 5; i++) {
				clusterTestResultsTF[i].setText("0");
				clusterTestResultsCB[i].setEnabled(false);
			}
			if (clusterLabels != null) {
				for (int i = 0; i < clusterLabels.size(); i++) {
					clusterLabels.elementAt(i).setBackground(null);
				}
			}
			checkWhatToShow();
			return;
		}
		setClusterLabelsStyles();
		for (int i = 0; i < testResults.size(); i++) {
			ClusterTestResult ctr = testResults.elementAt(i);
			if (i < clusterLabels.size()) {
				Label l = clusterLabels.elementAt(i);
				l.setBackground(((ctr.getNFalseNegatives() > 0) || ctr.getNFalsePositives() > 0) ? Color.PINK : Color.GREEN.brighter());
				SingleClusterInfo scl = clustersInfo.getSingleClusterInfo(i);
				new PopupManager(l, "Cluster " + (i + 1) + "\n"
						+ ((clustersInspected.contains(clusterLabels.elementAt(i).getText())) ? "already inspected\n" : (clustersSeen.contains(clusterLabels.elementAt(i).getText())) ? "already seen\n" : "not seen yet\n") + scl.origSize
						+ " original members\n" + ctr.getNCorrectlyClassified() + " correctly classified\n" + ctr.getNFalseNegatives() + " false negatives\n" + ctr.getNFalsePositives() + " false positives\n", true);
			}
		}
		ClusterTestResult ctr = testResults.elementAt(selClusterIdx);
		clusterTestResultsTF[1].setText(String.valueOf(ctr.getNCorrectlyClassified()));
		clusterTestResultsCB[1].setEnabled(ctr.getNCorrectlyClassified() > 0);
		clusterTestResultsTF[2].setText(String.valueOf(ctr.getNFalseNegatives()));
		clusterTestResultsCB[2].setEnabled(ctr.getNFalseNegatives() > 0);
		clusterTestResultsTF[2].setBackground((ctr.getNFalseNegatives() > 0) ? Color.PINK : null);
		clusterTestResultsTF[3].setText(String.valueOf(ctr.getNFalsePositives()));
		clusterTestResultsCB[3].setEnabled(ctr.getNFalsePositives() > 0);
		clusterTestResultsTF[3].setBackground((ctr.getNFalsePositives() > 0) ? Color.PINK : null);
		clusterTestResultsTF[4].setText(String.valueOf(getNUnclassified()));
		clusterTestResultsCB[4].setEnabled(getNUnclassified() > 0);
		checkWhatToShow();
	}

	protected void applyFilters() {
		if (selClusterIdx < 0) {
			if (origLayerFilter != null) {
				origLayerFilter.clearFilter();
			}
			if (spFilter != null) {
				spFilter.clearFilter();
			}
			return;
		}
		int clusterN = clustersInfo.getSingleClusterInfo(selClusterIdx).clusterN;
		if (spFilter == null) {
			spFilter = new ObjectFilterBySelection();
			spFilter.setObjectContainer(spLayer);
			spFilter.setEntitySetIdentifier(spLayer.getEntitySetIdentifier());
			spLayer.setObjectFilter(spFilter);
		}
		Vector specIds = new Vector(10, 10);
		SingleClusterInfo clIn = clustersInfo.getSingleClusterInfo(selClusterIdx);
		int k = 0;
		for (int i = 0; i < clIn.getSpecimensCount(); i++)
			if (specList.isIndexSelected(i)) {
				specIds.addElement(clIn.getClusterSpecimenInfo(i).specimen.id);
			}
		spFilter.setActiveObjects(specIds);

		if (origLayer == null)
			return;
		if (!origLayer.getLayerDrawn()) {
			origLayer.setLayerDrawn(true);
		}
		if (origLayerFilter == null) {
			origLayerFilter = new ObjectFilterBySelection();
			origLayerFilter.setObjectContainer(origLayer);
			origLayerFilter.setEntitySetIdentifier(origLayer.getEntitySetIdentifier());
			origLayer.removeObjectFilter(origLayer.getObjectFilter());
			origLayer.setObjectFilter(origLayerFilter);
		}
		ClusterTestResult ctr = null;
		if (testResults != null) {
			ctr = testResults.elementAt(selClusterIdx);
		}
		if (ctr != null && clusterTestResultsCB[1].getState()) {
			origLayerFilter.setActiveObjectIndexes(ctr.correctlyClassified);
		} else if (ctr != null && clusterTestResultsCB[2].getState()) {
			origLayerFilter.setActiveObjectIndexes(ctr.falseNegatives);
		} else if (ctr != null && clusterTestResultsCB[3].getState()) {
			origLayerFilter.setActiveObjectIndexes(ctr.falsePositives);
		} else if (unclassified != null && clusterTestResultsCB[4].getState()) {
			origLayerFilter.setActiveObjectIndexes(unclassified);
		} else if (showOnlySpecimenCB.getState()) {
			origLayerFilter.setActiveObjects(specIds);
		} else {
			IntArray idxs = new IntArray(100, 100);
			for (DClusterObject clusteredObject : clusteredObjects) {
				if (clusteredObject.clusterIdx + 1 == clusterN)
					if (!showSubclusterCB.getState() || (clusteredObject.subIdx >= 0 && specList.isIndexSelected(clusteredObject.subIdx))) {
						idxs.addElement(clusteredObject.idx);
					}
			}
			origLayerFilter.setActiveObjectIndexes(idxs);
		}
	}

	public void testClusters() {
		ActionDescr aDescr = new ActionDescr();
		aDescr.aName = "Test the classifier";
		aDescr.startTime = System.currentTimeMillis();

		if (clAssigner == null) {
			clAssigner = new ObjectsToClustersAssigner(clustersInfo);
		} else {
			clAssigner.clearPreviousResults();
		}
		clAssigner.mustFindClosest = closestCB.getState();
		if (testResults == null) {
			testResults = new Vector<ClusterTestResult>(Math.max(10, clustersInfo.getClustersCount()), 10);
		} else {
			testResults.removeAllElements();
		}
		;
		for (int i = 0; i < clustersInfo.getClustersCount(); i++) {
			ClusterTestResult ctr = new ClusterTestResult();
			ctr.clusterN = clustersInfo.getSingleClusterInfo(i).clusterN;
			testResults.addElement(ctr);
		}
		if (unclassified == null) {
			unclassified = new IntArray(100, 100);
		} else {
			unclassified.removeAllElements();
		}
		for (DClusterObject clusteredObject : clusteredObjects) {
			ObjectToClusterAssignment oclas = (ObjectToClusterAssignment) clAssigner.processObject(origLayer.getObject(clusteredObject.idx));
			if (oclas == null) {
				unclassified.addElement(clusteredObject.idx);
				continue;
			}
			if (oclas.clusterN < 0) {
				unclassified.addElement(clusteredObject.idx);
			}
			ClusterTestResult ctr = getClusterTestResult(clusteredObject.clusterIdx + 1);
			if (ctr != null)
				if (ctr.clusterN == oclas.clusterN) {
					ctr.addCorrectlyClassified(clusteredObject.idx);
				} else {
					ctr.addFalseNegative(clusteredObject.idx);
				}
			if (oclas.clusterN > 0 && clusteredObject.clusterIdx + 1 != oclas.clusterN) {
				ctr = getClusterTestResult(oclas.clusterN);
				if (ctr != null) {
					ctr.addFalsePositive(clusteredObject.idx);
				}
			}
		}
		showClusterTestResults();
		fillSpecDetailsPanel();

		aDescr.endTime = System.currentTimeMillis();
		int nClOk = 0, nClFalsePos = 0, nClFalseNeg = 0, nFalsePos = 0, nFalseNeg = 0;
		for (int i = 0; i < testResults.size(); i++) {
			ClusterTestResult ctr = testResults.elementAt(i);
			int nfneg = ctr.getNFalseNegatives(), nfpos = ctr.getNFalsePositives();
			if (nfneg < 1 && nfpos < 1) {
				++nClOk;
			} else {
				if (nfneg > 0) {
					++nClFalseNeg;
				}
				if (nfpos > 0) {
					++nClFalsePos;
				}
				nFalseNeg += nfneg;
				nFalsePos += nfpos;
			}
		}

		aDescr.addParamValue("N clusters OK", new Integer(nClOk));
		aDescr.addParamValue("N clusters with false positives", new Integer(nClFalsePos));
		aDescr.addParamValue("N clusters with false negatives", new Integer(nClFalseNeg));
		aDescr.addParamValue("Total N false positives", new Integer(nFalsePos));
		aDescr.addParamValue("Total N false negatives", new Integer(nFalseNeg));
		core.logAction(aDescr);

		composeDTstr();
/*
    sLFlastCommand=sLFdt_fmt+" Test clusters, active="+clustersInfo.getSingleClusterInfo(selClusterIdx).clusterLabel;
    core.logSimpleAction(sLFlastCommand);
*/
		//vLFIdxBuffer.addElement("<P>"+sLFlastCommand);
		//printLogFileIndex();
	}

	protected void divideClusterUsingSpecimens(SingleClusterInfo clIn, Vector<DClusterObject> specimens) {
		if (clIn == null || specimens == null || specimens.size() < 2)
			return;
		ActionDescr aDescr = new ActionDescr();
		aDescr.aName = "Divide cluster " + clIn.clusterLabel + " in " + specimens.size() + " clusters using selected objects as seeds";
		for (int i = 0; i < specimens.size(); i++) {
			aDescr.addParamValue("Seed " + (i + 1), specimens.elementAt(i).id);
		}
		aDescr.startTime = System.currentTimeMillis();

		ArrayList<DClusterObject> data = new ArrayList<DClusterObject>(clIn.origSize);
		for (DClusterObject clusteredObject : clusteredObjects)
			if (clusteredObject.clusterIdx + 1 == clIn.clusterN) {
				data.add(clusteredObject);
			}
		ArrayList<DClusterObject> seeds = new ArrayList<DClusterObject>(specimens.size());
		for (int i = 0; i < specimens.size(); i++) {
			seeds.add(specimens.elementAt(i));
		}
		KMedoids<DClusterObject> km = new KMedoids<DClusterObject>(clustersInfo.distanceMeter);
		HashMap<DClusterObject, ArrayList<DClusterObject>> result = km.doClustering(data, seeds);
		if (result == null || result.size() < 2) {
			Dialogs.showMessage(CManager.getAnyFrame(this), "K-Medoids algorithm has not produced any clusters!", "Refinement failed!");
			return;
		}
		origLayer.setLayerDrawn(true);
		spLayer.setLayerDrawn(false);
		workMapView.getLayerManager().activateLayer(origLayer.getContainerIdentifier());
		Frame win = CManager.getAnyFrame((Component) workMapView);
		if (win != null) {
			win.toFront();
		}
		CusterRefinementViewer clRefV = new CusterRefinementViewer(result, km, origLayer, core);
		OKDialog dia = new OKDialog(CManager.getAnyFrame(this), "Cluster refinement", true);
		dia.addContent(clRefV);
		dia.show();
		clRefV.destroy();
		spLayer.setLayerDrawn(true);
		if (!dia.wasCancelled()) {
			Vector<SingleClusterInfo> clAdded = applyClusterDivision(clIn, result);
			aDescr.endTime = System.currentTimeMillis();
			if (clAdded != null && clAdded.size() > 0) {
				aDescr.addParamValue("N added clusters", new Integer(clAdded.size()));
				for (int i = 0; i < clAdded.size(); i++) {
					SingleClusterInfo cl = clAdded.elementAt(i);
					aDescr.addParamValue("Added cluster " + (i + 1) + " label", cl.clusterLabel);
					aDescr.addParamValue("Added cluster " + (i + 1) + " size", new Integer(cl.origSize));
					aDescr.addParamValue("Added cluster " + (i + 1) + " N prototypes", new Integer(cl.getSpecimensCount()));
				}
			}
			core.logAction(aDescr);
			composeDTstr();
/*
      sLFlastCommand=sLFdt_fmt+" Divide cluster user specimens, active="+clustersInfo.getSingleClusterInfo(selClusterIdx).clusterLabel;
      core.logSimpleAction(sLFlastCommand);
*/
			//vLFIdxBuffer.addElement("<P>"+sLFlastCommand);
			//printLogFileIndex();
		}
	}

	protected void divideClusterInKParts(int k) {
		if (selClusterIdx < 0 || k < 2)
			return;
		SingleClusterInfo clIn = clustersInfo.getSingleClusterInfo(selClusterIdx);
		ActionDescr aDescr = new ActionDescr();
		aDescr.aName = "Divide cluster " + clIn.clusterLabel + " in " + k + " clusters using K-Medoids algorithm";
		aDescr.startTime = System.currentTimeMillis();
		ArrayList<DClusterObject> data = new ArrayList<DClusterObject>(clIn.origSize);
		for (DClusterObject clusteredObject : clusteredObjects)
			if (clusteredObject.clusterIdx + 1 == clIn.clusterN) {
				data.add(clusteredObject);
			}
		KMedoids<DClusterObject> km = new KMedoids<DClusterObject>(clustersInfo.distanceMeter);
		HashMap<DClusterObject, ArrayList<DClusterObject>> result = km.doClustering(data, k);
		if (result == null || result.size() < 2) {
			Dialogs.showMessage(CManager.getAnyFrame(this), "K-Medoids algorithm has not produced any clusters!", "Refinement failed!");
			return;
		}
		origLayer.setLayerDrawn(true);
		spLayer.setLayerDrawn(false);
		workMapView.getLayerManager().activateLayer(origLayer.getContainerIdentifier());
		Frame win = CManager.getAnyFrame((Component) workMapView);
		if (win != null) {
			win.toFront();
		}
		CusterRefinementViewer clRefV = new CusterRefinementViewer(result, km, origLayer, core);
		OKDialog dia = new OKDialog(CManager.getAnyFrame(this), "Cluster refinement", true);
		dia.addContent(clRefV);
		dia.show();
		clRefV.destroy();
		spLayer.setLayerDrawn(true);
		if (!dia.wasCancelled()) {
			Vector<SingleClusterInfo> clAdded = applyClusterDivision(clIn, result);
			aDescr.endTime = System.currentTimeMillis();
			if (clAdded != null && clAdded.size() > 0) {
				aDescr.addParamValue("N added clusters", new Integer(clAdded.size()));
				for (int i = 0; i < clAdded.size(); i++) {
					SingleClusterInfo cl = clAdded.elementAt(i);
					aDescr.addParamValue("Added cluster " + (i + 1) + " label", cl.clusterLabel);
					aDescr.addParamValue("Added cluster " + (i + 1) + " size", new Integer(cl.origSize));
					aDescr.addParamValue("Added cluster " + (i + 1) + " N prototypes", new Integer(cl.getSpecimensCount()));
				}
			}
			core.logAction(aDescr);
			composeDTstr();
/*
      sLFlastCommand=sLFdt_fmt+" Divide cluster in K parts, active="+clustersInfo.getSingleClusterInfo(selClusterIdx).clusterLabel;
      core.logSimpleAction(sLFlastCommand);
*/
			//vLFIdxBuffer.addElement("<P>"+sLFlastCommand);
			//printLogFileIndex();
		}
	}

	protected ClusterSpecimenInfo getClusterSpecimenInfo(DClusterObject centre, ArrayList<DClusterObject> cluster) {
		if (centre == null || cluster == null || cluster.size() < 1)
			return null;
		ClusterSpecimenInfo clSpec = new ClusterSpecimenInfo();
		clSpec.specimen = centre;
		clSpec.nSimilarOrig = cluster.size();
		clSpec.distanceThr = 0;
		double sumDist = 0;
		for (DClusterObject member : cluster)
			if (!member.equals(centre)) {
				double d = clustersInfo.distanceMeter.distance(centre, member);
				sumDist += d;
				if (d > clSpec.distanceThr) {
					clSpec.distanceThr = d;
				}
			}
		if (clSpec.distanceThr <= 0) {
			clSpec.distanceThr = maxSubclusterRadius / 5;
			if (Double.isNaN(maxSubclusterRadius)) {
				maxSubclusterRadius = getMaxDistThreshold();
			}
		}
		clSpec.origDistanceThr = clSpec.distanceThr;
		clSpec.meanDistOrig = sumDist / clSpec.nSimilarOrig;
		return clSpec;
	}

	/**
	 * Returns the descriptions of the new clusters
	 */
	protected Vector<SingleClusterInfo> applyClusterDivision(SingleClusterInfo clIn, HashMap<DClusterObject, ArrayList<DClusterObject>> division) {
		if (clIn == null || division == null || division.size() < 2)
			return null;

		int i0 = spTable.getAttrIndex("_cluster_N_"), i1 = spTable.getAttrIndex("_specimen_N_"), i2 = spTable.getAttrIndex("_distance_thr_"), i3 = spTable.getAttrIndex("_N_neighbours_"), i4 = spTable.getAttrIndex("_min_dist_neighb_");
		for (int i = 0; i < clIn.getSpecimensCount(); i++) {
			ClusterSpecimenInfo spec = clIn.getClusterSpecimenInfo(i);
			DataRecord rec = spTable.getDataRecord(spTable.indexOf(spec.specimen.id));
			if (rec == null) {
				continue;
			}
			rec.setAttrValue(null, i0);
			rec.setAttrValue(null, i1);
			rec.setAttrValue(null, i2);
			rec.setAttrValue(null, i3);
			rec.setAttrValue(null, i4);
		}

		Vector<DClusterObject> centroids = new Vector(division.keySet().size(), 10);
		Vector<ArrayList<DClusterObject>> clusters = new Vector<ArrayList<DClusterObject>>(centroids.capacity(), 10);
		int maxSize = 0, maxIdx = -1;
		for (DClusterObject centre : division.keySet()) {
			if (centre == null) {
				continue;
			}
			centroids.addElement(centre);
			ArrayList<DClusterObject> cluster = division.get(centre);
			clusters.addElement(cluster);
			int size = cluster.size();
			if (size > maxSize) {
				maxSize = size;
				maxIdx = clusters.size() - 1;
			}
		}
		Vector<SingleClusterInfo> specToAdd = new Vector<SingleClusterInfo>(centroids.capacity(), 10);
		ArrayList<DClusterObject> mainPart = clusters.elementAt(maxIdx);
		DClusterObject mainPartCentre = centroids.elementAt(maxIdx);
		clusters.removeElementAt(maxIdx);
		centroids.removeElementAt(maxIdx);
		SingleClusterInfo mainPartCl = new SingleClusterInfo();
		mainPartCl.clusterN = clIn.clusterN;
		mainPartCl.clusterLabel = clIn.clusterLabel;
		mainPartCl.origSize = mainPart.size();
		mainPartCl.addSpecimen(getClusterSpecimenInfo(mainPartCentre, mainPart));
		if (spLayer.findObjectById(mainPartCentre.id) == null) {
			specToAdd.addElement(mainPartCl);
		} else {
			ClusterSpecimenInfo spec = mainPartCl.getClusterSpecimenInfo(0);
			DataRecord rec = spTable.getDataRecord(spTable.indexOf(spec.specimen.id));
			if (rec != null) {
				rec.setAttrValue(mainPartCl.clusterLabel, i0);
				rec.setNumericAttrValue(1, "1", i1);
				rec.setNumericAttrValue(spec.distanceThr, String.valueOf(spec.distanceThr), i2);
				rec.setNumericAttrValue(spec.nSimilarOrig, String.valueOf(spec.nSimilarOrig), i3);
				rec.setNumericAttrValue(spec.meanDistOrig, String.valueOf(spec.meanDistOrig), i4);
			}
		}

		ClustersInfo clInfo = new ClustersInfo();
		clInfo.table = clustersInfo.table;
		clInfo.clustersColN = clustersInfo.clustersColN;
		clInfo.objContainer = clustersInfo.objContainer;
		clInfo.distanceMeter = clustersInfo.distanceMeter;
		clInfo.init(Math.max(clustersInfo.getClustersCount() + clusters.size(), 10), 10);
		int maxClusterN = clIn.clusterN;
		for (int i = 0; i < clustersInfo.getClustersCount(); i++) {
			SingleClusterInfo scl = clustersInfo.getSingleClusterInfo(i);
			if (scl.clusterN != clIn.clusterN) {
				clInfo.addSingleClusterInfo((SingleClusterInfo) clustersInfo.getSingleClusterInfo(i).clone());
				if (scl.clusterN > maxClusterN) {
					maxClusterN = scl.clusterN;
				}
			} else {
				clInfo.addSingleClusterInfo(mainPartCl);
			}
		}
		Vector<SingleClusterInfo> newClusters = new Vector<SingleClusterInfo>(10, 10);
		for (int i = 0; i < clusters.size(); i++) {
			ArrayList<DClusterObject> cluster = clusters.elementAt(i);
			SingleClusterInfo scl = new SingleClusterInfo();
			scl.clusterN = ++maxClusterN;
			scl.clusterLabel = String.valueOf(scl.clusterN);
			scl.origSize = cluster.size();
			scl.addSpecimen(getClusterSpecimenInfo(centroids.elementAt(i), cluster));
			clInfo.addSingleClusterInfo(scl);
			newClusters.addElement(scl);
			if (spLayer.findObjectById(centroids.elementAt(i).id) == null) {
				specToAdd.addElement(scl);
			} else {
				ClusterSpecimenInfo spec = scl.getClusterSpecimenInfo(0);
				DataRecord rec = spTable.getDataRecord(spTable.indexOf(spec.specimen.id));
				if (rec != null) {
					rec.setAttrValue(scl.clusterLabel, i0);
					rec.setNumericAttrValue(1, "1", i1);
					rec.setNumericAttrValue(spec.distanceThr, String.valueOf(spec.distanceThr), i2);
					rec.setNumericAttrValue(spec.nSimilarOrig, String.valueOf(spec.nSimilarOrig), i3);
					rec.setNumericAttrValue(spec.meanDistOrig, String.valueOf(spec.meanDistOrig), i4);
				}
			}
		}
		DClusterObject clObj[] = new DClusterObject[clusteredObjects.length];
		for (int i = 0; i < clusteredObjects.length; i++)
			if (clusteredObjects[i].clusterIdx + 1 == clIn.clusterN) {
				clObj[i] = (DClusterObject) clusteredObjects[i].clone();
				clObj[i].subIdx = 0;
				clObj[i].isSpecimen = false;
				clObj[i].specimenId = null;
				if (mainPart.contains(clusteredObjects[i])) {
					clObj[i].isSpecimen = clusteredObjects[i].equals(mainPartCentre);
					clObj[i].specimenId = mainPartCentre.id;
				} else {
					boolean found = false;
					for (int j = 0; j < clusters.size() && !found; j++)
						if (clusters.elementAt(j).contains(clusteredObjects[i])) {
							found = true;
							SingleClusterInfo scl = clInfo.getSingleClusterInfo(clustersInfo.getClustersCount() + j);
							clObj[i].clusterIdx = scl.clusterN - 1;
							ClusterSpecimenInfo cspec = scl.getClusterSpecimenInfo(0);
							if (cspec != null) {
								clObj[i].isSpecimen = clObj[i].equals(cspec.specimen);
								clObj[i].specimenId = cspec.specimen.id;
							}
						}
				}
			} else {
				clObj[i] = clusteredObjects[i];
			}
		ClassifierBuildState cbState = new ClassifierBuildState();
		cbState.clusteredObjects = clusteredObjects;
		cbState.clustersInfo = clustersInfo;
		cbState.nObjInSpecimensLayer = spLayer.getObjectCount();
		cbState.nRecInSpecimensTable = spTable.getDataItemCount();
		if (states == null) {
			states = new Vector<ClassifierBuildState>(20, 20);
		}
		states.addElement(cbState);

		spLayer.setLayerDrawn(true);
		if (specToAdd.size() > 0) {
			for (int i = 0; i < specToAdd.size(); i++) {
				SingleClusterInfo scl = specToAdd.elementAt(i);
				ClusterSpecimenInfo spec = scl.getClusterSpecimenInfo(0);
				DGeoObject gobj = (DGeoObject) getDGeoObject(spec.specimen).makeCopy();
				DataRecord rec = new DataRecord(gobj.getIdentifier(), gobj.getLabel());
				spTable.addDataRecord(rec);
				rec.setAttrValue(scl.clusterLabel, i0);
				rec.setNumericAttrValue(1, "1", i1);
				rec.setNumericAttrValue(spec.distanceThr, String.valueOf(spec.distanceThr), i2);
				rec.setNumericAttrValue(spec.nSimilarOrig, String.valueOf(spec.nSimilarOrig), i3);
				rec.setNumericAttrValue(spec.meanDistOrig, String.valueOf(spec.meanDistOrig), i4);
				gobj.setThematicData(rec);
				spLayer.addGeoObject(gobj);
			}
			spLayer.notifyPropertyChange("ObjectSet", null, null);
			spTable.notifyPropertyChange("data_added", null, null);
		} else {
			Vector v = new Vector(5, 1);
			v.addElement(spTable.getAttributeId(i0));
			v.addElement(spTable.getAttributeId(i1));
			v.addElement(spTable.getAttributeId(i2));
			v.addElement(spTable.getAttributeId(i3));
			v.addElement(spTable.getAttributeId(i4));
			spTable.notifyPropertyChange("values", null, v);
		}

		clearTestResults();
		clustersInfo = clInfo;
		update(clObj);
		undoB.setEnabled(true);
		return newClusters;
	}

	protected DGeoObject getDGeoObject(DClusterObject o) {
		if (o == null)
			return null;
		if (o.originalObject instanceof TrajectoryObject)
			return ((TrajectoryObject) o.originalObject).mobj;
		if (o.originalObject instanceof DGeoObject)
			return (DGeoObject) o.originalObject;
		return null;
	}

	protected void cleanCluster() {
		if (selClusterIdx < 0)
			return;
		Highlighter hl = core.getHighlighterForSet(origLayer.getEntitySetIdentifier());
		Vector selObj = hl.getSelectedObjects();
		if (selObj == null || selObj.size() < 1) {
			Dialogs.showMessage(getFrame(), "First select the cluster members you want to be excluded from the cluster.", "Nothing is selected!");
			return;
		}
		SingleClusterInfo clIn = clustersInfo.getSingleClusterInfo(selClusterIdx);
		IntArray oIdxs = new IntArray(selObj.size(), 1);
		int nGoodMembers = 0;
		for (int i = 0; i < clusteredObjects.length; i++)
			if (clusteredObjects[i].clusterIdx + 1 == clIn.clusterN) {
				int idx = StringUtil.indexOfStringInVectorIgnoreCase(clusteredObjects[i].id, selObj);
				if (idx >= 0) {
					oIdxs.addElement(i);
				} else {
					++nGoodMembers;
				}
			}
		if (oIdxs.size() < 1) {
			Dialogs.showMessage(getFrame(), "Select the cluster members you want to be excluded from the cluster. " + "The objects that are currently selected are not members of this cluster.", "Select cluster members to exclude!");
			return;
		}
		if (nGoodMembers < 1) {
			Dialogs.showMessage(getFrame(), "You have selected all cluster members! It is impossible to exclude all members from the cluster!", "No cluster members remain!");
			return;
		}
		selObj = new Vector(oIdxs.size(), 1);
		for (int i = 0; i < oIdxs.size(); i++) {
			int idx = oIdxs.elementAt(i);
			selObj.addElement(clusteredObjects[idx].id);
		}
		ActionDescr aDescr = new ActionDescr();
		aDescr.aName = "Exclude " + (oIdxs.size()) + " " + ((oIdxs.size() == 1) ? "member" : "members") + " from cluster " + clIn.clusterLabel;
		aDescr.addParamValue("Excluded members", selObj);
		aDescr.addParamValue("N remaining members", new Integer(nGoodMembers));
		aDescr.startTime = System.currentTimeMillis();

		Vector<DClusterObject> goodMembers = new Vector<DClusterObject>(nGoodMembers, 1);
		for (int i = 0; i < clusteredObjects.length; i++)
			if (clusteredObjects[i].clusterIdx + 1 == clIn.clusterN)
				if (oIdxs.indexOf(i) < 0) {
					goodMembers.addElement(clusteredObjects[i]);
				}

		HashMap<DClusterObject, ArrayList<DClusterObject>> result = null;
		if (Double.isNaN(maxSubclusterRadius)) {
			maxSubclusterRadius = getMaxDistThreshold();
		}
		if (nGoodMembers > 1) {
			maxSubclusterRadius = Dialogs.askForDoubleValue(CManager.getAnyFrame(this), "Maximum radius of a subcluster?", maxSubclusterRadius, maxSubclusterRadius / 5, maxSubclusterRadius * 5,
					"The cluster will be divided into \"round\" subclusters.", "Maximum subcluster radius", false);
			aDescr.addParamValue("Max subcluster radius", new Double(maxSubclusterRadius));
			RoundClustersProducer<DClusterObject> rClProd = new RoundClustersProducer<DClusterObject>();
			result = rClProd.getRoundClusters(goodMembers, 0, goodMembers.size(), clustersInfo.distanceMeter, maxSubclusterRadius);
			if (result == null || result.isEmpty()) {
				Dialogs.showMessage(CManager.getAnyFrame(this), "Failed to divide the cluster into subclusters!", "Division failed!");
				return;
			}
			aDescr.addParamValue("N subclusters", new Integer(result.keySet().size()));
		} else {
			aDescr.addParamValue("N subclusters", new Integer(1));
		}
		int i0 = spTable.getAttrIndex("_cluster_N_"), i1 = spTable.getAttrIndex("_specimen_N_"), i2 = spTable.getAttrIndex("_distance_thr_"), i3 = spTable.getAttrIndex("_N_neighbours_"), i4 = spTable.getAttrIndex("_min_dist_neighb_");
		for (int i = 0; i < clIn.getSpecimensCount(); i++) {
			DataRecord rec = spTable.getDataRecord(spTable.indexOf(clIn.getClusterSpecimenInfo(i).specimen.id));
			if (rec != null) {
				rec.setAttrValue(null, i0);
				rec.setAttrValue(null, i1);
				rec.setAttrValue(null, i2);
				rec.setAttrValue(null, i3);
				rec.setAttrValue(null, i4);
			}
		}
		DClusterObject dataCopy[] = new DClusterObject[goodMembers.size()];
		for (int i = 0; i < goodMembers.size(); i++) {
			dataCopy[i] = (DClusterObject) goodMembers.get(i).clone();
		}

		SingleClusterInfo clInCopy = new SingleClusterInfo();
		clInCopy.clusterN = clIn.clusterN;
		clInCopy.clusterLabel = clIn.clusterLabel;
		clInCopy.origSize = clIn.origSize - oIdxs.size();
		if (result != null) {
			for (DClusterObject centre : result.keySet()) {
				if (centre == null) {
					continue;
				}
				ArrayList<DClusterObject> cluster = result.get(centre);
				ClusterSpecimenInfo spec = getClusterSpecimenInfo(centre, cluster);
				clInCopy.addSpecimen(spec);
				int subIdx = clInCopy.getSpecimensCount() - 1;
				aDescr.addParamValue("New prototype " + (subIdx + 1) + " id", spec.specimen.id);
				aDescr.addParamValue("New subcluster " + (subIdx + 1) + " size", new Integer(spec.nSimilarOrig));
				for (int i = 0; i < goodMembers.size(); i++)
					if (cluster.contains(goodMembers.get(i))) {
						dataCopy[i].subIdx = subIdx;
						dataCopy[i].specimenId = centre.id;
						dataCopy[i].isSpecimen = dataCopy[i].id.equals(centre.id);
					}
			}
		} else {
			//A single member remains in the cluster. This will be the prototype.
			ClusterSpecimenInfo spec = new ClusterSpecimenInfo();
			spec.specimen = goodMembers.elementAt(0);
			spec.distanceThr = spec.origDistanceThr = maxSubclusterRadius / 5;
			spec.meanDistOrig = 0;
			spec.nSimilarOrig = 1;
			clInCopy.addSpecimen(spec);
			aDescr.addParamValue("New prototype id", spec.specimen.id);
			aDescr.addParamValue("New subcluster size", new Integer(1));
			dataCopy[0].subIdx = 0;
			dataCopy[0].specimenId = dataCopy[0].id;
			dataCopy[0].isSpecimen = true;
		}
		IntArray specToAdd = new IntArray(clInCopy.getSpecimensCount(), 10);
		for (int i = 0; i < clInCopy.getSpecimensCount(); i++) {
			ClusterSpecimenInfo spec = clInCopy.getClusterSpecimenInfo(i);
			if (spLayer.findObjectById(spec.specimen.id) == null) {
				specToAdd.addElement(i);
			} else {
				DataRecord rec = spTable.getDataRecord(spTable.indexOf(spec.specimen.id));
				if (rec != null) {
					rec.setAttrValue(clInCopy.clusterLabel, i0);
					rec.setNumericAttrValue(i + 1, String.valueOf(i + 1), i1);
					rec.setNumericAttrValue(spec.distanceThr, String.valueOf(spec.distanceThr), i2);
					rec.setNumericAttrValue(spec.nSimilarOrig, String.valueOf(spec.nSimilarOrig), i3);
					rec.setNumericAttrValue(spec.meanDistOrig, String.valueOf(spec.meanDistOrig), i4);
				}
			}
		}

		ClustersInfo clInfo = new ClustersInfo();
		clInfo.table = clustersInfo.table;
		clInfo.clustersColN = clustersInfo.clustersColN;
		clInfo.objContainer = clustersInfo.objContainer;
		clInfo.distanceMeter = clustersInfo.distanceMeter;
		clInfo.init(Math.max(clustersInfo.getClustersCount(), 10), 10);
		for (int i = 0; i < clustersInfo.getClustersCount(); i++) {
			SingleClusterInfo scl = clustersInfo.getSingleClusterInfo(i);
			if (scl.clusterN != clIn.clusterN) {
				clInfo.addSingleClusterInfo((SingleClusterInfo) clustersInfo.getSingleClusterInfo(i).clone());
			} else {
				clInfo.addSingleClusterInfo(clInCopy);
			}
		}

		DClusterObject clObj[] = new DClusterObject[clusteredObjects.length];
		for (int i = 0; i < clusteredObjects.length; i++)
			if (clusteredObjects[i].clusterIdx + 1 == clIn.clusterN) {
				int idx = goodMembers.indexOf(clusteredObjects[i]);
				if (idx >= 0) {
					clObj[i] = dataCopy[idx];
				} else {
					clObj[i] = (DClusterObject) clusteredObjects[i].clone();
					clObj[i].clusterIdx = -1;
					clObj[i].subIdx = -1;
					clObj[i].isSpecimen = false;
					clObj[i].specimenId = null;
				}
			} else {
				clObj[i] = clusteredObjects[i];
			}

		ClassifierBuildState cbState = new ClassifierBuildState();
		cbState.clusteredObjects = clusteredObjects;
		cbState.clustersInfo = clustersInfo;
		cbState.nObjInSpecimensLayer = spLayer.getObjectCount();
		cbState.nRecInSpecimensTable = spTable.getDataItemCount();
		if (states == null) {
			states = new Vector<ClassifierBuildState>(20, 20);
		}
		states.addElement(cbState);

		spLayer.setLayerDrawn(true);
		if (specToAdd.size() > 0) {
			for (int i = 0; i < specToAdd.size(); i++) {
				int subIdx = specToAdd.elementAt(i);
				ClusterSpecimenInfo spec = clInCopy.getClusterSpecimenInfo(subIdx);
				DGeoObject gobj = (DGeoObject) getDGeoObject(spec.specimen).makeCopy();
				DataRecord rec = new DataRecord(gobj.getIdentifier(), gobj.getLabel());
				spTable.addDataRecord(rec);
				rec.setAttrValue(clInCopy.clusterLabel, i0);
				rec.setNumericAttrValue(subIdx + 1, String.valueOf(subIdx + 1), i1);
				rec.setNumericAttrValue(spec.distanceThr, String.valueOf(spec.distanceThr), i2);
				rec.setNumericAttrValue(spec.nSimilarOrig, String.valueOf(spec.nSimilarOrig), i3);
				rec.setNumericAttrValue(spec.meanDistOrig, String.valueOf(spec.meanDistOrig), i4);
				gobj.setThematicData(rec);
				spLayer.addGeoObject(gobj);
			}
			spLayer.notifyPropertyChange("ObjectSet", null, null);
			spTable.notifyPropertyChange("data_added", null, null);
		} else {
			Vector v = new Vector(5, 1);
			v.addElement(spTable.getAttributeId(i0));
			v.addElement(spTable.getAttributeId(i1));
			v.addElement(spTable.getAttributeId(i2));
			v.addElement(spTable.getAttributeId(i3));
			v.addElement(spTable.getAttributeId(i4));
			spTable.notifyPropertyChange("values", null, v);
		}

		clearTestResults();
		clustersInfo = clInfo;
		update(clObj);
		undoB.setEnabled(true);

		aDescr.endTime = System.currentTimeMillis();
		core.logAction(aDescr);
		composeDTstr();

	}

	protected void refineCluster() {
		if (selClusterIdx < 0)
			return;
		SingleClusterInfo clIn = clustersInfo.getSingleClusterInfo(selClusterIdx);
		SelectDialog selDia = new SelectDialog(CManager.getAnyFrame(this), "Refine cluster", "Refine cluster " + clIn.clusterN + " by dividing into 2 or more clusters");
		int nSpec = clIn.getSpecimensCount();
		InputIntPanel iipNei = null, iipK = null;
		boolean optSelected = false;
		if (nSpec > 1) {
			selDia.addLabel("Use current prototypes as initial seeds of new clusters:");
			selDia.addOption("all " + nSpec + " prototypes", "all_spec", false);
			int selSpecIdxs[] = specList.getSelectedIndexes();
			if (selSpecIdxs != null && selSpecIdxs.length > 1 && selSpecIdxs.length < nSpec) {
				selDia.addOption("the " + selSpecIdxs.length + " selected prototypes", "sel_spec", true);
				optSelected = true;
			}
			if (nSpec > 2) {
				selDia.addOption("the prototypes having at least N neighbours", "spec_N_nei", false);
				iipNei = new InputIntPanel("N =", Math.max(clIn.origSize / nSpec, 5), 2, clIn.origSize, null);
				selDia.addComponent(iipNei);
			}
			selDia.addSeparator();
		}
		Highlighter hl = core.getHighlighterForSet(origLayer.getEntitySetIdentifier());
		Vector selObj = hl.getSelectedObjects();
		int nSelObj = (selObj == null) ? 0 : selObj.size();
		if (nSelObj > 1) {
			selObj = (Vector) selObj.clone();
			for (int i = 0; i < clusteredObjects.length && selObj.size() > 2; i++)
				if (clusteredObjects[i].clusterIdx + 1 != clIn.clusterN) {
					int idx = StringUtil.indexOfStringInVectorIgnoreCase(clusteredObjects[i].id, selObj);
					if (idx >= 0) {
						selObj.removeElementAt(idx);
					}
				}
			if (selObj.size() > 1) {
				if (selObj.size() < nSelObj) {
					hl.replaceSelectedObjects(this, selObj);
				}
				selDia.addOption("use " + selObj.size() + " currently selected objects as initial seeds", "sel_obj", true);
				selDia.addSeparator();
				optSelected = true;
			}
			nSelObj = selObj.size();
		}
		selDia.addOption("divide into K arbitrary clusters", "arbitrary", !optSelected);
		iipK = new InputIntPanel("K =", 2, 2, clIn.origSize / 2, null);
		selDia.addComponent(iipK);
		if (nSelObj < 2) {
			selDia.addSeparator();
			selDia.addLabel("Note: you can also select (e.g. by clicking) 2 or more arbitrary objects");
			selDia.addLabel("of the original cluster to be used as the seeds of the new clusters.");
		}
		selDia.show();
		if (selDia.wasCancelled())
			return;
		String optId = selDia.getSelectedOptionId();
		if (optId == null)
			return;
		if (optId.equals("arbitrary")) {
			if (!iipK.canClose()) {
				Dialogs.showMessage(CManager.getAnyFrame(this), iipK.getErrorMessage(), "Illegal number of clusters!");
			} else {
				divideClusterInKParts(iipK.getEnteredValue());
			}
		} else {
			Vector<DClusterObject> specimens = new Vector<DClusterObject>(Math.max(nSpec, nSelObj), 1);
			if (optId.equals("all_spec")) {
				for (int i = 0; i < nSpec; i++) {
					specimens.addElement(clIn.getClusterSpecimenInfo(i).specimen);
				}
			} else if (optId.equals("sel_spec")) {
				for (int i = 0; i < nSpec; i++)
					if (specList.isIndexSelected(i)) {
						specimens.addElement(clIn.getClusterSpecimenInfo(i).specimen);
					} else {
						;
					}
			} else if (optId.equals("spec_N_nei")) {
				if (!iipNei.canClose()) {
					Dialogs.showMessage(CManager.getAnyFrame(this), iipNei.getErrorMessage(), "Illegal number of neighbours!");
					return;
				}
				int nNei = iipNei.getEnteredValue();
				for (int i = 0; i < nSpec; i++) {
					ClusterSpecimenInfo spIn = clIn.getClusterSpecimenInfo(i);
					if (spIn.nSimilarOrig >= nNei) {
						specimens.addElement(spIn.specimen);
					}
				}
				if (specimens.size() < 1) {
					Dialogs.showMessage(CManager.getAnyFrame(this), "No prototypes having at least " + nNei + " neighbours have been found!", "No suitable prototypes!");
					return;
				}
				if (specimens.size() < 2) {
					Dialogs.showMessage(CManager.getAnyFrame(this), "Only 1 prototype having at least " + nNei + " neighbours has been found!", "Only 1 suitable prototype!");
					return;
				}
			} else if (optId.equals("sel_obj")) {
				for (DClusterObject clusteredObject : clusteredObjects)
					if (clusteredObject.clusterIdx + 1 == clIn.clusterN && StringUtil.isStringInVectorIgnoreCase(clusteredObject.id, selObj)) {
						specimens.addElement(clusteredObject);
					}
			}
			divideClusterUsingSpecimens(clIn, specimens);
		}
	}

	protected void refineSubcluster() {
		if (selClusterIdx < 0)
			return;
		int selSpecIdxs[] = specList.getSelectedIndexes();
		if (selSpecIdxs == null || selSpecIdxs.length != 1) {
			Dialogs.showMessage(CManager.getAnyFrame(this), "Select the subcluster to refine!", "Select subcluster");
			return;
		}
		if (!showSubclusterCB.getState()) {
			whatToShowCBG.setSelectedCheckbox(showSubclusterCB);
			applyFilters();
		}
		int subIdx = selSpecIdxs[0];
		SingleClusterInfo clIn = clustersInfo.getSingleClusterInfo(selClusterIdx);
		ClusterSpecimenInfo spec = clIn.getClusterSpecimenInfo(subIdx);

		ActionDescr aDescr = new ActionDescr();
		aDescr.aName = "Refine subcluster " + (subIdx + 1) + " of cluster " + clIn.clusterLabel;
		aDescr.addParamValue("Prototype id", spec.specimen.id);
		aDescr.addParamValue("Original subcluster size", new Integer(spec.nSimilarOrig));

		ArrayList<DClusterObject> data = new ArrayList<DClusterObject>(spec.nSimilarOrig + 10);
		for (DClusterObject clusteredObject : clusteredObjects)
			if (clusteredObject.clusterIdx + 1 == clIn.clusterN && clusteredObject.subIdx == subIdx) {
				data.add(clusteredObject);
			}
		if (data.size() < 2) {
			Dialogs.showMessage(CManager.getAnyFrame(this), "The subcluster must contain at least 2 objects!", "Select subcluster");
			return;
		}
		SelectDialog selDia = new SelectDialog(CManager.getAnyFrame(this), "Refine subcluster", "Refine subcluster " + (subIdx + 1) + " of cluster " + clIn.clusterN + " by dividing into 2 or more subclusters");
		Highlighter hl = core.getHighlighterForSet(origLayer.getEntitySetIdentifier());
		Vector selObj = hl.getSelectedObjects();
		int nSelObj = (selObj == null) ? 0 : selObj.size();
		if (nSelObj > 1) {
			Vector selected = selObj;
			selObj = new Vector(selObj.size(), 1);
			for (int i = 0; i < data.size(); i++)
				if (StringUtil.isStringInVectorIgnoreCase(data.get(i).id, selected)) {
					selObj.addElement(data.get(i).id);
				}
			if (selObj.size() > 1 && selObj.size() < nSelObj) {
				hl.replaceSelectedObjects(this, selObj);
			}
			nSelObj = selObj.size();
		}
		if (nSelObj > 1) {
			selDia.addOption("use " + nSelObj + " currently selected objects as initial seeds", "sel_obj", true);
			selDia.addSeparator();
		}
		selDia.addOption("divide into K arbitrary subclusters", "arbitrary", nSelObj < 2);
		InputIntPanel iipK = new InputIntPanel("K =", 2, 2, data.size(), null);
		selDia.addComponent(iipK);
		if (nSelObj < 2) {
			selDia.addSeparator();
			selDia.addLabel("Note: you can also select (e.g. by clicking) 2 or more arbitrary objects");
			selDia.addLabel("of the subcluster to be used as the seeds of the new subclusters.");
		}
		selDia.show();
		if (selDia.wasCancelled())
			return;
		String optId = selDia.getSelectedOptionId();
		if (optId == null)
			return;
		if (optId.equals("arbitrary") && !iipK.canClose()) {
			Dialogs.showMessage(CManager.getAnyFrame(this), iipK.getErrorMessage(), "Illegal number of clusters!");
			return;
		}
		KMedoids<DClusterObject> km = new KMedoids<DClusterObject>(clustersInfo.distanceMeter);
		HashMap<DClusterObject, ArrayList<DClusterObject>> result = null;
		if (optId.equals("arbitrary")) {
			aDescr.addParamValue("Division mode", "arbitrary");
			result = km.doClustering(data, iipK.getEnteredValue());
		} else {
			aDescr.addParamValue("Division mode", "using selected objects");
			Vector<DClusterObject> specimens = new Vector<DClusterObject>(nSelObj, 1);
			for (int i = 0; i < data.size(); i++)
				if (StringUtil.isStringInVectorIgnoreCase(data.get(i).id, selObj)) {
					specimens.addElement(data.get(i));
					int n = specimens.size();
					aDescr.addParamValue("Selected object " + n, specimens.elementAt(n - 1).id);
				}
			result = km.doClustering(data, specimens);
		}
		if (result == null || result.size() < 2) {
			Dialogs.showMessage(CManager.getAnyFrame(this), "K-Medoids algorithm has not produced any subclusters!", "Refinement failed!");
			return;
		}
		origLayer.setLayerDrawn(true);
		spLayer.setLayerDrawn(false);
		workMapView.getLayerManager().activateLayer(origLayer.getContainerIdentifier());
		Frame win = CManager.getAnyFrame((Component) workMapView);
		if (win != null) {
			win.toFront();
		}
		CusterRefinementViewer clRefV = new CusterRefinementViewer(result, km, origLayer, core);
		OKDialog dia = new OKDialog(CManager.getAnyFrame(this), "Refinement of a subcluster", true);
		dia.addContent(clRefV);
		dia.show();
		clRefV.destroy();
		spLayer.setLayerDrawn(true);
		if (dia.wasCancelled())
			return;
		int i0 = spTable.getAttrIndex("_cluster_N_"), i1 = spTable.getAttrIndex("_specimen_N_"), i2 = spTable.getAttrIndex("_distance_thr_"), i3 = spTable.getAttrIndex("_N_neighbours_"), i4 = spTable.getAttrIndex("_min_dist_neighb_");
		DataRecord rec = spTable.getDataRecord(spTable.indexOf(spec.specimen.id));
		if (rec != null) {
			rec.setAttrValue(null, i0);
			rec.setAttrValue(null, i1);
			rec.setAttrValue(null, i2);
			rec.setAttrValue(null, i3);
			rec.setAttrValue(null, i4);
		}

		Vector<DClusterObject> centroids = new Vector(result.keySet().size(), 10);
		Vector<ArrayList<DClusterObject>> clusters = new Vector<ArrayList<DClusterObject>>(centroids.capacity(), 10);
		int maxSize = 0, maxIdx = -1;
		for (DClusterObject centre : result.keySet()) {
			if (centre == null) {
				continue;
			}
			centroids.addElement(centre);
			ArrayList<DClusterObject> cluster = result.get(centre);
			clusters.addElement(cluster);
			int size = cluster.size();
			if (size > maxSize) {
				maxSize = size;
				maxIdx = clusters.size() - 1;
			}
		}
		DClusterObject dataCopy[] = new DClusterObject[data.size()];
		for (int i = 0; i < data.size(); i++) {
			dataCopy[i] = (DClusterObject) data.get(i).clone();
		}

		SingleClusterInfo clInCopy = (SingleClusterInfo) clIn.clone();
		IntArray specToAdd = new IntArray(centroids.capacity(), 10);
		ArrayList<DClusterObject> cluster = clusters.elementAt(maxIdx);
		DClusterObject centre = centroids.elementAt(maxIdx);
		ClusterSpecimenInfo specNew = getClusterSpecimenInfo(centre, cluster);
		clInCopy.specimens.setElementAt(specNew, subIdx);
		if (spLayer.findObjectById(centre.id) == null) {
			specToAdd.addElement(subIdx);
		} else {
			rec = spTable.getDataRecord(spTable.indexOf(centre.id));
			if (rec != null) {
				rec.setAttrValue(clInCopy.clusterLabel, i0);
				rec.setNumericAttrValue(subIdx + 1, String.valueOf(subIdx + 1), i1);
				rec.setNumericAttrValue(specNew.distanceThr, String.valueOf(specNew.distanceThr), i2);
				rec.setNumericAttrValue(specNew.nSimilarOrig, String.valueOf(specNew.nSimilarOrig), i3);
				rec.setNumericAttrValue(specNew.meanDistOrig, String.valueOf(specNew.meanDistOrig), i4);
			}
		}
		for (int j = 0; j < cluster.size(); j++) {
			int objIdx = data.indexOf(cluster.get(j));
			if (objIdx >= 0) {
				dataCopy[objIdx].subIdx = subIdx;
				dataCopy[objIdx].specimenId = centre.id;
				dataCopy[objIdx].isSpecimen = dataCopy[objIdx].id.equals(centre.id);
			}
		}

		int specToSelect[] = new int[clusters.size()];
		specToSelect[0] = subIdx;
		int k = 1;
		for (int i = 0; i < clusters.size(); i++)
			if (i != maxIdx) {
				cluster = clusters.elementAt(i);
				centre = centroids.elementAt(i);
				specNew = getClusterSpecimenInfo(centre, cluster);
				clInCopy.addSpecimen(specNew);
				int spIdx = clInCopy.getSpecimensCount() - 1;
				specToSelect[k++] = spIdx;
				for (int j = 0; j < cluster.size(); j++) {
					int objIdx = data.indexOf(cluster.get(j));
					if (objIdx >= 0) {
						dataCopy[objIdx].subIdx = spIdx;
						dataCopy[objIdx].specimenId = centre.id;
						dataCopy[objIdx].isSpecimen = dataCopy[objIdx].id.equals(centre.id);
					}
				}
				if (spLayer.findObjectById(centre.id) == null) {
					specToAdd.addElement(spIdx);
				} else {
					rec = spTable.getDataRecord(spTable.indexOf(centre.id));
					if (rec != null) {
						rec.setAttrValue(clInCopy.clusterLabel, i0);
						rec.setNumericAttrValue(spIdx + 1, String.valueOf(spIdx + 1), i1);
						rec.setNumericAttrValue(specNew.distanceThr, String.valueOf(specNew.distanceThr), i2);
						rec.setNumericAttrValue(specNew.nSimilarOrig, String.valueOf(specNew.nSimilarOrig), i3);
						rec.setNumericAttrValue(specNew.meanDistOrig, String.valueOf(specNew.meanDistOrig), i4);
					}
				}
			}

		ClustersInfo clInfo = new ClustersInfo();
		clInfo.table = clustersInfo.table;
		clInfo.clustersColN = clustersInfo.clustersColN;
		clInfo.objContainer = clustersInfo.objContainer;
		clInfo.distanceMeter = clustersInfo.distanceMeter;
		clInfo.init(Math.max(clustersInfo.getClustersCount(), 10), 10);
		for (int i = 0; i < clustersInfo.getClustersCount(); i++) {
			SingleClusterInfo scl = clustersInfo.getSingleClusterInfo(i);
			if (scl.clusterN != clIn.clusterN) {
				clInfo.addSingleClusterInfo((SingleClusterInfo) clustersInfo.getSingleClusterInfo(i).clone());
			} else {
				clInfo.addSingleClusterInfo(clInCopy);
			}
		}

		DClusterObject clObj[] = new DClusterObject[clusteredObjects.length];
		for (int i = 0; i < clusteredObjects.length; i++)
			if (clusteredObjects[i].clusterIdx + 1 == clIn.clusterN && clusteredObjects[i].subIdx == subIdx) {
				int idx = data.indexOf(clusteredObjects[i]);
				if (idx >= 0) {
					clObj[i] = dataCopy[idx];
				} else {
					clObj[i] = clusteredObjects[i];
				}
			} else {
				clObj[i] = clusteredObjects[i];
			}

		ClassifierBuildState cbState = new ClassifierBuildState();
		cbState.clusteredObjects = clusteredObjects;
		cbState.clustersInfo = clustersInfo;
		cbState.nObjInSpecimensLayer = spLayer.getObjectCount();
		cbState.nRecInSpecimensTable = spTable.getDataItemCount();
		if (states == null) {
			states = new Vector<ClassifierBuildState>(20, 20);
		}
		states.addElement(cbState);

		spLayer.setLayerDrawn(true);
		if (specToAdd.size() > 0) {
			aDescr.addParamValue("N subclusters added", new Integer(specToAdd.size()));
			for (int i = 0; i < specToAdd.size(); i++) {
				subIdx = specToAdd.elementAt(i);
				spec = clInCopy.getClusterSpecimenInfo(subIdx);
				DGeoObject gobj = (DGeoObject) getDGeoObject(spec.specimen).makeCopy();
				rec = new DataRecord(gobj.getIdentifier(), gobj.getLabel());
				spTable.addDataRecord(rec);
				rec.setAttrValue(clInCopy.clusterLabel, i0);
				rec.setNumericAttrValue(subIdx + 1, String.valueOf(subIdx + 1), i1);
				rec.setNumericAttrValue(spec.distanceThr, String.valueOf(spec.distanceThr), i2);
				rec.setNumericAttrValue(spec.nSimilarOrig, String.valueOf(spec.nSimilarOrig), i3);
				rec.setNumericAttrValue(spec.meanDistOrig, String.valueOf(spec.meanDistOrig), i4);
				gobj.setThematicData(rec);
				spLayer.addGeoObject(gobj);
				aDescr.addParamValue("Subcluster " + (i + 1) + " centroid id", spec.specimen.id);
				aDescr.addParamValue("Subcluster " + (i + 1) + " size", new Integer(spec.nSimilarOrig));
			}
			spLayer.notifyPropertyChange("ObjectSet", null, null);
			spTable.notifyPropertyChange("data_added", null, null);
		} else {
			Vector v = new Vector(5, 1);
			v.addElement(spTable.getAttributeId(i0));
			v.addElement(spTable.getAttributeId(i1));
			v.addElement(spTable.getAttributeId(i2));
			v.addElement(spTable.getAttributeId(i3));
			v.addElement(spTable.getAttributeId(i4));
			spTable.notifyPropertyChange("values", null, v);
		}

		clearTestResults();
		clustersInfo = clInfo;
		update(clObj);
		undoB.setEnabled(true);
		for (int i = 0; i < specList.getItemCount(); i++) {
			specList.deselect(i);
		}
		for (int element : specToSelect) {
			specList.select(element);
		}
		fillSpecDetailsPanel();
		applyFilters();

		aDescr.endTime = System.currentTimeMillis();
		core.logAction(aDescr);
		composeDTstr();
/*
    sLFlastCommand=sLFdt_fmt+
        " Refine subcluster, active="+clustersInfo.getSingleClusterInfo(selClusterIdx).clusterLabel+
        ", subcluster="+(1+subIdx);
    core.logSimpleAction(sLFlastCommand);
*/
		//vLFIdxBuffer.addElement("<P>"+sLFlastCommand);
		//printLogFileIndex();
	}

	protected void mergeSubclusters() {
		if (selClusterIdx < 0)
			return;
		SingleClusterInfo clIn = clustersInfo.getSingleClusterInfo(selClusterIdx);
		if (clIn.getSpecimensCount() < 2) {
			Dialogs.showMessage(CManager.getAnyFrame(this), "Cluster " + clIn.clusterLabel + " has no subclusters!", "No subclusters!");
			return;
		}
		int selSpecIdxs[] = specList.getSelectedIndexes();
		if (selSpecIdxs == null || selSpecIdxs.length < 2) {
			Dialogs.showMessage(CManager.getAnyFrame(this), "Select at least 2 subclusters to merge!", "Select subclusters");
			return;
		}
		mergeSubclusters(clIn, selSpecIdxs);
	}

	protected void mergeSubclusters(SingleClusterInfo clIn, int subIdxs[]) {
		ActionDescr aDescr = new ActionDescr();
		aDescr.aName = "Merge " + subIdxs.length + " subclusters of cluster " + clIn.clusterLabel;
		aDescr.startTime = System.currentTimeMillis();
		for (int i = 0; i < subIdxs.length; i++) {
			aDescr.addParamValue("Subcluster " + (i + 1) + " index", new Integer(subIdxs[i]));
			ClusterSpecimenInfo spec = clIn.getClusterSpecimenInfo(subIdxs[i]);
			aDescr.addParamValue("Subcluster " + (i + 1) + " centroid id", spec.specimen.id);
		}
		ArrayList<DClusterObject> data = new ArrayList<DClusterObject>(clIn.origSize);
		for (DClusterObject clusteredObject : clusteredObjects)
			if (clusteredObject.clusterIdx + 1 == clIn.clusterN) {
				boolean found = false;
				for (int j = 0; j < subIdxs.length && !found; j++)
					if (clusteredObject.subIdx == subIdxs[j]) {
						data.add(clusteredObject);
						found = true;
					}
			}
		KMedoids<DClusterObject> km = new KMedoids<DClusterObject>(clustersInfo.distanceMeter);
		DClusterObject centre = km.getCentroid(data);
		aDescr.addParamValue("New centroid id", centre.id);
		aDescr.addParamValue("New subcluster size", new Integer(data.size()));

		int i0 = spTable.getAttrIndex("_cluster_N_"), i1 = spTable.getAttrIndex("_specimen_N_"), i2 = spTable.getAttrIndex("_distance_thr_"), i3 = spTable.getAttrIndex("_N_neighbours_"), i4 = spTable.getAttrIndex("_min_dist_neighb_");
		for (int subIdx : subIdxs) {
			ClusterSpecimenInfo spec = clIn.getClusterSpecimenInfo(subIdx);
			DataRecord rec = spTable.getDataRecord(spTable.indexOf(spec.specimen.id));
			if (rec != null) {
				rec.setAttrValue(null, i0);
				rec.setAttrValue(null, i1);
				rec.setAttrValue(null, i2);
				rec.setAttrValue(null, i3);
				rec.setAttrValue(null, i4);
			}
		}

		SingleClusterInfo clInCopy = new SingleClusterInfo();
		clInCopy.clusterN = clIn.clusterN;
		clInCopy.clusterLabel = clIn.clusterLabel;
		clInCopy.origSize = clIn.origSize;
		int newSpNs[] = new int[clIn.getSpecimensCount()];
		for (int i = 0; i < clIn.getSpecimensCount(); i++)
			if (!specList.isIndexSelected(i)) {
				clInCopy.addSpecimen(clIn.getClusterSpecimenInfo(i));
				newSpNs[i] = clInCopy.getSpecimensCount() - 1;
			} else {
				newSpNs[i] = -1;
			}
		ClusterSpecimenInfo spec = getClusterSpecimenInfo(centre, data);
		clInCopy.addSpecimen(spec);
		int subIdx = clInCopy.getSpecimensCount() - 1;
		for (int i = 0; i < newSpNs.length; i++)
			if (newSpNs[i] < 0) {
				newSpNs[i] = subIdx;
			}
		DataRecord rec = null;
		boolean added = false;
		if (spLayer.findObjectById(centre.id) == null) {
			DGeoObject gobj = (DGeoObject) getDGeoObject(spec.specimen).makeCopy();
			rec = new DataRecord(gobj.getIdentifier(), gobj.getLabel());
			spTable.addDataRecord(rec);
			gobj.setThematicData(rec);
			spLayer.addGeoObject(gobj);
			added = true;
		} else {
			rec = spTable.getDataRecord(spTable.indexOf(centre.id));
		}
		if (rec != null) {
			rec.setAttrValue(clInCopy.clusterLabel, i0);
			rec.setNumericAttrValue(subIdx + 1, String.valueOf(subIdx + 1), i1);
			rec.setNumericAttrValue(spec.distanceThr, String.valueOf(spec.distanceThr), i2);
			rec.setNumericAttrValue(spec.nSimilarOrig, String.valueOf(spec.nSimilarOrig), i3);
			rec.setNumericAttrValue(spec.meanDistOrig, String.valueOf(spec.meanDistOrig), i4);
		}
		DClusterObject clObj[] = new DClusterObject[clusteredObjects.length];
		for (int i = 0; i < clusteredObjects.length; i++)
			if (clusteredObjects[i].clusterIdx + 1 == clIn.clusterN) {
				clObj[i] = (DClusterObject) clusteredObjects[i].clone();
				clObj[i].subIdx = newSpNs[clObj[i].subIdx];
				if (clObj[i].subIdx == subIdx) {
					clObj[i].isSpecimen = clObj[i].id.equals(centre.id);
					clObj[i].specimenId = centre.id;
				}
			} else {
				clObj[i] = clusteredObjects[i];
			}

		ClustersInfo clInfo = new ClustersInfo();
		clInfo.table = clustersInfo.table;
		clInfo.clustersColN = clustersInfo.clustersColN;
		clInfo.objContainer = clustersInfo.objContainer;
		clInfo.distanceMeter = clustersInfo.distanceMeter;
		clInfo.init(Math.max(clustersInfo.getClustersCount(), 10), 10);
		for (int i = 0; i < clustersInfo.getClustersCount(); i++) {
			SingleClusterInfo scl = clustersInfo.getSingleClusterInfo(i);
			if (scl.clusterN != clIn.clusterN) {
				clInfo.addSingleClusterInfo((SingleClusterInfo) clustersInfo.getSingleClusterInfo(i).clone());
			} else {
				clInfo.addSingleClusterInfo(clInCopy);
			}
		}

		ClassifierBuildState cbState = new ClassifierBuildState();
		cbState.clusteredObjects = clusteredObjects;
		cbState.clustersInfo = clustersInfo;
		cbState.nObjInSpecimensLayer = spLayer.getObjectCount();
		cbState.nRecInSpecimensTable = spTable.getDataItemCount();
		if (states == null) {
			states = new Vector<ClassifierBuildState>(20, 20);
		}
		states.addElement(cbState);

		spLayer.setLayerDrawn(true);

		if (added) {
			spLayer.notifyPropertyChange("ObjectSet", null, null);
			spTable.notifyPropertyChange("data_added", null, null);
		} else {
			Vector v = new Vector(5, 1);
			v.addElement(spTable.getAttributeId(i0));
			v.addElement(spTable.getAttributeId(i1));
			v.addElement(spTable.getAttributeId(i2));
			v.addElement(spTable.getAttributeId(i3));
			v.addElement(spTable.getAttributeId(i4));
			spTable.notifyPropertyChange("values", null, v);
		}

		clearTestResults();
		clustersInfo = clInfo;
		update(clObj);
		undoB.setEnabled(true);
		for (int i = 0; i < specList.getItemCount(); i++) {
			specList.deselect(i);
		}
		specList.select(subIdx);
		fillSpecDetailsPanel();
		applyFilters();

		aDescr.endTime = System.currentTimeMillis();
		core.logAction(aDescr);
		composeDTstr();
/*
    sLFlastCommand=sLFdt_fmt+" Merge subclusters, active="+clustersInfo.getSingleClusterInfo(selClusterIdx).clusterLabel;
    core.logSimpleAction(sLFlastCommand);
*/
		//vLFIdxBuffer.addElement("<P>"+sLFlastCommand);
		//printLogFileIndex();
	}

	protected void excludeSubclusters() {
		if (selClusterIdx < 0)
			return;
		SingleClusterInfo clIn = clustersInfo.getSingleClusterInfo(selClusterIdx);
		if (clIn.getSpecimensCount() < 2) {
			Dialogs.showMessage(CManager.getAnyFrame(this), "Cluster " + clIn.clusterLabel + " has no subclusters!", "No subclusters!");
			return;
		}
		int selSpecIdxs[] = specList.getSelectedIndexes();
		if (selSpecIdxs == null || selSpecIdxs.length < 1) {
			Dialogs.showMessage(CManager.getAnyFrame(this), "Select at least 1 subcluster to exclude!", "Select subcluster");
			return;
		}
		if (selSpecIdxs.length >= clIn.getSpecimensCount()) {
			Dialogs.showMessage(CManager.getAnyFrame(this), "Select less than " + clIn.getSpecimensCount() + " subclusters!", "Select subcluster");
			return;
		}
		SelectDialog selDia = new SelectDialog(CManager.getAnyFrame(this), "Exclude subcluster(s)", "What to do with the selected " + selSpecIdxs.length + " " + ((selSpecIdxs.length > 1) ? "subclusters" : "subcluster") + "?");
		if (selSpecIdxs.length > 1) {
			selDia.addOption("make " + selSpecIdxs.length + " new clusters", "make_clusters", true);
			selDia.addOption("make 1 new cluster by merging the subclusters", "merged_cluster", false);
		} else {
			selDia.addOption("make a new cluster", "make_clusters", true);
		}
		selDia.addOption("treat the members as \"noise\" (unclassified)", "noise", false);
		selDia.show();
		if (selDia.wasCancelled())
			return;
		boolean makeClusters = selDia.getSelectedOptionId().equals("make_clusters");
		boolean merge = selDia.getSelectedOptionId().equals("merged_cluster");
		ActionDescr aDescr = new ActionDescr();
		aDescr.aName = "Exclude " + (selSpecIdxs.length) + " " + ((selSpecIdxs.length == 1) ? "subcluster" : "subclusters") + " from cluster " + clIn.clusterLabel;
		aDescr.addParamValue("Make new cluster(s)", new Boolean(makeClusters));
		aDescr.addParamValue("Merge subclusters in one new cluster", new Boolean(merge));
		aDescr.startTime = System.currentTimeMillis();

		SingleClusterInfo clInCopy = new SingleClusterInfo();
		clInCopy.clusterN = clIn.clusterN;
		clInCopy.clusterLabel = clIn.clusterLabel;
		clInCopy.origSize = 0;
		int newSpNs[] = new int[clIn.getSpecimensCount()], exclSpNs[] = new int[clIn.getSpecimensCount()];
		Vector<ClusterSpecimenInfo> specToExclude = new Vector<ClusterSpecimenInfo>(selSpecIdxs.length, 10);
		for (int i = 0; i < clIn.getSpecimensCount(); i++)
			if (!specList.isIndexSelected(i)) {
				ClusterSpecimenInfo spec = clIn.getClusterSpecimenInfo(i);
				clInCopy.addSpecimen(spec);
				clInCopy.origSize += spec.nSimilarOrig;
				newSpNs[i] = clInCopy.getSpecimensCount() - 1;
				exclSpNs[i] = -1;
			} else {
				newSpNs[i] = -1;
				ClusterSpecimenInfo spec = clIn.getClusterSpecimenInfo(i);
				specToExclude.addElement(spec);
				exclSpNs[i] = specToExclude.size() - 1;
				aDescr.addParamValue("Excluded subcluster " + (exclSpNs[i] + 1) + " index", new Integer(i));
				aDescr.addParamValue("Excluded subcluster " + (exclSpNs[i] + 1) + " centroid", spec.specimen.id);
			}
		ClustersInfo clInfo = new ClustersInfo();
		clInfo.table = clustersInfo.table;
		clInfo.clustersColN = clustersInfo.clustersColN;
		clInfo.objContainer = clustersInfo.objContainer;
		clInfo.distanceMeter = clustersInfo.distanceMeter;
		clInfo.init(clustersInfo.getClustersCount() + specToExclude.size(), 10);
		int maxClusterN = clIn.clusterN;
		for (int i = 0; i < clustersInfo.getClustersCount(); i++) {
			SingleClusterInfo scl = clustersInfo.getSingleClusterInfo(i);
			if (scl.clusterN != clIn.clusterN) {
				clInfo.addSingleClusterInfo((SingleClusterInfo) clustersInfo.getSingleClusterInfo(i).clone());
				if (scl.clusterN > maxClusterN) {
					maxClusterN = scl.clusterN;
				}
			} else {
				clInfo.addSingleClusterInfo(clInCopy);
			}
		}
		int clusterNs[] = null;
		int i0 = spTable.getAttrIndex("_cluster_N_"), i1 = spTable.getAttrIndex("_specimen_N_"), i2 = spTable.getAttrIndex("_distance_thr_"), i3 = spTable.getAttrIndex("_N_neighbours_"), i4 = spTable.getAttrIndex("_min_dist_neighb_");
		if (makeClusters) {
			clusterNs = new int[specToExclude.size()];
			for (int i = 0; i < specToExclude.size(); i++) {
				SingleClusterInfo scl = new SingleClusterInfo();
				scl.clusterN = ++maxClusterN;
				clusterNs[i] = scl.clusterN;
				scl.clusterLabel = String.valueOf(scl.clusterN);
				ClusterSpecimenInfo spec = specToExclude.elementAt(i);
				scl.origSize = spec.nSimilarOrig;
				scl.addSpecimen(spec);
				clInfo.addSingleClusterInfo(scl);
				DataRecord rec = spTable.getDataRecord(spTable.indexOf(spec.specimen.id));
				if (rec != null) {
					rec.setAttrValue(scl.clusterLabel, i0);
					rec.setNumericAttrValue(1, "1", i1);
					rec.setNumericAttrValue(spec.distanceThr, String.valueOf(spec.distanceThr), i2);
					rec.setNumericAttrValue(spec.nSimilarOrig, String.valueOf(spec.nSimilarOrig), i3);
					rec.setNumericAttrValue(spec.meanDistOrig, String.valueOf(spec.meanDistOrig), i4);
				}
				aDescr.addParamValue("Added cluster " + (i + 1), scl.clusterLabel);
			}
		} else if (merge) {
			clusterNs = new int[1];
			SingleClusterInfo scl = new SingleClusterInfo();
			scl.clusterN = maxClusterN + 1;
			clusterNs[0] = scl.clusterN;
			scl.clusterLabel = String.valueOf(scl.clusterN);
			scl.origSize = 0;
			clInfo.addSingleClusterInfo(scl);
			aDescr.addParamValue("Added cluster", scl.clusterLabel);
			for (int i = 0; i < specToExclude.size(); i++) {
				ClusterSpecimenInfo spec = specToExclude.elementAt(i);
				scl.origSize += spec.nSimilarOrig;
				scl.addSpecimen(spec);
				DataRecord rec = spTable.getDataRecord(spTable.indexOf(spec.specimen.id));
				if (rec != null) {
					rec.setAttrValue(scl.clusterLabel, i0);
					rec.setNumericAttrValue(i + 1, String.valueOf(i + 1), i1);
					rec.setNumericAttrValue(spec.distanceThr, String.valueOf(spec.distanceThr), i2);
					rec.setNumericAttrValue(spec.nSimilarOrig, String.valueOf(spec.nSimilarOrig), i3);
					rec.setNumericAttrValue(spec.meanDistOrig, String.valueOf(spec.meanDistOrig), i4);
				}
			}
		} else {
			for (int i = 0; i < specToExclude.size(); i++) {
				ClusterSpecimenInfo spec = specToExclude.elementAt(i);
				DataRecord rec = spTable.getDataRecord(spTable.indexOf(spec.specimen.id));
				if (rec != null) {
					rec.setAttrValue(null, i0);
					rec.setAttrValue(null, i1);
					rec.setAttrValue(null, i2);
					rec.setAttrValue(null, i3);
					rec.setAttrValue(null, i4);
				}
			}
		}
		DClusterObject clObj[] = new DClusterObject[clusteredObjects.length];
		for (int i = 0; i < clusteredObjects.length; i++)
			if (clusteredObjects[i].clusterIdx + 1 == clIn.clusterN) {
				clObj[i] = (DClusterObject) clusteredObjects[i].clone();
				if (newSpNs[clusteredObjects[i].subIdx] >= 0) {
					clObj[i].subIdx = newSpNs[clusteredObjects[i].subIdx];
				} else if (clusterNs != null) {
					int exclIdx = exclSpNs[clusteredObjects[i].subIdx];
					clObj[i].clusterIdx = (clusterNs.length > 1) ? clusterNs[exclIdx] - 1 : clusterNs[0] - 1;
					clObj[i].subIdx = (clusterNs.length > 1) ? 0 : exclIdx;
				} else {
					clObj[i].clusterIdx = -1;
					clObj[i].subIdx = -1;
					clObj[i].isSpecimen = false;
					clObj[i].specimenId = null;
				}
			} else {
				clObj[i] = clusteredObjects[i];
			}

		ClassifierBuildState cbState = new ClassifierBuildState();
		cbState.clusteredObjects = clusteredObjects;
		cbState.clustersInfo = clustersInfo;
		cbState.nObjInSpecimensLayer = spLayer.getObjectCount();
		cbState.nRecInSpecimensTable = spTable.getDataItemCount();
		if (states == null) {
			states = new Vector<ClassifierBuildState>(20, 20);
		}
		states.addElement(cbState);

		spLayer.setLayerDrawn(true);
		Vector v = new Vector(5, 1);
		v.addElement(spTable.getAttributeId(i0));
		v.addElement(spTable.getAttributeId(i1));
		v.addElement(spTable.getAttributeId(i2));
		v.addElement(spTable.getAttributeId(i3));
		v.addElement(spTable.getAttributeId(i4));
		spTable.notifyPropertyChange("values", null, v);

		clearTestResults();
		clustersInfo = clInfo;
		update(clObj);
		undoB.setEnabled(true);

		aDescr.endTime = System.currentTimeMillis();
		core.logAction(aDescr);
		composeDTstr();
/*
    sLFlastCommand=sLFdt_fmt+" Exclude subclusters, active="+clustersInfo.getSingleClusterInfo(selClusterIdx).clusterLabel;
    core.logSimpleAction(sLFlastCommand);
*/
		//vLFIdxBuffer.addElement("<P>"+sLFlastCommand);
		//printLogFileIndex();
	}

	protected void removeSpecimens() {
		if (selClusterIdx < 0)
			return;
		SingleClusterInfo clIn = clustersInfo.getSingleClusterInfo(selClusterIdx);
		if (clIn.getSpecimensCount() < 2) {
			Dialogs.showMessage(CManager.getAnyFrame(this), "Cluster " + clIn.clusterLabel + " has a single prototype, which cannot be removed!", "Single prototype!");
			return;
		}
		int selSpecIdxs[] = specList.getSelectedIndexes();
		if (selSpecIdxs == null || selSpecIdxs.length < 1) {
			Dialogs.showMessage(CManager.getAnyFrame(this), "Select at least 1 prototype to remove!", "Select prototype");
			return;
		}
		if (selSpecIdxs.length >= clIn.getSpecimensCount()) {
			Dialogs.showMessage(CManager.getAnyFrame(this), "Select less than " + clIn.getSpecimensCount() + " prototypes!", "Select prototype");
			return;
		}
		if (selSpecIdxs.length == clIn.getSpecimensCount() - 1) {
			boolean merge = Dialogs.askYesOrNo(CManager.getAnyFrame(this), "Removing all but one " + "prototypes is equivalent to merging the subclusters. Do you wish to proceed?", "Merge subclusters?");
			if (!merge)
				return;
			int subIdxs[] = new int[clIn.getSpecimensCount()];
			for (int i = 0; i < subIdxs.length; i++) {
				subIdxs[i] = i;
			}
			mergeSubclusters(clIn, subIdxs);
			return;
		}
		ArrayList<DClusterObject> seeds = new ArrayList<DClusterObject>(clIn.getSpecimensCount() - selSpecIdxs.length);
		ActionDescr aDescr = new ActionDescr();
		aDescr.startTime = System.currentTimeMillis();
		for (int i = 0; i < clIn.getSpecimensCount(); i++)
			if (!specList.isIndexSelected(i)) {
				ClusterSpecimenInfo spec = clIn.getClusterSpecimenInfo(i);
				seeds.add(spec.specimen);
				aDescr.addParamValue("Prototype " + (i + 1) + " index", new Integer(i));
				aDescr.addParamValue("Prototype " + (i + 1) + " id", spec.specimen.id);
			}
		aDescr.aName = "Remove " + seeds.size() + " prototypes in cluster " + clIn.clusterLabel + " (dissolve subclusters)";

		ArrayList<DClusterObject> data = new ArrayList<DClusterObject>(clIn.origSize);
		for (DClusterObject clusteredObject : clusteredObjects)
			if (clusteredObject.clusterIdx + 1 == clIn.clusterN) {
				data.add(clusteredObject);
			}
		KMedoids<DClusterObject> km = new KMedoids<DClusterObject>(clustersInfo.distanceMeter);
		HashMap<DClusterObject, ArrayList<DClusterObject>> result = km.doClustering(data, seeds);
		if (result == null || result.size() < 2) {
			Dialogs.showMessage(CManager.getAnyFrame(this), "K-Medoids algorithm has not produced any subclusters!", "Operation failed!");
			return;
		}
		origLayer.setLayerDrawn(true);
		spLayer.setLayerDrawn(false);
		workMapView.getLayerManager().activateLayer(origLayer.getContainerIdentifier());
		Frame win = CManager.getAnyFrame((Component) workMapView);
		if (win != null) {
			win.toFront();
		}
		CusterRefinementViewer clRefV = new CusterRefinementViewer(result, km, origLayer, core);
		OKDialog dia = new OKDialog(CManager.getAnyFrame(this), "Re-division of a cluster", true);
		dia.addContent(clRefV);
		dia.show();
		clRefV.destroy();
		spLayer.setLayerDrawn(true);
		if (dia.wasCancelled())
			return;
		int i0 = spTable.getAttrIndex("_cluster_N_"), i1 = spTable.getAttrIndex("_specimen_N_"), i2 = spTable.getAttrIndex("_distance_thr_"), i3 = spTable.getAttrIndex("_N_neighbours_"), i4 = spTable.getAttrIndex("_min_dist_neighb_");
		for (int i = 0; i < clIn.getSpecimensCount(); i++) {
			DataRecord rec = spTable.getDataRecord(spTable.indexOf(clIn.getClusterSpecimenInfo(i).specimen.id));
			if (rec != null) {
				rec.setAttrValue(null, i0);
				rec.setAttrValue(null, i1);
				rec.setAttrValue(null, i2);
				rec.setAttrValue(null, i3);
				rec.setAttrValue(null, i4);
			}
		}

		DClusterObject dataCopy[] = new DClusterObject[data.size()];
		for (int i = 0; i < data.size(); i++) {
			dataCopy[i] = (DClusterObject) data.get(i).clone();
		}

		SingleClusterInfo clInCopy = new SingleClusterInfo();
		clInCopy.clusterN = clIn.clusterN;
		clInCopy.clusterLabel = clIn.clusterLabel;
		clInCopy.origSize = clIn.origSize;
		IntArray specToAdd = new IntArray(result.size(), 10);
		for (DClusterObject centre : result.keySet()) {
			if (centre == null) {
				continue;
			}
			ArrayList<DClusterObject> cluster = result.get(centre);
			ClusterSpecimenInfo spec = getClusterSpecimenInfo(centre, cluster);
			clInCopy.addSpecimen(spec);
			int subIdx = clInCopy.getSpecimensCount() - 1;
			aDescr.addParamValue("New prototype " + (subIdx + 1) + " id", spec.specimen.id);
			aDescr.addParamValue("New subcluster " + (subIdx + 1) + " size", new Integer(spec.nSimilarOrig));
			for (int i = 0; i < data.size(); i++)
				if (cluster.contains(data.get(i))) {
					dataCopy[i].subIdx = subIdx;
					dataCopy[i].specimenId = centre.id;
					dataCopy[i].isSpecimen = dataCopy[i].id.equals(centre.id);
				}
			if (spLayer.findObjectById(centre.id) == null) {
				specToAdd.addElement(subIdx);
			} else {
				DataRecord rec = spTable.getDataRecord(spTable.indexOf(centre.id));
				if (rec != null) {
					rec.setAttrValue(clInCopy.clusterLabel, i0);
					rec.setNumericAttrValue(subIdx + 1, String.valueOf(subIdx + 1), i1);
					rec.setNumericAttrValue(spec.distanceThr, String.valueOf(spec.distanceThr), i2);
					rec.setNumericAttrValue(spec.nSimilarOrig, String.valueOf(spec.nSimilarOrig), i3);
					rec.setNumericAttrValue(spec.meanDistOrig, String.valueOf(spec.meanDistOrig), i4);
				}
			}
		}
		ClustersInfo clInfo = new ClustersInfo();
		clInfo.table = clustersInfo.table;
		clInfo.clustersColN = clustersInfo.clustersColN;
		clInfo.objContainer = clustersInfo.objContainer;
		clInfo.distanceMeter = clustersInfo.distanceMeter;
		clInfo.init(Math.max(clustersInfo.getClustersCount(), 10), 10);
		for (int i = 0; i < clustersInfo.getClustersCount(); i++) {
			SingleClusterInfo scl = clustersInfo.getSingleClusterInfo(i);
			if (scl.clusterN != clIn.clusterN) {
				clInfo.addSingleClusterInfo((SingleClusterInfo) clustersInfo.getSingleClusterInfo(i).clone());
			} else {
				clInfo.addSingleClusterInfo(clInCopy);
			}
		}

		DClusterObject clObj[] = new DClusterObject[clusteredObjects.length];
		for (int i = 0; i < clusteredObjects.length; i++)
			if (clusteredObjects[i].clusterIdx + 1 == clIn.clusterN) {
				int idx = data.indexOf(clusteredObjects[i]);
				if (idx >= 0) {
					clObj[i] = dataCopy[idx];
				} else {
					clObj[i] = clusteredObjects[i];
				}
			} else {
				clObj[i] = clusteredObjects[i];
			}

		ClassifierBuildState cbState = new ClassifierBuildState();
		cbState.clusteredObjects = clusteredObjects;
		cbState.clustersInfo = clustersInfo;
		cbState.nObjInSpecimensLayer = spLayer.getObjectCount();
		cbState.nRecInSpecimensTable = spTable.getDataItemCount();
		if (states == null) {
			states = new Vector<ClassifierBuildState>(20, 20);
		}
		states.addElement(cbState);

		spLayer.setLayerDrawn(true);
		if (specToAdd.size() > 0) {
			for (int i = 0; i < specToAdd.size(); i++) {
				int subIdx = specToAdd.elementAt(i);
				ClusterSpecimenInfo spec = clInCopy.getClusterSpecimenInfo(subIdx);
				DGeoObject gobj = (DGeoObject) getDGeoObject(spec.specimen).makeCopy();
				DataRecord rec = new DataRecord(gobj.getIdentifier(), gobj.getLabel());
				spTable.addDataRecord(rec);
				rec.setAttrValue(clInCopy.clusterLabel, i0);
				rec.setNumericAttrValue(subIdx + 1, String.valueOf(subIdx + 1), i1);
				rec.setNumericAttrValue(spec.distanceThr, String.valueOf(spec.distanceThr), i2);
				rec.setNumericAttrValue(spec.nSimilarOrig, String.valueOf(spec.nSimilarOrig), i3);
				rec.setNumericAttrValue(spec.meanDistOrig, String.valueOf(spec.meanDistOrig), i4);
				gobj.setThematicData(rec);
				spLayer.addGeoObject(gobj);
			}
			spLayer.notifyPropertyChange("ObjectSet", null, null);
			spTable.notifyPropertyChange("data_added", null, null);
		} else {
			Vector v = new Vector(5, 1);
			v.addElement(spTable.getAttributeId(i0));
			v.addElement(spTable.getAttributeId(i1));
			v.addElement(spTable.getAttributeId(i2));
			v.addElement(spTable.getAttributeId(i3));
			v.addElement(spTable.getAttributeId(i4));
			spTable.notifyPropertyChange("values", null, v);
		}

		clearTestResults();
		clustersInfo = clInfo;
		update(clObj);
		undoB.setEnabled(true);

		aDescr.endTime = System.currentTimeMillis();
		core.logAction(aDescr);
		composeDTstr();
/*
    sLFlastCommand=sLFdt_fmt+" Remove specimens, active="+clustersInfo.getSingleClusterInfo(selClusterIdx).clusterLabel;
    core.logSimpleAction(sLFlastCommand);
*/
		//vLFIdxBuffer.addElement("<P>"+sLFlastCommand);
		//printLogFileIndex();
	}

	public void restoreState(int stateIdx, boolean updateUI) {
		if (states == null || stateIdx < 0 || stateIdx >= states.size())
			return;
		clearTestResults();
		ClassifierBuildState cbState = states.elementAt(stateIdx);
		if (stateIdx == 0) {
			states.removeAllElements();
		} else {
			while (states.size() > stateIdx) {
				states.removeElementAt(states.size() - 1);
			}
		}
		undoB.setEnabled(states.size() > 0);
		int nObjExtra = spLayer.getObjectCount() - cbState.nObjInSpecimensLayer;
		if (nObjExtra > 0) {
			for (int i = 0; i < nObjExtra; i++) {
				spLayer.removeGeoObject(spLayer.getObjectCount() - 1);
			}
			int nRecExtra = spTable.getDataItemCount() - cbState.nRecInSpecimensTable;
			for (int i = 0; i < nRecExtra; i++) {
				spTable.removeDataItem(spTable.getDataItemCount() - 1);
			}
			spLayer.notifyPropertyChange("ObjectSet", null, null);
			spTable.notifyPropertyChange("data_removed", null, null);
		}
		clustersInfo = cbState.clustersInfo;
		int i0 = spTable.getAttrIndex("_cluster_N_"), i1 = spTable.getAttrIndex("_specimen_N_"), i2 = spTable.getAttrIndex("_distance_thr_"), i3 = spTable.getAttrIndex("_N_neighbours_"), i4 = spTable.getAttrIndex("_min_dist_neighb_");
		for (int i = 0; i < clustersInfo.getClustersCount(); i++) {
			SingleClusterInfo scl = clustersInfo.getSingleClusterInfo(i);
			for (int j = 0; j < scl.getSpecimensCount(); j++) {
				ClusterSpecimenInfo spec = scl.getClusterSpecimenInfo(j);
				DataRecord rec = spTable.getDataRecord(spTable.indexOf(spec.specimen.id));
				if (rec != null) {
					rec.setAttrValue(scl.clusterLabel, i0);
					rec.setNumericAttrValue(j + 1, String.valueOf(j + 1), i1);
					rec.setNumericAttrValue(spec.distanceThr, String.valueOf(spec.distanceThr), i2);
					rec.setNumericAttrValue(spec.nSimilarOrig, String.valueOf(spec.nSimilarOrig), i3);
					rec.setNumericAttrValue(spec.meanDistOrig, String.valueOf(spec.meanDistOrig), i4);
				}
			}
		}
		Vector v = new Vector(5, 1);
		v.addElement(spTable.getAttributeId(i0));
		v.addElement(spTable.getAttributeId(i1));
		v.addElement(spTable.getAttributeId(i2));
		v.addElement(spTable.getAttributeId(i3));
		v.addElement(spTable.getAttributeId(i4));
		spTable.notifyPropertyChange("values", null, v);

		if (updateUI) {
			update(cbState.clusteredObjects);
		}

		composeDTstr();
		sLFlastCommand = sLFdt_fmt + " Restore previous state, active=" + clustersInfo.getSingleClusterInfo(selClusterIdx).clusterLabel;
		core.logSimpleAction(sLFlastCommand);
		//vLFIdxBuffer.addElement("<P>"+sLFlastCommand);
		//printLogFileIndex();
	}

	public void actionPerformed(ActionEvent e) {
		if (selClusterIdx < 0)
			return;
		String cmd = e.getActionCommand();
		if (cmd == null)
			return;
		if (cmd.equals("select_all")) {
			for (int i = 0; i < specList.getItemCount(); i++) {
				specList.select(i);
			}
			fillSpecDetailsPanel();
			applyFilters();
		} else if (cmd.equals("deselect_all")) {
			for (int i = 0; i < specList.getItemCount(); i++) {
				specList.deselect(i);
			}
			fillSpecDetailsPanel();
			applyFilters();
		} else if (cmd.equals("test")) {
			((Button) e.getSource()).setEnabled(false);
			String str = clusterCh.getSelectedItem();
			if (!clustersInspected.contains(str)) {
				clustersInspected.addElement(clusterCh.getSelectedItem());
				int idx = clusterCh.getSelectedIndex();
				clusterLabels.elementAt(idx).setFont(clusterLabels.elementAt(idx).getFont().deriveFont(Font.BOLD));
			}
			testClusters();
			((Button) e.getSource()).setEnabled(true);
		} else if (cmd.equals("refine_cluster")) {
			refineCluster();
		} else if (cmd.equals("clean_cluster")) {
			cleanCluster();
		} else if (cmd.equals("refine_sub")) {
			refineSubcluster();
		} else if (cmd.equals("merge_sub")) {
			mergeSubclusters();
		} else if (cmd.equals("exclude_subcluster")) {
			excludeSubclusters();
		} else if (cmd.equals("remove_specimen")) {
			removeSpecimens();
		} else if (cmd.startsWith("change_threshold_")) {
			if (selClusterIdx < 0)
				return;
			SingleClusterInfo clIn = clustersInfo.getSingleClusterInfo(selClusterIdx);
			String str = cmd.substring(17);
			int idx = Integer.parseInt(str);
			if (idx < 0 || idx >= clIn.getSpecimensCount())
				return;
			ClusterSpecimenInfo spec = clIn.getClusterSpecimenInfo(idx);
			//
			/*
			double thr=Dialogs.askForDoubleValue(CManager.getAnyFrame(this),
			  "New distance threshold?",spec.distanceThr,0,Double.NaN,null,
			  "Change distance threshold",true);
			if (Double.isNaN(thr)) return;
			spec.distanceThr=thr;
			clearTestResults();
			fillSpecDetailsPanel();
			*/
			//
			InputDoublePanel idp = new InputDoublePanel("New distance threshold?", spec.distanceThr, 0, Double.NaN, null);
			Panel p = new Panel(new ColumnLayout());
			p.add(idp);
			p.add(new Label("Cancel resets to " + spec.origDistanceThr));
			p.add(new Line(false));
			final OKDialog dia = new OKDialog(CManager.getAnyFrame(this), "Change distance threshold", true);
			dia.addContent(p);
			dia.show();
			if (dia.wasCancelled()) {
				resetThreshold(idx);
			} else {
				spec.distanceThr = idp.getEnteredValue();
				clearTestResults();
				fillSpecDetailsPanel();
			}
			//
		} else if (cmd.startsWith("reset_threshold_")) {
			String str = cmd.substring(16);
			int idx = Integer.parseInt(str);
			resetThreshold(idx);
		} else if (cmd.equals("undo")) {
			if (states == null || states.size() < 1)
				return;
			restoreState(states.size() - 1, true);
		} else if (cmd.equals("show_details_of_clusters")) {
			extraTbl.setVisible(true);
			bExtraTable.setEnabled(false);
		}
	}

	protected void resetThreshold(int idx) {
		if (selClusterIdx < 0)
			return;
		SingleClusterInfo clIn = clustersInfo.getSingleClusterInfo(selClusterIdx);
		if (idx < 0 || idx >= clIn.getSpecimensCount())
			return;
		ClusterSpecimenInfo spec = clIn.getClusterSpecimenInfo(idx);
		spec.distanceThr = spec.origDistanceThr;
		clearTestResults();
		fillSpecDetailsPanel();
	}

	protected double getMaxDistThreshold() {
		double maxDist = 0;
		for (int i = 0; i < clustersInfo.getClustersCount(); i++) {
			SingleClusterInfo ci = clustersInfo.getSingleClusterInfo(i);
			for (int j = 0; j < ci.getSpecimensCount(); j++) {
				ClusterSpecimenInfo si = ci.getClusterSpecimenInfo(j);
				if (si.distanceThr > maxDist) {
					maxDist = si.distanceThr;
				}
			}
		}
		return maxDist;
	}

	protected ClusterTestResult getClusterTestResult(int clusterN) {
		if (testResults == null)
			return null;
		for (int i = 0; i < testResults.size(); i++)
			if (testResults.elementAt(i).clusterN == clusterN)
				return testResults.elementAt(i);
		return null;
	}

	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(specList)) {
			fillSpecDetailsPanel();
		} else if (e.getSource().equals(clusterCh)) {
			selClusterIdx = clusterCh.getSelectedIndex();
			String str = clusterCh.getSelectedItem();
			if (!clustersSeen.contains(str)) {
				clustersSeen.addElement(str);
				clusterLabels.elementAt(selClusterIdx).setFont(clusterLabels.elementAt(selClusterIdx).getFont().deriveFont(Font.PLAIN));
			}
			setClusterLabelsStyles();
			showClusterInfo();
			if (extraTbl != null) {
				extraTbl.selectSingleRow(str);
			}
		}
		applyFilters();
	}

	/**
	 * printing methods
	 */

	protected void composeDTstr() {
		String str[] = core.updateDTinPageMaker();
		sLFdt_fmt = str[0]; // human-readable
		sLFdt = str[1]; // file name
		sLFpath = str[2];
	}

	protected void printAll() {
		composeDTstr();
		PageCollection pc = new PageCollection();
		printAllClusters(pc);
		sLFlastCommand = sLFdt_fmt + " <A HREF=\"./" + sLFdt + ".html\">Classifier for all clusters</A></P>";
		core.logSimpleAction(sLFlastCommand);
		core.makePages(pc);
		// restore the map on the screen
		applyFilters();
	}

	protected Frame getFrame() {
		Frame fr = null;
		if (workMapView instanceof Component) {
			fr = CManager.getFrame((Component) workMapView);
		}
		if (fr == null) {
			fr = CManager.getAnyFrame(this);
		}
		if (fr == null) {
			fr = core.getUI().getMainFrame();
		}
		return fr;
	}

	protected void printCurrent() {
		if (selClusterIdx < 0)
			return;
		composeDTstr(); // update date&time
		PageCollection pc = new PageCollection();
		PageStructure ps = new PageStructure();
		pc.addPage(ps);
		ps.layout = PageStructure.LAYOUT_2_COLUMNS;
		int clusterN = clustersInfo.getSingleClusterInfo(selClusterIdx).clusterN;
		pc.refText = sLFdt_fmt + " cluster " + clusterN;
		ps.fname = "_" + clusterN;
		ps.title = "Cluster " + clusterN + " - " + sLFdt_fmt;
		if (core.mayAskForComments()) {
			String txt = Dialogs.askForComment(getFrame(), "Comment?", null, "Comment", true);
			if (txt != null) {
				ps.header = txt;
			}
		}
		Vector specIds = new Vector(10, 10);
		SingleClusterInfo clIn = clustersInfo.getSingleClusterInfo(selClusterIdx);
		for (int i = 0; i < clIn.getSpecimensCount(); i++) {
			specIds.addElement(clIn.getClusterSpecimenInfo(i).specimen.id);
		}
		spFilter.setActiveObjects(specIds);
		IntArray idxs = new IntArray(100, 100);
		for (DClusterObject clusteredObject : clusteredObjects) {
			if (clusteredObject.clusterIdx + 1 == clusterN) {
				idxs.addElement(clusteredObject.idx);
			}
		}
		origLayerFilter.setActiveObjectIndexes(idxs);
		SimpleMapView smv = ((SimpleMapView) workMapView);
		PageElementImage pei = new PageElementImage();
		pei.image = smv.getMapAsImage();
		pei.fname = clustersInfo.getSingleClusterInfo(selClusterIdx).clusterLabel;
		printDetailsOfOneCluster(selClusterIdx, ps, pei);
		sLFlastCommand = sLFdt_fmt + " <A HREF=\"./" + sLFdt + "_" + clustersInfo.getSingleClusterInfo(selClusterIdx).clusterLabel + ".html\">Classifier for cluster " + clustersInfo.getSingleClusterInfo(selClusterIdx).clusterLabel + "</A></P>";
		core.logSimpleAction(sLFlastCommand);
		core.makePages(pc);
		// restore the map on the screen
		applyFilters();
	}

	protected void printDetailsOfOneCluster(int clusterIdx, PageStructure ps, PageElementImage pei_clusterImage) {
		int clusterN = clustersInfo.getSingleClusterInfo(clusterIdx).clusterN;
		SingleClusterInfo clIn = clustersInfo.getSingleClusterInfo(clusterIdx);
		SimpleMapView smv = ((SimpleMapView) workMapView);
		DLayerManager lman = (DLayerManager) workMapView.getLayerManager();
		boolean drawOSMLayer = false;
		DGeoLayer osmLayer = lman.getOSMLayer();
		if (osmLayer != null) {
			drawOSMLayer = osmLayer.getLayerDrawn();
			if (drawOSMLayer) {
				osmLayer.setLayerDrawn(false);
			}
		}
		Frame mFrame = null;
		if (smallMap != null) {
			mFrame = new Frame("Temporary");
			ColumnLayout cl = new ColumnLayout();
			cl.setAlignment(ColumnLayout.Hor_Left);
			mFrame.setLayout(cl);
			mFrame.add(smallMap);
			mFrame.pack();
			mFrame.setVisible(true);
		}
		// process each subcluster separately
		PageElementImage pei[] = new PageElementImage[clIn.getSpecimensCount()];
		for (int ispec = 0; ispec < clIn.getSpecimensCount(); ispec++) {
			Vector specIds = new Vector(1, 1);
			specIds.addElement(clIn.getClusterSpecimenInfo(ispec).specimen.id);
			spFilter.setActiveObjects(specIds);
			IntArray idxs = new IntArray(100, 100);
			for (DClusterObject clusteredObject : clusteredObjects)
				if (clusteredObject.clusterIdx + 1 == clusterN && clusteredObject.subIdx == ispec) {
					idxs.addElement(clusteredObject.idx);
				}
			origLayerFilter.setActiveObjectIndexes(idxs);
			pei[ispec] = new PageElementImage();
			pei[ispec].image = (smallMap == null) ? smv.getMapAsImage(0.4f) : smallMap.getMapAsImage();
			if (pei[ispec].image != null) {
				pei[ispec].width = pei[ispec].image.getWidth(null);
			}
			pei[ispec].fname = "tn" + clIn.clusterLabel + "_" + clIn.getClusterSpecimenInfo(ispec).specimen.id;
		}
		if (mFrame != null) {
			mFrame.dispose();
		}
		if (drawOSMLayer) {
			osmLayer.setLayerDrawn(true);
		}
		PageElementText pet = new PageElementText();
		ps.addElement(pet);
		pet.text = "<B>" + sLFdt_fmt + " - Cluster " + clIn.clusterLabel + "</B>";
		PageElementTable petbl = new PageElementTable();
		ps.addElement(petbl);
		ps.addElement(pei_clusterImage);
		PageElementMultiple pem = new PageElementMultiple();
		ps.addElement(pem);

		petbl.texts = new String[1 + clIn.getSpecimensCount()][];
		for (int nr = 0; nr < petbl.texts.length; nr++) {
			petbl.texts[nr] = new String[6];
		}
		petbl.texts[0][0] = "prototype<BR>ID";
		petbl.texts[0][1] = "Distance<BR>threshold";
		petbl.texts[0][2] = "Original<BR>subcluster<BR>size";
		petbl.texts[0][3] = "N<BR>neighbours<BR>found in<BR>the test";
		petbl.texts[0][4] = "Mean<BR>distance to<BR>the original<BR>neigbours";
		petbl.texts[0][5] = "Mean<BR>distance to<BR>the found<BR>neigbours";
		double maxvals[] = new double[3];
		for (int i = 0; i < maxvals.length; i++) {
			maxvals[i] = 0;
		}
		for (int i = 0; i < clIn.getSpecimensCount(); i++)
			if (specList.isIndexSelected(i)) {
				ClusterSpecimenInfo csp = clIn.getClusterSpecimenInfo(i);
				if (maxvals[0] < csp.distanceThr) {
					maxvals[0] = csp.distanceThr;
				}
				if (maxvals[1] < csp.meanDistOrig) {
					maxvals[1] = csp.meanDistOrig;
				}
				if (maxvals[2] < csp.meanDistNew) {
					maxvals[2] = csp.meanDistNew;
				}
			}
		for (int ispec = 0; ispec < clIn.getSpecimensCount(); ispec++) {
			ClusterSpecimenInfo csp = clIn.getClusterSpecimenInfo(ispec);
			petbl.texts[1 + ispec][0] = "" + csp.specimen.id;
			petbl.texts[1 + ispec][1] = StringUtil.doubleToStr(csp.distanceThr, 0, maxvals[0]);
			petbl.texts[1 + ispec][2] = "" + csp.nSimilarOrig;
			petbl.texts[1 + ispec][3] = "" + csp.nSimilarNew;
			petbl.texts[1 + ispec][4] = StringUtil.doubleToStr(csp.meanDistOrig, 0, maxvals[1]);
			petbl.texts[1 + ispec][5] = StringUtil.doubleToStr(csp.meanDistNew, 0, maxvals[2]);
		}
		int w = (pei[0].width > 0) ? pei[0].width : pei[0].image.getWidth(null);
		pem.nColumns = Math.max(650 / w, 2);
		for (int i = 0; i < pei.length; i++) {
			PageElementMultiple pem_subcl = new PageElementMultiple();
			pem.addItem(pem_subcl);
			pem_subcl.nColumns = 1;
			pem_subcl.addItem(pei[i]);
			PageElementText pet_subcl = new PageElementText();
			pem_subcl.addItem(pet_subcl);
			pet_subcl.text = clIn.getClusterSpecimenInfo(i).specimen.id;
		}

		// save images - to save memory!
		ps.saveAllImages(core.getSupervisor(), sLFpath + sLFdt);
	}

	protected void printAllClusters(PageCollection pc) { // based on applyFilters()
		PageStructure ps = new PageStructure();
		pc.addPage(ps);
		ps.layout = PageStructure.LAYOUT_2_COLUMNS;
		ps.fname = ""; // just date_time.html
		ps.title = "All clusters - " + sLFdt_fmt;
		if (core.mayAskForComments()) {
			String txt = Dialogs.askForComment(getFrame(), "Comment?", null, "Comment", true);
			if (txt != null) {
				ps.header = txt;
			}
		}
		PageElementImage pei[] = new PageElementImage[1 + clustersInfo.getClustersCount()], // large images
		pei_tn[] = new PageElementImage[clustersInfo.getClustersCount()]; // thumbnails
		Vector specIds = new Vector(10, 10);
		// prepare a map with specimens of all clusters
		for (int clusterIdx = 0; clusterIdx < clustersInfo.getClustersCount(); clusterIdx++) {
			SingleClusterInfo clIn = clustersInfo.getSingleClusterInfo(clusterIdx);
			for (int i = 0; i < clIn.getSpecimensCount(); i++) {
				specIds.addElement(clIn.getClusterSpecimenInfo(i).specimen.id);
			}
		}
		spFilter.setActiveObjects(specIds);
		origLayerFilter.setActiveObjects(specIds);
		// save image with specimens of all clusters
		SimpleMapView smv = ((SimpleMapView) workMapView);
		pei[0] = new PageElementImage();
		pei[0].image = smv.getMapAsImage();
		pei[0].fname = "all";
		// now process each cluster separately
		DLayerManager lman = (DLayerManager) workMapView.getLayerManager();
		boolean drawOSMLayer = false;
		DGeoLayer osmLayer = lman.getOSMLayer();
		if (osmLayer != null) {
			drawOSMLayer = osmLayer.getLayerDrawn();
		}
		Frame mFrame = null;
		if (smallMap != null) {
			mFrame = new Frame("Temporary");
			ColumnLayout cl = new ColumnLayout();
			cl.setAlignment(ColumnLayout.Hor_Left);
			mFrame.setLayout(cl);
			mFrame.add(smallMap);
			mFrame.pack();
			mFrame.setVisible(true);
		}
		for (int iteration = 0; iteration < 2; iteration++) {
			if (iteration == 1 && drawOSMLayer) {
				osmLayer.setLayerDrawn(false);
			}
			for (int clusterIdx = 0; clusterIdx < clustersInfo.getClustersCount(); clusterIdx++) {
				int clusterN = clustersInfo.getSingleClusterInfo(clusterIdx).clusterN;
				specIds = new Vector(10, 10);
				SingleClusterInfo clIn = clustersInfo.getSingleClusterInfo(clusterIdx);
				for (int i = 0; i < clIn.getSpecimensCount(); i++) {
					specIds.addElement(clIn.getClusterSpecimenInfo(i).specimen.id);
				}
				spFilter.setActiveObjects(specIds);
				IntArray idxs = new IntArray(100, 100);
				for (DClusterObject clusteredObject : clusteredObjects) {
					if (clusteredObject.clusterIdx + 1 == clusterN) {
						idxs.addElement(clusteredObject.idx);
					}
				}
				origLayerFilter.setActiveObjectIndexes(idxs);
				if (iteration == 0) {
					pei[1 + clusterIdx] = new PageElementImage();
					pei[1 + clusterIdx].image = smv.getMapAsImage();
					pei[1 + clusterIdx].fname = clIn.clusterLabel;
				} else {
					pei_tn[clusterIdx] = new PageElementImage();
					pei_tn[clusterIdx].image = (smallMap == null) ? smv.getMapAsImage(0.4f) : smallMap.getMapAsImage();
					if (pei_tn[clusterIdx].image != null) {
						pei_tn[clusterIdx].width = pei_tn[clusterIdx].image.getWidth(null);
					}
					pei_tn[clusterIdx].fname = "tn" + clIn.clusterLabel;
				}
			}
		}
		if (mFrame != null) {
			mFrame.dispose();
		}
		for (int clusterIdx = 0; clusterIdx < clustersInfo.getClustersCount(); clusterIdx++) {
			PageStructure ps_subcl = new PageStructure();
			pc.addPage(ps_subcl);
			ps_subcl.layout = PageStructure.LAYOUT_2_COLUMNS;
			ps_subcl.fname = "_" + clustersInfo.getSingleClusterInfo(clusterIdx).clusterLabel; // just date_time.html
			ps_subcl.title = "Cluster " + clustersInfo.getSingleClusterInfo(clusterIdx).clusterLabel + " - " + sLFdt_fmt;
			printDetailsOfOneCluster(clusterIdx, ps_subcl, pei[1 + clusterIdx]);
		}
		if (drawOSMLayer) {
			osmLayer.setLayerDrawn(true);
		}
		// create HTML
		PageElementText pet = new PageElementText();
		ps.addElement(pet);
		pet.text = "<B>" + sLFdt_fmt + " - All clusters</B>";
		PageElementTable petbl = new PageElementTable();
		ps.addElement(petbl);
		ps.addElement(pei[0]);
		PageElementMultiple pem = new PageElementMultiple();
		ps.addElement(pem);
		petbl.texts = new String[1 + clustersInfo.getClustersCount()][];
		for (int nr = 0; nr < petbl.texts.length; nr++) {
			petbl.texts[nr] = new String[(testResults == null) ? 4 : 7];
		}
		petbl.texts[0][0] = "cluster ID";
		petbl.texts[0][1] = "N sub-<BR>clusters";
		petbl.texts[0][2] = "max<BR>distance<BR>threshold";
		petbl.texts[0][3] = "N original<BR>members";
		if ((testResults != null)) {
			petbl.texts[0][4] = "correctly<BR>classified";
			petbl.texts[0][5] = "false<BR>negatives";
			petbl.texts[0][6] = "false<BR>positives";
		}
		double overallMax = 0;
		for (int i = 0; i < clustersInfo.getClustersCount(); i++) {
			SingleClusterInfo scl = clustersInfo.getSingleClusterInfo(i);
			for (int j = 0; j < scl.getSpecimensCount(); j++)
				if (scl.getClusterSpecimenInfo(j).distanceThr > overallMax) {
					overallMax = scl.getClusterSpecimenInfo(j).distanceThr;
				}
		}
		for (int i = 0; i < clustersInfo.getClustersCount(); i++) {
			SingleClusterInfo scl = clustersInfo.getSingleClusterInfo(i);
			if (scl == null) {
				continue;
			}
			petbl.texts[1 + i][0] = scl.clusterLabel;
			petbl.texts[1 + i][1] = "" + scl.getSpecimensCount();
			double max = 0f;
			for (int j = 0; j < scl.getSpecimensCount(); j++)
				if (scl.getClusterSpecimenInfo(j).distanceThr > max) {
					max = scl.getClusterSpecimenInfo(j).distanceThr;
				}
			petbl.texts[1 + i][2] = ((max > 0) ? StringUtil.doubleToStr(max, 0, overallMax) : "");
			petbl.texts[1 + i][3] = "" + scl.origSize;
			if (testResults != null && testResults.elementAt(i) != null) {
				ClusterTestResult ctr = testResults.elementAt(i);
				petbl.texts[1 + i][4] = "" + ctr.getNCorrectlyClassified();
				petbl.texts[1 + i][5] = "" + ctr.getNFalseNegatives();
				petbl.texts[1 + i][6] = "" + ctr.getNFalsePositives();
			}
		}
		int w = (pei_tn[0].width > 0) ? pei_tn[0].width : pei_tn[0].image.getWidth(null);
		pem.nColumns = Math.max(800 / w, 2);
		for (int i = 0; i < pei_tn.length; i++) {
			PageElementMultiple pem_subcl = new PageElementMultiple();
			//pem_subcl.pageRef=pc.pages.elementAt(1+i);
			pei_tn[i].pageRef = pc.pages.elementAt(1 + i);
			pem.addItem(pem_subcl);
			pem_subcl.nColumns = 1;
			pem_subcl.addItem(pei_tn[i]);
			PageElementText pet_subcl = new PageElementText();
			pem_subcl.addItem(pet_subcl);
			SingleClusterInfo scl = clustersInfo.getSingleClusterInfo(i);
			pet_subcl.text = scl.clusterLabel; // pei_tn[i].fname;
			pet_subcl.pageRef = pc.pages.elementAt(1 + i);
		}
	}

	/**
	 * Tries to create an image layer to be used as a background for small maps
	 */
	protected void makeImageLayer() {
		if (imgMaker != null)
			return;
		DLayerManager lman = (DLayerManager) workMapView.getLayerManager();
		if (lman == null)
			return;
		boolean toDraw[] = new boolean[lman.getLayerCount()];
		for (int i = 0; i < lman.getLayerCount(); i++) {
			DGeoLayer layer = lman.getLayer(i);
			toDraw[i] = layer.getLayerDrawn() && !(layer instanceof DLinkLayer) && !(layer instanceof DAggregateLinkLayer) && layer.getSubtype() != Geometry.movement;
		}
		origLayer.setHasAllObjects(true);
		RealRectangle bounds = origLayer.getWholeLayerBounds();
		float w = bounds.rx2 - bounds.rx1, h = bounds.ry2 - bounds.ry1, dw = 0.05f * w, dh = 0.05f * h;
		RealRectangle terr = new RealRectangle(bounds.rx1 - dw, bounds.ry1 - dh, bounds.rx2 + dw, bounds.ry2 + dh);
		w = terr.rx2 - terr.rx1;
		h = terr.ry2 - terr.ry1;
		int iw = Math.round(50f * getFrame().getToolkit().getScreenResolution() / 25.33f);
		int ih = (int) Math.round(Math.ceil(iw * h / w));
		imgMaker = new MapBkgImageMaker(core, lman, toDraw, false, terr, new Dimension(iw, ih), this);
		imgMaker.start();
	}

	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(imgMaker) && e.getPropertyName().equals("bkg_map")) {
			if (e.getNewValue() != null && (e.getNewValue() instanceof DGeoLayer)) {
				bkgLayer = (DGeoLayer) e.getNewValue();
				DLayerManager lman = (DLayerManager) workMapView.getLayerManager();
				DLayerManager lm = (DLayerManager) lman.makeCopy(false);
				if (lm.getOSMLayer() != null) {
					lm.getOSMLayer().setLayerDrawn(false);
				}
				lm.addGeoLayer(bkgLayer);
				lm.addGeoLayer(origLayer);
				lm.addGeoLayer(spLayer);
				smallMap = new MapCanvas();
				smallMap.setMapContent(lm);
				Image img = imgMaker.getMapBkgImage();
				int iw = img.getWidth(null), ih = img.getHeight(null);
				smallMap.setPreferredSize(iw, ih);
				smallMap.setSize(iw, ih);
				MapContext mc = smallMap.getMapContext();
				mc.setVisibleTerritory(bkgLayer.getWholeLayerBounds());
				mc.setViewportBounds(0, 0, iw, ih);
			}
			imgMaker = null;
		}
	}

	/**
	 * Standard interfaces
	 */

	protected boolean destroyed = false;

	public void destroy() {
		if (destroyed)
			return;
		if (imgMaker != null) {
			imgMaker.stopWork();
			imgMaker = null;
		}
		if (origLayer != null && origLayerFilter != null) {
			origLayer.removeObjectFilter(origLayerFilter);
			origLayerFilter.destroy();
			origLayerFilter = null;
		}
		if (spFilter != null) {
			spLayer.removeObjectFilter(spFilter);
			spFilter.destroy();
			spFilter = null;
		}
		destroyed = true;
		if (extraTbl != null) {
			extraTbl.dispose();
			extraTbl = null;
		}
	}

	public boolean isDestroyed() {
		return destroyed;
	}

	public ClustersInfo getClustersInfo() {
		return clustersInfo;
	}

	public DClusterObject[] getClusteredObjects() {
		return clusteredObjects;
	}

	public DGeoLayer getOrigLayer() {
		return origLayer;
	}

	public MapViewer getMapViewer() {
		return workMapView;
	}

	/**
	 * methods from MouseListener interface:
	 * reactions to clicks on the cluster status labels
	 */
	public void mouseReleased(MouseEvent e) {
		if (e.getSource() instanceof Label) {
			Label l = (Label) e.getSource();
			clusterCh.select(l.getText());
			itemStateChanged(new ItemEvent(clusterCh, 0, null, 0));
		}
	}

	public void mouseClicked(MouseEvent e) {
	} //see mouseReleased

	public void mousePressed(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mouseMoved(MouseEvent e) {
	}

	public void mouseDragged(MouseEvent e) {
	}

	/**
	 * methods from WindowListener interface:
	 * reactions to closing of extra statistics table window
	 */
	public void windowClosing(WindowEvent e) {
		extraTbl.setVisible(false);
		bExtraTable.setEnabled(true);
	}

	public void windowClosed(WindowEvent e) {
	}

	public void windowOpened(WindowEvent e) {
	}

	public void windowIconified(WindowEvent e) {
	}

	public void windowDeiconified(WindowEvent e) {
	}

	public void windowActivated(WindowEvent e) {
	}

	public void windowDeactivated(WindowEvent e) {
	}

}
