package spade.analysis.tools.moves;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.util.Map;
import java.util.Vector;

import spade.analysis.geocomp.voronoi.VoronoiNew;
import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.BaseAnalyser;
import spade.analysis.tools.clustering.PointOrganizer;
import spade.analysis.tools.clustering.PointOrganizerSpatialIndex;
import spade.lib.basicwin.Centimeter;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.IdMaker;
import spade.lib.util.IntArray;
import spade.lib.util.ObjectWithMeasure;
import spade.lib.util.StringUtil;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.SpatialEntity;
import spade.vis.dataview.ShowRecManager;
import spade.vis.dmap.DAggregateLinkLayer;
import spade.vis.dmap.DAggregateLinkObject;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DLayerManager;
import spade.vis.dmap.DLinkObject;
import spade.vis.dmap.DMovingObject;
import spade.vis.dmap.DPlaceVisitsLayer;
import spade.vis.dmap.DPlaceVisitsObject;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.CircleCollector;
import spade.vis.geometry.GeoComp;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealCircle;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.geometry.RealRectangle;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.spec.DataSourceSpec;
import spade.vis.spec.LinkDataDescription;
import core.ActionDescr;
import core.ResultDescr;
import data_load.DataManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 10-Aug-2007
 * Time: 16:29:48
 * Generalises and aggregates trajectories.
 */
public class TrajectoriesGeneraliser extends BaseAnalyser implements spade.lib.util.Comparator {
	/**
	 * Generalized positions produced by the tool
	 */
	protected Vector<DPlaceVisitsObject> places = null;
	/**
	 * Aggregated links (moves) produced by the tool
	 */
	protected Vector<DAggregateLinkObject> aggMoves = null;
	/**
	 * The resulting layer with the generalized places
	 */
	protected DPlaceVisitsLayer placeLayer = null;
	/**
	 * The resulting layer with the aggregate moves
	 */
	protected DAggregateLinkLayer aggLinkLayer = null;
	/**
	 * Generalised trajectories (produced optionally)
	 */
	protected Vector<DMovingObject> genTr = null;

