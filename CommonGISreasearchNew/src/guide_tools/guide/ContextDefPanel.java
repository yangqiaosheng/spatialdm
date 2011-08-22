package guide_tools.guide;

import java.awt.Container;
import java.awt.Panel;

import spade.lib.basicwin.DialogContent;

/**
* A ContextDefPanel is a panel used for definition of a context element.
* In most cases it contains a component that implements the DialogContent
* interface, for example, a SelectPanel. In such cases the ContextDefPanel
* "acts on behalf" of this component in a dialog providing access to
* the methods canClose() and getErrorMessage() of this component.
*/

public class ContextDefPanel extends Panel implements DialogContent {
	/**
	* Checks if the container passed is an instance of SelectPanel.
	* If not, checks its components.
	*/
	protected static SelectPanel getSelectPanel(Container cont) {
		if (cont == null)
			return null;
		if (cont instanceof SelectPanel)
			return (SelectPanel) cont;
		for (int i = 0; i < cont.getComponentCount(); i++)
			if (cont.getComponent(i) instanceof Container) {
				SelectPanel sp = getSelectPanel((Container) cont.getComponent(i));
				if (sp != null)
					return sp;
			}
		return null;
	}

	/**
	* Does not check if the container passed is an instance of DialogContent,
	* checks only its components.
	*/
	protected static DialogContent getDialogContent(Container cont) {
		if (cont == null)
			return null;
		for (int i = 0; i < cont.getComponentCount(); i++)
			if (cont.getComponent(i) instanceof DialogContent)
				return (DialogContent) cont.getComponent(i);
			else if (cont.getComponent(i) instanceof Container) {
				DialogContent dc = getDialogContent((Container) cont.getComponent(i));
				if (dc != null)
					return dc;
			}
		return null;
	}

	protected SelectPanel getSelectPanel() {
		return getSelectPanel(this);
	}

	public ContextItem getContextItemDefinition() {
		SelectPanel sp = getSelectPanel();
		if (sp != null)
			return sp.getContextItemDefinition();
		return null;
	}

	@Override
	public boolean canClose() {
		DialogContent dc = getDialogContent(this);
		if (dc == null)
			return true;
		return dc.canClose();
	}

	@Override
	public String getErrorMessage() {
		DialogContent dc = getDialogContent(this);
		if (dc == null)
			return null;
		return dc.getErrorMessage();
	}
}