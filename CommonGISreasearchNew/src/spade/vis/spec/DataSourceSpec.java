package spade.vis.spec;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.DataOutputStream;
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
import spade.lib.util.StringUtil;
import spade.vis.dmap.DrawingParameters;

public class DataSourceSpec implements java.io.Serializable {
	/**
	* Data source identifier: may be used in configuration files for linking
	* tables to corresponding map layers.
	*/
	public String id = null;
	/**
	* The name of the table/layer/map to be shown to the user
	*/
	public String name = null;
	/**
	* This may be, for example, the path to the file with the data, but also
	* "clipboard" or other indication of the data source
	*/
	public String source = null;
	/**
	* In a GML file there may be several layers. They are distinguished by their
	* "typeNames". This variable specifies the typename for a layer loaded
	* from such a GML file.
	*/
	public String typeName = null;
	/**
	* If a data source, such as a GML file, contains several layers, not all of
	* them may be needed. A project (application) specification file (*.app) may
	* list only those layers that are needed. For each of the layers there may be
	* individual parameters etc. This vector contains specifications for layers
	* to be loaded from a common source. The reader must skip layers for that
	* there is no specification. The elements of the vector are instances of
	* DataSourceSpec.
	*/
	public Vector layersToLoad = null;
	/**
	* The path to a table with identifiers of the geographical objects (if stored
	* separately from geographical data, as in a case of a shape file) and,
	* possibly, other thematic data.
	* In this case the variable source specifies the source of the geographical
	* data, i.e. contours or coordinates of the objects
	*/
	public String objDescrSource = null;

