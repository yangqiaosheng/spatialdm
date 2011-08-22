package configstart;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.Supervisor;
import spade.analysis.system.ToolReCreator;
import spade.lib.basicwin.CManager;
import spade.vis.map.MapViewer;
import spade.vis.spec.MapWindowSpec;
import spade.vis.spec.SaveableTool;

/**
* Builds map visualization at the system startup, according to an externally
* provided specification.
*/
public class MapWindowInitiator implements ToolReCreator {
	/**
	* Replies whether a VisInitiator can use the given specification for
	* re-constructing the corresponding tool. The specification must be an
	* instance of the class spade.vis.spec.MapWindowSpec.
	*/
	@Override
	public boolean canFulfillSpecification(Object spec) {
		if (spec == null || !(spec instanceof MapWindowSpec))
			return false;
		MapWindowSpec wsp = (MapWindowSpec) spec;
		if (wsp.tagName == null)
			return false;
		return wsp.tagName.equalsIgnoreCase("map_window");
	}

	/**
	* On the basis of the given specification, re-constructs the corresponding
	* tool. The specification must be an instance of the class
	* spade.vis.spec.MapWindowSpec.
	*/
	@Override
	public void fulfillSpecification(Object spec, DataKeeper dKeeper, Supervisor supervisor, Object visManager, boolean makeMapManipulator) {
		if (spec == null || !(spec instanceof MapWindowSpec))
			return;
		MapWindowSpec wsp = (MapWindowSpec) spec;
		if (wsp.tagName.equals("map_window")) {
			setupMapWindow(wsp, supervisor);
		}
	}

	private void setupMapWindow(MapWindowSpec wsp, Supervisor supervisor) {
		if (supervisor == null || supervisor.getUI() == null)
			return;
		MapViewer mapView = supervisor.getUI().getMapViewer((wsp.primary) ? "main" : wsp.windowId);
		if (mapView == null)
			return;
		Window win = null;
		if (wsp.primary) {
			win = supervisor.getUI().getMainFrame();
		} else if (mapView instanceof Component) {
			win = CManager.getWindow((Component) mapView);
		}
		if (win != null) {
			if (wsp.bounds != null) {
				Rectangle scrSize = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
				if (wsp.bounds.intersects(scrSize) || (scrSize.contains(wsp.bounds.x, wsp.bounds.y) && scrSize.contains(wsp.bounds.x + wsp.bounds.width, wsp.bounds.y + wsp.bounds.height))) {
					win.setBounds(wsp.bounds);
				}
			}
			if (wsp.title != null) {
				win.setName(wsp.title);
				if (win instanceof Frame) {
					((Frame) win).setTitle(wsp.title);
				}
			}
		}
		if (wsp.properties != null && (mapView instanceof SaveableTool)) {
			((SaveableTool) mapView).setProperties(wsp.properties);
		}
		if (wsp.extent != null) {
			mapView.showTerrExtent(wsp.extent.rx1, wsp.extent.ry1, wsp.extent.rx2, wsp.extent.ry2);
		}
	}

}
