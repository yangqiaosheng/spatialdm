package spade.analysis.plot.bargraph;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

import spade.lib.basicwin.Metrics;
import spade.lib.util.StringUtil;

/** Represents the graph used in {@link
 * propraktikum03.vis.components.Correlation2dComponent
 * Correlation2dComponent}.
 * <P>
 * It is meant to visualize a given number of correlation coefficients. See
 * {@link DistrictOverviewGraph DistrictOverviewGraph} for a related code
 * example.
 *
 * @author Mario Boley
 * @version 1.1
 */
public class CorrelationBarGraph extends HorizontalBarGraph implements MouseListener {

	/** Class representing a single bar in a CorrelationBarGraph.
	 *
	 */
	class CorrelationBar extends Bar {
		/** X-Coordinate of the value-String.
		 */
		int absValX;
		/** Y-Coordinate of the value-String.
		 */
		int absValY;

		/** Draws the value-String.
		 *
		 * @param g the Graphics context on which the CorrelationBarGraph is to be
		 *     drawn
		 */
		void drawAbsValue(Graphics g) {
			if (contains(absValX, absValY)) {
				g.setColor(textInsideBarColor);
			} else {
				g.setColor(textOutsideBarColor);
			}
			String str = (borderValue == 1f) ? Float.toString(value) : StringUtil.floatToStr(value, 0, borderValue);
			g.drawString(str, absValX, absValY);
		}

		/** Return wether the given coordinates are inside the bars description or not
		 *
		 * @param x
		 * @param y
		 * @return
		 */
		boolean containsDescription(int x, int y) {
			return (((x >= descriptionX) && (x <= descriptionX + Metrics.stringWidth(description))) && ((y <= descriptionY) && (y >= descriptionY - Metrics.getFontMetrics().getHeight())));

		}

		/** Draws the bar.
		 *
		 * @param g the Graphics context on which the CorrelationBarGraph is to be
		 *     drawn
		 */
		@Override
		void drawBar(Graphics g) {
			if ((value >= 0) || drawSigned) {
				g.setColor(barColor);
			} else {
				g.setColor(negativeBarColor);
			}
			g.fillRect(x, y, width, height);
			g.setColor(baselineColor);
			g.drawRect(x, y, width, height);
		}

		/** Calls super.draw(g) and then draws the value-String.
		 *
		 * @param g the Graphics context on which the CorrelationBarGraph is to be
		 *     drawn
		 */
		@Override
		void draw(Graphics g) {
			super.draw(g);
			drawAbsValue(g);
		}

	}

	/** The maximum width of all bar-descritions.
	 */
	protected int maxDescriptionWidth = 0;

	/** If true, bars are drawn singed - if false, bars are drawn for absolute values.
	 */
	protected boolean drawSigned = true;
	/** The number of the bar the compareBar is attached to.
	 * Is -1 if no bar is selected.
	 */
	protected int selected = -1;

	/** The Color of Strings drawn inside a bar.
	 */
	protected Color textInsideBarColor = Color.white;
	/** The Color negative bars are painted in absolute-mode.
	 */
	protected Color negativeBarColor = Color.red;
	/** The Color in which the compareBar is painted.
	 */
	protected Color compareBarColor = Color.white;
	/** A list of the registered ActionListener
	 *
	 */
	protected Vector actionListener;

	/** Sets fields to standart values.
	 * <P>
	 * <UL>
	 * <LI> {@link #textInsideBarColor textInsideBarColor} to white
	 * <LI> {@link #negativeBarColor negativeBarColor} to red
	 * <LI> {@link #compareBarColor compareBarColor} to white
	 * <LI> {@link #borderValue borderValue} to 1f
	 * </UL>
	 */
	public CorrelationBarGraph() {
		super();
		textInsideBarColor = Color.white;
		negativeBarColor = Color.red;
		compareBarColor = Color.white;
		borderValue = 1f;
		actionListener = new Vector();
	}

	public void addActionListener(ActionListener listener) {
		System.out.println("listener added");
		actionListener.addElement(listener);
	}

	/** Calculates the position of a Float-value at current screen-proportions.
	 *
	 * @param val the Float-value to which the screen-position is to be calculated
	 * @return the X-Coordinate associated to the value
	 */
	protected int calcPixelPos(float val) {
		return zeroLineLoc + Math.round(maxLength / borderValue * val);
	}

	/** Sets the field {@link #textInsideBarColor textInsideBarColor}.
	 *
	 * @param c Color for the Strings, which are displayed inside a bar
	 */
	public void setTextInsideBarColor(Color c) {
		if (c == null)
			return;
		textInsideBarColor = c;
	}

	/** Sets the field {@link #negativeBarColor negativeBarColor}.
	 *
	 * @param c Color for the negative bars when drawSinged is not set
	 */
	public void setNegativeBarColor(Color c) {
		if (c == null)
			return;
		negativeBarColor = c;
	}

