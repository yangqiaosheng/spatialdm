package spade.lib.color;

import java.awt.Color;
import java.awt.Component;

/**
* Provides encoding of numeric values by colors
*/
public class SpectrumColorScale extends BaseColorScale {
	protected float minHue = /*0.0f*/0.15f, maxHue = /*0.72f*/1.0f;
	protected static float excludeLeft = 0.250f, excludeRight = 0.4f, excludeLength = excludeRight - excludeLeft;

	/**
	* Returns the color for the given value packed into an integer:
	* top 8 bits (0xFF << 24) - alpha value
	* 2nd 8 bits (0xFF << 16) - red value
	* 3rd 8 bits (0xFF << 8) - green value
	* bottom 8 bits (0xFF) - blue value
	*/
	@Override
	public int encodeValue(float val) {
		//if (val<0) return 0x00000000; //for test
		if (val < minLimit) {
			val = minLimit;
		}
		if (val > maxLimit) {
			val = maxLimit;
		}
		float ratio = (/*(maxLimit-val)*/(val - minLimit) / (maxLimit - minLimit));
		float hue = minHue + ratio * (maxHue - minHue - excludeLength);
		if (hue > excludeLeft) {
			hue += excludeLength;
		}
		int color = Color.HSBtoRGB(hue, 1.0f, 1.0f);
		return color;
	}

	/**
	* Must returns a UI component that allows the user to manipulate specific
	* parameters of this color scale (i.e. not transparency or minimum and
	* maximum limits that are manipulated in the same way for all color scales).
	* Returns null (no specific manipulation for this color scale).
	*/
	@Override
	public Component getManipulator() {
		return null;
	}
}