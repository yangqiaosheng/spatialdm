package spade.analysis.vis3d;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.beans.PropertyChangeEvent;
import java.util.NoSuchElementException;

import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DLayerManager;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapContext;
import spade.vis.mapvis.Visualizer;

public class LayerManager3D extends DLayerManager {
	public final Color projectionLinesColor = Color.gray;
	public final Color frameColor = Color.white;
	public final Color focussingColor = Color.magenta;

	protected DGeoLayer origLayer = null;
	protected DGeoLayerZ specLayer = null;
	protected boolean drawProjectionLines = false, drawBoundingFrame = true, usePointsInsteadOfAreas = true;

	public DGeoLayer getSpecialLayer() {
		return specLayer;
	}

	public DGeoLayer getOriginalLayer() {
		return origLayer;
	}

	public void setSpecialLayer(DGeoLayerZ layer, DGeoLayer originalLayer) {
		specLayer = layer;
		if (specLayer != null) {
			super.addGeoLayer(specLayer);
		}
		origLayer = originalLayer;
		if (origLayer != null) {
			origLayer.addPropertyChangeListener(this);
		}
	}

	public boolean areProjectionLinesDrawn() {
		return drawProjectionLines;
	}

	public boolean isBoundingFrameDrawn() {
		return drawBoundingFrame;
	}

	public void setDrawProjectionLines(boolean value) {
		if (drawProjectionLines != value) {
			drawProjectionLines = !drawProjectionLines;
			notifyPropertyChange("content", null, null, true, false);
		}
	}

	public void setDrawBoundingFrame(boolean value) {
		if (drawBoundingFrame != value) {
			drawBoundingFrame = !drawBoundingFrame;
			notifyPropertyChange("content", null, null, true, false);
		}
	}

	public void setUsePointsInsteadOfAreas(boolean value) {
		if (usePointsInsteadOfAreas != value) {
			usePointsInsteadOfAreas = !usePointsInsteadOfAreas;
			notifyPropertyChange("content", null, null, true, false);
		}
	}

	@Override
	public void drawBackground(Graphics g, MapContext mc) {
		draw(g, mc);
		//System.out.println("LayerManager3D :: drawBackground()");
	}

	@Override
	public void drawForeground(Graphics g, MapContext mc) {
		MapMetrics3D mm3d = null;
		if (mc instanceof MapMetrics3D) {
			mm3d = (MapMetrics3D) mc;
		}

		double minZ = mm3d.getMinZ(), maxZ = mm3d.getMaxZ(), Z0 = mm3d.getZ0(), absMinZ = mm3d.getAbsMinZ(), absMaxZ = mm3d.getAbsMaxZ();
		drawVisibleObjects(g, mm3d);
		// draw the closest parts of the bounding frame
		g.setColor(frameColor);
		if (drawBoundingFrame) {
			drawLastBone(g, mm3d, absMinZ, absMaxZ);
		}
		if (drawBoundingFrame && mm3d.isFocussingActive()) {
			g.setColor(focussingColor);
			drawLastBone(g, mm3d, minZ, maxZ);
		}
	}

	private void drawMapReference(Graphics g, MapContext mc) {
		MapMetrics3D mm3d = null;
		if (mc instanceof MapMetrics3D) {
			mm3d = (MapMetrics3D) mc;
		}
		if (mm3d != null) {
			RealRectangle rr = mm3d.getVisibleTerritory();
			//RealRectangle rr=specLayer.getCurrentLayerBounds();

			Point p1 = mm3d.get3DTransform(rr.rx1, rr.ry1, mm3d.getZ0()), p2 = mm3d.get3DTransform(rr.rx1, rr.ry2, mm3d.getZ0()), p3 = mm3d.get3DTransform(rr.rx2, rr.ry2, mm3d.getZ0()), p4 = mm3d.get3DTransform(rr.rx2, rr.ry1, mm3d.getZ0());
			int xp[] = { p1.x, p2.x, p3.x, p4.x }, yp[] = { p1.y, p2.y, p3.y, p4.y };
			g.setColor(Color.lightGray);
			g.fillPolygon(xp, yp, 4);
			g.setColor(Color.gray);
			g.drawPolygon(xp, yp, 4);
		}
		// fill the reference map
		mustDrawDiagrams = false;
		mustDrawLabels = false;

		for (int i = 0; i < getLayerCount(); i++) {
			DGeoLayer gl = getLayer(i);
			if (gl == null || !gl.getLayerDrawn() || gl.equals(specLayer)) {
				continue;
			}
			mustDrawDiagrams = mustDrawDiagrams || gl.getHasDiagrams();
			mustDrawLabels = mustDrawLabels || gl.getDrawingParameters().drawLabels;
			if (gl.getHasDiagrams() && gl.getType() == Geometry.point) {
				continue;
			}
			gl.draw(g, mc);
			//gl.drawStrictly(g,mc);
		}
		super.drawForeground(g, mc);
	}