	public void setCore(ESDACore core) {
		this.core = core;
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
			if ((layer instanceof DGeoLayer) && layer.getObjectCount() > 0 && (layer.getObjectAt(0) instanceof DMovingObject)) {
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
			showMessage("No layers with trajectories found!", true);
			return;
		}
		Panel mainP = new Panel(new ColumnLayout());
		mainP.add(new Label("Select the layer with trajectories to summarize:"));
		List list = new List(Math.max(moveLayers.size() + 1, 5));
		for (int i = 0; i < moveLayers.size(); i++) {
			list.add(((DGeoLayer) moveLayers.elementAt(i)).getName());
		}
		list.select(list.getItemCount() - 1);
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
		CheckboxGroup areasTypeCBG = new CheckboxGroup();
		Checkbox cbCircles = new Checkbox("circles", false, areasTypeCBG);
		Checkbox cbVoronoi = new Checkbox("Voronoi polygons", true, areasTypeCBG);
		pp = new Panel(new FlowLayout(FlowLayout.LEFT, 2, 0));
		pp.add(new Label("Generalize positions as"));
		pp.add(cbCircles);
		pp.add(cbVoronoi);
		mainP.add(pp);
		Checkbox cbActive = new Checkbox("use only active (after filtering) trajectories", false);
		mainP.add(cbActive);
		Checkbox cbStartsEnds = new Checkbox("use only the start and end positions", false);
		mainP.add(cbStartsEnds);
		Checkbox cbIntersect = new Checkbox("intersect the trajectories with the areas", true);
		mainP.add(cbIntersect);
		Checkbox cbPointLayer = new Checkbox("make a map layer with the characteristic points", false);
		mainP.add(cbPointLayer);
		Checkbox cbGenTrLayer = new Checkbox("make a map layer with generalised trajectories", false);
		mainP.add(cbGenTrLayer);
		Checkbox noOptCB = new Checkbox("skip the optimization phase", false);
		mainP.add(noOptCB);

		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Generalise trajectories", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return;
		int idx = list.getSelectedIndex();
		if (idx < 0)
			return;
		float angle = 0f;
		String str = angleTF.getText();
		String parStr = str;
		if (str != null) {
			try {
				angle = Float.valueOf(str).floatValue();
			} catch (NumberFormatException nfe) {
			}
		}
		if (angle < 10) {
			angle = 10;
		}
		double angleRadian = angle * Math.PI / 180;
		long stopTime = 0l;
		str = timeGapTF.getText();
		parStr += "/" + str;
		if (str != null) {
			try {
				stopTime = Long.valueOf(str).longValue();
			} catch (NumberFormatException nfe) {
			}
		}
		if (stopTime < 0) {
			stopTime = 0;
		}
		float minDist = 0f;
		str = minDistTF.getText();
		parStr += "/" + str;
		if (str != null) {
			try {
				minDist = Float.valueOf(str).floatValue();
			} catch (NumberFormatException nfe) {
			}
		}
		float maxDist = 0f;
		str = maxDistTF.getText();
		parStr += "/" + str;
		if (str != null) {
			try {
				maxDist = Float.valueOf(str).floatValue();
			} catch (NumberFormatException nfe) {
			}
		}
		float clRadius = 0f;
		str = clRadTF.getText();
		if (str != null) {
			try {
				clRadius = Float.valueOf(str).floatValue();
			} catch (NumberFormatException nfe) {
			}
		}
		parStr += "; r=" + str;
		float minDistOrig = minDist, maxDistOrig = maxDist, clRadiusOrig = clRadius;
		minDist /= geoFactorX;
		maxDist /= geoFactorX;
		clRadius /= geoFactorX;
		float clRadiusY = clRadiusOrig / geoFactorY;
		boolean intersect = cbIntersect.getState();
		boolean useActive = cbActive.getState();
		boolean useOnlyStartsEnds = cbStartsEnds.getState();
		boolean makePointLayer = cbPointLayer.getState();
		boolean makeGenTrLayer = cbGenTrLayer.getState();
		DGeoLayer moveLayer = (DGeoLayer) moveLayers.elementAt(idx);

		ActionDescr aDescr = new ActionDescr();
		aDescr.aName = "Summarise trajectories";
		aDescr.addParamValue("Layer id", moveLayer.getContainerIdentifier());
		aDescr.addParamValue("Layer name", moveLayer.getName());
		aDescr.addParamValue("min angle of turn", new Float(angle));
		aDescr.addParamValue("min stop time", new Long(stopTime));
		aDescr.addParamValue("min distance between extracted points", new Float(minDistOrig));
		aDescr.addParamValue("max distance between extracted points", new Float(maxDistOrig));
		aDescr.addParamValue("cluster radius", new Float(clRadiusOrig));
		aDescr.addParamValue("type of areas", areasTypeCBG.getSelectedCheckbox().getLabel());
		aDescr.addParamValue("use only active trajectories", new Boolean(useActive));
		aDescr.addParamValue("use only start and end points", new Boolean(useOnlyStartsEnds));
		aDescr.startTime = System.currentTimeMillis();
		core.logAction(aDescr);

		PointOrganizerSpatialIndex pOrg = new PointOrganizerSpatialIndex();
		pOrg.setSpatialExtent(minx, miny, maxx, maxy);
/*
    pOrg.setMaxRad(maxRadOrig);
    pOrg.setGeo(geo);
*/
		//since the Voronoi polygons are built on the basis of Euclidean distances,
		//we should use Euclidean distances also in point clustering
		pOrg.setMaxRad(Math.min(clRadius, clRadiusY));
		pOrg.setGeo(false, 1, 1);
		long t0 = System.currentTimeMillis();
		int np = 0;
		if (useOnlyStartsEnds) {
			for (int i = 0; i < moveLayer.getObjectCount(); i++)
				if (!useActive || moveLayer.isObjectActive(i)) {
					DGeoObject gobj = moveLayer.getObject(i);
					if (gobj instanceof DMovingObject) {
						DMovingObject mobj = (DMovingObject) gobj;
						Vector track = mobj.getTrack();
						if (track == null || track.size() < 2) {
							continue;
						}
						pOrg.addPoint(((SpatialEntity) track.elementAt(0)).getCentre());
						++np;
						pOrg.addPoint(((SpatialEntity) track.elementAt(track.size() - 1)).getCentre());
						++np;
					}
				}
			if (np % 100 == 0) {
				showMessage(np + " points processed; " + pOrg.getGroupCount() + " groups built", false);
			}
		} else {
			//preliminary simplification: extraction of characteristic points
			Vector simpleTracks = new Vector(moveLayer.getObjectCount(), 1);
			showMessage("Extracting the characteristic points of the trajectories ...", false);
			for (int i = 0; i < moveLayer.getObjectCount(); i++)
				if (!useActive || moveLayer.isObjectActive(i)) {
					DGeoObject gobj = moveLayer.getObject(i);
					if (gobj instanceof DMovingObject) {
						DMovingObject mobj = (DMovingObject) gobj;
						Vector genTrack = TrUtil.getCharacteristicPoints(mobj.getTrack(), angleRadian, stopTime, minDistOrig, maxDistOrig, geoFactorX, geoFactorY);
						if (genTrack != null) {
							simpleTracks.addElement(genTrack);
							mobj.setGeneralisedTrack(genTrack);
						}
					}
				}
			long t = System.currentTimeMillis();
			showMessage("Extracting characteristic points took " + (t - t0) + " msec.", false);
			System.out.println("Extracting characteristic points took " + (t - t0) + " msec.");
			//Group the characteristic points by proximity
			t0 = System.currentTimeMillis();
			showMessage("Grouping the characteristic points by proximity ...", false);
			for (int i = 0; i < simpleTracks.size(); i++) {
				Vector genTrack = (Vector) simpleTracks.elementAt(i);
				if (genTrack != null) {
					for (int j = 0; j < genTrack.size(); j++) {
						pOrg.addPoint(((SpatialEntity) genTrack.elementAt(j)).getCentre());
						++np;
					}
				}
				if (np % 100 == 0) {
					showMessage(np + " points processed; " + pOrg.getGroupCount() + " groups built", false);
				}
			}
		}
		int nGroups = pOrg.getGroupCount();
		if (nGroups < 2) {
			showMessage("Failed to group the extracted points!", true);
			return;
		}
		pOrg.reDistributePoints();
		long t = System.currentTimeMillis();
		showMessage(nGroups + " point clusters obtained in " + (t - t0) + " msec.", false);
		System.out.println(nGroups + " point clusters obtained in " + (t - t0) + " msec.");
/*
    showMessage("Merging close groups...",false);
    t0=System.currentTimeMillis();
    pOrg.mergeCloseGroups();
    int nGroups1=pOrg.getGroupCount();
    t=System.currentTimeMillis();
    if (nGroups1==nGroups)  {
      showMessage("No groups have been merged; elapsed time "+(t-t0)+" msec.",false);
      System.out.println("No groups have been merged; elapsed time "+(t-t0)+" msec.");
    }
    else {
      showMessage((nGroups-nGroups1)+"x2 groups have been merged; elapsed time "+(t-t0)+" msec.",false);
      System.out.println((nGroups-nGroups1)+"x2 groups have been merged; elapsed time "+(t-t0)+" msec.");
      nGroups=nGroups1;
    }
*/
/**/
		if (!noOptCB.getState()) {
			showMessage("Optimizing the grouping...", false);
			t0 = System.currentTimeMillis();
			pOrg.optimizeGrouping();
			nGroups = pOrg.getGroupCount();
			t = System.currentTimeMillis();
			showMessage(nGroups + " groups after optimization; elapsed time " + (t - t0) + " msec.", false);
			System.out.println(nGroups + " groups after optimization; elapsed time " + (t - t0) + " msec.");
		}
/**/
		//end group the points
		boolean ok = false;
		if (cbCircles.getState()) {
			Vector circles = new Vector(nGroups, 10);
			for (int i = 0; i < nGroups; i++) {
				RealPoint centre = pOrg.getCentroid(i);
				//RealCircle cir=new RealCircle(centre.x,centre.y,(float)(pOrg.getRadius(i)/geoFactorX));
				RealCircle cir = new RealCircle(centre.x, centre.y, (float) pOrg.getRadius(i));
				if (cir.rad < minDist) {
					cir.rad = minDist;
				}
				circles.addElement(cir);
			}
			pOrg = null;
			ok = summarizeByCircles(moveLayer, circles, minDist, useActive, useOnlyStartsEnds, intersect, makeGenTrLayer);
		} else if (cbVoronoi.getState()) {
			/*
			showMessage("Extracting the medoids of the point clusters...",false);
			t0=System.currentTimeMillis();
			pOrg.recountMedoids();
			t=System.currentTimeMillis();
			showMessage("Extracting cluster medoids took "+(t-t0)+" msec.",false);
			System.out.println("Extracting cluster medoids took "+(t-t0)+" msec.");
			*/
			Vector<RealPoint> points = new Vector<RealPoint>(nGroups, 10);
			for (int i = 0; i < nGroups; i++) {
				points.addElement(pOrg.getCentroid(i));
			}

			//introducing additional points in empty areas and on the boundaries
			showMessage("Introducing additional points...", false);
			RealRectangle br = moveLayer.getCurrentLayerBounds();
			if (br != null) {
				minx = br.rx1;
				maxx = br.rx2;
				miny = br.ry1;
				maxy = br.ry2;
			}
			t0 = System.currentTimeMillis();
			float dy = 2 * clRadiusY, dx = 2 * clRadius, dx2 = dx / 2, dy2 = dy / 2;
			float y1 = miny - dy - dy2, y2 = maxy + dy + dy2;
			float x1 = minx - dx - dx2, x2 = maxx + dx + dx2;
			int k = 0;
			for (float y = y1; y <= y2 + dy2; y += dy) {
				float ddx = (k % 2 == 0) ? 0 : dx2;
				++k;
				for (float x = x1 + ddx; x <= x2 + dx2; x += dx)
					if (pOrg.isFarFromAll(x, y)) {
						points.addElement(new RealPoint(x, y));
					}
			}
			t = System.currentTimeMillis();
			showMessage("Introducing additional points took " + (t - t0) + " msec.", false);
			System.out.println("Introducing additional points took " + (t - t0) + " msec.");
			//end introducing additional points in empty areas and on the boundaries
			/**/
			if (makePointLayer) {
				makePointLayer(pOrg, geo, parStr);
			}
			pOrg = null;

			showMessage("Building Voronoi polygons; wait...", false);
			t0 = System.currentTimeMillis();
			VoronoiNew voronoi = new VoronoiNew(points);
			if (!voronoi.isValid()) {
				showMessage("Failed to triangulate!", true);
				return;
			}
			voronoi.setBuildNeighbourhoodMatrix(true);
			RealPolyline areas[] = voronoi.getPolygons(x1, y1, x2, y2);
			if (areas == null) {
				showMessage("Failed to build polygons!", true);
				return;
			}
			t = System.currentTimeMillis();
			int nPolygons = 0;
			for (RealPolyline area : areas)
				if (area != null) {
					++nPolygons;
				}
			showMessage("Got " + nPolygons + " Voronoi polygons; elapsed time = " + (t - t0) + " msec.", false);
			System.out.println("Got " + nPolygons + " Voronoi polygons; elapsed time = " + (t - t0) + " msec.");
			//exclude the neibourhood relations with the boarding polygons
			/*
			if (neiMatrix!=null)
			  for (k=0; k<areas.length; k++)
			    if (areas[k]!=null) {
			      float cnt[]=areas[k].getCentroid();
			      if (cnt[0]<minx || cnt[0]>maxx || cnt[1]<miny || cnt[1]>maxy) {
			        for (int n=0; n<areas.length; n++)
			          if (n!=k) {
			            neiMatrix[n][k]=false;
			            neiMatrix[k][n]=false;
			          }
			        //areas[k]=null;
			      }
			    }
			*/
			Map<Integer, Integer> neighbourMap = voronoi.getNeighbourhoodMap();
			ok = summarizeByPolygons(moveLayer, areas, neighbourMap, useActive, useOnlyStartsEnds, intersect, makeGenTrLayer);
		}
		if (ok) {
			makeLayersAndTables(moveLayer, parStr, places, aggMoves, useActive, useOnlyStartsEnds, intersect, aDescr);
		}
		aDescr.endTime = System.currentTimeMillis();
		if (genTr != null && genTr.size() > 0) {
			//make a layer with the generalised trajectories and the corresponding table
			String layerName = "Generalised " + moveLayer.getName() + " (" + parStr + ")";
			DMovingObject mobj = genTr.elementAt(0);
			DataTable dtTraj = null;
			if (mobj.getData() != null && (mobj.getData() instanceof DataRecord)) {
				AttributeDataPortion tblSrcTraj = moveLayer.getThematicData();
				int idx0 = 0;
				if (tblSrcTraj != null) {
					idx0 = tblSrcTraj.getAttrCount();
					String pref = "List of places", name = pref;
					for (int i = 2; tblSrcTraj.findAttrByName(name) >= 0; i++) {
						name = pref + " (" + i + ")";
					}
					tblSrcTraj.addAttribute(name, IdMaker.makeId(name, (DataTable) tblSrcTraj), AttributeTypes.character);
					pref = "N of visited places";
					name = pref;
					for (int i = 2; tblSrcTraj.findAttrByName(name) >= 0; i++) {
						name = pref + " (" + i + ")";
					}
					tblSrcTraj.addAttribute(name, IdMaker.makeId(name, (DataTable) tblSrcTraj), AttributeTypes.integer);
				}
				dtTraj = new DataTable();
				dtTraj.setName(layerName + ": general data");
				DataRecord rec = (DataRecord) mobj.getData();
				rec.setAttrValue(mobj.listOfVisitedAreas, idx0);
				rec.setNumericAttrValue(mobj.nVisitedAreas, String.valueOf(mobj.nVisitedAreas), idx0 + 1);
				DataRecord rec1 = rec.makeCopy(true, true);
				dtTraj.setAttrList(rec1.getAttrList());
				dtTraj.addDataRecord(rec1);
				mobj.setThematicData(rec1);
				for (int i = 1; i < genTr.size(); i++) {
					mobj = genTr.elementAt(i);
					rec = (DataRecord) mobj.getData();
					if (rec != null) {
						rec.setAttrValue(mobj.listOfVisitedAreas, idx0);
						rec.setNumericAttrValue(mobj.nVisitedAreas, String.valueOf(mobj.nVisitedAreas), idx0 + 1);
						rec1 = rec.makeCopy(false, true);
						dtTraj.addDataRecord(rec1);
						mobj.setThematicData(rec1);
					}
				}
			}
			DGeoLayer tLayer = new DGeoLayer();
			tLayer.setType(Geometry.line);
			tLayer.setName(layerName);
			tLayer.setGeoObjects(genTr, true);
			tLayer.setHasMovingObjects(true);
			tLayer.setGeographic(moveLayer.isGeographic());
			DrawingParameters dp = tLayer.getDrawingParameters();
			if (dp == null) {
				dp = new DrawingParameters();
				tLayer.setDrawingParameters(dp);
			}
			dp.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
			dp.lineWidth = 2;
			dp.transparency = 0;
			DataLoader dLoader = core.getDataLoader();
			dLoader.addMapLayer(tLayer, -1);
			dLoader.processTimeReferencedObjectSet(tLayer);
			if (dtTraj != null) {
				int trTblN = dLoader.addTable(dtTraj);
				tLayer.setDataTable(dtTraj);
				dLoader.setLink(tLayer, trTblN);
				tLayer.setLinkedToTable(true);
				tLayer.setThematicFilter(dtTraj.getObjectFilter());
				dLoader.processTimeReferencedObjectSet(dtTraj);
			}
		}
	}

