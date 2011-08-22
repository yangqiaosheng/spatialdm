package spade.analysis.classification;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Label;
import java.awt.Panel;

import spade.analysis.manipulation.Manipulator;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.FoldablePanel;
import spade.lib.basicwin.Line;
import spade.vis.database.AttributeDataPortion;

public class QualitativeCrossClassManipulator extends Panel implements Manipulator //, ActionListener, ItemListener, ColorListener, PropertyChangeListener
{
	protected Supervisor sup = null;
	protected QualitativeCrossClassifier qcc = null;

	/**
	* Construction of map manipulator. Returns true if successfully constructed.
	* Arguments: 1) supervisor responsible for dynamic linking of displays;
	* 2) visualizer or classifier to be manipulated; 3) table with source data;
	* 4) table filter
	* The Manipulator must check if the visualizer has the appropriate type.
	* For QualitativeClassManipulator visualizer should be an instance of
	* QualitativeClassifier.
	*/
	@Override
	public boolean construct(Supervisor sup, Object visualizer, AttributeDataPortion dataTable) {
		if (visualizer == null)
			return false;
		if (!(visualizer instanceof QualitativeCrossClassifier))
			return false;
		qcc = (QualitativeCrossClassifier) visualizer;
		//qcc.addPropertyChangeListener(this);
		this.sup = sup;
		setLayout(new ColumnLayout());
		makeInterior();
		return true;
	}

	protected void makeInterior() {
		//add(new Label(qcc.getAttributeName(),Label.CENTER));
		//constructMainPanel();
		//add(mainP);
		add(new Line(false));
		Panel fpi = new Panel();
		fpi.setLayout(new BorderLayout());
		ClassificationStatisticsCanvas csc = new ClassificationStatisticsCanvas(qcc);
		fpi.add(csc, "Center");
		// following text: "Classification statistics"
		FoldablePanel fp = new FoldablePanel(fpi, new Label("Classification")); //res.getString("Classification")));
		fp.setBackground(new Color(223, 223, 223));
		add(fp);
		add(new Line(false));
		RangedDistPanel rdp = new RangedDistPanel(sup, qcc);
		fp = new FoldablePanel(rdp, new Label("Ranged distribution")); //new Label(res.getString("Ranged_dist")));
		add(fp);
		add(new Line(false));
		Component broadPan = null;
		try {
			Object obj = Class.forName("spade.analysis.classification.ClassBroadcastPanel").newInstance();
			if (obj != null && (obj instanceof ClassOperator) && (obj instanceof Component)) {
				ClassOperator cop = (ClassOperator) obj;
				if (cop.construct(qcc, sup)) {
					broadPan = (Component) obj;
				}
			}
		} catch (Exception e) {
		}
		if (broadPan != null) {
			add(broadPan);
		}
	}

}