	public void draw_new(Graphics g, MapContext mc) {
		DrawingParameters dparm = specLayer.getDrawingParameters(), dparm1 = dparm.makeCopy();
		dparm1.drawLabels = false;
		specLayer.removePropertyChangeListener(this);
		specLayer.setDrawingParameters(dparm1);
		MapMetrics3D mm3d = null;
		if (mc instanceof MapMetrics3D) {
			mm3d = (MapMetrics3D) mc;
		}

		if (mm3d != null) {
			double minZ = mm3d.getMinZ(), maxZ = mm3d.getMaxZ(), Z0 = mm3d.getZ0(), absMinZ = mm3d.getAbsMinZ(), absMaxZ = mm3d.getAbsMaxZ();

			if (Z0 < mm3d.getViewpointZ()) {
				// refrence map is below the viewpoint altitude
				if (Z0 < absMinZ) {
					;
				} else {
					if (drawBoundingFrame) {
						if (mm3d.isFocussingActive()) {
							drawFrame(g, mm3d, absMinZ, (Z0 < minZ) ? Z0 : minZ, 1);
							if (Z0 > minZ) {
								drawFrame(g, mm3d, minZ, (Z0 < maxZ) ? Z0 : maxZ, 0);
							}
							if (Z0 > maxZ) {
								drawFrame(g, mm3d, maxZ, Z0, 0);
							}
						} else {
							drawFrame(g, mm3d, minZ, Z0);
						}
					}
				}
			} else // viewpoint is below of reference map
			if (Z0 < absMaxZ) { // reference map is inside 3D-Cube
				if (drawBoundingFrame) {
					if (mm3d.isFocussingActive()) {
						if (Z0 < maxZ) {
							drawFrame(g, mm3d, maxZ, absMaxZ, 0);
							drawFrame(g, mm3d, (Z0 > minZ) ? Z0 : minZ, maxZ, 1);
							if (Z0 < minZ) {
								drawFrame(g, mm3d, Z0, minZ, 1);
							}
						} else {
							drawFrame(g, mm3d, Z0, absMaxZ);
						}
					} else {
						drawFrame(g, mm3d, Z0, maxZ);
					}
				}
			}
		}
		drawMapReference(g, mc);
		// draw all stuff after reference frame
		if (mm3d != null) {
			double minZ = mm3d.getMinZ(), maxZ = mm3d.getMaxZ(), Z0 = mm3d.getZ0(), absMinZ = mm3d.getAbsMinZ(), absMaxZ = mm3d.getAbsMaxZ();
			if (Z0 < mm3d.getViewpointZ())
				// reference map is below of viewpoint's level
				if (Z0 > absMaxZ) {
					;
				} else {
					if (drawBoundingFrame)
						if (mm3d.isFocussingActive()) {
							if (Z0 < minZ) {
								drawFrame(g, mm3d, Z0, minZ, 1);
							}
							if (Z0 < maxZ) {
								if (Z0 > minZ) {
									drawFrame(g, mm3d, Z0, maxZ, 1);
								} else {
									drawFrame(g, mm3d, minZ, maxZ, 2);
								}
								drawFrame(g, mm3d, maxZ, absMaxZ, 0);
							} else {
								drawFrame(g, mm3d, Z0, absMaxZ);
							}
						} else {
							drawFrame(g, mm3d, Z0, maxZ);
						}
				}
			else // viewpoint's level is below than reference map
			if (Z0 > absMinZ) {
				if (drawBoundingFrame) {
					if (mm3d.isFocussingActive()) {
						if (Z0 > minZ) {
							drawFrame(g, mm3d, (Z0 <= maxZ) ? minZ : maxZ, Z0, 0);
							if (Z0 > maxZ) {
								drawFrame(g, mm3d, minZ, maxZ, 2);
							}
							drawFrame(g, mm3d, absMinZ, minZ, 1);
						} else {
							drawFrame(g, mm3d, absMinZ, Z0);
						}
					} else {
						drawFrame(g, mm3d, minZ, Z0);
					}
				}
			}
			// draw the closest parts of the bounding frame
			g.setColor(frameColor);
			if (drawBoundingFrame) {
				drawLastBone(g, mm3d, absMinZ, absMaxZ);
			}
			if (drawBoundingFrame && mm3d.isFocussingActive()) {
				g.setColor(focussingColor);
				drawLastBone(g, mm3d, minZ, maxZ);
			}
			//drawSpecLayer(g,mm3d,absMinZ,absMaxZ);
			drawVisibleObjects(g, mm3d);
		}
		specLayer.setDrawingParameters(dparm);
		specLayer.addPropertyChangeListener(this);
		//notifyPropertyChange("background_drawn",null,null,true,false);
		if (firstDraw) {
			notifyPropertyChange("LayersLoaded", null, null, false, true);
			firstDraw = false;
		}
		//System.out.println("LMan3D:draw()");
	}

