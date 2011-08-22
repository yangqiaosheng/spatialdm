package spade.analysis.tools.table_processing;

import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.analysis.tools.BaseAnalyser;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.vis.database.AttributeDataPortion;
import spade.vis.space.GeoLayer;
import spade.vis.space.GeoObject;
import spade.vis.space.LayerManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jun 21, 2010
 * Time: 3:24:46 PM
 * Assigns the names of geographical objects to the corresponding table records
 */
public class GeoNamesToTable extends BaseAnalyser {
	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	@Override
	public void run(ESDACore core) {
		this.core = core;
		if (core == null || core.getUI() == null)
			return;
		if (core == null || core.getUI() == null)
			return;
		this.core = core;
		SystemUI ui = core.getUI();
		if (ui.getCurrentMapViewer() == null || ui.getCurrentMapViewer().getLayerManager() == null) {
			showMessage("No map exists!", true);
			return;
		}
		LayerManager lman = core.getUI().getCurrentMapViewer().getLayerManager();
		Vector<GeoLayer> layers = new Vector<GeoLayer>(lman.getLayerCount(), 1);
		for (int i = 0; i < lman.getLayerCount(); i++) {
			GeoLayer layer = lman.getGeoLayer(i);
			if (layer.getObjectCount() > 1 && layer.hasThematicData()) {
				layers.addElement(layer);
			}
		}
		if (layers.size() < 1) {
			showMessage("No suitable layers found!", true);
			return;
		}
		List list = new List(Math.min(Math.max(2, layers.size()), 5));
		for (int i = 0; i < layers.size(); i++) {
			list.add(layers.elementAt(i).getName());
		}
		list.select(layers.size() - 1);
		Panel p = new Panel(new ColumnLayout());
		p.add(new Label("Select the layer with the object names:"));
		p.add(list);
		OKDialog ok = new OKDialog(getFrame(), "Object names", true);
		ok.addContent(p);
		ok.show();
		if (ok.wasCancelled())
			return;
		int idx = list.getSelectedIndex();
		if (idx < 0)
			return;
		GeoLayer layer = layers.elementAt(idx);
		AttributeDataPortion table = layer.getThematicData();
		int nNames = 0;
		for (int i = 0; i < layer.getObjectCount(); i++) {
			GeoObject gObj = layer.getObjectAt(i);
			if (gObj.getName() == null) {
				continue;
			}
			gObj.getData().setName(gObj.getName());
			++nNames;
		}
		if (nNames < 1) {
			showMessage("No object names found!", true);
			return;
		}
		showMessage(nNames + " object names have been assigned to their table records", false);
	}
}
