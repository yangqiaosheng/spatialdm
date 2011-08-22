package spade.analysis.geocomp;

import java.awt.Checkbox;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.analysis.tools.clustering.PointOrganizerSpatialIndex;
import spade.analysis.tools.moves.PointsInCircle;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Centimeter;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DLayerManager;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.geometry.RealRectangle;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.space.SelectLayer;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 24, 2009
 * Time: 1:57:54 PM
 * Spatially groups points in a selected point layer according to the given maximum radius.
 * Optionally produces a layer with group centroids.
 */
public class PointGrouper extends GeoCalculator {
	protected ESDACore core = null;
	/**
	 * The original point layer, selected by the user
	 */
	protected DGeoLayer pointLayer = null;
	/**
	 * The maximal radius of a group of points, specified by the user
	 */
	protected double maxRad = Double.NaN;
	/**
	 * Whether the user wishes that a layer with group centres is generated.
	 */
	protected boolean makeLayerWithCentroids = false;
	/**
	 * Whether additional points in empty areas should be added (this
	 * may be needed for building Voronoi polygons after grouping the
	 * points but not for the grouping itself)
	 */
	protected boolean addPoints = false;
	/**
	 * Whether circles must be built around the groups of points
	 */
	protected boolean makeCircles = false;

	/**
	 * Spatially groups points in a selected point layer according to the given maximum radius.
	 * The arguments are a layer manager (a GeoCalculator must itself care about
	 * selection of a layer or layers of appropriate type, in this case layers
	 * with point objects) and SystemUI (to be used for displaying messages and
	 * finding an owner frame for dialogs)
	 * If calculation was successful, returns the produced layer (an instance
	 * of DGeoLayer) with points (instances of RealPoint).
	*/
	@Override
	public Object doCalculation(LayerManager lman, ESDACore core) {
		this.core = core;
		if (!getSettings(lman))
			return false;
		PointOrganizerSpatialIndex pOrg = groupPoints(pointLayer, maxRad);
		if (pOrg == null)
			return null;
		AttrSpec asp = addGroupNumbersToTable(pointLayer, pOrg);
		Vector<DGeoLayer> layers = new Vector<DGeoLayer>(2, 1);
		if (makeLayerWithCentroids) {
			DGeoLayer layer = makeLayerWithGroupCentroids(pOrg);
			if (layer != null) {
				layers.addElement(layer);
			}
		}
		if (makeCircles) {
			DGeoLayer layer = makeCirclesAroundGroups(pOrg);
			if (layer != null) {
				layers.addElement(layer);
			}
		}
		if (layers.size() < 1)
			return asp;
		if (layers.size() == 1)
			return layers.elementAt(0);
		return layers;
	}

	/**
	 * Asks the user to select the point layer and maximum radius
	 */
	protected boolean getSettings(LayerManager lman) {
		return getSettings(lman, false);
	}

