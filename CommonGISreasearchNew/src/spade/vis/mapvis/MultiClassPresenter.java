package spade.vis.mapvis;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.Vector;

import spade.analysis.classification.Classifier;
import spade.analysis.classification.ObjectColorer;
import spade.analysis.classification.TableClassifier;
import spade.analysis.transform.AttributeTransformer;
import spade.lib.basicwin.Metrics;
import spade.vis.database.ThematicDataItem;
import spade.vis.geometry.MosaicSign;
import spade.vis.geometry.Sign;

/**
* The base class for the visualizers representing results of multiple homogeneous
* classifications in one of two variants:
* 1) by special signs with coloured segments;
* 2) by "small multiples",
* depending on the value of the special logical variable.
*/
public class MultiClassPresenter extends DataPresenter implements SignDrawer, MultiMapVisualizer, PropertyChangeListener {
	/**
	* The classifiers used for each classification. All classifiers must be of the
	* same type.
	*/
	protected Vector classifiers = null;

	public Vector getClassifiers() {
		return classifiers;
	}

	/**
	* Destroys the visualizer when it is no more used.
	*/
	@Override
	public void destroy() {
		if (classifiers != null) {
			for (int i = 0; i < classifiers.size(); i++)
				if (classifiers.elementAt(i) != null && (classifiers.elementAt(i) instanceof Classifier)) {
					((Classifier) classifiers.elementAt(i)).destroy();
				}
			classifiers.removeAllElements();
			classifiers = null;
		}
		super.destroy();
	}

	/**
	* This variable determines whether the results of the multiple classifications
	* are represented by special signs with coloured segments ("mosaic signs")
	* or by "small multiples". By default, the signs are used.
	*/
	protected boolean useSigns = true;
	/**
	* An instance of sign used in the method getPresentation(...) if the
	* representation by signs is used
	*/
	protected MosaicSign symbol = null;
	/**
	* An instance of sign used in the dialog for changing sign parameters if the
	* representation by signs is used
	*/
	protected MosaicSign s1 = null;
	/**
	* Current map number (the method getPresentation is called for this map) if the
	* representation by "small multiples" is used
	*/
	protected int currMapIdx = 0;

	/**
	* Informs whether this visualizer allows each of the visualized attributes to
	* be transformed individually (returns true) or requires all attributes to
	* be transformed in the same way (returns false). By default, returns false.
	*/
	@Override
	public boolean getAllowTransformIndividually() {
		return false;
	}

	/**
	 * Sets the slassifiers
	 */
	public void setClassifiers(Vector classifiers) {
		this.classifiers = classifiers;
		if (classifiers != null) {
			for (int i = 0; i < classifiers.size(); i++) {
				((Classifier) classifiers.elementAt(i)).addPropertyChangeListener(this);
			}
		}
	}

	/**
	* Returns the number of individual classifiers used
	*/
	public int getNClassifiers() {
		if (classifiers == null)
			return 0;
		return classifiers.size();
	}

	/**
	* Returns the number of classes in the individual classifiers
	*/
	public int getNClasses() {
		if (classifiers == null || classifiers.size() < 1)
			return 0;
		return ((Classifier) classifiers.elementAt(0)).getNClasses();
	}

	@Override
	public Color getClassColor(int classN) {
		if (classifiers == null || classifiers.size() < 1)
			return null;
		return ((Classifier) classifiers.elementAt(0)).getClassColor(classN);
	}

	public int getHiddenClassCount() {
		if (classifiers == null || classifiers.size() < 1)
			return 0;
		return ((Classifier) classifiers.elementAt(0)).getHiddenClassCount();
	}

	public void setClassIsHidden(boolean value, int classN) {
		if (classifiers == null || classifiers.size() < 1)
			return;
		for (int i = 0; i < classifiers.size(); i++) {
			((Classifier) classifiers.elementAt(i)).setClassIsHidden(value, classN);
		}
	}

	public void exposeAllClasses() {
		if (classifiers == null || classifiers.size() < 1)
			return;
		for (int i = 0; i < classifiers.size(); i++) {
			((Classifier) classifiers.elementAt(i)).exposeAllClasses();
		}
	}

