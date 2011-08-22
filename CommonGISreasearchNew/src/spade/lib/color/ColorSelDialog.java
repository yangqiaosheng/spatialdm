package spade.lib.color;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.TabbedPanel;
import spade.lib.color.ColorBrewer.Schemes;
import spade.lib.lang.Language;

class CS2Show extends Canvas {
	static final int mm = Math.round(Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f);
	static int N = 40;
	float HueL = 0f, HueR = 0f;
	boolean useShades = true;
	protected Color midColor = Color.white;

	public CS2Show(boolean useShades) {
		super();
		this.useShades = useShades;
	}

	public void setHueL(float HueL) {
		this.HueL = HueL;
		repaint();
	}

	public void setHueR(float HueR) {
		this.HueR = HueR;
		repaint();
	}

	public void setMidColor(Color color) {
		midColor = color;
		repaint();
	}

	@Override
	public void paint(Graphics g) {
		Dimension sz = getSize();
		int dx = sz.width - 10, dy = sz.height - 1;
		if (useShades) {
			int M = 2 * N + 1, dx0 = dx / M;
			dx = M * dx0;
			for (int i = -N; i <= N; i++) {
				if (i == 0) {
					g.setColor(midColor);
				} else {
					g.setColor(CS.getColor(Math.abs(i), N, (i < 0) ? HueL : HueR));
				}
				g.fillRect(5 + (i + N) * dx0, 0, dx0, dy);
			}
		} else {
			int half = dx / 2;
			g.setColor(CS.getColor(0.7f, HueL));
			g.fillRect(5, 0, half + 1, dy);
			g.setColor(CS.getColor(0.7f, HueR));
			g.fillRect(6 + half, 0, half + 1, dy);
			g.setColor(midColor);
			g.fillRect(5 + half, 0, 3, dy);
		}
		g.setColor(Color.black);
		g.drawRect(5, 0, dx, dy);
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(mm * (2 * N + 1) + 10, 4 * mm);
	}
}

class CB2Show extends Canvas {
	static final int mm = Math.round(Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f);
	static int N = 3; // N classes
	int cbScaleN = 0; // ColorBrewer scale N
	boolean isInverted = false;

	public CB2Show(int cbScaleN, boolean isInverted) {
		this.cbScaleN = cbScaleN;
		this.isInverted = isInverted;
	}

	public void draw(Graphics g) {
		Dimension sz = getSize();
		int dx = sz.width - 10, dy = sz.height - 1;
		int M = 2 * N + 1, dx0 = dx / M;
		dx = M * dx0;
		for (int i = -N; i <= N; i++) {
			g.setColor(Schemes.getColor((i == 0) ? 0.5f : (float) (i + N) / (2 * N), cbScaleN, isInverted));
			g.fillRect(5 + (i + N) * dx0, 0, dx0, dy);
		}
		g.setColor(Color.black);
		g.drawRect(5, 0, dx, dy);
	}

	@Override
	public void paint(Graphics g) {
		draw(g);
	}

	public void redraw() {
		Graphics g = getGraphics();
		if (g != null) {
			draw(g);
			g.dispose();
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(mm * (2 * N + 1) + 10, 4 * mm);
	}
}

public class ColorSelDialog extends Panel implements ActionListener, ItemListener {
	static ResourceBundle res = Language.getTextResource("spade.lib.color.Res");
	protected int ncolors = 0;

	protected TabbedPanel tp = null;

	// our standard colour scales
	protected Rainbow rainbows[] = null;
	protected BrightColorSelection midColorSel = null;
	protected CS2Show CS2S = null;
	protected boolean changed = false;

	// ColorBrewer colour scales
	protected CB2Show CB2S[] = null;
	protected Checkbox cbCB[] = null;
	protected Checkbox cbInverted = null;

