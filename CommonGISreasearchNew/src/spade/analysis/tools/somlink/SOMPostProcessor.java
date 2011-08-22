package spade.analysis.tools.somlink;

import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.tools.WekaKnowledgeExplorer;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.ObjectFilter;
import useSOM.SOMCellInfo;
import useSOM.SOMResult;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Nov 2, 2009
 * Time: 11:07:52 AM
 * Operations with SOM results, in particular, clustering of SOM cells
 */
public class SOMPostProcessor {
	/**
	 * Makes a temporary table with data describing the neurons of the SOM cells.
	 * This table can be used for k-means clustering in Weka.
	 */
	public static DataTable makeTableWithSOMNeuronsData(SOMApplInfo somapi) {
		if (somapi == null || somapi.selAttrs == null || somapi.tblSOMResult == null || somapi.somRes == null)
			return null;
		SOMResult sr = somapi.somRes;
		if (sr.xdim < 1 || sr.ydim < 1)
			return null;
		double nrData[] = null;
		for (int y = 0; y < sr.ydim && nrData == null; y++) {
			for (int x = 0; x < sr.xdim && nrData == null; x++) {
				nrData = sr.cellInfos[x][y].neuronFV;
			}
		}
		if (nrData == null)
			return null;
		DataTable tbl = new DataTable();
		tbl.addAttribute("x", "x", AttributeTypes.integer);
		tbl.addAttribute("y", "y", AttributeTypes.integer);
		int aidx0 = tbl.getAttrCount();
		for (int i = 0; i < nrData.length; i++) {
			int aIdx = aidx0 + i + 1;
			tbl.addAttribute("Feature " + aIdx, "f" + aIdx, AttributeTypes.real);
		}
		int cellIdx = 0;
		for (int y = 0; y < sr.ydim; y++) {
			for (int x = 0; x < sr.xdim; x++)
				if (sr.cellInfos[x][y].nObj > 0) {
					nrData = sr.cellInfos[x][y].neuronFV;
					if (nrData != null) {
						String id = String.valueOf(cellIdx + 1);
						DataRecord rec = new DataRecord(id);
						tbl.addDataRecord(rec);
						rec.setNumericAttrValue(x, String.valueOf(x), 0);
						rec.setNumericAttrValue(y, String.valueOf(y), 1);
						for (int i = 0; i < nrData.length; i++) {
							rec.setNumericAttrValue(nrData[i], String.valueOf(nrData[i]), aidx0 + i);
						}
						++cellIdx;
					}
				}
		}
		return tbl;
	}

