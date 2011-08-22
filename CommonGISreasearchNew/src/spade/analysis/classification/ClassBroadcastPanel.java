package spade.analysis.classification;

import java.awt.Checkbox;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.lang.Language;

/**
* This panel may be added to any class manipulator. It contains control for
* broadcasting of the classes (i.e. propagating colors of objects among
* the graphical displays)
*/

public class ClassBroadcastPanel extends Panel implements ClassOperator, ItemListener, Destroyable, PropertyChangeListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.classification.Res");
	private Checkbox broadcastCB = null;

	protected Classifier classifier = null;
	protected Supervisor supervisor = null;

	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;

	@Override
	public boolean construct(Classifier classifier, Supervisor sup) {
		if (classifier == null || sup == null)
			return false;
		this.classifier = classifier;
		this.supervisor = sup;
		setLayout(new ColumnLayout());
		// following text: "Broadcast classification"
		broadcastCB = new Checkbox(res.getString("Broadcast"), false);
		broadcastCB.addItemListener(this);
		add(broadcastCB);
		supervisor.addPropertyChangeListener(this);
		return true;
	}

	public void replaceClassifier(Classifier clNew) {
		if (clNew == null || clNew.equals(classifier))
			return;
		if (classifier != null && broadcastCB.getState()) {
			supervisor.removeObjectColorer(classifier);
		}
		classifier = clNew;
		if (broadcastCB.getState()) {
			supervisor.setObjectColorer(classifier);
		}
	}

	/*
	* broadcasting state method
	*/
	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (supervisor == null || classifier == null)
			return;
		Object o = ie.getSource();
		if (o == broadcastCB) {
			if (broadcastCB.getState()) { // enable broadcasting
				supervisor.setObjectColorer(classifier);
			} else { // disable broadcasting
				supervisor.removeObjectColorer(classifier);
			}
		}
	}

	/**
	* Reacts to supervisor's notifications about changing colors. In particular,
	* checks whether the source of the colors (i.e. the current classifier in the
	* supervisor) has changed. If so, sets the state of the checkbox depending
	* on whether or not this classifier is currently used.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource().equals(supervisor) && pce.getPropertyName().equals(Supervisor.eventObjectColors)) {
			broadcastCB.setState(classifier.equals(supervisor.getObjectColorer()));
		}
	}

	@Override
	public void destroy() {
		// disable broadcasting
		supervisor.removeObjectColorer(classifier);
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
