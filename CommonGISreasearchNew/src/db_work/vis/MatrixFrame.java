package db_work.vis;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Vector;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.FoldablePanel;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 12-Jun-2006
 * Time: 16:07:57
 * To change this template use File | Settings | File Templates.
 */
public class MatrixFrame extends Frame implements WindowListener, ItemListener {

	protected MatrixCanvas mc = null;
	protected Checkbox cbFillMode[] = null, cbXAsc = null, cbYAsc = null;

	public MatrixFrame(String attrName, Vector labelsV, Vector labelsH, float totals[][]) {
		super("Matrix " + attrName);
		addWindowListener(this);
		setVisible(true);
		setLayout(new BorderLayout());
		add(mc = new MatrixCanvas(attrName, labelsV, labelsH, totals), BorderLayout.CENTER);
		Panel p = new Panel(new ColumnLayout());
		FoldablePanel fp = new FoldablePanel(p, new Label("Controls:"));
		fp.open();
		add(fp, BorderLayout.SOUTH);
		Panel pp = new Panel(new FlowLayout(FlowLayout.LEFT));
		p.add(pp);
		pp.add(new Label("Mode:"));
		CheckboxGroup cbg = new CheckboxGroup();
		cbFillMode = new Checkbox[3];
		pp.add(cbFillMode[0] = new Checkbox("xy", true, cbg));
		pp.add(cbFillMode[1] = new Checkbox("x", false, cbg));
		pp.add(cbFillMode[2] = new Checkbox("y", false, cbg));
		for (Checkbox element : cbFillMode) {
			element.addItemListener(this);
		}
		pp.add(new Label("Ascending order:"));
		pp.add(cbXAsc = new Checkbox("x", true));
		pp.add(cbYAsc = new Checkbox("y", false));
		cbXAsc.addItemListener(this);
		cbYAsc.addItemListener(this);
		pack();
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		for (int i = 0; i < cbFillMode.length; i++)
			if (cbFillMode[i].getState()) {
				mc.setFillMode(i);
			}
		mc.setXAsc(cbXAsc.getState());
		mc.setYAsc(cbYAsc.getState());
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
