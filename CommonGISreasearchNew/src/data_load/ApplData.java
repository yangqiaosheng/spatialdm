package data_load;

import java.awt.Color;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.lang.Language;
import spade.lib.util.CopyFile;
import spade.time.Date;
import spade.time.TimeCount;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealRectangle;
import spade.vis.spec.DataPipeSpec;
import spade.vis.spec.DataSourceSpec;
import spade.vis.spec.DecisionSpec;

/**
* Description of an application with a specification of the data to load.
* Application description is usually stored in a *.mwi or *.app file.
*/
public class ApplData implements java.io.Serializable {
	static ResourceBundle res = Language.getTextResource("data_load.Res");
	/**
	* Name of the application
	*/
	public String applName = null;
	/**
	* Name of the territory
	*/
	public String terrName = null;
	/**
	* Metadata attribute name of the current level bounding geometry:
	* for instance ("->" means "part-of"): Lombardia->Italy, Italy->Europe
	*/
	public String partOfAttrName = null;
	/**
	* Name of attribute with file names of level-down geometry
	*/
	public String levelDownAttrName = null;
	/**
	* The territory extent to be initially visible in the map view. May be null.
	* In this case the whole territory is shown.
	* The extent is an array of 4 float numbers:
	* 0) x1; 1) y1; 2) x2; 3) y2
	*/
	public float extent[] = null;
	/**
	* The full territory extent (may be null).
	* The extent is an array of 4 float numbers:
	* 0) x1; 1) y1; 2) x2; 3) y2
	*/
	public float fullExtent[] = null;
	/**
	* User-defined scaling factor for the map
	*/
	public float user_factor = 1.0f;
	/**
	* User-defined unit in which coordinates are specified
	*/
	public String user_unit = "m";
	/**
	 * Indicates if the coordinates are geographic:
	 * 0 - not, 1 - yes, -1 - unknown
	 */
	public int coordsAreGeographic = -1;
	/**
	 * OpenStreetMaps map tiles may be stored in a local file system.
	 * If there are such files, the following field may contain the path
	 * to the tile index file with the extension ".osm"
	 */
	public String pathOSMTilesIndex = null;
//ID
	/**
	* A parameter controlling visibility of a legend
	*/
	public boolean show_legend = true;
	/**
	* Percentage of space taken by legend in application window
	*/
	public int percent_of_legend = 30;
	/**
	* A parameter controlling visibility of a territory name
	*/
	public boolean show_terrname = true;
	/**
	* A parameter controlling visibility of a background color change control
	*/
	public boolean show_bgcolor = true;
	/**
	* A parameter controlling visibility of a scale
	*/
	public boolean show_scale = true;
	/**
	* Indicates if total number of objects is displayed in legend.
	*/
	public boolean show_nobjects = true;
	/**
	* A parameter controlling visibility of persistent record
	*/
	public boolean show_persistent = false;
	/**
	* A parameter controlling visibility of object tooltips
	*/
	public boolean show_tooltip = true;
	/**
	* Project description
	*/
	public String about_project = "";
//~ID
// P.G.
	/**
	* A parameter controlling visibility of a manipulator
	*/
	public boolean show_manipulator = true;
	/**
	* Percentage of space taken by manipulator in application window
	*/
	public int percent_of_manipulator = 30;
// ~P.G.
	/**
	* The URL of the data server that is used for loading the data
	*/
	public String dataServerURL = null;
	/**
	* Global background color for map canvas
	*/
	public Color applBgColor = Color.lightGray;
	/**
	* Vector of specifications of the data to be loaded
	*/
	public Vector dataSpecs = null;
	/**
	* Path to the knowledge base on task support specific for this application
	* (if such a knowledge base exists). Normally a generic knowledge base
	* is loaded. The path to the generic knowledge base is specified in system.cnf
	*/
	public String pathToTaskKB = null;
	/**
	* Path to the tutorial (typically null).
	* There may be a kind of tutorial designed for a particular application.
	* A tutorial includes tasks and questions to answer.
	*/
	public String pathToTutorial = null;
	/**
	* The specifications of the "datapipe", if given for the application.
	* A "datapipe" is an external data source from which the system may get
	* new objects to be added to some map layer.
	* Each element of the vector is an instance of the class
	* @see spade.vis.spec.DataPipeSpec
	*/
	public Vector dataPipeSpecs = null;
	/**
	* This variable stores the error message. An error may occur when the
	* application description is stored in a file.
	*/
	protected String err = null;
	/**
	 * The URL[user/pass] for Connction to Oracle Spatial for Spatial Analysis
	 */
	protected String oraSpaConn = null;
	/**
	* Used in the variant of the system supporting object selection (possibly,
	* with sending selected objects to an external HTML form through JavaScript
	* functions)
	*/
	public String selectObjectsIn = null;

