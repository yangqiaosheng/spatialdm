package spade.time.vis;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.transform.AttributeTransformer;
import spade.analysis.transform.TransformerOwner;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.StringInRectangle;
import spade.lib.color.CS;
import spade.lib.color.ColorSelDialog;
import spade.lib.lang.Language;
import spade.lib.util.IntArray;
import spade.lib.util.NumRange;
import spade.lib.util.StringUtil;
import spade.time.TimeMoment;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataItem;
import spade.vis.database.DataTreater;
import spade.vis.database.Parameter;
import spade.vis.database.ThematicDataItem;
import spade.vis.database.ThematicDataOwner;
import spade.vis.dataview.TransformedDataPresenter;
import spade.vis.geometry.Sign;
import spade.vis.map.MapContext;
import spade.vis.mapvis.BaseVisualizer;
import spade.vis.mapvis.SignDrawer;
import spade.vis.mapvis.SignParamsController;
import spade.vis.spec.AnimationAttrSpec;
import spade.vis.spec.TemporalToolSpec;
import spade.vis.spec.ToolSpec;

/**
* Represents the value flow of a numeric attribute by a polygon built from a
* time-series graph. Such polygons are put on the map as diagrams.
*/
public class ValueFlowVisualizer extends BaseVisualizer implements TransformerOwner, SignDrawer, DataTreater, TransformedDataPresenter, PropertyChangeListener {
	static ResourceBundle res = Language.getTextResource("spade.time.vis.Res");
	/**
	* The table with the data to be visualized
	*/
	protected AttributeDataPortion table = null;
	/**
	* A Visualizer may optionally be connected to a transformer of attribute
	* values. In this case, it represents transformed attribute values.
	*/
	protected AttributeTransformer aTrans = null;
	/**
	* The time-dependent super-attribute to be represented, i.e. an attribute
	* having references to child attributes corresponding to different values
	* of a temporal parameter.
	*/
	protected Attribute supAttr = null;
	/**
	* The temporal parameter
	*/
	protected Parameter par = null;
	/**
	* The attribute may additionally depend on some other parameters. The values
	* of these additional parameters must be fixed by the user. These vectors
	* contain the names and the selected values of the additional parameters.
	*/
	protected Vector otherParNames = null, otherParValues = null;
	/**
	* Indices of the table columns used in the visualization. The columns are
	* sorted according to their time references. There must be an element for
	* each parameter value. If such a column has not been found, the element is -1.
	*/
	protected IntArray colNs = null;
	/**
	* Absolute minimum and maximum attribute values from all relevant columns
	*/
	protected double absMin = Double.NaN, absMax = Double.NaN;
	/**
	* The first and the last time moments the data refer to
	*/
	protected TimeMoment start = null, end = null;
	/*
	* Temporal focus (horisontal focusing of the graph): start and finish
	*/
	protected TimeMoment tFocusStart = null, tFocusEnd = null;
	/*
	* indices in colNs for focus start and end
	*/
	protected int idxTFstart = 0, idxTFend = 0;
	/**
	* The length of the interval to be shown
	*/
	protected int iLen = 0;
	/**
	* The array of attribute values used for drawing diagrams
	*/
	protected double values[] = null;
	/**
	* The sign used for data representation
	*/
	protected ValueFlowSign sign = null;
	/**
	* The sign used for drawing the icon and changing sign parameters
	*/
	protected ValueFlowSign vfs = null;

	/**
	* Returns the name of the visualization method implemented by this
	* Visualizer.
	*/
	@Override
	public String getVisualizationName() {
		if (visName == null) {
			visName = res.getString("Value_flow_diagrams");
		}
		return visName;
	}

	/**
	* Sets a reference to the table with the data to visualize.
	*/
	public void setDataSource(AttributeDataPortion table) {
		this.table = table;
	}

	/**
	* Returns the reference to the table with the data to visualize.
	*/
	public AttributeDataPortion getDataSource() {
		return table;
	}

	/**
	* Returns the identifier of the table this Visualizer is linked with.
	*/
	@Override
	public String getTableIdentifier() {
		if (table == null)
			return null;
		return table.getContainerIdentifier();
	}

	/**
	* Sets the time-dependent super-attribute to be represented, i.e. an attribute
	* having references to child attributes corresponding to different values
	* of a temporal parameter.
	*/
	public void setAttribute(Attribute attr) {
		if (attr != null && attr.getChildrenCount() > 1) {
			supAttr = attr;
		}
	}

