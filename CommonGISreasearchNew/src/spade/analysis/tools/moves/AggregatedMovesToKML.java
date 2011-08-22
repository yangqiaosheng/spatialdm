package spade.analysis.tools.moves;

import java.awt.FileDialog;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.tools.DataAnalyser;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.CopyFile;
import spade.lib.util.StringUtil;
import spade.vis.dmap.DAggregateLinkLayer;
import spade.vis.dmap.DGeoLayer;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealLine;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.space.LayerManager;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 20-Mar-2008
 * Time: 16:25:00
 * To change this template use File | Settings | File Templates.
 */
public class AggregatedMovesToKML implements DataAnalyser {

	protected ESDACore core = null;

	/**
	* Remembers the last directory where KML files were saved
	*/
	protected static String lastDir = null;

	/**
	 * Returns true when the tool has everything necessary for its operation.
	 * For example, if the tool manages a number of analysis methods, it should
	 * check whether the class for at least one method is available.
	 * Always returns true.
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
		if (core.getUI().getCurrentMapViewer() == null || core.getUI().getCurrentMapViewer().getLayerManager() == null) {
			showMessage("No map exists!", true);
			return;
		}
		//Find instances of DAggregateLinkLayer
		LayerManager lman = core.getUI().getCurrentMapViewer().getLayerManager();
		Vector aggLayers = new Vector(lman.getLayerCount(), 1);
		for (int i = 0; i < lman.getLayerCount(); i++)
			if (lman.getGeoLayer(i) instanceof DAggregateLinkLayer) {
				aggLayers.addElement(lman.getGeoLayer(i));
			}
		if (aggLayers.size() < 1) {
			showMessage("No layers with aggregated moves (vectors) found!", true);
			return;
		}
		int idx = 0;
		if (aggLayers.size() > 1) {
			Panel mainP = new Panel(new ColumnLayout());
			mainP.add(new Label("Select the layer with aggregated moves:"));
			List mList = new List(Math.max(aggLayers.size() + 1, 5));
			for (int i = 0; i < aggLayers.size(); i++) {
				mList.add(((DAggregateLinkLayer) aggLayers.elementAt(i)).getName());
			}
			mList.select(0);
			mainP.add(mList);
			OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Build movement matrix", true);
			dia.addContent(mainP);
			dia.show();
			if (dia.wasCancelled())
				return;
			idx = mList.getSelectedIndex();
			if (idx < 0)
				return;
		}
		DAggregateLinkLayer aggLayer = (DAggregateLinkLayer) aggLayers.elementAt(idx);
		// classes of lines according to values of the selected attribute (default=N Active Moves)
		int attrIdx = aggLayer.nMovesIdxActive;
		double minv = Double.NaN, maxv = Double.NaN;
		for (int tr = 0; tr < aggLayer.getObjectCount(); tr++)
			if (aggLayer.isObjectActive(tr)) {
				double v = aggLayer.getThematicData().getNumericAttrValue(attrIdx, tr);
				if (Double.isNaN(minv) || v < minv) {
					minv = v;
				}
				if (Double.isNaN(maxv) || v > maxv) {
					maxv = v;
				}
			}
		float breaks[] = new float[5];
		for (int i = 0; i < breaks.length; i++) {
			breaks[i] = (float) (minv + (maxv - minv) * (i + 1) / (breaks.length + 1));
		}
		// output file name
		FileDialog fd = new FileDialog(core.getUI().getMainFrame(), "Output file");
		if (lastDir != null) {
			fd.setDirectory(lastDir);
		}
		fd.setFile("*.kml");
		fd.setMode(FileDialog.SAVE);
		fd.show();
		if (fd.getDirectory() == null)
			return;
		lastDir = fd.getDirectory();
		String filename = fd.getFile();
		String str = CopyFile.getExtension(filename);
		if (str == null || str.length() < 1) {
			filename += ".kml";
		}
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(lastDir + filename);
		} catch (IOException ioe) {
			showMessage(ioe.toString(), true);
			return;
		}
		//float minx=Float.NaN, miny=Float.NaN, maxx=Float.NaN, maxy=Float.NaN;
		String name = "Visual Analytics Toolkit";
		DataOutputStream dos = new DataOutputStream(out);
		try {
			dos.writeBytes("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
			dos.writeBytes("<kml xmlns=\"http://earth.google.com/kml/2.1\">\r\n");
			dos.writeBytes("<Document>\r\n");
			dos.writeBytes("<name>" + name + "</name>\r\n");
			dos.writeBytes("<description>Value range from " + minv + " to " + maxv + "</description>\r\n");
			dos.writeBytes("<Style id=\"groundLine\">\r\n");
			dos.writeBytes("  <LineStyle>\r\n");
			dos.writeBytes("    <color>7fff00ff</color>\r\n");
			dos.writeBytes("    <width>3</width>\r\n");
			dos.writeBytes("  </LineStyle>\r\n");
			dos.writeBytes("</Style>\r\n");
			dos.writeBytes("<Style id=\"skyLinePoly\">\r\n");
			dos.writeBytes("  <LineStyle>\r\n");
			dos.writeBytes("    <color>7fff00ff</color>\r\n");
			dos.writeBytes("    <width>3</width>\r\n");
			dos.writeBytes("  </LineStyle>\r\n");
			dos.writeBytes("  <PolyStyle>\r\n");
			dos.writeBytes("    <color>3f00ff00</color>\r\n");
			dos.writeBytes("  </PolyStyle>\r\n");
			dos.writeBytes("</Style>\r\n");
			for (int style = 0; style <= breaks.length; style++) {
				dos.writeBytes("<Style id=\"line_" + style + "\">\r\n");
				dos.writeBytes("  <LineStyle>\r\n");
				dos.writeBytes("    <color>7f" + StringUtil.padString(Integer.toHexString(255 * style / breaks.length), '0', 2, true) + "0000</color>\r\n");
				dos.writeBytes("    <width>" + (2 + 2 * style) + "</width>\r\n");
				dos.writeBytes("  </LineStyle>\r\n");
				dos.writeBytes("</Style>\r\n");
			}
			dos.writeBytes("\r\n");
			/*
			dos.writeBytes("<LookAt>\r\n");
			dos.writeBytes("  <longitude>"+(minx+maxx)/2+"</longitude>\r\n");
			dos.writeBytes("  <latitude>"+(miny+maxy)/2+"</latitude>\r\n");
			dos.writeBytes("  <altitude>0</altitude>\r\n");
			dos.writeBytes("  <range>10000</range>\r\n");
			dos.writeBytes("  <tilt>70</tilt>\r\n");
			dos.writeBytes("  <heading>0</heading>\r\n");
			dos.writeBytes("</LookAt>\r\n\r\n");
			*/

