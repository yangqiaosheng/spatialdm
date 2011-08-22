package spade.lib.basicwin;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;

/**
* Draws connecting lines between UI components
*/
public class ConnectionCanvas extends Canvas {
	/**
	* The component providing information about connections to be shown
	*/
	protected ConnectingAgent connector = null;

	/**
	* Sets a reference to the component that will provide information about
	* connections to be shown.
	*/
	public void setConnectingAgent(ConnectingAgent cAgent) {
		connector = cAgent;
	}

	@Override
	public Dimension getPreferredSize() {
		if (connector != null)
			return connector.getConnectionCanvasPrefSize();
		return new Dimension(50, 10);
	}

	@Override
	public void paint(Graphics g) {
		draw(g);
	}

	public void draw(Graphics g) {
		if (g == null || connector == null || connector.getConnectionCount() < 1 || !isShowing())
			return;
		Dimension d = getSize();
		Point p = getLocationOnScreen();
		g.setColor(getForeground());
		for (int i = 0; i < connector.getConnectionCount(); i++) {
			int x1 = connector.getConnectionStartX(i) - p.x, y1 = connector.getConnectionStartY(i) - p.y, x2 = connector.getConnectionEndX(i) - p.x, y2 = connector.getConnectionEndY(i) - p.y;
			if (x1 < 0) {
				x1 = 0;
			} else if (x1 >= d.width) {
				x1 = d.width - 1;
			}
			if (y1 < 0) {
				y1 = 0;
			} else if (y1 >= d.height) {
				y1 = d.height - 1;
			}
			if (x2 < 0) {
				x2 = 0;
			} else if (x2 >= d.width) {
				x2 = d.width - 1;
			}
			if (y2 < 0) {
				y2 = 0;
			} else if (y2 >= d.height) {
				y2 = d.height - 1;
			}
			g.drawLine(x1, y1, x2, y2);
		}
	}

	public void redraw() {
		Graphics g = getGraphics();
		if (g == null)
			return;
		Dimension d = getSize();
		g.clearRect(0, 0, d.width, d.height);
		draw(g);
	}
}