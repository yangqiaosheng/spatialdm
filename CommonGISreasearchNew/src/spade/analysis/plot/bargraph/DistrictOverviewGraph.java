package spade.analysis.plot.bargraph;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.HotSpot;
import spade.lib.basicwin.Metrics;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;

/** Represents the graph used in DistOverviewComponent.
 * <p>
 * It is meant to display the value of one district (or average many selected
 * districts) in addition to the statistical values of mean, median and the
 * absolute maximum an minimum value of an attribute on the entire map.
 * <p>
 * Example code for setting up a DistrictOverviewBarGraph on a PlotCanvas.
 * Suppositional desvec, medvec, avevec, maxvec and minvec are Vectors
 * containing the descriptions, the medians, the averages, the maximum and the
 * minimum of a given number of attributes. For those the current seleceted
 * (highlighted) district has stored its values in valvec.
 * <pre>
 * DisttrictOverviewBarGraph barGraph=new DistrictOverviewBarGraph();
 *
 * barGraph.setValues(valvec);
 * barGraph.setDescriptions(desvec);
 * barGraph.setMedians(medvec);
 * barGraph.setAverages(avevec);
 * barBraph.setMaximums(maxvec);
 * barGraph.setMinimums(minvec);
 *
 * //Note that setValues has to be called before set-methods, because just
 * //setValues changes bounds of the internal bar-array and creates new
 * //bar-objects.
 *
 * barGraph.setMargin(5);
 * barGraph.setSpace(0,0,0);
 * barGraph.setMinBarWidth(Metrics.getFontMetrics().getHeight());
 * barGraph.setMinSpaceWidth(0);
 *
 * // Assume the graph is to be attached to a panel via a PlotCanvas gCanvas
 * barGraph.setCanvas(gCanvas);
 * gCanvas.setContent(barGraph);
 * barGraph.setup();
 * barGraph.draw(gCanvas.getGraphics());
 *
 * // Now suppose a new district is selected - again values are stored in valvec
 * barGraph.setValues(valvec);
 * barGraph.draw(gCanvas.getGraphics());
 * // setup has not to be called again, because setValues does this already
 * // the values, when updating
 *
 * </pre>
 *
 * @author Mario Boley
 */
public class DistrictOverviewGraph extends VerticalBarGraph implements MouseMotionListener {

	/** Class representing a single bar in a {@link DistrictOverviewBarGraph
	 *  DistrictOverviewBarGraph}.
	 *  In addition to the members of its ancestor it also contains information
	 *  about median, average, maximums and minimums. These are represented
	 *  graphically as a red and a blue crossline respective as the hull of the bar.
	 *
	 */
	class DistrictOverviewBar extends Bar {

		/** Value of the represented attributes median.
		 */
		float median, average, max, min;

		/** Hotspot displaying the value, minimum, maximum, mean and median of the bar's attribute
		 *
		 */
		HotSpot hotSpot;

		/** The full description of the attribute for the popup
		 *
		 */
		String fullDescription;

		/** Y-coordinate of the bar's frame.
		 */
		int frameY;
		/** Heigth of the bar's frame.
		 */
		int frameHeight;
		/** Y-coordinate of the bar's average-line.
		 */
		int averageY;
		/** Y-coordinate of the bar's median-line.
		 */
		int medianY;

		/** Draws the frame.
		 *
		 * @param g the Graphics context on which the bar is to be painted
		 */
		void drawFrame(Graphics g) {
			g.setColor(frameColor);
			g.drawRect(x, frameY, width, frameHeight);
		}

		/** Draws the median-line.
		 *
		 * @param g the Graphics context on which the bar is to be painted
		 */
		void drawMedian(Graphics g) {
			g.setColor(medianColor);
			g.drawLine(x - 2, medianY, (averageY == medianY) ? x + Math.round(width / 2) : x + width + 2, medianY);
		}

		/** Draws the average-line.
		 *
		 * @param g the Graphics context on which the bar is to be painted
		 */
		void drawAverage(Graphics g) {
			g.setColor(averageColor);
			g.drawLine(x - 2, averageY, x + width + 2, averageY);
		}

