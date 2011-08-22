package spade.analysis.decision;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Metrics;
import spade.lib.lang.Language;
import spade.lib.util.InfoSaver;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataTable;
import spade.vis.spec.DecisionSpec;

/**
* An implementation of the DecisionSupporter interface.
* A DecisionSupporter is linked to a table and reacts to appearance in it
* of a column with ranking or ordered classification of options. When such
* a column appears, starts a dialog in which the user can edit the order
* or classification and then store the result of decision making.
*/

public class DecisionSupporterImpl implements DecisionSupporter, ActionListener {
	static ResourceBundle resa = Language.getTextResource("spade.analysis.decision.Res");

	protected DataTable dTable = null;
	protected String sourceAttrId = null;
	protected Supervisor sup = null;
	protected Frame win = null;
	protected DecisionPanel dpanel = null;
	/**
	* The information necessary for storing results of decision making and voting.
	*/
	protected DecisionSpec dInfo = null;

	/**
	* Passes to the DecisionSupporter the information necessary for voting
	* and/or storing the decisions made.
	*/
	@Override
	public void setDecisionInfo(DecisionSpec decInfo) {
		dInfo = decInfo;
	}

	/**
	* Links the DecisionSupporter to the given table
	*/
	@Override
	public void setTable(DataTable table) {
		dTable = table;
		if (dTable != null) {
			dTable.setDecisionSupporter(this);
		}
	}

	/**
	* Passes the supervisor to the DecisionSupporter. The supervisor, in
	* particular, handles object events and their propagation among parallel
	* displays.
	*/
	@Override
	public void setSupervisor(Supervisor sup) {
		this.sup = sup;
	}

