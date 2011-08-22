package spade.time.ui;

import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.Supervisor;
import spade.analysis.system.ToolReCreator;
import spade.lib.basicwin.CManager;
import spade.vis.spec.WinSpec;

/**
* Restores the state of the time controls
*/
public class TimePanelRestorer implements ToolReCreator {
	/**
	* Replies whether this component can use the given specification for
	* re-constructing the corresponding tool. The argument spec may be an instance
	* of some specification class, for example, spade.vis.spec.ToolSpec or
	* spade.vis.spec.AnimatedVisSpec. The method must check whether the class
	* of the specification is appropriate for this tool re-creator.
	*/
	@Override
	public boolean canFulfillSpecification(Object spec) {
		return spec != null && (spec instanceof WinSpec) && (((WinSpec) spec).tagName.equals("time_controls") || ((WinSpec) spec).tagName.equals("time_filter_controls"));
	}

	/**
	* On the basis of the given specification, re-constructs the corresponding
	* tool. The argument @arg spec may be an instance of some specification class,
	* for example, spade.vis.spec.ToolSpec or spade.vis.spec.AnimatedVisSpec.
	* The argument @arg visManager is a component used for creating visual data
	* displays and cartographic visualizers. This may be either a DisplayProducer
	* or a SimpleDataMapper, depending on the configuration.
	*/
	@Override
	public void fulfillSpecification(Object spec, DataKeeper dKeeper, Supervisor supervisor, Object visManager, boolean makeMapManipulator) {
		if (spec == null || !(spec instanceof WinSpec))
			return;
		if (supervisor == null || supervisor.getUI() == null || supervisor.getUI().getTimeUI() == null || !(supervisor.getUI().getTimeUI() instanceof TimeFunctionsUI))
			return;
		TimeFunctionsUI timeUI = (TimeFunctionsUI) supervisor.getUI().getTimeUI();
		WinSpec wsp = (WinSpec) spec;
		TimeControlPanel slp = (wsp.tagName.equals("time_controls")) ? timeUI.getDisplayTimeControls() : timeUI.getTimeFilterControls();
		if (slp == null)
			return;
		Window win = CManager.getWindow(slp);
		if (win != null) {
			if (wsp.bounds != null) {
				Rectangle scrSize = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
				if (wsp.bounds.width > scrSize.width) {
					wsp.bounds.width = scrSize.width;
				}
				if (wsp.bounds.height > scrSize.height) {
					wsp.bounds.height = scrSize.height;
				}
				if (wsp.bounds.x + wsp.bounds.width > scrSize.width) {
					wsp.bounds.x = scrSize.width - wsp.bounds.width;
				}
				if (wsp.bounds.y + wsp.bounds.height > scrSize.height) {
					wsp.bounds.y = scrSize.height - wsp.bounds.height;
				}
				win.setBounds(wsp.bounds);
			}
			if (wsp.title != null) {
				win.setName(wsp.title);
				if (win instanceof Frame) {
					((Frame) win).setTitle(wsp.title);
				}
			}
		}
		if (wsp.properties != null) {
			slp.setProperties(wsp.properties);
		}
	}
}