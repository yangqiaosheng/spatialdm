package spade.analysis.tools;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.DataLoader;
import spade.analysis.system.DisplayProducer;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.StepwiseDBSCAN.boundingBox;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.color.CS;
import spade.lib.util.IdMaker;
import spade.lib.util.IntArray;
import spade.lib.util.QSortAlgorithm;
import spade.time.FocusInterval;
import spade.time.TimeCount;
import spade.time.TimeMoment;
import spade.time.ui.TimeControlPanel;
import spade.time.vis.ClusterTimeGraphPanel;
import spade.time.vis.TimeGraph;
import spade.time.vis.TimeGraphPanel;
import spade.time.vis.TimeGraphSummary;
import spade.vis.action.HighlightListener;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.Parameter;
import spade.vis.database.SpatialEntity;
import spade.vis.dataview.TableViewer;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DTemporalCluster;
import spade.vis.geometry.RealPoint;
import spade.vis.map.MapViewer;
import spade.vis.mapvis.Visualizer;
import spade.vis.space.GeoLayer;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.DBScan;
import weka.clusterers.EM;
import weka.clusterers.SimpleKMeans;
import weka.core.Instances;

public class WekaClusterersCP extends Frame implements ActionListener {
	protected Instances instances = null;
	protected int tblRowsInInstances[] = null;
	protected ESDACore core = null;
	protected AttributeDataPortion tbl = null;
	protected int attrIdx = -1;
	protected Checkbox cbNewAttr = null;
	protected TextField tfNClusters = null;

	// new: parameters for dbscan stepwise
	protected TextField tfNSpatial = null;
	protected TextField tfNTemporal = null;
	protected TextField tfNNeighbour = null;
	protected TextField tfNSpatialDis = null;
	protected TextField tfNEntries = null;

	protected Choice chClusterer = null;
	protected Label lAttrName = null;
	protected WekaAttrSelector was = null;

