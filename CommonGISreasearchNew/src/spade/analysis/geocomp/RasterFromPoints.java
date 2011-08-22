package spade.analysis.geocomp;

import java.awt.Choice;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Centimeter;
import spade.lib.basicwin.OKDialog;
import spade.lib.color.GrayColorScale;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.ThematicDataItem;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DGridLayer;
import spade.vis.dmap.DLayerManager;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.MultiGeometry;
import spade.vis.geometry.RasterGeometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapDraw;
import spade.vis.map.MapViewer;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.space.SelectLayer;

/**
* Produces a raster layer from a layer with point objects
*/
public class RasterFromPoints extends GeoCalculator {
	static ResourceBundle res = Language.getTextResource("spade.analysis.geocomp.Res");
	/**
	* The functions used for producing a raster from points
	*/
	public static final String[] functions =
	// following string: "Distance", "Closeness", "Influence", "Number", "Density", "Weighted density"
	{ res.getString("Distance"), res.getString("Closeness"), res.getString("Influence"), res.getString("Number"), res.getString("Density"), res.getString("Weighted_density") };
	protected static final int fDistance = 0, fCloseness = 1, fInfluence = 2, fNumber = 3, fDensity = 4, fWeighted = 5;
	/**
	* Index of the first function in the list requiring presence of thematic data
	*/
	protected static final int firstWithThemData = fWeighted;

