package spade.analysis.classification;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Label;
import java.awt.Panel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.generators.PlotGeneratorsDescriptor;
import spade.analysis.manipulation.DichromaticSlider;
import spade.analysis.manipulation.Manipulator;
import spade.analysis.manipulation.Slider;
import spade.analysis.plot.DotPlot;
import spade.analysis.system.Supervisor;
import spade.analysis.transform.AttributeTransformer;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.FoldablePanel;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.PlotCanvas;
import spade.lib.color.Rainbow;
import spade.lib.lang.Language;
import spade.lib.util.Aligner;
import spade.lib.util.NumRange;
import spade.vis.action.ObjectEvent;
import spade.vis.action.ObjectEventHandler;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTreater;
import spade.vis.event.EventSource;
import spade.vis.spec.SaveableTool;
import spade.vis.spec.ToolSpec;

/**
* An assembly of UI elements that supports interactive classification of
* objects on the basis of a single numeric attribute by breaking its value
* range into intervals.
*/

public class NumAttr1ClassManipulator extends Panel implements Manipulator, PropertyChangeListener, Destroyable, DataTreater, SaveableTool, ObjectEventHandler, EventSource {
	static ResourceBundle res = Language.getTextResource("spade.analysis.classification.Res");
	/**
	* Identifier of this visualization method
	*/
	protected String methodId = null;
	/**
	* Used to generate unique identifiers of instances of NumAttr1ClassManipulator
	*/
	protected static int nInstances = 0;
	protected int instanceN = 0;
	/**
	* The classifier being manipulated by this interface component
	*/
	protected NumAttr1Classifier classifier = null;
	/**
	* The dot plot shows attribute value dispersion
	*/
	protected DotPlot dp = null;
	/**
	* The (current) column number of the attribute used for the classification.
	* May change in the course of animation.
	*/
	protected int colN = -1;
	/**
	* The panel contains statistical quality indicators, class statistics, and
	* a cumulative curve.
	*/
	protected NumericClassifierControlPanel nccp = null;

	protected Supervisor supervisor = null;
	/**
	* Indicates whether the manipulator should position the slider vertically
	*/
	public boolean sliderVertical = false;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;
	/**
	* As a SaveableTool, a plot may be registered somewhere and, hence, must
	* notify the component where it is registered about its destroying. This vector
	* contains the listeners to be notified about destroying of the visualizer.
	*/
	protected Vector destroyListeners = null;

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
		if (!(visualizer instanceof NumAttr1Classifier))
			return false;
		classifier = (NumAttr1Classifier) visualizer;
		instanceN = ++nInstances;
		supervisor = sup;
		if (supervisor != null) {
			supervisor.registerObjectEventSource(this);
			supervisor.registerDataDisplayer(this);
		}
		if (classifier.hasSubAttributes()) {
			classifier.addPropertyChangeListener(this);
		}
		AttributeTransformer aTrans = classifier.getAttributeTransformer();
		if (aTrans != null) {
			aTrans.addPropertyChangeListener(this);
		}

		colN = classifier.getAttrColumnN(0);
		dp = new DotPlot(!sliderVertical, false, true, supervisor, this);
		dp.setDataSource(dataTable);
		dp.setReactToTableDataChange(false);
		if (aTrans != null) {
			dp.setAttributeTransformer(aTrans, false);
		}
		dp.setFieldNumber(colN);
		dp.setIsZoomable(false);
		dp.setTextDrawing(false);
		String aid = classifier.getAttrId();
		NumRange nr = (aTrans != null) ? aTrans.getAttrValueRange(aid) : dataTable.getAttrValueRange(aid);
		dp.setMinMax(nr.minValue, nr.maxValue);
		if (dataTable.isAttributeTemporal(colN) && classifier.getMinTime() == null) {
			classifier.findMinMaxTime();
		}
		if (classifier.isAttrTemporal()) {
			dp.setAbsMinMaxTime(classifier.getMinTime(), classifier.getMaxTime());
		}
		dp.setup();
		dp.checkWhatSelected();
		PlotCanvas dpCanvas = new PlotCanvas();
		dp.setCanvas(dpCanvas);
		dpCanvas.setContent(dp);

		DichromaticSlider sl = new DichromaticSlider();
		Slider clSlider = sl.getClassificationSlider();
		clSlider.setMinMax(nr.minValue, nr.maxValue);
		if (classifier.isAttrTemporal()) {
			clSlider.setAbsMinMaxTime(classifier.getMinTime(), classifier.getMaxTime());
		}
		sl.setIsHorisontal(!sliderVertical);
		PlotCanvas slCanvas = new PlotCanvas();
		sl.setCanvas(slCanvas);
		slCanvas.setContent(sl);
		//classifier.setSlider(sl.getClassificationSlider());

		Aligner aligner = new Aligner();
		dp.setMayDefineAlignment(true);
		dp.setAligner(aligner);
		sl.setAligner(aligner);

		Panel lp = new Panel();
		lp.setLayout(new BorderLayout());
		lp.add(new Label(dataTable.getAttributeName(classifier.getAttrId()), Label.CENTER), BorderLayout.CENTER);
		Rainbow rainbow = new Rainbow();
		lp.add(rainbow, BorderLayout.WEST);

		nccp = new NumericClassifierControlPanel(dataTable, colN, clSlider, sl, sup, classifier);
		rainbow.setActionListener(nccp);

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

