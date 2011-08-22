package spade.analysis.tools.clustering;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.analysis.classification.Classifier;
import spade.analysis.classification.QualClassifierUI;
import spade.analysis.system.DataKeeper;
import spade.analysis.system.DisplayProducer;
import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.analysis.tools.DataAnalyser;
import spade.analysis.tools.distances.DistanceByAttributes;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.GetPathDlg;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.SelectDialog;
import spade.lib.color.CS;
import spade.lib.util.CopyFile;
import spade.lib.util.FloatArray;
import spade.lib.util.IntArray;
import spade.lib.util.NumValManager;
import spade.lib.util.StringUtil;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.ObjectFilter;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.geometry.Geometry;
import spade.vis.map.MapViewer;
import spade.vis.mapvis.ClassDrawer;
import spade.vis.mapvis.Visualizer;
import spade.vis.space.LayerManager;
import ui.AttributeChooser;
import core.ActionDescr;
import core.ResultDescr;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 19-Apr-2007
 * Time: 11:10:32
 * Provides an interface to the clustering method OPTICS.
 */
public class ClusterAnalysis implements DataAnalyser, PropertyChangeListener {

	protected ESDACore core = null;
	protected SystemUI ui = null;
	/**
	 * The new table with the core distances and reachability distances created
	 * by the clusterer. This table should be removed afterwards.
	 */
	protected DataTable distTable = null;
	/**
	 * Contains numbers of new columns with clustering results created during the
	 * exploration of the results of clustering
	 */
	protected IntArray newColNs = null;

	/**
	 * Returns true when the tool has everything necessary for its operation.
	 * For example, if the tool manages a number of analysis methods, it should
	 * check whether the class for at least one method is available.
	 */
	public boolean isValid(ESDACore core) {
		return true;
	}

	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	public void run(ESDACore core) {
		if (core == null || core.getUI() == null)
			return;
		this.core = core;
		ui = core.getUI();
		LayerManager lman = null;
		Vector layers = null;
		if (ui.getCurrentMapViewer() != null && ui.getCurrentMapViewer().getLayerManager() != null) {
			lman = core.getUI().getCurrentMapViewer().getLayerManager();
			//find appropriate instances of DGeoLayers (i.e. with points or trajectories)
			layers = new Vector(lman.getLayerCount(), 1);
			for (int i = 0; i < lman.getLayerCount(); i++)
				if (lman.getGeoLayer(i) instanceof DGeoLayer) {
					DGeoLayer layer = (DGeoLayer) lman.getGeoLayer(i);
					if (layer.getObjectCount() < 5) {
						continue;
					}
					if (layer.getType() == Geometry.point) {
						layers.addElement(layer);
					} else if (layer.getType() == Geometry.line && layer.getSubtype() == Geometry.movement) {
						layers.addElement(layer);
					}
				}
			if (layers.size() < 1) {
				layers = null;
			}
		}
		//find appropriate tables, i.e. with matrices of distances between the records
		DataKeeper dKeeper = core.getDataKeeper();
		Vector<DataTable> tables = new Vector<DataTable>(dKeeper.getTableCount(), 1);
		for (int i = 0; i < dKeeper.getTableCount(); i++)
			if (dKeeper.getTable(i) instanceof DataTable) {
				DataTable tbl = (DataTable) dKeeper.getTable(i);
				if (tbl.getDataItemCount() < 3) {
					continue;
				}
				if (tbl.getDistanceMatrix() != null) {
					tables.addElement(tbl);
				}
			}
		if (tables.size() < 1) {
			tables = null;
		}

		if (layers == null && tables == null) {
			showMessage("No appropriate layers or tables found!", true);
			return;
		}
		Panel p = new Panel(new ColumnLayout());
		CheckboxGroup cbg = null;
		Checkbox layerCB = null, tableCB = null;
		List layerList = null, tableList = null;
		if (layers != null && tables != null) {
			cbg = new CheckboxGroup();
			layerCB = new Checkbox("Select the layer to analyse:", true, cbg);
			tableCB = new Checkbox("Select the table to analyse:", false, cbg);
		}
		if (layers != null) {
			p.add((layerCB != null) ? layerCB : new Label("Select the layer to analyse:"));
			layerList = new List(layers.size() + 1);
			for (int i = 0; i < layers.size(); i++) {
				layerList.add(((DGeoLayer) layers.elementAt(i)).getName());
			}
			layerList.select(layers.size() - 1);
			p.add(layerList);
		}
		if (tables != null) {
			p.add((tableCB != null) ? tableCB : new Label("Select the table to analyse:"));
			tableList = new List(tables.size() + 1);
			for (int i = 0; i < tables.size(); i++) {
				tableList.add(tables.elementAt(i).getName());
			}
			tableList.select(tables.size() - 1);
			p.add(tableList);
		}
		OKDialog dia = new OKDialog(ui.getMainFrame(), "Cluster analysis", true);
		dia.addContent(p);
		dia.show();
		if (dia == null || dia.wasCancelled()) {
			dia = null;
			return;
		}
		if (layerList != null && (layerCB == null || layerCB.getState())) {
			int idx = layerList.getSelectedIndex();
			if (idx < 0)
				return;
			applyClusteringToLayer((DGeoLayer) layers.elementAt(idx));
		} else if (tableList != null) {
			int idx = tableList.getSelectedIndex();
			if (idx < 0)
				return;
			applyClusteringToTable(tables.elementAt(idx));
		}
	}

