package spade.kbase.tasks;

import java.util.Vector;

public class AltInput extends Input {
	/**
	* Several alternative variants of input of an operation
	*/
	public Vector inputs = null;

	public void addAlternative(Input input) {
		if (input == null)
			return;
		if (inputs == null) {
			inputs = new Vector(10, 5);
		}
		if (!inputs.contains(input)) {
			inputs.addElement(input);
		}
	}

	public int getNAlternatives() {
		if (inputs != null && inputs.size() > 0)
			return inputs.size();
		if (arguments == null)
			return 0;
		return arguments.size();
	}

	public Input getAlternative(int idx) {
		if (idx < 0 || idx >= getNAlternatives())
			return null;
		if (inputs != null && idx < inputs.size())
			return (Input) inputs.elementAt(idx);
		if (arguments == null)
			return null;
		if (arguments.size() == 1)
			return this;
		Input input = new Input();
		input.arguments = new Vector(1, 1);
		input.arguments.addElement(this.arguments.elementAt(idx));
		input.isOptional = this.isOptional;
		return input;
	}
}