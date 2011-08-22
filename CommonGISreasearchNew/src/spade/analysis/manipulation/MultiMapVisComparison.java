package spade.analysis.manipulation;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;

import spade.analysis.system.Supervisor;
import spade.analysis.transform.AttributeTransformer;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.FocusListener;
import spade.lib.basicwin.Focuser;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.PlotCanvas;
import spade.lib.color.Rainbow;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeDataPortion;
import spade.vis.mapvis.MultiMapNumDrawer;

/**
* A MultiMapManipulator is a UI component supporting visual comparison of values
* of several comparable numeric attributes represented on "small multiples"
* (i.e. several maps shown simultaneously in the same canvas) to a
* user-specified reference value.
*/
public class MultiMapVisComparison extends Panel implements Manipulator, ActionListener, SliderListener, FocusListener, PropertyChangeListener, Destroyable {
	static ResourceBundle res = Language.getTextResource("spade.analysis.manipulation.Res");

	protected Checkbox dynRepaintCB = null;
	protected Slider slider = null;
	protected Focuser focuser = null;
	protected TextField cmpTF = null;
	protected AttributeDataPortion dataTable = null;
	protected MultiMapNumDrawer vis = null;
	protected Supervisor supervisor = null;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;

	/**
	* Construction of map manipulator. Returns true if successfully constructed.
	* Arguments: 1) supervisor responsible for dynamic linking of displays;
	* 2) visualizer or classifier to be manipulated; 3) table with source data;
	* 4) table filter
	* The Manipulator must check if the visualizer has the appropriate type.
	* For MultiMapManipulator visualizer should be an instance of MultiMapNumDrawer.
	*/
	@Override
	public boolean construct(Supervisor sup, Object visualizer, AttributeDataPortion dataTable) {
		if (visualizer == null || dataTable == null)
			return false;
		if (!(visualizer instanceof MultiMapNumDrawer))
			return false;
		this.vis = (MultiMapNumDrawer) visualizer;
		this.dataTable = dataTable;
		supervisor = sup;
		vis.addVisChangeListener(this);
		AttributeTransformer aTrans = vis.getAttributeTransformer();
		if (aTrans != null) {
			aTrans.addPropertyChangeListener(this);
		}

		setLayout(new BorderLayout());
		Panel p = new Panel(new BorderLayout());
		// following text: "Compare to:"
		p.add(new Label(res.getString("Compare_to_")), BorderLayout.WEST);
		double dmin = vis.getDataMin(), dmax = vis.getDataMax();
		cmpTF = new TextField((Double.isNaN(vis.getCmp())) ? "" : StringUtil.doubleToStr(vis.getCmp(), dmin, dmax), 6);
		p.add(cmpTF, BorderLayout.CENTER);
		ColumnLayout cl = new ColumnLayout();
		cl.setAlignment(ColumnLayout.Hor_Left);
		Panel pp = new Panel(cl);
		for (int i = 0; i < vis.getAttributes().size(); i++) {
			pp.add(new Label(vis.getAttrName(i)));
		}
		pp.add(new Line(false));
		pp.add(p);
		add(pp, "North");

		p = new Panel(new BorderLayout());
		TextField maxTF = new TextField(StringUtil.doubleToStr(dmax, dmin, dmax, true), 6);
		pp = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		// following text: "Max:"
		pp.add(new Label(res.getString("Max_")));
		pp.add(maxTF);
		p.add(pp, "North");
		TextField minTF = new TextField(StringUtil.doubleToStr(dmin, dmin, dmax, false), 6);
		pp = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		// following text: "Min:"
		pp.add(new Label(res.getString("Min_")));
		pp.add(minTF);
		p.add(pp, "South");

		slider = new Slider();
		slider.setIsHorisontal(false);
		slider.setMaxNBreaks(1);
		slider.setMinMax(dmin, dmax);
//ID
		slider.setFocusMinMax(vis.getFocuserMin(), vis.getFocuserMax());
//~ID
		slider.setMidPoint(vis.getCmp());
		slider.setPositiveHue(vis.getPositiveHue());
		slider.setNegativeHue(vis.getNegativeHue());
		slider.setUseShades(vis.usesShades());
		slider.setTextField(cmpTF);
		slider.addSliderListener(this);
		PlotCanvas canvas = new PlotCanvas();
		slider.setCanvas(canvas);
		canvas.setContent(slider);
		canvas.setInsets(4, 10, 0, 10);

		pp = new Panel(new BorderLayout());
		pp.add(canvas, "West");
		dynRepaintCB = new Checkbox(res.getString("dynamic_map_update"));
		FocuserWithMultiDotPlots fmdp = new FocuserWithMultiDotPlots(sup, vis, dataTable, false, minTF, maxTF, dynRepaintCB);
		pp.add(fmdp, "Center");
		p.add(pp, "West");
		add(p, "Center");

		p = new Panel(new ColumnLayout());
		p.add(new Line(false));
		p.add(dynRepaintCB);
		pp = new Panel(new BorderLayout());
		Rainbow rb = new Rainbow();
		rb.setActionListener(this);
		pp.add(rb, "West");
		p.add(pp);
		add(p, "South");

		focuser = fmdp.getFocuser();
		focuser.addFocusListener(this);
		return true;
	}

