package spade.analysis.classification;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
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

import spade.analysis.manipulation.Slider;
import spade.analysis.manipulation.SliderListener;
import spade.analysis.plot.ScatterPlotWithSliders;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.StringInRectangle;
import spade.lib.color.Color2d;
import spade.lib.lang.Language;
import spade.lib.util.DoubleArray;
import spade.lib.util.FloatArray;
import spade.lib.util.IntArray;
import spade.lib.util.NumRange;
import spade.lib.util.NumValManager;
import spade.lib.util.QSortAlgorithm;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.ThematicDataItem;

/**
* Implements cross- classification of objects on the basis of two numeric
* attributes by breaking their values range into intervals.
*/

public class NumAttr2Classifier extends TableClassifier implements ActionListener, SliderListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.classification.Res");

	protected double minVh = Double.NaN, maxVh = Double.NaN, minVv = Double.NaN, maxVv = Double.NaN;
	protected DoubleArray breaksh = null, breaksv = null;
	protected Slider sliderh = null, sliderv = null;
	protected ScatterPlotWithSliders sp = null;

	protected NumAttr2ClassManipulator na2cm = null;

	public void setNumAtrr2ClassManipulator(NumAttr2ClassManipulator na2cm) {
		this.na2cm = na2cm;
//ID
		if (breaksh != null && breaksh.size() > 0 && breaksv != null && breaksv.size() > 0) {
			if (sliderh != null && sliderh.getBreaks().size() == 0) {
				for (int i = 0; i < breaksh.size(); i++) {
					sliderh.addBreak(breaksh.elementAt(i));
				}
				breaksChanged(sliderh, breaksh.getArray(), breaksh.size());
			}
			if (sliderv != null && sliderv.getBreaks().size() == 0) {
				for (int i = 0; i < breaksv.size(); i++) {
					sliderv.addBreak(breaksv.elementAt(i));
				}
				breaksChanged(sliderv, breaksv.getArray(), breaksv.size());
			}
		} else {
			//~ID
			for (int b = 0; b <= 1; b++) {
				Slider slider = (b == 0) ? sliderh : sliderv;
				double br[] = autoClassEqSize(b == 0, 2);
				if (br == null || br.length == 0) {
					br = new double[1];
					if (b == 0) {
						br[0] = (float) (minVh + maxVh) / 2;
					} else {
						br[0] = (float) (minVv + maxVv) / 2;
					}
				}
				slider.addBreak(br[0]);
				DoubleArray fa = new DoubleArray(4, 4);
				fa.addElement(br[0]);
				if (b == 0) {
					breaksh = fa;
				} else {
					breaksv = fa;
				}
				breaksChanged(slider, br, br.length);
			}
		}
	}

	public NumAttr2Classifier() {
		setSlidersColors();
	}

	public NumAttr2Classifier(AttributeDataPortion data, String attrIdh, String attrIdv) {
		this();
		if (data != null) {
			setTable(data);
		}
		setAttributes(attrIdh, attrIdv);
	}

	/**
	* Checks if this visualization method is applicable to the specified number
	* of attributes having the specified types. The possible types see in
	* @see spade.vis.database.AttributeTypes
	*/
	@Override
	public boolean isApplicable(int attrNumber, char attrTypes[]) {
		return attrNumber == 2 && attrTypes.length >= 2 && (AttributeTypes.isNumericType(attrTypes[0]) || AttributeTypes.isTemporal(attrTypes[0])) && (AttributeTypes.isNumericType(attrTypes[1]) || AttributeTypes.isTemporal(attrTypes[1]));
	}

	/**
	* Sets the attribute to be used for classification.
	*/
	public void setAttributes(String attrIdh, String attrIdv) {
		Vector v = new Vector(1, 1);
		v.addElement(attrIdh);
		v.addElement(attrIdv);
		setAttributes(v);
	}

	/**
	* Returns the identifier the numeric attribute (horizontal) used for
	* classification
	*/
	public String getAttrIdHor() {
		return getAttrId(0);
	}

	/**
	* Returns the identifier the numeric attribute (vertical) used for
	* classification
	*/
	public String getAttrIdVert() {
		return getAttrId(1);
	}

	/**
	* Returns the column number corresponding to the numeric attribute
	* (horizontal) used for classification
	*/
	public int getColNHor() {
		return getAttrColumnN(0);
	}

	/**
	* Returns the column number corresponding to the numeric attribute
	* (vertical) used for classification
	*/
	public int getColNVert() {
		return getAttrColumnN(1);
	}

	public void setSliders(Slider sliderh, Slider sliderv, ScatterPlotWithSliders sp) {
		this.sliderh = sliderh;
		if (sliderh != null) {
			sliderh.addSliderListener(this);
		}
		this.sliderv = sliderv;
		if (sliderv != null) {
			sliderv.addSliderListener(this);
		}
		this.sp = sp;
		setColorScale();
	}

	public Slider getSliderH() {
		return sliderh;
	}

	public Slider getSliderV() {
		return sliderv;
	}

	protected boolean dynUpdate = true;

	public void setDynamicUpdate(boolean dynUpdate) {
		this.dynUpdate = dynUpdate;
	}

	public void setBreaksH(DoubleArray breaksh, boolean notify) {
		this.breaksh = breaksh;
		setColorScale();
		if (na2cm != null && !Double.isNaN(minVv)) {
			na2cm.setTextFieldAndLabels((float) minVv, (float) maxVv, (float) minVh, (float) maxVh);
		}
		if (notify) {
			notifyChange("classes");
		}
	}

	public void setBreaksV(DoubleArray breaksv, boolean notify) {
		this.breaksv = breaksv;
		setColorScale();
		if (na2cm != null && !Double.isNaN(minVv)) {
			na2cm.setTextFieldAndLabels((float) minVv, (float) maxVv, (float) minVh, (float) maxVh);
		}
		if (notify) {
			notifyChange("classes");
		}
	}

	public DoubleArray getBreaksH() {
		return breaksh;
	}

	public DoubleArray getBreaksV() {
		return breaksv;
	}

	@Override
	public int getNClasses() {
		if (breaksh == null || breaksv == null)
			return 1;
		return (breaksh.size() + 1) * (breaksv.size() + 1);
	}

	protected void findMinMax() {
		minVh = Float.NaN;
		maxVh = Float.NaN;
		minVv = Float.NaN;
		maxVv = Float.NaN;
		NumRange rh = getAttrValueRange(0), rv = getAttrValueRange(1);
		if (rh != null) {
			minVh = rh.minValue;
			maxVh = rh.maxValue;
		}
		if (rv != null) {
			minVv = rv.minValue;
			maxVv = rv.maxValue;
		}
	}

	/**
	* Returns the value range of the "horizontal" attribute
	*/
	public NumRange getHorValueRange() {
		if (Double.isNaN(minVh) || Double.isNaN(maxVh)) {
			findMinMax();
		}
		if (Double.isNaN(minVh) || Double.isNaN(maxVh))
			return null;
		NumRange nr = new NumRange();
		nr.minValue = minVh;
		nr.maxValue = maxVh;
		return nr;
	}

	/**
	* Returns the value range of the "vertical" attribute
	*/
	public NumRange getVertValueRange() {
		if (Double.isNaN(minVv) || Double.isNaN(maxVv)) {
			findMinMax();
		}
		if (Double.isNaN(minVv) || Double.isNaN(maxVv))
			return null;
		NumRange nr = new NumRange();
		nr.minValue = minVv;
		nr.maxValue = maxVv;
		return nr;
	}

	/**
	* Prepares its internal variables to the classification.
	*/
	@Override
	public void setup() {
		findMinMax();
		if (na2cm != null && !Double.isNaN(minVv)) {
			na2cm.setTextFieldAndLabels((float) minVv, (float) maxVv, (float) minVh, (float) maxVh);
		}
		notifyClassesChange();
	}

	@Override
	public String getClassName(int classN) {
		if (classN < 0 || classN > getNClasses())
			return null;
		return "class " + classN; // to be done...
		/*
		if (Float.isNaN(minVal) || Float.isNaN(maxVal)) findMinMax();
		if (Float.isNaN(minVal) || Float.isNaN(maxVal)) return null;
		if (getNClasses()==1)
		  return StringUtil.floatToStr(minVal,minVal,maxVal)+".."+
		         StringUtil.floatToStr(maxVal,minVal,maxVal);
		if (classN==0)
		  return "< "+StringUtil.floatToStr(breaks.elementAt(0),minVal,maxVal);
		if (classN==breaks.size())
		  return ">= "+StringUtil.floatToStr(breaks.elementAt(classN-1),minVal,maxVal);
		return
		  "["+
		  StringUtil.floatToStr(breaks.elementAt(classN-1),minVal,maxVal)+
		  ".."+
		  StringUtil.floatToStr(breaks.elementAt(classN),minVal,maxVal)+
		  ")";
		*/
	}

	protected Color makeColor(int k, int i, int n1, int j, int n2, boolean isInverted, boolean notGray) {
		if (notGray) {
			float hue, sat, bri;
			if (k == -1) {
				hue = 0.7f * (n1 - 1 - i) / (n1 - 1);
				sat = (isInverted) ? 0.2f + 0.7f * j / (n2 - 1) : 0.2f + 0.7f * (n2 - 1 - j) / (n2 - 1);
				bri = 1 - 0.8f * sat;
			} else {
				hue = Color2d.Hue(k, i, n1, j, n2, isInverted);
				sat = Color2d.Saturation(i, n1, j, n2);
				bri = Color2d.Brightness(i, n1, j, n2);
			}
			//System.out.println("* i="+i+" ,n1="+n1+", j="+j+", n2="+n2);
			//System.out.println("* hue="+hue+", sat="+sat+", bri="+bri);
			return Color.getHSBColor(hue, sat, bri);
			//c.assignPastel(Color.getHSBColor(hue,(float)(0.75*sat),bri));
		} else
			return Color.lightGray;
		//c.assignPastel(Color.lightGray);
	}

	protected int colorScaleNumber = 2;
	protected boolean colorScaleIsInverted = false;
	protected boolean hiddenClasses[] = null;

	protected void setSlidersColors() {
		if (hiddenClasses != null && hiddenClasses.length != getNClasses()) {
			hiddenClasses = null;
		}
		if (breaksh == null || breaksv == null || sliderh == null || sliderv == null)
			return;
		int nh = breaksh.size() + 1, nv = breaksv.size() + 1;
		Color[] colors = new Color[nv];
		for (int v = 0; v < nv; v++) {
			colors[v] = makeColor(colorScaleNumber, 0, nh, v, nv, colorScaleIsInverted, true);
		}
		sliderv.setColors(colors);
		colors = new Color[nh];
		for (int h = 0; h < nh; h++) {
			colors[h] = makeColor(colorScaleNumber, h, nh, 0, nv, colorScaleIsInverted, true);
		}
		sliderh.setColors(colors);
		Color spColors[] = new Color[nv * nh];
		int n = -1;
		for (int h = 0; h < nh; h++) {
			for (int v = 0; v < nv; v++) {
				n++;
				spColors[n] = getClassColor(n);
				/*
				  (hiddenClasses!=null && hiddenClasses[n]) ?
				     Color.lightGray:
				     makeColor(colorScaleNumber,h,nh,v,nv,colorScaleIsInverted,true);
				*/
			}
		}
		sp.setSpColors(spColors);
	}

	public void setColorScale() {
		setSlidersColors();
//ID
		if (sp != null && sp.getCanvas() != null) {
			sp.getCanvas().repaint();
		}
//~ID
		colorsChanged(this);
	}

	public void setColorScale(int colorScaleNumber, boolean colorScaleIsInverted) {
		this.colorScaleNumber = colorScaleNumber;
		this.colorScaleIsInverted = colorScaleIsInverted;
		setColorScale();
	}

	public int getColorScaleNumber() {
		return colorScaleNumber;
	}

	public boolean getColorScaleInverted() {
		return colorScaleIsInverted;
	}

	@Override
	public Color getClassColor(int classN) {
		if (classN < 0 || classN > getNClasses() || breaksh == null || breaksv == null)
			return null;
		int nh = breaksh.size() + 1, nv = breaksv.size() + 1;
		int v = classN / nh, h = classN - v * nh;
		return (hiddenClasses != null && hiddenClasses[classN]) ? Color.lightGray : makeColor(colorScaleNumber, h, nh, v, nv, colorScaleIsInverted, true);
	}

	protected int getXYclass(double valh, double valv) {
		if (Double.isNaN(valh) || Double.isNaN(valv) || breaksh == null || breaksv == null)
			return -1;
		int hclass = -1;
		for (int i = 0; i < breaksh.size(); i++)
			if (valh < breaksh.elementAt(i)) {
				hclass = i;
				break;
			}
		if (hclass == -1) {
			hclass = breaksh.size();
		}
		int vclass = -1;
		for (int i = 0; i < breaksv.size(); i++)
			if (valv < breaksv.elementAt(i)) {
				vclass = i;
				break;
			}
		if (vclass == -1) {
			vclass = breaksv.size();
		}
		int classN = hclass + vclass * (1 + breaksh.size());
		//System.out.println("cl="+classN+", h="+valh+" ("+hclass+"), v="+valv+" ("+vclass+")");
		return classN;
	}

	@Override
	public int getRecordClass(ThematicDataItem dit) {
		if (dit == null)
			return -1;
		double valh = getNumericAttrValue(dit, 0), valv = getNumericAttrValue(dit, 1);
		return getXYclass(valh, valv);
	}

	protected double[] autoClassHEqInt(int n) {
		double br[] = new double[n - 1];
		for (int i = 1; i < n; i++) {
			br[i - 1] = minVh + (maxVh - minVh) * i / n;
		}
		return br;
	}

	protected double[] autoClassVEqInt(int n) {
		double br[] = new double[n - 1];
		for (int i = 1; i < n; i++) {
			br[i - 1] = minVv + (maxVv - minVv) * i / n;
		}
		return br;
	}

	protected double[] autoClassEqSize(boolean isH, int n) {
		double vals[] = new double[data.getDataItemCount()];
		int k = 0;
		for (int i = 0; i < vals.length; i++) {
			double value = getNumericAttrValue((isH) ? getColNHor() : getColNVert(), i);
			if (!Double.isNaN(value)) {
				vals[k++] = value;
			}
		}
		if (k < vals.length) {
			double v1[] = new double[k];
			for (int i = 0; i < k; i++) {
				v1[i] = vals[i];
			}
			vals = v1;
		}
		double allbr[] = NumValManager.breakToIntervals(vals, n, true);
		DoubleArray fa = new DoubleArray(allbr.length - 2, 1);
		for (int i = 1; i < allbr.length - 1; i++)
			// eliminate min, max, and repeating values
			if (allbr[i] != allbr[i - 1]) {
				fa.addElement(allbr[i]);
			}
		return fa.getTrimmedArray();
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource() == sp) { // mouse click, to be interpreted as class hiding
			if (getNClasses() <= 2)
				return;
			String str = ae.getActionCommand();
			if (str == null || str.trim().length() == 0)
				return;
			str = str.trim();
			float x = Float.NaN, y = Float.NaN;
			StringTokenizer st = new StringTokenizer(str, " ");
			if (st.hasMoreTokens()) {
				try {
					x = Float.valueOf(st.nextToken()).floatValue();
				} catch (NumberFormatException nfe) {
					return;
				}
			}
			if (st.hasMoreTokens()) {
				try {
					y = Float.valueOf(st.nextToken()).floatValue();
				} catch (NumberFormatException nfe) {
					return;
				}
			}
			if (Float.isNaN(x) || x < minVh || x > maxVh || Float.isNaN(y) || y < minVv || y > maxVv)
				return;
			int n = getXYclass(x, y);
			if (n < 0)
				return;
			if (hiddenClasses == null || hiddenClasses.length != getNClasses()) {
				hiddenClasses = new boolean[getNClasses()];
				for (int i = 0; i < hiddenClasses.length; i++) {
					hiddenClasses[i] = false;
				}
			}
			int nHidden = 0;
			for (boolean hiddenClasse : hiddenClasses)
				if (hiddenClasse) {
					nHidden++;
				}
			if (nHidden == 0) {
				for (int i = 0; i < hiddenClasses.length; i++) {
					hiddenClasses[i] = true;
				}
				hiddenClasses[n] = false;
			} else {
				hiddenClasses[n] = !hiddenClasses[n];
				nHidden = 0;
				for (boolean hiddenClasse : hiddenClasses)
					if (hiddenClasse) {
						nHidden++;
					}
				if (nHidden == getNClasses()) {
					for (int i = 0; i < hiddenClasses.length; i++) {
						hiddenClasses[i] = false;
					}
				}
			}
			setColorScale();
			return;
		}
		if (na2cm.isTfH(ae.getSource())) {
			StringTokenizer st = new StringTokenizer(na2cm.getTfHtext(), " ");
			FloatArray fa = new FloatArray(10, 10);
			while (st.hasMoreTokens()) {
				String str = st.nextToken();
				if (str == null || str.length() == 0) {
					break;
				}
				try {
					float f = Float.valueOf(str).floatValue();
					if (f >= minVh && f <= maxVh) {
						fa.addElement(f);
					}
				} catch (NumberFormatException nfe) {
				}
			}
			if (fa.size() > 0) {
				sliderh.removeAllBreaks();
				double br[] = new double[Math.min(fa.size(), sliderh.getMaxNBreaks())];
				for (int i = 0; i < br.length; i++) {
					br[i] = fa.elementAt(i);
				}
				QSortAlgorithm.QuickSort(br, 0, br.length - 1);
				for (double element : br) {
					sliderh.addBreak(element);
				}
				breaksChanged(sliderh, br, br.length);
			} else if (na2cm != null && !Double.isNaN(minVv)) {
				na2cm.setTextFieldAndLabels((float) minVv, (float) maxVv, (float) minVh, (float) maxVh);
			}
			return;
		}
		if (na2cm.isTfV(ae.getSource())) {
			StringTokenizer st = new StringTokenizer(na2cm.getTfVtext(), " ");
			FloatArray fa = new FloatArray(10, 10);
			while (st.hasMoreTokens()) {
				String str = st.nextToken();
				if (str == null || str.length() == 0) {
					break;
				}
				try {
					float f = Float.valueOf(str).floatValue();
					if (f >= minVv && f <= maxVv) {
						fa.addElement(f);
					}
				} catch (NumberFormatException nfe) {
				}
			}
			if (fa.size() > 0) {
				sliderv.removeAllBreaks();
				double br[] = new double[Math.min(fa.size(), sliderv.getMaxNBreaks())];
				for (int i = 0; i < br.length; i++) {
					br[i] = fa.elementAt(i);
				}
				QSortAlgorithm.QuickSort(br, 0, br.length - 1);
				for (double element : br) {
					sliderv.addBreak(element);
				}
				breaksChanged(sliderv, br, br.length);
			} else if (na2cm != null && !Double.isNaN(minVv)) {
				na2cm.setTextFieldAndLabels((float) minVv, (float) maxVv, (float) minVh, (float) maxVh);
			}
			return;
		}
		if (ae.getSource() instanceof Button) {
			String cmd = ae.getActionCommand();
			int n = 0;
			try {
				n = Integer.valueOf(cmd).intValue();
			} catch (NumberFormatException nfe) {
				return;
			}
			if (n < 2 || n > 5)
				return;
			if (na2cm.isXYselected()) {
				for (int b = 0; b <= 1; b++) {
					Slider slider = (b == 0) ? sliderh : sliderv;
					double br[] = null;
					if (na2cm.isEqIntSelected()) {
						br = (b == 0) ? autoClassHEqInt(n) : autoClassVEqInt(n);
					} else {
						br = autoClassEqSize(b == 0, n);
					}
					slider.removeAllBreaks();
					for (double element : br) {
						slider.addBreak(element);
					}
					breaksChanged(slider, br, br.length);
				}
			} else {
				Slider slider = null;
				double br[] = null;
				if (na2cm.isXselected()) {
					slider = sliderh;
					if (na2cm.isEqIntSelected()) {
						br = autoClassHEqInt(n);
					} else {
						br = autoClassEqSize(true, n);
					}
				} else {
					slider = sliderv;
					if (na2cm.isEqIntSelected()) {
						br = autoClassVEqInt(n);
					} else {
						br = autoClassEqSize(false, n);
					}
				}
				slider.removeAllBreaks();
				for (double element : br) {
					slider.addBreak(element);
				}
				breaksChanged(slider, br, br.length);
			}
		}
	}

	/*
	* Results of the classification. Min and Max values are not listed in <breaks>.
	* If there are no breaks, breaks==null.
	* Note that breaks.length is not always equal to the real number of breaks!
	* Use nBreaks!
	*/
	@Override
	public void breaksChanged(Object source, double[] br, int nBreaks) {
		DoubleArray breaks = (source == sliderh) ? breaksh : breaksv;
		Slider slider = (Slider) source;
		if (breaks == null) {
			breaks = new DoubleArray(10, 10);
		} else {
			breaks.removeAllElements();
		}
		for (int i = 0; i < nBreaks; i++) {
			breaks.addElement((float) br[i]);
		}
		exposeAllClasses();
		changingClasses.removeAllElements();
		slider.exposeAllClasses();
		setColorScale();
		if (na2cm != null && !Double.isNaN(minVv)) {
			na2cm.setTextFieldAndLabels((float) minVv, (float) maxVv, (float) minVh, (float) maxVh);
		}
		notifyChange("classes");
	}

	private IntArray changingClasses = new IntArray(2, 2);

	/*
	* This function is called during the process of moving a delimiter
	* between classes. The classifier does nothing upon this event.
	*/
	@Override
	public void breakIsMoving(Object source, int n, double currValue) {
		//System.out.println("* n="+n);
		if (!dynUpdate)
			return;
		Slider slider = (Slider) source;
		int nBreaks = slider.getNBreaks();
		double br[] = new double[nBreaks];
		for (int i = 0; i < nBreaks; i++) {
			br[i] = (slider == sliderh) ? breaksh.elementAt(i) : breaksv.elementAt(i);
		}
		if (n < nBreaks) {
			br[n] = currValue;
		}
		breaksChanged(slider, br, nBreaks);
	}

	/**
	* Change of colors of the slider
	*/
	@Override
	public void colorsChanged(Object source) {
		notifyChange("colors");
	}

	public Rectangle drawClassSymbols(Graphics g, int startY, int leftmarg, int prefW) {
		if (data == null || attr == null || attr.size() < 2 || breaksh == null || breaksv == null)
			return new Rectangle(leftmarg, startY, 0, 0);
		if (Double.isNaN(minVh) || Double.isNaN(maxVh) || Double.isNaN(minVv) || Double.isNaN(maxVv)) {
			findMinMax();
		}
		FontMetrics fm = g.getFontMetrics();
		int asc = fm.getAscent(), asc2 = asc / 2, step = asc + 5;

		Point p = StringInRectangle.drawText(g, getAttributeName(1), leftmarg, startY, prefW, false);
		int y = p.y, maxX = p.x;
		int nh = breaksh.size() + 1, nv = breaksv.size() + 1;

		y += 10;
		int x0 = 10;
		int ncl = 0;
		for (int iv = nv - 1; iv >= 0; iv--) {
			for (int ih = 0; ih < nh; ih++) {
				g.setColor(this.getClassColor(ncl));
				g.fillRect(x0 + step * ih, y + step * iv, step, step);
				ncl++;
			}
		}
		g.setColor(Color.black);
		for (int iv = 0; iv <= nv; iv++) { // horisontal lines
			g.drawLine(x0, y + step * iv, x0 + step * nh, y + step * iv);
			double v = (iv == 0) ? maxVv : ((iv == nv) ? minVv : breaksv.elementAt(nv - iv - 1));
			g.drawString(StringUtil.doubleToStr(v, minVv, maxVv), x0 + step * nh + 12, y + step * iv + asc2);
		}
		int xx = x0 + step * nh, yy = y + step * nv;
		g.drawLine(xx, yy, xx + 10, yy);
		g.drawLine(xx + 5, yy - 5, xx + 10, yy);
		g.drawLine(xx + 5, yy + 5, xx + 10, yy);
		for (int ih = 0; ih <= nh; ih++) { // vertical lines
			g.drawLine(x0 + step * ih, y, x0 + step * ih, y + step * nv);
			double v = (ih == 0) ? minVh : ((ih == nh) ? maxVh : breaksh.elementAt(ih - 1));
			g.drawString(StringUtil.doubleToStr(v, minVh, maxVh), x0 + step * ih, y + step * nv + asc * (ih + 1));
		}
		g.drawLine(x0, y, x0, y - 10);
		g.drawLine(x0 - 5, y - 5, x0, y - 10);
		g.drawLine(x0 + 5, y - 5, x0, y - 10);
		y += nv * step + 15 + asc * nh;

		if (x0 + step * nh + 5 > maxX) {
			maxX = x0 + step * nh + 5;
		}

		p = StringInRectangle.drawText(g, getAttributeName(0), leftmarg, y, prefW, true);
		y = p.y;
		if (p.x > maxX) {
			maxX = p.x;
		}
		return new Rectangle(leftmarg, startY, maxX - leftmarg + Metrics.mm(), y - startY);
	}

	@Override
	public Rectangle drawClassStatistics(Graphics g, int startY, int leftmarg, int prefW) {
		if (data == null || attr == null || attr.size() < 2 || breaksh == null || breaksv == null)
			return new Rectangle(leftmarg, startY, 0, 0);
		FontMetrics fm = g.getFontMetrics();
		int asc = fm.getAscent(), step = asc + 5, rectW = Metrics.mm() * 4, ssx = leftmarg + rectW + Metrics.mm();
		int y = startY, maxX = prefW;
		y += asc / 2;
		int x0 = 2, y0 = y, mx = ssx;
		int nh = breaksh.size() + 1;
		for (int i = 0; i < getNClasses(); i++) {
			g.setColor(getClassColor(i));
			g.fillRect(x0 + leftmarg, y, rectW, step);
			g.setColor(Color.black);
			g.drawRect(x0 + leftmarg, y, rectW, step);
			y += step;
			if ((i + 1) % nh == 0) {
				y += step / 2;
			}
		}
		if (maxX < mx) {
			maxX = mx;
		}

		int nTotal = getSetSize();
		if (nTotal > 0) {
			y = y0 + step / 2 + asc / 2;
			mx += 2 * Metrics.mm();
			IntArray counts = getClassSizes(), countsFlt = getFilteredClassSizes();
			for (int i = getNClasses() - 1; i >= 0; i--) {
				float perc = 100.0f * counts.elementAt(getNClasses() - 1 - i) / nTotal;
				String str = String.valueOf(counts.elementAt(getNClasses() - 1 - i)) + " objects (" + StringUtil.floatToStr(perc, 0.0f, 100.0f) + "%)";
				if (countsFlt != null && counts.elementAt(i) != countsFlt.elementAt(i)) {
					perc = 100.0f * countsFlt.elementAt(i) / counts.elementAt(i);
					str += "; active: " + countsFlt.elementAt(i) + " objects (" + StringUtil.floatToStr(perc, 0.0f, 100.0f) + "%)";
				}
				g.drawString(str, mx, y);
				int x = mx + fm.stringWidth(str);
				if (x > maxX) {
					maxX = x;
				}
				y += step;
				if (i % nh == 0) {
					y += step / 2;
				}
			}
			y -= step;
		} else {
			y += asc;
		}
		return new Rectangle(leftmarg, startY, maxX - leftmarg + Metrics.mm(), y - startY);
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

		if (data == null || attr == null || attr.size() < 2 || breaksh == null || breaksv == null)
			return new Rectangle(leftmarg, startY, 0, 0);
		int y = startY;
		Rectangle r = drawClassSymbols(g, startY, leftmarg, prefW);
		if (r.width > prefW) {
			prefW = r.width;
		}
		y += r.height;
		r = drawClassStatistics(g, y, leftmarg, prefW);
		if (r.width > prefW) {
			prefW = r.width;
		}
		y += r.height;
		return new Rectangle(leftmarg, startY, prefW, y - startY);
	}

	/**
	* Checks if new values appeared in data and if this affects the classes
	*/
	@Override
	protected void checkValues() {
		findMinMax();
		if (na2cm != null && !Double.isNaN(minVv)) {
			na2cm.setTextFieldAndLabels((float) minVv, (float) maxVv, (float) minVh, (float) maxVh);
		}
	}

	/**
	* A classifier must give the user an opportunity to change interactively
	* colors assigned to classes. This method starts the procedure of class
	* color changing.
	*/
	@Override
	public void startChangeColors() {
		Panel p = new Panel(new ColumnLayout());
		CheckboxGroup cbg = new CheckboxGroup();
		Panel pp = new Panel(new FlowLayout());
		Checkbox cbColorHS = new Checkbox("Hue+Saturation", cbg, colorScaleNumber == -1);
		pp.add(cbColorHS);
		Checkbox cbColorMix = new Checkbox("Mix", cbg, colorScaleNumber != -1);
		pp.add(cbColorMix);
		p.add(pp);
		// following text:"Invert"
		Checkbox cbInvert = new Checkbox(res.getString("Invert"), colorScaleIsInverted);
		p.add(cbInvert);
		Choice chColor = new Choice();
		// following text:"Green & Red"
		chColor.add(res.getString("Green_Red"));
		// following text:"Red & Blue"
		chColor.add(res.getString("Red_Blue"));
		// following text:"Yellow & Red"
		chColor.add(res.getString("Yellow_Red"));
		// following text:"Yellow & Blue"
		chColor.add(res.getString("Yellow_Blue"));
		// following text:"Green & Blue"
		chColor.add(res.getString("Green_Blue"));
		// following text:"Yellow & Magenta"
		chColor.add(res.getString("Yellow_Magenta"));
		// following text:"Cyan & Blue"
		chColor.add(res.getString("Cyan_Blue"));
		if (colorScaleNumber >= 2) {
			chColor.select(colorScaleNumber - 2);
		}
		p.add(new Label(res.getString("select_color_scale_")));
		p.add(chColor);
		OKDialog okd = new OKDialog(CManager.getAnyFrame(), res.getString("Color_scale"), true);
		okd.addContent(p);
		okd.show();
		if (okd.wasCancelled())
			return;
		if (cbColorHS.getState()) {
			setColorScale(-1, cbInvert.getState());
		} else if (2 + chColor.getSelectedIndex() != colorScaleNumber || cbInvert.getState() != colorScaleIsInverted) {
			setColorScale(2 + chColor.getSelectedIndex(), cbInvert.getState());
		}
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

		param.put("colorScaleNumber", String.valueOf(colorScaleNumber == -1 ? -1 : colorScaleNumber - 2));
		param.put("colorScaleIsInverted", String.valueOf(colorScaleIsInverted));

		if (breaksh == null || breaksh.size() == 0) {
			param.put("breaksh", "null");
		} else {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < breaksh.size(); i++) {
				sb.append(String.valueOf(breaksh.elementAt(i)));
				sb.append(" ");
			}
			param.put("breaksh", sb.toString());
		}

		if (breaksv == null || breaksv.size() == 0) {
			param.put("breaksv", "null");
		} else {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < breaksv.size(); i++) {
				sb.append(String.valueOf(breaksv.elementAt(i)));
				sb.append(" ");
			}
			param.put("breaksv", sb.toString());
		}

		return param;
	}

	@Override
	public void setVisProperties(Hashtable param) {
		try {
			int csnum = Integer.parseInt((String) param.get("colorScaleNumber"));
			setColorScale(csnum == -1 ? -1 : csnum + 2, new Boolean((String) param.get("colorScaleIsInverted")).booleanValue());
		} catch (Exception ex) {
		}

		String sBreaks = (String) param.get("breaksh");
		if (sBreaks != null && sBreaks.length() > 0 && !sBreaks.equals("null")) {
			StringTokenizer st = new StringTokenizer(sBreaks, " ");
			DoubleArray newbreaks = new DoubleArray();
			while (st.hasMoreTokens()) {
				newbreaks.addElement(new Double(st.nextToken()).doubleValue());
			}
			setBreaksH(newbreaks, false);
		}

		sBreaks = (String) param.get("breaksv");
		if (sBreaks != null && sBreaks.length() > 0 && !sBreaks.equals("null")) {
			StringTokenizer st = new StringTokenizer(sBreaks, " ");
			DoubleArray newbreaks = new DoubleArray();
			while (st.hasMoreTokens()) {
				newbreaks.addElement(new Double(st.nextToken()).doubleValue());
			}
			setBreaksV(newbreaks, false);
		}

/*
    exposeAllClasses();
    changingClasses.removeAllElements();
//    slider.exposeAllClasses();
    setColorScale();
//    if (na2cm!=null && !Float.isNaN(minVv)) na2cm.setTextFieldAndLabels(minVv,maxVv,minVh,maxVh);
    notifyChange("classes");
*/

		super.setVisProperties(param);
//    setDataMinMax(dataMin, dataMax);
//    setFocuserMinMax(focuserMin, focuserMax);
	}
//~ID
}
