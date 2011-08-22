package data_load.cache;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.SystemUI;
import spade.lib.lang.Language;
import spade.vis.database.DataPortion;
import spade.vis.database.DataSupplier;
import spade.vis.database.LayerData;
import spade.vis.database.SpatialDataItem;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealRectangle;
import data_load.DataAgent;

/**
* A DataBroker connects to a data reader in order to optimise the process of
* data loading by means of caching
*/

public class DataBroker implements DataAgent {
	static ResourceBundle res = Language.getTextResource("data_load.cache.Res");
	public static int OBJECTS_PER_CELL = 2;
	public static String CLEANER = "data_load.cache.DistanceCacheCleaner";
	public Hashtable objects;
	private Index index;
	private CacheCleaner cacheCleaner;
	private int Xdiv = 10;
	private int Ydiv = 10;
	/**
	* The data reader, which actually reads data (the data broker only caches
	* the data)
	*/
	private DataSupplier reader;
	/**
	* The UI used, in particular, for displaying notification messages
	*/
	protected SystemUI ui = null;

	/**
	* Sets the data reader, which actually reads data (the data broker only caches
	* the data)
	*/
	@Override
	public void setDataReader(DataSupplier reader) {
		this.reader = reader;
	}

	/**
	* Sets the system UI that can be used, in particular, for displaying
	* messages about the status of data loading.
	*/
	@Override
	public void setUI(SystemUI ui) {
		this.ui = ui;
	}

	/**
	* Displays the notification message using the system UI. The second argument
	* indicates whether this is an error message.
	*/
	protected void showMessage(String msg, boolean error) {
		if (ui != null) {
			ui.showMessage(msg, error);
		}
		if (msg != null && error) {
			System.err.println("ERROR: " + msg);
		}
	}

	/**
	* Prepares itself to functioning.
	* @param extent   - the total territory extent of all the data in the data
	*                   source (valid for geographical data)
	* @param rowCount - the total number of rows in the database
	* On the basis of these parameters the DataBroker determines
	* the parameters of the cache required.
	*/
	@Override
	public void init(RealRectangle extent, int rowCount) {
		float divCount = (float) Math.sqrt(rowCount / OBJECTS_PER_CELL);
		float ratio = (float) Math.sqrt((extent.rx2 - extent.rx1) / (extent.ry2 - extent.ry1));
		Xdiv = (int) (divCount * ratio);
		Ydiv = (int) (divCount / ratio);
		index = new Index(extent, Xdiv, Ydiv);
		objects = new Hashtable();
		String cleaner = CLEANER;
		try {
			System.out.println("Data Broker constructs a cache cleaner as an instance of " + cleaner);
			cacheCleaner = (CacheCleaner) Class.forName(cleaner).newInstance();
		} catch (Exception e) {
			//following text: "could not create cache cleaner "
			showMessage(res.getString("could_not_create") + e, true);
		}
	}

	/**
	* Constructs and returns a DataPortion containing all DataItems
	* available (calls the corresponding function of the data reader)
	*/
	@Override
	public DataPortion getData() {
		if (reader == null)
			return null;
		return reader.getData();
	}

	/**
	* Constructs and returns a DataPortion containing DataItems fitting in at
	* least one of the specified bounding rectangles (listed in the vector).
	* The elements of the vector are instances of
	* @see spade.vis.geometry.RealRectangle
	* Readresses the query to the data reader and applies the caching mechanism.
	*/
	@Override
	public DataPortion getData(Vector requestedBounds) {
		if (requestedBounds == null || requestedBounds.size() == 0 || index.extent == null)
			return getData();
		RealRectangle ext = index.extent;
		boolean allData = false;
		for (int i = 0; i < requestedBounds.size() && !allData; i++) {
			RealRectangle r = (RealRectangle) requestedBounds.elementAt(i);
			allData = r.rx1 <= ext.rx1 && r.rx2 >= ext.rx2 && r.ry1 <= ext.ry1 && r.ry2 >= ext.ry2;
		}
		Vector bounds = new Vector(5, 5);
		boolean newDataNeeded = index.transform(requestedBounds, bounds);
		if (newDataNeeded) {
			loadData(bounds);
		} else {
			showMessage(res.getString("No_data_to_be_loaded"), false);
		}
		//following text:"Selecting relevant objects in DataBroker..."
		showMessage(res.getString("Selecting_relevant"), false);
		LayerData layerData = new LayerData();
		layerData.setBoundingRectangle(ext.rx1, ext.ry1, ext.rx2, ext.ry2);
		int nobj = 0;
		for (Enumeration e = objects.keys(); e.hasMoreElements();) {
			String id = (String) e.nextElement();
			SpatialDataItem sdi = (SpatialDataItem) objects.get(id);
			if (allData || mustAdd(sdi, requestedBounds)) {
				layerData.addDataItem(sdi);
				cacheCleaner.add(sdi);
				++nobj;
				if (nobj % 50 == 0) {
					//following text:"Selecting relevant objects in DataBroker: "
					showMessage(res.getString("Selecting_relevant1") +
					//following text:" objects selected"
							nobj + res.getString("objects_selected"), false);
				}
			}
		}
		showMessage(null, false);
		layerData.setHasAllData(allData);
		return layerData;
	}

