//ID
package spade.lib.slider;

public class ScaleConverter extends Object {

	private int smin; //screen minimum coordinate
	private int smax; //screen maximum coordinate
	private float wmin; //world  minimum coordinate
	private float wmax; //world  maximum coordinate
	private float sscale;
	private float wscale;

	public ScaleConverter() {
		smin = 0;
		smax = 0;
		wmin = (float) 0.0;
		wmax = (float) 0.0;
		sscale = (float) 0.0;
		wscale = (float) 0.0;
	}

	public ScaleConverter(int smin, int smax, float wmin, float wmax) {
		Reset(smin, smax, wmin, wmax);
	}

	public void Reset(int smin, int smax, float wmin, float wmax) {
		this.smin = smin;
		this.smax = smax;
		this.wmin = wmin;
		this.wmax = wmax;
		sscale = (smax - smin) / (wmax - wmin);
		wscale = (wmax - wmin) / (smax - smin);
	}

	public void Reset(int smin, int smax) {
		Reset(smin, smax, this.wmin, this.wmax);
	}

	public void Reset(float wmin, float wmax) {
		Reset(this.smin, this.smax, wmin, wmax);
	}

	public float getScreenScale() {
		return sscale;
	}

	public float getWorldScale() {
		return wscale;
	}

	public int toScreen(float v) {
		return smin + (int) ((v - wmin) * sscale);
	}

	public float toWorld(int v) {
		return wmin + (v - smin) * wscale;
	}

}
//~ID