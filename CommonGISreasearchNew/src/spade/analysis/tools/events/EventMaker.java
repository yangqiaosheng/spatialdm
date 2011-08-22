package spade.analysis.tools.events;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.util.Vector;

import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.StringUtil;
import spade.time.Date;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.GeoComp;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.MultiGeometry;
import spade.vis.geometry.RealPoint;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Feb 3, 2010
 * Time: 11:10:43 AM
 * Creates a map layer with events (i.e. time-referenced spatial objects)
 * or adds new events to an existing layer.
 */
public class EventMaker {
	/**
	 * Reserved identifiers for the table columns with the event type and event start and end times
	 */
	public static final String evTypeColId = "__event_type__", evStartTimeColId = "__event_start_time__", evEndTimeColId = "__event_end_time__", evNPointsColId = "__event_N_points__", evDistanceColId = "__event_distance__",
			evDisplColId = "__event_displacement__", evSpaExtColId = "__event_spatial_extent__", evDirNumColId = "__event_move_direction__", evDirStrColId = "__event_move_direction_str__", evDurColId = "__event_duration__",
			evDurSecColId = "__event_duration_sec__", evDurMinColId = "__event_duration_min__", evDurHourColId = "__event_duration_hour__", evDurDayColId = "__event_duration_day__", evDurMonthColId = "__event_duration_month__",
			evStartMonthColId = "__event_start_month__", evStartDayOfWeekColId = "__event_start_day_of_week__", evStartTimeOfDayColId = "__event_start_time_of_day__", evStartHourColId = "__event_start_hour__";
	/**
	 * The system core provides access to existing layers and tables and allows to
	 * add new layers and tables to the system
	 */
	protected ESDACore core = null;
	/**
	 * The layer with events, either newly created or selected from existing layers
	 */
	protected DGeoLayer evtLayer = null;
	/**
	 * Indicates whether the layer existed before, i.e. new events are added to
	 * an existing layer rather than a new layer is created.
	 */
	protected boolean existingLayer = false;
	/**
	 * The table with data about the events
	 */
	protected DataTable table = null;
	/**
	 * Indicates whether the table existed before, i.e. data about new events are added to
	 * an existing table rather than a new table is created.
	 */
	protected boolean existingTable = false;
	/**
	 * The index of the table column in which the type of the event is specified
	 */
	protected int evTypeColN = -1;
	/**
	 * The index of the table column in which the start time of the event is specified
	 */
	protected int evTStartColN = -1;
	/**
	 * The index of the table column in which the end time of the event is specified
	 */
	protected int evTEndColN = -1;
	/**
	 * The index of the table column in which the number of points in the event is specified
	 */
	protected int evNPointsColN = -1;
	/**
	 * The index of the table column in which the travelled distance during the event is specified
	 */
	protected int evDistanceColN = -1;
	/**
	 * The index of the table column in which the displacement during the event is specified
	 */
	protected int evDisplColN = -1;
	/**
	 * The index of the table column in which the spatial extent of the event is specified
	 */
	protected int evSpaExtColN = -1;
	/**
	 * The indexes of the table columns in which the movement direction is specified in degrees
	 * and as a string
	 */
	protected int evDirNumColN = -1, evDirStrColN = -1;
	/**
	 * The indexes of the table columns in which the duration of the event is specified
	 * with different precision
	 */
	protected int evDurSecColN = -1, evDurMinColN = -1, evDurHourColN = -1, evDurDayColN = -1, evDurMonthColN = -1;
	/**
	 * The index of the table column in which the duration of the event is specified
	 * when the time is counted in abstract units
	 */
	protected int evDurColN = -1;
	/**
	 * The indexes of the table columns with date components: month, day of week,
	 * time of day, hour of day.
	 */
	protected int monthColN = -1, dayOfWeekColN = -1, timeOfDayColN = -1, hourColN = -1;

	/**
	 * The system core provides access to existing layers and tables and allows to
	 * add new layers and tables to the system
	 */
	public void setSystemCore(ESDACore core) {
		this.core = core;
	}

	/**
	 * The layer with events, either newly created or selected from existing layers
	 */
	public DGeoLayer getEventLayer() {
		return evtLayer;
	}

	/**
	 * The table with data about the events
	 */
	public DataTable getEventTable() {
		return table;
	}

