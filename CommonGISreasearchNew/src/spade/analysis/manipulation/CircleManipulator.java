package spade.analysis.manipulation;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.plot.DotPlot;
import spade.analysis.system.Supervisor;
import spade.analysis.transform.AttributeTransformer;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.FocusListener;
import spade.lib.basicwin.Focuser;
import spade.lib.basicwin.FoldablePanel;
import spade.lib.basicwin.PlotCanvas;
import spade.lib.color.Rainbow;
import spade.lib.lang.Language;
import spade.lib.util.Aligner;
import spade.lib.util.StringUtil;
import spade.time.TimeMoment;
import spade.vis.action.ObjectEvent;
import spade.vis.action.ObjectEventHandler;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTreater;
import spade.vis.event.DEvent;
import spade.vis.event.EventReceiver;
import spade.vis.event.EventSource;
import spade.vis.mapvis.CirclePresenter;

/** CircleManipulator supports the manipulation of the single numeric attribute
* cartographic representation.
The component allows the user to set a reference value, and the map changes so as to
* represent the differences between values of the attribute and the reference
* value. The reference value may be dynamically changed.
*/
public class CircleManipulator extends Panel implements SliderListener, FocusListener, ActionListener, ItemListener, PropertyChangeListener, Manipulator, DataTreater, EventReceiver, EventSource, ObjectEventHandler, ObjectEventReactor, Destroyable {
	/**
	* Used to generate unique identifiers of instances of CirclePresenter
	*/
	static ResourceBundle res = Language.getTextResource("spade.analysis.manipulation.Res");
	protected static int nInstances = 0;
	protected int instanceN = 0;

	protected TextField cmpTF = null, minTF = null, maxTF = null;
	protected Checkbox dynRepaintCB = null;

	protected CirclePresenter vis = null;
	protected Slider slider = null;
	protected DotPlot dp = null, dpfixed = null;
	protected Focuser focuser = null;
	protected AttributeDataPortion data = null;
	protected Supervisor supervisor = null;
	/**
	* Used to switch the interpretation of mouse click to object comparison
	*/
	protected ClickManagePanel cmpan = null;
	/**
	* The identifier of the selected object for the visual comparison.
	*/
	protected String selObj = null;
	protected Label lRef = null;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;

