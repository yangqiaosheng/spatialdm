package spade.analysis.tools.moves;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.List;
import java.awt.Panel;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.tools.DataAnalyser;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.TextCanvas;
import spade.time.manage.TemporalDataManager;
import spade.vis.dmap.DAggregateLayer;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DMovingObject;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 21, 2008
 * Time: 10:58:32 AM
 * Visualises interactions between trajectories
 */
public class InteractionsViewTool implements DataAnalyser {

	protected ESDACore core = null;

	/**
	 * Returns true when the tool has everything necessary for its operation.
	 * For example, if the tool manages a number of analysis methods, it should
	 * check whether the class for at least one method is available.
	 * A TrajectoriesGeneraliser always returns true.
	 */
	@Override
	public boolean isValid(ESDACore core) {
		return true;
	}

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
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
		TemporalDataManager timeMan = core.getDataKeeper().getTimeManager();
		if (timeMan == null) {
			showMessage("No time manager exists (=> no time-referenced data)!", true);
			return;
		}
		int nCont = timeMan.getContainerCount();
		if (nCont < 1) {
			showMessage("No time-referenced data found!", true);
			return;
		}
		Vector interLayers = new Vector(nCont, 1);
		for (int i = 0; i < nCont; i++)
			if (timeMan.getContainer(i) instanceof DAggregateLayer) {
				DAggregateLayer aggLayer = (DAggregateLayer) timeMan.getContainer(i);
				if (aggLayer.getSourceLayer() != null) {
					//check if the layer contains trajectories
					DGeoLayer layer = aggLayer.getSourceLayer();
					if (layer.getObjectCount() > 1 && (layer.getObject(0) instanceof DMovingObject)) {
						interLayers.addElement(aggLayer);
					}
				}
			}
		if (interLayers.size() < 1) {
			showMessage("No layers with interactions between trajectories found!", true);
			return;
		}
		List lst = new List(Math.max(interLayers.size(), 5));
		for (int i = 0; i < interLayers.size(); i++) {
			DGeoLayer layer = (DGeoLayer) interLayers.elementAt(i);
			lst.add(layer.getName());
		}
		lst.select(0);
		Panel p = new Panel(new BorderLayout());
		TextCanvas tc = new TextCanvas();
		tc.setText("Visualise the interactions from the layer:");
		p.add(tc, BorderLayout.NORTH);
		p.add(lst, BorderLayout.CENTER);
		Frame mainFrame = null;
		if (core.getUI() != null) {
			mainFrame = core.getUI().getMainFrame();
		}
		if (mainFrame == null) {
			mainFrame = CManager.getAnyFrame();
		}
		if (lst.getItemCount() > 1) {
			OKDialog okd = new OKDialog(mainFrame, "Visualise interactions", true);
			okd.addContent(p);
			okd.show();
			if (okd.wasCancelled())
				return;
		}
		int selIdx = lst.getSelectedIndex();
		if (selIdx < 0)
			return;
		DAggregateLayer aggLayer = (DAggregateLayer) interLayers.elementAt(selIdx);
		InteractionsTimeLineViewPanel tlv = new InteractionsTimeLineViewPanel(aggLayer, aggLayer.getSourceLayer());
		tlv.setName(aggLayer.getName());
		tlv.setSupervisor(core.getSupervisor());
		core.getDisplayProducer().showGraph(tlv);
	}
}
