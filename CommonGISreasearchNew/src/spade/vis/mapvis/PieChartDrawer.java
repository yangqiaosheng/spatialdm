package spade.vis.mapvis;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.lib.basicwin.StringInRectangle;
import spade.lib.lang.Language;
import spade.lib.util.DoubleArray;
import spade.lib.util.StringUtil;
import spade.vis.database.DataTable;
import spade.vis.database.ThematicDataItem;
import spade.vis.geometry.Pie;
import spade.vis.geometry.Sign;
import spade.vis.geometry.StructSign;

/**
* Implements visualization of several comparable attributes by bar charts.
*/

public class PieChartDrawer extends MultiNumberDrawer implements AttrColorHandler, SignDrawer {
	static ResourceBundle res = Language.getTextResource("spade.vis.mapvis.Res");
	public static final int mm = Math.round(java.awt.Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f);

	/**
	* An instance of a PieChart
	*/
	protected Pie sign = null;

	double sumMIN = Float.NaN, sumMAX = Float.NaN;
	/**
	* True if pies vary by sizes
	*/
	protected boolean useSize = false;
	/**
	* True if pies can be no less than some specified minimum size
	*/
//ID
	public boolean useMinSize = false;

//~ID

	public double getSumMIN() {
		return sumMIN;
	}

	public double getSumMAX() {
		return sumMAX;
	}

	/**
	* Checks if this visualization method is applicable to the given set of
	* attributes. May use the DataInformer to check types and values
	* of the attributes.
	*/
	@Override
	public boolean isApplicable(Vector attr) {
		if (!super.isApplicable(attr))
			return false;
		for (int i = 0; i < attr.size(); i++)
			if (attr.elementAt(i) != null && getDataMin(i) < 0) {
				err = attr.elementAt(i) + ": " + errors[9];
				return false;
			}
		return true;
	}

