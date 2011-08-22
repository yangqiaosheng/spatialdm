package spade.vis.mapvis;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.SelectDialog;
import spade.lib.basicwin.StringInRectangle;
import spade.lib.color.ColorDlg;
import spade.lib.help.Helper;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeTypes;
import spade.vis.database.ThematicDataItem;
import spade.vis.geometry.SimpleSign;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Mar 8, 2010
 * Time: 1:27:13 PM
 * Represents values of a qualitative attribute by simple icons varying only in shape
 */
public class SimpleSignPresenter extends DataPresenter {
	/**
	 * The Sign instance used for drawing
	 */
	protected SimpleSign sign = null;
	/**
	* Vector of different values of the attribute
	*/
	protected Vector vals = null;
	/**
	 * The shapes to represent the values
	 */
	protected IntArray shapes = null;
	/**
	 * The number of occurrences of each value
	 */
	protected int[] valueCounts = null;
	/**
	 * Whether this visualizer is allowed to change the color of the icons
	 */
	protected boolean mayChangeColor = true;
	/**
	 * Whether this visualizer is allowed to change the size of the icons
	 */
	protected boolean mayChangeSize = true;
	/**
	 * What values will be shown in the legend
	 */
	protected boolean showInLegend[] = null;
	/**
	 * The default color; if defined, must be used in the legend
	 */
	protected Color defaultColor = null;

	/**
	 * Whether this visualizer is allowed to change the color of the icons
	 */
	public void setMayChangeColor(boolean mayChangeColor) {
		this.mayChangeColor = mayChangeColor;
	}

	/**
	 * Whether this visualizer is allowed to change the size of the icons
	 */
	public void setMayChangeSize(boolean mayChangeSize) {
		this.mayChangeSize = mayChangeSize;
	}

	/**
	 * The default color; if defined, must be used in the legend
	 */
	public Color getDefaultColor() {
		return defaultColor;
	}

	/**
	 * The default color; if defined, must be used in the legend
	 */
	public void setDefaultColor(Color defaultColor) {
		this.defaultColor = defaultColor;
	}

