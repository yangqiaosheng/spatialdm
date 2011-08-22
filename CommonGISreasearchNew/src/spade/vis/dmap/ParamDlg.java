//iitp
package spade.vis.dmap;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ResourceBundle;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.Slider;
import spade.lib.basicwin.TabbedPanel;
import spade.lib.color.ColorDlg;
import spade.lib.color.ColorListener;
import spade.lib.font.FontListener;
import spade.lib.font.FontSelectDlg;
import spade.lib.lang.Language;
import spade.vis.geometry.Geometry;

public class ParamDlg extends Dialog implements ActionListener, ItemListener, WindowListener, ColorListener, FontListener {
	static ResourceBundle res = Language.getTextResource("spade.vis.dmap.Res");
	protected Button ok, cancel, chLine, chFill, chLabels, chLabelsFont, chCircleColor;
	protected TextField tfName = null, tfCircleSize = null, tfMinScaleDC = null, tfMaxScaleDC = null;
	protected Checkbox isLine = null, isFill = null, drawLabels = null, useCircle = null, useDrawCondition = null, allowSpatialFilter = null;
	protected Choice thickness[] = null;
// PG
	protected FontSelectDlg pFontSelect = null;
//~PG
	protected ColorDlg cdialog = null;
	protected ParamListener pl;
	protected Object selector;
	protected DrawingParameters dp, oldp;
	protected char type;
	protected boolean hasLabels = false;
	protected String name = null, oldName = null;
	protected Frame ownerFrame = null;
	protected float cmValue = 1f;
	protected Slider slider = null;
	protected Label transLabel = null;
	protected Checkbox drawHoles = null;

	public ParamDlg(Frame owner, ParamListener pl, Object selector, DrawingParameters dp, char type, String name, boolean hasLabels, float cmValue) {
		super(owner, name + ": " + res.getString("Parameters"), true);
		ownerFrame = owner;
		if (cdialog != null) {
			cdialog.dispose();
			cdialog = null;
		}
		this.pl = pl;
		this.selector = selector;
		this.dp = dp;
		oldp = dp.makeCopy();
		this.type = type;
		this.hasLabels = hasLabels;
		this.name = name;
		oldName = name;
		this.cmValue = cmValue;
		addWindowListener(this);
		addComponents();
		Dimension frsz = getSize();
		int sw = Metrics.scrW(), sh = Metrics.scrH();
		if (frsz.width > sw * 2 / 3) {
			frsz.width = sw * 2 / 3;
		}
		if (frsz.height > sh * 2 / 3) {
			frsz.height = sh * 2 / 3;
		}
		setBounds((sw - frsz.width) / 2, (sh - frsz.height) / 2, frsz.width, frsz.height);
		show();
	}

