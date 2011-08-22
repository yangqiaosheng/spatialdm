package spade.time.ui;

import java.awt.Component;
import java.beans.PropertyChangeListener;

import spade.analysis.system.ESDACore;
import spade.time.FocusInterval;
import spade.time.manage.TemporalDataManager;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.Parameter;

/**
* The interface for a UI component providing access to all system functions
* dealing with time-referenced data
*/
public interface TimeUI {
	/**
	* Sets a reference to the manager of time-referenced data
	*/
	public void setDataManager(TemporalDataManager tdMan);

	/**
	* Sets a reference to the system's core
	*/
	public void setCore(ESDACore core);

	/**
	* Returns true if there is a container (e.g. map layer) with time-referenced
	* objects. This allows one to start time analysis functions.
	*/
	public boolean canStartTimeAnalysis();

	/**
	* Reacts to user's activation of the time functions: displays appropriate
	* dialogs, etc.
	*/
	public void startTimeAnalysis();

	/**
	 * Builds a time line display of the data from the container with the given
	 * identifier
	 */
	public Component buildTimeLineView(String containerId);

	/**
	* Returns the number of available methods for the visualitation and analysis
	* of time-series data.
	*/
	public int getTimeSeriesVisMethodsCount();

	/**
	* Returns the identifier of the method for the visualitation and analysis
	* of time-series data with the given index.
	*/
	public String getTimeSeriesVisMethodId(int idx);

	/**
	* Returns the name of the method for the visualitation and analysis
	* of time-series data with the given index.
	*/
	public String getTimeSeriesVisMethodName(int idx);

	/**
	* Visualizes time-dependent data from the given table assuming that the
	* attributes to visualize and the visualization methods have been previously
	* selected. The selection of the attributes is stored in the argument
	* attrChooser, which is expected to be an instance of the class
	* ui.AttributeChooser. If this argument is null or has inappropriate type,
	* displays a dialog for attribute selection. The visualization method
	* is specified by its identifier passed in the argument visMethodId.
	* If this argument is null, displays a dialog for method selection.
	* Returns 0 if successfully done, 1 if failed or the user cancelled the
	* visualization, and -1 if the button "Back" has been pressed.
	*/
	public int visualizeTable(AttributeDataPortion table, Object attrSelector, String visMethodId);

	/**
	* Retrieves the FocusInterval, which propagates the events of changing the
	* current time moment, from the time control panel. If the panel does not
	* exist yet, creates it.
	* @param par - the temporal parameter
	*/
	public FocusInterval getFocusInterval(Parameter par);

	/**
	 * Returns the FocusInterval, which propagates the events of changing the
	 * current time moment, from the time filter control panel.
	 */
	public FocusInterval getTimeFilterFocusInterval();

	/**
	* Registers a listener of opening or closing of the time control panels
	*/
	public void addPropertyChangeListener(PropertyChangeListener l);

	/**
	* Removes the listener of opening or closing of the time control panels
	*/
	public void removePropertyChangeListener(PropertyChangeListener l);
}
