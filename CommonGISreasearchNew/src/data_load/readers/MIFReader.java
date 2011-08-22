/**
 * this file reader reads avery data and specification of data which is in a mif file
 * but not the corresponding mid file (which usually has the same name)
 */
package data_load.readers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.analysis.system.AttrDataReader;
import spade.analysis.system.CompositeDataReader;
import spade.analysis.system.DataReader;
import spade.analysis.system.DataReaderFactory;
import spade.analysis.system.GeoDataReader;
import spade.lib.lang.Language;
import spade.lib.util.CopyFile;
import spade.lib.util.IntArray;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataPortion;
import spade.vis.database.DataRecord;
import spade.vis.database.DataSupplier;
import spade.vis.database.DataTable;
import spade.vis.database.LayerData;
import spade.vis.database.SpatialEntity;
import spade.vis.database.TableContentSupplier;
import spade.vis.dmap.DGeoLayer;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.MultiGeometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.geometry.RealRectangle;
import spade.vis.spec.DataSourceSpec;

/**
 * MIFReader Class
 * Reader f?r MIF-Datei Format
 *
 * @author: Waldemar Wansidler (Dialogis)
 * @modified: Thomas Niessen (FhG)
 */

public class MIFReader extends DataStreamReader implements GeoDataReader, AttrDataReader, CompositeDataReader, DataSupplier, TableContentSupplier {
	static ResourceBundle res = Language.getTextResource("data_load.readers.Res");
	/**
	* The spatial data loaded
	*/
	private LayerData data = null;
	/**
	* The DataReaderFactory is used for getting an appropriate reader for
	* the MID file with thematic data
	*/
	protected DataReaderFactory readerFactory = null;
	/**
	* The reader of the object information contained in the MID file
	*/
	protected DataReader midReader = null;
	/**
	* The attribute data loaded
	*/
	protected DataTable table = null;
	/**
	* Indicates an error in thematic data (contained in a MID file)
	*/
	protected boolean attrDataError = false;

	/** Minimale X Koordinate f?r OVL-Datei */
	protected float xMin = (float) 1.0e+100;
	/** Maximale X Koordinate f?r OVL-Datei */
	protected float xMax = (float) -1.0e+100;
	/** Minimale Y Koordinate f?r OVL-Datei */
	protected float yMin = (float) 1.0e+100;
	/** Maximale Y Koordinate f?r OVL-Datei */
	protected float yMax = (float) -1.0e+100;
	/** Vektor graphLines besteht aus Vektoren mit (X,Y) Paar Koordinaten.
	 * Jedes Vektor in graphLines ist eine Polylinie */
	protected Vector graphLines = new Vector();
	/** Headers Z?hler */
	protected int counter = 0;
	/** names of data columns*/
	private String[] colNames = null;
	/** types of data columns (String, int, float, Date, boolean)*/
	private String[] colTypes = null;
	/** could data columns be read correctly */
	private boolean hasDataSpec = false;
	/** number of data columns */
	private int numCol = 0;
	/** delimiter for data in MID file (default: TAB) */
	private String delimiter = "\t";

	/**
	* The bounding rectangles of the objects loaded. Used for sorting the objects
	* according to their inclusions.
	*/
	private Vector rectangles = null;
	/**
	 *  filed numbers for  id- and name-field
	 */
	private int idFieldN = -1, nameFieldN = -1;
	/**
	 *  is true if data preview loading
	 */
	private boolean isDataPreview = false;

	public String getDelimiter() {
		return delimiter;
	}

	/**
	* Returns if a corresponding MID file exists
	*/
	public boolean hasDataSpec() {
		return hasDataSpec;
	}

	/**
	* Returns the number of columns in the MID file
	*/
	public int getNumColumns() {
		return numCol;
	}

	/**
	* Returns an array of String containing the column types
	* The names correspond to the datatypes in java (int, String, Date, float, boolean)
	* @return String[]
	*/
	public String[] getDataTypes() {
		return colTypes;
	}

	/**
	* Returns an array of String containing the column names
	* @return String[]
	*/
	public String[] getDataNames() {
		return colNames;
	}

