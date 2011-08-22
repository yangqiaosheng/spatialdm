package spade.analysis.tools;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Label;
import java.awt.Panel;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.ESDACore;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.IntArray;
import spade.time.TimeMoment;
import spade.vis.database.AttributeDataPortion;
import ui.AttributeChooser;
import ui.TableManager;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffSaver;

public class WekaKnowledgeExplorer implements DataAnalyser {

	protected boolean AnyNominalAttribute = false;
	protected Instances instances = null;
	protected int tblRowsInInstances[] = null;

	// Men�punkt erscheint nur, wenn auch weka definiert ist.
	@Override
	public boolean isValid(ESDACore core) {
		try {
			return (null != Class.forName("weka.core.Instance"));
		} catch (Exception e) {
			return false;
		}
	}

	public void saveToFile(String file) {
		try {
			if (instances == null)
				throw new NullPointerException();
			ArffSaver saver = new ArffSaver();
			saver.setInstances(instances);
			saver.setFile(new File(file));
			saver.setDestination(new File(file));
			saver.writeBatch();
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}
	}

	public Instances loadFromFile(String file) {
		Instances insts = null;
		try {
			ArffLoader loader = new ArffLoader();
			loader.setFile(new File(file));
			loader.setSource(new File(file));
			insts = loader.getDataSet();
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}
		return insts;
	}

	// Schnittstelle nach Weka, Konvertierung der Daten, aufrufen von
	// AttributeSelectioPanel1
	@Override
	public void run(ESDACore core) {
		System.out.println("CommonGIS-Weka-Interface.");
		AttributeDataPortion tbl = selectTable(core);
		if (tbl == null)
			return;
		buildInstances(core, tbl);
		if (instances == null)
			return;
		// �bergabe an Weka
		Panel p = new Panel();
		p.setLayout(new ColumnLayout());
		CheckboxGroup cbg = new CheckboxGroup();
		Checkbox cbOption1 = new Checkbox("Attribute Selection", true, cbg);
		p.add(cbOption1);
		Checkbox cbOption2 = new Checkbox("Clustering", true, cbg);
		p.add(cbOption2);
		Checkbox cbOption3 = new Checkbox("J48 classification", false, cbg);
		cbOption3.setEnabled(AnyNominalAttribute);
		p.add(cbOption3);
		Checkbox cbOption4 = null;
		if (core.getSystemSettings().checkParameterValue("isLocalSystem", "true")) {
			try {
				if (Class.forName("javax.swing.JPanel") != null) {
					cbOption4 = new Checkbox("Knowledge Explorer UI", false, cbg);
					p.add(new Line(false));
					p.add(cbOption4);
				}
			} catch (Exception e) {
			}
		}
		p.add(new Line(false));
		Label l = null;
		p.add(l = new Label("Attention!"));
		l.setForeground(Color.red);
		p.add(l = new Label("All Weka-based methods are applied to the current state"));
		l.setForeground(Color.red);
		p.add(l = new Label(" of data and don't react to further data changes."));
		l.setForeground(Color.red);
		p.add(l = new Label("All data records with missing values are ignored."));
		l.setForeground(Color.red);
		p.add(new Line(false));
		OKDialog dlg = new OKDialog(CManager.getAnyFrame(), "WeKa operation", true);
		dlg.addContent(p);
		dlg.show();
		if (dlg.wasCancelled())
			return;
		if (cbOption1.getState()) {
			new WekaAttributeSelectorCP(instances, core, tbl);
		}
		if (cbOption2.getState()) {
			new WekaClusterersCP(instances, tblRowsInInstances, core, tbl);
		}
		if (cbOption3.getState()) {
			new WekaJ48CP(instances, core, tbl);
		}
		if (cbOption4 != null && cbOption4.getState()) {
			runWeka(instances);
		}
	}

	public void runWekaSimpleKMeans(ESDACore core, AttributeDataPortion tbl) {
		Vector attrIds = new Vector(10, 10);
		for (int i = 0; i < tbl.getAttrCount(); i++)
			if (tbl.getAttribute(i).isNumeric()) {
				attrIds.addElement(tbl.getAttributeId(i));
			}
		if (attrIds.size() > 0) {
			runWekaSimpleKMeans(core, tbl, attrIds, 5);
		}
		int n = 1;
	}

	public Vector<double[][]> runWekaSimpleKMeans(ESDACore core, AttributeDataPortion tbl, Vector attrIds, int maxClasses) {
		return runWekaSimpleKMeans(core, tbl, attrIds, 2, maxClasses);
	}

	public Vector<double[][]> runWekaSimpleKMeans(ESDACore core, AttributeDataPortion tbl, Vector attrIds, int minClasses, int maxClasses) {
		buildInstances(core, tbl, attrIds);
		if (instances == null)
			return null;
		Vector<double[][]> vCentroids = new Vector<double[][]>(maxClasses - minClasses + 1);
		for (int nCl = minClasses; nCl <= maxClasses; nCl++) {
			WekaSimpleKMeans wSKM = new WekaSimpleKMeans(instances, tblRowsInInstances, core, tbl, attrIds, nCl);
			Instances iCentroids = wSKM.clusterCentroids;
			double dCentroids[][] = new double[iCentroids.numInstances()][];
			for (int i = 0; i < dCentroids.length; i++) {
				dCentroids[i] = new double[iCentroids.numAttributes()];
				for (int j = 0; j < dCentroids[i].length; j++) {
					dCentroids[i][j] = iCentroids.instance(i).value(j);
				}
			}
			vCentroids.addElement(dCentroids);
		}
		return vCentroids;
	}

