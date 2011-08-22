package spade.analysis.classification;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.RowLayout;

/**
 * BroadcastClassesCP (Control Panel) listens to broadcasting classification.
 * The control allows selection of classes for displaying.
 * Registered listeners receive boolean[] that indicates which classes
 * should be displayed. First item corresponds to "remainder" (non-classified
 * objects), then all classes
 */
public class BroadcastClassesCP extends Panel implements PropertyChangeListener, ActionListener, ItemListener, Destroyable {
	protected Supervisor supervisor = null;
	protected String eventName = null;
	/**
	* The identifier of the relevant object set. The panel must not react to
	* "foreign" events, i.e. events referring to other object sets.
	*/
	protected String entitySetId = null;

	protected String prefix = null;

	protected boolean initialState = false, colorTexts = false, colorTextsBkg = true;
	protected String classNames[] = null;
	protected Checkbox cbShowClasses[] = null;
	protected Button bPlus = null, bMinus = null;

	/**
	* objectSetId: the identifier of the relevant object set. The panel must not
	*       react to foreign" events, i.e. events referring to other object sets.
	* eventName: event that will be passed to listeners
	* initialState: initial state of checkboxes
	* colorText: whether checkbox texts should be colored
	* colorTextBkg: whether checkbox backgrouns should be colored
	*/
	public BroadcastClassesCP(String objectSetId, String prefix, String eventName, boolean initialState, boolean colorTexts, boolean colorTextsBkg) {
		super();
		entitySetId = objectSetId;
		this.eventName = eventName;
		this.prefix = prefix;
		this.initialState = initialState;
		this.colorTexts = colorTexts;
		this.colorTextsBkg = colorTextsBkg;
		createPanel();
	}

	protected void createPanel() {
		removeAll();
		Panel p = this;
		if (eventName.equals("eventShowClassesFlows")) {
			setLayout(new BorderLayout());
			p = new Panel();
			add(p, BorderLayout.NORTH);
		}
		p.setLayout(new RowLayout(0, 0));
		if (classNames != null && classNames.length > 0) {
			p.add(new Label(prefix, Label.LEFT));
			p.add(bPlus = new Button("+"));
			bPlus.addActionListener(this);
			p.add(bMinus = new Button("-"));
			bMinus.addActionListener(this);
			cbShowClasses = new Checkbox[classNames.length];
			for (int i = 0; i < classNames.length; i++) {
				p.add(cbShowClasses[i] = new Checkbox(classNames[i], initialState));
				cbShowClasses[i].addItemListener(this);
			}
			setVisible(true);
			CManager.validateAll(this);
		}
	}

	/**
	* registers to supervisor as listener
	*/
	public void setSupervisor(Supervisor supervisor) {
		this.supervisor = supervisor;
		if (supervisor != null) {
			supervisor.addPropertyChangeListener(this);
		}
		processEventObjectColors();
	}

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getPropertyName().equals(Supervisor.eventObjectColors)) {
			processEventObjectColors();
			return;
		}
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (ie.getSource() instanceof Checkbox && cbShowClasses != null) {
			for (Checkbox cbShowClasse : cbShowClasses)
				if (cbShowClasse.equals(ie.getSource())) {
					setShowClasses(false);
					return;
				}
		}
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		boolean anyChanged = false;
		if (ae.getSource().equals(bPlus)) {
			for (int i = 0; i < cbShowClasses.length; i++)
				if (!cbShowClasses[i].getState()) {
					cbShowClasses[i].setState(true);
					anyChanged = true;
				}
		}
		if (ae.getSource().equals(bMinus)) {
			for (Checkbox cbShowClasse : cbShowClasses)
				if (cbShowClasse.getState()) {
					cbShowClasse.setState(false);
					anyChanged = true;
				}
		}
		if (anyChanged) {
			setShowClasses(false);
		}
	}

	protected void processEventObjectColors() {
		ObjectColorer colorer = (supervisor == null) ? null : supervisor.getObjectColorer();
		if (colorer == null || !(colorer instanceof Classifier) || !colorer.getEntitySetIdentifier().equals(entitySetId)) {
			// Broadcast classification has been switched of
			if (cbShowClasses == null)
				return;
			removeAll();
			CManager.validateAll(this);
			classNames = null;
			cbShowClasses = null;
			setShowClasses(true);
			return;
		}
		Classifier classifier = (Classifier) colorer;
		boolean needUpdate = classNames == null || classNames.length != classifier.getNClasses() + 1;
		if (needUpdate) {
			classNames = new String[classifier.getNClasses() + 1];
			classNames[classNames.length - 1] = "remainder";
			for (int i = 0; i < classNames.length - 1; i++) {
				classNames[i] = classifier.getClassName(i);
			}
			createPanel();
			setShowClasses(true);
		} else {
			boolean classesChanged = false;
			int i = classNames.length - 1;
			classNames[i] = "remainder";
			cbShowClasses[i].setLabel(classNames[i]);
			for (i = 0; i < classifier.getNClasses(); i++) {
				boolean changed = (i < classifier.getNClasses()) && (classNames[i] == null || !classNames[i].equals(classifier.getClassName(i)));
				classesChanged |= changed;
				if (changed) {
					classNames[i] = classifier.getClassName(i);
					cbShowClasses[i].setLabel(classNames[i]);
				}
			}
			if (classesChanged) {
				CManager.validateAll(cbShowClasses[0]);
			}
			setShowClasses(classesChanged);
		}
		for (int i = 0; i < classNames.length; i++) {
			Color c = (i < classNames.length - 1) ? classifier.getClassColor(i) : new Color(160, 160, 160);
			boolean isBright = c.getGreen() > 127;
			//if (colorTexts) cbShowClasses[i].setForeground(c);
			if (colorTextsBkg) {
				cbShowClasses[i].setBackground(c);
				cbShowClasses[i].setForeground((isBright) ? Color.black : Color.white);
			}
		}
	}

	protected void setShowClasses(boolean classesChanged) {
		boolean flags[] = null, anySelected = false;
		if (cbShowClasses != null && cbShowClasses.length > 0) {
			flags = new boolean[cbShowClasses.length];
			for (int i = 0; i < flags.length - 1; i++) {
				flags[i + 1] = cbShowClasses[i].getState();
			}
			flags[0] = cbShowClasses[flags.length - 1].getState();
		}
		BroadcastClassesCPinfo bccpi = new BroadcastClassesCPinfo();
		bccpi.eventName = eventName;
		bccpi.classesChanged = classesChanged;
		bccpi.showClasses = flags;
		if (pcSupport != null) {
			pcSupport.firePropertyChange(eventName, null, bccpi);
		}
	}

	//-------------------- broadcasting property changes -------------------------
	protected PropertyChangeSupport pcSupport = null;

	@Override
	public void addPropertyChangeListener(PropertyChangeListener list) {
		if (list == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(list);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener list) {
		if (list == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(list);
	}

	//-------------------- Destroyable interface ---------------------------------
	/**
	* Stops listening of all events, unregisters itself from object event sources
	*/
	protected boolean destroyed = false;

	@Override
	public void destroy() {
		if (supervisor != null) {
			supervisor.removePropertyChangeListener(this);
		}
		destroyed = true;
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

}
