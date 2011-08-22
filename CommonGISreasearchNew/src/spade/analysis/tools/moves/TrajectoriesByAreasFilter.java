package spade.analysis.tools.moves;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import spade.analysis.plot.ObjectList;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.DataAnalyser;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.IdMaker;
import spade.vis.action.Highlighter;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DMovingObject;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPolyline;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Mar 18, 2008
 * Time: 11:30:31 AM
 * To change this template use File | Settings | File Templates.
 */
public class TrajectoriesByAreasFilter implements DataAnalyser, ActionListener {

	protected ESDACore core = null;

	protected ObjectList ol = null;

	private Frame frame = null;

	protected DGeoLayer moveLayer = null;
	protected DGeoLayer areaLayer = null;

	/**
	 * Returns true when the tool has everything necessary for its operation.
	 * For example, if the tool manages a number of analysis methods, it should
	 * check whether the class for at least one method is available.
	 * A TrajectoriesGeneraliser always returns true.
	 */
	@Override
	public boolean isValid(ESDACore core) {
		return true;
	}

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
		Vector areaLayers = new Vector(lman.getLayerCount(), 1);
		for (int i = 0; i < lman.getLayerCount(); i++) {
			GeoLayer layer = lman.getGeoLayer(i);
			if ((layer instanceof DGeoLayer) && layer.getObjectCount() > 0)
				if (layer.getObjectAt(0) instanceof DMovingObject) {
					moveLayers.addElement(layer);
				} else if (layer.getType() == Geometry.area && layer.getObjectCount() > 1) {
					areaLayers.addElement(layer);
				}
		}
		if (moveLayers.size() < 1) {
			showMessage("No layers with trajectories found!", true);
			return;
		}
		if (areaLayers.size() < 1) {
			showMessage("No layers with areas found!", true);
			return;
		}
		// UI for selecting layers with trajectories and areas
		Panel mainP = new Panel(new ColumnLayout());
		mainP.add(new Label("Select the layer with trajectories to summarise:"));
		List mList = new List(Math.max(moveLayers.size() + 1, 5));
		for (int i = 0; i < moveLayers.size(); i++) {
			mList.add(((DGeoLayer) moveLayers.elementAt(i)).getName());
		}
		mList.select(0);
		mainP.add(mList);
		mainP.add(new Line(false));
		mainP.add(new Label("Select the layer with areas:"));
		List aList = new List(Math.max(areaLayers.size() + 1, 5));
		for (int i = 0; i < areaLayers.size(); i++) {
			aList.add(((DGeoLayer) areaLayers.elementAt(i)).getName());
		}
		aList.select(0);
		mainP.add(aList);
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Filter trajectories by areas (1)", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return;
		int idx = mList.getSelectedIndex();
		if (idx < 0)
			return;
		moveLayer = (DGeoLayer) moveLayers.elementAt(idx);
		idx = aList.getSelectedIndex();
		if (idx < 0)
			return;
		areaLayer = (DGeoLayer) areaLayers.elementAt(idx);

