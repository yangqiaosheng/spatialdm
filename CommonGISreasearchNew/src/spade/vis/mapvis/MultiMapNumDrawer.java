package spade.vis.mapvis;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Hashtable;
import java.util.Vector;

import spade.analysis.transform.AttributeTransformer;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.StringInRectangle;
import spade.lib.color.ColorSelDialog;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTable;
import spade.vis.database.ThematicDataItem;

/**
* Represents several numeric attributes on "small multiples" using in the
* individual maps a method suitable for a single numeric attribute, e.g.,
* choropleth map, standalone bars, or circles. This visualization method is
* applicable to comparable attributes.
*/

public class MultiMapNumDrawer extends MultiNumberDrawer implements MultiMapVisualizer {
	/**
	* The visualizer used for the individual maps. This must be a descendant of
	* the class NumberDrawer. By default, this ia a NumValuePainter.
	*/
	protected NumberDrawer drawer = null;
	/**
	* Current map number (the method getPresentation is called for this map)
	*/
	protected int currMapIdx = 0;

	/**
	* Sets a reference to the table with the data to visualize.
	*/
	@Override
	public void setDataSource(AttributeDataPortion table) {
		this.table = table;
		if (drawer != null) {
			drawer.setDataSource(table);
		}
	}

	/**
	* Connects the DataPresenter to a transformer of attribute values. After this,
	* it must represent transformed attribute values. The argument listenChanges
	* determines whether the visualizer will listen to the changes of the transformed
	* values and appropriately reset itself.
	*/
	@Override
	public void setAttributeTransformer(AttributeTransformer transformer, boolean listenChanges) {
		super.setAttributeTransformer(transformer, listenChanges);
		if (drawer != null) {
			drawer.setAttributeTransformer(transformer, false);
		}
	}

	/**
	* Informs whether this visualizer allows each of the visualized attributes to
	* be transformed individually (returns true) or requires all attributes to
	* be transformed in the same way (returns false). Returns false because
	* the attributes must be comparable.
	*/
	@Override
	public boolean getAllowTransformIndividually() {
		return false;
	}

	/**
	* The Visualizer sets its parameters. For this purpose it may need
	* data statistics. Statistics are received from a StatisticsProvider.
	* A reference to a StatisticsProvider may be received from the DataInformer.
	*/
	@Override
	public void setup() {
		dataMIN = Float.NaN;
		dataMAX = Float.NaN;
		super.setup();
		if (drawer != null) {
			drawer.setDataMinMax(dataMIN, dataMAX);
			drawer.setFocuserMinMax(dataMIN, dataMAX);
			drawer.setCmp(cmpTo);
		}
	}

	/**
	* Destroys the visualizer when it is no more used.
	*/
	@Override
	public void destroy() {
		if (drawer != null) {
			drawer.destroy();
		}
		super.destroy();
	}

	/**
	 * Should return true if the visualization changes.
	 * The argument "valuesTransformed" indicates whether the values have been
	 * transformed (e.g. by an attribute transformer), which means that the
	 * value range might completely change. In this case, the visualiser may
	 * need to reset its parameters. Otherwise, a slight adaptation may be
	 * sufficient, if needed at all.
	 */
	@Override
	public boolean adjustToDataChange(boolean valuesTransformed) {
		boolean adjusted = super.adjustToDataChange(valuesTransformed);
		if (adjusted && drawer != null) {
			drawer.setDataMinMax(dataMIN, dataMAX);
			drawer.setFocuserMinMax(dataMIN, dataMAX);
			drawer.setCmp(cmpTo);
		}
		return adjusted;
	}

	@Override
	public void setCmp(double cmpTo) {
		this.cmpTo = cmpTo;
		if (drawer != null) {
			drawer.setCmp(cmpTo);
			notifyVisChange();
		}
	}

	@Override
	public void setFocuserMinMax(double focuserMin, double focuserMax) {
		if (drawer != null) {
			drawer.setFocuserMinMax(focuserMin, focuserMax);
			notifyVisChange();
		}
	}

	/**
	* Returns the required number of maps ("small multiples"), depending on the
	* number of attributes to be represented.
	*/
	@Override
	public int getNIndMaps() {
		if (attr == null)
			return 0;
		return attr.size();
	}

	/**
	* Returns the name of the map with the given index in the "small multiples".
	* This may be, for example, the name of the attribute represented by this map
	* or the date this map refers to.
	*/
	@Override
	public String getIndMapName(int idx) {
		return getAttrName(idx);
	}

