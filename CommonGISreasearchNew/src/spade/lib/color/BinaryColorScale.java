package spade.lib.color;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;

import spade.lib.lang.Language;

/**
* Provides special encoding for "binary" rasters: 0 is not shown and 1 is shown
* with some color that may be selected by the user. In the "reversed" state,
* on the opposite, 1 is not shown and 0 is shown. When applied to an arbitrary
* raster (i.e. not only with values 0 and 1), treats all values <=0 as 0 and
* all values >0 as 1.
*/
public class BinaryColorScale extends BaseColorScale implements ItemListener, ActionListener, ColorListener {
	static ResourceBundle res = Language.getTextResource("spade.lib.color.Res");
	/**
	* When reversed is true, values 1 are not shown and values 0 are shown.
	*/
	public boolean reversed = false;
	/**
	* The color used to show values
	*/
	public int color = Color.gray.brighter().getRGB(); //Color.red.getRGB();

	/**
	* Returns the color for the given value packed into an integer:
	* top 8 bits (0xFF << 24) - alpha value
	* 2nd 8 bits (0xFF << 16) - red value
	* 3rd 8 bits (0xFF << 8) - green value
	* bottom 8 bits (0xFF) - blue value
	*/
	@Override
	public int getPackedColorForValue(float val) {
		if (!reversed && val < 0.0001)
			return 0x00000000;
		if (reversed && val >= 0.0001)
			return 0x00000000;
		if ((alphaFactor & 0xFF000000) == 0x00000000)
			return 0x00000000;
		if (val < minLimit || val > maxLimit)
			return 0x00000000;
		return color & alphaFactor;
	}

	@Override
	public int encodeValue(float val) {
		return color;
	}

	/**
	* Draws a color bar representing this color scale
	*/
	@Override
	public void drawColorBar(Graphics g, int x, int y, int w, int h) {
		if ((!reversed && minLimit >= 0.0001f) || (reversed && maxLimit < 0.0001f)) {
			g.setColor(new Color(color));
			g.fillRect(x, y, w + 1, h + 1);
		} else if ((!reversed && maxLimit < 0.0001f) || (reversed && minLimit >= 0.0001f)) {
			g.setColor(Color.gray);
			g.drawRect(x, y, w, h);
		} else {
			int w1 = Math.round((0.0001f - minLimit) * w / (maxLimit - minLimit));
			if (w1 < 10) {
				w1 = 10;
			}
			g.setColor(Color.gray);
			if (!reversed) {
				g.drawRect(x, y, w1, h);
			} else {
				g.drawRect(x + w1, y, w - w1, h);
			}
			g.setColor(new Color(color));
			if (!reversed) {
				g.fillRect(x + w1, y, w - w1 + 1, h + 1);
			} else {
				g.fillRect(x, y, w1 + 1, h + 1);
			}
		}
	}

	/**
	* Returns a UI component that allows the user to manipulate specific
	* parameters of this color scale (i.e. not transparency or minimum and
	* maximum limits that are manipulated in the same way for all color scales)
	* A GrayColorScale returns a checkbox for switching on/off the "reversed" mode..
	*/
	@Override
	public Component getManipulator() {
		Panel p = new Panel(new BorderLayout());
		// following string: "reversed"
		Checkbox cb = new Checkbox(res.getString("reversed"), reversed);
		cb.addItemListener(this);
		p.add(cb, "West");
		Rainbow rb = new Rainbow();
		rb.setActionListener(this);
		p.add(rb, "East");
		return p;
	}

	/**
	* Reacts to changes of the state of the "reversed" checkbox
	*/
	@Override
	public void itemStateChanged(ItemEvent ev) {
		if (ev.getSource() instanceof Checkbox) {
			Checkbox cb = (Checkbox) ev.getSource();
			if (reversed != cb.getState()) {
				reversed = cb.getState();
				notifyScaleChange();
			}
		}
	}

	/**
	* Reacts to a click in the "rainbow" icon indicating that the user wishes to
	* change the color of the scale.
	*/
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof Rainbow) {
			//get a frame necessary for construction of a dialog
			Component c = (Component) e.getSource();
			Frame fr = null;
			while (c != null && fr == null) {
				c = c.getParent();
				if (c != null)
					if (c instanceof Frame) {
						fr = (Frame) c;
					} else if (c instanceof Dialog) {
						Dialog dia = (Dialog) c;
						if (dia.getParent() != null && (dia.getParent() instanceof Frame)) {
							fr = (Frame) dia.getParent();
							//if (dia.getOwner() instanceof Frame) fr=(Frame)dia.getOwner();
						}
					}
			}
			if (fr == null) {
				fr = new Frame();
			}
			// following string:"Select a color for the scale"
			ColorDlg cdlg = new ColorDlg(fr, res.getString("Select_a_color_for"));
			cdlg.selectColor(this, null, new Color(color));
		}
	}

	/**
	* Reacts to a change of the current color in the color selection dialog
	*/
	@Override
	public void colorChanged(Color color, Object selector) {
		this.color = color.getRGB();
		notifyScaleChange();
	}

	/**
	 * Pass parameter string to setup color scale
	 */
	@Override
	public void setParameters(String par) {
		if (par == null || par == "")
			return;
		reversed = par.substring(0, 1).equalsIgnoreCase("r");
		notifyScaleChange();
	}

	/**
	 * Pass parameter string to setup color scale
	 */
	@Override
	public String getParameters() {
		return reversed ? "REVERSED" : "NORMAL";
	}
}
