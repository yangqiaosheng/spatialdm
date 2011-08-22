package data_update;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Vector;

import spade.vis.database.DataRecord;
import spade.vis.database.SpatialEntity;

/**
* Synchronizes multiple attempts to add data (spatial entities) to the same
* file.
*/
public class DataUpdateManager implements PropertyChangeListener {
	/**
	* The name of the file (ASCII format with delimiters) for storing thematic
	* data about spatial entities and, for point objects, X- and Y- coordinates.
	*/
	protected String tableFileName = null;
	/**
	* The delimiter to be used in the table file (by default comma)
	*/
	protected String delimiter = ",";
	/**
	* The name of the geometry file (OVL). Mandatory for line and area entities.
	*/
	protected String geoFileName = null;
	/**
	* The spatial entities to be stored. The vector may also contain DataRecords
	* that are used for updating thematic data attached to existing objects.
	*/
	protected Vector entities = null;
	/**
	* The thread used for storing the data
	*/
	protected DataUpdateThread dupThread = null;

	/**
	* Sets the name of the file (ASCII format with delimiters) for storing thematic
	* data about spatial entities and, for point objects, X- and Y- coordinates.
	*/
	public void setTableFileName(String name) {
		tableFileName = name;
	}

	/**
	* Sets the name of the geometry file (OVL). Mandatory for line and area entities.
	*/
	public void setGeoFileName(String name) {
		geoFileName = name;
	}

	/**
	* Sets the delimiter to be used in the table file
	*/
	public void setDelimiter(String delim) {
		if (delim == null || delim.length() < 1)
			return;
		delimiter = delim.substring(0, 1);
	}

	/**
	* Checks if the given path is the same as the path to the table this
	* DataUpdateManager deals with
	*/
	public boolean doesWriteToFile(String path) {
		if (path == null || tableFileName == null)
			return false;
		if (path.equals(tableFileName))
			return true;
		String str1 = path.replace('\\', '/'), str2 = tableFileName.replace('\\', '/');
		if (str1.equals(str2))
			return true;
		File file1 = new File(str1), file2 = new File(str2);
		try {
			str1 = file1.getCanonicalPath();
			str2 = file2.getCanonicalPath();
			if (str1.equals(str2))
				return true;
		} catch (Exception e) {
		}
		return false;
	}

	/**
	* Adds a new spatial entity to be stored
	*/
	public void addEntity(Object ent) {
		if (ent == null)
			return;
		if (ent instanceof SpatialEntity) {
			SpatialEntity spe = (SpatialEntity) ent;
			if (spe.getGeometry() == null)
				return;
		} else if (ent instanceof DataRecord) {
			DataRecord rec = (DataRecord) ent;
			if (rec.getId() == null)
				return;
		} else
			return; //illegal object type
		synchronized (this) {
			if (entities == null) {
				entities = new Vector(20, 10);
			}
			entities.addElement(ent);
		}
	}

	/**
	* If no data are currently being stored (i.e. the data storing thread is
	* not running or null), starts the thread for storing the spatial
	* entities accumulated in the vector of entities. Otherwise, postpones this
	* operation until the thread finishes.
	*/
	public void storeData() {
		if (entities == null || entities.size() < 1 || tableFileName == null)
			return;
		synchronized (this) {
			if (dupThread != null && dupThread.isRunning())
				return;
			Vector entCopy = (Vector) entities.clone();
			entities.removeAllElements();
			dupThread = new DataUpdateThread(entCopy, tableFileName, delimiter, geoFileName, this);
			dupThread.start();
		}
	}

	/**
	* Through this method the DataUpdateThread notifies when it has finished
	* writing data to the file(s). If at this time there are some entities
	* to store, starts again the thread to store the new entities.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource().equals(dupThread) && pce.getPropertyName().equals("finished")) {
			dupThread = null;
		}
		storeData();
	}
}
