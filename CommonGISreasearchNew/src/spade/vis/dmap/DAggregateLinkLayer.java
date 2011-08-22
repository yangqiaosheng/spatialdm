package spade.vis.dmap;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.beans.PropertyChangeEvent;
import java.util.Vector;

import spade.analysis.classification.ObjectColorer;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.StringInRectangle;
import spade.lib.util.StringUtil;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.TimeFilter;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapContext;
import spade.vis.mapvis.Visualizer;
import spade.vis.space.GeoLayer;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 13-Aug-2007
 * Time: 16:45:29
 * Includes aggregated links, instances of DAggregateLinkObject.
 * Shows the number of links in an aggregate by line thickness.
 * Reacts fo filtering applied to the original links and trajectories.
 */
public class DAggregateLinkLayer extends DGeoLayer implements LinkedToMapLayers {
	/**
	 * The maximum possible thickness of a line
	 */
	public static int absMaxThickness = Math.round(5 * Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f);
	/**
	 * The current maximum thickness of a line
	 */
	protected int maxThickness = 0;
	/**
	 * A reference to a layer with the original trajectories the links have
	 * been constructed from. May be null.
	 */
	protected DGeoLayer trajLayer = null;
	/**
	 * The parameters used for generating this layer
	 */
	public boolean onlyActiveTrajectories = false;
	public boolean onlyStartsEnds = false;
	public boolean findIntersections = true;
	/**
	 * A reference to a layer with the simplified (generalised) trajectories
	 * the links have been constructed from. May be null.
	 */
	protected DGeoLayer simpleTrajLayer = null;
	/**
	 * A reference to a layer with the places connected by the links
	 */
	protected DGeoLayer placeLayer = null;
	/**
	 * The maximum number of links in an aggregate
	 */
	protected int maxNLinks = 0;
	/**
	 * The aggregates having less than this number of active links
	 * will be hidden from the view (not drawn)
	 */
	protected int minShownNLinks = 0;
	/**
	 * The identifiers of the active trajectories, i.e. trajectories satisfying
	 * the filters of the layers trajLayer and simpleTrajLayer
	 */
	protected Vector activeTrajIds = null;
	/**
	 * The last time interval selected with the use of the time filter.
	 */
	protected TimeMoment tStart = null, tEnd = null;
	/**
	 * Used for preparation of the layer before the first drawing
	 */
	protected boolean neverDrawn = true;
	/**
	 * Any object that assigns colors to the trajectories
	 */
	protected ObjectColorer trajColorer = null;
	/**
	 * Fields in the table with thematic data, which are re-computed
	 * after changes of the filtering of the trajectories
	 */
	public int startTimeIdxActive = -1, endTimeIdxActive = -1, nMovesIdxActive = -1, minDurIdxActive = -1, avgDurIdxActive = -1, maxDurIdxActive = -1, nTrajIdxActive = -1, trIdsIdxActive = -1;
	/**
	 * The number of the table column to be represented by line thickness.
	 * If -1, the line thickness represents the number of active moves.
	 */
	public int thicknessColN = -1;
	/**
	 * Indicates that the attribute with the index thicknessColN means
	 * a quantity represented by integer numbers. In this case, the
	 * arrow will not be thicker than the number it represents.
	 */
	public boolean thColumnContainsNumbers = false;
	/**
	 * The maximum value in the column represented by line thickness
	 */
	protected double thicknessMaxValue = 0;

	/**
	 * Returns the reference to the layer with the original trajectories
	 * the links have been constructed from. May be null.
	 */
	public DGeoLayer getTrajectoryLayer() {
		return trajLayer;
	}

	/**
	 * Sets a reference to the layer with the original trajectories
	 * the links have been constructed from. Starts listening to
	 * changes of the layer filter.
	 */
	public void setTrajectoryLayer(DGeoLayer trajLayer) {
		if (this.trajLayer != null) {
			this.trajLayer.removePropertyChangeListener(this);
		}
		this.trajLayer = trajLayer;
		if (trajLayer != null) {
			trajLayer.addPropertyChangeListener(this);
			if (trajColorer == null) {
				trajColorer = tryFindTrajectoryColorer();
			}
		} else {
			trajColorer = null;
		}
	}

