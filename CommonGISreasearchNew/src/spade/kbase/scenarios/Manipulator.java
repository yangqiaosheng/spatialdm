package spade.kbase.scenarios;

import java.util.StringTokenizer;
import java.util.Vector;

/**
* A map manipulator description, an internal representation of the "Manipulator"
* element of the task knowledge base.
*/
public class Manipulator {
	public String name = null, explanation = null;

	/**
	* A map manipulation tool may be used for several primitive tasks
	* (see Common.primTasks). This vector contains the list of tasks supported
	* by the manipulator
	*/
	public Vector tasksSupported = null;
	/**
	* There may be seperate instructions for the primitive tasks supported.
	*/
	public Vector instructions = null, instrTasks = null;

	/**
	* Adds a task to the list of primitive tasks supported by this manipulator
	*/
	public void addSupportedTask(String task) {
		if (task == null)
			return;
		if (tasksSupported == null) {
			tasksSupported = new Vector(5, 5);
		}
		if (tasksSupported.indexOf(task) < 0) {
			tasksSupported.addElement(task);
		}
	}

	/**
	* The argument liststr contains a list of values separated by whitespaces
	*/
	public void setSupportedTasks(String liststr) {
		if (liststr == null)
			return;
		StringTokenizer st = new StringTokenizer(liststr, " ,;\t\n\r\f");
		if (!st.hasMoreTokens())
			return;
		while (st.hasMoreTokens()) {
			addSupportedTask(st.nextToken());
		}
	}

	public void addInstruction(String instr) {
		addInstruction(instr, null);
	}

	/**
	* The argument "task" indicates for what task (way of use of the instrument)
	* the instruction is relevant
	*/
	public void addInstruction(String instr, String task) {
		if (instr == null)
			return;
		if (instructions == null) {
			instructions = new Vector(10, 5);
		}
		instructions.addElement(instr);
		if (task == null)
			return;
		if (instrTasks == null) {
			instrTasks = new Vector(10, 5);
		}
		for (int i = instrTasks.size(); i < instructions.size() - 1; i++) {
			instrTasks.addElement(null);
		}
		instrTasks.addElement(task);
		if (tasksSupported == null) {
			tasksSupported = new Vector(5, 5);
		}
		if (tasksSupported.indexOf(task) < 0) {
			tasksSupported.addElement(task);
		}
	}

	/**
	* Returns the general instruction (i.e. not marked by a particular task)
	*/
	public String getGeneralInstruction() {
		if (instructions == null || instructions.size() < 1)
			return null;
		if (instrTasks == null)
			return (String) instructions.elementAt(0);
		for (int i = 0; i < instrTasks.size(); i++)
			if (instrTasks.elementAt(i) == null)
				return (String) instructions.elementAt(i);
		if (instructions.size() > instrTasks.size())
			return (String) instructions.elementAt(instrTasks.size());
		return null;
	}

	/**
	* Returns instruction associated with the given task (see the list of tasks
	* in Common.primTasks)
	*/
	public String getInstruction(String task) {
		if (task == null)
			return getGeneralInstruction();
		if (instructions == null || instructions.size() < 1)
			return null;
		if (instrTasks == null || instrTasks.size() < 1)
			return null;
		for (int i = 0; i < instrTasks.size(); i++)
			if (task.equalsIgnoreCase((String) instrTasks.elementAt(i)))
				return (String) instructions.elementAt(i);
		return null;
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

	@Override
	public String toString() {
		String str = "Manipulator: <" + name + ">; supports";
		if (tasksSupported == null) {
			str += " null";
		} else {
			for (int i = 0; i < tasksSupported.size(); i++) {
				str += " " + (String) tasksSupported.elementAt(i);
			}
		}
		str += "\n";
		if (explanation != null) {
			str += "  Explanation=<" + explanation + ">\n";
		}
		if (instructions != null && instructions.size() > 0) {
			for (int i = 0; i < instructions.size(); i++)
				if (instrTasks == null || instrTasks.size() <= i || instrTasks.elementAt(i) == null) {
					str += "  Instruction=<" + instructions.elementAt(i) + ">\n";
				} else {
					str += "  Instruction(" + instrTasks.elementAt(i) + ")=<" + instructions.elementAt(i) + ">\n";
				}
		}
		str += "/Manipulator\n";
		return str;
	}

	/**
	* Checks if the given task is supported by this manipulator
	*/
	public boolean isTaskSupported(String task) {
		if (task == null || tasksSupported == null)
			return false;
		for (int i = 0; i < tasksSupported.size(); i++)
			if (task.equalsIgnoreCase((String) tasksSupported.elementAt(i)))
				return true;
		return false;
	}
}