package spade.vis.database;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.time.TimeIntervalSelector;
import spade.time.TimeMoment;
import spade.vis.action.HighlightListener;
import spade.vis.action.Highlighter;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 27-Nov-2007
 * Time: 11:14:56
 * Aggregates DataItems into DataAggregates, computes statistics,
 * reacts to filtering of the original DataItems by recomputing the
 * statistics
 */
public class DataAggregator implements PropertyChangeListener, HighlightListener {
	/**
	 * A reference to a container from which the original (atomic) items are taken.
	 */
	public ObjectContainer itemCont = null;
	/**
	 * The filter of the original items
	 */
	protected ObjectFilter itemFilter = null;
	/**
	 * How the items are called, e.g. "visits", "events", etc.
	 */
	public String itemsName = "objects";
	/**
	 * The table with the aggregated data
	 */
	protected DataTable aggrTable = null;
	/**
	 * Indicates whether the identifiers of the original items must be included
	 * in the table with the aggregated data
	 */
	public boolean includeOrigItemsIds = true;
	/**
	 * Indicates whether the names of the original items must be included
	 * in the table with the aggregated data
	 */
	public boolean includeOrigItemsNames = true;
	/**
	 * Numbers of some columns in the table with the aggregated data
	 */
	public int totNumCN = -1, currNumCN = -1, allIdsCN = -1, currIdsCN = -1, allNamesCN = -1, currNamesCN = -1;

	protected Supervisor supervisor = null;

	protected boolean destroyed = false;

	/**
	 * Returns a reference to a container from which the original (atomic) items are taken.
	 */
	public ObjectContainer getItemContainer() {
		return itemCont;
	}

	/**
	 * Sets reference to a container from which the original (atomic) items are taken.
	 * Starts listening to filter change events.
	 */
	public void setItemContainer(ObjectContainer itemCont) {
		this.itemCont = itemCont;
		if (itemCont != null) {
			itemCont.addPropertyChangeListener(this);
			itemFilter = itemCont.getObjectFilter();
			if (itemFilter != null) {
				itemFilter.addPropertyChangeListener(this);
			}
			if (aggrTable != null) {
				aggrTable.setName(itemCont.getName() + " (aggregated)");
			}
		}
	}

	/**
	 * How the items are called, e.g. "visits", "events", etc.
	 */
	public void setItemsName(String itemsName) {
		this.itemsName = itemsName;
	}

	/**
	 * Sets whether the identifiers of the original items must be included
	 * in the table with the aggregated data
	 */
	public void setIncludeOrigItemsIds(boolean includeOrigItemsIds) {
		this.includeOrigItemsIds = includeOrigItemsIds;
	}

	/**
	 * Sets whether the names of the original items must be included
	 * in the table with the aggregated data
	 */
	public void setIncludeOrigItemsNames(boolean includeOrigItemsNames) {
		this.includeOrigItemsNames = includeOrigItemsNames;
	}

	public void setSupervisor(Supervisor supervisor) {
		this.supervisor = supervisor;
		if (supervisor != null && aggrTable != null) {
			supervisor.getHighlighter(aggrTable.getEntitySetIdentifier()).addHighlightListener(this);
		}
	}

	/**
	 * Returns the table with the aggregates
	 */
	public DataTable getAggregateDataTable() {
		return aggrTable;
	}

	/**
	 * Returns the current number of aggregates
	 */
	public int getAggregateCount() {
		if (aggrTable == null)
			return 0;
		return aggrTable.getDataItemCount();
	}

	/**
	 * Constructs a new aggregate with the given identifier and name and
	 * adds it to the table of aggregated data. Returns a reference to the
	 * constructed aggregate. If an aggregate with this identifier already
	 * exists, returns a reference to the existing aggregate.
	 */
	public DataAggregate addAggregate(String id, String name) {
		if (id == null) {
			id = String.valueOf(getAggregateCount() + 1);
		}
		DataAggregate aggr = getAggregate(id);
		if (aggr != null) {
			if (name != null) {
				aggr.setName(name);
			}
			return aggr;
		}
		aggr = new DataAggregate(id, name);
		if (aggrTable == null) {
			aggrTable = new DataTable();
			aggrTable.addPropertyChangeListener(this);
			if (itemCont != null) {
				aggrTable.setName(itemCont.getName() + " (aggregated)");
			} else {
				aggrTable.setName("Aggregated data about " + itemsName);
			}
			aggrTable.addAttribute("Total number of " + itemsName, "total_num", AttributeTypes.integer);
			totNumCN = aggrTable.getAttrCount() - 1;
			aggrTable.addAttribute("Current number of " + itemsName, "current_num", AttributeTypes.integer);
			currNumCN = aggrTable.getAttrCount() - 1;
			if (includeOrigItemsIds) {
				aggrTable.addAttribute("All " + itemsName + " (IDs)", "all_ids", AttributeTypes.character);
				allIdsCN = aggrTable.getAttrCount() - 1;
				aggrTable.addAttribute("Current " + itemsName + " (IDs)", "curr_ids", AttributeTypes.character);
				currIdsCN = aggrTable.getAttrCount() - 1;
			}
			if (includeOrigItemsNames) {
				aggrTable.addAttribute("All " + itemsName + " (names)", "all_names", AttributeTypes.character);
				allNamesCN = aggrTable.getAttrCount() - 1;
				aggrTable.addAttribute("Current " + itemsName + " (names)", "curr_names", AttributeTypes.character);
				currNamesCN = aggrTable.getAttrCount() - 1;
			}
		}
		aggrTable.addDataRecord(aggr);
		return aggr;
	}

