package spade.analysis.tools.schedule;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.tools.moves.AggregatedMovesInformer;
import spade.lib.basicwin.Destroyable;
import spade.lib.lang.Language;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.vis.database.DataTable;
import spade.vis.database.ObjectFilter;
import spade.vis.spec.LinkDataDescription;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 22-Feb-2007
 * Time: 17:50:42
 * To change this template use File | Settings | File Templates.
 */
public class MovementAggregator implements AggregatedMovesInformer, PropertyChangeListener, Destroyable {
	static ResourceBundle res = Language.getTextResource("spade.analysis.tools.schedule.SchedulerTexts_tools_schedule");

	private static String[] aggregationModes = { res.getString("Number_of_trips"), res.getString("Number_of_trips_with_load"), res.getString("Number_of_trips_without_load"), res.getString("Number_of_items"),
			res.getString("Number_of_different_vehicles"), res.getString("Number_of_different_vehicles_with_load") };

	protected DataTable souTbl = null;

	@Override
	public String getSetIDforSelection() {
		return (souTbl == null) ? null : souTbl.getEntitySetIdentifier();
	}

	protected LinkDataDescription ldd = null;
	/**
	 * ObjectFilter associated with the source table.
	 */
	protected ObjectFilter filter = null;

	// vectors of IDs of sources and destinations
	protected Vector srcIDs = null, destIDs = null;
	// arrays of names of sources and destinations
	// if names are not present in the table, these arrays are filled by IDs
	protected String srcNames[] = null, destNames[] = null;
	// table columns with source and destination names etc.
	protected int srcNamesIdx = -1, destNamesIdx = -1;
	protected int itemNumColIdx = -1, vehicleIdColIdx = -1;

	// Pair-Wise Lists of records for sources and destinations
	protected IntArray pwl[][] = null;

	@Override
	public Vector getObjIDsforSelection(int ns, int nd) {
		IntArray ia = pwl[ns][nd];
		if (ia == null || ia.size() == 0)
			return null;
		Vector v = new Vector(ia.size(), 10);
		for (int i = 0; i < ia.size(); i++) {
			v.addElement(souTbl.getDataItemId(ia.elementAt(i)));
		}
		return v;
	}

	// Lists of records for all sources and for all destinations; computed simultaneously with pwl
	protected IntArray sl[] = null, dl[] = null;

	protected int attrIndexes[] = null;

	public int getNAttributes() {
		if (attrIndexes == null)
			return 0;
		return attrIndexes.length;
	}

	public int[] getAttrIndexes() {
		return attrIndexes;
	}

	@Override
	public String[] getAttrNames() {
		if (attrIndexes == null || attrIndexes.length < 1)
			return null;
		String aNames[] = new String[attrIndexes.length];
		for (int i = 0; i < attrIndexes.length; i++)
			if (attrIndexes[i] >= 0 && attrIndexes[i] < aggregationModes.length) {
				aNames[i] = aggregationModes[attrIndexes[i]];
			} else {
				aNames[i] = "unknown name";
			}
		return aNames;
	}

	// matrix of objects representing results of the aggregations
	// later to be replaced by DataTable
	// mao[a][s][d]
	protected Object mao[][][] = null;
	// totals for sources and destinations, sums of max values depending on the meaning
	// ts[a][s], td[a][d]
	protected Object ts[][] = null, td[][] = null;

	@Override
	public int getNofSources() {
		return srcNames.length;
	}

	@Override
	public int getNofDestinations() {
		return destNames.length;
	}

	public String[] getSrcNames() {
		String names[] = new String[getNofSources()];
		for (int i = 0; i < names.length; i++) {
			names[i] = getSrcName(i);
		}
		return names;
	}

	@Override
	public String getSrcName(int ns) {
		if (ns < srcNames.length)
			return srcNames[ns];
		else
			return "";
	}

	@Override
	public String getSrcId(int ns) {
		if (ns < srcIDs.size())
			return (String) srcIDs.elementAt(ns);
		else
			return "";
	}

	public int getSrcIdx(String srcId) {
		if (srcIDs == null || srcId == null)
			return -1;
		return StringUtil.indexOfStringInVectorIgnoreCase(srcId, srcIDs);
	}

