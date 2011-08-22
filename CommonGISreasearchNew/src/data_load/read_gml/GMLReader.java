package data_load.read_gml;

import java.awt.TextArea;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

import spade.analysis.system.MultiLayerReader;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.CopyFile;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataTable;
import spade.vis.database.SpatialEntity;
import spade.vis.database.TableContentSupplier;
import spade.vis.dmap.DGeoLayer;
import spade.vis.geometry.MultiGeometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.spec.DataSourceSpec;
import data_load.readers.DataStreamReader;

public class GMLReader extends DataStreamReader implements ContentHandler, ErrorHandler, MultiLayerReader, TableContentSupplier {
	private static final String GML_FILTER = "ru.iitp.gis.gml.GMLFilter";

	private static final String TYPE = "type";
	private static final String TYPE_NAME = "typeName";

	private static final short FEATURE_COLLECTION = 0;
	private static final short DESCRIPTION = 1;
	private static final short NAME = 2;
	private static final short BOUNDED_BY = 3;
	private static final short BOX = 4;
	private static final short FEATURE_MEMBER = 5;
	private static final short FEATURE = 6;
	private static final short PROPERTY = 7;
	private static final short GEOMETRY_PROPERTY = 8;
	private static final short POINT = 9;
	private static final short LINE_STRING = 10;
	private static final short POLYGON = 11;
	private static final short OUTER_BOUNDARY_IS = 12;
	private static final short INNER_BOUNDARY_IS = 13;
	private static final short LINEAR_RING = 14;
	private static final short MULTI_LINE_STRING = 15;
	private static final short MULTI_POINT = 16;
	private static final short MULTI_POLYGON = 17;
	private static final short GEOMETRY_COLLECTION = 18;
	private static final short COORDINATES = 19;
	private static final short COORD = 20;
	private static final short X = 21;
	private static final short Y = 22;

	private static final short LINE_STRING_MEMBER = 23;
	private static final short POLYGON_MEMBER = 24;

	private static final short BOOLEAN = 0;
	private static final short INTEGER = 1;
	private static final short REAL = 2;
	private static final short STRING = 3;

	private static final Hashtable names = new Hashtable();
	private static final Hashtable types = new Hashtable();

	static {
		names.put("FeatureCollection", new Short(FEATURE_COLLECTION));
		names.put("description", new Short(DESCRIPTION));
		names.put("name", new Short(NAME));
		names.put("boundedBy", new Short(BOUNDED_BY));
		names.put("Box", new Short(BOX));
		names.put("featureMember", new Short(FEATURE_MEMBER));
		names.put("Feature", new Short(FEATURE));
		names.put("property", new Short(PROPERTY));
		names.put("geometryProperty", new Short(GEOMETRY_PROPERTY));
		names.put("geometricProperty", new Short(GEOMETRY_PROPERTY));
		names.put("Point", new Short(POINT));
		names.put("LineString", new Short(LINE_STRING));
		names.put("Polygon", new Short(POLYGON));
		names.put("outerBoundaryIs", new Short(OUTER_BOUNDARY_IS));
		names.put("innerBoundaryIs", new Short(INNER_BOUNDARY_IS));
		names.put("LinearRing", new Short(LINEAR_RING));
		names.put("MultiPoint", new Short(MULTI_POINT));
		names.put("MultiLineString", new Short(MULTI_LINE_STRING));
		names.put("MultiPolygon", new Short(MULTI_POLYGON));
		names.put("MultiGeometry", new Short(GEOMETRY_COLLECTION));
		names.put("GeometryCollection", new Short(GEOMETRY_COLLECTION));
		names.put("coordinates", new Short(COORDINATES));
		names.put("coord", new Short(COORD));
		names.put("X", new Short(X));
		names.put("Y", new Short(Y));

		names.put("lineStringMember", new Short(LINE_STRING_MEMBER));
		names.put("polygonMember", new Short(POLYGON_MEMBER));

		types.put("boolean", new Character(AttributeTypes.logical));
		types.put("integer", new Character(AttributeTypes.integer));
		types.put("real", new Character(AttributeTypes.real));
		types.put("string", new Character(AttributeTypes.character));
	}

