package spade.lib.basicwin;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
* A MyCanvas does not paint by itself but notifies its PropertyChangeListener
* about the necessity to paint. The PropertyChangeListener then paints the
* canvas.
*/
public class MyCanvas extends Canvas {
	/**
	* The preferred size of the canvas
	*/
	protected Dimension prefSize = new Dimension(50, 50);
	/**
	* The object that paints the canvas when it is notified about the necessity
	* to paint
	*/
	protected PropertyChangeListener painter = null;

	/**
	* Sets the preferred size of the canvas
	*/
	public void setPreferredSize(int width, int height) {
		prefSize.width = width;
		prefSize.height = height;
	}

	/**
	* Returns the preferred size of the canvas
	*/
	@Override
	public Dimension getPreferredSize() {
		return prefSize;
	}

	/**
	* Sets a reference to the object that will paint the canvas when it is
	* notified about the necessity to paint
	*/
	public void setPainter(PropertyChangeListener painter) {
		this.painter = painter;
	}

	/**
	* Instead of painting itself, notifies its painter about the necessity to
	* paint by sending a property change event. The "new value" of the event
	* contains the graphics of this canvas.
	*/
	@Override
	public void paint(Graphics g) {
		if (painter != null) {
			painter.propertyChange(new PropertyChangeEvent(this, "must_paint", null, g));
		}
	}
}