	/**
	 * Makes a temporary table with data describing the prototypes from the SOM cells.
	 * This table can be used for k-means clustering in Weka.
	 */
	public static DataTable makeTableWithSOMProtoData(SOMApplInfo somapi) {
		if (somapi == null || somapi.selAttrs == null || somapi.tblSOMResult == null || somapi.somRes == null)
			return null;
		SOMResult sr = somapi.somRes;
		if (sr.xdim < 1 || sr.ydim < 1)
			return null;
		DataTable tbl = new DataTable();
		tbl.addAttribute("index", "idx", AttributeTypes.integer);
		tbl.addAttribute("x", "x", AttributeTypes.integer);
		tbl.addAttribute("y", "y", AttributeTypes.integer);
		IntArray colNs = null;
		int cNs[][] = null;
		ObjectFilter tFilter = null;
		int aidx0 = tbl.getAttrCount();
		if (somapi.applySOMtoParam) {
			tFilter = somapi.tblSOM.getObjectFilter();
			if (tFilter != null && !tFilter.areObjectsFiltered()) {
				tFilter = null;
			}
			cNs = new int[somapi.selAttrs.size()][somapi.paramSOM.getValueCount()];
			for (int i = 0; i < somapi.selAttrs.size(); i++) {
				for (int j = 0; j < somapi.paramSOM.getValueCount(); j++) {
					cNs[i][j] = -1;
				}
				Attribute at = somapi.selAttrs.elementAt(i);
				for (int j = 0; j < at.getChildrenCount(); j++) {
					Attribute child = at.getChild(j);
					int aIdx = somapi.tblSOM.getAttrIndex(child.getIdentifier());
					if (aIdx < 0) {
						continue;
					}
					Object pval = child.getParamValue(somapi.paramSOM.getName());
					int pvIdx = somapi.paramSOM.getValueIndex(pval);
					if (pvIdx >= 0) {
						cNs[i][pvIdx] = aIdx;
					}
				}
			}
			if (somapi.selAttrs.size() == 1) {
				for (int i = 0; i < somapi.tblSOM.getDataItemCount(); i++)
					if (tFilter == null || tFilter.isActive(i)) {
						tbl.addAttribute(somapi.tblSOM.getDataItemName(i), "at" + (tbl.getAttrCount() + 1), somapi.selAttrs.elementAt(0).getType());
					} else {
						;
					}
			} else {
				for (int k = 0; k < somapi.selAttrs.size(); k++) {
					Attribute at = somapi.selAttrs.elementAt(k);
					for (int i = 0; i < somapi.tblSOM.getDataItemCount(); i++)
						if (tFilter == null || tFilter.isActive(i)) {
							tbl.addAttribute(somapi.tblSOM.getDataItemName(i) + " (" + at.getIdentifier() + ")", "at" + (tbl.getAttrCount() + 1), at.getType());
						}
				}
			}
		} else {
			colNs = new IntArray(100, 100);
			for (int i = 0; i < somapi.selAttrs.size(); i++) {
				Attribute at = somapi.selAttrs.elementAt(i);
				if (!at.hasChildren()) {
					int idx = somapi.tblSOM.getAttrIndex(at.getIdentifier());
					if (idx >= 0) {
						colNs.addElement(idx);
						tbl.addAttribute(at.getName(), "at" + (tbl.getAttrCount() + 1), at.getType());
					}
				} else {
					for (int j = 0; j < at.getChildrenCount(); j++) {
						Attribute child = at.getChild(j);
						int idx = somapi.tblSOM.getAttrIndex(child.getIdentifier());
						if (idx >= 0) {
							colNs.addElement(idx);
							tbl.addAttribute(child.getName(), "at" + (tbl.getAttrCount() + 1), child.getType());
						}
					}
				}
			}
		}
		for (int y = 0; y < sr.ydim; y++) {
			for (int x = 0; x < sr.xdim; x++) {
				SOMCellInfo sci = sr.cellInfos[x][y];
				if (sci.nObj < 1 || sci.protoId < 1) {
					continue;
				}
				int oIdx = sci.protoId - 1;
				DataRecord origRec = (somapi.applySOMtoParam) ? somapi.tblSOMResult.getDataRecord(oIdx) : somapi.tblSOM.getDataRecord(oIdx);
				DataRecord rec = new DataRecord(origRec.getId(), origRec.getName());
				tbl.addDataRecord(rec);
				rec.setNumericAttrValue(oIdx, String.valueOf(oIdx), 0);
				rec.setNumericAttrValue(x, String.valueOf(x), 1);
				rec.setNumericAttrValue(y, String.valueOf(y), 2);
				if (somapi.applySOMtoParam) {
					int aidx = aidx0;
					for (int i = 0; i < somapi.tblSOM.getDataItemCount(); i++)
						if (tFilter == null || tFilter.isActive(i)) {
							for (int k = 0; k < somapi.selAttrs.size(); k++) {
								String val = "0";
								if (cNs[k][oIdx] >= 0) {
									val = somapi.tblSOM.getAttrValueAsString(cNs[k][oIdx], i);
									if (val == null) {
										val = "0";
									}
								}
								rec.setAttrValue(val, aidx++);
							}
						}
				} else {
					for (int j = 0; j < colNs.size(); j++) {
						String val = origRec.getAttrValueAsString(colNs.elementAt(j));
						if (val == null) {
							val = "0";
						}
						rec.setAttrValue(val, aidx0 + j);
					}
				}
			}
		}
		return tbl;
	}

	/**
	 * Applies k-means clustering from the Weka library to the given table.
	 * Runs the clustering algorithm several time for k = minNClusters, ..., maxNClusters.
	 * For each k creates a column in the table with the cluster labels
	 * and a column with the distances from the objects to the cluster centroids.
	 * The columns with the cluster labels come first, then the columns with the
	 * distances.
	 * Returns 2 indexes of table columns:
	 * 0) the 1st column with the cluster labels
	 * 1) the 1st column with the distances to the centroids
	 */
	public static int[] applyKMeans(ESDACore core, DataTable tbl, Vector attrIds, int minNClusters, int maxNClusters) {
		if (tbl == null || attrIds == null)
			return null;
		if (minNClusters < 2) {
			minNClusters = 2;
		}
		if (maxNClusters < minNClusters) {
			maxNClusters = minNClusters;
		}
		WekaKnowledgeExplorer wke = new WekaKnowledgeExplorer();
		int resColNs[] = new int[2];
		resColNs[0] = tbl.getAttrCount();
		Vector<double[][]> kMeansResult = wke.runWekaSimpleKMeans(core, tbl, attrIds, minNClusters, maxNClusters);
		//the result contains cluster centroids for k-means clustering with different k
		if (kMeansResult == null || kMeansResult.size() < 1)
			return null;
		resColNs[1] = tbl.getAttrCount();
		int cN[] = tbl.getAttrIndices(attrIds);
		for (int i = 0; i < kMeansResult.size(); i++) {
			double dCentroids[][] = kMeansResult.elementAt(i);
			int nCl = dCentroids.length;
			tbl.getAttribute(resColNs[0] + i).setName("k-means cluster (k=" + nCl + ")");
			int aIdx = tbl.getAttrCount();
			tbl.addAttribute("Distance to centroid for k=" + nCl, "at" + (aIdx + 1), AttributeTypes.real);
			double vals[] = new double[cN.length];
			for (int cIdx = 0; cIdx < nCl; cIdx++) {
				String cNstr = String.valueOf(cIdx + 1);
				for (int r = 0; r < tbl.getDataItemCount(); r++) {
					DataRecord rec = tbl.getDataRecord(r);
					if (rec.getAttrValueAsString(i + resColNs[0]).indexOf(cNstr) >= 0) {
						for (int k = 0; k < cN.length; k++) {
							vals[k] = rec.getNumericAttrValue(cN[k]);
						}
						double d = getMinkowskiDistance(vals, dCentroids[cIdx], 2);
						rec.setNumericAttrValue(d, StringUtil.doubleToStr(d, 5), aIdx);
					}
				}
			}
		}
		Vector<String> resultAttrs = new Vector<String>(tbl.getAttrCount() - resColNs[0], 1);
		for (int i = resColNs[0]; i < tbl.getAttrCount(); i++) {
			resultAttrs.addElement(tbl.getAttributeId(i));
		}
		tbl.notifyPropertyChange("new_attributes", null, resultAttrs);
		return resColNs;
	}

