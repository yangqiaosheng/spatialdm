package spade.analysis.tools.moves;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.analysis.tools.BaseAnalyser;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.BubbleSort;
import spade.lib.util.IdMaker;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.time.Date;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.time.TimeUtil;
import spade.time.ui.EnterDateSchemeUI;
import spade.time.ui.SetTimeBreaksUI;
import spade.time.ui.TimeDialogs;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.ObjectFilter;
import spade.vis.database.Parameter;
import spade.vis.database.TimeFilter;
import spade.vis.dmap.DAggregateLinkLayer;
import spade.vis.dmap.DAggregateLinkObject;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DLinkObject;
import spade.vis.dmap.DMovingObject;
import spade.vis.dmap.DPlaceVisitsLayer;
import spade.vis.dmap.DPlaceVisitsObject;
import spade.vis.dmap.PlaceVisitInfo;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jul 10, 2009
 * Time: 4:07:42 PM
 * For a layer with dynamic aggregates such as aggregated moves or generalized positions
 * creates a group of attributes in the table with values for clusters or classes of the
 * aggregated objects of for different time intervals.
 */
public class AggregatesItemizer extends BaseAnalyser implements spade.lib.util.Comparator {
	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	public void run(ESDACore core) {
		if (core == null || core.getUI() == null)
			return;
		this.core = core;
		if (core.getUI().getCurrentMapViewer() == null || core.getUI().getCurrentMapViewer().getLayerManager() == null) {
			showMessage("No map exists!", true);
			return;
		}
		//find suitable map layers:
		//1) instanceof DAggregateLinkLayer
		//2) instanceof DPlaceVisitsLayer
		//the layers must have tables with thematic data
		LayerManager lman = core.getUI().getCurrentMapViewer().getLayerManager();
		Vector<DGeoLayer> aggLayers = new Vector(lman.getLayerCount(), 1);
		for (int i = 0; i < lman.getLayerCount(); i++) {
			GeoLayer layer = lman.getGeoLayer(i);
			if (layer.getObjectCount() < 1) {
				continue;
			}
			if (layer instanceof DAggregateLinkLayer) {
				DAggregateLinkLayer agLayer = (DAggregateLinkLayer) layer;
				if (!agLayer.hasThematicData()) {
					continue;
				}
				if (agLayer.getTrajectoryLayer() == null) {
					continue;
				}
				aggLayers.addElement(agLayer);
			} else if (layer instanceof DPlaceVisitsLayer) {
				DPlaceVisitsLayer agLayer = (DPlaceVisitsLayer) layer;
				if (!agLayer.hasThematicData()) {
					continue;
				}
				if (agLayer.getTrajectoryLayer() == null) {
					continue;
				}
				aggLayers.addElement(agLayer);
			}
		}
		if (aggLayers.size() < 1) {
			showMessage("No appropriate layers with aggregate moves or areas found!", true);
			return;
		}
		Panel mainP = new Panel(new ColumnLayout());
		mainP.add(new Label("Select the layer with aggregate moves or areas:"));
		List list = new List(Math.max(aggLayers.size() + 1, 5));
		for (int i = 0; i < aggLayers.size(); i++) {
			list.add(aggLayers.elementAt(i).getName());
		}
		list.select(0);
		mainP.add(list);
		CheckboxGroup cbg = new CheckboxGroup();
		Checkbox byClassCB = new Checkbox("itemize by clusters (classes) of trajectories", true, cbg);
		mainP.add(byClassCB);
		Checkbox byTimeCB = new Checkbox("itemize by time intervals", false, cbg);
		mainP.add(byTimeCB);
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Itemize aggregates", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return;
		int idx = list.getSelectedIndex();
		if (idx < 0)
			return;

		DGeoLayer trLayer = null;
		DAggregateLinkLayer agLayer = null;
		DPlaceVisitsLayer pvLayer = null;
		if (aggLayers.elementAt(idx) instanceof DAggregateLinkLayer) {
			agLayer = (DAggregateLinkLayer) aggLayers.elementAt(idx);
			trLayer = agLayer.getTrajectoryLayer();
		} else {
			pvLayer = (DPlaceVisitsLayer) aggLayers.elementAt(idx);
			trLayer = pvLayer.getTrajectoryLayer();
		}

		if (byTimeCB.getState()) {
			itemizeAggregatesByTimeIntervals((agLayer != null) ? agLayer : pvLayer, trLayer);
		} else if (byClassCB.getState()) {
			DataTable trTable = (DataTable) trLayer.getThematicData();
			if (trTable == null) {
				showMessage("The layer with trajectories has no table with attributes!", true);
				return;
			}
			IntArray aIdxs = new IntArray(20, 10);
			for (int i = 0; i < trTable.getAttrCount(); i++) {
				Attribute at = trTable.getAttribute(i);
				if (at.isClassification() || at.getType() == AttributeTypes.character || at.getType() == AttributeTypes.logical) {
					aIdxs.addElement(i);
				}
			}
			if (aIdxs.size() < 1) {
				showMessage("The table has no columns defining trajectory classes or clusters!", true);
				return;
			}
			list = new List(Math.max(10, Math.min(aIdxs.size(), 3)));
			for (int i = 0; i < aIdxs.size(); i++) {
				list.add(trTable.getAttributeName(aIdxs.elementAt(i)));
			}
			list.select(aIdxs.size() - 1);
			mainP = new Panel(new BorderLayout());
			mainP.add(new Label("Select the table column defining the classes or clusters:"), BorderLayout.NORTH);
			mainP.add(list, BorderLayout.CENTER);
			dia = new OKDialog(core.getUI().getMainFrame(), "Select column with classes", true);
			dia.addContent(mainP);
			dia.show();
			if (dia.wasCancelled())
				return;
			int k = list.getSelectedIndex();
			if (k < 0)
				return;
			int colN = aIdxs.elementAt(k);
			if (agLayer != null) {
				itemizeAggregatesByTrajectoryClasses(agLayer, trLayer, trTable, colN);
			} else {
				itemizeAggregatesByTrajectoryClasses(pvLayer, trLayer, trTable, colN);
			}
		}
	}

