package spade.time.transform;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.RowLayout;
import spade.lib.lang.Language;
import spade.lib.util.SmoothingParams;
import spade.time.TimeMoment;
import spade.vis.database.Parameter;

/**
* The UI for showing current smoothing parameters of a TimeSmoother. Starts
* a dialog for changing the parameters.
*/
public class TimeSmoothUI extends Panel implements ActionListener, ItemListener {
	static ResourceBundle res = Language.getTextResource("spade.time.transform.Res");
	/**
	* The TimeSmoother to be manipulated
	*/
	protected TimeSmoother tsm = null;
	/**
	* The information panel, which changes its content depending on the current
	* smoothing parameters
	*/
	protected Panel infoPan = null;
	/**
	* Button "Change"
	*/
	protected Button changeBt = null;
	/**
	* The combobox for selecting the smoothing operation
	*/
	protected Choice opCh = null;
	/**
	* The checkbox for selecting between aggregated values and residuals
	*/
	protected Checkbox resCB = null;
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
	* The combobox for selecting between centered and backward smoothing
	*/
	protected Choice backCenterCh = null;

	public TimeSmoothUI(TimeSmoother tSmoother) {
		tsm = tSmoother;
		if (tsm == null)
			return;
		changeBt = new Button(res.getString("change"));
		changeBt.setActionCommand("change");
		changeBt.addActionListener(this);
		ColumnLayout cl = new ColumnLayout();
		cl.setAlignment(ColumnLayout.Hor_Left);
		setLayout(cl);
		add(new Label(res.getString("temp_aggr")));
		makeInfoPanel();
		if (infoPan != null) {
			add(infoPan);
		}
	}

