package spade.analysis.classification;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
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
import spade.analysis.manipulation.Slider;
import spade.analysis.plot.ScatterPlotWithSliders;
import spade.analysis.system.Supervisor;
import spade.analysis.transform.AttributeTransformer;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.FoldablePanel;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.PlotCanvas;
import spade.lib.lang.Language;
import spade.lib.util.DoubleArray;
import spade.lib.util.NumRange;
import spade.vis.action.ObjectEvent;
import spade.vis.action.ObjectEventHandler;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTreater;
import spade.vis.event.EventSource;

/**
* An assembly of UI elements that supports interactive classification of
* objects on the basis of a single numeric attribute by breaking its value
* range into intervals.
*/

public class NumAttr2ClassManipulator extends Panel implements ItemListener, PropertyChangeListener, Manipulator, Destroyable, DataTreater, ObjectEventHandler, EventSource {
	static ResourceBundle res = Language.getTextResource("spade.analysis.classification.Res");
	/**
	* Used to generate unique identifiers of instances of NumAttr1ClassManipulator
	*/
	protected static int nInstances = 0;
	protected int instanceN = 0;

	/**
	* The classifier being manipulated by this interface component
	*/
	protected NumAttr2Classifier classifier = null;
	/**
	* The (current) column number of the "horizontal" attribute used for the
	* classification. May change in the course of animation.
	*/
	protected int colNHor = -1;
	/**
	* The (current) column number of the "vertical" attribute used for the
	* classification. May change in the course of animation.
	*/
	protected int colNVert = -1;

	protected Supervisor supervisor = null;

	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;

	protected ScatterPlotWithSliders sp = null;
	protected Choice chColor = null;
	protected Checkbox cbInvert = null, cbDynUpdate = null, cbXY = null, cbX = null, cbY = null, cbEqSize = null, cbEqInt = null, cbColorHS = null, cbColorMix = null;

	public boolean isXYselected() {
		return cbXY.getState();
	}

	public boolean isXselected() {
		return cbX.getState();
	}

	public boolean isYselected() {
		return cbY.getState();
	}

	public boolean isEqSizeSelected() {
		return cbEqSize.getState();
	}

	public boolean isEqIntSelected() {
		return cbEqInt.getState();
	}

	protected Label lMinV = null, lMaxV = null, lMinH = null, lMaxH = null;
	protected TextField tfV = null, tfH = null;

	public boolean isTfV(Object obj) {
		return tfV != null && tfV == obj;
	}

	public boolean isTfH(Object obj) {
		return tfH != null && tfH == obj;
	}

	public String getTfVtext() {
		return tfV.getText().trim();
	}

	public String getTfHtext() {
		return tfH.getText().trim();
	}

