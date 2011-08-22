package spade.vis.dataview;

import java.awt.Color;

/**
* Some visualizers are not interested in displaying attributes with null values
* in data popups. Such visualizers must implement this interface, which is
* recognized by the ShowRecManager.
* Additionally, the visualizers may inform about record classes (if exist).
*/
public interface DataViewRegulator {
	/**
	* Replies whether attributes with null values should be shown in data popups.
	*/
	public boolean getShowAttrsWithNullValues();

	/**
	* Returns the class number for the data record with the given index
	*/
	public int getRecordClassN(int recN);

	/**
	* Returns the name of the class with the given number
	*/
	public String getClassName(int classN);

	/**
	* Returns the color of the class with the given number
	*/
	public Color getClassColor(int classN);
}