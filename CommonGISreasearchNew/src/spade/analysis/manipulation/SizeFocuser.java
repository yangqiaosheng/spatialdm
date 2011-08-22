package spade.analysis.manipulation;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
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
import spade.lib.basicwin.PlotCanvas;
import spade.lib.lang.Language;
import spade.lib.util.Aligner;
import spade.time.TimeMoment;
import spade.vis.action.ObjectEvent;
import spade.vis.action.ObjectEventHandler;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTreater;
import spade.vis.event.EventSource;
import spade.vis.mapvis.LineThicknessVisualiser;
import spade.vis.mapvis.NumberDrawer;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 29-Jan-2007
 * Time: 17:21:21
 * For a visualiser representing numeric values by sizes (e.g. line thicknesses),
 * allows the user to focuse on value subranges.
 */
public class SizeFocuser extends Panel implements Manipulator, PropertyChangeListener, FocusListener, EventSource, DataTreater, ObjectEventHandler, Destroyable {
	static ResourceBundle res = Language.getTextResource("spade.analysis.manipulation.Res");
	/**
	* Used to generate unique identifiers of instances of SizeFocuser
	*/
	protected static int nInstances = 0;
	protected int instanceN = 0;
	/**
	 * The visualiser being manipulated
	 */
	protected NumberDrawer vis = null;

	protected AttributeDataPortion data = null;
	protected Supervisor supervisor = null;