	/**
	* Checks semantic applicability of this visualization method to the
	* attributes previously set in the visualizer. Uses the DataInformer to
	* get semantic information about the attributes. Returns true if the
	* attributes are linked by the inclusion relationship.
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
		boolean result = dt.getSemanticsManager().areAttributesIncluded(attr, visName);
		if (!result) {
			err = res.getString("No_inclusion_or");
		}
		return result;
	}

	/**
	* The Visualizer sets its parameters. For this purpose it may need
	* data statistics. Statistics are received from a StatisticsProvider.
	* A reference to a StatisticsProvider may be received from the DataInformer.
	*/
	@Override
	public void setup() {
		if (!isApplicable(attr))
			return;
		if (attr.elementAt(0) == null) {
			sumMIN = Float.NaN;
			sumMAX = Float.NaN;
			int ntimes = 1;
			if (subAttr != null) {
				for (int i = 0; i < subAttr.size(); i++) {
					Vector v = (Vector) subAttr.elementAt(i);
					if (v != null && v.size() > ntimes) {
						ntimes = v.size();
					}
				}
			}
			for (int i = 0; i < table.getDataItemCount(); i++) {
				ThematicDataItem item = (ThematicDataItem) table.getDataItem(i);
				for (int j = 0; j < ntimes; j++) {
					double sum = Double.NaN;
					for (int k = 1; k < attr.size(); k++) {
						int colN = -1;
						if (subAttr == null || subAttr.size() <= k || subAttr.elementAt(k) == null) {
							colN = table.getAttrIndex((String) attr.elementAt(k));
						} else {
							Vector v = (Vector) subAttr.elementAt(k);
							int n = j;
							if (n >= v.size()) {
								n = v.size() - 1;
							}
							colN = table.getAttrIndex((String) v.elementAt(n));
						}
						double val = (aTrans == null) ? item.getNumericAttrValue(colN) : aTrans.getNumericAttrValue(colN, i);
						if (!Double.isNaN(val))
							if (Double.isNaN(sum)) {
								sum = val;
							} else {
								sum += val;
							}
					}
					if (Double.isNaN(sum)) {
						continue;
					}
					if (Double.isNaN(sumMIN) || sumMIN > sum) {
						sumMIN = sum;
					}
					if (Double.isNaN(sumMAX) || sumMAX < sum) {
						sumMAX = sum;
					}
				}
			}
			System.out.println("sumMIN=" + sumMIN + ", sumMAX=" + sumMAX);
		} else {
			sumMIN = getDataMin(0);
			sumMAX = getDataMax(0);
		}
		focuserMIN = sumMIN;
		focuserMAX = sumMAX;
		visCompVals = null;
		visCompPropVals = null;
		visCompObjName = "";
		visCompObjNumber = -1;
		if (sign != null) {
			setupSign(sign);
		}
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
		double minSum = Double.NaN, maxSum = Double.NaN;
		if (attr.elementAt(0) == null) {
			int ntimes = 1;
			if (subAttr != null) {
				for (int i = 0; i < subAttr.size(); i++) {
					Vector v = (Vector) subAttr.elementAt(i);
					if (v != null && v.size() > ntimes) {
						ntimes = v.size();
					}
				}
			}
			for (int i = 0; i < table.getDataItemCount(); i++) {
				ThematicDataItem item = (ThematicDataItem) table.getDataItem(i);
				for (int j = 0; j < ntimes; j++) {
					double sum = Double.NaN;
					for (int k = 1; k < attr.size(); k++) {
						int colN = -1;
						if (subAttr == null || subAttr.size() <= k || subAttr.elementAt(k) == null) {
							colN = table.getAttrIndex((String) attr.elementAt(k));
						} else {
							Vector v = (Vector) subAttr.elementAt(k);
							int n = j;
							if (n >= v.size()) {
								n = v.size() - 1;
							}
							colN = table.getAttrIndex((String) v.elementAt(n));
						}
						double val = (aTrans == null) ? item.getNumericAttrValue(colN) : aTrans.getNumericAttrValue(colN, i);
						if (!Double.isNaN(val))
							if (Double.isNaN(sum)) {
								sum = val;
							} else {
								sum += val;
							}
					}
					if (Double.isNaN(sum)) {
						continue;
					}
					if (Double.isNaN(minSum) || minSum > sum) {
						minSum = sum;
					}
					if (Double.isNaN(maxSum) || maxSum < sum) {
						maxSum = sum;
					}
				}
			}
		} else {
			minSum = getDataMin(0);
			maxSum = getDataMax(0);
		}
		if (minSum == sumMIN && maxSum == sumMAX)
			return false;
		if (!valuesTransformed && minSum >= sumMIN && maxSum <= sumMAX)
			return false;
		if (valuesTransformed || sumMIN > minSum) {
			focuserMIN = sumMIN = minSum;
		}
		if (valuesTransformed || sumMAX < maxSum) {
			focuserMAX = sumMAX = maxSum;
		}
		visCompVals = null;
		visCompPropVals = null;
		visCompObjName = "";
		visCompObjNumber = -1;
		if (sign != null) {
			setupSign(sign);
		}
		return true;
	}

	protected int order[] = null;

	public int[] getOrder() {
		if (order == null) {
			order = new int[attr.size()];
			for (int i = 0; i < attr.size() - 1; i++) {
				order[i] = i + 1;
			}
			order[order.length - 1] = 0;
		}
		return order;
	}

	public void setOrder(int order[]) {
		this.order = order;
		sign.setPieOrder(order);
		notifyVisChange();
	}