	/**
	 * Finds an aggregate with the given identifier
	 */
	public DataAggregate getAggregate(String id) {
		if (id == null || aggrTable == null)
			return null;
		int idx = aggrTable.indexOf(id);
		if (idx < 0)
			return null;
		return (DataAggregate) aggrTable.getDataRecord(idx);
	}

	/**
	 * Attaches the given atomic item to the aggregate with the
	 * given identifier. If the aggregate does not exist yet,
	 * it is created.
	 */
	public void addItem(DataItem objData, String aggrId) {
		addItem(objData, aggrId, null, null);
	}

	/**
	 * Attaches a time-referenced occurrence of the given atomic item to the
	 * aggregate with the given identifier. If the aggregate does not exist yet,
	 * it is created.
	 */
	public void addItem(DataItem objData, String aggrId, TimeMoment startTime, TimeMoment endTime) {
		if (objData == null || aggrId == null)
			return;
		DataAggregate aggr = getAggregate(aggrId);
		if (aggr == null) {
			aggr = addAggregate(aggrId, null);
		}
		aggr.addItem(objData, startTime, endTime);
	}

	/**
	 * Collects filter-independent statistics and puts in the table
	 */
	public void fillStaticAggregateData() {
		if (aggrTable == null)
			return;
		for (int i = 0; i < aggrTable.getDataItemCount(); i++) {
			DataAggregate aggr = (DataAggregate) aggrTable.getDataRecord(i);
			Vector items = aggr.getAllDifferentItems();
			int nTotal = (items == null) ? 0 : items.size();
			aggr.setNumericAttrValue(nTotal, String.valueOf(nTotal), totNumCN);
			if (allIdsCN >= 0 || allNamesCN >= 0)
				if (nTotal < 1) {
					if (allIdsCN >= 0) {
						aggr.setAttrValue(null, allIdsCN);
					}
					if (allNamesCN >= 0) {
						aggr.setAttrValue(null, allNamesCN);
					}
				} else {
					if (allIdsCN >= 0) {
						String str = "";
						for (int j = 0; j < items.size(); j++) {
							DataItem dit = (DataItem) items.elementAt(j);
							if (j > 0) {
								str += ";";
							}
							str += dit.getId();
						}
						aggr.setAttrValue(str, allIdsCN);
					}
					if (allNamesCN >= 0) {
						String str = "";
						for (int j = 0; j < items.size(); j++) {
							DataItem dit = (DataItem) items.elementAt(j);
							String name = dit.getName();
							if (name == null) {
								name = dit.getId();
							}
							if (j > 0) {
								str += ";";
							}
							str += name;
						}
						aggr.setAttrValue(str, allNamesCN);
					}
				}
		}
	}

	/**
	* Finds the time filter of the table with the aggregated data, if available
	*/
	public TimeFilter getTimeFilterOfAggregatedData() {
		if (aggrTable == null)
			return null;
		ObjectFilter oFilter = aggrTable.getObjectFilter();
		if (oFilter == null)
			return null;
		if (oFilter instanceof TimeFilter)
			return (TimeFilter) oFilter;
		if (oFilter instanceof CombinedFilter) {
			CombinedFilter cFilter = (CombinedFilter) oFilter;
			for (int i = cFilter.getFilterCount() - 1; i >= 0; i--)
				if (cFilter.getFilter(i) instanceof TimeFilter)
					return (TimeFilter) cFilter.getFilter(i);
		}
		return null;
	}

