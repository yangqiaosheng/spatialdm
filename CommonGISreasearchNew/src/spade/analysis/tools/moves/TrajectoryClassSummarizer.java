package spade.analysis.tools.moves;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.util.Vector;

import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.BaseAnalyser;
import spade.analysis.tools.clustering.ClusterImage;
import spade.analysis.tools.clustering.ClustersOverviewPanel;
import spade.lib.basicwin.Centimeter;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.lib.color.CS;
import spade.lib.util.BubbleSort;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.time.TimeReference;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.LayerData;
import spade.vis.database.ObjectFilterBySelection;
import spade.vis.database.SpatialEntity;
import spade.vis.dataview.ShowRecManager;
import spade.vis.dmap.DAggregateLinkLayer;
import spade.vis.dmap.DAggregateLinkObject;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DLayerManager;
import spade.vis.dmap.DLinkLayer;
import spade.vis.dmap.DLinkObject;
import spade.vis.dmap.DMovingObject;
import spade.vis.dmap.DOSMLayer;
import spade.vis.dmap.DrawingParameters;
import spade.vis.dmap.MapCanvas;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.ImageGeometry;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapContext;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.spec.DataSourceSpec;
import spade.vis.spec.LinkDataDescription;
import ui.SimpleMapView;
import core.ActionDescr;
import core.ResultDescr;
import data_load.DataManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Feb 20, 2009
 * Time: 2:07:19 PM
 * Generates summarized representation of classes or clusters of
 * trajectories.
 */
public class TrajectoryClassSummarizer extends BaseAnalyser implements spade.lib.util.Comparator {