	/**
	* This method is called when a column with ranking or ordered classification
	* of options appears in the table. When such an event occurs,
	* the DecisionSupporter starts a dialog in which the user can edit the order
	* or classification and then store the result of decision making.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (win != null) {
			if (win.isShowing()) {
				win.dispose();
			}
			win = null;
		}
		sourceAttrId = (String) e.getNewValue();
		boolean useClasses = dInfo != null && dInfo.decisionType != null && dInfo.decisionType.equalsIgnoreCase("classification");
		dpanel = new DecisionPanel(dTable, sourceAttrId, sup, useClasses);
		if (!dpanel.hasContents())
			return;
		win = new Frame(resa.getString("Decision"));
		win.setLayout(new BorderLayout());
		win.add(dpanel, "Center");
		Panel p = new Panel(new FlowLayout(FlowLayout.CENTER, 30, 4));
		Button b = new Button("OK");
		p.add(b);
		b.setActionCommand("OK");
		b.addActionListener(this);
		b = new Button(resa.getString("Cancel"));
		p.add(b);
		b.setActionCommand("cancel");
		b.addActionListener(this);
		win.add(p, "South");
		win.pack();
		Dimension size = win.getSize();
		win.setBounds((Metrics.scrW() - size.width) / 2, (Metrics.scrH() - size.height) / 2, size.width, size.height);
		win.show();
		size = win.getPreferredSize();
		if (size.width > Metrics.scrW() * 2 / 3) {
			size.width = Metrics.scrW() * 2 / 3;
		}
		if (size.height > Metrics.scrH() * 3 / 4) {
			size.height = Metrics.scrH() * 3 / 4;
		}
		win.setBounds((Metrics.scrW() - size.width) / 2, (Metrics.scrH() - size.height) / 2, size.width, size.height);
		sup.getWindowManager().registerWindow(win);
	}

	/**
	* Reaction to pressing "OK" or "Cancel" button in the dialog
	*/
	@Override
	public void actionPerformed(ActionEvent e) {
		if (win == null)
			return;
		if (e.getActionCommand().equals("cancel")) {
			disposeWindow();
			//remove the temporary attribute
			int aIdx = dTable.getAttrIndex(sourceAttrId);
			if (aIdx >= 0 && dTable.getAttributeName(aIdx).startsWith("temporary")) {
				dTable.removeAttribute(aIdx);
			}
		} else if (e.getActionCommand().equals("OK")) {
			String userID = dpanel.getUserID();
			String sVotingServletURL = sup.getSystemSettings().getParameterAsString("VotingServletURL");
			boolean hasVotingServlet = (sVotingServletURL != null && sVotingServletURL.length() > 0);
			if (userID.indexOf("?") > -1 && hasVotingServlet) {
				userID = dpanel.changeUserID();
			}
			disposeWindow();

			int res[] = dpanel.getResult();

			if (res == null)
				return;
			//store results in the table
			int aIdx = dTable.getAttrIndex(sourceAttrId);
			Vector derFrom = null;
			if (aIdx >= 0 && dTable.getAttributeName(aIdx).startsWith("temporary")) {
				derFrom = dTable.getAttributeDependencyList(aIdx);
				dTable.removeAttribute(aIdx);
			}
			boolean isClass = dpanel.isResultClassification();
			int idx = dTable.addDerivedAttribute("decision", AttributeTypes.integer, (isClass) ? AttributeTypes.classify_order : AttributeTypes.evaluate_rank, derFrom);
			for (int i = 0; i < res.length; i++) {
				dTable.getDataRecord(i).setAttrValue(String.valueOf(res[i]), idx);
			}
			if (sup == null)
				return;

			boolean isApplet = !sup.getSystemSettings().checkParameterValue("isLocalSystem", "true");

			if (isApplet && ((dInfo == null || dInfo.resultScript == null) && !hasVotingServlet)) {
				//this is an applet; there must be a script for saving decisions
				//specified in system settings
				// following text: "The script for storing decisions on the server is not specified!
				sup.getUI().showMessage(resa.getString("The_script_for") + resa.getString("server_is_not"), true);
				return;
			}
			InfoSaver is = new InfoSaver();
			is.setIsApplet(isApplet);
			if (isApplet) {
				Object obj = sup.getSystemSettings().getParameter("DocumentBase");
				if (obj != null && (obj instanceof java.net.URL)) {
					is.setDocBaseURL((java.net.URL) obj);
				}
			}
			if (!hasVotingServlet) {
				if (dInfo != null) {
					is.setPathToScript(dInfo.resultScript);
				}
				String fname = null;
				if (dInfo != null) {
					fname = dInfo.resultFile;
				}
				if (fname == null) {
					fname = is.generateFileName() + ".csv";
				}
				String dir = null;
				if (dInfo != null) {
					dir = dInfo.resultDir;
				}
				if (dir != null && dir.length() > 0) {
					if (!dir.endsWith("/") && !dir.endsWith("\\")) {
						dir += "/";
					}
					fname = dir + fname;
				}
				is.setFileName(fname);
				is.saveString("id,name,result");
				for (int i = 0; i < res.length; i++) {
					is.saveString(dTable.getDataItemId(i) + "," + dTable.getDataItemName(i) + "," + String.valueOf(res[i]));
				}
				is.finish();
				// following Text: "The results are saved to file "
				sup.getUI().showMessage(resa.getString("The_results_are_saved") + fname);
			} else {
				String applName = sup.getSystemSettings().getParameterAsString("Application");
				if (applName == null || applName.length() < 1) {
					sup.getSystemSettings().getParameterAsString("APPL_NAME");
				}
				try {
					is.setVotingServletURL(sVotingServletURL);
					is.saveStringThroughServlet(applName + "," + userID);
					for (int i = 0; i < res.length; i++) {
						is.saveStringThroughServlet(dTable.getDataItemId(i) + ",\"" + dTable.getDataItemName(i) + "\"," + String.valueOf(res[i]));
					}
					is.saveStringThroughServlet("<EOF>");
					is.finish();
				} catch (Exception myEx) {
					sup.getUI().showMessage(myEx.toString(), true);
					return;
				}
				sup.getUI().showMessage(resa.getString("Voting_was_saved") + sVotingServletURL);
				//"Voting was saved through servlet "
			}
		}
	}

	protected void disposeWindow() {
		if (win != null) {
			CManager.destroyComponent(win);
			win.dispose();
			win = null;
		}
	}
}