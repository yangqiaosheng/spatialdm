package spade.vis.dmap;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 17-Aug-2007
 * Time: 14:47:55
 * Some map layers may keep links to other map layers. This interface
 * is needed for an adequate generation of map and layer copies: a copy
 * of a layer must be linked with copies of the other layers but not
 * with the original layers.
 */
public interface LinkedToMapLayers {
	/**
	 * Among the given copies of layers, looks for the copies of the
	 * layers this layer is linked to (by their identifiers) and replaces
	 * the references to the original layers by the references to their copies.
	 */
	public void checkAndCorrectLinks(Vector copiesOfMapLayers);
}