	private void loadData(Vector bounds) {
		if (reader == null)
			return;
		DataPortion dp = null;
		for (int i = 0; i < 2 && dp == null; i++) {
			try {
				dp = reader.getData(bounds);
			} catch (Exception e) { //e.g. out of memory
				//following text: "Data Broker failed to load data; clearing the cache..."
				System.out.println(e);
				showMessage(res.getString("Data_Broker_failed_to"), true);
				clearCache(bounds);
			}
		}
		if (dp == null) {
			//following text:"Data Broker failed to load data"
			showMessage(res.getString("Data_Broker_failed_to1"), true);
		} else {
			for (int i = 0; i < dp.getDataItemCount(); i++) {
				String id = dp.getDataItemId(i);
				SpatialDataItem sdi = (SpatialDataItem) dp.getDataItem(i);
				objects.put(id, sdi);
				cacheCleaner.add(sdi);
			}
		}
	}

	private void clearCache(Vector requestedBounds) {
		//following text:"Cache clearing"
		showMessage(res.getString("Cache_clearing"), false);
		cacheCleaner.prepare(requestedBounds);
		if (index != null) {
			index.debug(index.loaded);
		}
		String id;
		while ((id = cacheCleaner.nextObjectId()) != null) {
			clearCells(id);
			if (objects != null) {
				objects.remove(id);
			}
			cacheCleaner.remove(id);
		}
		System.gc();
		//following text:"Cache cleaning is finished"
		showMessage(res.getString("Cache_cleaning_is"), false);
		System.out.println("Cache cleaning is finished");
		if (objects != null) {
			System.out.println("Objects in cache: " + objects.size());
		}
		System.out.println("Free memory: " + Runtime.getRuntime().freeMemory());
		if (index != null) {
			index.debug(index.loaded);
		}
	}

	private boolean mustAdd(SpatialDataItem sdi, Vector bounds) {
		if (bounds == null)
			return true;
		Geometry g = sdi.getGeometry();
		for (int i = 0; i < bounds.size(); i++) {
			RealRectangle r = (RealRectangle) bounds.elementAt(i);
			if (g.fitsInRectangle(r.rx1, r.ry1, r.rx2, r.ry2))
				return true;
		}
		return false;
	}

	public void clearCells(String id) {
		int startXCell, startYCell, endXCell, endYCell;
		SpatialDataItem sdi = (SpatialDataItem) objects.get(id);
		Geometry g = sdi.getGeometry();
		float[] bounds = g.getBoundRect();
		startXCell = index.getXCell(bounds[0]);
		endXCell = index.getXCell(bounds[2]);
		startYCell = index.getYCell(bounds[1]);
		endYCell = index.getYCell(bounds[3]);
		index.markCells(index.loaded, false, startXCell, startYCell, endXCell, endYCell);
	}

	/**
	* When no more data from the DataSupplier are needed, this method is
	* called. Here the DataSupplier can clear its internal structures.
	* The DataBroker should clear all its cache etc.
	*/
	@Override
	public void clearAll() {
		if (reader != null) {
			reader.clearAll();
		}
		reader = null;
		if (objects != null) {
			objects.clear();
		}
		objects = null;
		index = null;
		System.out.println("DataBroker cleaned all");
	}

}
