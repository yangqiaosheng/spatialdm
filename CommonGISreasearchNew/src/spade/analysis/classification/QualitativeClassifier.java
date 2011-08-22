package spade.analysis.classification;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.PopupManager;
import spade.lib.basicwin.StringInRectangle;
import spade.lib.color.CS;
import spade.lib.color.ColorCanvas;
import spade.lib.color.ColorDlg;
import spade.lib.help.Helper;
import spade.lib.lang.Language;
import spade.lib.util.BubbleSort;
import spade.lib.util.IntArray;
import spade.lib.util.QSortAlgorithm;
import spade.lib.util.StringUtil;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.ThematicDataItem;
import spade.vis.spec.AttrValueColorPrefSpec;
import spade.vis.spec.AttrValueOrderPrefSpec;
import spade.vis.spec.DataSourceSpec;

/**
* ==========================================================================
* last updates:
* ==========================================================================
* => hdz, 03.2004:
*   ---------------
*   - new function isNumerical() to afford saving integers as qualitative values
*   - new function setAttributeTypeNumerical(boolean) to change the attributetype to integer
*   - change function storeClassNamesAndColors(): Attribute type (I) also possible*
* => hdz, 04.2004
*   - applicable for few numeric values
* ==========================================================================
**/

public class QualitativeClassifier extends TableClassifier implements SingleAttributeClassifier, ActionListener, spade.lib.util.Comparator {
	static ResourceBundle res = Language.getTextResource("spade.analysis.classification.Res");

	/**
	* Vector of different values of the attribute
	*/
	protected Vector vals = null;

	/**
	* Colors for representing values
	*/
	protected Vector colors = null;

	/** hdz added to make possible to also save integers as "Qualitative" values
	 *  03.2004
	 */
	private boolean isNumAttributes = false;

	public QualitativeClassifier() {
	}

	public QualitativeClassifier(AttributeDataPortion data, String attrId) {
		this();
		if (data != null) {
			setTable(data);
		}
		setAttribute(attrId);
	}

	/**
	 * hdz added sets the member numAttributes as false
	 * 03.2004
	 * @param numericalType boolean
	 */
	public void setAttributeTypeNumerical(boolean numericalType) {
		isNumAttributes = numericalType;
	}

	/**
	* Sets the attribute to be used for classification.
	*/
	public void setAttribute(String attrId) {
		Vector v = new Vector(1, 1);
		v.addElement(attrId);
		setAttributes(v);
		vals = null;
	}

	/**
	* Returns the identifier of the qualitative attribute used for
	* classification
	*/
	@Override
	public String getAttrId() {
		if (attr == null || attr.size() < 1)
			return null;
		return (String) attr.elementAt(0);
	}

	/**
	 * Returns the index (column number) in the table of the attribute used for
	 * the classification
	 */
	@Override
	public int getAttrColumnN() {
		return super.getAttrColumnN(0);
	}

	/**
	* Checks if this visualization method is applicable to the specified number
	* of attributes having the specified types. The possible types see in
	* @see spade.vis.database.AttributeTypes
	* hdz, added true for numerical types with few Arguments
	*/
	@Override
	public boolean isApplicable(int attrNumber, char attrTypes[]) {
//    return attrNumber==1 && AttributeTypes.isNominalType(attrTypes[0] );

		boolean testNumerical = data.isValuesCountBelow(attr, Helper.getMaxNumValuesForQualitativeVis());
		return (attrNumber == 1 && (AttributeTypes.isNominalType(attrTypes[0]) || (testNumerical && AttributeTypes.isNumericType(attrTypes[0]))));
	}

	/**
	* Informs whether this classifier allows the classified attributes to
	* be transformed. This classifier returns false.
	*/
	@Override
	public boolean getAllowTransform() {
		return false;
	}

	public String getAttributeName() {
		return getAttributeName(0);
	}

