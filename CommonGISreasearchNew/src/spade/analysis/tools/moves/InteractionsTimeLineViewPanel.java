package spade.analysis.tools.moves;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Label;
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
import java.util.Vector;

import spade.analysis.aggregates.AggregateContainer;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.MultiSelector;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.RowLayout;
import spade.lib.util.StringUtil;
import spade.time.FocusInterval;
import spade.time.TimeMoment;
import spade.time.ui.TimeSlider;
import spade.time.ui.TimeSliderPanel;
import spade.time.vis.TimeLineLabelsCanvas;
import spade.vis.database.DataItem;
import spade.vis.database.ObjectContainer;
import spade.vis.database.ThematicDataItem;
import spade.vis.database.ThematicDataOwner;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 21, 2008
 * Time: 11:24:25 AM
 * A panel including a time line (Gantt chart) display of trajectories and interactions between them
 */
public class InteractionsTimeLineViewPanel extends Panel implements ActionListener, ItemListener, ComponentListener, PropertyChangeListener {
	/**
	 * The container with the members of the interactions
	 */
	protected ObjectContainer memberContainer = null;
	/**
	 * The time slider
	 */
	protected TimeSlider timeSlider = null;
	/**
	 * The canvas in which the visualisation is done
	 */
	protected InteractionsTimeLineViewCanvas evCanvas = null;
	/**
	 * The canvas in which labels are drawn (to remain fixed independently of
	 * the scroller position)
	 */
	protected TimeLineLabelsCanvas tLabCanvas = null;
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
	 * Shows the names of the attributes used for grouping
	 */
	protected TextField attrTF = null;
	/**
	 * Ascending order?
	 */
	protected Checkbox cbOrderAsc = null;
	/**
	* Used for changing the temporal extent of the visualization
	*/
	protected FocusInterval focusInterval = null;
	/**
	 * Used for extending the focuser to the whole time extent
	 */
	protected Button bStartEnd = null;

	protected Panel controlP = null;

	/**
	 * Constructs the visualisation display.
	 */
	public InteractionsTimeLineViewPanel(AggregateContainer aggLayer, ObjectContainer memberContainer) {
		setLayout(new BorderLayout());
		evCanvas = new InteractionsTimeLineViewCanvas();
		this.memberContainer = memberContainer;
		evCanvas.setData(aggLayer, memberContainer);
		scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
		scp.add(evCanvas);
		add(scp, BorderLayout.CENTER);
		scp.addComponentListener(this);
		tLabCanvas = new TimeLineLabelsCanvas();
		add(tLabCanvas, BorderLayout.NORTH);
		evCanvas.setTimeLineLabelsCanvas(tLabCanvas);
		boolean hasAttributes = false;
		if (memberContainer != null && memberContainer.getObjectCount() > 0) {
			ThematicDataItem td = null;
			for (int i = 0; i < memberContainer.getObjectCount() && td == null; i++) {
				DataItem data = memberContainer.getObjectData(i);
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
		if (hasAttributes) {
			Panel p = new Panel(new BorderLayout());
			p.add(new Label("Sort/group by:"), BorderLayout.NORTH);
			attrTF = new TextField("<no attributes selected>");
			attrTF.setEditable(false);
			p.add(attrTF, BorderLayout.CENTER);
			Button b = new Button("Change");
			b.setActionCommand("change");
			b.addActionListener(this);
			p.add(b, BorderLayout.EAST);
			p.add(cbOrderAsc = new Checkbox("Ascending order", true), BorderLayout.SOUTH);
			cbOrderAsc.addItemListener(this);
			Panel pp = new Panel(new ColumnLayout());
			pp.add(p);
			Panel ppp = new Panel(new BorderLayout());
			ppp.add(pp, BorderLayout.CENTER);
			ppp.add(new Line(true), BorderLayout.EAST);
			controlP.add(ppp, BorderLayout.WEST);
		}
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
			Panel pp = new Panel(new ColumnLayout());
			pp.add(bStartEnd);
			controlP.add(pp, BorderLayout.EAST);
		}
	}

	public void addUIElement(Component uiElement) {
		if (uiElement == null)
			return;
		if (controlP == null) {
			controlP = new Panel(new RowLayout());
			add(controlP, BorderLayout.SOUTH);
		}
		controlP.add(uiElement);
	}

	public void setSupervisor(Supervisor supervisor) {
		this.supervisor = supervisor;
		evCanvas.setSupervisor(supervisor);
		if (timeSlider != null) {
			timeSlider.setSupervisor(supervisor);
		}
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
		if (memberContainer == null)
			return null;
		for (int i = 0; i < memberContainer.getObjectCount(); i++) {
			DataItem data = memberContainer.getObjectData(i);
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
	public void itemStateChanged(ItemEvent ie) {
		if (ie.getSource().equals(cbOrderAsc)) {
			evCanvas.setSortType(cbOrderAsc.getState());
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd == null)
			return;
		if (cmd.equals("full_extent")) {
			focusInterval.showWholeInterval();
		} else if (cmd.equalsIgnoreCase("change")) {
			Vector attrIds = new Vector(50, 50), attrNames = new Vector(50, 50);
			ThematicDataItem td = getThematicDataItem();
			if (td != null) {
				for (int i = 0; i < td.getAttrCount(); i++) {
					attrIds.addElement(td.getAttributeId(i));
					attrNames.addElement(td.getAttributeName(i));
				}
			}
			if (attrIds.size() < 1) {
				remove(attrTF.getParent());
				attrTF = null;
				return;
			}
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
				evCanvas.setSortAttrIds(null);
				attrTF.setText("<no attributes selected>");
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
				evCanvas.setSortAttrIds(sortAttrIds);
				attrTF.setText(str);
			}
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
			attrTF.setText("<no attributes selected>");
		} else {
			evCanvas.setSortAttrIds(sortAttrIds);
			attrTF.setText(str);
		}
	}

	/**
	 * Listens to changes of the time focuser
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {
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
}
