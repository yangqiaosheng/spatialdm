//ID
package data_load.readers;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.analysis.system.GeoDataReader;
import spade.lib.lang.Language;
import spade.lib.util.CopyFile;
import spade.vis.database.DataPortion;
import spade.vis.database.DataSupplier;
import spade.vis.database.LayerData;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGridLayer;
import spade.vis.geometry.RasterGeometry;
import spade.vis.spec.DataSourceSpec;

/**
* Gets a raster from a BIL file
*/
public class BILReader extends BaseDataReader implements GeoDataReader, DataSupplier {
	static ResourceBundle res = Language.getTextResource("data_load.readers.Res");
	/**
	* The spatial data loaded
	*/
	protected LayerData data = null;

	/**
	* Assuming that the data stream is already opened, tries to read from it
	* spatial raster data in FLT format
	*/
	protected LayerData readSpecific() {
		int Col = -1, Row = -1;
		boolean Intr = true, Geog = true;
		float Xbeg, Ybeg, DX = Float.NaN, DY = Float.NaN;
		float[][] ras = null;
		float minV = Float.POSITIVE_INFINITY;
		float maxV = Float.NEGATIVE_INFINITY;
		float x1, y1, x2, y2;

		final int BIL = 0;
		final int BIP = 1;
		final int BSQ = 2;
		final boolean Intel = true;
		final boolean Motorola = false;
		int nbands = 1;
		int nbits = 8;
		boolean byteorder = Intel;
		int layout = BIL;
		long skipbytes = 0;
		Float ulxmap = null;
		Float ulymap = null;
		long bandrowbytes = -1;
		long totalrowbytes = -1;
		long bandgapbytes = 0;

		String dataSource = spec.source;

		try {
			InputStream stream = null;
			DataInputStream dreader;
			BufferedInputStream breader;
			BufferedReader hreader;
			String s, key, val;
			StringTokenizer st;

// .hdr
			dataSource = CopyFile.getDir(dataSource) + CopyFile.getNameWithoutExt(dataSource) + ".hdr";
			//following text: "reading"
			showMessage(res.getString("reading") + dataSource, false);
			stream = openStream(dataSource);
			if (stream == null)
				return null;
			hreader = new BufferedReader(new InputStreamReader(stream));

			while (true) {
				s = hreader.readLine();
				if (s == null) {
					break;
				}
				s = s.trim();
				if (s.length() == 0) {
					continue;
				}

				st = new StringTokenizer(s, " \n\r\t");
				if (st.countTokens() != 2) {
					continue;
				}
				key = st.nextToken();
				val = st.nextToken();

				if (key.equalsIgnoreCase("ncols")) {
					Col = Integer.parseInt(val);
				} else if (key.equalsIgnoreCase("nrows")) {
					Row = Integer.parseInt(val);
				} else if (key.equalsIgnoreCase("nbands")) {
					nbands = Integer.parseInt(val);
				} else if (key.equalsIgnoreCase("nbits")) {
					nbits = Integer.parseInt(val);
				} else if (key.equalsIgnoreCase("byteorder")) {
					if (val.equalsIgnoreCase("i")) {
						byteorder = Intel;
					} else {
						byteorder = Motorola;
					}
				} else if (key.equalsIgnoreCase("layout")) {
					if (val.equalsIgnoreCase("bip")) {
						layout = BIP;
					} else if (val.equalsIgnoreCase("bsq")) {
						layout = BSQ;
					} else {
						layout = BIL;
					}
				} else if (key.equalsIgnoreCase("ulxmap")) {
					ulxmap = new Float(val);
				} else if (key.equalsIgnoreCase("ulymap")) {
					ulymap = new Float(val);
				} else if (key.equalsIgnoreCase("xdim")) {
					DX = new Float(val).floatValue();
				} else if (key.equalsIgnoreCase("ydim")) {
					DY = new Float(val).floatValue();
				} else if (key.equalsIgnoreCase("skipbytes")) {
					skipbytes = Long.parseLong(val);
				} else if (key.equalsIgnoreCase("bandrowbytes")) {
					bandrowbytes = Long.parseLong(val);
				} else if (key.equalsIgnoreCase("totalrowbytes")) {
					totalrowbytes = Long.parseLong(val);
				} else if (key.equalsIgnoreCase("bandgapbytes")) {
					bandgapbytes = Long.parseLong(val);
				}
			}
			closeStream(stream);
			//checking
			if (Col <= 0 || Row <= 0 || nbands <= 0 || skipbytes < 0 || bandgapbytes < 0)
				return null;
			if (nbits != 1 && nbits != 4 && nbits != 8 && nbits != 16 && nbits != 32)
				return null;
			if (bandrowbytes <= 0) {
				bandrowbytes = (long) Math.ceil((float) Col * (float) nbits / 8);
			}

			if (totalrowbytes <= 0) //questionable...
				if (layout == BIL) {
					totalrowbytes = nbands * bandrowbytes;
				} else if (layout == BIP) {
					totalrowbytes = (long) Math.ceil((float) Col * (float) nbands * nbits / 8);
				}

//reading georeference
			try {
				dataSource = CopyFile.getDir(dataSource) + CopyFile.getNameWithoutExt(dataSource) + ".blw";
				//following text:"reading "
//        showMessage(res.getString("reading")+dataSource,false);
				stream = openStream(dataSource);
				if (stream != null) {
					hreader = new BufferedReader(new InputStreamReader(stream));
					for (int i = 0; i < 6; i++) {
						try {
							s = hreader.readLine();
							if (s == null) {
								break;
							}
							if (i == 1 || i == 2) {
								continue; //these lines contain rotation parameters
							}
							s = s.trim();
							Float dbl = null;
							try {
								dbl = Float.valueOf(s);
							} catch (NumberFormatException nfe) {
							}
							if (dbl == null) {
								continue;
							}
							if (i == 0) {
								DX = dbl.floatValue();
							} else if (i == 3) {
								DY = Math.abs(dbl.floatValue());
							} else if (i == 4) {
								ulxmap = dbl;
							} else {
								ulymap = dbl;
							}
						} catch (IOException ioe1) {
							showMessage(ioe1.toString(), true);
							break;
						}
					}
					closeStream(stream);
				}
			} catch (Exception ex) {
			}

			if (DX == Float.NaN) {
				DX = 1;
			}
			if (DY == Float.NaN) {
				DY = 1;
			}
			if (ulxmap == null) {
				ulxmap = new Float(0);
			}
			if (ulymap == null) {
				ulymap = new Float(Row - 1);
			}
//      Xbeg = ulxmap.floatValue() - DX/2;
//      Ybeg = ulymap.floatValue() - Row*DY + DY/2;
//ID
			Xbeg = ulxmap.floatValue();
			Ybeg = ulymap.floatValue() - (Row - 1) * DY;
//~ID
			System.out.println("Raster parameters:  " + "Col: " + Col + ", Row: " + Row + ", Xbeg: " + Xbeg + ", Ybeg: " + Ybeg + ", DX: " + DX + ", DY: " + DY);

/// Reading raster

			//not yet implemented features
			if (nbits == 1 || nbits == 4)
				return null;
			if (nbands != 1)
				return null;

			if (nbits == 32) {
				System.out.println("*** 32-bit values in BIL files are treated as IEEE FLOAT values! ***");
			}
			if (nbits == 32) {
				System.out.println("*** Motorola byte order files are read with swapping of bytes!!! ***");
			}

//.bil ...
			if (layout == BIL) {
				dataSource = CopyFile.getDir(dataSource) + CopyFile.getNameWithoutExt(dataSource) + ".bil";
			} else
				return null; //not yet implemented

			//following text:"reading "
			showMessage(res.getString("reading") + dataSource, false);
			stream = openStream(dataSource);
			breader = new BufferedInputStream(stream, (int) bandrowbytes);
			dreader = new DataInputStream(breader);

			dreader.skip(skipbytes);

			byte curB;
			short curS;
			long curI;
			float curV;

			ras = new float[Col][Row];
			for (int i = 0; i < Row; i++) {
				for (int j = 0; j < Col; j++) {
					ras[j][i] = Float.NaN;
				}
			}
/*
      x1=(float)Xbeg;
      y1=(float)Ybeg;
      x2=(float)(Xbeg+DX*Col);
      y2=(float)(Ybeg+DY*Row);
*/
//ID
			x1 = (Xbeg - DX / 2);
			y1 = (Ybeg - DY / 2);
			x2 = (Xbeg + DX * Col - DX / 2);
			y2 = (Ybeg + DY * Row - DY / 2);
//~ID

			if (nbits == 8) {
				for (int yy = 0; yy < Row; yy++) {
					for (int xx = 0; xx < Col; xx++) {
						curV = (byte) dreader.read();
						if (curV < spec.validMin || curV > spec.validMax) {
							curV = Float.NaN;
						}
						ras[xx][Row - yy - 1] = curV;
					}
					if (yy != Row - 1) {
						dreader.skip(bandrowbytes - (long) Math.ceil((float) Col * (float) nbits / 8));
					}
					if (yy % 100 == 0) {
						//following text:"Read"
						//following text:"of"
						//following text:"rows"
						showMessage(res.getString("Read") + yy + res.getString("of") + Row + res.getString("rows"), false);
					}
				}
			} else if (nbits == 16) {
				for (int yy = 0; yy < Row; yy++) {
					for (int xx = 0; xx < Col; xx++) {
						if (byteorder == Motorola) {
							curS = (short) (dreader.read() << 8);
							curS |= dreader.read();
						} else {
							curS = (short) dreader.read();
							curS |= dreader.read() << 8;
						}
						curV = curS;
						if (curV < spec.validMin || curV > spec.validMax) {
							curV = Float.NaN;
						}
						ras[xx][Row - yy - 1] = curV;
					}
					if (yy != Row - 1) {
						dreader.skip(bandrowbytes - (long) Math.ceil((float) Col * (float) nbits / 8));
					}
					if (yy % 100 == 0) {
						//following text:"Read"
						//following text:"of"
						//following text:"rows"
						showMessage(res.getString("Read") + yy + res.getString("of") + Row + res.getString("rows"), false);
					}
				}
			} else if (nbits == 32) {
				//          return null; //not yet implemented
// the following block is used for reading raw floating point binary grid data as BIL files
				for (int yy = 0; yy < Row; yy++) {
					for (int xx = 0; xx < Col; xx++) {
// maybe vice versa
						if (byteorder == Intel) {
							curV = dreader.readFloat();
						} else {
							int bytes = dreader.readInt();
							int swapped = 0;
							swapped |= ((bytes & 0xFF000000l) >> 24);
							swapped |= ((bytes & 0x00FF0000l) >> 8);
							swapped |= ((bytes & 0x0000FF00l) << 8);
							swapped |= ((bytes & 0x000000FFl) << 24);
							curV = Float.intBitsToFloat(swapped);
						}
						if (curV < spec.validMin || curV > spec.validMax) {
							curV = Float.NaN;
						}
						ras[xx][Row - yy - 1] = curV;
					}
					if (yy != Row - 1) {
						dreader.skip(bandrowbytes - (long) Math.ceil((float) Col * (float) nbits / 8));
					}
					if (yy % 100 == 0) {
						showMessage(res.getString("Read") + yy + res.getString("of") + Row + res.getString("rows"), false);
					}
				}
			} else
				return null; //not yet implemented

		} catch (IOException ioe) {
			//following text:"Exception reading raster: "
			showMessage(res.getString("Exception_reading") + ioe, true);
			return null;
		}

		LayerData data = new LayerData();
		data.setBoundingRectangle(x1, y1, x2, y2);
		RasterGeometry geom = new RasterGeometry();
		geom.ras = ras;
		geom.rx1 = x1;
		geom.ry1 = y1;
		geom.rx2 = x2;
		geom.ry2 = y2;

		geom.Col = Col;
		geom.Row = Row;
		geom.Intr = Intr;
		geom.Geog = Geog;
		geom.Xbeg = Xbeg;
		geom.Ybeg = Ybeg;
		geom.DX = DX;
		geom.DY = DY;
		geom.maxV = maxV;
		geom.minV = minV;
		geom.recalculateStatistics();

		SpatialEntity spe = new SpatialEntity("raster");
		spe.setGeometry(geom);
		data.addDataItem(spe);
		//following text:"The raster has been loaded from "
		showMessage(res.getString("The_raster_has_been") + spec.source, false);
		data.setHasAllData(true);
		return data;

//  System.out.println("Raster parameters:  " + Col + "  " + Row + "  " + Xbeg + "  " + Ybeg + "  " + DX + "  " + DY + "  " + Intr + "  " + Geog);
//  return null;
	}

//~ID
	/**
	* Opens the stream on the specified data source. If the source
	* string starts with HTTP or FILE, accesses the source as a URL, otherwise
	* opens it as a local file
	*/
	protected InputStream openStream(String dataSource) {
		if (dataSource == null)
			return null;
		int idx = dataSource.indexOf(':');
		boolean isURL = false;
		if (idx > 0) {
			String pref = dataSource.substring(0, idx);
			if (pref.equalsIgnoreCase("HTTP") || pref.equalsIgnoreCase("FILE")) {
				isURL = true;
			}
		}
		InputStream stream = null;
		try {
			if (isURL) { //try to access the source as a URL
				URL url = new URL(dataSource);
				URLConnection urlc = url.openConnection();
				urlc.setUseCaches(mayUseCache);
				stream = urlc.getInputStream();
			} else {
				stream = new FileInputStream(dataSource);
			}
		} catch (IOException ioe) {
			//following text:"Error accessing "
			showMessage(res.getString("Error_accessing") + dataSource + ": " + ioe, true);
			return null;
		}
		return stream;
	}

	protected void closeStream(InputStream stream) {
		if (stream == null)
			return;
		try {
			stream.close();
		} catch (IOException ioe) {
		}
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
		if (!mayAskUser) {
			setDataReadingInProgress(true);
		}
		if (spec == null || spec.source == null)
			if (mayAskUser) {
				//following text:"Select the file with raster data"
				String path = browseForFile(res.getString("Select_the_file_with"), "*.bil");
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
		//following text:"Start reading raster data from "
		showMessage(res.getString("Start_reading_raster") + spec.source, false);
		data = readSpecific();
		dataError = data == null;
		setDataReadingInProgress(false);
		return !dataError;
	}

	/**
	* Returns the map layer constructed from the geographical data loaded (if any).
	* If the data have not been loaded yet, the reader constructs a layer without
	* objects and sets itself as the  data supplier of the layer. Then the data
	* will be loaded when the layer is first darwn.
	*/
	@Override
	public DGeoLayer getMapLayer() {
		DGridLayer layer = new DGridLayer();
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