	/**
	 * The index of the table column in which the type of the event is specified
	 */
	public int getEventTypeColN() {
		return evTypeColN;
	}

	/**
	 * The index of the table column in which the start time of the event is specified
	 */
	public int getEvTStartColN() {
		return evTStartColN;
	}

	/**
	 * The index of the table column in which the end time of the event is specified
	 */
	public int getEvTEndColN() {
		return evTEndColN;
	}

	/**
	 * Creates a new map layer in which events (time-referenced spatial objects)
	 * will be added using appropriate methods.
	 * @param objType specifies whether the objects will be points, lines, or areas.
	 * @param geographic specifies whether the coordinates of the objects are geographic
	 *   (longitudes and latitudes)
	 * @param movement specifies whether the layer will contain movement events
	 */
	public DGeoLayer makeGeoEventLayer(char objType, boolean geographic, boolean movement, String desiredLayerName) {
		if (core == null)
			return null;
		String name = (desiredLayerName == null) ? "Events" : desiredLayerName;
/*
    name=Dialogs.askForStringValue(getFrame(),"Name of the layer?","Events",
      "A new map layer with events will be created","Map layer with events",true);
    if (name==null)
      return null;
*/
		table = new DataTable();
		table.setName(name);
		table.addAttribute("Event type", evTypeColId, AttributeTypes.character);
		table.addAttribute("Start time", evStartTimeColId, AttributeTypes.time);
		table.addAttribute("End time", evEndTimeColId, AttributeTypes.time);
		table.addAttribute("Spatial extent", evSpaExtColId, AttributeTypes.real);
		evTypeColN = 0;
		evTStartColN = 1;
		evTEndColN = 2;
		evSpaExtColN = 3;
		if (objType == Geometry.point) {
			table.addAttribute("N points", evNPointsColId, AttributeTypes.integer);
			evNPointsColN = 4;
		}
		if (movement) {
			evDistanceColN = table.getAttrCount();
			evDisplColN = evDistanceColN + 1;
			table.addAttribute("Travelled distance", evDistanceColId, AttributeTypes.real);
			table.addAttribute("Displacement", evDisplColId, AttributeTypes.real);
			evDirNumColN = table.getAttrCount();
			evDirStrColN = evDirNumColN + 1;
			Attribute at = new Attribute(evDirNumColId, AttributeTypes.integer);
			at.setName("Movement direction (degree)");
			at.setPeriod(new Integer(0), new Integer(359));
			table.addAttribute(at);
			table.addAttribute("Movement direction (text)", evDirStrColId, AttributeTypes.character);
		}
		evtLayer = new DGeoLayer();
		evtLayer.setName(table.getName());
		evtLayer.setType(objType);
		evtLayer.setGeographic(geographic);
		DrawingParameters dp = evtLayer.getDrawingParameters();
		if (dp == null) {
			dp = new DrawingParameters();
			evtLayer.setDrawingParameters(dp);
		}
		dp.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
		dp.fillColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
		dp.fillContours = false;
		dp.lineWidth = 1;
		return evtLayer;
	}

	/**
	 * Suggests the user to choose one of the existing event layers, if any.
	 * If there are no such layers, or if the user did not select any of them,
	 * a new layer is created.
	 * @param objType specifies whether the objects will be points, lines, or areas.
	 * @param geographic specifies whether the coordinates of the objects are geographic
	 *   (longitudes and latitudes)
	 * @param movement specifies whether the layer will contain movement events
	 */
	public DGeoLayer chooseOrMakeGeoEventLayer(char objType, boolean geographic, boolean movement) {
		return chooseOrMakeGeoEventLayer(objType, geographic, movement, null);
	}

