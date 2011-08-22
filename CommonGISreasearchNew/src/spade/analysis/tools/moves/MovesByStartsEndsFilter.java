package spade.analysis.tools.moves;

import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.BaseAnalyser;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.OKFrame;
import spade.lib.util.IdMaker;
import spade.vis.action.Highlighter;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.dmap.DAggregateLinkLayer;
import spade.vis.dmap.DAggregateLinkObject;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DLinkLayer;
import spade.vis.dmap.DLinkObject;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jan 18, 2010
 * Time: 9:51:09 AM
 * To change this template use File | Settings | File Templates.
 */
public class MovesByStartsEndsFilter extends BaseAnalyser implements ActionListener {
	private OKFrame frame = null;

	protected DGeoLayer moveLayer = null;
	protected DGeoLayer areaLayer = null;

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
		//Find instances of DGeoLayer containing movement vectors
		LayerManager lman = core.getUI().getCurrentMapViewer().getLayerManager();
		Vector<DGeoLayer> moveLayers = new Vector<DGeoLayer>(lman.getLayerCount(), 1);
		Vector<DGeoLayer> placeLayers = new Vector<DGeoLayer>(lman.getLayerCount(), 1);
		for (int i = 0; i < lman.getLayerCount(); i++) {
			GeoLayer layer = lman.getGeoLayer(i);
			DGeoLayer pl = null;
			if (layer instanceof DLinkLayer) {
				pl = ((DLinkLayer) layer).getPlaceLayer();
			} else if (layer instanceof DAggregateLinkLayer) {
				pl = ((DAggregateLinkLayer) layer).getPlaceLayer();
			}
			if (pl == null) {
				continue;
			}
			int idx = lman.getIndexOfLayer(pl.getContainerIdentifier());
			if (idx < 0) {
				continue;
			}
			moveLayers.addElement((DGeoLayer) layer);
			placeLayers.addElement(pl);
		}
		if (moveLayers.size() < 1) {
			showMessage("No layers with moves (vectors) found!", true);
			return;
		}
		Panel p = new Panel(new ColumnLayout());
		p.add(new Label("Select the map layer with moves (vectors):"));
		List list = new List(Math.max(moveLayers.size() + 1, 5));
		for (int i = 0; i < moveLayers.size(); i++) {
			list.add(moveLayers.elementAt(i).getName());
		}
		list.select(0);
		p.add(list);
		OKDialog dia = new OKDialog(getFrame(), "Filter moves by starts/ends", true);
		dia.addContent(p);
		dia.show();
		if (dia.wasCancelled())
			return;
		int idx = list.getSelectedIndex();
		if (idx < 0)
			return;
		moveLayer = moveLayers.elementAt(idx);
		areaLayer = placeLayers.elementAt(idx);
		Panel mainP = new Panel(new ColumnLayout());
		mainP.add(new Label("Select the area(s) in the layer", Label.CENTER));
		mainP.add(new Label("\"" + areaLayer.getName() + "\".", Label.CENTER));
		mainP.add(new Label("Press \"OK\" when ready.", Label.CENTER));
		frame = new OKFrame(this, "Select areas", true);
		frame.addContent(mainP);
		frame.start();
	}

	public void doFiltering() {
		// areas have been selected.
		// Filtering trajectories in <moveLayer> by selected objects in <areaLayer>
		Highlighter highlighter = core.getSupervisor().getHighlighter(areaLayer.getEntitySetIdentifier());
		Vector areaIds = highlighter.getSelectedObjects();
		if (areaIds == null && areaIds.size() < 1) {
			showMessage("No areas selected!", true);
			return;
		}
		String areasName = Dialogs.askForStringValue(getFrame(), "Give a name for the selected area(s)", "areas", null, "Name for the area(s)?", false);
		String aNames[] = { "Start in " + areasName, "End in " + areasName, "Start or end in " + areasName };
		aNames = Dialogs.editStringValues(getFrame(), null, aNames, "Edit the names of the new attributes if needed", "Attribute names", true);
		if (aNames == null)
			return;
		DataTable table = (DataTable) moveLayer.getThematicData();
		boolean newTable = table == null;
		if (newTable) {
			table = new DataTable();
			table.setName(moveLayer.getName());
		}
		int idx0 = table.getAttrCount();
		for (String aName : aNames) {
			table.addAttribute(aName, IdMaker.makeId(aName, table), AttributeTypes.logical);
		}
		for (int i = 0; i < moveLayer.getObjectCount(); i++) {
			DGeoObject gobj = moveLayer.getObject(i);
			DGeoObject startNode = null, endNode = null;
			if (gobj instanceof DLinkObject) {
				startNode = ((DLinkObject) gobj).getStartNode();
				endNode = ((DLinkObject) gobj).getEndNode();
			} else if (gobj instanceof DAggregateLinkObject) {
				startNode = ((DAggregateLinkObject) gobj).startNode;
				endNode = ((DAggregateLinkObject) gobj).endNode;
			} else {
				continue;
			}
			boolean start = areaIds.contains(startNode.getIdentifier()), end = areaIds.contains(endNode.getIdentifier());
			DataRecord rec = (DataRecord) gobj.getData();
			if (rec == null) {
				rec = new DataRecord(gobj.getIdentifier(), gobj.getName());
				rec.setTimeReference(gobj.getTimeReference());
				table.addDataRecord(rec);
				gobj.setThematicData(rec);
			}
			rec.setAttrValue((start) ? "true" : "false", idx0);
			rec.setAttrValue((end) ? "true" : "false", idx0 + 1);
			rec.setAttrValue((start || end) ? "true" : "false", idx0 + 2);
		}
		if (newTable) {
			DataLoader dLoader = core.getDataLoader();
			dLoader.setLink(moveLayer, dLoader.addTable(table));
			moveLayer.setThematicFilter(table.getObjectFilter());
			moveLayer.setLinkedToTable(true);
			showMessage("Table " + table.getName() + " has been attached to layer " + moveLayer.getName(), false);
		} else {
			table.makeUniqueAttrIdentifiers();
		}
		showMessage("3 logical attributes have been added to the table \"" + table.getName() + "\"", false);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(frame) && e.getActionCommand().equals("closed"))
			if (!frame.wasCancelled()) {
				doFiltering();
			}
		frame = null;
	}
}
