package spade.analysis.aggregates;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.analysis.plot.AttributeFreeTool;
import spade.analysis.plot.ObjectsSuitabilityChecker;
import spade.analysis.plot.QueryOrSearchTool;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.MultiSelector;
import spade.lib.util.IntArray;
import spade.vis.action.HighlightListener;
import spade.vis.database.CombinedFilter;
import spade.vis.database.DataItem;
import spade.vis.database.ObjectContainer;
import spade.vis.database.ObjectFilter;
import spade.vis.database.ObjectFilterBySelection;
import spade.vis.database.ThematicDataItem;
import spade.vis.database.ThematicDataOwner;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 21, 2008
 * Time: 3:14:59 PM
 * A UI for filtering aggregates by their members
 */
public class AggregateFilterUI extends Panel implements QueryOrSearchTool, AttributeFreeTool, ObjectsSuitabilityChecker, PropertyChangeListener, ItemListener, ActionListener, HighlightListener, Destroyable {
	/**
	 * The container of aggregates to be filtered
	 */
	protected AggregateContainer aCont = null;
	/**
	 * The container of the members
	 */
	protected ObjectContainer mCont = null;
	/**
	 * The filter of the aggregates
	 */
	protected AggregateFilter aggrFilter = null;
	/**
	 * The filter of the members (used optionally).
	 */
	protected ObjectFilterBySelection memberFilter = null;
	/**
	 * Propagates highlighting and selection of objects
	 */
	protected Supervisor supervisor = null;
	/**
	 * Used for the selection
	 */
	protected MultiSelector msel = null;
	/**
	 * Not all objects of the container mCont may be members of some aggregates.
	 * Objects that are not members of any aggregates should not be shown in the list.
	 * This array contains the indexes of the objects from the container that are
	 * actually included in aggregates.
	 */
	protected int mIdxs[] = null;
	/**
	 * Switches the filter on and off
	 */
	protected Checkbox filterCB = null;
	/**
	 * Indicate whether only aggregates containing all selected members
	 * or at least one member will be active
	 */
	protected Checkbox allCB = null, oneCB = null;
	/**
	 * Shows the number of currently active aggregates
	 */
	protected TextField activeCountTF = null;
	/**
	 * Switches on and off the filter of the members.
	 */
	protected Checkbox filterMembersCB = null;
	/**
	 * The error message
	 */
	protected String err = null;
	/**
	* The name of the component (used as the name of the corresponding tab)
	*/
	protected String name = null;

	/**
	 * Checks if the given container is suitable for this tool
	 */
	@Override
	public boolean isSuitable(ObjectContainer oCont) {
		if (oCont == null || oCont.getObjectCount() < 1)
			return false;
		if (!(oCont instanceof AggregateContainer))
			return false;
		AggregateContainer aggCont = (AggregateContainer) oCont;
		ObjectContainer mbCont = aggCont.getMemberContainer();
		if (mbCont == null || mbCont.getObjectCount() < 1)
			return false;
		return true;
	}

	/**
	* Sets a reference to the ObjectContainer this tool must work with
	*/
	@Override
	public void setObjectContainer(ObjectContainer oCont) {
		if (oCont == null || oCont.getObjectCount() < 1) {
			err = "No objects to filter!";
			return;
		}
		if (!(oCont instanceof AggregateContainer)) {
			err = "The tool requires a container (e.g. map layer) with aggregates (e.g. interactions)!";
			return;
		}
		aCont = (AggregateContainer) oCont;
		mCont = aCont.getMemberContainer();
		if (mCont == null || mCont.getObjectCount() < 1) {
			err = "No reference to a container with the aggregate members found!";
			return;
		}
	}

	/**
	* Returns the reference to the ObjectContainer this tool works with
	*/
	@Override
	public ObjectContainer getObjectContainer() {
		return aCont;
	}

	/**
	 * Must set the identifiers of the attributes to be used in the tool.
	 * This tool does not use any attributes.
	 */
	@Override
	public void setAttributeList(Vector attr) {
	}

	/**
	* If could not construct itself, returns the error message explaining the
	* reason of the failure
	*/
	@Override
	public String getErrorMessage() {
		return err;
	}

