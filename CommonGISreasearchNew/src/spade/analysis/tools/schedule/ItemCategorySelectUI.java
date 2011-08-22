package spade.analysis.tools.schedule;

import java.awt.Choice;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;

import spade.lib.basicwin.RowLayout;
import spade.lib.lang.Language;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 12-Mar-2007
 * Time: 15:01:18
 * A UI to select the item category for viewing corresponding transportation
 * orders and other relevant data in course of exploring a transportation
 * schedule.
 */
public class ItemCategorySelectUI extends Panel implements ItemListener, PropertyChangeListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.tools.schedule.SchedulerTexts_tools_schedule");

	/**
	 * A non-UI object actually performing the selection. There may be one
	 * ItemCategorySelector for several UIs. In this case, the UIs need to be
	 * synchronized. Therefore, each UI must listen to category selection
	 * events from the ItemCategorySelector.
	 */
	protected ItemCategorySelector iCatSelector = null;

	protected Choice catChoice = null;

	public ItemCategorySelectUI(ItemCategorySelector iCatSelector) {
		this.iCatSelector = iCatSelector;
		iCatSelector.addCategoryChangeListener(this);
		catChoice = new Choice();
		for (int i = 0; i < iCatSelector.itemCategories.size(); i++) {
			catChoice.add((String) iCatSelector.itemCategories.elementAt(i));
		}
		catChoice.add(res.getString("all_cat"));
		int idx = iCatSelector.getSelectedCategoryIndex();
		if (idx >= 0) {
			catChoice.select(idx);
		} else {
			catChoice.select(catChoice.getItemCount() - 1);
		}
		catChoice.addItemListener(this);
		setLayout(new RowLayout(5, 0));
		add(new Label(res.getString("item_cat") + ":"));
		add(catChoice);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(catChoice)) {
			int idx = catChoice.getSelectedIndex();
			if (idx == catChoice.getItemCount() - 1) {
				idx = -1;
			}
			if (idx != iCatSelector.catIdx) {
				iCatSelector.doCategorySelection(idx);
			}
		}
	}

	/**
	 * Reacts to category selection events from the ItemCategorySelector.
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(iCatSelector) && e.getPropertyName().equals("category_selection")) {
			int idx = iCatSelector.getSelectedCategoryIndex();
			if (idx < 0) {
				idx = catChoice.getItemCount() - 1;
			}
			catChoice.select(idx);
		}
	}
}
