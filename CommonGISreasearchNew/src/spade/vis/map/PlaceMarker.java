package spade.vis.map;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import spade.vis.geometry.Geometry;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 28-Feb-2008
 * Time: 10:20:17
 * Marks a specified place on a map (in a MapDraw) by drawing cicles or ellipses
 * in this place and around it.
 */
public class PlaceMarker implements PropertyChangeListener {
	public static float mm = Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f;
	/**
	 * The place to be marked
	 */
	public Geometry place = null;
	/**
	* Coordinates of the centre of the place
	*/
	public float px = Float.NaN, py = Float.NaN;
	/**
	 * The horizontal and vertical radii of the ellipse encircling the place, in real units
	 */
	public float pRadiusX = 0, pRadiusY = 0;
	/**
	* The map in which the place must be marked
	*/
	protected MapDraw map = null;
	/**
	 * The radius (in pixels) of the small circle marking the place or its centre
	 */
	public int minRadius = Math.round(2 * mm);
	/**
	 * The distance between the place boundary and the larger circle, in pixels
	 */
	public int dist = Math.round(4 * mm);
	/**
	 * Two alternating colours used for marking
	 */
	public Color color1 = Color.yellow, color2 = Color.black;

	/**
	 * @param place - The place to be marked
	 * @param map - The map in which the place must be marked
	 * @param minRadius_mm - The radius (in millimeters) of the small circle marking the place or its centre
	 * @param dist_mm - The distance between the place boundary and the larger circle, in millimeters
	 */
	public PlaceMarker(Geometry place, MapDraw map, int minRadius_mm, int dist_mm) {
		if (map == null || place == null)
			return;
		float br[] = place.getBoundRect();
		if (br == null)
			return;
		this.place = place;
		this.map = map;
		minRadius = Math.round(minRadius_mm * mm);
		dist = Math.round(dist_mm * mm);
		px = (br[0] + br[2]) / 2;
		py = (br[1] + br[3]) / 2;
		pRadiusX = (br[2] - br[0]) / 2;
		pRadiusY = (br[3] - br[1]) / 2;
		map.adjustExtentToShowArea(px - pRadiusX, py - pRadiusY, px + pRadiusX, py + pRadiusY);
		//start listening to map zooming and redrawing events
		map.addPropertyChangeListener(this);
		drawMarker();
	}

	/**
	 * Sets two alternating colours used for marking
	 */
	public void setColors(Color color1, Color color2) {
		this.color1 = color1;
		this.color2 = color2;
	}

	/**
	 * Draws a visual marker around the place
	 */
	public void drawMarker() {
		if (map == null || Float.isNaN(px) || Float.isNaN(py))
			return;
		Graphics g = map.getGraphics();
		if (g == null)
			return;
		MapContext mc = map.getMapContext();
		if (mc == null)
			return;
		int x0 = mc.scrX(px, py), y0 = mc.scrY(px, py), x1 = mc.scrX(px - pRadiusX, py + pRadiusY), y1 = mc.scrY(px - pRadiusX, py + pRadiusY);
		if (x1 > x0 - minRadius - 1) {
			x1 = x0 - minRadius - 1;
		}
		if (y1 > y0 - minRadius - 1) {
			y1 = y0 - minRadius - 1;
		}
		x1 -= dist;
		y1 -= dist;
		int xD = (x0 - x1) * 2 + 1, yD = (y0 - y1) * 2 + 1;
		g.setColor(color1);
		for (int i = 0; i < 2; i++) {
			g.drawOval(x1, y1, xD, yD);
			++x1;
			++y1;
			xD -= 2;
			yD -= 2;
		}
		g.setColor(color2);
		for (int i = 0; i < 2; i++) {
			g.drawOval(x1, y1, xD, yD);
			++x1;
			++y1;
			xD -= 2;
			yD -= 2;
		}
		x1 = x0 - minRadius - 1;
		y1 = y0 - minRadius - 1;
		xD = yD = minRadius * 2 + 1;
		while (xD > 2) {
			g.setColor(color1);
			for (int i = 0; i < 2 && xD > 2; i++) {
				g.drawOval(x1, y1, xD, yD);
				++x1;
				++y1;
				xD -= 2;
				yD -= 2;
			}
			g.setColor(color2);
			for (int i = 0; i < 2 && xD > 2; i++) {
				g.drawOval(x1, y1, xD, yD);
				++x1;
				++y1;
				xD -= 2;
				yD -= 2;
			}
		}
	}

	public void eraseMarker() {
		if (map != null && !Float.isNaN(px) && !Float.isNaN(py)) {
			map.redraw();
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(map)) {
			drawMarker();
		}
	}

	/**
	 * Stops listening to map zooming and panning events
	 */
	public void destroy() {
		if (map != null) {
			map.removePropertyChangeListener(this);
		}
		if (!Float.isNaN(px) && !Float.isNaN(py)) {
			map.redraw();
		}
		map = null;
	}

}
