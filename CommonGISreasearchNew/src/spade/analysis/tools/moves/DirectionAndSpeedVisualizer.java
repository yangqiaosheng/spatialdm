package spade.analysis.tools.moves;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Vector;

import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.StringInRectangle;
import spade.lib.util.StringUtil;
import spade.vis.database.Attribute;
import spade.vis.database.DataItem;
import spade.vis.database.DataTable;
import spade.vis.database.DataTreater;
import spade.vis.database.Parameter;
import spade.vis.database.ThematicDataItem;
import spade.vis.database.ThematicDataOwner;
import spade.vis.geometry.Sign;
import spade.vis.map.MapContext;
import spade.vis.mapvis.BaseVisualizer;
import spade.vis.mapvis.SignDrawer;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Mar 4, 2008
 * Time: 11:58:10 AM
 * To change this template use File | Settings | File Templates.
 */
public class DirectionAndSpeedVisualizer extends BaseVisualizer implements SignDrawer, DataTreater {

	protected float minSpeed = 0f; // used in legend only

	protected DataTable dt = null; // data table
	protected Parameter pars[] = null; // parameters of current aggregation (at least 1: DandS, may be also time)
	protected int idxFirstColumn = 0, idxLastColumn = -1;
	protected Vector vAttr = null;
	protected int idxAttr = -1; // attribute which is selected for visualization
	protected int idxPars[] = null;
	protected int columnNumbers[] = null;
	protected Color segmentColors[] = null;
	protected boolean drawSegments[] = null;

	protected double maxValue = Double.NaN, // max value of the selected attribute
			currMaxValue = maxValue, currMinValue = 0f; // focuser values

	public double getMax() {
		return maxValue;
	}

	public double getCurrMin() {
		return currMinValue;
	}

	public double getCurrMax() {
		return currMaxValue;
	}

	protected boolean stressMaxValue = false, stressMaxValueUseRatio = false;
	protected float stressMaxValueRatio = 0f, stressMaxValueDiff = 0f;

	public void setStressMaxValue(boolean stressMaxValue, boolean stressMaxValueUseRatio, float stressMaxValueRatio, float stressMaxValueDiff) {
		if (this.stressMaxValue == stressMaxValue && this.stressMaxValueUseRatio == stressMaxValueUseRatio && this.stressMaxValueRatio == stressMaxValueRatio && this.stressMaxValueDiff == stressMaxValueDiff)
			return;
		this.stressMaxValue = stressMaxValue;
		this.stressMaxValueUseRatio = stressMaxValueUseRatio;
		this.stressMaxValueRatio = stressMaxValueRatio;
		this.stressMaxValueDiff = stressMaxValueDiff;
		notifyVisChange();
	}

	public Color getSegmentColor(int idx) {
		return segmentColors[idx];
	}

	/**
	* The sign used for data representation
	*/
	protected DirectionAndSpeedSign sign = null;
	/**
	* The sign used for drawing the icon and changing sign parameters
	*/
	protected DirectionAndSpeedSign dass = null;

	/**
	* Returns the name of the visualization method implemented by this
	* Visualizer.
	*/
	@Override
	public String getVisualizationName() {
		if (visName == null) {
			visName = "Direction & speed diagrams";
		}
		return visName;
	}

	// these methods provide access to attributes and parameters
	// they are used by manipulator
	public Vector getDASAttributeList() {
		return vAttr;
	}

	public Parameter[] getParameters() {
		return pars;
	}

	public void setDataSource(int nColumns, float minSpeed, DataTable dt, Parameter pars[], int idxFirstColumn) {
		this.dt = dt;
		this.pars = pars;
		this.idxFirstColumn = idxFirstColumn;
		this.minSpeed = minSpeed;
		idxLastColumn = dt.getAttrCount() - 1;
		if (nColumns == 5) {
			segmentColors = new Color[] { Color.GRAY, Color.BLUE, Color.GREEN.darker(), Color.RED, Color.MAGENTA };
		} else {
			segmentColors = new Color[] { Color.GRAY, Color.BLUE, // N
					new Color(0, 191, 191), // NE
					Color.GREEN.darker(), // E
					new Color(127, 127, 0), // SE
					Color.RED, // S
					new Color(191, 0, 127), // SW
					Color.MAGENTA, // W
					new Color(127, 0, 255) };
		}
		; // NE
		drawSegments = new boolean[nColumns];
		for (int i = 0; i < drawSegments.length; i++) {
			drawSegments[i] = true;
		}
		idxPars = new int[pars.length];
		vAttr = new Vector(10, 10);
		for (int i = idxFirstColumn; i <= idxLastColumn; i++) {
			Attribute attr = dt.getAttribute(i).getParent();
			if (attr != null && vAttr.indexOf(attr) == -1) {
				vAttr.addElement(attr);
			}
		}
		if (vAttr.size() > 0) {
			idxAttr = 0;
			for (int i = 0; i < idxPars.length; i++) {
				idxPars[i] = 0;
			}
		}
		columnNumbers = new int[nColumns];
		setColumnNumbers();
	}