	/**
	* Produces a raster layer from a layer with point objects.
	* The arguments are a layer manager (a GeoCalculator must itself care about
	* selection of a layer or layers of appropriate type, in this case a layer
	* with point objects) and SystemUI (to be used for displaying messages and
	* finding an owner frame for dialogs)
	* If calculation was successful, returns the produced raster layer (an instance
	* of DGridLayer).
	*/
	@Override
	public Object doCalculation(LayerManager lman, ESDACore core) {
		SystemUI ui = core.getUI();

		//propose the user to select a layer with point objects
		if (lman == null)
			return null;
		Frame win = null;
		if (ui != null) {
			win = ui.getMainFrame();
		}
		if (win == null) {
			win = CManager.getAnyFrame();
		}
		// following string:"Select a POINT layer"
		GeoLayer layer = SelectLayer.selectLayer(lman, Geometry.point, res.getString("Select_a_POINT_layer"), win);
		if (layer == null)
			return null;

		//propose the user to set the parameters
		Panel p = new Panel(new GridLayout(4, 1));
		// following string:"Specify parameters for computation:"
		p.add(new Label(res.getString("Specify_parameters")));
		Panel pp = new Panel(new FlowLayout(FlowLayout.LEFT));
		// following string:"Function:"
		pp.add(new Label(res.getString("Function_")));
		Choice c = new Choice();
		for (int i = 0; i < firstWithThemData; i++) {
			c.add(functions[i]);
		}
		if (layer.hasThematicData()) {
			for (int i = firstWithThemData; i < functions.length; i++) {
				c.add(functions[i]);
			}
		}
		pp.add(c);
		p.add(pp);
		pp = new Panel(new FlowLayout(FlowLayout.LEFT));
		// following string:"Radius:"
		pp.add(new Label("Radius:"));
		TextField radTF = new TextField("1.0", 5);
		pp.add(radTF);
		p.add(pp);

		pp = new Panel(new FlowLayout(FlowLayout.LEFT));
		pp.add(new Label(res.getString("Scale")));
		Centimeter cm = new Centimeter();
		pp.add(cm);
		float sc = ui.getMapViewer(ui.getCurrentMapN()).getMapDrawer().getMapContext().getPixelValue() * cm.getMinimumSize().width * ((DLayerManager) lman).user_factor;
		pp.add(new Label(StringUtil.floatToStr(sc, 2) + " " + ((DLayerManager) lman).getUserUnit()));
		p.add(pp);

		// following string:"Set parameters"
		OKDialog okd = new OKDialog(win, res.getString("Set_parameters"), true);
		okd.addContent(p);
		okd.show();
		if (okd.wasCancelled())
			return null;
		int func = c.getSelectedIndex();
		float rad = 1.0f;
		String str = radTF.getText();
		if (str != null) {
			try {
				rad = Float.valueOf(str).floatValue();
			} catch (NumberFormatException nfe) {
			}
		}
		if (rad <= 0) {
			rad = 0.1f;
		}

		int attrN = -1; //the attribute used for weighting;
		if (func == fWeighted) {
			AttributeDataPortion table = layer.getThematicData();
			if (table == null || !table.hasData()) {
				// following string:"No thematic data in the layer!"
				if (ui != null) {
					ui.showMessage(res.getString("No_thematic_data_in"), true);
				}
				return null;
			}
			p = new Panel(new GridLayout(3, 1));
			// following string:"Select the attribute for weighting:"
			p.add(new Label(res.getString("Select_the_attribute")));
			c = new Choice();
			Vector aIds = new Vector(table.getAttrCount(), 5);
			for (int i = 0; i < table.getAttrCount(); i++)
				if (table.isAttributeNumeric(i)) {
					aIds.addElement(table.getAttributeId(i));
					c.add(table.getAttributeName(i));
				}
			p.add(c);
			// following string:"Select attribute"
			okd = new OKDialog(win, res.getString("Select_attribute"), true);
			okd.addContent(p);
			okd.show();
			if (okd.wasCancelled())
				return null;
			int idx = c.getSelectedIndex();
			String attrId = (String) aIds.elementAt(idx);
			attrN = table.getAttrIndex(attrId);
		}

		RealRectangle bounds = ((DGeoLayer) layer).getActiveLayerBounds();
		RasterGeometry pnt = new RasterGeometry(bounds.rx1 - rad, bounds.ry1 - rad, bounds.rx2 + rad, bounds.ry2 + rad);
// strange defaults
		pnt.Xbeg = pnt.rx1;
		pnt.Ybeg = pnt.ry1;
		pnt.Col = 200;
		pnt.Row = 200;
		pnt.DX = (pnt.rx2 - pnt.rx1) / pnt.Col;
		pnt.DY = (pnt.ry2 - pnt.ry1) / pnt.Row;
		pnt.Intr = true;
		pnt.Geog = true;

		double step = Math.min(Math.abs((pnt.rx2 - pnt.rx1) / 200), Math.abs((pnt.ry2 - pnt.ry1) / 200));
		if (pnt.DX > 0) {
			pnt.DX = (float) step;
		} else {
			pnt.DX = (float) -step;
		}
		if (pnt.DY > 0) {
			pnt.DY = (float) step;
		} else {
			pnt.DY = (float) -step;
		}
		pnt.Col = Math.round((bounds.rx2 - bounds.rx1 + 2 * rad) / pnt.DX);
		pnt.Row = Math.round((bounds.ry2 - bounds.ry1 + 2 * rad) / pnt.DY);

		if (ui == null || ui.getCurrentMapN() < 0) {
			ui.showMessage(res.getString("No_map_found"), true);
			return null;
		}
		MapViewer mview = ui.getMapViewer(ui.getCurrentMapN());
		if (mview == null) {
			ui.showMessage(res.getString("No_map_found"));
			return null;
		}
		MapDraw map = mview.getMapDrawer();
		if (map == null) {
			ui.showMessage(res.getString("No_map_found"));
			return null;
		}

		ParameterSheet ps = new ParameterSheet(pnt, map, lman);
		// following string:"Set parameters"
		okd = new OKDialog(win, res.getString("Set_parameters"), true);
		okd.addContent(ps);
		okd.show();
		ps.clearHighlighting();
		if (okd.wasCancelled())
			return null;

		try {
			ps.updateGeometry(pnt);
		} catch (Exception ex) {
			// following string:"Invalid formula!"
			if (ui != null) {
				ui.showMessage(res.getString("Invalid_formula_"), true);
			}
			return null;
		}

		pnt.ras = new float[pnt.Col][pnt.Row];
		double RadX = rad / pnt.DX;
		double RadY = rad / pnt.DY;
		double dist, distX, distY;
		int xx, yy, x1, x2, y1, y2, tx1, tx2;
		RealPoint rp;

		MultiGeometry mg = null;
		boolean multi = false;
		int mgparts = 1;

		ThematicDataItem pdata;
		float weight = 0;
		float incr = (float) (1 / (Math.PI * rad * rad));

//clear raster
		switch (func) {
		case fDistance:
			for (yy = 0; yy < pnt.Row; yy++) {
				for (xx = 0; xx < pnt.Col; xx++) {
					pnt.ras[xx][yy] = rad;
				}
			}
			break;
		case fWeighted:
			if (attrN < 0)
				return null;
		case fDensity:
		case fCloseness:
		case fInfluence:
		case fNumber:
			for (yy = 0; yy < pnt.Row; yy++) {
				for (xx = 0; xx < pnt.Col; xx++) {
					pnt.ras[xx][yy] = 0;
				}
			}
			break;
		}

		for (int i = 0; i < layer.getObjectCount(); i++) {
			DGeoObject gobj = (DGeoObject) layer.getObjectAt(i);
			if (gobj == null) {
				continue;
			}
			if (!((DGeoLayer) layer).isObjectActive(i)) {
				continue;
			}

			if (gobj.getGeometry() instanceof MultiGeometry) {
				mg = (MultiGeometry) gobj.getGeometry();
				multi = true;
				mgparts = mg.getPartsCount();
			} else {
				multi = false;
				mgparts = 1;
			}
			for (int part = 0; part < mgparts; part++) {
				if (multi) {
					rp = (RealPoint) mg.getPart(part);
				} else {
					rp = (RealPoint) gobj.getGeometry();
				}
				if (rp == null) {
					continue;
				}

				if (func == fWeighted) {
					pdata = gobj.getData();
					if (pdata == null) {
						continue;
					}
					weight = (float) pdata.getNumericAttrValue(attrN);
				}

				xx = Math.round(pnt.getGridX(rp.x));
				yy = Math.round(pnt.getGridY(rp.y));
				x1 = (int) Math.floor(xx - RadX);
				x2 = (int) Math.ceil(xx + RadX);
				y1 = (int) Math.floor(yy - RadY);
				y2 = (int) Math.ceil(yy + RadY);
				if (x1 < 0) {
					x1 = 0;
				}
				if (y1 < 0) {
					y1 = 0;
				}
				if (x2 >= pnt.Col) {
					x2 = pnt.Col - 1;
				}
				if (y2 >= pnt.Row) {
					y2 = pnt.Row - 1;
				}
				for (int ry = y1; ry <= y2; ry++) {
					for (int rx = x1; rx <= x2; rx++) {
/*
          if ( Math.sqrt( (xx-rx)*(xx-rx)/RadX/RadX + (yy-ry)*(yy-ry)/RadY/RadY ) < 1 )
            pnt.ras[rx][ry]+=weight*incr;
*/
//calculation for each point
						switch (func) {
						case fDistance:
							dist = rp.distanceToPoint(pnt.getWorldX(rx), pnt.getWorldY(ry));
							if (dist < rad) {
								pnt.ras[rx][ry] = (float) Math.min(pnt.ras[rx][ry], dist);
							}
							break;
						case fCloseness:
							dist = rp.distanceToPoint(pnt.getWorldX(rx), pnt.getWorldY(ry));
							dist = 1 - dist / rad;
							if (dist > 0) {
								pnt.ras[rx][ry] = (float) Math.max(pnt.ras[rx][ry], dist);
							}
							break;
						case fInfluence:
							dist = rp.distanceToPoint(pnt.getWorldX(rx), pnt.getWorldY(ry));
							dist = 1 - dist / rad;
							if (dist > 0) {
								pnt.ras[rx][ry] += (float) dist;
							}
							break;
						case fNumber:
//                    dist = rp.distanceToPoint(pnt.getWorldX(rx), pnt.getWorldY(ry));
//                    if (dist < rad) pnt.ras[rx][ry] += 1;
							if (Math.sqrt((xx - rx) * (xx - rx) / RadX / RadX + (yy - ry) * (yy - ry) / RadY / RadY) < 1) {
								pnt.ras[rx][ry] += 1;
							}
							break;
						case fDensity:
//                    dist = rp.distanceToPoint(pnt.getWorldX(rx), pnt.getWorldY(ry));
//                    if (dist < rad) pnt.ras[rx][ry] += incr;
							if (Math.sqrt((xx - rx) * (xx - rx) / RadX / RadX + (yy - ry) * (yy - ry) / RadY / RadY) < 1) {
								pnt.ras[rx][ry] += incr;
							}
							break;
						case fWeighted:
//                    dist = rp.distanceToPoint(pnt.getWorldX(rx), pnt.getWorldY(ry));
//                    if (dist < rad) pnt.ras[rx][ry] += incr*weight;
							if (Math.sqrt((xx - rx) * (xx - rx) / RadX / RadX + (yy - ry) * (yy - ry) / RadY / RadY) < 1) {
								pnt.ras[rx][ry] += weight * incr;
							}
							break;
						}

					}
				}
				if (ui != null) {
					int perc = Math.round((i + 1) / layer.getObjectCount() * 100.0f);
					// following string:"Calculation: "+perc+"% ready"
					ui.showMessage(res.getString("Calculation_") + perc + res.getString("_ready"));
				}
			}
		}

/*
    for (yy=0; yy<pnt.Row; yy++)
      for (xx=0; xx<pnt.Col; xx++)
        pnt.ras[xx][yy]=0;

    switch (func) {
      case fDensity:
        incr=(float)(1/(Math.PI*rad*rad));
        for (int i=0; i<layer.getObjectCount(); i++) {
          DGeoObject gobj=(DGeoObject)layer.getObjectAt(i);
          if (gobj==null) continue;
          if (!((DGeoLayer)layer).isObjectActive(i)) continue;
          rp = (RealPoint)gobj.getGeometry();
          if (rp==null) continue;
          xx = (int)Math.round(pnt.getGridX(rp.x));
          yy = (int)Math.round(pnt.getGridY(rp.y));
          x1=(int)Math.floor(xx-RadX);
          x2=(int)Math.ceil(xx+RadX);
          y1=(int)Math.floor(yy-RadY);
          y2=(int)Math.ceil(yy+RadY);
          if (x1<0) x1=0;
          if (y1<0) y1=0;
          if (x2>=pnt.Col) x2=pnt.Col-1;
          if (y2>=pnt.Row) y2=pnt.Row-1;
          for (int ry=y1; ry<=y2; ry++)
            for (int rx=x1; rx<=x2; rx++) {
              if ( Math.sqrt( (xx-rx)*(xx-rx)/RadX/RadX + (yy-ry)*(yy-ry)/RadY/RadY ) < 1 )
                pnt.ras[rx][ry]+=incr;
            }
          if (ui!=null) {
            int perc=Math.round((i+1)/layer.getObjectCount()*100.0f);
            // following string:"Calculation: "+perc+"% ready"
            ui.showMessage(res.getString("Calculation_")+perc+res.getString("_ready"));
          }
        }
        break;
      case fWeighted:
        if (attrN<0) return null;
        incr=(float)(1/(Math.PI*rad*rad));
        for (int i=0; i<layer.getObjectCount(); i++) {
          DGeoObject gobj=(DGeoObject)layer.getObjectAt(i);
          if (gobj==null) continue;
          if (!((DGeoLayer)layer).isObjectActive(i)) continue;
          rp = (RealPoint)gobj.getGeometry();
          if (rp==null) continue;
          ThematicDataItem pdata = gobj.getData();
          if (pdata==null) continue;
          float weight=pdata.getNumericAttrValue(attrN);
          xx = (int)Math.round(pnt.getGridX(rp.x));
          yy = (int)Math.round(pnt.getGridY(rp.y));
          x1=(int)Math.floor(xx-RadX);
          x2=(int)Math.ceil(xx+RadX);
          y1=(int)Math.floor(yy-RadY);
          y2=(int)Math.ceil(yy+RadY);
          if (x1<0) x1=0;
          if (y1<0) y1=0;
          if (x2>=pnt.Col) x2=pnt.Col-1;
          if (y2>=pnt.Row) y2=pnt.Row-1;
          for (int ry=y1; ry<=y2; ry++)
            for (int rx=x1; rx<=x2; rx++) {
              if ( Math.sqrt( (xx-rx)*(xx-rx)/RadX/RadX + (yy-ry)*(yy-ry)/RadY/RadY ) < 1 )
                pnt.ras[rx][ry]+=weight*incr;
            }
          if (ui!=null) {
            int perc=Math.round((i+1)/layer.getObjectCount()*100.0f);
            // following string:"Calculation: "+perc+"% ready"
            ui.showMessage(res.getString("Calculation_")+perc+res.getString("_ready"));
          }
        }
        break;
      case fNumber:
        for (int i=0; i<layer.getObjectCount(); i++) {
          DGeoObject gobj=(DGeoObject)layer.getObjectAt(i);
          if (gobj==null) continue;
          if (!((DGeoLayer)layer).isObjectActive(i)) continue;
          rp = (RealPoint)gobj.getGeometry();
          if (rp==null) continue;
          xx = (int)Math.round(pnt.getGridX(rp.x));
          yy = (int)Math.round(pnt.getGridY(rp.y));

          x1=(int)Math.floor(xx-RadX);
          x2=(int)Math.ceil(xx+RadX);
          y1=(int)Math.floor(yy-RadY);
          y2=(int)Math.ceil(yy+RadY);
          if (x1<0) x1=0;
          if (y1<0) y1=0;
          if (x2>=pnt.Col) x2=pnt.Col-1;
          if (y2>=pnt.Row) y2=pnt.Row-1;

          for (int ry=y1; ry<=y2; ry++)
            for (int rx=x1; rx<=x2; rx++) {
              if ( Math.sqrt( (xx-rx)*(xx-rx)/RadX/RadX + (yy-ry)*(yy-ry)/RadY/RadY ) < 1 )
                pnt.ras[rx][ry]+=1;
            }
          if (ui!=null) {
            int perc=Math.round((i+1)/layer.getObjectCount()*100.0f);
            // following string:"Calculation: "+perc+"% ready"
            ui.showMessage(res.getString("Calculation_")+perc+res.getString("_ready"));
          }
        }
        break;
      case fDistance:
        for (yy=0; yy<pnt.Row; yy++)
          for (xx=0; xx<pnt.Col; xx++)
            pnt.ras[xx][yy]=(float)rad;
        for (int i=0; i<layer.getObjectCount(); i++) {
          DGeoObject gobj=(DGeoObject)layer.getObjectAt(i);
          if (gobj==null) continue;
          if (!((DGeoLayer)layer).isObjectActive(i)) continue;
          rp = (RealPoint)gobj.getGeometry();
          if (rp==null) continue;
          xx = (int)Math.round(pnt.getGridX(rp.x));
          yy = (int)Math.round(pnt.getGridY(rp.y));
          x1=(int)Math.floor(xx-RadX);
          x2=(int)Math.ceil(xx+RadX);
          y1=(int)Math.floor(yy-RadY);
          y2=(int)Math.ceil(yy+RadY);
          if (x1<0) x1=0;
          if (y1<0) y1=0;
          if (x2>=pnt.Col) x2=pnt.Col-1;
          if (y2>=pnt.Row) y2=pnt.Row-1;
          for (int ry=y1; ry<=y2; ry++)
            for (int rx=x1; rx<=x2; rx++) {
              distX = (xx-rx)*pnt.DX;
              distY = (yy-ry)*pnt.DY;
              dist = Math.sqrt( distX*distX + distY*distY );
              if ( dist < rad )
                pnt.ras[rx][ry] = (float)Math.min(pnt.ras[rx][ry], dist);
            }
          if (ui!=null) {
            int perc=Math.round((i+1)/layer.getObjectCount()*100.0f);
            // following string:"Calculation: "+perc+"% ready"
            ui.showMessage(res.getString("Calculation_")+perc+res.getString("_ready"));
          }
        }
        break;
      case fCloseness:
        for (int i=0; i<layer.getObjectCount(); i++) {
          DGeoObject gobj=(DGeoObject)layer.getObjectAt(i);
          if (gobj==null) continue;
          if (!((DGeoLayer)layer).isObjectActive(i)) continue;
          rp = (RealPoint)gobj.getGeometry();
          if (rp==null) continue;
          xx = (int)Math.round(pnt.getGridX(rp.x));
          yy = (int)Math.round(pnt.getGridY(rp.y));
          x1=(int)Math.floor(xx-RadX);
          x2=(int)Math.ceil(xx+RadX);
          y1=(int)Math.floor(yy-RadY);
          y2=(int)Math.ceil(yy+RadY);
          if (x1<0) x1=0;
          if (y1<0) y1=0;
          if (x2>=pnt.Col) x2=pnt.Col-1;
          if (y2>=pnt.Row) y2=pnt.Row-1;
          for (int ry=y1; ry<=y2; ry++)
            for (int rx=x1; rx<=x2; rx++) {
              distX = (xx-rx)*pnt.DX;
              distY = (yy-ry)*pnt.DY;
              dist = 1-Math.sqrt( distX*distX + distY*distY )/rad;
              if ( dist > 0 )
                pnt.ras[rx][ry] = (float)Math.max(pnt.ras[rx][ry], dist);
            }
          if (ui!=null) {
            int perc=Math.round((i+1)/layer.getObjectCount()*100.0f);
            // following string:"Calculation: "+perc+"% ready"
            ui.showMessage(res.getString("Calculation_")+perc+res.getString("_ready"));
          }
        }
        break;
      case fInfluence:
        for (int i=0; i<layer.getObjectCount(); i++) {
          DGeoObject gobj=(DGeoObject)layer.getObjectAt(i);
          if (gobj==null) continue;
          if (!((DGeoLayer)layer).isObjectActive(i)) continue;
          rp = (RealPoint)gobj.getGeometry();
          if (rp==null) continue;
          xx = (int)Math.round(pnt.getGridX(rp.x));
          yy = (int)Math.round(pnt.getGridY(rp.y));

          x1=(int)Math.floor(xx-RadX);
          x2=(int)Math.ceil(xx+RadX);
          y1=(int)Math.floor(yy-RadY);
          y2=(int)Math.ceil(yy+RadY);
          if (x1<0) x1=0;
          if (y1<0) y1=0;
          if (x2>=pnt.Col) x2=pnt.Col-1;
          if (y2>=pnt.Row) y2=pnt.Row-1;

          for (int ry=y1; ry<=y2; ry++)
            for (int rx=x1; rx<=x2; rx++) {
              distX = (xx-rx)*pnt.DX;
              distY = (yy-ry)*pnt.DY;
              dist = Math.sqrt( distX*distX + distY*distY );

              dist = 1-dist/rad;

              if ( dist > 0 )
                pnt.ras[rx][ry] += (float)dist;
            }
          if (ui!=null) {
            int perc=Math.round((i+1)/layer.getObjectCount()*100.0f);
            // following string:"Calculation: "+perc+"% ready"
            ui.showMessage(res.getString("Calculation_")+perc+res.getString("_ready"));
          }
        }
        break;
    } //end switch
*/

		pnt.recalculateStatistics();
		String prep = " " + ((func <= 1) ? res.getString("to1") : res.getString("of1")) + " ";
		DGridLayer grl = constructRasterLayer(pnt, functions[func] + prep + layer.getName());
		GrayColorScale cs = new GrayColorScale();
		cs.reversed = true;
		grl.getGridVisualizer().setColorScale(cs);
		return grl;
	}
}
