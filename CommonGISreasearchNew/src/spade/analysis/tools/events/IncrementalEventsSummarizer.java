package spade.analysis.tools.events;

import java.awt.Checkbox;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.util.Vector;

import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.IdMaker;
import spade.time.Date;
import spade.time.TimeMoment;
import spade.time.ui.EnterDateSchemeUI;
import spade.time.ui.TimeDialogs;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.Parameter;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.geometry.Geometry;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import core.ActionDescr;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jan 4, 2010
 * Time: 6:09:16 PM
 * Uses a given territory division (e.g. Voronoi cells) to summarise incrementally
 * events, e.g. coming from a database. The events are added one by one.
 */
public class IncrementalEventsSummarizer {
	/**
	 * provides access to the status bar, main window, data loader, etc.
	 */
	protected ESDACore core = null;
	/**
	 * The name of the source DB table or layer with the events
	 */
	public String origSetName = null;
	/**
	 * Actually summarises the trajectories
	 */
	protected EventsAccumulator acc = null;
	/**
	 * The description of the operation
	 */
	public ActionDescr aDescr = null;

	/**
	 * @param core - provides access to the status bar, main window, data loader, etc.
	 * @param origSetName - the name of the table or layer with the original data
	 *   (used for the generation of the names of the resulting layer and table)
	 */
	public boolean init(ESDACore core, String origSetName) {
		this.core = core;
		this.origSetName = origSetName;
		if (core.getUI().getCurrentMapViewer() == null || core.getUI().getCurrentMapViewer().getLayerManager() == null) {
			core.getUI().showMessage("No map exists!", true);
			return false;
		}
		//Find instances of DGeoLayer containing areas
		LayerManager lman = core.getUI().getCurrentMapViewer().getLayerManager();
		Vector areaLayers = new Vector(lman.getLayerCount(), 1);
		for (int i = 0; i < lman.getLayerCount(); i++) {
			GeoLayer layer = lman.getGeoLayer(i);
			if ((layer instanceof DGeoLayer) && layer.getObjectCount() > 0)
				if (layer.getType() == Geometry.area && layer.getObjectCount() > 1) {
					areaLayers.addElement(layer);
				}
		}
		if (areaLayers.size() < 1) {
			core.getUI().showMessage("No layers with areas found!", true);
			return false;
		}
		Panel mainP = new Panel(new ColumnLayout());
		mainP.add(new Label("Select the layer with areas:"));
		List aList = new List(Math.max(areaLayers.size() + 1, 5));
		for (int i = 0; i < areaLayers.size(); i++) {
			aList.add(((DGeoLayer) areaLayers.elementAt(i)).getName());
		}
		aList.select(aList.getItemCount() - 1);
		mainP.add(aList);
		Checkbox cbNei = new Checkbox("count also events in neighbouring areas, if possible", false);
		mainP.add(cbNei);
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Summarise events", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return false;
		int idx = aList.getSelectedIndex();
		if (idx < 0)
			return false;
		DGeoLayer areaLayer = (DGeoLayer) areaLayers.elementAt(idx);
		if (areaLayer == null || areaLayer.getObjectCount() < 1) {
			core.getUI().showMessage("No areas found!", true);
			return false;
		}

		aDescr = new ActionDescr();
		aDescr.aName = "Summarisation of events from " + origSetName + " by " + areaLayer.getName();
		aDescr.startTime = System.currentTimeMillis();
		aDescr.addParamValue("Table with events", origSetName);
		aDescr.addParamValue("Layer with areas", areaLayer.getName());
		core.logAction(aDescr);

		acc = new EventsAccumulator();
		acc.setAreas(areaLayer);
		acc.setCountEventsAround(cbNei.getState());

		aDescr.endTime = System.currentTimeMillis();
		return true;
	}

	/**
	 * @param start - start of the whole time span (what is before must be ignored)
	 * @param end - end of the whole time span (what is after must be ignored)
	 * @param breaks - the breaks that divide the time span into intervals
	 * @param useCycle - whether the division is done according to the cyclical time model
	 * @param cycleUnit - units of the cycle
	 * @param nCycleElements - length of the cycle
	 * @param cycleName - name of the cycle
	 */
	public void setTemporalAggregationParameters(TimeMoment start, TimeMoment end, Vector<TimeMoment> breaks, boolean useCycle, char cycleUnit, int nCycleElements, String cycleName) {
		acc.setTemporalAggregationParameters(start, end, breaks, useCycle, cycleUnit, nCycleElements, cycleName);
	}

	/**
	 * Accumulates the event with the given x- and y-coordinates and time reference (t1..t2)
	 */
	public void accumulateEvent(float x, float y, TimeMoment t1, TimeMoment t2) {
		if (acc != null) {
			acc.accumulateEvent(x, y, t1, t2);
		}
		return;
	}