	/**
	 * Assuming that k-means clustering has been applied to the table with
	 * SOM neurons somInfo.tblSOMneuro, assigns the cluster labels of the neurons to
	 * all objects contained in the respective cells.
	 * The objects are destribed in the table somInfo.tblSOMResult.
	 * @param somInfo - contains information abou the SOM input and output
	 * @param neuroClColIdx0 - the index of the first column containing k-means cluster labels
	 *   in the table with SOM neurons
	 * @param neuroClColIdxLast - the maximum number of the k-means clusters, i.e. columns
	 *   of the table somInfo.tblSOMneuro with the cluster labels
	 * @return the index of the first column containing k-means cluster labels
	 *   in the table with all objects (somInfo.tblSOMResult)
	 */
	public static int assignClustersOfPrototypesToAllObjects(SOMApplInfo somInfo, int neuroClColIdx0, int neuroClColIdxLast) {
		if (somInfo == null || neuroClColIdx0 < 0 || somInfo.tblSOMResult == null || somInfo.tblSOMneuro == null || somInfo.somRes == null)
			return -1;
		//add columns with clusters to the table with all objects
		int objClColIdx = somInfo.tblSOMResult.getAttrCount();
		for (int i = neuroClColIdx0; i <= neuroClColIdxLast; i++) {
			Attribute at = somInfo.tblSOMneuro.getAttribute(i);
			Attribute at1 = new Attribute("at" + (somInfo.tblSOMResult.getAttrCount() + 1), at.getType());
			at1.setName(at.getName());
			somInfo.tblSOMResult.addAttribute(at1);
			at1.setValueListAndColors(at.getValueList(), at.getValueColors());
		}
		SOMResult sr = somInfo.somRes;
		int xCN = somInfo.tblSOMneuro.getAttrIndex("x"), yCN = somInfo.tblSOMneuro.getAttrIndex("y");
		for (int rn = 0; rn < somInfo.tblSOMneuro.getDataItemCount(); rn++) {
			DataRecord pRec = somInfo.tblSOMneuro.getDataRecord(rn);
			int x = (int) pRec.getNumericAttrValue(xCN), y = (int) pRec.getNumericAttrValue(yCN);
			SOMCellInfo sci = sr.cellInfos[x][y];
			if (sci.nObj < 1) {
				continue;
			}
			for (int i = 0; i < sci.oIds.size(); i++) {
				int id = sci.oIds.get(i).intValue();
				if (id > 0) {
					DataRecord rec = somInfo.tblSOMResult.getDataRecord(id - 1);
					if (rec != null) {
						for (int j = neuroClColIdx0; j <= neuroClColIdxLast; j++) {
							String clusterLabel = pRec.getAttrValueAsString(j);
							int cIdx = objClColIdx + j - neuroClColIdx0;
							rec.setAttrValue(clusterLabel, cIdx);
						}
					}
				}
			}
		}
		Vector<String> resultAttrs = new Vector<String>(somInfo.tblSOMResult.getAttrCount() - objClColIdx, 1);
		for (int i = objClColIdx; i < somInfo.tblSOMResult.getAttrCount(); i++) {
			resultAttrs.addElement(somInfo.tblSOMResult.getAttributeId(i));
		}
		somInfo.tblSOMResult.notifyPropertyChange("new_attributes", null, resultAttrs);
		return objClColIdx;
	}

	public static double getMinkowskiDistance(double v1[], double v2[], double p) {
		double squaressum = 0;
		if (v1 == null || v2 == null || v1.length != v2.length)
			return 0;
		for (int i = 0; i < v1.length; i++) {
			squaressum += Math.pow(Math.abs(v1[i] - v2[i]), p);
		}
		double exp = 1.0d / p;
		double distance = Math.pow(squaressum, exp);
		return distance;
	}

}