	/**
	* Construction of map manipulator. Returns true if successfully constructed.
	* Arguments: 1) supervisor responsible for dynamic linking of displays;
	* 2) visualizer or classifier to be manipulated; 3) table with source data;
	* 4) table filter
	* The Manipulator must check if the visualizer has the appropriate type.
	* For NumAttr1ClassManipulator visualizer should be an instance of NumAttr1Classifier.
	*/
	@Override
	public boolean construct(Supervisor sup, Object visualizer, AttributeDataPortion dataTable) {
		if (visualizer == null || dataTable == null)
			return false;
		if (!(visualizer instanceof NumAttr2Classifier))
			return false;
		classifier = (NumAttr2Classifier) visualizer;
		classifier.addPropertyChangeListener(this);
		colNHor = classifier.getColNHor();
		colNVert = classifier.getColNVert();
		instanceN = ++nInstances;
		supervisor = sup;
		if (supervisor != null) {
			supervisor.registerObjectEventSource(this);
			supervisor.registerDataDisplayer(this);
		}
		AttributeTransformer aTrans = classifier.getAttributeTransformer();
		if (aTrans != null) {
			aTrans.addPropertyChangeListener(this);
		}

		lMinV = new Label("");
		lMaxV = new Label("");
		lMinH = new Label("");
		lMaxH = new Label("");
		tfV = new TextField("");
		tfH = new TextField("");
		tfV.addActionListener(classifier);
		tfH.addActionListener(classifier);

		sp = new ScatterPlotWithSliders(true, true, sup, sup);
		sp.setDataSource(dataTable);
		sp.setReactToTableDataChange(false);
		sp.setFieldNumbers(colNHor, colNVert);
		sp.setIsZoomable(false);
		if (aTrans != null) {
			sp.setAttributeTransformer(aTrans, false);
		}
		sp.setup();
		NumRange nr1 = classifier.getHorValueRange(), nr2 = classifier.getVertValueRange();
		sp.setMinMax(nr1.minValue, nr1.maxValue, nr2.minValue, nr2.maxValue);
		sp.checkWhatSelected();
		PlotCanvas canvas = new PlotCanvas();
		sp.setCanvas(canvas);
		canvas.setContent(sp);
		sp.constructSliders();
		sp.setClickListener(classifier);
		Slider sliderh = sp.getHSlider(), sliderv = sp.getVSlider();
		classifier.setSliders(sliderh, sliderv, sp);
		classifier.setNumAtrr2ClassManipulator(this);

		setLayout(new ColumnLayout());
		add(new Label("Y: " + classifier.getAttributeName(1), Label.LEFT));
		Panel p = new Panel();
		p.setLayout(new BorderLayout());
		p.add(lMinV, "West");
		p.add(tfV, "Center");
		p.add(lMaxV, "East");
		add(p);
		p = new Panel(new BorderLayout());
		p.add(canvas, "Center");
		add(p);
		p = new Panel();
		p.setLayout(new BorderLayout());
		p.add(lMinH, "West");
		p.add(tfH, "Center");
		p.add(lMaxH, "East");
		add(p);

		add(new Label("X: " + classifier.getAttributeName(0), Label.RIGHT));

		add(new Line(false));
		//following Text: "Dynamic update"
		cbDynUpdate = new Checkbox(res.getString("Dynamic_update"), true);
		cbDynUpdate.addItemListener(this);
		add(cbDynUpdate);

		add(new Line(false));
		Panel fpContents = new Panel();
		fpContents.setLayout(new ColumnLayout());
		p = new Panel();
		p.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		//following string: "split"
		p.add(new Label(res.getString("split")));
		CheckboxGroup cbg = new CheckboxGroup();
		p.add(cbXY = new Checkbox("X,Y", true, cbg));
		p.add(cbX = new Checkbox("X", false, cbg));
		p.add(cbY = new Checkbox("Y", false, cbg));
		fpContents.add(p);
		p = new Panel();
		p.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		//following String: "into"
		p.add(new Label(res.getString("into")));
		for (int i = 2; i < 6; i++) {
			String str = String.valueOf(i);
			Button b = new Button(str);
			b.addActionListener(classifier);
			b.setActionCommand(str);
			p.add(b);
		}
		//following text: " intervals"
		p.add(new Label(res.getString("intervals")));
		fpContents.add(p);

		p = new Panel();
		p.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		//following text:"of equal"
		p.add(new Label(res.getString("of_equal")));
		fpContents.add(p);
		p = new Panel();
		p.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		cbg = new CheckboxGroup();
		//following text:"group sizes", "lengths"
		p.add(cbEqSize = new Checkbox(res.getString("group_sizes"), true, cbg));
		p.add(cbEqInt = new Checkbox(res.getString("lengths"), false, cbg));
		fpContents.add(p);

		// following text: "Automatic classification"
		FoldablePanel fp = new FoldablePanel(fpContents, new Label(res.getString("Automatic")));
		fpContents.setBackground(new Color(223, 223, 223));
		add(fp);

		add(new Line(false));
		Panel fpi = new Panel();
		fpi.setLayout(new BorderLayout());
		ClassificationStatisticsCanvas csc = new ClassificationStatisticsCanvas(classifier);
		fpi.add(csc, "Center");
		// following text: "Classification statistics"
		fp = new FoldablePanel(fpi, new Label(res.getString("Classification")));
		fp.setBackground(Color.white);
		add(fp);

		add(new Line(false));

		fpContents = new Panel();
		fpContents.setLayout(new ColumnLayout());
		p = new Panel();
		p.setLayout(new FlowLayout());
		cbg = new CheckboxGroup();
		cbColorHS = new Checkbox("Hue+Saturation", cbg, classifier.getColorScaleNumber() == -1);
		cbColorHS.addItemListener(this);
		p.add(cbColorHS);
		cbColorMix = new Checkbox("Mix", cbg, classifier.getColorScaleNumber() != -1);
		cbColorMix.addItemListener(this);
		p.add(cbColorMix);
		fpContents.add(p);
		// following text:"Invert"
		cbInvert = new Checkbox(res.getString("Invert"), classifier.getColorScaleInverted());
		cbInvert.addItemListener(this);
		fpContents.add(cbInvert);
		chColor = new Choice();
		// following text:"Green & Red"
		chColor.add(res.getString("Green_Red"));
		// following text:"Red & Blue"
		chColor.add(res.getString("Red_Blue"));
		// following text:"Yellow & Red"
		chColor.add(res.getString("Yellow_Red"));
		// following text:"Yellow & Blue"
		chColor.add(res.getString("Yellow_Blue"));
		// following text:"Green & Blue"
		chColor.add(res.getString("Green_Blue"));
		// following text:"Yellow & Magenta"
		chColor.add(res.getString("Yellow_Magenta"));
		// following text:"Cyan & Blue"
		chColor.add(res.getString("Cyan_Blue"));
//ID
		if (classifier.getColorScaleNumber() == -1) {
			chColor.setEnabled(false);
//      cbInvert.setEnabled(false);
		} else {
			chColor.select(classifier.getColorScaleNumber() - 2);
		}
//~ID
		chColor.addItemListener(this);
		fpContents.add(chColor);
		fp = new FoldablePanel(fpContents, new Label(res.getString("Color_scale")));
		fpContents.setBackground(new Color(223, 223, 223));
		add(fp);
		add(new Line(false));
		RangedDistPanel rdp = new RangedDistPanel(sup, classifier);
		fp = new FoldablePanel(rdp, new Label(res.getString("Ranged_dist")));
		add(fp);

		//
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
		if (chp != null) {
			add(new Line(false));
			add(chp);
		}

		return true;
	}