	/**
	 * ?ffnet Datei und liest alle Daten.
	 */
	public boolean loadGFile() {
		if (stream == null)
			return false;
		try {
			load(stream);
			//following text:": reading finished"
			showMessage(spec.source + res.getString("_reading_finished"), false);
			//if (!Protocol.trace1) System.out.println("done");
		} catch (IOException ioe) {
			showMessage(ioe.toString(), true);
			return false;
		} catch (Exception e) {
			//following text:"Error: "
			showMessage(res.getString("Error_") + e.toString(), true);
			//Protocol.println();
			//Protocol.println("     " + e);
			return false;
		}
		return data != null;
	}

	/**
	 * load Methode liest alle Daten aus MIF/MID-Datei
	 * @param inBuf java.io.BufferedReader - MIF/MID-Datei
	 */
	private void load(InputStream inStream) throws IOException {
		BufferedReader inBuf = new BufferedReader(new InputStreamReader(inStream));
		String workStr = inBuf.readLine();
		/* sucht delimiter & "columns"-Block */
		while (workStr != null) {
			workStr = workStr.trim().toLowerCase();
			if (workStr.indexOf("columns") != -1 || workStr.indexOf("delimiter") != -1) {
				if (workStr.indexOf("delimiter") != -1) {
					int start = workStr.indexOf("\"");
					int end = workStr.lastIndexOf("\"");
					this.delimiter = workStr.substring(start + 1, end);
				} else {
					workStr = readColumnsSpec(inBuf, workStr); // stores columnsnames & -types in arrays
					break;
				}
			}
			workStr = inBuf.readLine();
		}
		/* sucht "data"-Block */
		while (workStr != null) {
			workStr = workStr.trim().toLowerCase();
			if (workStr.equals("data")) {
				break;
			}
			workStr = inBuf.readLine();
		}

		/* wenn "data"-Block ist nicht gefunden, dann gibt's keine graphische Objekte */
		if (workStr == null)
			return;
		workStr = inBuf.readLine();
		//following text:"Reading the coordinates..."
		showMessage(res.getString("Reading_the"), false);
		data = new LayerData();

		while (workStr != null) {
			workStr = workStr.trim().toUpperCase();
			/* wenn Linie, dann hinzuf?gen Linie */
			if (workStr.indexOf("LINE") == 0) {
				loadLine(workStr);
			} else if (workStr.indexOf("PLINE") == 0) {
				loadPolyline(inBuf, workStr);
			} else if (workStr.indexOf("REGION") == 0) {
				loadRegion(inBuf, workStr);
			} else if (workStr.indexOf("POINT") == 0) {
				loadPoint(workStr);
			}
			workStr = inBuf.readLine();
			if (data.getDataItemCount() % 50 == 0) {
				//following text:" contours read"
				showMessage(String.valueOf(data.getDataItemCount()) + res.getString("contours_read"), false);
			}
			if (isDataPreview && data.getDataItemCount() > 1) {
				break;
			}
		}
		inBuf.close();
	}

	/**
	* readColumnsSpec Methode liest Namen und Typen der Daten-Spalten ein
	* @param inBuf java.io.BufferedReader - MIF/MID-Datei
	* @param workStr java.lang.String - aktuelle Zeile
	* @return last String that was read
	*/
	private String readColumnsSpec(BufferedReader inBuf, String workStr) throws IOException {
		StringTokenizer lineTok = new StringTokenizer(workStr, " ");
		int lineTokSize = lineTok.countTokens();
		if (lineTokSize != 2) {
			//following text:"Corruption found in data specification"
			showMessage(res.getString("Corruption_found_in"), true);
			attrDataError = true;
			return workStr;
		}
		workStr = lineTok.nextToken().trim();
		workStr = lineTok.nextToken().trim(); // returns number of columns
		int numCol = Integer.parseInt(workStr);
		colNames = new String[numCol];
		colTypes = new String[numCol];
		int line = 0;
		while (!(workStr = inBuf.readLine()).trim().equalsIgnoreCase("data") && workStr != null) {
			lineTok = new StringTokenizer(workStr, " ");
			lineTokSize = lineTok.countTokens();
			if (lineTokSize < 2) { // no regular data specification
				//following text:"Corruption found in data specification"
				showMessage(res.getString("Corruption_found_in"), true);
				attrDataError = true;
				colNames = null;
				colTypes = null;
				return workStr;
			}
			workStr = lineTok.nextToken().trim();
			colNames[line] = workStr;
			workStr = lineTok.nextToken().trim().toLowerCase();
			lineTok = new StringTokenizer(workStr, "(");
			colTypes[line] = lineTok.nextToken().trim().toLowerCase();
			line++;
		}
		if (line < numCol) {
			numCol = line;
			String cn[] = colNames, ct[] = colTypes;
			colNames = new String[numCol];
			colTypes = new String[numCol];
			for (int i = 0; i < numCol; i++) {
				colNames[i] = cn[i];
				colTypes[i] = ct[i];
			}
		}
		this.hasDataSpec = true;
		this.numCol = numCol;
		return workStr;
	}