	/**
	* Retrieves all the values of the attribute
	*/
	protected void makeValueList() {
		Attribute attr = data.getAttribute(getAttrId());
		AttrValueColorPrefSpec cps = null;
		AttrValueOrderPrefSpec aOrdPref = null;
		if (data.getDataSource() != null && (data.getDataSource() instanceof DataSourceSpec)) {
			DataSourceSpec spec = (DataSourceSpec) data.getDataSource();
			if (spec.descriptors != null) {
				for (int i = 0; i < spec.descriptors.size() && (cps == null || aOrdPref == null); i++)
					if (cps == null && (spec.descriptors.elementAt(i) instanceof AttrValueColorPrefSpec)) {
						AttrValueColorPrefSpec cps0 = (AttrValueColorPrefSpec) spec.descriptors.elementAt(i);
						if (cps0.attrName != null && cps0.attrName.equalsIgnoreCase(attr.getName())) {
							cps = cps0;
						}
					} else if (aOrdPref == null && (spec.descriptors.elementAt(i) instanceof AttrValueOrderPrefSpec)) {
						AttrValueOrderPrefSpec aOrdPref0 = (AttrValueOrderPrefSpec) spec.descriptors.elementAt(i);
						if (aOrdPref0.attrName != null && aOrdPref0.attrName.equalsIgnoreCase(attr.getName())) {
							aOrdPref = aOrdPref0;
						}
					}
			}
		}
		vals = getAllAttrValues(0);
		if (aOrdPref != null && aOrdPref.values != null && aOrdPref.values.size() > 1) {
			Vector ordVals = new Vector(vals.size(), 10);
			for (int i = 0; i < aOrdPref.values.size(); i++)
				if (StringUtil.isStringInVectorIgnoreCase((String) aOrdPref.values.elementAt(i), vals)) {
					ordVals.addElement(aOrdPref.values.elementAt(i));
				}
			if (ordVals.size() < vals.size()) {
				for (int i = 0; i < vals.size(); i++)
					if (!StringUtil.isStringInVectorIgnoreCase((String) vals.elementAt(i), ordVals)) {
						ordVals.addElement(vals.elementAt(i));
					}
			}
			vals = ordVals;
		} else {
			BubbleSort.sort(vals, this);
		}
		if (!attr.hasChildren() && attr.isClassification()) {
			colors = new Vector(vals.size(), 10);
			String str[] = new String[vals.size()], oldVals[] = attr.getValueList();
			Color oldC[] = attr.getValueColors(), c[] = new Color[vals.size()];
			for (int i = 0; i < vals.size(); i++) {
				str[i] = (String) vals.elementAt(i);
				c[i] = null;
				if (oldVals != null && oldC != null) {
					for (int j = 0; j < oldVals.length && c[i] == null; j++)
						if (str[i].equalsIgnoreCase(oldVals[j])) {
							c[i] = oldC[j];
						}
				}
				if (c[i] == null)
					if (i < CS.niceColors.length) {
						c[i] = CS.getNiceColor(i);
					} else if (i < CS.niceColors.length * 3) {
						c[i] = c[i - CS.niceColors.length].darker();
					} else {
						c[i] = Color.getHSBColor((float) Math.random(), (float) Math.max(Math.random(), 0.5), (float) Math.max(Math.random(), 0.5));
					}
				colors.addElement(c[i]);
			}
			attr.setValueListAndColors(str, c);
		} else {
			setupAllColors();
		}

		if (cps != null && cps.colorPrefs != null && cps.colorPrefs.size() > 0) {
			//set the colors of the classes according to the preferences
			for (int i = 0; i < cps.colorPrefs.size(); i++) {
				Object pair[] = (Object[]) cps.colorPrefs.elementAt(i);
				if ((pair[0] instanceof String) && (pair[1] instanceof Color)) {
					setColorForValue((String) pair[0], (Color) pair[1]);
				}
			}
		}
	}

	/**
	 * Compares two string values. First attempts to transform them to numbers
	 */
	@Override
	public int compare(Object o1, Object o2) {
		if (o1 == null || o2 == null)
			return 0;
		if (!(o1 instanceof String) || !(o2 instanceof String))
			return 0;
		String st1 = (String) o1, st2 = (String) o2;
		float v1 = Float.NaN, v2 = Float.NaN;
		try {
			v1 = Float.parseFloat(st1);
		} catch (NumberFormatException e) {
		}
		try {
			v2 = Float.parseFloat(st2);
		} catch (NumberFormatException e) {
		}
		if (Float.isNaN(v1))
			if (Float.isNaN(v2))
				return st1.compareTo(st2);
			else
				return 1;
		if (Float.isNaN(v2))
			return -1;
		if (v1 < v2)
			return -1;
		if (v1 > v2)
			return 1;
		return 0;
	}

