package spade.analysis.tools.moves;

import spade.analysis.system.ESDACore;
import spade.lib.basicwin.Dialogs;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DMovingObject;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jun 18, 2008
 * Time: 3:46:45 PM
 * Removes outliers from trajectories.
 */
public class OutlierRemover {

	protected ESDACore core = null;

	/**
	 * This method constructs and starts the tool. Everything the tool may need
	 * for integration with other components of the system can be received
	 * from the system's core passed as an argument.
	 */
	public void run(DGeoLayer moveLayer, ESDACore core) {
		this.core = core;
		int nObjTot = 0, nObjChanged = 0;
		double maxSpeed = Dialogs.askForDoubleValue(core.getUI().getMainFrame(), "Maximum possible speed?", Double.NaN, Double.NaN, Double.NaN, null, "Clean trajectories", true);
		if (Double.isNaN(maxSpeed))
			return;
		for (int i = 0; i < moveLayer.getObjectCount(); i++) {
			DGeoObject gobj = moveLayer.getObject(i);
			if (gobj instanceof DMovingObject) {
				DMovingObject mobj = (DMovingObject) gobj;
				boolean changed = mobj.removeOutliers(maxSpeed);
				++nObjTot;
				if (changed) {
					++nObjChanged;
				}
				if (nObjTot % 100 == 0 || changed) {
					showMessage("Checked: " + nObjTot + " trajectories; cleaned: " + nObjChanged, false);
				}
			}
		}
		if (nObjChanged < 1) {
			showMessage("No trajectories have been cleaned!", false);
			return;
		}
		showMessage(nObjChanged + " trajectories have been cleaned out of " + nObjTot, false);
		moveLayer.notifyPropertyChange("ObjectSet", null, null);
	}

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
	}
}
