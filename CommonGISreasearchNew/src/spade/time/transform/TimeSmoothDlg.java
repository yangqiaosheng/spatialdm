package spade.time.transform;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.DialogContent;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.RowLayout;
import spade.lib.lang.Language;
import spade.lib.util.SmoothingParams;
import spade.time.TimeMoment;
import spade.vis.database.Parameter;

/**
* The UI for selecting the smoothing operation (mean, median, sum, etc.),
* depth, and whether to return the aggregated values or resiguals.
*/
public class TimeSmoothDlg extends Panel implements ItemListener, DialogContent {
	static ResourceBundle res = Language.getTextResource("spade.time.transform.Res");
	/**
	* The smoothing operation names
	*/
	protected static String opNames[] = { res.getString("off"), res.getString("mean"), res.getString("median"), res.getString("maximum"), res.getString("minimum"), res.getString("maximum-minimum"), res.getString("sum") };
	/**
	* The temporal parameter from the table with the data
	*/
	protected Parameter tPar = null;
	/**
	* The radio buttons for selecting the smoothing operation
	*/
	protected Checkbox opCB[] = null;
	/**
	* The radio buttons for selecting between fixed and variable depth
	*/
	protected Checkbox fixedDepthCB = null, varDepthCB = null;
	/**
	* The radio buttons for selecting between returning aggregated values and
	* residuals
	*/
	protected Checkbox aggValsCB = null, resCB = null;
	/**
	* The radio buttons for selecting between smoothing only on the basis of the
	* previous values or the neighboring values in both directions (centered)
	*/
	protected Checkbox backCB = null, centeredCB = null;
	/**
	* The text field for entering the depth of the aggregation
	*/
	protected TextField depthTF = null;
	/**
	* The text field for entering the beginning of the aggregation period (for
	* variable depth)
	*/
	protected TextField aggStartTF = null;
	/**
	* This variable contains the error message if the settings in the dialog are
	* inconsistent
	*/
	public String err = null;

	/**
	* Constructs the UI
	*/
	public TimeSmoothDlg(Parameter tPar, SmoothingParams spar) {
		if (tPar == null || spar == null)
			return;
		this.tPar = tPar;

		CheckboxGroup cbg = new CheckboxGroup();
		opCB = new Checkbox[SmoothingParams.SmoothLAST + 1];
		for (int i = 0; i <= SmoothingParams.SmoothLAST; i++) {
			opCB[i] = new Checkbox(opNames[i], i == spar.smoothMode, cbg);
			opCB[i].addItemListener(this);
		}
		cbg = new CheckboxGroup();
		fixedDepthCB = new Checkbox(res.getString("fixed") + ":", spar.smoothDepth > 0, cbg);
		fixedDepthCB.addItemListener(this);
		varDepthCB = new Checkbox(res.getString("variable") + ":", spar.smoothDepth <= 0, cbg);
		varDepthCB.addItemListener(this);
		cbg = new CheckboxGroup();
		aggValsCB = new Checkbox(res.getString("aggr_val"), !spar.smoothDifference, cbg);
		resCB = new Checkbox(res.getString("residuals") + " (" + res.getString("act_minus_agg") + ")", spar.smoothDifference, cbg);
		cbg = new CheckboxGroup();
		backCB = new Checkbox(res.getString("backwards"), !spar.smoothCentered, cbg);
		centeredCB = new Checkbox(res.getString("both_sides"), spar.smoothCentered, cbg);
		depthTF = new TextField(String.valueOf(spar.smoothDepth), 2);
		aggStartTF = new TextField(10);
		if (spar.smoothStartIdx >= 0 && spar.smoothStartIdx < tPar.getValueCount()) {
			aggStartTF.setText(tPar.getValue(spar.smoothStartIdx).toString());
		}

		setLayout(new ColumnLayout());
		add(new Label(res.getString("temp_aggr")));
		add(opCB[0]);
		add(new Label(res.getString("Operation") + ":"));
		for (int i = 1; i < opCB.length; i++) {
			add(opCB[i]);
		}
		add(new Line(false));
		add(new Label(res.getString("Depth") + ":"));
		Panel p = new Panel(new RowLayout(3, 0));
		p.add(fixedDepthCB);
		p.add(depthTF);
		p.add(new Label(res.getString("moments")));
		Panel pp = new Panel(new GridLayout(2, 1));
		pp.add(backCB);
		pp.add(centeredCB);
		p.add(pp);
		add(p);
		p = new Panel(new RowLayout(3, 0));
		p.add(varDepthCB);
		p.add(new Label(res.getString("all_since")));
		p.add(aggStartTF);
		add(p);
		add(new Line(false));
		add(new Label(res.getString("Return")));
		add(aggValsCB);
		add(resCB);

		enableControls();
	}

