package ui;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.DisplayProducer;
import spade.analysis.system.Supervisor;
import spade.analysis.system.WindowManager;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.lib.ui.ListOrderer;
import spade.vis.database.Attribute;
import spade.vis.map.MapViewer;
import spade.vis.mapvis.DataMapper;
import spade.vis.mapvis.SimpleDataMapper;

/**
* A panel for the selection of cartographic and non-cartographic visualisation
* methods for some previously chosen attributes. Allows the user to reorder
* the attributes.
*/

public class ChooseVisMethodPanel extends Panel implements ActionListener {
	static ResourceBundle res = Language.getTextResource("ui.Res");

	protected TableData td = null;
	protected DataMapper dataMapper = null;
	protected DisplayProducer displayProducer = null;
	protected AttributeChooser attrChooser = null;
	protected Vector selColumns = null;
	protected Checkbox mapVisCB[] = null;
	protected Checkbox visCB[] = null;
	protected Vector visMethodIds = null;
	protected Vector mapVisMethods = null; //identifiers of map visualisation methods
	protected Panel attrNamePanel = null;
	protected Button reorderBut = null;
	protected Choice mapLocationChoice = null;
	protected Vector mapViews = null;

	public ChooseVisMethodPanel(TableData td, Object attrSelector, DisplayProducer displayProducer, DataMapper dataMapper, Supervisor supervisor) {
		if (td == null || attrSelector == null || displayProducer == null)
			return;
		if (attrSelector instanceof AttributeChooser) {
			attrChooser = (AttributeChooser) attrSelector;
		}
		if (attrChooser == null)
			return;
		this.td = td;
		this.displayProducer = displayProducer;
		this.dataMapper = dataMapper;
		selColumns = (Vector) attrChooser.getSelectedColumns().clone();
		Vector colIds = attrChooser.getSelectedColumnIds();
		int nparts = 1;
		int nMapVisMethods = 0;
		char objType = 0;
		if (td.themLayer != null) {
			objType = td.themLayer.getType();
			if (dataMapper != null) {
				mapVisMethods = dataMapper.getAvailableMethodList(objType);
				if (mapVisMethods != null) {
					nMapVisMethods = mapVisMethods.size();
				}
			}
		}
		visMethodIds = new Vector(displayProducer.getDisplayMethodCount(), 1);
		for (int i = 0; i < displayProducer.getDisplayMethodCount(); i++) {
			String id = displayProducer.getDisplayMethodId(i);
			if (!displayProducer.isDisplayMethodAttributeFree(id)) {
				visMethodIds.addElement(id);
			}
		}
		if (nMapVisMethods > 0) {
			++nparts;
		}
		if (visMethodIds.size() > 0) {
			++nparts;
		}
		setLayout(new GridLayout(1, nparts, 2, 0));
		setBackground(Color.darkGray);
		Panel pp = new Panel(new BorderLayout());
		pp.setBackground(Color.lightGray);
		Panel p1 = new Panel(new ColumnLayout());
		p1.add(new Label(res.getString("Selected_attributes_")));
		p1.add(new Line(false));
		pp.add(p1, BorderLayout.NORTH);
		p1 = new Panel(new ColumnLayout());
		for (int i = 0; i < selColumns.size(); i++) {
			Attribute attr = (Attribute) selColumns.elementAt(i);
			p1.add(new Label(attr.getName()));
		}
		attrNamePanel = p1;
		int maxLines = Math.max(nMapVisMethods + 2, visMethodIds.size());
		if (maxLines < 10) {
			maxLines = 10;
		}
		if (selColumns.size() > maxLines) {
			ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
			scp.add(p1);
			pp.add(scp, BorderLayout.CENTER);
		} else {
			pp.add(p1, BorderLayout.CENTER);
		}
		if (selColumns.size() > 1) {
			reorderBut = new Button(res.getString("reorder"));
			reorderBut.setActionCommand("reorder");
			reorderBut.addActionListener(this);
			p1 = new Panel(new ColumnLayout());
			p1.add(new Line(false));
			p1.add(reorderBut);
			pp.add(p1, BorderLayout.SOUTH);
		}
		add(pp);
		if (td.themLayer != null && nMapVisMethods > 0) {
			pp = new Panel(new ColumnLayout());
			pp.setBackground(Color.lightGray);
			add(pp);
			pp.add(new Label(res.getString("Maps")));
			pp.add(new Line(false));
			CheckboxGroup cbg = new CheckboxGroup();
			mapVisCB = new Checkbox[1 + nMapVisMethods];
			mapVisCB[0] = new Checkbox(res.getString("Do_not_build_a_map"), true, cbg);
			pp.add(mapVisCB[0]);
			pp.add(new Line(false));
			for (int i = 0; i < nMapVisMethods; i++) {
				String methodId = (String) mapVisMethods.elementAt(i);
				mapVisCB[1 + i] = new Checkbox(SimpleDataMapper.getMethodName(methodId, objType), false, cbg);
				mapVisCB[1 + i].setEnabled(dataMapper.isMethodApplicable(methodId, td.table, colIds, objType));
				pp.add(mapVisCB[1 + i]);
			}
			pp.add(new Label(res.getString("Map_location") + ":", Label.CENTER));
			mapLocationChoice = new Choice();
			mapLocationChoice.add(res.getString("Automatic"));
			mapLocationChoice.add(res.getString("Main_window"));
			mapLocationChoice.add(res.getString("New_window"));
			pp.add(mapLocationChoice);
			WindowManager wm = null;
			if (supervisor != null) {
				wm = supervisor.getWindowManager();
			}
			if (wm != null && wm.getWindowCount() > 0) {
				for (int i = 0; i < wm.getWindowCount(); i++)
					if (wm.getWindow(i) instanceof Frame) {
						Frame w = (Frame) wm.getWindow(i);
						MapViewer mv = findMapView(w);
						if (mv != null) {
							if (mapViews == null) {
								mapViews = new Vector(20, 10);
							}
							mapViews.addElement(mv);
							mapLocationChoice.add(w.getTitle());
						}
					}
			}
		}
		if (visMethodIds.size() > 0) {
			pp = new Panel(new ColumnLayout());
			pp.setBackground(Color.lightGray);
			add(pp);
			pp.add(new Label(res.getString("Charts")));
			pp.add(new Line(false));
			CheckboxGroup cbg = new CheckboxGroup();
			visCB = new Checkbox[1 + visMethodIds.size()];
			visCB[0] = new Checkbox(res.getString("Do_not_build_a_chart"), true, cbg);
			pp.add(visCB[0]);
			pp.add(new Line(false));
			int k = 0;
			for (int i = 0; i < displayProducer.getDisplayMethodCount(); i++) {
				String id = displayProducer.getDisplayMethodId(i);
				if (!visMethodIds.contains(id)) {
					continue;
				}
				++k;
				visCB[k] = new Checkbox(displayProducer.getDisplayMethodName(i), false, cbg);
				visCB[k].setEnabled(displayProducer.isDisplayMethodApplicable(i, td.table, colIds));
				pp.add(visCB[k]);
			}
		}
	}

