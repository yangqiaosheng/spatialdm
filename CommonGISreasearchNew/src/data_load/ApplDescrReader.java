package data_load;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.lib.util.CopyFile;
import spade.lib.util.Parameters;
import spade.lib.util.ProcessListener;
import spade.lib.util.ReadUtil;
import spade.lib.util.StringUtil;
import spade.time.Date;
import spade.time.TimeCount;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.RealRectangle;
import spade.vis.spec.DataPipeSpec;
import spade.vis.spec.DataSourceSpec;
import spade.vis.spec.DecisionSpec;
import spade.vis.spec.TagReaderFactory;

/**
* Reads an application description (list of applDescr layers and their drawing
* parameters plus, possibly, list of tables) from a *.app file
*/

public class ApplDescrReader {
	/**
	* The listener of the status of reading
	*/
	protected ProcessListener plist = null;
	protected String processName = null;

	/**
	* Sets the listener of the reading status
	*/
	public void setProcessListener(ProcessListener list) {
		plist = list;
	}

	/**
	* Opens the stream on the specified data source. If the source
	* string starts with HTTP or FILE, accesses the source as a URL, otherwise
	* opens it as a local file
	*/
	protected InputStream openStream(String dataSource) {
		if (dataSource == null)
			return null;
		int idx = dataSource.indexOf(':');
		boolean isURL = false;
		InputStream stream = null;
		if (idx > 0) {
			String pref = dataSource.substring(0, idx);
			if (pref.equalsIgnoreCase("HTTP") || pref.equalsIgnoreCase("FILE")) {
				isURL = true;
			}
		}
		try {
			if (isURL) { //try to access the source as a URL
				URL url = new URL(dataSource);
				stream = url.openStream();
			} else {
				stream = new FileInputStream(dataSource);
			}
		} catch (IOException ioe) {
			notifyProcessState("Error accessing " + dataSource + ": " + ioe, true);
			return null;
		}
		return stream;
	}

	protected void closeStream(InputStream stream) {
		if (stream == null)
			return;
		try {
			stream.close();
		} catch (IOException ioe) {
		}
	}

	/**
	* Notifies the process listener, if exists, about the status of the process of
	* reading. If "trouble" is true, then this is an error message.
	*/
	public void notifyProcessState(String processState, boolean trouble) {
		if (plist != null) {
			plist.receiveNotification(this, processName, processState, trouble);
		} else {
			System.out.println(processName + ": " + processState);
		}
	}