	public void setIdxAttr(int idxAttr) {
		if (this.idxAttr != idxAttr) {
			this.idxAttr = idxAttr;
			maxValue = Float.NaN;
			currMaxValue = Float.NaN;
			setColumnNumbers();
		}
	}

	public void setIdxParsValue(int idx, int value) {
		idxPars[idx] = value;
		setColumnNumbers();
	}

	public void setDrawSegment(int idx, boolean toDraw) {
		drawSegments[idx] = toDraw;
		if (sign != null) {
			sign.setDrawSegment(drawSegments);
		}
		if (dass != null) {
			dass.setDrawSegment(drawSegments);
		}
		notifyVisChange();
	}

	protected void setColumnNumbers() {
		int n = idxPars[idxPars.length - 1], mult = 1;
		for (int level = idxPars.length - 2; level >= 0; level--) {
			mult *= pars[level + 1].getValueCount();
			n += idxPars[level] * mult;
		}
		columnNumbers[0] = idxFirstColumn + n + idxAttr * mult * pars[0].getValueCount();
		for (int i = 1; i < columnNumbers.length; i++) {
			columnNumbers[i] = columnNumbers[0] + i * mult;
		}
		setup();
	}

	public void setFocuserMinMax(double min, double max) {
		if (currMinValue == min && currMaxValue == max)
			return;
		currMinValue = min;
		currMaxValue = max;
		if (sign != null) {
			sign.setMinMax(currMinValue, currMaxValue);
			notifyVisChange();
		}
	}

	// --- from BaseVisualizer ---------------------------------------------------------------
	/**
	* Here the Visualizer sets its parameters.
	*/
	@Override
	public void setup() {
		if (dt == null)
			return;
		if (Double.isNaN(maxValue)) {
			for (int i = idxFirstColumn; i <= idxLastColumn; i++) {
				Attribute attr = dt.getAttribute(i).getParent();
				if (attr == vAttr.elementAt(idxAttr)) {
					for (int j = 0; j < dt.getDataItemCount(); j++) {
						double v = dt.getNumericAttrValue(i, j);
						if (Double.isNaN(maxValue) || v > maxValue) {
							maxValue = v;
						}
					}
				}
			}
		}
		if (Double.isNaN(currMaxValue)) {
			currMaxValue = maxValue;
		}
		if (sign != null) {
			sign.setMinMax(currMinValue, currMaxValue);
		}
		notifyVisChange();
	}

	/**
	* This method informs whether the Visualizer produces diagrams.
	* This is important for defining the order of drawing of GeoLayers on the
	* map: the diagrams should be drawn on top of all geography.
	*/
	@Override
	public boolean isDiagramPresentation() {
		return true;
	}

	/**
	* This method is called by a GeoObject. The GeoObject sends its DataItem
	* to the Visualizer, and the Visualizer returns an Object specifying the
	* presentation of data contained in this DataItem.
	*/
	@Override
	public Object getPresentation(DataItem dit, MapContext mc) {
		if (dit == null)
			return null;
		if (dit instanceof ThematicDataItem)
			return getPresentation((ThematicDataItem) dit);
		if (dit instanceof ThematicDataOwner) {
			ThematicDataItem thema = ((ThematicDataOwner) dit).getThematicData();
			if (thema == null)
				return null;
			return getPresentation(thema);
		}
		return null;
	}

