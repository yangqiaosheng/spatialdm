package spade.analysis.tools;

/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    Copyright (C) 2004
 *    & Matthias Schubert (schubert@dbs.ifi.lmu.de)
 *    & Zhanna Melnikova-Albrecht (melnikov@cip.ifi.lmu.de)
 *    & Rainer Holzmann (holzmann@cip.ifi.lmu.de)
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import spade.analysis.tools.index.Cluster;
import spade.analysis.tools.index.Event;
import spade.analysis.tools.index.EventList;
import weka.clusterers.AbstractClusterer;
import weka.clusterers.forOPTICSAndDBScan.DataObjects.DataObject;
import weka.clusterers.forOPTICSAndDBScan.Databases.Database;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

/**
 * <!-- globalinfo-start --> Martin Ester, Hans-Peter Kriegel, Joerg Sander,
 * Xiaowei Xu: A Density-Based Algorithm for Discovering Clusters in Large
 * Spatial Databases with Noise. In: Second International Conference on
 * Knowledge Discovery and Data Mining, 226-231, 1996.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- technical-bibtex-start --> BibTeX:
 * 
 * <pre>
 * &#64;inproceedings{Ester1996,
 *    author = {Martin Ester and Hans-Peter Kriegel and Joerg Sander and Xiaowei Xu},
 *    booktitle = {Second International Conference on Knowledge Discovery and Data Mining},
 *    editor = {Evangelos Simoudis and Jiawei Han and Usama M. Fayyad},
 *    pages = {226-231},
 *    publisher = {AAAI Press},
 *    title = {A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases with Noise},
 *    year = {1996}
 * }
 * </pre>
 * <p/>
 * <!-- technical-bibtex-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -E &lt;double&gt;
 *  epsilon (default = 0.9)
 * </pre>
 * 
 * <pre>
 * -M &lt;int&gt;
 *  minPoints (default = 6)
 * </pre>
 * 
 * <pre>
 * -I &lt;String&gt;
 *  index (database) used for DBScan (default = weka.clusterers.forOPTICSAndDBScan.Databases.SequentialDatabase)
 * </pre>
 * 
 * <pre>
 * -D &lt;String&gt;
 *  distance-type (default = weka.clusterers.forOPTICSAndDBScan.DataObjects.EuclidianDataObject)
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Matthias Schubert (schubert@dbs.ifi.lmu.de)
 * @author Zhanna Melnikova-Albrecht (melnikov@cip.ifi.lmu.de)
 * @author Rainer Holzmann (holzmann@cip.ifi.lmu.de)
 * @version $Revision: 1.1.2.1 $
 */
public class TOGDBSCAN extends AbstractClusterer implements OptionHandler, TechnicalInformationHandler {
	// each cluster has a beginning and an end in time
	public class cluster {
		long begin = 0;
		long end = 0;
		StringBuffer label;
		int entries = 0;
		// the coordinates of the event list that was used for the last merge
		// with this cluster
		double[] coLastUpdate = null;
	}

	// the list of temporally ordered clustered with time intervals
	List<Cluster> linked_clusters = new LinkedList<Cluster>();

	/**
	 * Holds the current clusterID
	 */
	private int clusterID;

	/**
	 * merges temporally dense regions of events with the clusters and events
	 * found in the centre object
	 * 
	 * @param events
	 *            to be clustered
	 */
	int cluster_id = 0;

