package spade.kbase.scenarios;

import java.util.StringTokenizer;
import java.util.Vector;

/**
* A data analysis instrument, an internal representation of the "Instrument"
* element of the task knowledge base.
*/
public class Instrument {
	static public final String specials[] = { "additional_map" };
	/**
	* One of "instruct", "map_vis" and "tool". "Tool" means use of interactive
	* facilities available in the system or additional graphs linked to maps.
	*/
	public String type = null;
	/**
	* When the type of the instrument is "tool", the variable function indicates
	* the kind of tool
	*/
	public String function = null;
	public boolean isDefault = false;

	public String name = null, explanation = null, instruction = null;

	public Vector restrictions = null; //Vector of instances of Restriction
	public Vector priorities = null; //Vector of instances of Priority
	public Vector inputs = null; //Vector of instances of Input
	/**
	* Requirements to tasks to be supported by the instrument (one of the elements
	* of the array Common.primTasks).  Relevant, in particular, to visualization
	* design and to selection of appropriate instructions for instruments.
	*/
	public Vector useReq = null;
	/**
	* Tasks that are desired to be supported but not critical
	*/
	public Vector useDesired = null;
	/**
	* Some special requirements, for example, additional map display
	*/
	public Vector specReq = null;

	public void setInstrumentType(String value) {
		type = value;
	}

	public void addInput(Input input) {
		if (input == null)
			return;
		if (inputs == null) {
			inputs = new Vector(10, 5);
		}
		inputs.addElement(input);
	}

	public int getInputCount() {
		if (inputs == null)
			return 0;
		return inputs.size();
	}

	public Input getInput(int idx) {
		if (idx < 0 || idx >= getInputCount())
			return null;
		return (Input) inputs.elementAt(idx);
	}

	public void addRestriction(Restriction restr) {
		if (restr == null)
			return;
		if (restrictions == null) {
			restrictions = new Vector(10, 5);
		}
		restrictions.addElement(restr);
	}

	public void addPriority(Priority prior) {
		if (prior == null)
			return;
		if (priorities == null) {
			priorities = new Vector(10, 5);
		}
		priorities.addElement(prior);
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

	/**
	* Sets the required tasks to be supported by the instrument (one of the
	* elements of the array Common.primTasks).
	* The argument liststr contains a list of values separated by whitespaces
	*/
	public void setRequiredUses(String liststr) {
		if (liststr == null)
			return;
		StringTokenizer st = new StringTokenizer(liststr, " ,;\t\n\r\f");
		if (!st.hasMoreTokens())
			return;
		useReq = new Vector(st.countTokens(), 5);
		while (st.hasMoreTokens()) {
			String tsk = st.nextToken();
			useReq.addElement(tsk);
		}
	}

	/**
	* Sets the desired tasks to be supported by the instrument (one of the
	* elements of the array Common.primTasks).
	* The argument liststr contains a list of values separated by whitespaces
	*/
	public void setDesiredUses(String liststr) {
		if (liststr == null)
			return;
		StringTokenizer st = new StringTokenizer(liststr, " ,;\t\n\r\f");
		if (!st.hasMoreTokens())
			return;
		useDesired = new Vector(st.countTokens(), 5);
		while (st.hasMoreTokens()) {
			String tsk = st.nextToken();
			useDesired.addElement(tsk);
		}
	}

	/**
	* The argument liststr contains a list of values separated by whitespaces
	*/
	public void setSpecialDemands(String liststr) {
		if (liststr == null)
			return;
		StringTokenizer st = new StringTokenizer(liststr, " ,;\t\n\r\f");
		if (!st.hasMoreTokens())
			return;
		specReq = new Vector(st.countTokens(), 5);
		while (st.hasMoreTokens()) {
			String str = st.nextToken();
			int n = -1;
			for (int i = 0; i < specials.length && n < 0; i++)
				if (str.equalsIgnoreCase(specials[i])) {
					n = i;
				}
			if (n >= 0) {
				specReq.addElement(specials[n]);
			}
		}
	}

	@Override
	public String toString() {
		String str = "Instrument: type=" + type;
		if (function != null) {
			str += " function=" + function;
		}
		if (isDefault) {
			str += " default";
		}
		if (specReq != null && specReq.size() > 0) {
			str += " special_req=<";
			for (int i = 0; i < specReq.size(); i++)
				if (i == 0) {
					str += specReq.elementAt(i);
				} else {
					str += "," + specReq.elementAt(i);
				}
			str += ">";
		}
		if (useReq != null && useReq.size() > 0) {
			str += " required_tasks=<";
			for (int i = 0; i < useReq.size(); i++)
				if (i == 0) {
					str += useReq.elementAt(i);
				} else {
					str += "," + useReq.elementAt(i);
				}
			str += ">";
		}
		if (useDesired != null && useDesired.size() > 0) {
			str += " desired_tasks=<";
			for (int i = 0; i < useDesired.size(); i++)
				if (i == 0) {
					str += useDesired.elementAt(i);
				} else {
					str += "," + useDesired.elementAt(i);
				}
			str += ">";
		}
		str += "\n";
		if (name != null) {
			str += "  Name=<" + name + ">\n";
		}
		if (explanation != null) {
			str += "  Explanation=<" + explanation + ">\n";
		}
		if (explanation != null) {
			str += "  Instruction=<" + instruction + ">\n";
		}
		if (restrictions != null && restrictions.size() > 0) {
			for (int i = 0; i < restrictions.size(); i++) {
				str += "  " + restrictions.elementAt(i).toString() + "\n";
			}
		}
		if (priorities != null && priorities.size() > 0) {
			for (int i = 0; i < priorities.size(); i++) {
				str += "  " + priorities.elementAt(i).toString() + "\n";
			}
		}
		if (inputs != null && inputs.size() > 0) {
			for (int i = 0; i < inputs.size(); i++) {
				str += "  " + inputs.elementAt(i).toString() + "\n";
			}
		}
		str += "/Instrument\n";
		return str;
	}
}