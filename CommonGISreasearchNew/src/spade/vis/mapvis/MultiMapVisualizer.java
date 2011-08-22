package spade.vis.mapvis;

/**
* This is an interface for visualization methods used for realizing "small
* multiples", i.e. several maps in a single panel using the same representation
* symbolics. In particular, these maps may refer to different time moments.
*/
public interface MultiMapVisualizer extends Visualizer {
	/**
	* Returns the required number of maps ("small multiples"), depending on the
	* number of attributes to be represented.
	*/
	public int getNIndMaps();

	/**
	* Returns the name of the map with the given index in the "small multiples".
	* This may be, for example, the name of the attribute represented by this map
	* or the date this map refers to.
	*/
	public String getIndMapName(int idx);

	/**
	* Sets the number of the current individual map. Since this moment all
	* calls of the "getPresentation" method will relate to this map.
	*/
	public void setCurrentMapIndex(int idx);

	/**
	* Returns the number of the current individual map.
	*/
	public int getCurrentMapIndex();
}