	/** Sets the field {@link #compareBarColor compareBarColor}.
	 *
	 * @param c Color for the compareBar
	 */
	public void setCompareBarColor(Color c) {
		if (c == null)
			return;
		compareBarColor = c;
	}

	/** Sets if the bars are displayed with their absolute-values or signed.
	 *
	 * @param b true for drawSinged - false for absolute-mode
	 */
	public void setDrawSigned(boolean b) {
		if (b != drawSigned) {
			drawSigned = b;
			calcBoundsDependentData();
		}
	}

	@Override
	public void setValues(Vector v) {
		if (v == null)
			return;
		if (bars == null || bars.length != v.size()) {
			bars = new CorrelationBar[v.size()];
		}
		for (int i = 0; i < v.size(); i++) {
			if (bars[i] == null) {
				bars[i] = new CorrelationBar();
			}
			bars[i].value = ((Float) v.elementAt(i)).floatValue();
		}
	}

	@Override
	public void setDescriptions(Vector v) {
		if (v != null && v.size() > 0 && v.elementAt(0) instanceof String) {
			maxDescriptionWidth = Metrics.stringWidth((String) v.elementAt(0));
		} else
			return;
		for (int i = 0; i < Math.min(v.size(), bars.length); i++) {
			if (v.elementAt(i) instanceof String) {
				bars[i].description = (String) v.elementAt(i);
				if (Metrics.stringWidth((String) v.elementAt(i)) > maxDescriptionWidth) {
					maxDescriptionWidth = Metrics.stringWidth((String) v.elementAt(i));
				}
			}
		}
	}

	/** Sets the canvas and adds this instance to it as mouseListener.
	 *
	 * @param c Canvas to be attached to this CorrelationBarGraph
	 */
	@Override
	public void setCanvas(Canvas c) {
		if (c == null)
			return;
		canvas = c;
		canvas.addMouseListener(this);
	}

	/** Removes itself as mouseListener.
	 */
	@Override
	public void destroy() {
		canvas.removeMouseListener(this);
		destroyed = true;
	}

	@Override
	protected void calcBoundsDependentData() {
		if (bounds == null || bars.length == 0)
			return;
		if (drawSigned) {
			super.calcBoundsDependentData();
		} else {
			Math.round(spaceWidth = Math.min(maxSpaceWidth, Math.max(minSpaceWidth, (Math.round((bounds.height - topMargin - bottomMargin) / bars.length) * spacePercentage) / 100)));
			barWidth = Math.min(maxBarWidth, Math.max(minBarWidth, Math.round((bounds.height - topMargin - bottomMargin) / bars.length) - spaceWidth));

			zeroLineLoc = bounds.x + leftMargin + maxDescriptionWidth;
			maxLength = bounds.width - zeroLineLoc - rightMargin;
		}
	}

	@Override
	public void setup() {
		if (bars == null || canvas == null || bounds == null)
			return;
		for (int i = 0; i < bars.length; i++) {

			int length = Math.round(maxLength / borderValue * (drawSigned ? bars[i].value : Math.abs(bars[i].value)));
			if (length >= 0) {
				bars[i].x = zeroLineLoc;
			} else {
				bars[i].x = zeroLineLoc + length;
			}
			bars[i].y = bounds.y + topMargin + i * (barWidth + spaceWidth);
			bars[i].width = Math.abs(length);
			bars[i].height = barWidth;
			if (length >= 0) {
				bars[i].descriptionX = zeroLineLoc - 2 - Metrics.stringWidth(bars[i].description);
			} else {
				bars[i].descriptionX = zeroLineLoc + 2;
			}
			bars[i].descriptionY = bars[i].y + Math.round(bars[i].height / 2) + Math.round(canvas.getGraphics().getFontMetrics().getHeight() / 3);

			((CorrelationBar) bars[i]).absValY = bars[i].descriptionY;
			String str = (borderValue == 1f) ? Float.toString(bars[i].value) : StringUtil.floatToStr(bars[i].value, 0, borderValue);

			if ((drawSigned ? bars[i].value : Math.abs(bars[i].value)) < 0) {
				if (canvas.getGraphics().getFontMetrics().stringWidth(str) > bars[i].width) {
					((CorrelationBar) bars[i]).absValX = bars[i].x - canvas.getGraphics().getFontMetrics().stringWidth(str);
				} else {
					((CorrelationBar) bars[i]).absValX = bars[i].x;
				}
			} else {
				if (canvas.getGraphics().getFontMetrics().stringWidth(str) < bars[i].width) {
					((CorrelationBar) bars[i]).absValX = bars[i].x + bars[i].width - canvas.getGraphics().getFontMetrics().stringWidth(str);
				} else {
					((CorrelationBar) bars[i]).absValX = bars[i].x + bars[i].width + 2;
				}
			}
		}
	}

