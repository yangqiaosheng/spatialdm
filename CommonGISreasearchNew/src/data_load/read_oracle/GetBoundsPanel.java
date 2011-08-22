package data_load.read_oracle;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;

import spade.lib.basicwin.MyCanvas;
import spade.lib.lang.Language;
import spade.vis.geometry.RealRectangle;

/**
* A UI to get from the user the territory limits for loading geographic data
* from a database
*/
public class GetBoundsPanel extends Panel implements ItemListener, ActionListener, PropertyChangeListener {
	static ResourceBundle res = Language.getTextResource("data_load.read_oracle.Res");
	protected RealRectangle layerExt = null, mapExt = null, customExt = null;
	protected Checkbox cb[] = null;
	protected Color cbColors[] = { Color.red.darker(), Color.blue.darker(), Color.green.darker() };
	protected TextField tf[] = null;
	protected String labTexts[] = { "min X:", "min Y:", "max X:", "max Y:" };
	protected MyCanvas can = null;

	/**
	* The argument layerExt is the extent of the whole layer while mapExt is the
	* extent of the currently visible territory in the map view (may be null)
	*/
	public GetBoundsPanel(RealRectangle layerExt, RealRectangle mapExt) {
		setBackground(Color.lightGray);
		this.layerExt = layerExt;
		this.mapExt = mapExt;
		customExt = (RealRectangle) layerExt.clone();
		int n = 7;
		cb = new Checkbox[3];
		CheckboxGroup cbg = new CheckboxGroup();
		//following text:"whole layer extent"
		cb[0] = new Checkbox(res.getString("whole_layer_extent"), true, cbg);
		//following text:"current map extent"
		if (mapExt != null) {
			cb[1] = new Checkbox(res.getString("current_map_extent"), false, cbg);
		} else {
			cb[1] = null;
			--n;
		}
		//following text:"other"
		cb[2] = new Checkbox(res.getString("other"), false, cbg);
		Panel p = new Panel(new GridLayout(n, 1));
		for (int i = 0; i < cb.length; i++)
			if (cb[i] != null) {
				cb[i].setForeground(cbColors[i]);
				p.add(cb[i]);
				cb[i].addItemListener(this);
			}
		tf = new TextField[4];
		for (int i = 0; i < tf.length; i++) {
			Panel pp = new Panel(new BorderLayout());
			pp.add(new Label(labTexts[i]), "West");
			tf[i] = new TextField(30);
			tf[i].addActionListener(this);
			pp.add(tf[i], "Center");
			p.add(pp);
		}
		setLayout(new BorderLayout());
		add(p, "North");
		can = new MyCanvas();
		add(can, "Center");
		can.setPreferredSize(100, 100);
		can.setPainter(this);
		fillTextFields();
	}

	protected void fillTextFields() {
		int n = -1;
		for (int i = 0; i < cb.length && n < 0; i++)
			if (cb[i] != null && cb[i].getState()) {
				n = i;
			}
		RealRectangle r = (n == 0) ? layerExt : (n == 1) ? mapExt : customExt;
		tf[0].setText(String.valueOf(r.rx1));
		tf[1].setText(String.valueOf(r.ry1));
		tf[2].setText(String.valueOf(r.rx2));
		tf[3].setText(String.valueOf(r.ry2));
		if (tf[0].isEnabled() != (n == 2)) {
			for (TextField element : tf) {
				element.setEnabled(n == 2);
			}
			can.repaint();
		}
	}

