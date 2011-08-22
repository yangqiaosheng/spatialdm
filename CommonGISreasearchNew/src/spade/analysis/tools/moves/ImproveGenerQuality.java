package spade.analysis.tools.moves;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import spade.analysis.geocomp.voronoi.VoronoiNew;
import spade.analysis.manipulation.Manipulator;
import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.OKFrame;
import spade.lib.basicwin.TextCanvas;
import spade.vis.action.Highlighter;
import spade.vis.database.DataTable;
import spade.vis.dmap.DAggregateLinkLayer;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DPlaceVisitsLayer;
import spade.vis.dmap.DPlaceVisitsObject;
import spade.vis.dmap.PlaceVisitInfo;
import spade.vis.geometry.RealPoint;
import spade.vis.map.MapViewer;
import spade.vis.space.GeoObject;
import spade.vis.space.LayerManager;
import core.ActionDescr;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 29, 2009
 * Time: 12:46:26 PM
 * Iteratively improves the quality of the generalization of movement data.
 * Interacts with the user, who selects the areas where the improvement is needed.
 */
public class ImproveGenerQuality implements ActionListener {
	protected ESDACore core = null;
	/**
	 * The owner waits for a notification about the termination of the process
	 */
	protected ActionListener owner = null;
	/**
	 * The layer with the aggregate moves where the quality of the
	 * generalization (points -> areas) should be improved.
	 */
	protected DAggregateLinkLayer agLayer = null;
	/**
	 * The layer with the generalized positions (points -> areas),
	 * which needs to be refined.
	 */
	protected DPlaceVisitsLayer placeLayer = null;
	/**
	 * The table describing the generalized places
	 */
	protected DataTable placeTable = null;
	/**
	 * The layer with the original movement data (trajectories)
	 */
	protected DGeoLayer moveLayer = null;
	/**
	 * The layer with aggregate moves resulting from the latest refinement step.
	 */
	protected DAggregateLinkLayer currAgLayer = null;
	/**
	 * The layer with the generalized positions resulting from the latest refinement step.
	 */
	protected DPlaceVisitsLayer currPlaceLayer = null;
	/**
	 * The table describing the generalized places resulting from the latest refinement step.
	 */
	protected DataTable currPlaceTable = null;
	/**
	 * Used for the visualization of the distortions in the
	 * generalized places
	 */
	protected MapViewer mapView = null;
	/**
	 * The layer manager of the map view
	 */
	protected LayerManager lman = null;
	/**
	 * Remains on the screen until the user selects the cells where
	 * to apply quality optimization.
	 */
	protected OKFrame selAreasFrame = null;
	/**
	 * Describes the changes after each step of the refinement
	 */
	protected DataTable metaData = null;
	/**
	 * Used to ask the user whether he/she wants to continue the improvement
	 * procedure.
	 */
	protected OKFrame askContinueFrame = null;
	/**
	 * The number of iteration steps done
	 */
	protected int nSteps = 0;

	/**
	 * @param core - used for the access to the map, data manager, etc.
	 * @param agLayer - the layer with the aggregate moves where the
	 *   quality of the generalization (points -> areas) should be improved.
	 */
	public ImproveGenerQuality(ESDACore core, DAggregateLinkLayer agLayer, ActionListener owner) {
		this.core = core;
		this.agLayer = agLayer;
		this.owner = owner;
		if (core != null && core.getUI() != null) {
			mapView = core.getUI().getCurrentMapViewer();
			if (mapView != null) {
				lman = mapView.getLayerManager();
			}
		}
		if (agLayer != null) {
			placeLayer = (DPlaceVisitsLayer) agLayer.getPlaceLayer();
			if (placeLayer != null) {
				placeTable = (DataTable) placeLayer.getThematicData();
			}
			moveLayer = placeLayer.getTrajectoryLayer();
		}
		currAgLayer = agLayer;
		currPlaceLayer = placeLayer;
		currPlaceTable = placeTable;
	}

