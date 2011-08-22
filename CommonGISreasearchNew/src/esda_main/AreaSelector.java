package esda_main;

import spade.analysis.system.DataLoader;
import spade.lib.util.Parameters;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.ObjectContainer;
import spade.vis.space.GeoLayer;
import core.ObjSelectorHTML;
import core.TerritorySelector;

/**
* Used for selecting territories for analysis in the GIMMI project
*/
public class AreaSelector extends ShowMap {
	/**
	* Processes object selection events and puts information about selected objects
	* into an external HTML form using Javascript functions.
	*/
	protected ObjSelectorHTML osel = null;

	/**
	* Read and set values of system parameters
	*/
	@Override
	protected void setSystemParameters(Parameters parm) {
		super.setSystemParameters(parm);
		//set specific parameters for GIMMI
		parm.setParameter("soil_attr", getParameter("soil_attr"));
		parm.setParameter("climate_attr", getParameter("climate_attr"));
		parm.setParameter("crop_attr", getParameter("crop_attr"));
		parm.setParameter("Data_From", getParameter("Data_From"));
		parm.setParameter("Enable_Object_List", getParameter("Enable_Object_List"));
		parm.setParameter("Show_Bounds", getParameter("Show_Bounds"));
	}

	/**
	* Skips reading system's parameters from the file system.cnf
	*/
	@Override
	protected void readSystemParamsFromFile(Parameters parm) {
	}

	@Override
	protected void constructUI() {
		osel = null;
		super.constructUI();
		if (ui == null || supervisor == null || ui.getDataKeeper() == null)
			return;
		if (ui.getDataKeeper().getMap(0) == null)
			return;
		ui.openMapView(0);
		for (int i = 0; i < ui.getDataKeeper().getTableCount(); i++) {
			AttributeDataPortion t = ui.getDataKeeper().getTable(i);
			if (!t.hasData()) {
				t.loadData();
			}
		}
		ObjectContainer cont = TerritorySelector.findObjContainerForSelection(ui.getDataKeeper(), supervisor.getSystemSettings());
		if (cont == null) {
			System.out.println(">>AS: container for area selection not found !");
			return;
		}
		System.out.println(">>AS: container " + cont.getContainerIdentifier() + " found; create object selector");
		boolean hierarchy = false;
		if (ui.getDataKeeper() instanceof DataLoader) {
			String partOfAttrName = supervisor.getSystemSettings().getParameterAsString("PART_OF_ATTR_NAME"), levelDownAttrName = supervisor.getSystemSettings().getParameterAsString("LEVEL_DOWN_ATTR_NAME");
			if (partOfAttrName != null || levelDownAttrName != null) {
				AttributeDataPortion table = null;
				if (cont instanceof AttributeDataPortion) {
					table = (AttributeDataPortion) cont;
				} else if (cont instanceof GeoLayer) {
					table = ((GeoLayer) cont).getThematicData();
				}
				if (table != null) {
					hierarchy = table.findAttrByName(partOfAttrName) >= 0 || table.findAttrByName(levelDownAttrName) >= 0;
				}
			}
		}
		if (hierarchy) {
			TerritorySelector tsel = new TerritorySelector();
			tsel.setDataLoader((DataLoader) ui.getDataKeeper());
			osel = tsel;
		} else {
			osel = new ObjSelectorHTML();
		}
		osel.setObjectContainer(cont);
		osel.setSupervisor(supervisor);
		osel.setShowBounds(supervisor.getSystemSettings().checkParameterValue("Show_Bounds", "true"));
		osel.prepareToWork();
	}
}
