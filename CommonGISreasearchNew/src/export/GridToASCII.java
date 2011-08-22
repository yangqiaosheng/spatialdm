package export;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

import spade.analysis.system.SystemUI;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.ObjectFilter;
import spade.vis.dmap.DVectorGridLayer;
import spade.vis.geometry.Geometry;
import spade.vis.spec.DataSourceSpec;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Aug 10, 2010
 * Time: 5:58:20 PM
 * Stores a grid layer (DVectorGridLayer) to 2 ASCII files:
 * 1) *.grd - definition of the columns and rows of the grid
 * 2) *.csv - attribute values in the grid cells
 */
public class GridToASCII extends TableToCSV implements LayerExporter {
	/**
	* Returns a short text to explain the user what kind of layer may be saved
	* in this format. For example, "vector layer", "raster layer", "point objects"
	*/
	@Override
	public String getDataChar() {
		return "grid layer (vector)";
	}

	/**
	* Returns the name of the output format suitable for showing to the user.
	* For example, "Shape (ArcView)", "csv (comma-separated values)"
	*/
	@Override
	public String getFormatName() {
		return "Grid";
	}

	/**
	* Returns the usual file extension corresponding to this export format
	*/
	@Override
	public String getFileExtension() {
		return "grd";
	}

	/**
	 * Replies if this exporter is applicable to a layer of the given type and,
	 * possibly, subtype.
	 */
	@Override
	public boolean isApplicable(char layerType, char subType) {
		return layerType == Geometry.area && subType == Geometry.rectangle;
	}

	/**
	* Returns true if this data exporter can store attributes from a table
	*/
	@Override
	public boolean canWriteAttributes() {
		return true;
	}

	/**
	* Returns true if the format requires creation of more than one files
	*/
	@Override
	public boolean needsMultipleFiles() {
		return true;
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
	public boolean storeData(Object data, ObjectFilter filter, Vector selAttr, String dir, String gridFileName, SystemUI ui) {
		if (data == null || gridFileName == null)
			return false;
		if (!(data instanceof DVectorGridLayer)) {
			if (ui != null) {
				ui.showMessage("The layer is not a grid!", true);
			}
			return false;
		}
		DVectorGridLayer gl = (DVectorGridLayer) data;
		float colXCoord[] = gl.getColXCoords();
		if (colXCoord == null) {
			if (ui != null) {
				ui.showMessage("The layer has no data!", true);
			}
			return false;
		}
		float rowYCoord[] = gl.getRowYCoords();
		if (rowYCoord == null) {
			if (ui != null) {
				ui.showMessage("The layer has no data!", true);
			}
			return false;
		}
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(dir + gridFileName);
		} catch (IOException ioe) {
			if (ui != null) {
				ui.showMessage(ioe.toString(), true);
			}
			System.out.println(ioe.toString());
			return false;
		}

		DataOutputStream dos = new DataOutputStream(out);
		boolean error = false;
		try {
			dos.writeBytes("<columns>\r\n");
			for (int i = 0; i < colXCoord.length; i++) {
				if (i > 0) {
					dos.writeBytes(", ");
				}
				dos.writeBytes(String.valueOf(colXCoord[i]));
			}
			dos.writeBytes("\r\n");
			dos.writeBytes("</columns>\r\n");
			dos.writeBytes("<rows>\r\n");
			for (int i = 0; i < rowYCoord.length; i++) {
				if (i > 0) {
					dos.writeBytes(", ");
				}
				dos.writeBytes(String.valueOf(rowYCoord[i]));
			}
			dos.writeBytes("\r\n");
			dos.writeBytes("</rows>\r\n");
		} catch (IOException ioe) {
			if (ui != null) {
				ui.showMessage(ioe.toString(), true);
			}
			System.out.println(ioe.toString());
			error = true;
		}
		try {
			out.close();
		} catch (IOException e) {
		}
		if (error)
			return false;
		makeSourceSpecification(gl, dir + gridFileName);
		out = null;

		AttributeDataPortion table = gl.getThematicData();
		if (table == null)
			return true;

		String tableFileName = makeFileNameForTable(gridFileName);

		boolean result = false;
		try {
			out = new FileOutputStream(dir + tableFileName);
		} catch (IOException ioe) {
			if (ui != null) {
				ui.showMessage(ioe.toString(), true);
			}
			System.out.println(ioe.toString());
			return false;
		}
		if (out != null) {
			result = storeData(table, filter, selAttr, out, ui);
			try {
				out.close();
			} catch (IOException e) {
			}
			if (result) {
				makeSourceSpecification(table, dir + tableFileName);
				DataSourceSpec spec = (DataSourceSpec) table.getDataSource();
				if (spec != null) {
					gl.setDataSource(spec);
					spec.source = dir + gridFileName;
					spec.objDescrSource = dir + tableFileName;
					spec.format = "GRID";
				}
			}
			return result;
		}
		// following string: "Could not open the file "
		if (ui != null) {
			ui.showMessage("Could not open the file " + dir + gridFileName, true);
		}
		return false;
	}

	@Override
	protected void makeSourceSpecification(Object data, String source) {
		if (data instanceof AttributeDataPortion) {
			super.makeSourceSpecification(data, source);
			return;
		}
		if (!(data instanceof DVectorGridLayer))
			return;
		DVectorGridLayer gl = (DVectorGridLayer) data;
		DataSourceSpec spec = null;
		if (gl.getDataSource() != null && (gl.getDataSource() instanceof DataSourceSpec)) {
			spec = (DataSourceSpec) gl.getDataSource();
		}
		if (spec == null || spec.source == null || spec.source.equalsIgnoreCase("_derived")) {
			if (spec == null) {
				spec = new DataSourceSpec();
				gl.setDataSource(spec);
			}
			spec.source = source;
			spec.format = "GRID";
			spec.name = gl.getName();
			spec.objDescrSource = makeFileNameForTable(source);
		}
	}

	protected String makeFileNameForTable(String gridFileName) {
		String tableFileName = gridFileName;
		if (gridFileName.endsWith(".grd") || gridFileName.endsWith(".GRD")) {
			tableFileName = gridFileName.substring(0, gridFileName.length() - 4);
		} else if (gridFileName.endsWith(".grid") || gridFileName.endsWith(".GRID")) {
			tableFileName = gridFileName.substring(0, gridFileName.length() - 5);
		}
		tableFileName += ".csv";
		return tableFileName;
	}
}
