package data_load.read_gml;

import java.util.Vector;

import spade.analysis.system.DataReader;
import spade.vis.database.DataPortion;
import spade.vis.database.DataSupplier;
import spade.vis.database.LayerData;

/**
* A passive data supplier does not load any data itself but accepts the data
* from some reader (e.g. from a MultiLayerReader) and passes it to a certain
* data consumer (e.g. to a map layer). This intermediary object is needed
* because a reader itself cannot be a supplier of multiple layers.
*/
public class PassiveDataSupplier implements DataSupplier {
	/**
	* The data reader that actually loads the data
	*/
	protected DataReader reader = null;
	/**
	* The spatial data received from the actual reader
	*/
	protected LayerData lData = null;

	/**
	* Sets a reference to the data reader that actually loads the data
	*/
	public void setDataReader(DataReader reader) {
		this.reader = reader;
	}

	/**
	* Sets the spatial data loaded by the actual reader
	*/
	public void setLayerData(LayerData data) {
		lData = data;
	}

	/**
	* Returns the spatial data loaded by the actual reader. If the data are not
	* yet loaded, calls the method loadData of its reader. In this method, the
	* reader must pass the data loaded to this data supplier, so that the
	* internal variable lData becomes not null.
	*/
	@Override
	public DataPortion getData() {
		if (lData != null)
			return lData;
		if (reader == null)
			return null;
		if (reader.loadData(false))
			return lData;
		return null;
	}

	/**
	* Currently does not pay any attention to the bounds
	*/
	@Override
	public DataPortion getData(Vector bounds) {
		return getData();
	}

	@Override
	public void clearAll() {
		lData = null;
	}
}