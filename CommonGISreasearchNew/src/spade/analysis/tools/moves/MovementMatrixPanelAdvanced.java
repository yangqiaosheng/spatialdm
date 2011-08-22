package spade.analysis.tools.moves;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.FocusListener;
import spade.lib.basicwin.Focuser;
import spade.lib.basicwin.FocuserCanvas;
import spade.lib.basicwin.RowLayout;
import spade.lib.util.Matrix;
import spade.lib.util.StringUtil;
import spade.vis.spec.LinkDataDescription;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Mar 19, 2008
 * Time: 11:19:47 AM
 *
 * MovementMatrixPanelAdvanced extends MovementMatrixPanel
 * which is kept for OASIS purposes
 *
 * Represents a set of movements in an aggregated way in a matrix with the
 * rows and columns corresponding to the source and destination locations,
 * respectively, and symbols in the cells represent various aggregated
 * values: N of moves, total amount of items moved, average time, etc.
 */
public class MovementMatrixPanelAdvanced extends Panel implements ItemListener, FocusListener, PropertyChangeListener, Destroyable {
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;
	/**
	 * The canvas in which the visualisation is done
	 */
	protected MovementMatrixCanvas matrix = null;
	/**
	 * The scroll pane containing the canvas
	 */
	protected ScrollPane scp = null;
	/**
	 * A description (metadata) of the links (movements) specified in the
	 * source table
	 */
	protected LinkDataDescription ldd = null;
	/**
	 * Aggregates the movement or transportation data from the source table
	 * by pairs of locations (source, destination)
	 */
	protected AggregatedMovesInformer ma = null;
	/**
	 * Indicates whether the AggregatedMovesInformer should be destroyed when this
	 * display is destroyed. By default is false.
	 */
	protected boolean destroyAggregatorWhenDestroyed = false;

	protected int na = 0; // number of attribute to display: 0..nAttr-1

	protected Choice chAttr = null, chVisMethod = null;

	protected Focuser f = null;

	protected Checkbox cbSort = null, cbRemoveZero = null;
	protected Choice chSortMode = null, chSortAscDesc = null;
	protected Panel panelToAddControls = null;
	/**
	 * The matrix with the distances and/or times between the source and
	 * destination locations
	 */
	public Matrix distancesMatrix = null;

	/**
	 * Constructs the visualisation display.
	 * @param ma - supplies transportation or movement data aggregated by pairs (source, destination)
	 * @param distancesMatrix - the matrix with the distances and/or travel times
	 *                          between the source and destination locations
	 */
	public MovementMatrixPanelAdvanced(Supervisor supervisor, String locationsSetId, AggregatedMovesInformer ma, Matrix distancesMatrix, boolean includeFocuser) {
		this.ma = ma;
		this.distancesMatrix = distancesMatrix;
		ma.addPropertyChangeListener(this);
		setLayout(new BorderLayout());
		na = 0;
		matrix = new MovementMatrixCanvas(supervisor, locationsSetId, null, distancesMatrix);
		scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
		scp.add(matrix);
		add(scp, BorderLayout.CENTER);

		matrix.setAggregatedMovesInformer(ma);
		matrix.setNa(na);

		Panel pControls = new Panel(new ColumnLayout());
		add(pControls, BorderLayout.SOUTH);
		Panel p = new Panel(new RowLayout(1, 0));
		panelToAddControls = p;
		pControls.add(p);
		p.add(new Label("What to show:"));
		p.add(chAttr = new Choice());
		chAttr.addItemListener(this);
		String aNames[] = ma.getAttrNames();
		for (String aName : aNames) {
			chAttr.add(aName);
		}
		p.add(new Label("How to display:"));
		p.add(chVisMethod = new Choice());
		chVisMethod.addItemListener(this);
		chVisMethod.add("Squares");
		chVisMethod.add("Vertical bars");
		chVisMethod.add("Horisontal bars");
		if (distancesMatrix != null) {
			chVisMethod.add("T (+ distances)");
		}
		if (includeFocuser) {
			p = new Panel(new BorderLayout());
			pControls.add(p);
			f = new Focuser();
			f.setIsVertical(false);
			f.setSingleDelimiter("right");
			f.addFocusListener(this);
			float max = ma.getMaxIntMatrixValue(na);
			f.setAbsMinMax(0f, max);
			f.setIsUsedForQuery(true);
			TextField tfmin = new TextField("0"), tfmax = new TextField(StringUtil.floatToStr(max, 0), 10);
			f.setTextFields(tfmin, tfmax);
			p.add(new FocuserCanvas(f, false), BorderLayout.CENTER);
			p.add(tfmax, BorderLayout.EAST);
			p.add(new Label("Max shown:"), BorderLayout.WEST);
		}
		p = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		pControls.add(p);
		p.add(cbSort = new Checkbox("Sort by", true));
		cbSort.addItemListener(this);
		p.add(chSortMode = new Choice());
		chSortMode.addItemListener(this);
		chSortMode.add("alphabetically");
		chSortMode.add("values in all sources and destinations");
		chSortMode.add("values in selected sources/destinations");
		if (distancesMatrix != null) {
			chSortMode.add("distances to selected sources/destinations");
		}
		p.add(new Label("in order:"));
		p.add(chSortAscDesc = new Choice());
		chSortAscDesc.addItemListener(this);
		chSortAscDesc.add("descending");
		chSortAscDesc.add("ascending");
		p.add(cbRemoveZero = new Checkbox("remove empty", false));
		cbRemoveZero.addItemListener(this);
		chSortMode.setEnabled(cbSort.getState());
		chSortAscDesc.setEnabled(cbSort.getState());
		cbRemoveZero.setEnabled(cbSort.getState());
		matrix.setSortMode(cbSort.getState(), chSortAscDesc.getSelectedIndex() == 1, cbRemoveZero.getState(), chSortMode.getSelectedIndex());
	}

