//ID
package spade.vis.geometry;

import java.util.Enumeration;
import java.util.Hashtable;

import spade.analysis.geocomp.functions.Function;

public class RasterGeometry extends RealRectangle {

	public int Col, Row;
	public float Xbeg, Ybeg, DX, DY;
	public boolean Intr, Geog;
	public float minV, maxV;
	/**
	* Indicates that the raster is binary, i.e. consists of 0 and 1
	*/
	public boolean isBinary = false;

	public float ras[][];

	public RasterGeometry() {
	}

	public RasterGeometry(float x1, float y1, float x2, float y2) {
		super(x1, y1, x2, y2);
	}

	@Override
	public char getType() {
		return raster;
	}

	@Override
	public Object clone() {
		RasterGeometry rg = new RasterGeometry();
		rg.rx1 = rx1;
		rg.ry1 = ry1;
		rg.rx2 = rx2;
		rg.ry2 = ry2;
		rg.Col = Col;
		rg.Row = Row;
		rg.Xbeg = Xbeg;
		rg.Ybeg = Ybeg;
		rg.DX = DX;
		rg.DY = DY;
		rg.Intr = Intr;
		rg.Geog = Geog;
		rg.maxV = maxV;
		rg.minV = minV;
		rg.isBinary = isBinary;
		rg.ras = new float[Col][Row];
		for (int j = 0; j < Row; j++) {
			for (int i = 0; i < Col; i++) {
				rg.ras[i][j] = ras[i][j];
			}
		}
		return rg;
	}

	public void recalculateStatistics() {
		if (isBinary) {
			minV = 0;
			maxV = 1;
		} else {
			minV = Float.MAX_VALUE;
			maxV = -Float.MAX_VALUE;
			float curV;
			for (int yy = 0; yy < Row; yy++) {
				for (int xx = 0; xx < Col; xx++) {
					curV = ras[xx][yy];
					if (minV > curV) {
						minV = curV;
					}
					if (maxV < curV) {
						maxV = curV;
					}
				}
			}
		}
	}

	public float getWorldX(float gridX) {
		return Xbeg + gridX * DX;
	}

	public float getWorldY(float gridY) {
		return Ybeg + gridY * DY;
	}

	public float getGridX(float worldX) {
		return (worldX - Xbeg) / DX;
	}

	public float getGridY(float worldY) {
		return (worldY - Ybeg) / DY;
	}

