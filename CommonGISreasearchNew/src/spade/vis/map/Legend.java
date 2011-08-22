package spade.vis.map;

import java.awt.Graphics;
import java.beans.PropertyChangeListener;

import spade.vis.event.EventReceiver;

/**
* A Legend registers any items implementing the interface LegendDrawer.
* If the Legend interface is implemented by a drawing component,
* then in the method "paint" this component commands its LegendDrawers to
* paint themselves in the canvas one below another in the order they come
* in the list.
* A Legend may listen to changes of properties of LegendDrawers that
* require legend redrawing. For this purpose it extends the
* PropertyChangeListener interface.
*/
public interface Legend extends PropertyChangeListener {
	public void addLegendDrawer(LegendDrawer ldr);

	public void removeLegendDrawer(LegendDrawer ldr);

	public void redraw();

	public Graphics getLegendGraphics();

	public void addMouseEventReceiver(EventReceiver eres);

	public void removeMouseEventReceiver(EventReceiver eres);
}
