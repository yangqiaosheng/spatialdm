package spade.analysis.tools.moves;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.tools.BaseAnalyser;
import spade.analysis.tools.SingleInstanceTool;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.TextCanvas;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.dmap.DAggregateLinkLayer;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DPlaceVisitsLayer;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 23, 2009
 * Time: 3:00:24 PM
 * A UI and set of tools to measure and improve the quality of the
 * generalization and summarization of trajectories, which has
 * been done earlier.
 */
public class GeneralizationQualityManager extends BaseAnalyser implements SingleInstanceTool, ActionListener {
	protected ImproveGenerQuality imprQualProcedure = null;

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
		if (imprQualProcedure != null) {
			showMessage("Finish the interactive procedure for quality optimization!", true);
			return;
		}
		if (core.getUI().getCurrentMapViewer() == null || core.getUI().getCurrentMapViewer().getLayerManager() == null) {
			showMessage("No map exists!", true);
			return;
		}
		//find a suitable map layer:
		//1) instanceof DAggregateLinkLayer
		//2) has a reference to a DPlaceVisitsLayer, which is also present on the map
		//3) the DPlaceVisitsLayer consists of polygons (supposedly Voronoi polygons)
		//4) the DPlaceVisitsLayer has a table with thematic data
		LayerManager lman = core.getUI().getCurrentMapViewer().getLayerManager();
		Vector<DAggregateLinkLayer> aggLayers = new Vector(lman.getLayerCount(), 1);
		for (int i = 0; i < lman.getLayerCount(); i++) {
			GeoLayer layer = lman.getGeoLayer(i);
			if ((layer instanceof DAggregateLinkLayer) && layer.getObjectCount() > 1) {
				DAggregateLinkLayer agLayer = (DAggregateLinkLayer) layer;
				DGeoLayer pLayer = agLayer.getPlaceLayer();
				if (pLayer == null || pLayer.getObjectCount() < 2) {
					continue;
				}
				if (!(pLayer instanceof DPlaceVisitsLayer)) {
					continue;
				}
				if (!pLayer.hasThematicData()) {
					continue;
				}
				int idx = lman.getIndexOfLayer(pLayer.getContainerIdentifier());
				if (idx < 0 || !pLayer.equals(lman.getGeoLayer(idx))) {
					continue;
				}
				DGeoObject pObj = pLayer.getObject(0);
				if (pObj.getGeometry() == null || !(pObj.getGeometry() instanceof RealPolyline)) {
					continue;
				}
				aggLayers.addElement(agLayer);
			}
		}
		if (aggLayers.size() < 1) {
			showMessage("No appropriate layers with aggregate moves found!", true);
			return;
		}
		Panel mainP = new Panel(new ColumnLayout());
		mainP.add(new Label("Select the layer with aggregate moves:"));
		List list = new List(Math.max(aggLayers.size() + 1, 5));
		for (int i = 0; i < aggLayers.size(); i++) {
			list.add(aggLayers.elementAt(i).getName());
		}
		list.select(0);
		mainP.add(list);
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Generalization quality analysis", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return;
		int idx = list.getSelectedIndex();
		if (idx < 0)
			return;
		DAggregateLinkLayer agLayer = aggLayers.elementAt(idx);
		DPlaceVisitsLayer placeLayer = (DPlaceVisitsLayer) agLayer.getPlaceLayer();
		DataTable placeTable = (DataTable) placeLayer.getThematicData();
		DGeoLayer moveLayer = placeLayer.getTrajectoryLayer();
		DataTable metaData = (DataTable) moveLayer.getMetaData();
		if (metaData != null && !metaData.getEntitySetIdentifier().equals("generalization_refinement")) {
			metaData = null;
		}
		TextCanvas tc = null;
		if (placeLayer.maxDistortion <= 0) {
			if (!Dialogs.askYesOrNo(core.getUI().getMainFrame(), "Count the displacements of the trajectories in " + placeLayer.getName() + "?", "Count displacements"))
				return;
			placeLayer.computeDistortions();
			if (placeLayer.maxDistortion <= 0) {
				showMessage("Failed to compute the displacements!", true);
				return;
			}
			tc = new TextCanvas();
			tc.setText("3 new attributes have been added to the table " + placeTable.getName() + " with the total (sum), mean, and maximum displacements " + "in the areas");
			if (metaData == null) {
				metaData = new DataTable();
				metaData.setName("Refinement of " + agLayer.getName());
				metaData.addAttribute("Layer name", "layer_name", AttributeTypes.character);
				metaData.addAttribute("Derived from layer", "orig_layer_name", AttributeTypes.character);
				metaData.addAttribute("N of areas", "n_areas", AttributeTypes.integer);
				metaData.addAttribute("Overall max displacement", "max_distortion", AttributeTypes.real);
				metaData.addAttribute("Max of mean local displacements", "max_mean_distortion", AttributeTypes.real);
				metaData.addAttribute("Max of total local displacements", "max_sum_distortion", AttributeTypes.real);
				metaData.addAttribute("Overall mean displacement", "mean_distortion", AttributeTypes.real);
				metaData.addAttribute("Overall total displacement", "sum_distortion", AttributeTypes.real);
				moveLayer.setMetaData(metaData);
				core.getDataLoader().addTable(metaData);
				metaData.setEntitySetIdentifier("generalization_refinement");
			}
			DataRecord rec = new DataRecord(String.valueOf(metaData.getDataItemCount() + 1));
			metaData.addDataRecord(rec);
			rec.setAttrValue(placeLayer.getName(), 0);
			DPlaceVisitsLayer origPlaceLayer = placeLayer.getOrigPlaceLayer();
			if (origPlaceLayer != null) {
				rec.setAttrValue(origPlaceLayer.getName(), 1);
			}
			rec.setNumericAttrValue(placeLayer.getObjectCount(), String.valueOf(placeLayer.getObjectCount()), 2);
			rec.setNumericAttrValue(placeLayer.maxDistortion, StringUtil.floatToStr(placeLayer.maxDistortion, 2), 3);
			rec.setNumericAttrValue(placeLayer.maxMeanDistortion, StringUtil.floatToStr(placeLayer.maxMeanDistortion, 2), 4);
			rec.setNumericAttrValue(placeLayer.maxSumDistortion, StringUtil.floatToStr(placeLayer.maxSumDistortion, 2), 5);
			rec.setNumericAttrValue(placeLayer.meanDistortion, StringUtil.floatToStr(placeLayer.meanDistortion, 2), 6);
			rec.setNumericAttrValue(placeLayer.sumDistortion, StringUtil.floatToStr(placeLayer.sumDistortion, 2), 7);
			metaData.notifyPropertyChange("data_added", null, null);
		}
		mainP = new Panel(new ColumnLayout());
		Panel p = new Panel();
		GridBagLayout gridbag = new GridBagLayout();
		p.setLayout(gridbag);
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0f;
		c.weighty = 1.0f;
		c.fill = GridBagConstraints.HORIZONTAL;
		Label l = new Label("Overall maximum displacement:");
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label(String.valueOf(placeLayer.maxDistortion));
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label("Maximum of mean local displacements:");
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label(String.valueOf(placeLayer.maxMeanDistortion));
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label("Maximum of total local displacements:");
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label(String.valueOf(placeLayer.maxSumDistortion));
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label("Overall mean displacement:");
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label(String.valueOf(placeLayer.meanDistortion));
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label("Overall total displacement:");
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label(String.valueOf(placeLayer.sumDistortion));
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		mainP.add(p);
		if (tc != null) {
			mainP.add(tc);
		}
		mainP.add(new Line(false));
		CheckboxGroup cbg = new CheckboxGroup();
		Checkbox distoByIntervalsCB = new Checkbox("Count frequencies of displacements in the areas by intervals", !placeLayer.distoByIntervalsCounted, cbg);
		mainP.add(distoByIntervalsCB);
		Checkbox makeVisitsLayerCB = new Checkbox("Make a layer with points representing visits of the places", !distoByIntervalsCB.getState(), cbg);
		mainP.add(makeVisitsLayerCB);
		Checkbox refineCB = new Checkbox("Refine the division of the territory", false, cbg);
		mainP.add(refineCB);
		Checkbox optiCB = new Checkbox("Automatically optimize the quality of the generalization", false, cbg);
		mainP.add(optiCB);
		Checkbox interactCB = new Checkbox("Interactively optimize the quality of the generalization", false, cbg);
		mainP.add(interactCB);
		dia = new OKDialog(core.getUI().getMainFrame(), "Generalization quality analysis", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return;
		if (makeVisitsLayerCB != null && makeVisitsLayerCB.getState()) {
			GeneralizationQualityUtil genQ = new GeneralizationQualityUtil(core);
			Vector places = genQ.getSuitablePlaces(placeLayer);
			if (places != null && places.size() > 0) {
				DGeoLayer vLayer = genQ.makeVisitsLayer(placeLayer, places);
				if (vLayer != null && places.size() >= placeLayer.getObjectCount()) {
					placeLayer.setVisitsPointLayer(vLayer);
				}
			}
		} else if (distoByIntervalsCB.getState()) {
			mainP = new Panel(new ColumnLayout());
			mainP.add(new Label("Distortion frequencies by intervals", Label.CENTER));
			mainP.add(new Label("Specify the intervals:"));
			p = new Panel();
			gridbag = new GridBagLayout();
			p.setLayout(gridbag);
			c = new GridBagConstraints();
			c.weightx = 1.0f;
			c.weighty = 1.0f;
			c.fill = GridBagConstraints.HORIZONTAL;
			l = new Label("Upper limit:");
			c.gridwidth = 2;
			gridbag.setConstraints(l, c);
			p.add(l);
			TextField upLimitTF = new TextField(String.valueOf(placeLayer.maxDistortion), 10);
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(upLimitTF, c);
			p.add(upLimitTF);
			l = new Label("N of intervals:");
			c.gridwidth = 2;
			gridbag.setConstraints(l, c);
			p.add(l);
			TextField nIntervalsTF = new TextField("10", 10);
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(nIntervalsTF, c);
			p.add(nIntervalsTF);
			mainP.add(p);
			dia = new OKDialog(core.getUI().getMainFrame(), "Distortions in places", true);
			dia.addContent(mainP);
			dia.show();
			if (dia.wasCancelled())
				return;
			String str = upLimitTF.getText();
			float upLimit = 0;
			if (str != null) {
				try {
					upLimit = Float.valueOf(str).floatValue();
				} catch (NumberFormatException nfe) {
				}
			}
			if (upLimit <= 0)
				return;
			int nIntervals = 0;
			str = nIntervalsTF.getText();
			if (str != null) {
				try {
					nIntervals = Integer.valueOf(str).intValue();
				} catch (NumberFormatException nfe) {
				}
			}
			if (nIntervals < 2)
				return;
			int nAttr0 = placeTable.getAttrCount();
			placeLayer.computeDistortionsByIntervals(upLimit, nIntervals);
			int nAttr = placeTable.getAttrCount();
			int nAdded = nAttr - nAttr0;
			if (nAdded < 1) {
				showMessage("Failed to compute the frequencies by the intervals!", true);
			} else {
				Dialogs.showMessage(core.getUI().getMainFrame(), nAdded + " new attributes have been added to the table " + placeTable.getName(), "New attributes");
			}
		} else if (refineCB.getState()) {
			//let the user select a layer with points
			Vector<GeoLayer> pLayers = new Vector(lman.getLayerCount() - 1, 1);
			for (int i = 0; i < lman.getLayerCount(); i++) {
				GeoLayer layer = lman.getGeoLayer(i);
				if (layer.getObjectCount() > 0 && layer.getType() == Geometry.point) {
					pLayers.addElement(layer);
				}
			}
			if (pLayers.size() < 1) {
				showMessage("No suitable point layers found!", true);
				return;
			}
			mainP = new Panel(new ColumnLayout());
			mainP.add(new Label("Select the layer with new generating points:"));
			list = new List(Math.max(pLayers.size() + 1, 5));
			for (int i = 0; i < pLayers.size(); i++) {
				list.add(pLayers.elementAt(i).getName());
			}
			list.select(0);
			mainP.add(list);
			dia = new OKDialog(core.getUI().getMainFrame(), "Generalization quality analysis", true);
			dia.addContent(mainP);
			dia.show();
			if (dia.wasCancelled())
				return;
			idx = list.getSelectedIndex();
			if (idx < 0)
				return;
			GeoLayer pLayer = pLayers.elementAt(idx);
			Vector<RealPoint> points = new Vector<RealPoint>(100, 100);
			for (int i = 0; i < pLayer.getObjectCount(); i++)
				if (pLayer.isObjectActive(i)) {
					Geometry geom = pLayer.getObjectAt(i).getGeometry();
					if (geom != null && (geom instanceof RealPoint)) {
						points.addElement((RealPoint) geom);
					}
				}
			if (points.size() < 1) {
				showMessage("No points found!", true);
				return;
			}
			if (points.size() > 50)
				if (!Dialogs.askYesOrNo(core.getUI().getMainFrame(), points.size() + " points found. Continue?", "Too many points!"))
					return;
			GeneralizationQualityUtil genQ = new GeneralizationQualityUtil(core);
			genQ.refineSummarization(placeLayer, points);
		} else if (interactCB.getState()) {
			imprQualProcedure = new ImproveGenerQuality(core, agLayer, this);
			if (!imprQualProcedure.isValid() || !imprQualProcedure.startWork()) {
				imprQualProcedure = null;
			}
		} else if (optiCB.getState()) {
			p = new Panel();
			gridbag = new GridBagLayout();
			p.setLayout(gridbag);
			c = new GridBagConstraints();
			c.weightx = 1.0f;
			c.weighty = 1.0f;
			c.fill = GridBagConstraints.HORIZONTAL;
			l = new Label("Select places with total displacements >=", Label.RIGHT);
			c.gridwidth = 3;
			gridbag.setConstraints(l, c);
			p.add(l);
			TextField tfSumDistLowLimit = new TextField("60", 3);
			c.gridwidth = 1;
			gridbag.setConstraints(tfSumDistLowLimit, c);
			p.add(tfSumDistLowLimit);
			l = new Label("% of the current maximum");
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(l, c);
			p.add(l);
			l = new Label("and with mean displacements >=", Label.RIGHT);
			c.gridwidth = 3;
			gridbag.setConstraints(l, c);
			p.add(l);
			TextField tfMeanDistLowLimit = new TextField("80", 3);
			c.gridwidth = 1;
			gridbag.setConstraints(tfMeanDistLowLimit, c);
			p.add(tfMeanDistLowLimit);
			l = new Label("% of the current maximum");
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(l, c);
			p.add(l);
			l = new Label("Maximum number of selected places");
			c.gridwidth = 3;
			gridbag.setConstraints(l, c);
			p.add(l);
			TextField tfMaxPlaces = new TextField("5", 3);
			c.gridwidth = 1;
			gridbag.setConstraints(tfMaxPlaces, c);
			p.add(tfMaxPlaces);
			l = new Label("per step");
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(l, c);
			p.add(l);
			l = new Label("Terminate when the improvement is below");
			c.gridwidth = 3;
			gridbag.setConstraints(l, c);
			p.add(l);
			TextField tfMinImpr = new TextField("1", 3);
			c.gridwidth = 1;
			gridbag.setConstraints(tfMinImpr, c);
			p.add(tfMinImpr);
			l = new Label("% to the previous result");
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(l, c);
			p.add(l);
			l = new Label("Maximum number of iteration steps:");
			c.gridwidth = 3;
			gridbag.setConstraints(l, c);
			p.add(l);
			TextField tfMaxSteps = new TextField("5", 3);
			c.gridwidth = 1;
			gridbag.setConstraints(tfMaxSteps, c);
			p.add(tfMaxSteps);
			l = new Label("");
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(l, c);
			p.add(l);
			l = new Label("Maximum allowed computation time:");
			c.gridwidth = 3;
			gridbag.setConstraints(l, c);
			p.add(l);
			TextField tfMaxTime = new TextField("60", 3);
			c.gridwidth = 1;
			gridbag.setConstraints(tfMaxTime, c);
			p.add(tfMaxTime);
			l = new Label("seconds");
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(l, c);
			p.add(l);
			mainP = new Panel(new ColumnLayout());
			mainP.add(new Label("Automatic iterative improvement of the generalization quality"));
			mainP.add(p);
			dia = new OKDialog(core.getUI().getMainFrame(), "Quality optimization", true);
			dia.addContent(mainP);
			dia.show();
			if (dia.wasCancelled())
				return;
			String str = tfSumDistLowLimit.getText();
			float sumDistLowLimit = 90f;
			if (str != null) {
				try {
					sumDistLowLimit = Float.valueOf(str).floatValue();
				} catch (NumberFormatException nfe) {
				}
			}
			if (sumDistLowLimit <= 0)
				return;
			str = tfMeanDistLowLimit.getText();
			float meanDistLowLimit = 90f;
			if (str != null) {
				try {
					meanDistLowLimit = Float.valueOf(str).floatValue();
				} catch (NumberFormatException nfe) {
				}
			}
			int maxNPlaces = 0;
			str = tfMaxPlaces.getText();
			if (str != null) {
				try {
					maxNPlaces = Integer.valueOf(str).intValue();
				} catch (NumberFormatException nfe) {
				}
			}
			str = tfMinImpr.getText();
			float minImprovement = 10f;
			if (str != null) {
				try {
					minImprovement = Float.valueOf(str).floatValue();
				} catch (NumberFormatException nfe) {
				}
			}
			str = tfMaxSteps.getText();
			int nIter = 0;
			if (str != null) {
				try {
					nIter = Integer.valueOf(str).intValue();
				} catch (NumberFormatException nfe) {
				}
			}
			str = tfMaxTime.getText();
			long maxTime = 0l;
			if (str != null) {
				try {
					maxTime = Long.valueOf(str).longValue();
				} catch (NumberFormatException nfe) {
				}
			}
			GeneralizationQualityUtil genQ = new GeneralizationQualityUtil(core);
			DAggregateLinkLayer resLayer = genQ.optimizeQuality(agLayer, sumDistLowLimit, meanDistLowLimit, maxNPlaces, minImprovement, nIter, maxTime);
			if (resLayer == null) {
				showMessage("Failed to optimize!", true);
				return;
			}
			DPlaceVisitsLayer origPlaceLayer = placeLayer;
			placeLayer = (DPlaceVisitsLayer) resLayer.getPlaceLayer();
			placeLayer.computeDistortions();
			DataRecord rec = new DataRecord(String.valueOf(metaData.getDataItemCount() + 1));
			metaData.addDataRecord(rec);
			rec.setAttrValue(placeLayer.getName(), 0);
			rec.setAttrValue(origPlaceLayer.getName(), 1);
			rec.setNumericAttrValue(placeLayer.getObjectCount(), String.valueOf(placeLayer.getObjectCount()), 2);
			rec.setNumericAttrValue(placeLayer.maxDistortion, StringUtil.floatToStr(placeLayer.maxDistortion, 2), 3);
			rec.setNumericAttrValue(placeLayer.maxMeanDistortion, StringUtil.floatToStr(placeLayer.maxMeanDistortion, 2), 4);
			rec.setNumericAttrValue(placeLayer.maxSumDistortion, StringUtil.floatToStr(placeLayer.maxSumDistortion, 2), 5);
			rec.setNumericAttrValue(placeLayer.meanDistortion, StringUtil.floatToStr(placeLayer.meanDistortion, 2), 6);
			rec.setNumericAttrValue(placeLayer.sumDistortion, StringUtil.floatToStr(placeLayer.sumDistortion, 2), 7);
			metaData.notifyPropertyChange("data_added", null, null);
		}
	}

