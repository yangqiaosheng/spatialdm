package spade.analysis.tools.table_processing;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.BaseAnalyser;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.TextCanvas;
import spade.lib.util.DoubleArray;
import spade.lib.util.IdUtil;
import spade.lib.util.IntArray;
import spade.lib.util.NumValManager;
import spade.lib.util.StringUtil;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataItem;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.ObjectContainer;
import spade.vis.database.SpatialDataItem;
import spade.vis.dmap.DGeoLayer;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import ui.AttributeChooser;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Nov 2, 2010
 * Time: 3:47:05 PM
 * For a selected set of objects, computes statistics of attribute values
 * from related objects of the same or another object set. It is assumed that
 * the second object set is described by a table in which one of the columns
 * contains references to objects of the first set (i.e. identifiers of
 * those objects).  
 */
public class RelatedObjectsStatisticsComputer extends BaseAnalyser {
	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	@Override
	public void run(ESDACore core) {
		this.core = core;
		DataKeeper dk = core.getDataKeeper();
		Vector<ObjectContainer> containers = new Vector<ObjectContainer>(20, 10);
		LayerManager lman = dk.getMap(0);
		if (lman != null) {
			for (int i = 0; i < lman.getLayerCount(); i++) {
				GeoLayer layer = lman.getGeoLayer(i);
				if (!(layer instanceof ObjectContainer)) {
					continue;
				}
				if (layer.getObjectCount() < 1) {
					continue;
				}
				String setId = layer.getEntitySetIdentifier();
				if (setId == null) {
					continue;
				}
				boolean sameSetId = false;
				for (int j = 0; j < containers.size() && !sameSetId; j++) {
					sameSetId = setId.equals(containers.elementAt(j).getEntitySetIdentifier());
				}
				if (!sameSetId) {
					containers.addElement((ObjectContainer) layer);
				}
			}
		}
		for (int i = 0; i < dk.getTableCount(); i++) {
			AttributeDataPortion table = dk.getTable(i);
			if (!(table instanceof ObjectContainer)) {
				continue;
			}
			if (table.getDataItemCount() < 1) {
				continue;
			}
			String setId = table.getEntitySetIdentifier();
			if (setId == null) {
				continue;
			}
			boolean sameSetId = false;
			for (int j = 0; j < containers.size() && !sameSetId; j++) {
				sameSetId = setId.equals(containers.elementAt(j).getEntitySetIdentifier());
			}
			if (!sameSetId) {
				containers.addElement((ObjectContainer) table);
			}
		}
		if (containers.size() < 2) {
			showMessage("Less than 2 different object sets found!", true);
			return;
		}
		Panel mainP = new Panel(new BorderLayout());
		TextCanvas tc = new TextCanvas();
		tc.addTextLine("This tool is used when there are two related sets of objects, which means that " + "one of the sets has a table with a column containing identifiers of objects from the other set.\n ");
		tc.addTextLine("For each object of one of the sets (Set 1), the tool will find all related " + "objects of the other set (Set 2) " + "and compute statistics of their attribute values: minimum, maximum, mean, standard deviation, "
				+ "median, quartiles, and sum. The statistics are put in the table describing Set 1 " + "as new attributes of the objects of Set 1.\n");
		mainP.add(tc, BorderLayout.NORTH);
		List list1 = new List(Math.min(10, containers.size())), list2 = new List(Math.min(10, containers.size()));
		for (int i = 0; i < containers.size(); i++) {
			list1.add(containers.elementAt(i).getName());
			list2.add(containers.elementAt(i).getName());
		}
		list1.select(0);
		list2.select(1);
		CheckboxGroup cbg = new CheckboxGroup();
		Panel p = new Panel(new GridLayout(1, 2));
		mainP.add(p, BorderLayout.CENTER);
		Panel pp = new Panel(new BorderLayout());
		pp.add(new Label("Set 1:"), BorderLayout.NORTH);
		pp.add(list1, BorderLayout.CENTER);
		Checkbox cbSet1HasRefs = new Checkbox("has references to objects of Set 2", false, cbg);
		pp.add(cbSet1HasRefs, BorderLayout.SOUTH);
		p.add(pp);
		pp = new Panel(new BorderLayout());
		pp.add(new Label("Set 2:"), BorderLayout.NORTH);
		pp.add(list2, BorderLayout.CENTER);
		Checkbox cbSet2HasRefs = new Checkbox("has references to objects of Set 1", false, cbg);
		pp.add(cbSet2HasRefs, BorderLayout.SOUTH);
		p.add(pp);
		boolean ok = false;
		OKDialog dia = new OKDialog(getFrame(), "Related objects statistics", true);
		dia.addContent(mainP);
		int idx1 = -1, idx2 = -1;
		while (!ok) {
			dia.show();
			if (dia.wasCancelled())
				return;
			idx1 = list1.getSelectedIndex();
			if (idx1 < 0) {
				showMessage("Set 1 has not been selected!", true);
			} else {
				idx2 = list2.getSelectedIndex();
				if (idx2 < 0) {
					showMessage("Set 2 has not been selected!", true);
				} else if (!cbSet1HasRefs.getState() && !cbSet2HasRefs.getState()) {
					showMessage("Specify which object set has references to objects of the other set!", true);
				} else if (cbSet1HasRefs.getState()) {
					ok = containers.elementAt(idx1) instanceof AttributeDataPortion;
					if (!ok) {
						GeoLayer layer = (GeoLayer) containers.elementAt(idx1);
						ok = layer.getThematicData() != null;
					}
					if (!ok) {
						showMessage("Set 1 has no table (=> no references to objects of Set 2)!", true);
					}
				} else if (cbSet2HasRefs.getState()) {
					ok = containers.elementAt(idx2) instanceof AttributeDataPortion;
					if (!ok) {
						GeoLayer layer = (GeoLayer) containers.elementAt(idx2);
						ok = layer.getThematicData() != null;
					}
					if (!ok) {
						showMessage("Set 2 has no table (=> no references to objects of Set 1)!", true);
					}
				}
			}
		}
		ObjectContainer oCont1 = containers.elementAt(idx1), oCont2 = containers.elementAt(idx2);
		AttributeDataPortion table1 = null, table2 = null;
		if (oCont1 instanceof AttributeDataPortion) {
			table1 = (AttributeDataPortion) oCont1;
		} else {
			table1 = ((GeoLayer) oCont1).getThematicData();
		}
		if (oCont2 instanceof AttributeDataPortion) {
			table2 = (AttributeDataPortion) oCont2;
		} else {
			table2 = ((GeoLayer) oCont2).getThematicData();
		}
		boolean set1HasRefs = cbSet1HasRefs.getState();
		AttributeDataPortion table = (set1HasRefs) ? table1 : table2;
		List list = new List(10);
		for (int i = 0; i < table.getAttrCount(); i++) {
			list.add(table.getAttributeName(i));
		}
		list.select(0);
		mainP = new Panel(new BorderLayout());
		p = new Panel(new ColumnLayout());
		mainP.add(p, BorderLayout.NORTH);
		p.add(new Label("Which column of the table"));
		p.add(new Label(table.getName(), Label.CENTER));
		p.add(new Label("contains identifiers of objects from"));
		p.add(new Label(((set1HasRefs) ? oCont2.getName() : oCont1.getName()) + "?", Label.CENTER));
		p.add(new Label("Select the column:"));
		mainP.add(list, BorderLayout.CENTER);
		dia = new OKDialog(getFrame(), "Object identifiers", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return;
		int colN = list.getSelectedIndex();
		if (colN < 0)
			return;
		int numCols[] = null;
		String statNames[] = { "count of related objects", "identifiers of related objects", "Minimum", "Maximum", "Sum", "Mean", "Standard deviation", "Median", "1st quartile", "3rd quartile" };
		boolean statToCount[] = { false, false, false, false, false, false, false, false, false, false };
		if (table2 == null) {
			if (!Dialogs.askYesOrNo(getFrame(), "Objects of set 2 have no table with attributes" + " that could be used for computing statistics. Do you wish to obtain just " + "counts of the related objects and lists of their identifiers?",
					"No attributes!"))
				return;
			statToCount[0] = true;
			statToCount[1] = !set1HasRefs;
		}
		if (table2 != null) {
			//the user chooses the numeric attributes from which statistics is required
			AttributeChooser attrSel = new AttributeChooser();
			attrSel.selectColumns(table2, null, null, true, "Select one or more numeric attributes " + "for computing statistics for objects in " + oCont1.getName(), core.getUI());
			Vector attrIds = attrSel.getSelectedColumnIds();
			if (attrIds != null && attrIds.size() > 0) {
				numCols = new int[attrIds.size()];
				for (int i = 0; i < attrIds.size(); i++) {
					numCols[i] = table2.getAttrIndex((String) attrIds.elementAt(i));
				}
				p = new Panel(new ColumnLayout());
				p.add(new Label("Which statistics you wish to obtain?"));
				Checkbox cb[] = new Checkbox[statNames.length];
				for (int i = 0; i < statNames.length; i++) {
					cb[i] = new Checkbox(statNames[i], false);
					p.add(cb[i]);
				}
				dia = new OKDialog(getFrame(), "Required statistics?", true);
				dia.addContent(p);
				dia.show();
				if (dia.wasCancelled())
					return;
				boolean someSelected = false;
				for (int i = 0; i < cb.length; i++) {
					statToCount[i] = cb[i].getState();
					someSelected = someSelected || statToCount[i];
				}
				if (!someSelected) {
					showMessage("Required statistics have not been selected!", true);
					return;
				}
			} else {
				if (!Dialogs.askYesOrNo(getFrame(), "You have not select any numeric attributes " + "for computing statistics. Do you wish to obtain just " + "counts of the related objects and lists of their identifiers?",
						"No numeric attributes selected!"))
					return;
				statToCount[0] = true;
				statToCount[1] = !set1HasRefs;
			}
		}

		showMessage("Wait... Retrieving links between objects of two sets", false);
		int links[][] = getLinks(oCont1, oCont2, set1HasRefs, table, colN);
		if (links == null) {
			showMessage("No links between the objects of the two sets found!", true);
			return;
		}
		showMessage(null, false);
		//if table 1 does not exist, create it
		if (table1 == null || !(table1 instanceof DataTable)) {
			table1 = new DataTable();
			table1.setName("Characteristics of " + oCont1.getName());
			DGeoLayer layer = (oCont1 instanceof DGeoLayer) ? (DGeoLayer) oCont1 : null;
			for (int i = 0; i < oCont1.getObjectCount(); i++) {
				DataRecord rec = new DataRecord(oCont1.getObjectId(i), oCont1.getObjectData(i).getName());
				((DataTable) table1).addDataRecord(rec);
				if (layer != null) {
					layer.getObject(i).setThematicData(rec);
				}
			}
			int tN = core.getDataLoader().addTable((DataTable) table1);
			if (layer != null) {
				layer.setDataTable(table1);
				layer.setLinkedToTable(true);
				core.getDataLoader().setLink(layer, tN);
			}
		}
		//make new attributes in table 1
		int aIdx0 = table1.getAttrCount();
		int aIdxs[] = new int[statToCount.length];
		for (int i = 0; i < aIdxs.length; i++) {
			aIdxs[i] = -1;
		}
		if (statToCount[0]) {
			aIdxs[0] = table1.getAttrCount();
			table1.addAttribute("Count of related " + oCont2.getName(), "ev_count_" + aIdxs[0], AttributeTypes.integer);
		}
		if (statToCount[1]) {
			aIdxs[1] = table1.getAttrCount();
			table1.addAttribute("IDs of related " + oCont2.getName(), "ev_ids_" + aIdxs[1], AttributeTypes.character);
		}
		int nNumStat = 0;
		for (int i = 2; i < statToCount.length; i++)
			if (statToCount[i]) {
				++nNumStat;
			}
		if (nNumStat > 0) {
			for (int i = 2; i < statToCount.length; i++)
				if (statToCount[i]) {
					aIdxs[i] = table1.getAttrCount();
					table1.addAttribute(statNames[i] + " of " + table2.getAttributeName(numCols[0]) + " from related " + oCont2.getName(), statNames[i] + "_" + IdUtil.getPureAttrId(table2.getAttributeId(numCols[0])) + "_" + aIdxs[i],
							AttributeTypes.real);
				}
			for (int j = 1; j < numCols.length; j++) {
				for (int i = 2; i < statToCount.length; i++)
					if (statToCount[i]) {
						table1.addAttribute(statNames[i] + " of " + table2.getAttributeName(numCols[j]) + " from related " + oCont2.getName(),
								statNames[i] + "_" + IdUtil.getPureAttrId(table2.getAttributeId(numCols[j])) + "_" + table1.getAttrCount(), AttributeTypes.real);
					}
			}
		}
		DoubleArray dar = (nNumStat > 0) ? new DoubleArray(20, 10) : null;
		//fill the new table columns with the values
		showMessage("Wait... Statistics computation", false);
		for (int i = 0; i < links.length; i++) {
			if (links[i] == null && !statToCount[0]) {
				continue;
			}
			DataRecord rec = null;
			DataItem dit = oCont1.getObjectData(i);
			if (dit != null)
				if (dit instanceof DataRecord) {
					rec = (DataRecord) dit;
				} else if (dit instanceof SpatialDataItem) {
					dit = ((SpatialDataItem) dit).getThematicData();
					if (dit != null && (dit instanceof DataRecord)) {
						rec = (DataRecord) dit;
					}
				}
			if (rec == null) {
				continue; //this is hardly possible
			}
			if (links[i] == null) {
				rec.setNumericAttrValue(0, "0", aIdxs[0]);
			} else {
				if (statToCount[0]) {
					rec.setNumericAttrValue(links[i].length, String.valueOf(links[i].length), aIdxs[0]);
				}
				if (statToCount[1]) {
					String ids = (table2 != null) ? table2.getDataItemId(links[i][0]) : oCont2.getObjectId(links[i][0]);
					for (int j = 1; j < links[i].length; j++) {
						ids += ";" + ((table2 != null) ? table2.getDataItemId(links[i][j]) : oCont2.getObjectId(links[i][j]));
					}
					rec.setAttrValue(ids, aIdxs[1]);
				}
				if (nNumStat > 0) {
					for (int j = 0; j < numCols.length; j++) {
						dar.removeAllElements();
						for (int k = 0; k < links[i].length; k++) {
							double val = table2.getNumericAttrValue(numCols[j], links[i][k]);
							if (!Double.isNaN(val)) {
								dar.addElement(val);
							}
						}
						if (dar.size() < 1) {
							continue;
						}
						double min = dar.elementAt(0), max = min, sum = min;
						for (int k = 1; k < dar.size(); k++) {
							double val = dar.elementAt(k);
							if (max < val) {
								max = val;
							}
							if (min > val) {
								min = val;
							}
							sum += val;
						}
						if (statToCount[2]) {
							rec.setNumericAttrValue(min, aIdxs[2] + j * nNumStat);
						}
						if (statToCount[3]) {
							rec.setNumericAttrValue(max, aIdxs[3] + j * nNumStat);
						}
						if (statToCount[4]) {
							rec.setNumericAttrValue(sum, aIdxs[4] + j * nNumStat);
						}
						if (statToCount[5] || statToCount[6])
							if (dar.size() < 2) {
								if (statToCount[5]) {
									rec.setNumericAttrValue(sum, aIdxs[5] + j * nNumStat);
								}
								if (statToCount[6]) {
									rec.setNumericAttrValue(0, "0", aIdxs[6] + j * nNumStat);
								}
							} else {
								double mean = sum / dar.size();
								if (statToCount[5]) {
									rec.setNumericAttrValue(mean, aIdxs[5] + j * nNumStat);
								}
								double stD = 0f;
								for (int k = 0; k < dar.size(); k++) {
									stD += Math.pow(dar.elementAt(k) - mean, 2);
								}
								stD = Math.sqrt(stD / dar.size());
								if (statToCount[6]) {
									rec.setNumericAttrValue(stD, aIdxs[6] + j * nNumStat);
								}
							}
						if (statToCount[7] || statToCount[8] || statToCount[9]) {
							if (dar.size() < 3) {
								if (statToCount[7]) {
									rec.setNumericAttrValue(min, aIdxs[7] + j * nNumStat);
								}
							} else {
								IntArray iar = new IntArray(3, 1);
								if (statToCount[7]) {
									iar.addElement(50);
								}
								if (statToCount[8]) {
									iar.addElement(25);
								}
								if (statToCount[9]) {
									iar.addElement(75);
								}
								double vals[] = NumValManager.getPercentiles(dar, iar.getTrimmedArray());
								if (vals != null) {
									for (int k = 0; k < vals.length; k++)
										if (iar.elementAt(k) == 50) {
											rec.setNumericAttrValue(vals[k], aIdxs[7] + j * nNumStat);
										} else if (iar.elementAt(k) == 25) {
											rec.setNumericAttrValue(vals[k], aIdxs[8] + j * nNumStat);
										} else if (iar.elementAt(k) == 75) {
											rec.setNumericAttrValue(vals[k], aIdxs[9] + j * nNumStat);
										}
								}
							}
						}
					}
				}
			}
		}
		showMessage(null, false);
		//allow the user to edit the names of the new attributes
		Vector attr = new Vector(table1.getAttrCount() - aIdx0, 1);
		for (int i = aIdx0; i < table1.getAttrCount(); i++) {
			attr.addElement(table1.getAttribute(i));
		}
		String aNames[] = new String[attr.size()];
		for (int i = 0; i < aNames.length; i++) {
			aNames[i] = ((Attribute) attr.elementAt(i)).getName();
		}
		aNames = Dialogs.editStringValues(core.getUI().getMainFrame(), null, aNames, "Edit the names of the table attributes if needed", "Attribute names", true);
		if (aNames != null) {
			for (int i = 0; i < aNames.length; i++)
				if (aNames[i] != null) {
					((Attribute) attr.elementAt(i)).setName(aNames[i]);
				}
		}
		if (table1 instanceof DataTable) {
			((DataTable) table1).makeUniqueAttrIdentifiers();
			int colNs[] = new int[table1.getAttrCount() - aIdx0];
			for (int i = 0; i < colNs.length; i++) {
				colNs[i] = aIdx0 + i;
			}
			((DataTable) table1).setNiceStringsForNumbers(colNs, false);
		}
		for (int i = 0; i < attr.size(); i++) {
			attr.setElementAt(((Attribute) attr.elementAt(i)).getIdentifier(), i);
		}
		table.notifyPropertyChange("new_attributes", null, attr);
		showMessage("Computation finished!", false);
	}

	/**
	 * Retrieves the links between objects of the two sets.
	 * It is assumed that either the first or the second set has a table with a column containing
	 * identifiers of objects of the other set.
	 * @param oCont1 - the first set (container) of objects
	 * @param oCont2 - the second set (container) of objects
	 * @param tableBelongsToSet1 - true if set 1 has a table with a column containing references
	 *   to objects of set 2 (i.e. identifiers of these objects) and false if set 2 has a table
	 *   with references to objects of set 1.
	 * @param table - the table with the references to objects of set 2, if tableBelongsToSet1 is true,
	 *   or to objects of set 1, if tableBelongsToSet1 is false.
	 * @param colN - the column in the table containing identifiers of objects of set 2 or set 1,
	 *   depending on the value of tableBelongsToSet1
	 * @return  two-dimensional array: for each object of the first set contains an array of object indexes
	 *   in the second set related to this object
	 */
	protected int[][] getLinks(ObjectContainer oCont1, ObjectContainer oCont2, boolean tableBelongsToSet1, AttributeDataPortion table, int colN) {
		if (oCont1 == null || oCont1.getObjectCount() < 1 || oCont2 == null || oCont2.getObjectCount() < 1)
			return null;
		if (table == null || colN < 0 || !table.hasData())
			return null;
		AttributeDataPortion table2 = null;
		if (tableBelongsToSet1) {
			if (oCont2 instanceof AttributeDataPortion) {
				table2 = (AttributeDataPortion) oCont2;
			} else if (oCont2 instanceof GeoLayer) {
				table2 = ((GeoLayer) oCont2).getThematicData();
			}
		}
		IntArray depIAr[] = new IntArray[oCont1.getObjectCount()];
		boolean hasDependency = false;
		for (int i = 0; i < table.getDataItemCount(); i++) {
			String values = table.getAttrValueAsString(colN, i);
			if (values == null) {
				continue;
			}
			Vector ids = StringUtil.getNames(values, ";");
			if (ids == null || ids.size() < 1) {
				continue;
			}
			if (tableBelongsToSet1) {
				for (int j = 0; j < ids.size(); j++) {
					int oidx = (table2 != null) ? table2.indexOf((String) ids.elementAt(j)) : oCont2.getObjectIndex((String) ids.elementAt(j));
					if (oidx >= 0) {
						if (depIAr[i] == null) {
							depIAr[i] = new IntArray(50, 50);
							hasDependency = true;
						}
						depIAr[i].addElement(oidx);
					}
				}
			} else {
				for (int j = 0; j < ids.size(); j++) {
					int oidx = oCont1.getObjectIndex((String) ids.elementAt(j));
					if (oidx >= 0) {
						if (depIAr[oidx] == null) {
							depIAr[oidx] = new IntArray(50, 50);
							hasDependency = true;
						}
						depIAr[oidx].addElement(i);
					}
				}
			}
		}
		if (!hasDependency)
			return null;
		int dependencies[][] = new int[oCont1.getObjectCount()][];
		for (int i = 0; i < dependencies.length; i++) {
			dependencies[i] = (depIAr[i] == null) ? null : depIAr[i].getTrimmedArray();
		}
		return dependencies;
	}
}
