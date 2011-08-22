package spade.vis.geometry;

import java.awt.Graphics;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Mar 8, 2010
 * Time: 5:52:03 PM
 */
public interface DrawableSymbol {
	public void setSize(int size);

	public void draw(Graphics g, int x, int y, int w, int h);
}