		if (sliderVertical) {
			Panel pp = new Panel(new BorderLayout());
			pp.add(dpCanvas, BorderLayout.WEST);
			pp.add(slCanvas, BorderLayout.EAST);
			setLayout(new BorderLayout());
			add(pp, BorderLayout.WEST);
			pp = new Panel(new ColumnLayout());
			pp.add(lp);
			pp.add(new Line(false));
			pp.add(nccp);
			RangedDistPanel rdp = new RangedDistPanel(sup, classifier);
			FoldablePanel fp = new FoldablePanel(rdp, new Label(res.getString("Ranged_dist")));
			pp.add(fp);
			if (chp != null) {
				pp.add(new Line(false));
				pp.add(chp);
			}
			add(pp, BorderLayout.CENTER);
		} else {
			setLayout(new BorderLayout());
			Panel p = new Panel();
			p.setLayout(new ColumnLayout());
			p.add(lp);
			p.add(new Line(false));
			p.add(dpCanvas);
			p.add(slCanvas);
			p.add(new Line(false));
			add(p, BorderLayout.NORTH);
			p = new Panel(new ColumnLayout());
			p.add(nccp);
			RangedDistPanel rdp = new RangedDistPanel(sup, classifier);
			FoldablePanel fp = new FoldablePanel(rdp, new Label(res.getString("Ranged_dist")));
			p.add(fp);
			add(p, BorderLayout.CENTER);
			if (chp != null) {
				p = new Panel(new ColumnLayout());
				p.add(new Line(false));
				p.add(chp);
				add(p, BorderLayout.SOUTH);
			}
		}
		return true;
	}

	/**
	* Returns the slider used for break specification
	*/
	public Slider getBreakSlider() {
		return nccp.getBreakSlider();
	}

	/**
	* A method from the ObjectEventHandler interface.
	* The manipulator receives object events from its dot plots and tranferres
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
		return "1_Num_Attr_Class_" + instanceN;
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
		if (destroyListeners != null) {
			PropertyChangeEvent pce = new PropertyChangeEvent(this, "destroyed", null, null);
			for (int i = 0; i < destroyListeners.size(); i++) {
				((PropertyChangeListener) destroyListeners.elementAt(i)).propertyChange(pce);
			}
		}
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
	* Replies whether it is linked to the data set (table) with the given identifier
	*/
	@Override
	public boolean isLinkedToDataSet(String setId) {
		return classifier.isLinkedToDataSet(setId);
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
	* Checks whether the attribute used for classification has changed. This
	* may happen when a time-dependent attribute is represented on an animated map.
	* In this case, the manipulator resets the dot plot.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(classifier) && e.getPropertyName().equals("classes")) {
			int cN = classifier.getAttrColumnN(0);
			if (cN == colN)
				return;
			colN = cN;
			dp.setFieldNumber(colN);
			dp.redraw();
			nccp.setColumnN(colN);
		} else if ((e.getSource() instanceof AttributeTransformer) && e.getPropertyName().equals("values")) {
			AttributeTransformer aTrans = classifier.getAttributeTransformer();
			String aid = classifier.getAttrId();
			NumRange nr = (aTrans != null) ? aTrans.getAttrValueRange(aid) : classifier.getTable().getAttrValueRange(aid);
			if (nr != null) {
				dp.setMinMax(nr.minValue, nr.maxValue);
				dp.setup();
				dp.checkWhatSelected();
				dp.redraw();
			}
		}
	}

//---------------- implementation of the SaveableTool interface ------------
	/**
	* Adds a listener to be notified about destroying of the visualize.
	* As a SaveableTool, a visualizer may be registered somewhere and, hence, must
	* notify the component where it is registered about its destroying.
	*/
	@Override
	public void addDestroyingListener(PropertyChangeListener lst) {
		if (lst == null)
			return;
		if (destroyListeners == null) {
			destroyListeners = new Vector(5, 5);
		}
		if (!destroyListeners.contains(lst)) {
			destroyListeners.addElement(lst);
		}
	}

	/**
	* Returns the keyword used in the opening tag of a stored state description
	* of this tool. A visualizer description (specification) is usually stored as
	* a sequence of lines starting with <map> and ending with </map>. Hence,
	* this method returns "map".
	*/
	@Override
	public String getTagName() {
		return "chart";
	}

	/**
	* Returns the specification of this visualizer (normally an instance of the
	* class spade.vis.spec.ToolSpec) for storing in a file.
	*/
	@Override
	public Object getSpecification() {
		ToolSpec spec = new ToolSpec();
		spec.tagName = getTagName();
		spec.methodId = getMethodId();
		if (classifier != null && classifier.getTable() != null) {
			spec.table = classifier.getTable().getContainerIdentifier();
		}
		spec.attributes = getAttributeList();
		spec.properties = getProperties();
		return spec;
	}

	/**
	* Returns custom properties of the tool: String -> String
	* By default, returns null.
	*/
	public Hashtable getProperties() {
		Hashtable prop = null;
		if (classifier != null) {
			prop = classifier.getVisProperties();
		}
		//possibly, add more properties
		return prop;
	}

	/**
	* After the plot is constructed, it may be requested to setup its individual
	* properties according to the given list of stored properties.
	* The base Plot class does nothing in this method.
	*/
	@Override
	public void setProperties(Hashtable properties) {
		if (properties != null && classifier != null) {
			classifier.setVisProperties(properties);
			nccp.adjustToClassifierProperties();
		}
	}

	/**
	* Through this function the component constructing the plot can set the
	* identifier of the visualization method.
	*/
	public void setMethodId(String id) {
		methodId = id;
	}

	/**
	* Returns the identifier of the visualization method implemented by this
	* class.
	*/
	public String getMethodId() {
		return methodId;
	}

	@Override
	public String getName() {
		String name = PlotGeneratorsDescriptor.getToolName(getMethodId()) + " " + getInstanceN();
		if (name == null) {
			name = getMethodId() + " " + getInstanceN();
		}
		return name;
	}
}