	protected TextField minTF = null, maxTF = null;
	protected Checkbox dynRepaintCB = null;
	protected Trapezoid trap = null;
	protected DotPlot dp = null, dpfixed = null;
	protected Focuser focuser = null;
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
	* For VisComparison visualizer should be an instance of NumberDrawer.
	*/
	@Override
	public boolean construct(Supervisor sup, Object visualizer, AttributeDataPortion dataTable) {
		if (visualizer == null || dataTable == null)
			return false;
		if (!(visualizer instanceof NumberDrawer))
			return false;
		this.vis = (NumberDrawer) visualizer;
		data = dataTable;
		instanceN = ++nInstances;
		supervisor = sup;
		vis.addVisChangeListener(this);
		AttributeTransformer aTrans = vis.getAttributeTransformer();
		if (aTrans != null) {
			aTrans.addPropertyChangeListener(this);
		}

		double fmin = vis.getFocuserMin(), fmax = vis.getFocuserMax();
		TimeMoment minTime = null, maxTime = null;
		if (vis.isAttrTemporal()) {
			minTime = vis.getMinTime();
			maxTime = vis.getMaxTime();
		}
		int tfLen = Math.max(vis.getFocuserMaxAsString().length(), vis.getFocuserMinAsString().length());

		setLayout(new BorderLayout());
		add(new Label(vis.getAttrName(0)), BorderLayout.NORTH);

		Panel p = new Panel(new BorderLayout());
		maxTF = new TextField(vis.getFocuserMaxAsString(), tfLen);
		Panel pp = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		pp.add(new Label(res.getString("Max_")));
		pp.add(maxTF);
		p.add(pp, "North");
		minTF = new TextField(vis.getFocuserMinAsString(), tfLen);
		pp = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		pp = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		pp.add(new Label(res.getString("Min_")));
		pp.add(minTF);
		p.add(pp, "South");

		trap = new Trapezoid();
		trap.setIsHorizontal(false);
		if (vis instanceof LineThicknessVisualiser) {
			LineThicknessVisualiser lvis = (LineThicknessVisualiser) vis;
			trap.setColor(lvis.getDefaultColor());
			trap.setMinSize(lvis.getMinThickness());
			trap.setMaxSize(lvis.getMaxThickness());
		}
		PlotCanvas canvas = new PlotCanvas();
		trap.setCanvas(canvas);
		canvas.setContent(trap);
		canvas.setInsets(4, 4, 0, 4);
		Panel p1 = new Panel(new BorderLayout());
		p1.add(canvas, "West");

		dp = new DotPlot(false, false, true, supervisor, this);
		dp.setIsZoomable(true);
		dp.setFocuserOnLeft(false);
		dp.setTextDrawing(false);
		dp.setFocuserDrawsTexts(false);
		if (vis.hasSubAttributes()) {
			dp.setMayMoveDelimiters(false);
		}
		dp.setDataSource(dataTable);
		dp.setReactToTableDataChange(false);
		if (aTrans != null) {
			dp.setAttributeTransformer(aTrans, false);
		}
		dp.setFieldNumber(dataTable.getAttrIndex(vis.getAttrId(0)));
		dp.setMinMax(fmin, fmax);
		if (minTime != null && maxTime != null) {
			dp.setAbsMinMaxTime(minTime, maxTime);
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
			focuser.setMinMax(vis.getDataMin(), vis.getDataMax(), vis.getFocuserMin(), vis.getFocuserMax());
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
		if (aTrans != null) {
			dpfixed.setAttributeTransformer(aTrans, false);
		}
		dpfixed.setFieldNumber(dataTable.getAttrIndex(vis.getAttrId(0)));
		dpfixed.setMinMax(vis.getDataMin(), vis.getDataMax());
		if (minTime != null && maxTime != null) {
			dpfixed.setAbsMinMaxTime(minTime, maxTime);
		}
		dpfixed.setup();
		dpfixed.checkWhatSelected();
		dpfixed.setIsHidden(fmax == vis.getDataMax() && fmin == vis.getDataMin());
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
		trap.setAligner(aligner);
		dpfixed.setAligner(aligner);

		add(p, "Center");

		p = new Panel(new ColumnLayout());
		dynRepaintCB = new Checkbox(res.getString("dynamic_map_update"));
		p.add(dynRepaintCB);
		add(p, "South");
		return true;
	}

	@Override
	public void focusChanged(Object source, double lowerLimit, double upperLimit) {
		if (focuser != null) {
			dpfixed.setIsHidden(!focuser.isRestricted());
		}
		vis.setFocuserMinMax((float) lowerLimit, (float) upperLimit);
	}

	@Override
	public void limitIsMoving(Object source, int n, double currValue) {
		dpfixed.setIsHidden(false);
		if (dynRepaintCB.getState()) {
			if (n == 0) {
				vis.setFocuserMinMax((float) currValue, vis.getFocuserMax());
			} else {
				vis.setFocuserMinMax(vis.getFocuserMin(), (float) currValue);
			}
		}
	}

	/**
	* Reaction to changes of the visualizer or to data transformations
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(vis)) {
			if (!vis.getAttrId(0).equalsIgnoreCase(data.getAttributeId(dp.getFieldNumber()))) {
				int aidx = data.getAttrIndex(vis.getAttrId(0));
				dp.setFieldNumber(aidx);
				dp.setup();
				dpfixed.setFieldNumber(aidx);
				dpfixed.setup();
				dp.redraw();
				dpfixed.redraw();
			} else if (vis instanceof LineThicknessVisualiser) {
				LineThicknessVisualiser lvis = (LineThicknessVisualiser) vis;
				if (!lvis.getDefaultColor().equals(trap.getColor())) {
					trap.setColor(lvis.getDefaultColor());
					trap.redraw();
				} else if (lvis.getMinThickness() != trap.getMinSize() || lvis.getMaxThickness() != trap.getMaxSize()) {
					trap.setMinSize(lvis.getMinThickness());
					trap.setMaxSize(lvis.getMaxThickness());
					CManager.validateAll(trap.getCanvas());
					trap.redraw();
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
			}
		}
	}

	/**
	* Removes itself from receivers of object events
	*/
	@Override
	public void destroy() {
		if (supervisor != null) {
			supervisor.removeObjectEventSource(this);
		}
		if (vis.getAttributeTransformer() != null) {
			vis.getAttributeTransformer().removePropertyChangeListener(this);
		}
		vis.destroy();
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
	* Returns a unique identifier of the component
	* (used only internally, not shown to the user).
	* The identifier is used for explicit linking of producers and recipients of
	* object events.
	*/
	@Override
	public String getIdentifier() {
		return "VisComparison_" + instanceN;
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
	* Replies whether it is linked to the data set (table) with the given identifier
	*/
	@Override
	public boolean isLinkedToDataSet(String setId) {
		return vis.isLinkedToDataSet(setId);
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
}