	public void mergeClusters(List centre, List neighbour) {
		LinkedList<Event> llNeighbour = (LinkedList<Event>) neighbour;
		ListIterator<Event> itNeighbour = llNeighbour.listIterator();
		Cluster lastCluster = null;
		Event lastCentre = null;
		Event lastEvent = null;
		// for all events in the neighbour list
		while (itNeighbour.hasNext()) {
			// retrieve time of the event

			DataObject nextNeighbour = itNeighbour.next();
			long beginNeighbour = ((Event) nextNeighbour).begin;
			long endNeighbour = ((Event) nextNeighbour).end;
			// lastEvent = (Event) nextNeighbour;
			boolean found = false;
			try {
				// compare to list of events and clusters of the centred point

				// can we compare consequent events?

				// first we try the merge consequent event among the
				// neighbours of the centre
				// we compare always two consequent events

				if (lastEvent != null) {

					// is next event close enough to the previous one
					if (beginNeighbour - minTempEspsilon <= lastEvent.end) {
						// System.out.println("next event close in time");
						// look if next event is already clustered
						if (((Event) nextNeighbour).c == null) {
							((Event) nextNeighbour).c = new Cluster();
							((Event) nextNeighbour).c.label = DataObject.NOISE;
							linked_clusters.add(((Event) nextNeighbour).c);
							System.out.println("cluster of next event was null");
						}

						// merge next event with cluster of last event

						// first check if next event is noise
						if (((Event) nextNeighbour).c.label == DataObject.NOISE) {
							// check if also last event was noise too
							if (lastEvent.c.label == DataObject.NOISE) {
								// both are noise
								// create new cluster the same for both
								// System.out
								// .println("last and next cluster are noise");

								clusterID++;
								numberOfGeneratedClusters++;
								// System.out.println("New number of clusters "
								// + numberOfGeneratedClusters);
								// System.out.println("Create new cluster "
								// + clusterID + ", "
								// + numberOfGeneratedClusters);
								((Event) nextNeighbour).c = new Cluster();
								((Event) nextNeighbour).c.label = clusterID;
								((Event) nextNeighbour).c.entries = 2;
								linked_clusters.add(((Event) nextNeighbour).c);
								lastEvent.c = ((Event) nextNeighbour).c;
							}
							// only next event is noise
							else {
								// System.out.println("only next event is noise");
								// cluster of last event also cluster
								// for next event
								// add next event to cluster of last event
								((Event) nextNeighbour).c = lastEvent.c;
								lastEvent.c.entries++;
							}
						}
						// next event is not noise
						else {
							// check if also last event was noise
							if (lastEvent.c.label == DataObject.NOISE) {
								// only last event is noise
								// cluster of next event also cluster
								// of last event
								// add last event to cluster of next event
								// System.out
								// .println("next event is not noise but last event");
								lastEvent.c = ((Event) nextNeighbour).c;
								((Event) nextNeighbour).c.entries++;
							}
							// no noise
							else {
								// cluster ID of last event becomes cluster
								// ID of next event
								// System.out
								// .println("both events very not noise: "
								// + ((Event) nextNeighbour).c.label
								// + lastEvent.c.label);
								// check that cluster IDs are not the same -
								// recurrent cluster
								if (((Event) nextNeighbour).c.label != lastEvent.c.label) {
									// merge two existing clusters
									((Event) nextNeighbour).c.label = lastEvent.c.label;
									((Event) nextNeighbour).c.entries += lastEvent.c.entries;
									lastEvent.c.entries = ((Event) nextNeighbour).c.entries;
									numberOfGeneratedClusters--;
									// System.out
									// .println("New number of clusters "
									// + numberOfGeneratedClusters);
									// System.out
									// .println("merge in neighbourhood: "
									// + ((Event) nextNeighbour).c.label + ", "
									// + lastEvent.c.label);
								} else {
									// ((Event) nextNeighbour).c.entries +=
									// lastEvent.c.entries;
									// lastEvent.c.entries = ((Event)
									// nextNeighbour).c.entries;
								}
							}
						}
					} else {
						// next event is not close in time to be in same
						// cluster with last event
						// is next event already assigned to a cluster?
						// System.out
						// .println("last and next event not close in time");
						if (((Event) nextNeighbour).c == null) {
							((Event) nextNeighbour).c = new Cluster();
							linked_clusters.add(((Event) nextNeighbour).c);
							((Event) nextNeighbour).c.label = DataObject.NOISE;
						}
					}
					// first event on neighbour list
				} else {
					// is next event already assigned to a cluster?
					// System.out.println("First event in neighbourhood");
					if (((Event) nextNeighbour).c == null) {
						((Event) nextNeighbour).c = new Cluster();
						linked_clusters.add(((Event) nextNeighbour).c);
						((Event) nextNeighbour).c.label = DataObject.NOISE;
					}
				}
				lastEvent = (Event) nextNeighbour;
				// if no events exists in the centre
				if (centre == null) {
					continue;
				}
				// for iteration over the events in the centre
				ListIterator<Event> itCentre = centre.listIterator();
				Event nextCentre = null;
				// search for merging position
				// first position in the centre events to be close enough
				// list of events in the centre with the temporal epsilon
				// neighbourhood of the next neighbour event
				List<Event> nearCentreEvents = new LinkedList<Event>();
				boolean merge = false;
				if (lastCentre == null) {
					while (itCentre.hasNext()) {
						if (endNeighbour >= (nextCentre = itCentre.next()).begin - minTempEspsilon) {
							if (beginNeighbour <= nextCentre.end + minTempEspsilon) {
								// in temporal epsilon range
							} else {
								// next event happened after last centre
								// event ended
								continue;
							}
						} else {
							merge = true;
							lastCentre = nextCentre;
							// fill all events close in time
							nearCentreEvents.add(nextCentre);
							Event nextNearEvent = null;
							int aheads = 0;
							while (itCentre.hasNext()) {
								nextNearEvent = itCentre.next();
								aheads++;
								// in range of next central event?
								if ((beginNeighbour <= nextNearEvent.end + minTempEspsilon) && (endNeighbour >= nextNearEvent.begin - minTempEspsilon)) {
									nearCentreEvents.add(nextNearEvent);
								} else {
									break;
								}

							}
							System.out.println("last centre null ahead: " + aheads);
							// set back
							for (int i = 0; i < aheads; i++) {
								itCentre.previous();
							}

							break;
						}

					}
				} else {
					// are we still in temporal epsilon range?
					if ((beginNeighbour >= lastCentre.end + minTempEspsilon) || (endNeighbour <= lastCentre.begin - minTempEspsilon)) {
						// look for new merging position - we are no longer in
						// range
						while (itCentre.hasNext()) {
							if (endNeighbour >= (nextCentre = itCentre.next()).begin - minTempEspsilon) {
								if (beginNeighbour <= nextCentre.end + minTempEspsilon) {
									// in temporal epsilon range
								} else {
									// next event happened after last centre
									// event ended
									continue;
								}
							} else {
								merge = true;
								lastCentre = nextCentre;
								// fill all events close in time
								nearCentreEvents.add(nextCentre);
								Event nextNearEvent = null;
								int aheads = 0;
								while (itCentre.hasNext()) {
									nextNearEvent = itCentre.next();
									aheads++;
									// in range of next central event?
									if ((beginNeighbour <= nextNearEvent.end + minTempEspsilon) && (endNeighbour >= nextNearEvent.begin - minTempEspsilon)) {
										nearCentreEvents.add(nextNearEvent);
									} else {
										break;
									}

								}
								System.out.println("out of range ahead: " + aheads);
								// set back
								for (int i = 0; i < aheads; i++) {
									itCentre.previous();
								}

								break;
							}

						}

					} else {
						// we are still in range
						nearCentreEvents.add(lastCentre);
						Event nextNearEvent = null;
						int aheads = 0;
						while (itCentre.hasNext()) {
							nextNearEvent = itCentre.next();
							aheads++;
							// in range of next central event?
							if ((beginNeighbour <= nextNearEvent.end + minTempEspsilon) && (endNeighbour >= nextNearEvent.begin - minTempEspsilon)) {
								nearCentreEvents.add(nextNearEvent);
							} else {
								break;
							}

						}
						System.out.println("in range ahead: " + aheads);
						// set back
						for (int i = 0; i < aheads; i++) {
							itCentre.previous();
						}
					}
				}
				// look if we can merge clusters
				for (Event nextNearEvent : nearCentreEvents) {
					if (nextNearEvent.c.label == DataObject.NOISE) {
						// next centre event is noise
						if (((Event) nextNeighbour).c.label == DataObject.NOISE) {
							// also the next neighbour event is noise
							// create new cluster the same for both
							nextNearEvent.c = new Cluster();
							nextNearEvent.c.entries = 2;
							clusterID++;
							numberOfGeneratedClusters++;
							nextNearEvent.c.label = clusterID;
							((Event) nextNeighbour).c = nextNearEvent.c;
							linked_clusters.add(((Event) nextNeighbour).c);
						} else {
							// only next centre event is noise
							// next event in neighbourhood is not noise
							// cluster from next neighbour event becomes cluster
							// of next centre event
							nextNearEvent.c = ((Event) nextNeighbour).c;
							((Event) nextNeighbour).c.entries++;
						}
					} else {
						// next centre event is not noise
						if (((Event) nextNeighbour).c.label == DataObject.NOISE) {
							// but the next neighbour event is noise
							// cluster from next centre event will also be
							// cluster of next neighbour event
							((Event) nextNeighbour).c = nextNearEvent.c;
							((Event) nextNeighbour).c.entries++;

						} else {
							// also next neighbour event is not noise
							// merge clusters
							if (((Event) nextNeighbour).c.label != nextNearEvent.c.label) {

								((Event) nextNeighbour).c.entries += nextNearEvent.c.entries;
								nextNearEvent.c.entries = ((Event) nextNeighbour).c.entries;
								// System.out.println("Remove "
								// + numberOfGeneratedClusters);
								numberOfGeneratedClusters--;
								// System.out.println("New number of clusters "
								// + numberOfGeneratedClusters);

								// System.out
								// .println("Merge between neighbourhood and centre: "
								// + ((Event) nextNeighbour).c.label
								// + "," + nextNearEvent.c.label);
								nextNearEvent.c.label = ((Event) nextNeighbour).c.label;

							} else {
								// ((Event) nextNeighbour).c.entries +=
								// nextNearEvent.c.entries;
								// nextNearEvent.c.entries = ((Event)
								// nextNeighbour).c.entries;
							}
						}
					}

				}

			} catch (Exception e) {
				e.printStackTrace();
				// TODO: handle exception
			}
		}
	}