	private void loadPoint(String lineStr) {
		StringTokenizer lineTok = new StringTokenizer(lineStr, " ");
		int lineTokSize = lineTok.countTokens();
		if (lineTokSize == 3) {
			String workStr = lineTok.nextToken().trim(); // returns "POINT"
			workStr = lineTok.nextToken().trim();
			/* ?bersetzen von String in Float Format */
			Float xCoord = new Float(workStr);
			workStr = lineTok.nextToken().trim();
			Float yCoord = new Float(workStr);
			/* wenn coord > maximale X Koordinate, dann setzen xMax */
			if (xCoord.floatValue() > xMax) {
				xMax = xCoord.floatValue();
			}
			/* wenn coord < minimale X Koordinate, dann setzen xMin */
			if (xCoord.floatValue() < xMin) {
				xMin = xCoord.floatValue();
			}
			/* wenn coord > maximale Y Koordinate, dann setzen yMax */
			if (yCoord.floatValue() > yMax) {
				yMax = yCoord.floatValue();
			}
			/* wenn coord < minimale Y Koordinate, dann setzen yMin */
			if (yCoord.floatValue() < yMin) {
				yMin = yCoord.floatValue();
			}

			RealPoint rp = new RealPoint();
			rp.x = xCoord.floatValue();
			rp.y = yCoord.floatValue();
			addSpatialEntity(rp);
		}
	}

	/**
	* loadLine Methode hinzuf?gt Linie in graphLines Vektoren
	* und ruft dann makeRealPolyLine auf
	* Syntax: LINE  x1 y1 x2 y2.
	* @param: lineStr java.lang.String - Linie String
	*/
	private void loadLine(String lineStr) throws IOException {
		/* Linie teilen, jede Token als jede einzelne  X oder Y Koordinate */
		StringTokenizer lineTok = new StringTokenizer(lineStr, " ");
		int lineTokSize = lineTok.countTokens();
		Vector graphLine = new Vector();
		for (int i = 0; i < lineTokSize; i++) {
			String workStr = lineTok.nextToken().trim();
			if (i > 0) {
				/* ?bersetzen von String in Float Format */
				Float coord = new Float(workStr);
				graphLine.addElement(coord);
				/* wenn i ist 0, 2, 4, ..., dann coord ist X Koordinate */
				if (i % 2 != 0) {
					/* wenn coord > maximale X Koordinate, dann setzen xMax */
					if (coord.floatValue() > xMax) {
						xMax = coord.floatValue();
					}
					/* wenn coord < minimale X Koordinate, dann setzen xMin */
					if (coord.floatValue() < xMin) {
						xMin = coord.floatValue();
					}
				}
				/* wenn i ist 1, 3, 5, ..., dann coord ist Y Koordinate */
				else {
					/* wenn coord > maximale Y Koordinate, dann setzen yMax */
					if (coord.floatValue() > yMax) {
						yMax = coord.floatValue();
					}
					/* wenn coord < minimale Y Koordinate, dann setzen yMin */
					if (coord.floatValue() < yMin) {
						yMin = coord.floatValue();
					}
				}
			}
		}
		RealPolyline pl = makeRealPolyLine(graphLine, (lineTokSize - 1) / 2, false);
		if (pl != null) {
			addSpatialEntity(pl);
		}
	}

