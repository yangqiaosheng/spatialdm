package spade.time;

import java.beans.PropertyChangeListener;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 23-Oct-2007
 * Time: 17:02:31
 * Used for selection of time intervals from an appropriate user control
 * or any object when necessary
 */
public interface TimeIntervalSelector {
	public void addTimeSelectionListener(PropertyChangeListener list);

	public void removeTimeSelectionListener(PropertyChangeListener list);

	public TimeMoment getDataIntervalStart();

	public TimeMoment getDataIntervalEnd();

	public TimeMoment getCurrIntervalStart();

	public TimeMoment getCurrIntervalEnd();

	public void selectTimeInterval(TimeMoment start, TimeMoment end);
}
