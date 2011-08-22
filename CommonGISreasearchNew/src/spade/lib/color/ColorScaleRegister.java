package spade.lib.color;

import java.util.ResourceBundle;

import spade.lib.lang.Language;
import spade.lib.util.IntArray;

public class ColorScaleRegister {
	/**
	* The list of known color scales. For each color scale its name and the full
	* class name is specified. A class realizing a type of color scale
	* must implement the interface ColorScale (for example, by extending the
	* abstract class BaseColorScale that implements this interface)
	*/
	static ResourceBundle res = Language.getTextResource("spade.lib.color.Res");
	protected static final String scales[][] = {
	/* following strings:
	* "Spectrum","spade.lib.color.SpectrumColorScale"},
	* {"Diverging (double-ended)","spade.lib.color.DivergingColorScale"},
	* {"Gray scale","spade.lib.color.GrayColorScale"},
	* {"Binary","spade.lib.color.BinaryColorScale"},
	* {"Regions-oriented","spade.lib.color.RegionsColorScale"}*/
	{ res.getString("Spectrum"), "spade.lib.color.SpectrumColorScale" }, { res.getString("Diverging_double"), "spade.lib.color.DivergingColorScale" }, { res.getString("Gray_scale"), "spade.lib.color.GrayColorScale" },
			{ res.getString("Binary"), "spade.lib.color.BinaryColorScale" }, { res.getString("Regions_oriented"), "spade.lib.color.RegionsColorScale" } };
	/**
	* Indices of available scales
	*/
	protected IntArray avail = null;

	/**
	* Checks presence of the classes implementing the color scales
	*/
	protected void checkAvailability() {
		if (avail != null)
			return;
		avail = new IntArray(scales.length, 5);
		for (int i = 0; i < scales.length; i++) {
			try {
				Class cl = Class.forName(scales[i][1]);
				if (cl != null) {
					avail.addElement(i);
				}
			} catch (Exception e) {
			}
		}
	}

	/**
	* Returns the number of available color scales
	*/
	public int getScaleCount() {
		if (avail == null) {
			checkAvailability();
		}
		return avail.size();
	}

	/**
	* Returns the name of the available scale with the given index in the list
	* of AVAILABLE color scales
	*/
	public String getScaleName(int idx) {
		if (idx < 0 || idx >= getScaleCount())
			return null;
		return scales[avail.elementAt(idx)][0];
	}

	/**
	* Returns the class name for the available scale with the given index in the list
	* of AVAILABLE color scales
	*/
	public String getScaleClassName(int idx) {
		if (idx < 0 || idx >= getScaleCount())
			return null;
		return scales[avail.elementAt(idx)][1];
	}

	/**
	* Constructs an instance of the class implementing the color scale with the
	* given index (in the list of AVAILABLE color scales)
	*/
	public ColorScale getColorScale(int idx) {
		if (idx < 0 || idx >= getScaleCount())
			return null;
		try {
			Object obj = Class.forName(scales[avail.elementAt(idx)][1]).newInstance();
			if (obj != null && (obj instanceof ColorScale))
				return (ColorScale) obj;
		} catch (Exception e) {
		}
		return null;
	}
}
