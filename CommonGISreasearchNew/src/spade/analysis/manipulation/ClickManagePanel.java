package spade.analysis.manipulation;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.Destroyable;
import spade.lib.lang.Language;
import spade.vis.action.ObjectEvent;
import spade.vis.event.DEvent;
import spade.vis.event.EventConsumer;
import spade.vis.event.EventReceiver;

public class ClickManagePanel extends Panel implements ItemListener, EventConsumer, Destroyable {
	/**
	* Used to generate unique identifiers of instances of ClickManagePanel
	*/
	static ResourceBundle res = Language.getTextResource("spade.analysis.manipulation.Res");
	protected static int nInstances = 0;
	protected int instanceN = 0;

	protected Checkbox clickCB = null;
	protected Choice displayChoice = null;
	protected Supervisor sup = null;
	protected static String evtType = ObjectEvent.click, evtMeaning = "compare",
	//following text: "visual comparison"
			evtMeaningText = res.getString("visual_comparison");
	protected boolean listenAllDisplays = false;
	protected Object mapToListen = null;
	protected EventReceiver comparer = null;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;

	public ClickManagePanel(Supervisor supervisor) {
		super();
		instanceN = ++nInstances;
		sup = supervisor;
		setLayout(new GridLayout(2, 1, 0, 0));
		// following text: "compare by click in"
		clickCB = new Checkbox(res.getString("compare_by_click_in"), false);
		clickCB.addItemListener(this);
		add(clickCB);
		displayChoice = new Choice();
		displayChoice.setEnabled(false);
		// following text: "this map"
		displayChoice.addItem(res.getString("this_map"));
		// following text:"any display"
		displayChoice.addItem(res.getString("any_display"));
		displayChoice.addItemListener(this);
		Panel p = new Panel(new BorderLayout());
		p.add(displayChoice, "East");
		add(p);
	}

	/**
	* A ClickManagePanel may process object click events either from all displays
	* or from the map its manipulator is attached to. This map is a primary
	* event source for this component.
	*/
	public void setPrimaryEventSource(Object evtSource) {
		mapToListen = evtSource;
	}

	public void setComparer(EventReceiver comp) {
		comparer = comp;
	}

	/**
	* The Event Consumer answers whether it consumes the specified event
	* when it has the given meaning.
	*/
	@Override
	public boolean doesConsumeEvent(DEvent evt, String eventMeaning) {
		return (listenAllDisplays || evt.getSource() == mapToListen) && doesConsumeEvent(evt.getId(), eventMeaning);
	}

	/**
	* The Event Consumer answers whether it consumes the specified event
	* when it has the given meaning.
	*/
	@Override
	public boolean doesConsumeEvent(String eType, String eventMeaning) {
		return comparer != null && clickCB.getState() && eType.equals(evtType) && eventMeaning.equals(evtMeaning);
	}

	/**
	* The EventReceiver answers whether it is interested in getting the specified
	* kind of events.
	*/
	@Override
	public boolean doesListenToEvent(String eventId) {
		return comparer != null && clickCB.getState() && eventId.equals(evtType);
	}

	/**
	* This method is used for delivering events to the Event Receiver.
	*/
	@Override
	public void eventOccurred(DEvent evt) {
		if (comparer != null && clickCB.getState() && (listenAllDisplays || evt.getSource() == mapToListen)) {
			comparer.eventOccurred(evt);
		}
	}

	/**
	* Returns a unique identifier of the component
	* (used only internally, not shown to the user).
	* The identifier is used for explicit linking of producers and recipients of
	* object events.
	*/
	@Override
	public String getIdentifier() {
		return "ClickManager_" + instanceN;
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == clickCB)
			if (clickCB.getState()) {
				displayChoice.setEnabled(true);
				if (sup != null) {
					sup.registerObjectEventConsumer(this, evtType, evtMeaning, evtMeaningText);
				}
			} else {
				displayChoice.setEnabled(false);
				if (sup != null) {
					sup.removeObjectEventConsumer(this, evtType, evtMeaning);
				}
			}
		else if (e.getSource() == displayChoice) {
			listenAllDisplays = displayChoice.getSelectedIndex() > 0;
		}
	}

	@Override
	public void destroy() {
		if (sup != null) {
			sup.removeObjectEventConsumer(this, evtType, evtMeaning);
		}
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