	protected void applyClusteringToLayer(DGeoLayer layer) {
		LayerClusterer cl = null;
		if (layer.getType() == Geometry.point) {
			cl = new PointLayerClusterer();
		} else if (layer.getType() == Geometry.line && layer.getSubtype() == Geometry.movement) {
			cl = new TrajectoryClusterer();
		}
		if (cl == null) {
			showMessage("Sorry... No clustering tools for this type of objects!", true);
			return;
		}
		cl.setLayer(layer);
		Vector distCompNames = cl.getDistanceComputerNames();
		if (distCompNames == null || distCompNames.size() < 1) {
			showMessage("Sorry... No distance functions found for this type of objects!", true);
			return;
		}
		SelectDialog selDia = new SelectDialog(core.getUI().getMainFrame(), "Distance function", "Choose a distance function:");
		for (int i = 0; i < distCompNames.size(); i++) {
			selDia.addOption((String) distCompNames.elementAt(i), String.valueOf(i + 1), i == 0);
		}
		selDia.addSeparator();
		selDia.addOption("Load pre-computed distances from a file", "matrix", false);
		Checkbox cbMakePlot = new Checkbox("create a reachability plot", false);
		selDia.addComponent(cbMakePlot);
		selDia.show();
		if (selDia.wasCancelled())
			return;
		boolean makePlot = cbMakePlot.getState();

		ActionDescr aDescr = new ActionDescr();
		aDescr.aName = "Clustering of geographical objects by OPTICS";
		aDescr.addParamValue("Layer id", layer.getContainerIdentifier());
		aDescr.addParamValue("Layer name", layer.getName());

		if (selDia.getSelectedOptionId().equals("matrix")) {
			cl = useDistancesFromFile(layer, aDescr);
			if (cl == null)
				return;
		} else { //no pre-computed distances exist
			int methodN = selDia.getSelectedOptionN();
			aDescr.addParamValue("Distance function name", distCompNames.elementAt(methodN));
			double wh[] = DGeoLayer.getExtentXY(layer);
			float width = (float) wh[0], height = (float) wh[1];
			float defRad = Math.min(width, height) / 200;
			String radStr = StringUtil.floatToStr(defRad, 0, defRad * 10);
			Panel p = new Panel(new ColumnLayout());
			p.add(new Label("Distance function: " + distCompNames.elementAt(methodN), Label.CENTER));
			p.add(new Label("Specify general clustering parameters:", Label.CENTER));
			Panel pp = new Panel(new BorderLayout());
			pp.add(new Label("Distance threshold?"), BorderLayout.WEST);
			TextField distTF = new TextField(radStr);
			pp.add(distTF, BorderLayout.CENTER);
			p.add(pp);
			pp = new Panel(new BorderLayout());
			pp.add(new Label("Minimum number of objects in the neighbourhood?"), BorderLayout.WEST);
			TextField nObjTF = new TextField("3");
			pp.add(nObjTF, BorderLayout.CENTER);
			p.add(pp);
			p.add(new Line(false));
			p.add(new Label("X-extent of the layer: " + StringUtil.floatToStr(width, 0, width)));
			p.add(new Label("Y-extent of the layer: " + StringUtil.floatToStr(height, 0, height)));
			p.add(new Line(false));
			Checkbox cbUseAttr = null;
			if (layer.getThematicData() != null && (layer.getThematicData() instanceof DataTable)) {
				cbUseAttr = new Checkbox("use also thematic attributes", false);
				p.add(cbUseAttr);
			}

			OKDialog dia = new OKDialog(ui.getMainFrame(), "Set parameters for OPTICS", true);
			dia.addContent(p);
			dia.show();
			if (dia.wasCancelled())
				return;
			double dist = 0f;
			int minObj = 3;
			String str = distTF.getText();
			if (str != null) {
				try {
					dist = Double.valueOf(str).doubleValue();
				} catch (NumberFormatException nfe) {
				}
			}
			if (dist <= 0) {
				dist = defRad;
			}
			str = nObjTF.getText();
			if (str != null) {
				try {
					minObj = Integer.valueOf(str).intValue();
				} catch (NumberFormatException nfe) {
				}
			}
			if (minObj < 2) {
				minObj = 2;
			}

			//cl.setLayer(layer);
			cl.setSystemCore(core);
			if (!cl.generateDistanceComputer(methodN))
				return;
			if (cbUseAttr != null && cbUseAttr.getState()) {
				DataTable table = (DataTable) layer.getThematicData();
				AttributeChooser attrChooser = new AttributeChooser();
				attrChooser.selectColumns(table, "Select attributes to use in clustering", core.getUI());
				Vector attr = attrChooser.getSelectedColumnIds();
				if (attr != null && attr.size() > 0) {
					DistanceByAttributes dAttr = new DistanceByAttributes();
					dAttr.setLayer(layer);
					dAttr.setTable(table);
					dAttr.setAttributes(attr);
					if (dAttr.askParameters()) {
						cl.setDistByAttrComputer(dAttr);
					}
				}
			}
			long t0 = System.currentTimeMillis();
			aDescr.startTime = t0;
			cl.doClustering(dist, minObj);
			long t1 = System.currentTimeMillis();
			aDescr.endTime = t1;
			System.out.println("Clustering took " + (t1 - t0) + " msec.");
		}
		HashMap params = cl.getParameters(null);
		if (params != null && !params.isEmpty()) {
			Set keys = params.keySet();
			if (keys != null) {
				for (Iterator it = keys.iterator(); it.hasNext();) {
					Object key = it.next(), value = params.get(key);
					if (key != null && value != null) {
						aDescr.addParamValue(key.toString(), value);
					}
				}
			}
		}
		core.logAction(aDescr);

		distTable = cl.getResult();
		if (distTable == null || !distTable.hasData()) {
			aDescr.addParamValue("Failed", new Boolean(true));
			showMessage("Clustering failed!", true);
			return;
		}
		Vector v = distTable.getKAttrValues(distTable.getAttributeId(0), 2);
		if (v == null || v.size() < 2) {
			aDescr.addParamValue("Failed", new Boolean(true));
			showMessage("No distances computed!", true);
			return;
		}
		showMessage("Clustering complete!", false);
		if (makePlot) {
			String name = "Clustering of " + layer.getName();
			/*
			    name=Dialogs.askForStringValue(core.getUI().getMainFrame(),
			        "Table name?",name,
			        "A table with clustering results is obtained and will be temporarily "+
			        "added to the tables of the application","Temporary table",false);
			*/
			distTable.setName(name);
			int tblN = core.getDataLoader().addTable(distTable);
			distTable.setEntitySetIdentifier(layer.getEntitySetIdentifier());
		}
		//core.getDataLoader().setLink(layer,tblN);
		String descr = "Clustered by OPTICS: distance threshold = " + cl.getDistanceThreshold() + "; minimum number of neighbours of a core object = " + cl.getMinNeighbours() + ".";
		String d1 = cl.getDescription();
		if (d1 != null) {
			descr += " " + d1;
		}
		//store clustering results in a table column
		String tblName = null;
		DataTable layerData = null;
		boolean newTable = false;
		if (layer.getThematicData() == null || !(layer.getThematicData() instanceof DataTable)) {
			tblName = layer.getName() + ": clustering results";
			newTable = true;
		} else {
			layerData = (DataTable) layer.getThematicData();
			tblName = layerData.getName();
		}
		String colName = Dialogs.askForStringValue(core.getUI().getMainFrame(), "Column name?", "Clusters (OPTICS; " + cl.getDistanceComputer().getMethodName() + "; " + cl.getDistanceThreshold() + "/" + cl.getMinNeighbours() + ")",
				"A new column will be created in the table " + tblName, "New column", true);
		if (colName == null)
			return;
		if (newTable) {
			layerData = new DataTable();
			layerData.setName(tblName);
			layerData.setEntitySetIdentifier(layer.getEntitySetIdentifier());
			ResultDescr res = new ResultDescr();
			res.product = layerData;
			aDescr.addResultDescr(res);
		}
		Attribute attr = new Attribute("_clusters_" + (layerData.getAttrCount() + 1), AttributeTypes.character);
		attr.setName(colName);
		layerData.addAttribute(attr);
		ResultDescr res = new ResultDescr();
		res.product = attr;
		res.owner = layerData;
		aDescr.addResultDescr(res);
		res.comment = "cluster numbers";
		int colIdx = layerData.getAttrCount() - 1;

		Vector clAttr = new Vector(1, 1);
		clAttr.addElement(layerData.getAttributeId(colIdx));
		layerData.notifyPropertyChange("new_attributes", null, clAttr);

		determineClusters(cl, layerData, colIdx);

		Visualizer vis = layer.getVisualizer(), bkvis = layer.getBackgroundVisualizer();
		boolean anotherClustering = (vis != null && (vis instanceof ClassDrawer)) || (bkvis != null && (bkvis instanceof ClassDrawer));
		boolean useMainView = !anotherClustering && ((vis == null && bkvis == null) || !Dialogs.askYesOrNo(ui.getMainFrame(), "Do you wish to view the results of clustering in a new map window?", "New map?"));
		MapViewer mw = ui.getMapViewer((useMainView) ? "main" : "_blank_");
		if (!mw.equals(ui.getCurrentMapViewer()) && mw.getLayerManager() != null) {
			//find the copy of the geographical layer in the new map view
			int lidx = mw.getLayerManager().getIndexOfLayer(layer.getContainerIdentifier());
			if (lidx >= 0) {
				layer = (DGeoLayer) mw.getLayerManager().getGeoLayer(lidx);
			} else {
				mw = ui.getMapViewer("main");
			}
		} else {
			mw = ui.getMapViewer("main");
		}
		DisplayProducer dpr = core.getDisplayProducer();
		vis = dpr.displayOnMap("qualitative_colour", layerData, clAttr, layer, mw);

		if (makePlot) {
			//Create a window with the reachability plot
			ClusterHistogramPanel clHist = new ClusterHistogramPanel(cl, layerData, colIdx, descr);
			clHist.setName(distTable.getName());
			clHist.setHighlighter(core.getHighlighterForSet(layer.getEntitySetIdentifier()));

			Classifier classifier = null;
			vis = layer.getVisualizer();
			if (vis != null && (vis instanceof ClassDrawer)) {
				classifier = ((ClassDrawer) vis).getClassifier();
			}
			if (classifier != null) {
				clHist.setClassifier(classifier);
			}
			clHist.setClusterRefiner(new ClassifierBuilder(core));
			clHist.addClusterChangeListener(this);
			core.getDisplayProducer().showGraph(clHist);
		}
	}