	/**
	* Sets the number of the current individual map. Since this moment all
	* calls of the "getPresentation" method will relate to this map.
	*/
	@Override
	public void setCurrentMapIndex(int idx) {
		if (attr == null || idx < 0 || idx >= getNIndMaps())
			return;
		if (drawer != null && (drawer.getAttributes() == null || idx != currMapIdx)) {
			Vector attrIds = new Vector(1, 1), attrNames = new Vector(1, 1);
			attrIds.addElement(attr.elementAt(idx));
			attrNames.addElement(getAttrName(idx));
			drawer.setAttributes(attrIds, attrNames);
			if (subAttr != null && subAttr.size() > idx && subAttr.elementAt(idx) != null) {
				drawer.setSubAttributes((Vector) subAttr.elementAt(idx), 0);
			} else {
				drawer.setSubAttributes(null, 0);
			}
			if (invariants != null && invariants.size() > idx && invariants.elementAt(idx) != null) {
				drawer.setInvariant((String) invariants.elementAt(idx), 0);
			} else {
				drawer.setInvariant(null, 0);
			}
		}
		currMapIdx = idx;
		if (drawer != null) {
			drawer.setCurrentSubAttrIndex(subAttrIdx);
		}
	}

	/**
	* Returns the number of the current individual map.
	*/
	@Override
	public int getCurrentMapIndex() {
		return currMapIdx;
	}

	/**
	* Sets the index of the subattribute to be currently taken for the
	* visualization.
	*/
	@Override
	public void setCurrentSubAttrIndex(int idx) {
		super.setCurrentSubAttrIndex(idx);
		if (drawer != null) {
			drawer.setCurrentSubAttrIndex(subAttrIdx);
		}
	}

	/**
	* Draws the part of the legend explaining this specific presentation method.
	*/
	@Override
	public Rectangle drawMethodSpecificLegend(Graphics g, int startY, int leftmarg, int prefW) {
		g.setColor(Color.black);
		Rectangle r = new Rectangle(leftmarg, startY, 0, 0);
		int y = startY;
		if (attr != null) {
			for (int i = 0; i < attr.size(); i++) {
				Point p = StringInRectangle.drawText(g, getAttrName(i), leftmarg, y, prefW, true);
				y = p.y + 3;
				p.x -= leftmarg;
				if (r.width < p.x) {
					r.width = p.x;
				}
			}
		}
		if (drawer != null) {
			Rectangle r1 = drawer.showNumberEncoding(g, y, leftmarg, prefW);
			y += r1.height;
			if (r.width < r1.width) {
				r.width = r1.width;
			}
		}
		r.height = y - startY;
		return r;
	}

	/**
	* This method is called by a GeoObject. The GeoObject sends its data
	* to the Visualizer, and the Visualizer returns an Object specifying the
	* presentation of data contained in this DataItem. This may be, in
	* particular, Color or Diagram.
	* This visualizer passes the data to the visualizer used for the individual
	* maps in the multi-map presentation. The current map number must be set
	* using the method setCurrentMapIndex.
	*/
	@Override
	public Object getPresentation(ThematicDataItem dit) {
		if (drawer == null || dit == null)
			return null;
		if (sdController != null && !sdController.mustDrawObject(dit.getId()))
			return null;
		return drawer.getPresentation(dit);
	}

	/**
	* Returns the visualizer used for drawing the individual maps.
	*/
	public Visualizer getSingleMapVisualizer() {
		return drawer;
	}

	/**
	* Checks semantic applicability of this visualization method to the
	* attributes previously set in the visualizer. Uses the DataInformer to
	* get semantic information about the attributes. Returns true if the
	* attributes are comparable.
	*/
	@Override
	public boolean checkSemantics() {
		err = null;
		if (attr == null || attr.size() < 2)
			return true;
		if (table == null) {
			err = errors[1];
			return false;
		}
		if (!(table instanceof DataTable)) {
			err = errors[11];
			return false;
		}
		DataTable dt = (DataTable) table;
		if (dt.getSemanticsManager() == null) {
			err = errors[11];
			return false;
		}
		boolean result = dt.getSemanticsManager().areAttributesComparable(attr, visName);
		// following string: "The attributes are not comparable!"
		if (!result) {
			err = res.getString("The_attributes_are1");
		}
		return result;
	}

	/**
	* Draws a small picture symbolizing this presentation technique
	*/
	@Override
	public void drawIcon(Graphics g, int x, int y, int w, int h) {
		int rw = w / 2 - 3;
		g.setColor(Color.yellow.darker());
		g.fillRect(x, y, rw, h);
		g.fillRect(x + rw + 3, y, rw, h);
		g.setColor(Color.black);
		g.drawRect(x, y, rw, h);
		g.drawRect(x + rw + 3, y, rw, h);
	}

