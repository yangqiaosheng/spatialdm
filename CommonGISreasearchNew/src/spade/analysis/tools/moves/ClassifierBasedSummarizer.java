package spade.analysis.tools.moves;

import java.awt.Checkbox;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.util.Vector;

import spade.analysis.system.DataLoader;
import spade.analysis.system.DisplayProducer;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.clustering.ClusterSpecimenInfo;
import spade.analysis.tools.clustering.ObjectsToClustersAssigner;
import spade.analysis.tools.clustering.SingleClusterInfo;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.Frequencies;
import spade.lib.util.GeoDistance;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.dataview.ShowRecManager;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DLinkLayer;
import spade.vis.dmap.DLinkObject;
import spade.vis.dmap.DMovingObject;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapViewer;
import spade.vis.mapvis.DataMapper;
import spade.vis.mapvis.Visualizer;
import spade.vis.spec.DataSourceSpec;
import spade.vis.spec.LinkDataDescription;
import data_load.DataManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Mar 12, 2009
 * Time: 4:14:01 PM
 * Used for summarisation of trajectories (in particular, coming one by one
 * from a database) on the basis of a classifier, which contains cluster
 * specimens (prototypes).
 */
public class ClassifierBasedSummarizer {
	protected ESDACore core = null;

	protected boolean geo = false;
	/**
	 * Each accumulator processes trajectories of a single cluster
	 */
	protected MovesAccumulator accumulators[] = null;
	/**
	 * The name of the source table with the trajectories
	 */
	public String tblName = null;
	/**
	 * Parameters of the generalisation
	 */
	public float angle = 0f, minDist = 0f, maxDist = 0f, clRadius = 0f;
	public long stopTime = 0l;

