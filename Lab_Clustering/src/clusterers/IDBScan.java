package clusterers;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import weka.clusterers.AbstractClusterer;
import weka.clusterers.UpdateableClusterer;
import weka.clusterers.forOPTICSAndDBScan.DataObjects.DataObject;
import weka.clusterers.forOPTICSAndDBScan.Databases.Database;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

/**
 *  <p>
 *  Authors: Haolin Zhi, Peca Iulian <br/>
 *  Date: August, 01, 2010 <br/>
 *  Time: 1:23:38 PM <br/>
 *  $ Revision 1.5 $ <br/>
 *  </p>
 *
 *  @author Haolin Zhi (zhi@cs.uni-bonn.de)
 *  @author Peca Iulian (pkiulian@gmail.com)
 *  @version $Revision: 1.5 $
 */
public class IDBScan extends AbstractClusterer implements OptionHandler, TechnicalInformationHandler, UpdateableClusterer {

	/**
	 *  for serialization 
	 */
	private static final long serialVersionUID = -8518412307828035277L;

	/**
	 * Holds the current clusterID
	 */
	private int clusterID;

	/**
	 * The database that is used for DBScan
	 */
	private Database database;

	/**
	 * Holds the distance-type that is used
	 * (default = weka.clusterers.forOPTICSAndDBScan.DataObjects.EuclidianDataObject)
	 */
	private String database_distanceType = "weka.clusterers.forOPTICSAndDBScan.DataObjects.EuclidianDataObject";

	/**
	 * Holds the type of the used database
	 * (default = weka.clusterers.forOPTICSAndDBScan.Databases.SequentialDatabase)
	 */
	private String database_Type = "weka.clusterers.forOPTICSAndDBScan.Databases.SequentialDatabase";

	/**
	 * Holds the time-value (seconds) for the duration of the DBSCAN clustering-process
	 */
	private double elapsedTimeForDBSCAN;

	/**
	 * Holds the time-value (seconds) for the duration of the Incremental DBSCAN clustering-process
	 */
	private double elapsedTimeForIDBSCAN;

	/**
	 * Specifies the radius for a range-query
	 */
	private double epsilon = 0.9;

	/**
	 * Specifies the density (the range-query must contain at least minPoints DataObjects)
	 */
	private int minPoints = 6;

	/**
	 * Holds the number of clusters generated
	 */
	private int numberOfGeneratedClusters;

	/**
	 * Counter for the processed instances
	 */
	private int processed_InstanceID;

	/**
	 * Replace missing values in training instances
	 */
	private ReplaceMissingValues replaceMissingValues_Filter;

	/**
	 * whether a merge event has occurred
	 */
	private boolean hasMerged = false;

	/**
	 * Generate Clustering via DBScan
	 * @param instances The instances that need to be clustered
	 * @throws java.lang.Exception If clustering was not successful
	 */
	@Override
	public void buildClusterer(Instances instances) throws Exception {
		// can clusterer handle the data?
		getCapabilities().testWithFail(instances);
		begin = System.currentTimeMillis();
		long time_1 = System.currentTimeMillis();

		processed_InstanceID = 0;
		numberOfGeneratedClusters = 0;
		clusterID = 0;

		replaceMissingValues_Filter = new ReplaceMissingValues();
		replaceMissingValues_Filter.setInputFormat(instances);
		Instances filteredInstances = Filter.useFilter(instances, replaceMissingValues_Filter);

		database = databaseForName(getDatabase_Type(), filteredInstances);

		for (int i = 0; i < database.getInstances().numInstances(); i++) {
			DataObject dataObject = dataObjectForName(getDatabase_distanceType(), database.getInstances().instance(i), Integer.toString(i), database);
			database.insert(dataObject);
		}
		//		System.out.println("Clustered Instances by DBScan:" + database.size());
		database.setMinMaxValues();

		@SuppressWarnings("unchecked")
		Iterator<DataObject> iterator = database.dataObjectIterator();
		while (iterator.hasNext()) {
			DataObject dataObject = iterator.next();
			if (dataObject.getClusterLabel() == DataObject.UNCLASSIFIED) {
				if (expandCluster(dataObject)) {
					clusterID++;
					numberOfGeneratedClusters++;
				}
			}
		}

		long time_2 = System.currentTimeMillis();
		elapsedTimeForDBSCAN = (time_2 - time_1) / 1000.0;
	}

