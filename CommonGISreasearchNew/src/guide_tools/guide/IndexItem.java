package guide_tools.guide;

import spade.vis.database.AttributeDataPortion;
import spade.vis.space.GeoLayer;

/**
* IndexItems contain information about links between maps, map layers
* and tables. A GuideSupport gets this information from the system manager
* and stores it in order to find it easier and faster next time
*/
public class IndexItem {
	public int mapN = 0;
	public String layerId = null;
	public GeoLayer layer = null;
	public int tableN = -1;
	public AttributeDataPortion table = null;
}