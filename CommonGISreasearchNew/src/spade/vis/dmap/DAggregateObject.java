package spade.vis.dmap;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.ImageObserver;
import java.util.Vector;
import java2d.Drawing2D;

import spade.analysis.aggregates.Aggregate;
import spade.analysis.aggregates.AggregateMember;
import spade.analysis.tools.moves.InteractionData;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.vis.database.SpatialDataItem;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapContext;
import spade.vis.space.GeoObject;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 16, 2008
 * Time: 11:16:25 AM
 * An aggregate object includes several member objects (DGeoObjects).
 * Each member may have its own times of entering the aggregate and exiting it.
 * The aggregate itself may have its life time (start and end of existence).
 * The aggregate reacts to filtering and classification (colouring) applied
 * to its members. In particular, when all members are inactive, the aggregate
 * is hidden.
 */
public class DAggregateObject extends DGeoObject implements Aggregate {
	/**
	 * The members of the aggregate; described by instances of AggregateMember
	 */
	protected Vector members = null;
	/**
	 * Whether the aggregate object must draw the members when it is drawn
	 * (by default true)
	 */
	protected boolean drawMembers = true;
	/**
	 * Whether the aggregate object must fill the contours of the members when
	 * the members are drawn (by default false)
	 */
	protected boolean fillMembers = false;
	/**
	 * Whether the aggregate object must give different colors to the members when
	 * the members are drawn (by default true); otherwise, the members will have
	 * the same color as assigned to the aggregate object.
	 */
	protected boolean giveColorsToMembers = true;
	/**
	 * May contain additional information about the aggregate.
	 * In particular, this may be an instance of InteractionData, if this aggregate
	 * represents an interaction.
	 */
	protected Object extraInfo = null;
	/**
	 * Indicates whether the aggregate is persistent, i.e. its life is not
	 * limited in time.
	 */
	protected boolean persistent = false;

	/**
	 * Adds a new aggregate member. If the member is always present in the aggregate,
	 * enterTime and exitTime may be null.
	 */
	public void addMember(DGeoObject obj, TimeMoment enterTime, TimeMoment exitTime) {
		if (obj == null)
			return;
		AggregateMemberObject member = new AggregateMemberObject();
		member.obj = obj;
		TimeReference tref = data.getTimeReference();
		if (enterTime != null && exitTime != null) {
			member.enterTime = enterTime.getCopy();
			member.exitTime = exitTime.getCopy();
			member.validPart = (DGeoObject) obj.getObjectCopyForTimeInterval(member.enterTime, member.exitTime);
			if (!persistent) {
				if (tref == null) {
					tref = new TimeReference();
					data.setTimeReference(tref);
				}
				if (tref.getValidFrom() == null || tref.getValidFrom().compareTo(member.enterTime) > 0) {
					tref.setValidFrom(member.enterTime);
				}
				if (tref.getValidUntil() == null || tref.getValidUntil().compareTo(member.exitTime) < 0) {
					tref.setValidUntil(member.exitTime);
				}
			}
		} else {
			persistent = true;
			if (tref != null) {
				data.setTimeReference(null);
			}
		}
		if (members == null) {
			members = new Vector(10, 10);
		}
		members.addElement(member);
	}

	/**
	 * Returns the additional information about the aggregate, if available.
	 * In particular, this may be an instance of InteractionData, if this aggregate
	 * represents an interaction.
	 */
	public Object getExtraInfo() {
		return extraInfo;
	}

	/**
	 * Attaches additional information about the aggregate.
	 * In particular, this may be an instance of InteractionData, if this aggregate
	 * represents an interaction.
	 */
	public void setExtraInfo(Object extraInfo) {
		this.extraInfo = extraInfo;
	}

	public boolean isPersistent() {
		return persistent;
	}

	/**
	 * Returns the total number of members
	 */
	@Override
	public int getMemberCount() {
		if (members == null)
			return 0;
		return members.size();
	}

	/**
	 * Returns the number of active members, i.e. which are not filtered out
	 */
	public int getActiveMemberCount() {
		if (members == null)
			return 0;
		int n = 0;
		for (int i = 0; i < members.size(); i++)
			if (((AggregateMemberObject) members.elementAt(i)).active) {
				++n;
			}
		return n;
	}