	/**
	 * Collects statistics for currently active original items and puts in the table
	 */
	public void accountForFilter() {
		if (aggrTable == null || aggrTable.getDataItemCount() < 1)
			return;
		TimeFilter timeFilter = getTimeFilterOfAggregatedData();
		for (int i = 0; i < aggrTable.getDataItemCount(); i++) {
			DataAggregate aggr = (DataAggregate) aggrTable.getDataRecord(i);
/*
      if (aggr.getId().equals("24")) {
        System.out.println(aggr.getId());
      }
*/
			if (timeFilter != null && !timeFilter.isActive(i)) {
				aggr.setNumericAttrValue(0, "0", currNumCN);
				if (currIdsCN >= 0) {
					aggr.setAttrValue(null, currIdsCN);
				}
				if (currNamesCN >= 0) {
					aggr.setAttrValue(null, currNamesCN);
				}
				continue;
			}
			aggr.accountForFilter(itemFilter, timeFilter);
			Vector items = aggr.getDifferentActiveItems();
			int nActive = (items == null) ? 0 : items.size();
			aggr.setNumericAttrValue(nActive, String.valueOf(nActive), currNumCN);
			if (currIdsCN >= 0 || currNamesCN >= 0)
				if (nActive < 1) {
					if (currIdsCN >= 0) {
						aggr.setAttrValue(null, currIdsCN);
					}
					if (currNamesCN >= 0) {
						aggr.setAttrValue(null, currNamesCN);
					}
				} else {
					if (currIdsCN >= 0) {
						String str = "";
						for (int j = 0; j < items.size(); j++) {
							DataItem dit = (DataItem) items.elementAt(j);
							if (j > 0) {
								str += ";";
							}
							str += dit.getId();
						}
						aggr.setAttrValue(str, currIdsCN);
					}
					if (currNamesCN >= 0) {
						String str = "";
						for (int j = 0; j < items.size(); j++) {
							DataItem dit = (DataItem) items.elementAt(j);
							String name = dit.getName();
							if (name == null) {
								name = dit.getId();
							}
							if (j > 0) {
								str += ";";
							}
							str += name;
						}
						aggr.setAttrValue(str, currNamesCN);
					}
				}
		}
		Vector attr = new Vector(5, 5);
		attr.addElement(aggrTable.getAttributeId(currNumCN));
		if (currIdsCN >= 0) {
			attr.addElement(aggrTable.getAttributeId(currIdsCN));
		}
		if (currNamesCN >= 0) {
			attr.addElement(aggrTable.getAttributeId(currNamesCN));
		}
		aggrTable.notifyPropertyChange("values", null, attr);
	}

	/**
	* Reaction to changes of object filter of the original objects
	*/
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource().equals(itemCont)) {
			if (pce.getPropertyName().equalsIgnoreCase("destroyed")) {
				destroy();
			} else if (pce.getPropertyName().equalsIgnoreCase("filter")) {
				if (itemFilter != null) {
					itemFilter.removePropertyChangeListener(this);
				}
				itemFilter = itemCont.getObjectFilter();
				if (itemFilter != null) {
					itemFilter.addPropertyChangeListener(this);
				}
				accountForFilter();
			}
		} else if (pce.getSource().equals(itemFilter)) {
			if (pce.getPropertyName().equalsIgnoreCase("destroyed")) {
				itemFilter.removePropertyChangeListener(this);
				itemFilter = null;
			} else {
				accountForFilter();
			}
		} else if (pce.getSource().equals(aggrTable)) {
			if (pce.getPropertyName().equalsIgnoreCase("destroyed")) {
				destroy();
			}
		} else if (pce.getSource() instanceof TimeIntervalSelector) {
			accountForFilter();
		}
	}

	/**
	* Removes itself from listeners of the item filter and the item container
	*/
	public void destroy() {
		if (destroyed)
			return;
		if (supervisor != null && aggrTable != null) {
			supervisor.getHighlighter(aggrTable.getEntitySetIdentifier()).removeHighlightListener(this);
		}
		if (itemFilter != null) {
			itemFilter.removePropertyChangeListener(this);
		}
		if (itemCont != null) {
			itemCont.removePropertyChangeListener(this);
		}
		destroyed = true;
	}

	@Override
	public void highlightSetChanged(Object source, String setId, Vector highlighted) {
	}

	@Override
	public void selectSetChanged(Object source, String setId, Vector selected) {
		if (aggrTable == null || itemCont == null || supervisor == null)
			return;
		if (!setId.equalsIgnoreCase(aggrTable.getEntitySetIdentifier()))
			return;
		Highlighter hl = supervisor.getHighlighter(itemCont.getEntitySetIdentifier());
		if (hl == null)
			return;
		if (selected == null || selected.size() < 1) {
			hl.clearSelection(this);
		} else {
			Vector v = new Vector(50, 50);
			for (int i = 0; i < selected.size(); i++) {
				int idx = aggrTable.indexOf((String) selected.elementAt(i));
				if (idx < 0) {
					continue;
				}
				DataAggregate aggr = (DataAggregate) aggrTable.getDataRecord(idx);
				Vector activeItems = aggr.getDifferentActiveItems();
				if (activeItems == null) {
					continue;
				}
				for (int j = 0; j < activeItems.size(); j++) {
					DataRecord rec = (DataRecord) activeItems.elementAt(j);
					if (!v.contains(rec.getId())) {
						v.addElement(rec.getId());
					}
				}
			}
			if (v.size() < 1) {
				hl.clearSelection(this);
			} else {
				hl.replaceSelectedObjects(this, v);
			}
		}
	}
}
