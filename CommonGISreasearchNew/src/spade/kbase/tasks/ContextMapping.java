package spade.kbase.tasks;

public class ContextMapping {
	public String taskId = null, from = null, to = null;

	public boolean isValid() {
		return from != null && to != null;
	}
}