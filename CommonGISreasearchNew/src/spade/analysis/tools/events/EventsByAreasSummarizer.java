package spade.analysis.tools.events;

import java.awt.Checkbox;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.util.Vector;

import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.analysis.tools.BaseAnalyser;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.IdMaker;
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
import spade.vis.database.Parameter;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DVectorGridLayer;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Aug 4, 2009
 * Time: 11:30:30 AM
 * Summarizes events by areas: counts the number of events in each area,
 * possibly, by time intervals.
 */
public class EventsByAreasSummarizer extends BaseAnalyser {
	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	@Override
	public void run(ESDACore core) {
		if (core == null || core.getUI() == null)
			return;
		this.core = core;
		SystemUI ui = core.getUI();
		if (ui.getCurrentMapViewer() == null || ui.getCurrentMapViewer().getLayerManager() == null) {
			showMessage("No map exists!", true);
			return;
		}
		LayerManager lman = ui.getCurrentMapViewer().getLayerManager();
		Vector<DGeoLayer> evLayers = new Vector<DGeoLayer>(lman.getLayerCount(), 1);
		Vector<DGeoLayer> areaLayers = new Vector<DGeoLayer>(lman.getLayerCount(), 1);
		for (int i = 0; i < lman.getLayerCount(); i++) {
			GeoLayer layer = lman.getGeoLayer(i);
			if (layer.getObjectCount() < 1) {
				continue;
			}
			if (!(layer instanceof DGeoLayer)) {
				continue;
			}
			if (layer.getType() != Geometry.point && layer.getType() != Geometry.area) {
				continue;
			}
			if (layer.hasTimeReferences()) {
				evLayers.addElement((DGeoLayer) layer);
			} else if (layer.getType() == Geometry.area) {
				areaLayers.addElement((DGeoLayer) layer);
			}
		}
		if (evLayers.size() < 1) {
			showMessage("No layers with events found!", true);
			return;
		}
		if (areaLayers.size() < 1) {
			showMessage("No layers with areas found!", true);
			return;
		}
		Panel mainP = new Panel(new ColumnLayout());
		mainP.add(new Label("Select the layer with events to summarise:"));
		List mList = new List(Math.max(evLayers.size() + 1, 5));
		for (int i = 0; i < evLayers.size(); i++) {
			mList.add(evLayers.elementAt(i).getName());
		}
		mList.select(mList.getItemCount() - 1);
		mainP.add(mList);
		mainP.add(new Label("Select the layer with areas:"));
		List aList = new List(Math.max(areaLayers.size() + 1, 5));
		for (int i = 0; i < areaLayers.size(); i++) {
			aList.add(areaLayers.elementAt(i).getName());
		}
		aList.select(aList.getItemCount() - 1);
		mainP.add(aList);
		Checkbox cbActive = new Checkbox("use only active (after filtering) events", false);
		mainP.add(cbActive);

		OKDialog dia = new OKDialog(ui.getMainFrame(), "Summarise events by areas", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return;
		int idx = mList.getSelectedIndex();
		if (idx < 0)
			return;
		DGeoLayer evLayer = evLayers.elementAt(idx);
		idx = aList.getSelectedIndex();
		if (idx < 0)
			return;
		DGeoLayer areaLayer = areaLayers.elementAt(idx);
		boolean useActive = cbActive.getState();

		TimeReference tSpan = evLayer.getOriginalTimeSpan();
		if (useActive) {
			TimeMoment t1 = tSpan.getValidUntil(), t2 = tSpan.getValidFrom();
			for (int ne = 0; ne < evLayer.getObjectCount(); ne++)
				if (evLayer.isObjectActive(ne)) {
					DGeoObject gobj = evLayer.getObject(ne);
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
				showMessage("No active events with valid time references found!", true);
				return;
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
		dia = new OKDialog(ui.getMainFrame(), "Temporal aggregation", true);
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
		String aName = "N events by " + ((useCycle) ? parName : "time intervals");
		String aNameTotal = "N events total";
		DataTable areaTable = (DataTable) areaLayer.getThematicData();
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
		if (!useCycle || br.size() < nCycleElements) {
			++nIntervals;
		}
		int nAreas = areaLayer.getObjectCount();

		int counts[][] = new int[nAreas][nIntervals];
		int totals[] = new int[nAreas];
		for (int i = 0; i < nAreas; i++) {
			totals[i] = 0;
			for (int j = 0; j < nIntervals; j++) {
				counts[i][j] = 0;
			}
		}

		for (int ne = 0; ne < evLayer.getObjectCount(); ne++)
			if (!useActive || evLayer.isObjectActive(ne)) {
				DGeoObject gobj = evLayer.getObject(ne);
				if (gobj.getSpatialData() == null) {
					continue;
				}
				RealPoint pt = ((SpatialEntity) gobj.getSpatialData()).getCentre();
				if (pt == null) {
					continue;
				}
				TimeReference tref = gobj.getTimeReference();
				if (tref == null || tref.getValidFrom() == null) {
					continue;
				}
				int aIdx = -1;
				for (int na = 0; na < nAreas && aIdx < 0; na++) {
					DGeoObject area = areaLayer.getObject(na);
					if (area.contains(pt.x, pt.y, 0f)) {
						aIdx = na;
					}
				}
				if (aIdx < 0) {
					continue;
				}
				++totals[aIdx];
				TimeMoment t1 = tref.getValidFrom(), t2 = tref.getValidUntil();
				int tIdx[] = TimeUtil.getIndexesOfTimeIntervals(t1, t2, br, nIntervals, useCycle, cycleUnit, nCycleElements);
				if (tIdx == null) {
					continue;
				}
				if (tIdx[0] <= tIdx[1]) {
					for (int i = tIdx[0]; i <= tIdx[1]; i++) {
						++counts[aIdx][i];
					}
				} else {
					for (int i = tIdx[0]; i < nIntervals; i++) {
						++counts[aIdx][i];
					}
					for (int i = 0; i <= tIdx[1]; i++) {
						++counts[aIdx][i];
					}
				}
			}

		//add columns to the table
		boolean makeNewTable = areaTable == null;
		if (makeNewTable) {
			String name = areaLayer.getName();
/*
      name= Dialogs.askForStringValue(core.getUI().getMainFrame(),"Name of the table?",name,
        "A table with statistics about the events will be generated and attached " +
          "to the layer \""+name+"\"","New table",false);
*/
			areaTable = new DataTable();
			areaTable.setName(name);
			areaTable.setEntitySetIdentifier(areaLayer.getEntitySetIdentifier());
		}

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
			DGeoObject gobj = areaLayer.getObject(i);
			DataRecord rec = (DataRecord) gobj.getData();
			if (rec == null) {
				rec = new DataRecord(gobj.getIdentifier(), gobj.getName());
				areaTable.addDataRecord(rec);
				gobj.setThematicData(rec);
			}
			for (int j = 0; j < nIntervals; j++) {
				rec.setNumericAttrValue(counts[i][j], String.valueOf(counts[i][j]), aIdx0 + j);
			}
			rec.setNumericAttrValue(totals[i], String.valueOf(totals[i]), totIdx);
		}
		if (makeNewTable) {
			DataLoader dLoader = core.getDataLoader();
			dLoader.setLink(areaLayer, dLoader.addTable(areaTable));
			areaLayer.setThematicFilter(areaTable.getObjectFilter());
			areaLayer.setLinkedToTable(true);
			showMessage("Table " + areaTable.getName() + " has been attached to layer " + areaLayer.getName(), false);
		} else {
			showMessage((areaTable.getAttrCount() - aIdx0) + " columns have been added to table " + areaTable.getName(), false);
		}
		//areaTable.finishedDataLoading();
		areaTable.makeUniqueAttrIdentifiers();

		//check if information about neighbours of geo objects is available in the area layer
		boolean isGrid = areaLayer instanceof DVectorGridLayer;
		boolean hasNeiInfo = false;
		if (!isGrid) {
			for (int i = 0; i < areaLayer.getObjectCount() && !hasNeiInfo; i++) {
				DGeoObject gobj = areaLayer.getObject(i);
				hasNeiInfo = gobj.neighbours != null && gobj.neighbours.size() > 0;
			}
		}
		if ((!isGrid && !hasNeiInfo) || !Dialogs.askYesOrNo(core.getUI().getMainFrame(), "Compute the number of events in the neighbourhood of each area (including the area itself)?", "Account for neighbours?")) {
			if (!makeNewTable) {
				showMessage("Finished; " + (areaTable.getAttrCount() - aIdx0) + " columns have been added to table " + areaTable.getName(), false);
			}
			return;
		}

		int neiCounts[][] = new int[nAreas][nIntervals];
		int neiTotals[] = new int[nAreas];
		int nNei[] = new int[nAreas];
		for (int i = 0; i < nAreas; i++) {
			neiTotals[i] = totals[i];
			nNei[i] = 0;
			for (int j = 0; j < nIntervals; j++) {
				neiCounts[i][j] = counts[i][j];
			}
		}
		DVectorGridLayer grid = (isGrid) ? (DVectorGridLayer) areaLayer : null;
		for (int na = 0; na < nAreas; na++) {
			DGeoObject area = areaLayer.getObject(na);
			if (isGrid) {
				int pos[] = grid.getRowAndColumn(na);
				if (pos == null) {
					continue;
				}
				int r1 = pos[0] - 1, r2 = pos[0] + 1, c1 = pos[1] - 1, c2 = pos[1] + 1;
				if (r1 < 0) {
					r1 = 0;
				}
				if (r2 >= grid.getNRows()) {
					r2 = grid.getNRows() - 1;
				}
				if (c1 < 0) {
					c1 = 0;
				}
				if (c2 >= grid.getNCols()) {
					c2 = grid.getNCols() - 1;
				}
				for (int r = r1; r <= r2; r++) {
					for (int c = c1; c <= c2; c++)
						if (r != pos[0] || c != pos[1]) {
							int oIdx = grid.getObjectIndex(r, c);
							if (oIdx >= 0) {
								++nNei[na];
								neiTotals[na] += totals[oIdx];
								for (int j = 0; j < nIntervals; j++) {
									neiCounts[na][j] += counts[oIdx][j];
								}
							}
						}
				}
			} else {
				if (area.neighbours == null || area.neighbours.size() < 1) {
					continue;
				}
				for (int i = 0; i < area.neighbours.size(); i++) {
					int oIdx = areaLayer.getObjectIndex(area.neighbours.elementAt(i));
					if (oIdx >= 0) {
						++nNei[na];
						neiTotals[na] += totals[oIdx];
						for (int j = 0; j < nIntervals; j++) {
							neiCounts[na][j] += counts[oIdx][j];
						}
					}
				}
			}
		}
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
		dia = new OKDialog(ui.getMainFrame(), "Additional attributes", true);
		dia.addContent(p);
		dia.show();
		if (dia.wasCancelled())
			return;
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
			DGeoObject gobj = areaLayer.getObject(i);
			DataRecord rec = (DataRecord) gobj.getData();
			for (int j = 0; j < nIntervals; j++) {
				rec.setNumericAttrValue(neiCounts[i][j], String.valueOf(neiCounts[i][j]), aIdx1 + j);
			}
			rec.setNumericAttrValue(neiTotals[i], String.valueOf(neiTotals[i]), totIdx);
		}
		//areaTable.finishedDataLoading();
		areaTable.makeUniqueAttrIdentifiers();

		if (!Dialogs.askYesOrNo(core.getUI().getMainFrame(), "Compute the average number of events per area in each neighbourhood?", "Average number of events?")) {
			showMessage("Finished; " + (areaTable.getAttrCount() - aIdx0) + " columns have been added to table " + areaTable.getName(), false);
			return;
		}

		aName = "Average " + aN + ", including neighbourhood";
		aNameTotal = "Average " + aNT + ", including neighbourhood";
		p = new Panel(new ColumnLayout());
		p.add(new Label("Additional attributes will be produced.", Label.CENTER));
		p.add(new Label("Edit the names of the attributes if needed.", Label.CENTER));
		p.add(new Label("Name of the parameter-dependent attribute:"));
		atf = new TextField(aName);
		p.add(atf);
		p.add(new Label("Attribute name for \"total\":"));
		aTottf = new TextField(aNameTotal);
		p.add(aTottf);
		dia = new OKDialog(ui.getMainFrame(), "Additional attribute", true);
		dia.addContent(p);
		dia.show();
		if (dia.wasCancelled())
			return;
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
			DGeoObject gobj = areaLayer.getObject(i);
			DataRecord rec = (DataRecord) gobj.getData();
			int nNb = 1 + nNei[i];
			for (int j = 0; j < nIntervals; j++) {
				float val = 1.0f * neiCounts[i][j] / nNb;
				rec.setNumericAttrValue(val, StringUtil.floatToStr(val, 2), aIdx1 + j);
			}
			float val = 1.0f * neiTotals[i] / nNb;
			rec.setNumericAttrValue(val, StringUtil.floatToStr(val, 2), totIdx);
		}
		//areaTable.finishedDataLoading();
		areaTable.makeUniqueAttrIdentifiers();

		showMessage("Finished; " + (areaTable.getAttrCount() - aIdx0) + " columns have been added to table " + areaTable.getName(), false);
	}
}
