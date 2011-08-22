package spade.analysis.tools.similarity;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.FileDialog;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.BaseAnalyser;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.IdMaker;
import spade.lib.util.IntArray;
import spade.lib.util.NumRange;
import spade.lib.util.QSortAlgorithm;
import spade.lib.util.StringUtil;
import spade.vis.action.Highlighter;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.Parameter;
import ui.AttributeChooser;
import ui.TableManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Aug 4, 2009
 * Time: 5:41:28 PM
 * Computes a matrix of Euclidean distances between objects (rows) of a table
 * in the multidimensional space of attribute values.
 */
public class ComputeDistanceMatrixForTable extends BaseAnalyser {
	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	@Override
	public void run(ESDACore core) {
		this.core = core;
		if (core == null)
			return;
		DataKeeper dk = core.getDataKeeper();
		if (dk == null || dk.getTableCount() < 1) {
			showMessage("No tables found!", true);
			return;
		}
		DataTable tbl = selectTable();
		if (tbl == null)
			return;
		AttributeChooser attrSel = new AttributeChooser();
		attrSel.selectColumns(tbl, null, null, true, "Select attributes", core.getUI());
		Vector attr = attrSel.getSelectedColumnIds();
		if (attr == null || attr.size() < 1)
			return;
		IntArray attrNumbers = new IntArray();
		for (int i = 0; i < attr.size(); i++) {
			int idx = tbl.getAttrIndex((String) attr.elementAt(i));
			if (idx >= 0) {
				attrNumbers.addElement(idx);
			}
		}
		if (attrNumbers.size() < 1)
			return;

		NumRange ranges[] = new NumRange[attrNumbers.size()];
		IntArray tmp = new IntArray(1, 1);
		int nValid = 0;
		for (int i = 0; i < attrNumbers.size(); i++) {
			tmp.removeAllElements();
			tmp.addElement(attrNumbers.elementAt(i));
			ranges[i] = tbl.getValueRangeInColumns(tmp, false);
			if (ranges[i] != null && ranges[i].maxValue <= ranges[i].minValue) {
				ranges[i] = null;
			}
			if (ranges[i] != null) {
				++nValid;
			}
		}
		if (nValid < 1) {
			showMessage("Failed to get the value ranges of the attributes!", true);
			return;
		}
		if (nValid < attrNumbers.size()) {
			boolean yes = Dialogs.askYesOrNo(core.getUI().getMainFrame(), "Only " + nValid + " out of " + attrNumbers.size() + " attributes have valid value ranges " + "and can be used for computing the distances. Continue anyway?",
					"Not all attributes can be used!");
			if (!yes)
				return;
		}
		Panel p = new Panel(new ColumnLayout());
		p.add(new Label("Compute"));
		Checkbox cbMatrix = new Checkbox("the full matrix of distances between the rows", true);
		Checkbox cbNei = new Checkbox("the distances between the successive rows", false);
		Checkbox cbSel = new Checkbox("the distances to the selected rows", false);
		p.add(cbMatrix);
		p.add(cbNei);
		p.add(cbSel);
		IntArray selRowNs = null;
		Highlighter hl = core.getHighlighterForContainer(tbl.getContainerIdentifier());
		Vector selIds = hl.getSelectedObjects();
		if (selIds != null && selIds.size() > 0) {
			selRowNs = new IntArray(selIds.size(), 1);
			for (int i = 0; i < selIds.size(); i++) {
				int rn = tbl.indexOf(selIds.elementAt(i).toString());
				if (rn >= 0) {
					selRowNs.addElement(rn);
				}
			}
			if (selRowNs.size() < 1) {
				selRowNs = null;
			}
		}
		if (selRowNs == null) {
			cbSel.setEnabled(false);
		}
		p.add(new Line(false));
		p.add(new Label("Compute the distance as"));
		CheckboxGroup cbg = new CheckboxGroup();
		Checkbox cbAve = new Checkbox("average of the absolute differences", true, cbg);
		Checkbox cbMan = new Checkbox("Manhattan distance", false, cbg);
		Checkbox cbEuc = new Checkbox("Euclidean distance", false, cbg);
		p.add(cbAve);
		p.add(cbMan);
		p.add(cbEuc);
		p.add(new Line(false));
		p.add(new Label("Transform the values of the attributes?"));
		cbg = new CheckboxGroup();
		Checkbox cbNoTrans = new Checkbox("no transformation", true, cbg);
		Checkbox cbIndiv = new Checkbox("0..1 relative to the individual value range of each attribute", false, cbg);
		Checkbox cbCommon = new Checkbox("0..1 relative to the common value range of all attributes", false, cbg);
		p.add(cbNoTrans);
		p.add(cbIndiv);
		p.add(cbCommon);
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Parameters", true);
		dia.addContent(p);
		dia.show();
		if (dia.wasCancelled())
			return;
		boolean computeMatrix = cbMatrix.getState(), computeNei = cbNei.getState(), computeSel = cbSel.getState();
		if (!computeMatrix && !computeNei && !computeSel)
			return;
		boolean ave = cbAve.getState(), euc = cbEuc.getState();
		boolean transIndiv = cbIndiv.getState(), transCommon = cbCommon.getState();

		int colNs[] = new int[nValid];
		double maxDist[] = new double[nValid];
		int k = 0;
		for (int i = 0; i < ranges.length; i++)
			if (ranges[i] != null) {
				colNs[k] = attrNumbers.elementAt(i);
				maxDist[k] = ranges[i].maxValue - ranges[i].minValue;
				++k;
			}

		float distMatrix[][] = null;
		int nObj = tbl.getDataItemCount();
		if (computeMatrix) {
			distMatrix = tbl.getDistanceMatrix();
			if (distMatrix != null && distMatrix.length != nObj) {
				distMatrix = null;
			}
			if (distMatrix == null) {
				long freeMem = Runtime.getRuntime().freeMemory(), needMem = ((long) nObj) * nObj * Float.SIZE / 8;
				if (needMem >= freeMem / 3) {
					System.out.println("Garbage collector started, free memory before: " + freeMem);
					Runtime.getRuntime().gc();
					freeMem = Runtime.getRuntime().freeMemory();
					System.out.println("Garbage collector finished, free memory after: " + freeMem);
				}
				try {
					distMatrix = new float[nObj][nObj];
				} catch (OutOfMemoryError out) {
					System.out.println("Similarity computing: not enough memory for distance matrix, need: " + needMem);
					showMessage("Similarity computing: not enough memory for distance matrix, need: " + needMem, true);
					return;
				}
				System.out.println("Similarity computing: distance matrix constructed!");
				showMessage("Similarity computing: distance matrix constructed!", false);
			} else {
				System.out.println("Similarity computing: reusing previously created distance matrix");
				showMessage("Similarity computing: reusing previously created distance matrix", false);
			}

			for (int i = 0; i < nObj; i++) {
				for (int j = 0; j < nObj; j++)
					if (i == j) {
						distMatrix[i][j] = 0;
					} else {
						distMatrix[i][j] = Float.NaN;
					}
			}
		}

		double commonDist = Double.NaN;
		if (nValid > 1 && transCommon) {
			NumRange commonRange = null;
			for (NumRange range : ranges)
				if (range != null) {
					if (commonRange == null) {
						commonRange = new NumRange();
						commonRange.minValue = range.minValue;
						commonRange.maxValue = range.maxValue;
					} else {
						if (commonRange.minValue > range.minValue) {
							commonRange.minValue = range.minValue;
						}
						if (commonRange.maxValue < range.maxValue) {
							commonRange.maxValue = range.maxValue;
						}
					}
				}
			commonDist = commonRange.maxValue - commonRange.minValue;
		}

		if (computeMatrix) {
			double values1[] = new double[colNs.length];
			for (int i = 0; i < nObj - 1; i++) {
				DataRecord rec1 = tbl.getDataRecord(i);
				for (int nc = 0; nc < colNs.length; nc++) {
					values1[nc] = rec1.getNumericAttrValue(colNs[nc]);
				}
				for (int j = i + 1; j < nObj; j++) {
					DataRecord rec2 = tbl.getDataRecord(j);
					double dist = 0;
					int nval = 0;
					for (int nc = 0; nc < colNs.length; nc++)
						if (!Double.isNaN(values1[nc])) {
							double v = rec2.getNumericAttrValue(colNs[nc]);
							if (!Double.isNaN(v)) {
								++nval;
								double diff = Math.abs(v - values1[nc]);
								if (transCommon) {
									diff /= commonDist;
								} else if (transIndiv) {
									diff /= maxDist[nc];
								}
								if (euc) {
									dist += diff * diff;
								} else {
									dist += diff;
								}
							}
						}
					if (nval < 1) {
						distMatrix[i][j] = distMatrix[j][i] = Float.NaN;
						continue;
					}
					if (euc) {
						dist = Math.sqrt(dist);
					} else if (ave) {
						dist = dist / nval;
					}
					distMatrix[i][j] = distMatrix[j][i] = (float) dist;
				}
			}
			tbl.setDistanceMatrix(distMatrix);
			String title = ((ave) ? "Average" : (euc) ? "Euclidean" : "Manhattan") + " distance; "
					+ ((transCommon || transIndiv) ? "values transformed to 0..1 relative to " + ((transCommon) ? "common value range" : "individual value ranges") : "non-transformed values");
			title = Dialogs.askForStringValue(getFrame(), "Give an explanatory title for the distance matrix?", title, null, "Title", false);
			tbl.setDistMatrixTitle(title);
			if (Dialogs.askYesOrNo(core.getUI().getMainFrame(), "Store the computed distances in a file?", "Distances > File")) {
				writeDistanceMatrix(tbl);
			}
			showMessage("Similarity computing: distance matrix computed!", false);
		}
		if (computeNei) {
			//make a new table column
			String genName = tbl.getGenericNameOfEntity();
			if (genName == null) {
				genName = "record";
			}
			String pref = "Difference from previous " + genName, aName = pref;
			for (int i = 2; tbl.findAttrByName(aName) >= 0; i++) {
				aName = pref + " (" + i + ")";
			}
			aName = Dialogs.askForStringValue(core.getUI().getMainFrame(), "Column name?", aName, "A new column will be created in the table " + tbl.getName(), "New column", true);
			if (aName == null)
				return;
			Attribute at = new Attribute(IdMaker.makeId(tbl.getAttrCount() + " " + aName, tbl), AttributeTypes.real);
			at.setName(aName);
			tbl.addAttribute(at);
			int aIdx = tbl.getAttrCount() - 1;
			double minD = Float.NaN, maxD = Float.NaN;
			if (distMatrix != null) {
				for (int rn = 1; rn < nObj; rn++)
					if (!Float.isNaN(distMatrix[rn - 1][rn])) {
						DataRecord rec = tbl.getDataRecord(rn);
						rec.setNumericAttrValue(distMatrix[rn - 1][rn], aIdx);
						if (Double.isNaN(minD) || minD > distMatrix[rn - 1][rn]) {
							minD = distMatrix[rn - 1][rn];
						}
						if (Double.isNaN(maxD) || maxD < distMatrix[rn - 1][rn]) {
							maxD = distMatrix[rn - 1][rn];
						}
					} else {
						continue;
					}
			} else {
				for (int rn = 1; rn < nObj; rn++) {
					DataRecord rec0 = tbl.getDataRecord(rn - 1), rec1 = tbl.getDataRecord(rn);
					double dist = 0;
					int nval = 0;
					for (int nc = 0; nc < colNs.length; nc++) {
						double val0 = rec0.getNumericAttrValue(colNs[nc]), val1 = rec1.getNumericAttrValue(colNs[nc]);
						if (!Double.isNaN(val0) && !Double.isNaN(val1)) {
							++nval;
							double diff = Math.abs(val1 - val0);
							if (transCommon) {
								diff /= commonDist;
							} else if (transIndiv) {
								diff /= maxDist[nc];
							}
							if (euc) {
								dist += diff * diff;
							} else {
								dist += diff;
							}
						}
					}
					if (nval < 1) {
						continue;
					}
					if (euc) {
						dist = Math.sqrt(dist);
					} else if (ave) {
						dist = dist / nval;
					}
					if (Double.isNaN(minD) || minD > dist) {
						minD = dist;
					}
					if (Double.isNaN(maxD) || maxD < dist) {
						maxD = dist;
					}
					rec1.setNumericAttrValue(dist, aIdx);
				}
			}
			if (Double.isNaN(minD)) {
				showMessage("Similarity computing: no differences computed!", true);
			} else {
				for (int rn = 1; rn < nObj; rn++) {
					DataRecord rec = tbl.getDataRecord(rn);
					double dist = rec.getNumericAttrValue(aIdx);
					if (!Double.isNaN(dist)) {
						rec.setNumericAttrValue(dist, StringUtil.doubleToStr(dist, minD, maxD), aIdx);
					}
				}
				showMessage("Similarity computing: differences between successive rows computed!", false);
			}
			//tbl.finishedDataLoading();
			tbl.makeUniqueAttrIdentifiers();
		}
		if (computeSel) {
			int aIdx0 = tbl.getAttrCount();
			double minD = Float.NaN, maxD = Float.NaN;
			int selRN[] = selRowNs.getTrimmedArray();
			String genName = tbl.getGenericNameOfEntity();
			if (genName == null) {
				genName = "record";
			}
			if (selRN.length == 1) {
				//make a new table column
				String pref = "Difference from " + genName + " " + tbl.getDataItemName(selRN[0]), aName = pref;
				for (int i = 2; tbl.findAttrByName(aName) >= 0; i++) {
					aName = pref + " (" + i + ")";
				}
				aName = Dialogs.askForStringValue(core.getUI().getMainFrame(), "Column name?", aName, "A new column will be created in the table " + tbl.getName(), "New column", true);
				if (aName == null)
					return;
				Attribute at = new Attribute(IdMaker.makeId(tbl.getAttrCount() + " " + aName, tbl), AttributeTypes.real);
				at.setName(aName);
				tbl.addAttribute(at);
			} else {
				//make a parameter-dependent attribute
				Parameter par = new Parameter();
				QSortAlgorithm.sort(selRN);
				for (int element : selRN) {
					par.addValue(tbl.getDataItemName(element));
				}
				String pref = genName, parName = pref;
				for (int i = 2; tbl.getParameter(parName) != null; i++) {
					parName = pref + " (" + i + ")";
				}
				par.setName(parName);
				tbl.addParameter(par);
				pref = "Difference from " + genName;
				String aName = pref;
				for (int i = 2; tbl.findAttrByName(aName) >= 0; i++) {
					aName = pref + " (" + i + ")";
				}
				p = new Panel(new ColumnLayout());
				p.add(new Label("A parameter-dependent attribute will be produced.", Label.CENTER));
				p.add(new Label("Edit the names of the attribute and the parameter if needed.", Label.CENTER));
				p.add(new Label("Name of the parameter-dependent attribute:"));
				TextField atf = new TextField(aName);
				p.add(atf);
				p.add(new Label("Parameter name:"));
				TextField ptf = new TextField(parName);
				p.add(ptf);
				p.add(new Label("Parameter values: " + par.getFirstValue() + ((selRN.length == 2) ? ", " : ", ..., ") + par.getLastValue()));
				dia = new OKDialog(core.getUI().getMainFrame(), "New attribute", true);
				dia.addContent(p);
				dia.show();
				if (dia.wasCancelled())
					return;
				String str = atf.getText();
				if (str != null && str.trim().length() > 0) {
					aName = str.trim();
				}
				str = ptf.getText();
				if (str != null && str.trim().length() > 0) {
					parName = str.trim();
					par.setName(parName);
				}
				Attribute attrParent = new Attribute(IdMaker.makeId(tbl.getAttrCount() + " " + aName, tbl), AttributeTypes.real);
				attrParent.setName(aName);
				for (int i = 0; i < par.getValueCount(); i++) {
					String name = par.getValue(i).toString();
					Attribute attrChild = new Attribute(attrParent.getIdentifier() + "_" + name + "_" + tbl.getAttrCount(), attrParent.getType());
					attrChild.addParamValPair(par.getName(), par.getValue(i));
					attrParent.addChild(attrChild);
					tbl.addAttribute(attrChild);
				}
			}
			if (distMatrix != null) {
				for (int rn = 0; rn < nObj; rn++) {
					for (int is = 0; is < selRN.length; is++)
						if (!Float.isNaN(distMatrix[rn][selRN[is]])) {
							DataRecord rec = tbl.getDataRecord(rn);
							rec.setNumericAttrValue(distMatrix[rn][selRN[is]], aIdx0 + is);
							if (Double.isNaN(minD) || minD > distMatrix[rn][selRN[is]]) {
								minD = distMatrix[rn][selRN[is]];
							}
							if (Double.isNaN(maxD) || maxD < distMatrix[rn][selRN[is]]) {
								maxD = distMatrix[rn][selRN[is]];
							}
						}
				}
			} else {
				double values1[] = new double[colNs.length];
				for (int i = 0; i < nObj; i++) {
					DataRecord rec1 = tbl.getDataRecord(i);
					for (int nc = 0; nc < colNs.length; nc++) {
						values1[nc] = rec1.getNumericAttrValue(colNs[nc]);
					}
					for (int is = 0; is < selRN.length; is++) {
						DataRecord rec2 = tbl.getDataRecord(selRN[is]);
						double dist = 0;
						int nval = 0;
						for (int nc = 0; nc < colNs.length; nc++)
							if (!Double.isNaN(values1[nc])) {
								double v = rec2.getNumericAttrValue(colNs[nc]);
								if (!Double.isNaN(v)) {
									++nval;
									double diff = Math.abs(v - values1[nc]);
									if (transCommon) {
										diff /= commonDist;
									} else if (transIndiv) {
										diff /= maxDist[nc];
									}
									if (euc) {
										dist += diff * diff;
									} else {
										dist += diff;
									}
								}
							}
						if (nval < 1) {
							continue;
						}
						if (euc) {
							dist = Math.sqrt(dist);
						} else if (ave) {
							dist = dist / nval;
						}
						if (Double.isNaN(minD) || minD > dist) {
							minD = dist;
						}
						if (Double.isNaN(maxD) || maxD < dist) {
							maxD = dist;
						}
						rec1.setNumericAttrValue(dist, aIdx0 + is);
					}
				}
			}
			if (Double.isNaN(minD)) {
				showMessage("Similarity computing: no differences computed!", true);
			} else {
				for (int rn = 0; rn < nObj; rn++) {
					DataRecord rec = tbl.getDataRecord(rn);
					for (int aIdx = aIdx0; aIdx < tbl.getAttrCount(); aIdx++) {
						double dist = rec.getNumericAttrValue(aIdx);
						if (!Double.isNaN(dist)) {
							rec.setNumericAttrValue(dist, StringUtil.doubleToStr(dist, minD, maxD), aIdx);
						}
					}
				}
				showMessage("Similarity computing: differences from the selected rows computed!", false);
			}
			//tbl.finishedDataLoading();
			tbl.makeUniqueAttrIdentifiers();
		}
	}

