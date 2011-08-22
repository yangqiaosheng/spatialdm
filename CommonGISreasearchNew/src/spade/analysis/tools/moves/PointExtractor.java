package spade.analysis.tools.moves;

import java.awt.Checkbox;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.util.Vector;

import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.BaseAnalyser;
import spade.lib.basicwin.Centimeter;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.StringUtil;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DLayerManager;
import spade.vis.dmap.DMovingObject;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.Computing;
import spade.vis.geometry.GeoComp;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.geometry.RealRectangle;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Oct 8, 2010
 * Time: 2:10:10 PM
 * Extracts specific poiunts from trajectories: turn points, stop points,
 * starts, ends, crossings
 */
public class PointExtractor extends BaseAnalyser {
	/**
	 * This method constructs and starts the tool. Everything the tool may need
	 * for integration with other components of the system can be received
	 * from the system's core passed as an argument.
	 */
	@Override
	public void run(ESDACore core) {
		if (core == null || core.getUI() == null)
			return;
		this.core = core;
		if (core.getUI().getCurrentMapViewer() == null || core.getUI().getCurrentMapViewer().getLayerManager() == null) {
			showMessage("No map exists!", true);
			return;
		}
		//Find instances of DGeoLayer containing trajectories
		LayerManager lman = core.getUI().getCurrentMapViewer().getLayerManager();
		Vector moveLayers = new Vector(lman.getLayerCount(), 1);
		boolean geo = false;
		float minx = Float.NaN, miny = Float.NaN, maxx = Float.NaN, maxy = Float.NaN;
		for (int i = 0; i < lman.getLayerCount(); i++) {
			GeoLayer layer = lman.getGeoLayer(i);
			if ((layer instanceof DGeoLayer) && layer.getObjectCount() > 0 && (layer.getObjectAt(0) instanceof DMovingObject)) {
				moveLayers.addElement(layer);
				geo = geo || layer.isGeographic();
				RealRectangle r = ((DGeoLayer) layer).getWholeLayerBounds();
				if (r == null) {
					r = ((DGeoLayer) layer).getCurrentLayerBounds();
				}
				if (r != null) {
					if (Float.isNaN(minx) || minx > r.rx1) {
						minx = r.rx1;
					}
					if (Float.isNaN(maxx) || maxx < r.rx2) {
						maxx = r.rx2;
					}
					if (Float.isNaN(miny) || miny > r.ry1) {
						miny = r.ry1;
					}
					if (Float.isNaN(maxy) || maxy < r.ry2) {
						maxy = r.ry2;
					}
				}
			}
		}
		if (moveLayers.size() < 1) {
			showMessage("No layers with trajectories found!", true);
			return;
		}
		Panel mainP = new Panel(new ColumnLayout());
		mainP.add(new Label("Select the layer with trajectories to extract points:"));
		List list = new List(Math.max(moveLayers.size() + 1, 5));
		for (int i = 0; i < moveLayers.size(); i++) {
			list.add(((DGeoLayer) moveLayers.elementAt(i)).getName());
		}
		list.select(0);
		mainP.add(list);
		Checkbox cbStarts = new Checkbox("start positions", false);
		mainP.add(cbStarts);
		Checkbox cbEnds = new Checkbox("end positions", false);
		mainP.add(cbEnds);
		Checkbox cbTurns = new Checkbox("positions of significant turns", false);
		mainP.add(cbTurns);
		Checkbox cbStops = new Checkbox("positions of stops", false);
		mainP.add(cbStops);
		Checkbox cbCrossings = new Checkbox("crossing points", false);
		Checkbox cbNoTime = new Checkbox("ignore time", false);
		Panel p = new Panel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		p.add(cbCrossings);
		p.add(cbNoTime);
		mainP.add(p);
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Extract points from trajectories", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return;
		int idx = list.getSelectedIndex();
		if (idx < 0)
			return;
		boolean getStarts = cbStarts.getState(), getEnds = cbEnds.getState(), getTurns = cbTurns.getState(), getStops = cbStops.getState(), getCrossings = cbCrossings.getState();
		if (!getStarts && !getEnds && !getStops && !getTurns && !getCrossings)
			return;
		float geoFactorX = 1f, geoFactorY = 1f;
		String parStr = "";
		if (getStarts) {
			parStr += "starts, ";
		}
		if (getEnds) {
			parStr += "ends, ";
		}
		if (getCrossings) {
			parStr += "crossings, ";
		}
		if (getTurns) {
			parStr += "turns, ";
		}
		if (getStops) {
			parStr += "stops, ";
		}
		parStr = parStr.substring(0, parStr.length() - 2);

		double wh[] = DGeoLayer.getExtentXY(minx, miny, maxx, maxy, geo);
		float width = (float) wh[0], height = (float) wh[1];
		if (geo) {
			geoFactorX = width / (maxx - minx);
			geoFactorY = height / (maxy - miny);
		}

		float angle = 0f;
		float minRad = 0f;
		long stopTime = 0l;

		if (getStops || getTurns) {
			float defMinRad = Math.min(width, height) / 200;
			float factor = 1;
			if (defMinRad > 1) {
				while (defMinRad >= 10) {
					factor *= 10;
					defMinRad /= 10;
				}
			} else {
				while (defMinRad < 1) {
					factor /= 10;
					defMinRad *= 10;
				}
			}
			if (defMinRad < 3) {
				defMinRad = 1;
			} else if (defMinRad < 7) {
				defMinRad = 5;
			} else {
				defMinRad = 10;
			}
			defMinRad *= factor;
			String minRadStr = StringUtil.floatToStr(defMinRad, 0, defMinRad * 10);
			TextField angleTF = null;
			TextField timeGapTF = null;
			p = new Panel();
			GridBagLayout gridbag = new GridBagLayout();
			p.setLayout(gridbag);
			GridBagConstraints c = new GridBagConstraints();
			c.weightx = 1.0f;
			c.weighty = 1.0f;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridwidth = GridBagConstraints.REMAINDER;
			Label l = new Label("Specify parameters for point extraction", Label.CENTER);
			gridbag.setConstraints(l, c);
			p.add(l);
			if (getTurns) {
				l = new Label("Minimum angle of direction change (degrees):");
				c.gridwidth = 3;
				gridbag.setConstraints(l, c);
				p.add(l);
				angleTF = new TextField("30", 10);
				c.gridwidth = GridBagConstraints.REMAINDER;
				gridbag.setConstraints(angleTF, c);
				p.add(angleTF);
			}
			if (getStops) {
				l = new Label("Minimum duration of a stop:");
				c.gridwidth = 3;
				gridbag.setConstraints(l, c);
				p.add(l);
				timeGapTF = new TextField("60", 10);
				c.gridwidth = GridBagConstraints.REMAINDER;
				gridbag.setConstraints(timeGapTF, c);
				p.add(timeGapTF);
			}
			l = new Label("Minimum radius around a position:");
			c.gridwidth = 3;
			gridbag.setConstraints(l, c);
			p.add(l);
			TextField minRadTF = new TextField(minRadStr, 10);
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(minRadTF, c);
			p.add(minRadTF);
			l = new Label("X-extent:", Label.RIGHT);
			c.gridwidth = 1;
			gridbag.setConstraints(l, c);
			p.add(l);
			TextField ttf = new TextField(StringUtil.floatToStr(width, 0f, Math.max(width, height)), 10);
			ttf.setEditable(false);
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(ttf, c);
			p.add(ttf);
			l = new Label("Y-extent:", Label.RIGHT);
			c.gridwidth = 1;
			gridbag.setConstraints(l, c);
			p.add(l);
			ttf = new TextField(StringUtil.floatToStr(height, 0f, Math.max(width, height)), 10);
			ttf.setEditable(false);
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(ttf, c);
			p.add(ttf);
			Panel pp = new Panel(new FlowLayout(FlowLayout.LEFT));
			pp.add(new Label("Scale:"));
			Centimeter cm = new Centimeter();
			pp.add(cm);
			float sc = core.getUI().getMapViewer(core.getUI().getCurrentMapN()).getMapDrawer().getMapContext().getPixelValue() * cm.getMinimumSize().width * ((geo) ? geoFactorX : ((DLayerManager) lman).user_factor);
			pp.add(new Label(StringUtil.floatToStr(sc, 2) + " " + ((geo) ? "m" : ((DLayerManager) lman).getUserUnit())));
			gridbag.setConstraints(pp, c);
			p.add(pp);

			dia = new OKDialog(core.getUI().getMainFrame(), "Specify parameters", true);
			dia.addContent(p);
			dia.show();
			if (dia.wasCancelled())
				return;
			if (angleTF != null) {
				String str = angleTF.getText();
				parStr += "; turn >=" + str;
				if (str != null) {
					try {
						angle = Float.valueOf(str).floatValue();
					} catch (NumberFormatException nfe) {
					}
				}
				if (angle < 10) {
					angle = 10;
				}
			}
			if (timeGapTF != null) {
				String str = timeGapTF.getText();
				parStr += "; stop time >=" + str;
				if (str != null) {
					try {
						stopTime = Long.valueOf(str).longValue();
					} catch (NumberFormatException nfe) {
					}
				}
				if (stopTime < 0) {
					stopTime = 0;
				}
			}
			String str = minRadTF.getText();
			if (str != null) {
				try {
					minRad = Float.valueOf(str).floatValue();
				} catch (NumberFormatException nfe) {
				}
			}
			parStr += "; radius =" + str;
		}

		double angleRadian = angle * Math.PI / 180;
		float minRadOrig = minRad;
		minRad /= geoFactorX;

		DGeoLayer moveLayer = (DGeoLayer) moveLayers.elementAt(idx);
		Vector<SpatialEntity> points = new Vector<SpatialEntity>(moveLayer.getObjectCount() * 5, 1000);
		DataTable pTable = new DataTable();
		pTable.addAttribute("Trajectory ID", "tr_id", AttributeTypes.character);
		//if (getStarts || getEnds || getStops || getTurns)
		pTable.addAttribute("Time", "time", AttributeTypes.time);
		int cIdx0 = pTable.getAttrCount();
		if (getCrossings) {
			pTable.addAttribute("Crossed trajectory ID", "cross_tr_id", AttributeTypes.character);
			pTable.addAttribute("Both trajectories IDs", "both_tr_id", AttributeTypes.character);
		}
		int n = 0;
		if (getStarts || getEnds || getStops || getTurns) {
			for (int i = 0; i < moveLayer.getObjectCount(); i++) {
				if (!moveLayer.isObjectActive(i)) {
					continue;
				}
				DGeoObject gobj = moveLayer.getObject(i);
				if (gobj instanceof DMovingObject) {
					DMovingObject mobj = (DMovingObject) gobj;
					Vector souTrack = mobj.getTrack();
					if (souTrack == null || souTrack.size() < 2) {
						continue;
					}
					Vector charPoints = TrUtil.getCharacteristicPoints(mobj.getTrack(), getStarts, getEnds, getStops, getTurns, angleRadian, stopTime, minRadOrig, geoFactorX, geoFactorY);
					if (charPoints == null || charPoints.size() < 1) {
						continue;
					}
					for (int j = 0; j < charPoints.size(); j++) {
						SpatialEntity spe = (SpatialEntity) charPoints.elementAt(j);
						if (spe == null) {
							continue;
						}
						spe = (SpatialEntity) spe.clone();
						spe.setId(mobj.getIdentifier() + "_" + (j + 1));
						DataRecord rec = new DataRecord(spe.getId());
						pTable.addDataRecord(rec);
						rec.setAttrValue(mobj.getIdentifier(), 0);
						if (spe.getTimeReference() != null) {
							rec.setAttrValue(spe.getTimeReference().getValidFrom(), 1);
						}
						spe.setThematicData(rec);
						points.addElement(spe);
					}
					++n;
					if (n % 100 == 0) {
						showMessage("Characteristic points: " + n + " trajectories processed", false);
					}
				}
			}
		}
		n = 0;
		int npOld = points.size();
		boolean useTime = !cbNoTime.getState();
		if (getCrossings) {
			double cosMinAngle = 1;
			if (!useTime) {
				angle = Dialogs.askForIntValue(getFrame(), "Minimum angle between lines?", 30, 0, 90, "Crossings between line segments will be computed when the angle between " + "them is not less than the given minimum angle.", "Compute crossings",
						true);
				if (angle >= 0 && angle <= 90) {
					parStr += "; crossing angle >=" + angle;
					cosMinAngle = Math.cos(angle * Math.PI / 180);
				}
			}
			showMessage("Computing crossings...", false);
			for (int i = 0; i < moveLayer.getObjectCount() - 1; i++) {
				if (n > 0 && (n < 20 || n % 20 == 0)) {
					showMessage("Crossings: " + n + " trajectories processed, " + (points.size() - npOld) + " points extracted", false);
				}
				if (!moveLayer.isObjectActive(i)) {
					continue;
				}
				DGeoObject gobj = moveLayer.getObject(i);
				if (gobj instanceof DMovingObject) {
					DMovingObject mobj1 = (DMovingObject) gobj;
					RealPolyline track1 = (RealPolyline) mobj1.getGeometry();
					if (track1 == null || track1.p == null || track1.p.length < 2) {
						continue;
					}
					TimeReference tr1 = mobj1.getTimeReference();
					RealRectangle b1 = mobj1.getBounds();
					for (int j = i + 1; j < moveLayer.getObjectCount(); j++) {
						if (!moveLayer.isObjectActive(j)) {
							continue;
						}
						gobj = moveLayer.getObject(j);
						if (gobj instanceof DMovingObject) {
							DMovingObject mobj2 = (DMovingObject) gobj;
							RealPolyline track2 = (RealPolyline) mobj2.getGeometry();
							if (track2 == null || track2.p == null || track2.p.length < 2) {
								continue;
							}
							RealRectangle b2 = mobj2.getBounds();
							RealRectangle commonRect = b1.intersect(b2);
							if (commonRect == null) {
								continue;
							}
							TimeReference commonTime = null;
							if (useTime) {
								TimeReference tr2 = mobj2.getTimeReference();
								commonTime = TimeReference.intersection(tr1, tr2);
								if (commonTime == null) {
									continue;
								}
							}
							int nCrosses = 0;
							for (int k1 = 0; k1 < track1.p.length - 1; k1++) {
								RealPoint p1 = track1.p[k1], p2 = track1.p[k1 + 1];
								RealRectangle commonSubRect = commonRect.intersect(p1.x, p1.y, p2.x, p2.y);
								if (commonSubRect == null) {
									continue;
								}
								TimeReference tSegm1 = null;
								if (useTime) {
									tSegm1 = getTimeOfTrackSegment(mobj1, k1);
									if (tSegm1 == null) {
										continue;
									}
									if (!TimeReference.doIntersect(commonTime, tSegm1)) {
										continue;
									}
								}
								float dx = Float.NaN, dy = Float.NaN;
								if (!useTime && cosMinAngle < 1) {
									dx = (p2.x - p1.x) * geoFactorX;
									dy = (p2.y - p1.y) * geoFactorY;
									if (Math.abs(dx) <= 0 && Math.abs(dy) <= 0) {
										continue;
									}
								}
								for (int k2 = 0; k2 < track2.p.length - 1; k2++) {
									RealPoint p3 = track2.p[k2], p4 = track2.p[k2 + 1];
									if (!commonSubRect.doesIntersect(p3.x, p3.y, p4.x, p4.y)) {
										continue;
									}
									TimeReference tSegm2 = null, tSegmCommon = null;
									if (useTime) {
										tSegm2 = getTimeOfTrackSegment(mobj2, k2);
										if (tSegm2 == null) {
											continue;
										}
										tSegmCommon = TimeReference.intersection(tSegm1, tSegm2);
										if (tSegmCommon == null) {
											continue;
										}
									}
									float dx2 = Float.NaN, dy2 = Float.NaN;
									if (!useTime && cosMinAngle < 1) {
										dx2 = (p4.x - p3.x) * geoFactorX;
										dy2 = (p4.y - p3.y) * geoFactorY;
										if (Math.abs(dx2) <= 0 && Math.abs(dy2) <= 0) {
											continue;
										}
									}
									RealPoint ip = Computing.findIntersection(p1, p2, p3, p4);
									if (ip == null) {
										continue;
									}
									if (!useTime && cosMinAngle < 1) {
										double cos = GeoComp.getCosAngleBetweenVectors(dx, dy, dx2, dy2);
										if (cos > cosMinAngle) {
											continue;
										}
									}
									TimeMoment it = null;
									if (tSegmCommon != null) {
										it = tSegmCommon.getValidFrom();
										if (!tSegm1.getValidUntil().equals(tSegm1.getValidFrom())) {
											it = tSegm1.getValidFrom();
											double sLen = dx * dx + dy * dy;
											if (sLen > 0) {
												double ddx = ip.x - p1.x, ddy = ip.y - p1.y;
												double sLenInter = ddx * ddx + ddy * ddy;
												if (sLenInter > 0) {
													double ratio = Math.sqrt(sLenInter) / Math.sqrt(sLen);
													long iLen = tSegm1.getValidUntil().subtract(tSegm1.getValidFrom());
													long iLenInter = Math.round(iLen * ratio);
													if (iLenInter > 0) {
														it = it.getCopy();
														it.add(iLenInter);
													}
												}
											}
											if (it.compareTo(tSegmCommon.getValidFrom()) < 0 || it.compareTo(tSegmCommon.getValidUntil()) > 0) {
												continue; //do not intersect during the common time interval
											}
										}
									}
									++nCrosses;
									SpatialEntity spe = new SpatialEntity("cross_" + mobj1.getIdentifier() + "_" + mobj2.getIdentifier() + "_" + nCrosses);
									spe.setGeometry(ip);
									if (tSegmCommon != null) {
										spe.setTimeReference(tSegmCommon);
									}
									DataRecord rec = new DataRecord(spe.getId());
									pTable.addDataRecord(rec);
									rec.setAttrValue(mobj1.getIdentifier(), 0);
									if (it != null) {
										rec.setAttrValue(it, 1);
									}
									rec.setAttrValue(mobj2.getIdentifier(), cIdx0);
									rec.setAttrValue(mobj1.getIdentifier() + ";" + mobj2.getIdentifier(), cIdx0 + 1);
									points.addElement(spe);
								}
							}
						}
					}
					++n;
				}
			}
		}
		if (points.size() < 1) {
			showMessage("No points have been extracted!", true);
			return;
		}
		showMessage(points.size() + " points have been extracted!", false);
		String name = "Points from " + moveLayer.getName() + ": " + parStr;
/*
    name=Dialogs.askForStringValue(getFrame(),"Name of the layer?",name,
      "A new map layer with "+points.size()+" point objects will be created","Map layer with points",true);
    if (name==null)
      return;
*/
		DGeoLayer ptLayer = new DGeoLayer();
		ptLayer.setName(name);
		ptLayer.setType(Geometry.point);
		DrawingParameters dp = ptLayer.getDrawingParameters();
		if (dp == null) {
			dp = new DrawingParameters();
			ptLayer.setDrawingParameters(dp);
		}
		dp.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
		dp.fillColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
		dp.fillContours = true;
		for (int i = 0; i < points.size(); i++) {
			DGeoObject dgo = new DGeoObject();
			dgo.setup(points.elementAt(i));
			ptLayer.addGeoObject(dgo, false);
		}
		ptLayer.setDataTable(pTable);
		pTable.setName(ptLayer.getName());
		DataLoader dLoader = core.getDataLoader();
		int tblN = dLoader.addTable(pTable);
		dLoader.addMapLayer(ptLayer, -1);
		dLoader.setLink(ptLayer, tblN);
		if (getStarts || getEnds || getStops || getTurns || (getCrossings && useTime)) {
			dLoader.processTimeReferencedObjectSet(ptLayer);
		}
	}

	protected TimeReference getTimeOfTrackSegment(DMovingObject mobj, int idx) {
		if (mobj == null)
			return null;
		TimeReference tr1 = mobj.getPositionTime(idx);
		if (tr1 == null)
			return null;
		TimeReference tr2 = mobj.getPositionTime(idx + 1);
		if (tr2 == null)
			return null;
		TimeMoment t1 = tr1.getValidFrom(), t2 = tr2.getValidUntil();
		if (t2 == null) {
			t2 = tr2.getValidFrom();
		}
		if (t1 == null || t2 == null)
			return null;
		return new TimeReference(t1, t2);
	}
}
