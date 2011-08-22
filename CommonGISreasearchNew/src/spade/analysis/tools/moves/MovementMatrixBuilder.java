package spade.analysis.tools.moves;

import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.tools.DataAnalyser;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.vis.dmap.DAggregateLinkLayer;
import spade.vis.space.LayerManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 12-Mar-2008
 * Time: 12:05:30
 * For a user-selected vayer with aggregated links (vectors) generates
 * a movement matrix.
 */
public class MovementMatrixBuilder implements DataAnalyser {

	protected ESDACore core = null;

	/**
	 * Returns true when the tool has everything necessary for its operation.
	 * For example, if the tool manages a number of analysis methods, it should
	 * check whether the class for at least one method is available.
	 * Always returns true.
	 */
	@Override
	public boolean isValid(ESDACore core) {
		return true;
	}

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
		//Find instances of DAggregateLinkLayer
		LayerManager lman = core.getUI().getCurrentMapViewer().getLayerManager();
		Vector aggLayers = new Vector(lman.getLayerCount(), 1);
		for (int i = 0; i < lman.getLayerCount(); i++)
			if (lman.getGeoLayer(i) instanceof DAggregateLinkLayer) {
				aggLayers.addElement(lman.getGeoLayer(i));
			}
		if (aggLayers.size() < 1) {
			showMessage("No layers with aggregated moves (vectors) found!", true);
			return;
		}
		int idx = 0;
		if (aggLayers.size() > 1) {
			Panel mainP = new Panel(new ColumnLayout());
			mainP.add(new Label("Select the layer with aggregated moves:"));
			List mList = new List(Math.max(aggLayers.size() + 1, 5));
			for (int i = 0; i < aggLayers.size(); i++) {
				mList.add(((DAggregateLinkLayer) aggLayers.elementAt(i)).getName());
			}
			mList.select(0);
			mainP.add(mList);
			OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Build movement matrix", true);
			dia.addContent(mainP);
			dia.show();
			if (dia.wasCancelled())
				return;
			idx = mList.getSelectedIndex();
			if (idx < 0)
				return;
		}
		DAggregateLinkLayer aggLayer = (DAggregateLinkLayer) aggLayers.elementAt(idx);
		AggregatedMovesRepresenter aMoves = new AggregatedMovesRepresenter(aggLayer);
		MovementMatrixPanelAdvanced matr = new MovementMatrixPanelAdvanced(core.getSupervisor(), (aggLayer.getPlaceLayer() == null) ? null : aggLayer.getPlaceLayer().getEntitySetIdentifier(), aMoves, null, true);
		matr.setName("Movement matrix: " + aggLayer.getName());
		core.getDisplayProducer().showGraph(matr);
	}

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
	}
}