	public static ObjectWithMeasure getShortestPath(RealPolyline areas[], Map<Integer, Integer> neighbourMap, int idx1, int idx2, boolean isGeographic) {
		if (areas == null || neighbourMap == null)
			return null;
		int nObj = areas.length;
		if (nObj < 1)
			return null;
		if (idx1 < 0 || idx2 < 0 || idx1 >= nObj || idx2 >= nObj)
			return null;
		if (idx1 == idx2) {
			int path[] = new int[1];
			path[0] = idx1;
			return new ObjectWithMeasure(path, 0);
		}
		double pathLen = 0;
		IntArray path = new IntArray(20, 10);
		path.addElement(idx1);
		boolean reached = false;
		RealPoint ptGoal = SpatialEntity.getCentre(areas[idx2]);
		while (!reached) {
			double minDist = Double.NaN;
			int minIdx = -1;
			RealPoint pt0 = SpatialEntity.getCentre(areas[idx1]);
			for (int i = 0; i < nObj; i++)
				if (i != idx1 && neighbourMap.get(idx1) == i && path.indexOf(i) < 0) {
					if (i == idx2) {
						minDist = 0;
						minIdx = i;
						break;
					}
					RealPoint pt = SpatialEntity.getCentre(areas[i]);
					double dist = GeoComp.distance(ptGoal.x, ptGoal.y, pt.x, pt.y, isGeographic) + GeoComp.distance(pt0.x, pt0.y, pt.x, pt.y, isGeographic);
					if (Double.isNaN(minDist) || dist < minDist) {
						minDist = dist;
						minIdx = i;
					}
				}
			if (minIdx < 0) {
				break;
			}
			path.addElement(minIdx);
			RealPoint pt = SpatialEntity.getCentre(areas[minIdx]);
			pathLen += GeoComp.distance(pt0.x, pt0.y, pt.x, pt.y, isGeographic);
			idx1 = minIdx;
			reached = minIdx == idx2;
		}
		if (!reached)
			return null; //the destination is unreachable
		return new ObjectWithMeasure(path.getTrimmedArray(), pathLen);
	}

