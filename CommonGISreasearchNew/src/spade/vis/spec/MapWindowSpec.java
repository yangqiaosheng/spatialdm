package spade.vis.spec;

import java.io.Serializable;

import spade.vis.geometry.RealRectangle;

/**
* Used for saving and restoring "snapshots", i.e. system states. Contains
* information about a single tool that is (was) working at the moment of
* state saving.
*/
public class MapWindowSpec extends WinSpec implements Serializable {
	/**
	* The identifier of the window.
	*/
	public String windowId = null;
	/**
	* True for main window, false otherwise.
	*/
	public boolean primary = false;
	/**
	* Map extent in world coordinates.
	*/
	public RealRectangle extent = null;
}