	protected AttributeDataPortion selectTable(ESDACore core) {
		DataKeeper dk = core.getDataKeeper();
		if (dk.getTableCount() < 1)
			return null;
		TableManager tman = new TableManager();
		tman.setDataKeeper(dk);
		tman.setUI(core.getUI());
		int tn = tman.selectTableNumber("Select Table");
		if (tn < 0)
			return null;
		return dk.getTable(tn);
	}

	/**
	 * Method builds Weka instances from CommonGIS table For nominal attributes
	 * value lists and colours are created
	 */
	protected void buildInstances(ESDACore core, AttributeDataPortion tbl) {
		buildInstances(core, tbl, null);
	}

	protected void buildInstances(ESDACore core, AttributeDataPortion tbl, Vector attr) {
		// Auswahl der einzelnen Attribute
		if (attr == null) {
			AttributeChooser attrSel = new AttributeChooser();
			attrSel.selectColumns(tbl, "Select Attributes", core.getUI());
			attr = attrSel.getSelectedColumnIds();
		}
		if (attr == null)
			return;
		// Intarray attrNumbers wird gef�llt
		IntArray attrNumbers = null;
		if (attr != null && attr.size() > 0 && tbl != null && tbl.getAttrCount() > 0) {
			attrNumbers = new IntArray();
			for (int i = 0; i < attr.size(); i++) {
				int idx = tbl.getAttrIndex((String) attr.elementAt(i));
				if (idx >= 0) {
					attrNumbers.addElement(idx);
				}
			}
		}
		// Neue Instances wird kreiert und sukzessive mit den Attributen gef�llt
		FastVector fv = new FastVector();
		for (int i = 0; i < attrNumbers.size(); i++) {
			int attrN = attrNumbers.elementAt(i);
			Attribute at = null;
			if (tbl.isAttributeNumeric(attrN)) {
				at = new Attribute(tbl.getAttributeId(attrN));
			} else {
				AnyNominalAttribute = true;
				if (tbl.isAttributeTemporal(attrN)) {
					at = new Attribute(tbl.getAttributeId(attrN));
				} else {

					FastVector f = new FastVector();
					String s[] = tbl.getAttribute(attrN).getValueList();
					if (s != null && s.length > 0) {
						for (String element : s) {
							f.addElement(element);
						}
					} else {
						String vlist[] = tbl.getAttribute(attrN).getValueList();
						if (vlist == null) {
							Vector v = tbl.getAllAttrValuesAsStrings(tbl.getAttributeId(attrN));
							vlist = new String[v.size()];
							for (int j = 0; j < vlist.length; j++) {
								vlist[j] = (String) v.elementAt(j);
							}
							tbl.getAttribute(attrN).setValueListAndColors(vlist, null);
							tbl.getAttribute(attrN).setupDefaultColors();
							vlist = tbl.getAttribute(attrN).getValueList();
						}
						for (String element : vlist) {
							f.addElement(element);
							/*
							 * more comlex method (see above) is used to ensure that
							 * all colours are assigned and orders of values and
							 * colours are preserved !!! Vector
							 * v=tbl.getAllAttrValuesAsStrings(tbl.getAttributeId
							 * (attrN)); for (int j=0; j<v.size(); j++)
							 * f.addElement(v.elementAt(j));
							 */
						}
					}
					at = new Attribute(tbl.getAttributeId(attrN), f);
				}
			}
			fv.addElement(at);
		}
		instances = new Instances(tbl.getName(), fv, 0);
		tblRowsInInstances = new int[tbl.getDataItemCount()];
		// Instance-Daten werden erstellt & zur Instances hinzugef�gt
		String st = "";
		int x = 0;
		for (int i = 0; i < tbl.getDataItemCount(); i++)
			if (tbl.getObjectFilter() == null || tbl.getObjectFilter().isActive(i)) {
				Instance in = new Instance(instances.numAttributes());
				tblRowsInInstances[instances.numInstances()] = i;
				for (int j = 0; j < attrNumbers.size(); j++) {
					int attrN = attrNumbers.elementAt(j);
					if (tbl.isAttributeNumeric(attrN)) {
						double val = tbl.getNumericAttrValue(attrN, i);
						if (Double.isNaN(val)) {
							in.setMissing(instances.attribute(j));
						} else {
							in.setValue(instances.attribute(j), val);
						}
					} else {
						if (tbl.isAttributeTemporal(attrN)) {
							TimeMoment time = (TimeMoment) tbl.getAttrValue(attrN, i);
							double val = time.toNumber();
							if (Double.isNaN(val)) {
								in.setMissing(instances.attribute(j));
							} else {
								in.setValue(instances.attribute(j), val);
							}
						} else {
							String str = tbl.getAttrValueAsString(attrN, i);
							if (str == null) {
								in.setMissing(instances.attribute(j));
							} else {
								in.setValue(instances.attribute(j), str);
							}
						}
					}
				}
				instances.add(in);
			}
		return;
	}

	protected void runWeka(Instances ins) {
		// weka.gui.explorer.Explorer wekaExplorer=new Explorer();
		// wekaExplorer.main(ins);
		try { // Java 1.2
			Class wekaExplorer = Class.forName("weka.adds.Explorer2");
			if (wekaExplorer != null) {
				Object wekaExplorerInstance = wekaExplorer.newInstance();
				if (wekaExplorerInstance != null) {
					Class parmTypes[] = new Class[1];
					parmTypes[0] = Class.forName("weka.core.Instances");
					Method wekaExplorerMain = wekaExplorer.getMethod("main", parmTypes);
					if (wekaExplorerMain != null) {
						Object params[] = new Object[1];
						params[0] = ins;
						wekaExplorerMain.invoke(wekaExplorerInstance, params);
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
