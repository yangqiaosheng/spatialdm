package spade.analysis.vis3d;

public interface FlatControlListener {
	/*
	*  Interface needed to inform all interested in changes of
	*  X,Z-flat parameters: (distance,fi)
	*  distance from viewer's eye to the object and angle of Y-axis rotation
	*/
	public void flatPositionChanged(EyePosition ep);
}
