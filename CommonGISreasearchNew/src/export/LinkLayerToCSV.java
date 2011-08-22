package export;

import java.io.OutputStream;
import java.util.Vector;

import spade.analysis.system.SystemUI;
import spade.lib.util.CopyFile;
import spade.vis.database.DataTable;
import spade.vis.database.ObjectFilter;
import spade.vis.dmap.DAggregateLinkLayer;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DLinkLayer;
import spade.vis.geometry.Geometry;
import spade.vis.spec.DataSourceSpec;
import spade.vis.spec.LinkDataDescription;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Aug 18, 2010
 * Time: 4:20:20 PM
 * Exports infromation about link layer containing vectors linking objects from another layer
 */
public class LinkLayerToCSV extends TableToCSV implements LayerExporter {
	/**
	* Returns a short text to explain the user what kind of layer may be saved
	* in this format. For example, "vector layer", "raster layer", "point objects"
	*/
	// following string: "point objects only"
	@Override
	public String getDataChar() {
		return "links or moves between places";
	}

	/**
	 * Replies if this exporter is applicable to a layer of the given type and,
	 * possibly, subtype.
	 */
	@Override
	public boolean isApplicable(char layerType, char subType) {
		return layerType == Geometry.line;
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
		if (!(data instanceof DLinkLayer) && !(data instanceof DAggregateLinkLayer)) {
			if (ui != null) {
				ui.showMessage("This is not a layer with links!", true);
			}
			return false;
		}
		DGeoLayer layer = (DGeoLayer) data;
		if (layer.getObjectCount() < 1) {
			// following string: "No data in the layer!"
			if (ui != null) {
				ui.showMessage(res.getString("No_data_in_the_layer_"), true);
			}
			return false;
		}
		DataTable tbl = (DataTable) layer.getThematicData();
		if (tbl == null) {
			if (ui != null) {
				ui.showMessage("The layer must have a table containing link starts and ends!", true);
			}
			return false;
		}
		DGeoLayer placeLayer = null;
		if (layer instanceof DLinkLayer) {
			placeLayer = ((DLinkLayer) layer).getPlaceLayer();
		}
		if (layer instanceof DAggregateLinkLayer) {
			placeLayer = ((DAggregateLinkLayer) layer).getPlaceLayer();
		}
		if (placeLayer == null) {
			if (ui != null) {
				ui.showMessage("The layer is not linked to a layer with places!", true);
			}
			return false;
		}
		DataSourceSpec dss = (DataSourceSpec) placeLayer.getDataSource();
		if (dss == null || dss.source == null || dss.source.equalsIgnoreCase("_derived")) {
			if (ui != null) {
				ui.showMessage("The layer with places has not been stored yet!", true);
			}
			return false;
		}
		DataSourceSpec spec = (DataSourceSpec) layer.getDataSource();
		if (spec == null) {
			spec = (DataSourceSpec) tbl.getDataSource();
		}
		if (spec == null || spec.descriptors == null) {
			if (ui != null) {
				ui.showMessage("No metadata about link starts and ends found!", true);
			}
			return false;
		}
		LinkDataDescription ldd = null;
		for (int i = 0; i < spec.descriptors.size() && ldd == null; i++)
			if (spec.descriptors.elementAt(i) instanceof LinkDataDescription) {
				ldd = (LinkDataDescription) spec.descriptors.elementAt(i);
			}
		if (ldd == null) {
			if (ui != null) {
				ui.showMessage("No metadata about link starts and ends found!", true);
			}
			return false;
		}
		ldd.layerRef = CopyFile.getName(dss.source);
		if (ldd.souColName == null && ldd.souColIdx >= 0) {
			ldd.souColName = tbl.getAttributeName(ldd.souColIdx);
		}
		if (ldd.souColIdx < 0) {
			ldd.souColIdx = tbl.findAttrByName(ldd.souColName);
		}
		if (ldd.destColName == null && ldd.destColIdx >= 0) {
			ldd.destColName = tbl.getAttributeName(ldd.destColIdx);
		}
		if (ldd.destColIdx < 0) {
			ldd.destColIdx = tbl.findAttrByName(ldd.destColName);
		}
		if (ldd.souColIdx < 0 || ldd.destColIdx < 0) {
			if (ui != null) {
				ui.showMessage("No metadata about link starts and ends found!", true);
			}
			return false;
		}
		if (selAttr == null) {
			selAttr = new Vector(5, 5);
		}
		if (!selAttr.contains(tbl.getAttributeId(ldd.destColIdx))) {
			selAttr.insertElementAt(tbl.getAttributeId(ldd.destColIdx), 0);
		}
		if (!selAttr.contains(tbl.getAttributeId(ldd.souColIdx))) {
			selAttr.insertElementAt(tbl.getAttributeId(ldd.souColIdx), 0);
		}
		if (ldd.souTimeColName == null && ldd.souTimeColIdx >= 0) {
			ldd.souTimeColName = tbl.getAttributeName(ldd.souTimeColIdx);
		}
		if (ldd.destTimeColName == null && ldd.destTimeColIdx >= 0) {
			ldd.destTimeColName = tbl.getAttributeName(ldd.destTimeColIdx);
		}
		if (super.storeData(tbl, filter, selAttr, stream, ui)) {
			this.spec = (DataSourceSpec) spec.clone();
			this.spec.toBuildMapLayer = true;
			return true;
		}
		return false;
	}

	@Override
	protected void makeSourceSpecification(Object data, String source) {
		if (!(data instanceof DGeoLayer)) {
			super.makeSourceSpecification(data, source);
			return;
		}
		if (spec == null)
			return;
		DGeoLayer layer = (DGeoLayer) data;
		spec.source = source;
		spec.format = "ASCII";
		spec.delimiter = ",";
		spec.nRowWithFieldNames = 0;
		spec.idFieldName = "id";
		spec.nameFieldName = "Name";
		spec.name = layer.getName();
		if (tRefDescr != null && tRefDescr.size() > 0) {
			if (spec.descriptors == null) {
				spec.descriptors = new Vector(5, 5);
			}
			for (int i = 0; i < tRefDescr.size(); i++) {
				spec.descriptors.addElement(tRefDescr.elementAt(i));
			}
		}
		DataSourceSpec dss = (DataSourceSpec) layer.getDataSource();
		if (dss == null || dss.source == null || dss.source.equalsIgnoreCase("_derived")) {
			layer.setDataSource(spec);
		}
	}
}
