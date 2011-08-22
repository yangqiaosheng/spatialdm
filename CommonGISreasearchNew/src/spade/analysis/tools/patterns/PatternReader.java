package spade.analysis.tools.patterns;

import java.awt.Color;
import java.awt.Frame;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.lib.basicwin.CManager;
import spade.lib.util.FloatArray;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.SpatialEntity;
import spade.vis.dataview.ShowRecManager;
import spade.vis.dmap.DAggregateLayer;
import spade.vis.dmap.DAggregateLinkLayer;
import spade.vis.dmap.DAggregateLinkObject;
import spade.vis.dmap.DAggregateObject;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DLinkLayer;
import spade.vis.dmap.DLinkObject;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.MultiGeometry;
import spade.vis.geometry.RealCircle;
import spade.vis.geometry.RealLine;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealRectangle;
import spade.vis.space.GeoLayer;
import spade.vis.space.GeoObject;
import spade.vis.space.LayerManager;
import spade.vis.spec.DataSourceSpec;
import data_load.DataManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jan 29, 2009
 * Time: 6:58:28 PM
 * Reads pattern descriptions from an ASCII file having a special format.
 * The file must start with the tag <PATTERNS> and end with the tag
 * </PATTERNS>.
 * A pattern description starts with   <PATTERN some_attributes>
 * and ends with </PATTERN>.
 * Currently the reader can read T-patterns. A T-pattern includes a
 * sequence of regions and travel durations (minimum, maximum) between them.
 * Before the descriptions of the T-patterns a reference to the file
 * describing the regions must be given in the following format:
 * <REGIONS FILE="file_name" />
 */
public class PatternReader {
	protected ESDACore core = null;

	public void setCore(ESDACore core) {
		this.core = core;
	}

	/**
	 * The name of the file or identifier of the layer with the regions
	 * occurring in the patterns.
	 */
	protected String regionsFName = null;
	/**
	 * The directory from where patterns are loaded. The regions occurring
	 * in the patterns are normally stored in a file in the same directory.
	 * It is assumed that the string ends with a separator / or \.
	 */
	protected String dir = null;

	public void setDirectory(String dir) {
		this.dir = dir;
	}

