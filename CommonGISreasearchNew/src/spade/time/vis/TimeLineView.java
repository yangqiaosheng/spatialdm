package spade.time.vis;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.analysis.system.SystemUI;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.MultiSelector;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.RowLayout;
import spade.lib.lang.Language;
import spade.lib.util.ListSelector;
import spade.lib.util.StringUtil;
import spade.time.FocusInterval;
import spade.time.TimeMoment;
import spade.time.TimePositionNotifier;
import spade.time.ui.TimeSlider;
import spade.time.ui.TimeSliderPanel;
import spade.vis.database.DataItem;
import spade.vis.database.ObjectContainer;
import spade.vis.database.ThematicDataItem;
import spade.vis.database.ThematicDataOwner;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 01-Feb-2007
 * Time: 16:49:00
 * Includes an EventCanvas and interactive manipulation controls.
 */
public class TimeLineView extends Panel implements ActionListener, ItemListener, ComponentListener, PropertyChangeListener, Destroyable {
	static ResourceBundle res = Language.getTextResource("spade.time.vis.SchedulerTexts_time_vis");
	/**
	 * The container with time-referenced items (events), which are visualised
	 */
	protected ObjectContainer oCont = null;
	/**
	 * The time slider for focusing on time subranges
	 */
	protected TimeSlider timeSlider = null;
	/**
	 * The canvas in which the visualisation is done
	 */
	protected TimeLineCanvas evCanvas = null;
	/**
	 * The canvas in which labels are drawn (to remain fixed independently of
	 * the scroller position)
	 */
	protected TimeLineLabelsCanvas tLabCanvas = null;
	/**
	 * Used for showing information related to the mouse position
	 */
	protected Label infoLabel = null;
	/**
	 * The scroll pane containing the canvas
	 */
	protected ScrollPane scp = null;
	/**
	 * Supervisor provides access of a plot to the Highlighter (common for
	 * all data displays) and in this way links together all displays
	 */
	protected Supervisor supervisor = null;
	/**
	 * Shows the names of the attributes used for sorting
	 */
	protected TextField sortAttrTF = null;
	/**
	 * Shows the name of the attribute used for grouping of lines
	 */
	protected TextField groupAttrTF = null;
	/**
	 * Shows the name of the attribute used for aligning
	 */
	protected TextField alignAttrTF = null;
	/**
	 * Sets the vertical size (width) of an element representing an event
	 */
	protected TextField lineWidthTF = null;
	/**
	 * Allows to switch between showing all or only active events
	 */
	protected Checkbox onlyActiveCB = null;
	/**
	* Used for changing the temporal extent of the visualization
	*/
	protected FocusInterval focusInterval = null;
	/**
	 * Used for extending the focuser to the whole time extent
	 */
	protected Button bStartEnd = null;

	protected Panel controlP = null, // main control panel
			controlPextra = null; // placeholder for further UI elements
	/**
	 * Used to notify other components about time moments corresponding to mouse positions
	 */
	protected TimePositionNotifier tpn = null;