	/**
	* Returns the name of the time-dependent attribute (i.e. super-attribute)
	* being visualized
	*/
	public String getAttributeName() {
		if (supAttr == null)
			return null;
		return supAttr.getName();
	}

	/**
	* Sets the temporal parameter
	*/
	public void setTemporalParameter(Parameter par) {
		if (par.isTemporal() && par.getValueCount() > 1) {
			this.par = par;
		}
	}

	/**
	* Returns its temporal parameter
	*/
	public Parameter getTemporalParameter() {
		return par;
	}

	/**
	* The attribute may additionally depend on some other parameters. The values
	* of these additional parameters must be fixed by the user. This method
	* is used to specify the user-selected value of the given parameter.
	*/
	public void setOtherParameterValue(String paramName, Object paramValue) {
		if (paramName == null || paramValue == null)
			return;
		if (otherParNames == null) {
			otherParNames = new Vector(5, 5);
			otherParValues = new Vector(5, 5);
			otherParNames.addElement(paramName);
			otherParValues.addElement(paramValue);
			return;
		}
		int idx = StringUtil.indexOfStringInVectorIgnoreCase(paramName, otherParNames);
		if (idx >= 0) {
			otherParValues.setElementAt(paramValue, idx);
		} else {
			otherParNames.addElement(paramName);
			otherParValues.addElement(paramValue);
		}
	}

	/**
	* Retrieves and sorts the indices of the columns with values of the
	* attribute to be visualized. The columns are sorted according to their
	* time references.
	*/
	protected void makeColumnList() {
		if (table == null || supAttr == null || par == null)
			return;
		if (colNs == null) {
			colNs = new IntArray(par.getValueCount(), 5);
		} else {
			colNs.removeAllElements();
		}
		int nCols = 0;
		for (int i = 0; i < par.getValueCount(); i++) {
			int colN = -1;
			Object value = par.getValue(i);
			for (int j = 0; j < supAttr.getChildrenCount() && colN < 0; j++) {
				Attribute attr = supAttr.getChild(j);
				if (attr.hasParamValue(par.getName(), value)) {
					boolean ok = true;
					if (attr.getParameterCount() > 1 && otherParNames != null) {
						for (int k = 0; k < attr.getParameterCount() && ok; k++) {
							String pname = attr.getParamName(k);
							if (!pname.equalsIgnoreCase(par.getName())) {
								int idx = StringUtil.indexOfStringInVectorIgnoreCase(pname, otherParNames);
								if (idx >= 0) {
									ok = attr.getParamValue(k).equals(otherParValues.elementAt(idx));
								}
							}
						}
					}
					if (ok) {
						colN = table.getAttrIndex(attr.getIdentifier());
					}
				}
			}
			if (colN >= 0) {
				++nCols;
			}
			colNs.addElement(colN); //there must be an element for each parameter
			//value. If such a column has not been found, the element is -1.
		}
		if (nCols < 2) {
			colNs = null; //actually no columns relevant to this parameter have been found
			return;
		}
		start = (TimeMoment) par.getValue(0);
		end = (TimeMoment) par.getValue(par.getValueCount() - 1);
		tFocusStart = start;
		tFocusEnd = end;
		idxTFstart = 0;
		idxTFend = nCols - 1;
		iLen = idxTFend - idxTFstart;
	}

	/**
	* Informs whether this visualizer allows the visualized attributes to
	* be transformed. By default, returns true.
	*/
	public boolean getAllowTransform() {
		return true;
	}

	/**
	* Connects the visualizer to a transformer of attribute values. After this,
	* it must represent transformed attribute values. The argument listenChanges
	* determines whether the visualizer will listen to the changes of the transformed
	* values and appropriately reset itself. This is not always desirable; for
	* example, a visualizer may be a part of another visualizer, which makes
	* all necessary changes.
	*/
	public void setAttributeTransformer(AttributeTransformer transformer, boolean listenChanges) {
		if (aTrans != null) {
			aTrans.removePropertyChangeListener(this);
		}
		aTrans = transformer;
		if (listenChanges && aTrans != null) {
			aTrans.addPropertyChangeListener(this);
		}
	}

	/**
	* Returns its transformer of attribute values (if exists)
	*/
	@Override
	public AttributeTransformer getAttributeTransformer() {
		return aTrans;
	}

