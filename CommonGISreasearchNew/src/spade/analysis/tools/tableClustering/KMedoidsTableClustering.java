package spade.analysis.tools.tableClustering;

import java.util.ArrayList;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.DataAnalyser;
import spade.lib.util.FloatArray;
import spade.lib.util.IntArray;
import spade.vis.database.AttributeDataPortion;
import ui.AttributeChooser;
import ui.TableManager;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Jan 2, 2009
 * Time: 2:58:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class KMedoidsTableClustering implements DataAnalyser {

	protected String attrNames[] = null;
	protected float attrRanges[] = null;

	//Men�punkt erscheint nur, wenn auch weka definiert ist.
	@Override
	public boolean isValid(ESDACore core) {
		try {
			return (null != Class.forName("it.unipi.di.sax.kmedoids.KMedoids"));
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public void run(ESDACore core) {
		System.out.println("Table Data Clustering by KMedoids");
		AttributeDataPortion tbl = selectTable(core);
		if (tbl == null)
			return;
		ArrayList<FloatArray> data = prepareFlatTable(core, tbl);
		if (data != null) {
			new KMedoidsTableClusteringCP(core, tbl, data, attrNames, attrRanges);
		}
	}

	protected AttributeDataPortion selectTable(ESDACore core) {
		DataKeeper dk = core.getDataKeeper();
		if (dk.getTableCount() < 1)
			return null;
		TableManager tman = new TableManager();
		tman.setDataKeeper(dk);
		tman.setUI(core.getUI());
		int tn = tman.selectTableNumber("Select Table");
		if (tn < 0)
			return null;
		return dk.getTable(tn);
	}

	/**
	 * Method builds Vector of FloatArrays from CommonGIS table
	 * For nominal attributes value lists and colours are created
	 */
	protected ArrayList<FloatArray> prepareFlatTable(ESDACore core, AttributeDataPortion tbl) {
		ArrayList<FloatArray> data = null;
		AttributeChooser attrSel = new AttributeChooser();
		attrSel.selectColumns(tbl, null, null, true, "Select Attributes", core.getUI());
		Vector attr = attrSel.getSelectedColumnIds();
		if (attr == null)
			return null;
		float min[] = new float[attr.size()], max[] = new float[attr.size()];
		for (int i = 0; i < min.length; i++) {
			min[i] = max[i] = Float.NaN;
		}
		IntArray attrNumbers = null;
		if (attr != null && attr.size() > 0 && tbl != null && tbl.getAttrCount() > 0) {
			attrNames = new String[attr.size()];
			attrNumbers = new IntArray();
			for (int i = 0; i < attr.size(); i++) {
				int idx = tbl.getAttrIndex((String) attr.elementAt(i));
				if (idx >= 0) {
					attrNumbers.addElement(idx);
					attrNames[i] = tbl.getAttributeName(idx);
				}
			}
		}

		data = new ArrayList<FloatArray>();
		for (int i = 0; i < tbl.getDataItemCount(); i++)
			if (tbl.getObjectFilter() == null || tbl.getObjectFilter().isActive(i)) {
				FloatArray fa = new FloatArray(1 + attrNumbers.size(), 10);
				fa.addElement(i);
				for (int j = 0; j < attrNumbers.size(); j++) {
					int attrN = attrNumbers.elementAt(j);
					if (tbl.isAttributeNumeric(attrN) || tbl.isAttributeTemporal(attrN)) {
						float f = (float) tbl.getNumericAttrValue(attrN, i);
						if (Float.isNaN(min[j]) || min[j] > f) {
							min[j] = f;
						}
						if (Float.isNaN(max[j]) || max[j] < f) {
							max[j] = f;
						}
						fa.addElement(f);
					}
				}
				data.add(fa);
			}
		attrRanges = new float[attr.size()];
		for (int i = 0; i < min.length; i++) {
			attrRanges[i] = max[i] - min[i];
		}

		return data;
	}

}