	protected void makeInfoPanel() {
		if (tsm == null)
			return;
		boolean showing = isShowing();
		if (showing) {
			setVisible(false);
		}
		if (infoPan == null) {
			ColumnLayout cl = new ColumnLayout();
			cl.setAlignment(ColumnLayout.Hor_Left);
			infoPan = new Panel(cl);
		} else {
			infoPan.removeAll();
		}
		SmoothingParams spar = tsm.getSmoothingParams();
		if (spar.smoothMode == SmoothingParams.SmoothNONE) {
			Panel p = new Panel(new RowLayout(5, 0));
			p.add(new Label(res.getString("off")));
			p.add(changeBt);
			infoPan.add(p);
		} else {
			if (opCh == null) {
				opCh = new Choice();
				opCh.addItemListener(this);
			} else {
				opCh.removeAll();
			}
			if (spar.smoothMode == SmoothingParams.SmoothMAXMIN || spar.smoothMode == SmoothingParams.SmoothSUM) {
				opCh.add(res.getString("max-min"));
				opCh.add(res.getString("sum"));
				if (spar.smoothMode == SmoothingParams.SmoothSUM) {
					opCh.select(1);
				} else {
					opCh.select(0);
				}
			} else {
				opCh.add(res.getString("mean"));
				opCh.add(res.getString("median"));
				opCh.add(res.getString("max"));
				opCh.add(res.getString("min"));
				opCh.select(spar.smoothMode - 1);
			}
			Panel p = new Panel(new RowLayout(5, 0));
			p.add(opCh);
			p.add(changeBt);
			infoPan.add(p);
			if (spar.smoothDepth > 0) {
				p = new Panel(new RowLayout(5, 0));
				p.add(new Label(res.getString("for")));
				if (depthTF == null) {
					depthTF = new TextField(2);
					depthTF.addActionListener(this);
				}
				depthTF.setText(String.valueOf(spar.smoothDepth));
				p.add(depthTF);
				p.add(new Label(res.getString("moments")));
				if (backCenterCh == null) {
					backCenterCh = new Choice();
					backCenterCh.addItemListener(this);
					backCenterCh.add(res.getString("backwards"));
					backCenterCh.add(res.getString("centered"));
				}
				if (spar.smoothCentered) {
					backCenterCh.select(1);
				} else {
					backCenterCh.select(0);
				}
				p.add(backCenterCh);
				infoPan.add(p);
			} else {
				p = new Panel(new RowLayout(5, 0));
				p.add(new Label(res.getString("for") + " " + res.getString("all_since")));
				if (aggStartTF == null) {
					aggStartTF = new TextField(10);
					aggStartTF.addActionListener(this);
				}
				TimeMoment t = tsm.getAggStartMoment();
				if (t != null) {
					aggStartTF.setText(t.toString());
				} else {
					aggStartTF.setText("");
				}
				p.add(aggStartTF);
				infoPan.add(p);
			}
			if (spar.smoothMode != SmoothingParams.SmoothMAXMIN && spar.smoothMode != SmoothingParams.SmoothSUM) {
				if (resCB == null) {
					resCB = new Checkbox(res.getString("compute_residuals"));
					resCB.addItemListener(this);
				}
				resCB.setState(spar.smoothDifference);
				infoPan.add(resCB);
			}
		}
		if (showing) {
			setVisible(true);
			CManager.validateAll(infoPan.getComponent(0));
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		SmoothingParams spar = tsm.getSmoothingParams();
		if (opCh != null && e.getSource().equals(opCh)) {
			int mode = spar.smoothMode;
			if (mode == SmoothingParams.SmoothMAXMIN || mode == SmoothingParams.SmoothSUM) {
				mode = (opCh.getSelectedIndex() == 0) ? SmoothingParams.SmoothMAXMIN : SmoothingParams.SmoothSUM;
			} else {
				mode = opCh.getSelectedIndex() + 1;
			}
			if (mode != spar.smoothMode) {
				spar.smoothMode = mode;
				tsm.doTransformation();
			}
		} else if (backCenterCh != null && e.getSource().equals(backCenterCh)) {
			boolean centered = backCenterCh.getSelectedIndex() == 1;
			if (centered != spar.smoothCentered) {
				spar.smoothCentered = centered;
				tsm.doTransformation();
			}
		} else if (resCB != null && e.getSource().equals(resCB)) {
			if (resCB.getState() != spar.smoothDifference) {
				spar.smoothDifference = resCB.getState();
				tsm.doTransformation();
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof TextField) {
			SmoothingParams spar = tsm.getSmoothingParams();
			Parameter tPar = tsm.getTemporalParameter();
			if (depthTF != null && e.getSource().equals(depthTF)) {
				int len = 0;
				String str = depthTF.getText();
				if (str != null) {
					str = str.trim();
					if (str.length() > 0) {
						try {
							len = Integer.valueOf(str).intValue();
						} catch (NumberFormatException nfe) {
						}
					}
				}
				if (len < 2 || len >= tPar.getValueCount() - 1) {
					depthTF.setText(String.valueOf(spar.smoothDepth));
				} else if (spar.smoothDepth != len) {
					spar.smoothDepth = len;
					tsm.doTransformation();
				}
				return;
			}
			if (aggStartTF != null && e.getSource().equals(aggStartTF)) {
				int idx = -1;
				String str = aggStartTF.getText();
				if (str != null) {
					str = str.trim();
					if (str.length() > 0) {
						TimeMoment tm0 = (TimeMoment) tPar.getFirstValue(), tm = tm0.getCopy(), tmLast = (TimeMoment) tPar.getValue(tPar.getValueCount() - 2);
						if (tm.setMoment(str) && tm.compareTo(tm0) >= 0 && tm.compareTo(tmLast) <= 0) {
							for (int i = 0; i < tPar.getValueCount() && idx < 0; i++)
								if (((TimeMoment) tPar.getValue(i)).compareTo(tm) == 0) {
									idx = i;
								} else if (((TimeMoment) tPar.getValue(i)).compareTo(tm) > 0)
									if (i > 0) {
										idx = i - 1;
									} else {
										idx = i;
									}
						}
					}
				}
				if (idx < 0) {
					aggStartTF.setText(tPar.getValue(spar.smoothStartIdx).toString());
				} else if (idx != spar.smoothStartIdx) {
					spar.smoothDepth = 0;
					spar.smoothStartIdx = idx;
					tsm.doTransformation();
				}
				return;
			}
		} else {
			String cmd = e.getActionCommand();
			if (cmd == null)
				return;
			if (cmd.equals("change")) {
				TimeSmoothDlg tsd = new TimeSmoothDlg(tsm.getTemporalParameter(), tsm.getSmoothingParams());
				OKDialog okd = new OKDialog(CManager.getAnyFrame(this), res.getString("temp_aggr"), true);
				okd.addContent(tsd);
				okd.show();
				if (!okd.wasCancelled()) {
					tsm.setSmothingParams(tsd.getSmoothingParams());
					makeInfoPanel();
				}
			}
		}
	}
}