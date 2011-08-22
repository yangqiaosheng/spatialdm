package data_load.intelligence;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.lib.ui.ListOrderer;
import spade.lib.util.IntArray;
import spade.vis.spec.ParamDescription;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 20-Jul-2004
 * Time: 12:16:17
 * Contains UI controls for describing a single non-temporal parameter.
 */
public class OneParamPanel extends Panel implements ActionListener, ItemListener {
	static ResourceBundle res = Language.getTextResource("data_load.intelligence.Res");
	/**
	 * The index of the column with the references (if the references are in a column)
	 */
	protected int colIdx = -1;
	/**
	 * The name of the parameter
	 */
	protected String parName = null;
	/**
	 * The listener of the user pressing the "remove" button.
	 */
	protected ActionListener owner = null;
	/**
	 * The values of the parameter
	 */
	protected Vector values = null;
	/**
	 * The list of parameter values shown to the user, possibly, in a user-
	 * specified order. For saving space, the list is shown as a choice control,
	 * although no choice is supposed.
	 */
	protected Choice valueCh = null;
	/**
	 * The switch from sorted to unsorted display of parameter values.
	 */
	protected Checkbox sortedCB = null;
	/**
	 * The button to activate value reordering, if the sorting option is chosen.
	 */
	protected Button reorderBt = null;
	/**
	 * Specifies the order for displaying parameter values
	 */
	protected IntArray order = null;
	/**
	 * The values of the parameter in a user-specified order
	 */
	protected Vector ordValues = null;

	/**
	 * Constructs the panel for describing one non-temporal parameter (dimension)
	 * of a multi-dimensional table.
	 * @param paramName - the name of the parameter
	 * @param colIdx - the index of the column in the table (if the parameter is
	 *                 specified in a column)
	 * @param values - the list of parameter values
	 * @param owner - who will be notified when the user presses the "remove"
	 *                button
	 */
	public OneParamPanel(String paramName, int colIdx, Vector values, ActionListener owner) {
		this.parName = paramName;
		this.colIdx = colIdx;
		this.values = values;
		this.owner = owner;
		setLayout(new GridLayout(1, 5, 2, 2));
		add(new Label(paramName));
		valueCh = new Choice();
		if (values == null || values.size() < 1) {
			valueCh.add(res.getString("No_values_found"));
		} else {
			for (int i = 0; i < values.size(); i++) {
				valueCh.add(values.elementAt(i).toString());
			}
		}
		add(valueCh);
		sortedCB = new Checkbox(res.getString("Sorted"), false);
		add(sortedCB);
		sortedCB.addItemListener(this);
		if (values == null || values.size() < 1) {
			sortedCB.setEnabled(false);
		}
		reorderBt = new Button(res.getString("Reorder"));
		reorderBt.addActionListener(this);
		reorderBt.setActionCommand("reorder");
		reorderBt.setEnabled(false);
		Panel p = new Panel(new FlowLayout(FlowLayout.CENTER, 5, 0));
		p.add(reorderBt);
		add(p);
		Button b = new Button(res.getString("Remove"));
		b.setActionCommand("remove");
		b.addActionListener(this);
		p = new Panel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
		p.add(b);
		add(p);
	}

	/**
	 * Reaction to pressing the "remove" button
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("remove") && owner != null) {
			owner.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "remove_" + colIdx));
		} else if (e.getActionCommand().equals("reorder")) {
			ListOrderer lord = new ListOrderer(values, false);
			OKDialog okd = new OKDialog(CManager.getAnyFrame(this), res.getString("Order_param_values"), false);
			okd.addContent(lord);
			okd.show();
			if (okd.wasCancelled())
				return;
			order = lord.getItemOrder();
			ordValues = lord.getOrderedItems();
			valueCh.setVisible(false);
			valueCh.removeAll();
			for (int i = 0; i < ordValues.size(); i++) {
				valueCh.add(ordValues.elementAt(i).toString());
			}
			valueCh.setVisible(true);
		}
	}

	/**
	 * Reaction to switching from unsorted to sorted value list and back
	 */
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(sortedCB)) {
			reorderBt.setEnabled(sortedCB.getState());
			//possibly, change the order of the values in the choice element with the values
			if (ordValues != null && ordValues.size() == values.size()) {
				valueCh.setVisible(false);
				valueCh.removeAll();
				if (sortedCB.getState()) {
					for (int i = 0; i < ordValues.size(); i++) {
						valueCh.add(ordValues.elementAt(i).toString());
					}
				} else {
					for (int i = 0; i < values.size(); i++) {
						valueCh.add(values.elementAt(i).toString());
					}
				}
				valueCh.setVisible(true);
			}
		}
	}

	/**
	 * Constructs and returns a description of the column parameter according to
	 * the states of the controls
	 */
	public ParamDescription getParamDescription() {
		ParamDescription pd = new ParamDescription();
		pd.columnName = parName;
		pd.columnIdx = colIdx;
		pd.isTemporal = false;
		pd.setMustBeOrdered(sortedCB.getState());
		if (sortedCB.getState())
			if (ordValues != null && ordValues.size() > 0) {
				pd.setValueOrder(ordValues);
			} else {
				pd.setValueOrder(values);
			}
		return pd;
	}
}