	/**
	 * @param classifier - the classifier on which this summarizer is base
	 * @param terrBounds - the boundary rectangle of the territory
	 * @param core - used for displaying messages, etc.
	 * @param tblName - the name of the source table with the trajectories
	 */
	public ClassifierBasedSummarizer(ObjectsToClustersAssigner classifier, RealRectangle terrBounds, ESDACore core, String tblName) {
		this.core = core;
		this.tblName = tblName;
		if (classifier == null || classifier.clustersInfo == null || classifier.clustersInfo.getClustersCount() < 1) {
			core.getUI().showMessage("No information about clusters in the classifier!", true);
			return;
		}
		if (classifier.clustersInfo.distanceMeter == null) {
			core.getUI().showMessage("No distance meter in the classifier!", true);
			return;
		}
		if (terrBounds == null) {
			core.getUI().showMessage("Undefined territory bounds!", true);
			return;
		}
		Object obj = classifier.getExampleObject();
		if (obj == null)
			return;
		if (!(obj instanceof DMovingObject)) {
			core.getUI().showMessage("The objects in the classifier are not trajectories!", true);
			return;
		}
		geo = classifier.clustersInfo.distanceMeter.isGeographic();

		float width, height, geoFactorX = 1f, geoFactorY = 1f;
		if (geo) {
			float my = (terrBounds.ry1 + terrBounds.ry2) / 2;
			width = (float) GeoDistance.geoDist(terrBounds.rx1, my, terrBounds.rx2, my);
			float mx = (terrBounds.rx1 + terrBounds.rx2) / 2;
			height = (float) GeoDistance.geoDist(mx, terrBounds.ry1, mx, terrBounds.ry2);
			geoFactorX = width / (terrBounds.rx2 - terrBounds.rx1);
			geoFactorY = height / (terrBounds.ry2 - terrBounds.ry1);
		} else {
			width = terrBounds.rx2 - terrBounds.rx1;
			height = terrBounds.ry2 - terrBounds.ry1;
		}
		float defMinDist = Math.min(width, height) / 200;
		float factor = 1;
		if (defMinDist > 1) {
			while (defMinDist >= 10) {
				factor *= 10;
				defMinDist /= 10;
			}
		} else {
			while (defMinDist < 1) {
				factor /= 10;
				defMinDist *= 10;
			}
		}
		if (defMinDist < 3) {
			defMinDist = 1;
		} else if (defMinDist < 7) {
			defMinDist = 5;
		} else {
			defMinDist = 10;
		}
		defMinDist *= factor;
		String minDistStr = StringUtil.floatToStr(defMinDist, 0, defMinDist * 10), maxDistStr = StringUtil.floatToStr(defMinDist * 5, 0, defMinDist * 10), clRadStr = StringUtil.floatToStr(defMinDist * 20, 0, defMinDist * 100);
		Panel p = new Panel();
		GridBagLayout gridbag = new GridBagLayout();
		p.setLayout(gridbag);
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0f;
		c.weighty = 1.0f;
		c.fill = GridBagConstraints.HORIZONTAL;
		Label l = new Label("Minimum angle of direction change (degrees):");
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField angleTF = new TextField("30", 10);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(angleTF, c);
		p.add(angleTF);
		l = new Label("Minimum duration of a stop:");
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField timeGapTF = new TextField("300", 10);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(timeGapTF, c);
		p.add(timeGapTF);
		l = new Label("Minimum radius around a position:");
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label("Minimum distance to next position:");
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField minDistTF = new TextField(minDistStr, 10);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(minDistTF, c);
		p.add(minDistTF);
		l = new Label("Maximum distance to next position:");
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField maxDistTF = new TextField(maxDistStr, 10);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(maxDistTF, c);
		p.add(maxDistTF);
		l = new Label("Desired radius of point clusters:");
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField clRadTF = new TextField(clRadStr, 10);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(clRadTF, c);
		p.add(clRadTF);
		l = new Label("X-extent:", Label.RIGHT);
		c.gridwidth = 1;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField ttf = new TextField(StringUtil.floatToStr(width, 0f, Math.max(width, height)), 10);
		ttf.setEditable(false);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(ttf, c);
		p.add(ttf);
		l = new Label("Y-extent:", Label.RIGHT);
		c.gridwidth = 1;
		gridbag.setConstraints(l, c);
		p.add(l);
		ttf = new TextField(StringUtil.floatToStr(height, 0f, Math.max(width, height)), 10);
		ttf.setEditable(false);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(ttf, c);
		p.add(ttf);
		Panel mainP = new Panel(new ColumnLayout());
		mainP.add(p);
		Checkbox cbStartsEnds = new Checkbox("use only the start and end positions", false);
		mainP.add(cbStartsEnds);
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Summarize classes of trajectories", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return;
		angle = 0f;
		String str = angleTF.getText();
		if (str != null) {
			try {
				angle = Float.valueOf(str).floatValue();
			} catch (NumberFormatException nfe) {
			}
		}
		if (angle < 10) {
			angle = 10;
		}
		stopTime = 0l;
		str = timeGapTF.getText();
		if (str != null) {
			try {
				stopTime = Long.valueOf(str).longValue();
			} catch (NumberFormatException nfe) {
			}
		}
		if (stopTime < 0) {
			stopTime = 0;
		}
		minDist = 0f;
		str = minDistTF.getText();
		if (str != null) {
			try {
				minDist = Float.valueOf(str).floatValue();
			} catch (NumberFormatException nfe) {
			}
		}
		maxDist = 0f;
		str = maxDistTF.getText();
		if (str != null) {
			try {
				maxDist = Float.valueOf(str).floatValue();
			} catch (NumberFormatException nfe) {
			}
		}
		clRadius = 0f;
		str = clRadTF.getText();
		if (str != null) {
			try {
				clRadius = Float.valueOf(str).floatValue();
			} catch (NumberFormatException nfe) {
			}
		}

		accumulators = new MovesAccumulator[classifier.clustersInfo.getClustersCount()];
		for (int i = 0; i < accumulators.length; i++) {
			accumulators[i] = null;
			SingleClusterInfo clIn = classifier.clustersInfo.getSingleClusterInfo(i);
			if (clIn == null || clIn.getSpecimensCount() < 1) {
				continue;
			}
			Vector<DMovingObject> proto = new Vector<DMovingObject>(clIn.getSpecimensCount(), 1);
			for (int j = 0; j < clIn.getSpecimensCount(); j++) {
				ClusterSpecimenInfo spec = clIn.getClusterSpecimenInfo(j);
				DGeoObject gobj = classifier.getDGeoObject(spec.specimen);
				if (gobj == null || !(gobj instanceof DMovingObject)) {
					continue;
				}
				proto.addElement((DMovingObject) gobj);
			}
			if (proto.size() < 1) {
				continue;
			}
			accumulators[i] = new MovesAccumulator();
			accumulators[i].cIdx = clIn.clusterN;
			accumulators[i].cLabel = clIn.clusterLabel;
			if (accumulators[i].cLabel == null) {
				accumulators[i].cLabel = String.valueOf(clIn.clusterN);
			}
			accumulators[i].buildAreas(proto, geo, angle, stopTime, minDist, maxDist, clRadius, cbStartsEnds.getState(), terrBounds);
			if (!accumulators[i].isReady()) {
				accumulators[i] = null;
				continue;
			}
		}
	}

