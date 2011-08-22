package spade.vis.mapvis;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import spade.analysis.classification.ObjectColorer;
import spade.analysis.classification.QualitativeClassifier;
import spade.analysis.transform.AttributeTransformer;
import spade.lib.basicwin.Drawing;
import spade.lib.basicwin.StringInRectangle;
import spade.lib.help.Helper;
import spade.lib.util.NumRange;
import spade.vis.database.AttributeTypes;
import spade.vis.database.ThematicDataItem;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 31-Jan-2007
 * Time: 15:12:32
 * Visualises two attributes, numeric and qualitative, associated with linear
 * objects. The numeric attribute is represented by line thickness, and the
 * qualitative attribute by line color.
 */
public class LineThicknessAndColorVisualiser extends DataPresenter {
	/**
	 * Index of the numeric attribute
	 */
	protected int nIdx = 0;
	/**
	 * Index of the qualitative attribute
	 */
	protected int qIdx = 1;
	/**
	 * Used to define line thicknesses
	 */
	protected LineThicknessVisualiser lThickVis = null;
	/**
	 * Used to define line colors
	 */
	protected QualitativeClassifier qClassifier = null;

	/**
	 * Checks if this visualization method is applicable to the given set of
	 * attributes. May use the DataInformer to check types and values
	 * of the attributes.
	 */
	@Override
	public boolean isApplicable(Vector attr) {
		err = null;
		if (attr == null || attr.size() < 1) {
			err = errors[0];
			return false;
		}
		if (table == null) {
			err = errors[1];
			return false;
		}
		if (attr.size() < 2) {
			err = errors[7];
			return false;
		}
		if (attr.size() > 2) {
			err = errors[8];
			return false;
		}

		boolean num1 = false, num2 = false, nom1 = false, nom2 = false;
		for (int i = 0; i < attr.size(); i++) {
			String id = (String) attr.elementAt(i);
			if (id == null) {
				err = "Null attribute identifier!";
				return false;
			}
			char type = table.getAttributeType(id);
			if (AttributeTypes.isNumericType(type))
				if (i == 0) {
					num1 = true;
				} else {
					num2 = true;
				}
			else if (AttributeTypes.isNominalType(type))
				if (i == 0) {
					nom1 = true;
				} else {
					nom2 = true;
				}
		}
		if (!num1 && !num2) {
			err = "One of the attributes must be numeric!";
			return false;
		}
		if (num1 && num2) { //both attributes are numeric
			Vector v = new Vector(1, 1);
			v.addElement(attr.elementAt(1));
			boolean fewValues = table.isValuesCountBelow(v, Helper.getMaxNumValuesForQualitativeVis());
			if (fewValues) {
				num2 = false;
				nom2 = true;
			} else {
				v.removeAllElements();
				v.addElement(attr.elementAt(0));
				fewValues = table.isValuesCountBelow(v, Helper.getMaxNumValuesForQualitativeVis());
				if (fewValues) {
					num1 = false;
					nom1 = true;
				}
			}
		}
		if (!nom1 && !nom2) {
			err = "One of the attributes must be qualitative!";
			return false;
		}
		if (num1) {
			nIdx = 0;
			qIdx = 1;
		} else {
			nIdx = 1;
			qIdx = 0;
		}
		String id = (String) attr.elementAt(qIdx);
		Vector values = (subAttr == null || subAttr.size() <= qIdx || subAttr.elementAt(qIdx) == null) ? table.getAllAttrValuesAsStrings(id) : table.getAllAttrValuesAsStrings((Vector) subAttr.elementAt(qIdx));
		if (values == null) {
			err = id + ": " + errors[3];
			return false;
		}
		id = (String) attr.elementAt(nIdx);
		NumRange nr = (subAttr == null || subAttr.size() <= nIdx || subAttr.elementAt(nIdx) == null) ? getAttrValueRange(id) : getAttrValueRange((Vector) subAttr.elementAt(nIdx));
		if (nr == null || Double.isNaN(nr.maxValue)) {
			err = id + ": " + errors[3];
			return false;
		}
		return true;
	}

	/**
	* Must check semantic applicability of this visualization method to the
	* attributes previously set in the visualizer.
	*/
	@Override
	public boolean checkSemantics() {
		return true;
	}

