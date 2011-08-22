package spade.time.ui;

import java.awt.Checkbox;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Line;
import spade.lib.lang.Language;
import spade.time.Animator;
import spade.time.FocusInterval;
import spade.vis.spec.SaveableTool;
import spade.vis.spec.WinSpec;

/**
* Contains UI elements for choosing current time moment or interval and for
* controlling animation.
*/
public class TimeControlPanel extends Panel implements Destroyable, SaveableTool, PropertyChangeListener, ItemListener {
	static ResourceBundle res = Language.getTextResource("spade.time.ui.Res");
	/**
	* The focus time interval manipulated through this control panel
	*/
	protected FocusInterval focusInt = null;
	/**
	* A sub-panel with the time slider
	*/
	protected TimeSliderPanel slPan = null;
	/**
	* A sub-panel with the animation controls.
	*/
	protected AnimationPanel animPan = null;
	/**
	 * A sub-panel for selection of time intervals through clicking
	 * in time-aware data displays
	 */
	protected TimeIntervalSelectorPanel intSelPan = null;
	/**
	 * A reference to another TimeControlPanel, which may be a master of this
	 * TimeControlPanel. This means that this TimeControlPanel will synchronise
	 * its animation with master's animation when the user sets the appropriate
	 * mode.
	 */
	protected TimeControlPanel master = null;
	/**
	 * Used to switch on and off the synchronisation of the animation
	 * in this panel with the animation in another panel
	 */
	protected Checkbox slaveCB = null;
	/**
	* The owner of the frame. In particular, the owner must be notified when the
	* panel is destroyed.
	*/
	protected PropertyChangeListener owner = null;
	/**
	* As a SaveableTool, a visualizer may be registered somewhere and, hence, must
	* notify the component where it is registered about its destroying. This vector
	* contains the listeners to be notified about destroying of the visualizer.
	*/
	protected Vector destroyListeners = null;

	protected boolean destroyed = false;
	/**
	 * The keyword used in the opening tag of a stored state description
	 * of this tool. This may be either "time_controls", if the panel is used for
	 * controlling the display time in visualisations of time-series data, or
	 * "time_filter_controls", if the panel is used for controlling a temporal
	 * filter of events or movement data.
	 * The default value is "time_controls".
	 */
	protected String tagName = "time_controls";

	/**
	* Constructs the panel for the given time focuser.
	*/
	public TimeControlPanel(FocusInterval fint, PropertyChangeListener owner, int sliderStep, boolean maySelectIntervals) {
		this.owner = owner;
		focusInt = fint;
		setLayout(new ColumnLayout());
		String name = (maySelectIntervals) ? "Time filter" : "Display time controls";
		add(new Label(name, Label.CENTER));
		TimeSlider tsl = new TimeSlider(fint, sliderStep);
		slPan = new TimeSliderPanel(tsl, owner, maySelectIntervals);
		add(slPan);
		animPan = new AnimationPanel(fint, sliderStep);
		add(animPan);
		setName(name);
		if (maySelectIntervals) {
			tagName = "time_filter_controls";
			intSelPan = new TimeIntervalSelectorPanel();
			intSelPan.setFocusInterval(focusInt);
			add(new Line(false));
			add(intSelPan);
		}
	}

	public FocusInterval getFocusInterval() {
		return slPan.getFocusInterval();
	}

	/**
	 * The supervisor can send notifications about coloring of time moments
	 */
	public void setSupervisor(Supervisor supervisor) {
		slPan.getTimeSlider().setSupervisor(supervisor);
		if (intSelPan != null) {
			intSelPan.setSupervisor(supervisor);
		}
	}

	/**
	 * Sets a reference to another TimeControlPanel, which may be a master of this
	 * TimeControlPanel. This means that this TimeControlPanel will synchronise
	 * its animation with master's animation when the user sets the appropriate
	 * mode.
	 * Adds a checkbox to switch on and off the "slave" (synchronisation) mode.
	 */
	public void setMaster(TimeControlPanel master) {
		this.master = master;
		if (master != null) {
			master.addDestroyingListener(this);
			focusInt.addPropertyChangeListener(this);
			Dimension oldPrefSize = getPreferredSize();
			slaveCB = new Checkbox("Slave mode", false);
			slaveCB.addItemListener(this);
			add(slaveCB);
			invalidate();
			Window win = CManager.getWindow(this);
			if (win != null && oldPrefSize != null) {
				Dimension s = getPreferredSize();
				int dy = s.height - oldPrefSize.height;
				s = win.getSize();
				win.setSize(s.width, s.height + dy);
			}
			CManager.validateAll(slaveCB);
		}
	}

