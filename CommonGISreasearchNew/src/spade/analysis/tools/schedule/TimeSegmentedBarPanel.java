package spade.analysis.tools.schedule;

import java.awt.BorderLayout;
import java.awt.Panel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;

import spade.lib.basicwin.SplitLayout;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;
import spade.time.TimeIntervalSelector;
import spade.time.TimeMoment;
import spade.time.vis.TimeAndItemsSelectListener;
import spade.time.vis.TimeLineLabelsCanvas;
import spade.time.vis.TimeSegmentedBarCanvas;
import spade.time.vis.TimeSegmentedBarLegendCanvas;
import spade.vis.action.Highlighter;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 09-Mar-2007
 * Time: 10:24:11
 * To change this template use File | Settings | File Templates.
 */
public class TimeSegmentedBarPanel extends Panel implements PropertyChangeListener {

	static ResourceBundle res = Language.getTextResource("spade.analysis.tools.schedule.SchedulerTexts_tools_schedule");

	protected VehicleCounter vehicleCounter = null;
	protected DestinationUseCounter destUseCounter = null;
	protected TimeSegmentedBarCanvas vActCanvas = null, vLoadCanvas = null, pActCanvas = null, destUseCanvas = null;
	protected TimeSegmentedBarLegendCanvas vActLCanvas = null, vLoadLCanvas = null, pActLCanvas = null, destUseLCanvas = null;
	protected TimeLineLabelsCanvas timeLabelsCanvas = null;