	/**
	 * @param agLayer -  must be DAggregateLinkLayer or DPlaceVisitsLayer
	 * @param trLayer - the layer with the trajectories
	 * @param trTable - the table with the data about the trajectories
	 * @param colN - the column containing class labels
	 */
	protected void itemizeAggregatesByTrajectoryClasses(DGeoLayer agLayer, DGeoLayer trLayer, DataTable trTable, int colN) {
		if (agLayer == null || trLayer == null || trTable == null || colN < 0)
			return;
		IntArray iar = new IntArray(1, 1);
		iar.addElement(colN);
		Vector classLabels = trTable.getAllValuesInColumnsAsStrings(iar);
		if (classLabels == null || classLabels.size() < 1) {
			showMessage("No values in the table column with classes!", true);
			return;
		}
		if (classLabels.size() > 1) {
			BubbleSort.sort(classLabels, this);
		}

		int numTr[][] = new int[agLayer.getObjectCount()][classLabels.size()];
		int classSizes[] = new int[classLabels.size()];
		for (int i = 0; i < agLayer.getObjectCount(); i++) {
			for (int j = 0; j < classLabels.size(); j++) {
				numTr[i][j] = 0;
			}
		}
		for (int j = 0; j < classSizes.length; j++) {
			classSizes[j] = 0;
		}

		for (int i = 0; i < trLayer.getObjectCount(); i++) {
			DGeoObject gobj = trLayer.getObject(i);
			if ((gobj instanceof DMovingObject) && gobj.getData() != null) {
				String val = gobj.getData().getAttrValueAsString(colN);
				if (val == null) {
					continue;
				}
				int clIdx = -1;
				for (int j = 0; j < classLabels.size() && clIdx < 0; j++)
					if (val.equals(classLabels.elementAt(j))) {
						clIdx = j;
					}
				if (clIdx < 0) {
					continue;
				}
				++classSizes[clIdx];
				String trId = gobj.getIdentifier();
				for (int j = 0; j < agLayer.getObjectCount(); j++) {
					DGeoObject aObj = agLayer.getObject(j);
					if (aObj instanceof DAggregateLinkObject)
						if (((DAggregateLinkObject) aObj).includesTrajectory(trId)) {
							++numTr[j][clIdx];
						} else {
							;
						}
					else if (aObj instanceof DPlaceVisitsObject)
						if (((DPlaceVisitsObject) aObj).wasVisitedBy(trId)) {
							++numTr[j][clIdx];
						}
				}
			}
		}

		//add columns to the table
		DataTable agTable = (DataTable) agLayer.getThematicData();

		Parameter par = new Parameter();
		for (int i = 0; i < classLabels.size(); i++) {
			par.addValue(classLabels.elementAt(i));
		}
		String parName = "cl";
		par.setName(parName);
		for (int k = 1; agTable.getParameter(par.getName()) != null; k++) {
			par.setName(parName + k);
		}
		agTable.addParameter(par);

		String aName = trTable.getAttributeName(colN) + ", number";
		Attribute attrParentNum = new Attribute(IdMaker.makeId(agTable.getAttrCount() + " " + aName, agTable), AttributeTypes.integer);
		attrParentNum.setName(aName);
		int aIdx0 = agTable.getAttrCount();
		for (int i = 0; i < classLabels.size(); i++) {
			String name = classLabels.elementAt(i).toString();
			Attribute attrChild = new Attribute(attrParentNum.getIdentifier() + "_" + name, attrParentNum.getType());
			attrChild.addParamValPair(par.getName(), name);
			attrParentNum.addChild(attrChild);
			agTable.addAttribute(attrChild);
		}

		aName = trTable.getAttributeName(colN) + ", %";
		Attribute attrParentPerc = new Attribute(IdMaker.makeId(agTable.getAttrCount() + " " + aName, agTable) + "_%", AttributeTypes.real);
		attrParentPerc.setName(aName);
		int aIdx1 = agTable.getAttrCount();
		for (int i = 0; i < classLabels.size(); i++) {
			String name = classLabels.elementAt(i).toString();
			Attribute attrChild = new Attribute(attrParentPerc.getIdentifier() + "_" + name, attrParentPerc.getType());
			attrChild.addParamValPair(par.getName(), name);
			attrParentPerc.addChild(attrChild);
			agTable.addAttribute(attrChild);
		}

		for (int i = 0; i < agLayer.getObjectCount(); i++) {
			DGeoObject gobj = agLayer.getObject(i);
			DataRecord rec = (DataRecord) gobj.getData();
			if (rec == null) {
				continue;
			}
			for (int j = 0; j < classLabels.size(); j++) {
				rec.setNumericAttrValue(numTr[i][j], String.valueOf(numTr[i][j]), aIdx0 + j);
			}
			for (int j = 0; j < classLabels.size(); j++)
				if (classSizes[j] == 0 || numTr[i][j] == 0) {
					rec.setNumericAttrValue(0, "0.00", aIdx1 + j);
				} else {
					float perc = 100.0f * numTr[i][j] / classSizes[j];
					rec.setNumericAttrValue(perc, StringUtil.floatToStr(perc, 2), aIdx1 + j);
				}
		}
		//agTable.finishedDataLoading();
		agTable.makeUniqueAttrIdentifiers();
		showMessage("Finished; " + (2 * classLabels.size()) + " columns have been added to table " + agTable.getName(), false);
	}

