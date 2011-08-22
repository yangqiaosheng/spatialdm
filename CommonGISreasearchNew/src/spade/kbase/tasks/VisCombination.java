package spade.kbase.tasks;

import java.util.Vector;

public class VisCombination {
	public static final String combMethods[] = { "overlay", "multimap" };
	public static final int OVERLAY = 0, MULTIMAP = 1;

	public int method = 1;
	public Vector components = null;

	public void setCombinationMethod(String value) {
		if (value != null && value.length() > 0) {
			for (int i = 0; i < combMethods.length; i++)
				if (value.equalsIgnoreCase(combMethods[i])) {
					method = i;
					break;
				}
		}
	}

	public void addVisComponent(VisReq comp) {
		if (comp == null)
			return;
		if (components == null) {
			components = new Vector(10, 10);
		}
		if (!components.contains(comp)) {
			components.addElement(comp);
		}
	}

	public int getNComponents() {
		if (components == null)
			return 0;
		return components.size();
	}

	public VisReq getVisComponent(int idx) {
		if (idx >= 0 && idx < getNComponents())
			return (VisReq) components.elementAt(idx);
		return null;
	}

	public boolean isValid() {
		return getNComponents() >= 2;
	}
}