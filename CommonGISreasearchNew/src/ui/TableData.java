package ui;

import spade.vis.database.AttributeDataPortion;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

/**
* Contains data about a table (e.g. selected for data visualization):
* the table itself, its number in the list of available tables, number
* of the corresponding map, the layer manager of this map, and the
* corresponding map layer
*/
public class TableData {
	public int tableN = -1, mapN = -1;
	public AttributeDataPortion table = null;
	public LayerManager lman = null;
	public String layerId = null;
	public GeoLayer themLayer = null;
}