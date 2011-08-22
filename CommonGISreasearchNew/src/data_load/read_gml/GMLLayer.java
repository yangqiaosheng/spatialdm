package data_load.read_gml;

import java.util.ArrayList;
import java.util.Hashtable;

import spade.vis.database.Attribute;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.LayerData;
import spade.vis.database.SpatialEntity;
import spade.vis.database.ThematicDataItem;

public class GMLLayer {
	protected String typeName = null;
	protected LayerData layerData = new LayerData();
	protected ArrayList attrList = new ArrayList();
	protected ArrayList recordList = new ArrayList();

	public GMLLayer(String typeName) {
		this.typeName = typeName;
	}

	public String getTypeName() {
		return typeName;
	}

	public SpatialEntity addSpatialEntity() {
		SpatialEntity spe = new SpatialEntity(String.valueOf(layerData.getDataItemCount()));
		layerData.addItemSimple(spe);
		return spe;
	}

	public void addAttribute(String name, char type) {
		if (!hasAttribute(name)) {
			attrList.add(new Attribute(name, type));
		}
	}

	public LayerData getLayerData() {
		return layerData;
	}

	public boolean hasAttribute(String name) {
		for (int i = attrList.size() - 1; i >= 0; i--)
			if (((Attribute) attrList.get(i)).getName().equals(name))
				return true;
		return false;
	}

	public void addAttributeValue(String name, Object value) {
		((Hashtable) recordList.get(recordList.size() - 1)).put(name, value);
	}

	public void linkGeoObjectsToTableRecords(DataTable table) {
		if (table == null || !table.hasData() || layerData == null || layerData.getDataItemCount() < 1)
			return;
		for (int i = layerData.getDataItemCount() - 1; i >= 0; i--) {
			SpatialEntity spe = (SpatialEntity) layerData.getDataItem(i);
			ThematicDataItem tdi = table.getThematicData(spe.getId());
			spe.setThematicData(tdi);
		}
	}

	public boolean fillDataTable(DataTable table) {
		if (attrList.size() == 0)
			return false;
		for (int i = 0; i < attrList.size(); i++) {
			table.addAttribute((Attribute) attrList.get(i));
		}
		for (int i = 0; i < recordList.size(); i++) {
			DataRecord dr = new DataRecord(String.valueOf(i));
			Hashtable vtab = (Hashtable) recordList.get(i);
			for (int j = 0; j < attrList.size(); j++) {
				dr.addAttrValue(vtab.get(((Attribute) attrList.get(j)).getName()));
			}
			table.addDataRecord(dr);
		}
		table.determineAttributeTypes();
		if (table.hasData()) {
			table.finishedDataLoading();
		}
		return true;
	}
}