	public DGeoLayer chooseOrMakeGeoEventLayer(char objType, boolean geographic, boolean movement, String desiredLayerName) {
		if (core == null || core.getUI() == null)
			return null;
		SystemUI ui = core.getUI();
		if (ui.getCurrentMapViewer() != null && ui.getCurrentMapViewer().getLayerManager() != null) {
			LayerManager lman = ui.getCurrentMapViewer().getLayerManager();
			Vector<DGeoLayer> evLayers = new Vector<DGeoLayer>(lman.getLayerCount(), 1);
			for (int i = 0; i < lman.getLayerCount(); i++) {
				GeoLayer layer = lman.getGeoLayer(i);
				if (!(layer instanceof DGeoLayer)) {
					continue;
				}
				if (layer.getType() != objType) {
					continue;
				}
				if (layer.getObjectCount() > 0 && layer.hasTimeReferences()) {
					evLayers.addElement((DGeoLayer) layer);
				}
			}
			if (evLayers.size() > 0) {
				Panel mainP = new Panel(new ColumnLayout());
				mainP.add(new Label("Select the layer in which to add new events:"));
				List mList = new List(Math.max(evLayers.size() + 1, 5));
				for (int i = 0; i < evLayers.size(); i++) {
					mList.add(evLayers.elementAt(i).getName());
				}
				mList.select(mList.getItemCount() - 1);
				mainP.add(mList);
				OKDialog dia = new OKDialog(ui.getMainFrame(), "Add new events to a layer", true);
				dia.addContent(mainP);
				dia.show();
				if (!dia.wasCancelled()) {
					int idx = mList.getSelectedIndex();
					if (idx >= 0) {
						evtLayer = evLayers.elementAt(idx);
					}
				}
			}
		}
		if (evtLayer == null) {
			evtLayer = makeGeoEventLayer(objType, geographic, movement, desiredLayerName);
		} else {
			existingLayer = true;
			if (evtLayer.getThematicData() != null && (evtLayer.getThematicData() instanceof DataTable)) {
				table = (DataTable) evtLayer.getThematicData();
			}
			if (table == null) {
				table = new DataTable();
				table.setName(evtLayer.getName());
				table.addAttribute("Event type", evTypeColId, AttributeTypes.character);
				table.addAttribute("Start time", evStartTimeColId, AttributeTypes.time);
				table.addAttribute("End time", evEndTimeColId, AttributeTypes.time);
				table.addAttribute("Spatial extent", evSpaExtColId, AttributeTypes.real);
				evTypeColN = 0;
				evTStartColN = 1;
				evTEndColN = 2;
				evSpaExtColN = 3;
				if (objType == Geometry.point) {
					table.addAttribute("N points", evNPointsColId, AttributeTypes.integer);
					evNPointsColN = 4;
				}
				if (movement) {
					evDistanceColN = table.getAttrCount();
					evDisplColN = evDistanceColN + 1;
					table.addAttribute("Travelled distance", evDistanceColId, AttributeTypes.real);
					table.addAttribute("Displacement", evDisplColId, AttributeTypes.real);
					evDirNumColN = table.getAttrCount();
					evDirStrColN = evDirNumColN + 1;
					Attribute at = new Attribute(evDirNumColId, AttributeTypes.integer);
					at.setName("Movement direction (degree)");
					at.setPeriod(new Integer(0), new Integer(359));
					table.addAttribute(at);
					table.addAttribute("Movement direction (text)", evDirStrColId, AttributeTypes.character);
				}
				DataLoader dLoader = core.getDataLoader();
				int tblN = dLoader.addTable(table);
				dLoader.setLink(evtLayer, tblN);
				evtLayer.setLinkedToTable(true);
			} else {
				existingTable = true;
				evTypeColN = table.getAttrIndex(evTypeColId);
				Vector v = new Vector(10, 5);
				if (evTypeColN < 0) {
					table.addAttribute("Event type", evTypeColId, AttributeTypes.character);
					evTypeColN = table.getAttrCount() - 1;
					v.addElement(evTypeColId);
				}
				evTStartColN = table.getAttrIndex(evStartTimeColId);
				if (evTStartColN < 0) {
					table.addAttribute("Start time", evStartTimeColId, AttributeTypes.time);
					evTStartColN = table.getAttrCount() - 1;
					v.addElement(evStartTimeColId);
				}
				evTEndColN = table.getAttrIndex(evEndTimeColId);
				if (evTEndColN < 0) {
					table.addAttribute("End time", evEndTimeColId, AttributeTypes.time);
					evTEndColN = table.getAttrCount() - 1;
					v.addElement(evEndTimeColId);
				}
				evSpaExtColN = table.getAttrIndex(evSpaExtColId);
				if (evSpaExtColN < 0) {
					table.addAttribute("Spatial extent", evSpaExtColId, AttributeTypes.real);
					evSpaExtColN = table.getAttrCount() - 1;
					v.addElement(evEndTimeColId);
				}
				if (objType == Geometry.point) {
					evNPointsColN = table.getAttrIndex(evNPointsColId);
					if (evNPointsColN < 0) {
						table.addAttribute("N points", evNPointsColId, AttributeTypes.integer);
						evNPointsColN = table.getAttrCount() - 1;
						v.addElement(evNPointsColId);
					}
				}
				if (movement) {
					evDistanceColN = table.getAttrIndex(evDistanceColId);
					if (evDistanceColN < 0) {
						table.addAttribute("Travelled distance", evDistanceColId, AttributeTypes.real);
						evDistanceColN = table.getAttrCount() - 1;
						v.addElement(evDistanceColId);
					}
					evDisplColN = table.getAttrIndex(evDisplColId);
					if (evDisplColN < 0) {
						table.addAttribute("Displacement", evDisplColId, AttributeTypes.real);
						evDisplColN = table.getAttrCount() - 1;
						v.addElement(evDisplColId);
					}
					evDirNumColN = table.getAttrIndex(evDirNumColId);
					if (evDirNumColN < 0) {
						Attribute at = new Attribute(evDirNumColId, AttributeTypes.integer);
						at.setName("Movement direction (degree)");
						at.setPeriod(new Integer(0), new Integer(359));
						table.addAttribute(at);
						evDirNumColN = table.getAttrCount() - 1;
						v.addElement(evDirNumColId);
					}
					evDirStrColN = table.getAttrIndex(evDirStrColId);
					if (evDirStrColN < 0) {
						table.addAttribute("Movement direction (text)", evDirStrColId, AttributeTypes.character);
						evDirStrColN = table.getAttrCount() - 1;
						v.addElement(evDirStrColId);
					}
				}
				if (v.size() > 0) {
					table.notifyPropertyChange("new_attributes", null, v);
				}
				monthColN = table.getAttrIndex(evStartMonthColId);
				dayOfWeekColN = table.getAttrIndex(evStartDayOfWeekColId);
				timeOfDayColN = table.getAttrIndex(evStartTimeOfDayColId);
				hourColN = table.getAttrIndex(evStartHourColId);
				evDurSecColN = table.getAttrIndex(evDurSecColId);
				evDurMinColN = table.getAttrIndex(evDurMinColId);
				evDurHourColN = table.getAttrIndex(evDurHourColId);
				evDurDayColN = table.getAttrIndex(evDurDayColId);
				evDurMonthColN = table.getAttrIndex(evDurMonthColId);
				evDurColN = table.getAttrIndex(evDurColId);
			}
		}
		return evtLayer;
	}