	public void draw(Graphics g, MapContext mc) {
		if (specLayer == null)
			return;
		DrawingParameters dparm = specLayer.getDrawingParameters(), dparm1 = dparm.makeCopy();
		dparm1.drawLabels = false;
		specLayer.removePropertyChangeListener(this);
		specLayer.setDrawingParameters(dparm1);
		MapMetrics3D mm3d = null;
		if (mc instanceof MapMetrics3D) {
			mm3d = (MapMetrics3D) mc;
		}

		if (mm3d != null) {
			double minZ = mm3d.getMinZ(), maxZ = mm3d.getMaxZ(), Z0 = mm3d.getZ0(), absMinZ = mm3d.getAbsMinZ(), absMaxZ = mm3d.getAbsMaxZ();

			if (Z0 < mm3d.getViewpointZ()) {
				// refrence map is below the viewpoint altitude
				if (Z0 < absMinZ) {
					;
				} else {
					if (drawBoundingFrame) {
						if (mm3d.isFocussingActive()) {
							drawFrame(g, mm3d, absMinZ, (Z0 < minZ) ? Z0 : minZ, 1);
							if (Z0 > minZ) {
								drawFrame(g, mm3d, minZ, (Z0 < maxZ) ? Z0 : maxZ, 0);
							}
							if (Z0 > maxZ) {
								drawFrame(g, mm3d, maxZ, Z0, 0);
							}
						} else {
							drawFrame(g, mm3d, minZ, Z0);
						}
					}
					drawSpecLayer(g, mm3d, minZ, (Z0 < maxZ) ? Z0 : maxZ);
				}
			} else // viewpoint is below of reference map
			if (Z0 < absMaxZ) { // reference map is inside 3D-Cube
				if (drawBoundingFrame) {
					if (mm3d.isFocussingActive()) {
						if (Z0 < maxZ) {
							drawFrame(g, mm3d, maxZ, absMaxZ, 0);
							drawFrame(g, mm3d, (Z0 > minZ) ? Z0 : minZ, maxZ, 1);
							if (Z0 < minZ) {
								drawFrame(g, mm3d, Z0, minZ, 1);
							}
						} else {
							drawFrame(g, mm3d, Z0, absMaxZ);
						}
					} else {
						drawFrame(g, mm3d, Z0, maxZ);
					}
				}
				drawSpecLayer(g, mm3d, (Z0 < minZ) ? minZ : Z0, maxZ);
			}
		}
		drawMapReference(g, mc);
		// draw all stuff after reference frame
		if (mm3d != null) {
			Double minZ = mm3d.getMinZ(), maxZ = mm3d.getMaxZ(), Z0 = mm3d.getZ0(), absMinZ = mm3d.getAbsMinZ(), absMaxZ = mm3d.getAbsMaxZ();
			if (Z0 < mm3d.getViewpointZ())
				// reference map is below of viewpoint's level
				if (Z0 > absMaxZ) {
					;
				} else {
					if (drawBoundingFrame)
						if (mm3d.isFocussingActive()) {
							if (Z0 < minZ) {
								drawFrame(g, mm3d, Z0, minZ, 1);
							}
							if (Z0 < maxZ) {
								if (Z0 > minZ) {
									drawFrame(g, mm3d, Z0, maxZ, 1);
								} else {
									drawFrame(g, mm3d, minZ, maxZ, 2);
								}
								drawFrame(g, mm3d, maxZ, absMaxZ, 0);
							} else {
								drawFrame(g, mm3d, Z0, absMaxZ);
							}
						} else {
							drawFrame(g, mm3d, Z0, maxZ);
						}
					drawSpecLayer(g, mm3d, (Z0 < minZ) ? minZ : Z0, maxZ);
				}
			else // viewpoint's level is below than reference map
			if (Z0 > absMinZ) {
				drawSpecLayer(g, mm3d, minZ, (Z0 < maxZ) ? Z0 : maxZ);
				if (drawBoundingFrame) {
					if (mm3d.isFocussingActive()) {
						if (Z0 > minZ) {
							drawFrame(g, mm3d, (Z0 <= maxZ) ? minZ : maxZ, Z0, 0);
							if (Z0 > maxZ) {
								drawFrame(g, mm3d, minZ, maxZ, 2);
							}
							drawFrame(g, mm3d, absMinZ, minZ, 1);
						} else {
							drawFrame(g, mm3d, absMinZ, Z0);
						}
					} else {
						drawFrame(g, mm3d, minZ, Z0);
					}
				}
			}
			drawVisibleObjects(g, mm3d);
			// draw the closest parts of the bounding frame
			g.setColor(frameColor);
			if (drawBoundingFrame) {
				drawLastBone(g, mm3d, absMinZ, absMaxZ);
			}
			if (drawBoundingFrame && mm3d.isFocussingActive()) {
				g.setColor(focussingColor);
				drawLastBone(g, mm3d, minZ, maxZ);
			}
		}
		specLayer.setDrawingParameters(dparm);
		specLayer.addPropertyChangeListener(this);
		//notifyPropertyChange("background_drawn",null,null,true,false);
		if (firstDraw) {
			notifyPropertyChange("LayersLoaded", null, null, false, true);
			firstDraw = false;
		}
		//System.out.println("LMan3D:draw()");
	}

