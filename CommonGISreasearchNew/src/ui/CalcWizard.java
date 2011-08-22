package ui;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.calc.CalcManager;
import spade.analysis.system.DataKeeper;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.PopupManager;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;
import spade.vis.database.DataTable;

public class CalcWizard {
	static ResourceBundle res = Language.getTextResource("ui.Res");
	protected Supervisor sup = null;
	protected CalcManager calcMan = null;
	protected Frame frame = null;

	public CalcWizard(DataKeeper dataKeeper, CalcManager calcMan, Supervisor supervisor, TableManager tman) {
		if (calcMan.getNAvailableMethods() < 1) {
			// following string:  "No calculations available!"
			showMessage(res.getString("No_calculations"), true);
			return;
		}
		// following string: "Select the table for calculation"
		TableData td = tman.selectCurrTable(res.getString("Select_the_table_for1"));
		if (td == null)
			return;
		if (!td.table.hasData()) {
			td.table.loadData();
		}
		if (!td.table.hasData()) {
			showMessage(res.getString("Failed_to_load_data"), true);
			return;
		}
		this.sup = supervisor;
		this.calcMan = calcMan;
		if (sup != null && sup.getUI() != null) {
			frame = sup.getUI().getMainFrame();
		}
		if (frame == null) {
			frame = CManager.getAnyFrame();
		}

		int methodId = selectCalcMethod();
		if (methodId < 0)
			return;
		boolean ok = false;
		int fn[] = null;
		Vector attrDescr = null;
		do {
			String prompt = calcMan.getAttrSelectionPrompt(methodId);
			// following string: "Select attribute(s) for calculations"
			if (prompt == null) {
				prompt = res.getString("Select_attribute_s");
			}
			Vector attr = null;
			AttributeChooser attrSel = new AttributeChooser();
			attrSel.setDataKeeper(dataKeeper);
			attrSel.setSupervisor(supervisor);
			if (attrSel.selectColumns(td.table, prompt, supervisor.getUI()) == null)
				return;
			attr = attrSel.getSelectedColumnIds();
			attrDescr = attrSel.getAttrDescriptors();
			if (attr == null || attr.size() < 1)
				return;
			fn = td.table.getAttrIndices(attr);
			ok = calcMan.isApplicable(methodId, td.table, fn, attrDescr);
			if (!ok) {
				// following string: "The method "+calcMan.getMethodName(methodId)+" is not "+"applicable to the selected attributes!"
				showMessage(res.getString("The_method") + calcMan.getMethodName(methodId) + res.getString("is_not") + res.getString("applicable_to_the"), true);
			}
		} while (!ok);
		if (calcMan.applyCalcMethod(methodId, (DataTable) td.table, fn, attrDescr, td.layerId) == null) {
			showMessage(calcMan.getErrorMessage(), true);
		}
	}

	protected int selectCalcMethod() {
		int nMethods = calcMan.getNAvailableMethods();
		if (nMethods < 1)
			return -1;
		boolean differentGroups = false;
		String groupName = calcMan.getMethodGroupName(calcMan.getAvailableMethodId(0));
		for (int i = 1; i < nMethods; i++) {
			int mId = calcMan.getAvailableMethodId(i);
			if (!StringUtil.sameStrings(groupName, calcMan.getMethodGroupName(mId))) {
				differentGroups = true;
			}
		}
		Panel allP = new Panel();
		allP.setLayout(new BorderLayout());
		Panel pl = new Panel(), pr = null;
		pl.setLayout(new ColumnLayout());
		if (differentGroups) {
			pr = new Panel();
			pr.setLayout(new ColumnLayout());
			allP.add(pl, "West");
			allP.add(pr, "East");
		} else {
			allP.add(pl, "Center");
		}
		CheckboxGroup cbg = new CheckboxGroup();
		Checkbox calcMethods[] = new Checkbox[nMethods];
		groupName = null;
		for (int i = 0; i < nMethods; i++) {
			int mId = calcMan.getAvailableMethodId(i);
			if (differentGroups)
				if (!StringUtil.sameStrings(groupName, calcMan.getMethodGroupName(mId))) {
					groupName = calcMan.getMethodGroupName(mId);
					pl.add(new Line(false));
					pr.add(new Line(false));
					pr.add(new Label(groupName));
				} else {
					pr.add(new Label(""));
				}
			calcMethods[i] = new Checkbox(calcMan.getMethodName(mId), i == 0, cbg);
			pl.add(calcMethods[i]);
			String expl = calcMan.getMethodExplanation(mId);
			if (expl != null) {
				new PopupManager(calcMethods[i], expl, true);
			}
		}
		allP.add(new Line(false), "South");
		// following string:  "Calculate"
		OKDialog dlg = new OKDialog(frame, res.getString("Calculate"), true);
		dlg.addContent(allP);
		dlg.show();
		if (dlg.wasCancelled())
			return -1;
		for (int i = 0; i < calcMethods.length; i++)
			if (calcMethods[i].getState())
				return calcMan.getAvailableMethodId(i);
		return -1;
	}

	protected void showMessage(String msg, boolean error) {
		if (sup != null && sup.getUI() != null) {
			sup.getUI().showMessage(msg, error);
		}
	}

}
