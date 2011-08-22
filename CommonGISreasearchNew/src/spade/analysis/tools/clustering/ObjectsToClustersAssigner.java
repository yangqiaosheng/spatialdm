package spade.analysis.tools.clustering;

import java.awt.Color;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import spade.analysis.system.ESDACore;
import spade.analysis.system.Processor;
import spade.lib.basicwin.SelectDialog;
import spade.lib.util.IntArray;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DMovingObject;
import spade.vis.dmap.DrawingParameters;
import spade.vis.dmap.GeoToXML;
import spade.vis.dmap.TrajectoryObject;
import spade.vis.geometry.Geometry;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jan 6, 2009
 * Time: 5:45:33 PM
 * Assigns new objects to existing clusters, using cluster specimens and
 * corresponding distance thresholds
 */
public class ObjectsToClustersAssigner implements Processor {
	/**
	 * Contains information about the clusters, which can be used for assigning new
	 * objects to these clusters
	 */
	public ClustersInfo clustersInfo = null;
	/**
	 * Indicates whether a cluster representative with the closest distance to
	 * the classified object must be found for assigning the object to a cluster.
	 * If this variable is false, the classifier picks the first cluster representative
	 * with the distance to the object within the corresponding threshold.
	 */
	public boolean mustFindClosest = true;
	/**
	 * A user-provided name of the classifier; may be null
	 */
	public String name = null;
	/**
	 * May contain an error message
	 */
	public String err = null;

	public ObjectsToClustersAssigner() {
	}

	public ObjectsToClustersAssigner(ClustersInfo clustersInfo) {
		this.clustersInfo = clustersInfo;
	}

	/**
	 * Returns the name of this tool
	 */
	public String getName() {
		if (name != null)
			return name;
		if (clustersInfo == null || clustersInfo.table == null || clustersInfo.clustersColN < 0)
			return "Assign objects to clusters";
		return "Assign objects to " + clustersInfo.table.getAttributeName(clustersInfo.clustersColN);
	}

	/**
	 * Sets the name of this tool
	 */
	public void setName(String aName) {
		name = aName;
	}

	/**
	 * Returns the class name of the result obtained after processing a single object
	 */
	public String getSingleResultClassName() {
		return ObjectToClusterAssignment.class.getName();
	}

	/**
	 * Returns the class name of the final result obtained after processing all objects
	 */
	public String getFinalResultClassName() {
		return ClustersInfo.class.getName();
	}

	/**
	 * Finds an example of an object to which this classifier may be applied
	 * (retrieves it from one of the specimens).
	 */
	public Object getExampleObject() {
		if (clustersInfo == null || clustersInfo.distanceMeter == null || clustersInfo.getClustersCount() < 1)
			return null;
		Object obj = null;
		for (int i = 0; i < clustersInfo.getClustersCount() && obj == null; i++) {
			SingleClusterInfo clIn = clustersInfo.getSingleClusterInfo(i);
			for (int j = 0; j < clIn.getSpecimensCount() && obj == null; j++) {
				DClusterObject clObj = clIn.getClusterSpecimenInfo(j).specimen;
				DGeoObject gobj = getDGeoObject(clObj);
				if (gobj != null) {
					obj = gobj;
				} else {
					obj = clObj.originalObject;
				}
			}
		}
		return obj;
	}

	/**
	 * Replies whether this processor is applicable to the given type of object
	 * @param objType - the type of the object, which must be equal to one of the
	 *   constants defined in the interface Processor
	 * @return true if applicable
	 */
	public boolean isApplicableTo(int objType) {
		if (clustersInfo == null || clustersInfo.distanceMeter == null || clustersInfo.getClustersCount() < 1)
			return false;
		Object obj = getExampleObject();
		if (obj == null)
			return false;
		if (!(obj instanceof DGeoObject))
			return false;
		DGeoObject gobj = (DGeoObject) obj;
		if (objType == Processor.GEO_TRAJECTORY)
			return (gobj instanceof DMovingObject);
		if (objType == Processor.GEO_POINT)
			return gobj.getSpatialType() == Geometry.point;
		if (objType == Processor.GEO_AREA)
			return gobj.getSpatialType() == Geometry.area;
		if (objType == Processor.GEO_LINE)
			return gobj.getSpatialType() == Geometry.line;
		return false;
	}

