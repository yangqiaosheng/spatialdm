package spade.lib.color;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import spade.lib.basicwin.Metrics;
import spade.lib.util.StringUtil;

/**
* Provides encoding of numeric values by colors
*/
public abstract class BaseColorScale implements ColorScale {
	/**
	* Absolute minimum and maximum values of the value range to be encoded
	*/
//  protected float minV=0.0f, maxV=0.0f;
	protected float minV = Float.NaN, maxV = Float.NaN;
	/**
	* Minimum and maximum values of the visible value range
	*/
//  protected float minLimit=0.0f, maxLimit=0.0f;
	protected float minLimit = Float.NaN, maxLimit = Float.NaN;
	/**
	* Transparency of the color scale
	*/
	protected int alpha = 0xFF, alphaFactor = (alpha << 24) | 0x00FFFFFF;
	/**
	 * Specifies whether notifications for map updates should be sent dynamically
	 */
	protected boolean dynamic = false;

	@Override
	public void setDynamic(boolean dynamic) {
		this.dynamic = dynamic;
	}

	@Override
	public boolean getDynamic() {
		return dynamic;
	}

	/**
	* Registers listeners of property changes and sends them notifications
	*/
	protected PropertyChangeSupport pcSup = null;

	/**
	* Sets the alpha (transparency) value
	*/
	@Override
	public void setAlpha(float value) {
		if (value < 0.0f) {
			value = 0.0f;
		} else if (value > 1.0f) {
			value = 1.0f;
		}
		alpha = Math.round(value * 255);
		alphaFactor = (alpha << 24) | 0x00FFFFFF;
	}

	@Override
	public float getAlpha() {
		return alpha / 255.0f;
	}

	/**
	* Sets the minimum and the maximum possible values
	*/
	@Override
	public void setMinMax(float min, float max) {
		minV = min;
		maxV = max;
		if (Float.isNaN(minLimit)) {
			minLimit = minV;
		}
		if (Float.isNaN(maxLimit)) {
			maxLimit = maxV;
//    minLimit=minV; maxLimit=maxV;
		}
	}

	@Override
	public float getMinValue() {
		return minV;
	}

	@Override
	public float getMaxValue() {
		return maxV;
	}

	/**
	* Sets the filter: the threshold below which the pixels will be invisible
	*/
	@Override
	public void setMinLimit(float limit) {
		if (limit >= minV && limit <= maxV) {
			minLimit = limit;
		} else {
			minLimit = minV;
		}
	}

	@Override
	public float getMinLimit() {
		if (minLimit < minV)
			return minV;
		return minLimit;
	}

	/**
	* Sets the filter: the threshold above which the pixels will be invisible
	*/
	@Override
	public void setMaxLimit(float limit) {
		if (limit >= minV && limit <= maxV) {
			maxLimit = limit;
		} else {
			maxLimit = maxV;
		}
	}

	@Override
	public float getMaxLimit() {
		if (maxLimit > maxV)
			return maxV;
		return maxLimit;
	}

	/**
	* Returns the color for the given value
	*/
	@Override
	public Color getColorForValue(float value) {
		return new Color(getPackedColorForValue(value)/*,true*/);
	}

	/**
	* Returns the color for the given value packed into an integer:
	* top 8 bits (0xFF << 24) - alpha value
	* 2nd 8 bits (0xFF << 16) - red value
	* 3rd 8 bits (0xFF << 8) - green value
	* bottom 8 bits (0xFF) - blue value
	*/
	@Override
	public int getPackedColorForValue(float val) {
		if ((alphaFactor & 0xFF000000) == 0x00000000)
			return 0x00000000;
		if (val < minLimit || val > maxLimit)
			return 0x00000000;
		return encodeValue(val) & alphaFactor;
	}

	/**
	* To be implemented in subclasses
	*/
	protected abstract int encodeValue(float val);

	/**
	* A ColorScale should be able to add its description at the end of the
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
	public Rectangle drawLegend(Graphics g, int startY, int leftMarg, int prefW) {
		int barH = 4 * Metrics.mm();
		int y = startY, by = y + barH, maxX = leftMarg + prefW, barW = prefW - 2 * leftMarg;
		drawColorBar(g, leftMarg, y, barW, barH);
		//show the minimum value
		FontMetrics fm = g.getFontMetrics();
		int fh = fm.getHeight(), asc = fm.getAscent();
		y += barH + Metrics.mm() / 2;

		String str = StringUtil.floatToStr(minLimit, minLimit, maxLimit);
		g.setColor(Color.black);
		g.drawString(str, leftMarg, y + asc);
		g.drawLine(leftMarg, by, leftMarg, y);
		int sw = fm.stringWidth(str), x1 = leftMarg + sw;
		if (maxX < x1) {
			maxX = x1;
		}
		//show the maximum value
		str = StringUtil.floatToStr(maxLimit, minLimit, maxLimit);
		sw = fm.stringWidth(str);
		int x = leftMarg + barW - sw;
		if (x < x1 + 2 * Metrics.mm()) {
			y += fh;
		}
		if (x < leftMarg) {
			x = leftMarg;
		}
		g.drawString(str, x, y + asc);
		g.drawLine(leftMarg + barW, by, leftMarg + barW, y);
		y += fh;
		if (maxX < x + sw) {
			maxX = x + sw;
		}

		return new Rectangle(leftMarg, startY, maxX - leftMarg, y - startY);
	}

	/**
	* Draws a color bar representing this color scale
	*/
	@Override
	public void drawColorBar(Graphics g, int x, int y, int w, int h) {
		int nSteps = w / 3;

		int xpos = 0;
		for (int i = 0; i < nSteps; i++) {
			float v = minLimit + ((float) xpos) / w * (maxLimit - minLimit);
			g.setColor(new Color(encodeValue(v)));
			int dx = Math.round(1.0f * (w - xpos) / (nSteps - i));
			g.fillRect(x + xpos, y, dx + 1, h);
			xpos += dx;
		}
	}

	/**
	* Returns a UI component that allows the user to manipulate specific
	* parameters of this color scale (i.e. not transparency or minimum and
	* maximum limits that are manipulated in the same way for all color scales)
	*/
	@Override
	public abstract Component getManipulator();

	/**
	* Registers a listener of changes of properties of the color scale
	*/
	@Override
	public void addPropertyChangeListener(PropertyChangeListener list) {
		if (list == null)
			return;
		if (pcSup == null) {
			pcSup = new PropertyChangeSupport(this);
		}
		pcSup.addPropertyChangeListener(list);
	}

	/**
	* Removes the listener of property changes
	*/
	@Override
	public void removePropertyChangeListener(PropertyChangeListener list) {
		if (pcSup != null && list != null) {
			pcSup.removePropertyChangeListener(list);
		}
	}

	/**
	* Notifies about changes of the color scale
	*/
	public void notifyScaleChange() {
		if (pcSup != null) {
			pcSup.firePropertyChange("color_scale", null, this);
		}
	}

	/**
	 * Pass parameter string to setup color scale
	 */
	@Override
	public void setParameters(String par) {
	}

	/**
	 * Pass parameter string to setup color scale
	 */
	@Override
	public String getParameters() {
		return "";
	}
}
