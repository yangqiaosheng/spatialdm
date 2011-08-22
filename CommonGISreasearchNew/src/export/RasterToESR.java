package export;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.SystemUI;
import spade.lib.lang.Language;
import spade.vis.database.ObjectFilter;
import spade.vis.dmap.DGridLayer;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RasterGeometry;
import spade.vis.spec.DataSourceSpec;

/**
* Saves a raster layer into a file in ESR format
*/
public class RasterToESR implements LayerExporter {
	static ResourceBundle res = Language.getTextResource("export.Res");

	/**
	* Returns the name of the output format suitable for showing to the user.
	* For example, "Shape (ArcView)", "csv (comma-separated values)"
	*/
	@Override
	public String getFormatName() {
		return "ESR (ArcGIS)";
	}

	/**
	* Returns the usual file extension corresponding to this export format
	*/
	@Override
	public String getFileExtension() {
		return "esr";
	}

	/**
	* Returns true if this data exporter can store attributes from a table
	*/
	@Override
	public boolean canWriteAttributes() {
		return false;
	}

	/**
	* Returns a short text to explain the user what kind of layer may be saved
	* in this format. For example, "vector layer", "raster layer", "point objects"
	*/
	@Override
	public String getDataChar() {
		return "raster layer";
	}

	/**
	 * Replies if this exporter is applicable to a layer of the given type and,
	 * possibly, subtype.
	 */
	@Override
	public boolean isApplicable(char layerType, char subType) {
		return layerType == Geometry.raster;
	}

	/**
	* Returns true if the format requires creation of more than one files
	*/
	@Override
	public boolean needsMultipleFiles() {
		return false;
	}

