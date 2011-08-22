package spade.analysis.manipulation;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Metrics;
import spade.lib.color.ColorCanvas;
import spade.lib.color.ColorDlg;
import spade.lib.color.ColorListener;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeDataPortion;
import spade.vis.mapvis.AttrColorHandler;
import spade.vis.mapvis.ListPresenter;

/**
* Manipulates representation of data by "stacks", i.e. overlapping rectangles
* representing multiple items.
*/
public class StackManipulator extends Panel implements Manipulator, ItemListener, ActionListener, ColorListener, PropertyChangeListener, Destroyable {
	static ResourceBundle res = Language.getTextResource("spade.analysis.manipulation.Res");
	/**
	* The ListPresenter (the ancestor of StackDrawer) to manipulate
	*/
	protected ListPresenter lpres = null;
	/**
	* The list of items (may be switched on/off or recolored)
	*/
	protected Vector items = null;
	/**
	* The list of colors for the items
	*/
	protected Vector colors = null;
	/**
	* Indicates whether to paint items in different colors.
	*/
	protected Checkbox useColorsCB = null;
	/**
	* Indicate active or hidden state of the items
	*/
	protected Checkbox itemCB[] = null;
	/**
	* Clicking on the canvases allows to change colors for items.
	*/
	protected ColorCanvas ccanv[] = null;
	/**
	* If the list presenter represents multiple logical attributes, the item
	* colors are actually the colors assigned to the attributes and managed
	* by an AttrColorHandler that is common for all system components.
	* In this case the manipulator must listen to attribute color change events
	* from the AttrColorHandler. In order to stop event listening when the
	* manipulator is destroyed, a reference to the AttrColorHandler is stored.
	*/
	protected AttrColorHandler attrColorHandler = null;
	protected boolean destroyed = false;

	/**
	* Construction of map manipulator. Returns true if successfully constructed.
	* Arguments: 1) supervisor responsible for dynamic linking of displays;
	* 2) visualizer or classifier to be manipulated; 3) table with source data;
	* 4) table filter
	* The Manipulator must check if the visualizer has the appropriate type.
	*/
	@Override
	public boolean construct(Supervisor sup, Object visualizer, AttributeDataPortion table) {
		if (visualizer == null || !(visualizer instanceof ListPresenter))
			return false;
		lpres = (ListPresenter) visualizer;
		lpres.addPropertyChangeListener(this);
		if (lpres.getAttributes() != null && lpres.getAttributes().size() > 1) {
			attrColorHandler = lpres.getAttrColorHandler();
			if (attrColorHandler != null) {
				attrColorHandler.addPropertyChangeListener(this);
			}
		}
		setLayout(new ColumnLayout());
		makeInterface();
		return true;
	}

	protected void makeInterface() {
		removeAll();
		items = lpres.getAllItems();
		colors = lpres.getItemColors();
		if (items == null || items.size() < 1)
			return;
		if (useColorsCB == null) {
			// following text: "use different colors"
			useColorsCB = new Checkbox(res.getString("use_different_colors"));
			useColorsCB.addItemListener(this);
		}
		add(useColorsCB);
		useColorsCB.setState(lpres.getUseColors());
		if (itemCB == null || itemCB.length != items.size()) {
			itemCB = new Checkbox[items.size()];
			for (int i = 0; i < items.size(); i++) {
				itemCB[i] = new Checkbox("");
				itemCB[i].addItemListener(this);
			}
		}
		if (ccanv == null || ccanv.length != items.size()) {
			ccanv = new ColorCanvas[items.size()];
			for (int i = 0; i < items.size(); i++) {
				ccanv[i] = new ColorCanvas();
				ccanv[i].setActionListener(this);
				ccanv[i].setPreferredSize(3 * Metrics.mm(), 3 * Metrics.mm());
			}
		}
		for (int i = 0; i < items.size(); i++) {
			Panel p = new Panel(new BorderLayout(2, 2));
			p.add(ccanv[i], "West");
			p.add(itemCB[i], "East");
			if (lpres.getUseColors()) {
				ccanv[i].setColor((Color) colors.elementAt(i));
			} else {
				ccanv[i].setColor(lpres.getDefaultColor());
			}
			itemCB[i].setState(lpres.isItemActive((String) items.elementAt(i)));
			Panel pp = new Panel(new BorderLayout());
			pp.add(p, "West");
			pp.add(new Label(lpres.getItemName((String) items.elementAt(i))), "Center");
			add(pp);
		}
		if (this.isShowing()) {
			CManager.validateAll(this);
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(useColorsCB)) {
			boolean useColors = useColorsCB.getState();
			for (int i = 0; i < items.size(); i++)
				if (useColors) {
					ccanv[i].setColor((Color) colors.elementAt(i));
				} else {
					ccanv[i].setColor(lpres.getDefaultColor());
				}
			lpres.setUseColors(useColors);
		} else if (itemCB != null) {
			for (int i = 0; i < itemCB.length; i++)
				if (e.getSource().equals(itemCB[i])) {
					lpres.setItemActive((String) items.elementAt(i), itemCB[i].getState());
					break;
				}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof ColorCanvas) {
			//the user wishes to change one of the colors for the items or the default
			//sign color
			ColorCanvas cc = (ColorCanvas) e.getSource();
			ColorDlg cDlg = new ColorDlg(CManager.getAnyFrame(this),
			// following text:"Choose color for item":"Choose default color"
					((lpres.getUseColors()) ? res.getString("Choose_color_for_item") : res.getString("Choose_default_color")));
			cDlg.selectColor(this, cc, cc.getColor());
		}
	}

	/**
	* Called when some item color has been changed in the dialog.
	*/
	@Override
	public void colorChanged(Color color, Object selector) {
		ColorCanvas cc = (ColorCanvas) selector;
		if (lpres.getUseColors()) {
			for (int i = 0; i < items.size(); i++)
				if (cc.equals(ccanv[i]))
					if (attrColorHandler != null) {
						attrColorHandler.setColorForAttribute(color, (String) items.elementAt(i));
						return;
					} else {
						ccanv[i].setColor(color);
						colors.setElementAt(color, i);
						;
						lpres.setColorForItem(color, (String) items.elementAt(i));
						break;
					}
		} else {
			for (int i = 0; i < items.size(); i++) {
				ccanv[i].setColor(color);
			}
			lpres.setDefaultColor(color);
		}
		lpres.notifyVisChange();
	}

	/**
	* This method gets called when
	* 1) assignment of colors for attributes has been changed by some other
	*    component;
	* 2) the list presenter detected that new item had appeared (e.g. resulting
	*    from data update).
	*/
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getSource().equals(attrColorHandler)) {
			for (int i = 0; i < items.size(); i++) {
				Color c = attrColorHandler.getColorForAttribute((String) items.elementAt(i));
				colors.setElementAt(c, i);
				if (lpres.getUseColors()) {
					ccanv[i].setColor(c);
				}
			}
			if (lpres.getUseColors()) {
				lpres.notifyVisChange();
			}
		} else if (evt.getSource().equals(lpres)) {
			String item = (String) evt.getNewValue();
			if (item != null && StringUtil.isStringInVectorIgnoreCase(item, items)) {
				makeInterface();
			}
		}
	}

	/**
	* Makes necessary operations for destroying, e.g. unregisters from
	* listening highlighting and other events.
	*/
	@Override
	public void destroy() {
		if (attrColorHandler != null) {
			attrColorHandler.removePropertyChangeListener(this);
		}
		lpres.removePropertyChangeListener(this);
		destroyed = true;
	}

	/**
	* Replies whether is destroyed or not
	*/
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}
}
