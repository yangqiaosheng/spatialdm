package spade.analysis.tools.table_processing;

import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.BaseAnalyser;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.IdMaker;
import spade.lib.util.IntArray;
import spade.time.TimeMoment;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.ObjectFilter;
import spade.vis.database.Parameter;
import spade.vis.database.ThematicDataItem;
import ui.AttributeChooser;
import ui.TableManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jul 10, 2009
 * Time: 5:52:00 PM
 * Transposes a table: the columns become rows and vice versa
 */
public class TransposeTable extends BaseAnalyser {
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
			showMessage("No tables found loaded!", true);
			return;
		}
		AttributeDataPortion tbl = selectTable(core);
		if (tbl == null)
			return;
		AttributeChooser attrSel = new AttributeChooser();
		attrSel.selectColumns(tbl, "Select Attributes", core.getUI());
		Vector attr = attrSel.getSelectedColumnIds();
		if (attr == null || attr.size() < 1)
			return;
		IntArray attrNumbers = new IntArray();
		boolean hasNotNumbers = false;
		Vector<Attribute> parents = null;
		Parameter param = null;
		for (int i = 0; i < attr.size(); i++) {
			int idx = tbl.getAttrIndex((String) attr.elementAt(i));
			if (idx >= 0) {
				attrNumbers.addElement(idx);
				if (attrNumbers.size() == 1 && tbl.getParamCount() > 0) {
					Attribute at = tbl.getAttribute(idx);
					if (at.getParameterCount() == 1) {
						param = tbl.getParameter(at.getParamName(0));
						if (param != null) {
							parents = new Vector<Attribute>(10, 10);
							parents.addElement(at.getParent());
						}
					}
				} else if (param != null) {
					Attribute at = tbl.getAttribute(idx);
					if (at.getParameterCount() == 1 && at.getParamName(0).equals(param.getName())) {
						Attribute parent = at.getParent();
						if (!parents.contains(parent)) {
							parents.addElement(parent);
						}
					} else {
						parents = null;
						param = null;
					}
				}
				hasNotNumbers = hasNotNumbers || !tbl.isAttributeNumeric(idx);
			}
		}
		if (attrNumbers.size() < 1)
			return;
		String tblName = "Transposed " + tbl.getName();
		if (parents != null && parents.size() == 1) {
			tblName = parents.elementAt(0).getName();
		}
		String genName = null;
		if (param != null) {
			genName = param.getName();
		}

		Panel p = new Panel(new ColumnLayout());
		p.add(new Label("A new table will be created.", Label.CENTER));
		p.add(new Label("Table name:"));
		TextField tntf = new TextField(tblName);
		p.add(tntf);
		p.add(new Label("Generic name of an item described by a record:"));
		TextField gntf = new TextField(genName);
		p.add(gntf);
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "New table", true);
		dia.addContent(p);
		dia.show();
		if (dia.wasCancelled())
			return;
		String str = tntf.getText();
		if (str != null && str.trim().length() > 0) {
			tblName = str.trim();
		}
		str = gntf.getText();
		if (str != null && str.trim().length() > 0) {
			genName = str.trim();
		} else {
			genName = null;
		}

		DataTable transp = new DataTable();
		transp.setName(tblName);
		if (genName != null) {
			transp.setGenericNameOfEntity(genName);
		}

		ObjectFilter tFilter = tbl.getObjectFilter();
		if (tFilter != null && !tFilter.areObjectsFiltered()) {
			tFilter = null;
		}

		if (param == null) {
			for (int i = 0; i < tbl.getDataItemCount(); i++)
				if (tFilter == null || tFilter.isActive(i)) {
					ThematicDataItem data = (ThematicDataItem) tbl.getDataItem(i);
					transp.addAttribute(data.getName(), data.getId(), (hasNotNumbers) ? AttributeTypes.character : AttributeTypes.real);
				}
			for (int j = 0; j < attrNumbers.size(); j++) {
				int cN = attrNumbers.elementAt(j);
				DataRecord rec = new DataRecord(tbl.getAttributeId(cN), tbl.getAttributeName(cN));
				transp.addDataRecord(rec);
				for (int i = 0; i < tbl.getDataItemCount(); i++) {
					String valStr = tbl.getAttrValueAsString(cN, i);
					if (valStr == null) {
						continue;
					}
					if (tbl.isAttributeNumeric(cN)) {
						rec.setNumericAttrValue(tbl.getNumericAttrValue(cN, i), valStr, i);
					} else {
						rec.setAttrValue(valStr, i);
					}
				}
			}
		} else {
			Attribute atParVal = null;
			Object parVal = param.getFirstValue();
			if (parVal instanceof TimeMoment) {
				transp.setNatureOfItems(DataTable.NATURE_TIME);
				atParVal = new Attribute("xxx", AttributeTypes.time);
			} else if (parVal instanceof Integer) {
				atParVal = new Attribute("xxx", AttributeTypes.integer);
			} else if ((parVal instanceof Float) || (parVal instanceof Double)) {
				atParVal = new Attribute("xxx", AttributeTypes.real);
			} else {
				atParVal = new Attribute("xxx", AttributeTypes.character);
			}
			atParVal.setName(param.getName());
			atParVal.setIdentifier(IdMaker.makeId(param.getName(), transp));
			transp.addAttribute(atParVal);

			String oName = tbl.getGenericNameOfEntity();
			if (oName == null) {
				oName = "object";
			}
			oName = Dialogs.askForStringValue(core.getUI().getMainFrame(), "Generic name of an item described by a row in the source table?", oName, "One or more parameter-dependent attributes will be generated. "
					+ "The generic name of an item will be used as the name of the parameter.", "Parameter name?", false);
			if (tbl.getGenericNameOfEntity() == null) {
				tbl.setGenericNameOfEntity(oName);
			}
			Parameter param1 = new Parameter();
			param1.setName(oName);
			IntArray recNs = new IntArray(tbl.getDataItemCount(), 10);
			for (int i = 0; i < tbl.getDataItemCount(); i++)
				if (tFilter == null || tFilter.isActive(i)) {
					ThematicDataItem data = (ThematicDataItem) tbl.getDataItem(i);
					param1.addValue(data.getName());
					recNs.addElement(i);
				}
			transp.addParameter(param1);
			Vector<Attribute> trParents = new Vector<Attribute>(parents.size(), 1);
			for (int i = 0; i < parents.size(); i++) {
				Attribute parent0 = parents.elementAt(i);
				Attribute parent1 = new Attribute(IdMaker.makeId(parent0.getName(), transp), parent0.getType());
				parent1.setName(parent0.getName());
				trParents.addElement(parent1);
				int k = 0;
				for (int j = 0; j < param1.getValueCount(); j++) {
					Attribute at1 = new Attribute(parent1.getIdentifier() + (++k), parent1.getType());
					parent1.addChild(at1);
					at1.addParamValPair(param1.getName(), param1.getValue(j));
					transp.addAttribute(at1);
				}
			}
			for (int k = 0; k < param.getValueCount(); k++) {
				Object val = param.getValue(k);
				DataRecord rec = new DataRecord(val.toString(), val.toString());
				transp.addDataRecord(rec);
				if (!(val instanceof String)) {
					rec.setDescribedObject(val);
				}
				rec.setAttrValue(val, 0);
				for (int i = 0; i < trParents.size(); i++) {
					Attribute parent0 = parents.elementAt(i), parent1 = trParents.elementAt(i);
					for (int j = 0; j < parent0.getChildrenCount(); j++) {
						Attribute at0 = parent0.getChild(j);
						if (val.equals(at0.getParamValue(param.getName()))) {
							int cN0 = tbl.getAttrIndex(at0.getIdentifier());
							if (cN0 >= 0) {
								for (int n = 0; n < param1.getValueCount(); n++) {
									int recN0 = recNs.elementAt(n);
									String valStr = tbl.getAttrValueAsString(cN0, recN0);
									if (valStr == null) {
										continue;
									}
									Attribute at1 = parent1.getChild(n);
									int cN1 = transp.getAttrIndex(at1.getIdentifier());
									if (cN1 >= 0)
										if (parent1.isNumeric()) {
											rec.setNumericAttrValue(tbl.getNumericAttrValue(cN0, recN0), valStr, cN1);
										} else {
											rec.setAttrValue(valStr, cN1);
										}
								}
							}
						}
					}
				}
			}
		}
		DataLoader dLoader = core.getDataLoader();
		dLoader.addTable(transp);
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

}
