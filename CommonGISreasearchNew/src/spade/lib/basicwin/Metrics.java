package spade.lib.basicwin;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Toolkit;

public class Metrics {
	protected static int millimeter = 0;
	protected static float centimeter = 0;
	protected static Dimension ss = null;
	protected static FontMetrics fmetr = null;
	public static int fh = 0, asc = 0;

	protected static void getScreenSize() {
		if (ss != null)
			return;
		Toolkit tk = null;
		try {
			tk = Toolkit.getDefaultToolkit();
		} catch (Exception e) {
		}
		if (tk != null) {
			ss = tk.getScreenSize();
		}
	}

	public static int scrW() {
		if (ss == null) {
			getScreenSize();
		}
		if (ss != null)
			return ss.width;
		return 0;
	}

	public static int scrH() {
		if (ss == null) {
			getScreenSize();
		}
		if (ss != null)
			return ss.height;
		return 0;
	}

	public static int mm() {
		if (millimeter > 0)
			return millimeter;
		Toolkit tk = null;
		try {
			tk = Toolkit.getDefaultToolkit();
		} catch (Exception e) {
		}
		if (tk == null)
			return 0;
		millimeter = Math.round(tk.getScreenResolution() / 25.33f);
		return millimeter;
	}

	public static float cm() {
		if (centimeter > 0)
			return centimeter;
		Toolkit tk = null;
		try {
			tk = Toolkit.getDefaultToolkit();
		} catch (Exception e) {
		}
		if (tk == null)
			return 0;
		centimeter = tk.getScreenResolution() / 2.533f;
		return centimeter;
	}

	public static void setFontMetrics(FontMetrics fm) {
		fmetr = fm;
		if (fmetr != null) {
			fh = fmetr.getHeight();
			asc = fmetr.getAscent();
			System.out.println("Font metrics defined; height=" + fh);
		}
	}

	public static void setFontMetrics(Graphics g) {
		if (g != null) {
			setFontMetrics(g.getFontMetrics());
		}
	}

	public static FontMetrics getFontMetrics() {
		return fmetr;
	}

	public static int stringWidth(String str) {
		if (str == null || fmetr == null)
			return 0;
		return fmetr.stringWidth(str);
	}
}