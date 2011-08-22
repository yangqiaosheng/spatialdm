package spade.kbase.scenarios;

import java.util.StringTokenizer;
import java.util.Vector;

/**
* Priorities for selection of cartographic visualisation methods, an internal
* representation of the "Priority" element of the task knowledge base.
*/
public class Priority {
	/**
	* order of preference (priority)
	*/
	public Vector order = null;

	/**
	* "variable" (visual variable) or "method" (cartographic representation method)
	*/
	public String refersTo = null;

	/**
	* The argument liststr contains a list of values separated by whitespaces
	*/
	public void setOrder(String liststr) {
		if (liststr == null)
			return;
		StringTokenizer st = new StringTokenizer(liststr, " ,;\t\n\r\f");
		if (!st.hasMoreTokens())
			return;
		order = new Vector(st.countTokens(), 5);
		while (st.hasMoreTokens()) {
			order.addElement(st.nextToken());
		}
	}

	@Override
	public String toString() {
		String str = "Priority: ";
		if (refersTo != null) {
			str += "refers_to " + refersTo + " ";
		}
		if (order != null && order.size() > 0) {
			str += "order=<";
			for (int i = 0; i < order.size(); i++)
				if (i > 0) {
					str += "," + order.elementAt(i);
				} else {
					str += order.elementAt(i);
				}
			str += ">";
		}
		return str;
	}
}