	/**
	* The Visualizer sets its parameters. For this purpose it may need
	* data statistics. Statistics are received from a StatisticsProvider.
	* A reference to a StatisticsProvider may be received from the DataInformer.
	*/
	@Override
	public void setup() {
		if (table == null || attr == null || !isApplicable(attr))
			return;
		Vector v = new Vector(1, 1);
		v.addElement(attr.elementAt(nIdx));
		lThickVis = new LineThicknessVisualiser();
		lThickVis.setDataSource(table);
		lThickVis.setAttributes(v);
		lThickVis.addVisChangeListener(this);
		if (aTrans != null) {
			lThickVis.setAttributeTransformer(aTrans, true);
		}
		if (subAttr != null && subAttr.size() > nIdx && subAttr.elementAt(nIdx) != null) {
			lThickVis.setSubAttributes((Vector) subAttr.elementAt(nIdx), 0);
		}
		if (invariants != null && invariants.size() > nIdx && invariants.elementAt(nIdx) != null) {
			lThickVis.setInvariant((String) invariants.elementAt(nIdx), 0);
		}
		lThickVis.setup();

		qClassifier = new QualitativeClassifier(table, (String) attr.elementAt(qIdx));
		qClassifier.addPropertyChangeListener(this);
		if (subAttr != null && subAttr.size() > qIdx && subAttr.elementAt(qIdx) != null) {
			qClassifier.setSubAttributes((Vector) subAttr.elementAt(qIdx), 0);
		}
		if (invariants != null && invariants.size() > qIdx && invariants.elementAt(qIdx) != null) {
			qClassifier.setInvariant((String) invariants.elementAt(qIdx), 0);
		}
		qClassifier.setup();
		if (qClassifier.getNClasses() > 0) {
			lThickVis.setDefaultColor(qClassifier.getClassColor(0));
		}
	}

	/**
	* Destroys the visualizer when it is no more used.
	*/
	@Override
	public void destroy() {
		if (lThickVis != null) {
			lThickVis.destroy();
		}
		if (qClassifier != null) {
			qClassifier.destroy();
		}
		super.destroy();
	}

	/**
	 * Returns a reference to the classifier.
	 */
	@Override
	public ObjectColorer getObjectColorer() {
		return qClassifier;
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
		boolean changed = false;
		if (lThickVis != null) {
			changed = lThickVis.adjustToDataChange(valuesTransformed);
		}
		if (qClassifier != null) {
			qClassifier.setup();
			changed = true;
		}
		return changed;
	}

	/**
	 * Returns an instance of LineDrawSpec, which specifies how to represent the
	 * data from the given ThematicDataItem.
	 */
	@Override
	public Object getPresentation(ThematicDataItem dit) {
		if (lThickVis == null)
			return null;
		LineDrawSpec lds = (LineDrawSpec) lThickVis.getPresentation(dit);
		if (lds == null)
			return null;
		if (qClassifier != null) {
			lds.color = qClassifier.getColorForDataItem(dit);
		}
		return lds;
	}

	/**
	* Draws a small picture symbolizing this presentation technique
	*/
	@Override
	public void drawIcon(Graphics g, int x, int y, int w, int h) {
		if (lThickVis == null)
			return;
		int th = lThickVis.minThickness, dx = (w - 3 * th - 9) / 3;
		if (dx < 2) {
			dx = 2;
		}
		Rectangle r = g.getClipBounds();
		g.setClip(x, y, w, h);
		Color color = lThickVis.getDefaultColor();
		if (qClassifier != null && qClassifier.getNClasses() > 0) {
			color = qClassifier.getClassColor(0);
		}
		g.setColor(color);
		Drawing.drawLine(g, th, x, y, x + dx, y + h, true, false);
		x += dx + th;
		th += 3;
		if (qClassifier != null && qClassifier.getNClasses() > 1) {
			color = qClassifier.getClassColor(1);
			g.setColor(color);
		}
		Drawing.drawLine(g, th, x, y, x + dx, y + h, true, false);
		x += dx + th;
		th += 3;
		if (qClassifier != null) {
			if (qClassifier.getNClasses() > 2) {
				color = qClassifier.getClassColor(2);
			} else if (qClassifier.getNClasses() > 0) {
				color = qClassifier.getClassColor(0);
			}
			g.setColor(color);
		}
		Drawing.drawLine(g, th, x, y, x + dx, y + h, true, false);
		if (r != null) {
			g.setClip(r.x, r.y, r.width, r.height);
		} else {
			g.setClip(null);
		}
	}

