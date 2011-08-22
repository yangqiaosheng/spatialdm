package spade.vis.event;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.lang.Language;

public class EventLinkEditor extends Panel implements ItemListener, ActionListener {
	static ResourceBundle res = Language.getTextResource("spade.vis.event.Res");
	/**
	* Brokers of specific types of events (each broker is responsible for one
	* event type)
	*/
	protected Vector brokers = null;
	/**
	* The broker corresponding to the currently considered event type (only
	* one event type is considered at a time, and there is a switch between
	* event types if there are more than one brokers)
	*/
	protected EventBroker cbr = null;
	/**
	* Currently considered event receiver
	*/
	protected EventReceiver cer = null;
	/**
	* UI elements
	*/
	protected Panel sendersPanel = null;
	protected List linkList = null;
	protected Button removeLinkButton = null;
	protected Choice evtTypeChoice = null, receiversChoice = null;
	protected Checkbox sendersCB[] = null;

	/**
	* Checks if it is possible to set any links in the given event broker.
	* Links are possible to set if the broker has at least one event source and
	* at least one event receiver registered.
	*/
	public static boolean canSetLinks(EventBroker ebr) {
		return ebr.hasEventSources() && ebr.hasEventReceivers();
	}

	/**
	* From the given vector of event brokers removes the brokers in which
	* one cannot set any links.
	*/
	public static Vector removeNonEditableBrokers(Vector brokers) {
		if (brokers == null || brokers.size() < 1)
			return null;
		for (int i = brokers.size() - 1; i >= 0; i--)
			if (!canSetLinks((EventBroker) brokers.elementAt(i))) {
				brokers.removeElementAt(i);
			}
		if (brokers.size() < 1)
			return null;
		return brokers;
	}

	public EventLinkEditor(Vector brokers) {
		super();
		this.brokers = removeNonEditableBrokers(brokers);
		if (brokers == null)
			return;
		cbr = (EventBroker) brokers.elementAt(0);

		setLayout(new BorderLayout());

		Panel p = new Panel(new ColumnLayout());
		add(p, "North");
		// following string: "Set transmission links for events"
		p.add(new Label(res.getString("Set_transmission")));
		if (brokers.size() > 1) {
			evtTypeChoice = new Choice();
			evtTypeChoice.addItemListener(this);
			for (int i = 0; i < brokers.size(); i++) {
				EventBroker br = (EventBroker) brokers.elementAt(i);
				evtTypeChoice.addItem(br.getEventName());
			}
			p.add(evtTypeChoice);
			evtTypeChoice.select(0);
		} else {
			Label l = new Label(cbr.getEventName(), Label.CENTER);
			p.add(l);
			l.setBackground(Color.darkGray);
			l.setForeground(Color.yellow);
		}
		p.add(new Line(false));
		// following string: "Component"
		p.add(new Label(res.getString("Component")));
		receiversChoice = new Choice();
		receiversChoice.addItemListener(this);
		p.add(receiversChoice);
		// following string: "may receive events from"
		p.add(new Label(res.getString("may_receive_events")));

		sendersPanel = new Panel(new ColumnLayout());
		ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
		scp.add(sendersPanel);
		add(scp, "Center");
		p = new Panel(new ColumnLayout());
		add(p, "South");
		// following string: "Current links:"
		p.add(new Label(res.getString("Current_links_")));
		linkList = new List(5);
		p.add(linkList);
		linkList.addActionListener(this);
		Panel pp = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		// following string: "Remove"
		removeLinkButton = new Button(res.getString("Remove"));
		removeLinkButton.addActionListener(this);
		pp.add(removeLinkButton);
		p.add(pp);

		fillControls();
	}

	/**
	* Fills the UI elements depending on what event type is currently considered,
	* i.e. what broker is the current broker (referenced by the variable cbr).
	*/
	protected void fillControls() {
		if (cbr == null)
			return;
		receiversChoice.removeAll();
		Vector v = cbr.getAllEventReceivers();
		cer = null;
		for (int i = 0; i < v.size(); i++) {
			EventReceiver er = (EventReceiver) v.elementAt(i);
			receiversChoice.addItem(er.getIdentifier());
			if (i == 0) {
				cer = er;
			}
		}
		receiversChoice.select(0);
		linkList.removeAll();
		v = cbr.getAllLinks();
		if (v != null && v.size() > 0) {
			for (int i = 0; i < v.size(); i++) {
				String lk[] = (String[]) v.elementAt(i);
				linkList.add(lk[0] + " >> " + lk[1]);
			}
			removeLinkButton.setEnabled(true);
		} else {
			removeLinkButton.setEnabled(false);
		}
		sendersPanel.removeAll();
		v = cbr.getAllEventSources();
		sendersCB = new Checkbox[v.size()];
		for (int i = 0; i < v.size(); i++) {
			EventSource es = (EventSource) v.elementAt(i);
			sendersCB[i] = new Checkbox(es.getIdentifier(), cbr.isLinkSet(es, cer));
			sendersCB[i].addItemListener(this);
			sendersPanel.add(sendersCB[i]);
		}
		CManager.validateAll(sendersPanel);
	}

	protected void setCheckboxesStates() {
		if (cbr == null || cer == null || sendersCB == null)
			return;
		Vector v = cbr.getAllEventSources();
		for (int i = 0; i < v.size(); i++) {
			EventSource es = (EventSource) v.elementAt(i);
			sendersCB[i].setState(cbr.isLinkSet(es, cer));
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == evtTypeChoice) {
			int idx = evtTypeChoice.getSelectedIndex();
			cbr = (EventBroker) brokers.elementAt(idx);
			fillControls();
		} else if (e.getSource() == receiversChoice) {
			int idx = receiversChoice.getSelectedIndex();
			Vector v = cbr.getAllEventReceivers();
			cer = (EventReceiver) v.elementAt(idx);
			setCheckboxesStates();
		} else if (e.getSource() instanceof Checkbox) {
			int idx = -1;
			for (int i = 0; i < sendersCB.length && idx < 0; i++)
				if (sendersCB[i] == e.getSource()) {
					idx = i;
				}
			if (idx < 0)
				return;
			EventSource es = (EventSource) cbr.getAllEventSources().elementAt(idx);
			if (sendersCB[idx].getState()) {//add a link
				cbr.setLink(es, cer);
				linkList.add(es.getIdentifier() + " >> " + cer.getIdentifier());
			} else { //remove the link
				int n = cbr.getIndexOfLink(es, cer);
				if (n >= 0) {
					linkList.remove(n);
				}
				cbr.breakLink(es, cer);
			}
			removeLinkButton.setEnabled(cbr.hasLinks());
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == removeLinkButton || e.getSource() == linkList) {
			int idx = linkList.getSelectedIndex();
			if (idx < 0)
				return;
			String lk[] = (String[]) cbr.getAllLinks().elementAt(idx);
			cbr.breakLink(lk[0], lk[1]);
			linkList.remove(idx);
			if (lk[1].equals(cer.getIdentifier())) {
				setCheckboxesStates();
			}
		}
	}
}
