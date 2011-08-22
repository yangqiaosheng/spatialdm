package guide_tools.guide;

import java.awt.Button;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Vector;

import spade.kbase.scenarios.ContextElement;
import spade.kbase.scenarios.TreeNode;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.TextCanvas;

/**
* The ContextView shows currently defined context, reflects all its changes
* (listens to property changes of the context) and allows to redefine some
* of the context elements.
* The ContextView itself may have listeners of context changes.
*/
public class ContextView extends Panel implements PropertyChangeListener, ActionListener, Destroyable {
	public static Color bkgContextColor = new Color(210, 255, 255);
	protected GuideCore core = null;
	protected Context context = null;
	protected boolean contextEditing = false;
	protected PropertyChangeSupport pcSupport = null;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;

	public ContextView(GuideCore core) {
		super();
		this.core = core;
		context = core.context;
		setLayout(new ColumnLayout());
		setBackground(bkgContextColor);
		if (context != null) {
			context.addPropertyChangeListener(this);
		}
		showContext();
	}

	/**
	* Constructs the interface elements representing the context
	*/
	protected void showContext() {
		removeAll();
		if (context != null && context.getItemCount() > 0) {
			Label l = new Label("Context:", Label.CENTER);
			add(l);
			for (int i = 0; i < context.getItemCount(); i++) {
				ContextItem citem = context.getContextItem(i);
				if (!citem.isActive()) {
					continue;
				}
				Object content = citem.getContent();
				if (content instanceof Boolean) {
					continue;
				}
				TextCanvas ctc = new TextCanvas();
				ContextElement cel = citem.getTemplate();
				ctc.addTextLine(context.fillNameSlots(cel.getName()) + ":");
				if (content == null) {
					ctc.addTextLine(" -- undefined");
				} else if (content instanceof Vector) {
					Vector v = (Vector) content;
					for (int j = 0; j < v.size(); j++) {
						ctc.addTextLine(" -- " + context.getTextForItemValue(citem, v.elementAt(j)));
					}
				} else {
					ctc.addTextLine(" -- " + context.getTextForItemValue(citem, content));
				}
				add(ctc);
				Panel p = new Panel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
				Button b = new Button("change");
				b.setActionCommand("change_" + cel.localId);
				b.addActionListener(this);
				p.add(b);
				add(p);
			}
		}
		if (isShowing()) {
			CManager.validateAll(this);
		}
	}

	/**
	* Reacts to changes of the current context
	*/
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (contextEditing)
			return;
		if (evt.getPropertyName().equals("context")) {
			showContext();
		}
	}

	/**
	* Reacts to pressing of "change" buttons
	*/
	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd == null)
			return;
		if (cmd.startsWith("change")) {
			int idx = cmd.indexOf('_');
			if (idx < 0)
				return;
			String id = cmd.substring(idx + 1);
			ContextItem old = context.getContextItem(id);
			if (old == null || old.getTaskId() == null)
				return;
			TreeNode task = core.getTask(old.getTaskId());
			if (task == null)
				return;
			Vector tasks = new Vector(5, 5);
			tasks.addElement(task);
			Vector dependent = core.context.getDependentItems(id, null);
			if (dependent != null) {
				for (int i = 0; i < dependent.size(); i++) {
					ContextItem item = (ContextItem) dependent.elementAt(i);
					TreeNode t = core.getTask(item.getTaskId());
					if (t != null && !tasks.contains(t)) {
						tasks.addElement(t);
					}
				}
			}
			contextEditing = true;
			boolean changed = false;
			ContextDefWizard cdw = new ContextDefWizard();
			ContextItem cit = cdw.defineContextElement(core, old.getTemplate(), task);
			if (cit != null && !cit.sameContent(old.getContent())) {
				changed = true;
				context.addContextItem(cit);
				setEnabled(false);
				cdw.specifyContext(this, core, tasks, true);
			} else {
				finishContextEditing(false);
			}
		} else if ((e.getSource() instanceof ContextDefWizard) && cmd.equals("finished")) {
			setEnabled(true);
			finishContextEditing(true);
		}
	}

	protected void finishContextEditing(boolean changed) {
		contextEditing = false;
		showContext();
		if (changed) {
			notifyPropertyChange("context", null, null);
		}
	}

	public boolean isContextBeingEdited() {
		return contextEditing;
	}

	/**
	* Removes itself from listeners of property changes of the context
	*/
	@Override
	public void destroy() {
		if (context != null) {
			context.removePropertyChangeListener(this);
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

	@Override
	public void addPropertyChangeListener(PropertyChangeListener pcl) {
		if (pcl == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(pcl);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener pcl) {
		if (pcl == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(pcl);
	}

	public void notifyPropertyChange(String name, Object oldValue, Object newValue) {
		if (pcSupport != null) {
			pcSupport.firePropertyChange(name, oldValue, newValue);
		}
	}
}