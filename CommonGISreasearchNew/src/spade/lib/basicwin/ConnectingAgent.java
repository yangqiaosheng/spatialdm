package spade.lib.basicwin;

import java.awt.Dimension;

/**
* A ConnectingAgent is used together with ConnectionCanvas. It informs the
* ConnectionCanvas about the connections to be shown.
*/
public interface ConnectingAgent {
	/**
	* Returns the number of connections to be shown by connecting lines in the
	* ConnectionCanvas
	*/
	public int getConnectionCount();

	/**
	* Returns the absolute x-coordinate of the beginning of the connection
	* with the given index. Everywhere absolute screen coordinates are used!
	*/
	public int getConnectionStartX(int idx);

	/**
	* Returns the absolute y-coordinate of the beginning of the connection
	* with the given index. Everywhere absolute screen coordinates are used!
	*/
	public int getConnectionStartY(int idx);

	/**
	* Returns the absolute x-coordinate of the end of the connection
	* with the given index. Everywhere absolute screen coordinates are used!
	*/
	public int getConnectionEndX(int idx);

	/**
	* Returns the absolute y-coordinate of the end of the connection
	* with the given index. Everywhere absolute screen coordinates are used!
	*/
	public int getConnectionEndY(int idx);

	/**
	* Returns the preferred size of the ConnectionCanvas
	*/
	public Dimension getConnectionCanvasPrefSize();
}