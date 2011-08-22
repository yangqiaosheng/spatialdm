package data_load;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.TableAttrSemanticsUI;
import spade.lib.basicwin.OKDialog;
import spade.time.Date;
import spade.time.TimeCount;
import spade.time.TimeMoment;
import spade.time.TimeRefDescription;
import spade.time.TimeReference;
import spade.time.ui.TimeFormatUI;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;
import spade.vis.spec.DataSourceSpec;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Sep 9, 2009
 * Time: 5:40:33 PM
 * Builds a map layer with point objects from a table containing coordinates
 */
public class PointLayerFromTableBuilder {
	protected ESDACore core = null;
	/**
	 * The necessary contents of a table
	 */
	public static final String souTableContents[] = { "x-coordinates", "y-coordinates", "identifiers of the spatial objects", "time moments" };
	/**
	 * The number of mandatory contents
	 */
	public static final int nMandContents = 2;

	/**
	 * Allows the user to select a table and tries to construct a map layer
	 * from the data contained in the table.
	 */
	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	public void run(ESDACore core) {
		if (core == null || core.getUI() == null)
			return;
		this.core = core;
		DataKeeper dKeeper = core.getDataKeeper();
		if (dKeeper == null || dKeeper.getTableCount() < 1) {
			showMessage("No tables available!", true);
			return;
		}
		List tList = new List(Math.min(10, dKeeper.getTableCount()));
		for (int i = 0; i < dKeeper.getTableCount(); i++) {
			AttributeDataPortion tbl = dKeeper.getTable(i);
			tList.add(tbl.getName());
		}
		tList.select(0);
		Panel p = new Panel(new BorderLayout());
		p.add(new Label("Select a table with movement data:"), BorderLayout.NORTH);
		p.add(tList, BorderLayout.CENTER);
		OKDialog okd = new OKDialog(core.getUI().getMainFrame(), "Select a table with movement data", true);
		okd.addContent(p);
		okd.show();
		if (okd.wasCancelled())
			return;
		int tIdx = tList.getSelectedIndex();
		if (tIdx < 0)
			return;
		AttributeDataPortion table = dKeeper.getTable(tIdx);
		if (!(table instanceof DataTable)) {
			showMessage("The table is not an instance of DataTable", true);
			return;
		}
		DataTable dTable = (DataTable) table;
		TimeRefDescription trefD = null;
		int trIdColN = -1, xColN = -1, yColN = -1, timeColN = -1;
		DataSourceSpec spec = (DataSourceSpec) dTable.getDataSource();
		if (spec != null) {
			if (!spec.multipleRowsPerObject) {
				trIdColN = spec.idFieldN;
				if (trIdColN < 0 && spec.idFieldName != null) {
					trIdColN = dTable.findAttrByName(spec.idFieldName);
				}
			}
			if (spec.xCoordFieldName != null) {
				xColN = dTable.findAttrByName(spec.xCoordFieldName);
			}
			if (spec.yCoordFieldName != null) {
				yColN = dTable.findAttrByName(spec.yCoordFieldName);
			}
			if (spec.descriptors != null) {
				for (int i = 0; i < spec.descriptors.size() && trefD == null; i++)
					if (spec.descriptors.elementAt(i) instanceof TimeRefDescription) {
						trefD = (TimeRefDescription) spec.descriptors.elementAt(i);
					}
			}
			if (trefD != null) {
				if (trefD.attrName != null) {
					timeColN = dTable.findAttrByName(trefD.attrName);
				}
				if (timeColN < 0 && trefD.sourceColumns != null && trefD.sourceColumns.length == 1) {
					timeColN = dTable.findAttrByName(trefD.sourceColumns[0]);
				}
			}
		}
		if (timeColN < 0) {
			for (int i = 0; i < dTable.getAttrCount() && timeColN < 0; i++)
				if (dTable.isAttributeTemporal(i) && tableColumnContainsTimes(dTable, i)) {
					timeColN = i;
				}
		}
		if (trIdColN < 0 || xColN < 0 || yColN < 0 || timeColN < 0) {
			TableAttrSemanticsUI semUI = new TableAttrSemanticsUI(table, souTableContents, nMandContents);
			if (!semUI.isOK()) {
				String err = semUI.getErrorMessage();
				if (err != null) {
					showMessage(err, true);
				} else {
					showMessage("Inappropriate table!", true);
				}
				return;
			}
			p = new Panel(new BorderLayout());
			p.add(new Label("Specify the table columns with the given contents:"), BorderLayout.NORTH);
			p.add(semUI, BorderLayout.CENTER);
			okd = new OKDialog(core.getUI().getMainFrame(), "Specify meanings of table columns", true);
			okd.addContent(p);
			okd.show();
			if (okd.wasCancelled())
				return;
			int colIdxMeaning[] = semUI.getColumnNumbers();
			xColN = colIdxMeaning[0];
			yColN = colIdxMeaning[1];
			trIdColN = colIdxMeaning[2];
			timeColN = colIdxMeaning[3];
			if (spec == null) {
				spec = new DataSourceSpec();
				spec.id = dTable.getEntitySetIdentifier();
			} else {
				spec = (DataSourceSpec) spec.clone();
			}
			spec.name = "Points from " + table.getName();
			spec.xCoordFieldName = dTable.getAttributeName(xColN);
			spec.yCoordFieldName = dTable.getAttributeName(yColN);
			if (trIdColN >= 0) {
				spec.idFieldN = trIdColN;
				spec.idFieldName = dTable.getAttributeName(trIdColN);
			}
			spec.multipleRowsPerObject = false;
		}
		if (timeColN >= 0) {
			if (trefD == null) {
				trefD = new TimeRefDescription();
				if (spec.descriptors == null) {
					spec.descriptors = new Vector(5, 5);
				}
				spec.descriptors.addElement(trefD);
				trefD.sourceColumns = new String[1];
				trefD.sourceColumns[0] = dTable.getAttributeName(timeColN);
				trefD.meaning = TimeRefDescription.OCCURRED_AT;
				trefD.isParameter = false;
			}
			trefD.attrName = dTable.getAttributeName(timeColN);
		}
		if (timeColN >= 0 && !tableColumnContainsTimes(dTable, timeColN)) {
			String scheme = null;
			Attribute attr = table.getAttribute(timeColN);
			//try to transform strings into time moments
			Vector v = table.getKAttrValuesAsStrings(attr.getIdentifier(), 50);
			if (v == null || v.size() < 1) {
				showMessage("No values in table column \"" + attr.getName() + "\"!", true);
				return;
			}
			TimeFormatUI tfUI = new TimeFormatUI("The string values in column \"" + attr.getName() + "\" need to be transformed into time moments.\n" + "Provide information for interpreting the string values as " + "dates and/or times.", v);
			okd = new OKDialog(core.getUI().getMainFrame(), "Transform strings into dates/times", true);
			okd.addContent(tfUI);
			okd.show();
			String err = tfUI.getErrorMessage();
			if (err != null) {
				showMessage(err, true);
			}
			if (okd.wasCancelled())
				return;
			scheme = tfUI.getScheme();
			setTimesInTableColumn(dTable, timeColN, scheme);
			trefD.schemes = new String[1];
			trefD.schemes[0] = scheme;
			dTable.setTimeReferences(timeColN, -1);
			if (dTable.isTimeReferenced() && (dKeeper instanceof PropertyChangeListener)) {
				dTable.addPropertyChangeListener((PropertyChangeListener) dKeeper);
				dTable.notifyPropertyChange("got_time_references", null, null);
			}
		}
		DGeoLayer layer = makePointLayer(dTable, trIdColN, xColN, yColN, timeColN, (DrawingParameters) spec.drawParm);
		if (layer == null) {
			showMessage("Failed to construct a map layer!", true);
			return;
		}
		String name = dTable.getName();
/*
    name= Dialogs.askForStringValue(core.getUI().getMainFrame(),"Name of the layer?",name,
      null,"Name of the layer?",false);
*/
		layer.setName(name);
		spec.name = name;
		layer.setDataTable(dTable);
		layer.setDataSource(spec);
		if (spec.drawParm == null) {
			spec.drawParm = layer.getDrawingParameters();
		}
		spec.toBuildMapLayer = true;

		DataLoader dLoader = core.getDataLoader();
		int trTblN = dLoader.getTableIndex(dTable.getContainerIdentifier());
		dLoader.addMapLayer(layer, -1);
		dLoader.setLink(layer, trTblN);
		layer.setThematicFilter(dTable.getObjectFilter());
		layer.setLinkedToTable(true);
		if (layer.hasTimeReferences()) {
			dLoader.processTimeReferencedObjectSet(layer);
		}
	}

