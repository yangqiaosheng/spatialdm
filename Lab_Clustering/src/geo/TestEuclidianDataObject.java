package geo;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import weka.clusterers.ClusterEvaluation;
import weka.clusterers.DBScan;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

/**
 * @author Admin
 *
 */
public class TestEuclidianDataObject {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			// load the arff file
			ArffLoader loader = new ArffLoader();
			loader.setFile(new File("data/sample.arff"));
			loader.setSource(new File("data/sample.arff"));
			// get the instances from the arff file
			Instances insts = loader.getDataSet();
			// new clusterer
			DBScan clusterer = new DBScan();
			// set objects to be clustered
//			clusterer.setDatabase_distanceType("GeoObject");
			// set minimum neighbourhood radius
			clusterer.setEpsilon(0.2);
			// set minimum number of neighbours
			clusterer.setMinPoints(2);
			// generate clusters
			clusterer.buildClusterer(insts);
			// new evaluation for the clustering
			ClusterEvaluation eval = new ClusterEvaluation();
			eval.setClusterer(clusterer);
			eval.evaluateClusterer(insts);
			System.out.println(eval.clusterResultsToString());
			BufferedWriter writer = new BufferedWriter(new FileWriter("output/simple_clustering.arff"));
			writer.write("@relation results \n \n");
			writer.write("@attribute x numeric \n");
			writer.write("@attribute y numeric \n");
			writer.write("@attribute c numeric \n \n");
			writer.write("@data \n");
			double[] res = eval.getClusterAssignments();
			for (int i=0; i<res.length; i++) {
				writer.write(insts.instance(i).valueSparse(0) +"," + insts.instance(i).valueSparse(1) + 
						"," + res[i] + " \n");
			}
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
