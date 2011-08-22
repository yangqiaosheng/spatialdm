package spade.analysis.plot;

import spade.analysis.system.Supervisor;
import spade.lib.util.DoubleArray;
import spade.vis.action.ObjectEventHandler;

public class DotPlotNonLinear extends DotPlot {
	protected DoubleArray fpAbs = null, fpScr = null; // Focal points: abs values and relative positions

	public void setFp(double abs[], double scr[]) {
		if (abs == null) {
			fpAbs = null;
			fpScr = null;
			return;
		}
		fpAbs = new DoubleArray(abs.length, 1);
		fpScr = new DoubleArray(scr.length, 1);
		for (int i = 0; i < abs.length; i++) {
			fpAbs.addElement(abs[i]);
			fpScr.addElement(scr[i]);
		}
	}

	public DotPlotNonLinear(boolean isHorisontal, boolean isIndependent, boolean allowSelection, Supervisor sup, ObjectEventHandler handler) {
		super(isHorisontal, isIndependent, allowSelection, sup, handler);
	}

	protected static float N = 50;

	protected double abs2scrRight(double val, double x1, double x2) {
		return Math.log(1 + N * (val - x1) / (x2 - x1)) / Math.log(1 + N);
	}

	protected double abs2scr(double abs) {
		if (fpAbs == null || fpAbs.size() == 0 || abs <= min || abs >= max)
			return (abs - min) / (max - min);
		int n = fpAbs.size();
		for (int i = 0; i < fpAbs.size(); i++)
			if (abs < fpAbs.elementAt(i)) {
				n = i;
				break;
			}
		if (n == 0)
			return fpScr.elementAt(0) * abs2scrRight(fpAbs.elementAt(0) + (fpAbs.elementAt(0) - abs), min, fpAbs.elementAt(0));
		if (n == fpAbs.size()) {
			int i = fpAbs.size() - 1;
			return fpScr.elementAt(i) + (1 - fpScr.elementAt(i)) * abs2scrRight(abs, fpAbs.elementAt(i), max);
		}
		// consider a case with a point between 2 focals
		// ...
		return 0;
	}

	@Override
	public int mapX(double v) {
		return bounds.x + mx1 + (int) Math.round(abs2scr(v) * width);
	}

	@Override
	public int mapY(double v) {
		return bounds.y + my1 + height - (int) Math.round(abs2scr(v) * height);
	}

	@Override
	public double absX(int x) {
		return 0; //min+(max-min)*(x-mx1-bounds.x)/width;
	}

	@Override
	public double absY(int y) {
		return 0; //min+(max-min)*(height-y+my1+bounds.y)/height;
	}

}