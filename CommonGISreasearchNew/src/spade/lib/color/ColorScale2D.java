package spade.lib.color;

import java.awt.Color;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Oct 22, 2009
 * Time: 4:04:20 PM
 * Produces a 2-dimensional color scale with the blue, cyan, red, and yellow colors
 * in the upper left, upper right, lower left, and lower right corners, respectively.
 * The other colors are derived by mixing these main colors.
 */
public class ColorScale2D {
	public static Color ulColor = Color.blue, urColor = Color.cyan, llColor = Color.red, lrColor = Color.yellow;
	public static Color cColors[] = { ulColor, urColor, llColor, lrColor };

	protected static int reds[] = null, greens[] = null, blues[] = null;

	protected static void getComponents() {
		if (reds != null && greens != null && blues != null)
			return;
		reds = new int[cColors.length];
		greens = new int[cColors.length];
		blues = new int[cColors.length];
		for (int i = 0; i < cColors.length; i++) {
			reds[i] = cColors[i].getRed();
			greens[i] = cColors[i].getGreen();
			blues[i] = cColors[i].getBlue();
		}
	}

	/**
	 * Generates the color for the given row and column of a coloured matrix
	 * assuming that the matrix has the specified number of rows and columns
	 * @param rowIdx - index of the row in the matrix
	 * @param columnIdx - index of the column in the matrix
	 * @param nRows - number of rows in the matrix
	 * @param nColumns - number of columns in the matrix
	 * @return the generated color
	 */
	public static Color getColor(int rowIdx, int columnIdx, int nRows, int nColumns) {
		getComponents();
		if (nRows < 1) {
			nRows = 1;
		}
		if (nColumns < 1) {
			nColumns = 1;
		}
		if (rowIdx > nRows - 1) {
			rowIdx = nRows - 1;
		}
		if (rowIdx < 0) {
			rowIdx = 0;
		}
		if (columnIdx > nColumns - 1) {
			columnIdx = nColumns - 1;
		}
		if (columnIdx < 0) {
			columnIdx = 0;
		}
		double r = rowIdx, c = columnIdx;
		if (nRows < nColumns && nRows > 1) {
			//scale vertically, i.e. modify row index
			r = r * (nColumns - 1) / (nRows - 1);
		} else if (nRows > nColumns && nColumns > 1) {
			//scale horizontally, i.e. modify column index
			c = c * (nRows - 1) / (nColumns - 1);
		}
		int maxD = Math.max(nRows, nColumns) - 1;
		double r1 = maxD - r, c1 = maxD - c;
		//distances to the corners
		double dc[] = { Math.sqrt(r * r + c * c), Math.sqrt(r * r + c1 * c1), Math.sqrt(r1 * r1 + c * c), Math.sqrt(r1 * r1 + c1 * c1) };
		double weights[] = new double[4];
		for (int i = 0; i < 4; i++) {
			weights[i] = (maxD - dc[i]) / maxD;
			if (weights[i] < 0) {
				weights[i] = 0;
			}
		}
		double dr = 0, dg = 0, db = 0;
		for (int i = 0; i < cColors.length; i++) {
			dr += weights[i] * reds[i];
			dg += weights[i] * greens[i];
			db += weights[i] * blues[i];
		}
		if (dr < 0) {
			dr = 0;
		}
		if (dg < 0) {
			dg = 0;
		}
		if (db < 0) {
			db = 0;
		}
		int red = (int) Math.min(dr, 255), green = (int) Math.min(dg, 255), blue = (int) Math.min(db, 255);
		return new Color(red, green, blue);
	}

	/**
	 * A continuous version
	 * @return the generated color
	 */
	public static Color getColor(float x, float y, float minX, float maxX, float minY, float maxY) {
		getComponents();
		if (x > maxX) {
			x = maxX;
		}
		if (x < minX) {
			x = minX;
		}
		if (y > maxY) {
			y = maxY;
		}
		if (y < minY) {
			y = minY;
		}
		float wX = maxX - minX, wY = maxY - minY;
		double r = y - minY, c = x - minX;
		if (wY < wX) {
			//scale vertically, i.e. modify r
			r = r * wX / wY;
		} else if (wY > wX) {
			//scale horizontally, i.e. modify c
			c = c * wY / wX;
		}
		float maxD = Math.max(wX, wY);
		double r1 = maxD - r, c1 = maxD - c;
		//distances to the corners
		double dc[] = { Math.sqrt(r * r + c * c), Math.sqrt(r * r + c1 * c1), Math.sqrt(r1 * r1 + c * c), Math.sqrt(r1 * r1 + c1 * c1) };
		double weights[] = new double[4];
		for (int i = 0; i < 4; i++) {
			weights[i] = (maxD - dc[i]) / maxD;
			if (weights[i] < 0) {
				weights[i] = 0;
			}
		}
		double dr = 0, dg = 0, db = 0;
		for (int i = 0; i < cColors.length; i++) {
			dr += weights[i] * reds[i];
			dg += weights[i] * greens[i];
			db += weights[i] * blues[i];
		}
		if (dr < 0) {
			dr = 0;
		}
		if (dg < 0) {
			dg = 0;
		}
		if (db < 0) {
			db = 0;
		}
		int red = (int) Math.min(dr, 255), green = (int) Math.min(dg, 255), blue = (int) Math.min(db, 255);
		return new Color(red, green, blue);
	}

	/**
	 * Generates a color using the circular color model
	 */
	public static Color getColorCircular(float x, float y, float minX, float maxX, float minY, float maxY) {
		if (x > maxX) {
			x = maxX;
		}
		if (x < minX) {
			x = minX;
		}
		if (y > maxY) {
			y = maxY;
		}
		if (y < minY) {
			y = minY;
		}
		float wX = maxX - minX, wY = maxY - minY;
		double yy = y - minY, xx = x - minX;
		xx -= wX / 2;
		yy -= wY / 2;
		if (xx == 0 && yy == 0)
			return new Color(245, 245, 245);
		//double rad=Math.max(wX,wY)/2;
		double rad = (wX + wY) / 4;
		double dist = Math.sqrt(xx * xx + yy * yy);
		double cos = xx / dist;
		double angle = Math.acos(cos);
		if (yy < 0) {
			angle = Math.PI + Math.PI - angle;
		}
		angle -= Math.PI / 2;
		if (angle < 0) {
			angle += 2 * Math.PI;
		}
		float hue = (float) (angle / Math.PI / 2);
		dist /= rad;
		dist += 0.05f; //to avoid fully desaturated colors
		float sat = (float) Math.min(dist, 1);
		float bri = (dist <= 1) ? 1f : (float) (2 - dist);
		return Color.getHSBColor(hue, sat, bri);
	}
}
