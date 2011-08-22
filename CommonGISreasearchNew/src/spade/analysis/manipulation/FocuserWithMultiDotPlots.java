package spade.analysis.manipulation;

import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.TextField;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.analysis.plot.DotPlot;
import spade.analysis.system.Supervisor;
import spade.analysis.transform.AttributeTransformer;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.FocusListener;
import spade.lib.basicwin.Focuser;
import spade.vis.action.ObjectEvent;
import spade.vis.action.ObjectEventHandler;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTreater;
import spade.vis.event.EventSource;
import spade.vis.mapvis.MultiNumberDrawer;

/**
* This component is used to manipulate map visualisations presenting several
* comparable numeric attributes. Each dot plot represents dispersion of
* values of one of the attributes. The focuser allows to restrict the
* range of values represented on the map. Usually this operation is accompanied
* by "zooming" of the signs used to represent values of the attributes.
*/

public class FocuserWithMultiDotPlots extends Canvas implements MouseListener, MouseMotionListener, FocusListener, Destroyable, DataTreater, ObjectEventHandler, EventSource, PropertyChangeListener {
	/**
	* Used to generate unique identifiers of instances of the manipulator
	*/
	protected static int nInstances = 0;
	protected int instanceN = 0;

	protected MultiNumberDrawer vis = null;
	protected Vector attr = null;
	protected Focuser f = null;
	protected DotPlot dp[] = null;
	protected Checkbox dynRepaintCB = null;

	protected Supervisor supervisor = null;
	protected AttributeDataPortion table = null;
	/**
	* Indicates whether the focuser will use zero as its absolute lower (upper)
	* limit or minimum (maximum) value in actual data.
	*/
	protected boolean adjustLimitsToZero = true;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;

	/**
	* The argument adjustLimitsToZero indicates whether the focuser will use zero
	* as its absolute lower (upper) limit or minimum (maximum) value in actual
	* data.
	*/
	public FocuserWithMultiDotPlots(Supervisor sup, MultiNumberDrawer vis, AttributeDataPortion dataTable, boolean adjustLimitsToZero, TextField minTF, TextField maxTF, Checkbox dynRepaintCB) {
		super();
		instanceN = ++nInstances;
		this.vis = vis;
		this.dynRepaintCB = dynRepaintCB;
		table = dataTable;
		this.adjustLimitsToZero = adjustLimitsToZero;
		attr = vis.getAttributes();
		supervisor = sup;
		if (supervisor != null) {
			supervisor.registerObjectEventSource(this);
		}
		vis.addVisChangeListener(this);
		AttributeTransformer aTrans = vis.getAttributeTransformer();
		if (aTrans != null) {
			aTrans.addPropertyChangeListener(this);
		} else if (table != null) {
			table.addPropertyChangeListener(this);
		}

		//constructFocuser();
		f = new Focuser();
		f.setTextFields(minTF, maxTF);
		f.addFocusListener(this);
		f.setIsVertical(true);
		double vmin = vis.getDataMin(), vmax = vis.getDataMax();
		if (adjustLimitsToZero)
			if (vmin > 0) {
				vmin = 0f;
			} else if (vmax < 0) {
				vmax = 0f;
			}
//ID
		f.setMinMax(vmin, vmax, vis.getFocuserMin(), vis.getFocuserMax());
//    f.setAbsMinMax(vmin,vmax); //vis.getDataMin(),vis.getDataMax());
//~ID
		f.setSpacingFromAxis(0);
		f.setIsLeft(true);
		f.setToDrawCurrMinMax(true);
		f.setTextDrawing(false);
		f.setIsUsedForQuery(true);
		//constructDotPlots();
		dp = new DotPlot[attr.size()];
		Vector attrs = vis.getAttributeList();
		for (int i = 0; i < dp.length; i++) {
			dp[i] = new DotPlot(false, false, true, supervisor, this);
			dp[i].setIsZoomable(false);
			dp[i].setFocuserDrawsTexts(false);
			dp[i].setTextDrawing(false);
			dp[i].setDataSource(dataTable);
			dp[i].setReactToTableDataChange(false);
			dp[i].setFieldNumber(dataTable.getAttrIndex((String) attrs.elementAt(i)));
			dp[i].setMinMax(vis.getDataMin(i), vis.getDataMax(i));
			if (aTrans != null) {
				dp[i].setAttributeTransformer(aTrans, false);
			}
			dp[i].setup();
			dp[i].checkWhatSelected();
			dp[i].setCanvas(this);
		}
		//
		addMouseListener(this);
		addMouseMotionListener(this);
	}

