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
import weka.core.converters.CSVLoader;

/**
 * @author Haolin Zhi
 *
 */
public class TestGeoDataObjectDBScan {

	public static void main(String[] args) {
		try {
			// load the arff file
			CSVLoader loader = new CSVLoader();
			loader.setFile(new File("data/berlin_sample_positions.csv"));

			// get the instances from the arff file
			Instances insts = loader.getDataSet();

			// new clusterer
			DBScan clusterer = new DBScan();
			// set database type
			// clusterer.setDatabase_Type("clusterers.kdtree.GeoKDTreeDatabase");
			// set objects type to be clustered
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

			// store the result to a file
			BufferedWriter writer = new BufferedWriter(new FileWriter("output/berlin_sample_clusters.csv"));
			writer.write("id,Name,Cluster\n");
			double[] res = eval.getClusterAssignments();
			for (int i = 0; i < res.length; i++) {
				if (res[i] == -1) {
					writer.write((int) insts.instance(i).value(0) + "," + (int) insts.instance(i).value(1) + ",\n");
				} else {
					writer.write((int) insts.instance(i).value(0) + "," + (int) insts.instance(i).value(1) + ",class " + (int) (res[i] + 1) + "\n");
				}
			}
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}