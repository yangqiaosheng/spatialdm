package spade.analysis.manipulation;

import java.awt.BorderLayout;
import java.awt.Label;
import java.awt.Panel;
import java.util.ResourceBundle;

import spade.lib.basicwin.FocusListener;
import spade.lib.basicwin.Focuser;
import spade.lib.basicwin.FocuserCanvas;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.PopupManager;
import spade.lib.lang.Language;
import spade.vis.mapvis.UtilitySignDrawer;

/**
* This is a manipulator for "utility signs" map visualisation method.
* It allows the user to select interactively some utility interval and removes
* from the map signs of objects with utility lying outside this interval.
* The utility is estimated as a total area of all sign elements and measured
* in percents to the maximum possible area (i.e. the area of the sign that
* would be drawn for a hypothetical "ideal" object with the best values of all
* of the attributes).
*/
public class UtilitySignsFilter extends Panel implements FocusListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.manipulation.Res");

	UtilitySignDrawer usd = null;
	Focuser f = null;
	Label l = null;
	// following text: "Show signs: "
	String txt1 = res.getString("Show_signs_"),
	// following text: "% of maximum area"
			txt2 = res.getString("_of_maximum_area");
	int min = 0, max = 100;

	public UtilitySignsFilter(UtilitySignDrawer usd) {
		this.usd = usd;
		setLayout(new BorderLayout());
		add(l = new Label(txt1 + min + ".." + max + txt2), "North");
		// following text: "The system allows to restrict the number of signs shown \nby the total area of the sign.\nUse the focuser for this purpose."
		String str = res.getString("The_system_allows_to");
		new PopupManager(l, str, true);
		f = new Focuser();
		f.addFocusListener(this);
//ID
//    f.setAbsMinMax(0f,100f);
		f.setMinMax(0f, 100f, usd.getDrawingLimitMin() * 100, usd.getDrawingLimitMax() * 100);
//~ID
		f.setIsVertical(false);
		f.setIsUsedForQuery(true);
		FocuserCanvas fc = new FocuserCanvas(f, false);
		add(fc, "Center");
		new PopupManager(fc, str, true);
		add(new Line(false), "South");
	}

	@Override
	public void focusChanged(Object source, double lowerLimit, double upperLimit) {
		min = (int) Math.round(lowerLimit);
		max = (int) Math.round(upperLimit);
		visChange();
	}

	@Override
	public void limitIsMoving(Object source, int n, double currValue) { // n==0 -> min, n==1 -> max
		int m = (int) Math.round(currValue);
		if (n == 0) {
			min = m;
		} else {
			max = m;
		}
		visChange();
	}

	protected void visChange() {
		l.setText(txt1 + min + ".." + max + txt2);
		usd.setDrawingLimits(min / 100f, max / 100f);
		usd.notifyVisChange();
	}
}
