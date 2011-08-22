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
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.SelectDialog;
import spade.lib.basicwin.StringInRectangle;
import spade.lib.help.Helper;
import spade.vis.database.AttributeTypes;
import spade.vis.database.ThematicDataItem;
import spade.vis.geometry.SimpleSign;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Mar 10, 2010
 * Time: 3:17:44 PM
 * Visualizes combinations of values of two qualitative attributes by icons varying in colors and shapes
 */
public class ColorAndShapeVisualizer extends DataPresenter {
	/**
	 * Index of the attribute represented by colors
	 */
	protected int cIdx = 0;
	/**
	 * Index of the attribute represented by shapes
	 */
	protected int sIdx = 1;
	/**
	 * Used to represent values by colors
	 */
	protected QualitativeClassifier qClassifier = null;
	/**
	 * Used to represent values by shapes
	 */
	protected SimpleSignPresenter sPres = null;

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
		for (int i = 0; i < attr.size(); i++) {
			String id = (String) attr.elementAt(i);
			if (id == null) {
				err = "Null attribute identifier!";
				return false;
			}
			char at = table.getAttributeType((String) attr.elementAt(i));
			if (!AttributeTypes.isNominalType(at) && AttributeTypes.isIntegerType(at))
				if (!table.isValuesCountBelow(attr, Helper.getMaxNumValuesForQualitativeVis())) {
					err = attr.elementAt(i) + ": " + errors[10];
					return false;
				}
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
	* This method informs whether the Visualizer produces diagrams.
	* This is important for defining the order of drawing of GeoLayers on the
	* map: the diagrams should be drawn on top of all geography.
	*/
	@Override
	public boolean isDiagramPresentation() {
		return true;
	}

	/**
	* Informs whether this visualizer allows the visualized attributes to
	* be transformed. This visualizer returns false.
	*/
	@Override
	public boolean getAllowTransform() {
		return false;
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
		qClassifier = new QualitativeClassifier(table, (String) attr.elementAt(0));
		qClassifier.addPropertyChangeListener(this);
		if (subAttr != null && subAttr.size() > 0 && subAttr.elementAt(0) != null) {
			qClassifier.setSubAttributes((Vector) subAttr.elementAt(0), 0);
		}
		if (invariants != null && invariants.size() > 0 && invariants.elementAt(0) != null) {
			qClassifier.setInvariant((String) invariants.elementAt(0), 0);
		}
		qClassifier.setup();
		Vector v = new Vector(1, 1);
		v.addElement(attr.elementAt(1));
		sPres = new SimpleSignPresenter();
		sPres.setDataSource(table);
		sPres.setAttributes(v);
		sPres.addVisChangeListener(this);
		sPres.setMayChangeColor(false);
		sPres.setDefaultColor(Color.red);
		if (subAttr != null && subAttr.size() > 1 && subAttr.elementAt(1) != null) {
			sPres.setSubAttributes((Vector) subAttr.elementAt(1), 0);
		}
		if (invariants != null && invariants.size() > 1 && invariants.elementAt(1) != null) {
			sPres.setInvariant((String) invariants.elementAt(1), 0);
		}
		sPres.setup();
	}

	/**
	* Destroys the visualizer when it is no more used.
	*/
	@Override
	public void destroy() {
		if (sPres != null) {
			sPres.destroy();
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
		if (sPres != null) {
			changed = sPres.adjustToDataChange(valuesTransformed);
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
		if (sPres == null)
			return null;
		SimpleSign sgn = (SimpleSign) sPres.getPresentation(dit);
		if (sgn == null)
			return null;
		if (qClassifier != null) {
			sgn.setColor(qClassifier.getColorForDataItem(dit));
		}
		return sgn;
	}

	/**
	* Draws a small picture symbolizing this presentation technique
	*/
	@Override
	public void drawIcon(Graphics g, int x, int y, int w, int h) {
		if (sPres == null)
			return;
		sPres.drawIcon(g, x, y, w, h);
	}

	/**
	* Draws the part of the legend explaining this specific presentation method.
	*/
	@Override
	public Rectangle drawMethodSpecificLegend(Graphics g, int startY, int leftMarg, int prefW) {
		if (sPres == null || qClassifier == null)
			return null;
		g.setColor(Color.black);
		Point p = StringInRectangle.drawText(g, getAttrName(0), leftMarg, startY, prefW, true);
		Rectangle r = qClassifier.drawLegend(null, g, p.y + 10, leftMarg, prefW);
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
		Rectangle r1 = sPres.drawMethodSpecificLegend(g, startY, leftMarg, prefW);
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
		if (sPres != null) {
			Vector v = new Vector(1, 1);
			v.addElement(attr.elementAt(1));
			sPres.setAttributes(v);
			if (attrNames != null && attrNames.size() > 1) {
				sPres.setAttrName((String) attrNames.elementAt(1), 0);
			}
		}
		if (qClassifier != null) {
			qClassifier.setAttribute((String) attributes.elementAt(0));
		}
	}

	/**
	* Sets the name of the attribute with the given index in the list of the
	* attributes handled by this visualizer.
	*/
	@Override
	public void setAttrName(String name, int attrIdx) {
		if (name != null && attrIdx == 1 && sPres != null) {
			sPres.setAttrName(name, 0);
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
		if (attrIdx == 1 && sPres != null) {
			sPres.setSubAttributes(sub, 0);
		} else if (attrIdx == 0 && qClassifier != null) {
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
		if (attrIdx == 1 && sPres != null) {
			sPres.setInvariant(inv, 0);
		} else if (attrIdx == 0 && qClassifier != null) {
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
		if (sPres != null) {
			sPres.setCurrentSubAttrIndex(idx);
		}
		if (qClassifier != null) {
			qClassifier.setCurrentSubAttrIndex(idx);
		}
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
		if (sPres != null && qClassifier != null) {
			SelectDialog selDia = new SelectDialog(CManager.getAnyFrame(), "Symbol parameters", "What to modify?");
			selDia.addOption("colors", "colors", true);
			selDia.addOption("symbol size", "size", false);
			selDia.show();
			if (selDia.getSelectedOptionN() == 0) {
				qClassifier.startChangeColors();
			} else {
				sPres.startChangeParameters();
			}
		} else if (sPres != null) {
			sPres.startChangeParameters();
		} else if (qClassifier != null) {
			qClassifier.startChangeColors();
		}
	}

	/**
	 * Reacts to changes in the classifier or in the line thickness visualiser
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(qClassifier)) {
			//...
			notifyVisChange();
		} else if (e.getSource().equals(sPres)) {
			//...
			notifyVisChange();
		} else {
			super.propertyChange(e);
		}
	}

	@Override
	public Hashtable getVisProperties() {
		if (qClassifier == null || sPres == null)
			return null;
		Hashtable param = qClassifier.getVisProperties();
		if (param == null) {
			param = new Hashtable();
		}
		Hashtable param1 = sPres.getVisProperties();
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
		if (sPres != null) {
			sPres.setVisProperties(param);
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

	public SimpleSignPresenter getSimpleSignPresenter() {
		return sPres;
	}

	public QualitativeClassifier getQualitativeClassifier() {
		return qClassifier;
	}
}