	/**
	 * Returns the reference to the layer with the simplified trajectories
	 * the links have been constructed from. May be null.
	 */
	public DGeoLayer getSimpleTrajectoryLayer() {
		return simpleTrajLayer;
	}

	/**
	 * Sets a reference to the layer with the simplified trajectories
	 * the links have been constructed from. Starts listening to
	 * changes of the layer filter.
	 */
	public void setSimpleTrajectoryLayer(DGeoLayer simpleTrajLayer) {
		if (this.simpleTrajLayer != null) {
			this.simpleTrajLayer.removePropertyChangeListener(this);
		}
		this.simpleTrajLayer = simpleTrajLayer;
		if (simpleTrajLayer != null) {
			simpleTrajLayer.addPropertyChangeListener(this);
			if (trajColorer == null) {
				trajColorer = tryFindTrajectoryColorer();
			}
		}
	}

	/**
	 * Returns the reference to a layer with the places connected by the links,
	 * if available.
	 */
	public DGeoLayer getPlaceLayer() {
		return placeLayer;
	}

	/**
	 * Sets a reference to a layer with the places connected by the links
	 */
	public void setPlaceLayer(DGeoLayer placeLayer) {
		this.placeLayer = placeLayer;
	}

	/**
	 * Tries to find a classifier of either original or simplified trajectories
	 */
	protected ObjectColorer tryFindTrajectoryColorer() {
		if (trajLayer != null) {
			if (trajLayer.getVisualizer() != null && trajLayer.getVisualizer().getObjectColorer() != null)
				return trajLayer.getVisualizer().getObjectColorer();
			if (trajLayer.getBackgroundVisualizer() != null && trajLayer.getBackgroundVisualizer().getObjectColorer() != null)
				return trajLayer.getBackgroundVisualizer().getObjectColorer();
		}
		if (simpleTrajLayer == null)
			return null;
		if (simpleTrajLayer.getVisualizer() != null && simpleTrajLayer.getVisualizer().getObjectColorer() != null)
			return simpleTrajLayer.getVisualizer().getObjectColorer();
		if (simpleTrajLayer.getBackgroundVisualizer() != null)
			return simpleTrajLayer.getBackgroundVisualizer().getObjectColorer();
		return null;
	}

	public void setThicknessColN(int thicknessColN, boolean columnContainsNumbers) {
		if (this.thicknessColN == thicknessColN)
			return;
		this.thicknessColN = thicknessColN;
		thColumnContainsNumbers = columnContainsNumbers;
		findThicknessMaxValue();
		notifyPropertyChange("ObjectData", null, null);
	}

	/**
	 * The aggregates having less than this number of active links
	 * will be hidden from the view (not drawn)
	 */
	public void setMinShownNLinks(int minShownNLinks) {
		this.minShownNLinks = minShownNLinks;
	}

