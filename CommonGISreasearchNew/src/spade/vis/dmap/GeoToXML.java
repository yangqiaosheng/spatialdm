package spade.vis.dmap;

import java.io.IOException;
import java.io.Writer;
import java.util.Vector;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import spade.time.Date;
import spade.time.TimeCount;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.vis.database.Attribute;
import spade.vis.database.DataRecord;
import spade.vis.database.SpatialEntity;
import spade.vis.database.ThematicDataItem;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.MultiGeometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jan 26, 2009
 * Time: 2:15:42 PM
 * Represents geographical objects in XML format.
 */
public class GeoToXML {
	public static String pointToString(RealPoint rp) {
		if (rp == null)
			return null;
		return "<xCoord>" + rp.x + "</xCoord><yCoord>" + rp.y + "</yCoord>";
	}

	public static RealPoint getPosition(Element positionElement) {
		if (positionElement == null)
			return null;
		if (!positionElement.hasChildNodes())
			return null;
		NodeList children = positionElement.getChildNodes();
		double x = Double.NaN, y = Double.NaN;
		for (int j = 0; j < children.getLength(); j++)
			if (children.item(j) != null && children.item(j).getNodeType() == Node.ELEMENT_NODE) {
				Element child = (Element) children.item(j);
				if (child.getTagName().equalsIgnoreCase("xCoord")) {
					x = getDouble(getTextFromNode(child));
				} else if (child.getTagName().equalsIgnoreCase("yCoord")) {
					y = getDouble(getTextFromNode(child));
				}
			}
		if (!Double.isNaN(x) && !Double.isNaN(y))
			return new RealPoint((float) x, (float) y);
		return null;
	}

	public static String dateToString(Date d) {
		if (d == null)
			return "";
		String str = "<date";
		if (d.useElement('y')) {
			str += " year=\"" + d.getElementValue('y') + "\"";
		}
		if (d.useElement('m')) {
			str += " month=\"" + d.getElementValue('m') + "\"";
		}
		if (d.useElement('d')) {
			str += " day=\"" + d.getElementValue('d') + "\"";
		}
		if (d.useElement('h')) {
			str += " hour=\"" + d.getElementValue('h') + "\"";
		}
		if (d.useElement('t')) {
			str += " minute=\"" + d.getElementValue('t') + "\"";
		}
		if (d.useElement('s')) {
			str += " second=\"" + d.getElementValue('s') + "\"";
		}
		str += " />";
		return str;
	}

	public static String timeCountToString(TimeCount tc) {
		if (tc == null)
			return "";
		return "<moment value=\"" + tc.toNumber() + "\" />";
	}

	public static String timeToString(TimeMoment t) {
		if (t == null)
			return "";
		if (t instanceof Date)
			return dateToString((Date) t);
		if (t instanceof TimeCount)
			return timeCountToString((TimeCount) t);
		return "";
	}

	public static TimeMoment getTime(Element timeElement) {
		if (timeElement == null)
			return null;
		if (!timeElement.hasChildNodes())
			return null;
		NodeList children = timeElement.getChildNodes();
		for (int j = 0; j < children.getLength(); j++)
			if (children.item(j) != null && children.item(j).getNodeType() == Node.ELEMENT_NODE) {
				Element child = (Element) children.item(j);
				if (child.getTagName().equalsIgnoreCase("date")) {
					Date d = getDate(child);
					if (d != null)
						return d;
				} else if (child.getTagName().equalsIgnoreCase("moment")) {
					if (child.hasAttribute("value")) {
						long val = getLong(child.getAttribute("value"));
						if (val >= 0)
							return new TimeCount(val);
					}
				}
			}
		return null;
	}

	public static Date getDate(Element dateElement) {
		if (dateElement == null)
			return null;
		if (!dateElement.getTagName().equalsIgnoreCase("date"))
			return null;
		Date d = new Date();
		if (dateElement.hasAttribute("year")) {
			d.setElementValue('y', getInt(dateElement.getAttribute("year")));
		}
		if (dateElement.hasAttribute("month")) {
			d.setElementValue('m', getInt(dateElement.getAttribute("month")));
		}
		if (dateElement.hasAttribute("day")) {
			d.setElementValue('d', getInt(dateElement.getAttribute("day")));
		}
		if (dateElement.hasAttribute("hour")) {
			d.setElementValue('h', getInt(dateElement.getAttribute("hour")));
		}
		if (dateElement.hasAttribute("minute")) {
			d.setElementValue('t', getInt(dateElement.getAttribute("minute")));
		}
		if (dateElement.hasAttribute("second")) {
			d.setElementValue('s', getInt(dateElement.getAttribute("second")));
		}
		return d;
	}