	/**
	* Draws the part of the legend explaining this specific presentation method.
	*/
	@Override
	public Rectangle drawMethodSpecificLegend(Graphics g, int startY, int leftMarg, int prefW) {
		if (lThickVis == null || qClassifier == null)
			return null;
		g.setColor(Color.black);
		Point p = StringInRectangle.drawText(g, getAttrName(nIdx), leftMarg, startY, prefW, true);
		Rectangle r = lThickVis.showNumberEncoding(g, p.y + 10, leftMarg, prefW);
		p.x -= leftMarg;
		p.y -= startY;
		if (r == null)
			return new Rectangle(leftMarg, startY, p.x, p.y);
		r.height += p.y;
		r.y = startY;
		if (r.width < p.x) {
			r.width = p.x;
		}
		startY += r.height;
		Rectangle r1 = qClassifier.drawLegend(null, g, startY, leftMarg, prefW);
		if (r1 != null) {
			r.height += r1.height;
			if (r1.width > r.width) {
				r.width = r1.width;
			}
		}
		return r;
	}

	/**
	* Sets the list of identifiers of attributes to be visualized.
	* Additionally, full names of the attributes (if different from the
	* identifiers) may be specified.
	*/
	@Override
	public void setAttributes(Vector attributes, Vector attrNames) {
		super.setAttributes(attributes, attrNames);
		if (attributes == null || attributes.size() < 2)
			return;
		if (lThickVis != null) {
			Vector v = new Vector(1, 1);
			v.addElement(attr.elementAt(nIdx));
			lThickVis.setAttributes(v);
			if (attrNames != null && attrNames.size() > nIdx) {
				lThickVis.setAttrName((String) attrNames.elementAt(nIdx), 0);
			}
		}
		if (qClassifier != null) {
			qClassifier.setAttribute((String) attributes.elementAt(qIdx));
		}
	}

	/**
	* Sets the name of the attribute with the given index in the list of the
	* attributes handled by this visualizer.
	*/
	@Override
	public void setAttrName(String name, int attrIdx) {
		if (name != null && attrIdx == nIdx && lThickVis != null) {
			lThickVis.setAttrName(name, 0);
		}
	}

	/**
	* Some attributes may be dependent on parameters. This method associates such
	* an attribute with a list of identifiers of the children attributes. The
	* attribute is specified through its index in the list of the attributes
	* handled by this visualizer.
	*/
	@Override
	public void setSubAttributes(Vector sub, int attrIdx) {
		super.setSubAttributes(sub, attrIdx);
		if (attrIdx == nIdx && lThickVis != null) {
			lThickVis.setSubAttributes(sub, 0);
		} else if (attrIdx == qIdx && qClassifier != null) {
			qClassifier.setSubAttributes(sub, 0);
		}
	}

	/**
	* If there are time-dependent attributes, they may also refer to different
	* values of other, non-temporal parameters. For such attributes, this
	* vector contains "invariants" - strings indicating the values of the
	* additional parameters. This method sets the invariant for a group of
	* "sub-attributes" corresponding to the attribute with the given index.
	*/
	@Override
	public void setInvariant(String inv, int attrIdx) {
		super.setInvariant(inv, attrIdx);
		if (attrIdx == nIdx && lThickVis != null) {
			lThickVis.setInvariant(inv, 0);
		} else if (attrIdx == qIdx && qClassifier != null) {
			qClassifier.setInvariant(inv, 0);
		}
	}

	/**
	* Sets the index of the subattribute to be currently taken for the
	* visualization.
	*/
	@Override
	public void setCurrentSubAttrIndex(int idx) {
		super.setCurrentSubAttrIndex(idx);
		if (lThickVis != null) {
			lThickVis.setCurrentSubAttrIndex(idx);
		}
		if (qClassifier != null) {
			qClassifier.setCurrentSubAttrIndex(idx);
		}
	}

	/**
	 * This method informs whether the Visualizer produces diagrams.
	 * This is important for defining the order of drawing of GeoLayers on the
	 * map: the diagrams should be drawn on top of all geography.
	 * Returns false.
	 */
	@Override
	public boolean isDiagramPresentation() {
		return false;
	}