	public boolean isValid() {
		return core != null && mapView != null && lman != null && currAgLayer != null && currPlaceLayer != null && currPlaceLayer.getObjectCount() > 0 && currPlaceTable != null && currPlaceTable.hasData() && moveLayer != null
				&& moveLayer.getObjectCount() > 0;
	}

	/**
	 * @return true if successfully started
	 */
	public boolean startWork() {
		if (currPlaceLayer.maxDistortion <= 0) {
			currPlaceLayer.computeDistortions();
		}
		if (currPlaceLayer.maxDistortion <= 0) { //no distortions?
			showMessage("No displacements have been computed!", true);
			return false;
		}
		int aIdx = currPlaceTable.getAttrIndex("sum_disto");
		if (aIdx < 0) {
			showMessage("Failed to find the column with total displacements in the table", true);
			return false;
		}
		visualizeDistortions();
		Panel p = new Panel(new ColumnLayout());
		p.add(new Label("Quality improvement", Label.CENTER));
		TextCanvas tc = new TextCanvas();
		tc.addTextLine("The quality of the generalization may be improved by " + "refining the division of the territory.");
		tc.addTextLine("For this purpose, it is reasonable to subdivide the areas (Voronoi cells) " + "where the total displacements (sums of the displacements of the trajectory points inside the areas) " + "and the mean displacements are too high.");
		p.add(tc);
		p.add(new Line(false));
		p.add(new Label("Select the cells where the displacement should be reduced."));
		tc = new TextCanvas();
		tc.addTextLine("You may either apply persistent selection (e.g. by clicking on " + "the areas on the map) or use the Dynamic Query.");
		p.add(tc);
		p.add(new Line(false));
		p.add(new Label("Press \"OK\" when ready."));
		p.add(new Label("Press \"Cancel\" to finish the process.", Label.RIGHT));
		makeUIFrame(p);
		return true;
	}

	protected void visualizeDistortions() {
		int aIdxSum = currPlaceTable.getAttrIndex("sum_disto");
		int aIdxMean = currPlaceTable.getAttrIndex("mean_disto");
		if (aIdxSum < 0 && aIdxMean < 0)
			return;
		Vector vat = new Vector(2, 1);
		if (aIdxSum >= 0) {
			vat.addElement(currPlaceTable.getAttributeId(aIdxSum));
		}
		if (aIdxMean >= 0) {
			vat.addElement(currPlaceTable.getAttributeId(aIdxMean));
		}
		core.getDisplayProducer().displayOnMap("utility_bars", currPlaceTable, vat, currPlaceLayer, mapView);
		Object query = core.getDisplayProducer().applyTool("dynamic_query", currPlaceTable, vat, currPlaceLayer.getContainerIdentifier(), null);
		if (query != null && (query instanceof Component)) {
			Frame fr = CManager.getFrame((Component) query);
			if (fr != null) {
				Dimension sd = Toolkit.getDefaultToolkit().getScreenSize(), wd = fr.getSize();
				fr.setLocation(sd.width - wd.width, sd.height - wd.height - 50);
			}
		}
	}

	protected void makeUIFrame(Component c) {
		selAreasFrame = new OKFrame(this, "Select areas", true);
		selAreasFrame.addContent(c);
		selAreasFrame.pack();
		Dimension sd = Toolkit.getDefaultToolkit().getScreenSize(), wd = selAreasFrame.getSize();
		selAreasFrame.setLocation(sd.width - wd.width, 0);
		selAreasFrame.setVisible(true);
	}

	/**
	 * Finds the manipulator in the given UI component.
	 */
	protected Manipulator findManipulator(Component c) {
		if (c == null)
			return null;
		if (c instanceof Manipulator)
			return (Manipulator) c;
		if (c instanceof Container) {
			Container cont = (Container) c;
			for (int i = 0; i < cont.getComponentCount(); i++) {
				Manipulator m = findManipulator(cont.getComponent(i));
				if (m != null)
					return m;
			}
		}
		return null;
	}

