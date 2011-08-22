package spade.analysis.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.ESDACore;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.IntArray;
import spade.time.Date;
import spade.time.TimeCount;
import spade.time.TimeMoment;
import spade.time.ui.TimeFormatUI;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataTable;
import spade.vis.database.ThematicDataItem;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DLinkLayer;
import spade.vis.dmap.DLinkObject;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.Geometry;
import spade.vis.space.GeoLayer;
import spade.vis.space.GeoObject;
import spade.vis.space.LayerManager;
import spade.vis.spec.DataSourceSpec;
import spade.vis.spec.LinkDataDescription;
import data_load.LayerFromTableGenerator;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 03-Jan-2007
 * Time: 14:22:04
 * Builds a geographical layer consisting of links (vectors), which connect
 * objects in another layer. It is assumed that the data necessary for
 * constructing the links are available in a previously loaded table and the
 * layer with the objects to be linked is also loaded in the system.
 */
public class LinkLayerBuilder implements DataAnalyser, LayerFromTableGenerator {

	protected ESDACore core = null;
	/**
	 * The necessary and optional contents of a table with link data
	 */
	public static final String linkTableContents[] = { "identifiers of start (source) locations", "identifiers of end (destination) locations", "start times", "end times" };
	/**
	 * The number of mandatory contents
	 */
	public static final int nMandContents = 2;

	/**
	* Returns true when the tool has everything necessary for its operation.
	* A LinkLayerBuilder does not need any additional classes and therefore always
	* returns true.
	*/
	@Override
	public boolean isValid(ESDACore core) {
		return true;
	}

