package spade.kbase.tasks;

public class AttrRestriction {
	public static final String attrTypes[] = { "any", "numeric", "qualitative", "logical" };
	public static final String relations[] = { "any", "inclusion", "parts_in_whole", "comparability", "none" };

	public int minNumber = 1, maxNumber = Integer.MAX_VALUE;
	protected int typeN = 0; //index in the array attrTypes, "any" by default
	protected int relN = 0; //index in the array relations, "any" by default

	public void setAllowedType(String allowedType) {
		if (allowedType == null)
			return;
		for (int i = 0; i < attrTypes.length; i++)
			if (allowedType.equalsIgnoreCase(attrTypes[i])) {
				typeN = i;
				break;
			}
	}

	public boolean isTypeAllowed(String aType) {
		if (aType == null)
			return false;
		return aType.equalsIgnoreCase(attrTypes[typeN]);
	}

	public void setAllowedRelation(String allowedRelation) {
		if (allowedRelation == null)
			return;
		for (int i = 0; i < relations.length; i++)
			if (allowedRelation.equalsIgnoreCase(relations[i])) {
				relN = i;
				break;
			}
	}

	public boolean isRelationAllowed(String relation) {
		if (relation == null)
			return false;
		return relation.equalsIgnoreCase(relations[relN]);
	}

	@Override
	public String toString() {
		return "AttrRestriction:\nminNumber=" + minNumber + " maxNumber=" + maxNumber + "\ntype=" + attrTypes[typeN] + " relation=" + relations[relN];
	}
}