	/**
	* Constructs an instance of BarChart for further use depending on the
	* required diagram type
	*/
	protected void checkCreateSign() {
		if (sign == null) {
			sign = new Pie();
			setupSign(sign);
		}
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

	protected void setupSign(Pie sgn) {
		sgn.setMayChangeProperty(Sign.MAX_SIZE, true);
		sgn.setMayChangeProperty(Sign.MIN_SIZE, useSize && useMinSize);
		sgn.setMayChangeProperty(Sign.SEGMENT_ORDER, false);
		sgn.setMayChangeProperty(Sign.USE_FRAME, false);
		sgn.setPieOrder(getOrder());
		int attrN = (attr == null) ? 1 : attr.size();
		sgn.setOrderingMethod(StructSign.OrderPreserved);
		sgn.setNSegments(attrN);
		for (int i = 0; i < attrN; i++) {
			float f = (float) (0.2 + 0.8 * Math.random());
			if (i == 2) {
				f = (sgn.getSegmentPart(0) + sgn.getSegmentPart(1)) / 2;
			}
			if (i == 0 && attr.elementAt(0) == null) {
				f = 0f;
			}
			sgn.setSegmentPart(i, f);
			sgn.setSegmentWeight(i, 1.0f / attrN);
		}
		if (colorHandler == null || attr == null) {
			sgn.setDefaultColors();
		} else {
			for (int i = 0; i < attrN; i++) {
				sgn.setSegmentColor(i, super.getColorForAttribute(i));
			}
		}
	}

	/**
	* Sets the list of identifiers of attributes to be visualized.
	* Additionally, full names of the attributes (if different from the
	* identifiers) may be specified.
	* UtilitySignDrawer redefines the method from the ancestor in order to
	* reset the sign structure
	*/
	@Override
	public void setAttributes(Vector attributes, Vector attrNames) {
		super.setAttributes(attributes, attrNames);
		if (sign != null) {
			setupSign(sign);
		}
	}

	/**
	* Informs whether this visualizer allows the visualized attributes to
	* be transformed. Returns false.
	*/
	@Override
	public boolean getAllowTransform() {
		return false;
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

	protected double MIN = Double.NaN, MAX = Double.NaN;

	protected void setGlobalMinMax() {
		MIN = dataMIN;
		MAX = dataMAX;
		if (MIN > 0) {
			MIN = 0f;
		}
		if (MAX < 0) {
			MAX = 0f;
		}
		if (focuserMIN > MIN) {
			MIN = focuserMIN;
		}
		if (focuserMAX < MAX) {
			MAX = focuserMAX;
		}
	}

	@Override
	public void setFocuserMinMax(double focuserMin, double focuserMax) {
		this.focuserMIN = focuserMin;
		this.focuserMAX = focuserMax;
		setGlobalMinMax();
		notifyVisChange();
	}

	// -- Visual Comparison
//ID
	public String visCompObjName = "";
//~ID
	protected int visCompObjNumber = -1;
	protected double visCompVals[] = null;
	protected double visCompPropVals[] = null;

	public void setVisCompObj(int visCompObjNumber, String objName, double visCompVals[], double visCompPropVals[]) {
		visCompObjName = objName;
		this.visCompObjNumber = visCompObjNumber;
		this.visCompVals = visCompVals;
		this.visCompPropVals = visCompPropVals;
		notifyVisChange();
	}

	public void setUseSize(boolean useSize) {
		this.useSize = useSize;
		sign.setMayChangeProperty(Sign.MAX_SIZE, useSize);
		sign.setMayChangeProperty(Sign.SIZE, !useSize);
		sign.setMayChangeProperty(Sign.MIN_SIZE, useSize && useMinSize);
		notifyVisChange();
	}

	public void setUseMinSize(boolean useMinSize) {
		this.useMinSize = useMinSize;
		sign.setMayChangeProperty(Sign.MIN_SIZE, useSize && useMinSize);
		notifyVisChange();
	}

	/**
	* This method is called by a GeoObject. The GeoObject sends its DataItem
	* to the Visualizer, and the Visualizer returns an Object specifying the
	* presentation of data contained in this DataItem. This may be, in
	* particular, Color or Diagram. Typically the argument DataItem is
	* a ThematicDataItem, although in some cases (e.g. manual classification)
	* this is not required.
	*/
	@Override
	public Object getPresentation(ThematicDataItem dit) {
		if (dit == null || attr == null || attr.size() < 1)
			return null;
		if (sdController != null && !sdController.mustDrawObject(dit.getId()))
			return null;
		if (Double.isNaN(MIN)) {
			setGlobalMinMax();
		}
		if (Double.isNaN(epsilon)) {
			computeComparisonTolerance();
		}
		checkCreateSign();
		sign.setIsTransparent(false);
		sign.setPieOrder(order);
		sign.setVisCompVals(DoubleArray.double2float(visCompVals));
		double sum = 0f;
		double minSignWidth = (useMinSize ? sign.getMinWidth() : 0f), minSignHeight = (useMinSize ? sign.getMinHeight() : 0f);

		for (int i = 1; i < attr.size(); i++)
			if (Double.isNaN(getDataMin(i))) {
				sign.setSegmentPart(i, 0.0f);
			} else {
				double v = getNumericAttrValue(dit, i);
				if (Double.isNaN(v)) {
					v = 0.0f;
				}
				sign.setSegmentPart(i, (float) v);
				sum += v;
			}
		if (attr.elementAt(0) == null) {
			if (sum < focuserMIN - epsilon || sum > focuserMAX + epsilon) {
				sign.setIsTransparent(true);
			}
			if (sum < focuserMIN)
				return null;
			double f = 1f;
			//if (sumMAX-sumMIN>Math.abs(0.001*sumMAX))
			//  f=(sum-sumMIN)/(sumMAX-sumMIN);
			if (useSize && sum <= focuserMAX && focuserMAX - focuserMIN > Math.abs(0.001 * focuserMAX)) {
				f = Math.sqrt((sum - focuserMIN) / (focuserMAX - focuserMIN));
			}
			sign.setSizes((int) Math.round(minSignWidth + f * (sign.getMaxWidth() - minSignWidth)), (int) Math.round(minSignHeight + f * (sign.getMaxHeight() - minSignHeight)));
		} else {
			double v = getNumericAttrValue(dit, 0);
			if (Double.isNaN(v)) {
				if (sum < focuserMIN - epsilon || sum > focuserMAX + epsilon) {
					sign.setIsTransparent(true);
				}
				if (sum < focuserMIN)
					return null;
				sign.setSegmentPart(0, 0f);
				double f = 1f;
				//if (sumMAX-sumMIN>Math.abs(0.001*sumMAX))
				//  f=(sum-sumMIN)/(sumMAX-sumMIN);
				if (useSize && sum <= focuserMAX && focuserMAX - focuserMIN > Math.abs(0.001 * focuserMAX)) {
					f = Math.sqrt((sum - focuserMIN) / (focuserMAX - focuserMIN));
				}
				sign.setSizes((int) Math.round(minSignWidth + f * (sign.getMaxWidth() - minSignWidth)), (int) Math.round(minSignHeight + f * (sign.getMaxHeight() - minSignHeight)));
			} else {
				if (v < focuserMIN - epsilon || v > focuserMAX + epsilon) {
					sign.setIsTransparent(true);
				}
				if (v < focuserMIN)
					return null;
				sign.setSegmentPart(0, (float) (v - sum));
				float f = 1f;
				//if (sumMAX-sumMIN>Math.abs(0.001*sumMAX))
				//  f=(v-sumMIN)/(sumMAX-sumMIN);
				if (useSize && v <= focuserMAX && focuserMAX - focuserMIN > Math.abs(0.001 * focuserMAX)) {
					f = (float) Math.sqrt((v - focuserMIN) / (focuserMAX - focuserMIN));
				}
				sign.setSizes((int) Math.round(minSignWidth + f * (sign.getMaxWidth() - minSignWidth)), (int) Math.round(minSignHeight + f * (sign.getMaxHeight() - minSignHeight)));
			}
		}
		return sign;
	}

	/**
	* Draws a small picture symbolizing this presentation technique
	*/
	@Override
	public void drawIcon(Graphics g, int x, int y, int w, int h) {
		checkCreateSign();
		sign.setPieOrder(order);
		for (int i = 0; i < attr.size(); i++) {
			int n = (order == null) ? i : order[i];
			if (attr.elementAt(n) == null) {
				continue;
			}
			sign.setSegmentPart(n, (1.0f + i) / attr.size());
		}
		int width = sign.getMaxWidth(), height = sign.getMaxHeight();
		sign.setMaxSizes(w - 2, h - 2);
		sign.setSizes(w - 2, h - 2);
		sign.draw(g, x, y, w, h);
		sign.setMaxSizes(width, height);
	}

	/**
	* Draws the part of the legend explaining this specific presentation method.
	*/
	@Override
	public Rectangle drawMethodSpecificLegend(Graphics g, int startY, int leftMarg, int prefW) {
		checkCreateSign();
		int rSize = 8 * mm, gap = 2 * mm, minSize = (useMinSize ? sign.getMinWidth() : 0), maxSize = sign.getMaxWidth(), y = startY, maxX = leftMarg, fontHeight = g.getFontMetrics().getHeight();

		Point p = null;
		for (int i = 0; i < attr.size(); i++) {
			int n = (order == null) ? i : order[i];
			if (attr.elementAt(n) == null) {
				continue;
			}
			Color c = getColorForAttribute(n);
			g.setColor(c);
			g.fillArc(leftMarg, y, rSize, rSize, 135, -90);
			g.setColor(c.darker());
			g.drawArc(leftMarg, y, rSize, rSize, 135, -88);
			g.setColor(Color.black);
			String name = getAttrName(n);
			if (n == 0) {
				name += res.getString("_the_rest_of_");
			}
			p = StringInRectangle.drawText(g, name, leftMarg + rSize + gap, y + rSize / 4 - fontHeight / 2, prefW - rSize - gap, false);
			//y=(p.y-startY>rSize)?p.y:startY+rSize;
			y = p.y + rSize / 4 - fontHeight / 2;
			if (maxX < p.x) {
				maxX = p.x;
			}
		}
		y = p.y + gap;

		boolean focus_L = focuserMAX - focuserMIN < sumMAX - sumMIN && focuserMIN - sumMIN > Math.abs(0.001 * sumMIN), focus_R = focuserMAX - focuserMIN < sumMAX - sumMIN && sumMAX - focuserMAX > Math.abs(0.001 * sumMAX);

		if (useSize) {
			// draw small and big pies representing proportion of sum and size of sign
			p = StringInRectangle.drawText(g, res.getString("Area_is"), leftMarg + gap / 2, y, prefW - gap, false);
			y = p.y + gap / 2;
			for (int j = 0; j < 4; j++) {
				int diam = (j / 2 == 0) ? minSize : maxSize;
				int xpos = (j / 2 == 0) ? leftMarg + gap + maxSize / 2 - minSize / 2 : leftMarg + gap;

				String sValue = StringUtil.doubleToStr(sumMIN, sumMIN, sumMAX);
				switch (j) {
				case 0: {
					sValue = StringUtil.doubleToStr(sumMIN, sumMIN, sumMAX);
					break;
				}
				case 1: {
					sValue = StringUtil.doubleToStr(focuserMIN, focuserMIN, focuserMAX);
					break;
				}
				case 2: {
					sValue = StringUtil.doubleToStr(focuserMAX, focuserMIN, focuserMAX);
					break;
				}
				case 3:
					sValue = StringUtil.doubleToStr(sumMAX, sumMIN, sumMAX);
				}

				g.setColor(Color.lightGray);

				if ((j == 0 && !focus_L) || (j == 3 && !focus_R)) {
					continue;
				}
				if (j != 0 && j != 3) {
					g.fillOval(xpos, y, diam, diam);
				}
				g.setColor(Color.black);
				g.drawOval(xpos, y, diam, diam);
				xpos += (diam / 2 + maxSize / 2 + gap);
				y += (diam / 2 - fontHeight / 2);
				p = StringInRectangle.drawText(g, sValue, xpos, y, prefW - gap - xpos, false);
				y = p.y + (diam / 2 - fontHeight / 2 + gap);
				xpos = leftMarg;
			}
		} else {
			if (focus_R) {
				int diam = maxSize;
				int xpos = leftMarg + gap;
				String comment = null;
				comment = res.getString("from") + StringUtil.doubleToStr(focuserMIN, focuserMIN, focuserMAX) + res.getString("to") + StringUtil.doubleToStr(focuserMAX, focuserMIN, focuserMAX);
				g.setColor(Color.lightGray);
				g.fillOval(xpos, y, diam, diam);
				g.setColor(Color.black);
				g.drawOval(xpos, y, diam, diam);
				xpos += (diam / 2 + maxSize / 2 + gap);
				y += (diam / 2 - fontHeight / 2);
				p = StringInRectangle.drawText(g, comment, xpos, y, prefW - gap - xpos, false);
				y = p.y + (diam / 2 - fontHeight / 2 + gap);
				xpos = leftMarg + gap;

				comment = res.getString("from") + StringUtil.doubleToStr(focuserMAX, focuserMIN, focuserMAX) + res.getString("to") + StringUtil.doubleToStr(sumMAX, sumMIN, sumMAX);
				g.drawOval(xpos, y, diam, diam);
				xpos += (diam / 2 + maxSize / 2 + gap);
				y += (diam / 2 - fontHeight / 2);
				p = StringInRectangle.drawText(g, comment, xpos, y, prefW - gap - xpos, false);
				y = p.y + (diam / 2 - fontHeight / 2 + gap);
				xpos = leftMarg;
			}
		}
		// Visual comparison:
		if (visCompObjNumber > -1) {
			g.setColor(Color.black);
			p = StringInRectangle.drawText(g, res.getString("Visual_comparison_to") + visCompObjName + "\"", leftMarg + gap, y, prefW - gap, false);
			y = p.y + gap;

			for (int i = 0; i < attr.size(); i++) {
				int n = (order == null) ? i : order[i];
				if (attr.elementAt(n) == null) {
					continue;
				}
				g.setColor(sign.getSegmentColor(n));
				g.fillRect(leftMarg, y, rSize / 2, rSize / 2);
				g.setColor(Color.black);
				String name = getAttrName(n);
				if (n == 0) {
					name += res.getString("_the_rest_of_");
				}
				p = StringInRectangle.drawText(g, StringUtil.doubleToStr(visCompPropVals[n], 1) + " %", leftMarg + rSize / 2 + gap, y + rSize / 4 - fontHeight / 2, prefW - rSize - gap, false);
				//y=(p.y-startY>rSize)?p.y:startY+rSize;
				y = p.y + rSize / 4 - fontHeight / 2;
				if (maxX < p.x) {
					maxX = p.x;
				}
			}
		}
		return new Rectangle(leftMarg, startY, maxX - leftMarg, y - startY);
	}

	/**
	* A method from the AttrColorHandler interface.
	* Returns the color used to represent the specified attribute.
	* May return null.
	*/
	@Override
	public Color getColorForAttribute(String attrId) {
		int idx = getAttrIndex(attrId);
		if (idx >= 0)
			return getColorForAttribute(idx);
		return null;
	}

	@Override
	public Color getColorForAttribute(int idx) {
		if (sign != null && idx >= 0 && idx < sign.getNSegments())
			return sign.getSegmentColor(idx);
		return super.getColorForAttribute(idx);
	}

	/**
	* A method from the AttrColorHandler interface.
	* Sets the color to be used to represent the specified attribute.
	*/
	@Override
	public void setColorForAttribute(Color color, String attrId) {
		setColorForAttribute(color, getAttrIndex(attrId));
	}

	@Override
	public void setColorForAttribute(Color color, int attrIdx) {
		if (attrIdx >= 0 && attrIdx < attr.size()) {
			if (sign != null && !color.equals(sign.getSegmentColor(attrIdx))) {
				sign.setSegmentColor(attrIdx, color);
				notifyVisChange();
			}
			super.setColorForAttribute(color, attrIdx);
		}
	}

	/**
	* Sets the colors for the specified group of attributes.
	*/
	@Override
	public void setColorsForAttributes(Vector colors, Vector attrIds) {
		if (attrIds == null || colors == null || attrIds.size() < 1 || colors.size() < 1)
			return;
		int nChanged = 0;
		for (int i = 0; i < attrIds.size() && i < colors.size(); i++) {
			String attrId = (String) attrIds.elementAt(i);
			int idx = getAttrIndex(attrId);
			if (idx >= 0 && idx < attr.size()) {
				++nChanged;
				Color color = (Color) colors.elementAt(i);
				if (sign != null && !color.equals(sign.getSegmentColor(idx))) {
					sign.setSegmentColor(idx, color);
				}
				super.setColorForAttribute(color, idx);
			}
		}
		if (nChanged > 0) {
			notifyVisChange();
		}
	}

	/**
	* Methods from the interface SignDrawer
	*/
	@Override
	public Sign getSignInstance() {
		checkCreateSign();
		Pie pie = new Pie();
		setupSign(pie);
		pie.setSizes(sign.getMaxWidth(), sign.getMaxHeight());
		pie.setMinSizes(sign.getMinWidth(), sign.getMinHeight());
		pie.setMaxSizes(sign.getMaxWidth(), sign.getMaxHeight());
		pie.setMustDrawFrame(sign.getMustDrawFrame());
		return pie;
	}

	/**
	* Through this method a SignDrawer is informed that some of the sign properties
	* have changed. The sign drawer must care about repainting the map.
	* The possible identifiers of sign properties are listed in the class
	* @see spade.vis.geometry.Sign
	*/
	@Override
	public void signPropertyChanged(int propertyId, Sign sgn) {
		if (sgn == null || sign == null)
			return;
		switch (propertyId) {
		case Sign.MAX_SIZE:
			sign.setMaxSizes(sgn.getMaxWidth(), sgn.getMaxHeight());
			break;
		case Sign.MIN_SIZE:
			sign.setMinSizes(sgn.getMinWidth(), sgn.getMinHeight());
			break;
		default:
			return;
		}
		notifyVisChange();
	}

	@Override
	public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
		if (colorHandler != null) {
			colorHandler.addPropertyChangeListener(l);
		}
	}

	@Override
	public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
		if (colorHandler != null) {
			colorHandler.removePropertyChangeListener(l);
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
	* visualization method: minimum and maximum sizes of the signs and
	* colors of sign elements.
	*/
	@Override
	public void startChangeParameters() {
		SignParamsController pc = new SignParamsController();
		pc.startChangeParameters(this);
	}

//ID
	@Override
	public Hashtable getVisProperties() {
		Hashtable param;
		param = super.getVisProperties();
		if (param == null) {
			param = new Hashtable();
		}

		param.put("maxD", String.valueOf(sign.getMaxHeight()));
		param.put("useMinSize", String.valueOf(useMinSize));
		param.put("cmpObj", (visCompObjName.length() > 0) ? visCompObjName : "none");

		StringBuffer sb = new StringBuffer();
		Vector colors = getAttributeColors();
		for (int i = 1; i < colors.size(); i++) {
			sb.append(Integer.toHexString((getColorForAttribute(i).getRGB())).substring(2));
			sb.append(" ");
		}
		param.put("colors", sb.toString());

		return param;
	}

	@Override
	public void setVisProperties(Hashtable param) {
		int temp;
		try {
			temp = new Integer((String) param.get("maxD")).intValue();
			sign.setMaxSizes(temp, temp);
		} catch (Exception ex) {
		}
		try {
			useMinSize = new Boolean((String) param.get("useMinSize")).booleanValue();
		} catch (Exception ex) {
		}
		try {
			String s = (String) param.get("cmpObj");
			visCompObjName = (s.equalsIgnoreCase("none")) ? "" : s;
		} catch (Exception ex) {
		}

		String sColors = (String) param.get("colors");
		if (sColors == null || sColors.length() < 1 || sColors.equals("null")) {
			sign.setDefaultColors();
		} else {
			StringTokenizer st = new StringTokenizer(sColors, " ");
			int i = 0;
			while (st.hasMoreTokens()) {
				setColorForAttribute(new Color(Integer.parseInt(st.nextToken(), 16)), i + 1);
				i++;
			}
		}

		super.setVisProperties(param);
	}
//~ID
}