	public String server = null; //for remote data loading
	public String driver = null; //for JDBC
	public String url = null; //for JDBC
	public String user = null; //for JDBC
	public String password = null; //for JDBC
	public String catalog = null; //for JDBC
	/**
	* The names of the fields to load (if not all fields are to be loaded).
	* Usually this information is specified for loaders from a database.
	*/
	public Vector columns = null; //for JDBC
	/**
	* This information allows to link a table described by this specification with
	* a map layer. Usually this information is provided in an application
	* configuration file.
	*/
	public String layerName = null;
	/**
	* The format of the data. Usually this is the extension of the file, but may
	* be also "JDBC", "Oracle", "ASCII", etc.
	*/
	public String format = null;
	/**
	* The string of length 1 containing the symbol used as the delimiter in the
	* data file. By default is null. Needed for reading data from
	* ASCII files.
	*/
	public String delimiter = null;
	/**
	* The number of the field with object identifiers in a source of attribute
	* data. Initially -1.
	*/
	public int idFieldN = -1;
	/**
	* The name of the field with object identifiers in a source of attribute data.
	*/
	public String idFieldName = null;
	/**
	* The number of the field with object names. By default, this is -1
	* (i.e. none of the fields is interpreted as containing object names).
	*/
	public int nameFieldN = -1;
	/**
	* The name of the field with object names.
	*/
	public String nameFieldName = null;
	/**
	* Specifies in what line from the beginning of the ASCII data stream (or
	* after the header with metadata) there are field names. By default, equals
	* to -1 meaning that there is no such line. Relevant for readers from ASCII
	* format.
	*/
	public int nRowWithFieldNames = -1;
	/**
	* Specifies in what line from the beginning of the data stream (or
	* after the header with metadata) there are field types. By default, equals
	* to -1 meaning that there is no such line
	* Field types are encoded by symbols 'N' (numeric), 'C' (character),
	* 'L' (logical), or 'D' (date).
	* To be added: for a field with "date" type the date scheme may be specified.
	* Relevant for readers from ASCII format.
	*/
	public int nRowWithFieldTypes = -1;
	/**
	* The name of the field with geometry (for loading data from a database where
	* thematic data are stored together with geometry). By default, this is null
	* (i.e. none of the fields is interpreted as containing geometry).
	*/
	public String geoFieldName = null;
	/**
	* The name of the field with X-coordinates (if the table contains coordinates
	* of point objects)
	*/
	public String xCoordFieldName = null;
	/**
	* The name of the field with Y-coordinates (if the table contains coordinates
	* of point objects)
	*/
	public String yCoordFieldName = null;
	/**
	* The name of the field with circle radii (if the table contains centres and
	* radii of circle objects)
	*/
	public String radiusFieldName = null;
	/**
	 * If the data describe complex objects such as trajectories, several table
	 * rows may describe a single object. In this case, the identifiers in these
	 * rows will be the same. They need to be treated as values of an attribute
	 * rather than as record identifiers.
	 */
	public boolean multipleRowsPerObject = false;
	/**
	* For specification of a map layer - the type of the objects in the layer
	* (point, line, area, raster - @see spade.vis.geometry.Geometry)
	*/
	public char objType = 0;
	/**
	* For specification of a map layer - the subtype of the objects in the layer
	* (e.g. circles, movements, vectors - @see spade.vis.geometry.Geometry)
	*/
	public char objSubType = 0;
	/**
	* For geographical layers, indicates whether the layer may contain polygons
	* with holes. Information about the absense of holes may be used for
	* optimizing loading in some readers.
	*/
	public boolean mayHaveHoles = true;
	/**
	* For geographical layers, indicates whether the layer may contain objects
	* consisting of multiple parts (e.g. countries with islands).
	* Information about the absense of multi-part objects may be used for
	* optimizing loading in some readers.
	*/
	public boolean mayHaveMultiParts = true;
	/**
	* For geographical layers, indicates whether object identifiers are important.
	* Identifiers are typically not important when a layer is used as a background
	* and has no associated thematic data. Information about unimportance of
	* identifiers may be used for optimizing loading in some readers.
	*/
	public boolean idsImportant = true;
	/**
	 * For a table, indicates whether this table is used for building a map layer.
	 * This may be, for example, a table describing flows or links between
	 * locations, which are specified in another layer.
	 */
	public boolean toBuildMapLayer = false;
	/**
	* For a map layer - its drawing parameters (usually an instance of
	* spade.vis.dmap.DrawingParameters)
	*/
	public Object drawParm = null;
	/**
	* A vector of bounding rectangles specifying the territory to load.
	* Valid in a case of loading data from a database where
	* thematic data are stored together with geometry.
	* For an image map layer (i.e. loaded from a GIF or JPEG file) specifies
	* its geographical reference.
	* Elements of the vector are instances of spade.vis.geometry.RealRectangle
	*/
	public Vector bounds = null;
	/**
	* The total number of rows in the database table (if appropriate). Needed,
	* for example, for initialising a data broker that optimises access to the
	* database by means of caching.
	*/
	public int dbRowCount = 0;
	/**
	* The territory extent of all the data in the database table (if the table
	* contains spatial information). Needed, for example,
	* for initialising a data broker that optimises access to the database
	* by means of caching. If dataExtent is set, this is an instance of
	* spade.vis.geometry.RealRectangle
	*/
	public Object dataExtent = null;
	/**
	* Indicates whether the table is to be used for decision making (relevant
	* for table data)
	*/
	public boolean useForDecision = false;
	/**
	* If the table is used for decision making, this variable (instance of
	* spade.vis.spec.DecisionSpec) contains information necessary for
	* storing results of decision making
	*/
	public Object decisionInfo = null;
	/**
	* Additional descriptors, which can describe any aspects of data structure,
	* including semantics necessary for correct data interpretation.
	* Each descriptor has to implement the interface spase.vis.spec.TagReader.
	* In particular, there may be a TimeRefDescriptor that describes time
	* references contained in data.
	*/
	public Vector descriptors = null;
	/**
	* The value is true if among the descriptors there is at least one descriptor
	* of a temporal parameter.
	*/
	public boolean hasTemporalParameter = false;
	/**
	* valid range of values in raster
	*/
	public float validMin = Float.NEGATIVE_INFINITY;
	public float validMax = Float.POSITIVE_INFINITY;
//ID
	public String gridAttribute = "";
	public Vector gridParameterNames = null;
	public Vector gridParameterValues = null;
//~ID
	/**
	 * May contain any other information needed for various purposes
	 */
	public Hashtable extraInfo = null;

