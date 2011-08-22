package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;

import spade.analysis.system.MapToolbar;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.PopupManager;
import spade.lib.basicwin.TImgButton;
import spade.lib.basicwin.TImgButtonGroup;
import spade.lib.lang.Language;
import spade.vis.event.DMouseEvent;
import spade.vis.event.EventMeaningManager;
import spade.vis.map.Zoomable;

/**
* Simple map tool bar does not allow map manipulation by click
*/
class SimpleMapToolBar extends Panel implements ActionListener, MapToolbar, PropertyChangeListener, Destroyable {
	static ResourceBundle res = Language.getTextResource("ui.Res");

	protected Zoomable map = null;
	protected EventMeaningManager evtMeanMan = null;
	protected TImgButton bMapPan = null, bZoomIn = null, bZoomOut = null, bZoomUndo = null, bDragZoomIn = null, bDragShift = null, bDragSelect = null, bSaveToFile = null, bPrint = null, bDeselect = null;
	public static String cmdMapPan = "MapPan", cmdZoomIn = "ZoomIn", cmdZoomOut = "ZoomOut", cmdZoomUndo = "ZoomUndo", cmdDragZoomIn = "DragZoomIn", cmdDragShift = "DragShift", cmdDragSelect = "DragSelect", cmdSaveToFile = "SaveToFile",
			cmdPrint = "Print", cmdDeselect = "Deselect";
	protected TImgButtonGroup dragBGr = null;
	protected Panel buttonPanel = null, buttonPanelParent = null, buttonPanelRight = null;
	protected ActionListener owner = null;
	protected boolean printAllowed = false, saveAllowed = false;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;