	/**
	 * Clears any previous results and prepares for a new run
	 */
	public void clearPreviousResults() {
		for (int clIdx = 0; clIdx < clustersInfo.clusterInfos.size(); clIdx++) {
			SingleClusterInfo clIn = clustersInfo.clusterInfos.elementAt(clIdx);
			if (clIn.specimens != null && clIn.specimens.size() > 0) {
				for (int spIdx = 0; spIdx < clIn.specimens.size(); spIdx++) {
					ClusterSpecimenInfo spec = clIn.specimens.elementAt(spIdx);
					spec.nSimilarNew = 0;
					spec.meanDistNew = 0;
				}
			}
		}
	}

	/**
	 * Initialises what is needed for the operation
	 * The Processor may access any system components, data, maps, etc.
	 * through the provided system core.
	 */
	public void initialise(ESDACore core) {
		clearPreviousResults();
		if (core != null) {
			SelectDialog selDia = new SelectDialog(core.getUI().getMainFrame(), "Classification strategy?", "What strategy must be used for assigning an object to a cluster?");
			selDia.addOption("find the closest prototype among all close cluster prototypes", "closest", mustFindClosest);
			selDia.addOption("pick the first close cluster prototype", "first", !mustFindClosest);
			selDia.addSeparator();
			selDia.addLabel("Note: \"close\" prototype is such a prototype that the distance");
			selDia.addLabel("to the object is within the corresponding distance threshold.");
			selDia.addLabel("Each cluster prototype has its individual distance threshold.");
			selDia.show();
			mustFindClosest = selDia.getSelectedOptionN() == 0;
		}
	}

	/**
	 * Processes the given object
	 * @param obj - the object to process
	 * @return the result of object processing or null if the object cannot be
	 *         processed
	 */
	public Object processObject(Object obj) {
		if (obj == null)
			return null;
		if (!(obj instanceof DGeoObject))
			//System.out.println("Inappropriate object: "+obj);
			return null;
		if (clustersInfo == null || clustersInfo.distanceMeter == null || clustersInfo.clusterInfos == null || clustersInfo.clusterInfos.size() < 1)
			//System.out.println("Internal settings lack!");
			return null;
		Object obj0 = getExampleObject();
		if (!(obj0 instanceof DGeoObject))
			return null;
		if (!obj0.getClass().getName().equals(obj.getClass().getName()))
			//System.out.println("Inappropriate object class: "+obj.getClass().getName());
			return null;
		DGeoObject gobj = (DGeoObject) obj;
		boolean geo = ((DGeoObject) obj0).isGeographic() || gobj.isGeographic();
		gobj.setGeographic(geo);
		DClusterObject clObj = null;
		if (clustersInfo.distanceMeter instanceof LayerClusterer) {
			clObj = ((LayerClusterer) clustersInfo.distanceMeter).makeDClusterObject(gobj, -1);
		} else {
			clObj = new DClusterObject(gobj, gobj.getIdentifier(), -1);
		}
		int clusterN = -1, specIdx = -1;
		double distToSpecimen = Double.NaN;
		//System.out.println("Processing object "+gobj.getIdentifier());
		if (mustFindClosest) { //find the closest specimen
			int clusterIdx = -1;
			for (int clIdx = 0; clIdx < clustersInfo.clusterInfos.size(); clIdx++) {
				SingleClusterInfo clIn = clustersInfo.clusterInfos.elementAt(clIdx);
				if (clIn.specimens != null && clIn.specimens.size() > 0) {
					for (int spIdx = 0; spIdx < clIn.specimens.size(); spIdx++) {
						ClusterSpecimenInfo spec = clIn.specimens.elementAt(spIdx);
						double d = clustersInfo.distanceMeter.distance(clObj, spec.specimen);
						//System.out.println(" -> distance to specimen "+spIdx+" of cluster "+clIn.clusterN+" = "+d+
						//" (threshold = "+spec.distanceThr+")");
						if (!Double.isNaN(d) && d <= spec.distanceThr && (clusterIdx < 0 || d < distToSpecimen)) {
							clusterIdx = clIdx;
							specIdx = spIdx;
							distToSpecimen = d;
						}
					}
				}
			}
			if (clusterIdx >= 0 && specIdx >= 0) {
				SingleClusterInfo clIn = clustersInfo.clusterInfos.elementAt(clusterIdx);
				ClusterSpecimenInfo spec = clIn.specimens.elementAt(specIdx);
				clusterN = clIn.clusterN;
				++spec.nSimilarNew;
				spec.meanDistNew = (spec.meanDistNew * (spec.nSimilarNew - 1) + distToSpecimen) / spec.nSimilarNew;
			}
		} else { //find the first sufficiently close specimen (i.e. the distance is within the threshold)
			for (int clIdx = 0; clIdx < clustersInfo.clusterInfos.size() && clusterN < 0; clIdx++) {
				SingleClusterInfo clIn = clustersInfo.clusterInfos.elementAt(clIdx);
				if (clIn.specimens != null && clIn.specimens.size() > 0) {
					for (int spIdx = 0; spIdx < clIn.specimens.size() && clusterN < 0; spIdx++) {
						ClusterSpecimenInfo spec = clIn.specimens.elementAt(spIdx);
						distToSpecimen = clustersInfo.distanceMeter.distance(clObj, spec.specimen);
						//System.out.println(" -> distance to specimen "+spIdx+" of cluster "+clIn.clusterN+" = "+d+
						//" (threshold = "+spec.distanceThr+")");
						if (!Double.isNaN(distToSpecimen) && distToSpecimen <= spec.distanceThr) {
							clusterN = clIn.clusterN;
							specIdx = spIdx;
							++spec.nSimilarNew;
							spec.meanDistNew = (spec.meanDistNew * (spec.nSimilarNew - 1) + distToSpecimen) / spec.nSimilarNew;
						}
					}
				}
			}
		}
		ObjectToClusterAssignment oclas = new ObjectToClusterAssignment();
		oclas.id = gobj.getIdentifier();
		if (clusterN > 0) {
			//System.out.println("+ object "+gobj.getIdentifier()+" > > > cluster "+clusterN+", specimen "+specIdx);
			oclas.clusterN = clusterN;
			oclas.specimenIdx = specIdx + 1;
			oclas.distance = distToSpecimen;
		}
		return oclas;
	}

