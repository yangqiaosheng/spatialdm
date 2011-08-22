package spade.analysis.manipulation;

import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Panel;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Drawable;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.Positioner;
import spade.lib.util.Aligner;
import spade.lib.util.DoubleArray;

/**
* A dichromatic slider allows to utilise a diverging color scale in
* classification of objects on the basis of a single numeric
* attribute by breaking its value range into intervals. The component contains
* a Slider for interactive moving of class breaks and a Positioner for selection
* of the midpoint of the diverging color scale.
*/
public class DichromaticSlider implements Drawable, PropertyChangeListener, SliderListener {
	protected Slider clSlider = null; // classification slider
	protected Positioner mpSlider = null; // middle point selector
	protected boolean isHorisontal = true;
	protected Rectangle bounds = null;
	protected boolean minMaxSet = false;
	protected Canvas canvas = null;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;

	public DichromaticSlider() {
		clSlider = new Slider();
		clSlider.addSliderListener(this);
		mpSlider = new Positioner();
		mpSlider.addPropertyChangeListener(this);
		setMidPoint(0f);
	}

	public void setMidPoint(double value) {
		mpSlider.setAbsPos(value);
		clSlider.setMidPoint(value);
	}

	/**
	* Sets the orientation of the slider
	*/
	public void setIsHorisontal(boolean value) {
		isHorisontal = value;
		clSlider.setIsHorisontal(value);
		mpSlider.setIsHorisontal(value);
	}

	public float getPositiveHue() {
		return clSlider.getPositiveHue();
	}

	public float getNegativeHue() {
		return clSlider.getNegativeHue();
	}

	public void setPositiveHue(float hue) {
		clSlider.setPositiveHue(hue);
	}

	public void setNegativeHue(float hue) {
		clSlider.setNegativeHue(hue);
	}

	/**
	* Sets the middle color of the diverging color scale
	*/
	public void setMiddleColor(Color color) {
		clSlider.setMiddleColor(color);
	}

	/**
	* Returns the middle color of the diverging color scale
	*/
	public Color getMiddleColor() {
		return clSlider.getMiddleColor();
	}

	public Slider getClassificationSlider() {
		return clSlider;
	}

	/**
	* The canvas in which this object should draw itself
	*/
	@Override
	public void setCanvas(Canvas c) {
		canvas = c;
		clSlider.setCanvas(c);
		mpSlider.setCanvas(c);
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension d1 = clSlider.getPreferredSize(), d2 = mpSlider.getPreferredSize();
		if (isHorisontal) {
			d1.height += d2.height;
		} else {
			d1.width += d2.width;
		}
		return d1;
	}

	/**
	* Sets boundaries in which the object should fit itself
	*/
	@Override
	public void setBounds(Rectangle bounds) {
		this.bounds = bounds;
		if (bounds == null)
			return;
		Dimension d1 = clSlider.getPreferredSize(), d2 = mpSlider.getPreferredSize();
		if (isHorisontal) {
			int h1 = d1.height, h2 = d2.height;
			if (h1 + h2 > bounds.height) {
				float ratio = 1.0f * bounds.height / (h1 + h2);
				h1 = Math.round(ratio * h1);
				h2 = bounds.height - h1;
			}
			clSlider.setBounds(new Rectangle(bounds.x, bounds.y, bounds.width, h1));
			mpSlider.setBounds(new Rectangle(bounds.x, bounds.y + h1, bounds.width, h2));
		} else {
			int w1 = d1.width, w2 = d2.width;
			if (w1 + w2 > bounds.width) {
				float ratio = 1.0f * bounds.width / (w1 + w2);
				w1 = Math.round(ratio * w1);
				w2 = bounds.width - w1;
			}
			clSlider.setBounds(new Rectangle(bounds.x, bounds.y, w1, bounds.height));
			mpSlider.setBounds(new Rectangle(bounds.x + w1, bounds.y, w2, bounds.height));
		}
	}

	@Override
	public Rectangle getBounds() {
		return bounds;
	}

	/**
	* Draws the object in the given graphics.
	*/
	@Override
	public void draw(Graphics g) {
		if (!minMaxSet) {
			double min = clSlider.getMin(), max = clSlider.getMax(), mp = clSlider.getMidPoint();
			mpSlider.setMinMax(min, max);
			if (mp < min || mp > max) {
				mp = (max <= 0) ? max : min;
				clSlider.setMidPoint(mp);
			}
			mpSlider.setAbsPos(mp);
			minMaxSet = true;
		}
		clSlider.draw(g);
		mpSlider.draw(g);
	}