	/**
	* Construction of map manipulator. Returns true if successfully constructed.
	* Arguments: 1) supervisor responsible for dynamic linking of displays;
	* 2) visualizer or classifier to be manipulated; 3) table with source data;
	* 4) table filter
	* The Manipulator must check if the visualizer has the appropriate type.
	* For CircleManipulator visualizer should be an instance of CirclePresenter
	*/
	@Override
	public boolean construct(Supervisor sup, Object visualizer, AttributeDataPortion dataTable) {
		if (visualizer == null || dataTable == null)
			return false;
		if (!(visualizer instanceof CirclePresenter))
			return false;
		this.vis = (CirclePresenter) visualizer;
		data = dataTable;
		instanceN = ++nInstances;
		supervisor = sup;
		if (sup != null) {
			sup.registerObjectEventSource(this);
		}
		vis.addVisChangeListener(this);
		AttributeTransformer aTrans = vis.getAttributeTransformer();
		if (aTrans != null) {
			aTrans.addPropertyChangeListener(this);
		}

		setLayout(new BorderLayout());
		Panel p = new Panel(new BorderLayout());
		Panel ppp = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		ppp.add(new Label("Reference="));
		ppp.add(lRef = new Label(""));
		p.add(ppp, BorderLayout.NORTH);
		// following text: "Compare to:"
		p.add(new Label(res.getString("Compare_to_")), BorderLayout.WEST);

		double fmin = vis.getFocuserMin(), fmax = vis.getFocuserMax();
		TimeMoment minTime = null, maxTime = null;
		if (vis.isAttrTemporal()) {
			minTime = vis.getMinTime();
			maxTime = vis.getMaxTime();
		}
		int tfLen = Math.max(vis.getFocuserMaxAsString().length(), vis.getFocuserMinAsString().length());
		cmpTF = new TextField(vis.getCmpAsString(), tfLen);
		p.add(cmpTF, BorderLayout.CENTER);
		ColumnLayout cl = new ColumnLayout();
		cl.setAlignment(ColumnLayout.Hor_Left);
		Panel pp = new Panel(cl);
		pp.add(new Label(vis.getAttrName((String) vis.getAttributes().elementAt(0))));
		pp.add(p);
		add(pp, "North");

		p = new Panel(new BorderLayout());
		maxTF = new TextField(vis.getFocuserMaxAsString(), tfLen);
		pp = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		// following text:"Max:"
		pp.add(new Label(res.getString("Max_")));
		pp.add(maxTF);
		p.add(pp, "North");
		minTF = new TextField(vis.getFocuserMinAsString(), tfLen);
		pp = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		// following text:"Min:"
		pp.add(new Label(res.getString("Min_")));
		pp.add(minTF);
		p.add(pp, "South");

		slider = new Slider();
		slider.setIsHorisontal(false);
		slider.setMinMax(fmin, fmax);
		if (minTime != null && maxTime != null) {
			slider.setAbsMinMaxTime(minTime, maxTime);
		}
		slider.setMaxNBreaks(1);
		slider.setMidPoint(vis.getCmp());
		slider.setNegativeHue(vis.negHue);
		slider.setPositiveHue(vis.posHue);
		slider.setUseShades(false);
		slider.addSliderListener(this);
		slider.setTextField(cmpTF);
		PlotCanvas canvas = new PlotCanvas();
		slider.setCanvas(canvas);
		canvas.setContent(slider);
		canvas.setInsets(4, 4, 0, 4);
		Panel p1 = new Panel(new BorderLayout());
		p1.add(canvas, "West");

		dp = new DotPlot(false, false, true, supervisor, this);
		dp.setIsZoomable(true);
		dp.setFocuserOnLeft(false);
		dp.setTextDrawing(false);
		dp.setFocuserDrawsTexts(false);
		dp.setDataSource(dataTable);
		dp.setReactToTableDataChange(false);
		dp.setFieldNumber(dataTable.getAttrIndex(vis.getAttrId(0)));
		dp.setMinMax(fmin, fmax);
		if (vis.hasSubAttributes()) {
			dp.setMayMoveDelimiters(false);
		}
		if (minTime != null && maxTime != null) {
			dp.setAbsMinMaxTime(minTime, maxTime);
		}
		if (aTrans != null) {
			dp.setAttributeTransformer(aTrans, false);
		}
		dp.setup();
		dp.checkWhatSelected();
		canvas = new PlotCanvas();
		dp.setCanvas(canvas);
		canvas.setContent(dp);
		canvas.setInsets(0, 4, 0, 4);
		p1.add(canvas, "East");

		focuser = dp.getFocuser();
		if (focuser != null) {
			focuser.setTextFields(minTF, maxTF);
//ID
			focuser.setMinMax(vis.getDataMin(), vis.getDataMax(), vis.getFocuserMin(), vis.getFocuserMax());
//~ID
			if (minTime != null && maxTime != null) {
				focuser.setAbsMinMaxTime(minTime, maxTime);
			}
			focuser.addFocusListener(this);
			focuser.setCanvas(canvas);
		}

		dpfixed = new DotPlot(false, false, true, supervisor, this);
		dpfixed.setIsHidden(true);
		dpfixed.setIsZoomable(false);
		dpfixed.setFocuserOnLeft(true);
		dpfixed.setTextDrawing(true);
		dpfixed.setFocuserDrawsTexts(false);
		dpfixed.setDataSource(dataTable);
		dpfixed.setReactToTableDataChange(false);
		dpfixed.setFieldNumber(dataTable.getAttrIndex(vis.getAttrId(0)));
		dpfixed.setMinMax(vis.getDataMin(), vis.getDataMax());
		if (minTime != null && maxTime != null) {
			dpfixed.setAbsMinMaxTime(minTime, maxTime);
		}
		if (aTrans != null) {
			dpfixed.setAttributeTransformer(aTrans, false);
		}
		dpfixed.setup();
		dpfixed.checkWhatSelected();
//ID
		dpfixed.setIsHidden(fmax == vis.getDataMax() && fmin == vis.getDataMin());
//~ID
		canvas = new PlotCanvas();
		dpfixed.setCanvas(canvas);
		canvas.setContent(dpfixed);
		canvas.setInsets(0, 4, 4, 4);
		pp = new Panel(new BorderLayout());
		pp.add(p1, "West");
		pp.add(canvas, "Center");
		p.add(pp);

		Aligner aligner = new Aligner();
		dp.setMayDefineAlignment(true);
		dp.setAligner(aligner);
		slider.setAligner(aligner);
		dpfixed.setAligner(aligner);

		add(p, "Center");

		pp = new Panel(new BorderLayout());
		Rainbow rb = new Rainbow();
		rb.setActionListener(this);
		pp.add(rb, "West");
		p = new Panel(new ColumnLayout());
		p.add(pp);
		// Option switcher
		CheckboxGroup cbgOptions = new CheckboxGroup();
//ID
		Checkbox cbOption1 = new Checkbox(null, vis.useMinSize, cbgOptions);
		Checkbox cbOption2 = new Checkbox(null, !vis.useMinSize, cbgOptions);
//~ID
		cbOption1.setName("use_min_diam");
		cbOption2.setName("use_max_diam");
		cbOption1.addItemListener(this);
		cbOption2.addItemListener(this);
		Panel pOption1 = new Panel(new FlowLayout(FlowLayout.LEFT));
		Panel pOption2 = new Panel(new FlowLayout(FlowLayout.LEFT));
		pOption1.add(cbOption1);
		pOption2.add(cbOption2);
		pp = new Panel(new BorderLayout());
		PieOption po1 = new PieOption(true);
		PieOption po2 = new PieOption(false);
		pOption1.add(po1);
		pOption2.add(po2);
		pp.add(pOption2, "North");
		pp.add(pOption1, "South");
		// following text:"Visualisation options"
		Label lDescrCaption = new Label(res.getString("Visualisation_options"), Label.LEFT);
		lDescrCaption.setBackground(Color.getHSBColor(0.0f, 0.0f, 0.8f));
		lDescrCaption.setSize(50, 15);
		FoldablePanel fpOption = new FoldablePanel(pp, lDescrCaption);
		p.add(fpOption);
		//~ P.G.
		// following text:"dynamic map update"
		dynRepaintCB = new Checkbox(res.getString("dynamic_map_update"));
		p.add(dynRepaintCB);
		cmpan = new ClickManagePanel(supervisor);
		cmpan.setComparer(this);
		p.add(cmpan);
		add(p, "South");
		return true;
	}

