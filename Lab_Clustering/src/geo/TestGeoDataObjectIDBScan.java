package geo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import clusterers.IDBScan;

import weka.clusterers.ClusterEvaluation;
import weka.clusterers.DBScan;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ArffLoader;

/**
 * @author Haolin Zhi
 *
 */
public class TestGeoDataObjectIDBScan {

	public static void main(String[] args) {
		try {
			// load the arff file
			ArffLoader loader = new ArffLoader();
			loader.setFile(new File("data/berlin_sub_test1.arff"));
//			loader.setFile(new File("data/berlin_sub_testt.arff"));

			// get the instances from the arff file
			Instances insts = loader.getDataSet();

			// new clusterer
			IDBScan clusterer = new IDBScan();
			// set objects to be clustered
			clusterer.setDatabase_distanceType("geo.GeoDataObject");
			// set minimum neighbourhood radius
			clusterer.setEpsilon(300);
			// set minimum number of neighbours
			clusterer.setMinPoints(8);
			// generate clusters
			clusterer.buildClusterer(insts);

			
			ArffLoader loader2 = new ArffLoader();
			loader2.setFile(new File("data/berlin_sub_test2.arff"));
			Instances insts2 = loader2.getDataSet();
			clusterer.insert(insts2);
			
			// new evaluation for the clustering
			for (int i = 0; i < insts2.numInstances(); i++) {
				insts.add(insts2.instance(i));
			}
			ClusterEvaluation eval = new ClusterEvaluation();
			eval.setClusterer(clusterer);
			eval.evaluateClusterer(insts);
			System.out.println(eval.clusterResultsToString());

			BufferedWriter writer = new BufferedWriter(new FileWriter("output/berlin_sub_clustering.arff"));
			writer.write("@relation results \n \n");
			writer.write("@attribute x numeric \n");
			writer.write("@attribute y numeric \n");
			writer.write("@attribute c numeric \n \n");
			writer.write("@data \n");
			double[] res = eval.getClusterAssignments();
			for (int i = 0; i < res.length; i++) {
				writer.write(insts.instance(i).valueSparse(0) + "," + insts.instance(i).valueSparse(1) + "," + res[i] + " \n");
			}
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}