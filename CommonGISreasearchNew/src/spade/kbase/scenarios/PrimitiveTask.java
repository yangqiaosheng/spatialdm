package spade.kbase.scenarios;

/**
* A primitive task, an internal representation of the "PrimitiveTask"
* element of the task knowledge base.
*/
public class PrimitiveTask {
	protected String id = null;
	public String name = null;

	public PrimitiveTask(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