		/** Removes the bar from screen.
		 *
		 * @param g the Graphics context of the bars location.
		 */
		void delete(Graphics g) {
			g.setColor(backgroundColor);
			g.fillRect(x, y, width, height);
		}

		/** Draws the whole bar.
		 *
		 * @param g the Graphics context on which the bar is to be painted
		 */
		@Override
		void draw(Graphics g) {
			super.draw(g);
			drawFrame(g);
			drawAverage(g);
			drawMedian(g);
		}

		@Override
		boolean contains(int ax, int ay) {
			return ((ax >= x && ax <= x + width) && (ay >= frameY && ay <= frameY + frameHeight));
		}
	}

	static ResourceBundle res = Language.getTextResource("spade.analysis.plot.Res");

	/** stores number of bar the cursor is in or -1 if cursor outside all bars
	*/
	protected int cursorIn = -1;

	/** list of all listeners which receive ActionEvents
	*/
	protected Vector actionListener;

	/** indicates the existance of a positve maximum
	*/
	protected boolean positiveMaximum = false;
	/** indicates the existance of a negative minimum
	*/
	protected boolean negativeMinimum = false;

	/** Color used for the bars frames (representing maximum and minimum).
	*/
	protected Color frameColor = Color.darkGray, medianColor = Color.red, averageColor = Color.blue;

	/**
	 * <p>
	 * <UL>
	 * <LI> {@link #barColor barColor} to Color(0.45f, 0.45f, 0.45f)
	 * <LI> {@link #textOutsideBarColor textOutsideBarColor} to white
	 * </UL>
	 */
	public DistrictOverviewGraph() {
		barColor = new Color(0.45f, 0.45f, 0.45f);
		textOutsideBarColor = Color.white;
		actionListener = new Vector();
	}

	/** Adds a new ActionListener to receive an ActionEvent containing number of the bar the mousecursor
	* currently is in or -1.
	*
	* @param listener the new ActionListener to be added
	*/
	public void addActionListener(ActionListener listener) {
		if (listener == null)
			return;
		actionListener.addElement(listener);
	}

	/** Removes an ActionListener from the list.
	*
	* @param listener ActionListener to be removed
	*/
	public void removeActionListener(ActionListener listener) {
		actionListener.removeElement(listener);
	}

	/** Sets the values of the bars (attribute values of the selected districts).
	 * Creates a new array bars and creates a corresponding amount of
	 * DistrictOverviewBar-Objects if the old one does not fit the number of Floats
	 * delivered. Note that the other set-methods neither change array-bounds nor
	 * create objects. So when passing a complete new set of data to a
	 * DistrictOverviewBarGraph, this method has to be called always first.
	 *
	 * @param v a vector of Float-objects containing the values of the bars
	 */
	@Override
	public void setValues(Vector v) {
		if (v == null)
			return;
		boolean update = true;
		if (bars == null || bars.length != v.size()) {
			bars = new DistrictOverviewBar[v.size()];
			update = false;
		}
		for (int i = 0; i < v.size(); i++) {
			if (bars[i] == null) {
				bars[i] = new DistrictOverviewBar();
			}
			bars[i].value = ((Float) v.elementAt(i)).floatValue();
		}
		if (update && ((canvas != null) && (canvas.getGraphics() != null) && (bounds != null))) {
			for (int i = 0; i < bars.length; i++) {
				((DistrictOverviewBar) bars[i]).delete(canvas.getGraphics());
				if (Math.abs(((DistrictOverviewBar) bars[i]).min) > ((DistrictOverviewBar) bars[i]).max) {
					borderValue = ((DistrictOverviewBar) bars[i]).min;
				} else {
					borderValue = ((DistrictOverviewBar) bars[i]).max;
				}
				setupBar(i);
				bars[i].draw(canvas.getGraphics());
			}
		}

	}

