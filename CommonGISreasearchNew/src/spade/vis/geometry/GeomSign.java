package spade.vis.geometry;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.StringTokenizer;
import java.util.Vector;

/**
* This is a class to implement geometric icons that may vary in shape, fill
* color, frame color, frame thickness, the character inside the icon, and
* the color of the character.
*/
public class GeomSign implements Diagram {
	/**
	* Possible shapes
	*/
	public static final int SQUARE = 0, CIRCLE = 1, DIAMOND = 2, UP_TRIANGLE = 3, DOWN_TRIANGLE = 4, LEFT_TRIANGLE = 5, RIGHT_TRIANGLE = 6, HORIZONTAL_BAR = 7, VERTICAL_BAR = 8, SHAPE_FIRST = 0, SHAPE_LAST = 8;
	/**
	* The same shapes encoded as strings
	*/
	public static final String shapeStrings[] = { "SQUARE", "CIRCLE", "DIAMOND", "UP_TRIANGLE", "DOWN_TRIANGLE", "LEFT_TRIANGLE", "RIGHT_TRIANGLE", "HORIZONTAL_BAR", "VERTICAL_BAR" };
	/**
	* Default size of the sign
	*/
	public static int defaultSize = 16;
	/**
	* The shape of this sign
	*/
	protected int shape = SHAPE_FIRST;
	/**
	* The frame thickness. If equals to 0, no frame is drawn.
	*/
	protected int frameTh = 1;
	/**
	* The character to be drawn inside the icon. For convenience, this is a
	* 1-character string. If equals to null, no character is drawn.
	*/
	protected String symbol = null;
	/**
	* The colors of the sign elements. If fillColor is null, the sign is not
	* filled.
	*/
	protected Color frameColor = Color.black, fillColor = Color.red, symbolColor = Color.black;
	/**
	* The width and height of the sign
	*/
	protected int width = defaultSize, height = defaultSize;
	/**
	* The position of the label
	*/
	protected int labelX = 0, labelY = 0;
	/**
	* The polygon used for drawing non-rectangular and non-round signs
	*/
	protected static int xp[] = null, yp[] = null;

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	/**
	* Returns the position of the label. This method should be called after draw(...).
	*/
	@Override
	public Point getLabelPosition() {
		return new Point(labelX, labelY);
	}

	public void setWidth(int w) {
		width = w;
	}

	public void setHeight(int h) {
		height = h;
	}

	public void setShape(int sh) {
		if (sh >= SHAPE_FIRST && sh <= SHAPE_LAST) {
			shape = sh;
		}
	}

	public int getShape() {
		return shape;
	}

	public void setFrameThickness(int th) {
		if (th >= 0 && th < width / 2) {
			frameTh = th;
		}
	}

	public int getFrameThickness() {
		return frameTh;
	}

