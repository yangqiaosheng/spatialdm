package spade.analysis.manipulation;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.analysis.transform.AttributeTransformer;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.PlotCanvas;
import spade.lib.color.ColorCanvas;
import spade.lib.color.ColorDlg;
import spade.lib.color.ColorListener;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;
import spade.vis.action.ObjectEvent;
import spade.vis.database.AttributeDataPortion;
import spade.vis.event.DEvent;
import spade.vis.event.EventReceiver;
import spade.vis.mapvis.AttrColorHandler;
import spade.vis.mapvis.MultiNumberDrawer;

/**
* A MultiVisComparison is a UI component supporting visual comparison of values
* of several comparable numeric attributes represented by parallel bars to a
* user-specified reference value. Thereby the signs of the map are changed to
* represent differenced between values of the attributes and the reference
* value.
* A MultiVisComparison can react to object selection events in a case when
* only one attribute is represented on the map. Then the value of the
* attribute associated with the selected object will be used as the reference
* value for the comparison.
*/

public class MultiVisComparison extends Panel implements Manipulator, PropertyChangeListener, ActionListener, SliderListener, ColorListener, EventReceiver, ObjectEventReactor, Destroyable {
	static ResourceBundle res = Language.getTextResource("spade.analysis.manipulation.Res");
	/**
	* Used to generate unique identifiers of instances of MultiVisComparison
	*/
	protected static int nInstances = 0;
	protected int instanceN = 0;

