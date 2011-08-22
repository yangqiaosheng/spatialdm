package spade.time;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.Destroyable;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Aug 19, 2009
 * Time: 3:47:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class TimePositionNotifier implements PropertyChangeListener, Destroyable {

	protected int mouseX = -1;

	protected TimeMoment mouseTime = null;

	/**
	 * for trajectory data: last time moment in data before the mouseTime,
	 * geographic positions and attribute value, if any
	 */
	public String lastId = null;
	public TimeMoment lastDataTime = null;
	public double lastMapX = Double.NaN, lastMapY = Double.NaN;
	public double lastValue = Double.NaN;
	/**
	 * Through the supervisor, the time position notifier can notify all
	 * interested components about the change od the current time
	 */
	protected Supervisor supervisor = null;

	/**
	 * For clusters
	 * 
	 */
	public String lastC_id = "";

	public void copyTo(TimePositionNotifier tPN) {
		if (tPN == null)
			return;
		tPN.mouseTime = mouseTime;
		tPN.lastId = lastId;
		tPN.lastDataTime = lastDataTime;
	}

	public int getMouseX() {
		return mouseX;
	}

	public void setMouseX(int mouseX) {
		this.mouseX = mouseX;
	}

	public TimeMoment getMouseTime() {
		return mouseTime;
	}

	public void setMouseTime(TimeMoment mouseTime) {
		this.mouseTime = mouseTime;
	}

	/**
	* The list of property change listeners to be notified
	* about changes of the current moment or interval.
	*/
	protected Vector listeners = null;

	/**
	* Registers a listener of changes of the current moment or interval
	*/
	public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
		if (l == null)
			return;
		if (listeners == null) {
			listeners = new Vector(5, 5);
		}
		if (!listeners.contains(l)) {
			listeners.addElement(l);
		}
	}

	/**
	* Removes the listener of changes of the current moment or interval
	*/
	public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
		if (l == null || listeners == null)
			return;
		listeners.removeElement(l);
	}

	/**
	 * Through the supervisor, the time position notifier can notify all
	 * interested components about the change od the current time
	 */
	public void setSupervisor(Supervisor supervisor) {
		this.supervisor = supervisor;
		if (supervisor != null) {
			supervisor.addPropertyChangeListener(this);
		}
	}

	public void notifyTimeSelection(TimeMoment t) {
		if (supervisor != null && t != null) {
			supervisor.notifyGlobalPropertyChange("time_moment_selection", t);
		}
	}

	public void notifyPositionChange(Object src) {
		if (supervisor != null) {
			TimePositionNotifier tpn = new TimePositionNotifier();
			tpn.lastId = this.lastId;
			tpn.setMouseTime(this.getMouseTime());
			supervisor.notifyGlobalPropertyChange("current_moment", tpn);
		}
		if (listeners == null || listeners.size() < 1)
			return;
		PropertyChangeEvent pce = new PropertyChangeEvent(this, "current_moment", src, this);
		for (int i = 0; i < listeners.size(); i++) {
			((PropertyChangeListener) listeners.elementAt(i)).propertyChange(pce);
/*
		System.out.println("Time position notified: t="+mouseTime+", obj="+lastId+
		  ", map X="+lastMapX+", mapY="+lastMapY);
*/
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(supervisor) && e.getPropertyName().equals("current_moment") && (e.getNewValue() instanceof TimePositionNotifier)) {
			if (listeners == null || listeners.size() < 1)
				return;
			TimePositionNotifier tPN = (TimePositionNotifier) e.getNewValue();
			if (this.equals(tPN))
				return;
			if (tPN.getMouseTime() == null && this.mouseTime == null)
				return;
			if (tPN.getMouseTime() != null && tPN.getMouseTime().equals(this.mouseTime))
				return;
			PropertyChangeEvent pce = new PropertyChangeEvent(this, "current_moment", supervisor, tPN);
			for (int i = 0; i < listeners.size(); i++) {
				((PropertyChangeListener) listeners.elementAt(i)).propertyChange(pce);
			}
		}
	}

	protected boolean destroyed = false;

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	@Override
	public void destroy() {
		if (destroyed)
			return;
		if (supervisor != null) {
			supervisor.removePropertyChangeListener(this);
		}
		listeners = null;
		destroyed = true;
	}

}
