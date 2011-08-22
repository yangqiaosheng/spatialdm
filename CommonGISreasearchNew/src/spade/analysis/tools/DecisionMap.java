package spade.analysis.tools;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.StringReader;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.lib.basicwin.Arrow;
import spade.lib.basicwin.DialogContent;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.CombinedFilter;
import spade.vis.database.DataTable;
import spade.vis.database.SemanticsManager;
import spade.vis.database.TableFilter;
import spade.vis.mapvis.UtilitySignDrawer;
import spade.vis.space.GeoLayer;
import ui.TableManager;
import fgapplet.server.VMT;

class DecisionMapInput extends Panel implements ActionListener, ItemListener, DialogContent {
	static ResourceBundle res = Language.getTextResource("spade.analysis.tools.Res");
	protected SemanticsManager sm = null;
	protected AttributeDataPortion dtab = null;
	protected Arrow ar[] = null;
	protected Choice ch[] = null;
	protected Checkbox cbUseFilter = null;
	// following string: "Select attribute for X", "Select attribute for Y", "Select attribute for Z (Color)", "(optional) Select 1st attribute for scrollbar", "(optional) Select 2nd attribute for scrollbar"
	protected String cap[] = { res.getString("Select_attribute_for"), res.getString("Select_attribute_for1"), res.getString("Select_attribute_for2"), res.getString("_optional_Select_1st"), res.getString("_optional_Select_2nd") };

	public DecisionMapInput(AttributeDataPortion dtab, boolean objectsAreFiltered) {
		super();
		this.dtab = dtab;
		if (dtab instanceof DataTable) {
			sm = ((DataTable) dtab).getSemanticsManager();
		}
		ar = new Arrow[5];
		ch = new Choice[5];
		setLayout(new BorderLayout());
		Panel p = new Panel();
		p.setLayout(new GridLayout(6, 1));
		add(p, "North");
		for (int i = 0; i < 5; i++) {
			Panel pp = new Panel();
			pp.setLayout(new BorderLayout());
			p.add(pp);
			ar[i] = new Arrow(this, true, i);
			pp.add(ar[i], "West");
			ar[i].setEnabled(false);
			ch[i] = new Choice();
			pp.add(ch[i], "Center");
			ch[i].addItemListener(this);
			ch[i].addItem(cap[i]);
			for (int j = 0; j < dtab.getAttrCount(); j++)
				if (dtab.isAttributeNumeric(j)) {
					ch[i].addItem(dtab.getAttributeName(j));
				}
		}
		if (objectsAreFiltered) {
			// following string: "Filter objects by query"
			cbUseFilter = new Checkbox(res.getString("Filter_objects_by"), false);
			p.add(cbUseFilter);
		}
	}

	public int getAttrNum(int n) {
		int k = 0;
		for (int i = 0; i < dtab.getAttrCount(); i++)
			if (dtab.isAttributeNumeric(i)) {
				k++;
				if (k == n)
					return i;
			}
		return -1;
	}

	public boolean getUseFilter() {
		return cbUseFilter != null && cbUseFilter.getState();
	}

	public int getSelectedIndex(int i) {
		return ch[i].getSelectedIndex();
	}

	public boolean isMax(int i) {
		return ar[i].isMax();
	}

	@Override
	public boolean canClose() {
		return ch[0].getSelectedIndex() > 0 && ch[1].getSelectedIndex() > 0 && ch[2].getSelectedIndex() > 0 && ch[0].getSelectedIndex() != ch[1].getSelectedIndex() && ch[0].getSelectedIndex() != ch[2].getSelectedIndex()
				&& ch[1].getSelectedIndex() != ch[2].getSelectedIndex();
	}

	@Override
	public String getErrorMessage() {
		if (ch[0].getSelectedIndex() <= 0 || ch[1].getSelectedIndex() <= 0 || ch[2].getSelectedIndex() <= 0)
			// following string: "At least 3 criteria should be selected"
			return res.getString("At_least_3_criteria");
		if (ch[0].getSelectedIndex() == ch[1].getSelectedIndex() || ch[0].getSelectedIndex() == ch[2].getSelectedIndex() || ch[1].getSelectedIndex() == ch[2].getSelectedIndex())
			// following string:"First 3 criteria should be different"
			return res.getString("First_3_criteria");
		return "";
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		int n = -1;
		for (int i = 0; i < ch.length; i++)
			if (ch[i].equals(ie.getSource())) {
				n = i;
				break;
			}
		if (n < 0)
			return;
		if (ch[n].getSelectedIndex() == 0) {
			ar[n].setEnabled(false);
			return;
		}
		ar[n].setEnabled(true);
		int attrN = getAttrNum(ch[n].getSelectedIndex());
		String ID = dtab.getAttributeId(attrN);
		if (sm.isAttributeBenefitCriterion(ID)) {
			ar[n].setIsMax(true);
		}
		if (sm.isAttributeCostCriterion(ID)) {
			ar[n].setIsMax(false);
		}
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
	}
}

