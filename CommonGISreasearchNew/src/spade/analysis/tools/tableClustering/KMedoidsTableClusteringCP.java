package spade.analysis.tools.tableClustering;

import it.unipi.di.sax.kmedoids.BisectingKMedoids;
import it.unipi.di.sax.kmedoids.ClusterQualityMeasure;
import it.unipi.di.sax.kmedoids.DiameterClusterQualityMeasure;
import it.unipi.di.sax.kmedoids.KMedoids;
import it.unipi.di.sax.optics.DistanceMeter;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

import spade.analysis.plot.bargraph.CorrelationBarGraph;
import spade.analysis.system.DataKeeper;
import spade.analysis.system.DisplayProducer;
import spade.analysis.system.ESDACore;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.PlotCanvas;
import spade.lib.color.CS;
import spade.lib.util.FloatArray;
import spade.lib.util.StringUtil;
import spade.vis.action.Highlighter;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataTable;
import spade.vis.space.GeoLayer;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Jan 2, 2009
 * Time: 3:28:42 PM
 * Based on spade.analysis.tools.WekaClusterersCP
 */
public class KMedoidsTableClusteringCP extends Frame implements ActionListener, ItemListener {

	protected ESDACore core = null;
	protected AttributeDataPortion tbl = null;
	protected int attrClNIdx = -1, attrIsCIdx = -1;
	protected ArrayList<FloatArray> data = null;
	float attrRanges[] = null;

	int results[] = null;
	boolean iscentroid[] = null;
	Vector vClusterSizes = null;
	float maxClusterSize = 0f;

	Choice chClusterer = null;
	TextField tfNClusters = null, tfRadius = null;
	Checkbox cbNormalize = null;
	Choice chDistFunc = null;
	Checkbox cbNewAttr = null;
	Label lAttrName = null;
	Button bRun = null;

	CorrelationBarGraph hbg = null;
	PlotCanvas gCanvas = null;
	ScrollPane sp = null;