	/**
	* Prepares its internal variables to the classification.
	*/
	@Override
	public void setup() {
		vals = null;
		objClassNumbers = null;
		makeValueList();
		notifyClassesChange();
	}

	/**
	* Adds a new class (used for freehand classification).
	* Does not notify the change of the classes.
	*/
	public void addClass(String className) {
		if (className == null)
			return;
		if (vals == null) {
			vals = new Vector(20, 5);
		}
		vals.addElement(className);
		if (colors == null) {
			colors = new Vector(20, 5);
		}
		for (int i = colors.size(); i < vals.size(); i++) {
			colors.addElement(CS.getNiceColorExt(i));
		}
		storeClassNamesAndColors();
	}

	/**
	* Removes the class with the given index (used for freehand classification).
	* Before calling this method, it must be ensured that the table column does
	* not contain this value any more.
	* Does not notify the change of the classes.
	*/
	public void removeClass(int classIdx) {
		if (vals == null || classIdx < 0 || classIdx >= vals.size())
			return;
		vals.removeElementAt(classIdx);
		if (colors != null && classIdx < colors.size()) {
			colors.removeElementAt(classIdx);
		}
		storeClassNamesAndColors();
	}

	/**
	* Sets the classes in the given order. Used for freehand classification.
	* Notifier the listeners about the change of the classes.
	*/
	public void setClasses(Vector classNames, Vector classColors) {
		if (classNames == null || classNames.size() < 1)
			return;
		vals = (Vector) classNames.clone();
		if (classColors != null) {
			colors = (Vector) classColors.clone();
		} else {
			colors = new Vector(20, 5);
		}
		if (colors.size() < vals.size()) {
			for (int i = colors.size(); i < vals.size(); i++) {
				colors.addElement(CS.getNiceColorExt(i));
			}
		}
		storeClassNamesAndColors();
		notifyClassesChange();
	}

	/**
	* Determines the class the record with the given number belongs to
	*/
	@Override
	public int getRecordClass(ThematicDataItem dit) {
		if (dit == null)
			return -1;
		if (vals == null) {
			makeValueList();
		}
		if (vals == null)
			return -1;
		int colN = getAttrColumnN(0);
		if (colN < 0)
			return -1;
		String str = dit.getAttrValueAsString(colN);
		if (str == null || str.trim().length() == 0)
			return -1;
		str = str.trim();
		return StringUtil.indexOfStringInVectorIgnoreCase(str, vals);
	}

	/**
	 * Returns the color corresponding to the given attribute value
	 */
	@Override
	public Color getColorForValue(Object value) {
		if (value == null || vals == null)
			return null;
		int classN = StringUtil.indexOfStringInVectorIgnoreCase(value.toString(), vals);
		if (classN < 0)
			return null;
		return getClassColor(classN);
	}

	@Override
	public int getNClasses() {
		if (vals == null) {
			makeValueList();
		}
		if (vals == null)
			return 0;
		return vals.size();
	}

	@Override
	public String getClassName(int classN) {
		if (classN < 0 || classN >= getNClasses())
			return null;
		return (String) vals.elementAt(classN);
	}

	public void setClassName(String name, int classN) {
		if (name == null || classN < 0 || classN >= getNClasses())
			return;
		vals.setElementAt(name, classN);
		storeClassNamesAndColors();
	}

	@Override
	public Color getClassColor(int classN) {
		if (classN < 0 || classN >= getNClasses())
			return null;
		if (colors == null) {
			setupAllColors();
		}
		while (classN >= colors.size()) {
			colors.addElement(CS.getNiceColorExt(classN));
		}
		return (Color) colors.elementAt(classN);
	}

	/**
	* A LegendDrawer should be able to add its description at the end of the
	* legend formed by previous legend drawers. The argument startY specifies
	* the vertical position from which the LegendDrawer should start drawing
	* its part of the legend.The argument leftMarg specifies the left margin
	* (amount of space on the left to be kept blank). The argument prefW
	* specifies the preferrable width of the legend (to avoid horizontal
	* scrolling).
	* The method should return the rectangle occupied by the drawn part of
	* the legend.
	*/
	@Override
	public Rectangle drawLegend(Component c, Graphics g, int startY, int leftmarg, int prefW) {
		return drawClassStatistics(g, startY, leftmarg, prefW);
	}

