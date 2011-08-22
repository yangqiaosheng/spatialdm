package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Panel;

import spade.lib.basicwin.TextCanvas;

public class AboutProjectTab extends Panel {
	public AboutProjectTab(String descr) {
		super();
		setLayout(new BorderLayout());
		Panel p = new Panel();
		p.setLayout(new BorderLayout());
		add(p, "Center");
		TextCanvas tc = new TextCanvas();
		if (descr != null && descr != "") {
			tc.addTextLine(descr);
		} else {
			tc.addTextLine("No project description available");
		}
		tc.toBeCentered = false;
		tc.setForeground(Color.blue.darker());
		tc.setPreferredSize(300, 100);
		p.add(tc, "Center");
	}
}