	public void setTextFieldAndLabels(float minv, float maxv, float minh, float maxh) {
		// following text:"Splits: "
		lMinV.setText(res.getString("Splits_") + sp.numToStringVert(minv));
		String str = "";
		DoubleArray fabr = classifier.getBreaksV();
		if (fabr != null) {
			for (int i = 0; i < fabr.size(); i++) {
				str += ((i == 0) ? "" : " ") + sp.numToStringVert(fabr.elementAt(i));
			}
		}
		tfV.setText(str);
		lMaxV.setText(sp.numToStringVert(maxv));
		// following text:"Splits: "
		lMinH.setText(res.getString("Splits_") + sp.numToStringHor(minh));
		str = "";
		fabr = classifier.getBreaksH();
		if (fabr != null) {
			for (int i = 0; i < fabr.size(); i++) {
				str += ((i == 0) ? "" : " ") + sp.numToStringHor(fabr.elementAt(i));
			}
		}
		tfH.setText(str);
		lMaxH.setText(sp.numToStringHor(maxh));
		if (isShowing()) {
			CManager.validateAll(lMinV);
		}
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (ie.getSource() == chColor) {
			classifier.setColorScale(2 + chColor.getSelectedIndex(), cbInvert.getState());
		}
		if (ie.getSource() == cbColorMix || ie.getSource() == cbColorHS || ie.getSource() == cbInvert)
			if (cbColorHS.getState()) {
				classifier.setColorScale(-1, cbInvert.getState());
				chColor.setEnabled(false);
			} else {
				classifier.setColorScale(2 + chColor.getSelectedIndex(), cbInvert.getState());
				chColor.setEnabled(true);
			}
		if (ie.getSource() == cbDynUpdate) {
			classifier.setDynamicUpdate(cbDynUpdate.getState());
		}
		return;
	}

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(classifier)) {
			if (e.getPropertyName().equals("colors")) {
				if (chColor != null && classifier.getColorScaleNumber() >= 2) {
					chColor.select(classifier.getColorScaleNumber() - 2);
				}
				if (cbInvert != null) {
					cbInvert.setState(classifier.getColorScaleInverted());
				}
				if (chColor != null) {
					chColor.setEnabled(classifier.getColorScaleNumber() != -1);
				}
				if (cbColorHS != null) {
					cbColorHS.setState(classifier.getColorScaleNumber() == -1);
				}
				if (cbColorMix != null) {
					cbColorMix.setState(classifier.getColorScaleNumber() != -1);
				}
			} else if (e.getPropertyName().equals("classes")) {
				int cH = classifier.getColNHor(), cV = classifier.getColNVert();
				if (cH == colNHor && cV == colNVert)
					return;
				colNHor = cH;
				colNVert = cV;
				sp.setFieldNumbers(colNHor, colNVert);
				sp.redraw();
			}
		} else if ((e.getSource() instanceof AttributeTransformer) && e.getPropertyName().equals("values")) {
			NumRange nr1 = classifier.getHorValueRange(), nr2 = classifier.getVertValueRange();
			if (nr1 == null || nr2 == null)
				return;
			sp.setup();
			sp.setMinMax(nr1.minValue, nr1.maxValue, nr2.minValue, nr2.maxValue);
			sp.checkWhatSelected();
			DoubleArray fabr = classifier.getBreaksH();
			if (fabr != null) {
				for (int i = fabr.size() - 1; i >= 0; i--) {
					double br = fabr.elementAt(i);
					if (br <= nr1.minValue || br >= nr1.maxValue) {
						fabr.removeElementAt(i);
					}
				}
			} else {
				fabr = new DoubleArray(10, 1);
			}
			if (fabr.size() < 1) {
				fabr.addElement((nr1.minValue + nr1.maxValue) / 2);
			}
			classifier.setBreaksH(fabr, false);
			Slider sl = sp.getHSlider();
			sl.removeAllBreaks();
			for (int i = 0; i < fabr.size(); i++) {
				sl.addBreak(fabr.elementAt(i));
			}
			fabr = classifier.getBreaksV();
			if (fabr != null) {
				for (int i = fabr.size() - 1; i >= 0; i--) {
					double br = fabr.elementAt(i);
					if (br <= nr2.minValue || br >= nr2.maxValue) {
						fabr.removeElementAt(i);
					}
				}
			} else {
				fabr = new DoubleArray(10, 1);
			}
			if (fabr.size() < 1) {
				fabr.addElement((nr2.minValue + nr2.maxValue) / 2);
			}
			classifier.setBreaksV(fabr, true);
			sl = sp.getVSlider();
			sl.removeAllBreaks();
			for (int i = 0; i < fabr.size(); i++) {
				sl.addBreak(fabr.elementAt(i));
			}
			sp.redraw();
			setTextFieldAndLabels((float) nr2.minValue, (float) nr2.maxValue, (float) nr1.minValue, (float) nr1.maxValue);
		}
	}

	/**
	* A method from the ObjectEventHandler interface.
	* The DynamicQuery receives object events from its dot plots and tranferres
	* them to the supervisor.
	*/
	@Override
	public void processObjectEvent(ObjectEvent oevt) {
		if (supervisor != null) {
			ObjectEvent e = new ObjectEvent(this, oevt.getType(), oevt.getSourceMouseEvent(), oevt.getSetIdentifier(), oevt.getAffectedObjects());
			e.dataT = oevt.getDataTreater();
			supervisor.processObjectEvent(e);
		}
	}