	/**
	* Sets the character to be drawn inside the icon. For convenience, this must
	* be a 1-character string (longer strings are truncated). If equals to null,
	* no character is drawn.
	*/
	public void setSymbol(String s) {
		if (s == null) {
			symbol = null;
			return;
		}
		s = s.trim();
		if (s.length() < 1) {
			symbol = null;
			return;
		}
		if (s.length() > 1) {
			s = s.substring(0, 1);
		}
		symbol = s;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setFrameColor(Color color) {
		frameColor = color;
	}

	public void setFillColor(Color color) {
		fillColor = color;
	}

	public void setSymbolColor(Color color) {
		symbolColor = color;
	}

	public Color getFrameColor() {
		return frameColor;
	}

	public Color getFillColor() {
		return fillColor;
	}

	public Color getSymbolColor() {
		return symbolColor;
	}

	/**
	* This function is to be used for point objects.
	*/
	@Override
	public void draw(Graphics g, int x, int y) {
		if (g == null)
			return;
		int w = width, h = height, x0 = x - w / 2, y0 = y - h / 2;
		drawInFrame(g, x0, y0, w, h, w, h, shape, (frameTh > 0) ? frameColor : null, fillColor);
		labelX = x0;
		labelY = y0 + height + 2;
		if (frameTh > 1) {
			for (int i = 2; i <= frameTh; i++) {
				++x0;
				++y0;
				w -= 2;
				h -= 2;
				drawInFrame(g, x0, y0, w, h, width, height, shape, frameColor, null);
			}
		}
		//draw the symbol inside the sign
		if (symbol != null) {
			FontMetrics fm = g.getFontMetrics();
			int fh = fm.getHeight(), asc = fm.getAscent(), sw = fm.stringWidth(symbol);
			g.setColor(symbolColor);
			g.drawString(symbol, x - sw / 2, y - fh / 2 + asc);
		}
	}

	/**
	* A utility function: draws the sign inside the given frame. This allows
	* to draw frames of different thicknesses by means of looping, starting
	* from the normal symbol size and then decreasing the frame by one pixel
	* on each side. If frameColor is null, no frame is drawn. If fillColor is
	* null, the sign is not filled. Arguments signW and signH specify the
	* full size of the sign.
	*/
	protected static void drawInFrame(Graphics g, int x0, int y0, int w, int h, int signW, int signH, int shape, Color frameColor, Color fillColor) {
		//allocate memory for the polygon vertices, if needed
		if ((xp == null || yp == null) && (shape == DIAMOND || shape == UP_TRIANGLE || shape == DOWN_TRIANGLE || shape == LEFT_TRIANGLE || shape == RIGHT_TRIANGLE)) {
			xp = new int[5];
			yp = new int[5];
		}
		switch (shape) {
		case SQUARE:
			if (fillColor != null) {
				g.setColor(fillColor);
				g.fillRect(x0, y0, w + 1, h + 1);
			}
			if (frameColor != null) {
				g.setColor(frameColor);
				g.drawRect(x0, y0, w, h);
			}
			break;
		case HORIZONTAL_BAR: {
			int h1 = signH / 2, dy = (signH - h1) / 2;
			if (fillColor != null) {
				g.setColor(fillColor);
				g.fillRect(x0, y0 + dy, w + 1, h + 1 - dy * 2);
			}
			if (frameColor != null) {
				g.setColor(frameColor);
				g.drawRect(x0, y0 + dy, w, h - dy * 2);
			}
		}
			break;
		case VERTICAL_BAR: {
			int w1 = signW / 2, dx = (signW - w1) / 2;
			if (fillColor != null) {
				g.setColor(fillColor);
				g.fillRect(x0 + dx, y0, w + 1 - dx * 2, h + 1);
			}
			if (frameColor != null) {
				g.setColor(frameColor);
				g.drawRect(x0 + dx, y0, w - dx * 2, h);
			}
		}
			break;
		case CIRCLE:
			if (fillColor != null) {
				g.setColor(fillColor);
				g.fillOval(x0, y0, w + 1, h + 1);
			}
			if (frameColor != null) {
				g.setColor(frameColor);
				g.drawOval(x0, y0, w, h);
			}
			break;
		case DIAMOND:
			xp[0] = x0;
			yp[0] = y0 + h / 2;
			xp[1] = x0 + w / 2;
			yp[1] = y0;
			xp[2] = x0 + w;
			yp[2] = yp[0];
			xp[3] = xp[1];
			yp[3] = y0 + h;
			xp[4] = xp[0];
			yp[4] = yp[0];
			if (fillColor != null) {
				g.setColor(fillColor);
				g.fillPolygon(xp, yp, 4);
			}
			if (frameColor != null) {
				g.setColor(frameColor);
				g.drawPolygon(xp, yp, 5);
			}
			break;
		case UP_TRIANGLE:
			xp[0] = x0;
			yp[0] = y0 + h;
			xp[1] = x0 + w / 2;
			yp[1] = y0;
			xp[2] = x0 + w;
			yp[2] = yp[0];
			xp[3] = xp[0];
			yp[3] = yp[0];
			if (fillColor != null) {
				g.setColor(fillColor);
				g.fillPolygon(xp, yp, 3);
			}
			if (frameColor != null) {
				g.setColor(frameColor);
				g.drawPolygon(xp, yp, 4);
			}
			break;
		case DOWN_TRIANGLE:
			xp[0] = x0;
			yp[0] = y0;
			xp[1] = x0 + w / 2;
			yp[1] = y0 + h;
			xp[2] = x0 + w;
			yp[2] = yp[0];
			xp[3] = xp[0];
			yp[3] = yp[0];
			if (fillColor != null) {
				g.setColor(fillColor);
				g.fillPolygon(xp, yp, 3);
			}
			if (frameColor != null) {
				g.setColor(frameColor);
				g.drawPolygon(xp, yp, 4);
			}
			break;
		case LEFT_TRIANGLE:
			xp[0] = x0;
			yp[0] = y0 + h / 2;
			xp[1] = x0 + w;
			yp[1] = y0;
			xp[2] = xp[1];
			yp[2] = y0 + h;
			xp[3] = xp[0];
			yp[3] = yp[0];
			if (fillColor != null) {
				g.setColor(fillColor);
				g.fillPolygon(xp, yp, 3);
			}
			if (frameColor != null) {
				g.setColor(frameColor);
				g.drawPolygon(xp, yp, 4);
			}
			break;
		case RIGHT_TRIANGLE:
			xp[0] = x0;
			yp[0] = y0;
			xp[1] = x0 + w;
			yp[1] = y0 + h / 2;
			xp[2] = xp[0];
			yp[2] = y0 + h;
			xp[3] = xp[0];
			yp[3] = yp[0];
			if (fillColor != null) {
				g.setColor(fillColor);
				g.fillPolygon(xp, yp, 3);
			}
			if (frameColor != null) {
				g.setColor(frameColor);
				g.drawPolygon(xp, yp, 4);
			}
			break;
		}
	}

	/**
	* Draws the specified shape of default size at the given position (upper-left
	* corner of the icon). If frameColor is null, no frame is drawn. If fillColor
	* is null, the sign is not filled.
	*/
	protected static void drawShape(Graphics g, int x, int y, int shape, Color frameColor, Color fillColor) {
		drawInFrame(g, x, y, defaultSize, defaultSize, defaultSize, defaultSize, shape, frameColor, fillColor);
	}

	/**
	* This function is to be used for area objects.
	* The rectangle in which to fit the diagram is specified.
	*/
	@Override
	public void draw(Graphics g, Rectangle r) {
		if (r == null)
			return;
		draw(g, r.x, r.y, r.width, r.height);
	}

	/**
	* Same as previous, only the arguments are not wrapped into a Rectangle object
	*/
	@Override
	public void draw(Graphics g, int x, int y, int w, int h) {
		draw(g, x + w / 2, y + h / 2);
	}

	/**
	* Replies whether this diagram is centered, i.e. the center of the diagram
	* coinsides with the center of the object
	*/
	@Override
	public boolean isCentered() {
		return true;
	}

	/**
	* Generates a collection (Vector) of signs to represent attribute values.
	* The input parameter is an array of the length equal to the number of
	* attributes, in which for each attribute the number of different values
	* is specified.
	* The priorities for assignment of sign elements to attribute values is the
	* following:
	* shape, fill color, frame thickness, symbol, frame color, symbol color
	*/
	public static Vector generateSigns(int nval[]) {
		if (nval == null)
			return null;
		//indicates which of the sign parameters have been already used
		boolean used[] = { false, false, false, false, false, false };
		//the number of different values that can be represented by each sign
		//element
		int ndif[] = { SHAPE_LAST + 1, 999, 6, 35, 999, 999 };
		//the number of attributes to be considered; if more than 6, consider only 6
		int nattr = nval.length;
		if (nattr > 6) {
			nattr = 6;
		}
		//for each attribute stores the index of the sign element that represents it
		int elemN[] = new int[nattr];
		//assign to each attribute the sign element to represent it
		for (int i = 0; i < nattr; i++) {
			elemN[i] = -1;
			for (int j = 0; j < used.length && elemN[i] < 0; j++)
				if (!used[j] && ndif[j] >= nval[i]) {
					elemN[i] = j;
					used[j] = true;
				}
			if (elemN[i] < 0) {
				int maxDif = 0, idxMax = -1;
				for (int j = 0; j < used.length; j++)
					if (!used[j] && ndif[j] > maxDif) {
						maxDif = ndif[j];
						idxMax = j;
					}
				elemN[i] = idxMax;
				used[idxMax] = true;
			}
		}
		int nsigns = 1;
		for (int i = 0; i < nattr; i++)
			if (nval[i] > 0) {
				nsigns *= nval[i];
			}
		Vector signs = new Vector(nsigns, 5);
		for (int i = 0; i < nsigns; i++) {
			GeomSign sign = new GeomSign();
			int num = i;
			for (int j = nattr - 1; j >= 0; j--)
				if (nval[j] > 0) {
					int valIdx = num % nval[j];
					if (ndif[elemN[j]] <= valIdx) {
						valIdx = valIdx % ndif[elemN[j]];
					}
					if (elemN[j] == 0) {
						sign.setShape(valIdx);
					} else if (elemN[j] == 1) {
						sign.setFillColor(Color.getHSBColor(1.0f * valIdx / nval[j], 0.5f, 1.0f));
					} else if (elemN[j] == 2) {
						sign.setFrameThickness(valIdx);
					} else if (elemN[j] == 3) {
						if (valIdx < 9) {
							sign.setSymbol(String.valueOf(valIdx + 1));
						} else {
							String s = "!";
							sign.setSymbol(s.replace('!', (char) ('a' + (valIdx - 9))));
						}
					} else if (elemN[j] == 4) {
						sign.setFrameColor(Color.getHSBColor(1.0f * valIdx / nval[j], 1.0f, 0.7f));
					} else if (elemN[j] == 4) {
						sign.setSymbolColor(Color.getHSBColor(1.0f * valIdx / nval[j], 1.0f, 0.7f));
					}
					num = num / nval[j];
				}
			signs.addElement(sign);
		}
		return signs;
	}

	/**
	* Used in toString() and valueOf()
	*/
	protected static String key = "_GeomSign_:";

	public static String getKey() {
		return key;
	}

	/**
	* Generate a string representation of the sign, suitable, in particular,
	* for storing in a file
	*/
	@Override
	public String toString() {
		String str = key + shapeStrings[shape] + ";" + frameTh + ";";
		if (symbol == null) {
			str += "null;";
		} else {
			str += symbol + ";";
		}
		if (fillColor == null) {
			str += "null;";
		} else {
			str += "(" + fillColor.getRed() + "," + fillColor.getGreen() + "," + fillColor.getBlue() + ");";
		}
		if (frameColor == null) {
			str += "null;";
		} else {
			str += "(" + frameColor.getRed() + "," + frameColor.getGreen() + "," + frameColor.getBlue() + ");";
		}
		if (symbolColor == null) {
			str += "null;";
		} else {
			str += "(" + symbolColor.getRed() + "," + symbolColor.getGreen() + "," + symbolColor.getBlue() + ")";
		}
		return str;
	}

	/**
	* Constructs a sign from the given string representation
	*/
	public static GeomSign valueOf(String str) {
		if (str == null || !str.startsWith(key))
			return null;
		str = str.substring(key.length());
		StringTokenizer st = new StringTokenizer(str, ";");
		GeomSign sign = new GeomSign();
		for (int i = 0; i < 6 && st.hasMoreTokens(); i++) {
			String tok = st.nextToken().trim();
			if (i == 0) {
				for (int j = 0; j < shapeStrings.length; j++)
					if (tok.equalsIgnoreCase(shapeStrings[j])) {
						sign.setShape(j);
						break;
					}
			} else if (i == 1) {
				try {
					int t = Integer.valueOf(tok).intValue();
					sign.setFrameThickness(t);
				} catch (NumberFormatException e) {
				}
			} else if (i == 2)
				if (tok.equalsIgnoreCase("null")) {
					sign.setSymbol(null);
				} else {
					sign.setSymbol(tok);
				}
			else { //colors
				Color color = null;
				if (!tok.equalsIgnoreCase("null")) {
					int rgb[] = { 0, 0, 0 };
					StringTokenizer st1 = new StringTokenizer(tok, "(,)");
					for (int j = 0; j < 3 && st1.hasMoreTokens(); j++) {
						try {
							rgb[j] = Integer.valueOf(st1.nextToken()).intValue();
						} catch (NumberFormatException e) {
						}
					}
					color = new Color(rgb[0], rgb[1], rgb[2]);
				}
				if (i == 3) {
					sign.setFillColor(color);
				} else if (i == 4) {
					sign.setFrameColor(color);
				} else {
					sign.setSymbolColor(color);
				}
			}
		}
		return sign;
	}

	public Image getImage(Component c) {
		if (c == null)
			return null;
		Image image = c.createImage(width + 1, height + 1);
		if (image == null)
			return null;
		Graphics g = image.getGraphics();
		if (g == null)
			return null;
		draw(g, 0, 0, width, height);
		g.dispose();
		return image;
	}
}