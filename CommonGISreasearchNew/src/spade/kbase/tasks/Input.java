package spade.kbase.tasks;

import java.util.StringTokenizer;
import java.util.Vector;

public class Input {
	public Vector arguments = null;
	public boolean isOptional = false;

	/**
	* The argument liststr contains a list of identifiers separated by whitespaces
	*/
	public void setArguments(String liststr) {
		if (liststr == null)
			return;
		StringTokenizer st = new StringTokenizer(liststr, " ,;\t\n\r\f");
		if (!st.hasMoreTokens())
			return;
		arguments = new Vector(st.countTokens(), 5);
		while (st.hasMoreTokens()) {
			arguments.addElement(st.nextToken());
		}
	}

}