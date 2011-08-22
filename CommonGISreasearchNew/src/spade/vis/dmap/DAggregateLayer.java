package spade.vis.dmap;

import java.awt.Button;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Point;
import java.awt.ScrollPane;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.util.Vector;

import spade.analysis.aggregates.AggregateContainer;
import spade.analysis.classification.ObjectColorer;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKFrame;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.vis.action.ObjectEvent;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.ObjectContainer;
import spade.vis.database.ObjectFilterBySelection;
import spade.vis.database.ThematicDataItem;
import spade.vis.database.TimeFilter;
import spade.vis.event.DEvent;
import spade.vis.event.EventReceiver;
import spade.vis.geometry.GeoComp;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapContext;
import spade.vis.space.GeoLayer;
import ui.AttributeChooser;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 16, 2008
 * Time: 12:40:09 PM
 * A map layer containing DAggregateObjects, which aggregate objects from another layer.
 */
public class DAggregateLayer extends DGeoLayer implements EventReceiver, AggregateContainer {
	/**
	 * A reference to the layer from which the members of the aggregates are taken
	 */
	protected DGeoLayer souLayer = null;
	/**
	 * Indicates which objects of the souLayer are active, i.e. satisfy
	 * the filters of the souLayer
	 */
	protected boolean active[] = null;
	/**
	 * The last time interval selected with the use of the time filter.
	 */
	protected TimeMoment tStart = null, tEnd = null;
	/**
	 * Used for preparation of the layer before the first drawing
	 */
	protected boolean neverDrawn = true;
	/**
	 * The Supervisor from which the layer can receive object events.
	 * When an aggregate is selected by a long pressing of the mouse button,
	 * it shows detailed information about the members of the aggregate.
	 */
	protected Supervisor supervisor = null;
	/**
	 * Indices of the table columns with the general information about the aggregates
	 */
	public int startTimeCN = -1, endTimeCN = -1, durationCN = -1, nMembersCN = -1, nActiveMembersCN = -1, memberIdsCN = -1, activeMemberIdsCN = -1;

	/**
	 * Generates a table with statistical information about the visits
	 * of the places. Attaches the records of the table to the objects
	 * of the layer.
	 */
	public DataTable constructTableWithStatistics() {
		if (geoObj == null || geoObj.size() < 1)
			return null;
		DataTable table = new DataTable();
		if (hasTimeReferences()) {
			table.addAttribute("Start time", "start_time", AttributeTypes.time);
			startTimeCN = table.getAttrCount() - 1;
			Attribute timeAttr = table.getAttribute(startTimeCN);
			timeAttr.timeRefMeaning = Attribute.VALID_FROM;
			table.addAttribute("End time", "end_time", AttributeTypes.time);
			endTimeCN = table.getAttrCount() - 1;
			timeAttr = table.getAttribute(endTimeCN);
			timeAttr.timeRefMeaning = Attribute.VALID_UNTIL;
			table.addAttribute("Duration", "duration", AttributeTypes.integer);
			durationCN = table.getAttrCount() - 1;
		}
		table.addAttribute("N members", "n_members", AttributeTypes.integer);
		nMembersCN = table.getAttrCount() - 1;
		table.addAttribute("N active members", "n_active_members", AttributeTypes.integer);
		nActiveMembersCN = table.getAttrCount() - 1;
		table.addAttribute("IDs of members", "member_ids", AttributeTypes.character);
		memberIdsCN = table.getAttrCount() - 1;
		table.addAttribute("IDs of active members", "active_member_ids", AttributeTypes.character);
		activeMemberIdsCN = table.getAttrCount() - 1;
		table.addAttribute("Spatial extent", "extent", AttributeTypes.real);
		int extentCN = table.getAttrCount() - 1;
		for (int i = 0; i < geoObj.size(); i++)
			if (geoObj.elementAt(i) instanceof DAggregateObject) {
				DAggregateObject aggr = (DAggregateObject) geoObj.elementAt(i);
				DataRecord rec = new DataRecord(aggr.getIdentifier());
				table.addDataRecord(rec);
				if (startTimeCN >= 0 && !aggr.isPersistent()) {
					TimeReference tref = aggr.getTimeReference();
					if (tref.getValidFrom() != null && tref.getValidUntil() != null) {
						rec.setTimeReference(tref);
						rec.setAttrValue(tref.getValidFrom(), startTimeCN);
						rec.setAttrValue(tref.getValidUntil(), endTimeCN);
						long dur = tref.getValidUntil().subtract(tref.getValidFrom());
						rec.setNumericAttrValue(dur, String.valueOf(dur), durationCN);
					}
				}
				RealRectangle br = aggr.getBoundRect();
				if (br != null) {
					double d = GeoComp.distance(br.rx1, br.ry1, br.rx2, br.ry2, isGeographic);
					if (!Double.isNaN(d)) {
						rec.setNumericAttrValue(d, StringUtil.doubleToStr(d, 3), extentCN);
					}
				}
				int n = aggr.getMemberCount();
				rec.setNumericAttrValue(n, String.valueOf(n), nMembersCN);
				n = aggr.getActiveMemberCount();
				rec.setNumericAttrValue(n, String.valueOf(n), nActiveMembersCN);
				rec.setAttrValue(aggr.getMemberIds(), memberIdsCN);
				rec.setAttrValue(aggr.getActiveMemberIds(), activeMemberIdsCN);
				aggr.setThematicData(rec);
			}
		return table;
	}