	/**
	 * Returns true if all members are active
	 */
	public boolean allMembersActive() {
		if (members == null)
			return false;
		for (int i = 0; i < members.size(); i++)
			if (!((AggregateMemberObject) members.elementAt(i)).active)
				return false;
		return true;
	}

	/**
	 * Returns the identifiers of all its members
	 */
	public String getMemberIds() {
		if (members == null || members.size() < 1)
			return null;
		String str = null;
		for (int i = 0; i < members.size(); i++) {
			AggregateMemberObject m = (AggregateMemberObject) members.elementAt(i);
			if (str == null) {
				str = m.obj.getIdentifier();
			} else {
				str += ";" + m.obj.getIdentifier();
			}
		}
		return str;
	}

	/**
	 * Returns the identifiers of its active members
	 */
	public String getActiveMemberIds() {
		if (members == null || members.size() < 1)
			return null;
		String str = null;
		for (int i = 0; i < members.size(); i++) {
			AggregateMemberObject m = (AggregateMemberObject) members.elementAt(i);
			if (m.active)
				if (str == null) {
					str = m.obj.getIdentifier();
				} else {
					str += ";" + m.obj.getIdentifier();
				}
		}
		return str;
	}

	/**
	 * If this aggregate contains a member with the given identifier,
	 * sets its state to either active or inactive
	 */
	public void setMemberIsActive(String memberId, boolean active) {
		if (members == null || members.size() < 1)
			return;
		for (int i = 0; i < members.size(); i++) {
			AggregateMemberObject m = (AggregateMemberObject) members.elementAt(i);
			if (m.obj.getIdentifier().equals(memberId)) {
				m.active = active;
			}
		}
	}

	public AggregateMemberObject getMember(int idx) {
		if (members == null || idx < 0 || idx >= members.size())
			return null;
		return (AggregateMemberObject) members.elementAt(idx);
	}

	/**
	 * Returns the identifier of the member with the given index
	 */
	@Override
	public String getMemberId(int idx) {
		if (members == null || idx < 0 || idx >= members.size())
			return null;
		return ((AggregateMemberObject) members.elementAt(idx)).obj.getIdentifier();
	}

	/**
	 * Returns the members of this aggregate represented by instances of the class AggregateMember
	 */
	@Override
	public Vector getAggregateMembers() {
		if (members == null || members.size() < 1)
			return null;
		Vector m = new Vector(members.size(), 1);
		for (int i = 0; i < members.size(); i++) {
			AggregateMemberObject memObj = (AggregateMemberObject) members.elementAt(i);
			AggregateMember memb = new AggregateMember();
			memb.id = memObj.obj.getIdentifier();
			memb.enterTime = memObj.enterTime;
			memb.exitTime = memObj.exitTime;
			m.addElement(memb);
		}
		return m;
	}

	/**
	* Returns a copy of this object. The spatial and thematic data items
	* used by this objects are not duplicated; the copy will use references to the
	* same data items
	*/
	@Override
	public GeoObject makeCopy() {
		DAggregateObject obj = new DAggregateObject();
		if (data != null) {
			obj.setup((SpatialDataItem) data.clone());
		}
		if (members != null) {
			obj.members = (Vector) members.clone();
		}
		obj.label = label;
		obj.highlighted = highlighted;
		obj.selected = selected;
		obj.persistent = persistent;
		return obj;
	}

	/**
	 * Whether the aggregate object must draw the members when it is drawn
	 * (by default true)
	 */
	public void setDrawMembers(boolean drawMembers) {
		this.drawMembers = drawMembers;
	}

	/**
	 * Whether the aggregate object must fill the contours of the members when
	 * the members are drawn (by default false)
	 */
	public void setFillMembers(boolean fillMembers) {
		this.fillMembers = fillMembers;
	}

	/**
	 * Whether the aggregate object must give different colors to the members when
	 * the members are drawn (by default true); otherwise, the members will have
	 * the same color as assigned to the aggregate object.
	 */
	public void setGiveColorsToMembers(boolean giveColorsToMembers) {
		this.giveColorsToMembers = giveColorsToMembers;
	}

	public boolean doesGiveColorsToMembers() {
		return giveColorsToMembers;
	}

	protected static Color colors[] = { Color.red, Color.blue, Color.magenta, Color.orange, Color.green, Color.yellow, Color.pink };