	public boolean summarizeByPolygons(DGeoLayer moveLayer, RealPolyline areas[], Map<Integer, Integer> neighbourMap, boolean onlyActiveTrajectories, boolean onlyStartsEnds, boolean findIntersections, boolean makeGenTrLayer) {
		if (moveLayer == null || areas == null || areas.length < 2)
			return false;
		boolean geo = moveLayer.isGeographic();
		//construct aggregated link objects
		//in parallel, collect information about the generalised places
		Vector aggLinks = new Vector(areas.length * 5, 100);
		Vector<DPlaceVisitsObject> pvObjects = new Vector<DPlaceVisitsObject>(areas.length, 10);
		int pvIdxs[] = new int[areas.length];
		int nAr = areas.length, nDigits = 0;
		do {
			nAr /= 10;
			++nDigits;
		} while (nAr > 1);
		for (int i = 0; i < areas.length; i++)
			if (areas[i] != null) {
				String id = StringUtil.padString(String.valueOf(i + 1), '0', nDigits, true);
				SpatialEntity spe = new SpatialEntity(id, id);
				spe.setGeometry(areas[i]);
				DPlaceVisitsObject obj = new DPlaceVisitsObject();
				obj.setup(spe);
				obj.setLabel(id);
				pvObjects.addElement(obj);
				pvIdxs[i] = pvObjects.size() - 1;
			} else {
				pvIdxs[i] = -1;
			}
		//adding information about the neighbours of the places
		if (neighbourMap != null) {
			for (int i = 0; i < areas.length; i++)
				if (pvIdxs[i] >= 0) {
					DGeoObject pObj = pvObjects.elementAt(pvIdxs[i]);
					for (int j = 0; j < areas.length; j++)
						if (j != i && neighbourMap.get(i) == j && pvIdxs[j] >= 0) {
							pObj.addNeighbour(pvObjects.elementAt(pvIdxs[j]));
						}
				}
		}
		IntArray placeIdxs = new IntArray(100, 50);
		Vector enterTimes = new Vector(100, 50), exitTimes = new Vector(100, 50);
		if (makeGenTrLayer) {
			genTr = new Vector<DMovingObject>(moveLayer.getObjectCount(), 1);
		}
		for (int i = 0; i < moveLayer.getObjectCount(); i++) {
			if (onlyActiveTrajectories && !moveLayer.isObjectActive(i)) {
				continue;
			}
			DGeoObject gobj = moveLayer.getObject(i);
			if (!(gobj instanceof DMovingObject)) {
				continue;
			}
			DMovingObject mobj = (DMovingObject) gobj;
			String id = mobj.getIdentifier();
			Vector track = mobj.getTrack();
			if (track == null || track.size() < 2) {
				continue;
			}
			if (track.size() > 2)
				if (onlyStartsEnds) {
					Vector t = new Vector(2, 1);
					t.addElement(track.elementAt(0));
					t.addElement(track.elementAt(track.size() - 1));
					track = t;
				}
			placeIdxs.removeAllElements();
			enterTimes.removeAllElements();
			exitTimes.removeAllElements();
			int j = 0;
			int lastAreaIdx = -1;
			while (j < track.size()) {
				SpatialEntity spe = (SpatialEntity) track.elementAt(j);
				RealPoint pt = spe.getCentre();
				int aIdx = -1;
				if (/*onlyStartsEnds ||*/lastAreaIdx < 0 || j == 0 || neighbourMap == null) {
					aIdx = findAreaContainingPosition(areas, pt.x, pt.y);
				} else {
					for (int k = 0; k < areas.length && aIdx < 0; k++)
						if (k != lastAreaIdx && areas[k] != null && neighbourMap.get(lastAreaIdx) == k)
							if (areas[k].contains(pt.x, pt.y, 0, true)) {
								aIdx = k;
							}
					if (aIdx < 0 && findIntersections) {
						aIdx = findAreaContainingPosition(areas, pt.x, pt.y);
						ObjectWithMeasure om = getShortestPath(areas, neighbourMap, lastAreaIdx, aIdx, geo);
						if (om != null) {
							int path[] = (int[]) om.obj;
							int np = path.length - 1;
							SpatialEntity spe0 = (SpatialEntity) track.elementAt(j - 1);
							TimeMoment t0 = spe0.getTimeReference().getValidUntil();
							TimeMoment t1 = spe.getTimeReference().getValidFrom();
							long timeDiff = t1.subtract(t0), step = timeDiff / np;
							for (int pi = 1; pi < path.length - 1; pi++) {
								TimeMoment t = t0.getCopy();
								t.add(step);
								int pvIdx = pvIdxs[path[pi]];
								DPlaceVisitsObject place = pvObjects.elementAt(pvIdx);
								place.addCross(id, track, j - 1, t, geo);
								placeIdxs.addElement(pvIdx);
								enterTimes.addElement(t);
								exitTimes.addElement(t);
								t0 = t;
							}
						}
					}
/*
          if (aIdx<0 && findIntersections) {
            SpatialEntity spe0=(SpatialEntity)track.elementAt(j-1);
            RealPoint pt0=spe0.getCentre();
            TimeMoment t0=spe0.getTimeReference().getValidUntil();
            TimeMoment t1 =spe.getTimeReference().getValidFrom();
            IntArray visited=new IntArray(10,10);
            visited.addElement(lastAreaIdx);
            do {
              //find the neibouring area intersected by the trajectory segment
              double minDist=Double.NaN;
              int neiIdx=-1;
              for (int k=0; k<areas.length; k++)
                if (k!=lastAreaIdx && areas[k]!=null &&
                    neiMatrix[lastAreaIdx][k] && visited.indexOf(k)<0) {
                  RealPoint inter[]=Computing.findIntersections(pt0,pt,areas[k]);
                  if (inter==null) continue;
                  for (int n=0; n<inter.length; n++) {
                    double d=GeoComp.distance(pt0.x, pt0.y, inter[n].x, inter[n].y, geo);
                    if (Double.isNaN(minDist) || minDist>d) {
                      minDist=d; neiIdx=k;
                    }
                  }
                }
              if (neiIdx<0) break; //seems impossible...
              float c[]=areas[neiIdx].getCentroid();
              RealPoint pCross=Computing.closestPoint(pt.x,pt.y,pt0.x,pt0.y,c[0],c[1]);
              double rest=GeoComp.distance(pt.x,pt.y,pCross.x,pCross.y,geo);
              double distRatio=minDist/(minDist+rest);
              long timeDiff=t1.subtract(t0);
              TimeMoment t=t0.getCopy();
              t.add(Math.round(distRatio*timeDiff));
              int pvIdx=pvIdxs[neiIdx];
              DPlaceVisitsObject place=pvObjects.elementAt(pvIdx);
              place.addCross(id,track,j-1,t,geo);
              placeIdxs.addElement(pvIdx);
              enterTimes.addElement(t); exitTimes.addElement(t);
              lastAreaIdx=neiIdx;
              visited.addElement(lastAreaIdx);
              pt0=pCross;
              t0=t;
              for (int k=0; k<areas.length && aIdx<0; k++)
                if (k!=lastAreaIdx && areas[k]!=null &&
                    neiMatrix[lastAreaIdx][k] && visited.indexOf(k)<0)
                  if (areas[k].contains(pt.x,pt.y,0,true))
                    aIdx=k;
            } while (aIdx<0);
          }
*/
					if (aIdx < 0) {
						aIdx = findAreaContainingPosition(areas, pt.x, pt.y);
					}
				}
				if (aIdx < 0) {
					++j;
					continue;
				}
				lastAreaIdx = aIdx;
				int pvIdx = pvIdxs[aIdx];
				DPlaceVisitsObject place = (DPlaceVisitsObject) pvObjects.elementAt(pvIdx);
				int exitIdx = place.addVisit(id, track, j, geo);
				TimeMoment t0 = null, t1 = null;
				spe = (SpatialEntity) track.elementAt(j);
				if (spe.getTimeReference() != null) {
					t0 = spe.getTimeReference().getValidFrom();
				}
				spe = (SpatialEntity) track.elementAt(exitIdx);
				if (spe.getTimeReference() != null) {
					t1 = spe.getTimeReference().getValidUntil();
					if (t1 == null) {
						t1 = spe.getTimeReference().getValidFrom();
					}
				}
				/*
				float c[]=areas[aIdx].getCentroid();
				RealPoint pt1=spe.getCentre();
				if (exitIdx>=track.size()-1 ||
				    Computing.distance(pt.x,pt.y,pt1.x,pt1.y, c[0],c[1],geo)<maxRad/2) {
				  placeIdxs.addElement(pvIdx);
				  enterTimes.addElement(t0); exitTimes.addElement(t1);
				}
				*/
				placeIdxs.addElement(pvIdx);
				enterTimes.addElement(t0);
				exitTimes.addElement(t1);
				j = exitIdx + 1;
			}
			if (placeIdxs.size() < 1) {
				continue;
			}
			if (placeIdxs.size() < 2) {
				DPlaceVisitsObject place = pvObjects.elementAt(placeIdxs.elementAt(0));
				TimeMoment t0 = (TimeMoment) enterTimes.elementAt(0), t1 = (TimeMoment) exitTimes.elementAt(0);
				DLinkObject link = new DLinkObject();
				link.setup(place, place, t0, t1);
				DAggregateLinkObject aggLink = null;
				for (int k = 0; k < aggLinks.size() && aggLink == null; k++) {
					aggLink = (DAggregateLinkObject) aggLinks.elementAt(k);
					if (!aggLink.startNode.getIdentifier().equals(place.getIdentifier()) || !aggLink.endNode.getIdentifier().equals(place.getIdentifier())) {
						aggLink = null;
					}
				}
				if (aggLink == null) {
					aggLink = new DAggregateLinkObject();
					aggLinks.addElement(aggLink);
				}
				aggLink.addLink(link, id);
				if (makeGenTrLayer) {
					DMovingObject genMObj = new DMovingObject();
					genMObj.setIdentifier(mobj.getIdentifier());
					genMObj.setEntityId(mobj.getEntityId());
					genMObj.setGeographic(mobj.isGeographic());
					if (mobj.getData() != null) {
						genMObj.setThematicData(mobj.getData());
					}
					genTr.addElement(genMObj);
					float c[] = place.getGeometry().getCentroid();
					RealPoint pos = new RealPoint(c);
					genMObj.addPosition(pos, t0, t0);
					genMObj.addPosition(pos, t1, t1);
					genMObj.listOfVisitedAreas = place.getIdentifier();
					genMObj.nVisitedAreas = 1;
				}
			} else {
				DMovingObject genMObj = null;
				if (makeGenTrLayer) {
					genMObj = new DMovingObject();
					genMObj.setIdentifier(mobj.getIdentifier());
					genMObj.setEntityId(mobj.getEntityId());
					if (mobj.getData() != null) {
						genMObj.setThematicData(mobj.getData());
					}
					genTr.addElement(genMObj);
					genMObj.setGeographic(mobj.isGeographic());
					DPlaceVisitsObject place = (DPlaceVisitsObject) pvObjects.elementAt(placeIdxs.elementAt(0));
					float c[] = place.getGeometry().getCentroid();
					genMObj.addPosition(new RealPoint(c), (TimeMoment) enterTimes.elementAt(0), (TimeMoment) exitTimes.elementAt(0));
					genMObj.listOfVisitedAreas = place.getIdentifier();
					genMObj.nVisitedAreas = 1;
				}
				for (int n = 1; n < placeIdxs.size(); n++) {
					DPlaceVisitsObject start = (DPlaceVisitsObject) pvObjects.elementAt(placeIdxs.elementAt(n - 1)), end = (DPlaceVisitsObject) pvObjects.elementAt(placeIdxs.elementAt(n));
					start.addLinkToPlace(end);
					end.addLinkToPlace(start);
					TimeMoment t0 = (TimeMoment) exitTimes.elementAt(n - 1), t1 = (TimeMoment) enterTimes.elementAt(n);
					DLinkObject link = new DLinkObject();
					link.setup(start, end, t0, t1);
					DAggregateLinkObject aggLink = null;
					for (int k = 0; k < aggLinks.size() && aggLink == null; k++) {
						aggLink = (DAggregateLinkObject) aggLinks.elementAt(k);
						if (!aggLink.startNode.getIdentifier().equals(start.getIdentifier()) || !aggLink.endNode.getIdentifier().equals(end.getIdentifier())) {
							aggLink = null;
						}
					}
					if (aggLink == null) {
						aggLink = new DAggregateLinkObject();
						aggLinks.addElement(aggLink);
					}
					aggLink.addLink(link, id);
					if (genMObj != null) {
						float c[] = end.getGeometry().getCentroid();
						RealPoint pos = new RealPoint(c);
						genMObj.addPosition(pos, t1, (TimeMoment) exitTimes.elementAt(n));
						genMObj.listOfVisitedAreas += " " + end.getIdentifier();
						++genMObj.nVisitedAreas;
					}
				}
			}
			if ((i + 1) % 100 == 0) {
				showMessage("Processed " + (i + 1) + " trajectories; " + (moveLayer.getObjectCount() - i - 1) + " remain.", false);
			}
		}
		if (pvObjects.size() < 2 || aggLinks.size() < 1)
			return false;
		this.places = pvObjects;
		aggMoves = aggLinks;
		return true;
	}

