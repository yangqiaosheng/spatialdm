package spade.kbase.tasks;

import java.util.StringTokenizer;
import java.util.Vector;

public class VisReq {
	public Vector variants = null;
	public Vector applyTo = null;

	public void addVisVariant(VisVariant variant) {
		if (variant == null)
			return;
		if (variants == null) {
			variants = new Vector(10, 10);
		}
		if (!variants.contains(variant)) {
			variants.addElement(variant);
		}
	}

	public int getNVariants() {
		if (variants == null)
			return 0;
		return variants.size();
	}

	public VisVariant getVisVariant(int idx) {
		if (idx >= 0 && idx < getNVariants())
			return (VisVariant) variants.elementAt(idx);
		return null;
	}

	/**
	* The argument liststr contains a list of identifiers separated by whitespaces
	*/
	public void setApplyTo(String liststr) {
		if (liststr == null)
			return;
		StringTokenizer st = new StringTokenizer(liststr, " ,;\t\n\r\f");
		if (!st.hasMoreTokens())
			return;
		applyTo = new Vector(st.countTokens(), 5);
		while (st.hasMoreTokens()) {
			applyTo.addElement(st.nextToken());
		}
	}

	public boolean isValid() {
		return applyTo != null && applyTo.size() > 0 && getNVariants() > 0;
	}
}