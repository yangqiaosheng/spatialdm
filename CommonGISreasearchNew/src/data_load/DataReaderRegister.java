package data_load;

import java.util.ResourceBundle;

import spade.analysis.system.DataReader;
import spade.analysis.system.DataReaderFactory;
import spade.lib.lang.Language;
import spade.lib.util.IntArray;

/**
* In this class all data readers must be registered
*/
public class DataReaderRegister implements DataReaderFactory {
	static ResourceBundle res = Language.getTextResource("data_load.Res");
	/**
	* The list of known data readers. For each data reader specifies
	* 1) its unique internal identifier;
	* 2) the full name of the class implementing it;
	* 3) a short text characterising the data source appropriate for this
	*    reader. The text must be suitable for putting on a button. For example,
	*    "DBF", "Oracle database", "Clipboard", etc.;
	* 4) a more detailed text describing the data source type. This text
	*    must be usable as an explanation to a button.
	* Each reader must implement the interface spade.analysis.system.DataReader.
	*/
	protected static String readers[][] = { { "shape", "data_load.readers.ShapeReader", res.getString("Shape_file"), res.getString("read_vector_data_from") },
			{ "mif", "data_load.readers.MIFReader", res.getString("MIF_MID_file"), res.getString("read_vector_data_from1") }, { "ovl", "data_load.readers.OVLReader", res.getString("OVL_file"), res.getString("read_vector_data_from2") },
			{ "grid", "data_load.readers.GridASCIIReader", res.getString("GRID_file"), res.getString("read_vector_grid") }, { "adf", "data_load.readers.ADFReader", res.getString("ADF_file"), res.getString("read_raster_data_from") },
			{ "bil", "data_load.readers.BILReader", res.getString("BIL_file"), res.getString("read_raster_data_from1") }, { "flt", "data_load.readers.FLTReader", res.getString("FLT_file"), res.getString("read_raster_data_from2") },
			{ "esr", "data_load.readers.ESRReader", res.getString("ESR_file"), res.getString("read_raster_data_from3") }, { "image", "data_load.readers.ImageReader", res.getString("image"), res.getString("load_a_GIF_or_JPEG") },
			{ "ascii", "data_load.readers.ASCIIReader", res.getString("ASCII_file"), res.getString("read_thematic_data") }, { "dbf", "data_load.readers.DBFReader", res.getString("DBF_file"), res.getString("read_thematic_data1") },
			{ "quadtree", "data_load.readers.QuadTreeFromCSVReader", res.getString("QuadTree_from_ASCII"), res.getString("read_QuadTree_from_ASCII") },
			/*
			{"gml","data_load.readers.GMLAdapter",res.getString("GML_file"),
			 res.getString("read_data_thematic")},
			*/
			{ "gml_1", "data_load.read_gml.GML10Reader", res.getString("GML10_file"), res.getString("read_GML1") }, { "gml_2", "data_load.read_gml.GMLReader", res.getString("GML20_file"), res.getString("read_GML2") },
			{ "wkb", "data_load.readers.WKBAdapter", res.getString("WKB_file"), res.getString("read_data_thematic1") }, { "clipboard", "data_load.readers.ClipBoardReader", res.getString("clipboard"), res.getString("read_thematic_data2") },
			{ "JDBC", "data_load.read_db.JDBCReader", res.getString("ODBC"), res.getString("read_thematic_data3") }, { "xml", "data_load.readers.XMLReader", res.getString("XML_file"), res.getString("read_from_XML") },
			{ "oracle", "data_load.read_oracle.OracleReader", res.getString("Oracle"), res.getString("read_thematic_and_or") }, { "mim", "data_load.readers.MultiImageReader", res.getString("multi_image"), res.getString("read_multi_images") }

	//{"ArcSDE","data_load.readers.ArcSDEReader","ArcSDE","read spatial data from ArcSDE-layer."}
	};
	/**
	* Correspondence between the readers and data formats. The first element in
	* each subarray indicates the format, the second is the identifier of the
	* corresponding data reader. In format strings small and capital letters
	* are not distinguished
	*/
	protected static String formatCorr[][] = { { "OVL", "ovl" }, { "OVR", "ovl" }, { "CSV", "ascii" }, { "TXT", "ascii" }, { "DAT", "ascii" }, { "ASCII", "ascii" }, { "DBF", "dbf" }, { "QUADTREE", "quadtree" }, { "XML", "xml" }, { "GRID", "grid" },
			{ "GRD", "grid" }, { "GIF", "image" }, { "JPG", "image" },
			{ "JPEG", "image" },
			// P.G.: added new image formats to be understandable by the system
			{ "TIF", "image" }, { "TIFF", "image" }, { "TARGA", "image" }, { "TGA", "image" }, { "PNG", "image" }, { "XPM", "image" }, { "PCX", "image" }, { "PSD", "image" }, { "PCT", "image" }, { "PICT", "image" }, { "CUR", "image" },
			{ "XBM", "image" }, { "BMP", "image" }, { "ICO", "image" }, { "RASTER", "image" }, { "PCT", "image" }, { "RAS", "image" },
			// ~P.G.
			{ "SHP", "shape" }, { "SHAPE", "shape" }, { "FLT", "flt" }, { "ESR", "esr" }, { "GML", "gml_2" }, { "GML1.0", "gml_1" }, { "GML2.0", "gml_2" }, { "GML2.1", "gml_2" }, { "GML10", "gml_1" }, { "GML20", "gml_2" }, { "GML21", "gml_2" },
			{ "WKB", "wkb" }, { "MIF", "mif" }, { "BIL", "bil" }, { "ADF", "adf" }, { "JDBC", "JDBC" }, { "ODBC", "JDBC" }, { "ORACLE", "oracle" }, { "ArcSDE", "ArcSDE" }, { "mim", "mim" }

	};
	/**
	* Formats that may contain thematic data. This also applies to the formats for
	* geographic data that may additionally include or be linked to thematic data,
	* as, for example, shape format of ArcView.
	*/
	protected static String attrFormats[] = { "ascii", "dbf", "shape", "quadtree", "xml", "gml", "gml_1", "gml_2", "mif", "JDBC", "oracle", "grid" };
	/**
	* The array of indices of available readers (to avoid multiple checking of
	* presence of classes)
	*/
	protected IntArray available = null;
	/**
	* The error message
	*/
	protected String err = null;