	/**
	* Constructs the tool using the earlier provided references to the supervisor
	* and the object container. Returns true if successfully constructed.
	*/
	@Override
	public boolean construct() {
		if (aCont == null || aCont.getObjectCount() < 1) {
			if (err == null) {
				err = "No container of aggregates provided!";
			}
			return false;
		}
		mCont = aCont.getMemberContainer();
		if (mCont == null || mCont.getObjectCount() < 1) {
			if (err == null) {
				err = "No container of aggregate members found!";
			}
			return false;
		}
		Vector aggregates = aCont.getAggregates();
		if (aggregates == null || aggregates.size() < 1) {
			if (err == null) {
				err = "No aggregates exist!";
			}
			return false;
		}
		IntArray mIdxsIA = new IntArray(mCont.getObjectCount(), 10);
		for (int i = 0; i < mCont.getObjectCount(); i++) {
			String oId = mCont.getObjectId(i);
			boolean isAggMember = false;
			for (int j = 0; j < aggregates.size() && !isAggMember; j++) {
				Aggregate ag = (Aggregate) aggregates.elementAt(j);
				isAggMember = AggregateMember.findMemberById(ag.getAggregateMembers(), oId) >= 0;
			}
			if (isAggMember) {
				mIdxsIA.addElement(i);
			}
		}
		if (mIdxsIA.size() < 1) {
			if (err == null) {
				err = "No aggregate members found!";
			}
			return false;
		}
		mIdxs = mIdxsIA.getTrimmedArray();
		getAggregateFilter();
		if (aggrFilter == null) {
			err = "Could not generate a filter!";
			return false;
		}
		Vector selMemberIds = null;
		IntArray selMemberNums = null;
		if (aggrFilter.areObjectsFiltered()) {
			selMemberIds = aggrFilter.getActiveMemberIds();
			if (selMemberIds != null && selMemberIds.size() > 0) {
				selMemberNums = new IntArray(selMemberIds.size(), 1);
			}
		}
		Vector names = new Vector(mIdxs.length);
		int nameColN = -1;
		for (int i = 0; i < mIdxs.length; i++) {
			DataItem dit = mCont.getObjectData(mIdxs[i]);
			String name = dit.getName();
			if ((name == null || name.equals(dit.getId())) && (i == 0 || nameColN >= 0)) {
				ThematicDataItem data = getThematicDataItem(dit);
				if (data != null) {
					if (i == 0) {
						for (int j = 0; j < data.getAttrCount() && nameColN < 0; j++)
							if (data.getAttributeId(j).equalsIgnoreCase("name") || data.getAttributeName(j).equalsIgnoreCase("name")) {
								nameColN = j;
							}
					}
					if (nameColN >= 0) {
						name = dit.getId() + " (" + data.getAttrValueAsString(nameColN) + ")";
					}
				}
			}
			if (name == null) {
				name = dit.getId();
			}
			names.addElement(name);
			if (selMemberNums != null && selMemberIds.contains(dit.getId())) {
				selMemberNums.addElement(i);
			}
		}
		msel = new MultiSelector(names, false);
		msel.addSelectionChangeListener(this);
		setLayout(new BorderLayout());
		add(msel, BorderLayout.CENTER);
		Panel p = new Panel(new GridLayout(3, 1));
		p.add(new Label("Filtering of aggregates by their members", Label.CENTER));
		p.add(new Label("Aggregates: " + aCont.getName()));
		p.add(new Label("Members: " + mCont.getName()));
		add(p, BorderLayout.NORTH);
		CheckboxGroup cbg = new CheckboxGroup();
		p = new Panel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		filterCB = new Checkbox("Select aggregates containing", aggrFilter.areObjectsFiltered());
		filterCB.addItemListener(this);
		p.add(filterCB);
		Panel pp = new Panel(new GridLayout(2, 1));
		oneCB = new Checkbox("at least one of", !aggrFilter.mustHaveAllSelectedMembers(), cbg);
		oneCB.addItemListener(this);
		pp.add(oneCB);
		allCB = new Checkbox("all", aggrFilter.mustHaveAllSelectedMembers(), cbg);
		allCB.addItemListener(this);
		pp.add(allCB);
		p.add(pp);
		p.add(new Label("selected members"));
		pp = new Panel(new ColumnLayout());
		pp.add(p);
		p = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		p.add(new Label("Active:"));
		activeCountTF = new TextField(String.valueOf(aCont.getObjectCount()), 4);
		activeCountTF.setEditable(false);
		p.add(activeCountTF);
		p.add(new Label("aggregates out of " + aCont.getObjectCount()));
		pp.add(p);
		filterMembersCB = new Checkbox("Apply filtering to members of the aggregates", false);
		filterMembersCB.addItemListener(this);
		pp.add(filterMembersCB);
		p = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 2));
		Button b = new Button("Highlight members of currently active aggregates");
		b.setActionCommand("hl_members");
		b.addActionListener(this);
		p.add(b);
		pp.add(p);
		add(pp, BorderLayout.SOUTH);
		if (aggrFilter.areObjectsFiltered()) {
			showNActiveAggregates();
			if (selMemberNums != null && selMemberNums.size() > 0) {
				msel.selectItems(selMemberNums.getTrimmedArray());
			}
		}
		return true;
	}

	private ThematicDataItem getThematicDataItem(DataItem data) {
		if (data == null)
			return null;
		if (data instanceof ThematicDataItem)
			return (ThematicDataItem) data;
		if (data instanceof ThematicDataOwner)
			return ((ThematicDataOwner) data).getThematicData();
		return null;
	}

	/**
	* Sets a reference to the system's supervisor used for propagating events
	* among system's components.
	*/
	@Override
	public void setSupervisor(Supervisor sup) {
		supervisor = sup;
		if (supervisor != null && aCont != null && mCont != null) {
			supervisor.registerHighlightListener(this, aCont.getEntitySetIdentifier());
			supervisor.registerHighlightListener(this, mCont.getEntitySetIdentifier());
		}
	}

	/**
	 * Tries to find an existing instance of AggregateFilter for its AggregateContainer.
	 * If not found, creates such a filter.
	 */
	protected void getAggregateFilter() {
		if (aggrFilter != null)
			return;
		if (aCont == null || aCont.getObjectCount() < 1)
			return;
		ObjectFilter oFilter = aCont.getObjectFilter();
		if (oFilter != null)
			if (oFilter instanceof AggregateFilter) {
				aggrFilter = (AggregateFilter) oFilter;
			} else if (oFilter instanceof CombinedFilter) {
				CombinedFilter cFilter = (CombinedFilter) oFilter;
				for (int i = 0; i < cFilter.getFilterCount() && aggrFilter == null; i++)
					if (cFilter.getFilter(i) instanceof AggregateFilter) {
						aggrFilter = (AggregateFilter) cFilter.getFilter(i);
					}
			}
		if (aggrFilter != null)
			return;
		aggrFilter = new AggregateFilter();
		aggrFilter.setObjectContainer(aCont);
		aggrFilter.setEntitySetIdentifier(aCont.getEntitySetIdentifier());
		aCont.setObjectFilter(aggrFilter);
	}

	protected void showNActiveAggregates() {
		if (!aggrFilter.areObjectsFiltered()) {
			activeCountTF.setText(String.valueOf(aCont.getObjectCount()));
		} else {
			IntArray active = aggrFilter.getActiveAggregatesIndexes();
			if (active != null) {
				activeCountTF.setText(String.valueOf(active.size()));
			} else {
				activeCountTF.setText("0");
			}
		}
	}

	protected void sendSelectedMembersToFilter() {
		int selIdxs[] = msel.getSelectedIndexes();
		Vector selIds = null;
		if (selIdxs != null && selIdxs.length > 0) {
			selIds = new Vector(selIdxs.length, 1);
			for (int selIdx : selIdxs) {
				selIds.addElement(mCont.getObjectId(mIdxs[selIdx]));
			}
		}
		aggrFilter.setActiveMembers(selIds);
		filterMembersIfNeeded(selIdxs);
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource().equals(msel)) {
			if (filterCB.getState()) {
				sendSelectedMembersToFilter();
				showNActiveAggregates();
			}
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(filterMembersCB)) {
			filterMembersIfNeeded(null);
			return;
		}
		if (e.getSource().equals(filterCB)) {
			if (filterCB.getState()) {
				sendSelectedMembersToFilter();
			} else {
				aggrFilter.clearFilter();
			}
		} else if (e.getSource().equals(oneCB) && oneCB.getState()) {
			aggrFilter.setMustHaveAllSelectedMembers(false);
		} else if (e.getSource().equals(allCB) && allCB.getState()) {
			aggrFilter.setMustHaveAllSelectedMembers(true);
		}
		showNActiveAggregates();
	}

	protected void getFilterBySelection() {
		if (memberFilter != null)
			return;
		if (mCont == null)
			return;
		memberFilter = new ObjectFilterBySelection();
		memberFilter.setObjectContainer(mCont);
		memberFilter.setEntitySetIdentifier(mCont.getEntitySetIdentifier());
		mCont.setObjectFilter(memberFilter);
	}

	protected void filterMembersIfNeeded(int selMemberIdxs[]) {
		if (!filterMembersCB.getState()) {
			if (memberFilter != null) {
				memberFilter.clearFilter();
			}
			return;
		}
		getFilterBySelection();
		if (selMemberIdxs == null) {
			selMemberIdxs = msel.getSelectedIndexes();
		}
		if (selMemberIdxs != null && selMemberIdxs.length > 0) {
			for (int i = 0; i < selMemberIdxs.length; i++) {
				selMemberIdxs[i] = selMemberIdxs[mIdxs[i]];
			}
		}
		memberFilter.setActiveObjectIndexes(selMemberIdxs);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() != null && e.getActionCommand().equals("hl_members")) {
			selectMembersOfActiveAggregates();
		}
	}

	protected void selectMembersOfActiveAggregates() {
		ObjectFilter oFilter = aCont.getObjectFilter();
		if (oFilter == null || !oFilter.areObjectsFiltered())
			return;
		Vector aggregates = aCont.getAggregates();
		Vector amids = new Vector(aggregates.size() * 3, 50);
		for (int i = 0; i < aggregates.size(); i++)
			if (oFilter.isActive(i)) {
				Aggregate aggr = (Aggregate) aggregates.elementAt(i);
				for (int j = 0; j < aggr.getMemberCount(); j++) {
					String id = aggr.getMemberId(j);
					if (!amids.contains(id)) {
						amids.addElement(id);
					}
				}
			}
		supervisor.getHighlighter(mCont.getEntitySetIdentifier()).replaceSelectedObjects(this, amids);
	}

	/**
	* Notification about change of the set of objects to be transiently
	* highlighted. The argument "source" is usually a reference to a
	* Highlighter. The argument setId is the identifier of the set the
	* highlighted objects belong to (e.g. map layer or table).
	* The argument highlighted is a vector of identifiers of
	* currently highlighted objects.
	*/
	@Override
	public void highlightSetChanged(Object source, String setId, Vector highlighted) {
	}

	/**
	* Notification about change of the set of objects to be selected (durably
	* highlighted). The argument "source" is usually a reference to a
	* Highlighter. The argument setId is the identifier of the set the
	* highlighted objects belong to (e.g. map layer or table).
	* The argument selected is a vector of identifiers of
	* currently selected objects.
	*/
	@Override
	public void selectSetChanged(Object source, String setId, Vector selected) {
		if (setId.equals(aCont.getEntitySetIdentifier())) {
			//highlight the members of the selected aggregates in the list(s)
			if (selected == null || selected.size() < 1) {
				msel.highlightItems(null);
			} else {
				Vector aggregates = aCont.getAggregates();
				if (aggregates == null)
					return;
				IntArray hlMembers = new IntArray(50, 50);
				for (int i = 0; i < aggregates.size(); i++) {
					Aggregate aggr = (Aggregate) aggregates.elementAt(i);
					if (!selected.contains(aggr.getIdentifier())) {
						continue;
					}
					Vector members = aggr.getAggregateMembers();
					if (members == null || members.size() < 1) {
						continue;
					}
					for (int j = 0; j < members.size(); j++) {
						AggregateMember member = (AggregateMember) members.elementAt(j);
						int idx = mCont.getObjectIndex(member.id);
						if (idx >= 0 && hlMembers.indexOf(idx) < 0) {
							hlMembers.addElement(idx);
						}
					}
				}
				if (hlMembers.size() > 0) {
					int hlAr[] = hlMembers.getTrimmedArray();
					for (int i = 0; i < hlAr.length; i++) {
						hlAr[i] = IntArray.indexOf(hlAr[i], mIdxs);
					}
					msel.highlightItems(hlAr);
				}
			}
		} else if (setId.equals(mCont.getEntitySetIdentifier())) {
			//highlight the members of the selected aggregates in the list(s)
			if (selected == null || selected.size() < 1) {
				msel.highlightItems(null);
			} else {
				IntArray hlMembers = new IntArray(selected.size(), 50);
				for (int i = 0; i < mCont.getObjectCount(); i++)
					if (selected.contains(mCont.getObjectId(i))) {
						hlMembers.addElement(i);
					}
				if (hlMembers.size() > 0) {
					int hlAr[] = hlMembers.getTrimmedArray();
					for (int i = 0; i < hlAr.length; i++) {
						hlAr[i] = IntArray.indexOf(hlAr[i], mIdxs);
					}
					msel.highlightItems(hlAr);
				}
			}
		}
	}

	@Override
	public String getName() {
		if (name != null)
			return name;
		name = "Aggregate filter";
		if (aCont != null) {
			name += ": " + aCont.getName();
		}
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	protected boolean destroyed = false;

	@Override
	public void destroy() {
		if (destroyed)
			return;
		if (mCont != null && memberFilter != null) {
			mCont.removeObjectFilter(memberFilter);
			memberFilter.destroy();
			memberFilter = null;
		}
		if (supervisor != null && aCont != null && mCont != null) {
			supervisor.removeHighlightListener(this, aCont.getEntitySetIdentifier());
			supervisor.removeHighlightListener(this, mCont.getEntitySetIdentifier());
		}
		if (aggrFilter != null) {
			aCont.removeObjectFilter(aggrFilter);
			aggrFilter.destroy();
		}
		destroyed = true;
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}
}
