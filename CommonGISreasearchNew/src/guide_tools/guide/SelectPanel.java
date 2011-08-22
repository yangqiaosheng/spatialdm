package guide_tools.guide;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import spade.analysis.system.SystemManager;
import spade.kbase.scenarios.ContextElement;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.DialogContent;

/**
* This UI element is used when definition of a context element requires
* selection from a number of options.
* It proposes the user the options as checkboxes or radio buttons.
* The panel is constructed on the basis of a given ContextElement
* (specification of the element). Depending on the method of definition
* ("select_one" or "select_many"), allows single or multiple selections
*/

public class SelectPanel extends Panel implements DialogContent, ActionListener {
	/**
	* The Vector "options" contains the options (Objects), and the Vector
	* optCB contains corresponding checkboxes
	*/
	protected Vector options = null, optCB = null;
	/**
	* The ContextElement according to which the panel is constructed
	*/
	protected ContextElement cel = null;
	/**
	* Identifier of the task the context element is required for
	*/
	protected String taskId = null;
	protected SystemManager sysman = null;
	protected int mapN = 0;
	/**
	* The context that has been defined up to now
	*/
	protected Context contextDef = null;
	/**
	* Shows whether specification of the context element is optional, i.e. the
	* user may leave all the options unselected.
	*/
	protected boolean optional = false;
	/**
	* This variable contains the error message that can be generated during
	* setting up the panel
	*/
	protected String err = null;

	public SelectPanel(ContextElement element, boolean optional, String taskId, int mapN, SystemManager sman, Context contextDef) {
		cel = element;
		this.optional = optional;
		this.taskId = taskId;
		sysman = sman;
		this.mapN = mapN;
		this.contextDef = contextDef;
		setup();
	}

	protected boolean sameVectors(Vector v1, Vector v2) {
		if (v1 == null || v2 == null)
			return false;
		if (v1.size() != v2.size())
			return false;
		for (int i = 0; i < v1.size(); i++)
			if (!v1.elementAt(i).equals(v2.elementAt(i)))
				return false;
		return true;
	}

	public void setup() {
		if (cel == null || contextDef == null)
			return;
		Vector previousOptions = options;
		options = contextDef.getOptions(cel, mapN);
		if (options == null || options.size() < 1) {
			err = contextDef.getErrorMessage();
			return;
		}
		//Possibly, the options are the same as previous time?
		if (sameVectors(options, previousOptions)) {
			System.out.println("The options did not change!");
			return;
		}
		String layerId = null;
		if (cel.type.equals("attributes")) {
			ContextItem cit = contextDef.getContextItem(cel.refersTo);
			if (cit != null && cit.getContent() != null && (cit.getContent() instanceof String)) {
				layerId = (String) cit.getContent();
			}
		}
		String optionNames[] = new String[options.size()];
		for (int i = 0; i < options.size(); i++) {
			if (cel.type.equals("attributes")) {
				optionNames[i] = sysman.getAttributeName((String) options.elementAt(i), mapN, layerId);
			} else if (cel.type.equals("layer")) {
				optionNames[i] = sysman.getLayerName(mapN, (String) options.elementAt(i));
			}
			if (optionNames[i] == null) {
				optionNames[i] = (String) options.elementAt(i);
			}
		}
		removeAll();
		CheckboxGroup cbg = null;
		if (cel.method.equals("select_one")) {
			cbg = new CheckboxGroup();
		}
		Panel p = this;
		if (optionNames.length > 2) {
			p = new Panel();
			ScrollPane sp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
			sp.add(p);
			setLayout(new BorderLayout());
			add(sp, "Center");
		}
		p.setLayout(new ColumnLayout());
		optCB = new Vector(optionNames.length, 5);
		for (String optionName : optionNames) {
			Checkbox cb = new Checkbox(optionName, false, cbg);
			optCB.addElement(cb);
			p.add(cb);
		}
		if (cbg == null && optionNames.length > 2) { //multiple options may be selected
			Panel pp = new Panel(new FlowLayout(FlowLayout.CENTER, 20, 5));
			Button b = new Button("Select all");
			b.setActionCommand("select_all");
			b.addActionListener(this);
			pp.add(b);
			b = new Button("Unselect all");
			b.setActionCommand("unselect_all");
			b.addActionListener(this);
			pp.add(b);
			add(pp, "South");
		}
		//somehow previous definitions should be reflected (if any)
		if (contextDef != null) {
			ContextItem cit = contextDef.getContextItem(cel.localId);
			if (cit != null) { //this element has been defined earlier
				Object content = cit.getContent();
				if (content != null)
					if (content instanceof Vector) {
						Vector v = (Vector) content;
						for (int i = 0; i < options.size(); i++)
							if (v.contains(options.elementAt(i))) {
								((Checkbox) optCB.elementAt(i)).setState(true);
							}
					} else {
						for (int i = 0; i < options.size(); i++)
							if (options.elementAt(i).equals(content)) {
								((Checkbox) optCB.elementAt(i)).setState(true);
							}
					}
			}
		}
	}

	@Override
	public boolean isValid() {
		return options != null && options.size() > 0;
	}

	@Override
	public String getErrorMessage() {
		return err;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof Button) {
			String cmd = e.getActionCommand();
			boolean state = false;
			if (cmd.equals("select_all")) {
				state = true;
			} else if (!cmd.equals("unselect_all"))
				return;
			for (int i = 0; i < optCB.size(); i++) {
				((Checkbox) optCB.elementAt(i)).setState(state);
			}
		}
	}

	public boolean isAnyOptionSelected() {
		if (optCB == null) {
			err = "No options to select from!";
			return false;
		}
		for (int i = 0; i < optCB.size(); i++) {
			Checkbox cb = (Checkbox) optCB.elementAt(i);
			if (cb.getState())
				return true;
		}
		err = "No option is selected!";
		return false;
	}

	public ContextItem getContextItemDefinition() {
		if (!isAnyOptionSelected())
			if (optional)
				return new ContextItem(cel, mapN, null);
			else
				return null;
		Object content = null;
		if (cel.method.equals("select_one")) {
			for (int i = 0; i < optCB.size() && content == null; i++) {
				Checkbox cb = (Checkbox) optCB.elementAt(i);
				if (cb.getState()) {
					content = options.elementAt(i);
				}
			}
		} else {
			Vector sel = new Vector(optCB.size(), 5);
			for (int i = 0; i < optCB.size(); i++) {
				Checkbox cb = (Checkbox) optCB.elementAt(i);
				if (cb.getState()) {
					sel.addElement(options.elementAt(i));
				}
			}
			sel.trimToSize();
			content = sel;
		}
		if (content == null && !optional)
			return null;
		return new ContextItem(cel, mapN, content);
	}

	/**
	* A method from the DialogContent interface. Checks whether the user
	* has selected some option(s). If yes, forms the context element,
	* adds it to the current context and returns true
	*/
	@Override
	public boolean canClose() {
		if (!isAnyOptionSelected() && !optional)
			return false;
		//contextDef.addContextItem(getContextItemDefinition());
		return true;
	}
}