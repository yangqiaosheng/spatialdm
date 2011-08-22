package connection.zeno;

import java.util.Vector;

/**
* The interface is introduced to avoid cyclic references between the classes
* DataPipesManager and DataPipeThread.
*/
public interface DataReceiver {
	/**
	* In this method the data receiver does what is necessary when new data
	* arrive. The data are sent to the receiver as a vector of entries. Each entry,
	* in its turn, is a vector of pairs tag+value.
	*/
	public void newDataReceived(String dataURL, Vector entriesGot);
}