	public TimeSegmentedBarPanel(VehicleCounter vc, DestinationUseCounter destUseCounter, ItemCategorySelector iCatSelector) {
		super();
		this.vehicleCounter = vc;
		this.destUseCounter = destUseCounter;
		int catIdx = -1;
		String catName = null;
		if (iCatSelector != null) {
			catIdx = iCatSelector.getSelectedCategoryIndex();
			catName = iCatSelector.getSelectedCategory();
		}
		int nGraphs = 0;
		if (catName == null) {
			catName = res.getString("all_cat");
		}
		VehicleActivityData vad = (catIdx >= 0) ? vc.getSuitableVehicleActivities(catName) : vc.getFullActivityData();
		vActCanvas = new TimeSegmentedBarCanvas(true, VehicleCounter.activity_colors, res.getString("Vehicles_for") + " " + catName, VehicleCounter.activity_names, vad.vehicleIds, vc.times, vad.vehicleActivity, null, vad.statesBefore, null,
				vad.statesAfter, null);
		++nGraphs;
		VehicleLoadData vld = vc.capUseAllCat;
		if (catIdx >= 0 && vc.capUses != null && vc.capUses.size() > catIdx) {
			vld = (VehicleLoadData) vc.capUses.elementAt(catIdx);
		}
		if (vld != null) {
			vLoadCanvas = new TimeSegmentedBarCanvas(true, VehicleLoadData.capacity_colors, res.getString("Fleet_capacities_for") + " " + catName, VehicleLoadData.capacity_labels, vld.vehicleIds, vc.times, vld.capCats, vld.capSizes,
					vld.capCatsBefore, vld.capSizesBefore, vld.capCatsAfter, vld.capSizesAfter);
			++nGraphs;
		}
		if (destUseCounter != null) {
			CapacityUseData capUseData = destUseCounter.getCapacityUseData((catIdx >= 0) ? catName : null);
			if (capUseData == null && catIdx >= 0) {
				capUseData = destUseCounter.getCapacityUseData(null);
			}
			if (capUseData != null) {
				destUseCanvas = new TimeSegmentedBarCanvas(true, CapacityUseData.state_colors, res.getString("Capacities_in_destinations_for") + " " + catName, CapacityUseData.state_names, capUseData.ids, capUseData.times, capUseData.states,
						capUseData.fills, capUseData.statesBefore, capUseData.fillsBefore, capUseData.statesAfter, capUseData.fillsAfter);
				++nGraphs;
			}
		}
		if (vc.hasItemData()) {
			pActCanvas = new TimeSegmentedBarCanvas(true, vc.getItemColors(), catName, vc.getItemComments(), vc.getItemIDs(null), vc.times, vc.getItemStates(null), vc.getItemCountsByTime(null), vc.getItemStatesBefore(null), vc.getItemCounts(null),
					vc.getItemStatesAfter(null), vc.getItemCounts(null));
			++nGraphs;
		}

		setLayout(new BorderLayout());
		TimeLineLabelsCanvas ruler = new TimeLineLabelsCanvas();
		ruler.setTimeInterval((TimeMoment) vc.times.elementAt(0), (TimeMoment) vc.times.elementAt(vc.times.size() - 1));
		add(ruler, BorderLayout.NORTH);
		if (vLoadCanvas == null) {
			Panel pc = new Panel(new BorderLayout());
			pc.add(vActCanvas, BorderLayout.CENTER);
			pc.add(vActLCanvas = new TimeSegmentedBarLegendCanvas(VehicleCounter.activity_colors, res.getString("Fleet_activities"), VehicleCounter.activity_names), BorderLayout.EAST);
			add(pc, BorderLayout.CENTER);
			ruler.setRulerController(vActCanvas);
		} else {
			Panel splp = new Panel();
			SplitLayout spl = new SplitLayout(splp, SplitLayout.HOR);
			splp.setLayout(spl);
			add(splp, BorderLayout.CENTER);
			if (pActCanvas != null) {
				Panel pc = new Panel(new BorderLayout());
				pc.add(pActCanvas, BorderLayout.CENTER);
				pc.add(pActLCanvas = new TimeSegmentedBarLegendCanvas(vc.getItemColors(), res.getString("Items_state"), vc.getItemComments()), BorderLayout.EAST);
				spl.addComponent(pc, 1f / nGraphs);
				pActLCanvas.setMax(pActCanvas.getMaxSum());
				if (!ruler.hasRulerController()) {
					ruler.setRulerController(pActCanvas);
				}
			}
			if (destUseCanvas != null) {
				Panel pc = new Panel(new BorderLayout());
				pc.add(destUseCanvas, BorderLayout.CENTER);
				pc.add(destUseLCanvas = new TimeSegmentedBarLegendCanvas(CapacityUseData.state_colors, res.getString("Destinations_use"), CapacityUseData.state_names), BorderLayout.EAST);
				spl.addComponent(pc, 1f / nGraphs);
				destUseLCanvas.setMax(destUseCanvas.getMaxSum());
				if (!ruler.hasRulerController()) {
					ruler.setRulerController(destUseCanvas);
				}
			}
			if (vLoadCanvas != null) {
				Panel pc = new Panel(new BorderLayout());
				pc.add(vLoadCanvas, BorderLayout.CENTER);
				pc.add(vLoadLCanvas = new TimeSegmentedBarLegendCanvas(VehicleLoadData.capacity_colors, res.getString("Fleet_use"), VehicleLoadData.capacity_labels), BorderLayout.EAST);
				vLoadLCanvas.setMax(vLoadCanvas.getMaxSum());
				spl.addComponent(pc, 1f / nGraphs);
				if (!ruler.hasRulerController()) {
					ruler.setRulerController(vLoadCanvas);
				}
			}
			if (vActCanvas != null) {
				Panel pc = new Panel(new BorderLayout());
				pc.add(vActCanvas, BorderLayout.CENTER);
				pc.add(vActLCanvas = new TimeSegmentedBarLegendCanvas(VehicleCounter.activity_colors, res.getString("Fleet_activities"), VehicleCounter.activity_names), BorderLayout.EAST);
				vActLCanvas.setMax(vActCanvas.getMaxSum());
				spl.addComponent(pc, 1f / nGraphs);
				if (!ruler.hasRulerController()) {
					ruler.setRulerController(vActCanvas);
				}
			}
		}
		if (iCatSelector != null) {
			iCatSelector.addCategoryChangeListener(this);
			ItemCategorySelectUI iCatSel = new ItemCategorySelectUI(iCatSelector);
			add(iCatSel, BorderLayout.SOUTH);
		}
		/*
		colors=new Color[2];
		colors[0]=Color.blue; colors[1]=Color.cyan;
		int counts[][]=null, states[][]=null;
		for (int i=0; i<vehicleCounter.loads.size(); i++) {
		  str=new String[2];
		  str[0]=""; str[1]=(String)vehicleCounter.itemCategories.elementAt(i);
		  counts=(int[][])vehicleCounter.loads.elementAt(i);
		  states=computeStatesFromCounts(counts);
		  spl.addComponent(new TimeSegmentedBarCanvas(true,VehicleCounter.activity_colors,
		  null,VehicleCounter.activity_names,vehicleCounter.vehicleIds,vehicleCounter.times,states,counts),0.5f/vehicleCounter.loads.size());
		}
		*/
	}

	public void setHighlighterForVehicles(Highlighter highlighter) {
		vActCanvas.setHighlighter(highlighter);
		if (vLoadCanvas != null) {
			vLoadCanvas.setHighlighter(highlighter);
		}
	}

	public void setHighlighterForItems(Highlighter highlighter) {
		if (pActCanvas != null) {
			pActCanvas.setHighlighter(highlighter);
		}
	}

	public void setHighlighterForDestinations(Highlighter highlighter) {
		if (destUseCanvas != null) {
			destUseCanvas.setHighlighter(highlighter);
		}
	}