	/**
	 * Returns the reference to the layer with the original objects (aggregate members).
	 */
	@Override
	public DGeoLayer getSourceLayer() {
		return souLayer;
	}

	/**
	 * Returns a reference to a container (e.g. map layer) of the aggregate members.
	 * This is the souLayer.
	 */
	@Override
	public ObjectContainer getMemberContainer() {
		return souLayer;
	}

	/**
	 * Sets a reference to the layer with the original objects (aggregate members).
	 * Starts listening to changes of the layer filter.
	 */
	@Override
	public void setSourceLayer(DGeoLayer souLayer) {
		if (this.souLayer != null) {
			this.souLayer.removePropertyChangeListener(this);
		}
		this.souLayer = souLayer;
		if (souLayer != null) {
			souLayer.addPropertyChangeListener(this);
		}
	}

	/**
	 * Used for extending the filtering of the aggregates by their members also
	 * to the table describing the properties of the aggregates
	 */
	protected ObjectFilterBySelection aggTblSelFilter = null;

	/**
	 * Finds or constructs a filter to be used for extending the filtering of the
	 * aggregates by their members also to the table describing the properties of
	 * the aggregates
	 */
	protected boolean getFilterBySelection() {
		if (aggTblSelFilter != null)
			return true;
		if (dTable == null || dTable.getDataItemCount() < 1)
			return false;
		aggTblSelFilter = new ObjectFilterBySelection();
		aggTblSelFilter.setObjectContainer((DataTable) dTable);
		aggTblSelFilter.setEntitySetIdentifier(dTable.getEntitySetIdentifier());
		((DataTable) dTable).setObjectFilter(aggTblSelFilter);
		return true;
	}

	/**
	 * Finds out which source objects (members of aggregates) are currently active according
	 * to the filter of the layer with the objects. Returns true if the active objects
	 * have really changed
	 */
	protected boolean findActiveMembers() {
		if (souLayer == null)
			return false;
		int nObj = souLayer.getObjectCount();
		if (nObj < 1)
			return false;
		boolean changed = false;
		if (active == null) {
			active = new boolean[nObj];
			for (int i = 0; i < nObj; i++) {
				active[i] = true;
			}
		}
		for (int i = 0; i < active.length; i++)
			if (active[i] != souLayer.isObjectActive(i)) {
				changed = true;
				active[i] = !active[i];
				String id = souLayer.getObjectId(i);
				for (int j = 0; j < geoObj.size(); j++)
					if (geoObj.elementAt(j) instanceof DAggregateObject) {
						DAggregateObject aggr = (DAggregateObject) geoObj.elementAt(j);
						aggr.setMemberIsActive(id, active[i]);
					}
			}
		if (changed && getFilterBySelection()) {
			IntArray activeAggrIdxs = new IntArray(geoObj.size(), 1);
			for (int j = 0; j < geoObj.size(); j++)
				if (geoObj.elementAt(j) instanceof DAggregateObject) {
					DAggregateObject aggr = (DAggregateObject) geoObj.elementAt(j);
					if (aggr.getActiveMemberCount() > 0) {
						activeAggrIdxs.addElement(j);
					}
				}
			if (activeAggrIdxs.size() == geoObj.size()) {
				aggTblSelFilter.clearFilter();
			} else {
				aggTblSelFilter.setActiveObjectIndexes(activeAggrIdxs);
			}
		}
		return changed;
	}

