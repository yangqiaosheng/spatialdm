package spade.vis.spec;

import java.awt.Rectangle;
import java.io.Serializable;
import java.util.Hashtable;

/**
* Used for saving states of various tools which appear in individual windows.
*/
public class WinSpec implements Serializable {
	/**
	* The tag to be used for storing this specification in a file. Every
	* specification is stored as a sequence of lines starting with <tagName>
	* and ending with </tagName>.
	*/
	public String tagName = null;
	/**
	* The title of the window.
	*/
	public String title = null;
	/**
	* Window location and size in screen coordinates.
	*/
	public Rectangle bounds = null;
	/**
	* Custom properties of the tool.
	* String -> String
	*/
	public Hashtable properties = null;
}