	@Override
	public void draw(Graphics g, MapContext mc) {
		if (geoObj == null || geoObj.size() < 1 || g == null || mc == null)
			return;
		if (neverDrawn) {
			neverDrawn = false;
			findActiveTrajectories();
			countActiveLinks();
			if (dTable != null)
				return;
		}
		lastPixelValue = mc.getPixelValue();
		boolean timeFilterSame = false;
		TimeFilter tf = getTimeFilter();
		if (tf == null) {
			timeFilterSame = tStart == null && tEnd == null;
		} else {
			TimeMoment t1 = tf.getFilterPeriodStart(), t2 = tf.getFilterPeriodEnd();
			timeFilterSame = ((t1 == null && tStart == null) || (t1 != null && tStart != null && t1.equals(tStart))) && ((t2 == null && tEnd == null) || (t2 != null && tEnd != null && t2.equals(tEnd)));
		}
		if (!timeFilterSame) {
			countActiveLinks();
			if (dTable != null)
				return;
		}
		if (maxNLinks < 1)
			return;
		Visualizer v = null;
		if (vis != null && vis.isEnabled()) {
			v = vis;
		} else if (bkgVis != null && bkgVis.isEnabled()) {
			v = bkgVis;
		}

		maxThickness = absMaxThickness;
		boolean showAttribute = thicknessColN >= 0 && thicknessMaxValue > 0;
		if (!showAttribute && maxThickness > maxNLinks) {
			maxThickness = maxNLinks;
		} else if (showAttribute && thColumnContainsNumbers && maxThickness > thicknessMaxValue) {
			maxThickness = (int) Math.round(thicknessMaxValue);
		}
		RealRectangle rr = mc.getVisibleTerritory();
		drawParm.fillContours = false;
		int origTh = drawParm.lineWidth;
		Color drawParmColor = drawParm.lineColor, defColor = drawParmColor;
		if (trajColorer != null) {
			defColor = Color.darkGray;
		}
		nActive = 0;
		for (int i = 0; i < geoObj.size(); i++)
			if (isObjectActive(i)) {
				++nActive;
				DAggregateLinkObject aggLink = (DAggregateLinkObject) geoObj.elementAt(i);
				aggLink.setVisualizer(v);
				if (aggLink.nActiveLinks < minShownNLinks) {
					continue;
				}
				if (!aggLink.fitsInRectangle(rr.rx1, rr.ry1, rr.rx2, rr.ry2)) {
					continue;
				}
				if (v == null) {
					int th = 1;
					if (showAttribute) {
						DataRecord rec = (DataRecord) aggLink.getData();
						if (rec != null) {
							Double val = rec.getNumericAttrValue(thicknessColN);
							if (!Double.isNaN(val) && val > 0) {
								th = (int) Math.round(val * maxThickness / thicknessMaxValue);
							}
						}
					} else {
						th = Math.round(1.0f * aggLink.nActiveLinks * maxThickness / maxNLinks);
					}
					if (th < 1) {
						th = 1;
					}
					drawParm.lineWidth = th;
					drawParm.lineColor = (aggLink.color != null) ? aggLink.color : defColor;
				} else if (aggLink.color != null) {
					drawParm.lineColor = aggLink.color;
				}
				aggLink.setDrawingParameters(drawParm);
				aggLink.draw(g, mc);
			}
		drawParm.lineWidth = origTh;
		drawParm.lineColor = drawParmColor;
	}

	/**
	 * Returns the current maximum thickness of a line
	 */
	public int getMaxThickness() {
		return maxThickness;
	}

	/**
	 * Puts additional information (if any) about a layer in the legend immediately below
	 * the line with the layer name and icon
	 */
	@Override
	public Rectangle showAdditionalLayerInfo(Graphics g, int startY, int leftMarg, int prefW) {
		if (g == null)
			return null;
		FontMetrics fm = g.getFontMetrics();
		if (fm == null)
			return null;
		g.setColor(Color.black);
		Visualizer v = null;
		if (vis != null && vis.isEnabled()) {
			v = vis;
		} else if (bkgVis != null && bkgVis.isEnabled()) {
			v = bkgVis;
		}
		if (v != null)
			return null;
		boolean showAttribute = thicknessColN >= 0 && thicknessMaxValue > 0;
		int w = 0, h = 0;
		String str = "Shown: ";
		if (showAttribute) {
			str += dTable.getAttributeName(thicknessColN);
		} else {
			boolean hasTrajectories = trajLayer != null && trajLayer.getObjectCount() > 0 && (trajLayer.getObject(0) instanceof DMovingObject);
			str += "number of " + ((hasTrajectories) ? "moves" : "trips");
		}
		Point p = StringInRectangle.drawText(g, str, leftMarg, startY, prefW, false);
		h = p.y - startY;
		w = p.x;
		str = "Maximum: " + String.valueOf((showAttribute) ? thicknessMaxValue : maxNLinks);
		int w1 = fm.stringWidth(name);
		g.drawString(str, leftMarg, startY + h + fm.getAscent());
		if (w1 > w) {
			w = w1;
		}
		h += fm.getHeight() + 1;
		return new Rectangle(leftMarg, startY, w, h);
	}

	/**
	* Determines whether the object is active, i.e. not filtered out.
	*/
	@Override
	public boolean isObjectActive(DGeoObject gobj) {
		if (gobj == null || !(gobj instanceof DAggregateLinkObject))
			return false;
		DAggregateLinkObject aggLink = (DAggregateLinkObject) gobj;
		if (aggLink.nActiveLinks < 1)
			return false;
		if (oFilter == null || !oFilter.areObjectsFiltered())
			return true;
		return oFilter.isActive(gobj.getSpatialData());
	}

