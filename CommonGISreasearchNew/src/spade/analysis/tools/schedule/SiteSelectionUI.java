package spade.analysis.tools.schedule;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.plot.GeoObjectSelector;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.lang.Language;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DLayerManager;
import spade.vis.map.MapViewer;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 26-Feb-2008
 * Time: 18:05:36
 * Allows the user to select sites from a map layer through a map or through a list.
 * The map and the list are linked.
 */
public class SiteSelectionUI extends Panel implements Destroyable {
	static ResourceBundle res = Language.getTextResource("spade.analysis.tools.schedule.SchedulerTexts_tools_schedule");

	protected Vector suitableLayers = null, locations = null;
	protected DGeoLayer locLayer = null;
	protected GeoObjectSelector locList = null;
	protected Choice layerCh = null;
	protected Checkbox locLayerCB = null, otherLayerCB = null;
	protected Checkbox clickMapCB = null;
	protected boolean destroyed = false;

	public SiteSelectionUI(Vector suitableLayers, Vector locations, DGeoLayer locLayer, DLayerManager lman, Supervisor supervisor, MapViewer mapView) {
		this.suitableLayers = suitableLayers;
		this.locations = locations;
		this.locLayer = locLayer;
		if (locations != null) {
			setLayout(new BorderLayout());
		} else {
			setLayout(new ColumnLayout());
		}
		CheckboxGroup cbg = new CheckboxGroup();
		clickMapCB = new Checkbox(res.getString("enter_sites_click_map"), locations == null, cbg);
		if (locations != null) {
			locLayerCB = new Checkbox(res.getString("take_sites_from") + " " + locLayer.getName(), true, cbg);
			add(locLayerCB, BorderLayout.NORTH);
			locList = new GeoObjectSelector(locLayer, locations, lman, supervisor, true);
			locList.setMapView(mapView);
			add(locList, BorderLayout.CENTER);
			if (suitableLayers == null) {
				add(clickMapCB, BorderLayout.SOUTH);
			}
		}
		if (suitableLayers != null) {
			otherLayerCB = new Checkbox(res.getString("take_sites_from") + " ", false, cbg);
			layerCh = new Choice();
			for (int i = 0; i < suitableLayers.size(); i++) {
				DGeoLayer layer = (DGeoLayer) suitableLayers.elementAt(i);
				layerCh.add(layer.getName());
			}
			Panel otherLayersPan = new Panel(new BorderLayout());
			otherLayersPan.add(otherLayerCB, BorderLayout.WEST);
			otherLayersPan.add(layerCh, BorderLayout.CENTER);
			Panel p = new Panel(new GridLayout(2, 1));
			p.add(otherLayersPan);
			p.add(clickMapCB);
			if (locations == null) {
				add(p);
			} else {
				add(p, BorderLayout.SOUTH);
			}
		}
	}

	public SiteSelectionUI(DGeoLayer locLayer, DLayerManager lman, Supervisor supervisor, MapViewer mapView) {
		this.locLayer = locLayer;
		locList = new GeoObjectSelector(locLayer, null, lman, supervisor, true);
		locList.setMapView(mapView);
		setLayout(new BorderLayout());
		add(new Label(res.getString("Select_the_sites") + ":"), BorderLayout.NORTH);
		add(locList, BorderLayout.CENTER);
	}

	public Vector getSuitableLayers() {
		return suitableLayers;
	}

	public Vector getLocations() {
		return locations;
	}

	public DGeoLayer getLocLayer() {
		return locLayer;
	}

	public GeoObjectSelector getLocList() {
		return locList;
	}

	@Override
	public void destroy() {
		if (destroyed)
			return;
		if (locList != null) {
			locList.destroy();
		}
		destroyed = true;
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	public boolean wishClickMap() {
		return clickMapCB != null && clickMapCB.getState();
	}

	public boolean sitesSelectedFromLocLayer() {
		return locLayerCB != null && locLayerCB.getState();
	}

	public DGeoLayer getSelectedLayer() {
		if (suitableLayers != null && otherLayerCB != null && otherLayerCB.getState()) {
			int idx = layerCh.getSelectedIndex();
			if (idx < 0)
				return null;
			return (DGeoLayer) suitableLayers.elementAt(idx);
		}
		return null;
	}
}
