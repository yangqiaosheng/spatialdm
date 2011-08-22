package export;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
* Saves a raster layer into a file in ADF format (ArcView)
*/
public class RasterToADF implements LayerExporter {
	static ResourceBundle res = Language.getTextResource("export.Res");

	/**
	* Returns the name of the output format suitable for showing to the user.
	* For example, "Shape (ArcView)", "csv (comma-separated values)"
	*/
	@Override
	public String getFormatName() {
		return "ADF (ArcView)";
	}

	/**
	* Returns the usual file extension corresponding to this export format
	*/
	@Override
	public String getFileExtension() {
		return "adf";
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
	* Since the ADF format uses several files for storing data, this method
	* is not applicable. Therefore it immediately returns false.
	*/
	@Override
	public boolean storeData(Object data, ObjectFilter filter, Vector selAttr, OutputStream stream, SystemUI ui) {
		return false;
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
		if (!(data instanceof DGridLayer)) {
			// following string:  "Illegal data type: DGridLayer expected!"
			if (ui != null) {
				ui.showMessage("Illegal data type: DGridLayer expected!", true);
			}
			return false;
		}
		DGridLayer layer = (DGridLayer) data;
		if (layer.getObjectCount() < 1) {
			// following string: "No data in the layer!"
			if (ui != null) {
				ui.showMessage("No data in the layer!", true);
			}
			return false;
		}
		RasterGeometry rg = (RasterGeometry) layer.getObject(0).getGeometry();
		if (rg == null) {
			// following string: "No raster data found!"
			if (ui != null) {
				ui.showMessage("No raster data found!", true);
			}
			return false;
		}
		int HTileXSize = 32;
		int HTileYSize = 32;
		int HTilesPerRow = (rg.Col % HTileXSize == 0) ? rg.Col / HTileXSize : rg.Col / HTileXSize + 1;
		int HTilesPerColumn = (rg.Row % HTileYSize == 0) ? rg.Row / HTileYSize : rg.Row / HTileYSize + 1;
		DataOutputStream writer = null;
		// following string: "Writing dblbnd.adf..."
		if (ui != null) {
			ui.showMessage("Writing dblbnd.adf...", false);
		}
		boolean error = false;
		try {
			writer = new DataOutputStream(new FileOutputStream(dir + "dblbnd.adf"));
			writer.writeDouble(rg.Xbeg - rg.DX / 2);
			writer.writeDouble(rg.Ybeg - rg.DY / 2);
			writer.writeDouble(rg.Xbeg + rg.DX * (rg.Col - 1) + rg.DX / 2);
			writer.writeDouble(rg.Ybeg + rg.DY * (rg.Row - 1) + rg.DY / 2);
		} catch (IOException ioe) {
			System.out.println(ioe);
			if (ui != null) {
				ui.showMessage(ioe.toString(), true);
			}
			error = true;
		}
		if (writer != null) {
			try {
				writer.close();
			} catch (IOException ioe) {
			}
		}
		writer = null;
		// following string: "Writing sta.adf..."
		if (ui != null) {
			ui.showMessage("Writing sta.adf...", false);
		}
		try {
			writer = new DataOutputStream(new FileOutputStream(dir + "sta.adf"));
			writer.writeDouble(rg.minV);
			writer.writeDouble(rg.maxV);
			writer.writeDouble(((double) rg.maxV + (double) rg.minV) / 2);
			writer.writeDouble(((double) rg.maxV - (double) rg.minV) / 4);
		} catch (IOException ioe) {
			System.out.println(ioe);
			if (ui != null) {
				ui.showMessage(ioe.toString(), true);
			}
			error = true;
		}
		if (writer != null) {
			try {
				writer.close();
			} catch (IOException ioe) {
			}
		}
		writer = null;
		// following string: "Writing w001001x.adf..."
		if (ui != null) {
			ui.showMessage("Writing w001001x.adf...", false);
		}
		try {
			writer = new DataOutputStream(new FileOutputStream(dir + "w001001x.adf"));
			writer.writeLong(0x0000270AFFFFFC14l);
			writer.writeLong(0);
			writer.writeLong(0);
			writer.writeInt((HTilesPerRow * HTilesPerColumn * 8 + 72 + 4 + 16 + 8) / 2);
			for (int i = 0; i < 9; i++) {
				writer.writeLong(0);
			}
			for (int i = 0; i < HTilesPerRow * HTilesPerColumn; i++) {
				writer.writeInt((100 + i * (HTileXSize * HTileYSize * 4 + 2)) / 2);
				writer.writeInt(HTileXSize * HTileYSize * 2);
			}
		} catch (IOException ioe) {
			System.out.println(ioe);
			if (ui != null) {
				ui.showMessage(ioe.toString(), true);
			}
			error = true;
		}
		if (writer != null) {
			try {
				writer.close();
			} catch (IOException ioe) {
			}
		}
		writer = null;
		// following string:  "Writing hdr.adf..."
		if (ui != null) {
			ui.showMessage("Writing hdr.adf...", false);
		}
		try {
			writer = new DataOutputStream(new FileOutputStream(dir + "hdr.adf"));
			writer.writeLong(0x47524944312e3200l);
			writer.writeLong(0);
			writer.writeInt(2);
			for (int i = 0; i < 29; i++) {
				writer.writeLong(0);
			}
			writer.writeInt(0);
			writer.writeDouble(rg.DX);
			writer.writeDouble(rg.DY);
			writer.writeLong(0);
			writer.writeLong(0);
			writer.writeInt(HTilesPerRow);
			writer.writeInt(HTilesPerColumn);
			writer.writeInt(HTileXSize);
			writer.writeInt(1);
			writer.writeInt(HTileYSize);
		} catch (IOException ioe) {
			System.out.println(ioe);
			if (ui != null) {
				ui.showMessage(ioe.toString(), true);
			}
			error = true;
		}
		if (writer != null) {
			try {
				writer.close();
			} catch (IOException ioe) {
			}
		}
		writer = null;
		// following string: "Writing w001001.adf..."
		if (ui != null) {
			ui.showMessage("Writing w001001.adf...", false);
		}
		try {
			writer = new DataOutputStream(new FileOutputStream(dir + "w001001.adf"));
			writer.writeLong(0x0000270afffffc14l);
			writer.writeLong(0);
			writer.writeLong(0);
			writer.writeInt((HTilesPerRow * HTilesPerColumn * (HTileXSize * HTileYSize * 4 + 2) + 100) / 2);
			for (int i = 0; i < 9; i++) {
				writer.writeLong(0);
			}

			float val;
			for (int j = 0; j < HTilesPerColumn; j++) {
				for (int i = 0; i < HTilesPerRow; i++) {
					writer.writeShort(HTileXSize * HTileYSize * 2);
					for (int yy = 0; yy < HTileYSize; yy++) {
						for (int xx = 0; xx < HTileXSize; xx++) {
							try {
								val = rg.ras[i * HTileXSize + xx][rg.Row - 1 - (j * HTileYSize + yy)];
								if (Float.isNaN(val) || Float.isInfinite(val) || Math.abs(val) > 1E38) {
									writer.writeFloat(Float.intBitsToFloat(0xff7fffff));
								} else {
									writer.writeFloat(val);
								}
							} catch (ArrayIndexOutOfBoundsException ex) {
								writer.writeFloat(Float.intBitsToFloat(0xff7fffff));
							}
						}
					}
				}
			}
		} catch (IOException ioe) {
			System.out.println(ioe);
			if (ui != null) {
				ui.showMessage(ioe.toString(), true);
			}
			error = true;
		}
		if (writer != null) {
			try {
				writer.close();
			} catch (IOException ioe) {
			}
		}
		// following string: "Finished export in ADF format"
		if (!error && ui != null) {
			ui.showMessage(res.getString("Finished_export_in"), false);
		}
		return !error;
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