	@Override
	public void paint(Graphics g) {
		g.setColor(Color.black);
		g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
		f.setAlignmentParameters(30, getSize().height - 10, getSize().height - 20);
		f.draw(g);
		double vmin = vis.getDataMin(), vmax = vis.getDataMax();
		if (adjustLimitsToZero) {
			if (vmin > 0 && vmax > 0) {
				vmin = 0f;
			}
			if (vmin < 0 && vmax < 0) {
				vmax = 0f;
			}
		}
		for (int i = 0; i < dp.length; i++) {
			/*
			float rmin=(vis.getDataMin(i)-vis.getDataMin())/(vis.getDataMax()-vis.getDataMin()),
			      rmax=(vis.getDataMax(i)-vis.getDataMin())/(vis.getDataMax()-vis.getDataMin());
			*/
			double rmin = (vis.getDataMin(i) - vmin) / (vmax - vmin), rmax = (vis.getDataMax(i) - vmin) / (vmax - vmin);
			int imax = 10 + (int) Math.round((1 - rmax) * (getSize().height - 20)), imaxmin = (int) Math.round((rmax - rmin) * (getSize().height - 20));
			dp[i].setBounds(new Rectangle(35 + 20 * i, imax - Focuser.getRequiredMargin(), 20, imaxmin + 2 * Focuser.getRequiredMargin()));
			dp[i].draw(g);
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(30 + attr.size() * 20, 200);
	}

	/**
	* Returns its focuser
	*/
	public Focuser getFocuser() {
		return f;
	}

	/*
	* methods from FocusListener interface - begin
	*/
	@Override
	public void focusChanged(Object source, double lowerLimit, double upperLimit) {
		//System.out.println("* min="+lowerLimit+", max="+upperLimit);
		repaint();
		vis.setFocuserMinMax(lowerLimit, upperLimit);
	}

	@Override
	public void limitIsMoving(Object source, int n, double currValue) {
		if (!dynRepaintCB.getState())
			return;
		//System.out.println("* n="+n+", value="+currValue);
		if (n == 0) {
			focusChanged(source, currValue, f.getCurrMax());
		} else {
			focusChanged(source, f.getCurrMin(), currValue);
		}
	}

	/*
	* methods from FocusListener interface - end
	*/

	/*
	* mouse methods - begin
	*/
	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (f.isMouseCaptured()) {
			Graphics g = getGraphics();
			f.mouseDragged(e.getX(), e.getY(), g);
			g.dispose();
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		f.captureMouse(e.getX(), e.getY());
		if (f.isMouseCaptured()) {
			setCursor(new Cursor(Cursor.N_RESIZE_CURSOR));
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (f.isMouseCaptured()) {
			f.releaseMouse();
			setCursor(Cursor.getDefaultCursor());
		}
	}

	/*
	* mouse methods - end
	*/

	/**
	* Destroys its dot plots, i.e. makes them unregister from
	* listening highlighting and other events.
	*/
	@Override
	public void destroy() {
		if (destroyed)
			return;
		if (table != null) {
			table.removePropertyChangeListener(this);
		}
		if (dp != null) {
			for (DotPlot element : dp) {
				element.destroy();
			}
		}
		if (f != null) {
			f.destroy();
		}
		if (supervisor != null) {
			supervisor.removeObjectEventSource(this);
		}
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
	* The focuser receives object events from its dotplots and tranferres
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
		return "Multi_Num_Attr_Focuser_" + instanceN;
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
	* this Data Treater deals with. Returns colors from the visualizer.
	*/
	@Override
	public Vector getAttributeColors() {
		return vis.getAttributeColors();
	}

	/**
	* Reacts to changes of table data. In particular, the absolute minimum or
	* maximum values may change. In this case the focuser must be redrawn.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(table) && (e.getPropertyName().equals("data_added") || e.getPropertyName().equals("data_removed") || e.getPropertyName().equals("data_updated") || e.getPropertyName().equals("values"))) {
			if (e.getPropertyName().equals("values")) {
				boolean changed = false;
				Vector v = (Vector) e.getNewValue();
				if (v != null) {
					for (int i = 0; i < v.size() && !changed; i++) {
						changed = attr.contains(v.elementAt(i));
					}
				}
				if (!changed)
					return;
			}
			vis.actualizeParameters();
			for (int i = 0; i < dp.length; i++) {
				dp[i].setMinMax(vis.getDataMin(i), vis.getDataMax(i));
				dp[i].setup();
			}
			double vmin = vis.getDataMin(), vmax = vis.getDataMax();
			if (adjustLimitsToZero) {
				if (vmin > 0 && vmax > 0) {
					vmin = 0f;
				}
				if (vmin < 0 && vmax < 0) {
					vmax = 0f;
				}
			}
			f.setAbsMinMax(vmin, vmax); //vis.getDataMin(),vis.getDataMax());
			repaint();
		} else if (e.getSource().equals(vis) && dp != null) {
			Vector attrs = vis.getAttributeList();
			boolean changed = false;
			for (int i = 0; i < attrs.size() && !changed; i++) {
				changed = !((String) attrs.elementAt(i)).equalsIgnoreCase(table.getAttributeId(dp[i].getFieldNumber()));
			}
			if (changed) {
				for (int i = 0; i < dp.length; i++) {
					dp[i].setFieldNumber(table.getAttrIndex((String) attrs.elementAt(i)));
					dp[i].setup();
					dp[i].redraw();
				}
			}
		}
		if ((e.getSource() instanceof AttributeTransformer) && e.getPropertyName().equals("values")) {
			double vmin = vis.getDataMin(), vmax = vis.getDataMax();
			if (adjustLimitsToZero) {
				if (vmin > 0 && vmax > 0) {
					vmin = 0f;
				}
				if (vmin < 0 && vmax < 0) {
					vmax = 0f;
				}
			}
			f.setMinMax(vmin, vmax, vmin, vmax);
			for (int i = 0; i < dp.length; i++) {
				dp[i].setMinMax(vis.getDataMin(i), vis.getDataMax(i));
				dp[i].setup();
			}
			repaint();
		}
	}
}
