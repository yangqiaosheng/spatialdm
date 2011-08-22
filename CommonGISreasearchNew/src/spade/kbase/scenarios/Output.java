package spade.kbase.scenarios;

/**
* An output of a data analysis tool, an internal representation of the "Output"
* element of the task knowledge base.
*/
public class Output {
	public static final String outputTypes[] = { "layer", "map_vis", "thematic_data" };
	public String localId = null;
	public String type = null;
	public String refersTo = null;

	public void setOutputType(String value) {
		if (value != null && value.length() > 0) {
			for (String outputType : outputTypes)
				if (value.equalsIgnoreCase(outputType)) {
					type = value;
					break;
				}
		}
	}

	public boolean isValid() {
		return type != null;
	}

	@Override
	public String toString() {
		if (refersTo != null)
			return "Output: id=" + localId + " type=" + type + " refers_to=" + refersTo;
		return "Output: id=" + localId + " type=" + type;
	}
}