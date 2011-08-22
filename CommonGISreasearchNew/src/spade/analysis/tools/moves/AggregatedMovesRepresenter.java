package spade.analysis.tools.moves;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Vector;

import spade.lib.basicwin.Destroyable;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataTable;
import spade.vis.dmap.DAggregateLinkLayer;
import spade.vis.dmap.DAggregateLinkObject;
import spade.vis.dmap.DGeoObject;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 12-Mar-2008
 * Time: 10:23:25
 * Serves as an interface between Movement Matrix and a layer with aggregated
 * moves (DAggregateLinkLayer).
 */
public class AggregatedMovesRepresenter implements AggregatedMovesInformer, PropertyChangeListener, Destroyable {
	/**
	 * The layer with aggregated moves represented by this object
	 */
	public DAggregateLinkLayer aggLayer = null;
	/**
	 * The table associated with aggLayer
	 */
	public DataTable aggTable = null;
	/**
	 * All source locations extracted from the layer. Each location is
	 * an instance of DGeoObject.
	 */
	protected Vector sources = null;
	/**
	 * All destrination locations extracted from the layer. Each location is
	 * an instance of DGeoObject.
	 */
	protected Vector destinations = null;
	/**
	 * A matrix containing indexes of the objects in the aggLayer
	 * (instances of DAggregateLinkObject) for pairs of source and destination
	 */
	protected int links[][] = null;
	/**
	 * The numbers of the columns with numeric attributes in the table
	 * associated with the aggLayer.
	 */
	protected IntArray numColNs = null;

	/**
	 * @param aggLayer - the layer with aggregated moves to be represented by this object
	 */
	public AggregatedMovesRepresenter(DAggregateLinkLayer aggLayer) {
		this.aggLayer = aggLayer;
		if (aggLayer == null)
			return;
		aggTable = (DataTable) aggLayer.getThematicData();
		extractSources();
		if (sources == null)
			return;
		extractDestinations();
		if (destinations == null)
			return;
		constructLinkMatrix();
		findNumericAttributes();
		if (numColNs != null) {
			aggTable.addPropertyChangeListener(this);
		} else {
			aggLayer.addPropertyChangeListener(this);
		}
	}

	/**
	 * Extracts all source locations of the aggregated moved included in the layer.
	 */
	protected void extractSources() {
		if (sources != null && sources.size() > 0)
			return;
		if (aggLayer == null || aggLayer.getObjectCount() < 1)
			return;
		sources = new Vector(aggLayer.getObjectCount(), 10);
		for (int i = 0; i < aggLayer.getObjectCount(); i++) {
			DAggregateLinkObject aggObj = (DAggregateLinkObject) aggLayer.getObject(i);
			if (aggObj.startNode != null && !sources.contains(aggObj.startNode)) {
				sources.addElement(aggObj.startNode);
			}
		}
		if (sources.size() < 1) {
			sources = null;
		}
	}

	/**
	 * Extracts all destination locations of the aggregated moved included in the layer.
	 */
	protected void extractDestinations() {
		if (destinations != null && destinations.size() > 0)
			return;
		if (aggLayer == null || aggLayer.getObjectCount() < 1)
			return;
		destinations = new Vector(aggLayer.getObjectCount(), 10);
		for (int i = 0; i < aggLayer.getObjectCount(); i++) {
			DAggregateLinkObject aggObj = (DAggregateLinkObject) aggLayer.getObject(i);
			if (aggObj.endNode != null && !destinations.contains(aggObj.endNode)) {
				destinations.addElement(aggObj.endNode);
			}
		}
		if (destinations.size() < 1) {
			destinations = null;
		}
	}

	/**
	 * Constructs the matrix containing indexes of objects in the aggLayer
	 * (instances of DAggregateLinkObject) for pairs of source and destination
	 */
	protected void constructLinkMatrix() {
		if (sources == null || destinations == null)
			return;
		links = new int[sources.size()][destinations.size()];
		for (int i = 0; i < sources.size(); i++) {
			for (int j = 0; j < destinations.size(); j++) {
				links[i][j] = -1;
			}
		}
		for (int i = 0; i < aggLayer.getObjectCount(); i++) {
			DAggregateLinkObject aggObj = (DAggregateLinkObject) aggLayer.getObject(i);
			if (aggObj.startNode == null || aggObj.endNode == null) {
				continue;
			}
			int sIdx = sources.indexOf(aggObj.startNode);
			if (sIdx < 0) {
				continue;
			}
			int dIdx = destinations.indexOf(aggObj.endNode);
			if (dIdx < 0) {
				continue;
			}
			links[sIdx][dIdx] = i;
		}
	}

