package configstart;

import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.DisplayProducer;
import spade.analysis.system.Supervisor;
import spade.analysis.system.ToolReCreator;
import spade.vis.spec.SaveableTool;
import spade.vis.spec.WinSpec;

/**
* Builds map visualization at the system startup, according to an externally
* provided specification.
*/
public class ChartWindowInitiator implements ToolReCreator, PropertyChangeListener {
	/**
	* Specification of the chart window
	*/
	protected WinSpec chartWinSpec = null;
	/**
	* Specification of the query window
	*/
	protected WinSpec queryWinSpec = null;

	/**
	* Replies whether a VisInitiator can use the given specification for
	* re-constructing the corresponding tool. The specification must be an
	* instance of the class spade.vis.spec.MapWindowSpec.
	*/
	@Override
	public boolean canFulfillSpecification(Object spec) {
		if (spec == null || !(spec instanceof WinSpec))
			return false;
		WinSpec wsp = (WinSpec) spec;
		if (wsp.tagName == null)
			return false;
		return wsp.tagName.equalsIgnoreCase("chart_window") || wsp.tagName.equalsIgnoreCase("query_window");
	}

	/**
	* On the basis of the given specification, re-constructs the corresponding
	* tool. The specification must be an instance of the class
	* spade.vis.spec.MapWindowSpec.
	*/
	@Override
	public void fulfillSpecification(Object spec, DataKeeper dKeeper, Supervisor supervisor, Object visManager, boolean makeMapManipulator) {
		if (spec == null || !(spec instanceof WinSpec))
			return;
		if (!(visManager instanceof DisplayProducer))
			return;
		DisplayProducer dprod = (DisplayProducer) visManager;
		WinSpec winSpec = (WinSpec) spec;
		Frame win = null;
		if (winSpec.tagName.equals("chart_window")) {
			win = dprod.getChartFrame();
		} else if (winSpec.tagName.equals("query_window")) {
			win = dprod.getQueryFrame();
		} else
			return;
		if (win != null) {
			setupWindow(win, winSpec);
		} else {
			if (winSpec.tagName.equals("chart_window")) {
				chartWinSpec = winSpec;
			} else {
				queryWinSpec = winSpec;
			}
			dprod.addWinCreateListener(this);
		}
	}

	private void setupWindow(Frame win, WinSpec winSpec) {
		if (win == null)
			return;
		if (winSpec.properties != null && (win.getComponent(0) instanceof SaveableTool)) {
			((SaveableTool) win.getComponent(0)).setProperties(winSpec.properties);
		}
		if (winSpec.bounds != null) {
			Rectangle scrSize = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
			if (winSpec.bounds.width > scrSize.width) {
				winSpec.bounds.width = scrSize.width;
			}
			if (winSpec.bounds.height > scrSize.height) {
				winSpec.bounds.height = scrSize.height;
			}
			if (winSpec.bounds.x + winSpec.bounds.width > scrSize.width) {
				winSpec.bounds.x = scrSize.width - winSpec.bounds.width;
			}
			if (winSpec.bounds.y + winSpec.bounds.height > scrSize.height) {
				winSpec.bounds.y = scrSize.height - winSpec.bounds.height;
			}
			win.setBounds(winSpec.bounds);
		}
		if (winSpec.title != null) {
			win.setName(winSpec.title);
			win.setTitle(winSpec.title);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getPropertyName().equals("graph_frame")) {
			if (chartWinSpec != null) {
				Frame win = (Frame) e.getNewValue();
				if (win == null)
					return;
				setupWindow(win, chartWinSpec);
				chartWinSpec = null;
			}
			if (queryWinSpec == null) {
				DisplayProducer dprod = (DisplayProducer) e.getSource();
				dprod.removeWinCreateListener(this);
			}
		} else if (e.getPropertyName().equals("query_frame") && queryWinSpec != null) {
			if (queryWinSpec != null) {
				Frame win = (Frame) e.getNewValue();
				if (win == null)
					return;
				setupWindow(win, queryWinSpec);
				queryWinSpec = null;
			}
			if (chartWinSpec == null) {
				DisplayProducer dprod = (DisplayProducer) e.getSource();
				dprod.removeWinCreateListener(this);
			}
		}
	}

}