	protected MapViewer findMapView(Container c) {
		if (c == null)
			return null;
		if (c instanceof MapViewer)
			return (MapViewer) c;
		for (int i = 0; i < c.getComponentCount(); i++) {
			Component comp = c.getComponent(i);
			if (comp instanceof MapViewer)
				return (MapViewer) comp;
		}
		MapViewer mv = null;
		for (int i = 0; i < c.getComponentCount() && mv == null; i++) {
			Component comp = c.getComponent(i);
			if (comp instanceof Container) {
				mv = findMapView((Container) comp);
			}
		}
		return mv;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() == null)
			return;
		if (e.getActionCommand().equals("reorder")) {
			Panel p = new Panel(new ColumnLayout());
			p.add(new Label(res.getString("Selected_attributes_")));
			ListOrderer lord = new ListOrderer(selColumns, true);
			p.add(lord);
			OKDialog okd = new OKDialog(CManager.getAnyFrame(), res.getString("reorder_attrs"), true, false);
			okd.addContent(p);
			okd.show();
			if (okd.wasCancelled())
				return;
			int nPrev = selColumns.size();
			selColumns = lord.getOrderedItems();
			int nCurr = selColumns.size();
			attrNamePanel.setVisible(false);
			attrNamePanel.removeAll();
			Vector colIds = new Vector(selColumns.size());
			for (int i = 0; i < selColumns.size(); i++) {
				Attribute at = (Attribute) selColumns.elementAt(i);
				attrNamePanel.add(new Label(at.getName()));
				colIds.addElement(at.getIdentifier());
			}
			attrNamePanel.setVisible(true);
			CManager.validateAll(attrNamePanel);
			if (selColumns.size() < 2) {
				reorderBut.setEnabled(false);
			}
			if (nCurr < nPrev) {
				if (mapVisCB != null) {
					for (int i = 1; i < mapVisCB.length; i++) {
						String methodId = (String) mapVisMethods.elementAt(i - 1);
						mapVisCB[i].setEnabled(dataMapper.isMethodApplicable(methodId, td.table, colIds, td.themLayer.getType()));
						if (!mapVisCB[i].isEnabled() && mapVisCB[i].getState()) {
							mapVisCB[0].setState(true);
							mapVisCB[i].setState(false);
						}
					}
				}
				if (visCB != null) {
					for (int i = 1; i < visCB.length; i++) {
						visCB[i].setEnabled(displayProducer.isDisplayMethodApplicable(i - 1, td.table, colIds));
						if (!visCB[i].isEnabled() && visCB[i].getState()) {
							visCB[0].setState(true);
							visCB[i].setState(false);
						}
					}
				}
			}
		}
	}

	public String getMapVisMethodId() {
		if (mapVisCB == null)
			return null;
		if (mapVisCB[0].getState())
			return null;
		int visN = -1;
		for (int i = 1; i < mapVisCB.length && visN < 0; i++)
			if (mapVisCB[i].getState()) {
				visN = i - 1;
			}
		if (visN < 0)
			return null;
		return (String) mapVisMethods.elementAt(visN);
	}

	public String getChartVisMethodId() {
		if (visCB == null)
			return null;
		if (visCB[0].getState())
			return null;
		int visN = -1;
		for (int i = 1; i < visCB.length && visN < 0; i++)
			if (visCB[i].getState()) {
				visN = i - 1;
			}
		if (visN < 0)
			return null;
		return (String) visMethodIds.elementAt(visN);
	}

	public Vector getReorderedAttributes() {
		return selColumns;
	}

	public Vector getReorderedColumnIds() {
		Vector colIds = new Vector(selColumns.size());
		for (int i = 0; i < selColumns.size(); i++) {
			Attribute at = (Attribute) selColumns.elementAt(i);
			colIds.addElement(at.getIdentifier());
		}
		return colIds;
	}

	/**
	* Returns the string "auto" if the automatic mode is selected, "main" if
	* the main window is selected, "new" if a new window must be created,
	* or a reference to a MapViewer if a particular map window is selected.
	*/
	public Object getMapViewSelection() {
		if (mapLocationChoice == null)
			return null;
		int idx = mapLocationChoice.getSelectedIndex();
		if (idx < 0)
			return null;
		if (idx == 0)
			return "auto";
		if (idx == 1)
			return "main";
		if (idx == 2)
			return "new";
		if (mapViews == null)
			return null;
		idx -= 3;
		if (idx >= mapViews.size())
			return null;
		return mapViews.elementAt(idx);
	}
}