	/**
	 * On the basis of the given table with coordinates of points, identifiers of
	 * objects, and, possibly, times, constructs a map layer with point objects.
	 * @param table - the table with the source data
	 * @param idColN - the column with the identifiers of the point objects;
	 *   if -1, the identifiers of the table records are taken as the identifiers
	 *   of the point objects
	 * @param xColN - the column with the x-coordinates
	 * @param yColN - the column with the y-coordinates
	 * @param timeColN - the column with the times (may be -1)
	 * @param drawParm - the drawing parameters of the new layer (if null, new
	 *                   parameters are generated)
	 * @return the layer built
	 */
	protected DGeoLayer makePointLayer(DataTable table, int idColN, int xColN, int yColN, int timeColN, DrawingParameters drawParm) {
		if (table == null || xColN < 0 || yColN < 0)
			return null;
		Vector geoObj = new Vector(table.getDataItemCount(), 100);
		for (int i = 0; i < table.getDataItemCount(); i++) {
			DataRecord rec = table.getDataRecord(i);
			double x = rec.getNumericAttrValue(xColN), y = rec.getNumericAttrValue(yColN);
			if (Double.isNaN(x) || Double.isNaN(y)) {
				continue;
			}
			RealPoint pt = new RealPoint((float) x, (float) y);
			String id = (idColN >= 0) ? rec.getAttrValueAsString(idColN) : null;
			if (id == null) {
				id = rec.getId();
			}
			TimeReference tref = null;
			SpatialEntity spe = new SpatialEntity(id, id);
			spe.setGeometry(pt);
			if (timeColN >= 0) {
				Object val = rec.getAttrValue(timeColN);
				if (val != null && (val instanceof TimeMoment)) {
					TimeMoment t = (TimeMoment) val;
					tref = new TimeReference();
					tref.setValidFrom(t);
					tref.setValidUntil(t);
				}
				spe.setTimeReference(tref);
			}
			DGeoObject obj = new DGeoObject();
			obj.setup(spe);
			geoObj.addElement(obj);
		}
		if (geoObj.size() < 1)
			return null;
		DGeoLayer layer = new DGeoLayer();
		layer.setType(Geometry.point);
		layer.setName(table.getName());
		layer.setGeoObjects(geoObj, true);
		if (drawParm == null) {
			DrawingParameters dp = new DrawingParameters();
			dp.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
			dp.fillColor = Color.getHSBColor((float) Math.random(), (float) Math.random(), (float) Math.random());
			dp.fillContours = false;
			layer.setDrawingParameters(dp);
		} else {
			layer.setDrawingParameters(drawParm);
		}
		layer.setDataSource(null);
		return layer;
	}

