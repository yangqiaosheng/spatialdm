package de.fraunhofer.iais.spatial.entity;

import java.util.Map;

import com.google.common.collect.Maps;

/**
 * Result of the query related to FlickrArea
 * @author haolin
 *
 */
public class AreaResult {

	private Area area;
	private long selectedCount;
	private Histograms histograms = new Histograms();

	public AreaResult(Area area) {
		this.area = area;
	}

	public Area getArea() {
		return area;
	}

	public void setArea(Area area) {
		this.area = area;
	}

	public long getSelectCount() {
		return selectedCount;
	}

	public void setSelectedCount(long selectCount) {
		this.selectedCount = selectCount;
	}

	public Histograms getHistograms() {
		return histograms;
	}

	public void setHistograms(Histograms histograms) {
		this.histograms = histograms;
	}

}
