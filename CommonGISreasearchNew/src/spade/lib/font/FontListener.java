package spade.lib.font;

import java.awt.Font;

public interface FontListener {
	public void fontChanged(Font font, int fontStyle, Object selector);
}
