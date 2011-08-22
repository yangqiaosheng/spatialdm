package spade.vis.mapvis;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.MemoryImageSource;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;

import spade.analysis.manipulation.GridManipulator;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.OKDialog;
import spade.lib.color.BinaryColorScale;
import spade.lib.color.ColorScale;
import spade.lib.color.ColorScaleManipulator;
import spade.lib.color.DivergingColorScale;
import spade.lib.color.GrayColorScale;
import spade.lib.lang.Language;
import spade.vis.database.DataItem;
import spade.vis.database.SpatialDataItem;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.LocatedImage;
import spade.vis.geometry.RasterGeometry;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapContext;

/**
* Visualizes grid data by creating an image, in which the values from ther grid
* are encoded by colors. Can apply various colour scales: diverging, spectral,
* grey etc.
*/
public class GridVisImplement extends BaseVisualizer implements PropertyChangeListener, GridVisualizer {
	static ResourceBundle res = Language.getTextResource("spade.vis.mapvis.Res");

	protected static final String csNamePrefix = "spade.lib.color.";
	protected static final String csNamePostfix = "ColorScale";
	/**
	* The minimum and maximum values to be encoded
	*/
	protected float minV = Float.NaN, maxV = Float.NaN;
	/**
	* The color scale used to encode grid data
	*/
	protected ColorScale cs = null;

	/**
	* The values of isolines
	*/
	protected float[] isolines = null;

	/**
	* Indicates, whether rasters are interpolated when drawn
	*/
	protected boolean smooth = true;
	/**
	* Is set in true, when raster is not drawn behind isolines
	*/
	protected boolean isolinesOnly = false;

	protected DrawingParameters drawParm = null;
	/**
	* Grid visualizaed
	*/
	protected RasterGeometry rgeom = null; // initializes after first paint
//ID
	protected float interpolated[];
	protected LocatedImage locImg;

	protected RealRectangle rasterExtent = new RealRectangle();
	protected RealRectangle terrExtent = new RealRectangle();

	protected boolean extentChanged(float x1, float y1, float x2, float y2, float tx1, float ty1, float tx2, float ty2) {
		return !(x1 == rasterExtent.rx1 && y1 == rasterExtent.ry1 && x2 == rasterExtent.rx2 && y2 == rasterExtent.ry2 && tx1 == terrExtent.rx1 && ty1 == terrExtent.ry1 && tx2 == terrExtent.rx2 && ty2 == terrExtent.ry2);
	}

//~ID
	/**
	* Sets the minimum and maximum values to be encoded
	*/
	@Override
	public void setMinMax(float min, float max) {
		minV = min;
		maxV = max;
		if (cs != null) {
			cs.setMinMax(min, max);
		}
	}

	@Override
	public void setDrawingParameters(DrawingParameters dp) {
		drawParm = dp;
		cs = null;
		checkMakeColorScale();
		locImg = null;
		notifyVisChange();
	}

	@Override
	public DrawingParameters getDrawingParameters() {
		return drawParm;
	}

	/**
	* Here the Visualizer sets its parameters.
	*/
	@Override
	public void setup() {
	}

	/**
	* If there is no color scale yet, makes a default color scale
	*/
	protected void checkMakeColorScale() {
		if (cs == null) {
			if (drawParm != null && drawParm.colorScale != "") {
				try {
					cs = (ColorScale) Class.forName(csNamePrefix + drawParm.colorScale + csNamePostfix).newInstance();
					cs.setAlpha((100.0f - drawParm.transparency) / 100.0f);
					if (!Float.isNaN(minV) && !Float.isNaN(maxV)) {
						cs.setMinMax(minV, maxV);
					}
					cs.setParameters(drawParm.csParameters);
					if (cs instanceof BinaryColorScale) {
						smooth = false;
					}
				} catch (Exception ex) {
				}
			}
			if (cs == null) {
				if (Float.isNaN(minV) || Float.isNaN(maxV) || maxV <= 0.0 || minV >= 0.0) {
					setColorScale(new GrayColorScale());
//        cs = new GrayColorScale();
				} else {
					setColorScale(new DivergingColorScale());
				}
//        cs = new DivergingColorScale();
				if (!Float.isNaN(minV) && !Float.isNaN(maxV)) {
					cs.setMinMax(minV, maxV);
				}
			}
		}
	}