	/**
	 * Sets the clustering parameters
	 * 
	 * @param eps
	 *            The spatial threshold
	 * @param minTempEps
	 *            The temporal threshold
	 */
	public void setParameters(double eps, double minTempEps, int minP) {
		epsilon = eps;
		minTempEspsilon = minTempEps * 1000; // in milliseconds
		minPoints = minP;
	}

	/** for serialization */
	static final long serialVersionUID = -1666498248451219728L;

	/**
	 * Specifies the radius for a range-query
	 */
	private double epsilon = 0.9;

	/**
	 * Specifies the time radius for a range-query
	 */
	private double minTempEspsilon = 0.9;

	/**
	 * Specifies the density (the range-query must contain at least minPoints
	 * DataObjects)
	 */
	private int minPoints = 6;

	/**
	 * Replace missing values in training instances
	 */
	private ReplaceMissingValues replaceMissingValues_Filter;

	/**
	 * Holds the number of clusters generated
	 */
	private int numberOfGeneratedClusters = 0;

	/**
	 * Holds the distance-type that is used (default =
	 * weka.clusterers.forOPTICSAndDBScan.DataObjects.EuclidianDataObject)
	 */
	private String database_distanceType = "weka.clusterers.forOPTICSAndDBScan.DataObjects.EuclidianDataObject";

