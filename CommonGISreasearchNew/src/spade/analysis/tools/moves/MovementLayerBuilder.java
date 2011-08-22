package spade.analysis.tools.moves;

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
import spade.analysis.tools.DataAnalyser;
import spade.analysis.tools.TableAttrSemanticsUI;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.Comparator;
import spade.lib.util.IntArray;
import spade.lib.util.QSortAlgorithm;
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
import spade.vis.dmap.DMovingObject;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.spec.DataSourceSpec;
import data_load.LayerFromTableGenerator;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 17-Apr-2007
 * Time: 17:58:52
 * Builds a geographical layer consisting of trajectories of moving objects.
 * It is assumed that the data necessary for constructing the trajectories are
 * available in a previously loaded table and, possibly, a layer with the
 * positions of the objects at different time moments (if there is no layer,
 * the positions must be specified in the table).
 */
public class MovementLayerBuilder implements DataAnalyser, LayerFromTableGenerator, Comparator {

	protected ESDACore core = null;
	/**
	 * The necessary contents of a table with movement data
	 */
	public static final String souTableContents[] = { "identifiers of lines or trajectories", "x-coordinates", "y-coordinates", "time moments", "x-coordinates of event ends", "y-coordinates of event ends", "time moments of event ends", };
	/**
	 * The number of mandatory contents
	 */
	public static final int nMandContents = 4;

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
		int x2ColN = -1, y2ColN = -1, time2ColN = -1;
		DataSourceSpec spec = (DataSourceSpec) dTable.getDataSource();
		if (spec != null) {
			if (spec.multipleRowsPerObject) {
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
			trIdColN = colIdxMeaning[0];
			xColN = colIdxMeaning[1];
			yColN = colIdxMeaning[2];
			timeColN = colIdxMeaning[3];
			x2ColN = colIdxMeaning[4];
			y2ColN = colIdxMeaning[5];
			time2ColN = colIdxMeaning[6];
			if (spec == null) {
				spec = new DataSourceSpec();
				spec.id = dTable.getEntitySetIdentifier();
			} else {
				spec = (DataSourceSpec) spec.clone();
			}
			spec.name = "Trajectories from " + table.getName();
			spec.xCoordFieldName = dTable.getAttributeName(xColN);
			spec.yCoordFieldName = dTable.getAttributeName(yColN);
			spec.idFieldN = trIdColN;
			spec.idFieldName = dTable.getAttributeName(trIdColN);
			spec.multipleRowsPerObject = true;
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
		if (time2ColN >= 0 && !tableColumnContainsTimes(dTable, time2ColN)) {
			String scheme = null;
			Attribute attr = table.getAttribute(time2ColN);
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
			setTimesInTableColumn(dTable, time2ColN, scheme);
			trefD.schemes = new String[1];
			trefD.schemes[0] = scheme;
			dTable.setTimeReferences(timeColN, time2ColN);
			if (dTable.isTimeReferenced() && (dKeeper instanceof PropertyChangeListener)) {
				dTable.addPropertyChangeListener((PropertyChangeListener) dKeeper);
				dTable.notifyPropertyChange("got_time_references", null, null);
			}
		}
		DGeoLayer tLayer = buildLineOrMovementLayer(dTable, trIdColN, xColN, yColN, timeColN, x2ColN, y2ColN, time2ColN, (DrawingParameters) spec.drawParm);
		if (tLayer == null) {
			if (err == null) {
				err = "Failed to construct a layer with trajectories!";
			}
			showMessage(err, true);
			return;
		}
		tLayer.setName(spec.name);
		tLayer.setDataSource(spec);
		if (spec.drawParm == null) {
			spec.drawParm = tLayer.getDrawingParameters();
		}
		spec.toBuildMapLayer = true;

		//it is important first to add the layer to the map in order to properly
		//set the "geographic" flag. Otherwise, the computed inter-point distances
		//and the track lengths in the table will be wrong for geographical trajectories
		DataLoader dLoader = core.getDataLoader();
		dLoader.addMapLayer(tLayer, -1);

		DataTable dtTraj = TrajectoriesTableBuilder.makeTrajectoryDataTable(tLayer.getObjects());
		dtTraj.setName(tLayer.getName() + ": general data");

		int trTblN = dLoader.addTable(dtTraj);
		tLayer.setDataTable(dtTraj);
		dLoader.setLink(tLayer, trTblN);
		tLayer.setLinkedToTable(true);
		tLayer.setThematicFilter(dtTraj.getObjectFilter());
		tLayer.setLinkedToTable(true);
		if (tLayer.hasTimeReferences()) {
			dLoader.processTimeReferencedObjectSet(tLayer);
			dLoader.processTimeReferencedObjectSet(dtTraj);
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
		if (!spec.multipleRowsPerObject)
			return false;
		if (spec.idFieldName == null || spec.xCoordFieldName == null || spec.yCoordFieldName == null)
			return false;
		return true;
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
			err = "Inappropriate data for constructing line or movement objects!";
			return null;
		}
		DataTable dTable = (DataTable) table;
		DataSourceSpec spec = (DataSourceSpec) table.getDataSource();
		if (spec == null) {
			err = "MovementLayerBuilder: no specification for building a layer found!";
			return null;
		}
		TimeRefDescription trefD = null;
		int trIdColN = spec.idFieldN, xColN = -1, yColN = -1, timeColN = -1;
		if (trIdColN < 0 && spec.idFieldName != null) {
			trIdColN = dTable.findAttrByName(spec.idFieldName);
		}
		if (trIdColN < 0) {
			err = "No trajectory/line identifiers in the table!";
			return null;
		}
		if (spec.xCoordFieldName != null) {
			xColN = dTable.findAttrByName(spec.xCoordFieldName);
		}
		if (xColN < 0) {
			err = "No column with X-coordinates in the table!";
			return null;
		}
		if (spec.yCoordFieldName != null) {
			yColN = dTable.findAttrByName(spec.yCoordFieldName);
		}
		if (yColN < 0) {
			err = "No column with Y-coordinates in the table!";
			return null;
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
		if (timeColN < 0) {
			for (int i = 0; i < dTable.getAttrCount() && timeColN < 0; i++)
				if (dTable.isAttributeTemporal(i) && tableColumnContainsTimes(dTable, i)) {
					timeColN = i;
				}
		}
		DGeoLayer layer = buildLineOrMovementLayer(dTable, trIdColN, xColN, yColN, timeColN, (DrawingParameters) spec.drawParm);
		if (layer == null) {
			if (err == null) {
				err = "Failed to construct a layer!";
			}
			return null;
		}
		spec.drawParm = layer.getDrawingParameters();
		return layer;
	}

	/**
	 * On the basis of the given table with coordinates of points, identifiers of
	 * trajectories or lines, and, possibly, times, constructs a map layer with
	 * polylines or trajectories.
	 * @param table - the table with the link data
	 * @param idColN - the column with the identifiers of the trajectories or lines
	 * @param xColN - the column with the x-coordinates
	 * @param yColN - the column with the y-coordinates
	 * @param timeColN - the column with the times (may be -1)
	 * @param drawParm - the drawing parameters of the new layer (if null, new
	 *                   parameters are generated)
	 * @return the layer built
	 */
	private DGeoLayer buildLineOrMovementLayer(DataTable table, int idColN, int xColN, int yColN, int timeColN, DrawingParameters drawParm) {
		return buildLineOrMovementLayer(table, idColN, xColN, yColN, timeColN, -1, -1, -1, drawParm);
	}

	/**
	 * On the basis of the given table with coordinates of points, identifiers of
	 * trajectories or lines, and, possibly, times, constructs a map layer with
	 * polylines or trajectories.
	 * @param table - the table with the link data
	 * @param idColN - the column with the identifiers of the trajectories or lines
	 * @param xColN - the column with the x-coordinates
	 * @param yColN - the column with the y-coordinates
	 * @param timeColN - the column with the times (may be -1)
	 * @param x2ColN - the column with the x-coordinates of the second point (event end)
	 * @param y2ColN - the column with the y-coordinates of the second point (event end)
	 * @param time2ColN - the column with the times of the second point (event end) (may be -1)
	 * @param drawParm - the drawing parameters of the new layer (if null, new
	 *                   parameters are generated)
	 * @return the layer built
	 */
	private DGeoLayer buildLineOrMovementLayer(DataTable table, int idColN, int xColN, int yColN, int timeColN, int x2ColN, int y2ColN, int time2ColN, DrawingParameters drawParm) {
		if (table == null || idColN < 0 || xColN < 0 || yColN < 0) {
			err = "No data for building lines or trajectories!";
			return null;
		}
		Vector geoObj = new Vector(500, 100);
		String lastId = null;
		DGeoObject obj = null;
		Vector points = new Vector(500, 100);
		Vector pointAttrList = null;
		IntArray pointAttrColNs = null;
		if (timeColN >= 0) { //find out if any thematic data are attached to the positions
			pointAttrList = new Vector(table.getAttrCount(), 1);
			pointAttrColNs = new IntArray(table.getAttrCount(), 1);
			for (int i = 0; i < table.getAttrCount(); i++)
				if (i != idColN && i != xColN && i != yColN && i != timeColN) {
					String aName = table.getAttributeName(i);
					if (aName.equalsIgnoreCase("trN") || aName.equalsIgnoreCase("pIdx")) {
						continue;
					}
					pointAttrColNs.addElement(i);
					pointAttrList.addElement(table.getAttribute(i));
				}
			if (pointAttrList.size() < 1) {
				pointAttrList = null;
				pointAttrColNs = null;
			}
		}
		Vector records = (Vector) table.getData().clone();
		this.trIdColN = idColN;
		this.timeColN = timeColN;
		QSortAlgorithm.sort(records, this, false);
		for (int i = 0; i < records.size(); i++) {
			DataRecord rec = (DataRecord) records.elementAt(i);
			String id = rec.getAttrValueAsString(idColN);
			if (lastId == null || !lastId.equalsIgnoreCase(id)) {
				if (obj != null) {
					if (points.size() < 1) {
						geoObj.removeElementAt(geoObj.size() - 1);
					} else if (obj instanceof DMovingObject) {
						Vector track = new Vector(points.size(), 1);
						for (int j = 0; j < points.size(); j++) {
							track.addElement(points.elementAt(j));
						}
						((DMovingObject) obj).setTrack(track);
					} else {
						RealPolyline poly = new RealPolyline();
						poly.p = new RealPoint[points.size()];
						for (int j = 0; j < points.size(); j++) {
							poly.p[j] = (RealPoint) points.elementAt(j);
						}
						obj.getSpatialData().setGeometry(poly);
					}
				}
				points.removeAllElements();
				//a new object comes
				SpatialEntity spe = new SpatialEntity(id, id);
				if (timeColN < 0) {
					obj = new DGeoObject();
				} else {
					obj = new DMovingObject();
				}
				obj.setup(spe);
				if (obj instanceof DMovingObject) {
					obj.setIdentifier(id);
				}
				geoObj.addElement(obj);
				lastId = id;
			}
			double x = rec.getNumericAttrValue(xColN), y = rec.getNumericAttrValue(yColN);
			if (Double.isNaN(x) || Double.isNaN(y)) {
				continue;
			}
			RealPoint pt = new RealPoint((float) x, (float) y);
			if (timeColN < 0) {
				points.addElement(pt);
			} else {
				Object val = rec.getAttrValue(timeColN);
				if (val == null || !(val instanceof TimeMoment)) {
					continue;
				}
				TimeMoment t = (TimeMoment) val;
				SpatialEntity pos = new SpatialEntity(id + "_" + (points.size() + 1));
				pos.setGeometry(pt);
				TimeReference tref = new TimeReference();
				tref.setValidFrom(t);
				tref.setValidUntil(t);
				pos.setTimeReference(tref);
				if (pointAttrList != null) {
					DataRecord pRec = new DataRecord(pos.getId());
					pRec.setAttrList(pointAttrList);
					for (int j = 0; j < pointAttrColNs.size(); j++) {
						pRec.addAttrValue(rec.getAttrValue(pointAttrColNs.elementAt(j)));
					}
					pos.setThematicData(pRec);
				}
				points.addElement(pos);
				if (x2ColN >= 0 && y2ColN >= 0) {
					x = rec.getNumericAttrValue(x2ColN);
					y = rec.getNumericAttrValue(y2ColN);
					if (Double.isNaN(x) || Double.isNaN(y)) {
						continue;
					}
					pt = new RealPoint((float) x, (float) y);
					if (time2ColN < 0) {
						points.addElement(pt);
					} else {
						val = rec.getAttrValue(time2ColN);
						if (val == null || !(val instanceof TimeMoment)) {
							continue;
						}
						t = (TimeMoment) val;
						pos = new SpatialEntity(id + "_" + (points.size() + 1));
						pos.setGeometry(pt);
						tref = new TimeReference();
						tref.setValidFrom(t);
						tref.setValidUntil(t);
						pos.setTimeReference(tref);
						if (pointAttrList != null) {
							DataRecord pRec = new DataRecord(pos.getId());
							pRec.setAttrList(pointAttrList);
							for (int j = 0; j < pointAttrColNs.size(); j++) {
								pRec.addAttrValue(rec.getAttrValue(pointAttrColNs.elementAt(j)));
							}
							pos.setThematicData(pRec);
						}
						points.addElement(pos);
					}
				}
			}
		}
		if (obj != null) {
			if (points.size() < 2) {
				geoObj.removeElementAt(geoObj.size() - 1);
			} else if (obj instanceof DMovingObject) {
				((DMovingObject) obj).setTrack(points);
			} else {
				RealPolyline poly = new RealPolyline();
				poly.p = new RealPoint[points.size()];
				for (int j = 0; j < points.size(); j++) {
					poly.p[j] = (RealPoint) points.elementAt(j);
				}
				obj.getSpatialData().setGeometry(poly);
			}
		}
		if (geoObj.size() < 1) {
			err = "No geographical objects have been constructed!";
			return null;
		}
		DGeoLayer layer = new DGeoLayer();
		layer.setType(Geometry.line);
		layer.setName("Trajectories from " + table.getName());
		layer.setGeoObjects(geoObj, true);
		layer.setHasMovingObjects(timeColN >= 0);
		if (drawParm == null) {
			DrawingParameters dp = new DrawingParameters();
			layer.setDrawingParameters(dp);
			dp.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
			dp.lineWidth = 3;
		} else {
			layer.setDrawingParameters(drawParm);
		}
		layer.setDataSource(null);
		return layer;
	}

	private int trIdColN = -1, timeColN = -1;

	/**
	 * Returns 0 if the objects are equal, <0 if the first object is less than the
	 * second one, >0 otherwise
	 */
	@Override
	public int compare(Object obj1, Object obj2) {
		if (obj1 == null)
			if (obj2 == null)
				return 0;
			else
				return 1;
		if (obj2 == null)
			return -1;
		if (!(obj1 instanceof DataRecord) && !(obj2 instanceof DataRecord))
			return 0;
		DataRecord rec1 = (DataRecord) obj1, rec2 = (DataRecord) obj2;
		String id1 = rec1.getAttrValueAsString(trIdColN), id2 = rec2.getAttrValueAsString(trIdColN);
		if (id1 == null)
			if (id2 == null)
				return 0;
			else
				return 1;
		if (id2 == null)
			return -1;
		int cmp = id1.compareTo(id2);
		if (cmp < 0)
			return -1;
		if (cmp > 0)
			return 1;
		if (timeColN < 0)
			return 0;
		obj1 = rec1.getAttrValue(timeColN);
		obj2 = rec2.getAttrValue(timeColN);
		if (obj1 == null)
			if (obj2 == null)
				return 0;
			else
				return 1;
		if (obj2 == null)
			return -1;
		if (!(obj1 instanceof TimeMoment) && !(obj2 instanceof TimeMoment))
			return 0;
		TimeMoment t1 = (TimeMoment) obj1, t2 = (TimeMoment) obj2;
		return t1.compareTo(t2);
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

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
	}

	/**
	 * Returns the generated error message or null if successful
	 */
	@Override
	public String getErrorMessage() {
		return err;
	}
}
