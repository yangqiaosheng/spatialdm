//iitp
package spade.lib.color;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;
import java.util.ResourceBundle;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.FoldablePanel;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.Slider;
import spade.lib.lang.Language;

public class ColorDlg extends Dialog implements ActionListener, WindowListener, FocusListener {
	static ResourceBundle res = Language.getTextResource("spade.lib.color.Res");
	Button but, cancel;
	public Color color = new Color(0, 0, 0);
	float hue = 0, sat = 0, bri = 0;
	Color oldcol = new Color(0, 0, 0);
	Color forcol = new Color(0, 0, 0);
// boolean firstrun = false;
	ColorListener cl;
	Object selector;
	LeftCanvas lcanvas = new LeftCanvas(this);
	RightCanvas rcanvas = new RightCanvas(this);

	Slider slRed = new Slider(this, 0, 255, 0);
	Slider slGrn = new Slider(this, 0, 255, 0);
	Slider slBlu = new Slider(this, 0, 255, 0);
	Slider slHue = new Slider(this, 0, 360, 0);
	Slider slSat = new Slider(this, 0, 100, 0);
	Slider slBri = new Slider(this, 0, 100, 0);

	TextField tRed = new TextField(3);
	TextField tGrn = new TextField(3);
	TextField tBlu = new TextField(3);
	TextField tHue = new TextField(3);
	TextField tSat = new TextField(3);
	TextField tBri = new TextField(3);

	FoldablePanel fsliders = new FoldablePanel();
	boolean dynamic = false;
	protected boolean wasCancelled = false;

	public boolean wasCancelled() {
		return wasCancelled;
	}

	public ColorDlg(Frame owner, String title) {
		super(owner, res.getString("Select_color"), true);
		setResizable(false);
		restOfConstructor();
	}

	public ColorDlg(Frame owner) {
		this(owner, res.getString("Select_color"));
	}

	public void restOfConstructor() {
		addComponents();
		pack();
//  setSize(256+55, 256+400);
		addWindowListener(this);
		tRed.addActionListener(this);
		tGrn.addActionListener(this);
		tBlu.addActionListener(this);
		tHue.addActionListener(this);
		tSat.addActionListener(this);
		tBri.addActionListener(this);
		tRed.addFocusListener(this);
		tGrn.addFocusListener(this);
		tBlu.addFocusListener(this);
		tHue.addFocusListener(this);
		tSat.addFocusListener(this);
		tBri.addFocusListener(this);
		fsliders.addActionListener(this);
	}

