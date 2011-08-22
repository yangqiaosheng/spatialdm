package spade.analysis.decision;

import java.beans.PropertyChangeListener;

import spade.analysis.system.Supervisor;
import spade.vis.database.DataTable;
import spade.vis.spec.DecisionSpec;

/**
* A DecisionSupporter is linked to a table and reacts to appearance in it
* of a column with ranking or ordered classification of options. When such
* a column appears, sallows the user to edit the order
* or classification and then store the result of decision making.
*/

public interface DecisionSupporter extends PropertyChangeListener {
	/**
	* Passes to the DecisionSupporter the information necessary for voting
	* and/or storing the decisions made.
	*/
	public void setDecisionInfo(DecisionSpec decInfo);

	/**
	* Links the DecisionSupporter to the given table
	*/
	public void setTable(DataTable table);

	/**
	* Passes the supervisor to the DecisionSupporter. The supervisor, in
	* particular, handles object events and their propagation among parallel
	* displays.
	*/
	public void setSupervisor(Supervisor sup);
}