	/**
	 * Holds the type of the used database (default =
	 * weka.clusterers.forOPTICSAndDBScan.Databases.SequentialDatabase)
	 */
	private String database_Type = "weka.clusterers.forOPTICSAndDBScan.Databases.SequentialDatabase";

	/**
	 * The database that is used for DBScan
	 */
	private Database database;

	/**
	 * Counter for the processed instances
	 */
	private int processed_InstanceID;

	/**
	 * Holds the time-value (seconds) for the duration of the clustering-process
	 */
	private double elapsedTime;

	/**
	 * Returns default capabilities of the clusterer.
	 * 
	 * @return the capabilities of this clusterer
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

	// *****************************************************************************************************************
	// constructors
	// *****************************************************************************************************************

	// *****************************************************************************************************************
	// methods
	// *****************************************************************************************************************

	/**
	 * Generate Clustering via DBScan
	 * 
	 * @param instances
	 *            The instances that need to be clustered
	 * @throws java.lang.Exception
	 *             If clustering was not successful
	 */
	@Override
	public void buildClusterer(Instances instances) throws Exception {
		// can clusterer handle the data?
		getCapabilities().testWithFail(instances);

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
		database.setMinMaxValues();

		Iterator iterator = database.dataObjectIterator();
		while (iterator.hasNext()) {
			EventList dataObject = (EventList) iterator.next();
			if (!dataObject.processed) {
				if (expandCluster(dataObject)) {
					// clusterID++;
					// numberOfGeneratedClusters++;
					System.out.println("Cluster in list: " + linked_clusters.size());
					// System.out.println("Cluster in numbers: "
					// + numberOfGeneratedClusters);
					for (int i = 0; i < linked_clusters.size(); i++) {
						if ((linked_clusters.get(i).entries < minPoints) && linked_clusters.get(i).label != DataObject.NOISE) {
							linked_clusters.get(i).label = DataObject.NOISE;
							numberOfGeneratedClusters--;
							// System.out
							// .println("postprocess new number of clusters: "
							// + numberOfGeneratedClusters);
						}
					}
					linked_clusters = new LinkedList<Cluster>();
				}
			}
		}

		long time_2 = System.currentTimeMillis();
		elapsedTime = (time_2 - time_1) / 1000.0;
	}

