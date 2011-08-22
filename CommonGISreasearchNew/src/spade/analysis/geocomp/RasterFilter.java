package spade.analysis.geocomp;

import java.awt.Choice;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.util.ResourceBundle;

import spade.analysis.geocomp.mutil.CFloat;
import spade.analysis.geocomp.mutil.SortVector;
import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Centimeter;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;
import spade.vis.dmap.DLayerManager;
import spade.vis.geometry.RasterGeometry;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

/**
* Performs filtering or smoothing of a raster layer using functions Mean,
* Median, "RMS", Local anomalies, Maximum, Minimum, Max-Min
*/
public class RasterFilter extends GeoCalculator {
	static ResourceBundle res = Language.getTextResource("spade.analysis.geocomp.Res");
	/**
	* Filtering functions
	*/
	protected static final String[] functions =
	// following strings: "Mean", "Median", "RMS", "Local anomalies", "Maximum", "Minimum", "Max-Min"
	{ res.getString("Mean"), "Median", "RMS", res.getString("Local_anomalies"), "Maximum", "Minimum", "Max-Min" };
	protected static final int fMean = 0, fMedian = 1, fRMS = 2, fLocal = 3, fMax = 4, fMin = 5, fMaxMin = 6;

	/**
	* Filters a raster layer. The arguments are a layer manager (a
	* GeoCalculator must itself care about selection of a layer or layers of
	* appropriate type) and SystemUI (to be used for displaying messages and
	* finding an owner frame for dialogs)
	* If calculation was successful, returns a new layer (an instance
	* of DGridLayer) being the result of filtering.
	*/
	@Override
	public Object doCalculation(LayerManager lman, ESDACore core) {
		SystemUI ui = core.getUI();
		// following strings:"Select RASTER layer"
		GeoLayer layer = selectRasterLayer(lman, ui, res.getString("Select_RASTER_layer"));
		if (layer == null)
			return null;
		RasterGeometry rg = getRaster(layer, ui);
		if (rg == null)
			return null;

		//the user sets parameters of the tool
		double delta = Math.max(rg.DX, rg.DY);
		Panel p = new Panel(new GridLayout(4, 1));
		// following strings:"Filtering parameters"
		p.add(new Label(res.getString("Filtering_parameters")));
		Panel pp = new Panel(new FlowLayout(FlowLayout.LEFT));
		// following strings:"Function:"
		pp.add(new Label(res.getString("Function_")));
		Choice c = new Choice();
		for (String function : functions) {
			c.add(function);
		}
		pp.add(c);
		p.add(pp);
		pp = new Panel(new FlowLayout(FlowLayout.LEFT));
		// following strings:"Radius:"
		pp.add(new Label(res.getString("Radius_")));
		TextField radTF = new TextField(String.valueOf(delta), 5);
		pp.add(radTF);
		p.add(pp);

		pp = new Panel(new FlowLayout(FlowLayout.LEFT));
		pp.add(new Label(res.getString("Scale")));
		Centimeter cm = new Centimeter();
		pp.add(cm);
		float sc = ui.getMapViewer(ui.getCurrentMapN()).getMapDrawer().getMapContext().getPixelValue() * cm.getMinimumSize().width * ((DLayerManager) lman).user_factor;
		pp.add(new Label(StringUtil.floatToStr(sc, 2) + " " + ((DLayerManager) lman).getUserUnit()));
		p.add(pp);

		Frame win = null;
		if (ui != null) {
			win = ui.getMainFrame();
		}
		if (win == null) {
			win = CManager.getAnyFrame();
		}
		// following strings:"Set parameters"
		OKDialog okd = new OKDialog(win, res.getString("Set_parameters"), true);
		okd.addContent(p);
		okd.show();
		if (okd.wasCancelled())
			return null;

		int func = c.getSelectedIndex();
		double rad = delta;
		String str = radTF.getText();
		if (str != null) {
			try {
				rad = Double.valueOf(str).doubleValue();
			} catch (NumberFormatException nfe) {
			}
		}
		if (rad < delta) {
			rad = delta;
		}

		double RadX = rad / rg.DX;
		double RadY = rad / rg.DY;
		double sum = 0, sum1 = 0;
		int counter = 0, counter1 = 0;
		float val = 0, val1 = 0;
		int x1, x2, y1, y2, tx1, tx2;

		RasterGeometry filter = (RasterGeometry) rg.clone();

		for (int yy = 0; yy < filter.Row; yy++) {
			for (int xx = 0; xx < filter.Col; xx++) {
				filter.ras[xx][yy] = 0;
			}
		}

		float rsize = 1.0f * filter.Row * filter.Col;

		switch (func) {
		case fMean:
			for (int yy = 0; yy < filter.Row; yy++) {
				for (int xx = 0; xx < filter.Col; xx++) {
					tx1 = (int) Math.floor(xx - RadX);
					tx2 = (int) Math.ceil(xx + RadX);
					y1 = (int) Math.floor(yy - RadY);
					y2 = (int) Math.ceil(yy + RadY);
					if (tx1 < 0) {
						x1 = 0;
					} else {
						x1 = tx1;
					}
					if (y1 < 0) {
						y1 = 0;
					}
					if (tx2 >= filter.Col) {
						x2 = filter.Col - 1;
					} else {
						x2 = tx2;
					}
					if (y2 >= filter.Row) {
						y2 = filter.Row - 1;
					}
					if (tx1 < 1 || x2 != tx2) {
						sum = 0;
						counter = 0;
						for (int ry = y1; ry <= y2; ry++) {
							for (int rx = x1; rx <= x2; rx++) {
								val = rg.ras[rx][ry];
								if (!Float.isNaN(val)) {
									sum += val;
									counter++;
								}
							}
						}
					} else {
						sum1 = 0;
						counter1 = 0;
						for (int ry = y1; ry <= y2; ry++) {
							val = rg.ras[x1 - 1][ry];
							if (!Float.isNaN(val)) {
								sum1 += val;
								counter1++;
							}
						}
						sum -= sum1;
						counter -= counter1;
						sum1 = 0;
						counter1 = 0;
						for (int ry = y1; ry <= y2; ry++) {
							val = rg.ras[x2][ry];
							if (!Float.isNaN(val)) {
								sum1 += val;
								counter1++;
							}
						}
						sum += sum1;
						counter += counter1;
					}
					if (counter == 0) {
						filter.ras[xx][yy] = Float.NaN;
					} else {
						filter.ras[xx][yy] = (float) (sum / counter);
					}
				}
				if (ui != null) {
					int perc = Math.round(((yy + 1) * filter.Col) / rsize * 100);
					// following strings:"Calculation: "+String.valueOf(perc)+"% ready"
					ui.showMessage(res.getString("Calculation_") + String.valueOf(perc) + res.getString("_ready"));
				}
			}
			break;

		case fLocal:
			for (int yy = 0; yy < filter.Row; yy++) {
				for (int xx = 0; xx < filter.Col; xx++) {
					tx1 = (int) Math.floor(xx - RadX);
					tx2 = (int) Math.ceil(xx + RadX);
					y1 = (int) Math.floor(yy - RadY);
					y2 = (int) Math.ceil(yy + RadY);
					if (tx1 < 0) {
						x1 = 0;
					} else {
						x1 = tx1;
					}
					if (y1 < 0) {
						y1 = 0;
					}
					if (tx2 >= filter.Col) {
						x2 = filter.Col - 1;
					} else {
						x2 = tx2;
					}
					if (y2 >= filter.Row) {
						y2 = filter.Row - 1;
					}
					if (tx1 < 1 || x2 != tx2) {
						sum = 0;
						counter = 0;
						for (int ry = y1; ry <= y2; ry++) {
							for (int rx = x1; rx <= x2; rx++) {
								val = rg.ras[rx][ry];
								if (!Float.isNaN(val)) {
									sum += val;
									counter++;
								}
							}
						}
					} else {
						sum1 = 0;
						counter1 = 0;
						for (int ry = y1; ry <= y2; ry++) {
							val = rg.ras[x1 - 1][ry];
							if (!Float.isNaN(val)) {
								sum1 += val;
								counter1++;
							}
						}
						sum -= sum1;
						counter -= counter1;
						sum1 = 0;
						counter1 = 0;
						for (int ry = y1; ry <= y2; ry++) {
							val = rg.ras[x2][ry];
							if (!Float.isNaN(val)) {
								sum1 += val;
								counter1++;
							}
						}
						sum += sum1;
						counter += counter1;
					}
					if (counter == 0) {
						filter.ras[xx][yy] = Float.NaN;
					} else {
						filter.ras[xx][yy] = rg.ras[xx][yy] - (float) (sum / counter);
					}
				}
				if (ui != null) {
					int perc = Math.round(((yy + 1) * filter.Col) / rsize * 100);
					// following strings:"Calculation: "+String.valueOf(perc)+"% ready"
					ui.showMessage(res.getString("Calculation_") + String.valueOf(perc) + res.getString("_ready"));
				}
			}
			break;
		case fRMS:
			for (int yy = 0; yy < filter.Row; yy++) {
				for (int xx = 0; xx < filter.Col; xx++) {
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
					if (x2 >= filter.Col) {
						x2 = filter.Col - 1;
					}
					if (y2 >= filter.Row) {
						y2 = filter.Row - 1;
					}
					sum = 0;
					counter = 0;
					for (int ry = y1; ry <= y2; ry++) {
						for (int rx = x1; rx <= x2; rx++) {
							val = rg.ras[rx][ry];
							if (!Float.isNaN(val)) {
								sum += val;
								counter++;
							}
						}
					}
					if (counter <= 1) {
						filter.ras[xx][yy] = Float.NaN;
						continue;
					} else {
						sum /= counter;
					}
					sum1 = 0;
					for (int ry = y1; ry <= y2; ry++) {
						for (int rx = x1; rx <= x2; rx++) {
							val = rg.ras[rx][ry];
							if (!Float.isNaN(val)) {
								sum1 += (val - sum) * (val - sum);
							}
						}
					}
					filter.ras[xx][yy] = (float) Math.sqrt(sum1 / (counter - 1));
				}
				if (ui != null) {
					int perc = Math.round(((yy + 1) * filter.Col) / rsize * 100);
					// following strings:"Calculation: "+String.valueOf(perc)+"% ready"
					ui.showMessage(res.getString("Calculation_") + String.valueOf(perc) + res.getString("_ready"));
				}
			}
			break;
		case fMax:
			for (int yy = 0; yy < filter.Row; yy++) {
				for (int xx = 0; xx < filter.Col; xx++) {
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
					if (x2 >= filter.Col) {
						x2 = filter.Col - 1;
					}
					if (y2 >= filter.Row) {
						y2 = filter.Row - 1;
					}

					sum = Double.NEGATIVE_INFINITY;
					for (int ry = y1; ry <= y2; ry++) {
						for (int rx = x1; rx <= x2; rx++) {
							val = rg.ras[rx][ry];
							if (!Float.isNaN(val)
//                                    && Math.sqrt( (xx-rx)*(xx-rx)/RadX/RadX + (yy-ry)*(yy-ry)/RadY/RadY ) < 1
//                                    && (xx-rx)*(xx-rx) + (yy-ry)*(yy-ry) < RadX*RadY
							) {
								sum = Math.max(sum, val);
							}
						}
					}
					if (Double.isInfinite(sum)) {
						filter.ras[xx][yy] = Float.NaN;
					} else {
						filter.ras[xx][yy] = (float) sum;
					}
				}
				if (ui != null) {
					int perc = Math.round(((yy + 1) * filter.Col) / rsize * 100);
					// following strings:"Calculation: "+String.valueOf(perc)+"% ready
					ui.showMessage(res.getString("Calculation_") + String.valueOf(perc) + res.getString("_ready"));
				}
			}
			break;
		case fMin:
			for (int yy = 0; yy < filter.Row; yy++) {
				for (int xx = 0; xx < filter.Col; xx++) {

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
					if (x2 >= filter.Col) {
						x2 = filter.Col - 1;
					}
					if (y2 >= filter.Row) {
						y2 = filter.Row - 1;
					}

					sum = Double.POSITIVE_INFINITY;
					for (int ry = y1; ry <= y2; ry++) {
						for (int rx = x1; rx <= x2; rx++) {
							val = rg.ras[rx][ry];
							if (!Float.isNaN(val)) {
								sum = Math.min(sum, val);
							}
						}
					}

					if (Double.isInfinite(sum)) {
						filter.ras[xx][yy] = Float.NaN;
					} else {
						filter.ras[xx][yy] = (float) sum;
					}
				}
				if (ui != null) {
					int perc = Math.round(((yy + 1) * filter.Col) / rsize * 100);
					// following strings:"Calculation: "+String.valueOf(perc)+"% ready"
					ui.showMessage(res.getString("Calculation_") + String.valueOf(perc) + res.getString("_ready"));
				}
			}
			break;
		case fMaxMin:
			for (int yy = 0; yy < filter.Row; yy++) {
				for (int xx = 0; xx < filter.Col; xx++) {
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
					if (x2 >= filter.Col) {
						x2 = filter.Col - 1;
					}
					if (y2 >= filter.Row) {
						y2 = filter.Row - 1;
					}

					sum = Double.POSITIVE_INFINITY;
					sum1 = Double.NEGATIVE_INFINITY;
					for (int ry = y1; ry <= y2; ry++) {
						for (int rx = x1; rx <= x2; rx++) {
							val = rg.ras[rx][ry];
							if (!Float.isNaN(val)) {
								sum = Math.min(sum, val);
								sum1 = Math.max(sum1, val);
							}
						}
					}

					if (Double.isInfinite(sum) || Double.isInfinite(sum1)) {
						filter.ras[xx][yy] = Float.NaN;
					} else {
						filter.ras[xx][yy] = (float) (sum1 - sum);
					}
				}
				if (ui != null) {
					int perc = Math.round(((yy + 1) * filter.Col) / rsize * 100);
					// following strings:"Calculation: "+String.valueOf(perc)+"% ready"
					ui.showMessage(res.getString("Calculation_") + String.valueOf(perc) + res.getString("_ready"));
				}
			}
			break;
		case fMedian:
			SortVector sv = new SortVector((int) (RadX * RadY));
			for (int yy = 0; yy < filter.Row; yy++) {
				for (int xx = 0; xx < filter.Col; xx++) {

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
					if (x2 >= filter.Col) {
						x2 = filter.Col - 1;
					}
					if (y2 >= filter.Row) {
						y2 = filter.Row - 1;
					}

					sv.removeAllElements();

					for (int ry = y1; ry <= y2; ry++) {
						for (int rx = x1; rx <= x2; rx++) {
							val = rg.ras[rx][ry];
							if (!Float.isNaN(val)) {
								sv.Insert(new CFloat(val));
							}
						}
					}

					if (sv.isEmpty()) {
						filter.ras[xx][yy] = Float.NaN;
					} else {
						filter.ras[xx][yy] = ((CFloat) sv.elementAt(sv.size() / 2)).v.floatValue();
					}
				}
				if (ui != null) {
					int perc = Math.round(((yy + 1) * filter.Col) / rsize * 100);
					// following strings: "Calculation: "+String.valueOf(perc)+"% ready"
					ui.showMessage(res.getString("Calculation_") + String.valueOf(perc) + res.getString("_ready"));
				}
			}
			break;
		} //end switch
		filter.recalculateStatistics();
		return constructRasterLayer(filter, functions[func] + " from " + layer.getName());
	}
}