	public float getInterpolatedValue(float x, float y) {
		int x0 = (int) Math.floor(x);
		int y0 = (int) Math.floor(y);
		int x01 = x0 + 1;
		int y01 = y0 + 1;

		double xf = x - x0;
		double xf1 = 1 - xf;
		double yf = y - y0;
		double yf1 = 1 - yf;

		float avg = Float.NaN, avg1 = 0, nbr = 0, delta = 0, delta1 = 0;

		try {
			avg = (float) (ras[x0][y0] * xf1 * yf1 + ras[x01][y0] * xf * yf1 + ras[x0][y01] * xf1 * yf + ras[x01][y01] * xf * yf);
		} catch (ArrayIndexOutOfBoundsException e) {
			if (x01 >= Col || x0 < 0) {
				x0 = x01 = Math.round(x);
			}
			if (y01 >= Row || y0 < 0) {
				y0 = y01 = Math.round(y);
			}
			try {
				avg = (float) (ras[x0][y0] * xf1 * yf1 + ras[x01][y0] * xf * yf1 + ras[x0][y01] * xf1 * yf + ras[x01][y01] * xf * yf);
			} catch (Exception ex) {
				return Float.NaN;
			}
		}
		if (Float.isNaN(avg)) {
			float v0_0 = ras[x0][y0];
			float v01_0 = ras[x01][y0];
			float v0_01 = ras[x0][y01];
			float v01_01 = ras[x01][y01];
			float average = 0;
			int valuable = 0;
			float mNaN = 0;

			if (!Float.isNaN(v0_0)) {
				average += v0_0;
				valuable++;
			} else {
				mNaN += xf1 * yf1;
			}
			if (!Float.isNaN(v01_0)) {
				average += v01_0;
				valuable++;
			} else {
				mNaN += xf * yf1;
			}
			if (!Float.isNaN(v0_01)) {
				average += v0_01;
				valuable++;
			} else {
				mNaN += xf1 * yf;
			}
			if (!Float.isNaN(v01_01)) {
				average += v01_01;
				valuable++;
			} else {
				mNaN += xf * yf;
			}

			if (valuable > 0 && mNaN < 0.5) {
				average /= valuable;
				if (Float.isNaN(v0_0)) {
					v0_0 = average;
				}
				if (Float.isNaN(v01_0)) {
					v01_0 = average;
				}
				if (Float.isNaN(v0_01)) {
					v0_01 = average;
				}
				if (Float.isNaN(v01_01)) {
					v01_01 = average;
				}

				avg = (float) (v0_0 * xf1 * yf1 + v01_0 * xf * yf1 + v0_01 * xf1 * yf + v01_01 * xf * yf);
			}
/*
      float mNaN=0, rest=0, vras=0;
      double mult=0;

      vras = ras[x0][y0];
      mult = xf1*yf1;
      if (Float.isNaN(vras))
        mNaN += mult;
      else
        rest += vras*mult;

      vras = ras[x01][y0];
      mult = xf*yf1;
      if (Float.isNaN(vras))
        mNaN += mult;
      else
        rest += vras*mult;

      vras = ras[x0][y01];
      mult = xf1*yf;
      if (Float.isNaN(vras))
        mNaN += mult;
      else
        rest += vras*mult;

      vras = ras[x01][y01];
      mult = xf*yf;
      if (Float.isNaN(vras))
        mNaN += mult;
      else
        rest += vras*mult;

      if (mNaN < 0.5) avg = rest;
*/
		}
		if (avg < minV || avg > maxV) {
			avg = Float.NaN;
		}

		if ((!Intr || isBinary) && !Float.isNaN(avg)) {
			// choosing nearest neighbor
			delta = delta1 = Float.POSITIVE_INFINITY;
			avg1 = Float.NaN;

			nbr = ras[x0][y0];
			delta1 = Math.abs(avg - nbr);
			if (delta1 < delta) {
				avg1 = nbr;
				delta = delta1;
			}

			nbr = ras[x01][y0];
			delta1 = Math.abs(avg - nbr);
			if (delta1 < delta) {
				avg1 = nbr;
				delta = delta1;
			}

			nbr = ras[x0][y01];
			delta1 = Math.abs(avg - nbr);
			if (delta1 < delta) {
				avg1 = nbr;
				delta = delta1;
			}

			nbr = ras[x01][y01];
			delta1 = Math.abs(avg - nbr);
			if (delta1 < delta) {
				avg1 = nbr;
				delta = delta1;
			}

			avg = avg1;
		}
		return avg;
	}

	public float getNearestValue(float x, float y) {
		int xr = Math.round(x);
		int yr = Math.round(y);

		float val = Float.NaN;

		try {
			val = ras[xr][yr];
		} catch (Exception ex) {
		}

		return val;
	}