	/**
	 * Asks the user to select the point layer and maximum radius
	 * @param askAddPoints - if the dialog is used for building Voronoi cells,
	 *   the user is asked whether additional points should be introduced in
	 *   empty areas.
	 */
	protected boolean getSettings(LayerManager lman, boolean askAddPoints) {
		SystemUI ui = core.getUI();
		//propose the user to select a layer with point objects
		if (lman == null)
			return false;
		Frame win = null;
		if (ui != null) {
			win = ui.getMainFrame();
		}
		if (win == null) {
			win = CManager.getAnyFrame();
		}
		GeoLayer pl = SelectLayer.selectLayer(lman, Geometry.point, "Select a layer with points:", win);
		if (pl == null || !(pl instanceof DGeoLayer))
			return false;
		pointLayer = (DGeoLayer) pl;
		double wh[] = DGeoLayer.getExtentXY(pointLayer);
		if (wh == null || wh[0] <= 0 || wh[1] <= 0)
			return false;
		float width = (float) wh[0], height = (float) wh[1];
		RealRectangle r = pointLayer.getWholeLayerBounds();
		if (r == null) {
			r = pointLayer.getCurrentLayerBounds();
		}
		float geoFactorX = 1f, geoFactorY = 1f;
		boolean geo = pointLayer.isGeographic();
		if (geo) {
			geoFactorX = width / (r.rx2 - r.rx1);
			geoFactorY = height / (r.ry2 - r.ry1);
		}
		float defMaxRad = Math.min(width, height) / 20;
		float factor = 1;
		if (defMaxRad > 1) {
			while (defMaxRad >= 10) {
				factor *= 10;
				defMaxRad /= 10;
			}
		} else {
			while (defMaxRad < 1) {
				factor /= 10;
				defMaxRad *= 10;
			}
		}
		if (defMaxRad < 3) {
			defMaxRad = 1;
		} else if (defMaxRad < 7) {
			defMaxRad = 5;
		} else {
			defMaxRad = 10;
		}
		defMaxRad *= factor;
		String maxRadStr = StringUtil.floatToStr(defMaxRad, 0, defMaxRad * 10);
		Panel mainP = new Panel(new ColumnLayout());
		Panel p = new Panel();
		GridBagLayout gridbag = new GridBagLayout();
		p.setLayout(gridbag);
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0f;
		c.weighty = 1.0f;
		c.fill = GridBagConstraints.HORIZONTAL;
		Label l = new Label("Maximum group radius?");
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		TextField maxRadTF = new TextField(maxRadStr, 10);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(maxRadTF, c);
		p.add(maxRadTF);
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
		Checkbox cbPointLayer = new Checkbox("make a map layer with the centroids of the groups", true);
		mainP.add(cbPointLayer);
		Checkbox cbAddPoints = null, cbMakeCircles = null;
		if (askAddPoints) {
			cbAddPoints = new Checkbox("introduce additional generating points for Voronoi cells in empty areas", false);
			mainP.add(cbAddPoints);
		} else {
			cbMakeCircles = new Checkbox("make circles around the groups of points", false);
			mainP.add(cbMakeCircles);
		}

		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Group points", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return false;
		maxRad = 0f;
		String str = maxRadTF.getText();
		if (str != null) {
			try {
				maxRad = Float.valueOf(str).floatValue();
			} catch (NumberFormatException nfe) {
			}
		}
		makeLayerWithCentroids = cbPointLayer.getState();
		if (cbAddPoints != null) {
			addPoints = cbAddPoints.getState();
		}
		if (cbMakeCircles != null) {
			makeCircles = cbMakeCircles.getState();
		}
		return true;
	}

