package spade.analysis.tools.moves;

import java.awt.Choice;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.system.Processor;
import spade.analysis.tools.DataAnalyser;
import spade.analysis.tools.clustering.ClustersInfo;
import spade.analysis.tools.clustering.ObjectToClusterAssignment;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DMovingObject;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jan 7, 2009
 * Time: 3:59:27 PM
 * Applies a processor of trajectories to a selected layer with trajectories.
 */
public class ApplyTrajectoriesProcessor implements DataAnalyser {
	protected ESDACore core = null;

	/**
	 * Returns true when the tool has everything necessary for its operation.
	 * For example, if the tool manages a number of analysis methods, it should
	 * check whether the class for at least one method is available.
	 * A TrajectoriesGeneraliser always returns true.
	 */
	@Override
	public boolean isValid(ESDACore core) {
		return true;
	}

	/**
	 * This method constructs and starts the tool. Everything the tool may need
	 * for integration with other components of the system can be received
	 * from the system's core passed as an argument.
	 */
	@Override
	public void run(ESDACore core) {
		if (core == null || core.getUI() == null)
			return;
		this.core = core;
		if (core.getUI().getCurrentMapViewer() == null || core.getUI().getCurrentMapViewer().getLayerManager() == null) {
			showMessage("No map exists!", true);
			return;
		}
		//Find instances of DGeoLayer containing trajectories
		LayerManager lman = core.getUI().getCurrentMapViewer().getLayerManager();
		Vector moveLayers = new Vector(lman.getLayerCount(), 1);
		for (int i = 0; i < lman.getLayerCount(); i++) {
			GeoLayer layer = lman.getGeoLayer(i);
			if ((layer instanceof DGeoLayer) && layer.getObjectCount() > 0 && (layer.getObjectAt(0) instanceof DMovingObject)) {
				moveLayers.addElement(layer);
			}
		}
		if (moveLayers.size() < 1) {
			showMessage("No layers with trajectories found!", true);
			return;
		}
		Vector vp = core.getProcessorsForObjectType(Processor.GEO_TRAJECTORY);
		if (vp == null) {
			showMessage("No suitable processors for trajectories", true);
			return;
		}
		Panel mainP = new Panel(new ColumnLayout());
		mainP.add(new Label("Select the layer with trajectories:"));
		List list = new List(Math.max(moveLayers.size() + 1, 5));
		for (int i = 0; i < moveLayers.size(); i++) {
			list.add(((DGeoLayer) moveLayers.elementAt(i)).getName());
		}
		list.select(0);
		mainP.add(list);
		mainP.add(new Label("Apply processor:"));
		Choice chProcessors = new Choice();
		for (int i = 0; i < vp.size(); i++) {
			Processor pr = (Processor) vp.elementAt(i);
			chProcessors.addItem(pr.getName());
		}
		chProcessors.select(vp.size() - 1);
		mainP.add(chProcessors);
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Processing of trajectories", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return;
		int idx = list.getSelectedIndex();
		if (idx < 0)
			return;
		DGeoLayer moveLayer = (DGeoLayer) moveLayers.elementAt(idx);
		Processor processor = (Processor) vp.elementAt(chProcessors.getSelectedIndex());
		processor.initialise(core);
		processor.createUI();
		Vector results = new Vector(moveLayer.getObjectCount(), 10);
		long t0 = System.currentTimeMillis();
		for (int i = 0; i < moveLayer.getObjectCount(); i++) {
			Object res = processor.processObject(moveLayer.getObject(i));
			if (res != null) {
				results.addElement(res);
			} else {
				showMessage("Failed to process object " + moveLayer.getObjectId(i), true);
			}
		}
		long t = System.currentTimeMillis() - t0;
		showMessage("Processing finished; elapsed time " + t + " msec, " + (t / 1000) + " sec", false);
		System.out.println("Processing finished; elapsed time " + t + " msec, " + (t / 1000) + " sec");
		Object prRes = processor.getResult();
		processor.closeUI();
		if (prRes == null && results.size() < 1) {
			showMessage("No results of the processing obtained!", true);
			return;
		}
		if (results.size() > 0) {
			if (results.elementAt(0) instanceof ObjectToClusterAssignment) {
				showMessage(results.size() + " objects have been assigned to clusters!", false);
				DataTable table = null;
				boolean newTable = false;
				if (moveLayer.getThematicData() != null && (moveLayer.getThematicData() instanceof DataTable)) {
					table = (DataTable) moveLayer.getThematicData();
				} else {
					String tblName = moveLayer.getName();
/*
          tblName=Dialogs.askForStringValue(core.getUI().getMainFrame(),"Table name?",
                           tblName,
                           "A new table will be created and attached to the layer",
                           "New table",true);
          if (tblName==null) return;
*/
					table = new DataTable();
					table.setName(tblName);
					for (int i = 0; i < moveLayer.getObjectCount(); i++) {
						DGeoObject gobj = moveLayer.getObject(i);
						DataRecord rec = new DataRecord(gobj.getIdentifier(), gobj.getName());
						rec.setTimeReference(gobj.getTimeReference());
						table.addDataRecord(rec);
						gobj.setThematicData(rec);
					}
				}
				Panel p = new Panel(new ColumnLayout());
				p.add(new Label("Three new attributes will be produced in the table " + table.getName(), Label.CENTER));
				p.add(new Label("Edit the names of the attributes and the parameter if needed.", Label.CENTER));
				p.add(new Label("Attribute names:"));
				String a1Name = "Cluster N (classification)", a2Name = "Specimen N", a3Name = "Distance";
				TextField a1tf = new TextField(a1Name), a2tf = new TextField(a2Name), a3tf = new TextField(a3Name);
				p.add(a1tf);
				p.add(a2tf);
				p.add(a3tf);
				dia = new OKDialog(core.getUI().getMainFrame(), "New attributes", false);
				dia.addContent(p);
				dia.show();
				String str = a1tf.getText();
				if (str != null && str.trim().length() > 0) {
					a1Name = str.trim();
				}
				str = a2tf.getText();
				if (str != null && str.trim().length() > 0) {
					a2Name = str.trim();
				}
				str = a3tf.getText();
				if (str != null && str.trim().length() > 0) {
					a3Name = str.trim();
				}
				Attribute attr = new Attribute("_clusters_" + (table.getAttrCount() + 1), AttributeTypes.character);
				attr.setName(a1Name);
				if (prRes != null && (prRes instanceof ClustersInfo)) {
					ClustersInfo clustersInfo = (ClustersInfo) prRes;
					if (clustersInfo.table != null && clustersInfo.clustersColN >= 0) {
						Attribute clAttr = clustersInfo.table.getAttribute(clustersInfo.clustersColN);
						attr.setValueListAndColors(clAttr.getValueList(), clAttr.getValueColors());
					}
				}
				table.addAttribute(attr);
				int colN = table.getAttrCount() - 1;
				attr = new Attribute("_specimens_" + (table.getAttrCount() + 1), AttributeTypes.character);
				attr.setName(a2Name);
				table.addAttribute(attr);
				attr = new Attribute("_distances_" + (table.getAttrCount() + 1), AttributeTypes.real);
				attr.setName(a3Name);
				table.addAttribute(attr);

				for (int i = 0; i < results.size(); i++) {
					ObjectToClusterAssignment oclas = (ObjectToClusterAssignment) results.elementAt(i);
					//System.out.println("Object <"+oclas.id+">: cluster "+oclas.clusterN+
					//"; specimen "+oclas.specimenIdx+"; distance = "+oclas.distance);
					int objIdx = table.indexOf(oclas.id);
					if (objIdx >= 0) {
						DataRecord rec = table.getDataRecord(objIdx);
						if (oclas.clusterN < 0) {
							rec.setAttrValue("noise", colN);
						} else {
							rec.setAttrValue(String.valueOf(oclas.clusterN), colN);
							rec.setAttrValue(String.valueOf(oclas.specimenIdx), colN + 1);
							rec.setNumericAttrValue(oclas.distance, String.valueOf(oclas.distance), colN + 2);
						}
					}
				}
				if (newTable) {
					core.getDataLoader().addTable(table);
					core.getDataLoader().processTimeReferencedObjectSet(table);
				}
				showMessage("The results have been stored to the table", false);
			}
		}
	}

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
	}
}