	/**
	 * Writes clustering results into a table column
	 */
	protected void determineClusters(Clusterer clusterer, DataTable objectData, int colN) {
		if (clusterer == null || objectData == null || colN < 0)
			return;
		double thr = clusterer.getDistanceThreshold();
		if (Double.isNaN(thr))
			return;
		Vector<DClusterObject> objectsOrdered = clusterer.getObjectsOrdered();
		if (objectsOrdered == null || objectsOrdered.size() < 1)
			return;
		int nObj = objectsOrdered.size();
		int objCl[] = new int[nObj];
		int nClusters = 0;
		for (int i = 0; i < nObj; i++) {
			objCl[i] = -1; //noise
			DClusterObject clObj = objectsOrdered.elementAt(i);
			if (!Double.isNaN(clObj.reachabilityDistance))
				if (clObj.reachabilityDistance <= thr)
					if (i == 0 || (i > 0 && objCl[i - 1] < 0)) { //new cluster
						++nClusters;
						objCl[i] = nClusters - 1;
					} else {
						objCl[i] = objCl[i - 1]; //continuation of the previous cluster
					}
				else {
					;
				}
			else if (i < nObj - 1 && !Double.isNaN(clObj.coreDistance) && clObj.coreDistance < thr) {
				DClusterObject next = objectsOrdered.elementAt(i + 1);
				if (!Double.isNaN(next.reachabilityDistance) && next.reachabilityDistance < thr) {
					//this is a center of a new cluster
					++nClusters;
					objCl[i] = nClusters - 1;
				}
			}
			clObj.clusterIdx = objCl[i];
		}
		Attribute attr = objectData.getAttribute(colN);
		String vals[] = new String[nClusters + 1];
		Color c[] = new Color[nClusters + 1];
		Color cc[] = getClusterColors(nClusters);
		for (int i = 0; i < nClusters; i++) {
			vals[i] = String.valueOf(i + 1);
			c[i] = cc[i];
		}
		vals[nClusters] = "noise";
		c[nClusters] = getColorForNoise();
		attr.setValueListAndColors(vals, c);
		LayerClusterer lClusterer = null;
		if (clusterer instanceof LayerClusterer) {
			lClusterer = (LayerClusterer) clusterer;
		}
		for (int i = 0; i < nObj; i++) {
			String id = objectsOrdered.elementAt(i).id;
			DataRecord rec = null;
			int idx = objectData.indexOf(id);
			if (idx >= 0) {
				rec = objectData.getDataRecord(idx);
			}
			if (rec == null && lClusterer != null) {
				DGeoObject gobj = lClusterer.getDGeoObject(objectsOrdered.elementAt(i));
				rec = new DataRecord(id, gobj.getLabel());
				objectData.addDataRecord(rec);
				gobj.setThematicData(rec);
			}
			int classN = objCl[i];
			if (classN < 0 || classN >= nClusters) {
				rec.setAttrValue(vals[nClusters], colN);
			} else {
				rec.setAttrValue(vals[classN], colN);
			}
		}
	}