	protected void drawSpecLayer_old(Graphics g, MapMetrics3D mm3d, float lowZ, float upZ) {
		if (specLayer == null || mm3d == null)
			return;
		DrawingParameters dparm = specLayer.getDrawingParameters();
		Visualizer vis = specLayer.getVisualizer();
		specLayer.setUsePointGeometry(usePointsInsteadOfAreas);
		specLayer.updateBounds(mm3d);
		RealRectangle visTerr = mm3d.getVisibleTerritory();

		boolean isInsideZoom;

		try {
			while (specLayer.hasMoreObjects()) {
				DGeoObjectZ zobj = specLayer.getNext(mm3d);
				RealPoint rpObjCenter = zobj.getCenter();
				isInsideZoom = visTerr.contains(rpObjCenter.x, rpObjCenter.y, 0.0f) && zobj.fitsInRectangle(visTerr.rx1, visTerr.ry1, visTerr.rx2, visTerr.ry2);
				if (!specLayer.canObjectBeDrawn(zobj, lowZ, upZ) || !isInsideZoom) {
					continue;
				}
				if (drawProjectionLines) {
					if (rpObjCenter == null) {
						continue;
					}
					float x1 = rpObjCenter.x, y1 = rpObjCenter.y;
					Point p1 = mm3d.get3DTransform(x1, y1, zobj.getZPosition()), p2 = mm3d.get3DTransform(x1, y1, mm3d.getZ0());
					g.setColor(projectionLinesColor);
					g.drawLine(p1.x, p1.y, p2.x, p2.y);
				}
				zobj.setDrawingParameters(dparm);
				zobj.setVisualizer(vis);
				if (vis != null && vis.isDiagramPresentation()) {
					zobj.drawDiagram(g, mm3d);
				} else {
					zobj.draw(g, mm3d);
				}
				if (zobj.isSelected()) {
					zobj.showSelection(g, mm3d);
				}
				if (zobj.isHighlighted()) {
					zobj.showHighlight(g, mm3d);
				}
			}
		} catch (NoSuchElementException nse) {
		}
	}