	public static void writeTimeReference(TimeReference tref, Writer writer) throws IOException {
		if (tref == null || tref.getValidFrom() == null || writer == null)
			return;
		writer.write("\t<existenceTime>\r\n");
		writer.write("\t\t<begin> " + timeToString(tref.getValidFrom()) + " </begin>\r\n");
		if (tref.getValidUntil() != null) {
			writer.write("\t\t<end> " + timeToString(tref.getValidUntil()) + " </end>\r\n");
		}
		writer.write("\t</existenceTime>\r\n");
	}

	public static TimeReference getTimeReference(Element timeRefElement) {
		if (timeRefElement == null)
			return null;
		if (!timeRefElement.getTagName().equalsIgnoreCase("existenceTime"))
			return null;
		if (!timeRefElement.hasChildNodes())
			return null;
		NodeList children = timeRefElement.getChildNodes();
		TimeMoment t0 = null, t1 = null;
		for (int j = 0; j < children.getLength(); j++)
			if (children.item(j) != null && children.item(j).getNodeType() == Node.ELEMENT_NODE) {
				Element child = (Element) children.item(j);
				if (child.getTagName().equalsIgnoreCase("begin")) {
					t0 = getTime(child);
				} else if (child.getTagName().equalsIgnoreCase("end")) {
					t1 = getTime(child);
				}
			}
		if (t0 == null)
			return null;
		TimeReference tref = new TimeReference();
		tref.setValidFrom(t0);
		tref.setValidUntil((t1 != null) ? t1 : t0);
		return tref;
	}

	public static String trajectoryPositionToString(SpatialEntity trPosition) {
		TimeReference tref = trPosition.getTimeReference();
		if (tref == null || tref.getValidFrom() == null)
			return null;
		RealPoint rp = trPosition.getCentre();
		if (rp == null)
			return null;
		String str = "<point> <position>" + pointToString(rp) + "</position>";
		str += " <time>" + timeToString(tref.getValidFrom()) + "</time>";
		if (tref.getValidUntil() != null && !tref.getValidUntil().equals(tref.getValidFrom())) {
			str += " <lastTime>" + timeToString(tref.getValidUntil()) + "</lastTime>";
		}
		str += " </point>";
		return str;
	}

	public static SpatialEntity getTrajectoryPosition(Element trPosElement) {
		if (trPosElement == null)
			return null;
		if (!trPosElement.getTagName().equalsIgnoreCase("point"))
			return null;
		if (!trPosElement.hasChildNodes())
			return null;
		NodeList children = trPosElement.getChildNodes();
		TimeMoment t0 = null, t1 = null;
		RealPoint point = null;
		for (int j = 0; j < children.getLength(); j++)
			if (children.item(j) != null && children.item(j).getNodeType() == Node.ELEMENT_NODE) {
				Element child = (Element) children.item(j);
				if (child.getTagName().equalsIgnoreCase("position")) {
					point = getPosition(child);
				} else if (child.getTagName().equalsIgnoreCase("time")) {
					t0 = getTime(child);
				} else if (child.getTagName().equalsIgnoreCase("lastTime")) {
					t1 = getTime(child);
				}
			}
		if (point == null || t0 == null)
			return null;
		SpatialEntity spe = new SpatialEntity("0");
		spe.setGeometry(point);
		TimeReference tref = new TimeReference();
		tref.setValidFrom(t0);
		tref.setValidUntil((t1 != null) ? t1 : t0);
		spe.setTimeReference(tref);
		return spe;
	}