public class DecisionMap implements DataAnalyser, WindowListener, fgapplet.EPHDataSource {
	static ResourceBundle res = ResourceBundle.getBundle("spade.analysis.tools.Res");
	protected String dir = null;
	protected AttributeDataPortion dtab = null;
	protected TableFilter tFilter = null;
	protected DecisionMapInput dmi = null;
	protected Vector attrs = null; // IDs of used attributes
	protected GeoLayer gl = null;
	protected UtilitySignDrawer usd = null;

	/* should analyse:
	  1) presence of RGDB library
	  2) this is not the Internet version
	*/
	@Override
	public boolean isValid(ESDACore core) {
		if (!core.getSystemSettings().checkParameterValue("isLocalSystem", "true"))
			return false;
		if (null == core.getSystemSettings().getParameterAsString("PATH_TO_RGDB"))
			return false;
		try {
			Class.forName("fgapplet.server.Calculator");
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	protected Frame frRGDB = null;
	protected ESDACore core = null;

	@Override
	public void run(ESDACore core) {
		this.core = core;
		if (core.getDataKeeper().getTableCount() < 1)
			return;
		TableManager tman = new TableManager();
		tman.setDataKeeper(core.getDataKeeper());
		tman.setUI(core.getUI());
		// following string:"Select the table for analysis"
		int tn = tman.selectTableNumber(res.getString("Select_the_table_for"));
		if (tn < 0)
			return;
		dtab = core.getDataKeeper().getTable(tn);
		if (dtab.getObjectFilter() instanceof TableFilter) {
			tFilter = (TableFilter) dtab.getObjectFilter();
		} else if (dtab.getObjectFilter() instanceof CombinedFilter) {
			CombinedFilter cf = (CombinedFilter) dtab.getObjectFilter();
			for (int i = 0; i < cf.getFilterCount(); i++)
				if (cf.getFilter(i) instanceof TableFilter) {
					tFilter = (TableFilter) cf.getFilter(i);
				}
		}
		dmi = new DecisionMapInput(dtab, tFilter.areObjectsFiltered());
		// following string:"RGDB shell"
		OKDialog dlg = new OKDialog(core.getUI().getMainFrame(), res.getString("RGDB_shell"), true);
		dlg.addContent(dmi);
		dlg.show();
		if (dlg.wasCancelled())
			return;
		for (int i = 0; i < 5; i++) {
			int n = dmi.getSelectedIndex(i);
			if (n == 0) {
				continue;
			}
			int attrN = dmi.getAttrNum(n);
			String ID = dtab.getAttributeId(attrN);
			if (dmi.isMax(i)) {
				dmi.sm.setAttributeIsBenefitCriterion(ID);
			} else {
				dmi.sm.setAttributeIsCostCriterion(ID);
			}
		}
		if (!dmi.getUseFilter()) {
			tFilter = null;
		}
		// check for number of data items
		int Ndi = (tFilter == null) ? dtab.getDataItemCount() : tFilter.getNSatisfying();
		if (Ndi > RGDBmax) {
			System.err.println("In this version number of data items is restricted by " + RGDBmax);
			return;
		}
		//----------- start of RGDB launch code -----------

		int N[] = new int[5];
		for (int i = 0; i < 5; i++)
			if (dmi.getSelectedIndex(i) > 0) {
				N[i] = dmi.getAttrNum(dmi.getSelectedIndex(i));
			} else {
				N[i] = -1;
			}

		int ncols = 1;
		for (int i = 0; i < 5; i++)
			if (N[i] >= 0) {
				ncols++;
			}

		attrs = new Vector(5, 5);

		VMT vmt = new VMT();
		vmt.setNCols(ncols);

		int dirs[] = new int[ncols];
		dirs[0] = 0;
		for (int i = 0, i1 = 1; i < 5; i++)
			if (N[i] >= 0) {
				dirs[i1++] = dmi.isMax(i) ? 1 : -1;
			}
		vmt.setDirs(dirs);

		String names[] = new String[ncols];
		names[0] = "Name";
		for (int i = 0, i1 = 1; i < 5; i++)
			if (N[i] >= 0) {
				names[i1++] = dtab.getAttributeName(N[i]).replace(' ', '_');
				attrs.addElement(dtab.getAttributeId(N[i]));
			}
		vmt.setNames(names);

		int k = -1;
		Vector hasEmptyValNames = null, hasEmptyValIDs = null;
		for (int j = 0; j < dtab.getDataItemCount(); j++)
			if (tFilter == null || tFilter.isActive(j)) {
				String ID = dtab.getDataItemId(j).replace(' ', '_');
				k++;
				vmt.setElem(k, 0, ID);
				for (int i = 0, i1 = 1; i < 5; i++)
					if (N[i] >= 0) {
						double val = dtab.getNumericAttrValue(N[i], j);
						if (Double.isNaN(val)) {
							String name = dtab.getDataItemName(i);
							if (hasEmptyValNames == null) {
								hasEmptyValNames = new Vector(10, 10);
							}
							if (hasEmptyValIDs == null) {
								hasEmptyValIDs = new Vector(10, 10);
							}
							if (!StringUtil.isStringInVectorIgnoreCase(name, hasEmptyValNames)) {
								hasEmptyValNames.addElement(new String(name));
							}
							if (!StringUtil.isStringInVectorIgnoreCase(ID, hasEmptyValIDs)) {
								hasEmptyValIDs.addElement(new String(ID));
							}
						} else {
							vmt.setElem(k, /*i1++*/i + 1, val);
						}
					}
			}
		if (hasEmptyValNames != null) {
			Panel p = new Panel();
			p.setLayout(new GridLayout(4 + 3 + hasEmptyValNames.size(), 1));
			// following string:"Following objects have undefined"
			p.add(new Label(res.getString("Following_objects")));
			// following string:"values of some criteria:"
			p.add(new Label(res.getString("values_of_some")));
			for (int i = 0; i < hasEmptyValNames.size(); i++) {
				p.add(new Label("" + (1 + i) + ". " + (String) hasEmptyValNames.elementAt(i)));
			}
			p.add(new Label(""));
			// following string:"Possible actions:"
			p.add(new Label(res.getString("Possible_actions_")));
			CheckboxGroup cbg = new CheckboxGroup();
			Checkbox cb[] = new Checkbox[3];
			// following string:"replace missing values by worst"
			cb[0] = new Checkbox(res.getString("replace_missing"), true, cbg);
			// following string:"remove objects with missing values"
			cb[1] = new Checkbox(res.getString("remove_objects_with"), false, cbg);
			// following string:"do not build decision map
			cb[2] = new Checkbox(res.getString("do_not_build_decision"), false, cbg);
			for (Checkbox element : cb) {
				p.add(element);
			}
			// following string:"RGDB shell"
			dlg = new OKDialog(core.getUI().getMainFrame(), res.getString("RGDB_shell"), true);
			dlg.addContent(p);
			dlg.show();
			if (dlg.wasCancelled() || cb[2].getState())
				return;
			k = -1;
			if (cb[0].getState()) {
				float worstVals[] = new float[5];
				for (int i = 0; i < worstVals.length; i++) {
					worstVals[i] = Float.NaN;
				}
				for (int j = 0; j < dtab.getDataItemCount(); j++)
					if (tFilter == null || tFilter.isActive(j)) {
						for (int i = 0; i < worstVals.length; i++) {
							double val = dtab.getNumericAttrValue(N[i], j);
							if (Double.isNaN(val)) {
								continue;
							}
							if (Float.isNaN(worstVals[i])) {
								worstVals[i] = (float) val;
								continue;
							}
							if (dirs[i + 1] == 1 && worstVals[i] > val) {
								worstVals[i] = (float) val;
								continue;
							}
							if (dirs[i + 1] == -1 && worstVals[i] < val) {
								worstVals[i] = (float) val;
								continue;
							}
						}
					}
				for (int j = 0; j < dtab.getDataItemCount(); j++)
					if (tFilter == null || tFilter.isActive(j)) {
						String ID = dtab.getDataItemId(j).replace(' ', '_');
						k++;
						for (int i = 0; i < 5; i++)
							if (N[i] >= 0) {
								double val = dtab.getNumericAttrValue(N[i], j);
								if (Double.isNaN(val)) {
									vmt.setElem(k, i + 1, worstVals[i]);
								}
							}
					}
			} else {
				vmt = new VMT();
				vmt.setNCols(ncols);
				vmt.setDirs(dirs);
				vmt.setNames(names);
				for (int j = 0; j < dtab.getDataItemCount(); j++)
					if (tFilter == null || tFilter.isActive(j)) {
						String ID = dtab.getDataItemId(j).replace(' ', '_');
						if (StringUtil.isStringInVectorIgnoreCase(ID, hasEmptyValIDs)) {
							continue;
						}
						k++;
						vmt.setElem(k, 0, ID);
						for (int i = 0; i < 5; i++)
							if (N[i] >= 0) {
								double val = dtab.getNumericAttrValue(N[i], j);
								if (Double.isNaN(val)) {
									// error - should not be
								} else {
									vmt.setElem(k, i + 1, val);
								}
							}
					}
			}
		}

		//fgapplet.server.Calculator.setCalcBaseDir("d:\\projects\\vmjava\\fgapplet\\inst\\calc");
		String fname = core.getSystemSettings().getParameterAsString("PATH_TO_RGDB");
		String PathToRGDB = (new File(fname)).getAbsolutePath();
		fgapplet.server.Calculator.setCalcBaseDir(PathToRGDB);
		maxRGDBid++;
		RGDBid = maxRGDBid;
		RGDBset = fgapplet.server.Calculator.calculate(vmt.toString(), RGDBid);

		frRGDB = new Frame("RGDB");
		frRGDB.setSize(760, 500);
		frRGDB.setLayout(new BorderLayout());
		fgapplet.AWTDesignApplet fgapp = new fgapplet.AWTDesignApplet(this);
		frRGDB.add(fgapp, BorderLayout.CENTER);
		fgapp.init();
		frRGDB.show();
		frRGDB.addWindowListener(this);
		//--------- end of RGDB launch code
		//The window must be properly registered in order to be closed in a case
		//when the aplication is closed or changed.
		core.getWindowManager().registerWindow(frRGDB);
	}

	String RGDBset; // used to store RGDB set build by external calculation module
	static int maxRGDBid = 0; // session id number. If you suppose to use calculation module
	// concurently, you have to handle different ids.
	int RGDBid = 0;
	int RGDBmax = 1000; // max number or rows

	// ----- RGDB interface methods --------------

	@Override
	public java.io.Reader getData() {
		return new StringReader(RGDBset);
	}

	@Override
	public void setPoint(double[] point) {
		VMT result = fgapplet.server.Calculator.findPareto(RGDBid, point);
		Vector objIds = new Vector(result.getNRows(), 10);
		for (int i = 0; i < result.getNRows(); i++) {
			objIds.addElement(new String(result.getElemString(i, 0)));
		}
		core.selectObjects(this, dtab.getContainerIdentifier(), objIds);

		System.err.print("* Selection:");
		for (int i = 0; i < result.getNRows(); i++) {
			System.err.print(" " + result.getElemString(i, 0));
		}
		System.err.print("--- cmpVals");
		for (double element : point) {
			System.err.print(" " + StringUtil.floatToStr((float) element));
		}
		System.err.println("  ---");

		if (gl == null) {
			gl = core.getDataKeeper().getTableLayer(dtab);
		}
		if (usd == null || gl.getVisualizer() != usd) {
			core.getDisplayProducer().displayOnMap("utility_bars", dtab, attrs, // attributes
					gl, // map layer
					core.getUI().getMapViewer(core.getUI().getCurrentMapN())); // map viewer
			usd = (UtilitySignDrawer) gl.getVisualizer();
		}
		usd.getSelectiveDrawingController().setDrawSelectedOnly(true);
		float cmpVals[] = new float[point.length];
		for (int i = 0; i < point.length; i++) {
			cmpVals[i] = (float) point[i];
		}
		usd.setCmpModeOn(-1, "specified values", cmpVals);
	}

	// ------------------------------------------------

	@Override
	public void windowClosing(WindowEvent e) {
		if (e.getSource() == frRGDB) {
			frRGDB.dispose();
			return;
		}
	}

	@Override
	public void windowOpened(WindowEvent e) {
	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}
}
