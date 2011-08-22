package spade.analysis.tools.table_processing;

import java.awt.BorderLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.BaseAnalyser;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.BubbleSort;
import spade.lib.util.DoubleArray;
import spade.lib.util.IntArray;
import spade.lib.util.NumRange;
import spade.lib.util.NumValManager;
import spade.lib.util.ObjectWithCount;
import spade.lib.util.StringUtil;
import spade.time.Date;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.Parameter;
import ui.AttributeChooser;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 9, 2010
 * Time: 2:54:42 PM
 * Summarizes classes or clusters of table records by computing the mean
 * and standard deviation for each numeric attribute and the number of
 * different values and the most frequent value for each qualitative attribute
 */
public class ClassSummarizer extends BaseAnalyser {
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
		AttributeDataPortion tbl = TableProcessor.selectTable(core);
		if (tbl == null)
			return;
		if (!(tbl instanceof DataTable)) {
			showMessage("The table is not DataTable!", true);
			return;
		}
		if (tbl.getDataItemCount() < 1) {
			showMessage("No data in the table!", true);
			return;
		}
		DataTable table = (DataTable) tbl;
		IntArray colNs = new IntArray(table.getAttrCount(), 10);
		List list = new List(Math.min(table.getAttrCount(), 5));
		for (int i = 0; i < table.getAttrCount(); i++) {
			Attribute at = table.getAttribute(i);
			if (at.getType() == AttributeTypes.character || at.getType() == AttributeTypes.integer) {
				colNs.addElement(i);
				list.add(at.getName());
			}
		}
		if (colNs.size() < 1) {
			showMessage("The table has no columns with classes or clusters!", true);
			return;
		}
		list.select(0);
		Panel p = new Panel(new BorderLayout());
		p.add(new Label("Select the table column containing classes or clusters:"), BorderLayout.NORTH);
		p.add(list, BorderLayout.CENTER);
		OKDialog dia = new OKDialog(getFrame(), "Summarize classes or clusters", true);
		dia.addContent(p);
		dia.show();
		if (dia.wasCancelled())
			return;
		int clColN = colNs.elementAt(list.getSelectedIndex());
		IntArray iar = new IntArray(1, 1);
		iar.addElement(clColN);
		Vector values = table.getAllValuesInColumnsAsStrings(iar);
		if (values == null || values.size() < 1) {
			showMessage("No values in column " + table.getAttributeName(clColN), true);
			return;
		}
		Vector selAttr = null;
		AttributeChooser attrSel = new AttributeChooser();
		if (attrSel.selectColumns(table, null, null, false, "Select the attributes to summarize", core.getUI()) != null) {
			selAttr = attrSel.getSelectedColumnIds();
		}
		if (selAttr == null || selAttr.size() < 1)
			return;
		//if temporal attributes have been selected, exclude them
		int nExcluded = 0;
		for (int i = selAttr.size() - 1; i >= 0; i--) {
			Attribute at = table.getAttribute((String) selAttr.elementAt(i));
			if (at.isTemporal()) {
				Vector v = table.getKAttrValues(at.getIdentifier(), 1);
				if (v == null || v.size() < 1) {
					continue;
				}
				if (v.elementAt(0) instanceof Date) {
					selAttr.removeElementAt(i);
					nExcluded++;
				}
			}
		}
		if (nExcluded > 0) {
			if (selAttr.size() < 1) {
				showMessage("Summarization cannot be done for dates!", true);
				return;
			}
			if (!Dialogs.askYesOrNo(getFrame(), nExcluded + " columns contain dates and therefore have been excluded. " + selAttr.size() + " columns remain. Continue?", "Dates excluded"))
				return;
		}
		String tName = table.getAttributeName(clColN);
/*
    tName=Dialogs.askForStringValue(getFrame(),"Table name?",tName,
      "A new table will be created.","New table",true);
    if (tName==null)
      return;
*/

		int cNs[] = new int[selAttr.size()];
		for (int i = 0; i < selAttr.size(); i++) {
			cNs[i] = table.getAttrIndex((String) selAttr.elementAt(i));
		}