	/**
	* Checks if the given format string represents some known format
	*/
	public static boolean isValidFormat(String format) {
		if (format == null)
			return false;
		for (String[] element : formatCorr)
			if (format.equalsIgnoreCase(element[0]))
				return true;
		return false;
	}

	/**
	* Replies whether the given data format may contain attribute (thematic) data.
	* For an unknown format returns false.
	*/
	public static boolean mayHaveAttrData(String format) {
		if (format == null)
			return false;
		String formatId = null;
		for (int i = 0; i < formatCorr.length && formatId == null; i++)
			if (format.equalsIgnoreCase(formatCorr[i][0])) {
				formatId = formatCorr[i][1];
			}
		if (formatId == null)
			return false; //unknown format
		for (String attrFormat : attrFormats)
			if (formatId.equalsIgnoreCase(attrFormat))
				return true;
		return false;
	}

	/**
	* Returns the index of the reader with the given identifier in the array of
	* ALL readers
	*/
	protected int getReaderIndex(String id) {
		if (id == null)
			return -1;
		for (int i = 0; i < readers.length; i++)
			if (id.equals(readers[i][0]))
				return i;
		return -1;
	}

	/**
	* Checks availability of the classes implementing the specified data readers
	*/
	protected void checkAvailability() {
		if (available == null) {
			available = new IntArray(readers.length, 1);
			for (int i = 0; i < readers.length; i++)
				if (readers[i][1] != null) { //the name of the class implementing the tool
					try {
						Class.forName(readers[i][1]);
						available.addElement(i);
						//System.out.println("available data reader: "+readers[i][2]+
						//                   ", class = "+readers[i][1]);
					} catch (Exception e) {
						//System.out.println("NOT available data reader: "+readers[i][2]+
						//  ", class = "+readers[i][1]);
					}
				}
		}
	}

	/**
	* Returns the number of available classes for data reading
	*/
	public int getAvailableReaderCount() {
		if (available == null) {
			checkAvailability();
		}
		return available.size();
	}

	/**
	* Returns the identifier of the AVAILABLE reader with the given index in the
	* list of available readers
	*/
	public String getAvailableReaderId(int idx) {
		if (idx < 0 || idx >= getAvailableReaderCount())
			return null;
		return readers[available.elementAt(idx)][0];
	}

	/**
	* Returns the class name of the AVAILABLE reader with the given index in the
	* list of available readers
	*/
	protected String getAvailableReaderClassName(int idx) {
		if (idx < 0 || idx >= getAvailableReaderCount())
			return null;
		return readers[available.elementAt(idx)][1];
	}

	/**
	* Returns the short name of the AVAILABLE reader with the given index in the
	* list of available readers
	*/
	public String getAvailableReaderName(int idx) {
		if (idx < 0 || idx >= getAvailableReaderCount())
			return null;
		return readers[available.elementAt(idx)][2];
	}

	/**
	* Returns the description (extended name) of the AVAILABLE reader with the
	* given index in the list of available readers
	*/
	public String getAvailableReaderDescr(int idx) {
		if (idx < 0 || idx >= getAvailableReaderCount())
			return null;
		return readers[available.elementAt(idx)][3];
	}

	/**
	* Constructs an instance of a data reader according to the given identifier
	* of the reader.
	*/
	public DataReader constructReader(String id) {
		int midx = getReaderIndex(id);
		if (midx < 0) {
			//following text:"No reader with the identifier "
			err = res.getString("No_reader_with_the") + id + "!";
			return null;
		}
		String className = readers[midx][1];
		if (className == null) {
			//following text:"No class specified for the reader "
			err = res.getString("No_class_specified") + id + "!";
			return null; //the reader is not implemented
		}
		try {
			Object obj = Class.forName(className).newInstance();
			if (obj != null) {
				if (!(obj instanceof DataReader)) {
					//following text:" is not a DataReader!"
					err = className + res.getString("is_not_a_DataReader_");
					return null;
				}
				return (DataReader) obj;
			}
		} catch (Exception e) {
			err = e.toString();
			//System.out.println(err);
		}
		return null;
	}

	/**
	* Constructs a data reader for data having the specified format
	*/
	public DataReader getReaderOfFormat(String format) {
		if (format == null) {
			//following text:"The format is not specified"
			err = res.getString("The_format_is_not");
			return null;
		}
		for (String[] element : formatCorr)
			if (format.equalsIgnoreCase(element[0]))
				return constructReader(element[1]);
		//following text:"Unknown data format: "
		err = res.getString("Unknown_data_format_") + format;
		return null;
	}

	/**
	* If construction of a reader failed, returns the message
	* explaining the reason of this.
	*/
	public String getErrorMessage() {
		return err;
	}
}
