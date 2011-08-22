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

import spade.analysis.manipulation.AttrWeightUser;
import spade.lib.basicwin.Drawing;
import spade.lib.basicwin.StringInRectangle;
import spade.lib.lang.Language;
import spade.lib.util.FloatArray;
import spade.lib.util.StringUtil;
import spade.vis.database.ThematicDataItem;
import spade.vis.geometry.BarChart;
import spade.vis.geometry.Pie;
import spade.vis.geometry.Sign;
import spade.vis.geometry.StructSign;

/**
* A representation method used for decision support. Supports two variants of
* utility signs: utility bar charts and utility wheels. A utility sign consists
* of several graphical elements: bars in utility bar charts and circle segments
* in utility wheels. Each element corresponds to one of the attributes under
* consideration (decision criterion). One dimension of an element (height in a
* bar and radius in a circle segment) represents the value of the attribute for
* the object this sign stands for. When the attribute is a benefit criterion,
* the size is proportional to the value, for a cost criterion the inverse
* proportion is kept. Hence, better values are always represented by bigger
* sizes. The other dimension (width in a bar and angle in a circle segment)
* represents the importance of the criterion. When the user interactively
* changes the weights of the criteria, the signs on the map are immediately
* redrawn.
* By the construction of a sign, the total area of all its elements shows
* approximately the "goodness", or utility, of the object this sign stands for.
* Hence, a decision maker needs to look on the map for signs with largest areas.
*/

public class UtilitySignDrawer extends MultiNumberDrawer implements AttrWeightUser, AttrColorHandler, SignDrawer {
	static ResourceBundle res = Language.getTextResource("spade.vis.mapvis.Res");
	public static final int mm = Math.round(java.awt.Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f);

	/**
	* Possible types of utility signs: pies or bars
	*/
	public static int PieSign = 0, BarSign = 1;
	/**
	* An instance of either a Pie or a BarChart
	*/
	protected StructSign sign = null;

	/**
	* The type of the sign to be used - one of the constants BarSign (default)
	* or PieSign
	*/
	protected int signType = BarSign;

	public int getSignType() {
		return signType;
	}

	protected static final int defaultOrderMethod = StructSign.OrderDescending;

	/**
	* detects minimum size of signs to be drawn (part - 0..1 of maximum possible area)
	*/
	protected float drawingLimitMin = 0f, drawingLimitMax = 1f;

	public void setDrawingLimits(float drawingLimitMin, float drawingLimitMax) {
		this.drawingLimitMin = drawingLimitMin;
		this.drawingLimitMax = drawingLimitMax;
	}

//ID
	public float getDrawingLimitMin() {
		return drawingLimitMin;
	}

	public float getDrawingLimitMax() {
		return drawingLimitMax;
	}

//~ID
	/**
	* Weights of the attributes (criteria). Negative weights mean criteria
	* to be minimized
	*/
	protected FloatArray weights = null;

	/**
	* Checks if this visualization method is applicable to the given set of
	* attributes. May use the DataInformer to check types and values
	* of the attributes.
	*/
	@Override
	public boolean isApplicable(Vector attr) {
		minAttrNumber = 1;
		return super.isApplicable(attr);
	}

	/**
	* Must check semantic applicability of this visualization method to the
	* attributes previously set in the visualizer. Here there is no need in this
	* because the relationship between the attributes is not relevant.
	* Returns true.
	*/
	@Override
	public boolean checkSemantics() {
		return true;
	}

	/**
	* Constructs an instance of StructSign for further use depending on the
	* required diagram type
	*/
	protected void checkCreateSign() {
		if (sign == null || ((sign instanceof Pie) && signType == BarSign) || ((sign instanceof BarChart) && signType == PieSign)) {
			if (signType == PieSign) {
				Pie pie = new Pie();
				pie.showNumberByAngle = false; //the numbers will be shown by radius
				sign = pie;
			} else {
				sign = new BarChart();
				int w = Math.round(attr.size() * Sign.mm * 1.8f);
				sign.setMaxWidth(w);
			}
			setupSign(sign);
		}
	}