	/**
	 * Adds a TimeAndItemsSelectListener, which listens to simultaneous selections
	 * of subsets of items (specified by their identifiers) and time intervals
	 * (specified by the start and end time moments).
	 * Each TimeAndItemsSelectListener receives events when the user clicks on
	 * bar segments
	 */
	public void addTimeAndItemsSelectListener(TimeAndItemsSelectListener listener) {
		vActCanvas.addTimeAndItemsSelectListener(listener);
		if (vLoadCanvas != null) {
			vLoadCanvas.addTimeAndItemsSelectListener(listener);
		}
	}

	/**
	 * Removes the listener of the time and item selection events
	 */
	public void removeTimeAndItemsSelectListener(TimeAndItemsSelectListener listener) {
		vActCanvas.removeTimeAndItemsSelectListener(listener);
		if (vLoadCanvas != null) {
			vLoadCanvas.removeTimeAndItemsSelectListener(listener);
		}
	}

	private int[][] computeStatesFromCounts(int counts[][]) {
		int states[][] = new int[counts.length][];
		for (int i = 0; i < states.length; i++) {
			states[i] = new int[counts[i].length];
			for (int j = 0; j < states[i].length; j++) {
				states[i][j] = (counts[i][j] == 0) ? 0 : 1;
			}
		}
		return states;
	}

	/**
	 * Used for the selection of time intervals by clicking on bars of the plot
	 */
	public void setTimeIntervalSelector(TimeIntervalSelector timeIntSel) {
		if (vActCanvas != null) {
			vActCanvas.setTimeIntervalSelector(timeIntSel);
		}
		if (vLoadCanvas != null) {
			vLoadCanvas.setTimeIntervalSelector(timeIntSel);
		}
		if (pActCanvas != null) {
			pActCanvas.setTimeIntervalSelector(timeIntSel);
		}
		if (destUseCanvas != null) {
			destUseCanvas.setTimeIntervalSelector(timeIntSel);
		}
	}

	/**
	 * Reacts to changes of the selected item category
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getPropertyName().equals("category_selection")) {
			String currItemCat = (String) e.getNewValue();
			int idx = -1;
			if (currItemCat != null) {
				idx = StringUtil.indexOfStringInVectorIgnoreCase(currItemCat, vehicleCounter.itemCategories);
			} else {
				currItemCat = res.getString("all_cat");
			}
			VehicleActivityData vad = (idx >= 0) ? vehicleCounter.getSuitableVehicleActivities(currItemCat) : vehicleCounter.getFullActivityData();
			if (vad != null) {
				vActCanvas.setData(vad.vehicleIds, vad.vehicleActivity, null, vad.statesBefore, null, vad.statesAfter, null, res.getString("Vehicles_for") + " " + currItemCat);
			} else {
				vActCanvas.setData(null, null, null, res.getString("Vehicles_for") + " " + currItemCat);
			}
			if (vLoadCanvas != null) {
				VehicleLoadData vld = (idx >= 0) ? (idx < vehicleCounter.capUses.size()) ? (VehicleLoadData) vehicleCounter.capUses.elementAt(idx) : null : vehicleCounter.capUseAllCat;
				if (vld != null) {
					vLoadCanvas.setData(vld.vehicleIds, vld.capCats, vld.capSizes, vld.capCatsBefore, vld.capSizesBefore, vld.capCatsAfter, vld.capSizesAfter, res.getString("Fleet_capacities_for") + " " + currItemCat);
				} else {
					vLoadCanvas.setData(null, null, null, res.getString("Fleet_capacities_for") + " " + currItemCat);
				}
			}
			if (pActCanvas != null) {
				String cat = (idx == -1) ? null : currItemCat;
				pActCanvas.setData(vehicleCounter.getItemIDs(cat), vehicleCounter.getItemStates(cat), vehicleCounter.getItemCountsByTime(cat), vehicleCounter.getItemStatesBefore(cat), vehicleCounter.getItemCounts(cat),
						vehicleCounter.getItemStatesAfter(cat), vehicleCounter.getItemCounts(cat), currItemCat);
			}
			if (destUseCanvas != null) {
				CapacityUseData capUseData = destUseCounter.getCapacityUseData((idx >= 0) ? currItemCat : null);
				if (capUseData != null) {
					destUseCanvas.setData(capUseData.ids, capUseData.states, capUseData.fills, capUseData.statesBefore, capUseData.fillsBefore, capUseData.statesAfter, capUseData.fillsAfter, res.getString("Destinations_for") + " " + currItemCat);
				} else {
					destUseCanvas.setData(null, null, null, res.getString("Capacities_in_destinations_for") + " " + currItemCat);
				}
			}
			vActLCanvas.setMax(vActCanvas.getMaxSum());
			vLoadLCanvas.setMax(vLoadCanvas.getMaxSum());
			pActLCanvas.setMax(pActCanvas.getMaxSum());
			if (destUseCanvas != null) {
				destUseLCanvas.setMax(destUseCanvas.getMaxSum());
			}
		}
	}

}