	public Object getExtraInfoByKey(String key) {
		if (extraInfo == null || key == null || extraInfo.isEmpty())
			return null;
		return extraInfo.get(key);
	}

	public String getExtraInfoByKeyAsString(String key) {
		Object info = getExtraInfoByKey(key);
		if (info == null)
			return null;
		return info.toString();
	}

	/**
	* Creates a copy of itself
	*/
	@Override
	public Object clone() {
		DataSourceSpec dss = new DataSourceSpec();
		dss.bounds = bounds;
		dss.catalog = catalog;
		dss.columns = (columns == null) ? null : (Vector) columns.clone();
		dss.dataExtent = dataExtent;
		dss.dbRowCount = dbRowCount;
		dss.decisionInfo = decisionInfo;
		dss.delimiter = delimiter;
		dss.drawParm = drawParm;
		dss.driver = driver;
		dss.format = format;
		dss.geoFieldName = geoFieldName;
		dss.id = id;
		dss.idFieldN = idFieldN;
		dss.idFieldName = idFieldName;
		dss.idsImportant = idsImportant;
		dss.layerName = layerName;
		dss.layersToLoad = (layersToLoad == null) ? null : (Vector) layersToLoad.clone();
		dss.mayHaveHoles = mayHaveHoles;
		dss.mayHaveMultiParts = mayHaveMultiParts;
		dss.multipleRowsPerObject = multipleRowsPerObject;
		dss.name = name;
		dss.nameFieldN = nameFieldN;
		dss.nameFieldName = nameFieldName;
		dss.nRowWithFieldNames = nRowWithFieldNames;
		dss.nRowWithFieldTypes = nRowWithFieldTypes;
		dss.objDescrSource = objDescrSource;
		dss.objType = objType;
		dss.objSubType = objSubType;
		dss.password = password;
		dss.server = server;
		dss.source = source;
		dss.typeName = typeName;
		dss.url = url;
		dss.useForDecision = useForDecision;
		dss.user = user;
		dss.xCoordFieldName = xCoordFieldName;
		dss.yCoordFieldName = yCoordFieldName;
		dss.radiusFieldName = radiusFieldName;
		dss.descriptors = (descriptors == null) ? null : (Vector) descriptors.clone();
		if (extraInfo != null) {
			dss.extraInfo = (Hashtable) extraInfo.clone();
		}
		dss.hasTemporalParameter = hasTemporalParameter;
		dss.validMax = validMax;
		dss.validMin = validMin;
		dss.toBuildMapLayer = toBuildMapLayer;
//ID
		dss.gridAttribute = gridAttribute;
		dss.gridParameterNames = gridParameterNames;
		dss.gridParameterValues = gridParameterValues;
//~ID
		return dss;
	}

	/**
	* The argument ("symbol") should be a string of length 1 containing the
	* delimiter symbol.
	*/
	public void setDelimiter(String symbol) {
		if (symbol != null && symbol.length() > 0)
			if (symbol.length() == 1) {
				delimiter = symbol;
			} else {
				delimiter = symbol.substring(0, 1);
			}
	}

	public String getDelimiter() {
		return delimiter;
	}

	/**
	* Sets the names of the fields to load (if not all fields are to be loaded).
	*/
	public void setColumns(String columns[]) {
		if (columns == null || columns.length == 0)
			return;
		this.columns = new Vector();
		for (String column : columns) {
			this.columns.addElement(column);
		}
	}

	/**
	* Sets the names of the fields to load (if not all fields are to be loaded).
	* The names are listed in a string delimited with commas or semicolons.
	*/
	public void setColumns(String nameStr) {
		if (nameStr == null)
			return;
		nameStr = nameStr.trim();
		if (nameStr.startsWith("\"")) {
			int i1 = 0;
			while (i1 >= 0 && i1 < nameStr.length()) {
				int i2 = nameStr.indexOf('\"', i1 + 1);
				if (i2 < 0) {
					i2 = nameStr.length();
				}
				if (i2 > i1 + 1) {
					String s = nameStr.substring(i1 + 1, i2).trim();
					if (s.length() > 0) {
						if (columns == null) {
							columns = new Vector(10, 10);
						}
						columns.addElement(s);
					}
				}
				if (i2 < nameStr.length() - 1) {
					i1 = nameStr.indexOf('\"', i2 + 1);
				} else {
					i1 = -1;
				}
			}
		} else {
			StringTokenizer st = new StringTokenizer(nameStr, ",\r\n");
			if (!st.hasMoreTokens())
				return;
			columns = new Vector(st.countTokens(), 5);
			while (st.hasMoreTokens()) {
				columns.addElement(st.nextToken());
			}
		}
	}