	int proc = 0;

	/**
	 * Assigns this dataObject to a cluster or remains it as NOISE
	 * 
	 * @param dataObject
	 *            The DataObject that needs to be assigned
	 * @return true, if the DataObject could be assigned, else false
	 */
	private boolean expandCluster(DataObject dataObject) {
		// retrieves a list of lists of events
		List<EventList> seedList = database.epsilonRangeQuery(getEpsilon(), dataObject);
		// count events in neighbourhood
		int size = 0;
		for (int i = 0; i < seedList.size(); i++) {
			size += seedList.get(i).events.size();
		}
		/** dataObject is NO coreObject */
		if (size < getMinPoints()) {
			dataObject.setClusterLabel(DataObject.NOISE);
			((EventList) dataObject).processed = true;
			return false;
		}
		// set centre to been processed
		((EventList) dataObject).processed = true;

		/** dataObject is coreObject */
		for (int i = 0; i < seedList.size(); i++) {
			EventList seedListDataObject = seedList.get(i);
			/**
			 * label this seedListDataObject with the current clusterID, because
			 * it is in epsilon-range
			 */
			// temporal merge on centre
			seedListDataObject.setClusterLabel(clusterID++);
			if (seedListDataObject.equals(dataObject)) {
				seedList.remove(i);
				i--;
				mergeClusters(null, seedListDataObject.events);
				break;
			}
		}
		// merge of centre with all neighbours
		for (int i = 0; i < seedList.size(); i++) {
			EventList seedListDataObject = seedList.get(i);
			if (!seedListDataObject.equals(dataObject)) {
				mergeClusters(((EventList) dataObject).events, seedListDataObject.events);
				seedListDataObject.setClusterLabel(1);
			}
		}

		/** Iterate the seedList of the startDataObject */
		for (int j = 0; j < seedList.size(); j++) {
			DataObject seedListDataObject = seedList.get(j);
			List<EventList> seedListDataObject_Neighbourhood = database.epsilonRangeQuery(getEpsilon(), seedListDataObject);
			size = 0;
			for (int i = 0; i < seedListDataObject_Neighbourhood.size(); i++) {
				size += seedListDataObject_Neighbourhood.get(i).events.size();
			}

			/** seedListDataObject is coreObject */
			if (size >= getMinPoints()) {
				((EventList) seedListDataObject).processed = true;
				((EventList) seedListDataObject).setClusterLabel(1);
				for (int i = 0; i < seedListDataObject_Neighbourhood.size(); i++) {
					EventList p = seedListDataObject_Neighbourhood.get(i);
					if (p.getClusterLabel() != DataObject.UNCLASSIFIED) {
						continue;
					}

					if (p.getClusterLabel() != DataObject.NOISE) {
						seedList.add(p);
					} else {
						//p.setClusterLabel(DataObject.NOISE);
						//continue;
					}
					// p.setClusterLabel(clusterID);
					System.out.println("merge " + proc++ + " i:" + i + " j:" + j);
					mergeClusters(((EventList) seedListDataObject).events, p.events);
					p.setClusterLabel(1);
					// p.processed = true;

				}
			} else {
				((EventList) seedListDataObject).processed = true;
				((EventList) seedListDataObject).setClusterLabel(DataObject.NOISE);
			}
			seedList.remove(j);
			j--;
		}

		return true;
	}