	/**
	 * If the times of the events are dates (spade.time.Date), adds new columns
	 * with components of the dates (month, day of week, time of day, hour)
	 * to the table describing events. Which columns will be added, depends on
	 * the specified time range of the events.
	 */
	public void accountForEventTimeRange(TimeMoment t1, TimeMoment t2) {
		if (t1 == null || t2 == null)
			return;
		Vector v = new Vector(10, 5);
		if (!(t1 instanceof Date) || t1.getPrecisionIdx() == 1) {
			table.addAttribute("Duration", evDurColId, AttributeTypes.integer);
			evDurColN = table.getAttrCount() - 1;
			v.addElement(evDurColId);
		} else {
			Date d1 = (Date) t1, d2 = (Date) t2;
			char prec = d1.getPrecision();
			if (d1.hasElement('m') && monthColN < 0) {
				d1.setPrecision('m');
				d2.setPrecision('m');
				long diff = d2.subtract(d1);
				if (diff > 1) {
					Attribute at = new Attribute(evStartMonthColId, AttributeTypes.integer);
					at.setName("Start month");
					at.setPeriod(new Integer(1), new Integer(12));
					table.addAttribute(at);
					monthColN = table.getAttrCount() - 1;
					v.addElement(evStartMonthColId);
				}
			}
			if (d1.hasElement('d') && dayOfWeekColN < 0) {
				d1.setPrecision('d');
				d2.setPrecision('d');
				long diff = d2.subtract(d1);
				if (diff > 1) {
					Attribute at = new Attribute(evStartDayOfWeekColId, AttributeTypes.integer);
					at.setName("Start day of week");
					at.setPeriod(new Integer(1), new Integer(7));
					table.addAttribute(at);
					dayOfWeekColN = table.getAttrCount() - 1;
					v.addElement(evStartDayOfWeekColId);
				}
			}
			if (dayOfWeekColN >= 0 && timeOfDayColN < 0 && d1.hasElement('h') && d1.hasElement('t')) {
				Attribute at = new Attribute(evStartTimeOfDayColId, AttributeTypes.time);
				at.setName("Start time of day");
				Date dStart = new Date();
				dStart.setElementValue('h', 0);
				dStart.setElementValue('t', 0);
				if (d1.hasElement('s')) {
					dStart.setElementValue('s', 0);
					dStart.setPrecision('s');
					dStart.setDateScheme("hh:tt:ss");
				} else {
					dStart.setPrecision('t');
					dStart.setDateScheme("hh:tt");
				}
				Date dEnd = (Date) dStart.getCopy();
				dEnd.setElementValue('h', 23);
				dEnd.setElementValue('t', 59);
				if (d1.hasElement('s')) {
					dEnd.setElementValue('s', 59);
				}
				at.setPeriod(dStart, dEnd);
				table.addAttribute(at);
				timeOfDayColN = table.getAttrCount() - 1;
				v.addElement(evStartTimeOfDayColId);
			}
			if (d1.hasElement('h') && hourColN < 0) {
				d1.setPrecision('h');
				d2.setPrecision('h');
				long diff = d2.subtract(d1);
				if (diff > 1) {
					Attribute at = new Attribute(evStartHourColId, AttributeTypes.integer);
					at.setName("Start hour");
					at.setPeriod(new Integer(0), new Integer(23));
					table.addAttribute(at);
					hourColN = table.getAttrCount() - 1;
					v.addElement(evStartHourColId);
				}
			}
			d1.setPrecision(prec);
			d2.setPrecision(prec);
			if (d1.hasElement('s') && evDurSecColN < 0) {
				table.addAttribute("Duration (sec)", evDurSecColId, AttributeTypes.integer);
				evDurSecColN = table.getAttrCount() - 1;
				v.addElement(evDurSecColId);
			}
			if (d1.hasElement('t') && evDurMinColN < 0) {
				table.addAttribute("Duration (min)", evDurMinColId, (evDurSecColN >= 0) ? AttributeTypes.real : AttributeTypes.integer);
				evDurMinColN = table.getAttrCount() - 1;
				v.addElement(evDurMinColId);
			}
			if (hourColN >= 0) {
				table.addAttribute("Duration (hours)", evDurHourColId, (evDurMinColN >= 0) ? AttributeTypes.real : AttributeTypes.integer);
				evDurHourColN = table.getAttrCount() - 1;
				v.addElement(evDurHourColId);
			}
			if (dayOfWeekColN >= 0) {
				table.addAttribute("Duration (days)", evDurDayColId, (evDurHourColN >= 0) ? AttributeTypes.real : AttributeTypes.integer);
				evDurDayColN = table.getAttrCount() - 1;
				v.addElement(evDurDayColId);
			}
			if (monthColN >= 0 && d1.getPrecisionIdx() < 3) {
				table.addAttribute("Duration (months)", evDurMonthColId, AttributeTypes.integer);
				evDurMonthColN = table.getAttrCount() - 1;
				v.addElement(evDurMonthColId);
			}
		}
		if (v.size() > 0) {
			table.notifyPropertyChange("new_attributes", null, v);
		}
	}