	void addComponents() {
		setLayout(new BorderLayout());
		Panel colbox = new Panel();
		Panel butbox = new Panel();
		colbox.setBackground(Color.lightGray);
		// following string: "Choose this color"
		but = new Button(res.getString("Choose_this_color"));
		but.setBackground(color);
		forcol = new Color(Color.white.getRGB() ^ color.getRGB());
		but.setForeground(forcol);
		but.addActionListener(this);
		// following string: "Return to this color"
		cancel = new Button(res.getString("Return_to_this_color"));
		cancel.setBackground(oldcol);
		forcol = new Color(Color.white.getRGB() ^ oldcol.getRGB());
		cancel.setForeground(forcol);
		cancel.addActionListener(this);
		colbox.setLayout(new FlowLayout());
		colbox.add(lcanvas);
		colbox.add(rcanvas);
		butbox.setLayout(new BorderLayout());
//  butbox.setSize(256+55, 100);
		butbox.add("South", cancel);
		butbox.add("Center", but);
		Panel bottom = new Panel();
//  bottom.setLayout(new BorderLayout());
		bottom.setLayout(new ColumnLayout());
		add("North", colbox);
		add("Center", bottom);

		Panel sliders = new Panel();
		fsliders.setContent(sliders);
		Panel panR = new Panel();
		Panel panH = new Panel();
		panR.setLayout(new BorderLayout());
		panH.setLayout(new BorderLayout());
		Panel slidR = new Panel();
		Panel slidH = new Panel();
		slidR.setLayout(new GridLayout(3, 1));
		slidH.setLayout(new GridLayout(3, 1));
		Panel valsR = new Panel();
		Panel valsH = new Panel();
		valsR.setLayout(new GridLayout(3, 2));
		valsH.setLayout(new GridLayout(3, 2));
		Label lab;

		sliders.setLayout(new ColumnLayout());
/*
  one = new Panel();
  two = new Panel();
  one.setLayout(new BorderLayout());
  two.setLayout(new GridLayout(1,2));
  lab = new Label("Red");
  lab.setBackground(Color.lightGray);
  two.add(lab);
  two.add(tRed);
  one.add("West", two);
  one.add("Center", slRed);
  sliders.add(one);
*/
////////////////////

		lab = new Label("  R");
		lab.setBackground(Color.lightGray);
		valsR.add(lab);
		valsR.add(tRed);
		slidR.add(slRed);

		lab = new Label("  G");
		lab.setBackground(Color.lightGray);
		valsR.add(lab);
		valsR.add(tGrn);
		slidR.add(slGrn);

		lab = new Label("  B");
		lab.setBackground(Color.lightGray);
		valsR.add(lab);
		valsR.add(tBlu);
		slidR.add(slBlu);

		lab = new Label("  H");
		lab.setBackground(Color.lightGray);
		valsH.add(lab);
		valsH.add(tHue);
		slidH.add(slHue);

		lab = new Label("  S");
		lab.setBackground(Color.lightGray);
		valsH.add(lab);
		valsH.add(tSat);
		slidH.add(slSat);

		lab = new Label("  B");
		lab.setBackground(Color.lightGray);
		valsH.add(lab);
		valsH.add(tBri);
		slidH.add(slBri);

//////////////////////
//  valsR.setSize(50, valsR.getSize().height);
//  valsH.setSize(50, valsH.getSize().height);
		panR.add("West", valsR);
		panR.add("Center", slidR);
		panH.add("West", valsH);
		panH.add("Center", slidH);

		sliders.add(panR);
		sliders.add(new Line(false));
		sliders.add(panH);
/*
  bottom.add("North", fsliders);
  bottom.add("Center", butbox);
*/
		bottom.add(fsliders);
		bottom.add(butbox);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == fsliders) {
			pack();
		} else if (e.getSource() == but) {
			wasCancelled = false;
			if (cl != null) {
				cl.colorChanged(color, selector);
			}
			dispose();
		} else if (e.getSource() == cancel) {
			wasCancelled = true;
			color = oldcol;
			if (cl != null) {
				cl.colorChanged(color, selector);
			}
			dispose();
		} else {
			valuesChanged(e.getSource());
			/*  try {
			  if (e.getSource() == slHue) {
			//    color=newColor(slHue.getValue()/360f, getSat(color), getBri(color));
			    hue=slHue.getValue()/360f;
			    update(false);
			  } else
			  if (e.getSource() == slSat) {
			//    color=newColor(getHue(color), slSat.getValue()/100f, getBri(color));
			    sat=slSat.getValue()/100f;
			    update(false);
			  } else
			  if (e.getSource() == slBri) {
			//    color=newColor(getHue(color), getSat(color), slBri.getValue()/100f);
			    bri=slBri.getValue()/100f;
			    update(false);
			  } else
			
			  if (e.getSource() == tHue) {
			    hue=Float.parseFloat(tHue.getText())/360f;
			    if (hue>1.0) hue=1f;
			    if (hue<0.0) hue=0f;
			//    color=newColor(hue, getSat(color), getBri(color));
			    update(false);
			  } else
			  if (e.getSource() == tSat) {
			    sat=Float.parseFloat(tSat.getText())/100f;
			    if (sat>1.0) sat=1f;
			    if (sat<0.0) sat=0f;
			//    color=newColor(getHue(color), sat, getBri(color));
			    update(false);
			  } else
			  if (e.getSource() == tBri) {
			    bri=Float.parseFloat(tBri.getText())/100f;
			    if (bri>1.0) bri=1f;
			    if (bri<0.0) bri=0f;
			//    color=newColor(getHue(color), getSat(color), bri);
			    update(false);
			  } else
			
			  {
			  if (e.getSource() == slRed) {
			    color=new Color((int)slRed.getValue(), color.getGreen(), color.getBlue());
			  } else
			  if (e.getSource() == slGrn) {
			    color=new Color(color.getRed(), (int)slGrn.getValue(), color.getBlue());
			  } else
			  if (e.getSource() == slBlu) {
			    color=new Color(color.getRed(), color.getGreen(), (int)slBlu.getValue());
			  } else
			  if (e.getSource() == tRed) {
			    int val=Integer.parseInt(tRed.getText());
			    if (val>255) val=255;
			    if (val<0) val=0;
			    color=new Color(val, color.getGreen(), color.getBlue());
			  } else
			  if (e.getSource() == tGrn) {
			    int val=Integer.parseInt(tGrn.getText());
			    if (val>255) val=255;
			    if (val<0) val=0;
			    color=new Color(color.getRed(), val, color.getBlue());
			  } else
			  if (e.getSource() == tBlu) {
			    int val=Integer.parseInt(tBlu.getText());
			    if (val>255) val=255;
			    if (val<0) val=0;
			    color=new Color(color.getRed(), color.getGreen(), val);
			  }
			    update(true);
			  }
			  } catch (Exception ex) {}*/
		}
	}

	public void valuesChanged(Object source) {
		try {
			if (source == slHue) {
//    color=newColor(slHue.getValue()/360f, getSat(color), getBri(color));
				hue = (float) slHue.getValue() / 360f;
				update(false);
			} else if (source == slSat) {
//    color=newColor(getHue(color), slSat.getValue()/100f, getBri(color));
				sat = (float) slSat.getValue() / 100f;
				update(false);
			} else if (source == slBri) {
//    color=newColor(getHue(color), getSat(color), slBri.getValue()/100f);
				bri = (float) slBri.getValue() / 100f;
				update(false);
			} else

			if (source == tHue) {
				try {
					float h = Float.valueOf(tHue.getText()).floatValue() / 360f;
					hue = h;
					if (hue > 1.0) {
						hue = 1f;
					}
					if (hue < 0.0) {
						hue = 0f;
					}
					update(false);
				} catch (NumberFormatException nfe) {
				}
			} else if (source == tSat) {
				try {
					float s = Float.valueOf(tSat.getText()).floatValue() / 100f;
					sat = s;
					if (sat > 1.0) {
						sat = 1f;
					}
					if (sat < 0.0) {
						sat = 0f;
					}
					update(false);
				} catch (NumberFormatException nfe) {
				}
			} else if (source == tBri) {
				try {
					float b = Float.valueOf(tBri.getText()).floatValue() / 100f;
					bri = b;
					if (bri > 1.0) {
						bri = 1f;
					}
					if (bri < 0.0) {
						bri = 0f;
					}
					update(false);
				} catch (NumberFormatException nfe) {
				}
			} else {
				if (source == slRed) {
					color = new Color((int) slRed.getValue(), color.getGreen(), color.getBlue());
				} else if (source == slGrn) {
					color = new Color(color.getRed(), (int) slGrn.getValue(), color.getBlue());
				} else if (source == slBlu) {
					color = new Color(color.getRed(), color.getGreen(), (int) slBlu.getValue());
				} else if (source == tRed) {
					int val = Integer.parseInt(tRed.getText());
					if (val > 255) {
						val = 255;
					}
					if (val < 0) {
						val = 0;
					}
					color = new Color(val, color.getGreen(), color.getBlue());
				} else if (source == tGrn) {
					int val = Integer.parseInt(tGrn.getText());
					if (val > 255) {
						val = 255;
					}
					if (val < 0) {
						val = 0;
					}
					color = new Color(color.getRed(), val, color.getBlue());
				} else if (source == tBlu) {
					int val = Integer.parseInt(tBlu.getText());
					if (val > 255) {
						val = 255;
					}
					if (val < 0) {
						val = 0;
					}
					color = new Color(color.getRed(), color.getGreen(), val);
				}
				update(true);
			}
		} catch (Exception ex) {
		}
	}

	@Override
	public void paint(Graphics g) {
		pack();
		lcanvas.repaint();
		rcanvas.repaint();
		but.setBackground(color);
		forcol = new Color(Color.white.getRGB() ^ color.getRGB());
		but.setForeground(forcol);
		cancel.setBackground(oldcol);
		forcol = new Color(Color.white.getRGB() ^ oldcol.getRGB());
		cancel.setForeground(forcol);
	}

	public void update(boolean RGB) {
		if (RGB) {
			hue = getHue(color);
			sat = getSat(color);
			bri = getBri(color);
		} else {
			color = newColor(hue, sat, bri);
		}

		but.setBackground(color);
		forcol = new Color(Color.white.getRGB() ^ color.getRGB());
		but.setForeground(forcol);
		cancel.setBackground(oldcol);
		forcol = new Color(Color.white.getRGB() ^ oldcol.getRGB());
		cancel.setForeground(forcol);

		slRed.setValue(color.getRed());
		slGrn.setValue(color.getGreen());
		slBlu.setValue(color.getBlue());
		slHue.setValue(hue * 360);
		slSat.setValue(sat * 100);
		slBri.setValue(bri * 100);

		tRed.setText(new Integer(color.getRed()).toString());
		tGrn.setText(new Integer(color.getGreen()).toString());
		tBlu.setText(new Integer(color.getBlue()).toString());
		tHue.setText(new Integer((int) (hue * 360)).toString());
		tSat.setText(new Integer((int) (sat * 100)).toString());
		tBri.setText(new Integer((int) (bri * 100)).toString());

		lcanvas.repaint();
		rcanvas.repaint();

		if (dynamic && cl != null) {
			cl.colorChanged(color, selector);
		}
	}