	@Override
	public Rectangle drawClassStatistics(Graphics g, int startY, int leftmarg, int prefW) {
		if (data == null || getAttrColumnN(0) < 0)
			return new Rectangle(leftmarg, startY, 0, 0);
		if (vals == null) {
			makeValueList();
		}
		Point p = StringInRectangle.drawText(g, getAttributeName(), leftmarg, startY, prefW, true);
		int y = p.y, maxX = p.x;
		if (vals == null || vals.size() == 0)
			return new Rectangle(leftmarg, startY, maxX - leftmarg, y - startY);
		FontMetrics fm = g.getFontMetrics();
		int asc = fm.getAscent(), step = asc + 5, rectW = Metrics.mm() * 4, ssx = leftmarg + rectW + Metrics.mm();
		y += asc / 2;
		int x = ssx;
		if (x > maxX) {
			maxX = x;
		}
		int y0 = y, mx = ssx;
		if (showInLegend != null && showInLegend.length != getNClasses()) {
			showInLegend = null;
		}
		for (int i = 0; i < getNClasses(); i++) {
			boolean toShow = showInLegend == null || showInLegend[i];
			if (!toShow) {
				continue;
			}
			g.setColor(getClassColor(i));
			g.fillRect(leftmarg, y, rectW, step);
			g.setColor(Color.black);
			g.drawRect(leftmarg, y, rectW, step);
			y += step + 2;
		}
		if (maxX < mx) {
			maxX = mx;
		}
		int nTotal = getSetSize();
		if (nTotal > 0) {
			y = y0 + step / 2 + asc / 2;
			mx += 2 * Metrics.mm();
			IntArray counts = getClassSizes(), countsFlt = getFilteredClassSizes();
			for (int i = 0; i < getNClasses(); i++) {
				boolean toShow = showInLegend == null || showInLegend[i];
				if (!toShow) {
					continue;
				}
				float perc = 100.0f * counts.elementAt(i) / nTotal;
				String str = getClassName(i) + ": " + String.valueOf(counts.elementAt(i)) + " objects (" + StringUtil.floatToStr(perc, 0.0f, 100.0f) + "%)";
				if (countsFlt != null && counts.elementAt(i) != countsFlt.elementAt(i)) {
					perc = 100.0f * countsFlt.elementAt(i) / counts.elementAt(i);
					str += "; active: " + countsFlt.elementAt(i) + " objects (" + StringUtil.floatToStr(perc, 0.0f, 100.0f) + "%)";
				}
				g.drawString(str, mx, y);
				x = mx + fm.stringWidth(str);
				if (x > maxX) {
					maxX = x;
				}
				y += step + 2;
			}
		} else {
			y += asc;
		}
		return new Rectangle(leftmarg, startY, maxX - leftmarg + Metrics.mm(), y - startY);
	}

	// hdz 2004.04 added sorting of the numerical attributes
	protected void setupAllColors() {
		if (vals == null || vals.size() < 1) {
			if (colors != null) {
				colors.removeAllElements();
			}
			return;
		}
		Attribute attr = data.getAttribute(getAttrId());
		if (attr.getValueList() == null) {
			String v[] = new String[vals.size()];
			for (int i = 0; i < vals.size(); i++) {
				v[i] = (String) vals.elementAt(i);
			}
			// hdz added sorting of values if they are integers
			if (attr.isNumeric()) {
				QSortAlgorithm.sort_as_number(v);
			}
			attr.setValueListAndColors(v, null);
		}
		if (attr.getValueColors() == null) {
			attr.setupDefaultColors();
		}
		if (attr.getValueColors() == null)
			return;
		colors = new Vector(attr.getValueColors().length, 5);
		vals.removeAllElements();
		for (int i = 0; i < attr.getValueColors().length; i++) {
			colors.addElement(attr.getValueColors()[i]);
			vals.addElement(attr.getValueList()[i]);
		}
	}

	@Override
	public void getValueColorsFromAttribute() {
		Attribute attr = data.getAttribute(getAttrId());
		if (attr == null)
			return;
		Color c[] = attr.getValueColors();
		if (c == null)
			return;
		String v[] = attr.getValueList();
		if (colors == null) {
			colors = new Vector(c.length, 10);
		} else {
			colors.removeAllElements();
		}
		if (vals == null) {
			vals = new Vector(c.length, 10);
		} else {
			vals.removeAllElements();
		}
		for (int i = 0; i < c.length; i++) {
			colors.addElement(c[i]);
			vals.addElement(v[i]);
		}
	}