	/**
	* Creates a diagram representing the value frow from the given ThematicDataItem
	*/
	public Object getPresentation(ThematicDataItem dit) {
		if (dt == null || dit == null)
			return null;
		if (sign == null) {
			sign = new DirectionAndSpeedSign();
			sign.setMinMax(currMinValue, currMaxValue);
			sign.setDrawSegment(drawSegments);
			sign.setSegmentColors(segmentColors);
		}
		double values[] = new double[columnNumbers.length];
		for (int i = 0; i < values.length; i++) {
			values[i] = dit.getNumericAttrValue(columnNumbers[i]);
		}
		sign.setValues(values);
		if (stressMaxValue) {
			double mv = Double.NaN;
			for (int i = 0; i < drawSegments.length; i++)
				if (drawSegments[i] && !Double.isNaN(values[i]) && (Double.isNaN(mv) || values[i] > mv)) {
					mv = values[i];
				}
			boolean sds[] = drawSegments.clone();
			if (stressMaxValueUseRatio) {
				for (int i = 0; i < drawSegments.length; i++)
					if (values[i] * (1f + stressMaxValueRatio) < mv) {
						sds[i] = false;
					} else {
						;
					}
			} else {
				for (int i = 0; i < drawSegments.length; i++)
					if (values[i] + stressMaxValueDiff < mv) {
						sds[i] = false;
					} else {
						;
					}
			}
			sign.setDrawSegment(sds);
		} else {
			sign.setDrawSegment(drawSegments);
		}
		return sign;
	}

	/**
	* The method from the LegendDrawer interface.
	* Draws the common part of the legend irrespective of the presentation method
	* (e.g. the name of the map), then calls drawMethodSpecificLegend(...)
	*/
	@Override
	public Rectangle drawLegend(Component c, Graphics g, int startY, int leftMarg, int prefW) {
		if (!enabled)
			return drawReducedLegend(c, g, startY, leftMarg, prefW);
		drawCheckbox(c, g, startY, leftMarg);
		int w = switchSize + Metrics.mm(), y = startY;
		g.setColor(Color.black);
		String name = getVisualizationName();
		if (name != null) {
			Point p = StringInRectangle.drawText(g, "Visualization method: " + name, leftMarg + w, y, prefW - w, false);
			y = p.y;
			w = p.x - leftMarg;
		}
		if (y < startY + switchSize + Metrics.mm()) {
			y = startY + switchSize + Metrics.mm();
		}
		if (dt != null) {
			Point p = StringInRectangle.drawText(g, dt.getName(), leftMarg, y, prefW, true);
			y = p.y;
			if (w < p.x - leftMarg) {
				w = p.x - leftMarg;
			}
		}
		// attribute and parameters
		Attribute attr = (Attribute) vAttr.elementAt(idxAttr);
		Point p = StringInRectangle.drawText(g, attr.getName(), leftMarg, y, prefW, false);
		y = p.y;
		if (w < p.x - leftMarg) {
			w = p.x - leftMarg;
		}
		if (idxPars != null) {
			for (int i = 1; i < pars.length; i++) {
				String str = pars[i].getName() + " = " + pars[i].getValue(idxPars[i]).toString();
				p = StringInRectangle.drawText(g, str, leftMarg, y, prefW, false);
				y = p.y;
				if (w < p.x - leftMarg) {
					w = p.x - leftMarg;
				}
			}
		}
		// sign segments
		boolean all = true;
		for (int i = 0; i < drawSegments.length && all; i++) {
			all = drawSegments[i];
		}
		String str = "No move (speed < " + minSpeed + ", shown by circles) + " + drawSegments.length + " directions; " + ((all) ? "all are " : "selection is") + " shown";
		p = StringInRectangle.drawText(g, str, leftMarg, y, prefW, false);
		y = p.y;
		if (w < p.x - leftMarg) {
			w = p.x - leftMarg;
		}
		// dominant direction?
		if (stressMaxValue) {
			str = "Only segments with maximal value";
			if (stressMaxValueUseRatio && stressMaxValueRatio > 0) {
				str += " (or less by " + (StringUtil.doubleToStr(stressMaxValueRatio * 100, 0, 100)) + " %)";
			}
			if (!stressMaxValueUseRatio && stressMaxValueDiff > 0) {
				str += " (or less by " + StringUtil.doubleToStr(stressMaxValueDiff, 0, maxValue) + " )";
			}
			str += " are shown in each sign";
			p = StringInRectangle.drawText(g, str, leftMarg, y, prefW, false);
			y = p.y;
			if (w < p.x - leftMarg) {
				w = p.x - leftMarg;
			}
		}
		// sign description
		FontMetrics fm = g.getFontMetrics();
		int fh = fm.getHeight(), asc = fm.getAscent(), signh = sign.getMaxHeight() / 2;
		int xpos = leftMarg + 20 + 2;
		y += Math.max(signh + 3, asc);
		if (currMaxValue == maxValue) {
			g.fillRect(5, y - signh, 5, signh);
		} else {
			g.drawRect(5, y - signh, 5, signh + 1);
		}
		g.drawLine(xpos, y, xpos + 10, y);
		str = StringUtil.doubleToStr(maxValue, 0, maxValue);
		g.drawString(str, xpos + 12, y);
		if (currMaxValue < maxValue) {
			y += Math.max(signh + 3, asc);
			g.fillRect(5, y - signh, 5, signh);
			g.drawLine(xpos, y, xpos + 10, y);
			str = StringUtil.doubleToStr(currMaxValue, 0, maxValue);
			g.drawString(str, xpos + 12, y);
		}
		if (currMinValue > 0) {
			y += asc;
			g.fillRect(5, y - 2, 5, 2);
			g.drawLine(xpos, y, xpos + 10, y);
			str = StringUtil.doubleToStr(currMinValue, 0, maxValue);
			g.drawString(str, xpos + 12, y);
		}
		y += asc;
		if (currMinValue > 0) {
			g.drawRect(5, y - 2, 5, 2);
		} else {
			g.fillRect(5, y - 2, 5, 2);
		}
		g.drawLine(xpos, y, xpos + 10, y);
		g.drawString("0", xpos + 12, y);

		return new Rectangle(leftMarg, startY, w, y - startY);
	}