	public static void writeGeometry(Geometry geom, Writer writer) throws IOException {
		if (geom == null || writer == null)
			return;
		if (geom instanceof RealPoint) {
			RealPoint rp = (RealPoint) geom;
			writer.write("\t<location>" + pointToString(rp) + "</location>\r\n");
		} else if (geom instanceof RealPolyline) {
			RealPolyline rl = (RealPolyline) geom;
			if (rl.p == null)
				return;
			writer.write("\t<shape closed=\"" + rl.isClosed + "\">\r\n");
			for (RealPoint element : rl.p) {
				writer.write("\t\t<point>" + pointToString(element) + "</point>\r\n");
			}
			writer.write("\t</shape>\r\n");
		} else if (geom instanceof MultiGeometry) {
			MultiGeometry mg = (MultiGeometry) geom;
			if (mg.getPartsCount() < 2) {
				writeGeometry(mg.getPart(0), writer);
			} else {
				writer.write("\t<parts>\r\n");
				for (int i = 0; i < mg.getPartsCount(); i++)
					if (mg.getPart(i) instanceof RealPolyline) {
						RealPolyline rl = (RealPolyline) mg.getPart(i);
						if (rl.p == null) {
							continue;
						}
						writer.write("\t\t<part closed=\"" + rl.isClosed + "\">\r\n");
						for (RealPoint element : rl.p) {
							writer.write("\t\t\t<point>" + pointToString(element) + "</point>\r\n");
						}
						writer.write("\t\t</part>\r\n");
					} else if (mg.getPart(i) instanceof RealPoint) {
						RealPoint rp = (RealPoint) mg.getPart(i);
						writer.write("\t\t<part closed=\"false\">\r\n");
						writer.write("\t\t\t<point>" + pointToString(rp) + "</point>\r\n");
						writer.write("\t\t</part>\r\n");
					}
				writer.write("\t</parts>\r\n");
			}
		}
	}

	public static RealPolyline getPolyline(Element polyElement) {
		if (!polyElement.hasChildNodes())
			return null;
		NodeList ptNodes = polyElement.getChildNodes();
		Vector points = new Vector(100, 100);
		for (int k = 0; k < ptNodes.getLength(); k++)
			if (ptNodes.item(k) != null && ptNodes.item(k).getNodeType() == Node.ELEMENT_NODE) {
				Element ptNode = (Element) ptNodes.item(k);
				if (ptNode.getTagName().equalsIgnoreCase("point")) {
					RealPoint point = getPosition(ptNode);
					if (point != null) {
						points.addElement(point);
					}
				}
			}
		if (points.size() < 2)
			return null;
		RealPolyline poly = new RealPolyline();
		poly.p = new RealPoint[points.size()];
		for (int i = 0; i < points.size(); i++) {
			poly.p[i] = (RealPoint) points.elementAt(i);
		}
		if (polyElement.hasAttribute("closed")) {
			poly.isClosed = getBoolean(polyElement.getAttribute("closed"));
		}
		return poly;
	}

	public static Geometry getGeometry(Element geoObjectElement) {
		if (geoObjectElement == null)
			return null;
		if (!geoObjectElement.hasChildNodes())
			return null;
		NodeList children = geoObjectElement.getChildNodes();
		for (int j = 0; j < children.getLength(); j++)
			if (children.item(j) != null && children.item(j).getNodeType() == Node.ELEMENT_NODE) {
				Element child = (Element) children.item(j);
				if (child.getTagName().equalsIgnoreCase("location")) {
					RealPoint point = getPosition(child);
					if (point != null)
						return point;
				} else if (child.getTagName().equalsIgnoreCase("shape")) {
					RealPolyline poly = getPolyline(child);
					if (poly == null) {
						continue;
					}
					return poly;
				} else if (child.getTagName().equalsIgnoreCase("trajectory")) {
					//...
				} else if (child.getTagName().equalsIgnoreCase("parts")) {
					if (!child.hasChildNodes()) {
						continue;
					}
					MultiGeometry mg = new MultiGeometry();
					NodeList partNodes = child.getChildNodes();
					for (int k = 0; k < partNodes.getLength(); k++)
						if (partNodes.item(k) != null && partNodes.item(k).getNodeType() == Node.ELEMENT_NODE) {
							Element partNode = (Element) partNodes.item(k);
							if (partNode.getTagName().equalsIgnoreCase("part")) {
								Geometry part = getPolyline(partNode);
								if (part != null) {
									mg.addPart(part);
								}
							}
						}
					if (mg.getPartsCount() > 0)
						return mg;
				}
			}
		return null;
	}