	/**
	 * Adds a new event to the layer and data about the event to the table.
	 * @param id - the identifier of the new event
	 * @param type - the type of the event, which will be written in the table
	 * @param position - the spatial position of the event, specified as a geometry (e.g. RealPoint)
	 * @param validFrom - the time moment when the event appears
	 * @param validUntil - the last time moment of the event existence
	 * @return the DGeoObject that has been created.
	 * The data record describing the can be obtained using the method getData()
	 * of the geo object (returns ThematicDataItem, which can be cast to DataRecord).
	 */
	public DGeoObject addEvent(String id, String type, Geometry position, TimeMoment validFrom, TimeMoment validUntil) {
		if (evtLayer == null || table == null)
			return null;
		if (id == null) {
			id = "event_" + (evtLayer.getObjectCount() + 1);
		}
		DataRecord rec = new DataRecord(id);
		table.addDataRecord(rec);
		if (evTypeColN >= 0) {
			rec.setAttrValue(type, evTypeColN);
		}
		if (evTStartColN >= 0) {
			rec.setAttrValue(validFrom, evTStartColN);
		}
		if (evTEndColN >= 0) {
			rec.setAttrValue(validUntil, evTEndColN);
		}
		if (evDirStrColN >= 0) {
			rec.setAttrValue("n/a", evDirStrColN);
		}
		if (position != null) {
			if (evSpaExtColN >= 0) {
				float b[] = position.getBoundRect();
				if (b != null) {
					double dist = GeoComp.distance(b[0], b[1], b[2], b[3], evtLayer.isGeographic());
					rec.setNumericAttrValue(dist, StringUtil.doubleToStr(dist, 3), evSpaExtColN);
				}
			}
			MultiGeometry mg = (position instanceof MultiGeometry) ? (MultiGeometry) position : null;
			int nPoints = (mg == null) ? 1 : mg.getPartsCount();
			if (evNPointsColN >= 0) {
				rec.setNumericAttrValue(nPoints, String.valueOf(nPoints), evNPointsColN);
			}
			if (evDistanceColN >= 0 || evDisplColN >= 0)
				if (nPoints < 2) {
					if (evDistanceColN >= 0) {
						rec.setNumericAttrValue(0, "0.000", evDistanceColN);
					}
					if (evDisplColN >= 0) {
						rec.setNumericAttrValue(0, "0.000", evDisplColN);
					}
				} else {
					RealPoint p0 = SpatialEntity.getCentre(mg.getPart(0)), p1 = p0;
					double dist = 0;
					for (int i = 1; i < mg.getPartsCount(); i++) {
						RealPoint p2 = SpatialEntity.getCentre(mg.getPart(i));
						dist += GeoComp.distance(p1.x, p1.y, p2.x, p2.y, evtLayer.isGeographic());
						p1 = p2;
					}
					if (evDistanceColN >= 0) {
						rec.setNumericAttrValue(dist, StringUtil.doubleToStr(dist, 3), evDistanceColN);
					}
					dist = GeoComp.distance(p1.x, p1.y, p0.x, p0.y, evtLayer.isGeographic());
					if (evDisplColN >= 0) {
						rec.setNumericAttrValue(dist, StringUtil.doubleToStr(dist, 3), evDisplColN);
					}
					if (evDirNumColN >= 0) {
						//compute the direction
						double dir = GeoComp.getAngleYAxis(p1.x - p0.x, p1.y - p0.y);
						String str = null;
						if (!Double.isNaN(dir)) {
							int iDir = (int) Math.round(dir);
							rec.setNumericAttrValue(iDir, String.valueOf(iDir), evDirNumColN);
							str = GeoComp.getAngleYAxisAsString(dir);
						}
						if (str == null) {
							str = "n/a";
						}
						rec.setAttrValue(str, evDirStrColN);
					}
				}
		}
		if (evDurColN >= 0) {
			long dur = validUntil.subtract(validFrom);
			rec.setNumericAttrValue(dur, String.valueOf(dur), evDurColN);
		}
		if (validFrom instanceof Date) {
			Date d = (Date) validFrom;
			if (monthColN >= 0) {
				int m = d.getElementValue('m');
				rec.setNumericAttrValue(m, String.valueOf(m), monthColN);
			}
			if (dayOfWeekColN >= 0) {
				int day = d.getDayOfWeek();
				rec.setNumericAttrValue(day, String.valueOf(day), dayOfWeekColN);
			}
			if (timeOfDayColN >= 0) {
				Date dt = new Date();
				dt.setElementValue('h', d.getElementValue('h'));
				dt.setElementValue('t', d.getElementValue('t'));
				dt.setElementValue('s', d.getElementValue('s'));
				rec.setAttrValue(dt, timeOfDayColN);
			}
			if (hourColN >= 0) {
				int h = d.getElementValue('h');
				rec.setNumericAttrValue(h, String.valueOf(h), hourColN);
			}
			if (validUntil == null || validFrom.equals(validUntil)) {
				if (evDurSecColN >= 0) {
					rec.setNumericAttrValue(0, "0", evDurSecColN);
				}
				if (evDurMinColN >= 0) {
					rec.setNumericAttrValue(0, "0", evDurMinColN);
				}
				if (evDurHourColN >= 0) {
					rec.setNumericAttrValue(0, "0", evDurHourColN);
				}
				if (evDurDayColN >= 0) {
					rec.setNumericAttrValue(0, "0", evDurDayColN);
				}
				if (evDurMonthColN >= 0) {
					rec.setNumericAttrValue(0, "0", evDurMonthColN);
				}
			} else {
				Date d2 = (Date) validUntil;
				float diff = Float.NaN;
				if (evDurSecColN >= 0) {
					diff = d2.subtract(d, 's');
					rec.setNumericAttrValue(diff, String.valueOf(diff), evDurSecColN);
				}
				if (evDurMinColN >= 0) {
					diff = (!Float.isNaN(diff)) ? diff / 60 : d2.subtract(d, 't');
					String diffStr = (rec.getAttrType(evDurMinColN) == AttributeTypes.integer) ? String.valueOf(Math.round(diff)) : StringUtil.floatToStr(diff, 2);
					rec.setNumericAttrValue(diff, diffStr, evDurMinColN);
				}
				if (evDurHourColN >= 0) {
					diff = (!Float.isNaN(diff)) ? diff / 60 : d2.subtract(d, 'h');
					String diffStr = (rec.getAttrType(evDurHourColN) == AttributeTypes.integer) ? String.valueOf(Math.round(diff)) : StringUtil.floatToStr(diff, 2);
					rec.setNumericAttrValue(diff, diffStr, evDurHourColN);
				}
				if (evDurDayColN >= 0) {
					diff = (!Float.isNaN(diff)) ? diff / 24 : d2.subtract(d, 'd');
					String diffStr = (rec.getAttrType(evDurDayColN) == AttributeTypes.integer) ? String.valueOf(Math.round(diff)) : StringUtil.floatToStr(diff, 2);
					rec.setNumericAttrValue(diff, diffStr, evDurDayColN);
				}
				if (evDurMonthColN >= 0) {
					diff = d2.subtract(d, 'm');
					rec.setNumericAttrValue(diff, String.valueOf(diff), evDurMonthColN);
				}
			}
		}
		SpatialEntity se = new SpatialEntity(id);
		se.setGeometry(position);
		TimeReference tr = new TimeReference();
		tr.setValidFrom(validFrom);
		tr.setValidUntil(validUntil);
		se.setTimeReference(tr);
		rec.setTimeReference(tr);
		DGeoObject dgo = new DGeoObject();
		dgo.setup(se);
		dgo.setThematicData(rec);
		evtLayer.addGeoObject(dgo, false);
		return dgo;
	}

