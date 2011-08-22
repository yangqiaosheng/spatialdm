package spade.analysis.tools.moves;

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

import spade.analysis.classification.ObjectColorer;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.DataAnalyser;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.CopyFile;
import spade.lib.util.StringUtil;
import spade.time.Date;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.vis.action.Highlighter;
import spade.vis.database.AttributeTypes;
import spade.vis.database.SpatialEntity;
import spade.vis.database.ThematicDataItem;
import spade.vis.database.TimeFilter;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DMovingObject;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapViewer;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 20-Aug-2007
 * Time: 15:56:42
 * Exports selected trajectories to KML (GoogleEarth)
 */
public class TrajectoriesToKML implements DataAnalyser {
	protected ESDACore core = null;
	/**
	* Remembers the last directory where KML files were saved
	*/
	protected static String lastDir = null;

	/**
	 * Returns true when the tool has everything necessary for its operation.
	 * For example, if the tool manages a number of analysis methods, it should
	 * check whether the class for at least one method is available.
	 * A TrajectoriesGeneraliser always returns true.
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
		Vector moveLayers = new Vector(lman.getLayerCount(), 1);
		boolean geo = false, someObjectsSelected = false;
		for (int i = 0; i < lman.getLayerCount(); i++) {
			GeoLayer layer = lman.getGeoLayer(i);
			if ((layer instanceof DGeoLayer) && layer.getObjectCount() > 0 && (layer.getObjectAt(0) instanceof DMovingObject)) {
				moveLayers.addElement(layer);
				geo = geo || layer.isGeographic();
				if (!someObjectsSelected) {
					Highlighter hl = core.getHighlighterForContainer(layer.getContainerIdentifier());
					if (hl != null) {
						Vector sel = hl.getSelectedObjects();
						someObjectsSelected = sel != null && sel.size() > 0;
					}
				}
			}
		}
		if (moveLayers.size() < 1) {
			showMessage("No layers with trajectories found!", true);
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
		mainP.add(new Label("Select the layer with trajectories to view in Google*:"));
		List list = new List(Math.max(moveLayers.size() + 1, 5));
		for (int i = 0; i < moveLayers.size(); i++) {
			list.add(((DGeoLayer) moveLayers.elementAt(i)).getName());
		}
		list.select(0);
		mainP.add(list);
		Checkbox cbSel = null;
		if (someObjectsSelected) {
			cbSel = new Checkbox("export only selected trajectories", true);
			mainP.add(cbSel);
		}
		mainP.add(new Line(false));
		mainP.add(new Label("Information content in GEarth:"));
		p = new Panel(new FlowLayout(FlowLayout.LEFT));
		Checkbox cbGEInfoContent[] = new Checkbox[3];
		p.add(cbGEInfoContent[0] = new Checkbox("points", true));
		p.add(cbGEInfoContent[1] = new Checkbox("ground lines", true));
		p.add(cbGEInfoContent[2] = new Checkbox("sky lines", true));
		mainP.add(p);
		mainP.add(new Line(false));
		mainP.add(new Label("Align times of trajectories:"));
		p = new Panel(new FlowLayout(FlowLayout.LEFT));
		CheckboxGroup cbgalign = new CheckboxGroup();
		Checkbox cbAlignStartTimes[] = new Checkbox[3];
		p.add(cbAlignStartTimes[0] = new Checkbox("no alignment", true, cbgalign));
		p.add(cbAlignStartTimes[1] = new Checkbox("align starts", false, cbgalign));
		p.add(cbAlignStartTimes[2] = new Checkbox("align ends", false, cbgalign));
		mainP.add(p);
		p = new Panel(new FlowLayout(FlowLayout.LEFT));
		p.add(new Label("Max altitude:"));
		TextField tfAltitude = new TextField("1000", 8);
		p.add(tfAltitude);
		mainP.add(p);
		mainP.add(new Line(false));
		mainP.add(new Label("Name to appear in Google:"));
		TextField tfName = new TextField("Data");
		mainP.add(tfName);
		mainP.add(new Line(false));
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Export parameters", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return;
		int idx = list.getSelectedIndex();
		if (idx < 0)
			return;
		DGeoLayer moveLayer = (DGeoLayer) moveLayers.elementAt(idx);
		Vector selected = null;
		if (cbSel != null && cbSel.getState()) {
			Highlighter hl = core.getHighlighterForSet(moveLayer.getEntitySetIdentifier());
			if (hl != null) {
				selected = hl.getSelectedObjects();
			}
			if (selected == null || selected.size() < 1)
				if (!Dialogs.askYesOrNo(core.getUI().getMainFrame(), "None of the trajectories " + "have been selected in this layer. Export all active trajectories?", "No selected trajectories!"))
					return;
		}
		int attrN = -1;
		float minV = 0f, maxV = 100f;
		if (cbDest[0].getState()) {
			DMovingObject mobj = (DMovingObject) moveLayer.getObject(0);
			Vector track = mobj.getTrack();
			SpatialEntity se = (SpatialEntity) track.elementAt(0);
			ThematicDataItem tdi = se.getThematicData();
			if (tdi != null) {
				cbg = new CheckboxGroup();
				int na = tdi.getAttrCount();
				if (na > 0) {
					p = new Panel();
					p.setLayout(new ColumnLayout());
					Checkbox cbAttr[] = new Checkbox[1 + na];
					p.add(cbAttr[0] = new Checkbox("Time", true, cbg));
					for (int i = 0; i < na; i++) {
						p.add(cbAttr[1 + i] = new Checkbox(tdi.getAttributeName(i), false, cbg));
						cbAttr[1 + i].setEnabled(AttributeTypes.isNumericType(tdi.getAttrType(i)));
					}
					p.add(new Line(false));
					dia = new OKDialog(core.getUI().getMainFrame(), "Select attribute for Z coordinate", true);
					dia.addContent(p);
					dia.show();
					if (dia.wasCancelled())
						return;
					for (int i = 0; i < cbAttr.length; i++)
						if (cbAttr[i].getState()) {
							attrN = i - 1;
							break;
						}
					if (attrN >= 0) { // set min and max values to be mapped to altitude
						minV = Float.NaN;
						maxV = Float.NaN;
						float sum = 0f;
						int n = 0;
						for (int i = 0; i < moveLayer.getObjectCount(); i++)
							if (moveLayer.getObject(i) instanceof DMovingObject) {
								mobj = (DMovingObject) moveLayer.getObject(i);
								if (selected != null && !selected.contains(mobj.getIdentifier())) {
									continue;
								}
								if (!moveLayer.isObjectActive(i)) {
									continue;
								}
								Vector vTrack = mobj.getTrack();
								if (vTrack != null) {
									for (int j = 0; j < vTrack.size(); j++) {
										SpatialEntity spe = (SpatialEntity) vTrack.elementAt(j);
										tdi = spe.getThematicData();
										if (tdi != null) {
											double v = tdi.getNumericAttrValue(attrN);
											if (Float.isNaN(minV) || minV > v) {
												minV = (float) v;
											}
											if (Float.isNaN(maxV) || maxV < v) {
												maxV = (float) v;
											}
											sum += v;
											n++;
										}
									}
								}
							}
						if (n > 0) {
							sum /= n;
							Panel pp;
							TextField tfMin = null, tfMax = null;
							p = new Panel(new ColumnLayout());
							p.add(new Label("Modify attribute range:", Label.CENTER));
							pp = new Panel(new FlowLayout(FlowLayout.LEFT));
							pp.add(new Label("min:"));
							pp.add(tfMin = new TextField("" + minV));
							p.add(pp);
							pp = new Panel(new FlowLayout(FlowLayout.LEFT));
							pp.add(new Label("max:"));
							pp.add(tfMax = new TextField("" + maxV));
							p.add(pp);
							p.add(new Label("avg=" + sum));
							dia = new OKDialog(core.getUI().getMainFrame(), "Specify attribute range", true);
							dia.addContent(p);
							dia.show();
							if (dia.wasCancelled())
								return;
							try {
								minV = Float.valueOf(tfMin.getText()).floatValue();
							} catch (NumberFormatException nfe) {
								minV = 0f;
							}
							try {
								maxV = Float.valueOf(tfMax.getText()).floatValue();
							} catch (NumberFormatException nfe) {
								maxV = 100f;
							}
							if (maxV <= minV) {
								minV = 0f;
								maxV = 100f;
							}
						} else {
							minV = 0f;
							maxV = 100f;
						}
					}
				}
			}
		}
		boolean bGEInfoContent[] = new boolean[cbGEInfoContent.length];
		for (int i = 0; i < cbGEInfoContent.length; i++) {
			bGEInfoContent[i] = cbGEInfoContent[i].getState();
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
		int result = 0;
		if (out != null) {
			if (cbDest[0].getState()) {
				int alt = 1000;
				try {
					alt = Integer.valueOf(tfAltitude.getText()).intValue();
				} catch (NumberFormatException nfe) {
				}
				;
				int alignStartTimes = 0;
				if (cbAlignStartTimes[1].getState()) {
					alignStartTimes = 1;
				}
				if (cbAlignStartTimes[2].getState()) {
					alignStartTimes = 2;
				}
				result = exportTrajectoriesToGEarth(moveLayer, selected, out, bGEInfoContent, alignStartTimes, alt, minV, maxV, attrN, tfName.getText());
			} else {
				result = exportTrajectoriesToGMap(moveLayer, selected, out, tfName.getText());
			}
			try {
				out.close();
			} catch (IOException e) {
			}
		}
		if (result > 0) {
			showMessage(result + " trajectories have been exported to file " + lastDir + filename, false);
		}
	}

	/**
	 * @return the number of exported trajectories
	 */
	protected int exportTrajectoriesToGMap(DGeoLayer moveLayer, Vector selected, OutputStream stream, String name) {
		if (moveLayer == null || stream == null || moveLayer.getObjectCount() < 1)
			return 0;
		if (selected != null && selected.size() < 1) {
			selected = null;
		}
		TimeFilter tf = moveLayer.getTimeFilter();
		TimeMoment t1 = null, t2 = null;
		boolean timeFiltered = false;
		if (tf != null) {
			t1 = tf.getFilterPeriodStart();
			if (t1 != null) {
				t2 = tf.getFilterPeriodEnd();
				timeFiltered = true;
			}
		}
		float minx = Float.NaN, miny = Float.NaN, maxx = Float.NaN, maxy = Float.NaN;
		Vector selTrajectories = new Vector((selected == null) ? 100 : selected.size(), 100);
		for (int i = 0; i < moveLayer.getObjectCount(); i++)
			if (moveLayer.getObject(i) instanceof DMovingObject) {
				DMovingObject mobj = (DMovingObject) moveLayer.getObject(i);
				if (selected != null && !selected.contains(mobj.getIdentifier())) {
					continue;
				}
				if (!moveLayer.isObjectActive(i)) {
					continue;
				}
				if (timeFiltered) {
					mobj = (DMovingObject) mobj.getObjectVersionForTimeInterval(t1, t2);
					if (mobj == null) {
						continue;
					}
				}
				RealRectangle rr = mobj.getBounds();
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
				Vector track = mobj.getTrack();
				if (track == null || track.size() < 1) {
					continue;
				}
				selTrajectories.addElement(mobj);
			}
		if (selTrajectories.size() < 1) {
			showMessage("No suitable trajectories found!", true);
			return 0;
		}

		ObjectColorer trajColorer = null;
		if (moveLayer.getVisualizer() != null) {
			trajColorer = moveLayer.getVisualizer().getObjectColorer();
		}
		if (trajColorer == null && moveLayer.getBackgroundVisualizer() != null) {
			trajColorer = moveLayer.getBackgroundVisualizer().getObjectColorer();
		}

		DataOutputStream dos = new DataOutputStream(stream);
		int nSaved = 0;

		try {
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
			dos.writeBytes("  var startIcon = new GIcon();\r\n");
			dos.writeBytes("  startIcon.image = \"http://www.google.com/mapfiles/dd-start.png\";\r\n");
			dos.writeBytes("  startIcon.iconSize = new GSize(20, 34);\r\n");
			dos.writeBytes("  startIcon.iconAnchor = new GPoint(10, 34);\r\n");
			dos.writeBytes("  var endIcon = new GIcon();\r\n");
			dos.writeBytes("  endIcon.image = \"http://www.google.com/mapfiles/dd-end.png\";\r\n");
			dos.writeBytes("  endIcon.iconSize = new GSize(20, 34);\r\n");
			dos.writeBytes("  endIcon.iconAnchor = new GPoint(10, 34);\r\n");
			dos.writeBytes("  var polyline;\r\n");
			for (int i = 0; i < selTrajectories.size(); i++) {
				DMovingObject mobj = (DMovingObject) selTrajectories.elementAt(i);
				Vector track = mobj.getTrack();
				Color color = null; //the color of this trajectory
				if (trajColorer != null) {
					color = trajColorer.getColorForObject(mobj.getIdentifier());
				}
				if (color == null) {
					DrawingParameters dp = moveLayer.getDrawingParameters();
					if (dp != null) {
						color = dp.lineColor;
					}
				}
				if (color == null) {
					color = Color.BLUE;
				}
				int colorR = color.getRed(), colorG = color.getGreen(), colorB = color.getBlue();
				dos.writeBytes("  polyline = new GPolyline([\r\n");
				for (int j = 0; j < track.size(); j++) {
					SpatialEntity spe = (SpatialEntity) track.elementAt(j);
					RealPoint p = spe.getCentre();
					dos.writeBytes("    new GLatLng(" + p.y + "," + p.x + ")");
					if (j < track.size() - 1) {
						dos.writeBytes(",");
					}
					dos.writeBytes("\r\n");
				}
				String colorStr = StringUtil.padString(Integer.toHexString(colorR), '0', 2, true) + StringUtil.padString(Integer.toHexString(colorG), '0', 2, true) + StringUtil.padString(Integer.toHexString(colorB), '0', 2, true);
				dos.writeBytes("  ], \"#" + colorStr + "\", 4);\r\n");
				dos.writeBytes("  map.addOverlay(polyline);\r\n");
				SpatialEntity spe = (SpatialEntity) track.elementAt(0);
				RealPoint p = spe.getCentre();
				dos.writeBytes("  map.addOverlay(new GMarker(new GLatLng(" + p.y + "," + p.x + "),{icon:startIcon}));\r\n");
				spe = (SpatialEntity) track.elementAt(track.size() - 1);
				p = spe.getCentre();
				dos.writeBytes("  map.addOverlay(new GMarker(new GLatLng(" + p.y + "," + p.x + "),{icon:endIcon}));\r\n");
				nSaved++;
			}
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
			dos.writeBytes("\r\n");
		} catch (IOException ioe) {
			showMessage(ioe.toString(), true);
			return 0;
		}
		if (nSaved < 1) {
			showMessage("No suitable trajectories found!", true);
			return 0;
		}
		return nSaved;
	}

