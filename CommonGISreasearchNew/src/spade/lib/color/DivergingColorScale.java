package spade.lib.color;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.Slider;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;

/**
* Provides encoding of numeric values by colors. A diverging color scale has a
* special reference value (midpoint) and encodes values above the midpoint
* in shades of one color and values below the midpoint in shades of another
* color. For a diverging color scale it is possible to change interactively
* the midpoint and the colors for the left and the right parts of the scale.
*/
public class DivergingColorScale extends BaseColorScale implements ActionListener {
	static ResourceBundle res = Language.getTextResource("spade.lib.color.Res");
	protected float midPoint = 0.0f;
	protected float posHue = 0.125f, negHue = 0.6f;
	/**
	* Controls for manipulation of color scale parameters
	*/
	protected TextField midPointTF = null;
	protected Slider midPointSl = null;
	protected Rainbow posrb = null, negrb = null;

	public void setMidPoint(float value) {
		midPoint = value;
	}

	public float getMidPoint() {
		return midPoint;
	}

	public void setPosHue(float value) {
		if (value >= 0.0f && value <= 1.0f) {
			posHue = value;
		}
	}

	public float getPosHue() {
		return posHue;
	}

	public void setNegHue(float value) {
		if (value >= 0.0f && value <= 1.0f) {
			negHue = value;
		}
	}

	public float getNegHue() {
		return negHue;
	}

	/**
	* Returns the color for the given value packed into an integer:
	* top 8 bits (0xFF << 24) - alpha value
	* 2nd 8 bits (0xFF << 16) - red value
	* 3rd 8 bits (0xFF << 8) - green value
	* bottom 8 bits (0xFF) - blue value
	*/
	@Override
	public int encodeValue(float val) {
		float min = minLimit - midPoint, max = maxLimit - midPoint;
		val -= midPoint;
		if (val < min) {
			val = min;
		}
		if (val > max) {
			val = max;
		}
		int color = 0xFFFFFFFF; //white
		if (val != 0.0) {
			float maxMod = Math.abs(max);
			if (Math.abs(min) > maxMod) {
				maxMod = Math.abs(min);
			}
			float ystart = 0.0f;
			if (min > 0.0) {
				ystart = min;
			} else if (max < 0.0) {
				ystart = -max;
			}
			float hue = (val >= 0.0) ? posHue : negHue;
			float minS = 0.4f, minB = 0.4f;
			if (hue >= 1.2f && hue <= 0.5f) {
				minS += 0.1f;
				minB -= 0.1f;
			}
			float slength = 1.0f - minS, sblength = 1.0f - minB + slength;
			float ratio = ((Math.abs(val) - ystart) / (maxMod - ystart)), sbvalue = ratio * sblength, sat = (sbvalue <= slength) ? minS + sbvalue : 1.0f, br = (sbvalue <= slength) ? 1.0f : minB + sblength - sbvalue;
			color = Color.HSBtoRGB(hue, sat, br);
		}
		return color;
	}

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
		if (midPoint <= minLimit || midPoint >= maxLimit)
			return super.drawLegend(g, startY, leftMarg, prefW);
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
		int x2 = leftMarg + barW - sw;
		if (x2 < x1 + 2 * Metrics.mm()) {
			y += fh;
		}
		if (x2 < leftMarg) {
			x2 = leftMarg;
		}
		g.drawString(str, x2, y + asc);
		g.drawLine(leftMarg + barW, by, leftMarg + barW, y);
		if (maxX < x2 + sw) {
			maxX = x2 + sw;
		}
		//show the midpoint value
		str = StringUtil.floatToStr(midPoint, minLimit, maxLimit);
		sw = fm.stringWidth(str);
		int x = leftMarg + Math.round((midPoint - minLimit) / (maxLimit - minLimit) * barW);
		int x3 = x, yy = y;
		if (x > x1 && x < x2 && sw < x2 - x1 - 4 * Metrics.mm()) {
			x3 = x - sw / 2;
			if (x3 < x1 + 2 * Metrics.mm()) {
				x3 = x1 + 2 * Metrics.mm();
			}
			if (x3 + sw > x2 - 2 * Metrics.mm()) {
				x3 = x2 - 2 * Metrics.mm() - sw;
			}
		} else {
			y += fh;
			yy = startY + barH + Metrics.mm() / 2;
			x3 = x - sw / 2;
			if (x3 < leftMarg) {
				x3 = leftMarg;
			}
			if (x3 + sw > leftMarg + prefW) {
				x3 = leftMarg + prefW - sw;
			}
		}
		g.setColor(Color.white);
		g.fillRect(x - 1, startY, 3, barH);
		g.setColor(Color.black);
		g.drawString(str, x3, y + asc);
		g.drawLine(x, by, x, yy);
		if (maxX < x3 + sw) {
			maxX = x3 + sw;
		}
		y += fh;