	protected void getSelectedAreasAndRefine() {
		Vector selAreas = null;
		Vector<DPlaceVisitsObject> places = null;
		Highlighter hl = core.getHighlighterForSet(currPlaceLayer.getEntitySetIdentifier());
		if (hl != null) {
			selAreas = hl.getSelectedObjects();
		}
		if (selAreas != null && selAreas.size() < 1) {
			selAreas = null;
		}
		if (currPlaceLayer.areObjectsFiltered() && selAreas != null) {
			Panel p = new Panel(new ColumnLayout());
			p.add(new Label("What areas to refine?"));
			CheckboxGroup cbg = new CheckboxGroup();
			Checkbox fltCB = new Checkbox("all areas satisfying the filter", true, cbg);
			p.add(fltCB);
			Checkbox selCB = new Checkbox(selAreas.size() + " persistently selected areas", false, cbg);
			p.add(selCB);
			OKDialog okd = new OKDialog(core.getUI().getMainFrame(), "Area selection", true);
			okd.addContent(p);
			okd.show();
			if (okd.wasCancelled()) {
				informOwner("cancel");
				return;
			}
			if (fltCB.getState()) {
				selAreas = null;
			}
		}
		if (selAreas != null) {
			places = new Vector<DPlaceVisitsObject>(selAreas.size(), 1);
			for (int i = 0; i < selAreas.size(); i++) {
				GeoObject obj = currPlaceLayer.findObjectById(selAreas.elementAt(i).toString());
				if (obj != null && (obj instanceof DPlaceVisitsObject)) {
					places.addElement((DPlaceVisitsObject) obj);
				}
			}
			if (places.size() < 1) {
				places = null;
			}
		} else {
			places = new Vector<DPlaceVisitsObject>(20, 20);
			for (int i = 0; i < currPlaceLayer.getObjectCount(); i++)
				if (currPlaceLayer.isObjectActive(i)) {
					places.addElement((DPlaceVisitsObject) currPlaceLayer.getObject(i));
				}
		}
		if (places == null || places.size() < 1) {
			showMessage("No areas have been selected!", true);
			Component c = selAreasFrame.getMainComponent();
			makeUIFrame(c);
			return;
		}
		refineSelectedAreas(places);
	}

	/**
	 * The previou layers (for the case if the user wants to restore
	 * the previous state)
	 */
	private DAggregateLinkLayer prevAgLayer = null;
	private DPlaceVisitsLayer prevPlaceLayer = null;
	private DataTable prevAgLinkTable = null, prevPlaceTable = null;