	protected boolean tableColumnContainsTimes(AttributeDataPortion table, int colN) {
		if (table == null || colN < 0 || colN >= table.getAttrCount())
			return false;
		for (int j = 0; j < table.getDataItemCount(); j++) {
			Object val = table.getAttrValue(colN, j);
			if (val != null && (val instanceof TimeMoment))
				return true;
		}
		return false;
	}

	/**
	 * If a table column supposed to contain times actually contain string values,
	 * tries to transform the strings into instances of Date or TimeCount,
	 * depending on the time scheme (template) specified.
	 * @param table - the table in which the transformation is done
	 * @param timeCIdx - the number of the column to contain times
	 * @param timeScheme - the template for the interpretation of the strings
	 */
	protected void setTimesInTableColumn(DataTable table, int timeCIdx, String timeScheme) {
		if (table == null || timeCIdx < 0)
			return;
		if (!table.isAttributeTemporal(timeCIdx)) {
			Attribute attr = table.getAttribute(timeCIdx);
			attr.setType(AttributeTypes.time);
			boolean simple = Date.isSimple(timeScheme);
			TimeMoment time = null;
			if (simple) {
				time = new TimeCount();
				if (timeScheme != null && timeScheme.length() > 0) {
					time.setPrecision(timeScheme.charAt(0));
				}
			} else {
				time = new Date();
				((Date) time).setDateScheme(timeScheme);
			}
			for (int j = 0; j < table.getDataItemCount(); j++) {
				String strVal = table.getAttrValueAsString(timeCIdx, j);
				if (strVal == null) {
					continue;
				}
				TimeMoment val = null;
				if (time.setMoment(strVal)) {
					val = time.getCopy();
				}
				table.getDataRecord(j).setAttrValue(val, timeCIdx);
			}
		}
	}

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
	}
}
