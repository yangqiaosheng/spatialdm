package de.fraunhofer.iais.spatial.entity;

import java.util.Date;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Entity mapped to the table FLICKR_DE_WEST_TABLE and FLICKR_DE_WEST_TABLE_GEOM
 * @author zhi
 */

public class FlickrPhoto {

	private int index;
	private long id;
	private FlickrPolygon area;
	private double longitude;
	private double latitude;
	private Date date;
	private String personId;
	private int viewed;
	private String title;
	private String rawTags;
	private String smallUrl;

//	@Override
//	public String toString() {
//		return super.toString()
//				+ "\tindex:" + this.getIndex()
//				+ "\tPHOTO_ID:" + this.getId()
//				+ "\tAreaid:" + this.getArea().getId()
//				+ "\tRadius:" + this.getArea().getRadius()
//				+ "\tDT:" + this.getDate()
//				+ "\tLATITUDE:" + this.getLatitude()
//				+ "\tLONGITUDE:" + this.getLongitude()
//				+ "\tPERSON:" + this.getPersonId()
//				+ "\tRAWTAGS:" + this.getRawTags()
//				+ "\tTITLE:" + this.getTitle()
//				+ "\tSMALLURL:" + this.getSmallUrl()
//				+ "\tVIEWED:" + this.getViewed();
//	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).
	       append("index", index).
	       append("id", id).
	       append("area", area).
	       append("longitude", longitude).
	       append("latitude", latitude).
	       append("date", date).
	       append("personId", personId).
	       append("viewed", viewed).
	       append("title", title).
	       append("smallUrl", smallUrl).
	       append("rawTags", rawTags).
	       toString();
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public FlickrPolygon getArea() {
		return area;
	}

	public void setArea(FlickrPolygon area) {
		this.area = area;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getPersonId() {
		return personId;
	}

	public void setPersonId(String person) {
		this.personId = person;
	}

	public int getViewed() {
		return viewed;
	}

	public void setViewed(int viewed) {
		this.viewed = viewed;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getRawTags() {
		return rawTags;
	}

	public void setRawTags(String rawTags) {
		this.rawTags = rawTags;
	}

	public String getSmallUrl() {
		return smallUrl;
	}

	public void setSmallUrl(String smallUrl) {
		this.smallUrl = smallUrl;
	}
}
