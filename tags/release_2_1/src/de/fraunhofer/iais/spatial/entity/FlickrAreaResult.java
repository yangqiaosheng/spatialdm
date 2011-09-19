package de.fraunhofer.iais.spatial.entity;

import java.util.Map;

import com.google.common.collect.Maps;

/**
 * Result of the query related to FlickrArea
 * @author haolin
 *
 */
public class FlickrAreaResult {

	private FlickrArea area;
	private Map<String, Integer> tagsCount = Maps.newLinkedHashMap();
	private long selectedCount;
	private Histograms histograms = new Histograms();

	public FlickrAreaResult(FlickrArea area) {
		this.area = area;
	}

	public FlickrArea getArea() {
		return area;
	}

	public void setArea(FlickrArea area) {
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


	public Map<String, Integer> getTagsCount() {
		return tagsCount;
	}
}
