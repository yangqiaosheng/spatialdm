package spade.lib.basicwin;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.TextField;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 03-Mar-2008
 * Time: 16:45:07
 */
public class EditStringsPanel extends Panel implements DialogContent {
	protected String defValues[] = null;
	protected String labels[] = null;
	protected String err = null;
	protected TextField tf[] = null;
	protected String enteredVals[] = null;
	protected Panel mainP = null;
	float factor = 1f;

	/**
	 * Constructs a UI for editing or entering string values in multiple text fields
	 * @param labels - used for labels on the left of the text fields; no label if null
	 * @param defValues - default values
	 * @param explanation - optional; when not null, a label or text canvas with
	 *                      this text is included in the panel above the text field
	 */
	public EditStringsPanel(String labels[], String defValues[], String explanation) {
		super();
		this.defValues = defValues;
		this.labels = labels;
		tf = new TextField[defValues.length];
		GridBagLayout gridbag = new GridBagLayout();
		mainP = new Panel(gridbag);
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0f;
		c.weighty = 1.0f;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = GridBagConstraints.REMAINDER;
		if (explanation != null) {
			Component lab = null;
			if (explanation.length() <= 60) {
				lab = new Label(explanation, Label.CENTER);
			} else {
				TextCanvas tc = new TextCanvas();
				tc.setText(explanation);
				lab = tc;
			}
			gridbag.setConstraints(lab, c);
			mainP.add(lab);
		}
		for (int i = 0; i < defValues.length; i++) {
			tf[i] = new TextField(defValues[i], Math.max(40, defValues[i].length()));
			if (labels != null) {
				Label lab = new Label((labels[i] != null) ? labels[i] : "", Label.RIGHT);
				c.gridwidth = 1;
				gridbag.setConstraints(lab, c);
				mainP.add(lab);
			}
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(tf[i], c);
			mainP.add(tf[i]);
		}
		setLayout(new BorderLayout());
		if (defValues.length <= 10) {
			add(mainP, BorderLayout.CENTER);
		} else {
			ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
			scp.add(mainP);
			add(scp, BorderLayout.CENTER);
			factor = 10f / defValues.length;
		}
	}

	@Override
	public Dimension getPreferredSize() {
		if (factor >= 1)
			return super.getPreferredSize();
		Dimension d1 = mainP.getPreferredSize();
		if (d1 == null)
			return null;
		return new Dimension(d1.width + 10, Math.round(factor * d1.height) + 10);
	}

	public String[] getEnteredValues() {
		return enteredVals;
	}

	protected void addFieldName(int idx) {
		if (err == null)
			return;
		if (labels == null || labels[idx] == null) {
			err += " in the field N " + idx;
		} else {
			err += " in the field <" + labels[idx] + ">";
		}

	}

	@Override
	public boolean canClose() {
		err = null;
		if (enteredVals == null) {
			enteredVals = new String[tf.length];
		}
		for (int i = 0; i < tf.length; i++) {
			enteredVals[i] = null;
			String str = tf[i].getText();
			if (str != null) {
				str = str.trim();
			}
			if (str == null || str.length() < 1) {
				err = "No value entered";
				addFieldName(i);
				return false;
			}
			enteredVals[i] = str;
		}
		return true;
	}

	@Override
	public String getErrorMessage() {
		return err;
	}
}