	/**
	* Reads an application description from a *.app file specified by the argument
	* dataSource.
	*/
	public ApplData readMapDescription(String dataSource, ProcessListener plist, Parameters parm) {
		processName = "Reading " + dataSource;
		this.plist = plist;
		InputStream stream = openStream(dataSource);
		if (stream == null)
			return null;
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		boolean firstLine = true;
		String str = null;
		DataSourceSpec spec = null;
		DrawingParameters dparm = null;
		DecisionSpec deciSp = null;
		int langN = -1;
		//used to transform relative paths into absolute
		String dir = CopyFile.getDir(dataSource);
		ApplData applDescr = new ApplData();
		while (true) {
			try {
				if (firstLine) {
					str = reader.readLine();
					firstLine = false;
					if (str.startsWith("*")) {
						Vector v = ReadUtil.getLanguages(str);
						if (v != null && v.size() > 0) {
							String clang = "ENGLISH";
							for (int i = 0; i < v.size() && langN < 0; i++)
								if (clang.equalsIgnoreCase((String) v.elementAt(i))) {
									langN = i;
								}
						}
						if (langN < 0) {
							langN = 0;
						}
						continue;
					}
					if (langN < 0) {
						langN = 0;
					}
				} else {
					str = reader.readLine();
				}
			} catch (EOFException eof) {
				break;
			} catch (IOException ioe) {
				notifyProcessState("Error: " + ioe, true);
				return null;
			}
			if (str == null) {
				break;
			}
			if (str.startsWith("*")) {
				continue; //this is a comment
			}
			str = str.trim();
			if (str.length() < 1) {
				continue; //empty line
			}

//ID
			if (str.indexOf('%') > 0 && parm != null) {
				System.out.println("Source string: <" + str + ">");
				StringBuffer sb = new StringBuffer();
				int curr = 0, idx;

				while ((idx = str.indexOf('%', curr)) > 0) {
					sb.append(str.substring(curr, idx));
					curr = idx;
					idx++;
					while (idx < str.length() && Character.isDigit(str.charAt(idx))) {
						idx++;
					}
					String key = str.substring(curr, idx);
					Object val = parm.getParameter(key);
					if (val == null) {
						sb.append(key);
					} else {
						sb.append(val);
					}
					curr = idx;
				}
				sb.append(str.substring(curr, str.length()));

				str = sb.toString();
				System.out.println("Transformed into <" + str + ">");
			}
//~ID

			if (str.startsWith("<")) { //this is a "tag"
				if (spec != null) {
					try {
						spec.readTag(str, reader);
					} catch (EOFException eof) {
						break;
					} catch (IOException ioe) {
						notifyProcessState("Error: " + ioe, true);
						return null;
					}
				} else {
					try {
						TagReaderFactory.skipTag(str, reader);
					} catch (IOException ioe) {
						notifyProcessState("Error: " + ioe, true);
						return null;
					}
				}
				continue;
			}
			Vector tokens = StringUtil.getNames(str, " ;");
			if (tokens == null || tokens.size() < 1) {
				continue;
			}
			String keyWord = ((String) tokens.elementAt(0)).toUpperCase();
			if (keyWord.equals("DATAPIPE")) {
				try {
					DataPipeSpec dps = readDataPipeSpec(reader);
					if (dps != null) {
						if (applDescr.dataPipeSpecs == null) {
							applDescr.dataPipeSpecs = new Vector(5, 5);
						}
						applDescr.dataPipeSpecs.addElement(dps);
					}
					continue;
				} catch (EOFException eof) {
					break;
				} catch (IOException ioe) {
					notifyProcessState("Error: " + ioe, true);
					break;
				}
			}
//ID
			if (keyWord.equals("ABOUT_PROJECT")) {
				try {
					applDescr.about_project = readProjectDescr(reader);
					if (applDescr.about_project == null) {
						applDescr.about_project = "";
					}
					continue;
				} catch (EOFException eof) {
					break;
				} catch (IOException ioe) {
					notifyProcessState("Error: " + ioe, true);
					break;
				}
			}
//~ID
			String val = null;
			if (keyWord.equals("KEY") || keyWord.equals("ATTRIBUTES") || keyWord.equals("LABELFONT")) //can contain quotes and punctuation
			{
				val = str.substring(keyWord.length() + 1).trim();
				if (!keyWord.equals("ATTRIBUTES") && val.charAt(0) == '\"' && val.charAt(val.length() - 1) == '\"') {
					val = val.substring(1, val.length() - 1).trim();
				}
			} else if (keyWord.equals("DELIMITER")) {//can contain blanks, quotes, and punctuation
				val = str.substring(keyWord.length() + 1);
				int i1 = val.indexOf('\"');
				if (i1 >= 0) {
					int i2 = val.indexOf('\"', i1 + 1);
					if (i2 - i1 > 1) {
						val = val.substring(i1 + 1, i2);
					}
				}
				String v = val.trim();
				if (v.length() > 0) {
					val = v;
				}
			} else {
				if (tokens.size() < 2) {
					continue;
				}
				if (langN + 1 < tokens.size() && (keyWord.equals("TERR_NAME") || keyWord.equals("APPL_NAME") || keyWord.equals("USER_UNIT"))) {
					val = (String) tokens.elementAt(langN + 1);
				} else {
					val = (String) tokens.elementAt(1);
				}
			}
			//Now, we have divided the line into the keyword and the value
			//and will analyse the keyword in order to set the right fields in spec
			//or in dparm
			if (keyWord.equals("APPL_NAME")) {
				applDescr.applName = val;
				continue;
			}
			if (keyWord.equals("TERR_NAME")) {
				applDescr.terrName = val;
				continue;
			}
			if (keyWord.equals("MAP_TILE_INDEX_FILE")) {
				if (dir != null && !CopyFile.isAbsolutePath(val)) {
					val = dir + val;
				}
				applDescr.pathOSMTilesIndex = val;
				continue;
			}
			if (keyWord.equals("OBJECT_SELECTION_IN")) {
				applDescr.selectObjectsIn = val;
				continue;
			}
			if (keyWord.equals("PART_OF_ATTR_NAME") || keyWord.equals("LEVEL_UP_ATTR_NAME")) {
				applDescr.partOfAttrName = val;
				continue;
			}
			if (keyWord.equals("LEVEL_DOWN_ATTR_NAME")) {
				applDescr.levelDownAttrName = val;
				continue;
			}
			if (keyWord.equals("DATA_SERVER")) {
				applDescr.dataServerURL = val;
				continue;
			}
			if (keyWord.equals("TASK_KBASE")) {
				applDescr.pathToTaskKB = val;
				continue;
			}
			if (keyWord.equals("TUTORIAL")) {
				applDescr.pathToTutorial = val;
				continue;
			}
			if (keyWord.equals("TERR_EXTENT") || //the initially visible territory extent
					keyWord.equals("FULL_EXTENT")) { //the full territory extent
				StringTokenizer stt = new StringTokenizer(val, " (,);");
				float f[] = new float[4];
				boolean gotAll = false;
				for (int i = 0; i < 4 && stt.hasMoreTokens(); i++) {
					try {
						f[i] = Float.valueOf(stt.nextToken()).floatValue();
						if (i == 3) {
							gotAll = true;
						}
					} catch (NumberFormatException ne) {
						break;
					}
				}
				if (gotAll)
					if (keyWord.equals("TERR_EXTENT")) {
						applDescr.extent = f;
					} else {
						applDescr.fullExtent = f;
					}
				continue;
			}
			if (keyWord.equals("USER_FACTOR")) {
				try {
					applDescr.user_factor = Float.valueOf(val).floatValue();
				} catch (NumberFormatException nfe) {
					applDescr.user_factor = 1.0f;
				}
				continue;
			}
			if (keyWord.equals("USER_UNIT")) {
				applDescr.user_unit = val;
				continue;
			}
			if (keyWord.equals("HAS_GEO_COORD")) {
				applDescr.coordsAreGeographic = (val.startsWith("+")) ? 1 : 0;
				continue;
			}
//ID
			if (keyWord.equals("SHOW_LEGEND")) {
				applDescr.show_legend = !val.startsWith("-");
				continue;
			}
			if (keyWord.equals("SHOW_LEGEND_TERRNAME")) {
				applDescr.show_terrname = !val.startsWith("-");
				continue;
			}
			if (keyWord.equals("SHOW_LEGEND_BGCOLOR")) {
				applDescr.show_bgcolor = !val.startsWith("-");
				continue;
			}
			if (keyWord.equals("SHOW_LEGEND_SCALE")) {
				applDescr.show_scale = !val.startsWith("-");
				continue;
			}
			if (keyWord.equals("SHOW_LEGEND_NOBJECTS")) {
				applDescr.show_nobjects = !val.startsWith("-");
				continue;
			}
			if (keyWord.equals("SHOW_RECORD_PERSISTENT")) {
				applDescr.show_persistent = !val.startsWith("-");
				continue;
			}
			if (keyWord.equals("SHOW_RECORD_TOOLTIP")) {
				applDescr.show_tooltip = !val.startsWith("-");
				continue;
			}
			if (keyWord.equals("SHOW_LEGEND_SIZE")) {
				try {
					applDescr.percent_of_legend = Integer.valueOf(val).intValue();
				} catch (NumberFormatException nfe) {
					applDescr.percent_of_legend = 30;
				}
				if (applDescr.percent_of_legend < 0) {
					applDescr.percent_of_legend = 0;
				}
				if (applDescr.percent_of_legend > 100) {
					applDescr.percent_of_legend = 100;
				}
				continue;
			}
//~ID
// P.G.
			if (keyWord.equals("SHOW_MANIPULATOR")) {
				applDescr.show_manipulator = !val.startsWith("-");
				continue;
			}
			if (keyWord.equals("SHOW_MANIPULATOR_SIZE")) {
				try {
					applDescr.percent_of_manipulator = Integer.valueOf(val).intValue();
				} catch (NumberFormatException nfe) {
					applDescr.percent_of_manipulator = 30;
				}
				if (applDescr.percent_of_manipulator < 0) {
					applDescr.percent_of_manipulator = 0;
				}
				if (applDescr.percent_of_manipulator > 100) {
					applDescr.percent_of_manipulator = 100;
				}
				continue;
			}
			if (keyWord.equals("ORACLE_SPATIAL_CONNECT")) {
				applDescr.oraSpaConn = val;
				continue;
			}
			if (keyWord.equals("MIN_SCALE_DENOMINATOR")) {
				try {
					applDescr.minScaleDenominator = Float.valueOf(val).floatValue();
				} catch (NumberFormatException nfe) {
					applDescr.minScaleDenominator = Float.NaN;
				}
				continue;
			}
			if (keyWord.equals("APPL_BGCOLOR")) {
				int r = 0, g = 0, b = 0;
				StringTokenizer stt = new StringTokenizer(val, " (,);");
				for (int i = 0; i < 3 && stt.hasMoreTokens(); i++) {
					try {
						int n = Integer.valueOf(stt.nextToken()).intValue();
						if (i == 0) {
							r = n;
						} else if (i == 1) {
							g = n;
						} else {
							b = n;
						}
					} catch (NumberFormatException ne) {
					}
				}
				applDescr.applBgColor = new Color(r, g, b);
				continue;
			}
// ~P.G.
			if (keyWord.equals("LAYER") || keyWord.equals("TABLEDATA")) {
				if (spec != null && spec.source != null) {
					applDescr.addDataSpecification(spec);
				}
				spec = new DataSourceSpec();
				dparm = null;
				deciSp = null;
				if (keyWord.equals("LAYER")) {
					dparm = new DrawingParameters();
					spec.drawParm = dparm;
				}
				if (val.equalsIgnoreCase("SOURCE=DB")) { //the layer is loaded from a database
					spec.format = "JDBC";
					if (applDescr.extent != null) {
						spec.bounds = new Vector(1, 1);
						spec.bounds.addElement(new RealRectangle(applDescr.extent));
					}
				} else {
					//if a relative path to the layer file is specified, transform it
					//into an absolute path using the path specified in dataSource
					String path = val;
					if (dir != null && !CopyFile.isAbsolutePath(path)) {
						path = dir + path;
					}
					String ext = CopyFile.getExtension(val);
					if (ext != null) {
						spec.format = ext;
					}
					spec.source = path;
				}
				if (tokens.size() > 2) {
					if (langN + 2 < tokens.size()) {
						spec.name = (String) tokens.elementAt(langN + 2);
					} else {
						spec.name = (String) tokens.elementAt(2);
					}
				} else {
					spec.name = spec.source;
				}
				continue;
			}
			if (keyWord.equals("FORMAT")) {
				spec.format = val;
				continue;
			}
			if (keyWord.equals("TYPENAME")) {
				spec.typeName = val;
				continue;
			}
			if (keyWord.equals("SERVER")) {
				spec.server = val;
				continue;
			}
			if (keyWord.equals("URL")) {
				spec.url = val;
				continue;
			}
			if (keyWord.equals("TABLE")) {
				spec.source = val;
				if (spec.name == null) {
					spec.name = val;
				}
				continue;
			}
			if (keyWord.equals("DELIMITER")) {
				if (val.equalsIgnoreCase("TAB")) {
					spec.delimiter = "\t";
				} else {
					spec.delimiter = val;
				}
				continue;
			}
			if (keyWord.equals("FIELD_NAMES_IN_ROW")) {
				try {
					spec.nRowWithFieldNames = Integer.valueOf(val).intValue() - 1;
				} catch (NumberFormatException e) {
					notifyProcessState("Illegal number of row with field names for " + spec.source, true);
				}
				continue;
			}
			if (keyWord.equals("FIELD_TYPES_IN_ROW")) {
				try {
					spec.nRowWithFieldTypes = Integer.valueOf(val).intValue() - 1;
				} catch (NumberFormatException e) {
					notifyProcessState("Illegal number of row with field types for " + spec.source, true);
				}
				continue;
			}
			if (keyWord.equals("ID_FIELD") || keyWord.equals("NAME_FIELD")) {
				try {
					int n = Integer.valueOf(val).intValue() - 1;
					//System.out.println("Transformed number: "+keyWord+"="+n);
					if (keyWord.equals("ID_FIELD")) {
						spec.idFieldN = n;
					} else {
						spec.nameFieldN = n;
					}
				} catch (NumberFormatException nfe) {
					if (keyWord.equals("ID_FIELD")) {
						spec.idFieldName = val;
					} else {
						spec.nameFieldName = val;
					}
				}
				continue;
			}
			if (keyWord.equals("ID_FIELD_NAME")) {
				spec.idFieldName = val;
				continue;
			}
			if (keyWord.equals("NAME_FIELD_NAME")) {
				spec.nameFieldName = val;
				continue;
			}
			if (keyWord.equals("MULTIPLE_ROWS_PER_OBJECT")) {
				spec.multipleRowsPerObject = val.startsWith("+");
				continue;
			}
			if (keyWord.equals("GEO_FIELD") || keyWord.equals("GEO_FIELD_NAME")) {
				spec.geoFieldName = val;
				continue;
			}
			if (keyWord.equals("X_FIELD") || keyWord.equals("X_FIELD_NAME")) {
				spec.xCoordFieldName = val;
				continue;
			}
			if (keyWord.equals("Y_FIELD") || keyWord.equals("Y_FIELD_NAME")) {
				spec.yCoordFieldName = val;
				continue;
			}
			if (keyWord.equals("RADIUS_FIELD") || keyWord.equals("RADIUS_FIELD_NAME")) {
				spec.radiusFieldName = val;
				continue;
			}
			if (keyWord.equals("ATTRIBUTES")) {
				spec.setColumns(val);
				continue;
			}
			if (keyWord.equals("KEY") || keyWord.equals("USER")) {
				boolean psw = keyWord.equals("KEY");
				int add = (psw) ? 8 : 7;
				StringBuffer sb = new StringBuffer(val.length());
				for (int j = val.length() - 1; j >= 0; j--) {
					sb.append((char) (val.charAt(j) + add));
				}
				if (psw) {
					spec.password = sb.toString();
				} else {
					spec.user = sb.toString();
				}
				continue;
			}
			if (keyWord.equals("OBJECT_INFO")) {
				spec.objDescrSource = StringUtil.readName(val).trim();
				if (dir != null && !CopyFile.isAbsolutePath(spec.objDescrSource)) {
					spec.objDescrSource = dir + spec.objDescrSource;
				}
				for (int i = 2; i < tokens.size(); i++) {
					String expr = (String) tokens.elementAt(i);
					StringTokenizer stk = new StringTokenizer(expr, " =");
					if (stk.countTokens() > 1) {
						keyWord = stk.nextToken().toUpperCase();
						val = stk.nextToken();
						int iv = -1;
						try {
							iv = Integer.valueOf(val).intValue();
						} catch (NumberFormatException nfe) {
						}
						if (keyWord.equals("ID_FIELD"))
							if (iv >= 0) {
								spec.idFieldN = iv;
							} else {
								spec.idFieldName = val;
							}
						else if (keyWord.equals("NAME_FIELD"))
							if (iv >= 0) {
								spec.nameFieldN = iv;
							} else {
								spec.nameFieldName = val;
							}
					}
				}
				continue;
			}
			if (keyWord.equals("LAYER_REF")) { //the table refers to a layer
				spec.layerName = val;
				continue;
			}
			if (keyWord.equals("HOLES")) {
				spec.mayHaveHoles = val.startsWith("+");
				continue;
			}
			if (keyWord.equals("BUILD_MAP_LAYER")) {
				spec.toBuildMapLayer = val.startsWith("+");
				if (spec.toBuildMapLayer && dparm == null) {
					dparm = new DrawingParameters();
					spec.drawParm = dparm;
				}
				continue;
			}
			if (keyWord.equals("MULTI_PARTS")) {
				spec.mayHaveMultiParts = val.startsWith("+");
				continue;
			}
			if (keyWord.equals("IDS_IMPORTANT")) {
				spec.idsImportant = val.startsWith("+");
				continue;
			}
			if (keyWord.equals("USE_FOR_DECISION")) {//the table is used for decision
				if (val.startsWith("+")) {
					spec.useForDecision = true;
					if (deciSp == null) {
						deciSp = new DecisionSpec();
					}
					spec.decisionInfo = deciSp;
				}
				continue;
			}
			if (keyWord.startsWith("DECISION")) {
				if (deciSp == null) {
					spec.useForDecision = true;
					deciSp = new DecisionSpec();
					spec.decisionInfo = deciSp;
				}
				if (keyWord.equals("DECISION_RESULT_FILE")) {
					deciSp.resultFile = val;
				} else if (keyWord.equals("DECISION_RESULT_DIR")) {
					deciSp.resultDir = val;
				} else if (keyWord.equals("DECISION_RESULT_SCRIPT")) {
					deciSp.resultScript = val;
				} else if (keyWord.equals("DECISION_TYPE")) {
					deciSp.decisionType = val;
				}
				continue;
			}
			if (keyWord.equals("TYPE")) {
				spec.objType = ApplData.getGeometryTypeForString(val);
				continue;
			}
			if (keyWord.equals("SUBTYPE")) {
				spec.objSubType = ApplData.getGeometryTypeForString(val);
				continue;
			}
			if (keyWord.equals("BOUNDS")) { //esp. for an image
				StringTokenizer stt = new StringTokenizer(val, " (,);");
				float f[] = new float[4];
				boolean gotAll = false;
				for (int i = 0; i < 4 && stt.hasMoreTokens(); i++) {
					try {
						f[i] = Float.valueOf(stt.nextToken()).floatValue();
						if (i == 3) {
							gotAll = true;
						}
					} catch (NumberFormatException ne) {
						break;
					}
				}
				if (gotAll) {
					if (spec.bounds == null) {
						spec.bounds = new Vector(1, 1);
					} else {
						spec.bounds.removeAllElements();
					}
					RealRectangle rr = new RealRectangle(f[0], f[1], f[2], f[3]);
					spec.bounds.addElement(rr);
				}
				continue;
			}
			if (dparm != null) { //drawing parameters of the layer
				if (keyWord.equals("DRAWING")) {
					dparm.drawLayer = !val.startsWith("-");
					continue;
				}
				if (keyWord.equals("ALLOW_SPATIAL_FILTER")) {
					dparm.allowSpatialFilter = !val.startsWith("-");
					continue;
				}
				if (keyWord.equals("HOLES_DRAWING")) {
					dparm.drawHoles = val.startsWith("+");
					continue;
				}
				if (keyWord.equals("CONDITIONAL_DRAWING")) {
					dparm.drawCondition = val.startsWith("+");
					if (dparm.drawCondition) {
						StringTokenizer st = new StringTokenizer(val.substring(2), ",()");
						String s = "NaN";
						if (st.hasMoreElements()) {
							s = st.nextToken().trim();
						}
						if (!s.equalsIgnoreCase("NaN")) {
							try {
								dparm.minScaleDC = Float.valueOf(s).floatValue();
							} catch (NumberFormatException nfe) {
							}
						}
						s = "NaN";
						if (st.hasMoreElements()) {
							s = st.nextToken().trim();
						}
						if (!s.equalsIgnoreCase("NaN")) {
							try {
								dparm.maxScaleDC = Float.valueOf(s).floatValue();
							} catch (NumberFormatException nfe) {
							}
						}
						if (Float.isNaN(dparm.minScaleDC) && Float.isNaN(dparm.maxScaleDC)) {
							dparm.drawCondition = false;
						}
					}
					continue;
				}
				if (keyWord.equals("MAX_LEVELS")) {
					try {
						dparm.maxLevels = Integer.valueOf(val).intValue();
					} catch (NumberFormatException ne) {
						dparm.maxLevels = -1;
					}
					continue;
				}
				if (keyWord.equals("LABELS")) {
					dparm.drawLabels = val.startsWith("+");
					continue;
				}
				if (keyWord.equals("BORDERS")) {
					dparm.drawBorders = !val.startsWith("-");
					continue;
				}
				if (keyWord.equals("HL_CIRCLES")) {
					dparm.hlDrawCircles = !val.startsWith("-");
					continue;
				}
				if (keyWord.equals("HLIGHTEDW")) {
					try {
						dparm.hlWidth = Integer.valueOf(val).intValue();
					} catch (NumberFormatException ne) {
					}
					continue;
				}
				if (keyWord.equals("HL_CIRCLE_SIZE")) {
					try {
						dparm.hlCircleSize = Integer.valueOf(val).intValue();
					} catch (NumberFormatException ne) {
					}
					continue;
				}
				if (keyWord.equals("SELECTEDW")) {
					try {
						dparm.selWidth = Integer.valueOf(val).intValue();
					} catch (NumberFormatException ne) {
					}
					continue;
				}
				if (keyWord.equals("BORDERW")) {
					try {
						dparm.lineWidth = Integer.valueOf(val).intValue();
					} catch (NumberFormatException ne) {
					}
					continue;
				}
				if (keyWord.equals("BORDERCOLOR") || keyWord.equals("LABELCOLOR") || keyWord.equals("FOREGROUND") || keyWord.equals("BACKGROUND") || keyWord.equals("HL_CIRCLE_COLOR")) {
					int r = 0, g = 0, b = 0;
					StringTokenizer stt = new StringTokenizer(val, " (,);");
					for (int i = 0; i < 3 && stt.hasMoreTokens(); i++) {
						try {
							int n = Integer.valueOf(stt.nextToken()).intValue();
							if (i == 0) {
								r = n;
							} else if (i == 1) {
								g = n;
							} else {
								b = n;
							}
						} catch (NumberFormatException ne) {
						}
					}
					if (keyWord.equals("BORDERCOLOR")) {
						dparm.lineColor = new Color(r, g, b);
					} else if (keyWord.equals("LABELCOLOR")) {
						dparm.labelColor = new Color(r, g, b);
					} else if (keyWord.equals("FOREGROUND")) {
						dparm.patternColor = new Color(r, g, b);
					} else if (keyWord.equals("BACKGROUND")) {
						dparm.fillColor = new Color(r, g, b);
					} else if (keyWord.equals("HL_CIRCLE_COLOR")) {
						dparm.hlCircleColor = new Color(r, g, b);
					}
					continue;
				}
				if (keyWord.equals("LABELFONT")) {
					Vector fontDescr = null;
					fontDescr = StringUtil.getNames(val, ",");
					if (fontDescr != null) {
						dparm.fontName = (String) fontDescr.elementAt(0);
						for (int i = 1; i < fontDescr.size(); i++) {
							try {
								int n = Integer.valueOf(fontDescr.elementAt(i).toString()).intValue();
								switch (i) {
								case 1: {
									dparm.fontStyle = n;
									break;
								}
								case 2: {
									dparm.fontSize = n;
									break;
								}
								case 3: {
									dparm.labelStyle = n;
								}
								}
							} catch (NumberFormatException ne) {
							}
							//System.out.println("Read from app font: "+sFontName+" style "+fontStyle+" size "+fontSize+" option "+fontOption);
						}
					} else {
						System.out.println("LABELFONT: invalid font description syntax");
					}
					continue;
				}
				if (keyWord.equals("HATCH_STYLE")) {
					try {
						dparm.patternN = Integer.valueOf(val).intValue();
					} catch (NumberFormatException nfe) {
						dparm.patternN = 1;
					}
					if (dparm.patternN == 0) {
						dparm.fillContours = false;
					}
					continue;
				}
				if (keyWord.equals("TRANSPARENCY")) {
					try {
						dparm.transparency = Integer.parseInt(val);
						if (dparm.transparency > 100 || dparm.transparency < 0) {
							dparm.transparency = 0;
						}
						if (!java2d.Drawing2D.isJava2D) {
							dparm.isTransparent = dparm.transparency == 0 ? false : true;
						}
					} catch (Exception ex) {
						//dparm.transparency = -1;
						dparm.isTransparent = val.startsWith("+");
						dparm.transparency = dparm.isTransparent ? 50 : 0;
					}
					continue;
				}
//ID
				if (keyWord.equals("COLOR_SCALE")) {
					dparm.colorScale = val;
					continue;
				}
				if (keyWord.equals("CS_PARAMETERS")) {
					dparm.csParameters = str.substring(keyWord.length() + 1).trim();
					if (dparm.csParameters.startsWith("\"") && dparm.csParameters.endsWith("\"")) {
						dparm.csParameters = dparm.csParameters.substring(1, dparm.csParameters.length() - 1);
					}
					String path = dparm.csParameters;
					if (dir != null && !CopyFile.isAbsolutePath(path)) {
						path = dir + path;
					}
					if (CopyFile.checkExistence(path)) {
						dparm.csParameters = path;
					}
					continue;
				}
			}
			if (keyWord.equals("VALID_RANGE")) {
				StringTokenizer range = new StringTokenizer(str.substring(keyWord.length() + 1).trim(), " ");
				try {
					if (range.hasMoreTokens()) {
						spec.validMin = new Float(range.nextToken()).floatValue();
					}
					if (range.hasMoreTokens()) {
						spec.validMax = new Float(range.nextToken()).floatValue();
					}
				} catch (Exception ex) {
				}
				continue;
			}
//~ID
//ID
			if (keyWord.equals("ATTRIBUTE")) {
				String s = str.substring(keyWord.length() + 1).trim();
				if (s == null || s.length() == 0) {
					break;
				}
				if (s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
					s = s.substring(1, s.length() - 1);
				}
				spec.gridAttribute = s;
				continue;
			}
			if (keyWord.equals("PARAMETER")) {
				StringTokenizer params = new StringTokenizer(str.substring(keyWord.length() + 1).trim(), "\"");
				try {
					String s = "";
					if (params.hasMoreTokens()) {
						s = params.nextToken().trim();
					}
					if (s.length() == 0) {
						break;
					}
					if (spec.gridParameterNames == null) {
						spec.gridParameterNames = new Vector();
						spec.gridParameterValues = new Vector();
					}
					spec.gridParameterNames.addElement(s);

					s = "";
					if (params.hasMoreTokens()) {
						s = params.nextToken().trim();
					}
					if (!s.equalsIgnoreCase("VALUE")) {
						break;
					}

					s = "";
					if (params.hasMoreTokens()) {
						s = params.nextToken();
					}
					if (s.length() == 0) {
						break;
					}
					spec.gridParameterValues.addElement(s);

				} catch (Exception ex) {
				}
				continue;
			}
			if (keyWord.equals("TIME_PARAMETER")) {
				StringTokenizer params = new StringTokenizer(str.substring(keyWord.length() + 1).trim(), "\"");
				try {
					String s = "";
					if (params.hasMoreTokens()) {
						s = params.nextToken().trim();
					}
					if (s.length() == 0) {
						break;
					}
					if (spec.gridParameterNames == null) {
						spec.gridParameterNames = new Vector();
						spec.gridParameterValues = new Vector();
					}
					spec.gridParameterNames.addElement(s);

					s = "";
					if (params.hasMoreTokens()) {
						s = params.nextToken().trim();
					}
					if (!s.equalsIgnoreCase("VALUE")) {
						break;
					}

					String valS = "";
					String scheme = "";
					if (params.hasMoreTokens()) {
						valS = params.nextToken().trim();
					}
					if (s.length() == 0) {
						break;
					}

					s = "";
					if (params.hasMoreTokens()) {
						s = params.nextToken().trim();
					}
					if (s.equalsIgnoreCase("SCHEME")) {

						if (params.hasMoreTokens()) {
							scheme = params.nextToken().trim();
						}
						if (scheme.length() == 0) {
							break;
						}
					}

					if (scheme.length() > 0) {
						Date tm = new Date();
						tm.setDateScheme(scheme);
						tm.setMoment(valS);
						spec.gridParameterValues.addElement(tm);
					} else {
						spec.gridParameterValues.addElement(new TimeCount(new Long(valS).longValue()));
					}

					spec.hasTemporalParameter = true;
				} catch (Exception ex) {
				}
				continue;
			}
//~ID
			//any other keyword
			if (spec.extraInfo == null) {
				spec.extraInfo = new Hashtable();
			}
			spec.extraInfo.put(keyWord, val);
		}
		if (spec != null && spec.source != null) {
			applDescr.addDataSpecification(spec);
		}
		closeStream(stream);
		if (applDescr.getDataSpecCount() < 1) {
			notifyProcessState("No data specification read!", true);
			return null;
		}
		applDescr.findCommonSourceData();
		notifyProcessState("Read " + applDescr.getDataSpecCount() + " data specifications", false);
		return applDescr;
	}

