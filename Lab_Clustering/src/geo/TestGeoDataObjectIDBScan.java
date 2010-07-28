package geo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import clusterers.IDBScan;

import weka.clusterers.ClusterEvaluation;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

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
		//Incremental DBSCAN Clusterer
		clusterer = new IDBScan();

		// set objects to be clustered
		clusterer.setDatabase_distanceType("geo.GeoDataObject");

		// set minimum neighborhood radius
		clusterer.setEpsilon(300);

		// set minimum number of neighbors
		clusterer.setMinPoints(30);
	}

	public static void main(String[] args) throws Exception {
		// load the arff file
		ArffLoader loader = new ArffLoader();
		loader.setFile(new File("data/berlin_sub.arff"));
		//			loader.setFile(new File("data/berlin_sub2.arff"));
		//			loader.setFile(new File("data/berlin_sub_2500.arff"));

		// get the instances from the arff file
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

//		long start = System.currentTimeMillis();
		
		// clusters the first half data using DBSCAN
		bulidClustererByDBScan(subInstances1);
		
//		long middle = System.currentTimeMillis();
		
		// clusters the second half data using Incremental DBSCAN
		bulidClustererByIDBScan(subInstances2);

//		long end = System.currentTimeMillis();
		
		evaluateResult(totalInstances);

		System.out.println(subInstances1.numInstances() + " Instances are clustered by DBScan");
//		System.out.println("Elapsed time for DBScan: " + (middle - start) / 1000.0);
		System.out.println(subInstances2.numInstances() + " Instances are clustered by IDBScan");
//		System.out.println("Elapsed time for IDBScan: " + (end - middle) / 1000.0);

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

		//store the result to a file
		BufferedWriter writer = new BufferedWriter(new FileWriter("output/berlin_sub_clustering.arff"));
		writer.write("@relation results \n \n");
		writer.write("@attribute x numeric \n");
		writer.write("@attribute y numeric \n");
		writer.write("@attribute c numeric \n \n");
		writer.write("@data \n");
		double[] res = eval.getClusterAssignments();
		for (int i = 0; i < res.length; i++) {
			//			if (res[i] != -1) 	escape all the noise
			writer.write(totalInstances.instance(i).valueSparse(0) + "," + totalInstances.instance(i).valueSparse(1) + "," + res[i] + " \n");
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