package spade.analysis.tools;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.Frame;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import spade.analysis.system.ESDACore;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.vis.database.AttributeDataPortion;
import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.ChiSquaredAttributeEval;
import weka.attributeSelection.GainRatioAttributeEval;
import weka.attributeSelection.InfoGainAttributeEval;
import weka.attributeSelection.OneRAttributeEval;
import weka.attributeSelection.Ranker;
import weka.attributeSelection.ReliefFAttributeEval;
import weka.attributeSelection.SymmetricalUncertAttributeEval;
import weka.core.Instances;

public class WekaAttributeSelectorCP extends Frame implements ActionListener {

	protected AttributeDataPortion tbl = null;
	protected Instances instances = null;
	protected Choice chEvaluator = null;

	public WekaAttributeSelectorCP(Instances instances, ESDACore core, AttributeDataPortion tbl) {
		super("Weka Attribute Selector Control Panel");
		this.instances = instances;
		this.tbl = tbl;
		selectTargetAttribute();
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				dispose();
			}
		});
		setLayout(new BorderLayout());
		Panel p = new Panel();
		p.setLayout(new ColumnLayout());
		add(p, BorderLayout.CENTER);
		p.add(new Label("Target attribute:", Label.LEFT));
		p.add(new Label(tbl.getAttributeName(instances.attribute(instances.classIndex()).name()), Label.CENTER));
		p.add(new Line(false));
		p.add(new Label("Evaluator:", Label.LEFT));
		p.add(chEvaluator = new Choice());
		chEvaluator.add("ReliefFAttributeEval");
		chEvaluator.add("InfoGainAttributeEval");
		chEvaluator.add("GainRatioAttributeEval");
		chEvaluator.add("SymmetricalUncertAttributeEval");
		chEvaluator.add("OneRAttributeEval");
		chEvaluator.add("ChiSquaredAttributeEval");
		chEvaluator.select(0);
		if (!instances.attribute(instances.classIndex()).isNominal()) {
			chEvaluator.setEnabled(false);
		}
		p.add(new Label("Search Method:", Label.LEFT));
		p.add(new Label("Ranker", Label.CENTER));
		p.add(new Line(false));
		Button b = new Button("Run Attribute Selector");
		b.addActionListener(this);
		p.add(b);
		setSize(500, 300);
		pack();
		show();
		//The window must be properly registered in order to be closed in a case
		//when the aplication is closed or changed.
		core.getWindowManager().registerWindow(this);
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		runWekaAttributeSelector();
	}

	protected void runWekaAttributeSelector(/*, ESDACore core, AttributeDataPortion tbl*/) {
		try {
			ASEvaluation evaluator = null;
			int n = chEvaluator.getSelectedIndex();
			switch (n) {
			case 0:
				evaluator = (new ReliefFAttributeEval());
				break;
			case 1:
				evaluator = (new InfoGainAttributeEval());
				break;
			case 2:
				evaluator = (new GainRatioAttributeEval());
				break;
			case 3:
				evaluator = (new SymmetricalUncertAttributeEval());
				break;
			case 4:
				evaluator = (new OneRAttributeEval());
				break;
			case 5:
				evaluator = (new ChiSquaredAttributeEval());
				break;
			}
			ASSearch search = (new Ranker());
			System.out.println("* Attribute Selector: Start");
			AttributeSelection as = new AttributeSelection();
			as.setEvaluator(evaluator);
			as.setSearch(search);
			as.setFolds(10);
			as.setSeed(1);
			as.SelectAttributes(instances);
			System.out.println("* Attribute Selector: Finish");
			System.out.println(as.toResultsString());
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	protected void selectTargetAttribute() {
		int selIdx;
		if (instances.numAttributes() == 1) {
			selIdx = 0;
		} else {
			List l = new List(5);
			for (int i = 0; i < instances.numAttributes(); i++) {
				String tblAttrId = instances.attribute(i).name(), tblAttrName = tbl.getAttributeName(tblAttrId);
				l.add(tblAttrName);
			}
			l.select(0);
			OKDialog dlg = new OKDialog(CManager.getAnyFrame(), "Select target attribute", false);
			dlg.addContent(l);
			dlg.show();
			selIdx = l.getSelectedIndex();
		}
		instances.setClassIndex(selIdx);
	}

}