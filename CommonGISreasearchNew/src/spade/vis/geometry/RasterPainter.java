package spade.vis.geometry;

import java.awt.Color;

public class RasterPainter {
	protected static int defAlpha = 0xFF000000;
	protected static float cmpTo = 0.0f;
	protected static float posHue = 0.15f, negHue = 0.7f;

	public static int getColorForValue(double val, double min, double max) {
		return getColorForValue((float) val, (float) min, (float) max);
	}

	public static int getColorForValue(float val, float min, float max) {
		if (val < 0)
			return 0x33FFFFFF;
		min -= cmpTo;
		max -= cmpTo;
		val -= cmpTo;
		float maxMod = Math.abs(max);
		if (Math.abs(min) > maxMod) {
			maxMod = Math.abs(min);
		}
		float ystart = 0;
		if (min > 0) {
			ystart = min;
		} else if (max < 0) {
			ystart = -max;
		}
		float ratio = (Math.abs(val) - ystart) / (maxMod - ystart);
		float hue = (val > 0) ? posHue : negHue;
		return Color.HSBtoRGB(hue, 1.0f, ratio);
	}
}