package spade.analysis.manipulation;

/**
* Listener to the events of classification of a numeric attribute.
*/

public interface SliderListener {
	/**
	* Results of the classification. Min and Max values are not listed in <breaks>.
	* If there are no breaks, breaks==null.
	* Note that breaks.length is not always equal to the real number of breaks!
	* Use nBreaks!
	*/
	public void breaksChanged(Object source, double[] breaks, int nBreaks);

	/**
	* This function is called during the process of moving a delimiter
	* between classes. <n> shows number of the delimiter (n==0 corresponds
	* to a delimiter between 0th and 1st classes)
	*/
	public void breakIsMoving(Object source, int n, double currValue);

	/**
	* Change of colors
	*/
	public void colorsChanged(Object source);
}