	protected Checkbox dynRepaintCB = null;
	protected MultiNumberDrawer vis = null;
	protected boolean singleAttr = false;
	protected Slider slider = null;
	protected ColorCanvas ccs[] = null;
	protected AttributeDataPortion dataTable = null;
	protected ColorDlg cDlg = null;
	protected Supervisor supervisor = null;
	protected TextField cmpTF = null;
	/**
	* Used to switch the interpretation of mouse click to object comparison
	*/
	protected ClickManagePanel cmpan = null;
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
	* For MultiVisComparison visualizer should be an instance of MultiNumberDrawer.
	*/
	@Override
	public boolean construct(Supervisor sup, Object visualizer, AttributeDataPortion dataTable) {
		if (visualizer == null || dataTable == null)
			return false;
		if (!(visualizer instanceof MultiNumberDrawer))
			return false;
		this.vis = (MultiNumberDrawer) visualizer;
		this.dataTable = dataTable;
		Vector attr = vis.getAttributes();
		if (attr == null || attr.size() < 1)
			return false;
		singleAttr = attr.size() == 1;
		instanceN = ++nInstances;
		supervisor = sup;
		supervisor.addPropertyChangeListener(this);
		AttributeTransformer aTrans = vis.getAttributeTransformer();
		if (aTrans != null) {
			aTrans.addPropertyChangeListener(this);
		}

		setLayout(new BorderLayout());
		Panel p = new Panel(new BorderLayout());
		// following text: "Compare to:"
		p.add(new Label(res.getString("Compare_to_")), BorderLayout.WEST);
		double dmin = vis.getDataMin(), dmax = vis.getDataMax();
		cmpTF = new TextField("", 6);
		if (!Double.isNaN(vis.getCmp())) {
			cmpTF.setText(StringUtil.doubleToStr(vis.getCmp(), dmin, dmax));
		}
		p.add(cmpTF, BorderLayout.CENTER);
		ColumnLayout cl = new ColumnLayout();
		cl.setAlignment(ColumnLayout.Hor_Left);
		Panel pp = new Panel(cl);
		for (int i = 0; i < vis.getAttributes().size(); i++) {
			Panel ppp = new Panel(new BorderLayout());
			pp.add(ppp);
			ppp.add(new Label(vis.getAttrName(i)), "Center");
			if (vis instanceof AttrColorHandler) {
				if (ccs == null) {
					ccs = new ColorCanvas[vis.getAttributes().size()];
				}
				ccs[i] = new ColorCanvas();
				ccs[i].setColor(vis.getColorForAttribute(i));
				ccs[i].setActionListener(this);
				ppp.add(ccs[i], "West");
			}
		}
		pp.add(new Line(false));
		pp.add(p);
		add(pp, "North");

		p = new Panel(new BorderLayout());
		TextField maxTF = new TextField(StringUtil.doubleToStr(dmax, dmin, dmax, true), 6);
		pp = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		// following text: "Max:"
		pp.add(new Label(res.getString("Max_")));
		pp.add(maxTF);
		p.add(pp, "North");
		TextField minTF = new TextField(StringUtil.doubleToStr(dmin, dmin, dmax, false), 6);
		pp = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		// following text: "Min:"
		pp.add(new Label(res.getString("Min_")));
		pp.add(minTF);
		p.add(pp, "South");

		slider = new Slider();
		slider.setIsHorisontal(false);
		slider.setMaxNBreaks(1);
		double vmin = dmin, vmax = dmax;
		if (vmin > 0 && vmax > 0) {
			vmin = 0f;
		} else if (vmin < 0 && vmax < 0) {
			vmax = 0f;
		}
		slider.setMinMax(vmin, vmax);
		//System.out.println("* sl smp 1, val="+vis.getCmp());
//ID
		if (Double.isNaN(vis.getCmp()) && dmin * dmax < 0) {
			slider.setMidPoint(0f);
			cmpTF.setText("0");
		} else {
			slider.setMidPoint(vis.getCmp());
		}
		slider.setTextField(cmpTF);
//~ID
		slider.setPositiveHue(-1f);
		slider.setNegativeHue(-1f);
		slider.setUseShades(false);
		slider.addSliderListener(this);
		PlotCanvas canvas = new PlotCanvas();
		slider.setCanvas(canvas);
		canvas.setContent(slider);
		canvas.setInsets(4, 10, 0, 10);

		// following text: "dynamic map update"
		dynRepaintCB = new Checkbox(res.getString("dynamic_map_update"));

		pp = new Panel(new BorderLayout());
		pp.add(canvas, "West");
		pp.add(new FocuserWithMultiDotPlots(sup, vis, dataTable, true, minTF, maxTF, dynRepaintCB), "Center");
		p.add(pp, "West");

		add(p, "Center");

		p = new Panel(new ColumnLayout());
		p.add(new Line(false));
		//dynRepaintCB=new Checkbox("dynamic map update");
		p.add(dynRepaintCB);
		if (singleAttr) {
			//the manipulator uses object click for visual comparison only in the
			//case when a single attribute is represented
			cmpan = new ClickManagePanel(supervisor);
			cmpan.setComparer(this);
			p.add(cmpan);
		}
		add(p, "South");
		return true;
	}

	/*
	* Results of visual comparison. nBreaks<=1
	*/
	@Override
	public void breaksChanged(Object source, double[] breaks, int nBreaks) {
		if (source == slider) {
			//System.out.println("* sl.cmp="+slider.getMidPoint());
			vis.setCmp(slider.getMidPoint());
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
		}
	}