	/**
	 * Returns a reference to one of the classifiers.
	 */
	@Override
	public ObjectColorer getObjectColorer() {
		if (classifiers == null || classifiers.size() < 1)
			return null;
		return (Classifier) classifiers.elementAt(0);
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
		if (classifiers != null) {
			for (int i = 0; i < classifiers.size(); i++)
				if (classifiers.elementAt(i) instanceof TableClassifier) {
					((TableClassifier) classifiers.elementAt(i)).setAttributeTransformer(transformer, false);
				}
		}
	}

	/**
	* Returns the required number of maps ("small multiples"), depending on the
	* number of the individual classifiers available.
	*/
	@Override
	public int getNIndMaps() {
		if (useSigns)
			return 1;
		return getNClassifiers();
	}

	/**
	* Returns the name of the map with the given index in the "small multiples".
	*/
	@Override
	public String getIndMapName(int idx) {
		if (idx < 0 || idx >= getNClassifiers())
			return null;
		if (classifiers.elementAt(idx) instanceof TableClassifier) {
			TableClassifier tcl = (TableClassifier) classifiers.elementAt(idx);
			if (tcl.getAttributes() != null && tcl.getAttributes().size() == 1)
				return tcl.getAttributeName(0);
		}
		return String.valueOf(idx);
	}

	/**
	* Sets the number of the current individual map. Since this moment all
	* calls of the "getPresentation" method will relate to this map.
	*/
	@Override
	public void setCurrentMapIndex(int idx) {
		if (idx < 0 || idx >= getNIndMaps())
			return;
		currMapIdx = idx;
	}

	/**
	* Returns the number of the current individual map.
	*/
	@Override
	public int getCurrentMapIndex() {
		return currMapIdx;
	}

	/**
	* Constructs an instance of BarChart for further use in the method
	* getPresentation(...)
	*/
	protected void checkCreateSymbol() {
		if (useSigns && symbol == null && getNClassifiers() > 0) {
			symbol = new MosaicSign();
			setupSymbol(symbol);
			int ncols = getNClassifiers(), nrows = 1;
			while (ncols > 10) {
				++nrows;
				ncols = getNClassifiers() / nrows;
			}
			symbol.setSizes(ncols * Sign.mm, nrows * Sign.mm);
			if (symbol.getWidth() < 5 * Sign.mm) {
				symbol.setWidth(5 * Sign.mm);
			}
			if (symbol.getHeight() < 5 * Sign.mm) {
				symbol.setHeight(5 * Sign.mm);
			}
			symbol.setNColumns(ncols);
		}
	}

	protected void setupSymbol(MosaicSign sgn) {
		sgn.setMayChangeProperty(Sign.MAX_SIZE, false);
		sgn.setMayChangeProperty(Sign.MIN_SIZE, false);
		sgn.setMayChangeProperty(Sign.USE_FRAME, false);
		sgn.setMayChangeProperty(Sign.SEGMENT_ORDER, false);
		sgn.setMayChangeProperty(Sign.COLOR, false);
		sgn.setMayChangeProperty(Sign.SIZE, true);
	}

	public MosaicSign getSymbol() {
		useSigns = true;
		checkCreateSymbol();
		return symbol;
	}

	/**
	* Methods from the interface SignDrawer. Produces an instance of BarChart for
	* the use in the dialog for changing sign parameters
	*/
	@Override
	public Sign getSignInstance() {
		if (!useSigns)
			return null;
		if (s1 != null)
			return s1;
		checkCreateSymbol();
		if (symbol == null)
			return null;
		s1 = new MosaicSign();
		setupSymbol(s1);
		s1.setSizes(symbol.getWidth(), symbol.getHeight());
		s1.setSegmColors(symbol.getSegmColors());
		s1.setNColumns(symbol.getNColumns());
		return s1;
	}

	/**
	* Modifies segment colors in the sign instance so that they correspond to
	* class colors
	*/
	protected void setColorsInSignInstance() {
		if (!useSigns || s1 == null || classifiers == null || classifiers.size() < 0)
			return;
		Color colors[] = new Color[classifiers.size()];
		int n = getNClasses();
		for (int i = 0; i < colors.length; i++) {
			colors[i] = getClassColor(i % n);
		}
		s1.setSegmColors(colors);
	}