	void addComponents() {
		TabbedPanel tabP = new TabbedPanel();

		thickness = new Choice[3];
		for (int i = 0; i < 3; i++) {
			thickness[i] = new Choice();
			for (int j = 1; j <= 9; j++) {
				thickness[i].add(String.valueOf(j));
			}
			thickness[i].addItemListener(this);
		}

		if (type == Geometry.point || type == Geometry.area || type == Geometry.line) {
			Panel subP = new Panel(new ColumnLayout());
			chLine = new Button(res.getString("Change_line_color"));
			chLine.addActionListener(this);
			chLine.setBackground(dp.lineColor);
			chLine.setForeground(new Color(dp.lineColor.getRGB() ^ 0xFFFFFF));
			chLine.setEnabled(dp.drawBorders);
			chFill = new Button(res.getString("Change_fill_color"));
			chFill.addActionListener(this);
			chFill.setBackground(dp.fillColor);
			chFill.setForeground(new Color(dp.fillColor.getRGB() ^ 0xFFFFFF));
			chFill.setEnabled(dp.fillContours);
			isLine = new Checkbox(res.getString("draw_lines"), dp.drawBorders);
			isLine.addItemListener(this);
			isFill = new Checkbox(res.getString("fill_contours"), dp.fillContours);
			isFill.addItemListener(this);

			int k = dp.lineWidth;
			/*
			 * 2004-01-20 AO
			 * linethickness can now be even too.
			 */
			//if (k%2==0) ++k;
			thickness[0].select(Integer.toString(k));
			Panel pTh = new Panel();
			pTh.add(new Label(res.getString("Select_line_thickness")));
			pTh.add(thickness[0]);

			switch (type) {
			case Geometry.point:
			case Geometry.area:
				subP.add(isLine);
				subP.add(chLine);
				subP.add(pTh);
				subP.add(isFill);
				subP.add(chFill);
				break;
			case Geometry.line:
				subP.add(chLine);
				subP.add(pTh);
				break;
			}
			if (java2d.Drawing2D.isJava2D && type != Geometry.image && type != Geometry.raster /*&& type != Geometry.line*/) {
				slider = new Slider(this, 0, 100, oldp.transparency);
				slider.setNAD(true);
				transLabel = new Label(getTransText());
				subP.add(transLabel);
				subP.add(slider);
			}

			if (java2d.Drawing2D.isJava2D && type == Geometry.area) {
				if (selector instanceof DGeoLayer) {
					if (((DGeoLayer) selector).hasHoles) {
						drawHoles = new Checkbox("transparent painting of holes", oldp.drawHoles);
						drawHoles.addItemListener(this);
						subP.add(new Line(false));
						subP.add(drawHoles);
					}
				}
			}
			tabP.addComponent(res.getString("Drawing"), subP);
		}
		Panel subP = new Panel(new ColumnLayout());
		Panel p = new Panel(new BorderLayout());
		p.add(new Label(res.getString("Name_")), "West");
		tfName = new TextField(name, 20);
		tfName.addActionListener(this);
		p.add(tfName, "Center");
		subP.add(p);
		if (hasLabels && (type == Geometry.point || type == Geometry.area || type == Geometry.line)) {
			drawLabels = new Checkbox(res.getString("put_labels_on_the_map"), dp.drawLabels);
			drawLabels.addItemListener(this);
			subP.add(drawLabels);
			chLabels = new Button(res.getString("Change_color_of"));
			chLabels.addActionListener(this);
			chLabels.setBackground(dp.labelColor);
			chLabels.setForeground(new Color(dp.labelColor.getRGB() ^ 0xFFFFFF));
			chLabels.setEnabled(dp.drawLabels);
			subP.add(chLabels);
			chLabelsFont = new Button(res.getString("Change_font_for"));
			chLabelsFont.addActionListener(this);
			chLabelsFont.setForeground(dp.labelColor);
			chLabelsFont.setEnabled(dp.drawLabels);
			subP.add(chLabelsFont);
		}
		tabP.addComponent(res.getString("Names"), subP);

		subP = new Panel(new ColumnLayout());
		Panel pDC = new Panel(new BorderLayout());
		useDrawCondition = new Checkbox(res.getString("draw_condition"));
		useDrawCondition.setState(oldp.drawCondition);
		useDrawCondition.addItemListener(this);

		tfMinScaleDC = new TextField(Float.isNaN(dp.minScaleDC) ? "" : "" + dp.minScaleDC);
		tfMaxScaleDC = new TextField(Float.isNaN(dp.maxScaleDC) ? "" : "" + dp.maxScaleDC);
		tfMinScaleDC.setColumns(5);
		tfMaxScaleDC.setColumns(5);
		tfMinScaleDC.setEnabled(oldp.drawCondition);
		tfMaxScaleDC.setEnabled(oldp.drawCondition);

		pDC.add(useDrawCondition, "North");
		Panel ptfDC = new Panel(new FlowLayout());
		ptfDC.add(new Label(res.getString("min_scale") + " 1:", Label.RIGHT));
		ptfDC.add(tfMinScaleDC);
		ptfDC.add(new Label(res.getString("max_scale") + " 1:", Label.RIGHT));
		pDC.add(ptfDC, "Center");
		ptfDC.add(tfMaxScaleDC);
		tfMinScaleDC.addActionListener(this);
		tfMaxScaleDC.addActionListener(this);
		subP.add(pDC);

		Panel pp = new Panel(new FlowLayout(FlowLayout.LEFT));
		pp.add(new Label("Scale"));
		spade.lib.basicwin.Centimeter cm = new spade.lib.basicwin.Centimeter();
		pp.add(cm);
		pp.add(new Label("1:" + spade.lib.util.StringUtil.floatToStr(cmValue, 2)));
		subP.add(pp);
		tabP.addComponent(res.getString("Scale"), subP);

		subP = new Panel(new ColumnLayout());
		Panel pTh = new Panel(new FlowLayout());
		pTh.add(new Label(res.getString("lth_highlighting")));//"Line thickness for transient highlighting:"
		pTh.add(thickness[1]);
		subP.add(pTh);
		int k = dp.hlWidth;
		/*
		 * 2004-01-20 AO
		 * linethickness can now be even too.
		 */
		//if (k%2==0) ++k;
		thickness[1].select(Integer.toString(k));
		if (type == Geometry.area) {
			useCircle = new Checkbox(res.getString("circles_highlighting"), dp.hlDrawCircles);//"mark highlighted objects with circles"
			useCircle.addItemListener(this);
			subP.add(useCircle);
			p = new Panel(new BorderLayout());
			p.add(new Label(res.getString("circle_size")), "West");
			tfCircleSize = new TextField(String.valueOf(dp.hlCircleSize), 3);
			tfCircleSize.addActionListener(this);
			tfCircleSize.setEnabled(dp.hlDrawCircles);
			p.add(tfCircleSize, "Center");
			chCircleColor = new Button(res.getString("circle_color") + " *");
			chCircleColor.setEnabled(dp.hlDrawCircles);
			chCircleColor.setBackground(dp.hlCircleColor);
			chCircleColor.setForeground(new Color(dp.hlCircleColor.getRGB() ^ 0xFFFFFF));
			chCircleColor.addActionListener(this);
			p.add(chCircleColor, "East");
			subP.add(p);
			subP.add(new Label("* " + res.getString("distorted_color"), Label.RIGHT));//"the color may be distorted"
		}
		pTh = new Panel(new FlowLayout());
		pTh.add(new Label(res.getString("lth_selection")));//"Line thickness for durable highlighting:"
		pTh.add(thickness[2]);
		subP.add(pTh);
		k = dp.selWidth;
		/*
		 * 2004-01-20 AO
		 * linethickness can now be even too.
		 */
		//if (k%2==0) ++k;
		thickness[2].select(Integer.toString(k));
		tabP.addComponent(res.getString("Highlighting"), subP);

		subP = new Panel(new ColumnLayout());
		allowSpatialFilter = new Checkbox("allow spatial filtering", dp.allowSpatialFilter);
		allowSpatialFilter.addItemListener(this);
		subP.add(allowSpatialFilter);
		tabP.addComponent("Filtering", subP);

		ok = new Button(res.getString("OK"));
		ok.addActionListener(this);
		cancel = new Button(res.getString("Cancel"));
		cancel.addActionListener(this);
		Panel pOk = new Panel(new FlowLayout(FlowLayout.CENTER, 30, 0));
		pOk.add(ok);
		pOk.add(cancel);
		setLayout(new BorderLayout(5, 5));
		add(tabP, BorderLayout.CENTER);
		add(pOk, BorderLayout.SOUTH);
		tabP.makeLayout();
		pack();
	}

