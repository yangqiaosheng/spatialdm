package spade.analysis.tools.db_tools.statistics;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 24-Jan-2007
 * Time: 12:45:15
 * Displays the given collection of items, which must be instances of
 * TableStatisticsItem, in a table form.
 */
public class TableStatisticsDisplay extends Panel {
	/**
	 * Constructs a panel that displays the given collection of items in a table form.
	 * @param items - array of instances of TableStatisticsItem
	 */
	public TableStatisticsDisplay(TableStatisticsItem items[]) {
		GridBagLayout gridbag = new GridBagLayout();
		setLayout(gridbag);
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0f;
		c.weighty = 1.0f;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = GridBagConstraints.REMAINDER;
		Label l = new Label("Value statistics for the selected table columns", Label.CENTER);
		gridbag.setConstraints(l, c);
		add(l);
		for (TableStatisticsItem item : items) {
			if (item == null || item.name == null) {
				continue;
			}
			l = new Label(item.name);
			boolean fromTo = item.value != null && item.value.length() > 0 && item.maxValue != null && item.maxValue.length() > 0;
			c.gridwidth = (fromTo) ? 2 : 3;
			gridbag.setConstraints(l, c);
			add(l);
			if (fromTo) {
				l = new Label("from", Label.CENTER);
				c.gridwidth = 1;
				gridbag.setConstraints(l, c);
				add(l);
				TextField tf = new TextField(item.value);
				tf.setEditable(false);
				gridbag.setConstraints(tf, c);
				add(tf);
				l = new Label("to", Label.CENTER);
				gridbag.setConstraints(l, c);
				add(l);
				tf = new TextField(item.maxValue);
				tf.setEditable(false);
				c.gridwidth = GridBagConstraints.REMAINDER;
				gridbag.setConstraints(tf, c);
				add(tf);
			} else {
				TextField tf = new TextField(item.value);
				tf.setEditable(false);
				c.gridwidth = 1;
				gridbag.setConstraints(tf, c);
				add(tf);
				l = new Label((item.comment == null) ? "" : item.comment);
				c.gridwidth = GridBagConstraints.REMAINDER;
				gridbag.setConstraints(l, c);
				add(l);
			}
		}
	}
}