	public float getAggregatedValue(float wx, float wy, float cellX, float cellY) {
		// if there's only one value to aggregate, we return interpolated value instead
		if (cellX < Math.abs(DX) || cellY < Math.abs(DY))
			return getInterpolatedValue(getGridX(wx), getGridY(wy));

		int x1 = (int) Math.ceil(getGridX(wx - cellX / 2));
		int x2 = (int) Math.floor(getGridX(wx + cellX / 2));
		int y1 = (int) Math.ceil(getGridY(wy - cellY / 2));
		int y2 = (int) Math.floor(getGridY(wy + cellY / 2));

		if (Intr) {
			float curV = 0, aggr = 0;
			int count = 0, countNaN = 0;

			// we summarize values in our cell
			for (int yy = y1; yy <= y2; yy++) {
				for (int xx = x1; xx <= x2; xx++) {
					try {
						curV = ras[xx][yy];
						if (Float.isNaN(curV)) {
							countNaN++;
						} else {
							aggr += curV;
						}
						count++;
					} catch (Exception ex) {
					}
				}
			}

			// if the cell is small, we add precisely interpolated value to our sum
			if (cellX < 3 * Math.abs(DX) || cellY < 3 * Math.abs(DY)) {
				curV = getInterpolatedValue(getGridX(wx), getGridY(wy));
				if (Float.isNaN(curV)) {
					countNaN++;
				} else {
					aggr += curV;
				}
				count++;
			}

			if (countNaN > count / 2)
				return Float.NaN;
			else
				// the mean of values is returned
				return aggr / (count - countNaN);
		} else {
			// qualitative rasters
			Hashtable freq = new Hashtable();

			int total = 0;
			Float val;
			Integer count;
			float interpolated = Float.NaN;

			// a numbers of values' occurencies are calculated
			for (int yy = y1; yy <= y2; yy++) {
				for (int xx = x1; xx <= x2; xx++) {
					try {
						val = new Float(ras[xx][yy]);
						count = (Integer) freq.get(val);
						if (count == null) {
							count = new Integer(0);
						}
						count = new Integer(count.intValue() + 1);
						freq.put(val, count);
						total++;
					} catch (Exception ex) {
					}
				}
			}

			// again, some precision tricks
			if (cellX < 3 * Math.abs(DX) || cellY < 3 * Math.abs(DY)) {
				try {
					interpolated = getInterpolatedValue(getGridX(wx), getGridY(wy));
					val = new Float(interpolated);
					count = (Integer) freq.get(val);
					if (count == null) {
						count = new Integer(0);
					}
					count = new Integer(count.intValue() + 1);
					freq.put(val, count);
					total++;
				} catch (Exception ex) {
				}
			}

			try {
				if (((Integer) freq.get(new Float(Float.NaN))).intValue() > total / 2)
					return Float.NaN;
			} catch (Exception ex) {
			}

			float dominant = Float.NaN;
			int occur = 0;

			// searching for dominant value
			for (Enumeration values = freq.keys(); values.hasMoreElements();) {
				val = (Float) values.nextElement();
				if (val.isNaN()) {
					continue;
				}
				int hits = ((Integer) freq.get(val)).intValue();
				if (hits > occur) {
					dominant = val.floatValue();
					occur = hits;
				}
			}

			// dominance := present >= times than others and more than 1 time (subject to discuss...)
			if (occur > 1)
				return dominant;
			else
			// all the values are different
			if (Float.isNaN(interpolated))
				return getInterpolatedValue(getGridX(wx), getGridY(wy));
			else
				return interpolated;
		}
	}

	public float getAggregatedValue(float wx, float wy, float cellX, float cellY, Function op) {
		if (op == null)
			return Float.NaN;

		op.init();

		// if there's only one value to aggregate, we return interpolated value instead
		if (cellX < Math.abs(DX) || cellY < Math.abs(DY)) {
			float interpolated = getInterpolatedValue(getGridX(wx), getGridY(wy));
			op.addData(interpolated);
			return (float) op.getResult();
		}

		int x1 = (int) Math.ceil(getGridX(wx - cellX / 2));
		int x2 = (int) Math.floor(getGridX(wx + cellX / 2));
		int y1 = (int) Math.ceil(getGridY(wy - cellY / 2));
		int y2 = (int) Math.floor(getGridY(wy + cellY / 2));

		float curV = 0;//, aggr=0;
		int count = 0, countNaN = 0;

		// we summarize values in our cell
		for (int yy = y1; yy <= y2; yy++) {
			for (int xx = x1; xx <= x2; xx++) {
				try {
					curV = ras[xx][yy];
					if (Float.isNaN(curV)) {
						countNaN++;
					} else {
						op.addData(curV);
					}
					count++;
				} catch (Exception ex) {
				}
			}
		}

		// if the cell is small, we add precisely interpolated value to our sum
		if (cellX < 3 * Math.abs(DX) || cellY < 3 * Math.abs(DY)) {
			curV = getInterpolatedValue(getGridX(wx), getGridY(wy));
			if (Float.isNaN(curV)) {
				countNaN++;
			} else {
				op.addData(curV);
			}
			count++;
		}

		if (countNaN > count / 2)
			return Float.NaN;
		else
			return (float) op.getResult();
	}
}
//~ID