	@Override
	public void setDescriptions(Vector v) {
// super.setDescriptions(v);
		if (v != null && bars != null && v.size() > 0) {
			for (int i = 0; i < Math.min(v.size(), bars.length); i++)
				if (v.elementAt(i) instanceof String) {
					((DistrictOverviewBar) bars[i]).fullDescription = (String) v.elementAt(i);
				}
		}
	}

	/** Sets the averages.
	 *
	 * @param v a Vector containing the averages of the selected attributes
	 */
	public void setAverages(Vector v) {
		if (v != null && bars != null && v.size() > 0) {
			for (int i = 0; i < Math.min(v.size(), bars.length); i++) {
				if (v.elementAt(i) instanceof Float) {
					((DistrictOverviewBar) bars[i]).average = ((Float) v.elementAt(i)).floatValue();
				}
			}
		}
	}

	/** Sets the medians.
	 *
	 * @param v a Vector containing the medians of each bar.
	 */
	public void setMedians(Vector v) {
		if (v != null && bars != null && v.size() > 0) {
			for (int i = 0; i < Math.min(v.size(), bars.length); i++) {
				if (v.elementAt(i) instanceof Float) {
					((DistrictOverviewBar) bars[i]).median = ((Float) v.elementAt(i)).floatValue();
				}
			}
		}
	}

	/** Sets the maximums.
	 *
	 * @param v a Vector containing the maximums of each bar
	 */
	public void setMaximums(Vector v) {
		if (v != null && bars != null && v.size() > 0) {
			positiveMaximum = false;
			for (int i = 0; i < Math.min(v.size(), bars.length); i++) {
				if (v.elementAt(i) instanceof Float) {
					((DistrictOverviewBar) bars[i]).max = ((Float) v.elementAt(i)).floatValue();
					if (((DistrictOverviewBar) bars[i]).max > 0) {
						positiveMaximum = true;
					}
				}
			}
		}
	}

	/** Sets the minimums.
	 *
	 * @param v a Vector containing the minimums of each bar.
	 */
	public void setMinimums(Vector v) {
		if (v != null && bars != null && v.size() > 0) {
			negativeMinimum = false;
			for (int i = 0; i < Math.min(v.size(), bars.length); i++) {
				if (v.elementAt(i) instanceof Float) {
					((DistrictOverviewBar) bars[i]).min = ((Float) v.elementAt(i)).floatValue();
				}
				if (((DistrictOverviewBar) bars[i]).min < 0) {
					negativeMinimum = true;
				}
			}
		}
	}

	/** Is called by {@link #setup() setup} and {@link #setValues(Vector v)
	 *  setValues} to calculate screen-data for each bar.
	 *
	 * @param i number of the bar to be set up
	 */
	private void setupBar(int i) {
		bars[i].description = StringUtil.getCutString(((DistrictOverviewBar) bars[i]).fullDescription, barWidth, canvas.getGraphics());

		if (bars[i].value < 0) {
			bars[i].y = Math.max(zeroLineLoc, ((DistrictOverviewBar) bars[i]).frameY);
			bars[i].height = Math.round(maxLength / Math.abs(borderValue) * -1 * bars[i].value) - ((((DistrictOverviewBar) bars[i]).frameY > zeroLineLoc) ? (((DistrictOverviewBar) bars[i]).frameY - zeroLineLoc) : 0);
		} else {
			bars[i].y = zeroLineLoc - Math.round(maxLength / Math.abs(borderValue) * bars[i].value);
			bars[i].height = Math.min(zeroLineLoc, ((DistrictOverviewBar) bars[i]).frameY + ((DistrictOverviewBar) bars[i]).frameHeight) - bars[i].y;
		}

	}

