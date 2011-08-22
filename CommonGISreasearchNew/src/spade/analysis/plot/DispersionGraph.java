package spade.analysis.plot;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.TImgButton;
import spade.lib.lang.Language;
import spade.vis.database.AttributeDataPortion;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Fraunhofer AIS.SPADE</p>
 * @author unascribed
 * @version 1.0
 */

public class DispersionGraph extends Panel implements ActionListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.plot.Res");

	TImgButton upBt;
	TImgButton reverseBt;
	DispersionGraphCanvas dispGraph;
	TextField currMinTF;
	TextField currMaxTF;

	public DispersionGraph(AttributeDataPortion table, String attrId, Supervisor supervisor) {
		setLayout(new BorderLayout());
		//Panel sp = new Panel(new FlowLayout(FlowLayout.LEFT,2,1));
		Panel sp = new Panel(new BorderLayout());
		add(sp, "North");
		if (table != null && attrId != null) {
			int idx = table.getAttrIndex(attrId);
			if (idx >= 0) {
				sp.add(new Label(table.getAttributeName(idx)), "Center");
			}
		}

		Panel spp = new Panel(new FlowLayout(FlowLayout.LEFT, 2, 1));
		sp.add(spp, "East");

		upBt = new TImgButton("/icons/arrow.gif");
		upBt.addActionListener(this);
		spp.add(upBt);

		reverseBt = new TImgButton("/icons/shift.gif");
		reverseBt.addActionListener(this);
		spp.add(reverseBt);
		dispGraph = new DispersionGraphCanvas();
		dispGraph.setTable(table);
		dispGraph.setAttribute(attrId);
		dispGraph.setSupervisor(supervisor);

		Panel tp = new Panel(new BorderLayout());
		add(tp, "Center");
		tp.add(dispGraph, "Center");
		Panel tpp = new Panel(new GridLayout(1, 3));
		tp.add(tpp, "North");
		currMinTF = new TextField();
		tpp.add(currMinTF);
		tpp.add(new Label(" "));
		currMaxTF = new TextField();
		tpp.add(currMaxTF);
		dispGraph.setTextFields(currMinTF, currMaxTF);
		//add(dispGraph,"Center");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if (o == reverseBt) {
			dispGraph.setInverse(!dispGraph.isInverse());
		} else if (o == upBt) {
			dispGraph.shiftColors();
		}
	}

	public DispersionGraphCanvas getGraph() {
		return dispGraph;
	}
}