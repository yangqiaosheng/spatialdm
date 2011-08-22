package spade.lib.color;

import java.awt.Color;

public class CS {
	//to exclude the part of the spectrum with green colors
	protected static float excludeLeft = 0.250f, excludeRight = 0.4f, excludeLength = excludeRight - excludeLeft;

	public static Color niceColors[] = { Color.red, Color.green, Color.blue, Color.yellow, Color.magenta, new Color(255, 128, 0), new Color(255, 0, 128), new Color(128, 0, 255), new Color(0, 128, 255), Color.cyan };

	public static Color getNiceColor(int idx) {
		if (idx < 0)
			return Color.white;
		if (idx >= niceColors.length)
			return Color.black;
		return niceColors[idx];
	}

	public static Color getNiceColorExt(int idx) {
		if (idx < 0)
			return Color.white;
		if (idx < niceColors.length)
			return niceColors[idx];
		idx -= niceColors.length;
		if (idx < niceColors.length)
			return niceColors[idx].darker();
		idx -= niceColors.length;
		if (idx < niceColors.length) {
			float hsb[] = Color.RGBtoHSB(niceColors[idx].getRed(), niceColors[idx].getGreen(), niceColors[idx].getBlue(), null);
			return Color.getHSBColor(hsb[0], hsb[1] * 0.6f, hsb[2]);
		}
		return Color.getHSBColor((float) Math.random(), (float) Math.max(Math.random(), 0.5), (float) Math.max(Math.random(), 0.5));
	}

	public static Color getBWColor(int i, int N) {
		float f = 0.95f * (N - i + 0f) / N;
		return new Color(f, f, f);
	}

	public static Color getBWColor(float r) {
		float f = 0.95f * (1f - r);
		return new Color(f, f, f);
	}

	public static Color getColor(int i, int N, float hue) {
		return getColor(((float) i) / N, hue);
	}

	public static float getNthHue(int i, int N) {
		if (i >= N)
			return 1.0f;
		//exclude the part of the spectrum between excludeLeft and excludeRight
		float val = (1.0f - excludeLength) * i / N;
		if (val <= excludeLeft)
			return val;
		val -= excludeLeft;
		return excludeRight + val;
	}

	public static Color getNthPureColor(int i, int N) {
		if (i >= N)
			return Color.black;
		else
			return Color.getHSBColor(getNthHue(i, N), 1f, 1f);
	}

	public static Color makeColor(int n, float p, int max) {
		if (n >= max)
			return getBWColor(p);
		float hue = getNthHue(n, max);
		return getColor(p, hue);
	}

	/*
	public static Color getColor (float p, float hue) {
	  if (hue>=1.0f) return getBWColor(p);
	  float sat=1.0f, br=1.0f, minS=0.15f, minBr=0.4f;
	  float scale, gap; // length of common color scale, and sat/br gap
	  if (p<0) p=0; if (p>1) p=1;
	  if (hue<0.05){ minS=0.15f; minBr=0.4f;} else
	  if (hue<0.1f){ minS=0.2f; minBr=0.38f;} else
	  if (hue<0.15f){ minS=0.25f; minBr=0.3f;} else
	  if (hue<0.2f){ minS=0.3f; minBr=0.2f;} else
	  if (hue<0.25f){ minS=0.3f; minBr=0.2f;} else
	  if (hue<0.3f){ minS=0.25f; minBr=0.3f;} else
	  if (hue<0.35f){ minS=0.25f; minBr=0.3f;} else
	  if (hue<0.4f){ minS=0.25f; minBr=0.3f;} else
	  if (hue<0.45f){ minS=0.28f; minBr=0.3f;} else
	  if (hue<0.5f){ minS=0.3f; minBr=0.3f;} else
	  if (hue<0.55f){ minS=0.28f; minBr=0.25f;} else
	  if (hue<0.6f){ minS=0.25f; minBr=0.25f;} else
	  if (hue<0.65f){ minS=0.22f; minBr=0.35f;} else
	  if (hue<0.7f){ minS=0.18f; minBr=0.35f;} else
	  if (hue<0.75f){ minS=0.18f; minBr=0.35f;} else
	  if (hue<0.8f){ minS=0.2f; minBr=0.3f;} else
	  if (hue<0.85f){ minS=0.2f; minBr=0.25f;} else
	  if (hue<0.9f){ minS=0.2f; minBr=0.2f;} else
	  if (hue<0.95f){ minS=0.2f; minBr=0.3f;}
	    else { minS=0.18f; minBr=0.35f;}
	  minS+=0.2f; minBr+=0.2f;
	  scale=2-minS-minBr; gap=(1-minS)/scale;
	  if (p<=gap) { sat=minS+(p/gap)*(1-minS); br=1.0f;} else
	    {sat=1.0f; br=minBr+((1-p)/(1-gap))*(1-minBr);}
	  return Color.getHSBColor(hue,sat,br);
	}
	*/
	public static Color getColor(float p, float hue) {
		if (hue >= 1.0f)
			return getBWColor(p);
		if (p < 0) {
			p = 0;
		}
		if (p > 1) {
			p = 1;
		}
		float minS = 0.25f, minBr = 0.35f;
		Color c1 = Color.getHSBColor(hue, minS, 1.0f), c2 = Color.getHSBColor(hue, 1.0f, minBr);
		int r1 = c1.getRed(), g1 = c1.getGreen(), b1 = c1.getBlue(), r = r1 + Math.round(p * (c2.getRed() - r1)), g = g1 + Math.round(p * (c2.getGreen() - g1)), b = b1 + Math.round(p * (c2.getBlue() - b1));
		return new Color(r, g, b);
	}