	/**
	 * Assigns colors to clusters (this does not include the
	 * color for the noise).
	 */
	public Color[] getClusterColors(int nClusters) {
		Color clColors[] = new Color[nClusters];
		for (int i = 0; i < nClusters; i++) {
			if (i < CS.niceColors.length) {
				clColors[i] = CS.getNiceColor(i);
			} else if (i < CS.niceColors.length * 3) {
				clColors[i] = clColors[i - CS.niceColors.length].darker();
			} else {
				clColors[i] = Color.getHSBColor((float) Math.random(), (float) Math.max(Math.random(), 0.5), (float) Math.max(Math.random(), 0.5));
			}
		}
		return clColors;
	}

	/**
	 * Returns the color assigned to the noise
	 */
	public Color getColorForNoise() {
		return Color.gray.darker();
	}

	protected DistanceMatrixBasedClusterer useDistancesFromFile(DGeoLayer layer, ActionDescr aDescr) {
		//load the file with distances
		GetPathDlg fd = new GetPathDlg(core.getUI().getMainFrame(), "File with the distances?");
		fd.setFileMask("*.csv;*.txt");
		fd.show();
		String path = fd.getPath();
		if (path == null)
			return null;
		InputStream stream = openStream(path);
		if (stream == null)
			return null;
		aDescr.addParamValue("Distance matrix", path);
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		int nObj = layer.getObjectCount();
		float distMatrix[][] = new float[nObj][nObj];
		for (int i = 0; i < nObj; i++) {
			for (int j = 0; j < nObj; j++) {
				distMatrix[i][j] = Float.NaN;
			}
		}
		for (int i = 0; i < nObj; i++) {
			distMatrix[i][i] = 0;
		}
		int nExp = nObj * (nObj - 1) / 2;
		FloatArray distArray = new FloatArray(nExp, 100);
		boolean error = false;
		while (!error) {
			try {
				String s = reader.readLine();
				if (s == null) {
					break;
				}
				s = s.trim();
				if (s.length() < 1) {
					continue;
				}
				StringTokenizer st = new StringTokenizer(s, " ,;\t\r\n");
				if (st.countTokens() < 3) {
					continue;
				}
				int i1 = -1, i2 = -1;
				float d = Float.NaN;
				try {
					i1 = Integer.parseInt(st.nextToken()) - 1;
					i2 = Integer.parseInt(st.nextToken()) - 1;
					d = Float.parseFloat(st.nextToken());
				} catch (NumberFormatException nfe) {
					continue;
				}
				if (i1 >= 0 && i2 != 0 && i1 < nObj && i2 < nObj && !Float.isNaN(d) && d >= 0) {
					distMatrix[i1][i2] = distMatrix[i2][i1] = d;
					distArray.addElement(d);
				}
			} catch (EOFException eofe) {
				break;
			} catch (IOException ioe) {
				showMessage("Error reading data: " + ioe, true);
				error = true;
			}
		}
		try {
			stream.close();
		} catch (IOException e) {
		}
		if (error)
			return null;
		if (distArray.size() < 1) {
			showMessage("No distances loaded from " + path + "!", true);
			return null;
		}
		if (distArray.size() < 3) {
			showMessage("Only " + distArray.size() + " distances loaded from " + path + "!", true);
			return null;
		}
		showMessage("Got " + distArray.size() + " distances", false);
		if (distArray.size() < nExp) {
			String str = "Only " + distArray.size() + " out of the expected " + nExp + " distances have been loaded.\n" + "Would you like to proceed anyway?";
			if (!Dialogs.askYesOrNo(core.getUI().getMainFrame(), str, "Not all distances got!"))
				return null;
		}
		int perc[] = { 0, 5, 25, 50, 100 };
		float minMedMax[] = NumValManager.getPercentiles(distArray, perc);
		if (minMedMax[0] == minMedMax[4]) {
			showMessage("All distances are the same!", true);
			return null;
		}

		GridBagLayout gridbag = new GridBagLayout();
		Panel p = new Panel(gridbag);
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0f;
		c.weighty = 1.0f;
		c.fill = GridBagConstraints.HORIZONTAL;
		Label l = new Label("Minimum distance:");
		c.gridwidth = 2;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label(String.valueOf(minMedMax[0]));
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label("Maximum distance:");
		c.gridwidth = 2;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label(String.valueOf(minMedMax[4]));
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label("5% percentile distance:");
		c.gridwidth = 2;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label(String.valueOf(minMedMax[1]));
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label("1st quartile distance:");
		c.gridwidth = 2;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label(String.valueOf(minMedMax[2]));
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label("Median distance:");
		c.gridwidth = 2;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label(String.valueOf(minMedMax[3]));
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label("Specify clustering parameters:", Label.CENTER);
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label("Distance threshold?");
		c.gridwidth = 2;
		gridbag.setConstraints(l, c);
		p.add(l);
		String radStr = StringUtil.floatToStr(minMedMax[1], 0, minMedMax[1] * 10);
		TextField distTF = new TextField(radStr);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(distTF, c);
		p.add(distTF);
		l = new Label("Minimum number of objects?");
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField nObjTF = new TextField("3");
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(nObjTF, c);
		p.add(nObjTF);
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Set parameters for OPTICS", true);
		dia.addContent(p);
		dia.show();
		if (dia.wasCancelled())
			return null;
		String str = distTF.getText();
		double dist = 0;
		if (str != null) {
			try {
				dist = Double.valueOf(str).doubleValue();
			} catch (NumberFormatException nfe) {
			}
		}
		if (dist <= 0) {
			dist = minMedMax[1];
		}
		if (dist <= 0) {
			dist = minMedMax[2];
		}
		if (dist <= 0) {
			dist = minMedMax[3];
		}
		str = nObjTF.getText();
		int minObj = 2;
		if (str != null) {
			try {
				minObj = Integer.valueOf(str).intValue();
			} catch (NumberFormatException nfe) {
			}
		}
		if (minObj < 2) {
			minObj = 2;
		}

		DistanceMatrixBasedClusterer cl = new DistanceMatrixBasedClusterer();
		cl.setDistanceMatrix(distMatrix);
		cl.setMatrixName(CopyFile.getNameWithoutExt(path));
		cl.setLayer(layer);
		cl.setSystemCore(core);
		if (!cl.generateDistanceComputer(0))
			return null;
		aDescr.startTime = System.currentTimeMillis();
		cl.doClustering(dist, minObj);
		aDescr.endTime = System.currentTimeMillis();
		return cl;
	}

