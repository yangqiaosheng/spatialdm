package spade.analysis.manipulation;

import java.awt.Checkbox;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.Destroyable;
import spade.lib.lang.Language;
import spade.vis.database.AttributeDataPortion;
import spade.vis.mapvis.SelectiveDrawingController;
import spade.vis.mapvis.SignDrawer;

/**
* Allows the user to swith to a special mode of object representation on a map
* when only selected objects are represented
*/
public class SelectiveDrawingControlPanel extends Panel implements ItemListener, PropertyChangeListener, Destroyable, Manipulator {
	static ResourceBundle res = Language.getTextResource("spade.analysis.manipulation.Res");

	protected Checkbox cb = null;
	protected boolean destroyed = false;

	/**
	* The controller of selective sign drawing (i.e. when the visualizer
	* generates representations not for all objects but only for selected ones).
	* Such a controller must always exist for each SignDrawer
	*/
	protected SelectiveDrawingController controller = null;

	protected SignDrawer sd = null;

	/**
	* Construction of map manipulator. Returns true if successfully constructed.
	* Arguments: 1) supervisor responsible for dynamic linking of displays;
	* 2) visualizer or classifier to be manipulated; 3) table with source data;
	* 4) table filter
	* The Manipulator must check if the visualizer has the appropriate type.
	*/
	@Override
	public boolean construct(Supervisor sup, Object visualizer, AttributeDataPortion table) {
		if (sup == null || visualizer == null || table == null || !(visualizer instanceof SignDrawer))
			return false;
		sd = (SignDrawer) visualizer;
		controller = sd.getSelectiveDrawingController();
		controller.setHighlighter(sup.getHighlighter(table.getEntitySetIdentifier()));
		controller.addPropertyChangeListener(this);
		setLayout(new GridLayout(1, 1));
		// following text: "show only selected objects"
		cb = new Checkbox(res.getString("show_only_selected"));
		cb.addItemListener(this);
		add(cb);
		return true;
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		controller.setDrawSelectedOnly(cb.getState());
	}

	/**
	* This method gets called when the drawing mode (selective/not selective) is
	* changed.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getSource().equals(controller) && controller.getDrawSelectedOnly() != cb.getState()) {
			cb.setState(controller.getDrawSelectedOnly());
		}
	}

	@Override
	public void destroy() {
		controller.setDrawSelectedOnly(false);
		destroyed = true;
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}
}
