package data_load.readers;

import java.util.ResourceBundle;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import spade.lib.lang.Language;
import spade.lib.util.CopyFile;
import spade.vis.database.Attribute;
import spade.vis.database.DataPortion;
import spade.vis.database.DataRecord;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DMovingObject;
import spade.vis.dmap.GeoToXML;
import spade.vis.spec.DataSourceSpec;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 11-Nov-2005
 * Time: 16:11:19
 * Loads geographical objects and associated thematic data from an XML file.
 */
public class XMLReader extends TableReader {
	static ResourceBundle res = Language.getTextResource("data_load.readers.Res");

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
		if (spec == null)
			if (mayAskUser) {
				//following text:"Select the file with the table"
				String path = browseForFile(res.getString("Select_the_file_with2"), "*.xml");
				System.out.println("Path=" + path);
				if (path == null) {
					setDataReadingInProgress(false);
					return false;
				}
				spec = new DataSourceSpec();
				spec.source = path;
			} else {
				//following text:"The table data source is not specified!"
				showMessage(res.getString("The_table_data_source"), true);
				setDataReadingInProgress(false);
				return false;
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
		dataError = false;
		Document doc = null;
		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			doc = docBuilder.parse(stream);
			// normalize text representation
			doc.getDocumentElement().normalize();
		} catch (SAXParseException err) {
			System.out.println("** Parsing error" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId());
			System.out.println("   " + err.getMessage());
			showMessage(err.getMessage(), true);
			// print stack trace as below
		} catch (SAXException e) {
			Exception x = e.getException();
			((x == null) ? e : x).printStackTrace();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		closeStream();
		setDataReadingInProgress(false);
		if (doc == null) {
			dataError = true;
			return false;
		}
		Element classElem = getElementByTagName(doc.getDocumentElement().getChildNodes(), "ObjectData");
		if (classElem == null) {
			showMessage("No element with the tag <ObjectData> found!", true);
			dataError = true;
			return false;
		}
		NodeList children = classElem.getElementsByTagName("Object");
		if (children == null || children.getLength() < 1 || children.item(0) == null || children.item(0).getNodeType() != Node.ELEMENT_NODE) {
			showMessage("No object descriptions found!", true);
			dataError = true;
			return false;
		}
		Vector attrList = null;
		boolean hasGeo = false;
		Vector gObjects = null; //trajectories, if occur
		for (int i = 0; i < children.getLength(); i++) {
			Element objNode = (Element) children.item(i);
			DGeoObject gObj = GeoToXML.getDGeoObject(objNode);
			if (gObj == null) {
				continue;
			}
			if (gObjects == null) {
				gObjects = new Vector(200, 100);
			}
			if (gObj.getIdentifier() == null || gObj.getIdentifier().equalsIgnoreCase("_temporary_")) {
				gObj.setIdentifier(String.valueOf(gObjects.size() + 1));
			}
			gObjects.addElement(gObj);
			hasGeo = gObj.getSpatialData() != null && gObj.getGeometry() != null;
			if (gObj.getData() != null) {
				DataRecord drec = (DataRecord) gObj.getData();
				if (attrList == null || attrList.size() < 1) {
					attrList = drec.getAttrList();
				} else {
					for (int k = 0; k < drec.getAttrCount(); k++) {
						Attribute at = drec.getAttribute(k);
						String aName = at.getName();
						boolean found = false;
						for (int j = 0; j < attrList.size() && !found; j++) {
							Attribute at1 = (Attribute) attrList.elementAt(j);
							if (aName.equalsIgnoreCase(at1.getName())) {
								found = true;
								at.setIdentifier(at1.getIdentifier()); //to be sure that the attributes are identical
							}
						}
						if (!found) {
							attrList.addElement(at);
						}
					}
				}
			}
		}
		if (gObjects == null || gObjects.size() < 1) {
			showMessage("No object descriptions found!", true);
			dataError = true;
			return false;
		}
		if (attrList != null && attrList.size() > 0) {
			if (table == null) {
				constructTable();
			}
			Vector attrIds = new Vector(attrList.size(), 1);
			for (int j = 0; j < attrList.size(); j++) {
				Attribute attr = (Attribute) attrList.elementAt(j);
				attrIds.addElement(attr.getIdentifier());
				table.addAttribute(attr.getName(), attr.getIdentifier(), attr.getType());
			}
			for (int i = 0; i < gObjects.size(); i++) {
				DGeoObject gobj = (DGeoObject) gObjects.elementAt(i);
				DataRecord rec = (DataRecord) gobj.getData();
				if (rec == null) {
					continue;
				}
				DataRecord rec1 = new DataRecord(rec.getId(), rec.getName());
				for (int j = 0; j < attrIds.size(); j++) {
					rec1.addAttrValue(rec.getAttrValue((String) attrIds.elementAt(j)));
				}
				table.addDataRecord(rec1);
				gobj.setThematicData(rec1);
			}
			if (table.hasData()) {
				table.finishedDataLoading();
			}
		}
		if (table != null && !table.hasData()) {
			table = null;
		}
		if (hasGeo) {
/*
      data=new LayerData();
      for (int i=0; i<gObjects.size(); i++)
        data.addItemSimple(((DGeoObject)gObjects.elementAt(i)).getSpatialData());
*/
			if (layer == null) {
				layer = new DGeoLayer();
				layer.setDataSource(spec);
				if (spec.name != null) {
					layer.setName(spec.name);
				} else if (spec.source != null) {
					layer.setName(CopyFile.getName(spec.source));
				}
			}
			layer.setGeoObjects(gObjects, true);
			layer.setHasMovingObjects(gObjects.elementAt(0) instanceof DMovingObject);
			layer.setDataTable(table);
		}
		return table != null || layer != null;
	}

	/**
	* Returns the map layer constructed from the specifications contained in the
	* XML file (if any). If the XML file contains no geographical data, returns null.
	* If the XML file has not been loaded yet, the reader constructs a layer without
	* objects and sets itself as the  data supplier of the layer. Then the data
	* will be loaded when the layer is first drawn.
	*/
	@Override
	public DGeoLayer getMapLayer() {
		if (layer != null)
			return layer;
		if (spec == null)
			return null;
		layer = new DGeoLayer();
		layer.setDataSource(spec);
		if (spec.name != null) {
			layer.setName(spec.name);
		} else if (spec.source != null) {
			layer.setName(CopyFile.getName(spec.source));
		}
		if (data != null) {
			layer.receiveSpatialData(data);
		} else {
			loadData(false);
		}
		return layer;
	}

	/**
	* Returns the SpatialDataPortion containing all DataItems available
	*/
	@Override
	public DataPortion getData() {
		if (data != null)
			return data;
		if (dataError)
			return null;
		if (table == null || !table.hasData()) {
			if (dataReadingInProgress) {
				waitDataReadingFinish();
			} else {
				loadData(false);
			}
			if (data != null)
				return data;
		}
		return data;
	}

	protected Element getElementByTagName(NodeList nodes, String tagName) {
		if (nodes == null || nodes.getLength() < 1 || tagName == null)
			return null;
		if (nodes instanceof Element) {
			Element element = (Element) nodes;
			if (element.getTagName().equalsIgnoreCase(tagName))
				return element;
		}
		for (int i = 0; i < nodes.getLength(); i++)
			if (nodes.item(i) != null && nodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) nodes.item(i);
				if (element.getTagName().equalsIgnoreCase(tagName))
					return element;
			}
		return null;
	}

}