	/**
	 * In the table associated with the aggregated moves layer (aggLayer)
	 * finds the numbers of columns with numeric (moreover, integer) attributes
	 * and stores them in numColNs.
	 */
	protected void findNumericAttributes() {
		if (aggTable == null || !aggTable.hasData())
			return;
		numColNs = new IntArray(aggTable.getAttrCount(), 10);
		for (int i = 0; i < aggTable.getAttrCount(); i++)
			if (aggTable.getAttributeType(i) == AttributeTypes.integer) {
				numColNs.addElement(i);
			}
		if (numColNs.size() < 1) {
			numColNs = null;
		}
	}

	/**
	 * Returns the number of different source locations
	 */
	@Override
	public int getNofSources() {
		if (sources == null)
			return 0;
		return sources.size();
	}

	/**
	 * Returns the number of different destination locations
	 */
	@Override
	public int getNofDestinations() {
		if (destinations == null)
			return 0;
		return destinations.size();
	}

	/**
	 * Returns the object representing the source location with the given index
	 */
	public DGeoObject getSource(int souIdx) {
		if (sources == null || souIdx < 0 || souIdx >= sources.size())
			return null;
		return (DGeoObject) sources.elementAt(souIdx);
	}

	/**
	 * Returns the object representing the destination location with the given index
	 */
	public DGeoObject getDestination(int destIdx) {
		if (destinations == null || destIdx < 0 || destIdx >= destinations.size())
			return null;
		return (DGeoObject) destinations.elementAt(destIdx);
	}

	/**
	 * Returns the name of the source location with the given index
	 */
	@Override
	public String getSrcName(int souIdx) {
		DGeoObject place = getSource(souIdx);
		if (place == null)
			return null;
		return place.getName();
	}

	/**
	 * Returns the identifier of the source location with the given index
	 */
	@Override
	public String getSrcId(int souIdx) {
		DGeoObject place = getSource(souIdx);
		if (place == null)
			return null;
		return place.getIdentifier();
	}

	/**
	 * Returns the name of the destination location with the given index
	 */
	@Override
	public String getDestName(int destIdx) {
		DGeoObject place = getDestination(destIdx);
		if (place == null)
			return null;
		return place.getName();
	}

	/**
	 * Returns the identifier of the destination location with the given index
	 */
	@Override
	public String getDestId(int destIdx) {
		DGeoObject place = getDestination(destIdx);
		if (place == null)
			return null;
		return place.getIdentifier();
	}

	/**
	 * Returns the names of the attributes attached to aggregated moves
	 * (e.g. number of moves)
	 */
	@Override
	public String[] getAttrNames() {
		if (numColNs == null) {
			String names[] = { "N of all moves", "N of active moves" };
			return names;
		}
		String names[] = new String[numColNs.size()];
		for (int i = 0; i < numColNs.size(); i++) {
			names[i] = aggTable.getAttributeName(numColNs.elementAt(i));
		}
		return names;
	}

	/**
	 * Returns the value of the attribute with the given index for the
	 * given pair of the source and destination.
	 * For movement matrix this should be an instance of Integer!
	 */
	@Override
	public Object getMatrixValue(int attrIdx, int souIdx, int destIdx) {
		if (links == null || attrIdx < 0 || souIdx < 0 || souIdx >= sources.size() || destIdx < 0 || destIdx >= destinations.size())
			return null;
		if (numColNs == null)
			if (attrIdx >= 2)
				return null;
			else {
				;
			}
		else if (attrIdx >= numColNs.size())
			return null;
		int oIdx = links[souIdx][destIdx];
		if (oIdx < 0)
			return null;
		DAggregateLinkObject aggObj = (DAggregateLinkObject) aggLayer.getObject(oIdx);
		if (numColNs == null) {
			if (attrIdx == 0)
				if (aggObj.souLinks == null)
					return new Integer(0);
				else
					return new Integer(aggObj.souLinks.size());
			if (attrIdx == 1)
				return new Integer(aggObj.nActiveLinks);
		}
		if (aggObj.getData() == null)
			return null;
		double val = aggObj.getData().getNumericAttrValue(numColNs.elementAt(attrIdx));
		if (Double.isNaN(val))
			return null;
		return new Integer((int) Math.round(val));
	}

