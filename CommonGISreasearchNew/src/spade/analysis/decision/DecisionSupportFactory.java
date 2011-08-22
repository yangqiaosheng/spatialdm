package spade.analysis.decision;

import java.util.ResourceBundle;

import spade.analysis.system.Supervisor;
import spade.lib.lang.Language;
import spade.vis.database.DataTable;
import spade.vis.spec.DecisionSpec;

/**
* A DecisionSupportFactory is used to construct instances of DecisionSupporterImpl.
* It does this indirectly, using Class.forName(...).newInstance().
* This allows to avoid import of the class DecisionSupporterImpl and, concequently,
* automatic inclusion of this class and the classes it uses in application
* deployment. Hence, it is possible to decide on the stage of deployment
* whether
*/

public class DecisionSupportFactory {
	static ResourceBundle res = Language.getTextResource("spade.analysis.decision.Res");

	public static DecisionSupporter makeDecisionSupporter(DecisionSpec decisionInfo, DataTable dTable, Supervisor sup) {
		if (sup == null)
			return null;
		if (!sup.getSystemSettings().checkParameterValue("isLocalSystem", "true")) {
			//this is an applet; there must be a script for saving decisions
			//specified in system settings
			String sVotingServletURL = sup.getSystemSettings().getParameterAsString("VotingServletURL");
			boolean hasVotingServlet = (sVotingServletURL != null && sVotingServletURL.length() > 0);

			if ((decisionInfo == null || decisionInfo.resultScript == null) && !hasVotingServlet) {
				//following text: "The script for storing decisions on the server is not specified!"
				sup.getUI().showMessage(res.getString("The_script_for") + res.getString("server_is_not"), true);
				return null;
			}
		}
		System.out.println("Trying to construct a DecisionSupporter");
		DecisionSupporter ds = null;
		try {
			ds = (DecisionSupporter) Class.forName("spade.analysis.decision.DecisionSupporterImpl").newInstance();
		} catch (Exception e) {
			System.out.println("Failed to construct a DecisionSupporter: " + e.toString());
		}
		if (ds != null) {
			ds.setDecisionInfo(decisionInfo);
			ds.setTable(dTable);
			ds.setSupervisor(sup);
		}
		return ds;
	}
}