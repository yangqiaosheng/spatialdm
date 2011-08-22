package spade.time.vis;

import java.awt.FontMetrics;
import java.util.Vector;

import spade.lib.basicwin.Metrics;
import spade.time.Date;
import spade.time.TimeMoment;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Mar 8, 2010
 * Time: 11:18:03 AM
 * To change this template use File | Settings | File Templates.
 *
 * This class takes the range of dates and provides positions of major ticks
 * on a timeline of a given size
 */
public class TimeLineDates {

	public static Vector<TimeMoment> getTicks(TimeMoment start, TimeMoment end, int width) {
		FontMetrics fm = Metrics.getFontMetrics();

		long length = end.toNumber() - start.toNumber() + 1, step = 1, l = length;
		int max = width / (10 + fm.stringWidth(end.toString()));
		Vector<TimeMoment> niceTimes = new Vector<TimeMoment>(100, 100);
		TimeMoment st = start.getCopy(), en = end.getCopy();

		if (st instanceof Date && ((Date) st).getPrecisionIdx() > 0) { // Date but not year
			Date dtst = (Date) st, dten = (Date) en;
			boolean fits = false;
			dtst.roundDown();
			dten.roundUp();
			long dt = dten.subtract(dtst);
			int minWidth = (int) dt * (10 + fm.stringWidth(end.toString()));
			fits = minWidth <= width;
			//System.out.println("* prec="+dtst.getPrecision()+", dt="+dt+", fits? "+((fits)?"yes":"no"));
			int prevIdx = -1;
			while (!fits && dtst.getPrecisionIdx() > 1) {
				dtst.setPrecision(Date.time_symbols[dtst.getPrecisionIdx() - 1]);
				while (dtst.getPrecisionIdx() > 1 && !dtst.hasElement(Date.time_symbols[dtst.getPrecisionIdx()])) {
					dtst.setPrecision(Date.time_symbols[dtst.getPrecisionIdx() - 1]);
				}
				if (dtst.getPrecisionIdx() >= 1) {
					dten.setPrecision(Date.time_symbols[dtst.getPrecisionIdx()]);
					dtst.roundDown();
					dten.roundUp();
					if (prevIdx == dtst.getPrecision()) {
						break;
					} else {
						prevIdx = dtst.getPrecision();
					}
					dt = dten.subtract(dtst);
					minWidth = (int) dt * (10 + fm.stringWidth(end.toString()));
					fits = minWidth <= width;
					//System.out.println("* prec="+dtst.getPrecision()+", dt="+dt+", fits? "+((fits)?"yes":"no"));
				}
			}
			//System.out.println("* prec="+dtst.getPrecision()+", dt="+dt+", fits? "+((fits)?"yes":"no"));
			if (fits && minWidth > 0) {
				// check how many may fit
				int nn = (int) Math.ceil(width / minWidth), period = 1;
				int desiredPrecisionIdx;
				char desiredPrecision = Date.time_symbols[0]; // dummy
				if (nn == 1) {
					desiredPrecisionIdx = dtst.getPrecisionIdx();
					while (desiredPrecisionIdx > 1 && !dtst.hasElement(Date.time_symbols[desiredPrecisionIdx])) {
						desiredPrecisionIdx--;
					}
					desiredPrecision = Date.time_symbols[desiredPrecisionIdx];
				} else {
					switch (start.getPrecisionIdx()) {
					case 1: // month;
						if (nn == 2) {
							period = 6;
						} else if (nn <= 4) {
							period = 3;
						} else if (nn <= 11) {
							period = 2;
						}
						break;
					case 2: // day;
						if (nn == 2) {
							period = 15;
						} else if (nn == 3) {
							period = 10;
						} else if (nn <= 15) {
							period = 5;
						} else if (nn <= 30) {
							period = 2;
						}
						break;
					case 3: // hour;
						if (nn <= 3) {
							period = 12;
						} else if (nn <= 7) {
							period = 6;
						} else if (nn <= 11) {
							period = 3;
						} else if (nn <= 23) {
							period = 2;
						}
						break;
					case 4:
					case 5: // hour or minute;
						if (nn < 4) {
							period = 30;
						} else if (nn <= 4) {
							period = 15;
						} else if (nn <= 12) {
							period = 5;
						} else if (nn <= 59) {
							period = 2;
						}
						break;
					}
				}
				//System.out.println("* prec="+dtst.getPrecision()+","+start.getPrecision()+", dt="+dt+", nn="+nn+", period="+period);
				for (Date dtIdx = (Date) start.getCopy(); /*!dtIdx.equals(end.getCopy())*/end.subtract(dtIdx) > 0; dtIdx = (Date) dtIdx.getNext()) {
					if (nn == 1) {
						if (niceTimes.size() == 0) {
							int cmpValue = (dtIdx.getPrecision() == 'd' || dtIdx.getPrecision() == 'm') ? 1 : 0;
							if (dtIdx.getElementValue(dtIdx.getPrecision()) == cmpValue) {
								niceTimes.addElement(dtIdx);
							}
						} else {
							if (dtIdx.subtract(niceTimes.lastElement(), desiredPrecision) > 0) {
								niceTimes.addElement(dtIdx);
							}
						}
					} else {
						int value = dtIdx.getElementValue(dtIdx.getPrecision()), cmp = (dtIdx.getPrecision() == 'm') ? 1 : 0;
						if (value % period == cmp || (value == 1 && (dtIdx.getPrecision() == 'd' || dtIdx.getPrecision() == 'm')))
							if (value == 30 && dtIdx.getPrecision() == 'd') {
								;
							} else {
								niceTimes.addElement(dtIdx);
							}
					}
				}
			}
		}

		if (st instanceof Date && niceTimes.size() == 0) { // round to years
			st.setPrecision('y');
			((Date) st).roundDown();
			en.setPrecision('y');
			((Date) en).roundUp();
			l = en.subtract(st);
		} else { // restore original precision
			st = start.getCopy();
			en = end.getCopy();
		}
		if (!(st instanceof Date) || ((Date) st).getPrecisionIdx() == 0) { // TimeCount or year
			float factors[] = { 2, 2.5f, 2 }; // 2,5,10,20,50,100,...
			int factorIdx = 0;
			while (l > max) {
				float factor = factors[factorIdx];
				factorIdx++;
				if (factorIdx == factors.length) {
					factorIdx = 0;
				}
				l /= factor;
				step *= factor;
			}
			for (TimeMoment tm = st; !tm.equals(en); tm = tm.getNext()) {
				if (tm.toNumber() % step == 0) {
					niceTimes.addElement(tm);
				}
			}
		}

		return niceTimes;
	}

}