	/**
	* Informs whether this visualizer allows each of the visualized attributes to
	* be transformed individually (returns false).
	*/
	@Override
	public boolean getAllowTransformIndividually() {
		return false;
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
		if (lThickVis != null) {
			lThickVis.setAttributeTransformer(transformer, true);
		}
	}

	/**
	* Returns the number of classes in the individual classifiers
	*/
	public int getNClasses() {
		if (qClassifier == null)
			return 0;
		return qClassifier.getNClasses();
	}

	/**
	* Replies whether the color scale used by this visualization method may be
	* changed. This does not include interactive analytical manipulation.
	* Returns true.
	*/
	@Override
	public boolean canChangeColors() {
		return qClassifier != null;
	}

	/**
	* A classifier must give the user an opportunity to change interactively
	* colors assigned to classes. Here the ClassDrawer invokes the method
	* startChangeColors of its classifier.
	*/
	@Override
	public void startChangeColors() {
		if (qClassifier != null) {
			qClassifier.startChangeColors();
		}
	}

	/**
	* Must reply whether any parameters of this visualization method may be
	* changed, e.g. colors or sizes of signs. This does not include interactive
	* analytical manipulation.
	* Returns true.
	*/
	@Override
	public boolean canChangeParameters() {
		return true;
	}

	/**
	* Constructs and displays a dialog for changing parameters of this
	* visualization method: minimum and maximum thickness of the lines.
	*/
	@Override
	public void startChangeParameters() {
		if (lThickVis != null) {
			lThickVis.startChangeParameters();
		}
	}

	/**
	 * Reacts to changes in the classifier or in the line thickness visualiser
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(qClassifier)) {
			//...
			if (qClassifier.getNClasses() > 0 && !lThickVis.getDefaultColor().equals(qClassifier.getClassColor(0))) {
				lThickVis.setDefaultColor(qClassifier.getClassColor(0));
				lThickVis.notifyVisChange();
			}
			notifyVisChange();
		} else if (e.getSource().equals(lThickVis)) {
			//...
			notifyVisChange();
		} else {
			super.propertyChange(e);
		}
	}

	@Override
	public Hashtable getVisProperties() {
		if (qClassifier == null || lThickVis == null)
			return null;
		Hashtable param = qClassifier.getVisProperties();
		if (param == null) {
			param = new Hashtable();
		}
		Hashtable param1 = lThickVis.getVisProperties();
		if (param1 != null && !param1.isEmpty()) {
			Enumeration keys = param1.keys();
			while (keys.hasMoreElements()) {
				Object key = keys.nextElement();
				Object value = param1.get(key);
				if (value != null) {
					param.put(key, value);
				}
			}
		}
		return param;
	}

	@Override
	public void setVisProperties(Hashtable param) {
		if (param == null)
			return;
		if (qClassifier != null) {
			qClassifier.setVisProperties(param);
		}
		if (lThickVis != null) {
			lThickVis.setVisProperties(param);
		}
	}

	/**
	* A method from the DataViewRegulator interface.
	* Replies whether attributes with null values should be shown in data popups.
	* For this purpose, checks whether the classifier is a TableClassifier and,
	* if so, whether it prohibits displaying attributes with null values.
	* In all other cases, returns true.
	*/
	@Override
	public boolean getShowAttrsWithNullValues() {
		if (qClassifier == null)
			return true;
		return qClassifier.getShowAttrsWithNullValues();
	}

	/**
	* A method from the DataViewRegulator interface.
	* Returns the class number for the data record with the given index
	*/
	@Override
	public int getRecordClassN(int recN) {
		if (qClassifier == null)
			return -1;
		return qClassifier.getRecordClass(recN);
	}

	/**
	* A method from the DataViewRegulator interface.
	* Returns the name of the class with the given number
	*/
	@Override
	public String getClassName(int classN) {
		if (qClassifier == null || classN < 0)
			return null;
		return qClassifier.getClassName(classN);
	}

	/**
	* A method from the DataViewRegulator interface.
	* Returns the color of the class with the given number
	*/
	@Override
	public Color getClassColor(int classN) {
		if (qClassifier == null || classN < 0)
			return null;
		return qClassifier.getClassColor(classN);
	}

	public LineThicknessVisualiser getLineThicknessVisualiser() {
		return lThickVis;
	}

	public QualitativeClassifier getQualitativeClassifier() {
		return qClassifier;
	}
}
