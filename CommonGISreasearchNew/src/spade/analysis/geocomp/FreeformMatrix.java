package spade.analysis.geocomp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

import spade.lib.basicwin.DialogContent;
import spade.lib.lang.Language;

public class FreeformMatrix extends Panel implements DialogContent, ActionListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.geocomp.Res");
	protected String err = null;

	public int size;
	protected double scale;
	protected TextField eScale;
	protected Component[][] mComp;
	protected double[][] mNum;

	public FreeformMatrix(int size) {
		super();
		this.size = size;
		setLayout(new BorderLayout());
		Panel ps = new Panel();
		ps.setLayout(new FlowLayout());
		// following text: "Scale:"
		ps.add(new Label(res.getString("Scale_")));
		eScale = new TextField("1", 5);
		ps.add(eScale);
		add("South", ps);
		Panel p = new Panel();
		add("Center", p);
		p.setLayout(new GridLayout(size, size));
		mComp = new Component[size][size];
		mNum = new double[size][size];
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				TextField tf = new TextField();
				mComp[x][y] = tf;
				mNum[x][y] = 0;
				if (2 * x + 1 == size && 2 * y + 1 == size) {
					tf.setBackground(Color.yellow);
					mNum[x][y] = 1;
					tf.setText(Double.toString(mNum[x][y]));
				}
				p.add(tf);
				tf.addActionListener(this);
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent ev) {
		update();
	}

	public boolean update() {
		TextField tf = null;
		try {
			scale = new Double(eScale.getText()).doubleValue();
			if (scale <= 0)
				throw new Exception();
			for (int y = 0; y < size; y++) {
				for (int x = 0; x < size; x++) {
					tf = (TextField) mComp[x][y];
					if (tf.getText().equals("")) {
						mNum[x][y] = Double.NaN;
					} else {
						mNum[x][y] = new Double(tf.getText()).doubleValue();
					}
				}
			}
			return true;
		} catch (Exception ex) {
			if (tf != null) {
				tf.requestFocus();
			} else {
				eScale.requestFocus();
			}
			return false;
		}
	}

	public double getAbsoluteValue(int x, int y) {
		return mNum[x][size - 1 - y] / scale;
	}

	public double getRelativeValue(int x, int y) {
		return getAbsoluteValue(x + (size - 1) / 2, y + (size - 1) / 2);
	}

	@Override
	public String getErrorMessage() {
		return err;
	}

	@Override
	public boolean canClose() {
		err = null;
		if (!update()) {
			err = "Invalid numbers!";
			return false;
		}
		return true;
	}
}
