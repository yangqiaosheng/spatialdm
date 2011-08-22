package spade.kbase.scenarios;

import java.util.Vector;

/**
* Manager of a collection of specifications of map visualization methods
*/

public class VisCollection {
	protected Vector visMethods = null; // Vector of Visualization

	public Vector getVisMethods() {
		return visMethods;
	}

	public void addVisMethod(Visualization vis) {
		if (vis == null)
			return;
		if (visMethods == null) {
			visMethods = new Vector(50, 20);
		}
		visMethods.addElement(vis);
	}

	public int getMethodCount() {
		if (visMethods == null)
			return 0;
		return visMethods.size();
	}

	public Visualization getVisMethod(int idx) {
		if (idx < 0 || idx >= getMethodCount())
			return null;
		return (Visualization) visMethods.elementAt(idx);
	}

	public String getVisMethodName(int idx) {
		Visualization vis = getVisMethod(idx);
		if (vis != null)
			return vis.getName();
		else
			return null;
	}

	public void printVisMethods() {
		for (int i = 0; i < getMethodCount(); i++) {
			System.out.println(visMethods.elementAt(i).toString());
		}
	}

	public Visualization findVisMethod(String method) {
		if (method == null)
			return null;
		for (int i = 0; i < getMethodCount(); i++) {
			Visualization vis = (Visualization) visMethods.elementAt(i);
			if (method.equals(vis.method))
				return vis;
		}
		return null;
	}

	/**
	* Selects suitable visualizations for the given number of attributes
	* with the specified types and relationships between them. The relationships
	* may be undefined. In this case VisCollection selects only
	* relationships-insensitive methods. Possible types and relationships are
	* defined in the class Common.
	* The vector requiredUses specifies the list of primitive tasks that are
	* to be supported (mandatorily). Only the methods that support these tasks
	* are selected.
	* If there are several suitable methods, they are ordered according to the
	* specified priorities. If priority for two methods is the same, they are
	* ordered according to increasing complexity.
	*/
	public Vector selectVisMethods(int nAttrs, char attrTypes[], String attrRel, Vector requiredUses, Vector priorities) {
		Vector selected = new Vector(5, 5);
		for (int i = 0; i < getMethodCount(); i++) {
			Visualization vis = getVisMethod(i);
			//is the method applicable to the given attributes?
			if (!vis.isApplicable(nAttrs, attrTypes, attrRel)) {
				continue;
			}
			//does the method support all the required tasks?
			boolean supports = true;
			if (requiredUses != null && requiredUses.size() > 0) {
				for (int j = 0; j < requiredUses.size() && supports; j++) {
					supports = vis.isTaskSupported((String) requiredUses.elementAt(j)) || vis.isTaskSupportedByManipulator((String) requiredUses.elementAt(j));
				}
			}
			if (supports) {
				selected.addElement(vis);
			}
		}
		if (selected.size() < 1)
			return null;
		selected.trimToSize();
		if (selected.size() > 1) {
			for (int i = 1; i < selected.size(); i++) {
				int j = i - 1;
				while (j >= 0 && getOrder((Visualization) selected.elementAt(j), (Visualization) selected.elementAt(i), priorities) > 0) {
					--j;
				}
				++j;
				if (j < i) {
					Object obj = selected.elementAt(i);
					selected.removeElementAt(i);
					selected.insertElementAt(obj, j);
				}
			}
		}
		return selected;
	}

	/**
	* Returns -1 if vis1 should come earlier than vis2, 1 if later, and 0 if
	* they are equivalent
	*/
	protected int getOrder(Visualization vis1, Visualization vis2, Vector priorities) {
		if (priorities != null) {
			for (int i = 0; i < priorities.size(); i++) {
				Priority prior = (Priority) priorities.elementAt(i);
				if (prior.order == null || prior.order.size() < 1) {
					continue;
				}
				//a priority can refer either to the visual variable or to the method
				char ref = 0;
				if (prior.refersTo.equals("variable")) {
					ref = 'v';
				} else if (prior.refersTo.equals("method")) {
					ref = 'm';
				} else {
					continue;
				}
				boolean equal = false;
				for (int j = 0; j < prior.order.size() && !equal; j++) {
					String val = (String) prior.order.elementAt(j);
					if (val == null) {
						continue;
					}
					switch (ref) {
					case 'v':
						if (val.equalsIgnoreCase(vis1.variable))
							if (val.equalsIgnoreCase(vis2.variable)) {
								equal = true;
							} else
								return -1;
						else if (val.equalsIgnoreCase(vis2.variable))
							return 1;
						break;
					case 'm':
						if (val.equalsIgnoreCase(vis1.method))
							if (val.equalsIgnoreCase(vis2.method)) {
								equal = true;
							} else
								return -1;
						else if (val.equalsIgnoreCase(vis2.method))
							return 1;
						break;
					}
				}
			}
		}
		if (vis1.complexity < vis2.complexity)
			return -1;
		if (vis1.complexity > vis2.complexity)
			return 1;
		return 0;
	}

}