	public Animator getAnimator() {
		return animPan.getAnimator();
	}

//------------------ interface SaveableTool -----------------------------------
	/**
	* Returns the keyword used in the opening tag of a stored state description
	* of this tool. A tool state description (specification) is stored as a
	* sequence of lines starting with <tagName> and ending with </tagName>, where
	* tagName is a unique keyword for a particular class of tools.
	*/
	@Override
	public String getTagName() {
		return tagName;
	}

	/**
	* Returns the specification (i.e. state description) of this tool for storing
	* in a file. The specification must allow correct re-construction of the tool.
	*/
	@Override
	public Object getSpecification() {
		WinSpec spec = new WinSpec();
		spec.tagName = getTagName();
		Window win = CManager.getWindow(this);
		if (win != null) {
			spec.bounds = win.getBounds();
			if (win instanceof Frame) {
				spec.title = ((Frame) win).getTitle();
			} else {
				spec.title = win.getName();
			}
		}
		if (slPan != null) {
			spec.properties = slPan.getProperties();
		}
		if (animPan != null) {
			spec.properties = animPan.getProperties(spec.properties);
		}
		return spec;
	}

	/**
	* After the tool is constructed, it may be requested to setup its individual
	* properties according to the given list of stored properties.
	*/
	@Override
	public void setProperties(Hashtable properties) {
		if (properties != null) {
			if (slPan != null) {
				slPan.setProperties(properties);
			}
			if (animPan != null) {
				animPan.setProperties(properties);
			}
		}
	}

	/**
	* Adds a listener to be notified about destroying the tool.
	* A SaveableTool may be registered somewhere and, hence, must notify the
	* component where it is registered about its destroying.
	*/
	@Override
	public void addDestroyingListener(PropertyChangeListener lst) {
		if (lst == null)
			return;
		if (destroyListeners == null) {
			destroyListeners = new Vector(5, 5);
		}
		if (!destroyListeners.contains(lst)) {
			destroyListeners.addElement(lst);
		}
	}

	public void removeDestroyListener(PropertyChangeListener lst) {
		if (lst == null || destroyListeners == null)
			return;
		destroyListeners.removeElement(lst);
	}

	/**
	* Destroys the time controls when they are no more used, in particular, stops
	* listening to events. Destroys the subcomponents.
	* Besides, sends a PropertyChangeEvent with the name "destroyed" to its
	* owner and other destroying listener(s), @see addDestroyingListener.
	*/
	@Override
	public void destroy() {
		if (isDestroyed())
			return;
		slPan.destroy();
		animPan.destroy();
		if (intSelPan != null) {
			intSelPan.destroy();
		}
		if (master != null) {
			master.removeDestroyListener(this);
			focusInt.removePropertyChangeListener(this);
			master = null;
		}
		destroyed = true;
		PropertyChangeEvent pce = new PropertyChangeEvent(this, "destroyed", null, null);
		if (owner != null) {
			owner.propertyChange(pce);
		}
		if (destroyListeners != null) {
			for (int i = 0; i < destroyListeners.size(); i++) {
				((PropertyChangeListener) destroyListeners.elementAt(i)).propertyChange(pce);
			}
		}
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	/**
	 * Reacts to its master being destroyed
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(master) && e.getPropertyName().equals("destroyed")) {
			master.removeDestroyListener(this);
			master = null;
			focusInt.removePropertyChangeListener(this);
			if (slaveCB != null) {
				Dimension oldPrefSize = getPreferredSize();
				Window win = CManager.getWindow(this);
				remove(slaveCB);
				slaveCB = null;
				invalidate();
				if (win != null && oldPrefSize != null) {
					validate();
					Dimension s = getPreferredSize();
					int dy = s.height - oldPrefSize.height;
					s = win.getSize();
					win.setSize(s.width, s.height + dy);
				}
				CManager.validateAll(this);
			}
		} else if (e.getSource().equals(focusInt) && slaveCB != null && e.getPropertyName().equals("animation")) {
			if (e.getNewValue().equals("start")) {
				slaveCB.setEnabled(false);
			} else if (e.getNewValue().equals("stop")) {
				slaveCB.setEnabled(true);
			}
		}
	}

	/**
	 * Reacts to switching on/off the "slave" mode
	 */
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(slaveCB) && master != null) {
			boolean slave = slaveCB.getState();
			animPan.setEnabled(!slave);
			slPan.setSlaveMode(slave);
			if (slave) {
				master.getAnimator().setSlaveAnimator(getAnimator());
				animPan.updateUI();
			} else {
				master.getAnimator().setSlaveAnimator(null);
			}
		}
	}
}