package spade.analysis.classification;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
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

import spade.analysis.system.Supervisor;

public class RangedDistMultiPanel extends Panel implements ActionListener, ItemListener {
	static ResourceBundle res = spade.lib.lang.Language.getTextResource("spade.analysis.classification.Res");
	protected int ncolumns = 0;
	protected long maxCount = 0;
	protected Vector classifiers = null;
	/**
	* The names to distinguish between the individual charts
	*/
	protected Vector names = null;
	/**
	* The individual charts; each chart corresponds to one of the classifiers
	*/
	protected RangedDistCanvas charts[] = null;

	protected Supervisor sup = null;
	protected Panel centerPanel = null;
	Checkbox rangedCB;
	Checkbox commonCB;
	Button fitBt;

	public RangedDistMultiPanel(Supervisor sup, Vector classifiers, Vector names, int ncolumns) {
		this.ncolumns = ncolumns;
		this.classifiers = classifiers;
		this.names = names;
		this.sup = sup;
		setLayout(new BorderLayout());
		centerPanel = new Panel();
		add(centerPanel, "Center");
		Panel p = new Panel(new FlowLayout(FlowLayout.LEFT, 2, 1));
		add(p, "South");
		fitBt = new Button(res.getString("Fit_to_border"));
		fitBt.addActionListener(this);
		rangedCB = new Checkbox(res.getString("Ranged"));
		rangedCB.addItemListener(this);
		commonCB = new Checkbox(res.getString("Common_count"));
		commonCB.addItemListener(this);

		p.add(rangedCB);
		p.add(commonCB);

		p.add(fitBt);
		init();

	}

	public void setNColumns(int ncolumns) {
		if (this.ncolumns != ncolumns && ncolumns > 0) {
			this.ncolumns = ncolumns;
			init();
		}
	}

	public int getNColumns() {
		return ncolumns;
	}

	/**
	 *  Sets the maximum count (class size) for all canvases
	 */
	public void setMaxCount(long c) {
		maxCount = c;
	}

	public void repaintAllCharts() {
		if (charts == null)
			return;
		boolean commonCount = commonCB.getState();
		for (RangedDistCanvas chart : charts) {
			if (commonCount) {
				chart.setCommonMaxV(maxCount);
			} else {
				chart.setCommonMaxV(0);
			}
			chart.repaint();
		}
	}

	protected void init() {
		if (centerPanel.getLayout() != null && (centerPanel.getLayout() instanceof GridLayout)) {
			GridLayout gl = (GridLayout) centerPanel.getLayout();
			gl.setColumns(ncolumns);
			centerPanel.invalidate();
			invalidate();
			validate();
			return;
		}
		centerPanel.setLayout(new GridLayout(0, (ncolumns == 0) ? 2 : ncolumns));
		if (charts == null || charts.length != classifiers.size()) {
			charts = new RangedDistCanvas[classifiers.size()];
			for (int i = 0; i < classifiers.size(); i++) {
				charts[i] = new RangedDistCanvas(sup, (Classifier) classifiers.elementAt(i));
				charts[i].setRanged(rangedCB.getState());
			}
		}
		for (int i = 0; i < charts.length; i++)
			if (names == null || names.size() < 1) {
				centerPanel.add(charts[i]);
			} else {
				Panel p = new Panel(new BorderLayout(0, 0));
				String name = "";
				if (i < names.size() && names.elementAt(i) != null) {
					name = (String) names.elementAt(i);
				}
				p.add(new Label(name, Label.CENTER), BorderLayout.NORTH);
				p.add(charts[i], BorderLayout.CENTER);
				centerPanel.add(p);
			}
		if (isShowing()) {
			centerPanel.invalidate();
			invalidate();
			validate();
		}
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {

		Object o = ie.getSource();

		if (o == rangedCB && charts != null) {
			for (RangedDistCanvas chart : charts) {
				chart.setRanged(rangedCB.getState());
			}
		} else if (o == commonCB) {
			repaintAllCharts();
		}
	}

	@Override
	public void actionPerformed(ActionEvent ev) {
		Object o = ev.getSource();
		if (o == fitBt && charts != null) {
			for (RangedDistCanvas chart : charts) {
				chart.FitToBorder();
				chart.repaint();
			}
		}
	}

}