	/**
	 * Returns true if at least one cluster of trajectories can be summarized
	 */
	public boolean isReady() {
		if (accumulators == null)
			return false;
		for (MovesAccumulator accumulator : accumulators)
			if (accumulator != null)
				return true;
		return false;
	}

	/**
	 * Returns the number of classes for which accumulators exist
	 */
	public int getNClasses() {
		if (accumulators == null)
			return 0;
		int n = 0;
		for (MovesAccumulator accumulator : accumulators)
			if (accumulator != null) {
				++n;
			}
		return n;
	}

	/**
	 * Processes the given trajectory belonging to the specified cluster or class
	 */
	public boolean processTrajectory(DMovingObject mobj, int clusterN) {
		if (accumulators == null)
			return false;
		for (MovesAccumulator accumulator : accumulators)
			if (accumulator != null && clusterN == accumulator.cIdx)
				return accumulator.accumulate(mobj);
		return false;
	}

	/**
	 * Builds a map layer and a corresponding table from the accumulated moves
	 */
	public DGeoLayer makeLayer() {
		if (!isReady())
			return null;

		Vector<DPlaceVisitsCounter> sumPlaces = new Vector<DPlaceVisitsCounter>(100, 100);
		Vector<DLinkObject> sumMoves = new Vector<DLinkObject>(100, 100);
		IntArray acIdxs = new IntArray(100, 100);

		int nClasses = 0;
		for (int i = 0; i < accumulators.length; i++)
			if (accumulators[i] != null && accumulators[i].sumPlaces != null && accumulators[i].sumMoves != null) {
				++nClasses;
				for (int j = 0; j < accumulators[i].sumPlaces.size(); j++)
					if (accumulators[i].sumPlaces.elementAt(j) != null && accumulators[i].sumPlaces.elementAt(j).hasVisitsOrCrosses()) {
						sumPlaces.addElement(accumulators[i].sumPlaces.elementAt(j));
					}
				for (int j = 0; j < accumulators[i].sumMoves.size(); j++) {
					sumMoves.addElement(accumulators[i].sumMoves.elementAt(j));
					acIdxs.addElement(i);
				}
			}
		if (sumPlaces.size() < 2 || sumMoves.size() < 1)
			return null;

		String name = "Summarized moves from " + tblName + " by " + nClasses + " classes (" + angle + "/" + minDist + "/" + maxDist + "; r=" + clRadius + ")";
/*
    name= Dialogs.askForStringValue(core.getUI().getMainFrame(),"Name of the layer and table?",name,
      "A new map layer with summarized moves and a corresponding table will be created.",
      "New map layer",false);
*/

		DataLoader dataLoader = core.getDataLoader();
		DGeoLayer placeLayer = new DGeoLayer();
		placeLayer.setGeographic(geo);
		placeLayer.setType(Geometry.area);
		placeLayer.setName("Generalised positions");
		placeLayer.setGeoObjects(sumPlaces, true);

		//construct a table with thematic information about the aggregated moves
		DataTable sumTbl = new DataTable();
		sumTbl.setName(name);
		sumTbl.addAttribute("Class (cluster)", "_orig_class_", AttributeTypes.character);
		int clIdx = sumTbl.getAttrCount() - 1;
		sumTbl.addAttribute("N of trajectories in the class", "n_traj", AttributeTypes.integer);
		int nTrajIdx = sumTbl.getAttrCount() - 1;
		sumTbl.addAttribute("N of moves", "n_moves", AttributeTypes.integer);
		int nMovesIdx = sumTbl.getAttrCount() - 1;
		sumTbl.addAttribute("Start ID", "startId", AttributeTypes.character);
		int startIdIdx = sumTbl.getAttrCount() - 1;
		sumTbl.addAttribute("End ID", "endId", AttributeTypes.character);
		int endIdIdx = sumTbl.getAttrCount() - 1;
		sumTbl.addAttribute("Direction", "direction", AttributeTypes.character);
		int dirIdx = sumTbl.getAttrCount() - 1;
		sumTbl.addAttribute("Length", "length", AttributeTypes.real);
		int lenIdx = sumTbl.getAttrCount() - 1;
		DataSourceSpec spec = new DataSourceSpec();
		spec.id = sumTbl.getContainerIdentifier();
		spec.name = sumTbl.getName();
		spec.toBuildMapLayer = true;
		spec.descriptors = new Vector(5, 5);
		LinkDataDescription aggldd = new LinkDataDescription();
		aggldd.layerRef = placeLayer.getContainerIdentifier();
		aggldd.souColIdx = startIdIdx;
		aggldd.destColIdx = endIdIdx;
		spec.descriptors.addElement(aggldd);
		sumTbl.setDataSource(spec);

		for (int i = 0; i < sumMoves.size(); i++) {
			DLinkObject lobj = sumMoves.elementAt(i);
			DataRecord rec = new DataRecord(lobj.getIdentifier());
			sumTbl.addDataRecord(rec);
			int aIdx = acIdxs.elementAt(i);
			rec.setAttrValue(accumulators[aIdx].cLabel, clIdx);
			rec.setNumericAttrValue(accumulators[aIdx].count, String.valueOf(accumulators[aIdx].count), nTrajIdx);
			rec.setAttrValue(lobj.getStartNode().getIdentifier(), startIdIdx);
			rec.setAttrValue(lobj.getEndNode().getIdentifier(), endIdIdx);
			rec.setAttrValue(lobj.getLinkDirection(), dirIdx);
			int nLinks = lobj.getNTimes();
			rec.setNumericAttrValue(nLinks, String.valueOf(nLinks), nMovesIdx);
			double length = lobj.getLength();
			rec.setNumericAttrValue(length, String.valueOf(length), lenIdx);
			lobj.setThematicData(rec);
		}

		int aggTblN = dataLoader.addTable(sumTbl);
		DLinkLayer sumMovesLayer = new DLinkLayer();
		sumMovesLayer.setType(Geometry.line);
		sumMovesLayer.setName(name);
		sumMovesLayer.setGeographic(geo);
		sumMovesLayer.setGeoObjects(sumMoves, true);
		sumMovesLayer.setHasMovingObjects(false);
		sumMovesLayer.setPlaceLayer(placeLayer);
		sumMovesLayer.setDataSource(spec);
		DrawingParameters dp1 = sumMovesLayer.getDrawingParameters();
		if (dp1 == null) {
			dp1 = new DrawingParameters();
			sumMovesLayer.setDrawingParameters(dp1);
		}
		dp1.lineColor = Color.red;
		dp1.transparency = 40;
		spec.drawParm = dp1;
		dataLoader.addMapLayer(sumMovesLayer, -1);
		sumTbl.setEntitySetIdentifier(sumMovesLayer.getEntitySetIdentifier());
		dataLoader.setLink(sumMovesLayer, aggTblN);
		sumMovesLayer.setLinkedToTable(true);

		ShowRecManager recMan = null;
		if (dataLoader instanceof DataManager) {
			recMan = ((DataManager) dataLoader).getShowRecManager(aggTblN);
		}
		if (recMan != null) {
			Vector showAttr = new Vector(sumTbl.getAttrCount(), 10);
			for (int i = 0; i < sumTbl.getAttrCount() - 1; i++) {
				showAttr.addElement(sumTbl.getAttributeId(i));
			}
			recMan.setPopupAddAttrs(showAttr);
		}

		Frequencies freq = new Frequencies();
		freq.itemsAreStrings = true;
		freq.init(accumulators.length, 10);
		for (MovesAccumulator accumulator : accumulators) {
			freq.addItem(accumulator.cLabel, (int) accumulator.count);
		}
		sumTbl.getAttribute(clIdx).setValueFrequencies(freq);

		DisplayProducer displayProducer = core.getDisplayProducer();
		if (displayProducer != null) {
			DataMapper dataMapper = null;
			if (displayProducer.getDataMapper() != null && (displayProducer.getDataMapper() instanceof DataMapper)) {
				dataMapper = (DataMapper) displayProducer.getDataMapper();
			}
			MapViewer mapView = core.getUI().getCurrentMapViewer();
			if (dataMapper != null && mapView != null) {
				Vector attr = new Vector(2, 1);
				attr.addElement(sumTbl.getAttributeId(clIdx));
				attr.addElement(sumTbl.getAttributeId(nMovesIdx));
				Object vis = dataMapper.constructVisualizer("line_thickness_color", Geometry.line);
				Visualizer visualizer = displayProducer.displayOnMap(vis, "line_thickness_color", sumTbl, attr, sumMovesLayer, mapView);
				core.getSupervisor().registerTool(visualizer);
			}
		}
		return sumMovesLayer;
	}

}