	/**
	 * Reacts to the termination of the process of interactive quality refinement
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		core.getUI().clearStatusLine();
		if (e.getSource().equals(imprQualProcedure)) {
			if (e.getActionCommand().equals("finish")) {
				DAggregateLinkLayer resLayer = imprQualProcedure.getRefinedLinkLayer();
				DPlaceVisitsLayer placeLayer = imprQualProcedure.getRefinedPlaceLayer();
				if (resLayer != null && placeLayer != null) {
					DGeoLayer moveLayer = resLayer.getTrajectoryLayer();
					DataTable metaData = (DataTable) moveLayer.getMetaData();
					if (metaData != null && !metaData.getEntitySetIdentifier().equals("generalization_refinement")) {
						metaData = null;
					}
					if (metaData != null) {
						placeLayer.computeDistortions();
						DataRecord rec = new DataRecord(String.valueOf(metaData.getDataItemCount() + 1));
						metaData.addDataRecord(rec);
						rec.setAttrValue(placeLayer.getName(), 0);
						DPlaceVisitsLayer origPlaceLayer = imprQualProcedure.getOrigPlaceLayer();
						rec.setAttrValue(origPlaceLayer.getName(), 1);
						rec.setNumericAttrValue(placeLayer.getObjectCount(), String.valueOf(placeLayer.getObjectCount()), 2);
						rec.setNumericAttrValue(placeLayer.maxDistortion, StringUtil.floatToStr(placeLayer.maxDistortion, 2), 3);
						rec.setNumericAttrValue(placeLayer.maxMeanDistortion, StringUtil.floatToStr(placeLayer.maxMeanDistortion, 2), 4);
						rec.setNumericAttrValue(placeLayer.maxSumDistortion, StringUtil.floatToStr(placeLayer.maxSumDistortion, 2), 5);
						rec.setNumericAttrValue(placeLayer.meanDistortion, StringUtil.floatToStr(placeLayer.meanDistortion, 2), 6);
						rec.setNumericAttrValue(placeLayer.sumDistortion, StringUtil.floatToStr(placeLayer.sumDistortion, 2), 7);
						metaData.notifyPropertyChange("data_added", null, null);
					}
				}
			}
			imprQualProcedure = null;
		}
	}
}
