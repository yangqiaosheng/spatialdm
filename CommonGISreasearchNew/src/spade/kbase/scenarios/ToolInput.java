package spade.kbase.scenarios;

import java.util.Vector;

/**
* Represents specification of an input of an instrument.
*/
public class ToolInput {
	public static final String inputTypes[] = { "layer", "attribute" };

	public String arg_id = null, arg_type = null;
	public String refersTo = null;
	public int minNumber = 1, maxNumber = 1; //-1 means unrestricted
	public Vector restrictions = null; //Vector of instances of Restriction

	public void setMin(String minStr) {
		if (minStr == null || minStr.length() < 1)
			return;
		try {
			minNumber = Integer.valueOf(minStr).intValue();
		} catch (NumberFormatException nfe) {
			minNumber = 1;
		}
	}

	public void setMax(String maxStr) {
		if (maxStr == null || maxStr.length() < 1)
			return;
		if (maxStr.equalsIgnoreCase("any")) {
			maxNumber = -1;
		} else {
			try {
				maxNumber = Integer.valueOf(maxStr).intValue();
			} catch (NumberFormatException nfe) {
				maxNumber = 1;
			}
		}
	}

	public void addRestriction(Restriction restr) {
		if (restr == null)
			return;
		if (restrictions == null) {
			restrictions = new Vector(10, 5);
		}
		restrictions.addElement(restr);
	}

	public ToolInput makeCopy() {
		ToolInput ti = new ToolInput();
		ti.arg_id = arg_id;
		ti.arg_type = arg_type;
		ti.refersTo = refersTo;
		ti.minNumber = minNumber;
		ti.maxNumber = maxNumber;
		if (restrictions != null) {
			ti.restrictions = (Vector) restrictions.clone();
		}
		return ti;
	}

	@Override
	public Object clone() {
		return makeCopy();
	}

	@Override
	public String toString() {
		String str = "ToolInput: arg_id=" + arg_id + " arg_type=" + arg_type;
		if (refersTo != null) {
			str += " refers_to=" + refersTo + " ";
		}
		str += " minNumber=" + minNumber + " maxNumber=" + maxNumber;
		if (restrictions != null && restrictions.size() > 0) {
			str += "\n";
			for (int i = 0; i < restrictions.size(); i++) {
				str += "  " + restrictions.elementAt(i).toString() + "\n";
			}
			str += "/ToolInput\n";
		}
		return str;
	}

}