		DataTable cTable = summarize(table, clColN, cNs, tName);
		if (cTable == null)
			return;
		//add the table to the system
		DataLoader dLoader = core.getDataLoader();
		dLoader.addTable(cTable);
	}

	public DataTable summarize(DataTable origTable, int classColN, int colNsToSum[], String tName) {
		DataTable cTable = new DataTable();
		cTable.setName(tName);
		Attribute clAt = origTable.getAttribute(classColN);
		cTable.addAttribute("Class", "class", clAt.getType());
		if (clAt.isClassification()) {
			Attribute clAtNew = cTable.getAttribute(cTable.getAttrCount() - 1);
			clAtNew.setValueListAndColors(clAt.getValueList(), clAt.getValueColors());
		}
		cTable.addAttribute("N members", "n_members", AttributeTypes.integer);
		Vector<Attribute> origParents = null, newParents1 = null, newParents2 = null, newParents3 = null;
		for (int element : colNsToSum) {
			Attribute at = origTable.getAttribute(element);
			Attribute parent = at.getParent();
			if (parent != null && at.getParameterCount() > 0) {
				int pIdx = -1;
				if (origParents != null) {
					pIdx = origParents.indexOf(parent);
				}
				if (pIdx < 0) {
					if (origParents == null) {
						origParents = new Vector<Attribute>(20, 20);
						newParents1 = new Vector<Attribute>(20, 20);
						newParents2 = new Vector<Attribute>(20, 20);
						newParents3 = new Vector<Attribute>(20, 20);
					}
					boolean numeric = at.isNumeric() && !at.isClassification();
					Attribute parent1 = null, parent2 = null, parent3 = null;
					parent1 = new Attribute(parent.getIdentifier() + "_1", (numeric) ? AttributeTypes.real : parent.getType());
					parent2 = new Attribute(parent.getIdentifier() + "_2", (numeric) ? AttributeTypes.real : AttributeTypes.integer);
					parent3 = new Attribute(parent.getIdentifier() + "_3", (numeric) ? AttributeTypes.real : AttributeTypes.integer);
					if (numeric) {
						parent1.setName(parent.getName() + ", sum");
						parent2.setName(parent.getName() + ", mean");
						parent3.setName(parent.getName() + ", std.dev.");
					} else {
						parent1.setName(parent.getName() + ", mode");
						parent2.setName(parent.getName() + ", mode frequency");
						parent3.setName(parent.getName() + ", N different values");
					}
					origParents.addElement(parent);
					newParents1.addElement(parent1);
					newParents2.addElement(parent2);
					newParents3.addElement(parent3);
				}
				for (int j = 0; j < at.getParameterCount(); j++) {
					String parName = at.getParamName(j);
					pIdx = -1;
					Parameter par = cTable.getParameter(parName);
					if (par == null) {
						Parameter origPar = origTable.getParameter(parName);
						if (origPar == null) {
							continue;
						}
						par = new Parameter();
						par.setName(parName);
						par.setTemporal(origPar.isTemporal());
						for (int k = 0; k < origPar.getValueCount(); k++) {
							par.addValue(origPar.getValue(k));
						}
						cTable.addParameter(par);
					}
				}
			}
		}
		int cNsNew[] = new int[colNsToSum.length * 3];
		for (int i = 0; i < cNsNew.length; i++) {
			cNsNew[i] = -1;
		}
		for (int nTimes = 1; nTimes <= 3; nTimes++) {
			for (int i = 0; i < colNsToSum.length; i++) {
				Attribute at = origTable.getAttribute(colNsToSum[i]);
				boolean numeric = at.isNumeric() && !at.isClassification();
				Attribute atNew = new Attribute(at.getIdentifier() + "_" + nTimes, (numeric) ? AttributeTypes.real : (nTimes == 1) ? at.getType() : AttributeTypes.integer);
				if (numeric) {
					atNew.setName(at.getName() + ((nTimes == 1) ? ", sum" : (nTimes == 2) ? ", mean" : ", std.dev."));
				} else {
					atNew.setName(at.getName() + ((nTimes == 1) ? ", mode" : (nTimes == 2) ? ", mode frequency" : ", N different values"));
				}
				Attribute parent = at.getParent();
				if (parent != null && at.getParameterCount() > 0 && origParents != null) {
					int pIdx = origParents.indexOf(parent);
					if (pIdx >= 0) {
						Attribute newParent = (nTimes == 1) ? newParents1.elementAt(pIdx) : (nTimes == 2) ? newParents2.elementAt(pIdx) : newParents3.elementAt(pIdx);
						if (newParent != null) {
							newParent.addChild(atNew);
							for (int j = 0; j < at.getParameterCount(); j++) {
								atNew.addParamValPair(at.getParamName(j), at.getParamValue(j));
							}
						}
					}
				}
				cNsNew[i * 3 + nTimes - 1] = cTable.getAttrCount();
				cTable.addAttribute(atNew);
			}
		}
		cTable.makeUniqueAttrIdentifiers();

		int prec[] = new int[colNsToSum.length];
		for (int i = 0; i < colNsToSum.length; i++) {
			prec[i] = 0;
			if (origTable.isAttributeNumeric(colNsToSum[i])) {
				NumRange nr = origTable.getAttrValueRange(origTable.getAttributeId(colNsToSum[i]));
				if (nr != null) {
					prec[i] = StringUtil.getPreferredPrecision(nr.minValue, nr.minValue, nr.maxValue);
				}
			}
		}

		IntArray iar = new IntArray(1, 1);
		iar.addElement(classColN);
		Vector values = origTable.getAllValuesInColumnsAsStrings(iar);
		BubbleSort.sort(values);
		Vector<DataRecord> recs = new Vector<DataRecord>(origTable.getDataItemCount() / values.size() * 2, 100);
		for (int nc = 0; nc < values.size(); nc++) {
			Object val = values.elementAt(nc);
			//extract values with this value
			recs.removeAllElements();
			for (int i = 0; i < origTable.getDataItemCount(); i++) {
				DataRecord rec = origTable.getDataRecord(i);
				if (val.equals(rec.getAttrValue(classColN))) {
					recs.addElement(rec);
				}
			}
			if (recs.size() < 1) {
				continue;
			}
			DataRecord clRec = new DataRecord(val.toString(), val.toString());
			if (!(val instanceof String)) {
				clRec.setDescribedObject(val);
			}
			cTable.addDataRecord(clRec);
			clRec.setAttrValue(val, 0);
			clRec.setNumericAttrValue(recs.size(), String.valueOf(recs.size()), 1);
			for (int i = 0; i < colNsToSum.length; i++) {
				Attribute at = origTable.getAttribute(colNsToSum[i]);
				boolean numeric = at.isNumeric() && !at.isClassification();
				if (numeric) {
					DoubleArray dar = new DoubleArray(recs.size(), 10);
					double sum = 0;
					for (int nr = 0; nr < recs.size(); nr++) {
						double num = recs.elementAt(nr).getNumericAttrValue(colNsToSum[i]);
						if (!Double.isNaN(num)) {
							dar.addElement(num);
							sum += num;
						}
					}
					if (dar.size() < 1) {
						continue;
					}
					double ave = sum / dar.size();
					double stdev = NumValManager.getStdD(dar, ave);
					clRec.setNumericAttrValue(sum, StringUtil.doubleToStr(sum, prec[i]), cNsNew[i * 3]);
					clRec.setNumericAttrValue(ave, StringUtil.doubleToStr(ave, prec[i] + 1), cNsNew[i * 3 + 1]);
					clRec.setNumericAttrValue(stdev, StringUtil.doubleToStr(stdev, prec[i] + 2), cNsNew[i * 3 + 2]);
				} else {
					Vector<ObjectWithCount> vCounts = new Vector<ObjectWithCount>(recs.size(), 10);
					for (int nr = 0; nr < recs.size(); nr++) {
						Object value = recs.elementAt(nr).getAttrValue(colNsToSum[i]);
						if (value == null) {
							continue;
						}
						int idx = -1;
						for (int k = 0; k < vCounts.size() && idx < 0; k++)
							if (vCounts.elementAt(k).hasObject(value)) {
								idx = k;
							}
						if (idx < 0) {
							vCounts.addElement(new ObjectWithCount(value, 1));
						} else {
							vCounts.elementAt(idx).add();
						}
					}
					if (vCounts.size() < 1) {
						continue;
					}
					if (vCounts.size() > 1) {
						BubbleSort.sort(vCounts);
					}
					clRec.setAttrValue(vCounts.elementAt(0).obj, cNsNew[i * 3]);
					int count = vCounts.elementAt(0).count;
					clRec.setNumericAttrValue(count, String.valueOf(count), cNsNew[i * 3 + 1]);
					clRec.setNumericAttrValue(vCounts.size(), String.valueOf(vCounts.size()), cNsNew[i * 3 + 2]);
				}
			}
		}
		return cTable;
	}
}
