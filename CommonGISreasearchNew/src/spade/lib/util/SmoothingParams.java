package spade.lib.util;

/**
* Parameters for temporal aggregation (in particular, smoothing) of
* time-dependent numeric data
*/
public class SmoothingParams {
	/**
	* Possible modes of temporal aggregation/smoothing:
	*/
	public static final int SmoothNONE = 0, SmoothAVG = 1, SmoothMEDIAN = 2, SmoothMAX = 3, SmoothMIN = 4, SmoothMAXMIN = 5, SmoothSUM = 6, SmoothLAST = SmoothSUM;

	/**
	* The same modes as strings (to be used for saving and restoring states)
	*/
	public static final String smoothModeNames[] = { "none", "mean", "median", "max", "min", "max-min", "sum" };

	public int smoothMode = SmoothNONE, smoothDepth = 3, smoothStartIdx = -1;

	public boolean smoothDifference = false, smoothCentered = true;

	public void setSmoothMode(int smoothMode, int smoothDepth, boolean smoothDifference, boolean smoothCentered) {
		this.smoothMode = smoothMode;
		this.smoothDepth = smoothDepth;
		this.smoothStartIdx = -1;
		this.smoothDifference = smoothDifference;
		this.smoothCentered = smoothCentered;
	}

	public void setSmoothMode(int smoothMode, int startMomentIdx, boolean smoothDifference) {
		this.smoothMode = smoothMode;
		this.smoothDifference = smoothDifference;
		smoothStartIdx = startMomentIdx;
		if (smoothStartIdx >= 0) {
			this.smoothDepth = 0;
		}
	}

	public void setSmoothMode(int smoothMode) {
		this.smoothMode = smoothMode;
	}

}