	public ColorSelDialog(int ncolors, float hues[], Color midColor, String prompts[], boolean makeDoubleEnded, boolean useShades) {
		// following string: "Color selection"
		this.ncolors = ncolors;

		setLayout(new BorderLayout());
		tp = new TabbedPanel();

		Panel csp = new Panel(new ColumnLayout());
		rainbows = new Rainbow[ncolors];
		for (int i = 0; i < ncolors; i++) {
			csp.add(new Label(prompts[i], Label.LEFT));
			csp.add(rainbows[i] = new Rainbow());
			rainbows[i].usedToSelectHue = true;
			rainbows[i].setActionListener(this);
			rainbows[i].setCurrHue((hues[i] >= 0) ? hues[i] : 0);
		}
		if (makeDoubleEnded) {
			if (midColor != null) {
				midColorSel = new BrightColorSelection();
				midColorSel.setCurrColor(midColor);
				midColorSel.setActionListener(this);
				csp.add(new Label(res.getString("middle_color"), Label.LEFT));
				csp.add(midColorSel);
			}
			// following string: "Color scale:"
			csp.add(new Label(res.getString("Color_scale_"), Label.LEFT)); // Color scale:
			csp.add(CS2S = new CS2Show(useShades));
			CS2S.setHueL(hues[1]);
			CS2S.setHueR((hues[0] >= 0) ? hues[0] : 0f);
		}

		if (makeDoubleEnded && midColor != null) {
			Panel cbp = new Panel(new ColumnLayout());
			cbCB = new Checkbox[Schemes.schemeNames.length];
			CB2S = new CB2Show[Schemes.schemeNames.length];

			CheckboxGroup cbg = new CheckboxGroup();
			boolean isInverted = hues[0] <= -1100f;
			for (int i = 0; i < cbCB.length; i++) {
				cbp.add(cbCB[i] = new Checkbox(Schemes.schemeNames[i], hues[0] == -1000 - i - ((isInverted) ? 100 : 0), cbg)); // Color scale:
				cbp.add(CB2S[i] = new CB2Show(i, isInverted));
			}
			cbp.add(new Line(false));
			cbp.add(cbInverted = new Checkbox("inverted", isInverted));
			cbInverted.addItemListener(this);

			tp.addComponent("ColorBrewer", cbp);
		}
		tp.addComponent("other colour scales", csp);
		tp.makeLayout();
		if (hues[0] >= 0 && tp.getTabCount() > 1) {
			tp.selectTab(1);
		}
		add(tp, BorderLayout.CENTER);
		add(new Line(false), BorderLayout.SOUTH);
	}

	@Override
	public String getName() {
		return res.getString("Color_selection");
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (ie.getSource().equals(cbInverted)) {
			for (int i = 0; i < CB2S.length; i++) {
				CB2S[i].isInverted = cbInverted.getState();
				CB2S[i].redraw();
			}
		}
	}

	public int getNHues() {
		return ncolors;
	}

	public float getHueForItem(int itemN) {
		if (rainbows == null || itemN < 0 || itemN >= rainbows.length)
			return 0;
		if (itemN == 0 && cbCB != null && tp.getActiveTabN() == 0) {
			for (int i = 0; i < cbCB.length; i++)
				if (cbCB[i].getState())
					return -1000 - i - ((cbInverted.getState() ? 100 : 0));
		}
		return rainbows[itemN].getCurrHue();
	}

	public Color getMidColor() {
		if (midColorSel == null)
			return null;
		return midColorSel.getCurrColor();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof Rainbow) {
			changed = true;
			if (CS2S != null)
				if (e.getSource() == rainbows[0]) {
					CS2S.setHueR(rainbows[0].getCurrHue());
				} else if (e.getSource() == rainbows[1]) {
					CS2S.setHueL(rainbows[1].getCurrHue());
				}
		} else if (e.getSource().equals(midColorSel)) {
			CS2S.setMidColor(midColorSel.getCurrColor());
		}
	}
}