	/**
	* This method informs whether the Visualizer produces diagrams.
	* This is important for defining the order of drawing of GeoLayers on the
	* map: the diagrams should be drawn on top of all geography.
	* The result of this method depends on which visualizer is used for the
	* individual maps.
	*/
	@Override
	public boolean isDiagramPresentation() {
		if (drawer != null)
			return drawer.isDiagramPresentation();
		return false;
	}

	/**
	* Returns the hue used for values above the reference value in the visual
	* comparison
	*/
	public float getPositiveHue() {
		if (drawer == null)
			return 0f;
		return drawer.getPositiveHue();
	}

	/**
	* Returns the hue used for values below the reference value in the visual
	* comparison
	*/
	public float getNegativeHue() {
		if (drawer == null)
			return 0.7f;
		return drawer.getNegativeHue();
	}

	/**
	* Sets the color hues to be used for representing values above and below the
	* reference value
	*/
	public void setColors(float posHue, float negHue) {
		if (drawer != null) {
			drawer.posHue = posHue;
			drawer.negHue = negHue;
			notifyVisChange();
		}
	}

	/**
	* Returns true if uses different shades to represent differences between
	* attribute values and the reference value
	*/
	public boolean usesShades() {
		return drawer != null && drawer.usesShades();
	}

	/**
	* Sets the reference value for the visual comparison
	*/
	public void setCmp(float cmpTo) {
		this.cmpTo = cmpTo;
		if (drawer != null) {
			drawer.setCmp(cmpTo);
			notifyVisChange();
		}
	}

	/**
	* Sets the focus interval. Values outside this interval are not represented
	* on the map.
	*/
	public void setFocuserMinMax(float focuserMin, float focuserMax) {
		this.focuserMIN = focuserMin;
		this.focuserMAX = focuserMax;
		if (drawer != null) {
			drawer.setFocuserMinMax(focuserMin, focuserMax);
			notifyVisChange();
		}
	}

	/**
	* Replies whether the color scale used by this visualization method may be
	* changed. This does not include interactive analytical manipulation.
	* Returns true.
	*/
	@Override
	public boolean canChangeColors() {
		return true;
	}

	/**
	* Constructs and displays a dialog for changing color hues.
	*/
	@Override
	public void startChangeColors() {
		float hues[] = new float[2];
		hues[0] = drawer.posHue;
		hues[1] = drawer.negHue;
		String prompts[] = new String[2];
		// following text:"Positive color ?"
		prompts[0] = res.getString("Positive_color_");
		// following text:"Negative color ?"
		prompts[1] = res.getString("Negative_color_");
		ColorSelDialog csd = new ColorSelDialog(2, hues, null, prompts, true, usesShades());
		OKDialog okd = new OKDialog(CManager.getAnyFrame(), csd.getName(), true);
		okd.addContent(csd);
		okd.show();
		if (okd.wasCancelled())
			return;
		drawer.setColors(csd.getHueForItem(0), csd.getHueForItem(1));
		notifyVisChange();
	}

//ID

	@Override
	public Hashtable getVisProperties() {
		Hashtable param = null;
		try {
			param = drawer.getVisProperties();
		} catch (Exception ex) {
		}
//    param = super.getVisProperties();
		if (param == null) {
			param = new Hashtable();
		}

		param.put("focuserMin", String.valueOf(drawer.getFocuserMin()));
		param.put("focuserMax", String.valueOf(drawer.getFocuserMax()));

		param.put("cmpTo", String.valueOf(cmpTo));
		param.put("posHue", String.valueOf(getPositiveHue()));
		param.put("negHue", String.valueOf(getNegativeHue()));

		return param;
	}

	@Override
	public void setVisProperties(Hashtable param) {
		drawer.setVisProperties(param);

		float temp;

		try {
			temp = new Float((String) param.get("focuserMin")).floatValue();
			if (!Float.isNaN(temp)) {
				setFocuserMinMax(temp, drawer.getFocuserMax());
			}
		} catch (Exception ex) {
		}
		try {
			temp = new Float((String) param.get("focuserMax")).floatValue();
			if (!Float.isNaN(temp)) {
				setFocuserMinMax(drawer.getFocuserMin(), temp);
			}
		} catch (Exception ex) {
		}

		try {
			temp = new Float((String) param.get("cmpTo")).floatValue();
			if (!Float.isNaN(temp)) {
				setCmp(temp);
			}
		} catch (Exception ex) {
		}
		try {
			temp = new Float((String) param.get("posHue")).floatValue();
			if (!Float.isNaN(temp)) {
				setColors(temp, getNegativeHue());
			}
		} catch (Exception ex) {
		}
		try {
			temp = new Float((String) param.get("negHue")).floatValue();
			if (!Float.isNaN(temp)) {
				setColors(getPositiveHue(), temp);
			}
		} catch (Exception ex) {
		}

//    super.setVisProperties(param);
	}

//~ID
}