/*
 public void prepare() {
  oldcol=color;
 }
*/
	public void selectColor(ColorListener cl, Object selector, Color c, boolean dynamic) {
		this.dynamic = dynamic;
		slRed.setNAD(dynamic);
		slGrn.setNAD(dynamic);
		slBlu.setNAD(dynamic);
		slHue.setNAD(dynamic);
		slSat.setNAD(dynamic);
		slBri.setNAD(dynamic);

		selectColor(cl, selector, c);
	}

	public void selectColor(ColorListener cl, Object selector, Color c) {
		this.cl = cl;
		this.selector = selector;
		color = c;
		oldcol = color;
		update(true);
		lcanvas.repaint();
		rcanvas.repaint();
		Dimension frsz = getSize();
		int sw = Metrics.scrW(), sh = Metrics.scrH();
		if (frsz.width > sw * 2 / 3) {
			frsz.width = sw * 2 / 3;
		}
		if (frsz.height > sh * 2 / 3) {
			frsz.height = sh * 2 / 3;
		}
		setBounds((sw - frsz.width) / 2, (sh - frsz.height) / 2, frsz.width, frsz.height);
		setVisible(true);
	}

	public Color getColor() {
		return color;
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}

	@Override
	public void windowActivated(WindowEvent e) {
		repaint();
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		repaint();
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowClosing(WindowEvent e) {
		wasCancelled = true;
		color = oldcol;
		if (cl != null) {
			cl.colorChanged(color, selector);
		}
		dispose();
	}

	@Override
	public void windowOpened(WindowEvent e) {
		repaint();
	}

	@Override
	public void focusGained(FocusEvent e) {
	}

	@Override
	public void focusLost(FocusEvent e) {
		valuesChanged(e.getSource());
	}

	public float getHue(Color c) {
		float[] hsbvals = new float[3];
		hsbvals = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsbvals);
		return hsbvals[0];
	}

	public float getSat(Color c) {
		float[] hsbvals = new float[3];
		hsbvals = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsbvals);
		return hsbvals[1];
	}

	public float getBri(Color c) {
		float[] hsbvals = new float[3];
		hsbvals = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsbvals);
		return hsbvals[2];
	}

	public Color newColor(float hue, float sat, float bri) {
		return new Color(Color.HSBtoRGB(hue, sat, bri));
	}
}