	/**
	 * Groups the points and returns the result as PointOrganizer.
	 */
	public PointOrganizerSpatialIndex groupPoints(DGeoLayer pointLayer, double maxRad) {
		if (pointLayer == null || pointLayer.getObjectCount() < 2 || Double.isNaN(maxRad) || maxRad <= 0)
			return null;
		double wh[] = DGeoLayer.getExtentXY(pointLayer);
		if (wh == null || wh[0] <= 0 || wh[1] <= 0)
			return null;
		float width = (float) wh[0], height = (float) wh[1];
		RealRectangle r = pointLayer.getWholeLayerBounds();
		if (r == null) {
			r = pointLayer.getCurrentLayerBounds();
		}
		if (r == null || r.rx2 - r.rx1 <= 0 || r.ry2 - r.ry1 <= 0)
			return null;
		float geoFactorX = 1f, geoFactorY = 1f;
		if (pointLayer.isGeographic()) {
			geoFactorX = width / (r.rx2 - r.rx1);
			geoFactorY = height / (r.ry2 - r.ry1);
		}
		PointOrganizerSpatialIndex pOrg = new PointOrganizerSpatialIndex();
		pOrg.setSpatialExtent(r.rx1, r.ry1, r.rx2, r.ry2);
		if (makeCircles && pointLayer.isGeographic()) {
			pOrg.setGeo(true, geoFactorX, geoFactorY);
			pOrg.setMaxRad(maxRad);
		} else {
			pOrg.setGeo(false, 1, 1);
			double maxRadX = maxRad, maxRadY = maxRad;
			if (pointLayer.isGeographic()) {
				maxRadX = maxRad / geoFactorX;
				maxRadY = maxRad / geoFactorY;
			}
			pOrg.setMaxRad(Math.min(maxRadX, maxRadY));
		}
		long t0 = System.currentTimeMillis();
		core.getUI().showMessage("Grouping the points by proximity ...", false);
		int np = 0;
		for (int i = 0; i < pointLayer.getObjectCount(); i++)
			if (pointLayer.isObjectActive(i)) {
				DGeoObject gobj = pointLayer.getObject(i);
				Geometry geom = gobj.getGeometry();
				if (geom == null) {
					continue;
				}
				RealPoint p = null;
				if (geom instanceof RealPoint) {
					p = (RealPoint) geom;
				} else {
					float c[] = geom.getCentroid();
					if (c == null) {
						continue;
					}
					p = new RealPoint(c[0], c[1]);
				}
				p.setIndex(i);
				pOrg.addPoint(p);
				++np;
				if (np % 100 == 0) {
					core.getUI().showMessage(np + " points processed; " + pOrg.getGroupCount() + " groups built", false);
				}
			}
		int nGroups = pOrg.getGroupCount();
		if (nGroups < 2) {
			core.getUI().showMessage("Failed to group the extracted points!", true);
			return null;
		}
		pOrg.reDistributePoints();
		long t = System.currentTimeMillis();
		core.getUI().showMessage(nGroups + " point clusters obtained in " + (t - t0) + " msec.", false);
		System.out.println(nGroups + " point clusters obtained in " + (t - t0) + " msec.");

		if (makeCircles) { //try to merge close groups
			t0 = System.currentTimeMillis();
			core.getUI().showMessage("Merging close groups...", false);
			pOrg.mergeCloseGroups();
			int nGroups1 = pOrg.getGroupCount();
			t = System.currentTimeMillis();
			if (nGroups1 == nGroups) {
				core.getUI().showMessage("No groups have been merged; elapsed time " + (t - t0) + " msec.", false);
				System.out.println("No groups have been merged; elapsed time " + (t - t0) + " msec.");
			} else {
				core.getUI().showMessage((nGroups - nGroups1) + " groups have been merged; elapsed time " + (t - t0) + " msec.", false);
				System.out.println((nGroups - nGroups1) + " groups have been merged; elapsed time " + (t - t0) + " msec.");
			}
		}

		core.getUI().showMessage("Optimizing the grouping...", false);
		t0 = System.currentTimeMillis();
		pOrg.optimizeGrouping();
		nGroups = pOrg.getGroupCount();
		t = System.currentTimeMillis();
		core.getUI().showMessage(nGroups + " groups after optimization; elapsed time " + (t - t0) + " msec.", false);
		System.out.println(nGroups + " groups after optimization; elapsed tim " + (t - t0) + " msec.");
		return pOrg;
	}

	/**
	 * Adds an attribute with the group number to the table describing the original points.
	 * If the table does not exist yet, it is created.
	 */
	public AttrSpec addGroupNumbersToTable(DGeoLayer pointLayer, PointOrganizerSpatialIndex pOrg) {
		DataTable pTable = null;
		boolean newTable = false;
		if (pointLayer.hasThematicData() && (pointLayer.getThematicData() instanceof DataTable)) {
			pTable = (DataTable) pointLayer.getThematicData();
		} else {
			pTable = new DataTable();
			pTable.setName("Data about " + pointLayer.getName());
			newTable = true;
		}
		int aIdx = pTable.getAttrCount();
		pTable.addAttribute("Group N (r=" + maxRad + ")", "group_N_" + pTable.getAttrCount(), AttributeTypes.character);
		pTable.addAttribute("Group size (r=" + maxRad + ")", "group_size_" + pTable.getAttrCount(), AttributeTypes.integer);
		for (int i = 0; i < pOrg.getGroupCount(); i++) {
			Vector<RealPoint> points = pOrg.getGroup(i);
			if (points == null || points.size() < 1) {
				continue;
			}
			for (int j = 0; j < points.size(); j++) {
				RealPoint p = points.elementAt(j);
				int oIdx = p.getIndex();
				if (oIdx < 0) {
					continue;
				}
				DGeoObject gobj = pointLayer.getObject(oIdx);
				DataRecord rec = (DataRecord) gobj.getData();
				if (rec == null) {
					rec = new DataRecord(gobj.getIdentifier(), gobj.getName());
					pTable.addDataRecord(rec);
					gobj.setThematicData(rec);
				}
				rec.setAttrValue(String.valueOf(i), aIdx);
				rec.setNumericAttrValue(points.size(), String.valueOf(points.size()), aIdx + 1);
			}
		}
		if (newTable) {
			pointLayer.setDataTable(pTable);
			pointLayer.setLinkedToTable(true);
			int tN = core.getDataLoader().addTable(pTable);
			core.getDataLoader().setLink(pointLayer, tN);
		}
		AttrSpec asp = new AttrSpec();
		asp.layer = pointLayer;
		asp.table = pTable;
		asp.attrIds = new Vector(2, 1);
		asp.attrIds.addElement(pTable.getAttributeId(aIdx));
		asp.attrIds.addElement(pTable.getAttributeId(aIdx + 1));
		return asp;
	}

