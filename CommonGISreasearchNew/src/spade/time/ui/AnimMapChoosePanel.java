package spade.time.ui;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.analysis.system.WindowManager;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.DialogContent;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.lib.ui.ListOrderer;
import spade.time.vis.VisAttrDescriptor;
import spade.vis.database.AttrDescriptor;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.Parameter;
import spade.vis.map.MapViewer;
import spade.vis.mapvis.DataMapper;
import spade.vis.mapvis.SimpleDataMapper;
import spade.vis.space.GeoLayer;

/**
* A panel for the selection of a cartographic visualisation method to be used on
* an animated map. The method is chosen for some previously chosen attributes.
* Allows the user to reorder the attributes and to select "lagged" time moments
* (e.g. t+1, t+2, ..., where t is the current time moment in the animation).
*/

public class AnimMapChoosePanel extends Panel implements ActionListener, DialogContent {
	static ResourceBundle res = Language.getTextResource("spade.time.ui.Res");

	protected AttributeDataPortion table = null;
	protected GeoLayer layer = null;
	protected Parameter par = null;
	protected DataMapper dataMapper = null;
	protected Vector mapVisMethods = null; //identifiers of map visualisation methods
	/**
	* Consists of instances of spade.vis.database.AttrDescriptor
	*/
	protected Vector attrDescr = null;
	/**
	* Consists of instances of spade.time.vis.VisAttrDescriptor
	*/
	protected Vector visAttrDescr = null;
	protected Checkbox mapVisCB[] = null;
	protected Panel attrNamePanel = null, buttonPanel = null;
	protected Button reorderBut = null, advancedBut = null;
	protected String errMsg = null;
	protected Choice mapLocationChoice = null;
	protected Vector mapViews = null;

