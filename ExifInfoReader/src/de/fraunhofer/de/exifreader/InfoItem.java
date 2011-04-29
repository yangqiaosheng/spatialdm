package de.fraunhofer.de.exifreader;

import java.io.File;
import java.util.Date;

public class InfoItem {

	private Date createDate;
	private double latitude;
	private double longitude;
	private File file;

	public InfoItem(Date createDate, double latitude, double longitude, File file) {
		this.createDate = createDate;
		this.longitude = longitude;
		this.latitude = latitude;
		this.file = file;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}


}