	/**
	* Through this method a SignDrawer is informed that some of the sign properties
	* have changed. The sign drawer must care about repainting the map.
	* The possible identifiers of sign properties are listed in the class
	* @see spade.vis.geometry.Sign
	*/
	@Override
	public void signPropertyChanged(int propertyId, Sign sgn) {
		if (!useSigns || sgn == null || symbol == null)
			return;
		if (propertyId == Sign.SIZE) {
			symbol.setSizes(sgn.getWidth(), sgn.getHeight());
			notifyVisChange();
		}
	}

	/**
	* This method is called by a GeoObject. The GeoObject sends its DataItem
	* to the Visualizer, and the Visualizer returns an Object specifying the
	* presentation of data contained in this DataItem.
	*/
	@Override
	public Object getPresentation(ThematicDataItem dit) {
		if (dit == null || table == null || getNClassifiers() < 1)
			return null;
		if (useSigns) {
			if (sdController != null && !sdController.mustDrawObject(dit.getId()))
				return null;
			checkCreateSymbol();
			Color colors[] = new Color[classifiers.size()];
			for (int i = 0; i < classifiers.size(); i++) {
				Classifier cl = (Classifier) classifiers.elementAt(i);
				colors[i] = cl.getColorForDataItem(dit, table.getContainerIdentifier());
			}
			symbol.setSegmColors(colors);
			return symbol;
		}
		if (currMapIdx >= 0 && currMapIdx < classifiers.size())
			if (classifiers.elementAt(currMapIdx) instanceof TableClassifier) {
				TableClassifier tcl = (TableClassifier) classifiers.elementAt(currMapIdx);
				return tcl.getColorForRecord(dit);
			} else {
				Classifier cl = (Classifier) classifiers.elementAt(currMapIdx);
				return cl.getColorForDataItem(dit);
			}
		return null;
	}

	/**
	* Draws a small picture symbolizing this presentation technique
	*/
	@Override
	public void drawIcon(Graphics g, int x, int y, int w, int h) {
		if (useSigns) {
			if (s1 == null) {
				getSignInstance();
			}
			if (s1 == null)
				return;
			int width = s1.getWidth(), height = s1.getHeight();
			s1.setSizes(w - 2, h - 2);
			s1.draw(g, x, y + h);
			s1.setSizes(width, height);
		} else {
			int rw = w / 2 - 3;
			Color c1 = Color.red, c2 = Color.blue;
			if (getNClasses() > 1) {
				c1 = getClassColor(0);
				c2 = getClassColor(getNClasses() - 1);
			}
			g.setColor(c1);
			g.fillRect(x, y, rw, h);
			g.fillRect(x + rw + 3, y, rw, h);
			g.setColor(c2);
			int h3 = h / 3;
			g.fillRect(x, y, rw, h3);
			g.fillRect(x + rw + 3, y + h - h3, rw, h3);
			g.setColor(Color.black);
			g.drawRect(x, y, rw, h);
			g.drawRect(x + rw + 3, y, rw, h);
		}
	}

