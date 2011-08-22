package data_load.readers;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.AttrDataReader;
import spade.analysis.system.CompositeDataReader;
import spade.analysis.system.DataReader;
import spade.analysis.system.DataReaderFactory;
import spade.analysis.system.GeoDataReader;
import spade.lib.lang.Language;
import spade.lib.util.CopyFile;
import spade.lib.util.Formats;
import spade.vis.database.DataPortion;
import spade.vis.database.DataSupplier;
import spade.vis.database.DataTable;
import spade.vis.database.LayerData;
import spade.vis.database.SpatialEntity;
import spade.vis.database.ThematicDataItem;
import spade.vis.dmap.DGeoLayer;
import spade.vis.geometry.MultiGeometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolygon;
import spade.vis.geometry.RealPolyline;
import spade.vis.spec.DataSourceSpec;

/**
* Gets SpatialEntities from an SHP file. Besides, a ShapeReader can load
* thematic data from a DBF or CSV file associated with the SHP file.
*/
public class ShapeReader extends DataStreamReader implements GeoDataReader, AttrDataReader, CompositeDataReader, DataSupplier {
	static ResourceBundle res = Language.getTextResource("data_load.readers.Res");
	private final static int MAIN_HEADER_SIZE = 50;
	/**
	* The spatial data loaded
	*/
	protected LayerData data = null;
	/**
	* The attribute data loaded
	*/
	protected DataTable table = null;
	/**
	* The DataReaderFactory is used for getting an appropriate reader for
	* the file with thematic data (usually DBF)
	*/
	protected DataReaderFactory readerFactory = null;
	/**
	* The reader of the object information attached to the shape file
	*/
	protected DataReader tableReader = null;
	/**
	* Indicates if the library with Java2D functions is available
	*/
	protected boolean hasJava2D = false;
	/**
	* Indicates whether it makes sense to search for polygons with holes
	*/
	protected boolean findHoles = true;

	/**
	* Sets the DataReaderFactory that can produce an appropriate reader for
	* the additional data source
	*/
	@Override
	public void setDataReaderFactory(DataReaderFactory factory) {
		readerFactory = factory;
	}