//---------------- implementation of the EventSource interface ------------
	/**
	* The EventSource answers whether it can produce the specified
	* type of events.
	*/
	@Override
	public boolean doesProduceEvent(String eventId) {
		return eventId.equals(ObjectEvent.click) || eventId.equals(ObjectEvent.point) || eventId.equals(ObjectEvent.frame);
	}

	/**
	* Returns a unique identifier of the event source (may be produced
	* automatically, used only internally, not shown to the user).
	* The identifier is used for explicit linking of producers and recipients of
	* object events.
	*/
	@Override
	public String getIdentifier() {
		return "2_Num_Attr_Class_" + instanceN;
	}

	/**
	* Makes necessary operations for destroying, e.g. unregisters from
	* listening highlighting and other events.
	*/
	@Override
	public void destroy() {
		if (supervisor != null) {
			supervisor.removeObjectEventSource(this);
			supervisor.removeDataDisplayer(this);
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

	public int getInstanceN() {
		return instanceN;
	}

	/**
	* A method from the DataTreater interface.
	* Returns a vector of IDs of the attributes this Data Treater deals with.
	* A class manipulator takes the attribute list from its classifier.
	*/
	@Override
	public Vector getAttributeList() {
		return classifier.getAttributeList();
	}

	/**
	* A method from the DataTreater interface.
	* Must return a vector of colors used for representation of the attributes
	* this Data Treater deals with. By default, returns null.
	*/
	@Override
	public Vector getAttributeColors() {
		return null;
	}

	/**
	* A method from the DataTreater interface.
	* Replies whether it is linked to the data set (table) with the given identifier
	*/
	@Override
	public boolean isLinkedToDataSet(String setId) {
		return classifier.isLinkedToDataSet(setId);
	}
}