		// selecting areas
		mainP = new Panel(new ColumnLayout());
		mainP.add(new Label("Select visited areas", Label.CENTER));
		ol = new ObjectList();
		ol.construct(core.getSupervisor(), 20, areaLayer);
		mainP.add(ol);
		mainP.add(new Line(false));
		Panel p = new Panel(new BorderLayout());
		Button b = new Button("OK");
		b.addActionListener(this);
		b.setActionCommand("finish");
		p.add(b, BorderLayout.WEST);
		b = new Button("Cancel");
		b.addActionListener(this);
		b.setActionCommand("cancel");
		p.add(b, BorderLayout.EAST);
		mainP.add(p);
		frame = new Frame("Filter trajectories by areas (2)");
		frame.setLayout(new BorderLayout());
		frame.add(mainP);
		frame.pack();
		Dimension ss = Toolkit.getDefaultToolkit().getScreenSize(), fs = frame.getSize();
		frame.setLocation(ss.width - fs.width - 50, 50);
		frame.setVisible(true);
		frame.setResizable(false);
	}

	protected void continueSettingParameters() {
		// areas have been selected.
		// Filtering trajectories in <moveLayer> by selected objects in <areaLayer>
		Vector referenceObjectsIDs = null;
		Highlighter highlighter = core.getSupervisor().getHighlighter(areaLayer.getEntitySetIdentifier());
		if (highlighter.getSelectedObjects() != null && highlighter.getSelectedObjects().size() > 0) {
			//highlighter.removeHighlightListener(this);
			referenceObjectsIDs = new Vector(highlighter.getSelectedObjects().size(), 10);
			for (int i = 0; i < highlighter.getSelectedObjects().size(); i++) {
				referenceObjectsIDs.addElement(highlighter.getSelectedObjects().elementAt(i));
			}
		} else {
			referenceObjectsIDs = new Vector(areaLayer.getObjectCount(), 10);
			for (int i = 0; i < areaLayer.getObjectCount(); i++) {
				referenceObjectsIDs.addElement(areaLayer.getObjectId(i));
			}
		}
		int N = 1;
		if (referenceObjectsIDs.size() > 1) {
			Panel p = new Panel(new ColumnLayout());
			p.add(new Label("N selected areas = " + referenceObjectsIDs.size(), Label.CENTER));
			p.add(new Line(false));
			p.add(new Label("Min number of visited among them?", Label.CENTER));
			Choice ch = new Choice();
			for (int i = 1; i < referenceObjectsIDs.size(); i++) {
				ch.add("" + i);
			}
			ch.add("all");
			ch.select(ch.getItemCount() - 1);
			p.add(ch);
			p.add(new Line(false));
			OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Filter trajectories by areas (3)", true);
			dia.addContent(p);
			dia.show();
			if (dia.wasCancelled())
				return;
			N = 1 + ch.getSelectedIndex();
		}
		performComputations(N, referenceObjectsIDs);
	}

	protected void performComputations(int N, Vector referenceObjectsIDs) {
		DataTable dt = (DataTable) moveLayer.getThematicData();
		if (!dt.hasData()) {
			dt.loadData();
		}
		String attrName = spade.lib.basicwin.Dialogs.askForStringValue(core.getUI().getMainFrame(), "Attribute name:", "selected by areas", "", "Set attribute name", false);
		dt.addAttribute(attrName, IdMaker.makeId("sel_by_areas", dt), AttributeTypes.logical);
		int idxAttr = dt.getAttrCount() - 1;
		int nSelected = 0;
		for (int t = 0; t < moveLayer.getObjectCount(); t++) {
			int visitedCounter = 0;
			DMovingObject dmo = (DMovingObject) moveLayer.getObject(t);
			Geometry geo = dmo.getGeometry();
			if (geo instanceof RealPolyline) {
				RealPolyline mrpl = (RealPolyline) dmo.getGeometry();
				for (int a = 0; a < referenceObjectsIDs.size() && visitedCounter < N; a++) {
					DGeoObject adgo = areaLayer.getObject(areaLayer.getObjectIndex((String) referenceObjectsIDs.elementAt(a)));
					Geometry ag = adgo.getGeometry();
					boolean isInside = false;
					for (int p = 0; p < mrpl.p.length && !isInside; p++) {
						isInside = ag.contains(mrpl.p[p].getX(), mrpl.p[p].getY(), 0f);
					}
					if (isInside) {
						visitedCounter++;
					}
				}
				if (visitedCounter >= N) {
					nSelected++;
					//System.out.println("* inside: "+nSelected+") "+dmo.getIdentifier());
				}
			}
			DataRecord dr = (DataRecord) dmo.getData();
			if (dr == null) {
				System.out.println("! no data record: " + dmo.getIdentifier());
			} else {
				dr.setAttrValue((visitedCounter >= N) ? "yes" : "no", idxAttr);
			}
		}
		showMessage(nSelected + " trajectories have been selected", nSelected == 0);
	}

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("finish") || e.getActionCommand().equals("cancel")) {
			ol.destroy();
			frame.dispose();
		}
		if (e.getActionCommand().equals("finish")) {
			continueSettingParameters();
		}
	}
}
