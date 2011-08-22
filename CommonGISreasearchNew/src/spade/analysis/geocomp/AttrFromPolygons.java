package spade.analysis.geocomp;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.ThematicDataItem;
import spade.vis.dmap.DGeoObject;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.MultiGeometry;
import spade.vis.geometry.RealPoint;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.space.SelectLayer;
import ui.AttributeChooser;

/**
* Calculates attributes of vector objects on the basis of a raster layer
*/
public class AttrFromPolygons extends GeoCalculator {
	static ResourceBundle res = Language.getTextResource("spade.analysis.geocomp.Res");
	/**
	* The function used for generation of attributes
	*/
	public static final String[] functions = { res.getString("Polygon_ID"), res.getString("Polygon_name"), res.getString("Inside_a_polygon"), res.getString("Attributes_from_") };
/*
  protected static final int
    fID     = 0,
    fName   = 1,
    fInside = 2;
*/

	protected OKDialog okd;
	protected DataTable aTable = null; // for area layer
	protected SystemUI ui;
	protected Vector attrs;

	/**
	* Calculates attributes of polygon objects on the basis of a point layer.
	* The arguments are a layer manager (a GeoCalculator must itself care about
	* selection of a layer or layers of appropriate type, in this case a layer
	* with point objects) and SystemUI (to be used for displaying messages and
	* finding an owner frame for dialogs)
	* If calculation was successful, addt the generated attribute to the table
	* attached to the vector layer and returns its identifier. If there was
	* no table yet, creates the table.
	*/
	@Override
	public Object doCalculation(LayerManager lman, ESDACore core) {
		ui = core.getUI();
		//propose the user to select a layer with vector objects
		if (lman == null)
			return null;
		Frame win = null;
		if (ui != null) {
			win = ui.getMainFrame();
		}
		if (win == null) {
			win = CManager.getAnyFrame();
		}
		// following text:"Select VECTOR layer"
		GeoLayer layer = SelectLayer.selectLayer(lman, "P", res.getString("Select_a_POINT_layer"), win);
		if (layer == null)
			return null;
		DataTable table = null;
		if (!layer.hasThematicData()) {
			table = constructTable(layer);
			if (table == null) {
				if (ui != null) {
					// following text:"Cannot construct a table for the layer"
					ui.showMessage(res.getString("Cannot_construct_a"), true);
				}
				return null;
			}
		} else if (!(layer.getThematicData() instanceof DataTable)) {
			// following text:"Illegal table type!
			if (ui != null) {
				ui.showMessage(res.getString("Illegal_table_type_"), true);
			}
			return null;
		} else {
			table = (DataTable) layer.getThematicData();
		}

		boolean otherTable = !layer.hasThematicData(table);
		if (otherTable) {
			layer.receiveThematicData(table);
			if (layer.getObjectFilter() != null) {
				layer.setThematicFilter(table.getObjectFilter());
			}
		}

		//propose the user to select a raster layer
		// following text:"Select RASTER layer"
		GeoLayer aLayer = SelectLayer.selectLayer(lman, "A", res.getString("Select_an_AREA_layer"), win);
		if (aLayer == null)
			return null;

		// forming Panel for calculation dialog
		Panel p = new Panel(new BorderLayout());
		// following text:"Specify parameters for computation:"
		p.add(new Label(res.getString("Specify_parameters")), "North");

		Panel pp = new Panel(new GridLayout(4, 1));
		p.add(pp, "Center");

//    CheckboxGroup gFunc = new CheckboxGroup();
		Checkbox cID = new Checkbox(functions[0], /*gFunc,*/false);
		Checkbox cName = new Checkbox(functions[1], /*gFunc,*/false);
		Checkbox cInside = new Checkbox(functions[2], /*gFunc,*/false);

		Button bAttributes = new Button(functions[3]);
		bAttributes.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
//        if (okd != null) okd.dispose();
				AttributeChooser attrSel = new AttributeChooser();
				attrs = attrSel.selectColumns(aTable, res.getString("Select_attributes"), ui);
			}
		});

		pp.add(cID);
		pp.add(cName);
		pp.add(cInside);
		if (aLayer.hasThematicData() && aLayer.getThematicData() instanceof DataTable) {
			aTable = (DataTable) aLayer.getThematicData();

			if (!aLayer.hasThematicData(aTable)) {
				aLayer.receiveThematicData(aTable);
				if (aLayer.getObjectFilter() != null) {
					aLayer.setThematicFilter(aTable.getObjectFilter());
				}
			}

			pp.add(bAttributes);
		}

		// following text:"Set parameters"
		okd = new OKDialog(win, res.getString("Set_parameters"), true);
		okd.addContent(p);
		okd.show();
		if (okd.wasCancelled())
			return null;
/*
    int func=0;
    if (cID.getState())   func=fID; else
    if (cName.getState()) func=fName; else
                          func=fInside;
*/
		AttrSpec asp = new AttrSpec();
		asp.layer = layer;
		asp.table = table;
		asp.attrIds = new Vector();

		DGeoObject aObject, gobj;
		RealPoint point;
		DataRecord pdata;
		Geometry geom;

		MultiGeometry mg = null;
		boolean multi = false;
		int mgparts = 1;

		boolean inside = false;
		int lastAttr = table.getAttrList().size() - 1;