	/**
	* Determines whether the object with the given index is active, i.e. not
	* filtered out.
	*/
	@Override
	public boolean isObjectActive(int idx) {
		if (idx < 0 || geoObj == null || idx >= geoObj.size())
			return false;
		if (!(geoObj.elementAt(idx) instanceof DAggregateLinkObject))
			return false;
		DAggregateLinkObject aggLink = (DAggregateLinkObject) geoObj.elementAt(idx);
		if (aggLink.nActiveLinks < 1)
			return false;
		return super.isObjectActive(idx);
	}

	/**
	 * Overrides the method from the superclass in order to ignore the last argument
	 */
	@Override
	public Vector findObjectsAt(int x, int y, MapContext mc, boolean findOne) {
		if (!drawParm.drawLayer)
			return null;
		if (mc == null)
			return null;
		int nObj = getObjectCount();
		boolean ordered = order != null && order.length > 0;
		if (ordered) {
			nObj = order.length;
		}
		Vector pointed = new Vector(10, 10);
		float rx = mc.absX(x), ry = mc.absY(y);
		for (int i = nObj - 1; i >= 0; i--) {
			//the backward order is important: smaller contours are drawn later
			int ii = (ordered) ? order[i] : i;
			if (!isObjectActive(ii)) {
				continue;
			}
			DGeoObject gobj = getObject(ii);
			if (gobj.contains(rx, ry, 0.5f * Metrics.mm() * mc.getPixelValue())) {
				pointed.addElement(gobj.getIdentifier());
			}
		}
		if (pointed.size() < 1)
			return null;
		return pointed;
	}