	protected boolean gml10 = false;
	protected boolean mayAskUser = true;

	//------------- necessary variables and structures for GML parsing -----------
	private XMLReader reader;
	private Hashtable layerTable = new Hashtable();
	// current layer
	private GMLLayer layer;

	private ArrayList coordList;
	private String propertyName;
	private SpatialEntity spe;

	private int id = 0;
	private Stack stack = new Stack();

	private StringBuffer sb = new StringBuffer();
	private StringBuffer errors = new StringBuffer();
	//----------------------------------------------------------------------------
	/**
	* The layers loaded from the GML file
	*/
	protected DGeoLayer layers[] = null;
	/**
	* The tables associated with the loaded layers
	*/
	protected DataTable tables[] = null;
	/**
	* The suppliers of geographic data to the layers (used here because the
	* GMLReader alone cannot suply data to multiple layers)
	*/
	protected PassiveDataSupplier suppliers[] = null;

	@Override
	public boolean loadData(boolean mayAskUser) {
		if (dataError)
			return false;
		if (dataReadingInProgress) {
			waitDataReadingFinish();
			return !dataError;
		}
		setDataReadingInProgress(true);
		errors.setLength(0);
		this.mayAskUser = mayAskUser;
		if (spec == null) {
			if (mayAskUser) {
				String path = browseForFile("Select GML document", "*.*");
				if (path == null) {
					setDataReadingInProgress(false);
					return false;
				}
				spec = new DataSourceSpec();
				spec.source = path;
//ID
				spec.format = gml10 ? "GML10" : "GML20";
//~ID
			} else {
				showMessage("The data source is not specified!", true);
				setDataReadingInProgress(false);
				return false;
			}
		}
		showMessage("Reading data from " + spec.source, false);
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setNamespaceAware(true);
			factory.setValidating(false);
			reader = factory.newSAXParser().getXMLReader();
		} catch (Exception e) {
			showMessage("Could not create XML reader: " + e.getMessage(), true);
			dataError = true;
			setDataReadingInProgress(false);
			return false;
		}
		if (!gml10) {
			try {
				XMLFilter filter = (XMLFilter) Class.forName(GML_FILTER).newInstance();
				filter.setParent(reader);
				String s = spec.source.replace('\\', '/');
				s = s.substring(0, s.lastIndexOf('/') + 1);
				filter.setProperty("documentBase", s);
				reader = filter;
			} catch (Exception e) {
				showMessage("Could not create GMLFilter: " + e.getMessage(), true);
				dataError = true;
				setDataReadingInProgress(false);
				return false;
			}
		}
		reader.setContentHandler(this);
		reader.setErrorHandler(this);
		try {
			reader.parse(gml10 ? new NODTDInputSource(spec.source) : new InputSource(spec.source));
//      reader.parse(new InputSource(spec.source));
		} catch (IOException e) {
			showMessage("Could not load " + spec.source + ": " + e.getMessage(), true);
			dataError = true;
			setDataReadingInProgress(false);
			return false;
		} catch (SAXException e) {
			showMessage("Could not process " + spec.source + ": " + e.getMessage(), true);
			e.printStackTrace();
			dataError = true;
			setDataReadingInProgress(false);
			return false;
		}
		// clear temporary data
		layer = null;
		coordList = null;
		propertyName = null;
		spe = null;
		sb.setLength(0);
		if (layerTable.isEmpty()) {
			layerTable = null;
			if (errors.length() != 0) {
				showErrorDialog();
			} else {
				showMessage("No appropriate data found in " + spec.source, true);
			}
			setDataReadingInProgress(false);
			return false;
		}
		if (layerTable.size() > 1 && (spec.layersToLoad == null || spec.layersToLoad.size() < 1)) {
			spec.layersToLoad = new Vector(layerTable.size(), 1);
			Enumeration e = layerTable.elements();
			for (int i = 0; i < layerTable.size() && e.hasMoreElements(); i++) {
				GMLLayer gmlLayer = (GMLLayer) e.nextElement();
				DataSourceSpec dss = (DataSourceSpec) spec.clone();
				dss.id = null;
				dss.typeName = gmlLayer.getTypeName();
				dss.name = CopyFile.getName(spec.source) + " - " + dss.typeName;
				spec.layersToLoad.addElement(dss);
			}
		}
		if (layers == null || tables == null) {
			constructLayersAndTables();
		}
		int nLayers = layers.length;
		Enumeration e = layerTable.elements();
		for (int i = 0; i < nLayers && e.hasMoreElements(); i++) {
			GMLLayer gmlLayer = (GMLLayer) e.nextElement();
			gmlLayer.getLayerData().setHasAllData(true);
			gmlLayer.fillDataTable(tables[i]);
			tables[i].setTableContentSupplier(null);
			gmlLayer.linkGeoObjectsToTableRecords(tables[i]);
			if (suppliers != null) {
				suppliers[i].setLayerData(gmlLayer.getLayerData());
			} else {
				layers[i].receiveSpatialData(gmlLayer.getLayerData());
			}
		}
		layerTable = null;
		suppliers = null;
		setDataReadingInProgress(false);
		return true;
	}

	/**
	* Constructs layers and tables, sets necessary references to the proper data
	* source specifications, data suppliers, etc.
	*/
	protected void constructLayersAndTables() {
		if (layers != null && tables != null)
			return;
		if (spec == null)
			return;
		int nLayers = 1;
		boolean dataLoaded = layerTable != null && layerTable.size() > 0;
		if (dataLoaded) {
			nLayers = layerTable.size();
		} else if (spec.layersToLoad != null && spec.layersToLoad.size() > 1) {
			nLayers = spec.layersToLoad.size();
		}
		layers = new DGeoLayer[nLayers];
		tables = new DataTable[nLayers];
		if (!dataLoaded) {
			suppliers = new PassiveDataSupplier[nLayers];
		}
		for (int i = 0; i < nLayers; i++) {
			layers[i] = new DGeoLayer();
			tables[i] = new DataTable();
			DataSourceSpec dss = (spec.layersToLoad != null && spec.layersToLoad.size() > i) ? (DataSourceSpec) spec.layersToLoad.elementAt(i) : spec;
			layers[i].setDataSource(dss);
			tables[i].setDataSource(dss);
			if (dss.id != null) {
				layers[i].setContainerIdentifier(dss.id);
			}
			if (dss.name != null) {
				layers[i].setName(dss.name);
			} else if (dss.source != null) {
				layers[i].setName(CopyFile.getName(dss.source));
			}
			tables[i].setName(layers[i].getName());
			if (!dataLoaded) {
				tables[i].setTableContentSupplier(this);
				suppliers[i] = new PassiveDataSupplier();
				suppliers[i].setDataReader(this);
				layers[i].setDataSupplier(suppliers[i]);
			}
		}
	}

	/**
	* Reports how many layers have been loaded (or will be loaded).
	*/
	@Override
	public int getLayerCount() {
		if (layers == null) {
			constructLayersAndTables();
		}
		if (layers == null)
			return 0;
		return layers.length;
	}

	/**
	* Returns the layer with the given index
	*/
	@Override
	public DGeoLayer getMapLayer(int idx) {
		if (layers == null || idx < 0 || idx >= layers.length)
			return null;
		return layers[idx];
	}

	/**
	* Returns the table with the attribute (thematic) data attached to the layer
	* with the given index (if any).
	*/
	@Override
	public DataTable getAttrData(int idx) {
		if (tables == null || idx < 0 || idx >= tables.length)
			return null;
		return tables[idx];
	}

	/**
	* A method from the interface TableContentSupplier.
	* Using this method the table asks its supplier to fill it with the data.
	* The method returns true if the data have been successfully loaded.
	*/
	@Override
	public boolean fillTable() {
		return loadData(false);
	}

	private RealPoint createPoint() {
		RealPoint rp = (RealPoint) coordList.get(0);
		coordList = null;
		return rp;
	}

	private RealPolyline createLine() {
		int numPoints = coordList.size();
		RealPolyline line = new RealPolyline();
		line.p = new RealPoint[numPoints];
		for (int i = 0; i < numPoints; i++) {
			line.p[i] = (RealPoint) coordList.get(i);
		}
		coordList = null;
		return line;
	}

	private void coordinates() {
		String d = ",";
		coordList = new ArrayList();
		StringTokenizer st1 = new StringTokenizer(sb.toString());
		while (st1.hasMoreTokens()) {
			String s = st1.nextToken();
			int i = s.indexOf(d);
			if (i != -1) {
				RealPoint rp = new RealPoint();
				rp.x = new Float(s.substring(0, i)).floatValue();
				rp.y = new Float(s.substring(i + 1)).floatValue();
				coordList.add(rp);
			}
		}
	}

	private void coord() {
		if (coordList == null) {
			coordList = new ArrayList();
		}
		coordList.add(new RealPoint());
	}

	private void x() {
		RealPoint rp = (RealPoint) coordList.get(coordList.size() - 1);
		rp.x = new Float(sb.toString().trim()).floatValue();
	}

	private void y() {
		RealPoint rp = (RealPoint) coordList.get(coordList.size() - 1);
		rp.y = new Float(sb.toString().trim()).floatValue();
	}

	private char attrType(String type) {
		if (type == null)
			return AttributeTypes.character;
		Character t = (Character) types.get(type);
		return t != null ? t.charValue() : AttributeTypes.character;
	}

	//----------------------------------------------------------------------------
	// SAX
	//----------------------------------------------------------------------------

	@Override
	public void startDocument() throws SAXException {
	}

	@Override
	public void endDocument() throws SAXException {
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
	}

	/**
	* The list of layers to be skipped
	*/
	protected Vector layersToSkip = null;

	/**
	* Checks if the layer with the given name (more precisely, TYPENAME) must be
	* skipped.
	*/
	protected boolean mustSkip(String layerName) {
		if (layerName == null)
			return false;
		if (layersToSkip != null && StringUtil.isStringInVectorIgnoreCase(layerName, layersToSkip))
			return true;
		if (spec.layersToLoad == null && spec.typeName == null)
			return false;
		boolean toSkip = true;
		if (spec.layersToLoad != null) {
			for (int i = 0; i < spec.layersToLoad.size() && toSkip; i++) {
				DataSourceSpec dss = (DataSourceSpec) spec.layersToLoad.elementAt(i);
				if (dss.typeName != null && dss.typeName.equalsIgnoreCase(layerName)) {
					toSkip = false;
				}
			}
		} else {
			toSkip = !spec.typeName.equalsIgnoreCase(layerName);
		}
		if (toSkip) {
			if (layersToSkip == null) {
				layersToSkip = new Vector(5, 5);
			}
			layersToSkip.addElement(layerName);
		}
		return toSkip;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		Short tagId = (Short) names.get(qName);
		stack.push(tagId);
		sb.setLength(0);
		switch (tagId.shortValue()) {
		case DESCRIPTION:
		case NAME:
			if (layer != null) {
				propertyName = qName;
				layer.addAttribute(propertyName, AttributeTypes.character);
			}
			break;
		case FEATURE:
			String layerName = atts.getValue(TYPE_NAME);
			layer = (GMLLayer) layerTable.get(layerName);
			if (layer == null && !mustSkip(layerName)) {
				layer = new GMLLayer(layerName);
				layerTable.put(layerName, layer);
			}
			if (layer != null) {
				(layer.recordList).add(new Hashtable());
			}
			break;
		case PROPERTY:
			if (layer != null) {
				propertyName = atts.getValue(TYPE_NAME);
				layer.addAttribute(propertyName, attrType(atts.getValue(TYPE)));
			}
			break;
		case GEOMETRY_PROPERTY:
			if (layer != null) {
				spe = layer.addSpatialEntity();
			} else {
				spe = null;
			}
			break;
		case MULTI_POINT:
		case MULTI_LINE_STRING:
		case MULTI_POLYGON:
		case GEOMETRY_COLLECTION:
			if (spe != null) {
				spe.setGeometry(new MultiGeometry());
			}
			break;
		case COORD:
			coord();
			break;

		default:
			break;
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		short tagId = ((Short) stack.pop()).shortValue();
		if (layer == null)
			return;
		switch (tagId) {
		case DESCRIPTION:
		case NAME:
			layer.addAttributeValue(propertyName, sb.toString());
			break;
		case BOUNDED_BY:
			//setBounds();
			break;
		case PROPERTY:
			layer.addAttributeValue(propertyName, sb.toString());
			break;
		case POINT:
			if (spe != null)
				if (spe.getGeometry() instanceof MultiGeometry) {
					((MultiGeometry) spe.getGeometry()).addPart(createPoint());
				} else {
					spe.setGeometry(createPoint());
				}
			break;
		case LINE_STRING:
			if (spe != null)
				if (spe.getGeometry() instanceof MultiGeometry) {
					((MultiGeometry) spe.getGeometry()).addPart(createLine());
				} else {
					spe.setGeometry(createLine());
				}
			break;
		case LINEAR_RING:
			if (spe != null)
				if (spe.getGeometry() == null) {
					spe.setGeometry(createLine());
				} else if (spe.getGeometry() instanceof RealPolyline) {
					MultiGeometry mg = new MultiGeometry();
					mg.addPart(spe.getGeometry());
					spe.setGeometry(mg);
				} else {
					((MultiGeometry) spe.getGeometry()).addPart(createLine());
				}
			break;
		case COORDINATES:
			coordinates();
			break;
		case X:
			x();
			break;
		case Y:
			y();
			break;
		default:
			break;
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		sb.append(ch, start, length);
	}

	@Override
	public void setDocumentLocator(Locator locator) {
	}

	@Override
	public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
	}

	@Override
	public void processingInstruction(String target, String data) throws SAXException {
	}

	@Override
	public void skippedEntity(String name) throws SAXException {
	}

	@Override
	public void error(SAXParseException e) throws SAXException {
		if (e.getClass().getName().equals("ru.iitp.gis.xml.SchemaNotSpecifiedException")) {
			if (mayAskUser) {
				String schema = browseForFile("Select XML schema file", "*.xsd");
				if (schema != null) {
					try {
						reader.setProperty("http://www.opengis.net/gml", new InputSource(schema));
					} catch (Exception ex) {
						ex.printStackTrace();
						showMessage("Could not load XML schema: " + e.getMessage(), true);
					}
				}
			}
		} else {
			String error = e.getMessage() + ", line " + e.getLineNumber() + (e.getColumnNumber() != -1 ? ", column " + e.getColumnNumber() : "");
			showMessage(error, true);
		}
	}

	@Override
	public void fatalError(SAXParseException e) throws SAXException {
		showMessage("Could not process document: " + e.getMessage(), true);
	}

	@Override
	public void warning(SAXParseException e) throws SAXException {
		System.out.println("Warning: " + e.getMessage());
	}

	@Override
	protected void showMessage(String msg, boolean error) {
		super.showMessage(msg, error);
		if (error) {
			errors.append(msg + "\n");
		}
	}

	void showErrorDialog() {
		OKDialog dlg = new OKDialog(ui.getMainFrame(), "Error", false);
		dlg.addContent(new TextArea(errors.toString()));
		dlg.show();
	}
}