	public static void writeGeometry(DGeoObject gobj, Writer writer) throws IOException {
		if (gobj == null || writer == null)
			return;
		if (!(gobj instanceof DMovingObject)) {
			writeGeometry(gobj.getGeometry(), writer);
			return;
		}
		DMovingObject mobj = (DMovingObject) gobj;
		Vector track = mobj.getTrack();
		if (track == null || track.size() < 1)
			return;
		writer.write("\t<trajectory>\r\n");
		for (int j = 0; j < track.size(); j++) {
			String str = trajectoryPositionToString((SpatialEntity) track.elementAt(j));
			if (str == null) {
				continue;
			}
			writer.write("\t\t" + str + "\r\n");
		}
		writer.write("\t</trajectory>\r\n");
	}

	public static Vector getTrack(Element geoObjectElement) {
		if (geoObjectElement == null)
			return null;
		if (!geoObjectElement.hasChildNodes())
			return null;
		NodeList children = geoObjectElement.getChildNodes();
		for (int j = 0; j < children.getLength(); j++)
			if (children.item(j) != null && children.item(j).getNodeType() == Node.ELEMENT_NODE) {
				Element child = (Element) children.item(j);
				if (child.getTagName().equalsIgnoreCase("trajectory")) {
					if (!child.hasChildNodes()) {
						continue;
					}
					NodeList ptNodes = child.getChildNodes();
					Vector track = new Vector(100, 100);
					for (int k = 0; k < ptNodes.getLength(); k++)
						if (ptNodes.item(k) != null && ptNodes.item(k).getNodeType() == Node.ELEMENT_NODE) {
							Element ptNode = (Element) ptNodes.item(k);
							if (ptNode.getTagName().equalsIgnoreCase("point")) {
								SpatialEntity position = getTrajectoryPosition(ptNode);
								if (position != null) {
									track.addElement(position);
									position.setId(String.valueOf(track.size()));
								}
							}
						}
					if (track.size() > 0)
						return track;
				}
			}
		return null;
	}

	public static void writeGeoObject(DGeoObject gObj, Writer writer) throws IOException {
		if (gObj == null || writer == null)
			return;
		writer.write("<Object>\r\n");
		writer.write("\t<id>" + gObj.getIdentifier() + "</id>\r\n");
		String name = gObj.getName();
		if (name != null && name.equals(gObj.getIdentifier())) {
			name = null;
		}
		if (name != null) {
			writer.write("\t<name>" + name + "</name>\r\n");
		}
		writeGeometry(gObj, writer);
		writeTimeReference(gObj.getTimeReference(), writer);
		ThematicDataItem dit = gObj.getData();
		if (dit != null) {
			if (name == null) {
				name = dit.getName();
				if (name != null && !name.equals(dit.getId())) {
					writer.write("\t<name>" + name + "</name>\r\n");
				}
			}
			for (int j = 0; j < dit.getAttrCount(); j++) {
				String val = dit.getAttrValueAsString(j);
				if (val != null) {
					writer.write("\t<property name=\"" + dit.getAttributeName(j) + "\" type=\"" + getTypeAsString(dit.getAttrType(j)) + "\">" + val + "</property>\r\n");
				}
			}
		}
		writer.write("</Object>\r\n\r\n");
	}