	/**
	 * Reacts to changes of the filter of the trajectory layer(s)
	 */
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (dTable != null && pce.getSource() == dTable && pce.getPropertyName().equals("values") && (vis == null || !vis.isEnabled()) && (bkgVis == null || !bkgVis.isEnabled()) && thicknessColN >= 0) {
			Vector attrs = (Vector) pce.getNewValue();
			if (attrs == null || attrs.size() < 1)
				return;
			String aId = dTable.getAttributeId(thicknessColN);
			if (StringUtil.isStringInVectorIgnoreCase(aId, attrs)) {
				findThicknessMaxValue();
				notifyPropertyChange("ObjectData", null, null);
			}
			return;
		}
		if (!pce.getSource().equals(trajLayer) && !pce.getSource().equals(simpleTrajLayer)) {
			super.propertyChange(pce);
			return;
		}
		if (pce.getPropertyName().equals("ObjectFilter")) {
			if (findActiveTrajectories()) {
				countActiveLinks();
				notifyPropertyChange("VisParameters", null, null);
			}
		} else if (pce.getPropertyName().equals("VisParameters") || pce.getPropertyName().equals("Visualization")) {
			DGeoLayer trLayer = (DGeoLayer) pce.getSource();
			boolean colorsChanged = false;
			ObjectColorer oCol = null;
			if (trLayer.getVisualizer() != null) {
				oCol = trLayer.getVisualizer().getObjectColorer();
			}
			if (oCol == null && trLayer.getBackgroundVisualizer() != null) {
				oCol = trLayer.getBackgroundVisualizer().getObjectColorer();
			}
			if (oCol != null)
				if (trajColorer != null) {
					colorsChanged = oCol.equals(trajColorer);
				} else {
					trajColorer = oCol;
					colorsChanged = true;
				}
			else {
				oCol = tryFindTrajectoryColorer();
				if ((oCol == null && trajColorer != null) || (oCol != null && !oCol.equals(trajColorer))) {
					trajColorer = oCol;
					colorsChanged = true;
				}
			}
			if (colorsChanged) {
				countActiveLinks();
				notifyPropertyChange("VisParameters", null, null);
			}
		}
	}

	/**
	 * Finds out which trajectories are currently active according to the filter of the
	 * layer(s) with the trajectories. Returns true if the list of active trajectories
	 * has really changed
	 */
	protected boolean findActiveTrajectories() {
		if (trajLayer == null && simpleTrajLayer == null)
			return false;
		int nTraj = (trajLayer == null) ? simpleTrajLayer.getObjectCount() : trajLayer.getObjectCount();
		if (nTraj < 1)
			return false;
		boolean changed = false;
		if (activeTrajIds == null) {
			activeTrajIds = new Vector(nTraj, 1);
			changed = true;
		}
		for (int i = 0; i < nTraj; i++) {
			boolean active = (trajLayer == null || trajLayer.isObjectActive(i)) && (simpleTrajLayer == null || simpleTrajLayer.isObjectActive(i));
			String trId = (trajLayer == null) ? simpleTrajLayer.getObjectId(i) : trajLayer.getObjectId(i);
			int idx = activeTrajIds.indexOf(trId);
			if (active)
				if (idx >= 0) {
					;
				} else {
					activeTrajIds.addElement(trId);
					changed = true;
				}
			else if (idx >= 0) {
				activeTrajIds.removeElementAt(idx);
				changed = true;
			}
		}
		return changed;
	}

	protected void addFieldsToTable() {
		if (dTable != null && dTable.getDataItemCount() > 0 && nMovesIdxActive < 0 && trajLayer != null) {
			boolean hasTrajectories = trajLayer.getObjectCount() > 0 && (trajLayer.getObject(0) instanceof DMovingObject);
			String moveStr = (hasTrajectories) ? "moves" : "trips";
			dTable.addAttribute("N of active " + moveStr, "n_moves_active", AttributeTypes.integer);
			nMovesIdxActive = dTable.getAttrCount() - 1;
			if (hasTrajectories) {
				dTable.addAttribute("N of different active trajectories", "n_traj_active", AttributeTypes.integer);
				nTrajIdxActive = dTable.getAttrCount() - 1;
			}
			dTable.addAttribute("Earliest start time for active " + moveStr, "startTime_active", AttributeTypes.time);
			startTimeIdxActive = dTable.getAttrCount() - 1;
			dTable.addAttribute("Latest end time for active " + moveStr, "endTime_active", AttributeTypes.time);
			endTimeIdxActive = dTable.getAttrCount() - 1;
			dTable.addAttribute("Min duration among active " + moveStr, "min_dur_active", AttributeTypes.integer);
			minDurIdxActive = dTable.getAttrCount() - 1;
			dTable.addAttribute("Avg duration among active " + moveStr, "avg_dur_active", AttributeTypes.integer);
			avgDurIdxActive = dTable.getAttrCount() - 1;
			dTable.addAttribute("Max duration among active " + moveStr, "max_dur_active", AttributeTypes.integer);
			maxDurIdxActive = dTable.getAttrCount() - 1;
			if (hasTrajectories) {
				dTable.addAttribute("IDs of active trajectories", "trIds_active", AttributeTypes.character);
				trIdsIdxActive = dTable.getAttrCount() - 1;
			}
		}
	}

	/**
	 * Counts the number of active links in each aggregate taking into account
	 * the filter(s) of the trajectory layer(s) and the time filter of this
	 * layer.
	 */
	public void countActiveLinks() {
		TimeFilter tf = getTimeFilter();
		TimeMoment t1 = null, t2 = null;
		boolean timeFiltered = false;
		if (tf != null) {
			t1 = tf.getFilterPeriodStart();
			t2 = tf.getFilterPeriodEnd();
			timeFiltered = t1 != null || t2 != null;
		}
		tStart = (t1 == null) ? null : t1.getCopy();
		tEnd = (t2 == null) ? null : t2.getCopy();
		maxNLinks = 0;
		thicknessMaxValue = 0;
		addFieldsToTable();
		for (int i = 0; i < geoObj.size(); i++)
			if (geoObj.elementAt(i) instanceof DAggregateLinkObject) {
				DAggregateLinkObject aggLink = (DAggregateLinkObject) geoObj.elementAt(i);
				DataRecord rec = null;
				if (nMovesIdxActive >= 0 && aggLink.getData() != null && (aggLink.getData() instanceof DataRecord)) {
					rec = (DataRecord) aggLink.getData();
					rec.setAttrValue(null, startTimeIdxActive);
					rec.setAttrValue(null, endTimeIdxActive);
					rec.setNumericAttrValue(0, "0", nMovesIdxActive);
					rec.setNumericAttrValue(0, "0", minDurIdxActive);
					rec.setNumericAttrValue(0, "0", avgDurIdxActive);
					rec.setNumericAttrValue(0, "0", maxDurIdxActive);
					if (nTrajIdxActive >= 0) {
						rec.setNumericAttrValue(0, "0", nTrajIdxActive);
					}
					if (trIdsIdxActive >= 0) {
						rec.setAttrValue(null, trIdsIdxActive);
					}
				}
				aggLink.nActiveLinks = 0;
				if (trajColorer != null) {
					aggLink.color = null;
				}
				if (aggLink.souLinks == null || aggLink.souLinks.size() < 1) {
					continue;
				}
				long minDur = 0, maxDur = 0, avgDur = 0, avgDurN = 0;
				Vector trIds = new Vector(aggLink.souTrajIds.size(), 1);
				String trIdsStr = "";
				TimeMoment tFirst = null, tLast = null;
				for (int j = 0; j < aggLink.souLinks.size(); j++) {
					DLinkObject link = (DLinkObject) aggLink.souLinks.elementAt(j);
					String trId = null;
					if (activeTrajIds != null && aggLink.souTrajIds != null) {
						trId = (String) aggLink.souTrajIds.elementAt(j);
						if (trId != null && !activeTrajIds.contains(trId)) {
							continue;
						}
					}
					if (timeFiltered && link.startTime != null)
						if (!tf.isActive(link.getSpatialData().getTimeReference())) {
							continue;
						}
					++aggLink.nActiveLinks;
					if (rec != null) {
						if (!trIds.contains(trId)) {
							trIds.addElement(trId);
							if (trIds.size() > 1) {
								trIdsStr += ";";
							}
							trIdsStr += trId;
						}
						TimeReference tref = link.getSpatialData().getTimeReference();
						if (tref != null) {
							TimeMoment from = tref.getValidFrom(), until = tref.getValidUntil();
							if (from != null && (tFirst == null || tFirst.compareTo(from) > 0)) {
								tFirst = from;
							}
							if (until != null && (tLast == null || tLast.compareTo(until) < 0)) {
								tLast = until;
							}
							if (from != null && until != null) {
								long dur = tref.getValidUntil().subtract(tref.getValidFrom());
								if (dur > 0) {
									avgDur += dur;
									avgDurN++;
									if (maxDur == 0) {
										minDur = dur;
										maxDur = dur;
									} else if (maxDur < dur) {
										maxDur = dur;
									} else if (minDur > dur) {
										minDur = dur;
									}
								}
							}
						}
					}
					if (trajColorer != null && (aggLink.color != null || (aggLink.nActiveLinks == 1 && trId != null))) {
						Color color = trajColorer.getColorForObject(trId);
						if (color == null) {
							aggLink.color = null;
						} else if (aggLink.nActiveLinks == 1) {
							aggLink.color = color;
						} else if (!color.equals(aggLink.color)) {
							aggLink.color = null;
						}
					}
				}
				if (maxNLinks < aggLink.nActiveLinks) {
					maxNLinks = aggLink.nActiveLinks;
				}
				if (rec != null) {
					rec.setAttrValue(tFirst, startTimeIdxActive);
					rec.setAttrValue(tLast, endTimeIdxActive);
					rec.setNumericAttrValue(aggLink.nActiveLinks, String.valueOf(aggLink.nActiveLinks), nMovesIdxActive);
					rec.setNumericAttrValue(minDur, String.valueOf(minDur), minDurIdxActive);
					if (avgDurN > 0) {
						avgDur /= avgDurN;
					}
					rec.setNumericAttrValue(avgDur, String.valueOf(avgDur), avgDurIdxActive);
					rec.setNumericAttrValue(maxDur, String.valueOf(maxDur), maxDurIdxActive);
					if (nTrajIdxActive >= 0) {
						rec.setNumericAttrValue(trIds.size(), String.valueOf(trIds.size()), nTrajIdxActive);
					}
					if (trIdsIdxActive >= 0) {
						rec.setAttrValue(trIdsStr, trIdsIdxActive);
					}
					if (aggLink.nActiveLinks > 0 && thicknessColN >= 0) {
						Double val = rec.getNumericAttrValue(thicknessColN);
						if (!Double.isNaN(val) && val > thicknessMaxValue) {
							thicknessMaxValue = val;
						}
					}
				}
			}
		if (dTable != null) {
			//notify about changed data in the table
			Vector attr = new Vector(dTable.getAttrCount(), 1);
			attr.addElement(dTable.getAttributeId(startTimeIdxActive));
			attr.addElement(dTable.getAttributeId(endTimeIdxActive));
			attr.addElement(dTable.getAttributeId(nMovesIdxActive));
			attr.addElement(dTable.getAttributeId(minDurIdxActive));
			attr.addElement(dTable.getAttributeId(avgDurIdxActive));
			attr.addElement(dTable.getAttributeId(maxDurIdxActive));
			attr.addElement(dTable.getAttributeId(nTrajIdxActive));
			attr.addElement(dTable.getAttributeId(trIdsIdxActive));
			dTable.notifyPropertyChange("values", null, attr);
		}
	}

	protected void findThicknessMaxValue() {
		thicknessMaxValue = 0;
		if (thicknessColN < 0)
			return;
		for (int i = 0; i < geoObj.size(); i++)
			if (isObjectActive(i)) {
				DAggregateLinkObject aggLink = (DAggregateLinkObject) geoObj.elementAt(i);
				DataRecord rec = (DataRecord) aggLink.getData();
				if (rec == null) {
					continue;
				}
				Double val = rec.getNumericAttrValue(thicknessColN);
				if (!Double.isNaN(val) && val > thicknessMaxValue) {
					thicknessMaxValue = val;
				}
			}
	}

	/**
	 * Returns the identifiers of the active trajectories (i.e. satisfying all
	 * current filters)
	 */
	public Vector getActiveTrajIds() {
		return activeTrajIds;
	}

	/**
	* Returns its time filter, if available
	*/
	@Override
	public TimeFilter getTimeFilter() {
		TimeFilter tf = null;
		if (trajLayer != null) {
			tf = trajLayer.getTimeFilter();
		}
		if (tf != null)
			return tf;
		if (simpleTrajLayer != null) {
			tf = simpleTrajLayer.getTimeFilter();
		}
		if (tf != null)
			return tf;
		return super.getTimeFilter();
	}

	/**
	* Returns a copy of this GeoLayer. The reference to the visualizer is also copied.
	*/
	@Override
	public GeoLayer makeCopy() {
		DAggregateLinkLayer layer = new DAggregateLinkLayer();
		copyTo(layer);
		layer.setTrajectoryLayer(trajLayer);
		layer.setSimpleTrajectoryLayer(simpleTrajLayer);
		layer.setPlaceLayer(placeLayer);
		return layer;
	}

	/**
	 * Among the given copies of layers, looks for the copies of the
	 * layers this layer is linked to (by their identifiers) and replaces
	 * the references to the original layers by the references to their copies.
	 */
	@Override
	public void checkAndCorrectLinks(Vector copiesOfMapLayers) {
		if ((trajLayer == null && simpleTrajLayer == null) || copiesOfMapLayers == null || copiesOfMapLayers.size() < 1)
			return;
		boolean trajLayerOK = trajLayer == null;
		boolean simpleTrajLayerOK = simpleTrajLayer == null;
		boolean placeLayerOK = placeLayer == null;
		for (int i = 0; i < copiesOfMapLayers.size() && (!trajLayerOK || !simpleTrajLayerOK || !placeLayerOK); i++)
			if (copiesOfMapLayers.elementAt(i) instanceof DGeoLayer) {
				DGeoLayer layer = (DGeoLayer) copiesOfMapLayers.elementAt(i);
				if (!trajLayerOK && layer.getEntitySetIdentifier().equals(trajLayer.getEntitySetIdentifier())) {
					setTrajectoryLayer(layer);
					trajLayerOK = true;
				} else if (!simpleTrajLayerOK && layer.getEntitySetIdentifier().equals(simpleTrajLayer.getEntitySetIdentifier())) {
					setSimpleTrajectoryLayer(layer);
					simpleTrajLayerOK = true;
				} else if (!placeLayerOK && layer.getEntitySetIdentifier().equals(placeLayer.getEntitySetIdentifier())) {
					setPlaceLayer(layer);
					placeLayerOK = true;
				}
			}
	}

	/**
	 * Removes itself from listeners of the filtering events of the layers
	 * with the trajectories
	 */
	@Override
	public void destroy() {
		if (destroyed)
			return;
		if (trajLayer != null) {
			trajLayer.removePropertyChangeListener(this);
		}
		if (simpleTrajLayer != null) {
			simpleTrajLayer.removePropertyChangeListener(this);
		}
		super.destroy();
	}
}