	/**
	 * Reacts to filtering of the members of the aggregates
	 */
	protected void reactToMemberFilter() {
		if (dTable == null || nActiveMembersCN < 0)
			return;
		TimeFilter tf = getTimeFilter();
		TimeMoment t1 = null, t2 = null;
		boolean timeFiltered = false;
		if (tf != null) {
			t1 = tf.getFilterPeriodStart();
			t2 = tf.getFilterPeriodEnd();
			timeFiltered = t1 != null || t2 != null;
		}
		tStart = (t1 == null) ? null : t1.getCopy();
		tEnd = (t2 == null) ? null : t2.getCopy();
		TimeReference timeLimits = null;
		if (timeFiltered) {
			timeLimits = new TimeReference();
			timeLimits.setValidFrom(tStart);
			timeLimits.setValidUntil(tEnd);
		}
		for (int i = 0; i < geoObj.size(); i++)
			if (geoObj.elementAt(i) instanceof DAggregateObject) {
				DAggregateObject aggr = (DAggregateObject) geoObj.elementAt(i);
				if (aggr.getData() == null || !(aggr.getData() instanceof DataRecord)) {
					continue;
				}
				DataRecord rec = (DataRecord) aggr.getData();
				int n = aggr.getActiveMemberCount();
				rec.setNumericAttrValue(n, String.valueOf(n), nActiveMembersCN);
				rec.setAttrValue(aggr.getActiveMemberIds(), activeMemberIdsCN);
			}
		Vector attrIds = new Vector(30, 10);
		attrIds.addElement(dTable.getAttributeId(nActiveMembersCN));
		attrIds.addElement(dTable.getAttributeId(activeMemberIdsCN));
		dTable.notifyPropertyChange("values", null, attrIds);
	}

	@Override
	public void draw(Graphics g, MapContext mc) {
		if (geoObj == null || geoObj.size() < 1 || g == null || mc == null)
			return;
		if (neverDrawn) {
			neverDrawn = false;
			findActiveMembers();
			reactToMemberFilter();
			return;
		}
		boolean timeFilterSame = false;
		TimeFilter tf = getTimeFilter();
		if (tf == null) {
			timeFilterSame = tStart == null && tEnd == null;
		} else {
			TimeMoment t1 = tf.getFilterPeriodStart(), t2 = tf.getFilterPeriodEnd();
			timeFilterSame = ((t1 == null && tStart == null) || (t1 != null && tStart != null && t1.equals(tStart))) && ((t2 == null && tEnd == null) || (t2 != null && tEnd != null && t2.equals(tEnd)));
		}
		if (!timeFilterSame) {
			reactToMemberFilter();
			return;
		}
		super.draw(g, mc);
	}

	@Override
	public boolean areObjectsFiltered() {
		if (souLayer != null && souLayer.areObjectsFiltered())
			return true;
		return super.areObjectsFiltered();
	}

	/**
	* Determines whether the object with the given index is active, i.e. not
	* filtered out.
	*/
	@Override
	public boolean isObjectActive(int idx) {
		if (idx < 0 || geoObj == null || idx >= geoObj.size())
			return false;
		if (!(geoObj.elementAt(idx) instanceof DAggregateObject))
			return false;
		DAggregateObject aggr = (DAggregateObject) geoObj.elementAt(idx);
		//if (aggr.getActiveMemberCount()<1) return false;
		if (!aggr.allMembersActive())
			return false;
		return super.isObjectActive(idx);
	}

