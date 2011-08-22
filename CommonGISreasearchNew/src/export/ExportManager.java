package export;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.analysis.tools.DataAnalyser;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.TextCanvas;
import spade.lib.lang.Language;
import spade.lib.util.CopyFile;
import spade.lib.util.IntArray;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTable;
import spade.vis.database.ObjectFilter;
import spade.vis.dmap.DGeoLayer;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.spec.DataSourceSpec;
import spade.vis.spec.ParamExpert;
import spade.vis.spec.TagReader;
import ui.AttributeChooser;

/**
* Provides access to classes for export of data from Descartes
*/
public class ExportManager implements DataAnalyser {
	static ResourceBundle res = Language.getTextResource("export.Res");
	/**
	* The register of known exporters
	*/
	protected ExporterRegister ereg = new ExporterRegister();
	/**
	* Remembers the last directory where data were saved
	*/
	protected static String lastDir = null;

	/**
	* Returns true when the tool has everything necessary for its operation.
	* In this case the manager checks if at least one export tool is available.
	*/
	@Override
	public boolean isValid(ESDACore core) {
		String tools[] = ereg.getExporterClassNames();
		if (tools == null)
			return false;
		for (String tool : tools) {
			try {
				Class cl = Class.forName(tool);
				if (cl != null)
					return true;
			} catch (Exception e) {
			}
		}
		return false;
	}