	protected DataTable selectTable() {
		DataKeeper dk = core.getDataKeeper();
		if (dk.getTableCount() < 1)
			return null;
		TableManager tman = new TableManager();
		tman.setDataKeeper(dk);
		tman.setUI(core.getUI());
		int tn = tman.selectTableNumber("Select table");
		if (tn < 0)
			return null;
		AttributeDataPortion tbl = dk.getTable(tn);
		if (!(tbl instanceof DataTable)) {
			showMessage("The table is not a DataTable!", true);
			return null;
		}
		return (DataTable) tbl;
	}

	public void writeDistanceMatrix(DataTable table) {
		float distMatrix[][] = table.getDistanceMatrix();
		if (distMatrix == null)
			return;

		FileDialog fd = new FileDialog(core.getUI().getMainFrame(), "Specify the file to store the distances");
		fd.setFile("*.txt");
		fd.setMode(FileDialog.SAVE);
		fd.show();
		if (fd.getDirectory() == null)
			return;
		String fname = fd.getDirectory() + fd.getFile();

		int nObj = table.getDataItemCount();

		FileOutputStream out = null;
		try {
			out = new FileOutputStream(fname);
		} catch (IOException ioe) {
			showMessage("Could not create file " + fname, true);
			System.out.println(ioe);
		}
		if (out == null)
			return;
		DataOutputStream dos = new DataOutputStream(out);
		try {
			dos.writeBytes("From,To,distance\n");
			for (int i = 0; i < nObj - 1; i++) {
				String id1 = table.getDataItemId(i);
				if (id1 != null) {
					for (int j = i + 1; j < nObj; j++) {
						String id2 = table.getDataItemId(j);
						if (id2 == null || id2.equalsIgnoreCase(id1)) {
							continue;
						}
						String str = id1 + "," + id2 + ",";
						if (Float.isNaN(distMatrix[i][j])) {
							str += "NaN";
						} else {
							str += distMatrix[i][j];
						}
						dos.writeBytes(str + "\n");
					}
				}
			}
		} catch (IOException ioe) {
			showMessage("Error writing file " + fname, true);
			System.out.println(ioe);
		}
		try {
			out.close();
		} catch (IOException e) {
		}
	}

}