	/**
	 * Produces a map layer with group centroids and a corresponding table
	 * with the numbers and sizes of the groups
	 */
	public DGeoLayer makeLayerWithGroupCentroids(PointOrganizerSpatialIndex pOrg) {
		if (pOrg == null)
			return null;
		int nGroups = pOrg.getGroupCount();
		if (nGroups < 1)
			return null;
		Vector<RealPoint> points = new Vector<RealPoint>(nGroups, 10);
		for (int i = 0; i < nGroups; i++) {
			points.addElement(pOrg.getCentroid(i));
		}

		String name = "Centroids of the groups of points from " + pointLayer.getName() + " (r=" + maxRad + ")";
		Vector<DGeoObject> pObj = new Vector<DGeoObject>(500, 100);
		DataTable table = new DataTable();
		table.setName(name);
		table.addAttribute("Group N", "group_N", AttributeTypes.character);
		table.addAttribute("Group size", "group_size", AttributeTypes.integer);
		for (int i = 0; i < points.size(); i++) {
			RealPoint p = points.elementAt(i);
			SpatialEntity spe = new SpatialEntity(String.valueOf(i + 1));
			spe.setGeometry(p);
			DataRecord rec = new DataRecord(spe.getId());
			table.addDataRecord(rec);
			rec.setAttrValue(String.valueOf(i), 0);
			int grSize = pOrg.getGroup(i).size();
			rec.setNumericAttrValue(grSize, String.valueOf(grSize), 1);
			spe.setThematicData(rec);
			DGeoObject gObj = new DGeoObject();
			gObj.setup(spe);
			pObj.addElement(gObj);
		}
		DGeoLayer cenLayer = new DGeoLayer();
		cenLayer.setName(table.getName());
		cenLayer.setType(Geometry.point);
		cenLayer.setGeographic(pointLayer.isGeographic());
		cenLayer.setGeoObjects(pObj, true);
		cenLayer.setDataTable(table);
		cenLayer.setLinkedToTable(true);
		DrawingParameters dp = cenLayer.getDrawingParameters();
		if (dp == null) {
			dp = new DrawingParameters();
			cenLayer.setDrawingParameters(dp);
		}
		dp.lineColor = dp.fillColor = Color.red;
		return cenLayer;
	}

