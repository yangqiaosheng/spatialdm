package spade.analysis.tools.somlink;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

import spade.lib.basicwin.Metrics;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Oct 30, 2009
 * Time: 4:41:10 PM
 * Represents a SOM cell and displays information related to this cell.
 */
public class SOMCellView extends Canvas implements MouseListener {
	public static int frameWidth = 3;
	/**
	 * The x- and y-indexes of this cell in the matrix
	 */
	public int cellX = -1, cellY = -1;
	/**
	 * The linear index of this cell
	 */
	public int cellIdx = -1;
	/**
	 * Number of objects in the cell
	 */
	public int nObj = 0;
	/**
	 * The maximum number of objects per cell; used to compute the
	 * width of the frame or the size of the symbol showing the number
	 * of objects
	 */
	public int maxNObjPerCell = 0;
	/**
	 * The current color of this cell, which may be either an individual
	 * color of this cell or the color of a cluster this cell belongs
	 */
	public Color cellColor = null;
	/**
	 * The current (last) individual color of this cell
	 */
	public Color indivColor = null;
	/**
	 * The original color of this cell (coming from the SOM tool)
	 */
	public Color origColor = null;
	/**
	 * The distances to the neighbouring cells in the following order:
	 * N, W, S, E, NW, NE, SW, SE (compass directions)
	 */
	public double distances[] = null;
	/**
	 * Whether to show the distances to the neighbouring cells
	 */
	public boolean showDistances = true;
	/**
	 * The maximum distance, which is used for defining the colors or shades
	 * for showing the distances to the neighbours
	 */
	public double maxDistance = Double.NaN;
	/**
	 * The maximum distance to the prototype object within the cell
	 */
	public double maxDistToPrototype = Double.NaN;
	/**
	 * The average distance to the prototype object within the cell
	 */
	public double aveDistToPrototype = Double.NaN;
	/**
	 * An image to be shown in the cell
	 */
	public BufferedImage image = null;
	/**
	 * Whether to draw the image
	 */
	public boolean drawImage = true;
	/**
	 * An "index" image of this cell indicating which objects it includes
	 */
	public BufferedImage indexImage = null;
	/**
	 * Whether to draw the "index" image of this cell indicating which objects it includes
	 */
	public boolean drawIndexImage = true;
	/**
	 * Whether to show the statistics
	 */
	public boolean showStatistics = true;
	/**
	 * The cluster label (may be absent)
	 */
	public String clusterLabel = null;
	/**
	 * The owner is notified about mouse clicks in this component
	 */
	protected ActionListener owner = null;

	@Override
	public Dimension getPreferredSize() {
		int mm = Metrics.mm();
		return new Dimension(40 * mm, 40 * mm);
	}

	/**
	 * The owner is notified about mouse clicks in this component
	 */
	public void setOwner(ActionListener owner) {
		this.owner = owner;
		if (owner != null) {
			addMouseListener(this);
		}
	}