	public boolean loadPatterns(BufferedReader reader) {
		if (reader == null)
			return false;
		int k = 0;
		boolean start = true, openingTagFound = false;
		Vector<TPattern> patterns = new Vector<TPattern>(100, 100);
		TPattern current = null;
		boolean hasTravelTimes = false;
		while (true) {
			try {
				String s = reader.readLine();
				if (s == null) {
					break;
				}
				s = s.trim();
				if (s.length() < 1) {
					continue;
				}
				if (start) { //at the beginning of the stream some header may be present.
					StringTokenizer st = new StringTokenizer(s, " \n\r");
					if (st.hasMoreTokens()) {
						String tok = st.nextToken().toLowerCase();
						if (tok.startsWith("http") || tok.startsWith("accept-") || tok.startsWith("server") || tok.startsWith("date:") || tok.startsWith("last-modified:") || tok.startsWith("etag:") || tok.startsWith("content-")
								|| tok.startsWith("connection")) {
							continue;
						}
					}
				}
				if (s.equalsIgnoreCase("<PATTERNS>")) {
					start = false;
					openingTagFound = true;
					continue;
				}
				if (s.equalsIgnoreCase("</PATTERNS>")) {
					break;
				}
				if (!openingTagFound) {
					continue;
				}
				String sUp = s.toUpperCase();
				if (sUp.startsWith("<REGIONS")) {
					int idx = sUp.indexOf("FILE");
					if (idx < 0) {
						continue;
					}
					idx = sUp.indexOf('=', idx + 4);
					if (idx < 0) {
						continue;
					}
					Vector names = StringUtil.getNames(s.substring(idx + 1));
					if (names == null || names.size() < 1) {
						continue;
					}
					regionsFName = StringUtil.removeQuotes((String) names.elementAt(0));
				} else if (sUp.equals("</PATTERN>")) {
					if (current != null && current.objIds != null && current.objIds.size() >= 2) {
						patterns.addElement(current);
					}
					hasTravelTimes = hasTravelTimes || (current.minTravTimes != null && current.minTravTimes.size() > 0);
					current = null;
				} else if (sUp.startsWith("<PATTERN")) {
					current = new TPattern();
					StringTokenizer st = new StringTokenizer(sUp, "<> =,;\r\n");
					st.nextToken(); //the keyword PATTERN
					while (st.hasMoreTokens()) {
						String tok = st.nextToken();
						if (tok.equals("ID") && st.hasMoreTokens()) {
							current.id = st.nextToken();
						} else if (tok.equals("SUPPORT") && st.hasMoreTokens()) {
							String val = st.nextToken();
							try {
								current.support = Integer.parseInt(val);
							} catch (NumberFormatException e) {
							}
						} else if (tok.equals("LENGTH") && st.hasMoreTokens()) {
							String val = st.nextToken();
							try {
								current.length = Integer.parseInt(val);
							} catch (NumberFormatException e) {
							}
						}
					}
				} else if (sUp.startsWith("REGION") || sUp.startsWith("<REGION")) {
					if (current != null) {
						StringTokenizer st = new StringTokenizer(sUp, "<> =,;\r\n");
						st.nextToken(); //the keyword REGION
						while (st.hasMoreTokens()) {
							String tok = st.nextToken();
							if (tok.equals("ID") && st.hasMoreTokens()) {
								if (current.objIds == null) {
									current.objIds = new Vector<String>(10, 10);
								}
								current.objIds.addElement(st.nextToken());
							}
						}
					}
				} else if (sUp.startsWith("TIME") || sUp.startsWith("<TIME")) {
					if (current != null) {
						StringTokenizer st = new StringTokenizer(sUp, "<> =,;\r\n");
						st.nextToken(); //the keyword TIME
						int nTimes = 0;
						while (st.hasMoreTokens() && nTimes < 2) {
							String tok = st.nextToken();
							try {
								float t = Float.parseFloat(tok);
								if (!Float.isNaN(t))
									if (nTimes == 0) {
										if (current.minTravTimes == null) {
											current.minTravTimes = new FloatArray(10, 10);
										}
										current.minTravTimes.addElement(t);
									} else {
										if (current.maxTravTimes == null) {
											current.maxTravTimes = new FloatArray(10, 10);
										}
										current.maxTravTimes.addElement(t);
									}
							} catch (NumberFormatException e) {
							}
							++nTimes;
						}
					}
				}
				++k;
				if (k % 100 == 0) {
					showMessage(k + " lines read", false);
				}
			} catch (EOFException ioe) {
				if (patterns.size() > 0) {
					showMessage(patterns.size() + " patterns loaded", false);
				} else {
					showMessage("Unexpected end of file; no data loaded!", true);
				}
			} catch (IOException ioe) {
				showMessage("Error reading patterns: " + ioe, true);
				break;
			}
		}
		if (!openingTagFound) {
			showMessage("The opening tag <PATTERNS> has not been found!", true);
			return false;
		}
		if (patterns.size() < 1) {
			showMessage("No patterns found!", true);
			return false;
		}
		if (regionsFName == null) {
			showMessage("No reference to a file or layer with the regions found!", true);
			return false;
		}
		DataLoader loader = core.getDataLoader();
		DGeoLayer regLayer = findLayer(regionsFName, loader); //map layer with the regions
		if (regLayer == null) {
			DataSourceSpec spec = new DataSourceSpec();
			spec.source = (dir == null) ? regionsFName : dir + regionsFName;
			if (!loader.loadData(spec)) {
				showMessage("Could not load the file " + spec.source + " describing the regions!", true);
				return false;
			}
			regLayer = findLayer(regionsFName, loader);
			if (regLayer == null) {
				showMessage("Could not build a map layer from the file " + spec.source + " describing the regions!", true);
				return false;
			}
			regLayer.setName("Regions");
			DrawingParameters dp = regLayer.getDrawingParameters();
			if (dp == null) {
				dp = new DrawingParameters();
			}
			dp.lineColor = Color.pink.darker();
			dp.fillColor = Color.pink;
			dp.transparency = 50;
			regLayer.setDrawingParameters(dp);
		}
		Vector<DAggregateObject> patObjects = new Vector<DAggregateObject>(patterns.size(), 10);
		DataTable patTable = new DataTable();
		patTable.addAttribute("Regions", "regions", AttributeTypes.character);
		patTable.addAttribute("Length", "length", AttributeTypes.integer);
		patTable.addAttribute("Support", "support", AttributeTypes.real);
		if (hasTravelTimes) {
			patTable.addAttribute("Min duration", "min_dur", AttributeTypes.real);
			patTable.addAttribute("Max duration", "max_dur", AttributeTypes.real);
			patTable.addAttribute("Sum of min durations", "sum_min_dur", AttributeTypes.real);
			patTable.addAttribute("Sum of max durations", "sum_max_dur", AttributeTypes.real);
		}
		DataTable regTable = (DataTable) regLayer.getThematicData();
		boolean makeRegTable = regTable == null;
		if (makeRegTable) {
			regTable = new DataTable();
			regTable.setName(regLayer.getName());
		}
		regTable.addAttribute("Appears in patterns", null, AttributeTypes.character);
		int rtCN = regTable.getAttrCount() - 1;
		regTable.addAttribute("N of the patterns", null, AttributeTypes.integer);
		if (makeRegTable) {
			for (int i = 0; i < regLayer.getObjectCount(); i++) {
				DGeoObject geo = regLayer.getObject(i);
				DataRecord rec = new DataRecord(geo.getIdentifier(), geo.getName());
				regTable.addDataRecord(rec);
				rec.setNumericAttrValue(0, "0", rtCN + 1);
				geo.setThematicData(rec);
			}
		}
		for (int i = 0; i < patterns.size(); i++) {
			TPattern pat = patterns.elementAt(i);
			if (pat.objIds == null || pat.objIds.size() < 1) {
				continue;
			}
			RealPoint pts[] = new RealPoint[pat.objIds.size()];
			DGeoObject members[] = new DGeoObject[pat.objIds.size()];
			boolean ok = true;
			float minDur = Float.NaN, maxDur = Float.NaN, sumMinDur = 0, sumMaxDur = 0;
			for (int j = 0; j < pat.objIds.size() && ok; j++) {
				GeoObject geo = regLayer.findObjectById(pat.objIds.elementAt(j));
				if (geo == null || geo.getGeometry() == null || !(geo instanceof DGeoObject)) {
					showMessage("Did not find region with the identifier = " + pat.objIds.elementAt(j), true);
					ok = false;
				} else {
					pts[j] = getCentre(geo.getGeometry());
					members[j] = (DGeoObject) geo;
					if (hasTravelTimes && pat.minTravTimes != null && pat.minTravTimes != null) {
						if (j < pat.minTravTimes.size()) {
							float val = pat.minTravTimes.elementAt(j);
							if (!Float.isNaN(val)) {
								if (Float.isNaN(minDur) || val < minDur) {
									minDur = val;
								}
								sumMinDur += val;
							}
						}
						if (j < pat.maxTravTimes.size()) {
							float val = pat.maxTravTimes.elementAt(j);
							if (!Float.isNaN(val)) {
								if (Float.isNaN(maxDur) || val > maxDur) {
									maxDur = val;
								}
								sumMaxDur += val;
							}
						}
					}
				}
			}
			if (!ok) {
				continue;
			}
			Geometry geom = null;
			if (hasTravelTimes) {
				if (pts.length == 1) {
					geom = pts[0];
				} else if (pts.length == 2) {
					RealLine rl = new RealLine();
					geom = rl;
					rl.x1 = pts[0].x;
					rl.y1 = pts[0].y;
					rl.x2 = pts[1].x;
					rl.y2 = pts[1].y;
					rl.directed = true;
				} else {
					MultiGeometry mg = new MultiGeometry();
					geom = mg;
					for (int j = 0; j < pts.length - 1; j++) {
						RealLine rl = new RealLine();
						rl.x1 = pts[j].x;
						rl.y1 = pts[j].y;
						rl.x2 = pts[j + 1].x;
						rl.y2 = pts[j + 1].y;
						rl.directed = true;
						mg.addPart(rl);
					}
				}
			} else {
				RealRectangle bounds = members[0].getBounds();
				for (int j = 1; j < members.length; j++) {
					RealRectangle rect = members[j].getBounds();
					bounds = bounds.union(rect);
				}
				geom = bounds;
			}
			SpatialEntity spe = new SpatialEntity(pat.id);
			spe.setGeometry(geom);
			DAggregateObject obj = new DAggregateObject();
			obj.setup(spe);
			String regIds = "";
			for (int j = 0; j < members.length; j++) {
				obj.addMember(members[j], null, null);
				if (j > 0) {
					regIds += ";";
				}
				regIds += members[j].getIdentifier();
				DataRecord rec = (DataRecord) members[j].getData();
				String str = rec.getAttrValueAsString(rtCN);
				if (str == null) {
					str = pat.id;
				} else {
					str += ";" + pat.id;
				}
				rec.setAttrValue(str, rtCN);
				double val = rec.getNumericAttrValue(rtCN + 1);
				if (Double.isNaN(val)) {
					rec.setNumericAttrValue(1, "1", rtCN + 1);
				} else {
					int n = 1 + (int) Math.round(val);
					rec.setNumericAttrValue(n, String.valueOf(n), rtCN + 1);
				}
			}
			obj.setExtraInfo(pat);
			obj.setGiveColorsToMembers(false);
			patObjects.addElement(obj);
			DataRecord rec = new DataRecord(obj.getIdentifier());
			patTable.addDataRecord(rec);
			rec.setAttrValue(regIds, 0);
			rec.setNumericAttrValue(pat.length, String.valueOf(pat.length), 1);
			rec.setNumericAttrValue(pat.support, String.valueOf(pat.support), 2);
			if (hasTravelTimes) {
				if (!Float.isNaN(minDur)) {
					rec.setNumericAttrValue(minDur, String.valueOf(minDur), 3);
					rec.setNumericAttrValue(sumMinDur, String.valueOf(sumMinDur), 5);
				}
				if (!Float.isNaN(maxDur)) {
					rec.setNumericAttrValue(maxDur, String.valueOf(maxDur), 4);
					rec.setNumericAttrValue(sumMaxDur, String.valueOf(sumMaxDur), 6);
				}
			}
			obj.setThematicData(rec);
		}
		if (patObjects.size() < 1) {
			showMessage("Could not construct geographical objects from the patterns!", true);
			return false;
		}
		boolean geo = regLayer.isGeographic();
		DAggregateLayer patLayer = new DAggregateLayer();
		patLayer.setGeographic(geo);
		patLayer.setType((hasTravelTimes) ? Geometry.line : Geometry.area);
		patLayer.setName("Patterns with regions from " + regionsFName);
		patLayer.setGeoObjects(patObjects, true);
		patLayer.setSourceLayer(regLayer);
		DrawingParameters dp = patLayer.getDrawingParameters();
		if (dp == null) {
			dp = new DrawingParameters();
			patLayer.setDrawingParameters(dp);
		}
		dp.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
		dp.fillColor = dp.lineColor.brighter();
		dp.fillContours = !hasTravelTimes;
		dp.lineWidth = 2;
		dp.transparency = (hasTravelTimes) ? 40 : 80;
		loader.addMapLayer(patLayer, -1);
		patTable.setName("Data about " + patLayer.getName());
		int tblN = loader.addTable(patTable);
		patTable.setEntitySetIdentifier(patLayer.getEntitySetIdentifier());
		loader.setLink(patLayer, tblN);
		patLayer.setLinkedToTable(true);
		patLayer.setSupervisor(core.getSupervisor());
		ShowRecManager recMan = null;
		if (loader instanceof DataManager) {
			recMan = ((DataManager) loader).getShowRecManager(tblN);
		}
		if (recMan != null) {
			Vector showAttr = new Vector(patTable.getAttrCount(), 10);
			for (int j = 0; j < patTable.getAttrCount(); j++) {
				showAttr.addElement(patTable.getAttributeId(j));
			}
			recMan.setPopupAddAttrs(showAttr);
		}
		if (makeRegTable) {
			tblN = loader.addTable(regTable);
			regTable.setEntitySetIdentifier(regLayer.getEntitySetIdentifier());
			loader.setLink(regLayer, tblN);
			regLayer.setLinkedToTable(true);
		}
		regLayer.setLayerDrawn(false);
		if (hasTravelTimes) {
			//make a separate layer with links and a corresponding table
			DataTable linkTbl = new DataTable();
			linkTbl.setName("Moves from " + patLayer.getName());
			linkTbl.addAttribute("Pattern ID", "pattern_id", AttributeTypes.character);
			linkTbl.addAttribute("Index", "index", AttributeTypes.integer);
			linkTbl.addAttribute("Origin ID", "origin", AttributeTypes.character);
			linkTbl.addAttribute("Destination ID", "destination", AttributeTypes.character);
			linkTbl.addAttribute("Min duration", "min_dur", AttributeTypes.real);
			int minDurCN = linkTbl.getAttrCount() - 1;
			linkTbl.addAttribute("Max duration", "max_dur", AttributeTypes.real);
			int maxDurCN = linkTbl.getAttrCount() - 1;
			Vector linkObj = new Vector(patObjects.size() * 3, 100);
			for (int i = 0; i < patObjects.size(); i++) {
				DAggregateObject aobj = patObjects.elementAt(i);
				if (aobj.getMemberCount() < 2) {
					continue;
				}
				DGeoObject obj1 = aobj.getMember(0).obj;
				for (int j = 1; j < aobj.getMemberCount(); j++) {
					DGeoObject obj2 = aobj.getMember(j).obj;
					DLinkObject lObj = new DLinkObject();
					lObj.setup(obj1, obj2, null, null);
					lObj.setIdentifier(aobj.getIdentifier() + "_" + j);
					lObj.setGeographic(geo);
					DataRecord rec = new DataRecord(lObj.getIdentifier());
					linkTbl.addDataRecord(rec);
					rec.setAttrValue(aobj.getIdentifier(), 0);
					rec.setNumericAttrValue(j, String.valueOf(j), 1);
					rec.setAttrValue(obj1.getIdentifier(), 2);
					rec.setAttrValue(obj2.getIdentifier(), 3);
					TPattern pat = (TPattern) aobj.getExtraInfo();
					if (pat != null && pat.minTravTimes != null && pat.minTravTimes != null) {
						if (j - 1 < pat.minTravTimes.size()) {
							float val = pat.minTravTimes.elementAt(j - 1);
							if (!Float.isNaN(val)) {
								rec.setNumericAttrValue(val, String.valueOf(val), minDurCN);
							}
						}
						if (j - 1 < pat.maxTravTimes.size()) {
							float val = pat.maxTravTimes.elementAt(j - 1);
							if (!Float.isNaN(val)) {
								rec.setNumericAttrValue(val, String.valueOf(val), maxDurCN);
							}
						}
					}
					lObj.setThematicData(rec);
					linkObj.addElement(lObj);
					obj1 = obj2;
				}
			}
			if (linkObj.size() > 0) {
				DLinkLayer linkLayer = new DLinkLayer();
				linkLayer.setType(Geometry.line);
				linkLayer.setPlaceLayer(regLayer);
				linkLayer.setGeographic(geo);
				linkLayer.setName(linkTbl.getName());
				linkLayer.setGeoObjects(linkObj, true);
				dp = linkLayer.getDrawingParameters();
				if (dp == null) {
					dp = new DrawingParameters();
					linkLayer.setDrawingParameters(dp);
				}
				dp.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
				dp.lineWidth = 2;
				dp.transparency = 40;
				loader.addMapLayer(linkLayer, -1);
				tblN = loader.addTable(linkTbl);
				linkTbl.setEntitySetIdentifier(linkLayer.getEntitySetIdentifier());
				loader.setLink(linkLayer, tblN);
				linkLayer.setLinkedToTable(true);
				linkLayer.setLayerDrawn(false);

				Vector aggLinks = new Vector(linkObj.size(), 100);
				for (int i = 0; i < linkObj.size(); i++) {
					DLinkObject link = (DLinkObject) linkObj.elementAt(i);
					DGeoObject startNode = link.getStartNode(), endNode = link.getEndNode();
					DAggregateLinkObject aggLink = null;
					for (int j = 0; j < aggLinks.size() && aggLink == null; j++) {
						aggLink = (DAggregateLinkObject) aggLinks.elementAt(j);
						if (!aggLink.startNode.getIdentifier().equals(startNode.getIdentifier()) || !aggLink.endNode.getIdentifier().equals(endNode.getIdentifier())) {
							aggLink = null;
						}
					}
					if (aggLink == null) {
						aggLink = new DAggregateLinkObject();
						aggLinks.addElement(aggLink);
						aggLink.setGeographic(geo);
					}
					aggLink.addLink(link, link.getIdentifier());
				}
				//construct a table with thematic information about the aggregated moves
				DataTable aggTbl = new DataTable();
				aggTbl.setName("Aggregated moves from " + patLayer.getName());
				aggTbl.addAttribute("Origin ID", "startId", AttributeTypes.character);
				int startIdIdx = aggTbl.getAttrCount() - 1;
				aggTbl.addAttribute("Destination ID", "endId", AttributeTypes.character);
				int endIdIdx = aggTbl.getAttrCount() - 1;
				aggTbl.addAttribute("Direction", "direction", AttributeTypes.character);
				int dirIdx = aggTbl.getAttrCount() - 1;
				aggTbl.addAttribute("Distance", "distance", AttributeTypes.integer);
				int distIdx = aggTbl.getAttrCount() - 1;
				aggTbl.addAttribute("N of moves", "n_moves", AttributeTypes.integer);
				int nMovesIdx = aggTbl.getAttrCount() - 1;
				int minDurIdx = -1, maxDurIdx = -1;
				if (minDurCN >= 0 || maxDurCN >= 0) {
					aggTbl.addAttribute("Min move duration", "min_dur", AttributeTypes.integer);
					minDurIdx = aggTbl.getAttrCount() - 1;
					aggTbl.addAttribute("Max move duration", "max_dur", AttributeTypes.integer);
					maxDurIdx = aggTbl.getAttrCount() - 1;
				}
				for (int i = 0; i < aggLinks.size(); i++) {
					DAggregateLinkObject lobj = (DAggregateLinkObject) aggLinks.elementAt(i);
					DataRecord rec = new DataRecord(lobj.getIdentifier());
					aggTbl.addDataRecord(rec);
					String srcId = lobj.startNode.getIdentifier(), destId = lobj.endNode.getIdentifier();
					rec.setAttrValue(srcId, startIdIdx);
					rec.setAttrValue(destId, endIdIdx);
					rec.setAttrValue(lobj.getLinkDirection(), dirIdx);
					double length = lobj.getLength();
					if (!Double.isNaN(length)) {
						rec.setNumericAttrValue(length, String.valueOf(length), distIdx);
					}
					int nLinks = lobj.souLinks.size();
					rec.setNumericAttrValue(nLinks, String.valueOf(nLinks), nMovesIdx);
					if (minDurCN >= 0 || maxDurCN >= 0) {
						double minDur = Double.NaN, maxDur = Double.NaN;
						for (int j = 0; j < nLinks; j++) {
							DLinkObject link = (DLinkObject) lobj.souLinks.elementAt(j);
							DataRecord lrec = (DataRecord) link.getData();
							if (lrec == null) {
								continue;
							}
							if (minDurCN >= 0) {
								double val = lrec.getNumericAttrValue(minDurCN);
								if (!Double.isNaN(val)) {
									if (Double.isNaN(minDur) || minDur > val) {
										minDur = val;
									}
									if (Double.isNaN(maxDur) || maxDur < val) {
										maxDur = val;
									}
								}
							}
							if (maxDurCN >= 0) {
								double val = lrec.getNumericAttrValue(maxDurCN);
								if (!Double.isNaN(val)) {
									if (Double.isNaN(minDur) || minDur > val) {
										minDur = val;
									}
									if (Double.isNaN(maxDur) || maxDur < val) {
										maxDur = val;
									}
								}
							}
						}
						if (!Double.isNaN(minDur)) {
							rec.setNumericAttrValue(minDur, String.valueOf(minDur), minDurIdx);
							rec.setNumericAttrValue(maxDur, String.valueOf(maxDur), maxDurIdx);
						}
					}
					lobj.setThematicData(rec);
				}
				int aggTblN = loader.addTable(aggTbl);

				DAggregateLinkLayer aggLinkLayer = new DAggregateLinkLayer();
				aggLinkLayer.setType(Geometry.line);
				aggLinkLayer.setName(aggTbl.getName());
				aggLinkLayer.setGeographic(geo);
				aggLinkLayer.setGeoObjects(aggLinks, true);
				aggLinkLayer.setHasMovingObjects(true);
				aggLinkLayer.setTrajectoryLayer(linkLayer);
				aggLinkLayer.setPlaceLayer(regLayer);
				DrawingParameters dp1 = aggLinkLayer.getDrawingParameters();
				if (dp1 == null) {
					dp1 = new DrawingParameters();
					aggLinkLayer.setDrawingParameters(dp1);
				}
				dp1.lineColor = Color.red.darker();
				loader.addMapLayer(aggLinkLayer, -1);
				aggTbl.setEntitySetIdentifier(aggLinkLayer.getEntitySetIdentifier());
				loader.setLink(aggLinkLayer, aggTblN);
				aggLinkLayer.setLinkedToTable(true);
				aggLinkLayer.countActiveLinks();
				aggLinkLayer.setLayerDrawn(false);
			}
		}
		LayerManager lman = core.getUI().getCurrentMapViewer().getLayerManager();
		lman.activateLayer(patLayer.getContainerIdentifier());
		return true;
	}

