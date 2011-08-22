package spade.vis.map;

import java.beans.PropertyChangeListener;

import spade.vis.geometry.RealRectangle;

public interface Zoomable {
//---------------- information functions --------------------
	/**
	* The method returns false if the current scale cannot be increased.
	* This and following methods can be used, for example, to disable or hide
	* the corresponding buttons.
	*/
	public boolean canZoomIn();

	/**
	* The method returns false if the current scale cannot be decreased.
	*/
	public boolean canZoomOut();

	/**
	* The method returns false if the component does not support the "pan"
	* operation, e.g. drawing of the whole territory in the current viewport.
	*/
	public boolean canPan();

	/**
	* The method replies whether the current viewport can be moved in the
	* specified direction.
	* "where" should be one of "North", "South", "East", "West"
	*/
	public boolean canMove(String where);

	/**
	* The method replies whether the last zooming operation can be cancelled.
	*/
	public boolean canUndo();

//------------------- actual zooming-panning functions ------------------
	/**
	* Enlarges the current scale.
	* The component decides by itself by how much it increases the scale.
	*/
	public void zoomIn();

	/**
	* Decreases the current scale.
	* The component decides by itself by how much it decreases the scale.
	*/
	public void zoomOut();

	/**
	* Fits the whole represented territory or image to the size of the viewport.
	*/
	public void pan();

	/**
	* "Moves" the viewport over the shown territory (image etc.) in the
	* specified direction.
	* "where" should be one of "North", "South", "East", "West"
	*/
	public void move(String where);

	/**
	* "Moves" the viewport over the shown territory (image etc.) by
	* the specified horizontal (dx) and vertical (dy) offsets. The offsets
	* are given in screen coordinates.
	*/
	public void move(int dx, int dy);

	/**
	* Enlarges the part of the image specified by the rectangle to the
	* maximum possible scale at which the rectangle still fits in the
	* viewport.
	*/
	public void zoomInRectangle(int x, int y, int width, int height);

	/**
	* Shows the specified territory extent in the map viewport.
	*/
	public void showTerrExtent(float x1, float y1, float x2, float y2);

	/**
	* Shows the specified territory extent in the map viewport.
	*/
	public void setVisibleTerritory(RealRectangle rr);

	/**
	* Increases the scale by the given factor. If the value of "factor" is
	* between 0 and 1, the scale is actually decreased. The "factor"
	* must be a positive number.
	*/
	public void zoomByFactor(float factor);

	/**
	* Cancels the last zooming operation
	*/
	public void undo();

	/**
	* A Zoomable should notify about changes of its scale
	*/
	public void addPropertyChangeListener(PropertyChangeListener l);
}