	protected void refineSelectedAreas(Vector<DPlaceVisitsObject> selAreas) {
		if (selAreas == null || selAreas.size() < 1)
			return;
		//float distLowLimit=currPlaceLayer.maxDistortion*0.4f;
		float distLowLimit = selAreas.elementAt(0).meanDistortion;
		for (int i = 1; i < selAreas.size(); i++) {
			float dist = selAreas.elementAt(i).meanDistortion;
			if (dist < distLowLimit) {
				distLowLimit = dist;
			}
		}

		GeneralizationQualityUtil gQ = new GeneralizationQualityUtil(core);
		Vector<PlaceVisitInfo> visits = GeneralizationQualityUtil.getVisitsBigDistortion(selAreas, distLowLimit);
		if (visits == null) {
			informAboutFailure("Failed to extract trajectory points from the areas!");
			return;
		}
		Vector<RealPoint> centres = GeneralizationQualityUtil.getCentresOfBiggestGroups(visits, selAreas, currPlaceLayer.maxDistortion / 2, currPlaceLayer.isGeographic());
		if (centres == null || centres.size() < 1) {
			informAboutFailure("Failed to group the trajectory points extracted from the areas!");
			return;
		}
		VoronoiNew voronoi = gQ.refineTerritoryDivision(currPlaceLayer, centres, placeLayer.maxDistortion / 5);
		if (voronoi == null) {
			informAboutFailure("Failed to build Voronoi polygons!");
			return;
		}
		TrajectoriesGeneraliser trGen = new TrajectoriesGeneraliser();
		trGen.setCore(core);
		boolean ok = trGen.summarizeByPolygons(moveLayer, voronoi.getResultingCells(), voronoi.getNeighbourhoodMap(), agLayer.onlyActiveTrajectories, agLayer.onlyStartsEnds, agLayer.findIntersections, false);
		if (!ok) {
			informAboutFailure("Failed to summarize the data by Voronoi polygons!");
			return;
		}
		Vector<DPlaceVisitsObject> refinedPlaces = trGen.getPlaces();
		if (refinedPlaces == null || refinedPlaces.size() < 1) {
			informAboutFailure("Failed to summarize the data by Voronoi polygons!");
			return;
		}
		ActionDescr aDescr = new ActionDescr();
		aDescr.aName = "Interactively refine the summarization of trajectories";
		aDescr.addParamValue("Trajectories layer id", moveLayer.getContainerIdentifier());
		aDescr.addParamValue("Trajectories layer name", moveLayer.getName());
		aDescr.addParamValue("Areas layer id", currPlaceLayer.getContainerIdentifier());
		aDescr.addParamValue("Areas layer name", currPlaceLayer.getName());
		aDescr.addParamValue("N points added", centres.size());
		aDescr.startTime = System.currentTimeMillis();
		core.logAction(aDescr);
		trGen.makeLayersAndTables(moveLayer, "refined; " + centres.size() + " points added", trGen.getPlaces(), trGen.getAggMoves(), agLayer.onlyActiveTrajectories, agLayer.onlyStartsEnds, agLayer.findIntersections, aDescr);
		DPlaceVisitsLayer refinedPlaceLayer = trGen.getPlaceLayer();
		if (refinedPlaceLayer == null) {
			informAboutFailure("Failed to generate a refined layer!");
			return;
		}
		refinedPlaceLayer.setOrigPlaceLayer(placeLayer);
		refinedPlaceLayer.computeDistortions();

		if (metaData == null) {
			metaData = GeneralizationQualityUtil.makeTableOfChanges(placeLayer);
			metaData.setName("Interactive refinement of " + agLayer.getName());
			core.getDataLoader().addTable(metaData);
		}
		float imprMax = (currPlaceLayer.maxDistortion - refinedPlaceLayer.maxDistortion) / currPlaceLayer.maxDistortion * 100, imprMaxMean = (currPlaceLayer.maxMeanDistortion - refinedPlaceLayer.maxMeanDistortion) / currPlaceLayer.maxMeanDistortion
				* 100, imprMaxSum = (currPlaceLayer.maxSumDistortion - refinedPlaceLayer.maxSumDistortion) / currPlaceLayer.maxSumDistortion * 100, imprSum = (currPlaceLayer.sumDistortion - refinedPlaceLayer.sumDistortion)
				/ currPlaceLayer.sumDistortion * 100, imprMean = (currPlaceLayer.meanDistortion - refinedPlaceLayer.meanDistortion) / currPlaceLayer.meanDistortion * 100;
		++nSteps;
		GeneralizationQualityUtil.addRecordAboutChange(metaData, nSteps, refinedPlaceLayer, imprMaxMean, imprMaxSum, imprMean, imprSum);

		prevAgLayer = currAgLayer;
		prevAgLinkTable = (DataTable) prevAgLayer.getThematicData();
		prevPlaceLayer = currPlaceLayer;
		prevPlaceTable = currPlaceTable;

		if (!currPlaceLayer.equals(placeLayer)) {
			core.removeMapLayer(currAgLayer.getContainerIdentifier(), true);
			core.removeMapLayer(currPlaceLayer.getContainerIdentifier(), true);
		} else {
			placeLayer.setLayerDrawn(false);
			agLayer.setLayerDrawn(false);
		}
		currAgLayer = trGen.getAggLinkLayer();
		currPlaceLayer = refinedPlaceLayer;
		currPlaceTable = (DataTable) currPlaceLayer.getThematicData();
		visualizeDistortions();

		Panel p = new Panel(new ColumnLayout());
		p.add(new Label("Results of the refinement:", Label.CENTER));
		p.add(showRefinementResults(currPlaceLayer, imprMax, imprMaxMean, imprMaxSum, imprMean, imprSum));
		p.add(new Line(false));
		p.add(new Label("What would you like to do next?"));
		p.add(makeSelectionPanel());

		askContinueFrame = new OKFrame(this, "Continue the process?", false);
		askContinueFrame.addContent(p);
		askContinueFrame.pack();
		Dimension sd = Toolkit.getDefaultToolkit().getScreenSize(), wd = askContinueFrame.getSize();
		askContinueFrame.setLocation(sd.width - wd.width, 0);
		askContinueFrame.setVisible(true);
	}

