package spade.analysis.tools;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.CopyFile;
import spade.lib.util.StringUtil;
import spade.vis.action.Highlighter;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.MultiGeometry;
import spade.vis.geometry.RealCircle;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.geometry.RealRectangle;
import spade.vis.geometry.Sign;
import spade.vis.map.MapContext;
import spade.vis.map.MapViewer;
import spade.vis.mapvis.Visualizer;
import spade.vis.space.LayerManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 29-Aug-2007
 * Time: 11:32:14
 * Exports geographical vector objects to Google Earth and Google Maps.
 */
public class GeoObjectsToGoogle implements DataAnalyser {
	protected ESDACore core = null;
	/**
	* Remembers the last directory where KML files were saved
	*/
	protected static String lastDir = null;

	/**
	 * Returns true when the tool has everything necessary for its operation.
	 * For example, if the tool manages a number of analysis methods, it should
	 * check whether the class for at least one method is available.
	 * Here, always returns true.
	 */
	public boolean isValid(ESDACore core) {
		return true;
	}

	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	public void run(ESDACore core) {
		if (core == null || core.getUI() == null)
			return;
		this.core = core;
		MapViewer mapViewer = core.getUI().getLatestMapViewer();
		if (mapViewer == null || mapViewer.getLayerManager() == null) {
			showMessage("No map exists!", true);
			return;
		}
		//Find instances of DGeoLayer containing trajectories
		LayerManager lman = mapViewer.getLayerManager();
		Vector layers = new Vector(lman.getLayerCount(), 1);
		boolean geo = false, someObjectsSelected = false;
		for (int i = 0; i < lman.getLayerCount(); i++)
			if (lman.getGeoLayer(i) instanceof DGeoLayer) {
				DGeoLayer layer = (DGeoLayer) lman.getGeoLayer(i);
				if (layer.getObjectCount() < 1) {
					continue;
				}
				if (layer.getType() != Geometry.point && layer.getType() != Geometry.line && layer.getType() != Geometry.area) {
					continue;
				}
				layers.addElement(layer);
				geo = geo || layer.isGeographic();
				if (!someObjectsSelected) {
					Highlighter hl = core.getHighlighterForContainer(layer.getContainerIdentifier());
					if (hl != null) {
						Vector sel = hl.getSelectedObjects();
						someObjectsSelected = sel != null && sel.size() > 0;
					}
				}
			}
		if (layers.size() < 1) {
			showMessage("No layers with vector objects found!", true);
			return;
		}
		if (!geo) {
			showMessage("The data are not in geographic coordinates!", true);
			return;
		}
		Panel mainP = new Panel(new ColumnLayout());
		mainP.add(new Label("Export to:"));
		Panel p = new Panel(new FlowLayout(FlowLayout.LEFT));
		CheckboxGroup cbg = new CheckboxGroup();
		Checkbox cbDest[] = new Checkbox[2];
		p.add(cbDest[0] = new Checkbox("KML (Google Earth)", true, cbg));
		p.add(cbDest[1] = new Checkbox("HTML (Google Map)", false, cbg));
		mainP.add(p);
		mainP.add(new Line(false));
		mainP.add(new Label("Select the layer with objects to view in Google*:"));
		List list = new List(Math.max(layers.size() + 1, 5));
		for (int i = 0; i < layers.size(); i++) {
			list.add(((DGeoLayer) layers.elementAt(i)).getName());
		}
		list.select(0);
		mainP.add(list);
		Checkbox cbSel = null;
		if (someObjectsSelected) {
			cbSel = new Checkbox("export only selected objects", true);
			mainP.add(cbSel);
		}
		mainP.add(new Label("Name to appear in Google:"));
		TextField tfName = new TextField("Objects");
		mainP.add(tfName);
		mainP.add(new Line(false));
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Export objects to Google", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return;
		int idx = list.getSelectedIndex();
		if (idx < 0)
			return;
		DGeoLayer layer = (DGeoLayer) layers.elementAt(idx);
		Vector selected = null;
		if (cbSel != null && cbSel.getState()) {
			Highlighter hl = core.getHighlighterForSet(layer.getEntitySetIdentifier());
			if (hl != null) {
				selected = hl.getSelectedObjects();
			}
			if (selected == null || selected.size() < 1)
				if (!Dialogs.askYesOrNo(core.getUI().getMainFrame(), "None of the objects " + "have been selected in layer <" + layer.getName() + ">. Export all active objects?", "No selected objects!"))
					return;
		}
		FileDialog fd = new FileDialog(core.getUI().getMainFrame(), "Output file");
		if (lastDir != null) {
			fd.setDirectory(lastDir);
		}
		fd.setFile((cbDest[0].getState()) ? "*.kml" : "*.html");
		fd.setMode(FileDialog.SAVE);
		fd.show();
		if (fd.getDirectory() == null)
			return;
		lastDir = fd.getDirectory();
		String filename = fd.getFile();
		String str = CopyFile.getExtension(filename);
		if (str == null || str.length() < 1) {
			filename += (cbDest[0].getState()) ? ".kml" : ".html";
		}
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(lastDir + filename);
		} catch (IOException ioe) {
			showMessage(ioe.toString(), true);
			return;
		}
		if (out == null) {
			showMessage("Could not create the file!", true);
			return;
		}
		int result = 0;
		if (cbDest[0].getState()) {
			result = exportObjectsToGoogleEarth(layer, selected, out, tfName.getText());
		} else {
			result = exportObjectsToGoogleMap(layer, selected, out, tfName.getText());
		}
		if (result > 0) {
			showMessage(result + " objects have been exported to file " + lastDir + filename, false);
		}
	}

	/**
	 * @return the number of exported objects
	 */
	protected int exportObjectsToGoogleEarth(DGeoLayer layer, Vector selected, OutputStream stream, String name) {
		if (layer == null || stream == null || layer.getObjectCount() < 1)
			return 0;
		if (selected != null && selected.size() < 1) {
			selected = null;
		}
		float minx = Float.NaN, miny = Float.NaN, maxx = Float.NaN, maxy = Float.NaN;
		Vector selObjects = new Vector((selected == null) ? 100 : selected.size(), 100);
		for (int i = 0; i < layer.getObjectCount(); i++) {
			DGeoObject obj = layer.getObject(i);
			if (selected != null && !selected.contains(obj.getIdentifier())) {
				continue;
			}
			Geometry geom = obj.getGeometry();
			if (geom == null) {
				continue;
			}
			if (!(geom instanceof RealPoint) && !(geom instanceof RealCircle) && !(geom instanceof RealPolyline) && !(geom instanceof MultiGeometry)) {
				continue;
			}
			if (!layer.isObjectActive(i)) {
				continue;
			}
			RealRectangle rr = obj.getBounds();
			if (Float.isNaN(minx) || minx > rr.rx1) {
				minx = rr.rx1;
			}
			if (Float.isNaN(miny) || minx > rr.ry1) {
				miny = rr.ry1;
			}
			if (Float.isNaN(maxx) || maxx < rr.rx2) {
				maxx = rr.rx2;
			}
			if (Float.isNaN(maxy) || maxy < rr.ry2) {
				maxy = rr.ry2;
			}
			selObjects.addElement(obj);
		}
		if (selObjects.size() < 1) {
			showMessage("No suitable objects found!", true);
			return 0;
		}
		Visualizer vis = layer.getVisualizer(), bkgVis = layer.getBackgroundVisualizer();
		MapContext mc = null;
		if (vis != null || bkgVis != null) {
			mc = core.getUI().getLatestMapViewer().getMapDrawer().getMapContext();
		}
		Color defLineColor = Color.blue, defFillColor = Color.yellow;
		DrawingParameters dp = layer.getDrawingParameters();
		int lineWidth = 3, transparency = 255;
		if (dp != null) {
			defLineColor = dp.lineColor;
			defFillColor = dp.fillColor;
			if (!dp.fillContours) {
				defFillColor = null;
			}
			lineWidth = dp.lineWidth;
			transparency = 255 * (100 - dp.transparency) / 100;
		}

		DataOutputStream dos = new DataOutputStream(stream);
		int nSaved = 0;

		try {
			//write the file header...
			//...
			dos.writeBytes("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
			dos.writeBytes("<kml xmlns=\"http://earth.google.com/kml/2.1\">\r\n");
			dos.writeBytes("<Document>\r\n");
			dos.writeBytes("<name>" + name + "</name>\r\n");

			for (int i = 0; i < selObjects.size(); i++) {
				DGeoObject obj = (DGeoObject) selObjects.elementAt(i);
				Color color = null;
				if (vis != null) {
					Object pres = vis.getPresentation(obj.getSpatialData(), mc);
					if (pres != null)
						if (pres instanceof Color) {
							color = (Color) pres;
						} else if (pres instanceof Sign) {
							color = ((Sign) pres).getColor();
						}
				}
				if (color == null && bkgVis != null) {
					Object pres = bkgVis.getPresentation(obj.getSpatialData(), mc);
					if (pres != null && pres instanceof Color) {
						color = (Color) pres;
					}
				}
				Color lineColor = (color != null && layer.getType() == Geometry.line) ? color : defLineColor;
				Color fillColor = (color != null) ? color : defFillColor;

				putGeometry(obj.getGeometry(), lineColor, fillColor, lineWidth, transparency, dos, true);
				//...
			}

			//write the file footer...
			dos.writeBytes("\r\n");
			dos.writeBytes("\r\n");
			dos.writeBytes("</Document>\r\n</kml>\r\n");
		} catch (IOException ioe) {
			showMessage(ioe.toString(), true);
			return 0;
		}

		return nSaved;
	}

	/**
	 * @return the number of exported objects
	 */
	protected int exportObjectsToGoogleMap(DGeoLayer layer, Vector selected, OutputStream stream, String name) {
		if (layer == null || stream == null || layer.getObjectCount() < 1)
			return 0;
		if (selected != null && selected.size() < 1) {
			selected = null;
		}
		float minx = Float.NaN, miny = Float.NaN, maxx = Float.NaN, maxy = Float.NaN;
		Vector selObjects = new Vector((selected == null) ? 100 : selected.size(), 100);
		for (int i = 0; i < layer.getObjectCount(); i++) {
			DGeoObject obj = layer.getObject(i);
			if (selected != null && !selected.contains(obj.getIdentifier())) {
				continue;
			}
			Geometry geom = obj.getGeometry();
			if (geom == null) {
				continue;
			}
			if (!(geom instanceof RealPoint) && !(geom instanceof RealCircle) && !(geom instanceof RealPolyline) && !(geom instanceof MultiGeometry)) {
				continue;
			}
			if (!layer.isObjectActive(i)) {
				continue;
			}
			RealRectangle rr = obj.getBounds();
			if (Float.isNaN(minx) || minx > rr.rx1) {
				minx = rr.rx1;
			}
			if (Float.isNaN(miny) || minx > rr.ry1) {
				miny = rr.ry1;
			}
			if (Float.isNaN(maxx) || maxx < rr.rx2) {
				maxx = rr.rx2;
			}
			if (Float.isNaN(maxy) || maxy < rr.ry2) {
				maxy = rr.ry2;
			}
			selObjects.addElement(obj);
		}
		if (selObjects.size() < 1) {
			showMessage("No suitable objects found!", true);
			return 0;
		}
		Visualizer vis = layer.getVisualizer(), bkgVis = layer.getBackgroundVisualizer();
		MapContext mc = null;
		if (vis != null || bkgVis != null) {
			mc = core.getUI().getLatestMapViewer().getMapDrawer().getMapContext();
		}
		Color defLineColor = Color.blue, defFillColor = Color.yellow;
		DrawingParameters dp = layer.getDrawingParameters();
		int lineWidth = 3, transparency = 255;
		if (dp != null) {
			if (dp.lineColor != null) {
				defLineColor = dp.lineColor;
			}
			if (dp.fillColor != null) {
				defFillColor = dp.fillColor;
			}
			if (!dp.fillContours) {
				defFillColor = null;
			}
			lineWidth = dp.lineWidth;
			transparency = 255 * (100 - dp.transparency) / 100;
		}

		DataOutputStream dos = new DataOutputStream(stream);
		int nSaved = 0;

		try {
			//write the file header...
			dos.writeBytes("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\r\n");
			dos.writeBytes("    \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\r\n");
			dos.writeBytes("<html xmlns=\"http://www.w3.org/1999/xhtml\"\r\n");
			dos.writeBytes("      xmlns:v=\"urn:schemas-microsoft-com:vml\">\r\n");
			dos.writeBytes("  <head>\r\n");
			dos.writeBytes("    <meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\"/>\r\n");
			dos.writeBytes("    <title>" + name + "</title>\r\n");
			dos.writeBytes("<script src=\"http://maps.google.com/maps?file=api&amp;v=2&amp;key=ABQIAAAApv6Ws5gaRljPHstl9atm_hQTLlh_E_-6dOWotgx62c5YMCoZXhSfanusICTH1niY61AEfrcu-CGrlw\"\r\n");
			dos.writeBytes("        type=\"text/javascript\"></script>\r\n");
			dos.writeBytes("<script type=\"text/javascript\">\r\n");
			dos.writeBytes("//<![CDATA[\r\n");
			dos.writeBytes("function load() {\r\n");
			dos.writeBytes("if (GBrowserIsCompatible()) {\r\n");
			dos.writeBytes("  var map = new GMap2(document.getElementById(\"map\"));\r\n");
			dos.writeBytes("  map.addControl(new GLargeMapControl());\r\n");
			dos.writeBytes("  map.addControl(new GMapTypeControl());\r\n");
			dos.writeBytes("  map.setCenter(new GLatLng(" + (miny + maxy) / 2 + "," + (minx + maxx) / 2 + "), 13); \r\n");
			dos.writeBytes("  var myIcon = new GIcon();\r\n");
			dos.writeBytes("  myIcon.image = \"http://labs.google.com/ridefinder/images/mm_20_blue.png\";\r\n");
			dos.writeBytes("  myIcon.shadow = \"http://labs.google.com/ridefinder/images/mm_20_shadow.png\";\r\n");
			dos.writeBytes("  myIcon.iconSize = new GSize(12, 20);\r\n");
			dos.writeBytes("  myIcon.shadowSize = new GSize(22, 20);\r\n");
			dos.writeBytes("  myIcon.iconAnchor = new GPoint(6, 20);\r\n");
			dos.writeBytes("  myIcon.infoWindowAnchor = new GPoint(5, 1);\r\n");
			dos.writeBytes("  var polyline;\r\n");
			//dos.writeBytes("  myIcon.image = \"http://maps.google.com/mapfiles/kml/pal4/icon24.png\";\r\n");
			//dos.writeBytes("  myIcon.iconSize = new GSize(20, 34);\r\n");
			//dos.writeBytes("  myIcon.iconAnchor = new GPoint(10, 34);\r\n");

			for (int i = 0; i < selObjects.size(); i++) {
				DGeoObject obj = (DGeoObject) selObjects.elementAt(i);
				Color color = null;
				if (vis != null) {
					Object pres = vis.getPresentation(obj.getSpatialData(), mc);
					if (pres != null)
						if (pres instanceof Color) {
							color = (Color) pres;
						} else if (pres instanceof Sign) {
							color = ((Sign) pres).getColor();
						}
				}
				if (color == null && bkgVis != null) {
					Object pres = bkgVis.getPresentation(obj.getSpatialData(), mc);
					if (pres != null && pres instanceof Color) {
						color = (Color) pres;
					}
				}
				Color lineColor = (color != null && layer.getType() == Geometry.line) ? color : defLineColor;
				Color fillColor = (color != null) ? color : defFillColor;

				putGeometry(obj.getGeometry(), lineColor, fillColor, lineWidth, transparency, dos, false);
				//...
			}

			//write the file footer...
			dos.writeBytes("  }\r\n");
			dos.writeBytes("}\r\n");
			dos.writeBytes("//]]>\r\n");
			dos.writeBytes("</script>\r\n");
			dos.writeBytes("</head>\r\n");
			dos.writeBytes("<body onload=\"load()\" onunload=\"GUnload()\">\r\n");
			dos.writeBytes("  <div id=\"map\" style=\"width: 800px; height: 800px\"></div>\r\n");
			dos.writeBytes("</body>\r\n");
			dos.writeBytes("</html>\r\n");
			dos.writeBytes("\r\n");
		} catch (IOException ioe) {
			showMessage(ioe.toString(), true);
			return 0;
		}

		return nSaved;
	}

	protected void putGeometry(Geometry geom, Color lineColor, Color fillColor, int lineWidth, int transparency, DataOutputStream dos, boolean toKml) throws IOException {
		if (geom == null)
			return;
		if (geom instanceof RealPoint) {
			putPoint((RealPoint) geom, lineColor, fillColor, dos, toKml);
		} else if (geom instanceof RealCircle) {
			putLineOrArea(((RealCircle) geom).getPolygon(30), lineColor, fillColor, lineWidth, transparency, dos, toKml);
		} else if (geom instanceof RealPolyline) {
			putLineOrArea((RealPolyline) geom, lineColor, fillColor, lineWidth, transparency, dos, toKml);
		} else if (geom instanceof MultiGeometry) {
			MultiGeometry mgeo = (MultiGeometry) geom;
			for (int i = 0; i < mgeo.getPartsCount(); i++) {
				putGeometry(mgeo.getPart(i), lineColor, fillColor, lineWidth, transparency, dos, toKml);
			}
		}
	}

	protected void putPoint(RealPoint p, Color lineColor, Color fillColor, DataOutputStream dos, boolean toKml) throws IOException {
		if (toKml) {
			putPointToKml(p, lineColor, fillColor, dos);
		} else {
			putPointToHtml(p, lineColor, fillColor, dos);
		}
	}

	protected void putPointToKml(RealPoint p, Color lineColor, Color fillColor, DataOutputStream dos) throws IOException {
		dos.writeBytes("  <Placemark>\r\n");
		dos.writeBytes("    <Point>\r\n");
		dos.writeBytes("      <coordinates>" + p.x + "," + p.y + ",0</coordinates>\r\n");
		dos.writeBytes("    </Point>\r\n");
		dos.writeBytes("    <Style>\r\n");
		dos.writeBytes("      <IconStyle>\r\n");
		dos.writeBytes("        <Icon><href>http://maps.google.com/mapfiles/kml/pal4/icon24.png</href></Icon> \r\n");
		if (fillColor == null) {
			dos.writeBytes("        <color>ff00ffff</color>\r\n");
		} else {
			dos.writeBytes("        <color>ff" + StringUtil.padString(Integer.toHexString(fillColor.getBlue()), '0', 2, true) + StringUtil.padString(Integer.toHexString(fillColor.getGreen()), '0', 2, true)
					+ StringUtil.padString(Integer.toHexString(fillColor.getRed()), '0', 2, true) + "</color>\r\n");
		}
		dos.writeBytes("      </IconStyle>\r\n");
		dos.writeBytes("    </Style>\r\n");
		dos.writeBytes("  </Placemark>\r\n");
		dos.writeBytes("\r\n");
	}

	protected void putPointToHtml(RealPoint p, Color lineColor, Color fillColor, DataOutputStream dos) throws IOException {
		dos.writeBytes("  map.addOverlay(new GMarker(new GLatLng(" + p.y + "," + p.x + "),{icon:myIcon}));\r\n");
	}

	protected void putLineOrArea(RealPolyline p, Color lineColor, Color fillColor, int lineWidth, int transparency, DataOutputStream dos, boolean toKml) throws IOException {
		if (toKml) {
			putLineOrAreaToKml(p, lineColor, fillColor, lineWidth, transparency, dos);
		} else {
			putLineOrAreaToHtml(p, lineColor, fillColor, lineWidth, transparency, dos);
		}
	}

	protected void putLineOrAreaToKml(RealPolyline p, Color lineColor, Color fillColor, int lineWidth, int transparency, DataOutputStream dos) throws IOException {
		if (p.getIsClosed() && fillColor != null) {
			dos.writeBytes("  <Placemark>\r\n");
			dos.writeBytes("    <name>fill</name>\r\n");
			dos.writeBytes("    <Polygon>\r\n");
			dos.writeBytes("      <outerBoundaryIs>\r\n");
			dos.writeBytes("        <LinearRing>\r\n");
			dos.writeBytes("          <coordinates>\r\n");
			for (RealPoint element : p.p) {
				dos.writeBytes("            " + element.x + "," + element.y + ",0\r\n");
			}
			dos.writeBytes("          </coordinates>\r\n");
			dos.writeBytes("        </LinearRing>\r\n");
			dos.writeBytes("      </outerBoundaryIs>\r\n");
			dos.writeBytes("    </Polygon>\r\n");
			dos.writeBytes("    <Style>\r\n");
			dos.writeBytes("      <PolyStyle>\r\n");
			dos.writeBytes("         <color>" + StringUtil.padString(Integer.toHexString(transparency), '0', 2, true) + StringUtil.padString(Integer.toHexString(fillColor.getBlue()), '0', 2, true)
					+ StringUtil.padString(Integer.toHexString(fillColor.getGreen()), '0', 2, true) + StringUtil.padString(Integer.toHexString(fillColor.getRed()), '0', 2, true) + "</color>\r\n");
			dos.writeBytes("      </PolyStyle>\r\n");
			dos.writeBytes("    </Style>\r\n");
			dos.writeBytes("  </Placemark>\r\n");
			dos.writeBytes("\r\n");
		}
		dos.writeBytes("  <Placemark>\r\n");
		dos.writeBytes("    <name>line</name>\r\n");
		dos.writeBytes("    <LineString>\r\n");
		dos.writeBytes("      <coordinates>\r\n");
		for (RealPoint element : p.p) {
			dos.writeBytes("        " + element.x + "," + element.y + ",0\r\n");
		}
		dos.writeBytes("      </coordinates>\r\n");
		dos.writeBytes("    </LineString>\r\n");
		dos.writeBytes("    <Style>\r\n");
		dos.writeBytes("      <LineStyle>\r\n");
		dos.writeBytes("        <color>" + StringUtil.padString(Integer.toHexString(transparency), '0', 2, true) + StringUtil.padString(Integer.toHexString(lineColor.getBlue()), '0', 2, true)
				+ StringUtil.padString(Integer.toHexString(lineColor.getGreen()), '0', 2, true) + StringUtil.padString(Integer.toHexString(lineColor.getRed()), '0', 2, true) + "</color>\r\n");
		dos.writeBytes("        <width>" + lineWidth + "</width>\r\n");
		dos.writeBytes("      </LineStyle>\r\n");
		dos.writeBytes("    </Style>\r\n");
		dos.writeBytes("  </Placemark>\r\n");
		dos.writeBytes("\r\n");
	}

	protected void putLineOrAreaToHtml(RealPolyline p, Color lineColor, Color fillColor, int lineWidth, int transparency, DataOutputStream dos) throws IOException {
		if (p.getIsClosed() && fillColor != null) {
			dos.writeBytes("  polyline = new GPolygon([\r\n");
		} else {
			dos.writeBytes("  polyline = new GPolyline([\r\n");
		}
		for (int i = 0; i < p.p.length; i++) {
			dos.writeBytes("    new GLatLng(" + p.p[i].y + "," + p.p[i].x + ")");
			if (i < p.p.length - 1) {
				dos.writeBytes(",");
			}
			dos.writeBytes("\r\n");
		}
		String colorLine = StringUtil.padString(Integer.toHexString(lineColor.getRed()), '0', 2, true) + StringUtil.padString(Integer.toHexString(lineColor.getGreen()), '0', 2, true)
				+ StringUtil.padString(Integer.toHexString(lineColor.getBlue()), '0', 2, true);
		if (p.getIsClosed() && fillColor != null) {
			String colorFill = StringUtil.padString(Integer.toHexString(fillColor.getRed()), '0', 2, true) + StringUtil.padString(Integer.toHexString(fillColor.getGreen()), '0', 2, true)
					+ StringUtil.padString(Integer.toHexString(fillColor.getBlue()), '0', 2, true);
			dos.writeBytes("  ], \"#" + colorLine + "\", " + lineWidth + "," + (transparency / 255f) + ",\"#" + colorFill + "\"," + (transparency / 255f) + ");\r\n");
		} else {
			dos.writeBytes("  ], \"#" + colorLine + "\", " + lineWidth + ");\r\n");
		}
		dos.writeBytes("  map.addOverlay(polyline);\r\n");
	}

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
	}
}
