package spade.time;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import spade.time.manage.TemporalDataManager;
import spade.vis.database.TimeFilter;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 23-Oct-2007
 * Time: 16:15:22
 * Used for selection of time intervals not through the time slider but
 * by other means
 */
public class TimeIntervalSelectorImpl implements PropertyChangeListener, TimeIntervalSelector {
	/**
	* A reference to the manager of time-referenced data
	*/
	protected TemporalDataManager timeMan = null;
	/**
	 * Used for the selection of time intervals
	 */
	protected FocusInterval fint = null;
	/**
	 * Indicates whether the starting time moment is included in the specified
	 * time interval. If not, the items which are only valid until this moment
	 * will be filtered out. By default, the starting moment is included.
	 */
	protected boolean includeIntervalStart = true;
	/**
	 * Indicates whether the ending time moment is included in the specified
	 * time interval. If not, the items which begin to be valid from this moment
	 * will be filtered out. By default, the ending moment is included.
	 */
	protected boolean includeIntervalEnd = true;
	/**
	* Used for handling the list of time selection listeners and notifying them
	* about changes of the current time interval.
	*/
	protected PropertyChangeSupport pcSupport = null;

	/**
	 * Sets whether the starting time moment must be included in the specified
	 * time interval. If not, the items which are only valid until this moment
	 * will be filtered out. By default, the starting moment is included.
	 */
	public void setIncludeIntervalStart(boolean includeIntervalStart) {
		if (includeIntervalStart == this.includeIntervalStart)
			return;
		this.includeIntervalStart = includeIntervalStart;
		if (timeMan != null) {
			for (int i = 0; i < timeMan.getContainerCount(); i++) {
				TimeFilter filter = timeMan.getTimeFilter(i);
				filter.setIncludeIntervalStart(includeIntervalStart);
			}
		}
	}

	/**
	 * Sets whether the ending time moment must be included in the specified
	 * time interval. If not, the items which begin to be valid from this moment
	 * will be filtered out. By default, the ending moment is included.
	 */
	public void setIncludeIntervalEnd(boolean includeIntervalEnd) {
		if (includeIntervalEnd == this.includeIntervalEnd)
			return;
		this.includeIntervalEnd = includeIntervalEnd;
		if (timeMan != null) {
			for (int i = 0; i < timeMan.getContainerCount(); i++) {
				TimeFilter filter = timeMan.getTimeFilter(i);
				filter.setIncludeIntervalEnd(includeIntervalEnd);
			}
		}
	}

	/**
	* Sets a reference to the manager of time-referenced data
	*/
	public void setDataManager(TemporalDataManager tdMan) {
		timeMan = tdMan;
		if (timeMan != null) {
			timeMan.addPropertyChangeListener(this);
		}
		makeFocusInterval();
	}

	protected void makeFocusInterval() {
		if (fint != null) {
			fint.clearTimeLimits();
			fint = null;
			notifyIntervalChange();
		}
		if (timeMan == null || timeMan.getContainerCount() < 1)
			return;
		fint = new FocusInterval();
		fint.setWhatIsFixed(FocusInterval.NONE);
		TimeMoment first = null, last = null;
		char precision = 0;
		for (int i = 0; i < timeMan.getContainerCount(); i++) {
			TimeFilter filter = timeMan.getTimeFilter(i);
			TimeMoment t1 = filter.getEarliestMoment(), t2 = filter.getLatestMoment();
			if (t1 != null && t2 != null) {
				if (first == null || first.compareTo(t1) > 0) {
					first = t1;
				}
				if (last == null || last.compareTo(t2) < 0) {
					last = t2;
				}
				fint.addPropertyChangeListener(filter);
				filter.setIncludeIntervalStart(includeIntervalStart);
				filter.setIncludeIntervalEnd(includeIntervalEnd);
				char prec = t1.getPrecision();
				if (prec != 0)
					if (precision == 0) {
						precision = prec;
					} else if (precision != prec) {
						for (int j = Date.time_symbols.length - 1; j >= 0; j--)
							if (precision == Date.time_symbols[j]) {
								break;
							} else if (prec == Date.time_symbols[j]) {
								precision = prec;
								break;
							}
					}
			}
		}
		if (first == null || last == null)
			return;
		first = first.getCopy();
		last = last.getCopy();
		if (precision != 0) {
			first.setPrecision(precision);
			last.setPrecision(precision);
		}
		fint.setDataInterval(first, last);
		fint.clearTimeLimits();
	}

	/**
	* Reacts to changes in the TemporalDataManager
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(timeMan))
			if (e.getPropertyName().equals("all_data_removed")) {
				if (fint != null) {
					fint.setCurrInterval(null, null);
					fint = null;
					notifyIntervalChange();
				}
			} else if (e.getPropertyName().equals("data_appeared") || e.getPropertyName().equals("data_added") || e.getPropertyName().equals("table_updated") || e.getPropertyName().equals("container_updated")
					|| e.getPropertyName().equals("table_removed") || e.getPropertyName().equals("container_removed")) {
				makeFocusInterval();
			}
	}

	@Override
	public TimeMoment getCurrIntervalStart() {
		if (fint == null)
			return null;
		return fint.getCurrIntervalStart();
	}

	@Override
	public TimeMoment getCurrIntervalEnd() {
		if (fint == null)
			return null;
		return fint.getCurrIntervalEnd();
	}

	@Override
	public void selectTimeInterval(TimeMoment start, TimeMoment end) {
		if (fint == null)
			return;
		if (fint.setCurrInterval(start, end)) {
			notifyIntervalChange();
		}
	}

	@Override
	public TimeMoment getDataIntervalStart() {
		if (fint == null)
			return null;
		return fint.getDataIntervalStart();
	}

	@Override
	public TimeMoment getDataIntervalEnd() {
		if (fint == null)
			return null;
		return fint.getDataIntervalEnd();
	}

	/**
	* Registers a time selection listener
	*/
	@Override
	public synchronized void addTimeSelectionListener(PropertyChangeListener l) {
		if (l == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(l);
	}

	/**
	* Removes the time selection listener
	*/
	@Override
	public synchronized void removeTimeSelectionListener(PropertyChangeListener l) {
		if (l == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(l);
	}

	/**
	* Used for notifying the time selection listeners
	* about changes of the current time interval.
	*/
	protected void notifyIntervalChange() {
		if (pcSupport == null)
			return;
		pcSupport.firePropertyChange("current_interval", getCurrIntervalStart(), getCurrIntervalEnd());
	}
}
