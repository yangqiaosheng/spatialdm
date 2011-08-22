package guide_tools.tutorial;

import java.io.PrintStream;
import java.util.Vector;

public class Advise {
	/**
	* An advise consists of one or more instructions. An instruction may be
	* supplied with a reference to a help topic (identifier of the help
	* topic). The vector instructions contains these instructions (strings).
	*/
	public Vector instructions = null;
	/**
	* Identifiers of the help topics corresponding to the instructions.
	* There may be no help topic for the last instruction. Hence, the
	* vector helpTopics may be shorter than the vector instructions.
	* If there is only one instruction, the vector helpTopics may be null.
	*/
	public Vector helpTopics = null;

	/**
	* Adds the string to the text of the last instruction, if there was yet no
	* help topic for this instruction. Otherwise, adds it as a new instruction
	* to the list of instructions.
	*/
	public void addInstructionLine(String str) {
		if (str == null)
			return;
		if (instructions == null) {
			instructions = new Vector(3, 3);
		}
		int nInstr = getInstructionCount();
		if (nInstr < 1 || getHelpTopicCount() >= nInstr) {
			instructions.addElement(new String(str));
		} else {
			String txt = getInstruction(nInstr - 1) + " " + str;
			instructions.setElementAt(txt, nInstr - 1);
		}
	}

	public int getInstructionCount() {
		if (instructions == null)
			return 0;
		return instructions.size();
	}

	public String getInstruction(int idx) {
		if (idx < 0 || idx >= getInstructionCount())
			return null;
		return (String) instructions.elementAt(idx);
	}

	public void addHelpTopicId(String id) {
		if (id == null)
			return;
		if (helpTopics == null) {
			helpTopics = new Vector(3, 3);
		}
		helpTopics.addElement(id);
	}

	public int getHelpTopicCount() {
		if (helpTopics == null)
			return 0;
		return helpTopics.size();
	}

	public String getHelpTopicId(int idx) {
		if (idx < 0 || idx >= getHelpTopicCount())
			return null;
		return (String) helpTopics.elementAt(idx);
	}

	public void printToStream(PrintStream ps) {
		if (getInstructionCount() < 1)
			return;
		if (getInstructionCount() == 1) {
			ps.println("Recommendation:");
		} else {
			ps.println("Recommendations:");
		}
		for (int i = 0; i < getInstructionCount(); i++) {
			if (getInstructionCount() > 1) {
				ps.print((i + 1) + ") ");
			}
			ps.println(getInstruction(i));
			if (i < getHelpTopicCount()) {
				ps.println("help [" + getHelpTopicId(i) + "]");
			}
		}
	}
}