	@Override
	public Rectangle drawMethodSpecificLegend(Graphics g, int startY, int leftMarg, int prefW) {
		return null;
	}

	/**
	* Draws a small picture symbolizing this presentation technique
	*/
	@Override
	public void drawIcon(Graphics g, int x, int y, int w, int h) {
		getSignInstance();
		if (dass == null)
			return;
		int maxW = dass.getMaxWidth(), maxH = dass.getMaxHeight();
		dass.setMaxSizes(w - 2, h - 4);
		dass.draw(g, x, y, w, h);
		dass.setMaxSizes(maxW, maxH);
	}

	// --- from DataTreater interface --------------------------------------------------------------
	/**
	* A method from the DataTreater interface.
	* Returns the list of attributes being visualized.
	*/
	@Override
	public Vector getAttributeList() {
		if (columnNumbers == null || columnNumbers.length < 1)
			return null;
		Vector v = new Vector(columnNumbers.length, 1);
		for (int columnNumber : columnNumbers) {
			v.addElement(dt.getAttributeId(columnNumber));
		}
		return v;
	}

	/**
	* A method from the DataTreater interface.
	* Replies whether it is linked to the data set (table) with the given identifier
	*/
	@Override
	public boolean isLinkedToDataSet(String setId) {
		if (dt != null)
			return dt.getEntitySetIdentifier().equals(setId);
		return tableId != null && tableId.equals(setId);
	}

	/**
	* A method from the DataTreater interface.
	* Returns a vector of colors used for representation of the attributes this
	* Data Treater deals with. May return null if no colors are used.
	*/
	@Override
	public Vector getAttributeColors() {
		return null;
	}

	// --- from SignDrawer interface ---------------------------------------------------------------
	/**
	* Produces an instance of the Sign used by this visualizer. This instance will
	* be used for drawing the sign in the dialog for changing sign properties.
	*/
	@Override
	public Sign getSignInstance() {
		if (sign == null)
			return null;
		if (dass == null) {
			dass = new DirectionAndSpeedSign();
			dass.setMinMax(0f, 6f);
			double v[] = { 5f, 2f, 3f, 4f, 7f };
			dass.setValues(v);
			dass.setDrawSegment(drawSegments);
			dass.setSegmentColors(segmentColors);
			copySignProperties(dass);
		}
		return dass;
	}

	/**
	* Sets properties of the given sign instance according to the sign properties
	* used in the visualization.
	*/
	public void copySignProperties(DirectionAndSpeedSign dass) {
		if (dass == null || sign == null)
			return;
		dass.setMaxSizes(sign.getMaxWidth(), sign.getMaxHeight());
		dass.setMustDrawFrame(sign.getMustDrawFrame());
	}

	/**
	* Through this method a SignDrawer is informed that some of the sign properties
	* have changed. The sign drawer must care about repainting the map.
	* The possible identifiers of sign properties are listed in the class
	* @see spade.vis.geometry.Sign
	*/
	@Override
	public void signPropertyChanged(int propertyId, Sign s) {
		if (!(s instanceof DirectionAndSpeedSign))
			return;
		DirectionAndSpeedSign dass = (DirectionAndSpeedSign) s;
		switch (propertyId) {
		case Sign.MAX_SIZE:
			sign.setMaxSizes(dass.getMaxWidth(), dass.getMaxHeight());
			break;
		case Sign.USE_FRAME:
			sign.setMustDrawFrame(dass.getMustDrawFrame());
			break;
		}
		notifyVisChange();
	}

}