	/**
	 * Classifies a given instance.
	 * 
	 * @param instance
	 *            The instance to be assigned to a cluster
	 * @return int The number of the assigned cluster as an integer
	 * @throws java.lang.Exception
	 *             If instance could not be clustered successfully
	 */
	@Override
	public int clusterInstance(Instance instance) throws Exception {
		if (processed_InstanceID >= database.size()) {
			processed_InstanceID = 0;
		}
		int cnum = ((Event) (database.getDataObject(Integer.toString(processed_InstanceID++)))).c.label;
		if (cnum == DataObject.NOISE)
			// throw new Exception();
			return -1;
		else
			return cnum;
	}

	/**
	 * Returns the number of clusters.
	 * 
	 * @return int The number of clusters generated for a training dataset.
	 * @throws java.lang.Exception
	 *             if number of clusters could not be returned successfully
	 */
	@Override
	public int numberOfClusters() throws Exception {
		return numberOfGeneratedClusters;
	}

	/**
	 * Returns an enumeration of all the available options..
	 * 
	 * @return Enumeration An enumeration of all available options.
	 */
	@Override
	public Enumeration listOptions() {
		Vector vector = new Vector();

		vector.addElement(new Option("\tepsilon (default = 0.9)", "E", 1, "-E <double>"));
		vector.addElement(new Option("\tminPoints (default = 6)", "M", 1, "-M <int>"));
		vector.addElement(new Option("\tindex (database) used for DBScan (default = weka.clusterers.forOPTICSAndDBScan.Databases.SequentialDatabase)", "I", 1, "-I <String>"));
		vector.addElement(new Option("\tdistance-type (default = weka.clusterers.forOPTICSAndDBScan.DataObjects.EuclidianDataObject)", "D", 1, "-D <String>"));
		return vector.elements();
	}