	public Focuser getFocuser() {
		return focuser;
	}

	/**
	 * Allows or prohibits to adjust the focuser position to the closest
	 * value
	 */
	public void setMayAdjustFocuserPosition(boolean allow) {
		dp.setMayMoveDelimiters(allow);
	}

	/**
	* Reaction on action events from the Rainbow: starting the dialog for
	* changing colors
	*/
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof Rainbow) {
			vis.startChangeColors();
			return;
		}
	}

	/**
	* Reaction to changes of the visualizer (e.g. colors may be changed through
	* the legend)
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(vis)) {
			if (vis.getPositiveHue() != slider.getPositiveHue() || vis.getNegativeHue() != slider.getNegativeHue()) {
				slider.setPositiveHue(vis.getPositiveHue());
				slider.setNegativeHue(vis.getNegativeHue());
				slider.redraw();
			} else if (!vis.getAttrId(0).equalsIgnoreCase(data.getAttributeId(dp.getFieldNumber()))) {
				int aidx = data.getAttrIndex(vis.getAttrId(0));
				dp.setFieldNumber(aidx);
				dp.setup();
				dpfixed.setFieldNumber(aidx);
				dpfixed.setup();
				dp.redraw();
				dpfixed.redraw();
				if (selObj != null) {
					int recN = data.indexOf(selObj);
					if (recN >= 0) {
						double val = data.getNumericAttrValue(aidx, recN);
						if (!Double.isNaN(val)) {
							vis.setCmp(val);
							slider.setMidPoint(val);
							slider.redraw();
							slider.notifyBreaksChange();
						}
					}
				}
			}
		} else if (e.getSource() instanceof AttributeTransformer) {
			if (e.getPropertyName().equals("values")) {
				double fmin = vis.getFocuserMin(), fmax = vis.getFocuserMax(), dmin = vis.getDataMin(), dmax = vis.getDataMax();
				dpfixed.setMinMax(dmin, dmax);
				dpfixed.setup();
				dpfixed.redraw();
				dp.setMinMax(dmin, dmax);
				dp.setup();
				focuser.setMinMax(dmin, dmax, fmin, fmax);
				focuser.refresh();
				dp.focusChanged(focuser, fmin, fmax);
				//dp.redraw();
				slider.setMinMaxCmp(fmin, fmax, vis.getCmp());
				cmpTF.setText(StringUtil.doubleToStr(vis.getCmp(), fmin, fmax));
			}
		}
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		Object objSrc = ie.getSource();
		if (objSrc instanceof Checkbox) {
			Checkbox cb = (Checkbox) objSrc;
			if (cb.getName().equalsIgnoreCase("use_min_diam")) {
				vis.setUseMinSize(true);
			}
			if (cb.getName().equalsIgnoreCase("use_max_diam")) {
				vis.setUseMinSize(false);
			}
			return;
		}

	}

	/*
	* This function is called during the process of moving a slider
	* <n> shows number of the slider (n==0 corresponds
	* to a delimiter between 0th and 1st classes)
	*/
	@Override
	public void breakIsMoving(Object source, int n, double currValue) {
		if (source == slider && dynRepaintCB.getState()) {
			vis.setCmp(slider.getMidPoint());
			selObj = null;
			lRef.setText("");
		}
	}

	/*
	* Results of visual comparison. nBreaks<=1
	*/
	@Override
	public void breaksChanged(Object source, double[] breaks, int nBreaks) {
		if (source == slider && slider.getMidPoint() != vis.getCmp()) {
			vis.setCmp(slider.getMidPoint());
			selObj = null;
			lRef.setText("");
		}
	}

	/**
	 * Change of colors
	 */
	@Override
	public void colorsChanged(Object source) {
	}

	/**
	 * Removes itself from receivers of object events
	 */
	@Override
	public void destroy() {
		if (cmpan != null) {
			cmpan.destroy();
		}
		if (supervisor != null) {
			supervisor.removeObjectEventSource(this);
		}
		destroyed = true;
	}

	/**
	 * The EventReceiver answers whether it is interested in getting the specified
	 * kind of events. A VisComparison device is interested in receiving
	 * object clicks events. It uses them for setting reference values for
	 * visual comparison.
	 */
	@Override
	public boolean doesListenToEvent(String eventId) {
		return eventId.equals(ObjectEvent.click);
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

	/*
	*  this method manipulates with focuser
	*/
	@Override
	public void focusChanged(Object source, double lowerLimit, double upperLimit) {
		if (focuser != null) {
			dpfixed.setIsHidden(!focuser.isRestricted());
		}
		vis.setFocuserMinMax(lowerLimit, upperLimit);
		slider.setMinMax(lowerLimit, upperLimit);
	}

	/**
	 * A method from the DataTreater interface.
	 * Returns a vector of IDs of the attributes this Data Treater deals with.
	 * Takes the list of the attributes from its visualizer.
	 */
	@Override
	public Vector getAttributeList() {
		return vis.getAttributeList();
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
	 * Returns a unique identifier of the component
	 * (used only internally, not shown to the user).
	 * The identifier is used for explicit linking of producers and recipients of
	 * object events.
	 */
	@Override
	public String getIdentifier() {
		return "Circle" + instanceN;
	}

	/**
	  * Replies whether is destroyed or not
	  */
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	/**
	* A method from the DataTreater interface.
	* Replies whether it is linked to the data set (table) with the given identifier
	*/
	@Override
	public boolean isLinkedToDataSet(String setId) {
		return vis.isLinkedToDataSet(setId);
	}

	@Override
	public void limitIsMoving(Object source, int n, double currValue) {
		dpfixed.setIsHidden(false);
		if (dynRepaintCB.getState()) {
			if (n == 0) {
				vis.setFocuserMinMax(currValue, vis.getFocuserMax());
			} else {
				vis.setFocuserMinMax(vis.getFocuserMin(), currValue);
			}
		}
	}

	/**
	* A method from the ObjectEventHandler interface.
	* The VisualComparison receives object events from its dotplot and tranferres
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

	//------------- implementation of the ObjectEventReactor interface ------------
	/**
	* An ObjectEventReactor may process object events either from all displays
	* or from the component (e.g. map) it is attached to. This component is a
	* primary event source for the ObjectEventReactor. A reference to the
	* primary event source is set using this method.
	*/
	@Override
	public void setPrimaryEventSource(Object evtSource) {
		if (cmpan != null) {
			cmpan.setPrimaryEventSource(evtSource);
		}
	}

	/**
	* This method is used for delivering events to the Event Receiver.
	*/
	@Override
	public void eventOccurred(DEvent evt) {
		if (vis == null || data == null)
			return;
		int attrN = data.getAttrIndex(vis.getAttrId(0));
		if (attrN < 0)
			return;
		if (evt instanceof ObjectEvent) {
			selObj = null;
			lRef.setText("");
			ObjectEvent oe = (ObjectEvent) evt;
			if (StringUtil.sameStrings(oe.getSetIdentifier(), data.getEntitySetIdentifier()) && oe.getType().equals(ObjectEvent.click)) {
				boolean changed = false;
				for (int i = 0; i < oe.getAffectedObjectCount() && !changed; i++) {
					String objId = oe.getObjectIdentifier(i);
					int recN = data.indexOf(objId);
					if (recN >= 0) {
						double val = data.getNumericAttrValue(attrN, recN);
						if (!Double.isNaN(val)) {
							vis.setCmp(val);
							slider.setMidPoint(val);
							changed = true;
							selObj = objId;
							lRef.setText(data.getDataItemName(data.indexOf(objId)));
							CManager.invalidateAll(lRef);
							CManager.validateAll(lRef);
						}
					}
				}
				if (!changed) {
					slider.setMidPoint(0.0f);
				}
				slider.redraw();
				slider.notifyBreaksChange();
			}
		}
	}

}
