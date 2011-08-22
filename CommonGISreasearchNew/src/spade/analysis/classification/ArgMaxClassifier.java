package spade.analysis.classification;

import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.Slider;
import spade.lib.color.CS;
import spade.lib.lang.Language;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.ThematicDataItem;
import spade.vis.mapvis.AttrColorHandler;

public class ArgMaxClassifier extends TableClassifier implements ActionListener, ItemListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.classification.Res");
	/**
	* if attributes are structured (like a01,a05,a23, bb05,bb14,bb29, ...
	* they are structured to {a,bb,...} and {1,5,14,23,29,...}
	* The first list is stored in <names>, the 2nd - in <numbers>
	* In non-structured case all attributes are in <names>, <numbers> has one value 0
	*/
	//protected Vector names=null;

	protected Vector parVals = null; // values of the distinguishing parameters

	//protected int numbers[]=null;
	/**
	* for each attribute: indexes in <names> and <numbers>
	*/
	//protected int idxNames[]=null,
	//              idxNumbers[]=null;

	protected Slider slValue = null, slProportion = null;
	protected Checkbox cbMissingValues = null;

	protected AttrColorHandler colorHandler = null;

	public void setAttrColorHandler(AttrColorHandler handler) {
		colorHandler = handler;
	}

	public void setControls(Slider slValue, Slider slProportion, Checkbox cbMissingValues) {
		this.slValue = slValue;
		this.slProportion = slProportion;
		this.cbMissingValues = cbMissingValues;
	}

	protected void setControlState() {
	}

	/**
	* Checks if this visualization method is applicable to the specified number
	* of attributes having the specified types. The possible types see in
	* @see spade.vis.database.AttributeTypes
	*/
	@Override
	public boolean isApplicable(int attrNumber, char attrTypes[]) {
		if (attrNumber < 2)
			return false;
		for (int i = 0; i < attrNumber; i++)
			if (AttributeTypes.isNumericType(attrTypes[i]) || AttributeTypes.isTemporal(attrTypes[i])) {
				continue;
			} else
				return false;
		return true;
	}

	/**
	* Prepares its internal variables to the classification.
	*/
	@Override
	public void setup() {
		System.out.println("*** setup start");
		Vector vid = new Vector(attr.size(), 10);
		for (int i = 0; i < attr.size(); i++)
			if (subAttr == null || subAttr.size() <= i || subAttr.elementAt(i) == null) {
				vid.addElement(attr.elementAt(i));
			} else {
				Vector sub = (Vector) subAttr.elementAt(i);
				int idx = -1;
				for (int j = 0; j < sub.size() && idx < 0; j++)
					if (sub.elementAt(j) != null) {
						idx = j;
					}
				if (idx >= 0) {
					vid.addElement(sub.elementAt(idx));
				} else {
					vid.addElement(attr.elementAt(i));
				}
			}
		parVals = data.getDistinguishingParameters(vid);
		/*
		if (parVals!=null)
		  for (int i=0; i<parVals.size(); i++) {
		    Vector v=(Vector)parVals.elementAt(i);
		    System.out.println(v.elementAt(0)+":");
		    for (int j=1; j<v.size(); j++)
		      System.out.println(" j="+(j-1)+", params[j]="+v.elementAt(j)+", attr[j]="+data.getAttributeName(getAttrColumnN(j-1)));
		    //System.out.println();
		  }
		*/
		notifyClassesChange();
		System.out.println("*** setup finish");
	}

	/**
	* Checks if new values appeared in data and if this affects the classes
	*/
	@Override
	protected void checkValues() { // is it needed here?
		//System.out.println("* classifier checkValues");
	}

	@Override
	public int getRecordClass(ThematicDataItem dit) {
		if (dit == null)
			return -1;
		float maxVal = Float.NaN, sum = 0f;
		int maxN = -1;
		for (int i = 0; i < getAttributes().size(); i++) {
			double val = getNumericAttrValue(dit, i);
			if (Double.isNaN(val))
				if (cbMissingValues != null && cbMissingValues.getState()) {
					val = 0f;
				} else {
					continue;
				}
			if (Float.isNaN(maxVal) || val > maxVal) {
				maxN = i;
				maxVal = (float) val;
			}
			sum += val;
		}
		if (Float.isNaN(maxVal))
			return -1;
		if (slValue == null)
			return (maxVal == 0f) ? 0 : 2 + maxN;
		if (maxVal <= slValue.getValue())
			return 1;
		else if (100f * maxVal / sum < slProportion.getValue())
			return 0;
		else
			return 2 + maxN;
	}

	@Override
	public Color getClassColor(int classN) {
		if (classN == 0)
			return Color.white;
		else if (classN == 1)
			return Color.black;
		else if (colorHandler == null)
			return CS.getNthPureColor(classN - 2, getNClasses() - 2);
		else
			return colorHandler.getColorForAttribute(getInvariantAttrId(classN - 2));
	}

	@Override
	public String getClassName(int classN) {
		if (classN == 0)
			return res.getString("Mix");
		if (classN == 1)
			return res.getString("Empty");
		if (parVals == null || parVals.size() < 1)
			return data.getAttributeName(getAttrColumnN(classN - 2));
		Attribute a = data.getAttribute(getAttrColumnN(classN - 2));
		String name = null;
		for (int i = 0; i < parVals.size(); i++)
			if (parVals.elementAt(i) != null) {
				Vector v = (Vector) parVals.elementAt(i);
				if (v.size() > 0) {
					String parId = (String) v.elementAt(0);
					Object parValue = a.getParamValue(parId);
					if (parValue != null) {
						if (name != null) {
							name += ", " + parId + "=" + parValue.toString();
						} else {
							name = parId + "=" + parValue.toString();
						}
					}
				}
			}
		if (name == null)
			return data.getAttributeName(getAttrColumnN(classN - 2));
		return name;
	}

	@Override
	public int getNClasses() {
		return 2 + getAttributes().size();
	}

	/**
	* A classifier must give the user an opportunity to change interactively
	* colors assigned to classes. This method starts the procedure of class
	* color changing.
	*/
	@Override
	public void startChangeColors() {
	}

	@Override
	public Rectangle drawLegend(Component c, Graphics g, int startY, int leftmarg, int prefW) {
		int y = startY;
		if (parVals != null && parVals.size() == 2) {
			FontMetrics fm = g.getFontMetrics();
			int maxX = 0;
			int asc = fm.getAscent(), step = asc + 5, ssx = leftmarg + Metrics.mm(), maxW = 0;
			Vector vrows = (Vector) parVals.elementAt(0), vcols = (Vector) parVals.elementAt(1);
			for (int i = 1; i < vrows.size(); i++) {
				g.drawString((String) vrows.elementAt(i), ssx, y + step * i);
				int w = fm.stringWidth((String) vrows.elementAt(i));
				if (w > maxW) {
					maxW = w;
				}
			}
			int w = step, N = 0;
			for (int i = 1; i < vrows.size(); i++) {
				for (int j = 1; j < vcols.size(); j++) {
					g.setColor((colorHandler == null) ? CS.getNthPureColor(N, getNClasses() - 2) : colorHandler.getColorForAttribute(getInvariantAttrId(N)));
					g.fillRect(ssx + maxW + w * j, y - asc + step * i, w, step);
					N++;
				}
			}
			IntArray counts = getClassSizes();
			g.setColor(Color.white);
			for (int k = 2; k < counts.size(); k++)
				if (counts.elementAt(k) > 0) {
					Attribute a = data.getAttribute(getAttrColumnN(k - 2));
					int orderRows = vrows.indexOf(a.getParamValue((String) vrows.elementAt(0)), 1), orderCols = vcols.indexOf(a.getParamValue((String) vcols.elementAt(0)), 1);
					int xs = ssx + maxW + w * orderCols, ys = y - asc + step * orderRows + 1;
					g.drawRect(xs + 1, ys + 1, w - 4, step - 4);
					//System.out.println("* r="+orderRows+", c="+orderCols+", "+a.getName());
				} else if (k >= 2) { // check if there are any values corresponing to the threshold
					boolean found = false;
					int col = getAttrColumnN(k - 2);
					for (int i = 0; i < data.getDataItemCount() && !found; i++) {
						double v = getNumericAttrValue(col, i);
						if (Double.isNaN(v)) {
							continue;
						}
						found = v >= ((slValue == null) ? 0f : slValue.getValue());
					}
					if (found) {
						Attribute a = data.getAttribute(getAttrColumnN(k - 2));
						int orderRows = vrows.indexOf(a.getParamValue((String) vrows.elementAt(0)), 1), orderCols = vcols.indexOf(a.getParamValue((String) vcols.elementAt(0)), 1);
						int xs = ssx + maxW + w * orderCols, ys = y - asc + step * orderRows + w - 4;
						g.drawLine(xs + 1, ys + 1, xs + 1 + w - 4, ys + 1);
					}
				}
			y += step * vrows.size();
			if (vcols.size() > 3) {
				String str = "" + vcols.elementAt(1) + "," + vcols.elementAt(2) + ",...," + vcols.elementAt(vcols.size() - 1);
				g.setColor(Color.black);
				g.drawString(str, ssx + maxW + w, y);
				y += step;
			}
		}
		return drawClassStatistics(g, y, leftmarg, prefW);
	}

	@Override
	public Rectangle drawClassStatistics(Graphics g, int startY, int leftmarg, int prefW) {
		//System.out.println("*** draw class statistics start");
		int y = startY, maxX = 0;
		FontMetrics fm = g.getFontMetrics();
		int asc = fm.getAscent(), step = asc + 5, rectW = Metrics.mm() * 4, ssx = leftmarg + rectW + Metrics.mm();
		y += asc;
		int y0 = y, mx = ssx;
		Vector vrows = null, vcols = null;
		if (parVals != null && parVals.size() >= 2) {
			vrows = (Vector) parVals.elementAt(0);
			vcols = (Vector) parVals.elementAt(1);
			Attribute a = data.getAttribute(getAttrColumnN(0));
			if (a.getParent() != null) {
				String str = a.getParent().getName();
				g.setColor(Color.black);
				g.drawString(str, mx, y);
				y += step + step / 4;
			}
		}
		int nTotal = getSetSize();
		int lastGroup = -1;
		IntArray counts = getClassSizes();
		for (int i = 0; i < counts.size(); i++)
			if (counts.elementAt(i) > 0) {
				String name = this.getClassName(i);
				if (i >= 2 && vrows != null) {
					Attribute a = data.getAttribute(getAttrColumnN(i - 2));
					int orderRows = vrows.indexOf(a.getParamValue((String) vrows.elementAt(0)), 1), orderCols = vcols.indexOf(a.getParamValue((String) vcols.elementAt(0)), 1);
					if (orderRows != lastGroup) {
						y += step / 4;
						lastGroup = orderRows;
					}
					name = "";
					for (int j = 0; j < parVals.size(); j++) {
						name += ((j == 0) ? "" : ", ") + (String) a.getParamValue((String) ((Vector) parVals.elementAt(j)).elementAt(0));
					}
				}
				g.setColor(getClassColor(i));
				g.fillRect(leftmarg, y - step + asc / 2, rectW, step - 1);
				g.setColor(Color.black);
				g.drawRect(leftmarg, y - step + asc / 2, rectW, step - 2);
				float perc = 100f * counts.elementAt(i) / nTotal;
				String str = String.valueOf(counts.elementAt(i)) + " (" + StringUtil.floatToStr(perc, 0f, 100f) + "%) " + name;
				g.drawString(str, mx, y);
				int x = mx + fm.stringWidth(str);
				if (x > maxX) {
					maxX = x;
				}
				y += step;
			}
		//System.out.println("*** draw class statistics finish");
		return new Rectangle(leftmarg, startY, maxX - leftmarg + Metrics.mm(), y - startY);
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		setControlState();
		notifyChange("classes");
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		setControlState();
		notifyChange("classes");
	}

	/**
	* Replies whether attributes with null values should be shown in data popups.
	* In this case, returns false.
	*/
	@Override
	public boolean getShowAttrsWithNullValues() {
		return false;
	}

