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
public class TestGeoDataObjectDBScan {

	public static void main(String[] args) {
		try {
			// load the arff file
			ArffLoader loader = new ArffLoader();
			loader.setFile(new File("data/berlin_sub.arff"));
//			loader.setFile(new File("data/berlin_sub_2500.arff"));
//			loader.setFile(new File("data/berlin_sub_testt.arff"));
//			loader.setFile(new File("data/berlin_subt.arff"));

			// get the instances from the arff file
			Instances insts = loader.getDataSet();

			// new clusterer
			DBScan clusterer = new DBScan();
			// set objects to be clustered
			clusterer.setDatabase_distanceType("geo.GeoDataObject");
			// set minimum neighbourhood radius
			clusterer.setEpsilon(300);
			// set minimum number of neighbours
			clusterer.setMinPoints(30);
			// generate clusters
			clusterer.buildClusterer(insts);

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
				if(res[i]!=-1)
				writer.write(insts.instance(i).valueSparse(0) + "," + insts.instance(i).valueSparse(1) + "," + res[i] + " \n");
			}
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}