	public String[] getDestNames() {
		String names[] = new String[getNofDestinations()];
		for (int i = 0; i < names.length; i++) {
			names[i] = getDestName(i);
		}
		return names;
	}

	@Override
	public String getDestName(int nd) {
		if (nd < destNames.length)
			return destNames[nd];
		else
			return "";
	}

	@Override
	public String getDestId(int nd) {
		if (nd < destIDs.size())
			return (String) destIDs.elementAt(nd);
		else
			return "";
	}

	public int getDestIdx(String destId) {
		if (destIDs == null || destId == null)
			return -1;
		return StringUtil.indexOfStringInVectorIgnoreCase(destId, destIDs);
	}

	@Override
	public Object getMatrixValue(int na, int ns, int nd) {
		return mao[na][ns][nd];
	}

	@Override
	public Object getTotalSrcValue(int na, int ns) {
		return ts[na][ns];
	}

	@Override
	public Object getTotalDestValue(int na, int nd) {
		return td[na][nd];
	}

	@Override
	public int getMaxIntMatrixValue(int na) {
		//if (mao==null || !(mao[na][0][0] instanceof Integer)) return -1;
		int max = -1;
		for (int s = 0; s < mao[na].length; s++) {
			for (int d = 0; d < mao[na][s].length; d++) {
				Object o = mao[na][s][d];
				if (o == null) {
					continue;
				}
				int v = ((Integer) o).intValue();
				if (max == -1 || max < v) {
					max = v;
				}
			}
		}
		/*
		if (max<0) {
		  System.out.println("* Panic!");
		} */
		return max;
	}

	@Override
	public int getMaxIntSrcValue(int na) {
		//if (ts==null || !(ts[na][0] instanceof Integer)) return -1;
		int max = -1;
		for (int s = 0; s < ts[na].length; s++) {
			Object o = ts[na][s];
			if (o == null) {
				continue;
			}
			int v = ((Integer) o).intValue();
			if (max == -1 || max < v) {
				max = v;
			}
		}
		return max;
	}

	@Override
	public int getMaxIntDestValue(int na) {
		//if (td==null || !(td[na][0] instanceof Integer)) return -1;
		int max = -1;
		for (int d = 0; d < td[na].length; d++) {
			Object o = td[na][d];
			if (o == null) {
				continue;
			}
			int v = ((Integer) o).intValue();
			if (max == -1 || max < v) {
				max = v;
			}
		}
		return max;
	}

	public MovementAggregator(DataTable souTbl, LinkDataDescription ldd, int srcNamesIdx, int destNamesIdx, int itemNumColIdx, int vehicleIdColIdx) {
		IntArray aix = new IntArray(aggregationModes.length, 1);
		aix.addElement(0); //"Number of trips"
		this.souTbl = souTbl;
		this.ldd = ldd;
		this.srcNamesIdx = srcNamesIdx;
		this.destNamesIdx = destNamesIdx;
		this.itemNumColIdx = itemNumColIdx;
		if (itemNumColIdx >= 0) {
			aix.addElement(1); //"Number of trips with load"
			aix.addElement(2); //"Number of trips without load"
			aix.addElement(3); //"Number of people"
		}
		this.vehicleIdColIdx = vehicleIdColIdx;
		if (vehicleIdColIdx >= 0) {
			aix.addElement(4); //"Number of different vehicles"
			if (itemNumColIdx >= 0) {
				aix.addElement(5); //"Number of different vehicles with load"
			}
		}
		attrIndexes = aix.getTrimmedArray();
		if (souTbl != null) {
			souTbl.addPropertyChangeListener(this);
			filter = souTbl.getObjectFilter();
			if (filter != null) {
				filter.addPropertyChangeListener(this);
			}
		}
		computeIDlists();
		accountForFilter();
	}