	protected int findAreaContainingPosition(RealPolyline areas[], float x, float y) {
		if (areas == null)
			return -1;
		for (int i = 0; i < areas.length; i++)
			if (areas[i] != null && areas[i].contains(x, y, 0, true))
				return i;
		return -1;
	}

	public boolean summarizeByCircles(DGeoLayer moveLayer, Vector circles, float minRad, boolean onlyActiveTrajectories, boolean onlyStartsEnds, boolean findIntersections, boolean makeGenTrLayer) {
		if (moveLayer == null || circles == null || circles.size() < 2)
			return false;
		boolean geo = moveLayer.isGeographic();
		CircleCollector circleCollector = new CircleCollector();
		circleCollector.allocate(circles.size(), 10);
		for (int i = 0; i < circles.size(); i++) {
			circleCollector.addCircle((RealCircle) circles.elementAt(i));
		}
		circleCollector.setupIndex();
		circles = circleCollector.circles;
		Vector<DPlaceVisitsObject> circleObj = new Vector<DPlaceVisitsObject>(circles.size(), 10);
		for (int i = 0; i < circles.size(); i++) {
			SpatialEntity spe = new SpatialEntity(String.valueOf(i + 1));
			spe.setGeometry((RealCircle) circles.elementAt(i));
			DPlaceVisitsObject obj = new DPlaceVisitsObject();
			obj.setup(spe);
			circleObj.addElement(obj);
		}
		//construct aggregated link objects
		//in parallel, collect information about the generalised places
		//if requested, construct also simplified trajectories containing only
		//the enter and exit points for each generalized place
		Vector<DAggregateLinkObject> aggLinks = new Vector<DAggregateLinkObject>(circles.size() * 5, 100);
		Vector places = new Vector(circles.size(), 50), enterTimes = new Vector(circles.size(), 50), exitTimes = new Vector(circles.size(), 50);
		IntArray placeIdxs = new IntArray(circles.size(), 50), enterPointIdxs = new IntArray(circles.size(), 50), exitPointIdxs = new IntArray(circles.size(), 50);
		for (int i = 0; i < moveLayer.getObjectCount(); i++) {
			if (onlyActiveTrajectories && !moveLayer.isObjectActive(i)) {
				continue;
			}
			DGeoObject gobj = moveLayer.getObject(i);
			if (!(gobj instanceof DMovingObject)) {
				continue;
			}
			DMovingObject mobj = (DMovingObject) gobj;
			String id = mobj.getIdentifier();
			Vector souTrack = mobj.getTrack();
			if (souTrack == null || souTrack.size() < 2) {
				continue;
			}
			if (onlyStartsEnds && souTrack.size() > 2) {
				Vector t = new Vector(2, 1);
				t.addElement(souTrack.elementAt(0));
				t.addElement(souTrack.elementAt(souTrack.size() - 1));
				souTrack = t;
			}
			places.removeAllElements();
			placeIdxs.removeAllElements();
			enterPointIdxs.removeAllElements();
			exitPointIdxs.removeAllElements();
			enterTimes.removeAllElements();
			exitTimes.removeAllElements();
			int j = 0;
			while (j < souTrack.size()) {
				SpatialEntity spe = (SpatialEntity) souTrack.elementAt(j);
				RealPoint pt = spe.getCentre();
				int cirIdx = circleCollector.getContainingCircleIndex(pt.x, pt.y);
				if (cirIdx < 0) {
					++j;
					continue;
				}
				DPlaceVisitsObject place = circleObj.elementAt(cirIdx);
				int exitIdx = place.addVisit(id, souTrack, j, moveLayer.isGeographic());
				TimeMoment t0 = null, t1 = null;
				spe = (SpatialEntity) souTrack.elementAt(j);
				if (spe.getTimeReference() != null) {
					t0 = spe.getTimeReference().getValidFrom();
				}
				spe = (SpatialEntity) souTrack.elementAt(exitIdx);
				if (spe.getTimeReference() != null) {
					t1 = spe.getTimeReference().getValidUntil();
					if (t1 == null) {
						t1 = spe.getTimeReference().getValidFrom();
					}
				}
				places.addElement(place);
				placeIdxs.addElement(cirIdx);
				enterPointIdxs.addElement(j);
				exitPointIdxs.addElement(exitIdx);
				enterTimes.addElement(t0);
				exitTimes.addElement(t1);
				j = exitIdx + 1;
			}
			if (places.size() > 1 && findIntersections) {
				//insert intermediate circles
				int nInserted = 0;
				for (int k = 1; k < placeIdxs.size(); k++) {
					int pIdx1 = exitPointIdxs.elementAt(k - 1), pIdx2 = enterPointIdxs.elementAt(k);
					if (pIdx1 == pIdx2) {
						continue;
					}
					int betwIdxs[] = circleCollector.getCirclesBetween(placeIdxs.elementAt(k - 1), placeIdxs.elementAt(k));
					if (betwIdxs != null) {
						Vector interTimes = new Vector(10, 10);
						IntArray interCirIdxs = new IntArray(10, 10);
						IntArray pIdxs = new IntArray(10, 10);
						for (int pIdx = pIdx1; pIdx < pIdx2; pIdx++) {
							RealPoint p1 = mobj.getPosition(pIdx).getCentre(), p2 = mobj.getPosition(pIdx + 1).getCentre();
							double dist = GeoComp.distance(p2.x, p2.y, p1.x, p1.y, geo);
							if (dist == 0) {
								continue;
							}
							for (int circleIdx : betwIdxs) {
								RealCircle cir = (RealCircle) circleCollector.circles.elementAt(circleIdx);
								RealPoint ptIn = cir.getPointOfLineInsideCircle(p1.x, p1.y, p2.x, p2.y);
								if (ptIn != null) {
									double distRatio = GeoComp.distance(ptIn.x, ptIn.y, p1.x, p1.y, geo) / dist;
									TimeMoment t1 = mobj.getPositionTime(pIdx).getValidUntil(), t2 = mobj.getPositionTime(pIdx + 1).getValidFrom();
									long timeDiff = t2.subtract(t1);
									TimeMoment t = t1.getCopy();
									t.add(Math.round(distRatio * timeDiff));
									//find the right position, according to the time
									int insertIdx = -1;
									for (int tIdx = 0; tIdx < interTimes.size() && insertIdx < 0; tIdx++)
										if (t.compareTo((TimeMoment) interTimes.elementAt(tIdx)) < 0) {
											insertIdx = tIdx;
										}
									if (insertIdx < 0) {
										interCirIdxs.addElement(circleIdx);
										interTimes.addElement(t);
										pIdxs.addElement(pIdx);
									} else {
										interCirIdxs.insertElementAt(circleIdx, insertIdx);
										interTimes.insertElementAt(t, insertIdx);
										pIdxs.insertElementAt(pIdx, insertIdx);
									}
								}
							}
						}
						if (interCirIdxs.size() > 0) {
							for (int ii = 0; ii < interCirIdxs.size(); ii++) {
								int circleIdx = interCirIdxs.elementAt(ii);
								DPlaceVisitsObject place = circleObj.elementAt(circleIdx);
								place.addCross(id, souTrack, pIdxs.elementAt(ii), (TimeMoment) interTimes.elementAt(ii), moveLayer.isGeographic());
								places.insertElementAt(place, k);
								placeIdxs.insertElementAt(circleIdx, k);
								enterPointIdxs.insertElementAt(pIdxs.elementAt(ii), k);
								exitPointIdxs.insertElementAt(pIdxs.elementAt(ii), k);
								enterTimes.insertElementAt((TimeMoment) interTimes.elementAt(ii), k);
								exitTimes.insertElementAt((TimeMoment) interTimes.elementAt(ii), k);
								++k;
								++nInserted;
							}
						}
					}
				}
				//if (nInserted>0)
				//System.out.println("Trajectory "+id+": "+nInserted+" places inserted");
			}
			if (places.size() < 2) {
				DPlaceVisitsObject place = null;
				TimeMoment t0 = null, t1 = null;
				if (places.size() > 0) {
					place = (DPlaceVisitsObject) places.elementAt(0);
					t0 = (TimeMoment) enterTimes.elementAt(0);
					t1 = (TimeMoment) exitTimes.elementAt(0);
				} else {
					SpatialEntity spe = (SpatialEntity) souTrack.elementAt(0);
					RealPoint pt = spe.getCentre();
					RealCircle cir = new RealCircle(pt.x, pt.y, minRad);
					SpatialEntity speCir = new SpatialEntity(String.valueOf(circleObj.size() + 1));
					speCir.setGeometry(cir);
					if (spe.getTimeReference() != null) {
						t0 = spe.getTimeReference().getValidFrom();
						t1 = spe.getTimeReference().getValidUntil();
						if (t1 == null) {
							t1 = spe.getTimeReference().getValidFrom();
						}
					}
					if (souTrack.size() > 1) {
						spe = (SpatialEntity) souTrack.elementAt(souTrack.size() - 1);
						if (spe.getTimeReference() != null) {
							t1 = spe.getTimeReference().getValidUntil();
							if (t1 == null) {
								t1 = spe.getTimeReference().getValidFrom();
							}
						}
					}
					place = new DPlaceVisitsObject();
					place.addVisit(id, souTrack, 0, moveLayer.isGeographic());
					place.setup(speCir);
					circleObj.addElement(place);
				}
				DLinkObject link = new DLinkObject();
				link.setup(place, place, t0, t1);
				DAggregateLinkObject aggLink = null;
				for (int k = 0; k < aggLinks.size() && aggLink == null; k++) {
					aggLink = aggLinks.elementAt(k);
					if (!aggLink.startNode.getIdentifier().equals(place.getIdentifier()) || !aggLink.endNode.getIdentifier().equals(place.getIdentifier())) {
						aggLink = null;
					}
				}
				if (aggLink == null) {
					aggLink = new DAggregateLinkObject();
					aggLinks.addElement(aggLink);
				}
				aggLink.addLink(link, id);
			} else {
				for (j = 1; j < places.size(); j++) {
					DPlaceVisitsObject start = (DPlaceVisitsObject) places.elementAt(j - 1), end = (DPlaceVisitsObject) places.elementAt(j);
					TimeMoment t0 = (TimeMoment) exitTimes.elementAt(j - 1), t1 = (TimeMoment) enterTimes.elementAt(j);
					DLinkObject link = new DLinkObject();
					link.setup(start, end, t0, t1);
					DAggregateLinkObject aggLink = null;
					for (int k = 0; k < aggLinks.size() && aggLink == null; k++) {
						aggLink = aggLinks.elementAt(k);
						if (!aggLink.startNode.getIdentifier().equals(start.getIdentifier()) || !aggLink.endNode.getIdentifier().equals(end.getIdentifier())) {
							aggLink = null;
						}
					}
					if (aggLink == null) {
						aggLink = new DAggregateLinkObject();
						aggLinks.addElement(aggLink);
					}
					aggLink.addLink(link, id);
				}
			}
		}

		for (int i = circleObj.size() - 1; i >= 0; i--)
			if ((circleObj.elementAt(i)).getNVisits() < 1) {
				circleObj.removeElementAt(i);
			}

		if (circleObj.size() < 2 || aggLinks.size() < 1)
			return false;
		this.places = circleObj;
		aggMoves = aggLinks;
		return true;
	}