	/**
	 * Returns the total value (e.g. sum) of the attribute (specified by
	 * its index) computed for all moves originating from
	 * the source location with the given index.
	 * For movement matrix this should be an instance of Integer!
	 */
	@Override
	public Object getTotalSrcValue(int attrIdx, int souIdx) {
		if (links == null || attrIdx < 0 || souIdx < 0 || souIdx >= sources.size())
			return null;
		if (numColNs == null)
			if (attrIdx >= 2)
				return null;
			else {
				;
			}
		else if (attrIdx >= numColNs.size())
			return null;
		int sum = 0;
		for (int i = 0; i < destinations.size(); i++)
			if (links[souIdx][i] >= 0) {
				DAggregateLinkObject aggObj = (DAggregateLinkObject) aggLayer.getObject(links[souIdx][i]);
				if (numColNs == null) {
					if (attrIdx == 0)
						if (aggObj.souLinks != null) {
							sum += aggObj.souLinks.size();
						} else {
							;
						}
					else if (attrIdx == 1) {
						sum += aggObj.nActiveLinks;
					}
				} else {
					if (aggObj.getData() == null) {
						continue;
					}
					double val = aggObj.getData().getNumericAttrValue(numColNs.elementAt(attrIdx));
					if (!Double.isNaN(val)) {
						sum += (int) Math.round(val);
					}
				}
			}
		return new Integer(sum);
	}

	/**
	 * Returns the total value (e.g. sum) of the attribute (specified by
	 * its index) computed for all moves ending in
	 * the destination location with the given index.
	 * For movement matrix this should be an instance of Integer!
	 */
	@Override
	public Object getTotalDestValue(int attrIdx, int destIdx) {
		if (links == null || attrIdx < 0 || destIdx < 0 || destIdx >= destinations.size())
			return null;
		if (numColNs == null)
			if (attrIdx >= 2)
				return null;
			else {
				;
			}
		else if (attrIdx >= numColNs.size())
			return null;
		int sum = 0;
		for (int i = 0; i < sources.size(); i++)
			if (links[i][destIdx] >= 0) {
				DAggregateLinkObject aggObj = (DAggregateLinkObject) aggLayer.getObject(links[i][destIdx]);
				if (numColNs == null) {
					if (attrIdx == 0)
						if (aggObj.souLinks != null) {
							sum += aggObj.souLinks.size();
						} else {
							;
						}
					else if (attrIdx == 1) {
						sum += aggObj.nActiveLinks;
					}
				} else {
					if (aggObj.getData() == null) {
						continue;
					}
					double val = aggObj.getData().getNumericAttrValue(numColNs.elementAt(attrIdx));
					if (!Double.isNaN(val)) {
						sum += (int) Math.round(val);
					}
				}
			}
		return new Integer(sum);
	}

	/**
	 * Returns the maximum value of the attribute with the given index
	 * rounded to an integer value.
	 */
	@Override
	public int getMaxIntMatrixValue(int attrIdx) {
		if (links == null || attrIdx < 0)
			return 0;
		if (numColNs == null)
			if (attrIdx >= 2)
				return 0;
			else {
				;
			}
		else if (attrIdx >= numColNs.size())
			return 0;
		int max = Integer.MIN_VALUE;
		for (int i = 0; i < sources.size(); i++) {
			for (int j = 0; j < destinations.size(); j++)
				if (links[i][j] >= 0) {
					DAggregateLinkObject aggObj = (DAggregateLinkObject) aggLayer.getObject(links[i][j]);
					int value = Integer.MIN_VALUE;
					if (numColNs == null) {
						if (attrIdx == 0)
							if (aggObj.souLinks != null) {
								value = aggObj.souLinks.size();
							} else {
								;
							}
						else if (attrIdx == 1) {
							value = aggObj.nActiveLinks;
						}
					} else {
						if (aggObj.getData() == null) {
							continue;
						}
						double val = aggObj.getData().getNumericAttrValue(numColNs.elementAt(attrIdx));
						if (!Double.isNaN(val)) {
							value = (int) Math.round(val);
						}
					}
					if (max < value) {
						max = value;
					}
				}
		}
		return max;
	}

