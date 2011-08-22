package spade.kbase.tasks;

import java.util.Vector;

public class AndContextMapping extends ContextMapping {
	public Vector cmappings = null;

	public void addContextMapping(ContextMapping cmap) {
		if (cmap == null || !cmap.isValid())
			return;
		if (cmappings == null) {
			cmappings = new Vector(5, 5);
		}
		if (!cmappings.contains(cmap)) {
			cmappings.addElement(cmap);
		}
	}

	public int getNContextMappings() {
		if (cmappings == null)
			return 0;
		return cmappings.size();
	}

	public ContextMapping getContextMapping(int idx) {
		if (idx < 0 || idx >= getNContextMappings())
			return null;
		return (ContextMapping) cmappings.elementAt(idx);
	}

	@Override
	public boolean isValid() {
		return getNContextMappings() > 0;
	}
}