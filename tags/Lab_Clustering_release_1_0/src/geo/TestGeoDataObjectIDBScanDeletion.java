package geo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import weka.clusterers.ClusterEvaluation;
import weka.clusterers.DBScan;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import clusterers.IDBScan;

/**
 * @author Haolin Zhi
 *
 */
public class TestGeoDataObjectIDBScanDeletion {

	private static IDBScan idbscan;
	private static DBScan dbscan;
	
	/**
	 * initialization
	 */
	static {
		// Incremental DBSCAN Clusterer
		idbscan = new IDBScan();
		dbscan = new DBScan();

		// set database type
//		clusterer.setDatabase_Type("clusterers.database.CachedSpatialIndexDatabase");
		idbscan.setDatabase_Type("clusterers.database.InsertCachedSpatialIndexDatabase");
//		clusterer.setDatabase_Type("clusterers.database.CachedSequentialDatabase");
//		clusterer.setDatabase_Type("clusterers.database.InsertCachedSequentialDatabase");
//		clusterer.setDatabase_Type("clusterers.database.InsertCachedKDTreeDatabase");
//		clusterer.setDatabase_Type("weka.clusterers.forOPTICSAndDBScan.Databases.SequentialDatabase");

		// set objects type to be clustered
		dbscan.setDatabase_distanceType("geo.GeoDataObject");
		idbscan.setDatabase_distanceType("geo.GeoDataObject");

		// set minimum neighborhood radius
		dbscan.setEpsilon(300);
		idbscan.setEpsilon(300);

		// set minimum number of neighbors
		dbscan.setMinPoints(30);
		idbscan.setMinPoints(30);
	}

	public static void main(String[] args) throws Exception {
		// load the csv file
//		CSVLoader loader = new CSVLoader();
//		loader.setFile(new File("data/berlin_sample_positions.csv"));

		// load the arff file
		ArffLoader loader = new ArffLoader();
		loader.setFile(new File("data/berlin_sub.arff"));

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

		// 	clusters the first half data using DBSCAN
		bulidClustererByDBScan(subInstances1);

		// clusters the second half data using Incremental DBSCAN
		bulidClustererByIDBScan(totalInstances);

		evaluateResult(subInstances1);

		System.out.println(subInstances1.numInstances() + " Instances are clustered by DBScan");
		System.out.println(subInstances2.numInstances() + " Instances are clustered by IDBScan");
	}

	/**
	 * evaluate the clustering result and store the result to a file 
	 * @param totalInstances
	 * @throws Exception
	 * @throws IOException
	 */
	private static void evaluateResult(Instances totalInstances) throws Exception, IOException {
		ClusterEvaluation eval = new ClusterEvaluation();
		eval.setClusterer(dbscan);
		eval.evaluateClusterer(totalInstances);
		System.out.println(eval.clusterResultsToString());
		
		
		ClusterEvaluation eval2 = new ClusterEvaluation();
		eval2.setClusterer(idbscan);
		eval2.evaluateClusterer(totalInstances);
		System.out.println(eval2.clusterResultsToString());

		// store the result to a file
		BufferedWriter writer = new BufferedWriter(new FileWriter("output/berlin_clusters_idbscan.csv"));
		writer.write("id,Name,Cluster\n");
		double[] res = eval.getClusterAssignments();

		for (int i = 0; i < res.length; i++) {
			if (res[i] == -1) {
				writer.write(i + "," + i + ",\n");
			} else {
				writer.write(i + "," + i + ",class " + (int) (res[i] + 1) + "\n");
			}
		}
		writer.close();

		writer = new BufferedWriter(new FileWriter("output/berlin_clusters_idbscan.arff"));
		writer.write("@relation results \n \n");
		writer.write("@attribute x numeric \n");
		writer.write("@attribute y numeric \n");
		writer.write("@attribute c numeric \n \n");
		writer.write("@data \n");
		for (int i = 0; i < res.length; i++) {
			int xIndex = totalInstances.instance(i).numValues() - 2;
			int yIndex = totalInstances.instance(i).numValues() - 1;
			writer.write(totalInstances.instance(i).value(xIndex) + "," + totalInstances.instance(i).value(yIndex) + "," + res[i] + " \n");
		}
		writer.close();
		
		writer = new BufferedWriter(new FileWriter("output/berlin_info_idbscan.txt"));
		writer.write(eval.clusterResultsToString());
		writer.close();
	}

	/**
	 * clusters the instances using DBSCAN
	 * @param instances
	 * @throws Exception
	 */
	private static void bulidClustererByDBScan(Instances instances) throws Exception {
		dbscan.buildClusterer(instances);
	}

	/**
	 * clusters the instances using Incremental DBSCAN
	 * @param instances
	 * @throws Exception
	 */
	private static void bulidClustererByIDBScan(Instances totalInstances) throws Exception {
		idbscan.buildClusterer(totalInstances);
		for (int i = totalInstances.numInstances()-1; i >= totalInstances.numInstances()/2; i--) {
			idbscan.deleteInstance(String.valueOf(i));
		}
		idbscan.updateFinished();
	}
}