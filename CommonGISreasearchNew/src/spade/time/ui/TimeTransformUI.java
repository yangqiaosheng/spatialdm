package spade.time.ui;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;

import spade.analysis.system.ESDACore;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.time.Date;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.time.manage.TemporalDataManager;
import spade.time.transform.TimesTransformer;
import spade.vis.database.ObjectContainer;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jul 25, 2008
 * Time: 2:15:57 PM
 * Transforms time references in all containers with time referenced objects
 * from absolute times to various variants of relative times.
 */
public class TimeTransformUI {
	/**
	* A reference to the system's core
	*/
	protected ESDACore core = null;
	/**
	 * Indicates whether the time references in the data are in transformed
	 * or in the original states.
	 */
	protected boolean transformed = false;

	/**
	 * Checks if there are suitable data to transform
	 */
	public boolean canTransform(TemporalDataManager timeManager) {
		if (timeManager == null)
			return false;
		int nCont = timeManager.getContainerCount();
		if (nCont < 1)
			return false;
		TimeMoment tFirst = null, tLast = null;
		for (int i = 0; i < nCont; i++) {
			TimeReference tref = timeManager.getContainer(i).getOriginalTimeSpan();
			if (tref == null) {
				continue;
			}
			TimeMoment t1 = tref.getOrigFrom(), t2 = tref.getOrigUntil();
			if (t1 != null) {
				if (tFirst == null || tFirst.compareTo(t1) > 0) {
					tFirst = t1;
				}
				if (tLast == null || tLast.compareTo(t1) < 0) {
					tLast = t1;
				}
			}
			if (t2 != null) {
				if (tLast == null || tLast.compareTo(t2) < 0) {
					tLast = t2;
				}
			}
		}
		if (tFirst == null || tLast == null || tFirst.compareTo(tLast) >= 0)
			return false;
		if (tFirst instanceof Date) {
			Date d1 = (Date) tFirst.getCopy(), d2 = (Date) tLast.getCopy();
			if (d1.hasElement('d')) {
				long diff = d2.subtract(d1, 'd');
				if (diff > 0) {
					if (d1.hasElement('h'))
						return true; //can be transformed into hours of a day
					if (diff > 7)
						return true; //can be transformed into days of the week
				}
			}
			if (d1.hasElement('y') && d1.hasElement('m')) {
				long diff = d2.subtract(d1, 'm');
				if (diff > 12)
					return true; //can be transformed into months of a year
			}
		}
		for (int i = 0; i < nCont; i++)
			if (timeManager.getContainer(i).containsChangingObjects())
				return true; //can be transformed into times
								//relative w.r.t. start and/or end times of the object existence
		return false;
	}

