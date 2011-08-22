package spade.analysis.tools;

import java.awt.BorderLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.lib.basicwin.InputDoublePanel;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.SelectDialog;
import spade.lib.util.IdMaker;
import spade.lib.util.IntArray;
import spade.lib.util.NumRange;
import spade.lib.util.StringUtil;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DVectorGridLayer;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import ui.AttributeChooser;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Sep 28, 2009
 * Time: 2:57:53 PM
 * For a layer having information about the neighbourhood relationships
 * among the objects, does spatial smoothing for a selected numeric attribute
 * (possibly, parameter-dependent).
 */
public class SpatialSmoothing extends BaseAnalyser {
	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	@Override
	public void run(ESDACore core) {
		this.core = core;
		if (core == null || core.getUI() == null || core.getUI().getCurrentMapViewer() == null) {
			showMessage("No map exists!", true);
			return;
		}
		//find suitable map layers:
		//1) the layers must have tables with thematic data
		//2) the layers must have information about the neighbourhood relationships among the objects
		LayerManager lman = core.getUI().getCurrentMapViewer().getLayerManager();
		Vector<DGeoLayer> layers = new Vector(lman.getLayerCount(), 1);
		for (int i = 0; i < lman.getLayerCount(); i++) {
			GeoLayer layer = lman.getGeoLayer(i);
			if (layer.getObjectCount() < 2) {
				continue;
			}
			if (!(layer instanceof DGeoLayer)) {
				continue;
			}
			if (layer.getThematicData() == null) {
				continue;
			}
			DGeoLayer dLayer = (DGeoLayer) layer;
			boolean isGrid = dLayer instanceof DVectorGridLayer;
			if (!isGrid) {
				boolean hasNeiInfo = false;
				for (int j = 0; j < dLayer.getObjectCount() && !hasNeiInfo; j++) {
					DGeoObject gobj = dLayer.getObject(j);
					hasNeiInfo = gobj.neighbours != null && gobj.neighbours.size() > 0;
				}
				if (!hasNeiInfo) {
					continue;
				}
			}
			layers.addElement(dLayer);
		}
		if (layers.size() < 1) {
			showMessage("No suitable layers found!", true);
			return;
		}
		Panel mainP = new Panel(new BorderLayout());
		mainP.add(new Label("Select the layer for the spatial smoothing:"), BorderLayout.NORTH);
		List layerList = new List(Math.max(layers.size() + 1, 5));
		for (int i = 0; i < layers.size(); i++) {
			layerList.add(layers.elementAt(i).getName());
		}
		layerList.select(layerList.getItemCount() - 1);
		mainP.add(layerList, BorderLayout.CENTER);
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Spatial smoothing", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return;
		int idx = layerList.getSelectedIndex();
		if (idx < 0)
			return;
		DGeoLayer layer = layers.elementAt(idx);
		DataTable table = (DataTable) layer.getThematicData();
		AttributeChooser attrSel = new AttributeChooser();
		attrSel.setSelectOnlyOne(true);
		Vector attributes = attrSel.selectTopLevelAttributes(table, null, null, true, "Select the attribute to smooth:", core.getUI());
		if (attributes == null || attributes.size() < 1)
			return;
		Attribute topAttr = (Attribute) attributes.elementAt(0);
		showMessage("Selected: " + topAttr.getName(), false);
		if (topAttr.hasChildren()) {
			attributes = topAttr.getChildren();
		}
		Vector<String> attrIds = new Vector<String>(attributes.size(), 1);
		for (int i = 0; i < attributes.size(); i++) {
			attrIds.addElement(((Attribute) attributes.elementAt(i)).getIdentifier());
		}
		IntArray colNs = table.getRelevantColumnNumbers(attrIds);
		NumRange range = table.getValueRangeInColumns(colNs);
		if (range == null || Double.isNaN(range.minValue)) {
			showMessage("Could not get numeric values from the table!", true);
			return;
		}

		SelectDialog selDia = new SelectDialog(core.getUI().getMainFrame(), "Operation?", "Choose the aggregation operation:");
		selDia.addOption("mean", "mean", true);
		selDia.addOption("sum", "sum", false);
		selDia.addOption("minimum", "min", false);
		selDia.addOption("maximum", "max", false);
		selDia.addSeparator();
		InputDoublePanel upLimitP = new InputDoublePanel("Ignore values greater than", true, range.maxValue, range.minValue, range.maxValue, null);
		selDia.addComponent(upLimitP);
		InputDoublePanel lowLimitP = new InputDoublePanel("Ignore values smaller than", true, range.minValue, range.minValue, range.maxValue, null);
		selDia.addComponent(lowLimitP);
		selDia.show();
		if (selDia.wasCancelled())
			return;
		double lowLimit = Double.NaN, upLimit = Double.NaN;
		if (upLimitP.isSelected()) {
			upLimit = upLimitP.getEnteredValue();
		}
		if (lowLimitP.isSelected()) {
			lowLimit = lowLimitP.getEnteredValue();
		}
		String oper = selDia.getSelectedOptionId();

		String aName = topAttr.getName() + ": " + selDia.getSelectedOptionName() + " among neighbours";
/*
    aName= Dialogs.askForStringValue(core.getUI().getMainFrame(),"Attribute name?",aName,
      "A new attribute will be added to the table \""+table.getName()+"\"","New attribute",true);
*/
		if (aName == null)
			return;
		int idx0 = table.getAttrCount();
		if (attributes.size() == 1) {
			//make a single column
			table.addAttribute(aName, IdMaker.makeId(idx0 + "_" + oper + " " + aName, table), AttributeTypes.real);
		} else {
			//make a parameter-dependent attribute
			Attribute attrParent = new Attribute(IdMaker.makeId(idx0 + "_" + oper + " " + aName, table), AttributeTypes.real);
			attrParent.setName(aName);
			String prefix = attrParent.getIdentifier() + "_";
			for (int i = 0; i < attributes.size(); i++) {
				Attribute child0 = (Attribute) attributes.elementAt(i);
				Attribute child = new Attribute(prefix + String.valueOf(idx0 + i), attrParent.getType());
				for (int j = 0; j < child0.getParameterCount(); j++) {
					child.addParamValPair(child0.getParamValPair(j));
				}
				attrParent.addChild(child);
				table.addAttribute(child);
			}
		}
		int prec = StringUtil.getPreferredPrecision(range.minValue, range.minValue, range.maxValue);
		Vector<DGeoObject> neiObj = new Vector<DGeoObject>(20, 10);
		boolean isGrid = layer instanceof DVectorGridLayer;
		if (isGrid) {
			DVectorGridLayer grid = (DVectorGridLayer) layer;
			for (int nr = 0; nr < grid.getNRows(); nr++) {
				for (int nc = 0; nc < grid.getNCols(); nc++) {
					DGeoObject gobj = grid.getObject(nr, nc);
					if (gobj.neighbours != null && gobj.neighbours.size() > 0) {
						continue;
					}
					if (gobj.neighbours == null) {
						gobj.neighbours = new Vector(8, 10);
					}
					int r1 = Math.max(nr - 1, 0), r2 = Math.min(nr + 1, grid.getNRows() - 1), c1 = Math.max(nc - 1, 0), c2 = Math.min(nc + 1, grid.getNCols() - 1);
					for (int r = r1; r <= r2; r++) {
						for (int c = c1; c <= c2; c++)
							if (r != nr || c != nc) {
								DGeoObject nei = grid.getObject(r, c);
								if (nei != null) {
									gobj.neighbours.addElement(nei.getIdentifier());
								}
							}
					}
				}
			}
		}

		for (int na = 0; na < layer.getObjectCount(); na++)
			if (layer.isObjectActive(na)) {
				DGeoObject gobj = layer.getObject(na);
				DataRecord rec = (DataRecord) gobj.getData();
				if (rec == null) {
					continue;
				}
				neiObj.removeAllElements();
				if (gobj.neighbours != null) {
					for (int i = 0; i < gobj.neighbours.size(); i++) {
						int nIdx = layer.getObjectIndex(gobj.neighbours.elementAt(i).toString());
						if (!layer.isObjectActive(nIdx)) {
							continue;
						}
						DGeoObject nei = layer.getObject(nIdx);
						if (nei != null && nei.getData() != null) {
							neiObj.addElement(nei);
						}
					}
				}
				for (int j = 0; j < colNs.size(); j++) {
					int cN = colNs.elementAt(j);
					double value = rec.getNumericAttrValue(cN);
					if (!Double.isNaN(value) && !Double.isNaN(upLimit) && value > upLimit) {
						value = Double.NaN;
					}
					if (!Double.isNaN(value) && !Double.isNaN(lowLimit) && value < lowLimit) {
						value = Double.NaN;
					}
					int nNei = 0;
					if (!Double.isNaN(value)) {
						++nNei;
					}
					for (int i = 0; i < neiObj.size(); i++) {
						DataRecord recNei = (DataRecord) neiObj.elementAt(i).getData();
						double neiVal = recNei.getNumericAttrValue(cN);
						if (!Double.isNaN(neiVal) && !Double.isNaN(upLimit) && neiVal > upLimit) {
							neiVal = Double.NaN;
						}
						if (!Double.isNaN(neiVal) && !Double.isNaN(lowLimit) && neiVal < lowLimit) {
							neiVal = Double.NaN;
						}
						if (Double.isNaN(neiVal)) {
							continue;
						}
						++nNei;
						if (Double.isNaN(value)) {
							value = neiVal;
						} else if (oper.equals("mean") || oper.equals("sum")) {
							value += neiVal;
						} else if (oper.equals("min")) {
							if (value > neiVal) {
								value = neiVal;
							}
						} else if (oper.equals("max")) {
							if (value < neiVal) {
								value = neiVal;
							}
						}
					}
					if (Double.isNaN(value)) {
						continue;
					}
					if (oper.equals("mean")) {
						value /= nNei;
					}
					rec.setNumericAttrValue(value, StringUtil.doubleToStr(value, prec), idx0 + j);
				}
			}
		table.makeUniqueAttrIdentifiers();
		showMessage("Spatial smoothing completed.", false);
	}
}
