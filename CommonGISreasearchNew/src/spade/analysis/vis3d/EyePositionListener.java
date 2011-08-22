package spade.analysis.vis3d;

public interface EyePositionListener {
	/*
	*  Interface needed to inform all interested in changes of
	*  viewer's EyePosition
	*/

	public void eyePositionChanged(EyePosition ep);
}