	/**
	 * Returns the maximum of the total values for all sources
	 * (see getTotalSrcValue(...))
	 * @param attrIdx - attribute index
	 */
	@Override
	public int getMaxIntSrcValue(int attrIdx) {
		if (links == null || attrIdx < 0)
			return 0;
		if (numColNs == null)
			if (attrIdx >= 2)
				return 0;
			else {
				;
			}
		else if (attrIdx >= numColNs.size())
			return 0;
		int max = Integer.MIN_VALUE;
		for (int souIdx = 0; souIdx < sources.size(); souIdx++) {
			int sum = 0;
			for (int i = 0; i < destinations.size(); i++)
				if (links[souIdx][i] >= 0) {
					DAggregateLinkObject aggObj = (DAggregateLinkObject) aggLayer.getObject(links[souIdx][i]);
					if (numColNs == null) {
						if (attrIdx == 0)
							if (aggObj.souLinks != null) {
								sum += aggObj.souLinks.size();
							} else {
								;
							}
						else if (attrIdx == 1) {
							sum += aggObj.nActiveLinks;
						}
					} else {
						if (aggObj.getData() == null) {
							continue;
						}
						double val = aggObj.getData().getNumericAttrValue(numColNs.elementAt(attrIdx));
						if (!Double.isNaN(val)) {
							sum += (int) Math.round(val);
						}
					}
				}
			if (max < sum) {
				max = sum;
			}
		}
		return max;
	}

	/**
	 * Returns the maximum of the total values for all destinations
	 * (see getTotalDestValue(...))
	 * @param attrIdx - attribute index
	 */
	@Override
	public int getMaxIntDestValue(int attrIdx) {
		if (links == null || attrIdx < 0)
			return 0;
		if (numColNs == null)
			if (attrIdx >= 2)
				return 0;
			else {
				;
			}
		else if (attrIdx >= numColNs.size())
			return 0;
		int max = Integer.MIN_VALUE;
		for (int destIdx = 0; destIdx < destinations.size(); destIdx++) {
			int sum = 0;
			for (int i = 0; i < sources.size(); i++)
				if (links[i][destIdx] >= 0) {
					DAggregateLinkObject aggObj = (DAggregateLinkObject) aggLayer.getObject(links[i][destIdx]);
					if (numColNs == null) {
						if (attrIdx == 0)
							if (aggObj.souLinks != null) {
								sum += aggObj.souLinks.size();
							} else {
								;
							}
						else if (attrIdx == 1) {
							sum += aggObj.nActiveLinks;
						}
					} else {
						if (aggObj.getData() == null) {
							continue;
						}
						double val = aggObj.getData().getNumericAttrValue(numColNs.elementAt(attrIdx));
						if (!Double.isNaN(val)) {
							sum += (int) Math.round(val);
						}
					}
				}
			if (max < sum) {
				max = sum;
			}
		}
		return max;
	}

	public static final int selectNone = 0, selectSouMoves = 1, selectAggrMoves = 2, selectPlaces = 3;
	/**
	 * Indicates whether highlighting/selection will be applied to the source
	 * trajectory layer, to the layer with aggregated links, or to the layer
	 * with the sources and destinations
	 */
	public int whatToSelect = selectAggrMoves;