	protected void applyClusteringToTable(DataTable table) {
		if (table == null)
			return;
		if (table.getDistanceMatrix() == null) {
			showMessage("The table has no distance matrix!", true);
			return;
		}
		float distMatrix[][] = table.getDistanceMatrix();
		int nObj = table.getDataItemCount(), nObjTotal = nObj;
		if (distMatrix.length != nObj
				&& !Dialogs.askYesOrNo(ui.getMainFrame(), "The number of rows and columns in the distance matrix (" + distMatrix.length + ") differs from the number of table records (" + nObj + "). Continue anyway?", "Inconsistency!"))
			return;

		ObjectFilter filter = table.getObjectFilter();
		if (filter != null && filter.areObjectsFiltered()) {
			nObj = 0;
			for (int i = 0; i < nObjTotal; i++)
				if (filter.isActive(i)) {
					++nObj;
				}
			if (nObj < 4) {
				showMessage("Not enough active objects (" + nObj + ") after filtering!", true);
				return;
			}
		}
		int nExp = nObj * (nObj - 1) / 2;
		FloatArray distArray = new FloatArray(nExp, 100);
		for (int i = 0; i < nObjTotal - 1; i++)
			if (filter == null || filter.isActive(i)) {
				for (int j = i + 1; j < nObjTotal; j++)
					if (!Float.isNaN(distMatrix[i][j]) && (filter == null || filter.isActive(j))) {
						distArray.addElement(distMatrix[i][j]);
					}
			}
		if (distArray.size() < 2) {
			showMessage("No distances in the distance matrix!", true);
			return;
		}

		int perc[] = { 0, 5, 25, 50, 100 };
		float minMedMax[] = NumValManager.getPercentiles(distArray, perc);
		if (minMedMax[0] == minMedMax[4]) {
			showMessage("All distances are the same!", true);
			return;
		}

		GridBagLayout gridbag = new GridBagLayout();
		Panel p = new Panel(gridbag);
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0f;
		c.weighty = 1.0f;
		c.fill = GridBagConstraints.HORIZONTAL;
		Label l = new Label("Minimum distance:");
		c.gridwidth = 2;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label(String.valueOf(minMedMax[0]));
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label("Maximum distance:");
		c.gridwidth = 2;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label(String.valueOf(minMedMax[4]));
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label("5% percentile distance:");
		c.gridwidth = 2;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label(String.valueOf(minMedMax[1]));
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label("1st quartile distance:");
		c.gridwidth = 2;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label(String.valueOf(minMedMax[2]));
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label("Median distance:");
		c.gridwidth = 2;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label(String.valueOf(minMedMax[3]));
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label("Specify clustering parameters:", Label.CENTER);
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label("Distance threshold?");
		c.gridwidth = 2;
		gridbag.setConstraints(l, c);
		p.add(l);
		String radStr = StringUtil.floatToStr(minMedMax[1], 0, minMedMax[1] * 10);
		TextField distTF = new TextField(radStr);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(distTF, c);
		p.add(distTF);
		l = new Label("Minimum number of objects?");
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField nObjTF = new TextField("3");
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(nObjTF, c);
		p.add(nObjTF);
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Set parameters for OPTICS", true);
		dia.addContent(p);
		double dist = 0;
		do {
			dia.show();
			if (dia.wasCancelled())
				return;
			String str = distTF.getText();
			if (str != null) {
				try {
					dist = Double.valueOf(str).doubleValue();
				} catch (NumberFormatException nfe) {
				}
			}
			if (dist <= 0) {
				showMessage("Invalid distance threshold!", true);
			}
		} while (dist <= 0);
		String str = nObjTF.getText();
		int minObj = 2;
		if (str != null) {
			try {
				minObj = Integer.valueOf(str).intValue();
			} catch (NumberFormatException nfe) {
			}
		}
		if (minObj < 2) {
			minObj = 2;
		}

		ActionDescr aDescr = new ActionDescr();
		aDescr.aName = "Clustering of table records by OPTICS";
		aDescr.addParamValue("Table id", table.getContainerIdentifier());
		aDescr.addParamValue("Table name", table.getName());

		TableClusterer cl = new TableClusterer();
		cl.setTable(table);
		cl.setDistanceMatrix(distMatrix);
		cl.setSystemCore(core);
		cl.setDistanceThreshold(dist);
		cl.setMinNeighbours(minObj);

		aDescr.startTime = System.currentTimeMillis();
		cl.doClustering(dist, minObj);
		aDescr.endTime = System.currentTimeMillis();

		HashMap params = cl.getParameters(null);
		if (params != null && !params.isEmpty()) {
			Set keys = params.keySet();
			if (keys != null) {
				for (Iterator it = keys.iterator(); it.hasNext();) {
					Object key = it.next(), value = params.get(key);
					if (key != null && value != null) {
						aDescr.addParamValue(key.toString(), value);
					}
				}
			}
		}
		core.logAction(aDescr);

		distTable = cl.getResult();
		if (distTable == null || !distTable.hasData()) {
			aDescr.addParamValue("Failed", new Boolean(true));
			showMessage("Clustering failed!", true);
			return;
		}
		Vector v = distTable.getKAttrValues(distTable.getAttributeId(0), 2);
		if (v == null || v.size() < 2) {
			aDescr.addParamValue("Failed", new Boolean(true));
			showMessage("No distances computed!", true);
			return;
		}
		showMessage("Clustering complete!", false);
		String name = "Clustering of " + table.getName();
/*
    name=Dialogs.askForStringValue(core.getUI().getMainFrame(),
        "Table name?",name,
        "A table with clustering results is obtained and will be temporarily "+
        "added to the tables of the application","Temporary table",false);
*/
		distTable.setName(name);
		int tblN = core.getDataLoader().addTable(distTable);
		distTable.setEntitySetIdentifier(table.getEntitySetIdentifier());
		//core.getDataLoader().setLink(layer,tblN);
		String descr = "Clustered by OPTICS: distance threshold = " + cl.getDistanceThreshold() + "; minimum number of neighbours of a core object = " + cl.getMinNeighbours() + ".";
		String d1 = cl.getDescription();
		if (d1 != null) {
			descr += " " + d1;
		}
		//store the results in a new table column
		String colName = "Clusters (OPTICS; " + cl.getDistanceThreshold() + "/" + cl.getMinNeighbours() + ")";
/*
    colName=Dialogs.askForStringValue(core.getUI().getMainFrame(),
        "Column name?",colName,
        "A new column will be created in the table "+table.getName(),"New column",true);
    if (colName==null) return;
*/
		Attribute attr = new Attribute("_clusters_" + (table.getAttrCount() + 1), AttributeTypes.character);
		attr.setName(colName);
		table.addAttribute(attr);
		ResultDescr res = new ResultDescr();
		res.product = attr;
		res.owner = table;
		aDescr.addResultDescr(res);
		res.comment = "cluster numbers";
		int colIdx = table.getAttrCount() - 1;
		Vector clAttr = new Vector(1, 1);
		clAttr.addElement(table.getAttributeId(colIdx));

		//Create a window with the reachability plot
		ClusterHistogramPanel clHist = new ClusterHistogramPanel(cl, table, colIdx, descr);
		clHist.setName(distTable.getName());
		clHist.setHighlighter(core.getHighlighterForSet(table.getEntitySetIdentifier()));
		clHist.addClusterChangeListener(this);

		//if the table is attached to a layer, visualise the clusters by qualitative colouring
		LayerManager lman = null;
		if (ui.getCurrentMapViewer() != null) {
			lman = ui.getCurrentMapViewer().getLayerManager();
		}
		DGeoLayer layer = null;
		if (lman != null) {
			for (int i = 0; i < lman.getLayerCount() && layer == null; i++)
				if (lman.getGeoLayer(i) instanceof DGeoLayer) {
					DGeoLayer gl = (DGeoLayer) lman.getGeoLayer(i);
					if (gl.hasThematicData(table)) {
						layer = gl;
					}
				}
		}
		if (layer != null) {
			Visualizer vis = layer.getVisualizer(), bkvis = layer.getBackgroundVisualizer();
			boolean anotherClustering = (vis != null && (vis instanceof ClassDrawer)) || (bkvis != null && (bkvis instanceof ClassDrawer));
			boolean useMainView = !anotherClustering && ((vis == null && bkvis == null) || !Dialogs.askYesOrNo(ui.getMainFrame(), "Do you wish to view the results of clustering in a new map window?", "New map?"));
			MapViewer mw = ui.getMapViewer((useMainView) ? "main" : "_blank_");
			if (!mw.equals(ui.getCurrentMapViewer()) && mw.getLayerManager() != null) {
				//find the copy of the geographical layer in the new map view
				int lidx = mw.getLayerManager().getIndexOfLayer(layer.getContainerIdentifier());
				if (lidx >= 0) {
					layer = (DGeoLayer) mw.getLayerManager().getGeoLayer(lidx);
				} else {
					mw = ui.getMapViewer("main");
				}
			} else {
				mw = ui.getMapViewer("main");
			}
			DisplayProducer dpr = core.getDisplayProducer();
			vis = dpr.displayOnMap("qualitative_colour", table, clAttr, layer, mw);
			Classifier classifier = null;
			vis = layer.getVisualizer();
			if (vis != null && (vis instanceof ClassDrawer)) {
				classifier = ((ClassDrawer) vis).getClassifier();
			}
			if (classifier != null) {
				clHist.setClassifier(classifier);
			}
		} else {
			QualClassifierUI classUI = new QualClassifierUI(table, colIdx, core.getSupervisor());
			Classifier classifier = classUI.getClassifier();
			if (classifier != null) {
				clHist.setClassifier(classifier);
			}
			core.getDisplayProducer().showGraph(classUI);
		}

		core.getDisplayProducer().showGraph(clHist);
		table.notifyPropertyChange("new_attributes", null, clAttr);
	}

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
	}

	/**
	 * Reacts to cdestroying the ClusterHistogramPanel
	 */
	public void propertyChange(PropertyChangeEvent e) {
		if ((e.getSource() instanceof ClusterHistogramPanel) && (e.getPropertyName().equals("destroy") || e.getPropertyName().equals("destroyed"))) {
			core.getDisplayProducer().closeDestroyedTools();
			removeIntermediateTable();
		}
	}

	protected void removeIntermediateTable() {
		if (distTable != null) {
			core.removeTable(distTable.getContainerIdentifier());
			distTable = null;
		}
	}

	/**
	* Opens the stream on the earlier specified data source. If the source
	* string starts with HTTP or FILE, accesses the source as a URL, otherwise
	* opens it as a local file
	*/
	protected InputStream openStream(String path) {
		if (path == null)
			return null;
		int idx = path.indexOf(':');
		boolean isURL = false;
		if (idx > 0) {
			String pref = path.substring(0, idx);
			if (pref.equalsIgnoreCase("HTTP") || pref.equalsIgnoreCase("FILE")) {
				isURL = true;
			}
		}
		try {
			if (isURL) { //try to access the source as a URL
				showMessage("Trying to open the URL " + path, false);
				System.out.println("Trying to open the URL " + path);
				URL url = new URL(path);
				System.out.println("URL=" + url);
				URLConnection urlc = url.openConnection();
				return urlc.getInputStream();
			} else
				return new FileInputStream(path);
		} catch (IOException ioe) {
			showMessage("Error accessing " + path + ": " + ioe, true);
		} catch (Throwable thr) {
			showMessage("Error accessing " + path + ": " + thr.toString(), true);
		}
		return null;
	}
}
