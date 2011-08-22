package spade.time.vis;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Hashtable;

import spade.vis.geometry.Sign;

/**
* Represents the value flow of a numeric attribute by a polygon built from a
* time-series graph. Such polygons are put on the map as diagrams.
*/
public class ValueFlowSign extends Sign {
	/**
	* The minimum and maximum attribute values to be represented
	*/
	protected double min = Double.NaN, max = Double.NaN;
	/**
	* The array of the attribute values to be represented
	*/
	protected double values[] = null;
	/**
	* The array of transformed values used for drawing of the polygon
	*/
	protected int iv[] = null;
	/**
	* Used for polygon drawing
	*/
	protected int xp[] = null, yp[] = null;
	/**
	* The colors for the positive and negative values
	*/
	protected Color posColor = Color.orange, negColor = Color.blue;

	/**
	* The constructor is used to set the default sizes and sign properties that
	* can be modified
	*/
	public ValueFlowSign() {
		setMayChangeProperty(MAX_SIZE, true);
		setMayChangeProperty(USE_FRAME, true);
		maxW = 15 * mm;
		maxH = 10 * mm;
		usesFrame = false;
	}

	/**
	* Sets the minimum and maximum attribute values to be represented
	*/
	public void setMinMax(double minVal, double maxVal) {
		min = minVal;
		max = maxVal;
	}

	/**
	* Sets the array of attribute values to be represented
	*/
	public void setValues(double values[]) {
		this.values = values;
	}

	/**
	* Returns the color used for showing positive values
	*/
	public Color getPositiveColor() {
		return posColor;
	}

	/**
	* Sets the color to be used for showing positive values
	*/
	public void setPositiveColor(Color color) {
		posColor = color;
	}

	/**
	* Returns the color used for showing negative values
	*/
	public Color getNegativeColor() {
		return negColor;
	}

	/**
	* Sets the color to be used for showing negative values
	*/
	public void setNegativeColor(Color color) {
		negColor = color;
	}

	/**
	* This function is to be used for point objects.
	*/
	@Override
	public void draw(Graphics g, int x, int y) {
		labelX = x;
		labelY = y;
		if (values == null || Double.isNaN(min) || Double.isNaN(max))
			return;
		if (iv == null || iv.length != values.length) {
			iv = new int[values.length];
		}
		int np = values.length; //number of points
		double maxMod = Math.abs(max);
		if (min < 0 && maxMod < -min) {
			maxMod = -min;
		}
		boolean hasValues = false;
		int imax = 0, imin = 0;
		for (int i = 0; i < np; i++)
			if (Double.isNaN(values[i])) {
				iv[i] = Integer.MIN_VALUE;
			} else {
				hasValues = true;
				if (values[i] >= maxMod) {
					iv[i] = maxH;
				} else if (values[i] <= -maxMod) {
					iv[i] = -maxH;
				} else {
					iv[i] = (int) Math.round(values[i] * maxH / maxMod);
				}
				if (iv[i] < imin) {
					imin = iv[i];
				} else if (iv[i] > imax) {
					imax = iv[i];
				}
			}
		if (!hasValues)
			return;
		if (imin < 0) {
			labelY = y - imin;
		}
		if (usesFrame) {
			g.setColor(Color.gray);
			if (imax > 0) {
				int h = (int) Math.round(1.0f * max * maxH / maxMod);
				g.drawRect(x, y - h, maxW, h);
			}
			if (imin < 0) {
				int h = (int) Math.round(-1.0f * min * maxH / maxMod);
				g.drawRect(x, y, maxW, h);
				labelY = y + h;
			}
		}
		if (xp == null || xp.length < iv.length + 3) {
			xp = new int[iv.length + 3];
		}
		if (yp == null || yp.length < iv.length + 3) {
			yp = new int[iv.length + 3];
		}
		g.setColor(Color.black);
		g.drawOval(x - 1, y - 1, 2, 2);
		yp[0] = (iv[0] == Integer.MIN_VALUE) ? y : y - iv[0];
		xp[0] = x;
		int n = 1;
		int sign = (iv[0] > 0) ? 1 : (iv[0] < 0) ? -1 : 0;
		int maxWOld = maxW;
		if (np > maxW) {
			maxW = np; //to avoid zero length line segments
		}
		for (int i = 1; i < np; i++) {
			int xpos = x + Math.round(1.0f * i * maxW / (np - 1)), ypos = (iv[i] == Integer.MIN_VALUE) ? y : y - iv[i];
			boolean sameSign = false;
			if (iv[i] == Integer.MIN_VALUE) {
				sameSign = iv[i - 1] == Integer.MIN_VALUE;
			} else if (iv[i - 1] != Integer.MIN_VALUE) {
				sameSign = (sign >= 0 && iv[i] >= 0) || (sign <= 0 && iv[i] <= 0);
			}
			if (sameSign) {
				if (iv[i] != 0) {
					sign = (iv[i] > 0) ? 1 : -1;
				}
			} else {
				if (iv[i - 1] == Integer.MIN_VALUE) {
					g.setColor(Color.white);
					int start = xp[0] - Math.round(1.0f * maxW / (np - 1)) + 1;
					if (start < x) {
						start = x;
					}
					for (int j = start; j <= xpos; j += 3) {
						g.drawLine(j, y, j + 1, y);
					}
				} else {
					xp[n] = (iv[i] != Integer.MIN_VALUE) ? (xp[n - 1] + xpos) / 2 : xp[n - 1];
					yp[n] = y;
					xp[n + 1] = xp[0];
					yp[n + 1] = y;
					int npp = n + 2;
					if (yp[0] != y) {
						xp[n + 2] = xp[0];
						yp[n + 2] = yp[0];
						++npp;
					}
					g.setColor((sign >= 0) ? posColor : negColor);
					g.setColor(java2d.Drawing2D.getTransparentColor(g.getColor(), transparency));
					g.fillPolygon(xp, yp, npp - 1);
					g.setColor(Color.black);
					g.drawPolygon(xp, yp, npp);
				}
				if (iv[i] != Integer.MIN_VALUE && iv[i - 1] != Integer.MIN_VALUE) {
					xp[0] = xp[n];
					yp[0] = yp[n];
					n = 1;
				} else {
					n = 0;
				}
				sign = (iv[i] > 0) ? 1 : (iv[i] < 0) ? -1 : 0;
			}
			xp[n] = xpos;
			yp[n] = ypos;
			++n;
		}
		if (iv[np - 1] == Integer.MIN_VALUE) {
			g.setColor(Color.white);
			int start = xp[0] - Math.round(1.0f * maxW / (np - 1)) + 1;
			if (start < x) {
				start = x;
			}
			for (int j = start; j <= x + maxW; j += 3) {
				g.drawLine(j, y, j + 1, y);
			}
		} else {
			xp[n] = xp[n - 1];
			yp[n] = y;
			xp[n + 1] = xp[0];
			yp[n + 1] = y;
			int npp = n + 2;
			if (yp[0] != y) {
				xp[n + 2] = xp[0];
				yp[n + 2] = yp[0];
				++npp;
			}
			g.setColor((sign >= 0) ? posColor : negColor);
			g.setColor(java2d.Drawing2D.getTransparentColor(g.getColor(), transparency));
			g.fillPolygon(xp, yp, npp - 1);
			g.setColor(Color.black);
			g.drawPolygon(xp, yp, npp);
		}
		maxW = maxWOld;
	}