	/**
	 * The error message
	 */
	protected String err = null;

	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	@Override
	public void run(ESDACore core) {
		err = null;
		if (core == null || core.getUI() == null)
			return;
		this.core = core;
		if (core.getUI().getCurrentMapViewer() == null || core.getUI().getCurrentMapViewer().getLayerManager() == null) {
			showMessage("No map exists!", true);
			return;
		}
		LayerManager lman = core.getUI().getCurrentMapViewer().getLayerManager();
		if (lman.getLayerCount() < 1) {
			showMessage("No map layers available!", true);
			return;
		}
		if (core.getDataKeeper() == null || core.getDataKeeper().getTableCount() < 1) {
			showMessage("No tables available!", true);
			return;
		}
		List lList = new List(Math.min(10, lman.getLayerCount()));
		IntArray lInd = new IntArray(lman.getLayerCount(), 1);
		for (int i = 0; i < lman.getLayerCount(); i++) {
			GeoLayer layer = lman.getGeoLayer(i);
			if (layer.getType() == Geometry.point || layer.getType() == Geometry.area) {
				lList.add(layer.getName());
				lInd.addElement(i);
			}
		}
		if (lList.getItemCount() < 1) {
			showMessage("No appropriate layers with locations found!", true);
			return;
		}
		DataKeeper dKeeper = core.getDataKeeper();
		List tList = new List(Math.min(10, dKeeper.getTableCount()));
		for (int i = 0; i < dKeeper.getTableCount(); i++) {
			AttributeDataPortion tbl = dKeeper.getTable(i);
			tList.add(tbl.getName());
		}
		tList.select(0);
		Panel p = new Panel(new BorderLayout());
		p.add(new Label("Select the table with link data:"), BorderLayout.NORTH);
		p.add(tList, BorderLayout.CENTER);
		OKDialog okd = new OKDialog(core.getUI().getMainFrame(), "Select table with link data", true);
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

		GeoLayer layer = null;

		LinkDataDescription ldd = null;
		DataSourceSpec spec = (DataSourceSpec) table.getDataSource();
		if (spec == null) {
			spec = new DataSourceSpec();
		}
		if (spec.descriptors != null) {
			for (int i = 0; i < spec.descriptors.size() && ldd == null; i++)
				if (spec.descriptors.elementAt(i) instanceof LinkDataDescription) {
					ldd = (LinkDataDescription) spec.descriptors.elementAt(i);
				}
		}
		if (ldd != null && ldd.layerRef != null) {
			for (int i = 0; i < lman.getLayerCount() && layer == null; i++)
				if (lman.getGeoLayer(i).getContainerIdentifier().equalsIgnoreCase(ldd.layerRef)) {
					layer = lman.getGeoLayer(i);
				}
		}
		int colIdxMeaning[] = null;
		if (ldd != null && (ldd.souColName != null || ldd.souColIdx >= 0) && (ldd.destColName != null || ldd.destColIdx >= 0)) {
			if (ldd.souColIdx < 0) {
				ldd.souColIdx = table.findAttrByName(ldd.souColName);
			}
			if (ldd.souColIdx >= 0) {
				if (ldd.destColIdx < 0) {
					ldd.destColIdx = table.findAttrByName(ldd.destColName);
				}
				if (ldd.destColIdx >= 0) {
					if (ldd.souTimeColIdx < 0 && ldd.souTimeColName != null) {
						ldd.souTimeColIdx = table.findAttrByName(ldd.souTimeColName);
					}
					if (ldd.destTimeColIdx < 0 && ldd.destTimeColName != null) {
						ldd.destTimeColIdx = table.findAttrByName(ldd.destTimeColName);
					}
					colIdxMeaning = new int[4];
					colIdxMeaning[0] = ldd.souColIdx;
					colIdxMeaning[1] = ldd.destColIdx;
					colIdxMeaning[2] = ldd.souTimeColIdx;
					colIdxMeaning[3] = ldd.destTimeColIdx;
				}
			}
		}
		if (colIdxMeaning == null) {
			TableAttrSemanticsUI semUI = new TableAttrSemanticsUI(table, linkTableContents, nMandContents);
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
			colIdxMeaning = semUI.getColumnNumbers();
		}
		if (layer == null) {
			p = new Panel(new BorderLayout());
			p.add(new Label("Select the layer with locations:"), BorderLayout.NORTH);
			p.add(lList, BorderLayout.CENTER);
			okd = new OKDialog(core.getUI().getMainFrame(), "Select layer with locations", true);
			okd.addContent(p);
			okd.show();
			if (okd.wasCancelled())
				return;
			int lIdx = lList.getSelectedIndex();
			if (lIdx < 0)
				return;
			lIdx = lInd.elementAt(lIdx);
			layer = lman.getGeoLayer(lIdx);
		}

		String schemes[] = null;
		if (colIdxMeaning[2] >= 0 || colIdxMeaning[3] >= 0) {
			schemes = new String[2];
			schemes[0] = null;
			schemes[1] = null;
			if (ldd != null && (ldd.souTimeColIdx >= 0 || ldd.destTimeColIdx >= 0)) {
				schemes[0] = ldd.souTimeScheme;
				schemes[1] = ldd.destTimeScheme;
			} else {
				for (int i = 0; i < 2; i++) {
					int timeCIdx = colIdxMeaning[2 + i];
					if (timeCIdx >= 0 && !table.isAttributeTemporal(timeCIdx)) {
						Attribute attr = table.getAttribute(timeCIdx);
						//try to transform strings into time moments
						Vector v = table.getKAttrValuesAsStrings(attr.getIdentifier(), 50);
						if (v == null || v.size() < 1) {
							showMessage("No values in table column \"" + attr.getName() + "\"!", true);
							return;
						}
						TimeFormatUI tfUI = new TimeFormatUI("The string values in column \"" + attr.getName() + "\" need to be transformed into time moments.\n" + "Provide information for interpreting the string values as " + "dates and/or times.",
								v);
						okd = new OKDialog(core.getUI().getMainFrame(), "Transform strings into dates/times", true);
						okd.addContent(tfUI);
						okd.show();
						String err = tfUI.getErrorMessage();
						if (err != null) {
							showMessage(err, true);
						}
						if (okd.wasCancelled())
							return;
						schemes[i] = tfUI.getScheme();
					}
				}
			}
			setTimesInTableColumn(dTable, colIdxMeaning[2], schemes[0]);
			setTimesInTableColumn(dTable, colIdxMeaning[3], schemes[1]);
			dTable.setTimeReferences(colIdxMeaning[2], colIdxMeaning[3]);
			if (dTable.isTimeReferenced() && (dKeeper instanceof PropertyChangeListener)) {
				dTable.addPropertyChangeListener((PropertyChangeListener) dKeeper);
				dTable.notifyPropertyChange("got_time_references", null, null);
			}
		}
		DGeoLayer linkLayer = buildLinkLayer(dTable, layer, colIdxMeaning[0], colIdxMeaning[2], colIdxMeaning[1], colIdxMeaning[3], (DrawingParameters) spec.drawParm);
		if (linkLayer == null) {
			if (err == null) {
				err = "Failed to construct a link layer!";
			}
			showMessage(err, true);
			return;
		}
		if (ldd == null) {
			ldd = new LinkDataDescription();
			ldd.layerRef = layer.getContainerIdentifier();
			ldd.souColName = table.getAttributeName(colIdxMeaning[0]);
			ldd.souColIdx = colIdxMeaning[0];
			ldd.destColName = table.getAttributeName(colIdxMeaning[1]);
			ldd.destColIdx = colIdxMeaning[1];
			if (colIdxMeaning[2] >= 0) {
				ldd.souTimeColName = table.getAttributeName(colIdxMeaning[2]);
				ldd.souTimeColIdx = colIdxMeaning[2];
				if (schemes != null && schemes[0] != null) {
					ldd.souTimeScheme = schemes[0];
				}
			}
			if (colIdxMeaning[3] >= 0) {
				ldd.destTimeColName = table.getAttributeName(colIdxMeaning[3]);
				ldd.destTimeColIdx = colIdxMeaning[3];
				if (schemes != null && schemes[1] != null) {
					ldd.destTimeScheme = schemes[1];
				}
			}
			if (spec.descriptors == null) {
				spec.descriptors = new Vector(5, 5);
			}
			spec.descriptors.addElement(ldd);
		}
		spec.drawParm = linkLayer.getDrawingParameters();
		spec.toBuildMapLayer = true;
		linkLayer.setDataSource(spec);
		core.getDataLoader().addMapLayer(linkLayer, -1);
		core.getDataLoader().setLink(linkLayer, tIdx);
		if (linkLayer.hasTimeReferences() && (dKeeper instanceof PropertyChangeListener)) {
			linkLayer.addPropertyChangeListener((PropertyChangeListener) dKeeper);
			linkLayer.notifyPropertyChange("got_time_references", null, null);
		}
	}

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
	}

	/**
	 * Checks if this generator is relevant according to the given metadata
	 * (table destription).
	 */
	@Override
	public boolean isRelevant(DataSourceSpec spec) {
		if (spec == null)
			return false;
		if (spec.descriptors == null)
			return false;
		LinkDataDescription ldd = null;
		for (int i = 0; i < spec.descriptors.size() && ldd == null; i++)
			if (spec.descriptors.elementAt(i) instanceof LinkDataDescription) {
				ldd = (LinkDataDescription) spec.descriptors.elementAt(i);
			}
		return ldd != null && ldd.layerRef != null && (ldd.souColName != null || ldd.souColIdx >= 0) && (ldd.destColName != null || ldd.destColIdx >= 0);
	}

	/**
	 * Builds a map layer on the basis of an appropriate table. It is assumed that
	 * the Data Source Specification of the table contains all necessary metadata.
	 * @param table - the table with source data and metadata for layer generation
	 * @param dKeeper - the keeper of all data currently loaded in the system
	 *                  (the generator may need additional data)
	 * @param currMapN - the index of the current map (Layer Manager) among all
	 *                   maps loaded in the system (currently, only one map exists)
	 * @return the layer built or null.
	 */
	@Override
	public DGeoLayer buildLayer(AttributeDataPortion table, DataKeeper dKeeper, int currMapN) {
		err = null;
		if (table == null || dKeeper == null || !(table instanceof DataTable)) {
			err = "Inappropriate data for constructing link objects!";
			return null;
		}
		DataTable dTable = (DataTable) table;
		LayerManager lman = dKeeper.getMap(currMapN);
		if (lman == null) {
			err = "LinkLayerBuilder: no Layer Manager found!";
			return null;
		}
		DataSourceSpec spec = (DataSourceSpec) table.getDataSource();
		if (spec == null) {
			err = "LinkLayerBuilder: no specification for building a link layer found!";
			return null;
		}
		LinkDataDescription ldd = null;
		if (spec.descriptors != null) {
			for (int i = 0; i < spec.descriptors.size() && ldd == null; i++)
				if (spec.descriptors.elementAt(i) instanceof LinkDataDescription) {
					ldd = (LinkDataDescription) spec.descriptors.elementAt(i);
				}
		}
		if (ldd == null || ldd.layerRef == null || (ldd.souColName == null && ldd.souColIdx < 0) || (ldd.destColName == null && ldd.destColIdx < 0)) {
			err = "LinkLayerBuilder: no appropriate link data description!";
			return null;
		}
		GeoLayer layer = null;
		for (int i = 0; i < lman.getLayerCount() && layer == null; i++)
			if (lman.getGeoLayer(i).getContainerIdentifier().equalsIgnoreCase(ldd.layerRef)) {
				layer = lman.getGeoLayer(i);
			}
		if (layer == null) {
			err = "LinkLayerBuilder: layer [" + ldd.layerRef + "] not found!";
			return null;
		}
		if (layer.getObjectCount() < 2) {
			layer.loadGeoObjects();
		}
		if (layer.getObjectCount() < 2) {
			err = "LinkLayerBuilder: layer [" + ldd.layerRef + "] has not enough objects!";
			return null;
		}
		if (ldd.souColIdx < 0) {
			ldd.souColIdx = table.findAttrByName(ldd.souColName);
		}
		if (ldd.souColIdx < 0) {
			err = "LinkLayerBuilder: no column with the identifiers of the link start locations!";
			return null;
		}
		if (ldd.destColIdx < 0) {
			ldd.destColIdx = table.findAttrByName(ldd.destColName);
		}
		if (ldd.destColIdx < 0) {
			err = "LinkLayerBuilder: no column with the identifiers of the link end locations!";
			return null;
		}
		if (ldd.souTimeColIdx < 0 && ldd.souTimeColName != null) {
			ldd.souTimeColIdx = table.findAttrByName(ldd.souTimeColName);
		}
		if (ldd.destTimeColIdx < 0 && ldd.destTimeColName != null) {
			ldd.destTimeColIdx = table.findAttrByName(ldd.destTimeColName);
		}
		if (ldd.souTimeColIdx >= 0) {
			setTimesInTableColumn(dTable, ldd.souTimeColIdx, ldd.souTimeScheme);
		}
		if (ldd.destTimeColIdx >= 0) {
			setTimesInTableColumn(dTable, ldd.destTimeColIdx, ldd.destTimeScheme);
		}
		if (ldd.souTimeColIdx >= 0) {
			dTable.setTimeReferences(ldd.souTimeColIdx, ldd.destTimeColIdx);
			if (dTable.isTimeReferenced() && (dKeeper instanceof PropertyChangeListener)) {
				dTable.addPropertyChangeListener((PropertyChangeListener) dKeeper);
				dTable.notifyPropertyChange("got_time_references", null, null);
			}
		}
		DGeoLayer linkLayer = buildLinkLayer(dTable, layer, ldd.souColIdx, ldd.souTimeColIdx, ldd.destColIdx, ldd.destTimeColIdx, (DrawingParameters) spec.drawParm);
		if (linkLayer == null) {
			if (err == null) {
				err = "LinkLayerBuilder: failed to construct a link layer!";
			}
			return null;
		}
		spec.drawParm = linkLayer.getDrawingParameters();
		return linkLayer;
	}

	/**
	 * If a table column supposed to contain times actually contain string values,
	 * tries to transform the strings into instances of Date or TimeCount,
	 * depending on the time scheme (template) specified.
	 * @param table - the table in which the transformation is done
	 * @param timeCIdx - the number of the column to contain times
	 * @param timeScheme - the template for the interpretation of the strings
	 */
	private void setTimesInTableColumn(DataTable table, int timeCIdx, String timeScheme) {
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

	/**
	 * On the basis of the given table where link data are specified (i.e. the
	 * identifiers of the start and end locations and, optionally, start and end
	 * times) and the given map layer with the start and end locations, builds
	 * a new map layer where the objects are instances of DLinkObjec.
	 * @param table - the table with the link data
	 * @param placeLayer - the layer with the start and end locations of the links
	 * @param startCIdx - the column with the identifiers of the link starts
	 * @param t1CIdx - the column with the times of the link starts
	 * @param endCIdx - the column with the identifiers of the link ends
	 * @param t2CIdx - the column with the times of the link ends
	 * @param drawParm - the drawing parameters of the new layer (if null, new
	 *                   parameters are generated)
	 * @return the layer built
	 */
	private DGeoLayer buildLinkLayer(DataTable table, GeoLayer placeLayer, int startCIdx, int t1CIdx, int endCIdx, int t2CIdx, DrawingParameters drawParm) {
		if (table == null || placeLayer == null || startCIdx < 0 || endCIdx < 0) {
			err = "No data for building links!";
			return null;
		}
		//construct DLinkObjects
		Vector linkObj = new Vector(table.getDataItemCount(), 100);
		boolean timeReferenced = false;
		for (int i = 0; i < table.getDataItemCount(); i++) {
			ThematicDataItem tit = (ThematicDataItem) table.getDataItem(i);
			String id1 = tit.getAttrValueAsString(startCIdx), id2 = tit.getAttrValueAsString(endCIdx);
			if (id1 == null || id2 == null) {
				continue;
			}
			GeoObject obj1 = placeLayer.findObjectById(id1);
			if (obj1 == null || !(obj1 instanceof DGeoObject)) {
				continue;
			}
			GeoObject obj2 = placeLayer.findObjectById(id2);
			if (obj2 == null || !(obj2 instanceof DGeoObject)) {
				continue;
			}
			TimeMoment t1 = null, t2 = null;
			if (t1CIdx >= 0) {
				Object val = tit.getAttrValue(t1CIdx);
				if (val != null && (val instanceof TimeMoment)) {
					t1 = (TimeMoment) val;
				}
			}
			if (t1 != null && t2CIdx >= 0) {
				Object val = tit.getAttrValue(t2CIdx);
				if (val != null && (val instanceof TimeMoment)) {
					t2 = (TimeMoment) val;
				}
			}
			timeReferenced = timeReferenced || (t1 != null && t2 != null);
			DLinkObject lObj = new DLinkObject();
			lObj.setup((DGeoObject) obj1, (DGeoObject) obj2, t1, t2);
			lObj.setIdentifier(tit.getId());
			if (tit.getName() != null) {
				lObj.setName(tit.getName());
			}
			lObj.setThematicData(tit);
			linkObj.addElement(lObj);
		}
		if (linkObj.size() < 1) {
			err = "No link objects have been constructed!";
			return null;
		}
		DLinkLayer linkLayer = new DLinkLayer();
		linkLayer.setType(Geometry.line);
		linkLayer.setName(table.getName());
		linkLayer.setGeoObjects(linkObj, true);
		if (drawParm == null) {
			DrawingParameters dp = new DrawingParameters();
			linkLayer.setDrawingParameters(dp);
			dp.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
			dp.lineWidth = 3;
		} else {
			linkLayer.setDrawingParameters(drawParm);
		}
		linkLayer.setDataSource(null);
		linkLayer.setPlaceLayer((DGeoLayer) placeLayer);
		return linkLayer;
	}

	/**
	 * Returns the generated error message or null if successful
	 */
	@Override
	public String getErrorMessage() {
		return err;
	}
}
