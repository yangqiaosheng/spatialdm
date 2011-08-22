package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Panel;
import java.util.ResourceBundle;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.ImageCanvas;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.TextCanvas;
import spade.lib.lang.Language;
import spade.lib.util.IconUtil;

public class AboutSystem extends Panel // implements ActionListener
{
	static ResourceBundle res = Language.getTextResource("ui.Res");
	static String aboutDescartes[] = { res.getString("CommonGIS"), res.getString("System_for"), res.getString("Version") + " 2.1.21, 16/06/2005, 15:15", res.getString("copyright"), res.getString("e-mail"), res.getString("url") };
	static String aboutProjects[] = { res.getString("development_supported_by_EU"), res.getString("within_projects"), res.getString("commongis_project"), "(" + res.getString("commongis_project_url") + "),", res.getString("spin_project"),
			"(" + res.getString("spin_project_url") + "),", res.getString("and_") + res.getString("GIMMI_project"), "(" + res.getString("GIMMI_project_url") + ")" };
	static String others[] = { res.getString("third_parties_software"), "1. " + res.getString("iitp") /*,
																										"2. "+res.getString("uni_Bonn"),
																										"   "+res.getString("uni_Bonn_url")*/};

	public AboutSystem() {
		super();
		setLayout(new ColumnLayout());
		Panel p = new Panel();
		p.setLayout(new BorderLayout());
		ImageCanvas iCan = new ImageCanvas(IconUtil.loadImage(this.getClass(), "/icons/DescartesPhoto.gif", 10000));
		iCan.setPreferredSize(140, 170);
		p.add(iCan, "West");
		TextCanvas tc = new TextCanvas();
		for (String aboutDescarte : aboutDescartes) {
			tc.addTextLine(aboutDescarte);
		}
		tc.addTextLine("-");
		tc.addTextLine(res.getString("Java_version_") + java.lang.System.getProperty("java.version") + ", " + java.lang.System.getProperty("os.name") + ", ver." + java.lang.System.getProperty("os.version"));
		tc.toBeCentered = true;
		tc.setForeground(Color.blue.darker());
		tc.setPreferredSize(200, 100);
		p.add(tc, "Center");
		add(p);
		add(new Line(false));

		p = new Panel(new BorderLayout());
		iCan = new ImageCanvas(IconUtil.loadImage(this.getClass(), "/icons/CommonGISLogo.gif", 10000));
		iCan.setPreferredSize(117, 133);
		p.add(iCan, "West");
		tc = new TextCanvas();
		for (String aboutProject : aboutProjects) {
			tc.addTextLine(aboutProject);
		}
		tc.toBeCentered = true;
		tc.setForeground(Color.blue.darker());
		tc.setPreferredSize(350, 140);
		p.add(tc, "Center");
		iCan = new ImageCanvas(IconUtil.loadImage(this.getClass(), "/icons/SPINLogo.gif", 10000));
		iCan.setPreferredSize(118, 138);
		p.add(iCan, "East");
		add(p);
		add(new Line(false));
		tc = new TextCanvas();
		for (String other : others) {
			tc.addTextLine(other);
		}
		tc.toBeCentered = false;
		tc.setForeground(Color.blue.darker());
		tc.setPreferredSize(200, 30);
		add(tc);
		/*
		Button bFGC=new Button("Force GC");
		bFGC.addActionListener(this);
		add(bFGC);
		*/
	}
	/*
	public void actionPerformed (ActionEvent ae) {
	  if (ae.getSource() instanceof Button) {
	    try {
	      Runtime.getRuntime().gc();
	      System.out.println("Garbage collection()");
	    } catch (SecurityException se) { se.printStackTrace(); }
	  }
	}
	*/
}
