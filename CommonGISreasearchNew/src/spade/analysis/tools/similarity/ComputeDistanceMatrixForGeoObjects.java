package spade.analysis.tools.similarity;

import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.analysis.tools.BaseAnalyser;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.vis.dmap.DGeoLayer;
import spade.vis.geometry.Geometry;
import spade.vis.space.LayerManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 29, 2010
 * Time: 12:10:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class ComputeDistanceMatrixForGeoObjects extends BaseAnalyser {
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
		SystemUI ui = core.getUI();
		if (ui.getCurrentMapViewer() == null || ui.getCurrentMapViewer().getLayerManager() == null) {
			showMessage("No map exists!", true);
			return;
		}
		LayerManager lman = core.getUI().getCurrentMapViewer().getLayerManager();
		Vector<DGeoLayer> layers = new Vector<DGeoLayer>(lman.getLayerCount(), 1);
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
			showMessage("No suitable map layers found!", true);
			return;
		}
		List list = new List(Math.min(Math.max(2, layers.size()), 5));
		for (int i = 0; i < layers.size(); i++) {
			list.add(layers.elementAt(i).getName());
		}
		list.select(layers.size() - 1);
		Panel p = new Panel(new ColumnLayout());
		p.add(new Label("Select the layer for computing distances:"));
		p.add(list);
		OKDialog ok = new OKDialog(getFrame(), "Spatial distance to geo objects", true);
		ok.addContent(p);
		ok.show();
		if (ok.wasCancelled())
			return;
		int idx = list.getSelectedIndex();
		if (idx < 0)
			return;
		DGeoLayer layer = layers.elementAt(idx);
		SimilarityComputer simComp = null;
		if (layer.getType() == Geometry.point) {
			simComp = new PointSimilarityComputer();
		} else if (layer.getType() == Geometry.line && layer.getSubtype() == Geometry.movement) {
			simComp = new TrajectorySimilarityComputer();
		}
		if (simComp == null) {
			showMessage("Sorry... Not implemented yet!", true);
			return;
		}
		simComp.setLayer(layer);
		simComp.setSystemCore(core);
		simComp.computeDistanceMatrix();
	}
}
