package spade.analysis.classification;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.manipulation.DichromaticSlider;
import spade.analysis.manipulation.Manipulator;
import spade.analysis.manipulation.Slider;
import spade.analysis.manipulation.SliderListener;
import spade.analysis.system.Supervisor;
import spade.analysis.transform.AttributeTransformer;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.PlotCanvas;
import spade.lib.color.Rainbow;
import spade.lib.lang.Language;
import spade.lib.util.DoubleArray;
import spade.lib.util.FloatArray;
import spade.lib.util.IntArray;
import spade.lib.util.NumValManager;
import spade.lib.util.StringUtil;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.ThematicDataItem;
import spade.vis.mapvis.MultiNumAttr1ClassDrawer;

/**
* Manipulates the visualizer MultiNumAttr1ClassDrawer, which represents results
* of multiple 1-dimensional classifications by special signs with coloured
* segments. Allows to change class breaks and colors.
*/
public class MultiNumAttr1ClassManipulator extends Panel implements Manipulator, SliderListener, PropertyChangeListener, ActionListener, WindowListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.classification.Res");

	protected AttributeDataPortion dTable = null;

	/**
	* The slider is used for specifying class breaks
	*/
	private Slider slider = null;

	/**
	* The dichromatic slider is used for specifying class colors
	*/
	private DichromaticSlider dSlider = null;

	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;

	/**
	* The visualizer manipulated by this manipulator.
	*/
	protected MultiNumAttr1ClassDrawer vis = null;

	/**
	* The field for entering the breaks
	*/
	private TextField tfVals = null;

	protected Button bAutoClass = null, bRangedDist = null;

	protected Frame fRDMP = null;
	protected RangedDistMultiPanel RDMP = null;
	protected Supervisor supervisor = null;

	/**
	* The labels showing minimum and maximum attribute values
	*/
	private Label lMin = null, lMax = null;

	private Checkbox dynUpdateCB = null;
	/**
	* The maximum class size. Must be updated when the classes or data change.
	*/
	protected long maxClassSize = 0;

	/**
	* Construction of map manipulator. Returns true if successfully constructed.
	* Arguments: 1) supervisor responsible for dynamic linking of displays;
	* 2) visualizer or classifier to be manipulated; 3) table with source data;
	* 4) table filter
	* The Manipulator must check if the visualizer has the appropriate type.
	* For NumAttr1ClassManipulator visualizer should be an instance of NumAttr1Classifier.
	*/
	@Override
	public boolean construct(Supervisor sup, Object visualizer, AttributeDataPortion dataTable) {
		if (visualizer == null || dataTable == null)
			return false;
		if (!(visualizer instanceof MultiNumAttr1ClassDrawer))
			return false;
		supervisor = sup;
		vis = (MultiNumAttr1ClassDrawer) visualizer;
		vis.addVisChangeListener(this);
		dTable = dataTable;
		AttributeTransformer aTrans = vis.getAttributeTransformer();
		if (aTrans != null) {
			aTrans.addPropertyChangeListener(this);
		} else {
			dTable.addPropertyChangeListener(this);
		}

		setLayout(new ColumnLayout());
		Panel lp = new Panel();
		lp.setLayout(new BorderLayout());
		Rainbow rainbow = new Rainbow();
		rainbow.setActionListener(this);
		lp.add(rainbow, BorderLayout.WEST);
		dynUpdateCB = new Checkbox(res.getString("Dynamic_update"), false);
		lp.add(dynUpdateCB, BorderLayout.CENTER);
		add(lp);
		add(new Line(false));

		double min = vis.getDataMin(), max = vis.getDataMax();

		lMin = new Label(StringUtil.doubleToStr(min, min, max), Label.LEFT);
		lMax = new Label(StringUtil.doubleToStr(max, min, max), Label.RIGHT);
		tfVals = new TextField("");

		dSlider = new DichromaticSlider();
		slider = dSlider.getClassificationSlider();
		slider.setMinMax(min, max);
		slider.setIsHorisontal(true);
		slider.setMidPoint(min);
		FloatArray breaks = vis.getBreaks();
		if (breaks != null) {
			for (int i = 0; i < breaks.size(); i++) {
				slider.addBreak(breaks.elementAt(i));
			}
			if (breaks.size() > 1) {
				int k = breaks.size() / 2;
				slider.setMidPoint((breaks.elementAt(k - 1) + breaks.elementAt(k)) / 2);
			}
		}
//ID
		slider.setMidPoint(vis.getMiddleValue());
//~ID
		dSlider.setMidPoint(slider.getMidPoint());
		slider.setTextField(tfVals);
		slider.addSliderListener(this);
//ID
/*
    vis.setPositiveHue(slider.getPositiveHue());
    vis.setNegativeHue(slider.getNegativeHue());
    vis.setMiddleColor(slider.getMiddleColor());
*/
		slider.setPositiveHue(vis.getPositiveHue());
		slider.setNegativeHue(vis.getNegativeHue());
		slider.setMiddleColor(vis.getMiddleColor());
//~ID

		PlotCanvas slCanvas = new PlotCanvas();
		dSlider.setCanvas(slCanvas);
		slCanvas.setContent(dSlider);
		add(slCanvas);
		add(new Line(false));
		Panel pp = new Panel();
		pp.setLayout(new BorderLayout());
		pp.add(lMin, "West");
		pp.add(tfVals, "Center");
		pp.add(lMax, "East");
		add(pp);
		add(bAutoClass = new Button("Automatic classification"));
		bAutoClass.addActionListener(this);
		if (vis.isDiagramPresentation()) {
			add(new Line(false));
			add(new ColumnNumberControl(vis));
			add(new Line(false));
		}
		add(bRangedDist = new Button("Class distributions"));
		bRangedDist.addActionListener(this);
		for (int i = 0; i < vis.getNClasses(); i++) {
			vis.setClassColor(slider.getColor(i), i);
		}
		vis.classesHaveChanged();
		vis.notifyVisChange();
		getMaxClassSize();
		return true;
	}

	/**
	* Finds the maximum class size among all the classifiers
	*/
	protected void getMaxClassSize() {
		maxClassSize = 0;
		if (vis == null)
			return;
		Vector classifiers = vis.getClassifiers();
		if (classifiers == null || classifiers.size() < 1)
			return;
		for (int i = 0; i < classifiers.size(); i++) {
			Classifier cl = (Classifier) classifiers.elementAt(i);
			IntArray sizes = cl.getClassSizes();
			if (sizes == null || sizes.size() < 1) {
				continue;
			}
			for (int j = 0; j < sizes.size(); j++)
				if (sizes.elementAt(j) > maxClassSize) {
					maxClassSize = sizes.elementAt(j);
				}
		}
		if (RDMP != null) {
			RDMP.setMaxCount(maxClassSize);
		}
	}

	/*
	* Results of the classification. Min and Max values are not listed in <breaks>.
	* If there are no breaks, breaks==null.
	* Note that breaks.length is not always equal to the real number of breaks!
	* Use nBreaks!
	*/
	@Override
	public void breaksChanged(Object source, double[] breaks, int nBreaks) {
		slider.exposeAllClasses();
		slider.redraw();
		vis.exposeAllClasses();
		vis.setBreaks(DoubleArray.double2float(breaks), nBreaks);
		for (int i = 0; i < vis.getNClasses(); i++) {
			vis.setClassColor(slider.getColor(i), i);
		}
		vis.classesHaveChanged();
		vis.notifyVisChange();
		getMaxClassSize();
		if (RDMP != null) {
			RDMP.repaintAllCharts();
		}
	}

	/*
	* This function is called during the process of moving a delimiter
	* between classes. <n> shows number of the delimiter (n==0 corresponds
	* to a delimiter between 0th and 1st classes)
	*/
	@Override
	public void breakIsMoving(Object source, int n, double currValue) {
		if (!dynUpdateCB.getState())
			return;
		if (vis.getHiddenClassCount() == 0) {
			for (int i = 0; i < n; i++) {
				vis.setClassIsHidden(true, i);
				slider.setClassIsHidden(true, i);
			}
			for (int i = n + 2; i < vis.getNClasses(); i++) {
				vis.setClassIsHidden(true, i);
				slider.setClassIsHidden(true, i);
			}
		}
		vis.setBreak((float) currValue, n);
		vis.classesHaveChanged();
		vis.notifyVisChange();
		getMaxClassSize();
		if (RDMP != null) {
			RDMP.repaintAllCharts();
		}
	}

	/**
	* Change of colors in the slider
	*/
	@Override
	public void colorsChanged(Object source) {
		boolean changed = false;
		for (int i = 0; i < vis.getNClasses(); i++)
			if (!slider.getColor(i).equals(vis.getClassColor(i))) {
				vis.setClassColor(slider.getColor(i), i);
				changed = true;
			}
		if (changed) {
			vis.notifyVisChange();
			if (RDMP != null) {
				RDMP.repaintAllCharts();
			}
		}
	}

	protected void adjustToDataChange() {
		double min = vis.getDataMin(), max = vis.getDataMax();
		if (min == slider.getMin() && max == slider.getMax())
			return;
		slider.setMinMax(min, max);
		slider.removeSliderListener(this);
		slider.removeAllBreaks();
		FloatArray breaks = vis.getBreaks();
		if (breaks == null) {
			breaks = new FloatArray(10, 10);
		}
		newBreaks(breaks, false);
		lMin.setText(StringUtil.doubleToStr(min, min, max));
		lMax.setText(StringUtil.doubleToStr(max, min, max));
		CManager.validateAll(lMin);
		slider.redraw();
		slider.fillTextField();
		slider.addSliderListener(this);
		colorsChanged(this);
	}

	private void newBreaks(FloatArray breaks, boolean resetMidPoint) {
		double min = vis.getDataMin(), max = vis.getDataMax();
		for (int i = breaks.size() - 1; i >= 0; i--)
			if (breaks.elementAt(i) <= min || breaks.elementAt(i) >= max) {
				breaks.removeElementAt(i);
			}
		if (breaks.size() < 1) {
			double v = (max - min) / 3;
			breaks.addElement((float) (min + v));
			breaks.addElement((float) (min + v * 2));
		}
		for (int i = 0; i < breaks.size(); i++) {
			slider.addBreak(breaks.elementAt(i));
		}
		if (resetMidPoint || slider.getMidPoint() < min || slider.getMidPoint() > max) {
			slider.setMidPoint(min);
			if (breaks.size() > 1) {
				int k = breaks.size() / 2;
				slider.setMidPoint((breaks.elementAt(k - 1) + breaks.elementAt(k)) / 2);
			}
			dSlider.setMidPoint(slider.getMidPoint());
		}
		double br[] = new double[breaks.size()];
		for (int i = 0; i < br.length; i++) {
			br[i] = breaks.elementAt(i);
		}
		breaksChanged(this, br, br.length);
	}

	protected void autoClass() {
		Panel p = new Panel();
		p.setLayout(new ColumnLayout());
		p.add(new Label("Automatic classification", Label.CENTER));
		p.add(new Line(false));
		p.add(new Label("Class number:", Label.CENTER));
		Panel pp = new Panel();
		pp.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		p.add(pp);
		CheckboxGroup cbg = new CheckboxGroup();
		Checkbox cbN[] = new Checkbox[20];
		for (int i = 0; i < cbN.length; i++) {
			pp.add(cbN[i] = new Checkbox(Integer.toString(2 + i), 2 + i == vis.getNClasses(), cbg));
		}
		p.add(new Label("Procedure:", Label.CENTER));
		cbg = new CheckboxGroup();
		Checkbox cbEqInt = new Checkbox("Equal intervals", true, cbg), cbEqCl = new Checkbox("Equal classes for:", false, cbg);
		p.add(cbEqInt);
		p.add(cbEqCl);
		pp = new Panel();
		pp.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		p.add(pp);
		pp.add(new Label(" "));
		Vector attr = vis.getAttributeList();
		Choice ch = new Choice();
		for (int i = 0; i < attr.size(); i++) {
			ch.addItem(dTable.getAttributeName((String) attr.elementAt(i)));
		}
		pp.add(ch);
		OKDialog okDlg = new OKDialog(CManager.getAnyFrame(this), "Automatic classification", true);
		okDlg.addContent(p);
		okDlg.show();
		if (okDlg.wasCancelled())
			return;
		int ncl = -1;
		for (int i = 0; i < cbN.length && ncl == -1; i++)
			if (cbN[i].getState()) {
				ncl = i + 2;
			}
		if (ncl < 2) {
			ncl = 3;
		}
		FloatArray breaks = new FloatArray(ncl, 10);
		if (cbEqInt.getState()) {
			double min = vis.getDataMin(), max = vis.getDataMax();
			double d = (max - min) / ncl;
			for (int i = 1; i < ncl; i++) {
				breaks.addElement((float) (min + i * d));
			}
		} else {
			//int attrN=vis.getColumnN(ch.getSelectedIndex());
			FloatArray fa = new FloatArray(dTable.getDataItemCount(), 10);
			for (int i = 0; i < dTable.getDataItemCount(); i++) {
				double f = vis.getNumericAttrValue((ThematicDataItem) dTable.getDataItem(i), ch.getSelectedIndex());
				//dTable.getNumericAttrValue(attrN,i);
				if (!Double.isNaN(f)) {
					fa.addElement((float) f);
				}
			}
			float br[] = NumValManager.breakToIntervals(fa, ncl, true);
			for (int i = 0; i < br.length - 2; i++) {
				breaks.addElement(br[i + 1]);
			}
		}
		slider.removeSliderListener(this);
		slider.removeAllBreaks();
		newBreaks(breaks, true);
		CManager.validateAll(lMin);
		slider.redraw();
		slider.fillTextField();
		slider.addSliderListener(this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource().equals(vis)) {
			if (pce.getPropertyName().equals("hues")) {
				slider.setPositiveHue(vis.getPositiveHue());
				slider.setNegativeHue(vis.getNegativeHue());
				slider.setMiddleColor(vis.getMiddleColor());
				slider.redraw();
				slider.notifyColorsChanged();
			} else if (RDMP != null && vis.getNColumns() != RDMP.getNColumns()) {
				RDMP.setNColumns(vis.getNColumns());
			}
		} else if (pce.getSource().equals(dTable)) {
			if (pce.getPropertyName().equals("destroyed") || pce.getPropertyName().equals("structure_complete")) {
				destroy();
			} else if (pce.getPropertyName().equals("values") || pce.getPropertyName().equals("data_removed") || pce.getPropertyName().equals("data_added") || pce.getPropertyName().equals("data_updated")) {
				adjustToDataChange();
			}
		} else if (pce.getSource() instanceof AttributeTransformer) {
			if (pce.getPropertyName().equals("values")) {
				adjustToDataChange();
			} else if (pce.getPropertyName().equals("destroyed")) {
				destroy();
			}
		}
	}

	/**
	* Reacts to clicking on the "rainbow" icon
	*/
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(bAutoClass)) {
			autoClass();
			return;
		}
		if (e.getSource().equals(bRangedDist)) {
			if (RDMP == null) {
				Vector names = new Vector(vis.getNClassifiers(), 1);
				Attribute parent = null;
				boolean commonParent = true;
				for (int i = 0; i < vis.getNClassifiers() && commonParent; i++) {
					Attribute at = dTable.getAttribute(vis.getAttrId(i));
					if (at.getParent() == null) {
						commonParent = false;
					} else if (parent == null) {
						parent = at.getParent();
					} else {
						commonParent = parent.equals(at.getParent());
					}
				}
				if (!commonParent) {
					for (int i = 0; i < vis.getNClassifiers(); i++) {
						String name = vis.getInvariant(i);
						if (name == null) {
							name = vis.getAttrName(i);
						}
						names.addElement(name);
					}
				} else {
					for (int i = 0; i < vis.getNClassifiers(); i++) {
						Attribute at = dTable.getAttribute(vis.getAttrId(i));
						String name = null;
						int npar = at.getParameterCount();
						for (int j = 0; j < npar; j++) {
							String str = at.getParamValue(j).toString();
							if (npar > 1) {
								str = at.getParamName(j) + " = " + str;
							}
							if (name == null) {
								name = str;
							} else {
								name += "; " + str;
							}
						}
						if (name == null) {
							name = vis.getAttrName(i);
						}
						names.addElement(name);
					}
				}
				RDMP = new RangedDistMultiPanel(supervisor, vis.getClassifiers(), names, vis.getNColumns());
				RDMP.setMaxCount(maxClassSize);
				fRDMP = new Frame("RD...");
				fRDMP.add(RDMP);
				fRDMP.addWindowListener(this);
				fRDMP.setSize(400, 300);
				fRDMP.show();
			}

			fRDMP.toFront();
			return;
		}
		if (vis.isEnabled()) {
			vis.startChangeColors();
		}
	}

	public void destroy() {
		if (fRDMP != null) {
			fRDMP.dispose();
			fRDMP = null;
			RDMP = null;
		}
		if (dTable != null) {
			dTable.removePropertyChangeListener(this);
		}
		destroyed = true;
	}

	@Override
	public void windowOpened(WindowEvent e) {
	}

	@Override
	public void windowClosing(WindowEvent e) {
		if (fRDMP != null) {
			fRDMP.dispose();
			fRDMP = null;
			RDMP = null;
		}
	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}
}