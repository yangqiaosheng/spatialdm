package spade.lib.basicwin;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Mar 13, 2008
 * Time: 11:08:38 AM
 *
 * Panel with Checkboxes. Collects their inputs (ItemEvents) and generates a single ActionEvent.
 * If included into Foldablepanel, can change Panel's label
 * Checkboxes are assigned to a common CheckboxGroup
 */
public class CheckboxPanel extends Panel implements ItemListener {

	protected Checkbox cb[] = null;
	protected int idxSelected = -1;

	/**
	 * returns index of the selected checkbox
	 */
	public int getSelectedIndex() {
		return idxSelected;
	}

	protected ActionListener al = null;
	protected String sTaskCaption = null;
	protected Label label = null;

	public CheckboxPanel(ActionListener al, Label label, Checkbox cb[]) {
		super();
		this.label = label;
		if (label != null) {
			this.sTaskCaption = label.getText();
		}
		this.cb = cb;
		this.al = al;
		setLayout(new ColumnLayout());
		if (cb != null) {
			CheckboxGroup cbg = new CheckboxGroup();
			for (Checkbox element : cb) {
				element.setCheckboxGroup(cbg);
				element.addItemListener(this);
				add(element);
			}
		}
		itemStateChanged(null);
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (cb != null) {
			for (int i = 0; i < cb.length; i++)
				if (cb[i].getState()) {
					idxSelected = i;
					if (label != null) {
						label.setText(sTaskCaption + ": " + cb[idxSelected].getLabel());
					}
					if (al != null && ie != null) {
						al.actionPerformed(new ActionEvent(this, 0, "selection", idxSelected));
					}
					return;
				}
		}
	}

}