	/*
	* Action
	*/
	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource() instanceof ColorCanvas) {
			// finding the ColorCanvas clicked
			ColorCanvas cc = null;
			String name = null;
			for (int i = 0; i < ccs.length && cc == null; i++)
				if (ccs[i] == ae.getSource()) {
					cc = ccs[i];
					name = vis.getAttrName(i);
				}
			// getting new color for it
			if (cDlg == null) {
				cDlg = new ColorDlg(CManager.getAnyFrame(this), "");
			}
			//cDlg.setVisible(true);
			// following text: "Color for: "
			cDlg.setTitle(res.getString("Color_for_") + name);
			cDlg.selectColor(this, cc, cc.getColor());
			return;
		}
	}

	/**
	* Change of colors, method from SliderListener interface
	*/
	@Override
	public void colorsChanged(Object source) {
	}

	/*
	* color change through the dialog
	*/
	@Override
	public void colorChanged(Color c, Object sel) {
		// find a ColorCanvas produced the message
		ColorCanvas cc = null;
		int idx = -1;
		for (int i = 0; i < ccs.length && cc == null; i++)
			if (ccs[i] == sel) {
				cc = ccs[i];
				idx = i;
			}
		// save a color
		cc.setColor(c);
		vis.setColorForAttribute(c, idx);
		// hide a dialog
		cDlg.setVisible(false);
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource() == supervisor) {
			if (pce.getPropertyName().equals(Supervisor.eventAttrColors)) {
				for (int i = 0; i < ccs.length; i++) {
					String id = vis.getInvariantAttrId(i);
					Color newColor = supervisor.getColorForAttribute(id);
					if (ccs[i].getColor() != null && !ccs[i].getColor().equals(newColor)) {
						ccs[i].setColor(newColor);
						if (!newColor.equals(vis.getColorForAttribute(i))) {
							vis.setColorForAttribute(newColor, i);
						}
					}
				}
			}
			return;
		} else if (pce.getSource() instanceof AttributeTransformer) {
			if (pce.getPropertyName().equals("values")) {
				double dmin = vis.getDataMin(), dmax = vis.getDataMax();
				cmpTF.setText(StringUtil.doubleToStr(vis.getCmp(), dmin, dmax));
				double vmin = dmin, vmax = dmax;
				if (vmin > 0 && vmax > 0) {
					vmin = 0f;
				} else if (vmin < 0 && vmax < 0) {
					vmax = 0f;
				}
				slider.setMinMaxCmp(vmin, vmax, vis.getCmp());
			}
		}
	}

//---------------- implementation of the EventReceiver interface ------------
	/**
	* The EventReceiver answers whether it is interested in getting the specified
	* kind of events. A MultiVisComparison device is interested in receiving
	* object clicks events only in the case when a single attribute is represented.
	* It uses them for setting reference values for visual comparison.
	*/
	@Override
	public boolean doesListenToEvent(String eventId) {
		return singleAttr && eventId.equals(ObjectEvent.click);
	}

	/**
	* This method is used for delivering events to the Event Receiver.
	*/
	@Override
	public void eventOccurred(DEvent evt) {
		if (!singleAttr || vis == null || dataTable == null)
			return;
		if (evt instanceof ObjectEvent) {
			ObjectEvent oe = (ObjectEvent) evt;
			if (StringUtil.sameStrings(oe.getSetIdentifier(), dataTable.getEntitySetIdentifier()) && oe.getType().equals(ObjectEvent.click)) {
				int attrN = dataTable.getAttrIndex(vis.getAttrId(0));
				if (attrN < 0)
					return;
				boolean changed = false;
				for (int i = 0; i < oe.getAffectedObjectCount() && !changed; i++) {
					String objId = oe.getObjectIdentifier(i);
					int recN = dataTable.indexOf(objId);
					if (recN >= 0) {
						double val = dataTable.getNumericAttrValue(attrN, recN);
						if (!Double.isNaN(val)) {
							//System.out.println("* sl smp 2, val="+val);
							slider.setMidPoint(val);
							changed = true;
						}
					}
				}
				if (!changed) {
					if (slider.getMin() * slider.getMax() < 0) {
						slider.setMidPoint(0f);
					} else {
						slider.setMidPoint(slider.getMin());
					}
				}
				slider.redraw();
				slider.notifyBreaksChange();
			}
		}
	}

	/**
	* Removes itself from receivers of object events
	*/
	@Override
	public void destroy() {
		if (cmpan != null) {
			cmpan.destroy();
		}
		slider.removeSliderListener(this);
		supervisor.removePropertyChangeListener(this);
		destroyed = true;
	}

	/**
	* Replies whether is destroyed or not
	*/
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	/**
	* Returns a unique identifier of the component
	* (used only internally, not shown to the user).
	* The identifier is used for explicit linking of producers and recipients of
	* object events.
	*/
	@Override
	public String getIdentifier() {
		return "Bar_Comparison_" + instanceN;
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
}
