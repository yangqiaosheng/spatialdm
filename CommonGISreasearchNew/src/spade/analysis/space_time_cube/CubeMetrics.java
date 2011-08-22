package spade.analysis.space_time_cube;

import java.awt.Point;

import spade.time.Date;
import spade.time.TimeMoment;
import spade.vis.dmap.MapMetrics;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 28, 2008
 * Time: 11:52:56 AM
 * Used for drawing spatio-temporal objects in a space-time cube
 */
public class CubeMetrics extends MapMetrics {
	/**
	 * The x-extent of the visible territory, in real units
	 */
	public double xExtent = 0;
	/**
	 * The y-extent of the visible territory, in real units
	 */
	public double yExtent = 0;
	/**
	 * The y-origin, which may be different from y0
	 */
	public double yOrig = 0;
	/**
	 * The minimum time moment, which is mapped onto z-coordinate 0
	 */
	public TimeMoment minTime = null;
	/**
	 * The maximum time moment, which is mapped onto z-coordinate 1
	 */
	public TimeMoment maxTime = null;
	/**
	 * The length of the time interval
	 */
	public double timeLen = 0;

	/**
	 * The minimum time moment, which is mapped onto z-coordinate 0
	 */
	public void setMinTime(TimeMoment minTime) {
		this.minTime = minTime;
	}

	/**
	 * The maximum time moment, which is mapped onto z-coordinate 1
	 */
	public void setMaxTime(TimeMoment maxTime) {
		this.maxTime = maxTime;
	}

	/**
	* Depending on the current territory extent and the screen area where the
	* map is drawn sets its internal variables used for coordinate transformation.
	*/
	@Override
	public void setup() {
		if (visTerr == null || viewport == null) {
			reset();
			return;
		}
		mapCount = 1;
		currMapN = 0;
		currOrg = new Point(0, 0);
		super.setup();
		xExtent = mapW * step;
		yExtent = mapH * stepY;
		yOrig = prLat2 - yExtent;
		if (minTime != null && maxTime != null) {
			timeLen = maxTime.subtract(minTime);
		}
	}

	/**
	 * Returns the X-coordinate in the cube for the given real-world point.
	 * The range is from -1 to 1.
	 */
	public double cubeX(float x, float y) {
		double val = -1 + 2 * (x - x0) / xExtent;
		return val;
	}

	/**
	 * Returns the Y-coordinate in the cube for the given real-world point
	 * The range is from -1 to 1.
	 */
	public double cubeY(float x, float y) {
		double yy = (isGeographic) ? lat2Y(y) : y;
		double val = -1 + 2 * (yy - yOrig) / yExtent;
		return val;
	}

	/**
	 * Returns the Z-coordinate in the cube for the given time moment
	 * The range is from 0 to 1.
	 */
	public double cubeZ(TimeMoment t) {
		if (t == null || minTime == null || timeLen < 1)
			return 0;
		if (t.getPrecision() != minTime.getPrecision() && (t instanceof Date) && (minTime instanceof Date)) {
			Date d = (Date) t, d0 = (Date) minTime, d1 = (Date) d0.getCopy();
			for (char time_symbol : Date.time_symbols)
				if (d.hasElement(time_symbol)) {
					d1.setElementValue(time_symbol, d.getElementValue(time_symbol));
				}
			return d1.subtract(d0) / timeLen;
		}
		return t.subtract(minTime) / timeLen;
	}
}