	/**
	* loadPolyline Methode f?gt Polylinie in graphLines Vektoren hinzu
	* und ruft dann addObjectAndLine auf.
	* Syntax: PLINE  [ MULTIPLE numsections ]
	*			numpts1
	*			x1 y1
	*			x2 y2
	*			...
	*			[  numpts 2
	*			 x1 y1
	*			 x2 y2
	*			 ... ]
	*			...
	* @param: inBuf java.io.BufferedReader - MIF/MID-Datei
	* @param: plineStr java.lang.String - Polylinie String
	*/
	private void loadPolyline(BufferedReader inBuf, String plineStr) throws IOException {
		/* teilen plineStr und schauen gibt's eine oder mehrere Sektionen */
		StringTokenizer plineTok = new StringTokenizer(plineStr, " ");
		int plineTokSize = plineTok.countTokens();
		/* Koordinaten Anzahl */
		int nums = -1;
		/* Sektionen Anzahl */
		int numSection = 1;
		boolean multiple = false;
		for (int i = 0; i < plineTokSize; i++) {
			String workStr = plineTok.nextToken().trim();
			/* wenn gibt's mehrere Sektionen, dann setzen multiple */
			if (i == 1 && workStr.equals("MULTIPLE")) {
				multiple = true;
			} else if (i == 1 || i == 3) {
				nums = (new Integer(workStr)).intValue();
			}
			/* wenn gibt's mehrere Sektionen, dann setzen Sektionen Anzahl */
			if (i == 2 && multiple) {
				numSection = (new Integer(workStr)).intValue();
			}
		}
		/* f?r jede Sektion ... */
		MultiGeometry mgeom = null;
		if (numSection > 1) {
			mgeom = new MultiGeometry();
		}
		Geometry geom = mgeom;
		for (int i = 0; i < numSection; i++) {
			/* ... ein graphLine Vektor hinzuf?gen */
			RealPolyline pl = addObjectAndLine(inBuf, nums, false);
			if (pl != null)
				if (mgeom != null) {
					mgeom.addPart(pl);
				} else {
					geom = pl;
				}
			nums = -1;
		}
		if (geom != null) {
			addSpatialEntity(geom);
		}
	}

	/**
	* loadRegion Methode f?gt Region in graphLines Vektoren hinzu
	* und ruft dan addObjectAndLine auf.
	* Syntax: REGION  numpolygons
	*			numpts1
	*			x1 y1
	*			x2 y2
	*			...
	*			[  numpts 2
	*			 x1 y1
	*			 x2 y2
	*			 ... ]
	*			...
	* @param: inBuf java.io.BufferedReader - MIF/MID-Datei
	* @param: regionStr java.lang.String - Region String
	*/
	private void loadRegion(BufferedReader inBuf, String regionStr) throws IOException {
		/* teilen Region String und schauen wie viele Polygons gibt's in diesem Region */
		StringTokenizer regionTok = new StringTokenizer(regionStr, " ");
		int regionTokSize = regionTok.countTokens();
		/* Polygons Anzahl */
		int numPolygons = 0;
		for (int i = 0; i < regionTokSize; i++) {
			String workStr = regionTok.nextToken().trim();
			/* setzen Polygons Anzahl */
			if (i == 1) {
				numPolygons = (new Integer(workStr)).intValue();
			}
		}
		/* f?r jeden Polygon ... */
		MultiGeometry mgeom = null;
		if (numPolygons > 1) {
			mgeom = new MultiGeometry();
		}
		Geometry geom = mgeom;
		for (int i = 0; i < numPolygons; i++) {
			/* ... hinzuf?gen ein graphObject und ein graphLine Vektor */
			RealPolyline pl = addObjectAndLine(inBuf, -1, true);
			if (pl != null)
				if (mgeom != null) {
					mgeom.addPart(pl);
				} else {
					geom = pl;
				}
		}
		if (geom != null) {
			addSpatialEntity(geom);
		}
	}

	/**
	* getNextString Methode gibt n?chsten nicht leeren String zur?ck.
	* @param inBuf java.io.BufferedReader - MIF/MID-Datei
	* @return java.lang.String - n?chste nicht leere String
	*/
	private String getNextString(BufferedReader inBuf) throws IOException {
		String workStr = inBuf.readLine().trim();
		while (workStr.length() == 0) {
			workStr = inBuf.readLine().trim();
		}
		return workStr;
	}

