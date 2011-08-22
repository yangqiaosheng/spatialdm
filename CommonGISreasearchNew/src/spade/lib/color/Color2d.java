package spade.lib.color;

// no texts to process

//import java.awt.Color;

public class Color2d {
	static public float Hue(int ColorScaleN, int i, int n, boolean reverse) {
		int k = (reverse) ? n - 1 - i : i;
		float hue, h1 = 0, h2 = 0;
		switch (ColorScaleN) {
		case 1: // 1=contrast
			hue = (float) (1.0 / 8.0 + (7.0 / 8.0) * (2 * k + 1.0) / (2 * n));
			return hue;
		case 2: // 2=GreenRed
			h2 = 0.3f;
			h1 = 0.001f;
			break;
		case 3: // 3=RedBlue
			h2 = 0.99f;
			h1 = 0.67f;
			break;
		case 4: // 4=YellowRed
			h2 = 0.167f;
			h1 = 0.001f;
			break;
		case 5: // 5=YellowBlue
			h2 = 0.167f;
			h1 = 0.667f;
			break;
		case 6: // 6=GreenBlue
			h2 = 0.375f;
			h1 = 0.667f;
			break;
		case 7: //  7=YellowMagenta
			h2 = 1.167f;
			h1 = 0.833f;
			break;
		case 8: // 8=CyanBlue
			h2 = 0.5f;
			h1 = 0.667f;
			break;
		}
		hue = h2 + (h1 - h2) * k / (n - 1);
		if (hue > 1) {
			hue -= 1;
		}
		if (hue > 0.998) {
			hue = 0.998f;
		}
		return hue;
	}

	static public float Hue(int ColorScaleN, int i, int n, int j, int m, boolean reverse) {
		j = m - 1 - j;
		return Hue(ColorScaleN, i + j, n + m - 1, reverse);
	}

	static public float Saturation(int i, int n, int j, int m) {
		j = m - 1 - j;
		return (i >= j) ? 1.0f : (float) (1.0 - 0.5 * (j - i) / (Math.max(n, m) - 1.0));
	}

	static public float Brightness(int i, int n, int j, int m) {
		j = m - 1 - j;
		float f = (i > j) ? 0.6f : 0.2f;
		return (float) (0.8 - (f * (i - j)) / Math.max(n, m));
	}
}