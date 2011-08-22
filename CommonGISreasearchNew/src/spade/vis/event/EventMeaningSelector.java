package spade.vis.event;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.util.IntArray;

public class EventMeaningSelector extends Panel implements ItemListener, PropertyChangeListener, Destroyable {
	protected EventMeaningManager evtMan = null;
	protected Vector checkBoxes = null;
	protected IntArray evN = null, meanN = null;
	protected String prompt = null;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;

	public static boolean hasMultipleMeanings(EventMeaningManager man) {
		if (man == null)
			return false;
		if (man.getEventCount() < 1)
			return false;
		for (int i = 0; i < man.getEventCount(); i++)
			if (man.getNEventMeanings(i) > 1)
				return true;
		return false;
	}

	public EventMeaningSelector(EventMeaningManager man, String prompt) {
		super();
		evtMan = man;
		this.prompt = prompt;
		if (man == null)
			return;
		man.addPropertyChangeListener(this);
		setLayout(new ColumnLayout());
		makeInterface();
	}

	protected void makeInterface() {
		if (checkBoxes == null) {
			checkBoxes = new Vector(10, 10);
		} else {
			checkBoxes.removeAllElements();
		}
		if (evN == null) {
			evN = new IntArray(10, 10);
		} else {
			evN.removeAllElements();
		}
		if (meanN == null) {
			meanN = new IntArray(10, 10);
		} else {
			meanN.removeAllElements();
		}

		removeAll();

		if (prompt != null) {
			add(new Label(prompt, Label.CENTER));
		}

		for (int i = 0; i < evtMan.getEventCount(); i++)
			if (evtMan.getNEventMeanings(i) > 1) {
				EventData ed = evtMan.getEventData(i);
				add(new Label(ed.getName() + ":"));
				CheckboxGroup cbg = new CheckboxGroup();
				for (int j = 0; j < ed.getNMeanings(); j++) {
					Checkbox cb = new Checkbox(ed.getMeaningText(j), j == ed.getCurrentMeaningN(), cbg);
					cb.addItemListener(this);
					add(cb);
					checkBoxes.addElement(cb);
					evN.addElement(i);
					meanN.addElement(j);
				}
			}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (checkBoxes == null)
			return;
		for (int i = 0; i < checkBoxes.size(); i++)
			if (e.getSource() == checkBoxes.elementAt(i)) {
				Checkbox cb = (Checkbox) e.getSource();
				if (cb.getState()) {
					evtMan.setCurrentEventMeaning(evN.elementAt(i), meanN.elementAt(i));
				}
				return;
			}
	}

	/**
	* The EventMeaningSelector listens to property changes of the corresponding
	* EventMeaningManager, i.e. to adding or removal of event meanings.
	* When some change occurs, the EventMeaningSelector should reflect the changes
	* in its interface.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		makeInterface();
		invalidate();
		getParent().validate();
	}

	/**
	* Removes itself from listeners of the event meaning manager
	*/
	@Override
	public void destroy() {
		evtMan.removePropertyChangeListener(this);
		destroyed = true;
	}

	/**
	* Replies whether is destroyed or not
	*/
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}
}
