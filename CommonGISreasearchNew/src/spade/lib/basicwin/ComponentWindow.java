package spade.lib.basicwin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

import spade.lib.lang.Language;

/**
* Special bounding container for any component, primarily graphical analysis displays.
* It is panel with header containing name of the component and optional buttons
* "Expand" and "Close". If user presses these buttons action events are sending
* to parent component.
*/

public class ComponentWindow extends Panel {
	protected Component comp = null;
	protected String compId = null;
	protected String compName = null;
	Header h = null;

	protected boolean canExpand = true;

	public ComponentWindow(Component c, String id, ActionListener cParent) {
		comp = c;
		compId = id;
		compName = comp.getName();
		setName("Component window: " + compName);
		setLayout(new BorderLayout());
		h = new Header(compName, id, cParent);
		add(h, "North");
		add(comp, "Center");
	}

	public Component getContent() {
		return comp;
	}

	public Component getHeader() {
		return h;
	}

	public String getComponentID() {
		return compId;
	}

	public String getComponentName() {
		return compName;
	}

	public void setContent(Component content) {
		if (content == null) {
			this.remove(comp);
		}
		comp = content;
		if (comp != null) {
			add(comp, "Center");
		}
	}

	public void setCanExpand(boolean flag) {
		canExpand = flag;
		h.getExpandButton().setEnabled(flag);
	}

	public void setCaptionForeground(Color fgColor) {
		if (h != null) {
			h.fgColor = fgColor;
		}
	}

	public void setCaptionBackground(Color bgColor) {
		if (h != null) {
			h.bgColor = bgColor;
		}
	}

	public void setCaptionVisible(boolean flag) {
		h.setVisible(flag);
	}

	public void setHasExpandButton(boolean flag) {
		h.setHasExpandButton(flag);
	}

	public void setHasCloseButton(boolean flag) {
		h.setHasCloseButton(flag);
	}

	public void validateComponentWindow() {
		invalidate();
		CManager.validateAll(h.getCloseButton());
		//System.out.println("ComponentWindow validated");
	}
}

/**
*  Internal class Header is used for "window caption" emulation.
*  Possible customisations: presence or absence of control buttons,
*  background and foreground colors.
*/

class Header extends Panel {
	//static ResourceBundle res = ResourceBundle.getBundle("spade.lib.basicwin.Res", Language.lang);
	static ResourceBundle res = Language.getTextResource("spade.lib.basicwin.Res");
	private TImgButton bClose = null, bExpand = null;
	private String cName = "";
	private String cID = "";
	public Color bgColor = Color.darkGray.brighter();
	public Color fgColor = Color.white;
	boolean hasExpandButton = true;
	boolean hasCloseButton = true;

	public static final String cmdExpand = "expand", cmdClose = "close";

	public Header(String cName, String cID, ActionListener base) {
		if (cName != null && cName.length() > 0) {
			this.cName = cName;
		}
		this.cID = cID;
		setBackground(bgColor);
		setLayout(new BorderLayout());
		Label t = new Label(this.cName);
		t.setBackground(bgColor);
		t.setForeground(fgColor);
		add(t, "Center");
		if (hasExpandButton || hasCloseButton) {
			Panel bPanel = new Panel(new GridLayout(1, 2));
			bExpand = new TImgButton("/icons/expand.gif");
			bClose = new TImgButton("/icons/close.gif");
			// following text: "Expand window"
			new PopupManager(bExpand, res.getString("Expand_window"), true);
			// following text: "Close window"
			new PopupManager(bClose, res.getString("Close_window"), true);
			if (hasExpandButton) {
				bPanel.add(bExpand);
			}
			if (hasCloseButton) {
				bPanel.add(bClose);
			}
			bExpand.addActionListener(base);
			bClose.addActionListener(base);
			bExpand.setActionCommand(cmdExpand + "-" + cID);
			bClose.setActionCommand(cmdClose + "-" + cID);
			add(bPanel, "East");
		}
		//System.out.println("Header-"+cID+" is o.k.");
	}

	public TImgButton getExpandButton() {
		return bExpand;
	}

	public TImgButton getCloseButton() {
		return bClose;
	}

	public void setHasExpandButton(boolean flag) {
		if (hasExpandButton == flag)
			return;
		hasExpandButton = !hasExpandButton;
		bExpand.setVisible(hasExpandButton);
	}

	public void setHasCloseButton(boolean flag) {
		if (hasCloseButton == flag)
			return;
		hasCloseButton = !hasCloseButton;
		bClose.setVisible(hasCloseButton);
	}
}