	/**
	* Sets the color scale to be used to encode grid data
	*/
	@Override
	public void setColorScale(ColorScale cs) {
		this.cs = cs;
		if (cs != null) {
			cs.setMinMax(minV, maxV);
		}
		if (drawParm == null) {
			drawParm = new DrawingParameters();
		}
		drawParm.transparency = Math.round(100.0f - cs.getAlpha() * 100);
		String csName = cs.getClass().getName();
		drawParm.colorScale = (csName.indexOf('.') >= 0) ? csName.substring(csName.lastIndexOf('.') + 1) : csName;
		if (drawParm.colorScale.endsWith(csNamePostfix)) {
			drawParm.colorScale = drawParm.colorScale.substring(0, drawParm.colorScale.length() - csNamePostfix.length());
		}
		drawParm.csParameters = cs.getParameters();
		notifyVisChange();
	}

	/**
	* This method is called by a GeoObject. The GeoObject sends its DataItem
	* to the Visualizer, and the Visualizer returns an Object specifying the
	* presentation of data contained in this DataItem. This may be, in
	* particular, an Image. Typically the argument DataItem is
	* a ThematicDataItem, but in this case a SpatialDataItem is required.
	*/
	@Override
	public Object getPresentation(DataItem dit, MapContext mc) {
		if (dit == null || mc == null || !(dit instanceof SpatialDataItem))
			return null;
		SpatialDataItem spd = (SpatialDataItem) dit;
		Geometry g = spd.getGeometry();
		if (g == null || !(g instanceof RasterGeometry))
			return null;
		rgeom = (RasterGeometry) g;
		if (rgeom.ras == null)
			return null;
		if (Float.isNaN(minV) || Float.isNaN(maxV)) {
			setMinMax(rgeom.minV, rgeom.maxV);
		}

		RealRectangle terr = mc.getVisibleTerritory();
		if (terr == null)
			return null;
		float wx1 = (rgeom.rx1 < terr.rx1) ? terr.rx1 : rgeom.rx1, wx2 = (rgeom.rx2 > terr.rx2) ? terr.rx2 : rgeom.rx2, wy1 = (rgeom.ry1 < terr.ry1) ? terr.ry1 : rgeom.ry1, wy2 = (rgeom.ry2 > terr.ry2) ? terr.ry2 : rgeom.ry2;

		if (extentChanged(wx1, wy1, wx2, wy2, terr.rx1, terr.ry1, terr.rx2, terr.ry2)) {
			interpolated = null;
			locImg = null;
			rasterExtent = new RealRectangle(wx1, wy1, wx2, wy2);
			terrExtent = new RealRectangle(terr.rx1, terr.ry1, terr.rx2, terr.ry2);
		}

		int x1 = mc.scrX(wx1, wy1), y1 = mc.scrY(wx1, wy1), x2 = mc.scrX(wx2, wy2), y2 = mc.scrY(wx2, wy2);

		if (interpolated == null) {
			checkMakeColorScale();

			interpolated = new float[(x2 - x1 + 1) * (y1 - y2 + 1)];

			int curP = 0, curV = 0;
			double xf = 0, xf1 = 0, yf = 0, yf1 = 0;
			int x0 = 0, y0 = 0, x01 = 0, y01 = 0;
			float avg = 0, avg1 = 0, nbr = 0, delta = 0, delta1 = 0;

			double wy = wy2, stepY = (wy2 - wy1) / (y1 - y2), stepX = (wx2 - wx1) / (x2 - x1);

//  interpolated = rgeom.getInterpolatedMatrix(wx1, wy1, wx2, wy2, (x2-x1+1), (y1-y2+1));

			if ((stepY < rgeom.DY || stepX < rgeom.DX) && smooth) {
				// Renderer with linear interpolation
				for (int yy = y2; yy <= y1; yy++) {
					double t = (wy - rgeom.Ybeg) / rgeom.DY;
					wy -= stepY;
					y0 = (int) Math.floor(t);
					y01 = y0 + 1;
					yf = t - y0;
					yf1 = y0 + 1 - t;

					double wx = wx1;
					for (int xx = x1; xx <= x2; xx++) {
						t = (wx - rgeom.Xbeg) / rgeom.DX;
						wx += stepX;
						x0 = (int) Math.floor(t);
						x01 = x0 + 1;
						xf = t - x0;
						xf1 = x0 + 1 - t;

						try {
							avg = (float) (rgeom.ras[x0][y0] * xf1 * yf1 + rgeom.ras[x01][y0] * xf * yf1 + rgeom.ras[x0][y01] * xf1 * yf + rgeom.ras[x01][y01] * xf * yf);
						} catch (ArrayIndexOutOfBoundsException e) {
							if (x0 + 1 >= rgeom.Col || x0 < 0) {
								x0 = x01 = (int) Math.round(t);
							}
							if (y0 + 1 >= rgeom.Row || y0 < 0) {
								y0 = y01 = (int) Math.round((wy - rgeom.Ybeg) / rgeom.DY);
							}
							try {
								avg = (float) (rgeom.ras[x0][y0] * xf1 * yf1 + rgeom.ras[x01][y0] * xf * yf1 + rgeom.ras[x0][y01] * xf1 * yf + rgeom.ras[x01][y01] * xf * yf);
							} catch (Exception ex) {
								interpolated[curP] = Float.NaN;
								curP++;
								continue;
							}
						}

						// trying to get values from nowhere
						if (Float.isNaN(avg)) {
							float v0_0 = rgeom.ras[x0][y0];
							float v01_0 = rgeom.ras[x01][y0];
							float v0_01 = rgeom.ras[x0][y01];
							float v01_01 = rgeom.ras[x01][y01];
							float average = 0;
							int valuable = 0;
							float mNaN = 0;

							if (!Float.isNaN(v0_0)) {
								average += v0_0;
								valuable++;
							} else {
								mNaN += xf1 * yf1;
							}
							if (!Float.isNaN(v01_0)) {
								average += v01_0;
								valuable++;
							} else {
								mNaN += xf * yf1;
							}
							if (!Float.isNaN(v0_01)) {
								average += v0_01;
								valuable++;
							} else {
								mNaN += xf1 * yf;
							}
							if (!Float.isNaN(v01_01)) {
								average += v01_01;
								valuable++;
							} else {
								mNaN += xf * yf;
							}

							if (valuable > 0 && mNaN < 0.5) {
								average /= valuable;
								if (Float.isNaN(v0_0)) {
									v0_0 = average;
								}
								if (Float.isNaN(v01_0)) {
									v01_0 = average;
								}
								if (Float.isNaN(v0_01)) {
									v0_01 = average;
								}
								if (Float.isNaN(v01_01)) {
									v01_01 = average;
								}

								avg = (float) (v0_0 * xf1 * yf1 + v01_0 * xf * yf1 + v0_01 * xf1 * yf + v01_01 * xf * yf);
							}
/*              float mNaN=0, rest=0, vras=0;
              double mult=0;

              vras = rgeom.ras[x0][y0];
              mult = xf1*yf1;
              if (Float.isNaN(vras))
                mNaN += mult;
              else
                rest += vras*mult;

              vras = rgeom.ras[x01][y0];
              mult = xf*yf1;
              if (Float.isNaN(vras))
                mNaN += mult;
              else
                rest += vras*mult;

              vras = rgeom.ras[x0][y01];
              mult = xf1*yf;
              if (Float.isNaN(vras))
                mNaN += mult;
              else
                rest += vras*mult;

              vras = rgeom.ras[x01][y01];
              mult = xf*yf;
              if (Float.isNaN(vras))
                mNaN += mult;
              else
                rest += vras*mult;

              if (mNaN < 0.5) avg = rest;*/
						}

						if (avg < minV || avg > maxV) {
							avg = Float.NaN;
						}

						if ((!rgeom.Intr || rgeom.isBinary) && !Float.isNaN(avg)) {
							// choosing nearest neighbor
							delta = delta1 = Float.POSITIVE_INFINITY;
							avg1 = Float.NaN;

							nbr = rgeom.ras[x0][y0];
							delta1 = Math.abs(avg - nbr);
							if (delta1 < delta) {
								avg1 = nbr;
								delta = delta1;
							}

							nbr = rgeom.ras[x01][y0];
							delta1 = Math.abs(avg - nbr);
							if (delta1 < delta) {
								avg1 = nbr;
								delta = delta1;
							}

							nbr = rgeom.ras[x0][y01];
							delta1 = Math.abs(avg - nbr);
							if (delta1 < delta) {
								avg1 = nbr;
								delta = delta1;
							}

							nbr = rgeom.ras[x01][y01];
							delta1 = Math.abs(avg - nbr);
							if (delta1 < delta) {
								avg1 = nbr;
								delta = delta1;
							}

							avg = avg1;
						}

						interpolated[curP] = avg;
						curP++;
					}
				}
			} else {
				double gx, gx0 = rgeom.getGridX(wx1);
				double gy = rgeom.getGridY(wy2);
				double grDX = stepX / rgeom.DX;
				double grDY = stepY / rgeom.DY;
				for (int yy = y2; yy <= y1; yy++) {
					gx = gx0;
					for (int xx = x1; xx <= x2; xx++) {
						avg = Float.NaN;
						try {
							avg = rgeom.ras[(int) Math.round(gx)][(int) Math.round(gy)];
						} catch (ArrayIndexOutOfBoundsException e) {
						}
						interpolated[curP] = avg;
						curP++;
						gx += grDX;
					}
					gy -= grDY;
				}
			}
		}
		if (locImg == null) {
			int pixels[] = new int[interpolated.length];

			//isolines
			if (isolines != null && isolines.length > 0) {

				float avg, min, max, neigh;
				int x, y, xc, yc, width = (x2 - x1 + 1), height = (y1 - y2 + 1);
				for (int curP = 0; curP < pixels.length; curP++) {
					min = max = avg = interpolated[curP];

					y = curP / width;
					x = curP % width;

					if (x > 0 && y > 0 && x < width - 1 && y < height - 1) {
						neigh = interpolated[(y - 1) * width + x - 1];
						if (neigh < min) {
							min = neigh;
						}
						if (neigh > max) {
							max = neigh;
						}
						neigh = interpolated[(y - 1) * width + x];
						if (neigh < min) {
							min = neigh;
						}
						if (neigh > max) {
							max = neigh;
						}
						neigh = interpolated[(y - 1) * width + x + 1];
						if (neigh < min) {
							min = neigh;
						}
						if (neigh > max) {
							max = neigh;
						}
						neigh = interpolated[(y) * width + x - 1];
						if (neigh < min) {
							min = neigh;
						}
						if (neigh > max) {
							max = neigh;
						}
						neigh = interpolated[(y) * width + x];
						if (neigh < min) {
							min = neigh;
						}
						if (neigh > max) {
							max = neigh;
						}
						neigh = interpolated[(y) * width + x + 1];
						if (neigh < min) {
							min = neigh;
						}
						if (neigh > max) {
							max = neigh;
						}
						neigh = interpolated[(y + 1) * width + x - 1];
						if (neigh < min) {
							min = neigh;
						}
						if (neigh > max) {
							max = neigh;
						}
						neigh = interpolated[(y + 1) * width + x];
						if (neigh < min) {
							min = neigh;
						}
						if (neigh > max) {
							max = neigh;
						}
						neigh = interpolated[(y + 1) * width + x + 1];
						if (neigh < min) {
							min = neigh;
						}
						if (neigh > max) {
							max = neigh;
						}
					}

					boolean isIso1 = false;
					boolean isIso2 = false;
					if (!Float.isNaN(avg)) {
						for (float iso : isolines) {
							if (min <= iso && max > iso && avg <= iso) {
								isIso1 = true;
								break;
							}
							if (min <= iso && max > iso && avg > iso) {
								isIso2 = true;
								break;
							}
						}
						pixels[curP] = cs.getPackedColorForValue(avg);
						if (isIso1) {
							pixels[curP] = 0xFF000000;
						} else if (isIso2) {
							pixels[curP] = 0xFFFFFFFF;
						} else if (!isolinesOnly) {
							pixels[curP] = cs.getPackedColorForValue(avg);
						} else {
							pixels[curP] = 0x00000000;
						}
					}

				}

			} else if (!isolinesOnly) {
				float avg;
				for (int curP = 0; curP < pixels.length; curP++) {
					avg = interpolated[curP];
					if (!Float.isNaN(avg)) {
						pixels[curP] = cs.getPackedColorForValue(avg);
					}
				}
			}
			Image img = java.awt.Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(x2 - x1 + 1, y1 - y2 + 1, pixels, 0, x2 - x1 + 1));

			if (img == null)
				return null;
			locImg = new LocatedImage();
			locImg.img = img;
			locImg.x = x1;
			locImg.y = y2;

		}
		return locImg;
	}

	/**
	* This method informs whether the Visualizer produces diagrams.
	* This is important for defining the order of drawing of GeoLayers on the
	* map: the diagrams should be drawn on top of all geography.
	* A GridVisualizer typically produces large images that cannot be ragarded
	* as diagrams.
	*/
	@Override
	public boolean isDiagramPresentation() {
		return false;
	}

	/**
	* Draws the part of the legend explaining this specific presentation method.
	*/
	@Override
	public Rectangle drawMethodSpecificLegend(Graphics g, int startY, int leftmarg, int prefW) {
		if (Float.isNaN(minV) || Float.isNaN(maxV))
			return new Rectangle(leftmarg, startY, 0, 0);
		checkMakeColorScale();
		Rectangle rect = cs.drawLegend(g, startY, leftmarg, prefW);
		if (rgeom != null) {
			FontMetrics fm = g.getFontMetrics();
			int asc = fm.getAscent();
			String str = res.getString("Resolution_") + rgeom.Col + res.getString("columns_x") + rgeom.Row + res.getString("rows");
			//int sw=fm.stringWidth(str);
			g.setColor(Color.black);
			g.drawString(str, leftmarg, startY + rect.height + asc);
			//if (sw>prefW) prefW=sw;
			rect.height += asc;
		}
		return rect;
	}

	/**
	* Draws a small picture symbolizing this presentation technique
	*/
	@Override
	public void drawIcon(Graphics g, int x, int y, int w, int h) {
		if (Float.isNaN(minV) || Float.isNaN(maxV))
			return;
		checkMakeColorScale();
		cs.drawColorBar(g, x, y, w, h);
	}

	/**
	* Must reply whether any parameters of this visualization method may be
	* changed. Returns true: the color scale can be changed.
	*/
	@Override
	public boolean canChangeParameters() {
		return false;
	}

	/**
	* Constructs and displays a dialog for changing parameters of the color scale
	*/
	@Override
	public void startChangeParameters() {
		ColorScaleManipulator csm = new ColorScaleManipulator(cs);
		csm.addPropertyChangeListener(this);
		OKDialog okd = new OKDialog(CManager.getAnyFrame(null), res.getString("Modify_color_scale"), false);
		okd.addContent(csm);
		okd.show();
	}

	/**
	* This function is fired when properties of some of the GeoLayers change.
	* The DLayerManager should notify its listeners that properties have
	* changed (in particular, to fire map redrawing).
	*/
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String prName = evt.getPropertyName();
		if (evt.getSource() instanceof ColorScaleManipulator) {
			locImg = null;
//      if (prName.equals("NewScale")) {
			cs = (ColorScale) evt.getNewValue();
			setColorScale(cs);
			if (man != null) {
				man.setColorScale(cs);
//      } else notifyVisChange();
			}
		}
		if (evt.getSource() instanceof GridManipulator) {
			if (prName.equals("Isolines")) {
				locImg = null;
				isolines = (float[]) evt.getNewValue();
				notifyVisChange();
			} else if (prName.equals("DrawSmooth")) {
				smooth = ((Boolean) evt.getNewValue()).booleanValue();
				interpolated = null;
				locImg = null;
				notifyVisChange();
			} else if (prName.equals("IsolinesOnly")) {
				isolinesOnly = ((Boolean) evt.getNewValue()).booleanValue();
				locImg = null;
				notifyVisChange();
			}

		}
	}

	public Component getColorScaleManipulator() {
		ColorScaleManipulator csm = new ColorScaleManipulator(cs);
		csm.addPropertyChangeListener(this);
		return csm;
	}

//ID
	protected GridManipulator man;

	@Override
	public Component getGridManipulator() {
		man = new GridManipulator(minV, maxV);
		man.addComponents();
		man.setDrawSmooth(smooth);
		man.setIsolinesOnly(isolinesOnly);
		man.setDynamic(cs.getDynamic());
		man.setIsolines(isolines);
		man.setColorScale(cs);
		man.addPropertyChangeListener(this);
		man.add(getColorScaleManipulator());
		return man;
	}
//~ID
}