	protected void drawSpecLayer(Graphics g, MapMetrics3D mm3d, double lowZ, double upZ) {
		if (specLayer == null || mm3d == null)
			return;
		RealRectangle visTerr = mm3d.getVisibleTerritory();

		boolean isInsideZoom;

		try {
			while (specLayer.hasMoreObjects()) {
				DGeoObjectZ zobj = specLayer.getNext(mm3d);
				RealPoint rpObjCenter = zobj.getCenter();
				isInsideZoom = visTerr.contains(rpObjCenter.x, rpObjCenter.y, 0.0f) && zobj.fitsInRectangle(visTerr.rx1, visTerr.ry1, visTerr.rx2, visTerr.ry2);
				if (!specLayer.canObjectBeDrawn(zobj, lowZ, upZ) || !isInsideZoom) {
					continue;
				}
				if (drawProjectionLines) {
					if (rpObjCenter == null) {
						continue;
					}
					float x1 = rpObjCenter.x, y1 = rpObjCenter.y;
					Point p1 = mm3d.get3DTransform(x1, y1, zobj.getZPosition()), p2 = mm3d.get3DTransform(x1, y1, mm3d.getZ0());
					g.setColor(projectionLinesColor);
					g.drawLine(p1.x, p1.y, p2.x, p2.y);
				}
			}
		} catch (NoSuchElementException nse) {
		}
	}

	protected void drawVisibleObjects(Graphics g, MapMetrics3D mm3d) {
		if (specLayer == null || mm3d == null)
			return;
		//System.out.println("LayerManager3D:: drawVisibleObjects()");

		DrawingParameters dparm = specLayer.getDrawingParameters();
		Visualizer vis = specLayer.getVisualizer();
		specLayer.setUsePointGeometry(usePointsInsteadOfAreas);
		specLayer.updateBounds(mm3d);
		RealRectangle visTerr = mm3d.getVisibleTerritory();

		try {
			while (specLayer.hasMoreObjects()) {
				DGeoObjectZ zobj = specLayer.getNext(mm3d);
				if (!specLayer.isObjectVisible(zobj, mm3d)) {
					continue;
				}
				zobj.setDrawingParameters(dparm);
				zobj.setVisualizer(vis);
				if (vis != null && vis.isDiagramPresentation()) {
					zobj.drawDiagram(g, mm3d);
				} else {
					zobj.draw(g, mm3d);
				}
				if (zobj.isSelected()) {
					zobj.showSelection(g, mm3d);
				}
				if (zobj.isHighlighted()) {
					zobj.showHighlight(g, mm3d);
				}
			}
		} catch (NoSuchElementException nse) {
		}
	}

	@Override
	public void drawMarkedObjects(Graphics g, MapContext mc) {
		if (specLayer == null || mc == null)
			return;
		MapMetrics3D mm3d = null;
		if (mc instanceof MapMetrics3D) {
			mm3d = (MapMetrics3D) mc;
		}
		if (mm3d == null)
			return;
		//drawVisibleObjects(g,mm3d);
		specLayer.drawSelectedObjects(g, mc);
		specLayer.showHighlighting(g, mc);
	}