	/**
	 * Returns the final result of the processing
	 */
	public Object getResult() {
		return clustersInfo;
	}

	/**
	 * Replies if this processor can operate in an automatic mode,
	 * i.e. without any user interface. This method returns true if the
	 * user interface is optional or not needed at all.
	 */
	public boolean canWorkAutomatically() {
		return true;
	}

	/**
	 * If the Processor has some user interface (possibly, optional),
	 * it is created in this method.
	 */
	public void createUI() {
	}

	/**
	 * Closes the user interface, if it has been previously created.
	 */
	public void closeUI() {
	}

	/**
	 * Stores the information about the classifier in XML format
	 */
	public String toXML() {
		if (clustersInfo == null || clustersInfo.getClustersCount() < 1 || clustersInfo.distanceMeter == null)
			return null;
		StringWriter writer = new StringWriter();
		writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n");

		writer.write("<Classifier");
		if (name != null) {
			writer.write(" name=\"" + name + "\"\r\n");
		}
		writer.write(" mustFindClosest=\"" + mustFindClosest + "\"");
		Object obj = getExampleObject();
		if (obj instanceof DGeoObject) {
			DGeoObject gobj = (DGeoObject) obj;
			if (gobj.isGeographic()) {
				writer.write(" useGeoCoordinates=\"true\"");
			}
		}
		writer.write(" >\r\n\r\n");

		writer.write("<distanceMeter className=\"" + clustersInfo.distanceMeter.getClass().getName() + "\" >\r\n");
		HashMap params = clustersInfo.distanceMeter.getParameters(null);
		if (params != null && !params.isEmpty()) {
			Set keys = params.keySet();
			if (keys != null) {
				for (Iterator it = keys.iterator(); it.hasNext();) {
					Object key = it.next(), value = params.get(key);
					if (key != null && value != null) {
						writer.write("\t<parameter name=\"" + key.toString() + "\">" + value.toString() + "</parameter>\r\n");
					}
				}
			}
		}
		writer.write("</distanceMeter>\r\n\r\n");

		for (int i = 0; i < clustersInfo.getClustersCount(); i++) {
			SingleClusterInfo clIn = clustersInfo.getSingleClusterInfo(i);
			writer.write("<cluster clusterN=\"" + clIn.clusterN + "\"");
			writer.write(" clusterLabel=\"" + ((clIn.clusterLabel == null) ? String.valueOf(clIn.clusterN) : clIn.clusterLabel) + "\"");
			if (clIn.origSize > 0) {
				writer.write(" origSize=\"" + clIn.origSize + "\"");
			}
			writer.write(" >\r\n");
			for (int j = 0; j < clIn.getSpecimensCount(); j++) {
				ClusterSpecimenInfo spec = clIn.getClusterSpecimenInfo(j);
				writer.write("\t<specimen distanceThr=\"" + spec.distanceThr + "\"");
				if (spec.nSimilarOrig > 0) {
					writer.write(" nSimilarOrig=\"" + spec.nSimilarOrig + "\"");
				}
				if (spec.meanDistOrig > 0) {
					writer.write(" meanDistOrig=\"" + spec.meanDistOrig + "\"");
				}
				writer.write(" >\r\n");
				try {
					GeoToXML.writeGeoObject(getDGeoObject(spec.specimen), writer);
				} catch (IOException ioe) {
				}
				writer.write("\t</specimen>\r\n");
			}
			writer.write("</cluster>\r\n\r\n");
		}

		writer.write("</Classifier>\r\n\r\n");
		return writer.toString();
	}