	protected void computeIDlists() {
		srcIDs = souTbl.getAllAttrValuesAsStrings(souTbl.getAttributeId(ldd.souColIdx));
		destIDs = souTbl.getAllAttrValuesAsStrings(souTbl.getAttributeId(ldd.destColIdx));
		if (srcIDs == null || srcIDs.size() == 0 || destIDs == null || destIDs.size() == 0)
			return;
		srcNames = new String[srcIDs.size()];
		for (int i = 0; i < srcNames.length; i++) {
			srcNames[i] = (String) srcIDs.elementAt(i);
		}
		destNames = new String[destIDs.size()];
		for (int i = 0; i < destNames.length; i++) {
			destNames[i] = (String) destIDs.elementAt(i);
		}
		if (srcNamesIdx < 0 || destNamesIdx < 0)
			return;
		for (int i = 0; i < souTbl.getDataItemCount(); i++)
			if (/*filter.isActive(i)*/true) {
				String srcID = (String) souTbl.getAttrValue(ldd.souColIdx, i), destID = (String) souTbl.getAttrValue(ldd.destColIdx, i), srcName = (String) souTbl.getAttrValue(srcNamesIdx, i), destName = (String) souTbl.getAttrValue(destNamesIdx, i);
				if (srcName != null && srcName.length() > 0) {
					int n = srcIDs.indexOf(srcID);
					if (n >= 0) {
						srcNames[n] = srcName;
					}
				}
				if (destName != null && destName.length() > 0) {
					int n = destIDs.indexOf(destID);
					if (n >= 0) {
						destNames[n] = destName;
					}
				}
			}
	}

	protected void accountForFilter() {
		computeListOfPairs();
		computeMatrix();
	}

	protected void computeListOfPairs() {
		int smax = srcNames.length, dmax = destNames.length;
		sl = new IntArray[smax];
		dl = new IntArray[dmax];
		for (int s = 0; s < smax; s++) {
			sl[s] = null;
		}
		for (int d = 0; d < dmax; d++) {
			dl[d] = null;
		}
		pwl = new IntArray[smax][];
		for (int s = 0; s < smax; s++) {
			pwl[s] = new IntArray[dmax];
			for (int d = 0; d < dmax; d++) {
				pwl[s][d] = null;
			}
		}
		for (int r = 0; r < souTbl.getDataItemCount(); r++)
			if (filter.isActive(r)) {
				String srcID = (String) souTbl.getAttrValue(ldd.souColIdx, r), destID = (String) souTbl.getAttrValue(ldd.destColIdx, r);
				int s = srcIDs.indexOf(srcID), d = destIDs.indexOf(destID);
				if (s < 0 || d < 0) {
					continue;
				}
				if (pwl[s][d] == null) {
					pwl[s][d] = new IntArray();
				}
				pwl[s][d].addElement(r);
				if (sl[s] == null) {
					sl[s] = new IntArray();
				}
				sl[s].addElement(r);
				if (dl[d] == null) {
					dl[d] = new IntArray();
				}
				dl[d].addElement(r);
			}
	}

	protected void computeMatrix() {
		int smax = srcNames.length, dmax = destNames.length;
		ts = new Object[getNAttributes()][];
		for (int na = 0; na < getNAttributes(); na++) {
			ts[na] = new Object[smax];
			for (int s = 0; s < smax; s++) {
				if (sl[s] == null) {
					continue;
				}
				IntArray ia = sl[s];
				ts[na][s] = computeIntValue(attrIndexes[na], ia);
			}
		}
		td = new Object[getNAttributes()][];
		for (int na = 0; na < getNAttributes(); na++) {
			td[na] = new Object[dmax];
			for (int d = 0; d < dmax; d++) {
				if (dl[d] == null) {
					continue;
				}
				IntArray ia = dl[d];
				td[na][d] = computeIntValue(attrIndexes[na], ia);
			}
		}
		mao = new Object[getNAttributes()][][];
		for (int na = 0; na < getNAttributes(); na++) {
			mao[na] = new Object[smax][];
			for (int s = 0; s < smax; s++) {
				mao[na][s] = new Object[dmax];
			}
			for (int s = 0; s < smax; s++) {
				for (int d = 0; d < dmax; d++) {
					mao[na][s][d] = null;
					if (pwl[s][d] == null) {
						continue;
					}
					IntArray ia = pwl[s][d];
					mao[na][s][d] = computeIntValue(attrIndexes[na], ia);
				}
			}
		}
	}