class LeftCanvas extends Canvas implements MouseListener, MouseMotionListener {
	Image img, grad;
	Graphics og;
	ColorDlg dialog;

	public LeftCanvas(ColorDlg c) {
		super();
		dialog = c;
		int pixels[] = new int[256 * 256];
		for (int i = 0; i < 256; i++) {
			for (int j = 0; j < 256; j++) {
				pixels[i * 256 + j] = Color.HSBtoRGB((float) j / 255, (255 - (float) i) / 255, (float) 1.0);
			}
		}
		grad = createImage(new MemoryImageSource(256, 256, ColorModel.getRGBdefault(), pixels, 0, 256));
		setSize(256, 256);
		addMouseListener(this);
		addMouseMotionListener(this);
	}

	@Override
	public void paint(Graphics g) {
		if (img == null) {
			img = createImage(256, 256);
			og = img.getGraphics();
		}

//		if(dialog.firstrun) g.drawImage(img, 0, 0, this);
		og.drawImage(grad, 0, 0, this);

		og.setXORMode(Color.white);
		og.setColor(Color.black);

		int x = (int) (dialog.hue * 255);
		int y = (int) ((1 - dialog.sat) * 255);
//    int ox = (int)(dialog.ohue*255);
//    int oy = (int)((1-dialog.osat)*255);
		final int r = 3;
/*
    if(!dialog.firstrun) {
      og.drawLine(ox-r, oy, ox+r, oy);
      og.drawLine(ox, oy-r, ox, oy+r);
    }
*/
		og.drawLine(x - r, y, x + r, y);
		og.drawLine(x, y - r, x, y + r);
		og.setPaintMode();

		g.drawImage(img, 0, 0, this);

	}

