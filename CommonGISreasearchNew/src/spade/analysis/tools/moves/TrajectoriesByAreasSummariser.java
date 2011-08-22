package spade.analysis.tools.moves;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.tools.BaseAnalyser;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DMovingObject;
import spade.vis.geometry.Geometry;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 06-Mar-2008
 * Time: 13:10:18
 * Summarises trajectories into aggregated moves between pre-specified areas.
 */
public class TrajectoriesByAreasSummariser extends BaseAnalyser {

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
		Vector areaLayers = new Vector(lman.getLayerCount(), 1);
		for (int i = 0; i < lman.getLayerCount(); i++) {
			GeoLayer layer = lman.getGeoLayer(i);
			if ((layer instanceof DGeoLayer) && layer.getObjectCount() > 0)
				if (layer.getObjectAt(0) instanceof DMovingObject) {
					moveLayers.addElement(layer);
				} else if (layer.getType() == Geometry.area && layer.getObjectCount() > 1) {
					areaLayers.addElement(layer);
				}
		}
		if (moveLayers.size() < 1) {
			showMessage("No layers with trajectories found!", true);
			return;
		}
		if (areaLayers.size() < 1) {
			showMessage("No layers with areas found!", true);
			return;
		}
		Panel mainP = new Panel(new ColumnLayout());
		mainP.add(new Label("Select the layer with trajectories to summarise:"));
		List mList = new List(Math.max(moveLayers.size() + 1, 5));
		for (int i = 0; i < moveLayers.size(); i++) {
			mList.add(((DGeoLayer) moveLayers.elementAt(i)).getName());
		}
		mList.select(mList.getItemCount() - 1);
		mainP.add(mList);
		mainP.add(new Label("Select the layer with areas:"));
		List aList = new List(Math.max(areaLayers.size() + 1, 5));
		for (int i = 0; i < areaLayers.size(); i++) {
			aList.add(((DGeoLayer) areaLayers.elementAt(i)).getName());
		}
		aList.select(aList.getItemCount() - 1);
		mainP.add(aList);
		Checkbox cbActiveTr = new Checkbox("use only active (after filtering) trajectories", false);
		mainP.add(cbActiveTr);
		Checkbox cbActiveAreas = new Checkbox("use only active (after filtering) areas", false);
		mainP.add(cbActiveAreas);
		Checkbox cbStartsEnds = new Checkbox("use only the start and end positions", false);
		mainP.add(cbStartsEnds);
		Checkbox cbIntersect = new Checkbox("intersect the trajectories with the areas", true);
		mainP.add(cbIntersect);
		mainP.add(new Line(false));
		CheckboxGroup cbg = new CheckboxGroup();
		Checkbox cbStatic = new Checkbox("static aggregation", false, cbg);
		Checkbox cbDynamic = new Checkbox("dynamic aggregation", true, cbg);
		Panel p = new Panel(new GridLayout(1, 2));
		p.add(cbStatic);
		p.add(cbDynamic);
		mainP.add(p);

		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Summarise trajectories", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return;
		int idx = mList.getSelectedIndex();
		if (idx < 0)
			return;
		DGeoLayer moveLayer = (DGeoLayer) moveLayers.elementAt(idx);
		idx = aList.getSelectedIndex();
		if (idx < 0)
			return;
		DGeoLayer areaLayer = (DGeoLayer) areaLayers.elementAt(idx);
		boolean intersect = cbIntersect.getState();
		boolean useActiveTr = cbActiveTr.getState();
		boolean useActiveAreas = cbActiveAreas.getState();
		boolean useOnlyStartsEnds = cbStartsEnds.getState();
		boolean dynamic = cbDynamic.getState();

		IncrementalMovesSummariser mSum = new IncrementalMovesSummariser();
		if (!mSum.init(core, moveLayer.getName(), areaLayer, useActiveAreas, useOnlyStartsEnds, intersect, dynamic))
			return;

		int nProc = 0;
		for (int i = 0; i < moveLayer.getObjectCount(); i++)
			if (!useActiveTr || moveLayer.isObjectActive(i)) {
				DGeoObject gobj = moveLayer.getObject(i);
				if (!(gobj instanceof DMovingObject)) {
					continue;
				}
				mSum.addTrajectory((DMovingObject) gobj);
				++nProc;
				if (nProc % 100 == 0) {
					showMessage(nProc + " trajectories processed", false);
				}
			}

		mSum.makeLayers(moveLayer);
	}

}