	public DGeoObject getDGeoObject(DClusterObject o) {
		if (o == null)
			return null;
		if (o.originalObject instanceof TrajectoryObject)
			return ((TrajectoryObject) o.originalObject).mobj;
		if (o.originalObject instanceof DGeoObject)
			return (DGeoObject) o.originalObject;
		return null;
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

	/**
	 * Checks if this type of processor can be restored from the given
	 * XML document
	 */
	public boolean canRestoreFromXML(Document doc) {
		if (doc == null)
			return false;
		Element classElem = getElementByTagName(doc.getDocumentElement().getChildNodes(), "Classifier");
		if (classElem == null)
			return false;
		Element dmDescr = getElementByTagName(classElem.getChildNodes(), "distanceMeter");
		if (dmDescr == null)
			return false;
		return true;
	}

	/**
	 * Restores itself (i.e. all necessary internal settings) from
	 * the given XML document. Returns true if successful.
	 */
	public boolean restoreFromXML(Document doc) {
		err = null;
		if (doc == null)
			return false;
		Element classElem = getElementByTagName(doc.getDocumentElement().getChildNodes(), "Classifier");
		if (classElem == null) {
			err = "No element with the tag <Classifier> found!";
			return false;
		}
		if (classElem.hasAttribute("name")) {
			setName(classElem.getAttribute("name"));
		}
		if (classElem.hasAttribute("mustFindClosest")) {
			mustFindClosest = GeoToXML.getBoolean(classElem.getAttribute("mustFindClosest"));
		}
		boolean useGeoCoordinates = false;
		if (classElem.hasAttribute("useGeoCoordinates")) {
			useGeoCoordinates = GeoToXML.getBoolean(classElem.getAttribute("useGeoCoordinates"));
		}
		Element dmDescr = getElementByTagName(classElem.getChildNodes(), "distanceMeter");
		if (dmDescr == null) {
			err = "No distance meter description found!";
			return false;
		}
		if (!dmDescr.hasAttribute("className")) {
			err = "No class name of the distance meter found!";
			return false;
		}
		DistanceMeterExt dm = null;
		try {
			Object distMeter = Class.forName(dmDescr.getAttribute("className")).newInstance();
			if (distMeter instanceof DistanceMeterExt) {
				dm = (DistanceMeterExt) distMeter;
			}
		} catch (Exception e) {
			err = "Could not construct an instance of distance meter; class name = " + dmDescr.getAttribute("className");
			return false;
		}
		if (dm == null) {
			err = "Could not construct an instance of distance meter; class name = " + dmDescr.getAttribute("className");
			return false;
		}
		NodeList paramNodes = dmDescr.getElementsByTagName("parameter");
		if (paramNodes != null && paramNodes.getLength() > 0) {
			HashMap params = new HashMap(Math.max(20, paramNodes.getLength()));
			for (int i = 0; i < paramNodes.getLength(); i++) {
				Element paramElement = (Element) paramNodes.item(i);
				if (!paramElement.hasAttribute("name")) {
					continue;
				}
				String name = paramElement.getAttribute("name"), value = GeoToXML.getTextFromNode(paramElement);
				if (name != null && value != null) {
					params.put(name, value);
				}
			}
			dm.setup(params);
		}
		if (!dm.hasValidSettings()) {
			err = "Invalid settings of the distance meter; class name = " + dmDescr.getAttribute("className");
			return false;
		}
		ClustersInfo clInfo = new ClustersInfo();
		clInfo.distanceMeter = dm;
		NodeList children = classElem.getElementsByTagName("cluster");
		if (children == null || children.getLength() < 1 || children.item(0) == null || children.item(0).getNodeType() != Node.ELEMENT_NODE) {
			err = "No cluster descriptions found!";
			return false;
		}
		int nObj = 0;
		for (int i = 0; i < children.getLength(); i++) {
			Element clusterElement = (Element) children.item(i);
			if (!clusterElement.hasChildNodes()) {
				continue;
			}
			NodeList specNodes = clusterElement.getElementsByTagName("specimen");
			if (specNodes == null || specNodes.getLength() < 1 || specNodes.item(0) == null || specNodes.item(0).getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			SingleClusterInfo clIn = new SingleClusterInfo();
			if (clusterElement.hasAttribute("clusterN")) {
				clIn.clusterN = GeoToXML.getInt(clusterElement.getAttribute("clusterN"));
			}
			if (clusterElement.hasAttribute("clusterLabel")) {
				clIn.clusterLabel = clusterElement.getAttribute("clusterLabel");
			} else {
				clIn.clusterLabel = String.valueOf(clIn.clusterN);
			}
			if (clusterElement.hasAttribute("origSize")) {
				clIn.origSize = GeoToXML.getInt(clusterElement.getAttribute("origSize"));
			}
			for (int j = 0; j < specNodes.getLength(); j++) {
				Element specNode = (Element) specNodes.item(j);
				if (!specNode.hasChildNodes()) {
					continue;
				}
				Element objNode = getElementByTagName(specNode.getChildNodes(), "Object");
				if (objNode == null) {
					continue;
				}
				DGeoObject gObj = GeoToXML.getDGeoObject(objNode);
				if (gObj == null) {
					continue;
				}
				if (useGeoCoordinates) {
					gObj.setGeographic(true);
				}
				DClusterObject clObj = makeDClusterObject(gObj, nObj++, dm);
				ClusterSpecimenInfo spec = new ClusterSpecimenInfo();
				spec.specimen = clObj;
				if (specNode.hasAttribute("distanceThr")) {
					spec.distanceThr = GeoToXML.getDouble(specNode.getAttribute("distanceThr"));
				}
				if (specNode.hasAttribute("meanDistOrig")) {
					spec.meanDistOrig = GeoToXML.getDouble(specNode.getAttribute("meanDistOrig"));
				}
				if (specNode.hasAttribute("nSimilarOrig")) {
					spec.nSimilarOrig = GeoToXML.getInt(specNode.getAttribute("nSimilarOrig"));
				}
				clIn.addSpecimen(spec);
			}
			if (clIn.getSpecimensCount() > 0) {
				clInfo.addSingleClusterInfo(clIn);
			}
		}
		if (clInfo.getClustersCount() > 0) {
			clustersInfo = clInfo;
		} else {
			err = "No valid cluster descriptions found!";
		}
		return clustersInfo != null;
	}

	public DClusterObject makeDClusterObject(DGeoObject gobj, int indexInContainer, DistanceMeterExt dm) {
		if (gobj == null)
			return null;
		if (dm != null && (dm instanceof LayerClusterer))
			return ((LayerClusterer) dm).makeDClusterObject(gobj, indexInContainer);
		DClusterObject clObj = new DClusterObject(gobj, gobj.getIdentifier(), indexInContainer);
		return clObj;
	}

	/**
	 * Informs if two or more processors of this type can be joined
	 * in a single processor.
	 */
	public boolean canJoin() {
		return true;
	}

	/**
	 * Joins this processor with another processor of the same type.
	 * Returns the resulting processor or null if not successful.
	 */
	public Processor join(Processor proc) {
		err = null;
		if (proc == null)
			return null;
		if (!(proc instanceof ObjectsToClustersAssigner)) {
			err = "The second processor is not an ObjectsToClustersAssigner!";
			return null;
		}
		if (clustersInfo == null || clustersInfo.getClustersCount() < 1) {
			err = "The first processor has no description of clusters!";
			return null;
		}
		if (clustersInfo.distanceMeter == null) {
			err = "The first processor has no distance meter!";
			return null;
		}
		if (!clustersInfo.distanceMeter.hasValidSettings()) {
			err = "The  distance meter of the first processor has invalid internal settings!";
			return null;
		}
		ObjectsToClustersAssigner oClas = (ObjectsToClustersAssigner) proc;
		if (oClas.clustersInfo == null || oClas.clustersInfo.getClustersCount() < 1) {
			err = "The second processor has no description of clusters!";
			return null;
		}
		if (oClas.clustersInfo.distanceMeter == null) {
			err = "The second processor has no distance meter!";
			return null;
		}
		if (!oClas.clustersInfo.distanceMeter.hasValidSettings()) {
			err = "The  distance meter of the second processor has invalid internal settings!";
			return null;
		}
		String clName1 = clustersInfo.distanceMeter.getClass().getName(), clName2 = oClas.clustersInfo.distanceMeter.getClass().getName();
		if (!clName1.equals(clName2)) {
			err = "Incompatible distance meters: [" + clName1 + "] and [" + clName2 + "]!";
			return null;
		}
		HashMap par1 = clustersInfo.distanceMeter.getParameters(null), par2 = oClas.clustersInfo.distanceMeter.getParameters(null);
		if (par1 != null) {
			clName1 = (String) par1.get("distanceComputer");
			if (clName1 != null) {
				if (par2 == null || par2.get("distanceComputer") == null) {
					err = "The second processor has no distance computer (must be instance of " + clName1 + ")!";
					return null;
				}
				clName2 = (String) par2.get("distanceComputer");
				if (!clName1.equals(clName2)) {
					err = "Incompatible distance computers: [" + clName1 + "] and [" + clName2 + "]!";
					return null;
				}
			}
		}

		ObjectsToClustersAssigner oClNew = new ObjectsToClustersAssigner();
		oClNew.mustFindClosest = this.mustFindClosest;
		oClNew.clustersInfo = (ClustersInfo) clustersInfo.clone();
		int maxClusterN = -1;
		for (int i = 0; i < oClNew.clustersInfo.getClustersCount(); i++) {
			SingleClusterInfo clIn = oClNew.clustersInfo.getSingleClusterInfo(i);
			if (clIn.clusterN > maxClusterN) {
				maxClusterN = clIn.clusterN;
			}
		}
		for (int i = 0; i < oClas.clustersInfo.getClustersCount(); i++) {
			SingleClusterInfo clIn = (SingleClusterInfo) oClas.clustersInfo.getSingleClusterInfo(i).clone();
			clIn.clusterN = maxClusterN + i + 1;
			clIn.clusterLabel = String.valueOf(clIn.clusterN);
			for (int j = 0; j < clIn.getSpecimensCount(); j++) {
				clIn.getClusterSpecimenInfo(j).specimen.clusterIdx = clIn.clusterN - 1;
			}
			oClNew.clustersInfo.addSingleClusterInfo(clIn);
		}
		return oClNew;
	}

	/**
	 * Returns an error message, if some operation has failed.
	 */
	public String getErrorMessage() {
		return err;
	}

	/**
	 * Informs if this processor can generate a map layer from its content.
	 */
	public boolean canMakeMapLayer() {
		Object obj = getExampleObject();
		return obj != null && (obj instanceof DGeoObject);
	}

	/**
	 * Produces a map layer from its content and, possibly, a table with
	 * thematic data attached to the layer. Returns the layer, if successful.
	 * The layer has a reference to the produced table, if any.
	 */
	public DGeoLayer makeMapLayer(String name) {
		Object obj = getExampleObject();
		if (obj == null || !(obj instanceof DGeoObject))
			return null;
		Vector spObjects = new Vector(200, 100);
		IntArray clNs = new IntArray(200, 100), spNs = new IntArray(200, 100);
		Vector attrList = null;
		for (int i = 0; i < clustersInfo.getClustersCount(); i++) {
			SingleClusterInfo scl = clustersInfo.getSingleClusterInfo(i);
			for (int j = 0; j < scl.getSpecimensCount(); j++) {
				DGeoObject gobj = getDGeoObject(scl.getClusterSpecimenInfo(j).specimen);
				if (gobj != null) {
					spObjects.addElement(gobj);
					clNs.addElement(i);
					spNs.addElement(j);
					if (gobj.getData() != null) {
						DataRecord drec = (DataRecord) gobj.getData();
						if (attrList == null || attrList.size() < 1) {
							attrList = drec.getAttrList();
						} else {
							for (int k = 0; k < drec.getAttrCount(); k++) {
								Attribute at = drec.getAttribute(k);
								String aName = at.getName();
								boolean found = false;
								for (int l = 0; l < attrList.size() && !found; l++) {
									Attribute at1 = (Attribute) attrList.elementAt(l);
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
			}
		}
		DataTable spTable = new DataTable();
		spTable.setName(name);
		int nAttr = (attrList == null) ? 0 : attrList.size();
		Vector attrIds = new Vector(nAttr + 5, 1);
		if (nAttr > 0) {
			for (int j = 0; j < attrList.size(); j++) {
				Attribute attr = (Attribute) attrList.elementAt(j);
				attrIds.addElement(attr.getIdentifier());
				spTable.addAttribute(attr.getName(), attr.getIdentifier(), attr.getType());
				//System.out.println("Attribute <"+((Attribute)attrList.elementAt(j)).getIdentifier()+
				//    "> : type = "+String.valueOf(((Attribute)attrList.elementAt(j)).getType()));
			}
		}
		spTable.addAttribute("Final cluster N", "_cluster_N_final_" + (spTable.getAttrCount() + 1), AttributeTypes.character);
		int clNColN = spTable.getAttrCount() - 1;
		spTable.addAttribute("Prototype N", "_specimen_N_", AttributeTypes.integer);
		spTable.addAttribute("Distance threshold", "_distance_thr_", AttributeTypes.real);
		spTable.addAttribute("N of neighbours", "_N_neighbours_", AttributeTypes.integer);
		spTable.addAttribute("Mean distance to neighbours", "_min_dist_neighb_", AttributeTypes.real);
		for (int i = 0; i < spObjects.size(); i++) {
			DGeoObject gobj = (DGeoObject) spObjects.elementAt(i);
			DataRecord rec1 = new DataRecord(gobj.getIdentifier(), gobj.getName());
			DataRecord rec = (DataRecord) gobj.getData();
			if (rec != null) {
				for (int j = 0; j < attrIds.size(); j++) {
					rec1.addAttrValue(rec.getAttrValue((String) attrIds.elementAt(j)));
				}
			}
			SingleClusterInfo clIn = clustersInfo.getSingleClusterInfo(clNs.elementAt(i));
			rec1.addAttrValue(clIn.clusterLabel);
			int spIdx = spNs.elementAt(i);
			rec1.setNumericAttrValue(spIdx + 1, String.valueOf(spIdx + 1), clNColN + 1);
			ClusterSpecimenInfo spec = clIn.getClusterSpecimenInfo(spIdx);
			rec.setNumericAttrValue(spec.distanceThr, String.valueOf(spec.distanceThr), clNColN + 2);
			rec.setNumericAttrValue(spec.nSimilarOrig, String.valueOf(spec.nSimilarOrig), clNColN + 3);
			rec.setNumericAttrValue(spec.meanDistOrig, String.valueOf(spec.meanDistOrig), clNColN + 4);
			spTable.addDataRecord(rec1);
			gobj.setThematicData(rec1);
		}
		if (spTable.hasData()) {
			spTable.finishedDataLoading();
		} else {
			spTable = null;
		}
		DGeoObject gObj = (DGeoObject) obj;
		DGeoLayer spLayer = new DGeoLayer();
		spLayer.setType(gObj.getSpatialType());
		spLayer.setName(name);
		spLayer.setGeoObjects(spObjects, true);
		spLayer.setHasMovingObjects(gObj instanceof DMovingObject);
		DrawingParameters dp = spLayer.getDrawingParameters();
		if (dp == null) {
			dp = new DrawingParameters();
			spLayer.setDrawingParameters(dp);
		}
		dp.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
		dp.lineWidth = 2;
		dp.transparency = 0;
		if (spTable != null) {
			spLayer.setDataTable(spTable);
		}
		return spLayer;
	}
}