	public WekaClusterersCP(Instances instances, int tblRowsInInstances[], ESDACore core, AttributeDataPortion tbl) {
		super("Weka Clusterer Control Panel");
		this.instances = instances;
		this.core = core;
		this.tbl = tbl;
		this.tblRowsInInstances = tblRowsInInstances;
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dispose();
			}
		});
		setLayout(new BorderLayout());
		add(was = new WekaAttrSelector(instances, tbl), BorderLayout.CENTER);
		Panel p = new Panel();
		add(p, BorderLayout.SOUTH);
		p.setLayout(new ColumnLayout());
		p.add(new Line(false));
		Panel pp = new Panel(new FlowLayout());
		pp.add(new Label("Clusterer:"));
		pp.add(chClusterer = new Choice());
		chClusterer.add("SimpleKmeans");
		chClusterer.add("EM");
		// new: integrate dbscan
		chClusterer.add("DBSCAN");
		// new: integrate dbscan for spatial temporal clustering
		chClusterer.add("DBSCAN (Stepwise)");
		// new: spatial temporal indexed clustering
		chClusterer.add("ST(I) DBSCAN");
		// a faster version of stepwise dbscan
		chClusterer.add("DBSCAN (Stepwise)");
		p.add(pp);

		// new: for dbscan stepwise
		chClusterer.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				// TODO Auto-generated method stub
				if (chClusterer.getSelectedItem().equals("DBSCAN (Stepwise)")) {
					tfNSpatial.setEnabled(true);
					tfNTemporal.setEnabled(true);
					tfNNeighbour.setEnabled(true);
					tfNSpatialDis.setEnabled(true);
					tfNEntries.setEnabled(true);
					tfNClusters.setEnabled(false);
				} else if (chClusterer.getSelectedItem().equals("DBSCAN")) {
					tfNSpatial.setEnabled(true);
					tfNTemporal.setEnabled(false);
					tfNSpatialDis.setEnabled(false);
					tfNEntries.setEnabled(false);
					tfNNeighbour.setEnabled(true);
					tfNClusters.setEnabled(false);
				} else if (chClusterer.getSelectedItem().equals("ST(I) DBSCAN")) {
					tfNSpatial.setEnabled(true);
					tfNTemporal.setEnabled(true);
					tfNSpatialDis.setEnabled(false);
					tfNEntries.setEnabled(false);
					tfNNeighbour.setEnabled(true);
					tfNClusters.setEnabled(false);
				} else {
					tfNSpatial.setEnabled(false);
					tfNTemporal.setEnabled(false);
					tfNNeighbour.setEnabled(false);
					tfNSpatialDis.setEnabled(false);
					tfNEntries.setEnabled(false);
					tfNClusters.setEnabled(true);
				}
			}
		});
		p.add(new Line(false));
		pp = new Panel(new FlowLayout());
		pp.add(new Label("Desired Number of Clusters:"));
		pp.add(tfNClusters = new TextField("2", 4));
		p.add(pp);

		// new: parameters for dbscan stepwise
		pp = new Panel(new FlowLayout());
		pp.add(new Label("Spatial threshold:"));
		pp.add(tfNSpatial = new TextField("500", 4));
		tfNSpatial.setEnabled(false);
		p.add(pp);

		pp = new Panel(new FlowLayout());
		pp.add(new Label("Temporal threshold:"));
		pp.add(tfNTemporal = new TextField("600", 4));
		tfNTemporal.setEnabled(false);
		p.add(pp);

		pp = new Panel(new FlowLayout());
		pp.add(new Label("Minimum entries for temporal thrshold:"));
		pp.add(tfNEntries = new TextField("500", 4));
		tfNEntries.setEnabled(false);
		p.add(pp);

		pp = new Panel(new FlowLayout());
		pp.add(new Label("Spatial distance for temporal threshold:"));
		pp.add(tfNSpatialDis = new TextField("600", 4));
		tfNSpatialDis.setEnabled(false);
		p.add(pp);

		pp = new Panel(new FlowLayout());
		pp.add(new Label("Minimum neighbours:"));
		pp.add(tfNNeighbour = new TextField("3", 4));
		tfNNeighbour.setEnabled(false);
		p.add(pp);

		p.add(new Line(false));
		pp = new Panel();
		pp.setLayout(new BorderLayout());
		Button b = new Button("Run clusterer");
		b.addActionListener(this);
		pp.add(b, BorderLayout.CENTER);
		cbNewAttr = new Checkbox("Store results in new attribute", true);
		cbNewAttr.setEnabled(false);
		pp.add(cbNewAttr, BorderLayout.EAST);
		p.add(pp);
		p.add(lAttrName = new Label("Attribute name="));
		setSize(500, 300);
		pack();
		show();
		// The window must be properly registered in order to be closed in a
		// case
		// when the aplication is closed or changed.
		core.getWindowManager().registerWindow(this);
	}

	public void actionPerformed(ActionEvent ae) {
		int nclasses = 0;

		boolean error = false;
		try {

			nclasses = Integer.parseInt(tfNClusters.getText());
			if (nclasses < 2) {
				nclasses = 2;
				error = true;
			}
		} catch (NumberFormatException nfe) {
			error = true;
		}
		if (error) {
			tfNClusters.setText("2");
		} else {
			runClusterer(nclasses);
			cbNewAttr.setState(false);
		}
	}

	// new:
	int dbscanCluster = 0;
	// new: initialisation
	double results[] = null;

	protected void runClusterer(int nclasses) {

		core.getUI().showMessage("Clustering in progress...", false);
		long t0 = System.currentTimeMillis();
		// new: more clustering methods
		switch (chClusterer.getSelectedIndex()) {
		case 0: {
			results = runSimpleKMeans(nclasses);
			break;
		}
		case 1: {
			results = runEM(nclasses);
			break;
		}
		case 2: {
			results = runDBSCAN(nclasses);
			nclasses = dbscanCluster;
			break;
		}
		case 3: {
			results = runDBSCAN_Stepwise(0);
			nclasses = dbscanCluster;
			long t = System.currentTimeMillis();
			System.out.println("The clustering took " + (t - t0) + " msec., " + ((t - t0) / 1000f) + " sec.");
			core.getUI().showMessage("Clustering finished; putting the results in the table", false);
			return;
		}
		case 4: {
			results = runSTDBSCAN(nclasses);
			nclasses = dbscanCluster;
			break;
		}
		case 5: {
			results = runDBSCAN_Stepwise_Fast(0);
			nclasses = dbscanCluster;
			long t = System.currentTimeMillis();
			System.out.println("The clustering took " + (t - t0) + " msec., " + ((t - t0) / 1000f) + " sec.");
			core.getUI().showMessage("Clustering finished; putting the results in the table", false);
			return;
		}
		}

		DataTable dTable = (DataTable) tbl;
		if (attrIdx >= 0 && !cbNewAttr.getState()) {
			updateTable(dTable, nclasses, results);
			// inform all displays about change of values
			Vector attr = new Vector(1, 1);
			attr.addElement(dTable.getAttributeId(attrIdx));
			dTable.notifyPropertyChange("values", null, attr);
		} else {
			int i = 0;
			String attrName = "";
			do {
				i++;
				attrName = "Cluster " + i;
			} while (dTable.findAttrByName(attrName) >= 0);
			lAttrName.setText("Attribute name=" + attrName);
			cbNewAttr.setEnabled(true);
			dTable.addAttribute(attrName, null, AttributeTypes.character);
			attrIdx = dTable.getAttrCount() - 1;
			updateTable(dTable, nclasses, results);
			DataKeeper dk = core.getDataKeeper();
			GeoLayer layer = dk.getTableLayer(dTable);
			if (layer != null) {
				Vector clusterAttr = new Vector(1, 1);
				clusterAttr.addElement(dTable.getAttributeId(attrIdx));
				DisplayProducer dpr = core.getDisplayProducer();
				dpr.displayOnMap("qualitative_colour", dTable, clusterAttr, layer, core.getUI().getMapViewer(0));
			}
		}
	}

	protected double[] runSimpleKMeans(int nclasses) {
		Instances sInstances = was.getSubset();
		System.out.println("* SimpleKMeans Clusterer: Start");
		SimpleKMeans clusterer = new SimpleKMeans();
		ClusterEvaluation eval = new ClusterEvaluation();

		try {
			clusterer.setNumClusters(nclasses);
			clusterer.setSeed(10);
			clusterer.buildClusterer(sInstances);
			eval.setClusterer(clusterer);
			eval.evaluateClusterer(sInstances);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("* SimpleKMeans Clusterer: Finish");
		return eval.getClusterAssignments();
	}

	TableViewer tableViewer = null;
	DataTable tmp_dt = null;
	DGeoLayer spLayer = null;
	MapViewer workMapView = null;
	public DataTable refTable = null;
	Visualizer vis = null;

	public void showStats(final DataTable freqTable) {
		freqTable.setEntitySetIdentifier("ClusterFrequenyTable");
		refTable = freqTable;
		// select clusters to be loaded
		String tblNameClust = "Results";

		tableViewer = new TableViewer(freqTable, core.getSupervisor(), new PropertyChangeListener() {
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
		p.add(new Label("Select clusters for loading from " + tblNameClust), BorderLayout.NORTH);
		p.add(tableViewer, BorderLayout.CENTER);
		Panel pp = new Panel(new ColumnLayout());
		pp.add(new Label("Selected clusters:"));
		final Label lSelected = new Label("                                             "), lTrN = new Label("0 trajectories in total");
		pp.add(lSelected);
		pp.add(lTrN);
		pp.add(new Line(false));
		Panel ppp = new Panel(new BorderLayout());
		pp.add(ppp);
		final Button bCopyClusters = new Button("Copy selected cluster(s) to new Oracle table");
		bCopyClusters.setEnabled(false);
		ppp.add(bCopyClusters, BorderLayout.EAST);
		pp.add(new Line(false));
		p.add(pp, BorderLayout.SOUTH);

		// clear all previous selected objects
		core.getSupervisor().getHighlighter(freqTable.getEntitySetIdentifier()).clearSelection(this);
		core.getSupervisor().getHighlighter(freqTable.getEntitySetIdentifier()).addHighlightListener(new HighlightListener() {
			public void highlightSetChanged(Object source, String setId, Vector highlighted) {

				// plot the temporal sub clusters of a spatial super
				// cluster
				if (setId == null || setId.equals("-1"))
					return;

				double[] clusters = null;
				List<Double> lClusters = new ArrayList<Double>();
				int id = -1;

				for (int i = 0; i < tbl.getDataItemCount(); i++) {
					{
						// cluster id decreased by one due to
						// visualization of the clusters with ids

						if (Double.parseDouble(setId) == results[i]) {
							id++;
							lClusters.add(results[i]);
						}
						lClusters.add(-1.0);
					}

				}
				clusters = new double[lClusters.size()];
				for (int i = 0; i < lClusters.size(); i++) {
					clusters[i] = lClusters.get(i);
				}
				int i = 0;
				String attrName = "Cluster";
				do {
					i++;
					attrName = "Cluster " + i;
				} while (tbl.findAttrByName(attrName) >= 0);
				tbl.addAttribute(attrName, null, AttributeTypes.character);
				attrIdx = tbl.getAttrCount() - 1;
				updateTable((DataTable) tbl, id + 1, clusters);
				DataKeeper dk = core.getDataKeeper();
				GeoLayer layer = dk.getTableLayer(tbl);
				if (layer != null) {
					Vector clusterAttr = new Vector(1, 1);
					clusterAttr.addElement(tbl.getAttributeId(attrIdx));
					DisplayProducer dpr = core.getDisplayProducer();
					dpr.displayOnMap("qualitative_colour", tbl, clusterAttr, layer, core.getUI().getMapViewer(0));
				}

			}

			public void selectSetChanged(Object source, String setId, Vector highlighted) {
				String str = "";
				long nTr = 0;
				if (highlighted != null) {
					for (int i = 0; i < highlighted.size(); i++) {
						String id = ((String) highlighted.elementAt(i));
						int rowN = freqTable.getObjectIndex(id);
						if (rowN >= 0) {
							str += ((str.length() > 0) ? "," : "") + id;
							nTr += freqTable.getNumericAttrValue(1, rowN);
						}
					}
				}
				lSelected.setText(str);
				lTrN.setText(nTr + " trajectories in total");
				bCopyClusters.setEnabled(nTr > 0);

				IntArray selRows = tableViewer.getSelNumbers();
				Vector v = core.getSupervisor().getHighlighter(freqTable.getEntitySetIdentifier()).getSelectedObjects();
				if (v == null)
					return;
				// get selected classes
				ArrayList<String> l = new ArrayList<String>();

				DataTable t = freqTable;
				// number of entries
				int n = 0;
				// ids of the selected clusters
				String ids = "";
				// the parent of the selected cluster
				String parent = "";
				String ns = "";
				for (int i = 0; i < v.size(); i++) {
					l.add((String) ((spade.vis.database.DataRecord) t.getData().get(Integer.parseInt(v.get(i).toString()) - 1)).getAttrValues().get(0));
					parent = (String) ((spade.vis.database.DataRecord) t.getData().get(Integer.parseInt(v.get(i).toString()) - 1)).getAttrValues().get(1);
					ids += ", " + (String) ((spade.vis.database.DataRecord) t.getData().get(Integer.parseInt(v.get(i).toString()) - 1)).getAttrValues().get(0);
					n += Integer.parseInt(((spade.vis.database.DataRecord) t.getData().get(Integer.parseInt(v.get(i).toString()) - 1)).getAttrValues().get(2).toString());
					ns += ", " + ((spade.vis.database.DataRecord) t.getData().get(Integer.parseInt(v.get(i).toString()) - 1)).getAttrValues().get(2).toString();
				}

				// plot the temporal sub clusters of a spatial super
				// cluster
				if (parent.equals("-1")) {
					// only the subclusters of cluster c_id are
					// considered
					String c_id = l.get(0);
					l.clear();
					n = 0;
					// a new table is filled
					DataTable dt_new = new DataTable();
					dt_new.setAttrList(freqTable.getAttrList());
					// scan the table
					DataRecord rec = null;
					// not empty
					boolean empty = true;
					for (int k = 0; k < freqTable.getDataItemCount(); k++) {
						rec = freqTable.getDataRecord(k);
						if (c_id.equals(rec.getAttrValue(1))) {
							// table to plot
							dt_new.addDataRecord(rec);
							// sub clusters
							l.add(rec.getAttrValueAsString(0));
							// number of entries in the sub clusters
							n += Integer.parseInt(rec.getAttrValueAsString(2));
							empty = false;
						}
					}
					if (!empty) {
						showTimeSeriesTraj(dt_new);
					}
					// return;
				}

				double[] clusters = new double[n];
				int id = -1;
				boolean inserted = false;
				DataLoader dLoader = core.getDataLoader();
				if (tmp_dt != null) {
					dLoader.removeTable(tmp_dt.getContainerIdentifier());
				}
				// create new data table containing only entries with
				// selected clusters
				tmp_dt = new DataTable();

				//tmp_dt.setEntitySetIdentifier(tbl
				//		.getEntitySetIdentifier());
				tmp_dt.makeUniqueAttrIdentifiers();
				// add all attributes
				for (int i = 0; i < tbl.getAttrCount(); i++) {
					tmp_dt.addAttribute(tbl.getAttribute(i));
				}

				// need new data table and new layer
				DataKeeper dk = core.getDataKeeper();
				DGeoLayer layer = (DGeoLayer) dk.getTableLayer(tbl);

				Vector<DGeoObject> spObjects = new Vector(50, 50);
				for (int i = 0; i < tbl.getDataItemCount(); i++) {
					inserted = false;
					for (int j = 0; j < l.size(); j++) {
						// cluster id decreased by one due to
						// visualization of the clusters with ids

						if (Double.parseDouble(l.get(j)) == results[i]) {
							id++;
							clusters[id] = results[i];
							inserted = true;
							tmp_dt.addDataItem(tbl.getDataItem(i));
							spObjects.add((DGeoObject) layer.getObjectAt(i));
							break;
						}

					}

				}

				if (spLayer == null) {
					// create new layer
					spLayer = new DGeoLayer();
					spLayer.setType(layer.getType());
					spLayer.setName("Subcluster");
					spLayer.setGeoObjects(spObjects, true);
					spLayer.setGeographic(layer.isGeographic());
					spLayer.setHasMovingObjects(layer.getHasMovingObjects());
					// spLayer.setEntitySetIdentifier();
					spLayer.setDataTable(tmp_dt);
					spLayer.setLinkedToTable(true);
					// spLayer.setThematicFilter(tmp_dt.getObjectFilter());

					// link
					dLoader.addMapLayer(spLayer, -1);
					dLoader.processTimeReferencedObjectSet(spLayer);
					spLayer.setLayerDrawn(false);
					workMapView = core.getUI().getMapViewer(null);
					// find the copy of the geographical layer in the
					// new map view
					int lidx = workMapView.getLayerManager().getIndexOfLayer(spLayer.getContainerIdentifier());
					if (lidx >= 0) {
						spLayer = (DGeoLayer) workMapView.getLayerManager().getGeoLayer(lidx);
					}
					lidx = workMapView.getLayerManager().getIndexOfLayer(layer.getContainerIdentifier());
					if (lidx >= 0) {
						((DGeoLayer) workMapView.getLayerManager().getGeoLayer(lidx)).setLayerDrawn(false);
					}

				} else {
					DisplayProducer dpr = core.getDisplayProducer();
					dpr.eraseDataFromMap(spLayer, workMapView);
					// spLayer.removeAllObjects();
					spLayer.setGeoObjects(spObjects, true);
					spLayer.notifyPropertyChange("ObjectSet", null, null);
					// spLayerCopy.removeAllObjects();
					spLayer.setGeoObjects(spObjects, true);
					spLayer.notifyPropertyChange("ObjectSet", null, null);
				}
				int tblN = dLoader.addTable(tmp_dt);
				dLoader.setLink(spLayer, tblN);

				// update table for all cluster, also for noise (+1)

				tmp_dt.addAttribute("Selected clusters", null, AttributeTypes.character);
				attrIdx = tmp_dt.getAttrCount() - 1;
				updateTable((DataTable) tmp_dt, dbscanCluster + 1, clusters);
				// attrIdx++;
				if (layer != null) {
					Vector clusterAttr = new Vector(1, 1);
					clusterAttr.addElement(tmp_dt.getAttributeId(attrIdx));
					DisplayProducer dpr = core.getDisplayProducer();
					vis = dpr.displayOnMap("qualitative_colour", tmp_dt, clusterAttr, spLayer, workMapView);

				}

			}
		}

		);
		bCopyClusters.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
			}
		});
		Frame f = null;
		if (core.getUI() != null) {
			f = core.getUI().getMainFrame();
		}
		if (f == null) {
			f = CManager.getAnyFrame();
		}

		OKDialog dia = new OKDialog(false, f, "Select clustering results", true);
		dia.addContent(p);

		lSelected.setFont(lSelected.getFont().deriveFont(Font.BOLD));
		dia.show();
		if (dia.wasCancelled())
			return;
		Vector v = core.getSupervisor().getHighlighter(freqTable.getEntitySetIdentifier()).getSelectedObjects();
		if (v == null || v.size() == 0)
			return;
	}

	/**
	 * A reference to an already created time filter panel, which is used for
	 * events and movement data. This allows to avoid creation of two panels for
	 * the same data set.
	 */
	protected TimeControlPanel timeFilterControlPanel = null;
	/**
	 * A reference to an already created display time control panel, which is
	 * used in visualisation of time-series data. This allows to avoid creation
	 * of two panels for the same data set.
	 */
	protected TimeControlPanel displayTimeControlPanel = null;

	public void showTimeSeriesTraj(AttributeDataPortion table) {
		DGeoLayer trajectory = new DGeoLayer();
		Vector mObjects = new Vector(200, 100); // trajectories
		DTemporalCluster mov = new DTemporalCluster();
		// the date of the startings and endings of the clusters
		List<spade.time.Date> dates = new ArrayList<spade.time.Date>();

		Vector<Object[]> colors = new Vector<Object[]>();
		for (int k = 0; k < table.getDataItemCount(); k++) {

			// beginning
			TimeMoment tc_start = new TimeCount();
			// get time reference
			String dr_start = (String) ((DataRecord) ((DataTable) table).getData().get(k)).getAttrValue(9);
			// end
			TimeMoment tc_end = new TimeCount();
			// get time reference
			String dr_end = (String) ((DataRecord) ((DataTable) table).getData().get(k)).getAttrValue(10);
			try {
				SimpleDateFormat df1 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.UK);
				// tc_start.setMoment(df1.parse(dr_start).getTime());
				// tc_end.setMoment(df1.parse(dr_end).getTime());
				spade.time.Date d = new spade.time.Date();
				spade.time.Date d2 = new spade.time.Date();
				try {
					d.setDate(new Date(df1.parse(dr_start).getTime()), new Time(df1.parse(dr_start).getTime()));
					d2.setDate(new Date(df1.parse(dr_end).getTime()), new Time(df1.parse(dr_end).getTime()));
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
				tc_start = d;
				tc_end = d2;
				dates.add((spade.time.Date) d.getCopy());
				dates.add((spade.time.Date) d2.getCopy());
			} catch (Exception e) {
				e.printStackTrace();
			}
			DataRecord rec = ((DataTable) table).getDataRecord(k); // new
			// DataRecord("");
			// add number of entries in the cluster
			SpatialEntity se = new SpatialEntity("");
			se.setGeometry(new RealPoint(1, 1));
			mov.setup(se);
			mov.addPosition(new RealPoint(1, 1), tc_start, tc_end, rec);
			DataRecord r = new DataRecord("");
			r.addAttrValue(mov);
		}
		// look for temporal gaps among the temporal cluster
		spade.time.Date[] arrayDates = new spade.time.Date[dates.size()];
		arrayDates = dates.toArray(arrayDates);
		// sorting according to time
		QSortAlgorithm.sort(arrayDates);
		//
		for (int i = 1; i < arrayDates.length / 2; i++) {
			// starts next cluster at end of the last one?
			if (!(arrayDates[2 * i].equals(arrayDates[2 * i - 1]))) {
				// add new record as noise for the temporal gap
				DataRecord rec = new DataRecord("tmp");
				// invalid data
				rec.setAttrList(((DataTable) table).getDataRecord(0).getAttrList());
				rec.setAttrValue("-1", 0);
				for (int j = 1; j < rec.getAttrCount(); j++) {
					rec.setAttrValue(0, j);
				}
				SpatialEntity se = new SpatialEntity("");
				se.setGeometry(new RealPoint(1, 1));
				mov.setup(se);
				arrayDates[2 * i - 1].add(1);
				arrayDates[2 * i].add(-1);
				mov.addPosition(new RealPoint(1, 1), arrayDates[2 * i - 1], arrayDates[2 * i], rec);
				// mov.addPosition(new RealPoint(1, 1), arrayDates[2*i],
				// arrayDates[2*i], rec);
			} else {

			}
		}
		// add colors
		Map<String, Color> clusterToColor = new HashMap<String, Color>();
		int color = 0;
		for (int i = 0; i < mov.getPositionCount(); i++) {
			TimeMoment tm = mov.getPosition(i).getTimeReference().getValidFrom();
			String id = (String) mov.getPosition(i).getThematicData().getAttrValue(0);
			if (!clusterToColor.containsKey(id)) {
				color++;
				float cl = (color) / (mov.getPositionCount());
				// always the same color for noise
				if (id.equals("-1")) {
					cl = 0.0f;
				}
				clusterToColor.put(id, Color.getHSBColor(cl, (float) Math.random(), (float) Math.random()));
			}

			colors.add(new Object[] { tm, clusterToColor.get(id) });
		}

		int i = 0;
		trajectory.addGeoObject(mov);
		trajectory.setContainerIdentifier("clusters");
		trajectory.setEntitySetIdentifier("clusters");
		ClusterTimeGraphPanel ttgp = new ClusterTimeGraphPanel(trajectory);
		ttgp.setName("Time graph:");
		ttgp.setTableRef(refTable.getEntitySetIdentifier());
		// core.getSupervisor().getColorsForTimes();
		ttgp.setColorsForTimes(colors);
		ttgp.setSupervisor(core.getSupervisor());
		// core.getSupervisor().registerTool(ttgp);
		core.getDisplayProducer().showGraph(ttgp);
	}

	public void showTimeSeries(AttributeDataPortion table) {
		Parameter par = new Parameter();
		Attribute att = table.getAttribute(10);
		Attribute attrParentNum = new Attribute(IdMaker.makeId(((DataTable) table).getAttrCount() + " ", (DataTable) table), AttributeTypes.integer);
		attrParentNum.setName("");
		par.setName("Temp");
		// create new table for plotting temp referenzed date
		DataTable dt = new DataTable();
		Vector attValues = new Vector();
		// for all data to be plotted
		for (int k = 0; k < table.getDataItemCount(); k++) {
			// get time reference
			String dr_start = (String) ((DataRecord) ((DataTable) table).getData().get(k)).getAttrValue(9);
			String dr_end = (String) ((DataRecord) ((DataTable) table).getData().get(k)).getAttrValue(10);
			// delete CET from time string
			// dr_start = dr_start.replaceAll("CET ", "");
			// dr_end = dr_end.replaceAll("CET ", "");
			SimpleDateFormat df1 = new SimpleDateFormat("E MMM dd HH:mm:ss zzz yyyy", Locale.UK);
			// create date objects for time of beginning and end of cluster
			spade.time.Date d = new spade.time.Date();
			spade.time.Date d2 = new spade.time.Date();
			try {
				d.setDate(new Date(df1.parse(dr_start).getTime()), new Time(df1.parse(dr_start).getTime()));
				d2.setDate(new Date(df1.parse(dr_end).getTime()), new Time(df1.parse(dr_end).getTime()));
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
			// add all time stamps to the parameter
			par.addValue(d);
			par.addValue(d2);
			// this parameter is temporal
			par.setTemporal(true);
			String name = d.toString();
			// create for each time stamp an attribute
			Attribute attrChild1 = new Attribute(attrParentNum.getIdentifier() + "_" + name, attrParentNum.getType());
			attrParentNum.addChild(attrChild1);
			// generate the values for the attributes, i.e. attribute values for
			// beginning and end of cluster
			attValues.add((String) ((DataRecord) ((DataTable) table).getData().get(k)).getAttrValue(2));
			attrChild1.setType(AttributeTypes.integer);
			// add the attributes to the table
			dt.addAttribute(attrChild1);
			// connect attribute and parameter value
			attrChild1.addParamValPair(par.getName(), par.getValue(2 * k));
			// also for end
			Attribute attrChild2 = new Attribute(attrParentNum.getIdentifier() + "_" + name, attrParentNum.getType());
			attrParentNum.addChild(attrChild2);
			// generate the values for the attributes, i.e. attribute values for
			// beginning and end of cluster
			attValues.add((String) ((DataRecord) ((DataTable) table).getData().get(k)).getAttrValue(2));
			attrChild2.setType(AttributeTypes.integer);
			// add the attributes to the table
			dt.addAttribute(attrChild2);
			// connect attribute and parameter value
			attrChild2.addParamValPair(par.getName(), par.getValue(2 * k + 1));
		}
		// add parameter to table
		((DataTable) dt).addParameter(par);
		// now a record must be created that contains the attribute values
		DataRecord dr = new DataRecord("1", "1");
		dr.setAttrValues(attValues);
		// add the record
		((DataTable) dt).addDataRecord(dr);
		// sort the parameter values
		par.sortValues();

		Vector<Attribute> attrDescr = new Vector<Attribute>();
		Vector<Parameter> paras = new Vector<Parameter>();
		attrDescr.add(table.getAttribute(2));
		paras.add(table.getParameter(2));
		if (table == null || par == null || attrDescr == null || attrDescr.size() < 1)
			return;
		FocusInterval fint = getFocusInterval(par);

		if (fint == null)
			return;
		// generate graph for plotting the time series
		TimeGraph tg[] = new TimeGraph[attrDescr.size()];
		TimeGraphSummary tsummary[] = new TimeGraphSummary[attrDescr.size()];
		for (int i = 0; i < attrDescr.size(); i++) {

			tg[i] = new TimeGraph();
			tg[i].setTable(dt);
			tg[i].setSupervisor(core.getSupervisor());
			tg[i].setAttribute(attrParentNum);
			tg[i].setTemporalParameter(par);
			tg[i].setFocusInterval(fint);
			tg[i].setTimeFocusFullExtent();
			tg[i].recalcAbsMinMax();
			tsummary[i] = new TimeGraphSummary();
			tsummary[i].setTable(dt);
			tsummary[i].setSupervisor(core.getSupervisor());
			tsummary[i].setAttribute(attrParentNum);
			tsummary[i].setTemporalParameter(par);

		}
		TimeGraphPanel tgp = new TimeGraphPanel(core.getSupervisor(), tg, tsummary);
		tgp.setName("time_graph");
		// tgp.setSupervisor(core.getSupervisor());
		core.getSupervisor().registerTool(tgp);
		core.getDisplayProducer().showGraph(tgp);
	}

	/**
	 * Returns the FocusInterval, which propagates the events of changing the
	 * current time moment, from the display time control panel. If the panel
	 * does not exist yet, creates it. This method is used in visualisation of
	 * time-series data.
	 * 
	 * @param par
	 *            - the temporal parameter
	 */
	public FocusInterval getFocusInterval(Parameter par) {
		if (displayTimeControlPanel != null)
			return displayTimeControlPanel.getFocusInterval();
		FocusInterval fint = new FocusInterval();
		TimeMoment t0 = ((TimeMoment) par.getValue(0)).getCopy();
		TimeMoment t1 = ((TimeMoment) par.getValue(par.getValueCount() - 1)).getCopy();
		fint.setDataInterval(t0, t1);
		t1 = (TimeMoment) par.getValue(1);
		long step = t1.subtract(t0);
		// check whether the step is constant
		for (int i = 2; i < par.getValueCount() && step > 1; i++) {
			t0 = t1;
			t1 = (TimeMoment) par.getValue(i);
			long step1 = t1.subtract(t0);
			if (step != step1) {
				step = 1;
			}
		}
		if (step < 1) {
			step = 1;
		}
		displayTimeControlPanel = new TimeControlPanel(fint, null, (int) step, false);
		displayTimeControlPanel.setSupervisor(core.getSupervisor());
		if (timeFilterControlPanel != null) {
			timeFilterControlPanel.setMaster(displayTimeControlPanel);
		}
		core.getSupervisor().registerTool(displayTimeControlPanel);
		core.getDisplayProducer().makeWindow(displayTimeControlPanel, displayTimeControlPanel.getName());
		// notifyPropertyChange("open_display_time_controls");
		return fint;
	}

	protected double[] runDBSCAN_Stepwise(int nclasses) {
		// new: parameters for dbscan stepwise
		double eps = 0.0, temp = 0.0;
		double spa_dist = 0.0;
		int entries = 0;
		int n = 3;
		Instances sInstances = was.getSubset();
		System.out.println("* Spatial temporal DBSCAN Clusterer: Start");
		StepwiseDBSCAN clus = new StepwiseDBSCAN();
		if (chClusterer.getSelectedItem().equals("DBSCAN (Stepwise)")) {
			n = Integer.parseInt(tfNNeighbour.getText());
			eps = Integer.parseInt(tfNSpatial.getText());
			temp = Integer.parseInt(tfNTemporal.getText());
			spa_dist = Integer.parseInt(tfNSpatialDis.getText());
			entries = Integer.parseInt(tfNEntries.getText());
		} else
			return null;
		clus.setParameters(eps, temp, n, spa_dist, entries);
		double[] res = null;
		double[] pars = null;
		try {
			clus.build(sInstances);
			res = clus.getAssignments();
			pars = clus.getParents();
			results = res;
			int l = 0;
			dbscanCluster = clus.numClusters();
			int parents = clus.getNumParents();
			if (parents <= 0)
				return null;

			// show results
			DataTable dTable = (DataTable) tbl;
			{
				int j = 0;
				String attrName = "Parent Cluster";
				do {
					j++;
					attrName = "Parent Cluster " + j;
				} while (dTable.findAttrByName(attrName) >= 0);

				dTable.addAttribute(attrName, null, AttributeTypes.character);

				attrIdx = dTable.getAttrCount() - 1;
				updateTable(dTable, parents, pars);

				// add clustering statistics
				int firstIdx = dTable.getAttrCount();
				Vector<String> attrNames = clus.getStatisticsNames();
				Vector<Character> attrTypes = clus.getStatisticsTypes();
				for (int i = 0; i < attrNames.size(); i++) {
					dTable.addAttribute(attrNames.get(i), attrTypes.get(i));
				}

				dTable.addAttribute("Cluster" + j, null, AttributeTypes.character);
				attrIdx = dTable.getAttrCount() - 1;
				updateTable(dTable, dbscanCluster, results);

				// add clustering statistics
				int secondIdx = dTable.getAttrCount();
				for (int i = 0; i < attrNames.size(); i++) {
					dTable.addAttribute("Sub " + attrNames.get(i), attrTypes.get(i));
				}

				// update table with statistics
				DataRecord rec = null;
				boundingBox parent = null;
				boundingBox child = null;
				for (int k = 0; k < results.length; k++) {
					rec = dTable.getDataRecord(k);
					parent = clus.getSpatialBounding(new Integer((int) pars[k]));
					child = clus.getSupBounding(new Integer((int) results[k]));
					for (int i = 0; i < parent.getStatisticsValues().size(); i++) {
						if (parent != null) {
							rec.setAttrValue(parent.getStatisticsValues().get(i), firstIdx + i);
						}
						if (child != null) {
							rec.setAttrValue(child.getStatisticsValues().get(i), secondIdx + i);
						}
					}
				}

				/*
				 * DataKeeper dk = core.getDataKeeper(); GeoLayer layer =
				 * dk.getTableLayer(dTable); if (layer != null) { Vector
				 * clusterAttr = new Vector(1, 1);
				 * clusterAttr.addElement(dTable.getAttributeId(attrIdx-1));
				 * DisplayProducer dpr = core.getDisplayProducer();
				 * dpr.displayOnMap("qualitative_colour", dTable, clusterAttr,
				 * layer, core.getUI().getMapViewer(0)); }
				 */
			}

			showStats(clus.getStats());

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("* Spatial temporal DBSCAN Clusterer: Finish");
		return res;
	}

	protected double[] runDBSCAN_Stepwise_Fast(int nclasses) {
		// new: parameters for dbscan stepwise
		double eps = 0.0, temp = 0.0;
		double spa_dist = 0.0;
		int entries = 0;
		int n = 3;
		Instances sInstances = was.getSubset();
		System.out.println("* Spatial temporal DBSCAN Clusterer: Start");
		StepwiseDBSCAN clus = new StepwiseDBSCAN();
		if (chClusterer.getSelectedItem().equals("DBSCAN (Stepwise)")) {
			n = Integer.parseInt(tfNNeighbour.getText());
			eps = Integer.parseInt(tfNSpatial.getText());
			temp = Integer.parseInt(tfNTemporal.getText());
			spa_dist = Integer.parseInt(tfNSpatialDis.getText());
			entries = Integer.parseInt(tfNEntries.getText());
		} else
			return null;
		clus.setParameters(eps, temp, n, spa_dist, entries);
		double[] res = null;
		double[] pars = null;
		try {
			clus.build2(sInstances);
			res = clus.getAssignments();
			pars = clus.getParents();
			results = res;
			int l = 0;
			dbscanCluster = clus.numClusters();
			int parents = clus.getNumParents();
			if (parents <= 0)
				return null;

			// show results
			DataTable dTable = (DataTable) tbl;
			{
				int j = 0;
				String attrName = "Parent Cluster";
				do {
					j++;
					attrName = "Parent Cluster " + j;
				} while (dTable.findAttrByName(attrName) >= 0);

				dTable.addAttribute(attrName, null, AttributeTypes.character);

				attrIdx = dTable.getAttrCount() - 1;
				updateTable(dTable, parents, pars);

				// add clustering statistics
				int firstIdx = dTable.getAttrCount();
				Vector<String> attrNames = clus.getStatisticsNames();
				Vector<Character> attrTypes = clus.getStatisticsTypes();
				for (int i = 0; i < attrNames.size(); i++) {
					dTable.addAttribute(attrNames.get(i), attrTypes.get(i));
				}

				dTable.addAttribute("Cluster" + j, null, AttributeTypes.character);
				attrIdx = dTable.getAttrCount() - 1;
				updateTable(dTable, dbscanCluster, results);

				// add clustering statistics
				int secondIdx = dTable.getAttrCount();
				for (int i = 0; i < attrNames.size(); i++) {
					dTable.addAttribute("Sub " + attrNames.get(i), attrTypes.get(i));
				}

				// update table with statistics
				DataRecord rec = null;
				boundingBox parent = null;
				boundingBox child = null;
				for (int k = 0; k < results.length; k++) {
					rec = dTable.getDataRecord(k);
					parent = clus.getSpatialBounding(new Integer((int) pars[k]));
					child = clus.getSupBounding(new Integer((int) results[k]));
					for (int i = 0; i < parent.getStatisticsValues().size(); i++) {
						if (parent != null) {
							rec.setAttrValue(parent.getStatisticsValues().get(i), firstIdx + i);
						}
						if (child != null) {
							rec.setAttrValue(child.getStatisticsValues().get(i), secondIdx + i);
						}
					}
				}

				/*
				 * DataKeeper dk = core.getDataKeeper(); GeoLayer layer =
				 * dk.getTableLayer(dTable); if (layer != null) { Vector
				 * clusterAttr = new Vector(1, 1);
				 * clusterAttr.addElement(dTable.getAttributeId(attrIdx-1));
				 * DisplayProducer dpr = core.getDisplayProducer();
				 * dpr.displayOnMap("qualitative_colour", dTable, clusterAttr,
				 * layer, core.getUI().getMapViewer(0)); }
				 */
			}

			showStats(clus.getStats());

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("* Spatial temporal DBSCAN Clusterer: Finish");
		return res;
	}

	protected double[] runSTDBSCAN(int nclasses) {
		double eps = 0.0;
		double temp = 0.0;
		int n = 3;
		Instances sInstances = was.getSubset();
		System.out.println("* DBScan Clusterer: Start");
		TOGDBSCAN clusterer = new TOGDBSCAN();
		ClusterEvaluation eval = new ClusterEvaluation();
		double[] res = null;
		try {
			if (chClusterer.getSelectedItem().equals("ST(I) DBSCAN")) {
				n = Integer.parseInt(tfNNeighbour.getText());
				eps = Integer.parseInt(tfNSpatial.getText());
				temp = Integer.parseInt(tfNTemporal.getText());
			} else
				return null;
			clusterer.setParameters(eps, temp, n);
			clusterer.setDatabase_distanceType("spade.analysis.tools.Event");
			clusterer.setDatabase_Type("spade.analysis.tools.SpatialTemporalIndexDatabase");
			clusterer.buildClusterer(sInstances);
			eval.setClusterer(clusterer);
			// eval.evaluateClusterer(sInstances);
			dbscanCluster = clusterer.numberOfClusters();
			res = new double[sInstances.numInstances()];
			// evaluation assigns noise as 0.0, but we need noise as -1.0
			for (int i = 0; i < sInstances.numInstances(); i++) {
				try {
					res[i] = clusterer.clusterInstance(sInstances.instance(i));
				} catch (Exception e) {
					// TODO: handle exception
					res[i] = -1.0;
				}
				// if (res[i]<=0.0) res[i] = -1.0;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("* EM Clusterer: Finish");
		return res;
	}

	protected double[] runEM(int nclasses) {
		Instances sInstances = was.getSubset();
		System.out.println("* EM Clusterer: Start");
		EM clusterer = new EM();
		ClusterEvaluation eval = new ClusterEvaluation();
		try {
			clusterer.setNumClusters(nclasses);
			clusterer.setSeed(10);
			clusterer.buildClusterer(sInstances);
			eval.setClusterer(clusterer);
			eval.evaluateClusterer(sInstances);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("* EM Clusterer: Finish");
		return eval.getClusterAssignments();
	}

	protected double[] runDBSCAN(int nclasses) {
		double eps = 0.0;
		int n = 3;
		Instances sInstances = was.getSubset();
		System.out.println("* DBScan Clusterer: Start");
		DBScan clusterer = new DBScan();
		ClusterEvaluation eval = new ClusterEvaluation();
		double[] res = null;
		try {
			if (chClusterer.getSelectedItem().equals("DBSCAN")) {
				n = Integer.parseInt(tfNNeighbour.getText());
				eps = Integer.parseInt(tfNSpatial.getText());
			} else
				return null;
			clusterer.setEpsilon(eps);
			clusterer.setMinPoints(n);
			clusterer.setDatabase_distanceType("spade.analysis.tools.GeoDataObject");
			clusterer.setDatabase_Type("spade.analysis.tools.SpatialIndexDatabase");
			clusterer.buildClusterer(sInstances);
			eval.setClusterer(clusterer);
			eval.evaluateClusterer(sInstances);
			dbscanCluster = clusterer.numberOfClusters();
			res = new double[sInstances.numInstances()];
			// evaluation assigns noise as 0.0, but we need noise as -1.0
			for (int i = 0; i < sInstances.numInstances(); i++) {
				try {
					res[i] = clusterer.clusterInstance(sInstances.instance(i));
				} catch (Exception e) {
					// TODO: handle exception
					res[i] = -1.0;
				}
				// if (res[i]<=0.0) res[i] = -1.0;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("* EM Clusterer: Finish");
		return res;
	}

	private void updateTable(DataTable table, int nclasses, double[] results) {
		spade.vis.database.Attribute cgattr = table.getAttribute(attrIdx);
		String valueList[] = new String[nclasses];
		Color valueColors[] = new Color[nclasses];
		for (int i = 0; i < nclasses; i++) {
			valueList[i] = "class " + (i + 1);
			if (nclasses < 10) {
				valueColors[i] = CS.getNiceColor(i); // scs.getColorForValue(k);
			} else {
				valueColors[i] = CS.getNiceColor(i % 9);
				int cn = i;
				if (i > 44) {
					valueColors[cn] = CS.getBWColor(i - 44, nclasses - 44);
				} else {
					while (cn > 9) {
						cn -= 9;
						valueColors[i] = valueColors[i].darker();
					}
				}
			}
		}
		cgattr.setValueListAndColors(valueList, valueColors);
		for (int i = 0; i < results.length; i++)
			if (results[i] >= 0 && results[i] < nclasses) {
				table.getDataRecord(tblRowsInInstances[i]).setAttrValue(valueList[(int) Math.round(results[i])], attrIdx);
			} else {
				table.getDataRecord(tblRowsInInstances[i]).setAttrValue(null, attrIdx);
			}
	}

}