	/**
	 * Compares two strings. First checks if the strings represent numbers.
	 * If so, compares the numbers.
	 * Returns 0 if the objects are equal, <0 if the first object is less than the
	 * second one, >0 otherwise
	 */
	public int compare(Object obj1, Object obj2) {
		if (obj1 == null || obj2 == null || !(obj1 instanceof String) || !(obj2 instanceof String))
			return 0;
		String st1 = (String) obj1, st2 = (String) obj2;
		try {
			int i1 = Integer.parseInt(st1);
			int i2 = Integer.parseInt(st2);
			if (i1 < i2)
				return -1;
			if (i1 > i2)
				return 1;
			return 0;
		} catch (NumberFormatException e) {
		}
		return st1.compareTo(st2);
	}

	/**
	 * @param agLayer -  must be DAggregateLinkLayer or DPlaceVisitsLayer
	 * @param trLayer - the layer with the trajectories
	 */
	protected void itemizeAggregatesByTimeIntervals(DGeoLayer agLayer, DGeoLayer trLayer) {
		if (agLayer == null || trLayer == null)
			return;
		DPlaceVisitsLayer pLayer = (agLayer instanceof DPlaceVisitsLayer) ? (DPlaceVisitsLayer) agLayer : null;
		DAggregateLinkLayer lLayer = (agLayer instanceof DAggregateLinkLayer) ? (DAggregateLinkLayer) agLayer : null;
		if (pLayer == null && lLayer == null) {
			showMessage("Invalid type of the aggregate layer: " + agLayer.getClass().getName(), true);
			return;
		}
		SystemUI ui = core.getUI();
		ObjectFilter oFilter = trLayer.getObjectFilter();
		boolean useActive = oFilter != null && oFilter.areObjectsFiltered() && Dialogs.askYesOrNo(ui.getMainFrame(), "Use only active (after filtering) trajectories?", "Account for filter?");
		TimeReference tSpan = trLayer.getOriginalTimeSpan();
		TimeFilter timeFilter = null;
		if (useActive) {
			TimeMoment t1 = tSpan.getValidUntil(), t2 = tSpan.getValidFrom();
			for (int ne = 0; ne < trLayer.getObjectCount(); ne++)
				if (trLayer.isObjectActive(ne)) {
					DGeoObject gobj = trLayer.getObject(ne);
					TimeReference tref = gobj.getTimeReference();
					if (tref == null) {
						continue;
					}
					TimeMoment t = tref.getValidFrom();
					if (t == null) {
						continue;
					}
					if (t1.compareTo(t) > 0) {
						t1 = t;
					}
					t = tref.getValidUntil();
					if (t == null) {
						t = tref.getValidFrom();
					}
					if (t2.compareTo(t) < 0) {
						t2 = t;
					}
				}
			if (t1.compareTo(t2) > 0) {
				showMessage("No active trajectories with valid time references found!", true);
				return;
			}
			timeFilter = trLayer.getTimeFilter();
			if (timeFilter != null && !timeFilter.areObjectsFiltered()) {
				timeFilter = null;
			}
			if (timeFilter != null) {
				TimeMoment tf1 = timeFilter.getEarliestMoment(), tf2 = timeFilter.getLatestMoment();
				if (tf1.compareTo(t1) > 0) {
					tf1.copyTo(t1);
				}
				if (tf2.compareTo(t2) < 0) {
					tf2.copyTo(t2);
				}
			}
			tSpan = new TimeReference();
			tSpan.setValidFrom(t1);
			tSpan.setValidUntil(t2);
		}
		if (tSpan.getValidFrom().compareTo(tSpan.getValidUntil()) >= 0) {
			showMessage("Invalid time span of the data!", true);
			return;
		}
		SetTimeBreaksUI tdUI = new SetTimeBreaksUI(tSpan.getValidFrom(), tSpan.getValidUntil());
		OKDialog dia = new OKDialog(ui.getMainFrame(), "Temporal aggregation", true);
		dia.addContent(tdUI);
		dia.show();
		if (dia.wasCancelled() || tdUI.getTimeBreaks() == null)
			return;

		Vector<TimeMoment> br = tdUI.getTimeBreaks();
		if (br.size() < 1)
			return;
		tSpan.setValidFrom(tdUI.getStart());
		tSpan.setValidUntil(tdUI.getEnd());

		boolean useCycle = tdUI.useCycle();
		char cycleUnit = tdUI.getCycleUnit();
		int nCycleElements = tdUI.getNCycleElements();

		Parameter par = new Parameter();
		String parName = "Time interval (start)";
		if (useCycle) {
			for (int i = 0; i < br.size(); i++) {
				par.addValue(br.elementAt(i));
			}
			parName = tdUI.getCycleName();
		} else {
			boolean dates = br.elementAt(0) instanceof Date;
			char datePrecision = 0;
			String dateScheme = null;
			if (dates) {
				String expl[] = { "Time interval: from " + tSpan.getValidFrom() + " to " + tSpan.getValidUntil() };
				datePrecision = TimeDialogs.askDesiredDatePrecision(expl, (Date) tSpan.getValidFrom());
				dateScheme = ((Date) br.elementAt(0)).scheme;
				if (datePrecision != br.elementAt(0).getPrecision()) {
					dateScheme = Date.removeExtraElementsFromScheme(dateScheme, datePrecision);
				}
				EnterDateSchemeUI enterSch = new EnterDateSchemeUI("Edit, if desired, the date/time scheme (template) for the parameter values. " + " The chosen precision is " + Date.getTextForTimeSymbol(datePrecision) + ". " + expl[0],
						"Date scheme:", dateScheme);
				dia = new OKDialog(ui.getMainFrame(), "Date/time scheme", false);
				dia.addContent(enterSch);
				dia.show();
				dateScheme = enterSch.getScheme();
			}
			par.addValue(tSpan.getValidFrom().getCopy());
			for (int i = 0; i < br.size(); i++) {
				par.addValue(br.elementAt(i));
			}
			if (dates) {
				for (int i = 0; i < par.getValueCount(); i++) {
					((Date) par.getValue(i)).setPrecision(datePrecision);
					((Date) par.getValue(i)).setDateScheme(dateScheme);
				}
			}
		}
		par.setName(parName);

		String itemsName = (agLayer instanceof DAggregateLinkLayer) ? "trajectories" : "visitors";
		String aName = "N " + itemsName + " by " + ((useCycle) ? parName : "time intervals");
		String aNameTotal = "N " + itemsName + " total";
		DataTable areaTable = (DataTable) agLayer.getThematicData();
		if (areaTable != null) {
			String str = aName + " (";
			for (int k = 2; areaTable.findAttrByName(aName) >= 0; k++) {
				aName = str + k + ")";
			}
			str = aNameTotal + " (";
			for (int k = 2; areaTable.findAttrByName(aNameTotal) >= 0; k++) {
				aNameTotal = str + k + ")";
			}
		}

		Panel p = new Panel(new ColumnLayout());
		p.add(new Label("A parameter-dependent attribute will be produced.", Label.CENTER));
		p.add(new Label("Edit the names of the attributes and the parameter if needed.", Label.CENTER));
		p.add(new Label("Name of the parameter-dependent attribute:"));
		TextField atf = new TextField(aName);
		p.add(atf);
		p.add(new Label("Parameter name:"));
		TextField ptf = new TextField(par.getName());
		p.add(ptf);
		p.add(new Label("Parameter values from " + par.getFirstValue() + " to " + par.getLastValue()));
		p.add(new Label("Attribute name for \"total\":"));
		TextField aTottf = new TextField(aNameTotal);
		p.add(aTottf);
		dia = new OKDialog(ui.getMainFrame(), "New attributes", true);
		dia.addContent(p);
		dia.show();
		if (dia.wasCancelled())
			return;
		String str = atf.getText();
		if (str != null && str.trim().length() > 0) {
			aName = str.trim();
		}
		str = aTottf.getText();
		if (str != null && str.trim().length() > 0) {
			aNameTotal = str.trim();
		}
		str = ptf.getText();
		if (str != null && str.trim().length() > 0) {
			parName = str.trim();
			par.setName(parName);
		}

		int nIntervals = br.size();
		if (!useCycle) {
			++nIntervals;
		}
		int nAreas = agLayer.getObjectCount();

		int visitors[][] = new int[nAreas][nIntervals];
		int visits[][] = new int[nAreas][nIntervals];
		int totalVisitors[] = new int[nAreas];
		int totalVisits[] = new int[nAreas];
		for (int i = 0; i < nAreas; i++) {
			totalVisitors[i] = 0;
			totalVisits[i] = 0;
			for (int j = 0; j < nIntervals; j++) {
				visitors[i][j] = 0;
				visits[i][j] = 0;
			}
		}

		int neiVisitors[][] = null;
		int neiVisits[][] = null;
		int neiTotalVisitors[] = null, neiTotalVisits[] = null, nNei[] = null;
		//check if information about neighbours of geo objects is available in the area layer
		boolean hasNeiInfo = false;
		if (pLayer != null) {
			for (int i = 0; i < pLayer.getObjectCount() && !hasNeiInfo; i++) {
				DGeoObject gobj = pLayer.getObject(i);
				hasNeiInfo = gobj.neighbours != null && gobj.neighbours.size() > 0;
			}
		}
		if (hasNeiInfo && Dialogs.askYesOrNo(core.getUI().getMainFrame(), "Compute the number of visitors and visits in the neighbourhood of each area " + "(including the area itself)?", "Account for neighbours?")) {
			neiVisitors = new int[nAreas][nIntervals];
			neiVisits = new int[nAreas][nIntervals];
			neiTotalVisitors = new int[nAreas];
			neiTotalVisits = new int[nAreas];
			nNei = new int[nAreas];
			for (int i = 0; i < nAreas; i++) {
				neiTotalVisitors[i] = 0;
				neiTotalVisits[i] = 0;
				nNei[i] = 0;
				for (int j = 0; j < nIntervals; j++) {
					neiVisitors[i][j] = 0;
					neiVisits[i][j] = 0;
				}
			}
		}
		Vector trIds = null;
		if (useActive) {
			trIds = (pLayer != null) ? pLayer.getActiveTrajIds() : lLayer.getActiveTrajIds();
		}

		//the following array will indicate for each trajectory visiting a place
		//in which time intervals it has been counted, to avoid counting the same
		//trajectory more than once in one interval
		Vector<String> usedTrIds = new Vector<String>(100, 100);
		Vector<boolean[]> countedTimeInts = new Vector<boolean[]>(100, 100);

		for (int na = 0; na < nAreas; na++) {
			DPlaceVisitsObject place = (pLayer != null) ? (DPlaceVisitsObject) pLayer.getObject(na) : null;
			DAggregateLinkObject move = (lLayer != null) ? (DAggregateLinkObject) lLayer.getObject(na) : null;
			int nItems = 0;
			if (place != null && place.visits != null) {
				nItems = place.visits.size();
			}
			if (move != null && move.souLinks != null) {
				nItems = move.souLinks.size();
			}
			//if (nItems<1) continue;
			//Vector<String> usedTrIds=(place!=null && neiCounts!=null)?new Vector<String>(nItems*5,10):null;
			usedTrIds.removeAllElements();
			countedTimeInts.removeAllElements();
			for (int ni = 0; ni < nItems; ni++) {
				String trId = null;
				TimeMoment t1 = null, t2 = null;
				if (place != null) {
					PlaceVisitInfo pvi = (PlaceVisitInfo) place.visits.elementAt(ni);
					trId = pvi.trId;
					t1 = pvi.enterTime;
					t2 = pvi.exitTime;
				} else if (move != null) {
					if (move.souTrajIds != null) {
						trId = (String) move.souTrajIds.elementAt(ni);
					}
					DLinkObject link = (DLinkObject) move.souLinks.elementAt(ni);
					TimeReference tref = link.getSpatialData().getTimeReference();
					if (tref != null) {
						t1 = tref.getValidFrom();
						t2 = tref.getValidUntil();
					}
				}
				if (useActive && trIds != null && trId != null && !trIds.contains(trId)) {
					continue;
				}
				if (t1 == null && t2 == null) {
					continue;
				}
				if (useActive && timeFilter != null && !timeFilter.isActive(t1, t2)) {
					continue;
				}
				++totalVisits[na];
				boolean countedInInterval[] = null;
				int trIdx = usedTrIds.indexOf(trId);
				if (trIdx >= 0) {
					countedInInterval = countedTimeInts.elementAt(trIdx);
				} else {
					countedInInterval = new boolean[nIntervals];
					for (int i = 0; i < nIntervals; i++) {
						countedInInterval[i] = false;
					}
					usedTrIds.addElement(trId);
					countedTimeInts.addElement(countedInInterval);
					++totalVisitors[na];
				}
				int tIdx[] = TimeUtil.getIndexesOfTimeIntervals(t1, t2, br, nIntervals, useCycle, cycleUnit, nCycleElements);
				if (tIdx == null) {
					continue;
				}
				if (tIdx[0] <= tIdx[1]) {
					for (int i = tIdx[0]; i <= tIdx[1]; i++) {
						++visits[na][i];
						if (!countedInInterval[i]) {
							++visitors[na][i];
							countedInInterval[i] = true;
						}
					}
				} else {
					for (int i = tIdx[0]; i < nIntervals; i++) {
						++visits[na][i];
						if (!countedInInterval[i]) {
							++visitors[na][i];
							countedInInterval[i] = true;
						}
					}
					for (int i = 0; i <= tIdx[1]; i++) {
						++visits[na][i];
						if (!countedInInterval[i]) {
							++visitors[na][i];
							countedInInterval[i] = true;
						}
					}
				}
			}
			if (place != null && neiVisitors != null) {
				neiTotalVisitors[na] = totalVisitors[na];
				neiTotalVisits[na] = totalVisits[na];
				for (int i = 0; i < nIntervals; i++) {
					neiVisitors[na][i] = visitors[na][i];
					neiVisits[na][i] = visits[na][i];
				}
				if (place.neighbours == null || place.neighbours.size() < 1) {
					continue;
				}
				for (int nn = 0; nn < place.neighbours.size(); nn++) {
					int oIdx = pLayer.getObjectIndex(place.neighbours.elementAt(nn));
					if (oIdx >= 0) {
						++nNei[na];
						DPlaceVisitsObject place1 = (DPlaceVisitsObject) pLayer.getObject(oIdx);
						if (place1.visits == null || place1.visits.size() < 1) {
							continue;
						}
						for (int ni = 0; ni < place1.visits.size(); ni++) {
							PlaceVisitInfo pvi = (PlaceVisitInfo) place1.visits.elementAt(ni);
							if (useActive && trIds != null && !trIds.contains(pvi.trId)) {
								continue;
							}
							TimeMoment t1 = pvi.enterTime, t2 = pvi.exitTime;
							if (t1 == null && t2 == null) {
								continue;
							}
							if (useActive && timeFilter != null && !timeFilter.isActive(t1, t2)) {
								continue;
							}
							int tIdx[] = TimeUtil.getIndexesOfTimeIntervals(t1, t2, br, nIntervals, useCycle, cycleUnit, nCycleElements);
							if (tIdx == null) {
								continue;
							}
							boolean countedInInterval[] = null;
							int trIdx = usedTrIds.indexOf(pvi.trId);
							if (trIdx >= 0) {
								countedInInterval = countedTimeInts.elementAt(trIdx);
							} else {
								countedInInterval = new boolean[nIntervals];
								for (int i = 0; i < nIntervals; i++) {
									countedInInterval[i] = false;
								}
								usedTrIds.addElement(pvi.trId);
								countedTimeInts.addElement(countedInInterval);
								++neiTotalVisitors[na];
							}
							if (tIdx[0] <= tIdx[1]) {
								for (int i = tIdx[0]; i <= tIdx[1]; i++) {
									++neiVisits[na][i];
									if (!countedInInterval[i]) {
										++neiVisitors[na][i];
										countedInInterval[i] = true;
									}
								}
							} else {
								for (int i = tIdx[0]; i < nIntervals; i++) {
									++neiVisits[na][i];
									if (!countedInInterval[i]) {
										++neiVisitors[na][i];
										countedInInterval[i] = true;
									}
								}
								for (int i = 0; i <= tIdx[1]; i++) {
									++neiVisits[na][i];
									if (!countedInInterval[i]) {
										++neiVisitors[na][i];
										countedInInterval[i] = true;
									}
								}
							}
						}
					}
				}
			}
		}
		//add columns to the table
		for (int k = 1; areaTable.getParameter(par.getName()) != null; k++) {
			par.setName(parName + k);
		}
		par.setTemporal(!areaTable.hasTemporalParameter());
		areaTable.addParameter(par);

		Attribute attrParentNum = new Attribute(IdMaker.makeId(areaTable.getAttrCount() + " " + aName, areaTable), AttributeTypes.integer);
		attrParentNum.setName(aName);
		int aIdx0 = areaTable.getAttrCount();
		for (int i = 0; i < par.getValueCount(); i++) {
			String name = par.getValue(i).toString();
			Attribute attrChild = new Attribute(attrParentNum.getIdentifier() + "_" + name, attrParentNum.getType());
			attrChild.addParamValPair(par.getName(), par.getValue(i));
			attrParentNum.addChild(attrChild);
			areaTable.addAttribute(attrChild);
		}
		Attribute attrTotal = new Attribute(IdMaker.makeId(areaTable.getAttrCount() + " " + aNameTotal, areaTable), AttributeTypes.integer);
		attrTotal.setName(aNameTotal);
		int totIdx = areaTable.getAttrCount();
		areaTable.addAttribute(attrTotal);
		for (int i = 0; i < nAreas; i++) {
			DGeoObject gobj = agLayer.getObject(i);
			DataRecord rec = (DataRecord) gobj.getData();
			if (rec == null) {
				rec = new DataRecord(gobj.getIdentifier(), gobj.getName());
				areaTable.addDataRecord(rec);
				gobj.setThematicData(rec);
			}
			for (int j = 0; j < nIntervals; j++) {
				rec.setNumericAttrValue(visitors[i][j], String.valueOf(visitors[i][j]), aIdx0 + j);
			}
			rec.setNumericAttrValue(totalVisitors[i], String.valueOf(totalVisitors[i]), totIdx);
		}
		showMessage((areaTable.getAttrCount() - aIdx0) + " columns have been added to table " + areaTable.getName(), false);
		//areaTable.finishedDataLoading();
		areaTable.makeUniqueAttrIdentifiers();
		if (neiVisitors != null) {
			String aN = aName, aNT = aNameTotal;
			aName += ", including neighbourhood";
			aNameTotal += ", including neighbourhood";
			p = new Panel(new ColumnLayout());
			p.add(new Label("Additional attributes will be produced.", Label.CENTER));
			p.add(new Label("Edit the names of the attributes if needed.", Label.CENTER));
			p.add(new Label("Name of the parameter-dependent attribute:"));
			atf = new TextField(aName);
			p.add(atf);
			p.add(new Label("Attribute name for \"total\":"));
			aTottf = new TextField(aNameTotal);
			p.add(aTottf);
			dia = new OKDialog(ui.getMainFrame(), "Additional attributes", false);
			dia.addContent(p);
			dia.show();
			str = atf.getText();
			if (str != null && str.trim().length() > 0) {
				aName = str.trim();
			}
			str = aTottf.getText();
			if (str != null && str.trim().length() > 0) {
				aNameTotal = str.trim();
			}

			attrParentNum = new Attribute(IdMaker.makeId(areaTable.getAttrCount() + " " + aName, areaTable) + "_nei", AttributeTypes.integer);
			attrParentNum.setName(aName);
			int aIdx1 = areaTable.getAttrCount();
			for (int i = 0; i < par.getValueCount(); i++) {
				String name = par.getValue(i).toString();
				Attribute attrChild = new Attribute(IdMaker.makeId(attrParentNum.getIdentifier() + "_" + name, areaTable), attrParentNum.getType());
				attrChild.addParamValPair(par.getName(), par.getValue(i));
				attrParentNum.addChild(attrChild);
				areaTable.addAttribute(attrChild);
			}
			attrTotal = new Attribute(IdMaker.makeId(areaTable.getAttrCount() + " " + aNameTotal, areaTable), AttributeTypes.integer);
			attrTotal.setName(aNameTotal);
			totIdx = areaTable.getAttrCount();
			areaTable.addAttribute(attrTotal);
			for (int i = 0; i < nAreas; i++) {
				DGeoObject gobj = pLayer.getObject(i);
				DataRecord rec = (DataRecord) gobj.getData();
				for (int j = 0; j < nIntervals; j++) {
					rec.setNumericAttrValue(neiVisitors[i][j], String.valueOf(neiVisitors[i][j]), aIdx1 + j);
				}
				rec.setNumericAttrValue(neiTotalVisitors[i], String.valueOf(neiTotalVisitors[i]), totIdx);
			}
			//areaTable.finishedDataLoading();
			areaTable.makeUniqueAttrIdentifiers();

			if (Dialogs.askYesOrNo(core.getUI().getMainFrame(), "Compute the average number of visitors per area in each neighbourhood?", "Average number of visitors?")) {
				aName = "Average " + aN + ", including neighbourhood";
				aNameTotal = "Average " + aNT + ", including neighbourhood";
				atf.setText(aName);
				aTottf.setText(aNameTotal);
				dia = new OKDialog(ui.getMainFrame(), "Additional attribute", false);
				dia.addContent(p);
				dia.show();
				str = atf.getText();
				if (str != null && str.trim().length() > 0) {
					aName = str.trim();
				}
				str = aTottf.getText();
				if (str != null && str.trim().length() > 0) {
					aNameTotal = str.trim();
				}

				attrParentNum = new Attribute(IdMaker.makeId(areaTable.getAttrCount() + " " + aName, areaTable), AttributeTypes.real);
				attrParentNum.setName(aName);
				aIdx1 = areaTable.getAttrCount();
				for (int i = 0; i < par.getValueCount(); i++) {
					String name = par.getValue(i).toString();
					Attribute attrChild = new Attribute(IdMaker.makeId(attrParentNum.getIdentifier() + "_" + name, areaTable), attrParentNum.getType());
					attrChild.addParamValPair(par.getName(), par.getValue(i));
					attrParentNum.addChild(attrChild);
					areaTable.addAttribute(attrChild);
				}
				attrTotal = new Attribute(IdMaker.makeId(areaTable.getAttrCount() + " " + aNameTotal, areaTable), AttributeTypes.real);
				attrTotal.setName(aNameTotal);
				totIdx = areaTable.getAttrCount();
				areaTable.addAttribute(attrTotal);
				for (int i = 0; i < nAreas; i++) {
					DGeoObject gobj = pLayer.getObject(i);
					DataRecord rec = (DataRecord) gobj.getData();
					int nNb = 1 + nNei[i];
					for (int j = 0; j < nIntervals; j++) {
						float val = 1.0f * neiVisitors[i][j] / nNb;
						rec.setNumericAttrValue(val, StringUtil.floatToStr(val, 2), aIdx1 + j);
					}
					float val = 1.0f * neiTotalVisitors[i] / nNb;
					rec.setNumericAttrValue(val, StringUtil.floatToStr(val, 2), totIdx);
				}
				//areaTable.finishedDataLoading();
				areaTable.makeUniqueAttrIdentifiers();
			}
		}

		itemsName = (agLayer instanceof DAggregateLinkLayer) ? "moves" : "visits";
		aName = "N " + itemsName + " by " + ((useCycle) ? parName : "time intervals");
		aNameTotal = "N " + itemsName + " total";
		str = aName + " (";
		for (int k = 2; areaTable.findAttrByName(aName) >= 0; k++) {
			aName = str + k + ")";
		}
		str = aNameTotal + " (";
		for (int k = 2; areaTable.findAttrByName(aNameTotal) >= 0; k++) {
			aNameTotal = str + k + ")";
		}
		p = new Panel(new ColumnLayout());
		p.add(new Label("Additional attributes will be produced.", Label.CENTER));
		p.add(new Label("Edit the names of the attributes if needed.", Label.CENTER));
		p.add(new Label("Name of the parameter-dependent attribute:"));
		atf = new TextField(aName);
		p.add(atf);
		p.add(new Label("Attribute name for \"total\":"));
		aTottf = new TextField(aNameTotal);
		p.add(aTottf);
		dia = new OKDialog(core.getUI().getMainFrame(), "Additional attributes", true);
		dia.addContent(p);
		dia.show();
		str = atf.getText();
		if (str != null && str.trim().length() > 0) {
			aName = str.trim();
		}
		str = aTottf.getText();
		if (str != null && str.trim().length() > 0) {
			aNameTotal = str.trim();
		}
		attrParentNum = new Attribute(IdMaker.makeId(areaTable.getAttrCount() + " " + aName, areaTable) + "_nei", AttributeTypes.integer);
		attrParentNum.setName(aName);
		int aIdx1 = areaTable.getAttrCount();
		for (int i = 0; i < par.getValueCount(); i++) {
			String name = par.getValue(i).toString();
			Attribute attrChild = new Attribute(IdMaker.makeId(attrParentNum.getIdentifier() + "_" + name, areaTable), attrParentNum.getType());
			attrChild.addParamValPair(par.getName(), par.getValue(i));
			attrParentNum.addChild(attrChild);
			areaTable.addAttribute(attrChild);
		}
		attrTotal = new Attribute(IdMaker.makeId(areaTable.getAttrCount() + " " + aNameTotal, areaTable), AttributeTypes.integer);
		attrTotal.setName(aNameTotal);
		totIdx = areaTable.getAttrCount();
		areaTable.addAttribute(attrTotal);
		for (int i = 0; i < nAreas; i++) {
			DGeoObject gobj = agLayer.getObject(i);
			DataRecord rec = (DataRecord) gobj.getData();
			if (rec == null) {
				rec = new DataRecord(gobj.getIdentifier(), gobj.getName());
				areaTable.addDataRecord(rec);
				gobj.setThematicData(rec);
			}
			for (int j = 0; j < nIntervals; j++) {
				rec.setNumericAttrValue(visits[i][j], String.valueOf(visits[i][j]), aIdx1 + j);
			}
			rec.setNumericAttrValue(totalVisits[i], String.valueOf(totalVisits[i]), totIdx);
		}
		showMessage((areaTable.getAttrCount() - aIdx0) + " columns have been added to table " + areaTable.getName(), false);
		areaTable.makeUniqueAttrIdentifiers();

		if (neiVisits != null) {
			String aN = aName, aNT = aNameTotal;
			aName += ", including neighbourhood";
			aNameTotal += ", including neighbourhood";
			atf.setText(aName);
			aTottf.setText(aNameTotal);
			dia = new OKDialog(ui.getMainFrame(), "Additional attributes", false);
			dia.addContent(p);
			dia.show();
			str = atf.getText();
			if (str != null && str.trim().length() > 0) {
				aName = str.trim();
			}
			str = aTottf.getText();
			if (str != null && str.trim().length() > 0) {
				aNameTotal = str.trim();
			}

			attrParentNum = new Attribute(IdMaker.makeId(areaTable.getAttrCount() + " " + aName, areaTable) + "_nei", AttributeTypes.integer);
			attrParentNum.setName(aName);
			aIdx1 = areaTable.getAttrCount();
			for (int i = 0; i < par.getValueCount(); i++) {
				String name = par.getValue(i).toString();
				Attribute attrChild = new Attribute(IdMaker.makeId(attrParentNum.getIdentifier() + "_" + name, areaTable), attrParentNum.getType());
				attrChild.addParamValPair(par.getName(), par.getValue(i));
				attrParentNum.addChild(attrChild);
				areaTable.addAttribute(attrChild);
			}
			attrTotal = new Attribute(IdMaker.makeId(areaTable.getAttrCount() + " " + aNameTotal, areaTable), AttributeTypes.integer);
			attrTotal.setName(aNameTotal);
			totIdx = areaTable.getAttrCount();
			areaTable.addAttribute(attrTotal);
			for (int i = 0; i < nAreas; i++) {
				DGeoObject gobj = pLayer.getObject(i);
				DataRecord rec = (DataRecord) gobj.getData();
				for (int j = 0; j < nIntervals; j++) {
					rec.setNumericAttrValue(neiVisits[i][j], String.valueOf(neiVisits[i][j]), aIdx1 + j);
				}
				rec.setNumericAttrValue(neiTotalVisits[i], String.valueOf(neiTotalVisits[i]), totIdx);
			}
			//areaTable.finishedDataLoading();
			areaTable.makeUniqueAttrIdentifiers();

			if (Dialogs.askYesOrNo(core.getUI().getMainFrame(), "Compute the average number of visits per area in each neighbourhood?", "Average number of visits?")) {
				aName = "Average " + aN + ", including neighbourhood";
				aNameTotal = "Average " + aNT + ", including neighbourhood";
				atf.setText(aName);
				aTottf.setText(aNameTotal);
				dia = new OKDialog(ui.getMainFrame(), "Additional attribute", false);
				dia.addContent(p);
				dia.show();
				str = atf.getText();
				if (str != null && str.trim().length() > 0) {
					aName = str.trim();
				}
				str = aTottf.getText();
				if (str != null && str.trim().length() > 0) {
					aNameTotal = str.trim();
				}

				attrParentNum = new Attribute(IdMaker.makeId(areaTable.getAttrCount() + " " + aName, areaTable), AttributeTypes.real);
				attrParentNum.setName(aName);
				aIdx1 = areaTable.getAttrCount();
				for (int i = 0; i < par.getValueCount(); i++) {
					String name = par.getValue(i).toString();
					Attribute attrChild = new Attribute(IdMaker.makeId(attrParentNum.getIdentifier() + "_" + name, areaTable), attrParentNum.getType());
					attrChild.addParamValPair(par.getName(), par.getValue(i));
					attrParentNum.addChild(attrChild);
					areaTable.addAttribute(attrChild);
				}
				attrTotal = new Attribute(IdMaker.makeId(areaTable.getAttrCount() + " " + aNameTotal, areaTable), AttributeTypes.real);
				attrTotal.setName(aNameTotal);
				totIdx = areaTable.getAttrCount();
				areaTable.addAttribute(attrTotal);
				for (int i = 0; i < nAreas; i++) {
					DGeoObject gobj = pLayer.getObject(i);
					DataRecord rec = (DataRecord) gobj.getData();
					int nNb = 1 + nNei[i];
					for (int j = 0; j < nIntervals; j++) {
						float val = 1.0f * neiVisits[i][j] / nNb;
						rec.setNumericAttrValue(val, StringUtil.floatToStr(val, 2), aIdx1 + j);
					}
					float val = 1.0f * neiTotalVisits[i] / nNb;
					rec.setNumericAttrValue(val, StringUtil.floatToStr(val, 2), totIdx);
				}
				//areaTable.finishedDataLoading();
				areaTable.makeUniqueAttrIdentifiers();
			}
		}
		showMessage("Finished; " + (areaTable.getAttrCount() - aIdx0) + " columns have been added to table " + areaTable.getName(), false);
	}
}