//ID
	@Override
	public Hashtable getVisProperties() {
		Hashtable param = null;
		try {
			param = super.getVisProperties();
		} catch (Exception ex) {
		}
		if (param == null) {
			param = new Hashtable();
		}

		try {
			param.put("missingIsZero", String.valueOf(cbMissingValues.getState()));
			param.put("presenceThres", String.valueOf(slValue.getValue()));
			param.put("mixThres", String.valueOf(slProportion.getValue()));
		} catch (Exception ex) {
		}

		return param;
	}

	private float presenceThres = Float.NaN;
	private float mixThres = Float.NaN;
	private boolean missingIsZero = true;

	public float getPresenceThreshold() {
		if (slValue != null)
			return (float) slValue.getValue();
		else if (!Float.isNaN(presenceThres))
			return presenceThres;
		else
			return 0;
	}

	public float getMixThreshold() {
		if (slProportion != null)
			return (float) slProportion.getValue();
		else if (!Float.isNaN(mixThres))
			return mixThres;
		else
			return 0;
	}

	public boolean getMissingIsZero() {
		if (cbMissingValues != null)
			return cbMissingValues.getState();
		else
			return missingIsZero;
	}

	@Override
	public void setVisProperties(Hashtable param) {
		float temp = Float.NaN;
		try {
			temp = new Float((String) param.get("presenceThres")).floatValue();
		} catch (Exception ex) {
		}
		if (!Float.isNaN(temp)) {
			presenceThres = temp;
		}
		try {
			temp = new Float((String) param.get("mixThres")).floatValue();
		} catch (Exception ex) {
		}
		if (!Float.isNaN(temp)) {
			mixThres = temp;
		}
		try {
			missingIsZero = new Boolean((String) param.get("missingIsZero")).booleanValue();
		} catch (Exception ex) {
		}

		super.setVisProperties(param);
	}
//~ID
}
