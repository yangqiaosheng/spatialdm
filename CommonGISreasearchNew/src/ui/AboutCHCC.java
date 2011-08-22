package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Panel;

import spade.lib.basicwin.ImageCanvas;
import spade.lib.basicwin.TextCanvas;
import spade.lib.util.IconUtil;

public class AboutCHCC extends Panel {
	static String aboutText[] = { "CHCC visualisation software", "Created as a specific adaptation and configuration of CommonGIS", "(http://www.CommonGIS.de/)", "Developed by the SPADE research team", "(http://www.ais.fraunhofer.de/SPADE/)",
			"of Fraunhofer Institute AIS", "(http://www.ais.fraunhofer.de/)" };

	public AboutCHCC() {
		super();
		setLayout(new BorderLayout());
		ImageCanvas iCan = new ImageCanvas(IconUtil.loadImage(this.getClass(), "/icons/CommonGISLogo.gif", 10000));
		iCan.setPreferredSize(117, 133);
		add(iCan, "West");

		TextCanvas tc = new TextCanvas();
		for (String element : aboutText) {
			tc.addTextLine(element);
		}
		tc.addTextLine("-");
		tc.addTextLine("Java version " + java.lang.System.getProperty("java.version") + ", " + java.lang.System.getProperty("os.name") + ", ver." + java.lang.System.getProperty("os.version"));
		tc.toBeCentered = true;
		tc.setForeground(Color.blue.darker());
		tc.setPreferredSize(400, 100);
		add(tc, "Center");
	}
}