	/**
	*  This funcion needed to draw bounding frame
	*  To use also for depicting focussing range, specify which flat will be used:
	*   focus = { 0 - lower, 1 - upper, 2 - both, (-1) - feature not used
	*/
	private void drawFrame(Graphics g, MapMetrics3D mm3d, double fromZ, double toZ) {
		drawFrame(g, mm3d, fromZ, toZ, -1);
	}

	private void drawFrame(Graphics g, MapMetrics3D mm3d, double fromZ, double toZ, int focus) {
		if (fromZ == toZ)
			return;
		RealRectangle visTerr = mm3d.getVisibleTerritory();
		//RealRectangle visTerr=specLayer.getCurrentLayerBounds();
		if (fromZ > toZ) {
			double tmp = fromZ;
			fromZ = toZ;
			toZ = fromZ;
		}
		double x[] = { visTerr.rx1, visTerr.rx1, visTerr.rx2, visTerr.rx2 }, y[] = { visTerr.ry1, visTerr.ry2, visTerr.ry2, visTerr.ry1 }, z[] = { fromZ, toZ };
		g.setColor(frameColor);
		for (int i = 0; i < x.length; i++) {
			Point p1 = mm3d.get3DTransform(x[i], y[i], z[0]), p2 = mm3d.get3DTransform(x[i], y[i], z[1]);
			g.drawLine(p1.x, p1.y, p2.x, p2.y);
		}
		int xp[] = new int[x.length];
		int yp[] = new int[x.length];
		for (int i = 0; i < 2 * x.length; i++) {
			Point p = mm3d.get3DTransform(x[i % x.length], y[i % x.length], z[i / x.length]);
			xp[i % x.length] = p.x;
			yp[i % x.length] = p.y;
			Color cOrig = g.getColor();
			if ((i + 1) % x.length == 0) {
				if (fromZ != mm3d.getZ0() && i / x.length == 0) {
					g.setColor((focus == 0 || focus == 2) ? focussingColor : frameColor);
					g.drawPolygon(xp, yp, x.length);
				}
				if (i / x.length == 1 && toZ != mm3d.getZ0()) {
					g.setColor((focus == 1 || focus == 2) ? focussingColor : frameColor);
					g.drawPolygon(xp, yp, x.length);
				}
			}
			if (cOrig != g.getColor()) {
				g.setColor(cOrig);
			}
		}
	}