	/*
	* Results of visual comparison. nBreaks<=1
	*/
	@Override
	public void breaksChanged(Object source, double[] breaks, int nBreaks) {
		if (source == slider) {
			vis.setCmp(slider.getMidPoint());
		}
	}

	/*
	* This function is called during the process of moving a slider
	* <n> shows number of the slider (n==0 corresponds
	* to a delimiter between 0th and 1st classes)
	*/
	@Override
	public void breakIsMoving(Object source, int n, double currValue) {
		if (source == slider && dynRepaintCB.getState()) {
			vis.setCmp(slider.getMidPoint());
		}
	}

	/**
	* Reacts to limit changes in the focuser: makes the slider redraw itself
	* in correspondence with the current limits.
	*/
	@Override
	public void focusChanged(Object source, double lowerLimit, double upperLimit) {
		slider.setFocusMinMax(lowerLimit, upperLimit);
		vis.setFocuserMinMax(lowerLimit, upperLimit);
	}

	@Override
	public void limitIsMoving(Object source, int n, double currValue) {
		if (dynRepaintCB.getState()) {
			slider.setFocusMinMax(focuser.getCurrMin(), focuser.getCurrMax());
			vis.setFocuserMinMax(focuser.getCurrMin(), focuser.getCurrMax());
		}
	}

	/*
	* Action
	*/
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof Rainbow) {
			vis.startChangeColors();
			return;
		}
	}

	/**
	* Reaction to changes of the visualizer (e.g. colors may be changed through
	* the legend)
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(vis)) {
			if (vis.getPositiveHue() != slider.getPositiveHue() || vis.getNegativeHue() != slider.getNegativeHue()) {
				slider.setPositiveHue(vis.getPositiveHue());
				slider.setNegativeHue(vis.getNegativeHue());
				slider.redraw();
			}
		} else if (e.getSource() instanceof AttributeTransformer) {
			if (e.getPropertyName().equals("values")) {
				double dmin = vis.getDataMin(), dmax = vis.getDataMax();
				cmpTF.setText(StringUtil.doubleToStr(vis.getCmp(), dmin, dmax));
				slider.setMinMaxCmp(dmin, dmax, vis.getCmp());
			}
		}
	}

	/**
	* Change of colors, method from SliderListener interface
	*/
	@Override
	public void colorsChanged(Object source) {
	}

	/**
	* Removes itself from receivers of  events
	*/
	@Override
	public void destroy() {
		slider.removeSliderListener(this);
		destroyed = true;
	}

	/**
	* Replies whether is destroyed or not
	*/
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}
}