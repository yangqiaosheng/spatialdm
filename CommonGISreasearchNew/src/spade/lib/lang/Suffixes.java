package spade.lib.lang;

/**
* This class specifies the "suffixes" to be used to generate
* the names of resource classes for different languages. For
* example, resources for german language are specified in classes
* named Res_de (one such class per package). 
*/

public class Suffixes {
	/**
	* The correspondence between the possible interface languages and the sufiixes.
	*/
	protected static final String[][] corr = new String[][] { { "english", "" }, { "german", "_de" }, { "french", "_fr" }, { "romanian", "_rom" }, { "czech", "_cz" } };

	/**
	* Returns the suffix corresponding to the given language. May return an empty
	* string.
	*/
	public String getSuffix(String language) {
		if (language == null)
			return "";
		for (String[] element : corr)
			if (language.equalsIgnoreCase(element[0]))
				return element[1];
		return "";
	}
}