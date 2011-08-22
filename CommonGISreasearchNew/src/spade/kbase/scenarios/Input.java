package spade.kbase.scenarios;


/**
* Represents specification of an input of an instrument.
*/
public class Input {
	/**
	* actual argument, i.e. identifier of some context element
	*/
	public String arg = null;
	/**
	* Formal argument mentioned in the description of the tool this instrument
	* refers to. May be null (for map visualisations where arguments are clear)
	*/
	public String standsFor = null;

	@Override
	public String toString() {
		String str = "Input: arg=" + arg;
		if (standsFor != null) {
			str += " stands_for=" + standsFor;
		}
		return str;
	}

}