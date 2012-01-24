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
	private int avg;
	private int min;
	private int max;
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


	public int getAvg() {
		return avg;
	}

	public void setAvg(int avg) {
		this.avg = avg;
	}

	public int getMin() {
		return min;
	}

	public void setMin(int min) {
		this.min = min;
	}

	public int getMax() {
		return max;
	}

	public void setMax(int max) {
		this.max = max;
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