	protected float minScaleDenominator = Float.NaN;

	/**
	* Adds a new specification to the vector of data specifications
	*/
	public void addDataSpecification(DataSourceSpec spec) {
		if (dataSpecs == null) {
			dataSpecs = new Vector(20, 10);
		}
		dataSpecs.addElement(spec);

	}

	/**
	* Checks among the data specifications for layers coming from the same source,
	* e.g., a GML file, which may contain multiple layers. If finds such cases,
	* creates a common data source specification for these layers.
	*/
	public void findCommonSourceData() {
		if (dataSpecs == null || dataSpecs.size() < 2)
			return;
		for (int i = 0; i < dataSpecs.size() - 1; i++) {
			DataSourceSpec dss = (DataSourceSpec) dataSpecs.elementAt(i);
			if (dss.source == null || dss.typeName == null) {
				continue;
			}
			int idx = -1;
			for (int j = i + 1; j < dataSpecs.size() && idx < 0; j++) {
				DataSourceSpec dss1 = (DataSourceSpec) dataSpecs.elementAt(j);
				if (dss1.typeName != null && dss.source.equals(dss1.source)) {
					idx = j;
				}
			}
			if (idx >= 0) {
				DataSourceSpec upsp = (DataSourceSpec) dss.clone();
				upsp.typeName = null;
				upsp.drawParm = null;
				upsp.layersToLoad = new Vector(10, 5);
				upsp.layersToLoad.addElement(dss);
				upsp.layersToLoad.addElement(dataSpecs.elementAt(idx));
				dataSpecs.removeElementAt(i);
				dataSpecs.insertElementAt(upsp, i);
				dataSpecs.removeElementAt(idx);
				for (int j = dataSpecs.size() - 1; j >= idx; j--) {
					DataSourceSpec dss1 = (DataSourceSpec) dataSpecs.elementAt(j);
					if (dss1.typeName != null && upsp.source.equals(dss1.source)) {
						upsp.layersToLoad.addElement(dss1);
						dataSpecs.removeElementAt(j);
					}
				}
			}
		}
	}

	/**
	* Returns the number of data specifications available
	*/
	public int getDataSpecCount() {
		if (dataSpecs == null)
			return 0;
		return dataSpecs.size();
	}

	/**
	* Returns the data specification with the given index
	*/
	public DataSourceSpec getDataSpecification(int idx) {
		if (idx < 0 || idx >= getDataSpecCount())
			return null;
		DataSourceSpec spec = (DataSourceSpec) dataSpecs.elementAt(idx);
		if (spec != null && spec.server == null && dataServerURL != null) {
			spec.server = dataServerURL;
		}
		return spec;
	}