	public static DGeoObject getDGeoObject(Element geoObjectElement) {
		if (geoObjectElement == null)
			return null;
		if (!geoObjectElement.hasChildNodes())
			return null;
		NodeList children = geoObjectElement.getChildNodes();
		DGeoObject gObj = null;
		Vector track = getTrack(geoObjectElement);
		Geometry geom = null;
		if (track == null) {
			geom = getGeometry(geoObjectElement);
			//if (geom==null) return null;
			SpatialEntity spe = new SpatialEntity("_temporary_");
			spe.setGeometry(geom);
			gObj = new DGeoObject();
			gObj.setup(spe);
		} else {
			DMovingObject mObj = new DMovingObject();
			mObj.setup(new SpatialEntity("_temporary_"));
			mObj.setTrack(track);
			gObj = mObj;
		}
		Vector attributes = null, values = null;
		for (int j = 0; j < children.getLength(); j++)
			if (children.item(j) != null && children.item(j).getNodeType() == Node.ELEMENT_NODE) {
				Element child = (Element) children.item(j);
				if (child.getTagName().equalsIgnoreCase("id")) {
					gObj.setIdentifier(getTextFromNode(child));
				} else if (child.getTagName().equalsIgnoreCase("name")) {
					gObj.setLabel(getTextFromNode(child));
				} else if (child.getTagName().equalsIgnoreCase("existenceTime")) {
					TimeReference tref = getTimeReference(child);
					if (tref != null) {
						gObj.getSpatialData().setTimeReference(tref);
					}
				} else if (child.getTagName().equalsIgnoreCase("neighbor")) {
					String id = getTextFromNode(child);
					if (id != null) {
						gObj.addNeighbour(id);
					}
				} else if (child.getTagName().equalsIgnoreCase("centroid")) {
					RealPoint point = getPosition(child);
					if (point != null) {
						gObj.getGeometry().setCentroid(point.x, point.y);
					}
				} else if (child.getTagName().equalsIgnoreCase("property")) {
					String valueStr = getTextFromNode(child);
					if (valueStr != null && child.hasAttribute("name")) {
						if (attributes == null) {
							attributes = new Vector(20, 20);
						}
						Attribute attr = new Attribute("attr_" + (attributes.size() + 1), getTypeFromString(child.getAttribute("type")));
						attr.setName(child.getAttribute("name"));
						attributes.addElement(attr);
						if (values == null) {
							values = new Vector(20, 20);
						}
						values.addElement(valueStr);
					}
				}
			}
		if (attributes != null && attributes.size() > 0) {
			DataRecord rec = new DataRecord(gObj.getIdentifier(), gObj.getName());
			rec.setAttrList(attributes);
			for (int i = 0; i < values.size(); i++) {
				rec.addAttrValue(values.elementAt(i));
			}
			gObj.setThematicData(rec);
		}
		if (gObj.getGeometry() == null && gObj.getData() == null)
			return null;
		return gObj;
	}

	public static String getTypeAsString(char attrType) {
		switch (attrType) {
		case 'I':
		case 'i':
		case 'R':
		case 'r':
			return "numeric";
		case 'C':
		case 'c':
			return "string";
		case 'L':
		case 'l':
			return "boolean";
		case 'T':
		case 't':
			return "time";
		case 'G':
		case 'g':
			return "geometry";
		}
		return "string";
	}

	public static char getTypeFromString(String typeStr) {
		if (typeStr == null)
			return 'C';
		if (typeStr.equalsIgnoreCase("numeric"))
			return 'R';
		if (typeStr.equalsIgnoreCase("integer"))
			return 'I';
		if (typeStr.equalsIgnoreCase("boolean"))
			return 'L';
		if (typeStr.equalsIgnoreCase("time"))
			return 'T';
		if (typeStr.equalsIgnoreCase("geometry"))
			return 'G';
		return 'C';
	}

	public static boolean getBoolean(String str) {
		if (str == null)
			return false;
		return str.equalsIgnoreCase("true") || str.equalsIgnoreCase("yes");
	}

	public static int getInt(String str) {
		if (str == null)
			return 0;
		try {
			int k = Integer.parseInt(str);
			return k;
		} catch (NumberFormatException e) {
		}
		return 0;
	}

	public static long getLong(String str) {
		if (str == null)
			return 0;
		try {
			long k = Long.parseLong(str);
			return k;
		} catch (NumberFormatException e) {
		}
		return 0;
	}

	public static double getDouble(String str) {
		if (str == null)
			return Double.NaN;
		try {
			double d = Double.parseDouble(str);
			return d;
		} catch (NumberFormatException e) {
		}
		return Double.NaN;
	}

	public static String getTextFromNode(Node node) {
		if (node == null || node.getNodeType() != Node.ELEMENT_NODE)
			return null;
		((Element) node).normalize();
		NodeList children = node.getChildNodes();
		if (children == null || children.getLength() < 1)
			return null;
		for (int i = 0; i < children.getLength(); i++)
			if (children.item(i) != null && children.item(i).getNodeType() == Node.TEXT_NODE) {
				Text txt = (Text) children.item(i);
				String str = txt.getData();
				if (str != null)
					return str.trim();
			}
		return null;
	}

}