	/**
	* A utility method used to read "tags", i.e. sequences of lines starting
	* with <keyword ...> and ending with </keyword>. The first argument is
	* the first string of the tag (i.e. any string starting with "<"), the
	* second argument is the reader used to read the data.
	*/
	public void readTag(String header, BufferedReader br) throws IOException {
		if (header == null || br == null || !header.startsWith("<"))
			return;
		String str = header.substring(1).toLowerCase();
		int k = str.indexOf('>');
		if (k > 0) {
			str = str.substring(0, k).trim();
		}
		k = str.indexOf(' ');
		if (k < 0) {
			k = str.length();
		}
		String key = str.substring(0, k);
		boolean ok = false;
		TagReader reader = TagReaderFactory.getTagReader(key);
		if (reader != null) {
			ok = reader.readDescription(header, br);
			if (ok) {
				if (descriptors == null) {
					descriptors = new Vector(5, 5);
				}
				descriptors.addElement(reader);
				hasTemporalParameter = hasTemporalParameter || ((reader instanceof ParamSpec) && ((ParamSpec) reader).isTemporalParameter());
			}
		}
		if (!ok) { //search for the end tag
			boolean end = false;
			do {
				str = br.readLine();
				if (str != null) {
					str = str.trim();
					if (str.length() < 1) {
						continue;
					}
					if (str.startsWith("</")) {//this the end tag
						str = str.toLowerCase();
						end = str.startsWith("</" + key);
					}
				}
			} while (!end && str != null);
		}
	}

	/**
	* Stores the additional descriptors of the data structure/semantics, such as
	* descriptors of time references or parameters.
	*/
	public void writeDataDescriptors(DataOutputStream dos) throws IOException {
		if (descriptors != null) {
			for (int i = 0; i < descriptors.size(); i++) {
				TagReader reader = (TagReader) descriptors.elementAt(i);
				reader.writeDescription(dos);
			}
		}
	}

	/**
	 * The message about an error during an i/o operation
	 */
	private static String errMsg = null;

