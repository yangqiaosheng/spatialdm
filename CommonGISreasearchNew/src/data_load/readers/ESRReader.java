//ID
package data_load.readers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
* Gets a raster from a ESR file
*/
public class ESRReader extends DataStreamReader implements GeoDataReader, DataSupplier {
	static ResourceBundle res = Language.getTextResource("data_load.readers.Res");
	/**
	* The spatial data loaded
	*/
	protected LayerData data = null;

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
				String path = browseForFile(res.getString("Select_the_file_with"), "*.esr");
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
	* spatial raster data in ESR format
	*/
	protected LayerData readSpecific() {
		if (stream == null)
			return null;

		int Col, Row;
		boolean Intr = true, Geog = true;
		float Xbeg, Ybeg, DX, DY;
		float[][] ras = null;
		float minV = Float.MAX_VALUE;
		float maxV = -Float.MAX_VALUE;
		float dummy;
		float x1, y1, x2, y2;

		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		//following text:"reading raster from "
		String txt = res.getString("reading_raster_from") + spec.source + ":";
		try {
			String field;
			String s;
			StringTokenizer st;

			s = reader.readLine();
			if (s == null)
				return null;
			s = s.trim();
			if (s.length() < 1)
				return null;
			st = new StringTokenizer(s, " \n\r\t");
			if (st.countTokens() != 2)
				return null;
			field = st.nextToken();
			if (!field.equalsIgnoreCase("ncols"))
				return null;
			Col = Integer.parseInt(st.nextToken());

			s = reader.readLine();
			if (s == null)
				return null;
			s = s.trim();
			if (s.length() < 1)
				return null;
			st = new StringTokenizer(s, " \n\r\t");
			if (st.countTokens() != 2)
				return null;
			field = st.nextToken();
			if (!field.equalsIgnoreCase("nrows"))
				return null;
			Row = Integer.parseInt(st.nextToken());

			s = reader.readLine();
			if (s == null)
				return null;
			s = s.trim();
			if (s.length() < 1)
				return null;
			st = new StringTokenizer(s, " \n\r\t");
			if (st.countTokens() != 2)
				return null;
			field = st.nextToken();
			if (!field.equalsIgnoreCase("xllcorner"))
				return null;
			Xbeg = (new Float(st.nextToken())).floatValue();

			s = reader.readLine();
			if (s == null)
				return null;
			s = s.trim();
			if (s.length() < 1)
				return null;
			st = new StringTokenizer(s, " \n\r\t");
			if (st.countTokens() != 2)
				return null;
			field = st.nextToken();
			if (!field.equalsIgnoreCase("yllcorner"))
				return null;
			Ybeg = (new Float(st.nextToken())).floatValue();

			s = reader.readLine();
			if (s == null)
				return null;
			s = s.trim();
			if (s.length() < 1)
				return null;
			st = new StringTokenizer(s, " \n\r\t");
			if (st.countTokens() != 2)
				return null;
			field = st.nextToken();
			if (!field.equalsIgnoreCase("cellsize"))
				return null;
			DX = (new Float(st.nextToken())).floatValue();
			DY = -DX;

			s = reader.readLine();
			if (s == null)
				return null;
			s = s.trim();
			if (s.length() < 1)
				return null;
			st = new StringTokenizer(s, " \n\r\t");
			if (st.countTokens() != 2)
				return null;
			field = st.nextToken();
			if (!field.equalsIgnoreCase("NDATA_value") && !field.equalsIgnoreCase("nodata_value"))
				return null;
			dummy = (new Float(st.nextToken())).floatValue();

			Xbeg += DX / 2;
			Ybeg += DY / 2 - DY * Row;

			ras = new float[Col][Row];
			int curX = (DX > 0) ? 0 : Col - 1, curY = (DY > 0) ? 0 : Row - 1;
			int incX = (DX > 0) ? 1 : -1, incY = (DY > 0) ? 1 : -1;
			int minX = curX, maxX = (DX > 0) ? Col : -1;
			String curToken;
			float curV = 0;
			int count = 0;
			while (true) {
				s = reader.readLine();
				if (s == null) {
					break;
				}
				s = s.trim();
				if (s.length() < 1) {
					break;
				}
				++count;
				if (count % 50 == 0) {
					txt = txt + " .";
					showMessage(txt, false);
				}
				st = new StringTokenizer(s, " \n\r\t");
				while (st.hasMoreTokens()) {
					curToken = st.nextToken();
//          if (curToken.compareTo(".")==0 || curToken.compareTo("-32767")==0)
//            ras[curX][curY]=Float.NaN;
//          else {
					curV = (new Float(curToken)).floatValue();
					if ((curV - dummy) < Float.MIN_VALUE * 2) {
						curV = Float.NaN;
					}
					if (curV < spec.validMin || curV > spec.validMax) {
						curV = Float.NaN;
					}
					ras[curX][curY] = curV;
					if (minV > curV) {
						minV = curV;
					}
					if (maxV < curV) {
						maxV = curV;
					}
//          }
					curX += incX;
					if (curX == maxX) {
						curX = minX;
						curY += incY;
					}
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
			if (DX < 0) {
				float tmp = x1;
				x1 = x2;
				x2 = tmp;
				Xbeg = Xbeg + DX * (Col - 1);
				DX = -DX;
			}
			if (DY < 0) {
				float tmp = y1;
				y1 = y2;
				y2 = tmp;
				Ybeg = Ybeg + DY * (Row - 1);
				DY = -DY;
			}
		} catch (IOException ioe) {
			//following text:"Exception reading raster: "
			showMessage(res.getString("Exception_reading") + ioe, true);
			return null;
		}
		LayerData ld = new LayerData();
		ld.setBoundingRectangle(x1, y1, x2, y2);
		RasterGeometry geom = new RasterGeometry(x1, y1, x2, y2);
		geom.ras = ras;
//    geom.rx1=x1; geom.ry1=y1; geom.rx2=x2; geom.ry2=y2;
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
		SpatialEntity spe = new SpatialEntity("raster");
		spe.setGeometry(geom);
		ld.addDataItem(spe);
		//following text:"The raster "
		//following text:" has been loaded"
		showMessage(res.getString("The_raster") + spec.source + res.getString("has_been_loaded"), false);
		ld.setHasAllData(true);
		return ld;
	}

//~ID
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
