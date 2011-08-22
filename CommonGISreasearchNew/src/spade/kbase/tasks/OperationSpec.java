package spade.kbase.tasks;

import java.util.Vector;

public class OperationSpec {
	public String type = null;
	public boolean isDefault = false, resultsMapped = false;

	public String name = null, instruction = null;
	public Vector inputs = null, outputs = null;

	public void addInput(Input input) {
		if (input == null)
			return;
		if (inputs == null) {
			inputs = new Vector(10, 5);
		}
		if (!inputs.contains(input)) {
			inputs.addElement(input);
		}
	}

	public int getNInputs() {
		if (inputs == null)
			return 0;
		return inputs.size();
	}

	public Input getInput(int idx) {
		if (idx < 0 || idx >= getNInputs())
			return null;
		return (Input) inputs.elementAt(idx);
	}

	public void addOutput(Output output) {
		if (output == null)
			return;
		if (outputs == null) {
			outputs = new Vector(10, 5);
		}
		if (!outputs.contains(output)) {
			outputs.addElement(output);
		}
	}

	public int getNOutputs() {
		if (outputs == null)
			return 0;
		return outputs.size();
	}

	public Output getOutput(int idx) {
		if (idx < 0 || idx >= getNOutputs())
			return null;
		return (Output) outputs.elementAt(idx);
	}

	public String getName() {
		return name;
	}

	public void setName(String txt) {
		name = txt;
	}

	public String getInstruction() {
		return instruction;
	}

	public void setInstruction(String txt) {
		instruction = txt;
	}

}