	// *****************************************************************************************************************
	// constructors
	// *****************************************************************************************************************

	// *****************************************************************************************************************
	// methods
	// *****************************************************************************************************************
	int num = 0;
	long begin = 0;

	/**
	 * Assigns this dataObject to a cluster or remains it as NOISE
	 * @param dataObject The DataObject that needs to be assigned
	 * @return true, if the DataObject could be assigned, else false
	 */
	private boolean expandCluster(DataObject dataObject) {
		@SuppressWarnings("unchecked")
		List<DataObject> seedList = database.epsilonRangeQuery(getEpsilon(), dataObject);
		/** dataObject is NO coreObject */
		if (seedList.size() < getMinPoints()) {
			System.out.println("the " + (++num) + " instance is being clustered by DBScan, escaped time: " + (System.currentTimeMillis()-begin)/1000.0 + "s,\tused memory: " + (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())*1.024*1.024/1000000.0 + "MB");
			dataObject.setClusterLabel(DataObject.NOISE);
			return false;
		}

		/** dataObject is coreObject */
		for (int i = 0; i < seedList.size(); i++) {
			DataObject seedListDataObject = seedList.get(i);
			/** label this seedListDataObject with the current clusterID, because it is in epsilon-range */
			if (seedListDataObject.getClusterLabel() == DataObject.UNCLASSIFIED) {
				System.out.println("the " + (++num) + " instance is being clustered by DBScan, escaped time: " + (System.currentTimeMillis()-begin)/1000.0 + "s,\tused memory: " + (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())*1.024*1.024/1000000.0 + "MB");
			}
			seedListDataObject.setClusterLabel(clusterID);
			if (seedListDataObject.equals(dataObject)) {
				seedList.remove(i);
				i--;
			}
		}

		/** Iterate the seedList of the startDataObject */
		for (int j = 0; j < seedList.size(); j++) {
			DataObject seedListDataObject = seedList.get(j);
			@SuppressWarnings("unchecked")
			List<DataObject> seedListDataObject_Neighbourhood = database.epsilonRangeQuery(getEpsilon(), seedListDataObject);

			/** seedListDataObject is coreObject */
			if (seedListDataObject_Neighbourhood.size() >= getMinPoints()) {
				for (int i = 0; i < seedListDataObject_Neighbourhood.size(); i++) {
					DataObject p = seedListDataObject_Neighbourhood.get(i);
					if (p.getClusterLabel() == DataObject.UNCLASSIFIED || p.getClusterLabel() == DataObject.NOISE) {
						if (p.getClusterLabel() == DataObject.UNCLASSIFIED) {
							System.out.println("the " + (++num) + " instance is being clustered by DBScan, escaped time: " + (System.currentTimeMillis()-begin)/1000.0 + "s,\tused memory: " + (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())*1.024*1.024/1000000.0 + "MB");
						}
						if (p.getClusterLabel() == DataObject.UNCLASSIFIED) {
							seedList.add(p);
						}
						p.setClusterLabel(clusterID);
					}
				}
			}

			seedList.remove(j);
			j--;
		}

		return true;
	}

	/**
	 * Classifies a given instance.
	 *
	 * @param instance The instance to be assigned to a cluster
	 * @return int The number of the assigned cluster as an integer
	 * @throws java.lang.Exception If instance could not be clustered
	 * successfully
	 */
	@Override
	public int clusterInstance(Instance instance) throws Exception {
		if (processed_InstanceID >= database.size()) {
			processed_InstanceID = 0;
		}
		int cnum = (database.getDataObject(Integer.toString(processed_InstanceID++))).getClusterLabel();
		if (cnum == DataObject.NOISE)
			throw new Exception();
		else
			return cnum;
	}