	/**
	* addObjectAndLine Methode hinzuf?gt Polylinie oder Polygon in
	* graphObjects und ruft dann makeRealPolyLine auf.
	* @param inBuf java.io.BufferedReader - MIF/MID-Datei
	* @param nums int - Koordinaten Anzahl
	*/
	private RealPolyline addObjectAndLine(BufferedReader inBuf, int nums, boolean isRegion) throws IOException {
		Vector graphLine = new Vector();
		float firstX = 0;
		float firstY = 0;
		Float x = null;
		Float y = null;
		/* wenn Koordinaten Anzahl ist -1, dann lasen die aus Datei */
		if (nums == -1) {
			nums = (new Integer(getNextString(inBuf))).intValue();
		}
		for (int j = 0; j < nums; j++) {
			StringTokenizer coordTok = new StringTokenizer(getNextString(inBuf), " ");
			/* ?bersetzen von String in Float Format */
			x = new Float(coordTok.nextToken());
			/* ?bersetzen von String in Float Format */
			y = new Float(coordTok.nextToken());
			/* f?r Statistik */
			if (j == 0) {
				firstX = x.floatValue();
				firstY = y.floatValue();
			}
			/* hinzuf?gen X Koordinate in graphLine Vektor */
			graphLine.addElement(x);
			/* hinzuf?gen Y Koordinate in graphLine Vektor */
			graphLine.addElement(y);
			/* wenn x > maximale X Koordinate, dann setzen xMax */
			if (x.floatValue() > xMax) {
				xMax = x.floatValue();
			}
			/* wenn x < minimale X Koordinate, dann setzen xMin */
			if (x.floatValue() < xMin) {
				xMin = x.floatValue();
			}
			/* wenn y > maximale Y Koordinate, dann setzen yMax */
			if (y.floatValue() > yMax) {
				yMax = y.floatValue();
			}
			/* wenn y < minimale Y Koordinate, dann setzen yMin */
			if (y.floatValue() < yMin) {
				yMin = y.floatValue();
			}
		}
		boolean closed = isRegion || (firstX == x.floatValue() && firstY == y.floatValue());
		return makeRealPolyLine(graphLine, nums, closed);
	}

	/**
	* makeRealPolyLine Method creates a RealPolyLine-object for Descartes
	* @param vec Vector - contains all points
	* @param numPoints int - number of points for polyline
	* @param closed boolean - indicates if polyline is closed
	*/
	private RealPolyline makeRealPolyLine(Vector vec, int numPoints, boolean closed) {
		RealPolyline line = new RealPolyline();
		line.p = new RealPoint[numPoints];
		for (int i = 0; i < numPoints * 2; i += 2) {
			Float fx = (Float) vec.elementAt(i);
			Float fy = (Float) vec.elementAt(i + 1);
			line.p[i / 2] = new RealPoint();
			line.p[i / 2].x = fx.floatValue();
			line.p[i / 2].y = fy.floatValue();
		}
		line.isClosed = closed;
		return line;
		/*
		Geometry g = line;
		SpatialEntity spe = new SpatialEntity(String.valueOf(Math.random()));
		spe.setGeometry(g);
		data.addDataItem(spe);
		*/
	}

	/**
	* addSpatialEntity Method creates an instance of SpatialEntity with the given
	* geometry and adds it to the spatial data portion being constructed
	* @param geom Geometry - the geometry of the new spatial object. May consist
	*                        of multiple polygons, lines, or points, i.e. this
	*                        may be a MultiGeometry
	*/
	private void addSpatialEntity(Geometry geom) {
		if (geom == null)
			return;
		SpatialEntity spe = new SpatialEntity(String.valueOf(data.getDataItemCount()));
		spe.setGeometry(geom);
		data.addItemSimple(spe);
		float[] rr = geom.getBoundRect();
		if (rr != null) {
			if (rectangles == null) {
				rectangles = new Vector(100, 100);
			}
			rectangles.addElement(rr);
		}
	}