	/**
	* Must reply whether any parameters of this visualization method may be
	* changed, e.g. colors or sizes of signs. This does not include interactive
	* analytical manipulation.
	*/
	@Override
	public boolean canChangeParameters() {
		return mayChangeColor || mayChangeSize;
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
	* Checks if this visualization method is applicable to the given set of
	* attributes. May use the DataInformer to check types and values
	* of the attributes.
	**/
	@Override
	public boolean isApplicable(Vector attr) {
		err = null;
		if (attr == null || attr.size() < 1) {
			err = errors[0];
			return false;
		}
		if (attr.size() > 1) {
			err = errors[6];
			return false;
		}
		if (table == null) {
			err = errors[1];
			return false;
		}
		char at = table.getAttributeType((String) attr.elementAt(0));
		if (!AttributeTypes.isNominalType(at) && AttributeTypes.isIntegerType(at))
			if (!table.isValuesCountBelow(attr, Helper.getMaxNumValuesForQualitativeVis())) {
				err = attr.elementAt(0) + ": " + errors[10];
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
		showInLegend = null;
		if (vals == null && shapes == null) {
			if (!isApplicable(attr))
				return;
			makeInitialAssignment();
		} else {
			valueCounts = null;
			Vector v1 = table.getAllAttrValuesAsStrings((String) attr.elementAt(0));
			if (v1 == null || v1.size() < 1) {
				vals = null;
				shapes = null;
				return;
			}
			for (int i = vals.size() - 1; i >= 0; i--)
				if (!StringUtil.isStringInVectorIgnoreCase((String) vals.elementAt(i), v1)) {
					vals.removeElementAt(i);
					shapes.removeElementAt(i);
				}
			for (int i = 0; i < v1.size(); i++)
				if (!StringUtil.isStringInVectorIgnoreCase((String) v1.elementAt(i), vals)) {
					vals.addElement(v1.elementAt(i));
					shapes.addElement(getSomeShape());
				}
		}
	}

	protected int getSomeShape() {
		if (shapes == null || shapes.size() < 1)
			return SimpleSign.SHAPE_FIRST;
		for (int i = SimpleSign.SHAPE_FIRST; i <= SimpleSign.SHAPE_LAST; i++)
			if (shapes.indexOf(i) < 0)
				return i;
		return (int) Math.round(Math.random() * SimpleSign.SHAPE_LAST);
	}

	/**
	* Retrieves all the values of the attribute
	*/
	protected void makeInitialAssignment() {
		if (table == null || attr == null)
			return;
		vals = table.getAllAttrValuesAsStrings((String) attr.elementAt(0));
		if (vals == null || vals.size() < 1)
			return;
		shapes = new IntArray(vals.size(), 10);
		for (int i = 0; i < vals.size(); i++) {
			shapes.addElement(Math.min(i, SimpleSign.SHAPE_LAST));
		}
		valueCounts = null;
	}

	protected void checkCreateSign() {
		if (sign == null) {
			sign = new SimpleSign();
		}
	}

	/**
	 * Returns the shape corresponding to the attribute value with the given index
	 */
	public int getShapeForValue(int valIdx) {
		if (shapes == null)
			return -1;
		if (valIdx < 0 || valIdx >= shapes.size())
			return -1;
		return shapes.elementAt(valIdx);
	}

	public void setShapeForValue(int shape, int valIdx) {
		if (shapes == null)
			return;
		if (valIdx < 0 || valIdx >= shapes.size())
			return;
		if (shapes.elementAt(valIdx) == shape)
			return;
		shapes.setElementAt(shape, valIdx);
		notifyVisChange();
	}

	/**
	 * Generates and returns an instance of SimpleSign for the
	 * attribute value with the given index
	 */
	public SimpleSign getSignForValue(int valIdx) {
		if (shapes == null)
			return null;
		if (valIdx < 0 || valIdx >= shapes.size())
			return null;
		checkCreateSign();
		SimpleSign s = new SimpleSign();
		s.setWidth(sign.getWidth());
		s.setHeight(sign.getHeight());
		s.setColor(sign.getColor());
		s.setShape(shapes.elementAt(valIdx));
		return s;
	}

	/**
	 * Generates and returns an instance of SimpleSign with the specified shape
	 */
	public SimpleSign getSignWithShape(int shape) {
		checkCreateSign();
		SimpleSign s = new SimpleSign();
		s.setWidth(sign.getWidth());
		s.setHeight(sign.getHeight());
		s.setColor(sign.getColor());
		s.setShape(shape);
		return s;
	}

	/**
	* Draws a small picture symbolizing this presentation technique
	*/
	@Override
	public void drawIcon(Graphics g, int x, int y, int w, int h) {
		checkCreateSign();
		sign.setShape(SimpleSign.STAR);
		if (defaultColor != null) {
			sign.setColor(defaultColor);
		}
		sign.draw(g, x, y, w, h);
	}

	/**
	* This method is called by a GeoObject. The GeoObject sends its DataItem
	* to the Visualizer, and the Visualizer returns an Object specifying the
	* presentation of data contained in this DataItem. This may be, in
	* particular, Color or Diagram.
	*/
	@Override
	public Object getPresentation(ThematicDataItem dit) {
		if (dit == null || attr == null)
			return null;
		if (vals == null || shapes == null) {
			makeInitialAssignment();
		}
		if (vals == null || vals.size() < 1)
			return null;
		String val = getStringAttrValue(dit, 0);
		if (val == null)
			return null;
		int idx = StringUtil.indexOfStringInVectorIgnoreCase(val, vals);
		if (idx < 0)
			return null;
		checkCreateSign();
		sign.setShape(shapes.elementAt(idx));
		return sign;
	}

	protected int[] countValues() {
		if (vals == null) {
			makeInitialAssignment();
		}
		if (vals == null || vals.size() < 1)
			return null;
		int attrN = table.getAttrIndex((String) attr.elementAt(0));
		int counts[] = new int[vals.size()];
		for (int i = 0; i < counts.length; i++) {
			counts[i] = 0;
		}
		for (int i = 0; i < table.getDataItemCount(); i++) {
			String val = table.getAttrValueAsString(attrN, i);
			int idx = StringUtil.indexOfStringInVectorIgnoreCase(val, vals);
			if (idx >= 0) {
				++counts[idx];
			}
		}
		return counts;
	}

	public Vector getValues() {
		if (vals == null) {
			makeInitialAssignment();
		}
		return vals;
	}

	public int[] getValueCounts() {
		if (valueCounts == null) {
			valueCounts = countValues();
		}
		return valueCounts;
	}

	/**
	 * What values are shown in the legend
	 */
	public boolean[] getShowInLegend() {
		return showInLegend;
	}

	/**
	 * Sets what values will be shown in the legend
	 */
	public void setShowInLegend(boolean[] showInLegend) {
		this.showInLegend = showInLegend;
	}

	/**
	* Draws the part of the legend explaining this specific presentation method.
	*/
	@Override
	public Rectangle drawMethodSpecificLegend(Graphics g, int startY, int leftmarg, int prefW) {
		if (table == null || attr == null)
			return new Rectangle(leftmarg, startY, 0, 0);
		if (vals == null || shapes == null) {
			makeInitialAssignment();
		}
		Point p = StringInRectangle.drawText(g, table.getAttributeName((String) attr.elementAt(0)), leftmarg, startY, prefW, true);
		int y = p.y, maxX = p.x;
		if (vals == null || vals.size() < 1)
			return new Rectangle(leftmarg, startY, maxX - leftmarg, y - startY);
		if (valueCounts == null) {
			valueCounts = countValues();
		}
		checkCreateSign();
		if (defaultColor != null) {
			sign.setColor(defaultColor);
		}
		FontMetrics fm = g.getFontMetrics();
		int asc = fm.getAscent(), step = asc + 5, rectW = Metrics.mm() * 4, ssx = leftmarg + rectW + Metrics.mm();
		y += asc / 2;
		if (step < sign.getHeight() + 2) {
			step = sign.getHeight() + 2;
		}
		int x = ssx;
		if (x > maxX) {
			maxX = x;
		}
		int y0 = y, mx = ssx;
		if (showInLegend != null && showInLegend.length != vals.size()) {
			showInLegend = null;
		}
		for (int i = 0; i < vals.size(); i++) {
			boolean toShow = showInLegend == null || showInLegend[i];
			if (!toShow) {
				continue;
			}
			sign.setShape(shapes.elementAt(i));
			sign.draw(g, leftmarg, y, rectW, step);
			y += step + 2;
		}
		g.setColor(Color.black);
		if (maxX < mx) {
			maxX = mx;
		}
		int nTotal = table.getDataItemCount();
		if (nTotal > 0) {
			y = y0 + step / 2 + asc / 2;
			mx += 2 * Metrics.mm();
			for (int i = 0; i < valueCounts.length; i++) {
				boolean toShow = showInLegend == null || showInLegend[i];
				if (!toShow) {
					continue;
				}
				float perc = 100.0f * valueCounts[i] / nTotal;
				String str = vals.elementAt(i) + ": " + String.valueOf(valueCounts[i]) + " objects (" + StringUtil.floatToStr(perc, 0.0f, 100.0f) + "%)";
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

	/**
	* Constructs and displays a dialog for changing parameters of this
	* visualization method: color and size of the signs.
	*/
	@Override
	public void startChangeParameters() {
		boolean changeColor = mayChangeColor;
		if (mayChangeColor && mayChangeSize) {
			SelectDialog selDia = new SelectDialog(CManager.getAnyFrame(), "Symbol parameters", "What to modify?");
			selDia.addOption("color", "color", true);
			selDia.addOption("size", "size", false);
			selDia.show();
			changeColor = selDia.getSelectedOptionN() == 0;
		}
		if (changeColor) {
			ColorDlg cDlg = new ColorDlg(CManager.getAnyFrame(), "Icon color");
			cDlg.selectColor(null, null, sign.getColor());
			if (!cDlg.wasCancelled()) {
				sign.setColor(cDlg.getColor());
				notifyVisChange();
			}
		} else {
			SimpleSign tmp = new SimpleSign();
			tmp.setColor(sign.getColor());
			tmp.setHeight(sign.getHeight());
			tmp.setWidth(sign.getWidth());
			tmp.setShape(SimpleSign.CIRCLE);
			SymbolSizeChanger ssc = new SymbolSizeChanger(tmp, 5, 50, Math.min(tmp.getWidth(), tmp.getHeight()));
			OKDialog dia = new OKDialog(CManager.getAnyFrame(), "Symbol size", true);
			dia.addContent(ssc);
			dia.show();
			if (!dia.wasCancelled()) {
				sign.setSize(tmp.getWidth());
				notifyVisChange();
			}
		}
	}

}