	/**
	* This function is to be used for area objects.
	* The rectangle in which to fit the diagram is specified.
	*/
	@Override
	public void draw(Graphics g, Rectangle r) {
		draw(g, r.x, r.y, r.width, r.height);
	}

	/**
	* Same as previous, only the arguments are not wrapped into a Rectangle object
	*/
	@Override
	public void draw(Graphics g, int x, int y, int w, int h) {
		if (w >= maxW) {
			draw(g, x + w / 2 - maxW / 2, y + h / 2);
		} else {
			draw(g, x + w / 2, y + h / 2);
		}
	}

	/**
	* Returns properties of this sign.
	*/
	public Hashtable getProperties() {
		Hashtable prop = new Hashtable();
		String str = Integer.toHexString(posColor.getRGB()).substring(2);
		prop.put("positive_color", str);
		str = Integer.toHexString(negColor.getRGB()).substring(2);
		prop.put("negative_color", str);
		prop.put("max_width", String.valueOf(maxW));
		prop.put("max_height", String.valueOf(maxH));
		prop.put("use_frame", String.valueOf(usesFrame));
		return prop;
	}

	/**
	* Sets sign properties according to the given specification.
	*/
	public void setProperties(Hashtable prop) {
		if (prop == null)
			return;
		String str = (String) prop.get("positive_color");
		if (str != null) {
			try {
				int n = Integer.parseInt(str, 16);
				posColor = new Color(n);
			} catch (NumberFormatException e) {
			}
		}
		str = (String) prop.get("negative_color");
		if (str != null) {
			try {
				int n = Integer.parseInt(str, 16);
				negColor = new Color(n);
			} catch (NumberFormatException e) {
			}
		}
		str = (String) prop.get("max_width");
		if (str != null) {
			try {
				int n = Integer.parseInt(str);
				if (n > 0) {
					maxW = n;
				}
			} catch (NumberFormatException e) {
			}
		}
		str = (String) prop.get("max_height");
		if (str != null) {
			try {
				int n = Integer.parseInt(str);
				if (n > 0) {
					maxH = n;
				}
			} catch (NumberFormatException e) {
			}
		}
		str = (String) prop.get("use_frame");
		if (str != null) {
			usesFrame = str.equalsIgnoreCase("true");
		}
	}
}
