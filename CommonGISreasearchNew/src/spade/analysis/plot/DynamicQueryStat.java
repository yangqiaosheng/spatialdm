package spade.analysis.plot;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.util.ResourceBundle;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.PercentBar;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;

/**
* Calculates statistics of satisfaction of a query: how many object
* satisfy each constraint separately and all the constraints together.
*/
public class DynamicQueryStat extends Panel {
	static ResourceBundle res = Language.getTextResource("spade.analysis.plot.Res");
	protected int allNum = 0; // total number of objects

	public DynamicQueryStat(int allNum, int nAttr) {
		this.allNum = allNum;
		int len = nAttr + 1;
		setLayout(new GridLayout(len, 1));
		for (int i = 0; i < len; i++) {
			PercentBar pb = new PercentBar(true);
			pb.setValue(100);
			Label l = new Label("   100 % : " + allNum + res.getString("from") + allNum);
			Panel p = new Panel();
			p.setLayout(new ColumnLayout());
			if (i > 0) {
				p.add(new Line(false));
			}
			p.add(l);
			p.add(pb);
			add(p);
		}
	}

	/**
	* Must be called when objects are added or removed. After that call
	* setNumbers for all query attributes!
	*/
	public void setObjectNumber(int number) {
		allNum = number;
	}

	public void setNumbers(int attrN, int nSelected, int nMissing) {
		if (attrN < 0 || attrN >= getComponentCount())
			return;
		float r1 = 100f * nSelected / allNum, r2 = (nMissing == 0) ? Float.NaN : 100f * nMissing / allNum;
		Panel p = (Panel) getComponent(attrN);
		// following text: nSelected+" from "+allNum
		String str = "   " + StringUtil.floatToStr(r1, 1) + "% : " + nSelected + res.getString("from") + allNum;
		// following text: nMissing+" missing values"
		if (nMissing > 0) {
			str += ", " + nMissing + res.getString("missing_values");
		}
		for (int i = 0; i < p.getComponentCount(); i++) {
			Component c = p.getComponent(i);
			if (c == null) {
				continue;
			}
			if (c instanceof PercentBar) {
				PercentBar pb = (PercentBar) c;
				pb.setValue(r1);
				pb.setValue2(r2);
			} else if (c instanceof Label) {
				Label l = (Label) c;
				l.setText(str);
				l.invalidate();
			}
		}
		p.validate();
	}

	public void removeAttr() {
		if (getComponentCount() < 2)
			return;
		remove(getComponentCount() - 1);
		GridLayout gl = (GridLayout) getLayout();
		gl.setRows(gl.getRows() - 1);
	}

	public void addAttr() {
		GridLayout gl = (GridLayout) getLayout();
		gl.setRows(gl.getRows() + 1);
		PercentBar pb = new PercentBar(true);
		pb.setValue(100);
		// following text: allNum+" from "+allNum
		Label l = new Label("   100 % : " + allNum + res.getString("from") + allNum);
		Panel p = new Panel();
		p.setLayout(new ColumnLayout());
		p.add(new Line(false));
		p.add(l);
		p.add(pb);
		add(p);
	}

}
