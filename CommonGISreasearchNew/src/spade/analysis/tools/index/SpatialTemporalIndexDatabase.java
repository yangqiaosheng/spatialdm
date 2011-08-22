package spade.analysis.tools.index;

/**
 * 
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import weka.clusterers.forOPTICSAndDBScan.DataObjects.DataObject;
import weka.core.Instances;
import edu.wlu.cs.levy.CG.KDTree;

/**
 * @author Admin
 *
 */
public class SpatialTemporalIndexDatabase extends SpatialIndexDatabase {

	// two dimensional kd-tree for the positions of events
	KDTree<EventList> sttree = new KDTree<EventList>(2);

	// list of all event lists for enumeration
	List<EventList> allEvents = new ArrayList<EventList>();

	public SpatialTemporalIndexDatabase(Instances instances) {
		super(instances);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * weka.clusterers.forOPTICSAndDBScan.Databases.Database#dataObjectIterator
	 * ()
	 */
	@Override
	public Iterator dataObjectIterator() {
		// TODO Auto-generated method stub
		return allEvents.iterator();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * weka.clusterers.forOPTICSAndDBScan.Databases.Database#getDataObject(java
	 * .lang.String)
	 */
	@Override
	public DataObject getDataObject(String key) {
		// TODO Auto-generated method stub
		return (DataObject) map.get(Integer.parseInt(key));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * weka.clusterers.forOPTICSAndDBScan.Databases.Database#epsilonRangeQuery
	 * (double, weka.clusterers.forOPTICSAndDBScan.DataObjects.DataObject)
	 */
	@Override
	public List epsilonRangeQuery(double epsilon, DataObject queryDataObject) {
		// TODO Auto-generated method stub
		EventList obj = (EventList) queryDataObject;
		try {
			double[] coordinates = obj.getCoordinates();
			List<EventList> eList = sttree.nearestGeo(coordinates, epsilon);
			return eList;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return null;
		}
	}

	int id = 0;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * weka.clusterers.forOPTICSAndDBScan.Databases.Database#insert(weka.clusterers
	 * .forOPTICSAndDBScan.DataObjects.DataObject)
	 */
	@Override
	public void insert(DataObject dataObject) {
		// TODO Auto-generated method stub

		GeoDataObject obj = (GeoDataObject) dataObject;
		map.put(id, obj);
		id++;
		try {
			double[] coordinates = { obj.getInstance().valueSparse(0), obj.getInstance().valueSparse(1) };
			//System.out.println(coordinates[0] + ", " + coordinates[1]);
			//map.put(dataObject.getKey(), dataObject);
			//List<GeoDataObject> res = tree.search(coordinates);
			EventList eList = sttree.search(coordinates);
			if (eList == null) {
				eList = new EventList(obj.getInstance(), obj.getKey(), this);
				eList.add(obj);
				eList.setCoordinates(coordinates);
				sttree.insert(coordinates, eList);
				allEvents.add(eList);
			} else {
				eList.add(obj);
			}

		} catch (edu.wlu.cs.levy.CG.KeyDuplicateException e) {
			// TODO: handle exception
			//e.printStackTrace();
			return;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return;
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