	public KMedoidsTableClusteringCP(ESDACore core, AttributeDataPortion tbl, ArrayList<FloatArray> data, String attrNames[], float attrRanges[]) {
		super("KMedoids Table Clusterer Control Panel");
		this.data = data;
		this.core = core;
		this.tbl = tbl;
		this.attrRanges = attrRanges;
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				dispose();
			}
		});
		setLayout(new BorderLayout());
		Panel p = new Panel(new ColumnLayout());
		add(p, BorderLayout.CENTER);
		p.add(new Label("Selected attributes:"));
		for (String attrName : attrNames) {
			p.add(new Label(attrName));
		}
		p.add(new Line(false));
		Panel pp = new Panel(new FlowLayout());
		pp.add(new Label("Clusterer:"));
		pp.add(chClusterer = new Choice());
		chClusterer.add("KMedoids");
		chClusterer.add("Bisecting KMedoids");
		chClusterer.addItemListener(this);
		p.add(pp);
		pp = new Panel(new FlowLayout());
		pp.add(new Label("Distance function:"));
		pp.add(chDistFunc = new Choice());
		chDistFunc.add("Euclidean");
		chDistFunc.add("Manchattan");
		p.add(pp);
		p.add(cbNormalize = new Checkbox("normalize attributes by min/max"));
		p.add(new Line(false));
		pp = new Panel(new FlowLayout());
		pp.add(new Label("Number of Clusters:"));
		pp.add(tfNClusters = new TextField("2", 4));
		p.add(pp);
		pp = new Panel(new FlowLayout());
		pp.add(new Label("Radius:"));
		pp.add(tfRadius = new TextField("500", 6));
		tfRadius.setEnabled(false);
		p.add(pp);
		p.add(new Line(false));
		pp = new Panel();
		pp.setLayout(new BorderLayout());

		bRun = new Button("Run clusterer");
		bRun.addActionListener(this);
		pp.add(bRun, BorderLayout.CENTER);
		cbNewAttr = new Checkbox("Store results in new attribute", true);
		cbNewAttr.setEnabled(false);
		pp.add(cbNewAttr, BorderLayout.EAST);
		p.add(pp);
		p.add(lAttrName = new Label("Attribute name="));

		p.add(new Line(false));
		p.add(new Label("Radii of resulting clusters:"));
		hbg = new CorrelationBarGraph();
		hbg.setMargin(5);
		hbg.setSpace(0, 0, 0);
		hbg.setMinBarWidth(Metrics.getFontMetrics().getHeight());
		hbg.setMinSpaceWidth(0);
		hbg.setMaxBarWidth(Metrics.getFontMetrics().getHeight());
		gCanvas = new PlotCanvas();
		hbg.setCanvas(gCanvas);
		gCanvas.setContent(hbg);
		Vector v = new Vector(1, 1);
		v.add(new Float(0));
		hbg.setValues(v);
		v = new Vector(1, 1);
		v.add("  ");
		hbg.setDescriptions(v);
		hbg.setDrawSigned(false);
		sp = new ScrollPane();
		sp.add(gCanvas);
		sp.setSize(new Dimension(110 * Metrics.mm(), 90 * Metrics.mm()));
		p.add(sp);

		setSize(500, 300);
		pack();
		show();
		//The window must be properly registered in order to be closed in a case
		//when the aplication is closed or changed.
		core.getWindowManager().registerWindow(this);
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (ie.getSource().equals(chClusterer)) {
			tfNClusters.setEnabled(chClusterer.getSelectedIndex() == 0);
			tfRadius.setEnabled(chClusterer.getSelectedIndex() > 0);
		}
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource().equals(bRun)) {
			runClusterer();
		}
	}

	protected void runClusterer() {
		results = new int[tbl.getDataItemCount()];
		iscentroid = new boolean[tbl.getDataItemCount()];
		for (int i = 0; i < results.length; i++) {
			results[i] = -1;
			iscentroid[i] = false;
		}
		int nclasses = 0;
		boolean error = false;
		if (chClusterer.getSelectedIndex() == 0) { // KMedoids
			try {
				nclasses = Integer.parseInt(tfNClusters.getText());
				if (nclasses < 2) {
					nclasses = 2;
					error = true;
				}
			} catch (NumberFormatException nfe) {
				error = true;
			}
			if (error) {
				tfNClusters.setText("2");
			} else {
				runKMedoids(nclasses);
				tfRadius.setText(StringUtil.floatToStr(maxClusterSize, 0, maxClusterSize));
			}
		} else { // BisectingKMedoids
			float radius = 0f;
			try {
				radius = Float.parseFloat(tfRadius.getText());
				if (radius <= 0) {
					nclasses = 100;
					error = true;
				}
			} catch (NumberFormatException nfe) {
				error = true;
			}
			if (error) {
				tfRadius.setText("0.1");
			} else {
				nclasses = runBisectingKMedoids(radius);
				tfNClusters.setText("" + nclasses);
			}
		}
		if (!error) {
			storeResultsInTable(nclasses);
			// update display
			cbNewAttr.setState(false);
			hbg.setValues(vClusterSizes);
			Vector v = new Vector(1, 1);
			for (int i = 0; i < nclasses; i++) {
				v.add("" + (1 + i));
			}
			hbg.setDescriptions(v);
			hbg.setBorderValue(maxClusterSize);
			hbg.setup();
			hbg.draw(gCanvas.getGraphics());
			sp.doLayout();
		}
	}

	protected void storeResultsInTable(int nclasses) {
		DataTable dTable = (DataTable) tbl;
		if (attrClNIdx >= 0 && !cbNewAttr.getState()) {
			updateTable(dTable, nclasses, results);
			// inform all displays about change of values
			Vector attr = new Vector(1, 1);
			attr.addElement(dTable.getAttributeId(attrClNIdx));
			attr.addElement(dTable.getAttributeId(attrIsCIdx));
			dTable.notifyPropertyChange("values", null, attr);
		} else {
			int i = 0;
			String attrName = "";
			do {
				i++;
				attrName = "Cluster " + i;
			} while (dTable.findAttrByName(attrName) >= 0);
			lAttrName.setText("Attribute name=" + attrName);
			cbNewAttr.setEnabled(true);
			dTable.addAttribute(attrName, null, AttributeTypes.character);
			attrClNIdx = dTable.getAttrCount() - 1;
			dTable.addAttribute("IsCentroid " + i, null, AttributeTypes.logical);
			attrIsCIdx = dTable.getAttrCount() - 1;
			updateTable(dTable, nclasses, results);
			DataKeeper dk = core.getDataKeeper();
			GeoLayer layer = dk.getTableLayer(dTable);
			if (layer != null) {
				Vector clusterAttr = new Vector(1, 1);
				clusterAttr.addElement(dTable.getAttributeId(attrClNIdx));
				DisplayProducer dpr = core.getDisplayProducer();
				dpr.displayOnMap("qualitative_colour", dTable, clusterAttr, layer, core.getUI().getMapViewer(0));
			}
		}

	}

	protected void runKMedoids(int nclasses) {

		KMedoids<FloatArray> km = new KMedoids<FloatArray>(new DistanceMeter<FloatArray>() {
			@Override
			public double distance(FloatArray o1, FloatArray o2) {
				if (o1 == null || o2 == null)
					return 0f;
				float d = 0f;
				if (chDistFunc.getSelectedIndex() == 0) {
					for (int i = 1; i < o1.size(); i++) {
						float diff = o1.elementAt(i) - o2.elementAt(i);
						if (cbNormalize.getState()) {
							diff /= attrRanges[i - 1];
						}
						d += Math.pow(diff, 2);
					}
				} else {
					for (int i = 1; i < o1.size(); i++) {
						float diff = Math.abs(o1.elementAt(i) - o2.elementAt(i));
						if (cbNormalize.getState()) {
							diff /= attrRanges[i - 1];
						}
						if (diff > d) {
							d = diff;
						}
					}
				}
				return d;
			}

			@Override
			public Collection<FloatArray> neighbors(FloatArray core, Collection<FloatArray> objects, double eps) {
				// no need for this
				return null;
			}
		});

		HashMap<FloatArray, ArrayList<FloatArray>> cls = km.doClustering(data, nclasses);
		getClusterRadii(km, cls);
		getResultsFromHashMap(cls, results, iscentroid);
	}

	protected int runBisectingKMedoids(float diam) {

		DistanceMeter<FloatArray> dm = new DistanceMeter<FloatArray>() {
			@Override
			public double distance(FloatArray o1, FloatArray o2) {
				if (o1 == null || o2 == null)
					return 0f;
				float d = 0f;
				if (chDistFunc.getSelectedIndex() == 0) {
					for (int i = 1; i < o1.size(); i++) {
						float diff = o1.elementAt(i) - o2.elementAt(i);
						if (cbNormalize.getState()) {
							diff /= attrRanges[i - 1];
						}
						d += Math.pow(diff, 2);
					}
				} else {
					for (int i = 1; i < o1.size(); i++) {
						float diff = Math.abs(o1.elementAt(i) - o2.elementAt(i));
						if (cbNormalize.getState()) {
							diff /= attrRanges[i - 1];
						}
						if (diff > d) {
							d = diff;
						}
					}
				}
				return d;
			}

			@Override
			public Collection<FloatArray> neighbors(FloatArray core, Collection<FloatArray> objects, double eps) {
				// no need for this
				return null;
			}
		};

		ClusterQualityMeasure<FloatArray> cqm = new DiameterClusterQualityMeasure<FloatArray>(dm, diam);
		BisectingKMedoids<FloatArray> km = new BisectingKMedoids<FloatArray>(dm, cqm);
		HashMap<FloatArray, ArrayList<FloatArray>> cls = km.doBisecting(data);
		getClusterRadii(km, cls);
		return getResultsFromHashMap(cls, results, iscentroid);
	}

	private int getResultsFromHashMap(HashMap<FloatArray, ArrayList<FloatArray>> cls, int results[], boolean iscentroid[]) {
		int cln = 0;
		for (FloatArray c : cls.keySet())
			if (c != null) {
				int rowN = (int) c.elementAt(0);
				//System.out.println("Centroid: " + rowN);
				results[rowN] = cln;
				iscentroid[rowN] = true;
				for (FloatArray v : cls.get(c)) {
					rowN = (int) v.elementAt(0);
					//System.out.println("  " + rowN);
					results[rowN] = cln;
				}
				cln++;
			}
		return cln;
	}

	private void getClusterRadii(KMedoids<FloatArray> km, HashMap<FloatArray, ArrayList<FloatArray>> cls) {
		double r[] = km.getClustersRadii(cls);
		maxClusterSize = 0;
		vClusterSizes = new Vector(r.length, 10);
		for (double element : r) {
			vClusterSizes.add(new Float(element));
			if (element > maxClusterSize) {
				maxClusterSize = (float) element;
			}
		}
	}

	private void updateTable(DataTable table, int nclasses, int[] results) {
		spade.vis.database.Attribute cgattr = table.getAttribute(attrClNIdx);
		String valueList[] = new String[nclasses];
		Color valueColors[] = new Color[nclasses];
		for (int i = 0; i < nclasses; i++) {
			valueList[i] = "cl " + (i + 1);
			if (nclasses < 10) {
				valueColors[i] = CS.getNiceColor(i); //scs.getColorForValue(k);
			} else {
				valueColors[i] = CS.getNiceColor(i % 9);
				int cn = i;
				if (i > 44) {
					valueColors[cn] = CS.getBWColor(i - 44, nclasses - 44);
				} else {
					while (cn > 9) {
						cn -= 9;
						valueColors[i] = valueColors[i].darker();
					}
				}
			}
		}
		cgattr.setValueListAndColors(valueList, valueColors);
		Vector vids = new Vector(nclasses, nclasses);
		for (int i = 0; i < results.length; i++)
			if (results[i] >= 0 && results[i] < nclasses) {
				table.getDataRecord(i).setAttrValue(valueList[Math.round(results[i])], attrClNIdx);
				table.getDataRecord(i).setAttrValue((iscentroid[i]) ? "y" : "n", attrIsCIdx);
				if (iscentroid[i]) {
					vids.addElement(table.getObjectId(i));
				}
			} else {
				table.getDataRecord(i).setAttrValue(null, attrClNIdx);
				table.getDataRecord(i).setAttrValue(null, attrIsCIdx);
			}
		Highlighter highlighter = core.getSupervisor().getHighlighter(table.getEntitySetIdentifier());
		highlighter.clearSelection(this);
		highlighter.makeObjectsSelected(this, vids);
	}

}