	private void informAboutFailure(String message) {
		showMessage(message, true);
		Panel p = new Panel(new ColumnLayout());
		Label l = new Label(message);
		l.setBackground(Color.red.darker());
		l.setForeground(Color.yellow);
		p.add(l);
		p.add(new Line(false));
		p.add(new Label("What would you like to do next?"));
		p.add(makeSelectionPanel());
		askContinueFrame = new OKFrame(this, "Continue the process?", false);
		askContinueFrame.addContent(p);
		askContinueFrame.pack();
		Dimension sd = Toolkit.getDefaultToolkit().getScreenSize(), wd = askContinueFrame.getSize();
		askContinueFrame.setLocation(sd.width - wd.width, 0);
		askContinueFrame.setVisible(true);
	}

	private Checkbox finishCB = null, restorePreviousCB = null, furtherRefineCB = null;
	private Panel selectionPanel = null;

	private Panel makeSelectionPanel() {
		if (selectionPanel != null) {
			finishCB.setState(true);
			restorePreviousCB.setState(false);
			furtherRefineCB.setState(false);
			return selectionPanel;
		}
		CheckboxGroup cbg = new CheckboxGroup();
		selectionPanel = new Panel(new ColumnLayout());
		finishCB = new Checkbox("finish the process and keep the last result", true, cbg);
		selectionPanel.add(finishCB);
		restorePreviousCB = new Checkbox("restore the previous state", false, cbg);
		selectionPanel.add(restorePreviousCB);
		furtherRefineCB = new Checkbox("further refine the last result", false, cbg);
		selectionPanel.add(furtherRefineCB);
		TextCanvas tc = new TextCanvas();
		tc.setText("If you choose the last option, select the areas to refine by means of " + "persistent selection or set the lower limit for the displacement using the focuser " + "in the map manipulator.");
		selectionPanel.add(tc);
		return selectionPanel;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(selAreasFrame) && e.getActionCommand().equals("closed")) {
			if (selAreasFrame.wasCancelled())
				if (currPlaceLayer.equals(placeLayer)) {
					informOwner("cancel");
				} else {
					finish();
				}
			else {
				getSelectedAreasAndRefine();
			}
		} else if (e.getSource().equals(askContinueFrame) && e.getActionCommand().equals("closed")) {
			if (finishCB.getState()) {
				finish();
			} else if (restorePreviousCB.getState()) {
				restorePreviousState();
			} else {
				getSelectedAreasAndRefine();
			}
		}
	}

	protected void finish() {
		float imprMaxMean = (placeLayer.maxMeanDistortion - currPlaceLayer.maxMeanDistortion) / placeLayer.maxMeanDistortion * 100, imprMaxSum = (placeLayer.maxSumDistortion - currPlaceLayer.maxSumDistortion) / placeLayer.maxSumDistortion * 100, imprSum = (placeLayer.sumDistortion - currPlaceLayer.sumDistortion)
				/ placeLayer.sumDistortion * 100, imprMean = (placeLayer.meanDistortion - currPlaceLayer.meanDistortion) / placeLayer.meanDistortion * 100;
		GeneralizationQualityUtil.addRecordAboutChange(metaData, nSteps, currPlaceLayer, imprMaxMean, imprMaxSum, imprMean, imprSum);
		String layerNames[] = { agLayer.getName() + " (refined)", placeLayer.getName() + " (refined; " + (currPlaceLayer.getObjectCount() - placeLayer.getObjectCount()) + " areas added)" };
		String labels[] = { "Aggregate moves:", "Generalized places:" };
		String editedNames[] = Dialogs.editStringValues(core.getUI().getMainFrame(), labels, layerNames, "Edit the names of the resulting layers", "Resulting layer names", false);
		if (editedNames == null) {
			editedNames = layerNames;
		}
		currAgLayer.setName(editedNames[0]);
		currAgLayer.getThematicData().setName(editedNames[0]);
		currPlaceLayer.setName(editedNames[1]);
		currPlaceTable.setName(editedNames[1]);
		currAgLayer.setTrajectoryLayer(moveLayer);
		currPlaceLayer.setTrajectoryLayer(moveLayer);
		informOwner("finish");
	}

	protected void restorePreviousState() {
		if (!currPlaceLayer.equals(placeLayer)) {
			core.removeMapLayer(currAgLayer.getContainerIdentifier(), true);
			core.removeMapLayer(currPlaceLayer.getContainerIdentifier(), true);
		}
		currAgLayer = prevAgLayer;
		currPlaceLayer = prevPlaceLayer;
		currPlaceTable = prevPlaceTable;
		if (!currAgLayer.equals(agLayer)) {
			DataLoader dl = core.getDataLoader();
			int tblN = dl.addTable(currPlaceTable);
			dl.addMapLayer(currPlaceLayer, 0);
			dl.linkTableToMapLayer(tblN, currPlaceLayer);
			tblN = dl.addTable(prevAgLinkTable);
			dl.addMapLayer(currAgLayer, 0);
			dl.linkTableToMapLayer(tblN, currAgLayer);
		}
		--nSteps;
		GeneralizationQualityUtil.addRecordAboutChange(metaData, nSteps, currPlaceLayer, 0, 0, 0, 0);
		startWork();
	}

	/**
	 * Constructs a panel with the information about the refinement results
	 */
	protected Panel showRefinementResults(DPlaceVisitsLayer placeLayer, float imprMax, float imprMaxMean, float imprMaxSum, float imprMean, float imprSum) {
		Panel p = new Panel();
		GridBagLayout gridbag = new GridBagLayout();
		p.setLayout(gridbag);
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0f;
		c.weighty = 1.0f;
		c.fill = GridBagConstraints.HORIZONTAL;
		Label l = new Label("");
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label("");
		c.gridwidth = 2;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label("Improvement, %");
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label("Maximum displacement:");
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label(String.valueOf(placeLayer.maxDistortion));
		c.gridwidth = 2;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label(String.valueOf(imprMax));
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label("Maximum mean displacement in area:");
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label(String.valueOf(placeLayer.maxMeanDistortion));
		c.gridwidth = 2;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label(String.valueOf(imprMaxMean));
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label("Maximum total displacement in area:");
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label(String.valueOf(placeLayer.maxSumDistortion));
		c.gridwidth = 2;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label(String.valueOf(imprMaxSum));
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label("Overall mean displacement:");
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label(String.valueOf(placeLayer.meanDistortion));
		c.gridwidth = 2;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label(String.valueOf(imprMean));
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label("Overall total displacement:");
		c.gridwidth = 3;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label(String.valueOf(placeLayer.sumDistortion));
		c.gridwidth = 2;
		gridbag.setConstraints(l, c);
		p.add(l);
		l = new Label(String.valueOf(imprSum));
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, c);
		p.add(l);
		return p;
	}

	/**
	 * @param eventId - one of "cancel", "fail", or "finish"
	 */
	protected void informOwner(String eventId) {
		if (owner != null && eventId != null) {
			owner.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, eventId));
		}
	}

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
	}

	public DAggregateLinkLayer getRefinedLinkLayer() {
		return currAgLayer;
	}

	public DPlaceVisitsLayer getRefinedPlaceLayer() {
		return currPlaceLayer;
	}

	public DPlaceVisitsLayer getOrigPlaceLayer() {
		return placeLayer;
	}
}
