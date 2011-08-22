package spade.analysis.plot.bargraph;

import java.awt.Dimension;
import java.awt.Graphics;

import spade.lib.basicwin.Metrics;

/** Class representing a simple bargraph with horizontal bars.
 * See {@link HorizontalBarGraph HorizontalBarGraph} for an example code.
 *
 * @version 1.0
 */
public class VerticalBarGraph extends BarGraph {

	/** Contains the current location of the line dividing positive and negative
	 * values.
	 */
	protected int zeroLineLoc = 0;
	/** Contains the current maximum length in pixels of a bar.
	 */
	protected int maxLength = 0;
	/** Contains current width in pixels of the spaces between the bars.
	 */
	protected int spaceWidth = 0;
	/** Contains the current width in pixels of each bar.
	 */
	protected int barWidth = 0;

	/** Calculates values for {@link #zeroLineLoc zeroLineLoc}, {@link #maxLength
	 * maxLength}, {@link #spaceWidth spaceWidth} and {@link #barWidth barWidth}.
	 * This is done according to current screen proportions taken from {@link
	 * #bounds}.
	 */
	@Override
	protected void calcBoundsDependentData() {
		if (bounds == null)
			return;
		zeroLineLoc = bounds.y + topMargin + Math.round((bounds.height - topMargin - bottomMargin) / 2);
		maxLength = bounds.height - zeroLineLoc - bottomMargin;
		spaceWidth = Math.min(maxSpaceWidth, Math.max(minSpaceWidth, Math.round((Math.round((bounds.width - leftMargin - rightMargin) / (bars.length + 1)) * spacePercentage) / 100)));
		barWidth = Math.min(maxBarWidth, Math.max(minBarWidth, Math.round((bounds.width - leftMargin - rightMargin) / (bars.length + 1)) - spaceWidth));
	}

	/** Sets X/Y-coordinates, height, width and description coordinates of each bar.
	 */
	@Override
	public void setup() {
		if ((bars == null) || (bounds == null))
			return;
		for (int i = 0; i < bars.length; i++) {
			int length = Math.round(maxLength / borderValue * bars[i].value);
			if (length < 0) {
				bars[i].y = zeroLineLoc;
			} else {
				bars[i].y = zeroLineLoc - length;
			}
			bars[i].x = bounds.x + leftMargin + i * (barWidth + spaceWidth) + Math.round((spaceWidth + barWidth) / 2);
			bars[i].height = Math.abs(length);
			bars[i].width = barWidth;
			if (length < 0) {
				bars[i].descriptionY = zeroLineLoc - 2;
			} else {
				bars[i].descriptionY = zeroLineLoc + Metrics.getFontMetrics().getHeight();
			}
			bars[i].descriptionX = bars[i].x + Math.round(bars[i].width / 2) - Math.round(Metrics.stringWidth(bars[i].description) / 2);
		}
	}

	/** Draws the zero-line and a vertical line at the left of the bargraph using
	 * {@link #baselineColor baselineColor}.
	 *
	 * @param g Graphics-context on which the background is to be drawn
	 */
	@Override
	protected void drawBackground(Graphics g) {
		g.setColor(baselineColor);
		g.drawLine(bounds.x + leftMargin, zeroLineLoc, bounds.x + bounds.width - rightMargin, zeroLineLoc);
		g.drawLine(bounds.x + leftMargin, bounds.y + topMargin, bounds.x + leftMargin, bounds.y + bounds.height - bottomMargin);
	}

	/** Calculates minimum occupied screen-space of the bargraph.
	 * It does so respecting margins, {@link #minBarLength minBarLength}, {@link
	 * #minBarWidth minBarWidth}, {@link #minSpaceWidth minSpaceWidth}.
	 *
	 * @return virtual screen-space of the bargraph
	 */
	@Override
	public java.awt.Dimension getPreferredSize() {
		if (bars == null)
			return super.getPreferredSize();
		return new Dimension(leftMargin + rightMargin + (bars.length + 1) * (minBarWidth + minSpaceWidth) + Math.round((spaceWidth + barWidth) / 2), minBarLength * 2 + topMargin + bottomMargin);
	}

}
