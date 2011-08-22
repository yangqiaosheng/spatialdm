//ID
package spade.lib.slider;

import java.util.EventListener;

public interface SliderListener extends EventListener {

	public void SliderCountChanged(SliderEvent e);

	public void SliderLimitsChanged(SliderEvent e);

	public void SliderDragged(SliderEvent e);

	public void SliderReleased(SliderEvent e);

	public void SliderHighlighted(SliderEvent e);

	public void SliderResized(SliderEvent e);

}
//~ID