package spade.time.vis;

import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;

import spade.analysis.manipulation.Manipulator;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.lang.Language;
import spade.time.FocusInterval;
import spade.time.TimeMoment;
import spade.time.ui.TimeSlider;
import spade.time.ui.TimeSliderPanel;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.Parameter;

/**
* Used to manipulate a ValueFlowVisualizer, which represents the value flow of
* a numeric attribute by a polygon built from a time-series graph. Such polygons
* are put on the map as diagrams. The manipulator allows to change the time
* extent represented on the map by the diagrams.
*/
public class ValueFlowManipulator extends Panel implements Manipulator, PropertyChangeListener, ActionListener {
	static ResourceBundle res = Language.getTextResource("spade.time.vis.Res");
	/**
	* The ValueFlowVisualizer to manipulate
	*/
	protected ValueFlowVisualizer vfVis = null;
	/**
	* Used for changing the temporal extent of the visualization
	*/
	protected FocusInterval focusInterval = null;
	/**
	* Button for setting the full time extent
	*/
	protected Button bStartEnd = null;

	/**
	* Construction of map manipulator. Returns true if successfully constructed.
	* Arguments: 1) supervisor responsible for dynamic linking of displays;
	* 2) visualizer or classifier to be manipulated; 3) table with source data;
	* 4) table filter
	* The Manipulator must check if the visualizer has the appropriate type.
	*/
	@Override
	public boolean construct(Supervisor sup, Object visualizer, AttributeDataPortion table) {
		if (visualizer == null || !(visualizer instanceof ValueFlowVisualizer))
			return false;
		vfVis = (ValueFlowVisualizer) visualizer;
		Parameter tPar = vfVis.getTemporalParameter();
		if (tPar == null)
			return false;
		focusInterval = new FocusInterval();
		TimeMoment t0 = (TimeMoment) tPar.getFirstValue(), t = (TimeMoment) tPar.getLastValue();
		focusInterval.setDataInterval(t0, t);
		TimeMoment t1 = vfVis.getTimeFocusStart(), t2 = vfVis.getTimeFocusEnd();
		if (t1 != null || t2 != null) {
			if (t1 == null) {
				t1 = t0;
			}
			if (t2 == null) {
				t2 = t;
			}
			focusInterval.setCurrInterval(t1, t2);
		}
		focusInterval.addPropertyChangeListener(this);
		TimeSlider timeSlider = new TimeSlider(focusInterval);
		timeSlider.setSupervisor(sup);
		TimeSliderPanel tspan = new TimeSliderPanel(timeSlider, this, true);
		bStartEnd = new Button(tPar.getFirstValue().toString() + ".." + tPar.getLastValue().toString());
		bStartEnd.addActionListener(this);
		bStartEnd.setActionCommand("full_extent");
		bStartEnd.setEnabled(false);

		setLayout(new ColumnLayout());
		add(new Label(vfVis.getAttributeName(), Label.CENTER));
		add(new Label(res.getString("time_extent") + ":"));
		add(tspan);
		Panel p = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 5));
		p.add(bStartEnd);
		add(p);
		return true;
	}

	/**
	* Reacts to pressing the button for setting the whole time extent
	*/
	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd == null)
			return;
		if (cmd.equals("full_extent")) {
			focusInterval.showWholeInterval();
		}
	}

	/**
	* Reacts to changing the temporal extent of the visualization
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource() instanceof FocusInterval) {
			if (e.getPropertyName().equalsIgnoreCase("current_interval")) {
				TimeMoment t1 = (TimeMoment) e.getOldValue(), t2 = (TimeMoment) e.getNewValue();
				if (t2.equals(t1))
					return;
				vfVis.setTimeFocusStartEnd(t1, t2);
				vfVis.notifyVisChange();
				bStartEnd.setEnabled(vfVis.getCurrFirstMomentIdx() > 0 || vfVis.getCurrLastMomentIdx() < vfVis.getTotalMomentCount() - 1);
			}
		}
	}
}