	protected boolean mayAskUser = true, addLayersAndTables = true;

	public void setMayAskUser(boolean mayAskUser) {
		this.mayAskUser = mayAskUser;
	}

	public void setAddLayersAndTables(boolean addLayersAndTables) {
		this.addLayersAndTables = addLayersAndTables;
	}

	/**
	 * @param moveLayer - the source layer with the trajectories
	 * @param parStr - the string describing the parameters
	 *   (used as a suffix in the names of the layers)
	 * @param placeObjects - previously generated DGeoObjects representing
	 *   generalized positions from the trajectories (areas)
	 * @param aggLinks - aggregated links connecting the generalized positions;
	 *   instances of DAggregateLinkObject
	 * @param aDescr - descriptor of the operation (to update)
	 */
	public void makeLayersAndTables(DGeoLayer moveLayer, String parStr, Vector<DPlaceVisitsObject> placeObjects, Vector<DAggregateLinkObject> aggLinks, boolean onlyActiveTrajectories, boolean onlyStartsEnds, boolean findIntersections,
			ActionDescr aDescr) {
		String name = "Generalised positions from " + moveLayer.getName() + " (" + parStr + ")";
/*
    if (mayAskUser)
      name= Dialogs.askForStringValue(core.getUI().getMainFrame(),"Name of the layer?",name,
        "A map layer with the generalized places will be generated","Generalized places",false);
*/
		DataLoader dataLoader = core.getDataLoader();
		placeLayer = new DPlaceVisitsLayer();
		placeLayer.setGeographic(moveLayer.isGeographic());
		placeLayer.onlyActiveTrajectories = onlyActiveTrajectories;
		placeLayer.onlyStartsEnds = onlyStartsEnds;
		placeLayer.findIntersections = findIntersections;
		placeLayer.setType(Geometry.area);
		placeLayer.setName(name);
		placeLayer.setGeoObjects(placeObjects, true);
		placeLayer.setTrajectoryLayer(moveLayer);
		DrawingParameters dp = placeLayer.getDrawingParameters();
		if (dp == null) {
			dp = new DrawingParameters();
			placeLayer.setDrawingParameters(dp);
		}
		dp.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
		dp.fillContours = false;
		if (addLayersAndTables) {
			dataLoader.addMapLayer(placeLayer, -1);
		}
		ResultDescr rd = new ResultDescr();
		rd.product = placeLayer;
		rd.comment = "generalised places from the trajectories";
		aDescr.addResultDescr(rd);
		placeLayer.setMadeByAction(aDescr);

		DataTable placeTbl = placeLayer.constructTableWithStatistics();
		if (placeTbl != null) {
			placeTbl.setName(placeLayer.getName());
			int tblN = -1;
			if (addLayersAndTables) {
				tblN = dataLoader.addTable(placeTbl);
			}
			placeTbl.setEntitySetIdentifier(placeLayer.getEntitySetIdentifier());
			if (addLayersAndTables) {
				dataLoader.setLink(placeLayer, tblN);
			} else {
				placeLayer.setDataTable(placeTbl);
			}
			placeLayer.setLinkedToTable(true);
			rd = new ResultDescr();
			rd.product = placeTbl;
			rd.comment = "statistics about the generalised places";
			aDescr.addResultDescr(rd);
			placeTbl.setMadeByAction(aDescr);
		}

		//construct a table with thematic information about the aggregated moves
		name = "Aggregated moves from " + moveLayer.getName() + " (" + parStr + ")";
/*
    if (mayAskUser)
      name= Dialogs.askForStringValue(core.getUI().getMainFrame(),"Name of the layer?",name,
        "A map layer with the aggregate moves will be generated","Aggregate moves",false);
*/
		DataTable aggTbl = new DataTable();
		aggTbl.setName(name);
		aggTbl.addAttribute("Start ID", "startId", AttributeTypes.character);
		int startIdIdx = aggTbl.getAttrCount() - 1;
		aggTbl.addAttribute("End ID", "endId", AttributeTypes.character);
		int endIdIdx = aggTbl.getAttrCount() - 1;
		aggTbl.addAttribute("Direction", "direction", AttributeTypes.character);
		int dirIdx = aggTbl.getAttrCount() - 1;
		aggTbl.addAttribute("Length", "length", AttributeTypes.real);
		int lenIdx = aggTbl.getAttrCount() - 1;
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
			lobj.setGeographic(moveLayer.isGeographic());
			DataRecord rec = new DataRecord(lobj.getIdentifier());
			aggTbl.addDataRecord(rec);
			rec.setAttrValue(lobj.startNode.getIdentifier(), startIdIdx);
			rec.setAttrValue(lobj.endNode.getIdentifier(), endIdIdx);
			rec.setAttrValue(lobj.getLinkDirection(), dirIdx);
			double length = lobj.getLength();
			rec.setNumericAttrValue(length, String.valueOf(length), lenIdx);
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
		int aggTblN = -1;
		if (addLayersAndTables) {
			aggTblN = dataLoader.addTable(aggTbl);
		}

		aggLinkLayer = new DAggregateLinkLayer();
		aggLinkLayer.onlyActiveTrajectories = onlyActiveTrajectories;
		aggLinkLayer.onlyStartsEnds = onlyStartsEnds;
		aggLinkLayer.findIntersections = findIntersections;
		aggLinkLayer.setType(Geometry.line);
		aggLinkLayer.setName(aggTbl.getName());
		aggLinkLayer.setGeographic(moveLayer.isGeographic());
		aggLinkLayer.setGeoObjects(aggLinks, true);
		aggLinkLayer.setHasMovingObjects(true);
		aggLinkLayer.setTrajectoryLayer(moveLayer);
		aggLinkLayer.setPlaceLayer(placeLayer);
		DrawingParameters dp1 = aggLinkLayer.getDrawingParameters();
		if (dp1 == null) {
			dp1 = new DrawingParameters();
			aggLinkLayer.setDrawingParameters(dp1);
		}
		dp1.lineColor = dp.lineColor;
		dp1.transparency = 40;
		spec.drawParm = dp1;
		aggLinkLayer.setDataSource(spec);
		if (addLayersAndTables) {
			dataLoader.addMapLayer(aggLinkLayer, -1);
		}
		aggTbl.setEntitySetIdentifier(aggLinkLayer.getEntitySetIdentifier());
		if (addLayersAndTables) {
			dataLoader.setLink(aggLinkLayer, aggTblN);
		} else {
			aggLinkLayer.setDataTable(aggTbl);
		}
		aggLinkLayer.setLinkedToTable(true);
		aggLinkLayer.countActiveLinks();

		rd = new ResultDescr();
		rd.product = aggLinkLayer;
		rd.comment = "aggregated moves constructed from the trajectories";
		aDescr.addResultDescr(rd);
		aggLinkLayer.setMadeByAction(aDescr);
		rd = new ResultDescr();
		rd.product = aggTbl;
		rd.comment = "statistics about the aggregated moves";
		aDescr.addResultDescr(rd);
		aggTbl.setMadeByAction(aDescr);

		if (addLayersAndTables) {
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
		}
	}