	/**
	 * Returns the tip text for this property
	 * @return tip text for this property suitable for
	 * displaying in the explorer/experimenter gui
	 */
	public String database_distanceTypeTipText() {
		return "used distance-type";
	}

	/**
	 * Returns the tip text for this property
	 * @return tip text for this property suitable for
	 * displaying in the explorer/experimenter gui
	 */
	public String database_TypeTipText() {
		return "used database";
	}

	/**
	 * Returns a new Class-Instance of the specified database
	 * @param database_Type String of the specified database
	 * @param instances Instances that were delivered from WEKA
	 * @return Database New constructed Database
	 */
	@SuppressWarnings("unchecked")
	public Database databaseForName(String database_Type, Instances instances) {
		Database database = null;

		Constructor<Database> co = null;
		try {
			co = (Constructor<Database>) (Class.forName(database_Type)).getConstructor(new Class[] { Instances.class });
			database = co.newInstance(new Object[] { instances });
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

		return database;
	}

	/**
	 * Returns a new Class-Instance of the specified database
	 * @param database_distanceType String of the specified distance-type
	 * @param instance The original instance that needs to hold by this DataObject
	 * @param key Key for this DataObject
	 * @param database Link to the database
	 * @return DataObject New constructed DataObject
	 */
	@SuppressWarnings("unchecked")
	public DataObject dataObjectForName(String database_distanceType, Instance instance, String key, Database database) {
		Object o = null;

		Constructor<Database> co = null;
		try {
			co = (Constructor<Database>) (Class.forName(database_distanceType)).getConstructor(new Class[] { Instance.class, String.class, Database.class });
			o = co.newInstance(new Object[] { instance, key, database });
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

		return (DataObject) o;
	}

	/**
	 * Returns the tip text for this property
	 * @return tip text for this property suitable for
	 * displaying in the explorer/experimenter gui
	 */
	public String epsilonTipText() {
		return "radius of the epsilon-range-queries";
	}

	/**
	 * Returns default capabilities of the clusterer.
	 *
	 * @return      the capabilities of this clusterer
	 */
	@Override
	public Capabilities getCapabilities() {
		Capabilities result = super.getCapabilities();
		result.disableAll();
		result.enable(Capability.NO_CLASS);

		// attributes
		result.enable(Capability.NOMINAL_ATTRIBUTES);
		result.enable(Capability.NUMERIC_ATTRIBUTES);
		result.enable(Capability.DATE_ATTRIBUTES);
		result.enable(Capability.MISSING_VALUES);

		return result;
	}

	/**
	 * Returns the distance-type
	 * @return String Distance-type
	 */
	public String getDatabase_distanceType() {
		return database_distanceType;
	}

	/**
	 * Returns the type of the used index (database)
	 * @return String Index-type
	 */
	public String getDatabase_Type() {
		return database_Type;
	}

	/**
	 * Returns the value of epsilon
	 * @return double Epsilon
	 */
	public double getEpsilon() {
		return epsilon;
	}

	/**
	 * Returns the value of minPoints
	 * @return int MinPoints
	 */
	public int getMinPoints() {
		return minPoints;
	}

	/**
	 * Gets the current option settings for the OptionHandler.
	 *
	 * @return String[] The list of current option settings as an array of strings
	 */
	@Override
	public String[] getOptions() {
		String[] options = new String[8];
		int current = 0;

		options[current++] = "-E";
		options[current++] = "" + getEpsilon();
		options[current++] = "-M";
		options[current++] = "" + getMinPoints();
		options[current++] = "-I";
		options[current++] = "" + getDatabase_Type();
		options[current++] = "-D";
		options[current++] = "" + getDatabase_distanceType();

		return options;
	}

	/**
	 * Returns the revision string.
	 * 
	 * @return		the revision
	 */
	@Override
	public String getRevision() {
		return RevisionUtils.extract("$Revision: 5538 $");
	}

	/**
	 * Returns an instance of a TechnicalInformation object, containing 
	 * detailed information about the technical background of this class,
	 * e.g., paper reference or book this class is based on.
	 * 
	 * @return the technical information about this class
	 */
	@Override
	public TechnicalInformation getTechnicalInformation() {
		TechnicalInformation result;

		result = new TechnicalInformation(Type.INPROCEEDINGS);
		result.setValue(Field.AUTHOR, "Martin Ester and Hans-Peter Kriegel and Joerg Sander and Xiaowei Xu");
		result.setValue(Field.TITLE, "A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases with Noise");
		result.setValue(Field.BOOKTITLE, "Second International Conference on Knowledge Discovery and Data Mining");
		result.setValue(Field.EDITOR, "Evangelos Simoudis and Jiawei Han and Usama M. Fayyad");
		result.setValue(Field.YEAR, "1996");
		result.setValue(Field.PAGES, "226-231");
		result.setValue(Field.PUBLISHER, "AAAI Press");

		return result;
	}

	/**
	 * Returns a string describing this DataMining-Algorithm
	 * @return String Information for the gui-explorer
	 */
	public String globalInfo() {
		return getTechnicalInformation().toString();
	}

	/**
	 * Returns an enumeration of all the available options..
	 *
	 * @return Enumeration An enumeration of all available options.
	 */
	@Override
	public Enumeration<Option> listOptions() {
		Vector<Option> vector = new Vector<Option>();

		vector.addElement(new Option("\tepsilon (default = 0.9)", "E", 1, "-E <double>"));
		vector.addElement(new Option("\tminPoints (default = 6)", "M", 1, "-M <int>"));
		vector.addElement(new Option("\tindex (database) used for DBScan (default = weka.clusterers.forOPTICSAndDBScan.Databases.SequentialDatabase)", "I", 1, "-I <String>"));
		vector.addElement(new Option("\tdistance-type (default = weka.clusterers.forOPTICSAndDBScan.DataObjects.EuclidianDataObject)", "D", 1, "-D <String>"));
		return vector.elements();
	}

	/**
	 * Returns the tip text for this property
	 * @return tip text for this property suitable for
	 * displaying in the explorer/experimenter gui
	 */
	public String minPointsTipText() {
		return "minimun number of DataObjects required in an epsilon-range-query";
	}

	/**
	 * Returns the number of clusters.
	 *
	 * @return int The number of clusters generated for a training dataset.
	 * @throws java.lang.Exception if number of clusters could not be returned
	 * successfully
	 */
	@Override
	public int numberOfClusters() throws Exception {
		return numberOfGeneratedClusters;
	}

	/**
	 * Sets a new distance-type
	 * @param database_distanceType The new distance-type
	 */
	public void setDatabase_distanceType(String database_distanceType) {
		this.database_distanceType = database_distanceType;
	}

	/**
	 * Sets a new database-type
	 * @param database_Type The new database-type
	 */
	public void setDatabase_Type(String database_Type) {
		this.database_Type = database_Type;
	}

	/**
	 * Sets a new value for epsilon
	 * @param epsilon Epsilon
	 */
	public void setEpsilon(double epsilon) {
		this.epsilon = epsilon;
	}

	/**
	 * Sets a new value for minPoints
	 * @param minPoints MinPoints
	 */
	public void setMinPoints(int minPoints) {
		this.minPoints = minPoints;
	}

	/**
	 * Sets the OptionHandler's options using the given list. All options
	 * will be set (or reset) during this call (i.e. incremental setting
	 * of options is not possible). <p/>
	 *
	 <!-- options-start -->
	 * Valid options are: <p/>
	 * 
	 * <pre> -E &lt;double&gt;
	 *  epsilon (default = 0.9)</pre>
	 * 
	 * <pre> -M &lt;int&gt;
	 *  minPoints (default = 6)</pre>
	 * 
	 * <pre> -I &lt;String&gt;
	 *  index (database) used for DBScan (default = weka.clusterers.forOPTICSAndDBScan.Databases.SequentialDatabase)</pre>
	 * 
	 * <pre> -D &lt;String&gt;
	 *  distance-type (default = weka.clusterers.forOPTICSAndDBScan.DataObjects.EuclidianDataObject)</pre>
	 * 
	 <!-- options-end -->
	 *
	 * @param options The list of options as an array of strings
	 * @throws java.lang.Exception If an option is not supported
	 */
	@Override
	public void setOptions(String[] options) throws Exception {
		String optionString = Utils.getOption('E', options);
		if (optionString.length() != 0) {
			setEpsilon(Double.parseDouble(optionString));
		}

		optionString = Utils.getOption('M', options);
		if (optionString.length() != 0) {
			setMinPoints(Integer.parseInt(optionString));
		}

		optionString = Utils.getOption('I', options);
		if (optionString.length() != 0) {
			setDatabase_Type(optionString);
		}

		optionString = Utils.getOption('D', options);
		if (optionString.length() != 0) {
			setDatabase_distanceType(optionString);
		}
	}

	/**
	 * Returns a description of the clusterer
	 * 
	 * @return a string representation of the clusterer
	 */
	@Override
	public String toString() {
		StringBuffer stringBuffer = new StringBuffer();

//		for (int i = 0; i < database.size(); i++) {
//			DataObject dataObject = database.getDataObject(Integer.toString(i));
//			stringBuffer.append("(" + Utils.doubleToString(Double.parseDouble(dataObject.getKey()), (Integer.toString(database.size()).length()), 0) + ".) " + Utils.padRight(dataObject.toString(), 69) + "  -->  "
//					+ ((dataObject.getClusterLabel() == DataObject.NOISE) ? "NOISE\n" : dataObject.getClusterLabel() + "\n"));
//		}

		stringBuffer.append("IDBScan clustering results\n" + "========================================================================================\n\n");
		stringBuffer.append("Clustered DataObjects: " + database.size() + "\n");
		stringBuffer.append("Number of attributes: " + database.getInstances().numAttributes() + "\n");
		stringBuffer.append("Epsilon: " + getEpsilon() + "; minPoints: " + getMinPoints() + "\n");
		stringBuffer.append("Index: " + getDatabase_Type() + "\n");
		stringBuffer.append("Distance-type: " + getDatabase_distanceType() + "\n");
		stringBuffer.append("Number of generated clusters: " + numberOfGeneratedClusters + "\n");
		DecimalFormat decimalFormat = new DecimalFormat(".##");
		stringBuffer.append("Elapsed time for DBScan: " + decimalFormat.format(elapsedTimeForDBSCAN) + "\n");
		stringBuffer.append("Elapsed time for IDBScan: " + decimalFormat.format(elapsedTimeForIDBSCAN) + "\n\n");

		return stringBuffer.toString() + "\n";
	}

	@Override
	public void updateClusterer(Instance newInstance) throws Exception {
		long start = System.currentTimeMillis();
		DataObject dataObject = dataObjectForName(getDatabase_distanceType(), newInstance, Integer.toString(database.size()), database);
		database.insert(dataObject);

		/*	Incremental DBSCAN method I
		 *  implement the algorithm from the paper:
		 *  Incremental Clustering for Mining in a Warehousing Environment	*/
//		insert2(dataObject);  
		insert(dataObject);     

		/* 	Incremental DBSCAN method II
		 *  faster than insert(dataObject)	*/
//		fastInsert(dataObject);

		System.out.println("the " + database.size() + " instance is being clustered by IDBScan, escaped time: " + (System.currentTimeMillis()-begin)/1000.0 + "s,\tused memory: " + (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())*1.024*1.024/1000000.0 + "MB");
		long end = System.currentTimeMillis();
		elapsedTimeForIDBSCAN += (end - start) / 1000.0;
	}

	@Override
	public void updateFinished() {
		long start = System.currentTimeMillis();
		database.setMinMaxValues();
		if (hasMerged) {
			sortClusterLabels();
			hasMerged = false;
		}
		long end = System.currentTimeMillis();
		elapsedTimeForIDBSCAN += (end - start) / 1000.0;
	}

	private void sortClusterLabels() {
		Set<Integer> clusterLabels = new TreeSet<Integer>();
		for (int i = 0; i < database.size(); i++) {
			int clusterLabel = database.getDataObject(Integer.toString(i)).getClusterLabel();
			if (clusterLabel != DataObject.NOISE) {
				clusterLabels.add(clusterLabel);
			}
		}
		numberOfGeneratedClusters = clusterLabels.size();

		Map<Integer, Integer> clusterLabelMaps = new TreeMap<Integer, Integer>();
		int jlable = 0;
		for (int clusterLabel : clusterLabels) {
			clusterLabelMaps.put(clusterLabel, jlable++);
		}

		for (int i = 0; i < database.size(); i++) {
			DataObject tochange = database.getDataObject(Integer.toString(i));
			if (tochange.getClusterLabel() != DataObject.NOISE) {
				tochange.setClusterLabel(clusterLabelMaps.get(tochange.getClusterLabel()));
			}
		}
	}

	/**
	 * optimized implementation of Incremental DBScan
	 * combine the benefits of insert() and insert2()
	 * @param dataObject
	 */
	@SuppressWarnings("unchecked")
	private void fastInsert(DataObject dataObject) {
		List<DataObject> firstNeighbourhoodList = database.epsilonRangeQuery(getEpsilon(), dataObject);

		dataObject.setClusterLabel(DataObject.NOISE);

		Set<Integer> neighbourhoodClusterLabels = new TreeSet<Integer>();

		/** Iterate the neighbourhoodList of the startDataObject */
		for (int j = 0; j < firstNeighbourhoodList.size(); j++) {
			DataObject firstNeighbourhood = firstNeighbourhoodList.get(j);
			List<DataObject> secondNeighbourhoodList = database.epsilonRangeQuery(getEpsilon(), firstNeighbourhood);

			if (secondNeighbourhoodList.size() >= getMinPoints()) {
				/** neighbourhoodDataObject is coreObject */

				if (firstNeighbourhood.getClusterLabel() == DataObject.NOISE) {
					/** create */
					for (int i = 0; i < secondNeighbourhoodList.size(); i++) {
						DataObject seedListDataObject_NeighbourhoodDataObject = secondNeighbourhoodList.get(i);
						if (seedListDataObject_NeighbourhoodDataObject.getClusterLabel() != DataObject.NOISE) {
							if (firstNeighbourhood == dataObject) {
								if (database.epsilonRangeQuery(getEpsilon(), seedListDataObject_NeighbourhoodDataObject).size() >= getMinPoints()) {
									neighbourhoodClusterLabels.add(seedListDataObject_NeighbourhoodDataObject.getClusterLabel());
								}
							}
						}
						seedListDataObject_NeighbourhoodDataObject.setClusterLabel(clusterID);
						neighbourhoodClusterLabels.add(clusterID);
					}
					clusterID++;
					numberOfGeneratedClusters++;
				} else {
					/** absorb or merge */
					neighbourhoodClusterLabels.add(firstNeighbourhood.getClusterLabel());
					for (int i = 0; i < secondNeighbourhoodList.size(); i++) {
						DataObject seedListDataObject_NeighbourhoodDataObject = secondNeighbourhoodList.get(i);
						if (seedListDataObject_NeighbourhoodDataObject.getClusterLabel() != DataObject.NOISE) {
							if (secondNeighbourhoodList.size() == getMinPoints()) {
								if (database.epsilonRangeQuery(getEpsilon(), seedListDataObject_NeighbourhoodDataObject).size() >= getMinPoints()) {
									neighbourhoodClusterLabels.add(seedListDataObject_NeighbourhoodDataObject.getClusterLabel());
								}
							}
						} else {
							seedListDataObject_NeighbourhoodDataObject.setClusterLabel(firstNeighbourhood.getClusterLabel());
						}
					}
				}
			}
		}

		/** merge */
		if (neighbourhoodClusterLabels.size() > 1) {
			hasMerged = true;

			if (firstNeighbourhoodList.size() < getMinPoints()) {
				System.out.println("transitive merge:" + neighbourhoodClusterLabels.size());
				//				return;
			} else {
				System.out.println("normal merge:" + neighbourhoodClusterLabels.size());
			}

			int mergeClusterID = neighbourhoodClusterLabels.iterator().next();
			neighbourhoodClusterLabels.remove(mergeClusterID);

			for (int i = 0; i < database.size(); i++) {
				DataObject mergedataObject = database.getDataObject(Integer.toString(i));
				if (neighbourhoodClusterLabels.contains(mergedataObject.getClusterLabel())) {
					mergedataObject.setClusterLabel(mergeClusterID);
				}
			}
		}
	}
	
	/**
	* implement the incremental method according to the paper
	* faster without using query cache
	* but slower when using query cache
	* @param dataObject
	*/
	@SuppressWarnings({ "unchecked" })
	private void insert2(DataObject dataObject) {
		Set<DataObject> updSeeds = new HashSet<DataObject>();
		Set<DataObject> updSeedsNeighbours = new HashSet<DataObject>();
		List<DataObject> firstNeighbourhoodList = database.epsilonRangeQuery(getEpsilon(), dataObject);
		
		dataObject.setClusterLabel(DataObject.NOISE);

		/** Iterate the neighbourhoodList of the startDataObject */
		for (int j = 0; j < firstNeighbourhoodList.size(); j++) {
			/** neighbourhoodDataObject is new coreObject p' */
			DataObject firstNeighbourhood = firstNeighbourhoodList.get(j);
			List<DataObject> secondNeighbourhoodList = database.epsilonRangeQuery(getEpsilon(), firstNeighbourhood);
			if (secondNeighbourhoodList.size() >= getMinPoints()) {
				updSeeds.add(firstNeighbourhood);
				updSeedsNeighbours.addAll(secondNeighbourhoodList);
			}
		}

		// count cluster label
		TreeSet<Integer> neighbourhoodClusterLabels = new TreeSet<Integer>();
		for (DataObject updSeed : updSeeds) {
			if (updSeed.getClusterLabel() != DataObject.NOISE) {
				neighbourhoodClusterLabels.add(updSeed.getClusterLabel());
			}
		}

		/** noise */
		if (updSeeds.size() == 0)
			return;

		/** creation */
		if (neighbourhoodClusterLabels.size() == 0) {
			for (DataObject updSeedsNeighbour : updSeedsNeighbours) {
				updSeedsNeighbour.setClusterLabel(clusterID);
			}
			clusterID++;
			numberOfGeneratedClusters++;
		}

		/** absorption */
		if (neighbourhoodClusterLabels.size() == 1) {
			int absorptClusterID = neighbourhoodClusterLabels.iterator().next();
			for (DataObject updSeedsNeighbour : updSeedsNeighbours) {
				if (updSeedsNeighbour.getClusterLabel() == DataObject.NOISE) {
					updSeedsNeighbour.setClusterLabel(absorptClusterID);
				}
			}
		}

		/** merge */
		if (neighbourhoodClusterLabels.size() > 1) {
			hasMerged = true;
			if (firstNeighbourhoodList.size() < getMinPoints()) {
				System.out.println("transitive merge:" + neighbourhoodClusterLabels.size());
			} else {
				System.out.println("normal merge:" + neighbourhoodClusterLabels.size());
			}

			int mergeClusterID = neighbourhoodClusterLabels.iterator().next();
			neighbourhoodClusterLabels.remove(mergeClusterID);

			for (DataObject updSeedsNeighbour : updSeedsNeighbours) {
				if (updSeedsNeighbour.getClusterLabel() == DataObject.NOISE) {
					updSeedsNeighbour.setClusterLabel(mergeClusterID);
				}
			}

			for (int i = 0; i < database.size(); i++) {
				DataObject mergedataObject = database.getDataObject(Integer.toString(i));
				if (neighbourhoodClusterLabels.contains(mergedataObject.getClusterLabel())) {
					mergedataObject.setClusterLabel(mergeClusterID);
				}
			}
		}
	}
	

	/**
	* implement the incremental method according to the paper
	* faster when using query cache
	* @param dataObject
	*/
	@SuppressWarnings({ "unchecked" })
	private void insert(DataObject dataObject) {
		Set<DataObject> updSeeds = new HashSet<DataObject>();
		List<DataObject> firstNeighbourhoodList = database.epsilonRangeQuery(getEpsilon(), dataObject);
		
		dataObject.setClusterLabel(DataObject.NOISE);

		/** Iterate the neighbourhoodList of the startDataObject */
		for (int j = 0; j < firstNeighbourhoodList.size(); j++) {
			/** neighbourhoodDataObject is new coreObject p' */
			DataObject firstNeighbourhood = firstNeighbourhoodList.get(j);
			if (database.epsilonRangeQuery(getEpsilon(), firstNeighbourhood).size() >= getMinPoints()) {
				updSeeds.add(firstNeighbourhood);
			}
		}

		// count cluster label
		TreeSet<Integer> neighbourhoodClusterLabels = new TreeSet<Integer>();
		for (DataObject updSeed : updSeeds) {
			if (updSeed.getClusterLabel() != DataObject.NOISE) {
				neighbourhoodClusterLabels.add(updSeed.getClusterLabel());
			}
		}

		/** noise */
		if (updSeeds.size() == 0)
			return;

		/** creation */
		if (neighbourhoodClusterLabels.size() == 0) {
			for (DataObject updSeed : updSeeds) {
				List<DataObject> createList = database.epsilonRangeQuery(getEpsilon(), updSeed);
				for (DataObject create : createList) {
					create.setClusterLabel(clusterID);
				}
			}
			clusterID++;
			numberOfGeneratedClusters++;
		}

		/** absorption */
		if (neighbourhoodClusterLabels.size() == 1) {
			int absorptClusterID = neighbourhoodClusterLabels.iterator().next();
			for (DataObject updSeed : updSeeds) {
				List<DataObject> absorptList = database.epsilonRangeQuery(getEpsilon(), updSeed);
				for (DataObject absorpt : absorptList) {
					if (absorpt.getClusterLabel() == DataObject.NOISE) {
						absorpt.setClusterLabel(absorptClusterID);
					}
				}
			}
		}

		/** merge */
		if (neighbourhoodClusterLabels.size() > 1) {
			hasMerged = true;
			if (firstNeighbourhoodList.size() < getMinPoints()) {
				System.out.println("transitive merge:" + neighbourhoodClusterLabels.size());
			} else {
				System.out.println("normal merge:" + neighbourhoodClusterLabels.size());
			}

			int mergeClusterID = neighbourhoodClusterLabels.iterator().next();
			neighbourhoodClusterLabels.remove(mergeClusterID);

			for (DataObject updSeed : updSeeds) {
				List<DataObject> absorptList = database.epsilonRangeQuery(getEpsilon(), updSeed);
				for (DataObject merge : absorptList) {
					if (merge.getClusterLabel() == DataObject.NOISE) {
						merge.setClusterLabel(mergeClusterID);
					}
				}
			}

			for (int i = 0; i < database.size(); i++) {
				DataObject mergedataObject = database.getDataObject(Integer.toString(i));
				if (neighbourhoodClusterLabels.contains(mergedataObject.getClusterLabel())) {
					mergedataObject.setClusterLabel(mergeClusterID);
				}
			}
		}
	}

	public void deleteInstance(String key) {
		//		DataObject deleteDataObject = database.getDataObject(key);
		//		if(deleteDataObject.getClusterLabel() == DataObject.NOISE){
		//			database.remove(key);
		//		}else{
		//			List<DataObject> neighbourhoodList = database.epsilonRangeQuery(getEpsilon(), deleteDataObject);
		//			for (int i = 0; i < neighbourhoodList.size(); i++) {
		//				DataObject neighbourhoodDataObject = neighbourhoodList.get(i);
		//				if(neighbourhoodDataObject.getClusterLabel() == deleteDataObject.getClusterLabel()){
		//					List<DataObject> seedListDataObject_Neighbourhood = database.epsilonRangeQuery(getEpsilon(), neighbourhoodDataObject);
		//					if(seedListDataObject_Neighbourhood.size() >= getMinPoints()){
		//						database.remove(key);
		//						break;
		//					}
		//				}
		//				
		//				
		//			}
		//		}
		//		
		//		
	}

}