	/**
	*  This function applies some heuristic rules to find which bones of the
	*  bounding frame are needed to be redrawn
	*/
	private void drawLastBone(Graphics g, MapMetrics3D mm3d, double bottomZ, double topZ) {
		RealRectangle rr = mm3d.getVisibleTerritory();
		//RealRectangle rr=specLayer.getCurrentLayerBounds();
		RealPoint vp = mm3d.getViewpointXY();
		double vpZ = mm3d.getViewpointZ();
		// draw last bone when needed only
		RealPoint bone = new RealPoint();
		RealPoint bone1 = new RealPoint();
		RealPoint bone2 = new RealPoint();
		if (vp.x >= rr.rx1 && vp.x <= rr.rx2 && vp.y >= rr.ry1 && vp.y <= rr.ry2)
			return;

		// N,S
		if (vp.x >= rr.rx1 && vp.x <= rr.rx2) {
			bone1.x = rr.rx1;
			bone2.x = rr.rx2;
			if (vp.y <= rr.ry1) {
				bone1.y = rr.ry1;
				bone2.y = rr.ry1;
			} else if (vp.y >= rr.ry2) {
				bone1.y = rr.ry2;
				bone2.y = rr.ry2;
			}
			;
		} else
		// W,E
		if (vp.y >= rr.ry1 && vp.y <= rr.ry2) {
			bone1.y = rr.ry1;
			bone2.y = rr.ry2;
			if (vp.x <= rr.rx1) {
				bone1.x = rr.rx1;
				bone2.x = rr.rx1;
			} else if (vp.x >= rr.rx2) {
				bone1.x = rr.rx2;
				bone2.x = rr.rx2;
			}
		} else
		// N-W
		if (vp.x < rr.rx1 && vp.y < rr.ry1) {
			bone.x = rr.rx1;
			bone.y = rr.ry1;
			bone1.x = rr.rx1;
			bone1.y = rr.ry2;
			bone2.x = rr.rx2;
			bone2.y = rr.ry1;
		} else
		// N-E
		if (vp.x > rr.rx2 && vp.y < rr.ry1) {
			bone.x = rr.rx2;
			bone.y = rr.ry1;
			bone1.x = rr.rx1;
			bone1.y = rr.ry1;
			bone2.x = rr.rx2;
			bone2.y = rr.ry2;
		} else
		// S-E
		if (vp.x > rr.rx2 && vp.y > rr.ry2) {
			bone.x = rr.rx2;
			bone.y = rr.ry2;
			bone1.x = rr.rx1;
			bone1.y = rr.ry2;
			bone2.x = rr.rx2;
			bone2.y = rr.ry1;
		} else
		// S-W
		if (vp.x < rr.rx1 && vp.y > rr.ry2) {
			bone.x = rr.rx1;
			bone.y = rr.ry2;
			bone1.x = rr.rx1;
			bone1.y = rr.ry1;
			bone2.x = rr.rx2;
			bone2.y = rr.ry2;
		}

		Point p1 = mm3d.get3DTransform(bone.x, bone.y, bottomZ), p2 = mm3d.get3DTransform(bone.x, bone.y, topZ), p3 = null, p4 = null;
		if (vpZ > mm3d.getMaxZ()) {
			p3 = mm3d.get3DTransform(bone1.x, bone1.y, topZ);
			p4 = mm3d.get3DTransform(bone2.x, bone2.y, topZ);
		} else if (vpZ < mm3d.getMinZ()) {
			p2 = mm3d.get3DTransform(bone.x, bone.y, bottomZ);
			p3 = mm3d.get3DTransform(bone1.x, bone1.y, bottomZ);
			p4 = mm3d.get3DTransform(bone2.x, bone2.y, bottomZ);
		}
		// if only one horisontal bone needed to be redrawn
		if (((vp.x >= rr.rx1 && vp.x <= rr.rx2) || (vp.y >= rr.ry1 && vp.y <= rr.ry2)) && !((vp.x >= rr.rx1 && vp.x <= rr.rx2) && (vp.y >= rr.ry1 && vp.y <= rr.ry2))) {
			if (p3 != null && p4 != null) {
				g.drawLine(p3.x, p3.y, p4.x, p4.y);
			}
			return;
		}
		// normal case
		Color cOrig = g.getColor();
		g.setColor(frameColor);
		if (!(vp.x >= rr.rx1 && vp.x <= rr.rx2 && vp.y >= rr.ry1 && vp.y <= rr.ry2)) {
			g.drawLine(p1.x, p1.y, p2.x, p2.y);
		}
		g.setColor(cOrig);
		if ((vpZ > mm3d.getMaxZ() && mm3d.getZ0() != topZ) || (vpZ < mm3d.getMinZ() && mm3d.getZ0() != bottomZ)) {
			g.drawLine(p3.x, p3.y, p2.x, p2.y);
			g.drawLine(p4.x, p4.y, p2.x, p2.y);
		}
	}

	/**
	* This function is fired when properties of some of the GeoLayers change.
	* The LayerManager3D checks if the event came from the original of the special
	* layer. If in this original layer the visualization changed, this
	* visualization is also given to the special layer (that is shown in 3D).
	* If the event came from another source, the propertyChange function of
	* the superclass is used.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String prName = evt.getPropertyName();
		//System.out.println("LayerManager3D :: received property change event = "+prName);
		if (evt.getSource().equals(origLayer)) {
			if (prName.equals("Visualization") && origLayer.getThematicData() != null && origLayer.getThematicData().equals(specLayer.getThematicData())) {
				specLayer.setVisualizer(origLayer.getVisualizer());
			}
		} else {
			super.propertyChange(evt);
		}
	}
}
