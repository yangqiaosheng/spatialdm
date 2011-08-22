package spade.analysis.classification;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;

import spade.analysis.system.Supervisor;

public class RangedDistPanel extends Panel implements ItemListener, ActionListener {
	static ResourceBundle res = spade.lib.lang.Language.getTextResource("spade.analysis.classification.Res");
	RangedDistCanvas distCanvas;
	Checkbox rangedCB;
	Button fitBt;

	public RangedDistPanel(Supervisor sup, Classifier classifier) {
		this(sup, classifier, false);
	}

	public RangedDistPanel(Supervisor sup, Classifier classifier, boolean ranged) {
		setLayout(new BorderLayout());
		Panel p = new Panel(new FlowLayout(FlowLayout.LEFT, 2, 1));
		add(p, "South");
		fitBt = new Button(res.getString("Fit_to_border"));
		fitBt.addActionListener(this);
		rangedCB = new Checkbox(res.getString("Ranged"), ranged);

		rangedCB.addItemListener(this);
		p.add(rangedCB);
		p.add(fitBt);

		distCanvas = new RangedDistCanvas(sup, classifier);
		distCanvas.setRanged(rangedCB.getState());
		add(distCanvas, "Center");
	}

	public RangedDistCanvas getGraph() {
		return distCanvas;
	}

/*
  * broadcasting state method
  */
	@Override
	public void itemStateChanged(ItemEvent ie) {

		Object o = ie.getSource();

		if (o == rangedCB) {
			distCanvas.setRanged(rangedCB.getState());
		}

	}

	@Override
	public void actionPerformed(ActionEvent ev) {
		Object o = ev.getSource();
		if (o == fitBt) {
			distCanvas.FitToBorder();
			distCanvas.repaint();
		}
	}

}