	/**
	 * Produces a map layer with group centroids and a corresponding table
	 * with the numbers and sizes of the groups
	 */
	public DGeoLayer makeCirclesAroundGroups(PointOrganizerSpatialIndex pOrg) {
		if (pOrg == null)
			return null;
		if (pOrg == null)
			return null;
		int nGroups = pOrg.getGroupCount();
		if (nGroups < 1)
			return null;
		RealPolyline circles[] = new RealPolyline[nGroups];
		double radX[] = new double[nGroups], radY[] = new double[nGroups], radii[] = new double[nGroups];
		int nCircles = 0;
		double maxRadX = pOrg.maxRadX, maxRadY = pOrg.maxRadY;
		if (Double.isNaN(maxRadX)) {
			maxRadX = pOrg.maxRad;
		}
		if (Double.isNaN(maxRadY)) {
			maxRadY = pOrg.maxRad;
		}
		double dx = maxRadX / 100, dy = maxRadY / 100;
		int nPoints = 36;
		double dAngle = 2 * Math.PI / nPoints;
		double extent = Math.min((pOrg.x2 - pOrg.x1) * pOrg.geoFactorX, (pOrg.y2 - pOrg.y1) * pOrg.geoFactorY);
		double minRadius = Dialogs.askForDoubleValue(core.getUI().getMainFrame(), "Desired minimal circle radius?", 0, 0, extent / 2, null, "Minimal circle radius", false);
		for (int i = 0; i < nGroups; i++) {
			circles[i] = null;
			radX[i] = 0;
			radY[i] = 0;
			PointsInCircle pCircle = pOrg.getGroupInCircle(i);
			if (pCircle == null) {
				continue;
			}
			RealRectangle bounds = pCircle.getBounds();
			if (bounds == null) {
				continue;
			}
			RealPoint c = new RealPoint((bounds.rx1 + bounds.rx2) / 2, (bounds.ry1 + bounds.ry2) / 2);
			radii[i] = pCircle.getMaxDistToPoint(c.x, c.y);
			double rad = Math.max(radii[i], minRadius), rx = rad / pOrg.geoFactorX + dx, ry = rad / pOrg.geoFactorY + dy;
			//generate a polyline approximating the circle
			RealPolyline z = new RealPolyline();
			z.isClosed = true;
			z.p = new RealPoint[nPoints + 1];
			double angle = 0;
			for (int j = 0; j < nPoints; j++) {
				double cos = Math.cos(angle), sin = Math.sin(angle);
				z.p[j] = new RealPoint(c.x + (float) (rx * cos), c.y + (float) (ry * sin));
				angle += dAngle;
			}
			z.p[nPoints] = z.p[0];
			circles[i] = z;
			radX[i] = rx;
			radY[i] = ry;
			++nCircles;
		}
		if (nCircles < 1)
			return null;
		String name = "Circles around the groups of points from " + pointLayer.getName() + " (r=" + maxRad + ")";
		Vector<DGeoObject> pObj = new Vector<DGeoObject>(500, 100);
		DataTable table = new DataTable();
		table.setName(name);
		table.addAttribute("Group N", "group_N", AttributeTypes.character);
		table.addAttribute("Group size", "group_size", AttributeTypes.integer);
		table.addAttribute("Centre X", "centre_X", AttributeTypes.real);
		table.addAttribute("Centre Y", "centre_Y", AttributeTypes.real);
		table.addAttribute("Group radius", "group_radius", AttributeTypes.real);
		table.addAttribute("X-Radius", "x_radius", AttributeTypes.real);
		table.addAttribute("Y-Radius", "y_radius", AttributeTypes.real);
		for (int i = 0; i < nGroups; i++)
			if (circles[i] != null) {
				SpatialEntity spe = new SpatialEntity(String.valueOf(i + 1));
				spe.setGeometry(circles[i]);
				DataRecord rec = new DataRecord(spe.getId());
				table.addDataRecord(rec);
				rec.setAttrValue(String.valueOf(i), 0);
				int grSize = pOrg.getGroup(i).size();
				rec.setNumericAttrValue(grSize, String.valueOf(grSize), 1);
				RealPoint c = pOrg.getCentroid(i);
				rec.setNumericAttrValue(c.x, String.valueOf(c.x), 2);
				rec.setNumericAttrValue(c.y, String.valueOf(c.y), 3);
				rec.setNumericAttrValue(radii[i], String.valueOf(radii[i]), 4);
				rec.setNumericAttrValue(radX[i], String.valueOf(radX[i]), 5);
				rec.setNumericAttrValue(radY[i], String.valueOf(radY[i]), 6);
				spe.setThematicData(rec);
				DGeoObject gObj = new DGeoObject();
				gObj.setup(spe);
				pObj.addElement(gObj);
			}
		DGeoLayer circleLayer = new DGeoLayer();
		circleLayer.setName(table.getName());
		circleLayer.setType(Geometry.area);
		circleLayer.setGeographic(pointLayer.isGeographic());
		circleLayer.setGeoObjects(pObj, true);
		circleLayer.setDataTable(table);
		circleLayer.setLinkedToTable(true);
		return circleLayer;
	}

	public void setCore(ESDACore core) {
		this.core = core;
	}
}