	/** Calculates values for {@link #zeroLineLoc zeroLineLoc}, {@link #maxLength
	 * maxLength}, {@link #spaceWidth spaceWidth} and {@link #barWidth barWidth}.
	 * This is done according to current screen proportions taken from {@link
	 * #bounds}.
	 */
	@Override
	protected void calcBoundsDependentData() {
		if (bounds == null)
			return;
		zeroLineLoc = bounds.y + topMargin;
		if (positiveMaximum) {
			zeroLineLoc += Math.round((bounds.height - topMargin - bottomMargin) / 2);
		}
		if (!negativeMinimum) {
			zeroLineLoc += Math.round((bounds.height - topMargin - bottomMargin) / 2);
		}
		maxLength = bounds.height - (bounds.height - zeroLineLoc) - bottomMargin;
		spaceWidth = Math.min(maxSpaceWidth, Math.max(minSpaceWidth, Math.round((Math.round((bounds.width - leftMargin - rightMargin) / (bars.length + 1)) * spacePercentage) / 100)));
		barWidth = Math.min(maxBarWidth, Math.max(minBarWidth, Math.round((bounds.width - leftMargin - rightMargin) / (bars.length + 1)) - spaceWidth));
	}

	@Override
	public void setup() {
		if ((bars == null) || (bounds == null))
			return;
		calcBoundsDependentData();
		for (int i = 0; i < bars.length; i++) {
			if (Math.abs(((DistrictOverviewBar) bars[i]).min) > ((DistrictOverviewBar) bars[i]).max) {
				borderValue = ((DistrictOverviewBar) bars[i]).min;
			} else {
				borderValue = ((DistrictOverviewBar) bars[i]).max;
			}

			bars[i].width = barWidth;
			bars[i].x = bounds.x + leftMargin + i * (barWidth + spaceWidth) + Math.round((barWidth + spaceWidth) / 2);

			if (borderValue < 0) {
				((DistrictOverviewBar) bars[i]).frameY = zeroLineLoc - Math.round(maxLength / Math.abs(((DistrictOverviewBar) bars[i]).min) * ((DistrictOverviewBar) bars[i]).max);
				((DistrictOverviewBar) bars[i]).frameHeight = bounds.y + bounds.height - ((DistrictOverviewBar) bars[i]).frameY - bottomMargin;
				if ((((DistrictOverviewBar) bars[i]).max < 0) && (Metrics.getFontMetrics().getHeight() > ((DistrictOverviewBar) bars[i]).frameY - zeroLineLoc)) {
					bars[i].descriptionY = zeroLineLoc;
				} else {
					bars[i].descriptionY = ((DistrictOverviewBar) bars[i]).frameY - 2;
				}
			} else {
				((DistrictOverviewBar) bars[i]).frameY = bounds.y + topMargin;
				((DistrictOverviewBar) bars[i]).frameHeight = zeroLineLoc - Math.round(maxLength / Math.abs(((DistrictOverviewBar) bars[i]).max) * ((DistrictOverviewBar) bars[i]).min) - topMargin;
				if ((((DistrictOverviewBar) bars[i]).min > 0) && (Metrics.getFontMetrics().getHeight() > zeroLineLoc - (((DistrictOverviewBar) bars[i]).frameY + ((DistrictOverviewBar) bars[i]).frameHeight))) {
					bars[i].descriptionY = zeroLineLoc + Metrics.getFontMetrics().getHeight();
				} else {
					bars[i].descriptionY = ((DistrictOverviewBar) bars[i]).frameY + ((DistrictOverviewBar) bars[i]).frameHeight + Metrics.getFontMetrics().getHeight();
				}
			}

			setupBar(i);

			bars[i].descriptionX = bars[i].x + Math.round(bars[i].width / 2) - Math.round(Metrics.stringWidth(bars[i].description) / 2);

			((DistrictOverviewBar) bars[i]).medianY = zeroLineLoc - Math.round(maxLength / Math.abs(borderValue) * ((DistrictOverviewBar) bars[i]).median);

			((DistrictOverviewBar) bars[i]).averageY = zeroLineLoc - Math.round(maxLength / Math.abs(borderValue) * ((DistrictOverviewBar) bars[i]).average);

		}
		if (bars != null) {
			for (int i = 0; i < bars.length; i++) {
				((DistrictOverviewBar) bars[i]).hotSpot = new HotSpot(canvas, bars[i].x, Math.min(((DistrictOverviewBar) bars[i]).descriptionY - Metrics.getFontMetrics().getHeight(), ((DistrictOverviewBar) bars[i]).frameY), bars[i].width,
						((DistrictOverviewBar) bars[i]).frameHeight
								+ ((Math.abs(((DistrictOverviewBar) bars[i]).max) < Math.abs(((DistrictOverviewBar) bars[i]).min)) ? Math.abs(((DistrictOverviewBar) bars[i]).frameY - bars[i].descriptionY) + Metrics.getFontMetrics().getHeight()
										: Math.abs(((DistrictOverviewBar) bars[i]).frameY + ((DistrictOverviewBar) bars[i]).frameHeight - bars[i].descriptionY)));

				DistrictOverviewBar bar = (DistrictOverviewBar) bars[i];
				((DistrictOverviewBar) bars[i]).hotSpot.setPopup(((DistrictOverviewBar) bars[i]).fullDescription + "\n " + res.getString("value") + ": " + StringUtil.floatToStr(bar.value, bar.min, bar.max) + "\n " + res.getString("average") + ": "
						+ StringUtil.floatToStr(bar.average, bar.min, bar.max) + "\n Maximum: " + StringUtil.floatToStr(bar.max, bar.min, bar.max) + "\n " + res.getString("median") + ": " + StringUtil.floatToStr(bar.median, bar.min, bar.max)
						+ "\n Minimum: " + StringUtil.floatToStr(bar.min, bar.min, bar.max));
			}
		}

	}