	protected DGeoLayer findLayer(String layerName, DataLoader loader) {
		if (loader == null || layerName == null)
			return null;
		if (loader.getMapCount() > 0) {
			LayerManager lman = loader.getMap(0);
			int lIdx = lman.getIndexOfLayer(layerName);
			if (lIdx >= 0) {
				GeoLayer layer = lman.getGeoLayer(lIdx);
				if (layer != null && (layer instanceof DGeoLayer))
					return (DGeoLayer) layer;
			}
		}
		return null;
	}

	public RealPoint getCentre(Geometry geom) {
		if (geom == null)
			return null;
		if (geom instanceof RealPoint)
			return (RealPoint) geom;
		if (geom instanceof RealCircle) {
			RealCircle c = (RealCircle) geom;
			return new RealPoint(c.cx, c.cy);
		}
		float bounds[] = geom.getBoundRect();
		if (bounds == null)
			return null;
		return new RealPoint((bounds[0] + bounds[2]) / 2, (bounds[1] + bounds[3]) / 2);
	}

	/**
	* Displays the notification message using the system UI. The second argument
	* indicates whether this is an error message.
	*/
	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		}
		if (msg != null && error) {
			System.err.println("ERROR: " + msg);
			//System.out.println(msg);
		}
	}

	/**
	* Gets a reference to a frame (needed for construction of dialogs).
	* First tries to get the main frame from the system UI. If this fails,
	* uses an invisible "dummy" frame
	*/
	protected Frame getFrame() {
		if (core != null && core.getUI() != null && core.getUI().getMainFrame() != null)
			return core.getUI().getMainFrame();
		return CManager.getAnyFrame();
	}
}