	/**
	* Informs whether this visualizer allows each of the visualized attributes to
	* be transformed individually (returns true) or requires all attributes to
	* be transformed in the same way (returns false). Returns false.
	*/
	public boolean getAllowTransformIndividually() {
		return false;
	}

	/**
	* A convenience method for retrieving a numeric value from the specified row
	* and column of the table. If the visualizer is attached to an attribute
	* transformer, the value is taken from the transformer.
	*/
	public double getNumericAttrValue(int colN, int rowN) {
		if (aTrans != null)
			return aTrans.getNumericAttrValue(colN, rowN);
		if (table != null)
			return table.getNumericAttrValue(colN, rowN);
		return Double.NaN;
	}

	/**
	* A convenience method for retrieving a numeric value from the
	* column with the given index in the given ThematicDataItem. If the visualizer
	* is attached to an attribute transformer, the value is taken from the
	* transformer.
	*/
	public double getNumericAttrValue(ThematicDataItem data, int colN) {
		if (data == null)
			return Double.NaN;
		if (aTrans != null)
			return aTrans.getNumericAttrValue(colN, data);
		return data.getNumericAttrValue(colN);
	}

	/**
	* Here the Visualizer sets its parameters.
	*/
	@Override
	public void setup() {
		absMin = Double.NaN;
		absMax = Double.NaN;
		if (table == null)
			return;
		if (colNs == null) {
			makeColumnList();
		}
		if (colNs == null)
			return;

		NumRange nr = (aTrans != null) ? aTrans.getValueRangeInColumns(colNs) : table.getValueRangeInColumns(colNs);
		if (nr != null) {
			absMin = nr.minValue;
			absMax = nr.maxValue;
			if (sign != null) {
				sign.setMinMax(absMin, absMax);
			}
		}
	}

	/**
	* Returns the index of the given time moment in the value list of the temporal
	* parameter.
	*/
	protected int getTimeMomentIdx(TimeMoment seekTM) {
		int idx = -1;
		for (int i = 0; i < par.getValueCount() && idx < 0; i++) {
			TimeMoment tm = (TimeMoment) par.getValue(i);
			if (tm.compareTo(seekTM) == 0) {
				idx = i;
			} else if (tm.compareTo(seekTM) > 0)
				if (i > 0) {
					idx = i - 1;
				} else {
					idx = i;
				}
		}
		return idx;
	}

	/*
	* Set the extent of time focusing (start, finish)
	*/
	public void setTimeFocusStart(TimeMoment tFocusStart) {
		this.tFocusStart = tFocusStart;
		idxTFstart = getTimeMomentIdx(tFocusStart);
		iLen = idxTFend - idxTFstart;
	}

	public void setTimeFocusEnd(TimeMoment tFocusEnd) {
		this.tFocusEnd = tFocusEnd;
		idxTFend = getTimeMomentIdx(tFocusEnd);
		iLen = idxTFend - idxTFstart;
	}

	public void setTimeFocusStartEnd(TimeMoment tFocusStart, TimeMoment tFocusEnd) {
		this.tFocusStart = tFocusStart;
		this.tFocusEnd = tFocusEnd;
		idxTFstart = getTimeMomentIdx(tFocusStart);
		idxTFend = getTimeMomentIdx(tFocusEnd);
		iLen = idxTFend - idxTFstart;
	}

	public void setTimeFocusFullExtent() {
		tFocusStart = start;
		tFocusEnd = end;
		idxTFstart = 0;
		idxTFend = par.getValueCount() - 1;
		iLen = idxTFend - idxTFstart;
	}

	/**
	* Returns the start moment of the focus interval
	*/
	public TimeMoment getTimeFocusStart() {
		return tFocusStart;
	}

	/**
	* Returns the end moment of the focus interval
	*/
	public TimeMoment getTimeFocusEnd() {
		return tFocusEnd;
	}

	/**
	* Returns the index of the first currectly represented moment
	*/
	public int getCurrFirstMomentIdx() {
		return idxTFstart;
	}

	/**
	* Returns the index of the last currectly represented moment
	*/
	public int getCurrLastMomentIdx() {
		return idxTFend;
	}

