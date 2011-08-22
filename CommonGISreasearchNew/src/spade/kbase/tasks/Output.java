package spade.kbase.tasks;

public class Output {
	public static final String outputTypes[] = { "layer", "attribute", "subset", "groups", "order", "table", "stat_values" };
	public String localId = null;
	public String type = null;
	public boolean multiple = false;
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
}