	protected void enableControls() {
		if (opCB[0].getState()) { //"off" selected
			//disable all additional options
			fixedDepthCB.setEnabled(false);
			varDepthCB.setEnabled(false);
			depthTF.setEnabled(false);
			backCB.setEnabled(false);
			centeredCB.setEnabled(false);
			aggStartTF.setEnabled(false);
			aggValsCB.setEnabled(false);
			resCB.setEnabled(false);
			return;
		}
		fixedDepthCB.setEnabled(true);
		varDepthCB.setEnabled(true);
		depthTF.setEnabled(fixedDepthCB.getState());
		backCB.setEnabled(fixedDepthCB.getState());
		centeredCB.setEnabled(fixedDepthCB.getState());
		aggStartTF.setEnabled(varDepthCB.getState());
		aggValsCB.setEnabled(true);
		boolean residAllowed = !opCB[SmoothingParams.SmoothMAXMIN].getState() && !opCB[SmoothingParams.SmoothSUM].getState();
		resCB.setEnabled(residAllowed);
		if (!residAllowed && resCB.getState()) {
			aggValsCB.setState(true);
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		enableControls();
	}

	/**
	* Checks the consistency of the settings in the dialog
	*/
	@Override
	public boolean canClose() {
		if (opCB[0].getState())
			return true;
		if (fixedDepthCB.getState()) {
			//check whether the interval length is valid
			String str = depthTF.getText();
			if (str == null || str.trim().length() < 1) {
				err = res.getString("no_depth");
				return false;
			}
			str = str.trim();
			int len = 0;
			try {
				len = Integer.valueOf(str).intValue();
			} catch (NumberFormatException nfe) {
				err = res.getString("ill_depth_string");
				return false;
			}
			if (len < 2) {
				err = res.getString("ill_depth") + ": " + res.getString("must_be") + " " + res.getString("at_least_2") + "!";
				return false;
			}
			if (len >= tPar.getValueCount() - 1) {
				err = res.getString("ill_depth") + ": " + res.getString("there_are_only") + " " + tPar.getValueCount() + " " + res.getString("moments") + "!";
				return false;
			}
		} else {
			String str = aggStartTF.getText();
			if (str == null || str.trim().length() < 1) {
				err = res.getString("no_agg_start");
				return false;
			}
			str = str.trim();
			TimeMoment tm = (TimeMoment) tPar.getFirstValue(), tm1 = tm.getCopy();
			if (!tm1.setMoment(str)) {
				err = res.getString("ill_start_string");
				return false;
			}
			if (tm1.compareTo(tm) < 0) {
				err = res.getString("ill_start_moment") + ": " + res.getString("must_be") + " " + res.getString("not_earlier_than") + " " + tm.toString() + "!";
				return false;
			}
			tm = (TimeMoment) tPar.getValue(tPar.getValueCount() - 2);
			if (tm1.compareTo(tm) > 0) {
				err = res.getString("ill_start_moment") + ": " + res.getString("must_be") + " " + res.getString("not_later_than") + " " + tm.toString() + "!";
				return false;
			}
		}
		return true;
	}

	/**
	* Returns the error message if the settings in the dialog are inconsistent
	*/
	@Override
	public String getErrorMessage() {
		return err;
	}

	/**
	* Fills the smoothing parameters data structure with the values specified by
	* the user and returns the result
	*/
	public SmoothingParams getSmoothingParams() {
		SmoothingParams spar = new SmoothingParams();
		if (opCB[0].getState())
			return spar; //no smoothing
		for (int i = 1; i < opCB.length && spar.smoothMode == SmoothingParams.SmoothNONE; i++)
			if (opCB[i].getState()) {
				spar.smoothMode = i;
			}
		if (fixedDepthCB.getState()) {
			String str = depthTF.getText().trim();
			try {
				spar.smoothDepth = Integer.valueOf(str).intValue();
			} catch (NumberFormatException e) {
			}
			spar.smoothCentered = centeredCB.getState();
			spar.smoothStartIdx = -1;
		} else {
			String str = aggStartTF.getText().trim();
			TimeMoment tm = ((TimeMoment) tPar.getFirstValue()).getCopy();
			tm.setMoment(str);
			int smoothStartIdx = -1;
			for (int i = 0; i < tPar.getValueCount() && smoothStartIdx < 0; i++) {
				if (((TimeMoment) tPar.getValue(i)).compareTo(tm) == 0) {
					smoothStartIdx = i;
				} else if (((TimeMoment) tPar.getValue(i)).compareTo(tm) > 0)
					if (i > 0) {
						smoothStartIdx = i - 1;
					} else {
						smoothStartIdx = i;
					}
			}
			spar.smoothStartIdx = smoothStartIdx;
			spar.smoothDepth = 0;
		}
		spar.smoothDifference = resCB.getState();
		return spar;
	}
}