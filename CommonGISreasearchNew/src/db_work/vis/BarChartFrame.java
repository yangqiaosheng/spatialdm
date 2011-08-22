package db_work.vis;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.FoldablePanel;
import spade.lib.basicwin.PopupManager;
import spade.lib.basicwin.SplitLayout;
import spade.lib.util.StringUtil;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 10-Feb-2006
 * Time: 12:28:42
 * To change this template use File | Settings | File Templates.
 */
public class BarChartFrame extends Frame implements WindowListener, ActionListener, ItemListener {

	protected BarChartCanvas bcc[] = null;

	protected Vector vTotals = null;
	protected String labels[] = null;

	protected float parts[][] = null;

	public void setParts(float parts[][]) {
		this.parts = parts;
		// redraw ...
	}

	protected Color partsColors[] = null;

	protected SplitLayout spl = null;
	protected Checkbox cbCumulative = null, cbCommonMinMax = null;
	protected Button bSameSize = null;

	public BarChartFrame(String name, Vector vSubNames, Vector vTotals, String labels[]) {
		super("Bar chart: " + name);
		addWindowListener(this);
		setVisible(true);
		this.vTotals = vTotals;
		this.labels = labels;
		int nbcc = vTotals.size();
		bcc = new BarChartCanvas[nbcc];
		for (int i = 0; i < nbcc; i++) {
			float totals[] = (float[]) vTotals.elementAt(i);
			bcc[i] = new BarChartCanvas(labels, totals);
		}
		setLayout(new BorderLayout());
		add(new Label(name), BorderLayout.NORTH);
		Panel pbcc = new Panel();
		add(pbcc, BorderLayout.CENTER);
		spl = new SplitLayout(pbcc, SplitLayout.HOR);
		pbcc.setLayout(spl);
		for (int i = 0; i < nbcc; i++) {
			Panel p = new Panel(new BorderLayout());
			String sl1 = (String) vSubNames.elementAt(i), sl2 = "range:" + StringUtil.floatToStr(bcc[i].getMin(), bcc[i].getMin(), bcc[i].getMax()) + ".." + StringUtil.floatToStr(bcc[i].getMax(), bcc[i].getMin(), bcc[i].getMax());
			Label l = new Label(sl1 + ", " + sl2);
			p.add(l, BorderLayout.NORTH);
			new PopupManager(l, sl1 + "\n" + sl2, true);
			p.add(bcc[i], BorderLayout.CENTER);
			spl.addComponent(p, 1f / nbcc);
		}
		Panel pc = new Panel();
		pc.setLayout(new ColumnLayout());
		FoldablePanel fp = new FoldablePanel(pc, new Label("Controls:"));
		fp.open();
		add(fp, BorderLayout.SOUTH);
		cbCumulative = new Checkbox("cumulative", false);
		cbCumulative.addItemListener(this);
		pc.add(cbCumulative);
		if (nbcc > 1) {
			cbCommonMinMax = new Checkbox("common MIN and MAX", false);
			cbCommonMinMax.addItemListener(this);
			pc.add(cbCommonMinMax);
			bSameSize = new Button("Same size");
			bSameSize.addActionListener(this);
			pc.add(bSameSize);
		}
		pack();
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (ie.getSource().equals(cbCumulative)) {
			for (BarChartCanvas element : bcc) {
				element.setIsCumulative(cbCumulative.getState());
			}
			return;
		}
		if (ie.getSource().equals(cbCommonMinMax)) {
			if (cbCommonMinMax.getState()) {
				float min = Float.NaN, max = Float.NaN;
				for (BarChartCanvas element : bcc) {
					if (Float.isNaN(min) || element.getMin() < min) {
						min = element.getMin();
					}
					if (Float.isNaN(max) || element.getMax() > max) {
						max = element.getMax();
					}
				}
				for (BarChartCanvas element : bcc) {
					element.setAbsMinMax(min, max);
					element.setUseAbsMinMax(true);
				}
			} else {
				for (BarChartCanvas element : bcc) {
					element.setUseAbsMinMax(false);
				}
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource() instanceof Button && ae.getSource().equals(bSameSize)) {
			spl.forceEqualParts();
			CManager.validateAll(spl.getComponent(0));
		}
	}

	@Override
	public void windowClosing(WindowEvent e) {
		if (e.getSource().equals(this)) {
			dispose();
			//any other cleenup...
		}
	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowOpened(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}
}