			int nMoves = 0;
			for (int style = breaks.length; style >= 0; style--) {
				dos.writeBytes("<Folder>\r\n");
				dos.writeBytes("<name>Values: " + ((style > 0) ? " > " + breaks[style - 1] : " le " + breaks[0]) + "</name>\r\n");
				for (int tr = 0; tr < aggLayer.getObjectCount(); tr++)
					if (aggLayer.isObjectActive(tr)) {
						double v = aggLayer.getThematicData().getNumericAttrValue(attrIdx, tr);
						Geometry geo = aggLayer.getObject(tr).getGeometry();
						if (geo instanceof RealLine && (style == breaks.length && v > breaks[style - 1]) || (style == 0 && v <= breaks[0]) || (style > 0 && style < breaks.length && v > breaks[style - 1] && v <= breaks[style])) {
							RealLine rl = (RealLine) geo;
							dos.writeBytes(" <Placemark>\r\n");
							dos.writeBytes("   <name>" + aggLayer.getObjectId(tr) + "</name>\r\n");
							dos.writeBytes("   <description>value: " + v + "</description>\r\n");
							dos.writeBytes("   <styleUrl>#line_" + style + "</styleUrl>\r\n");
							dos.writeBytes("   <LineString>\r\n");
							dos.writeBytes("   <altitudeMode>absolute</altitudeMode>\r\n");
							dos.writeBytes("   <coordinates>\r\n");
							dos.writeBytes("     " + rl.x1 + ", " + rl.y1 + ", 0 " + rl.x2 + ", " + rl.y2 + ", 10000 " + "\r\n");
							dos.writeBytes("   </coordinates>\r\n");
							dos.writeBytes("   </LineString>\r\n");
							dos.writeBytes(" </Placemark>\r\n");
							nMoves++;
						}
					}
				dos.writeBytes("</Folder>\r\n\r\n");
			}

			DGeoLayer places = aggLayer.getPlaceLayer();
			dos.writeBytes("<Folder>\r\n");
			dos.writeBytes("<name>Areas (ground)</name>\r\n");
			for (int i = 0; i < places.getObjectCount(); i++) {
				Geometry geo = places.getObject(i).getGeometry();
				if (geo instanceof RealPolyline) {
					RealPolyline rpl = (RealPolyline) geo;
					dos.writeBytes(" <Placemark>\r\n");
					dos.writeBytes("   <name>" + places.getObject(i).getLabel() + "</name>\r\n");
					dos.writeBytes("   <styleUrl>#groundLine</styleUrl>\r\n");
					dos.writeBytes("   <LineString>\r\n");
					dos.writeBytes("   <altitudeMode>relativeToGround</altitudeMode>\r\n");
					dos.writeBytes("   <coordinates>\r\n");
					for (RealPoint element : rpl.p) {
						dos.writeBytes("      " + element.getX() + ", " + element.getY() + ", 0\r\n");
					}
					dos.writeBytes("   </coordinates>\r\n");
					dos.writeBytes("   </LineString>\r\n");
					dos.writeBytes(" </Placemark>\r\n\r\n");
					dos.writeBytes("");
				}
			}
			dos.writeBytes("</Folder>\r\n");

			dos.writeBytes("<Folder>\r\n");
			dos.writeBytes("<name>Areas (sky)</name>\r\n");
			for (int i = 0; i < places.getObjectCount(); i++) {
				Geometry geo = places.getObject(i).getGeometry();
				if (geo instanceof RealPolyline) {
					RealPolyline rpl = (RealPolyline) geo;
					dos.writeBytes(" <Placemark>\r\n");
					dos.writeBytes("   <name>" + places.getObject(i).getLabel() + "</name>\r\n");
					dos.writeBytes("   <styleUrl>#skyLinePoly</styleUrl>\r\n");
					dos.writeBytes("   <LineString>\r\n");
					dos.writeBytes("   <altitudeMode>absolute</altitudeMode>\r\n");
					dos.writeBytes("   <coordinates>\r\n");
					for (RealPoint element : rpl.p) {
						dos.writeBytes("      " + element.getX() + ", " + element.getY() + ", 10000\r\n");
					}
					dos.writeBytes("   </coordinates>\r\n");
					dos.writeBytes("   </LineString>\r\n");
					dos.writeBytes(" </Placemark>\r\n\r\n");
					dos.writeBytes("");
				}
			}
			dos.writeBytes("</Folder>\r\n");

			dos.writeBytes("</Document>\r\n</kml>\r\n");
			dos.close();
			showMessage("Ready: " + nMoves + " moves are stored in " + lastDir + filename, false);

		} catch (IOException ioe) {
			showMessage(ioe.toString(), true);
		}
	}

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
	}
}
