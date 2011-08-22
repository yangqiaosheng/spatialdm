package spade.kbase.tasks;

import java.util.Vector;

public class VisVariant {
	public static final String variables[] = { "size", "value", "colour", "shape", "orientation", "presence" };
	public static final String types[] = { "painting", "signs" };
	public static final String methods[] = { "qualitative_colour", "shape", "logical_bunch", "value_paint", "multiples", "standalone_bars", "parallel_bars", "radial_bars", "segmented_bars", "doublesided_bars", "pies", "inclusion_signs", "class1D",
			"class2D", "class_dominant" };

	public boolean isDefault = false;
	public String visVariable = null; //size, value, colour, shape, etc.
	public String visType = null; //painting or signs
	public String visMethod = null; //one of the methods listed above
	public int complexity = 1;

	public String name = null;
	public String explanation = null;
	public Vector outputs = null;

	public void setVisVariable(String value) {
		if (value != null && value.length() > 0) {
			for (String variable : variables)
				if (value.equalsIgnoreCase(variable)) {
					visVariable = value;
					break;
				}
		}
	}

	public void setVisType(String value) {
		if (value != null && value.length() > 0) {
			for (String type : types)
				if (value.equalsIgnoreCase(type)) {
					visType = value;
					break;
				}
		}
	}

	public void setVisMethod(String value) {
		if (value != null && value.length() > 0) {
			for (String method : methods)
				if (value.equalsIgnoreCase(method)) {
					visMethod = value;
					break;
				}
		}
	}

	public void addOutputSpec(Output out) {
		if (out == null)
			return;
		if (outputs == null) {
			outputs = new Vector(5, 5);
		}
		if (!outputs.contains(out)) {
			outputs.addElement(out);
		}
	}

	public int getNOutputs() {
		if (outputs == null)
			return 0;
		return outputs.size();
	}

	public Output getOutputSpec(int idx) {
		if (idx < 0 || idx >= getNOutputs())
			return null;
		return (Output) outputs.elementAt(idx);
	}

	public boolean isValid() {
		return (visVariable != null || visType != null || visMethod != null) && name != null && explanation != null;
	}

	public String getName() {
		return name;
	}

	public void setName(String txt) {
		name = txt;
	}

	public String getExplanation() {
		return explanation;
	}

	public void setExplanation(String txt) {
		explanation = txt;
	}

}
