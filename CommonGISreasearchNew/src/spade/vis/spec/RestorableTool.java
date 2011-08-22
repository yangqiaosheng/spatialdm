package spade.vis.spec;

/**
* Used for restoring states of tools that have previously saved their states
* as certain specifications.
*/
public interface RestorableTool {
	/**
	* Setups the tool according to the given specification, if appropriate.
	*/
	public void applySpecification(Object spec);
}