	public void change(int x, int y) {
//    dialog.ohue = dialog.hue;
//    dialog.osat = dialog.sat;

//    dialog.color=dialog.newColor((float)x/255, (255-(float)y)/255, dialog.getBri(dialog.color));
		if (x < 0) {
			x = 0;
		}
		if (y < 0) {
			y = 0;
		}
		if (x > 255) {
			x = 255;
		}
		if (y > 255) {
			y = 255;
		}
		dialog.hue = (float) x / 255;
		dialog.sat = (255 - (float) y) / 255;

		dialog.update(false);
//  	repaint();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		change(e.getX(), e.getY());
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		change(e.getX(), e.getY());
	}

	@Override
	public void update(Graphics g) {
		paint(g);
	}
}

class RightCanvas extends Canvas implements MouseListener, MouseMotionListener {
	ColorDlg dialog;
	final int r = 25;
	Image img;
	Graphics og;

	public RightCanvas(ColorDlg c) {
		super();
		dialog = c;
/*
  int pixels[] = new int[r * 256];
	for(int i=0; i<256; i++)
		for(int j=0; j<r; j++)
			pixels[i * r + j] =
			 Color.HSBtoRGB((float)0.0,(float)0.0,(float)i/255);
  img = createImage(new MemoryImageSource(r, 256,
	                 	      ColorModel.getRGBdefault(), pixels, 0, r));
*/
		setSize(r + 10, 256);
		addMouseListener(this);
		addMouseMotionListener(this);
	}

	@Override
	public void paint(Graphics g) {
		int z = (int) (dialog.bri * 255);
//    int oz = (int)(dialog.obri*255);

		if (img == null) {
			img = createImage(r + 10, 256);
			og = img.getGraphics();
		}

		og.clearRect(0, 0, r + 10, 256);
		for (int i = 0; i < 255; i++) {
			og.setColor(dialog.newColor(dialog.getHue(dialog.color), dialog.getSat(dialog.color), (float) i / 256));
			og.drawLine(10, i, r + 10, i);
		}

		og.setXORMode(Color.white);
		og.setColor(Color.black);
		og.drawLine(0, z, r + 10, z);
		og.setPaintMode();

		g.drawImage(img, 0, 0, this);
	}

	public void change(int y) {
		if (y < 0) {
			y = 0;
		} else if (y > 255) {
			y = 255;
//    dialog.obri = dialog.bri;
		}

//    dialog.color = dialog.newColor(dialog.getHue(dialog.color), dialog.getSat(dialog.color), (float)y/255);
		dialog.bri = (float) y / 255;

		dialog.update(false);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		change(e.getY());
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		change(e.getY());
	}

	@Override
	public void update(Graphics g) {
		paint(g);
	}
}
//~iitp