	/**
	* Writes the description of the application in the specified file
	*/
	public boolean writeToFile(String dir, String fname) {
		if (fname == null) {
			//following text:"No file name specified!"
			err = res.getString("No_file_name");
			return false;
		}
		DataOutputStream dos = null;
		try {
			dos = new DataOutputStream(new FileOutputStream(dir + fname));
		} catch (IOException ioe) {
			err = res.getString("Cannot_open_the_file_") + ioe.toString();
			return false;
		}
		if (dos == null) {
			//following text:"Could not open the file "
			//following text:" for writing"
			err = res.getString("Could_not_open_the") + dir + fname + res.getString("for_writing");
			return false;
		}
		if (dir != null) {
			dir = dir.replace('\\', '/');
		}
		try {
			if (applName != null) {
				dos.writeBytes("APPL_NAME \"" + applName + "\"\r\n");
			}
			if (terrName != null) {
				dos.writeBytes("TERR_NAME \"" + terrName + "\"\r\n");
			}
			if (partOfAttrName != null) {
				dos.writeBytes("PART_OF_ATTR_NAME \"" + partOfAttrName + "\"\r\n");
			}
			if (levelDownAttrName != null) {
				dos.writeBytes("LEVEL_DOWN_ATTR_NAME \"" + levelDownAttrName + "\"\r\n");
			}
			if (dataServerURL != null) {
				dos.writeBytes("DATA_SERVER \"" + dataServerURL + "\"\r\n");
			}
			/*
			if (pathToTaskKB!=null) dos.writeBytes("TASK_KBASE \""+pathToTaskKB+"\"\r\n");
			if (pathToTutorial!=null) dos.writeBytes("TUTORIAL \""+pathToTutorial+"\"\r\n");
			*/
			if (extent != null) {
				dos.writeBytes("TERR_EXTENT (" + extent[0] + "," + extent[1] + "," + extent[2] + "," + extent[3] + ")\r\n");
			}
			if (fullExtent != null) {
				dos.writeBytes("FULL_EXTENT (" + fullExtent[0] + "," + fullExtent[1] + "," + fullExtent[2] + "," + fullExtent[3] + ")\r\n");
			}
			if (user_factor != 1.0f) {
				dos.writeBytes("USER_FACTOR " + user_factor + "\r\n");
			}
			if (user_unit != null) {
				dos.writeBytes("USER_UNIT \"" + user_unit + "\"\r\n");
			}
			if (coordsAreGeographic >= 0) {
				dos.writeBytes("HAS_GEO_COORD " + ((coordsAreGeographic > 0) ? "+" : "-") + "\r\n");
			}
			if (pathOSMTilesIndex != null) {
				String path = makeRelative(pathOSMTilesIndex, dir);
				dos.writeBytes("MAP_TILE_INDEX_FILE \"" + path + "\"\r\n");
			}
//ID
			dos.writeBytes("SHOW_LEGEND " + ((show_legend) ? "+" : "-") + "\r\n");
			dos.writeBytes("SHOW_LEGEND_SIZE " + percent_of_legend + "\r\n");
			dos.writeBytes("SHOW_LEGEND_TERRNAME " + ((show_terrname) ? "+" : "-") + "\r\n");
			dos.writeBytes("SHOW_LEGEND_BGCOLOR " + ((show_bgcolor) ? "+" : "-") + "\r\n");
			dos.writeBytes("SHOW_LEGEND_SCALE " + ((show_scale) ? "+" : "-") + "\r\n");
			dos.writeBytes("SHOW_LEGEND_NOBJECTS " + ((show_nobjects) ? "+" : "-") + "\r\n");
			dos.writeBytes("SHOW_RECORD_PERSISTENT " + ((show_persistent) ? "+" : "-") + "\r\n");
			dos.writeBytes("SHOW_RECORD_TOOLTIP " + ((show_tooltip) ? "+" : "-") + "\r\n");
//~ID
			dos.writeBytes("SHOW_MANIPULATOR " + ((show_manipulator) ? "+" : "-") + "\r\n");
			dos.writeBytes("SHOW_MANIPULATOR_SIZE " + percent_of_manipulator + "\r\n");
			if (applBgColor != null) {
				dos.writeBytes("APPL_BGCOLOR (" + applBgColor.getRed() + "," + applBgColor.getGreen() + "," + applBgColor.getBlue() + ")\r\n");
			}
			if (oraSpaConn != null) {
				dos.writeBytes("ORACLE_SPATIAL_CONNECTION " + oraSpaConn + "\r\n");
			}
			if (!Float.isNaN(minScaleDenominator)) {
				dos.writeBytes("MIN_SCALE_DENOMINATOR " + minScaleDenominator + "\r\n");
			}

			for (int i = 0; i < getDataSpecCount(); i++) {
				DataSourceSpec spec = getDataSpecification(i);
				if (spec.source == null) {
					continue;
				}
				String key = "TABLEDATA";
				if (!spec.toBuildMapLayer && (spec.geoFieldName != null || spec.objType != 0 || spec.drawParm != null || (spec.xCoordFieldName != null || spec.yCoordFieldName != null))) {
					key = "LAYER";
				}
				String source = spec.source;
				boolean database = spec.format != null && (spec.format.equalsIgnoreCase("JDBC") || spec.format.equalsIgnoreCase("ODBC") || spec.format.equalsIgnoreCase("ORACLE") || spec.format.equalsIgnoreCase("ArcSDE"));
				if (database) {
					source = "SOURCE=DB";
				} else {
					source = makeRelative(source, dir);
				}
				dos.writeBytes(key + " \"" + source + "\" \"" + spec.name + "\"\r\n");
				if (spec.typeName != null) {
					dos.writeBytes("TYPENAME \"" + spec.typeName + "\"\r\n");
				}
				if (spec.bounds != null && spec.bounds.size() > 0) {
					RealRectangle r = (RealRectangle) spec.bounds.elementAt(0);
					if (r != null) {
						dos.writeBytes("BOUNDS (" + r.rx1 + "," + r.ry1 + "," + r.rx2 + "," + r.ry2 + ")\r\n");
					}
				}
				if (spec.objDescrSource != null) {
					dos.writeBytes("OBJECT_INFO \"" + makeRelative(spec.objDescrSource, dir) + "\"\r\n");
				}
				if (spec.format != null) {
					dos.writeBytes("FORMAT \"" + spec.format.toUpperCase() + "\"\r\n");
				}
				if (spec.delimiter != null)
					if (spec.delimiter.startsWith("\t")) {
						dos.writeBytes("DELIMITER \"TAB\"\r\n");
					} else {
						dos.writeBytes("DELIMITER \"" + spec.delimiter + "\"\r\n");
					}
				if (spec.server != null && !spec.server.equals(dataServerURL)) {
					dos.writeBytes("SERVER \"" + spec.server + "\"\r\n");
				}
				if (spec.url != null) {
					dos.writeBytes("URL \"" + spec.url + "\"\r\n");
				}
				if (database) {
					dos.writeBytes("TABLE \"" + spec.source + "\"\r\n");
				}
				if (spec.user != null) {
					StringBuffer sb = new StringBuffer(spec.user.length());
					for (int j = spec.user.length() - 1; j >= 0; j--) {
						sb.append((char) (spec.user.charAt(j) - 7));
					}
					dos.writeBytes("USER \"" + sb.toString() + "\"\r\n");
				}
				if (spec.password != null) {
					StringBuffer sb = new StringBuffer(spec.password.length());
					for (int j = spec.password.length() - 1; j >= 0; j--) {
						sb.append((char) (spec.password.charAt(j) - 8));
					}
					dos.writeBytes("KEY \"" + sb.toString() + "\"\r\n");
				}
				if (spec.nRowWithFieldNames >= 0) {
					dos.writeBytes("FIELD_NAMES_IN_ROW " + (spec.nRowWithFieldNames + 1) + "\r\n");
				}
				if (spec.nRowWithFieldTypes >= 0) {
					dos.writeBytes("FIELD_TYPES_IN_ROW " + (spec.nRowWithFieldTypes + 1) + "\r\n");
				}
				if (spec.idFieldName != null) {
					dos.writeBytes("ID_FIELD \"" + spec.idFieldName + "\"\r\n");
				} else if (spec.idFieldN >= 0) {
					dos.writeBytes("ID_FIELD " + (spec.idFieldN + 1) + "\r\n");
				}
				if (spec.nameFieldName != null) {
					dos.writeBytes("NAME_FIELD \"" + spec.nameFieldName + "\"\r\n");
				} else if (spec.nameFieldN >= 0) {
					dos.writeBytes("NAME_FIELD " + (spec.nameFieldN + 1) + "\r\n");
				}
				if (spec.multipleRowsPerObject) {
					dos.writeBytes("MULTIPLE_ROWS_PER_OBJECT +\r\n");
				}
				if (spec.geoFieldName != null) {
					dos.writeBytes("GEO_FIELD \"" + spec.geoFieldName + "\"\r\n");
				}
				if (spec.xCoordFieldName != null) {
					dos.writeBytes("X_FIELD \"" + spec.xCoordFieldName + "\"\r\n");
				}
				if (spec.yCoordFieldName != null) {
					dos.writeBytes("Y_FIELD \"" + spec.yCoordFieldName + "\"\r\n");
				}
				if (spec.radiusFieldName != null) {
					dos.writeBytes("RADIUS_FIELD \"" + spec.radiusFieldName + "\"\r\n");
				}
				if (spec.columns != null && spec.columns.size() > 0) {
					int idx = -1;
					if (spec.idFieldName != null && (idx = spec.columns.indexOf(spec.idFieldName)) >= 0) {
						spec.columns.removeElementAt(idx);
					}
					if (spec.nameFieldName != null && (idx = spec.columns.indexOf(spec.nameFieldName)) >= 0) {
						spec.columns.removeElementAt(idx);
					}
					if (spec.geoFieldName != null && (idx = spec.columns.indexOf(spec.geoFieldName)) >= 0) {
						spec.columns.removeElementAt(idx);
					}
					if (spec.columns.size() > 0) {
						dos.writeBytes("ATTRIBUTES "); //put each column name in quotes
						for (int j = 0; j < spec.columns.size(); j++) {
							dos.writeBytes(((j > 0) ? "," : "") + "\"" + (String) spec.columns.elementAt(j) + "\"");
						}
						dos.writeBytes("\r\n");
					}
				}
				if (!spec.mayHaveHoles) {
					dos.writeBytes("HOLES -\r\n");
				}
				if (!spec.mayHaveMultiParts) {
					dos.writeBytes("MULTI_PARTS -\r\n");
				}
				if (!spec.idsImportant) {
					dos.writeBytes("IDS_IMPORTANT -\r\n");
				}
				spec.writeDataDescriptors(dos);
				if (spec.layerName != null) {
					dos.writeBytes("LAYER_REF \"" + spec.layerName + "\"\r\n");
				}
				if (spec.useForDecision && spec.decisionInfo != null) {
					DecisionSpec dsp = (DecisionSpec) spec.decisionInfo;
					dos.writeBytes("USE_FOR_DECISION +\r\n");
					if (dsp.decisionType != null) {
						dos.writeBytes("DECISION_TYPE \"" + dsp.decisionType + "\"\r\n");
					}
					if (dsp.resultDir != null) {
						dos.writeBytes("DECISION_RESULT_DIR \"" + dsp.resultDir + "\"\r\n");
					}
					if (dsp.resultFile != null) {
						dos.writeBytes("DECISION_RESULT_FILE \"" + dsp.resultFile + "\"\r\n");
					}
					if (dsp.resultScript != null) {
						dos.writeBytes("DECISION_RESULT_SCRIPT \"" + dsp.resultScript + "\"\r\n");
					}
				}
				if (spec.objType != 0 && spec.objType != Geometry.undefined) {
					dos.writeBytes("TYPE " + getStringForGeometryType(spec.objType) + "\r\n");
				}
				if (spec.objSubType != 0 && spec.objSubType != Geometry.undefined) {
					dos.writeBytes("SUBTYPE " + getStringForGeometryType(spec.objSubType) + "\r\n");
				}
//ID
				if (!Float.isInfinite(spec.validMin) || !Float.isInfinite(spec.validMax)) {
					dos.writeBytes("VALID_RANGE " + Float.toString(spec.validMin) + " " + Float.toString(spec.validMax) + "\r\n");
				}
//~ID
//ID
				if (!(spec.gridAttribute.length() == 0)) {
					dos.writeBytes("ATTRIBUTE \"" + spec.gridAttribute + "\"\r\n");
				}
				if (spec.gridParameterNames != null && spec.gridParameterValues != null && spec.gridParameterNames.size() > 0 && spec.gridParameterValues.size() > 0 && spec.gridParameterNames.size() == spec.gridParameterValues.size()) {
					for (int j = 0; j < spec.gridParameterNames.size(); j++)
						if (spec.gridParameterValues.elementAt(j) instanceof TimeCount) {
							dos.writeBytes("TIME_PARAMETER \"" + spec.gridParameterNames.elementAt(j) + "\" VALUE \"" + spec.gridParameterValues.elementAt(j).toString() + "\"\r\n");
						} else if (spec.gridParameterValues.elementAt(j) instanceof Date) {
							dos.writeBytes("TIME_PARAMETER \"" + spec.gridParameterNames.elementAt(j) + "\" VALUE \"" + spec.gridParameterValues.elementAt(j).toString() + "\" SCHEME \"" + ((Date) spec.gridParameterValues.elementAt(j)).scheme
									+ "\"\r\n");
						} else {
							dos.writeBytes("PARAMETER \"" + spec.gridParameterNames.elementAt(j) + "\" VALUE \"" + spec.gridParameterValues.elementAt(j) + "\"\r\n");
						}
				}
//~ID
				if (spec.toBuildMapLayer) {
					dos.writeBytes("BUILD_MAP_LAYER +\r\n");
				}
				if (spec.drawParm != null) { //drawing parameters of the layer
					DrawingParameters dp = (DrawingParameters) spec.drawParm;
					if (dp.maxLevels >= 0) {
						dos.writeBytes("MAX_LEVELS " + dp.maxLevels + "\r\n");
					}
					dos.writeBytes("DRAWING " + ((dp.drawLayer) ? "+" : "-") + "\r\n");
					if (dp.drawCondition && (!Float.isNaN(dp.minScaleDC) || !Float.isNaN(dp.maxScaleDC))) {
						dos.writeBytes("CONDITIONAL_DRAWING +(" + dp.minScaleDC + "," + dp.maxScaleDC + ")\r\n");
					}
					if (spec.objType == Geometry.area) {
						dos.writeBytes("HOLES_DRAWING " + (dp.drawHoles ? "+" : "-") + "\r\n");
					}
					dos.writeBytes("ALLOW_SPATIAL_FILTER " + ((dp.allowSpatialFilter) ? "+" : "-") + "\r\n");

					if (spec.objType != Geometry.image && spec.objType != Geometry.raster) {
						if (spec.objType == Geometry.area) {
							if (dp.transparency != -1) {
								dos.writeBytes("TRANSPARENCY " + dp.transparency + "\r\n");
							}
						} else {
							dos.writeBytes("TRANSPARENCY " + ((dp.isTransparent) ? "+" : "-") + "\r\n");
						}
						dos.writeBytes("BORDERS " + ((dp.drawBorders) ? "+" : "-") + "\r\n");
						dos.writeBytes("BORDERW " + dp.lineWidth + "\r\n");
						dos.writeBytes("HLIGHTEDW " + dp.hlWidth + "\r\n");
						dos.writeBytes("SELECTEDW " + dp.selWidth + "\r\n");
						dos.writeBytes("BORDERCOLOR (" + dp.lineColor.getRed() + "," + dp.lineColor.getGreen() + "," + dp.lineColor.getBlue() + ")\r\n");
						if (spec.objType != Geometry.line) {
							dos.writeBytes("BACKGROUND  (" + dp.fillColor.getRed() + "," + dp.fillColor.getGreen() + "," + dp.fillColor.getBlue() + ")\r\n");
							if (dp.fillContours) {
								dos.writeBytes("HATCH_STYLE 1\r\n");
							} else {
								dos.writeBytes("HATCH_STYLE 0\r\n");
							}
						}
						if (dp.hlDrawCircles) {
							dos.writeBytes("HL_CIRCLES +\r\n");
							dos.writeBytes("HL_CIRCLE_SIZE " + dp.hlCircleSize + "\r\n");
							dos.writeBytes("HL_CIRCLE_COLOR (" + dp.hlCircleColor.getRed() + "," + dp.hlCircleColor.getGreen() + "," + dp.hlCircleColor.getBlue() + ")\r\n");
						}
						if (dp.drawLabels) {
							dos.writeBytes("LABELS +\r\n");
							dos.writeBytes("LABELCOLOR (" + dp.labelColor.getRed() + "," + dp.labelColor.getGreen() + "," + dp.labelColor.getBlue() + ")\r\n");
							if (dp.fontName != null) {
								dos.writeBytes("LABELFONT \"" + dp.fontName + "\"," + dp.fontStyle + "," + dp.fontSize + "," + dp.labelStyle + "\r\n");
							}
						}
					} else if (spec.objType == Geometry.raster) {
						if (dp.transparency != -1) {
							dos.writeBytes("TRANSPARENCY " + ((dp.transparency > 100 || dp.transparency < 0) ? "0" : Integer.toString(dp.transparency)) + "\r\n");
						}
						if (dp.colorScale != "") {
							dos.writeBytes("COLOR_SCALE " + dp.colorScale + "\r\n");
						}
						if (dp.csParameters != "") {
							dos.writeBytes("CS_PARAMETERS \"" + ((CopyFile.checkExistence(dp.csParameters)) ? makeRelative(dp.csParameters, dir) : dp.csParameters) + "\"\r\n");
						}
					}

				}
				dos.flush();
			}
			if (dataPipeSpecs != null) {
				for (int i = 0; i < dataPipeSpecs.size(); i++) {
					DataPipeSpec spec = (DataPipeSpec) dataPipeSpecs.elementAt(i);
					dos.writeBytes("DATAPIPE\r\n");
					if (spec.dataSource != null) {
						dos.writeBytes("SOURCE \"" + spec.dataSource + "\"\r\n");
					}
					//check if the layer identifier is arbitrary and does not refer to
					//any actual layer
					boolean found = false;
					for (int j = 0; j < getDataSpecCount() && !found; j++) {
						DataSourceSpec dss = getDataSpecification(j);
						if (dss.source == null) {
							continue;
						}
						found = dss.source.endsWith(spec.layerId);
					}
					String layerId = (found) ? spec.layerId : null;
					if (!found && spec.tableFileName != null) {
						for (int j = 0; j < getDataSpecCount() && layerId == null; j++) {
							DataSourceSpec dss = getDataSpecification(j);
							if (dss.source == null) {
								continue;
							}
							if (CopyFile.sameFiles(spec.tableFileName, dss.source))
								if (dss.layerName != null) {
									layerId = dss.layerName;
								} else {
									layerId = dss.source;
								}
						}
						if (layerId == null) {
							layerId = spec.tableFileName;
						}
						layerId = makeRelative(layerId, dir);
					}
					if (layerId != null) {
						dos.writeBytes("ADD_TO_LAYER \"" + layerId + "\"\r\n");
					}
					if (spec.updater != null) {
						dos.writeBytes("UPDATER \"" + spec.updater + "\"\r\n");
					}
					if (spec.tableFileName != null) {
						dos.writeBytes("WRITE_UPDATES_TO \"" + spec.tableFileName + "\"\r\n");
					}
					if (spec.delimiter != null)
						if (spec.delimiter.startsWith("\t")) {
							dos.writeBytes("DELIMITER \"TAB\"\r\n");
						} else {
							dos.writeBytes("DELIMITER \"" + spec.delimiter + "\"\r\n");
						}
					dos.writeBytes("/DATAPIPE\r\n");
				}
			}
//ID
			if (about_project != null && about_project != "") {
				dos.writeBytes("ABOUT_PROJECT\r\n");
				dos.writeBytes(about_project);
				dos.writeBytes("\r\n/ABOUT_PROJECT\r\n");
			}
//~ID
		} catch (IOException ioe) {
			err = res.getString("Writing_error_") + ioe.toString();
			return false;
		}
		try {
			dos.close();
		} catch (IOException ioe) {
		}
		return true;
	}

