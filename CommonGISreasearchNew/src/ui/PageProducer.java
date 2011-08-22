package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.TextArea;
import java.awt.TextField;
import java.util.Vector;

import spade.analysis.plot.PrintableImage;
import spade.analysis.system.ESDACore;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.lib.page_util.PageCollection;
import spade.lib.page_util.PageElementImage;
import spade.lib.page_util.PageStructure;
import spade.vis.dmap.DLayerManager;
import spade.vis.dmap.MapCanvas;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Mar 27, 2009
 * Time: 3:08:15 PM
 * Produces HTML pages with map and legend.
 */
public class PageProducer {
	protected ESDACore core = null;
	protected static int imCount = 0, mapCount = 0;

	public PageProducer(ESDACore core) {
		this.core = core;
	}

	public void chooseAndPrint() {
		if (core == null || core.getUI() == null)
			return;
		Supervisor supervisor = core.getSupervisor();
		List list = new List(10);
		Vector displays = new Vector(20, 10);
		for (int i = 0; i < supervisor.getSaveableToolCount(); i++)
			if (supervisor.getSaveableTool(i) instanceof PrintableImage) {
				PrintableImage prIm = (PrintableImage) supervisor.getSaveableTool(i);
				displays.addElement(prIm);
				list.add(prIm.getName());
			} else if (supervisor.getSaveableTool(i) instanceof SimpleMapView) {
				SimpleMapView map = (SimpleMapView) supervisor.getSaveableTool(i);
				displays.addElement(map);
				list.add(map.getName());
			}
		list.select(0);
		Panel p = new Panel(new BorderLayout());
		p.add(new Label("Select what to print:"), BorderLayout.NORTH);
		p.add(list, BorderLayout.CENTER);
		Panel pp = new Panel(new ColumnLayout());
		pp.add(new Label("Title?", Label.CENTER));
		TextField titleTF = new TextField(60);
		pp.add(titleTF);
		pp.add(new Label("Comment?", Label.CENTER));
		TextArea tArea = new TextArea(10, 60);
		pp.add(tArea);
		p.add(pp, BorderLayout.SOUTH);
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "What to print?", true);
		dia.addContent(p);
		dia.show();
		if (dia.wasCancelled())
			return;

		PageStructure ps = new PageStructure();
		ps.title = titleTF.getText();
		if (ps.title == null || ps.title.length() < 1) {
			ps.title = list.getSelectedItem();
		}
		ps.header = tArea.getText();
		if (ps.header != null && ps.header.trim().length() < 1) {
			ps.header = ps.title;
		}

		int idx = list.getSelectedIndex();
		if (displays.elementAt(idx) instanceof PrintableImage) {
			PrintableImage prIm = (PrintableImage) displays.elementAt(idx);
			Image img = prIm.getImage();
			if (img == null) {
				core.getUI().showMessage("Failed to get an image!", true);
				return;
			}
			PageElementImage pElIm = new PageElementImage();
			pElIm.image = img;
			pElIm.width = img.getWidth(null);
			ps.addElement(pElIm);
			pElIm.fname = "img_" + imCount;
			ps.fname = "_image_" + (imCount++);
		} else {
			SimpleMapView map = (SimpleMapView) displays.elementAt(idx);
			Image imgMap = map.getMapDrawer().getMapAsImage();
			if (imgMap == null) {
				core.getUI().showMessage("Failed to get a map image!", true);
				return;
			}
			PageElementImage pElImMap = new PageElementImage();
			pElImMap.image = imgMap;
			pElImMap.width = imgMap.getWidth(null);
			pElImMap.fname = "map_" + mapCount;
			float ext[] = map.getMapExtent();
			if (ext != null) {
				pElImMap.x1 = ext[0];
				pElImMap.y1 = ext[1];
				pElImMap.x2 = ext[2];
				pElImMap.y2 = ext[3];
			}
			ps.addElement(pElImMap);
			int legW = pElImMap.width;
			if (legW < 250) {
				legW = 250;
			}
			Image img = null;
			if ((map.getLayerManager() instanceof DLayerManager) && (map.getMapDrawer() instanceof MapCanvas)) {
				DLayerManager lm = (DLayerManager) map.getLayerManager();
				MapCanvas mc = (MapCanvas) map.getMapDrawer();
				Image img0 = mc.createImage(50, 50);
				if (img0 != null) {
					Graphics g = img0.getGraphics();
					if (g != null) {
						Rectangle r = lm.drawLegendOnlyVisible(mc, g, 0, 0, legW);
						g.dispose();
						if (r != null) {
							img = mc.createImage(r.width, r.height);
							if (img != null) {
								g = img.getGraphics();
								g.setColor(Color.white);
								g.fillRect(0, 0, r.width + 1, r.height + 1);
								lm.drawLegendOnlyVisible(mc, g, 0, 0, legW);
								g.dispose();
							}
						}
					}
				}
			}
			if (img == null) {
				img = map.getLegendAsImage(new Dimension(legW, 1000));
			}
			if (img != null) {
				ps.layout = PageStructure.LAYOUT_2_COLUMNS;
				PageElementImage pElIm = new PageElementImage();
				pElIm.image = img;
				pElIm.width = img.getWidth(null);
				pElIm.fname = "legend_" + mapCount;
				ps.addElement(pElIm);
			}
			ps.fname = "_map_" + (mapCount++);
		}
		PageCollection pc = new PageCollection();
		pc.addPage(ps);
		String str[] = core.updateDTinPageMaker();
		String sLFdt_fmt = str[0]; // human-readable
		String sLFdt = str[1]; // file name
		String sLFlastCommand = sLFdt_fmt + " <A HREF=\"./" + sLFdt + ps.fname + ".html\">Printed " + ps.title + "</A></P>";
		core.logSimpleAction(sLFlastCommand);
		core.makePages(pc);
	}
}