	/**
	 * For a given event (probably, created from a single point), determines the
	 * spatial characteristics (travelled distance, displacement, spatial extent)
	 * based on the given sequence of time-referenced points.
	 * It is assumed that the event has been previously
	 * constructed by the EventMaker and has a data record with the required fiels.
	 * If the sequence length is less than 2, immediately exits.
	 */
	public void getEventSpatialPropertiesFromSequence(DGeoObject event, Vector<SpatialEntity> sequence) {
		if (event == null || sequence == null || sequence.size() < 2)
			return;
		if (event.getData() == null)
			return;
		DataRecord rec = (DataRecord) event.getData();
		if (evNPointsColN >= 0) {
			int nPoints = sequence.size();
			rec.setNumericAttrValue(nPoints, String.valueOf(nPoints), evNPointsColN);
		}
		if (evSpaExtColN >= 0) {
			float b[] = sequence.elementAt(0).getGeometry().getBoundRect().clone();
			for (int k = 1; k < sequence.size(); k++) {
				float br[] = sequence.elementAt(k).getGeometry().getBoundRect();
				if (br != null)
					if (b == null) {
						b = br;
					} else {
						if (b[0] > br[0]) {
							b[0] = br[0];
						}
						if (b[1] > br[1]) {
							b[1] = br[1];
						}
						if (b[2] < br[2]) {
							b[2] = br[2];
						}
						if (b[3] < br[3]) {
							b[3] = br[3];
						}
					}
			}
			if (b != null) {
				double dist = GeoComp.distance(b[0], b[1], b[2], b[3], evtLayer.isGeographic());
				rec.setNumericAttrValue(dist, StringUtil.doubleToStr(dist, 3), evSpaExtColN);
			}
		}
		if (evDistanceColN >= 0) {
			double dist = 0;
			RealPoint p1 = sequence.elementAt(0).getCentre();
			for (int k = 1; k < sequence.size(); k++) {
				RealPoint p2 = sequence.elementAt(k).getCentre();
				dist += GeoComp.distance(p1.x, p1.y, p2.x, p2.y, evtLayer.isGeographic());
				p1 = p2;
			}
			rec.setNumericAttrValue(dist, StringUtil.doubleToStr(dist, 3), evDistanceColN);
		}
		if (evDisplColN >= 0) {
			RealPoint p1 = sequence.elementAt(0).getCentre();
			RealPoint p2 = sequence.elementAt(sequence.size() - 1).getCentre();
			double dist = GeoComp.distance(p1.x, p1.y, p2.x, p2.y, evtLayer.isGeographic());
			rec.setNumericAttrValue(dist, StringUtil.doubleToStr(dist, 3), evDisplColN);
		}
		if (evDirNumColN >= 0) {
			//compute the direction
			RealPoint p1 = sequence.elementAt(0).getCentre();
			RealPoint p2 = sequence.elementAt(sequence.size() - 1).getCentre();
			double dir = GeoComp.getAngleYAxis(p2.x - p1.x, p2.y - p1.y);
			String str = null;
			if (!Double.isNaN(dir)) {
				int iDir = (int) Math.round(dir);
				rec.setNumericAttrValue(iDir, String.valueOf(iDir), evDirNumColN);
				str = GeoComp.getAngleYAxisAsString(dir);
			}
			if (str == null) {
				str = "n/a";
			}
			rec.setAttrValue(str, evDirStrColN);
		}
	}

