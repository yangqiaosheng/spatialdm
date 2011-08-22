package spade.analysis.plot.bargraph;

import java.awt.Dimension;
import java.awt.Graphics;

import spade.lib.basicwin.Metrics;

/** Class representing a simple bargraph with horizontal bars.
 * <p>
 * Example code for setting up a horizontal bargraph on a PlotCanvas.
 * Suppositional valvec is a Vector containing Floats and desvec one containing
 * Strings.
 * <pre>
 * HorizontalBarGraph barGraph=new HorizontalBarGraph();
 *
 * barGraph.setValues(valvec);
 * barGraph.setDescriptions(desvec);
 *
 * //Note that setValues has to be called before setDescriptions, because just
 * //setValues changes bounds of the internal bar-array and creates new
 * //bar-objects.
 *
 * barGraph.setMargin(5);
 * barGraph.setSpace(0,0,0);
 * barGraph.setMinBarWidth(Metrics.getFontMetrics().getHeight());
 * barGraph.setMinSpaceWidth(0);
 *
 * PlotCanvas() gCanvas = new PlotCanvas();
 * barGraph.setCanvas(gCanvas);
 * gCanvas.setContent(barGraph);
 * </pre>
 *
 * @author Mario Boley
 * @version 1.0
 */
public class HorizontalBarGraph extends BarGraph {

	/** Contains the current location of the line dividing positive and negative
	 *  values.
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
	 *  maxLength}, {@link #spaceWidth spaceWidth} and {@link #barWidth barWidth}.
	 *  This is done according to current screen proportions taken from {@link
	 *  #bounds}.
	 */
	@Override
	protected void calcBoundsDependentData() {
		if (bounds == null)
			return;
		zeroLineLoc = bounds.x + leftMargin + Math.round((bounds.width - leftMargin - rightMargin) / 2);
		maxLength = bounds.width - zeroLineLoc - rightMargin;
		Math.round(spaceWidth = Math.min(maxSpaceWidth, Math.max(minSpaceWidth, (Math.round((bounds.height - topMargin - bottomMargin) / bars.length) * spacePercentage) / 100)));
		barWidth = Math.min(maxBarWidth, Math.max(minBarWidth, Math.round((bounds.height - topMargin - bottomMargin) / bars.length) - spaceWidth));
	}

	/** Sets X/Y-coordinates, height, width and description coordinates of each bar.
	 */
	@Override
	public void setup() {
		if ((bars == null) || (bounds == null))
			return;
		for (int i = 0; i < bars.length; i++) {
			int length = Math.round(maxLength / borderValue * bars[i].value);
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
		}
	}

	/** Draws the zero-line and a horizontal line at the bottom of the bargraph
	 *  using {@link #baselineColor baselineColor}.
	 *
	 * @param g Graphics-context on which the background is to be drawn
	 */
	@Override
	protected void drawBackground(Graphics g) {
		g.setColor(baselineColor);
		g.drawLine(zeroLineLoc, bounds.y + topMargin, zeroLineLoc, bounds.y + bounds.height - bottomMargin);
		g.drawLine(bounds.x + leftMargin, bounds.y + bounds.height - bottomMargin, bounds.x + bounds.width - rightMargin, bounds.y + bounds.height - bottomMargin);
	}

	/** Calculates minimum occupied screen-space of the bargraph.
	 * It does so respecting margins, {@link #minBarLength minBarLength}, {@link
	 *  #minBarWidth minBarWidth}, {@link #minSpaceWidth minSpaceWidth}.
	 *
	 * @return virtual screen-space of the bargraph
	 */
	@Override
	public java.awt.Dimension getPreferredSize() {
		if (bars == null)
			return super.getPreferredSize();
		return new Dimension(minBarLength * 2 + leftMargin + rightMargin, topMargin + bottomMargin + bars.length * (minBarWidth + minSpaceWidth));
	}

}