	/**
	* Reads a "datapipe" specification from an application description file.
	* The specification must end with the keyword "/DATAPIPE".
	*/
	protected DataPipeSpec readDataPipeSpec(BufferedReader reader) throws IOException {
		if (reader == null)
			return null;
		DataPipeSpec dps = null;
		while (true) {
			String str = null;
			str = reader.readLine();
			if (str == null) {
				break;
			}
			if (str.startsWith("*")) {
				continue; //this is a comment
			}
			str = str.trim();
			if (str.length() < 1) {
				continue;
			}
			if (str.equalsIgnoreCase("/DATAPIPE")) {
				break;
			}
			Vector tokens = StringUtil.getNames(str.trim(), " ;");
			if (tokens.size() < 2) {
				continue;
			}
			String keyWord = ((String) tokens.elementAt(0)).toUpperCase();
			String val = null;
			if (keyWord.equals("DELIMITER")) {
				val = str.substring(keyWord.length() + 1).trim();
				if (val.charAt(0) == '\"' && val.charAt(val.length() - 1) == '\"') {
					val = val.substring(1, val.length() - 1).trim();
				}
			} else {
				val = (String) tokens.elementAt(1);
			}
			if (dps == null) {
				dps = new DataPipeSpec();
			}
			if (keyWord.equals("SOURCE")) {
				dps.dataSource = val;
			} else if (keyWord.equals("ADD_TO_LAYER")) {
				dps.layerId = val;
			} else if (keyWord.equals("UPDATER")) {
				dps.updater = val;
			} else if (keyWord.equals("WRITE_UPDATES_TO")) {
				dps.tableFileName = val;
			} else if (keyWord.equals("DELIMITER")) {
				dps.delimiter = val;
			}
		}
		return dps;
	}

	/**
	* Reads a "about_project" specification from an application description file.
	* The specification must end with the keyword "/ABOUT_PROJECT".
	*/
	protected String readProjectDescr(BufferedReader reader) throws IOException {
		if (reader == null)
			return null;
		StringBuffer buffer = new StringBuffer();
		while (true) {
			String str = null;
			str = reader.readLine();
			if (str == null) {
				break;
			}
			if (str.startsWith("*")) {
				continue; //this is a comment
			}
			str = str.trim();
			if (str.length() < 1) {
				continue;
			}
			if (str.equalsIgnoreCase("/ABOUT_PROJECT")) {
				break;
			}
			if (buffer.length() != 0) {
				buffer.append("\n");
			}
			buffer.append(str);
		}
		return buffer.toString();
	}
}