	/**
	 * Reads metadata about a single data source (file) from the specified
	 * ASCII file
	 */
	public static DataSourceSpec readMetadata(String descrFileName, String applPath) {
		errMsg = null;
		if (descrFileName == null)
			return null;
		if (!CopyFile.isAbsolutePath(descrFileName) && applPath != null) {
			applPath = CopyFile.getDir(applPath);
			if (applPath != null) {
				descrFileName = applPath + descrFileName;
			}
		}
		InputStream stream = openStream(descrFileName);
		if (stream == null)
			return null;
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		DataSourceSpec spec = new DataSourceSpec();
		DrawingParameters dparm = new DrawingParameters();
		spec.drawParm = dparm;
		while (true) {
			String str = null;
			try {
				str = reader.readLine();
			} catch (EOFException eof) {
				break;
			} catch (IOException ioe) {
				errMsg = "Error reading metadata: " + ioe;
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
			if (str.startsWith("<")) { //this is a "tag"
				try {
					spec.readTag(str, reader);
				} catch (EOFException eof) {
					break;
				} catch (IOException ioe) {
					errMsg = "Error reading metadata: " + ioe;
					return null;
				}
				continue;
			}
			Vector tokens = StringUtil.getNames(str, " ;");
			if (tokens == null || tokens.size() < 1) {
				continue;
			}
			String keyWord = ((String) tokens.elementAt(0)).toUpperCase();
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
				} else {
					val = (String) tokens.elementAt(1);
				}
			}
			if (keyWord.equals("LAYER") || keyWord.equals("TABLEDATA")) {
				spec.source = val;
			} else if (keyWord.equals("NAME")) {
				spec.name = val;
			} else if (keyWord.equals("FORMAT")) {
				spec.format = val;
			} else if (keyWord.equals("SERVER")) {
				spec.server = val;
			} else if (keyWord.equals("URL")) {
				spec.url = val;
			} else if (keyWord.equals("DELIMITER"))
				if (val.equalsIgnoreCase("TAB")) {
					spec.delimiter = "\t";
				} else {
					spec.delimiter = val;
				}
			else if (keyWord.equals("FIELD_NAMES_IN_ROW")) {
				try {
					spec.nRowWithFieldNames = Integer.valueOf(val).intValue() - 1;
				} catch (NumberFormatException e) {
					errMsg = "Illegal number of row with field names for " + spec.source;
				}
			} else if (keyWord.equals("FIELD_TYPES_IN_ROW")) {
				try {
					spec.nRowWithFieldTypes = Integer.valueOf(val).intValue() - 1;
				} catch (NumberFormatException e) {
					errMsg = "Illegal number of row with field types for " + spec.source;
				}
			} else if (keyWord.equals("ID_FIELD") || keyWord.equals("NAME_FIELD")) {
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
			} else if (keyWord.equals("ID_FIELD_NAME")) {
				spec.idFieldName = val;
			} else if (keyWord.equals("NAME_FIELD_NAME")) {
				spec.nameFieldName = val;
			} else if (keyWord.equals("ATTRIBUTES")) {
				spec.setColumns(val);
			} else if (keyWord.equals("KEY") || keyWord.equals("USER")) {
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
			} else if (keyWord.equals("LAYER_REF")) { //the table refers to a layer
				spec.layerName = val;
			} else if (keyWord.equals("DRAWING")) {
				if (val.startsWith("-")) {
					dparm.drawLayer = false;
				} else {
					dparm.drawLayer = true;
				}
			} else if (keyWord.equals("BORDERS")) {
				if (val.startsWith("-")) {
					dparm.drawBorders = false;
				} else {
					dparm.drawBorders = true;
				}
			} else if (keyWord.equals("BORDERW")) {
				try {
					dparm.lineWidth = Integer.valueOf(val).intValue();
				} catch (NumberFormatException ne) {
				}
			} else if (keyWord.equals("HLIGHTEDW")) {
				try {
					dparm.hlWidth = Integer.valueOf(val).intValue();
				} catch (NumberFormatException ne) {
				}
			} else if (keyWord.equals("SELECTEDW")) {
				try {
					dparm.selWidth = Integer.valueOf(val).intValue();
				} catch (NumberFormatException ne) {
				}
			} else if (keyWord.equals("BORDERCOLOR")) {
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
				dparm.lineColor = new Color(r, g, b);
			} else if (keyWord.equals("TRANSPARENCY")) {
				try {
					dparm.transparency = Integer.parseInt(val);
					if (dparm.transparency > 100 || dparm.transparency < 0) {
						dparm.transparency = 0;
					}
					if (!java2d.Drawing2D.isJava2D) {
						dparm.isTransparent = dparm.transparency == 0 ? false : true;
					}
				} catch (Exception ex) {
					dparm.isTransparent = val.startsWith("+");
					dparm.transparency = dparm.isTransparent ? 50 : 0;
				}
			} else { //any other keyword
				if (spec.extraInfo == null) {
					spec.extraInfo = new Hashtable();
				}
				spec.extraInfo.put(keyWord, val);
			}
		}
		closeStream(stream);
		return spec;
	}

	/**
	* Opens the stream on the specified data source. If the source
	* string starts with HTTP or FILE, accesses the source as a URL, otherwise
	* opens it as a local file
	*/
	protected static InputStream openStream(String dataSource) {
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
			errMsg = "Error accessing " + dataSource + ": " + ioe;
			return null;
		}
		return stream;
	}

	protected static void closeStream(InputStream stream) {
		if (stream == null)
			return;
		try {
			stream.close();
		} catch (IOException ioe) {
		}
	}

	/**
	 * Returns the message about an error during an i/o operation
	 */
	public static String getErrorMessage() {
		return errMsg;
	}
}