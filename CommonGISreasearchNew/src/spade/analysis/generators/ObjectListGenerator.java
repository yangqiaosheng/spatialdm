package spade.analysis.generators;

import java.awt.Component;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.plot.ObjectList;
import spade.analysis.system.Supervisor;
import spade.lib.lang.Language;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.ObjectContainer;

public class ObjectListGenerator extends VisGenerator {
	static ResourceBundle res = Language.getTextResource("spade.analysis.generators.Res");

	/*
	* Checks applicability of the method to selected data
	*/
	@Override
	public boolean isApplicable(AttributeDataPortion dataTable, Vector attributes)

	{
		if (dataTable == null) {
			// following text: "No data source provided!"
			err = res.getString("No_data_source");
			return false;
		}
		if (!dataTable.hasData()) {
			dataTable.loadData();
		}
		if (!dataTable.hasData()) {
			// following text: "No data in the data source!"
			err = res.getString("No_data_in_the_data");
			return false;
		}
		return true;
	}

	@Override
	public Component constructDisplay(String methodId, Supervisor sup, AttributeDataPortion dataTable, Vector attributes, Hashtable properties) {
		if (!isApplicable(dataTable, attributes))
			return null;
		ObjectList ol = new ObjectList();
		ol.construct(sup, 20, (ObjectContainer) dataTable);
		// following text: "Object list"
		ol.setName(res.getString("Object_list"));
		ol.setMethodId(methodId);
		ol.setProperties(properties);
		sup.registerTool(ol);
		return ol;
	}

}