	/** Draws two to four lines in addition to those drawn by super.drawBackground.
	 * One line is drawn at 0.5f and another one at 1.0f. If drawSinged is set it draws also lines at -0.5f and -1.0f. All lines are drawn with a Color between {@link #baseLineColor baseLineColor} and {@link #backGroundColor backGroundColor}. The inner lines are slightly brighter.
	 *
	 * @param g the Graphics context on which the CorrelationBarGraph is to be
	 *     drawn
	 */
	@Override
	protected void drawBackground(Graphics g) {
		super.drawBackground(g);
		if (bounds == null)
			return;
		g.setColor(new Color(baselineColor.getRGB() - backgroundColor.getRGB()));
		g.drawLine(calcPixelPos(1.0f), bounds.y + topMargin, calcPixelPos(1.0f), bounds.y + bounds.height - bottomMargin);
		if (drawSigned) {
			g.drawLine(calcPixelPos(-1.0f), bounds.y + topMargin, calcPixelPos(-1.0f), bounds.y + bounds.height - bottomMargin);
		}
		g.setColor(new Color(baselineColor.getRGB() - backgroundColor.getRGB()).brighter());
		if (drawSigned) {
			g.drawLine(calcPixelPos(-0.5f), bounds.y + topMargin, calcPixelPos(-0.5f), bounds.y + bounds.height - bottomMargin);
		}
		g.drawLine(calcPixelPos(0.5f), bounds.y + topMargin, calcPixelPos(0.5f), bounds.y + bounds.height - bottomMargin);

	}

	/** Draws the compare if required in addition to what super.draw() draws.
	 *
	 * @param g the Graphics context on which the CorrelationBarGraph is to be
	 *     drawn
	 */
	@Override
	public void draw(Graphics g) {
		if (g == null)
			return;
		super.draw(g);
		if (bars == null)
			return;

		if (selected >= 0 && selected < bars.length) {
			g.setColor(compareBarColor);
			if (bars[selected].value < 0 && drawSigned) {
				g.drawLine(bars[selected].x, bounds.y + topMargin, bars[selected].x, bounds.y + bounds.height - bottomMargin);
			}
			if (bars[selected].value > 0 || !drawSigned) {
				g.drawLine(bars[selected].x + bars[selected].width, bounds.y + topMargin, bars[selected].x + bars[selected].width, bounds.y + bounds.height - bottomMargin);
			}
		} else {
			selected = -1;
		}

	}

	@Override
	public java.awt.Dimension getPreferredSize() {
		this.calcBoundsDependentData();
		if (bars == null || drawSigned)
			return super.getPreferredSize();
		return new Dimension(minBarLength + maxDescriptionWidth + leftMargin + rightMargin, topMargin + bottomMargin + bars.length * (minBarWidth + minSpaceWidth));
	}

	/** Does nothing.
	 * Is required by MouseListener.
	 *
	 * @param e MouseEvent
	 */
	@Override
	public void mouseClicked(MouseEvent e) {
	}

	/** Returns the description-string that was clicked on.
	 * If no description was clicked: Checks if click occured in a Bar.
	 * If so sets the compareBar to this bar or disables compareBar in case
	 * compareBar was already associated to the bar.
	 *
	 * @param e MouseEvent passed from the canvas
	 */
	@Override
	public void mousePressed(MouseEvent e) {
		for (Bar bar : bars) {
			if (((CorrelationBar) bar).containsDescription(e.getX(), e.getY())) {
				for (int j = 0; j < actionListener.size(); j++) {
					((ActionListener) actionListener.elementAt(j)).actionPerformed(new ActionEvent(this, -1, bar.description));
				}
				return;
			}
		}

		int newSelected = -1;
		for (int i = 0; i < bars.length; i++) {
			if (((CorrelationBar) bars[i]).contains(e.getX(), e.getY())) {
				newSelected = i;
			}
		}
		if (newSelected < 0)
			return;
		else {
			if (selected == newSelected) {
				selected = -1;
			} else {
				selected = newSelected;
			}
			draw(canvas.getGraphics());
		}
	}

	/** Does nothing.
	 * Is required by MouseListener.
	 *
	 * @param e MouseEvent
	 */
	@Override
	public void mouseReleased(MouseEvent e) {
	}

	/** Does nothing.
	 * Is required by MouseListener.
	 *
	 * @param e MouseEvent
	 */
	@Override
	public void mouseEntered(MouseEvent e) {
	}

	/** Does nothing.
	 * Is required by MouseListener.
	 *
	 * @param e MouseEvent
	 */
	@Override
	public void mouseExited(MouseEvent e) {
	}

}
