package spade.lib.color;

import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;

import spade.lib.lang.Language;

/**
* Provides encoding of numeric values by colors
*/
public class GrayColorScale extends BaseColorScale implements ItemListener {
	/**
	* When reversed is true, darker shades correspond to bigger values
	*/
	static ResourceBundle res = Language.getTextResource("spade.lib.color.Res");
	public boolean reversed = false;

	/**
	* Returns the color for the given value packed into an integer:
	* top 8 bits (0xFF << 24) - alpha value
	* 2nd 8 bits (0xFF << 16) - red value
	* 3rd 8 bits (0xFF << 8) - green value
	* bottom 8 bits (0xFF) - blue value
	*/
	@Override
	public int encodeValue(float val) {
		if (val < minLimit) {
			val = minLimit;
		}
		if (val > maxLimit) {
			val = maxLimit;
		}
		float ratio = ((val - minLimit) / (maxLimit - minLimit));
		if (reversed) {
			ratio = 1 - ratio;
		}
		int color = Color.HSBtoRGB(0.0f, 0.0f, ratio);
		return color;
	}

	/**
	* Returns a UI component that allows the user to manipulate specific
	* parameters of this color scale (i.e. not transparency or minimum and
	* maximum limits that are manipulated in the same way for all color scales)
	* A GrayColorScale returns a checkbox for switching on/off the "reversed" mode..
	*/
	@Override
	public Component getManipulator() {
		// following string: "reversed"
		Checkbox cb = new Checkbox(res.getString("reversed"), reversed);
		cb.addItemListener(this);
		return cb;
	}

	/**
	* Reacts to changes of the state of the "reversed" checkbox
	*/
	@Override
	public void itemStateChanged(ItemEvent ev) {
		if (ev.getSource() instanceof Checkbox) {
			Checkbox cb = (Checkbox) ev.getSource();
			if (reversed != cb.getState()) {
				reversed = cb.getState();
				notifyScaleChange();
			}
		}
	}

	/**
	 * Pass parameter string to setup color scale
	 */
	@Override
	public void setParameters(String par) {
		if (par == null || par == "")
			return;
		reversed = par.substring(0, 1).equalsIgnoreCase("r");
		notifyScaleChange();
	}

	/**
	 * Pass parameter string to setup color scale
	 */
	@Override
	public String getParameters() {
		return reversed ? "REVERSED" : "NORMAL";
	}
}
