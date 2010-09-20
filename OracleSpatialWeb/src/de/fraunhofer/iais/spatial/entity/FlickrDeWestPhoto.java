package de.fraunhofer.iais.spatial.entity;

import java.util.Date;

/**
 * Entity mapped to the table FLICKR_DE_WEST_TABLE and FLICKR_DE_WEST_TABLE_GEOM
 * @author zhi
 */

public class FlickrDeWestPhoto {

	private long id;
	private FlickrDeWestArea area;
	private double longitude;
	private double latitude;
	private Date date;
	private String personId;
	private int viewed;
	private String title;
	private String rawTags;
	private String smallUrl;
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public FlickrDeWestArea getArea() {
		return area;
	}
	public void setArea(FlickrDeWestArea area) {
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
