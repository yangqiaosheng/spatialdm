package guide_tools.guide;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import spade.analysis.system.SystemManager;
import spade.kbase.scenarios.TaskKBase;
import spade.kbase.scenarios.TreeNode;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Metrics;

public class AnalysisGuide implements ActionListener {
	/**
	* The GuideCore contains the knowledge base, the context currently defined,
	* references to the system manager, current scenario etc. as well as
	* some constants used in different components of the Guide
	*/
	protected GuideCore core = null;

	protected ContextDefWizard cdw = null;
	protected Frame treeView = null;

	public AnalysisGuide(TaskKBase kbase, SystemManager sysman) {
		core = new GuideCore(sysman, kbase);
	}

	public void setMainWindow(Frame mainWin) {
		core.setCurrentFrame(mainWin);
	}

	public void start() {
		if (cdw != null || treeView != null)
			return;
		if (core.sysMan == null || core.kb == null)
			return;
		core.scenario = selectScenario();
		if (core.scenario == null)
			return;
		cdw = new ContextDefWizard();
		cdw.specifyContext(this, core, core.scenario, true);
	}

	public boolean isRunning() {
		return cdw != null || treeView != null;
	}

	/**
	* Currently selects the first core.scenario from the available scenarios.
	* Later will start a dialog for selection of the scenario.
	*/
	protected TreeNode selectScenario() {
		if (core.kb == null || core.kb.tasks == null)
			return null;
		Vector scv = core.kb.tasks.getScenarios();
		if (scv == null || scv.size() < 1)
			return null;
		return (TreeNode) scv.elementAt(0);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd == null)
			return;
		if (e.getSource().equals(cdw)) {
			if (cmd.equals("finished")) {
				if (cdw.getResult()) {
					startTaskSupport();
				}
				cdw = null;
			}
		} else if (e.getSource() instanceof TaskSupportPanel) {
			if (cmd.equals("Cancel")) {
				finish();
			}
		}
	}

	public void startTaskSupport() {
		if (core.scenario == null || treeView != null)
			return;
		TaskSupportPanel tspan = new TaskSupportPanel(core, core.scenario);
		tspan.addActionListener(this);
		treeView = new Frame("Scenario: " + core.scenario.getName());
		core.setCurrentFrame(treeView);
		treeView.setLayout(new BorderLayout());
		treeView.add(tspan, "Center");
		treeView.pack();
		Dimension frsz = treeView.getSize();
		if (frsz.width < Metrics.mm() * 150) {
			frsz.width = Metrics.mm() * 150;
		}
		if (frsz.height < Metrics.mm() * 120) {
			frsz.height = Metrics.mm() * 120;
		}
		int sw = Metrics.scrW(), sh = Metrics.scrH();
		if (frsz.width > sw * 2 / 3) {
			frsz.width = sw * 2 / 3;
		}
		if (frsz.height > sh * 2 / 3) {
			frsz.height = sh * 2 / 3;
		}
		treeView.setBounds((sw - frsz.width) / 2, (sh - frsz.height) / 2, frsz.width, frsz.height);
		treeView.show();
	}

	public void finish() {
		if (treeView != null) {
			treeView.dispose();
			CManager.destroyComponent(treeView);
			treeView = null;
		}
		if (cdw != null) {
			cdw.finish();
			cdw = null;
		}
	}
}