	protected boolean readSpecific() {
		if (!loadGFile() || data == null)
			return false;
		data.setBoundingRectangle(xMin, yMin, xMax, yMax);
		if (data.getDataItemCount() > 0) {
			//following text:" spatial entities loaded"
			showMessage(String.valueOf(data.getDataItemCount()) + res.getString("spatial_entities"), false);
			data.setHasAllData(true);
		} else {
			//following text:"No spatial entities loaded!"
			showMessage(res.getString("No_spatial_entities"), true);
			data = null;
			return false;
		}
		if (hasDataSpec && colNames != null) {
			if (table == null) {
				constructTable();
			}
			if (table != null && midReader != null) {
				DataSourceSpec dss = (DataSourceSpec) table.getDataSource();
				dss.delimiter = getDelimiter();
				dss.idFieldN = this.idFieldN;
				dss.nameFieldN = this.nameFieldN;

				for (int i = 0; i < colNames.length && (dss.idFieldN < 0 || dss.nameFieldN < 0); i++)
					if (dss.idFieldN < 0 && (colNames[i].equalsIgnoreCase("ID") || colNames[i].equalsIgnoreCase("IDENT") || colNames[i].equalsIgnoreCase("IDENTIFIER"))) {
						dss.idFieldN = i;
					} else if (dss.nameFieldN < 0 && colNames[i].equalsIgnoreCase("NAME")) {
						dss.nameFieldN = i;
					}

				if (midReader.loadData(false) && table.hasData()) {
					//set attribute names and types
					int k = -1;
					for (int i = 0; i < colNames.length; i++)
						if (i != dss.idFieldN && i != dss.nameFieldN) {
							++k;
							Attribute attr = table.getAttribute(k);
							attr.setName(colNames[i]);
							attr.setType(translateType(colTypes[i]));
						}

					//link the table records to the spatial entities
					//Vector attrList=table.getAttrList();
					for (int i = 0; i < table.getDataItemCount() && i < data.getDataItemCount(); i++) {
						DataRecord rec = table.getDataRecord(i);
						//rec.setAttrList(attrList);
						SpatialEntity spe = (SpatialEntity) data.getDataItem(i);
						if (!rec.getId().equals(spe.getId())) {
							spe.setId(rec.getId());
						}
						if (rec.getName() != null) {
							spe.setName(rec.getName());
						}
						spe.setThematicData(rec);
					}
					System.out.println("Attributes in table " + table.getName() + ":");
					for (int i = 0; i < table.getAttrCount(); i++) {
						System.out.println(i + ") [" + table.getAttributeName(i) + "] " + table.getAttributeType(i) + " isNumeric=" + AttributeTypes.isNumericType(table.getAttributeType(i)));
					}
				}
			}
		} else {
			attrDataError = true;
			spec.objDescrSource = null;
			if (table != null) {
				table.setDataSource(null);
				table.setTableContentSupplier(null);
			}
		}
		sortSpatialEntities();
		return true;
	}

	/**
	* Sorts spatial entities according to inclusion of their bounding rectangles.
	* Must be called after attaching thematic data!!!
	*/
	protected void sortSpatialEntities() {
		if (data == null || rectangles == null || data.getDataItemCount() < 2 || rectangles.size() < 2)
			return;
		IntArray nums = new IntArray(rectangles.size(), 1);
		nums.addElement(0);
		for (int i = 1; i < rectangles.size(); i++) {
			float[] r1 = (float[]) rectangles.elementAt(i);
			boolean inserted = false;
			for (int j = 0; j < nums.size() && !inserted; j++) {
				float[] r2 = (float[]) rectangles.elementAt(nums.elementAt(j));
				if (r1[0] <= r2[0] && r1[1] <= r2[1] && r1[2] >= r2[2] && r1[3] >= r2[3]) {
					nums.insertElementAt(i, j);
					inserted = true;
				}
			}
			if (!inserted) {
				nums.addElement(i);
			}
		}
		if (nums.size() < data.getDataItemCount()) {
			for (int i = 0; i < data.getDataItemCount() && nums.size() < data.getDataItemCount(); i++)
				if (nums.indexOf(i) < 0) {
					nums.addElement(i);
				}
		}
		LayerData data1 = new LayerData();
		for (int i = 0; i < nums.size(); i++) {
			data1.addItemSimple((SpatialEntity) data.getDataItem(nums.elementAt(i)));
		}
		RealRectangle b = data.getBoundingRectangle();
		if (b != null) {
			data1.setBoundingRectangle(b.rx1, b.ry1, b.rx2, b.ry2);
		}
		data1.setHasAllData(true);
		data = data1;
	}

