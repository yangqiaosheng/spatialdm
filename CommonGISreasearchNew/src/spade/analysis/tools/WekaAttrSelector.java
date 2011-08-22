package spade.analysis.tools;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.vis.database.AttributeDataPortion;
import weka.core.Instances;

public class WekaAttrSelector extends Panel implements ActionListener {

	protected Instances instances = null;
	protected Button bSelAll = null, bUnselAll = null;
	protected Checkbox cb[] = null;
	protected Label lNum[] = null;
	protected Panel pInScrollPane = null;

	public WekaAttrSelector(Instances instances, AttributeDataPortion tbl) {
		this(instances, tbl, false);
	}

	public WekaAttrSelector(Instances instances, AttributeDataPortion tbl, boolean isForTree) {
		super();
		this.instances = instances;
		setLayout(new BorderLayout());
		if (isForTree) {
			Panel p = new Panel();
			p.setLayout(new BorderLayout());
			p.add(new Label("Attributes:", Label.LEFT), BorderLayout.WEST);
			Label l = new Label("grey: used in tree", Label.RIGHT);
			l.setBackground(new Color(208, 208, 208));
			p.add(l, BorderLayout.CENTER);
			add(p, BorderLayout.NORTH);
		} else {
			add(new Label("Attributes:", Label.CENTER), BorderLayout.NORTH);
		}
		ScrollPane sp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
		pInScrollPane = new Panel(new BorderLayout());
		sp.add(pInScrollPane);
		Panel p = new Panel();
		pInScrollPane.add(p, BorderLayout.CENTER);
		p.setLayout(new ColumnLayout());
		cb = new Checkbox[instances.numAttributes()];
		for (int i = 0; i < cb.length; i++) {
			p.add(cb[i] = new Checkbox(tbl.getAttributeName(instances.attribute(i).name()), true));
			if (instances.classIndex() == i) {
				cb[i].setEnabled(false);
			}
		}
		add(sp, BorderLayout.CENTER);
		p = new Panel();
		p.setLayout(new BorderLayout());
		p.add(bSelAll = new Button("Select All"), BorderLayout.WEST);
		p.add(bUnselAll = new Button("Unselect All"), BorderLayout.EAST);
		add(p, BorderLayout.SOUTH);
		bSelAll.addActionListener(this);
		bUnselAll.addActionListener(this);
	}

	public void setNumbers(int n[]) {
		if (lNum == null) {
			Panel plNum = new Panel();
			plNum.setLayout(new ColumnLayout());
			lNum = new Label[n.length];
			for (int i = 0; i < n.length; i++) {
				lNum[i] = new Label((n[i] == 0) ? "" : String.valueOf(n[i]), Label.RIGHT);
				plNum.add(lNum[i]);
			}
			pInScrollPane.add(plNum, BorderLayout.EAST);
		} else {
			for (int i = 0; i < n.length; i++) {
				lNum[i].setText((n[i] == 0) ? "" : String.valueOf(n[i]));
			}
		}
		CManager.validateAll(this);
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource().equals(bSelAll)) {
			for (Checkbox element : cb) {
				element.setState(true);
			}
		}
		if (ae.getSource().equals(bUnselAll)) {
			for (Checkbox element : cb)
				if (element.isEnabled()) {
					element.setState(false);
				}
		}
	}

	public Instances getSubset() {
		boolean allSelected = true;
		for (int i = 0; i < cb.length && allSelected; i++) {
			allSelected = cb[i].getState();
		}
		if (allSelected)
			return instances;
		Instances sInstances = new Instances(instances);
		int nattrs = instances.numAttributes();
		for (int i = nattrs - 1; i >= 0; i--)
			if (!cb[i].getState()) {
				sInstances.deleteAttributeAt(i);
			}
		return sInstances;
	}

	public Checkbox[] getCheckbox() {
		return cb;
	}

	public int getIdxOfNthSelected(int n) {
		int k = -1;
		for (int i = 0; i < cb.length; i++) {
			if (cb[i].getState()) {
				k++;
			}
			if (k == n)
				return i;
		}
		return 0;
	}

}