	/**
	* Returns the total number of moments (i.e. values of the temporal parameter),
	* irrespective of the current temporal focus
	*/
	public int getTotalMomentCount() {
		if (par == null)
			return 0;
		return par.getValueCount();
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
		if (table == null || dit == null || colNs == null || iLen < 2)
			return null;
		if (sdController != null && !sdController.mustDrawObject(dit.getId()))
			return null;
		if (values == null || values.length != iLen + 1) {
			values = new double[iLen + 1];
		}
		boolean hasValues = false;
		for (int i = idxTFstart; i <= idxTFend; i++)
			if (colNs.elementAt(i) >= 0) {
				values[i - idxTFstart] = getNumericAttrValue(dit, colNs.elementAt(i));
				hasValues = hasValues || !Double.isNaN(values[i - idxTFstart]);
			} else {
				values[i - idxTFstart] = Double.NaN;
			}
		if (!hasValues)
			return null;
		if (sign == null) {
			sign = new ValueFlowSign();
			sign.setMinMax(absMin, absMax);
		}
		sign.setValues(values);
		return sign;
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
	* Draws the part of the legend explaining this presentation method.
	*/
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
			Point p = StringInRectangle.drawText(g, res.getString("vis_method") + ": " + name, leftMarg + w, y, prefW - w, false);
			y = p.y;
			w = p.x - leftMarg;
		}
		if (y < startY + switchSize + Metrics.mm()) {
			y = startY + switchSize + Metrics.mm();
		}
		if (table != null) {
			Point p = StringInRectangle.drawText(g, table.getName(), leftMarg, y, prefW, true);
			y = p.y;
			if (w < p.x - leftMarg) {
				w = p.x - leftMarg;
			}
		}
		if (aTrans != null) {
			String descr = aTrans.getDescription();
			if (descr != null) {
				descr = res.getString("Data_transformation") + ": " + descr;
				Point p = StringInRectangle.drawText(g, descr, leftMarg, y, prefW, true);
				y = p.y;
				if (w < p.x - leftMarg) {
					w = p.x - leftMarg;
				}
			}
		}
		if (supAttr != null) {
			Point p = StringInRectangle.drawText(g, supAttr.getName(), leftMarg, y, prefW, false);
			y = p.y;
			if (w < p.x - leftMarg) {
				w = p.x - leftMarg;
			}
		}
		if (otherParNames != null) {
			for (int i = 0; i < otherParNames.size(); i++) {
				String str = otherParNames.elementAt(i).toString() + " = " + otherParValues.elementAt(i).toString();
				Point p = StringInRectangle.drawText(g, str, leftMarg, y, prefW, false);
				y = p.y;
				if (w < p.x - leftMarg) {
					w = p.x - leftMarg;
				}
			}
		}
		if (Double.isNaN(absMin) || Double.isNaN(absMax))
			return new Rectangle(leftMarg, startY, w, y - startY);
		getSignInstance();
		double v[] = { 0.6f, 0.8f, 1f, 0.9f, 0.5f, 0.1f, 0.3f, 1f, 0.6f, 0.5f };
		int start = 0;
		double maxMod = (float) Math.abs(absMax);
		if (absMin < 0 && maxMod < Math.abs(absMin)) {
			maxMod = Math.abs(absMin);
		}
		if (Math.abs(absMin) / maxMod > 0.1f) {
			start = 5;
			for (int i = 0; i < start; i++) {
				v[i] *= absMin;
			}
		}
		for (int i = start; i < v.length; i++) {
			v[i] *= absMax;
		}
		vfs.setMinMax(absMin, absMax);
		vfs.setValues(v);
		int upH = (absMax > 0) ? (int) Math.round(absMax * vfs.getMaxHeight() / maxMod) : 0, loH = (absMin < 0) ? (int) Math.round(-absMin * vfs.getMaxHeight() / maxMod) : 0;
		FontMetrics fm = g.getFontMetrics();
		int fh = fm.getHeight(), asc = fm.getAscent();
		y += asc;
		vfs.draw(g, leftMarg, y + upH);
		int xpos = leftMarg + vfs.getMaxWidth() + 2;
		if (absMax < 0) {
			y += Math.round(-absMax * vfs.getMaxHeight() / maxMod);
		}
		g.setColor(Color.black);
		g.drawLine(xpos, y, xpos + 10, y);
		String str = StringUtil.doubleToStr(absMax, absMin, absMax);
		g.drawString(str, xpos + 12, y);
		int sw = fm.stringWidth(str);
		if (xpos + 12 + sw > w) {
			w = xpos + 12 + sw;
		}
		int y1 = y + upH + loH;
		if (absMin > 0) {
			y1 -= Math.round(absMin * vfs.getMaxHeight() / maxMod);
		}
		if (y1 - y < asc) {
			xpos += 12 + sw;
		}
		g.drawLine(xpos, y1, xpos + 10, y1);
		str = StringUtil.doubleToStr(absMin, absMin, absMax);
		g.drawString(str, xpos + 12, y1);
		sw = fm.stringWidth(str);
		if (xpos + 12 + sw > w) {
			w = xpos + 12 + sw;
		}
		y += upH + loH + fh;
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
		if (vfs == null)
			return;
		int maxW = vfs.getMaxWidth(), maxH = vfs.getMaxHeight();
		vfs.setMaxSizes(w - 2, h - 4);
		vfs.draw(g, x, y, w, h);
		vfs.setMaxSizes(maxW, maxH);
	}

	/**
	* Produces an instance of the Sign used by this visualizer. This instance will
	* be used for drawing the sign in the dialog for changing sign properties.
	*/
	@Override
	public Sign getSignInstance() {
		if (sign == null)
			return null;
		if (vfs == null) {
			vfs = new ValueFlowSign();
			vfs.setMinMax(-1f, 1f);
			double v[] = { -0.6f, -0.8f, -1f, -0.9f, -0.5f, 0.1f, 0.3f, 1f, 0.6f, 0.5f };
			vfs.setValues(v);
			copySignProperties(vfs);
		}
		return vfs;
	}

	/**
	* Sets properties of the given sign instance according to the sign properties
	* used in the visualization.
	*/
	public void copySignProperties(ValueFlowSign vfs) {
		if (vfs == null || sign == null)
			return;
		vfs.setMaxSizes(sign.getMaxWidth(), sign.getMaxHeight());
		vfs.setPositiveColor(sign.getPositiveColor());
		vfs.setNegativeColor(sign.getNegativeColor());
		vfs.setMustDrawFrame(sign.getMustDrawFrame());
	}

	/**
	* Through this method a SignDrawer is informed that some of the sign properties
	* have changed. The sign drawer must care about repainting the map.
	* The possible identifiers of sign properties are listed in the class
	* @see spade.vis.geometry.Sign
	*/
	@Override
	public void signPropertyChanged(int propertyId, Sign s) {
		if (!(s instanceof ValueFlowSign))
			return;
		ValueFlowSign vfs = (ValueFlowSign) s;
		switch (propertyId) {
		case Sign.MAX_SIZE:
			sign.setMaxSizes(vfs.getMaxWidth(), vfs.getMaxHeight());
			break;
		case Sign.USE_FRAME:
			sign.setMustDrawFrame(vfs.getMustDrawFrame());
			break;
		}
		notifyVisChange();
	}

	/**
	* Replies whether the color scale used by this visualization method may be
	* changed. This does not include interactive analytical manipulation.
	* Returns true.
	*/
	@Override
	public boolean canChangeColors() {
		return true;
	}

	/**
	* Constructs and displays a dialog for changing colors used in this
	* visualization method (if possible). Changing colors is different from
	* interactive analytical manipulation!
	* By default, does nothing.
	*/
	@Override
	public void startChangeColors() {
		if (sign == null)
			return;
		float hues[] = new float[2];
		hues[0] = CS.getHue(sign.getPositiveColor());
		hues[1] = CS.getHue(sign.getNegativeColor());
		String prompts[] = new String[2];
		// following text:"Positive color ?"
		prompts[0] = res.getString("Positive_color_");
		// following text:"Negative color ?"
		prompts[1] = res.getString("Negative_color_");
		ColorSelDialog csd = new ColorSelDialog(2, hues, null, prompts, true, false);
		OKDialog okd = new OKDialog(CManager.getAnyFrame(), csd.getName(), true);
		okd.addContent(csd);
		okd.show();
		if (okd.wasCancelled())
			return;
		sign.setPositiveColor(Color.getHSBColor(csd.getHueForItem(0), 0.9f, 0.9f));
		sign.setNegativeColor(Color.getHSBColor(csd.getHueForItem(1), 0.9f, 0.9f));
		if (vfs != null) {
			vfs.setPositiveColor(sign.getPositiveColor());
			vfs.setNegativeColor(sign.getNegativeColor());
		}
		notifyVisChange();
	}

	/**
	* Reacts to changes of the transformed data in its AttributeTransformer
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(aTrans) && e.getPropertyName().equals("values")) {
			setup();
			notifyVisChange();
		}
	}

	/**
	* Destroys the visualizer when it is no more used.
	*/
	@Override
	public void destroy() {
		if (aTrans != null) {
			aTrans.removePropertyChangeListener(this);
		}
		super.destroy();
	}

	/**
	* Must reply whether any parameters of this visualization method may be
	* changed, e.g. colors or sizes of signs. This does not include interactive
	* analytical manipulation.
	* Returns true.
	*/
	@Override
	public boolean canChangeParameters() {
		return true;
	}

	/**
	* Constructs and displays a dialog for changing parameters of this
	* visualization method: size of the signs.
	*/
	@Override
	public void startChangeParameters() {
		SignParamsController pc = new SignParamsController();
		pc.startChangeParameters(this);
	}

	/**
	* A method from the DataTreater interface.
	* Returns the list of attributes being visualized.
	*/
	@Override
	public Vector getAttributeList() {
		if (colNs == null || colNs.size() < 1)
			return null;
		Vector v = new Vector(iLen + 1, 1);
		for (int i = idxTFstart; i <= idxTFend; i++)
			if (colNs.elementAt(i) >= 0) {
				v.addElement(table.getAttributeId(colNs.elementAt(i)));
			}
		if (v.size() < 1)
			return null;
		return v;
	}

	/**
	* A method from the DataTreater interface.
	* Replies whether it is linked to the data set (table) with the given identifier
	*/
	@Override
	public boolean isLinkedToDataSet(String setId) {
		if (table != null)
			return table.getEntitySetIdentifier().equals(setId);
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

	/**
	* A method from the TransformedDataPresenter interface.
	* Returns the transformed value corresponding to the given row and column of
	* the original table. If the value is not transformed, returns null.
	*/
	@Override
	public String getTransformedValue(int rowN, int colN) {
		if (aTrans != null)
			return aTrans.getTransformedValueAsString(rowN, colN);
		return null;
	}

	/**
	* Returns the keyword used in the opening tag of a stored state description
	* of this tool. For this visualizer, the tag is "temporal_vis".
	*/
	@Override
	public String getTagName() {
		return "temporal_vis";
	}

	@Override
	public Hashtable getVisProperties() {
		if (sign == null) {
			sign = new ValueFlowSign();
		}
		Hashtable prop = sign.getProperties();
		if (tFocusStart != null || tFocusEnd != null) {
			if (prop == null) {
				prop = new Hashtable();
			}
			if (tFocusStart != null) {
				prop.put("focus_start", tFocusStart.toString());
			}
			if (tFocusEnd != null) {
				prop.put("focus_end", tFocusEnd.toString());
			}
		}
		return prop;
	}

	@Override
	public void setVisProperties(Hashtable prop) {
		if (prop == null)
			return;
		if (par != null && par.getFirstValue() != null && (par.getFirstValue() instanceof TimeMoment)) {
			TimeMoment t = (TimeMoment) par.getFirstValue();
			String val = (String) prop.get("focus_end");
			if (val != null) {
				TimeMoment t1 = t.getCopy();
				if (t1.setMoment(val)) {
					setTimeFocusEnd(t1);
				}
			}
			val = (String) prop.get("focus_start");
			if (val != null) {
				TimeMoment t1 = t.getCopy();
				if (t1.setMoment(val)) {
					setTimeFocusStart(t1);
				}
			}
		}
		if (sign == null) {
			sign = new ValueFlowSign();
		}
		sign.setProperties(prop);
		if (vfs == null) {
			getSignInstance();
		} else {
			copySignProperties(vfs);
		}
		notifyVisChange();
	}

	@Override
	public ToolSpec getVisSpec() {
		TemporalToolSpec spec = new TemporalToolSpec();
		spec.tagName = getTagName();
		spec.methodId = visId;
		spec.table = tableId;
		if (aTrans != null) {
			spec.transformSeqSpec = aTrans.getSpecSequence();
		}
		spec.location = getLocation();
		spec.properties = getVisProperties();
		if (supAttr != null) {
			AnimationAttrSpec asp = new AnimationAttrSpec();
			asp.parent = supAttr.getIdentifier();
			asp.isTimeDependent = true;
			if (otherParNames != null && otherParNames.size() > 0) {
				asp.fixedParams = (Vector) otherParNames.clone();
				asp.fixedParamVals = (Vector) otherParValues.clone();
			}
			spec.attrSpecs = new Vector(1, 1);
			spec.attrSpecs.addElement(asp);
		}
		return spec;
	}
}