	/**
	 * Sets the OptionHandler's options using the given list. All options will
	 * be set (or reset) during this call (i.e. incremental setting of options
	 * is not possible).
	 * <p/>
	 * 
	 * <!-- options-start --> Valid options are:
	 * <p/>
	 * 
	 * <pre>
	 * -E &lt;double&gt;
	 *  epsilon (default = 0.9)
	 * </pre>
	 * 
	 * <pre>
	 * -M &lt;int&gt;
	 *  minPoints (default = 6)
	 * </pre>
	 * 
	 * <pre>
	 * -I &lt;String&gt;
	 *  index (database) used for DBScan (default = weka.clusterers.forOPTICSAndDBScan.Databases.SequentialDatabase)
	 * </pre>
	 * 
	 * <pre>
	 * -D &lt;String&gt;
	 *  distance-type (default = weka.clusterers.forOPTICSAndDBScan.DataObjects.EuclidianDataObject)
	 * </pre>
	 * 
	 * <!-- options-end -->
	 * 
	 * @param options
	 *            The list of options as an array of strings
	 * @throws java.lang.Exception
	 *             If an option is not supported
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
	 * Gets the current option settings for the OptionHandler.
	 * 
	 * @return String[] The list of current option settings as an array of
	 *         strings
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
	 * Returns a new Class-Instance of the specified database
	 * 
	 * @param database_Type
	 *            String of the specified database
	 * @param instances
	 *            Instances that were delivered from WEKA
	 * @return Database New constructed Database
	 */
	public Database databaseForName(String database_Type, Instances instances) {
		Object o = null;

		Constructor co = null;
		try {
			co = (Class.forName(database_Type)).getConstructor(new Class[] { Instances.class });
			o = co.newInstance(new Object[] { instances });
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

		return (Database) o;
	}

	/**
	 * Returns a new Class-Instance of the specified database
	 * 
	 * @param database_distanceType
	 *            String of the specified distance-type
	 * @param instance
	 *            The original instance that needs to hold by this DataObject
	 * @param key
	 *            Key for this DataObject
	 * @param database
	 *            Link to the database
	 * @return DataObject New constructed DataObject
	 */
	public DataObject dataObjectForName(String database_distanceType, Instance instance, String key, Database database) {
		Object o = null;

		Constructor co = null;
		try {
			co = (Class.forName(database_distanceType)).getConstructor(new Class[] { Instance.class, String.class, Database.class });
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
	 * Sets a new value for minPoints
	 * 
	 * @param minPoints
	 *            MinPoints
	 */
	public void setMinPoints(int minPoints) {
		this.minPoints = minPoints;
	}

	/**
	 * Sets a new value for epsilon
	 * 
	 * @param epsilon
	 *            Epsilon
	 */
	public void setEpsilon(double epsilon) {
		this.epsilon = epsilon;
	}

	/**
	 * Returns the value of epsilon
	 * 
	 * @return double Epsilon
	 */
	public double getEpsilon() {
		return epsilon;
	}

	/**
	 * Returns the value of minPoints
	 * 
	 * @return int MinPoints
	 */
	public int getMinPoints() {
		return minPoints;
	}

	/**
	 * Returns the distance-type
	 * 
	 * @return String Distance-type
	 */
	public String getDatabase_distanceType() {
		return database_distanceType;
	}

	/**
	 * Returns the type of the used index (database)
	 * 
	 * @return String Index-type
	 */
	public String getDatabase_Type() {
		return database_Type;
	}

	/**
	 * Sets a new distance-type
	 * 
	 * @param database_distanceType
	 *            The new distance-type
	 */
	public void setDatabase_distanceType(String database_distanceType) {
		this.database_distanceType = database_distanceType;
	}

	/**
	 * Sets a new database-type
	 * 
	 * @param database_Type
	 *            The new database-type
	 */
	public void setDatabase_Type(String database_Type) {
		this.database_Type = database_Type;
	}

	/**
	 * Returns the tip text for this property
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String epsilonTipText() {
		return "radius of the epsilon-range-queries";
	}

	/**
	 * Returns the tip text for this property
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String minPointsTipText() {
		return "minimun number of DataObjects required in an epsilon-range-query";
	}

	/**
	 * Returns the tip text for this property
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String database_TypeTipText() {
		return "used database";
	}

	/**
	 * Returns the tip text for this property
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String database_distanceTypeTipText() {
		return "used distance-type";
	}

	/**
	 * Returns a string describing this DataMining-Algorithm
	 * 
	 * @return String Information for the gui-explorer
	 */
	public String globalInfo() {
		return getTechnicalInformation().toString();
	}

	/**
	 * Returns an instance of a TechnicalInformation object, containing detailed
	 * information about the technical background of this class, e.g., paper
	 * reference or book this class is based on.
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

	public Database getDatabase() {
		return database;
	}

	/**
	 * Returns a description of the clusterer
	 * 
	 * @return a string representation of the clusterer
	 */
	@Override
	public String toString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("DBScan clustering results\n" + "========================================================================================\n\n");
		stringBuffer.append("Clustered DataObjects: " + database.size() + "\n");
		stringBuffer.append("Number of attributes: " + database.getInstances().numAttributes() + "\n");
		stringBuffer.append("Epsilon: " + getEpsilon() + "; minPoints: " + getMinPoints() + "\n");
		stringBuffer.append("Index: " + getDatabase_Type() + "\n");
		stringBuffer.append("Distance-type: " + getDatabase_distanceType() + "\n");
		stringBuffer.append("Number of generated clusters: " + numberOfGeneratedClusters + "\n");
		DecimalFormat decimalFormat = new DecimalFormat(".##");
		stringBuffer.append("Elapsed time: " + decimalFormat.format(elapsedTime) + "\n\n");

		for (int i = 0; i < database.size(); i++) {
			DataObject dataObject = database.getDataObject(Integer.toString(i));
			stringBuffer.append("" + dataObject.getKey() + "," + dataObject.toString() + "," + ((dataObject.getClusterLabel() == DataObject.NOISE) ? "NOISE\n" : dataObject.getClusterLabel() + "\n"));
		}
		return stringBuffer.toString() + "\n";
	}

	/**
	 * Returns the revision string.
	 * 
	 * @return the revision
	 */
	@Override
	public String getRevision() {
		return RevisionUtils.extract("$Revision: 1.1.2.1 $");
	}

	/**
	 * 
	 */
	public TOGDBSCAN() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