	/**
	* A method from the SliderListener interface. A DichromaticSlider
	* ignores this event; it is interested only in the breakIsMoving event.
	*/
	@Override
	public void breaksChanged(Object source, double[] breaks, int nBreaks) {
		if (clSlider.getMin() != mpSlider.getMin() || clSlider.getMax() != mpSlider.getMax()) {
			mpSlider.setMinMax(clSlider.getMin(), clSlider.getMax());
			mpSlider.setAbsPos(clSlider.getMidPoint());
			mpSlider.redraw();
		}
	}

	/**
	* This function is called during the process of moving a delimiter
	* between classes. <n> shows number of the delimiter (n==0 corresponds
	* to a delimiter between 0th and 1st classes).
	* A DichromaticSlider adjust the position of the midpoint pointer so that
	* it is always in the middle of the respective class.
	*/
	@Override
	public void breakIsMoving(Object source, int n, double currValue) {
		double mp = mpSlider.getAbsPos();
		int k1, k2;
		double v1, v2;
		if (mp <= currValue) {
			k1 = n - 1;
			k2 = n;
			v2 = currValue;
			v1 = (k1 < 0) ? clSlider.getMin() : clSlider.getBreakValue(k1);
			if (mp < v1)
				return;
		} else {
			k1 = n;
			k2 = n + 1;
			v1 = currValue;
			v2 = (k2 >= clSlider.getNBreaks()) ? clSlider.getMax() : clSlider.getBreakValue(k2);
			if (mp > v2)
				return;
		}
		mp = (v1 + v2) / 2;
		clSlider.setMidPoint(mp);
		mpSlider.setAbsPos(mp);
		mpSlider.redraw();
	}

	/**
	 * A method from the SliderListener interface. A DichromaticSlider
	 * ignores this event; it is interested only in the breakIsMoving event.
	*/
	@Override
	public void colorsChanged(Object source) {
	}

	/**
	* Makes necessary operations for destroying, e.g. unregisters from
	* listening highlighting and other events.
	*/
	@Override
	public void destroy() {
		clSlider.destroy();
		mpSlider.destroy();
		mpSlider.removePropertyChangeListener(this);
		destroyed = true;
	}

	/**
	* Replies whether is destroyed or not
	*/
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	/*
	* Processes results of moving the midpoint in the midpoint positioner
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource() == mpSlider && e.getPropertyName().equals("pos_moved")) {
			double newval = mpSlider.getAbsPos(), oldval = clSlider.getMidPoint();
			DoubleArray da = clSlider.getBreaks();
			boolean same = (oldval < da.elementAt(0) && newval < da.elementAt(0)) || (oldval > da.elementAt(da.size() - 1) && newval > da.elementAt(da.size() - 1));
			for (int i = 0; i < da.size() - 1 && !same; i++) {
				same = newval >= da.elementAt(i) && newval <= da.elementAt(i + 1) && oldval >= da.elementAt(i) && oldval <= da.elementAt(i + 1);
			}
			if (same) {
				//System.out.println("* old="+oldval+", new="+newval);
				// ask user to set where to point
				CheckboxGroup cbg = new CheckboxGroup();
				Checkbox cb[] = new Checkbox[da.size() + 1];
				for (int i = 0; i < da.size(); i++) {
					cb[i] = new Checkbox("< " + da.elementAt(i), ((i == 0) && oldval <= da.elementAt(i)) || ((i > 0) && oldval >= da.elementAt(i - 1) && oldval <= da.elementAt(i)), cbg);
				}
				cb[cb.length - 1] = new Checkbox("> " + da.elementAt(da.size() - 1), false, cbg);
				Panel p = new Panel(new ColumnLayout());
				for (Checkbox element : cb) {
					p.add(element);
				}
				OKDialog ok = new OKDialog(CManager.getFrame(canvas), "Select reference class", true);
				ok.addContent(p);
				ok.show();
				if (!ok.wasCancelled()) {
					int n = -1;
					for (int i = 0; i < cb.length && n == -1; i++)
						if (cb[i].getState()) {
							n = i;
						}
					if (n >= 0) {
						if (n == 0) {
							newval = (mpSlider.getMin() + da.elementAt(0)) / 2;
						} else if (n == cb.length - 1) {
							newval = (mpSlider.getMax() + da.elementAt(da.size() - 1)) / 2;
						} else {
							newval = (da.elementAt(n - 1) + da.elementAt(n)) / 2;
						}
						mpSlider.setAbsPos(newval);
						mpSlider.redraw();
						clSlider.setMidPoint(newval);
						clSlider.redraw();
						clSlider.notifyColorsChanged();
					}
				}
			} else {
				clSlider.setMidPoint(newval);
				clSlider.redraw();
				clSlider.notifyColorsChanged();
			}
		}
	}

	public void setAligner(Aligner al) {
		clSlider.setAligner(al);
		mpSlider.setAligner(al);
	}

	public void redraw() {
		if (canvas != null) {
			Graphics g = canvas.getGraphics();
			if (g != null) {
				draw(g);
				g.dispose();
			}
		}
	}

}