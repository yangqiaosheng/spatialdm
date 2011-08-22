package spade.analysis.tools;

import java.awt.Color;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.lib.color.CS;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataTable;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.SimpleKMeans;
import weka.core.Instances;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Oct 30, 2009
 * Time: 5:00:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class WekaSimpleKMeans {

	protected Instances instances = null;
	protected int tblRowsInInstances[] = null;
	protected ESDACore core = null;
	protected AttributeDataPortion tbl = null;
	protected int attrIdx = -1;

	public Instances clusterCentroids = null;

	public WekaSimpleKMeans(Instances instances, int tblRowsInInstances[], ESDACore core, AttributeDataPortion tbl, Vector attrIds, int nclasses) {
		this.instances = instances;
		this.core = core;
		this.tbl = tbl;
		this.tblRowsInInstances = tblRowsInInstances;
		runClusterer(nclasses);
	}

	protected void runClusterer(int nclasses) {
		double results[];
		core.getUI().showMessage("Clustering in progress...", false);
		long t0 = System.currentTimeMillis();
		results = runSimpleKMeans(nclasses);
		long t = System.currentTimeMillis();
		System.out.println("The clustering took " + (t - t0) + " msec., " + ((t - t0) / 1000f) + " sec.");
		core.getUI().showMessage("Clustering finished; putting the results in the table", false);
		DataTable dTable = (DataTable) tbl;
		int i = 0;
		String attrName = "";
		do {
			i++;
			attrName = "Cluster " + i;
		} while (dTable.findAttrByName(attrName) >= 0);
		dTable.addAttribute(attrName, null, AttributeTypes.character);
		attrIdx = dTable.getAttrCount() - 1;
		updateTable(dTable, nclasses, results);
	}

	protected double[] runSimpleKMeans(int nclasses) {
		Instances sInstances = instances;
		System.out.println("* SimpleKMeans Clusterer: Start");
		SimpleKMeans clusterer = new SimpleKMeans();
		ClusterEvaluation eval = new ClusterEvaluation();
		try {
			clusterer.setNumClusters(nclasses);
			clusterer.setSeed(10);
			clusterer.buildClusterer(sInstances);
			eval.setClusterer(clusterer);
			eval.evaluateClusterer(sInstances);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("* SimpleKMeans Clusterer: Finish");
		double d[] = eval.getClusterAssignments();
		clusterCentroids = clusterer.getClusterCentroids();
		return d;
	}

	private void updateTable(DataTable table, int nclasses, double[] results) {
		spade.vis.database.Attribute cgattr = table.getAttribute(attrIdx);
		String valueList[] = new String[nclasses];
		Color valueColors[] = new Color[nclasses];
		for (int i = 0; i < nclasses; i++) {
			valueList[i] = "cluster " + (i + 1);
			if (i < CS.niceColors.length) {
				valueColors[i] = CS.getNiceColor(i);
			} else if (i < CS.niceColors.length * 3) {
				valueColors[i] = valueColors[i - CS.niceColors.length].darker();
			} else {
				valueColors[i] = Color.getHSBColor((float) Math.random(), (float) Math.max(Math.random(), 0.5), (float) Math.max(Math.random(), 0.5));
			}
		}
		cgattr.setValueListAndColors(valueList, valueColors);
		for (int i = 0; i < results.length; i++)
			if (results[i] >= 0 && results[i] < nclasses) {
				table.getDataRecord(tblRowsInInstances[i]).setAttrValue(valueList[(int) Math.round(results[i])], attrIdx);
			} else {
				table.getDataRecord(tblRowsInInstances[i]).setAttrValue(null, attrIdx);
			}
	}

}