	/**
	* This method informs whether the Visualizer produces diagrams.
	* This is important for defining the order of drawing of GeoLayers on the
	* map: the diagrams should be drawn on top of all geography.
	*/
	@Override
	public boolean isDiagramPresentation() {
		return useSigns;
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
		if (attrIdx >= 0 && attrIdx < getNClassifiers() && (classifiers.elementAt(attrIdx) instanceof TableClassifier)) {
			((TableClassifier) classifiers.elementAt(attrIdx)).setSubAttributes(sub, 0);
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
		if (attrIdx >= 0 && attrIdx < getNClassifiers() && (classifiers.elementAt(attrIdx) instanceof TableClassifier)) {
			((TableClassifier) classifiers.elementAt(attrIdx)).setInvariant(inv, 0);
		}
	}

	/**
	* Sets the index of the subattribute to be currently taken for the
	* visualization.
	*/
	@Override
	public void setCurrentSubAttrIndex(int idx) {
		super.setCurrentSubAttrIndex(idx);
		for (int i = 0; i < getNClassifiers(); i++)
			if (classifiers.elementAt(i) instanceof TableClassifier) {
				((TableClassifier) classifiers.elementAt(i)).setCurrentSubAttrIndex(idx);
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
		return useSigns || canChangeColors();
	}

	/**
	* Constructs and displays a dialog for changing parameters of this
	* visualization method; in this case, the size of the signs.
	*/
	@Override
	public void startChangeParameters() {
		if (useSigns) {
			if (symbol == null)
				return;
			SignParamsController pc = new SignParamsController();
			pc.startChangeParameters(this);
		} else if (canChangeColors()) {
			startChangeColors();
		}
	}

	/**
	* Returns the current number of columns in the class mosaic symbol
	*/
	public int getNColumns() {
		if (!useSigns)
			return 0;
		checkCreateSymbol();
		if (symbol == null)
			return 0;
		return symbol.getNColumns();
	}

	/**
	* Sets the number of columns in the class mosaic symbol
	*/
	public void setNColumns(int ncols) {
		if (!useSigns || symbol == null)
			return;
		if (ncols < 1) {
			ncols = 1;
		}
		int n = getNClassifiers();
		if (n > 0 && ncols > n) {
			ncols = n;
		}
		symbol.setNColumns(ncols);
		if (s1 != null) {
			s1.setNColumns(ncols);
		}
	}

	public void classesHaveChanged() {
		if (classifiers == null || classifiers.size() < 1)
			return;
		for (int i = 0; i < classifiers.size(); i++)
			if (classifiers.elementAt(i) instanceof Classifier) {
				((Classifier) classifiers.elementAt(i)).classesHaveChanged();
			}
	}

	/**
	* Draws the part of the legend explaining this specific presentation method.
	*/
	@Override
	public Rectangle drawMethodSpecificLegend(Graphics g, int startY, int leftmarg, int prefW) {
		g.setColor(Color.black);
		Rectangle r = drawAttrNames(g, startY, leftmarg, prefW, true);
		if (r == null) {
			r = new Rectangle(leftmarg, startY, 0, 0);
		}
		int y = r.y + r.height, w = r.width;
		if (classifiers != null && classifiers.size() > 0) {
			for (int i = 0; i < classifiers.size(); i++) {
				y += Metrics.mm();
				r = ((Classifier) classifiers.elementAt(i)).drawLegend(null, g, y, leftmarg, prefW);
				y = r.y + r.height;
				if (w < r.width) {
					w = r.width;
				}
			}
		}
		return new Rectangle(leftmarg, startY, w, y - startY);
	}

	/**
	 * Checks if this visualization method is applicable to the given set of
	 * attributes. May use the DataInformer to check types and values
	 * of the attributes.
	 * By default, returns true.
	 */
	@Override
	public boolean isApplicable(Vector attr) {
		return true;
	}

	/**
	 * Checks semantic applicability of this visualization method to the
	 * attributes previously set in the visualizer. Uses the DataInformer to
	 * get semantic information about the attributes.
	 * By default, returns true.
	 */
	@Override
	public boolean checkSemantics() {
		return true;
	}

	/**
	 * The Visualizer sets its parameters.
	 * By default, does nothing.
	 */
	@Override
	public void setup() {
	}

	/**
	* Reacts to changes of classification
	*/
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		notifyVisChange();
	}

//ID
	@Override
	public Hashtable getVisProperties() {
		Hashtable param;
		param = super.getVisProperties();
		if (param == null) {
			param = new Hashtable();
		}

		checkCreateSymbol();
		param.put("width", String.valueOf(symbol.getWidth()));
		param.put("height", String.valueOf(symbol.getHeight()));

		return param;
	}

	@Override
	public void setVisProperties(Hashtable param) {
		float temp;
//    temp = new Float((String)param.get("dataMin")).floatValue();
//    if (!Float.isNaN(temp)) dataMin = temp;
//    temp = new Float((String)param.get("dataMax")).floatValue();
		checkCreateSymbol();
		try {
			symbol.setHeight(new Integer((String) param.get("height")).intValue());
		} catch (Exception ex) {
		}
		try {
			symbol.setWidth(new Integer((String) param.get("width")).intValue());
		} catch (Exception ex) {
		}

		super.setVisProperties(param);
//    setDataMinMax(dataMin, dataMax);
//    setFocuserMinMax(focuserMin, focuserMax);
	}
//~ID
}
