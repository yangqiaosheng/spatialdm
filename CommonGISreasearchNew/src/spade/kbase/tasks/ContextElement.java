package spade.kbase.tasks;


public class ContextElement {
	public static final String contextTypes[] = { "territory", "layer", "table", "attribute" };
	public static final int territory = 0, layer = 1, table = 2, attribute = 3;

	protected int typeN = -1; //index in the array contextTypes

	public String localId = null, refersTo = null; //attributes of the element
	public Object restriction = null; //either a LayerRestriction or an AttrRestriction,
										//depending on the type of the context element

	public String name = null;
	public String explanation = null;
	public String instruction = null;

	public void setType(int type) {
		if (type >= 0 && type < contextTypes.length) {
			typeN = type;
		}
	}

	public void setType(String type) {
		typeN = -1;
		if (type == null)
			return;
		for (int i = 0; i < contextTypes.length && typeN < 0; i++)
			if (type.equalsIgnoreCase(contextTypes[i])) {
				typeN = i;
			}
	}

	public String getTypeName() {
		if (typeN < 0)
			return null;
		return contextTypes[typeN];
	}

	public int getType() {
		return typeN;
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

	public String getInstruction() {
		return instruction;
	}

	public void setInstruction(String txt) {
		instruction = txt;
	}

	public void setRestriction(Object restriction) {
		if (restriction == null) {
			this.restriction = null;
		} else if ((typeN == layer && (restriction instanceof LayerRestriction)) || (typeN == attribute && (restriction instanceof AttrRestriction))) {
			this.restriction = restriction;
		}
	}

	public Object getRestriction() {
		return restriction;
	}
}
