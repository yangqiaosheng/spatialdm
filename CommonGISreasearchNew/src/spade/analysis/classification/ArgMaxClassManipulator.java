package spade.analysis.classification;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Component;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.manipulation.Manipulator;
import spade.analysis.system.Supervisor;
import spade.analysis.transform.AttributeTransformer;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.FoldablePanel;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.Slider;
import spade.lib.lang.Language;
import spade.lib.util.NumRange;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeDataPortion;

public class ArgMaxClassManipulator extends Panel implements ItemListener, PropertyChangeListener, Manipulator, Destroyable {
	static ResourceBundle res = Language.getTextResource("spade.analysis.classification.Res");
	/**
	* Used to generate unique identifiers of instances of ArgMaxClassManipulator
	*/
	protected static int nInstances = 0;
	protected int instanceN = 0;
	protected ArgMaxClassifier classifier = null;
	protected Supervisor supervisor = null;
	protected AttributeDataPortion dataTable = null;
	protected Checkbox /*cbThreshold=null,
						cbValue=null,
						cbProportion=null,*/
	cbDynUpdate = null, cbMissingValues = null;
	protected Slider slValue = null, slProportion = null;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;

	protected float min = Float.NaN, max = Float.NaN;

	protected void countMinMax() {
		//System.out.println("*** count Min Max start");
		for (int i = 0; i < classifier.getAttributes().size(); i++) {
			NumRange nr = classifier.getAttrValueRange(i);
			if (nr == null) {
				continue;
			}
			if (Float.isNaN(min) || min > nr.minValue) {
				min = (float) nr.minValue;
			}
			if (Float.isNaN(max) || max < nr.maxValue) {
				max = (float) nr.maxValue;
			}
		}
		//System.out.println("*** count Min Max finish");
	}

	@Override
	public boolean construct(Supervisor sup, Object visualizer, AttributeDataPortion dataTable) {
		//System.out.println("*** manipulator construct start");
		if (visualizer == null || dataTable == null)
			return false;
		if (!(visualizer instanceof ArgMaxClassifier))
			return false;
		this.supervisor = sup;
		this.dataTable = dataTable;
		supervisor.addPropertyChangeListener(this);
		classifier = (ArgMaxClassifier) visualizer;
		AttributeTransformer aTrans = classifier.getAttributeTransformer();
		if (aTrans != null) {
			aTrans.addPropertyChangeListener(this);
		} else {
			dataTable.addPropertyChangeListener(this);
		}
		/*
		if (classifier.hasSubAttributes())
		  classifier.addPropertyChangeListener(this);
		*/
		instanceN = ++nInstances;
		countMinMax();
		setLayout(new ColumnLayout());
		add(new Label(res.getString("Presence_threshold")));
		Panel p = new Panel();
		p.setLayout(new BorderLayout());
		add(p);
//ID
		TextField tf = new TextField(StringUtil.floatToStr(classifier.getPresenceThreshold(), min, max), 5);
//~ID
		p.add(tf, BorderLayout.WEST);
//ID
		slValue = new Slider(classifier, min, max, classifier.getPresenceThreshold());
//~ID
		p.add(slValue, BorderLayout.CENTER);
		slValue.setTextField(tf);
//ID
		cbMissingValues = new Checkbox(res.getString("Missing_value") + " = 0", classifier.getMissingIsZero());
//~ID
		cbMissingValues.addItemListener(classifier);
		add(cbMissingValues);
		add(new Line(false));
		add(new Label(res.getString("Mix_threshold")));
		p = new Panel();
		p.setLayout(new BorderLayout());
		add(p);
//ID
		tf = new TextField(String.valueOf(classifier.getMixThreshold()), 3);
//~ID
		p.add(tf, BorderLayout.WEST);
//ID
		slProportion = new Slider(classifier, 0, 100, classifier.getMixThreshold());
//~ID
		p.add(slProportion, BorderLayout.CENTER);
		slProportion.setTextField(tf);
		add(new Line(false));
		cbDynUpdate = new Checkbox(res.getString("Dynamic_update"), false);
		cbDynUpdate.addItemListener(this);
		add(cbDynUpdate);
		RangedDistPanel rdp = new RangedDistPanel(sup, classifier);
		FoldablePanel fp = new FoldablePanel(rdp, new Label(res.getString("Ranged_dist")));
		add(fp);
		add(new Line(false));
		Component chp = null; //class handling panel
		Object obj = null;
		try {
			obj = Class.forName("spade.analysis.classification.ClassHandlingPanel").newInstance();
		} catch (Exception e) {
		}
		if (obj == null) {
			try {
				obj = Class.forName("spade.analysis.classification.ClassBroadcastPanel").newInstance();
			} catch (Exception e) {
			}
		}
		if (obj != null && (obj instanceof ClassOperator) && (obj instanceof Component)) {
			ClassOperator cop = (ClassOperator) obj;
			if (cop.construct(classifier, sup)) {
				chp = (Component) obj;
			}
		}
		add(chp);
		classifier.setControls(slValue, slProportion, cbMissingValues);
		classifier.setAttrColorHandler(supervisor.getAttrColorHandler());
		//System.out.println("*** manipulator construct finish");
		return true;
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (ie.getSource().equals(cbDynUpdate)) {
			slValue.setNAD(cbDynUpdate.getState());
			slProportion.setNAD(cbDynUpdate.getState());
		}
	}

	/**
	* Checks whether the attribute used for classification has changed. This
	* may happen when a time-dependent attribute is represented on an animated map.
	* In this case, the manipulator resets its controls.
	*
	* Later we should react to data changes (e.g. results of calculations)
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(dataTable) && (e.getPropertyName().equals("data_added") || e.getPropertyName().equals("data_removed") || e.getPropertyName().equals("data_updated") || e.getPropertyName().equals("values"))) {
			if (e.getPropertyName().equals("values")) {
				boolean changed = false;
				Vector v = (Vector) e.getNewValue();
				if (v != null) {
					for (int i = 0; i < v.size() && !changed; i++) {
						changed = (classifier.getAttrIndex((String) v.elementAt(i)) >= 0);
					}
				}
				if (!changed)
					return;
			}
			accountForDataChange();
		} else if (e.getSource() == supervisor) {
			if (e.getPropertyName().equals(Supervisor.eventAttrColors)) {
				classifier.notifyColorsChange();
				// really we need only to repaint the map, but I did not find a better way ...
			}
		} else if ((e.getSource() instanceof AttributeTransformer) && e.getPropertyName().equals("values")) {
			accountForDataChange();
		}
	}

	/**
	* Called when data are changed in the table or in the attribute transformer
	*/
	protected void accountForDataChange() {
		float prevMin = min, prevMax = max;
		min = max = Float.NaN;
		countMinMax();
		if (min == prevMin && max == prevMax)
			return;
		//System.out.println("* min="+min+", max="+max);
		if (min != prevMin) {
			slValue.setAbsMin(min);
			if (slValue.getValue() == prevMin) {
				slValue.setValue(min);
			}
		}
		if (max != prevMax) {
			slValue.setAbsMax(max);
		}
	}

	/**
	* Makes necessary operations for destroying, e.g. unregisters from
	* listening highlighting and other events.
	*/
	@Override
	public void destroy() {
		if (dataTable != null) {
			dataTable.removePropertyChangeListener(this);
			//supervisor.removeObjectEventSource(this);
			//supervisor.removeDataDisplayer(this);
		}
		if (supervisor != null) {
			supervisor.removePropertyChangeListener(this);
		}
		classifier.destroy();
		destroyed = true;
	}

	/**
	* Replies whether is destroyed or not
	*/
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}
}