	public void addUIElement(Component uiElement) {
		if (uiElement == null)
			return;
		panelToAddControls.add(uiElement);
	}

	/**
	 * Registers a listener of selections of sources and/or destinations
	 * (in fact, passes it to the canvas).
	 */
	public void addSiteSelectionListener(PropertyChangeListener l) {
		if (matrix != null) {
			matrix.addSiteSelectionListener(l);
		}
	}

	/**
	 * Removes the listener of selections of sources and/or destinations
	 * (in fact, calls the respective method of the canvas).
	 */
	public void removeSiteSelectionListener(PropertyChangeListener l) {
		if (matrix != null) {
			matrix.removeSiteSelectionListener(l);
		}
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension d = matrix.getPreferredSize(), s = super.getPreferredSize();
		return new Dimension(Math.max(s.width, d.width + scp.getVScrollbarWidth() + 10), Math.max(s.height, d.height + scp.getHScrollbarHeight() + 10));
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (chVisMethod == ie.getSource()) {
			matrix.setDrawMode(chVisMethod.getSelectedIndex() + 1);
			return;
		}
		if (chAttr == ie.getSource()) {
			na = chAttr.getSelectedIndex();
			matrix.setNa(na);
			if (f != null) {
				double prevValue = f.getCurrMax(), prevMax = f.getAbsMax(), currMax = ma.getMaxIntMatrixValue(na), currValue = (prevMax == 0f) ? currMax : currMax * prevValue / prevMax;
				f.setAbsMinMax(0f, currMax);
				f.setCurrMinMax(0f, currValue);
				f.refresh();
				matrix.setFocuserValue((float) currValue);
			} else {
				matrix.setFocuserValue(ma.getMaxIntMatrixValue(na));
			}
			notifyPropertyChange("aggregation_attribute", new Integer(na));
			return;
		}
		if (cbSort == ie.getSource() || cbRemoveZero == ie.getSource() || chSortMode == ie.getSource() || chSortAscDesc == ie.getSource()) {
			boolean b = cbSort.getState();
			chSortMode.setEnabled(b);
			chSortAscDesc.setEnabled(b);
			cbRemoveZero.setEnabled(b & chSortMode.getSelectedIndex() <= 1);
			if (chSortMode.getSelectedIndex() >= 2) {
				cbRemoveZero.setState(false);
			}
			matrix.setSortMode(cbSort.getState(), chSortAscDesc.getSelectedIndex() == 1, cbRemoveZero.getState(), chSortMode.getSelectedIndex());
			return;
		}
	}

//----------------------- spade.lib.basicwin.FocusListener interface ------------
	@Override
	public void focusChanged(Object source, double lowerLimit, double upperLimit) {
		if (source.equals(f)) {
			matrix.setFocuserValue((float) upperLimit);
		}
	}

	@Override
	public void limitIsMoving(Object source, int n, double currValue) {
		/* too much blinking...
		if (source.equals(f))
		  matrix.setFocuserValue(currValue);
		*/
	}

//----------------------- spade.lib.basicwin.FocusListener interface ------------

	/**
	* Reacts to changes of the aggregated data and to the destroy of the aggregator
	*/
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource().equals(ma)) {
			if (pce.getPropertyName().equals("data")) {
				double prevValue = f.getCurrMax();
				float max = ma.getMaxIntMatrixValue(na);
				if (f != null)
					if (max < f.getAbsMax()) {
						max = (float) f.getAbsMax();
					} else {
						f.setAbsMinMax(0f, max);
						f.setCurrMinMax(0f, max);
						f.refresh();
					}
				else {
					;
				}
				if (prevValue > max) {
					matrix.setFocuserValue(max);
				} else {
					matrix.redraw();
				}
			} else if (pce.getPropertyName().equals("destroy")) {
				destroy();
			}
		}
	}

	/**
	 * Specifies whether the MovementAggregator should be destroyed when this
	 * display is destroyed.
	 */
	public void setDestroyAggregatorWhenDestroyed(boolean destroyAggregatorWhenDestroyed) {
		this.destroyAggregatorWhenDestroyed = destroyAggregatorWhenDestroyed;
	}

	/**
	* Makes necessary operations for destroying, e.g. unregisters from
	* listening highlighting and other events.
	*/
	@Override
	public void destroy() {
		if (ma != null) {
			ma.removePropertyChangeListener(this);
		}
		//removeMouseMotionListener(this);
		destroyed = true;
		if (destroyAggregatorWhenDestroyed && (ma instanceof Destroyable)) {
			((Destroyable) ma).destroy();
		}
		notifyPropertyChange("destroy", null);
	}

	/**
	* Replies whether is destroyed or not
	*/
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	/**
	 * A MovementMatrix may have listeners reacting to the selection of the
	 * aggregation attribute (N of trips, N of people, etc.).
	 * To handle the list of listeners and notify them about changes of the
	 * aggregated data, the MovementAggregator uses a PropertyChangeSupport.
	 */
	protected PropertyChangeSupport pcSupport = null;

	/**
	 * Adds a listener of changes of the selection
	 */
	@Override
	public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
		if (l == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(l);
	}

	/**
	 * Removes a listener of changes of the selection
	 */
	@Override
	public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
		if (l == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(l);
	}

	/**
	 * Notifies all listeners about a change of some property
	 */
	public void notifyPropertyChange(String propName, Object value) {
		if (pcSupport == null)
			return;
		pcSupport.firePropertyChange(propName, null, value);
	}
}