	/**
	* Makes the attribute store the current class names and colors for further use
	*/
	protected void storeClassNamesAndColors() {
		if (vals != null && vals.size() > 0) {
			Attribute attr = data.getAttribute(getAttrId());
			if (attr != null && !attr.hasChildren()) {
				String v[] = new String[vals.size()];
				Color c[] = new Color[vals.size()];
				for (int i = 0; i < vals.size(); i++) {
					v[i] = (String) vals.elementAt(i);
					c[i] = getClassColor(i);
				}
				if (this.isNumAttributes) { //hdz added
					attr.setType('I');
				}
				attr.setValueListAndColors(v, c);
			}
			if (data.getDataSource() != null && (data.getDataSource() instanceof DataSourceSpec)) {
				DataSourceSpec spec = (DataSourceSpec) data.getDataSource();
				AttrValueColorPrefSpec cps = null;
				AttrValueOrderPrefSpec aOrdPref = null;
				if (spec.descriptors != null) {
					for (int i = 0; i < spec.descriptors.size() && (cps == null || aOrdPref == null); i++)
						if (cps == null && (spec.descriptors.elementAt(i) instanceof AttrValueColorPrefSpec)) {
							cps = (AttrValueColorPrefSpec) spec.descriptors.elementAt(i);
							if (cps.attrName == null || !cps.attrName.equalsIgnoreCase(attr.getName())) {
								cps = null;
							}
						} else if (aOrdPref == null && (spec.descriptors.elementAt(i) instanceof AttrValueOrderPrefSpec)) {
							aOrdPref = (AttrValueOrderPrefSpec) spec.descriptors.elementAt(i);
							if (aOrdPref.attrName == null || !aOrdPref.attrName.equalsIgnoreCase(attr.getName())) {
								aOrdPref = null;
							}
						}
				}
				if (spec.descriptors == null) {
					spec.descriptors = new Vector(5, 5);
				}
				if (cps == null) {
					cps = new AttrValueColorPrefSpec();
					cps.attrName = attr.getName();
					spec.descriptors.addElement(cps);
				}
				if (cps.colorPrefs == null) {
					cps.colorPrefs = new Vector(vals.size(), 10);
				} else {
					cps.colorPrefs.removeAllElements();
				}
				if (aOrdPref == null) {
					aOrdPref = new AttrValueOrderPrefSpec();
					aOrdPref.attrName = attr.getName();
					spec.descriptors.addElement(aOrdPref);
				}
				if (aOrdPref.values == null) {
					aOrdPref.values = new Vector(vals.size(), 10);
				} else {
					aOrdPref.values.removeAllElements();
				}
				for (int i = 0; i < vals.size(); i++) {
					aOrdPref.values.addElement(vals.elementAt(i));
					Object pair[] = new Object[2];
					pair[0] = vals.elementAt(i);
					pair[1] = getClassColor(i);
					cps.colorPrefs.addElement(pair);
				}
			}
		}
	}

	@Override
	public boolean allowChangeClassColor() {
		return true;
	}

	@Override
	public void setColorForClass(int classN, Color color) {
		setColorForClass(classN, color, true);
	}

	public void setColorForClass(int classN, Color color, boolean notifyChange) {
		if (colors == null) {
			colors = new Vector(10, 5);
		}
		while (classN >= colors.size()) {
			colors.addElement(Color.getHSBColor((float) Math.random(), (float) Math.random(), (float) Math.random()));
		}
		colors.setElementAt(color, classN);
		storeClassNamesAndColors();
		if (notifyChange) {
			notifyChange("colors", null);
		}
	}

	public void setColorForValue(String value, Color color) {
		if (value == null || color == null)
			return;
		if (vals == null) {
			makeValueList();
		}
		if (vals == null)
			return;
		int idx = StringUtil.indexOfStringInVectorIgnoreCase(value, vals);
		if (idx < 0)
			return;
		colors.setElementAt(color, idx);
	}

