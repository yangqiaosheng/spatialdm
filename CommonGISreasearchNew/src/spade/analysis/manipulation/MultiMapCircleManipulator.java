package spade.analysis.manipulation;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.FoldablePanel;
import spade.lib.lang.Language;
import spade.vis.database.AttributeDataPortion;
import spade.vis.mapvis.CirclePresenter;
import spade.vis.mapvis.MultiMapCircleDrawer;

/**
* A manipulator for "small multiples" with circles. Creates a MultiMapVisComparison
* instance and adds to it specific controls for circles.
*/
public class MultiMapCircleManipulator extends Panel implements Manipulator, ItemListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.manipulation.Res");
	/**
	* The visualizer to be manipulated. This must be a MultiMapCircleDrawer.
	*/
	protected MultiMapCircleDrawer mcDrawer = null;

	/**
	* Construction of map manipulator. Returns true if successfully constructed.
	* Arguments: 1) supervisor responsible for dynamic linking of displays;
	* 2) visualizer or classifier to be manipulated; 3) table with source data;
	* 4) table filter
	* The Manipulator must check if the visualizer has the appropriate type.
	* For MultiMapManipulator visualizer should be an instance of MultiMapNumDrawer.
	*/
	@Override
	public boolean construct(Supervisor sup, Object visualizer, AttributeDataPortion dataTable) {
		if (visualizer == null || dataTable == null)
			return false;
		if (!(visualizer instanceof MultiMapCircleDrawer))
			return false;
		mcDrawer = (MultiMapCircleDrawer) visualizer;
		MultiMapVisComparison mmcomp = new MultiMapVisComparison();
		if (!mmcomp.construct(sup, mcDrawer, dataTable))
			return false;
		setLayout(new BorderLayout());
		add(mmcomp, BorderLayout.CENTER);
		// Option switcher
		Label l = new Label(res.getString("Visualisation_options"));
		l.setBackground(Color.getHSBColor(0.0f, 0.0f, 0.8f));
		//l.setSize(50,15); //?
		Panel popt = new Panel(new GridLayout(2, 1));
		FoldablePanel fpOption = new FoldablePanel(popt, l);
		add(fpOption, BorderLayout.SOUTH);
		CheckboxGroup cbgOptions = new CheckboxGroup();
		Checkbox cb = new Checkbox(null, mcDrawer.getSignInstance().getUsesMinSize(), cbgOptions);
		cb.setName("use_min_diam");
		cb.addItemListener(this);
		Panel p = new Panel(new FlowLayout(FlowLayout.LEFT));
		p.add(cb);
		p.add(new PieOption(true));
		popt.add(p);
		cb = new Checkbox(null, !mcDrawer.getSignInstance().getUsesMinSize(), cbgOptions);
		cb.setName("use_max_diam");
		cb.addItemListener(this);
		p = new Panel(new FlowLayout(FlowLayout.LEFT));
		p.add(cb);
		p.add(new PieOption(false));
		popt.add(p);
		return true;
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		Object objSrc = ie.getSource();
		if (objSrc instanceof Checkbox) {
			Checkbox cb = (Checkbox) objSrc;
			if (!cb.getState())
				return;
			CirclePresenter cpr = (CirclePresenter) mcDrawer.getSingleMapVisualizer();
			if (cpr == null)
				return;
			if (cb.getName().equalsIgnoreCase("use_min_diam")) {
				cpr.setUseMinSize(true);
			} else if (cb.getName().equalsIgnoreCase("use_max_diam")) {
				cpr.setUseMinSize(false);
			}
			mcDrawer.notifyVisChange();
			return;
		}

	}
}