	public AnimMapChoosePanel(AttributeDataPortion table, GeoLayer themLayer, Parameter temporalParam, Vector attrDescr, DataMapper dataMapper, Supervisor supervisor) {
		if (table == null || themLayer == null || attrDescr == null || attrDescr.size() < 1 || dataMapper == null)
			return;
		if (dataMapper.getAvailableMethodCount() < 1)
			return;
		this.table = table;
		layer = themLayer;
		this.dataMapper = dataMapper;
		par = temporalParam;

		this.attrDescr = (Vector) attrDescr.clone();
		visAttrDescr = new Vector(attrDescr.size() * 2, 10);
		for (int i = 0; i < attrDescr.size(); i++) {
			AttrDescriptor ad = (AttrDescriptor) attrDescr.elementAt(i);
			VisAttrDescriptor vd = new VisAttrDescriptor();
			vd.attr = ad.attr;
			vd.attrId = ad.attr.getIdentifier();
			if (ad.children != null && ad.children.size() > 0) {
				vd.parent = ad.attr;
				vd.attr = (Attribute) ad.children.elementAt(0);
				vd.attrId = vd.attr.getIdentifier();
			}
			vd.isTimeDependent = false;
			int nFixedPar = 0;
			if (ad.parVals != null) {
				nFixedPar = ad.parVals.length;
				for (int j = 0; j < ad.parVals.length && !vd.isTimeDependent; j++)
					if (ad.parVals[j].elementAt(0).equals(par.getName())) {
						vd.isTimeDependent = ad.parVals[j].size() > 2;
						break;
					}
				if (vd.isTimeDependent) {
					--nFixedPar;
				}
			}
			if (nFixedPar > 0) {
				vd.fixedParams = new Vector(nFixedPar, 1);
				vd.fixedParamVals = new Vector(nFixedPar, 1);
				for (Vector parVal : ad.parVals)
					if (parVal.size() == 2) {
						String name = (String) parVal.elementAt(0);
						Object value = parVal.elementAt(1);
						vd.fixedParams.addElement(name);
						vd.fixedParamVals.addElement(value);
					}
			}
			visAttrDescr.addElement(vd);
		}

		int nMapVisMethods = 0;
		char objType = 0;
		if (themLayer != null) {
			objType = themLayer.getType();
			if (dataMapper != null) {
				mapVisMethods = dataMapper.getAvailableMethodList(objType);
				if (mapVisMethods != null) {
					nMapVisMethods = mapVisMethods.size();
				}
			}
		}
		setLayout(new GridLayout(1, 2, 2, 2));
		setBackground(Color.darkGray);
		Panel pp = new Panel(new BorderLayout());
		pp.setBackground(Color.lightGray);
		Panel p1 = new Panel(new ColumnLayout());
		p1.add(new Label(res.getString((visAttrDescr.size() > 1) ? "selected_attrs" : "selected_attr") + ":", Label.CENTER));
		p1.add(new Line(false));
		pp.add(p1, BorderLayout.NORTH);
		attrNamePanel = new Panel(new ColumnLayout());
		for (int i = 0; i < visAttrDescr.size(); i++) {
			VisAttrDescriptor ad = (VisAttrDescriptor) visAttrDescr.elementAt(i);
			attrNamePanel.add(new Label(ad.getName()));
		}
		int maxLines = nMapVisMethods;
		if (maxLines < 10) {
			maxLines = 10;
		}
		if (visAttrDescr.size() > maxLines) {
			ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
			scp.add(attrNamePanel);
			pp.add(scp, BorderLayout.CENTER);
		} else {
			pp.add(attrNamePanel, BorderLayout.CENTER);
		}
		p1 = new Panel(new ColumnLayout());
		p1.add(new Line(false));
		buttonPanel = new Panel(new FlowLayout(FlowLayout.CENTER, 30, 2));
		reorderBut = new Button(res.getString("reorder"));
		reorderBut.setActionCommand("reorder");
		reorderBut.addActionListener(this);
		if (visAttrDescr.size() > 1) {
			buttonPanel.add(reorderBut);
		}
		advancedBut = new Button(res.getString("Advanced"));
		advancedBut.setActionCommand("advanced");
		advancedBut.addActionListener(this);
		buttonPanel.add(advancedBut);
		p1.add(buttonPanel);
		pp.add(p1, BorderLayout.SOUTH);
		add(pp);
		mapVisCB = new Checkbox[nMapVisMethods];
		CheckboxGroup cbg = new CheckboxGroup();
		Vector selAttrIds = getAttrIds();
		for (int i = 0; i < nMapVisMethods; i++) {
			String methodId = (String) mapVisMethods.elementAt(i);
			mapVisCB[i] = new Checkbox(SimpleDataMapper.getMethodName(methodId, objType), cbg, false);
			if (!dataMapper.isMethodApplicable(methodId, table, selAttrIds, layer.getType())) {
				mapVisCB[i].setEnabled(false);
			}
		}
		pp = new Panel(new ColumnLayout());
		pp.setBackground(Color.lightGray);
		pp.add(new Label(res.getString("sel_cart_vis_method") + ":"));
		pp.add(new Line(false));
		for (Checkbox element : mapVisCB) {
			pp.add(element);
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
		add(pp);
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

	protected void checkApplicability() {
		Vector selAttrIds = getAttrIds();
		for (int i = 0; i < mapVisCB.length; i++) {
			String methodId = (String) mapVisMethods.elementAt(i);
			mapVisCB[i].setEnabled(dataMapper.isMethodApplicable(methodId, table, selAttrIds, layer.getType()));
			if (!mapVisCB[i].isEnabled() && mapVisCB[i].getState()) {
				mapVisCB[i].setState(false);
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() == null)
			return;
		if (e.getActionCommand().equals("reorder")) {
			Panel p = new Panel(new ColumnLayout());
			p.add(new Label(res.getString("selected_attrs")));
			ListOrderer lord = new ListOrderer(visAttrDescr, true);
			p.add(lord);
			OKDialog okd = new OKDialog(CManager.getAnyFrame(), res.getString("reorder_attrs"), true, false);
			okd.addContent(p);
			okd.show();
			if (okd.wasCancelled())
				return;
			int nPrev = visAttrDescr.size();
			visAttrDescr = lord.getOrderedItems();
			int nCurr = visAttrDescr.size();
			attrNamePanel.setVisible(false);
			attrNamePanel.removeAll();
			for (int i = 0; i < visAttrDescr.size(); i++) {
				VisAttrDescriptor ad = (VisAttrDescriptor) visAttrDescr.elementAt(i);
				attrNamePanel.add(new Label(ad.getName()));
			}
			attrNamePanel.setVisible(true);
			CManager.validateAll(attrNamePanel);
			if (visAttrDescr.size() < 2) {
				reorderBut.setEnabled(false);
			}
			if (nCurr < nPrev) {
				checkApplicability();
				//check if at least one time-dependent attribute left
				boolean timeDep = false;
				for (int i = 0; i < visAttrDescr.size() && !timeDep; i++) {
					VisAttrDescriptor vad = (VisAttrDescriptor) visAttrDescr.elementAt(i);
					timeDep = vad.parent != null && vad.parent.dependsOnParameter(par);
				}
				if (!timeDep) {
					advancedBut.setEnabled(false);
				}
				if (attrDescr.size() > 1) {
					//remove from attrDescr those descriptors which do not correspond to
					//any of the remaining attributes
					for (int i = attrDescr.size() - 1; i >= 0; i--) {
						AttrDescriptor ad = (AttrDescriptor) attrDescr.elementAt(i);
						boolean found = false;
						for (int j = 0; j < visAttrDescr.size() && !found; j++) {
							VisAttrDescriptor vad = (VisAttrDescriptor) visAttrDescr.elementAt(j);
							found = (vad.attr != null && vad.attr.equals(ad.attr)) || (vad.parent != null && vad.parent.equals(ad.attr));
							//check values of the fixed parameters, if any
							if (ad.parVals != null) {
								for (int k = 0; k < ad.parVals.length && found; k++)
									if (ad.parVals[k].size() == 2) {
										String name = (String) ad.parVals[k].elementAt(0);
										if (!name.equals(par.getName()))
											if (vad.fixedParams == null) {
												found = false;
											} else {
												int pn = -1;
												for (int n = 0; n < vad.fixedParams.size() && pn < 0; n++)
													if (name.equals(vad.fixedParams.elementAt(n))) {
														pn = n;
													}
												if (pn < 0) {
													found = false;
												} else {
													found = ad.parVals[k].elementAt(1).equals(vad.fixedParamVals.elementAt(pn));
												}
											}
									}
							}
						}
						if (!found) {
							attrDescr.removeElementAt(i);
						}
					}
				}
			}
		} else if (e.getActionCommand().equals("advanced")) {
			boolean timeDep = false;
			for (int i = 0; i < visAttrDescr.size() && !timeDep; i++) {
				VisAttrDescriptor vad = (VisAttrDescriptor) visAttrDescr.elementAt(i);
				timeDep = vad.parent != null && vad.parent.dependsOnParameter(par);
			}
			if (!timeDep) { //no time-dependent attributes
				advancedBut.setEnabled(false);
				return;
			}
			TimeParamValSelectPanel tPan = new TimeParamValSelectPanel(attrDescr, par, visAttrDescr);
			OKDialog okd = new OKDialog(CManager.getAnyFrame(), res.getString("Select_val_temp_par"), true, false);
			okd.addContent(tPan);
			okd.show();
			if (okd.wasCancelled())
				return;
			int nPrev = visAttrDescr.size();
			visAttrDescr = tPan.getAttrDescriptors();
			int nCurr = visAttrDescr.size();
			attrNamePanel.setVisible(false);
			attrNamePanel.removeAll();
			for (int i = 0; i < visAttrDescr.size(); i++) {
				VisAttrDescriptor ad = (VisAttrDescriptor) visAttrDescr.elementAt(i);
				attrNamePanel.add(new Label(ad.getName()));
			}
			attrNamePanel.setVisible(true);
			CManager.validateAll(attrNamePanel);
			if (!buttonPanel.isAncestorOf(reorderBut) && visAttrDescr.size() > 1) {
				buttonPanel.setVisible(false);
				buttonPanel.removeAll();
				buttonPanel.add(reorderBut);
				buttonPanel.add(advancedBut);
				buttonPanel.setVisible(true);
				buttonPanel.invalidate();
				buttonPanel.validate();
			}
			reorderBut.setEnabled(visAttrDescr.size() > 1);
			checkApplicability();
		}
	}

	public String getMapVisMethodId() {
		if (mapVisCB == null) {
			errMsg = res.getString("no_appr_method");
			return null;
		}
		int visN = -1;
		for (int i = 0; i < mapVisCB.length && visN < 0; i++)
			if (mapVisCB[i].isEnabled() && mapVisCB[i].getState()) {
				visN = i;
			}
		if (visN < 0) {
			errMsg = res.getString("no_method_selected");
			return null;
		}
		return (String) mapVisMethods.elementAt(visN);
	}

	/**
	* Returns a vector of instances of spade.time.vis.VisAttrDescriptor
	*/
	public Vector geAttributeDescriptors() {
		return visAttrDescr;
	}

	public Vector getAttrIds() {
		Vector selAttrIds = new Vector(visAttrDescr.size(), 1);
		for (int i = 0; i < visAttrDescr.size(); i++) {
			VisAttrDescriptor d = (VisAttrDescriptor) visAttrDescr.elementAt(i);
			selAttrIds.addElement(d.attrId);
		}
		return selAttrIds;
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

	@Override
	public boolean canClose() {
		return getMapVisMethodId() != null;
	}

	@Override
	public String getErrorMessage() {
		return errMsg;
	}
}