	/**
	 * Reacts to changes of the filter of the trajectory layer(s)
	 */
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource().equals(supervisor) && souLayer != null && geoObj != null && pce.getPropertyName().equals(Supervisor.eventObjectColors) && pce.getNewValue().equals(souLayer.getEntitySetIdentifier())) {
			//change the colors of the members
			ObjectColorer objectColorer = supervisor.getObjectColorer();
			if (objectColorer != null && !objectColorer.getEntitySetIdentifier().equals(souLayer.getEntitySetIdentifier())) {
				objectColorer = null;
			}
			for (int i = 0; i < geoObj.size(); i++)
				if (geoObj.elementAt(i) instanceof DAggregateObject) {
					DAggregateObject agg = (DAggregateObject) geoObj.elementAt(i);
					for (int j = 0; j < agg.getMemberCount(); j++) {
						AggregateMemberObject member = agg.getMember(j);
						if (objectColorer == null) {
							member.color = agg.giveMemberColor(j);
						} else {
							member.color = objectColorer.getColorForObject(member.obj.getIdentifier());
						}
					}
				}
			notifyPropertyChange("VisParameters", null, this);
			return;
		}
		if (!pce.getSource().equals(souLayer)) {
			super.propertyChange(pce);
			return;
		}
		if (pce.getPropertyName().equals("ObjectFilter")) {
			if (dTable != null && findActiveMembers()) {
				reactToMemberFilter();
				notifyPropertyChange("ObjectData", null, null);
			}
		}
	}

	/**
	* Returns a copy of this GeoLayer. The reference to the visualizer is also copied.
	*/
	@Override
	public GeoLayer makeCopy() {
		DAggregateLayer layer = new DAggregateLayer();
		copyTo(layer);
		layer.setSourceLayer(souLayer);
		return layer;
	}

	/**
	 * Among the given copies of layers, looks for the copies of the
	 * layers this layer is linked to (by their identifiers) and replaces
	 * the references to the original layers by the references to their copies.
	 */
	public void checkAndCorrectLinks(Vector copiesOfMapLayers) {
		if (souLayer == null || copiesOfMapLayers == null || copiesOfMapLayers.size() < 1)
			return;
		for (int i = 0; i < copiesOfMapLayers.size(); i++)
			if (copiesOfMapLayers.elementAt(i) instanceof DGeoLayer) {
				DGeoLayer layer = (DGeoLayer) copiesOfMapLayers.elementAt(i);
				if (layer.getEntitySetIdentifier().equals(souLayer.getEntitySetIdentifier())) {
					setSourceLayer(layer);
					break;
				}
			}
	}

	/**
	 * Removes itself from listeners of the filtering events of the layers
	 * with the trajectories
	 */
	@Override
	public void destroy() {
		if (destroyed)
			return;
		if (aggrShowWin != null) {
			aggrShowWin.dispose();
			aggrShowWin = null;
			lastShownAggregates = null;
		}
		if (souLayer != null) {
			souLayer.removePropertyChangeListener(this);
		}
		if (supervisor != null) {
			supervisor.removeObjectEventReceiver(this);
			supervisor.removePropertyChangeListener(this);
		}
		if (aggTblSelFilter != null) {
			((DataTable) dTable).removeObjectFilter(aggTblSelFilter);
			aggTblSelFilter.destroy();
			aggTblSelFilter = null;
		}
		super.destroy();
	}

	/**
	* Returns its time filter, if available
	*/
	@Override
	public TimeFilter getTimeFilter() {
		TimeFilter tf = null;
		if (souLayer != null) {
			tf = souLayer.getTimeFilter();
		}
		if (tf != null)
			return tf;
		return super.getTimeFilter();
	}

	@Override
	protected boolean drawContoursOfInactiveObjects() {
		return false;
	}