	@Override
	public void setBounds(Rectangle b) {
		super.setBounds(b);
		for (int i = 0; i < bars.length; i++) {
			((DistrictOverviewBar) bars[i]).hotSpot = new HotSpot(canvas, bars[i].x, Math.min(((DistrictOverviewBar) bars[i]).descriptionY - Metrics.getFontMetrics().getHeight(), ((DistrictOverviewBar) bars[i]).frameY), bars[i].width,
					((DistrictOverviewBar) bars[i]).frameHeight
							+ ((Math.abs(((DistrictOverviewBar) bars[i]).max) < Math.abs(((DistrictOverviewBar) bars[i]).min)) ? Math.abs(((DistrictOverviewBar) bars[i]).frameY - bars[i].descriptionY) + Metrics.getFontMetrics().getHeight() : Math
									.abs(((DistrictOverviewBar) bars[i]).frameY + ((DistrictOverviewBar) bars[i]).frameHeight - bars[i].descriptionY)));

			DistrictOverviewBar bar = (DistrictOverviewBar) bars[i];
			((DistrictOverviewBar) bars[i]).hotSpot.setPopup(((DistrictOverviewBar) bars[i]).fullDescription + "\n " + res.getString("value") + ": " + StringUtil.floatToStr(bar.value, bar.min, bar.max) + "\n " + res.getString("average") + ": "
					+ StringUtil.floatToStr(bar.average, bar.min, bar.max) + "\n Maximum: " + StringUtil.floatToStr(bar.max, bar.min, bar.max) + "\n " + res.getString("median") + ": " + StringUtil.floatToStr(bar.median, bar.min, bar.max)
					+ "\n Minimum: " + StringUtil.floatToStr(bar.min, bar.min, bar.max));
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (actionListener.isEmpty())
			return;
		int cur = -1;
		for (int i = 0; i < bars.length; i++)
			if (bars[i].contains(e.getX(), e.getY())) {
				cur = i;
				break;
			}
		if (cur == cursorIn)
			return;
		cursorIn = cur;
		for (int i = 0; i < actionListener.size(); i++) {
			((ActionListener) actionListener.elementAt(i)).actionPerformed(new ActionEvent(this, -1, String.valueOf(cursorIn)));
		}
	}

	@Override
	public void setCanvas(Canvas c) {
		super.setCanvas(c);
		if (canvas != null) {
			canvas.addMouseMotionListener(this);
		}
	}

	@Override
	public void destroy() {
		if (canvas != null) {
			canvas.removeMouseMotionListener(this);
		}
		super.destroy();
	}

}