	/**
	* Sets the type of the signs to draw: pie or bar chart
	*/
	public void setSignType(int type) {
		if (type == PieSign || type == BarSign) {
			signType = type;
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

	protected void setupSign(StructSign sgn) {
		sgn.setMayChangeProperty(Sign.USE_FRAME, true);
		sgn.setMayChangeProperty(Sign.MAX_SIZE, true);
		sgn.setMayChangeProperty(Sign.SEGMENT_ORDER, true);
		sgn.setOrderingMethod(defaultOrderMethod);
		int attrN = (attr == null) ? 1 : attr.size();
		sgn.setNSegments(attrN);
		for (int i = 0; i < attrN; i++) {
			float f = (float) (0.2 + 0.8 * Math.random());
			if (i == 2) {
				f = (sgn.getSegmentPart(0) + sgn.getSegmentPart(1)) / 2;
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
		setCmpModeOff();
		super.setAttributes(attributes, attrNames);
		if (sign != null) {
			setupSign(sign);
		}
	}

	/**
	* The Visualizer sets its parameters. Calls parent's setup, and then setups
	* its sign.
	*/
	@Override
	public void setup() {
		super.setup();
		setCmpModeOff();
		if (sign != null) {
			setupSign(sign);
		}
	}

	/**
	* Adds one more attribute to be manipulated to the current set
	* of the manipulated attributes.
	*/
	@Override
	public void addAttribute(String attrId, String attrName) {
		int nOld = (attr == null) ? 0 : attr.size();
		super.addAttribute(attrId, attrName);
		if (attr.size() == nOld)
			return;
		if (sign != null) {
			sign.setNSegments(attr.size());
			for (int i = 0; i < attr.size(); i++) {
				sign.setSegmentColor(i, super.getColorForAttribute(i));
			}
		}
		if (cmpObjN >= 0 && cmpVals != null) {
			float cv[] = new float[attr.size()], cvAbs[] = new float[attr.size()];
			for (int i = 0; i < cmpVals.length; i++) {
				cv[i] = cmpVals[i];
				cvAbs[i] = cmpValsAbs[i];
			}
			int n = attr.size() - 1;
			cv[n] = cvAbs[n] = Float.NaN;
			cmpVals = cv;
			cmpValsAbs = cvAbs;
			if (table != null) {
				int cn = getColumnN(n);
				double val = (cn < 0) ? Double.NaN : (aTrans == null) ? table.getNumericAttrValue(cn, cmpObjN) : aTrans.getNumericAttrValue(cn, cmpObjN);
				if (!Double.isNaN(val)) {
					cmpValsAbs[n] = (float) val;
					cmpVals[n] = (float) ((cmpValsAbs[n] - getDataMin(n)) / (getDataMax(n) - getDataMin(n)));
				}
			}
		} else {
			cmpVals = null;
			cmpValsAbs = null;
		}
	}

	/**
	* Removes one of the attributes being currently manipulated.
	*/
	@Override
	public void removeAttribute(int attrIdx) {
		if (attr == null)
			return;
		int nOld = attr.size();
		super.removeAttribute(attrIdx);
		if (attr.size() < nOld && sign != null) {
			sign.setNSegments(attr.size());
			for (int i = 0; i < attr.size(); i++) {
				sign.setSegmentColor(i, super.getColorForAttribute(i));
			}
		}
		if (cmpObjN >= 0 && cmpVals != null) {
			for (int i = attrIdx; i < cmpVals.length - 1; i++) {
				cmpVals[i] = cmpVals[i + 1];
				cmpValsAbs[i] = cmpValsAbs[i + 1];
			}
		}
	}

	/**
	* Sets the weights of the attributes. A negative weight means a criterion
	* to be minimized
	*/
	@Override
	public void setWeights(FloatArray weights) {
		this.weights = weights;
		if (weights != null && sign != null) {
			for (int i = 0; i < weights.size(); i++) {
				sign.setSegmentWeight(i, weights.elementAt(i));
			}
			notifyVisChange();
		}
	}

//ID
	public FloatArray getWeights() {
		return weights;
	}

//~ID
	protected boolean cmpMode = false;

	public boolean isInCmpMode() {
		return cmpMode;
	}

	protected float cmpVals[] = null, cmpValsAbs[] = null;
	protected int cmpObjN = -1;
//ID
	public String cmpObjName = "";

//~ID

	public float[] getCmpVals() {
		return cmpValsAbs;
	}

	public int getCmpObjNum() {
		return cmpObjN;
	}

	protected void setCmpModeOn(int cmpObjN, float cmpVals[]) {
		cmpMode = true;
		if (sign != null && sign instanceof BarChart) {
			((BarChart) sign).setCmpMode(cmpMode);
		}
		this.cmpVals = cmpVals;
		this.cmpObjN = cmpObjN;
		this.cmpValsAbs = new float[cmpVals.length];
		for (int i = 0; i < cmpVals.length; i++) {
			cmpValsAbs[i] = cmpVals[i];
			if (weights == null || weights.elementAt(i) > 0) {
				cmpVals[i] = (float) ((cmpVals[i] - getDataMin(i)) / (getDataMax(i) - getDataMin(i)));
			} else {
				cmpVals[i] = (float) ((getDataMax(i) - cmpVals[i]) / (getDataMax(i) - getDataMin(i)));
			}
		}
		notifyVisChange();
	}

	public void setCmpModeOn(int cmpObjN, String cmpObjName, float cmpVals[]) {
		this.cmpObjName = cmpObjName;
		this.setCmpModeOn(cmpObjN, cmpVals);
	}

	public void setCmpModeOff() {
		cmpMode = false;
		cmpObjN = -1;
		cmpObjName = "";
		if (sign != null && sign instanceof BarChart) {
			((BarChart) sign).setCmpMode(cmpMode);
		}
		notifyVisChange();
	}

	/**
	* This method is called by a GeoObject. The GeoObject sends its DataItem
	* to the Visualizer, and the Visualizer returns an Object specifying the
	* presentation of data contained in this DataItem. This may be, in
	* particular, Color or Diagram. Typically the argument DataItem is
	* a ThematicDataItem, although in some cases (e.g. manual classification)
	* this is not required.
	*
	* method check if the sign should be drawn (taking into account drawingLimit)
	* if not, return null
	*/
	@Override
	public Object getPresentation(ThematicDataItem dit) {
		if (dit == null)
			return null;
		if (attr == null || attr.size() < 1)
			return null;
		if (sdController != null && !sdController.mustDrawObject(dit.getId()))
			return null;
		checkCreateSign();
		if (signType == BarSign) {
			((BarChart) sign).cmpValue = Float.NaN;
		}
		float part = 0f;
		for (int i = 0; i < attr.size(); i++)
			if (Double.isNaN(getDataMin(i))) {
				sign.setSegmentPart(i, 0.0f);
			} else {
				double v = getNumericAttrValue(dit, i);
				if (!Double.isNaN(v)) {
					if (weights == null || weights.elementAt(i) > 0) {
						v = (v - getDataMin(i)) / (getDataMax(i) - getDataMin(i));
					} else {
						v = (getDataMax(i) - v) / (getDataMax(i) - getDataMin(i));
					}
					if (weights != null) {
						part += v * Math.abs(weights.elementAt(i));
					}
					if (signType == PieSign) {
						v = Math.sqrt(v);
					}
				} else {
					v = 0.0f;
				}
				if (cmpMode) {
					v -= cmpVals[i];
					if (attr.size() == 1) {
						((BarChart) sign).cmpValue = cmpVals[0];
					}
				}
				sign.setSegmentPart(i, (float) v);
			}
		return (weights == null || (part >= drawingLimitMin && part <= drawingLimitMax)) ? sign : null;
	}

	/**
	* Calculates the "size" of a sign, i.e. the utility value
	*/
	public float getUtility(ThematicDataItem dit) {
		if (dit == null)
			return 0f;
		if (attr == null || attr.size() < 1)
			return 0f;
		float utility = 0f;
		for (int i = 0; i < attr.size(); i++)
			if (!Double.isNaN(getDataMin(i))) {
				double v = getNumericAttrValue(dit, i);
				if (!Double.isNaN(v)) {
					if (weights == null || weights.elementAt(i) > 0) {
						v = (v - getDataMin(i)) / (getDataMax(i) - getDataMin(i));
					} else {
						v = (getDataMax(i) - v) / (getDataMax(i) - getDataMin(i));
					}
					if (weights != null) {
						utility += v * Math.abs(weights.elementAt(i));
					} else {
						utility += v;
					}
				}
			}
		return utility;
	}

	/**
	* Draws a small picture symbolizing this presentation technique
	*/
	@Override
	public void drawIcon(Graphics g, int x, int y, int w, int h) {
		checkCreateSign();
		for (int i = 0; i < attr.size(); i++) {
			sign.setSegmentPart(i, (1.0f + i) / attr.size());
		}
		int width = sign.getMaxWidth(), height = sign.getMaxHeight();
		sign.setMaxSizes(w - 2, h - 2);
		sign.draw(g, x, y, w, h);
		sign.setMaxSizes(width, height);
	}

	/**
	* Draws the part of the legend explaining this specific presentation method.
	*/
	@Override
	public Rectangle drawMethodSpecificLegend(Graphics g, int startY, int leftMarg, int prefW) {
		Color gray = Color.getHSBColor(0.0f, 0.0f, 0.7f);
		Point p = null;
		checkCreateSign();
		int rSize = 4 * mm, gap = 2 * mm;
		// following string: "Criterion weight"
		String descrWeight = res.getString("Criterion_weight"),
		// following string: "Utility value"
		descrUtil = res.getString("Utility_value"),
		// following string: "ideal case"
		descrIdeal = res.getString("ideal_case");

		int y = startY, maxX = leftMarg, fh = g.getFontMetrics().getHeight();
		int posX = leftMarg;
		y += gap;
		Rectangle sr = new Rectangle(posX, y, sign.getMaxWidth(), sign.getMaxHeight());
		if (signType == PieSign) {
			Pie pie = (Pie) sign;
			g.setColor(gray);
			Drawing.fillSector(g, posX + sr.width / 2, y + 3 * sr.height / 4, sr.height, 135, -90);
			//g.fillArc(posX,y+sr.height/4,sr.width,sr.height,135,-90);
			g.setColor(Color.black);
			//g.drawArc(posX,y+sr.height/4,sr.width,sr.height,135,-90);
			Drawing.drawSector(g, posX + sr.width / 2, y + 3 * sr.height / 4, sr.height, 135, -90);

			Drawing.drawHorizontalArrow(g, posX + Math.round(0.15f * sr.height), y + 3 * sr.height / 4 + gap / 2, Math.round(sr.height / (float) Math.sqrt(2.0f)), 4, true, true);
			p = StringInRectangle.drawText(g, descrWeight, posX + Math.round(0.15f * sr.height), y + 3 * sr.height / 4 + gap, prefW - posX, false);
			posX += sr.width - Math.round(0.15f * sr.height) + gap;
			if (maxX < p.x) {
				maxX = p.x;
			}

			Drawing.drawVerticalArrow(g, posX, y + sr.height / 4, 4, sr.height / 2, true, true);
			posX += gap;
			if (g.getFontMetrics().stringWidth(descrUtil) < (prefW - posX)) {
				p = StringInRectangle.drawText(g, descrUtil, posX, y + sr.height / 2 - fh / 2, prefW - posX, false);
			} else {
				p = StringInRectangle.drawText(g, descrUtil, posX, y + sr.height / 4 - fh / 2, prefW - posX, false);
			}
			if (maxX < p.x) {
				maxX = p.x;
			}

			posX = maxX + gap;
			for (int i = 0; i < attr.size(); i++) {
				Drawing.drawSector(g, posX + sr.width / 2, y + sr.height / 2, sr.height, i * 360 / attr.size(), -360 / attr.size());
			}
			g.drawOval(posX, y, sr.width, sr.height);
			/* // filling the ideal pie chart with "pies"
			for (int i=0;i<attr.size();i++) {
			  g.setColor(Color.getHSBColor(0.0f,0.0f,0.6f+0.15f*(i%2)));
			  g.fillArc(posX,y,sr.width,sr.height,i*360/attr.size(),-360/attr.size());
			  g.setColor(Color.black);
			  g.drawArc(posX,y,sr.width,sr.height,i*360/attr.size(),-360/attr.size());
			}
			*/
			//posX+=sign.getMaxWidth()+gap;
			y += sign.getMaxHeight();
			p = StringInRectangle.drawText(g, descrIdeal, posX, y, prefW - posX, false);
			y = p.y + gap;
			posX = leftMarg;

		} else {
			BarChart bar = (BarChart) sign;
			g.setColor(gray);
			g.fillRect(posX, y, sr.width / attr.size(), sr.height);
			g.setColor(Color.black);
			g.drawRect(posX, y, sr.width / attr.size(), sr.height);

			Drawing.drawHorizontalArrow(g, posX, y + sr.height + gap, sr.width / attr.size(), 4, true, true);
			posX += sr.width / attr.size() + gap;
			p = StringInRectangle.drawText(g, descrWeight, posX, y + sr.height + gap - fh / 2, prefW - posX, false);
			if (maxX < p.x) {
				maxX = p.x;
			}
			Drawing.drawVerticalArrow(g, posX, y, 4, sr.height, true, true);
			posX += gap;
			if (g.getFontMetrics().stringWidth(descrUtil) < (prefW - posX)) {
				p = StringInRectangle.drawText(g, descrUtil, posX, y + sr.height / 2 - fh / 2, prefW - posX, false);
			} else {
				p = StringInRectangle.drawText(g, descrUtil, posX, y, prefW - posX, false);
			}
			if (maxX < p.x) {
				maxX = p.x;
			}
			posX = maxX + gap;
			//here is drawing of simple rectangle
			//g.setColor(gray);
			//g.fillRect(posX,y,sign.getMaxWidth(),sign.getMaxHeight());
			g.setColor(Color.black);
			g.drawRect(posX, y, sign.getMaxWidth(), sign.getMaxHeight());

			/* // Filling the ideal rectangle with bars
			for (int i=0;i<attr.size();i++) {
			  g.setColor(Color.getHSBColor(0.0f,0.0f,0.6f+0.15f*(i%2)));
			  g.fillRect(posX+i*(sr.width/attr.size()),y,sr.width/attr.size(),sr.height);
			  g.setColor(Color.black);
			  g.drawRect(posX+i*(sr.width/attr.size()),y,sr.width/attr.size(),sr.height);
			}
			*/
			y += sign.getMaxHeight();
			p = StringInRectangle.drawText(g, descrIdeal, posX, y, prefW - posX, false);
			y = p.y + gap;
			posX = leftMarg;
		}
		// explanation: Attribute - Color correspondence
		for (int i = 0; i < attr.size(); i++) {
			g.setColor(sign.getSegmentColor(i));
			g.fillRect(leftMarg, y, rSize, rSize);
			g.setColor(Color.black);
			p = StringInRectangle.drawText(g, getAttrName(i), leftMarg + rSize + gap, y, prefW - rSize - gap, false);
			y = (p.y - startY > rSize) ? p.y : startY + rSize;
			if (maxX < p.x) {
				maxX = p.x;
			}
		}
		y += gap;
		// if comparison mode is ON
		if (cmpMode) {
			String sValue = "";
			g.setColor(Color.black);
			// following string: "Comparison to "
			p = StringInRectangle.drawText(g, res.getString("Comparison_to") + cmpObjName + ":", leftMarg, y, prefW, false);
			y = p.y;
			for (int i = 0; i < attr.size(); i++) {

				g.setColor(sign.getSegmentColor(i));
				g.fillRect(leftMarg, y, rSize, rSize);
				g.setColor(Color.black);
				sValue = (cmpValsAbs != null ? StringUtil.doubleToStr(cmpValsAbs[i], getDataMin(), getDataMax()) : "n/a");
				//sValue+=" ("+getAttrName(i)+")";
				p = StringInRectangle.drawText(g, sValue, leftMarg + rSize + gap, y, prefW - rSize - gap, false);
				y = (p.y - startY > rSize) ? p.y : startY + rSize;
				if (maxX < p.x) {
					maxX = p.x;
				}
			}
			y += gap;
		}
		for (int i = 0; i < attr.size(); i++) {
			double fAttrValBest = Double.NaN, fAttrValWorst = Double.NaN, fWeight = Double.NaN, fmin = getDataMin(i), fmax = getDataMax(i);
			boolean costCriterion = false;
			// following string: sWeight="Weight: "
			String sAttrName = getAttrName(i) + "\n", sWeight = res.getString("Weight_"),
			// following string: "\nBest: ",sWorst="\nWorst: "
			sBest = res.getString("Best_"), sWorst = res.getString("Worst_");
			if (weights != null) {
				costCriterion = weights.elementAt(i) < 0;
				fAttrValBest = (costCriterion ? fmin : fmax);
				fAttrValWorst = (costCriterion ? fmax : fmin);
				fWeight = Math.abs(weights.elementAt(i));
			}
			sWeight += (Double.isNaN(fWeight) ? "n/a" : StringUtil.doubleToStr(fWeight, 0.00, 1.00));
			sBest += (Double.isNaN(fAttrValBest) ? "n/a" : StringUtil.doubleToStr(fAttrValBest, fmin, fmax));
			sWorst += (Double.isNaN(fAttrValWorst) ? "n/a" : StringUtil.doubleToStr(fAttrValWorst, fmin, fmax));

			g.setColor(Color.black);
			posX = leftMarg + rSize / 2 + gap / 2;

			if (costCriterion) {
				Drawing.fillArrow(g, posX, y, posX + rSize / 2, y + 3 * rSize / 2, 2 * mm, 3 * mm / 2, false, true);
			} else {
				Drawing.fillArrow(g, posX, y + 3 * rSize / 2, posX + rSize / 2, y, 2 * mm, 3 * mm / 2, false, true);
			}
			posX += rSize / 2 + gap / 2;
			p = StringInRectangle.drawText(g, sAttrName + sWeight + sBest + sWorst, posX, y, prefW - posX, false);
			g.setColor(sign.getSegmentColor(i));
			g.fillRect(leftMarg, y, rSize / 2, p.y - y);

			y = (p.y - startY > rSize) ? p.y + gap : startY + rSize;
			if (maxX < p.x) {
				maxX = p.x;
			}
		}
		g.setColor(Color.black);

		// drawingLimit...
		if (drawingLimitMin > 0f || drawingLimitMax < 1f) {
			// following string: "Shown are only signs with"
			String sLimitTextBegin = res.getString("Shown_are_only_signs_with"),
			// following string: " of the maximum area covered"
			sLimitTextEnd = res.getString("of_the_maximum_area_covered"), sLimitText = "";
			if (drawingLimitMin > 0f) {
				// following string: " no less than "
				sLimitTextBegin += res.getString("no_less_than") + StringUtil.floatToStr(100 * drawingLimitMin, 0f, 100f) + "%";
			}
			if (drawingLimitMax < 1f) {
				// following string: " no more than "
				sLimitTextEnd = res.getString("no_more_than") + StringUtil.floatToStr(100 * drawingLimitMax, 0f, 100f) + "%" + sLimitTextEnd;
			}
			if (drawingLimitMin > 0f && drawingLimitMax < 1f) {
				sLimitText = sLimitTextBegin + " and" + sLimitTextEnd;
			} else {
				sLimitText = sLimitTextBegin + sLimitTextEnd;
			}
			p = StringInRectangle.drawText(g, sLimitText, leftMarg, y, prefW, false);
		}
		y = (p.y - startY > rSize) ? p.y + gap : startY + rSize;
		if (maxX < p.x) {
			maxX = p.x;
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
	* Takes (possibly) changed attribute colors from its colorHandler.
	*/
	public void checkAttrColorsChange() {
		if (colorHandler == null || attr == null || sign == null)
			return;
		boolean changed = false;
		for (int i = 0; i < attr.size(); i++) {
			Color color = super.getColorForAttribute(i);
			if (color != null && !color.equals(sign.getSegmentColor(i))) {
				sign.setSegmentColor(i, color);
				changed = true;
			}
		}
		if (changed) {
			notifyVisChange();
		}
	}

	/**
	* Methods from the interface SignDrawer
	*/
	@Override
	public Sign getSignInstance() {
		checkCreateSign();
		StructSign sgn = null;
		if (signType == PieSign) {
			Pie pie = new Pie();
			pie.showNumberByAngle = false; //the numbers will be shown by radius
			sgn = pie;
		} else {
			BarChart bar = new BarChart();
			bar.setCmpMode(false);
			sgn = bar;
		}
		setupSign(sgn);
		sgn.setOrderingMethod(sign.getOrderingMethod());
		sgn.setMinSizes(sign.getMinWidth(), sign.getMinHeight());
		sgn.setMaxSizes(sign.getMaxWidth(), sign.getMaxHeight());
		sgn.setMustDrawFrame(sign.getMustDrawFrame());
		return sgn;
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
		case Sign.USE_FRAME:
			sign.setMustDrawFrame(sgn.getMustDrawFrame());
			break;
		case Sign.SEGMENT_ORDER:
			sign.setOrderingMethod(((StructSign) sgn).getOrderingMethod());
			break;
		default:
			return;
		}
		notifyVisChange();
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

//ID
	@Override
	public Hashtable getVisProperties() {
		Hashtable param;
/*    param = super.getVisProperties();
    if (param == null)*/param = new Hashtable();

		if (signType == BarSign) {
			param.put("maxWidth", String.valueOf(sign.getMaxWidth()));
			param.put("maxHeight", String.valueOf(sign.getMaxHeight()));
		} else {
			param.put("maxD", String.valueOf(sign.getMaxWidth()));
		}
		param.put("framed", String.valueOf(sign.getMustDrawFrame()));
		if (signType == BarSign && cmpMode) {
			if (cmpObjName != null && cmpObjName.length() > 0 && cmpObjName != "specified values") {
				param.put("cmpObj", cmpObjName);
			} else if (cmpValsAbs != null && cmpValsAbs.length > 0) {
				String vals = "";
				for (float cmpValsAb : cmpValsAbs) {
					vals += String.valueOf(cmpValsAb) + " ";
				}
				param.put("cmpVals", vals);
			}
		}

		switch (sign.getOrderingMethod()) {
		case StructSign.OrderPreserved:
			param.put("order", "preserved");
			break;
		case StructSign.OrderAscending:
			param.put("order", "ascending");
			break;
		case StructSign.OrderDescending:
			param.put("order", "descending");
			break;
		default:
			break;
		}

		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < this.getAttributeColors().size()/*symbol.getNSegments()*/; i++) {
			sb.append(Integer.toHexString((getColorForAttribute(i)/*symbol.getSegmentColor(i))*/.getRGB())).substring(2));
			sb.append(" ");
		}
		param.put("colors", sb.toString());

		param.put("drawingLimitMin", String.valueOf((int) (drawingLimitMin * 100)) + "%");
		param.put("drawingLimitMax", String.valueOf((int) (drawingLimitMax * 100)) + "%");

		sb = new StringBuffer();
		for (int i = 0; i < weights.size(); i++) {
			sb.append(Math.abs(weights.elementAt(i)));
			sb.append(" ");
		}
		param.put("weights", sb.toString());

		return param;
	}

	@Override
	public void setVisProperties(Hashtable param) {
		float temp;
		try {
			String s = (String) param.get("drawingLimitMin");
			temp = new Float(s.substring(0, s.length() - 1)).floatValue() / 100;
			if (!Float.isNaN(temp)) {
				drawingLimitMin = temp;
			}
		} catch (Exception ex) {
		}
		try {
			String s = (String) param.get("order");
			if (s.equalsIgnoreCase("preserved")) {
				sign.setOrderingMethod(StructSign.OrderPreserved);
			} else if (s.equalsIgnoreCase("ascending")) {
				sign.setOrderingMethod(StructSign.OrderAscending);
			} else if (s.equalsIgnoreCase("descending")) {
				sign.setOrderingMethod(StructSign.OrderDescending);
			}
		} catch (Exception ex) {
		}
		try {
			String s = (String) param.get("drawingLimitMax");
			temp = new Float(s.substring(0, s.length() - 1)).floatValue() / 100;
			if (!Float.isNaN(temp)) {
				drawingLimitMax = temp;
			}
		} catch (Exception ex) {
		}

		try {
			sign.setMaxHeight(new Integer((String) param.get("maxHeight")).intValue());
		} catch (Exception ex) {
		}
		try {
			sign.setMaxWidth(new Integer((String) param.get("maxWidth")).intValue());
		} catch (Exception ex) {
		}
		try {
			int maxD = new Integer((String) param.get("maxD")).intValue();
			sign.setMaxSizes(maxD, maxD);
		} catch (Exception ex) {
		}
		try {
			sign.setMustDrawFrame(new Boolean((String) param.get("framed")).booleanValue());
		} catch (Exception ex) {
		}
		try {
			String s = (String) param.get("cmpObj");
			cmpObjName = (s.equalsIgnoreCase("none")) ? "" : s;
		} catch (Exception ex) {
		}
		try {
			String s = (String) param.get("cmpVals");
			if (s == null || s.length() < 1) {
				cmpValsAbs = null;
			} else {
				StringTokenizer st = new StringTokenizer(s, " ");
				cmpValsAbs = new float[st.countTokens()];
				int i = 0;
				while (st.hasMoreTokens()) {
					cmpValsAbs[i] = new Float(st.nextToken()).floatValue();
					i++;
				}
			}
		} catch (Exception ex) {
		}

		String sColors = (String) param.get("colors");
		if (sColors == null || sColors.length() < 1 || sColors.equals("null")) {
			sign.setDefaultColors();
		} else {
			StringTokenizer st = new StringTokenizer(sColors, " ");
			int i = 0;
			while (st.hasMoreTokens()) {
				setColorForAttribute(new Color(Integer.parseInt(st.nextToken(), 16)), i);
				i++;
			}
		}

		String sWeights = (String) param.get("weights");
		if (sWeights != null || sWeights.length() > 0) {
			StringTokenizer st = new StringTokenizer(sWeights, " ");
			FloatArray newWeights = new FloatArray();
			while (st.hasMoreTokens()) {
				newWeights.addElement(new Float(st.nextToken()).floatValue());
			}
			float sum = 0;
			for (int j = 0; j < newWeights.size(); j++) {
				sum += newWeights.elementAt(j);
			}
			weights = new FloatArray();
			for (int j = 0; j < newWeights.size(); j++) {
				weights.addElement(newWeights.elementAt(j) / sum);
			}
		}

		super.setVisProperties(param);
	}
//~ID
}
