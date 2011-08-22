package spade.vis.preference;

import java.awt.BorderLayout;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.ImageCanvas;
import spade.lib.basicwin.Line;
import spade.lib.color.ColorCanvas;
import spade.lib.color.ColorDlg;
import spade.lib.color.ColorListener;
import spade.lib.lang.Language;
import spade.vis.geometry.GeomSign;

class IconControlPanel extends Panel implements ActionListener, ItemListener, ColorListener {
	static ResourceBundle res = Language.getTextResource("spade.vis.preference.Res");
	protected GeomSign gs = null;

	public GeomSign getGeneratedSign() {
		return gs;
	}

	protected ImageCanvas imgCanvas = null;
	protected ColorDlg cDlg = null;
	Choice chShape = null, chBorder = null;
	ColorCanvas ccShape = null, ccBorder = null, ccSymbol = null;
	TextField tfSymbol = null;

	public IconControlPanel(GeomSign oldGs) {
		gs = oldGs;
		if (gs == null) {
			gs = new GeomSign();
			gs.setShape(GeomSign.SHAPE_FIRST);
			gs.setFillColor(Color.cyan);
			gs.setFrameThickness(0);
			gs.setFrameColor(Color.black);
			gs.setSymbol("");
			gs.setSymbolColor(Color.yellow);
		}
		imgCanvas = new ImageCanvas();
		imgCanvas.setImage(gs.getImage(/*imgCanvas*/CManager.getAnyFrame()));
		setLayout(new ColumnLayout());
		Panel p = new Panel();
		p.setLayout(new BorderLayout());
		p.add(imgCanvas, "West");
		add(p);
		add(new Line(false));
		chShape = new Choice();
		chBorder = new Choice();
		chShape.addItemListener(this);
		chBorder.addItemListener(this);
		for (int i = GeomSign.SHAPE_FIRST; i <= GeomSign.SHAPE_LAST; i++) {
			chShape.add(GeomSign.shapeStrings[i]);
		}
		chShape.select(gs.getShape());
		// following string: "No border"
		chBorder.add("No border");
		for (int i = 1; i <= 5; i++) {
			chBorder.add("" + i);
		}
		chBorder.select(gs.getFrameThickness());
		tfSymbol = new TextField("", 1);
		tfSymbol.addActionListener(this);
		tfSymbol.setText(gs.getSymbol());
		ccShape = new ColorCanvas();
		ccShape.setActionListener(this);
		ccBorder = new ColorCanvas();
		ccBorder.setActionListener(this);
		ccSymbol = new ColorCanvas();
		ccSymbol.setActionListener(this);
		ccShape.setColor(gs.getFillColor());
		ccBorder.setColor(gs.getFrameColor());
		ccSymbol.setColor(gs.getSymbolColor());
		Panel pp = new Panel();
		pp.setLayout(new BorderLayout());
		// following string:"Shape"
		pp.add(new Label(res.getString("Shape")), "West");
		pp.add(chShape, "Center");
		pp.add(ccShape, "East");
		add(pp);
		pp = new Panel();
		pp.setLayout(new BorderLayout());
		// following string:"Border"
		pp.add(new Label(res.getString("Border")), "West");
		pp.add(chBorder, "Center");
		pp.add(ccBorder, "East");
		add(pp);
		pp = new Panel();
		pp.setLayout(new BorderLayout());
		// following string: "Symbol"
		pp.add(new Label(res.getString("Symbol")), "West");
		pp.add(tfSymbol, "Center");
		pp.add(ccSymbol, "East");
		add(pp);
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource() instanceof ColorCanvas) {
			ColorCanvas cc = null;
			if (ae.getSource() == ccShape) {
				cc = ccShape;
			} else if (ae.getSource() == ccBorder) {
				cc = ccBorder;
			} else if (ae.getSource() == ccSymbol) {
				cc = ccSymbol;
			}
			if (cDlg == null) {
				// following string: "Select color"
				cDlg = new ColorDlg(CManager.getAnyFrame(this), res.getString("Select_color"));
			}
			cDlg.selectColor(this, cc, cc.getColor());
			return;
		}
		if (ae.getSource() == tfSymbol) {
			String str = tfSymbol.getText().trim();
			if (str.length() > 1) {
				str = str.substring(1, 1);
			}
			gs.setSymbol(str);
			imgCanvas.setImage(gs.getImage(imgCanvas));
			return;
		}
	}

	@Override
	public void colorChanged(Color c, Object sel) {
		ColorCanvas cc = null;
		String ID = null;
		if (sel == ccShape) {
			ccShape.setColor(c);
			gs.setFillColor(c);
		} else if (sel == ccBorder) {
			ccBorder.setColor(c);
			gs.setFrameColor(c);
		} else if (sel == ccSymbol) {
			ccSymbol.setColor(c);
			gs.setSymbolColor(c);
		}
		// hide a dialog
		cDlg.setVisible(false);
		// redraw the sign
		imgCanvas.setImage(gs.getImage(imgCanvas));
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (ie.getSource() == chShape) {
			gs.setShape(chShape.getSelectedIndex());
			imgCanvas.setImage(gs.getImage(imgCanvas));
			return;
		}
		if (ie.getSource() == chBorder) {
			gs.setFrameThickness(chBorder.getSelectedIndex());
			imgCanvas.setImage(gs.getImage(imgCanvas));
			return;
		}
	}
}
