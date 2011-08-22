package spade.analysis.manipulation;

import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ResourceBundle;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.PaintCanvas;
import spade.lib.basicwin.PlotCanvas;
import spade.lib.color.ColorScale;
import spade.lib.lang.Language;

public class GridManipulator extends Panel implements ActionListener, SliderListener, ItemListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.manipulation.Res");

	protected float min = Float.NEGATIVE_INFINITY;
	protected float max = Float.POSITIVE_INFINITY;
	protected Slider slider = new Slider();
	protected PaintCanvas legendBarCanvas = new PaintCanvas(0, 10) {
		@Override
		public void paint(Graphics g) {
//      if (cs!=null) cs.drawWholeColorBar(g, 0, 0, getWidth(), getHeight());
			if (cs != null) {
				cs.drawColorBar(g, (int) ((cs.getMinLimit() - cs.getMinValue()) / (cs.getMaxValue() - cs.getMinValue()) * getSize().width), 0, (int) ((cs.getMaxLimit() - cs.getMinLimit()) / (cs.getMaxValue() - cs.getMinValue()) * getSize().width),
						getSize().height);
			}
		}
	};
	private ColorScale cs = null;
	protected Checkbox dynUpd = null;
	protected Checkbox cSmooth = null;
	protected Checkbox cIsoOnly = null;
	protected boolean dynamic = false;

	public GridManipulator(float min, float max) {
		this.min = min;
		this.max = max;

		setLayout(new ColumnLayout());
	}

	public void addComponents() {
		add(new Line(false));

		add(new Label(res.getString("Drawing_properties"), Label.CENTER));

		cSmooth = new Checkbox(res.getString("interpolation"), true);
		cIsoOnly = new Checkbox(res.getString("isolines_only"), false);
		cSmooth.addItemListener(this);
		cIsoOnly.addItemListener(this);
		add(cSmooth);
		add(cIsoOnly);

		if (cs != null) {
			dynamic = cs.getDynamic();
		}
		dynUpd = new Checkbox(res.getString("dynamic_map_update"), dynamic);
		dynUpd.addItemListener(this);
		add(dynUpd);

		add(new Line(false));
		add(new Label(res.getString("Isolines"), Label.CENTER));

		TextField breaks = new TextField();
		slider.setMinMax(min, max);
		slider.setMidPoint(min);
		slider.setMaxNBreaks(50);

		Color[] colors = new Color[slider.getMaxNBreaks() + 1];
		for (int i = 0; i < colors.length; i++) {
			colors[i] = Color.lightGray;
		}
		slider.setColors(colors);

		PlotCanvas slCanvas = new PlotCanvas();
		slider.setCanvas(slCanvas);
		slider.setTextField(breaks);
		slider.addSliderListener(this);
		slCanvas.setContent(slider);
		slCanvas.setVisible(true);

		add(legendBarCanvas);
		add(slCanvas);
		add(breaks);

		add(new Line(false));
	}

//----------------- notification about changes of the color scale---------------
	/**
	* A ColorScaleManipulator may have a number of listeners of changes
	* of the color scale (PropertyChangeListeners).
	* To handle the list of listeners and notify them about changes of the
	* properties, a ColorScaleManipulator uses a PropertyChangeSupport.
	*/
	protected PropertyChangeSupport pcSupport = null;

	@Override
	public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
		if (l == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(l);
	}

	@Override
	public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
		if (l == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(l);
	}

	public void notifyPropertyChange(String propName, Object oldValue, Object newValue) {
		if (pcSupport == null)
			return;
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		pcSupport.firePropertyChange(propName, oldValue, newValue);
		setCursor(Cursor.getDefaultCursor());
	}

	/**
	 * Invoked when an action occurs.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		notifyPropertyChange("Isolines", null, null);
	}

	/**
	 * Results of the classification. Min and Max values are not listed in <breaks>.
	 * If there are no breaks, breaks==null.
	 * Note that breaks.length is not always equal to the real number of breaks!
	 * Use nBreaks!
	 */
	@Override
	public void breaksChanged(Object source, double[] breaks, int nBreaks) {
		float[] iso = new float[slider.getNBreaks()];
		for (int i = 0; i < iso.length; i++) {
			iso[i] = (float) slider.getBreakValue(i);
		}
		notifyPropertyChange("Isolines", null, iso);
	}

	/**
	 * This function is called during the process of moving a delimiter
	 * between classes. <n> shows number of the delimiter (n==0 corresponds
	 * to a delimiter between 0th and 1st classes)
	 */
	@Override
	public void breakIsMoving(Object source, int n, double currValue) {
		if (dynamic) {
			float[] iso = new float[slider.getNBreaks()];
			for (int i = 0; i < iso.length; i++) {
				iso[i] = (float) slider.getBreakValue(i);
			}
			notifyPropertyChange("Isolines", null, iso);
		}
	}

	/**
	 * Change of colors
	 */
	@Override
	public void colorsChanged(Object source) {
/*
    float[] iso = new float[slider.getNBreaks()];
    for (int i=0; i<iso.length; i++)
      iso[i] = slider.getBreakValue(i);
    notifyPropertyChange("Isolines", null, iso);
*/
	}

//----------------- end notification about changes of the color scale---------------
	public void setColorScale(ColorScale cs) {
		this.cs = cs;
		if (cs != null) {
			cs.setDynamic(dynamic);
		}
		legendBarCanvas.repaint();
	}

	public void setDrawSmooth(boolean smooth) {
		cSmooth.setState(smooth);
//    cMosaic.setState(!smooth);
	}

	public void setDynamic(boolean dynamic) {
		this.dynamic = dynamic;
		dynUpd.setState(dynamic);
		if (cs != null) {
			cs.setDynamic(dynamic);
		}
	}

	public void setIsolinesOnly(boolean isoOnly) {
		cIsoOnly.setState(isoOnly);
	}

	public void setIsolines(float[] iso) {
		if (iso == null)
			return;
		slider.removeAllBreaks();
		for (float element : iso) {
			slider.addBreak(element);
		}
		slider.notifyBreaksChange();
	}

	/**
	 * Invoked when an item has been selected or deselected by the user.
	 * The code written for this method performs the operations
	 * that need to occur when an item is selected (or deselected).
	 */
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == dynUpd) {
			dynamic = dynUpd.getState();
			if (cs != null) {
				cs.setDynamic(dynamic);
			}
		} else if (e.getSource() == cSmooth/* || e.getSource() == cMosaic*/) {
			notifyPropertyChange("DrawSmooth", null, new Boolean(cSmooth.getState()));
		} else if (e.getSource() == cIsoOnly) {
			notifyPropertyChange("IsolinesOnly", null, new Boolean(cIsoOnly.getState()));
		}
	}
}
