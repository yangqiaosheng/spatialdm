package spade.kbase.scenarios;

import java.util.Vector;

/**
* Destription of an analysis tool such as plots etc.
*/
public class Tool {
	static public final int minComplexity = 1, maxComplexity = 3;

	public String function = null, variant = null;
	public int complexity = 1;

	public String name = null, explanation = null;

	/**
	* An instrument may be used for several primitive tasks (see Common.primTasks).
	* There may be an instruction for each possible task.
	*/
	public Vector instructions = null, instrTasks = null;
	public Vector inputs = null, outputs = null;

	public void setFunction(String value) {
		function = value;
	}

	public void setComplexity(int val) {
		if (val < minComplexity) {
			complexity = minComplexity;
		} else if (val > maxComplexity) {
			complexity = maxComplexity;
		} else {
			complexity = val;
		}
	}

	public void setComplexity(String str) {
		if (str != null) {
			try {
				int k = Integer.valueOf(str).intValue();
				setComplexity(k);
			} catch (NumberFormatException nfe) {
			}
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
	* Sets the general instruction, i.e. not related to any primitive task.
	* If the instruction already exists, replaces the existing instruction with
	* the new one.
	*/
	public void setGeneralInstruction(String instr) {
		if (instr == null)
			return;
		if (instructions == null) {
			instructions = new Vector(5, 5);
		}
		if (instructions.size() < 1) {
			instructions.addElement(instr);
			if (instrTasks != null) {
				instrTasks.addElement(null);
			}
			return;
		}
		if (instrTasks == null) {
			instructions.setElementAt(instr, 0);
			return;
		}
		for (int i = 0; i < instrTasks.size(); i++)
			if (instrTasks.elementAt(i) == null) {
				instructions.setElementAt(instr, i);
				return;
			}
		instrTasks.addElement(null);
		instructions.addElement(instr);
	}

	/**
	* Returns instruction associated with the given task (see the list of tasks
	* above)
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

	public void addInput(ToolInput input) {
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

	public ToolInput getToolInput(int idx) {
		if (idx < 0 || idx >= getInputCount())
			return null;
		return (ToolInput) inputs.elementAt(idx);
	}

	public void addOutput(Output output) {
		if (output == null)
			return;
		if (outputs == null) {
			outputs = new Vector(10, 5);
		}
		outputs.addElement(output);
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

	public Tool makeCopy() {
		Tool t = new Tool();
		t.function = function;
		t.variant = variant;
		t.complexity = complexity;
		t.name = name;
		t.explanation = explanation;
		if (instructions != null && instructions.size() > 0) {
			t.instructions = new Vector(instructions.size(), 5);
			for (int i = 0; i < instructions.size(); i++) {
				String str = (String) instructions.elementAt(i);
				if (str != null) {
					t.instructions.addElement(new String(str));
				} else {
					t.instructions.addElement(null);
				}
			}
		}
		if (instrTasks != null && instrTasks.size() > 0) {
			t.instrTasks = new Vector(instrTasks.size(), 5);
			for (int i = 0; i < instrTasks.size(); i++) {
				String str = (String) instrTasks.elementAt(i);
				if (str != null) {
					t.instrTasks.addElement(new String(str));
				} else {
					t.instrTasks.addElement(null);
				}
			}
		}
		if (inputs != null) {
			for (int i = 0; i < getInputCount(); i++) {
				t.addInput(getToolInput(i).makeCopy());
			}
		}
		if (outputs != null) {
			t.outputs = (Vector) outputs.clone();
		}
		/*
		System.out.println("Source tool:----------------------------");
		System.out.println(this.toString());
		System.out.println("Copy of the tool:-----------------------");
		System.out.println(t.toString());
		*/
		return t;
	}

	@Override
	public String toString() {
		String str = "Tool: " + function;
		if (variant != null) {
			str += "(" + variant + ")";
		}
		str += "  Name=<" + name + "> complexity=" + complexity + "\n";
		if (explanation != null) {
			str += "  Explanation=<" + explanation + ">\n";
		}
		if (inputs != null && inputs.size() > 0) {
			for (int i = 0; i < inputs.size(); i++) {
				str += "  " + inputs.elementAt(i).toString() + "\n";
			}
		}
		if (outputs != null && outputs.size() > 0) {
			for (int i = 0; i < outputs.size(); i++) {
				str += "  " + outputs.elementAt(i).toString() + "\n";
			}
		}
		if (instructions != null && instructions.size() > 0) {
			for (int i = 0; i < instructions.size(); i++)
				if (instrTasks == null || instrTasks.size() <= i || instrTasks.elementAt(i) == null) {
					str += "  Instruction=<" + instructions.elementAt(i) + ">\n";
				} else {
					str += "  Instruction(" + instrTasks.elementAt(i) + ")=<" + instructions.elementAt(i) + ">\n";
				}
		}
		str += "/Tool\n";
		return str;
	}
}