	/**
	* Constructs a vector of instances of exporters
	*/
	protected Vector getExporters() {
		String tools[] = ereg.getExporterClassNames();
		if (tools == null)
			return null;
		Vector exp = new Vector(tools.length, 5);
		boolean hasNoNull = false;
		for (int i = 0; i < tools.length; i++) {
			exp.addElement(null);
			try {
				Class cl = Class.forName(tools[i]);
				if (cl != null) {
					exp.setElementAt(cl.newInstance(), i);
					hasNoNull = true;
				}
			} catch (Exception e) {
			}
		}
		if (hasNoNull)
			return exp;
		return null;
	}

	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	@Override
	public void run(ESDACore core) {
		Vector exp = getExporters();
		if (exp == null)
			return;
		//divide into table exporters and layer exporters
		IntArray texp = new IntArray(exp.size(), 5), lexp = new IntArray(exp.size(), 5);
		for (int i = 0; i < exp.size(); i++)
			if (exp.elementAt(i) != null)
				if (exp.elementAt(i) instanceof LayerExporter) {
					lexp.addElement(i);
				} else if (exp.elementAt(i) instanceof DataExporter) {
					texp.addElement(i);
				}
		if (texp.size() < 1 && lexp.size() < 1)
			return;
		//construct a dialog for asking the user what exporter to use
		Panel pan = new Panel(new ColumnLayout());
		Checkbox tcb[] = null, lcb[] = null;
		CheckboxGroup cbg = new CheckboxGroup();
		DataKeeper dk = core.getDataKeeper();
		if (texp.size() > 0 && dk.getTableCount() > 0) {
			// following string: "Export a table in format"
			pan.add(new Label(res.getString("Export_a_table_in")));
			tcb = new Checkbox[texp.size()];
			for (int i = 0; i < texp.size(); i++) {
				DataExporter dexp = (DataExporter) exp.elementAt(texp.elementAt(i));
				tcb[i] = new Checkbox(dexp.getFormatName(), false, cbg);
				pan.add(tcb[i]);
			}
		}
		if (lexp.size() > 0 && dk.getMapCount() > 0) {
			// following string: "Export a map layer in format"
			pan.add(new Label(res.getString("Export_a_map_layer_in")));
			lcb = new Checkbox[lexp.size()];
			for (int i = 0; i < lexp.size(); i++) {
				LayerExporter dexp = (LayerExporter) exp.elementAt(lexp.elementAt(i));
				lcb[i] = new Checkbox(dexp.getFormatName() + ": " + dexp.getDataChar(), false, cbg);
				pan.add(lcb[i]);
			}
		}
		Frame fr = null;
		if (core.getUI() != null) {
			fr = core.getUI().getMainFrame();
		}
		if (fr == null) {
			fr = CManager.getAnyFrame();
		}
		// following string: "Export data"
		OKDialog dia = new OKDialog(fr, res.getString("Export_data"), true);
		dia.addContent(pan);
		dia.show();
		if (dia.wasCancelled())
			return;
		DataExporter dex = null;
		if (tcb != null) {
			for (int i = 0; i < tcb.length && dex == null; i++)
				if (tcb[i].getState()) {
					dex = (DataExporter) exp.elementAt(texp.elementAt(i));
				}
		}
		if (dex == null && lcb != null) {
			for (int i = 0; i < lcb.length && dex == null; i++)
				if (lcb[i].getState()) {
					dex = (DataExporter) exp.elementAt(lexp.elementAt(i));
				}
		}
		if (dex == null)
			return; //the user did not select any exporter
		//select a table or a layer to save
		Object data = null;
		ObjectFilter filter = null;
		AttributeDataPortion tbl = null;
		if (dex instanceof LayerExporter) {
			int mapN = 0;
			if (core.getUI() != null) {
				mapN = core.getUI().getCurrentMapN();
			}
			if (mapN < 0) {
				mapN = 0;
			}
			LayerManager lman = dk.getMap(mapN);
			if (lman == null || lman.getLayerCount() < 1) {
				if (core.getUI() != null) {
					// following string: "No map layers found!"
					core.getUI().showMessage(res.getString("No_map_layers_found_"), true);
				}
				return;
			}
			IntArray lidx = new IntArray(lman.getLayerCount(), 1);
			List lst = new List(5);
			LayerExporter lex = (LayerExporter) dex;
			for (int i = 0; i < lman.getLayerCount(); i++) {
				GeoLayer gl = lman.getGeoLayer(i);
				if (gl.getLayerDrawn() && lex.isApplicable(gl.getType(), gl.getSubtype())) {
					lst.add(gl.getName());
					lidx.addElement(i);
				}
			}
			if (lidx.size() < 1) {
				if (core.getUI() != null) {
					// following string: "No map layers of this type!"
					core.getUI().showMessage(res.getString("No_map_layers_of_this"), true);
				}
				return;
			}
			lst.select(0);
			// following string:"Select the layer to export"
			dia = new OKDialog(fr, res.getString("Select_the_layer_to"), true);
			dia.addContent(lst);
			dia.show();
			if (dia.wasCancelled())
				return;
			int idx = lst.getSelectedIndex();
			GeoLayer gl = lman.getGeoLayer(lidx.elementAt(idx));
			data = gl;
			filter = gl.getObjectFilter();
			tbl = gl.getThematicData();
			if (tbl != null && tbl.getAttrCount() < 1) {
				tbl = null;
			}
			if (tbl != null && !lex.canWriteAttributes())
				if (texp != null && texp.size() > 0) {
					DataExporter tableExp = (DataExporter) exp.elementAt(texp.elementAt(0));
					TextCanvas tc = new TextCanvas();
					// following string:"Do you wish the attributes of the geographical objects to be stored in a separate file in format "..."?"
					tc.addTextLine(res.getString("Do_you_wish_the") + res.getString("to_be_stored_in_a") + tableExp.getFormatName() + "?");
					// following string: "Store attributes?"
					dia = new OKDialog(fr, res.getString("Store_attributes_"), OKDialog.YES_NO_MODE, true);
					dia.addContent(tc);
					dia.show();
					if (dia.wasCancelled()) {
						tbl = null;
					}
				} else {
					tbl = null;
				}
		} else {
			List lst = new List(5);
			for (int i = 0; i < dk.getTableCount(); i++) {
				lst.add(dk.getTable(i).getName());
			}
			lst.select(0);
			// following string: "Select the table to export"
			dia = new OKDialog(fr, res.getString("Select_the_table_to"), true);
			dia.addContent(lst);
			dia.show();
			if (dia.wasCancelled())
				return;
			int idx = lst.getSelectedIndex();
			tbl = dk.getTable(idx);
			data = tbl;
			filter = tbl.getObjectFilter();
		}
		Vector selAttr = null;
		if (tbl != null) {
			//if there are attributes that can be saved, let the user select attributes
			AttributeChooser attrSel = new AttributeChooser();
			if (attrSel.selectColumns(tbl, null, null, false, res.getString("Select_attributes_to"), core.getUI()) != null) {
				selAttr = attrSel.getSelectedColumnIds();
			}
			if (selAttr == null || selAttr.size() < 1)
				return;
		}
		//let the user select the file for saving data
		// following string:"Specify the file to export the data"
//ID
		String name = null;
		if (dex.getFileExtension() != null) {
			FileDialog fd = new FileDialog(fr, res.getString("Specify_the_file_to"));
			if (lastDir != null) {
				fd.setDirectory(lastDir);
			}
			fd.setFile("*." + dex.getFileExtension());
			fd.setMode(FileDialog.SAVE);
			fd.show();
			if (fd.getDirectory() == null)
				return;
			lastDir = fd.getDirectory();
			name = fd.getFile();
			String str = CopyFile.getExtension(name);
			if (str == null || str.length() < 1) {
				name += "." + dex.getFileExtension();
			}
		}
//~ID
		if (!storeData(dex, data, filter, selAttr, lastDir, name, core.getUI()))
			return;
		DataSourceSpec tblSpec = null;
		if (tbl != null) {
			tblSpec = dex.getStoredDataDescriptor();
		}
		if (tbl != null && !dex.canWriteAttributes()) {
			DataExporter tableExp = (DataExporter) exp.elementAt(texp.elementAt(0));
			String tblFileName = CopyFile.getNameWithoutExt(name) + "." + tableExp.getFileExtension();
			if (dex.getFileExtension().equalsIgnoreCase(tableExp.getFileExtension())) {
				tblFileName = CopyFile.getNameWithoutExt(name) + "_data." + tableExp.getFileExtension();
			}
			if ((dex instanceof RecordNumberSaver) && (tableExp instanceof RecordNumberSaver)) {
				RecordNumberSaver rs1 = (RecordNumberSaver) dex, rs2 = (RecordNumberSaver) tableExp;
				rs2.setSaveRecordNumbers(rs1.getSaveRecordNumbers());
				rs2.setRecNumColName(rs1.getRecNumColName());
			}
			storeData(tableExp, tbl, filter, selAttr, lastDir, tblFileName, core.getUI());
			tblSpec = tableExp.getStoredDataDescriptor();
		}
		if (data instanceof AttributeDataPortion) {
			tbl = (AttributeDataPortion) data;
		}
		if (tbl == null)
			return;
		if (tblSpec == null)
			return;
		//save a parameter description of this table
		ParamExpert pexp = null;
		try {
			Object obj = Class.forName("spade.vis.database.CaptionParamProcessor").newInstance();
			if (obj != null && (obj instanceof ParamExpert)) {
				pexp = (ParamExpert) obj;
			}
		} catch (Exception e) {
		}
		if (pexp == null)
			return;
		Vector parDescr = pexp.describeParameters(tbl, selAttr);
		if (parDescr != null) {
			boolean tableHasParamDesc = false;
			if (tblSpec != null && tblSpec.descriptors != null) {
				for (int i = 0; i < tblSpec.descriptors.size() && !tableHasParamDesc; i++) {
					for (int j = 0; j < parDescr.size() && !tableHasParamDesc; j++)
						if (tblSpec.descriptors.elementAt(i).getClass().equals(parDescr.elementAt(j).getClass())) {
							tableHasParamDesc = true;
						}
				}
			}
			if (!tableHasParamDesc && tblSpec != null) {
				if (tblSpec.descriptors == null) {
					tblSpec.descriptors = new Vector(5, 5);
				}
				for (int i = 0; i < parDescr.size(); i++) {
					tblSpec.descriptors.addElement(parDescr.elementAt(i));
				}
			}

			FileOutputStream out = null;
			try {
				out = new FileOutputStream(lastDir + name + ".descr");
			} catch (IOException ioe) {
				if (core.getUI() != null) {
					core.getUI().showMessage(ioe.toString(), true);
				}
			}
			if (out == null)
				return;
			DataOutputStream dout = new DataOutputStream(out);
			for (int i = 0; i < parDescr.size(); i++)
				if (parDescr.elementAt(i) instanceof TagReader) {
					TagReader tr = (TagReader) parDescr.elementAt(i);
					try {
						tr.writeDescription(dout);
					} catch (IOException ioe) {
						if (core.getUI() != null) {
							core.getUI().showMessage(ioe.toString(), true);
						}
						break;
					}
				}
			try {
				out.close();
			} catch (IOException ioe) {
			}
		}
		dk.tableWasExported(tbl, tblSpec);
	}

	protected boolean storeData(DataExporter dex, Object data, ObjectFilter filter, Vector selAttr, String dir, String filename, SystemUI ui) {
		boolean result = dex.storeData(data, filter, selAttr, dir, filename, ui);
		if (result) { //correct the Data Source Specification
			Object sou = null;
			if (data instanceof DataTable) {
				sou = ((DataTable) data).getDataSource();
			} else if (data instanceof DGeoLayer) {
				sou = ((DGeoLayer) data).getDataSource();
			}
			if (sou != null && (sou instanceof DataSourceSpec)) {
				DataSourceSpec spec = (DataSourceSpec) sou;
				if (spec.source == null || spec.source.equalsIgnoreCase("_derived")) {
					spec.source = dir + filename;
				}
			} else {
				DataSourceSpec spec = new DataSourceSpec();
				spec.source = dir + filename;
				spec.format = dex.getFileExtension();
				if (data instanceof DataTable) {
					((DataTable) data).setDataSource(spec);
				} else if (data instanceof DGeoLayer) {
					((DGeoLayer) data).setDataSource(spec);
				}
			}
		}
		return result;
	}
}
