package connection.zeno;

import java.util.Vector;

/**
* This thread is used when some new entries have been received from a datapipe.
* It is used to start a dialog from the DataPipesManager that proposes
* the user to georeference the new entries. The thread is needed because the
* Java plugin hangs when some dialog is started directly from the method
* windowActivated of the WindowListener interface.
* The thread does not directly refer to the DataPipesManager but addresses it
* through the DataReceiver interface. This is done to avoid cyclic references
* between the classes DataPipesManager and DataPipeThread.
*/
public class DataPipeThread extends Thread {
	/**
	* The DataReceiver to be activated.
	*/
	protected DataReceiver dRec = null;
	/**
	* The new entries to be passed to the data receiver
	*/
	protected Vector entries = null;
	/**
	* The URL of the data source
	*/
	protected String dataURL = null;

	/**
	* Constructs the thread with passing it the DataReceiver to be activated
	* and the new entries to be passed to the data receiver.
	*/
	public DataPipeThread(DataReceiver dReceiver, String dataURL, Vector newEntries) {
		super();
		dRec = dReceiver;
		this.dataURL = dataURL;
		entries = newEntries;
	}

	/**
	* Makes the DataReceiver do necessary actions on receiving the new data.
	* In particular, the DataPipesManager displays a dialog that proposes the user
	* to georeference the new entries.
	*/
	@Override
	public void run() {
		if (dRec != null) {
			dRec.newDataReceived(dataURL, entries);
		}
	}
}
