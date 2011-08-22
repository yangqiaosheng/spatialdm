package guide_tools.tutorial;

public class NumAnswer {
	/**
	* The case when the exact value is specified
	*/
	public float value = Float.NaN;
	/**
	* The case when the user may enter a number from an interval
	*/
	public float min = Float.NaN, max = Float.NaN;

	/**
	* Reads the answer from the given string. The string must contain either
	* the exact value (a float number) or the possible interval in the
	* format [<min_value>,<max_value>]
	*/
	public boolean getAnswerFromString(String str) {
		if (str == null)
			return false;
		str = str.trim();
		if (str.length() < 1)
			return false;
		if (str.charAt(0) == '[') { //this is an interval
			int idx = str.indexOf(']', 1);
			if (idx < 0) {
				idx = str.length();
			}
			str = str.substring(1, idx).trim();
			idx = str.indexOf(';');
			if (idx < 0) {
				idx = str.indexOf(',');
			}
			if (idx < 0) {
				System.out.println("incorrectly specified interval: " + str);
				return false;
			}
			try {
				min = Float.valueOf(str.substring(0, idx)).floatValue();
				max = Float.valueOf(str.substring(idx + 1)).floatValue();
			} catch (NumberFormatException nfe) {
				System.out.println("incorrectly specified interval " + "(cannot read a number): " + str);
				return false;
			}
		} else {
			try {
				value = Float.valueOf(str).floatValue();
			} catch (NumberFormatException nfe) {
				System.out.println("incorrectly specified numeric value: " + str);
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		if (!Float.isNaN(value))
			return String.valueOf(value);
		if (!Float.isNaN(min) && !Float.isNaN(max))
			return "from " + min + " to " + max;
		return "unspecified";
	}

	public boolean isAnswerCorrect(float v) {
		if (Float.isNaN(v))
			return false;
		if (!Float.isNaN(value))
			if (value != 0)
				return Math.abs((v - value) / value) < 0.001f;
			else
				return Math.abs(v) < 0.001f;
		if (!Float.isNaN(min) && !Float.isNaN(max))
			return v >= min && v <= max;
		return false;
	}

	public boolean isAnswerCorrect(Float fl) {
		if (fl == null)
			return false;
		return isAnswerCorrect(fl.floatValue());
	}

	public boolean isAnswerCorrect(Object obj) {
		if (obj == null)
			return false;
		if (obj instanceof Float)
			return isAnswerCorrect((Float) obj);
		return false;
	}
}