	public SimpleMapToolBar(Zoomable map, EventMeaningManager evtMeanMan, boolean allowSave, boolean allowPrint, ActionListener owner) {
		this.owner = owner;
		printAllowed = allowPrint && owner != null;
		saveAllowed = allowSave;
		this.evtMeanMan = evtMeanMan;
		this.map = map;
		this.map.addPropertyChangeListener(this);
		evtMeanMan.addPropertyChangeListener(this);
		setLayout(new ColumnLayout());
		buttonPanel = new Panel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 1));
		buttonPanelParent = new Panel(new BorderLayout());
		buttonPanelParent.setBackground(Color.lightGray);
		buttonPanelParent.add(buttonPanel, BorderLayout.WEST);
		add(buttonPanelParent);
		add(new Line(false));
		addZoomAndDragButtons(buttonPanel);
		setButtonStatus();
	}

	@Override
	public void addToolbarElement(Component toolbarElement) {
		if (buttonPanel != null && toolbarElement != null) {
			buttonPanel.add(toolbarElement);
		}
	}

	public void addToolbarElementRight(Component toolbarElement) {
		if (toolbarElement != null && buttonPanelParent != null) {
			if (buttonPanelRight == null) {
				buttonPanelRight = new Panel(new FlowLayout(FlowLayout.LEFT, 2, 1));
				buttonPanelParent.add(buttonPanelRight, BorderLayout.EAST);
			}
			buttonPanelRight.add(toolbarElement);
			CManager.validateAll(toolbarElement);
		}
	}

	public void removeToolbarElementRight(Component toolbarElement) {
		if (toolbarElement != null && buttonPanelRight != null) {
			buttonPanelRight.remove(toolbarElement);
			CManager.validateAll(buttonPanelRight);
		}
	}

	protected void addZoomAndDragButtons(Panel p) {
		p.add(bMapPan = new TImgButton("/icons/AllMap.gif"));
		new PopupManager(bMapPan, res.getString("Pan_map_to_the_window"), true);
		bMapPan.setActionCommand(cmdMapPan);
		bMapPan.addActionListener(this);

		p.add(bZoomIn = new TImgButton("/icons/ZoomIn.gif"));
		new PopupManager(bZoomIn, res.getString("Zoom_In"), true);
		bZoomIn.setActionCommand(cmdZoomIn);
		bZoomIn.addActionListener(this);

		p.add(bZoomOut = new TImgButton("/icons/ZoomOut.gif"));
		new PopupManager(bZoomOut, res.getString("Zoom_Out"), true);
		bZoomOut.setActionCommand(cmdZoomOut);
		bZoomOut.addActionListener(this);

		p.add(bZoomUndo = new TImgButton("/icons/ZoomUndo.gif"));
		new PopupManager(bZoomUndo, res.getString("Zoom_Undo"), true);
		bZoomUndo.setActionCommand(cmdZoomUndo);
		bZoomUndo.addActionListener(this);

		if (printAllowed) {
			p.add(new Label(""));
			p.add(bPrint = new TImgButton("/icons/Print.gif"));
			new PopupManager(bPrint, res.getString("Print"), true);
			bPrint.setActionCommand(cmdPrint);
			bPrint.addActionListener(owner);
			// saving map to file
		}
		if (saveAllowed) {
			p.add(new Label(""));
			if (printAllowed) {
				p.add(bSaveToFile = new TImgButton("/icons/Save.gif"));
				new PopupManager(bSaveToFile, res.getString("Save_map_to_file"), true);
			} else {
				p.add(bSaveToFile = new TImgButton("/icons/Print.gif"));
				new PopupManager(bSaveToFile, res.getString("Save_or_print_map"), true);
			}
			bSaveToFile.setActionCommand(cmdSaveToFile);
			//bSaveToFile.addActionListener(this);
			bSaveToFile.addActionListener(owner);
		}

		p.add(new Label(" "));
		/*
		ActionCanvas ac=new ActionCanvas("/icons/MouseDrag.gif");
		new PopupManager(ac,"Interpretations of mouse drag:\n - Zoom in;\n"+
		                    " - Shift viewport;\n - Select object(s)",true);
		p.add(ac);
		*/
		dragBGr = new TImgButtonGroup();
		dragBGr.addActionListener(this);
		p.add(dragBGr);

		dragBGr.addButton(bDragZoomIn = new TImgButton("/icons/ZoomSel.gif"));
		new PopupManager(bDragZoomIn, res.getString("Drag_Zoom_In"), true);
		bDragZoomIn.setActionCommand(cmdDragZoomIn);

		dragBGr.addButton(bDragShift = new TImgButton("/icons/Hand.gif"));
		new PopupManager(bDragShift, res.getString("Drag_Move_map"), true);
		bDragShift.setActionCommand(cmdDragShift);

		dragBGr.addButton(bDragSelect = new TImgButton("/icons/Check.gif"));
		new PopupManager(bDragSelect, res.getString("Drag_Select_objects"), true);
		bDragSelect.setActionCommand(cmdDragSelect);

		p.add(new Label(" "));

		p.add(bDeselect = new TImgButton("/icons/deselect.gif"));
		new PopupManager(bDeselect, res.getString("deselect"), true);
		bDeselect.setActionCommand(cmdDeselect);
		bDeselect.addActionListener(owner);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (map == null)
			return;
		if (e.getActionCommand().equals(cmdMapPan)) {
			map.pan();
			return;
		}
		if (e.getActionCommand().equals(cmdZoomIn)) {
			map.zoomIn();
			return;
		}
		if (e.getActionCommand().equals(cmdZoomOut)) {
			map.zoomOut();
			return;
		}
		if (e.getActionCommand().equals(cmdZoomUndo)) {
			map.undo();
			return;
		}
		if (e.getActionCommand().equals(cmdDragZoomIn)) {
			evtMeanMan.setCurrentEventMeaning(DMouseEvent.mDrag, "zoom");
			return;
		}
		if (e.getActionCommand().equals(cmdDragShift)) {
			evtMeanMan.setCurrentEventMeaning(DMouseEvent.mDrag, "shift");
			return;
		}
		if (e.getActionCommand().equals(cmdDragSelect)) {
			evtMeanMan.setCurrentEventMeaning(DMouseEvent.mDrag, "select");
			return;
		}
	}

	/**
	* When map scale changes, state of the buttons may also change
	*/
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getSource() == map && (evt.getPropertyName().equals("MapScale") || evt.getPropertyName().equals("MapContent"))) {
			setButtonStatus();
		} else if (evt.getSource() == evtMeanMan && evt.getPropertyName().endsWith("_current_meaning")) {
			if (evt.getNewValue().equals("zoom")) {
				dragBGr.setSelect(0);
			} else if (evt.getNewValue().equals("shift")) {
				dragBGr.setSelect(1);
			} else if (evt.getNewValue().equals("select")) {
				dragBGr.setSelect(2);
				//System.out.println("* "+evt);
				//System.out.println("* "+evt.getPropertyName()+", new val="+evt.getNewValue());
			}
		}
	}

	public void setButtonStatus() {
		if (map == null)
			return;
		bMapPan.setEnabled(map.canPan());
		bZoomIn.setEnabled(map.canZoomIn());
		bZoomOut.setEnabled(map.canZoomOut());
		bZoomUndo.setEnabled(map.canUndo());
		if (bPrint != null) {
			bPrint.setEnabled(true);
		}
		if (bSaveToFile != null) {
			bSaveToFile.setEnabled(true);
		}
	}

	/**
	* Removes itself from listeners of the event meaning manager
	*/
	@Override
	public void destroy() {
		evtMeanMan.removePropertyChangeListener(this);
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
