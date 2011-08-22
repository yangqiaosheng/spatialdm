package spade.vis.database;

/**
* The interface to be implemented by classes performing any processing and
* transformation of table data after they have been loaded. For example,
* there may be a processor that builds temporal attributes using semantic
* information about time references contained in data.
*/
public interface TableProcessor {
	/**
	* Performs, if appropriate, some transformation of the given table.
	* May use semantic information provided by the SemanticsManager of this
	* table.
	*/
	public void processTable(DataTable table);
}