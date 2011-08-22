package spade.kbase.tasks;

import java.util.Vector;

public class NextTaskSpec {
	public String explanation = null;

	public Vector cmappings = null;

	/**
	* cmap may be an instance of either either ContextMapping or AndContextMapping
	*/
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

	public String getExplanation() {
		return explanation;
	}

	public void setExplanation(String txt) {
		explanation = txt;
	}

}