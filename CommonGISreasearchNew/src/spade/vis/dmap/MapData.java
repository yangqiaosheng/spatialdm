package spade.vis.dmap;

import java.awt.Color;
import java.util.Vector;

public class MapData {
	/**
	* Name of the territory - should be got from the map description
	*/
	public String terrName = null;
	/**
	* The territory extent to be initially visible in the map view. May be null.
	* In this case the whole territory is shown.
	* The extent is an array of 4 float numbers:
	* 0) x1; 1) y1; 2) x2; 3) y2
	*/
	public float extent[] = null;
	/**
	* User-defined scaling factor - should be got from the map description
	*/
	public float user_factor = 1.0f;
	/**
	* User-defined unit in which coordinates are specified - should be got from
	* the map description
	*/
	public String user_unit = "m";
	/**
	* Global background color for map
	*/
	public Color bgColor = Color.lightGray;
	/**
	* Vector of layers
	*/
	public Vector layers;

	public void addLayer(Object layer) {
		if (layer == null)
			return;
		if (layers == null) {
			layers = new Vector(10, 10);
		}
		layers.addElement(layer);
	}
}