	@Override
	public void paint(Graphics g) {
		Dimension size = getSize();
		if (size == null)
			return;
		if (cellColor != null/* && nObj>0*/) {
			g.setColor(cellColor);
			g.fillRect(0, 0, size.width + 1, size.height + 1);
		}
		if (size.width < 10 || size.height < 10)
			return;
		int x0 = 0, y0 = 0, w = size.width, h = size.height, x1 = w, y1 = h;
		double maxDistToNeighbour = Double.NaN;
		int yOrig = y0;
		if (showDistances && distances != null && distances.length >= 4 && !Double.isNaN(maxDistance) && maxDistance > 0) {
			maxDistToNeighbour = 0;
			for (double distance : distances)
				if (distance > maxDistToNeighbour) {
					maxDistToNeighbour = distance;
				}
			//show the distances to the cell neighbours around the cell boundaries
			int w4 = w / 4, h4 = h / 4, w2 = w / 2, h2 = h / 2;
			//North
			g.setColor(Color.getHSBColor(0f, 0f, 1f - (float) (distances[0] / maxDistance)));
			g.fillRect(x0, y0, w + 1, frameWidth + 1);
			//West
			g.setColor(Color.getHSBColor(0f, 0f, 1f - (float) (distances[1] / maxDistance)));
			g.fillRect(x0, y0, frameWidth + 1, h + 1);
			//South
			g.setColor(Color.getHSBColor(0f, 0f, 1f - (float) (distances[2] / maxDistance)));
			g.fillRect(x0, y1 - frameWidth, w + 1, frameWidth + 1);
			//East
			g.setColor(Color.getHSBColor(0f, 0f, 1f - (float) (distances[3] / maxDistance)));
			g.fillRect(x1 - frameWidth, y0, frameWidth + 1, h + 1);
			if (distances.length >= 8) {
				//North-West
				g.setColor(Color.getHSBColor(0f, 0f, 1f - (float) (distances[4] / maxDistance)));
				g.fillRect(x0, y0, w4 + 1, frameWidth + 1);
				g.fillRect(x0, y0, frameWidth + 1, h4 + 1);
				//North-East
				g.setColor(Color.getHSBColor(0f, 0f, 1f - (float) (distances[5] / maxDistance)));
				g.fillRect(x1 - w4, y0, w4 + 1, frameWidth + 1);
				g.fillRect(x1 - frameWidth, y0, frameWidth + 1, h4 + 1);
				//South-West
				g.setColor(Color.getHSBColor(0f, 0f, 1f - (float) (distances[6] / maxDistance)));
				g.fillRect(x0, y1 - frameWidth, w4 + 1, frameWidth + 1);
				g.fillRect(x0, y1 - h4, frameWidth + 1, h4 + 1);
				//South-East
				g.setColor(Color.getHSBColor(0f, 0f, 1f - (float) (distances[7] / maxDistance)));
				g.fillRect(x1 - w4, y1 - frameWidth, w4 + 1, frameWidth + 1);
				g.fillRect(x1 - frameWidth, y1 - h4, frameWidth + 1, h4 + 1);
			}
			x0 += frameWidth;
			y0 += frameWidth;
			x1 -= frameWidth;
			y1 -= frameWidth;
			w -= 2 * frameWidth;
			h -= 2 * frameWidth;
			yOrig = y0;
		}
		Color c = (cellColor == null/* || nObj<1*/) ? getBackground() : cellColor;
		if (c.getGreen() + c.getRed() >= 400 || c.getGreen() > 200) {
			g.setColor(Color.gray);
		} else {
			g.setColor(Color.white);
		}
		if (nObj > 0 && maxNObjPerCell > 0) {
			int maxLen = w - 2 * frameWidth;
			int len = Math.round(1.0f * maxLen * nObj / maxNObjPerCell);
			g.drawRect(x0 + frameWidth, y0 + frameWidth, maxLen, frameWidth);
			g.fillRect(x0 + frameWidth, y0 + frameWidth, len + 1, frameWidth);
			yOrig = y0 + 3 * frameWidth;
		}
		int imW = 0, imH = 0;
		int fh = g.getFontMetrics().getHeight(), asc = g.getFontMetrics().getAscent();
		if (nObj > 0) {
			int nLines = 0;
			if (showStatistics) {
				++nLines;
			}
			if (clusterLabel != null) {
				++nLines;
			}
			if (showStatistics) {
				if (!Double.isNaN(maxDistToNeighbour)) {
					++nLines;
				}
				if (nObj > 1 && !Double.isNaN(maxDistToPrototype)) {
					++nLines;
				}
				if (nObj > 2 && !Double.isNaN(aveDistToPrototype)) {
					++nLines;
				}
			}
			String sNum = String.valueOf(nObj);
			int sw = g.getFontMetrics().stringWidth(sNum);
			int x = x0 + (w - sw) / 2, y = yOrig;
			if (image != null && drawImage) {
				int iw = image.getWidth(), ih = image.getHeight();
				if (imW < iw) {
					imW = iw;
				}
				imH += ih;
			}
			if (indexImage != null && drawIndexImage) {
				int iw = indexImage.getWidth(), ih = indexImage.getHeight();
				if (imW < iw) {
					imW = iw;
				}
				imH += ih;
			}
			if (imH < 1) {
				y = y0 + (h - nLines * fh) / 2;
				if (y < yOrig) {
					y = yOrig;
				}
			}
			y += asc;
			if (showStatistics) {
				g.drawString(sNum, x, y);
				if (clusterLabel != null) {
					sw = g.getFontMetrics().stringWidth(clusterLabel);
					x = x0 + (w - sw) / 2;
					y += fh;
					g.drawString(clusterLabel, x, y);
				}
				if (!Double.isNaN(maxDistToNeighbour)) {
					sNum = String.valueOf(Math.round(maxDistToNeighbour * 1000) / 1000f);
					sw = g.getFontMetrics().stringWidth(sNum);
					x = x0 + (w - sw) / 2;
					y += fh;
					g.drawString(sNum, x, y);
				}
				if (nObj > 1 && !Double.isNaN(maxDistToPrototype)) {
					sNum = String.valueOf(Math.round(maxDistToPrototype * 1000) / 1000f);
					sw = g.getFontMetrics().stringWidth(sNum);
					x = x0 + (w - sw) / 2;
					y += fh;
					g.drawString(sNum, x, y);
				}
				if (nObj > 2 && !Double.isNaN(aveDistToPrototype)) {
					sNum = String.valueOf(Math.round(aveDistToPrototype * 1000) / 1000f);
					sw = g.getFontMetrics().stringWidth(sNum);
					x = x0 + (w - sw) / 2;
					y += fh;
					g.drawString(sNum, x, y);
				}
			}
		}
		int width = w - 2 * frameWidth, height = h - 2 * frameWidth;
		int clY = y0 + height / 2 - fh / 2;
		if (imW > 0 && imH > 0) {
			int y = y0 + frameWidth + 1, x = x0 + frameWidth + 1;
			if (nObj > 0 && maxNObjPerCell > 0) {
				height -= 2 * frameWidth;
				y += 2 * frameWidth;
			}
			if (drawImage && drawIndexImage && image != null && indexImage != null) {
				height -= frameWidth;
				int iw = image.getWidth(), ih = image.getHeight(), iiw = indexImage.getWidth(), iih = indexImage.getHeight();
				//scale factor in x-dimension to fit the image width in the cell
				float ixFactor = Math.min(1f * width / iw, 1f), iixFactor = Math.min(1f * width / iiw, 1f);
				//the heights of the images after applying the scale factor
				float ih1 = ixFactor * ih, iih1 = iixFactor * iih, hSum = ih1 + iih1;
				//scale factor in y-dimension
				float yFactor = Math.min(height / hSum, 1f);
				float iFactor = ixFactor * yFactor, iiFactor = iixFactor * yFactor;
				if (iFactor < 1) {
					iw = (int) Math.ceil(iFactor * iw);
					ih = (int) Math.ceil(iFactor * ih);
				}
				g.drawImage(image, x + (width - iw) / 2, y, iw, ih, null);
				clY = y + ih;
				if (iiFactor < 1) {
					iiw = (int) Math.ceil(iiFactor * iiw);
					iih = (int) Math.ceil(iiFactor * iih);
				}
				g.drawImage(indexImage, x + (width - iiw) / 2, y + height - iih + frameWidth, iiw, iih, null);
			} else if (drawImage && image != null) {
				int iw = image.getWidth(), ih = image.getHeight();
				//scale factor in x-dimension to fit the image width in the cell
				float ixFactor = Math.min(1f * width / iw, 1f);
				//the height of the image after applying the scale factor
				float ih1 = ixFactor * ih;
				//scale factor in y-dimension
				float yFactor = Math.min(height / ih1, 1f);
				float iFactor = ixFactor * yFactor;
				if (iFactor < 1) {
					iw = (int) Math.ceil(iFactor * iw);
					ih = (int) Math.ceil(iFactor * ih);
				}
				g.drawImage(image, x + (width - iw) / 2, y, iw, ih, null);
				clY = y + ih;
			} else if (drawIndexImage && indexImage != null) {
				int iiw = indexImage.getWidth(), iih = indexImage.getHeight();
				//scale factor in x-dimension to fit the image width in the cell
				float iixFactor = Math.min(1f * width / iiw, 1f);
				//the height of the image after applying the scale factor
				float iih1 = iixFactor * iih;
				//scale factor in y-dimension
				float yFactor = Math.min(height / iih1, 1f);
				float iiFactor = iixFactor * yFactor;
				if (iiFactor < 1) {
					iiw = (int) Math.ceil(iiFactor * iiw);
					iih = (int) Math.ceil(iiFactor * iih);
				}
				g.drawImage(indexImage, x + (width - iiw) / 2, y + height - iih, iiw, iih, null);
				clY = y + height - iih - fh;
			}
			if (!showStatistics && clusterLabel != null) {
				int sw = g.getFontMetrics().stringWidth(clusterLabel);
				x = x0 + (w - sw) / 2;
				g.drawString(clusterLabel, x, clY + asc);
			}
		}
	}

	/**
	 * Invoked when the mouse button has been clicked (pressed
	 * and released) on a component.
	 */
	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() == 1 && owner != null) {
			owner.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "CellViewClicked"));
		}
	}

	/**
	 * Invoked when a mouse button has been pressed on a component.
	 */
	@Override
	public void mousePressed(MouseEvent e) {
	}

	/**
	 * Invoked when a mouse button has been released on a component.
	 */
	@Override
	public void mouseReleased(MouseEvent e) {
	}

	/**
	 * Invoked when the mouse enters a component.
	 */
	@Override
	public void mouseEntered(MouseEvent e) {
	}

	/**
	 * Invoked when the mouse exits a component.
	 */
	@Override
	public void mouseExited(MouseEvent e) {
	}
}