		return new Rectangle(leftMarg, startY, maxX - leftMarg, y - startY);
	}

	/**
	* Draws a color bar representing this color scale
	*/
	@Override
	public void drawColorBar(Graphics g, int x, int y, int w, int h) {
		int nSteps = w / 3;

		int xpos = 0;
		float v = minLimit;
		for (int i = 0; i < nSteps; i++) {
			int dx = Math.round(1.0f * (w - xpos) / (nSteps - i));
			float v1 = minLimit + ((float) (xpos + dx)) / w * (maxLimit - minLimit);
			g.setColor(new Color(encodeValue(v)));
			g.fillRect(x + xpos, y, dx + 1, h);
			if (v < midPoint && v1 > midPoint) {
				int x1 = Math.round((midPoint - minLimit) / (maxLimit - minLimit) * w);
				g.setColor(Color.white);
				g.fillRect(x + x1, y, dx + 1, h);
			}
			xpos += dx;
			v = v1;
		}
	}

	/**
	* Returns a UI component that allows the user to manipulate specific
	* parameters of this color scale (i.e. not transparency or minimum and
	* maximum limits that are manipulated in the same way for all color scales)
	* For a diverging color scale it is possible to change interactively
	* the midpoint and the colors for the left and the right parts of the scale.
	*/
	@Override
	public Component getManipulator() {
		Panel dcsPanel = new Panel(new ColumnLayout());
		String str = StringUtil.floatToStr(minV, minV, maxV);
		int l = str.length();
		str = StringUtil.floatToStr(maxV, maxV, maxV);
		int l0 = str.length();
		if (l < l0) {
			l = l0;
		}
		str = StringUtil.floatToStr(midPoint, maxV, maxV);
		l0 = str.length();
		if (l < l0) {
			l = l0;
		}
		midPointTF = new TextField(str, l0 + 2);
		midPointTF.addActionListener(this);
		Panel p = new Panel(new BorderLayout());
		// following string: "Set the midpoint of the scale:"
		p.add(new Label(res.getString("Set_the_midpoint_of")), "West");
		p.add(midPointTF, "East");
		dcsPanel.add(p);
		midPointSl = new Slider(this, minV, maxV, midPoint);
		midPointSl.setNAD(true);
		dcsPanel.add(midPointSl);
		p = new Panel(new GridLayout(4, 1));
		// following string: "Color for the left (negative) part of the scale:"
		p.add(new Label(res.getString("Color_for_the_left")));
		negrb = new Rainbow();
		negrb.usedToSelectHue = true;
		negrb.setCurrHue(negHue);
		negrb.setActionListener(this);
		p.add(negrb);
		// following string: ("Color for the right (positive) part of the scale:"
		p.add(new Label(res.getString("Color_for_the_right")));
		posrb = new Rainbow();
		posrb.usedToSelectHue = true;
		posrb.setCurrHue(posHue);
		posrb.setActionListener(this);
		p.add(posrb);
		dcsPanel.add(p);
		return dcsPanel;
	}

	/**
	* Reacts to user manipulations with color scale parameters
	*/
	@Override
	public void actionPerformed(ActionEvent evt) {
		if (evt.getSource() instanceof Rainbow) {
			if (evt.getSource() == negrb) {
				setNegHue(negrb.getCurrHue());
			} else {
				setPosHue(posrb.getCurrHue());
			}
		} else if (evt.getSource() == midPointTF) {
			String txt = midPointTF.getText();
			float mp = Float.NaN;
			if (txt != null) {
				try {
					mp = Float.valueOf(txt).floatValue();
				} catch (NumberFormatException nfe) {
				}
			}
			if (Float.isNaN(mp) || mp < minV || mp > maxV) {
				String str = StringUtil.floatToStr(midPoint, minV, maxV);
				midPointTF.setText(str);
				return;
			}
			setMidPoint(mp);
			midPointSl.setValue(mp);
		} else if (evt.getSource() == midPointSl) {
			float mp = (float) midPointSl.getValue();
			String str = StringUtil.floatToStr(mp, minV, maxV);
			midPointTF.setText(str);
			if (midPointSl.getIsDragging() && !dynamic)
				return;
			setMidPoint(mp);
		}
		notifyScaleChange();
	}

	/**
	 * Pass parameter string to setup color scale
	 */
	@Override
	public void setParameters(String par) {
		if (par == null || par == "")
			return;
		StringTokenizer st = new StringTokenizer(par, " ");
		if (st.countTokens() != 3)
			return;
		setNegHue(new Float(st.nextToken()).floatValue());
		setPosHue(new Float(st.nextToken()).floatValue());
		setMidPoint(new Float(st.nextToken()).floatValue());
		notifyScaleChange();
	}

	/**
	 * Pass parameter string to setup color scale
	 */
	@Override
	public String getParameters() {
		return Float.toString(getNegHue()) + " " + Float.toString(getPosHue()) + " " + Float.toString(getMidPoint());
	}
}