	/**
	* Writes the data to the given stream. The SystemUI provided may be used for
	* displaying diagnostic messages. The exporter must check if the object passed
	* to it has the required type. Returns true if the data have been successfully
	* stored. Arguments:
	* data:          the table or layer to be stored
	* filter:        filter of records or objects. May be null. If not null, only
	*                the records (objects) satisfying the filter must be stored.
	* selAttr:       selected attributes to be stored. If null, no attributes
	*                are stored. Not appropriate for exporters that only store
	*                geographic data.
	* stream:        the stream in which to put the data (not necessarily a file,
	*                may be, for example, a script.
	* This method is not suitable for exporters that need to write to several files!
	*/
	@Override
	public boolean storeData(Object data, ObjectFilter filter, Vector selAttr, OutputStream stream, SystemUI ui) {
		if (data == null || stream == null)
			return false;
		if (!(data instanceof DGridLayer)) {
			// following string: "Illegal data type: DGridLayer expected!"
			if (ui != null) {
				ui.showMessage(res.getString("Illegal_data_type2"), true);
			}
			return false;
		}
		DGridLayer layer = (DGridLayer) data;
		if (layer.getObjectCount() < 1) {
			// following string: "No data in the layer!"
			if (ui != null) {
				ui.showMessage(res.getString("No_data_in_the_layer_"), true);
			}
			return false;
		}
		RasterGeometry rg = (RasterGeometry) layer.getObject(0).getGeometry();
		if (rg == null) {
			// following string: "No raster data found!"
			if (ui != null) {
				ui.showMessage(res.getString("No_raster_data_found_"), true);
			}
			return false;
		}
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream));

		float cellsize = Math.min(Math.abs(rg.DX), Math.abs(rg.DY));
		int ncols = (int) Math.abs((rg.rx2 - rg.rx1) / cellsize);
		int nrows = (int) Math.abs((rg.ry2 - rg.ry1) / cellsize);
		String dummy = new Integer((int) ((rg.minV - 200) / 100) * 100).toString();

		try {
			// following string: "Writing data..."
			if (ui != null) {
				ui.showMessage(res.getString("Writing_data_"), false);
			}

			writer.write("ncols " + String.valueOf(ncols) + "\n");
			writer.write("nrows " + String.valueOf(nrows) + "\n");
			writer.write("xllcorner " + new Float(Math.min(rg.rx1, rg.rx2)).toString() + "\n");
			writer.write("yllcorner " + new Float(Math.min(rg.ry1, rg.ry2)).toString() + "\n");
			writer.write("cellsize " + new Float(cellsize).toString() + "\n");
			writer.write("NDATA_value " + dummy + "\n");

			float val;
			if (ncols == rg.Col && nrows == rg.Row) {
				for (int yy = nrows - 1; yy >= 0; yy--) {
					for (int xx = 0; xx < ncols; xx++) {
						val = rg.ras[xx][yy];
						writer.write((Float.isNaN(val) ? dummy : String.valueOf(val)) + " ");
					}
					if (ui != null && nrows % 100 == 0) {
						// following string:" rows stored"
						ui.showMessage(nrows + res.getString("rows_stored"), false);
					}
				}
			} else {
				for (int yy = nrows - 1; yy >= 0; yy--) {
					for (int xx = 0; xx < ncols; xx++) {
						val = rg.getInterpolatedValue(rg.getGridX(xx * cellsize + rg.Xbeg), rg.getGridY(yy * cellsize + rg.Ybeg)); //grid coordinates
						writer.write((Float.isNaN(val) ? dummy : String.valueOf(val)) + " ");
					}
					if (ui != null && nrows % 100 == 0) {
						// following string:" rows stored"
						ui.showMessage(nrows + res.getString("rows_stored"), false);
					}
				}
			}
/*
      float val;
      for (int yy=rg.Row-1; yy>=0; yy--) {
        for (int xx=0; xx<rg.Col-1; xx++) {
          val = rg.ras[xx][yy];
          writer.write((Float.isNaN(val) ? "." : String.valueOf(val)) + " ");
        }
        val = rg.ras[rg.Col-1][yy];
        writer.write((Float.isNaN(val) ? "." : String.valueOf(val)) + "\n");
        ++nrows;
        if (ui!=null && nrows%100==0)
          // following string:" rows stored"
          ui.showMessage(nrows+res.getString("rows_stored"),false);
      }
*/
		} catch (IOException ioe) {
			// following string: "Error writing to the file: "
			if (ui != null) {
				ui.showMessage(res.getString("Error_writing_to_the") + ioe.toString(), true);
			}
			return false;
		}
		try {
			writer.flush();
		} catch (IOException ioe) {
		}
		if (ui != null)
			// following string:"Finished, "+nrows+" rows stored"
			if (nrows > 0) {
				ui.showMessage(res.getString("Finished_") + nrows + res.getString("rows_stored"), false);
			} else {
				ui.showMessage(res.getString("No_data_actually"), true);
			}
		return nrows > 0;
	}

	/**
	* Some formats, for example, Shape or ADF, require data to be stored in several
	* files. In this case the data cannot be written in just one stream and,
	* hence, the previous storeData method is not applicable. For this case
	* the method with arguments directory and file name insead of a stream
	* must be defined. Exporters that save data in just one file should in this
	* method open the specified file as a stream and call the method with the
	* stream argument.
	*/
	@Override
	public boolean storeData(Object data, ObjectFilter filter, Vector selAttr, String dir, String filename, SystemUI ui) {
		if (data == null || filename == null)
			return false;
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(dir + filename);
		} catch (IOException ioe) {
			if (ui != null) {
				ui.showMessage(ioe.toString(), true);
			}
			return false;
		}
		if (out != null) {
			boolean result = storeData(data, filter, selAttr, out, ui);
			try {
				out.close();
			} catch (IOException e) {
			}
			return result;
		}
		// following string:"Could not open the file "
		if (ui != null) {
			ui.showMessage(res.getString("Could_not_open_the") + dir + filename, true);
		}
		return false;
	}

	/**
	 * Returns the description of the file or database table to which the data
	 * were stored
	 */
	@Override
	public DataSourceSpec getStoredDataDescriptor() {
		return null;
	}
}
