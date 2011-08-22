package spade.analysis.util;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.StringUtil;
import spade.vis.action.Highlighter;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.geometry.Geometry;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Sep 24, 2009
 * Time: 2:43:48 PM
 */
public class GeoObjectsSelector {
	public static DGeoLayer lastSelectedLayer = null;

	/**
	 * Asks the user to select a layer with geo objects of the specified type.
	 * Asks the user whether only currently selected objects should be used.
	 * Accounts for the filter!
	 * @param sup the system's supervisor
	 * @param objType - Geometry.point, Geometry.area, Geometry.line, etc.
	 *   If 0, ignored (any types are accepted)
	 * @return a vector of DGeoObjects
	 */
	public static Vector<DGeoObject> selectGeoObjects(Supervisor sup, char objType, String header) {
		if (sup == null || sup.getUI() == null)
			return null;
		if (sup.getUI().getCurrentMapViewer() == null || sup.getUI().getCurrentMapViewer().getLayerManager() == null) {
			sup.getUI().showMessage("No map exists!", true);
			return null;
		}
		LayerManager lman = sup.getUI().getCurrentMapViewer().getLayerManager();
		Vector<DGeoLayer> layers = new Vector<DGeoLayer>(lman.getLayerCount(), 1);
		for (int i = 0; i < lman.getLayerCount(); i++) {
			GeoLayer layer = lman.getGeoLayer(i);
			if (!layer.getLayerDrawn()) {
				continue;
			}
			if (layer.getObjectCount() < 1) {
				continue;
			}
			if (!(layer instanceof DGeoLayer)) {
				continue;
			}
			if (objType == 0 || objType == Geometry.undefined || layer.getType() == objType) {
				layers.addElement((DGeoLayer) layer);
			}
		}
		if (layers.size() < 1) {
			sup.getUI().showMessage("No suitable layers found!", true);
			return null;
		}
		Panel mainP = new Panel(new BorderLayout());
		if (header != null) {
			mainP.add(new Label(header), BorderLayout.NORTH);
		}
		List layerList = new List(Math.max(layers.size() + 1, 5));
		for (int i = 0; i < layers.size(); i++) {
			layerList.add(layers.elementAt(i).getName());
		}
		layerList.select(layerList.getItemCount() - 1);
		mainP.add(layerList, BorderLayout.CENTER);
		Checkbox getSelCB = new Checkbox("take only the selected objects", false);
		mainP.add(getSelCB, BorderLayout.SOUTH);
		OKDialog dia = new OKDialog(sup.getUI().getMainFrame(), header, true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return null;
		int idx = layerList.getSelectedIndex();
		if (idx < 0)
			return null;
		DGeoLayer layer = layers.elementAt(idx);
		boolean onlySelected = getSelCB.getState();
		Vector selIds = null;
		if (onlySelected) {
			Highlighter hl = sup.getHighlighter(layer.getEntitySetIdentifier());
			if (hl != null) {
				selIds = hl.getSelectedObjects();
				if (selIds != null && selIds.size() < 1) {
					selIds = null;
				}
			}
			if (selIds == null && !Dialogs.askYesOrNo(sup.getUI().getMainFrame(), "There are no selected objects in the layer \"" + layer.getName() + "\". Do you want to use ALL currently active (after filtering) objects?", "No selected objects!"))
				return null;
		}
		Vector<DGeoObject> objects = new Vector<DGeoObject>((selIds != null) ? selIds.size() : layer.getObjectCount(), 1);
		for (int nob = 0; nob < layer.getObjectCount(); nob++)
			if (layer.isObjectActive(nob)) {
				DGeoObject gobj = layer.getObject(nob);
				if (selIds == null || StringUtil.isStringInVectorIgnoreCase(gobj.getIdentifier(), selIds)) {
					objects.addElement(gobj);
				}
			}
		if (objects.size() < 1) {
			sup.getUI().showMessage("No objects found!", true);
			return null;
		}
		objects.trimToSize();
		lastSelectedLayer = layer;
		return objects;
	}
}
