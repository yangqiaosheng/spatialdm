package spade.vis.dataview;

/**
* Used for data record viewing. Allows the record showing component to display
* both original and transformed values.
*/
public interface TransformedDataPresenter {
	/**
	* Returns the transformed value corresponding to the given row and column of
	* the original table. If the value is not transformed, returns null.
	*/
	public String getTransformedValue(int rowN, int colN);
}