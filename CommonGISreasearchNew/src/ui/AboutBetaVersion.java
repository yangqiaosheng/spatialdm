package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ResourceBundle;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.ImageCanvas;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.TextCanvas;
import spade.lib.lang.Language;
import spade.lib.util.IconUtil;

public class AboutBetaVersion extends Panel // implements ActionListener
{
	static ResourceBundle res = Language.getTextResource("ui.Res");
	static String aboutDescartes[] = { "V-Analytics: Visual Analytics System for Spatial and Temporal Data", "(based on Iris, Descartes, CommonGIS)", res.getString("Version") + " 28/01/2011, 17:40", "(version for 2010 tutorials)", " ",
			//res.getString("debug_info")+" "+res.getString("excluded"),
			//res.getString("debug_info")+" "+res.getString("included"),
			res.getString("no_warranty_string"), res.getString("copyright"), res.getString("e-mail"),
			/*res.getString("url")*/"http://geoanalytics.net/and" };
	static String aboutProjects[] = { "Development of the software was partly supported by EU", "within funded projects", "EuroFigures, CommonGIS, SPIN, GIMMI, EFIS, GeoPKDD", "", "Scientific background:", "N.Andrienko and G.Andrienko",
			"Exploratory Analysis of Spatial and Temporal Data", "A Systematic Approach", "Springer, 2006; ISBN 3-540-25994-5" };
	/*
	{res.getString("development_supported_by_EU"),
	res.getString("within_projects"),
	res.getString("commongis_project"),
	"("+res.getString("commongis_project_url")+"),",
	res.getString("spin_project"),
	"("+res.getString("spin_project_url")+"),",
	res.getString("and_")+res.getString("GIMMI_project"),
	"("+res.getString("GIMMI_project_url")+")"};*/
	static String others[] = { res.getString("third_parties_software"), "1) WEKA by the University of Waikato", "2) Optics, KMedoids etc. by Univ.Pisa / CNR", "3) iXsom by TU Darmstadt", "4) Decision Map by CCAS",
			"5) ColorBrewer by Penn State Univ."
	//"1. "+res.getString("iitp")
	/*,
	"2. "+res.getString("uni_Bonn"),
	"   "+res.getString("uni_Bonn_url")*/};

	public AboutBetaVersion() {
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
		tc.setPreferredSize(300, 100);
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
		tc.setPreferredSize(600, 60);
		add(tc);
		/*
		Button bFGC=new Button("Force GC");
		bFGC.addActionListener(this);
		add(bFGC);
		*/
	}

	/**
	 * main function for UI test
	 * @param args
	 */
	public static void main(String args[]) {
		Frame f = new Frame("AboutBetaVersion");
		f.add(new AboutBetaVersion());
		f.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent we) {
				System.exit(0);
			}
		});
		f.pack();
		f.show();
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