	public static Color getLegibleColor(float p, float hue) {
		if (hue >= 1.0f)
			return getBWColor(p);
		if (p < 0) {
			p = 0;
		}
		if (p > 1) {
			p = 1;
		}
		float thrSat = (hue < 0.13f || hue > 0.6f) ? 0.5f : 0.35f, thrBri = (hue < 0.13f || hue > 0.6f) ? 0.4f : 0.3f;
		float sat = (p >= thrSat) ? 1f : 0.25f + 0.7f * p / thrSat, bri = (p <= thrBri) ? 1f : 0.4f + 0.55f * (1.0f - p) / (1.0f - thrBri);
		return Color.getHSBColor(hue, sat, bri);
	}

	public static Color makePastel(Color color) {
		if (color == null)
			return null;
		float hsb[] = new float[3];
		Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
		if (hsb[1] < 0.05f)
			return Color.white;
		return getColor(0.2f, hsb[0]);
	}

	public static int getHueN(Color color, int max) {
		if (color == null)
			return max;
		float hsb[] = new float[3];
		Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
		if (hsb[2] < 0.1f || hsb[1] < 0.1f)
			return max;
		return Math.round(hsb[0] * max);
	}

	//-------------------- makeColor --------------------
	/**
	* Calculates the color with degree of darkness proportional to the relative
	* position of value between minv and maxv, depending on the current
	* reference value for comparison
	*/
	public static Color makeColor(float minv, float maxv, float value, float CompareTo, float posHue, float negHue) {
		if (value == CompareTo)
			return Color.white;
		if (value < minv)
			return null;
		if (value > maxv)
			return null;
		minv -= CompareTo;
		maxv -= CompareTo;
		value -= CompareTo;
		float maxMod = Math.abs(maxv);
		if (Math.abs(minv) > maxMod) {
			maxMod = Math.abs(minv);
		}
		float ystart = 0;
		if (minv > 0) {
			ystart = minv;
		} else if (maxv < 0) {
			ystart = -maxv;
		}
		float r = (Math.abs(value) - ystart) / (maxMod - ystart);
		float hue = (value > 0) ? posHue : negHue;
		return CS.getColor(r, hue);
	}

	public static Color makeColor(double minv, double maxv, double value, double CompareTo, float posHue, float negHue) {
		if (value == CompareTo)
			return Color.white;
		if (value < minv)
			return null;
		if (value > maxv)
			return null;
		minv -= CompareTo;
		maxv -= CompareTo;
		value -= CompareTo;
		double maxMod = Math.abs(maxv);
		if (Math.abs(minv) > maxMod) {
			maxMod = Math.abs(minv);
		}
		double ystart = 0;
		if (minv > 0) {
			ystart = minv;
		} else if (maxv < 0) {
			ystart = -maxv;
		}
		float r = (float) ((Math.abs(value) - ystart) / (maxMod - ystart));
		float hue = (value > 0) ? posHue : negHue;
		return CS.getColor(r, hue);
	}

	/**
	* The same as makeColor(float minv, float maxv, float value, float CompareTo,
	* float posHue, float negHue), but CompareTo is assumed to be 0.0f.
	*/
	public static Color makeColor(float minv, float maxv, float value, float posHue, float negHue) {
		return makeColor(minv, maxv, value, 0.0f, posHue, negHue);
	}

	public static Color makeColor(double minv, double maxv, double value, float posHue, float negHue) {
		return makeColor(minv, maxv, value, 0.0f, posHue, negHue);
	}

	/*
	* Returns Hue, Sturation and Brightness of the color
	*/
	public static float getHue(Color c) {
		float hsb[] = new float[3];
		Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsb);
		return hsb[0];
	}

	public static float getSaturation(Color c) {
		float hsb[] = new float[3];
		Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsb);
		return hsb[1];
	}

	public static float getBrightness(Color c) {
		float hsb[] = new float[3];
		Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsb);
		return hsb[2];
	}

	/*
	* returns a complementary color
	*/
	public static Color getComplementaryColor(Color c) {
		float hsb[] = new float[3];
		Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsb);
		float newH = hsb[0] + 0.5f;
		if (newH > 1) {
			newH -= 1f;
		}
		return new Color(Color.HSBtoRGB(newH, hsb[1], hsb[2]));
	}

	/*
	* decreases saturation of color
	*/
	public static Color desaturate(Color c) {
		float hsb[] = new float[3];
		Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsb);
		float newS = hsb[1] * 0.6f;
		return new Color(Color.HSBtoRGB(hsb[0], newS, hsb[2]));
	}

	/*
	* if Java 2 available, adds <alpha> to a given color,
	* overwise returns the original color
	*/
	public static Color getAlphaColor(Color c, int alpha) {
		try {
			Class colorClass = Class.forName("java.awt.Color"), classParms[] = new Class[4];
			for (int p = 0; p < classParms.length; p++) {
				classParms[p] = int.class;
			}
			java.lang.reflect.Constructor constructor = colorClass.getConstructor(classParms);
			if (constructor != null) {
				Object objectParms[] = new Object[4];
				objectParms[0] = new Integer(c.getRed());
				objectParms[1] = new Integer(c.getGreen());
				objectParms[2] = new Integer(c.getBlue());
				objectParms[3] = new Integer(alpha);
				c = (Color) constructor.newInstance(objectParms);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return c;
	}
}