	/**
	 * Writes information about the movement direction in the thematic data record
	 * associated with the event if an appropriate field exists
	 */
	public boolean recordEventDirection(DGeoObject event, double dir) {
		if (event == null || evDirNumColN < 0)
			return false;
		if (event.getData() == null)
			return false;
		DataRecord rec = (DataRecord) event.getData();
		String str = null;
		if (!Double.isNaN(dir)) {
			int iDir = (int) Math.round(dir);
			rec.setNumericAttrValue(iDir, String.valueOf(iDir), evDirNumColN);
			str = GeoComp.getAngleYAxisAsString(dir);
		}
		if (str == null) {
			str = "n/a";
		}
		rec.setAttrValue(str, evDirStrColN);
		return true;
	}

	/**
	 * Finalizes the process of building or updating the layer
	 */
	public boolean finishLayerBuilding() {
		if (core == null || evtLayer == null || table == null)
			return false;
		if (existingLayer) {
			if (existingTable) {
				table.notifyPropertyChange("data_added", null, null);
			} else {
				core.getDataLoader().processTimeReferencedObjectSet(table);
			}
			evtLayer.notifyPropertyChange("ObjectSet", null, null);
		} else {
			DataLoader dLoader = core.getDataLoader();
			int tblN = dLoader.addTable(table);
			dLoader.addMapLayer(evtLayer, -1);
			System.out.println("EventMaker added map layer with " + evtLayer.getObjectCount() + " objects");
			long t0 = System.currentTimeMillis();
			dLoader.setLink(evtLayer, tblN);
			long t1 = System.currentTimeMillis();
			System.out.println("EventMaker: setLink took " + (t1 - t0) + " msec");
			evtLayer.setLinkedToTable(true);
			dLoader.processTimeReferencedObjectSet(evtLayer);
			dLoader.processTimeReferencedObjectSet(table);
			System.out.println("EventMaker finished!");
		}
		showMessage("A new map layer with extracted events has been built.", false);
		return true;
	}

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
	}

	protected Frame getFrame() {
		if (core == null || core.getUI() == null)
			return CManager.getAnyFrame();
		Frame fr = core.getUI().getMainFrame();
		if (fr == null) {
			fr = CManager.getAnyFrame();
		}
		return fr;
	}
}