	protected void showCustomBounds() {
		tf[0].setText(String.valueOf(customExt.rx1));
		tf[1].setText(String.valueOf(customExt.ry1));
		tf[2].setText(String.valueOf(customExt.rx2));
		tf[3].setText(String.valueOf(customExt.ry2));
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() instanceof Checkbox) {
			fillTextFields();
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof TextField) {
			TextField f = (TextField) e.getSource();
			String txt = f.getText();
			if (txt == null) {
				showCustomBounds();
				return;
			}
			txt = txt.trim();
			if (txt.length() < 1) {
				showCustomBounds();
				return;
			}
			float val = Float.NaN;
			try {
				val = Float.valueOf(txt).floatValue();
			} catch (NumberFormatException nfe) {
			}
			if (Float.isNaN(val)) {
				showCustomBounds();
				return;
			}
			//check consistency
			if (e.getSource().equals(tf[0])) { //min X changed
				if (val >= customExt.rx2) {
					showCustomBounds();
					return;
				}
				if (val < layerExt.rx1) {
					val = layerExt.rx1;
				}
				customExt.rx1 = val;
			} else if (e.getSource().equals(tf[1])) { //min Y changed
				if (val >= customExt.ry2) {
					showCustomBounds();
					return;
				}
				if (val < layerExt.ry1) {
					val = layerExt.ry1;
				}
				customExt.ry1 = val;
			} else if (e.getSource().equals(tf[2])) { //max X changed
				if (val <= customExt.rx1) {
					showCustomBounds();
					return;
				}
				if (val > layerExt.rx2) {
					val = layerExt.rx2;
				}
				customExt.rx2 = val;
			} else if (e.getSource().equals(tf[3])) { //max Y changed
				if (val <= customExt.ry1) {
					showCustomBounds();
					return;
				}
				if (val > layerExt.ry2) {
					val = layerExt.ry2;
				}
				customExt.ry2 = val;
			}
			showCustomBounds();
			can.repaint();
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(can) && e.getPropertyName().equals("must_paint")) {
			Graphics g = (Graphics) e.getNewValue();
			RealRectangle maxExt = (RealRectangle) layerExt.clone();
			if (mapExt != null) {
				if (maxExt.rx1 > mapExt.rx1) {
					maxExt.rx1 = mapExt.rx1;
				}
				if (maxExt.ry1 > mapExt.ry1) {
					maxExt.ry1 = mapExt.ry1;
				}
				if (maxExt.rx2 < mapExt.rx2) {
					maxExt.rx2 = mapExt.rx2;
				}
				if (maxExt.ry2 < mapExt.ry2) {
					maxExt.ry2 = mapExt.ry2;
				}
			}
			if (cb[2].getState()) {
				if (maxExt.rx1 > customExt.rx1) {
					maxExt.rx1 = customExt.rx1;
				}
				if (maxExt.ry1 > customExt.ry1) {
					maxExt.ry1 = customExt.ry1;
				}
				if (maxExt.rx2 < customExt.rx2) {
					maxExt.rx2 = customExt.rx2;
				}
				if (maxExt.ry2 < customExt.ry2) {
					maxExt.ry2 = customExt.ry2;
				}
			}
			Dimension size = can.getSize();
			int marg = 5;
			size.width -= 2 * marg;
			size.height -= 2 * marg;
			float rx = (maxExt.rx2 - maxExt.rx1) / size.width, ry = (maxExt.ry2 - maxExt.ry1) / size.height, r = (rx > ry) ? rx : ry;
			int nit = (cb[2].getState()) ? 3 : 2;
			for (int i = 0; i < nit; i++) {
				RealRectangle rect = (i == 0) ? layerExt : (i == 1) ? mapExt : customExt;
				if (rect == null) {
					continue;
				}
				int x = marg + Math.round((rect.rx1 - maxExt.rx1) / r), w = Math.round((rect.rx2 - rect.rx1) / r), y = marg + size.height - Math.round((rect.ry2 - maxExt.ry1) / r), h = Math.round((rect.ry2 - rect.ry1) / r);
				g.setColor(cbColors[i]);
				g.drawRect(x, y, w, h);
				g.drawRect(x + 1, y + 1, w - 2, h - 2);
			}
		}
	}

	public RealRectangle getExtent() {
		int n = -1;
		for (int i = 0; i < cb.length && n < 0; i++)
			if (cb[i] != null && cb[i].getState()) {
				n = i;
			}
		if (n == 0)
			return null; //no limitation
		return (n == 1) ? mapExt : customExt;
	}
}
