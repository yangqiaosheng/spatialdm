package spade.time.vis;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ResourceBundle;

import spade.lib.lang.Language;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 16-Mar-2007
 * Time: 16:06:37
 * To change this template use File | Settings | File Templates.
 */
public class TimeSegmentedBarLegendCanvas extends Canvas {
	static ResourceBundle res = Language.getTextResource("spade.time.vis.SchedulerTexts_time_vis");

	protected Color colors[] = null;
	protected String commonComment = null;
	protected String comments[] = null;
	protected int max = -1;

	public TimeSegmentedBarLegendCanvas(Color colors[], String commonComment, String comments[]) {
		super();
		this.colors = colors;
		this.commonComment = commonComment;
		this.comments = comments;
	}

	public void setMax(int max) {
		this.max = max;
		redraw();
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(200, 200);
	}

	public void redraw() {
		Graphics g = getGraphics();
		if (g != null) {
			draw(g);
			g.dispose();
		}
	}

	@Override
	public void paint(Graphics g) {
		draw(g);
	}

	public void draw(Graphics g) {
		g.setColor(Color.white);
		g.fillRect(0, 0, getSize().width + 1, getSize().height + 1);
		int fh = g.getFontMetrics(getFont()).getHeight();
		g.setColor(Color.black);
		g.drawString(commonComment, 5, fh);
		for (int i = 0; i < colors.length; i++) {
			g.setColor(colors[i]);
			g.fillRect(5, 10 + fh * (i + 1) + 1, fh, fh - 5);
			g.setColor(colors[i].darker());
			g.drawRect(5, 10 + fh * (i + 1) + 1, fh, fh - 5);
			g.setColor(Color.black);
			g.drawString(comments[i], 8 + fh, 5 + fh * (i + 2));
		}
		g.drawString(res.getString("Full_height") + "=" + max, 5, 10 + fh * (colors.length + 2) + 1);
	}

}
