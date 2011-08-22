package spade.analysis.classification;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.ScrollPane;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import spade.lib.basicwin.CManager;

public class ClassificationStatisticsCanvas extends Canvas implements PropertyChangeListener {
	protected Classifier classifier = null;
	protected int prevClassCount = 0;

	public ClassificationStatisticsCanvas(Classifier classifier) {
		this.classifier = classifier;
		classifier.addPropertyChangeListener(this);
	}

	@Override
	public Dimension getPreferredSize() {
		if (classifier == null)
			return new Dimension(0, 0);
		Image image = createImage(10, 10);
		if (image == null || image.getGraphics() == null) {
			image = CManager.getAnyFrame().createImage(10, 10);
		}
		if (image == null)
			return new Dimension(100, 100);
		Graphics g = image.getGraphics();
		if (g == null)
			return new Dimension(100, 100);
		int prefW = 100;
		Dimension size = getSize();
		if (size != null && size.width > 0) {
			prefW = size.width;
		}
		ScrollPane scp = CManager.getScrollPane(this);
		if (scp != null) {
			size = scp.getViewportSize();
			if (size != null && size.width > 0) {
				prefW = size.width - scp.getVScrollbarWidth() - 4;
			}
		}
		Rectangle r = classifier.drawClassStatistics(g, 0, 0, prefW);
		if (r != null)
			return new Dimension(r.width, r.height);
		return new Dimension(100, 100);
	}

	@Override
	public void paint(Graphics g) {
		classifier.drawClassStatistics(g, 0, 0, getSize().width);
		prevClassCount = classifier.getNClasses();
	}

	/**
	* Reacts to changes of the number of classes in the classifier: makes
	* the container redo the layout.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (!isShowing())
			return;
		if (e.getPropertyName().equals("classes") && classifier.getNClasses() != prevClassCount) {
			invalidate();
			CManager.validateAll(this);
		} else {
			repaint();
		}
	}
}
