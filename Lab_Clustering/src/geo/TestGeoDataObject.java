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
 * @author Haolin Zhi
 *
 */
public class TestGeoDataObject {

	public static void main(String[] args) {
		try {
			// load the arff file
			ArffLoader loader = new ArffLoader();
			loader.setFile(new File("data/berlin_sub.arff"));
			loader.setSource(new File("data/berlin_sub.arff"));
			// get the instances from the arff file
			Instances insts = loader.getDataSet();
			// new clusterer
			DBScan clusterer = new DBScan();
			// set objects to be clustered
			clusterer.setDatabase_distanceType("geo.GeoDataObject");
			// set minimum neighbourhood radius
			clusterer.setEpsilon(300);
			// set minimum number of neighbours
			clusterer.setMinPoints(2);
			// generate clusters
			clusterer.buildClusterer(insts);
			// new evaluation for the clustering
			ClusterEvaluation eval = new ClusterEvaluation();
			eval.setClusterer(clusterer);
			eval.evaluateClusterer(insts);
			System.out.println(eval.clusterResultsToString());
			BufferedWriter writer = new BufferedWriter(new FileWriter("clustering.arff"));
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