	/**
	 * Constructs the visualisation display.
	 * @param oCont - container with time-referenced items (events)
	 */
	public TimeLineView(ObjectContainer oCont) {
		this.oCont = oCont;
		setLayout(new BorderLayout());
		infoLabel = new Label("");
		evCanvas = new TimeLineCanvas();
		evCanvas.setObjectContainer(oCont);
		evCanvas.setInfoLabel(infoLabel);
		scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
		scp.add(evCanvas);
		add(scp, BorderLayout.CENTER);
		scp.addComponentListener(this);
		lineWidthTF = new TextField(String.valueOf(evCanvas.getUnitSize()), 2);
		lineWidthTF.addActionListener(this);
		Panel p = new Panel(new RowLayout());
		p.add(new Label("Element height:"));
		p.add(lineWidthTF);
		Panel pp = new Panel(new BorderLayout());
		pp.add(infoLabel, BorderLayout.CENTER);
		pp.add(p, BorderLayout.EAST);
		p = new Panel(new GridLayout(2, 1));
		p.add(pp);
		tLabCanvas = new TimeLineLabelsCanvas();
		p.add(tLabCanvas);
		add(p, BorderLayout.NORTH);
		evCanvas.setTimeLineLabelsCanvas(tLabCanvas);
		boolean hasAttributes = false;
		if (oCont != null && oCont.getObjectCount() > 0) {
			ThematicDataItem td = null;
			for (int i = 0; i < oCont.getObjectCount() && td == null; i++) {
				DataItem data = oCont.getObjectData(i);
				if (data == null) {
					continue;
				}
				if (data instanceof ThematicDataItem) {
					td = (ThematicDataItem) data;
				} else if (data instanceof ThematicDataOwner) {
					td = ((ThematicDataOwner) data).getThematicData();
				}
			}
			if (td != null) {
				hasAttributes = td.getAttrCount() > 0;
			}
		}
		controlP = new Panel(new BorderLayout());
		add(controlP, BorderLayout.SOUTH);
		p = new Panel(new ColumnLayout());
		if (hasAttributes) {
			pp = new Panel(new BorderLayout());
			pp.add(new Label("Sort by:"), BorderLayout.WEST);
			//p.add(new Label(res.getString("Sort_group_by")+":"),BorderLayout.NORTH);
			sortAttrTF = new TextField("<no attributes selected>");
			sortAttrTF.setEditable(false);
			pp.add(sortAttrTF, BorderLayout.CENTER);
			Button b = new Button(res.getString("Change"));
			b.setActionCommand("change_sort_attr");
			b.addActionListener(this);
			pp.add(b, BorderLayout.EAST);
			p.add(pp);
			pp = new Panel(new BorderLayout());
			pp.add(new Label("Group by:"), BorderLayout.WEST);
			groupAttrTF = new TextField("<no attributes selected>");
			groupAttrTF.setEditable(false);
			pp.add(groupAttrTF, BorderLayout.CENTER);
			b = new Button(res.getString("Change"));
			b.setActionCommand("change_group_attr");
			b.addActionListener(this);
			pp.add(b, BorderLayout.EAST);
			p.add(pp);
			pp = new Panel(new BorderLayout());
			pp.add(new Label("Align by:"), BorderLayout.WEST);
			alignAttrTF = new TextField("<no attributes selected>");
			alignAttrTF.setEditable(false);
			pp.add(alignAttrTF, BorderLayout.CENTER);
			b = new Button(res.getString("Change"));
			b.setActionCommand("change_align_attr");
			b.addActionListener(this);
			pp.add(b, BorderLayout.EAST);
			p.add(pp);
		}
		controlPextra = new Panel(new ColumnLayout());
		onlyActiveCB = new Checkbox(res.getString("hide_inactive_events"), false);
		onlyActiveCB.addItemListener(this);
		controlPextra.add(onlyActiveCB);
		controlPextra.add(new Line(false));
		//p.add(controlPextra,BorderLayout.SOUTH);
		p.add(controlPextra);
		pp = new Panel(new BorderLayout());
		pp.add(p, BorderLayout.CENTER);
		pp.add(new Line(true), BorderLayout.EAST);
		controlP.add(pp, BorderLayout.WEST);

		TimeMoment t0 = evCanvas.getDataStart(), t1 = evCanvas.getDataEnd();
		if (t0 == null || t1 == null) {
			evCanvas.setup();
			t0 = evCanvas.getDataStart();
			t1 = evCanvas.getDataEnd();
		}
		if (t0 != null && t1 != null) {
			focusInterval = new FocusInterval();
			focusInterval.setDataInterval(t0, t1);
			focusInterval.addPropertyChangeListener(this);
			timeSlider = new TimeSlider(focusInterval);
			TimeSliderPanel tspan = new TimeSliderPanel(timeSlider, this, true);
			bStartEnd = new Button("Full extent");
			bStartEnd.addActionListener(this);
			bStartEnd.setActionCommand("full_extent");
			bStartEnd.setEnabled(false);
			controlP.add(tspan, BorderLayout.CENTER);
			pp = new Panel(new ColumnLayout());
			pp.add(bStartEnd);
			controlP.add(pp, BorderLayout.EAST);
		}

		tpn = new TimePositionNotifier();
		evCanvas.setTimePositionNotifier(tpn);
		tLabCanvas.setTimePositionNotifier(tpn);
		tpn.addPropertyChangeListener(this);
	}

	/**
	 * used by scheduler for adding UI elements (category)
	 */
	public void addUIElement(Component uiElement) {
		if (uiElement == null)
			return;
		controlPextra.add(uiElement);
	}

	public void setSupervisor(Supervisor supervisor) {
		this.supervisor = supervisor;
		evCanvas.setSupervisor(supervisor);
		if (timeSlider != null) {
			timeSlider.setSupervisor(supervisor);
		}
		if (tpn != null) {
			tpn.setSupervisor(supervisor);
		}
	}

	/**
	 * A ListSelector specifies which objects should be drawn
	 */
	public void setListSelector(ListSelector listSelector) {
		evCanvas.setListSelector(listSelector);
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension d = evCanvas.getPreferredSize(), ss = Toolkit.getDefaultToolkit().getScreenSize();
		int w = Math.min(d.width, ss.width * 2 / 3), h = Math.min(d.height, ss.height * 2 / 3);
		w += scp.getVScrollbarWidth() + 10;
		h += scp.getHScrollbarHeight() + 10 + tLabCanvas.getPreferredSize().height;
		d = controlP.getPreferredSize();
		h += d.height;
		if (w < d.width) {
			w = d.width;
		}
		return new Dimension(w, h);
	}