	/**
	 * Parameters of the generalisation
	 */
	protected float angle = 0f, minDist = 0f, maxDist = 0f, clRadius = 0f;
	protected long stopTime = 0l;

	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	@Override
	public void run(ESDACore core) {
		if (core == null || core.getUI() == null)
			return;
		this.core = core;
		if (core.getUI().getCurrentMapViewer() == null || core.getUI().getCurrentMapViewer().getLayerManager() == null) {
			showMessage("No map exists!", true);
			return;
		}
		//Find instances of DGeoLayer containing trajectories
		LayerManager lman = core.getUI().getCurrentMapViewer().getLayerManager();
		Vector moveLayers = new Vector(lman.getLayerCount(), 1);
		boolean geo = false;
		float minx = Float.NaN, miny = Float.NaN, maxx = Float.NaN, maxy = Float.NaN;
		for (int i = 0; i < lman.getLayerCount(); i++) {
			GeoLayer layer = lman.getGeoLayer(i);
			if ((layer instanceof DGeoLayer) && layer.getObjectCount() > 0 && (layer.getObjectAt(0) instanceof DMovingObject) && layer.getThematicData() != null && (layer.getThematicData() instanceof DataTable)) {
				moveLayers.addElement(layer);
				geo = geo || layer.isGeographic();
				RealRectangle r = ((DGeoLayer) layer).getWholeLayerBounds();
				if (r == null) {
					r = ((DGeoLayer) layer).getCurrentLayerBounds();
				}
				if (r != null) {
					if (Float.isNaN(minx) || minx > r.rx1) {
						minx = r.rx1;
					}
					if (Float.isNaN(maxx) || maxx < r.rx2) {
						maxx = r.rx2;
					}
					if (Float.isNaN(miny) || miny > r.ry1) {
						miny = r.ry1;
					}
					if (Float.isNaN(maxy) || maxy < r.ry2) {
						maxy = r.ry2;
					}
				}
			}
		}
		if (moveLayers.size() < 1) {
			showMessage("No suitable layers with trajectories found!", true);
			return;
		}
		Panel mainP = new Panel(new ColumnLayout());
		mainP.add(new Label("Select the layer with trajectories to summarize:"));
		List list = new List(Math.max(moveLayers.size() + 1, 5));
		for (int i = 0; i < moveLayers.size(); i++) {
			list.add(((DGeoLayer) moveLayers.elementAt(i)).getName());
		}
		list.select(0);
		mainP.add(list);

		double wh[] = DGeoLayer.getExtentXY(minx, miny, maxx, maxy, geo);
		float width = (float) wh[0], height = (float) wh[1];
		float geoFactorX = 1f, geoFactorY = 1f;
		if (geo) {
			geoFactorX = width / (maxx - minx);
			geoFactorY = height / (maxy - miny);
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
		Panel pp = new Panel(new FlowLayout(FlowLayout.LEFT));
		pp.add(new Label("Scale:"));
		Centimeter cm = new Centimeter();
		pp.add(cm);
		float sc = core.getUI().getMapViewer(core.getUI().getCurrentMapN()).getMapDrawer().getMapContext().getPixelValue() * cm.getMinimumSize().width * ((geo) ? geoFactorX : ((DLayerManager) lman).user_factor);
		pp.add(new Label(StringUtil.floatToStr(sc, 2) + " " + ((geo) ? "m" : ((DLayerManager) lman).getUserUnit())));
		gridbag.setConstraints(pp, c);
		p.add(pp);
		mainP.add(p);
		Checkbox cbStartsEnds = new Checkbox("use only the start and end positions", false);
		mainP.add(cbStartsEnds);
		Checkbox cbIntersect = new Checkbox("intersect the trajectories with the areas", true);
		mainP.add(cbIntersect);

		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Summarize classes of trajectories", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return;
		int idx = list.getSelectedIndex();
		if (idx < 0)
			return;

		DGeoLayer moveLayer = (DGeoLayer) moveLayers.elementAt(idx);
		DataTable table = (DataTable) moveLayer.getThematicData();
		IntArray aIdxs = new IntArray(20, 10);
		for (int i = 0; i < table.getAttrCount(); i++) {
			Attribute at = table.getAttribute(i);
			if (at.isClassification() || at.getType() == AttributeTypes.character || at.getType() == AttributeTypes.logical) {
				aIdxs.addElement(i);
			}
		}
		if (aIdxs.size() < 1) {
			showMessage("The table has no columns defining trajectory classes or clusters!", true);
			return;
		}
		list = new List(Math.max(10, Math.min(aIdxs.size(), 3)));
		for (int i = 0; i < aIdxs.size(); i++) {
			list.add(table.getAttributeName(aIdxs.elementAt(i)));
		}
		list.select(aIdxs.size() - 1);
		mainP = new Panel(new BorderLayout());
		mainP.add(new Label("Select the table column defining the classes or clusters:"), BorderLayout.NORTH);
		mainP.add(list, BorderLayout.CENTER);
		dia = new OKDialog(core.getUI().getMainFrame(), "Select column with classes", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return;
		int k = list.getSelectedIndex();
		if (k < 0)
			return;
		int colN = aIdxs.elementAt(k);

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

		ActionDescr aDescr = new ActionDescr();
		aDescr.aName = "Summarise clusters (classes) of trajectories";
		aDescr.addParamValue("Layer id", moveLayer.getContainerIdentifier());
		aDescr.addParamValue("Layer name", moveLayer.getName());
		aDescr.addParamValue("Table id", table.getContainerIdentifier());
		aDescr.addParamValue("Table name", table.getName());
		aDescr.addParamValue("Attribute", table.getAttributeName(colN));
		aDescr.addParamValue("min angle of turn", new Float(angle));
		aDescr.addParamValue("min stop time", new Long(stopTime));
		aDescr.addParamValue("min distance between extracted points", new Float(minDist));
		aDescr.addParamValue("max distance between extracted points", new Float(maxDist));
		aDescr.addParamValue("cluster radius", new Float(clRadius));
		aDescr.addParamValue("use only start and end points", new Boolean(cbStartsEnds.getState()));
		aDescr.startTime = System.currentTimeMillis();
		core.logAction(aDescr);

		DAggregateLinkLayer sumTrLayer = summarizeClasses(moveLayer, table, colN, angle, stopTime, minDist, maxDist, clRadius, cbStartsEnds.getState(), cbIntersect.getState());
		aDescr.endTime = System.currentTimeMillis();
		if (sumTrLayer != null) {
			ResultDescr rd = new ResultDescr();
			rd.product = sumTrLayer;
			aDescr.addResultDescr(rd);
			sumTrLayer.setMadeByAction(aDescr);
			DataTable sumTable = (DataTable) sumTrLayer.getThematicData();
			rd = new ResultDescr();
			rd.product = sumTable;
			aDescr.addResultDescr(rd);
			sumTable.setMadeByAction(aDescr);

			int maxIW = Math.round(60f * core.getUI().getMainFrame().getToolkit().getScreenResolution() / 25.33f);
			Vector<ClusterImage> cImages = getClassImages(sumTrLayer, maxIW);
			if (cImages != null && cImages.size() > 0) {
				ClustersOverviewPanel imPanel = new ClustersOverviewPanel(table, colN, cImages, "Summarized classes of " + moveLayer.getName(), "Summarized representations of classes (" + table.getAttributeName(colN) + ") of " + moveLayer.getName(),
						core);
				ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_ALWAYS);
				ClusterImage cim = cImages.elementAt(0);
				int w = cim.image.getWidth(null), h = cim.image.getHeight(null);
				core.getDisplayProducer().makeWindow(imPanel, sumTrLayer.getName(), w * 3 + 30 + scp.getVScrollbarWidth(), h * 3 + 50);
			}
		}
	}

	/**
	 * @param moveLayer - the layer with the trajectories to summarize
	 * @param table - the table which specifies the classes of the trajectories
	 * @param colN - the index of the table column with the classes
	 * @param angle - min angle of turn considered as significant
	 * @param stopTime - min stop time considered as significant
	 * @param minDist - min distance to keep between extracted characteristic points of the trajectories
	 * @param maxDist - max distance to keep between extracted characteristic points of the trajectories
	 * @param clRadius - the desired radius of the point clusters
	 * @param useOnlyStartsEnds - whether to use only starts and ends
	 * @param findIntersections - whether to interpolate if points do not lie in ajacent areas
	 * @return layer with aggregated data
	 */
	public DAggregateLinkLayer summarizeClasses(DGeoLayer moveLayer, DataTable table, int colN, float angle, long stopTime, float minDist, float maxDist, float clRadius, boolean useOnlyStartsEnds, boolean findIntersections) {
		if (moveLayer == null || table == null || colN < 0)
			return null;
		IntArray iar = new IntArray(1, 1);
		iar.addElement(colN);
		Vector values = table.getAllValuesInColumnsAsStrings(iar);
		if (values == null || values.size() < 1) {
			showMessage("No values in the table column!", true);
			return null;
		}
		if (values.size() > 1) {
			int idx = StringUtil.indexOfStringInVectorIgnoreCase("noise", values);
			if (idx >= 0) {
				values.removeElementAt(idx);
			}
		}
		if (values.size() > 1) {
			BubbleSort.sort(values, this);
		}
		MovesAccumulator mAcc[] = new MovesAccumulator[values.size()];
		for (int i = 0; i < mAcc.length; i++) {
			mAcc[i] = new MovesAccumulator();
			mAcc[i].setGeo(moveLayer.isGeographic());
			mAcc[i].setMakeDynamicAggregates(false); //the places will not be dynamic aggregates!
			mAcc[i].setUseOnlyStartsEnds(useOnlyStartsEnds);
			mAcc[i].setFindIntersections(findIntersections);
			mAcc[i].cIdx = i;
			mAcc[i].cLabel = values.elementAt(i).toString();
		}
		for (int i = 0; i < moveLayer.getObjectCount(); i++) {
			DGeoObject gobj = moveLayer.getObject(i);
			if ((gobj instanceof DMovingObject) && gobj.getData() != null) {
				String val = gobj.getData().getAttrValueAsString(colN);
				if (val == null) {
					continue;
				}
				boolean found = false;
				for (int j = 0; j < mAcc.length && !found; j++)
					if (val.equals(mAcc[j].cLabel)) {
						found = true;
						mAcc[j].addOriginalTrajectory((DMovingObject) gobj);
					}
			}
		}

		RealRectangle r = moveLayer.getWholeLayerBounds();
		if (r == null) {
			r = moveLayer.getCurrentLayerBounds();
		}
		boolean geo = moveLayer.isGeographic();
		int nPlaces = 0, nLinks = 0;

		for (int i = 0; i < mAcc.length; i++) {
			mAcc[i].buildAreas(geo, angle, stopTime, minDist, maxDist, clRadius, useOnlyStartsEnds, r);
			if (!mAcc[i].isReady()) {
				continue;
			}
			//after the areas have been built, the accumulator must produce dynamic aggregates
			mAcc[i].setMakeDynamicAggregates(true);
			Vector<DMovingObject> traj = mAcc[i].getOrigTrajectories();
			if (traj != null) {
				for (int j = 0; j < traj.size(); j++) {
					mAcc[i].accumulate(traj.elementAt(j));
				}
				if (mAcc[i].sumPlaces != null) {
					nPlaces += mAcc[i].sumPlaces.size();
				}
				if (mAcc[i].aggLinks != null) {
					nLinks += mAcc[i].aggLinks.size();
				}
			}
		}

		Vector<DAggregateLinkObject> allAggLinks = new Vector<DAggregateLinkObject>(nLinks, 1);
		Vector<String> classLabels = new Vector(nLinks, 1);
		Vector<DGeoObject> allPlaces = new Vector<DGeoObject>(nPlaces, 1);
		for (MovesAccumulator element : mAcc)
			if (element.sumPlaces != null && element.aggLinks != null) {
				for (int j = 0; j < element.sumPlaces.size(); j++)
					if (element.sumPlaces.elementAt(j) != null) {
						allPlaces.addElement(element.sumPlaces.elementAt(j));
					}
				for (int j = 0; j < element.aggLinks.size(); j++) {
					allAggLinks.addElement(element.aggLinks.elementAt(j));
					classLabels.addElement(element.cLabel);
				}
			}

		return makeLayersAndTables(moveLayer, table, colN, allPlaces, allAggLinks, classLabels);
	}

	/**
	 * @param trLayer - the source layer with the trajectories
	 * @param placeObjects - previously generated DGeoObjects representing
	 *   generalized positions from the trajectories (areas)
	 * @param aggLinks - aggregated links connecting the generalized positions;
	 *   instances of DAggregateLinkObject
	 */
	protected DAggregateLinkLayer makeLayersAndTables(DGeoLayer trLayer, DataTable trTable, int trClassColN, Vector<DGeoObject> placeObjects, Vector<DAggregateLinkObject> aggLinks, Vector<String> classLabels) {
		String name = "Summarized moves from " + trLayer.getName() + " by " + trTable.getAttributeName(trClassColN) + " (" + angle + "/" + minDist + "/" + maxDist + "; r=" + clRadius + ")";
/*
    name= Dialogs.askForStringValue(core.getUI().getMainFrame(),"Name of the layer and table?",name,
      "A new map layer with summarized moves and a corresponding table will be created.",
      "New map layer",false);
*/

		DataLoader dataLoader = core.getDataLoader();
		DGeoLayer placeLayer = new DGeoLayer();
		placeLayer.setGeographic(trLayer.isGeographic());
		placeLayer.setType(Geometry.area);
		placeLayer.setName("Generalised positions from " + trLayer.getName());
		placeLayer.setGeoObjects(placeObjects, true);
		DrawingParameters dp = placeLayer.getDrawingParameters();
		if (dp == null) {
			dp = new DrawingParameters();
			placeLayer.setDrawingParameters(dp);
		}
		dp.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
		dp.fillContours = false;
		//dataLoader.addMapLayer(placeLayer,-1);

		//construct a table with thematic information about the aggregated moves
		DataTable aggTbl = new DataTable();
		aggTbl.setName(name);
		aggTbl.addAttribute("Class (cluster)", "_orig_class_", AttributeTypes.character);
		int clIdx = aggTbl.getAttrCount() - 1;
		aggTbl.addAttribute("Start ID", "startId", AttributeTypes.character);
		int startIdIdx = aggTbl.getAttrCount() - 1;
		aggTbl.addAttribute("End ID", "endId", AttributeTypes.character);
		int endIdIdx = aggTbl.getAttrCount() - 1;
		aggTbl.addAttribute("Direction", "direction", AttributeTypes.character);
		int dirIdx = aggTbl.getAttrCount() - 1;
		aggTbl.addAttribute("N of moves", "n_moves", AttributeTypes.integer);
		int nMovesIdx = aggTbl.getAttrCount() - 1;
		aggTbl.addAttribute("Min move duration", "min_dur", AttributeTypes.integer);
		int minDurIdx = aggTbl.getAttrCount() - 1;
		aggTbl.addAttribute("Max move duration", "max_dur", AttributeTypes.integer);
		int maxDurIdx = aggTbl.getAttrCount() - 1;
		aggTbl.addAttribute("N of different trajectories", "n_traj", AttributeTypes.integer);
		int nTrajIdx = aggTbl.getAttrCount() - 1;
		aggTbl.addAttribute("IDs of trajectories", "trIds", AttributeTypes.character);
		int trIdsIdx = aggTbl.getAttrCount() - 1;
		DataSourceSpec spec = new DataSourceSpec();
		spec.id = aggTbl.getContainerIdentifier();
		spec.name = aggTbl.getName();
		spec.toBuildMapLayer = true;
		spec.descriptors = new Vector(5, 5);
		LinkDataDescription aggldd = new LinkDataDescription();
		aggldd.layerRef = placeLayer.getContainerIdentifier();
		aggldd.souColIdx = startIdIdx;
		aggldd.destColIdx = endIdIdx;
		spec.descriptors.addElement(aggldd);
		aggTbl.setDataSource(spec);
		for (int i = 0; i < aggLinks.size(); i++) {
			DAggregateLinkObject lobj = aggLinks.elementAt(i);
			DataRecord rec = new DataRecord(lobj.getIdentifier());
			aggTbl.addDataRecord(rec);
			rec.setAttrValue(classLabels.elementAt(i), clIdx);
			rec.setAttrValue(lobj.startNode.getIdentifier(), startIdIdx);
			rec.setAttrValue(lobj.endNode.getIdentifier(), endIdIdx);
			rec.setAttrValue(lobj.getLinkDirection(), dirIdx);
			int nLinks = lobj.souLinks.size();
			rec.setNumericAttrValue(nLinks, String.valueOf(nLinks), nMovesIdx);
			Vector trIds = new Vector(nLinks, 1);
			String trIdsStr = "";
			long minDur = 0, maxDur = 0;
			for (int j = 0; j < nLinks; j++) {
				DLinkObject link = (DLinkObject) lobj.souLinks.elementAt(j);
				TimeReference tref = link.getTimeReference();
				if (tref != null && tref.getValidFrom() != null && tref.getValidUntil() != null) {
					long dur = tref.getValidUntil().subtract(tref.getValidFrom());
					if (dur > 0) {
						if (maxDur == 0) {
							minDur = dur;
							maxDur = dur;
						} else if (maxDur < dur) {
							maxDur = dur;
						} else if (minDur > dur) {
							minDur = dur;
						}
					}
				}
				String trId = (String) lobj.souTrajIds.elementAt(j);
				if (!trIds.contains(trId)) {
					trIds.addElement(trId);
					if (trIds.size() > 1) {
						trIdsStr += ";";
					}
					trIdsStr += trId;
				}
			}
			rec.setNumericAttrValue(minDur, String.valueOf(minDur), minDurIdx);
			rec.setNumericAttrValue(maxDur, String.valueOf(maxDur), maxDurIdx);
			rec.setNumericAttrValue(trIds.size(), String.valueOf(trIds.size()), nTrajIdx);
			rec.setAttrValue(trIdsStr, trIdsIdx);
			lobj.setThematicData(rec);
		}
		Attribute at = aggTbl.getAttribute(clIdx), at0 = trTable.getAttribute(trClassColN);
		if (at0.getValueColors() != null) {
			at.setValueListAndColors(at0.getValueList(), at0.getValueColors());
		}
		int aggTblN = dataLoader.addTable(aggTbl);

		DAggregateLinkLayer aggLinkLayer = new DAggregateLinkLayer();
		aggLinkLayer.setType(Geometry.line);
		aggLinkLayer.setName(name);
		aggLinkLayer.setGeographic(trLayer.isGeographic());
		aggLinkLayer.setGeoObjects(aggLinks, true);
		aggLinkLayer.setHasAllObjects(true);
		aggLinkLayer.setHasMovingObjects(true);
		aggLinkLayer.setTrajectoryLayer(trLayer);
		aggLinkLayer.setPlaceLayer(placeLayer);
		aggLinkLayer.setDataSource(spec);
		DrawingParameters dp1 = aggLinkLayer.getDrawingParameters();
		if (dp1 == null) {
			dp1 = new DrawingParameters();
			aggLinkLayer.setDrawingParameters(dp1);
		}
		dp1.lineColor = dp.lineColor;
		dp1.transparency = 40;
		spec.drawParm = dp1;
		dataLoader.addMapLayer(aggLinkLayer, -1);
		aggTbl.setEntitySetIdentifier(aggLinkLayer.getEntitySetIdentifier());
		dataLoader.setLink(aggLinkLayer, aggTblN);
		aggLinkLayer.setLinkedToTable(true);
		aggLinkLayer.countActiveLinks();

		ShowRecManager recMan = null;
		if (dataLoader instanceof DataManager) {
			recMan = ((DataManager) dataLoader).getShowRecManager(aggTblN);
		}
		if (recMan != null) {
			Vector showAttr = new Vector(aggTbl.getAttrCount(), 10);
			for (int i = 0; i < aggTbl.getAttrCount() - 1; i++)
				if (i != trIdsIdx) {
					showAttr.addElement(aggTbl.getAttributeId(i));
				}
			recMan.setPopupAddAttrs(showAttr);
		}
		return aggLinkLayer;
	}

	/**
	 * Compares two strings. First checks if the strings represent numbers.
	 * If so, compares the numbers.
	 * Returns 0 if the objects are equal, <0 if the first object is less than the
	 * second one, >0 otherwise
	 */
	@Override
	public int compare(Object obj1, Object obj2) {
		if (obj1 == null || obj2 == null || !(obj1 instanceof String) || !(obj2 instanceof String))
			return 0;
		String st1 = (String) obj1, st2 = (String) obj2;
		try {
			int i1 = Integer.parseInt(st1);
			int i2 = Integer.parseInt(st2);
			if (i1 < i2)
				return -1;
			if (i1 > i2)
				return 1;
			return 0;
		} catch (NumberFormatException e) {
		}
		return st1.compareTo(st2);
	}

	/**
	 * Creates small map images representing in a summarized form
	 * classes or clusters of trajectories
	 */
	public Vector<ClusterImage> getClassImages(DAggregateLinkLayer sumTrLayer, int maxImgWidth) {
		if (sumTrLayer == null || sumTrLayer.getObjectCount() < 2 || sumTrLayer.getTrajectoryLayer() == null || sumTrLayer.getThematicData() == null)
			return null;
		if (core == null || core.getUI() == null || core.getUI().getCurrentMapViewer() == null || !(core.getUI().getCurrentMapViewer() instanceof SimpleMapView))
			return null;
		SimpleMapView mw0 = (SimpleMapView) core.getUI().getCurrentMapViewer();
		if (mw0.getLayerManager() == null || !(mw0.getLayerManager() instanceof DLayerManager))
			return null;

		sumTrLayer.setHasAllObjects(true);
		RealRectangle bounds = sumTrLayer.getWholeLayerBounds();
		MapContext mc = mw0.getMapDrawer().getMapContext();
		Rectangle scrRect = mc.getScreenRectangle(bounds.rx1, bounds.ry1, bounds.rx2, bounds.ry2);
		int arW = sumTrLayer.getMaxThickness();
		if (arW <= 0) {
			arW = DAggregateLinkLayer.absMaxThickness;
		}
		int imgWidth = scrRect.width + arW * 2, imgHeight = scrRect.height + arW * 2;
		float ratio = 1f * imgWidth / maxImgWidth;
		double xScale = ((double) bounds.rx2 - bounds.rx1) / scrRect.width, yScale = ((double) bounds.ry2 - bounds.ry1) / scrRect.height;
		if (ratio > 1.1f) {
			imgWidth = Math.round(imgWidth / ratio);
			imgHeight = Math.round(imgHeight / ratio);
			xScale *= ratio;
			yScale *= ratio;
		}
		float dw = (float) xScale * arW, dh = (float) yScale * arW;
		bounds = new RealRectangle(bounds.rx1 - dw, bounds.ry1 - dh, bounds.rx2 + dw, bounds.ry2 + dh);

		DataTable table = (DataTable) sumTrLayer.getThematicData();
		int clColN = table.getAttrIndex("_orig_class_");
		if (clColN < 0)
			return null;
		int nMoveColN = table.getAttrIndex("n_moves");
		if (nMoveColN < 0)
			return null;
		IntArray iar = new IntArray(1, 1);
		iar.addElement(clColN);
		Vector cLabels = table.getAllValuesInColumnsAsStrings(iar);
		if (cLabels == null || cLabels.size() < 1) {
			showMessage("No classes or clusters defined in the table!", true);
			return null;
		}
		Color clColors[] = new Color[cLabels.size()];
		Attribute at = table.getAttribute(clColN);
		Color vColors[] = at.getValueColors();
		if (vColors != null) {
			String vals[] = at.getValueList();
			for (int i = 0; i < vals.length; i++) {
				int idx = cLabels.indexOf(vals[i]);
				if (idx >= 0) {
					clColors[idx] = vColors[i];
				}
			}
		}
		for (int i = 0; i < clColors.length; i++)
			if (clColors[i] == null)
				if (i < CS.niceColors.length) {
					clColors[i] = CS.getNiceColor(i);
				} else if (i < CS.niceColors.length * 3) {
					clColors[i] = clColors[i - CS.niceColors.length].darker();
				} else {
					clColors[i] = Color.getHSBColor((float) Math.random(), (float) Math.max(Math.random(), 0.5), (float) Math.max(Math.random(), 0.5));
				}

		DLayerManager lm0 = (DLayerManager) mw0.getLayerManager();
		boolean drawn[] = new boolean[lm0.getLayerCount()];
		boolean needRestore = false;
		for (int i = 0; i < lm0.getLayerCount(); i++) {
			DGeoLayer layer = lm0.getLayer(i);
			drawn[i] = layer.getLayerDrawn();
			if (drawn[i] && ((layer instanceof DLinkLayer) || (layer instanceof DAggregateLinkLayer) || layer.getSubtype() == Geometry.movement)) {
				layer.setLayerDrawn(false);
				needRestore = true;
			}
		}
		Image bkg = mw0.getMapBkgImage();
		if (needRestore) {
			for (int i = 0; i < lm0.getLayerCount(); i++) {
				DGeoLayer layer = lm0.getLayer(i);
				if (layer.getLayerDrawn() != drawn[i]) {
					layer.setLayerDrawn(true);
				}
			}
		}
		DLayerManager lm = (DLayerManager) lm0.makeCopy(false);
		if (bkg != null) {
			float b[] = mw0.getMapExtent();
			LayerData data = new LayerData();
			data.setBoundingRectangle(b[0], b[1], b[2], b[3]);
			ImageGeometry geom = new ImageGeometry();
			geom.img = bkg;
			geom.rx1 = b[0];
			geom.ry1 = b[1];
			geom.rx2 = b[2];
			geom.ry2 = b[3];
			//following text:"image"
			SpatialEntity spe = new SpatialEntity("image");
			spe.setGeometry(geom);
			data.addDataItem(spe);
			data.setHasAllData(true);
			DGeoLayer bkgLayer = new DGeoLayer();
			bkgLayer.receiveSpatialData(data);
			lm.addGeoLayer(bkgLayer);
		} else {
			for (int i = 0; i < lm0.getLayerCount(); i++) {
				DGeoLayer layer = lm0.getLayer(i);
				if (!layer.getLayerDrawn()) {
					continue;
				}
				if (layer instanceof DOSMLayer) {
					continue;
				}
				if (layer instanceof DLinkLayer) {
					continue;
				}
				if (layer instanceof DAggregateLinkLayer) {
					continue;
				}
				if (layer.getSubtype() == Geometry.movement) {
					continue;
				}
				String lId = layer.getContainerIdentifier();
				if (lId != null && (lId.equalsIgnoreCase(sumTrLayer.getContainerIdentifier()) || lId.equalsIgnoreCase(sumTrLayer.getTrajectoryLayer().getContainerIdentifier()))) {
					continue;
				}
				lm.addGeoLayer((DGeoLayer) layer.makeCopy());
			}
		}
		sumTrLayer = (DAggregateLinkLayer) sumTrLayer.makeCopy();
		sumTrLayer.setTrajectoryLayer(null);
		sumTrLayer.removeFilter();
		ObjectFilterBySelection clFilter = new ObjectFilterBySelection();
		clFilter.setObjectContainer(sumTrLayer);
		clFilter.setEntitySetIdentifier(sumTrLayer.getEntitySetIdentifier());
		sumTrLayer.setObjectFilter(clFilter);
		sumTrLayer.setThicknessColN(nMoveColN, true);
		DrawingParameters dp = sumTrLayer.getDrawingParameters();
		dp.transparency = 20;

		lm.addGeoLayer(sumTrLayer);
		lm.activateLayer(lm.getLayerCount() - 1);
		DOSMLayer osmLayer = lm.getOSMLayer();
		if (osmLayer != null) {
			osmLayer.setLayerDrawn(false);
		}

		MapCanvas map = new MapCanvas();
		map.setMapContent(lm);
		map.setPreferredSize(imgWidth, imgHeight);
		mc = map.getMapContext();
		mc.setVisibleTerritory(bounds);
		Frame mFrame = new Frame("Temporary");
		ColumnLayout cl = new ColumnLayout();
		cl.setAlignment(ColumnLayout.Hor_Left);
		mFrame.setLayout(cl);
		mFrame.add(map);
		mFrame.pack();
		mFrame.setVisible(true);
		map.setSize(imgWidth, imgHeight);

		Vector<ClusterImage> images = new Vector<ClusterImage>(cLabels.size(), 1);
		IntArray selMoves = new IntArray(500, 100);
		for (int i = 0; i < cLabels.size(); i++) {
			selMoves.removeAllElements();
			int maxNLinks = 0;
			String label = (String) cLabels.elementAt(i);
			for (int j = 0; j < sumTrLayer.getObjectCount(); j++) {
				DAggregateLinkObject lObj = (DAggregateLinkObject) sumTrLayer.getObject(j);
				DataRecord rec = (DataRecord) lObj.getData();
				if (rec == null) {
					continue;
				}
				if (label.equals(rec.getAttrValueAsString(clColN))) {
					selMoves.addElement(j);
					if (lObj.nActiveLinks > maxNLinks) {
						maxNLinks = lObj.nActiveLinks;
					}
					lObj.color = clColors[i];
				}
			}
			if (selMoves.size() < 1) {
				continue;
			}
			if (selMoves.size() < 10) {
				sumTrLayer.setMinShownNLinks(0);
			} else {
				sumTrLayer.setMinShownNLinks(Math.round(0.05f * maxNLinks));
			}
			clFilter.setActiveObjectIndexes(selMoves);
			map.invalidateImage();
			Image image = map.getMapAsImage();
			if (image != null) {
				ClusterImage cim = new ClusterImage();
				cim.clusterLabel = label;
				cim.image = image;
				images.addElement(cim);
			}
		}
		mFrame.dispose();
		if (images.size() < 1)
			return null;
		return images;
	}

}