	/**
	 * Puts the counts in the table associated with the areas
	 */
	public void putCountsInTable() {
		if (acc == null || acc.counts == null)
			return;
		Parameter par = new Parameter();
		String parName = (acc.useCycle) ? acc.cycleName : "Time interval (start)";
		if (acc.useCycle) {
			for (int i = 0; i < acc.timeBreaks.size(); i++) {
				par.addValue(acc.timeBreaks.elementAt(i));
			}
		} else {
			boolean dates = acc.timeBreaks.elementAt(0) instanceof Date;
			char datePrecision = 0;
			String dateScheme = null;
			if (dates) {
				String expl[] = { "(in the parameter values)", "Time interval: from " + acc.tStart + " to " + acc.tEnd };
				datePrecision = TimeDialogs.askDesiredDatePrecision(expl, (Date) acc.tStart);
				dateScheme = ((Date) acc.timeBreaks.elementAt(0)).scheme;
				if (datePrecision != acc.timeBreaks.elementAt(0).getPrecision()) {
					dateScheme = Date.removeExtraElementsFromScheme(dateScheme, datePrecision);
				}
				EnterDateSchemeUI enterSch = new EnterDateSchemeUI("Edit, if desired, the date/time scheme (template) for the parameter values. " + " The chosen precision is " + Date.getTextForTimeSymbol(datePrecision) + ". " + expl[0],
						"Date scheme:", dateScheme);
				OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Date/time scheme", false);
				dia.addContent(enterSch);
				dia.show();
				dateScheme = enterSch.getScheme();
			}
			par.addValue(acc.tStart.getCopy());
			for (int i = 0; i < acc.timeBreaks.size(); i++) {
				par.addValue(acc.timeBreaks.elementAt(i));
			}
			if (dates) {
				for (int i = 0; i < par.getValueCount(); i++) {
					((Date) par.getValue(i)).setPrecision(datePrecision);
					((Date) par.getValue(i)).setDateScheme(dateScheme);
				}
			}
		}
		par.setName(parName);
		String aName = "N events by " + ((acc.useCycle) ? parName : "time intervals");
		String aNameTotal = "N events total";
		DGeoLayer areaLayer = acc.areaLayer;
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
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "New attributes", true);
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
		int nAreas = areaLayer.getObjectCount();
		for (int i = 0; i < nAreas; i++) {
			DGeoObject gobj = areaLayer.getObject(i);
			DataRecord rec = (DataRecord) gobj.getData();
			if (rec == null) {
				rec = new DataRecord(gobj.getIdentifier(), gobj.getName());
				areaTable.addDataRecord(rec);
				gobj.setThematicData(rec);
			}
			for (int j = 0; j < acc.counts[i].length; j++) {
				rec.setNumericAttrValue(acc.counts[i][j], String.valueOf(acc.counts[i][j]), aIdx0 + j);
			}
			rec.setNumericAttrValue(acc.totals[i], String.valueOf(acc.totals[i]), totIdx);
		}
		if (makeNewTable) {
			DataLoader dLoader = core.getDataLoader();
			dLoader.setLink(areaLayer, dLoader.addTable(areaTable));
			areaLayer.setThematicFilter(areaTable.getObjectFilter());
			areaLayer.setLinkedToTable(true);
			core.getUI().showMessage("Table " + areaTable.getName() + " has been attached to layer " + areaLayer.getName(), false);
		} else {
			core.getUI().showMessage((areaTable.getAttrCount() - aIdx0) + " columns have been added to table " + areaTable.getName(), false);
		}
		areaTable.makeUniqueAttrIdentifiers();
		if (acc.neiCounts != null) {
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
			dia = new OKDialog(core.getUI().getMainFrame(), "Additional attributes", true);
			dia.addContent(p);
			dia.show();
			if (!dia.wasCancelled()) {
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
					if (rec == null) {
						continue;
					}
					for (int j = 0; j < acc.neiCounts[i].length; j++) {
						rec.setNumericAttrValue(acc.neiCounts[i][j], String.valueOf(acc.neiCounts[i][j]), aIdx1 + j);
					}
					rec.setNumericAttrValue(acc.neiTotals[i], String.valueOf(acc.neiTotals[i]), totIdx);
				}
			}
			areaTable.makeUniqueAttrIdentifiers();
			aName = "Average " + aN + ", including neighbourhood";
			aNameTotal = "Average " + aNT + ", including neighbourhood";
			atf.setText(aName);
			aTottf.setText(aNameTotal);
			dia = new OKDialog(core.getUI().getMainFrame(), "Additional attributes", true);
			dia.addContent(p);
			dia.show();
			if (!dia.wasCancelled()) {
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
					if (rec == null) {
						continue;
					}
					int nNb = 1;
					if (gobj.neighbours != null) {
						for (int j = 0; j < gobj.neighbours.size(); j++)
							if (areaLayer.findObjectById(gobj.neighbours.elementAt(j)) != null) {
								++nNb;
							}
					}
					for (int j = 0; j < acc.neiCounts[i].length; j++) {
						float val = 1.0f * acc.neiCounts[i][j] / nNb;
						rec.setNumericAttrValue(val, String.valueOf(val), aIdx1 + j);
					}
					float val = 1.0f * acc.neiTotals[i] / nNb;
					rec.setNumericAttrValue(val, String.valueOf(val), totIdx);
				}
				areaTable.makeUniqueAttrIdentifiers();
			}
		}
	}
}