//------------------ EventReceiver interface ------------------------------------
	/**
	* The EventReceiver answers whether it is interested in getting the specified
	* kind of events.
	*/
	@Override
	public boolean doesListenToEvent(String eventId) {
		return eventId != null && eventId.equals(ObjectEvent.select);
	}

	/**
	* This method is used for delivering events to the Event Receiver.
	*/
	@Override
	public void eventOccurred(DEvent evt) {
		if ((evt instanceof ObjectEvent) && evt.getSourceMouseEvent() != null && evt.getId().equals(ObjectEvent.select)) {
			ObjectEvent oe = (ObjectEvent) evt;
			Vector obj = oe.getAffectedObjects();
			if (obj == null || obj.size() < 1)
				return;
			if (oe.getSetIdentifier() == null || !oe.getSetIdentifier().equals(dTable.getEntitySetIdentifier()))
				return;
			MouseEvent me = evt.getSourceMouseEvent();
			Component comp = me.getComponent();
			Point pt = comp.getLocationOnScreen();
			pt.x += me.getX();
			pt.y += me.getY();
			showAggregates(obj, pt);
		}
	}

	/**
	* Returns a unique identifier of the event receiver (may be produced
	* automatically, used only internally, not shown to the user).
	*/
	@Override
	public String getIdentifier() {
		return getContainerIdentifier();
	}

	/**
	* Sets the Supervisor from which the layer can receive object events
	*/
	public void setSupervisor(Supervisor sup) {
		supervisor = sup;
		if (supervisor != null) {
			supervisor.registerObjectEventReceiver(this);
			supervisor.addPropertyChangeListener(this);
		}
	}

	/**
	 * Used to show information about the members of a selected aggregate
	 */
	protected OKFrame aggrShowWin = null;
	/**
	 * Additional attributes of the members to show
	 */
	protected Vector memberAttrIds = null;
	/**
	 * Latest shown aggregates
	 */
	protected Vector lastShownAggregates = null;

	/**
	 * Constructs and displays a window showing characteristics of the aggregate members
	 */
	public void showAggregates(Vector aggrIds, Point position) {
		if (aggrIds == null || aggrIds.size() < 1)
			return;
		if (aggrShowWin != null) {
			aggrShowWin.dispose();
			aggrShowWin = null;
			lastShownAggregates = null;
		}
		Vector aObj = new Vector(aggrIds.size(), 1);
		for (int i = 0; i < aggrIds.size(); i++) {
			DAggregateObject aggr = (DAggregateObject) findObjectById(aggrIds.elementAt(i).toString());
			if (aggr != null) {
				aObj.addElement(aggr);
			}
		}
		if (aObj.size() < 1)
			return;
		if (aObj.size() > 2) {
			supervisor.getUI().showMessage("Wait... Preparing a display of the members of the aggregate objects.");
		}
		if (aObj.size() > 1) {
			DGeoObject.sortGeoObjectsByTimes(aObj);
		}
		lastShownAggregates = aggrIds;
		AttributeDataPortion mtbl = null;
		if (souLayer != null) {
			mtbl = souLayer.getThematicData();
		}
		boolean addAttributes = mtbl != null && memberAttrIds != null && memberAttrIds.size() > 0;
		GridBagLayout gridbag = new GridBagLayout();
		Panel p = new Panel(gridbag);
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0f;
		c.weighty = 1.0f;
		c.fill = GridBagConstraints.HORIZONTAL;
		int naggr = 0;
		for (int i = 0; i < aObj.size(); i++) {
			DAggregateObject aggr = (DAggregateObject) aObj.elementAt(i);
			if (naggr > 0) {
				Line line = new Line(false);
				c.gridwidth = GridBagConstraints.REMAINDER;
				gridbag.setConstraints(line, c);
				p.add(line);
			}
			++naggr;
			String str = aggr.getIdentifier();
			if (!aggr.persistent) {
				TimeReference tr = aggr.getTimeReference();
				if (tr != null) {
					str += ": [" + tr.getValidFrom().toString() + " - " + tr.getValidUntil().toString() + "]";
				}
			}
			Label l = new Label(str);
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(l, c);
			p.add(l);
			int nMembers = aggr.getMemberCount();
			if (nMembers < 1) {
				continue;
			}
			l = new Label("");
			c.gridwidth = 1;
			gridbag.setConstraints(l, c);
			p.add(l);
			l = new Label("Id");
			c.gridwidth = 2;
			gridbag.setConstraints(l, c);
			p.add(l);
			l = new Label("Name");
			gridbag.setConstraints(l, c);
			p.add(l);
			/*
			l=new Label("Active?");
			gridbag.setConstraints(l,c);
			p.add(l);
			*/
			l = new Label("Earliest time");
			gridbag.setConstraints(l, c);
			p.add(l);
			l = new Label("Latest time");
			if (!addAttributes) {
				c.gridwidth = GridBagConstraints.REMAINDER;
			}
			gridbag.setConstraints(l, c);
			p.add(l);
			if (addAttributes) {
				for (int j = 0; j < memberAttrIds.size(); j++) {
					l = new Label(mtbl.getAttributeName((String) memberAttrIds.elementAt(j)));
					if (j == memberAttrIds.size() - 1) {
						c.gridwidth = GridBagConstraints.REMAINDER;
					}
					gridbag.setConstraints(l, c);
					p.add(l);
				}
			}
			for (int j = 0; j < nMembers; j++) {
				AggregateMemberObject member = aggr.getMember(j);
				l = new Label("");
				if (member.active && member.color != null && aggr.doesGiveColorsToMembers()) {
					l.setBackground(member.color);
				}
				c.gridwidth = 1;
				gridbag.setConstraints(l, c);
				p.add(l);
				l = new Label(member.obj.getIdentifier());
				c.gridwidth = 2;
				gridbag.setConstraints(l, c);
				p.add(l);
				str = member.obj.getName();
				if (str == null) {
					str = "";
				}
				l = new Label(str);
				gridbag.setConstraints(l, c);
				p.add(l);
				/*
				str=(member.active)?"yes":"no";
				l=new Label(str);
				gridbag.setConstraints(l,c);
				p.add(l);
				*/
				str = "";
				if (member.enterTime != null) {
					str = member.enterTime.toString();
				}
				l = new Label(str);
				gridbag.setConstraints(l, c);
				p.add(l);
				str = "";
				if (member.exitTime != null) {
					str = member.exitTime.toString();
				}
				l = new Label(str);
				if (!addAttributes) {
					c.gridwidth = GridBagConstraints.REMAINDER;
				}
				gridbag.setConstraints(l, c);
				p.add(l);
				if (addAttributes) {
					ThematicDataItem rec = member.obj.getData();
					if (rec == null) {
						l = new Label("");
						c.gridwidth = GridBagConstraints.REMAINDER;
						gridbag.setConstraints(l, c);
						p.add(l);
					} else {
						for (int k = 0; k < memberAttrIds.size(); k++) {
							str = rec.getAttrValueAsString((String) memberAttrIds.elementAt(k));
							l = new Label(str);
							if (k == memberAttrIds.size() - 1) {
								c.gridwidth = GridBagConstraints.REMAINDER;
							}
							gridbag.setConstraints(l, c);
							p.add(l);
						}
					}
				}
			}
		}
		if (naggr < 1)
			return;

		if (mtbl != null) {
			Button b = new Button("Attributes");
			b.setActionCommand("member_attributes");
			b.addActionListener(this);
			Panel pp = new Panel(new FlowLayout(FlowLayout.RIGHT, 10, 2));
			pp.add(b);
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(pp, c);
			p.add(pp);
		}

		aggrShowWin = new OKFrame(this, getName(), false);
		aggrShowWin.addContent(p);
		aggrShowWin.pack();
		Dimension ss = Toolkit.getDefaultToolkit().getScreenSize(), ws = aggrShowWin.getSize();
		if (ws.width > ss.width / 2 || ws.height > ss.height / 2) {
			aggrShowWin.remove(p);
			ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
			scp.add(p);
			aggrShowWin.addContent(scp);
			int w = Math.min(ws.width, ss.width / 2) + scp.getVScrollbarWidth() + 10, h = Math.min(ws.height, ss.height / 2) + scp.getHScrollbarHeight() + 10;
			aggrShowWin.setSize(w, h);
		}
		if (position != null) {
			aggrShowWin.setLocation(position.x, position.y);
		} else {
			ws = aggrShowWin.getSize();
			aggrShowWin.setLocation((ss.width - ws.width) / 2, (ss.height - ws.height) / 2);
		}
		aggrShowWin.setVisible(true);
		if (aObj.size() > 2) {
			supervisor.getUI().showMessage(null);
		}
	}

	/**
	 * Reacts to the window being closed
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(aggrShowWin)) {
			if (e.getActionCommand().equalsIgnoreCase("closed")) {
				aggrShowWin.dispose();
				aggrShowWin = null;
				lastShownAggregates = null;
			}
		} else if (e.getActionCommand() != null && e.getActionCommand().equals("member_attributes")) {
			AttributeChooser attrChooser = new AttributeChooser();
			Vector memberAttrs = attrChooser.selectColumns(souLayer.getThematicData(), memberAttrIds, null, false, "Select attributes:", supervisor.getUI());
			if (memberAttrs == null || memberAttrs.size() < 1) {
				memberAttrIds = null;
			} else {
				memberAttrIds = new Vector(memberAttrs.size(), 1);
				for (int i = 0; i < memberAttrs.size(); i++) {
					Attribute attr = (Attribute) memberAttrs.elementAt(i);
					memberAttrIds.addElement(attr.getIdentifier());
				}
			}
			Point pos = null;
			if (aggrShowWin != null) {
				pos = aggrShowWin.getLocationOnScreen();
			}
			aggrShowWin.dispose();
			aggrShowWin = null;
			showAggregates(lastShownAggregates, pos);
		} else {
			super.actionPerformed(e);
		}
	}

	/**
	 * Returns the aggregates - instances of Aggregate
	 */
	@Override
	public Vector getAggregates() {
		return geoObj;
	}
}