	private ThematicDataItem getThematicDataItem() {
		if (oCont == null)
			return null;
		for (int i = 0; i < oCont.getObjectCount(); i++) {
			DataItem data = oCont.getObjectData(i);
			if (data == null) {
				continue;
			}
			if (data instanceof ThematicDataItem)
				return (ThematicDataItem) data;
			if (data instanceof ThematicDataOwner)
				return ((ThematicDataOwner) data).getThematicData();
		}
		return null;
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(onlyActiveCB)) {
			evCanvas.setShowOnlyActiveEvents(onlyActiveCB.getState());
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(lineWidthTF)) {
			int w = -1;
			try {
				w = Integer.parseInt(lineWidthTF.getText());
			} catch (Exception ex) {
			}
			if (w > 0) {
				evCanvas.setUnitSize(w);
			} else {
				lineWidthTF.setText(String.valueOf(evCanvas.getUnitSize()));
			}
			return;
		}
		String cmd = e.getActionCommand();
		if (cmd == null)
			return;
		if (cmd.equals("full_extent")) {
			focusInterval.showWholeInterval();
		} else if (cmd.equalsIgnoreCase("change_sort_attr")) {
			Vector attrIds = new Vector(50, 50), attrNames = new Vector(50, 50);
			ThematicDataItem td = getThematicDataItem();
			if (td != null) {
				for (int i = 0; i < td.getAttrCount(); i++) {
					attrIds.addElement(td.getAttributeId(i));
					attrNames.addElement(td.getAttributeName(i));
				}
			}
			if (attrIds.size() < 1)
				return;
			MultiSelector ms = new MultiSelector(attrNames, true);
			Vector sortAttrIds = evCanvas.getSortAttrIds();
			if (sortAttrIds != null && sortAttrIds.size() > 0) {
				int selIdxs[] = new int[sortAttrIds.size()];
				for (int i = 0; i < sortAttrIds.size(); i++) {
					selIdxs[i] = StringUtil.indexOfStringInVectorIgnoreCase((String) sortAttrIds.elementAt(i), attrIds);
				}
				ms.selectItems(selIdxs);
			}
			Panel p = new Panel(new BorderLayout());
			p.add(new Label("Select the attribute(s) to be used for " + "sorting or grouping:"), BorderLayout.NORTH);
			p.add(ms, BorderLayout.CENTER);
			OKDialog okd = new OKDialog(CManager.getAnyFrame(this), "Select attributes", true);
			okd.addContent(p);
			okd.show();
			if (okd.wasCancelled())
				return;
			int selIdxs[] = ms.getSelectedIndexes();
			if (selIdxs == null) {
				sortAttrTF.setText("<no attributes selected>");
				evCanvas.setSortAttrIds(null);
				evCanvas.groupAndSort();
			} else {
				String str = "";
				if (sortAttrIds != null) {
					sortAttrIds.removeAllElements();
				} else {
					sortAttrIds = new Vector(10, 10);
				}
				for (int i = 0; i < selIdxs.length; i++) {
					int idx = selIdxs[i];
					sortAttrIds.addElement(attrIds.elementAt(idx));
					if (i > 0) {
						str += "; ";
					}
					str += "\"" + (String) attrNames.elementAt(idx) + "\"";
				}
				sortAttrTF.setText(str);
				evCanvas.setSortAttrIds(sortAttrIds);
				evCanvas.groupAndSort();
			}
		} else if (cmd.equalsIgnoreCase("change_group_attr") || cmd.equalsIgnoreCase("change_align_attr")) {
			Vector attrIds = new Vector(50, 50), attrNames = new Vector(50, 50);
			ThematicDataItem td = getThematicDataItem();
			if (td != null) {
				for (int i = 0; i < td.getAttrCount(); i++) {
					attrIds.addElement(td.getAttributeId(i));
					attrNames.addElement(td.getAttributeName(i));
				}
			}
			if (attrIds.size() < 1)
				return;
			boolean toGroup = cmd.equalsIgnoreCase("change_group_attr");
			String attrId = (toGroup) ? evCanvas.getGroupAttrId() : evCanvas.getAlignAttrId();
			List list = new List(Math.min(10, attrNames.size()));
			for (int i = 0; i < attrNames.size(); i++) {
				list.add((String) attrNames.elementAt(i));
			}
			if (attrId != null) {
				int aIdx = StringUtil.indexOfStringInVectorIgnoreCase(attrId, attrIds);
				if (aIdx >= 0) {
					list.select(aIdx);
				}
			}
			Panel p = new Panel(new BorderLayout());
			p.add(new Label("Select the attribute to be used for " + ((toGroup) ? "grouping:" : "aligning")), BorderLayout.NORTH);
			p.add(list, BorderLayout.CENTER);
			OKDialog okd = new OKDialog(CManager.getAnyFrame(this), "Select attribute", true);
			okd.addContent(p);
			okd.show();
			int selIdx = (okd.wasCancelled()) ? -1 : list.getSelectedIndex();
			TextField tf = (toGroup) ? groupAttrTF : alignAttrTF;
			if (selIdx < 0) {
				if (toGroup) {
					evCanvas.setGroupAttrId(null);
				} else {
					evCanvas.setAlignAttrId(null);
				}
				tf.setText("<no attributes selected>");
			} else {
				tf.setText("\"" + list.getSelectedItem() + "\"");
				String aId = (String) attrIds.elementAt(selIdx);
				if (toGroup) {
					evCanvas.setGroupAttrId(aId);
/*
          if (aId.equals(evCanvas.getAlignAttrId())) {
            evCanvas.setAlignAttrId(null);
            alignAttrTF.setText("<no attributes selected>");
          }
*/
				} else {
					evCanvas.setAlignAttrId(aId);
/*
          if (aId.equals(evCanvas.getGroupAttrId())) {
            evCanvas.setGroupAttrId(null);
            groupAttrTF.setText("<no attributes selected>");
          }
*/
				}
			}
			evCanvas.groupAndSort();
		}
	}

	public void sortBy(Vector sortAttrIds) {
		String str = "";
		ThematicDataItem td = getThematicDataItem();
		if (td == null) {
			sortAttrIds = null;
		}
		if (sortAttrIds != null && sortAttrIds.size() > 0) {
			for (int i = sortAttrIds.size() - 1; i >= 0; i--) {
				int idx = td.getAttrIndex((String) sortAttrIds.elementAt(i));
				if (idx < 0) {
					sortAttrIds.removeElementAt(i);
				} else {
					str = (str.length() > 0) ? td.getAttributeName(idx) + "; " + str : td.getAttributeName(idx);
				}
			}
		}
		if (sortAttrIds == null || sortAttrIds.size() < 1) {
			evCanvas.setSortAttrIds(null);
			sortAttrTF.setText("<no attributes selected>");
		} else {
			evCanvas.setSortAttrIds(sortAttrIds);
			sortAttrTF.setText(str);
		}
	}

	/**
	 * Listens to changes of the time focuser
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getPropertyName().equals("current_moment") && (e.getNewValue() instanceof TimePositionNotifier)) {
			TimePositionNotifier tpn = (TimePositionNotifier) e.getNewValue();
			if (supervisor != null) {
				SystemUI ui = supervisor.getUI();
				if (ui != null)
					if (tpn.getMouseX() < 0 || tpn.lastId == null) {
						ui.erasePositionMarksOnMaps();
					} else {
						ui.markPositionOnMaps((float) tpn.lastMapX, (float) tpn.lastMapY);
					}
			}
			return;
		}
		if (e.getSource() instanceof FocusInterval) {
			if (e.getPropertyName().equalsIgnoreCase("current_interval")) {
				TimeMoment t1 = (TimeMoment) e.getOldValue(), t2 = (TimeMoment) e.getNewValue();
				if (t2.subtract(t1) < 1) {
					t1 = t1.getCopy();
					t2 = t1.getCopy();
					t2.add(1);
					focusInterval.setCurrInterval(t1, t2);
					return;
				}
				evCanvas.setFocusInterval(t1, t2);
				TimeMoment start = evCanvas.getDataStart(), end = evCanvas.getDataEnd();
				bStartEnd.setEnabled(start.compareTo(t1) < 0 || end.compareTo(t2) > 0);
			}
		}
	}

	/**
	 * Invoked when the component's size changes.
	 */
	@Override
	public void componentResized(ComponentEvent e) {
		Dimension sps = scp.getSize(), cs = evCanvas.getSize();
		int w = sps.width - scp.getVScrollbarWidth() - 8;
		evCanvas.setSize(w, cs.height);
		cs = tLabCanvas.getSize();
		tLabCanvas.setSize(w, cs.height);
		evCanvas.invalidate();
		tLabCanvas.invalidate();
		scp.invalidate();
		CManager.validateAll(this);
	}

	/**
	 * Invoked when the component's position changes.
	 */
	@Override
	public void componentMoved(ComponentEvent e) {
	}

	/**
	 * Invoked when the component has been made visible.
	 */
	@Override
	public void componentShown(ComponentEvent e) {
	}

	/**
	 * Invoked when the component has been made invisible.
	 */
	@Override
	public void componentHidden(ComponentEvent e) {
	}

	//-------------- Destroyable interface
	boolean isDestroyed = false;

	@Override
	public boolean isDestroyed() {
		return isDestroyed;
	}

	@Override
	public void destroy() {
		isDestroyed = true;
		if (tpn != null) {
			tpn.destroy();
		}
	}
}
