package data_load.readers;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.util.ResourceBundle;

import spade.lib.basicwin.DialogContent;
import spade.lib.lang.Language;

/**
* In this panel the user can specify geographical boundaries of an image layer
*/
public class GetBoundsPanel extends Panel implements DialogContent {
	static ResourceBundle res = Language.getTextResource("data_load.readers.Res");
	public float x1 = Float.NaN, y1 = Float.NaN, x2 = Float.NaN, y2 = Float.NaN;
	protected TextField x1tf = null, x2tf = null, y1tf = null, y2tf = null;
	protected String err = null;

	public GetBoundsPanel() {
		setLayout(new BorderLayout());
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		c.weighty = 1.0f;
		c.fill = GridBagConstraints.HORIZONTAL;
		Panel p = new Panel(gridbag);
		Label l = new Label("X1=");
		c.gridwidth = 1;
		c.weightx = 0.0f;
		gridbag.setConstraints(l, c);
		p.add(l);
		x1tf = new TextField(5);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0f;
		gridbag.setConstraints(x1tf, c);
		p.add(x1tf);
		l = new Label("Y1=");
		c.gridwidth = 1;
		c.weightx = 0.0f;
		gridbag.setConstraints(l, c);
		p.add(l);
		y1tf = new TextField(5);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0f;
		gridbag.setConstraints(y1tf, c);
		p.add(y1tf);
		l = new Label("X2=");
		c.gridwidth = 1;
		c.weightx = 0.0f;
		gridbag.setConstraints(l, c);
		p.add(l);
		x2tf = new TextField(5);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0f;
		gridbag.setConstraints(x2tf, c);
		p.add(x2tf);
		l = new Label("Y2=");
		c.gridwidth = 1;
		c.weightx = 0.0f;
		gridbag.setConstraints(l, c);
		p.add(l);
		y2tf = new TextField(5);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0f;
		gridbag.setConstraints(y2tf, c);
		p.add(y2tf);
		//following text:"Specify the geographical boundaries:"
		add(new Label(res.getString("Specify_the")), "North");
		add(p, "Center");
	}

	@Override
	public boolean canClose() {
		String str = x1tf.getText();
		if (str == null || str.trim().length() < 1) {
			//following text:"X1 not specified!"
			err = res.getString("X1_not_specified_");
			return false;
		}
		try {
			//following text:"Illegal text for X1!"
			x1 = Float.valueOf(str).floatValue();
		} catch (NumberFormatException nfe) {
			err = res.getString("Illegal_text_for_X1_");
			return false;
		}
		str = x2tf.getText();
		if (str == null || str.trim().length() < 1) {
			//following text:"X2 not specified!"
			err = res.getString("X2_not_specified_");
			return false;
		}
		try {
			x2 = Float.valueOf(str).floatValue();
		} catch (NumberFormatException nfe) {
			//following text:"Illegal text for X2!"
			err = res.getString("Illegal_text_for_X2_");
			return false;
		}
		if (x2 <= x1) {
			//following text:"X2 must be bigger than X1!"
			err = res.getString("X2_must_be_bigger");
			return false;
		}
		str = y1tf.getText();
		if (str == null || str.trim().length() < 1) {
			//following text:"Y1 not specified!"
			err = res.getString("Y1_not_specified_");
			return false;
		}
		try {
			y1 = Float.valueOf(str).floatValue();
		} catch (NumberFormatException nfe) {
			//following text:"Illegal text for Y1!"
			err = res.getString("Illegal_text_for_Y1_");
			return false;
		}
		str = y2tf.getText();
		if (str == null || str.trim().length() < 1) {
			//following text:"Y2 not specified!"
			err = res.getString("Y2_not_specified_");
			return false;
		}
		try {
			y2 = Float.valueOf(str).floatValue();
		} catch (NumberFormatException nfe) {
			//following text:"Illegal text for Y2!"
			err = res.getString("Illegal_text_for_Y2_");
			return false;
		}
		if (y2 <= y1) {
			//following text:"Y2 must be bigger than Y1!"
			err = res.getString("Y2_must_be_bigger");
			return false;
		}
		return true;
	}

	@Override
	public String getErrorMessage() {
		return err;
	}
}