	/**
	 * Assigns a color to the member with the given index
	 * @param memberIdx - member index
	 * @return the color assigned
	 */
	public Color giveMemberColor(int memberIdx) {
		if (memberIdx >= colors.length * 2)
			return Color.getHSBColor((float) Math.random(), (float) Math.random(), (float) Math.random());
		Color c = colors[memberIdx % colors.length];
		if (memberIdx >= colors.length) {
			c = c.darker();
		}
		return c;
	}

	/**
	 * Returns the current color of the member with the given index
	 * @param memberIdx - member index
	 * @return current color of this member
	 */
	public Color getMemberColor(int memberIdx) {
		AggregateMemberObject member = getMember(memberIdx);
		if (member == null)
			return null;
		return member.color;
	}

	public RealRectangle getBoundRect() {
		if (extraInfo != null)
			if (extraInfo instanceof InteractionData) {
				InteractionData inter = (InteractionData) extraInfo;
				if (inter.points != null && inter.points.size() > 0) {
					RealPoint p = (RealPoint) inter.points.elementAt(0);
					RealRectangle br = new RealRectangle(p.x, p.y, p.x, p.y);
					for (int i = 1; i < inter.points.size(); i++) {
						p = (RealPoint) inter.points.elementAt(i);
						if (p.x < br.rx1) {
							br.rx1 = p.x;
						} else if (p.x > br.rx2) {
							br.rx2 = p.x;
						}
						if (p.y < br.ry1) {
							br.ry1 = p.y;
						} else if (p.y > br.ry2) {
							br.ry2 = p.y;
						}
					}
					return br;
				}
			}
		Geometry geom = getGeometry();
		if (geom == null)
			return null;
		float b[] = geom.getBoundRect();
		if (b == null)
			return null;
		return new RealRectangle(b);
	}

	@Override
	public void drawGeometry(Geometry geom, Graphics g, MapContext mc, Color borderColor, Color fillColor, int width, ImageObserver observer) {
		if (g == null)
			return;
		super.drawGeometry(geom, g, mc, borderColor, fillColor, width, observer);
		if (members == null)
			return;
		int transp = 0, lw = 1;
		Color lc = Color.darkGray;
		if (drawParam != null) {
			lw = drawParam.lineWidth;
			transp = drawParam.transparency;
			lc = drawParam.lineColor;
			if (lc == null) {
				lc = Color.darkGray;
			}
		}
		if (extraInfo != null)
			if (extraInfo instanceof InteractionData) {
				InteractionData inter = (InteractionData) extraInfo;
				if (inter.points != null && inter.links != null && inter.links.size() > 0) {
					g.setColor(lc);
					for (int i = 0; i < inter.links.size(); i++) {
						int link[] = (int[]) inter.links.elementAt(i);
						if (link[0] >= 0 && link[0] < inter.points.size() && link[1] >= 0 && link[1] < inter.points.size()) {
							RealPoint pt0 = (RealPoint) inter.points.elementAt(link[0]);
							RealPoint pt1 = (RealPoint) inter.points.elementAt(link[1]);
							int x0 = getScreenX(mc, pt0.x, pt0.y), y0 = getScreenY(mc, pt0.x, pt0.y);
							int x1 = getScreenX(mc, pt1.x, pt1.y), y1 = getScreenY(mc, pt1.x, pt1.y);
							g.drawLine(x0, y0, x1, y1);
						}
					}
				}
			}
		if (drawMembers && (borderColor != null || fillColor != null || giveColorsToMembers)) {
			for (int i = 0; i < members.size(); i++) {
				AggregateMemberObject m = (AggregateMemberObject) members.elementAt(i);
				if (m.active) {
					if (giveColorsToMembers)
						if (m.color == null) {
							m.color = giveMemberColor(i);
						} else {
							;
						}
					else {
						m.color = (fillColor == null) ? borderColor : fillColor;
					}
					Color c = m.color;
					if (transp > 0) {
						c = Drawing2D.getTransparentColor(c, transp);
					}
					if (m.validPart != null) {
						m.validPart.drawGeometry(g, mc, c, (fillMembers) ? c : null, lw, null);
					} else {
						m.obj.drawGeometry(g, mc, c, (fillMembers) ? c : null, lw, null);
					}
				}
			}
		}
	}
}