	protected Integer computeIntValue(int aIdx, IntArray ia) {
		int result = -1;
		switch (aIdx) {
		case 0: // N of trips
			result = ia.size();
			break;
		case 1: // N of trips with load
			if (itemNumColIdx < 0) {
				break;
			}
			result = 0;
			for (int i = 0; i < ia.size(); i++)
				if (souTbl.getNumericAttrValue(itemNumColIdx, ia.elementAt(i)) > 0) {
					result++;
				}
			break;
		case 2: // N of trips without load
			if (itemNumColIdx < 0) {
				break;
			}
			result = 0;
			for (int i = 0; i < ia.size(); i++)
				if (souTbl.getNumericAttrValue(itemNumColIdx, ia.elementAt(i)) == 0) {
					result++;
				}
			break;
		case 3: // N of people transported
			if (itemNumColIdx < 0) {
				break;
			}
			result = 0;
			for (int i = 0; i < ia.size(); i++) {
				result += souTbl.getNumericAttrValue(itemNumColIdx, ia.elementAt(i));
			}
			break;
		case 4: // N of vehicles
			if (vehicleIdColIdx < 0) {
				break;
			}
			Vector v = new Vector(10, 10);
			for (int i = 0; i < ia.size(); i++) {
				String vehicleID = souTbl.getAttrValueAsString(vehicleIdColIdx, ia.elementAt(i));
				if (v.indexOf(vehicleID) < 0) {
					v.addElement(vehicleID);
				}
			}
			result = v.size();
			break;
		case 5: // N of vehicles with load
			if (vehicleIdColIdx < 0 || itemNumColIdx < 0) {
				break;
			}
			v = new Vector(10, 10);
			for (int i = 0; i < ia.size(); i++)
				if (souTbl.getNumericAttrValue(itemNumColIdx, ia.elementAt(i)) > 0) {
					String vehicleID = souTbl.getAttrValueAsString(vehicleIdColIdx, ia.elementAt(i));
					if (v.indexOf(vehicleID) < 0) {
						v.addElement(vehicleID);
					}
				}
			result = v.size();
			break;
		default:
		}
		return new Integer(result);
	}

//----------------- notification about properties changes---------------
	/**
	 * A MovementAggregator may have a number of listeners of data changes,
	 * e.g. when the data are re-aggregated after filter changes.
	 * To handle the list of listeners and notify them about changes of the
	 * aggregated data, the MovementAggregator uses a PropertyChangeSupport.
	 */
	protected PropertyChangeSupport pcSupport = null;

	/**
	 * Adds a listener of changes of the aggregated data
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
	 * Removes a listener of changes of the aggregated data
	 */
	@Override
	public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
		if (l == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(l);
	}

	/**
	 * Notifies all listeners about a change of the aggregated data
	 */
	public void notifyDataChange() {
		if (pcSupport == null)
			return;
		pcSupport.firePropertyChange("data", null, null);
	}

	public void notifyDestroy() {
		if (pcSupport == null)
			return;
		pcSupport.firePropertyChange("destroy", null, null);
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource().equals(filter)) {
			if (pce.getPropertyName().equals("destroyed")) {
				filter.removePropertyChangeListener(this);
				filter = null;
			} else {
				accountForFilter();
				notifyDataChange();
			}
			return;
		}
		if (pce.getSource().equals(souTbl)) {
			if (pce.getPropertyName().equals("destroyed") || pce.getPropertyName().equals("structure_complete")) {
				destroy();
			} else if (pce.getPropertyName().equals("filter") || pce.getPropertyName().equals("ObjectFilter")) {
				if (filter != null) {
					filter.removePropertyChangeListener(this);
				}
				filter = souTbl.getObjectFilter();
				if (filter != null) {
					filter.addPropertyChangeListener(this);
				}
				accountForFilter();
				notifyDataChange();
			} else if (pce.getPropertyName().equals("data_added") || pce.getPropertyName().equals("data_removed") || pce.getPropertyName().equals("data_updated")) {
				accountForFilter();
				notifyDataChange();
			}
		}
	}

	protected boolean destroyed = false;

	/**
	* Makes necessary operations for destroying, e.g. unregisters from
	* listening highlighting and other events.
	*/
	@Override
	public void destroy() {
		if (filter != null) {
			filter.removePropertyChangeListener(this);
			filter = null;
		}
		if (souTbl != null) {
			souTbl.removePropertyChangeListener(this);
			souTbl = null;
		}
		destroyed = true;
		notifyDestroy();
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

}
