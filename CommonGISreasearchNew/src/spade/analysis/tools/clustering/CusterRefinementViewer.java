package spade.analysis.tools.clustering;

import it.unipi.di.sax.kmedoids.KMedoids;

import java.awt.Checkbox;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Line;
import spade.lib.util.IntArray;
import spade.vis.action.Highlighter;
import spade.vis.database.ObjectFilter;
import spade.vis.database.ObjectFilterBySelection;
import spade.vis.dmap.DGeoLayer;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jan 16, 2009
 * Time: 5:53:04 PM
 * Allows the user to view the suggested cluster refinement before
 * storing the new clusters.
 */
public class CusterRefinementViewer extends Panel implements Destroyable, ItemListener {
	/**
	 * Used for an access to the highlighter etc.
	 */
	protected ESDACore core = null;
	/**
	 * The result of cluster refinement
	 */
	protected HashMap<DClusterObject, ArrayList<DClusterObject>> refinement = null;
	/**
	 * The centroids of the clusters
	 */
	protected Vector<DClusterObject> centroids = null;
	/**
	 * The map layer used for showing the clusters
	 */
	protected DGeoLayer layer = null;
	/**
	 * The original filter of the layer (temporarily removed,
	 * to be restored afterwards).
	 */
	protected ObjectFilter origLayerFilter = null;
	/**
	 * Used for showing only objects of selected clusters
	 */
	protected ObjectFilterBySelection clFilter = null;

	protected Checkbox clCB[] = null;
	protected Checkbox hlCentroidsCB = null;

	public CusterRefinementViewer(HashMap<DClusterObject, ArrayList<DClusterObject>> refinement, KMedoids<DClusterObject> kMedoids, DGeoLayer layer, ESDACore core) {
		this.refinement = refinement;
		this.layer = layer;
		this.core = core;
		if (refinement == null || layer == null || refinement.size() < 2)
			return;
		centroids = new Vector(refinement.keySet().size(), 10);
		for (DClusterObject centre : refinement.keySet()) {
			if (centre == null) {
				continue;
			}
			centroids.addElement(centre);
		}
		clCB = new Checkbox[centroids.size()];
		setLayout(new GridLayout(centroids.size() + 3, 1));
		add(new Label("Check the results of the refinement:"));
		for (int i = 0; i < centroids.size(); i++) {
			ArrayList<DClusterObject> cluster = refinement.get(centroids.elementAt(i));
			clCB[i] = new Checkbox((i + 1) + " (" + cluster.size() + " objects; radius = " + kMedoids.getClusterRadius(cluster, centroids.elementAt(i)) + ")", true);
			clCB[i].addItemListener(this);
			add(clCB[i]);
		}
		add(new Line(false));
		hlCentroidsCB = new Checkbox("Highlight cluster centroids", true);
		hlCentroidsCB.addItemListener(this);
		add(hlCentroidsCB);
		highlightCentroids();

		origLayerFilter = layer.getObjectFilter();
		layer.removeObjectFilter(origLayerFilter);
		clFilter = new ObjectFilterBySelection();
		clFilter.setObjectContainer(layer);
		clFilter.setEntitySetIdentifier(layer.getEntitySetIdentifier());
		layer.setObjectFilter(clFilter);
		applyFilter();
	}

	protected void highlightCentroids() {
		Highlighter hl = core.getHighlighterForSet(layer.getEntitySetIdentifier());
		if (hlCentroidsCB.getState()) {
			Vector selObj = new Vector(centroids.size(), 1);
			for (int i = 0; i < centroids.size(); i++) {
				selObj.addElement(centroids.elementAt(i).id);
			}
			hl.replaceSelectedObjects(this, selObj);
		} else {
			hl.clearSelection(this);
		}
	}

	protected void applyFilter() {
		IntArray idxs = new IntArray(refinement.size(), 100);
		for (int i = 0; i < clCB.length; i++)
			if (clCB[i].getState()) {
				ArrayList<DClusterObject> cluster = refinement.get(centroids.elementAt(i));
				if (cluster == null) {
					continue;
				}
				for (DClusterObject member : cluster) {
					idxs.addElement(member.idx);
				}
			}
		clFilter.setActiveObjectIndexes(idxs);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(hlCentroidsCB)) {
			highlightCentroids();
		} else {
			applyFilter();
		}
	}

	protected boolean destroyed = false;

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	@Override
	public void destroy() {
		if (destroyed)
			return;
		if (layer != null) {
			if (clFilter != null) {
				layer.removeObjectFilter(clFilter);
				clFilter.destroy();
				clFilter = null;
			}
			if (origLayerFilter != null) {
				layer.setObjectFilter(origLayerFilter);
			}
		}
		Highlighter hl = core.getHighlighterForSet(layer.getEntitySetIdentifier());
		hl.clearSelection(this);
		destroyed = true;
	}
}