	protected String getTransText() {
		return res.getString("transparency_") + Math.round(slider.getValue()) + "%";
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == ok) {
			if (cdialog != null) {
				cdialog.dispose();
				cdialog = null;
			}
			dispose();
			String str = tfName.getText().trim();
			if (!str.equals(name)) {
				pl.nameChanged(selector, tfName.getText().trim());
				name = str;
				setTitle(name + ": " + res.getString("Parameters"));
			}
			if (tfCircleSize != null) {
				str = tfCircleSize.getText().trim();
				int k = 0;
				try {
					k = Integer.valueOf(str).intValue();
				} catch (NumberFormatException nfe) {
					k = 0;
				}
				if (k > 1) {
					dp.hlCircleSize = k;
				}
			}
			if (useDrawCondition.getState()) {
				str = tfMinScaleDC.getText().trim();
				float k = Float.NaN;
				try {
					if (str.equals("")) {
						k = Float.NaN;
					} else {
						k = Float.valueOf(str).floatValue();
					}
				} catch (NumberFormatException nfe) {
				}
				dp.minScaleDC = k;
				if (Float.isNaN(k)) {
					tfMinScaleDC.setText("");
				}

				str = tfMaxScaleDC.getText().trim();
				k = Float.NaN;
				try {
					if (str.equals("")) {
						k = Float.NaN;
					} else {
						k = Float.valueOf(str).floatValue();
					}
				} catch (NumberFormatException nfe) {
				}
				dp.maxScaleDC = k;
				if (Float.isNaN(k)) {
					tfMaxScaleDC.setText("");
				}
				dp.drawCondition = !Float.isNaN(dp.maxScaleDC) || !Float.isNaN(dp.minScaleDC);
			} else {
				dp.drawCondition = false;
				dp.maxScaleDC = Float.NaN;
				dp.minScaleDC = Float.NaN;
				tfMaxScaleDC.setText("");
				tfMinScaleDC.setText("");
			}
			pl.paramChanged(selector, dp.makeCopy());
		} else if (e.getSource() == cancel) {
			if (!name.equals(oldName)) {
				pl.nameChanged(selector, oldName);
			}
			pl.paramChanged(selector, oldp);
			if (cdialog != null) {
				cdialog.dispose();
				cdialog = null;
			}
			dispose();
		} else if (e.getSource() == chLine) {
			if (cdialog == null) {
				cdialog = new ColorDlg(ownerFrame, res.getString("Select_color"));
			}
			cdialog.selectColor(this, chLine, dp.lineColor);
		} else if (e.getSource() == chFill) {
			if (cdialog == null) {
				cdialog = new ColorDlg(ownerFrame, res.getString("Select_color"));
			}
			cdialog.selectColor(this, chFill, dp.fillColor);
		} else if (chLabels != null && e.getSource() == chLabels) {
			if (cdialog == null) {
				cdialog = new ColorDlg(ownerFrame, res.getString("Select_color"));
			}
			cdialog.selectColor(this, chLabels, dp.labelColor);
		} else if (chLabelsFont != null && e.getSource() == chLabelsFont) {
			OKDialog okdialog = new OKDialog(ownerFrame, res.getString("Select_font"), true, false);
			if (pFontSelect == null) {
				Font fntInit = null;
				if (dp.fontName != null) {
					fntInit = new Font(dp.fontName, dp.fontStyle, dp.fontSize);
				}
				if (fntInit == null) {
					fntInit = getFont();
				}
				pFontSelect = new FontSelectDlg(fntInit, dp.labelStyle, dp.labelColor);
				pFontSelect.addFontListener(this);
			}
			okdialog.addContent(pFontSelect);
			okdialog.show();
		} else if (e.getSource() == tfName) {
			String str = tfName.getText().trim();
			if (!str.equals(name)) {
				pl.nameChanged(selector, tfName.getText().trim());
				name = str;
				setTitle(name + ": " + res.getString("Parameters"));
			}
		} else if (e.getSource().equals(tfCircleSize)) {
			String str = tfCircleSize.getText().trim();
			int k = 0;
			try {
				k = Integer.valueOf(str).intValue();
			} catch (NumberFormatException nfe) {
			}
			if (k < 1) {
				tfCircleSize.setText(String.valueOf(dp.hlCircleSize));
			} else {
				dp.hlCircleSize = k;
			}
		} else if (e.getSource().equals(chCircleColor)) {
			if (cdialog == null) {
				cdialog = new ColorDlg(ownerFrame, res.getString("Select_color"));
			}
			cdialog.selectColor(this, chCircleColor, dp.hlCircleColor);
		} else if (e.getSource().equals(tfMinScaleDC)) {
			String str = tfMinScaleDC.getText().trim();
			float k = Float.NaN;
			try {
				if (str.trim().equals("")) {
					k = Float.NaN;
				} else {
					k = Float.valueOf(str).floatValue();
				}
			} catch (NumberFormatException nfe) {
			}
			if (Float.isNaN(k)) {
				tfMinScaleDC.setText("");
			}
			dp.minScaleDC = k;
		} else if (e.getSource().equals(tfMaxScaleDC)) {
			String str = tfMaxScaleDC.getText().trim();
			float k = Float.NaN;
			try {
				if (str.trim().equals("")) {
					k = Float.NaN;
				} else {
					k = Float.valueOf(str).floatValue();
				}
			} catch (NumberFormatException nfe) {
			}
			if (Float.isNaN(k)) {
				tfMaxScaleDC.setText("");
			}
			dp.maxScaleDC = k;
		} else if (e.getSource().equals(slider)) {
			dp.transparency = (int) slider.getValue();
			transLabel.setText(getTransText());
			if (!slider.getIsDragging()) {
				pl.paramChanged(selector, dp.makeCopy());
			}
		}

	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() instanceof Choice) {
			int idx = -1;
			for (int i = 0; i < 3 && idx < 0; i++)
				if (e.getSource().equals(thickness[i])) {
					idx = i;
				}
			if (idx >= 0) {
				int k = Integer.parseInt(thickness[idx].getSelectedItem());
				switch (idx) {
				case 0:
					dp.lineWidth = k;
					break;
				case 1:
					dp.hlWidth = k;
					break;
				case 2:
					dp.selWidth = k;
					break;
				}
				if (idx != 1) {
					pl.paramChanged(selector, dp.makeCopy());
				}
			}
		} else if (e.getSource().equals(isLine)) {
			chLine.setEnabled(dp.drawBorders = isLine.getState());
			pl.paramChanged(selector, dp.makeCopy());
		} else if (e.getSource().equals(isFill)) {
			chFill.setEnabled(dp.fillContours = isFill.getState());
			pl.paramChanged(selector, dp.makeCopy());
		} else if (drawLabels != null && e.getSource().equals(drawLabels)) {
			dp.drawLabels = drawLabels.getState();
			chLabels.setEnabled(dp.drawLabels);
			chLabelsFont.setEnabled(dp.drawLabels);
			pl.paramChanged(selector, dp.makeCopy());
		} else if (useCircle != null && e.getSource().equals(useCircle)) {
			dp.hlDrawCircles = useCircle.getState();
			tfCircleSize.setEnabled(dp.hlDrawCircles);
			chCircleColor.setEnabled(dp.hlDrawCircles);
		} else if (e.getSource().equals(useDrawCondition)) {
			dp.drawCondition = useDrawCondition.getState();
			tfMinScaleDC.setEnabled(dp.drawCondition);
			tfMaxScaleDC.setEnabled(dp.drawCondition);
			if (!dp.drawCondition) {
				tfMinScaleDC.setText("");
				tfMaxScaleDC.setText("");
			}
		} else if (e.getSource().equals(drawHoles)) {
			dp.drawHoles = drawHoles.getState();
			pl.paramChanged(selector, dp.makeCopy());
		} else if (e.getSource().equals(allowSpatialFilter)) {
			dp.allowSpatialFilter = allowSpatialFilter.getState();
			pl.paramChanged(selector, dp.makeCopy());
		}

	}

	@Override
	public void colorChanged(Color c, Object sel) {
		Button b = (Button) sel;
		if (b == chLine) {
			dp.lineColor = c;
			chLine.setBackground(c);
			chLine.setForeground(new Color(c.getRGB() ^ 0xFFFFFF));
		} else if (b == chFill) {
			dp.fillColor = c;
			chFill.setBackground(c);
			chFill.setForeground(new Color(c.getRGB() ^ 0xFFFFFF));
		} else if (b == chLabels) {
			dp.labelColor = c;
			chLabels.setBackground(c);
			chLabels.setForeground(new Color(c.getRGB() ^ 0xFFFFFF));
			chLabelsFont.setForeground(c);
			if (pFontSelect != null) {
				pFontSelect.setCurrentFontColor(c);
			}
		} else if (b == chCircleColor) {
			dp.hlCircleColor = c;
			chCircleColor.setBackground(c);
			chCircleColor.setForeground(new Color(c.getRGB() ^ 0xFFFFFF));
		}
		if (b != chCircleColor) {
			pl.paramChanged(selector, dp.makeCopy());
		}
	}

	@Override
	public void fontChanged(Font f, int style, Object sel) {
		if (f == null || (dp.fontName != null && dp.fontName.equals(f.getName()) && dp.fontStyle == f.getStyle() && dp.fontSize == f.getSize() && dp.labelStyle == style))
			return;
		dp.fontName = f.getName();
		dp.fontStyle = f.getStyle();
		dp.fontSize = f.getSize();
		dp.labelStyle = style;
		pl.paramChanged(selector, dp.makeCopy());
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowClosing(WindowEvent e) {
		pl.paramChanged(selector, oldp);
		dispose();
	}

	@Override
	public void windowOpened(WindowEvent e) {
	}
}
//~iitp
