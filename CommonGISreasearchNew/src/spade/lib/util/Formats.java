package spade.lib.util;

public class Formats {
	static int k[] = new int[4];
	static long kd[] = new long[8];

	public static int getInt(byte b[]) {
		for (int i = 0; i < 4; i++) {
			k[i] = b[i];
			if (k[i] < 0) {
				k[i] += 256;
			}
		}
		int i = (k[3] << 24) | (k[2] << 16) | (k[1] << 8) | k[0];
		return i;
	}

	public static int getUnsignedShort(byte b[]) {
		for (int i = 0; i < 2; i++) {
			k[i] = b[i];
			if (k[i] < 0) {
				k[i] += 256;
			}
		}
		int i = (k[1] << 8) | k[0];
		return i;
	}

	public static float getFloat(byte b[]) {
		return Float.intBitsToFloat(getInt(b));
	}

	public static int reverseShort(short n) {
		k[0] = n << 8;
		k[1] = n >> 8;
		return k[0] | k[1];
	}

	public static int reverseInt(int n) {
		k[0] = (n << 24) & 0xFF000000;
		k[1] = (n << 8) & 0x00FF0000;
		k[2] = (n >> 8) & 0x0000FF00;
		k[3] = (n >> 24) & 0x000000FF;
		return (k[0] | k[1] | k[2] | k[3]);
	}

	public static float reverseFloat(float fl) {
		int ifl = Float.floatToIntBits(fl);
		k[0] = (ifl << 24) & 0xFF000000;
		k[1] = (ifl << 8) & 0x00FF0000;
		k[2] = (ifl >> 8) & 0x0000FF00;
		k[3] = (ifl >> 24) & 0x000000FF;
		ifl = k[0] | k[1] | k[2] | k[3];
		return Float.intBitsToFloat(ifl);
	}

	public static float longToFloat(long l) {
		return (float) Double.longBitsToDouble(reverseLong(l));
	}

	public static long reverseLong(long l) {
		kd[0] = (l << 56) & 0xFF00000000000000L;
		kd[1] = (l << 40) & 0x00FF000000000000L;
		kd[2] = (l << 24) & 0x0000FF0000000000L;
		kd[3] = (l << 8) & 0x000000FF00000000L;
		kd[4] = (l >> 8) & 0x00000000FF000000L;
		kd[5] = (l >> 24) & 0x0000000000FF0000L;
		kd[6] = (l >> 40) & 0x000000000000FF00L;
		kd[7] = (l >> 56) & 0x00000000000000FFL;
		l = kd[0] | kd[1] | kd[2] | kd[3] | kd[4] | kd[5] | kd[6] | kd[7];
		return l;
	}

	public static boolean areEqual(float v1, float v2) {
		return Math.abs(v1 - v2) < 0.00001;
	}

	public static boolean areEqual(double v1, double v2) {
		return Math.abs(v1 - v2) < 0.00001;
	}

}
