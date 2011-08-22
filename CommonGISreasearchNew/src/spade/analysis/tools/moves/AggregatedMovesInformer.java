package spade.analysis.tools.moves;

import java.beans.PropertyChangeListener;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 11-Mar-2008
 * Time: 17:46:19
 */
public interface AggregatedMovesInformer {
	/**
	 * Returns the number of different source locations
	 */
	public int getNofSources();

	/**
	 * Returns the number of different destination locations
	 */
	public int getNofDestinations();

	/**
	 * Returns the name of the source location with the given index
	 */
	public String getSrcName(int souIdx);

	/**
	 * Returns the name of the destination location with the given index
	 */
	public String getDestName(int destIdx);

	/**
	 * Returns the identifier of the source location with the given index
	 */
	public String getSrcId(int souIdx);

	/**
	 * Returns the identifier of the destination location with the given index
	 */
	public String getDestId(int nd);

	/**
	 * Returns the names of the attributes attached to aggregated moves
	 * (e.g. number of moves)
	 */
	public String[] getAttrNames();

	/**
	 * Returns the value of the attribute with the given index for the
	 * given pair of the source and destination.
	 * For movement matrix this should be an instance of Integer!
	 */
	public Object getMatrixValue(int attrIdx, int souIdx, int destIdx);

	/**
	 * Returns the total value (e.g. sum) of the attribute (specified by
	 * its index) computed for all moves originating from
	 * the source location with the given index.
	 * For movement matrix this should be an instance of Integer!
	 */
	public Object getTotalSrcValue(int attrIdx, int souIdx);

	/**
	 * Returns the total value (e.g. sum) of the attribute (specified by
	 * its index) computed for all moves ending in
	 * the destination location with the given index.
	 * For movement matrix this should be an instance of Integer!
	 */
	public Object getTotalDestValue(int attrIdx, int destIdx);

	/**
	 * Returns the maximum value of the attribute with the given index
	 * rounded to an integer value.
	 */
	public int getMaxIntMatrixValue(int attrIdx);

	/**
	 * Returns the maximum of the total values for all sources
	 * (see getTotalSrcValue(...))
	 * @param attrIdx - attribute index
	 */
	public int getMaxIntSrcValue(int attrIdx);

	/**
	 * Returns the maximum of the total values for all destinations
	 * (see getTotalDestValue(...))
	 * @param attrIdx - attribute index
	 */
	public int getMaxIntDestValue(int attrIdx);

	/**
	 * Returns the identifier(s) of the object(s) corresponding to
	 * the source and the destination with the given index which
	 * will be highlighted or selected
	 * @param souIdx - index of the source location
	 * @param destIdx - index of the destination location
	 * @return vector of strings
	 */
	public Vector getObjIDsforSelection(int souIdx, int destIdx);

	/**
	 * Returns the identifier of the set where highlighing/selection will be done
	 * when the user clicks on matrix cells
	 */
	public String getSetIDforSelection();

	/**
	 * Adds a listener of changes of the aggregated movement data
	 */
	public void addPropertyChangeListener(PropertyChangeListener l);

	/**
	 * Removes a listener of changes of the aggregated movement data
	 */
	public void removePropertyChangeListener(PropertyChangeListener l);
}