// for each point
		for (int i = 0; i < layer.getObjectCount(); i++) {
			gobj = (DGeoObject) layer.getObjectAt(i);
			if (gobj == null) {
				continue;
			}
			pdata = (DataRecord) gobj.getData();
			if (pdata == null) {
				continue;
			}
			String value = "";

			if (gobj.getGeometry() instanceof MultiGeometry) {
				mg = (MultiGeometry) gobj.getGeometry();
				multi = true;
				mgparts = mg.getPartsCount();
			} else {
				multi = false;
				mgparts = 1;
			}
			for (int part = 0; part < mgparts; part++) {
				if (multi) {
					point = (RealPoint) mg.getPart(part);
				} else {
					point = (RealPoint) gobj.getGeometry();
				}

//check each polygon
				for (int j = 0; j < aLayer.getObjectCount(); j++) {
					if (ui != null && i % 100 == 0) {
						// following text:"Calculation: object "+(i+1)+" of "+layer.getObjectCount()+"; "+perc+"% ready"
						ui.showMessage(res.getString("Calculation_object") + (i + 1) + res.getString("of") + layer.getObjectCount());//+
					}

					aObject = (DGeoObject) aLayer.getObjectAt(j);
					if (aObject.contains(point.x, point.y, 0)) {
/*
          switch (func) {
            case fID:
              if (!value.equals(", "+aObject.getIdentifier()))
                value+=", "+aObject.getIdentifier();
              break;
            case fName:
              String label = aObject.getLabel();
              if (label != null)
                if (!value.equals(", "+label))
                  value+=", "+label; else;
              else
                if (!value.equals(", "+aObject.getIdentifier()))
                  value+=", "+aObject.getIdentifier();
              break;
            case fInside:
              value="yes";
              break;
            default: break;
          }
*/
						int attrAdded = 0;

						if (cID.getState()) {
							value = aObject.getIdentifier();
							pdata.setAttrValue(value, lastAttr + 1 + attrAdded);
							attrAdded++;
						}
						if (cName.getState()) {
							String label = aObject.getLabel();
							if (label != null) {
								value = label;
							} else {
								value = aObject.getIdentifier();
							}
							pdata.setAttrValue(value, lastAttr + 1 + attrAdded);
							attrAdded++;
						}
						if (cInside.getState()) {
							value = "yes";
							pdata.setAttrValue(value, lastAttr + 1 + attrAdded);
							attrAdded++;
						}
						if (attrs != null) {
							ThematicDataItem rec = aObject.getData();
							for (int a = 0; a < attrs.size(); a++) {
								pdata.setAttrValue(rec.getAttrValue(((Attribute) attrs.elementAt(a)).getIdentifier()), lastAttr + 1 + attrAdded);
								attrAdded++;
							}
						}

						inside = true;
						break;
					}
				}
				if (!inside)
					if (!cInside.getState()) {
						value = "";
					} else {
						value = "no";
					}
				inside = false;
			}
		}
/*
    if (value.startsWith(", ")) value = value.substring(2);
    pdata.addAttrValue(value);

    String attrName;
    switch (func) {
      case fID:     attrName = aLayer.getName() + " " + res.getString("objects") + " ID"; break;
      case fName:   attrName = aLayer.getName() + " " + res.getString("objects") + " " + res.getString("Name"); break;
      case fInside: attrName = res.getString("Inside") + " " + aLayer.getName(); break;
      default: attrName = "####";
    }
*/

		String attrName;
		if (cID.getState()) {
			attrName = aLayer.getName() + " " + res.getString("objects") + " ID";
			int attrN = table.addDerivedAttribute(attrName, AttributeTypes.character/*.real*/, AttributeTypes.compute, null);
			asp.attrIds.addElement(table.getAttributeId(attrN));
		}
		if (cName.getState()) {
			attrName = aLayer.getName() + " " + res.getString("objects") + " " + res.getString("Name");
			int attrN = table.addDerivedAttribute(attrName, AttributeTypes.character/*.real*/, AttributeTypes.compute, null);
			asp.attrIds.addElement(table.getAttributeId(attrN));
		}
		if (cInside.getState()) {
			attrName = res.getString("Inside") + " " + aLayer.getName();
			int attrN = table.addDerivedAttribute(attrName, AttributeTypes.character/*.real*/, AttributeTypes.compute, null);
			asp.attrIds.addElement(table.getAttributeId(attrN));
		}
		if (attrs != null) {
			for (int a = 0; a < attrs.size(); a++) {
				Attribute attr = (Attribute) attrs.elementAt(a);
				int attrN = table.addDerivedAttribute(attr.getName(), attr.getType(), AttributeTypes.compute, null);
				asp.attrIds.addElement(table.getAttributeId(attrN));
			}
		}

//    int attrN=table.addDerivedAttribute(attrName,AttributeTypes.character/*.real*/,AttributeTypes.compute,null);
//    asp.attrIds.addElement(table.getAttributeId(attrN));
		return asp;
	}
}
