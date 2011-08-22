package spade.lib.basicwin;

// no texts
import java.awt.Graphics;

public interface ItemPainter {
	public int itemH();

	public int maxItemW();

	public void drawItem(Graphics g, int n, int x, int y, int w, boolean IsActive);

	public void drawEmptyList(Graphics g, int x, int y, int w, int h);
}