	/**
	* Tries to constructs the table in which the information about the geo objects
	* (identifiers, names, attribute data) will be stored. Such information is
	* often contained in a DBF file accompanying the shape file (it usually has
	* the same name as the shape file). If there is no such file or no appropriate
	* reader, the internal variable table remains null.
	*/
	protected void constructTable() {
		if (table != null)
			return;
		if (readerFactory == null)
			return; //no reader for the table is available
		if (spec == null)
			return; //no data source specification available
		if (spec.objDescrSource == null) {
			//generate a default name of the file with thematic data about the objects
			String str = spec.source;
			if (spec.source.endsWith(".shp") || spec.source.endsWith(".SHP")) {
				str = spec.source.substring(0, spec.source.length() - 4);
			} else if (spec.source.endsWith(".shape") || spec.source.endsWith(".SHAPE")) {
				str = spec.source.substring(0, spec.source.length() - 6);
			}
			if (CopyFile.checkExistence(str + ".dbf")) {
				spec.objDescrSource = str + ".dbf";
			} else if (CopyFile.checkExistence(str + ".DBF")) {
				spec.objDescrSource = str + ".DBF";
			}
		}
		if (spec.objDescrSource == null)
			return; //no file with object information exists
		String format = CopyFile.getExtension(spec.objDescrSource);
		tableReader = readerFactory.getReaderOfFormat(format);
		if (tableReader == null || !(tableReader instanceof AttrDataReader)) {
			//following text:"No reader found for the file "
			showMessage(res.getString("No_reader_found_for1") + spec.objDescrSource, true);
			return;
		}
		DataSourceSpec dss = (DataSourceSpec) spec.clone();
		dss.source = spec.objDescrSource;
		dss.objDescrSource = null;
		tableReader.setDataSource(dss);
		tableReader.setUI(ui);
		AttrDataReader attrReader = (AttrDataReader) tableReader;
		table = attrReader.getAttrData();
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
				String path = browseForFile(res.getString("Select_the_file_with3"), "*.shp");
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
		if (table == null) {
			constructTable();
		}
		if (table != null && !table.hasData() && tableReader != null && tableReader.loadData(mayAskUser))
			if (mayAskUser) {
				//the user might have provided additional
				//information about the table describing the objects
				DataSourceSpec dss = (DataSourceSpec) table.getDataSource();
				spec.delimiter = dss.delimiter;
				spec.idFieldN = dss.idFieldN;
				spec.idFieldName = dss.idFieldName;
				spec.nameFieldN = dss.nameFieldN;
				spec.nameFieldName = dss.nameFieldName;
				spec.nRowWithFieldNames = dss.nRowWithFieldNames;
				spec.nRowWithFieldTypes = dss.nRowWithFieldTypes;
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
	* spatial data in SHAPE format
	*/
	protected LayerData readSpecific() {
		if (stream == null)
			return null;
		/**/
		try {
			hasJava2D = java2d.Drawing2D.isJava2D;
		} catch (Throwable e) {
		}
		;
		/**/
		findHoles = hasJava2D && spec.mayHaveHoles;
		System.out.println("findHoles:" + findHoles);
		//System.out.println("^^^^^^findHoles="+findHoles);
		DataInputStream reader = new DataInputStream(stream);
		//read the caption: the bounding rectangle of all the entities
		//following text:"Reading the bounding rectangle..."
		showMessage(res.getString("Reading_the_bounding"), false);
		try {
			int type = 0;
			reader.skipBytes(24);
			int size = reader.readInt();
			System.err.println("size=" + size);
			reader.skipBytes(4);
			type = Formats.reverseInt(reader.readInt());
			System.err.println("type=" + type);
			float xmin = Formats.longToFloat(reader.readLong());
			float ymin = Formats.longToFloat(reader.readLong());
			float xmax = Formats.longToFloat(reader.readLong());
			float ymax = Formats.longToFloat(reader.readLong());
			reader.skipBytes(32); //end of header
			LayerData data = new LayerData();
			data.setBoundingRectangle(xmin, ymin, xmax, ymax);
			//following text:"Reading the coordinates..."
			showMessage(res.getString("Reading_the"), false);
			switch (type) {
			case 1:
				loadPoints(reader, size, data);
				break;
			case 3:
				loadPolyLines(reader, size, data);
				break;
			case 5:
				loadPolygons(reader, size, data);
				//loadPolyLines(reader, size, data);
				break;
			}
			if (data != null) {
				data.setHasAllData(true);
				//if (tblN>=0 && geoLayer!=null) tHandler.setLink(geoLayer,tblN);
			}
			return data;
		} catch (Exception e) {
			System.err.println("Error while reading data from shapefile: " + e);
			return null;
		}
	}

	public void loadPoints(DataInputStream reader, int size, LayerData data) throws IOException {
		int pos = MAIN_HEADER_SIZE;
		int recnum = 0;
		while (pos < size) {
			reader.skipBytes(4);
//        recnum = reader.readInt(); //record number
			int length = reader.readInt();
			int shapetype = Formats.reverseInt(reader.readInt());
			if (0 != shapetype) {
				RealPoint rp = new RealPoint();
				rp.x = Formats.longToFloat(reader.readLong());
				rp.y = Formats.longToFloat(reader.readLong());
				String id = null;
				if (table != null) {
					id = table.getDataItemId(recnum);
				}
				if (id == null) {
					id = String.valueOf(recnum + 1);
				}
				SpatialEntity spe = new SpatialEntity(id);
				spe.setGeometry(rp);
				if (table != null) {
					if (table.getDataItem(recnum) instanceof ThematicDataItem) {
						spe.setThematicData((ThematicDataItem) table.getDataItem(recnum));
					}
					if (table.getDataItemName(recnum) != null) {
						spe.setName(table.getDataItemName(recnum));
					}
				}
				if (spec.idsImportant && spec.mayHaveMultiParts) {
					data.addDataItem(spe);
				} else {
					data.addItemSimple(spe);
				}
			}
			pos += 4 + length;
			++recnum;
			if (recnum % 50 == 0) {
				//following text:" shapes read"
				showMessage(String.valueOf(recnum) + res.getString("shapes_read"), false);
			}
		}
	}

	public void loadPolyLines(DataInputStream reader, int size, LayerData data) throws IOException {
		int pos = MAIN_HEADER_SIZE;
		int recnum = 0;
		while (pos < size) {
			reader.skipBytes(4);
//       recnum = reader.readInt();
			int length = reader.readInt();
			int shapetype = Formats.reverseInt(reader.readInt());

			if (0 != shapetype) {
				reader.skipBytes(32); //skipping bounds
				int numParts = Formats.reverseInt(reader.readInt());
				int numPoints = Formats.reverseInt(reader.readInt());
				int[] Parts = new int[numParts + 1]; //read numParts
				for (int i = 0; i < numParts; i++) {
					Parts[i] = Formats.reverseInt(reader.readInt()); //read parts
				}
				Parts[numParts] = numPoints;
				if (numParts > 1) {
					MultiGeometry mg = new MultiGeometry();
					for (int i = 0; i < numParts; i++) {
						int pointsInPart = Parts[i + 1] - Parts[i];
						RealPolyline line = new RealPolyline();
						line.p = new RealPoint[pointsInPart];
						for (int j = 0; j < pointsInPart; j++) {
							line.p[j] = new RealPoint();
							line.p[j].x = Formats.longToFloat(reader.readLong());
							line.p[j].y = Formats.longToFloat(reader.readLong());
						} //for (int j = Parts[i]; j < Parts[i + 1]; j++)
						mg.addPart(line);
					} // for (int i = 0; i < numParts; i++)
					String id = null;
					if (table != null) {
						id = table.getDataItemId(recnum);
					}
					if (id == null) {
						id = String.valueOf(recnum + 1);
					}
					SpatialEntity spe = new SpatialEntity(id);
					spe.setGeometry(mg);
					if (table != null) {
						if (table.getDataItem(recnum) instanceof ThematicDataItem) {
							spe.setThematicData((ThematicDataItem) table.getDataItem(recnum));
						}
						if (table.getDataItemName(recnum) != null) {
							spe.setName(table.getDataItemName(recnum));
						}
					}
					data.addItemSimple(spe);
					//data.addDataItem(spe);

					//}else{

					//}
				} else { //if (numParts > 1)
					RealPolyline line = new RealPolyline();
					line.p = new RealPoint[numPoints];
					for (int j = 0; j < numPoints; j++) {
						line.p[j] = new RealPoint();
						line.p[j].x = Formats.longToFloat(reader.readLong());
						line.p[j].y = Formats.longToFloat(reader.readLong());
					} //for (int j = 0; j < numPoints; j++)
					String id = null;
					if (table != null) {
						id = table.getDataItemId(recnum);
					}
					if (id == null) {
						id = String.valueOf(recnum + 1);
					}
					SpatialEntity spe = new SpatialEntity(id);
					spe.setGeometry(line);
					if (table != null) {
						if (table.getDataItem(recnum) instanceof ThematicDataItem) {
							spe.setThematicData((ThematicDataItem) table.getDataItem(recnum));
						}
						if (table.getDataItemName(recnum) != null) {
							spe.setName(table.getDataItemName(recnum));
						}
					}
					if (spec.idsImportant && (spec.mayHaveMultiParts || spec.mayHaveHoles)) {
						data.addDataItem(spe);
					} else {
						data.addItemSimple(spe);
					}
				} //if (numParts > 1)
			} // if (0 != shapetype)
			pos += 4 + length;
			++recnum;
			if (recnum % 50 == 0) {
				//following text:" shapes read
				showMessage(String.valueOf(recnum) + res.getString("shapes_read"), false);
			}
		} //while (pos < size)
	}

	public void loadPolygons(DataInputStream reader, int size, LayerData data) throws IOException {
		int pos = MAIN_HEADER_SIZE;
		int recnum = 0;
		while (pos < size) {
			reader.skipBytes(4);
//       recnum = reader.readInt();
			int length = reader.readInt();
			int shapetype = Formats.reverseInt(reader.readInt());

			if (0 != shapetype) {
				reader.skipBytes(32); //skipping bounds
				int numParts = Formats.reverseInt(reader.readInt());
				int numPoints = Formats.reverseInt(reader.readInt());
				int[] Parts = new int[numParts + 1]; //read numParts
				for (int i = 0; i < numParts; i++) {
					Parts[i] = Formats.reverseInt(reader.readInt()); //read parts
				}
				Parts[numParts] = numPoints;
				if (numParts > 1) {
					if (findHoles) {
						Vector vp = new Vector();
						for (int i = 0; i < numParts; i++) {
							int pointsInPart = Parts[i + 1] - Parts[i];

							RealPolyline line = new RealPolygon();
							line.p = new RealPoint[pointsInPart];
							line.isClosed = true;

							for (int j = 0; j < pointsInPart; j++) {
								line.p[j] = new RealPoint();
								line.p[j].x = Formats.longToFloat(reader.readLong());
								line.p[j].y = Formats.longToFloat(reader.readLong());
							}

							vp.addElement(line);
						}

						for (int i = 0; i < vp.size(); i++) {
							RealPolygon p = (RealPolygon) vp.elementAt(i);
							if (p.p != null) {
								for (int j = 0; j < vp.size(); j++) {
									RealPolygon q = (RealPolygon) vp.elementAt(j);
									if (q.p != null && i != j) {
										if (p.contains(q)) {
											RealPolyline l = new RealPolyline();
											l.p = q.p;
											l.isClosed = true;
											if (p.pp == null) {
												p.pp = new Vector();
											}
											p.pp.addElement(l);
											q.p = null;
										}
									}
								}
							}
						}

						MultiGeometry mg = new MultiGeometry();
						for (int i = 0; i < vp.size(); i++) {
							RealPolyline pl = (RealPolyline) vp.elementAt(i);
							if (pl.p != null) {
								mg.addPart(pl);
							}
						}

						String id = null;
						if (table != null) {
							id = table.getDataItemId(recnum);
						}
						if (id == null) {
							id = String.valueOf(recnum + 1);
						}
						SpatialEntity spe = new SpatialEntity(id);
						spe.setGeometry(mg);
						if (table != null) {
							if (table.getDataItem(recnum) instanceof ThematicDataItem) {
								spe.setThematicData((ThematicDataItem) table.getDataItem(recnum));
							}
							if (table.getDataItemName(recnum) != null) {
								spe.setName(table.getDataItemName(recnum));
							}

						}
						if (spec.idsImportant && spec.mayHaveMultiParts) {
							data.addDataItem(spe);
						} else {
							data.addItemSimple(spe);
							//data.addDataItem(spe);
							//System.out.println(shapetype+":"+spe.getId()+":"+spe.getName());
						}

					} else {

						MultiGeometry mg = new MultiGeometry();
						for (int i = 0; i < numParts; i++) {
							int pointsInPart = Parts[i + 1] - Parts[i];
							RealPolyline line = new RealPolyline();
							line.p = new RealPoint[pointsInPart];
							for (int j = 0; j < pointsInPart; j++) {
								line.p[j] = new RealPoint();
								line.p[j].x = Formats.longToFloat(reader.readLong());
								line.p[j].y = Formats.longToFloat(reader.readLong());
							} //for (int j = Parts[i]; j < Parts[i + 1]; j++)
							mg.addPart(line);
						} // for (int i = 0; i < numParts; i++)
						String id = null;
						if (table != null) {
							id = table.getDataItemId(recnum);
						}
						if (id == null) {
							id = String.valueOf(recnum + 1);
						}
						SpatialEntity spe = new SpatialEntity(id);
						spe.setGeometry(mg);
						if (table != null) {
							if (table.getDataItem(recnum) instanceof ThematicDataItem) {
								spe.setThematicData((ThematicDataItem) table.getDataItem(recnum));
							}
							if (table.getDataItemName(recnum) != null) {
								spe.setName(table.getDataItemName(recnum));
							}
						}
						data.addItemSimple(spe);
						//data.addDataItem(spe);
					}
					//}else{

					//}
				} else { //if (numParts > 1)
					RealPolyline line = new RealPolyline();
					line.p = new RealPoint[numPoints];
					for (int j = 0; j < numPoints; j++) {
						line.p[j] = new RealPoint();
						line.p[j].x = Formats.longToFloat(reader.readLong());
						line.p[j].y = Formats.longToFloat(reader.readLong());
					} //for (int j = 0; j < numPoints; j++)
					String id = null;
					if (table != null) {
						id = table.getDataItemId(recnum);
					}
					if (id == null) {
						id = String.valueOf(recnum + 1);
					}
					SpatialEntity spe = new SpatialEntity(id);
					spe.setGeometry(line);
					if (table != null) {
						if (table.getDataItem(recnum) instanceof ThematicDataItem) {
							spe.setThematicData((ThematicDataItem) table.getDataItem(recnum));
						}
						if (table.getDataItemName(recnum) != null) {
							spe.setName(table.getDataItemName(recnum));
						}
					}
					if (spec.idsImportant && (spec.mayHaveMultiParts || spec.mayHaveHoles)) {
						data.addDataItem(spe);
					} else {
						data.addItemSimple(spe);
					}

				} //if (numParts > 1)
			} // if (0 != shapetype)
			pos += 4 + length;
			++recnum;
			if (recnum % 50 == 0) {
				//following text:" shapes read
				showMessage(String.valueOf(recnum) + res.getString("shapes_read"), false);
			}
		} //while (pos < size)
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

	/**
	* Returns the table with the attribute (thematic) data loaded (if any)
	*/
	@Override
	public DataTable getAttrData() {
		if (table == null) {
			constructTable();
		}
		return table;
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
		if (dataReadingInProgress) {
			waitDataReadingFinish();
		} else {
			loadData(false);
		}
		return data;
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
		table = null;
	}
}