	/**
	* If the specified path begins with the specified directory, makes a relative
	* path by removing the directory from the path
	*/
	protected String makeRelative(String path, String dir) {
		if (path == null)
			return null;
		path = path.replace('\\', '/');
		if (dir != null && path.startsWith(dir)) { //transform to a relative path
			path = path.substring(dir.length());
			if (path.charAt(0) == '/') {
				path = path.substring(1);
			}
		}
		return path;
	}

	/**
	 * Returns a string for a given object geometry type as defined in the class
	 * Geometry
	 */
	public static String getStringForGeometryType(char type) {
		if (type == 0 || type == Geometry.undefined)
			return "UNDEFINED";
		if (type == Geometry.area)
			return "AREA";
		if (type == Geometry.line)
			return "LINE";
		if (type == Geometry.point)
			return "POINT";
		if (type == Geometry.image)
			return "IMAGE";
		if (type == Geometry.raster)
			return "RASTER";
		if (type == Geometry.circle)
			return "CIRCLE";
		if (type == Geometry.link)
			return "LINK";
		if (type == Geometry.movement)
			return "MOVEMENT";
		if (type == Geometry.vector)
			return "VECTOR";
		if (type == Geometry.rectangle)
			return "RECTANGLE";
		return "UNDEFINED";
	}

	/**
	 * For a given string returns the corresponding bject geometry type as defined
	 * in the class Geometry
	 */
	public static char getGeometryTypeForString(String str) {
		if (str == null)
			return Geometry.undefined;
		str = str.trim();
		if (str.length() < 1)
			return Geometry.undefined;
		str = str.toUpperCase();
		if (str.equals("AREA"))
			return Geometry.area;
		if (str.equals("LINE"))
			return Geometry.line;
		if (str.equals("POINT"))
			return Geometry.point;
		if (str.equals("IMAGE"))
			return Geometry.image;
		if (str.equals("RASTER"))
			return Geometry.raster;
		if (str.equals("CIRCLE"))
			return Geometry.circle;
		if (str.equals("LINK"))
			return Geometry.link;
		if (str.equals("MOVEMENT"))
			return Geometry.movement;
		if (str.equals("VECTOR"))
			return Geometry.vector;
		if (str.equals("RECTANGLE"))
			return Geometry.rectangle;
		return Geometry.undefined;
	}

	/**
	* Returns the error message
	*/
	public String getErrorMessage() {
		return err;
	}
}