	/**
	 * Returns the index of the circle containing the given point
	 */
	protected int getContainingCircleIndex(RealPoint p, Vector circles) {
		if (p == null || circles == null)
			return -1;
		for (int i = circles.size() - 1; i >= 0; i--) {
			RealCircle cir = (RealCircle) circles.elementAt(i);
			if (cir.contains(p.x, p.y, 0f))
				return i;
		}
		return -1;
	}

	/**
	 * Returns 0 if the objects (which must be instances of RealCircle) are equal
	 * (i.e. have equal radii), <0 if the radius of the first circle is less than
	 * the radius of the second second one, >0 otherwise
	 */
	public int compare(Object obj1, Object obj2) {
		if (obj1 == null || obj2 == null || !(obj1 instanceof RealCircle) || !(obj2 instanceof RealCircle))
			return 0;
		RealCircle c1 = (RealCircle) obj1, c2 = (RealCircle) obj2;
		if (c1.rad < c2.rad)
			return -1;
		if (c1.rad > c2.rad)
			return 1;
		return 0;
	}

	/**
	 * For research purposes: produces a map layer with the points grouped
	 * in the given PointOrganizer and a table with the assignment of the
	 * points to the clusters.
	 */
	public void makePointLayer(PointOrganizer pOrg, boolean geographic, String parStr) {
		if (pOrg == null || pOrg.getGroupCount() < 1)
			return;
		Vector<DGeoObject> pObj = new Vector<DGeoObject>(500, 100);
		String name = "Characteristic points (" + parStr + ")";
/*
    name= Dialogs.askForStringValue(core.getUI().getMainFrame(),"Name of the layer?",name,
      "A map layer with the characteristic points will be generated","Characteristic points",false);
*/
		DataTable table = new DataTable();
		table.setName(name);
		table.addAttribute("Cluster N", "cluster_N", AttributeTypes.character);
		int k = 0;
		for (int i = 0; i < pOrg.getGroupCount(); i++) {
			Vector<RealPoint> points = pOrg.getGroup(i);
			if (points == null || points.size() < 1) {
				continue;
			}
			for (int j = 0; j < points.size(); j++) {
				RealPoint p = points.elementAt(j);
				SpatialEntity spe = new SpatialEntity(String.valueOf(++k));
				spe.setGeometry(p);
				DataRecord rec = new DataRecord(spe.getId());
				table.addDataRecord(rec);
				rec.setAttrValue(String.valueOf(i), 0);
				spe.setThematicData(rec);
				DGeoObject gObj = new DGeoObject();
				gObj.setup(spe);
				pObj.addElement(gObj);
			}
		}
		DGeoLayer layer = new DGeoLayer();
		layer.setName(table.getName());
		layer.setType(Geometry.point);
		layer.setGeographic(geographic);
		layer.setGeoObjects(pObj, true);
		DrawingParameters dp = layer.getDrawingParameters();
		if (dp == null) {
			dp = new DrawingParameters();
			layer.setDrawingParameters(dp);
		}
		dp.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
		dp.fillColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
		dp.fillContours = false;
		DataLoader dataLoader = core.getDataLoader();
		dataLoader.addMapLayer(layer, -1);
		int tblN = dataLoader.addTable(table);
		table.setEntitySetIdentifier(layer.getEntitySetIdentifier());
		dataLoader.setLink(layer, tblN);
		layer.setLinkedToTable(true);
		name = "Centroids of clusters of characteristic points (" + parStr + ")";
		name = Dialogs.askForStringValue(core.getUI().getMainFrame(), "Layer name:", name, "Generate a map layer with the centroids of the clusters of characteristic points? " + "Press \"Cancel\" if the layer is not needed.", "Cluster centroids",
				true);
		if (name == null)
			return;

		pObj = new Vector<DGeoObject>(pOrg.getGroupCount(), 1);
		table = new DataTable();
		table.setName(name);
		table.addAttribute("Cluster N", "cluster_N", AttributeTypes.character);
		table.addAttribute("Cluster size", "cluster_size", AttributeTypes.integer);
		for (int i = 0; i < pOrg.getGroupCount(); i++) {
			Vector<RealPoint> points = pOrg.getGroup(i);
			if (points == null || points.size() < 1) {
				continue;
			}
			RealPoint p = pOrg.getCentroid(i);
			SpatialEntity spe = new SpatialEntity(String.valueOf(i + 1));
			spe.setGeometry(p);
			DataRecord rec = new DataRecord(spe.getId());
			table.addDataRecord(rec);
			rec.setAttrValue(String.valueOf(i), 0);
			rec.setNumericAttrValue(points.size(), String.valueOf(points.size()), 1);
			spe.setThematicData(rec);
			DGeoObject gObj = new DGeoObject();
			gObj.setup(spe);
			pObj.addElement(gObj);
		}
		layer = new DGeoLayer();
		layer.setName(table.getName());
		layer.setType(Geometry.point);
		layer.setGeographic(geographic);
		layer.setGeoObjects(pObj, true);
		dp = layer.getDrawingParameters();
		if (dp == null) {
			dp = new DrawingParameters();
			layer.setDrawingParameters(dp);
		}
		dp.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
		dp.fillColor = dp.lineColor;
		dp.fillContours = true;
		dataLoader.addMapLayer(layer, -1);
		tblN = dataLoader.addTable(table);
		table.setEntitySetIdentifier(layer.getEntitySetIdentifier());
		dataLoader.setLink(layer, tblN);
		layer.setLinkedToTable(true);
	}

	public Vector<DPlaceVisitsObject> getPlaces() {
		return places;
	}

	public Vector<DAggregateLinkObject> getAggMoves() {
		return aggMoves;
	}

	public Vector<DMovingObject> getGeneralizedTrajectories() {
		return genTr;
	}

	/**
	 * Returns the resulting layer with the generalized places
	 */
	public DPlaceVisitsLayer getPlaceLayer() {
		return placeLayer;
	}

	/**
	 * Returns the resulting layer with the aggregate moves
	 */
	public DAggregateLinkLayer getAggLinkLayer() {
		return aggLinkLayer;
	}
}