	/**
	* Transforms MIF/MID attribute types to Descartes types
	*/
	protected char translateType(String typeStr) {
		char type = AttributeTypes.character;
		if (typeStr != null)
			if (typeStr.equalsIgnoreCase("char")) {
				type = AttributeTypes.character;
			} else if (typeStr.equalsIgnoreCase("integer") || typeStr.equalsIgnoreCase("smallint")) {
				type = AttributeTypes.integer;
			} else if (typeStr.equalsIgnoreCase("decimal") || typeStr.equalsIgnoreCase("float")) {
				type = AttributeTypes.real;
			} else if (typeStr.equalsIgnoreCase("date")) {
				type = AttributeTypes.time;
			} else if (typeStr.equalsIgnoreCase("logical")) {
				type = AttributeTypes.logical;
			}
		return type;
	}

	/**
	* Sets the DataReaderFactory that can produce an appropriate reader for
	* the MID file with additional thematic data
	*/
	@Override
	public void setDataReaderFactory(DataReaderFactory factory) {
		readerFactory = factory;
	}

	/**
	* Tries to constructs the table in which the information about the geo objects
	* (identifiers, names, attribute data) will be stored. Such information is
	* often contained in a MID file accompanying the MIF file (it usually has
	* the same name as the MIF file). If there is no such file or no appropriate
	* reader, the internal variable table remains null.
	*/

	protected void constructTable() {
		if (attrDataError)
			return;
		if (table != null)
			return;
		if (readerFactory == null)
			return; //no reader for the table is available
		if (spec == null || spec.source == null)
			return; //no data source specification available
		if (readerFactory != null) { //try to get a MID reader
			midReader = readerFactory.getReaderOfFormat("ascii");
			if (midReader instanceof ASCIIReader && isDataPreview) {
				((ASCIIReader) midReader).maxLines = 20;
			}
			if (midReader == null || !(midReader instanceof AttrDataReader)) {
				//following text:"No reader found for MID files!"
				showMessage(res.getString("No_reader_found_for"), true);
				return;
			}
		}
		String fname = spec.objDescrSource;
		if (fname == null || !CopyFile.checkExistence(fname)) {
			int idx = spec.source.lastIndexOf('.');
			if (idx < 0) {
				idx = spec.source.length();
			}
			fname = spec.source.substring(0, idx);
			if (CopyFile.checkExistence(fname + ".mid")) {
				fname = fname + ".mid";
			} else if (CopyFile.checkExistence(fname + ".MID")) {
				fname = fname + ".MID";
			} else
				return; //no MID file exist
			spec.objDescrSource = fname;
		}

		DataSourceSpec dss = (DataSourceSpec) spec.clone();
		dss.source = fname;
		spec.objDescrSource = dss.source;
		dss.objDescrSource = null;
		dss.format = "ascii";
		dss.nRowWithFieldNames = -1;
		dss.nRowWithFieldTypes = -1;
		midReader.setDataSource(dss);
		midReader.setUI(ui);
		AttrDataReader attrReader = (AttrDataReader) midReader;
		table = attrReader.getAttrData();
		//It is important that MIF file (containing geography) is read prior to
		//the MID file containing attribute data because the MIF file contains a
		//description of the attributes. Therefore we attach the MIFReader to the
		//table as its data supplier instead of the MID (ascii) reader
		table.setTableContentSupplier(this);

	}

