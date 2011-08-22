package data_update;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Vector;

/**
* The thread to be used the DataUpdateServlet for storing spatial entities on
* the server. Can notify a specified PropertyChangeListener when the data
* storing process is finished.
*/
public class DataUpdateThread extends Thread {
	/**
	* The vector of SpatialEntities to be stored
	*/
	protected Vector data = null;
	/**
	* The name of the table file (ASCII format with delimiters) where to store
	* thematic data and, for point entities, X- and Y- coordinates.
	*/
	protected String tableFileName = null;
	/**
	* The delimiter to be used in the table file
	*/
	protected String delimiter = null;
	/**
	* The name of the geometry file (OVL). Mandatory for line and area entities.
	*/
	protected String geoFileName = null;
	/**
	* The PropertyChangeListener is notified about the data storing process being
	* finished.
	*/
	protected PropertyChangeListener listener = null;
	/**
	* Indicates whether the thread is currently in the process of saving data
	*/
	private boolean running = false;
	/**
	* The error message received from the DataUpdater in the case when errors
	* occurred during the data storing process.
	*/
	protected String err = null;

	/**
	* Constructs the thread. Stores the arguments in the internal variables.
	*/
	public DataUpdateThread(Vector data, String tableFileName, String delimiter, String geoFileName, PropertyChangeListener listener) {
		this.data = data;
		this.tableFileName = tableFileName;
		this.delimiter = delimiter;
		this.geoFileName = geoFileName;
		this.listener = listener;
	}

	/**
	* In this method the thread creates an instance of the class DataUpdater
	* and uses ut for storing the data.
	*/
	@Override
	public void run() {
		synchronized (this) {
			running = true;
		}
		DataUpdater dup = new DataUpdater();
		if (!dup.storeSpatialData(data, tableFileName, delimiter, geoFileName)) {
			err = dup.getErrorMessage();
			Calendar c = Calendar.getInstance();
			String date_time = c.get(Calendar.DAY_OF_MONTH) + "/" + (1 + c.get(Calendar.MONTH)) + "/" + c.get(Calendar.YEAR) + " at " + c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE) + ":" + c.get(Calendar.SECOND);
			synchronized (this) {
				FileWriter writer = null;
				try {
					writer = new FileWriter(System.getProperty("java.io.tmpdir") + "DataUpdate_errors.log", true);
					writer.write(date_time + "\nerror while storing data in " + tableFileName + ":\n" + err + "\n\n");
				} catch (IOException ioe) {
				}
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException ioe) {
					}
				}
				;
			}
		}
		synchronized (this) {
			running = false;
		}
		if (listener != null) {
			PropertyChangeEvent pce = new PropertyChangeEvent(this, "finished", null, null);
			listener.propertyChange(pce);
		}
	}

	/**
	* Replies if the thread is currently in the process of saving data
	*/
	public boolean isRunning() {
		return running;
	}
}