	/**
	* Checks if new values appeared in data and if this affects the classes
	*/
	@Override
	protected void checkValues() {
		if (data == null || data.getDataItemCount() < 1)
			return;
		if (vals == null) {
			vals = new Vector(10, 10);
		}
		makeValueList();
		/*
		Vector newVals=getAllAttrValues(0);
		if (newVals==null) return;
		for (int i=0; i<newVals.size(); i++)
		  if (!vals.contains(newVals.elementAt(i))) {
		    vals.addElement(newVals.elementAt(i));
		    if (colors!=null && colors.size()<vals.size())
		      colors.addElement(Color.getHSBColor((float)Math.random(),(float)Math.random(),(float)Math.random()));
		  }
		if (colors==null) setupAllColors();
		*/
	}

	/**
	* A classifier must give the user an opportunity to change interactively
	* colors assigned to classes. This method starts the procedure of class
	* color changing.
	*/
	@Override
	public void startChangeColors() {
		int ncl = getNClasses();
		if (ncl < 1)
			return;
		Panel p = new Panel(new ColumnLayout());
		ColorCanvas cc[] = new ColorCanvas[ncl];
		for (int i = 0; i < ncl; i++) {
			cc[i] = new ColorCanvas();
			cc[i].setColor(getClassColor(i));
			cc[i].setActionListener(this);
			cc[i].setActionCommand(String.valueOf(i));
			new PopupManager(cc[i], res.getString("Click_to_change"), true);
			Panel pp = new Panel(new BorderLayout());
			pp.add(cc[i], BorderLayout.WEST);
			pp.add(new Label(getClassName(i)), BorderLayout.CENTER);
			p.add(pp);
		}
		OKDialog okd = new OKDialog(CManager.getAnyFrame(), res.getString("Color_selection"), true);
		okd.addContent(p);
		okd.show();
		if (okd.wasCancelled())
			return;
		boolean changed = false;
		for (int i = 0; i < ncl; i++)
			if (!cc[i].getColor().equals(getClassColor(i))) {
				changed = true;
				colors.setElementAt(cc[i].getColor(), i);
			}
		if (changed) {
			storeClassNamesAndColors();
			notifyChange("colors", null);
		}
	}

	/**
	* Reacts to a click in a color canvas in the dialog for changing class colors.
	*/
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof ColorCanvas) {
			int cIdx = -1;
			try {
				cIdx = Integer.valueOf(e.getActionCommand()).intValue();
			} catch (NumberFormatException nfe) {
				return;
			}
			if (cIdx < 0 || cIdx >= getNClasses())
				return;
			ColorCanvas cc = (ColorCanvas) e.getSource();
			ColorDlg cDlg = new ColorDlg(CManager.getAnyFrame(null), res.getString("Color_for_") + getClassName(cIdx));
			cDlg.selectColor(null, cc, cc.getColor());
			cc.setColor(cDlg.getColor());
		}
	}

//ID
	@Override
	public Hashtable getVisProperties() {
		Hashtable param;
		param = super.getVisProperties();
		if (param == null) {
			param = new Hashtable();
		}
		if (colors == null) {
			param.put("colors", "null");
		} else {
			StringBuffer sb = new StringBuffer();
			StringBuffer sbAct = new StringBuffer();
			for (int i = 0; i < colors.size(); i++) {
				sb.append(Integer.toHexString(((Color) colors.elementAt(i)).getRGB()).substring(2));
				sb.append(" ");
				sbAct.append(isClassHidden(i) ? "- " : "+ ");
			}
			param.put("colors", sb.toString());
			param.put("activeClasses", sbAct.toString());
		}
		return param;
	}

	@Override
	public void setVisProperties(Hashtable param) {
		String sColors = (String) param.get("colors");
		Vector newColors = new Vector();
		if (sColors == null || sColors.length() < 1 || sColors.equals("null")) {
			colors = null;
		} else {
			StringTokenizer st = new StringTokenizer(sColors, " ");
			while (st.hasMoreTokens()) {
				newColors.addElement(new Color(Integer.parseInt(st.nextToken(), 16)));
			}
			colors = newColors;
		}
		String sActive = (String) param.get("activeClasses");
		if (sActive != null || sActive.length() > 0) {
			StringTokenizer st = new StringTokenizer(sActive, " ");
			if (st.countTokens() == colors.size()) {
				for (int i = 0; st.hasMoreTokens(); i++) {
					setClassIsHidden(st.nextToken().equals("-"), i);
				}
			}
		}
		super.setVisProperties(param);
	}
//~ID

}
