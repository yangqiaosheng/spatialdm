package geo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import weka.clusterers.ClusterEvaluation;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.CSVLoader;
import clusterers.IDBScan;

/**
 * @author Haolin Zhi
 *
 */
public class TestGeoDataObjectIDBScan {

	private static IDBScan clusterer;

	/**
	 * initialization
	 */
	static {
		// Incremental DBSCAN Clusterer
		clusterer = new IDBScan();

		// set database type
//		clusterer.setDatabase_Type("clusterers.database.CachedSpatialIndexDatabase");
		clusterer.setDatabase_Type("clusterers.database.InsertCachedSpatialIndexDatabase");
//		clusterer.setDatabase_Type("clusterers.database.CachedSequentialDatabase");
//		clusterer.setDatabase_Type("clusterers.database.InsertCachedSequentialDatabase");
//		clusterer.setDatabase_Type("weka.clusterers.forOPTICSAndDBScan.Databases.SequentialDatabase");
		// set objects type to be clustered
		clusterer.setDatabase_distanceType("geo.GeoDataObject");

		// set minimum neighborhood radius
		clusterer.setEpsilon(300);

		// set minimum number of neighbors
		clusterer.setMinPoints(8);
	}

	public static void main(String[] args) throws Exception {
		// load the csv file
//		CSVLoader loader = new CSVLoader();
//		loader.setFile(new File("data/berlin_sample_positions.csv"));
		
		ArffLoader loader = new ArffLoader();
		loader.setFile(new File("data/berlin.arff"));

		// get the instances from the file
		Instances totalInstances = loader.getDataSet();

		// split the total instances into 2 subsets

		Instances subInstances1 = new Instances(totalInstances, 0, 0);

		Instances subInstances2 = new Instances(totalInstances, 0, 0);

		for (int i = 0; i < totalInstances.numInstances(); i++) {
			if (i < totalInstances.numInstances() / 2) {
				subInstances1.add(totalInstances.instance(i));
			} else {
				subInstances2.add(totalInstances.instance(i));
			}
		}

		// long start = System.currentTimeMillis();

		// 		clusters the first half data using DBSCAN
		bulidClustererByDBScan(subInstances1);

		// long middle = System.currentTimeMillis();

		// clusters the second half data using Incremental DBSCAN
		bulidClustererByIDBScan(subInstances2);

		// long end = System.currentTimeMillis();

		evaluateResult(totalInstances);

		System.out.println(subInstances1.numInstances() + " Instances are clustered by DBScan");
		// System.out.println("Elapsed time for DBScan: " + (middle - start) / 1000.0);
		System.out.println(subInstances2.numInstances() + " Instances are clustered by IDBScan");
		// System.out.println("Elapsed time for IDBScan: " + (end - middle) / 1000.0);

	}

	/**
	 * evaluate the clustering result and store the result to a file 
	 * @param totalInstances
	 * @throws Exception
	 * @throws IOException
	 */
	private static void evaluateResult(Instances totalInstances) throws Exception, IOException {
		ClusterEvaluation eval = new ClusterEvaluation();
		eval.setClusterer(clusterer);
		eval.evaluateClusterer(totalInstances);
		System.out.println(eval.clusterResultsToString());

		// store the result to a file
		BufferedWriter writer = new BufferedWriter(new FileWriter("output/berlin_sample_clusters.csv"));
		writer.write("id,Name,Cluster\n");
		double[] res = eval.getClusterAssignments();
		for (int i = 0; i < res.length; i++) {
			if (res[i] == -1) {
				writer.write((int) totalInstances.instance(i).value(0) + "," + (int) totalInstances.instance(i).value(1) + ",\n");
			} else {
				writer.write((int) totalInstances.instance(i).value(0) + "," + (int) totalInstances.instance(i).value(1) + ",class " + (int) (res[i] + 1) + "\n");
			}
		}
		writer.close();
	}

	/**
	 * clusters the instances using DBSCAN
	 * @param instances
	 * @throws Exception
	 */
	private static void bulidClustererByDBScan(Instances instances) throws Exception {
		clusterer.buildClusterer(instances);
	}

	/**
	 * clusters the instances using Incremental DBSCAN
	 * @param instances
	 * @throws Exception
	 */
	private static void bulidClustererByIDBScan(Instances instances) throws Exception {
		for (int i = 0; i < instances.numInstances(); i++) {
			clusterer.updateClusterer(instances.instance(i));
		}
		clusterer.updateFinished();
	}
}