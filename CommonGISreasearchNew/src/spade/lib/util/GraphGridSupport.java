package spade.lib.util;

import java.util.Vector;

/**
* Used for drawing grids on graphs and plots. From the specified value range,
* the available screen size (width or height) for representation of this range,
* and minimum possible screen interval between grid positions computes a set
* of appropriate grid positions corresponding to more or less "round" numbers
* withing the value range.
*/
public class GraphGridSupport {
	/**
	* From the specified value range (from minValue to maxValue), the available
	* screen size scrSize (width or height) for representation of this range,
	* and minimum possible screen interval between grid positions minInterval
	* computes a set of appropriate grid positions corresponding to more or less
	* "round" numbers withing the value range. The argument maxInterval specifies
	* the maximum desired length of the interval between the grid positions.
	*/
	public static GridPosition[] makeGrid(float minValue, float maxValue, int scrSize, int minInterval, int maxInterval) {
		return makeGrid(minValue, maxValue, Float.NaN, scrSize, minInterval, maxInterval);
	}

	/**
	* The same as previous, but allows specification of a mandatory "base value"
	* for the grid. For example, it may be required to have a grid line
	* corresponding to the value 0 or 1.
	*/
	public static GridPosition[] makeGrid(float minValue, float maxValue, float baseValue, int scrSize, int minInterval, int maxInterval) {
		if (Float.isNaN(minValue) || Float.isNaN(maxValue) || minValue >= maxValue || scrSize <= minInterval || minInterval < 1)
			return null;
		int interval = (maxInterval > minInterval) ? (minInterval + maxInterval) / 2 : minInterval;
		int nlines = scrSize / interval;
		if (nlines < 1)
			return null;

		float step = (maxValue - minValue) / nlines;
		//round up the step
		float n = 1.0f;
		int prec = 0;
		if (step > 1) {
			while (step > 1) {
				step /= 10;
				n *= 10;
				--prec;
			}
		} else {
			while (step < 0.1f) {
				step *= 10;
				n /= 10;
				++prec;
			}
		}
		boolean odd = true;
		if (step < 0.15f) {
			step = 0.1f;
			++prec;
		} else if (step <= 0.22f) {
			step = 0.2f;
			++prec;
			odd = false;
		} else if (step <= 0.35f) {
			step = 0.25f;
			prec += 2;
		} else if (step <= 0.75f) {
			step = 0.5f;
			++prec;
		} else {
			step = 1.0f;
			odd = false;
		}
		step *= n;

		nlines = Math.round((maxValue - minValue) / step);
		if (scrSize / nlines > maxInterval) {
			step /= 2;
			if (n <= 10 || (odd && n <= 100)) {
				++prec;
			}
			nlines = Math.round((maxValue - minValue) / step);
		}
		if (Float.isNaN(baseValue) || baseValue < minValue || baseValue > maxValue) {
			baseValue = makeRoundNumber(minValue + step);
			while (baseValue < minValue) {
				baseValue += step;
			}
			while (baseValue > maxValue) {
				baseValue -= step;
			}
		}
		if (baseValue < minValue || baseValue > maxValue)
			return null;
		Vector positions = new Vector(nlines + 5, 1);
		float val = baseValue;
		int pos = Math.round((val - minValue) * scrSize / (maxValue - minValue));
		while (pos < scrSize) {
			if (pos > 0) {
				GridPosition grp = new GridPosition();
				grp.value = val;
				grp.offset = pos;
				grp.strVal = StringUtil.floatToStr(val, prec);
				positions.addElement(grp);
			}
			val += step;
			pos = Math.round((val - minValue) * scrSize / (maxValue - minValue));
		}
		val = baseValue - step;
		pos = Math.round((val - minValue) * scrSize / (maxValue - minValue));
		while (pos > 0) {
			if (pos < scrSize) {
				GridPosition grp = new GridPosition();
				grp.value = val;
				grp.offset = pos;
				grp.strVal = StringUtil.floatToStr(val, prec);
				positions.insertElementAt(grp, 0);
			}
			val -= step;
			pos = Math.round((val - minValue) * scrSize / (maxValue - minValue));
		}
		if (positions.size() < 1)
			return null;
		GridPosition result[] = new GridPosition[positions.size()];
		for (int i = 0; i < positions.size(); i++) {
			result[i] = (GridPosition) positions.elementAt(i);
		}
		return result;
	}

	/**
	* Heuristically "rounds" the given float number.
	*/
	public static float makeRoundNumber(float value) {
		if (Float.isNaN(value))
			return Float.NaN;
		boolean negative = value < 0;
		if (negative) {
			value = -value;
		}
		if (value == 0f)
			return 0f;
		//round up the number
		float n = 1.0f;
		if (value > 1) {
			while (value > 1) {
				value /= 10;
				n *= 10;
			}
		} else {
			while (value < 0.1f) {
				value *= 10;
				n /= 10;
			}
		}
		if (value < 0.15f) {
			value = 0.1f;
		} else if (value <= 0.22f) {
			value = 0.2f;
		} else if (value <= 0.35f) {
			value = 0.25f;
		} else if (value <= 0.75f) {
			value = 0.5f;
		} else {
			value = 1.0f;
		}
		value *= n;
		if (negative) {
			value = -value;
		}
		return value;
	}
}