package data_load.readers;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.GeoDataReader;
import spade.lib.lang.Language;
import spade.lib.util.CopyFile;
import spade.lib.util.Formats;
import spade.vis.database.DataPortion;
import spade.vis.database.DataSupplier;
import spade.vis.database.LayerData;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoLayer;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.spec.DataSourceSpec;

/**
* Gets SpatialEntities from an OVL file
*/
public class OVLReader extends DataStreamReader implements GeoDataReader, DataSupplier {
	static ResourceBundle res = Language.getTextResource("data_load.readers.Res");
	protected boolean ovrFormat = false;
	protected static byte b4[] = new byte[4];
	protected static byte b12[] = new byte[12];
	/**
	* The spatial data loaded
	*/
	protected LayerData data = null;

	public void setOVRFormat(boolean value) {
		ovrFormat = value;
	}

	protected float readFloat(DataInputStream DIS) throws IOException {
		if (ovrFormat) {
			DIS.readFully(b4);
			return Formats.getFloat(b4);
		} else
			return DIS.readFloat();
	}

	/**
	* Loads the data. When allowed (the argument mayAskUser is true) and necessary,
	* starts a dialog or a wizard for the user
	* to specify the data source, constraints, etc. Returns true if the data
	* have been successfully loaded.
	*/
	@Override
	public boolean loadData(boolean mayAskUser) {
		if (dataError)
			return false;
		if (dataReadingInProgress) {
			waitDataReadingFinish();
			return !dataError;
		}
		setDataReadingInProgress(true);
		if (spec == null || spec.source == null)
			if (mayAskUser) {
				//following text:"Select the file with geographical data"
				String path = browseForFile(res.getString("Select_the_file_with3"), "*.ovl");
				if (path == null) {
					setDataReadingInProgress(false);
					return false;
				}
				if (spec == null) {
					spec = new DataSourceSpec();
				}
				spec.source = path;
			} else {
				//following text:"The data source for layer is not specified!"
				showMessage(res.getString("The_data_source_for"), true);
				setDataReadingInProgress(false);
				return false;
			}
		if (spec.name == null) {
			spec.name = CopyFile.getName(spec.source);
		}
		//following text:"Start reading data from "
		showMessage(res.getString("Start_reading_data") + spec.source, false);
		closeStream();
		openStream(); //to be at the beginning
		if (stream == null) {
			dataError = true;
			setDataReadingInProgress(false);
			return false;
		}
		data = readSpecific();
		closeStream();
		dataError = data == null;
		setDataReadingInProgress(false);
		return !dataError;
	}

	/**
	* Assuming that the data stream is already opened, tries to read from it
	* spatial data in OVL format
	*/
	protected LayerData readSpecific() {
		if (stream == null)
			return null;
		DataInputStream reader = new DataInputStream(stream);
		//maybe, this is the old OVR format?
		if (spec.source != null) {
			String ext = CopyFile.getExtension(spec.source);
			if (ext != null && ext.equalsIgnoreCase("OVR")) {
				setOVRFormat(true);
			}
		}
		//read the caption: the bounding rectangle of all the entities
		//following text:": reading the bounding rectangle..."
		showMessage(spec.source + res.getString("_reading_the_bounding"), false);
		float x1, y1, x2, y2;
		try {
			x1 = readFloat(reader);
			y1 = readFloat(reader);
			x2 = readFloat(reader);
			y2 = readFloat(reader);
		} catch (IOException ioe) {
			//following text:"Exception reading geodata: "
			showMessage(res.getString("Exception_reading3") + ioe, true);
			return null;
		}
		LayerData data = new LayerData();
		data.setBoundingRectangle(x1, y1, x2, y2);
		//following text:": reading the coordinates..."
		showMessage(spec.source + res.getString("_reading_the"), false);
		int k = 0;
		while (true) {
			int np = 0;
			try {
				np = Math.round(readFloat(reader));
			} catch (IOException eofe) {
				break;
			}
			try {
				reader.readFully(b12);
				String id = new String(b12).trim();
				Geometry g = null;
				if (np == 1) {
					RealPoint rp = new RealPoint();
					rp.x = readFloat(reader);
					rp.y = readFloat(reader);
					g = rp;
				} else {
					RealPolyline line = new RealPolyline();
					line.p = new RealPoint[np];
					for (int i = 0; i < np; i++) {
						line.p[i] = new RealPoint();
						line.p[i].x = readFloat(reader);
						line.p[i].y = readFloat(reader);
					}
					g = line;
				}
				SpatialEntity spe = new SpatialEntity(id);
				spe.setGeometry(g);
				if (spec.idsImportant && (spec.mayHaveMultiParts || spec.mayHaveHoles)) {
					data.addDataItem(spe);
				} else {
					data.addItemSimple(spe);
				}
				++k;
				if (k % 50 == 0) {
					//following text:" contours read"
					showMessage(String.valueOf(k) + res.getString("contours_read"), false);
				}
			} catch (IOException ioe) {
				//following text:"Exception reading geodata: "
				//following text:" objects got"
				showMessage(res.getString("Exception_reading3") + ioe, true);
				System.err.println(k + res.getString("objects_got"));
				return null;
			}
		}
		if (data.getDataItemCount() > 0) {
			//following text: " spatial entities loaded"
			showMessage(spec.source + ": " + String.valueOf(data.getDataItemCount()) + res.getString("spatial_entities"), false);
			data.setHasAllData(true);
		} else {
			//following text:": no spatial entities loaded!"
			showMessage(spec.source + res.getString("_no_spatial_entities"), true);
			return null;
		}
		return data;
	}

	/**
	* Returns the map layer constructed from the geographical data loaded (if any).
	* If the data have not been loaded yet, the reader constructs a layer without
	* objects and sets itself as the  data supplier of the layer. Then the data
	* will be loaded when the layer is first darwn.
	*/
	@Override
	public DGeoLayer getMapLayer() {
		DGeoLayer layer = new DGeoLayer();
		layer.setDataSource(spec);
		if (spec.id != null) {
			layer.setContainerIdentifier(spec.id);
		}
		if (spec.name != null) {
			layer.setName(spec.name);
		} else if (spec.source != null) {
			layer.setName(CopyFile.getName(spec.source));
		}
		if (data != null) {
			layer.receiveSpatialData(data);
		} else {
			layer.setDataSupplier(this);
		}
		return layer;
	}

//----------------- DataSupplier interface -----------------------------------
	/**
	* Returns the SpatialDataPortion containing all DataItems available
	*/
	@Override
	public DataPortion getData() {
		if (data != null)
			return data;
		if (dataError)
			return null;
		if (loadData(false))
			return data;
		return null;
	}

	/**
	* Constructs and returns a DataPortion containing DataItems fitting in at
	* least one of the specified bounding rectangles (listed in the vector).
	* The elements of the vector are instances of
	* @see spade.vis.geometry.RealRectangle
	* Readers from files do not filter data according to any query,
	* therefore the method getData() without arguments is called
	*/
	@Override
	public DataPortion getData(Vector bounds) {
		return getData();
	}

	/**
	* When no more data from the DataSupplier are needed, this method is
	* called. Here the DataSupplier can clear its internal structures
	*/
	@Override
	public void clearAll() {
		data = null;
	}
}