	/**
	 * @return the number of exported trajectories
	 */
	protected int exportTrajectoriesToGEarth(DGeoLayer moveLayer, Vector selected, OutputStream stream, boolean bGEInfoContent[], int alignStartTimes, int altitude, float minV, float maxV, int attrN, String name)
	// attrN: -1 for time; >=0 for the attribute N to be used for Z coordinate
	{
		if (moveLayer == null || stream == null || moveLayer.getObjectCount() < 1)
			return 0;
		if (selected != null && selected.size() < 1) {
			selected = null;
		}
		TimeFilter tf = moveLayer.getTimeFilter();
		TimeMoment t1 = null, t2 = null;
		boolean timeFiltered = false;
		if (tf != null) {
			t1 = tf.getFilterPeriodStart();
			if (t1 != null) {
				t2 = tf.getFilterPeriodEnd();
				timeFiltered = true;
			}
		}
		TimeMoment minTime = null, maxTime = null;
		float minx = Float.NaN, miny = Float.NaN, maxx = Float.NaN, maxy = Float.NaN;
		long maxDuration = 0;
		Vector selTrajectories = new Vector((selected == null) ? 100 : selected.size(), 100);
		for (int i = 0; i < moveLayer.getObjectCount(); i++)
			if (moveLayer.getObject(i) instanceof DMovingObject) {
				DMovingObject mobj = (DMovingObject) moveLayer.getObject(i);
				if (selected != null && !selected.contains(mobj.getIdentifier())) {
					continue;
				}
				if (!moveLayer.isObjectActive(i)) {
					continue;
				}
				if (timeFiltered) {
					mobj = (DMovingObject) mobj.getObjectVersionForTimeInterval(t1, t2);
					if (mobj == null) {
						continue;
					}
				}
				RealRectangle rr = mobj.getBounds();
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
				Vector track = mobj.getTrack();
				if (track == null || track.size() < 1) {
					continue;
				}
				selTrajectories.addElement(mobj);
				SpatialEntity spe = (SpatialEntity) track.elementAt(0);
				TimeReference tref = spe.getTimeReference();
				if (minTime == null || minTime.compareTo(tref.getValidFrom()) > 0) {
					minTime = tref.getValidFrom();
				}
				spe = (SpatialEntity) track.elementAt(track.size() - 1);
				TimeReference trefmax = spe.getTimeReference();
				if (maxTime == null || maxTime.compareTo(trefmax.getValidUntil()) < 0) {
					maxTime = trefmax.getValidUntil();
				}
				long dur = trefmax.getValidUntil().subtract(tref.getValidFrom());
				if (dur > maxDuration) {
					maxDuration = dur;
				}
			}
		if (selTrajectories.size() < 1) {
			showMessage("No suitable trajectories found!", true);
			return 0;
		}
		if (minTime == null || maxTime == null) {
			showMessage("No time references found!", true);
			return 0;
		}
		long timeDiff = (alignStartTimes > 0) ? maxDuration : maxTime.subtract(minTime);
		if (timeDiff <= 0) {
			showMessage("Invalid time interval: zero length!", true);
			return 0;
		}

		ObjectColorer trajColorer = null;
		if (moveLayer.getVisualizer() != null) {
			trajColorer = moveLayer.getVisualizer().getObjectColorer();
		}
		if (trajColorer == null && moveLayer.getBackgroundVisualizer() != null) {
			trajColorer = moveLayer.getBackgroundVisualizer().getObjectColorer();
		}

		DataOutputStream dos = new DataOutputStream(stream);
		int nSaved = 0;
		float maxHeight = altitude;
		try {
			dos.writeBytes("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
			dos.writeBytes("<kml xmlns=\"http://earth.google.com/kml/2.1\">\r\n");
			dos.writeBytes("<Document>\r\n");
			dos.writeBytes("<name>" + name + "</name>\r\n");
			//dos.writeBytes("<description>Truck routes</description>\r\n");
			dos.writeBytes("<Style id=\"startIcon\"><IconStyle>\r\n");
			dos.writeBytes("  <Icon><href>http://maps.google.com/mapfiles/kml/pal4/icon60.png</href></Icon>\r\n");
			dos.writeBytes("  <scale>0.5</scale> \r\n");
			dos.writeBytes("<LineStyle> <color>77bbbbbb</color> </LineStyle>\r\n");
			dos.writeBytes("</IconStyle>\r\n");
			dos.writeBytes("</Style>\r\n");
			dos.writeBytes("<Style id=\"pointIcon\"><IconStyle>\r\n");
			dos.writeBytes("  <Icon><href>http://maps.google.com/mapfiles/kml/pal4/icon24.png</href></Icon> \r\n");
			dos.writeBytes("  <scale>0.1</scale> \r\n");
			dos.writeBytes("</IconStyle>\r\n");
			dos.writeBytes("<LineStyle> <color>77bbbbbb</color> </LineStyle>\r\n");
			dos.writeBytes("</Style>\r\n");
			dos.writeBytes("<Style id=\"endIcon\"><IconStyle>\r\n");
			dos.writeBytes("  <Icon><href>http://maps.google.com/mapfiles/kml/pal4/icon52.png</href></Icon>\r\n");
			dos.writeBytes("  <scale>0.5</scale> \r\n");
			dos.writeBytes("</IconStyle>\r\n");
			dos.writeBytes("<LineStyle> <color>77bbbbbb</color> </LineStyle>\r\n");
			dos.writeBytes("</Style>\r\n\r\n");
			dos.writeBytes("\r\n");

			dos.writeBytes("<LookAt>\r\n");
			dos.writeBytes("  <longitude>" + (minx + maxx) / 2 + "</longitude>\r\n");
			dos.writeBytes("  <latitude>" + (miny + maxy) / 2 + "</latitude>\r\n");
			dos.writeBytes("  <altitude>0</altitude>\r\n");
			dos.writeBytes("  <range>10000</range>\r\n");
			dos.writeBytes("  <tilt>70</tilt>\r\n");
			dos.writeBytes("  <heading>0</heading>\r\n");
			dos.writeBytes("</LookAt>\r\n\r\n");

			if (bGEInfoContent[0]) {
				for (int i = 0; i < selTrajectories.size(); i++) {

					DMovingObject mobj = (DMovingObject) selTrajectories.elementAt(i);
					Vector track = mobj.getTrack();
					dos.writeBytes("<Folder id=\"Trajectory " + mobj.getIdentifier() + "\">\r\n");
					dos.writeBytes("<name>Trajectory " + mobj.getIdentifier() + "; N positions = " + track.size() + "</name>\r\n");

					dos.writeBytes("<Folder id=\"All points\">\r\n");
					dos.writeBytes("<name>All points</name>\r\n");
					dos.writeBytes("\r\n");

					SpatialEntity spe = (SpatialEntity) track.elementAt(0);
					TimeReference tref = spe.getTimeReference();
					TimeMoment minTimeTr = tref.getValidFrom();
					spe = (SpatialEntity) track.elementAt(track.size() - 1);
					tref = spe.getTimeReference();
					TimeMoment maxTimeTr = tref.getValidUntil();
					long timeDiffTr = maxTimeTr.subtract(minTimeTr);

					Color color = null; //the color of this trajectory
					if (trajColorer != null) {
						color = trajColorer.getColorForObject(mobj.getIdentifier());
					}
					if (color == null) {
						DrawingParameters dp = moveLayer.getDrawingParameters();
						if (dp != null) {
							color = dp.lineColor;
						}
					}
					if (color == null) {
						color = Color.BLUE;
					}
					int colorR = color.getRed(), colorG = color.getGreen(), colorB = color.getBlue(), colorDR = color.darker().getRed(), colorDG = color.darker().getGreen(), colorDB = color.darker().getBlue();

					Vector genTrack = mobj.getGeneralisedTrack();
					if (genTrack == null) {
						genTrack = track;
					}

					for (int j = 0; j < genTrack.size(); j++) {

						if (j == 1 && genTrack.size() > 2) {
							dos.writeBytes("<Folder id=\"waypoints, trajectory " + mobj.getIdentifier() + "\">\r\n");
							dos.writeBytes("<name>waypoints, trajectory " + mobj.getIdentifier() + "</name>\r\n");
							dos.writeBytes("\r\n");
						}
						if (j == genTrack.size() - 1 && genTrack.size() > 2) {
							dos.writeBytes("</Folder>\r\n");
							dos.writeBytes("\r\n");
						}
						spe = (SpatialEntity) genTrack.elementAt(j);
						RealPoint p = spe.getCentre();
						tref = spe.getTimeReference();
						TimeMoment t = tref.getValidFrom();
						long height = 0;
						String strValue = null;
						if (attrN == -1) {
							switch (alignStartTimes) {
							case 0:
								height = Math.round(t.subtract(minTime) * maxHeight / timeDiff);
								break;
							case 1:
								height = Math.round(t.subtract(minTimeTr) * maxHeight / timeDiff);
								break;
							case 2:
								height = Math.round(((int) (timeDiff - timeDiffTr) + t.subtract(minTimeTr)) * maxHeight / timeDiff);
								break;
							}
						} else {
							ThematicDataItem tdi = spe.getThematicData();
							if (tdi != null) {
								double v = tdi.getNumericAttrValue(attrN);
								strValue = ", Value=" + StringUtil.doubleToStr(v, minV, maxV);
								height = Math.round((v - minV) * maxHeight / (maxV - minV));
							}
						}
						long relTime = t.subtract(minTimeTr);
						String dateStr = null, dateStrHuman = null;
						if (t instanceof Date) { //spade.time.Date
							Date d = (Date) t;
							String sch = d.scheme;
							d.scheme = "yyyy-mm-ddThh:tt:ssZ";
							dateStr = d.toString();
							d.scheme = "dd-mm-yyyy, hh:tt:ss";
							dateStrHuman = d.toString();
							d.scheme = sch;
						}
						dos.writeBytes("<Placemark>\r\n");
						if (j == 0) {
							dos.writeBytes("  <styleUrl>#startIcon</styleUrl>\r\n");
						} else if (j == genTrack.size() - 1) {
							dos.writeBytes("  <styleUrl>#endIcon</styleUrl>\r\n");
						} else {
							dos.writeBytes("  <styleUrl>#pointIcon</styleUrl>\r\n");
						}

						dos.writeBytes("  <description>T" + mobj.getIdentifier() + "." + j + "; " + dateStrHuman + "; " + relTime + ((strValue == null) ? "" : strValue) + "</description>\r\n");
						/* does not work; described: http://code.google.com/apis/kml/documentation/extendeddata.html
						dos.writeBytes("  <ExtendedData>\r\n");
						dos.writeBytes("    <Data name=\"Trajectory\"><value>"+mobj.getIdentifier()+"</value></Data>\r\n");
						dos.writeBytes("    <Data name=\"N\"><value>"+j+"</value></Data>\r\n");
						dos.writeBytes("    <Data name=\"time\"><value>"+dateStrHuman+"</value></Data>\r\n");
						dos.writeBytes("  </ExtendedData>\r\n");
						*/
						dos.writeBytes("  <Point id=\"T" + mobj.getIdentifier() + "." + j + "\">\r\n");
						dos.writeBytes("    <altitudeMode>relativeToGround</altitudeMode><extrude>1</extrude>\r\n");
						dos.writeBytes("    <coordinates>" + p.x + "," + p.y + "," + height + "</coordinates>\r\n");
						dos.writeBytes("  </Point>\r\n");
						dos.writeBytes("  <TimeStamp><when>" + dateStr + "</when></TimeStamp>\r\n");
						dos.writeBytes("</Placemark>\r\n");
						dos.writeBytes("\r\n");
					}

					dos.writeBytes("</Folder>\r\n");
					dos.writeBytes("\r\n");

					for (int k = 1; k <= 2; k++) {
						if (!bGEInfoContent[k]) {
							continue;
						}
						if (k == 1) {
							dos.writeBytes("<Folder id=\"Ground line\">\r\n");
							dos.writeBytes("<name>Ground line</name>\r\n");
						} else {
							dos.writeBytes("<Folder id=\"Sky line\">\r\n");
							dos.writeBytes("<name>Sky line</name>\r\n");
						}
						dos.writeBytes("\r\n");

						spe = (SpatialEntity) track.elementAt(0);
						RealPoint p = spe.getCentre();
						long prevHeight = 0;
						if (k == 2) {
							tref = spe.getTimeReference();
							TimeMoment t = tref.getValidFrom();
							prevHeight = 0;
							if (attrN == -1) {
								switch (alignStartTimes) {
								case 0:
									prevHeight = Math.round(t.subtract(minTime) * maxHeight / timeDiff);
									break;
								case 1:
									prevHeight = Math.round(t.subtract(minTimeTr) * maxHeight / timeDiff);
									break;
								case 2:
									prevHeight = Math.round(((int) (timeDiff - timeDiffTr) + t.subtract(minTimeTr)) * maxHeight / timeDiff);
									break;
								}
							} else {
								ThematicDataItem tdi = spe.getThematicData();
								if (tdi != null) {
									double v = tdi.getNumericAttrValue(attrN);
									prevHeight = Math.round((v - minV) * maxHeight / (maxV - minV));
								}
							}
						}
						float prevX = p.x, prevY = p.y;
						for (int j = 1; j < track.size(); j++) {
							spe = (SpatialEntity) track.elementAt(j);
							p = spe.getCentre();
							tref = spe.getTimeReference();
							TimeMoment t = tref.getValidFrom();
							int alpha = (timeDiffTr == 0) ? 255 : 63 + Math.round(192 * t.subtract(minTimeTr) / timeDiffTr), width = (timeDiffTr == 0) ? 4 : 6 - Math.round(4 * t.subtract(minTimeTr) / timeDiffTr);
							long height = 0;
							if (k == 2)
								if (attrN == -1) {
									switch (alignStartTimes) {
									case 0:
										height = Math.round(t.subtract(minTime) * maxHeight / timeDiff);
										break;
									case 1:
										height = Math.round(t.subtract(minTimeTr) * maxHeight / timeDiff);
										break;
									case 2:
										height = Math.round(((int) (timeDiff - timeDiffTr) + t.subtract(minTimeTr)) * maxHeight / timeDiff);
										break;
									}
								} else {
									ThematicDataItem tdi = spe.getThematicData();
									if (tdi != null) {
										double v = tdi.getNumericAttrValue(attrN);
										height = Math.round((v - minV) * maxHeight / (maxV - minV));
									}
								}
							/*
							switch (alignStartTimes) {
							  case 0:
							    height=Math.round(t.subtract(minTime)*maxHeight/timeDiff); break;
							  case 1:
							    height=Math.round(t.subtract(minTimeTr)*maxHeight/timeDiff); break;
							  case 2:
							    height=Math.round(((int)(timeDiff-timeDiffTr)+t.subtract(minTimeTr))*maxHeight/timeDiff); break;
							}
							*/

							String dateStr = null;
							if (t instanceof Date) { //spade.time.Date
								Date d = (Date) t;
								String sch = d.scheme;
								d.scheme = "yyyy-mm-ddThh:tt:ssZ";
								dateStr = d.toString();
								d.scheme = sch;
							}
							dos.writeBytes("<Placemark>\r\n");
							dos.writeBytes("  <LineString>\r\n");
							if (k == 1) {
								dos.writeBytes("    <altitudeMode>clampedToGround</altitudeMode><tessellate>1</tessellate>\r\n");
							} else {
								dos.writeBytes("    <altitudeMode>relativeToGround</altitudeMode><tessellate>0</tessellate>\r\n");
							}
							dos.writeBytes("    <coordinates> " + prevX + "," + prevY + "," + prevHeight + " " + p.x + "," + p.y + "," + height + "\r\n");
							dos.writeBytes("    </coordinates>\r\n");
							dos.writeBytes("  </LineString>\r\n");
							dos.writeBytes("  <Style>\r\n");
							dos.writeBytes("    <LineStyle>\r\n");
							if (k == 1) {
								dos.writeBytes("      <color>" + StringUtil.padString(Integer.toHexString(alpha), '0', 2, true) + StringUtil.padString(Integer.toHexString(colorDB), '0', 2, true)
										+ StringUtil.padString(Integer.toHexString(colorDG), '0', 2, true) + StringUtil.padString(Integer.toHexString(colorDR), '0', 2, true) + "</color>\r\n");
							} else {
								dos.writeBytes("      <color>" + StringUtil.padString(Integer.toHexString(alpha), '0', 2, true) + StringUtil.padString(Integer.toHexString(colorB), '0', 2, true)
										+ StringUtil.padString(Integer.toHexString(colorG), '0', 2, true) + StringUtil.padString(Integer.toHexString(colorR), '0', 2, true) + "</color>\r\n");
							}
							dos.writeBytes("      <width>" + width + "</width>\r\n");
							dos.writeBytes("    </LineStyle>\r\n");
							dos.writeBytes("  </Style>\r\n");
							dos.writeBytes("  <TimeStamp><when>" + dateStr + "</when></TimeStamp>\r\n");
							dos.writeBytes("</Placemark>\r\n");
							prevHeight = height;
							prevX = p.x;
							prevY = p.y;
						}

						dos.writeBytes("\r\n");
						dos.writeBytes("</Folder>\r\n");
						dos.writeBytes("\r\n");

					}

					++nSaved;
					dos.writeBytes("</Folder>\r\n");
					dos.writeBytes("\r\n");
				}
			}
			dos.writeBytes("</Document>\r\n</kml>\r\n");

		} catch (IOException ioe) {
			showMessage(ioe.toString(), true);
			return 0;
		}
		if (nSaved < 1) {
			showMessage("No suitable trajectories found!", true);
			return 0;
		}
		return nSaved;
	}

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
	}
}