	/**
	* Loads the data. When allowed (the argument mayAskUser is true) and necessary,
	* starts a dialog or a wizard for the user
	* to specify the data source, constraints, etc. Returns true if the data
	* have been successfully loaded.
	*/
	@Override
	public boolean loadData(boolean mayAskUser) {
		if (dataError)
			return false;
		if (dataReadingInProgress) {
			waitDataReadingFinish();
			return !dataError;
		}
		if (!mayAskUser) {
			setDataReadingInProgress(true);
		}
		if (spec == null || spec.source == null)
			if (mayAskUser) {
				//following text:"Select the file with geographical data"
				String path = browseForFile(res.getString("Select_the_file_with3"), "*.mif");
				if (path == null) {
					setDataReadingInProgress(false);
					return false;
				}
				if (spec == null) {
					spec = new DataSourceSpec();
				}
				spec.source = path;
				isDataPreview = true;
				DataSample sample = getDataSample(20);
				if (sample == null) {
					setDataReadingInProgress(false);
					return false;
				}

				DataPreviewDlg dpd = new DataPreviewDlg(getFrame(), sample, true);
				dpd.show();
				if (dpd.wasCancelled()) {
					setDataReadingInProgress(false);
					return false;
				}
				idFieldN = spec.idFieldN = dpd.getIdFieldN();
				nameFieldN = spec.nameFieldN = dpd.getNameFieldN();
				table.destroy();
				table = null;
				isDataPreview = false;

			} else {
				//following text:"The data source for layer is not specified!"
				showMessage(res.getString("The_data_source_for"), true);
				setDataReadingInProgress(false);
				return false;
			}
		else {
			idFieldN = spec.idFieldN;
			nameFieldN = spec.nameFieldN;
		}
		if (spec.name == null) {
			spec.name = CopyFile.getName(spec.source);
		}
		//following text:"Start reading data from "
		showMessage(res.getString("Start_reading_data") + spec.source, false);
		closeStream();
		openStream(); //to be at the beginning
		if (stream == null) {
			dataError = true;
			setDataReadingInProgress(false);
			return false;
		}
		dataError = !readSpecific();
		closeStream();
		setDataReadingInProgress(false);
		return !dataError;
	}

	/**
	* Returns the map layer constructed from the geographical data loaded (if any).
	* If the data have not been loaded yet, the reader constructs a layer without
	* objects and sets itself as the  data supplier of the layer. Then the data
	* will be loaded when the layer is first darwn.
	*/
	@Override
	public DGeoLayer getMapLayer() {
		DGeoLayer layer = new DGeoLayer();
		layer.setDataSource(spec);
		if (spec.id != null) {
			layer.setContainerIdentifier(spec.id);
		}
		if (spec.name != null) {
			layer.setName(spec.name);
		} else if (spec.source != null) {
			layer.setName(CopyFile.getName(spec.source));
		}
		if (data != null) {
			layer.receiveSpatialData(data);
		} else {
			layer.setDataSupplier(this);
		}
		return layer;
	}

	protected DataSample getDataSample(int maxNRows) {
		DataSample sample = new DataSample();
		if (table == null) {
			constructTable();
		}
		System.out.println(">>>>>>table>>>>>>" + table.getDataItemCount());
		if (table == null)
			return null;
		for (int i = 0; i < table.getAttrCount(); i++) {
			sample.addField(table.getAttributeName(i), table.getAttributeType(i));
		}
		for (int i = 0; i < table.getDataItemCount() && i < maxNRows; i++) {
			sample.addDataRecord(table.getDataRecord(i).getAttrValues());
		}
		return sample;
	}

	/**
	* Returns the table with the attribute (thematic) data loaded (if any)
	*/
	@Override
	public DataTable getAttrData() {
		if (attrDataError)
			return null;
		if (table == null) {
			constructTable();
		}
		return table;
	}

//----------------- TableContentSupplier interface -----------------------------------
	/**
	* It is important that MIF file (containing geography) is read prior to
	* the MID file containing attribute data because the MIF file contains a
	* description of the attributes. Therefore we attach the MIFReader to the
	* table as its data supplier instead of the MID (ascii) reader.
	* Using this method the table asks its supplier to fill it with the data.
	* The method returns true if the data have been successfully loaded.
	*/
	@Override
	public boolean fillTable() {
		if (dataError)
			return false;
		if (data != null && table != null)
			return table.hasData();
		return loadData(false);
	}

//----------------- DataSupplier interface -----------------------------------
	/**
	* Returns the SpatialDataPortion containing all DataItems available
	*/
	@Override
	public DataPortion getData() {
		if (data != null)
			return data;
		if (dataError)
			return null;
		if (loadData(false))
			return data;
		return null;
	}

	/**
	* Constructs and returns a DataPortion containing DataItems fitting in at
	* least one of the specified bounding rectangles (listed in the vector).
	* The elements of the vector are instances of
	* @see spade.vis.geometry.RealRectangle
	* Readers from files do not filter data according to any query,
	* therefore the method getData() without arguments is called
	*/
	@Override
	public DataPortion getData(Vector bounds) {
		return getData();
	}

	/**
	* When no more data from the DataSupplier are needed, this method is
	* called. Here the DataSupplier can clear its internal structures
	*/
	@Override
	public void clearAll() {
		data = null;
	}

}
