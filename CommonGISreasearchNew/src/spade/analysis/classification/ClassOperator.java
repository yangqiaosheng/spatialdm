package spade.analysis.classification;

import spade.analysis.system.Supervisor;

/**
* The interface to be fulfilled by components intended to do something with
* classes, for example, broadcast them or save in a table.
*/
public interface ClassOperator {
	/**
	* Constructs a ClassOperator. Needs 1) a reference to the classifier which
	* produces the classes; 2) a reference to the supervisor that can be used for
	* event propagation.
	* Returns true if successfully constructed.
	*/
	public boolean construct(Classifier classifier, Supervisor supervisor);
}