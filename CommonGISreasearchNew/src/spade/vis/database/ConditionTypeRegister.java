package spade.vis.database;

/**
* Defines the correspondence between possible types of query conditions, for
* example, "NumAttrCondition", and the classes implementing these condition
* types.
*/
public class ConditionTypeRegister {
	/**
	* Defines the correspondence between possible types of query conditions and
	* the classes implementing these condition types. For each condition
	* type, contains a pair {type, class name}.
	*/
	protected static String typeClasses[][] = { { "NumAttrCondition", "spade.vis.database.NumAttrCondition" }, { "QualAttrCondition", "spade.vis.database.QualAttrCondition" } };

	/**
	* For the given condition type, returns the name of the class implementing
	* this type of query condition, or null if the type is unknown.
	*/
	public static String getConditionClassName(String type) {
		if (type == null)
			return null;
		for (String[] typeClasse : typeClasses)
			if (typeClasse[0].equalsIgnoreCase(type))
				return typeClasse[1];
		return null;
	}

	/**
	* For the given condition type, returns an instance of the class implementing
	* this type of query condition, or null if the type is unknown or an instance
	* cannot be constructed for any other reason.
	*/
	public static Condition constructCondition(String type) {
		String name = getConditionClassName(type);
		if (name == null)
			return null;
		try {
			Condition c = (Condition) Class.forName(name).newInstance();
			return c;
		} catch (Exception e) {
		}
		return null;
	}
}
