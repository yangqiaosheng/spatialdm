//ID
package spade.lib.slider;

import java.util.EventObject;

public class SliderEvent extends EventObject {

	private int highlightedRange = -1;
	private float highlightedMin;
	private float highlightedMax;
	public int count = 0;

	public SliderEvent(Object source) {
		super(source);
	}

	public void setHighlightedRange(int highlightedRange) {
		this.highlightedRange = highlightedRange;
	}

	public int getHighlightedRange() {
		return highlightedRange;
	}

	public float getHighlightedMin() {
		return highlightedMin;
	}

	public float getHighlightedMax() {
		return highlightedMax;
	}

	public void setHighlightedMin(float highlightedMin) {
		this.highlightedMin = highlightedMin;
	}

	public void setHighlightedMax(float highlightedMax) {
		this.highlightedMax = highlightedMax;
	}

}
//~ID