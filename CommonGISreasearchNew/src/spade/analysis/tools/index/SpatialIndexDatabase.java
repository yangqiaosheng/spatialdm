package spade.analysis.tools.index;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import weka.clusterers.forOPTICSAndDBScan.DataObjects.DataObject;
import weka.clusterers.forOPTICSAndDBScan.Databases.Database;
import weka.core.Instances;
import weka.core.RevisionHandler;
import edu.wlu.cs.levy.CG.KDTree;

/**
 *
 */

/**
 * @author Admin
 *
 */
public class SpatialIndexDatabase implements Database, Serializable, RevisionHandler {

	TreeMap map = new TreeMap();
	// two dimensional kd-tree
	KDTree<List<GeoDataObject>> tree = new KDTree<List<GeoDataObject>>(2);
	Instances instances = null;
	int id = 0;

	public SpatialIndexDatabase(Instances instances) {
		this.instances = instances;

	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeweka.clusterers.forOPTICSAndDBScan.Databases.Database#contains(weka.
	 * clusterers.forOPTICSAndDBScan.DataObjects.DataObject)
	 */
	@Override
	public boolean contains(DataObject dataObjectQuery) {
		// TODO Auto-generated method stub

		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * weka.clusterers.forOPTICSAndDBScan.Databases.Database#coreDistance(int,
	 * double, weka.clusterers.forOPTICSAndDBScan.DataObjects.DataObject)
	 */
	@Override
	public List coreDistance(int minPoints, double epsilon, DataObject dataObject) {
		// TODO Auto-generated method stub
		return null;
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
		return map.values().iterator();
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
		GeoDataObject obj = (GeoDataObject) queryDataObject;

		try {
			double[] coordinates = { obj.getInstance().valueSparse(0), obj.getInstance().valueSparse(1) };
			List<List<GeoDataObject>> list = tree.nearestGeo(coordinates, epsilon);//tree.range(lowk, uppk);
			List<GeoDataObject> range = new ArrayList<GeoDataObject>();
			for (List<GeoDataObject> l : list) {
				for (GeoDataObject geo : l) {
					range.add(geo);
				}
			}
			return range;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * weka.clusterers.forOPTICSAndDBScan.Databases.Database#getAttributeMaxValues
	 * ()
	 */
	@Override
	public double[] getAttributeMaxValues() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * weka.clusterers.forOPTICSAndDBScan.Databases.Database#getAttributeMinValues
	 * ()
	 */
	@Override
	public double[] getAttributeMinValues() {
		// TODO Auto-generated method stub
		return null;
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
		return (DataObject) map.get(key);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see weka.clusterers.forOPTICSAndDBScan.Databases.Database#getInstances()
	 */
	@Override
	public Instances getInstances() {
		// TODO Auto-generated method stub
		return instances;
	}

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
		id++;
		GeoDataObject obj = (GeoDataObject) dataObject;

		try {
			double[] coordinates = { obj.getInstance().valueSparse(0), obj.getInstance().valueSparse(1) };
			map.put(dataObject.getKey(), dataObject);
			List<GeoDataObject> res = tree.search(coordinates);
			if (res == null) {
				res = new ArrayList<GeoDataObject>();
				res.add(obj);
				tree.insert(coordinates, res);
			} else {
				res.add(obj);
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

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * weka.clusterers.forOPTICSAndDBScan.Databases.Database#k_nextNeighbourQuery
	 * (int, double, weka.clusterers.forOPTICSAndDBScan.DataObjects.DataObject)
	 */
	@Override
	public List k_nextNeighbourQuery(int k, double epsilon, DataObject dataObject) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see weka.clusterers.forOPTICSAndDBScan.Databases.Database#keyIterator()
	 */
	@Override
	public Iterator keyIterator() {
		// TODO Auto-generated method stub
		return map.keySet().iterator();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * weka.clusterers.forOPTICSAndDBScan.Databases.Database#setMinMaxValues()
	 */
	@Override
	public void setMinMaxValues() {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see weka.clusterers.forOPTICSAndDBScan.Databases.Database#size()
	 */
	@Override
	public int size() {
		// TODO Auto-generated method stub
		return map.size();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see weka.core.RevisionHandler#getRevision()
	 */
	@Override
	public String getRevision() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
