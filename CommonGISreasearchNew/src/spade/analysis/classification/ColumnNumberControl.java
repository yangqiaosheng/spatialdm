package spade.analysis.classification;

import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.TImgButton;
import spade.lib.basicwin.TriangleDrawer;
import spade.lib.lang.Language;
import spade.vis.mapvis.MultiClassPresenter;

/**
* Manipulates the number of columns in the mosaic symbols used in a MultiClassPresenter
*/
public class ColumnNumberControl extends Panel implements ActionListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.classification.Res");
	/**
	* The MultiClassPresenter to be manipulated
	*/
	protected MultiClassPresenter vis = null;
	/**
	* The text field for entering the number of columns
	*/
	protected TextField tf = null;

	/**
	* Constructs the manipulator
	*/
	public ColumnNumberControl(MultiClassPresenter multiClassPres) {
		vis = multiClassPres;
		if (vis == null)
			return;
		setLayout(new ColumnLayout());
		add(new Label(res.getString("n_cols") + ":"));
		Panel p = new Panel(new FlowLayout(FlowLayout.LEFT, 3, 2));
		tf = new TextField(String.valueOf(vis.getNColumns()), 2);
		tf.addActionListener(this);
		p.add(tf);
		TriangleDrawer td = new TriangleDrawer(TriangleDrawer.N);
		td.setPreferredSize(12, 12);
		td.setMargins(2, 1);
		TImgButton ib = new TImgButton(td);
		ib.setActionCommand("more");
		ib.addActionListener(this);
		p.add(ib);
		td = new TriangleDrawer(TriangleDrawer.S);
		td.setPreferredSize(12, 12);
		td.setMargins(2, 1);
		ib = new TImgButton(td);
		ib.setActionCommand("less");
		ib.addActionListener(this);
		p.add(ib);
		add(p);
		add(new Label(res.getString("from") + " 1 " + res.getString("to") + " " + vis.getNClassifiers()));
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String str = tf.getText();
		int nc = vis.getNColumns();
		if (str != null) {
			str = str.trim();
			try {
				int n = Integer.valueOf(str).intValue();
				if (n > 0 && n <= vis.getNClassifiers()) {
					nc = n;
				}
			} catch (NumberFormatException nfe) {
			}
		}
		String cmd = e.getActionCommand();
		if (cmd != null)
			if (cmd.equals("more") && nc < vis.getNClassifiers()) {
				++nc;
			} else if (cmd.equals("less") && nc > 1) {
				--nc;
			}
		tf.setText(String.valueOf(nc));
		if (nc != vis.getNColumns()) {
			vis.setNColumns(nc);
			vis.notifyVisChange();
		}
	}
}