	/**
	 * Transforms absolute times into relative after asking the user about
	 * the desired method of the transformation. Returns true if actually
	 * transformed.
	 */
	public boolean transformTimes(TemporalDataManager timeManager, ESDACore core) {
		this.core = core;
		if (timeManager == null) {
			showMessage("No time-referenced objects!", true);
			return false;
		}
		int nCont = timeManager.getContainerCount();
		if (nCont < 1) {
			showMessage("No time-referenced objects!", true);
			return false;
		}
		TimeMoment tFirst = null, tLast = null;
		for (int i = 0; i < nCont; i++) {
			TimeReference tref = timeManager.getContainer(i).getOriginalTimeSpan();
			if (tref == null) {
				continue;
			}
			TimeMoment t1 = tref.getOrigFrom(), t2 = tref.getOrigUntil();
			if (t1 != null) {
				if (tFirst == null || tFirst.compareTo(t1) > 0) {
					tFirst = t1;
				}
				if (tLast == null || tLast.compareTo(t1) < 0) {
					tLast = t1;
				}
			}
			if (t2 != null) {
				if (tLast == null || tLast.compareTo(t2) < 0) {
					tLast = t2;
				}
			}
		}
		if (tFirst == null || tLast == null || tFirst.compareTo(tLast) >= 0) {
			showMessage("No time-referenced objects!", true);
			return false;
		}
		Checkbox dayCycleCB = null, weekCycleCB = null, monthCB = null, yearCycleCB = null;
		CheckboxGroup cbg = new CheckboxGroup();
		if (tFirst instanceof Date) {
			Date d1 = (Date) tFirst.getCopy(), d2 = (Date) tLast.getCopy();
			if (d1.hasElement('d')) {
				long diff = d2.subtract(d1, 'd');
				if (diff > 0) {
					if (d1.hasElement('h')) {
						dayCycleCB = new Checkbox("daily cycle", false, cbg);
					}
					if (diff > 7) {
						weekCycleCB = new Checkbox("weekly cycle", false, cbg);
					}
				}
			}
			if (d1.hasElement('m')) {
				long diff = d2.subtract(d1, 'm');
				if (diff > 0 && d1.hasElement('d')) {
					monthCB = new Checkbox("days in a month", false, cbg);
				}
				if (diff > 12 && d1.hasElement('y')) {
					yearCycleCB = new Checkbox("yearly (seasonal) cycle", false, cbg);
				}
			}
		}
		Checkbox startCB = null, endCB = null, startEndCB = null;
		for (int i = 0; i < nCont; i++)
			if (timeManager.getContainer(i).containsChangingObjects()) {
				startCB = new Checkbox("start time", false, cbg);
				endCB = new Checkbox("end time", false, cbg);
				startEndCB = new Checkbox("start and end times", false, cbg);
				break;
			}
		Checkbox origTimesCB = null;
		if (transformed) {
			origTimesCB = new Checkbox("restore the original time references", false, cbg);
		}
		if (dayCycleCB == null && weekCycleCB == null && monthCB == null && yearCycleCB == null && startCB == null && endCB == null && startEndCB == null && origTimesCB == null) {
			showMessage("No suitable time-referenced objects!", true);
			return false;
		}
		Panel mainP = new Panel(new ColumnLayout());
		mainP.add(new Label("Transformation of the time references", Label.CENTER));
		if (dayCycleCB != null || weekCycleCB != null || monthCB != null || yearCycleCB != null) {
			mainP.add(new Line(false));
			mainP.add(new Label("in relation to the temporal cycles:"));
			if (dayCycleCB != null) {
				mainP.add(dayCycleCB);
			}
			if (weekCycleCB != null) {
				mainP.add(weekCycleCB);
			}
			if (monthCB != null) {
				mainP.add(monthCB);
			}
			if (yearCycleCB != null) {
				mainP.add(yearCycleCB);
			}
		}
		if (startCB != null || endCB != null || startEndCB != null) {
			mainP.add(new Line(false));
			mainP.add(new Label("in relation to the individual life times of the objects:"));
			if (startCB != null) {
				mainP.add(startCB);
			}
			if (endCB != null) {
				mainP.add(endCB);
			}
			if (startEndCB != null) {
				mainP.add(startEndCB);
			}
		}
		if (origTimesCB != null) {
			mainP.add(new Line(false));
			mainP.add(origTimesCB);
		}
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Transformation of time", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return false;
		if (origTimesCB != null && origTimesCB.getState()) {
			for (int i = 0; i < nCont; i++) {
				TimesTransformer.restoreOriginalTimes(timeManager.getContainer(i));
			}
			showMessage("The original time references have been restored.", false);
			transformed = false;
			return true;
		}
		TimeMoment minTime = null, maxTime = null;
		if (dayCycleCB != null && dayCycleCB.getState()) {
			for (int i = 0; i < nCont; i++) {
				TimeReference tref = TimesTransformer.transformTimesToDayCycle(timeManager.getContainer(i));
				if (tref != null) {
					if (minTime == null || minTime.compareTo(tref.getValidFrom()) > 0) {
						minTime = tref.getValidFrom();
					}
					if (maxTime == null || maxTime.compareTo(tref.getValidUntil()) < 0) {
						maxTime = tref.getValidUntil();
					}
				}
			}
		} else if (weekCycleCB != null && weekCycleCB.getState()) {
			for (int i = 0; i < nCont; i++) {
				TimeReference tref = TimesTransformer.transformTimesToWeekCycle(timeManager.getContainer(i));
				if (tref != null) {
					if (minTime == null || minTime.compareTo(tref.getValidFrom()) > 0) {
						minTime = tref.getValidFrom();
					}
					if (maxTime == null || maxTime.compareTo(tref.getValidUntil()) < 0) {
						maxTime = tref.getValidUntil();
					}
				}
			}
		} else if (monthCB != null && monthCB.getState()) {
			for (int i = 0; i < nCont; i++) {
				TimeReference tref = TimesTransformer.transformTimesToDaysOfMonth(timeManager.getContainer(i));
				if (tref != null) {
					if (minTime == null || minTime.compareTo(tref.getValidFrom()) > 0) {
						minTime = tref.getValidFrom();
					}
					if (maxTime == null || maxTime.compareTo(tref.getValidUntil()) < 0) {
						maxTime = tref.getValidUntil();
					}
				}
			}
		} else if (yearCycleCB != null && yearCycleCB.getState()) {
			Panel p = new Panel();
			GridBagLayout gridbag = new GridBagLayout();
			p.setLayout(gridbag);
			GridBagConstraints c = new GridBagConstraints();
			c.weightx = 1.0f;
			c.weighty = 1.0f;
			c.fill = GridBagConstraints.HORIZONTAL;
			Label l = new Label("Date of the season start?");
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(l, c);
			p.add(l);
			l = new Label("Day (1-31):");
			c.gridwidth = 1;
			gridbag.setConstraints(l, c);
			p.add(l);
			TextField dTF = new TextField("1", 2);
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(dTF, c);
			p.add(dTF);
			l = new Label("Month (1-12):");
			c.gridwidth = 1;
			gridbag.setConstraints(l, c);
			p.add(l);
			TextField mTF = new TextField("1", 2);
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(mTF, c);
			p.add(mTF);
			dia = new OKDialog(core.getUI().getMainFrame(), "Season start?", true);
			dia.addContent(p);
			dia.show();
			if (dia.wasCancelled())
				return false;
			int d = 0, m = 0;
			try {
				d = Integer.parseInt(dTF.getText());
			} catch (Exception e) {
				d = 0;
			}
			if (d < 1 || d > 31) {
				core.getUI().showMessage("Invalid day: " + d + "!", true);
				return false;
			}
			try {
				m = Integer.parseInt(mTF.getText());
			} catch (Exception e) {
				m = 0;
			}
			if (m < 1 || m > 12) {
				core.getUI().showMessage("Invalid month: " + m + "!", true);
				return false;
			}
			Date date0 = new Date();
			date0.scheme = "dd/mm";
			date0.setElementValue('m', m);
			date0.setElementValue('d', d);
			date0.setPrecision('d');
			if (!date0.isValid()) {
				core.getUI().showMessage("Invalid date: " + date0 + "!", true);
				return false;
			}
			for (int i = 0; i < nCont; i++) {
				TimeReference tref = TimesTransformer.transformTimesToSeasonalCycle(timeManager.getContainer(i), date0);
				if (tref != null) {
					if (minTime == null || minTime.compareTo(tref.getValidFrom()) > 0) {
						minTime = tref.getValidFrom();
					}
					if (maxTime == null || maxTime.compareTo(tref.getValidUntil()) < 0) {
						maxTime = tref.getValidUntil();
					}
				}
			}
		} else if (startEndCB != null && startEndCB.getState()) {
			for (int i = 0; i < nCont; i++) {
				TimeReference tref = TimesTransformer.transformTimesRelativeToStartAndEnd(timeManager.getContainer(i));
				if (tref != null) {
					if (minTime == null || minTime.compareTo(tref.getValidFrom()) > 0) {
						minTime = tref.getValidFrom();
					}
					if (maxTime == null || maxTime.compareTo(tref.getValidUntil()) < 0) {
						maxTime = tref.getValidUntil();
					}
				}
			}
		} else if (startCB != null && startCB.getState()) {
			for (int i = 0; i < nCont; i++) {
				TimeReference tref = TimesTransformer.transformTimesRelativeToStart(timeManager.getContainer(i));
				if (tref != null) {
					if (minTime == null || minTime.compareTo(tref.getValidFrom()) > 0) {
						minTime = tref.getValidFrom();
					}
					if (maxTime == null || maxTime.compareTo(tref.getValidUntil()) < 0) {
						maxTime = tref.getValidUntil();
					}
				}
			}
		} else if (endCB != null && endCB.getState()) {
			long maxLen = 0L;
			for (int i = 0; i < nCont; i++) {
				ObjectContainer cont = timeManager.getContainer(i);
				for (int j = 0; j < cont.getObjectCount(); j++) {
					TimeReference tref = cont.getObjectData(j).getTimeReference();
					if (tref != null && tref.getValidFrom() != null && tref.getValidUntil() != null) {
						long len = tref.getValidUntil().subtract(tref.getValidFrom());
						if (len > maxLen) {
							maxLen = len;
						}
					}
				}
			}
			if (maxLen < 1) {
				showMessage("All time references have zero length!", true);
				return false;
			}
			for (int i = 0; i < nCont; i++) {
				TimeReference tref = TimesTransformer.transformTimesRelativeToEnd(timeManager.getContainer(i), maxLen);
				if (tref != null) {
					if (minTime == null || minTime.compareTo(tref.getValidFrom()) > 0) {
						minTime = tref.getValidFrom();
					}
					if (maxTime == null || maxTime.compareTo(tref.getValidUntil()) < 0) {
						maxTime = tref.getValidUntil();
					}
				}
			}
		}
		if (minTime == null || maxTime == null) {
			showMessage("No transformations have been made!", true);
			return false;
		}
		if (minTime.compareTo(maxTime) >= 0) {
			showMessage("All times became equal. The original times are being restored...", true);
			for (int i = 0; i < nCont; i++) {
				TimesTransformer.restoreOriginalTimes(timeManager.getContainer(i));
			}
			showMessage("All transformed times were equal. The original times have been restored.", true);
			transformed = false;
		} else {
			showMessage("The time references have been transformed.", false);
			transformed = true;
		}
		return true;
	}

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
	}
}