	/**
	 * Returns the identifier(s) of the object(s) corresponding to
	 * the source and the destination with the given index which
	 * will be highlighted or selected
	 * @param souIdx - index of the source location
	 * @param destIdx - index of the destination location
	 * @return vector of strings
	 */
	@Override
	public Vector getObjIDsforSelection(int souIdx, int destIdx) {
		if (whatToSelect == selectNone)
			return null;
		if (links == null || souIdx < 0 || souIdx >= sources.size() || destIdx < 0 || destIdx >= destinations.size())
			return null;
		int oIdx = links[souIdx][destIdx];
		if (oIdx < 0)
			return null;
		DAggregateLinkObject aggObj = (DAggregateLinkObject) aggLayer.getObject(oIdx);
		if (whatToSelect == selectAggrMoves) {
			Vector v = new Vector(1, 1);
			v.addElement(aggObj.getIdentifier());
			return v;
		}
		if (whatToSelect == selectPlaces) {
			Vector v = new Vector(2, 1);
			if (aggObj.startNode != null) {
				v.addElement(aggObj.startNode.getIdentifier());
			}
			if (aggObj.endNode != null) {
				v.addElement(aggObj.endNode.getIdentifier());
			}
			if (v.size() > 0)
				return v;
			return null;
		}
		if (aggObj.souTrajIds == null || aggObj.souTrajIds.size() < 1)
			return null;
		Vector activeTrIds = aggLayer.getActiveTrajIds();
		if (activeTrIds == null || activeTrIds.size() < 1)
			return aggObj.souTrajIds;
		Vector ids = new Vector(aggObj.souTrajIds.size(), 1);
		for (int i = 0; i < aggObj.souTrajIds.size(); i++)
			if (StringUtil.isStringInVectorIgnoreCase((String) aggObj.souTrajIds.elementAt(i), activeTrIds)) {
				ids.addElement(aggObj.souTrajIds.elementAt(i));
			}
		if (ids.size() < 1)
			return null;
		return ids;
	}

	/**
	 * Returns the identifier of the set where highlighing/selection will be done
	 * when the user clicks on matrix cells
	 */
	@Override
	public String getSetIDforSelection() {
		if (aggLayer == null)
			return null;
		if (aggLayer.getIsActive()) {
			whatToSelect = selectAggrMoves;
		} else if (aggLayer.getPlaceLayer() != null && aggLayer.getPlaceLayer().getIsActive()) {
			whatToSelect = selectPlaces;
		} else if (aggLayer.getTrajectoryLayer() != null && aggLayer.getTrajectoryLayer().getIsActive()) {
			whatToSelect = selectSouMoves;
		} else {
			whatToSelect = selectNone;
		}
		if (whatToSelect == selectNone)
			return null;
		if (whatToSelect == selectAggrMoves)
			return aggLayer.getEntitySetIdentifier();
		if (whatToSelect == selectPlaces)
			return aggLayer.getPlaceLayer().getEntitySetIdentifier();
		return aggLayer.getTrajectoryLayer().getEntitySetIdentifier();
	}

	/**
	 * Helps to handle the list of property change listeners and notify them about
	 * changes of the data.
	 */
	protected PropertyChangeSupport pcSupport = null;

	/**
	 * Adds a listener of changes of the aggregated movement data
	 */
	@Override
	public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
		if (l == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(l);
	}

	/**
	 * Removes a listener of changes of the aggregated movement data
	 */
	@Override
	public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
		if (l == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(l);
	}

	/**
	 * Notifies the property change listeners about changes of the data.
	 */
	public void notifyPropertyChange(String propName, Object oldValue, Object newValue) {
		if (pcSupport == null)
			return;
		pcSupport.firePropertyChange(propName, oldValue, newValue);
	}

	/**
	 * Reacts to property changes from the aggLayer or aggTable
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(aggLayer)) {
			if (e.getPropertyName().equalsIgnoreCase("ObjectData") || e.getPropertyName().equalsIgnoreCase("VisParameters")) {
				notifyPropertyChange("data", null, null);
			} else if (e.getPropertyName().equalsIgnoreCase("destroy") || e.getPropertyName().equalsIgnoreCase("destroyed")) {
				notifyPropertyChange("destroy", null, null);
			}
		} else if (e.getSource().equals(aggTable)) {
			if (e.getPropertyName().equalsIgnoreCase("values")) {
				notifyPropertyChange("data", null, null);
			} else if (e.getPropertyName().equalsIgnoreCase("destroy") || e.getPropertyName().equalsIgnoreCase("destroyed")) {
				notifyPropertyChange("destroy", null, null);
			}
		}
	}

	protected boolean destroyed = false;

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	@Override
	public void destroy() {
		if (numColNs != null) {
			aggTable.removePropertyChangeListener(this);
		} else {
			aggLayer.removePropertyChangeListener(this);
		}
		destroyed = true;
	}
}
